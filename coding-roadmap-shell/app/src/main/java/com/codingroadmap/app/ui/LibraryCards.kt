package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.TrackMeta

@Composable
fun TrackSummary(track: TrackMeta, progress: Float, onContinue: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(IvoryCard).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(104.dp).height(146.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF1E6D4)).padding(13.dp)
        ) {
            Column {
                Text("TRACK 01", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(track.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.weight(1f))
                Text("A NEW\nJOURNEY", color = Muted, fontSize = 8.sp)
            }
        }
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text("지금의 한 걸음이,\n더 넓은 세상을 만듭니다.", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp)
            Text("8개 챕터 · 초급", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            ProgressBar(progress, Modifier.padding(top = 14.dp))
            Text("${(progress * 100).toInt()}%", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
            Text("이어보기  →", color = Gold, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp).clickable(onClick = onContinue))
        }
    }
}

@Composable
fun ChapterRow(index: Int, title: String, visited: Boolean, current: Boolean, onClick: () -> Unit) {
    val bg = if (current) Color(0xFFF1E3CA) else IvoryCard
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFF1E6D4)), contentAlignment = Alignment.Center) {
            Text((index + 1).toString(), color = Ink, fontSize = 17.sp)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("Chapter ${index + 1} · $title", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(if (visited) "최근 학습 기록 있음" else "아직 시작하지 않음", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (visited) Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF6C8167), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Muted)
    }
}
