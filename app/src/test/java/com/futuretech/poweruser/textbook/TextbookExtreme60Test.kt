package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
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
        @Parameterized.Parameters(name = "v2-track-section-extreme-{0}")
        fun cases(): Collection<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
    }

    @Test
    fun curriculumAndSectionScenarioPasses() {
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

        val track = tracks[(caseId - 1) % tracks.size]
        assertTrue(track.title.length >= 5)
        assertTrue(track.summary.length >= 30)
        assertTrue(track.assetPath.startsWith("textbook/v2/"))
        val practice = CurriculumDataRepository.lessonById(track.practiceLessonId)
        assertNotNull(practice)
        assertEquals("TEXTBOOK_V2", practice!!.curriculumType)
        assertEquals(track.number, practice.stepNumber)
        assertEquals(track.title, practice.title)

        val legacyV1Ref = legacyChapters.firstOrNull { it.id == track.id }
        assertNotNull(legacyV1Ref)
        assertTrue(legacyV1Ref!!.contentAvailable)
        assertEquals(track.number, legacyV1Ref.number)

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
            assertFalse(tracks.any { it.assetPath.contains("textbook/v1/") })
        }
    }
}
