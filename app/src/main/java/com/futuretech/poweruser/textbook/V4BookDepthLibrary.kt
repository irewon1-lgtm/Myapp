package com.futuretech.poweruser.textbook

import kotlin.math.ceil

// Legacy V4 pack model is retained only so the old generated depth files still compile on this
// branch. V4BookDepthLibrary no longer serves these recap-oriented packs to learners.
internal data class BookDepthTopic(
    val title: String,
    val explanation: String
)

internal data class BookDepthPack(
    val sectionId: String,
    val topics: List<BookDepthTopic>,
    val workedExample: String,
    val mistakes: List<String>,
    val questions: List<String>
)

internal fun depthTopic(title: String, explanation: String) = BookDepthTopic(title, explanation)

internal fun depthPack(
    sectionId: String,
    topics: List<BookDepthTopic>,
    workedExample: String,
    mistakes: List<String>,
    questions: List<String>
) = BookDepthPack(sectionId, topics, workedExample, mistakes, questions)

// New expert layer: every lesson owns an arbitrary block sequence. There is no mandatory
// topic/example/mistake/question template and no repeated explanatory preamble.
internal data class ExpertDepthPack(
    val sectionId: String,
    val blocks: List<TextbookBlock>
)

internal fun expertPack(sectionId: String, vararg blocks: TextbookBlock) =
    ExpertDepthPack(sectionId, blocks.toList())

/**
 * Source files may contain transport escaping or internal authoring-version references. Neither is
 * learner-facing content, so normalize them once at the rendering boundary instead of allowing
 * implementation history such as "V3"/"V4" to leak into the book.
 */
private fun cleanEditorialText(text: String): String =
    text
        .replace("\\\"", "\"")
        .replace(
            "이 LESSON은 기존 V4에서 방향이 좋았던 부분을 기준점으로 삼아 더 깊게 확장한다.",
            "앞서 배운 queue의 성질에서 출발해 실제 운영 문제까지 더 깊게 확장한다."
        )
        .replace(
            "이 LESSON은 기존 V4의 좋은 방향을 유지하되 더 깊게 간다.",
            "앞서 배운 분산 시스템의 기본에서 한 단계 더 깊게 들어간다."
        )
        .replace("V3에서", "앞선 학습에서")
        .replace("V3가", "앞선 학습이")
        .replace("V3에", "앞선 학습에서")
        .replace("V3를", "앞선 학습을")
        .replace("V3의", "앞선 학습의")
        .replace("V3", "앞선 학습")
        .replace("V4에서", "앞선 학습에서")
        .replace("V4가", "앞선 학습이")
        .replace("V4에", "앞선 학습에서")
        .replace("V4를", "앞선 학습을")
        .replace("V4의", "앞선 학습의")
        .replace("V4", "앞선 학습")

internal fun depthTitle(text: String) = TextbookBlock.Heading(3, cleanEditorialText(text))
internal fun depthHeading(text: String) = TextbookBlock.Heading(4, cleanEditorialText(text))
internal fun depthParagraph(text: String) = TextbookBlock.Paragraph(cleanEditorialText(text.trim()))
internal fun depthCode(text: String, language: String = "text") =
    TextbookBlock.Code(language, cleanEditorialText(text.trimIndent().trim()))
internal fun depthBullets(vararg items: String) =
    TextbookBlock.BulletList(items.map { cleanEditorialText(it.trim()) }, ordered = false)

/**
 * V4 is not a recap layer. It only adds knowledge materially deeper than the V3 authored lesson:
 * mechanisms, implementation choices, failure modes, production trade-offs, diagnostic methods,
 * and the neighboring concepts required to reason about real systems.
 */
object V4BookDepthLibrary {
    private val packs: Map<String, ExpertDepthPack> = (
        V4ExpertDepthTrack01.packs +
            V4ExpertDepthTrack02.packs +
            V4ExpertDepthTrack03.packs +
            V4ExpertDepthTrack04.packs +
            V4ExpertDepthTrack05.packs +
            V4ExpertDepthTrack06.packs +
            V4ExpertDepthTrack07.packs +
            V4ExpertDepthTrack08.packs +
            V4ExpertDepthTrack09.packs +
            V4ExpertDepthTrack10.packs +
            V4ExpertDepthTrack11.packs
        ).associateBy { it.sectionId }

    fun blocksFor(sectionId: String): List<TextbookBlock> {
        val pack = packs[sectionId] ?: return emptyList()
        return listOf(TextbookBlock.Divider) + pack.blocks
    }

    fun hasDepthFor(sectionId: String): Boolean = sectionId in packs

    fun sectionIds(): Set<String> = packs.keys

    fun extraCharacterCount(sectionId: String): Int = blocksFor(sectionId).sumOf { block ->
        when (block) {
            is TextbookBlock.Heading -> block.text.length
            is TextbookBlock.Paragraph -> block.text.length
            is TextbookBlock.BulletList -> block.items.sumOf { it.length }
            is TextbookBlock.Code -> block.text.length
            is TextbookBlock.Table -> block.headers.sumOf { it.length } + block.rows.flatten().sumOf { it.length }
            TextbookBlock.Divider -> 0
        }
    }

    fun extraEstimatedMinutes(sectionId: String): Int =
        ceil(extraCharacterCount(sectionId) / 520.0).toInt().coerceAtLeast(0)
}
