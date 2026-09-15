package com.futuretech.poweruser.education

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningPracticePolicyClean25Test {

    @Test
    fun clean25_policy_gates() {
        val flow = LearningPracticePolicy.regularLessonFlow
        val noHint = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 0)
        val hint1 = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 1)
        val hint2 = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 2)
        val hint3 = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 3)
        val challenge = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.CHALLENGE, 3)

        val checks = listOf(
            "01 minimum problem count" to (flow.size >= LearningPracticePolicy.MIN_REGULAR_LESSON_PROBLEMS),
            "02 maximum problem count" to (flow.size <= LearningPracticePolicy.MAX_REGULAR_LESSON_PROBLEMS),
            "03 focused flow is exactly six" to (flow.size == 6),
            "04 no repeated problem type" to (flow.distinct().size == flow.size),
            "05 predict is present" to flow.contains(LessonProblemType.PREDICT_OUTPUT),
            "06 modify and run is present" to flow.contains(LessonProblemType.MODIFY_AND_RUN),
            "07 fill blank is present" to flow.contains(LessonProblemType.FILL_BLANK),
            "08 write from memory is present" to flow.contains(LessonProblemType.WRITE_FROM_MEMORY),
            "09 debug is present" to flow.contains(LessonProblemType.DEBUG),
            "10 AI answer verification is present" to flow.contains(LessonProblemType.VERIFY_AI_ANSWER),
            "11 practice allows hints" to LearningPracticePolicy.hintsAllowed(LearningSessionMode.PRACTICE),
            "12 challenge blocks hints" to !LearningPracticePolicy.hintsAllowed(LearningSessionMode.CHALLENGE),
            "13 practice allows AI" to LearningPracticePolicy.aiAssistanceAllowed(LearningSessionMode.PRACTICE),
            "14 challenge blocks AI" to !LearningPracticePolicy.aiAssistanceAllowed(LearningSessionMode.CHALLENGE),
            "15 practice allows solution reveal" to LearningPracticePolicy.solutionRevealAllowed(LearningSessionMode.PRACTICE),
            "16 challenge blocks solution reveal" to !LearningPracticePolicy.solutionRevealAllowed(LearningSessionMode.CHALLENGE),
            "17 no-hint success is strong" to (noHint.strength == MasteryEvidenceStrength.STRONG),
            "18 hint1 success is supported" to (hint1.strength == MasteryEvidenceStrength.SUPPORTED),
            "19 hint2 success is supported" to (hint2.strength == MasteryEvidenceStrength.SUPPORTED),
            "20 hint3 success needs review" to (hint3.strength == MasteryEvidenceStrength.REVIEW_REQUIRED),
            "21 hint3 explicitly needs review" to hint3.strength.needsReview,
            "22 no-hint completion remains 100" to (noHint.completionPercentage == 100),
            "23 hint3 completion remains 100" to (hint3.completionPercentage == 100),
            "24 challenge strips hint evidence and stays strong" to (challenge.maxHintLevel == 0 && challenge.strength == MasteryEvidenceStrength.STRONG),
            "25 stronger mastery cannot be downgraded" to (LearningPracticePolicy.strongerProgressStatus("VERIFIABLE", "EXECUTE") == "VERIFIABLE")
        )

        assertEquals("CLEAN25 must contain exactly 25 gates", 25, checks.size)
        checks.forEach { (name, passed) ->
            assertTrue("CLEAN25 failed: $name", passed)
        }
    }
}
