package com.mednavigator.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Lexend design system font — SansSerif (Roboto) as closest system fallback
private val LexendFamily = FontFamily.SansSerif

val Typography = Typography(
    displayLarge   = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.SemiBold,  fontSize = 32.sp, lineHeight = 41.6.sp),
    headlineLarge  = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.SemiBold,  fontSize = 32.sp, lineHeight = 41.6.sp),
    headlineMedium = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Medium,    fontSize = 24.sp, lineHeight = 33.6.sp),
    headlineSmall  = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Medium,    fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge     = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Medium,    fontSize = 24.sp, lineHeight = 33.6.sp),
    titleMedium    = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Medium,    fontSize = 18.sp, lineHeight = 28.8.sp),
    titleSmall     = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Medium,    fontSize = 16.sp, lineHeight = 19.2.sp, letterSpacing = 0.02.sp),
    bodyLarge      = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Normal,    fontSize = 20.sp, lineHeight = 32.sp),
    bodyMedium     = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Normal,    fontSize = 18.sp, lineHeight = 28.8.sp),
    bodySmall      = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 25.6.sp),
    labelLarge     = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 19.2.sp,  letterSpacing = 0.32.sp),
    labelMedium    = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 16.sp,    letterSpacing = 0.28.sp),
    labelSmall     = TextStyle(fontFamily = LexendFamily, fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 14.4.sp,  letterSpacing = 0.24.sp),
)