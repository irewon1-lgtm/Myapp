package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.data.CurriculumDataRepository
import com.futuretech.poweruser.data.PersonalErrorNoteEntity
import com.futuretech.poweruser.education.ErrorDrivenPracticeEngine
import com.futuretech.poweruser.education.LearningPracticePolicy
import com.futuretech.poweruser.education.LearningSessionMode
import com.futuretech.poweruser.education.LessonProblemType
import com.futuretech.poweruser.education.MasteryEvidence
import com.futuretech.poweruser.education.PracticeTestEngine
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

private data class ChallengeGrade(val passed: Boolean, val evidence: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterChallengeScreen(
    lessonId: String,
    errorNotes: List<PersonalErrorNoteEntity>,
    onNavigateBack: () -> Unit,
    onSwitchToPractice: () -> Unit,
    onMasteryCompleted: (String, String, Int, MasteryEvidence) -> Unit,
    onRecordErrorNote: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SandboxedExecutionEngine(context) }
    val lesson = remember(lessonId) {
        CurriculumDataRepository.lessonById(lessonId) ?: CurriculumDataRepository.beginnerLessons.first()
    }
    val flow = remember(errorNotes) { ErrorDrivenPracticeEngine.challengeFlow(errorNotes) }

    var stepIndex by remember(lessonId) { mutableStateOf(0) }
    var textAnswer by remember(lessonId) { mutableStateOf("") }
    var codeAnswer by remember(lessonId) { mutableStateOf("") }
    var selectedOption by remember(lessonId) { mutableStateOf(-1) }
    var runEvidence by remember(lessonId) { mutableStateOf("") }
    var submitEvidence by remember(lessonId) { mutableStateOf("") }
    var passedCurrent by remember(lessonId) { mutableStateOf(false) }
    var saved by remember(lessonId) { mutableStateOf(false) }

    val problemType = flow[stepIndex.coerceIn(0, flow.lastIndex)]
    val ordinal = flow.take(stepIndex).count { it == problemType }
    val language = remember(lesson.practiceLanguage) { challengeLanguage(lesson.practiceLanguage) }
    val predictionCode = remember(stepIndex, lessonId) { challengeVariantCode(lesson.codeSample, ordinal) }
    val modifyBaseline = remember(stepIndex, lessonId) { challengeVariantCode(lesson.initialPracticeCode, ordinal) }
    val writeReference = remember(stepIndex, lessonId) { challengeVariantCode(lesson.initialPracticeCode, ordinal) }
    val debugPair = remember(stepIndex, lessonId) {
        val variants = PracticeTestEngine.pairedEdgeVariants(lesson.brokenCode, lesson.brokenCodeFix)
        variants.getOrNull((ordinal - 1).coerceAtLeast(0))
    }
    val debugStart = if (ordinal == 0 || debugPair == null) lesson.brokenCode else debugPair.learnerCode
    val debugReference = if (ordinal == 0 || debugPair == null) lesson.brokenCodeFix else debugPair.referenceCode

    LaunchedEffect(stepIndex, problemType) {
        textAnswer = ""
        selectedOption = -1
        runEvidence = ""
        submitEvidence = ""
        passedCurrent = false
        codeAnswer = when (problemType) {
            LessonProblemType.MODIFY_AND_RUN -> modifyBaseline
            LessonProblemType.WRITE_FROM_MEMORY -> ""
            LessonProblemType.DEBUG -> debugStart
            else -> ""
        }
    }

    fun recordFailure(detail: String) {
        onRecordErrorNote(
            "${lesson.lessonId}_CHALLENGE_${problemType.name}",
            if (codeAnswer.isNotBlank()) codeAnswer else textAnswer,
            "Chapter Challenge 제출 실패",
            detail
        )
    }

    Scaffold(
        modifier = Modifier.testTag("chapter_challenge_root"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chapter Challenge", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${lesson.lessonId} · ${stepIndex + 1}/${flow.size}", fontSize = 11.sp)
                    }
                },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 돌아가기") } },
                actions = { TextButton(onClick = onSwitchToPractice) { Text("연습") } }
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
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("힌트·AI·정답 보기 없음 · 제출 결과만 숙련도에 반영", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(ErrorDrivenPracticeEngine.priorityLabel(errorNotes), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(flow.size) { index ->
                    Box(
                        Modifier.weight(1f).height(6.dp).background(
                            if (index <= stepIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .25f),
                            RoundedCornerShape(3.dp)
                        )
                    )
                }
            }

            Text(lesson.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(challengeTypeTitle(problemType, ordinal), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

            when (problemType) {
                LessonProblemType.PREDICT_OUTPUT -> {
                    ChallengeCodeBox(predictionCode)
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = { textAnswer = it; passedCurrent = false; submitEvidence = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("실행 결과를 정확히 예상") },
                        minLines = 2
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val expected = engine.execute(language, predictionCode)
                                val actual = PracticeTestEngine.normalizeOutput(textAnswer)
                                val target = PracticeTestEngine.normalizeOutput(expected.output)
                                val passed = expected.isSuccess && actual == target
                                passedCurrent = passed
                                submitEvidence = if (passed) "✓ 실제 runtime 출력과 일치합니다." else "! 실제 runtime 결과와 다릅니다. 입력→처리→출력을 다시 추적하세요."
                                if (!passed) recordFailure(submitEvidence)
                            }
                        },
                        enabled = textAnswer.isNotBlank()
                    ) { Text("✓ 제출") }
                }

                LessonProblemType.FILL_BLANK -> {
                    Text(challengeFillPrompt(lesson.fillInBlankPrompt), fontSize = 14.sp, lineHeight = 22.sp)
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = { textAnswer = it; passedCurrent = false; submitEvidence = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("정답") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val accepted = challengeAcceptedAnswers(lesson.fillInBlankPrompt)
                            val normalized = textAnswer.trim().lowercase()
                            val passed = accepted.any { it.lowercase() == normalized }
                            passedCurrent = passed
                            submitEvidence = if (passed) "✓ 핵심 개념을 정확히 회상했습니다." else "! 핵심 용어가 맞지 않습니다. 정의보다 역할을 먼저 떠올리세요."
                            if (!passed) recordFailure(submitEvidence)
                        },
                        enabled = textAnswer.isNotBlank()
                    ) { Text("✓ 제출") }
                }

                LessonProblemType.VERIFY_AI_ANSWER -> {
                    Text(lesson.aiHallucinationQuestion, fontSize = 14.sp, lineHeight = 22.sp)
                    lesson.aiHallucinationOptions.forEachIndexed { index, option ->
                        Row(Modifier.fillMaxWidth()) {
                            RadioButton(selected = selectedOption == index, onClick = { selectedOption = index; passedCurrent = false })
                            Text(option, modifier = Modifier.weight(1f).padding(top = 12.dp), fontSize = 13.sp)
                        }
                    }
                    Button(
                        onClick = {
                            val passed = selectedOption == lesson.correctOptionIndex
                            passedCurrent = passed
                            submitEvidence = if (passed) "✓ AI 답을 근거 기준으로 검증했습니다." else "! 표현의 자신감이 아니라 근거와 역할을 다시 확인하세요."
                            if (!passed) recordFailure(submitEvidence)
                        },
                        enabled = selectedOption >= 0
                    ) { Text("✓ 제출") }
                }

                LessonProblemType.MODIFY_AND_RUN,
                LessonProblemType.WRITE_FROM_MEMORY,
                LessonProblemType.DEBUG -> {
                    val prompt = when (problemType) {
                        LessonProblemType.MODIFY_AND_RUN -> "원본을 목적 있게 수정하고 실행 가능한 상태로 만드세요."
                        LessonProblemType.WRITE_FROM_MEMORY -> "예시를 보지 않고 같은 기능을 직접 작성하세요."
                        else -> "고장난 코드를 최소 수정으로 정상 동작하게 만드세요."
                    }
                    Text(prompt, fontSize = 13.sp)
                    if (problemType == LessonProblemType.MODIFY_AND_RUN) {
                        Text("현재 코드는 시작점입니다. 최소 한 곳을 목적 있게 바꿔야 합니다.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    OutlinedTextField(
                        value = codeAnswer,
                        onValueChange = { codeAnswer = it; passedCurrent = false; runEvidence = ""; submitEvidence = "" },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 7,
                        label = { Text("코드 · ${lesson.practiceLanguage}") },
                        textStyle = CodeTypography
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val run = engine.execute(language, codeAnswer)
                                    runEvidence = if (run.isSuccess) "▶ 실행 성공\n${run.output.take(500)}" else "▶ 실행 실패\n${run.errorMessage.orEmpty()}"
                                }
                            },
                            enabled = codeAnswer.isNotBlank()
                        ) { Text("▶ 실행") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val grade = when (problemType) {
                                        LessonProblemType.MODIFY_AND_RUN -> gradeModified(engine, language, codeAnswer, modifyBaseline)
                                        LessonProblemType.WRITE_FROM_MEMORY -> gradeAgainstReference(engine, language, codeAnswer, writeReference)
                                        LessonProblemType.DEBUG -> gradeAgainstReference(engine, language, codeAnswer, debugReference)
                                        else -> ChallengeGrade(false, "지원하지 않는 문제 유형")
                                    }
                                    passedCurrent = grade.passed
                                    submitEvidence = grade.evidence
                                    if (!grade.passed) recordFailure(grade.evidence)
                                }
                            },
                            enabled = codeAnswer.isNotBlank()
                        ) { Text("✓ 제출") }
                    }
                    if (runEvidence.isNotBlank()) ChallengeStatusBox(runEvidence, false)
                }
            }

            if (submitEvidence.isNotBlank()) ChallengeStatusBox(submitEvidence, passedCurrent)

            if (stepIndex < flow.lastIndex) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { if (stepIndex > 0) stepIndex-- }, enabled = stepIndex > 0) { Text("← 이전") }
                    Button(onClick = { if (passedCurrent) stepIndex++ }, enabled = passedCurrent) { Text("다음 →") }
                }
            } else {
                Button(
                    onClick = {
                        val evidence = LearningPracticePolicy.evidenceForSuccessfulSession(LearningSessionMode.CHALLENGE, 0)
                        onMasteryCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber, evidence)
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("challenge_completion_button"),
                    enabled = passedCurrent && !saved
                ) { Text(if (saved) "Challenge 저장 완료" else "10문제 Challenge 완료") }
            }
        }
    }
}

private fun challengeTypeTitle(type: LessonProblemType, ordinal: Int): String {
    val base = when (type) {
        LessonProblemType.PREDICT_OUTPUT -> "실행 결과 예상"
        LessonProblemType.MODIFY_AND_RUN -> "코드 수정·실행"
        LessonProblemType.FILL_BLANK -> "핵심 개념 회상"
        LessonProblemType.WRITE_FROM_MEMORY -> "직접 작성"
        LessonProblemType.DEBUG -> "디버깅"
        LessonProblemType.VERIFY_AI_ANSWER -> "AI 답 검증"
    }
    return if (ordinal == 0) base else "$base · 변형 ${ordinal + 1}"
}

private fun challengeVariantCode(code: String, ordinal: Int): String {
    if (ordinal <= 0) return code
    val variants = PracticeTestEngine.edgeVariants(code, maxVariants = 3)
    return variants.getOrNull((ordinal - 1) % maxOf(variants.size, 1))?.code ?: code
}

private fun challengeAcceptedAnswers(raw: String): List<String> {
    val inside = Regex("\\[([^]]+)]").find(raw)?.groupValues?.getOrNull(1).orEmpty()
    return inside.split("/", ",").map { it.trim() }.filter { it.isNotBlank() }
}

private fun challengeFillPrompt(raw: String): String = raw.replace(Regex("\\[([^]]+)]"), "[ ? ]")
private fun challengeLanguage(raw: String): ProgrammingLanguage = runCatching { ProgrammingLanguage.valueOf(raw) }.getOrDefault(ProgrammingLanguage.PYTHON)

private suspend fun gradeModified(engine: SandboxedExecutionEngine, language: ProgrammingLanguage, learner: String, baseline: String): ChallengeGrade {
    if (learner.trim() == baseline.trim()) return ChallengeGrade(false, "! 원본과 같습니다. 값·조건·문장 중 한 곳을 목적 있게 바꾸세요.")
    val run = engine.execute(language, learner)
    if (!run.isSuccess) return ChallengeGrade(false, "! 기본 실행 실패: ${run.errorMessage.orEmpty()}")
    val hidden = PracticeTestEngine.edgeVariants(learner, 3)
    val failed = hidden.count { !engine.execute(language, it.code).isSuccess }
    return if (failed == 0) ChallengeGrade(true, "✓ 기본 실행 + 숨은 경계 실행 ${hidden.size}/${hidden.size} 통과")
    else ChallengeGrade(false, "! 기본 실행은 성공했지만 숨은 경계 실행 ${hidden.size - failed}/${hidden.size} 통과")
}

private suspend fun gradeAgainstReference(engine: SandboxedExecutionEngine, language: ProgrammingLanguage, learner: String, reference: String): ChallengeGrade {
    val learnerRun = engine.execute(language, learner)
    val referenceRun = engine.execute(language, reference)
    if (!learnerRun.isSuccess) return ChallengeGrade(false, "! 제출 코드 실행 실패: ${learnerRun.errorMessage.orEmpty()}")
    if (!referenceRun.isSuccess) return ChallengeGrade(false, "! 기준 코드 실행 실패로 채점할 수 없습니다.")
    val defaultPass = PracticeTestEngine.normalizeOutput(learnerRun.output) == PracticeTestEngine.normalizeOutput(referenceRun.output)
    if (!defaultPass) return ChallengeGrade(false, "! 공개 테스트 실패: 기준 예제와 출력이 다릅니다.")
    val pairs = PracticeTestEngine.pairedEdgeVariants(learner, reference, 3)
    var passed = 0
    for (pair in pairs) {
        val learnerEdge = engine.execute(language, pair.learnerCode)
        val referenceEdge = engine.execute(language, pair.referenceCode)
        if (learnerEdge.isSuccess && referenceEdge.isSuccess && PracticeTestEngine.normalizeOutput(learnerEdge.output) == PracticeTestEngine.normalizeOutput(referenceEdge.output)) passed++
    }
    return if (passed == pairs.size) ChallengeGrade(true, "✓ 공개 1/1 + 숨은 경계 ${passed}/${pairs.size} 통과")
    else ChallengeGrade(false, "! 공개 예제는 통과했지만 숨은 경계 ${passed}/${pairs.size} 통과")
}

@Composable
private fun ChallengeCodeBox(code: String) {
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(12.dp)) {
        Text(code, style = CodeTypography, fontSize = 12.sp)
    }
}

@Composable
private fun ChallengeStatusBox(text: String, passed: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (passed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) { Text(text, Modifier.padding(12.dp), fontSize = 12.sp, lineHeight = 18.sp) }
}
