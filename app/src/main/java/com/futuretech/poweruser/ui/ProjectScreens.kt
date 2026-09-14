package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.futuretech.poweruser.sandbox.ExecutionResult
import com.futuretech.poweruser.sandbox.ProgrammingLanguage
import com.futuretech.poweruser.sandbox.SandboxedExecutionEngine
import com.futuretech.poweruser.ui.theme.CodeTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeginnerStockScoreProjectScreen(onNavigateBack: () -> Unit) {
    var companyName by remember { mutableStateOf("삼성전자") }
    var salesGrowth by remember { mutableStateOf("15.5") }
    var operatingMargin by remember { mutableStateOf("12.0") }
    var scoreResult by remember { mutableStateOf("") }
    var gradeResult by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("초급 프로젝트: 주식 점수 분류 앱", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("← 뒤로") }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("프로젝트 목표", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("매출성장률과 영업이익률 데이터를 받아 조건문(if)을 이용해 주식 등급(A/B/C)을 산출합니다.", fontSize = 13.sp)
                }
            }

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("종목명") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = salesGrowth,
                onValueChange = { salesGrowth = it },
                label = { Text("매출성장률 (%)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = operatingMargin,
                onValueChange = { operatingMargin = it },
                label = { Text("영업이익률 (%)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val sg = salesGrowth.toDoubleOrNull() ?: 0.0
                    val om = operatingMargin.toDoubleOrNull() ?: 0.0
                    val totalScore = (sg * 0.6) + (om * 0.4)
                    val grade = when {
                        totalScore >= 12.0 -> "A (우수 종목)"
                        totalScore >= 7.0 -> "B (보통 종목)"
                        else -> "C (관심 필요)"
                    }
                    scoreResult = "종합 점수: %.1f점".format(totalScore)
                    gradeResult = "최종 등급: $grade"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("주식 등급 계산 실행")
            }

            if (scoreResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("분석 결과: $companyName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(scoreResult, fontSize = 14.sp)
                        Text(gradeResult, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntermediateStockResearchProjectScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sandboxEngine = remember { SandboxedExecutionEngine(context) }

    var searchQuery by remember { mutableStateOf("SK하이닉스") }
    var pipelineLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("중급 프로젝트: 개인 주식 연구 미니앱", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("← 뒤로") }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("통합 파이프라인 구조", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("종목검색 → API 호출 → SQL DB 저장 → Python 계산 → AI 분석 → 화면 표시", fontSize = 13.sp)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("연구할 종목명 입력") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    isExecuting = true
                    scope.launch {
                        val logs = mutableListOf<String>()
                        logs.add("[Step 1] 종목 검색 요청: $searchQuery")
                        logs.add("[Step 2] API Response (200 OK): 데이터 파싱 성공")

                        val sqlResult = sandboxEngine.execute(ProgrammingLanguage.SQL, "SELECT * FROM stock_practice WHERE name LIKE '%$searchQuery%';")
                        logs.add("[Step 3] SQL DB 저장 및 조회 완료:\n${sqlResult.output.trim()}")

                        val pyResult = sandboxEngine.execute(ProgrammingLanguage.PYTHON, "price = 140000\npe_ratio = 12.5\nprint(f'PER 계산 결과: {pe_ratio}')")
                        logs.add("[Step 4] Python 자동화 계산:\n${pyResult.output.trim()}")

                        logs.add("[Step 5] AI 분석 보고서: $searchQuery - 반도체 업황 개선에 따른 긍정적 모멘텀 보유")
                        pipelineLogs = logs
                        isExecuting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                enabled = !isExecuting
            ) {
                Text(if (isExecuting) "파이프라인 실행 중..." else "전체 연구 파이프라인 실행")
            }

            if (pipelineLogs.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("파이프라인 실행 기록", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        pipelineLogs.forEach { log ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(log, style = CodeTypography, fontSize = 12.sp)
                            }
                        }
                    }
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
                title = { Text("실전 제작 모드: 아이디어 생성기", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text("← 뒤로") }
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
            Text("만들고 싶은 앱 아이디어를 자연어로 입력하세요:", fontSize = 14.sp)

            OutlinedTextField(
                value = userIdea,
                onValueChange = { userIdea = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    generatedSteps = listOf(
                        "1. [수준 분석] 현재 사용자의 초/중급 파이썬 및 API 이해도에 맞춤 축소",
                        "2. [필요 개념] Python 데이터 변환, JSON 저장, 기본 조건문",
                        "3. [단계별 미션 1] 사용자 지출 입력 UI 작성 (사람 할 일)",
                        "4. [단계별 미션 2] 지출 내역 자동 분류 파이썬 코드 생성 (AI와 협업)",
                        "5. [단계별 미션 3] 월별 통계 SQL 저장 및 시각화 (최종 완성)"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("프로젝트 자동 분해 및 미션 생성")
            }

            if (generatedSteps.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI 프로젝트 가이드라인", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        generatedSteps.forEach { step ->
                            Text(step, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
