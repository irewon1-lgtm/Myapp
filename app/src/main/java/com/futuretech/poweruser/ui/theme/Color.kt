package com.futuretech.poweruser.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Items 21-25 visual policy: charcoal/off-white foundation + one restrained blue accent.
// Success/failure must also be communicated with ✓ / ! + text, not color alone.
val UiBackground = Color(0xFF0B0C0F)
val UiSurface = Color(0xFF111318)
val UiSurfaceRaised = Color(0xFF171A20)
val UiSurfaceSoft = Color(0xFF1D2026)
val UiAccent = Color(0xFF8AB4F8)
val UiAccentDeep = Color(0xFF1E3553)
val UiText = Color(0xFFF3F1EA)
val UiTextMuted = Color(0xFFADB2BC)
val UiBorder = Color(0xFF30343C)
val UiError = Color(0xFFFF8A8A)

// Legacy names stay as aliases so older screens keep compiling while the visible palette is unified.
val AiBackground = UiBackground
val AiSurface = UiSurface
val AiSurfaceRaised = UiSurfaceRaised
val AiSurfaceSoft = UiSurfaceSoft
val AiCyan = UiAccent
val AiViolet = UiAccent
val AiGreen = UiTextMuted
val AiAmber = UiTextMuted
val AiRed = UiError
val AiText = UiText
val AiTextMuted = UiTextMuted
val AiBorder = UiBorder

val DarkColorScheme = darkColorScheme(
    primary = UiAccent,
    onPrimary = Color(0xFF0A1728),
    primaryContainer = UiAccentDeep,
    onPrimaryContainer = UiText,
    secondary = UiTextMuted,
    onSecondary = UiBackground,
    secondaryContainer = UiSurfaceSoft,
    onSecondaryContainer = UiText,
    tertiary = UiTextMuted,
    onTertiary = UiBackground,
    tertiaryContainer = UiSurfaceSoft,
    onTertiaryContainer = UiText,
    error = UiError,
    errorContainer = Color(0xFF3A2024),
    onErrorContainer = UiText,
    background = UiBackground,
    onBackground = UiText,
    surface = UiSurface,
    onSurface = UiText,
    surfaceVariant = UiSurfaceSoft,
    onSurfaceVariant = UiTextMuted,
    outline = UiBorder,
    outlineVariant = Color(0xFF242830)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF315F96),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FA),
    onPrimaryContainer = Color(0xFF172535),
    secondary = Color(0xFF636872),
    tertiary = Color(0xFF636872),
    error = Color(0xFFB3261E),
    background = Color(0xFFF5F3EC),
    surface = Color(0xFFFCFAF4),
    onBackground = Color(0xFF202226),
    onSurface = Color(0xFF202226),
    surfaceVariant = Color(0xFFEAE7DF),
    onSurfaceVariant = Color(0xFF565A62),
    outline = Color(0xFFC9C5BC)
)
