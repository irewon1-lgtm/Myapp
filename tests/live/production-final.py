"""Actual HTTPS validation, with a fresh disposable browser profile and synthetic local data.
No user credentials, no sync writes, no Netlify deployment.
"""
import json,pathlib,time,urllib.request
from playwright.sync_api import sync_playwright
base='https://chatbook-library-20260923.netlify.app'
out=pathlib.Path('review/live-production');out.mkdir(parents=True,exist_ok=True)
channel=json.loads(pathlib.Path('live/channel.json').read_text());manifest=json.loads(pathlib.Path('live/release.json').read_text())
report={'scope':'Real production HTTPS, cold browser and offline restart; no physical Galaxy and no user sync data','checks':[]}
def check(name,value,detail=None):
 report['checks'].append({'name':name,'passed':bool(value),'detail':detail});print(name,value,flush=True)
 if not value:raise AssertionError(name)
def wait_js(page,expression,timeout=60000):
 deadline=time.monotonic()+timeout/1000
 while time.monotonic()<deadline:
  try:
   if page.evaluate(expression):return
  except Exception:pass
  time.sleep(.15)
 raise AssertionError('Browser condition timed out: '+expression)
try:
 # Wait for the bounded channel cache, not a new deployment.
 for attempt in range(12):
  try:
   with urllib.request.urlopen(base+'/__cb/channel.json',timeout=15) as r:c=json.load(r)
   if c['current']['commit']==channel['current']['commit']:break
  except Exception:pass
  time.sleep(5)
 else:raise AssertionError('Latest live channel not observed over HTTPS')
 with sync_playwright() as p:
  b=p.chromium.launch(headless=True,args=['--no-sandbox']);ctx=b.new_context(viewport={'width':412,'height':915});page=ctx.new_page();errors=[]
  page.on('pageerror',lambda e:errors.append(str(e)))
  page.goto(base+'/',wait_until='domcontentloaded',timeout=60000)
  wait_js(page,'window.CB?.catalog?.books?.length>0&&window.ChatbookLive?.pending===false')
  check('actual UI selected the latest GitHub release',page.evaluate('ChatbookLive.commit')==channel['current']['commit'])
  for asset in ['/live-guard.js','/live-sw.js','/manifest.webmanifest']:
   result=page.evaluate("async p=>{const r=await fetch(p,{cache:'no-store'});return {status:r.status,type:r.headers.get('content-type'),boot:r.headers.get('x-chatbook-bootstrap')}}",asset)
   check('cold bootstrap path '+asset,result['status']==200 and result['boot']=='1',result)
  check('canonical spec still matches approved authoring rules',page.evaluate("async()=>{const r=await fetch('/engine-spec.json',{cache:'no-store'});return (await r.json()).contentDigest}")==manifest['specDigest'])
  check('all existing books remain present',page.evaluate('CB.catalog.books.length')>=13)
  for width in [360,412,800,1280]:
   page.set_viewport_size({'width':width,'height':915});page.evaluate('CB.read(CB.catalog.books[0].id)');page.wait_for_timeout(500)
   check('actual reader fits '+str(width),page.evaluate('CB.Reader.pages>=1&&document.documentElement.scrollWidth<=innerWidth+1'))
  page.evaluate("CB.set('annotation:note:production-check',{kind:'note',bookId:CB.Reader.book.id,anchor:CB.Reader.book.chapters[0].blocks[0].id,text:'DISPOSABLE_PROFILE_KEEP'});CB.Reader.bookmark();")
  check('synthetic reading record saved locally',page.evaluate("CB.get('annotation:note:production-check').text")=='DISPOSABLE_PROFILE_KEEP')
  for route in ['home','library','quiz','settings']:
   page.evaluate('(r)=>CB.go(r)',route);page.wait_for_timeout(150)
   check('actual '+route+' screen',page.evaluate('document.getElementById("app").innerText.length>10'))
  page.set_viewport_size({'width':412,'height':915});page.evaluate('CB.read(CB.catalog.books[0].id)');page.wait_for_timeout(400)
  page.screenshot(path=str(out/'actual-online-reader-412.png'),full_page=True)
  check('no actual user synchronization configured',page.evaluate('!CB.secret()'))
  wait_js(page,'ChatbookLive.offlineReady===true',timeout=150000)
  check('actual complete offline bundle committed',True)
  ctx.set_offline(True);page.reload(wait_until='domcontentloaded',timeout=45000)
  wait_js(page,'window.CB?.catalog?.books?.length>0&&window.ChatbookLive?.pending===false')
  check('actual offline restart uses verified reader',page.evaluate('ChatbookLive.commit')==channel['current']['commit'])
  check('actual offline note survives',page.evaluate("CB.get('annotation:note:production-check').text")=='DISPOSABLE_PROFILE_KEEP')
  page.evaluate('CB.read(CB.catalog.books[0].id)');page.wait_for_timeout(400)
  check('actual offline chapter navigation works',page.evaluate('CB.Reader.pages>=1'))
  page.screenshot(path=str(out/'actual-offline-reader-412.png'),full_page=True)
  check('no JavaScript exceptions during actual use',not errors,errors)
  b.close();report['status']='passed'
except Exception as e:report['status']='failed';report['failure']=str(e)
finally:
 report['completedAt']=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime());(out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
raise SystemExit(0 if report['status']=='passed' else 1)
