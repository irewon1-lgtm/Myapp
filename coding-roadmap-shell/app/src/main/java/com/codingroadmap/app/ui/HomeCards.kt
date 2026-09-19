package com.codingroadmap.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.TrackMeta

@Composable
fun ContinueCard(track: TrackMeta, chapter: Int, progress: Float, onClick: () -> Unit) {
    val title = track.chapters.getOrNull(chapter) ?: "학습 시작"
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF5E8D0), Color(0xFFE2CDA7))))
            .clickable(onClick = onClick).padding(22.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .22f), size.minDimension * .42f, Offset(size.width * .9f, size.height * .15f))
        }
        Column(Modifier.fillMaxWidth()) {
            Text("이어 학습하기", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Text("TRACK 01 · ${track.title}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp))
            Text("Chapter ${chapter + 1} · $title", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(progress, Modifier.weight(1f))
                Text("${(progress * 100).toInt()}%", color = Ink, fontSize = 12.sp, modifier = Modifier.padding(start = 9.dp))
            }
            Box(
                Modifier.padding(top = 18.dp).clip(RoundedCornerShape(50)).background(Gold).padding(horizontal = 18.dp, vertical = 10.dp)
            ) { Text("이어보기  →", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun BookCard(track: TrackMeta, progress: Float, onClick: () -> Unit) {
    val bg = when (track.id) {
        1 -> Color(0xFFF1E6D4)
        2 -> Forest
        3 -> Color(0xFFE4D7C5)
        else -> Color(0xFFD9D4CC)
    }
    val fg = if (track.id == 2) Color(0xFFF1EEE8) else Ink
    Column(Modifier.width(150.dp).clickable(enabled = track.available, onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(10.dp)).background(bg).padding(16.dp)) {
            Column {
                Text("TRACK ${track.id.toString().padStart(2, '0')}", color = fg.copy(alpha = .7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(track.title, color = fg, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                Text(track.subtitle, color = fg.copy(alpha = .72f), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                Spacer(Modifier.weight(1f))
                Text(if (track.available) "LEARNING\nEDITION" else "COMING\nNEXT", color = fg.copy(alpha = .55f), fontSize = 9.sp)
            }
        }
        Text(track.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 9.dp))
        if (track.available) {
            ProgressBar(progress, Modifier.padding(top = 7.dp))
        } else {
            Text("준비 중", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}
