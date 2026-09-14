package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.data.LearningProgressEntity
import com.futuretech.poweruser.data.LessonContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    progressList: List<LearningProgressEntity>,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToErrorNotes: () -> Unit,
    onNavigateToCustomProject: () -> Unit,
    onNavigateToBeginnerProject: () -> Unit,
    onNavigateToIntermediateProject: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    val beginnerProgressCount = progressList.count { it.curriculumType == "BEGINNER" && it.isCompleted }
    val totalBeginner = CurriculumDataRepository.beginnerLessons.size
    val lastStudied = progressList.maxByOrNull { it.lastStudiedTimestamp }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "미래기술 Power User 훈련소",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI와 함께 구조·판단·검증 능력을 기르는 대시보드",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
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
        ) {
            // Dashboard Summary Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("오늘의 학습 대시보드", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lastStudied != null) "최근 학습: ${lastStudied.title}" else "오늘 학습을 시작해보세요!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val targetId = lastStudied?.lessonId ?: "B01"
                                onNavigateToLesson(targetId)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (lastStudied != null) "이어서 하기 ▶" else "첫 레슨 시작 ▶")
                        }
                        OutlinedButton(
                            onClick = onNavigateToReview,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("복습 예정 카드")
                        }
                    }
                }
            }

            // Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("초급 과정 (8개)", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("중급 과정 (10개)", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("실전 프로젝트 & 내 오류", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> LessonListSection(lessons = CurriculumDataRepository.beginnerLessons, progressList = progressList, onNavigateToLesson = onNavigateToLesson)
                1 -> LessonListSection(lessons = CurriculumDataRepository.intermediateLessons, progressList = progressList, onNavigateToLesson = onNavigateToLesson)
                2 -> ProjectAndErrorSection(
                    onNavigateToCustomProject = onNavigateToCustomProject,
                    onNavigateToBeginnerProject = onNavigateToBeginnerProject,
                    onNavigateToIntermediateProject = onNavigateToIntermediateProject,
                    onNavigateToErrorNotes = onNavigateToErrorNotes
                )
            }
        }
    }
}

@Composable
fun LessonListSection(
    lessons: List<LessonContent>,
    progressList: List<LearningProgressEntity>,
    onNavigateToLesson: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(lessons) { lesson ->
            val progress = progressList.find { it.lessonId == lesson.lessonId }
            val isDone = progress?.isCompleted ?: false

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLesson(lesson.lessonId) }
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unit ${lesson.unitNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (isDone) "완료됨 ✓" else "학습 가능",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(lesson.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(lesson.explanation, fontSize = 13.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun ProjectAndErrorSection(
    onNavigateToCustomProject: () -> Unit,
    onNavigateToBeginnerProject: () -> Unit,
    onNavigateToIntermediateProject: () -> Unit,
    onNavigateToErrorNotes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToErrorNotes() }
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("내 개인 오류 노트", fontSize = 14.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Text("실제로 자주 틀리는 실습 에러 자동 수집함", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBeginnerProject() }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("초급 최종 프로젝트", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("주식 점수 분류 미니앱 [DEMO / RULE-BASED]", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToIntermediateProject() }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("중급 최종 프로젝트", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text("개인 주식 연구 미니앱 [MOCK / LOCAL PIPELINE]", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToCustomProject() }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("실전 제작 모드", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("내 아이디어 생성기 [LOCAL RULE-BASED]", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
