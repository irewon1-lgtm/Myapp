package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.*
import org.junit.Test

class V1TextbookCatalogTest {
    @Test fun catalogHasExactlyElevenOrderedChapters() {
        val chapters = V1TextbookCatalog.chapters
        assertEquals(11, chapters.size)
        assertEquals((1..11).toList(), chapters.map { it.number })
        assertEquals(11, chapters.map { it.id }.toSet().size)
        assertEquals(11, chapters.map { it.practiceLessonId }.toSet().size)
    }

    @Test fun sourceLessonsArePreservedExactlyOnce() {
        val ids = V1TextbookCatalog.allSourceLessonIds
        assertEquals(28, ids.size)
        assertEquals(28, ids.toSet().size)
        assertEquals("V1-01", ids.first())
        assertEquals("V1-28", ids.last())
    }

    @Test fun everyChapterHasDeepLearningMetadata() {
        V1TextbookCatalog.chapters.forEach { chapter ->
            assertTrue(chapter.summary.length >= 30)
            assertTrue(chapter.keyConcepts.size >= 5)
            assertTrue(chapter.humanMustKnow.length >= 30)
            assertTrue(chapter.aiCanHelp.length >= 30)
            assertTrue(chapter.estimatedReadMinutes >= 40)
            assertTrue(chapter.assetPath.endsWith(".md"))
        }
    }

    @Test fun everyChapterHasMatchingTenStepPracticeLesson() {
        val practices = CurriculumDataRepository.v1TextbookPracticeLessons
        assertEquals(11, practices.size)
        V1TextbookCatalog.chapters.forEach { chapter ->
            val lesson = CurriculumDataRepository.lessonById(chapter.practiceLessonId)
            assertNotNull(lesson)
            assertEquals("TEXTBOOK_V1", lesson!!.curriculumType)
            assertEquals(chapter.number, lesson.stepNumber)
            assertEquals(11, lesson.stepTotal)
            assertTrue(lesson.explainKeywords.size >= 5)
            assertTrue(lesson.aiHallucinationOptions.size >= 2)
        }
    }
}
