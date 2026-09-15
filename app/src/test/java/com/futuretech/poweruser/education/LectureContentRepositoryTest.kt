package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LectureContentRepositoryTest {
    @Test
    fun all90LessonsReceiveExpandedLectures() {
        val lessons = CurriculumDataRepository.allLessons
        assertEquals(90, lessons.size)
        val lectures = lessons.map { LectureContentRepository.forLesson(it) }
        assertEquals(90, lectures.size)
        assertTrue(lectures.all { it.sections.size >= 12 })
        assertTrue(lectures.all { it.totalCharacters >= 2200 })
    }

    @Test
    fun everyLectureStartsWithWhyAndEndsWithReadiness() {
        CurriculumDataRepository.allLessons.forEach { lesson ->
            val lecture = LectureContentRepository.forLesson(lesson)
            assertEquals(LectureSectionKind.WHY, lecture.sections.first().kind)
            assertEquals(LectureSectionKind.READINESS, lecture.sections.last().kind)
        }
    }

    @Test
    fun everyWorkedExampleHasLineByLineExplanation() {
        CurriculumDataRepository.allLessons.forEach { lesson ->
            val lecture = LectureContentRepository.forLesson(lesson)
            val worked = lecture.sections.first { it.kind == LectureSectionKind.WORKED_EXAMPLE }
            val codeLineCount = worked.code.lines().count { it.isNotBlank() }
            assertEquals("${lesson.lessonId} line explanation mismatch", codeLineCount, worked.codeLineExplanations.size)
        }
    }
}
