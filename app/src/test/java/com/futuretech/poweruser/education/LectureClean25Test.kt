package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LectureClean25Test(
    private val criterionId: Int,
    private val criterionName: String
) {
    @Test
    fun everyLessonPassesThisCleanCriterion() {
        val failures = CurriculumDataRepository.allLessons.mapNotNull { lesson ->
            val lecture = LectureContentRepository.forLesson(lesson)
            val check = LectureQualityValidator.validate(lecture).first { it.id == criterionId }
            if (check.passed) null else "${lesson.lessonId}: ${check.detail}"
        }
        assertTrue(
            "CLEAN $criterionId failed: $criterionName\n${failures.take(20).joinToString("\n")}",
            failures.isEmpty()
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "CLEAN {0}: {1}")
        fun data(): Collection<Array<Any>> {
            val sample = LectureContentRepository.forLesson(CurriculumDataRepository.allLessons.first())
            val checks = LectureQualityValidator.validate(sample)
            require(checks.size == LectureQualityValidator.CRITERIA_COUNT)
            return checks.map { arrayOf(it.id, it.name) }
        }
    }
}
