#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import importlib.util
import json
import re
from datetime import datetime, timezone
from pathlib import Path

HERE=Path(__file__).resolve().parent
OUT=HERE/'output'
SPEC=importlib.util.spec_from_file_location('us2000_collect_base',HERE/'collect_500.py')
if SPEC is None or SPEC.loader is None:raise RuntimeError('US2000_BASE_COLLECTOR_IMPORT_FAILED')
base=importlib.util.module_from_spec(SPEC);SPEC.loader.exec_module(base)

def parse_drawdown_raw(raw):
    matches=re.findall(r'([-+−]?\d+(?:\.\d+)?)\s*%',str(raw or ''))
    if not matches:return None
    pct=float(matches[-1].replace('−','-'))
    return abs(pct) if pct<0 else 0.0

_original=base.parse_finviz
def fixed_parse_finviz(page):
    out=_original(page);row=out.get('Drawdown_52W_Pct',{});fixed=parse_drawdown_raw(row.get('raw'))
    if fixed is not None:row['value']=fixed;out['Drawdown_52W_Pct']=row
    return out
base.parse_finviz=fixed_parse_finviz

manifest_path=OUT/'manifest.json'
manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
records=[];chunk_ranges=[]
for chunk in manifest['chunks']:
    part=json.loads((OUT/chunk['file']).read_text(encoding='utf-8'))
    start=len(records);records.extend(part);chunk_ranges.append((chunk,start,len(records)))

idx=next((i for i,r in enumerate(records) if r['ticker']=='WTI'),None)
if idx is None:raise RuntimeError('WTI_NOT_FOUND')
old=records[idx]
identity={k:v for k,v in old.items() if k not in {'metrics','collectionErrors'}}
recovered=base.collect_one(identity,manifest['asOf'])
finviz_keys=['Revenue_TTM_YoY_Pct','Gross_Margin_TTM_Pct','Operating_Margin_TTM_Pct','Net_Margin_TTM_Pct','ROA_TTM_Pct','TTM_PER','Price_Sales_TTM','Drawdown_52W_Pct']
still=[k for k in finviz_keys if recovered['metrics'][k]['status']=='HOLD']
if still:raise RuntimeError('WTI_RECOVERY_STILL_HOLD:'+','.join(still)+':'+json.dumps(recovered.get('collectionErrors',[])))
records[idx]=recovered

summary={k:{'numeric':0,'naBasis':0,'hold':0} for k in base.KEYS}
hold_details=[];drawdown_positive=0;drawdown_samples=[]
for r in records:
    for k in base.KEYS:
        cell=r['metrics'][k];s=cell['status']
        if s=='NUMERIC':summary[k]['numeric']+=1
        elif s=='NA_BASIS':summary[k]['naBasis']+=1
        elif s=='HOLD':
            summary[k]['hold']+=1
            hold_details.append({'ticker':r['ticker'],'metric':k,'reason':cell.get('reason'),'collectionErrors':r.get('collectionErrors',[])})
        else:raise RuntimeError('BAD_STATUS:'+r['ticker']+':'+k+':'+str(s))
    dd=r['metrics']['Drawdown_52W_Pct']
    if dd.get('status')=='NUMERIC' and isinstance(dd.get('value'),(int,float)) and dd['value']>0:
        drawdown_positive+=1
        if len(drawdown_samples)<5:drawdown_samples.append({'ticker':r['ticker'],'value':dd['value'],'raw':dd.get('raw')})

for chunk,start,end in chunk_ranges:
    raw=json.dumps(records[start:end],separators=(',',':'),ensure_ascii=False).encode()
    (OUT/chunk['file']).write_bytes(raw);chunk['count']=end-start;chunk['sha256']=hashlib.sha256(raw).hexdigest()
canonical=json.dumps(records,separators=(',',':'),ensure_ascii=False).encode()
total_resolved=sum(v['numeric']+v['naBasis'] for v in summary.values())
manifest['generatedAt']=datetime.now(timezone.utc).isoformat().replace('+00:00','Z')
manifest['summary']=summary
manifest['totalResolved']=total_resolved
manifest['totalHold']=5000-total_resolved
manifest['resolvedRatio']=round(total_resolved/5000,6)
manifest['recordsSha256']=hashlib.sha256(canonical).hexdigest()
manifest['holdDetails']=hold_details
manifest['parserAudit']={'drawdownCompositeRegression':parse_drawdown_raw('25.97 -16.10%'),'drawdownPositiveCount':drawdown_positive,'drawdownSamples':drawdown_samples}
manifest['targetedRecovery']={'ticker':'WTI','at':manifest['generatedAt'],'previousHoldCount':sum(1 for k in finviz_keys if old['metrics'][k]['status']=='HOLD'),'remainingHoldCount':len(hold_details),'collectionErrors':recovered.get('collectionErrors',[])}
manifest_path.write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
print('WTI_TARGETED_RECOVERY_COMPLETE',json.dumps(manifest['targetedRecovery'],ensure_ascii=False))
print('US2000_NEW500_FINAL',json.dumps({'resolved':total_resolved,'holds':manifest['totalHold'],'ratio':manifest['resolvedRatio'],'drawdownPositiveCount':drawdown_positive},ensure_ascii=False))
