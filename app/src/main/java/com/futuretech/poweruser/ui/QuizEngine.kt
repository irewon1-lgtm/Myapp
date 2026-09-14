package com.futuretech.poweruser.ui

data class FillBlankQuestion(
    val displayText: String,
    val acceptedAnswers: List<String>
)

/**
 * Existing curriculum text stores the answer inside square brackets, for example:
 * "앱과 서버 간 접속 규칙을 [ API ]라고 부릅니다."
 *
 * The UI must never show that answer before the learner submits an attempt.
 */
fun parseFillBlankPrompt(raw: String): FillBlankQuestion {
    val match = Regex("\\[(.+?)]").find(raw)
        ?: return FillBlankQuestion(raw, emptyList())

    val answers = match.groupValues[1]
        .split("/")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val display = raw.replaceRange(match.range, "[          ]")
    return FillBlankQuestion(display, answers)
}

fun normalizeQuizAnswer(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("\\s+"), "")
    .replace("-", "")
    .replace("_", "")

fun isFillBlankCorrect(input: String, acceptedAnswers: List<String>): Boolean {
    if (acceptedAnswers.isEmpty()) return false
    val normalizedInput = normalizeQuizAnswer(input)
    return acceptedAnswers.any { normalizeQuizAnswer(it) == normalizedInput }
}

private fun stripLineComment(line: String): String {
    val hashIndex = line.indexOf('#')
    val slashIndex = line.indexOf("//")
    val cutAt = listOf(hashIndex, slashIndex)
        .filter { it >= 0 }
        .minOrNull()
        ?: line.length
    return line.substring(0, cutAt)
}

fun canonicalizeCode(code: String): String = code
    .lines()
    .map { stripLineComment(it).trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n")
    .replace(Regex("\\s+"), "")

fun isDebugFixCorrect(input: String, expected: String): Boolean =
    canonicalizeCode(input) == canonicalizeCode(expected)
