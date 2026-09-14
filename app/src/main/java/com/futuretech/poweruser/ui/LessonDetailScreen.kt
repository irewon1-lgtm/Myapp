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

    val lesson = remember(lessonId) {
        CurriculumDataRepository.beginnerLessons.find { it.lessonId == lessonId }
            ?: CurriculumDataRepository.intermediateLessons.find { it.lessonId == lessonId }
            ?: CurriculumDataRepository.beginnerLessons.first()
    }

    val fillBlankQuestion = remember(lesson.fillInBlankPrompt) {
        parseFillBlankPrompt(lesson.fillInBlankPrompt)
    }

    var currentStep by remember(lessonId) { mutableStateOf(1) }
    var predictionText by remember(lessonId) { mutableStateOf("") }
    var predictionSubmitted by remember(lessonId) { mutableStateOf(false) }

    var fillBlankInput by remember(lessonId) { mutableStateOf("") }
    var fillBlankCorrect by remember(lessonId) { mutableStateOf(false) }
    var fillBlankChecked by remember(lessonId) { mutableStateOf(false) }
    var fillBlankAttempts by remember(lessonId) { mutableStateOf(0) }

    var codeInput by remember(lessonId) {
        mutableStateOf(if (lesson.lessonId == "I01") lesson.codeSample else lesson.initialPracticeCode)
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
    var completionSaved by remember(lessonId) { mutableStateOf(false) }

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
                    Text(
                        "${lesson.lessonId} · Step $currentStep/7",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← 목록으로", color = MaterialTheme.colorScheme.primary)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (i <= currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Text(
                text = lesson.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            when (currentStep) {
                1 -> {
                    LearningCard("Step 1. 핵심 개념 이해") {
                        Text(lesson.explanation, fontSize = 14.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "학습 목표: ${lesson.expectedOutcome}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                2 -> {
                    LearningCard("Step 2. 결과 먼저 예상하기") {
                        Text(
                            "정답을 보기 전에 아래 구조/코드가 무엇을 하게 될지 본인 말로 먼저 예상하세요.",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CodeBox(lesson.codeSample)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = predictionText,
                            onValueChange = {
                                predictionText = it
                                predictionSubmitted = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("내 예상") },
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { predictionSubmitted = predictionText.trim().isNotEmpty() },
                            enabled = predictionText.trim().isNotEmpty()
                        ) {
                            Text("예상 제출")
                        }
                        if (predictionSubmitted) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FeedbackBox(success = true, text = "비교 기준: ${lesson.expectedOutcome}")
                            Text(
                                "이 단계는 자동 정답 판정이 아니라, 실행 전 예측 습관을 만드는 단계입니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                3 -> {
                    LearningCard("Step 3. 빈칸 문제") {
                        Text(fillBlankQuestion.displayText, fontSize = 14.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = fillBlankInput,
                            onValueChange = {
                                fillBlankInput = it
                                fillBlankChecked = false
                                fillBlankCorrect = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("정답 입력") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                fillBlankAttempts++
                                fillBlankChecked = true
                                fillBlankCorrect = isFillBlankCorrect(
                                    fillBlankInput,
                                    fillBlankQuestion.acceptedAnswers
                                )
                                if (!fillBlankCorrect) {
                                    onRecordErrorNote(
                                        "${lesson.lessonId}_FILL_BLANK",
                                        fillBlankInput,
                                        "빈칸 문제 오답",
                                        "정답을 바로 보지 말고 개념 설명과 학습 목표를 다시 확인하세요."
                                    )
                                }
                            },
                            enabled = fillBlankInput.trim().isNotEmpty()
                        ) {
                            Text("정답 확인")
                        }
                        if (fillBlankChecked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FeedbackBox(
                                success = fillBlankCorrect,
                                text = if (fillBlankCorrect) "정답입니다. 다음 단계로 진행할 수 있습니다."
                                else "오답입니다. 다시 생각해보세요. (시도 $fillBlankAttempts회)"
                            )
                        }
                    }
                }

                4 -> {
                    LearningCard("Step 4. 직접 수정하고 실행하기") {
                        Text(
                            "코드를 직접 바꿔본 뒤 실행하세요. 이 단계는 실행을 실제로 시도해야 넘어갈 수 있습니다.",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("실습 코드") },
                            textStyle = CodeTypography,
                            minLines = 5
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        practiceAttempted = true
                                        val language = when (lesson.lessonId) {
                                            "I01" -> ProgrammingLanguage.HTML_JS
                                            "I02" -> ProgrammingLanguage.TYPESCRIPT
                                            "I04" -> ProgrammingLanguage.SQL
                                            else -> ProgrammingLanguage.PYTHON
                                        }
                                        val result = sandboxEngine.execute(language, codeInput)
                                        executionResult = result
                                        if (!result.isSuccess) {
                                            onRecordErrorNote(
                                                "${lesson.lessonId}_PRACTICE_ERROR",
                                                codeInput,
                                                result.errorMessage ?: "실행 오류",
                                                "오류 메시지를 읽고 입력 코드의 문법과 구조를 다시 확인하세요."
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text("▶ 실행")
                            }

                            OutlinedButton(
                                onClick = { if (currentHintStep < 3) currentHintStep++ }
                            ) {
                                Text("💡 힌트 $currentHintStep/3")
                            }
                        }

                        if (currentHintStep > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val hint = when (currentHintStep) {
                                1 -> lesson.hintLevel1
                                2 -> lesson.hintLevel2
                                else -> lesson.hintLevel3
                            }
                            FeedbackBox(success = true, text = "힌트: $hint")
                        }

                        if (lesson.lessonId == "I01") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("HTML/CSS/JS 미리보기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            HtmlPreviewSandboxView(htmlContent = codeInput)
                        }

                        executionResult?.let { result ->
                            Spacer(modifier = Modifier.height(10.dp))
                            FeedbackBox(
                                success = result.isSuccess,
                                text = if (result.isSuccess) {
                                    "실행 결과:\n${result.output.ifEmpty { "출력 없음" }}"
                                } else {
                                    "실행 오류:\n${result.errorMessage ?: "알 수 없는 오류"}"
                                }
                            )
                            Text(
                                "※ 현재 일부 언어는 학습용 제한 실행기입니다. 실행 성공 여부만으로 이해도를 판정하지 않습니다.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                5 -> {
                    LearningCard("Step 5. 고장난 코드 직접 고치기") {
                        Text(
                            "정답은 처음부터 보여주지 않습니다. 아래 코드를 직접 수정한 뒤 검사하세요.",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = debugInput,
                            onValueChange = {
                                debugInput = it
                                debugChecked = false
                                debugCorrect = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("수정할 코드") },
                            textStyle = CodeTypography,
                            minLines = 4
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                debugAttempts++
                                debugChecked = true
                                debugCorrect = isDebugFixCorrect(debugInput, lesson.brokenCodeFix)
                                if (!debugCorrect) {
                                    onRecordErrorNote(
                                        "${lesson.lessonId}_DEBUG",
                                        debugInput,
                                        "디버깅 문제 오답",
                                        if (debugAttempts == 1) lesson.hintLevel1 else lesson.hintLevel2
                                    )
                                }
                            }
                        ) {
                            Text("수정안 검사")
                        }

                        if (debugChecked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FeedbackBox(
                                success = debugCorrect,
                                text = if (debugCorrect) "수정 성공. 정답입니다."
                                else "아직 해결되지 않았습니다. (시도 $debugAttempts회)"
                            )
                        }

                        if (!debugCorrect && debugAttempts >= 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { showDebugAnswer = !showDebugAnswer }) {
                                Text(if (showDebugAnswer) "정답 숨기기" else "2회 실패 · 정답 보기")
                            }
                        }

                        if (showDebugAnswer) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBox(lesson.brokenCodeFix)
                            Text(
                                "정답을 본 뒤 그대로 넘기지 말고, 위 입력칸을 직접 수정한 후 다시 검사해야 통과됩니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                6 -> {
                    LearningCard("Step 6. AI 답변 판별 문제") {
                        Text(lesson.aiHallucinationQuestion, fontSize = 14.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        lesson.aiHallucinationOptions.forEachIndexed { index, optionText ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedOption == index,
                                    onClick = {
                                        selectedOption = index
                                        aiChecked = false
                                        aiCorrect = false
                                    }
                                )
                                Text(optionText, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                aiChecked = true
                                aiCorrect = selectedOption == lesson.correctOptionIndex
                                if (!aiCorrect) {
                                    onRecordErrorNote(
                                        "${lesson.lessonId}_AI_JUDGEMENT",
                                        selectedOption.toString(),
                                        "AI 판별 객관식 오답",
                                        "문제의 사실 주장과 검증 가능한 근거를 구분해서 다시 판단하세요."
                                    )
                                }
                            },
                            enabled = selectedOption >= 0
                        ) {
                            Text("선택한 답 채점")
                        }
                        if (aiChecked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FeedbackBox(
                                success = aiCorrect,
                                text = if (aiCorrect) "정답입니다."
                                else "오답입니다. 다른 선택지를 다시 검토하세요."
                            )
                        }
                    }
                }

                7 -> {
                    LearningCard("Step 7. 본인 말로 설명하고 완주") {
                        Text(lesson.explainPrompt, fontSize = 14.sp, lineHeight = 22.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userExplanation,
                            onValueChange = { userExplanation = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("내 언어로 설명") },
                            minLines = 4
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("완주 조건", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("• 빈칸 문제 정답 ${if (fillBlankCorrect) "✓" else "✗"}", fontSize = 12.sp)
                        Text("• 코드 실행 시도 ${if (practiceAttempted) "✓" else "✗"}", fontSize = 12.sp)
                        Text("• 디버깅 문제 정답 ${if (debugCorrect) "✓" else "✗"}", fontSize = 12.sp)
                        Text("• AI 판별 문제 정답 ${if (aiCorrect) "✓" else "✗"}", fontSize = 12.sp)
                        Text("• 자기 설명 20자 이상 ${if (userExplanation.trim().length >= 20) "✓" else "✗"}", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "자기 설명은 글자 수만으로 '우수'라고 자동 판정하지 않습니다. 핵심 객관 문제를 통과했는지와 별도로 저장합니다.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onStepCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber)
                                completionSaved = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = fillBlankCorrect && practiceAttempted && debugCorrect && aiCorrect &&
                                userExplanation.trim().length >= 20 && !completionSaved
                        ) {
                            Text(if (completionSaved) "완주 저장 완료" else "레슨 완주 저장")
                        }

                        if (completionSaved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FeedbackBox(
                                success = true,
                                text = "완주 기록이 저장되었습니다. 이 레슨은 간격 반복 복습 대상으로 등록됩니다."
                            )
                        }
                    }
                }
            }

            if (currentStep < 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (currentStep > 1) currentStep-- },
                        enabled = currentStep > 1
                    ) {
                        Text("← 이전")
                    }

                    Button(
                        onClick = { if (currentStep < 7) currentStep++ },
                        enabled = canAdvance(currentStep)
                    ) {
                        Text("다음 →")
                    }
                }

                if (!canAdvance(currentStep) && currentStep > 1) {
                    Text(
                        when (currentStep) {
                            2 -> "예상을 먼저 제출해야 다음 단계로 갈 수 있습니다."
                            3 -> "빈칸 문제를 맞혀야 다음 단계로 갈 수 있습니다."
                            4 -> "코드 실행을 최소 1회 시도해야 다음 단계로 갈 수 있습니다."
                            5 -> "디버깅 문제를 해결해야 다음 단계로 갈 수 있습니다."
                            6 -> "AI 판별 문제를 맞혀야 다음 단계로 갈 수 있습니다."
                            else -> ""
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("← 이전 단계")
                }
            }
        }
    }
}

@Composable
private fun LearningCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun CodeBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text, style = CodeTypography, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun FeedbackBox(success: Boolean, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (success) MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            color = if (success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
        )
    }
}
