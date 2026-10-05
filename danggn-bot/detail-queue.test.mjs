import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import vm from 'node:vm';
import { parseSearch, parseRouteSearch, analyzeHardware, detailOne, searchOne, main, CITIES, QUERIES } from './hourly-scan.mjs';
import { canonicalKey, mergeListings, migrateState, expireTasks, priority, retryTask, runDetailPool } from './detail-queue.mjs';
const now = Date.now();
const make = (id, extra={}) => ({id, url:`https://www.daangn.com/kr/buy-sell/title-${id}/`, title:'RTX5070 RAM 32GB', price:1000, status:'Ongoing', postedAt:new Date(now-1000).toISOString(), regionPath:'경기도 군포시', location:'당정동', city:'군포시', firstSeenAt:new Date(now).toISOString(), ...extra});
const beforeSource = await fs.readFile(new URL('./fixtures/hourly-scan-before.txt', import.meta.url),'utf8');
const context = vm.createContext({});
vm.runInContext(beforeSource.slice(beforeSource.indexOf('function extractBalancedJson'),beforeSource.indexOf('async function getHtml')) + '\n' + beforeSource.slice(beforeSource.indexOf('function capacityToGB'),beforeSource.indexOf('function extractCpu')) + '\nthis.parseSearch = parseSearch; this.analyzeHardware = analyzeHardware;',context);
context.BASE='https://www.daangn.com';context.SEARCH=context.BASE+'/kr/buy-sell/';

test('Test 1 parser: public timestamps, escaped embedded JSON, JSON-LD, boost excluded',()=>{
 const raw={id:'abc123',href:'/kr/buy-sell/abc123/',title:'노트북',createdAt:new Date(now).toISOString()};
 const fixtures=[`<script>{"fleamarketArticles":${JSON.stringify([raw])}}</script>`,`<script>enqueue(${JSON.stringify(JSON.stringify({fleamarketArticles:[raw]}))})</script>`,`<script type="application/ld+json">${JSON.stringify({'@type':'ItemList',itemListElement:[{item:{url:raw.href,name:raw.title,datePublished:raw.createdAt}}]})}</script>`];
 const old=fixtures.flatMap(x=>context.parseSearch(x,'q','r')).filter(x=>x.postedAt).length;
 const updated=fixtures.flatMap(x=>parseSearch(x,'q','r')).filter(x=>x.postedAt).length;
 assert.equal(old,1);assert.equal(updated,3);console.log('parser fixture postedAt: before 1/3 -> after 3/3');
 assert.equal(parseSearch('<script>{"fleamarketArticles":[{"id":"abc","boostedAt":"2026-10-05"}]}</script>','q','r')[0].postedAt,null);
 assert.equal(parseRouteSearch({allPage:{fleamarketArticles:[raw]}},'q',{slug:'r',id:1})[0].postedAt,raw.createdAt);
});

test('Test 2: four regions/queries coalesce to exactly one detail HTTP request',async()=>{
 const oldFetch=global.fetch;let calls=0;
 global.fetch=async()=>{calls++;return new Response(JSON.stringify({product:{content:'RAM 32GB',createdAt:new Date(now).toISOString(),region:{name1:'경기도',name2:'군포시',name3:'당정동'}}}));};
 try {
  const queue=['군포시','의왕시','안양시','과천시'].map(city=>make('same123',{sourceRegion:city,sourceCities:[city],sourceQuery:city}));
  const result=await runDetailPool(queue,Date.now()+10000,detailOne);
  assert.equal(calls,1);assert.equal(result.length,1);assert.equal(result[0].sourceRegion.length,4);
  assert.equal(canonicalKey({id:'unknown',url:'https://www.daangn.com/kr/buy-sell/foo/?a=1#b'}),canonicalKey({url:'https://www.daangn.com/kr/buy-sell/foo'}));
 } finally { global.fetch=oldFetch; }
});

test('Test 3: fresh events outrank 300 older backlog tasks',async()=>{
 const seen=[];const q=Array.from({length:300},(_,i)=>make('old'+i,{changeType:'newness_backlog'}));
 q.push(make('fresh',{changeType:'newness_check',freshTask:true}),make('drop',{changeType:'price_drop',freshTask:true}),make('new',{changeType:'new',freshTask:true}));
 await runDetailPool(q,Infinity,async x=>{seen.push(x.id);return x;},{workers:1});
 assert.deepEqual(seen.slice(0,3),['drop','new','fresh']);
});

test('Test 4: 404 clears work but price history remains; 429 backoff; timeout and missing date bounded',async()=>{
 const saved=make('x',{lastPrice:1000,pendingNewnessCheck:true,pendingDetailEvent:{changeType:'price_drop'}});retryTask(saved,'HTTP 404',now);assert.equal(saved.pendingDetailEvent,null);assert.equal(saved.lastPrice,1000);
 const rate=make('rate',{pendingNewnessCheck:true});retryTask(rate,'HTTP 429',now);assert.equal(rate.nextEligibleAt,now+3600000);retryTask(rate,'HTTP 429',now);assert.equal(rate.nextEligibleAt,now+7200000);
 for(const [error,limit] of [['TimeoutError',6],['POSTED_AT_UNKNOWN',3]]){const s={pendingNewnessCheck:true};for(let i=0;i<limit;i++)retryTask(s,error,now);assert.equal(s.pendingNewnessCheck,false);assert.equal(s.detailTerminal,true);}
 let attempts=0;await runDetailPool(Array.from({length:50},(_,i)=>make('rate'+i)),Infinity,async x=>{attempts++;return {...x,detailError:'HTTP 429'};},{workers:1});assert.equal(attempts,1);
});

test('Test 5: hardware corpus preserves old recall and restores broad bare/GPU variants',()=>{
 const texts=['램 32GB','렘32기가','RAM32G','memory 32 GB','메모리 32기가','RTX5070 RAM 32GB','VRAM16GB','그래픽 메모리 16G','GDDR6 16GB','16G','32기가','노트북 / 32G /','RAM 16GB','RTX 4090 Laptop','Ryzen AI Max 395 통합 메모리 64GB'];
 let before=0,after=0;
 for(const title of texts){const old=context.analyzeHardware({title});const fresh=analyzeHardware({title});if(old.ok)before++;if(fresh.ok)after++;assert.ok(!old.ok||fresh.ok,title);assert.ok(fresh.ok,title);}
 assert.equal(analyzeHardware({title:'VRAM16GB'}).hardwareReviewStatus,'PASS_VRAM');
 assert.equal(analyzeHardware({title:'16G'}).hardwareReviewStatus,'CHECK_MEMORY_GPU');
 console.log(`hardware fixture candidates: ${before}/${texts.length} -> ${after}/${texts.length}`);
});

test('Test 6: real state migration and reproducible multi-scan drain, new/price-drop alerts preserved',async()=>{
 const real=JSON.parse(await fs.readFile(new URL('./hourly-state.json',import.meta.url),'utf8'));
 const migrated=migrateState(structuredClone(real));
 const ids=new Set(Object.values(real.items).map(canonicalKey));assert.equal(Object.keys(migrated.items).length,ids.size);
 for(const [id,s] of Object.entries(migrated.items)) { const newest=Object.values(real.items).filter(x=>canonicalKey(x)===id).sort((a,b)=>new Date(b.lastSeenAt)-new Date(a.lastSeenAt))[0];assert.equal(s.lastPrice,newest.lastPrice); }
 const tmp=await fs.mkdtemp(path.join(os.tmpdir(),'danggn-test-'));
 try {
  let state={items:Object.fromEntries(Array.from({length:300},(_,i)=>['군포시|old'+i,make('old'+i,{postedAt:null,pendingNewnessCheck:true})])),newnessBacklogResetAt:new Date(now).toISOString()};
  state.items['군포시|drop']=make('drop',{lastPrice:1200});
  for(let scan=0;scan<3;scan++){
   let simulatedClock = now + scan * 1200000 + 52000;
   const seen=[];const pendingBefore=Object.values(state.items).filter(x=>x.pendingNewnessCheck||x.pendingDetailEvent).length;
   const {result,state:next}=await main({now:now+scan*1200000,state,statePath:path.join(tmp,'state.json'),resultPath:path.join(tmp,'result.json'),resolve:async city=>[{slug:city.name,id:1}],search:async()=>[make('drop'),make('fresh'+scan)],details:async(queue)=>runDetailPool(queue,now+scan*1200000+330000,async x=>{simulatedClock += 1200;seen.push(x.id);return {...x,postedAt:new Date(now+scan*1200000-1000).toISOString(),description:'RAM 32GB'};},{now:()=>simulatedClock})});
   console.log('drain',JSON.stringify({scan,start:pendingBefore,fresh:result.stats.freshDetailTasks,completed:result.stats.detailSucceeded,end:result.stats.backlogEnd,simulatedDurationMs:simulatedClock-(now+scan*1200000)}));
   if(scan===0) assert.ok(result.stats.backlogEnd < pendingBefore); else assert.equal(result.stats.backlogEnd,0);assert.ok(result.candidates.some(x=>x.id==='fresh'+scan));if(scan===0){assert.equal(seen[0],'drop');assert.ok(result.candidates.some(x=>x.id==='drop'&&x.changeType==='price_drop'));}else assert.ok(!result.candidates.some(x=>x.id==='drop'));
   state=next;
  }
  const stale=make('stale',{lastPrice:123,pendingNewnessCheck:true,firstSeenAt:new Date(now-7*3600000).toISOString()});expireTasks({items:{stale}},now);assert.equal(stale.pendingNewnessCheck,false);assert.equal(stale.lastPrice,123);
 }finally{await fs.rm(tmp,{recursive:true,force:true});}
});


test('migration keeps completed state, price baseline, and all source cities',()=>{
 const completed=make('shared',{lastPrice:900,pendingNewnessCheck:false,pendingDetailEvent:null,lastSeenAt:new Date(now).toISOString()});
 const stale=make('shared',{lastPrice:1000,pendingNewnessCheck:true,pendingDetailEvent:{changeType:'price_drop'},lastSeenAt:new Date(now-1000).toISOString(),city:'의왕시'});
 const state=migrateState({items:{a:stale,b:completed}});assert.equal(Object.keys(state.items).length,1);assert.equal(state.items.shared.lastPrice,900);assert.equal(state.items.shared.pendingDetailEvent,null);assert.equal(state.items.shared.sourceCities.length,2);
});

test('existing alert corpus preserves every hardware candidate and does not mutate dedupe data',async()=>{
 const filename=new URL('./pending-alerts.json',import.meta.url), original=await fs.readFile(filename,'utf8');
 for (const item of Object.values(JSON.parse(original).items)) {
  const title=[item.title,item.ramEvidence,item.vramEvidence,item.ambiguousMemoryEvidence].filter(Boolean).join(' ');
  const baseline=context.analyzeHardware({title});const updated=analyzeHardware({title});assert.ok(!baseline.ok||updated.ok,item.id);
 }
 assert.equal(await fs.readFile(filename,'utf8'),original);
});


test('unsupported route-data is requested once while every search still executes',async()=>{
 const old=global.fetch;let routeCalls=0,searchCalls=0;
 global.fetch=async(url)=>{ if(new URL(url).searchParams.has('_data')){routeCalls++;return new Response('Not Found',{status:404});}searchCalls++;return new Response('<script>{"fleamarketArticles":[{"id":"abc123","createdAt":"2026-10-05T00:00:00Z"}]}</script>'); };
 try {const rows=await Promise.all(['a','b','c'].map(query=>searchOne(query,{slug:'당정동-4459',id:4459})));assert.equal(routeCalls,1);assert.equal(searchCalls,3);assert.equal(rows.length,3);}finally{global.fetch=old;}
});
