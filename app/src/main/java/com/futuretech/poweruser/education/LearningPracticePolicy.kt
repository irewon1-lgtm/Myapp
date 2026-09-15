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

enum class MasteryStage(val label: String, val rank: Int) {
    SEE("봄", 1),
    UNDERSTAND("이해", 2),
    EXECUTE("실행", 3),
    APPLY("응용", 4),
    AI_COLLAB("AI 협업", 5),
    VERIFIABLE("검증 가능", 6);

    companion object {
        fun fromStorage(value: String?): MasteryStage? = entries.firstOrNull { it.name == value }
    }
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
    val stage: MasteryStage,
    val strength: MasteryEvidenceStrength,
    val maxHintLevel: Int,
    val completionPercentage: Int,
    val passed: Boolean
)

/**
 * Learning evidence policy.
 *
 * A completion percentage is not treated as mastery. The persisted stage describes
 * what the learner has actually demonstrated:
 * 봄 -> 이해 -> 실행 -> 응용 -> AI 협업 -> 검증 가능.
 *
 * Practice can prove up to AI_COLLAB. VERIFIABLE is reserved for an independent
 * Challenge pass with hints/AI/solution reveal disabled.
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
        val stage = when {
            mode == LearningSessionMode.CHALLENGE -> MasteryStage.VERIFIABLE
            safeHintLevel == 0 -> MasteryStage.AI_COLLAB
            safeHintLevel <= 2 -> MasteryStage.APPLY
            else -> MasteryStage.EXECUTE
        }
        return MasteryEvidence(
            mode = mode,
            stage = stage,
            strength = strength,
            maxHintLevel = safeHintLevel,
            completionPercentage = 100,
            passed = true
        )
    }

    fun progressStatus(evidence: MasteryEvidence): String = evidence.stage.name

    fun displayStage(status: String?): String = MasteryStage.fromStorage(status)?.label ?: MasteryStage.SEE.label

    /**
     * A later guided practice must not erase stronger evidence already earned.
     * A clean Challenge can upgrade a guided practice to VERIFIABLE.
     */
    fun strongerProgressStatus(existing: String?, candidate: String): String {
        val existingStage = MasteryStage.fromStorage(existing)
        val candidateStage = MasteryStage.fromStorage(candidate) ?: MasteryStage.SEE
        return if (existingStage != null && existingStage.rank >= candidateStage.rank) {
            existingStage.name
        } else {
            candidateStage.name
        }
    }
}
