const NAME='chatbook-offline-c0cfc8402f63';
const URLS=["./app.js","./art/ai.webp","./art/dialogue.webp","./art/economy.webp","./art/habit-portrait.webp","./art/habit.webp","./art/leaves.webp","./art/mind.webp","./art/parent.webp","./art/reading-scene.webp","./art/routine.webp","./art/slow.webp","./art/stilllife.webp","./art/wisdom.webp","./art/yt-vibe-0.webp","./art/yt-vibe-1.webp","./art/yt-vibe-2.webp","./art/yt-vibe-3.webp","./art/yt-vibe-hq1.webp","./art/yt-vibe-hq2.webp","./art/yt-vibe-hq3.webp","./art/yt-vibe-hqdefault.webp","./content/catalog.json","./content/source-maps/8KCuHHeC_M0-review.json","./content/source-maps/Kl5EcYd3G2E-review.json","./content/source-maps/Kl5EcYd3G2E.json","./core.js","./engine/benchmark.json","./engine/core.mjs","./engine/defaults.json","./engine/index.html","./engine/learning-policy.json","./engine/learning-validation.json","./engine/learning.mjs","./engine/narrative.mjs","./engine/verification.json","./engine/workbench.css","./engine/workbench.mjs","./exports.js","./icon-192.png","./icon-512.png","./index.html","./library.js","./live-guard.js","./manifest.webmanifest","./reader.js","./style.css","./vendor/pptxgen.bundle.js","./views.js","./"];
const INFO=["/engine-info.json","/engine-info.html","/engine-info","/engine-info/","/llms.txt","/version.json","/engine-spec.json","/engine-spec.html","/engine-spec","/engine-spec/"];
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
self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.openWindow(e.notification.data?.url||'./#review'));});