#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
import re

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "app/build/test-results/testDebugUnitTest"
EVIDENCE = ROOT / "textbook_evidence"
EVIDENCE.mkdir(exist_ok=True)


def collect(class_fragment: str):
    total = failures = errors = skipped = 0
    files = []
    for path in RESULTS.glob("TEST-*.xml"):
        root = ET.parse(path).getroot()
        if class_fragment not in root.attrib.get("name", "") and class_fragment not in path.name:
            continue
        files.append(path.name)
        total += int(root.attrib.get("tests", "0"))
        failures += int(root.attrib.get("failures", "0"))
        errors += int(root.attrib.get("errors", "0"))
        skipped += int(root.attrib.get("skipped", "0"))
    return total, failures, errors, skipped, files


extreme = collect("TextbookExtreme60Test")
clean = collect("TextbookClean25Test")
parser = collect("TextbookMarkdownParserTest")
catalog = collect("V1TextbookCatalogTest")
checks = []

def ck(name, ok, detail):
    checks.append((name, bool(ok), detail))

ck("Extreme60 exact count", extreme[0] == 60 and extreme[1:4] == (0,0,0), str(extreme[:4]))
ck("CLEAN25 exact count", clean[0] == 25 and clean[1:4] == (0,0,0), str(clean[:4]))
ck("Markdown parser tests", parser[0] >= 3 and parser[1:4] == (0,0,0), str(parser[:4]))
ck("Catalog tests", catalog[0] >= 4 and catalog[1:4] == (0,0,0), str(catalog[:4]))

asset_dir = ROOT / "app/src/main/assets/textbook/v1"
assets = sorted(asset_dir.glob("chapter_*.md"))
ck("11 textbook assets", len(assets) == 11, f"count={len(assets)}")
lengths = []
for p in assets:
    text = p.read_text(encoding="utf-8")
    lengths.append(len(text))
    for marker in ("Guided Lab", "Independent Lab", "Debug Challenge", "AI Audit", "회상 문제", "전이 문제", "Chapter 완료 증거"):
        ck(f"{p.name}: {marker}", marker in text, marker)
    ck(f"{p.name}: minimum depth", len(text) >= 8000, f"chars={len(text)}")
    ck(f"{p.name}: no placeholder", not re.search(r"\b(TODO|TBD|LOREM)\b|준비중|나중에 작성", text, re.I), "placeholder scan")

all_text = "\n".join(p.read_text(encoding="utf-8") for p in assets)
ck("Total textbook depth", len(all_text) >= 90000, f"chars={len(all_text)}")

catalog_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/V1TextbookCatalog.kt"
screen_path = ROOT / "app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt"
main_path = ROOT / "app/src/main/java/com/futuretech/poweruser/MainActivity.kt"
practice_path = ROOT / "app/src/main/java/com/futuretech/poweruser/data/V1TextbookPracticeData.kt"
for p in (catalog_path, screen_path, main_path, practice_path):
    ck(f"source exists: {p.name}", p.exists(), str(p))

if screen_path.exists():
    s = screen_path.read_text(encoding="utf-8")
    for marker in ("textbook_layout_expanded", "textbook_layout_medium", "textbook_layout_compact", "textbook_toc", "textbook_reader", "textbook_insight_rail", "textbook_practice_button"):
        ck(f"UI marker {marker}", marker in s, marker)
    model_text = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookModels.kt").read_text(encoding="utf-8")
    ck("Expanded tablet threshold", "widthDp >= 1080" in model_text, "1080dp")

if main_path.exists():
    s = main_path.read_text(encoding="utf-8")
    ck("Home textbook entry", "textbook_v1_entry" in s, "testTag")
    ck("Textbook route", 'composable("textbook_v1")' in s, "route")
    ck("Practice route", 'textbook_v1/practice/{lessonId}' in s, "route")

failed = [x for x in checks if not x[1]]
report = [
    "# V1 Textbook Extreme60 / CLEAN25 Report", "",
    f"- Extreme60: **{extreme[0]}/60**, failures={extreme[1]}, errors={extreme[2]}, skipped={extreme[3]}",
    f"- CLEAN25: **{clean[0]}/25**, failures={clean[1]}, errors={clean[2]}, skipped={clean[3]}",
    f"- Parser broad: **{parser[0]}** tests",
    f"- Catalog broad: **{catalog[0]}** tests",
    f"- Assets: **{len(assets)}** chapters / total chars **{len(all_text):,}**",
    f"- Chapter char range: **{min(lengths) if lengths else 0:,}–{max(lengths) if lengths else 0:,}**",
    f"- Static gates: **{len(checks)-len(failed)}/{len(checks)} PASS**", "", "## Static gate details",
]
for name, ok, detail in checks:
    report.append(f"- {'PASS' if ok else 'FAIL'} — {name}: {detail}")
report += ["", "## Final", f"**{'PASS' if not failed else 'FAIL'}**"]
out = EVIDENCE / "TEXTBOOK_V1_EXTREME60_CLEAN25_REPORT.md"
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print(out.read_text(encoding="utf-8"))
if failed:
    raise SystemExit(1)
