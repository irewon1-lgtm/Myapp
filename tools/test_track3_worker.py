"""Exercise the real browser Python worker, including cancellation and capstone."""
import json,os,shutil
from pathlib import Path
from playwright.sync_api import sync_playwright
from test_track3_browser import fixture,settle,open_chapter,OUT

def main():
 results=[]
 with sync_playwright() as p:
  exe=os.environ.get('CHROME') or shutil.which('chromium') or shutil.which('google-chrome') or shutil.which('google-chrome-stable')
  browser=p.chromium.launch(executable_path=exe,headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
  page=fixture(browser);open_chapter(page,1)
  page.evaluate("doAction('book-run',{dataset:{sample:chapters[current.c].pages[0].book.samples[0].id}})")
  def run(expected=None,error=None):
   page.locator('[data-action=run-python]').click()
   page.wait_for_function('practiceRunning===false',timeout=100000)
   actual=page.evaluate('({out:practiceOutput,error:practiceError})')
   if error: assert error in actual['error'],actual
   else:
    assert not actual['error'],actual
    assert actual['out']==expected,actual
   return actual
  results.append({'kind':'real example','result':run('5\n7')})
  assert page.locator('#practice-code').input_value()==page.evaluate("chapters[current.c].pages[0].book.samples[0].runCode")
  page.locator('#practice-code').fill('flag = 12\nprint(flag)');results.append({'kind':'edited code','result':run('12')})
  page.locator('#practice-code').fill('print("flag" in globals())');results.append({'kind':'isolated namespace','result':run('False')})
  page.locator('#practice-code').fill('print({}["missing"])');results.append({'kind':'expected runtime error','result':run(error='KeyError')})
  page.locator('#practice-code').fill('while True:\n    pass')
  page.locator('[data-action=run-python]').click();page.wait_for_timeout(300)
  assert page.evaluate('1+1')==2,'Main UI thread was blocked'
  page.locator('[data-action=book-stop]').click()
  assert page.evaluate('practiceRunning') is False
  results.append({'kind':'user cancellation and main UI responsiveness','status':'PASS'})
  open_chapter(page,9)
  page.evaluate("doAction('book-run',{dataset:{sample:chapters[current.c].pages[0].book.samples.find(s=>s.capstoneTests).id}})")
  results.append({'kind':'complete capstone tests','result':run('선택·정렬·원본 보존·잘못된 입력 검사 통과')})
  page.screenshot(path=str(OUT/'book-real-python-capstone.png'))
  browser.close()
 report={'status':'PASS','scope':'Actual Pyodide in a Chromium Worker over its pinned public CDN. NOT a physical Galaxy test.','cases':results}
 (OUT/'track3-book-worker-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({'status':'PASS','cases':len(results)}))
if __name__=='__main__':main()
