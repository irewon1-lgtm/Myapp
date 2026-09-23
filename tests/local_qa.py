from pathlib import Path
from playwright.sync_api import sync_playwright
import json,base64,zipfile,io,fitz,traceback,time
root=Path('/mnt/data/chatbook-tests');root.mkdir(exist_ok=True)
html=Path('/mnt/data/챗북_바로실행.html').read_text()
report={'environment':'Chromium DOM rendering via set_content; network navigation blocked by environment. Storage fallback in-memory here; not a physical Galaxy test.','checks':[],'errors':[],'exports':{}}
def ok(name,condition,details=None):
 report['checks'].append({'name':name,'passed':bool(condition),'details':details});print(name,condition,details or '',flush=True)
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':412,'height':915},device_scale_factor=1)
 page.set_default_timeout(3500)
 page.on('pageerror',lambda e:report['errors'].append(str(e)))
 page.route('https://fonts.**/*',lambda r:r.abort());page.set_content(html,wait_until='domcontentloaded');page.wait_for_timeout(500)
 page.evaluate("clearTimeout(CB.toastTimer);document.querySelector('#toast').innerHTML=''")
 for width,height in [(360,800),(412,915),(768,1024),(1366,900)]:
  page.set_viewport_size({'width':width,'height':height})
  for route in ['home','library','exports','quiz','settings','review']:
   page.evaluate('(r)=>CB.go(r)',route);page.wait_for_timeout(110)
   overflow=page.evaluate('document.documentElement.scrollWidth>innerWidth+1')
   ok(f'{route} width {width} no overflow',not overflow)
   if width==412: page.screenshot(path=str(root/f'{route}.png'),full_page=True)
 page.set_viewport_size({'width':412,'height':915})
 page.evaluate("CB.go('library')");page.wait_for_timeout(100)
 print('search inputs',page.locator('input').evaluate_all('(els)=>els.map(e=>e.id)'),flush=True)
 page.locator('#shelf-search').fill('대화');ok('library search filters',0<page.locator('.book-card').count()<9)
 page.locator('#shelf-search').fill('NORESULT-876');ok('empty search state',page.locator('.book-card').count()==0)
 page.locator('#shelf-search').fill('');page.locator('[data-action="shelf-view"][data-value="list"]').click();ok('list view',page.locator('#shelf-grid').evaluate("e=>e.classList.contains('list-view')"))
 page.evaluate("CB.read('habit')");page.wait_for_timeout(400);ok('measured pagination',page.evaluate('CB.Reader.pages>5'))
 pages=page.evaluate('CB.Reader.pages');page.locator('[data-action="reader-next"]').click();page.wait_for_timeout(300);ok('next page persisted',page.evaluate("CB.progress('habit').page===1"))
 page.locator('#bottom-bookmark').click();ok('bookmark saved',page.evaluate("CB.bookmarks('habit').length===1"))
 page.locator('[data-action="reader-note"]').click();page.locator('#note-text').fill('오늘의 습관은 커피 옆에서 시작하기.');page.locator('[data-action="save-note"]').click();ok('note saved',page.evaluate("CB.annotations('habit','note')[0].text.includes('커피')"))
 page.evaluate("CB.go('home')");page.wait_for_timeout(100);page.evaluate("CB.read('habit')");page.wait_for_timeout(400);ok('resume paragraph',page.evaluate("CB.progress('habit').page>=1"))
 page.evaluate("CB.showToc('habit')");print('toc action',page.locator('[data-action="toc-go"]').first.evaluate('(e)=>e.dataset'),flush=True)
 page.locator('[data-action="toc-go"]').nth(2).click();page.wait_for_timeout(350);ok('table of contents to chapter3',page.evaluate('CB.Reader.info().chapter===2'))
 page.evaluate("CB.showReadingSettings()");page.locator('#font-size').fill('25');page.wait_for_timeout(350);page.locator('[data-action="modal-close"]').first.click();ok('font reflow preserves chapter',page.evaluate('CB.Reader.pages>'+str(pages)+' && CB.Reader.info().chapter===2'))
 page.screenshot(path=str(root/'reader.png'),full_page=True)
 page.evaluate("CB.setPref('fontSize',18);CB.Reader.applySettings()");page.wait_for_timeout(200)
 page.evaluate("CB.startQuiz('ai')");page.wait_for_timeout(150)
 for qi in range(3):
  answer=page.evaluate('CB.selected().questions[CB.quizState().index].answer')
  selection=(answer+1)%4 if qi==0 else answer
  page.locator(f'[data-action="quiz-choice"][data-value="{selection}"]').click();page.locator('[data-action="quiz-submit"]').click();ok(f'quiz {qi+1} feedback',page.locator('.quiz-hint').inner_text().find('정답')>=0);page.locator('[data-action="quiz-next"]').click()
 ok('quiz real score',page.locator('.result-score').inner_text().replace(' ','')=='2/3')
 page.evaluate("CB.go('review')");page.wait_for_timeout(100);ok('wrong answers queued',page.evaluate('CB.reviewItems().length>=1'))
 page.locator('[data-action="review-start"]').first.click();page.locator('[data-action="review-reveal"]').click();page.locator('[data-action="review-rate"][data-value="easy"]').click();ok('spaced review rescheduled',page.evaluate('CB.reviewItems().length===0'))
 page.evaluate("CB.setPref('theme','dark')");ok('dark applied',page.evaluate("document.body.dataset.theme==='dark'"));page.evaluate("CB.setPref('theme','cream')")
 page.evaluate("CB.set('annotation:test-backup',{kind:'note',bookId:'habit',anchor:'habit-c1',text:'백업 확인'})")
 data=page.evaluate('JSON.stringify(CB.r)');page.evaluate('CB.r={}');page.evaluate('(r)=>CB.merge(JSON.parse(r))',data);ok('record merge roundtrip',page.evaluate("CB.get('annotation:test-backup').text==='백업 확인'"))
 page.evaluate("CB.select('habit');CB.go('exports')");page.wait_for_timeout(120)
 for kind in ['pdf','summary','print','pptx']:
  result=page.evaluate("""async(kind)=>{const b=CB.book('habit');const r=kind==='pptx'?await CB.Exporter.pptx(b):await CB.Exporter.pdf(b,kind);const a=new Uint8Array(await r.blob.arrayBuffer());let s='';for(let i=0;i<a.length;i+=8192)s+=String.fromCharCode(...a.subarray(i,i+8192));return {data:btoa(s),pages:r.pages,size:r.blob.size};}""",kind)
  raw=base64.b64decode(result['data']);suffix='pptx' if kind=='pptx' else 'pdf';(root/f'export-{kind}.{suffix}').write_bytes(raw)
  report['exports'][kind]={'pages':result['pages'],'bytes':len(raw)}
  if kind=='pptx':
   z=zipfile.ZipFile(io.BytesIO(raw)); slides=[n for n in z.namelist() if n.startswith('ppt/slides/slide') and n.endswith('.xml')];ok('PPTX real slides',len(slides)==8);ok('PPTX editable Korean text','인생은'.encode() in z.read('ppt/slides/slide1.xml'))
  else:
   pdf=fitz.open(stream=raw,filetype='pdf');ok(kind+' PDF valid',len(pdf)==result['pages']);ok(kind+' page count',len(pdf)==1 if kind!='pdf' else len(pdf)>=7)
   if kind in ['pdf','summary']:pdf[0].get_pixmap(matrix=fitz.Matrix(1,1)).save(str(root/f'{kind}-first.png'))
 ok('no JS runtime errors',not report['errors'],report['errors'])
 browser.close()
(root/'local-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print('DONE',report['exports'],flush=True)
