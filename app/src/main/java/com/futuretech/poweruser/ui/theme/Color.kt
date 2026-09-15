package com.futuretech.poweruser.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val AiBackground = Color(0xFF070A12)
val AiSurface = Color(0xFF0D1320)
val AiSurfaceRaised = Color(0xFF121A2B)
val AiSurfaceSoft = Color(0xFF172033)
val AiCyan = Color(0xFF6BE7FF)
val AiViolet = Color(0xFF9B8CFF)
val AiGreen = Color(0xFF59F2AE)
val AiAmber = Color(0xFFFFC66B)
val AiRed = Color(0xFFFF7187)
val AiText = Color(0xFFF4F7FF)
val AiTextMuted = Color(0xFF9AA8BF)
val AiBorder = Color(0xFF263248)

val DarkColorScheme = darkColorScheme(
    primary = AiCyan,
    onPrimary = Color(0xFF001218),
    primaryContainer = Color(0xFF0D3340),
    onPrimaryContainer = Color(0xFFC8F5FF),
    secondary = AiViolet,
    onSecondary = Color(0xFF120B2B),
    secondaryContainer = Color(0xFF2A214E),
    onSecondaryContainer = Color(0xFFE9E2FF),
    tertiary = AiGreen,
    onTertiary = Color(0xFF002116),
    tertiaryContainer = Color(0xFF123B2D),
    onTertiaryContainer = Color(0xFFC6FFE4),
    error = AiRed,
    background = AiBackground,
    onBackground = AiText,
    surface = AiSurface,
    onSurface = AiText,
    surfaceVariant = AiSurfaceSoft,
    onSurfaceVariant = AiTextMuted,
    outline = AiBorder,
    outlineVariant = Color(0xFF1B2639)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF005F73),
    secondary = Color(0xFF5B4CC4),
    tertiary = Color(0xFF007A52),
    background = Color(0xFFF6F8FC),
    surface = Color.White,
    onBackground = Color(0xFF101828),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF4B5870),
    outline = Color(0xFFD5DCE8)
)
