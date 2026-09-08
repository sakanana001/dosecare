package com.dosecare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * DoseCare Typography
 *
 * 字号阶梯偏大（精神疾病急性期友好）：
 * - Display: 32sp
 * - Headline: 24sp
 * - Title: 20sp
 * - Body: 16sp
 * - Label: 14sp
 * - Caption: 12sp
 */
val DoseCareTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 13.sp),
    labelSmall = TextStyle(fontSize = 12.sp)
)
