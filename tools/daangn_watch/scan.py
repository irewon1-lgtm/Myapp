from __future__ import annotations
import asyncio, json, re, sys, urllib.parse, urllib.request
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any
from playwright.async_api import BrowserContext, Page, async_playwright

BASE="https://www.daangn.com"
REGION_API=BASE+"/kr/api/v1/regions/keyword?keyword={query}"
REGIONS=["군포시","의왕시","안양시","과천시"]
KEYWORD="노트북"; LOOKBACK_HOURS=6; PAGE_TIMEOUT=20000
ROOT=Path(__file__).resolve().parent; OUT=ROOT/"output"; STATE=ROOT/"state.json"
UA="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
REMIX=re.compile(r"window\.__remixContext\s*=\s*(\{.*?\})\s*;",re.DOTALL)
LAPTOP=["노트북","그램","갤럭시북","맥북","macbook","thinkpad","ideapad","vivobook","zenbook","inspiron","latitude","vostro","elitebook","probook","surface laptop","yoga","aspire","swift","legion","victus","omen","tuf","loq","notebook"]
ACCESSORY=["파우치","가방","거치대","충전기","어댑터","키보드","마우스","케이스","액정만","부품용","도킹","허브","스탠드"]

def now(): return datetime.now(timezone.utc)
def iso(dt=None): return (dt or now()).astimezone(timezone.utc).isoformat(timespec="seconds")
def dtparse(v):
    if not v or not isinstance(v,str): return None
    try:
        d=datetime.fromisoformat(v.strip().replace("Z","+00:00"))
        if d.tzinfo is None: d=d.replace(tzinfo=timezone.utc)
        return d.astimezone(timezone.utc)
    except Exception: return None

def load_state():
    try:
        d=json.loads(STATE.read_text("utf-8")) if STATE.exists() else {}
        return {"version":1,"last_scan_at":d.get("last_scan_at"),"listings":d.get("listings",{})}
    except Exception as e:
        print("[WARN] state:",e,file=sys.stderr); return {"version":1,"last_scan_at":None,"listings":{}}

def save(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(json.dumps(obj,ensure_ascii=False,indent=2),"utf-8")

def regions(city):
    req=urllib.request.Request(REGION_API.format(query=urllib.parse.quote(city)),headers={"User-Agent":UA,"Accept":"application/json","Accept-Language":"ko-KR,ko;q=0.9"})
    with urllib.request.urlopen(req,timeout=15) as r: d=json.loads(r.read().decode())
    out=[]; seen=set()
    for x in d.get("locations",[]):
        if not isinstance(x,dict) or not x.get("id") or not x.get("name"): continue
        k=f"{x['name']}-{x['id']}"
        if k not in seen: seen.add(k); out.append({"id":x["id"],"name":x["name"]})
    return out

def remix_articles(html):
    m=REMIX.search(html)
    if not m: return []
    try:
        d=json.loads(m.group(1)); a=d.get("state",{}).get("loaderData",{}).get("routes/kr.buy-sell._index",{}).get("allPage",{}).get("fleamarketArticles",[])
        return [x for x in a if isinstance(x,dict)]
    except Exception: return []

def walk(o):
    if isinstance(o,dict):
        yield o
        for v in o.values(): yield from walk(v)
    elif isinstance(o,list):
        for v in o: yield from walk(v)

def detail_article(html):
    m=REMIX.search(html)
    if not m: return {}
    try: d=json.loads(m.group(1))
    except Exception: return {}
    best={}; score=-1
    for x in walk(d):
        k=set(x); s=sum(z in k for z in ("title","content","price","status","createdAt","href"))
        if s>score and ("content" in k or "title" in k): best=x; score=s
    return best

def absurl(h):
    if not h:return ""
    return h if h.startswith("http") else BASE+("/" if not h.startswith("/") else "")+h

def norm(raw,city,loc):
    ro=raw.get("region"); rn=ro.get("name","") if isinstance(ro,dict) else ""
    try: price=int(float(raw.get("price"))) if raw.get("price") not in (None,"") else None
    except Exception: price=None
    h=str(raw.get("href") or raw.get("url") or "")
    return {"id":str(raw.get("id") or raw.get("articleId") or h),"title":str(raw.get("title") or "").strip(),"price":price,"status":str(raw.get("status") or "").strip(),"created_at":str(raw.get("createdAt") or "").strip(),"boosted_at":str(raw.get("boostedAt") or "").strip(),"region":rn,"city":city,"search_location":loc,"url":absurl(h),"list_content":str(raw.get("content") or "").strip()}

def key(i): return str(i.get("id") or i.get("url") or "")

def is_laptop(title,text):
    s=(title+" "+text).lower()
    if not any(w in s for w in LAPTOP): return False
    if any(w in title.lower() for w in ACCESSORY) and not re.search(r"\b(?:i[3579]-?\d{4,5}|ryzen\s*[3579]|m[1-5])\b",s,re.I): return False
    return True

def specs(text):
    s=re.sub(r"\s+"," ",text.lower()); ram=set(); ssd=set(); hdd=set()
    for p in [r"(?:ram|램|메모리)\s*[:=\-]?\s*(16|32)\s*(?:g|gb|기가)?\b",r"\b(16|32)\s*(?:g|gb)\s*(?:ram|램|메모리)\b"]:
        ram|={int(m.group(1)) for m in re.finditer(p,s,re.I)}
    cap=r"(256|512|1024|1)\s*(tb|t|gb|g)?"
    def cv(m):
        n=int(m.group(1)); u=(m.group(2) or "gb").lower()
        return 1024 if u in ("tb","t") or (n==1 and u not in ("gb","g")) else n
    for m in re.finditer(rf"(?:ssd|nvme|m\.2)\s*[:=\-]?\s*{cap}",s,re.I): ssd.add(cv(m))
    for m in re.finditer(rf"{cap}\s*(?:ssd|nvme|m\.2)",s,re.I): ssd.add(cv(m))
    for m in re.finditer(rf"(?:hdd|하드)\s*[:=\-]?\s*{cap}",s,re.I): hdd.add(cv(m))
    for rv,sv in re.findall(r"\b(16|32)\s*(?:g|gb)?\s*[/|,+]\s*(512|1024|1\s*t(?:b)?)\s*(?:g|gb|tb)?\b",s,re.I):
        ram.add(int(rv)); z=re.sub(r"\s+","",sv.lower()); ssd.add(1024 if z.startswith("1t") else int(z))
    bad=256 in ssd and 1024 in hdd and not ({512,1024}&ssd)
    vr=sorted(ram&{16,32}); vs=sorted(ssd&{512,1024})
    cpu=""
    for p in [r"\b(i[3579]-?\d{4,5}[a-z]{0,2})\b",r"\b(ultra\s*[579]\s*\d{3}[a-z]{0,2})\b",r"\b(ryzen\s*[3579]\s*\d{4}[a-z]{0,3})\b",r"\b(m[1-5](?:\s*(?:pro|max|ultra))?)\b"]:
        m=re.search(p,s,re.I)
        if m: cpu=re.sub(r"\s+"," ",m.group(1)).strip(); break
    return {"ram_gb":vr[-1] if vr else None,"ssd_gb":vs[-1] if vs else None,"ram_values_seen":sorted(ram),"ssd_values_seen":sorted(ssd),"hdd_values_seen":sorted(hdd),"excluded_256ssd_plus_1tb_hdd":bad,"cpu_text":cpu,"meets_memory_storage":bool(vr and vs and not bad)}

@dataclass
class Stat:
    city:str; locations:int=0; list_items:int=0; unique_items:int=0; new_or_drop:int=0; detail_opened:int=0; spec_passed:int=0; errors:int=0

async def collect(page:Page,city,loc):
    url=f"{BASE}/kr/buy-sell/s/?in={urllib.parse.quote(str(loc['name']))}-{loc['id']}&only_on_sale=true&sort=recent&search={urllib.parse.quote(KEYWORD)}"
    api=[]
    async def resp(r):
        if "api/v1/fleamarket/search" not in r.url:return
        try:
            d=await r.json(); v=d.get("fleamarketArticles",[]) if isinstance(d,dict) else []
            if isinstance(v,list): api.extend(x for x in v if isinstance(x,dict))
        except Exception: pass
    page.on("response",resp)
    await page.goto(url,wait_until="domcontentloaded",timeout=PAGE_TIMEOUT); await page.wait_for_timeout(1200)
    await page.mouse.wheel(0,3200); await page.wait_for_timeout(900)
    raw=api or remix_articles(await page.content())
    return [norm(x,city,str(loc["name"])) for x in raw]

async def detail(ctx:BrowserContext,item):
    p=await ctx.new_page()
    try:
        await p.goto(item["url"],wait_until="domcontentloaded",timeout=PAGE_TIMEOUT); await p.wait_for_timeout(900)
        a=detail_article(await p.content()); c=str(a.get("content") or item.get("list_content") or "").strip()
        if not c:
            try:c=(await p.locator("body").inner_text(timeout=2500))[:12000]
            except Exception:c=""
        return c,a
    finally: await p.close()

async def run():
    OUT.mkdir(parents=True,exist_ok=True); st=load_state(); prev=st["listings"]; started=now(); cutoff=started-timedelta(hours=LOOKBACK_HOURS)
    diag={"scan_started_at":iso(started),"cutoff_at":iso(cutoff),"region_order":REGIONS,"cities":[],"notes":[]}; cand=[]; current={}
    async with async_playwright() as pw:
        browser=await pw.chromium.launch(headless=True,args=["--no-sandbox","--disable-dev-shm-usage"])
        ctx=await browser.new_context(user_agent=UA,locale="ko-KR",viewport={"width":1365,"height":900})
        for city in REGIONS:
            z=Stat(city); print(f"\n=== {city} 시작 ===")
            try: locs=await asyncio.to_thread(regions,city)
            except Exception as e: locs=[]; z.errors+=1; diag["notes"].append(f"{city} region API failed: {e}")
            z.locations=len(locs); found={}
            for loc in locs:
                p=await ctx.new_page()
                try:
                    xs=await collect(p,city,loc); z.list_items+=len(xs)
                    for i in xs:
                        k=key(i)
                        if k and k not in found: found[k]=i
                except Exception as e: z.errors+=1; print(f"[WARN] {city}/{loc.get('name')}: {e}")
                finally: await p.close()
                await asyncio.sleep(.25)
            z.unique_items=len(found)
            for k,i in found.items():
                current[k]=i
                if i.get("status") and i["status"]!="Ongoing": continue
                if not isinstance(i.get("price"),int) or i["price"]<=0: continue
                old=prev.get(k) if isinstance(prev,dict) else None; op=old.get("price") if isinstance(old,dict) else None; cr=dtparse(i.get("created_at"))
                new=old is None and cr is not None and cr>=cutoff; drop=isinstance(op,int) and i["price"]<op
                if not(new or drop): continue
                z.new_or_drop+=1; content,a=await detail(ctx,i); z.detail_opened+=1
                if not is_laptop(i["title"],content): continue
                sp=specs(i["title"]+"\n"+content)
                if not sp["meets_memory_storage"]: continue
                z.spec_passed+=1; cand.append({**i,"reason":"price_drop" if drop else "new_6h","previous_price":op,"price_drop_won":op-i["price"] if drop else None,"price_drop_pct":round((op-i["price"])/op*100,1) if drop and op else None,"detail_content":content[:12000],"specs":sp,"detail_observed":{x:a.get(x) for x in ("title","price","status","createdAt","boostedAt") if isinstance(a,dict)}})
            diag["cities"].append(z.__dict__); print(f"=== {city} 종료: 후보 {z.spec_passed}건 ===")
        await ctx.close(); await browser.close()
    listings=prev if isinstance(prev,dict) else {}
    for k,i in current.items(): listings[k]={"title":i["title"],"url":i["url"],"city":i["city"],"region":i["region"],"status":i["status"],"price":i["price"],"created_at":i["created_at"],"boosted_at":i["boosted_at"],"last_seen_at":iso()}
    limit=now()-timedelta(days=60); listings={k:v for k,v in listings.items() if not isinstance(v,dict) or dtparse(v.get("last_seen_at")) is None or dtparse(v.get("last_seen_at"))>=limit}
    save(STATE,{"version":1,"last_scan_at":iso(),"listings":listings})
    cand.sort(key=lambda x:(REGIONS.index(x["city"]) if x["city"] in REGIONS else 99,x.get("created_at") or ""))
    result={"scan_started_at":diag["scan_started_at"],"scan_finished_at":iso(),"lookback_hours":LOOKBACK_HOURS,"candidate_count":len(cand),"candidates":cand}
    save(OUT/"latest_candidates.json",result); save(OUT/"latest_diagnostics.json",diag)
    md=["# 당근 노트북 감시 최신 실행","",f"- 실행: {result['scan_finished_at']}",f"- 순서: {' → '.join(REGIONS)}",f"- 최근 신규 기준: {LOOKBACK_HOURS}시간",f"- 상세 검증 후보: {len(cand)}건",""]
    if not cand: md.append("조건(RAM 16/32GB + SSD/NVMe 512GB/1TB + 판매중 + 최근 신규/가격인하)을 통과한 원시 후보 없음.")
    for c in cand:
        sp=c["specs"]; md += [f"## {c['city']} · {c['title']}",f"- 사유: {c['reason']}",f"- 가격: {c['price']:,}원",f"- RAM/SSD: {sp['ram_gb']}GB / {sp['ssd_gb']}GB",f"- CPU 표기: {sp['cpu_text'] or '미확인'}",f"- 등록시각: {c['created_at'] or '미확인'}",f"- 끌올시각: {c['boosted_at'] or '없음/미확인'}",f"- 링크: {c['url']}",""]
    (OUT/"latest_scan.md").write_text("\n".join(md)+"\n","utf-8"); print(f"DONE candidates={len(cand)}")

if __name__=="__main__": asyncio.run(run())
