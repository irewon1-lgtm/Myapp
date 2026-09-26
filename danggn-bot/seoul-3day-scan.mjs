import fs from 'node:fs/promises';

const BASE='https://www.daangn.com';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const SINCE=new Date('2026-09-23T14:36:00+09:00').getTime();
const QUERIES=['노트북','그램','갤럭시북','ThinkPad'];
const TARGETS=[
 {name:'송파구', preferred:['잠실본동','잠실2동','가락본동','문정2동','방이2동','오금동','위례동','거여2동']},
 {name:'광진구', preferred:['자양1동','구의1동','광장동','화양동','중곡1동']},
 {name:'서초구', preferred:['서초1동','반포1동','방배본동','양재1동','잠원동']},
 {name:'강남구', preferred:['역삼1동','삼성1동','대치1동','압구정동','수서동']},
 {name:'강동구', preferred:['천호1동','성내1동','암사1동','명일1동','고덕1동']}
];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));

function balanced(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker,open){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf(open);if(s<0)return null;const j=balanced(t,s,open);if(!j)return null;try{return JSON.parse(j)}catch{return null}}
function aid(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function ts(v){if(v==null||v==='')return null;const d=new Date(typeof v==='number'&&v<1e12?v*1000:v);return Number.isFinite(d.getTime())?d.toISOString():null}
function norm(raw,q,rs){const id=aid(raw.href||raw.id)||'unknown';const url=raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):BASE+'/kr/buy-sell/'+id+'/';return{id,title:raw.title||'',price:num(raw.price),url,status:raw.status||'Ongoing',location:raw.locationName??raw.region?.name,regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' '),postedAt:ts(raw.createdAt??raw.created_at??raw.publishedAt??raw.published_at),boostedAt:ts(raw.boostedAt??raw.boosted_at),sourceQuery:q,sourceRegion:rs}}
function ld(html){const out=[];for(const m of html.matchAll(/<script[^>]+type=['"]application\/ld\+json['"][^>]*>([\s\S]*?)<\/script>/gi)){try{out.push(JSON.parse(m[1]))}catch{}}return out}
async function get(url,attempts=2){let err;for(let a=0;a<attempts;a++){try{const r=await fetch(url,{signal:AbortSignal.timeout(10000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok)throw new Error('HTTP '+r.status);return await r.text()}catch(e){err=e;if(a+1<attempts)await sleep(500)}}throw err}
async function resolve(t){const u=new URL(BASE+'/kr/api/v1/regions/keyword');u.searchParams.set('keyword','서울특별시 '+t.name);const data=JSON.parse(await get(u));const all=(data.locations||[]).filter(r=>Number(r.depth)===3&&r.name1==='서울특별시'&&r.name2===t.name&&/^\d+$/.test(String(r.id))).map(r=>({id:String(r.id),name:String(r.name3||r.name||'').trim(),slug:String(r.name3||r.name||'').trim()+'-'+r.id})).filter(r=>r.name);
 const by=new Map(all.map(r=>[r.name,r]));let pick=t.preferred.map(n=>by.get(n)).filter(Boolean);
 if(pick.length<3&&all.length){const step=Math.max(1,Math.floor(all.length/5));pick=[...pick,...all.filter((_,i)=>i%step===0)].slice(0,6)}
 const uniq=[];const seen=new Set();for(const r of pick){if(!seen.has(r.slug)){seen.add(r.slug);uniq.push(r)}}return {all,pick:uniq}}
async function searchOne(q,r){try{const u=new URL(BASE+'/kr/buy-sell/all/');u.searchParams.set('search',q);u.searchParams.set('in',r.slug);u.searchParams.set('_data','routes/kr.buy-sell._index');const data=JSON.parse(await get(u));if(data?.region?.id!=null&&String(data.region.id)!==r.id)throw new Error('region mismatch');const rows=data?.allPage?.fleamarketArticles;if(Array.isArray(rows)&&rows.length)return rows.map(x=>norm(x,q,r.slug)).slice(0,100)}catch{}
 const u=new URL(BASE+'/kr/buy-sell/');u.searchParams.set('search',q);u.searchParams.set('in',r.slug);const html=await get(u);const rows=after(html,'"fleamarketArticles":','[')||[];return Array.isArray(rows)?rows.map(x=>norm(x,q,r.slug)).slice(0,100):[]}
async function pool(tasks,n){const out=new Array(tasks.length);let i=0;async function w(){while(true){const x=i++;if(x>=tasks.length)return;try{out[x]=await tasks[x]()}catch(e){out[x]={__error:String(e)}}}}await Promise.all(Array.from({length:Math.min(n,tasks.length)},w));return out}
async function detail(item){try{const html=await get(item.url);const p=after(html,'"product":','{')??after(html,'\\\"product\\\":','{');if(p)return{...item,description:p.content||'',categoryName:p.category?.name,location:p.locationName??p.region?.name??item.location,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' ')||item.regionPath,status:p.status??item.status,price:num(p.price??item.price),postedAt:ts(p.createdAt??p.created_at??p.publishedAt??p.published_at)??item.postedAt,boostedAt:ts(p.boostedAt??p.boosted_at)??item.boostedAt};
 const j=ld(html).find(x=>x?.['@type']==='Product');return j?{...item,description:j.description||'',price:num(j.offers?.price??item.price)}:item}catch(e){return{...item,detailError:String(e)}}}
function specs(item){const text=((item.title||'')+' '+(item.description||'')).replace(/\s+/g,' ');
 const rm=text.match(/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i)?.[1]??text.match(/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i)?.[1]??text.match(/\b(16|32)\s*GB\b/i)?.[1]??null;
 const f=text.match(/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,40}?(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i)?.[1];
 const rev=text.match(/(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,40}?(?:SSD|NVMe|M\.?2)/i)?.[1];
 const generic=text.match(/\b(512\s*GB|1\s*TB|1024\s*GB)\b/i)?.[1]??null;const st=f??rev??generic;
 const mixed=/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,30}?256\s*(?:GB|G)[^.;,\n]{0,55}?(?:HDD|하드)[^.;,\n]{0,30}?(?:1\s*TB|1024\s*(?:GB|G))/i.test(text);
 return{ok:Boolean(rm&&st&&!mixed),ramGB:rm?Number(rm):null,storageGB:st?(/1\s*TB|1024/i.test(st)?1024:512):null}}
function cpu(text){const s=String(text||'').replace(/\s+/g,' ');return s.match(/\b(?:Intel\s*)?Core\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]??s.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]??s.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]??s.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]??null}
function district(item){const p=((item.regionPath||'')+' '+(item.location||''));return TARGETS.find(t=>p.includes(t.name))?.name??null}
function recent(i){if(!i.postedAt)return false;const x=new Date(i.postedAt).getTime();return Number.isFinite(x)&&x>=SINCE&&x<=Date.now()+300000}

const resolution=[];for(const t of TARGETS){try{resolution.push({target:t,...await resolve(t)})}catch(e){resolution.push({target:t,all:[],pick:[],error:String(e)})}}
const tasks=[];for(const rr of resolution)for(const r of rr.pick)for(const q of QUERIES)tasks.push(async()=>{try{const items=await searchOne(q,r);await sleep(160);return {district:rr.target.name,items}}catch(e){await sleep(700);throw e}});
const searched=await pool(tasks,1);const errs=searched.filter(x=>x?.__error);
const map=new Map();for(const batch of searched){if(!batch||batch.__error)continue;for(const i of batch.items){const k=i.id!=='unknown'?i.id:i.url;if(!map.has(k))map.set(k,i);else{const p=map.get(k);p.sourceQuery=[...new Set([].concat(p.sourceQuery||[],i.sourceQuery||[]))]}}}
let stage=[...map.values()].filter(i=>i.status==='Ongoing'&&i.price>=100000&&i.price<=1500000&&recent(i));
const detailed=await pool(stage.map(i=>()=>detail(i)),8);
const exact=detailed.filter(i=>i&&!i.__error&&!i.detailError&&i.status==='Ongoing'&&recent(i)&&district(i)&&specs(i).ok).map(i=>{const s=specs(i);return{id:i.id,title:i.title,price:i.price,url:i.url,district:district(i),location:i.location,regionPath:i.regionPath,postedAt:i.postedAt,boostedAt:i.boostedAt,cpuHint:cpu((i.title||'')+' '+(i.description||'')),ramGB:s.ramGB,storageGB:s.storageGB,description:String(i.description||'').slice(0,1400)}}).sort((a,b)=>new Date(b.postedAt)-new Date(a.postedAt)||a.price-b.price);
const result={generatedAt:new Date().toISOString(),since:new Date(SINCE).toISOString(),targets:TARGETS.map(x=>x.name),resolution:resolution.map(r=>({district:r.target.name,totalRegions:r.all.length,selected:r.pick.map(x=>x.slug),error:r.error||null})),stats:{searchTasks:tasks.length,searchErrors:errs.length,rawUnique:map.size,recentBeforeDetail:stage.length,exact:exact.length},items:exact};
console.log('SEOUL3D_STATS='+JSON.stringify(result.stats));\nconsole.log('SEOUL3D_ERRORS='+JSON.stringify(errs.slice(0,20)));
console.log('SEOUL3D_REGIONS='+JSON.stringify(result.resolution));
console.log('SEOUL3D_ITEMS='+JSON.stringify(exact));
await fs.writeFile('danggn-bot/seoul-3day-result.json',JSON.stringify(result,null,2));
