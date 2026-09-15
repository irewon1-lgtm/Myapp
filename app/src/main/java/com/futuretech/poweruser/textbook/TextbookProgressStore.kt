package com.futuretech.poweruser.textbook

import android.content.Context

class TextbookProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("v1_textbook_progress", Context.MODE_PRIVATE)

    fun selectedChapterId(): String? = prefs.getString("selected_chapter", null)

    fun saveSelectedChapter(id: String) {
        prefs.edit().putString("selected_chapter", id).apply()
    }

    fun selectedSectionIndex(chapterId: String): Int =
        prefs.getInt("selected_section_$chapterId", 0).coerceAtLeast(0)

    fun saveSelectedSectionIndex(chapterId: String, sectionIndex: Int) {
        prefs.edit().putInt("selected_section_$chapterId", sectionIndex.coerceAtLeast(0)).apply()
    }

    fun markReadComplete(id: String) {
        val updated = readCompletedIds().toMutableSet().apply { add(id) }
        prefs.edit().putStringSet("read_completed", updated).apply()
    }

    fun isReadComplete(id: String): Boolean = id in readCompletedIds()

    fun readCompletedIds(): Set<String> = prefs.getStringSet("read_completed", emptySet())?.toSet().orEmpty()

    fun saveScroll(id: String, index: Int, offset: Int) {
        prefs.edit()
            .putInt("scroll_index_$id", index.coerceAtLeast(0))
            .putInt("scroll_offset_$id", offset.coerceAtLeast(0))
            .apply()
    }

    fun scrollIndex(id: String): Int = prefs.getInt("scroll_index_$id", 0).coerceAtLeast(0)
    fun scrollOffset(id: String): Int = prefs.getInt("scroll_offset_$id", 0).coerceAtLeast(0)
}
