package com.futuretech.poweruser.education

enum class LearningSessionMode(val label: String) {
    PRACTICE("연습"),
    CHALLENGE("Chapter Challenge")
}

enum class LessonProblemType {
    PREDICT_OUTPUT,
    MODIFY_AND_RUN,
    FILL_BLANK,
    WRITE_FROM_MEMORY,
    DEBUG,
    VERIFY_AI_ANSWER
}

enum class MasteryEvidenceStrength(
    val label: String,
    val needsReview: Boolean
) {
    STRONG("강한 숙련 증거", false),
    SUPPORTED("보조를 사용한 숙련 증거", false),
    REVIEW_REQUIRED("복습 필요", true)
}

data class MasteryEvidence(
    val mode: LearningSessionMode,
    val strength: MasteryEvidenceStrength,
    val maxHintLevel: Int,
    val completionPercentage: Int,
    val passed: Boolean
)

/**
 * Product policy for items 13-15 of the learning-flow refactor.
 *
 * 13) Hints never subtract points or completion. They only reduce the strength of
 *     the mastery evidence produced by a successful practice session.
 * 14) Practice and Chapter Challenge are separate modes. Practice allows hints,
 *     AI assistance and retries; Challenge exposes none of those assists.
 * 15) A regular lesson ends with exactly six short, varied problems (within the
 *     4-6 product limit), not a long ten-question exam.
 */
object LearningPracticePolicy {
    const val MIN_REGULAR_LESSON_PROBLEMS = 4
    const val MAX_REGULAR_LESSON_PROBLEMS = 6

    val regularLessonFlow: List<LessonProblemType> = listOf(
        LessonProblemType.PREDICT_OUTPUT,
        LessonProblemType.MODIFY_AND_RUN,
        LessonProblemType.FILL_BLANK,
        LessonProblemType.WRITE_FROM_MEMORY,
        LessonProblemType.DEBUG,
        LessonProblemType.VERIFY_AI_ANSWER
    )

    fun hintsAllowed(mode: LearningSessionMode): Boolean =
        mode == LearningSessionMode.PRACTICE

    fun aiAssistanceAllowed(mode: LearningSessionMode): Boolean =
        mode == LearningSessionMode.PRACTICE

    fun solutionRevealAllowed(mode: LearningSessionMode): Boolean =
        mode == LearningSessionMode.PRACTICE

    fun evidenceForSuccessfulSession(
        mode: LearningSessionMode,
        maxHintLevel: Int
    ): MasteryEvidence {
        val safeHintLevel = if (mode == LearningSessionMode.CHALLENGE) 0 else maxHintLevel.coerceIn(0, 3)
        val strength = when {
            mode == LearningSessionMode.CHALLENGE -> MasteryEvidenceStrength.STRONG
            safeHintLevel == 0 -> MasteryEvidenceStrength.STRONG
            safeHintLevel <= 2 -> MasteryEvidenceStrength.SUPPORTED
            else -> MasteryEvidenceStrength.REVIEW_REQUIRED
        }
        return MasteryEvidence(
            mode = mode,
            strength = strength,
            maxHintLevel = safeHintLevel,
            completionPercentage = 100,
            passed = true
        )
    }

    fun progressStatus(evidence: MasteryEvidence): String = when (evidence.strength) {
        MasteryEvidenceStrength.STRONG -> "VERIFIABLE"
        MasteryEvidenceStrength.SUPPORTED -> "APPLY"
        MasteryEvidenceStrength.REVIEW_REQUIRED -> "EXECUTE"
    }

    /**
     * A later guided practice must not erase stronger evidence already earned.
     * A clean Challenge can upgrade a guided practice to VERIFIABLE.
     */
    fun strongerProgressStatus(existing: String?, candidate: String): String {
        val existingRank = progressStatusRank(existing)
        val candidateRank = progressStatusRank(candidate)
        return if (existingRank >= candidateRank && existing != null) existing else candidate
    }

    private fun progressStatusRank(status: String?): Int = when (status) {
        "SEE" -> 1
        "UNDERSTAND" -> 2
        "EXECUTE" -> 3
        "APPLY" -> 4
        "AI_COLLAB" -> 5
        "VERIFIABLE" -> 6
        else -> 0
    }
}
