import fs from 'node:fs/promises';

const BASE='https://www.daangn.com';
const SEARCH=BASE+'/kr/buy-sell/';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const regions=["정자동-1339","수내2동-1337","야탑3동-1349","판교동-1350","신흥2동-1307","복정동-1318","은행2동-1328"];
const queries=["노트북 16GB 512GB","노트북 16GB 1TB","노트북 32GB 512GB","노트북 32GB 1TB"];
const since=new Date("2026-09-11T00:00:00+09:00").getTime();

function balanced(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker,open){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf(open);if(s<0)return null;const j=balanced(t,s,open);if(!j)return null;try{return JSON.parse(j)}catch{return null}}
function aid(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function norm(raw,q,rs){const id=aid(raw.href||raw.id)||'unknown';const url=raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):SEARCH+id+'/';return{id,title:raw.title||'',price:num(raw.price),url,status:raw.status||'Ongoing',sourceQuery:q,sourceRegionSlug:rs}}
async function get(url){const r=await fetch(url,{signal:AbortSignal.timeout(6500),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok)throw new Error('HTTP '+r.status);return await r.text()}
function parseSearch(html,q,rs){const a=after(html,'"fleamarketArticles":','[')??[];return Array.isArray(a)?a.map(x=>norm(x,q,rs)):[]}
async function pool(tasks,limit){const out=new Array(tasks.length);let i=0;async function w(){while(true){const x=i++;if(x>=tasks.length)return;try{out[x]=await tasks[x]()}catch(e){out[x]={__error:String(e)}}}}await Promise.all(Array.from({length:Math.min(limit,tasks.length)},w));return out}
function exactText(text){const t=String(text||'').replace(/\s+/g,' ');const ram=/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b|\b(16|32)\s*GB\s*(?:RAM|메모리|램)\b/i.test(t);const ssd=/(?:SSD|NVMe|M\.?2)[^\n]{0,30}(?:512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))|(?:512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^\n]{0,30}(?:SSD|NVMe|M\.?2)/i.test(t);return ram&&ssd}
function likelyTitle(t){return /(16|32)\s*(GB|G)\b/i.test(t)&&/(512\s*(GB|G)|1\s*TB|1024\s*(GB|G))/i.test(t)}
async function detail(item){try{const h=await get(item.url);const p=after(h,'"product":','{')??after(h,'\\"product\\":','{');if(!p)return item;return{...item,description:p.content||'',categoryName:p.category?.name,location:p.locationName??p.region?.name,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' '),postedAt:p.createdAt,boostedAt:p.boostedAt,status:p.status??item.status,sellerName:p.user?.nickname,mannerTemperature:p.user?.score,viewCount:p.viewCount}}catch(e){return{...item,detailError:String(e)}}}
function score(x){const t=(x.title+' '+(x.description||'')).toLowerCase();let s=0;if(/32\s*gb/.test(t))s+=18;else if(/16\s*gb/.test(t))s+=10;if(/1\s*tb/.test(t))s+=12;else if(/512\s*(gb|g)/.test(t))s+=7;if(/i[357]-?1[345]\d{2}|core\s*ultra|ryzen\s*[3579]\s*[6789]\d{3}/i.test(t))s+=25;else if(/i[357]-?12\d{2}|ryzen\s*[3579]\s*5\d{3}/i.test(t))s+=18;else if(/i[357]-?11\d{2}|ryzen\s*[3579]\s*4\d{3}/i.test(t))s+=8;if(/그램|galaxy\s*book|갤럭시북|thinkpad|씽크패드|ideapad|vivobook|zenbook|pavilion|elitebook|latitude/i.test(t))s+=5;if(/부품용|고장|불량|깨짐|액정.*문제/i.test(t))s-=30;const p=x.price||0;if(p>0&&p<=500000)s+=20;else if(p<=650000)s+=15;else if(p<=800000)s+=10;else if(p<=1000000)s+=5;else s-=2;return s}

const searchTasks=[];
for(const rs of regions)for(const q of queries)searchTasks.push(async()=>{const u=new URL(SEARCH);u.searchParams.set('search',q);u.searchParams.set('in',rs);const h=await get(u);return parseSearch(h,q,rs).slice(0,35)});
const sr=await pool(searchTasks,6);
const all=sr.flatMap(x=>Array.isArray(x)?x:[]);
const map=new Map();for(const x of all){if(!map.has(x.id))map.set(x.id,x)}
const candidates=[...map.values()].filter(x=>x.status==='Ongoing'&&x.price>=100000&&x.price<=1500000&&likelyTitle(x.title)).slice(0,70);
const detailed=(await pool(candidates.map(x=>()=>detail(x)),6)).filter(x=>x&&!x.__error);
const exact=detailed.filter(x=>((x.regionPath||'').includes('성남시'))&&x.postedAt&&new Date(x.postedAt).getTime()>=since&&x.status==='Ongoing'&&exactText(x.title+' '+(x.description||''))).map(x=>({...x,score:score(x)})).sort((a,b)=>b.score-a.score||a.price-b.price);
console.log('SEONGNAM_SCAN_STATS='+JSON.stringify({raw:all.length,unique:map.size,likely:candidates.length,detailed:detailed.length,exact:exact.length,errors:sr.filter(x=>x&&x.__error).length}));
console.log('SEONGNAM_CANDIDATES_JSON='+JSON.stringify(exact));
await fs.writeFile('danggn-bot/seongnam-result.json',JSON.stringify({generatedAt:new Date().toISOString(),stats:{raw:all.length,unique:map.size,likely:candidates.length,detailed:detailed.length,exact:exact.length},items:exact},null,2));
