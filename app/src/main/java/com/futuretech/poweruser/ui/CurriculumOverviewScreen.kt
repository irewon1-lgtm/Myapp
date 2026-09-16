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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.textbook.TextbookChapter
import com.futuretech.poweruser.textbook.V1TextbookCatalog

private val CurriculumBg = Color(0xFF090A0D)
private val CurriculumCard = Color(0xFF111318)
private val CurriculumBorder = Color(0xFF2A2F38)
private val CurriculumText = Color(0xFFF4F6F8)
private val CurriculumMuted = Color(0xFFADB5C2)
private val CurriculumAccent = Color(0xFF8AB4F8)
private val CurriculumSoft = Color(0xFF171C24)

@Composable
fun CurriculumOverviewScreen(
    onOpenChapter: (String) -> Unit,
    onOpenLearningHome: () -> Unit
) {
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "코딩 완전과정",
                                color = CurriculumText,
                                fontSize = 30.sp,
                                lineHeight = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "완전 초보 → 설계·검증 가능한 개발자",
                                color = CurriculumAccent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onOpenLearningHome,
                            modifier = Modifier.testTag("learning_home_button"),
                            border = BorderStroke(1.dp, CurriculumBorder)
                        ) {
                            Text("학습 기록", color = CurriculumText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        text = "TRACK 11개 · BLOCK → LESSON → 짧은 회상 → TRACK 실전",
                        modifier = Modifier.testTag("curriculum_chapter_count"),
                        color = CurriculumMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                    Surface(
                        color = CurriculumSoft,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CurriculumBorder)
                    ) {
                        Text(
                            text = "처음 보는 전문용어는 먼저 쉬운 뜻을 만들고, 본문을 충분히 읽은 뒤 문제를 풉니다. TRACK 하나는 짧은 챕터가 아니라 독립 교재 단위입니다.",
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            color = CurriculumText,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            items(V1TextbookCatalog.chapters, key = { it.id }) { track ->
                TrackCard(track = track, onOpenChapter = onOpenChapter)
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: TextbookChapter,
    onOpenChapter: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curriculum_chapter_${track.id}")
            .clickable { onOpenChapter(track.id) },
        colors = CardDefaults.cardColors(containerColor = CurriculumCard),
        border = BorderStroke(1.dp, CurriculumBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = CurriculumAccent.copy(alpha = .12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "TRACK ${track.number.toString().padStart(2, '0')}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = CurriculumAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = CurriculumText,
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = track.summary,
                        color = CurriculumMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Text(
                text = "핵심 · ${track.keyConcepts.joinToString(" · ")}",
                color = CurriculumText,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "본문 + 코드 + 실습 + 완료 기준",
                    color = CurriculumMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "열기 →",
                    color = CurriculumAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
