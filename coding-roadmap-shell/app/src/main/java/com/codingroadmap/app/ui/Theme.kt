package com.codingroadmap.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ivory = Color(0xFFF7F3EA)
val IvoryCard = Color(0xFFFFFCF6)
val Ink = Color(0xFF2B2924)
val Muted = Color(0xFF7B7469)
val Gold = Color(0xFFB38A49)
val GoldSoft = Color(0xFFE8D7B7)
val Forest = Color(0xFF29483B)
val ForestSoft = Color(0xFFE2E9E2)
val ReaderBg = Color(0xFF121311)
val ReaderSurface = Color(0xFF1A1B18)
val ReaderSurface2 = Color(0xFF23231F)
val ReaderText = Color(0xFFEAE5DC)
val ReaderMuted = Color(0xFFA9A39A)
val ReaderGold = Color(0xFFD0AD70)

@Composable
fun RoadmapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Gold,
            background = Ivory,
            surface = IvoryCard,
            onSurface = Ink
        ),
        content = content
    )
}
