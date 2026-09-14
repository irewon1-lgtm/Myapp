package com.futuretech.poweruser.ui

data class FillBlankQuestion(
    val displayText: String,
    val acceptedAnswers: List<String>
)

data class ExplanationEvaluation(
    val passed: Boolean,
    val matchedKeywords: List<String>,
    val missingKeywords: List<String>,
    val coverage: Float
)

/**
 * Curriculum text stores accepted fill-blank answers inside square brackets.
 * The learner-facing UI removes the answer until an attempt is submitted.
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

fun evaluateExplanation(input: String, keywords: List<String>): ExplanationEvaluation {
    val normalized = normalizeQuizAnswer(input)
    val matched = keywords.filter { normalized.contains(normalizeQuizAnswer(it)) }
    val missing = keywords.filterNot { it in matched }
    val coverage = if (keywords.isEmpty()) 0f else matched.size.toFloat() / keywords.size.toFloat()
    // A meaningful explanation must be more than a short keyword dump and cover at least half the concepts.
    val passed = input.trim().length >= 40 && matched.size >= 2 && coverage >= 0.5f
    return ExplanationEvaluation(passed, matched, missing, coverage)
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
