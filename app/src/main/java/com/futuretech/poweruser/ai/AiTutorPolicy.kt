package com.futuretech.poweruser.ai

object AiTutorPolicy {
    private val likelySecretPatterns = listOf(
        Regex("(?i)sk-[A-Za-z0-9_-]{16,}"),
        Regex("(?i)\\bBearer\\s+[A-Za-z0-9._~-]{20,}"),
        Regex("-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
        Regex("(?i)\\b(?:password|passwd|api[_ -]?key|secret|access[_ -]?token)\\s*[:=]\\s*[^\\s]{8,}"),
        Regex("(?<!\\d)\\d{6}-?[1-4]\\d{6}(?!\\d)"),
        Regex("(?<!\\d)01[016789]-?\\d{3,4}-?\\d{4}(?!\\d)"),
        Regex("(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?![A-Za-z])")
    )

    private val fullSolutionIntent = Regex(
        "(?i)(전체\\s*코드|정답\\s*(?:전부|그대로)?|완성\\s*코드|다\\s*해줘|대신\\s*(?:작성|코딩)|write\\s+the\\s+whole\\s+code|full\\s+solution)"
    )

    private val aiGradingVerdict = Regex(
        """(?i)(정답\s*(?:입니다|이라고\s*판정|으로\s*판정|처리)|오답\s*(?:입니다|이라고\s*판정|으로\s*판정|처리)|(?:PASS|FAIL)\s*(?:입니다|로\s*판정|판정)|AI\s*(?:가|에게)?\s*채점|(?:this|your)\s+(?:answer|code)\s+is\s+(?:correct|incorrect))"""
    )

    fun availability(curriculumType: String, level: Int, practiceAttempted: Boolean): AiModeAvailability {
        val intermediate = curriculumType.equals("INTERMEDIATE", ignoreCase = true)
        val collaboration = intermediate
        val fullSolution = intermediate && level >= 3 && practiceAttempted
        val explanation = when {
            !intermediate -> "초급에서는 혼자 풀기와 힌트 모드만 사용합니다. AI 전체 대신작성과 협업 모드는 중급에서 해금됩니다."
            level < 3 -> "중급 Level 1~2는 협업 가능하지만 전체 코드 대신작성은 잠겨 있습니다. 설명·부분 수정·검증 중심으로 진행합니다."
            !practiceAttempted -> "중급 Level 3~5입니다. 먼저 직접 실행/수정 1회를 해야 전체 코드 요청이 해금됩니다."
            else -> "AI 협업이 해금되었습니다. 전체 코드를 받아도 반드시 실행·검증·수정 과정을 거칩니다."
        }
        return AiModeAvailability(
            collaborateEnabled = collaboration,
            fullSolutionEnabled = fullSolution,
            explanation = explanation
        )
    }

    fun authorize(request: AiTutorRequest): AiPolicyDecision {
        val text = request.userMessage.trim()
        if (request.mode == AiLearningMode.SOLO) {
            return AiPolicyDecision(false, AiFailureKind.POLICY_BLOCKED, "혼자 풀기 모드에서는 AI 요청을 보내지 않습니다.")
        }
        if (text.isBlank()) {
            return AiPolicyDecision(false, AiFailureKind.INVALID_INPUT, "AI에게 물어볼 내용을 입력하세요.")
        }
        if (text.length > 4_000 || request.code.length > 12_000) {
            return AiPolicyDecision(false, AiFailureKind.INVALID_INPUT, "AI 요청이 너무 깁니다. 질문은 4,000자, 코드는 12,000자 이하로 줄여주세요.")
        }
        if (containsLikelySecret(text) || containsLikelySecret(request.code)) {
            return AiPolicyDecision(
                false,
                AiFailureKind.SENSITIVE_INPUT,
                "API Key·비밀번호·토큰·주민번호·전화번호·이메일처럼 보이는 민감정보가 감지되어 전송을 막았습니다."
            )
        }

        val availability = availability(request.curriculumType, request.level, request.practiceAttempted)
        val asksForFullSolution = request.explicitFullSolutionRequest || fullSolutionIntent.containsMatchIn(text)

        if (request.mode == AiLearningMode.COLLABORATE && !availability.collaborateEnabled) {
            return AiPolicyDecision(false, AiFailureKind.POLICY_BLOCKED, "AI 협업 모드는 중급에서 해금됩니다.")
        }
        if (request.mode == AiLearningMode.HINT && asksForFullSolution) {
            return AiPolicyDecision(false, AiFailureKind.POLICY_BLOCKED, "힌트 모드에서는 전체 정답·완성 코드를 요청할 수 없습니다.")
        }
        if (request.mode == AiLearningMode.COLLABORATE && asksForFullSolution && !availability.fullSolutionEnabled) {
            return AiPolicyDecision(false, AiFailureKind.POLICY_BLOCKED, availability.explanation)
        }
        if (request.hintLevel !in 1..3) {
            return AiPolicyDecision(false, AiFailureKind.INVALID_INPUT, "힌트 단계는 1~3만 허용됩니다.")
        }
        return AiPolicyDecision(true)
    }

    fun buildInstructions(request: AiTutorRequest): String {
        val availability = availability(request.curriculumType, request.level, request.practiceAttempted)
        val common = """
            너는 '미래기술 Power User' 학습앱의 코딩·AI 튜터다.
            사용자의 목표는 정답 복사가 아니라 구조 이해, 직접 수정, 실행, 오류 범위 좁히기, AI 결과 검증 능력 향상이다.
            코드의 정답/오답과 PASS/FAIL은 앱의 실제 runtime + deterministic public/hidden tests만 결정한다.
            너는 채점자가 아니다. 앱이 전달한 판정을 바꾸거나 새 판정을 선언하지 말고, 테스트 증거를 바탕으로 원인과 다음 확인점을 설명한다.
            한국어로 쉽고 정확하게 답한다. 모르는 내용을 있는 것처럼 꾸미지 않는다.
            사용자의 API Key, 비밀번호, 토큰, 주민번호, 전화번호, 이메일 등 민감정보를 요구하지 않는다.
            현재 레슨: ${request.lessonId} / ${request.lessonTitle}
            과정: ${request.curriculumType}, Level ${request.level}
            언어: ${request.practiceLanguage}
            학습 목표: ${request.expectedOutcome}
        """.trimIndent()

        return when (request.mode) {
            AiLearningMode.SOLO -> common + "\nAI 호출 금지 모드다. 답을 생성하지 않는다."
            AiLearningMode.HINT -> common + """

                지금은 힌트 전용 모드다.
                힌트 단계 ${request.hintLevel}/3에 맞춰 한 단계만 도와라.
                Level 1: 방향만 제시한다.
                Level 2: 문제 위치나 확인할 구조를 지목한다.
                Level 3: 거의 정답에 가까운 수정 방향은 허용하지만 완성 코드와 최종 정답은 주지 않는다.
                코드 블록(```), 전체 완성 코드, 정답 복사본을 절대 제공하지 않는다.
                최대 6문장, 700자 이내로 답하고 마지막에 사용자가 직접 확인할 질문 1개를 붙인다.
            """.trimIndent()
            AiLearningMode.COLLABORATE -> common + if (availability.fullSolutionEnabled) {
                """

                    지금은 중급 AI 협업 모드다.
                    사용자가 이미 직접 시도했다. 설명→수정안→검증 순서로 협업한다.
                    사용자가 전체 코드를 명시적으로 요청하면 제공할 수 있지만, 코드 뒤에 반드시 '직접 검증할 3가지'를 붙인다.
                    AI 결과가 틀릴 수 있음을 밝히고 실행 결과와 오류 메시지로 검증하게 한다.
                """.trimIndent()
            } else {
                """

                    지금은 제한된 AI 협업 모드다.
                    전체 완성 코드나 정답 대체는 금지한다.
                    문제를 작은 단계로 나누고, 필요한 코드 조각은 짧게만 제공하며 사용자가 직접 이어서 작성하게 한다.
                    답변 끝에 사용자가 다음으로 직접 할 행동 1개를 명확히 제시한다.
                """.trimIndent()
            }
        }
    }

    fun buildInput(request: AiTutorRequest): String {
        val codeSection = if (request.code.isBlank()) "" else "\n\n현재 코드/답안:\n${request.code}"
        return "사용자 질문: ${request.userMessage}$codeSection"
    }

    fun guardResponse(request: AiTutorRequest, response: String): AiPolicyDecision {
        val text = response.trim()
        if (text.isBlank()) {
            return AiPolicyDecision(false, AiFailureKind.BAD_RESPONSE, "AI가 빈 응답을 반환했습니다.")
        }
        if (text.length > 8_000) {
            return AiPolicyDecision(false, AiFailureKind.BAD_RESPONSE, "AI 응답이 학습용 제한보다 너무 깁니다.")
        }
        if (aiGradingVerdict.containsMatchIn(text)) {
            return AiPolicyDecision(
                false,
                AiFailureKind.POLICY_BLOCKED,
                "AI는 정답/오답 또는 PASS/FAIL 판정을 내릴 수 없습니다. 앱의 runtime·deterministic test 결과만 채점에 사용합니다."
            )
        }
        if (request.mode == AiLearningMode.HINT) {
            val lines = text.lines().filter { it.isNotBlank() }
            val looksLikeFullCode = text.contains("```") || lines.count { looksLikeCodeLine(it) } >= 6
            if (looksLikeFullCode || text.length > 1_000) {
                return AiPolicyDecision(false, AiFailureKind.POLICY_BLOCKED, "힌트 모드에서 AI가 너무 많은 정답/코드를 제공하려 해 차단했습니다.")
            }
        }
        if (request.mode == AiLearningMode.COLLABORATE) {
            val availability = availability(request.curriculumType, request.level, request.practiceAttempted)
            if (!availability.fullSolutionEnabled) {
                val codeFenceLines = Regex("```[\\s\\S]*?```").findAll(text).sumOf { it.value.lines().size }
                if (codeFenceLines > 12) {
                    return AiPolicyDecision(false, AiFailureKind.POLICY_BLOCKED, "현재 단계에서는 AI 전체 코드 대신작성이 잠겨 있어 응답을 차단했습니다.")
                }
            }
        }
        return AiPolicyDecision(true)
    }

    fun localHint(level: Int, hint1: String, hint2: String, hint3: String): String = when (level.coerceIn(1, 3)) {
        1 -> hint1
        2 -> hint2
        else -> hint3
    }

    fun containsLikelySecret(text: String): Boolean = likelySecretPatterns.any { it.containsMatchIn(text) }

    fun asksForFullSolution(text: String): Boolean = fullSolutionIntent.containsMatchIn(text)

    private fun looksLikeCodeLine(line: String): Boolean {
        val t = line.trim()
        return t.startsWith("def ") || t.startsWith("class ") || t.startsWith("if ") ||
            t.startsWith("for ") || t.startsWith("while ") || t.startsWith("return ") ||
            t.startsWith("const ") || t.startsWith("let ") || t.startsWith("function ") ||
            t.contains("console.log(") || t.contains("print(") || t.endsWith("{") || t == "}"
    }
}
