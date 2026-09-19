package com.codingroadmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingroadmap.app.data.TrackMeta

@Composable
fun SearchScreen(track: TrackMeta, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    SystemBars(false)
    var query by remember { mutableStateOf("") }
    val hits = track.chapters.withIndex().filter { query.isBlank() || it.value.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize().background(Ivory).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "뒤로", tint = Ink) }
            Text("검색", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("챕터 검색") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hits.size) { i ->
                val hit = hits[i]
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(IvoryCard).clickable { onOpen(hit.index) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Chapter ${hit.index + 1}", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(hit.value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Muted)
                }
            }
        }
    }
}
