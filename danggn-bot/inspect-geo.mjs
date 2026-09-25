const url='https://www.daangn.com/kr/buy-sell/hp-%ED%8C%8C%EB%B9%8C%EB%A6%AC%EC%98%A8-x360-14%EC%9D%B8%EC%B9%98-i7-13%EC%84%B8%EB%8C%80-32gb-ram-1tb-nvme-%ED%8E%9C-%ED%8F%AC%ED%95%A8-%EA%B3%A0%EC%84%B1%EB%8A%A5-wz2mfeihg7ff/';
const r=await fetch(url,{headers:{'User-Agent':'Mozilla/5.0','Accept-Language':'ko-KR,ko;q=0.9'}});
const h=await r.text();
function bal(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker,open){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf(open);if(s<0)return null;const j=bal(t,s,open);if(!j)return null;try{return JSON.parse(j)}catch{return null}}
const p=after(h,'"product":','{')??after(h,'\\"product\\":','{');
const walk=(o,pfx='')=>{const out=[];if(!o||typeof o!=='object')return out;for(const [k,v] of Object.entries(o)){const key=pfx? pfx+'.'+k:k;if(/lat|lng|lon|coord|region|location|address/i.test(k))out.push({key,type:typeof v,value:(typeof v==='string'||typeof v==='number')?v:undefined});if(v&&typeof v==='object'&&pfx.split('.').length<3)out.push(...walk(v,key));}return out};
console.log('STATUS',r.status);
console.log('PRODUCT_KEYS',JSON.stringify(p?Object.keys(p):[]));
console.log('GEO_FIELDS',JSON.stringify(walk(p)));
console.log('REGION',JSON.stringify(p?.region||null));
