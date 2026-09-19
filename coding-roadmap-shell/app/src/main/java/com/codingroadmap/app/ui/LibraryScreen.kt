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
    contentReady: Boolean,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onHome: () -> Unit,
    onSaved: () -> Unit,
    onSettings: () -> Unit
) {
    SystemBars(false)
    val trackIndex = (track.id - 1).coerceAtLeast(0)
    val visitedForTrack = prefs.visitedRefs.count { it.startsWith("$trackIndex:") }
    val current = if (
        prefs.currentTrack == trackIndex &&
        track.chapters.isNotEmpty()
    ) {
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

            item {
                TrackSummary(
                    track = track,
                    progress = progress,
                    contentReady = contentReady,
                    onContinue = { onOpen(current) }
                )
            }

            item { TrackDescriptionPanel(track, contentReady) }

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
                        if (track.id == 1)
                            "8개 챕터 · 64페이지\n단어집부터 실습까지"
                        else
                            "8개 챕터\nTRACK 01 다음 단계",
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
                    index = index,
                    title = track.chapters[index],
                    visited = visited,
                    current = contentReady && index == current && visited,
                    enabled = contentReady,
                    onClick = { onOpen(index) }
                )
            }

            if (!contentReady) {
                item {
                    Text(
                        "TRACK 02의 챕터 순서와 자동 연결은 복구했습니다. 이번 버전은 TRACK 01 내용·단어집·페이지 밀도 개선에 집중했으며, TRACK 02 본문은 기존 v0.7.0 내용을 다음 단계에서 같은 형식으로 연결할 수 있게 분리해 두었습니다.",
                        fontFamily = EditorialSerif,
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
                    )
                }
            }
        }
    }
}
