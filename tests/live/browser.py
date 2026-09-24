"""Actual Chromium tests against a local same-origin gateway and the real Chatbook bundle.
Only synthetic reading records are used. No production writes, no video/API generation.
"""
import json, os, pathlib, subprocess, time, urllib.request, traceback
from playwright.sync_api import sync_playwright
ROOT=pathlib.Path(__file__).resolve().parents[2]; os.chdir(ROOT)
OUT=ROOT/'review'/'live-browser'; OUT.mkdir(parents=True,exist_ok=True)
report={'scope':'Actual Chromium browser with production Chatbook source and isolated upstream/API fault injection; not physical Galaxy.', 'checks':[], 'status':'running'}
def record(name,ok,detail=None):
    report['checks'].append({'name':name,'passed':bool(ok),'detail':detail})
    (OUT/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(('PASS ' if ok else 'FAIL ')+name, flush=True)
    if not ok: raise AssertionError(name+': '+str(detail))
server=subprocess.Popen(['node','tests/live/server.mjs'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
line=server.stdout.readline().strip(); print(line,flush=True); assert line.startswith('LOCAL_LIVE_TEST_PORT='),line
base='http://127.0.0.1:'+line.split('=')[1]
def control(mode,failPath=''):
    req=urllib.request.Request(base+'/__test/control',data=json.dumps({'mode':mode,'failPath':failPath}).encode(),headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req) as r:return json.load(r)
def get(path):
    with urllib.request.urlopen(base+path) as r:return r.status,dict(r.headers),r.read()
def wait_js(page, expression, arg=None, timeout=30000):
    deadline=time.monotonic()+timeout/1000
    last_error=None
    while time.monotonic()<deadline:
        try:
            if page.evaluate(expression, arg):
                return
        except Exception as error:
            last_error=error
        time.sleep(0.1)
    raise AssertionError('Browser condition timed out: '+expression+'; '+str(last_error))

def open_page(page,url=None):
    try: page.goto(url or base+'/',wait_until='domcontentloaded',timeout=45000)
    except Exception as e:
        if 'interrupted' not in str(e) and 'ERR_ABORTED' not in str(e):raise
    wait_js(page,'window.CB?.catalog?.books?.length > 0 && window.ChatbookLive?.pending === false',timeout=45000)
    page.wait_for_timeout(300)
try:
 with sync_playwright() as p:
    opts={'headless':True,'args':['--no-sandbox']}
    if os.environ.get('BROWSER_EXE'):opts['executable_path']=os.environ['BROWSER_EXE']
    browser=p.chromium.launch(**opts)
    ctx=browser.new_context(viewport={'width':412,'height':915})
    ctx.route('https://fonts.**/*',lambda route: route.abort())
    page=ctx.new_page(); errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
    control('a');open_page(page)
    record('approved release A booted',page.evaluate('ChatbookLive.commit')=='a'*40)
    record('same origin remains unchanged',page.evaluate('location.origin')==base)
    record('production-only script policy retained',"script-src 'self'" in get('/')[1].get('content-security-policy',''))
    record('PWA manifest stays at stable origin path',page.locator('link[rel=manifest]').get_attribute('href')=='/manifest.webmanifest')
    expected=len(json.loads((ROOT/'public/content/catalog.json').read_text())['books'])
    record('all existing books present',page.evaluate('CB.catalog.books.length')==expected,expected)
    book=page.evaluate('CB.catalog.books[0].id')
    page.evaluate('(id)=>CB.read(id)',book)
    wait_js(page,'CB.Reader.pages >= 1 && CB.Reader.measured')
    for width in [360,412,800,1280]:
        page.set_viewport_size({'width':width,'height':915});page.wait_for_timeout(350)
        record('reader fits width '+str(width),page.evaluate('document.documentElement.scrollWidth <= innerWidth+1'))
    page.set_viewport_size({'width':412,'height':915});page.wait_for_timeout(300)
    page.evaluate("CB.setPref('theme','dark'); CB.applyPrefs(); CB.Reader.applySettings();")
    record('dark theme still works',page.evaluate('document.body.dataset.theme')=='dark')
    page.evaluate('CB.Reader.bookmark()');page.wait_for_timeout(250)
    page.evaluate("CB.set('annotation:note:live-test',{kind:'note',bookId:CB.Reader.book.id,anchor:CB.Reader.book.chapters[0].blocks[0].id,text:'KEEP_NOTE_123'}); CB.set('custom:migration-test',{keep:true});")
    before=page.evaluate("({note:CB.get('annotation:note:live-test'),marker:CB.get('custom:migration-test'),bookmarks:CB.annotations(CB.Reader.book.id,'bookmark').length})")
    record('synthetic note and bookmark saved',before['note']['text']=='KEEP_NOTE_123' and before['bookmarks']>0)
    await_js="""async()=>{const b={id:'import-live-test',title:'MY_PRIVATE_IMPORTED_BOOK',imported:true,chapters:[{id:'imp-c',title:'내 장',blocks:[{id:'imp-p',type:'p',text:'PRIVATE_BOOK_RETAINED'}]}],questions:[]};CB.upsertBook(b);await CB.filePut('catalog',CB.catalog);const c=await caches.open('user-downloads');await c.put('/private-saved.txt',new Response('USER_FILE_KEEP'));} """
    page.evaluate(await_js)
    page.screenshot(path=str(OUT/'reader-dark-412.png'),full_page=True)
    wait_js(page,'ChatbookLive.offlineReady === true',timeout=45000)
    record('complete version A offline cache committed after integrity checks',True)
    record('normal reader raised no JavaScript errors',not errors,errors[:5])
    # Updating publication does NOT swap the currently open reader or invalidate note anchors.
    control('b');page.evaluate('ChatbookLive.check(false)');page.wait_for_timeout(800)
    record('open reader not hot-swapped while reading',page.evaluate('ChatbookLive.commit')=='a'*40)
    open_page(page)
    record('release B UI update works without Netlify deployment',page.evaluate('window.__LIVE_TEST_EDITION')=='B')
    record('new book published without Netlify deployment',page.evaluate("CB.catalog.books.some(b=>b.id==='live-fixture-new-book'&&b.title==='LIVE_NEW_BOOK_CONFIRMED')"))
    record('chapter update delivered with the same verified version',page.evaluate("CB.catalog.books[0].chapters[0].title")=='LIVE_CHAPTER_UPDATE_CONFIRMED')
    record('body update delivered with the same verified version',page.evaluate("CB.catalog.books[0].chapters[0].blocks[0].text")=='LIVE_BODY_UPDATE_CONFIRMED')
    record('existing note survived UI update',page.evaluate("CB.get('annotation:note:live-test').text")=='KEEP_NOTE_123')
    record('private imported book survives update',page.evaluate("CB.catalog.books.some(b=>b.id==='import-live-test'&&b.title==='MY_PRIVATE_IMPORTED_BOOK')"))
    record('unrelated cached downloads retained',page.evaluate("async()=>await(await(await caches.open('user-downloads')).match('/private-saved.txt')).text()")=='USER_FILE_KEEP')
    for route in ['home','library','quiz','settings']:
        page.evaluate('(r)=>CB.go(r)',route);page.wait_for_timeout(100)
        record('existing screen remains usable: '+route,page.evaluate('document.querySelector("#app").innerText.length>10'))
    wait_js(page,'ChatbookLive.offlineReady === true',timeout=45000)
    record('version B becomes complete offline version',True)
    # Same-origin API error does not remove private local data.
    api=page.evaluate("async()=>{try{await CB.api('sync/pull');return false}catch{return CB.get('custom:migration-test').keep}}")
    record('sync authentication failure leaves local records untouched',api)
    # Invalid release is hash-valid, but deliberately contains a syntax error.
    control('c');open_page(page)
    record('bad JavaScript rolls back to previous healthy B',page.evaluate('ChatbookLive.commit')=='b'*40,page.url)
    record('failed startup did not erase notes',page.evaluate("CB.get('annotation:note:live-test').text")=='KEEP_NOTE_123')
    control('d');open_page(page)
    record('corrupt bytes rejected and previous B retained',page.evaluate('ChatbookLive.commit')=='b'*40)
    control('e');open_page(page)
    record('partial download rolls back to previous B',page.evaluate('ChatbookLive.commit')=='b'*40)
    control('f');open_page(page)
    record('future incompatible shell cannot replace healthy reader',page.evaluate('ChatbookLive.commit') in ['b'*40,'bundled'])
    control('malformed');open_page(page)
    record('malformed channel leaves a usable reader',page.evaluate('CB.catalog.books.length')>=expected)
    # A stale upstream response cannot silently downgrade the last accepted release.
    control('a');open_page(page)
    record('stale channel does not downgrade accepted B',page.evaluate('ChatbookLive.commit')=='b'*40)
    control('b');open_page(page);wait_js(page,'ChatbookLive.offlineReady === true',timeout=45000)
    ctx.set_offline(True);open_page(page)
    record('offline restart loads complete cached release',page.evaluate('ChatbookLive.commit')=='b'*40)
    record('offline notes remain readable',page.evaluate("CB.get('annotation:note:live-test').text")=='KEEP_NOTE_123')
    unavailable=page.evaluate("async()=>{try{const r=await fetch('/engine-spec.json',{cache:'no-store'});return !r.ok;}catch{return true;}}")
    record('offline does not pretend old engine-spec is latest',unavailable)
    page.evaluate('(id)=>CB.read(id)',book);page.wait_for_timeout(350)
    record('offline chapter navigation works',page.evaluate('CB.Reader.pages >= 1'))
    page.screenshot(path=str(OUT/'offline-reader-412.png'),full_page=True)
    ctx.set_offline(False);ctx.close()
    # Current-spec endpoint works for an AI/HTTP reader with JavaScript disabled.
    control('1');static=browser.new_context(java_script_enabled=False,viewport={'width':360,'height':800});sp=static.new_page()
    sp.goto(base+'/engine-spec.html');record('current authoring specification readable without JavaScript','대표 본문 예시' in sp.inner_text('body'))
    sp.screenshot(path=str(OUT/'authoring-spec-360.png'),full_page=True);static.close()
    report['status']='passed';report['intentionalJavaScriptFaults']=errors
    browser.close()
except Exception as e:
 report['status']='failed';report['failure']=str(e);report['traceback']=traceback.format_exc();print(report['traceback'],flush=True)
finally:
 report['completedAt']=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime())
 (OUT/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
 server.terminate()
raise SystemExit(0 if report['status']=='passed' else 1)
