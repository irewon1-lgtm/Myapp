package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
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
import com.futuretech.poweruser.sandbox.ExecutionResult
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.components.HtmlPreviewSandboxView
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onStepCompleted: (String, String, Int) -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sandboxEngine = remember { SandboxedExecutionEngine(context) }
    val lesson = remember(lessonId) { CurriculumDataRepository.lessonById(lessonId) ?: CurriculumDataRepository.beginnerLessons.first() }
    val fillBlankQuestion = remember(lesson.fillInBlankPrompt) { parseFillBlankPrompt(lesson.fillInBlankPrompt) }

    var currentStep by remember(lessonId) { mutableStateOf(1) }
    var predictionText by remember(lessonId) { mutableStateOf("") }
    var predictionSubmitted by remember(lessonId) { mutableStateOf(false) }
    var fillBlankInput by remember(lessonId) { mutableStateOf("") }
    var fillBlankCorrect by remember(lessonId) { mutableStateOf(false) }
    var fillBlankChecked by remember(lessonId) { mutableStateOf(false) }
    var fillBlankAttempts by remember(lessonId) { mutableStateOf(0) }
    var codeInput by remember(lessonId) {
        mutableStateOf(if (lesson.practiceLanguage == "HTML_JS") lesson.codeSample else lesson.initialPracticeCode)
    }
    var executionResult by remember(lessonId) { mutableStateOf<ExecutionResult?>(null) }
    var practiceAttempted by remember(lessonId) { mutableStateOf(false) }
    var currentHintStep by remember(lessonId) { mutableStateOf(0) }
    var debugInput by remember(lessonId) { mutableStateOf(lesson.brokenCode) }
    var debugCorrect by remember(lessonId) { mutableStateOf(false) }
    var debugChecked by remember(lessonId) { mutableStateOf(false) }
    var debugAttempts by remember(lessonId) { mutableStateOf(0) }
    var showDebugAnswer by remember(lessonId) { mutableStateOf(false) }
    var selectedOption by remember(lessonId) { mutableStateOf(-1) }
    var aiChecked by remember(lessonId) { mutableStateOf(false) }
    var aiCorrect by remember(lessonId) { mutableStateOf(false) }
    var userExplanation by remember(lessonId) { mutableStateOf("") }
    var explanationChecked by remember(lessonId) { mutableStateOf(false) }
    var completionSaved by remember(lessonId) { mutableStateOf(false) }

    val explanationEvaluation = evaluateExplanation(userExplanation, lesson.explainKeywords)

    fun canAdvance(step: Int): Boolean = when (step) {
        1 -> true
        2 -> predictionSubmitted
        3 -> fillBlankCorrect
        4 -> practiceAttempted
        5 -> debugCorrect
        6 -> aiCorrect
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${lesson.moduleTitle} · Level ${lesson.stepNumber}/${lesson.stepTotal}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${lesson.lessonId} · 학습 Step $currentStep/7", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 목록") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..7) {
                    Box(
                        modifier = Modifier.weight(1f).height(6.dp).background(
                            if (i <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(3.dp)
                        )
                    )
                }
            }

            Column {
                Text(lesson.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("예상 소요 ${lesson.estimatedMinutes}분 · 한 레슨 안에서 예상→문제→실행→디버깅→검증", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            when (currentStep) {
                1 -> LearningCard("Step 1. 개념을 작은 단위로 이해") {
                    Text(lesson.explanation, fontSize = 14.sp, lineHeight = 23.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("이번 레벨의 졸업 기준", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(lesson.expectedOutcome, fontSize = 14.sp, lineHeight = 21.sp)
                }

                2 -> LearningCard("Step 2. 실행 전에 결과 예상") {
                    Text("정답이나 실행 결과를 보기 전에 무엇이 일어날지 먼저 적으세요. 틀려도 괜찮지만 빈칸으로 넘길 수는 없습니다.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    CodeBox(lesson.codeSample)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = predictionText,
                        onValueChange = { predictionText = it; predictionSubmitted = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("내 예상: 입력/처리/출력이 어떻게 될까?") }, minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { predictionSubmitted = predictionText.trim().isNotEmpty() }, enabled = predictionText.trim().isNotEmpty()) { Text("예상 제출") }
                    if (predictionSubmitted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FeedbackBox(true, "비교 기준: ${lesson.expectedOutcome}")
                    }
                }

                3 -> LearningCard("Step 3. 핵심 개념 빈칸 문제") {
                    Text(fillBlankQuestion.displayText, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fillBlankInput,
                        onValueChange = { fillBlankInput = it; fillBlankChecked = false; fillBlankCorrect = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("정답 입력") }, singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            fillBlankAttempts++
                            fillBlankChecked = true
                            fillBlankCorrect = isFillBlankCorrect(fillBlankInput, fillBlankQuestion.acceptedAnswers)
                            if (!fillBlankCorrect) onRecordErrorNote(
                                "${lesson.lessonId}_FILL_BLANK", fillBlankInput, "빈칸 문제 오답",
                                "${lesson.moduleTitle} Level ${lesson.stepNumber} 핵심 개념을 다시 확인하세요."
                            )
                        },
                        enabled = fillBlankInput.trim().isNotEmpty()
                    ) { Text("정답 확인") }
                    if (fillBlankChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(fillBlankCorrect, if (fillBlankCorrect) "정답입니다." else "오답입니다. 다시 시도하세요. (${fillBlankAttempts}회)")
                    }
                }

                4 -> LearningCard("Step 4. 직접 수정하고 실행") {
                    Text("코드를 읽기만 하지 말고 값을 바꾸거나 한 줄을 수정한 뒤 직접 실행하세요. 실패한 실행도 오류노트 학습 자료가 됩니다.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeInput, onValueChange = { codeInput = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("실습 코드 · ${lesson.practiceLanguage}") }, textStyle = CodeTypography, minLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    practiceAttempted = true
                                    val result = sandboxEngine.execute(languageFor(lesson.practiceLanguage), codeInput)
                                    executionResult = result
                                    if (!result.isSuccess) onRecordErrorNote(
                                        "${lesson.lessonId}_PRACTICE_ERROR", codeInput, result.errorMessage ?: "실행 오류",
                                        "오류 메시지와 발생 지점을 읽고 가장 작은 수정부터 시도하세요."
                                    )
                                }
                            }
                        ) { Text("▶ 실행") }
                        OutlinedButton(onClick = { if (currentHintStep < 3) currentHintStep++ }) { Text("💡 힌트 $currentHintStep/3") }
                    }
                    if (currentHintStep > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val hint = when (currentHintStep) { 1 -> lesson.hintLevel1; 2 -> lesson.hintLevel2; else -> lesson.hintLevel3 }
                        FeedbackBox(true, "힌트: $hint")
                    }
                    if (lesson.practiceLanguage == "HTML_JS") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("안전 미리보기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        HtmlPreviewSandboxView(htmlContent = codeInput)
                    }
                    executionResult?.let { result ->
                        Spacer(modifier = Modifier.height(10.dp))
                        FeedbackBox(result.isSuccess, if (result.isSuccess) "실행 결과:\n${result.output.ifEmpty { "출력 없음" }}" else "실행 오류:\n${result.errorMessage ?: "알 수 없는 오류"}")
                        Text("※ 실행기는 학습용 제한 환경입니다. 실행 성공만으로 레슨을 완료 처리하지 않습니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                5 -> LearningCard("Step 5. 일부러 고장난 예제 디버깅") {
                    Text("정답을 처음부터 보여주지 않습니다. 원인을 한 문장으로 생각한 뒤 아래 코드를 직접 고치세요.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = debugInput,
                        onValueChange = { debugInput = it; debugChecked = false; debugCorrect = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("고장난 코드 수정") }, textStyle = CodeTypography, minLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        debugAttempts++
                        debugChecked = true
                        debugCorrect = isDebugFixCorrect(debugInput, lesson.brokenCodeFix)
                        if (!debugCorrect) onRecordErrorNote(
                            "${lesson.lessonId}_DEBUG", debugInput, "디버깅 문제 오답",
                            when (debugAttempts) { 1 -> lesson.hintLevel1; 2 -> lesson.hintLevel2; else -> lesson.hintLevel3 }
                        )
                    }) { Text("수정안 검사") }
                    if (debugChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(debugCorrect, if (debugCorrect) "수정 성공." else "아직 해결되지 않았습니다. (${debugAttempts}회)")
                    }
                    if (!debugCorrect && debugAttempts >= 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showDebugAnswer = !showDebugAnswer }) { Text(if (showDebugAnswer) "정답 숨기기" else "정답 가이드 보기") }
                    }
                    if (showDebugAnswer) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBox(lesson.brokenCodeFix)
                        Text("정답을 본 뒤에도 위 입력칸을 직접 수정하고 다시 검사해야 통과됩니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                6 -> LearningCard("Step 6. AI의 그럴듯한 답 판별") {
                    Text(lesson.aiHallucinationQuestion, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    lesson.aiHallucinationOptions.forEachIndexed { index, optionText ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedOption == index, onClick = { selectedOption = index; aiChecked = false; aiCorrect = false })
                            Text(optionText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            aiChecked = true
                            aiCorrect = selectedOption == lesson.correctOptionIndex
                            if (!aiCorrect) onRecordErrorNote(
                                "${lesson.lessonId}_AI_JUDGEMENT", selectedOption.toString(), "AI 판별 문제 오답",
                                "문장의 확신도가 아니라 검증 가능한 사실·구조·근거를 다시 보세요."
                            )
                        }, enabled = selectedOption >= 0
                    ) { Text("선택한 답 채점") }
                    if (aiChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(aiCorrect, if (aiCorrect) "정답입니다." else "오답입니다. 근거를 다시 확인하세요.")
                    }
                }

                7 -> LearningCard("Step 7. 자기 말로 설명하고 숙련 확인") {
                    Text(lesson.explainPrompt, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userExplanation,
                        onValueChange = { userExplanation = it; explanationChecked = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("내 언어로 3문장 이상 설명") }, minLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { explanationChecked = true },
                        enabled = userExplanation.trim().isNotEmpty()
                    ) { Text("설명 점검") }
                    if (explanationChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val coverage = (explanationEvaluation.coverage * 100).toInt()
                        FeedbackBox(
                            explanationEvaluation.passed,
                            if (explanationEvaluation.passed) "핵심 개념 포함률 ${coverage}% · 설명 기준 통과"
                            else "핵심 개념 포함률 ${coverage}% · 빠진 개념: ${explanationEvaluation.missingKeywords.joinToString(", ")}"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("완주 조건", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• 빈칸 문제 정답 ${mark(fillBlankCorrect)}", fontSize = 12.sp)
                    Text("• 코드 실행 시도 ${mark(practiceAttempted)}", fontSize = 12.sp)
                    Text("• 디버깅 문제 해결 ${mark(debugCorrect)}", fontSize = 12.sp)
                    Text("• AI 판별 문제 정답 ${mark(aiCorrect)}", fontSize = 12.sp)
                    Text("• 40자 이상 + 핵심개념 50% 이상 설명 ${mark(explanationEvaluation.passed)}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onStepCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber); completionSaved = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = fillBlankCorrect && practiceAttempted && debugCorrect && aiCorrect && explanationEvaluation.passed && !completionSaved
                    ) { Text(if (completionSaved) "완주 저장 완료" else "Level ${lesson.stepNumber} 완주 저장") }
                    if (completionSaved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(true, "완주 기록 저장 완료. 다음 Level이 해금되고 이 개념은 1·3·7·14일 복습 대상으로 들어갑니다.")
                    }
                }
            }

            if (currentStep < 7) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { if (currentStep > 1) currentStep-- }, enabled = currentStep > 1) { Text("← 이전") }
                    Button(onClick = { if (currentStep < 7) currentStep++ }, enabled = canAdvance(currentStep)) { Text("다음 →") }
                }
                if (!canAdvance(currentStep) && currentStep > 1) {
                    Text(
                        when (currentStep) {
                            2 -> "예상을 제출해야 다음으로 갈 수 있습니다."
                            3 -> "빈칸 문제를 맞혀야 다음으로 갈 수 있습니다."
                            4 -> "코드를 최소 1회 직접 실행해야 다음으로 갈 수 있습니다."
                            5 -> "디버깅 문제를 해결해야 다음으로 갈 수 있습니다."
                            6 -> "AI 판별 문제를 맞혀야 다음으로 갈 수 있습니다."
                            else -> ""
                        }, fontSize = 12.sp, color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                OutlinedButton(onClick = { currentStep-- }, modifier = Modifier.fillMaxWidth()) { Text("← 이전 단계") }
            }
        }
    }
}

private fun languageFor(value: String): ProgrammingLanguage = when (value) {
    "SQL" -> ProgrammingLanguage.SQL
    "TYPESCRIPT" -> ProgrammingLanguage.TYPESCRIPT
    "HTML_JS" -> ProgrammingLanguage.HTML_JS
    else -> ProgrammingLanguage.PYTHON
}

private fun mark(value: Boolean): String = if (value) "✓" else "✗"

@Composable
private fun LearningCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun CodeBox(text: String) {
    Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Text(text, style = CodeTypography, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun FeedbackBox(success: Boolean, text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().background(
            if (success) MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            RoundedCornerShape(8.dp)
        ).padding(10.dp)
    ) {
        Text(text, fontSize = 13.sp, color = if (success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
    }
}
