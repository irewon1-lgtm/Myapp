import fs from 'node:fs/promises';

const BASE='https://www.daangn.com';
const SEARCH=BASE+'/kr/buy-sell/';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146 Safari/537.36';
const QUERIES=['노트북','그램','갤럭시북','ThinkPad'];
const WINDOW_MS=6*60*60*1000;
const AREAS=[
  {name:'송파구', province:'서울특별시', keyword:'송파구', regionNames:['잠실동','방이동','가락동','문정동','거여동']},
  {name:'광진구', province:'서울특별시', keyword:'광진구', regionNames:['자양동','구의동','중곡동','광장동']},
  {name:'강동구', province:'서울특별시', keyword:'강동구', regionNames:['천호동','길동','암사동','명일동','고덕동']},
  {name:'성남시', province:'경기도', keyword:'성남시', regionNames:['정자동','수내2동','야탑3동','판교동','신흥2동','복정동','은행2동']}
];

const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function balanced(s,i,o){const c=o==='{'?'}':']';let d=0,q=false,e=false;for(;i<s.length;i++){const x=s[i];if(q){if(e){e=false;continue}if(x==='\\'){e=true;continue}if(x==='"')q=false;continue}if(x==='"'){q=true;continue}if(x===o)d++;if(x===c&&--d===0)return s.slice(arguments[1],i+1)}return null}
function after(html,m,o){const k=html.indexOf(m);if(k<0)return null;const t=html.slice(k+m.length),s=t.indexOf(o);if(s<0)return null;const raw=balanced(t,s,o);try{return raw?JSON.parse(raw):null}catch{return null}}
function id(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function ts(v){if(v==null||v==='')return null;const d=new Date(typeof v==='number'&&v<1e12?v*1000:v);return Number.isFinite(d.getTime())?d.toISOString():null}
function firstTs(o,ks){for(const k of ks){const v=ts(o?.[k]);if(v)return v}return null}
async function get(url,tries=2){let last;for(let a=0;a<tries;a++){try{const r=await fetch(url,{signal:AbortSignal.timeout(10000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok)throw new Error('HTTP '+r.status);return await r.text()}catch(e){last=e;if(a+1<tries)await sleep(500)}}throw last}
async function pool(tasks,n=8){const out=new Array(tasks.length);let i=0;async function w(){while(true){const j=i++;if(j>=tasks.length)return;try{out[j]=await tasks[j]()}catch(e){out[j]={__error:String(e)}}}}await Promise.all(Array.from({length:Math.min(n,tasks.length)},w));return out}
function norm(raw,q,region,area){const aid=id(raw.href||raw.id)||'unknown';return{id:aid,title:raw.title||'',price:num(raw.price),url:raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):SEARCH+aid+'/',location:raw.locationName??raw.region?.name,regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' '),postedAt:firstTs(raw,['createdAt','created_at','publishedAt','published_at']),boostedAt:firstTs(raw,['boostedAt','boosted_at','bumpedAt','bumped_at']),status:raw.status||'Ongoing',sourceQuery:q,sourceRegion:region.slug,area:area.name}}
async function resolve(area){const u=new URL(BASE+'/kr/api/v1/regions/keyword');u.searchParams.set('keyword',area.province+' '+area.keyword);const d=JSON.parse(await get(u));const rows=(d.locations||[]).filter(r=>Number(r.depth)===3&&r.name1===area.province&&typeof r.name2==='string'&&(area.name==='성남시'?r.name2.startsWith('성남시'):r.name2===area.name)).map(r=>({id:String(r.id),name:String(r.name3||r.name||''),slug:String(r.name3||r.name||'')+'-'+String(r.id)}));const by=new Map(rows.map(r=>[r.name,r]));const sel=area.regionNames.map(n=>by.get(n)).filter(Boolean);return sel.length?sel:rows.slice(0,Math.min(7,rows.length))}
async function search(q,region,area){try{const u=new URL(BASE+'/kr/buy-sell/all/');u.searchParams.set('search',q);u.searchParams.set('in',region.slug);u.searchParams.set('_data','routes/kr.buy-sell._index');const d=JSON.parse(await get(u));const rows=d?.allPage?.fleamarketArticles;if(Array.isArray(rows))return rows.map(x=>norm(x,q,region,area)).slice(0,80)}catch{}const u=new URL(SEARCH);u.searchParams.set('search',q);u.searchParams.set('in',region.slug);const h=await get(u);const rows=after(h,'"fleamarketArticles":','[')||[];return Array.isArray(rows)?rows.map(x=>norm(x,q,region,area)).slice(0,80):[]}
async function detail(x){try{const h=await get(x.url);const p=after(h,'"product":','{')??after(h,'\\\"product\\\":','{');if(!p)return x;return{...x,description:p.content||'',location:p.locationName??p.region?.name??x.location,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' ')||x.regionPath,postedAt:firstTs(p,['createdAt','created_at','publishedAt','published_at'])??x.postedAt,boostedAt:firstTs(p,['boostedAt','boosted_at','bumpedAt','bumped_at'])??x.boostedAt,status:p.status??x.status,price:num(p.price??x.price),sellerName:p.user?.nickname,mannerTemperature:p.user?.score}}catch(e){return{...x,detailError:String(e)}}}
function specs(x){const t=((x.title||'')+' '+(x.description||'')).replace(/\s+/g,' ');const ram=t.match(/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i)?.[1]??t.match(/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i)?.[1]??t.match(/\b(16|32)\s*GB\b/i)?.[1]??null;const st=t.match(/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,40}?(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i)?.[1]??t.match(/(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,40}?(?:SSD|NVMe|M\.?2)/i)?.[1]??null;const mixed=/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,30}?256\s*(?:GB|G)[^.;,\n]{0,60}?(?:HDD|하드)[^.;,\n]{0,30}?(?:1\s*TB|1024)/i.test(t);return{ok:Boolean(ram&&st&&!mixed),ramGB:ram?Number(ram):null,storageGB:st?(/1\s*TB|1024/i.test(st)?1024:512):null}}
function cpu(x){const t=((x.title||'')+' '+(x.description||'')).replace(/\s+/g,' ');return t.match(/\bCore\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]??t.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]??t.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]??t.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]??null}
function areaOk(x){const p=(x.regionPath||'')+' '+(x.location||'');return x.area==='성남시'?p.includes('성남시'):p.includes(x.area)}

const now=Date.now(), stats=[], final=[];
for(const area of AREAS){
  const regions=await resolve(area);
  const tasks=[];for(const r of regions)for(const q of QUERIES)tasks.push(()=>search(q,r,area));
  const sr=await pool(tasks,8);
  const map=new Map();
  for(const x of sr.flatMap(v=>Array.isArray(v)?v:[])){if(!map.has(x.id))map.set(x.id,x)}
  const recent=[...map.values()].filter(x=>x.status==='Ongoing'&&x.price>=100000&&x.price<=1500000&&x.postedAt&&new Date(x.postedAt).getTime()>=now-WINDOW_MS&&new Date(x.postedAt).getTime()<=now+5*60*1000);
  const det=await pool(recent.slice(0,140).map(x=>()=>detail(x)),8);
  const good=det.filter(x=>x&&!x.__error&&x.status==='Ongoing'&&areaOk(x)).map(x=>({...x,...specs(x),cpuHint:cpu(x)})).filter(x=>x.ok);
  final.push(...good);
  stats.push({area:area.name,regions:regions.map(r=>r.slug),rawUnique:map.size,recent:recent.length,detailed:det.length,matched:good.length,errors:sr.filter(x=>x?.__error).length});
}
const dedup=new Map();for(const x of final){if(!dedup.has(x.id))dedup.set(x.id,x)}
const out={generatedAt:new Date().toISOString(),windowHours:6,areas:AREAS.map(x=>x.name),stats,candidates:[...dedup.values()]};
await fs.writeFile('danggn-bot/once-sgks-result.json',JSON.stringify(out,null,2));
console.log(JSON.stringify(out));
