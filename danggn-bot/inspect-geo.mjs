const center={lat:37.5184031,lng:127.1113465};
const regions=['대치1동-389','풍납1동-405','삼성1동-387','역삼동-6035','가락1동-420','방이동-6185','대치동-6032'];
function balanced(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf('{');if(s<0)return null;const j=balanced(t,s,'{');if(!j)return null;try{return JSON.parse(j)}catch{return null}}
function hav(a,b){const R=6371,toRad=x=>x*Math.PI/180;const dlat=toRad(b.lat-a.lat),dlon=toRad(b.lng-a.lng);const q=Math.sin(dlat/2)**2+Math.cos(toRad(a.lat))*Math.cos(toRad(b.lat))*Math.sin(dlon/2)**2;return 2*R*Math.asin(Math.sqrt(q))}
function cornerDists(b){if(!b)return[];return [[b.south,b.west],[b.south,b.east],[b.north,b.west],[b.north,b.east]].map(([lat,lng])=>hav(center,{lat,lng}))}
for(const slug of regions){
 const u='https://www.daangn.com/kr/buy-sell/?search=%EB%85%B8%ED%8A%B8%EB%B6%81&in='+encodeURIComponent(slug);
 const r=await fetch(u,{headers:{'User-Agent':'Mozilla/5.0','Accept-Language':'ko-KR,ko;q=0.9'}});
 const h=await r.text();
 const c=after(h,'"regionCenterCoordinate":'), b=after(h,'"regionBounds":');
 const ds=cornerDists(b);
 const km=c?hav(center,c):null;
 console.log('DEAL_RADIUS',JSON.stringify({slug,centerKm:km?Number(km.toFixed(3)):null,minCornerKm:ds.length?Number(Math.min(...ds).toFixed(3)):null,maxCornerKm:ds.length?Number(Math.max(...ds).toFixed(3)):null,centerInside:km!==null&&km<=10,allCornersInside:ds.length>0&&Math.max(...ds)<=10}));
 await new Promise(x=>setTimeout(x,250));
}
