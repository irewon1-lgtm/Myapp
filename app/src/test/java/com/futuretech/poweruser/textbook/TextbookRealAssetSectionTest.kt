package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookRealAssetSectionTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate real textbook asset: $assetPath; cwd=${File(".").absolutePath}")
    }

    private val expectedVisibleLessons = mapOf(
        1 to 2,
        2 to 4,
        3 to 2,
        4 to 3,
        5 to 4,
        6 to 2,
        7 to 2,
        8 to 2,
        9 to 1,
        10 to 2,
        11 to 2
    )

    @Test
    fun allElevenV3TracksBecomeFewDeepLessonsWithoutLosingAuthoredBlocks() {
        var totalVisibleLessons = 0

        V1TextbookCatalog.chapters.forEach { track ->
            val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(track.id, blocks)
            val expectedCount = requireNotNull(expectedVisibleLessons[track.number])
            totalVisibleLessons += sections.size

            val authoredBlockCount = blocks
                .filterIsInstance<TextbookBlock.Heading>()
                .count { it.level == 2 && it.text.startsWith("BLOCK ") }
            val renderedInternalBlockCount = sections
                .flatMap { it.blocks }
                .filterIsInstance<TextbookBlock.Heading>()
                .count { it.level == 3 && it.text.startsWith("BLOCK ") }
            val nestedLegacyLessonLabels = sections
                .flatMap { it.blocks }
                .filterIsInstance<TextbookBlock.Heading>()
                .filter { it.level == 4 && it.text.startsWith("LESSON ", ignoreCase = true) }

            assertTrue("${track.id}: parsed blocks", blocks.isNotEmpty())
            assertEquals("${track.id}: final learner-facing lesson count", expectedCount, sections.size)
            assertEquals("${track.id}: section ids unique", sections.size, sections.map { it.id }.toSet().size)
            assertTrue("${track.id}: long lesson estimate", sections.all { it.estimatedMinutes in 10..60 })
            assertTrue("${track.id}: every section titled", sections.all { it.title.isNotBlank() })
            assertTrue("${track.id}: no empty section", sections.all { it.blocks.isNotEmpty() })
            assertEquals(
                "${track.id}: every authored BLOCK remains visible inside merged lessons",
                authoredBlockCount,
                renderedInternalBlockCount
            )
            assertTrue(
                "${track.id}: old tiny LESSON labels must become subsection headings",
                nestedLegacyLessonLabels.isEmpty()
            )

            val legacyPath = "textbook/v2/track_${track.number.toString().padStart(2, '0')}.md"
            val legacyMarkdown = assetFile(legacyPath).readText(Charsets.UTF_8)
            val legacyBlocks = TextbookMarkdownParser.parse(legacyMarkdown)
            val legacyPages = TextbookSectioner.split("LEGACY-${track.id}", legacyBlocks)
            val legacyAverageWeight = legacyPages.map { it.weightedLength }.average()

            sections.forEachIndexed { index, section ->
                val depthRatio = section.weightedLength / legacyAverageWeight
                assertTrue(
                    "${track.id}: LESSON ${index + 1} depth ${"%.2f".format(depthRatio)}x must be >= 20x legacy average",
                    depthRatio >= 20.0
                )
            }
        }

        assertEquals("final visible lessons across 11 TRACKs", 26, totalVisibleLessons)
        assertTrue("590 old authored lessons must be reduced by far more than one third", totalVisibleLessons <= 393)
    }
}
