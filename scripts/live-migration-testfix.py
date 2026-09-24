"""Correct the browser test harness; do not relax the application's CSP."""
from pathlib import Path
helper='''def wait_js(page, expression, arg=None, timeout=30000):
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

'''
for name,marker in [('tests/live/browser.py','def open_page('),('tests/live/production-smoke.py','def check(')]:
    p=Path(name);s=p.read_text()
    assert 'page.wait_for_function(' in s and 'def wait_js(' not in s
    s=s.replace(marker,helper+marker,1).replace('page.wait_for_function(','wait_js(page,')
    p.write_text(s)
Path(__file__).unlink()
print('Browser polling corrected; CSP script-src self unchanged.',flush=True)
