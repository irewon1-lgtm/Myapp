package com.futuretech.poweruser.education

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Items31To35VisualClean25Test {
    @Test
    fun clean25_contract_has_exactly_25_unique_gates() {
        val gates = Items31To35QualityContract.clean25
        assertEquals(25, gates.size)
        assertEquals(25, gates.map { it.id }.distinct().size)
        assertEquals((1..25).map { "C%02d".format(it) }, gates.map { it.id })
    }

    @Test
    fun adaptive_layout_policy_satisfies_core_visual_gates() {
        assertEquals(600, LearningUi21To25Policy.TABLET_BREAKPOINT_DP)
        assertEquals(780, LearningUi21To25Policy.READER_MAX_WIDTH_DP)
        assertEquals(0.40f, LearningUi21To25Policy.TABLET_PROBLEM_WEIGHT, 0.0001f)
        assertTrue(
            LearningUi21To25Policy.TABLET_PROBLEM_WEIGHT in
                Items31To35QualityContract.TABLET_PROBLEM_WEIGHT_MIN..Items31To35QualityContract.TABLET_PROBLEM_WEIGHT_MAX
        )
        assertEquals(0.60f, LearningUi21To25Policy.TABLET_WORKSPACE_WEIGHT, 0.0001f)
        assertEquals(null, LearningUi21To25Policy.practiceSplit(360, false))
        assertEquals(null, LearningUi21To25Policy.practiceSplit(800, false))
        assertTrue(LearningUi21To25Policy.practiceSplit(800, true) != null)
    }

    @Test
    fun mastery_is_stage_based_not_score_based() {
        val cleanPractice = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 0)
        val guidedPractice = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.PRACTICE, 3)
        val challenge = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.CHALLENGE, 0)

        assertEquals(100, cleanPractice.completionPercentage)
        assertEquals(100, guidedPractice.completionPercentage)
        assertEquals(MasteryStage.AI_COLLAB, cleanPractice.stage)
        assertEquals(MasteryStage.EXECUTE, guidedPractice.stage)
        assertEquals(MasteryStage.VERIFIABLE, challenge.stage)
        assertFalse("guided practice must not be called verifiable", guidedPractice.stage == MasteryStage.VERIFIABLE)
    }

    @Test
    fun every_clean_gate_is_a_zero_tolerance_or_explicit_contract() {
        val text = Items31To35QualityContract.clean25.joinToString("\n") { it.requirement }
        listOf(
            "360dp", "본문 폭", "버튼 겹침", "형광", "장문", "48dp", "플래시", "상태",
            "38~42%", "단일 컬럼", "drawer", "색상만", "실행과 제출", "홈", "점수"
        ).forEach { keyword ->
            assertTrue("missing CLEAN25 concept: $keyword", text.contains(keyword))
        }
    }
}
