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
    var readingText by remember(lessonId) { mutableStateOf("") }
    var readingSubmitted by remember(lessonId) { mutableStateOf(false) }

    var codeInput by remember(lessonId) {
        mutableStateOf(if (lesson.practiceLanguage == "HTML_JS") lesson.codeSample else lesson.initialPracticeCode)
    }
    var executionResult by remember(lessonId) { mutableStateOf<ExecutionResult?>(null) }
    var practiceAttempted by remember(lessonId) { mutableStateOf(false) }
    var currentHintStep by remember(lessonId) { mutableStateOf(0) }

    var fillBlankInput by remember(lessonId) { mutableStateOf("") }
    var fillBlankCorrect by remember(lessonId) { mutableStateOf(false) }
    var fillBlankChecked by remember(lessonId) { mutableStateOf(false) }
    var fillBlankAttempts by remember(lessonId) { mutableStateOf(0) }

    var shortCodeInput by remember(lessonId) { mutableStateOf("") }
    var shortCodeCorrect by remember(lessonId) { mutableStateOf(false) }
    var shortCodeChecked by remember(lessonId) { mutableStateOf(false) }
    var shortCodeAttempts by remember(lessonId) { mutableStateOf(0) }
    var showShortAnswer by remember(lessonId) { mutableStateOf(false) }

    var debugInput by remember(lessonId) { mutableStateOf(lesson.brokenCode) }
    var debugCorrect by remember(lessonId) { mutableStateOf(false) }
    var debugChecked by remember(lessonId) { mutableStateOf(false) }
    var debugAttempts by remember(lessonId) { mutableStateOf(0) }
    var showDebugAnswer by remember(lessonId) { mutableStateOf(false) }

    var selectedOption by remember(lessonId) { mutableStateOf(-1) }
    var aiChecked by remember(lessonId) { mutableStateOf(false) }
    var aiCorrect by remember(lessonId) { mutableStateOf(false) }

    var missionCode by remember(lessonId) { mutableStateOf(lesson.initialPracticeCode) }
    var missionResult by remember(lessonId) { mutableStateOf<ExecutionResult?>(null) }
    var missionAttempted by remember(lessonId) { mutableStateOf(false) }

    var userExplanation by remember(lessonId) { mutableStateOf("") }
    var explanationChecked by remember(lessonId) { mutableStateOf(false) }
    var completionSaved by remember(lessonId) { mutableStateOf(false) }

    val explanationEvaluation = evaluateExplanation(userExplanation, lesson.explainKeywords)
    val missionChanged = canonicalizeCode(missionCode) != canonicalizeCode(lesson.initialPracticeCode)
    val missionPassed = missionAttempted && missionChanged && missionResult?.isSuccess == true

    fun canAdvance(step: Int): Boolean = when (step) {
        1 -> true
        2 -> predictionSubmitted
        3 -> readingSubmitted
        4 -> practiceAttempted
        5 -> fillBlankCorrect
        6 -> shortCodeCorrect
        7 -> debugCorrect
        8 -> aiCorrect
        9 -> missionPassed
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${lesson.moduleTitle} · Level ${lesson.stepNumber}/${lesson.stepTotal}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${lesson.lessonId} · 학습 Step $currentStep/10", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..10) {
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
                Text("약 ${lesson.estimatedMinutes}분 · 한 레슨에서도 읽기만 하지 않고 9번 이상 직접 판단/입력/수정", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            when (currentStep) {
                1 -> LearningCard("Step 1. 개념을 작은 단위로 이해") {
                    Text(lesson.explanation, fontSize = 14.sp, lineHeight = 23.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("이번 Level 졸업 기준", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(lesson.expectedOutcome, fontSize = 14.sp, lineHeight = 21.sp)
                }

                2 -> LearningCard("Step 2. 실행 전에 결과 예상") {
                    Text("실행하지 말고 먼저 결과를 예상하세요. 입력→처리→출력 순서로 적으면 됩니다.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    CodeBox(lesson.codeSample)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = predictionText,
                        onValueChange = { predictionText = it; predictionSubmitted = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("내 예상") }, minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { predictionSubmitted = predictionText.trim().length >= 10 }, enabled = predictionText.trim().length >= 10) { Text("예상 제출") }
                    if (predictionSubmitted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FeedbackBox(true, "이제 실제 학습 목표와 비교하세요: ${lesson.expectedOutcome}")
                    }
                }

                3 -> LearningCard("Step 3. 코드/구조 읽기 문제") {
                    Text("아래 예제에서 이 레슨의 핵심 역할을 하는 줄이나 구조를 찾아, 왜 중요한지 15자 이상으로 적으세요.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeBox(lesson.codeSample)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = readingText,
                        onValueChange = { readingText = it; readingSubmitted = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("핵심 부분 + 이유") }, minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { readingSubmitted = readingText.trim().length >= 15 }, enabled = readingText.trim().length >= 15) { Text("읽기 답안 제출") }
                    if (readingSubmitted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(true, "비교 포인트: ${lesson.expectedOutcome}")
                    }
                }

                4 -> LearningCard("Step 4. 한 부분 수정하고 실제 실행") {
                    Text("예제의 값/조건/문장을 최소 한 곳 직접 바꾸고 실행하세요. 실패한 실행도 오답노트에 남겨 학습에 사용합니다.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeInput, onValueChange = { codeInput = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("실습 코드 · ${lesson.practiceLanguage}") }, textStyle = CodeTypography, minLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                practiceAttempted = true
                                val result = sandboxEngine.execute(languageFor(lesson.practiceLanguage), codeInput)
                                executionResult = result
                                if (!result.isSuccess) onRecordErrorNote(
                                    "${lesson.lessonId}_PRACTICE_ERROR", codeInput, result.errorMessage ?: "실행 오류",
                                    "오류 메시지와 발생 지점을 읽고 가장 작은 수정부터 시도하세요."
                                )
                            }
                        }) { Text("▶ 실행") }
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
                    }
                }

                5 -> LearningCard("Step 5. 핵심 개념 빈칸 문제") {
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
                        }, enabled = fillBlankInput.trim().isNotEmpty()
                    ) { Text("정답 확인") }
                    if (fillBlankChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(fillBlankCorrect, if (fillBlankCorrect) "정답입니다." else "오답입니다. 다시 시도하세요. (${fillBlankAttempts}회)")
                    }
                }

                6 -> LearningCard("Step 6. 보지 않고 짧게 다시 작성") {
                    Text("Step 4에서 봤던 기본 실습 코드를 기억해서 다시 작성하세요. 공백과 주석은 달라도 되지만 핵심 코드는 같아야 합니다.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shortCodeInput,
                        onValueChange = { shortCodeInput = it; shortCodeChecked = false; shortCodeCorrect = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("기억에서 다시 작성") }, textStyle = CodeTypography, minLines = 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        shortCodeAttempts++
                        shortCodeChecked = true
                        shortCodeCorrect = isDebugFixCorrect(shortCodeInput, lesson.initialPracticeCode)
                        if (!shortCodeCorrect) onRecordErrorNote(
                            "${lesson.lessonId}_SHORT_CODE", shortCodeInput, "직접 작성 문제 오답",
                            if (shortCodeAttempts == 1) "핵심 변수와 실행 줄을 떠올려보세요." else "구조와 값의 순서를 다시 확인하세요."
                        )
                    }, enabled = shortCodeInput.trim().isNotEmpty()) { Text("작성 코드 검사") }
                    if (shortCodeChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(shortCodeCorrect, if (shortCodeCorrect) "핵심 코드 재작성 성공." else "아직 다릅니다. (${shortCodeAttempts}회)")
                    }
                    if (!shortCodeCorrect && shortCodeAttempts >= 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showShortAnswer = !showShortAnswer }) { Text(if (showShortAnswer) "예시 숨기기" else "2회 실패 · 예시 다시 보기") }
                    }
                    if (showShortAnswer) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBox(lesson.initialPracticeCode)
                        Text("예시를 본 뒤 입력칸에 다시 직접 작성하고 검사해야 통과됩니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                7 -> LearningCard("Step 7. 일부러 고장난 예제 디버깅") {
                    Text("정답을 처음부터 보여주지 않습니다. 원인을 먼저 생각하고 코드를 직접 고치세요.", fontSize = 13.sp)
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
                        Text("정답을 본 뒤에도 직접 수정하고 다시 검사해야 통과됩니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                8 -> LearningCard("Step 8. AI의 그럴듯한 답 판별") {
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
                                "문장의 자신감이 아니라 사실·구조·검증 가능한 근거를 다시 보세요."
                            )
                        }, enabled = selectedOption >= 0
                    ) { Text("선택한 답 채점") }
                    if (aiChecked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(aiCorrect, if (aiCorrect) "정답입니다." else "오답입니다. 근거를 다시 확인하세요.")
                    }
                }

                9 -> LearningCard("Step 9. 작은 응용 미션") {
                    Text("기본 실습 코드를 그대로 실행하지 말고 최소 한 곳을 바꾸세요. 숫자·문자·조건·HTML 내용 중 하나를 바꾼 뒤 실행해, 같은 구조가 새 상황에서도 동작하는지 확인합니다.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = missionCode,
                        onValueChange = { missionCode = it; missionAttempted = false; missionResult = null },
                        modifier = Modifier.fillMaxWidth(), label = { Text("응용 미션 코드 · 원본에서 반드시 변경") }, textStyle = CodeTypography, minLines = 5
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (missionChanged) "원본과 다른 수정이 감지되었습니다 ✓" else "아직 원본과 같습니다. 최소 한 곳을 바꾸세요.", fontSize = 12.sp, color = if (missionChanged) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            missionAttempted = true
                            val result = sandboxEngine.execute(languageFor(lesson.practiceLanguage), missionCode)
                            missionResult = result
                            if (!result.isSuccess) onRecordErrorNote(
                                "${lesson.lessonId}_MISSION", missionCode, result.errorMessage ?: "응용 미션 실행 오류",
                                "원본과 바꾼 부분 하나만 비교하고 오류가 생긴 지점을 좁혀보세요."
                            )
                        }
                    }, enabled = missionChanged) { Text("응용 코드 실행") }
                    missionResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(result.isSuccess, if (result.isSuccess) "응용 실행 성공. 다음 단계로 이동할 수 있습니다." else "실행 실패: ${result.errorMessage ?: "오류"}")
                    }
                }

                10 -> LearningCard("Step 10. 자기 말로 설명하고 숙련 확인") {
                    Text(lesson.explainPrompt, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userExplanation,
                        onValueChange = { userExplanation = it; explanationChecked = false },
                        modifier = Modifier.fillMaxWidth(), label = { Text("내 언어로 3문장 이상 설명") }, minLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { explanationChecked = true }, enabled = userExplanation.trim().isNotEmpty()) { Text("설명 점검") }
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
                    Text("Level 완주 조건", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• 결과 예상 제출 ${mark(predictionSubmitted)}", fontSize = 12.sp)
                    Text("• 구조 읽기 답안 ${mark(readingSubmitted)}", fontSize = 12.sp)
                    Text("• 직접 실행 ${mark(practiceAttempted)}", fontSize = 12.sp)
                    Text("• 빈칸 정답 ${mark(fillBlankCorrect)}", fontSize = 12.sp)
                    Text("• 짧은 코드 재작성 ${mark(shortCodeCorrect)}", fontSize = 12.sp)
                    Text("• 디버깅 해결 ${mark(debugCorrect)}", fontSize = 12.sp)
                    Text("• AI 판별 정답 ${mark(aiCorrect)}", fontSize = 12.sp)
                    Text("• 응용 미션 성공 ${mark(missionPassed)}", fontSize = 12.sp)
                    Text("• 자기 설명 핵심개념 50% 이상 ${mark(explanationEvaluation.passed)}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onStepCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber); completionSaved = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = predictionSubmitted && readingSubmitted && practiceAttempted && fillBlankCorrect && shortCodeCorrect && debugCorrect && aiCorrect && missionPassed && explanationEvaluation.passed && !completionSaved
                    ) { Text(if (completionSaved) "완주 저장 완료" else "Level ${lesson.stepNumber} 완주 저장") }
                    if (completionSaved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedbackBox(true, "저장 완료. 다음 Level이 해금되고 이 개념은 1·3·7·14일 복습 대상으로 등록됩니다.")
                    }
                }
            }

            if (currentStep < 10) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { if (currentStep > 1) currentStep-- }, enabled = currentStep > 1) { Text("← 이전") }
                    Button(onClick = { if (currentStep < 10) currentStep++ }, enabled = canAdvance(currentStep)) { Text("다음 →") }
                }
                if (!canAdvance(currentStep) && currentStep > 1) {
                    Text(
                        when (currentStep) {
                            2 -> "결과 예상을 10자 이상 적어야 합니다."
                            3 -> "코드/구조 읽기 답안을 15자 이상 적어야 합니다."
                            4 -> "코드를 최소 1회 직접 실행해야 합니다."
                            5 -> "빈칸 문제를 맞혀야 합니다."
                            6 -> "기본 실습 코드를 직접 다시 작성해 맞혀야 합니다."
                            7 -> "디버깅 문제를 해결해야 합니다."
                            8 -> "AI 판별 문제를 맞혀야 합니다."
                            9 -> "원본을 바꾼 응용 코드를 실제로 성공 실행해야 합니다."
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
