package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.ReaderPrefs

@Composable
fun SettingsScreen(
    prefs: ReaderPrefs,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onSaved: () -> Unit,
    onTextScale: (Float) -> Unit
) {
    SystemBars(false)
    Scaffold(
        containerColor = Ivory,
        bottomBar = { BottomNav("settings", onHome, onLibrary, onSaved, {}) }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("설정", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
                Text("읽기 환경과 앱 동작을 조절합니다.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(IvoryCard).padding(18.dp)) {
                    Text("본문 글자 크기", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("리더 화면에만 적용됩니다.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Slider(value = prefs.textScale, onValueChange = onTextScale, valueRange = .9f..1.35f, modifier = Modifier.padding(top = 10.dp))
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(IvoryCard).padding(18.dp)) {
                    Text("다크 리더", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("본문은 항상 눈부심을 줄인 다크 테마로 열립니다. 홈·서재·목차는 밝은 아이보리 테마를 유지합니다.", color = Muted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(IvoryCard).padding(18.dp)) {
                    Text("콘텐츠 구조", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("학습 본문은 앱 코드와 분리된 assets/content 데이터로 연결하도록 설계했습니다.", color = Muted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
    }
}
