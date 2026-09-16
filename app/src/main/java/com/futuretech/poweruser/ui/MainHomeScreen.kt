package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.LearningProgressEntity
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.V1TextbookCatalog

private val LibraryBg = Color(0xFF090A0D)
private val LibraryPaper = Color(0xFF111318)
private val LibraryBorder = Color(0xFF2A2F38)
private val LibraryText = Color(0xFFF4F6F8)
private val LibraryMuted = Color(0xFFAAB2BE)
private val LibraryAccent = Color(0xFF8AB4F8)
private val LibraryCoverTop = Color(0xFF18263A)
private val LibraryCoverBottom = Color(0xFF0D1118)

@Composable
fun MainHomeScreen(
    progressList: List<LearningProgressEntity>,
    dueReviewCount: Int,
    onOpenBook: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToProject: () -> Unit,
    onNavigateToCurriculum: () -> Unit,
    onNavigateToErrorNotes: () -> Unit
) {
    val context = LocalContext.current
    val bookStore = remember { TextbookProgressStore(context) }
    val currentTrack = V1TextbookCatalog.chapterById(bookStore.selectedChapterId() ?: "V1-C01")
        ?: V1TextbookCatalog.chapters.first()
    val currentLesson = bookStore.selectedSectionIndex(currentTrack.id) + 1
    val readTracks = bookStore.readCompletedIds().size
    val completedPractice = progressList.count { it.isCompleted }

    Scaffold(
        modifier = Modifier.testTag("learning_home"),
        containerColor = LibraryBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LibraryBg),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("내 코딩책", color = LibraryText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "읽고 · 직접 풀고 · 다시 복습하는 한 권의 교재",
                        color = LibraryMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBook)
                        .testTag("home_continue"),
                    colors = CardDefaults.cardColors(containerColor = LibraryPaper),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LibraryBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCover()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("이어서 읽기", color = LibraryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                currentTrack.title,
                                color = LibraryText,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3
                            )
                            Text(
                                "TRACK ${currentTrack.number.toString().padStart(2, '0')} · LESSON $currentLesson",
                                color = LibraryMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                "마지막으로 보던 페이지에서 그대로 이어집니다.",
                                color = LibraryMuted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Surface(
                                color = LibraryAccent.copy(alpha = .12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "책 열기  →",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    color = LibraryAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LibraryActionCard(
                        modifier = Modifier.weight(1f).testTag("home_curriculum"),
                        title = "목차",
                        subtitle = "11개 TRACK 전체",
                        onClick = onNavigateToCurriculum
                    )
                    LibraryActionCard(
                        modifier = Modifier.weight(1f).testTag("home_review"),
                        title = "복습",
                        subtitle = if (dueReviewCount > 0) "$dueReviewCount개 예정" else "오늘은 없음",
                        onClick = onNavigateToReview
                    )
                }
            }

            item {
                LibraryActionCard(
                    modifier = Modifier.fillMaxWidth().testTag("home_project"),
                    title = "직접 만들어보기",
                    subtitle = "읽은 내용을 실제 작은 기능과 코드로 연결합니다.",
                    onClick = onNavigateToProject
                )
            }

            item {
                HorizontalDivider(color = LibraryBorder)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("읽기 완료 $readTracks/${V1TextbookCatalog.TRACK_COUNT} TRACK", color = LibraryMuted, fontSize = 12.sp)
                        Text("실습 완료 $completedPractice개", color = LibraryMuted, fontSize = 12.sp)
                    }
                    TextButton(onClick = onNavigateToErrorNotes, modifier = Modifier.testTag("home_error_notes")) {
                        Text("내 오류", color = LibraryAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCover() {
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(164.dp)
            .background(
                Brush.verticalGradient(listOf(LibraryCoverTop, LibraryCoverBottom)),
                RoundedCornerShape(13.dp)
            )
            .testTag("home_book_cover")
    ) {
        Box(
            Modifier
                .width(7.dp)
                .height(164.dp)
                .background(LibraryAccent.copy(alpha = .75f), RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp))
                .align(Alignment.CenterStart)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 10.dp, top = 18.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("CODING", color = LibraryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("코딩\n완전과정", color = LibraryText, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("초급 · 중급", color = LibraryMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LibraryActionCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = LibraryPaper),
        border = androidx.compose.foundation.BorderStroke(1.dp, LibraryBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = LibraryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = LibraryMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
