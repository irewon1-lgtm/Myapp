"""Actual Chromium: continuous reader, isolated examples and same-origin capstone.
This does not simulate a physical Galaxy installation or native updater receipt.
"""
from __future__ import annotations
from functools import partial
from http.server import SimpleHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
import json,os,shutil,threading
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'review';OUT.mkdir(exist_ok=True)
SOURCE=(ROOT/'live/reader.html').read_text()
COURSE=json.loads((ROOT/'live/content.json').read_text())
RESULTS=[];ERRORS=[]


def fixture(browser,width=360,height=800,state=None):
    page=browser.new_page(viewport={'width':width,'height':height},device_scale_factor=1)
    page.on('pageerror',lambda error:ERRORS.append(str(error)))
    bridge='window.testSaved='+json.dumps(json.dumps(state or {},ensure_ascii=False))+';window.RoadmapNative={getState:()=>window.testSaved,saveState:s=>{window.testSaved=s},getLegacy:()=>"{}",getStatus:()=>"검증용 브리지",ready:()=>{}};'
    page.set_content(SOURCE.replace('<script>','<script>'+bridge,1),wait_until='domcontentloaded')
    return page


def settle(page):
    page.wait_for_timeout(180)
    page.wait_for_function("route!=='reader'||document.body.dataset.qaReady==='1'",timeout=20000)


def open_book(page,track,number):
    page.evaluate("x=>{document.body.dataset.qaReady='0';openPage('t'+x[0]+'-book-c'+String(x[1]).padStart(2,'0'))}",[track,number]);settle(page)


def metrics(page):
    return page.evaluate("""(()=>{const p=document.getElementById('paper'),f=document.getElementById('flow');return {width:innerWidth,height:innerHeight,font:getComputedStyle(f).fontSize,pages:physicalPages,qa:Object.assign({},document.body.dataset),docWidth:document.documentElement.scrollWidth,bodyHeight:p.clientHeight,flowHeight:f.scrollHeight,track:trackSlideMap.track,entries:trackSlideMap.entries.length}})()""")


def record(label,**data):RESULTS.append(dict(test=label,status='PASS',**data))


def reader_tests(browser):
    for width,height in [(360,800),(800,1280),(1280,800)]:
        p=fixture(browser,width,height)
        for n in range(1,13):
            open_book(p,4,n);m=metrics(p)
            assert m['docWidth']<=width,(n,m)
            assert m['flowHeight']<=m['bodyHeight']+2,(n,m)
            assert m['qa']['qaFooterVisible']=='1',(n,m)
            assert m['track']==4 and m['entries']==12,(n,m)
            assert p.evaluate("document.querySelectorAll('.book-code').length===chapters[current.c].pages[0].book.samples.length")
            assert p.evaluate("[...document.querySelectorAll('.book-pre')].every((e,i)=>e.textContent===chapters[current.c].pages[0].book.samples[i].code.replace(/\\n/g,''))"),'Rendered code changed'
            assert p.evaluate("chapters[current.c].pages[0].book.exercises.every(e=>document.querySelector('[data-exercise=\"'+e.id+'\"]'))")
            assert p.evaluate("[...document.querySelectorAll('.reader-top>*')].every(e=>{const r=e.getBoundingClientRect();return r.left>=0&&r.right<=innerWidth+1})")
            record('book layout',chapter=n,viewport=[width,height],pages=m['pages'])
            if n in [1,7,12]:p.screenshot(path=str(OUT/f'track4-book-{width}x{height}-ch{n}.png'))
        p.close()
    p=fixture(browser);open_book(p,4,12)
    for font in [15,24,18]:
        p.evaluate("v=>{state.readerFont=v;document.querySelector('.reader-shell').style.setProperty('--readerBody',v+'px');trackSlideMap.ready=false;layoutReader()}",font);settle(p)
        m=metrics(p);assert m['font']==str(font)+'px' and m['flowHeight']<=m['bodyHeight']+2
        record('font resize',font=font)
    p.locator('.reader-more').click();p.locator('[data-action=toggle-dark]').click();settle(p)
    assert p.evaluate('state.readerDark')
    anchor=p.evaluate("chapters[current.c].pages[0].book.samples.find(s=>s.file==='app.js').anchor")
    p.evaluate("a=>doAction('book-jump',{dataset:{anchor:a}})",anchor);settle(p)
    p.evaluate('changePhysical(1)');settle(p)
    spot=p.evaluate('Track3Book.spot()');assert spot['physical']>0
    p.locator('[data-action=bookmark]').click()
    p.locator('[data-action=book-note]').click();p.locator('#book-note-input').fill('트랙4: 저장 전에 원본을 바꾸지 않는다.');p.locator('[data-action=close]').click()
    p.evaluate("state.bookAnswers['book-exercise-3-1']='트랙3의 답 보존';doAction('book-answer',{dataset:{exercise:'t4-book-exercise-12-1',title:'연습 12-1'}})")
    p.locator('#book-answer-input').fill('브라우저 저장과 기기 간 공유는 다르다.');p.locator('[data-action=close]').click()
    saved=json.loads(p.evaluate('window.testSaved'));assert saved['bookAnswers']['book-exercise-3-1']=='트랙3의 답 보존'
    p.screenshot(path=str(OUT/'track4-phone-dark-long-code.png'));p.close()
    p=fixture(browser,state=saved);p.evaluate("doAction('continue',{})");settle(p)
    assert p.evaluate('chapters[current.c].track')==4 and p.evaluate('physicalPage')==spot['physical']
    assert p.evaluate("state.bookAnswers['t4-book-exercise-12-1']")=='브라우저 저장과 기기 간 공유는 다르다.'
    p.locator('.reader-aa').click();p.locator('#reader-font-range').fill('24');settle(p)
    p.set_viewport_size({'width':800,'height':1280});settle(p);m=metrics(p);assert m['flowHeight']<=m['bodyHeight']+2
    p.evaluate("doAction('summary',{})");assert p.locator('.book-toc button').count()>=10
    p.locator('.book-toc button').last.click();settle(p);assert p.evaluate('physicalPage')>0
    p.screenshot(path=str(OUT/'track4-tablet-source-references.png'))
    p.evaluate('jumpToTrackSlide(1)');settle(p);assert p.evaluate('chapters[current.c].track')==4 and p.evaluate('chapters[current.c].chapter')==1
    p.evaluate('changePhysical(-1)');settle(p);assert p.evaluate('chapters[current.c].track')==3 and p.evaluate('chapters[current.c].chapter')==9
    assert p.evaluate('trackSlideMap.entries.length')==9
    p.evaluate('changePhysical(1)');settle(p);assert p.evaluate('chapters[current.c].track')==4 and p.evaluate('physicalPage')==0
    p.evaluate("doAction('track-current',{})");assert p.locator('.editorial-chapter-row').count()==12
    p.locator('.editorial-chapter-row').nth(8).click();settle(p);assert p.evaluate('chapters[current.c].chapter')==9 and p.evaluate('physicalPage')==0
    record('record persistence, notes, distinct answers, font, rotation, TOC, 3/4 boundary, all chapter links');p.close()


def practice_tests(browser):
    p=fixture(browser,800,1000);open_book(p,4,7)
    sid=p.evaluate("chapters[current.c].pages[0].book.samples.find(s=>s.mode==='console').id")
    p.evaluate("id=>Track4Web.openSample(id)",sid)
    p.locator('#web-execute').click()
    p.wait_for_function("document.getElementById('web-run-status').textContent!=='실행 중'",timeout=10000)
    assert p.locator('#web-console').inner_text().strip()=='30',p.locator('#web-console').inner_text()
    record('real JavaScript execution')
    p.locator('#web-code-input').fill('console.log(6 * 7);');p.locator('#web-execute').click()
    p.wait_for_function("document.getElementById('web-console').textContent.trim()==='42'",timeout=10000)
    record('edited JavaScript execution')
    p.locator('#web-code-input').fill('const value = ;');p.locator('#web-execute').click()
    p.wait_for_function("document.getElementById('web-console').textContent.includes('SyntaxError')",timeout=10000)
    record('real syntax error surfaced')
    p.locator('#web-code-input').fill('while (true) {}');p.locator('#web-execute').click()
    p.wait_for_function("document.getElementById('web-console').textContent.includes('제한 시간')",timeout=10000)
    assert p.evaluate('1+1')==2
    record('infinite JavaScript worker stopped; parent responsive')
    p.locator('#web-code-input').fill('console.log(typeof document, typeof RoadmapNative);');p.locator('#web-execute').click()
    p.wait_for_function("document.getElementById('web-console').textContent.includes('undefined undefined')",timeout=10000)
    record('worker has no document or native bridge')
    p.locator('#web-code-input').fill('fetch("https://example.com/").then(()=>console.log("UNEXPECTED")).catch(()=>console.log("blocked"));');p.locator('#web-execute').click()
    p.wait_for_function("document.getElementById('web-console').textContent.includes('blocked')",timeout=10000)
    assert 'UNEXPECTED' not in p.locator('#web-console').inner_text()
    record('worker external network blocked')
    open_book(p,4,8)
    sid=p.evaluate("chapters[current.c].pages[0].book.samples.find(s=>s.mode==='html'&&s.code.includes('클릭과 함수')).id")
    p.evaluate("id=>Track4Web.openSample(id)",sid);p.locator('#web-execute').click()
    f=p.frame_locator('.web-example-frame');f.locator('#change').click()
    assert f.locator('#status').inner_text()=='버튼을 눌렀습니다.'
    record('actual isolated HTML click event')
    sid=p.evaluate("chapters[current.c].pages[0].book.samples.find(s=>s.mode==='html'&&s.code.includes('record-form')).id")
    p.evaluate("id=>Track4Web.openSample(id)",sid);p.locator('#web-execute').click();f=p.frame_locator('.web-example-frame')
    f.locator('#topic').fill('HTML');f.locator('#minutes').fill('20');f.locator('button[type=submit]').click()
    assert f.locator('#records li').count()==1 and 'HTML · 20분' in f.locator('#records').inner_text()
    sandbox=p.locator('.web-example-frame').get_attribute('sandbox');assert 'allow-same-origin' not in sandbox
    record('actual isolated form submission; host origin not granted')
    p.screenshot(path=str(OUT/'track4-html-practice-working.png'))
    open_book(p,4,12);p.evaluate("doAction('practice',{})");assert p.locator('#web-execute').is_disabled()
    assert '부분 코드' in p.locator('.web-scope').inner_text() or '여러 파일' in p.locator('.web-scope').inner_text()
    record('multi-file capstone not mislabeled as standalone runnable');p.close()
    # Render every standalone HTML example in a genuine browser document.
    for c in COURSE['chapters']:
        if c['track']!=4:continue
        for sample in c['pages'][0]['book']['samples']:
            if sample['mode']!='html':continue
            p=browser.new_page(viewport={'width':360,'height':800});p.on('pageerror',lambda e:ERRORS.append(str(e)))
            p.set_content(sample['code']);assert p.locator('body').inner_text().strip()
            record('standalone HTML document',sample=sample['id']);p.close()


class QuietHandler(SimpleHTTPRequestHandler):
    def log_message(self,*args):pass


def capstone_tests(browser):
    server=ThreadingHTTPServer(('127.0.0.1',0),partial(QuietHandler,directory=str(ROOT/'live/track4-book/project')))
    threading.Thread(target=server.serve_forever,daemon=True).start()
    url=f'http://127.0.0.1:{server.server_port}/'
    context=browser.new_context(viewport={'width':360,'height':800},accept_downloads=True)
    p=context.new_page();p.on('pageerror',lambda e:ERRORS.append(str(e)));p.goto(url)
    def add(topic,minutes):
        p.locator('#topic').fill(topic);p.locator('#minutes').fill(str(minutes));p.locator('#add-record').click()
    add('HTML',20);add('CSS',30);assert p.locator('#records>.record').count()==2
    assert '총 50분' in p.locator('#summary').inner_text()
    p.reload();assert p.locator('#records>.record').count()==2
    record('capstone add and same-origin reload')
    for topic,minutes in [(' ',20),('zero',0),('high',601),('decimal',1.5)]:
        add(topic,minutes);assert p.locator('#records>.record').count()==2
    assert p.evaluate("parseInput('A','1').ok&&parseInput('A','600').ok&&!parseInput('A','').ok&&!parseInput('A','Infinity').ok")
    record('capstone blank, range, decimal and boundary validation')
    p.locator('#records>.record').first.locator('button').first.click();p.locator('#filter').select_option('completed')
    assert p.locator('#records>.record').count()==1 and 'HTML' in p.locator('#records').inner_text()
    p.locator('#records>.record').first.locator('button').nth(1).click();p.locator('#filter').select_option('all')
    assert p.locator('#records>.record').count()==1 and 'CSS' in p.locator('#records').inner_text()
    p.reload();assert p.locator('#records>.record').count()==1 and 'CSS' in p.locator('#records').inner_text()
    record('completion filter and identity-based deletion persist')
    add('<img src=x onerror=alert(1)>',12)
    assert p.locator('#records img').count()==0 and '<img' in p.locator('#records').inner_text()
    record('user text not parsed as HTML')
    before=p.evaluate("localStorage.getItem('study-log-v1')")
    with p.expect_download() as info:p.locator('#export-records').click()
    downloaded=info.value;dest=OUT/'track4-capstone-backup.txt';downloaded.save_as(dest)
    assert dest.read_text()==before
    record('actual backup download matches stored bytes')
    p.once('dialog',lambda d:d.dismiss());p.locator('#reset-records').click()
    assert p.evaluate("localStorage.getItem('study-log-v1')")==before
    record('reset cancellation preserves records')
    p.evaluate("localStorage.setItem('unrelated-sentinel','keep')")
    p.once('dialog',lambda d:d.accept());p.locator('#reset-records').click()
    assert p.evaluate("localStorage.getItem('unrelated-sentinel')")=='keep'
    assert p.evaluate("localStorage.getItem('study-log-v1')") is None
    record('confirmed reset removes only own key')
    p.evaluate("localStorage.setItem('study-log-v1','{broken original')");p.reload()
    assert p.locator('#add-record').is_disabled()
    assert p.evaluate("localStorage.getItem('study-log-v1')")=='{broken original'
    with p.expect_download() as info:p.locator('#export-records').click()
    dest=OUT/'track4-corrupt-source-preserved.txt';info.value.save_as(dest);assert dest.read_text()=='{broken original'
    record('corrupt storage blocks edits, preserves and exports original')
    p.evaluate("localStorage.removeItem('study-log-v1')");p.reload();add('기존 기록',15)
    old=p.evaluate("localStorage.getItem('study-log-v1')")
    p.evaluate("window.realSetItem=Storage.prototype.setItem;Storage.prototype.setItem=function(){throw new DOMException('test full','QuotaExceededError')}")
    add('실패해도 남길 입력',20)
    assert p.locator('#topic').input_value()=='실패해도 남길 입력'
    assert p.locator('#records>.record').count()==1 and p.evaluate("localStorage.getItem('study-log-v1')")==old
    p.evaluate('Storage.prototype.setItem=window.realSetItem')
    record('storage failure retains original state and typed input')
    p.locator('#load-topics').click();p.wait_for_function("document.querySelectorAll('#topics-list li').length===3")
    record('same-origin real JSON fetch')
    for label,body,status in [('empty','[]',200),('http404','not found',404),('invalid-json','{broken',200),('wrong-shape','{"message":"unavailable"}',200)]:
        p.route('**/topics.json',lambda route,b=body,s=status:route.fulfill(status=s,content_type='application/json',body=b))
        p.locator('#load-topics').click();p.wait_for_function("!document.getElementById('load-topics').disabled")
        text=p.locator('#topics-message').inner_text()
        assert ('제공되는 주제가 없습니다' in text) if label=='empty' else ('못했습니다' in text),(label,text)
        record('request handling',scenario=label);p.unroute('**/topics.json')
    p.locator('#load-topics').click();p.wait_for_function("document.querySelectorAll('#topics-list li').length===3")
    pending=[];p.route('**/topics.json',lambda route:pending.append(route))
    p.locator('#load-topics').click();p.wait_for_function("document.getElementById('topics-message').textContent.includes('시간이 길어')",timeout=10000)
    assert p.locator('#load-topics').is_enabled() and p.locator('#topics-list li').count()==3
    for route in pending:
        try:route.abort()
        except Exception:pass
    p.unroute('**/topics.json');p.locator('#load-topics').click();p.wait_for_function("document.getElementById('topics-message').textContent.includes('3개')")
    record('request timeout, old data preservation and retry')
    for width,height in [(360,800),(800,1280),(1280,800)]:
        p.set_viewport_size({'width':width,'height':height});p.wait_for_timeout(100)
        assert p.evaluate('document.documentElement.scrollWidth<=innerWidth')
        record('capstone responsive viewport',viewport=[width,height])
    p.emulate_media(color_scheme='dark');p.set_viewport_size({'width':360,'height':800})
    p.screenshot(path=str(OUT/'track4-capstone-mobile-dark.png'),full_page=True)
    context.close();server.shutdown()


def main():
    with sync_playwright() as pw:
        executable=os.environ.get('CHROME') or shutil.which('chromium') or shutil.which('google-chrome') or shutil.which('google-chrome-stable')
        assert executable,'Chromium required'
        browser=pw.chromium.launch(executable_path=executable,headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
        try:
            reader_tests(browser);practice_tests(browser);capstone_tests(browser)
            assert not ERRORS,ERRORS
        finally:
            browser.close()
            report={'status':'PASS' if not ERRORS and len(RESULTS)>60 else 'INCOMPLETE','scope':'Real Chromium, native bridge emulator, isolated web execution and local HTTP capstone. NOT physical Galaxy installation or updater receipt.','cases':RESULTS,'consoleErrors':ERRORS}
            (OUT/'track4-book-browser-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'status':'PASS','cases':len(RESULTS),'consoleErrors':ERRORS}))

if __name__=='__main__':main()
