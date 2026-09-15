package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.SpacedRepetitionItemEntity
import org.junit.Assert.*
import org.junit.Test

class SpacedRepetitionEngineTest {
    private val baseNow = 1_700_000_000_000L

    private fun newItem(
        title: String = "변수와 상태",
        definition: String = "변수는 값을 이름으로 저장하는 기본 개념이다.",
        comparison: String = "상수와 변수의 차이를 구분한다."
    ): SpacedRepetitionItemEntity = SpacedRepetitionEngine.createItem(
        conceptId = "B05-01",
        conceptTitle = title,
        category = "BEGINNER",
        definition = definition,
        analogy = "라벨이 붙은 상자처럼 생각한다.",
        example = "score = 10",
        comparison = comparison,
        now = baseNow
    )

    @Test
    fun newCardIsDueImmediately() {
        val item = newItem()
        assertEquals(0, item.reviewIntervalDays)
        assertEquals(baseNow, item.nextReviewTimestamp)
        assertEquals(0, item.reviewCount)
        assertFalse(item.isMastered)
        assertTrue(SpacedRepetitionEngine.isDue(item, baseNow))
    }

    @Test
    fun successfulReviewsFollowTodayOneThreeSevenFourteenThenMastery() {
        var now = baseNow
        var item = newItem()
        val expectedIntervals = listOf(1, 3, 7, 14)

        expectedIntervals.forEachIndexed { index, expectedDays ->
            item = SpacedRepetitionEngine.recordReview(item, remembered = true, now = now)
            assertEquals(index + 1, item.reviewCount)
            assertEquals(expectedDays, item.reviewIntervalDays)
            assertFalse(item.isMastered)
            assertEquals(now + expectedDays * SpacedRepetitionEngine.DAY_MS, item.nextReviewTimestamp)
            now = item.nextReviewTimestamp
        }

        item = SpacedRepetitionEngine.recordReview(item, remembered = true, now = now)
        assertEquals(SpacedRepetitionEngine.TOTAL_REVIEW_SUCCESSES, item.reviewCount)
        assertTrue(item.isMastered)
        assertEquals(Long.MAX_VALUE, item.nextReviewTimestamp)
    }

    @Test
    fun forgottenCardMovesBackwardAndRetriesSameDay() {
        var item = newItem()
        item = SpacedRepetitionEngine.recordReview(item, true, baseNow)
        item = SpacedRepetitionEngine.recordReview(item, true, item.nextReviewTimestamp)
        assertEquals(2, item.reviewCount)

        val failNow = item.nextReviewTimestamp
        item = SpacedRepetitionEngine.recordReview(item, false, failNow)
        assertEquals(1, item.reviewCount)
        assertEquals(0, item.reviewIntervalDays)
        assertEquals(failNow + SpacedRepetitionEngine.RETRY_DELAY_MS, item.nextReviewTimestamp)
        assertFalse(item.isMastered)
    }

    @Test
    fun failureAtFirstReviewNeverCreatesNegativeStage() {
        val item = SpacedRepetitionEngine.recordReview(newItem(), false, baseNow)
        assertEquals(0, item.reviewCount)
        assertEquals(0, item.reviewIntervalDays)
        assertFalse(item.isMastered)
    }

    @Test
    fun masteredCardIsIdempotent() {
        var item = newItem()
        var now = baseNow
        repeat(5) {
            item = SpacedRepetitionEngine.recordReview(item, true, now)
            now = item.nextReviewTimestamp
        }
        val snapshot = item
        assertTrue(snapshot.isMastered)
        assertEquals(snapshot, SpacedRepetitionEngine.recordReview(snapshot, false, baseNow + 1234L))
        assertEquals(snapshot, SpacedRepetitionEngine.recordReview(snapshot, true, baseNow + 5678L))
    }

    @Test
    fun timestampMathNeverOverflows() {
        val nearMax = Long.MAX_VALUE - 1_000L
        val item = SpacedRepetitionEngine.createItem(
            conceptId = "overflow",
            conceptTitle = "overflow",
            category = "INTERMEDIATE",
            definition = "overflow guard",
            analogy = "guard",
            example = "",
            comparison = "",
            now = nearMax
        )
        val remembered = SpacedRepetitionEngine.recordReview(item, true, nearMax)
        val forgotten = SpacedRepetitionEngine.recordReview(item, false, nearMax)
        assertEquals(Long.MAX_VALUE, remembered.nextReviewTimestamp)
        assertEquals(Long.MAX_VALUE, forgotten.nextReviewTimestamp)
    }

    @Test
    fun mustRememberConceptsAreMarkedExplicitly() {
        assertEquals(MemoryPriority.MUST_REMEMBER, SpacedRepetitionEngine.memoryPriority(newItem()))
    }

    @Test
    fun aiAssistConceptsDoNotPretendEverythingMustBeMemorized() {
        val item = newItem(
            title = "복잡한 CSS 세부 명령",
            definition = "세부 문법은 AI에게 초안을 맡길 수 있다.",
            comparison = "보일러플레이트와 핵심 구조를 구분한다."
        )
        assertEquals(MemoryPriority.AI_CAN_HELP, SpacedRepetitionEngine.memoryPriority(item))
    }

    @Test
    fun ordinaryConceptDefaultsToUnderstanding() {
        val item = newItem(
            title = "클라우드 서비스 흐름",
            definition = "여러 서비스가 연결되는 전체 구조를 이해한다.",
            comparison = "로컬 실행과 원격 실행을 구분한다."
        )
        assertEquals(MemoryPriority.UNDERSTAND, SpacedRepetitionEngine.memoryPriority(item))
    }
}
