package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.SpacedRepetitionItemEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReviewExtreme60Test(private val caseId: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "review-extreme-{0}")
        fun cases(): Collection<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
    }

    @Test
    fun extremeSchedulingScenarioPasses() {
        val stage = (caseId - 1) % SpacedRepetitionEngine.TOTAL_REVIEW_SUCCESSES
        val remembered = caseId % 3 != 0
        val normalNow = 1_700_000_000_000L + caseId * 1_234_567L
        val now = if (caseId % 15 == 0) Long.MAX_VALUE - 500L else normalNow
        val item = itemAtStage(stage, now)
        val failureStreak = if (remembered) 0 else (caseId % 3) + 1
        val result = SpacedRepetitionEngine.recordReview(item, remembered, now, failureStreak)
        val guidance = SpacedRepetitionEngine.guidance(result)

        assertTrue(result.reviewCount in 0..SpacedRepetitionEngine.TOTAL_REVIEW_SUCCESSES)
        assertTrue(result.reviewIntervalDays in setOf(0, 1, 3, 7, 14, 30, 60))
        assertTrue(result.nextReviewTimestamp >= now || result.nextReviewTimestamp == Long.MAX_VALUE)
        assertTrue(guidance.stageLabel.isNotBlank())
        assertTrue(guidance.progressLabel.isNotBlank())
        assertTrue(guidance.instruction.length >= 20)

        if (!remembered) {
            assertFalse(result.isMastered)
            assertEquals(0, result.reviewIntervalDays)
            assertTrue(result.reviewCount <= stage)
        } else if (stage == SpacedRepetitionEngine.TOTAL_REVIEW_SUCCESSES - 1) {
            assertTrue(result.isMastered)
            assertEquals(SpacedRepetitionEngine.TOTAL_REVIEW_SUCCESSES, result.reviewCount)
            assertEquals(60, result.reviewIntervalDays)
            assertEquals(Long.MAX_VALUE, result.nextReviewTimestamp)
        } else {
            assertFalse(result.isMastered)
            assertEquals(stage + 1, result.reviewCount)
        }

        if (result.isMastered) assertFalse(SpacedRepetitionEngine.isDue(result, Long.MAX_VALUE))
    }

    private fun itemAtStage(stage: Int, now: Long): SpacedRepetitionItemEntity {
        val title = when (caseId % 3) {
            0 -> "API와 HTTP 검증"
            1 -> "클라우드 구조 이해"
            else -> "복잡한 CSS 세부 명령"
        }
        val intervals = listOf(0, 1, 3, 7, 14, 30)
        return SpacedRepetitionItemEntity(
            conceptId = "case-$caseId",
            conceptTitle = title,
            category = if (caseId % 2 == 0) "BEGINNER" else "INTERMEDIATE",
            definition = "극한 시나리오 $caseId 에서 개념의 구조와 판단 기준을 확인한다.",
            analogy = "비유 $caseId",
            example = "value = $caseId",
            comparison = "비교 기준 $caseId",
            reviewIntervalDays = intervals[stage],
            nextReviewTimestamp = now,
            reviewCount = stage,
            isMastered = false
        )
    }
}
