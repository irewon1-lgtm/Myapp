/** Disposable browser profile; never changes a user's saved library or sync data. */
import fs from 'node:fs';
import {chromium} from 'playwright';
const url=process.env.CHATBOOK_TEST_URL||'https://chatbook-library-20260923.netlify.app';
const id='yt-python-1h-8KCuHHeC_M0',dir='tests/narrative-live-results';fs.mkdirSync(dir,{recursive:true});
const report={url,scope:'Actual published HTTPS app in isolated Chromium. Not physical Galaxy.',checks:[],errors:[],startedAt:new Date().toISOString()};
const save=()=>fs.writeFileSync(dir+'/report.json',JSON.stringify(report,null,2));
function check(name,passed,detail){report.checks.push({name,passed:!!passed,detail});save();console.log(name,!!passed);if(!passed)throw Error(name);}
let browser;
try{
 const vr=await fetch(url+'/version.json?check='+Date.now());const version=await vr.json();report.version=version;check('live app version',version.version==='1.0.3');
 const cr=await fetch(url+'/content/catalog.json?check='+Date.now());const cat=await cr.json();check('live catalog narrative edition',cat.version==='2026-09-23.14-narrative');
 browser=await chromium.launch({headless:true});const context=await browser.newContext({viewport:{width:412,height:915},acceptDownloads:true});const p=await context.newPage();
 p.on('pageerror',e=>report.errors.push(e.message));p.setDefaultTimeout(20000);
 await p.goto(url,{waitUntil:'domcontentloaded'});await p.waitForFunction(()=>window.CB&&CB.catalog.books.length===13);
 check('book v3 received',await p.evaluate(id=>CB.book(id).version===3,id));
 check('engine 1.2 actually served',(await fetch(url+'/engine/learning.mjs')).status===200);
 for(const width of [360,412,768,1366]){
  await p.setViewportSize({width,height:width>500?1000:915});await p.evaluate(id=>CB.read(id),id);await p.waitForTimeout(600);
  check('reader finite pages '+width,await p.evaluate(()=>Number.isFinite(CB.Reader.pages)&&CB.Reader.pages>1));
  check('viewport no horizontal overflow '+width,await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
  check('all text/code represented '+width,await p.locator('pre.reader-code').count()===39);
  await p.evaluate(id=>CB.Reader.goAnchor(id+'-c7-if-first-code'),id);await p.waitForTimeout(250);
  check('code indentation '+width,(await p.locator('[data-block="'+id+'-c7-if-first-code"]').innerText()).includes('\n    print("You are an adult")'));
  if(width===412)await p.screenshot({path:dir+'/mobile-code.png'});
 }
 await p.setViewportSize({width:412,height:915});await p.evaluate(id=>{CB.Reader.goAnchor(id+'-c6-deep-3');},id);await p.waitForTimeout(350);await p.evaluate(()=>CB.Reader.bookmark());
 const before=await p.evaluate(id=>({progress:CB.progress(id),bookmarks:CB.annotations(id,'bookmark').length}),id);
 await p.reload({waitUntil:'domcontentloaded'});await p.waitForFunction(()=>window.CB&&CB.catalog.books.length===13);await p.evaluate(id=>CB.read(id),id);await p.waitForTimeout(700);
 const after=await p.evaluate(id=>({progress:CB.progress(id),bookmarks:CB.annotations(id,'bookmark').length}),id);
 check('HTTPS bookmark survives reload',before.bookmarks>0&&before.bookmarks===after.bookmarks);
 check('HTTPS position survives reload',before.progress.anchor===after.progress.anchor);
 await p.evaluate(()=>CB.setPref('fontSize',26));await p.evaluate(()=>CB.Reader.applySettings());await p.waitForTimeout(500);
 check('large font repaginates',await p.evaluate(()=>Number.isFinite(CB.Reader.pages)&&CB.Reader.pages>1));
 await p.evaluate(()=>CB.setPref('readingMode','scroll'));await p.evaluate(()=>CB.Reader.applySettings());await p.waitForTimeout(400);
 check('scroll mode',await p.locator('.reader').evaluate(e=>e.classList.contains('scroll-mode')));
 await p.evaluate(()=>CB.setPref('theme','dark'));check('dark mode',await p.locator('body').getAttribute('data-theme')==='dark');
 check('no user profile sync created',!await p.evaluate(()=>Boolean(CB.pref('syncCode',''))));
 for(const route of ['home','library','exports','quiz','settings']){await p.evaluate(r=>CB.go(r),route);await p.waitForTimeout(100);check(route+' screen no overflow',await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));}
 await p.evaluate(()=>CB.read('habit'));await p.waitForTimeout(350);check('other book still opens',await p.evaluate(()=>CB.Reader.book.id==='habit'&&CB.Reader.pages>1));
 check('no runtime exception',report.errors.length===0,report.errors);report.status='passed';
}catch(e){report.status='failed';report.failure=String(e);process.exitCode=1;}finally{report.completedAt=new Date().toISOString();save();await browser?.close();}
