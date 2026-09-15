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
import com.futuretech.poweruser.data.LessonContent
import com.futuretech.poweruser.education.EditableTestLiteral
import com.futuretech.poweruser.education.FeedbackVerdict
import com.futuretech.poweruser.education.PracticeFeedback
import com.futuretech.poweruser.education.PracticeFeedbackEngine
import com.futuretech.poweruser.education.PracticeSubmissionResult
import com.futuretech.poweruser.education.PracticeTestCaseResult
import com.futuretech.poweruser.education.PracticeTestEngine
import com.futuretech.poweruser.education.PracticeTestVisibility
import com.futuretech.poweruser.sandbox.ExecutionResult
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.components.AiLearningPanel
import com.futuretech.poweruser.ui.components.HtmlPreviewSandboxView
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

private data class RsRunState(
    val result: ExecutionResult,
    val customApplied: Boolean,
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSubmitPracticeScreen(
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
    var practiceRun by remember(lessonId) { mutableStateOf<RsRunState?>(null) }
    var practiceSubmit by remember(lessonId) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var practiceHintLevel by remember(lessonId) { mutableStateOf(0) }
    var practiceLiteralIndex by remember(lessonId) { mutableStateOf(0) }
    var practiceCustomEnabled by remember(lessonId) { mutableStateOf(false) }
    var practiceCustomValue by remember(lessonId) { mutableStateOf("") }

    var fillInput by remember(lessonId) { mutableStateOf("") }
    var fillCorrect by remember(lessonId) { mutableStateOf(false) }
    var fillAttempts by remember(lessonId) { mutableStateOf(0) }
    var fillFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }

    var shortCode by remember(lessonId) { mutableStateOf("") }
    var shortRun by remember(lessonId) { mutableStateOf<RsRunState?>(null) }
    var shortSubmit by remember(lessonId) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var shortLiteralIndex by remember(lessonId) { mutableStateOf(0) }
    var shortCustomEnabled by remember(lessonId) { mutableStateOf(false) }
    var shortCustomValue by remember(lessonId) { mutableStateOf("") }
    var showShortAnswer by remember(lessonId) { mutableStateOf(false) }

    var debugCode by remember(lessonId) { mutableStateOf(lesson.brokenCode) }
    var debugRun by remember(lessonId) { mutableStateOf<RsRunState?>(null) }
    var debugSubmit by remember(lessonId) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var debugLiteralIndex by remember(lessonId) { mutableStateOf(0) }
    var debugCustomEnabled by remember(lessonId) { mutableStateOf(false) }
    var debugCustomValue by remember(lessonId) { mutableStateOf("") }
    var showDebugAnswer by remember(lessonId) { mutableStateOf(false) }

    var selectedOption by remember(lessonId) { mutableStateOf(-1) }
    var aiCorrect by remember(lessonId) { mutableStateOf(false) }
    var aiFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }

    var missionCode by remember(lessonId) { mutableStateOf(lesson.initialPracticeCode) }
    var missionRun by remember(lessonId) { mutableStateOf<RsRunState?>(null) }
    var missionSubmit by remember(lessonId) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var missionLiteralIndex by remember(lessonId) { mutableStateOf(0) }
    var missionCustomEnabled by remember(lessonId) { mutableStateOf(false) }
    var missionCustomValue by remember(lessonId) { mutableStateOf("") }

    var explanation by remember(lessonId) { mutableStateOf("") }
    var explanationFeedback by remember(lessonId) { mutableStateOf<PracticeFeedback?>(null) }
    var completionSaved by remember(lessonId) { mutableStateOf(false) }

    val explanationEval = evaluateExplanation(explanation, lesson.explainKeywords)
    val missionChanged = canonicalizeCode(missionCode) != canonicalizeCode(lesson.initialPracticeCode)

    fun canAdvance(current: Int): Boolean = when (current) {
        1 -> true
        2 -> predictionSubmitted
        3 -> readingSubmitted
        4 -> practiceSubmit?.passed == true
        5 -> fillCorrect
        6 -> shortSubmit?.passed == true
        7 -> debugSubmit?.passed == true
        8 -> aiCorrect
        9 -> missionSubmit?.passed == true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${lesson.moduleTitle} · Level ${lesson.stepNumber}/${lesson.stepTotal}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${lesson.lessonId} · 문제/실습 $step/10",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 목록") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (i in 1..10) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (i <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = .3f),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Column {
                Text(lesson.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "▶ 실행은 자유 연습입니다. ✓ 제출을 눌렀을 때만 채점·오류 기록·진도가 바뀝니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            when (step) {
                1 -> RsPracticeCard("Step 1. 강의 핵심 30초 복습") {
                    Text(lesson.explanation, fontSize = 14.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("이번 Level 목표", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(lesson.expectedOutcome, fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onReviewLecture(11) }) { Text("핵심 정리 강의 다시보기") }
                }

                2 -> RsPracticeCard("Step 2. 실행 전에 결과 예상") {
                    Text(
                        "먼저 예상하세요. 제출 후 학습 목표와 비교하며, 글자 수만으로 정답 처리하지 않습니다.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    RsCodeBox(lesson.codeSample)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prediction,
                        onValueChange = {
                            prediction = it
                            predictionSubmitted = false
                            predictionFeedback = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("내 예상") },
                        minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            predictionSubmitted = true
                            predictionFeedback = PracticeFeedbackEngine.predictionReview(lesson, prediction)
                        },
                        enabled = prediction.trim().length >= 10
                    ) { Text("예상 제출·비교") }
                    predictionFeedback?.let { RsFeedbackCard(it, onReviewLecture) }
                }

                3 -> RsPracticeCard("Step 3. 코드/구조 읽기") {
                    Text("핵심 줄이나 구조를 하나 고르고 왜 중요한지 설명하세요.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    RsCodeBox(lesson.codeSample)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reading,
                        onValueChange = {
                            reading = it
                            readingSubmitted = false
                            readingFeedback = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("핵심 부분 + 이유") },
                        minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            readingSubmitted = true
                            readingFeedback = PracticeFeedbackEngine.readingReview(lesson, reading)
                        },
                        enabled = reading.trim().length >= 15
                    ) { Text("읽기 답안 제출·비교") }
                    readingFeedback?.let { RsFeedbackCard(it, onReviewLecture) }
                }

                4 -> RsPracticeCard("Step 4. 수정 → 실행 → 제출") {
                    Text(
                        "코드를 바꾼 뒤 ▶ 실행으로 마음껏 실험하세요. 실행 실패는 오답으로 기록되지 않습니다.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = practiceCode,
                        onValueChange = {
                            practiceCode = it
                            practiceRun = null
                            practiceSubmit = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("실습 코드 · ${lesson.practiceLanguage}") },
                        textStyle = CodeTypography,
                        minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    RsCustomTestControls(
                        code = practiceCode,
                        selectedIndex = practiceLiteralIndex,
                        customEnabled = practiceCustomEnabled,
                        customValue = practiceCustomValue,
                        onSelectedIndex = { idx, literal ->
                            practiceLiteralIndex = idx
                            practiceCustomValue = literal.displayValue
                        },
                        onEnabledChange = { practiceCustomEnabled = it },
                        onValueChange = { practiceCustomValue = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    practiceRun = rsRunOnly(
                                        engine,
                                        lesson,
                                        practiceCode,
                                        practiceLiteralIndex,
                                        practiceCustomEnabled,
                                        practiceCustomValue
                                    )
                                }
                            }
                        ) { Text("▶ 실행") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = rsSubmitOpenTask(
                                        engine = engine,
                                        lesson = lesson,
                                        code = practiceCode,
                                        baseline = if (lesson.practiceLanguage == "HTML_JS") lesson.codeSample else lesson.initialPracticeCode
                                    )
                                    practiceSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_PRACTICE_SUBMIT",
                                            practiceCode,
                                            "제출 테스트 실패",
                                            rsFailureGuide(result)
                                        )
                                    }
                                }
                            }
                        ) { Text("✓ 제출") }
                        OutlinedButton(
                            onClick = { if (practiceHintLevel < 3) practiceHintLevel++ },
                            enabled = practiceHintLevel < 3
                        ) {
                            Text(
                                if (practiceHintLevel >= 3) "💡 힌트 3/3 사용"
                                else "💡 힌트 ${practiceHintLevel + 1}/3 받기"
                            )
                        }
                    }
                    if (practiceHintLevel > 0) {
                        Spacer(Modifier.height(8.dp))
                        val hint = when (practiceHintLevel) {
                            1 -> lesson.hintLevel1
                            2 -> lesson.hintLevel2
                            else -> lesson.hintLevel3
                        }
                        RsNeutralBox("힌트 ${practiceHintLevel}/3", hint)
                    }
                    practiceRun?.let { RsRunCard(it) }
                    practiceSubmit?.let { RsSubmissionCard(it) }
                    if (lesson.practiceLanguage == "HTML_JS") {
                        Spacer(Modifier.height(10.dp))
                        HtmlPreviewSandboxView(htmlContent = practiceCode)
                    }
                }

                5 -> RsPracticeCard("Step 5. 핵심 개념 빈칸") {
                    Text(fillQuestion.displayText, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fillInput,
                        onValueChange = {
                            fillInput = it
                            fillCorrect = false
                            fillFeedback = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("정답 입력") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            fillAttempts++
                            val feedback = PracticeFeedbackEngine.fillBlank(
                                lesson,
                                fillInput,
                                fillQuestion.acceptedAnswers
                            )
                            fillFeedback = feedback
                            fillCorrect = feedback.verdict == FeedbackVerdict.CORRECT
                            if (!fillCorrect) {
                                onRecordErrorNote(
                                    "${lesson.lessonId}_FILL_BLANK",
                                    fillInput,
                                    "빈칸 문제 오답",
                                    "${feedback.why}\n${feedback.retryGuidance}"
                                )
                            }
                        },
                        enabled = fillInput.isNotBlank()
                    ) { Text("정답 확인") }
                    fillFeedback?.let { RsFeedbackCard(it, onReviewLecture) }
                    if (!fillCorrect && fillAttempts > 0) {
                        Text(
                            "제출 시도: ${fillAttempts}회",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                6 -> RsPracticeCard("Step 6. 보지 않고 직접 작성") {
                    Text(
                        "기억에서 코드를 작성합니다. ▶ 실행은 연습이고, ✓ 제출이 public + hidden test로 최종 판정합니다.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shortCode,
                        onValueChange = {
                            shortCode = it
                            shortRun = null
                            shortSubmit = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("기억에서 다시 작성") },
                        textStyle = CodeTypography,
                        minLines = 4
                    )
                    Spacer(Modifier.height(8.dp))
                    RsCustomTestControls(
                        code = shortCode,
                        selectedIndex = shortLiteralIndex,
                        customEnabled = shortCustomEnabled,
                        customValue = shortCustomValue,
                        onSelectedIndex = { idx, literal ->
                            shortLiteralIndex = idx
                            shortCustomValue = literal.displayValue
                        },
                        onEnabledChange = { shortCustomEnabled = it },
                        onValueChange = { shortCustomValue = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    shortRun = rsRunOnly(
                                        engine,
                                        lesson,
                                        shortCode,
                                        shortLiteralIndex,
                                        shortCustomEnabled,
                                        shortCustomValue
                                    )
                                }
                            },
                            enabled = shortCode.isNotBlank()
                        ) { Text("▶ 실행") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = rsSubmitReferenceTask(
                                        engine,
                                        lesson,
                                        shortCode,
                                        lesson.initialPracticeCode
                                    )
                                    shortSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_SHORT_SUBMIT",
                                            shortCode,
                                            "직접 작성 제출 실패",
                                            rsFailureGuide(result)
                                        )
                                    }
                                }
                            },
                            enabled = shortCode.isNotBlank()
                        ) { Text("✓ 제출") }
                    }
                    shortRun?.let { RsRunCard(it) }
                    shortSubmit?.let { RsSubmissionCard(it) }
                    if (shortSubmit?.passed == false) {
                        OutlinedButton(onClick = { showShortAnswer = !showShortAnswer }) {
                            Text(if (showShortAnswer) "예시 숨기기" else "예시 다시 보기")
                        }
                    }
                    if (showShortAnswer) {
                        RsCodeBox(lesson.initialPracticeCode)
                        Text("예시를 본 뒤에도 직접 다시 제출해야 통과합니다.", fontSize = 11.sp)
                    }
                }

                7 -> RsPracticeCard("Step 7. 고장난 예제 디버깅") {
                    Text(
                        "수정한 코드를 먼저 자유 실행한 뒤 제출하세요. 실행 실패만으로는 오답 기록이 생기지 않습니다.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = debugCode,
                        onValueChange = {
                            debugCode = it
                            debugRun = null
                            debugSubmit = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("고장난 코드 수정") },
                        textStyle = CodeTypography,
                        minLines = 4
                    )
                    Spacer(Modifier.height(8.dp))
                    RsCustomTestControls(
                        code = debugCode,
                        selectedIndex = debugLiteralIndex,
                        customEnabled = debugCustomEnabled,
                        customValue = debugCustomValue,
                        onSelectedIndex = { idx, literal ->
                            debugLiteralIndex = idx
                            debugCustomValue = literal.displayValue
                        },
                        onEnabledChange = { debugCustomEnabled = it },
                        onValueChange = { debugCustomValue = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    debugRun = rsRunOnly(
                                        engine,
                                        lesson,
                                        debugCode,
                                        debugLiteralIndex,
                                        debugCustomEnabled,
                                        debugCustomValue
                                    )
                                }
                            }
                        ) { Text("▶ 실행") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = rsSubmitReferenceTask(
                                        engine,
                                        lesson,
                                        debugCode,
                                        lesson.brokenCodeFix
                                    )
                                    debugSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_DEBUG_SUBMIT",
                                            debugCode,
                                            "디버깅 제출 실패",
                                            rsFailureGuide(result)
                                        )
                                    }
                                }
                            }
                        ) { Text("✓ 제출") }
                    }
                    debugRun?.let { RsRunCard(it) }
                    debugSubmit?.let { RsSubmissionCard(it) }
                    if (debugSubmit?.passed == false) {
                        OutlinedButton(onClick = { showDebugAnswer = !showDebugAnswer }) {
                            Text(if (showDebugAnswer) "정답 가이드 숨기기" else "정답 가이드 보기")
                        }
                    }
                    if (showDebugAnswer) {
                        RsCodeBox(lesson.brokenCodeFix)
                        Text("정답을 본 뒤에도 직접 수정하고 다시 제출해야 통과합니다.", fontSize = 11.sp)
                    }
                }

                8 -> RsPracticeCard("Step 8. AI의 그럴듯한 답 판별") {
                    Text(lesson.aiHallucinationQuestion, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    lesson.aiHallucinationOptions.forEachIndexed { index, option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedOption == index,
                                onClick = {
                                    selectedOption = index
                                    aiCorrect = false
                                    aiFeedback = null
                                }
                            )
                            Text(option, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val feedback = PracticeFeedbackEngine.aiJudgement(lesson, selectedOption)
                            aiFeedback = feedback
                            aiCorrect = feedback.verdict == FeedbackVerdict.CORRECT
                            if (!aiCorrect) {
                                onRecordErrorNote(
                                    "${lesson.lessonId}_AI_JUDGEMENT",
                                    selectedOption.toString(),
                                    "AI 판별 문제 오답",
                                    "${feedback.why}\n${feedback.retryGuidance}"
                                )
                            }
                        },
                        enabled = selectedOption >= 0
                    ) { Text("선택한 답 제출") }
                    aiFeedback?.let { RsFeedbackCard(it, onReviewLecture) }
                }

                9 -> RsPracticeCard("Step 9. 작은 응용 미션") {
                    Text(
                        "원본을 목적 있게 바꿔보세요. ▶ 실행으로 여러 값을 시험하고, ✓ 제출에서만 최종 판정합니다.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = missionCode,
                        onValueChange = {
                            missionCode = it
                            missionRun = null
                            missionSubmit = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("응용 미션 코드") },
                        textStyle = CodeTypography,
                        minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    RsCustomTestControls(
                        code = missionCode,
                        selectedIndex = missionLiteralIndex,
                        customEnabled = missionCustomEnabled,
                        customValue = missionCustomValue,
                        onSelectedIndex = { idx, literal ->
                            missionLiteralIndex = idx
                            missionCustomValue = literal.displayValue
                        },
                        onEnabledChange = { missionCustomEnabled = it },
                        onValueChange = { missionCustomValue = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    missionRun = rsRunOnly(
                                        engine,
                                        lesson,
                                        missionCode,
                                        missionLiteralIndex,
                                        missionCustomEnabled,
                                        missionCustomValue
                                    )
                                }
                            }
                        ) { Text("▶ 실행") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = rsSubmitOpenTask(
                                        engine,
                                        lesson,
                                        missionCode,
                                        lesson.initialPracticeCode
                                    )
                                    missionSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_MISSION_SUBMIT",
                                            missionCode,
                                            "응용 미션 제출 실패",
                                            rsFailureGuide(result)
                                        )
                                    }
                                }
                            },
                            enabled = missionChanged
                        ) { Text("✓ 제출") }
                    }
                    missionRun?.let { RsRunCard(it) }
                    missionSubmit?.let { RsSubmissionCard(it) }
                }

                10 -> RsPracticeCard("Step 10. 자기 말로 설명") {
                    Text(lesson.explainPrompt, fontSize = 14.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = {
                            explanation = it
                            explanationFeedback = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("내 언어로 설명") },
                        minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            explanationFeedback = PracticeFeedbackEngine.explanation(
                                lesson,
                                explanation,
                                explanationEval.matchedKeywords,
                                explanationEval.missingKeywords,
                                explanationEval.passed
                            )
                        },
                        enabled = explanation.isNotBlank()
                    ) { Text("설명 점검") }
                    explanationFeedback?.let { RsFeedbackCard(it, onReviewLecture) }

                    Spacer(Modifier.height(10.dp))
                    Text("Level 완주 조건", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• 결과 예상 제출 ${rsMark(predictionSubmitted)}", fontSize = 12.sp)
                    Text("• 구조 읽기 제출 ${rsMark(readingSubmitted)}", fontSize = 12.sp)
                    Text("• 수정 실습 제출 통과 ${rsMark(practiceSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• 빈칸 정답 ${rsMark(fillCorrect)}", fontSize = 12.sp)
                    Text("• 직접 작성 제출 통과 ${rsMark(shortSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• 디버깅 제출 통과 ${rsMark(debugSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• AI 판별 정답 ${rsMark(aiCorrect)}", fontSize = 12.sp)
                    Text("• 응용 미션 제출 통과 ${rsMark(missionSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• 자기 설명 기준 ${rsMark(explanationEval.passed)}", fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onStepCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber)
                            completionSaved = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = predictionSubmitted &&
                            readingSubmitted &&
                            practiceSubmit?.passed == true &&
                            fillCorrect &&
                            shortSubmit?.passed == true &&
                            debugSubmit?.passed == true &&
                            aiCorrect &&
                            missionSubmit?.passed == true &&
                            explanationEval.passed &&
                            !completionSaved
                    ) {
                        Text(if (completionSaved) "완주 저장 완료" else "Level ${lesson.stepNumber} 완주 저장")
                    }
                }
            }

            if (step in setOf(4, 7, 9, 10)) {
                val snapshot = when (step) {
                    4 -> practiceCode
                    7 -> debugCode
                    9 -> missionCode
                    else -> explanation
                }
                val submitted = when (step) {
                    4 -> practiceSubmit != null
                    7 -> debugSubmit != null
                    9 -> missionSubmit != null
                    else -> explanationFeedback != null
                }
                val deterministicEvidence = when (step) {
                    4 -> practiceSubmit?.let(::rsDeterministicEvidence).orEmpty()
                    7 -> debugSubmit?.let(::rsDeterministicEvidence).orEmpty()
                    9 -> missionSubmit?.let(::rsDeterministicEvidence).orEmpty()
                    else -> explanationFeedback?.let {
                        "앱 피드백=${it.verdict}\n근거=${it.why}"
                    }.orEmpty()
                }
                AiLearningPanel(
                    lesson = lesson,
                    currentStep = step,
                    practiceAttempted = submitted,
                    codeSnapshot = snapshot,
                    deterministicEvidence = deterministicEvidence
                )
            }

            if (step < 10) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (step > 1) step-- },
                        enabled = step > 1
                    ) { Text("← 이전") }
                    Button(
                        onClick = { if (canAdvance(step)) step++ },
                        enabled = canAdvance(step)
                    ) { Text("다음 →") }
                }
                if (!canAdvance(step) && step > 1) {
                    Text(
                        rsBlockReason(step),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("← 이전 단계") }
            }
        }
    }
}

@Composable
private fun RsCustomTestControls(
    code: String,
    selectedIndex: Int,
    customEnabled: Boolean,
    customValue: String,
    onSelectedIndex: (Int, EditableTestLiteral) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    val literals = remember(code) { PracticeTestEngine.extractEditableLiterals(code).take(4) }
    if (literals.isEmpty()) {
        RsNeutralBox(
            "테스트 값",
            "자동으로 바꿀 수 있는 값이 없습니다. 코드 안의 입력값을 직접 바꿔 ▶ 실행해도 채점 기록에는 영향을 주지 않습니다."
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = customEnabled, onCheckedChange = onEnabledChange)
            Text("실행할 때만 테스트 값 바꾸기", fontSize = 13.sp)
        }
        if (customEnabled) {
            Text(
                "아래 값은 ▶ 실행용 임시 입력입니다. 편집기의 제출 코드는 바뀌지 않습니다.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                literals.forEachIndexed { index, literal ->
                    FilterChip(
                        selected = selectedIndex.coerceIn(0, literals.lastIndex) == index,
                        onClick = { onSelectedIndex(index, literal) },
                        label = { Text(literal.displayValue.ifBlank { "빈 문자열" }, maxLines = 1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            val literal = literals[selectedIndex.coerceIn(0, literals.lastIndex)]
            OutlinedTextField(
                value = customValue,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("임시 테스트 값 · 원본 ${literal.raw}") },
                supportingText = { Text("빈 문자열·음수·큰 값 등도 직접 시험할 수 있습니다.") },
                singleLine = true
            )
        }
    }
}

private suspend fun rsRunOnly(
    engine: SandboxedExecutionEngine,
    lesson: LessonContent,
    code: String,
    selectedIndex: Int,
    customEnabled: Boolean,
    customValue: String
): RsRunState {
    var runCode = code
    var customApplied = false
    var note = "현재 편집 코드로 실행했습니다. 채점 기록에는 반영되지 않습니다."

    if (customEnabled) {
        val literals = PracticeTestEngine.extractEditableLiterals(code)
        val literal = literals.getOrNull(selectedIndex)
        if (literal == null) {
            return RsRunState(
                ExecutionResult(false, "", "선택한 테스트 값을 더 이상 찾을 수 없습니다."),
                false,
                "코드를 수정해 테스트 값 위치가 바뀌었습니다. 값을 다시 선택하세요."
            )
        }
        val overridden = PracticeTestEngine.applyUserOverride(code, literal, customValue)
        if (overridden == null) {
            return RsRunState(
                ExecutionResult(false, "", "테스트 값 형식이 원본 타입과 맞지 않습니다."),
                false,
                "숫자는 숫자로, Boolean은 true/false로 입력하세요."
            )
        }
        runCode = overridden
        customApplied = true
        note = "임시 테스트 값으로 실행했습니다. 편집 코드와 제출 상태는 변경되지 않았습니다."
    }

    val result = engine.execute(rsLanguage(lesson.practiceLanguage), runCode)
    return RsRunState(result, customApplied, note)
}

private suspend fun rsSubmitOpenTask(
    engine: SandboxedExecutionEngine,
    lesson: LessonContent,
    code: String,
    baseline: String
): PracticeSubmissionResult {
    val results = mutableListOf<PracticeTestCaseResult>()
    val changed = canonicalizeCode(code) != canonicalizeCode(baseline)
    results += PracticeTestCaseResult(
        id = "public-changed",
        visibility = PracticeTestVisibility.PUBLIC,
        label = "원본에서 목적 있게 수정",
        passed = changed,
        evidence = if (changed) "원본과 다른 제출 코드가 확인됐습니다."
        else "원본과 동일합니다. 값·조건·문장 중 하나를 목적 있게 바꾸세요."
    )

    val defaultRun = engine.execute(rsLanguage(lesson.practiceLanguage), code)
    results += PracticeTestCaseResult(
        id = "public-runtime",
        visibility = PracticeTestVisibility.PUBLIC,
        label = "기본 입력에서 실제 실행",
        passed = defaultRun.isSuccess,
        evidence = if (defaultRun.isSuccess) {
            "실제 runtime 실행 성공: ${defaultRun.output.ifBlank { "출력 없음" }.take(180)}"
        } else {
            defaultRun.errorMessage.orEmpty().ifBlank { "실행 실패" }
        }
    )

    val variants = PracticeTestEngine.edgeVariants(code)
    if (variants.isEmpty()) {
        results += PracticeTestCaseResult(
            id = "hidden-fallback",
            visibility = PracticeTestVisibility.HIDDEN,
            label = "숨은 구조 검증",
            passed = defaultRun.isSuccess,
            evidence = PracticeTestEngine.hiddenEvidence(defaultRun.isSuccess)
        )
    } else {
        variants.forEachIndexed { index, variant ->
            val edgeRun = engine.execute(rsLanguage(lesson.practiceLanguage), variant.code)
            results += PracticeTestCaseResult(
                id = "hidden-${index + 1}",
                visibility = PracticeTestVisibility.HIDDEN,
                label = "숨은 경계조건 ${index + 1}",
                passed = edgeRun.isSuccess,
                evidence = PracticeTestEngine.hiddenEvidence(edgeRun.isSuccess)
            )
        }
    }
    return PracticeSubmissionResult(results)
}

private suspend fun rsSubmitReferenceTask(
    engine: SandboxedExecutionEngine,
    lesson: LessonContent,
    code: String,
    reference: String
): PracticeSubmissionResult {
    val results = mutableListOf<PracticeTestCaseResult>()
    val learnerDefault = engine.execute(rsLanguage(lesson.practiceLanguage), code)
    val referenceDefault = engine.execute(rsLanguage(lesson.practiceLanguage), reference)
    val defaultPassed = learnerDefault.isSuccess &&
        referenceDefault.isSuccess &&
        PracticeTestEngine.normalizeOutput(learnerDefault.output) ==
        PracticeTestEngine.normalizeOutput(referenceDefault.output)

    results += PracticeTestCaseResult(
        id = "public-default",
        visibility = PracticeTestVisibility.PUBLIC,
        label = "공개 테스트 · 기본 예제 결과",
        passed = defaultPassed,
        evidence = when {
            !learnerDefault.isSuccess ->
                learnerDefault.errorMessage.orEmpty().ifBlank { "제출 코드 실행 실패" }
            !referenceDefault.isSuccess ->
                "기준 예제를 실행할 수 없어 채점할 수 없습니다."
            defaultPassed ->
                "기본 입력에서 기준 예제와 같은 결과를 냈습니다."
            else ->
                "실행은 됐지만 기준 예제와 출력이 다릅니다. 내 출력: ${learnerDefault.output.take(160)}"
        }
    )

    val variants = PracticeTestEngine.pairedEdgeVariants(code, reference)
    if (variants.isEmpty()) {
        val structural = canonicalizeCode(code) == canonicalizeCode(reference)
        results += PracticeTestCaseResult(
            id = "hidden-structural",
            visibility = PracticeTestVisibility.HIDDEN,
            label = "숨은 구조 검증",
            passed = structural && defaultPassed,
            evidence = PracticeTestEngine.hiddenEvidence(structural && defaultPassed)
        )
    } else {
        variants.forEachIndexed { index, variant ->
            val learnerEdge = engine.execute(rsLanguage(lesson.practiceLanguage), variant.learnerCode)
            val referenceEdge = engine.execute(rsLanguage(lesson.practiceLanguage), variant.referenceCode)
            val passed = learnerEdge.isSuccess &&
                referenceEdge.isSuccess &&
                PracticeTestEngine.normalizeOutput(learnerEdge.output) ==
                PracticeTestEngine.normalizeOutput(referenceEdge.output)
            results += PracticeTestCaseResult(
                id = "hidden-${index + 1}",
                visibility = PracticeTestVisibility.HIDDEN,
                label = "숨은 경계조건 ${index + 1}",
                passed = passed,
                evidence = PracticeTestEngine.hiddenEvidence(passed)
            )
        }
    }

    return PracticeSubmissionResult(results)
}

@Composable
private fun RsRunCard(run: RsRunState) {
    val success = run.result.isSuccess
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (success) "▶ 실행 성공 · 채점 안 됨" else "▶ 실행 실패 · 채점 안 됨",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(run.note, fontSize = 12.sp)
            if (success) {
                Text(
                    run.result.output.ifBlank { "출력 없음" },
                    fontSize = 12.sp,
                    style = CodeTypography
                )
            } else {
                Text(
                    run.result.errorMessage.orEmpty().ifBlank { "실행 오류" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (run.customApplied) {
                Text(
                    "테스트 값은 실행용 복사본에만 적용됐습니다.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RsSubmissionCard(result: PracticeSubmissionResult) {
    val container = if (result.passed) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = .5f)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (result.passed) "✓ 제출 통과" else "! 제출 실패",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                "공개 ${result.publicPassed}/${result.publicTotal} · 숨은 ${result.hiddenPassed}/${result.hiddenTotal}",
                fontSize = 12.sp
            )
            result.results.filter { it.visibility == PracticeTestVisibility.PUBLIC }.forEach { test ->
                Column {
                    Text(
                        "${if (test.passed) "✓" else "!"} ${test.label}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(test.evidence, fontSize = 11.sp, lineHeight = 17.sp)
                }
            }
            if (result.hiddenTotal > 0) {
                HorizontalDivider()
                result.results.filter { it.visibility == PracticeTestVisibility.HIDDEN }
                    .forEachIndexed { index, test ->
                        Text(
                            "${if (test.passed) "✓" else "!"} 숨은 테스트 ${index + 1} · " +
                                (if (test.passed) "통과" else "실패 — 입력값은 비공개"),
                            fontSize = 12.sp
                        )
                    }
            }
            if (!result.passed) {
                val failure = result.failureFeedback()
                HorizontalDivider()
                RsFeedbackLine("무엇이 실패했나", failure.whatFailed)
                RsFeedbackLine("증거", failure.evidence)
                RsFeedbackLine("왜 그런가", failure.why)
                RsFeedbackLine("어디를 생각해볼까", failure.focus)
                RsFeedbackLine("다시 실행", failure.retry)
            }
        }
    }
}

@Composable
private fun RsFeedbackCard(
    feedback: PracticeFeedback,
    onReviewLecture: (Int) -> Unit
) {
    val container = when (feedback.verdict) {
        FeedbackVerdict.CORRECT -> MaterialTheme.colorScheme.secondaryContainer
        FeedbackVerdict.INCORRECT, FeedbackVerdict.ERROR ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = .5f)
        FeedbackVerdict.REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(feedback.headline, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (feedback.verdict == FeedbackVerdict.INCORRECT || feedback.verdict == FeedbackVerdict.ERROR) {
                val evidence = buildString {
                    append("내 답/코드: ")
                    append(feedback.learnerAnswer.take(500).ifBlank { "(입력 없음)" })
                    if (feedback.correctAnswer.isNotBlank()) {
                        append("\n기준: ")
                        append(feedback.correctAnswer.take(500))
                    }
                }
                RsFeedbackLine("무엇이 실패했나", feedback.headline)
                RsFeedbackLine("증거", evidence)
                RsFeedbackLine("왜 그런가", feedback.why)
                RsFeedbackLine("어디를 생각해볼까", feedback.misconception)
                RsFeedbackLine("다시 실행", feedback.retryGuidance)
            } else {
                RsFeedbackLine("무엇이 확인됐나", feedback.why)
                RsFeedbackLine("헷갈리기 쉬운 지점", feedback.misconception)
                RsFeedbackLine("다시 할 때", feedback.retryGuidance)
            }
            OutlinedButton(
                onClick = { onReviewLecture(feedback.reviewSectionIndex) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("관련 강의 다시 보기") }
        }
    }
}

@Composable
private fun RsFeedbackLine(title: String, text: String) {
    Column {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(text.ifBlank { "-" }, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun RsPracticeCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun RsCodeBox(text: String) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, Modifier.padding(12.dp), style = CodeTypography)
    }
}

@Composable
private fun RsNeutralBox(title: String, text: String) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

private fun rsDeterministicEvidence(result: PracticeSubmissionResult): String = buildString {
    append("APP_GRADE=")
    append(if (result.passed) "PASS" else "FAIL")
    append("\npublic=${result.publicPassed}/${result.publicTotal}")
    append("\nhidden=${result.hiddenPassed}/${result.hiddenTotal}")
    if (!result.passed) {
        append("\n")
        append(result.failureFeedback().asText())
    }
}

private fun rsFailureGuide(result: PracticeSubmissionResult): String =
    result.failureFeedback().asText()

private fun rsBlockReason(step: Int): String = when (step) {
    2 -> "예상을 적고 제출해야 합니다."
    3 -> "코드 읽기 답안을 제출해야 합니다."
    4 -> "▶ 실행만으로는 통과하지 않습니다. ✓ 제출의 공개·숨은 테스트를 모두 통과해야 합니다."
    5 -> "빈칸 문제를 맞혀야 합니다."
    6 -> "직접 작성 코드를 ✓ 제출해 공개·숨은 테스트를 통과해야 합니다."
    7 -> "디버깅 수정안을 ✓ 제출해 공개·숨은 테스트를 통과해야 합니다."
    8 -> "AI 판별 문제를 맞혀야 합니다."
    9 -> "응용 코드를 ✓ 제출해 공개·숨은 테스트를 통과해야 합니다."
    else -> ""
}

private fun rsLanguage(value: String): ProgrammingLanguage = when (value) {
    "SQL" -> ProgrammingLanguage.SQL
    "TYPESCRIPT" -> ProgrammingLanguage.TYPESCRIPT
    "HTML_JS" -> ProgrammingLanguage.HTML_JS
    else -> ProgrammingLanguage.PYTHON
}

private fun rsMark(value: Boolean): String = if (value) "✓" else "✗"
