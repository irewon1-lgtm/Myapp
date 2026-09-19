package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.TrackMeta

@Composable
fun ReaderShellScreen(
    track: TrackMeta,
    trackIndex: Int,
    chapter: Int,
    page: Int,
    pageCount: Int,
    prefs: ReaderPrefs,
    onBack: () -> Unit,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onVisit: (Int, Int) -> Unit,
    onBookmark: (Int) -> Unit,
    onSaveNote: (Int, String) -> Unit,
    onTextScale: (Float) -> Unit
) {
    SystemBars(true)
    val title = track.chapters[chapter]
    var showDisplay by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var focusMode by rememberSaveable { mutableStateOf(false) }
    var note by remember(chapter, prefs.notes[chapter]) { mutableStateOf(prefs.notes[chapter].orEmpty()) }
    var drag by remember(trackIndex, chapter, page) { mutableFloatStateOf(0f) }

    LaunchedEffect(trackIndex, chapter, page) { onVisit(chapter, page) }

    Column(
        Modifier
            .fillMaxSize()
            .background(ReaderBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (!focusMode) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "목차", tint = ReaderText)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "TRACK ${track.id.toString().padStart(2, '0')} · ${track.title}",
                        fontFamily = EditorialSerif,
                        color = ReaderMuted,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Text(
                        "Chapter ${chapter + 1} · $title",
                        fontFamily = EditorialSerif,
                        color = ReaderText,
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { onBookmark(chapter) }) {
                    Icon(
                        if (chapter in prefs.bookmarks) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        "북마크",
                        tint = if (chapter in prefs.bookmarks) ReaderGold else ReaderText
                    )
                }
                IconButton(onClick = { showNote = true }) {
                    Icon(
                        Icons.Rounded.EditNote,
                        "메모",
                        tint = if (prefs.notes[chapter].isNullOrBlank()) ReaderText else ReaderGold
                    )
                }
                TextButton(onClick = { showDisplay = true }) {
                    Text("Aa", fontFamily = EditorialSerif, color = ReaderText, fontSize = 18.sp)
                }
            }
        }

        ReaderBody(
            trackId = track.id,
            title = title,
            chapter = chapter,
            chapterCount = track.chapters.size,
            page = page,
            pageCount = pageCount,
            scale = prefs.textScale,
            canPrev = canPrev,
            canNext = canNext,
            focusMode = focusMode,
            onToggleFocus = { focusMode = !focusMode },
            modifier = Modifier
                .weight(1f)
                .pointerInput(trackIndex, chapter, page, canPrev, canNext, focusMode) {
                    detectTapGestures { offset ->
                        when {
                            offset.x <= size.width * 0.18f && canPrev -> onPrev()
                            offset.x >= size.width * 0.82f && canNext -> onNext()
                            else -> focusMode = !focusMode
                        }
                    }
                }
                .pointerInput(trackIndex, chapter, page, canPrev, canNext) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = {
                            if (drag > 90f && canPrev) onPrev()
                            if (drag < -90f && canNext) onNext()
                            drag = 0f
                        },
                        onHorizontalDrag = { _, amount -> drag += amount }
                    )
                },
            onPrev = onPrev,
            onNext = onNext
        )
    }

    ReaderDialogs(
        showDisplay,
        showNote,
        prefs.textScale,
        note,
        { note = it },
        { showDisplay = false },
        { showNote = false },
        onTextScale,
        {
            onSaveNote(chapter, note)
            showNote = false
        }
    )
}
