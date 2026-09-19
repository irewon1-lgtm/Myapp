package com.codingroadmap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.TrackMeta

@Composable
fun LibraryScreen(
    track: TrackMeta,
    prefs: ReaderPrefs,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onHome: () -> Unit,
    onSaved: () -> Unit,
    onSettings: () -> Unit
) {
    SystemBars(false)
    val trackIndex = (track.id - 1).coerceAtLeast(0)
    val visitedForTrack = prefs.visitedRefs.count { it.startsWith("$trackIndex:") }
    val current = if (prefs.currentTrack == trackIndex && track.chapters.isNotEmpty()) {
        prefs.currentChapter.coerceIn(0, track.chapters.lastIndex)
    } else 0
    val progress = if (track.chapters.isEmpty()) 0f
        else visitedForTrack / track.chapters.size.toFloat()

    Scaffold(
        containerColor = Ivory,
        bottomBar = { BottomNav("library", onHome, {}, onSaved, onSettings) }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = Ink)
                    }
                    Text(
                        "TRACK ${track.id.toString().padStart(2, '0')} · ${track.title}",
                        fontFamily = EditorialSerif,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                }
            }

            item { TrackSummary(track, progress) { onOpen(current) } }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "목차",
                        fontFamily = EditorialSerif,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "8개 챕터 · 48페이지\n개념부터 실습까지",
                        fontFamily = EditorialSerif,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Muted
                    )
                }
            }

            items(track.chapters.size) { index ->
                val visited = "${trackIndex}:$index" in prefs.visitedRefs
                ChapterRow(
                    index,
                    track.chapters[index],
                    visited = visited,
                    current = index == current && visited,
                    onClick = { onOpen(index) }
                )
            }
        }
    }
}
