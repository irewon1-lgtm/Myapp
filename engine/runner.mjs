/** Durable, bounded orchestration. Model transport is injected, not hidden.
 * Exactly-once billing cannot be guaranteed across a process/network failure:
 * uncertain calls are HELD until a human reconciles them; never blindly retried.
 */
import fs from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import { canonical,digest,EngineError,task,submit,failurePolicy,reserveCall,publicationGate,writingPrerequisite } from '../public/engine/learning.mjs';
export class FileStore {
  constructor(dir) { this.dir=path.resolve(dir);fs.mkdirSync(this.dir,{recursive:true,mode:0o700});this.lock=null; }
  acquire() { try{this.lock=fs.openSync(path.join(this.dir,'writer.lock'),'wx',0o600);fs.writeFileSync(this.lock,canonical({pid:process.pid,createdAt:new Date().toISOString()}));}catch{throw new EngineError('WRITER_LOCKED','다른 작업이 이 상태 파일을 사용 중입니다. 비정상 종료 후에는 실행 중인 작업이 없는지 확인해야 합니다.');} }
  release(){if(this.lock!==null){fs.closeSync(this.lock);this.lock=null;fs.rmSync(path.join(this.dir,'writer.lock'),{force:true});}}
  async save(job){
    if(this.lock===null)throw new EngineError('LOCK_REQUIRED','상태 저장 잠금이 필요합니다.');
    const envelope={schema:1,hash:await digest(job),job};const tmp=path.join(this.dir,`job.${randomUUID()}.tmp`),dest=path.join(this.dir,'job.json');
    const fd=fs.openSync(tmp,'wx',0o600);try{fs.writeFileSync(fd,JSON.stringify(envelope));fs.fsyncSync(fd);}finally{fs.closeSync(fd);}
    if(fs.existsSync(dest))fs.copyFileSync(dest,path.join(this.dir,'job.last-good.json'));
    fs.renameSync(tmp,dest);
  }
  async load(){
    const p=path.join(this.dir,'job.json');let d;try{d=JSON.parse(fs.readFileSync(p,'utf8'));}catch{throw new EngineError('CORRUPT_STATE','상태 파일을 읽지 못했습니다. 원본 상태를 덮어쓰지 않습니다.');}
    if(d.hash!==await digest(d.job))throw new EngineError('CORRUPT_STATE','저장된 상태의 해시가 맞지 않습니다.');return d.job;
  }
}
const pause=ms=>new Promise(r=>setTimeout(r,ms));
export function availableTasks(job){
  if(job.cancelled)return[];
  return job.source.chunks.flatMap(ch=>{
    for(const stage of['source','beginner','review'])if(!job.checkpoints[`${ch.id}:${stage}`]){
      if(stage==='beginner'){const previous=writingPrerequisite(job,ch.id);if(previous&&!job.checkpoints[`${previous}:beginner`])return[];}
      const a=job.attempts[`${ch.id}:${stage}`];
      if(a&&['IN_FLIGHT','UNCERTAIN_CALL','BLOCKED_AUTH','REVIEW_REQUIRED','PAUSED_ERROR','BUDGET_PAUSED'].includes(a.state))return[];
      if(a?.retryAt>Date.now())return[];
      return[{chunkId:ch.id,stage}];
    }return[];
  });
}
export async function runSlice(job,{store,provider,wallBudgetMs=20000,sleep=pause}={}){
  if(!provider||typeof provider.generate!=='function')throw new EngineError('NO_PROVIDER','작성 작업을 수행할 제공자가 없습니다.');
  if(provider.kind!=='test-fixture'&&!job.config.externalCallsEnabled)throw new EngineError('EXTERNAL_CALLS_DISABLED','자동 외부 호출이 꺼져 있습니다. 요청 묶음을 작성자에게 전달하세요.');
  // Store must durably persist BEFORE any outbound request.
  const begin=Date.now();let count=0,serial=Promise.resolve();
  const save=()=>{serial=serial.then(()=>store.save(job));return serial;};
  while(Date.now()-begin<wallBudgetMs&&!job.cancelled){
    const work=availableTasks(job).slice(0,job.config.concurrency);if(!work.length)break;
    await Promise.all(work.map(async({chunkId,stage})=>{
      const key=`${chunkId}:${stage}`,old=job.attempts[key],attempt=(old?.count||0)+1;
      if(attempt>job.config.maxAttempts){job.attempts[key]={...old,state:'PAUSED_ERROR'};await save();return;}
      const requestId=`${job.id}:${key}:${attempt}`,packet=task(job,chunkId,stage);
      const maximumCostUsd=provider.kind==='test-fixture'?0:await provider.quoteMaximumCost(packet,job.config.maxOutputTokens);
      try{if(provider.kind!=='test-fixture')reserveCall(job,{maximumCostUsd,requestId});}
      catch(e){job.attempts[key]={count:attempt-1,state:e.code||'BUDGET_PAUSED'};job.status='BUDGET_PAUSED';await save();return;}
      job.attempts[key]={count:attempt,state:'IN_FLIGHT',requestId,startedAt:new Date().toISOString()};await save();
      const controller=new AbortController();let timer;
      try{
        const timeout=new Promise((_,reject)=>{timer=setTimeout(()=>{controller.abort();reject(new EngineError('TIMEOUT','응답 확인 전 시간 한도에 도달했습니다.'));},job.config.requestTimeoutMs);});
        const result=await Promise.race([provider.generate(packet,{signal:controller.signal,requestId,maxOutputTokens:job.config.maxOutputTokens}),timeout]);
        clearTimeout(timer);if(job.cancelled){job.attempts[key].state='CANCELLED';await save();return;}
        await submit(job,chunkId,stage,result.artifact,{actor:result.actor||provider.authorId||'author',model:result.model||provider.model||'unknown'});
        job.attempts[key].state='COMPLETED';job.attempts[key].usage=result.usage||null;
        job.attempts[key].maximumCostReservedUsd=maximumCostUsd;count++;await save();
      }catch(error){
        clearTimeout(timer);const decision=failurePolicy(error,attempt,job.config);
        job.attempts[key]={...job.attempts[key],state:decision.state,code:error.code||error.status||'UNKNOWN',retryAt:decision.retry?Date.now()+decision.delayMs:null};
        job.status=decision.state;await save();
        // No retry loop that sleeps through a serverless invocation. A later slice
        // can claim this one task after Retry-After, keeping all completed work.
      }
    }));
  }
  const gate=publicationGate(job);
  if(gate.ready)job.status='AWAITING_OWNER_APPROVAL';else if(job.cancelled)job.status='CANCELLED';
  await save();return{completedThisSlice:count,status:job.status,gate,elapsedMs:Date.now()-begin};
}
export async function recover(job,store){
  for(const a of Object.values(job.attempts))if(a.state==='IN_FLIGHT')a.state='UNCERTAIN_CALL';
  if(Object.values(job.attempts).some(a=>a.state==='UNCERTAIN_CALL'))job.status='UNCERTAIN_CALL';
  await store.save(job);return job;
}
