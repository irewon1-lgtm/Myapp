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
    val completedIds = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet()
    val beginner = CurriculumDataRepository.beginnerLessons
    val intermediate = CurriculumDataRepository.intermediateLessons
    val all = beginner + intermediate
    val totalMinutes = all.sumOf { it.estimatedMinutes }
    val nextLesson = all.firstOrNull { it.lessonId !in completedIds } ?: all.last()
    val completedCount = all.count { it.lessonId in completedIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("미래기술 Power User 훈련소", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "90개 마이크로 레슨 · 읽기보다 직접 예상·수정·실행·검증",
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
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("오늘의 학습", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        "다음: ${nextLesson.lessonId} · ${nextLesson.title}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "전체 $completedCount/${all.size} 레슨 · 설계 학습량 약 ${totalMinutes / 60}시간 ${totalMinutes % 60}분",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { completedCount.toFloat() / all.size.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onNavigateToLesson(nextLesson.lessonId) }) {
                            Text("이어서 학습 ▶")
                        }
                        OutlinedButton(onClick = onNavigateToReview) {
                            Text("복습")
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("초급 40", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("중급 50", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("실전·오류", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> ProgressiveLessonList(
                    lessons = beginner,
                    progressList = progressList,
                    prerequisiteSatisfied = true,
                    onNavigateToLesson = onNavigateToLesson
                )
                1 -> ProgressiveLessonList(
                    lessons = intermediate,
                    progressList = progressList,
                    prerequisiteSatisfied = beginner.all { it.lessonId in completedIds },
                    onNavigateToLesson = onNavigateToLesson
                )
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
private fun ProgressiveLessonList(
    lessons: List<LessonContent>,
    progressList: List<LearningProgressEntity>,
    prerequisiteSatisfied: Boolean,
    onNavigateToLesson: (String) -> Unit
) {
    val completedIds = progressList.filter { it.isCompleted }.map { it.lessonId }.toSet()
    val groups = lessons.groupBy { it.moduleNumber }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        groups.forEach { (moduleNumber, moduleLessons) ->
            val moduleDone = moduleLessons.count { it.lessonId in completedIds }
            item(key = "header-$moduleNumber-${moduleLessons.first().curriculumType}") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Module ${moduleNumber.toString().padStart(2, '0')}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                moduleLessons.first().moduleTitle,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("$moduleDone/${moduleLessons.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { moduleDone.toFloat() / moduleLessons.size.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(moduleLessons, key = { it.lessonId }) { lesson ->
                val index = lessons.indexOfFirst { it.lessonId == lesson.lessonId }
                val isDone = lesson.lessonId in completedIds
                val previousDone = index == 0 || lessons[index - 1].lessonId in completedIds
                val isUnlocked = prerequisiteSatisfied && previousDone
                val canOpen = isDone || isUnlocked

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canOpen) { onNavigateToLesson(lesson.lessonId) }
                        .border(
                            1.dp,
                            if (isUnlocked && !isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canOpen) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Level ${lesson.stepNumber}/${lesson.stepTotal} · 약 ${lesson.estimatedMinutes}분",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Text(
                                when {
                                    isDone -> "완료 ✓"
                                    isUnlocked -> "학습 가능"
                                    else -> "이전 레슨 완료 후 해금"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isDone -> MaterialTheme.colorScheme.secondary
                                    isUnlocked -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(lesson.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            lesson.expectedOutcome,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canOpen) 0.82f else 0.48f)
                        )
                    }
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
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard("내 개인 오류 노트", "틀린 빈칸·디버깅·AI 판별과 실행 오류를 자동 누적", MaterialTheme.colorScheme.error, onNavigateToErrorNotes)
        ActionCard("초급 최종 프로젝트", "40개 초급 레슨을 연결해 주식 점수 분류 미니앱 완성", MaterialTheme.colorScheme.primary, onNavigateToBeginnerProject)
        ActionCard("중급 최종 프로젝트", "50개 중급 레슨을 연결해 개인 주식 연구 파이프라인 완성", MaterialTheme.colorScheme.secondary, onNavigateToIntermediateProject)
        ActionCard("실전 제작 모드", "내 아이디어를 요구사항→작은 미션→검증으로 분해", MaterialTheme.colorScheme.primary, onNavigateToCustomProject)
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, color = accent, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp)
        }
    }
}
