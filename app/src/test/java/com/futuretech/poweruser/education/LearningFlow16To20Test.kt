package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.PersonalErrorNoteEntity
import com.futuretech.poweruser.data.SpacedRepetitionItemEntity
import org.junit.Assert.*
import org.junit.Test

class LearningFlow16To20Test {

    private fun note(type: String, count: Int): PersonalErrorNoteEntity = PersonalErrorNoteEntity(
        id = count.toLong(),
        errorType = type,
        codeSnippet = "sample",
        errorMessage = type,
        correctionGuide = "fix $type",
        occurrenceCount = count,
        lastOccurredTimestamp = 1_700_000_000_000L + count
    )

    @Test
    fun challengeHasTenMixedProblemsWithoutAdjacentDuplicates() {
        val flow = ErrorDrivenPracticeEngine.challengeFlow(emptyList())
        assertEquals(10, flow.size)
        assertTrue(flow.zipWithNext().all { (a, b) -> a != b })
        assertTrue(LessonProblemType.PREDICT_OUTPUT in flow)
        assertTrue(LessonProblemType.MODIFY_AND_RUN in flow)
        assertTrue(LessonProblemType.WRITE_FROM_MEMORY in flow)
        assertTrue(LessonProblemType.DEBUG in flow)
        assertTrue(LessonProblemType.VERIFY_AI_ANSWER in flow)
    }

    @Test
    fun frequentRealErrorGetsMoreChallengeSlots() {
        val baseline = ErrorDrivenPracticeEngine.challengeFlow(emptyList())
        val weighted = ErrorDrivenPracticeEngine.challengeFlow(
            listOf(note("FOCUSED_DEBUG", 8), note("FOCUSED_FILL", 1))
        )
        assertTrue(weighted.count { it == LessonProblemType.DEBUG } >= baseline.count { it == LessonProblemType.DEBUG })
        assertTrue(weighted.zipWithNext().all { (a, b) -> a != b })
    }

    @Test
    fun reviewVariantChangesInputInsteadOfRepeatingSameCard() {
        val item = SpacedRepetitionItemEntity(
            conceptId = "x",
            conceptTitle = "변수",
            category = "BEGINNER",
            definition = "값을 이름으로 저장한다.",
            analogy = "상자",
            example = "score = 10\nprint(score)",
            comparison = "상수와 구분",
            reviewIntervalDays = 3,
            nextReviewTimestamp = 0L,
            reviewCount = 2,
            isMastered = false
        )
        val variation = ReviewVariationEngine.forItem(item)
        assertNotNull(variation.changedExample)
        assertNotEquals(item.example, variation.changedExample)
        assertTrue(variation.prompt.contains("변형"))
    }

    @Test
    fun beginnerProjectUnlocksOneStageAtATime() {
        val s1 = ProjectStageEngine.beginnerStages(emptySet())
        assertTrue(s1[0].unlocked)
        assertFalse(s1[1].unlocked)

        val s2 = ProjectStageEngine.beginnerStages(setOf("B03-01"))
        assertTrue(s2[1].unlocked)
        assertFalse(s2[2].unlocked)

        val all = ProjectStageEngine.beginnerStages(setOf("B03-01", "B04-01", "B05-01"))
        assertTrue(all.all { it.unlocked })
    }

    @Test
    fun intermediateProjectUsesChapterGates() {
        val stages = ProjectStageEngine.intermediateStages(setOf("I01-01", "I02-01"))
        assertTrue(stages[0].unlocked)
        assertTrue(stages[1].unlocked)
        assertTrue(stages[2].unlocked)
        assertFalse(stages[3].unlocked)
    }
}
