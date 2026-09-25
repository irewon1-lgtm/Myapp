const urls=[
'https://www.daangn.com/kr/buy-sell/?search=%EB%85%B8%ED%8A%B8%EB%B6%81&in=%EC%9E%A0%EC%8B%A4%EB%8F%99-6188',
'https://www.daangn.com/kr/buy-sell/?search=%EB%85%B8%ED%8A%B8%EB%B6%81&in=%EC%8B%A0%EC%B2%9C%EB%8F%99-400'
];
for(const url of urls){
 const r=await fetch(url,{headers:{'User-Agent':'Mozilla/5.0','Accept-Language':'ko-KR,ko;q=0.9'}});
 const h=await r.text();
 console.log('URL',url,'STATUS',r.status,'LEN',h.length);
 for(const pat of ['latitude','longitude','\"lat\"','\"lng\"','coordinates','centerLat','centerLng']){
   const i=h.indexOf(pat);
   if(i>=0) console.log('FOUND',pat,h.slice(Math.max(0,i-180),i+360).replace(/\s+/g,' '));
 }
 const regionMatches=[...h.matchAll(/"region"\s*:\s*\{[^{}]{0,600}\}/g)].slice(0,3).map(m=>m[0]);
 console.log('REGION_SNIPPETS',JSON.stringify(regionMatches));
}
