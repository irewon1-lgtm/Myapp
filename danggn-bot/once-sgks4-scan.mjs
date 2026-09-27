import fs from 'node:fs/promises';
const BASE='https://www.daangn.com', SEARCH=BASE+'/kr/buy-sell/';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146 Safari/537.36';
const QUERIES=['노트북 16GB 512GB','노트북 16GB 1TB','노트북 32GB 512GB','노트북 32GB 1TB'];
const AREAS=[
 {name:'송파구',province:'서울특별시',regions:['잠실동','문정동']},
 {name:'광진구',province:'서울특별시',regions:['자양동','중곡동']},
 {name:'강동구',province:'서울특별시',regions:['천호동','고덕동']},
 {name:'성남시',province:'경기도',regions:['정자동','판교동','신흥2동','복정동']}
];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function bal(s,start,o){const c=o==='{'?'}':']';let d=0,q=false,e=false;for(let i=start;i<s.length;i++){const x=s[i];if(q){if(e){e=false;continue}if(x==='\\'){e=true;continue}if(x==='"')q=false;continue}if(x==='"'){q=true;continue}if(x===o)d++;if(x===c&&--d===0)return s.slice(start,i+1)}return null}
function after(h,m,o){const k=h.indexOf(m);if(k<0)return null;const t=h.slice(k+m.length),s=t.indexOf(o);if(s<0)return null;const raw=bal(t,s,o);try{return raw?JSON.parse(raw):null}catch{return null}}
function aid(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function nts(v){if(v==null||v==='')return null;const d=new Date(typeof v==='number'&&v<1e12?v*1000:v);return Number.isFinite(d.getTime())?d.toISOString():null}
function first(o,ks){for(const k of ks){const v=nts(o?.[k]);if(v)return v}return null}
async function get(url,tries=2){let e;for(let i=0;i<tries;i++){try{const r=await fetch(url,{signal:AbortSignal.timeout(10000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok)throw new Error('HTTP '+r.status);return await r.text()}catch(x){e=x;if(i+1<tries)await sleep(600)}}throw e}
async function pool(tasks,n){const out=new Array(tasks.length);let i=0;async function w(){while(true){const j=i++;if(j>=tasks.length)return;try{out[j]=await tasks[j]()}catch(e){out[j]={__error:String(e)}}await sleep(180)}}await Promise.all(Array.from({length:Math.min(n,tasks.length)},w));return out}
async function resolve(a){const u=new URL(BASE+'/kr/api/v1/regions/keyword');u.searchParams.set('keyword',a.province+' '+a.name);const d=JSON.parse(await get(u));const all=(d.locations||[]).filter(r=>Number(r.depth)===3&&r.name1===a.province&&typeof r.name2==='string'&&(a.name==='성남시'?r.name2.startsWith('성남시'):r.name2===a.name)).map(r=>({id:String(r.id),name:String(r.name3||r.name||''),slug:String(r.name3||r.name||'')+'-'+String(r.id)}));const by=new Map(all.map(r=>[r.name,r]));return a.regions.map(n=>by.get(n)).filter(Boolean)}
function norm(raw,q,r,a){const id=aid(raw.href||raw.id)||'unknown';return{id,title:raw.title||'',price:num(raw.price),url:raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):SEARCH+id+'/',location:raw.locationName??raw.region?.name,regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' '),postedAt:first(raw,['createdAt','created_at','publishedAt','published_at']),boostedAt:first(raw,['boostedAt','boosted_at','bumpedAt','bumped_at']),status:raw.status||'Ongoing',sourceQuery:q,sourceRegion:r.slug,area:a.name}}
async function search(q,r,a){try{const u=new URL(BASE+'/kr/buy-sell/all/');u.searchParams.set('search',q);u.searchParams.set('in',r.slug);u.searchParams.set('_data','routes/kr.buy-sell._index');const d=JSON.parse(await get(u));const rows=d?.allPage?.fleamarketArticles;if(Array.isArray(rows)&&rows.length)return rows.map(x=>norm(x,q,r,a)).slice(0,80)}catch{}const u=new URL(SEARCH);u.searchParams.set('search',q);u.searchParams.set('in',r.slug);const h=await get(u);const rows=after(h,'"fleamarketArticles":','[')||[];return Array.isArray(rows)?rows.map(x=>norm(x,q,r,a)).slice(0,80):[]}
async function detail(x){try{const h=await get(x.url);const p=after(h,'"product":','{')??after(h,'\\\"product\\\":','{');if(!p)return x;return{...x,description:p.content||'',location:p.locationName??p.region?.name??x.location,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' ')||x.regionPath,postedAt:first(p,['createdAt','created_at','publishedAt','published_at'])??x.postedAt,boostedAt:first(p,['boostedAt','boosted_at','bumpedAt','bumped_at'])??x.boostedAt,status:p.status??x.status,price:num(p.price??x.price),sellerName:p.user?.nickname,mannerTemperature:p.user?.score}}catch(e){return{...x,detailError:String(e)}}}
function spec(x){const t=((x.title||'')+' '+(x.description||'')).replace(/\s+/g,' ');const ram=t.match(/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i)?.[1]??t.match(/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i)?.[1]??t.match(/\b(16|32)\s*GB\b/i)?.[1]??null;const st=t.match(/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,40}?(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i)?.[1]??t.match(/(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,40}?(?:SSD|NVMe|M\.?2)/i)?.[1]??null;const mixed=/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,30}?256\s*(?:GB|G)[^.;,\n]{0,60}?(?:HDD|하드)[^.;,\n]{0,30}?(?:1\s*TB|1024)/i.test(t);return{ok:Boolean(ram&&st&&!mixed),ramGB:ram?Number(ram):null,storageGB:st?(/1\s*TB|1024/i.test(st)?1024:512):null}}
function cpu(x){const t=((x.title||'')+' '+(x.description||'')).replace(/\s+/g,' ');return t.match(/\bCore\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]??t.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]??t.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]??t.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]??null}
function areaOk(x){const p=(x.regionPath||'')+' '+(x.location||'');return x.area==='성남시'?p.includes('성남시'):p.includes(x.area)}
const now=Date.now(), cutoff=now-6*3600*1000, stats=[], candidates=[];
for(const a of AREAS){
 const rs=await resolve(a); const tasks=[]; for(const r of rs)for(const q of QUERIES)tasks.push(()=>search(q,r,a));
 const sr=await pool(tasks,3); const map=new Map(); for(const x of sr.flatMap(v=>Array.isArray(v)?v:[])){if(!map.has(x.id))map.set(x.id,x)}
 const base=[...map.values()].filter(x=>x.status==='Ongoing'&&x.price>=100000&&x.price<=1500000);
 const det=await pool(base.slice(0,160).map(x=>()=>detail(x)),4);
 const good=[];
 for(const x of det){if(!x||x.__error||x.detailError||x.status!=='Ongoing'||!areaOk(x)||!x.postedAt)continue;const pt=new Date(x.postedAt).getTime();if(!Number.isFinite(pt)||pt<cutoff||pt>now+5*60000)continue;const sp=spec(x);if(!sp.ok)continue;good.push({...x,...sp,cpuHint:cpu(x)})}
 candidates.push(...good); stats.push({area:a.name,regions:rs.map(x=>x.slug),rawUnique:map.size,detailed:det.length,matched:good.length,searchErrors:sr.filter(x=>x?.__error).length});
}
const ded=new Map();for(const x of candidates)if(!ded.has(x.id))ded.set(x.id,x);
const out={generatedAt:new Date().toISOString(),windowHours:6,areas:AREAS.map(x=>x.name),stats,candidates:[...ded.values()]};
await fs.writeFile('danggn-bot/once-sgks4-result.json',JSON.stringify(out,null,2));
console.log(JSON.stringify(out));