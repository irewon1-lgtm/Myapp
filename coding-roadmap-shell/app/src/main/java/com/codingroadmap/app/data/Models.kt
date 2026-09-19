package com.codingroadmap.app.data

data class TrackMeta(
    val id: Int,
    val title: String,
    val subtitle: String,
    val available: Boolean,
    val chapters: List<String>
)

data class Catalog(val tracks: List<TrackMeta>)

data class ReaderPrefs(
    val currentTrack: Int = 0,
    val currentChapter: Int = 0,
    val currentPage: Int = 0,
    val visitedChapters: Set<Int> = emptySet(),
    val visitedRefs: Set<String> = emptySet(),
    val bookmarks: Set<Int> = emptySet(),
    val notes: Map<Int, String> = emptyMap(),
    val textScale: Float = 1f,
    val pageModelVersion: Int = 1
)
