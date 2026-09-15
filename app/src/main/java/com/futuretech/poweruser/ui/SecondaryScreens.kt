package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.AppRepository
import com.futuretech.poweruser.data.PersonalErrorNoteEntity
import com.futuretech.poweruser.data.SpacedRepetitionItemEntity
import com.futuretech.poweruser.education.MemoryPriority
import com.futuretech.poweruser.education.ReviewVariationEngine
import com.futuretech.poweruser.education.SpacedRepetitionEngine
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedRepetitionReviewScreen(
    repository: AppRepository,
    onNavigateBack: () -> Unit
) {
    var dueReviews by remember { mutableStateOf<List<SpacedRepetitionItemEntity>>(emptyList()) }
    var revealedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var failureStreaks by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        dueReviews = repository.getDueReviews()
        revealedIds = revealedIds.intersect(dueReviews.map { it.conceptId }.toSet())
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        modifier = Modifier.testTag("review_screen"),
        topBar = {
            TopAppAppBarPlaceholder(onNavigateBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text("오늘 복습 ${dueReviews.size}개", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "같은 문장을 그대로 외우게 하지 않습니다. 입력·비교·문맥을 바꾼 문제로 다시 적용합니다. 틀림과 힌트 사용은 더 빨리, 독립 정답은 더 길게 간격을 조정합니다.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    )
                }
            }

            if (dueReviews.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("현재 복습 완료 ✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("지금 시점에 다시 볼 항목이 없습니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(dueReviews, key = { it.conceptId }) { item ->
                        val guidance = SpacedRepetitionEngine.guidance(item)
                        val variation = ReviewVariationEngine.forItem(item)
                        val revealed = item.conceptId in revealedIds
                        val priorityColor = when (guidance.priority) {
                            MemoryPriority.MUST_REMEMBER -> MaterialTheme.colorScheme.error
                            MemoryPriority.UNDERSTAND -> MaterialTheme.colorScheme.primary
                            MemoryPriority.AI_CAN_HELP -> MaterialTheme.colorScheme.secondary
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("review_card_${item.conceptId}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = priorityColor.copy(alpha = 0.12f)) {
                                        Text(
                                            guidance.priority.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = priorityColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        "${guidance.stageLabel} · ${guidance.progressLabel}",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(item.conceptTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(variation.prompt, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(variation.focus, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.outline)

                                variation.changedExample?.let { changed ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(changed, style = CodeTypography, fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                if (!revealed) {
                                    Button(
                                        onClick = { revealedIds = revealedIds + item.conceptId },
                                        modifier = Modifier.fillMaxWidth().testTag("reveal_${item.conceptId}")
                                    ) { Text("답·기준 확인") }
                                } else {
                                    ReviewAnswerBlock("한 문장 정의", item.definition)
                                    if (item.analogy.isNotBlank()) ReviewAnswerBlock("원래 학습 목표/비유", item.analogy)
                                    if (item.example.isNotBlank()) {
                                        Text("원래 예시", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(item.example, style = CodeTypography, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(9.dp))
                                    }
                                    if (item.comparison.isNotBlank()) ReviewAnswerBlock("헷갈림 방지", item.comparison)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val streak = (failureStreaks[item.conceptId] ?: 0) + 1
                                                failureStreaks = failureStreaks + (item.conceptId to streak)
                                                scope.launch {
                                                    repository.recordReview(item.conceptId, remembered = false, failureStreak = streak)
                                                    revealedIds = revealedIds - item.conceptId
                                                    refresh()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("again_${item.conceptId}")
                                        ) { Text("다시 보기") }
                                        Button(
                                            onClick = {
                                                failureStreaks = failureStreaks - item.conceptId
                                                scope.launch {
                                                    repository.recordReview(item.conceptId, remembered = true, failureStreak = 0)
                                                    revealedIds = revealedIds - item.conceptId
                                                    refresh()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("remember_${item.conceptId}")
                                        ) { Text("혼자 설명 가능 ✓") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppAppBarPlaceholder(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text("복습 · 변형 문제", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
    )
}

@Composable
private fun ReviewAnswerBlock(label: String, value: String) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(3.dp))
    Text(value, fontSize = 13.sp, lineHeight = 19.sp)
    Spacer(modifier = Modifier.height(9.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalErrorNotesScreen(
    errorNotesList: List<PersonalErrorNoteEntity>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 개인 오류 노트", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "여기 누적된 실제 오답은 Chapter Challenge 추가 출제 슬롯에 자동 가중됩니다.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (errorNotesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        "아직 기록된 오류가 없습니다. 문제를 틀리거나 제출 테스트가 실패하면 자동 수집됩니다.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(errorNotesList) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(note.errorType, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)) {
                                        Text(
                                            "발생 ${note.occurrenceCount}회",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(6.dp, 2.dp),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) { Text(note.codeSnippet, style = CodeTypography, fontSize = 12.sp) }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("오류: ${note.errorMessage}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Text(
                                    "교정 가이드: ${note.correctionGuide}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
