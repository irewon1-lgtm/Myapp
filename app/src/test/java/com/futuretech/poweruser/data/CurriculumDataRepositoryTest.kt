package com.futuretech.poweruser.data

import org.junit.Assert.*
import org.junit.Test

class CurriculumDataRepositoryTest {

    @Test
    fun testBeginnerLessonsCount() {
        val lessons = CurriculumDataRepository.beginnerLessons
        assertEquals(8, lessons.size)
    }

    @Test
    fun testIntermediateLessonsCount() {
        val lessons = CurriculumDataRepository.intermediateLessons
        assertEquals(10, lessons.size)
    }

    @Test
    fun testLessonContentsCompleteness() {
        val allLessons = CurriculumDataRepository.beginnerLessons + CurriculumDataRepository.intermediateLessons
        allLessons.forEach { lesson ->
            assertTrue(lesson.lessonId.isNotEmpty())
            assertTrue(lesson.title.isNotEmpty())
            assertTrue(lesson.explanation.isNotEmpty())
            assertTrue(lesson.codeSample.isNotEmpty())
            assertTrue(lesson.hintLevel1.isNotEmpty())
            assertTrue(lesson.hintLevel2.isNotEmpty())
            assertTrue(lesson.hintLevel3.isNotEmpty())
            assertTrue(lesson.aiHallucinationOptions.size >= 2)
        }
    }
}
