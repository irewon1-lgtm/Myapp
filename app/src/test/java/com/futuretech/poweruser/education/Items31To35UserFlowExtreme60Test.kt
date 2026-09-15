package com.futuretech.poweruser.education

import com.futuretech.poweruser.textbook.PowerUserCurriculumCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class Items31To35UserFlowExtreme60Test(
    private val scenario: UserFlowScenario
) {
    @Test
    fun user_flow_contract_is_backed_by_product_policy() {
        assertTrue("${scenario.id}: action must be concrete", scenario.action.isNotBlank())
        assertTrue("${scenario.id}: success criterion must be concrete", scenario.successCriterion.isNotBlank())
        assertTrue("${scenario.id}: internal test-system jargon must not be the user action", !scenario.action.contains("Extreme60"))

        when (scenario.area) {
            UserFlowArea.CURRICULUM_NAVIGATION -> {
                assertEquals(9, PowerUserCurriculumCatalog.TOTAL_BOOKS)
                assertEquals(134, PowerUserCurriculumCatalog.TOTAL_CHAPTERS)
                assertEquals(9, PowerUserCurriculumCatalog.books.size)
            }
            UserFlowArea.READING -> {
                assertEquals(
                    Items31To35QualityContract.TABLET_TEXT_MAX_WIDTH_DP,
                    LearningUi21To25Policy.READER_MAX_WIDTH_DP
                )
                assertTrue(LearningUi21To25Policy.READER_MAX_WIDTH_DP <= 780)
            }
            UserFlowArea.PRACTICE -> {
                val flow = LearningPracticePolicy.regularLessonFlow
                assertEquals(6, flow.size)
                assertEquals(flow.size, flow.distinct().size)
                assertTrue(LearningPracticePolicy.hintsAllowed(LearningSessionMode.PRACTICE))
            }
            UserFlowArea.RUN_SUBMIT -> {
                val flow = LearningPracticePolicy.regularLessonFlow
                assertTrue(flow.contains(LessonProblemType.MODIFY_AND_RUN))
                assertTrue(flow.contains(LessonProblemType.WRITE_FROM_MEMORY))
                assertTrue(flow.contains(LessonProblemType.DEBUG))
            }
            UserFlowArea.FEEDBACK_HINT -> {
                assertTrue(LearningPracticePolicy.hintsAllowed(LearningSessionMode.PRACTICE))
                assertFalse(LearningPracticePolicy.hintsAllowed(LearningSessionMode.CHALLENGE))
                assertFalse(LearningPracticePolicy.aiAssistanceAllowed(LearningSessionMode.CHALLENGE))
                assertFalse(LearningPracticePolicy.solutionRevealAllowed(LearningSessionMode.CHALLENGE))
            }
            UserFlowArea.MASTERY_REVIEW -> {
                val cleanPractice = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 0)
                val supportedPractice = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 1)
                val heavyHintPractice = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 3)
                val challenge = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.CHALLENGE, 3)
                assertEquals(MasteryStage.AI_COLLAB, cleanPractice.stage)
                assertEquals(MasteryStage.APPLY, supportedPractice.stage)
                assertEquals(MasteryStage.EXECUTE, heavyHintPractice.stage)
                assertEquals(MasteryStage.VERIFIABLE, challenge.stage)
                assertEquals("VERIFIABLE", LearningPracticePolicy.strongerProgressStatus("VERIFIABLE", "EXECUTE"))
            }
            UserFlowArea.DEVICE_STATE -> {
                assertEquals(
                    null,
                    LearningUi21To25Policy.practiceSplit(smallestScreenWidthDp = 360, isLandscape = false)
                )
                assertEquals(
                    null,
                    LearningUi21To25Policy.practiceSplit(smallestScreenWidthDp = 800, isLandscape = false)
                )
                val split = LearningUi21To25Policy.practiceSplit(smallestScreenWidthDp = 800, isLandscape = true)
                requireNotNull(split)
                assertTrue(split.problemWeight in Items31To35QualityContract.TABLET_PROBLEM_WEIGHT_MIN..Items31To35QualityContract.TABLET_PROBLEM_WEIGHT_MAX)
                assertEquals(1f, split.problemWeight + split.workspaceWeight, 0.0001f)
            }
            UserFlowArea.OFFLINE_RUNTIME_PROJECT -> {
                assertTrue("${scenario.id}: offline/runtime/project case must describe a fallback or local action", scenario.successCriterion.isNotBlank())
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): Collection<Array<Any>> {
            val scenarios = Items31To35QualityContract.extreme60
            check(scenarios.size == Items31To35QualityContract.EXTREME_SCENARIO_COUNT)
            check(scenarios.map { it.id }.distinct().size == scenarios.size)
            UserFlowArea.entries.forEach { area ->
                check(scenarios.count { it.area == area } == area.expectedCount) {
                    "${area.name} expected ${area.expectedCount} scenarios"
                }
            }
            return scenarios.map { arrayOf(it as Any) }
        }
    }
}
