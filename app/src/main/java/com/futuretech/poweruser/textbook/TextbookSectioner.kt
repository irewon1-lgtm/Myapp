package com.futuretech.poweruser.textbook

import kotlin.math.ceil

data class TextbookSection(
    val id: String,
    val index: Int,
    val title: String,
    val estimatedMinutes: Int,
    val blocks: List<TextbookBlock>,
    val weightedLength: Int
)

/**
 * Builds learner-facing textbook pages.
 *
 * Legacy/non-V3 callers keep the authored H2/H3 split so existing tools and synthetic tests can
 * continue to exercise the parser deterministically. The V3 beginner rewrite intentionally does
 * something different: closely related authored BLOCKs are grouped into one large learner-facing
 * LESSON. Inside that page the BLOCK headings remain visible as subchapters and the old tiny H3
 * LESSON headings are demoted to ordinary subheadings. This keeps TRACK -> LESSON -> BLOCK/subtopic
 * navigation readable without regressing into hundreds of five-line vocabulary pages.
 */
object TextbookSectioner {
    private const val WEIGHT_PER_MINUTE = 330
    internal const val V3_MIN_LESSON_MINUTES = 10
    internal const val V3_MAX_LESSON_MINUTES = 60

    private data class LessonGrouping(
        val sizes: List<Int>,
        val titles: List<String>
    )

    /**
     * V3 learner-facing grouping contract.
     *
     * IMPORTANT: this is not a silent best-effort hint. If an authored BLOCK count changes without
     * updating the grouping contract, V3 must fail fast instead of falling back to hundreds of
     * atomic vocabulary-like pages.
     */
    private val v3Groupings: Map<String, LessonGrouping> = mapOf(
        "V1-C01" to LessonGrouping(
            sizes = listOf(3, 2),
            titles = listOf(
                "컴퓨터·파일·문자는 어떻게 데이터가 되는가",
                "프로그램과 코드를 읽고 실행하는 첫걸음"
            )
        ),
        "V1-C02" to LessonGrouping(
            sizes = listOf(2, 2, 2, 2),
            titles = listOf(
                "문제를 나누고 값과 변수로 표현한다",
                "입력·조건·반복으로 프로그램의 흐름을 만든다",
                "여러 값과 함수를 이용해 프로그램을 구조화한다",
                "파일·모듈·오류처리까지 작은 프로그램으로 연결한다"
            )
        ),
        "V1-C03" to LessonGrouping(
            sizes = listOf(3, 4),
            titles = listOf(
                "배열·연결구조·해시·트리로 데이터를 담는 방법",
                "그래프·검색·정렬·복잡도로 문제를 푸는 방법"
            )
        ),
        "V1-C04" to LessonGrouping(
            sizes = listOf(2, 2, 2),
            titles = listOf(
                "브라우저와 HTML로 의미 있는 화면 구조를 만든다",
                "CSS와 반응형으로 화면을 배치하고 꾸민다",
                "DOM·이벤트·DevTools로 화면을 움직이고 고친다"
            )
        ),
        "V1-C05" to LessonGrouping(
            sizes = listOf(2, 2, 2, 2),
            titles = listOf(
                "JavaScript의 값·참조·함수가 움직이는 방식",
                "데이터 가공과 비동기의 출발점",
                "Promise·async/await·event loop를 시간 순서로 이해한다",
                "여러 비동기 작업과 TypeScript로 실제 앱을 안전하게 만든다"
            )
        ),
        "V1-C06" to LessonGrouping(
            sizes = listOf(3, 4),
            titles = listOf(
                "내 기기에서 서버까지 IP·DNS·TCP·TLS의 길을 따라간다",
                "HTTP·MIME·API·CORS까지 실제 통신 문제를 해결한다"
            )
        ),
        "V1-C07" to LessonGrouping(
            sizes = listOf(3, 3),
            titles = listOf(
                "서버가 요청을 받고 검증해 업무 로직으로 보내는 전체 흐름",
                "인증·캐시·큐·동시성·관측성으로 운영 서버를 만든다"
            )
        ),
        "V1-C08" to LessonGrouping(
            sizes = listOf(4, 4),
            titles = listOf(
                "DB 구조·SQL·집계·무결성으로 데이터를 올바르게 저장한다",
                "관계·JOIN·index·transaction으로 DB를 실제 서비스에 연결한다"
            )
        ),
        "V1-C09" to LessonGrouping(
            sizes = listOf(3),
            titles = listOf(
                "보안을 처음부터 끝까지: 신뢰·암호·웹 공격을 연결한다"
            )
        ),
        "V1-C10" to LessonGrouping(
            sizes = listOf(2, 2),
            titles = listOf(
                "버그 조사와 테스트로 수정의 증거를 만든다",
                "Git에서 빌드·배포·rollback까지 변경을 추적한다"
            )
        ),
        "V1-C11" to LessonGrouping(
            sizes = listOf(2, 3),
            titles = listOf(
                "요구사항에서 모듈·아키텍처까지 시스템의 뼈대를 설계한다",
                "상태·확장·장애대응을 종합 프로젝트로 연결한다"
            )
        )
    )

    fun split(chapterId: String, blocks: List<TextbookBlock>): List<TextbookSection> {
        if (blocks.isEmpty()) return emptyList()

        val atomic = splitAtomic(chapterId, blocks)
        val grouping = v3Groupings[chapterId] ?: return atomic

        require(grouping.sizes.isNotEmpty()) { "$chapterId: V3 grouping must not be empty" }
        require(grouping.sizes.all { it > 0 }) { "$chapterId: V3 grouping sizes must all be positive" }
        require(grouping.titles.size == grouping.sizes.size) {
            "$chapterId: V3 grouping title count ${grouping.titles.size} != group count ${grouping.sizes.size}"
        }
        require(grouping.sizes.sum() == atomic.size) {
            "$chapterId: authored BLOCK/atomic count changed: expected ${grouping.sizes.sum()}, actual ${atomic.size}. " +
                "Update the V3 grouping contract; atomic fallback is forbidden."
        }

        val merged = mutableListOf<TextbookSection>()
        var cursor = 0

        grouping.sizes.forEachIndexed { lessonIndex, groupSize ->
            val sourceSections = atomic.subList(cursor, cursor + groupSize)
            cursor += groupSize

            val mergedBlocks = sourceSections.flatMap { section ->
                section.blocks.mapNotNull(::asInternalLessonBlock)
            }
            val weightedLength = mergedBlocks.sumOf(::weightOf)
            val rawEstimatedMinutes = estimatedMinutesFor(weightedLength)

            merged += TextbookSection(
                id = "$chapterId-S${(lessonIndex + 1).toString().padStart(2, '0')}",
                index = lessonIndex,
                title = grouping.titles[lessonIndex],
                estimatedMinutes = rawEstimatedMinutes.coerceAtLeast(V3_MIN_LESSON_MINUTES),
                blocks = mergedBlocks,
                weightedLength = weightedLength
            )
        }

        return merged
    }

    /**
     * Original deterministic authoring split. Unknown/synthetic chapter ids always use this path.
     */
    private fun splitAtomic(chapterId: String, blocks: List<TextbookBlock>): List<TextbookSection> {
        val raw = mutableListOf<MutableList<TextbookBlock>>()
        var current = mutableListOf<TextbookBlock>()
        var currentHasBlockHeading = false
        var currentHasLessonHeading = false

        fun flush() {
            if (current.isNotEmpty()) {
                raw += current
                current = mutableListOf()
                currentHasBlockHeading = false
                currentHasLessonHeading = false
            }
        }

        blocks.forEach { block ->
            when (block) {
                is TextbookBlock.Heading -> when (block.level) {
                    2 -> {
                        if (currentHasBlockHeading || currentHasLessonHeading) flush()
                        current += block
                        currentHasBlockHeading = true
                    }
                    3 -> {
                        if (currentHasLessonHeading) flush()
                        current += block
                        currentHasLessonHeading = true
                    }
                    else -> current += block
                }
                else -> current += block
            }
        }
        flush()

        return raw.mapIndexed { index, sectionBlocks ->
            val weightedLength = sectionBlocks.sumOf(::weightOf)
            val headings = sectionBlocks.filterIsInstance<TextbookBlock.Heading>()
            val title = headings.firstOrNull { it.level == 3 }?.text
                ?: headings.firstOrNull { it.level == 2 }?.text
                ?: headings.firstOrNull { it.level == 1 }?.text
                ?: "LESSON ${index + 1}"

            TextbookSection(
                id = "$chapterId-A${(index + 1).toString().padStart(2, '0')}",
                index = index,
                title = title,
                estimatedMinutes = estimatedMinutesFor(weightedLength).coerceIn(4, 7),
                blocks = sectionBlocks.toList(),
                weightedLength = weightedLength
            )
        }
    }

    /**
     * A merged learner-facing LESSON already has its own title in the reader header.
     * - TRACK H1 is removed to avoid duplicate chapter headings.
     * - authored BLOCK H2 becomes an internal H3 subchapter but keeps the BLOCK label.
     * - the old tiny H3 `LESSON 01 · ...` becomes an H4 subsection with the label removed.
     */
    private fun asInternalLessonBlock(block: TextbookBlock): TextbookBlock? = when (block) {
        is TextbookBlock.Heading -> when (block.level) {
            1 -> null
            2 -> TextbookBlock.Heading(level = 3, text = block.text)
            3 -> TextbookBlock.Heading(level = 4, text = stripLegacyLessonLabel(block.text))
            else -> block
        }
        else -> block
    }

    private fun stripLegacyLessonLabel(value: String): String = value
        .replace(Regex("^LESSON\\s+\\d+\\s*·\\s*", RegexOption.IGNORE_CASE), "")
        .trim()

    internal fun estimatedMinutesFor(weightedLength: Int): Int =
        ceil(weightedLength / WEIGHT_PER_MINUTE.toDouble()).toInt().coerceAtLeast(1)

    internal fun weightOf(block: TextbookBlock): Int = when (block) {
        is TextbookBlock.Heading -> block.text.length.coerceAtLeast(30)
        is TextbookBlock.Paragraph -> block.text.length
        is TextbookBlock.BulletList -> block.items.sumOf { it.length + 12 }
        is TextbookBlock.Code -> (block.text.length * 1.45).toInt().coerceAtLeast(120)
        is TextbookBlock.Table -> (
            block.headers.sumOf { it.length } +
                block.rows.flatten().sumOf { it.length }
            ).coerceAtLeast(180)
        TextbookBlock.Divider -> 24
    }
}
