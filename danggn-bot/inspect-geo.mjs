const center={lat:37.5183859,lng:127.1107552};
const regions=['잠실동-6188','풍납1동-405','방이동-6185','가락1동-420','대치1동-389','삼성1동-387','역삼동-6035'];
function pick(h,k){const m=h.match(new RegExp('"'+k+'"\\s*":\\s*(\\{[^{}]+\\})'));if(!m)return null;try{return JSON.parse(m[1])}catch{return null}}
function hav(a,b){const R=6371,toRad=x=>x*Math.PI/180;const dlat=toRad(b.lat-a.lat),dlon=toRad(b.lng-a.lng);const s=Math.sin(dlat/2)**2+Math.cos(toRad(a.lat))*Math.cos(toRad(b.lat))*Math.sin(dlon/2)**2;return 2*R*Math.asin(Math.sqrt(s))}
for(const slug of regions){
 const u='https://www.daangn.com/kr/buy-sell/?search=%EB%85%B8%ED%8A%B8%EB%B6%81&in='+encodeURIComponent(slug);
 const r=await fetch(u,{headers:{'User-Agent':'Mozilla/5.0','Accept-Language':'ko-KR,ko;q=0.9'}});
 const h=await r.text();
 const c=pick(h,'regionCenterCoordinate'),b=pick(h,'regionBounds');
 console.log('RADIUS_TEST',JSON.stringify({slug,status:r.status,center:c,bounds:b,km:c?Number(hav(center,c).toFixed(3)):null}));
 await new Promise(x=>setTimeout(x,400));
}
