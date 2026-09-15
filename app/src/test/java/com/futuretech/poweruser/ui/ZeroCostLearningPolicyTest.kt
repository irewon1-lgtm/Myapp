package com.futuretech.poweruser.ui

import com.futuretech.poweruser.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroCostLearningPolicyTest {

    @Test
    fun rateLimitSimulationRequiresWaitInsteadOfInfiniteRetry() {
        val result = ZeroCostLearningPolicy.simulateApi(LocalApiScenario.RATE_LIMIT)
        assertTrue(result.evidence.contains("429"))
        assertTrue(result.nextAction.contains("대기"))
        assertTrue(result.nextAction.contains("무한 재시도하지"))
    }

    @Test
    fun timeoutAndOfflineRemainLocalLearningCases() {
        val timeout = ZeroCostLearningPolicy.simulateApi(LocalApiScenario.TIMEOUT)
        val offline = ZeroCostLearningPolicy.simulateApi(LocalApiScenario.OFFLINE)
        assertTrue(timeout.title.contains("지연"))
        assertTrue(offline.title.contains("네트워크"))
    }

    @Test
    fun localSensitiveInputDetectorFindsCommonPersonalData() {
        val text = "내 번호 010-1234-5678 / mail me@test.com / 주민번호 900101-1234567 / password=abcdefgh"
        val findings = ZeroCostLearningPolicy.sensitiveInputTypes(text)
        assertTrue(findings.contains("휴대전화번호"))
        assertTrue(findings.contains("이메일 주소"))
        assertTrue(findings.contains("주민등록번호처럼 보이는 값"))
        assertTrue(findings.contains("비밀번호"))
    }

    @Test
    fun benignLearningTextDoesNotTriggerLocalSensitiveDetector() {
        assertTrue(ZeroCostLearningPolicy.sensitiveInputTypes("GET과 POST 차이를 설명해줘").isEmpty())
    }

    @Test
    fun phishingExerciseAcceptsOnlyOfficialPathChoice() {
        assertFalse(ZeroCostLearningPolicy.phishingAnswerIsSafe(0))
        assertTrue(ZeroCostLearningPolicy.phishingAnswerIsSafe(1))
        assertFalse(ZeroCostLearningPolicy.phishingAnswerIsSafe(2))
    }

    @Test
    fun aiPolicyBlocksPhoneEmailAndResidentIdBeforeNetwork() {
        fun request(message: String) = AiTutorRequest(
            lessonId = "I03-04",
            curriculumType = "INTERMEDIATE",
            level = 4,
            moduleTitle = "API와 HTTP",
            lessonTitle = "오류·Rate Limit·재시도",
            expectedOutcome = "실패 정책을 구분한다",
            practiceLanguage = "PYTHON",
            mode = AiLearningMode.COLLABORATE,
            task = AiTutorTask.COLLABORATE,
            userMessage = message,
            hintLevel = 1,
            practiceAttempted = true
        )

        assertEquals(AiFailureKind.SENSITIVE_INPUT, AiTutorPolicy.authorize(request("010-1234-5678로 연락해")).failureKind)
        assertEquals(AiFailureKind.SENSITIVE_INPUT, AiTutorPolicy.authorize(request("내 메일은 me@test.com")).failureKind)
        assertEquals(AiFailureKind.SENSITIVE_INPUT, AiTutorPolicy.authorize(request("900101-1234567")).failureKind)
    }

    @Test
    fun hintModeNeverCallsPaidGatewayEvenWhenKeyExists() {
        class CountingGateway : AiTutorGateway {
            var calls = 0
            override suspend fun ask(apiKey: String?, request: AiTutorRequest): AiTutorResponse {
                calls++
                return AiTutorResponse(success = true, text = "network")
            }
        }

        val gateway = CountingGateway()
        val request = AiTutorRequest(
            lessonId = "I03-04",
            curriculumType = "INTERMEDIATE",
            level = 4,
            moduleTitle = "API와 HTTP",
            lessonTitle = "오류·Rate Limit·재시도",
            expectedOutcome = "실패 정책을 구분한다",
            practiceLanguage = "PYTHON",
            mode = AiLearningMode.HINT,
            task = AiTutorTask.HINT,
            userMessage = "힌트 줘",
            hintLevel = 2,
            practiceAttempted = true
        )

        val result = runBlocking {
            AiLearningCoordinator(gateway).run("paid-key-present", request, Triple("H1", "H2", "H3"))
        }

        assertTrue(result.success)
        assertTrue(result.usedLocalFallback)
        assertTrue(result.text.contains("무료 로컬 힌트 2/3"))
        assertTrue(result.text.contains("H2"))
        assertEquals(0, gateway.calls)
    }
}
