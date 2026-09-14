package com.futuretech.poweruser.data

import org.junit.Assert.*
import org.junit.Test

class CurriculumDataRepositoryTest {

    @Test
    fun `beginner has forty micro lessons`() {
        assertEquals(40, CurriculumDataRepository.beginnerLessons.size)
    }

    @Test
    fun `intermediate has fifty micro lessons`() {
        assertEquals(50, CurriculumDataRepository.intermediateLessons.size)
    }

    @Test
    fun `every module has five progressive levels`() {
        val beginnerGroups = CurriculumDataRepository.beginnerLessons.groupBy { it.moduleNumber }
        val intermediateGroups = CurriculumDataRepository.intermediateLessons.groupBy { it.moduleNumber }
        assertEquals(8, beginnerGroups.size)
        assertEquals(10, intermediateGroups.size)
        beginnerGroups.values.forEach { lessons -> assertEquals(listOf(1,2,3,4,5), lessons.map { it.stepNumber }) }
        intermediateGroups.values.forEach { lessons -> assertEquals(listOf(1,2,3,4,5), lessons.map { it.stepNumber }) }
    }

    @Test
    fun `lesson ids and sequences are unique`() {
        val all = CurriculumDataRepository.allLessons
        assertEquals(all.size, all.map { it.lessonId }.toSet().size)
        assertEquals(40, CurriculumDataRepository.beginnerLessons.map { it.unitNumber }.toSet().size)
        assertEquals(50, CurriculumDataRepository.intermediateLessons.map { it.unitNumber }.toSet().size)
    }

    @Test
    fun `all lessons include active learning material`() {
        CurriculumDataRepository.allLessons.forEach { lesson ->
            assertTrue(lesson.title.isNotBlank())
            assertTrue(lesson.explanation.length >= 50)
            assertTrue(lesson.expectedOutcome.isNotBlank())
            assertTrue(lesson.codeSample.isNotBlank())
            assertTrue(lesson.initialPracticeCode.isNotBlank())
            assertTrue(lesson.fillInBlankPrompt.contains("["))
            assertTrue(lesson.brokenCode.isNotBlank())
            assertTrue(lesson.brokenCodeFix.isNotBlank())
            assertTrue(lesson.aiHallucinationOptions.size >= 2)
            assertTrue(lesson.correctOptionIndex in lesson.aiHallucinationOptions.indices)
            assertTrue(lesson.explainKeywords.size >= 4)
            assertTrue(lesson.estimatedMinutes >= 20)
        }
    }
}
