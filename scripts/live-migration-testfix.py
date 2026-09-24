"""Correct isolated test fixtures; preserve production CSP and anti-rollback checks."""
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
p=Path('tests/live/server.mjs');s=p.read_text()
s=s.replace("const A=release('a',100),B=release('b',200,{'views.js':Buffer.concat([original['views.js'],Buffer.from('\\nwindow.__LIVE_TEST_EDITION=\"B\";\\n')])});",'''const nextCatalog=JSON.parse(original['content/catalog.json']);
nextCatalog.version='live-fixture-B';
nextCatalog.books[0].chapters[0].title='LIVE_CHAPTER_UPDATE_CONFIRMED';
nextCatalog.books[0].chapters[0].blocks[0].text='LIVE_BODY_UPDATE_CONFIRMED';
const extra=structuredClone(nextCatalog.books[0]);extra.id='live-fixture-new-book';extra.title='LIVE_NEW_BOOK_CONFIRMED';extra.questions=[];extra.chapters=[{id:'live-fixture-ch1',title:'새 장',blocks:[{id:'live-fixture-p1',type:'p',text:'새 책 등록 테스트'}]}];nextCatalog.books.push(extra);
const A=release('a',100),B=release('b',200,{'views.js':Buffer.concat([original['views.js'],Buffer.from('\\nwindow.__LIVE_TEST_EDITION="B";\\n')]),'content/catalog.json':Buffer.from(JSON.stringify(nextCatalog)),'library.js':Buffer.from('window.CHATBOOK_CATALOG='+JSON.stringify(nextCatalog)+';')});''')
assert 'nextCatalog.version' in s
s=s.replace('const map=new Map([A,B,syntax,corrupt,partial,future]',"const recovered=release('1',700);\nconst map=new Map([A,B,syntax,corrupt,partial,future,recovered]")
p.write_text(s)
p=Path('tests/live/browser.py');s=p.read_text()
s=s.replace("control('b');static=browser.new_context", "control('1');static=browser.new_context")
marker="    record('release B UI update works without Netlify deployment',page.evaluate('window.__LIVE_TEST_EDITION')=='B')"
assert marker in s
s=s.replace(marker,marker+'''
    record('new book published without Netlify deployment',page.evaluate("CB.catalog.books.some(b=>b.id==='live-fixture-new-book'&&b.title==='LIVE_NEW_BOOK_CONFIRMED')"))
    record('chapter update delivered with the same verified version',page.evaluate("CB.catalog.books[0].chapters[0].title")=='LIVE_CHAPTER_UPDATE_CONFIRMED')
    record('body update delivered with the same verified version',page.evaluate("CB.catalog.books[0].chapters[0].blocks[0].text")=='LIVE_BODY_UPDATE_CONFIRMED')''')
p.write_text(s)
Path(__file__).unlink()
print('CSP-safe polling and monotonic recovery fixtures corrected; content update assertions added.',flush=True)
