"""Compile twelve original long-form chapters without note-card quotas.
Other tracks and their stable identities are preserved. No model or network runs here.
"""
from __future__ import annotations
import copy
import hashlib
import html
import json
import pathlib
import re
from compile_track3_book import blocks_from_markdown, inline

ROOT = pathlib.Path(__file__).resolve().parents[1]
LABEL = '0.11.0 · Track 4 웹의 기본 · 12장 연속 교재'
TITLES = [
    '웹페이지는 어떻게 내 화면에 나타날까?',
    'HTML로 내용의 구조와 의미 만들기',
    '입력창·버튼·폼으로 사용자 입력 받기',
    'CSS는 어떤 규칙으로 화면에 적용될까?',
    '박스·Flexbox·Grid로 화면 배치하기',
    '휴대폰·태블릿에서도 읽고 누를 수 있는 화면',
    'Python에서 JavaScript로 — 아는 개념을 새 문법에 연결하기',
    '버튼을 누르면 무슨 일이 생길까? — DOM과 이벤트',
    '화면 상태와 데이터 저장 — 새로고침해도 기록 남기기',
    '인터넷에서 데이터 가져오기 — 요청·응답과 기다리는 작업',
    '고장 난 웹앱의 원인을 찾고 수정 검증하기',
    '종합 프로젝트 — AI 초안을 이해하고 내 웹앱으로 완성하기',
]
LANGUAGES = {'html':'HTML','css':'CSS','javascript':'JavaScript','json':'JSON','text':'출력·자료'}


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True).encode()).hexdigest()


def references(source):
    tail = source.split('## 이 장의 참고자료와 확인 범위',1)[1]
    rows = re.findall(r'^([1-5])\. \[([^\]]+)\]\((https?://[^\s)]+)\)(.*)$',tail,re.M)
    assert [r[0] for r in rows] == list('12345'), 'Each chapter must document five principal sources'
    result=[]
    for number,title,url,note in rows:
        if 'learningwebdesign.com' in url:
            author,year,scope='Jennifer Niederst Robbins',2025,'공식 목차·공개 안내; 유료 본문 전체 아님'
        elif 'murach.com' in url:
            author,year,scope='Zak Ruvalcaba · Anne Boehm · Mary Delamater',2024,'출판사 공개 목차·소개; 2024년 보조'
        elif 'eloquentjavascript.net' in url:
            author,year,scope='Marijn Haverbeke',2024,'관련 공개 본문; 2024년 보조'
        elif 'cs50.harvard.edu' in url:
            author,year,scope='Harvard University','공개 강좌','공개 강의 본문 또는 과제; 최초 집필 연도와 확인일 구분'
        elif 'fullstackopen.com' in url:
            author,year,scope='University of Helsinki','공개 강좌','관련 공개 강의 본문'
        elif 'developer.mozilla.org' in url:
            author,year,scope='MDN Web Docs','지속 갱신','관련 공식 문서·학습 본문'
        elif 'web.dev' in url:
            author,year,scope='Google web.dev','지속 갱신','공식 학습 과정의 관련 절'
        elif 'developer.chrome.com' in url:
            author,year,scope='Google Chrome Developers','지속 갱신','공식 개발자 도구 문서'
        elif 'w3.org' in url:
            author,year,scope='W3C WAI','공개 자료','첫 점검 안내; 완전한 적합성 인증 아님'
        elif 'whatwg.org' in url:
            author,year,scope='WHATWG','Living Standard','Web storage 표준 본문'
        elif 'anthropic.com' in url:
            author,year,scope='Anthropic',2026,'저자 연구 보고; 개발자 52명·특정 과제·단기 실험'
        else:
            raise ValueError('Uncatalogued reference '+url)
        result.append({'book':title,'author':author,'year':year,'url':url,'number':int(number),'scope':scope,'note':note.strip(' ,'),'accessed':'2026-09-25'})
    return result


def compile_chapter(number, source, project):
    raw=blocks_from_markdown(source)
    assert raw[0]['text']==f'{number}장. {TITLES[number-1]}'
    page_id=f't4-book-c{number:02d}'
    blocks=[];samples=[];sections=[];exercises=[];solutions=[]
    section=TITLES[number-1]
    for index,item in enumerate(raw):
        anchor=f'{page_id}-b{index+1:04d}'
        text=item['text'];kind=item['kind']
        attrs=f' id="{anchor}" data-book-anchor="{anchor}"'
        entry={'id':anchor,'kind':kind,'text':text,'section':section}
        if kind=='heading':
            level=item['level'];entry['level']=level
            if level==2:
                section=text;entry['section']=section;sections.append({'id':anchor,'title':text})
            ex=re.match(r'연습 (\d+-\d+)\.',text)
            sol=re.match(r'풀이 (\d+-\d+)',text)
            extra=''
            if ex:
                eid='t4-book-exercise-'+ex[1]
                entry['exercise']=eid;exercises.append({'id':eid,'heading':text,'anchor':anchor})
                extra='<button class="book-answer-button" data-action="book-answer" data-exercise="'+eid+'" data-title="'+html.escape(text,quote=True)+'">내 답 적기</button>'
            if sol:
                entry['solution']='t4-book-exercise-'+sol[1];solutions.append(entry['solution'])
            entry['html']=f'<h{level}{attrs}>'+inline(text)+extra+f'</h{level}>'
        elif kind=='paragraph':
            entry['html']='<p'+attrs+'>'+inline(text)+'</p>'
        elif kind=='quote':
            entry['html']='<blockquote'+attrs+'>'+inline(text)+'</blockquote>'
        elif kind=='rule':
            entry['html']='<hr'+attrs+'>'
        elif kind=='list':
            entry['html']='<ol'+attrs+'>'+''.join('<li>'+inline(s)+'</li>' for s in item['items'])+'</ol>'
        elif kind=='table':
            rows=item['rows']
            entry['html']='<table'+attrs+'><thead><tr>'+''.join('<th scope="col">'+inline(v)+'</th>' for v in rows[0])+'</tr></thead><tbody>'+''.join('<tr>'+''.join('<td>'+inline(v)+'</td>' for v in r)+'</tr>' for r in rows[1:])+'</tbody></table>'
        elif kind=='fence':
            info=item['info'];language=info.split()[0] if info else 'text'
            filematch=re.search(r'(?:^|\s)file=([\w.-]+)',info)
            if filematch:
                name=filematch[1]
                assert name in {'index.html','style.css','app.js','topics.json'}
                assert name not in project, 'Project file duplicated'
                project[name]=text+'\n'
            if language in {'html','css','javascript','json'}:
                sample_id=f'{page_id}-code{len(samples)+1:03d}'
                expected=None
                if index+1<len(raw) and raw[index+1]['kind']=='fence' and raw[index+1]['info']=='text':
                    expected=raw[index+1]['text']
                mode='console' if language=='javascript' and 'console' in info.split() else 'html' if language=='html' and 'demo' in info.split() else 'reference'
                sample={'id':sample_id,'anchor':anchor,'title':section,'language':language,'code':text,'runCode':None,'mode':mode,'expectedOutput':expected,'fragment':mode=='reference','file':filematch[1] if filematch else None}
                samples.append(sample);entry['sampleId']=sample_id
                entry['html']='<!--BOOK_CODE:'+sample_id+'-->'
            else:
                entry['html']='<pre class="book-output"'+attrs+'>'+html.escape(text)+'</pre>'
        else:
            raise ValueError(kind)
        blocks.append(entry)
    assert [e['id'] for e in exercises]==solutions and len(exercises)>=5
    refs=references(source)
    book={'schema':'continuous-textbook-v1','track':4,'sourceSha256':hashlib.sha256(source.encode()).hexdigest(),'sourcePath':f'live/track4-book/ch{number:02d}.md','blocks':blocks,'sections':sections,'samples':samples,'exercises':exercises,'html':'\n'.join(b['html'] for b in blocks),'text':'\n\n'.join(b['text'] for b in blocks)}
    intro=[b['text'] for b in blocks if b['kind']=='paragraph'][:3]
    page={'id':page_id,'kind':'book','title':f'{number}장. {TITLES[number-1]}','paragraphs':intro,'book':book}
    return {'id':f't4-book-ch{number:02d}','track':4,'chapter':number,'title':TITLES[number-1],'subtitle':intro[0],'desc':intro[0],'pages':[page],'references':refs,'edition':'continuous-web-book-20260925'}


HYDRATE="""
/* LOSSLESS_BOOK_HYDRATION_V1: remove duplicate aggregates in transit only. */
for (const chapter of COURSE.chapters) for (const page of chapter.pages) {
  const book=page.book;if(!book)continue;
  if(book.html===undefined)book.html=book.blocks.map(b=>b.html).join('\\n');
  if(book.text===undefined)book.text=book.blocks.map(b=>b.text).join('\\n\\n');
  for(const sample of book.samples)if(sample.runSameCode){sample.runCode=sample.code;delete sample.runSameCode;}
}
"""


def build(root=ROOT):
    path=root/'live/content.json';course=json.loads(path.read_text())
    before=[c for c in course['chapters'] if c.get('track')!=4]
    before_hash=digest(before)
    sources=[(root/f'live/track4-book/ch{n:02d}.md').read_text() for n in range(1,13)]
    project={}
    new=[compile_chapter(n,s,project) for n,s in enumerate(sources,1)]
    assert set(project)=={'index.html','style.css','app.js','topics.json'}
    existing4=[c for c in course['chapters'] if c.get('track')==4]
    assert not existing4 or all(c.get('edition')=='continuous-web-book-20260925' for c in existing4), 'Unexpected old Track 4 data requires explicit migration'
    course['chapters']=[c for c in before if c.get('track',0)<4]+new+[c for c in before if c.get('track',0)>4]
    assert before_hash==digest([c for c in course['chapters'] if c.get('track')!=4])
    track=next(t for t in course['tracks'] if t['id']==4)
    track.update(title='웹의 기본',subtitle='화면·동작·데이터를 연결하는 12장 연속 교재',available=True,edition='continuous-web-book-20260925')
    course['label']=LABEL
    ids=[p['id'] for c in course['chapters'] for p in c['pages']]
    assert len(ids)==len(set(ids))
    exercises=[e['id'] for c in course['chapters'] for p in c['pages'] if p.get('book') for e in p['book']['exercises']]
    assert len(exercises)==len(set(exercises)), 'Cross-track exercise identity collision'
    audit={'kind':'Lossless original manuscript compilation; not a device test','chapters':12,'authoredCharacters':sum(map(len,sources)),'blocks':sum(len(c['pages'][0]['book']['blocks']) for c in new),'exercises':sum(len(c['pages'][0]['book']['exercises']) for c in new),'codeBlocks':sum(len(c['pages'][0]['book']['samples']) for c in new),'referenceAssociations':60,'referencesPerChapter':5,'fiveUniqueBooksPerChapterClaim':False,'paidBooksReadInFullClaim':False,'otherTracksSha256':before_hash,'fixedPageQuota':False,'sourcePreservation':True}
    course['track4BookAudit']=audit
    path.write_text(json.dumps(course,ensure_ascii=False,indent=2)+'\n')
    project_dir=root/'live/track4-book/project';project_dir.mkdir(parents=True,exist_ok=True)
    for name,text in project.items(): (project_dir/name).write_text(text)
    (root/'live/track4-book/manuscript.md').write_text('# TRACK 04 · 웹의 기본 — 화면·동작·데이터 연결하기\n\n'+ '\n\n'.join(sources))
    source_map={'accessDate':'2026-09-25','scope':'Five principal source works/course/documentation collections per chapter; sources reused across chapters; not sixty paid books read in full. 2024 works are supplemental.','chapters':[{'chapter':c['chapter'],'title':c['title'],'sources':c['references']} for c in new]}
    (root/'live/track4-book/source-map.json').write_text(json.dumps(source_map,ensure_ascii=False,indent=2)+'\n')
    md=['# Track 4 장별 원전 확인 기록','',source_map['scope'],'']
    for c in new:
        md.extend([f"## {c['chapter']}장. {c['title']}",''])
        for r in c['references']:
            md.extend([f"{r['number']}. [{r['book']}]({r['url']}) — {r['author']} / {r['year']}",f"   확인 범위: {r['scope']}. {r['note']}",''])
    (root/'live/track4-book/source-map.md').write_text('\n'.join(md))
    template=(root/'live/reader.template.html').read_text()
    css=(root/'live/track3-book/reader.css').read_text()+'\n'+(root/'live/track4-book/reader.css').read_text()
    js=(root/'live/track3-book/reader.js').read_text()+'\n'+(root/'live/track4-book/reader.js').read_text()
    assert '</script' not in js.lower()
    template=template.replace('</style>','\n'+css+'\n</style>',1)
    assert template.count('\nrender();\n</script>')==1
    template=template.replace('\nrender();\n</script>','\n'+js+'\nrender();\n</script>',1)
    needle="const COURSE=JSON.parse(document.getElementById('course').textContent);"
    assert template.count(needle)==1
    template=template.replace(needle,needle+'\n'+HYDRATE)
    transport=copy.deepcopy(course)
    saved=0
    for c in transport['chapters']:
        for p in c['pages']:
            b=p.get('book')
            if not b:continue
            saved+=len(b.get('html','').encode())+len(b.get('text','').encode())
            b.pop('html',None);b.pop('text',None)
            for s in b['samples']:
                if s.get('runCode') is not None and s['runCode']==s['code']:
                    s.pop('runCode');s['runSameCode']=True
    raw=json.dumps(transport,ensure_ascii=False,separators=(',',':')).replace('<','\\u003c')
    output=template.replace('__COURSE_JSON__',raw)
    assert '__COURSE_JSON__' not in output
    size=len(output.encode());assert size<=3*1024*1024,(size,'Native HTML limit exceeded: no text may be truncated')
    (root/'live/reader.html').write_text(output)
    review=root/'review';review.mkdir(exist_ok=True)
    audit.update(readerBytes=size,duplicateAggregateBytesRemoved=saved)
    (review/'track4-book-compile.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(audit,ensure_ascii=False))
    return course

if __name__=='__main__':build()
