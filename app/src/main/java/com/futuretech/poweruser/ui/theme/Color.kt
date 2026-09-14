package com.futuretech.poweruser.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val SlatePrimary = Color(0xFF1E293B)
val SlateSecondary = Color(0xFF334155)
val AccentBlue = Color(0xFF2563EB)
val AccentGreen = Color(0xFF059669)
val AccentAmber = Color(0xFFD97706)
val AccentRed = Color(0xFFDC2626)

val SurfaceLight = Color(0xFFF8FAFC)
val CardLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val BorderLight = Color(0xFFE2E8F0)

val SurfaceDark = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val BorderDark = Color(0xFF334155)

val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    background = SurfaceDark,
    surface = CardDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = BorderDark
)

val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    background = SurfaceLight,
    surface = CardLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = BorderLight
)
