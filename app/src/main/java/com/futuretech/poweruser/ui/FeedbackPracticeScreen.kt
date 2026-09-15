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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.education.FeedbackVerdict
import com.futuretech.poweruser.education.PracticeFeedback
import com.futuretech.poweruser.education.PracticeFeedbackEngine
import com.futuretech.poweruser.sandbox.ExecutionResult
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.components.AiLearningPanel
import com.futuretech.poweruser.ui.components.HtmlPreviewSandboxView
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackPracticeScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onReviewLecture: (Int) -> Unit,
    onStepCompleted: (String, String, Int) -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SandboxedExecutionEngine(context) }
    val lesson = remember(lessonId) {
        CurriculumDataRepository.lessonById(lessonId) ?: CurriculumDataRepository.beginnerLessons.first()
    }
    val fillQuestion = remember(lesson.fillInBlankPrompt) { parseFillBlankPrompt(lesson.fillInBlankPrompt) }

    var step by remember(lessonId) { mutableStateOf(1) }
    var prediction by remember(lessonId) { mutableStateOf("") }
    var predictionSubmitted by remember(lessonId) { mutableStateOf(false) }
    var predictionFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }
    var reading by remember(lessonId) { mutableStateOf("") }
    var readingSubmitted by remember(lessonId) { mutableStateOf(false) }
    var readingFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }

    var practiceCode by remember(lessonId) {
        mutableStateOf(if (lesson.practiceLanguage == "HTML_JS") lesson.codeSample else lesson.initialPracticeCode)
    }
    var practiceResult by remember(lessonId) { mutableStateOf<ExecutionResult?>(null) }
    var practiceFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }
    var hintLevel by remember(lessonId) { mutableStateOf(0) }

    var fillInput by remember(lessonId) { mutableStateOf("") }
    var fillCorrect by remember(lessonId) { mutableStateOf(false) }
    var fillAttempts by remember(lessonId) { mutableStateOf(0) }
    var fillFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }

    var shortCode by remember(lessonId) { mutableStateOf("") }
    var shortCorrect by remember(lessonId) { mutableStateOf(false) }
    var shortAttempts by remember(lessonId) { mutableStateOf(0) }
    var shortFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }
    var showShortAnswer by remember(lessonId) { mutableStateOf(false) }

    var debugCode by remember(lessonId) { mutableStateOf(lesson.brokenCode) }
    var debugCorrect by remember(lessonId) { mutableStateOf(false) }
    var debugAttempts by remember(lessonId) { mutableStateOf(0) }
    var debugFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }
    var showDebugAnswer by remember(lessonId) { mutableStateOf(false) }

    var selectedOption by remember(lessonId) { mutableStateOf(-1) }
    var aiCorrect by remember(lessonId) { mutableStateOf(false) }
    var aiFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }

    var missionCode by remember(lessonId) { mutableStateOf(lesson.initialPracticeCode) }
    var missionResult by remember(lessonId) { mutableStateOf<ExecutionResult?>(null) }
    var missionFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }

    var explanation by remember(lessonId) { mutableStateOf("") }
    var explanationFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }
    var completionSaved by remember(lessonId) { mutableStateOf(false) }

    val explanationEval = evaluateExplanation(explanation, lesson.explainKeywords)
    val missionChanged = canonicalizeCode(missionCode) != canonicalizeCode(lesson.initialPracticeCode)
    val missionPassed = missionChanged && missionResult?.isSuccess == true

    fun canAdvance(current: Int): Boolean = when (current) {
        1 -> true
        2 -> predictionSubmitted
        3 -> readingSubmitted
        4 -> practiceResult?.isSuccess == true
        5 -> fillCorrect
        6 -> shortCorrect
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
                        Text("${lesson.lessonId} · 문제/실습 $step/10", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 목록") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..10) {
                    Box(
                        Modifier.weight(1f).height(6.dp).background(
                            if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .3f),
                            RoundedCornerShape(3.dp)
                        )
                    )
                }
            }

            Column {
                Text(lesson.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("강의를 모두 본 뒤 진행하는 문제·실습입니다. 제출 후에는 정답 여부와 이유를 확인합니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }

            when (step) {
                1 -> PracticeCard("Step 1. 강의 핵심 30초 복습") {
                    Text(lesson.explanation, fontSize = 14.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("이번 Level 목표", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(lesson.expectedOutcome, fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onReviewLecture(11) }) { Text("핵심 정리 강의 다시보기") }
                }

                2 -> PracticeCard("Step 2. 실행 전에 결과 예상") {
                    Text("먼저 예상하세요. 이 단계는 자유서술이라 단순 글자 수로 ‘정답’ 처리하지 않고, 제출 후 기준 답안과 비교합니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp)); CodeBoxV2(lesson.codeSample); Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        prediction,
                        { prediction = it; predictionSubmitted = false; predictionFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("내 예상") }, minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            predictionSubmitted = true
                            predictionFeedback = PracticeFeedbackEngine.predictionReview(lesson, prediction)
                        },
                        enabled = prediction.trim().length >= 10
                    ) { Text("예상 제출·비교") }
                    predictionFeedback?.let { DetailedFeedbackCard(it, onReviewLecture) }
                }

                3 -> PracticeCard("Step 3. 코드/구조 읽기") {
                    Text("핵심 줄이나 구조를 하나 고르고 왜 중요한지 설명하세요. 제출 후 모범 기준과 비교합니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp)); CodeBoxV2(lesson.codeSample); Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        reading,
                        { reading = it; readingSubmitted = false; readingFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("핵심 부분 + 이유") }, minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            readingSubmitted = true
                            readingFeedback = PracticeFeedbackEngine.readingReview(lesson, reading)
                        },
                        enabled = reading.trim().length >= 15
                    ) { Text("읽기 답안 제출·비교") }
                    readingFeedback?.let { DetailedFeedbackCard(it, onReviewLecture) }
                }

                4 -> PracticeCard("Step 4. 수정하고 실제 실행") {
                    Text("값·조건·문장을 최소 한 곳 직접 바꾸고 실행하세요. 이제는 실행 실패 상태로 다음 단계에 갈 수 없습니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        practiceCode,
                        { practiceCode = it; practiceResult = null; practiceFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("실습 코드 · ${lesson.practiceLanguage}") }, textStyle = CodeTypography, minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val r = engine.execute(languageForV2(lesson.practiceLanguage), practiceCode)
                                practiceResult = r
                                val f = PracticeFeedbackEngine.execution(lesson, practiceCode, r)
                                practiceFeedback = f
                                if (!r.isSuccess) onRecordErrorNote(
                                    "${lesson.lessonId}_PRACTICE_ERROR", practiceCode, r.errorMessage ?: "실행 오류", "${f.why}\n${f.retryGuidance}"
                                )
                            }
                        }) { Text("▶ 실제 실행") }
                        OutlinedButton(onClick = { if (hintLevel < 3) hintLevel++ }) { Text("💡 힌트 $hintLevel/3") }
                    }
                    if (hintLevel > 0) {
                        Spacer(Modifier.height(8.dp))
                        val hint = when (hintLevel) { 1 -> lesson.hintLevel1; 2 -> lesson.hintLevel2; else -> lesson.hintLevel3 }
                        NeutralBox("힌트", hint)
                    }
                    if (lesson.practiceLanguage == "HTML_JS") {
                        Spacer(Modifier.height(10.dp)); HtmlPreviewSandboxView(htmlContent = practiceCode)
                    }
                    practiceFeedback?.let { DetailedFeedbackCard(it, onReviewLecture) }
                }

                5 -> PracticeCard("Step 5. 핵심 개념 빈칸") {
                    Text(fillQuestion.displayText, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        fillInput,
                        { fillInput = it; fillCorrect = false; fillFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("정답 입력") }, singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        fillAttempts++
                        val f = PracticeFeedbackEngine.fillBlank(lesson, fillInput, fillQuestion.acceptedAnswers)
                        fillFeedback = f
                        fillCorrect = f.verdict == FeedbackVerdict.CORRECT
                        if (!fillCorrect) onRecordErrorNote(
                            "${lesson.lessonId}_FILL_BLANK", fillInput, "빈칸 문제 오답", "${f.why}\n${f.retryGuidance}"
                        )
                    }, enabled = fillInput.isNotBlank()) { Text("정답 확인") }
                    fillFeedback?.let { DetailedFeedbackCard(it, onReviewLecture, "feedback_fill") }
                    if (!fillCorrect && fillAttempts > 0) Text("시도 횟수: ${fillAttempts}회", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                6 -> PracticeCard("Step 6. 보지 않고 짧게 다시 작성") {
                    Text("강의에서 본 기본 실습 구조를 기억해서 다시 작성하세요. 틀리면 어느 핵심 줄이 다른지 보여줍니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        shortCode,
                        { shortCode = it; shortCorrect = false; shortFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("기억에서 다시 작성") }, textStyle = CodeTypography, minLines = 4
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        shortAttempts++
                        val f = PracticeFeedbackEngine.codeRewrite(lesson, shortCode, lesson.initialPracticeCode, shortAttempts)
                        shortFeedback = f
                        shortCorrect = f.verdict == FeedbackVerdict.CORRECT
                        if (!shortCorrect) onRecordErrorNote(
                            "${lesson.lessonId}_SHORT_CODE", shortCode, "직접 작성 문제 오답", "${f.why}\n${f.retryGuidance}"
                        )
                    }, enabled = shortCode.isNotBlank()) { Text("작성 코드 검사") }
                    shortFeedback?.let { DetailedFeedbackCard(it, onReviewLecture, "feedback_short") }
                    if (!shortCorrect && shortAttempts >= 2) {
                        OutlinedButton(onClick = { showShortAnswer = !showShortAnswer }) { Text(if (showShortAnswer) "예시 숨기기" else "2회 실패 · 예시 다시 보기") }
                    }
                    if (showShortAnswer) {
                        CodeBoxV2(lesson.initialPracticeCode)
                        Text("본 뒤에도 직접 다시 입력하고 검사해야 통과합니다.", fontSize = 11.sp)
                    }
                }

                7 -> PracticeCard("Step 7. 고장난 예제 디버깅") {
                    Text("원인을 생각하고 직접 고치세요. 오답이면 내 코드와 목표 수정의 첫 차이를 설명합니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        debugCode,
                        { debugCode = it; debugCorrect = false; debugFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("고장난 코드 수정") }, textStyle = CodeTypography, minLines = 4
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        debugAttempts++
                        val f = PracticeFeedbackEngine.debug(lesson, debugCode, debugAttempts)
                        debugFeedback = f
                        debugCorrect = f.verdict == FeedbackVerdict.CORRECT
                        if (!debugCorrect) onRecordErrorNote(
                            "${lesson.lessonId}_DEBUG", debugCode, "디버깅 문제 오답", "${f.why}\n${f.retryGuidance}"
                        )
                    }) { Text("수정안 검사") }
                    debugFeedback?.let { DetailedFeedbackCard(it, onReviewLecture, "feedback_debug") }
                    if (!debugCorrect && debugAttempts >= 2) {
                        OutlinedButton(onClick = { showDebugAnswer = !showDebugAnswer }) { Text(if (showDebugAnswer) "정답 숨기기" else "정답 가이드 보기") }
                    }
                    if (showDebugAnswer) {
                        CodeBoxV2(lesson.brokenCodeFix)
                        Text("정답을 본 뒤에도 직접 수정하고 다시 검사해야 통과합니다.", fontSize = 11.sp)
                    }
                }

                8 -> PracticeCard("Step 8. AI의 그럴듯한 답 판별") {
                    Text(lesson.aiHallucinationQuestion, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    lesson.aiHallucinationOptions.forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedOption == index, onClick = { selectedOption = index; aiCorrect = false; aiFeedback = null })
                            Text(option, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val f = PracticeFeedbackEngine.aiJudgement(lesson, selectedOption)
                        aiFeedback = f
                        aiCorrect = f.verdict == FeedbackVerdict.CORRECT
                        if (!aiCorrect) onRecordErrorNote(
                            "${lesson.lessonId}_AI_JUDGEMENT", selectedOption.toString(), "AI 판별 문제 오답", "${f.why}\n${f.retryGuidance}"
                        )
                    }, enabled = selectedOption >= 0) { Text("선택한 답 채점") }
                    aiFeedback?.let { DetailedFeedbackCard(it, onReviewLecture, "feedback_ai") }
                }

                9 -> PracticeCard("Step 9. 작은 응용 미션") {
                    Text("원본을 그대로 돌리지 말고 한 곳을 목적 있게 바꾼 뒤 실행하세요.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        missionCode,
                        { missionCode = it; missionResult = null; missionFeedback = PracticeFeedbackEngine.mission(lesson, it, canonicalizeCode(it) != canonicalizeCode(lesson.initialPracticeCode), null) },
                        Modifier.fillMaxWidth(), label = { Text("응용 미션 코드") }, textStyle = CodeTypography, minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val r = engine.execute(languageForV2(lesson.practiceLanguage), missionCode)
                            missionResult = r
                            val f = PracticeFeedbackEngine.mission(lesson, missionCode, missionChanged, r)
                            missionFeedback = f
                            if (!r.isSuccess) onRecordErrorNote(
                                "${lesson.lessonId}_MISSION", missionCode, r.errorMessage ?: "응용 미션 오류", "${f.why}\n${f.retryGuidance}"
                            )
                        }
                    }, enabled = missionChanged) { Text("응용 코드 실행") }
                    missionFeedback?.let { DetailedFeedbackCard(it, onReviewLecture, "feedback_mission") }
                }

                10 -> PracticeCard("Step 10. 자기 말로 설명") {
                    Text(lesson.explainPrompt, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        explanation,
                        { explanation = it; explanationFeedback = null },
                        Modifier.fillMaxWidth(), label = { Text("내 언어로 설명") }, minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        explanationFeedback = PracticeFeedbackEngine.explanation(
                            lesson, explanation, explanationEval.matchedKeywords, explanationEval.missingKeywords, explanationEval.passed
                        )
                    }, enabled = explanation.isNotBlank()) { Text("설명 점검") }
                    explanationFeedback?.let { DetailedFeedbackCard(it, onReviewLecture, "feedback_explanation") }
                    Spacer(Modifier.height(10.dp))
                    Text("Level 완주 조건", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• 결과 예상 제출 ${markV2(predictionSubmitted)}", fontSize = 12.sp)
                    Text("• 구조 읽기 제출 ${markV2(readingSubmitted)}", fontSize = 12.sp)
                    Text("• 실제 실행 성공 ${markV2(practiceResult?.isSuccess == true)}", fontSize = 12.sp)
                    Text("• 빈칸 정답 ${markV2(fillCorrect)}", fontSize = 12.sp)
                    Text("• 코드 재작성 ${markV2(shortCorrect)}", fontSize = 12.sp)
                    Text("• 디버깅 해결 ${markV2(debugCorrect)}", fontSize = 12.sp)
                    Text("• AI 판별 정답 ${markV2(aiCorrect)}", fontSize = 12.sp)
                    Text("• 응용 미션 성공 ${markV2(missionPassed)}", fontSize = 12.sp)
                    Text("• 자기 설명 기준 ${markV2(explanationEval.passed)}", fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onStepCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber); completionSaved = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = predictionSubmitted && readingSubmitted && practiceResult?.isSuccess == true && fillCorrect && shortCorrect && debugCorrect && aiCorrect && missionPassed && explanationEval.passed && !completionSaved
                    ) { Text(if (completionSaved) "완주 저장 완료" else "Level ${lesson.stepNumber} 완주 저장") }
                }
            }

            if (step in setOf(4, 7, 9, 10)) {
                val snapshot = when (step) { 4 -> practiceCode; 7 -> debugCode; 9 -> missionCode; else -> explanation }
                val attempted = when (step) { 4 -> practiceResult != null; 7 -> debugAttempts > 0; 9 -> missionResult != null; else -> explanationFeedback != null }
                AiLearningPanel(lesson = lesson, currentStep = step, practiceAttempted = attempted, codeSnapshot = snapshot)
            }

            if (step < 10) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { if (step > 1) step-- }, enabled = step > 1) { Text("← 이전") }
                    Button(onClick = { if (canAdvance(step)) step++ }, enabled = canAdvance(step)) { Text("다음 →") }
                }
                if (!canAdvance(step) && step > 1) {
                    Text(blockReason(step), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            } else {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) { Text("← 이전 단계") }
            }
        }
    }
}

private fun blockReason(step: Int) = when (step) {
    2 -> "예상을 적고 ‘제출·비교’를 눌러야 합니다."
    3 -> "코드 읽기 답안을 제출해야 합니다."
    4 -> "코드가 실제로 성공 실행되어야 다음 단계가 열립니다."
    5 -> "빈칸 문제를 맞혀야 합니다."
    6 -> "기본 실습 구조를 다시 작성해 맞혀야 합니다."
    7 -> "디버깅 문제를 해결해야 합니다."
    8 -> "AI 판별 문제를 맞혀야 합니다."
    9 -> "원본을 수정한 응용 코드를 성공 실행해야 합니다."
    else -> ""
}

private fun languageForV2(value: String): ProgrammingLanguage = when (value) {
    "SQL" -> ProgrammingLanguage.SQL
    "TYPESCRIPT" -> ProgrammingLanguage.TYPESCRIPT
    "HTML_JS" -> ProgrammingLanguage.HTML_JS
    else -> ProgrammingLanguage.PYTHON
}

private fun markV2(value: Boolean) = if (value) "✓" else "✗"

@Composable
private fun PracticeCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun CodeBoxV2(text: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)) {
        Text(text, Modifier.padding(12.dp), style = CodeTypography)
    }
}

@Composable
private fun NeutralBox(title: String, text: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun DetailedFeedbackCard(
    feedback: PracticeFeedback,
    onReviewLecture: (Int) -> Unit,
    testTag: String = "feedback_detail"
) {
    val color = when (feedback.verdict) {
        FeedbackVerdict.CORRECT -> MaterialTheme.colorScheme.secondary
        FeedbackVerdict.INCORRECT, FeedbackVerdict.ERROR -> MaterialTheme.colorScheme.error
        FeedbackVerdict.REVIEW -> MaterialTheme.colorScheme.tertiary
    }
    val label = when (feedback.verdict) {
        FeedbackVerdict.CORRECT -> "✓ 맞았습니다"
        FeedbackVerdict.INCORRECT -> "✗ 틀렸습니다"
        FeedbackVerdict.ERROR -> "⚠ 실행 오류"
        FeedbackVerdict.REVIEW -> "↔ 비교가 필요한 서술형"
    }
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .09f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(feedback.headline, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            FeedbackSection("내 답", feedback.learnerAnswer)
            FeedbackSection("정답/기준", feedback.correctAnswer)
            FeedbackSection("왜 그런가", feedback.why)
            FeedbackSection("어디서 헷갈렸나", feedback.misconception)
            FeedbackSection("기억할 한 문장", feedback.memoryTip)
            FeedbackSection("다시 할 때", feedback.retryGuidance)
            OutlinedButton(onClick = { onReviewLecture(feedback.reviewSectionIndex) }, modifier = Modifier.fillMaxWidth()) {
                Text("관련 강의로 돌아가서 다시 보기")
            }
        }
    }
}

@Composable
private fun FeedbackSection(title: String, text: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Text(text.ifBlank { "-" }, fontSize = 13.sp, lineHeight = 20.sp)
    }
}