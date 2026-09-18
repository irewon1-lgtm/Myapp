package com.futuretech.poweruser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CodingTocTrack(
    val number: Int,
    val level: String,
    val title: String,
    val goal: String,
    val chapters: List<String>,
    val milestone: String
)

private val codingTocTracks = listOf(
    CodingTocTrack(
        number = 1,
        level = "입문",
        title = "코딩 시작 — 실행부터 변수까지",
        goal = "코드가 무엇인지 알고 직접 실행해 본다.",
        chapters = listOf(
            "컴퓨터가 코드를 실행하는 전체 그림",
            "개발 도구와 실행 버튼",
            "값과 변수",
            "숫자·문자·참거짓",
            "입력과 출력",
            "오류 메시지 읽는 법",
            "작은 계산 프로그램",
            "첫 미니 프로젝트"
        ),
        milestone = "완료 목표 · 코드를 직접 실행하고 간단한 프로그램을 만든다."
    ),
    CodingTocTrack(
        number = 2,
        level = "입문",
        title = "Python 기초 — 조건문부터 함수까지",
        goal = "기본 문법을 실제 프로그램 흐름으로 연결한다.",
        chapters = listOf(
            "조건문 if",
            "반복문 for·while",
            "함수 만들기",
            "리스트와 딕셔너리",
            "문자열 다루기",
            "파일 읽기와 쓰기",
            "예외 처리",
            "Python 자동화 미니 프로젝트"
        ),
        milestone = "완료 목표 · 반복 작업을 자동화하는 작은 Python 프로그램을 만든다."
    ),
    CodingTocTrack(
        number = 3,
        level = "기초",
        title = "문제 해결 — 코드를 짜기 전에 생각하는 법",
        goal = "문제를 작은 단계로 나누고 검증하는 습관을 만든다.",
        chapters = listOf(
            "문제를 입력·처리·출력으로 나누기",
            "순서도와 의사코드",
            "리스트·스택·큐",
            "집합과 딕셔너리",
            "검색과 정렬의 기본",
            "시간 복잡도 감 잡기",
            "테스트 케이스 만들기",
            "문제 해결 미니 프로젝트"
        ),
        milestone = "완료 목표 · 문제를 쪼개고 스스로 풀이 순서를 설계한다."
    ),
    CodingTocTrack(
        number = 4,
        level = "기초",
        title = "웹의 기본 — 화면과 인터넷이 연결되는 구조",
        goal = "웹페이지와 서버가 어떻게 연결되는지 이해한다.",
        chapters = listOf(
            "웹페이지가 열리는 전체 흐름",
            "HTML로 구조 만들기",
            "CSS로 화면 꾸미기",
            "브라우저 개발자 도구",
            "주소·도메인·URL",
            "HTTP 요청과 응답",
            "폼과 사용자 입력",
            "첫 웹페이지 미니 프로젝트"
        ),
        milestone = "완료 목표 · 직접 만든 웹 화면을 브라우저에서 실행한다."
    ),
    CodingTocTrack(
        number = 5,
        level = "기초",
        title = "TypeScript — 움직이는 웹앱 만들기",
        goal = "JavaScript 실행 흐름과 타입을 함께 익힌다.",
        chapters = listOf(
            "JavaScript와 TypeScript의 관계",
            "변수·함수·객체 다시 보기",
            "타입과 interface",
            "배열과 객체 다루기",
            "이벤트와 화면 상태",
            "비동기·Promise·async/await",
            "fetch로 API 호출하기",
            "TypeScript 미니 웹앱"
        ),
        milestone = "완료 목표 · 사용자 입력과 API가 연결된 작은 웹앱을 만든다."
    ),
    CodingTocTrack(
        number = 6,
        level = "기초",
        title = "SQL과 데이터 — 저장하고 다시 찾는 법",
        goal = "앱의 데이터를 표 구조로 설계하고 직접 조회한다.",
        chapters = listOf(
            "데이터베이스·테이블·행·열",
            "SELECT로 데이터 조회",
            "INSERT·UPDATE·DELETE",
            "WHERE·ORDER BY·GROUP BY",
            "JOIN으로 표 연결하기",
            "기본키·외래키와 데이터 설계",
            "인덱스와 조회 속도",
            "트랜잭션과 데이터 안전",
            "SQL 미니 프로젝트"
        ),
        milestone = "완료 목표 · 앱에 필요한 데이터를 직접 설계하고 SQL로 다룬다."
    ),
    CodingTocTrack(
        number = 7,
        level = "실전",
        title = "API와 백엔드 — 앱 뒤쪽 만들기",
        goal = "요청을 받고 데이터베이스와 연결하는 서버를 만든다.",
        chapters = listOf(
            "서버와 API의 역할",
            "route와 endpoint",
            "path·query·header·body",
            "입력 검증",
            "controller·service·repository",
            "데이터베이스 연결",
            "로그인과 권한의 기본",
            "오류 응답과 로그",
            "백엔드 API 미니 프로젝트"
        ),
        milestone = "완료 목표 · 데이터 저장·조회가 되는 간단한 API를 만든다."
    ),
    CodingTocTrack(
        number = 8,
        level = "실전",
        title = "Git·테스트·디버깅 — 망가뜨리지 않고 고치기",
        goal = "변경을 기록하고 오류를 재현·검증하는 방법을 익힌다.",
        chapters = listOf(
            "Git이 저장하는 상태",
            "commit으로 변경 기록하기",
            "branch와 merge",
            "충돌 해결과 되돌리기",
            "테스트의 기본",
            "오류 재현과 디버깅",
            "로그와 stack trace",
            "빌드·CI·배포의 기본"
        ),
        milestone = "완료 목표 · 변경 기록과 테스트를 남기며 안전하게 앱을 수정한다."
    ),
    CodingTocTrack(
        number = 9,
        level = "실전",
        title = "Android 앱 — Galaxy에서 직접 실행하기",
        goal = "휴대폰에서 작동하는 Android 앱의 기본 구조를 만든다.",
        chapters = listOf(
            "Android 앱의 전체 구조",
            "Kotlin 필수 문법",
            "Jetpack Compose 화면 만들기",
            "상태와 사용자 이벤트",
            "화면 이동 Navigation",
            "휴대폰 저장소 사용",
            "API와 네트워크 연결",
            "권한·백그라운드 작업",
            "APK 빌드와 설치",
            "Android 미니 앱"
        ),
        milestone = "완료 목표 · 직접 만든 APK를 Galaxy에 설치해 실행한다."
    ),
    CodingTocTrack(
        number = 10,
        level = "실전",
        title = "AI 활용 개발 — LLM을 앱 기능으로 넣기",
        goal = "AI를 단순 채팅이 아니라 앱의 기능으로 연결한다.",
        chapters = listOf(
            "LLM이 입력을 처리하는 기본 구조",
            "프롬프트와 context",
            "구조화된 출력",
            "AI API 연결",
            "embedding과 검색",
            "RAG의 기본",
            "tool calling",
            "agent 작업 흐름",
            "eval과 결과 검증",
            "AI 미니 앱"
        ),
        milestone = "완료 목표 · AI 기능이 실제로 작동하는 작은 앱을 만든다."
    ),
    CodingTocTrack(
        number = 11,
        level = "심화",
        title = "시스템 심화 — 운영·보안·성능까지",
        goal = "앱이 커졌을 때 필요한 내부 구조와 운영 지식을 익힌다.",
        chapters = listOf(
            "프로세스·메모리·운영체제",
            "네트워크와 연결",
            "인증·권한·보안",
            "동시성과 비동기 처리",
            "성능·병목·캐시",
            "아키텍처와 모듈 분리",
            "컨테이너와 클라우드",
            "배포·관측·장애 대응",
            "최종 통합 프로젝트 설계"
        ),
        milestone = "완료 목표 · 작은 서비스를 설계하고 운영 관점까지 설명한다."
    )
)

@Composable
fun CodingTocScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "코딩 로드맵",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "완전 초보 → 직접 앱 제작 → AI 활용 → 심화",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "현재 단계: 목차만 구성 · 본문 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(codingTocTracks, key = { it.number }) { track ->
                CodingTrackCard(track)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "본문·문제·복습·점수·프로젝트 기능은 목차 확정 후 추가합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun CodingTrackCard(track: CodingTocTrack) {
    var expanded by rememberSaveable(track.number) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TRACK ${track.number.toString().padStart(2, '0')} · ${track.level}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(text = if (expanded) "닫기" else "${track.chapters.size}개 목차")
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.goal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                track.chapters.forEachIndexed { index, chapter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = (index + 1).toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = chapter,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = track.milestone,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
