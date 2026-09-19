package com.codingroadmap.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.codingroadmap.app.data.CatalogLoader
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.ReaderStore
import kotlinx.coroutines.launch

private const val READER_PAGES_PER_CHAPTER = 3

@Composable
fun CodingRoadmapShell() {
    val context = LocalContext.current
    val catalog = remember { CatalogLoader.load(context) }
    val store = remember { ReaderStore(context.applicationContext) }
    val prefs by store.state.collectAsState(initial = ReaderPrefs())
    val scope = rememberCoroutineScope()

    var screen by rememberSaveable { mutableStateOf("home") }
    var trackIndex by rememberSaveable { mutableIntStateOf(0) }
    var chapter by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(prefs.currentTrack) {
        if (screen == "home") {
            trackIndex = prefs.currentTrack.coerceIn(0, catalog.tracks.lastIndex)
        }
    }

    val track = catalog.tracks[trackIndex]

    fun openReader(targetTrack: Int, targetChapter: Int, targetPage: Int = 0) {
        val safeTrack = targetTrack.coerceIn(0, catalog.tracks.lastIndex)
        val target = catalog.tracks[safeTrack]
        if (target.chapters.isEmpty()) return
        trackIndex = safeTrack
        chapter = targetChapter.coerceIn(0, target.chapters.lastIndex)
        page = targetPage.coerceIn(0, READER_PAGES_PER_CHAPTER - 1)
        screen = "reader"
    }

    fun moveReader(delta: Int) {
        if (delta > 0) {
            when {
                page < READER_PAGES_PER_CHAPTER - 1 -> page += 1
                chapter < track.chapters.lastIndex -> {
                    chapter += 1
                    page = 0
                }
                else -> {
                    val nextTrack = (trackIndex + 1 until catalog.tracks.size)
                        .firstOrNull { catalog.tracks[it].chapters.isNotEmpty() }
                    if (nextTrack != null) {
                        trackIndex = nextTrack
                        chapter = 0
                        page = 0
                    }
                }
            }
        } else {
            when {
                page > 0 -> page -= 1
                chapter > 0 -> {
                    chapter -= 1
                    page = READER_PAGES_PER_CHAPTER - 1
                }
                else -> {
                    val prevTrack = (trackIndex - 1 downTo 0)
                        .firstOrNull { catalog.tracks[it].chapters.isNotEmpty() }
                    if (prevTrack != null) {
                        trackIndex = prevTrack
                        chapter = catalog.tracks[prevTrack].chapters.lastIndex
                        page = READER_PAGES_PER_CHAPTER - 1
                    }
                }
            }
        }
    }

    fun canMoveNext(): Boolean =
        page < READER_PAGES_PER_CHAPTER - 1 ||
            chapter < track.chapters.lastIndex ||
            (trackIndex + 1 until catalog.tracks.size).any { catalog.tracks[it].chapters.isNotEmpty() }

    fun canMovePrev(): Boolean =
        page > 0 ||
            chapter > 0 ||
            (trackIndex - 1 downTo 0).any { catalog.tracks[it].chapters.isNotEmpty() }

    BackHandler(enabled = screen != "home") {
        screen = when (screen) {
            "reader" -> "library"
            else -> "home"
        }
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "shell-screen"
    ) { current ->
        when (current) {
            "home" -> HomeScreen(
                catalog = catalog,
                prefs = prefs,
                onContinue = {
                    val ti = prefs.currentTrack.coerceIn(0, catalog.tracks.lastIndex)
                    val t = catalog.tracks[ti]
                    val ci = if (prefs.visitedRefs.isEmpty() && ti == 0) 3 else prefs.currentChapter
                    val pi = if (prefs.visitedRefs.isEmpty() && ti == 0) 0 else prefs.currentPage
                    openReader(ti, ci.coerceIn(0, t.chapters.lastIndex), pi)
                },
                onLibrary = { screen = "library" },
                onTrack = { ti ->
                    trackIndex = ti.coerceIn(0, catalog.tracks.lastIndex)
                    chapter = 0
                    page = 0
                    screen = "library"
                },
                onSaved = { screen = "saved" },
                onSearch = { screen = "search" },
                onSettings = { screen = "settings" }
            )
            "library" -> LibraryScreen(
                track = track,
                prefs = prefs,
                onBack = { screen = "home" },
                onOpen = { openReader(trackIndex, it, 0) },
                onHome = { screen = "home" },
                onSaved = { screen = "saved" },
                onSettings = { screen = "settings" }
            )
            "reader" -> ReaderShellScreen(
                track = track,
                trackIndex = trackIndex,
                chapter = chapter,
                page = page,
                pageCount = READER_PAGES_PER_CHAPTER,
                prefs = prefs,
                onBack = { screen = "library" },
                canPrev = canMovePrev(),
                canNext = canMoveNext(),
                onPrev = { moveReader(-1) },
                onNext = { moveReader(1) },
                onVisit = { c, p -> scope.launch { store.visit(trackIndex, c, p) } },
                onBookmark = { scope.launch { store.toggleBookmark(it) } },
                onSaveNote = { c, n -> scope.launch { store.saveNote(c, n) } },
                onTextScale = { scope.launch { store.setTextScale(it) } }
            )
            "saved" -> SavedScreen(
                track, prefs, { openReader(trackIndex, it, 0) }, { screen = "home" },
                { screen = "library" }, { screen = "settings" }
            )
            "search" -> SearchScreen(track, { screen = "home" }) { openReader(trackIndex, it, 0) }
            "settings" -> SettingsScreen(
                prefs, { screen = "home" }, { screen = "library" },
                { screen = "saved" }, { scope.launch { store.setTextScale(it) } }
            )
        }
    }
}
