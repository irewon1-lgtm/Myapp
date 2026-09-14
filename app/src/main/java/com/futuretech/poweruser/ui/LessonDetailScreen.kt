package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
fun LessonDetailScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sandboxEngine = remember { SandboxedExecutionEngine(context) }

    val lesson = remember(lessonId) {
        CurriculumDataRepository.beginnerLessons.find { it.lessonId == lessonId }
            ?: CurriculumDataRepository.intermediateLessons.find { it.lessonId == lessonId }
            ?: CurriculumDataRepository.beginnerLessons.first()
    }

    var selectedMode by remember { mutableStateOf(1) } // 1: 혼자 풀기, 2: 힌트 받기, 3: AI 협업
    var currentHintStep by remember { mutableStateOf(0) } // 0: 없음, 1, 2, 3
    var codeInput by remember { mutableStateOf(lesson.initialPracticeCode) }
    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var fillBlankInput by remember { mutableStateOf("") }
    var userExplanation by remember { mutableStateOf("") }
    var explanationFeedback by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf(-1) }
    var isAnswerChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${lesson.lessonId} - ${lesson.title}", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← 뒤로", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Mode Selector Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AI 모드 선택:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == 1,
                            onClick = { selectedMode = 1 },
                            label = { Text("1. 혼자 풀기") }
                        )
                        FilterChip(
                            selected = selectedMode == 2,
                            onClick = { selectedMode = 2 },
                            label = { Text("2. 힌트만") }
                        )
                        FilterChip(
                            selected = selectedMode == 3,
                            onClick = {
                                if (lesson.curriculumType == "BEGINNER") {
                                    // 초급 AI 대신작성 제한 규칙
                                    selectedMode = 2
                                } else {
                                    selectedMode = 3
                                }
                            },
                            label = {
                                Text(if (lesson.curriculumType == "BEGINNER") "3. AI협업(초급제한)" else "3. AI 같이 풀기")
                            }
                        )
                    }
                }
            }

            // 1. 쉬운 개념 설명
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. 쉬운 개념 설명", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(lesson.explanation, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("목표: ${lesson.expectedOutcome}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 2. 예시 코드 읽기
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. 구조/코드 읽기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(lesson.codeSample, style = CodeTypography, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // 3. 직접 코딩 및 실습 샌드박스
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3. 코드 실행 실습장", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("학습 샌드박스 코드 입력") },
                        textStyle = CodeTypography,
                        minLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val lang = if (lesson.lessonId.startsWith("I04")) ProgrammingLanguage.SQL else ProgrammingLanguage.PYTHON
                                    val res = sandboxEngine.execute(lang, codeInput)
                                    executionResult = res
                                    if (!res.isSuccess) {
                                        onRecordErrorNote(
                                            "CODE_EXECUTION_ERROR",
                                            codeInput,
                                            res.errorMessage ?: "Execution failed",
                                            "문법 및 오탈자를 점검하세요."
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("▶ 코드 실행")
                        }

                        if (selectedMode >= 2) {
                            OutlinedButton(
                                onClick = {
                                    if (currentHintStep < 3) currentHintStep++
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("💡 힌트 (${currentHintStep}/3)")
                            }
                        }
                    }

                    if (currentHintStep > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            val hintText = when (currentHintStep) {
                                1 -> "힌트 1 (방향): ${lesson.hintLevel1}"
                                2 -> "힌트 2 (위치): ${lesson.hintLevel2}"
                                else -> "힌트 3 (정답에 가까움): ${lesson.hintLevel3}"
                            }
                            Text(hintText, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    executionResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (res.isSuccess) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    if (res.isSuccess) "실행 결과 (성공):" else "실행 오류:",
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.isSuccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    res.output.ifEmpty { res.errorMessage ?: "" },
                                    style = CodeTypography,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            // 4. 고장난 코드 디버깅
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("4. 고장난 예제 고치기 (디버깅)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("아래 일부러 고장낸 코드를 정상 작동하도록 수정해보세요:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(lesson.brokenCode, style = CodeTypography, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("정답 수정안 참고:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text(lesson.brokenCodeFix, style = CodeTypography, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 5. AI 환각 및 검증 훈련
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("5. AI 환각 / 검증 훈련", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(lesson.aiHallucinationQuestion, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    lesson.aiHallucinationOptions.forEachIndexed { index, optionText ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedOption == index,
                                onClick = {
                                    selectedOption = index
                                    isAnswerChecked = true
                                }
                            )
                            Text(optionText, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    if (isAnswerChecked) {
                        val isCorrect = selectedOption == lesson.correctOptionIndex
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isCorrect) "정답입니다! AI 답변을 정확히 검증했습니다." else "오답입니다. AI 답변은 항상 원문 출처를 검증해야 합니다.",
                            color = if (isCorrect) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // 6. 본인 말로 설명하기 훈련
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("6. 본인 말로 설명하기 훈련", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(lesson.explainPrompt, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userExplanation,
                        onValueChange = { userExplanation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("본인의 말로 설명 입력") },
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            explanationFeedback = if (userExplanation.length > 10) {
                                "AI 피백: 핵심 개념이 잘 포함되어 있습니다. 이해 수준이 뛰어납니다."
                            } else {
                                "AI 피드백: 설명이 너무 짧습니다. 핵심 키워드를 포함하여 더 구체적으로 작성해 보세요."
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("설명 제출 및 AI 피드백 받기")
                    }

                    if (explanationFeedback.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(explanationFeedback, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
