package com.futuretech.poweruser.education

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningUi21To25PolicyTest {
    @Test
    fun guidanceFadesFromExampleToIndependentVariation() {
        assertEquals(
            listOf(
                GuidancePhase.WORKED_EXAMPLE,
                GuidancePhase.NEAR_TRANSFER,
                GuidancePhase.PARTIAL_SUPPORT,
                GuidancePhase.INDEPENDENT,
                GuidancePhase.VARIATION
            ),
            LearningUi21To25Policy.guidanceOrder
        )
    }

    @Test
    fun tabletLandscapeUsesFortySixtySplitOnly() {
        val split = LearningUi21To25Policy.practiceSplit(
            smallestScreenWidthDp = 800,
            isLandscape = true
        )
        requireNotNull(split)
        assertEquals(0.40f, split.problemWeight, 0.001f)
        assertEquals(0.60f, split.workspaceWeight, 0.001f)
        assertEquals(1.0f, split.problemWeight + split.workspaceWeight, 0.001f)
    }

    @Test
    fun phoneAndTabletPortraitStaySingleColumn() {
        assertNull(LearningUi21To25Policy.practiceSplit(411, isLandscape = true))
        assertNull(LearningUi21To25Policy.practiceSplit(800, isLandscape = false))
        assertEquals(listOf("문제", "코드", "결과"), LearningUi21To25Policy.phonePracticeOrder)
    }

    @Test
    fun readerWidthRemainsBookLike() {
        assertTrue(LearningUi21To25Policy.READER_MAX_WIDTH_DP in 680..820)
    }
}
