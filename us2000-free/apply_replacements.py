#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json
from pathlib import Path

HERE=Path(__file__).resolve().parent
OUT=HERE/'output'
REP=HERE/'replacements.json'
REMOVE_TO_ADD={'ENVA':'ICFI','TRUP':'OMF','YELP':'EE'}

manifest=json.loads((OUT/'manifest.json').read_text(encoding='utf-8'))
replacement_doc=json.loads(REP.read_text(encoding='utf-8'))
if replacement_doc.get('schema')!='US2000_COLLISION_REPLACEMENTS_1': raise RuntimeError('REPLACEMENT_SCHEMA')
repmap={r['ticker']:r for r in replacement_doc['records']}
if set(repmap)!=set(REMOVE_TO_ADD.values()): raise RuntimeError('REPLACEMENT_TICKERS')
if replacement_doc.get('totalHold')!=0: raise RuntimeError('REPLACEMENT_HOLD')

records=[]
for chunk in manifest['chunks']:
    raw=(OUT/chunk['file']).read_bytes()
    if hashlib.sha256(raw).hexdigest()!=chunk['sha256']: raise RuntimeError('OLD_CHUNK_HASH:'+chunk['file'])
    records.extend(json.loads(raw))
if len(records)!=500: raise RuntimeError('OLD_RECORD_COUNT')
old_by={r['ticker']:r for r in records}
for old,new in REMOVE_TO_ADD.items():
    if old not in old_by: raise RuntimeError('OLD_TICKER_MISSING:'+old)
    repl=dict(repmap[new]); repl['ordinal']=old_by[old]['ordinal']; records[records.index(old_by[old])]=repl

if len(records)!=500 or len({r['ticker'] for r in records})!=500 or len({int(r['cik']) for r in records})!=500: raise RuntimeError('CORRECTED_IDENTITY_CARDINALITY')
if any(x in {r['ticker'] for r in records} for x in REMOVE_TO_ADD): raise RuntimeError('OLD_COLLISION_TICKER_REMAINS')
if not set(REMOVE_TO_ADD.values()) <= {r['ticker'] for r in records}: raise RuntimeError('REPLACEMENT_MISSING')
keys=list(records[0]['metrics'])
if len(keys)!=10: raise RuntimeError('METRIC_COUNT')
if any(set(r['metrics'])!=set(keys) for r in records): raise RuntimeError('METRIC_KEYS')
if sum(v.get('status')=='HOLD' for r in records for v in r['metrics'].values()): raise RuntimeError('HOLD_REMAINS')

summary={k:{'numeric':0,'naBasis':0,'hold':0} for k in keys}
hold_details=[]; drawdown_positive=0; drawdown_samples=[]
for r in records:
    for k,v in r['metrics'].items():
        s=v.get('status')
        if s=='NUMERIC': summary[k]['numeric']+=1
        elif s=='NA_BASIS': summary[k]['naBasis']+=1
        elif s=='HOLD': summary[k]['hold']+=1; hold_details.append({'ticker':r['ticker'],'metric':k,'reason':v.get('reason')})
        else: raise RuntimeError('BAD_STATUS:'+r['ticker']+':'+k)
    dd=r['metrics']['Drawdown_52W_Pct']
    if dd.get('status')=='NUMERIC' and isinstance(dd.get('value'),(int,float)) and dd['value']>0:
        drawdown_positive+=1
        if len(drawdown_samples)<5: drawdown_samples.append({'ticker':r['ticker'],'value':dd['value'],'raw':dd.get('raw')})

chunks=[]
for n,start in enumerate(range(0,500,50)):
    part=records[start:start+50]
    raw=json.dumps(part,separators=(',',':'),ensure_ascii=False).encode()
    name=f'chunk-{n:02d}.json'; (OUT/name).write_bytes(raw)
    chunks.append({'file':name,'count':len(part),'sha256':hashlib.sha256(raw).hexdigest()})
canonical=json.dumps(records,separators=(',',':'),ensure_ascii=False).encode()
manifest['schema']='US2000_FREE_NEW500_2'
manifest['generatedAt']=replacement_doc['generatedAt']
manifest['asOf']=replacement_doc['asOf']
manifest['summary']=summary
manifest['totalResolved']=5000
manifest['totalHold']=0
manifest['resolvedRatio']=1.0
manifest['recordsSha256']=hashlib.sha256(canonical).hexdigest()
manifest['chunks']=chunks
manifest['holdDetails']=[]
manifest['identityAudit']['uniqueTickers']=500
manifest['identityAudit']['uniqueCiks']=500
manifest['identityAudit']['prctAbsent']=True
manifest['identityAudit']['envaPresent']=False
manifest['identityAudit']['collisionReplacementAudit']={'removedFromExpansion':list(REMOVE_TO_ADD),'addedToExpansion':list(REMOVE_TO_ADD.values()),'replacementCiks':{r['ticker']:int(r['cik']) for r in replacement_doc['records']}}
manifest['parserAudit']={'drawdownCompositeRegression':16.1,'drawdownPositiveCount':drawdown_positive,'drawdownSamples':drawdown_samples}
manifest['replacementEvidence']={'schema':replacement_doc['schema'],'generatedAt':replacement_doc['generatedAt'],'totalMetricCells':30,'totalHold':0}
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
print('US2000_CORRECTED_NEW500_CLEAN=PASS',json.dumps({'records':500,'cells':5000,'hold':0,'sha256':manifest['recordsSha256'],'removed':list(REMOVE_TO_ADD),'added':list(REMOVE_TO_ADD.values())},ensure_ascii=False))
