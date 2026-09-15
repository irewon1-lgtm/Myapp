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
editorial = collect("V1EditorialQualityTest")
checks = []


def ck(name, ok, detail):
    checks.append((name, bool(ok), detail))


ck("Extreme60 exact count", extreme[0] == 60 and extreme[1:4] == (0, 0, 0), str(extreme[:4]))
ck("CLEAN25 exact count", clean[0] == 25 and clean[1:4] == (0, 0, 0), str(clean[:4]))
ck("Markdown parser tests", parser[0] >= 3 and parser[1:4] == (0, 0, 0), str(parser[:4]))
ck("V1 catalog regression tests", catalog[0] >= 4 and catalog[1:4] == (0, 0, 0), str(catalog[:4]))
ck("V1 anti-bloat editorial test", editorial[0] >= 1 and editorial[1:4] == (0, 0, 0), str(editorial[:4]))

asset_dir = ROOT / "app/src/main/assets/textbook/v1"
assets = sorted(asset_dir.glob("chapter_*.md"))
ck("V1 source assets preserved", len(assets) == 11, f"count={len(assets)}")
lengths = []
substantive_counts = []
for index, p in enumerate(assets, start=1):
    text = p.read_text(encoding="utf-8")
    lengths.append(len(text))

    paragraphs = [
        re.sub(r"\s+", " ", paragraph).strip()
        for paragraph in re.split(r"\n\s*\n", text)
    ]
    substantive = [
        paragraph for paragraph in paragraphs
        if len(paragraph) >= 90
        and not paragraph.startswith("```")
        and not paragraph.startswith("|")
    ]
    substantive_counts.append(len(substantive))
    duplicate_paragraphs = {
        paragraph for paragraph in substantive
        if substantive.count(paragraph) > 1
    }
    heading_count = sum(1 for line in text.splitlines() if re.match(r"^#{1,4}\s+.+", line))

    ck(f"{p.name}: code/data examples", "```" in text, "code fence")
    ck(f"{p.name}: hands-on work", "실습" in text or "프로젝트" in text, "practice/project")
    ck(f"{p.name}: terminology support", "핵심 용어" in text, "glossary")
    ck(f"{p.name}: substantive paragraph count", len(substantive) >= 12, f"paragraphs={len(substantive)}")
    ck(f"{p.name}: no exact repeated explanatory paragraph", not duplicate_paragraphs, f"duplicates={len(duplicate_paragraphs)}")
    ck(f"{p.name}: no over-fragmentation", heading_count <= 34, f"headings={heading_count}")
    ck(f"{p.name}: minimum useful depth floor", len(text) >= 4500, f"chars={len(text)}")
    ck(f"{p.name}: no placeholder", not re.search(r"\b(TODO|TBD|LOREM)\b|준비중|나중에 작성", text, re.I), "placeholder scan")

    if index < 11:
        ck(
            f"{p.name}: capability-based exit criteria",
            "이 장을 끝내고 할 수 있어야 하는 것" in text,
            "capability checklist",
        )
        ck(
            f"{p.name}: real end-of-chapter task",
            "장 끝 미니 프로젝트" in text,
            "mini project",
        )
    else:
        ck(f"{p.name}: capstone pass criteria", "합격 기준" in text, "capstone rubric")
        ck(f"{p.name}: multi-case capstone", text.count("# 프로젝트") >= 4, f"projects={text.count('# 프로젝트')}")

all_text = "\n".join(p.read_text(encoding="utf-8") for p in assets)
# This is only a floor against accidental truncation. It is NOT a page-count target.
ck("Total V1 accidental-truncation floor", len(all_text) >= 60000, f"chars={len(all_text)}")
ck("Every chapter contributes substantive material", min(substantive_counts or [0]) >= 12, f"min={min(substantive_counts or [0])}")

curriculum_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/PowerUserCurriculumCatalog.kt"
sectioner_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookSectioner.kt"
screen_path = ROOT / "app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt"
overview_path = ROOT / "app/src/main/java/com/futuretech/poweruser/ui/CurriculumOverviewScreen.kt"
main_path = ROOT / "app/src/main/java/com/futuretech/poweruser/MainActivity.kt"

for p in (curriculum_path, sectioner_path, screen_path, overview_path, main_path):
    ck(f"source exists: {p.name}", p.exists(), str(p))

if curriculum_path.exists():
    s = curriculum_path.read_text(encoding="utf-8")
    book_rows = re.findall(r'CurriculumBook\("V[1-9]"', s)
    chapter_rows = re.findall(r'CurriculumChapterRef\("V[1-9]-C\d{2}"', s)
    ck("Canonical 9 books", len(book_rows) == 9, f"books={len(book_rows)}")
    ck("Canonical 134 chapters", len(chapter_rows) == 134, f"chapters={len(chapter_rows)}")
    ck("Only V1 content marked available", s.count(", true)") == 11 and s.count(", false)") == 123, "available=11 planned=123")

if sectioner_path.exists():
    s = sectioner_path.read_text(encoding="utf-8")
    ck("Section layer exists", "data class TextbookSection" in s and "object TextbookSectioner" in s, "section model + splitter")
    ck("Section estimate clamps 4-7", ".coerceIn(4, 7)" in s, "4..7")
    ck("Korean fallback label", '"단원 ${index + 1}"' in s, "단원")
    ck("Source is presentation-split only", "source asset remains the single source of truth" in s.lower(), "non-destructive")

if screen_path.exists() and overview_path.exists():
    learner_ui = screen_path.read_text(encoding="utf-8") + "\n" + overview_path.read_text(encoding="utf-8")
    banned = (
        "AI CODING OS", "KNOWLEDGE GRAPH", "LEARNING MATRIX", ">_ LAB",
        "AI COPILOT CONTEXT", "AI CONTEXT WINDOW", "EXECUTION GATE",
        "CORE TOKENS", "HUMAN DECISION", "AI DELEGATION"
    )
    for label in banned:
        ck(f"Learner UI hides internal label: {label}", label not in learner_ui, label)
    ck("Section strip exposed", "textbook_section_strip" in learner_ui, "section navigation")
    ck("Problem/practice linked from section", "문제·실습" in learner_ui and "textbook_practice_button" in learner_ui, "section -> practice")
    ck("Permanent TOC rail removed", "textbook_toc" not in learner_ui, "no permanent chapter rail")
    ck("Permanent insight rail removed", "textbook_insight_rail" not in learner_ui, "no permanent context rail")
    ck("Reader width limited", "widthIn(max = 780.dp)" in learner_ui, "780dp reading column")
    ck("Learner-facing chapter label Koreanized", 'text = "1권 · ${chapter.number}장/11"' in learner_ui, "장")
    ck("Learner-facing section label Koreanized", 'text = "단원 ${section.index + 1}/$sectionCount' in learner_ui, "단원")
    ck("Meaningless concept 1/1 progress removed", "textbook_concept_progress" not in learner_ui, "no concept 1/1")

if main_path.exists():
    s = main_path.read_text(encoding="utf-8")
    ck("Curriculum-first launch", 'startDestination = "curriculum"' in s, "startDestination")
    ck("Overall curriculum route", 'composable("curriculum")' in s, "route")
    ck("Chapter route", 'composable("textbook_v1/{chapterId}")' in s, "route")
    ck("Practice route preserved", 'textbook_v1/practice/{lessonId}' in s, "route")

failed = [x for x in checks if not x[1]]
report = [
    "# V1 Beginner-Depth / Anti-Bloat Quality Report",
    "",
    "Scope: beginner readability without padding, substantive examples/practice, 9-book -> 장 -> 단원 -> practice navigation, and full V1 regression.",
    "",
    f"- Extreme60: **{extreme[0]}/60**, failures={extreme[1]}, errors={extreme[2]}, skipped={extreme[3]}",
    f"- CLEAN25: **{clean[0]}/25**, failures={clean[1]}, errors={clean[2]}, skipped={clean[3]}",
    f"- Anti-bloat editorial: **{editorial[0]}** tests, failures={editorial[1]}, errors={editorial[2]}",
    f"- Parser broad: **{parser[0]}** tests",
    f"- V1 catalog broad: **{catalog[0]}** tests",
    f"- V1 assets: **{len(assets)}** chapters / total chars **{len(all_text):,}**",
    f"- Chapter char range: **{min(lengths) if lengths else 0:,}–{max(lengths) if lengths else 0:,}**",
    f"- Substantive paragraph range: **{min(substantive_counts) if substantive_counts else 0}–{max(substantive_counts) if substantive_counts else 0}**",
    f"- Static gates: **{len(checks)-len(failed)}/{len(checks)} PASS**",
    "",
    "Note: character counts are truncation guards only. They are not used as a page-count or padding target.",
    "",
    "## Static gate details",
]
for name, ok, detail in checks:
    report.append(f"- {'PASS' if ok else 'FAIL'} — {name}: {detail}")
report += ["", "## Final", f"**{'PASS' if not failed else 'FAIL'}**"]
out = EVIDENCE / "V1_BEGINNER_DEPTH_ANTI_BLOAT_REPORT.md"
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print(out.read_text(encoding="utf-8"))
if failed:
    raise SystemExit(1)
