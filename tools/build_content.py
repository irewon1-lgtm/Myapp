import json,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
course=json.loads((root/'live/content.json').read_text())
ids=[p['id'] for c in course['chapters'] for p in c['pages']]
assert len(ids)==len(set(ids))
assert len(course['chapters'])>=16
for c in course['chapters']:
 for i,p in enumerate(c['pages']):
  if p.get('answerOnNext'):assert c['pages'][i+1]['kind']=='solution'
raw=json.dumps(course,ensure_ascii=False,separators=(',',':')).replace('<','\\u003c')
template=(root/'live/reader.template.html').read_text()
assert template.count('__COURSE_JSON__')==1
(root/'live/reader.html').write_text(template.replace('__COURSE_JSON__',raw))
print('Built reader.html:',(root/'live/reader.html').stat().st_size,'bytes;',len(ids),'stable pages')
