/** Only generated app files live in chatbook-offline-* caches. Never touch user stores. */
export function serviceWorker(digest,urls,infoPaths){
  const paths=[...new Set(urls.filter(p=>!infoPaths.includes(new URL(p,'https://local/').pathname)))];
  return `const NAME='chatbook-offline-${digest.slice(0,12)}';
const URLS=${JSON.stringify(paths)};
const INFO=${JSON.stringify(infoPaths)};
const origin=()=>self.location?.origin||location.origin;
const key=url=>new URL(url,origin()).pathname;
const APP=new Set(URLS.map(u=>new URL(u,'https://local/').pathname));
self.addEventListener('install',e=>e.waitUntil(caches.open(NAME).then(c=>c.addAll(URLS))));
// Do not force an update into an open reader. Activation follows the normal SW lifecycle.
self.addEventListener('activate',e=>e.waitUntil((async()=>{
  for(const name of await caches.keys())if(name.startsWith('chatbook-offline-')&&name!==NAME)await caches.delete(name);
  const cache=await caches.open(NAME);
  for(const req of await cache.keys())if(INFO.includes(new URL(req.url).pathname))await cache.delete(req);
  await self.clients.claim();
})()));
self.addEventListener('fetch',e=>{
  if(e.request.method!=='GET')return;
  const u=new URL(e.request.url);
  if(u.origin!==origin()||u.pathname.startsWith('/api/')||u.pathname.startsWith('/.netlify/')||INFO.includes(u.pathname))return;
  e.respondWith((async()=>{
    try{
      const response=await fetch(e.request);
      if(response.ok&&APP.has(u.pathname)&&!u.search){
        const copy=response.clone();
        e.waitUntil(caches.open(NAME).then(c=>c.put(key(e.request.url),copy)).catch(()=>{}));
      }
      return response;
    }catch{
      const cache=await caches.open(NAME);
      return await cache.match(key(e.request.url))||(e.request.mode==='navigate'?await cache.match('/index.html'):null)||new Response('Offline file unavailable',{status:503});
    }
  })());
});
self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.openWindow(e.notification.data?.url||'./#review'));});`;
}
