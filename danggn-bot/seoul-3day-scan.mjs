import fs from 'node:fs/promises';

const BASE='https://www.daangn.com';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const SINCE=new Date('2026-09-23T14:36:00+09:00').getTime();
const QUERIES=['노트북','그램','갤럭시북','ThinkPad'];
const TARGETS=[
 {name:'송파구', preferred:['잠실본동','잠실2동','가락본동','문정2동','방이2동','오금동']},
 {name:'광진구', preferred:['광장동','화양동','자양제4동','자양제3동']},
 {name:'서초구', preferred:['서초1동','반포1동','방배본동','양재1동','잠원동']},
 {name:'강남구', preferred:['역삼1동','삼성1동','대치1동','압구정동','수서동']},
 {name:'강동구', preferred:['천호동','강일동','명일동','암사제1동','고덕제1동']}
];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));

function balanced(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker,open){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf(open);if(s<0)return null;const j=balanced(t,s,open);if(!j)return null;try{return JSON.parse(j)}catch{return null}}
function aid(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function ts(v){if(v==null||v==='')return null;if(typeof v==='number'&&v<1e12)v*=1000;const d=new Date(v);return Number.isFinite(d.getTime())?d.toISOString():null}
function norm(raw,q,rs){const id=aid(raw.href||raw.id)||'unknown';const url=raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):BASE+'/kr/buy-sell/'+id+'/';return{id,title:raw.title||'',price:num(raw.price),url,status:raw.status||'Ongoing',location:raw.locationName??raw.region?.name,regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' '),postedAt:ts(raw.createdAt??raw.created_at??raw.publishedAt??raw.published_at),boostedAt:ts(raw.boostedAt??raw.boosted_at),sourceQuery:q,sourceRegion:rs}}
function parseLd(html){const out=[];for(const m of html.matchAll(/<script[^>]+type=['"]application\/ld\+json['"][^>]*>([\s\S]*?)<\/script>/gi)){try{out.push(JSON.parse(m[1]))}catch{}}return out}
function parseHtml(html,q,rs){const embedded=after(html,'"fleamarketArticles":','[')||[];if(Array.isArray(embedded)&&embedded.length)return embedded.map(x=>norm(x,q,rs));for(const block of parseLd(html)){if(block?.['@type']!=='ItemList'||!Array.isArray(block.itemListElement))continue;return block.itemListElement.map(x=>x?.item).filter(Boolean).map(item=>({id:aid(item.url)||'unknown',title:item.name||'',price:num(item.offers?.price),url:item.url,status:'Ongoing',sourceQuery:q,sourceRegion:rs}))}return []}
async function get(url,attempts=2){let err;for(let a=0;a<attempts;a++){try{const r=await fetch(url,{signal:AbortSignal.timeout(12000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok&&r.status!==204)throw new Error('HTTP '+r.status);return await r.text()}catch(e){err=e;if(a+1<attempts)await sleep(700)}}throw err}
async function resolve(t){const u=new URL(BASE+'/kr/api/v1/regions/keyword');u.searchParams.set('keyword','서울특별시 '+t.name);const data=JSON.parse(await get(u));const all=(data.locations||[]).filter(r=>Number(r.depth)===3&&r.name1==='서울특별시'&&r.name2===t.name&&/^\d+$/.test(String(r.id))).map(r=>({id:String(r.id),name:String(r.name3||r.name||'').trim(),slug:String(r.name3||r.name||'').trim()+'-'+r.id})).filter(r=>r.name);const by=new Map(all.map(r=>[r.name,r]));return t.preferred.map(n=>by.get(n)).filter(Boolean)}
async function searchOne(q,r){try{const u=new URL(BASE+'/kr/buy-sell/all/');u.searchParams.set('search',q);u.searchParams.set('in',r.slug);u.searchParams.set('_data','routes/kr.buy-sell._index');const txt=await get(u);if(txt){const data=JSON.parse(txt);const rows=data?.allPage?.fleamarketArticles;if(Array.isArray(rows)&&rows.length)return rows.map(x=>norm(x,q,r.slug)).slice(0,100)}}catch{}const h=new URL(BASE+'/kr/buy-sell/');h.searchParams.set('search',q);h.searchParams.set('in',r.slug);return parseHtml(await get(h),q,r.slug).slice(0,100)}
async function pool(tasks,n){const out=new Array(tasks.length);let i=0;async function w(){while(true){const x=i++;if(x>=tasks.length)return;try{out[x]=await tasks[x]()}catch(e){out[x]={__error:String(e)}}}}await Promise.all(Array.from({length:Math.min(n,tasks.length)},w));return out}
async function detail(i){try{const html=await get(i.url);const p=after(html,'"product":','{')??after(html,'\\\"product\\\":','{');if(p)return{...i,description:p.content||'',location:p.locationName??p.region?.name??i.location,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' ')||i.regionPath,status:p.status??i.status,price:num(p.price??i.price),postedAt:ts(p.createdAt??p.created_at??p.publishedAt??p.published_at)??i.postedAt,boostedAt:ts(p.boostedAt??p.boosted_at)??i.boostedAt};const j=parseLd(html).find(x=>x?.['@type']==='Product');return j?{...i,description:j.description||'',price:num(j.offers?.price??i.price),postedAt:ts(j.dateCreated??j.datePublished)??i.postedAt}:i}catch(e){return{...i,detailError:String(e)}}}
function specs(i){const t=((i.title||'')+' '+(i.description||'')).replace(/\s+/g,' ');const ram=t.match(/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i)?.[1]??t.match(/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i)?.[1]??t.match(/\b(16|32)\s*GB\b/i)?.[1]??null;const s=t.match(/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,40}?(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i)?.[1]??t.match(/(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,40}?(?:SSD|NVMe|M\.?2)/i)?.[1]??t.match(/\b(512\s*GB|1\s*TB|1024\s*GB)\b/i)?.[1]??null;const mixed=/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,30}?256\s*(?:GB|G)[^.;,\n]{0,55}?(?:HDD|하드)[^.;,\n]{0,30}?(?:1\s*TB|1024\s*(?:GB|G))/i.test(t);return{ok:Boolean(ram&&s&&!mixed),ramGB:ram?Number(ram):null,storageGB:s?(/1\s*TB|1024/i.test(s)?1024:512):null}}
function cpu(t){const s=String(t||'').replace(/\s+/g,' ');return s.match(/\b(?:Intel\s*)?Core\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]??s.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]??s.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]??s.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]??null}
function district(i){const p=((i.regionPath||'')+' '+(i.location||''));return TARGETS.find(t=>p.includes(t.name))?.name??null}
function recent(i){if(!i.postedAt)return false;const x=new Date(i.postedAt).getTime();return Number.isFinite(x)&&x>=SINCE&&x<=Date.now()+300000}

const regions=[];for(const t of TARGETS){regions.push({district:t.name,regions:await resolve(t)})}
const tasks=[];for(const d of regions)for(const r of d.regions)for(const q of QUERIES)tasks.push(async()=>{const items=await searchOne(q,r);await sleep(180);return items});
const batches=await pool(tasks,2);const errors=batches.filter(x=>x?.__error);const map=new Map();for(const b of batches){if(!Array.isArray(b))continue;for(const i of b){const k=i.id!=='unknown'?i.id:i.url;if(!map.has(k))map.set(k,i);else{const p=map.get(k);p.sourceQuery=[...new Set([].concat(p.sourceQuery||[],i.sourceQuery||[]))]}}}
let base=[...map.values()].filter(i=>i.status==='Ongoing'&&i.price>=100000&&i.price<=1500000);
const strongTitle=base.filter(i=>/(16|32)\s*(GB|G)|512\s*(GB|G)|1\s*TB|1024\s*(GB|G)/i.test(i.title||''));
const weakTitle=base.filter(i=>!strongTitle.includes(i));
const queue=[...strongTitle,...weakTitle].slice(0,500);
const detailed=await pool(queue.map(i=>async()=>{const d=await detail(i);await sleep(160);return d}),4);
const exact=detailed.filter(i=>i&&!i.__error&&!i.detailError&&i.status==='Ongoing'&&recent(i)&&district(i)&&specs(i).ok).map(i=>{const s=specs(i);return{id:i.id,title:i.title,price:i.price,url:i.url,district:district(i),location:i.location,regionPath:i.regionPath,postedAt:i.postedAt,boostedAt:i.boostedAt,cpuHint:cpu((i.title||'')+' '+(i.description||'')),ramGB:s.ramGB,storageGB:s.storageGB,description:String(i.description||'').slice(0,1600)}}).sort((a,b)=>new Date(b.postedAt)-new Date(a.postedAt)||a.price-b.price);
const result={generatedAt:new Date().toISOString(),since:new Date(SINCE).toISOString(),regions:regions.map(x=>({district:x.district,selected:x.regions.map(r=>r.slug)})),stats:{searchTasks:tasks.length,searchErrors:errors.length,rawUnique:map.size,detailQueue:queue.length,detailErrors:detailed.filter(x=>x?.detailError||x?.__error).length,exact:exact.length},items:exact};
console.log('SEOUL3D_STATS='+JSON.stringify(result.stats));
console.log('SEOUL3D_ITEMS='+JSON.stringify(exact));
await fs.writeFile('danggn-bot/seoul-3day-result.json',JSON.stringify(result,null,2));
