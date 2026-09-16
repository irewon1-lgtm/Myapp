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

    @Test
    fun allElevenV3TracksBecomeFewDeepLessonsWithoutLosingAuthoredBlocks() {
        var totalVisibleLessons = 0

        V1TextbookCatalog.chapters.forEach { track ->
            val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(track.id, blocks)
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
            assertTrue("${track.id}: learner lesson count must stay small but not vanish", sections.size in 1..6)
            assertEquals("${track.id}: section ids unique", sections.size, sections.map { it.id }.toSet().size)
            assertTrue(
                "${track.id}: true estimated reading time must not be hidden by a 60-minute clamp: ${sections.map { it.estimatedMinutes }}",
                sections.all { it.estimatedMinutes in TextbookSectioner.V3_MIN_LESSON_MINUTES..TextbookSectioner.V3_MAX_LESSON_MINUTES }
            )
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

            sections.forEachIndexed { index, section ->
                val internalBlockCount = section.blocks
                    .filterIsInstance<TextbookBlock.Heading>()
                    .count { it.level == 3 && it.text.startsWith("BLOCK ") }
                assertTrue(
                    "${track.id}: LESSON ${index + 1} merges too many major BLOCKs ($internalBlockCount)",
                    internalBlockCount in 1..4
                )
            }

            val legacyPath = "textbook/v2/track_${track.number.toString().padStart(2, '0')}.md"
            val legacyMarkdown = assetFile(legacyPath).readText(Charsets.UTF_8)
            val legacyBlocks = TextbookMarkdownParser.parse(legacyMarkdown)
            val legacyPages = TextbookSectioner.split("LEGACY-${track.id}", legacyBlocks)
            val legacyAverageWeight = legacyPages.map { it.weightedLength }.average()

            sections.forEachIndexed { index, section ->
                val depthRatio = section.weightedLength / legacyAverageWeight
                assertTrue(
                    "${track.id}: LESSON ${index + 1} depth ${"%.2f".format(depthRatio)}x is still vocabulary-card sized",
                    depthRatio >= 12.0
                )
                assertTrue(
                    "${track.id}: LESSON ${index + 1} depth ${"%.2f".format(depthRatio)}x is overpacked beyond the audited 70x ceiling",
                    depthRatio <= 70.0
                )
            }
        }

        assertTrue(
            "learner-facing lesson count must remain far below the old 590 without becoming an arbitrary exact-count target: $totalVisibleLessons",
            totalVisibleLessons in 20..50
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun v3GroupingContractFailsFastInsteadOfSilentlyReturningAtomicPages() {
        val blocks = buildList<TextbookBlock> {
            repeat(4) { index ->
                add(TextbookBlock.Heading(2, "BLOCK ${index + 1}"))
                add(TextbookBlock.Heading(3, "LESSON 01 · test ${index + 1}"))
                add(TextbookBlock.Paragraph("충분히 긴 설명 ".repeat(250)))
            }
        }

        TextbookSectioner.split("V1-C01", blocks)
    }

    @Test
    fun overlongSyntheticV3LessonExposesTrueMinutesInsteadOfBeingClampedToSixty() {
        val blocks = buildList<TextbookBlock> {
            repeat(5) { index ->
                add(TextbookBlock.Heading(2, "BLOCK ${index + 1}"))
                add(TextbookBlock.Heading(3, "LESSON 01 · test ${index + 1}"))
                add(TextbookBlock.Paragraph("아주 긴 설명 ".repeat(3_000)))
            }
        }

        val sections = TextbookSectioner.split("V1-C01", blocks)
        assertTrue(
            "synthetic overlong content must reveal a >60 minute estimate so the quality gate can fail",
            sections.any { it.estimatedMinutes > TextbookSectioner.V3_MAX_LESSON_MINUTES }
        )
    }
}
