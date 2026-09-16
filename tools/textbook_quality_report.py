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
learning_flow = collect("TextbookLearningFlowTest")
real_asset = collect("TextbookRealAssetSectionTest")
checks = []


def ck(name, ok, detail):
    checks.append((name, bool(ok), detail))


# Runtime/JVM evidence. This report is only called after Gradle tests in CI.
ck("Extreme60 exact count", extreme[0] == 60 and extreme[1:4] == (0, 0, 0), str(extreme[:4]))
ck("CLEAN25 exact count", clean[0] == 25 and clean[1:4] == (0, 0, 0), str(clean[:4]))
ck("Markdown parser tests", parser[0] >= 3 and parser[1:4] == (0, 0, 0), str(parser[:4]))
ck("V2 catalog tests", catalog[0] >= 4 and catalog[1:4] == (0, 0, 0), str(catalog[:4]))
ck("V2 editorial prerequisite tests", editorial[0] >= 2 and editorial[1:4] == (0, 0, 0), str(editorial[:4]))
ck("V2 learning-flow tests", learning_flow[0] >= 5 and learning_flow[1:4] == (0, 0, 0), str(learning_flow[:4]))
ck("Real V2 asset section tests", real_asset[0] >= 1 and real_asset[1:4] == (0, 0, 0), str(real_asset[:4]))

asset_dir = ROOT / "app/src/main/assets/textbook/v2"
assets = sorted(asset_dir.glob("track_*.md"))
ck("Exactly 11 V2 TRACK assets", len(assets) == 11, f"count={len(assets)}")

lengths = []
block_counts = []
lesson_counts = []
substantive_counts = []
all_text_parts = []

for index, path in enumerate(assets, start=1):
    text = path.read_text(encoding="utf-8")
    all_text_parts.append(text)
    lines = text.lines()
    lengths.append(len(text))

    blocks = [(i, line) for i, line in enumerate(lines) if line.startswith("## BLOCK ")]
    lessons = [i for i, line in enumerate(lines) if line.startswith("### LESSON ")]
    support_labels = ("핵심 용어 사전", "완료 기준")
    instructional = [(i, line) for i, line in blocks if not any(label in line for label in support_labels)]
    block_counts.append(len(blocks))
    lesson_counts.append(len(lessons))

    paragraphs = [
        re.sub(r"\s+", " ", paragraph).strip()
        for paragraph in re.split(r"\n\s*\n", text)
    ]
    substantive = [
        paragraph for paragraph in paragraphs
        if len(paragraph) >= 120
        and not paragraph.startswith("```")
        and not paragraph.startswith("|")
    ]
    substantive_counts.append(len(substantive))
    duplicates = {
        paragraph for paragraph in substantive
        if substantive.count(paragraph) > 1
    }

    expected_prefix = f"# TRACK {index:02d}"
    ck(f"{path.name}: canonical TRACK heading", text.startswith(expected_prefix), expected_prefix)
    ck(f"{path.name}: book-scale BLOCK count", len(blocks) >= 25, f"blocks={len(blocks)}")
    ck(f"{path.name}: LESSON coverage", len(lessons) >= len(instructional), f"lessons={len(lessons)} instructional={len(instructional)}")
    ck(f"{path.name}: accidental truncation floor", len(text) >= 8000, f"chars={len(text)}")
    ck(f"{path.name}: code/data examples", "```" in text, "code fence")
    ck(f"{path.name}: glossary", "핵심 용어 사전" in text, "glossary")
    ck(f"{path.name}: capability completion criteria", "완료 기준" in text, "completion")
    ck(
        f"{path.name}: real project/practice",
        "TRACK 프로젝트" in text or "최종 프로젝트" in text or "프로젝트 ·" in text,
        "project",
    )
    ck(f"{path.name}: no repeated padding paragraph", not duplicates, f"duplicates={len(duplicates)}")
    ck(
        f"{path.name}: no placeholder",
        not re.search(r"\b(TODO|TBD|LOREM)\b|준비중|나중에 작성", text, re.I),
        "placeholder scan",
    )

    for block_pos, (block_start, heading) in enumerate(blocks):
        if any(label in heading for label in support_labels):
            continue
        block_end = blocks[block_pos + 1][0] if block_pos + 1 < len(blocks) else len(lines)
        has_lesson = any(block_start < lesson_index < block_end for lesson_index in lessons)
        ck(f"{path.name}: {heading} has LESSON", has_lesson, f"range={block_start + 1}-{block_end}")

all_text = "\n".join(all_text_parts)
ck("V2 total accidental-truncation floor", len(all_text) >= 120_000, f"chars={len(all_text):,}")
ck("Every TRACK has substantial explanation", min(substantive_counts or [0]) >= 8, f"min={min(substantive_counts or [0])}")


def ordered(source: str, headings):
    cursor = -1
    for heading in headings:
        next_index = source.find(heading, cursor + 1)
        if next_index < 0 or next_index <= cursor:
            return False
        cursor = next_index
    return True


if len(assets) >= 8:
    track5 = assets[4].read_text(encoding="utf-8")
    track6 = assets[5].read_text(encoding="utf-8")
    track8 = assets[7].read_text(encoding="utf-8")
    ck(
        "TRACK 05 teaches async prerequisites before advanced terms",
        ordered(track5, [
            "## BLOCK 24 · 비동기를 배우기 전에 기다림",
            "## BLOCK 28 · callback",
            "## BLOCK 29 · Promise",
            "## BLOCK 38 · event loop",
            "## BLOCK 39 · microtask",
            "## BLOCK 44 · race condition",
        ]),
        "waiting -> callback -> Promise -> event loop -> microtask -> race",
    )
    ck(
        "TRACK 06 teaches MIME/upload vocabulary incrementally",
        ordered(track6, [
            "## BLOCK 31 · 데이터 종류를 왜 알려줘야 하나",
            "## BLOCK 32 · MIME type",
            "## BLOCK 34 · Content-Type",
            "## BLOCK 35 · Accept",
            "## BLOCK 38 · 파일 업로드 문제",
            "## BLOCK 39 · multipart/form-data",
            "## BLOCK 40 · boundary",
        ]),
        "data type need -> MIME -> Content-Type -> Accept -> upload -> multipart -> boundary",
    )
    ck(
        "TRACK 08 teaches database integrity and ACID incrementally",
        ordered(track8, [
            "## BLOCK 20 · PRIMARY KEY",
            "## BLOCK 26 · FOREIGN KEY",
            "## BLOCK 28 · 데이터 무결성",
            "## BLOCK 44 · transaction을 배우기 전에",
            "## BLOCK 45 · transaction",
            "## BLOCK 47 · Atomicity",
            "## BLOCK 48 · Consistency",
            "## BLOCK 49 · Isolation",
            "## BLOCK 50 · Durability",
        ]),
        "PK -> FK -> integrity -> transaction -> A/C/I/D",
    )

catalog_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/V1TextbookCatalog.kt"
sectioner_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookSectioner.kt"
flow_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/TextbookLearningFlow.kt"
practice_data_path = ROOT / "app/src/main/java/com/futuretech/poweruser/data/V2TrackPracticeData.kt"
data_repo_path = ROOT / "app/src/main/java/com/futuretech/poweruser/data/CurriculumDataRepository.kt"
screen_path = ROOT / "app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt"
overview_path = ROOT / "app/src/main/java/com/futuretech/poweruser/ui/CurriculumOverviewScreen.kt"
main_path = ROOT / "app/src/main/java/com/futuretech/poweruser/MainActivity.kt"
legacy_curriculum_path = ROOT / "app/src/main/java/com/futuretech/poweruser/textbook/PowerUserCurriculumCatalog.kt"

for path in (
    catalog_path, sectioner_path, flow_path, practice_data_path, data_repo_path,
    screen_path, overview_path, main_path, legacy_curriculum_path,
):
    ck(f"source exists: {path.name}", path.exists(), str(path))

if catalog_path.exists():
    source = catalog_path.read_text(encoding="utf-8")
    ck("Catalog exposes TRACK count 11", 'const val TRACK_COUNT = 11' in source, "TRACK_COUNT")
    ck("Catalog points at V2 assets", '"textbook/v2/track_${number.toString().padStart(2, \'0\')}.md"' in source, "v2 asset path")
    practice_ids = re.findall(r'"V2-T\d{2}"', source)
    ck("Catalog has 11 V2 practice ids", len(practice_ids) == 11 and len(set(practice_ids)) == 11, f"ids={len(practice_ids)}")
    ck("Catalog no longer maps current TRACKs to TB1 practice", '"TB1-C' not in source, "no TB1 current mapping")

if practice_data_path.exists():
    source = practice_data_path.read_text(encoding="utf-8")
    ck("V2 practice type", 'type = "TEXTBOOK_V2"' in source, "TEXTBOOK_V2")
    ck("V2 practice creates exactly numbered ids", 'id = "V2-T%02d".format(n)' in source, "V2-T%02d")
    ck("V2 practice includes executable language variety", all(token in source for token in ('"PYTHON"', '"HTML_JS"', '"TYPESCRIPT"', '"SQL"')), "4 runtimes")

if data_repo_path.exists():
    source = data_repo_path.read_text(encoding="utf-8")
    ck("Repository registers V2 practice", "v2TrackPracticeLessons" in source and "V2TrackPracticeData.lessons" in source, "V2 repository")
    ck("V2 lookup precedes legacy V1", source.find("v2TrackPracticeLessons.find") < source.find("v1TextbookPracticeLessons.find"), "lookup order")

if sectioner_path.exists():
    source = sectioner_path.read_text(encoding="utf-8")
    ck("Section layer exists", "data class TextbookSection" in source and "object TextbookSectioner" in source, "section model + splitter")
    ck("LESSON estimates clamp 4-7", ".coerceIn(4, 7)" in source, "4..7")
    ck("Learner fallback says LESSON", '"LESSON ${index + 1}"' in source, "LESSON fallback")
    ck("Source split remains non-destructive", "source asset remains the single source of truth" in source.lower(), "non-destructive")

if flow_path.exists():
    source = flow_path.read_text(encoding="utf-8")
    ck("Inline problem is current-LESSON recall", "방금 읽은 ‘$title’" in source, "current title recall")
    ck("Inline recall is low stakes", "점수를 깎지 않으며" in source, "low-stakes")
    ck("Inline flow does not reuse legacy AI question", "aiHallucinationQuestion" not in source, "no stale prompt")

if screen_path.exists() and overview_path.exists():
    learner_ui = screen_path.read_text(encoding="utf-8") + "\n" + overview_path.read_text(encoding="utf-8")
    banned = (
        "AI CODING OS", "KNOWLEDGE GRAPH", "LEARNING MATRIX", ">_ LAB",
        "AI COPILOT CONTEXT", "AI CONTEXT WINDOW", "EXECUTION GATE",
        "CORE TOKENS", "HUMAN DECISION", "AI DELEGATION",
    )
    for label in banned:
        ck(f"Learner UI hides internal label: {label}", label not in learner_ui, label)
    ck("Overview presents 11 TRACKS", "TRACK 11개" in learner_ui and "코딩 완전과정" in learner_ui, "11-track overview")
    ck("Overview no longer presents old 9-book navigation", "9권 · 134장" not in learner_ui and "권 → 장 → 단원" not in learner_ui, "legacy overview absent")
    ck("LESSON strip exposed", "textbook_section_strip" in learner_ui and "LESSON ${index + 1}" in learner_ui, "lesson navigation")
    ck("TRACK practice linked", "문제·실습" in learner_ui and "textbook_practice_button" in learner_ui, "TRACK -> practice")
    ck("Reader width limited", "widthIn(max = 780.dp)" in learner_ui, "780dp reading column")
    ck("Learner-facing TRACK label", "TRACK ${chapter.number.toString().padStart(2, '0')}" in learner_ui, "TRACK")
    ck("Learner-facing LESSON progress", "LESSON ${section.index + 1}/$sectionCount" in learner_ui, "LESSON")

if main_path.exists():
    source = main_path.read_text(encoding="utf-8")
    ck("Curriculum-first launch", 'startDestination = "curriculum"' in source, "startDestination")
    ck("Overall curriculum route", 'composable("curriculum")' in source, "route")
    ck("Internal chapter route retained for progress compatibility", 'composable("textbook_v1/{chapterId}")' in source, "internal route")
    ck("Practice route retained", 'textbook_v1/practice/{lessonId}' in source, "practice route")

if legacy_curriculum_path.exists():
    source = legacy_curriculum_path.read_text(encoding="utf-8")
    book_rows = re.findall(r'CurriculumBook\("V[1-9]"', source)
    chapter_rows = re.findall(r'CurriculumChapterRef\("V[1-9]-C\d{2}"', source)
    ck("Legacy future curriculum metadata preserved: 9 books", len(book_rows) == 9, f"books={len(book_rows)}")
    ck("Legacy future curriculum metadata preserved: 134 chapters", len(chapter_rows) == 134, f"chapters={len(chapter_rows)}")

failed = [item for item in checks if not item[1]]
report = [
    "# V2 11-TRACK Book-Scale Textbook Quality Report",
    "",
    "Scope: complete novice -> software design/verification, TRACK -> BLOCK -> LESSON hierarchy, current-lesson retrieval, aligned executable practice, prerequisite ordering, anti-padding, and compatibility safeguards.",
    "",
    f"- Extreme60: **{extreme[0]}/60**, failures={extreme[1]}, errors={extreme[2]}, skipped={extreme[3]}",
    f"- CLEAN25: **{clean[0]}/25**, failures={clean[1]}, errors={clean[2]}, skipped={clean[3]}",
    f"- Editorial prerequisite tests: **{editorial[0]}**, failures={editorial[1]}, errors={editorial[2]}",
    f"- Learning-flow tests: **{learning_flow[0]}**, failures={learning_flow[1]}, errors={learning_flow[2]}",
    f"- V2 assets: **{len(assets)}** TRACKS / total chars **{len(all_text):,}**",
    f"- TRACK char range: **{min(lengths) if lengths else 0:,}–{max(lengths) if lengths else 0:,}**",
    f"- BLOCK range: **{min(block_counts) if block_counts else 0}–{max(block_counts) if block_counts else 0}**",
    f"- LESSON range: **{min(lesson_counts) if lesson_counts else 0}–{max(lesson_counts) if lesson_counts else 0}**",
    f"- Static/runtime gates: **{len(checks) - len(failed)}/{len(checks)} PASS**",
    "",
    "Character floors are truncation guards only; they are not padding/page-count targets.",
    "",
    "## Gate details",
]
for name, ok, detail in checks:
    report.append(f"- {'PASS' if ok else 'FAIL'} — {name}: {detail}")
report += ["", "## Final", f"**{'PASS' if not failed else 'FAIL'}**"]

out = EVIDENCE / "V2_11_TRACK_BOOKSCALE_REPORT.md"
out.write_text("\n".join(report) + "\n", encoding="utf-8")
print(out.read_text(encoding="utf-8"))
if failed:
    raise SystemExit(1)
