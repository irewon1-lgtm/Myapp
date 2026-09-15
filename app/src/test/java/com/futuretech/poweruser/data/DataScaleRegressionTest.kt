package com.futuretech.poweruser.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataScaleRegressionTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: LearningDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.learningDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun reviewQueueHandles100DueItemsWithoutDroppingOrDuplicating() = runBlocking {
        val now = 1_800_000_000_000L
        repeat(100) { index ->
            dao.insertOrUpdateRepetition(
                SpacedRepetitionItemEntity(
                    conceptId = "review-$index",
                    conceptTitle = "복습 $index",
                    category = "BEGINNER",
                    definition = "정의 $index",
                    analogy = "",
                    example = "",
                    comparison = "",
                    reviewIntervalDays = 0,
                    nextReviewTimestamp = now - index,
                    reviewCount = 0,
                    isMastered = false
                )
            )
        }

        val due = dao.getDueReviews(now)
        assertEquals(100, due.size)
        assertEquals(100, due.map { it.conceptId }.toSet().size)
    }

    @Test
    fun learningProgressPersistsMoreThan100Lessons() = runBlocking {
        repeat(150) { index ->
            dao.insertOrUpdateProgress(
                LearningProgressEntity(
                    lessonId = "lesson-$index",
                    curriculumType = if (index < 75) "BEGINNER" else "INTERMEDIATE",
                    unitNumber = index / 10,
                    title = "레슨 $index",
                    status = "UNDERSTAND",
                    completionPercentage = index % 101,
                    isCompleted = index % 3 == 0,
                    lastStudiedTimestamp = 1_800_000_000_000L + index
                )
            )
        }

        val all = dao.getAllProgress().first()
        assertEquals(150, all.size)
        assertTrue(all.any { it.lessonId == "lesson-149" })
    }

    @Test
    fun databaseKeepsLargeAccumulatedErrorHistory() = runBlocking {
        repeat(1_000) { index ->
            dao.insertOrUpdateErrorNote(
                PersonalErrorNoteEntity(
                    errorType = "TYPE-$index",
                    codeSnippet = "print($index)",
                    errorMessage = "error-$index",
                    correctionGuide = "guide-$index",
                    occurrenceCount = (index % 9) + 1,
                    lastOccurredTimestamp = 1_800_000_000_000L + index
                )
            )
        }

        val all = dao.getAllErrorNotes().first()
        assertEquals(1_000, all.size)
        assertTrue(all.first().occurrenceCount >= all.last().occurrenceCount)
    }
}
