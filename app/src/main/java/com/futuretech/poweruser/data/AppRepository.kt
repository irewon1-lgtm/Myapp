package com.futuretech.poweruser.data

import android.content.Context
import androidx.room.Room
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
        comparison: String
    ) {
        val item = SpacedRepetitionItemEntity(
            conceptId = conceptId,
            conceptTitle = conceptTitle,
            category = category,
            definition = definition,
            analogy = analogy,
            example = example,
            comparison = comparison,
            reviewIntervalDays = 1,
            nextReviewTimestamp = System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000L),
            reviewCount = 0,
            isMastered = false
        )
        dao.insertOrUpdateRepetition(item)
    }

    suspend fun getDueReviews(): List<SpacedRepetitionItemEntity> {
        return dao.getDueReviews(System.currentTimeMillis() + (24 * 60 * 60 * 1000L))
    }
}
