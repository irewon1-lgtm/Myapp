package com.futuretech.poweruser.education

enum class PracticeTestVisibility { PUBLIC, HIDDEN }

enum class EditableLiteralKind { STRING, NUMBER, BOOLEAN }

data class EditableTestLiteral(
    val start: Int,
    val endExclusive: Int,
    val raw: String,
    val kind: EditableLiteralKind
) {
    val displayValue: String
        get() = when (kind) {
            EditableLiteralKind.STRING ->
                if (raw.length >= 2) raw.substring(1, raw.length - 1) else ""
            else -> raw
        }
}

data class PracticeTestCaseResult(
    val id: String,
    val visibility: PracticeTestVisibility,
    val label: String,
    val passed: Boolean,
    val evidence: String
)

data class SubmissionFailureFeedback(
    val whatFailed: String,
    val evidence: String,
    val why: String,
    val focus: String,
    val retry: String
) {
    fun asText(): String = listOf(
        "무엇이 실패했나: $whatFailed",
        "증거: $evidence",
        "왜 그런가: $why",
        "어디를 생각해볼까: $focus",
        "다시 실행: $retry"
    ).joinToString("\n")
}

data class PracticeSubmissionResult(
    val results: List<PracticeTestCaseResult>
) {
    val passed: Boolean
        get() = results.isNotEmpty() && results.all { it.passed }

    val publicPassed: Int
        get() = results.count { it.visibility == PracticeTestVisibility.PUBLIC && it.passed }

    val publicTotal: Int
        get() = results.count { it.visibility == PracticeTestVisibility.PUBLIC }

    val hiddenPassed: Int
        get() = results.count { it.visibility == PracticeTestVisibility.HIDDEN && it.passed }

    val hiddenTotal: Int
        get() = results.count { it.visibility == PracticeTestVisibility.HIDDEN }

    fun failureFeedback(): SubmissionFailureFeedback {
        val publicFailure = results.firstOrNull {
            it.visibility == PracticeTestVisibility.PUBLIC && !it.passed
        }
        if (publicFailure != null) {
            return SubmissionFailureFeedback(
                whatFailed = publicFailure.label,
                evidence = publicFailure.evidence,
                why = "제출 코드가 공개 요구사항을 아직 만족하지 못했습니다. 판정은 실제 runtime과 deterministic test 결과만 사용합니다.",
                focus = "입력→처리→출력 흐름과 기준 예제를 비교하고, 첫 번째로 달라지는 지점을 찾으세요.",
                retry = "수정 후 ▶ 실행으로 확인하고 다시 ✓ 제출하세요."
            )
        }

        val hiddenFailures = results.count {
            it.visibility == PracticeTestVisibility.HIDDEN && !it.passed
        }
        if (hiddenFailures > 0) {
            return SubmissionFailureFeedback(
                whatFailed = "숨은 경계조건",
                evidence = "숨은 테스트 $hiddenPassed/$hiddenTotal 통과 · 실패 입력값 자체는 비공개",
                why = "기본 예제는 동작하지만 빈 값·음수·큰 값·중복 같은 변형에서 결과가 달라졌습니다.",
                focus = "특정 예제 값에만 맞춘 로직, 경계값 처리, 조건 분기를 확인하세요.",
                retry = "테스트 값을 직접 바꿔 ▶ 실행한 뒤 다시 ✓ 제출하세요."
            )
        }

        return SubmissionFailureFeedback(
            whatFailed = "제출 상태 재확인",
            evidence = "공개 $publicPassed/$publicTotal · 숨은 $hiddenPassed/$hiddenTotal",
            why = "현재 실패 테스트를 특정하지 못했습니다.",
            focus = "코드를 다시 실행해 최신 테스트 증거를 만든 뒤 제출하세요.",
            retry = "▶ 실행 → ✓ 제출 순서로 다시 확인하세요."
        )
    }
}

data class EdgeVariant(
    val label: String,
    val code: String
)

data class PairedEdgeVariant(
    val label: String,
    val learnerCode: String,
    val referenceCode: String
)

/**
 * Deterministic helpers for the practice Run/Submit flow.
 *
 * Run may use an optional learner-chosen literal override. Submit never uses that
 * scratch override: it grades the code currently in the editor against public and
 * hidden deterministic tests.
 */
object PracticeTestEngine {
    fun normalizeOutput(output: String): String = output
        .replace("\r\n", "\n")
        .lines()
        .map { it.trimEnd() }
        .joinToString("\n")
        .trim()

    fun extractEditableLiterals(code: String): List<EditableTestLiteral> {
        val found = mutableListOf<EditableTestLiteral>()
        var index = 0

        while (index < code.length) {
            val c = code[index]

            if (c == '#' || (c == '/' && index + 1 < code.length && code[index + 1] == '/') ||
                (c == '-' && index + 1 < code.length && code[index + 1] == '-')
            ) {
                index = skipLine(code, index)
                continue
            }

            if (c == '\'' || c == '"') {
                val quote = c
                val start = index
                index++
                var escaped = false
                while (index < code.length) {
                    val current = code[index]
                    if (escaped) {
                        escaped = false
                    } else if (current == '\\') {
                        escaped = true
                    } else if (current == quote) {
                        index++
                        break
                    }
                    index++
                }
                if (index > start + 1 && index <= code.length && code[index - 1] == quote) {
                    found += EditableTestLiteral(
                        start = start,
                        endExclusive = index,
                        raw = code.substring(start, index),
                        kind = EditableLiteralKind.STRING
                    )
                }
                continue
            }

            if (isNumberStart(code, index)) {
                val start = index
                if (code[index] == '-') index++
                while (index < code.length && code[index].isDigit()) index++
                if (index < code.length && code[index] == '.') {
                    index++
                    while (index < code.length && code[index].isDigit()) index++
                }
                found += EditableTestLiteral(
                    start = start,
                    endExclusive = index,
                    raw = code.substring(start, index),
                    kind = EditableLiteralKind.NUMBER
                )
                continue
            }

            val boolean = booleanAt(code, index)
            if (boolean != null) {
                found += EditableTestLiteral(
                    start = index,
                    endExclusive = index + boolean.length,
                    raw = boolean,
                    kind = EditableLiteralKind.BOOLEAN
                )
                index += boolean.length
                continue
            }

            index++
        }

        return found
    }

    fun applyUserOverride(
        code: String,
        literal: EditableTestLiteral,
        userValue: String
    ): String? {
        if (!isValidRange(code, literal)) return null
        val replacement = when (literal.kind) {
            EditableLiteralKind.STRING -> {
                val quote = literal.raw.firstOrNull() ?: '\''
                val escaped = userValue
                    .replace("\\", "\\\\")
                    .replace(quote.toString(), "\\$quote")
                "$quote$escaped$quote"
            }
            EditableLiteralKind.NUMBER -> {
                if (!userValue.trim().matches(Regex("-?\\d+(\\.\\d+)?"))) return null
                userValue.trim()
            }
            EditableLiteralKind.BOOLEAN -> {
                when (userValue.trim().lowercase()) {
                    "true" -> if (literal.raw.firstOrNull()?.isUpperCase() == true) "True" else "true"
                    "false" -> if (literal.raw.firstOrNull()?.isUpperCase() == true) "False" else "false"
                    else -> return null
                }
            }
        }
        return code.replaceRange(literal.start, literal.endExclusive, replacement)
    }

    fun edgeVariants(code: String, maxVariants: Int = 3): List<EdgeVariant> {
        val literal = extractEditableLiterals(code).firstOrNull() ?: return emptyList()
        return edgeReplacements(literal)
            .distinct()
            .filter { it != literal.raw }
            .take(maxVariants)
            .mapIndexed { idx, replacement ->
                EdgeVariant(
                    label = "edge-${idx + 1}",
                    code = code.replaceRange(literal.start, literal.endExclusive, replacement)
                )
            }
    }

    fun pairedEdgeVariants(
        learnerCode: String,
        referenceCode: String,
        maxVariants: Int = 3
    ): List<PairedEdgeVariant> {
        val learner = extractEditableLiterals(learnerCode)
        val reference = extractEditableLiterals(referenceCode)
        val pairIndex = (0 until minOf(learner.size, reference.size)).firstOrNull { idx ->
            learner[idx].kind == reference[idx].kind
        } ?: return emptyList()

        val learnerLiteral = learner[pairIndex]
        val referenceLiteral = reference[pairIndex]
        return edgeReplacements(referenceLiteral)
            .distinct()
            .filter { it != referenceLiteral.raw }
            .take(maxVariants)
            .mapIndexed { idx, replacement ->
                PairedEdgeVariant(
                    label = "edge-${idx + 1}",
                    learnerCode = learnerCode.replaceRange(
                        learnerLiteral.start,
                        learnerLiteral.endExclusive,
                        replacement
                    ),
                    referenceCode = referenceCode.replaceRange(
                        referenceLiteral.start,
                        referenceLiteral.endExclusive,
                        replacement
                    )
                )
            }
    }

    fun hiddenEvidence(passed: Boolean): String =
        if (passed) "숨은 경계조건을 통과했습니다."
        else "숨은 경계조건에서 실패했습니다. 입력값 자체는 공개하지 않습니다."

    private fun edgeReplacements(literal: EditableTestLiteral): List<String> = when (literal.kind) {
        EditableLiteralKind.STRING -> {
            val quote = literal.raw.firstOrNull() ?: '\''
            val content = literal.displayValue
            val duplicate = (content + content).take(96)
            val longValue = content.ifEmpty { "x" }.repeat(8).take(96)
            listOf(
                "$quote$quote",
                "$quote${escapeString(duplicate, quote)}$quote",
                "$quote${escapeString(longValue, quote)}$quote"
            )
        }
        EditableLiteralKind.NUMBER -> listOf("0", "-1", "999999")
        EditableLiteralKind.BOOLEAN -> {
            val upper = literal.raw.firstOrNull()?.isUpperCase() == true
            val isTrue = literal.raw.equals("true", ignoreCase = true)
            listOf(
                if (isTrue) {
                    if (upper) "False" else "false"
                } else {
                    if (upper) "True" else "true"
                }
            )
        }
    }

    private fun escapeString(value: String, quote: Char): String =
        value.replace("\\", "\\\\").replace(quote.toString(), "\\$quote")

    private fun isValidRange(code: String, literal: EditableTestLiteral): Boolean =
        literal.start >= 0 &&
            literal.endExclusive <= code.length &&
            literal.start < literal.endExclusive &&
            code.substring(literal.start, literal.endExclusive) == literal.raw

    private fun skipLine(code: String, start: Int): Int {
        val newline = code.indexOf('\n', start)
        return if (newline == -1) code.length else newline + 1
    }

    private fun isNumberStart(code: String, index: Int): Boolean {
        if (index !in code.indices) return false
        val c = code[index]
        val negative = c == '-' && index + 1 < code.length && code[index + 1].isDigit()
        val digit = c.isDigit()
        if (!negative && !digit) return false

        val previous = code.getOrNull(index - 1)
        if (previous != null && (previous.isLetterOrDigit() || previous == '_' || previous == '.')) return false

        val firstDigitIndex = if (negative) index + 1 else index
        val next = code.getOrNull(firstDigitIndex + 1)
        if (next != null && next.isLetter()) return false
        return true
    }

    private fun booleanAt(code: String, index: Int): String? {
        val options = listOf("True", "False", "true", "false")
        return options.firstOrNull { value ->
            code.regionMatches(index, value, 0, value.length) &&
                !isIdentifierChar(code.getOrNull(index - 1)) &&
                !isIdentifierChar(code.getOrNull(index + value.length))
        }
    }

    private fun isIdentifierChar(c: Char?): Boolean =
        c != null && (c.isLetterOrDigit() || c == '_')
}
