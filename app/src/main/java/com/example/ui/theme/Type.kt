package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Headings & Big Stats: Integral CF (Async loading so initial window SurfaceSyncGroup never blocks on TTF asset I/O)
val IntegralCfFontFamily = FontFamily(
    Font(
        resId = R.font.integral_cf,
        weight = FontWeight.Normal,
        loadingStrategy = FontLoadingStrategy.Async
    ),
    Font(
        resId = R.font.integral_cf,
        weight = FontWeight.Bold,
        loadingStrategy = FontLoadingStrategy.Async
    ),
    Font(
        resId = R.font.integral_cf,
        weight = FontWeight.ExtraBold,
        loadingStrategy = FontLoadingStrategy.Async
    ),
    Font(
        resId = R.font.integral_cf,
        weight = FontWeight.Black,
        loadingStrategy = FontLoadingStrategy.Async
    )
)

// UI Fonts (For Trackers, descriptions, logs, small texts & Active Workouts): SF Pro Semibold (Async loading)
val SfProSemiboldFontFamily = FontFamily(
    Font(
        resId = R.font.sf_pro_semibold,
        weight = FontWeight.Normal,
        loadingStrategy = FontLoadingStrategy.Async
    ),
    Font(
        resId = R.font.sf_pro_semibold,
        weight = FontWeight.Medium,
        loadingStrategy = FontLoadingStrategy.Async
    ),
    Font(
        resId = R.font.sf_pro_semibold,
        weight = FontWeight.SemiBold,
        loadingStrategy = FontLoadingStrategy.Async
    ),
    Font(
        resId = R.font.sf_pro_semibold,
        weight = FontWeight.Bold,
        loadingStrategy = FontLoadingStrategy.Async
    )
)

val Typography = Typography(
    // Big Stats & Hero Headings -> Integral CF
    displayLarge = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.4.sp
    ),
    displayMedium = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 31.sp,
        lineHeight = 37.sp,
        letterSpacing = 0.3.sp
    ),
    displaySmall = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.2.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = IntegralCfFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    // UI Trackers, Descriptions, Logs, Small Texts & Active Workouts -> SF Pro Semibold
    titleMedium = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.5.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 17.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SfProSemiboldFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp
    )
)