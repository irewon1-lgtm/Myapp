package com.futuretech.poweruser.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(Parameterized::class)
class AiExtreme60Test(
    private val caseId: String,
    private val check: () -> Boolean
) {
    @Test
    fun verify() {
        assertTrue("$caseId failed", check())
    }

    companion object {
        private fun request(
            mode: AiLearningMode = AiLearningMode.HINT,
            curriculum: String = "BEGINNER",
            level: Int = 1,
            attempted: Boolean = false,
            message: String = "다음에 무엇을 확인해야 해?",
            code: String = "x = 1\nprint(x)",
            hintLevel: Int = 1,
            full: Boolean = false
        ) = AiTutorRequest(
            lessonId = "TEST-01",
            curriculumType = curriculum,
            level = level,
            moduleTitle = "테스트 모듈",
            lessonTitle = "테스트 레슨",
            expectedOutcome = "직접 수정하고 검증한다",
            practiceLanguage = "PYTHON",
            mode = mode,
            task = AiTutorTask.DEBUG_GUIDANCE,
            userMessage = message,
            code = code,
            hintLevel = hintLevel,
            practiceAttempted = attempted,
            explicitFullSolutionRequest = full
        )

        private fun okJson(text: String): String = """{"output":[{"type":"message","content":[{"type":"output_text","text":${jsonString(text)}}]}]}"""

        private fun jsonString(value: String): String = org.json.JSONObject.quote(value)

        private class FakeTransport(
            private val result: AiHttpResult? = AiHttpResult(200, okJson("작은 힌트입니다.")),
            private val throwable: Throwable? = null
        ) : AiHttpTransport {
            var calls: Int = 0
            var lastHeaders: Map<String, String> = emptyMap()
            var lastBody: String = ""

            override suspend fun postJson(
                url: String,
                headers: Map<String, String>,
                body: String,
                connectTimeoutMs: Int,
                readTimeoutMs: Int
            ): AiHttpResult {
                calls++
                lastHeaders = headers
                lastBody = body
                throwable?.let { throw it }
                return result ?: AiHttpResult(500, "")
            }
        }

        private class FixedGateway(private val response: AiTutorResponse) : AiTutorGateway {
            var calls = 0
            override suspend fun ask(apiKey: String?, request: AiTutorRequest): AiTutorResponse {
                calls++
                return response
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val cases = listOf<Pair<String, () -> Boolean>>(
                "AI-X01" to { !AiTutorPolicy.availability("BEGINNER", 1, false).collaborateEnabled },
                "AI-X02" to { AiTutorPolicy.availability("BEGINNER", 1, false).hintEnabled },
                "AI-X03" to { AiTutorPolicy.availability("INTERMEDIATE", 1, false).collaborateEnabled },
                "AI-X04" to { !AiTutorPolicy.availability("INTERMEDIATE", 1, true).fullSolutionEnabled },
                "AI-X05" to { !AiTutorPolicy.availability("INTERMEDIATE", 2, true).fullSolutionEnabled },
                "AI-X06" to { !AiTutorPolicy.availability("INTERMEDIATE", 3, false).fullSolutionEnabled },
                "AI-X07" to { AiTutorPolicy.availability("INTERMEDIATE", 3, true).fullSolutionEnabled },
                "AI-X08" to { AiTutorPolicy.availability("INTERMEDIATE", 5, true).fullSolutionEnabled },
                "AI-X09" to { AiTutorPolicy.authorize(request(mode = AiLearningMode.SOLO)).failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X10" to { AiTutorPolicy.authorize(request()).allowed },
                "AI-X11" to { AiTutorPolicy.authorize(request(message = "전체 코드 정답 전부 줘")).failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X12" to { AiTutorPolicy.authorize(request(mode = AiLearningMode.COLLABORATE)).failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X13" to { AiTutorPolicy.authorize(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE")).allowed },
                "AI-X14" to { AiTutorPolicy.authorize(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 2, attempted = true, full = true)).failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X15" to { AiTutorPolicy.authorize(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 4, attempted = true, full = true)).allowed },

                "AI-X16" to { AiTutorPolicy.authorize(request(message = " ")).failureKind == AiFailureKind.INVALID_INPUT },
                "AI-X17" to { AiTutorPolicy.authorize(request(message = "a".repeat(4001))).failureKind == AiFailureKind.INVALID_INPUT },
                "AI-X18" to { AiTutorPolicy.authorize(request(code = "x".repeat(12001))).failureKind == AiFailureKind.INVALID_INPUT },
                "AI-X19" to { AiTutorPolicy.authorize(request(message = "내 키 sk-ABCDEFGHIJKLMNOPQRSTUVWX야")).failureKind == AiFailureKind.SENSITIVE_INPUT },
                "AI-X20" to { AiTutorPolicy.authorize(request(message = "Bearer abcdefghijklmnopqrstuvwxyz123456")).failureKind == AiFailureKind.SENSITIVE_INPUT },
                "AI-X21" to { AiTutorPolicy.authorize(request(message = "-----BEGIN PRIVATE KEY----- abc")).failureKind == AiFailureKind.SENSITIVE_INPUT },
                "AI-X22" to { AiTutorPolicy.authorize(request(message = "password=abcdefgh1234")).failureKind == AiFailureKind.SENSITIVE_INPUT },
                "AI-X23" to { !AiTutorPolicy.containsLikelySecret("API Key가 무엇인지 개념만 설명해줘") },
                "AI-X24" to { AiTutorPolicy.authorize(request(hintLevel = 0)).failureKind == AiFailureKind.INVALID_INPUT },
                "AI-X25" to { AiTutorPolicy.authorize(request(hintLevel = 4)).failureKind == AiFailureKind.INVALID_INPUT },

                "AI-X26" to { AiTutorPolicy.buildInstructions(request()).contains("완성 코드와 최종 정답은 주지 않는다") },
                "AI-X27" to { AiTutorPolicy.buildInstructions(request(hintLevel = 3)).contains("힌트 단계 3/3") },
                "AI-X28" to { AiTutorPolicy.buildInstructions(request()).contains("TEST-01") },
                "AI-X29" to { AiTutorPolicy.buildInstructions(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 1)).contains("전체 완성 코드나 정답 대체는 금지") },
                "AI-X30" to { AiTutorPolicy.buildInstructions(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 4, attempted = true)).contains("직접 검증할 3가지") },
                "AI-X31" to { AiTutorPolicy.buildInput(request(message = "질문ABC", code = "")).contains("질문ABC") },
                "AI-X32" to { AiTutorPolicy.buildInput(request(code = "print(777)")).contains("print(777)") },
                "AI-X33" to { AiTutorPolicy.localHint(1, "H1", "H2", "H3") == "H1" },
                "AI-X34" to { AiTutorPolicy.localHint(3, "H1", "H2", "H3") == "H3" },

                "AI-X35" to { AiTutorPolicy.guardResponse(request(), "변수 값이 바뀌는 지점을 먼저 확인해보세요. 왜 그 값이 필요한가요?").allowed },
                "AI-X36" to { AiTutorPolicy.guardResponse(request(), " ").failureKind == AiFailureKind.BAD_RESPONSE },
                "AI-X37" to { AiTutorPolicy.guardResponse(request(), "```python\nprint('정답')\n```").failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X38" to { AiTutorPolicy.guardResponse(request(), "def a():\n  print(1)\nif True:\n  print(2)\nfor x in [1]:\n  print(x)\nwhile False:\n  print(3)").failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X39" to { AiTutorPolicy.guardResponse(request(), "힌트".repeat(600)).failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X40" to { AiTutorPolicy.guardResponse(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 1), "한 줄씩 원인을 좁혀봅시다.").allowed },
                "AI-X41" to { AiTutorPolicy.guardResponse(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 1), "```python\n" + (1..13).joinToString("\n") { "print($it)" } + "\n```").failureKind == AiFailureKind.POLICY_BLOCKED },
                "AI-X42" to { AiTutorPolicy.guardResponse(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE", level = 4, attempted = true), "```python\n" + (1..13).joinToString("\n") { "print($it)" } + "\n```").allowed },
                "AI-X43" to { AiTutorPolicy.guardResponse(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE"), "x".repeat(8001)).failureKind == AiFailureKind.BAD_RESPONSE },
                "AI-X44" to { AiTutorPolicy.guardResponse(request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE"), "다음 단계는 오류 메시지의 첫 줄을 읽는 것입니다.").allowed },

                "AI-X45" to {
                    val t = FakeTransport()
                    val r = runBlocking { OpenAiTutorGateway(t).ask(null, request()) }
                    r.failureKind == AiFailureKind.MISSING_KEY && t.calls == 0
                },
                "AI-X46" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(401, "{}"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.AUTH
                },
                "AI-X47" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(429, "{}"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.RATE_LIMIT
                },
                "AI-X48" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(503, "{}"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.SERVER
                },
                "AI-X49" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(400, "{}"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.BAD_REQUEST
                },
                "AI-X50" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(408, "{}"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.TIMEOUT
                },
                "AI-X51" to {
                    val body = """{"output_text":"직접 확인할 힌트"}"""
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(200, body))).ask("test-key", request()) }
                    r.success && r.text.contains("직접 확인")
                },
                "AI-X52" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(200, okJson("배열 응답 성공")))).ask("test-key", request()) }
                    r.success && r.text == "배열 응답 성공"
                },
                "AI-X53" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(200, "{}"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.BAD_RESPONSE
                },
                "AI-X54" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(AiHttpResult(200, okJson("```python\\nprint(1)\\n```")))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.POLICY_BLOCKED
                },
                "AI-X55" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(throwable = UnknownHostException("offline"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.OFFLINE
                },
                "AI-X56" to {
                    val r = runBlocking { OpenAiTutorGateway(FakeTransport(throwable = SocketTimeoutException("slow"))).ask("test-key", request()) }
                    r.failureKind == AiFailureKind.TIMEOUT
                },
                "AI-X57" to {
                    val g = FixedGateway(AiTutorResponse(false, failureKind = AiFailureKind.MISSING_KEY, userMessage = "key 없음"))
                    val r = runBlocking { AiLearningCoordinator(g).run(null, request(), Triple("H1", "H2", "H3")) }
                    r.success && r.usedLocalFallback && r.text.contains("H1")
                },
                "AI-X58" to {
                    val g = FixedGateway(AiTutorResponse(false, failureKind = AiFailureKind.RATE_LIMIT, userMessage = "limit"))
                    val r = runBlocking { AiLearningCoordinator(g).run("k", request(hintLevel = 2), Triple("H1", "H2", "H3")) }
                    r.success && r.usedLocalFallback && r.text.contains("H2")
                },
                "AI-X59" to {
                    val g = FixedGateway(AiTutorResponse(false, failureKind = AiFailureKind.MISSING_KEY, userMessage = "key 없음"))
                    val r = runBlocking { AiLearningCoordinator(g).run(null, request(mode = AiLearningMode.COLLABORATE, curriculum = "INTERMEDIATE"), Triple("H1", "H2", "H3")) }
                    !r.success && r.failureKind == AiFailureKind.MISSING_KEY
                },
                "AI-X60" to {
                    val g = FixedGateway(AiTutorResponse(true, text = "should not call"))
                    val r = runBlocking { AiLearningCoordinator(g).run("k", request(message = "sk-ABCDEFGHIJKLMNOPQRSTUVWX"), Triple("H1", "H2", "H3")) }
                    !r.success && r.failureKind == AiFailureKind.SENSITIVE_INPUT && g.calls == 0
                }
            )
            check(cases.size == 60)
            return cases.map { (id, fn) -> arrayOf(id, fn as Any) }
        }
    }
}
