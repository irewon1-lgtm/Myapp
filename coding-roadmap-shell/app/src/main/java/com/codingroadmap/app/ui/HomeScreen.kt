package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
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
    onTrack: (Int) -> Unit,
    onSaved: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    SystemBars(false)

    val requestedIndex = prefs.currentTrack.coerceIn(0, catalog.tracks.lastIndex)
    val activeIndex = if (
        catalog.tracks[requestedIndex].available &&
        catalog.tracks[requestedIndex].chapters.isNotEmpty()
    ) requestedIndex else 0
    val active = catalog.tracks[activeIndex]

    fun progressFor(trackIndex: Int): Float {
        val track = catalog.tracks[trackIndex]
        if (track.chapters.isEmpty()) return 0f
        val seen = prefs.visitedRefs.count { it.startsWith("$trackIndex:") }
        return seen / track.chapters.size.toFloat()
    }

    val activeProgress = progressFor(activeIndex)
    val activeChapter = if (active.chapters.isEmpty()) 0
        else prefs.currentChapter.coerceIn(0, active.chapters.lastIndex)

    Scaffold(
        containerColor = Ivory,
        bottomBar = { BottomNav("home", {}, onLibrary, onSaved, onSettings) }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "코딩 로드맵",
                            fontFamily = EditorialSerif,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )
                        Text(
                            "오늘도, 더 나은 개발자가 되어볼까요?",
                            fontFamily = EditorialSerif,
                            fontSize = 15.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Rounded.NotificationsNone, "알림", tint = Ink)
                        }
                        Text(
                            "좋은 코드는\n더 좋은 가능성을\n만듭니다.  —",
                            fontFamily = EditorialSerif,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            color = Muted
                        )
                    }
                }
            }

            item { ContinueCard(active, activeChapter, activeProgress, onContinue) }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "학습 트랙",
                            fontFamily = EditorialSerif,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )
                        Text(
                            "지금은 TRACK 01을 먼저 완성합니다.",
                            fontFamily = EditorialSerif,
                            fontSize = 13.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Text(
                        "전체보기  ›",
                        fontFamily = EditorialSerif,
                        fontSize = 14.sp,
                        color = Muted,
                        modifier = Modifier.clickable(onClick = onLibrary).padding(bottom = 3.dp)
                    )
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 48.dp)
                ) {
                    itemsIndexed(catalog.tracks) { index, track ->
                        BookCard(track, progressFor(index)) {
                            if (track.available) onTrack(index)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "내 서재",
                            fontFamily = EditorialSerif,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink
                        )
                        Text(
                            "지금의 배움이, 언젠가 더 큰 나를 만듭니다.",
                            fontFamily = EditorialSerif,
                            fontSize = 13.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Text(
                        "전체보기  ›",
                        fontFamily = EditorialSerif,
                        fontSize = 14.sp,
                        color = Muted,
                        modifier = Modifier.clickable(onClick = onLibrary).padding(bottom = 3.dp)
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF3EBDD))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LeafMark(Modifier.size(54.dp), Gold)
                    Column(Modifier.padding(start = 15.dp).weight(1f)) {
                        Text("꾸준히 배우는", fontFamily = EditorialSerif, fontSize = 13.sp, color = Muted)
                        Text(
                            "당신이, 이미 특별합니다.",
                            fontFamily = EditorialSerif,
                            fontSize = 18.sp,
                            color = Ink,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Box(Modifier.width(1.dp).height(45.dp).background(Color(0xFFD7C9B4)))
                    Text(
                        "Good Code\nA Kinder Tomorrow\n—",
                        fontFamily = EditorialSerif,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = Muted,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            }
        }
    }
}
