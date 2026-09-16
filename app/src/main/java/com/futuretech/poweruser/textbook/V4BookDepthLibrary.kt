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

internal fun depthTitle(text: String) = TextbookBlock.Heading(3, text)
internal fun depthHeading(text: String) = TextbookBlock.Heading(4, text)
internal fun depthParagraph(text: String) = TextbookBlock.Paragraph(text.trim())
internal fun depthCode(text: String, language: String = "text") =
    TextbookBlock.Code(language, text.trimIndent().trim())
internal fun depthBullets(vararg items: String) =
    TextbookBlock.BulletList(items.map(String::trim), ordered = false)

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
