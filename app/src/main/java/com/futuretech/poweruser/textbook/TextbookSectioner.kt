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
 * continue to exercise the parser deterministically. V3 groups authored BLOCKs into textbook-sized
 * learner LESSONs while preserving every authored block in order.
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
     * A real Actions run proved that several 3-4 BLOCK pages exceeded the 60 minute upper bound.
     * The contract therefore keeps ordinary merged pages to at most two major BLOCKs, while TRACK 01
     * uses one BLOCK per LESSON because its first three-BLOCK page also exceeded the editorial depth
     * ceiling. The total remains inside the 20..50 learner-page quality range.
     */
    private val v3Groupings: Map<String, LessonGrouping> = mapOf(
        "V1-C01" to LessonGrouping(
            sizes = listOf(1, 1, 1, 1, 1),
            titles = listOf(
                "컴퓨터는 입력을 받고 처리해 결과를 내놓는다",
                "파일·폴더·경로와 bit·byte를 연결한다",
                "문자·Unicode·UTF-8이 byte가 되는 과정을 이해한다",
                "운영체제·프로그램·코드 실행을 연결한다",
                "첫 코드를 읽고 일부러 망가뜨린 뒤 고친다"
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
            sizes = listOf(2, 1, 2, 2),
            titles = listOf(
                "배열·연결구조·스택·큐·해시로 데이터를 담는다",
                "트리·BST·heap·trie로 계층 데이터를 다룬다",
                "그래프·검색·정렬로 연결된 데이터를 푼다",
                "Big-O·BFS·DFS·greedy·DP로 문제 해결을 확장한다"
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
            sizes = listOf(2, 2, 1, 1, 1),
            titles = listOf(
                "내 기기에서 서버까지 IP·DNS·TCP의 길을 따라간다",
                "TLS·URL·HTTP 요청과 응답을 실제 메시지로 읽는다",
                "MIME·Content-Type·multipart를 파일 업로드로 이해한다",
                "API·REST·인증·cookie·token을 연결한다",
                "CORS·cache·실시간 통신과 Network 진단을 연결한다"
            )
        ),
        "V1-C07" to LessonGrouping(
            sizes = listOf(2, 1, 2, 1),
            titles = listOf(
                "서버가 요청을 받고 parsing·validation까지 처리한다",
                "service·repository·DI로 업무 규칙의 책임을 나눈다",
                "인증·권한·cache·queue로 안전한 처리 흐름을 만든다",
                "동시성·rate limit·logs·metrics·traces로 운영을 관찰한다"
            )
        ),
        "V1-C08" to LessonGrouping(
            sizes = listOf(2, 2, 2, 2),
            titles = listOf(
                "DB 구조와 SQL CRUD로 데이터를 저장하고 찾는다",
                "집계와 constraint로 통계와 데이터 무결성을 지킨다",
                "관계·정규화·JOIN으로 여러 table을 연결한다",
                "index·EXPLAIN·transaction·ACID로 성능과 동시성을 다룬다"
            )
        ),
        "V1-C09" to LessonGrouping(
            sizes = listOf(1, 1, 1),
            titles = listOf(
                "보안 자산·위협·신뢰 경계·인증·권한을 구분한다",
                "encoding·hash·password·encryption·signature를 구분한다",
                "SQLi·XSS·CSRF·SSRF와 secret·파일 업로드 위험을 막는다"
            )
        ),
        "V1-C10" to LessonGrouping(
            sizes = listOf(1, 1, 2),
            titles = listOf(
                "버그를 재현하고 원인을 좁혀 수정 근거를 만든다",
                "테스트로 수정이 맞고 다시 깨지지 않는지 증명한다",
                "Git에서 빌드·배포·rollback까지 변경을 추적한다"
            )
        ),
        "V1-C11" to LessonGrouping(
            sizes = listOf(2, 1, 2),
            titles = listOf(
                "요구사항에서 모듈·아키텍처까지 시스템의 뼈대를 설계한다",
                "state·cache·queue·동시성으로 시간 순서 문제를 다룬다",
                "확장·장애대응·관측성을 종합 프로젝트로 연결한다"
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
     *
     * V3 authored files use H3 for more than learner LESSON headings (for example TRACK projects
     * and completion criteria). Those headings remain in the surrounding BLOCK. Only an authored
     * `LESSON ...` H3 can start a new V3 atomic lesson. Legacy/synthetic callers retain the historical
     * behavior where H3 headings may split pages.
     */
    private fun splitAtomic(chapterId: String, blocks: List<TextbookBlock>): List<TextbookSection> {
        val raw = mutableListOf<MutableList<TextbookBlock>>()
        var current = mutableListOf<TextbookBlock>()
        var currentHasBlockHeading = false
        var currentHasLessonHeading = false
        val isV3AuthoredTrack = chapterId in v3Groupings

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
                        val authoredLessonHeading = block.text.startsWith("LESSON ", ignoreCase = true)
                        if (isV3AuthoredTrack && !authoredLessonHeading) {
                            current += block
                        } else {
                            if (currentHasLessonHeading) flush()
                            current += block
                            currentHasLessonHeading = true
                        }
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

    /** A merged learner-facing LESSON already has its own title in the reader header. */
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
