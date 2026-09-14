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

    var currentStep by remember { mutableStateOf(1) } // Step 1: 개념 -> Step 2: 코드읽기 -> Step 3: 직접실행 -> Step 4: 디버깅/검증 -> Step 5: 복습/설명
    var currentHintStep by remember { mutableStateOf(0) }
    var codeInput by remember { mutableStateOf(lesson.initialPracticeCode) }
    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var userExplanation by remember { mutableStateOf("") }
    var explanationFeedback by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf(-1) }
    var isAnswerChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${lesson.lessonId} - Step $currentStep/5", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
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
            // Step Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                if (i <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                    // Step 1: 개념 설명
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Step 1. 핵심 개념 이해 [이론]", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(lesson.explanation, fontSize = 14.sp, lineHeight = 22.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("학습 목표: ${lesson.expectedOutcome}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                2 -> {
                    // Step 2: 구조 / 코드 읽기
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Step 2. 예시 구조 및 코드 읽기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                }
                3 -> {
                    // Step 3: 직접 실행 샌드박스
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Step 3. 코드 실행 실습장", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    val engineLabel = when {
                                        lesson.lessonId.startsWith("I01") -> "HTML safe preview"
                                        lesson.lessonId.startsWith("I04") -> "SQLite SQL"
                                        lesson.lessonId.startsWith("I02") -> "문법 변환 데모"
                                        else -> "학습용 Mini Python"
                                    }
                                    Text(engineLabel, fontSize = 11.sp, modifier = Modifier.padding(6.dp, 2.dp), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("실습 코드 입력") },
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
                                                    "MINI_PYTHON_ERROR",
                                                    codeInput,
                                                    res.errorMessage ?: "Execution error",
                                                    "학습용 Mini Python 문법 규칙(기본 변수, print, if, while)을 점검하세요."
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("▶ 코드 실행")
                                }

                                OutlinedButton(
                                    onClick = { if (currentHintStep < 3) currentHintStep++ },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("💡 힌트 (${currentHintStep}/3)")
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
                                        else -> "힌트 3 (정답 가이드): ${lesson.hintLevel3}"
                                    }
                                    Text(hintText, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            if (lesson.lessonId.startsWith("I01")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("HTML/CSS/JS 안전한 미리보기 WebView Sandbox:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                HtmlPreviewSandboxView(htmlContent = codeInput)
                            } else {
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
                                                style = CodeTypography
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Step 4: 디버깅 및 AI 환각 검증
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Step 4. 고장난 예제 고치기 & AI 환각 판별", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("고장난 코드 디버깅:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(lesson.brokenCode, style = CodeTypography, color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("올바른 수정안: ${lesson.brokenCodeFix}", style = CodeTypography, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("AI 환각 검증 문제:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(lesson.aiHallucinationQuestion, fontSize = 13.sp)

                            lesson.aiHallucinationOptions.forEachIndexed { index, optionText ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedOption == index, onClick = {
                                        selectedOption = index
                                        isAnswerChecked = true
                                    })
                                    Text(optionText, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // Step 5: 설명하기 & 완주
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Step 5. 본인 말로 설명하기 [로컬 규칙 피드백]", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(lesson.explainPrompt, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = userExplanation,
                                onValueChange = { userExplanation = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("내 언어로 설명 입력") },
                                minLines = 3
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    explanationFeedback = if (userExplanation.length > 10) {
                                        "[LOCAL RULE] 피드백: 핵심 설명이 10자 이상 포함되었습니다. 우수한 이해도입니다."
                                    } else {
                                        "[LOCAL RULE] 피드백: 설명이 다소 짧습니다. 핵심 개념 키워드를 더 채워보세요."
                                    }
                                    onStepCompleted(lesson.lessonId, lesson.curriculumType, lesson.unitNumber)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("설명 제출 및 레슨 최종 완주 저장")
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

            // Bottom Step Controller Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { if (currentStep > 1) currentStep-- },
                    enabled = currentStep > 1,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("← 이전 단계")
                }

                Button(
                    onClick = {
                        if (currentStep < 5) currentStep++
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (currentStep == 5) "학습 완료" else "다음 단계 →")
                }
            }
        }
    }
}
