"""Compile authored Markdown as complete chapters, never as fixed lesson cards.

Only Track 3 is replaced. Other tracks and legacy note identities are retained.
No network or model-generated text is used by this compiler.
"""
from __future__ import annotations
import ast
import hashlib
import html
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
LABEL = '0.10.0 · Track 3 연속 서술형 교재 · 9장'
TITLES = [
    '코드는 어떤 순서로 움직일까?', '문제를 코드로 바꾸는 법',
    '변수와 데이터의 변화를 추적하기', '함수 안과 밖을 읽는 법',
    '자주 쓰는 문제 해결 패턴', '오류를 재현하고 원인을 찾는 법',
    '테스트로 코드가 맞는지 검증하기', '엉킨 코드를 읽기 좋게 고치기',
    '남이 만든 코드를 해부하고 수정하기',
]
REFERENCES = [
    {'book':'Automate the Boring Stuff with Python, 3rd Edition','author':'Al Sweigart','year':2025,'url':'https://automatetheboringstuff.com/3e/','note':'각 장 끝에 실제로 참고한 공개 본문 절을 표시했습니다.'},
    {'book':'Think Python, 3rd Edition','author':'Allen B. Downey','year':2024,'url':'https://allendowney.github.io/ThinkPython/','note':'2024년 판입니다. 2025년 이후 출간 도서로 표시하지 않습니다.'},
    {'book':'Python Programming MOOC 2026','author':'University of Helsinki','year':2026,'url':'https://programming-26.mooc.fi/','note':'대학 강좌 운영 자료. 관련 공개 본문을 참고했습니다.'},
    {'book':'6.1000, Fall 2025','author':'MIT','year':2025,'url':'https://introcomp.mit.edu/fall25','note':'Lecture 3·5·6의 관련 텍스트와 도표를 확인했습니다.'},
    {'book':'CS50’s Introduction to Programming with Python','author':'Harvard University','year':'공개 강좌','url':'https://cs50.harvard.edu/python/','note':'관련 강의 노트. 확인일 2026-09-24는 최초 집필 연도가 아닙니다.'},
]
INLINE = re.compile(r'(`[^`\n]+`|\*\*[^*]+\*\*|\*[^*\n]+\*|\[[^\]]+\]\(https?://[^\s)]+\))')


def inline(text: str) -> str:
    parts = []
    last = 0
    for match in INLINE.finditer(text):
        parts.append(html.escape(text[last:match.start()]))
        value = match.group()
        if value.startswith('`'):
            parts.append('<code>' + html.escape(value[1:-1]) + '</code>')
        elif value.startswith('**'):
            parts.append('<strong>' + html.escape(value[2:-2]) + '</strong>')
        elif value.startswith('*'):
            parts.append('<em>' + html.escape(value[1:-1]) + '</em>')
        else:
            link = re.fullmatch(r'\[([^\]]+)\]\((https?://[^\s)]+)\)', value)
            assert link
            parts.append('<a target="_blank" rel="noopener noreferrer" href="' + html.escape(link[2], quote=True) + '">' + html.escape(link[1]) + '</a>')
        last = match.end()
    parts.append(html.escape(text[last:]))
    return ''.join(parts)


def blocks_from_markdown(text: str) -> list[dict]:
    lines = text.splitlines()
    blocks = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith('```'):
            info = line[3:].strip()
            i += 1
            body = []
            while i < len(lines) and not lines[i].startswith('```'):
                body.append(lines[i])
                i += 1
            if i == len(lines):
                raise ValueError('Unclosed code fence')
            blocks.append({'kind':'fence','info':info,'text':'\n'.join(body)})
            i += 1
            continue
        heading = re.match(r'^(#{1,4}) (.+)$', line)
        if heading:
            blocks.append({'kind':'heading','level':len(heading[1]),'text':heading[2]})
            i += 1
            continue
        if line.startswith('|'):
            rows = []
            while i < len(lines) and lines[i].startswith('|'):
                rows.append([x.strip() for x in lines[i].strip('|').split('|')])
                i += 1
            if len(rows) < 2 or not all(re.fullmatch(r':?-+:?', c.replace(' ','')) for c in rows[1]):
                raise ValueError('Malformed Markdown table')
            blocks.append({'kind':'table','rows':[rows[0]]+rows[2:],'text':'\n'.join(' | '.join(r) for r in [rows[0]]+rows[2:])})
            continue
        if line.startswith('> '):
            body = []
            while i < len(lines) and lines[i].startswith('> '):
                body.append(lines[i][2:])
                i += 1
            blocks.append({'kind':'quote','text':' '.join(body)})
            continue
        if re.match(r'^\d+\. ', line):
            items = []
            while i < len(lines) and re.match(r'^\d+\. ', lines[i]):
                items.append(re.sub(r'^\d+\. ', '', lines[i]))
                i += 1
            blocks.append({'kind':'list','items':items,'text':'\n'.join(items)})
            continue
        if line.strip() == '---':
            blocks.append({'kind':'rule','text':''})
            i += 1
            continue
        body = [line]
        i += 1
        while i < len(lines) and lines[i].strip() and not re.match(r'^(#{1,4} |```|\||> |\d+\. )', lines[i]):
            body.append(lines[i])
            i += 1
        blocks.append({'kind':'paragraph','text':' '.join(body)})
    return blocks


def compile_chapter(number: int, source: str) -> dict:
    raw = blocks_from_markdown(source)
    assert raw and raw[0]['text'] == f'{number}장. {TITLES[number-1]}'
    page_id = f't3-book-c{number:02d}'
    blocks = []
    samples = []
    sections = []
    exercises = []
    solutions = []
    section_title = TITLES[number-1]
    capstone_functions = ''
    for index, item in enumerate(raw):
        anchor = f'{page_id}-b{index+1:04d}'
        kind = item['kind']
        text = item['text']
        attrs = f' id="{anchor}" data-book-anchor="{anchor}"'
        entry = {'id':anchor,'kind':kind,'text':text,'section':section_title}
        if kind == 'heading':
            level = item['level']
            entry['level'] = level
            if level == 2:
                section_title = text
                entry['section'] = section_title
                sections.append({'id':anchor,'title':text})
            ex = re.match(r'연습 (\d+-\d+)\.', text)
            sol = re.match(r'풀이 (\d+-\d+)', text)
            extra = ''
            if ex:
                eid = f'book-exercise-{ex[1]}'
                entry['exercise'] = eid
                exercises.append({'id':eid,'heading':text,'anchor':anchor})
                extra = '<button class="book-answer-button" data-action="book-answer" data-exercise="'+eid+'" data-title="'+html.escape(text,quote=True)+'">내 답 적기</button>'
            if sol:
                entry['solution'] = f'book-exercise-{sol[1]}'
                solutions.append(entry['solution'])
            entry['html'] = f'<h{level}{attrs}>' + inline(text) + extra + f'</h{level}>'
        elif kind == 'paragraph':
            entry['html'] = f'<p{attrs}>' + inline(text) + '</p>'
        elif kind == 'quote':
            entry['html'] = f'<blockquote{attrs}>' + inline(text) + '</blockquote>'
        elif kind == 'rule':
            entry['html'] = '<hr'+attrs+'>'
        elif kind == 'list':
            entry['html'] = '<ol'+attrs+'>' + ''.join('<li>'+inline(x)+'</li>' for x in item['items']) + '</ol>'
        elif kind == 'table':
            rows = item['rows']
            entry['html'] = '<table'+attrs+'><thead><tr>' + ''.join('<th scope="col">'+inline(c)+'</th>' for c in rows[0]) + '</tr></thead><tbody>' + ''.join('<tr>'+''.join('<td>'+inline(c)+'</td>' for c in row)+'</tr>' for row in rows[1:]) + '</tbody></table>'
        elif kind == 'fence':
            info = item['info']
            if info.startswith('python'):
                sample_id = f'{page_id}-code{len(samples)+1:03d}'
                run_code = text
                is_fragment = 'fragment' in info
                expected_error = re.search(r'error=([A-Za-z]+)', info)
                expected = None
                if index+1 < len(raw) and raw[index+1]['kind']=='fence' and raw[index+1]['info']=='text':
                    expected = raw[index+1]['text']
                if number == 9 and '최종 프로그램 전체' in section_title:
                    tree = ast.parse(text)
                    capstone_functions = '\n\n'.join(ast.get_source_segment(text,n) for n in tree.body if isinstance(n,ast.FunctionDef))
                if 'capstone-tests' in info:
                    assert capstone_functions, 'Capstone definitions missing'
                    run_code = capstone_functions + '\n\n' + text
                samples.append({'id':sample_id,'anchor':anchor,'title':section_title,'code':text,'runCode':None if is_fragment else run_code,'expectedOutput':expected,'expectedError':expected_error[1] if expected_error else None,'fragment':is_fragment,'capstoneTests':'capstone-tests' in info})
                entry['sampleId'] = sample_id
                entry['html'] = '<!--BOOK_CODE:'+sample_id+'-->'
            else:
                entry['html'] = '<pre class="book-output"'+attrs+'>' + html.escape(text) + '</pre>'
        else:
            raise ValueError(kind)
        blocks.append(entry)
    assert len(exercises) > 0 and [e['id'] for e in exercises] == solutions, 'Exercise/solution sequence mismatch'
    source_fences = len(re.findall(r'^```[^`]*$', source, re.M)) // 2
    assert source_fences == sum(x['kind']=='fence' for x in blocks)
    book = {'schema':'continuous-textbook-v1','sourceSha256':hashlib.sha256(source.encode()).hexdigest(),'blocks':blocks,'sections':sections,'exercises':exercises,'samples':samples,'html':'\n'.join(x['html'] for x in blocks),'text':'\n\n'.join(x['text'] for x in blocks),'sourcePath':f'live/track3-book/ch{number:02d}.md'}
    first_paragraphs = [x['text'] for x in blocks if x['kind']=='paragraph'][:3]
    page = {'id':page_id,'kind':'book','title':f'{number}장. {TITLES[number-1]}','paragraphs':first_paragraphs,'book':book}
    return {'id':f't3-book-ch{number:02d}','track':3,'chapter':number,'title':TITLES[number-1],'subtitle':first_paragraphs[0],'desc':first_paragraphs[0],'references':REFERENCES,'pages':[page],'edition':'continuous-book-20260924'}


def legacy_text(page: dict) -> str:
    chunks = [page.get('title',''),*(page.get('paragraphs') or []),*(page.get('bullets') or [])]
    for key in ['code','codeNote','practicePrompt','question','answer']:
        if page.get(key):
            chunks.append(page[key])
    for item in page.get('glossary',[]):
        chunks.extend(str(item.get(k,'')) for k in ['term','description','usage','example'])
    for item in page.get('extras',[]):
        chunks.extend([str(item.get('title','')),str(item.get('body',''))])
    return '\n\n'.join(chunks)


def build(root: pathlib.Path = ROOT) -> dict:
    content_path = root/'live/content.json'
    course = json.loads(content_path.read_text())
    untouched = [c for c in course['chapters'] if c.get('track') != 3]
    before_digest = hashlib.sha256(json.dumps(untouched,ensure_ascii=False,sort_keys=True).encode()).hexdigest()
    archive = course.setdefault('legacyTrack3', {})
    aliases = course.setdefault('bookAliases', {})
    for old in course['chapters']:
        if old.get('track') != 3 or old.get('edition') == 'continuous-book-20260924':
            continue
        number = int(old['chapter'])
        for index, page in enumerate(old['pages']):
            archive.setdefault(page['id'],{'id':page['id'],'chapter':number,'chapterId':old['id'],'title':page['title'],'text':legacy_text(page),'index':index,'total':len(old['pages'])})
            aliases[page['id']] = f't3-book-c{number:02d}'
    sources = [(root/f'live/track3-book/ch{i:02d}.md').read_text() for i in range(1,10)]
    new = [compile_chapter(i,s) for i,s in enumerate(sources,1)]
    combined = []
    inserted = False
    for old in course['chapters']:
        if old.get('track') == 3:
            if not inserted:
                combined.extend(new)
                inserted = True
            continue
        if not inserted and old.get('track',0) > 3:
            combined.extend(new)
            inserted = True
        combined.append(old)
    if not inserted:
        combined.extend(new)
    course['chapters'] = combined
    course['label'] = LABEL
    track = next(t for t in course['tracks'] if t['id']==3)
    track.update({'title':'코드 읽기와 문제 해결','subtitle':'설명에서 예제·연습·풀이까지 이어 읽는 9장 교재','available':True,'edition':'continuous-book-20260924'})
    all_ids = [p['id'] for c in combined for p in c['pages']]
    assert len(all_ids)==len(set(all_ids))
    assert before_digest == hashlib.sha256(json.dumps([c for c in combined if c.get('track')!=3],ensure_ascii=False,sort_keys=True).encode()).hexdigest()
    audit = {'kind':'continuous-book compilation, not physical-device validation','chapters':9,'authoredCharacters':sum(len(s) for s in sources),'blocks':sum(len(c['pages'][0]['book']['blocks']) for c in new),'exercises':sum(len(c['pages'][0]['book']['exercises']) for c in new),'pythonBlocks':sum(len(c['pages'][0]['book']['samples']) for c in new),'legacyPageRecords':len(archive),'otherTracksSha256':before_digest,'fixedLessonPageQuota':False,'allFencesPreserved':True,'referencesPerChapter':5}
    course['track3BookAudit'] = audit
    content_path.write_text(json.dumps(course,ensure_ascii=False,indent=2)+'\n')
    template = (root/'live/reader.template.html').read_text()
    assert template.count('__COURSE_JSON__')==1
    css = (root/'live/track3-book/reader.css').read_text()
    js = (root/'live/track3-book/reader.js').read_text()
    assert '</script' not in js.lower(), 'Script close in book module'
    assert template.count('\nrender();\n</script>')==1
    template = template.replace('</style>', '\n'+css+'\n</style>',1)
    template = template.replace('\nrender();\n</script>', '\n'+js+'\nrender();\n</script>',1)
    raw = json.dumps(course,ensure_ascii=False,separators=(',',':')).replace('<','\\u003c')
    (root/'live/reader.html').write_text(template.replace('__COURSE_JSON__', raw))
    (root/'live/track3-book/manuscript.md').write_text('# TRACK 03 · 코드 읽기와 문제 해결\n\n연속 서술형 교재 · 2026-09-24\n\n'+ '\n\n'.join(sources))
    capstone = [s for s in new[-1]['pages'][0]['book']['samples'] if s['capstoneTests']]
    assert len(capstone)==1
    (root/'live/track3-book/capstone.py').write_text(capstone[0]['runCode']+'\n')
    (root/'review').mkdir(exist_ok=True)
    (root/'review/track3-book-compile.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(audit,ensure_ascii=False))
    return course


if __name__ == '__main__':
    build()
