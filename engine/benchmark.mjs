/** Run real parsing/planning plus an explicitly synthetic scheduling comparison.
 * Simulated seconds below are NOT predictions of Gemini/ChatGPT speed or billing.
 */
import fs from 'node:fs';
import path from 'node:path';
import { performance } from 'node:perf_hooks';
import {prepareSource,planChunks,bytes,DEFAULTS,digest} from '../public/engine/core.mjs';
import {syntheticTranscript} from '../tests/engine/fixtures.mjs';
const root=process.cwd(),resultsDir=path.join(root,'public/engine');
const rng=seed=>()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296;};
const percentile=(a,p)=>[...a].sort((x,y)=>x-y)[Math.floor((a.length-1)*p)];
const report={schema:1,generatedAt:new Date().toISOString(),scope:{localParsing:'Measured JavaScript execution on provided/synthetic transcript data',scheduling:'Synthetic event model. Not actual model generation, video ingestion, semantic quality or provider billing',videoDecoding:'Tested separately with a locally generated controlled clip',paidApiCalls:0},assumptions:{unicodeCharsToTokens:0.85,sourceOutputRatio:0.72,beginnerOutputRatio:1.3,reviewOutputRatio:0.12,sharedOutputTokensPerSimulatedSecond:100,perLaneOutputTokensPerSimulatedSecond:50,fixedSecondsPerCall:3,transient503Probability:0.02,concurrencyAboveTwo:'All lanes share the same output bandwidth. No benefit beyond two in this assumption; real providers may differ',seedsPerProfile:100},realSources:[],syntheticLongInputTests:[],comparisons:[],selectedDefaults:DEFAULTS};
for(const id of['q-j2scvSwdA','Kl5EcYd3G2E','0SGfDKMLdaI']){
 const p=path.join(root,'exact_source',id,'transcript.md');if(!fs.existsSync(p))continue;
 const raw=fs.readFileSync(p,'utf8');const start=performance.now();try{const s=await prepareSource({videoId:id,transcript:raw});report.realSources.push({videoId:id,title:s.title,durationSeconds:s.durationSeconds,rawBytes:bytes(raw),rawChars:raw.length,sha256:s.rawHash,cueCount:s.cues.length,chunks:s.chunks.length,largestChunkBytes:Math.max(...s.chunks.map(c=>c.textBytes)),elapsedMs:performance.now()-start,allCueIdsPreserved:s.chunks.flatMap(c=>c.sourceIds).join('|')===s.cues.map(c=>c.id).join('|'),warnings:s.warnings});}catch(e){report.realSources.push({videoId:id,error:e.code||e.message});}
}
for(const seconds of[780,1800,3600,7200]){
 const raw=syntheticTranscript(seconds);const start=performance.now();const source=await prepareSource({videoId:'0SGfDKMLdaI',durationSeconds:seconds,transcript:raw});
 report.syntheticLongInputTests.push({minutes:seconds/60,bytes:bytes(raw),cues:source.cues.length,chunks:source.chunks.length,elapsedMs:performance.now()-start,allCueIdsPreserved:source.chunks.flatMap(c=>c.sourceIds).length===source.cues.length,largestChunkBytes:Math.max(...source.chunks.map(c=>c.textBytes))});
 for(const target of[120,240,360,480])for(const concurrency of[1,2,3,4]){
  const chunks=await planChunks(source,{targetSeconds:target,maxSeconds:Math.max(360,target)}),chars=chunks.map(ch=>source.cues.filter(c=>ch.sourceIds.includes(c.id)).reduce((s,c)=>s+c.text.length,0));
  const runs=[],retryCalls=[],out=chars.map(n=>[Math.ceil(n*.85*.72),Math.ceil(n*.85*1.3),Math.ceil(n*.85*.12)]);
  for(let seed=1;seed<=100;seed++){
   const random=rng(seed),ready=chars.map(()=>0),stage=chars.map(()=>0),lanes=Array(concurrency).fill(0);let retries=0;
   while(stage.some(x=>x<3)){
    let best=null;for(let i=0;i<stage.length;i++)if(stage[i]<3)for(let lane=0;lane<lanes.length;lane++){const at=Math.max(lanes[lane],ready[i]);if(!best||at<best.at)best={i,lane,at};}
    const {i,lane,at}=best,k=stage[i],speed=Math.min(50,100/concurrency),t=(3+out[i][k]/speed)*(0.8+random()*.4);let end=at+t;
    if(random()<.02){end+=2+t;retries++;}
    lanes[lane]=end;ready[i]=end;stage[i]++;
   }
   runs.push(Math.max(...ready));retryCalls.push(retries);
  }
  const totalOutput=out.flat().reduce((a,b)=>a+b,0);
  report.comparisons.push({minutes:seconds/60,targetSeconds:target,concurrency,chunks:chunks.length,nominalModelCalls:chunks.length*3,estimatedOutputTokens:totalOutput,largestEstimatedSingleOutput:Math.max(...out.flat()),fitsOutputCap:Math.max(...out.flat())<=DEFAULTS.maxOutputTokens,simulatedP50Seconds:percentile(runs,.5),simulatedP95Seconds:percentile(runs,.95),meanExtraRetries:retryCalls.reduce((a,b)=>a+b,0)/100,largestFailedChunkMinutes:Math.max(...chunks.map(c=>c.end-c.start))/60});
 }
}
report.summary=report.syntheticLongInputTests.map(s=>{
 const rows=report.comparisons.filter(r=>r.minutes===s.minutes),chosen=rows.find(r=>r.targetSeconds===240&&r.concurrency===2),serial=rows.find(r=>r.targetSeconds===240&&r.concurrency===1),aggressive=rows.find(r=>r.targetSeconds===240&&r.concurrency===4);
 return{minutes:s.minutes,selectedCalls:chosen.nominalModelCalls,selectedLargestOutput:chosen.largestEstimatedSingleOutput,simulatedMedianRatioVsSerial:chosen.simulatedP50Seconds/serial.simulatedP50Seconds,simulatedMedianRatioVsFourLanes:chosen.simulatedP50Seconds/aggressive.simulatedP50Seconds,choiceReason:'Two lanes use the assumed shared bandwidth without excess in-flight requests. Four-minute chunks limit retry/expansion size. This is a conservative default, not a global optimum.'};
});
report.realSourceStatus=report.realSources.length?'REPLAYED_EXISTING_TRANSCRIPTS':'NOT_AVAILABLE_IN_LOCAL_FIXTURE_PACKAGE';
fs.mkdirSync(resultsDir,{recursive:true});fs.writeFileSync(path.join(resultsDir,'benchmark.json'),JSON.stringify(report,null,2));
console.log(JSON.stringify({realSources:report.realSources,stress:report.syntheticLongInputTests,comparisonProfiles:report.comparisons.length,syntheticSchedulingRuns:report.comparisons.length*100,summary:report.summary},null,2));
