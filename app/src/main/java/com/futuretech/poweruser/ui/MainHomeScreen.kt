package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.data.LearningProgressEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    progressList: List<LearningProgressEntity>,
    dueReviewCount: Int,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToProject: () -> Unit,
    onNavigateToCurriculum: () -> Unit,
    onNavigateToErrorNotes: () -> Unit
) {
    val completedIds = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet()
    val allLessons = CurriculumDataRepository.beginnerLessons + CurriculumDataRepository.intermediateLessons
    val nextLesson = allLessons.firstOrNull { it.lessonId !in completedIds } ?: allLessons.last()
    val completedCount = allLessons.count { it.lessonId in completedIds }

    Scaffold(
        modifier = Modifier.testTag("learning_home"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("학습", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "오늘 할 것만 간단하게",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("home_today_12min"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("오늘 12분", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "한 번에 많이 하지 말고, 이어서 한 단위만 끝냅니다.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
            }

            HomeActionCard(
                title = "이어서 학습",
                subtitle = "${nextLesson.title} · 약 ${nextLesson.estimatedMinutes}분",
                tag = "home_continue",
                onClick = { onNavigateToLesson(nextLesson.lessonId) }
            )

            HomeActionCard(
                title = "복습 ${dueReviewCount.coerceAtLeast(0)}개",
                subtitle = if (dueReviewCount > 0) "지금 다시 보면 좋은 내용만 모았습니다." else "오늘 예정된 복습이 없습니다.",
                tag = "home_review",
                onClick = onNavigateToReview
            )

            HomeActionCard(
                title = "프로젝트",
                subtitle = "배운 내용을 실제 작은 기능으로 연결합니다.",
                tag = "home_project",
                onClick = onNavigateToProject
            )

            HomeActionCard(
                title = "전체 과정",
                subtitle = "초급·중급 전체 과정과 현재 위치를 봅니다.",
                tag = "home_curriculum",
                onClick = onNavigateToCurriculum
            )

            HorizontalDivider(modifier = Modifier.padding(top = 2.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "진행 $completedCount/${allLessons.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onNavigateToErrorNotes,
                    modifier = Modifier.testTag("home_error_notes")
                ) {
                    Text("내 오류")
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
