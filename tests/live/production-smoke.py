"""Read-only production smoke using a disposable browser profile. No user sync credentials."""
import json,pathlib,time,os
from playwright.sync_api import sync_playwright
out=pathlib.Path('review/live-production');out.mkdir(parents=True,exist_ok=True)
channel=json.loads(pathlib.Path('live/channel.json').read_text());manifest=json.loads(pathlib.Path('live/release.json').read_text())
report={'scope':'Real production HTTPS and disposable Chromium; no physical Galaxy or real user sync data','checks':[]}
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

def check(name,value):
 report['checks'].append({'name':name,'passed':bool(value)});print(name,value,flush=True)
 if not value:raise AssertionError(name)
try:
 with sync_playwright() as p:
  b=p.chromium.launch(headless=True,args=['--no-sandbox']);c=b.new_context(viewport={'width':412,'height':915},service_workers='block');page=c.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
  response=page.goto('https://chatbook-library-20260923.netlify.app/',wait_until='domcontentloaded',timeout=60000)
  wait_js(page,'window.CB?.catalog?.books?.length>0&&window.ChatbookLive?.pending===false',timeout=60000)
  check('production gateway selected the verified GitHub commit',page.evaluate('ChatbookLive.commit')==channel['current']['commit'])
  check('canonical spec digest matches tested rules',page.evaluate("async()=>{const r=await fetch('/engine-spec.json',{cache:'no-store'});return (await r.json()).contentDigest}")==manifest['specDigest'])
  for width in [360,412,800]:
   page.set_viewport_size({'width':width,'height':915});page.evaluate('CB.read(CB.catalog.books[0].id)');page.wait_for_timeout(500)
   check('production reader usable at '+str(width),page.evaluate('CB.Reader.pages>=1&&document.documentElement.scrollWidth<=innerWidth+1'))
  page.set_viewport_size({'width':412,'height':915});page.screenshot(path=str(out/'live-reader-412.png'),full_page=True)
  for route in ['home','library','quiz','settings']:
   page.evaluate('(r)=>CB.go(r)',route);page.wait_for_timeout(100);check('production '+route+' screen',page.evaluate('document.getElementById("app").innerText.length>10'))
  check('no JavaScript exception in ordinary production use',not errors)
  check('no real synchronization configured',page.evaluate('!CB.secret()'))
  b.close();report['status']='passed'
except Exception as e:report['status']='failed';report['failure']=str(e)
finally:
 report['completedAt']=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime());(out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
raise SystemExit(0 if report['status']=='passed' else 1)
