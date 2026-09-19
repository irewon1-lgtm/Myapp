package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.Catalog
import com.codingroadmap.app.data.ReaderPrefs

@Composable
fun HomeScreen(
    catalog: Catalog,
    prefs: ReaderPrefs,
    onContinue: () -> Unit,
    onLibrary: () -> Unit,
    onSaved: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    SystemBars(false)
    val active = catalog.tracks.first()
    val progress = if (active.chapters.isEmpty()) 0f else prefs.visitedChapters.size / active.chapters.size.toFloat()

    Scaffold(
        containerColor = Ivory,
        bottomBar = { BottomNav("home", {}, onLibrary, onSaved, onSettings) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("코딩 로드맵", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                        Text("오늘도 조금씩 이어가 볼까요?", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                    IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, "검색", tint = Ink) }
                }
            }
            item { ContinueCard(active, prefs.currentChapter, progress, onContinue) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("내 서재", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                        Text("차근차근, 나만의 속도로.", color = Muted, fontSize = 13.sp)
                    }
                    Text("전체보기  ›", color = Gold, modifier = Modifier.clickable(onClick = onLibrary))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    catalog.tracks.forEach { track ->
                        BookCard(track, if (track.id == 1) progress else 0f) {
                            if (track.available) onLibrary()
                        }
                    }
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFF0E8DC)).padding(20.dp)
                ) {
                    Column {
                        Text("작은 배움이 쌓여", color = Muted, fontSize = 12.sp)
                        Text("더 큰 가능성이 됩니다.", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                        Text("겉은 따뜻한 서재, 읽을 때는 눈부심을 줄인 다크 리더로 설계했습니다.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
    }
}
