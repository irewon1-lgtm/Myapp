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
# families explicitly so evidence proves both legacy safety and the currently active V4 book reader.
required_test_families = [
    ("TextbookExtreme60Test", 60),
    ("TextbookClean25Test", 25),
    ("TextbookMarkdownParserTest", 3),
    ("V1TextbookCatalogTest", 4),
    ("V1EditorialQualityTest", 2),
    ("TextbookLearningFlowTest", 6),
    ("TextbookRealAssetSectionTest", 1),
    ("TextbookPageComposerTest", 5),
    ("V3ReaderResilienceContractTest", 3),
    ("V4BookDepthLibraryTest", 5),
    ("V4DepthIntegrationTest", 2),
    ("V4BookReaderSourceContractTest", 4),
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

# The original learner-facing source is still the 11 V3 authored assets. V4 must add depth without
# deleting or replacing those files.
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
reader = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/V4PagedBookScreen.kt").read_text(encoding="utf-8")
adaptive = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/AdaptiveTextbookScreen.kt").read_text(encoding="utf-8")
overview = (ROOT / "app/src/main/java/com/futuretech/poweruser/ui/CurriculumOverviewScreen.kt").read_text(encoding="utf-8")
main_activity = (ROOT / "app/src/main/java/com/futuretech/poweruser/MainActivity.kt").read_text(encoding="utf-8")
depth_library = (ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/V4BookDepthLibrary.kt").read_text(encoding="utf-8")
depth_test = (ROOT / "app/src/test/java/com/futuretech/poweruser/textbook/V4BookDepthLibraryTest.kt").read_text(encoding="utf-8")
expert_paths = [
    ROOT / f"app/src/main/java/com/futuretech/poweruser/textbook/V4ExpertDepthTrack{index:02d}.kt"
    for index in range(1, 12)
]
expert_texts = [path.read_text(encoding="utf-8") for path in expert_paths if path.is_file()]
depth_sources = "\n".join(expert_texts)
expert_pack_count = depth_sources.count('sectionId = "')
expert_code_count = depth_sources.count("depthCode(")
expert_source_chars = sum(len(text) for text in expert_texts)

# Curriculum/content contracts.
ck("Catalog exposes TRACK count 11", 'const val TRACK_COUNT = 11' in catalog, "TRACK_COUNT")
ck("Catalog points at V3 assets", '"textbook/v3/track_${number.toString().padStart(2, \'0\')}.md"' in catalog, "v3 path")
ck("V3 grouping preserves authored order", "sourceSections.flatMap" in sectioner and "section.blocks.mapNotNull(::asInternalLessonBlock)" in sectioner, "grouping")
ck("Inline recall stays low-stakes", "점수를 깎지 않으며" in flow and "방금 읽은 ‘${section.title}’" in flow, "recall")
ck("V4 depth is additive", "authoredWithGuide" in flow and "V4BookDepthLibrary.blocksFor" in flow and "insertDepthBeforeSelfCheck" in flow, "authored + depth")

# Active expert-depth architecture. The old generated BookDepthPack files are kept only for source
# compatibility and must not be treated as the learner-facing quality contract.
ck("Exactly 11 expert depth source files", len(expert_texts) == 11, f"files={len(expert_texts)}")
ck("All 42 learner lessons have dedicated expert packs", expert_pack_count == 42, f"packs={expert_pack_count}")
ck("Expert layer is book-scale rather than a thin recap", expert_source_chars >= 150_000, f"source_chars={expert_source_chars:,}")
ck("Every expert lesson has a concrete trace/code block", expert_code_count >= 42, f"depthCode={expert_code_count}")
ck(
    "Active library routes all 11 expert tracks",
    all(f"V4ExpertDepthTrack{index:02d}.packs" in depth_library for index in range(1, 12))
    and "V4BookDepthTrack01To03.packs" not in depth_library
    and "V4BookDepthTrack04To07.packs" not in depth_library
    and "V4BookDepthTrack08To11.packs" not in depth_library,
    "expert routing",
)
ck(
    "Old forced topic/example/mistake/question template is not active",
    all(token not in depth_sources for token in ("workedExample =", "mistakes =", "questions =", "손으로 따라가는 실전 흐름", "초보자가 실제로 많이 틀리는 지점", "다음 질문도 생각")),
    "free-form expert blocks",
)
ck(
    "Depth thresholds are enforced by JVM tests",
    ">= 1_600" in depth_test and ">= 80_000" in depth_test and "needs several real mechanisms" in depth_test and "needs at least one concrete trace/code scenario" in depth_test,
    "1600 chars/lesson + 80000 total + mechanism/code gates",
)
ck(
    "Long boilerplate duplication is rejected",
    "duplicates.isEmpty()" in depth_test and "noLongExplanatoryParagraphIsCopiedAcrossLessons" in depth_test,
    "duplicate paragraph gate",
)
ck(
    "Duplicate expert lesson ids fail fast before rendering",
    "duplicateSectionIds" in depth_library and "require(duplicateSectionIds.isEmpty())" in depth_library,
    "duplicate id guard",
)
ck(
    "Learner-facing text strips authoring labels and transport escaping",
    'replace("V3"' in depth_library and 'replace("V4"' in depth_library and "transport escaping" in depth_test and "internal V3 authoring labels" in depth_test and "internal V4 authoring labels" in depth_test,
    "editorial sanitizer + tests",
)
advanced_terms = (
    "grapheme", "NFC", "topological", "Dijkstra", "presigned", "idempotent",
    "outbox", "saga", "covering", "WAL", "deadlock", "pepper", "KDF",
    "property-based", "mutation", "contract test", "bulkhead", "error budget",
)
ck(
    "Requested advanced concepts are promoted into expert body",
    all(term.lower() in depth_sources.lower() for term in advanced_terms),
    "advanced body coverage",
)

# Active V4 e-book pagination contracts. These gates must inspect the file actually routed by the
# adaptive shell, not the retained V1 implementation.
ck("Adaptive shell routes to V4 reader", "V4PagedBookScreen(" in adaptive and "V1TextbookScreen(" not in adaptive, "active V4")
ck("Paged composer is wired", "TextbookPageComposer.paginate" in reader and "object TextbookPageComposer" in composer, "composer")
ck("Reader derives page capacity from viewport", "BoxWithConstraints" in reader and "(heightDp - 102f) / 27f" in reader, "viewport budget")
ck("Reader has no long LazyColumn", all(token not in reader for token in ("rememberLazyListState", "snapshotFlow", "verticalScroll", "LazyColumn")), "no vertical position engine")
ck("Right edge advances page", "textbook_right_tap_zone" in reader and ".clickable { next() }" in reader, "right tap")
ck("Left edge returns page", "textbook_left_tap_zone" in reader and ".clickable { previous() }" in reader, "left tap")
ck("Horizontal swipe page turn", "detectHorizontalDragGestures" in reader and "64.dp.toPx()" in reader, "swipe")
ck("Visible page number", "textbook_page_indicator" in reader and '"${pageIndex + 1} / $pageCount"' in reader, "page indicator")
ck("Lesson cover exists", "textbook_lesson_cover" in reader and "V4LessonCoverPage" in reader, "cover")
ck("Effective reader width remains book-like", "widthIn(max = 780.dp)" in adaptive, "780dp outer cap")
ck("Book flow does not wrap every bullet list in a card", "is TextbookBlock.BulletList -> Column(" in reader and "is TextbookBlock.BulletList -> Surface(" not in reader, "plain book bullets")
ck("Code stays wrapped, not horizontally scrolling", "softWrap = true" in reader and "horizontalScroll" not in reader, "code wrapping")
ck("Paginator uses remaining page capacity", "availableLines" in composer and "sliceForCapacity" in composer and "MIN_USEFUL_REMAINDER" in composer, "dense packing")

# Exact resume contracts: TRACK, LESSON/concept and page all persist locally. Page writes use commit
# because closing immediately after a page turn must not reopen the previous page.
ck("Exact page restore is wired", "selectedPageIndex(concept.id)" in reader and "saveSelectedPageIndex(concept.id, it)" in reader, "reader page")
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
    "# V4 Expert Book Reader + Deep Beginner Content Report",
    "",
    "Scope: intact V3 authored content plus free-form V4 expert depth for all 42 learner LESSONs, remaining-space page packing, viewport-derived book pages, exact resume, focused typography, and existing practice integration.",
    "",
    f"- Original V3 assets preserved: **{len(assets)}/11**",
    f"- Original TRACK character range: **{min(asset_lengths) if asset_lengths else 0:,}–{max(asset_lengths) if asset_lengths else 0:,}**",
    f"- V4 expert depth source files: **{len(expert_texts)}/11**",
    f"- V4 dedicated expert packs: **{expert_pack_count}/42**",
    f"- Expert source characters: **{expert_source_chars:,}**",
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

out = EVIDENCE / "V4_DENSE_BOOK_DEEP_CONTENT_REPORT.md"
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print(out.read_text(encoding="utf-8"))
if failed:
    raise SystemExit(1)
