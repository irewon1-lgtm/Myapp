package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderBody(
    title: String, chapter: Int, count: Int, scale: Float, modifier: Modifier = Modifier,
    onPrev: () -> Unit, onNext: () -> Unit
) {
    Column(modifier) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text("READING MODE", color = ReaderGold, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
            Text(title, color = ReaderText, fontSize = (31 * scale).sp, fontWeight = FontWeight.SemiBold, lineHeight = (40 * scale).sp, modifier = Modifier.padding(top = 10.dp))
            Text(
                "본문 콘텐츠는 다음 단계에서 JSON/Markdown으로 연결됩니다. 지금 버전은 UI·리더·저장 구조를 먼저 검증하기 위한 앱 쉘입니다.",
                color = ReaderMuted, fontSize = (16 * scale).sp, lineHeight = (28 * scale).sp, modifier = Modifier.padding(top = 24.dp)
            )
            Box(Modifier.fillMaxWidth().padding(top = 28.dp).background(ReaderSurface, RoundedCornerShape(20.dp)).padding(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("콘텐츠 슬롯", color = ReaderGold, fontSize = (13 * scale).sp, fontWeight = FontWeight.SemiBold)
                    Text("• 핵심 설명\n• 아주 쉽게\n• 코드 블록\n• 다이어그램\n• 실습 / 확인문제", color = ReaderText, fontSize = (15 * scale).sp, lineHeight = (25 * scale).sp)
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 16.dp).background(ReaderSurface2, RoundedCornerShape(20.dp)).padding(18.dp)) {
                Column {
                    Text("diagram://slot", color = ReaderGold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("개념 시각화 컴포넌트 연결 위치", color = ReaderText, fontSize = (15 * scale).sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = chapter > 0, onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "이전", tint = if (chapter > 0) ReaderText else ReaderMuted.copy(alpha = .35f)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${chapter + 1} / $count", color = ReaderMuted, fontSize = 11.sp)
                ProgressBar((chapter + 1) / count.toFloat(), Modifier.padding(top = 5.dp), dark = true)
            }
            IconButton(enabled = chapter < count - 1, onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "다음", tint = if (chapter < count - 1) ReaderText else ReaderMuted.copy(alpha = .35f)) }
        }
    }
}
