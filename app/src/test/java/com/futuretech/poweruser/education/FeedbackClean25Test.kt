package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.sandbox.ExecutionResult
import org.junit.Assert.*
import org.junit.Test

class FeedbackClean25Test {
    private val lesson = CurriculumDataRepository.allLessons.first()
    private val accepted = Regex("\\[(.+?)]").find(lesson.fillInBlankPrompt)!!.groupValues[1].split("/").map { it.trim() }

    @Test fun gate01CorrectLabelExplicit() = assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.fillBlank(lesson, accepted.first(), accepted).verdict)
    @Test fun gate02WrongLabelExplicit() = assertEquals(FeedbackVerdict.INCORRECT, PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).verdict)
    @Test fun gate03LearnerAnswerPreserved() = assertEquals("wrong", PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).learnerAnswer)
    @Test fun gate04CorrectAnswerShown() = assertTrue(PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).correctAnswer.contains(accepted.first()))
    @Test fun gate05WhyExplanationPresent() = assertTrue(PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).why.length >= 25)
    @Test fun gate06MisconceptionPresent() = assertTrue(PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).misconception.length >= 20)
    @Test fun gate07MemoryTipPresent() = assertTrue(PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).memoryTip.length >= 15)
    @Test fun gate08RetryGuidancePresent() = assertTrue(PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).retryGuidance.length >= 15)
    @Test fun gate09ReviewSectionValid() = assertTrue(PracticeFeedbackEngine.fillBlank(lesson, "wrong", accepted).reviewSectionIndex in 0..12)
    @Test fun gate10CaseWhitespaceNormalization() = assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.fillBlank(lesson, "  ${accepted.first().uppercase()}  ", accepted).verdict)
    @Test fun gate11AlternateAnswerAccepted() {
        val answers = listOf("환각", "Hallucination")
        assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.fillBlank(lesson, "hallucination", answers).verdict)
    }
    @Test fun gate12WrongFillNeverFalsePositive() = assertNotEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.fillBlank(lesson, "__definitely_wrong__", accepted).verdict)
    @Test fun gate13CodeFormattingIgnored() {
        val code = lesson.initialPracticeCode
        assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.codeRewrite(lesson, "\n$code\n# comment", code, 1).verdict)
    }
    @Test fun gate14ChangedCodeRejected() = assertEquals(FeedbackVerdict.INCORRECT, PracticeFeedbackEngine.codeRewrite(lesson, lesson.initialPracticeCode + "\nBROKEN", lesson.initialPracticeCode, 1).verdict)
    @Test fun gate15BrokenDebugRejected() = assertEquals(FeedbackVerdict.INCORRECT, PracticeFeedbackEngine.debug(lesson, lesson.brokenCode, 1).verdict)
    @Test fun gate16FixedDebugAccepted() = assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.debug(lesson, lesson.brokenCodeFix, 1).verdict)
    @Test fun gate17AiCorrectAccepted() = assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.aiJudgement(lesson, lesson.correctOptionIndex).verdict)
    @Test fun gate18AiWrongRejected() {
        val wrong = lesson.aiHallucinationOptions.indices.first { it != lesson.correctOptionIndex }
        assertEquals(FeedbackVerdict.INCORRECT, PracticeFeedbackEngine.aiJudgement(lesson, wrong).verdict)
    }
    @Test fun gate19ExecutionSuccessExplained() = assertEquals(FeedbackVerdict.CORRECT, PracticeFeedbackEngine.execution(lesson, "print(1)", ExecutionResult(true, "1")).verdict)
    @Test fun gate20ExecutionFailureExplained() = assertEquals(FeedbackVerdict.ERROR, PracticeFeedbackEngine.execution(lesson, "x", ExecutionResult(false, "", "SyntaxError")).verdict)
    @Test fun gate21SecurityErrorClassified() = assertTrue(PracticeFeedbackEngine.execution(lesson, "x", ExecutionResult(false, "", "security", isSecurityViolation = true)).headline.contains("보안"))
    @Test fun gate22TimeoutClassified() = assertTrue(PracticeFeedbackEngine.execution(lesson, "x", ExecutionResult(false, "", "timeout", isTimeout = true)).headline.contains("시간"))
    @Test fun gate23PredictionNotFakeCorrect() = assertEquals(FeedbackVerdict.REVIEW, PracticeFeedbackEngine.predictionReview(lesson, "충분히 길게 쓴 예상 답안입니다").verdict)
    @Test fun gate24ReadingNotFakeCorrect() = assertEquals(FeedbackVerdict.REVIEW, PracticeFeedbackEngine.readingReview(lesson, "충분히 길게 쓴 코드 읽기 답안입니다").verdict)
    @Test fun gate25AllLessonsHaveRichObjectiveFeedback() {
        CurriculumDataRepository.allLessons.forEach { l ->
            val a = Regex("\\[(.+?)]").find(l.fillInBlankPrompt)!!.groupValues[1].split("/").map { it.trim() }
            val f = PracticeFeedbackEngine.fillBlank(l, "wrong", a)
            assertTrue("${l.lessonId} why", f.why.length >= 25)
            assertTrue("${l.lessonId} retry", f.retryGuidance.length >= 15)
            assertTrue("${l.lessonId} review", f.reviewSectionIndex in 0..12)
        }
    }
}