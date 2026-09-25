import fs from 'node:fs/promises';

const BASE='https://www.daangn.com';
const SEARCH=BASE+'/kr/buy-sell/';
const REGION_API=BASE+'/kr/api/v1/regions/keyword?keyword=';
const REGIONS=['군포시','의왕시','안양시','과천시'];
const QUERY='노트북';
const LOOKBACK_HOURS=6;
const STATE_PATH=new URL('./state.json',import.meta.url);
const OUT_DIR=new URL('./output/',import.meta.url);
const RESULT_PATH=new URL('./output/latest_candidates.json',import.meta.url);
const DIAG_PATH=new URL('./output/latest_diagnostics.json',import.meta.url);
const MD_PATH=new URL('./output/latest_scan.md',import.meta.url);
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));

function balanced(input,start,open){
  const close=open==='{'?'}':']'; let depth=0,inString=false,esc=false;
  for(let i=start;i<input.length;i++){
    const c=input[i];
    if(inString){if(esc){esc=false;continue} if(c==='\\'){esc=true;continue} if(c==='"')inString=false; continue}
    if(c==='"'){inString=true;continue}
    if(c===open)depth++;
    if(c===close){depth--;if(depth===0)return input.slice(start,i+1)}
  }
  return null;
}
function after(html,marker,open){
  const i=html.indexOf(marker); if(i<0)return null;
  const tail=html.slice(i+marker.length), s=tail.indexOf(open); if(s<0)return null;
  const j=balanced(tail,s,open); if(!j)return null;
  try{return JSON.parse(j)}catch{return null}
}
function idOf(v){
  const s=String(v||'').trim();
  if(/^[a-z0-9]+$/i.test(s))return s;
  return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??s.match(/buy-sell\/([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]??null;
}
function priceNum(v){
  if(typeof v==='number')return Number.isFinite(v)?v:0;
  const n=Number.parseFloat(String(v??'').replace(/[^0-9.-]/g,'')); return Number.isFinite(n)?n:0;
}
function normalize(raw,city,loc){
  const id=idOf(raw.href||raw.id)||'unknown';
  const url=raw.href?(String(raw.href).startsWith('http')?raw.href:BASE+raw.href):SEARCH+id+'/';
  return {
    id,title:raw.title??'제목 없음',price:priceNum(raw.price),url,
    location:raw.locationName??raw.region?.name,
    regionPath:[raw.region?.name1,raw.region?.name2,raw.region?.name3].filter(Boolean).join(' ')||undefined,
    postedAt:raw.createdAt,boostedAt:raw.boostedAt,status:raw.status??'Ongoing',
    city,searchLocation:loc
  };
}
function parseSearch(html,city,loc){
  const a=after(html,'"fleamarketArticles":','[')??[];
  return Array.isArray(a)?a.map(x=>normalize(x,city,loc)):[];
}
async function get(url,accept='text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'){
  const r=await fetch(url,{signal:AbortSignal.timeout(12000),headers:{'User-Agent':UA,'Accept':accept,'Accept-Language':'ko-KR,ko;q=0.9'}});
  if(!r.ok)throw new Error('HTTP '+r.status+' '+r.statusText+' '+url);
  return r;
}
async function cityLocations(city){
  const r=await get(REGION_API+encodeURIComponent(city),'application/json,text/plain,*/*');
  const d=await r.json(); const out=[],seen=new Set();
  for(const x of d?.locations??[]){
    const id=x?.id??x?.dbId, name=String(x?.name??'').trim(); if(!id||!name)continue;
    const slug=name+'-'+id; if(seen.has(slug))continue; seen.add(slug); out.push({name,id,slug});
  }
  return out;
}
async function searchOne(city,loc){
  const u=new URL(SEARCH); u.searchParams.set('search',QUERY); u.searchParams.set('in',loc.slug); u.searchParams.set('only_on_sale','true'); u.searchParams.set('sort','recent');
  const html=await (await get(u)).text(); return parseSearch(html,city,loc.name);
}
async function detailOne(item){
  try{
    const html=await (await get(item.url)).text();
    const p=after(html,'"product":','{')??after(html,'\\\"product\\\":','{');
    if(!p)return {...item,description:''};
    return {...item,description:p.content??'',status:p.status??item.status,postedAt:p.createdAt??item.postedAt,boostedAt:p.boostedAt??item.boostedAt,location:p.locationName??p.region?.name??item.location,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' ')||item.regionPath};
  }catch(e){return {...item,description:'',detailError:String(e)}}
}
function dt(v){const n=Date.parse(v??'');return Number.isFinite(n)?n:null}
function exactSpecs(item){
  const text=((item.title??'')+' '+(item.description??'')).replace(/\s+/g,' ');
  const ram=/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G|기가)?\b/i.exec(text)||/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i.exec(text);
  const compact=/\b(16|32)\s*(?:GB|G)?\s*[/,+]\s*(512\s*(?:GB|G)?|1\s*TB|1024\s*(?:GB|G)?)\b/i.exec(text);
  const storage='(?:512\\s*(?:GB|G)|1\\s*TB|1024\\s*(?:GB|G))';
  const ssd=new RegExp('(?:SSD|NVMe|M\\.?2)[^\\n]{0,28}'+storage,'i').exec(text)||new RegExp(storage+'[^\\n]{0,28}(?:SSD|NVMe|M\\.?2)','i').exec(text);
  const bad256=/(?:SSD|NVMe|M\.?2)[^\n]{0,20}256\s*(?:GB|G)/i.test(text)&&/(?:HDD|하드)[^\n]{0,20}1\s*TB/i.test(text)&&!ssd&&!compact;
  const ramGB=ram?Number(ram[1]):compact?Number(compact[1]):null;
  let ssdGB=null; const sm=(ssd?.[0]??compact?.[2]??'').match(/(512|1024|1\s*TB)/i);
  if(sm)ssdGB=/1\s*TB/i.test(sm[1])?1024:Number(sm[1]);
  const cpu=text.match(/\b(i[3579]-?\d{4,5}[a-z]{0,2}|core\s*ultra\s*[579]\s*\d{3}[a-z]{0,2}|ryzen\s*[3579]\s*\d{4}[a-z]{0,3}|m[1-5](?:\s*(?:pro|max|ultra))?)\b/i)?.[1]??'';
  return {ramGB,ssdGB,cpuText:cpu,excluded256SsdPlus1TbHdd:bad256,pass:(ramGB===16||ramGB===32)&&(ssdGB===512||ssdGB===1024)&&!bad256};
}
function inCity(item,city){const p=((item.regionPath??'')+' '+(item.location??'')).trim();return !p||p.includes(city)}
async function loadState(){try{return JSON.parse(await fs.readFile(STATE_PATH,'utf8'))}catch{return {version:2,lastScanAt:null,listings:{}}}}
async function saveJson(url,obj){await fs.mkdir(OUT_DIR,{recursive:true});await fs.writeFile(url,JSON.stringify(obj,null,2)+'\n')}

const started=new Date(), cutoff=started.getTime()-LOOKBACK_HOURS*3600_000, state=await loadState(), prev=state.listings??{};
const current={}, candidates=[], diag={scanStartedAt:started.toISOString(),cutoffAt:new Date(cutoff).toISOString(),regionOrder:REGIONS,cities:[],notes:[]};

for(const city of REGIONS){
  const stat={city,locations:0,raw:0,unique:0,newOrPriceDrop:0,detailsOpened:0,specPassed:0,errors:0};
  console.log('CITY_START='+city);
  let locs=[];
  try{locs=await cityLocations(city)}catch(e){stat.errors++;diag.notes.push(city+' region api: '+String(e))}
  stat.locations=locs.length; const map=new Map();
  for(const loc of locs){
    try{
      const items=await searchOne(city,loc); stat.raw+=items.length;
      for(const x of items){if(x.id!=='unknown'&&!map.has(x.id))map.set(x.id,x)}
    }catch(e){stat.errors++;console.log('SEARCH_ERROR='+JSON.stringify({city,loc:loc.name,error:String(e)}))}
    await sleep(120);
  }
  stat.unique=map.size;
  for(const [id,item] of map){
    if(!inCity(item,city))continue;
    current[id]={title:item.title,url:item.url,city,location:item.location,price:item.price,status:item.status,postedAt:item.postedAt,boostedAt:item.boostedAt,lastSeenAt:new Date().toISOString()};
    if(item.status!=='Ongoing'||!item.price)continue;
    const old=prev[id], oldPrice=Number(old?.price||0), posted=dt(item.postedAt);
    const isNew=!old&&posted!==null&&posted>=cutoff;
    const isDrop=!!oldPrice&&item.price<oldPrice;
    if(!isNew&&!isDrop)continue;
    stat.newOrPriceDrop++;
    const d=await detailOne(item); stat.detailsOpened++;
    if(d.status!=='Ongoing')continue;
    const sp=exactSpecs(d); if(!sp.pass)continue;
    stat.specPassed++;
    candidates.push({...d,reason:isDrop?'price_drop':'new_6h',previousPrice:isDrop?oldPrice:null,priceDropWon:isDrop?oldPrice-item.price:null,priceDropPct:isDrop?Number((((oldPrice-item.price)/oldPrice)*100).toFixed(1)):null,specs:sp});
    await sleep(180);
  }
  diag.cities.push(stat); console.log('CITY_DONE='+JSON.stringify(stat));
}

const merged={...prev,...current};
const prune=Date.now()-60*86400_000; for(const [k,v] of Object.entries(merged)){const t=dt(v?.lastSeenAt);if(t!==null&&t<prune)delete merged[k]}
await fs.writeFile(STATE_PATH,JSON.stringify({version:2,lastScanAt:new Date().toISOString(),listings:merged},null,2)+'\n');
const result={scanStartedAt:diag.scanStartedAt,scanFinishedAt:new Date().toISOString(),lookbackHours:LOOKBACK_HOURS,candidateCount:candidates.length,candidates};
await saveJson(RESULT_PATH,result); await saveJson(DIAG_PATH,diag);
const lines=['# 당근 노트북 감시 최신 실행','', '- 순서: '+REGIONS.join(' → '),'- 최근 신규 기준: '+LOOKBACK_HOURS+'시간','- 원시 후보: '+candidates.length+'건',''];
if(!candidates.length)lines.push('조건을 통과한 신규/가격인하 원시 후보 없음.');
for(const c of candidates){lines.push('## '+c.city+' · '+c.title,'- 사유: '+c.reason,'- 가격: '+Number(c.price).toLocaleString('ko-KR')+'원','- RAM/SSD: '+c.specs.ramGB+'GB / '+c.specs.ssdGB+'GB','- CPU: '+(c.specs.cpuText||'미확인'),'- 등록: '+(c.postedAt||'미확인'),'- 끌올: '+(c.boostedAt||'없음/미확인'),'- 링크: '+c.url,'')}
await fs.writeFile(MD_PATH,lines.join('\n')+'\n');
console.log('SCAN_DIAGNOSTICS='+JSON.stringify(diag));
console.log('CANDIDATES_JSON='+JSON.stringify(candidates));
