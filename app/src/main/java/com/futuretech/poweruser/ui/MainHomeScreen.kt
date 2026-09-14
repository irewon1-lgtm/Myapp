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
import com.futuretech.poweruser.data.LessonContent
import com.futuretech.poweruser.sandbox.ExecutionResult
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    onNavigateToLesson: (String) -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToErrorNotes: () -> Unit,
    onNavigateToCustomProject: () -> Unit,
    onNavigateToBeginnerProject: () -> Unit,
    onNavigateToIntermediateProject: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 초급, 1: 중급, 2: 실전제작/프로젝트

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "미래기술 Power User 훈련",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AI와 함께 구조·판단·검증 능력을 기르는 훈련소",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Navigation Shortcut Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToReview,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("간격 복습", fontSize = 13.sp)
                }
                Button(
                    onClick = onNavigateToErrorNotes,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("오류 노트", fontSize = 13.sp)
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("초급 과정 (8개)", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("중급 과정 (10개)", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("실전 제작 & 프로젝트", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> LessonListSection(lessons = CurriculumDataRepository.beginnerLessons, onNavigateToLesson = onNavigateToLesson)
                1 -> LessonListSection(lessons = CurriculumDataRepository.intermediateLessons, onNavigateToLesson = onNavigateToLesson)
                2 -> ProjectsSection(
                    onNavigateToCustomProject = onNavigateToCustomProject,
                    onNavigateToBeginnerProject = onNavigateToBeginnerProject,
                    onNavigateToIntermediateProject = onNavigateToIntermediateProject
                )
            }
        }
    }
}

@Composable
fun LessonListSection(
    lessons: List<LessonContent>,
    onNavigateToLesson: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(lessons) { lesson ->
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
                        Text(
                            text = "Unit ${lesson.unitNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "학습 가능",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lesson.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lesson.explanation,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectsSection(
    onNavigateToCustomProject: () -> Unit,
    onNavigateToBeginnerProject: () -> Unit,
    onNavigateToIntermediateProject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToBeginnerProject() }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("초급 최종 프로젝트", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("주식 점수 분류 미니앱", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("종목명, 매출성장률, 영업이익률을 입력받아 등급을 판단하고 정돈된 결과를 표시합니다.", fontSize = 13.sp)
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
                Text("개인 주식 연구 미니앱", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("검색 → API → DB 저장 → Python 계산 → AI 분석 → 결과 화면을 하나의 파이프라인으로 연결합니다.", fontSize = 13.sp)
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
                Text("실전 제작 모드", fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary ?: MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("내가 만들고 싶은 아이디어 프로젝트", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("자연어로 아이디어를 입력하면 수준에 맞게 단계를 분해하여 AI와 함께 실전 앱을 제작합니다.", fontSize = 13.sp)
            }
        }
    }
}
