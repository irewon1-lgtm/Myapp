"""Verify source preservation and execute book examples in isolated processes."""
from __future__ import annotations
import hashlib
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from compile_track3_book import blocks_from_markdown


def main():
    course=json.loads((ROOT/'live/content.json').read_text())
    chapters=[c for c in course['chapters'] if c.get('track')==3]
    assert len(chapters)==9
    results=[]
    for c in chapters:
        assert len(c['pages'])==1, 'Book chapters must not be split into lesson cards'
        p=c['pages'][0];book=p['book']
        source=(ROOT/book['sourcePath']).read_text()
        assert hashlib.sha256(source.encode()).hexdigest()==book['sourceSha256']
        raw=blocks_from_markdown(source)
        assert [b['text'] for b in raw]==[b['text'] for b in book['blocks']], 'Source order or text was lost'
        fences=[b for b in raw if b['kind']=='fence' and b['info'].startswith('python')]
        assert len(fences)==len(book['samples'])
        assert [b['text'] for b in fences]==[s['code'] for s in book['samples']]
        assert len(c['references'])==5
        assert len(book['exercises'])==len([b for b in book['blocks'] if b.get('solution')])
        for s in book['samples']:
            row={'id':s['id'],'chapter':c['chapter'],'title':s['title']}
            if s['runCode'] is None:
                row.update(status='NOT_EXECUTED',reason='Intentionally incomplete fragment; no standalone-run claim')
                results.append(row);continue
            proc=subprocess.run([sys.executable,'-I','-c',s['runCode']],text=True,capture_output=True,timeout=5)
            if s['expectedError']:
                assert proc.returncode!=0,(s['id'],'Expected example exception did not occur')
                assert re.search(r'^'+re.escape(s['expectedError'])+r':',proc.stderr,re.M),(s['id'],proc.stderr)
                row.update(status='PASS_EXPECTED_EXCEPTION',exception=s['expectedError'])
            else:
                assert proc.returncode==0,(s['id'],proc.stderr)
                if s['expectedOutput'] is not None:
                    assert proc.stdout.rstrip('\n')==s['expectedOutput'],(s['id'],repr(proc.stdout),repr(s['expectedOutput']))
                row.update(status='PASS',stdout=proc.stdout)
            results.append(row)
    assert all(p.get('kind')!='book' for c in course['chapters'] if c.get('track')!=3 for p in c['pages'])
    report={'status':'PASS','kind':'Python source and execution tests, not device or UI tests','chapters':len(chapters),'authoredCharacters':course['track3BookAudit']['authoredCharacters'],'exercises':sum(len(c['pages'][0]['book']['exercises']) for c in chapters),'executedExamples':sum(x['status']!='NOT_EXECUTED' for x in results),'intentionalFragments':sum(x['status']=='NOT_EXECUTED' for x in results),'expectedExceptionExamples':sum(x['status']=='PASS_EXPECTED_EXCEPTION' for x in results),'allSourceBlocksAndFencesPreserved':True,'results':results}
    (ROOT/'review').mkdir(exist_ok=True)
    (ROOT/'review/track3-book-python-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='results'},ensure_ascii=False))


if __name__=='__main__':
    main()
