import fs from 'node:fs/promises';

const BASE='https://www.daangn.com';
const SEARCH=BASE+'/kr/buy-sell/';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const REGION='잠실동-6188';
const queries=['노트북','갤럭시북','그램','ThinkPad','레노버 노트북','HP 노트북','ASUS 노트북','델 노트북','삼성 노트북','맥북'];
const since=new Date('2026-09-11T00:00:00+09:00').getTime();

function balanced(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker,open){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf(open);if(s<0)return null;const j=balanced(t,s,open);if(!j)return null;try{return JSON.parse(j)}catch{return null}}
function aid(v){const s=String(v||'');return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?/i)?.[1]??null}
function num(v){const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,''));return Number.isFinite(n)?n:0}
function norm(raw,q){const id=aid(raw.href||raw.id)||'unknown';const url=raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):SEARCH+id+'/';return{id,title:raw.title||'',price:num(raw.price),url,status:raw.status||'Ongoing',location:raw.locationName??raw.region?.name,regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' '),postedAt:raw.createdAt,boostedAt:raw.boostedAt,sourceQuery:q}}
async function get(url){const r=await fetch(url,{signal:AbortSignal.timeout(7000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml','Accept-Language':'ko-KR,ko;q=0.9'}});if(!r.ok)throw new Error('HTTP '+r.status);return await r.text()}
function parseSearch(html,q){const a=after(html,'"fleamarketArticles":','[')??[];return Array.isArray(a)?a.map(x=>norm(x,q)):[]}
async function pool(tasks,limit){const out=new Array(tasks.length);let i=0;async function w(){while(true){const x=i++;if(x>=tasks.length)return;try{out[x]=await tasks[x]()}catch(e){out[x]={__error:String(e)}}}}await Promise.all(Array.from({length:Math.min(limit,tasks.length)},w));return out}
function local(p=''){return /서울특별시 (송파구|강남구|강동구|광진구|성동구|서초구)|경기도 성남시 수정구|경기도 하남시/.test(p)}
function exact(text){const t=String(text||'').replace(/\s+/g,' ');const ram=/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b|\b(16|32)\s*GB\s*(?:RAM|메모리|램)\b/i.test(t);const ssd=/(?:SSD|NVMe|M\.?2)[^\n]{0,35}(?:512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))|(?:512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^\n]{0,35}(?:SSD|NVMe|M\.?2)/i.test(t);return ram&&ssd}
async function detail(item){try{const h=await get(item.url);const p=after(h,'"product":','{')??after(h,'\\"product\\":','{');if(!p)return item;return{...item,description:p.content||'',categoryName:p.category?.name,location:p.locationName??p.region?.name,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' '),postedAt:p.createdAt,boostedAt:p.boostedAt,status:p.status??item.status,sellerName:p.user?.nickname,mannerTemperature:p.user?.score,viewCount:p.viewCount,sellerReviewCount:p.user?.reviewCount}}catch(e){return{...item,detailError:String(e)}}}
function score(x){const t=(x.title+' '+(x.description||'')).toLowerCase();let s=0;if(/32\s*gb/.test(t))s+=18;else if(/16\s*gb/.test(t))s+=10;if(/1\s*tb/.test(t))s+=12;else if(/512\s*(gb|g)/.test(t))s+=7;if(/i[357]-?1[345]\d{2}|core\s*ultra|ryzen\s*[3579]\s*[6789]\d{3}/i.test(t))s+=26;else if(/i[357]-?12\d{2}|ryzen\s*[3579]\s*5\d{3}/i.test(t))s+=19;else if(/i[357]-?11\d{2}|ryzen\s*[3579]\s*4\d{3}/i.test(t))s+=9;if(/그램|galaxy\s*book|갤럭시북|thinkpad|씽크패드|ideapad|vivobook|zenbook|pavilion|elitebook|latitude|맥북/i.test(t))s+=5;if(/n150|n100|celeron|셀러론|pentium|펜티엄/i.test(t))s-=25;if(/부품용|고장|불량|깨짐|액정.*문제/i.test(t))s-=35;const p=x.price||0;if(p>0&&p<=450000)s+=23;else if(p<=550000)s+=19;else if(p<=650000)s+=15;else if(p<=800000)s+=10;else if(p<=1000000)s+=5;else s-=3;if(/rtx\s*(3060|3070|3080|4060|4070)/i.test(t))s-=5;return s}

const sr=await pool(queries.map(q=>async()=>{const u=new URL(SEARCH);u.searchParams.set('search',q);u.searchParams.set('in',REGION);const h=await get(u);return parseSearch(h,q).slice(0,300)}),5);
const all=sr.flatMap(x=>Array.isArray(x)?x:[]);
const map=new Map();for(const x of all){if(!map.has(x.id))map.set(x.id,x);else{const p=map.get(x.id);p.sourceQuery=[...new Set([].concat(p.sourceQuery||[],x.sourceQuery||[]))]}}
const unique=[...map.values()].filter(x=>x.status==='Ongoing'&&x.price>=100000&&x.price<=1500000);
const localSummary=unique.filter(x=>local(x.regionPath||''));
const noRegion=unique.filter(x=>!x.regionPath).slice(0,80);
const toDetail=[...localSummary,...noRegion].slice(0,240);
const detailed=(await pool(toDetail.map(x=>()=>detail(x)),8)).filter(x=>x&&!x.__error);
const exacts=detailed.filter(x=>local(x.regionPath||'')&&x.status==='Ongoing'&&x.postedAt&&new Date(x.postedAt).getTime()>=since&&exact(x.title+' '+(x.description||''))).map(x=>({...x,score:score(x)})).sort((a,b)=>b.score-a.score||a.price-b.price);
console.log('JAMSIL_MAX_STATS='+JSON.stringify({raw:all.length,unique:unique.length,localSummary:localSummary.length,noRegion:noRegion.length,detailed:detailed.length,exact:exacts.length,errors:sr.filter(x=>x&&x.__error).length}));
console.log('JAMSIL_MAX_CANDIDATES='+JSON.stringify(exacts.slice(0,50)));
await fs.writeFile('danggn-bot/jamsil-max-result.json',JSON.stringify({generatedAt:new Date().toISOString(),items:exacts},null,2));
