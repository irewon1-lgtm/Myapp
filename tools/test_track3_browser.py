"""Real Chromium layout and interactions; no physical-device claim."""
from pathlib import Path
import json,os,shutil
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'review';OUT.mkdir(exist_ok=True)
SOURCE=(ROOT/'live/reader.html').read_text()
ERRORS=[];RESULTS=[]
def fixture(browser,width=360,height=800,state=None):
    page=browser.new_page(viewport={'width':width,'height':height},device_scale_factor=1)
    page.on('pageerror',lambda e:ERRORS.append(str(e)))
    bridge="window.testSaved="+json.dumps(json.dumps(state or {},ensure_ascii=False))+";window.RoadmapNative={getState:()=>window.testSaved,saveState:s=>{window.testSaved=s},getLegacy:()=> '{}',getStatus:()=> '검증용 브리지',ready:()=>{}};"
    page.set_content(SOURCE.replace('<script>','<script>'+bridge,1),wait_until='domcontentloaded')
    return page
def settle(page):
    page.wait_for_timeout(130)
    page.wait_for_function("route!=='reader'||document.body.dataset.qaReady==='1'",timeout=12000)
def open_chapter(page,num):
    page.evaluate("n=>{document.body.dataset.qaReady='0';openPage('t3-book-c'+String(n).padStart(2,'0'))}",num);settle(page)
def metrics(page):
    return page.evaluate("""(()=>{const p=document.getElementById('paper'),f=document.getElementById('flow');return {width:innerWidth,height:innerHeight,font:getComputedStyle(f).fontSize,pages:physicalPages,qa:Object.assign({},document.body.dataset),docWidth:document.documentElement.scrollWidth,bodyHeight:p.clientHeight,flowHeight:f.scrollHeight,toolbar:[...document.querySelectorAll('.reader-top>*')].map(e=>({left:e.getBoundingClientRect().left,right:e.getBoundingClientRect().right,bottom:e.getBoundingClientRect().bottom})),maxCodeHeight:[...document.querySelectorAll('.book-code-line')].reduce((m,e)=>Math.max(m,e.getBoundingClientRect().height),0)}})()""")
def main():
  with sync_playwright() as pw:
    executable=os.environ.get('CHROME') or shutil.which('chromium') or shutil.which('google-chrome') or shutil.which('google-chrome-stable')
    assert executable,'Chromium required'
    browser=pw.chromium.launch(executable_path=executable,headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
    for width,height in [(360,800),(800,1280),(1280,800)]:
      page=fixture(browser,width,height)
      for n in range(1,10):
        open_chapter(page,n);m=metrics(page)
        assert m['docWidth']<=width,(n,width,m)
        assert m['flowHeight']<=m['bodyHeight']+2,(n,width,m)
        assert m['qa']['qaFooterVisible']=='1',(n,width,m)
        assert m['qa']['qaBook']=='continuous-v1'
        assert m['maxCodeHeight']<=m['bodyHeight'],(n,width,'single code line taller than page')
        assert all(x['left']>=0 and x['right']<=width+1 and x['bottom']<130 for x in m['toolbar']),('toolbar clipped',m['toolbar'])
        assert page.evaluate("document.querySelectorAll('.book-code').length===chapters[current.c].pages[0].book.samples.length")
        assert page.evaluate("[...document.querySelectorAll('.book-pre')].every((e,i)=>e.textContent===chapters[current.c].pages[0].book.samples[i].code.replace(/\\n/g,''))"),'Code text changed by rendering'
        assert page.evaluate("(()=>{const ex=chapters[current.c].pages[0].book.exercises;return ex.every(x=>document.querySelector('[data-exercise=\"'+x.id+'\"]'))})()")
        RESULTS.append({'chapter':n,'viewport':[width,height],'font':18,'pages':m['pages'],'status':'PASS'})
        if n in [1,3,9]:page.screenshot(path=str(OUT/f'book-{width}x{height}-chapter{n}.png'))
      page.close()
    page=fixture(browser);open_chapter(page,9)
    for size in [15,24,18]:
      page.evaluate("v=>{state.readerFont=v;document.querySelector('.reader-shell').style.setProperty('--readerBody',v+'px');trackSlideMap.ready=false;layoutReader()}",size)
      settle(page);m=metrics(page)
      assert m['font']==f'{size}px';assert m['flowHeight']<=m['bodyHeight']+2
      RESULTS.append({'chapter':9,'viewport':[360,800],'font':size,'status':'PASS'})
    page.locator('.reader-more').click();assert page.locator('#reader-menu').is_visible()
    page.locator('[data-action=toggle-dark]').click();settle(page);assert page.evaluate('state.readerDark') is True
    page.screenshot(path=str(OUT/'book-phone-dark-chapter9.png'))
    anchor=page.evaluate("chapters[current.c].pages[0].book.samples.find(s=>s.capstoneTests).anchor")
    page.evaluate("x=>doAction('book-jump',{dataset:{anchor:x}})",anchor);settle(page)
    page.evaluate('changePhysical(1)');settle(page);before=page.evaluate('Track3Book.spot()')
    page.locator('[data-action=bookmark]').click();key=page.evaluate("state.bookmarks.at(-1)");assert '@' in key
    page.locator('[data-action=book-note]').click();page.locator('#book-note-input').fill('검사: 긴 코드의 저장 위치')
    page.locator('[data-action=close]').click();assert '검사: 긴 코드의 저장 위치' in page.evaluate('Object.values(state.notes)')
    page.evaluate("doAction('book-answer',{dataset:{exercise:'book-exercise-9-1',title:'연습 9-1'}})")
    page.locator('#book-answer-input').fill('예상 작업 번호는 [10, 20]');page.locator('[data-action=close]').click()
    assert page.evaluate("state.bookAnswers['book-exercise-9-1']")=='예상 작업 번호는 [10, 20]'
    page.evaluate("doAction('saved',{})");settle(page);page.locator('[data-action=book-open-mark]').first.click();settle(page)
    after=page.evaluate('Track3Book.spot()');assert after['physical']==before['physical'],(before,after)
    persisted=json.loads(page.evaluate('window.testSaved'));page.close()
    page=fixture(browser,state=persisted);page.evaluate("doAction('continue',{})");settle(page)
    assert page.evaluate('physicalPage')==before['physical']
    assert page.evaluate("state.bookAnswers['book-exercise-9-1']")=='예상 작업 번호는 [10, 20]'
    page.locator('.reader-aa').click();page.locator('#reader-font-range').fill('24');settle(page)
    assert page.evaluate('state.readerFont')==24
    page.set_viewport_size({'width':800,'height':1280});settle(page);m=metrics(page)
    assert m['flowHeight']<=m['bodyHeight']+2;page.screenshot(path=str(OUT/'book-rotated-code24.png'))
    page.evaluate("doAction('summary',{})");assert page.locator('.book-toc button').count()>=7
    page.locator('.book-toc button').nth(2).click();settle(page);assert page.evaluate('physicalPage')>0
    page.evaluate("jumpToTrackSlide(1)");settle(page);assert page.evaluate('chapters[current.c].chapter')==1
    page.evaluate("changePhysical(-1)");settle(page);assert page.evaluate('chapters[current.c].track')==2
    page.evaluate("changePhysical(1)");settle(page);assert page.evaluate('chapters[current.c].track')==3
    assert page.evaluate('physicalPage')==0
    RESULTS.append({'interaction':'toolbar, theme, notes, exercise answers, bookmark resume, restart, font slider, rotation, section TOC, track boundary','status':'PASS'});page.close()
    course=json.loads((ROOT/'live/content.json').read_text());oldid=next(iter(course['legacyTrack3']))
    t2=next(c for c in course['chapters'] if c['track']==2)['pages'][0]['id']
    old={'schema':1,'location':oldid,'bookmarks':[oldid,t2],'notes':{oldid:'예전 트랙3 메모',t2:'트랙2 유지 메모'},'complete':['t3-c1'],'migrated':True}
    page=fixture(browser,state=old);page.evaluate("doAction('saved',{})");settle(page)
    assert page.locator('[data-action=book-legacy]').count()>=1
    page.locator('[data-action=book-legacy]').first.click();assert '예전 트랙3 메모' in page.locator('#modal').inner_text()
    assert '예전 트랙3 메모'==page.evaluate('state.notes['+json.dumps(oldid)+']')
    assert '트랙2 유지 메모'==page.evaluate('state.notes['+json.dumps(t2)+']')
    page.screenshot(path=str(OUT/'book-legacy-record-preserved.png'))
    RESULTS.append({'interaction':'legacy Track3 full original text/note preserved; unrelated Track2 note preserved','status':'PASS'});page.close()
    assert not ERRORS,ERRORS;browser.close()
  report={'status':'PASS','scope':'Real Chromium with native-bridge emulator. NOT a physical Galaxy installation, network updater, or Python-worker execution test.','cases':RESULTS,'consoleErrors':ERRORS}
  (OUT/'track3-book-browser-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
  print(json.dumps({'status':'PASS','cases':len(RESULTS),'consoleErrors':ERRORS}))
if __name__=='__main__':main()
