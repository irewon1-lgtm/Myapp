#!/usr/bin/env python3
from __future__ import annotations
import gzip, importlib.util, json, re, time
from datetime import datetime, timezone
from pathlib import Path

HERE=Path(__file__).resolve().parent
SPEC=importlib.util.spec_from_file_location('us2000_collect_base',HERE/'collect_500.py')
if SPEC is None or SPEC.loader is None: raise RuntimeError('US2000_BASE_COLLECTOR_IMPORT_FAILED')
base=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(base)
TARGETS=['ICFI','OMF','EE']
EXPECTED_CIK={'ICFI':1362004,'OMF':1584207,'EE':1888447}

def parse_drawdown_raw(raw):
    matches=re.findall(r'([-+−]?\d+(?:\.\d+)?)\s*%',str(raw or ''))
    if not matches:return None
    pct=float(matches[-1].replace('−','-'))
    return abs(pct) if pct<0 else 0.0
_original=base.parse_finviz
def fixed_parse_finviz(page):
    out=_original(page); row=out.get('Drawdown_52W_Pct',{}); fixed=parse_drawdown_raw(row.get('raw'))
    if fixed is not None: row['value']=fixed; out['Drawdown_52W_Pct']=row
    return out
base.parse_finviz=fixed_parse_finviz
assert parse_drawdown_raw('25.97 -16.10%')==16.10

mapping_source=base.SEC
try:
    sec=json.loads(base.fetch(base.SEC,timeout=30,attempts=2,json_mode=True))
except Exception as e:
    print('SEC_DIRECT_UNAVAILABLE_USING_PINNED_MIRROR',str(e),flush=True)
    sec=json.loads(gzip.decompress(base.fetch_bytes(base.SEC_MIRROR,timeout=30,attempts=3)).decode('utf-8'))
    mapping_source=base.SEC_MIRROR
fields=sec.get('fields') or []
required={'name','cik','ticker','exchange'}
if set(fields)!=required: raise RuntimeError('SEC_MAPPING_SCHEMA:'+json.dumps(fields))
ix={name:fields.index(name) for name in required}
by={}
for row in sec.get('data',[]):
    key=base.norm(row[ix['ticker']])
    by[key]={'company':row[ix['name']],'cik':int(row[ix['cik']]),'exchange':row[ix['exchange']]}

identities=[]
for i,t in enumerate(TARGETS):
    if base.norm(t) not in by: raise RuntimeError('SEC_TICKER_MISSING:'+t)
    r=by[base.norm(t)]
    if r['cik']!=EXPECTED_CIK[t]: raise RuntimeError(f'SEC_CIK_MISMATCH:{t}:{r["cik"]}')
    if r['exchange'] not in {'NYSE','Nasdaq'}: raise RuntimeError(f'SEC_EXCHANGE_INVALID:{t}:{r["exchange"]}')
    identities.append({'ticker':t,'company':r['company'],'cik':r['cik'],'exchange':r['exchange'],'ordinal':0,'selectionLayer':'US2000_COLLISION_REPLACEMENT','identitySource':mapping_source})

asof=datetime.now(timezone.utc).date().isoformat()
records=[]
for ident in identities:
    best=None
    for attempt in range(1,4):
        row=base.collect_one(ident,asof)
        holds=[k for k,v in row['metrics'].items() if v.get('status')=='HOLD']
        print('COLLECT',ident['ticker'],'attempt',attempt,'holds',holds,'errors',row.get('collectionErrors',[]),flush=True)
        best=row
        if not holds: break
        if attempt<3: time.sleep(12)
    holds=[k for k,v in best['metrics'].items() if v.get('status')=='HOLD']
    if holds: raise RuntimeError('REPLACEMENT_HOLD:'+ident['ticker']+':'+','.join(holds))
    records.append(best)

if len(records)!=3 or len({r['ticker'] for r in records})!=3 or len({int(r['cik']) for r in records})!=3: raise RuntimeError('REPLACEMENT_IDENTITY_CARDINALITY')
if any(set(r['metrics'])!=set(base.KEYS) for r in records): raise RuntimeError('REPLACEMENT_METRIC_KEYS')
if sum(v.get('status')=='HOLD' for r in records for v in r['metrics'].values()): raise RuntimeError('REPLACEMENT_HOLD_REMAINS')

out={'schema':'US2000_COLLISION_REPLACEMENTS_1','generatedAt':datetime.now(timezone.utc).isoformat().replace('+00:00','Z'),'asOf':asof,'replaces':['ENVA','TRUP','YELP'],'records':records,'metricCount':10,'totalMetricCells':30,'totalHold':0,'identitySource':mapping_source}
path=HERE/'replacements.json'; path.write_text(json.dumps(out,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print('US2000_REPLACEMENTS_CLEAN=PASS',json.dumps({'tickers':[r['ticker'] for r in records],'ciks':[r['cik'] for r in records],'cells':30,'hold':0},ensure_ascii=False),flush=True)
