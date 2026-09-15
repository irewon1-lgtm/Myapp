package com.futuretech.poweruser.education

import com.futuretech.poweruser.ai.AiFailureKind
import com.futuretech.poweruser.ai.AiLearningMode
import com.futuretech.poweruser.ai.AiTutorPolicy
import com.futuretech.poweruser.ai.AiTutorRequest
import com.futuretech.poweruser.ai.AiTutorTask
import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Items10To12ContractTest {
    private fun request(mode: AiLearningMode = AiLearningMode.HINT) = AiTutorRequest(
        lessonId = "TB1-C01",
        curriculumType = "TEXTBOOK_V1",
        level = 1,
        moduleTitle = "module",
        lessonTitle = "lesson",
        expectedOutcome = "outcome",
        practiceLanguage = "PYTHON",
        mode = mode,
        task = AiTutorTask.DEBUG_GUIDANCE,
        userMessage = "왜 실패했는지 설명해줘",
        code = "print('x')",
        hintLevel = 1,
        practiceAttempted = true
    )

    @Test
    fun deterministicSubmissionResultAloneOwnsPassFail() {
        val pass = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "public", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", true, "ok")
            )
        )
        val fail = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "public", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", false, "secret input must stay hidden")
            )
        )
        assertTrue(pass.passed)
        assertFalse(fail.passed)
    }

    @Test
    fun publicFailureFeedbackUsesFivePartContract() {
        val result = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "기본 입력 실행", false, "NameError at line 2"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", true, "ok")
            )
        )
        val feedback = result.failureFeedback()
        assertEquals("기본 입력 실행", feedback.whatFailed)
        assertTrue(feedback.evidence.contains("NameError"))
        assertTrue(feedback.why.contains("deterministic"))
        val text = feedback.asText()
        listOf("무엇이 실패했나:", "증거:", "왜 그런가:", "어디를 생각해볼까:", "다시 실행:").forEach {
            assertTrue(text.contains(it))
        }
    }

    @Test
    fun hiddenFailureFeedbackNeverLeaksHiddenInput() {
        val result = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "public", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "hidden", false, "TOP_SECRET_EDGE_VALUE")
            )
        )
        val text = result.failureFeedback().asText()
        assertTrue(text.contains("실패 입력값 자체는 비공개"))
        assertFalse(text.contains("TOP_SECRET_EDGE_VALUE"))
    }

    @Test
    fun aiInstructionsDeclareRuntimeTestsAsOnlyGradeAuthority() {
        val instructions = AiTutorPolicy.buildInstructions(request())
        assertTrue(instructions.contains("runtime + deterministic public/hidden tests"))
        assertTrue(instructions.contains("너는 채점자가 아니다"))
    }

    @Test
    fun aiCannotDeclareCorrectIncorrectOrPassFailVerdict() {
        val blocked = AiTutorPolicy.guardResponse(
            request(),
            "정답입니다. 이 코드는 PASS 판정입니다."
        )
        assertFalse(blocked.allowed)
        assertEquals(AiFailureKind.POLICY_BLOCKED, blocked.failureKind)
    }

    @Test
    fun aiMayExplainFailureWithoutRegrading() {
        val allowed = AiTutorPolicy.guardResponse(
            request(),
            "앱 테스트에서 숨은 경계조건이 실패했습니다. 특정 예제 값에 의존했는지 조건 분기를 확인하세요."
        )
        assertTrue(allowed.allowed)
    }

    @Test
    fun threeHintLevelsRemainDistinctAndLevelThreeDoesNotLeakExactFix() {
        val lessons = CurriculumDataRepository.beginnerLessons +
            CurriculumDataRepository.intermediateLessons +
            CurriculumDataRepository.v1TextbookPracticeLessons
        assertTrue(lessons.isNotEmpty())
        lessons.forEach { lesson ->
            assertTrue(lesson.hintLevel1.startsWith("힌트 1"))
            assertTrue(lesson.hintLevel2.startsWith("힌트 2"))
            assertTrue(lesson.hintLevel3.startsWith("힌트 3"))
            assertNotEquals(lesson.hintLevel1, lesson.hintLevel2)
            assertNotEquals(lesson.hintLevel2, lesson.hintLevel3)
            val exactFix = lesson.brokenCodeFix.trim()
            if (exactFix.length >= 4) {
                assertFalse("${lesson.lessonId} hint3 leaked exact fix", lesson.hintLevel3.contains(exactFix))
            }
        }
    }

    @Test
    fun localHintPolicyReturnsExactlyRequestedLevel() {
        assertEquals("h1", AiTutorPolicy.localHint(1, "h1", "h2", "h3"))
        assertEquals("h2", AiTutorPolicy.localHint(2, "h1", "h2", "h3"))
        assertEquals("h3", AiTutorPolicy.localHint(3, "h1", "h2", "h3"))
    }
}
