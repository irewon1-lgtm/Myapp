/** Read-only HTTP checks and a disposable browser profile. Never writes user sync data. */
import fs from 'node:fs';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {chromium} from 'playwright';
import {INFO_PATHS} from '../scripts/engine-info.mjs';
const url='https://chatbook-library-20260923.netlify.app',dir='tests/release-live-results';
fs.mkdirSync(dir,{recursive:true});
const localVersion=JSON.parse(fs.readFileSync('dist/version.json','utf8'));
const localInfo=JSON.parse(fs.readFileSync('dist/engine-info.json','utf8'));
const expectedCatalog=JSON.parse(fs.readFileSync('dist/content/catalog.json','utf8'));
const report={url,scope:'Published HTTPS metadata and disposable Chromium reader smoke checks. No video generation, semantic certification or physical Galaxy test.',checks:[],errors:[],startedAt:new Date().toISOString()};
const save=()=>fs.writeFileSync(dir+'/report.json',JSON.stringify(report,null,2));
function check(name,passed,detail){report.checks.push({name,passed:!!passed,...(detail!==undefined?{detail}:{})});save();console.log(name,!!passed);if(!passed)throw Error(name);}
async function get(p){const r=await fetch(url+p+'?release-check='+Date.now(),{signal:AbortSignal.timeout(20000),cache:'no-store'});if(!r.ok)throw Error('HTTP '+r.status+' '+p);return r;}
let browser;
try{
 const vr=await get('/version.json'),version=await vr.json();report.version=version;
 check('deployed release is the tested source',version.sourceDigest===localVersion.sourceDigest&&version.releaseId===localVersion.releaseId);
 const ir=await get('/engine-info.json');check('metadata is JSON, not SPA fallback',ir.headers.get('content-type')?.includes('application/json'));
 const info=await ir.json();
 check('app and engine metadata have one release identity',info.release.id===version.releaseId&&info.release.sourceDigest===version.sourceDigest&&info.engine.version===version.engineVersion&&info.app.version===version.version);
 assert.deepEqual(info.engine,localInfo.engine);check('deployed engine policy equals tested executable policy',true);
 assert.deepEqual(info.assetSha256,localInfo.assetSha256);check('deployed metadata contains tested asset hashes',true);
 check('fresh source-bound build tests are reported',info.verification.buildTests.status==='passed'&&info.verification.buildTests.sourceDigest===version.sourceDigest&&info.verification.buildTests.nodeFailed===0);
 for(const p of INFO_PATHS){const r=await get(p);check('no-store header '+p,/no-store/i.test(r.headers.get('cache-control')||''),{header:r.headers.get('cache-control'),resolvedPath:new URL(r.url).pathname});}
 for(const [p,digest] of Object.entries(info.assetSha256)){const bytes=Buffer.from(await (await get(p)).arrayBuffer());check('live asset SHA256 '+p,createHash('sha256').update(bytes).digest('hex')===digest);}
 const home=await (await get('/')).text();fs.writeFileSync(dir+'/public-home.html',home);
 const text=await (await get('/llms.txt')).text();check('AI discovery names this release',text.includes(version.releaseId)&&text.includes('/engine-info.json'));
 const catalog=await (await get('/content/catalog.json')).json();assert.deepEqual(catalog,expectedCatalog);check('all current book content is preserved',true,{books:catalog.books.length});
 browser=await chromium.launch({headless:true});
 const staticContext=await browser.newContext({javaScriptEnabled:false,viewport:{width:360,height:800}});const s=await staticContext.newPage();s.setDefaultTimeout(20000);
 await s.goto(url,{waitUntil:'domcontentloaded'});
 const alternate=s.locator('link[rel="alternate"][type="application/json"]');
 check('home exposes machine-readable engine information',await alternate.count()>0&&(await alternate.first().getAttribute('href'))==='/engine-info.json');
 const link=s.locator('a[href*="engine-info"]').first();const linkCount=await s.locator('a[href*="engine-info"]').count();
 check('home exposes readable engine information without JavaScript',linkCount>0,linkCount?await link.getAttribute('href'):null);
 await link.click();await s.waitForLoadState('domcontentloaded');
 check('engine information reachable from home without JavaScript',await s.locator('h1').innerText()==='챗북 엔진 정보');
 check('static engine page contains current release',await s.locator('body').getAttribute('data-release-id')===version.releaseId);
 await s.screenshot({path:dir+'/engine-info-mobile.png'});await staticContext.close();
 const context=await browser.newContext({viewport:{width:412,height:915},serviceWorkers:'block'});
 await context.route('**/*',async route=>{const p=new URL(route.request().url()).pathname;if(INFO_PATHS.includes(p))return route.abort('failed');return route.continue();});
 const p=await context.newPage();p.on('pageerror',e=>report.errors.push(e.message));p.setDefaultTimeout(20000);
 await p.goto(url,{waitUntil:'domcontentloaded'});await p.waitForFunction(()=>window.CB&&CB.catalog?.books?.length>0);
 check('library opens when metadata endpoints fail',await p.evaluate(n=>CB.catalog.books.length===n,catalog.books.length));
 const id=catalog.books[0]?.id;if(!id)throw Error('Expected at least one readable book');
 for(const width of [360,412,768]){
  await p.setViewportSize({width,height:915});await p.evaluate(id=>CB.read(id),id);await p.waitForTimeout(500);
  check('reader works independently '+width,await p.evaluate(id=>CB.Reader.book.id===id&&Number.isFinite(CB.Reader.pages)&&CB.Reader.pages>=1,id));
  check('reader no horizontal overflow '+width,await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
 }
 await p.setViewportSize({width:412,height:915});await p.screenshot({path:dir+'/reader-without-metadata.png'});
 await p.evaluate(()=>CB.Reader.bookmark());const before=await p.evaluate(id=>CB.annotations(id,'bookmark').length,id);
 await p.reload({waitUntil:'domcontentloaded'});await p.waitForFunction(()=>window.CB&&CB.catalog?.books?.length>0);await p.evaluate(id=>CB.read(id),id);await p.waitForTimeout(500);
 check('disposable bookmark survives reload despite missing metadata',await p.evaluate(({id,before})=>before>0&&CB.annotations(id,'bookmark').length===before,{id,before}));
 for(const route of ['home','library','quiz','settings']){await p.evaluate(route=>CB.go(route),route);await p.waitForTimeout(100);check('existing route works '+route,await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));}
 check('no real user synchronization was configured',!await p.evaluate(()=>Boolean(CB.pref('syncCode',''))));
 check('no JavaScript runtime exception',report.errors.length===0,report.errors);report.status='passed';
}catch(e){report.status='failed';report.failure=String(e);process.exitCode=1;}finally{report.completedAt=new Date().toISOString();save();await browser?.close();}
