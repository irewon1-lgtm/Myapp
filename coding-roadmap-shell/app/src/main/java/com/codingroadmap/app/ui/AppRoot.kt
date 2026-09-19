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
import com.codingroadmap.app.data.Track1ContentLoader
import kotlinx.coroutines.launch

@Composable
fun CodingRoadmapShell() {
    val context = LocalContext.current
    val catalog = remember { CatalogLoader.load(context) }
    val track1Content = remember { Track1ContentLoader.load(context) }
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

    fun pageCountFor(ti: Int, ci: Int): Int =
        if (ti == 0 && ci in track1Content.indices) track1Content[ci].pages.size else 0

    fun openReader(targetTrack: Int, targetChapter: Int, targetPage: Int = 0) {
        val safeTrack = targetTrack.coerceIn(0, catalog.tracks.lastIndex)
        val target = catalog.tracks[safeTrack]
        if (target.chapters.isEmpty()) return

        val safeChapter = targetChapter.coerceIn(0, target.chapters.lastIndex)
        val count = pageCountFor(safeTrack, safeChapter)
        if (count <= 0) {
            trackIndex = safeTrack
            chapter = safeChapter
            page = 0
            screen = "library"
            scope.launch { store.setPosition(safeTrack, safeChapter, 0) }
            return
        }

        trackIndex = safeTrack
        chapter = safeChapter
        page = targetPage.coerceIn(0, count - 1)
        screen = "reader"
    }

    fun currentPageCount(): Int = pageCountFor(trackIndex, chapter)

    fun moveReader(delta: Int) {
        val currentCount = currentPageCount()
        if (currentCount <= 0) return

        if (delta > 0) {
            when {
                page < currentCount - 1 -> page += 1
                chapter < track.chapters.lastIndex -> {
                    chapter += 1
                    page = 0
                }
                else -> {
                    val nextTrack = (trackIndex + 1 until catalog.tracks.size)
                        .firstOrNull {
                            catalog.tracks[it].available &&
                                catalog.tracks[it].chapters.isNotEmpty()
                        }
                    if (nextTrack != null) {
                        trackIndex = nextTrack
                        chapter = 0
                        page = 0
                        screen = "library"
                        scope.launch { store.setPosition(nextTrack, 0, 0) }
                    }
                }
            }
        } else {
            when {
                page > 0 -> page -= 1
                chapter > 0 -> {
                    chapter -= 1
                    page = pageCountFor(trackIndex, chapter).coerceAtLeast(1) - 1
                }
                else -> Unit
            }
        }
    }

    fun canMoveNext(): Boolean {
        val count = currentPageCount()
        return count > 0 && (
            page < count - 1 ||
                chapter < track.chapters.lastIndex ||
                (trackIndex + 1 until catalog.tracks.size).any {
                    catalog.tracks[it].available &&
                        catalog.tracks[it].chapters.isNotEmpty()
                }
            )
    }

    fun canMovePrev(): Boolean =
        page > 0 || chapter > 0

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
                    if (ti == 0) {
                        val target = catalog.tracks[0]
                        openReader(
                            0,
                            prefs.currentChapter.coerceIn(0, target.chapters.lastIndex),
                            prefs.currentPage
                        )
                    } else {
                        trackIndex = ti
                        chapter = 0
                        page = 0
                        screen = "library"
                    }
                },
                onLibrary = {
                    trackIndex = prefs.currentTrack.coerceIn(0, catalog.tracks.lastIndex)
                    screen = "library"
                },
                onTrack = { ti ->
                    val safe = ti.coerceIn(0, catalog.tracks.lastIndex)
                    val target = catalog.tracks[safe]
                    if (target.available && target.chapters.isNotEmpty()) {
                        trackIndex = safe
                        chapter = 0
                        page = 0
                        screen = "library"
                    }
                },
                onSaved = { screen = "saved" },
                onSearch = { screen = "search" },
                onSettings = { screen = "settings" }
            )

            "library" -> LibraryScreen(
                track = track,
                prefs = prefs,
                contentReady = trackIndex == 0,
                onBack = { screen = "home" },
                onOpen = { ci ->
                    if (trackIndex == 0) openReader(0, ci, 0)
                },
                onHome = { screen = "home" },
                onSaved = { screen = "saved" },
                onSettings = { screen = "settings" }
            )

            "reader" -> {
                val chapterContent = track1Content[chapter]
                val pageCount = chapterContent.pages.size
                val pageData = chapterContent.pages[page.coerceIn(0, pageCount - 1)]

                ReaderShellScreen(
                    track = track,
                    trackIndex = trackIndex,
                    chapter = chapter,
                    page = page,
                    pageCount = pageCount,
                    pageData = pageData,
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
            }

            "saved" -> SavedScreen(
                catalog.tracks.first(),
                prefs,
                { openReader(0, it, 0) },
                { screen = "home" },
                {
                    trackIndex = 0
                    screen = "library"
                },
                { screen = "settings" }
            )

            "search" -> SearchScreen(
                catalog.tracks.first(),
                { screen = "home" }
            ) { openReader(0, it, 0) }

            "settings" -> SettingsScreen(
                prefs,
                { screen = "home" },
                {
                    trackIndex = prefs.currentTrack.coerceIn(0, catalog.tracks.lastIndex)
                    screen = "library"
                },
                { screen = "saved" },
                { scope.launch { store.setTextScale(it) } }
            )
        }
    }
}
