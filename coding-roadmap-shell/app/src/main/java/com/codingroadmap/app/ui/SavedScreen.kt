package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs
import com.codingroadmap.app.data.TrackMeta

@Composable
fun SavedScreen(
    track: TrackMeta,
    prefs: ReaderPrefs,
    onOpen: (Int) -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onSettings: () -> Unit
) {
    SystemBars(false)
    val saved = (prefs.bookmarks + prefs.notes.keys).sorted()
    Scaffold(
        containerColor = Ivory,
        bottomBar = { BottomNav("saved", onHome, onLibrary, {}, onSettings) }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                Text("북마크", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
                Text("다시 보고 싶은 순간들을 모아두는 공간입니다.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            }
            if (saved.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 30.dp).clip(RoundedCornerShape(24.dp)).background(IvoryCard).padding(24.dp)
                    ) {
                        Column {
                            Icon(Icons.Rounded.Bookmark, null, tint = Gold)
                            Text("아직 저장한 항목이 없습니다.", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp))
                            Text("리더에서 북마크나 메모를 추가하면 여기에 모입니다.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
                        }
                    }
                }
            } else {
                items(saved.size) { i ->
                    val chapter = saved[i]
                    val title = track.chapters.getOrNull(chapter) ?: "저장된 챕터"
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(IvoryCard).clickable { onOpen(chapter) }.padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (chapter in prefs.bookmarks) Icons.Rounded.Bookmark else Icons.Rounded.EditNote, null, tint = Gold)
                        Column(Modifier.padding(start = 13.dp).weight(1f)) {
                            Text("Chapter ${chapter + 1} · $title", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            val note = prefs.notes[chapter]
                            Text(note?.takeIf { it.isNotBlank() } ?: "북마크됨", color = Muted, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
