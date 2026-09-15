package com.futuretech.poweruser.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val lessonId: String,
    val curriculumType: String, // BEGINNER or INTERMEDIATE
    val unitNumber: Int,
    val title: String,
    val status: String, // SEE, UNDERSTAND, EXECUTE, APPLY, AI_COLLAB, VERIFIABLE
    val completionPercentage: Int,
    val isCompleted: Boolean,
    val lastStudiedTimestamp: Long
)

@Entity(tableName = "spaced_repetition")
data class SpacedRepetitionItemEntity(
    @PrimaryKey val conceptId: String,
    val conceptTitle: String,
    val category: String,
    val definition: String,
    val analogy: String,
    val example: String,
    val comparison: String,
    val reviewIntervalDays: Int, // 0(today), 1, 3, 7, 14
    val nextReviewTimestamp: Long,
    val reviewCount: Int,
    val isMastered: Boolean
)

@Entity(tableName = "error_notes")
data class PersonalErrorNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val errorType: String, // INDENTATION, JSON_SYNTAX, SQL_WHERE, API_KEY_MISSING, etc.
    val codeSnippet: String,
    val errorMessage: String,
    val correctionGuide: String,
    val occurrenceCount: Int,
    val lastOccurredTimestamp: Long
)

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_progress")
    fun getAllProgress(): Flow<List<LearningProgressEntity>>

    @Query("SELECT * FROM learning_progress WHERE lessonId = :id")
    suspend fun getProgressById(id: String): LearningProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: LearningProgressEntity)

    @Query("SELECT * FROM spaced_repetition WHERE nextReviewTimestamp <= :currentTimestamp AND isMastered = 0")
    suspend fun getDueReviews(currentTimestamp: Long): List<SpacedRepetitionItemEntity>

    @Query("SELECT COUNT(*) FROM spaced_repetition WHERE nextReviewTimestamp <= :currentTimestamp AND isMastered = 0")
    fun observeDueReviewCount(currentTimestamp: Long): Flow<Int>

    @Query("SELECT * FROM spaced_repetition WHERE conceptId = :conceptId LIMIT 1")
    suspend fun getRepetitionById(conceptId: String): SpacedRepetitionItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRepetition(item: SpacedRepetitionItemEntity)

    @Query(
        """UPDATE spaced_repetition
            SET reviewIntervalDays = 0, nextReviewTimestamp = :currentTimestamp
            WHERE reviewCount = 0
              AND reviewIntervalDays = 1
              AND isMastered = 0
              AND nextReviewTimestamp > :currentTimestamp"""
    )
    suspend fun promoteLegacyFirstReviews(currentTimestamp: Long): Int

    @Query("SELECT * FROM error_notes ORDER BY occurrenceCount DESC")
    fun getAllErrorNotes(): Flow<List<PersonalErrorNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateErrorNote(note: PersonalErrorNoteEntity)

    @Query("SELECT * FROM error_notes WHERE errorType = :type LIMIT 1")
    suspend fun getErrorNoteByType(type: String): PersonalErrorNoteEntity?
}

@Database(
    entities = [LearningProgressEntity::class, SpacedRepetitionItemEntity::class, PersonalErrorNoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
}
