package com.codingroadmap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.roadmapDataStore by preferencesDataStore("roadmap_reader")

class ReaderStore(private val context: Context) {
    private object Keys {
        val current = intPreferencesKey("current_chapter")
        val visited = stringSetPreferencesKey("visited_chapters")
        val bookmarks = stringSetPreferencesKey("bookmarks")
        val notes = stringPreferencesKey("notes_json")
        val textScale = floatPreferencesKey("text_scale")
    }

    val state: Flow<ReaderPrefs> = context.roadmapDataStore.data.map { p ->
        ReaderPrefs(
            currentChapter = p[Keys.current] ?: 0,
            visitedChapters = (p[Keys.visited] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet(),
            bookmarks = (p[Keys.bookmarks] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet(),
            notes = decodeNotes(p[Keys.notes]),
            textScale = (p[Keys.textScale] ?: 1f).coerceIn(.9f, 1.35f)
        )
    }

    suspend fun visit(chapter: Int) = context.roadmapDataStore.edit { p ->
        p[Keys.current] = chapter
        val s = (p[Keys.visited] ?: emptySet()).toMutableSet()
        s += chapter.toString()
        p[Keys.visited] = s
    }

    suspend fun toggleBookmark(chapter: Int) = context.roadmapDataStore.edit { p ->
        val s = (p[Keys.bookmarks] ?: emptySet()).toMutableSet()
        if (!s.add(chapter.toString())) s.remove(chapter.toString())
        p[Keys.bookmarks] = s
    }

    suspend fun saveNote(chapter: Int, note: String) = context.roadmapDataStore.edit { p ->
        val map = decodeNotes(p[Keys.notes]).toMutableMap()
        if (note.isBlank()) map.remove(chapter) else map[chapter] = note.trim()
        p[Keys.notes] = encodeNotes(map)
    }

    suspend fun setTextScale(value: Float) = context.roadmapDataStore.edit { p ->
        p[Keys.textScale] = value.coerceIn(.9f, 1.35f)
    }

    private fun decodeNotes(raw: String?): Map<Int, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().mapNotNull { key -> key.toIntOrNull()?.let { it to o.getString(key) } }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun encodeNotes(notes: Map<Int, String>): String = JSONObject().apply {
        notes.forEach { (k, v) -> put(k.toString(), v) }
    }.toString()
}
