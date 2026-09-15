package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TextbookExtreme60Test(private val caseId: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "textbook-extreme-{0}")
        fun cases(): Collection<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
    }

    @Test fun extremeTabletTextbookScenarioPasses() {
        val chapters = V1TextbookCatalog.chapters
        val chapter = chapters[(caseId - 1) % chapters.size]
        val widths = listOf(320, 359, 360, 599, 719, 720, 799, 840, 1079, 1080, 1280, 1600)
        val width = widths[(caseId - 1) % widths.size]
        val mode = TextbookLayoutMode.fromWidthDp(width)
        val lesson = CurriculumDataRepository.lessonById(chapter.practiceLessonId)

        assertEquals(11, chapters.size)
        assertTrue(chapter.id.matches(Regex("V1-C\\d{2}")))
        assertTrue(chapter.title.length >= 7)
        assertTrue(chapter.assetPath.matches(Regex("textbook/v1/chapter_\\d{2}\\.md")))
        assertTrue(chapter.sourceLessonIds.isNotEmpty())
        assertTrue(chapter.sourceLessonIds.size <= 4)
        assertTrue(chapter.keyConcepts.size >= 5)
        assertTrue(chapter.summary.length >= 30)
        assertTrue(chapter.humanMustKnow.length >= 30)
        assertTrue(chapter.aiCanHelp.length >= 30)
        assertNotNull(lesson)
        assertEquals(11, lesson!!.stepTotal)
        assertTrue(lesson.codeSample.isNotBlank())
        assertTrue(lesson.initialPracticeCode.isNotBlank())
        assertTrue(lesson.brokenCodeFix.isNotBlank())
        assertTrue(lesson.explainKeywords.size >= 5)

        when {
            width >= 1080 -> assertEquals(TextbookLayoutMode.EXPANDED, mode)
            width >= 720 -> assertEquals(TextbookLayoutMode.MEDIUM, mode)
            else -> assertEquals(TextbookLayoutMode.COMPACT, mode)
        }
        if (caseId % 10 == 0) {
            assertEquals(28, V1TextbookCatalog.allSourceLessonIds.toSet().size)
            assertEquals(11, V1TextbookCatalog.practiceLessonIds.size)
        }
    }
}
