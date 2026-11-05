package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Global reactive theme state updated by HunterSystemTheme so all semantic colors adapt immediately
var AppThemeIsDark by mutableStateOf(true)

// Pikachu Core Palette
val PikaYellow = Color(0xFFFFDE00)
val PikaYellowLight = Color(0xFFFFF176)
val PikaYellowDark = Color(0xFFF59E0B)
val PikaCheekRed = Color(0xFFEF233C)
val PikaCheekRedLight = Color(0xFFFF6B6B)
val PikaBrown = Color(0xFF8B4513)
val PikaEarBlack = Color(0xFF1E1E24)

// Reference UI Palette (Crimson Donut Card, Teal Headers, Indigo Water Bar)
val RefCrimsonRed = Color(0xFFEF233C)
val RefCrimsonDark = Color(0xFFD90429)
val RefTealHeadingDark = Color(0xFF38BDF8)
val RefTealHeadingLight = Color(0xFF1E6076)
val RefIndigoWater = Color(0xFF4F46E5)

// Dark Theme Canvas
val DarkBg = Color(0xFF12141A)
val DarkSurface = Color(0xFF1A1D26)
val DarkCard = Color(0xFF222734)
val DarkCardElevated = Color(0xFF2B3244)
val DarkBorder = Color(0xFF3B4358)

// Light Theme Canvas
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFFEF9C3)
val LightBorder = Color(0xFFCBD5E1)

// Dynamic Theme-Aware Semantic Colors (automatically switch between Dark & Light mode!)
val HunterCyan: Color
    get() = if (AppThemeIsDark) PikaYellow else Color(0xFFD97706)

val HunterCyanDim: Color
    get() = if (AppThemeIsDark) PikaYellowDark else Color(0xFFB45309)

val HunterEmerald: Color
    get() = if (AppThemeIsDark) Color(0xFF10B981) else Color(0xFF059669)

val HunterGold: Color
    get() = if (AppThemeIsDark) PikaYellowDark else Color(0xFFB45309)

val HunterViolet: Color
    get() = PikaCheekRed

val HunterCrimson: Color
    get() = PikaCheekRed

val ObsidianBg: Color
    get() = if (AppThemeIsDark) DarkBg.copy(alpha = 0.78f) else LightBg.copy(alpha = 0.80f)

val ObsidianSurface: Color
    get() = if (AppThemeIsDark) DarkSurface.copy(alpha = 0.82f) else LightSurface.copy(alpha = 0.84f)

val ObsidianCard: Color
    get() = if (AppThemeIsDark) DarkCard.copy(alpha = 0.80f) else LightCard.copy(alpha = 0.84f)

val ObsidianCardElevated: Color
    get() = if (AppThemeIsDark) DarkCardElevated.copy(alpha = 0.84f) else LightCardElevated.copy(alpha = 0.88f)

val BorderSubtle: Color
    get() = if (AppThemeIsDark) DarkBorder else LightBorder

val TextPrimary: Color
    get() = if (AppThemeIsDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val TextSecondary: Color
    get() = if (AppThemeIsDark) Color(0xFFCBD5E1) else Color(0xFF334155)

val TextMuted: Color
    get() = if (AppThemeIsDark) Color(0xFF94A3B8) else Color(0xFF475569)

val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFFCBD5E1)
val DarkTextMuted = Color(0xFF94A3B8)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF334155)
val LightTextMuted = Color(0xFF475569)

val AccentTeal: Color
    get() = if (AppThemeIsDark) PikaYellow else Color(0xFFD97706)
val AccentTealDark: Color
    get() = if (AppThemeIsDark) PikaYellowDark else Color(0xFFB45309)
val AccentPurple = PikaCheekRed
val AccentPurpleLight = PikaCheekRedLight
val AccentCyanGlow = PikaYellowLight
val WarningAmber: Color
    get() = if (AppThemeIsDark) PikaYellowDark else Color(0xFFB45309)
val DangerCrimson = PikaCheekRed
val DarkCardBg = DarkCard
val LightCardBg = LightCard