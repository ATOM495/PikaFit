package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeIsDark = compositionLocalOf { true }

private val ModernDarkColorScheme = darkColorScheme(
    primary = PikaYellow,
    onPrimary = Color(0xFF1E1E24),
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = PikaYellow,
    secondary = PikaCheekRed,
    onSecondary = Color.White,
    secondaryContainer = DarkCard,
    onSecondaryContainer = PikaCheekRedLight,
    tertiary = PikaYellowDark,
    onTertiary = Color(0xFF1E1E24),
    error = PikaCheekRed,
    onError = Color.White,
    background = DarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = DarkBorder
)

private val ModernLightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706), // Rich high-contrast amber-gold on light surfaces
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = PikaCheekRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF9F1239),
    tertiary = Color(0xFF1E6076),
    onTertiary = Color.White,
    error = PikaCheekRed,
    onError = Color.White,
    background = LightBg,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun HunterSystemTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    AppThemeIsDark = darkTheme
    SideEffect {
        AppThemeIsDark = darkTheme
    }
    val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme

    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}