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
import com.futuretech.poweruser.data.LessonContent
import com.futuretech.poweruser.education.EditableTestLiteral
import com.futuretech.poweruser.education.FeedbackVerdict
import com.futuretech.poweruser.education.LearningPracticePolicy
import com.futuretech.poweruser.education.LearningSessionMode
import com.futuretech.poweruser.education.LessonProblemType
import com.futuretech.poweruser.education.MasteryEvidence
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

private data class FocusedRunState(
    val result: ExecutionResult,
    val customApplied: Boolean,
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusedPracticeScreen(
    lessonId: String,
    mode: LearningSessionMode = LearningSessionMode.PRACTICE,
    onNavigateBack: () -> Unit,
    onSwitchMode: () -> Unit,
    onReviewLecture: (Int) -> Unit,
    onMasteryCompleted: (String, String, Int, MasteryEvidence) -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SandboxedExecutionEngine(context) }
    val lesson = remember(lessonId) {
        CurriculumDataRepository.lessonById(lessonId) ?: CurriculumDataRepository.beginnerLessons.first()
    }
    val flow = LearningPracticePolicy.regularLessonFlow
    val fillQuestion = remember(lesson.fillInBlankPrompt) { parseFillBlankPrompt(lesson.fillInBlankPrompt) }

    var step by remember(lessonId, mode) { mutableStateOf(1) }

    var prediction by remember(lessonId, mode) { mutableStateOf("") }
    var predictionSubmitted by remember(lessonId, mode) { mutableStateOf(false) }
    var predictionFeedback by remember(lessonId, mode) { mutableStateOf<PracticeFeedback?>(null) }

    var practiceCode by remember(lessonId, mode) {
        mutableStateOf(if (lesson.practiceLanguage == "HTML_JS") lesson.codeSample else lesson.initialPracticeCode)
    }
    var practiceRun by remember(lessonId, mode) { mutableStateOf<FocusedRunState?>(null) }
    var practiceSubmit by remember(lessonId, mode) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var practiceHintLevel by remember(lessonId, mode) { mutableStateOf(0) }
    var maxHintLevel by remember(lessonId, mode) { mutableStateOf(0) }
    var practiceLiteralIndex by remember(lessonId, mode) { mutableStateOf(0) }
    var practiceCustomEnabled by remember(lessonId, mode) { mutableStateOf(false) }
    var practiceCustomValue by remember(lessonId, mode) { mutableStateOf("") }

    var fillInput by remember(lessonId, mode) { mutableStateOf("") }
    var fillCorrect by remember(lessonId, mode) { mutableStateOf(false) }
    var fillFeedback by remember(lessonId, mode) { mutableStateOf<PracticeFeedback?>(null) }

    var shortCode by remember(lessonId, mode) { mutableStateOf("") }
    var shortRun by remember(lessonId, mode) { mutableStateOf<FocusedRunState?>(null) }
    var shortSubmit by remember(lessonId, mode) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var shortLiteralIndex by remember(lessonId, mode) { mutableStateOf(0) }
    var shortCustomEnabled by remember(lessonId, mode) { mutableStateOf(false) }
    var shortCustomValue by remember(lessonId, mode) { mutableStateOf("") }
    var showShortAnswer by remember(lessonId, mode) { mutableStateOf(false) }

    var debugCode by remember(lessonId, mode) { mutableStateOf(lesson.brokenCode) }
    var debugRun by remember(lessonId, mode) { mutableStateOf<FocusedRunState?>(null) }
    var debugSubmit by remember(lessonId, mode) { mutableStateOf<PracticeSubmissionResult?>(null) }
    var debugLiteralIndex by remember(lessonId, mode) { mutableStateOf(0) }
    var debugCustomEnabled by remember(lessonId, mode) { mutableStateOf(false) }
    var debugCustomValue by remember(lessonId, mode) { mutableStateOf("") }
    var showDebugAnswer by remember(lessonId, mode) { mutableStateOf(false) }

    var selectedOption by remember(lessonId, mode) { mutableStateOf(-1) }
    var aiCorrect by remember(lessonId, mode) { mutableStateOf(false) }
    var aiFeedback by remember(lessonId, mode) { mutableStateOf<PracticeFeedback?>(null) }

    var completionSaved by remember(lessonId, mode) { mutableStateOf(false) }
    var savedEvidence by remember(lessonId, mode) { mutableStateOf<MasteryEvidence?>(null) }

    val problemType = flow[(step - 1).coerceIn(0, flow.lastIndex)]
    val allPassed = predictionSubmitted &&
        practiceSubmit?.passed == true &&
        fillCorrect &&
        shortSubmit?.passed == true &&
        debugSubmit?.passed == true &&
        aiCorrect

    fun canAdvance(): Boolean = when (problemType) {
        LessonProblemType.PREDICT_OUTPUT -> predictionSubmitted
        LessonProblemType.MODIFY_AND_RUN -> practiceSubmit?.passed == true
        LessonProblemType.FILL_BLANK -> fillCorrect
        LessonProblemType.WRITE_FROM_MEMORY -> shortSubmit?.passed == true
        LessonProblemType.DEBUG -> debugSubmit?.passed == true
        LessonProblemType.VERIFY_AI_ANSWER -> aiCorrect
    }

    Scaffold(
        modifier = Modifier.testTag("focused_practice_root"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (mode == LearningSessionMode.PRACTICE) "연습" else "Chapter Challenge",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("session_mode_${mode.name.lowercase()}")
                        )
                        Text(
                            "${lesson.lessonId} · 문제 $step/${flow.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 돌아가기") } },
                actions = {
                    TextButton(onClick = onSwitchMode) {
                        Text(if (mode == LearningSessionMode.PRACTICE) "Challenge" else "연습")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FocusedModeBanner(mode)

            Row(
                Modifier.fillMaxWidth().testTag("problem_progress"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(flow.size) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (index < step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = .25f),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(lesson.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    "일반 Lesson은 6문제로 끝납니다. ▶ 실행은 자유 실험, ✓ 제출만 판정에 반영됩니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            when (problemType) {
                LessonProblemType.PREDICT_OUTPUT -> FocusedCard("1. 실행 결과 예상") {
                    Text("코드를 실행하기 전에 입력→처리→출력을 먼저 예상하세요.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    FocusedCodeBox(lesson.codeSample)
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
                    ) { Text("예상 제출") }
                    predictionFeedback?.let {
                        FocusedFeedbackCard(it, mode == LearningSessionMode.PRACTICE, onReviewLecture)
                    }
                }

                LessonProblemType.MODIFY_AND_RUN -> FocusedCard("2. 코드 수정 → 실행 → 제출") {
                    Text("원본을 목적 있게 바꾸고 여러 입력으로 자유 실행한 뒤 제출하세요.", fontSize = 13.sp)
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
                    FocusedCustomTestControls(
                        code = practiceCode,
                        selectedIndex = practiceLiteralIndex,
                        customEnabled = practiceCustomEnabled,
                        customValue = practiceCustomValue,
                        onSelectedIndex = { index, literal ->
                            practiceLiteralIndex = index
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
                                    practiceRun = focusedRunOnly(
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
                                    val result = focusedSubmitOpenTask(
                                        engine,
                                        lesson,
                                        practiceCode,
                                        if (lesson.practiceLanguage == "HTML_JS") lesson.codeSample else lesson.initialPracticeCode
                                    )
                                    practiceSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_FOCUSED_MODIFY",
                                            practiceCode,
                                            "수정 문제 제출 실패",
                                            focusedFailureGuide(result)
                                        )
                                    }
                                }
                            }
                        ) { Text("✓ 제출") }
                    }
                    if (LearningPracticePolicy.hintsAllowed(mode)) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                if (practiceHintLevel < 3) {
                                    practiceHintLevel++
                                    maxHintLevel = maxOf(maxHintLevel, practiceHintLevel)
                                }
                            },
                            enabled = practiceHintLevel < 3,
                            modifier = Modifier.testTag("practice_hint_button")
                        ) { Text("💡 힌트 ${practiceHintLevel}/3") }
                        if (practiceHintLevel > 0) {
                            val hint = when (practiceHintLevel) {
                                1 -> lesson.hintLevel1
                                2 -> lesson.hintLevel2
                                else -> lesson.hintLevel3
                            }
                            FocusedNeutralBox(
                                "힌트 ${practiceHintLevel}",
                                "$hint\n\n힌트 사용은 완료율을 깎지 않습니다. 성공했을 때 숙련 증거의 강도만 조정됩니다."
                            )
                        }
                    } else {
                        FocusedNeutralBox("평가 모드", "Chapter Challenge에서는 힌트가 잠깁니다.")
                    }
                    practiceRun?.let { FocusedRunCard(it) }
                    practiceSubmit?.let { FocusedSubmissionCard(it) }
                    if (lesson.practiceLanguage == "HTML_JS") {
                        Spacer(Modifier.height(8.dp))
                        HtmlPreviewSandboxView(htmlContent = practiceCode)
                    }
                    if (LearningPracticePolicy.aiAssistanceAllowed(mode)) {
                        Spacer(Modifier.height(8.dp))
                        AiLearningPanel(
                            lesson = lesson,
                            currentStep = 4,
                            practiceAttempted = practiceSubmit != null,
                            codeSnapshot = practiceCode
                        )
                    }
                }

                LessonProblemType.FILL_BLANK -> FocusedCard("3. 핵심 개념 빈칸") {
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
                            val feedback = PracticeFeedbackEngine.fillBlank(
                                lesson,
                                fillInput,
                                fillQuestion.acceptedAnswers
                            )
                            fillFeedback = feedback
                            fillCorrect = feedback.verdict == FeedbackVerdict.CORRECT
                            if (!fillCorrect) {
                                onRecordErrorNote(
                                    "${lesson.lessonId}_FOCUSED_FILL",
                                    fillInput,
                                    "빈칸 문제 오답",
                                    "${feedback.why}\n${feedback.retryGuidance}"
                                )
                            }
                        },
                        enabled = fillInput.isNotBlank()
                    ) { Text("✓ 제출") }
                    fillFeedback?.let {
                        FocusedFeedbackCard(it, mode == LearningSessionMode.PRACTICE, onReviewLecture)
                    }
                }

                LessonProblemType.WRITE_FROM_MEMORY -> FocusedCard("4. 보지 않고 직접 작성") {
                    Text("기억에서 코드를 작성하고 실제 runtime 테스트로 검증합니다.", fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shortCode,
                        onValueChange = {
                            shortCode = it
                            shortRun = null
                            shortSubmit = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("기억에서 작성") },
                        textStyle = CodeTypography,
                        minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    FocusedCustomTestControls(
                        code = shortCode,
                        selectedIndex = shortLiteralIndex,
                        customEnabled = shortCustomEnabled,
                        customValue = shortCustomValue,
                        onSelectedIndex = { index, literal ->
                            shortLiteralIndex = index
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
                                    shortRun = focusedRunOnly(
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
                                    val result = focusedSubmitReferenceTask(
                                        engine,
                                        lesson,
                                        shortCode,
                                        lesson.initialPracticeCode
                                    )
                                    shortSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_FOCUSED_WRITE",
                                            shortCode,
                                            "직접 작성 제출 실패",
                                            focusedFailureGuide(result)
                                        )
                                    }
                                }
                            },
                            enabled = shortCode.isNotBlank()
                        ) { Text("✓ 제출") }
                    }
                    shortRun?.let { FocusedRunCard(it) }
                    shortSubmit?.let { FocusedSubmissionCard(it) }
                    if (shortSubmit?.passed == false && LearningPracticePolicy.solutionRevealAllowed(mode)) {
                        OutlinedButton(onClick = { showShortAnswer = !showShortAnswer }) {
                            Text(if (showShortAnswer) "예시 숨기기" else "예시 보기")
                        }
                    }
                    if (showShortAnswer && LearningPracticePolicy.solutionRevealAllowed(mode)) {
                        FocusedCodeBox(lesson.initialPracticeCode)
                        Text("예시를 본 뒤에도 직접 수정하고 다시 제출해야 통과합니다.", fontSize = 11.sp)
                    }
                }

                LessonProblemType.DEBUG -> FocusedCard("5. 고장난 코드 디버깅") {
                    Text("증상→원인→최소 수정 순서로 고친 뒤 실행하고 제출하세요.", fontSize = 13.sp)
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
                        minLines = 5
                    )
                    Spacer(Modifier.height(8.dp))
                    FocusedCustomTestControls(
                        code = debugCode,
                        selectedIndex = debugLiteralIndex,
                        customEnabled = debugCustomEnabled,
                        customValue = debugCustomValue,
                        onSelectedIndex = { index, literal ->
                            debugLiteralIndex = index
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
                                    debugRun = focusedRunOnly(
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
                                    val result = focusedSubmitReferenceTask(
                                        engine,
                                        lesson,
                                        debugCode,
                                        lesson.brokenCodeFix
                                    )
                                    debugSubmit = result
                                    if (!result.passed) {
                                        onRecordErrorNote(
                                            "${lesson.lessonId}_FOCUSED_DEBUG",
                                            debugCode,
                                            "디버깅 제출 실패",
                                            focusedFailureGuide(result)
                                        )
                                    }
                                }
                            }
                        ) { Text("✓ 제출") }
                    }
                    debugRun?.let { FocusedRunCard(it) }
                    debugSubmit?.let { FocusedSubmissionCard(it) }
                    if (debugSubmit?.passed == false && LearningPracticePolicy.solutionRevealAllowed(mode)) {
                        OutlinedButton(onClick = { showDebugAnswer = !showDebugAnswer }) {
                            Text(if (showDebugAnswer) "정답 가이드 숨기기" else "정답 가이드 보기")
                        }
                    }
                    if (showDebugAnswer && LearningPracticePolicy.solutionRevealAllowed(mode)) {
                        FocusedCodeBox(lesson.brokenCodeFix)
                        Text("가이드를 본 뒤에도 직접 고쳐 다시 제출해야 통과합니다.", fontSize = 11.sp)
                    }
                    if (LearningPracticePolicy.aiAssistanceAllowed(mode)) {
                        Spacer(Modifier.height(8.dp))
                        AiLearningPanel(
                            lesson = lesson,
                            currentStep = 7,
                            practiceAttempted = debugSubmit != null,
                            codeSnapshot = debugCode
                        )
                    }
                }

                LessonProblemType.VERIFY_AI_ANSWER -> FocusedCard("6. AI가 만든 답 검증") {
                    Text(
                        "이 문제는 AI 도움을 받는 기능이 아니라 AI의 답을 검증하는 평가 문제입니다.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
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
                            Text(option, modifier = Modifier.weight(1f), fontSize = 13.sp)
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
                                    "${lesson.lessonId}_FOCUSED_AI_VERIFY",
                                    selectedOption.toString(),
                                    "AI 답 검증 오답",
                                    "${feedback.why}\n${feedback.retryGuidance}"
                                )
                            }
                        },
                        enabled = selectedOption >= 0
                    ) { Text("✓ 제출") }
                    aiFeedback?.let {
                        FocusedFeedbackCard(it, mode == LearningSessionMode.PRACTICE, onReviewLecture)
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("완료 조건", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• 결과 예상 ${focusedMark(predictionSubmitted)}", fontSize = 12.sp)
                    Text("• 수정·테스트 ${focusedMark(practiceSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• 핵심 빈칸 ${focusedMark(fillCorrect)}", fontSize = 12.sp)
                    Text("• 직접 작성 ${focusedMark(shortSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• 디버깅 ${focusedMark(debugSubmit?.passed == true)}", fontSize = 12.sp)
                    Text("• AI 답 검증 ${focusedMark(aiCorrect)}", fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val evidence = LearningPracticePolicy.evidenceForSuccessfulSession(mode, maxHintLevel)
                            onMasteryCompleted(
                                lesson.lessonId,
                                lesson.curriculumType,
                                lesson.unitNumber,
                                evidence
                            )
                            savedEvidence = evidence
                            completionSaved = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("focused_completion_button"),
                        enabled = allPassed && !completionSaved
                    ) {
                        Text(if (completionSaved) "완료 저장됨" else "6문제 완료 저장")
                    }
                    savedEvidence?.let { evidence ->
                        FocusedNeutralBox(
                            "숙련 증거 · ${evidence.strength.label}",
                            if (evidence.strength.needsReview) {
                                "완료율은 ${evidence.completionPercentage}%로 유지됩니다. 힌트 사용 때문에 벌점은 없지만, 독립 수행 증거가 약하므로 복습 대상으로 남깁니다."
                            } else {
                                "완료율 ${evidence.completionPercentage}% · 힌트 사용은 점수를 깎지 않고 숙련 증거의 강도에만 반영됩니다."
                            }
                        )
                    }
                }
            }

            if (step < flow.size) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (step > 1) step-- },
                        enabled = step > 1
                    ) { Text("← 이전") }
                    Button(
                        onClick = { if (canAdvance()) step++ },
                        enabled = canAdvance()
                    ) { Text("다음 →") }
                }
                if (!canAdvance()) {
                    Text(
                        focusedBlockReason(problemType),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("← 이전 문제") }
            }
        }
    }
}

@Composable
private fun FocusedModeBanner(mode: LearningSessionMode) {
    val text = if (mode == LearningSessionMode.PRACTICE) {
        "연습 모드 · 힌트/AI/무제한 재시도 허용 · 실행 실패와 힌트 사용에 벌점 없음"
    } else {
        "Chapter Challenge · 힌트/AI/정답 보기 잠금 · 제출 결과로만 독립 수행을 확인"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, Modifier.padding(12.dp), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun FocusedCustomTestControls(
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
        FocusedNeutralBox(
            "테스트 값",
            "자동 변경 가능한 값이 없습니다. 코드 안의 값을 직접 바꿔 ▶ 실행해도 제출 판정에는 영향이 없습니다."
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = customEnabled, onCheckedChange = onEnabledChange)
            Text("▶ 실행할 때만 테스트 값 바꾸기", fontSize = 13.sp)
        }
        if (customEnabled) {
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
                label = { Text("실행용 임시 값 · 원본 ${literal.raw}") },
                supportingText = { Text("빈 문자열·음수·큰 값 등도 직접 시험할 수 있습니다.") },
                singleLine = true
            )
        }
    }
}

private suspend fun focusedRunOnly(
    engine: SandboxedExecutionEngine,
    lesson: LessonContent,
    code: String,
    selectedIndex: Int,
    customEnabled: Boolean,
    customValue: String
): FocusedRunState {
    var runCode = code
    var customApplied = false
    var note = "현재 편집 코드로 실행했습니다. 실행 결과는 숙련도나 오답 기록을 바꾸지 않습니다."

    if (customEnabled) {
        val literals = PracticeTestEngine.extractEditableLiterals(code)
        val literal = literals.getOrNull(selectedIndex)
        if (literal == null) {
            return FocusedRunState(
                ExecutionResult(false, "", "선택한 테스트 값을 찾을 수 없습니다."),
                false,
                "코드가 바뀌어 테스트 값 위치가 달라졌습니다. 다시 선택하세요."
            )
        }
        val overridden = PracticeTestEngine.applyUserOverride(code, literal, customValue)
        if (overridden == null) {
            return FocusedRunState(
                ExecutionResult(false, "", "테스트 값 형식이 원본 타입과 맞지 않습니다."),
                false,
                "숫자는 숫자로, Boolean은 true/false로 입력하세요."
            )
        }
        runCode = overridden
        customApplied = true
        note = "임시 테스트 값으로 실행했습니다. 편집 코드와 제출 상태는 변경되지 않았습니다."
    }

    return FocusedRunState(
        result = engine.execute(focusedLanguage(lesson.practiceLanguage), runCode),
        customApplied = customApplied,
        note = note
    )
}

private suspend fun focusedSubmitOpenTask(
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

    val defaultRun = engine.execute(focusedLanguage(lesson.practiceLanguage), code)
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
            val edgeRun = engine.execute(focusedLanguage(lesson.practiceLanguage), variant.code)
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

private suspend fun focusedSubmitReferenceTask(
    engine: SandboxedExecutionEngine,
    lesson: LessonContent,
    code: String,
    reference: String
): PracticeSubmissionResult {
    val results = mutableListOf<PracticeTestCaseResult>()
    val learnerDefault = engine.execute(focusedLanguage(lesson.practiceLanguage), code)
    val referenceDefault = engine.execute(focusedLanguage(lesson.practiceLanguage), reference)
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
            !learnerDefault.isSuccess -> learnerDefault.errorMessage.orEmpty().ifBlank { "제출 코드 실행 실패" }
            !referenceDefault.isSuccess -> "기준 예제를 실행할 수 없어 채점할 수 없습니다."
            defaultPassed -> "기본 입력에서 기준 예제와 같은 결과를 냈습니다."
            else -> "실행은 됐지만 기준 예제와 출력이 다릅니다. 내 출력: ${learnerDefault.output.take(160)}"
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
            val learnerEdge = engine.execute(focusedLanguage(lesson.practiceLanguage), variant.learnerCode)
            val referenceEdge = engine.execute(focusedLanguage(lesson.practiceLanguage), variant.referenceCode)
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
private fun FocusedRunCard(run: FocusedRunState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (run.result.isSuccess) "▶ 실행 성공 · 채점 안 됨" else "▶ 실행 실패 · 채점 안 됨",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(run.note, fontSize = 12.sp)
            Text(
                if (run.result.isSuccess) run.result.output.ifBlank { "출력 없음" }
                else run.result.errorMessage.orEmpty().ifBlank { "실행 오류" },
                fontSize = 12.sp,
                style = CodeTypography
            )
            if (run.customApplied) {
                Text("임시 테스트 값은 실행 복사본에만 적용됐습니다.", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FocusedSubmissionCard(result: PracticeSubmissionResult) {
    val color = if (result.passed) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = .55f)
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(if (result.passed) "✓ 제출 통과" else "! 제출 실패", fontWeight = FontWeight.Bold)
            Text(
                "공개 ${result.publicPassed}/${result.publicTotal} · 숨은 ${result.hiddenPassed}/${result.hiddenTotal}",
                fontSize = 12.sp
            )
            result.results.filter { it.visibility == PracticeTestVisibility.PUBLIC }.forEach { test ->
                Text(
                    "${if (test.passed) "✓" else "!"} ${test.label} · ${test.evidence}",
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            result.results.filter { it.visibility == PracticeTestVisibility.HIDDEN }
                .forEachIndexed { index, test ->
                    Text(
                        "${if (test.passed) "✓" else "!"} 숨은 테스트 ${index + 1} · ${if (test.passed) "통과" else "실패"}",
                        fontSize = 12.sp
                    )
                }
            if (!result.passed) {
                Text("수정 후 ▶ 실행으로 확인하고 다시 ✓ 제출하세요.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FocusedFeedbackCard(
    feedback: PracticeFeedback,
    allowLectureReview: Boolean,
    onReviewLecture: (Int) -> Unit
) {
    val color = when (feedback.verdict) {
        FeedbackVerdict.CORRECT -> MaterialTheme.colorScheme.secondaryContainer
        FeedbackVerdict.INCORRECT, FeedbackVerdict.ERROR -> MaterialTheme.colorScheme.errorContainer
        FeedbackVerdict.REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .65f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(feedback.headline, fontWeight = FontWeight.Bold)
            FocusedFeedbackLine("무엇이 확인됐나", feedback.why)
            FocusedFeedbackLine("헷갈리기 쉬운 지점", feedback.misconception)
            FocusedFeedbackLine("다시 할 때", feedback.retryGuidance)
            if (allowLectureReview) {
                OutlinedButton(
                    onClick = { onReviewLecture(feedback.reviewSectionIndex) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("관련 강의 다시 보기") }
            }
        }
    }
}

@Composable
private fun FocusedFeedbackLine(title: String, text: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Text(text.ifBlank { "-" }, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun FocusedCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun FocusedCodeBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(text, Modifier.padding(12.dp), style = CodeTypography)
    }
}

@Composable
private fun FocusedNeutralBox(title: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

private fun focusedFailureGuide(result: PracticeSubmissionResult): String {
    val publicFailure = result.results.firstOrNull {
        it.visibility == PracticeTestVisibility.PUBLIC && !it.passed
    }
    return if (publicFailure != null) {
        "무엇이 실패했나: ${publicFailure.label}\n" +
            "증거: ${publicFailure.evidence}\n" +
            "왜 그런가: 제출 코드가 공개 요구사항을 아직 만족하지 못했습니다.\n" +
            "어디를 생각해볼까: 입력→처리→출력 흐름과 기준 예제를 비교하세요.\n" +
            "다시 실행: 수정 후 ▶ 실행으로 확인하고 ✓ 제출하세요."
    } else {
        "무엇이 실패했나: 숨은 경계조건\n" +
            "증거: 숨은 테스트 ${result.hiddenPassed}/${result.hiddenTotal} 통과\n" +
            "왜 그런가: 기본 예제는 되지만 변형 입력에서 동작이 달라졌습니다.\n" +
            "어디를 생각해볼까: 특정 예제 값에만 맞춘 코드는 아닌지 확인하세요.\n" +
            "다시 실행: 테스트 값을 직접 바꿔 ▶ 실행한 뒤 다시 ✓ 제출하세요."
    }
}

private fun focusedBlockReason(type: LessonProblemType): String = when (type) {
    LessonProblemType.PREDICT_OUTPUT -> "예상을 적고 제출해야 다음 문제로 이동합니다."
    LessonProblemType.MODIFY_AND_RUN -> "수정 코드를 ✓ 제출해 공개·숨은 테스트를 통과해야 합니다."
    LessonProblemType.FILL_BLANK -> "핵심 개념 빈칸을 맞혀야 합니다."
    LessonProblemType.WRITE_FROM_MEMORY -> "직접 작성 코드를 ✓ 제출해 테스트를 통과해야 합니다."
    LessonProblemType.DEBUG -> "디버깅 수정안을 ✓ 제출해 테스트를 통과해야 합니다."
    LessonProblemType.VERIFY_AI_ANSWER -> "AI 답 검증 문제를 맞혀야 합니다."
}

private fun focusedLanguage(value: String): ProgrammingLanguage = when (value) {
    "SQL" -> ProgrammingLanguage.SQL
    "TYPESCRIPT" -> ProgrammingLanguage.TYPESCRIPT
    "HTML_JS" -> ProgrammingLanguage.HTML_JS
    else -> ProgrammingLanguage.PYTHON
}

private fun focusedMark(value: Boolean): String = if (value) "✓" else "✗"
