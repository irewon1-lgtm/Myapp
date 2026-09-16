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
 * Converts a long TRACK source into deterministic BLOCK-sized reading sections without mutating
 * the source markdown. Internal chapter/section ids are intentionally preserved so existing
 * learner progress is not destroyed by the learner-facing TRACK -> BLOCK -> LESSON rename.
 * The source asset remains the single source of truth; this class controls presentation chunks only.
 */
object TextbookSectioner {
    private const val MIN_WEIGHT = 950
    private const val TARGET_WEIGHT = 1500
    private const val MAX_WEIGHT = 2100
    private const val HARD_MERGE_LIMIT = 2500
    private const val WEIGHT_PER_MINUTE = 330

    fun split(chapterId: String, blocks: List<TextbookBlock>): List<TextbookSection> {
        if (blocks.isEmpty()) return emptyList()

        val raw = mutableListOf<MutableList<TextbookBlock>>()
        var current = mutableListOf<TextbookBlock>()
        var currentWeight = 0

        fun flush() {
            if (current.isNotEmpty()) {
                raw += current
                current = mutableListOf()
                currentWeight = 0
            }
        }

        blocks.forEach { block ->
            val weight = weightOf(block)
            val isMeaningfulHeading = block is TextbookBlock.Heading && block.level <= 3
            val headingBoundary = isMeaningfulHeading && current.isNotEmpty() && currentWeight >= MIN_WEIGHT
            val sizeBoundary = current.isNotEmpty() && currentWeight >= TARGET_WEIGHT && currentWeight + weight > MAX_WEIGHT
            val hardBoundary = current.isNotEmpty() && currentWeight + weight > HARD_MERGE_LIMIT

            if (headingBoundary || sizeBoundary || hardBoundary) flush()
            current += block
            currentWeight += weight
        }
        flush()

        if (raw.size > 1 && raw.last().sumOf(::weightOf) < MIN_WEIGHT) {
            val tail = raw.removeAt(raw.lastIndex)
            val previous = raw.last()
            if (previous.sumOf(::weightOf) + tail.sumOf(::weightOf) <= HARD_MERGE_LIMIT) {
                previous += tail
            } else {
                raw += tail
            }
        }

        return raw.mapIndexed { index, sectionBlocks ->
            val weightedLength = sectionBlocks.sumOf(::weightOf)
            val heading = sectionBlocks.filterIsInstance<TextbookBlock.Heading>()
                .firstOrNull { it.level <= 3 }
                ?.text
                ?.takeIf { it.isNotBlank() }
            TextbookSection(
                id = "$chapterId-S${(index + 1).toString().padStart(2, '0')}",
                index = index,
                title = heading ?: "BLOCK ${index + 1}",
                estimatedMinutes = ceil(weightedLength / WEIGHT_PER_MINUTE.toDouble()).toInt().coerceIn(4, 7),
                blocks = sectionBlocks.toList(),
                weightedLength = weightedLength
            )
        }
    }

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
