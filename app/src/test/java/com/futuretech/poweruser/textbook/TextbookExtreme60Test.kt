package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TextbookExtreme60Test(private val caseId: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "v3-track-section-extreme-{0}")
        fun cases(): Collection<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
    }

    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate real textbook asset: $assetPath; cwd=${File(".").absolutePath}")
    }

    @Test
    fun curriculumAndRealV3SectionScenarioPasses() {
        val legacyBooks = PowerUserCurriculumCatalog.books
        val legacyChapters = PowerUserCurriculumCatalog.chapters
        val tracks = V1TextbookCatalog.chapters
        val practices = CurriculumDataRepository.v2TrackPracticeLessons

        assertEquals(9, legacyBooks.size)
        assertEquals(134, legacyChapters.size)
        assertEquals(11, tracks.size)
        assertEquals(11, practices.size)
        assertEquals((1..11).toList(), tracks.map { it.number })
        assertEquals((1..11).map { "V2-T%02d".format(it) }, tracks.map { it.practiceLessonId })

        // 60 cases rotate over all 11 real V3 tracks, so every real asset is exercised repeatedly.
        val track = tracks[(caseId - 1) % tracks.size]
        assertTrue(track.title.length >= 5)
        assertTrue(track.summary.length >= 30)
        assertTrue(track.assetPath.startsWith("textbook/v3/"))
        val practice = CurriculumDataRepository.lessonById(track.practiceLessonId)
        assertNotNull(practice)
        assertEquals("TEXTBOOK_V2", practice!!.curriculumType)
        assertEquals(track.number, practice.stepNumber)
        assertEquals(track.title, practice.title)

        val legacyV1Ref = legacyChapters.firstOrNull { it.id == track.id }
        assertNotNull(legacyV1Ref)
        assertTrue(legacyV1Ref!!.contentAvailable)
        assertEquals(track.number, legacyV1Ref.number)

        // REAL V3 ASSET PATH: this is the part Extreme60 previously did not exercise.
        val realMarkdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
        val realBlocks = TextbookMarkdownParser.parse(realMarkdown)
        val realSections = TextbookSectioner.split(track.id, realBlocks)

        assertTrue("${track.id}: real V3 parser output", realBlocks.isNotEmpty())
        assertTrue("${track.id}: real learner lesson count", realSections.size in 1..6)
        assertEquals("${track.id}: real section ids unique", realSections.size, realSections.map { it.id }.toSet().size)
        assertTrue("${track.id}: real section title", realSections.all { it.title.isNotBlank() })
        assertTrue("${track.id}: real weighted length", realSections.all { it.weightedLength > 0 })
        assertTrue(
            "${track.id}: real lesson must expose overload instead of hiding it",
            realSections.all {
                it.estimatedMinutes in TextbookSectioner.V3_MIN_LESSON_MINUTES..TextbookSectioner.V3_MAX_LESSON_MINUTES
            }
        )

        val authoredBlockCount = realBlocks
            .filterIsInstance<TextbookBlock.Heading>()
            .count { it.level == 2 && it.text.startsWith("BLOCK ") }
        val renderedBlockCount = realSections
            .flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.Heading>()
            .count { it.level == 3 && it.text.startsWith("BLOCK ") }
        assertEquals("${track.id}: real authored BLOCK preservation", authoredBlockCount, renderedBlockCount)

        realSections.forEachIndexed { index, section ->
            val majorBlockCount = section.blocks
                .filterIsInstance<TextbookBlock.Heading>()
                .count { it.level == 3 && it.text.startsWith("BLOCK ") }
            assertTrue(
                "${track.id}: real LESSON ${index + 1} major BLOCK overload=$majorBlockCount",
                majorBlockCount in 1..4
            )
            val leakedLegacyLabels = section.blocks
                .filterIsInstance<TextbookBlock.Heading>()
                .filter { it.level == 4 && it.text.startsWith("LESSON ", ignoreCase = true) }
            assertTrue(
                "${track.id}: real LESSON ${index + 1} leaked old tiny LESSON labels=$leakedLegacyLabels",
                leakedLegacyLabels.isEmpty()
            )
        }

        // LEGACY/SYNTHETIC PATH: preserve the old deterministic non-V3 behavior too.
        val repeatCount = 7 + (caseId % 7)
        val markdown = buildString {
            appendLine("# TRACK $caseId")
            repeat(repeatCount) { index ->
                appendLine("## BLOCK ${index + 1}")
                appendLine("### LESSON ${index + 1}")
                appendLine(("설명${index + 1} ".repeat(90 + (caseId % 20))).trim())
                if (index % 3 == 0) {
                    appendLine("```python")
                    appendLine("value = ${index + caseId}")
                    appendLine("print(value)")
                    appendLine("```")
                }
            }
        }
        val blocks = TextbookMarkdownParser.parse(markdown)
        val sections = TextbookSectioner.split("TEST-$caseId", blocks)

        assertTrue(sections.isNotEmpty())
        assertEquals(blocks, sections.flatMap { it.blocks })
        assertEquals(sections.size, sections.map { it.id }.toSet().size)
        assertTrue(sections.all { it.estimatedMinutes in 4..7 })
        assertTrue(sections.all { it.title.isNotBlank() })
        assertTrue(sections.all { it.weightedLength > 0 })

        if (caseId % 10 == 0) {
            assertEquals(11, V1TextbookCatalog.practiceLessonIds.size)
            assertEquals(28, V1TextbookCatalog.allSourceLessonIds.toSet().size)
            assertTrue(practices.all { it.curriculumType == "TEXTBOOK_V2" })
            assertFalse(tracks.any { it.assetPath.contains("textbook/v1/") || it.assetPath.contains("textbook/v2/") })
        }
    }
}
