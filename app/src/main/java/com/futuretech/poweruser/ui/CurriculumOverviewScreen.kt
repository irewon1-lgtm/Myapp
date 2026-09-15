package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.CurriculumBook
import com.futuretech.poweruser.textbook.CurriculumChapterRef
import com.futuretech.poweruser.textbook.PowerUserCurriculumCatalog

private val CurriculumBg = Color(0xFF101216)
private val CurriculumCard = Color(0xFF171A20)
private val CurriculumBorder = Color(0xFF2B3038)
private val CurriculumText = Color(0xFFF2F0EA)
private val CurriculumMuted = Color(0xFFA7ADB7)
private val CurriculumAccent = Color(0xFF8FB3FF)

@Composable
fun CurriculumOverviewScreen(
    onOpenChapter: (String) -> Unit
) {
    var expandedBookId by rememberSaveable { mutableStateOf("V1") }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("curriculum_overview_root"),
        color = CurriculumBg
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = "전체 과정",
                        color = CurriculumText,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${PowerUserCurriculumCatalog.TOTAL_BOOKS}권 · ${PowerUserCurriculumCatalog.TOTAL_CHAPTERS} Chapter",
                        modifier = Modifier.testTag("curriculum_chapter_count"),
                        color = CurriculumAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "권 → Chapter → Section → 문제·실습 순서로 학습합니다.",
                        color = CurriculumMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
            }

            items(PowerUserCurriculumCatalog.books, key = { it.id }) { book ->
                BookCard(
                    book = book,
                    expanded = expandedBookId == book.id,
                    onToggle = { expandedBookId = if (expandedBookId == book.id) "" else book.id },
                    onOpenChapter = onOpenChapter
                )
            }
        }
    }
}

@Composable
private fun BookCard(
    book: CurriculumBook,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenChapter: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curriculum_book_${book.id}")
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = CurriculumCard),
        border = BorderStroke(1.dp, if (expanded) CurriculumAccent.copy(alpha = .55f) else CurriculumBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CurriculumAccent.copy(alpha = .12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${book.number}권",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = CurriculumAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        color = CurriculumText,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${book.chapters.size} Chapter",
                        color = CurriculumMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = if (expanded) "접기" else "보기",
                    color = CurriculumAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (expanded) {
                HorizontalDivider(color = CurriculumBorder)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    book.chapters.forEach { chapter ->
                        ChapterRow(chapter = chapter, onOpenChapter = onOpenChapter)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: CurriculumChapterRef,
    onOpenChapter: (String) -> Unit
) {
    val enabled = chapter.contentAvailable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curriculum_chapter_${chapter.id}")
            .clickable(enabled = enabled) { onOpenChapter(chapter.id) }
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = chapter.number.toString().padStart(2, '0'),
            color = if (enabled) CurriculumAccent else CurriculumMuted.copy(alpha = .65f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = if (enabled) CurriculumText else CurriculumMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal
            )
            if (!enabled) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "커리큘럼 확정 · 본문 미탑재",
                    color = CurriculumMuted.copy(alpha = .72f),
                    fontSize = 11.sp
                )
            }
        }
        if (enabled) {
            Text("열기", color = CurriculumAccent, fontSize = 12.sp)
        }
    }
}
