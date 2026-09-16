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


def has_explicit_placeholder(text: str) -> bool:
    patterns = (
        re.compile(r"(?im)^\s*(?://|#|<!--)\s*(TODO|TBD)(?:\s*[:：-]|\s*-->)"),
        re.compile(r"LOREM\s+IPSUM", re.I),
        re.compile(r"나중에 작성", re.I),
        re.compile(r"준비중", re.I),
    )
    return any(pattern.search(text) for pattern in patterns)


checks = []


def ck(name, ok, detail):
    checks.append((name, bool(ok), detail))


# The report is executed only after the complete Gradle JVM suite. Re-check the textbook-specific
# families explicitly so the evidence file proves that pagination, deep-content, parser and reader
# contracts all ran rather than merely relying on a single build-success flag.
required_test_families = [
    ("TextbookExtreme60Test", 60),
    ("TextbookClean25Test", 25),
    ("TextbookMarkdownParserTest", 3),
    ("V1TextbookCatalogTest", 4),
    ("V1EditorialQualityTest", 2),
    ("TextbookLearningFlowTest", 5),
    ("TextbookRealAssetSectionTest", 1),
    ("TextbookPageComposerTest", 4),
    ("V3ReaderResilienceContractTest", 3),
]

runtime_rows = []
for family, minimum in required_test_families:
    result = collect(family)
    runtime_rows.append((family, result))
    ck(
        f"JVM contract: {family}",
        result[0] >= minimum and result[1:4] == (0, 0, 0),
        f"tests={result[0]} failures={result[1]} errors={result[2]} skipped={result[3]}",
    )

# Current learner-facing source is V3. Guard the real authored assets against accidental deletion,
# placeholder replacement, loss of code examples, or broken canonical TRACK ordering.
asset_dir = ROOT / "app/src/main/assets/textbook/v3"
assets = sorted(asset_dir.glob("track_*.md"))
ck("Exactly 11 current V3 TRACK assets", len(assets) == 11, f"count={len(assets)}")
asset_lengths = []
for index, path in enumerate(assets, start=1):
    text = path.read_text(encoding="utf-8")
    asset_lengths.append(len(text))
    ck(f"{path.name}: canonical TRACK heading", text.startswith(f"# TRACK {index:02d}"), f"TRACK {index:02d}")
    ck(f"{path.name}: substantial authored text", len(text) >= 8_000, f"chars={len(text):,}")
    ck(f"{path.name}: authored BLOCK structure", "## BLOCK " in text, "BLOCK heading")
    ck(f"{path.name}: executable/code examples", "```" in text, "code fence")
    ck(f"{path.name}: no explicit placeholder", not has_explicit_placeholder(text), "placeholder scan")

catalog = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/V1TextbookCatalog.kt").read_text(encoding="utf-8")
sectioner = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookSectioner.kt").read_text(encoding="utf-8")
flow = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookLearningFlow.kt").read_text(encoding="utf-8")
composer = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookPageComposer.kt").read_text(encoding="utf-8")
progress = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookProgressStore.kt").read_text(encoding="utf-8")
reader = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt").read_text(encoding="utf-8")
adaptive = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/AdaptiveTextbookScreen.kt").read_text(encoding="utf-8")
overview = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/CurriculumOverviewScreen.kt").read_text(encoding="utf-8")
main_activity = (ROOT / "app/src/main/java/com/futuretech/poweruser/MainActivity.kt").read_text(encoding="utf-8")

# Curriculum/content contracts.
ck("Catalog exposes TRACK count 11", 'const val TRACK_COUNT = 11' in catalog, "TRACK_COUNT")
ck("Catalog points at V3 assets", '"textbook/v3/track_${number.toString().padStart(2, \'0\')}.md"' in catalog, "v3 path")
ck("V3 grouping preserves authored order", "sourceSections.flatMap" in sectioner and "section.blocks.mapNotNull(::asInternalLessonBlock)" in sectioner, "grouping")
ck("Inline recall stays low-stakes", "점수를 깎지 않으며" in flow and "방금 읽은 ‘${section.title}’" in flow, "recall")

# E-book pagination contracts. These specifically prevent regression to the old endless vertical
# reader which was the user-visible defect this release fixes.
ck("Paged composer is wired", "TextbookPageComposer.paginate" in reader and "object TextbookPageComposer" in composer, "composer")
ck("Reader has no long LazyColumn", "rememberLazyListState" not in reader and "snapshotFlow" not in reader and "verticalScroll" not in reader, "no vertical position engine")
ck("Right edge advances page", "textbook_right_tap_zone" in reader and ".clickable { next() }" in reader, "right tap")
ck("Left edge returns page", "textbook_left_tap_zone" in reader and ".clickable { previous() }" in reader, "left tap")
ck("Horizontal swipe page turn", "detectHorizontalDragGestures" in reader and "72.dp.toPx()" in reader, "swipe")
ck("Visible page number", "textbook_page_indicator" in reader and '"${pageIndex + 1} / ${pages.size}"' in reader, "page indicator")
ck("Lesson cover exists", "textbook_lesson_cover" in reader and "LessonCoverPage" in reader, "cover")
ck("Reader width remains book-like", "widthIn(max = 820.dp)" in reader, "820dp max")
ck("Code stays wrapped, not horizontally scrolling", "softWrap = true" in reader and "horizontalScroll(rememberScrollState())" not in reader, "code wrapping")

# Exact resume contracts: TRACK, LESSON/concept and page all persist locally. Page writes use commit
# because closing immediately after a page turn must not reopen the previous page.
ck("Exact page restore is wired", "selectedPageIndex(currentConcept.id)" in reader and "saveSelectedPageIndex(currentConcept.id, it)" in reader, "reader page")
ck("Page key persists locally", '"reader_page_$conceptId"' in progress, "reader_page")
ck("Reader position commits synchronously", progress.count(".commit()") >= 4, "chapter/lesson/concept/page commits")

# Focused reading shell and library home.
ck("Temporary TOC drawer", "ModalNavigationDrawer" in adaptive and "drawerState.open()" in adaptive and "reader_toc_button" in reader, "drawer")
ck("Tablet reader is single column", "reader_single_column" in adaptive and "PermanentNavigationDrawer" not in adaptive and "NavigationRail" not in adaptive, "single column")
ck("Library is default destination", 'startDestination = "curriculum"' in main_activity, "startDestination")
ck("Library home headline", "내 코딩 서재" in overview, "library")
ck("Continue-reading action", "curriculum_continue_button" in overview and "읽던 페이지 이어서 보기" in overview, "continue")
ck("Horizontal book shelf", "curriculum_track_shelf" in overview and "LazyRow" in overview, "shelf")
ck("Review remains reachable", "curriculum_review_button" in overview, "review")

banned = (
    "AI CODING OS", "KNOWLEDGE GRAPH", "LEARNING MATRIX", ">_ LAB",
    "AI COPILOT CONTEXT", "AI CONTEXT WINDOW", "EXECUTION GATE",
    "CORE TOKENS", "HUMAN DECISION", "AI DELEGATION",
)
learner_ui = reader + "\n" + overview
for label in banned:
    ck(f"Learner UI hides internal label: {label}", label not in learner_ui, label)

failed = [item for item in checks if not item[1]]
report = [
    "# V3 Paged E-book Textbook Quality Report",
    "",
    "Scope: deep-beginner authored content, deterministic non-scrolling pagination, left/right page turns, exact resume, focused single-column reading, library home, and existing practice integration.",
    "",
    f"- Current V3 assets: **{len(assets)}/11**",
    f"- TRACK character range: **{min(asset_lengths) if asset_lengths else 0:,}–{max(asset_lengths) if asset_lengths else 0:,}**",
    f"- Runtime/source gates: **{len(checks) - len(failed)}/{len(checks)} PASS**",
    "",
    "## Runtime families",
]
for family, result in runtime_rows:
    report.append(f"- {family}: tests={result[0]}, failures={result[1]}, errors={result[2]}, skipped={result[3]}")
report += ["", "## Gate details"]
for name, ok, detail in checks:
    report.append(f"- {'PASS' if ok else 'FAIL'} — {name}: {detail}")
report += ["", "## Final", f"**{'PASS' if not failed else 'FAIL'}**"]

out = EVIDENCE / "V3_PAGED_EBOOK_REPORT.md"
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print(out.read_text(encoding="utf-8"))
if failed:
    raise SystemExit(1)
