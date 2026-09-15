package com.futuretech.poweruser.data

import android.content.Context
import androidx.room.Room
import com.futuretech.poweruser.education.SpacedRepetitionEngine
import kotlinx.coroutines.flow.Flow

class AppRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "futuretech_poweruser.db"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.learningDao()

    fun getAllProgress(): Flow<List<LearningProgressEntity>> = dao.getAllProgress()

    suspend fun getProgressById(id: String): LearningProgressEntity? = dao.getProgressById(id)

    suspend fun updateProgress(
        lessonId: String,
        curriculumType: String,
        unitNumber: Int,
        title: String,
        status: String,
        completionPercentage: Int,
        isCompleted: Boolean
    ) {
        val entity = LearningProgressEntity(
            lessonId = lessonId,
            curriculumType = curriculumType,
            unitNumber = unitNumber,
            title = title,
            status = status,
            completionPercentage = completionPercentage,
            isCompleted = isCompleted,
            lastStudiedTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdateProgress(entity)
    }

    suspend fun recordErrorNote(
        errorType: String,
        codeSnippet: String,
        errorMessage: String,
        correctionGuide: String
    ) {
        val existing = dao.getErrorNoteByType(errorType)
        if (existing != null) {
            val updated = existing.copy(
                codeSnippet = codeSnippet,
                errorMessage = errorMessage,
                correctionGuide = correctionGuide,
                occurrenceCount = existing.occurrenceCount + 1,
                lastOccurredTimestamp = System.currentTimeMillis()
            )
            dao.insertOrUpdateErrorNote(updated)
        } else {
            val newNote = PersonalErrorNoteEntity(
                errorType = errorType,
                codeSnippet = codeSnippet,
                errorMessage = errorMessage,
                correctionGuide = correctionGuide,
                occurrenceCount = 1,
                lastOccurredTimestamp = System.currentTimeMillis()
            )
            dao.insertOrUpdateErrorNote(newNote)
        }
    }

    fun getAllErrorNotes(): Flow<List<PersonalErrorNoteEntity>> = dao.getAllErrorNotes()

    suspend fun addSpacedRepetitionItem(
        conceptId: String,
        conceptTitle: String,
        category: String,
        definition: String,
        analogy: String,
        example: String,
        comparison: String,
        now: Long = System.currentTimeMillis()
    ) {
        val existing = dao.getRepetitionById(conceptId)
        if (existing != null) return

        dao.insertOrUpdateRepetition(
            SpacedRepetitionEngine.createItem(
                conceptId = conceptId,
                conceptTitle = conceptTitle,
                category = category,
                definition = definition,
                analogy = analogy,
                example = example,
                comparison = comparison,
                now = now
            )
        )
    }

    suspend fun scheduleReviewAfterLearning(
        conceptId: String,
        maxHintLevel: Int,
        now: Long = System.currentTimeMillis()
    ) {
        val current = dao.getRepetitionById(conceptId) ?: return
        dao.insertOrUpdateRepetition(
            SpacedRepetitionEngine.scheduleAfterLearning(
                current = current,
                maxHintLevel = maxHintLevel,
                now = now
            )
        )
    }

    suspend fun getDueReviews(now: Long = System.currentTimeMillis()): List<SpacedRepetitionItemEntity> {
        dao.promoteLegacyFirstReviews(now)
        return dao.getDueReviews(now)
            .sortedWith(compareBy<SpacedRepetitionItemEntity> { it.nextReviewTimestamp }.thenBy { it.conceptTitle })
    }

    suspend fun recordReview(
        conceptId: String,
        remembered: Boolean,
        failureStreak: Int = 1,
        now: Long = System.currentTimeMillis()
    ) {
        val current = dao.getRepetitionById(conceptId) ?: return
        dao.insertOrUpdateRepetition(
            SpacedRepetitionEngine.recordReview(
                current = current,
                remembered = remembered,
                now = now,
                failureStreak = failureStreak
            )
        )
    }
}
