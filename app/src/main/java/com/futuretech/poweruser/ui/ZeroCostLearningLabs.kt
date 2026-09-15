package com.futuretech.poweruser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LocalApiScenario(val label: String) {
    OK("200 정상"),
    RATE_LIMIT("429 호출 제한"),
    TIMEOUT("응답 지연/Timeout"),
    OFFLINE("오프라인")
}

data class LocalApiSimulation(
    val title: String,
    val evidence: String,
    val nextAction: String
)

object ZeroCostLearningPolicy {
    private val secretPatterns = listOf(
        "API Key/토큰" to Regex("(?i)(sk-[A-Za-z0-9_-]{12,}|Bearer\\s+[A-Za-z0-9._~-]{16,}|api[_ -]?key\\s*[:=]\\s*\\S+|access[_ -]?token\\s*[:=]\\s*\\S+)"),
        "비밀번호" to Regex("(?i)(password|passwd|비밀번호)\\s*[:=]\\s*\\S{6,}"),
        "주민등록번호처럼 보이는 값" to Regex("(?<!\\d)\\d{6}-?[1-4]\\d{6}(?!\\d)"),
        "휴대전화번호" to Regex("(?<!\\d)01[016789]-?\\d{3,4}-?\\d{4}(?!\\d)"),
        "이메일 주소" to Regex("(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?![A-Za-z])")
    )

    fun simulateApi(scenario: LocalApiScenario): LocalApiSimulation = when (scenario) {
        LocalApiScenario.OK -> LocalApiSimulation(
            title = "정상 응답",
            evidence = "HTTP 200 · 응답 본문 수신",
            nextAction = "응답 구조를 검증한 뒤 화면 상태를 갱신합니다."
        )
        LocalApiScenario.RATE_LIMIT -> LocalApiSimulation(
            title = "호출 제한",
            evidence = "HTTP 429 · 짧은 시간에 호출이 너무 많음",
            nextAction = "즉시 무한 재시도하지 말고 대기한 뒤 제한적으로 다시 시도합니다."
        )
        LocalApiScenario.TIMEOUT -> LocalApiSimulation(
            title = "응답 지연",
            evidence = "제한 시간 안에 응답이 끝나지 않음",
            nextAction = "사용자에게 지연을 알리고 자동 무한 재시도 대신 중단/재시도 선택지를 줍니다."
        )
        LocalApiScenario.OFFLINE -> LocalApiSimulation(
            title = "네트워크 없음",
            evidence = "서버 연결 전에 네트워크 경로가 끊김",
            nextAction = "오프라인에서도 가능한 학습은 계속하고 네트워크 기능만 잠시 중지합니다."
        )
    }

    fun sensitiveInputTypes(text: String): List<String> = secretPatterns
        .filter { (_, regex) -> regex.containsMatchIn(text) }
        .map { it.first }
        .distinct()

    fun phishingAnswerIsSafe(choice: Int): Boolean = choice == 1
}

@Composable
fun ZeroCostLearningLab(lessonId: String) {
    when (lessonId) {
        "I03-04" -> ApiFailureLab()
        "B01-05" -> ReinstallPolicyLab()
        "B08-02" -> PhishingLab()
        "B08-03" -> SensitiveInputLab()
    }
}

@Composable
private fun LabCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ApiFailureLab() {
    var selected by rememberSaveable { mutableStateOf(LocalApiScenario.RATE_LIMIT.name) }
    var result by rememberSaveable { mutableStateOf<LocalApiSimulation?>(null) }
    val scenario = LocalApiScenario.valueOf(selected)

    LabCard(
        title = "로컬 API 실패 시뮬레이터",
        subtitle = "실제 서버를 호출하지 않습니다. 실행해도 API 비용 0원, 네트워크 사용 0회입니다."
    ) {
        LocalApiScenario.entries.forEach { item ->
            FilterChip(
                selected = item == scenario,
                onClick = { selected = item.name; result = null },
                label = { Text(item.label) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = { result = ZeroCostLearningPolicy.simulateApi(scenario) },
            modifier = Modifier.fillMaxWidth().testTag("zero_cost_api_run")
        ) { Text("▶ 로컬 실행") }

        result?.let {
            HorizontalDivider()
            Text(it.title, fontWeight = FontWeight.Bold)
            Text("증거: ${it.evidence}", fontSize = 13.sp)
            Text("다음 행동: ${it.nextAction}", fontSize = 13.sp, lineHeight = 20.sp)
            Text("실제 API 호출: 0회", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReinstallPolicyLab() {
    var choice by rememberSaveable { mutableIntStateOf(-1) }
    LabCard(
        title = "업데이트와 재설치의 데이터 차이",
        subtitle = "이 앱은 개인정보 보호를 위해 Android 자동 백업을 끈 상태입니다."
    ) {
        Text("상황: 학습 기록을 유지하고 새 버전만 적용하려고 합니다. 어떤 행동이 맞을까요?", fontSize = 13.sp)
        OutlinedButton(onClick = { choice = 0 }, modifier = Modifier.fillMaxWidth()) {
            Text("A. 기존 앱 위에 정상 업데이트")
        }
        OutlinedButton(onClick = { choice = 1 }, modifier = Modifier.fillMaxWidth()) {
            Text("B. 앱 삭제 후 다시 설치")
        }
        if (choice >= 0) {
            val correct = choice == 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (correct) {
                        "✓ 맞습니다. 같은 앱의 정상 업데이트는 일반적으로 로컬 학습 데이터를 유지합니다."
                    } else {
                        "! 삭제 후 재설치하면 이 앱의 로컬 학습 기록과 저장된 AI Key가 초기화될 수 있습니다. 데이터 보존이 목적이면 삭제하지 말고 업데이트를 사용하세요."
                    },
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun SensitiveInputLab() {
    var input by rememberSaveable { mutableStateOf("password=abcdefgh1234") }
    var checked by rememberSaveable { mutableStateOf(false) }
    val findings = ZeroCostLearningPolicy.sensitiveInputTypes(input)

    LabCard(
        title = "개인정보·비밀값 로컬 검사",
        subtitle = "입력한 내용은 이 기기 안에서 정규식으로만 검사합니다. AI 전송 0회, 외부 서버 전송 0회입니다."
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; checked = false },
            modifier = Modifier.fillMaxWidth().testTag("local_sensitive_input"),
            label = { Text("연습용 문장") },
            minLines = 2,
            maxLines = 5
        )
        Button(
            onClick = { checked = true },
            modifier = Modifier.fillMaxWidth().testTag("local_sensitive_check")
        ) { Text("기기 안에서 검사") }

        if (checked) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (findings.isEmpty()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (findings.isEmpty()) {
                        "✓ 현재 규칙에서 민감정보 패턴을 찾지 못했습니다. 그래도 실제 개인정보를 AI에 넣기 전에는 사람이 한 번 더 확인해야 합니다."
                    } else {
                        "! 전송 금지 권고: ${findings.joinToString(", ")} 감지"
                    },
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun PhishingLab() {
    var choice by rememberSaveable { mutableIntStateOf(-1) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LabCard(
        title = "피싱 판별 문제",
        subtitle = "학습용 가상 사례입니다. 실제 링크를 열지 않습니다."
    ) {
        Text(
            "문자: ‘결제가 차단됐습니다. 10분 안에 mybank-security.xyz에서 비밀번호를 확인하세요.’",
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        FilterChip(
            selected = choice == 0,
            onClick = { choice = 0; submitted = false },
            label = { Text("문자 링크를 열고 비밀번호 입력") },
            modifier = Modifier.fillMaxWidth()
        )
        FilterChip(
            selected = choice == 1,
            onClick = { choice = 1; submitted = false },
            label = { Text("링크를 닫고 공식 앱을 직접 열어 확인") },
            modifier = Modifier.fillMaxWidth()
        )
        FilterChip(
            selected = choice == 2,
            onClick = { choice = 2; submitted = false },
            label = { Text("문자에 답장해 진짜인지 질문") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { submitted = true },
            enabled = choice >= 0,
            modifier = Modifier.fillMaxWidth().testTag("phishing_submit")
        ) { Text("✓ 제출") }

        if (submitted) {
            val correct = ZeroCostLearningPolicy.phishingAnswerIsSafe(choice)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (correct) {
                        "✓ 정답. 급한 문구와 낯선 도메인에 끌려가지 말고, 메시지 링크와 분리된 공식 경로로 직접 확인합니다."
                    } else {
                        "! 오답. 발신자 표시나 급한 문구를 신뢰하지 말고 링크를 닫은 뒤 공식 앱/직접 입력한 공식 주소로 확인해야 합니다."
                    },
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
