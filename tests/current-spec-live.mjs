/** Real published HTTP/Chromium checks; local HTTP only for worker lifecycle fault injection. */
import fs from 'node:fs';
import http from 'node:http';
import {createHash} from 'node:crypto';
import {chromium} from 'playwright';
import {canonical} from '../scripts/current-spec.mjs';
import {INFO_PATHS,serviceWorker} from '../scripts/engine-info.mjs';
const base='https://chatbook-library-20260923.netlify.app',dir='tests/current-spec-results';fs.mkdirSync(dir,{recursive:true});
const expected=JSON.parse(fs.readFileSync('dist/engine-spec.json','utf8'));
const report={scope:'Published HTTPS current specification and isolated Chromium; actual generated worker tested on local HTTP across update/failure. No physical Galaxy or new-video generation.',checks:[],errors:[],startedAt:new Date().toISOString()};
const save=()=>fs.writeFileSync(dir+'/report.json',JSON.stringify(report,null,2));
function check(name,value,detail){report.checks.push({name,passed:!!value,detail});save();console.log(name,!!value);}
const get=p=>fetch(base+p+'?verify='+Date.now(),{cache:'no-store',signal:AbortSignal.timeout(20000)});
let browser,server;
try{
  const response=await get('/engine-spec.json'),text=await response.text(),spec=JSON.parse(text);
  report.specDigest=spec.contentDigest;report.specBytes=Buffer.byteLength(text);fs.writeFileSync(dir+'/public-engine-spec.json',text);
  check('published current spec exactly matches tested bundle',canonical(spec)===canonical(expected));
  check('spec is JSON and not a JavaScript-only app fallback',response.headers.get('content-type')?.includes('application/json'));
  for(const p of ['/engine-spec.json','/engine-spec.html','/engine-spec','/engine-spec/']){const r=await get(p);check('current spec alias accessible and no-store '+p,r.ok&&/no-store/i.test(r.headers.get('cache-control')||''));}
  const home=await(await get('/')).text();fs.writeFileSync(dir+'/public-home.html',home);
  const bytes=Buffer.from(await(await get('/content/catalog.json')).arrayBuffer());
  const hash=b=>createHash('sha256').update(b).digest('hex');
  check('published catalog is byte-identical to preserved source',hash(bytes)===hash(fs.readFileSync('public/content/catalog.json')));
  report.books=JSON.parse(bytes).books.length;
  browser=await chromium.launch({headless:true});
  const staticContext=await browser.newContext({javaScriptEnabled:false,viewport:{width:360,height:800}}),p=await staticContext.newPage();
  p.setDefaultTimeout(20000);await p.goto(base,{waitUntil:'domcontentloaded'});
  check('base URL exposes the current spec JSON',await p.locator('link[rel="alternate"][type="application/json"][href="/engine-spec.json"]').count()===1);
  const specLink=p.locator('a[href="/engine-spec"],a[href="/engine-spec/"],a[href="/engine-spec.html"]').first();
  const hasSpecLink=await specLink.count()>0;check('base URL exposes a readable current-spec link',hasSpecLink);
  if(hasSpecLink){await specLink.click();await p.waitForLoadState('domcontentloaded');}else await p.goto(base+'/engine-spec.html',{waitUntil:'domcontentloaded'});
  check('current rules and prose readable without JavaScript',(await p.locator('body').innerText()).includes(spec.example.title));
  check('static page has matching spec identity',await p.locator('body').getAttribute('data-spec-digest')===spec.contentDigest);
  check('static page fits 360px',await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
  await p.screenshot({path:dir+'/current-spec-360.png',fullPage:true});await staticContext.close();
  const readerContext=await browser.newContext({viewport:{width:412,height:915},serviceWorkers:'block'});
  await readerContext.route('**/*',r=>INFO_PATHS.includes(new URL(r.request().url()).pathname)?r.abort():r.continue());
  const r=await readerContext.newPage();r.on('pageerror',e=>report.errors.push(e.message));
  await r.goto(base,{waitUntil:'domcontentloaded'});await r.waitForFunction(()=>window.CB?.catalog?.books?.length);
  await r.evaluate(()=>CB.read('yt-python-1h-8KCuHHeC_M0'));await r.waitForFunction(()=>CB.Reader?.pages>1);
  check('Python book works with current spec deliberately unavailable',await r.evaluate(()=>CB.Reader.book.id==='yt-python-1h-8KCuHHeC_M0'));
  await r.screenshot({path:dir+'/preserved-reader-412.png'});await readerContext.close();
  // Execute the actual new worker in a browser, without modifying the production site.
  let version=1,failInstall=false;
  server=http.createServer((req,res)=>{
    res.setHeader('Cache-Control','no-store');
    if(req.url.startsWith('/sw.js')){res.setHeader('Content-Type','text/javascript');res.end(serviceWorker(String(version).padStart(12,'0'),['./','./index.html','./reader.js',...(failInstall?['./missing.js']:[]) ]));}
    else if(req.url.startsWith('/missing.js')){res.statusCode=503;res.end('deliberate failed install');}
    else if(req.url.startsWith('/reader.js')){res.setHeader('Content-Type','text/javascript');res.end('/* READER_'+version+' */');}
    else if(req.url.includes('engine-spec')){res.setHeader('Content-Type','application/json');res.end('{"current":true}');}
    else {res.setHeader('Content-Type','text/html');res.end('<!doctype html><title>Reader lifecycle '+version+'</title><p>Open reading tab</p>');}
  });await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const local='http://127.0.0.1:'+server.address().port,ctx=await browser.newContext(),a=await ctx.newPage();
  await a.goto(local);await a.evaluate(async()=>{localStorage.setItem('chatbook.records.v1','PRESERVED');const c=await caches.open('user-downloads');await c.put('/saved.txt',new Response('KEEP'));await navigator.serviceWorker.register('/sw.js');await navigator.serviceWorker.ready;});
  await a.waitForFunction(()=>navigator.serviceWorker.controller);
  version=2;
  await a.evaluate(async()=>{await(await navigator.serviceWorker.getRegistration()).update();});
  await a.waitForFunction(async()=>Boolean((await navigator.serviceWorker.getRegistration())?.waiting));
  check('new worker waits while the old reading tab remains open',(await a.title()).endsWith('1'));
  await a.close();const b=await ctx.newPage();await b.goto(local);
  await b.waitForFunction(async()=>{const names=(await caches.keys()).filter(n=>n.startsWith('chatbook-offline-'));return names.length===1&&names[0].endsWith('000000000002');});
  check('activation leaves only the current generated app cache',true);
  check('local reader records survive worker update',await b.evaluate(()=>localStorage.getItem('chatbook.records.v1'))==='PRESERVED');
  check('user download cache survives worker update',await b.evaluate(async()=>await(await(await caches.open('user-downloads')).match('/saved.txt')).text())==='KEEP');
  await ctx.setOffline(true);
  check('offline fallback reads current version only',(await b.evaluate(async()=>await(await fetch('/reader.js?v=older')).text())).includes('READER_2'));
  check('offline mode cannot return a cached latest spec',await b.evaluate(async()=>{try{await fetch('/engine-spec.json');return false;}catch{return true;}}));
  await ctx.setOffline(false);version=3;failInstall=true;
  await b.evaluate(async()=>{const reg=await navigator.serviceWorker.getRegistration();await reg.update();const w=reg.installing;if(w&&!['redundant','installed'].includes(w.state))await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('worker install state timeout')),15000);const done=()=>{if(w.state==='redundant'||w.state==='installed'){clearTimeout(timer);resolve();}};w.addEventListener('statechange',done);done();});});
  await ctx.setOffline(true);
  check('failed new worker install retains working prior offline reader',(await b.evaluate(async()=>await(await fetch('/reader.js')).text())).includes('READER_2'));
  await ctx.close();check('no reader JavaScript exceptions',report.errors.length===0,report.errors);report.status=report.checks.every(x=>x.passed)?'passed':'failed';if(report.status==='failed'){report.failure=report.checks.filter(x=>!x.passed).map(x=>x.name).join('; ');process.exitCode=1;}
}catch(e){report.status='failed';report.failure=String(e);process.exitCode=1;}finally{report.completedAt=new Date().toISOString();save();await browser?.close();if(server)await new Promise(resolve=>server.close(resolve));}
