#!/usr/bin/env python3
from __future__ import annotations
import concurrent.futures as cf
import gzip, hashlib, json, random, re, time, urllib.error, urllib.parse, urllib.request
from collections import Counter
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "output"
TICKERS = [x.strip().upper() for x in (ROOT / "additions.txt").read_text(encoding="utf-8").splitlines() if x.strip()]
KEYS = [
    "Revenue_TTM_YoY_Pct","Gross_Margin_TTM_Pct","Operating_Margin_TTM_Pct",
    "Net_Margin_TTM_Pct","ROA_TTM_Pct","FCF_Yield_TTM_Pct","TTM_PER",
    "Price_Sales_TTM","Shares_Change_YoY_Pct","Drawdown_52W_Pct"
]
UA = "irewon1-lgtm US2000 public-data audit https://github.com/irewon1-lgtm/Myapp"
SEC = "https://www.sec.gov/files/company_tickers_exchange.json"
SEC_MIRROR = "https://raw.githubusercontent.com/supermodo/us-markets-timemachine/main/data/edgar/company_tickers_exchange/2026/2026-09-16.gz"
FINVIZ = "https://finviz.com/quote.ashx?t={ticker}&p=d"
STOCKANALYSIS = "https://stockanalysis.com/stocks/{slug}/statistics/"
MISSING = {"-","—","N/A","n/a","NA",""}

class PublicSourceError(RuntimeError):
    pass

class Cells(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.in_cell = 0
        self.buf = []
        self.cells = []
        self.text = []
    def handle_starttag(self, tag, attrs):
        if tag.lower() in {"td","th"}:
            self.in_cell += 1
            self.buf = []
    def handle_endtag(self, tag):
        if tag.lower() in {"td","th"} and self.in_cell:
            t = " ".join(" ".join(self.buf).split())
            if t:
                self.cells.append(t)
            self.in_cell -= 1
            self.buf = []
    def handle_data(self, data):
        t = " ".join(data.split())
        if t:
            self.text.append(t)
        if self.in_cell and t:
            self.buf.append(t)

def norm(t):
    return str(t or "").strip().upper().replace(".", "-").replace("/", "-")

def fetch(url, *, timeout=25, attempts=2, json_mode=False):
    last = None
    for n in range(attempts):
        time.sleep(random.uniform(0.12, 0.28))
        req = urllib.request.Request(url, headers={
            "User-Agent": UA,
            "Accept": "application/json" if json_mode else "text/html,application/xhtml+xml",
            "Accept-Language": "en-US,en;q=0.8",
            "Cache-Control": "no-cache",
        })
        try:
            with urllib.request.urlopen(req, timeout=timeout) as r:
                body = r.read(5_000_001)
                if len(body) > 5_000_000:
                    raise PublicSourceError("BODY_TOO_LARGE")
                return body.decode("utf-8", "replace")
        except urllib.error.HTTPError as e:
            last = e
            if e.code == 429 and n + 1 < attempts:
                try:
                    delay = min(20.0, max(1.0, float(e.headers.get("Retry-After") or 3)))
                except Exception:
                    delay = 3.0
                time.sleep(delay)
                continue
            raise PublicSourceError(f"HTTP_{e.code}")
        except (TimeoutError, urllib.error.URLError) as e:
            last = e
            if n + 1 < attempts:
                time.sleep(1.0 + n)
                continue
            raise PublicSourceError(type(e).__name__)
    raise PublicSourceError(type(last).__name__ if last else "FETCH_FAILED")

def fetch_bytes(url, *, timeout=30, attempts=2):
    last = None
    for n in range(attempts):
        req = urllib.request.Request(url, headers={"User-Agent":UA,"Accept":"application/octet-stream"})
        try:
            with urllib.request.urlopen(req, timeout=timeout) as r:
                return r.read(5_000_001)
        except (urllib.error.HTTPError, TimeoutError, urllib.error.URLError) as e:
            last = e
            if n + 1 < attempts:
                time.sleep(1.0 + n)
                continue
            raise PublicSourceError(f"MIRROR_FETCH:{type(e).__name__}:{getattr(e,'code','')}")
    raise PublicSourceError(type(last).__name__ if last else "MIRROR_FETCH_FAILED")

def number(s):
    if s is None:
        return None
    s = str(s).strip().replace(",", "").replace("−", "-")
    if s in MISSING:
        return None
    s = re.sub(r"[$€£]", "", s).replace("%", "").replace("x", "").strip()
    m = re.search(r"[-+]?\d+(?:\.\d+)?", s)
    return float(m.group()) if m else None

def tokens(page):
    p = Cells()
    p.feed(page)
    return p.cells, p.text

def value_after(items, label, max_gap=5):
    wanted = label.lower()
    for i, t in enumerate(items):
        if t.strip().lower() == wanted:
            for j in range(i+1, min(len(items), i+1+max_gap)):
                if number(items[j]) is not None or items[j] in MISSING:
                    return items[j]
    return None

def parse_finviz(page):
    cells, text = tokens(page)
    items = cells or text
    labels = {
        "Revenue_TTM_YoY_Pct":"Sales Y/Y TTM",
        "Gross_Margin_TTM_Pct":"Gross Margin",
        "Operating_Margin_TTM_Pct":"Oper. Margin",
        "Net_Margin_TTM_Pct":"Profit Margin",
        "ROA_TTM_Pct":"ROA",
        "TTM_PER":"P/E",
        "Price_Sales_TTM":"P/S",
        "Drawdown_52W_Pct":"52W High",
    }
    out = {}
    for key, label in labels.items():
        raw = value_after(items, label)
        v = number(raw)
        if key == "Drawdown_52W_Pct" and v is not None:
            v = abs(min(v, 0.0)) if v <= 0 else 0.0
        out[key] = {"raw":raw, "value":v}
    epsraw = value_after(items, "EPS (ttm)") or value_after(items, "EPS TTM")
    out["_eps_ttm"] = {"raw":epsraw, "value":number(epsraw)}
    if not any(v["raw"] is not None for k,v in out.items() if not k.startswith("_")):
        raise PublicSourceError("FINVIZ_FIELDS_NOT_FOUND")
    return out

def parse_stockanalysis(page):
    cells, text = tokens(page)
    items = cells or text
    flat = " | ".join(text)
    out = {}
    for key, labels in {
        "FCF_Yield_TTM_Pct":["FCF Yield","Free Cash Flow Yield"],
        "Shares_Change_YoY_Pct":["Shares Change (YoY)","Shares Change YoY"],
    }.items():
        raw = None
        for label in labels:
            raw = value_after(items, label)
            if raw is not None:
                break
        if raw is None:
            for label in labels:
                m = re.search(re.escape(label)+r".{0,160}?([-+−]?\d+(?:\.\d+)?\s*%|N/A|n/a|—|-)", flat, re.I)
                if m:
                    raw = m.group(1)
                    break
        out[key] = {"raw":raw, "value":number(raw)}
    if not any(v["raw"] is not None for v in out.values()):
        raise PublicSourceError("STOCKANALYSIS_FIELDS_NOT_FOUND")
    return out

def numeric_cell(value, raw, source, asof):
    return {"status":"NUMERIC","value":float(value),"raw":raw,"asOf":asof,
            "reason":"Public page direct metric","sourceUrls":[source],"sourceType":"PUBLIC_PAGE_DIRECT"}

def na_cell(display, reason, source, asof):
    return {"status":"NA_BASIS","value":None,"displayValue":display,"asOf":asof,
            "reason":reason,"sourceUrls":[source] if source else [],"sourceType":"PUBLIC_PAGE_DIRECT"}

def hold_cell(reason, asof):
    return {"status":"HOLD","value":None,"asOf":asof,"reason":reason,
            "sourceUrls":[],"sourceType":"PUBLIC_PAGE_DIRECT"}

def stockanalysis_slugs(ticker):
    raw = ticker.lower().replace("/", "-")
    out = [raw]
    if re.fullmatch(r"[a-z0-9]+-[a-z]", raw):
        out.append(raw.rsplit("-",1)[0]+"."+raw.rsplit("-",1)[1])
    return list(dict.fromkeys(out))

def collect_one(identity, asof):
    ticker = identity["ticker"]
    metrics = {}
    errors = []
    fu = FINVIZ.format(ticker=urllib.parse.quote(ticker, safe=".-"))
    try:
        f = parse_finviz(fetch(fu))
        for key in ["Revenue_TTM_YoY_Pct","Gross_Margin_TTM_Pct","Operating_Margin_TTM_Pct",
                    "Net_Margin_TTM_Pct","ROA_TTM_Pct","Price_Sales_TTM","Drawdown_52W_Pct"]:
            row = f.get(key,{})
            if row.get("value") is not None:
                metrics[key] = numeric_cell(row["value"], row.get("raw"), fu, asof)
            elif row.get("raw") in MISSING:
                metrics[key] = na_cell("N/A","Public page reports N/A",fu,asof)
            else:
                metrics[key] = hold_cell("FINVIZ_VALUE_UNAVAILABLE",asof)
        perow = f.get("TTM_PER",{})
        pe = perow.get("value")
        eps = f.get("_eps_ttm",{}).get("value")
        if pe is not None and pe > 0:
            metrics["TTM_PER"] = numeric_cell(pe, perow.get("raw"), fu, asof)
        elif eps is not None and eps <= 0:
            metrics["TTM_PER"] = na_cell("적자","TTM EPS is not positive; P/E not applicable",fu,asof)
        elif perow.get("raw") in MISSING:
            metrics["TTM_PER"] = na_cell("N/A","Public page reports P/E N/A",fu,asof)
        else:
            metrics["TTM_PER"] = hold_cell("FINVIZ_PER_UNAVAILABLE",asof)
    except Exception as e:
        errors.append("FINVIZ:"+str(e))
    sa_ok = False
    sa_last = None
    for slug in stockanalysis_slugs(ticker):
        su = STOCKANALYSIS.format(slug=urllib.parse.quote(slug, safe=".-"))
        try:
            s = parse_stockanalysis(fetch(su))
            for key in ["FCF_Yield_TTM_Pct","Shares_Change_YoY_Pct"]:
                row = s.get(key,{})
                if row.get("value") is not None:
                    metrics[key] = numeric_cell(row["value"], row.get("raw"), su, asof)
                elif row.get("raw") in MISSING:
                    metrics[key] = na_cell("N/A","Public page reports N/A",su,asof)
                else:
                    metrics[key] = hold_cell("STOCKANALYSIS_VALUE_UNAVAILABLE",asof)
            sa_ok = True
            break
        except Exception as e:
            sa_last = e
            if str(e) != "HTTP_404":
                break
    if not sa_ok:
        errors.append("STOCKANALYSIS:"+str(sa_last or "UNAVAILABLE"))
    for key in KEYS:
        if key not in metrics:
            metrics[key] = hold_cell("; ".join(errors)[:300] or "PUBLIC_SOURCE_UNAVAILABLE",asof)
    return {**identity,"metrics":metrics,"collectionErrors":errors}

def main():
    if len(TICKERS) != 500 or len(set(TICKERS)) != 500:
        raise SystemExit(f"US2000_TICKER_CARDINALITY:{len(TICKERS)}:{len(set(TICKERS))}")
    if "PRCT" in TICKERS or "ENVA" not in TICKERS:
        raise SystemExit("US2000_REPLACEMENT_CONTRACT")
    mapping_source = SEC
    try:
        sec = json.loads(fetch(SEC, timeout=30, attempts=2, json_mode=True))
    except PublicSourceError as e:
        print("SEC_DIRECT_UNAVAILABLE_USING_PINNED_MIRROR", str(e), flush=True)
        raw = gzip.decompress(fetch_bytes(SEC_MIRROR, timeout=30, attempts=3))
        sec = json.loads(raw.decode("utf-8"))
        mapping_source = SEC_MIRROR
    fields = sec.get("fields") or []
    required = {"name","cik","ticker","exchange"}
    if set(fields) != required:
        raise SystemExit("SEC_MAPPING_SCHEMA:"+json.dumps(fields))
    ix = {name:fields.index(name) for name in required}
    by_ticker = {}
    for row in sec.get("data",[]):
        name, cik, ticker, exchange = row[ix["name"]], row[ix["cik"]], row[ix["ticker"]], row[ix["exchange"]]
        key = norm(ticker)
        if key in by_ticker and int(by_ticker[key]["cik"]) != int(cik):
            raise SystemExit("SEC_DUPLICATE_TICKER:"+key)
        by_ticker[key] = {"company":name,"cik":int(cik),"exchange":exchange}
    missing = [t for t in TICKERS if norm(t) not in by_ticker]
    if missing:
        raise SystemExit("SEC_TICKER_MISSING:"+",".join(missing))
    identities = []
    ciks = set()
    for i,t in enumerate(TICKERS):
        row = by_ticker[norm(t)]
        cik = int(row["cik"])
        if not cik or cik in ciks:
            raise SystemExit("US2000_NEW500_CIK_COLLISION:"+t)
        if not row.get("exchange"):
            raise SystemExit("US2000_EXCHANGE_MISSING:"+t)
        ciks.add(cik)
        identities.append({
            "ticker":t,"company":row["company"],"cik":cik,"exchange":row["exchange"],
            "ordinal":1501+i,"selectionLayer":"US2000_FREE_PUBLIC_RUNNER",
            "identitySource":mapping_source,
        })
    asof = datetime.now(timezone.utc).date().isoformat()
    records = [None]*500
    failures = []
    with cf.ThreadPoolExecutor(max_workers=4) as ex:
        futs = {ex.submit(collect_one, ident, asof):i for i,ident in enumerate(identities)}
        for n,fut in enumerate(cf.as_completed(futs),1):
            i = futs[fut]
            try:
                records[i] = fut.result()
            except Exception as e:
                failures.append(f"{identities[i]['ticker']}:{type(e).__name__}:{e}")
            if n % 50 == 0:
                print(f"PROGRESS {n}/500", flush=True)
    if failures or any(r is None for r in records):
        raise SystemExit("COLLECTION_FATAL:"+";".join(failures[:20]))
    summary = {k:{"numeric":0,"naBasis":0,"hold":0} for k in KEYS}
    for r in records:
        if set(r["metrics"]) != set(KEYS):
            raise SystemExit("METRIC_KEY_CONTRACT:"+r["ticker"])
        for k in KEYS:
            s = r["metrics"][k]["status"]
            if s == "NUMERIC": summary[k]["numeric"] += 1
            elif s == "NA_BASIS": summary[k]["naBasis"] += 1
            elif s == "HOLD": summary[k]["hold"] += 1
            else: raise SystemExit("METRIC_STATUS:"+r["ticker"]+":"+k+":"+str(s))
    total_resolved = sum(v["numeric"]+v["naBasis"] for v in summary.values())
    canonical = json.dumps(records, separators=(",",":"), ensure_ascii=False).encode()
    records_hash = hashlib.sha256(canonical).hexdigest()
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("chunk-*.json"):
        old.unlink()
    chunks = []
    for n,start in enumerate(range(0,500,50)):
        part = records[start:start+50]
        raw = json.dumps(part, separators=(",",":"), ensure_ascii=False).encode()
        name = f"chunk-{n:02d}.json"
        (OUT/name).write_bytes(raw)
        chunks.append({"file":name,"count":len(part),"sha256":hashlib.sha256(raw).hexdigest()})
    manifest = {
        "schema":"US2000_FREE_NEW500_1",
        "generatedAt":datetime.now(timezone.utc).isoformat().replace("+00:00","Z"),
        "asOf":asof,
        "recordCount":500,
        "metricCount":10,
        "totalMetricCells":5000,
        "identityAudit":{
            "secSource":SEC,"mappingSource":mapping_source,"mirrorSnapshotDate":"2026-09-16" if mapping_source==SEC_MIRROR else None,"secMapped":500,"uniqueTickers":len(set(TICKERS)),
            "uniqueCiks":len(ciks),"exchangeCounts":dict(sorted(Counter(x["exchange"] for x in identities).items())),
            "prctAbsent":True,"envaPresent":True,
        },
        "summary":summary,
        "totalResolved":total_resolved,
        "totalHold":5000-total_resolved,
        "resolvedRatio":round(total_resolved/5000,6),
        "recordsSha256":records_hash,
        "chunks":chunks,
        "sourcePolicy":{
            "identity":"SEC company_tickers_exchange.json",
            "fundamentals":["Finviz public quote page","StockAnalysis public statistics page"],
            "login":False,"apiKey":False,"proxy":False,"captchaBypass":False,"workers":4,
        }
    }
    (OUT/"manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    print("US2000_FREE_COLLECTION_COMPLETE", json.dumps({
        "recordCount":500,"metricCells":5000,"resolved":total_resolved,
        "holds":5000-total_resolved,"resolvedRatio":manifest["resolvedRatio"],
        "exchanges":manifest["identityAudit"]["exchangeCounts"]
    }, ensure_ascii=False), flush=True)

if __name__ == "__main__":
    main()
