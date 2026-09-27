import fs from 'node:fs/promises';
const BASE='https://www.daangn.com',SEARCH=BASE+'/kr/buy-sell/';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146 Safari/537.36';
const Q=['노트북','그램','갤럭시북','ThinkPad'];
const AREAS=[
 {name:'송파구',province:'서울특별시'},
 {name:'광진구',province:'서울특별시'},
 {name:'강동구',province:'서울특별시'},
 {name:'성남시',province:'경기도'}
];
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function bal(s,start,o){const c=o==='{'?'}':']';let d=0,q=false,e=false;for(let i=start;i<s.length;i++){const x=s[i];if(q){if(e){e=false;continue}if(x==='\\'){e=true;continue}if(x==='"')q=false;continue}if(x==='"'){q=true;continue}if(x===o)d++;if(x===c&&--d===0)return s.slice(start,i+1)}return null}
function after(h,m,o){const k=h.indexOf(m);if(k<0)return null;const t=h.slice(k+m.length),i=t.indexOf(o);if(i<0)return null;const raw=bal(t,i,o);try{return raw?JSON.parse(raw):null}catch{return null}}
function aid(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function nts(v){if(v==null||v==='')return null;const d=new Date(typeof v==='number'&&v<1e12?v*1000:v);return Number.isFinite(d.getTime())?d.toISOString():null}
function first(o,ks){for(const k of ks){const v=nts(o?.[k]);if(v)return v}return null}
async function get(url,tries=2){let e;for(let i=0;i<tries;i++){try{const r=await fetch(url,{signal:AbortSignal.timeout(10000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok)throw new Error('HTTP '+r.status);return await r.text()}catch(x){e=x;if(i+1<tries)await sleep(500)}}throw e}
async function pool(tasks,n){const out=new Array(tasks.length);let i=0;async function w(){while(true){const j=i++;if(j>=tasks.length)return;try{out[j]=await tasks[j]()}catch(e){out[j]={__error:String(e)}}await sleep(120)}}await Promise.all(Array.from({length:Math.min(n,tasks.length)},w));return out}
async function resolve(a){const u=new URL(BASE+'/kr/api/v1/regions/keyword');u.searchParams.set('keyword',a.province+' '+a.name);const d=JSON.parse(await get(u));return(d.locations||[]).filter(r=>Number(r.depth)===3&&r.name1===a.province&&typeof r.name2==='string'&&(a.name==='성남시'?r.name2.startsWith('성남시'):r.name2===a.name)&&/^\d{1,8}$/.test(String(r.id))).map(r=>({id:String(r.id),name:String(r.name3||r.name||''),slug:String(r.name3||r.name||'')+'-'+String(r.id)}))}
function norm(raw,q,r,a){const id=aid(raw.href||raw.id)||'unknown';return{id,title:raw.title||'',price:num(raw.price),url:raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):SEARCH+id+'/',location:raw.locationName??raw.region?.name,regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' '),postedAt:first(raw,['createdAt','created_at','publishedAt','published_at']),boostedAt:first(raw,['boostedAt','boosted_at','bumpedAt','bumped_at']),status:raw.status||'Ongoing',sourceQuery:q,sourceRegion:r.slug,area:a.name}}
async function search(q,r,a){try{const u=new URL(BASE+'/kr/buy-sell/all/');u.searchParams.set('search',q);u.searchParams.set('in',r.slug);u.searchParams.set('_data','routes/kr.buy-sell._index');const d=JSON.parse(await get(u));const rows=d?.allPage?.fleamarketArticles;if(Array.isArray(rows)&&rows.length)return rows.map(x=>norm(x,q,r,a)).slice(0,80)}catch{}const u=new URL(SEARCH);u.searchParams.set('search',q);u.searchParams.set('in',r.slug);const h=await get(u);const rows=after(h,'"fleamarketArticles":','[')||[];return Array.isArray(rows)?rows.map(x=>norm(x,q,r,a)).slice(0,80):[]}
async function detail(x){try{const h=await get(x.url);const p=after(h,'"product":','{')??after(h,'\\\"product\\\":','{');if(!p)return x;return{...x,description:p.content||'',location:p.locationName??p.region?.name??x.location,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' ')||x.regionPath,postedAt:first(p,['createdAt','created_at','publishedAt','published_at'])??x.postedAt,boostedAt:first(p,['boostedAt','boosted_at','bumpedAt','bumped_at'])??x.boostedAt,status:p.status??x.status,price:num(p.price??x.price),sellerName:p.user?.nickname,mannerTemperature:p.user?.score}}catch(e){return{...x,detailError:String(e)}}}
function spec(x){const t=((x.title||'')+' '+(x.description||'')).replace(/\s+/g,' ');const ram=t.match(/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i)?.[1]??t.match(/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i)?.[1]??t.match(/\b(16|32)\s*GB\b/i)?.[1]??null;const st=t.match(/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,40}?(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i)?.[1]??t.match(/(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,40}?(?:SSD|NVMe|M\.?2)/i)?.[1]??null;const mixed=/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,30}?256\s*(?:GB|G)[^.;,\n]{0,60}?(?:HDD|하드)[^.;,\n]{0,30}?(?:1\s*TB|1024)/i.test(t);return{ok:Boolean(ram&&st&&!mixed),ramGB:ram?Number(ram):null,storageGB:st?(/1\s*TB|1024/i.test(st)?1024:512):null}}
function cpu(x){const t=((x.title||'')+' '+(x.description||'')).replace(/\s+/g,' ');return t.match(/\bCore\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]??t.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]??t.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]??t.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]??null}
function areaOk(x){const p=(x.regionPath||'')+' '+(x.location||'');return x.area==='성남시'?p.includes('성남시'):p.includes(x.area)}
const now=Date.now(),cut=now-6*3600*1000,stats=[],final=[];
for(const a of AREAS){
 const rs=await resolve(a);const tasks=[];for(const r of rs)for(const q of Q)tasks.push(()=>search(q,r,a));
 const sr=await pool(tasks,3);const map=new Map();
 for(const x of sr.flatMap(v=>Array.isArray(v)?v:[])){if(!map.has(x.id))map.set(x.id,x);else{const p=map.get(x.id);if(!p.postedAt&&x.postedAt)p.postedAt=x.postedAt;if(!p.boostedAt&&x.boostedAt)p.boostedAt=x.boostedAt}}
 const all=[...map.values()].filter(x=>x.status==='Ongoing'&&x.price>=100000&&x.price<=1500000);
 const q=all.filter(x=>{const pt=x.postedAt?Date.parse(x.postedAt):NaN,bt=x.boostedAt?Date.parse(x.boostedAt):NaN;return(Number.isFinite(pt)&&pt>=cut&&pt<=now+5*60000)||(Number.isFinite(bt)&&bt>=cut&&bt<=now+5*60000)});
 q.sort((a,b)=>Date.parse(b.postedAt||b.boostedAt||0)-Date.parse(a.postedAt||a.boostedAt||0));
 const det=await pool(q.slice(0,220).map(x=>()=>detail(x)),4);const good=[];
 for(const x of det){if(!x||x.__error||x.detailError||x.status!=='Ongoing'||!areaOk(x)||!x.postedAt)continue;const pt=Date.parse(x.postedAt);if(!Number.isFinite(pt)||pt<cut||pt>now+5*60000)continue;const sp=spec(x);if(!sp.ok)continue;good.push({...x,...sp,cpuHint:cpu(x)})}
 final.push(...good);stats.push({area:a.name,regionCount:rs.length,attempted:tasks.length,searchErrors:sr.filter(x=>x?.__error).length,rawUnique:map.size,recentOrBoostedQueue:q.length,detailed:det.length,matched:good.length});
}
const ded=new Map();for(const x of final)if(!ded.has(x.id))ded.set(x.id,x);
const out={generatedAt:new Date().toISOString(),windowHours:6,stats,candidates:[...ded.values()]};
await fs.writeFile('danggn-bot/once-sgks-all-result.json',JSON.stringify(out,null,2));
console.log(JSON.stringify(out));