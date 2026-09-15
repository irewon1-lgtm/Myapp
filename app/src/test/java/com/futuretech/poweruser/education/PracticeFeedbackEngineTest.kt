package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.sandbox.ExecutionResult
import org.junit.Assert.*
import org.junit.Test

class PracticeFeedbackEngineTest {
    private val lessons = CurriculumDataRepository.allLessons

    @Test
    fun everyLessonProducesRichCorrectAndIncorrectFillFeedback() {
        lessons.forEach { lesson ->
            val accepted = Regex("\\[(.+?)]").find(lesson.fillInBlankPrompt)?.groupValues?.get(1)
                ?.split("/")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
            assertTrue("${lesson.lessonId} fill answer missing", accepted.isNotEmpty())

            val correct = PracticeFeedbackEngine.fillBlank(lesson, accepted.first(), accepted)
            assertEquals(FeedbackVerdict.CORRECT, correct.verdict)
            assertRich(correct)

            val wrong = PracticeFeedbackEngine.fillBlank(lesson, "완전히틀린답_${lesson.lessonId}", accepted)
            assertEquals(FeedbackVerdict.INCORRECT, wrong.verdict)
            assertRich(wrong)
            assertTrue(wrong.correctAnswer.contains(accepted.first()))
        }
    }

    @Test
    fun everyLessonAiChoiceExplainsCorrectAndWrong() {
        lessons.forEach { lesson ->
            val correct = PracticeFeedbackEngine.aiJudgement(lesson, lesson.correctOptionIndex)
            assertEquals(FeedbackVerdict.CORRECT, correct.verdict)
            assertRich(correct)

            val wrongIndex = lesson.aiHallucinationOptions.indices.first { it != lesson.correctOptionIndex }
            val wrong = PracticeFeedbackEngine.aiJudgement(lesson, wrongIndex)
            assertEquals(FeedbackVerdict.INCORRECT, wrong.verdict)
            assertRich(wrong)
            assertEquals(lesson.aiHallucinationOptions[lesson.correctOptionIndex], wrong.correctAnswer)
        }
    }

    @Test
    fun everyLessonCodeRewriteAndDebugGiveLineLevelReason() {
        lessons.forEach { lesson ->
            val rewriteOk = PracticeFeedbackEngine.codeRewrite(lesson, lesson.initialPracticeCode, lesson.initialPracticeCode, 1)
            assertEquals(FeedbackVerdict.CORRECT, rewriteOk.verdict)
            assertRich(rewriteOk)

            val rewriteBad = PracticeFeedbackEngine.codeRewrite(lesson, lesson.initialPracticeCode + "\nBROKEN_EXTRA_LINE", lesson.initialPracticeCode, 1)
            assertEquals(FeedbackVerdict.INCORRECT, rewriteBad.verdict)
            assertRich(rewriteBad)
            assertTrue(rewriteBad.why.contains("줄") || rewriteBad.why.contains("구조"))

            val debugOk = PracticeFeedbackEngine.debug(lesson, lesson.brokenCodeFix, 1)
            assertEquals(FeedbackVerdict.CORRECT, debugOk.verdict)
            assertRich(debugOk)

            val debugBad = PracticeFeedbackEngine.debug(lesson, lesson.brokenCode, 1)
            assertEquals(FeedbackVerdict.INCORRECT, debugBad.verdict)
            assertRich(debugBad)
        }
    }

    @Test
    fun openEndedQuestionsAreNeverFalselyMarkedCorrect() {
        lessons.take(20).forEach { lesson ->
            val prediction = PracticeFeedbackEngine.predictionReview(lesson, "내가 생각한 예상 결과를 충분히 적었습니다")
            val reading = PracticeFeedbackEngine.readingReview(lesson, "핵심 구조와 그 이유를 내 말로 충분히 적었습니다")
            assertEquals(FeedbackVerdict.REVIEW, prediction.verdict)
            assertEquals(FeedbackVerdict.REVIEW, reading.verdict)
            assertRich(prediction)
            assertRich(reading)
        }
    }

    @Test
    fun executionFeedbackClassifiesSuccessSecurityTimeoutSyntaxNameAndType() {
        val lesson = lessons.first()
        val cases = listOf(
            ExecutionResult(true, "42") to FeedbackVerdict.CORRECT,
            ExecutionResult(false, "", "[security] blocked", isSecurityViolation = true) to FeedbackVerdict.ERROR,
            ExecutionResult(false, "", "timeout", isTimeout = true) to FeedbackVerdict.ERROR,
            ExecutionResult(false, "", "SyntaxError: invalid syntax") to FeedbackVerdict.ERROR,
            ExecutionResult(false, "", "NameError: x is not defined") to FeedbackVerdict.ERROR,
            ExecutionResult(false, "", "TypeError: bad type") to FeedbackVerdict.ERROR
        )
        cases.forEach { (result, verdict) ->
            val feedback = PracticeFeedbackEngine.execution(lesson, "print(42)", result)
            assertEquals(verdict, feedback.verdict)
            assertRich(feedback)
        }
    }

    @Test
    fun explanationFeedbackStatesAutomaticCheckLimit() {
        val lesson = lessons.first()
        val good = PracticeFeedbackEngine.explanation(lesson, "충분히 긴 설명입니다. 무엇인지 왜 필요한지 실제 예시까지 설명합니다.", lesson.explainKeywords, emptyList(), true)
        assertEquals(FeedbackVerdict.CORRECT, good.verdict)
        assertTrue(good.why.contains("자동 검사"))
        assertRich(good)

        val bad = PracticeFeedbackEngine.explanation(lesson, "짧음", emptyList(), lesson.explainKeywords, false)
        assertEquals(FeedbackVerdict.INCORRECT, bad.verdict)
        assertRich(bad)
    }

    private fun assertRich(feedback: PracticeFeedback) {
        assertTrue(feedback.headline.isNotBlank())
        assertTrue(feedback.learnerAnswer.isNotBlank())
        assertTrue(feedback.correctAnswer.isNotBlank())
        assertTrue("why too short: ${feedback.why}", feedback.why.length >= 25)
        assertTrue(feedback.misconception.length >= 20)
        assertTrue(feedback.memoryTip.length >= 15)
        assertTrue(feedback.retryGuidance.length >= 15)
        assertTrue(feedback.reviewSectionIndex in 0..12)
    }
}