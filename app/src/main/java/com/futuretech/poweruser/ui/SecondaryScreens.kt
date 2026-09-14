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
import com.futuretech.poweruser.data.AppRepository
import com.futuretech.poweruser.data.PersonalErrorNoteEntity
import com.futuretech.poweruser.data.SpacedRepetitionItemEntity
import com.futuretech.poweruser.ui.theme.CodeTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacedRepetitionReviewScreen(
    repository: AppRepository,
    onNavigateBack: () -> Unit
) {
    var dueReviews by remember { mutableStateOf<List<SpacedRepetitionItemEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        dueReviews = repository.getDueReviews()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("간격 반복 복습 카운터", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("실제 복습 예정 개념 (Room DB 연동)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (dueReviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("오늘 복습할 예정인 개념 카드가 없습니다. 모든 복습을 완료했습니다!", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dueReviews) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(item.conceptTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("정의: ${item.definition}", fontSize = 13.sp)
                                Text("쉬운 비유: ${item.analogy}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                if (item.example.isNotEmpty()) {
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalErrorNotesScreen(
    errorNotesList: List<PersonalErrorNoteEntity>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 개인 오류 노트", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onNavigateBack) { Text("← 뒤로") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("실제 오답 및 샌드박스 오류 자동 누적 수집", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))

            if (errorNotesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text("아직 기록된 오류가 없습니다. 실습 중 에러가 발생하면 여기에 자동 수집됩니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(errorNotesList) { note ->
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
                                    Text(note.errorType, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                    ) {
                                        Text("발생 ${note.occurrenceCount}회", fontSize = 11.sp, modifier = Modifier.padding(6.dp, 2.dp), color = MaterialTheme.colorScheme.error)
                                    }
                                }
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
    }
}
