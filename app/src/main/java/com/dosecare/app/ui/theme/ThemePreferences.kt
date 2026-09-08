package com.dosecare.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 主题偏好持久化 (v0.8c 新增, v0.8d 增强自定义色)
 *
 * 存:
 *  - 暗色模式 (SYSTEM/LIGHT/DARK)
 *  - 主题色索引 (0-4, 5 预设)
 *  - 自定义主题色 ARGB Int (可选, v0.8d)
 *
 * 用 SharedPreferences 简单存, 不用 DataStore (避免引入新依赖)
 *
 * 暴露 StateFlow 给 Compose 订阅, 任何写入会触发 recomposition
 */
enum class DarkModePref(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("亮色"),
    DARK("暗色");
}

/** 预设主题色 (5 选 1) — 跟 LightColors/DarkColors 配对 */
data class AccentColor(
    val name: String,
    val lightPrimary: Color,
    val lightOnPrimary: Color = Color.White,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color = Color(0xFF0A2A47),
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
)

object AccentColors {
    val SkyBlue = AccentColor(
        name = "浅蓝",
        lightPrimary = Color(0xFF5B8DBE),
        lightPrimaryContainer = Color(0xFFE3EEF7),
        lightOnPrimaryContainer = Color(0xFF0A2A47),
        darkPrimary = Color(0xFF8FB7DB),
        darkPrimaryContainer = Color(0xFF2A4566),
        darkOnPrimaryContainer = Color(0xFFD6E5F2),
    )
    val WarmOrange = AccentColor(
        name = "暖橙",
        lightPrimary = Color(0xFFE08A4F),
        lightPrimaryContainer = Color(0xFFFCE4D2),
        lightOnPrimaryContainer = Color(0xFF4A2510),
        darkPrimary = Color(0xFFE8A777),
        darkPrimaryContainer = Color(0xFF6E4527),
        darkOnPrimaryContainer = Color(0xFFFCE4D2),
    )
    val ForestGreen = AccentColor(
        name = "墨绿",
        lightPrimary = Color(0xFF5C8A6B),
        lightPrimaryContainer = Color(0xFFD7EFE0),
        lightOnPrimaryContainer = Color(0xFF1A3528),
        darkPrimary = Color(0xFF8FB7A0),
        darkPrimaryContainer = Color(0xFF355441),
        darkOnPrimaryContainer = Color(0xFFD7EFE0),
    )
    val RoseRed = AccentColor(
        name = "玫瑰红",
        lightPrimary = Color(0xFFC4646E),
        lightPrimaryContainer = Color(0xFFFADADD),
        lightOnPrimaryContainer = Color(0xFF4A1A20),
        darkPrimary = Color(0xFFD88A92),
        darkPrimaryContainer = Color(0xFF6E2E36),
        darkOnPrimaryContainer = Color(0xFFFADADD),
    )
    val DeepPurple = AccentColor(
        name = "深紫",
        lightPrimary = Color(0xFF7C5DAB),
        lightPrimaryContainer = Color(0xFFEAE0F8),
        lightOnPrimaryContainer = Color(0xFF26154A),
        darkPrimary = Color(0xFFA78FCD),
        darkPrimaryContainer = Color(0xFF463275),
        darkOnPrimaryContainer = Color(0xFFEAE0F8),
    )

    val all: List<AccentColor> = listOf(SkyBlue, WarmOrange, ForestGreen, RoseRed, DeepPurple)

    /**
     * 从用户选的任意 Color 派生完整的 AccentColor (v0.8d 自定义主题色)
     *
     * 算法: 把 RGB 转 HSV, 用 hue 保持色相, 调整 saturation/value 派生 container/onColor
     *  - lightPrimary = 用户色
     *  - lightPrimaryContainer = 同 hue 极低饱和极高亮度 (背景浅)
     *  - lightOnPrimaryContainer = 同 hue 高饱和低亮度 (深色文字)
     *  - darkPrimary = 同 hue 中饱和高亮度 (亮色按钮)
     *  - darkPrimaryContainer = 同 hue 中饱和低亮度 (深底)
     *  - darkOnPrimaryContainer = 同 hue 低饱和高亮度 (浅色文字)
     */
    fun deriveFromColor(userColor: Color): AccentColor {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (userColor.red * 255).toInt(),
            (userColor.green * 255).toInt(),
            (userColor.blue * 255).toInt(),
            hsv
        )
        val h = hsv[0]
        val s = hsv[1]
        val v = hsv[2]

        // 灰度主色 (黑/白/灰) 强制给一点饱和度, 避免 container 看着像纯灰
        val forcedS = s.coerceAtLeast(0.4f)

        return AccentColor(
            name = "自定义",
            lightPrimary = userColor,
            lightOnPrimary = Color.White,
            // 浅色模式背景: 同 hue, 极低饱和, 极高亮度
            lightPrimaryContainer = Color.hsv(h, (forcedS * 0.15f).coerceIn(0f, 1f), 0.95f),
            // 浅色模式文字: 同 hue, 高饱和, 低亮度
            lightOnPrimaryContainer = Color.hsv(h, forcedS, 0.22f),
            // 暗色模式按钮: 同 hue, 中饱和, 高亮度 (更亮)
            darkPrimary = Color.hsv(h, forcedS * 0.7f, 0.82f),
            darkOnPrimary = Color.hsv(h, forcedS, 0.15f),
            // 暗色模式背景: 同 hue, 中饱和, 低亮度
            darkPrimaryContainer = Color.hsv(h, forcedS, 0.28f),
            // 暗色模式文字: 同 hue, 低饱和, 高亮度
            darkOnPrimaryContainer = Color.hsv(h, forcedS * 0.25f, 0.94f),
        )
    }
}

data class ThemeState(
    val darkMode: DarkModePref = DarkModePref.SYSTEM,
    val accentIndex: Int = 0,
    val customColor: Color? = null,
) {
    /**
     * 当前实际生效的 AccentColor
     * - customColor != null → 从用户色派生
     * - 否则用 5 预设中的 accentIndex
     */
    val effectiveAccent: AccentColor
        get() = customColor?.let { AccentColors.deriveFromColor(it) }
            ?: AccentColors.all.getOrElse(accentIndex) { AccentColors.SkyBlue }
}

/**
 * 全局 Theme 状态 (singleton object, 类似 ViewModel 模式)
 *
 * MainActivity 启动时 load() 把 SharedPreferences 读到 StateFlow
 * 用户在 UI 改时调 setDarkMode / setAccent / setCustomColor, 同时落盘
 */
object ThemeController {
    private const val PREFS_NAME = "dosecare_theme"
    private const val KEY_DARK = "dark_mode"
    private const val KEY_ACCENT = "accent_index"
    private const val KEY_CUSTOM = "custom_color"

    private val _state = MutableStateFlow(ThemeState())
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    fun load(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dark = runCatching { DarkModePref.valueOf(prefs.getString(KEY_DARK, DarkModePref.SYSTEM.name)!!) }
            .getOrDefault(DarkModePref.SYSTEM)
        val accent = prefs.getInt(KEY_ACCENT, 0).coerceIn(0, AccentColors.all.size - 1)
        val customArgb = if (prefs.contains(KEY_CUSTOM)) prefs.getInt(KEY_CUSTOM, 0) else null
        val customColor = customArgb?.let { Color(it) }
        _state.value = ThemeState(dark, accent, customColor)
    }

    fun setDarkMode(context: Context, mode: DarkModePref) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DARK, mode.name).apply()
        _state.update { it.copy(darkMode = mode) }
    }

    fun setAccent(context: Context, index: Int) {
        val safeIndex = index.coerceIn(0, AccentColors.all.size - 1)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 选预设时清掉 customColor (互斥)
        prefs.edit()
            .putInt(KEY_ACCENT, safeIndex)
            .remove(KEY_CUSTOM)
            .apply()
        _state.update { it.copy(accentIndex = safeIndex, customColor = null) }
    }

    /**
     * 设置自定义主题色 (v0.8d 新增)
     * @param color 用户从 ColorPicker 选的颜色 (ARGB)
     */
    fun setCustomColor(context: Context, color: Color) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_CUSTOM, color.toArgb())
            .apply()
        _state.update { it.copy(customColor = color) }
    }

    /**
     * 清除自定义色, 回到当前 accentIndex 预设
     */
    fun clearCustomColor(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM).apply()
        _state.update { it.copy(customColor = null) }
    }
}
