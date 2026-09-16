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
 * Converts a long TRACK source into deterministic learner-facing LESSON pages without mutating
 * source markdown. H2 is a BLOCK label and is attached to the first H3 LESSON inside that BLOCK.
 * Every later H3 starts a new page. Support BLOCKs without H3 (for example glossary/completion)
 * remain a single page. This preserves the authored TRACK -> BLOCK -> LESSON hierarchy rather than
 * merging unrelated lessons merely because their text happens to be short.
 *
 * Internal chapter/section ids remain stable-shaped for navigation compatibility. V2 progress is
 * stored in its own preference namespace, so the rewritten course does not inherit V1 completion.
 * The source asset remains the single source of truth; this class controls presentation chunks only.
 */
object TextbookSectioner {
    private const val WEIGHT_PER_MINUTE = 330

    fun split(chapterId: String, blocks: List<TextbookBlock>): List<TextbookSection> {
        if (blocks.isEmpty()) return emptyList()

        val raw = mutableListOf<MutableList<TextbookBlock>>()
        var current = mutableListOf<TextbookBlock>()
        var currentHasLessonHeading = false

        fun flush() {
            if (current.isNotEmpty()) {
                raw += current
                current = mutableListOf()
                currentHasLessonHeading = false
            }
        }

        blocks.forEach { block ->
            when (block) {
                is TextbookBlock.Heading -> when (block.level) {
                    2 -> {
                        // A new BLOCK closes the previous LESSON/support page. The BLOCK heading then
                        // travels with its first LESSON so the learner can see both hierarchy levels.
                        if (current.isNotEmpty()) flush()
                        current += block
                    }
                    3 -> {
                        // First H3 after an H2 belongs to that BLOCK page. Additional H3 headings are
                        // explicit new LESSON boundaries and must never be merged by text length.
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
                id = "$chapterId-S${(index + 1).toString().padStart(2, '0')}",
                index = index,
                title = title,
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
