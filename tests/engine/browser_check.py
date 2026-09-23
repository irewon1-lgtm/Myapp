"""Actual Chromium layout and local pipeline UI checks. Not physical Galaxy QA."""
import sys,json,os,pathlib,mimetypes
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright
base=sys.argv[1] if len(sys.argv)>1 else 'http://127.0.0.1:8768/engine/'
out=pathlib.Path(os.environ.get('ENGINE_BROWSER_REPORT','/mnt/data/engine-browser-report.json'))
executable=next((s for s in ['/usr/bin/chromium','/usr/bin/google-chrome','/usr/bin/chromium-browser'] if os.path.exists(s)),None)
results=[]
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,executable_path=executable,args=['--no-sandbox'])
 for width,height in [(360,800),(412,915),(800,1280)]:
  context=browser.new_context(viewport={'width':width,'height':height},device_scale_factor=1,is_mobile=True,has_touch=True)
  if os.environ.get('ENGINE_LOCAL_SITE'):
   root=pathlib.Path(os.environ['ENGINE_LOCAL_SITE']).resolve()
   def serve(route):
    name=urlparse(route.request.url).path
    file=(root/name.lstrip('/')).resolve()
    if name.endswith('/'):file=file/'index.html'
    if not file.is_relative_to(root) or not file.is_file():route.fulfill(status=404,body='not found');return
    mime='application/javascript' if file.suffix=='.mjs' else (mimetypes.guess_type(str(file))[0] or 'application/octet-stream')
    route.fulfill(status=200,content_type=mime,body=file.read_bytes())
   context.route('**/*',serve)
  page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
  page.goto(base,wait_until='networkidle');page.wait_for_selector('body[data-engine-ready="true"]')
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth+1')
  initial=page.locator('body').get_attribute('class') or ''
  page.locator('#theme').click();assert (page.locator('body').get_attribute('class') or '')!=initial
  page.locator('summary').click();page.locator('#duration').fill('60');page.locator('#transcript').fill('[0:00] 서버는 요청을 받고 데이터를 처리합니다.\n\n[0:30] API는 다른 프로그램에서 데이터를 요청하는 창구입니다.')
  page.locator('#prepare').click();page.wait_for_function('!!window.chatbookEngineJob');assert '작업 준비 완료' in page.locator('#message').inner_text();assert page.evaluate('window.chatbookEngineJob.calls')==0
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth+1')
  with page.expect_download() as d:page.locator('#save').click()
  assert d.value.suggested_filename.endswith('.json')
  assert not errors, errors
  results.append({'viewport':[width,height],'horizontalOverflow':False,'themeToggle':True,'sourcePrepare':True,'jobDownload':True,'jsErrors':errors,'scope':'Chromium emulation; not a physical Galaxy'})
  if width==412 and os.environ.get('ENGINE_SCREENSHOT'):page.screenshot(path=os.environ['ENGINE_SCREENSHOT'],full_page=True)
  context.close()
 browser.close()
out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(results,ensure_ascii=False,indent=2));print(json.dumps(results))
