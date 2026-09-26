const url = new URL('https://www.daangn.com/kr/buy-sell/');
url.searchParams.set('search','노트북');
url.searchParams.set('in','정자동-1339');
const r = await fetch(url, {headers:{'User-Agent':'Mozilla/5.0','Accept-Language':'ko-KR,ko;q=0.9'}});
console.log('HTTP', r.status);
const html = await r.text();
console.log('LEN', html.length);
const marker='"fleamarketArticles":';
const i=html.indexOf(marker);
console.log('MARKER',i);
if(i>=0){
  const tail=html.slice(i+marker.length);
  const start=tail.indexOf('[');
  let depth=0,inStr=false,esc=false,end=-1;
  for(let p=start;p<tail.length;p++){
    const c=tail[p];
    if(inStr){ if(esc){esc=false;continue;} if(c==='\\'){esc=true;continue;} if(c==='"')inStr=false; continue; }
    if(c==='"'){inStr=true;continue;}
    if(c==='[')depth++;
    if(c===']' && --depth===0){end=p+1;break;}
  }
  const arr=JSON.parse(tail.slice(start,end));
  console.log('COUNT',arr.length);
  if(arr[0]){
    console.log('KEYS',JSON.stringify(Object.keys(arr[0]).sort()));
    console.log('SAMPLE',JSON.stringify(arr[0]));
  }
}