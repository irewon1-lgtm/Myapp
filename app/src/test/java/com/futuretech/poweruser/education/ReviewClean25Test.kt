package com.futuretech.poweruser.education

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReviewClean25Test(private val gate: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "review-clean-{0}")
        fun gates(): Collection<Array<Any>> = (1..25).map { arrayOf<Any>(it) }
    }

    @Test
    fun cleanGatePasses() {
        val start = 1_710_000_000_000L + gate * 100_000L
        var now = start
        var item = SpacedRepetitionEngine.createItem(
            conceptId = "clean-$gate",
            conceptTitle = when (gate % 3) {
                0 -> "함수와 입력 출력"
                1 -> "전체 데이터 흐름"
                else -> "보일러플레이트 세부 명령"
            },
            category = if (gate <= 12) "BEGINNER" else "INTERMEDIATE",
            definition = "CLEAN $gate 검증용 정의로 구조와 쓰임을 설명한다.",
            analogy = "CLEAN 비유 $gate",
            example = "result = $gate",
            comparison = "헷갈리는 개념과의 차이 $gate",
            now = now
        )

        assertTrue(SpacedRepetitionEngine.isDue(item, now))
        assertEquals(0, item.reviewIntervalDays)

        val successes = (gate - 1) % 5
        repeat(successes) {
            item = SpacedRepetitionEngine.recordReview(item, true, now)
            now = item.nextReviewTimestamp
        }

        if (gate % 4 == 0 && !item.isMastered) {
            val before = item.reviewCount
            item = SpacedRepetitionEngine.recordReview(item, false, now)
            assertEquals((before - 1).coerceAtLeast(0), item.reviewCount)
            assertEquals(0, item.reviewIntervalDays)
            assertFalse(item.isMastered)
        } else if (!item.isMastered) {
            item = SpacedRepetitionEngine.recordReview(item, true, now)
        }

        assertTrue(item.reviewCount in 0..5)
        assertTrue(item.reviewIntervalDays in setOf(0, 1, 3, 7, 14))
        assertEquals(item.isMastered, item.reviewCount == 5)
        assertTrue(SpacedRepetitionEngine.guidance(item).instruction.isNotBlank())

        if (item.isMastered) {
            assertEquals(Long.MAX_VALUE, item.nextReviewTimestamp)
        } else {
            assertTrue(item.nextReviewTimestamp >= start)
        }
    }
}
