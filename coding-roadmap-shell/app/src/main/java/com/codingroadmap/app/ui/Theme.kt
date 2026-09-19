package com.codingroadmap.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val Ivory = Color(0xFFFBF8F0)
val IvoryCard = Color(0xFFFFFDF8)
val Ink = Color(0xFF251F19)
val Muted = Color(0xFF8A7E70)
val Gold = Color(0xFFB88E4B)
val GoldSoft = Color(0xFFE8D4AE)
val Forest = Color(0xFF29483B)
val ForestSoft = Color(0xFFE2E9E2)
val ReaderBg = Color(0xFF17130E)
val ReaderSurface = Color(0xFF211D17)
val ReaderSurface2 = Color(0xFF1B1916)
val ReaderText = Color(0xFFF0E8DC)
val ReaderMuted = Color(0xFFB7ADA0)
val ReaderGold = Color(0xFFD7B26D)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 38.sp),
    headlineLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 30.sp),
    headlineMedium = TextStyle(fontFamily = EditorialSerif, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = EditorialSerif, fontSize = 20.sp),
    bodyLarge = TextStyle(fontFamily = CleanSans, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = CleanSans, fontSize = 14.sp)
)

@Composable
fun RoadmapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Gold,
            background = Ivory,
            surface = IvoryCard,
            onSurface = Ink
        ),
        typography = AppTypography,
        content = content
    )
}
