package com.futuretech.poweruser.education

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningPracticePolicyExtreme60Test {

    @Test
    fun extreme60_hint_mode_and_problem_count_scenarios() {
        var checked = 0

        repeat(60) { index ->
            val mode = if (index % 3 == 0) LearningSessionMode.CHALLENGE else LearningSessionMode.PRACTICE
            val requestedHintLevel = index % 7
            val evidence = LearningPracticePolicy.evidenceForSuccessfulSession(mode, requestedHintLevel)
            val flow = LearningPracticePolicy.regularLessonFlow

            assertTrue(
                "scenario $index: regular lessons must stay inside the 4-6 problem limit",
                flow.size in LearningPracticePolicy.MIN_REGULAR_LESSON_PROBLEMS..LearningPracticePolicy.MAX_REGULAR_LESSON_PROBLEMS
            )
            assertEquals("scenario $index: the focused lesson flow is six problems", 6, flow.size)
            assertEquals("scenario $index: problem types must not repeat", flow.size, flow.distinct().size)
            assertTrue("scenario $index: successful completion never loses completion percent", evidence.passed)
            assertEquals("scenario $index: hint use is not a score penalty", 100, evidence.completionPercentage)

            if (mode == LearningSessionMode.CHALLENGE) {
                assertFalse("scenario $index: challenge blocks hints", LearningPracticePolicy.hintsAllowed(mode))
                assertFalse("scenario $index: challenge blocks AI assistance", LearningPracticePolicy.aiAssistanceAllowed(mode))
                assertFalse("scenario $index: challenge blocks solution reveal", LearningPracticePolicy.solutionRevealAllowed(mode))
                assertEquals("scenario $index: challenge cannot carry hint evidence", 0, evidence.maxHintLevel)
                assertEquals(
                    "scenario $index: independent challenge pass is strong evidence",
                    MasteryEvidenceStrength.STRONG,
                    evidence.strength
                )
                assertEquals("scenario $index: challenge pass is verifiable", "VERIFIABLE", LearningPracticePolicy.progressStatus(evidence))
            } else {
                assertTrue("scenario $index: practice allows hints", LearningPracticePolicy.hintsAllowed(mode))
                assertTrue("scenario $index: practice allows AI assistance", LearningPracticePolicy.aiAssistanceAllowed(mode))
                assertTrue("scenario $index: practice allows solution reveal", LearningPracticePolicy.solutionRevealAllowed(mode))

                val clampedHint = requestedHintLevel.coerceIn(0, 3)
                assertEquals("scenario $index: practice records only the highest valid hint level", clampedHint, evidence.maxHintLevel)
                val expectedStrength = when (clampedHint) {
                    0 -> MasteryEvidenceStrength.STRONG
                    1, 2 -> MasteryEvidenceStrength.SUPPORTED
                    else -> MasteryEvidenceStrength.REVIEW_REQUIRED
                }
                assertEquals("scenario $index: hint changes evidence, not completion", expectedStrength, evidence.strength)

                val expectedStatus = when (expectedStrength) {
                    MasteryEvidenceStrength.STRONG -> "VERIFIABLE"
                    MasteryEvidenceStrength.SUPPORTED -> "APPLY"
                    MasteryEvidenceStrength.REVIEW_REQUIRED -> "EXECUTE"
                }
                assertEquals("scenario $index: mastery status follows evidence strength", expectedStatus, LearningPracticePolicy.progressStatus(evidence))
            }

            val preserved = LearningPracticePolicy.strongerProgressStatus("VERIFIABLE", LearningPracticePolicy.progressStatus(evidence))
            assertEquals("scenario $index: later guided practice must never erase stronger mastery", "VERIFIABLE", preserved)

            checked++
        }

        assertEquals(60, checked)
    }
}
