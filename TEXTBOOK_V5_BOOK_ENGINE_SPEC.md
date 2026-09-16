# TEXTBOOK V5 — BOOK-SCALE ENGINE CONTRACT

Status: design + executable-gate contract. This file is not a PASS report.

## 1. User contract translated into measurable gates

The current learner-facing baseline is the complete V3 authored corpus plus every V4 expert-depth block currently injected into those 11 tracks. The baseline MUST be measured from repository content, not copied into a hard-coded number.

For every TRACK 01..11:

- `semantic_chars(track) >= 5 * semantic_chars(current_all_11_tracks_baseline)`.
- The count is semantic text after Markdown syntax/whitespace normalization. File bytes, repeated padding, comments, source metadata and duplicated passages do not count.
- A track that reaches the raw character threshold by duplication MUST fail the novelty gate.
- There is no 60-minute or 70x ceiling. Real size and reading time must be exposed rather than clamped.
- Each track is a book, not one giant lesson: TRACK -> PART -> CHAPTER -> SECTION -> PAGE.

This literal requirement is intentionally much larger than a normal 500-page technical book. The gate remains literal unless the user later changes it.

## 2. Content quality model

No mandatory learner-facing template is repeated across every section. Internally, coverage is audited across these dimensions:

1. prerequisite / term introduction before first dependent use,
2. concrete execution or state trace,
3. causal mechanism (why the system behaves that way),
4. implementation boundary or data flow,
5. at least one realistic failure mode where appropriate,
6. diagnosis / evidence / debugging method,
7. trade-off or alternative design where a genuine choice exists,
8. transfer to a real program or service.

A section does not need eight headings. These are audit dimensions, not boilerplate prose.

Novice sequencing follows explanation -> worked example -> faded guidance -> independent application for skills that require procedural practice. Retrieval prompts are spaced across chapters rather than repeated after every paragraph.

## 3. Source discipline

The book must synthesize sources; it must not copy copyrighted textbook prose.

Each factual unit that goes beyond elementary language syntax is attached to one or more stable source IDs in a sidecar source map. Reader-visible references are generated from those IDs. A source ID must resolve to a registry entry with title, author/owner, edition/version when relevant, and canonical URL or bibliographic identifier.

Preferred hierarchy:

1. official specifications / platform documentation,
2. established university or vendor engineering texts,
3. major technical reference books,
4. peer-reviewed learning research for pedagogy,
5. secondary explanation only when primary material is insufficient.

Initial systems/reference spine:

- CSAPP3: Bryant & O'Hallaron, Computer Systems: A Programmer's Perspective, 3e.
- OSTEP: Arpaci-Dusseau & Arpaci-Dusseau, Operating Systems: Three Easy Pieces.
- TLPI: Michael Kerrisk, The Linux Programming Interface.
- ANDROID-FUNDAMENTALS: Android Developers official application fundamentals documentation.
- SRE: Google Site Reliability Engineering / SRE Workbook / incident and release-engineering guidance.

Reader-engine design evidence is maintained separately from learner-fact source spines and includes W3C EPUB/Reading Systems guidance, Readium pagination guidance and Android Compose text-measurement documentation.

Track-specific registries add language, database, network, security, testing and architecture primary sources.

## 4. Storage architecture for book-scale content

Do not put tens of megabytes of prose in Kotlin source strings.

New content lives under:

`app/src/main/assets/textbook/v5/track_XX/`

Each track contains a manifest and multiple part files. The app loads one part/chapter at a time. It MUST NOT call `readText()` on the full multi-megabyte track.

Required hierarchy metadata:

- track id/title
- part id/title/order
- chapter id/title/order
- section id/title/order
- content asset path
- source-map path
- prerequisite concept ids

Stable IDs are never derived from display titles.

## 5. Reader layout contract

The current character-count paginator (`charsPerLine`, fixed `27dp` line assumption) is forbidden for V5 final pagination.

Pagination must use actual Compose text/layout measurement at the active viewport width, density and font scale. `TextMeasurer`/`TextLayoutResult` or equivalent measured layout is the source of truth.

Phone page chrome contract:

- content display area consumes essentially the full viewport after system insets;
- permanent top/bottom chrome is minimized; controls may overlay/auto-hide;
- no arbitrary `102dp` reserved-height subtraction;
- no 30dp/22dp fixed inner padding on compact screens;
- no vertical scrolling inside normal reading pages;
- horizontal swipe and left/right edge taps both work;
- exact page/chapter/track resume survives recreation;
- font-scale changes trigger repagination, never clipping.

Density is a distribution gate rather than a rule that every adversarial page must hit an identical fill percentage. Standard reading sizes require at least 99.5% of non-final synthetic pages to use >=82% of measured content height; target tall-phone defaults require at least 99.9%. Real Compose/device layout has a separate instrumented gate. Cover/recall/end pages are excluded from density scoring.

Adaptive heading orphan rule, derived from the executed extreme simulation:

- a one-line heading at the bottom must keep at least one measured body line with it;
- a heading already wrapping to two or more measured lines reserves no additional body-line quota, because unconditional extra reservation created large artificial holes on compact screens;
- instrumented Compose tests remain the final authority for actual clipping and density.

Paragraph widow/orphan rule: avoid one-line fragments at page boundaries when a measured split can move one additional line without violating utilization.

Ordered-list rule: authored start numbering must survive parsing, pagination and rendering. A continuation page must never silently reset an ordered list to 1.

## 6. Extreme simulation matrix

The free test suite must cover at least:

- widths: 320, 360, 412, 600, 800 dp;
- representative short/tall viewports and portrait/landscape;
- font scales: 0.85, 1.0, 1.15, 1.30, 1.60, 2.00;
- Korean, Latin, long unbroken tokens, inline punctuation;
- paragraphs, ordered/unordered lists, code, tables and mixed pages;
- first/last page, section/part/track boundaries;
- invalid saved page index, process recreation, content revision;
- missing asset, malformed manifest, duplicate IDs and broken source IDs;
- synthetic very large track to prove loading remains part-local rather than whole-book.

## 7. CLEAN passes — distinct failure classes

A CLEAN PASS may only be reported after the named checks actually execute.

CLEAN-01 Corpus scale: every track meets literal 5x baseline semantic-content gate.
CLEAN-02 Novelty: repeated paragraph/shingle/filler ratios stay below thresholds; copy-padding fails.
CLEAN-03 Source integrity: all source IDs resolve; references have no broken mappings; sampled factual claims are cross-checked.
CLEAN-04 Prerequisite graph: important terms are introduced before dependent use; no unexplained jargon jumps.
CLEAN-05 Code/command correctness: executable examples compile/run where a local harness exists; destructive examples are not default commands.
CLEAN-06 Pagination: measured layout has no clipping/overflow and meets page-density gates across the viewport/font matrix.
CLEAN-07 Navigation/resume: forward/back, edge tap, swipe, chapter boundary and recreation preserve exact progress.
CLEAN-08 Stress/performance: large-book navigation loads a bounded working set; no all-track eager read.
CLEAN-09 Accessibility: large font and semantics remain usable; system insets do not consume text.
CLEAN-10 Cross-track consistency: shared terms/invariants do not contradict one another.
CLEAN-11 Learning progression: worked examples fade toward independent tasks instead of repeating solved examples.
CLEAN-12 Regression: legacy practice links, track IDs and unrelated app functions remain intact.

## 8. Verification status rules

- Static source inspection is not Android runtime proof.
- A test written but not executed is `PREPARED`, never `PASS`.
- A deterministic packing simulation validates packing logic only; it is not device rendering proof.
- Full Gradle / Android runtime / instrumented verification remains `UNVERIFIED` until actually executed.
- GitHub Actions and deployment require a separate explicit approval immediately before execution.

## 9. Build order

1. Freeze current baseline and add failing book-scale/novelty/source gates.
2. Replace heuristic pagination with measured pagination and compact reader chrome.
3. Introduce manifest + lazy part loader + source registry.
4. Re-author tracks as independent books from primary/reference sources.
5. Run local/free static and JVM checks available in the current environment.
6. Run the 12 distinct CLEAN categories that are executable without Actions.
7. Stop before Actions/deploy. Request approval only when one final external execution can validate Gradle/instrumented/runtime checks.
