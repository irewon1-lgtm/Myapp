package com.futuretech.poweruser.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.ai.*
import com.futuretech.poweruser.data.LessonContent
import com.futuretech.poweruser.data.SecureKeyStorage
import kotlinx.coroutines.launch

@Composable
fun AiLearningPanel(
    lesson: LessonContent,
    currentStep: Int,
    practiceAttempted: Boolean,
    codeSnapshot: String,
    deterministicEvidence: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyStorage = remember { SecureKeyStorage(context) }
    val coordinator = remember { AiLearningCoordinator(OpenAiTutorGateway()) }

    var modeName by rememberSaveable(lesson.lessonId) { mutableStateOf(AiLearningMode.SOLO.name) }
    val mode = AiLearningMode.valueOf(modeName)
    val availability = AiTutorPolicy.availability(lesson.curriculumType, lesson.stepNumber, practiceAttempted)

    var hasKey by remember(lesson.lessonId) { mutableStateOf(!keyStorage.getApiKey().isNullOrBlank()) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var question by rememberSaveable(lesson.lessonId, currentStep) { mutableStateOf("") }
    var hintLevel by rememberSaveable(lesson.lessonId, currentStep) { mutableStateOf(0) }
    var requestFullSolution by rememberSaveable(lesson.lessonId, currentStep) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var response by remember(lesson.lessonId, currentStep) { mutableStateOf<AiTutorResponse?>(null) }

    LaunchedEffect(availability.collaborateEnabled) {
        if (!availability.collaborateEnabled && mode == AiLearningMode.COLLABORATE) {
            modeName = AiLearningMode.SOLO.name
        }
    }
    LaunchedEffect(availability.fullSolutionEnabled) {
        if (!availability.fullSolutionEnabled) requestFullSolution = false
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI 학습 모드", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                "혼자 풀기와 3단계 힌트는 무료 로컬 처리입니다. 외부 AI 호출은 ‘AI와 같이’에서 사용자가 직접 실행할 때만 발생합니다.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                FilterChip(
                    selected = mode == AiLearningMode.SOLO,
                    onClick = { modeName = AiLearningMode.SOLO.name; response = null },
                    label = { Text("혼자 풀기") },
                    modifier = Modifier.weight(1f).testTag("ai_mode_solo")
                )
                FilterChip(
                    selected = mode == AiLearningMode.HINT,
                    onClick = { modeName = AiLearningMode.HINT.name; response = null },
                    label = { Text("힌트") },
                    modifier = Modifier.weight(1f).testTag("ai_mode_hint")
                )
                FilterChip(
                    selected = mode == AiLearningMode.COLLABORATE,
                    onClick = { modeName = AiLearningMode.COLLABORATE.name; response = null },
                    enabled = availability.collaborateEnabled,
                    label = { Text("AI와 같이") },
                    modifier = Modifier.weight(1f).testTag("ai_mode_collab")
                )
            }

            Text(availability.explanation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (mode) {
                AiLearningMode.SOLO -> {
                    AssistChip(onClick = {}, enabled = false, label = { Text("AI 호출 0회 · 스스로 해결") })
                    Text("이 모드에서는 네트워크 AI 요청 버튼 자체가 활성화되지 않습니다.", fontSize = 12.sp)
                }

                AiLearningMode.HINT -> {
                    Text(
                        "무료 로컬 힌트 · 외부 AI 호출 0회 · API Key 불필요",
                        modifier = Modifier.fillMaxWidth().testTag("ai_key_status"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(
                        onClick = {
                            if (hintLevel >= 3 || busy) return@Button
                            val nextLevel = hintLevel + 1
                            hintLevel = nextLevel
                            busy = true
                            response = null
                            scope.launch {
                                val request = AiTutorRequest(
                                    lessonId = lesson.lessonId,
                                    curriculumType = lesson.curriculumType,
                                    level = lesson.stepNumber,
                                    moduleTitle = lesson.moduleTitle,
                                    lessonTitle = lesson.title,
                                    expectedOutcome = lesson.expectedOutcome,
                                    practiceLanguage = lesson.practiceLanguage,
                                    mode = AiLearningMode.HINT,
                                    task = taskForStep(currentStep),
                                    userMessage = attachDeterministicEvidence(
                                        "현재 내 시도에서 다음으로 확인할 것 한 가지만 힌트로 알려줘.",
                                        deterministicEvidence
                                    ),
                                    code = codeSnapshot,
                                    hintLevel = nextLevel,
                                    practiceAttempted = practiceAttempted
                                )
                                response = coordinator.run(
                                    null,
                                    request,
                                    Triple(lesson.hintLevel1, lesson.hintLevel2, lesson.hintLevel3)
                                )
                                busy = false
                            }
                        },
                        enabled = !busy && hintLevel < 3,
                        modifier = Modifier.fillMaxWidth().testTag("ai_hint_request")
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(if (hintLevel == 0) "무료 힌트 1 받기" else "무료 힌트 ${hintLevel + 1}/3")
                    }
                    if (hintLevel >= 3) Text("3단계 힌트를 모두 사용했습니다. 이제 직접 수정·실행해 보세요.", fontSize = 12.sp)
                }

                AiLearningMode.COLLABORATE -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (hasKey) "실제 AI 협업 연결 준비됨 · 호출 시 비용 발생 가능" else "실제 AI 협업에는 API Key가 필요합니다.",
                            modifier = Modifier.weight(1f).testTag("ai_key_status"),
                            fontSize = 12.sp
                        )
                        TextButton(onClick = { showKeyDialog = true }) { Text("AI 설정") }
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it; response = null },
                        modifier = Modifier.fillMaxWidth().testTag("ai_collab_question"),
                        label = { Text("AI와 같이 풀 질문") },
                        supportingText = { Text("API Key·비밀번호·토큰·주민번호·전화번호·이메일은 입력하지 마세요. 최대 4,000자") },
                        minLines = 3,
                        maxLines = 8
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = requestFullSolution,
                            onCheckedChange = { requestFullSolution = it },
                            enabled = availability.fullSolutionEnabled,
                            modifier = Modifier.testTag("ai_full_solution_toggle")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("전체 코드 요청", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (availability.fullSolutionEnabled) "허용됨 · 받은 뒤 직접 실행·검증 필수"
                                else "현재 Level/시도 상태에서는 잠김",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (busy) return@Button
                            busy = true
                            response = null
                            scope.launch {
                                val request = AiTutorRequest(
                                    lessonId = lesson.lessonId,
                                    curriculumType = lesson.curriculumType,
                                    level = lesson.stepNumber,
                                    moduleTitle = lesson.moduleTitle,
                                    lessonTitle = lesson.title,
                                    expectedOutcome = lesson.expectedOutcome,
                                    practiceLanguage = lesson.practiceLanguage,
                                    mode = AiLearningMode.COLLABORATE,
                                    task = taskForStep(currentStep),
                                    userMessage = attachDeterministicEvidence(question, deterministicEvidence),
                                    code = codeSnapshot,
                                    hintLevel = 1,
                                    practiceAttempted = practiceAttempted,
                                    explicitFullSolutionRequest = requestFullSolution
                                )
                                response = coordinator.run(
                                    keyStorage.getApiKey(),
                                    request,
                                    Triple(lesson.hintLevel1, lesson.hintLevel2, lesson.hintLevel3)
                                )
                                busy = false
                            }
                        },
                        enabled = !busy && question.trim().isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().testTag("ai_collab_request")
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("외부 AI로 같이 풀기")
                    }
                }
            }

            response?.let { result ->
                HorizontalDivider()
                val title = when {
                    result.success && result.usedLocalFallback -> "무료 로컬 힌트"
                    result.success -> "AI 응답"
                    else -> "AI 요청 실패"
                }
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (result.success) result.text else result.userMessage.orEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("ai_response"),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = if (result.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                if (!result.success && result.failureKind == AiFailureKind.MISSING_KEY) {
                    TextButton(onClick = { showKeyDialog = true }) { Text("API Key 설정 열기") }
                }
            }
        }
    }

    if (showKeyDialog) {
        AiKeyDialog(
            hasSavedKey = hasKey,
            onDismiss = { showKeyDialog = false },
            onSave = { key ->
                keyStorage.saveApiKey(key.trim())
                hasKey = !keyStorage.getApiKey().isNullOrBlank()
                showKeyDialog = false
                response = null
            },
            onClear = {
                keyStorage.clearApiKey()
                hasKey = false
                showKeyDialog = false
                response = null
            }
        )
    }
}

@Composable
private fun AiKeyDialog(
    hasSavedKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("실제 AI 연결") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "개인 API Key를 입력하면 ‘AI와 같이’에서만 실제 AI 튜터 요청이 전송됩니다. 무료 힌트는 Key가 있어도 외부 AI를 호출하지 않습니다.",
                    fontSize = 13.sp
                )
                Text(
                    "실제 AI 협업 사용량에 따라 API 비용이 발생할 수 있습니다. Key는 앱 바이너리에 넣지 않고 Android Keystore 기반 암호화 저장소에 보관합니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "학습 질문과 현재 코드/답안은 AI 제공자에게 전송될 수 있으므로 개인정보·비밀번호·토큰을 넣지 마세요. 앱 삭제 후 재설치 시 저장 Key는 다시 입력해야 할 수 있습니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().testTag("ai_key_input"),
                    label = { Text(if (hasSavedKey) "새 API Key로 교체" else "API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(input) }, enabled = input.trim().isNotEmpty()) { Text("암호화 저장") }
        },
        dismissButton = {
            Row {
                if (hasSavedKey) TextButton(onClick = onClear) { Text("저장 Key 삭제") }
                TextButton(onClick = onDismiss) { Text("취소") }
            }
        }
    )
}

private fun attachDeterministicEvidence(message: String, evidence: String): String {
    val clean = evidence.trim()
    if (clean.isBlank()) return message
    return buildString {
        append(message.trim())
        append("\n\n[앱의 deterministic 채점 결과 · 읽기 전용]\n")
        append(clean.take(2_500))
        append("\n\n위 PASS/FAIL은 앱 runtime과 deterministic test가 이미 결정했습니다. 재채점하거나 뒤집지 말고 원인 설명과 다음 확인점만 제시하세요.")
    }
}

private fun taskForStep(step: Int): AiTutorTask = when (step) {
    4 -> AiTutorTask.CODE_EXPLANATION
    7 -> AiTutorTask.DEBUG_GUIDANCE
    10 -> AiTutorTask.EXPLANATION_FEEDBACK
    else -> AiTutorTask.COLLABORATE
}
