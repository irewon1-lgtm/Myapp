"""Source-level and actual Node execution checks; never a physical-device claim."""
from __future__ import annotations
import copy,hashlib,json,pathlib,re,subprocess,sys,tempfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from compile_track3_book import blocks_from_markdown
from compile_track4_book import TITLES,digest


def main():
    course=json.loads((ROOT/'live/content.json').read_text())
    chapters=[c for c in course['chapters'] if c.get('track')==4]
    assert len(chapters)==12
    assert [c['title'] for c in chapters]==TITLES
    assert next(t for t in course['tracks'] if t['id']==4)['available']
    results=[];html_demos=[];project={};code_count=0
    for c in chapters:
        assert len(c['pages'])==1 and c['pages'][0]['kind']=='book'
        book=c['pages'][0]['book'];source=(ROOT/book['sourcePath']).read_text()
        assert '内容' not in source and 'Niederstetter' not in source
        assert hashlib.sha256(source.encode()).hexdigest()==book['sourceSha256']
        raw=blocks_from_markdown(source)
        assert [b['text'] for b in raw]==[b['text'] for b in book['blocks']],c['chapter']
        assert len(c['references'])==5 and len(set(r['url'] for r in c['references']))==5
        ex=[b['exercise'] for b in book['blocks'] if b.get('exercise')]
        sol=[b['solution'] for b in book['blocks'] if b.get('solution')]
        assert ex==sol and len(ex)>=5
        assert all(x.startswith('t4-book-exercise-') for x in ex)
        assert [s['code'] for s in book['samples']]==[b['text'] for b in raw if b['kind']=='fence' and b['info'].split()[0] in {'html','css','javascript','json'}]
        for b in raw:
            if b['kind']!='fence':continue
            match=re.search(r'(?:^|\s)file=([\w.-]+)',b['info'])
            if match:project[match[1]]=b['text']+'\n'
        for sample in book['samples']:
            code_count+=1
            assert sample['runCode'] is None,'Web code must not reach the Python runner'
            row={'id':sample['id'],'chapter':c['chapter'],'language':sample['language'],'mode':sample['mode']}
            if sample['mode']=='console':
                proc=subprocess.run(['node','--unhandled-rejections=strict','-e',sample['code']],text=True,capture_output=True,timeout=5)
                assert proc.returncode==0,(sample['id'],proc.stderr)
                expected=sample['expectedOutput']
                if expected is not None:assert proc.stdout.rstrip('\n')==expected,(sample['id'],proc.stdout,expected)
                row.update(status='PASS_EXECUTED',stdout=proc.stdout,expectedChecked=expected is not None)
            elif sample['mode']=='html':
                html_demos.append(sample['id']);row['status']='BROWSER_TEST_REQUIRED'
            elif sample['language']=='json':
                json.loads(sample['code']);row['status']='PASS_JSON_PARSE'
            else:row['status']='CONTEXT_FRAGMENT_NOT_EXECUTED'
            results.append(row)
    assert set(project)=={'index.html','style.css','app.js','topics.json'}
    for name,text in project.items():assert (ROOT/'live/track4-book/project'/name).read_text()==text
    js_path=ROOT/'live/track4-book/project/app.js'
    p=subprocess.run(['node','--check',str(js_path)],capture_output=True,text=True);assert p.returncode==0,p.stderr
    js=js_path.read_text();assert 'localStorage.clear(' not in js and '.innerHTML' not in js
    assert 'if (!accepted) return;' in js
    assert '공부한 내용을 1~80자 범위로 적어 주세요.' in js
    source_map=json.loads((ROOT/'live/track4-book/source-map.json').read_text())
    assert sum(len(c['sources']) for c in source_map['chapters'])==60
    exercise_total=sum(len(c['pages'][0]['book']['exercises']) for c in chapters)
    assert exercise_total>=74
    html=(ROOT/'live/reader.html').read_text()
    assert len(html.encode())<=3*1024*1024
    data=re.search(r'<script[^>]*id="course"[^>]*>([\s\S]*?)</script>',html)
    assert data,'Embedded course script missing'
    transport=json.loads(data[1])
    for c in transport['chapters']:
        for page in c['pages']:
            book=page.get('book')
            if not book:continue
            book.setdefault('html','\n'.join(b['html'] for b in book['blocks']))
            book.setdefault('text','\n\n'.join(b['text'] for b in book['blocks']))
            for sample in book['samples']:
                if sample.pop('runSameCode',False):sample['runCode']=sample['code']
    assert transport==course,'Runtime transport hydration changed the course'
    baseline=ROOT/'review/track4-before.json'
    if baseline.exists():
        old=json.loads(baseline.read_text())
        assert digest([c for c in old['chapters'] if c.get('track')!=4])==digest([c for c in course['chapters'] if c.get('track')!=4]),'Unrelated chapters changed'
        assert [t for t in old['tracks'] if t['id']!=4]==[t for t in course['tracks'] if t['id']!=4],'Other track metadata changed'
        assert old.get('legacyTrack3')==course.get('legacyTrack3')
        assert old.get('bookAliases')==course.get('bookAliases')
    out=ROOT/'review';out.mkdir(exist_ok=True)
    report={'status':'PASS','scope':'Lossless manuscript/JSON verification and Node execution. Browser/device tests are separate.','chapters':12,'authoredCharacters':course['track4BookAudit']['authoredCharacters'],'exercises':exercise_total,'referenceAssociations':60,'codeBlocks':code_count,'executedJavaScriptExamples':sum(r['status']=='PASS_EXECUTED' for r in results),'htmlDemosForBrowser':len(html_demos),'readerBytes':len(html.encode()),'allBlocksPreserved':True,'transportHydrationExact':True,'otherTracksPreserved':baseline.exists(),'results':results}
    (out/'track4-book-source-tests.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='results'},ensure_ascii=False))

if __name__=='__main__':main()
