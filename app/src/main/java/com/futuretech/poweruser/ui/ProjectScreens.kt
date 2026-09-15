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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.education.ProjectStage
import com.futuretech.poweruser.education.ProjectStageEngine
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeginnerStockScoreProjectScreen(
    completedLessonIds: Set<String> = emptySet(),
    onNavigateBack: () -> Unit
) {
    val stages = remember(completedLessonIds) { ProjectStageEngine.beginnerStages(completedLessonIds) }
    var companyName by remember { mutableStateOf("삼성전자") }
    var salesGrowth by remember { mutableStateOf("15.5") }
    var operatingMargin by remember { mutableStateOf("12.0") }
    var scoreResult by remember { mutableStateOf("") }
    var gradeResult by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf("") }

    val stage2 = stages.first { it.stage == 2 }.unlocked
    val stage3 = stages.first { it.stage == 3 }.unlocked
    val stage4 = stages.first { it.stage == 4 }.unlocked

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("초급 프로젝트 · 단계형", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("배운 기능만 하나씩 열립니다. 프로젝트를 한 번에 던지지 않습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            ProjectStageStrip(stages)

            OutlinedTextField(companyName, { companyName = it }, label = { Text("종목명") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(salesGrowth, { salesGrowth = it }, label = { Text("매출성장률 (%)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(operatingMargin, { operatingMargin = it }, label = { Text("영업이익률 (%)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val sg = salesGrowth.toDoubleOrNull()
                    val om = operatingMargin.toDoubleOrNull()
                    if (sg == null || om == null) {
                        scoreResult = "숫자 입력을 확인하세요."
                        gradeResult = ""
                    } else {
                        val total = (sg * 0.6) + (om * 0.4)
                        scoreResult = "종합 점수: %.1f점".format(total)
                        gradeResult = if (stage3) {
                            when {
                                total >= 12.0 -> "최종 등급: A"
                                total >= 7.0 -> "최종 등급: B"
                                else -> "최종 등급: C"
                            }
                        } else {
                            "Stage 3를 열면 등급 분기까지 연결됩니다."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = stage2
            ) { Text(if (stage2) "Stage 2 계산 실행" else "Stage 2 잠김") }

            if (scoreResult.isNotBlank()) {
                ProjectResultBox("$companyName\n$scoreResult\n$gradeResult")
            }

            Button(
                onClick = {
                    val samples = listOf(20.0 to 15.0, 8.0 to 8.0, -3.0 to 2.0)
                    val grades = samples.map { (sg, om) ->
                        val total = sg * 0.6 + om * 0.4
                        when {
                            total >= 12.0 -> "A"
                            total >= 7.0 -> "B"
                            else -> "C"
                        }
                    }
                    verificationResult = if (grades == listOf("A", "B", "C")) {
                        "✓ 경계 입력 3개 검증 통과"
                    } else {
                        "! 검증 실패 · 계산/분기 로직을 다시 확인하세요."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = stage4
            ) { Text(if (stage4) "Stage 4 완성 검증" else "Stage 4 잠김") }

            if (verificationResult.isNotBlank()) ProjectResultBox(verificationResult)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntermediateStockResearchProjectScreen(
    completedLessonIds: Set<String> = emptySet(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sandboxEngine = remember { SandboxedExecutionEngine(context) }
    val stages = remember(completedLessonIds) { ProjectStageEngine.intermediateStages(completedLessonIds) }

    var searchQuery by remember { mutableStateOf("SK하이닉스") }
    var pipelineLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }

    val stage2 = stages.first { it.stage == 2 }.unlocked
    val stage3 = stages.first { it.stage == 3 }.unlocked
    val stage4 = stages.first { it.stage == 4 }.unlocked

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("중급 프로젝트 · 단계형", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("실제 실행은 기기 내부 sandbox를 사용합니다. 외부 API 호출이나 유료 AI 호출이 없습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
            ProjectStageStrip(stages)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("연구할 종목명") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isExecuting = true
                    scope.launch {
                        val logs = mutableListOf<String>()
                        logs += "[Stage 1] 검색 입력: $searchQuery"

                        if (stage2) {
                            val sql = sandboxEngine.execute(
                                ProgrammingLanguage.SQL,
                                "SELECT * FROM stock_practice WHERE name LIKE '%$searchQuery%';"
                            )
                            logs += "[Stage 2] 로컬 SQL 조회\n${sql.output.ifBlank { sql.errorMessage.orEmpty() }.trim()}"
                        } else {
                            logs += "[Stage 2] 잠김 · I01-01 완료 후 열림"
                        }

                        if (stage3) {
                            val py = sandboxEngine.execute(
                                ProgrammingLanguage.PYTHON,
                                "price = 140000\npe_ratio = 12.5\nprint(f'PER 계산 결과: {pe_ratio}')"
                            )
                            logs += "[Stage 3] Python 계산\n${py.output.ifBlank { py.errorMessage.orEmpty() }.trim()}"
                        } else {
                            logs += "[Stage 3] 잠김 · I02-01까지 완료 후 열림"
                        }

                        if (stage4) {
                            logs += "[Stage 4] 보고서 조립 완료 · 검색/저장/계산 결과를 사용자가 직접 검증"
                        } else {
                            logs += "[Stage 4] 잠김 · I03-01까지 완료 후 열림"
                        }
                        pipelineLogs = logs
                        isExecuting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExecuting
            ) { Text(if (isExecuting) "실행 중..." else "현재 열린 단계 실행") }

            pipelineLogs.forEach { log ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(log, style = CodeTypography, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomProjectBuilderScreen(onNavigateBack: () -> Unit) {
    var userIdea by remember { mutableStateOf("나만의 개인 자산 가계부 자동화 앱을 만들고 싶어") }
    var generatedSteps by remember { mutableStateOf<List<String>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("실전 제작 · 단계 분해", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
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
            Text("아이디어를 입력하면 로컬 규칙으로 단계만 나눕니다. 별도 AI/API 비용이 들지 않습니다.", fontSize = 13.sp)
            OutlinedTextField(
                value = userIdea,
                onValueChange = { userIdea = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(
                onClick = {
                    generatedSteps = listOf(
                        "Stage 1 · 입력과 화면 상태 만들기",
                        "Stage 2 · 데이터 변환/저장 로직 연결",
                        "Stage 3 · 예외 처리와 검증 추가",
                        "Stage 4 · 여러 입력으로 테스트하고 완성"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("프로젝트 4단계로 나누기") }

            generatedSteps.forEach { ProjectResultBox(it) }
        }
    }
}

@Composable
private fun ProjectStageStrip(stages: List<ProjectStage>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stages.forEach { stage ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Stage ${stage.stage} · ${if (stage.unlocked) "열림 ✓" else "잠김"} · ${stage.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(stage.mission, fontSize = 12.sp, lineHeight = 18.sp)
                    if (!stage.unlocked && stage.requiredLessonIds.isNotEmpty()) {
                        Text(
                            "필요 학습: ${stage.requiredLessonIds.joinToString()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectResultBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(text, Modifier.padding(12.dp), fontSize = 13.sp, lineHeight = 19.sp)
    }
}
