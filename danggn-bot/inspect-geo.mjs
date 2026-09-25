const center={lat:37.5183859,lng:127.1107552};
const regions=['잠실동-6188','풍납1동-405','방이동-6185','가락1동-420','대치1동-389','삼성1동-387','역삼동-6035'];
function balanced(input,start,open){const close=open==='{'?'}':']';let d=0,s=false,e=false;for(let i=start;i<input.length;i++){const c=input[i];if(s){if(e){e=false;continue}if(c==='\\'){e=true;continue}if(c==='"')s=false;continue}if(c==='"'){s=true;continue}if(c===open)d++;if(c===close){d--;if(d===0)return input.slice(start,i+1)}}return null}
function after(html,marker){const i=html.indexOf(marker);if(i<0)return null;const t=html.slice(i+marker.length),s=t.indexOf('{');if(s<0)return null;const j=balanced(t,s,'{');if(!j)return null;try{return JSON.parse(j)}catch{return null}}
function hav(a,b){const R=6371,toRad=x=>x*Math.PI/180;const dlat=toRad(b.lat-a.lat),dlon=toRad(b.lng-a.lng);const q=Math.sin(dlat/2)**2+Math.cos(toRad(a.lat))*Math.cos(toRad(b.lat))*Math.sin(dlon/2)**2;return 2*R*Math.asin(Math.sqrt(q))}
for(const slug of regions){
 const u='https://www.daangn.com/kr/buy-sell/?search=%EB%85%B8%ED%8A%B8%EB%B6%81&in='+encodeURIComponent(slug);
 const r=await fetch(u,{headers:{'User-Agent':'Mozilla/5.0','Accept-Language':'ko-KR,ko;q=0.9'}});
 const h=await r.text();
 const c=after(h,'"regionCenterCoordinate":'), b=after(h,'"regionBounds":');
 console.log('RADIUS_TEST',JSON.stringify({slug,status:r.status,center:c,bounds:b,km:c?Number(hav(center,c).toFixed(3)):null}));
 await new Promise(x=>setTimeout(x,250));
}
