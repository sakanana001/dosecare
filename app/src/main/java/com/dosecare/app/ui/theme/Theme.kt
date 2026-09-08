package com.dosecare.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * 共享的中性色 (不被 accent 影响的部分)
 * — primary/secondary/tertiary/onPrimary/... 由 accent 覆盖
 * — background/surface/error 永远保持统一
 */
private object NeutralLight {
    val background = Color(0xFFFAFBFC)
    val onBackground = Color(0xFF1F2326)
    val surface = Color.White
    val onSurface = Color(0xFF1F2326)
    val surfaceVariant = Color(0xFFF1F3F5)
    val onSurfaceVariant = Color(0xFF525861)
    val outline = Color(0xFFD8DCE0)
    val outlineVariant = Color(0xFFE7EAED)
    val error = Color(0xFFD5524E)
    val onError = Color.White
    val errorContainer = Color(0xFFFFE5E3)
    val onErrorContainer = Color(0xFF4A0E0C)
    // secondary/tertiary 备用 (暂时跟 primary 一致, accent 决定)
    val secondary = Color(0xFF8E6FBE)
    val onSecondary = Color.White
    val secondaryContainer = Color(0xFFEEE5F8)
    val onSecondaryContainer = Color(0xFF2A1A45)
    val tertiary = Color(0xFF6BBEA8)
    val onTertiary = Color.White
    val tertiaryContainer = Color(0xFFD7EFE7)
    val onTertiaryContainer = Color(0xFF1A4538)
}

private object NeutralDark {
    val background = Color(0xFF14171A)
    val onBackground = Color(0xFFE8EBEE)
    val surface = Color(0xFF1B1E22)
    val onSurface = Color(0xFFE8EBEE)
    val surfaceVariant = Color(0xFF25292E)
    val onSurfaceVariant = Color(0xFFB6BCC2)
    val outline = Color(0xFF353A40)
    val outlineVariant = Color(0xFF2A2E33)
    val error = Color(0xFFFFB4AF)
    val onError = Color(0xFF4A0E0C)
    val errorContainer = Color(0xFF7A201D)
    val onErrorContainer = Color(0xFFFFE5E3)
    val secondary = Color(0xFFBFA3DC)
    val onSecondary = Color(0xFF2A1A45)
    val secondaryContainer = Color(0xFF5A4275)
    val onSecondaryContainer = Color(0xFFEEE5F8)
    val tertiary = Color(0xFF90D2BF)
    val onTertiary = Color(0xFF1A4538)
    val tertiaryContainer = Color(0xFF2E6253)
    val onTertiaryContainer = Color(0xFFD7EFE7)
}

/**
 * 圆角方案 (v0.8c 加大, 更圆润)
 * - extraSmall  12dp (chip / 标签)
 * - small       14dp (TextField 边角)
 * - medium      18dp (button / dialog item)
 * - large       24dp (card)
 * - extraLarge  32dp (bottom sheet 顶部圆角 / 大 dialog)
 *
 * 比 Material 3 默认 (4/8/16/20) 更柔和, 适合医疗类 App 给人安心的感觉
 */
private val DoseCareShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * DoseCare 主题
 *
 * @param themeState  主题状态 (darkMode + accentIndex)
 * @param dynamicColor 是否用 Android 12+ 动态取色 (Material You) — 默认关, 走自定义 5 色
 */
@Composable
fun DoseCareTheme(
    themeState: ThemeState = ThemeState(),
    isSystemDark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 决定是否暗色: SYSTEM 跟系统, 否则用用户选的
    val darkTheme = when (themeState.darkMode) {
        DarkModePref.SYSTEM -> isSystemDark
        DarkModePref.LIGHT -> false
        DarkModePref.DARK -> true
    }

    val accent = themeState.effectiveAccent

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            // 暗色: 中性背景 + 用户选的主题色
            darkColorScheme(
                primary = accent.darkPrimary,
                onPrimary = accent.darkOnPrimary,
                primaryContainer = accent.darkPrimaryContainer,
                onPrimaryContainer = accent.darkOnPrimaryContainer,
                secondary = NeutralDark.secondary,
                onSecondary = NeutralDark.onSecondary,
                secondaryContainer = NeutralDark.secondaryContainer,
                onSecondaryContainer = NeutralDark.onSecondaryContainer,
                tertiary = NeutralDark.tertiary,
                onTertiary = NeutralDark.onTertiary,
                tertiaryContainer = NeutralDark.tertiaryContainer,
                onTertiaryContainer = NeutralDark.onTertiaryContainer,
                error = NeutralDark.error,
                onError = NeutralDark.onError,
                errorContainer = NeutralDark.errorContainer,
                onErrorContainer = NeutralDark.onErrorContainer,
                background = NeutralDark.background,
                onBackground = NeutralDark.onBackground,
                surface = NeutralDark.surface,
                onSurface = NeutralDark.onSurface,
                surfaceVariant = NeutralDark.surfaceVariant,
                onSurfaceVariant = NeutralDark.onSurfaceVariant,
                outline = NeutralDark.outline,
                outlineVariant = NeutralDark.outlineVariant,
            )
        }
        else -> {
            // 亮色: 中性背景 + 用户选的主题色
            lightColorScheme(
                primary = accent.lightPrimary,
                onPrimary = accent.lightOnPrimary,
                primaryContainer = accent.lightPrimaryContainer,
                onPrimaryContainer = accent.lightOnPrimaryContainer,
                secondary = NeutralLight.secondary,
                onSecondary = NeutralLight.onSecondary,
                secondaryContainer = NeutralLight.secondaryContainer,
                onSecondaryContainer = NeutralLight.onSecondaryContainer,
                tertiary = NeutralLight.tertiary,
                onTertiary = NeutralLight.onTertiary,
                tertiaryContainer = NeutralLight.tertiaryContainer,
                onTertiaryContainer = NeutralLight.onTertiaryContainer,
                error = NeutralLight.error,
                onError = NeutralLight.onError,
                errorContainer = NeutralLight.errorContainer,
                onErrorContainer = NeutralLight.onErrorContainer,
                background = NeutralLight.background,
                onBackground = NeutralLight.onBackground,
                surface = NeutralLight.surface,
                onSurface = NeutralLight.onSurface,
                surfaceVariant = NeutralLight.surfaceVariant,
                onSurfaceVariant = NeutralLight.onSurfaceVariant,
                outline = NeutralLight.outline,
                outlineVariant = NeutralLight.outlineVariant,
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 暗色时状态栏用浅色图标 (黑底白字), 亮色反之
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DoseCareTypography,
        shapes = DoseCareShapes,
        content = content
    )
}
