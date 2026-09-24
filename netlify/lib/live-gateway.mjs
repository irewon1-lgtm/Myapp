/** Fixed same-origin gateway. Only verified public release assets, never secrets or sync data. */
import fs from 'node:fs/promises';
import path from 'node:path';
import {createHash} from 'node:crypto';
export const REPO='irewon1-lgtm/Myapp', BRANCH='chatbook-app-20260923';
export const RAW='https://raw.githubusercontent.com/'+REPO+'/';
export const CSP="default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com data:; img-src 'self' data: blob: https://raw.githubusercontent.com https://i.ytimg.com; connect-src 'self' https://fonts.googleapis.com https://fonts.gstatic.com https://raw.githubusercontent.com; worker-src 'self' blob:; frame-src 'self' blob:; object-src 'none'; base-uri 'self'; form-action 'self'";
export const HASH=/^[a-f0-9]{64}$/, COMMIT=/^[a-f0-9]{40}$/;
export const sha=b=>createHash('sha256').update(b).digest('hex');
const types={'.js':'text/javascript','.mjs':'text/javascript','.css':'text/css','.html':'text/html','.json':'application/json','.webmanifest':'application/manifest+json','.txt':'text/plain','.svg':'image/svg+xml','.png':'image/png','.webp':'image/webp','.jpg':'image/jpeg','.jpeg':'image/jpeg','.woff2':'font/woff2'};
export function safePath(p){return typeof p==='string'&&p.length<240&&!p.startsWith('/')&&!p.includes('..')&&!p.includes('\\')&&!p.split('/').some(x=>!x||x.startsWith('.'))&&/^[A-Za-z0-9_./-]+$/.test(p)&&Object.hasOwn(types,path.extname(p));}
const need=(ok,msg)=>{if(!ok)throw Error(msg);};
export function validateManifest(m){
 need(m?.schema===1&&m.kind==='chatbook-live-release'&&m.shell===1&&m.syncSchema===1,'incompatible release');
 need(Number.isSafeInteger(m.sequence)&&m.sequence>0&&COMMIT.test(m.sourceSha),'invalid release identity');
 need(m.assets&&typeof m.assets==='object'&&!Array.isArray(m.assets),'invalid asset map');
 const entries=Object.entries(m.assets);need(entries.length>0&&entries.length<=400,'asset count');let total=0;
 for(const[p,a]of entries){need(safePath(p)&&HASH.test(a?.sha256)&&Number.isSafeInteger(a.bytes)&&a.bytes>=0&&a.bytes<=8*1024*1024,'invalid asset');total+=a.bytes;}
 need(total<=32*1024*1024,'release too large');
 for(const p of ['index.html','app.js','core.js','reader.js','views.js','style.css','library.js','content/catalog.json','engine-spec.json','engine-info.json','version.json'])need(Object.hasOwn(m.assets,p),'missing required asset: '+p);
 return m;
}
export function validateChannel(c){
 need(c?.schema===1&&c.kind==='chatbook-live-channel'&&Number.isSafeInteger(c.sequence)&&c.sequence>0,'invalid channel');
 need(COMMIT.test(c.current?.commit)&&HASH.test(c.current?.manifestSha),'invalid channel identity');return c;
}
export function renderEntry(html,commit,m,manifestSha){
 need(commit==='bundled'||COMMIT.test(commit),'bad entry commit');
 need(!/<base\b/i.test(html),'source must not override base');
 need(!/<script\b[^>]*>(?!\s*<\/script>)[\s\S]*?<\/script>/i.test(html),'inline script not allowed');
 const base='/__cb/r/'+commit+'/';
 html=html.replace(/<script\b([^>]*?)src=["']([^"']+)["']([^>]*)>/gi,(all,before,src,after)=>{
   const file=src.replace(/^\.\//,'').split('?')[0];need(safePath(file)&&m.assets[file],'unknown entry script');
   const integrity='sha256-'+Buffer.from(m.assets[file].sha256,'hex').toString('base64');
   return '<script'+before+'src="'+base+file+'" integrity="'+integrity+'"'+after+'>';
 });
 html=html.replace(/<link\b([^>]*?)href=["']([^"']+)["']([^>]*)>/gi,(all,before,href,after)=>{
   if(/rel=["']manifest["']/i.test(all))return '<link'+before+'href="/manifest.webmanifest"'+after+'>';
   if(!/rel=["']stylesheet["']/i.test(all)||/^https:/.test(href))return all;
   const file=href.replace(/^\.\//,'').split('?')[0];need(safePath(file)&&m.assets[file],'unknown stylesheet');
   return '<link'+before+'href="'+base+file+'" integrity="sha256-'+Buffer.from(m.assets[file].sha256,'hex').toString('base64')+'"'+after+'>';
 });
 return html.replace(/<head>/i,'<head><base href="'+base+'"><script src="/live-guard.js" data-commit="'+commit+'" data-sequence="'+m.sequence+'" data-manifest="'+manifestSha+'"></script>');
}
export function createGateway({fetcher=fetch,bundleRoot=path.resolve('dist'),manifestFile=path.resolve('dist/bootstrap-manifest.json'),now=Date.now,channelTTL=15000}={}){
 let recent=null,recentAt=0;const manifests=new Map(),bodies=new Map();let bodySize=0;
 async function getBytes(url,max){
   const r=await fetcher(url,{redirect:'error',signal:AbortSignal.timeout(7000),headers:{Accept:'application/octet-stream'}});
   need(r.ok,'upstream HTTP '+r.status);need(Number(r.headers.get('content-length')||0)<=max,'upstream size');
   const reader=r.body?.getReader();if(!reader){const b=Buffer.from(await r.arrayBuffer());need(b.length<=max,'upstream size');return b;}
   let size=0;const chunks=[];try{while(true){const {value,done}=await reader.read();if(done)break;size+=value.length;if(size>max){await reader.cancel();throw Error('upstream size');}chunks.push(value);}}finally{reader.releaseLock();}
   return Buffer.concat(chunks);
 }
 async function channel(){
   if(recent&&now()-recentAt<channelTTL)return recent;
   const c=validateChannel(JSON.parse((await getBytes(RAW+BRANCH+'/live/channel.json',16384)).toString()));
   need(!recent||c.sequence>=recent.sequence,'stale channel');recent=c;recentAt=now();return c;
 }
 async function manifest(commit,expected){
   need(commit==='bundled'||COMMIT.test(commit),'invalid commit');let v=manifests.get(commit);
   if(!v){const b=commit==='bundled'?await fs.readFile(manifestFile):await getBytes(RAW+commit+'/live/release.json',256*1024);
     v={manifest:validateManifest(JSON.parse(b.toString())),hash:sha(b),bytes:b};
     if(manifests.size>=12)manifests.delete(manifests.keys().next().value);manifests.set(commit,v);
   }
   need(!expected||v.hash===expected,'manifest hash mismatch');return v;
 }
 async function asset(commit,m,file){
   need(safePath(file)&&Object.hasOwn(m.assets,file),'asset not allowed');const meta=m.assets[file],key=commit+':'+file;
   let b=bodies.get(key);if(!b){
     b=commit==='bundled'?await fs.readFile(path.join(bundleRoot,file)):await getBytes(RAW+commit+'/live/assets/'+file,meta.bytes+1);
     need(b.length===meta.bytes&&sha(b)===meta.sha256,'asset integrity mismatch');
     while(bodies.size&&bodySize+b.length>24*1024*1024){const k=bodies.keys().next().value;bodySize-=bodies.get(k).length;bodies.delete(k);}
     bodies.set(key,b);bodySize+=b.length;
   }return b;
 }
 function response(body,status=200,file='error.json',extra={}){
   const mime=types[path.extname(file)]||'application/json';
   return new Response(body,{status,headers:{'Content-Type':mime+(/^(text\/|application\/(json|manifest))/.test(mime)?'; charset=utf-8':''),'Cache-Control':'no-store','Netlify-CDN-Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Content-Security-Policy':CSP,'X-Frame-Options':'SAMEORIGIN','Referrer-Policy':'no-referrer',...extra}});
 }
 async function entry(commit,expected){
   const v=await manifest(commit,expected),html=(await asset(commit,v.manifest,'index.html')).toString();
   return response(renderEntry(html,commit,v.manifest,v.hash),200,'index.html',{'X-Chatbook-Commit':commit,'X-Chatbook-Sequence':String(v.manifest.sequence),'X-Chatbook-Manifest':v.hash});
 }
 return async function handle(request){
   if(!['GET','HEAD'].includes(request.method))return response('{"error":"read-only endpoint"}',405);
   const u=new URL(request.url),p=u.pathname;
   // No credentials, cookies or user-selected hosts are ever forwarded upstream.
   if(/%(?:2f|5c|2e)|\.\./i.test(p))return response('{"error":"invalid path"}',400);
   try{
     // Platform SPA rewrites may forward an original static URL here. Handle
     // the immutable bootstrap explicitly, even on a completely cold CDN.
     if(['/live-guard.js','/live-sw.js','/manifest.webmanifest','/icon-192.png','/icon-512.png'].includes(p)){
       const file=p.slice(1),v=await manifest('bundled'),b=await asset('bundled',v.manifest,file);
       return response(request.method==='HEAD'?null:b,200,file,{'Cache-Control':'no-cache, must-revalidate','Service-Worker-Allowed':'/','X-Chatbook-Bootstrap':'1'});
     }
     if(p==='/'||p==='/index.html'){
       const pin=u.searchParams.get('cb_release');
       if(pin){if(pin!=='bundled'&&!COMMIT.test(pin))return response('{"error":"invalid release"}',400);try{return await entry(pin);}catch{return await entry('bundled');}}
       try{const c=await channel();return await entry(c.current.commit,c.current.manifestSha);}catch{return await entry('bundled');}
     }
     if(p==='/__cb/channel.json'){const c=await channel();return response(JSON.stringify(c));}
     const meta=p.match(/^\/__cb\/manifest\/([a-f0-9]{40}|bundled)\.json$/);
     if(meta){const v=await manifest(meta[1]);return response(v.bytes,200,'release.json',{'X-Chatbook-SHA256':v.hash});}
     const match=p.match(/^\/__cb\/r\/([a-f0-9]{40}|bundled)\/(.+)$/);
     if(match){const[,commit,file]=match;const v=await manifest(commit),b=await asset(commit,v.manifest,file);
       return response(request.method==='HEAD'?null:b,200,file,{'Cache-Control':commit==='bundled'?'no-cache':'public, max-age=31536000, immutable','Netlify-CDN-Cache-Control':commit==='bundled'?'no-store':'public, s-maxage=31536000, durable','X-Chatbook-SHA256':v.manifest.assets[file].sha256,'X-Chatbook-Commit':commit});}
     const aliases={'/engine-spec':'engine-spec.html','/engine-spec/':'engine-spec.html','/engine-info':'engine-info.html','/engine-info/':'engine-info.html','/engine/':'engine/index.html'};
     const file=aliases[p]||p.slice(1);
     if(!safePath(file)||!(/^(engine\/|content\/)/.test(file)||['engine-spec.json','engine-spec.html','engine-info.json','engine-info.html','llms.txt','version.json'].includes(file)))return response('{"error":"not found"}',404);
     const c=await channel(),v=await manifest(c.current.commit,c.current.manifestSha),b=await asset(c.current.commit,v.manifest,file);
     // Metadata is network-only: a disconnected reader may use cached books, but never claim stale rules are current.
     return response(request.method==='HEAD'?null:b,200,file,{'X-Chatbook-Commit':c.current.commit,'X-Chatbook-Sequence':String(c.sequence),'X-Chatbook-SHA256':v.manifest.assets[file].sha256});
   }catch(e){return response(JSON.stringify({error:'현재 검증된 파일을 확인하지 못했습니다. 저장된 독서 기록은 변경하지 않았습니다.'}),503);}
 };
}
