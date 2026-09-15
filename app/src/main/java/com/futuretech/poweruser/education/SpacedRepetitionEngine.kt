package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.SpacedRepetitionItemEntity

enum class MemoryPriority(val label: String) {
    MUST_REMEMBER("반드시 기억"),
    UNDERSTAND("구조 이해"),
    AI_CAN_HELP("AI 보조 가능")
}

data class ReviewGuidance(
    val priority: MemoryPriority,
    val instruction: String,
    val stageLabel: String,
    val progressLabel: String
)

object SpacedRepetitionEngine {
    const val DAY_MS: Long = 86_400_000L
    const val HOUR_MS: Long = 3_600_000L
    const val RETRY_DELAY_MS: Long = 10 * 60 * 1000L
    const val REPEATED_RETRY_DELAY_MS: Long = 5 * 60 * 1000L
    const val SEVERE_RETRY_DELAY_MS: Long = 2 * 60 * 1000L
    const val TOTAL_REVIEW_SUCCESSES: Int = 6

    private val mustRememberTokens = listOf(
        "변수", "함수", "조건문", "반복문", "입력", "출력", "상태", "오류", "에러", "검증",
        "api", "http", "json", "sql", "database", "db", "git", "commit", "branch",
        "권한", "mfa", "passkey", "피싱", "보안"
    )

    private val aiAssistTokens = listOf(
        "보일러플레이트", "boilerplate", "정규식", "regex", "복잡한 css", "세부 명령", "저수준 설정"
    )

    fun createItem(
        conceptId: String,
        conceptTitle: String,
        category: String,
        definition: String,
        analogy: String,
        example: String,
        comparison: String,
        now: Long
    ): SpacedRepetitionItemEntity {
        return SpacedRepetitionItemEntity(
            conceptId = conceptId,
            conceptTitle = conceptTitle,
            category = category,
            definition = definition,
            analogy = analogy,
            example = example,
            comparison = comparison,
            reviewIntervalDays = 0,
            nextReviewTimestamp = now.coerceAtLeast(0L),
            reviewCount = 0,
            isMastered = false
        )
    }

    fun scheduleAfterLearning(
        current: SpacedRepetitionItemEntity,
        maxHintLevel: Int,
        now: Long
    ): SpacedRepetitionItemEntity {
        if (current.isMastered) return current
        val safeNow = now.coerceAtLeast(0L)
        val delay = when (maxHintLevel.coerceIn(0, 3)) {
            3 -> HOUR_MS
            2 -> 6 * HOUR_MS
            1 -> 12 * HOUR_MS
            else -> DAY_MS
        }
        return current.copy(
            reviewIntervalDays = if (delay >= DAY_MS) 1 else 0,
            nextReviewTimestamp = safeAdd(safeNow, delay),
            reviewCount = 0,
            isMastered = false
        )
    }

    fun recordReview(
        current: SpacedRepetitionItemEntity,
        remembered: Boolean,
        now: Long,
        failureStreak: Int = 1
    ): SpacedRepetitionItemEntity {
        if (current.isMastered) return current

        val safeNow = now.coerceAtLeast(0L)
        if (!remembered) {
            val regressedCount = (current.reviewCount - 1).coerceAtLeast(0)
            val retryDelay = when {
                failureStreak >= 3 -> SEVERE_RETRY_DELAY_MS
                failureStreak == 2 -> REPEATED_RETRY_DELAY_MS
                else -> RETRY_DELAY_MS
            }
            return current.copy(
                reviewIntervalDays = 0,
                nextReviewTimestamp = safeAdd(safeNow, retryDelay),
                reviewCount = regressedCount,
                isMastered = false
            )
        }

        val nextCount = (current.reviewCount + 1).coerceAtMost(TOTAL_REVIEW_SUCCESSES)
        if (nextCount >= TOTAL_REVIEW_SUCCESSES) {
            return current.copy(
                reviewIntervalDays = 60,
                nextReviewTimestamp = Long.MAX_VALUE,
                reviewCount = TOTAL_REVIEW_SUCCESSES,
                isMastered = true
            )
        }

        val intervalDays = nextIntervalDays(current.reviewIntervalDays, current.reviewCount)
        return current.copy(
            reviewIntervalDays = intervalDays,
            nextReviewTimestamp = safeAdd(safeNow, intervalDays.toLong() * DAY_MS),
            reviewCount = nextCount,
            isMastered = false
        )
    }

    private fun nextIntervalDays(currentIntervalDays: Int, reviewCount: Int): Int = when {
        reviewCount <= 0 || currentIntervalDays <= 0 -> 1
        currentIntervalDays <= 1 -> 3
        currentIntervalDays <= 3 -> 7
        currentIntervalDays <= 7 -> 14
        currentIntervalDays <= 14 -> 30
        else -> 60
    }

    fun stageLabel(item: SpacedRepetitionItemEntity): String = when {
        item.isMastered -> "장기 기억 확인 완료"
        item.reviewCount <= 0 && item.reviewIntervalDays <= 0 -> "빠른 재확인 예정"
        item.reviewIntervalDays <= 0 -> "오늘 다시 복습"
        else -> "${item.reviewIntervalDays}일 뒤 복습"
    }

    fun memoryPriority(item: SpacedRepetitionItemEntity): MemoryPriority {
        val text = listOf(item.conceptTitle, item.definition, item.comparison)
            .joinToString(" ")
            .lowercase()

        return when {
            aiAssistTokens.any { it in text } -> MemoryPriority.AI_CAN_HELP
            mustRememberTokens.any { it in text } -> MemoryPriority.MUST_REMEMBER
            else -> MemoryPriority.UNDERSTAND
        }
    }

    fun guidance(item: SpacedRepetitionItemEntity): ReviewGuidance {
        val priority = memoryPriority(item)
        val instruction = when (priority) {
            MemoryPriority.MUST_REMEMBER -> "핵심 뜻과 판단 기준은 보지 않고 말할 수 있게 기억합니다."
            MemoryPriority.UNDERSTAND -> "문장을 통째로 외우기보다 구조와 쓰임을 자기 말로 설명합니다."
            MemoryPriority.AI_CAN_HELP -> "세부 문법은 AI를 써도 되지만 결과를 읽고 검증하는 기준은 익힙니다."
        }
        return ReviewGuidance(
            priority = priority,
            instruction = instruction,
            stageLabel = stageLabel(item),
            progressLabel = "독립 성공 ${item.reviewCount.coerceIn(0, TOTAL_REVIEW_SUCCESSES)}/$TOTAL_REVIEW_SUCCESSES"
        )
    }

    fun isDue(item: SpacedRepetitionItemEntity, now: Long): Boolean {
        return !item.isMastered && item.nextReviewTimestamp <= now.coerceAtLeast(0L)
    }

    private fun safeAdd(base: Long, delta: Long): Long {
        if (delta <= 0L) return base
        return if (base > Long.MAX_VALUE - delta) Long.MAX_VALUE else base + delta
    }
}
