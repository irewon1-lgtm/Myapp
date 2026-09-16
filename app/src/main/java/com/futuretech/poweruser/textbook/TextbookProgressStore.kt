package com.futuretech.poweruser.textbook

import android.content.Context

/**
 * V3 uses a separate preference namespace because the learner-facing textbook was rewritten and
 * regrouped again. Older v1/v2 progress is intentionally left untouched on-device so legacy
 * completion or lesson positions cannot falsely mark the deep-beginner V3 as completed.
 */
class TextbookProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("v3_deep_beginner_progress", Context.MODE_PRIVATE)

    fun selectedChapterId(): String? = prefs.getString("selected_track", null)

    fun saveSelectedChapter(id: String) {
        prefs.edit().putString("selected_track", id).apply()
    }

    fun selectedSectionIndex(chapterId: String): Int =
        prefs.getInt("selected_lesson_$chapterId", 0).coerceAtLeast(0)

    fun saveSelectedSectionIndex(chapterId: String, sectionIndex: Int) {
        prefs.edit().putInt("selected_lesson_$chapterId", sectionIndex.coerceAtLeast(0)).apply()
    }

    fun selectedConceptIndex(sectionId: String): Int =
        prefs.getInt("selected_recall_$sectionId", 0).coerceAtLeast(0)

    fun saveSelectedConceptIndex(sectionId: String, conceptIndex: Int) {
        prefs.edit().putInt("selected_recall_$sectionId", conceptIndex.coerceAtLeast(0)).apply()
    }

    /** Exact e-book page restore point for the current concept/lesson. */
    fun selectedPageIndex(conceptId: String): Int =
        prefs.getInt("reader_page_$conceptId", 0).coerceAtLeast(0)

    fun saveSelectedPageIndex(conceptId: String, pageIndex: Int) {
        // A page turn is infrequent and writes one Int. Commit synchronously so an immediate app
        // close cannot leave the learner on the previous page when the book is reopened.
        prefs.edit().putInt("reader_page_$conceptId", pageIndex.coerceAtLeast(0)).commit()
    }

    fun markReadComplete(id: String) {
        val updated = readCompletedIds().toMutableSet().apply { add(id) }
        prefs.edit().putStringSet("track_read_completed", updated).apply()
    }

    fun isReadComplete(id: String): Boolean = id in readCompletedIds()

    fun readCompletedIds(): Set<String> =
        prefs.getStringSet("track_read_completed", emptySet())?.toSet().orEmpty()

    /**
     * Legacy scroll restore is kept for compatibility with older builds/tests. The paged reader no
     * longer writes these keys, but preserving them avoids destructive migration behavior.
     */
    fun saveScroll(id: String, index: Int, offset: Int) {
        prefs.edit()
            .putInt("scroll_index_$id", index.coerceAtLeast(0))
            .putInt("scroll_offset_$id", offset.coerceAtLeast(0))
            .apply()
    }

    fun scrollIndex(id: String): Int = prefs.getInt("scroll_index_$id", 0).coerceAtLeast(0)
    fun scrollOffset(id: String): Int = prefs.getInt("scroll_offset_$id", 0).coerceAtLeast(0)
}
