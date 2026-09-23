#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { prepareSource,newJob,task,submit,approve,exportBook,publicationGate,setReadingPlan } from '../public/engine/learning.mjs';
import { FileStore } from './runner.mjs';
const [action,dir,...args]=process.argv.slice(2);
const fail=()=>{console.error('Usage: node engine/cli.mjs prepare JOB_DIR TRANSCRIPT VIDEO_ID [SECONDS]\n       node engine/cli.mjs task JOB_DIR CHUNK_ID source|beginner|review\n       node engine/cli.mjs submit JOB_DIR CHUNK_ID STAGE ARTIFACT_JSON ACTOR\n       node engine/cli.mjs plan JOB_DIR READING_PLAN_JSON\n       node engine/cli.mjs status JOB_DIR\n       node engine/cli.mjs approve JOB_DIR OWNER\n       node engine/cli.mjs export JOB_DIR OUTPUT_JSON');process.exitCode=2;};
if(!action||!dir)fail();else{
const store=new FileStore(dir);store.acquire();
try{
 if(action==='prepare'){
   if(fs.existsSync(path.join(dir,'job.json')))throw Error('기존 작업을 덮어쓰지 않습니다. 다른 JOB_DIR을 사용하세요.');
   const[filename,id,seconds]=args;const source=await prepareSource({transcript:fs.readFileSync(filename,'utf8'),videoId:id,durationSeconds:seconds?Number(seconds):undefined});
   const job=await newJob(source);await store.save(job);console.log(JSON.stringify({jobId:job.id,chunks:source.chunks,warnings:source.warnings},null,2));
 }else{
   const job=await store.load();
   if(action==='status')console.log(JSON.stringify({status:job.status,gate:publicationGate(job),completed:Object.keys(job.checkpoints)},null,2));
   else if(action==='task')console.log(JSON.stringify(task(job,args[0],args[1]),null,2));
   else if(action==='submit'){await submit(job,args[0],args[1],JSON.parse(fs.readFileSync(args[2],'utf8')),{actor:args[3]||'assistant-author'});await store.save(job);console.log(job.status);}
   else if(action==='plan'){setReadingPlan(job,JSON.parse(fs.readFileSync(args[0],'utf8')));await store.save(job);console.log('Reading plan saved; affected flow reviews invalidated.');}
   else if(action==='approve'){await approve(job,args[0]);await store.save(job);console.log(job.status);}
   else if(action==='export'){fs.writeFileSync(args[0],JSON.stringify(await exportBook(job),null,2));console.log(args[0]);}
   else fail();
 }
}catch(e){console.error(JSON.stringify({code:e.code||'ERROR',message:e.message,details:e.details||{}}));process.exitCode=1;}finally{store.release();}
}
