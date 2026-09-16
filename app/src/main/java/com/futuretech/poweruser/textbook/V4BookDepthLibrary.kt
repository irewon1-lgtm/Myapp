package com.futuretech.poweruser.textbook

import kotlin.math.ceil

internal data class BookDepthPack(
    val sectionId: String,
    val blocks: List<TextbookBlock>
)

internal fun depthPack(sectionId: String, vararg blocks: TextbookBlock) =
    BookDepthPack(sectionId, blocks.toList())

internal fun depthTitle(text: String) = TextbookBlock.Heading(3, text)
internal fun depthHeading(text: String) = TextbookBlock.Heading(4, text)
internal fun depthParagraph(text: String) = TextbookBlock.Paragraph(text.trim())
internal fun depthCode(text: String, language: String = "text") =
    TextbookBlock.Code(language, text.trimIndent().trim())
internal fun depthBullets(vararg items: String) =
    TextbookBlock.BulletList(items.map(String::trim), ordered = false)

/**
 * V4 is not a recap layer. It only adds knowledge that is materially deeper than the authored V3
 * lesson: underlying mechanisms, implementation choices, failure modes, production trade-offs,
 * diagnostic methods, or a neighboring concept that is required to reason about real systems.
 *
 * Packs own their complete block sequence. There is deliberately no mandatory global template and
 * no repeated explanatory preamble. Easy topics may receive a short technical bridge; difficult
 * topics may receive several mechanisms, traces, counterexamples, and operational checks.
 */
object V4BookDepthLibrary {
    private val packs: Map<String, BookDepthPack> = (
        V4BookDepthTrack01To03.packs +
            V4BookDepthTrack04To07.packs +
            V4BookDepthTrack08To11.packs
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
