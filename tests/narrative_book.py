"""Real authored-book regression. No network or model calls."""
from pathlib import Path
import json,hashlib,subprocess,sys
R=Path(__file__).resolve().parents[1];D=R/'docs/narrative-1.2';m=json.loads((D/'book-metrics.json').read_text());c=json.loads((R/'public/content/catalog.json').read_text());b=next(x for x in c['books'] if x['id']==m['bookId'])
assert len(c['books'])==13
for x in c['books']:
 if x['id'] in m['otherBooksUntouched']:assert hashlib.sha256(json.dumps(x,sort_keys=True,ensure_ascii=False).encode()).hexdigest()==m['otherBooksUntouched'][x['id']],x['id']
assert hashlib.sha256((R/'exact_source/8KCuHHeC_M0/transcript.md').read_bytes()).hexdigest()==m['sourceTranscriptSha256']
blocks=[p for ch in b['chapters'] for p in ch['blocks']];assert len(blocks)==m['newBlocks'];assert len({p['id'] for p in blocks})==len(blocks)
assert len(b['questions'])==36;assert sum(p['type']=='image' for p in blocks)==10
assert sum(p.get('_format')=='code' for p in blocks)==39
examples=json.loads((D/'example-checks.json').read_text())
for ex in examples:
 r=subprocess.run([sys.executable,'-c',ex['code']],input=ex['stdin'],text=True,capture_output=True,timeout=4)
 if ex['expectedError']:assert r.returncode!=0 and ex['expectedError'] in r.stderr,ex['id']
 else:assert r.returncode==0 and r.stdout==ex['expectedStdout'],ex['id']
js=(R/'public/library.js').read_text().strip();assert json.loads(js.split('=',1)[1].rstrip(';'))==c
print(json.dumps({'bookChecks':'pass','executedExamples':len(examples),'otherBooksPreserved':12,'rawTranscriptUnchanged':True}))
