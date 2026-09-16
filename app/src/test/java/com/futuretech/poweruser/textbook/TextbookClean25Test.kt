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
class TextbookClean25Test(private val gate: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "curriculum-section-clean-{0}")
        fun gates(): Collection<Array<Any>> = (1..25).map { arrayOf<Any>(it) }
    }

    @Test
    fun cleanGatePasses() {
        val legacyBooks = PowerUserCurriculumCatalog.books
        val legacyChapters = PowerUserCurriculumCatalog.chapters
        val tracks = V1TextbookCatalog.chapters
        val practices = CurriculumDataRepository.v2TrackPracticeLessons
        val sampleBlocks = TextbookMarkdownParser.parse(
            buildString {
                appendLine("# 샘플")
                repeat(12) { index ->
                    appendLine("## BLOCK ${index + 1}")
                    appendLine("### LESSON ${index + 1}")
                    appendLine(("개념을 실제 상황에 연결해서 설명한다. ".repeat(40)).trim())
                }
            }
        )
        val sections = TextbookSectioner.split("CLEAN", sampleBlocks)

        when (gate) {
            1 -> assertEquals(11, tracks.size)
            2 -> assertEquals((1..11).toList(), tracks.map { it.number })
            3 -> assertEquals(11, tracks.map { it.id }.toSet().size)
            4 -> assertTrue(tracks.all { it.assetPath.startsWith("textbook/v3/track_") })
            5 -> assertEquals((1..11).map { "V2-T%02d".format(it) }, tracks.map { it.practiceLessonId })
            6 -> assertEquals(11, practices.size)
            7 -> assertTrue(practices.all { it.curriculumType == "TEXTBOOK_V2" })
            8 -> assertTrue(practices.all { it.stepTotal == 11 })
            9 -> assertEquals(tracks.map { it.title }, practices.map { it.title })
            10 -> assertTrue(tracks.all { it.estimatedReadMinutes >= 1200 })
            11 -> assertEquals(28, V1TextbookCatalog.allSourceLessonIds.size)
            12 -> assertEquals(28, V1TextbookCatalog.allSourceLessonIds.toSet().size)
            13 -> assertEquals(11, V1TextbookCatalog.practiceLessonIds.size)
            14 -> assertTrue(sections.size >= 4)
            15 -> assertEquals(sampleBlocks, sections.flatMap { it.blocks })
            16 -> assertEquals(sections.size, sections.map { it.id }.toSet().size)
            17 -> assertTrue(sections.all { it.estimatedMinutes in 4..7 })
            18 -> assertTrue(sections.all { it.title.isNotBlank() })
            19 -> assertTrue(sections.all { it.weightedLength > 0 })
            20 -> assertTrue(tracks.none { it.title.contains("TODO", true) || it.title.contains("TBD", true) })
            21 -> assertTrue(practices.all { CurriculumDataRepository.lessonById(it.lessonId) != null })
            22 -> assertEquals(9, legacyBooks.size)
            23 -> assertEquals(134, legacyChapters.size)
            24 -> assertNotNull(PowerUserCurriculumCatalog.chapterById("V9-C16"))
            25 -> assertFalse(tracks.any { it.summary.isBlank() || it.keyConcepts.size < 5 })
        }
    }
}
