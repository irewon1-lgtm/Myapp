import fs from 'node:fs/promises';
const URL='https://www.daangn.com/kr/buy-sell/lg-%EA%B7%B8%EB%9E%A8-%EB%85%B8%ED%8A%B8%EB%B6%81-16%EC%9D%B8%EC%B9%98-%EC%8B%A4%EB%B2%84-fp383rbhdeu4/';
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146 Safari/537.36';
function balanced(s,start,o){const c=o==='{'?'}':']';let d=0,q=false,e=false;for(let i=start;i<s.length;i++){const x=s[i];if(q){if(e){e=false;continue}if(x==='\\'){e=true;continue}if(x==='"')q=false;continue}if(x==='"'){q=true;continue}if(x===o)d++;if(x===c&&--d===0)return s.slice(start,i+1)}return null}
function after(h,m,o){const k=h.indexOf(m);if(k<0)return null;const t=h.slice(k+m.length),s=t.indexOf(o);if(s<0)return null;const raw=balanced(t,s,o);try{return raw?JSON.parse(raw):null}catch{return null}}
const r=await fetch(URL,{signal:AbortSignal.timeout(10000),headers:{'User-Agent':UA,'Accept':'text/html,application/xhtml+xml','Accept-Language':'ko-KR,ko;q=0.9'}});
if(!r.ok)throw new Error('HTTP '+r.status);
const h=await r.text();
const p=after(h,'"product":','{')??after(h,'\\\"product\\\":','{');
if(!p)throw new Error('product not found');
const out={id:'fp383rbhdeu4',title:p.title,price:p.price,description:p.content||'',status:p.status,createdAt:p.createdAt,boostedAt:p.boostedAt,location:p.locationName??p.region?.name,regionPath:[p.region?.name1,p.region?.name2,p.region?.name3].filter(Boolean).join(' '),sellerName:p.user?.nickname,mannerTemperature:p.user?.score,url:URL};
await fs.writeFile('danggn-bot/once-detail-fp383rbhdeu4.json',JSON.stringify(out,null,2));
console.log(JSON.stringify(out));