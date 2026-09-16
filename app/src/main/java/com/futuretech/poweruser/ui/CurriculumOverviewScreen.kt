package com.futuretech.poweruser.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.TextbookChapter
import com.futuretech.poweruser.textbook.TextbookProgressStore
import com.futuretech.poweruser.textbook.V1TextbookCatalog

private val LibraryBg = Color(0xFF07080A)
private val LibraryPanel = Color(0xFF101216)
private val LibraryPanelSoft = Color(0xFF171A20)
private val LibraryBorder = Color(0xFF2A2F36)
private val LibraryText = Color(0xFFF5F7F8)
private val LibraryMuted = Color(0xFFA7AFBA)
private val LibraryAccent = Color(0xFF94B9EE)
private val LibraryAction = Color(0xFF2B527F)

private val CoverColors = listOf(
    Color(0xFF172235),
    Color(0xFF1D2630),
    Color(0xFF20202C),
    Color(0xFF16282A),
    Color(0xFF252019),
    Color(0xFF1B2030)
)

@Composable
fun CurriculumOverviewScreen(
    onOpenChapter: (String) -> Unit,
    onOpenReview: () -> Unit
) {
    val context = LocalContext.current
    val progressStore = remember { TextbookProgressStore(context) }
    val lastChapter = progressStore.selectedChapterId()
        ?.let(V1TextbookCatalog::chapterById)
        ?: V1TextbookCatalog.chapters.first()
    val lastLessonIndex = progressStore.selectedSectionIndex(lastChapter.id)

    Surface(
        modifier = Modifier.fillMaxSize().testTag("curriculum_overview_root"),
        color = LibraryBg
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item { LibraryHeader() }

            item {
                ContinueReadingCard(
                    chapter = lastChapter,
                    lessonIndex = lastLessonIndex,
                    onContinue = { onOpenChapter(lastChapter.id) },
                    onOpenReview = onOpenReview
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "책장",
                                color = LibraryText,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "TRACK 11개 · 한 권씩 골라 읽습니다.",
                                modifier = Modifier.testTag("curriculum_chapter_count"),
                                color = LibraryMuted,
                                fontSize = 12.sp
                            )
                        }
                        Text("좌우로 넘겨보기", color = LibraryAccent, fontSize = 11.sp)
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth().testTag("curriculum_track_shelf"),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(V1TextbookCatalog.chapters, key = { it.id }) { track ->
                            BookCoverCard(track = track, onOpenChapter = onOpenChapter)
                        }
                    }
                }
            }

            item { ReaderGuideCard() }
        }
    }
}

@Composable
private fun LibraryHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "내 코딩 서재",
            color = LibraryText,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "긴 강의 화면이 아니라, 책을 펼치듯 한 장씩 읽습니다.",
            color = LibraryMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun ContinueReadingCard(
    chapter: TextbookChapter,
    lessonIndex: Int,
    onContinue: () -> Unit,
    onOpenReview: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = LibraryPanel,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, LibraryBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroBookCover(chapter)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("이어서 읽기", color = LibraryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        chapter.title,
                        color = LibraryText,
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "LESSON ${lessonIndex + 1} · 읽던 페이지 자동 저장",
                        color = LibraryMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        chapter.summary,
                        color = LibraryMuted,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        maxLines = 4
                    )
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().testTag("curriculum_continue_button"),
                colors = ButtonDefaults.buttonColors(containerColor = LibraryAction, contentColor = LibraryText),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("읽던 페이지 이어서 보기", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOpenReview,
                modifier = Modifier.fillMaxWidth().testTag("curriculum_review_button"),
                border = BorderStroke(1.dp, LibraryBorder),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("복습 열기", color = LibraryAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HeroBookCover(chapter: TextbookChapter) {
    Surface(
        modifier = Modifier.width(112.dp).height(158.dp),
        color = CoverColors[(chapter.number - 1) % CoverColors.size],
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 15.dp, bottomEnd = 15.dp, bottomStart = 8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "CODING BOOK",
                    color = LibraryAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "TRACK\n${chapter.number.toString().padStart(2, '0')}",
                    color = LibraryText,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                chapter.title,
                color = LibraryText,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun BookCoverCard(
    track: TextbookChapter,
    onOpenChapter: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .testTag("curriculum_chapter_${track.id}")
            .clickable { onOpenChapter(track.id) },
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(205.dp),
            color = CoverColors[(track.number - 1) % CoverColors.size],
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 17.dp, bottomEnd = 17.dp, bottomStart = 8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shadowElevation = 7.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(15.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CODING BOOK", color = LibraryAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "TRACK ${track.number.toString().padStart(2, '0')}",
                            color = LibraryText,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        track.title,
                        color = LibraryText,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 5
                    )
                }
            }
        }
        Text(
            track.title,
            color = LibraryText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
    }
}

@Composable
private fun ReaderGuideCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = LibraryPanelSoft,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LibraryBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("읽는 방법", color = LibraryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "오른쪽 끝 터치 → 다음 장   ·   왼쪽 끝 터치 → 이전 장\n좌우 스와이프도 같은 방식으로 동작하고, 앱을 껐다 켜도 마지막 페이지에서 이어집니다.",
                color = LibraryMuted,
                fontSize = 13.sp,
                lineHeight = 21.sp
            )
        }
    }
}
