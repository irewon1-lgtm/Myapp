package com.futuretech.poweruser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futuretech.poweruser.ui.theme.CodeTypography

data class SpacedRepetitionItem(
    val title: String,
    val definition: String,
    val analogy: String,
    val example: String,
    val intervalDays: Int
)

data class ErrorNoteItem(
    val errorType: String,
    val codeSnippet: String,
    val errorMessage: String,
    val correctionGuide: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedRepetitionReviewScreen(onNavigateBack: () -> Unit) {
    val sampleItems = remember {
        listOf(
            SpacedRepetitionItem(
                title = "API (Application Programming Interface)",
                definition = "앱과 서버가 서로 데이터를 주고받기 위한 약속된 접속 창구.",
                analogy = "식당에서 손님(앱)과 주방(서버) 사이에서 주문을 전달하는 점원.",
                example = "GET /api/v1/stock/quote",
                intervalDays = 3
            ),
            SpacedRepetitionItem(
                title = "MFA (Multi-Factor Authentication)",
                definition = "비밀번호 외에 추가로 인증번호 등을 검증하는 다중 보안 방식.",
                analogy = "현관문 열쇠를 연 후 지문 인식을 한 번 더 거치는 보안 시스템.",
                example = "비밀번호 입력 + SMS 6자리 인증번호",
                intervalDays = 7
            ),
            SpacedRepetitionItem(
                title = "JSON (JavaScript Object Notation)",
                definition = "Key-Value 쌍으로 구성된 표준 데이터 교환 형식.",
                analogy = "라벨(Key)이 붙은 상자(Value)들의 모음.",
                example = "{\"name\": \"삼성전자\", \"price\": 75000}",
                intervalDays = 14
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("간격 반복 복습 시스템", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("복습 예정 개념 카드 (1/3/7/14일 간격 주기)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(sampleItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text("${item.intervalDays}일 주기 복습", fontSize = 11.sp, modifier = Modifier.padding(6.dp, 2.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("정의: ${item.definition}", fontSize = 13.sp)
                        Text("쉬운 비유: ${item.analogy}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(item.example, style = CodeTypography, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalErrorNotesScreen(onNavigateBack: () -> Unit) {
    val sampleErrorNotes = remember {
        listOf(
            ErrorNoteItem(
                errorType = "JSON Trailing Comma Error",
                codeSnippet = "{\"name\": \"현대차\", \"price\": 200000,}",
                errorMessage = "JSONParseException: Unexpected trailing comma at line 1",
                correctionGuide = "JSON의 마지막 항목 뒤에는 콤마(,)를 붙이지 않습니다."
            ),
            ErrorNoteItem(
                errorType = "Python Indentation Error",
                codeSnippet = "def calc():\nprint('오류')",
                errorMessage = "IndentationError: expected an indented block after function definition",
                correctionGuide = "파이썬 함수나 조건문 내부 블록은 스페이스 4칸 들여쓰기를 맞춰야 합니다."
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("개인 오류 노트", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("자주 틀린 오답 및 실습 오류 자동 기록", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            items(sampleErrorNotes) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(note.errorType, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(note.codeSnippet, style = CodeTypography, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("에러 메시지: ${note.errorMessage}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Text("교정 가이드: ${note.correctionGuide}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}
