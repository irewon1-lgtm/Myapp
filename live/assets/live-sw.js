/* Fixed offline controller. Activate follows the normal lifecycle; no forced reload/skipWaiting. */
const CONTROL='chatbook-live-control-v1',PREFIX='chatbook-live-cache-';
const META=new Set(['/engine-spec','/engine-spec/','/engine-spec.json','/engine-spec.html','/engine-info','/engine-info/','/engine-info.json','/engine-info.html','/version.json','/llms.txt','/__cb/channel.json']);
const valid=c=>c==='bundled'||/^[a-f0-9]{40}$/.test(c||'');
const hash=async b=>[...new Uint8Array(await crypto.subtle.digest('SHA-256',b))].map(x=>x.toString(16).padStart(2,'0')).join('');
const jobs=new Map();
async function pointer(){try{const r=await(await caches.open(CONTROL)).match('/__cb/offline-pointer');return r?await r.json():null;}catch{return null;}}
self.addEventListener('install',e=>e.waitUntil((async()=>{const c=await caches.open(CONTROL);await c.add('/live-guard.js');})()));
self.addEventListener('activate',e=>e.waitUntil(self.clients.claim()));
async function prepare(d){
 if(!valid(d.commit)||!Number.isSafeInteger(d.sequence)||!/^[a-f0-9]{64}$/.test(d.manifestSha||''))throw Error('identity');
 const commit=d.commit,name=PREFIX+commit,ctl=await caches.open(CONTROL),previous=await pointer();
 if(previous?.current?.commit===commit&&await(await caches.open(name)).match('/__cb/offline-entry'))return;
 const mr=await fetch('/__cb/manifest/'+commit+'.json',{cache:'no-store'});if(!mr.ok)throw Error('manifest');const mb=await mr.arrayBuffer();if(await hash(mb)!==d.manifestSha)throw Error('manifest hash');
 const m=JSON.parse(new TextDecoder().decode(mb));if(m.schema!==1||m.shell!==1||m.sequence!==d.sequence)throw Error('compatibility');
 const entries=Object.entries(m.assets||{});if(entries.length>400||entries.reduce((s,[,v])=>s+v.bytes,0)>32*1024*1024)throw Error('size');
 const cache=await caches.open(name);
 // Serial batches limit mobile memory. Partial caches never become the active offline version.
 for(let i=0;i<entries.length;i+=4)await Promise.all(entries.slice(i,i+4).map(async([file,meta])=>{
   if(!/^[A-Za-z0-9_./-]+$/.test(file)||file.includes('..'))throw Error('path');
   const url='/__cb/r/'+commit+'/'+file;const existing=await cache.match(url);if(existing&&existing.headers.get('X-Chatbook-SHA256')===meta.sha256)return;
   const r=await fetch(url);if(!r.ok)throw Error('asset');const bytes=await r.arrayBuffer();if(bytes.byteLength!==meta.bytes||await hash(bytes)!==meta.sha256)throw Error('integrity');
   await cache.put(url,new Response(bytes,{status:200,headers:r.headers}));
 }));
 const root=await fetch('/?cb_release='+commit+'&cb_probe=1',{cache:'no-store'});
 if(!root.ok||root.headers.get('X-Chatbook-Commit')!==commit)throw Error('entry');
 await cache.put('/__cb/offline-entry',root);
 const latest=await pointer();if(latest?.current?.sequence>d.sequence)return;
 const value={current:{commit,sequence:d.sequence},previous:latest?.current?.commit!==commit?latest?.current:latest?.previous};
 await ctl.put('/__cb/offline-pointer',new Response(JSON.stringify(value),{headers:{'Content-Type':'application/json'}}));
 // Keep the prior complete version. Never remove user-downloads, old app caches, localStorage or IndexedDB.
 for(const key of await caches.keys())if(key.startsWith(PREFIX)&&key!==name&&key!==PREFIX+value.previous?.commit)await caches.delete(key);
}
self.addEventListener('message',e=>{
 if(e.data?.type!=='CHATBOOK_HEALTHY')return;const d=e.data;
 e.waitUntil((async()=>{try{if(!jobs.has(d.commit))jobs.set(d.commit,prepare(d).finally(()=>jobs.delete(d.commit)));await jobs.get(d.commit);e.source?.postMessage({type:'CHATBOOK_OFFLINE_READY',commit:d.commit,notify:d.notify});}catch{e.source?.postMessage({type:'CHATBOOK_OFFLINE_FAILED',commit:d.commit,notify:d.notify});}})());
});
self.addEventListener('fetch',e=>{
 const r=e.request,u=new URL(r.url);if(r.method!=='GET'||u.origin!==self.location.origin||u.pathname.startsWith('/api/')||u.pathname.startsWith('/.netlify/')||META.has(u.pathname)||u.pathname.startsWith('/__cb/manifest/'))return;
 if(u.pathname==='/live-guard.js'){e.respondWith((async()=>{try{return await fetch(r);}catch{return await(await caches.open(CONTROL)).match('/live-guard.js')||new Response('',{status:503});}})());return;}
 const match=u.pathname.match(/^\/__cb\/r\/([a-f0-9]{40}|bundled)\//);
 if(match){e.respondWith((async()=>await(await caches.open(PREFIX+match[1])).match(u.pathname)||fetch(r))());return;}
 if(r.mode==='navigate'&&(u.pathname==='/'||u.pathname==='/index.html')){
   e.respondWith((async()=>{try{const response=await fetch(r);if(response.ok)return response;}catch{}
     const p=await pointer(),wanted=u.searchParams.get('cb_release');const choices=[wanted,p?.current?.commit,p?.previous?.commit].filter(valid);
     for(const c of [...new Set(choices)]){const old=await(await caches.open(PREFIX+c)).match('/__cb/offline-entry');if(old)return old;}
     return new Response('<!doctype html><meta charset="utf-8"><p>아직 이 기기에 오프라인 교재를 보관하지 못했습니다. 인터넷 연결 후 챗북을 다시 열어 주세요. 저장된 독서 기록은 삭제하지 않았습니다.</p>',{status:503,headers:{'Content-Type':'text/html; charset=utf-8'}});
   })());
 }
});
self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.openWindow('/#review'));});
