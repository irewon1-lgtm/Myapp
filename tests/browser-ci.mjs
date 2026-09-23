/** Live production browser checks. Uses only disposable test profiles and a random sync collection. */
import fs from 'node:fs';
import { chromium } from 'playwright';
const url='https://chatbook-library-20260923.netlify.app';
const report={startedAt:new Date().toISOString(),environment:'GitHub-hosted Chromium against the published Netlify HTTPS app; not physical Galaxy hardware',url,checks:[],runtimeErrors:[],failures:[]};
fs.mkdirSync('tests/screenshots',{recursive:true});
const save=()=>fs.writeFileSync('tests/ci-report.json',JSON.stringify(report,null,2));
const check=(name,value,details)=>{report.checks.push({name,passed:!!value,...(details?{details}:{})});console.log(name,!!value);save();if(!value)throw Error('Check failed: '+name);};
const pause=ms=>new Promise(r=>setTimeout(r,ms));
let browser,contextA,pageA,contextB,pageB,syncCreated=false;
try{
 let ready=false;
 for(let i=0;i<30;i++){try{const r=await fetch(url);if(r.ok&&(await r.text()).includes('id="app"')){ready=true;break}}catch{}await pause(4000)}
 check('Published HTTPS document',ready);
 browser=await chromium.launch({headless:true});
 contextA=await browser.newContext({viewport:{width:412,height:915},acceptDownloads:true});pageA=await contextA.newPage();pageA.setDefaultTimeout(18000);
 pageA.on('pageerror',e=>report.runtimeErrors.push(e.message));
 await pageA.goto(url,{waitUntil:'domcontentloaded'});await pageA.waitForFunction(()=>window.CB&&CB.catalog?.books?.length===9&&document.querySelector('.book-card'));
 await pageA.evaluate(()=>{clearTimeout(CB.toastTimer);document.querySelector('#toast').innerHTML=''});
 check('Real browser persistent storage',await pageA.evaluate(()=>CB.persistent));
 check('Nine original demonstration books',await pageA.evaluate(()=>CB.catalog.books.length===9));
 for(const width of [360,412,768,1366]){
  await pageA.setViewportSize({width,height:width===768?1024:915});
  for(const route of ['home','library','exports','quiz','settings','review']){
   await pageA.evaluate(r=>CB.go(r),route);await pageA.waitForTimeout(140);
   check(`${route} ${width}px no horizontal overflow`,await pageA.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
   if(width===412)await pageA.screenshot({path:`tests/screenshots/${route}.png`,fullPage:true});
  }
 }
 await pageA.setViewportSize({width:412,height:915});await pageA.evaluate(()=>CB.go('library'));await pageA.locator('#shelf-search').fill('대화');
 check('Library title search',await pageA.locator('.book-card').count()>0&&await pageA.locator('.book-card').count()<9);
 await pageA.locator('#shelf-search').fill('ZZ-NOTHING-1234');check('Empty library search state',await pageA.locator('.book-card').count()===0);await pageA.locator('#shelf-search').fill('');
 await pageA.evaluate(()=>CB.read('habit'));await pageA.waitForTimeout(800);check('Reader measured pagination',await pageA.evaluate(()=>CB.Reader.pages>5));
 await pageA.locator('[data-action="reader-next"]').click();await pageA.waitForTimeout(250);
 check('Next page saved',await pageA.evaluate(()=>CB.progress('habit').page===1));
 await pageA.locator('#bottom-bookmark').click();await pageA.locator('[data-action="reader-note"]').click();await pageA.locator('#note-text').fill('CI 시험 메모 · 본문 위치와 함께 저장');await pageA.locator('[data-action="save-note"]').click();
 check('Bookmark and note stored',await pageA.evaluate(()=>CB.bookmarks('habit').length===1&&CB.annotations('habit','note').length===1));
 await pageA.reload({waitUntil:'domcontentloaded'});await pageA.waitForFunction(()=>window.CB&&CB.Reader?.pages>1);await pageA.waitForTimeout(700);
 check('Page position survives real reload',await pageA.evaluate(()=>CB.progress('habit').page>=1));
 check('Bookmark and note survive real reload',await pageA.evaluate(()=>CB.bookmarks('habit').length===1&&CB.annotations('habit','note')[0].text.includes('CI 시험')));
 await pageA.evaluate(()=>CB.showToc('habit'));await pageA.locator('[data-action="toc-go"]').nth(2).click();await pageA.waitForTimeout(300);check('TOC opens correct chapter',await pageA.evaluate(()=>CB.Reader.info().chapter===2));
 await pageA.evaluate(()=>CB.showReadingSettings());await pageA.locator('#font-size').fill('25');await pageA.waitForTimeout(400);await pageA.locator('[data-action="modal-close"]').first().click();check('Font reflow retains chapter',await pageA.evaluate(()=>CB.Reader.info().chapter===2));
 await pageA.screenshot({path:'tests/screenshots/reader.png',fullPage:true});
 await pageA.evaluate(()=>{CB.setPref('fontSize',18);CB.Reader.applySettings();CB.startQuiz('ai')});await pageA.waitForTimeout(150);
 for(let qi=0;qi<3;qi++){
  const answer=await pageA.evaluate(()=>CB.selected().questions[CB.quizState().index].answer),selection=qi===0?(answer+1)%4:answer;
  await pageA.locator(`[data-action="quiz-choice"][data-value="${selection}"]`).click();await pageA.locator('[data-action="quiz-submit"]').click();await pageA.locator('[data-action="quiz-next"]').click();
 }
 check('Quiz actual 2 out of 3 score',(await pageA.locator('.result-score').innerText()).replaceAll(' ','')==='2/3');
 await pageA.evaluate(()=>CB.go('review'));check('Wrong answer enters review queue',await pageA.evaluate(()=>CB.reviewItems().length>=1));
 await pageA.locator('[data-action="review-start"]').first().click();await pageA.locator('[data-action="review-reveal"]').click();await pageA.locator('[data-action="review-rate"][data-value="easy"]').click();check('Review answer rescheduled',await pageA.evaluate(()=>CB.reviewItems().length===0));
 await pageA.evaluate(()=>CB.setPref('theme','dark'));await pageA.reload({waitUntil:'domcontentloaded'});await pageA.waitForFunction(()=>window.CB&&document.body.dataset.theme==='dark');check('Theme survives real reload',true);await pageA.evaluate(()=>CB.setPref('theme','cream'));
 await pageA.evaluate(()=>{CB.select('habit');CB.go('exports')});
 for(const kind of ['pdf','summary','print','pptx']){
  const pending=pageA.waitForEvent('download',{timeout:60000});await pageA.evaluate(k=>CB.exportBook(k,'habit'),kind);const d=await pending;const path=`tests/download-${kind}.${kind==='pptx'?'pptx':'pdf'}`;await d.saveAs(path);const bytes=fs.readFileSync(path);
  check(`Real ${kind} file download`,kind==='pptx'?bytes.subarray(0,2).toString()==='PK':bytes.subarray(0,5).toString()==='%PDF-',`${bytes.length} bytes`);
 }
 check('Four stored output records',await pageA.evaluate(()=>CB.all('export:').length===4));
 await pageA.reload({waitUntil:'domcontentloaded'});await pageA.waitForFunction(()=>window.CB?.Exporter);
 const exkey=await pageA.evaluate(()=>CB.all('export:')[0].key);const reopen=pageA.waitForEvent('download',{timeout:15000});await pageA.evaluate(key=>CB.openExport(key),exkey);const reopened=await reopen;check('IndexedDB output survives real reload',(await reopened.suggestedFilename()).length>4);
 const backupEvent=pageA.waitForEvent('download');await pageA.evaluate(()=>CB.backup());const backup=await backupEvent;await backup.saveAs('tests/backup-test.chatbook');const back=JSON.parse(fs.readFileSync('tests/backup-test.chatbook','utf8'));check('Backup contains reader records',back.app==='chatbook'&&Object.keys(back.records).some(k=>k.startsWith('annotation:')));
 await pageA.evaluate(()=>CB.offlineAll());check('Offline cache save state',await pageA.evaluate(()=>!!CB.get('offline:saved',false)));
 await pageA.waitForFunction(()=>!!navigator.serviceWorker.controller);await contextA.setOffline(true);await pageA.reload({waitUntil:'domcontentloaded'});await pageA.waitForFunction(()=>window.CB?.catalog?.books?.length===9);check('Offline reopening contains all nine books',true);await pageA.evaluate(()=>CB.read('habit'));await pageA.waitForTimeout(500);check('Offline page reader operates',await pageA.evaluate(()=>CB.Reader.pages>5));await contextA.setOffline(false);
 const unauth=await fetch(url+'/api/sync');check('Sync rejects unauthenticated requests',unauth.status===401);
 await pageA.evaluate(()=>CB.startSync());syncCreated=true;
 await pageA.evaluate(async()=>{CB.set('annotation:ci-cross-device',{kind:'note',bookId:'habit',anchor:'habit-c1',text:'DISPOSABLE CROSS DEVICE NOTE'});await CB.sync()});
 const secret=await pageA.evaluate(()=>CB.secret());
 contextB=await browser.newContext({viewport:{width:412,height:915}});pageB=await contextB.newPage();pageB.setDefaultTimeout(20000);pageB.on('pageerror',e=>report.runtimeErrors.push(e.message));await pageB.goto(url,{waitUntil:'domcontentloaded'});await pageB.waitForFunction(()=>window.CB?.startSync);await pageB.evaluate(code=>CB.startSync('CBK-'+code),secret);
 check('Second isolated browser decrypts shared note',await pageB.evaluate(()=>CB.get('annotation:ci-cross-device')?.text==='DISPOSABLE CROSS DEVICE NOTE'));
 await pageB.evaluate(async()=>{CB.set('annotation:ci-cross-device',{...CB.get('annotation:ci-cross-device'),text:'DISPOSABLE EDIT FROM SECOND DEVICE'});await CB.sync()});await pageA.evaluate(()=>CB.sync());
 check('Second-device edit synchronizes back',await pageA.evaluate(()=>CB.get('annotation:ci-cross-device')?.text==='DISPOSABLE EDIT FROM SECOND DEVICE'));
 check('Server stores encrypted payload only',await pageA.evaluate(async()=>{const raw=JSON.stringify(await CB.api('sync'));return !raw.includes('DISPOSABLE')&&raw.includes('"iv"')&&raw.includes('"data"')}));
 await pageA.evaluate(()=>{clearTimeout(CB.syncTimer);CB.setPref('sync',false);clearTimeout(CB.syncTimer)});await pageB.evaluate(()=>{clearTimeout(CB.syncTimer);CB.setPref('sync',false);clearTimeout(CB.syncTimer)});await pageA.waitForFunction(()=>!CB.syncing);await pageB.waitForFunction(()=>!CB.syncing);await pause(500);await pageA.evaluate(()=>CB.api('sync',{method:'DELETE'}));
 let cleaned=false;for(let i=0;i<20;i++){if(await pageA.evaluate(async()=>(await CB.api('sync')).items.length===0)){cleaned=true;break}await pause(3000)}
 check('Disposable sync collection cleaned',cleaned);syncCreated=false;
 check('No application JavaScript exceptions',report.runtimeErrors.length===0,report.runtimeErrors.length+' runtime errors');
 report.status='passed';
}catch(error){report.status='failed';report.failures.push(String(error.message));console.error('Live browser verification failed:',error.message);process.exitCode=1;
}finally{
 if(syncCreated&&pageA)try{await pageA.evaluate(()=>{clearTimeout(CB.syncTimer);CB.setPref('sync',false);clearTimeout(CB.syncTimer);return CB.api('sync',{method:'DELETE'})})}catch{}
 for(const filename of ['backup-test.chatbook','download-pdf.pdf','download-summary.pdf','download-print.pdf','download-pptx.pptx'])try{fs.unlinkSync('tests/'+filename)}catch{}
 if(browser)await browser.close();report.completedAt=new Date().toISOString();save();
}
