package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.sandbox.ExecutionResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FeedbackExtreme60Test(private val caseId: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "feedback-extreme-{0}")
        fun cases(): Collection<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
    }

    @Test
    fun extremeFeedbackScenarioPasses() {
        val lessons = CurriculumDataRepository.allLessons
        val lesson = lessons[(caseId - 1) % lessons.size]
        val accepted = Regex("\\[(.+?)]").find(lesson.fillInBlankPrompt)?.groupValues?.get(1)
            ?.split("/")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        assertTrue(accepted.isNotEmpty())

        val feedback = when ((caseId - 1) % 10) {
            0 -> PracticeFeedbackEngine.fillBlank(lesson, accepted.first(), accepted)
            1 -> PracticeFeedbackEngine.fillBlank(lesson, "wrong-$caseId", accepted)
            2 -> PracticeFeedbackEngine.aiJudgement(lesson, lesson.correctOptionIndex)
            3 -> {
                val wrong = lesson.aiHallucinationOptions.indices.first { it != lesson.correctOptionIndex }
                PracticeFeedbackEngine.aiJudgement(lesson, wrong)
            }
            4 -> PracticeFeedbackEngine.codeRewrite(lesson, lesson.initialPracticeCode, lesson.initialPracticeCode, 1)
            5 -> PracticeFeedbackEngine.codeRewrite(lesson, lesson.initialPracticeCode + "\nBROKEN_$caseId", lesson.initialPracticeCode, 2)
            6 -> PracticeFeedbackEngine.debug(lesson, lesson.brokenCodeFix, 1)
            7 -> PracticeFeedbackEngine.debug(lesson, lesson.brokenCode, 3)
            8 -> PracticeFeedbackEngine.execution(lesson, lesson.initialPracticeCode, executionError(caseId))
            else -> if (caseId % 2 == 0) {
                PracticeFeedbackEngine.predictionReview(lesson, "입력 처리 출력 흐름을 예상한 충분한 답안 $caseId")
            } else {
                PracticeFeedbackEngine.readingReview(lesson, "핵심 줄과 구조의 이유를 설명한 충분한 답안 $caseId")
            }
        }

        assertTrue(feedback.headline.isNotBlank())
        assertTrue(feedback.learnerAnswer.isNotBlank())
        assertTrue(feedback.correctAnswer.isNotBlank())
        assertTrue(feedback.why.length >= 25)
        assertTrue(feedback.misconception.length >= 20)
        assertTrue(feedback.memoryTip.length >= 15)
        assertTrue(feedback.retryGuidance.length >= 15)
        assertTrue(feedback.reviewSectionIndex in 0..12)

        when ((caseId - 1) % 10) {
            0, 2, 4, 6 -> assertEquals(FeedbackVerdict.CORRECT, feedback.verdict)
            1, 3, 5, 7 -> assertEquals(FeedbackVerdict.INCORRECT, feedback.verdict)
            8 -> assertEquals(FeedbackVerdict.ERROR, feedback.verdict)
            9 -> assertEquals(FeedbackVerdict.REVIEW, feedback.verdict)
        }
    }

    private fun executionError(id: Int): ExecutionResult = when (id % 6) {
        0 -> ExecutionResult(false, "", "SyntaxError: invalid syntax")
        1 -> ExecutionResult(false, "", "NameError: value is not defined")
        2 -> ExecutionResult(false, "", "TypeError: unsupported operand")
        3 -> ExecutionResult(false, "", "[security] blocked", isSecurityViolation = true)
        4 -> ExecutionResult(false, "", "timeout", isTimeout = true)
        else -> ExecutionResult(false, "", "SQLiteException: no such column")
    }
}