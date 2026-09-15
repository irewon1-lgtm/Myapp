#!/usr/bin/env python3
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "app/build/test-results/testDebugUnitTest"
EVIDENCE = ROOT / "textbook_evidence"
EVIDENCE.mkdir(exist_ok=True)


def collect(class_fragment: str):
    total = failures = errors = skipped = 0
    for path in RESULTS.glob("TEST-*.xml"):
        root = ET.parse(path).getroot()
        if class_fragment not in root.attrib.get("name", "") and class_fragment not in path.name:
            continue
        total += int(root.attrib.get("tests", "0"))
        failures += int(root.attrib.get("failures", "0"))
        errors += int(root.attrib.get("errors", "0"))
        skipped += int(root.attrib.get("skipped", "0"))
    return total, failures, errors, skipped


checks = []

def ck(name, ok, detail):
    checks.append((name, bool(ok), detail))


workbook_tests = collect("V1WorkbookQualityTest")
ck(
    "Workbook JVM integration tests",
    workbook_tests[0] >= 3 and workbook_tests[1:] == (0, 0, 0),
    str(workbook_tests),
)

asset_dir = ROOT / "app/src/main/assets/textbook/v1"
workbooks = sorted(asset_dir.glob("workbook_*.md"))
chapters = sorted(asset_dir.glob("chapter_*.md"))
ck("Exactly 11 workbook assets", len(workbooks) == 11, f"count={len(workbooks)}")
ck("Exactly 11 chapter assets", len(chapters) == 11, f"count={len(chapters)}")

workbook_chars = 0
training_counts = []
for index, path in enumerate(workbooks, start=1):
    text = path.read_text(encoding="utf-8")
    workbook_chars += len(text)
    training_count = sum(
        1 for line in text.splitlines()
        if line.startswith("## 훈련") or line.startswith("## 프로젝트") or line.startswith("# 프로젝트")
    )
    training_counts.append(training_count)

    ck(f"{path.name}: truncation floor", len(text) >= 5_500, f"chars={len(text)}")
    ck(f"{path.name}: training density", training_count >= 8, f"tasks={training_count}")
    ck(f"{path.name}: code/data examples", "```" in text, "code fence")
    ck(
        f"{path.name}: direct learner action",
        any(word in text for word in ("직접", "수정", "판정", "실행", "디버깅", "프로젝트")),
        "action verbs",
    )
    ck(
        f"{path.name}: no placeholder",
        not re.search(r"\b(TODO|TBD|LOREM)\b|준비중|나중에 작성", text, re.I),
        "placeholder scan",
    )

    paragraphs = [
        re.sub(r"\s+", " ", p).strip()
        for p in re.split(r"\n\s*\n", text)
        if len(re.sub(r"\s+", " ", p).strip()) >= 100
    ]
    duplicate_paragraphs = {
        p for p in paragraphs if paragraphs.count(p) > 1
    }
    ck(
        f"{path.name}: no exact repeated long paragraph",
        not duplicate_paragraphs,
        f"duplicates={len(duplicate_paragraphs)}",
    )

# Size is only a truncation guard, never a page-count target.
ck("Workbook total truncation floor", workbook_chars >= 75_000, f"chars={workbook_chars:,}")
ck("Every workbook has substantial task count", min(training_counts or [0]) >= 8, f"min={min(training_counts or [0])}")

screen_path = ROOT / "app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt"
if screen_path.is_file():
    source = screen_path.read_text(encoding="utf-8")
    ck("Reader derives matching workbook path", "workbook_${selected.number.toString().padStart(2, '0')}.md" in source, "path")
    ck("Reader loads chapter text", "val chapterText =" in source, "chapterText")
    ck("Reader loads workbook text", "val workbookText =" in source, "workbookText")
    ck("Reader appends workbook after chapter", '"$chapterText\\n\\n---\\n\\n$workbookText"' in source, "combined markdown")
else:
    ck("Reader source exists", False, str(screen_path))

failed = [item for item in checks if not item[1]]
report = [
    "# V1 Workbook Depth Report",
    "",
    "The workbook is evaluated as practice, not as padded prose.",
    "",
    f"- Workbook JVM tests: **{workbook_tests[0]}**, failures={workbook_tests[1]}, errors={workbook_tests[2]}, skipped={workbook_tests[3]}",
    f"- Workbook assets: **{len(workbooks)}/11**",
    f"- Workbook total characters: **{workbook_chars:,}** (truncation guard only)",
    f"- Training items per workbook: **{min(training_counts) if training_counts else 0}–{max(training_counts) if training_counts else 0}**",
    f"- Gates: **{len(checks)-len(failed)}/{len(checks)} PASS**",
    "",
    "## Gate details",
]
for name, ok, detail in checks:
    report.append(f"- {'PASS' if ok else 'FAIL'} — {name}: {detail}")
report += ["", "## Final", f"**{'PASS' if not failed else 'FAIL'}**"]

out = EVIDENCE / "V1_WORKBOOK_DEPTH_REPORT.md"
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print(out.read_text(encoding="utf-8"))
if failed:
    raise SystemExit(1)
