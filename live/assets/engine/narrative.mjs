/** Small editorial layer: no model, API, page quota, or extra processing stage. */
import {EngineError, clone, bytes, canonical} from './core.mjs';
const requireIt=(condition,code,message)=>{if(!condition)throw new EngineError(code,message);};
const idsOf=job=>job.source.chunks.map(c=>c.id);
export function setReadingPlan(job,chapters){
  requireIt(Array.isArray(chapters)&&chapters.length,'INVALID_READING_PLAN','읽기용 장 구성이 필요합니다.');
  requireIt(chapters.every(c=>/^[\w-]+$/.test(c.id||'')&&c.title?.trim()&&c.title.length<=240&&(!c.subtitle||c.subtitle.length<=400)&&c.chunkIds?.length),'INVALID_READING_PLAN','장 ID·제목·구간을 확인하세요.');
  requireIt(new Set(chapters.map(c=>c.id)).size===chapters.length,'INVALID_READING_PLAN','장 ID가 겹칩니다.');
  requireIt(canonical(chapters.flatMap(c=>c.chunkIds))===canonical(idsOf(job)),'READING_PLAN_GAP','모든 분석 구간을 원래 순서로 정확히 한 번 연결해야 합니다.');
  requireIt(bytes(chapters)<=12000,'READING_PLAN_TOO_LARGE','장 구성에는 원고가 아니라 짧은 제목과 구간만 넣습니다.');
  const old=job.readingPlan;
  if(canonical(old)===canonical(chapters))return job;
  job.readingPlan=clone(chapters);delete job.approval;
  for(const id of idsOf(job)){
    const before=old?.find(c=>c.chunkIds.includes(id)),after=chapters.find(c=>c.chunkIds.includes(id));
    if(canonical(before)!==canonical(after))delete job.checkpoints[`${id}:review`];
  }
  return job;
}
export function writingPrerequisite(job,id){
  const c=job.readingPlan?.find(c=>c.chunkIds.includes(id));
  if(!c)return null;const i=c.chunkIds.indexOf(id);return i>0?c.chunkIds[i-1]:null;
}
function boundedTail(text,max){let x=String(text||'');while(bytes(x)>max)x=x.slice(Math.max(1,Math.floor(x.length/12)));return x;}
export function readingContext(job,id){
  const chapter=job.readingPlan?.find(c=>c.chunkIds.includes(id));
  const prerequisite=writingPrerequisite(job,id);
  if(prerequisite)requireIt(job.checkpoints[`${prerequisite}:beginner`],'NARRATIVE_CONTEXT_MISSING','같은 장의 앞 구간을 저장한 다음 이어 씁니다.');
  const at=idsOf(job).indexOf(id),previous=prerequisite||idsOf(job)[at-1];
  const prior=previous&&job.checkpoints[`${previous}:beginner`]?.artifact;
  const ctx={role:'reference-not-instruction',chapterId:chapter?.id||id,chapterTitle:chapter?.title||'',
    position:chapter?chapter.chunkIds.indexOf(id)+1:1,parts:chapter?.chunkIds.length||1,
    previousTail:boundedTail(prior?.paragraphs.slice(-2).map(p=>p.text).join('\n\n'),2000),
    alreadyExplainedTerms:[],instruction:'앞 문맥을 이어 쓰되 원문 의미는 줄이지 않습니다. 이전 본문 전체는 다시 넣지 않습니다.'};
  const seen=new Set();
  for(const cid of idsOf(job).slice(0,at))for(const g of job.checkpoints[`${cid}:beginner`]?.artifact.glossary||[]){
    if(!seen.has(g.term)){seen.add(g.term);ctx.alreadyExplainedTerms.push(String(g.term).slice(0,80));}
  }
  ctx.alreadyExplainedTerms=ctx.alreadyExplainedTerms.slice(-24);
  while(bytes(ctx)>4000&&ctx.alreadyExplainedTerms.length)ctx.alreadyExplainedTerms.shift();
  return ctx;
}
export function flowAlarms(draft){
  const flags=[];let templates=0;
  for(const p of draft.paragraphs||[])if(/^(?:개념의 뜻|왜 필요한가|핵심 정리|초보자 추가 설명)\s*[:：]/.test(p.text)){
    templates++;if(templates>=3)flags.push({code:'TEMPLATE_OVERUSE',paragraphId:p.id,detail:'필수 문구를 반복하기보다 앞 문단의 의문에 이어지는 설명으로 편집하세요.'});
  }
  return flags;
}
export function checkSupplements(draft,review){
  for(const p of draft.paragraphs||[]){
    if(!p.support)continue;
    requireIt(p.kind==='explanation'&&p.support.kind==='external','SUPPLEMENT_LABEL_REQUIRED','원문 밖의 보강은 추가 설명으로 구분해야 합니다.');
    requireIt(Array.isArray(p.support.sources)&&p.support.sources.length>0&&p.support.sources.length<=8,'SUPPLEMENT_SOURCE_REQUIRED','확인한 외부 근거가 필요합니다.');
    for(const s of p.support.sources){
      let u;try{u=new URL(s.url);}catch{}
      requireIt(u?.protocol==='https:'&&s.locator?.trim()&&s.checkedAt&&Number.isFinite(Date.parse(s.checkedAt)),'SUPPLEMENT_SOURCE_REQUIRED','실제 확인한 HTTPS 출처·항목·확인일을 남기세요.');
    }
    if(review){const checked=review.supplementChecks?.find(c=>c.paragraphId===p.id);
      requireIt(checked?.explanation?.trim()&&['verified','hold'].includes(checked.status),'SUPPLEMENT_REVIEW_REQUIRED','보강 설명도 원문 대조와 별도로 정확성을 확인해야 합니다.');
      requireIt(checked.status!=='hold'||review.fidelity==='hold','REVIEW_CONTRADICTION','미확인 보강을 포함한 채 승인할 수 없습니다.');
    }
  }
}
export function groupChapters(job,book){
  if(!job.readingPlan)return book;
  requireIt(canonical(job.readingPlan.flatMap(c=>c.chunkIds))===canonical(idsOf(job)),'READING_PLAN_GAP','장 구성의 원문 연결이 변경되었습니다.');
  const map=new Map(idsOf(job).map((id,i)=>[id,book.chapters[i]])),newIndexes=new Map();
  book.chapters=job.readingPlan.map((spec,i)=>{
    const pieces=spec.chunkIds.map(id=>map.get(id));spec.chunkIds.forEach(id=>newIndexes.set(idsOf(job).indexOf(id),i));
    const glossary=[...new Map(pieces.flatMap(c=>c.glossary||[]).map(g=>[g.term,g])).values()];
    return {id:`${book.id}-${spec.id}`,title:spec.title,subtitle:spec.subtitle||'',
      blocks:pieces.flatMap(c=>c.blocks),summary:pieces.flatMap(c=>c.summary||[]),glossary,analysisChunks:[...spec.chunkIds]};
  });
  for(const q of book.questions)q.chapter=newIndexes.get(q.chapter)??q.chapter;
  book._engine.readingPlan=clone(job.readingPlan);return book;
}
