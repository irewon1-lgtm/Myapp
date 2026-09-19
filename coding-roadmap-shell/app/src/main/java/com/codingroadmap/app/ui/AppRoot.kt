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

@Composable
fun CodingRoadmapShell() {
    val context = LocalContext.current
    val catalog = remember { CatalogLoader.load(context) }
    val track = catalog.tracks.first()
    val store = remember { ReaderStore(context.applicationContext) }
    val prefs by store.state.collectAsState(initial = ReaderPrefs())
    val scope = rememberCoroutineScope()

    var screen by rememberSaveable { mutableStateOf("home") }
    var chapter by rememberSaveable { mutableIntStateOf(0) }

    fun openReader(index: Int) {
        chapter = index.coerceIn(0, track.chapters.lastIndex)
        screen = "reader"
    }

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
                catalog, prefs, { openReader(prefs.currentChapter) },
                { screen = "library" }, { screen = "saved" },
                { screen = "search" }, { screen = "settings" }
            )
            "library" -> LibraryScreen(
                track, prefs, { screen = "home" }, ::openReader,
                { screen = "home" }, { screen = "saved" }, { screen = "settings" }
            )
            "reader" -> ReaderShellScreen(
                track, chapter, prefs, { screen = "library" }, ::openReader,
                { scope.launch { store.visit(it) } },
                { scope.launch { store.toggleBookmark(it) } },
                { c, n -> scope.launch { store.saveNote(c, n) } },
                { scope.launch { store.setTextScale(it) } }
            )
            "saved" -> SavedScreen(
                track, prefs, ::openReader, { screen = "home" },
                { screen = "library" }, { screen = "settings" }
            )
            "search" -> SearchScreen(track, { screen = "home" }, ::openReader)
            "settings" -> SettingsScreen(
                prefs, { screen = "home" }, { screen = "library" },
                { screen = "saved" }, { scope.launch { store.setTextScale(it) } }
            )
        }
    }
}
