/** A saved job keeps the style it started with; do not silently migrate old jobs. */
import {authoringContent,fingerprint} from '../scripts/current-spec.mjs';
export function pinAuthoring(job,engine,document){
  if(job.authoringSpec)return job.authoringSpec;
  const content=authoringContent(engine,document);
  job.authoringSpec={contentDigest:fingerprint(content),content:structuredClone(content)};
  return job.authoringSpec;
}
export function readPinnedAuthoring(job){
  if(!job.authoringSpec)return {status:'legacy-unbound',message:'기존 작업에는 규격 사본이 없습니다. 현재 규격을 자동으로 덧씌우지 않습니다.'};
  if(fingerprint(job.authoringSpec.content)!==job.authoringSpec.contentDigest)throw Error('Saved authoring specification is damaged. Preserve the draft and recover its saved specification.');
  return job.authoringSpec;
}
