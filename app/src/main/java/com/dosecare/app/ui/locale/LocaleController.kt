package com.dosecare.app.ui.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * v0.9a 多语言支持
 *
 * 存用户选的语言 (SYSTEM / 简体中文 / English / 日本語), 写到 SharedPreferences
 * 启动时 MainActivity.load() 载入, 同时 AppCompatDelegate.setApplicationLocales
 * 让系统级 locale 切换生效 (Android 13+ 还会触发系统语言偏好 UI)
 *
 * 用法:
 *  - MainActivity.onCreate: `LocaleController.load(applicationContext)` 必须在 super 之前
 *  - UI 切换语言: `LocaleController.setLanguage(context, AppLanguage.EN)` 自动落盘 + 应用
 *  - 订阅 state: `LocaleController.state.collectAsState()` (大多场景不需要, locale 切换后会重启 Activity)
 */
enum class AppLanguage(val tag: String) {
    /** 跟随系统语言 */
    SYSTEM(""),

    /** 简体中文 */
    ZH_CN("zh-CN"),

    /** 英语 (美式) */
    EN("en"),

    /** 日语 */
    JA("ja");

    /** 是否为"显式设置" (非跟随系统) */
    val isExplicit: Boolean get() = this != SYSTEM

    companion object {
        /** 从 SharedPreferences 字符串反序列化, 找不到时回退到 SYSTEM */
        fun fromName(name: String?): AppLanguage =
            runCatching { valueOf(name ?: "") }.getOrDefault(SYSTEM)
    }
}

data class LocaleState(
    val language: AppLanguage = AppLanguage.SYSTEM,
)

/**
 * 全局 Locale 状态 (singleton)
 *
 * 模式跟 ThemeController 一致: 启动 load, UI 调 setLanguage 触发落盘 + 通知
 *
 * 注意: setLanguage 调 AppCompatDelegate.setApplicationLocales 后,
 * Android 13+ 触发自动重启, 旧版本通过 Activity.recreate() (AppCompat 1.7+ 内部做)
 * 不需要手动 recreate
 */
object LocaleController {
    private const val PREFS_NAME = "dosecare_locale"
    private const val KEY_LANG = "language"

    private val _state = MutableStateFlow(LocaleState())
    val state: StateFlow<LocaleState> = _state.asStateFlow()

    /**
     * 从 SharedPreferences 读 + 应用到 AppCompatDelegate
     * 必须在 super.onCreate 之前调 (onCreate 里 setContent 之前)
     */
    fun load(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = AppLanguage.fromName(prefs.getString(KEY_LANG, AppLanguage.SYSTEM.name))
        _state.value = LocaleState(lang)
        // 同步应用 (不触发 recompose, AppCompat 内部会自动重启 Activity)
        applyToDelegate(lang)
    }

    /**
     * 用户在设置里选了新语言, 落盘 + 应用
     */
    fun setLanguage(context: Context, language: AppLanguage) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.name).apply()
        _state.update { it.copy(language = language) }
        applyToDelegate(language)
    }

    /**
     * 把 AppLanguage 转成 LocaleListCompat 并 apply
     * SYSTEM → 空 list (跟随系统); 显式 → 单 locale list
     */
    private fun applyToDelegate(language: AppLanguage) {
        val list = if (language.isExplicit) {
            LocaleListCompat.forLanguageTags(language.tag)
        } else {
            LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(list)
    }

    /**
     * 当前显式设置的语言的 LocaleListCompat (供测试 / 调试)
     */
    fun currentLocaleList(): LocaleListCompat {
        val lang = _state.value.language
        return if (lang.isExplicit) {
            LocaleListCompat.forLanguageTags(lang.tag)
        } else {
            LocaleListCompat.getEmptyLocaleList()
        }
    }
}
