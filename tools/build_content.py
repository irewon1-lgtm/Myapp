import json,pathlib,re

root=pathlib.Path(__file__).resolve().parents[1]

EXPECTED_TRACK3_TITLES=[
 "코드는 어떤 순서로 움직일까?",
 "문제를 코드로 바꾸는 법",
 "변수와 데이터의 변화를 추적하기",
 "함수 안과 밖을 읽는 법",
 "자주 쓰는 문제 해결 패턴",
 "오류를 재현하고 원인을 찾는 법",
 "테스트로 코드가 맞는지 검증하기",
 "엉킨 코드를 읽기 좋게 고치기",
 "남이 만든 코드를 해부하고 수정하기",
]
EXPECTED_TRACK3_PAGE_COUNTS=[61,59,65,65,63,64,64,64,66]

def plain(s):
 s=str(s or '')
 s=re.sub(r'\[([^\]]+)\]\((https?://[^)]+)\)',r'\1 (\2)',s)
 s=s.replace('**','').replace('__','')
 s=re.sub(r'\x60([^\x60]+)\x60',r'\1',s)
 return s.strip()

def table_extra(lines):
 rows=[]
 for ln in lines:
  cells=[plain(x) for x in ln.strip().strip('|').split('|')]
  rows.append(cells)
 if len(rows)<2:return None
 headers=rows[0]
 data=[r for r in rows[2:] if r]
 body=[]
 for r in data:
  items=[]
  for i,v in enumerate(r):
   items.append((headers[i]+': '+v) if i<len(headers) else v)
  body.append(' · '.join(items))
 return {'title':'표','body':'\n'.join(body)} if body else None

def source_parts(sec):
 ms=list(re.finditer(r'^### (.+)$',sec,re.M))
 extras=[];refs=[]
 intro=sec[:ms[0].start()].strip() if ms else sec.strip()
 for i,m in enumerate(ms):
  title=plain(m.group(1))
  body=sec[m.end():ms[i+1].start() if i+1<len(ms) else len(sec)].strip()
  ps=[plain(p) for p in re.split(r'\n\s*\n',body) if plain(p) and plain(p)!='---']
  extras.append({'title':title,'body':'\n'.join(ps)})
  citation=ps[0] if ps else ''
  author=citation.split('·')[0].strip() if citation else ''
  ym=re.search(r'\b(20\d{2})\b',citation)
  year=int(ym.group(1)) if ym else 2026
  note=' · '.join(x for x in ps if x.startswith('확인 구간:') or x.startswith('집필 적용:'))
  refs.append({
   'book':re.sub(r'^\[[A-Z]\]\s*','',title),
   'author':author,
   'year':year,
   'note':note or '장별 참고자료'
  })
 paras=[plain(p) for p in re.split(r'\n\s*\n',intro) if plain(p)]
 return paras,extras,refs

def parse_generic(sec):
 fences=list(re.finditer(r'\x60\x60\x60([^\n]*)\n([\s\S]*?)\n\x60\x60\x60',sec))
 code=None;note=None
 if fences:
  lang=fences[0].group(1).strip().lower()
  if lang in ('python','py',''):
   code=fences[0].group(2)
  if len(fences)>1 and fences[1].group(1).strip().lower()=='text':
   note=fences[1].group(2)
 clean=re.sub(r'\x60\x60\x60[^\n]*\n[\s\S]*?\n\x60\x60\x60','\n\n',sec)
 clean=re.sub(r'^\*\*(독립 실행 예제|검증한 실행 결과|문제 코드 · 먼저 결과를 예측하세요)\*\*\s*$','',clean,flags=re.M)
 lines=clean.splitlines()
 extras=[];bullets=[];kept=[];i=0
 while i<len(lines):
  if lines[i].lstrip().startswith('|'):
   block=[]
   while i<len(lines) and lines[i].lstrip().startswith('|'):
    block.append(lines[i]);i+=1
   ex=table_extra(block)
   if ex:extras.append(ex)
   continue
  m=re.match(r'^\s*(\d+)\.\s+(.+)$',lines[i])
  if m:
   bullets.append(f"{m.group(1)}. {plain(m.group(2))}")
   i+=1;continue
  if lines[i].strip()=='---':
   i+=1;continue
  kept.append(lines[i]);i+=1
 paras=[]
 for p in re.split(r'\n\s*\n','\n'.join(kept)):
  p=plain(' '.join(x.strip() for x in p.splitlines() if x.strip()))
  if p:paras.append(p)
 return paras,bullets,extras,code,note

def build_track3(md,old_track3,non_track3_ids):
 old_by_num={int(c.get('chapter',0)):c for c in old_track3}
 cmatches=list(re.finditer(r'^# CHAPTER (\d{2}) · (.+)$',md,re.M))
 assert len(cmatches)==9,'Track 3 manuscript must contain 9 chapters'
 chapters=[]
 practice_count=solution_count=code_count=0
 used_ids=set(non_track3_ids)
 for ci,cm in enumerate(cmatches):
  num=int(cm.group(1));title=cm.group(2).strip()
  assert title==EXPECTED_TRACK3_TITLES[num-1],(num,title)
  end=cmatches[ci+1].start() if ci+1<len(cmatches) else len(md)
  body=md[cm.end():end]
  hs=list(re.finditer(r'^## (.+)$',body,re.M))
  assert len(hs)==EXPECTED_TRACK3_PAGE_COUNTS[num-1],(num,len(hs))
  old=old_by_num.get(num,{})
  old_pages=old.get('pages',[])
  pages=[];chapter_refs=[]
  for pi,h in enumerate(hs):
   ptitle=h.group(1).strip()
   sec=body[h.end():hs[pi+1].start() if pi+1<len(hs) else len(body)].strip()
   generated=f"t3-c{num:02d}-p{pi+1:03d}"
   old_id=old_pages[pi].get('id') if pi<len(old_pages) and old_pages[pi].get('id') else None
   pid=old_id if old_id and old_id not in used_ids else generated
   if pid in used_ids:
    base=generated;serial=2
    while f"{base}-v{serial}" in used_ids:serial+=1
    pid=f"{base}-v{serial}"
   used_ids.add(pid)
   page={'id':pid,'title':ptitle,'eyebrow':f"CHAPTER {num:02d} · {title}"}
   if ptitle.startswith('시작 용어'):
    page['kind']='glossary';page['paragraphs']=[]
    terms=[]
    ms=list(re.finditer(r'^### (.+)$',sec,re.M))
    for j,m in enumerate(ms):
     term=plain(m.group(1))
     tbody=sec[m.end():ms[j+1].start() if j+1<len(ms) else len(sec)].strip()
     parts=re.split(r'\n\s*\n',tbody)
     desc=plain(parts[0]) if parts else ''
     example=''
     for x in parts[1:]:
      px=plain(x)
      if px.startswith('예시:'):example=px[len('예시:'):].strip()
     terms.append({'term':term,'description':desc,'example':example})
    page['glossary']=terms
   elif ptitle=='이 장의 참고자료 5종':
    page['kind']='concept'
    paras,extras,refs=source_parts(sec)
    page['paragraphs']=paras;page['extras']=extras;chapter_refs=refs
   else:
    if '바로 해설' in ptitle:
     kind='solution';solution_count+=1
    elif ptitle.startswith('연습 '):
     kind='practice';practice_count+=1
    elif ptitle=='이 장을 마치며':
     kind='summary'
    elif '\x60\x60\x60python' in sec:
     kind='example'
    else:
     kind='concept'
    page['kind']=kind
    paras,bullets,extras,code,note=parse_generic(sec)
    if paras:page['paragraphs']=paras
    if bullets:page['bullets']=bullets
    if extras:page['extras']=extras
    if code:
     page['code']=code
     page['codeLabel']='문제 코드' if kind=='practice' else 'Python'
     code_count+=1
    if note is not None:
     page['codeNote']='검증한 실행 결과\n'+note
    if kind=='practice':
     page['answerOnNext']=True
   pages.append(page)
  assert len(chapter_refs)==5,(num,len(chapter_refs))
  summary=next((p for p in pages if p.get('kind')=='summary'),None)
  remember=(summary.get('paragraphs') or [''])[0] if summary else ''
  hero=pages[0]
  desc=(hero.get('paragraphs') or [''])[0]
  goal=''
  for x in hero.get('paragraphs',[]):
   if x.startswith('이 장의 목표:'):goal=x.split(':',1)[1].strip()
  chapters.append({
   'id':old.get('id') or f"track3-ch{num:02d}",
   'track':3,
   'chapter':num,
   'title':title,
   'subtitle':goal,
   'desc':desc,
   'remember':remember,
   'references':chapter_refs,
   'pages':pages
  })
 assert sum(len(c['pages']) for c in chapters)==571
 assert practice_count==180 and solution_count==180,(practice_count,solution_count)
 assert code_count==134,code_count
 for c in chapters:
  for i,p in enumerate(c['pages'][:-1]):
   if p.get('answerOnNext'):
    assert c['pages'][i+1].get('kind')=='solution',(c['chapter'],p['title'])
 return chapters

course=json.loads((root/'live/content.json').read_text())
manuscript=(root/'live/track3.manuscript.md').read_text()

old_track3=[c for c in course['chapters'] if c.get('track')==3]
non_track3_ids=[p['id'] for c in course['chapters'] if c.get('track')!=3 for p in c.get('pages',[])]
new_track3=build_track3(manuscript,old_track3,non_track3_ids)

new_chapters=[];inserted=False
for c in course['chapters']:
 if c.get('track')==3:
  if not inserted:
   new_chapters.extend(new_track3);inserted=True
  continue
 if not inserted and int(c.get('track',0) or 0)>3:
  new_chapters.extend(new_track3);inserted=True
 new_chapters.append(c)
if not inserted:new_chapters.extend(new_track3)
course['chapters']=new_chapters

track3=next((t for t in course.get('tracks',[]) if t.get('id')==3),None)
if track3 is None:
 track3={'id':3}
 course.setdefault('tracks',[]).append(track3)
track3.update({
 'title':'코드 읽기와 문제 해결',
 'subtitle':'코드를 외우는 단계에서, 코드를 이해하고 고치는 단계로.',
 'available':True
})
course['label']='0.9.8 · Track 3 코드 읽기와 문제 해결 · 9챕터 상세판'

ids=[p['id'] for c in course['chapters'] for p in c['pages']]
assert len(ids)==len(set(ids))
assert len(course['chapters'])>=16
track3_chapters=[c for c in course['chapters'] if c.get('track')==3]
assert [c.get('chapter') for c in track3_chapters]==list(range(1,10))
assert [c.get('title') for c in track3_chapters]==EXPECTED_TRACK3_TITLES
assert sum(len(c.get('pages',[])) for c in track3_chapters)==571
for c in course['chapters']:
 for i,p in enumerate(c['pages']):
  if p.get('answerOnNext'):
   assert i+1<len(c['pages']) and c['pages'][i+1]['kind']=='solution'

(root/'live/content.json').write_text(json.dumps(course,ensure_ascii=False,indent=2)+'\n')
raw=json.dumps(course,ensure_ascii=False,separators=(',',':')).replace('<','\\u003c')
template=(root/'live/reader.template.html').read_text()
assert template.count('__COURSE_JSON__')==1
(root/'live/reader.html').write_text(template.replace('__COURSE_JSON__',raw))
print('Built reader.html:',(root/'live/reader.html').stat().st_size,'bytes;',len(ids),'stable pages; Track 3:',len(track3_chapters),'chapters /',sum(len(c['pages']) for c in track3_chapters),'pages')