package com.codingroadmap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    val progress = if (track.chapters.isEmpty()) 0f else prefs.visitedChapters.size / track.chapters.size.toFloat()
    Scaffold(
        containerColor = Ivory,
        bottomBar = { BottomNav("library", onHome, {}, onSaved, onSettings) }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = Ink) }
                    Text("TRACK 01 · ${track.title}", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            item { TrackSummary(track, progress) { onOpen(prefs.currentChapter) } }
            item {
                Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text("목차", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("콘텐츠 본문은 다음 단계에서 연결합니다.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
            items(track.chapters.size) { index ->
                ChapterRow(
                    index = index,
                    title = track.chapters[index],
                    visited = index in prefs.visitedChapters,
                    current = index == prefs.currentChapter,
                    onClick = { onOpen(index) }
                )
            }
        }
    }
}
