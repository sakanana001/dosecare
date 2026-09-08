package com.dosecare.app.ui.locale

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * v0.9a 多语言支持
 *
 * 存用户选的语言 (SYSTEM / 简体中文 / English / 日本語), 写到 SharedPreferences
 * 启动时 MainActivity.load() 载入, 同时 AppCompatDelegate.setApplicationLocales
 * 让系统级 locale 切换生效 (Android 13+ 还会触发系统语言偏好 UI)
 *
 * 用法:
 *  - MainActivity.onCreate: `LocaleController.load(applicationContext)` 必须在 super 之前
 *  - UI 切换语言: `LocaleController.setLanguage(context, lang)` 自动落盘 + 应用
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
 * v0.9a bugfix: setLanguage 不光调用 setApplicationLocales, 还强制
 *  - Locale.setDefault(): 让 java.time / java.text 等 JVM API 跟着新 locale 走
 *    (AppCompatDelegate 只改 Configuration, 不改 JVM default)
 *  - currentActivity.recreate(): Activity 没自动 recreate 时手动触发
 *    (Compose 在某些 API 等级下缓存 stringResource 结果, 单纯 Configuration 切换不重渲染)
 */
object LocaleController {
    private const val PREFS_NAME = "dosecare_locale"
    private const val KEY_LANG = "language"

    private val _state = MutableStateFlow(LocaleState())
    val state: StateFlow<LocaleState> = _state.asStateFlow()

    /**
     * 当前显示的 Activity 引用 (用来手动 recreate)
     *  MainActivity.onCreate 时调 registerActivity, onDestroy 时调 unregisterActivity
     */
    private var currentActivity: Activity? = null

    fun registerActivity(activity: Activity) {
        currentActivity = activity
    }

    fun unregisterActivity(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    /**
     * 在 attachBaseContext 时调: 读 SharedPreferences 但不 apply, 不更新 state
     * 让 Activity 知道要用什么 locale 来创建 Resources
     */
    fun peekLanguage(context: Context): AppLanguage {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromName(prefs.getString(KEY_LANG, AppLanguage.SYSTEM.name))
    }

    /** 无参版本: 假定 state 已经被 load 初始化过, 直接返回当前 state 的 language
     *  注意: 必须在 LocaleController.load() 之后调, 否则返回 SYSTEM
     */
    fun peekLanguage(): AppLanguage = _state.value.language

    /**
     * 从 SharedPreferences 读 + 应用到 AppCompatDelegate
     * 必须在 super.onCreate 之前调 (onCreate 里 setContent 之前)
     */
    fun load(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = AppLanguage.fromName(prefs.getString(KEY_LANG, AppLanguage.SYSTEM.name))
        _state.value = LocaleState(lang)
        applyLocale(lang, context)
    }

    /**
     * 用户在设置里选了新语言, 落盘 + 应用
     * AppCompatDelegate.setApplicationLocales 在 API 33+ 会自动触发 Activity recreate
     * API 26-32 由 AppCompat 兼容层处理
     */
    fun setLanguage(context: Context, language: AppLanguage, activity: Activity? = currentActivity) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.name).apply()
        _state.update { it.copy(language = language) }
        applyLocale(language, context)
        // 不手动 recreate, AppCompatDelegate 1.7+ 自动处理
        // 备份: 如果检测到 recreate 没触发, 5 秒后强制 recreate
        activity?.let { act ->
            act.window.decorView.postDelayed({
                if (currentActivity === act) {
                    act.recreate()
                }
            }, 100)
        }
    }

    /**
     * 内部: 把 AppLanguage 应用到 AppCompatDelegate + Locale.setDefault
     *  - AppCompatDelegate: 让 Resources (stringResource) 用新 locale
     *  - Locale.setDefault: 让 JVM-level (java.time / java.text / DateFormat) 用新 locale
     *    包括 SYSTEM 模式 — 系统 Locale 跟 Android framework locale 不一定一致 (emulator 常有差异)
     */
    private fun applyLocale(language: AppLanguage, context: Context) {
        val list = if (language.isExplicit) {
            LocaleListCompat.forLanguageTags(language.tag)
        } else {
            LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(list)
        // 无论 SYSTEM 还是显式, 都同步 JVM Locale 到 Resources 用的 locale
        // SYSTEM 模式: 从 Configuration 取系统 locale
        // 显式: 用选中的 locale
        if (language.isExplicit) {
            Locale.setDefault(Locale.forLanguageTag(language.tag))
        } else {
            // 跟系统 Configuration 走, 避免 JVM 跟 Android framework 错位
            val systemLocale = context.resources.configuration.locales[0]
            if (systemLocale != null) {
                Locale.setDefault(systemLocale)
            }
        }
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
