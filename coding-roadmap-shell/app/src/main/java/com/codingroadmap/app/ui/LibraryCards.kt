package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.TrackMeta

@Composable
fun TrackSummary(
    track: TrackMeta,
    progress: Float,
    contentReady: Boolean,
    onContinue: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF7EEDF))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(112.dp).height(170.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(if (track.id == 2) Forest else Color(0xFFF2E9DB))
                .padding(12.dp)
        ) {
            val fg = if (track.id == 2) Color(0xFFF3EEE7) else Ink
            Column {
                Text(
                    "TRACK ${track.id.toString().padStart(2, '0')}",
                    fontSize = 9.sp,
                    color = fg.copy(.72f)
                )
                Text(
                    track.title,
                    fontFamily = EditorialSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    track.subtitle,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    color = fg.copy(.72f),
                    modifier = Modifier.padding(top = 6.dp)
                )
                TrackCoverArt(track.id, Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    if (track.id == 1) "FOUNDATION\nREAD · RUN · FIX" else "PYTHON\nNEXT JOURNEY",
                    fontFamily = EditorialSerif,
                    fontSize = 7.sp,
                    lineHeight = 9.sp,
                    color = fg.copy(.65f)
                )
            }
        }

        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                if (track.id == 1)
                    "코드를 읽고, 바꾸고,\n오류를 고치는 첫 번째 트랙"
                else
                    "조건문부터 함수까지,\nPython의 흐름을 확장합니다.",
                fontFamily = EditorialSerif,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = Ink
            )

            Text(
                if (track.id == 1)
                    "단어집부터 개념·도식·예제·실습·확인문제·정리까지 한 흐름으로 연결합니다."
                else
                    "TRACK 01에서 만든 기초 위에 판단·반복·함수·자료구조·파일·예외 처리를 연결합니다.",
                fontFamily = EditorialSerif,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Muted,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(progress, Modifier.weight(1f))
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.MenuBook, null, tint = Muted, modifier = Modifier.size(16.dp))
                Text("${track.chapters.size}개 챕터", fontSize = 10.sp, color = Muted)
                Icon(Icons.Rounded.Schedule, null, tint = Muted, modifier = Modifier.size(16.dp))
                Text(if (track.id == 1) "64페이지" else "다음 트랙", fontSize = 10.sp, color = Muted)
            }

            if (contentReady) {
                Text(
                    "이어보기  →",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 11.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Gold)
                        .clickable(onClick = onContinue)
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                )
            } else {
                Text(
                    "TRACK 01 완료 후 자동으로 여기로 이동합니다",
                    color = Gold,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 11.dp)
                )
            }
        }
    }
}

@Composable
fun TrackDescriptionPanel(track: TrackMeta, contentReady: Boolean) {
    val paragraphs = if (track.id == 1) {
        listOf(
            "이 트랙은 코딩을 거의 처음 접하는 사람이 짧은 Python 코드를 겁내지 않고 읽을 수 있도록 설계했습니다. 명령어를 많이 외우는 대신 코드가 실행되어 결과가 생기는 흐름, 변수와 타입, 입력과 형 변환, 오류 메시지 읽기, 간단한 계산을 차근차근 연결합니다.",
            "각 챕터는 단어집 2페이지로 시작합니다. 그 챕터에서 실제로 쓰는 단어를 한국어·영어 이름과 함께 익히고, 2~3문장 설명·어디서 쓰는지·간단한 예시까지 본 뒤 개념 페이지로 넘어갑니다.",
            "본문에서는 개념을 읽고 도식으로 흐름을 확인한 뒤 예제 코드를 실행합니다. 그다음 값을 직접 바꾸는 실습, 확인문제, 정리 순서로 반복합니다. 목표는 정답 암기가 아니라 ‘읽기 → 예상 → 수정 → 실행 → 비교’ 습관을 만드는 것입니다.",
            "TRACK 01이 끝나면 짧은 Python 코드의 흐름을 설명하고, 입력값과 타입을 확인하고, 오류 이름과 문제 줄을 읽어 검색이나 AI 질문에 필요한 정보를 정리하고, 작은 계산 프로그램을 직접 수정할 수 있는 상태를 목표로 합니다."
        )
    } else {
        listOf(
            "TRACK 02는 기존 v0.7.0의 방향대로 ‘Python 기초 — 조건문부터 함수까지’를 이어가는 다음 단계입니다. 조건에 따라 다른 길을 선택하는 if, 반복 작업을 줄이는 for·while, 코드를 묶어 재사용하는 함수까지 확장합니다.",
            "그다음 list·dict로 여러 데이터를 다루고, 문자열 정리, 파일 읽기·쓰기, 예외 처리까지 연결합니다. 마지막에는 여러 기능을 한 프로그램에 모아 반복 작업을 자동화하는 작은 Python 프로젝트로 마무리하는 흐름입니다.",
            if (contentReady)
                "현재 본문이 연결되어 있으므로 챕터를 눌러 바로 이어갈 수 있습니다."
            else
                "이번 업데이트는 TRACK 01 완성에 집중했기 때문에 TRACK 02에서는 목차와 연결 흐름만 먼저 복구했습니다. TRACK 01 마지막 페이지에서 오른쪽으로 넘기면 이 화면으로 자동 이동합니다."
        )
    }

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(IvoryCard)
            .padding(18.dp)
    ) {
        Text(
            "이 트랙에서 무엇을 배우나요?",
            fontFamily = EditorialSerif,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
        paragraphs.forEach { paragraph ->
            Text(
                paragraph,
                fontFamily = EditorialSerif,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                color = Muted,
                modifier = Modifier.padding(top = 11.dp)
            )
        }

        if (track.id == 1) {
            Text(
                "학습 순서",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "단어집 1/2 → 단어집 2/2 → 개념 → 시각화 → 예제 → 실습 → 확인문제 → 정리",
                fontFamily = EditorialSerif,
                color = Ink,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun ChapterRow(
    index: Int,
    title: String,
    visited: Boolean,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (current) Color(0xFFF1E2C9) else IvoryCard
    val minutes = listOf(12, 14, 16, 18, 20, 22, 18, 28).getOrElse(index) { 18 }
    val sections = 8

    Row(
        Modifier.fillMaxWidth().height(74.dp)
            .shadow(1.dp, RoundedCornerShape(17.dp))
            .clip(RoundedCornerShape(17.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFF2E5D2)),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", fontFamily = EditorialSerif, fontSize = 18.sp, color = Ink)
        }

        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                "Chapter ${index + 1} · $title",
                fontFamily = EditorialSerif,
                fontSize = 16.sp,
                color = if (enabled) Ink else Muted,
                maxLines = 1
            )
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MenuBook, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text(" ${minutes}분  |  ", fontSize = 10.sp, color = Muted)
                Icon(Icons.Rounded.Schedule, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text(" ${sections}개 섹션", fontSize = 10.sp, color = Muted)
            }
        }

        when {
            !enabled -> Text(
                "다음 트랙",
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFFF0ECE5))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )

            current -> Text(
                "⌁  읽는 중",
                color = Gold,
                fontSize = 11.sp,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(Color.White.copy(.72f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )

            visited -> Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0xFFE8ECE0))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF718466), modifier = Modifier.size(15.dp))
                Text(" 완료", fontSize = 11.sp, color = Color(0xFF607356))
            }
        }

        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Muted)
    }
}
