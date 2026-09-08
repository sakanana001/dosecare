package com.dosecare.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.ui.AppRoot
import com.dosecare.app.ui.locale.LocaleController
import com.dosecare.app.ui.theme.ThemeController
import com.dosecare.app.ui.theme.DoseCareTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

/**
 * 唯一 Activity（v0.7 Hilt + Room 时代）
 *
 * 入口: 4 个底栏 tab (处方 / 目录 / 对比 / 相互作用)
 * 加上 DrugDetail + Settings 模态
 *
 * v0.7 升级:
 * - @AndroidEntryPoint 让 Hilt 注入依赖
 * - DrugCatalogService 由 Hilt 提供 (从 Room DB 加载)
 * - 删除原来从 Application.drugCatalog 单例取的代码
 *
 * v0.8c 升级:
 * - 启动时 ThemeController.load() 从 SharedPreferences 读主题设置
 * - 包到 DoseCareTheme 里用 state.darkMode + state.accentIndex 生成 ColorScheme
 *
 * v0.9a 升级:
 * - 启动时 LocaleController.load() 应用用户选的语言 (SYSTEM / zh-CN / en / ja)
 * - attachBaseContext 重写 Resources Configuration, 让 Compose stringResource 立即用新 locale
 *   (解决 AppCompatDelegate.setApplicationLocales + Activity.recreate 在某些 API 不触发 Resources 刷新)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var catalog: DrugCatalogService

    /**
     * 在 super.attachBaseContext 之前, override Resources Configuration
     * 强制使用 LocaleController 选的语言
     * 这是 Compose stringResource 立即生效的关键
     *
     * 同时同步 JVM Locale (java.time / java.text 用), 解决:
     *  - 显式语言: 跟手选项走
     *  - SYSTEM: 跟 Android framework Configuration 的 locales 走
     *    (避免 emulator 上 framework=en 但 JVM=zh 的错位)
     */
    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleController.peekLanguage(newBase)
        val targetLocale: Locale? = if (lang.isExplicit) {
            Locale.forLanguageTag(lang.tag)
        } else {
            // SYSTEM 模式: 跟 Android framework locale 走
            newBase.resources.configuration.locales[0]
        }
        if (targetLocale != null) {
            Locale.setDefault(targetLocale)
        }
        val ctx = if (lang.isExplicit) {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(targetLocale!!)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // v0.9a: locale 必须在 super.onCreate 之前 apply
        LocaleController.load(applicationContext)
        LocaleController.registerActivity(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // v0.8c: 启动时载入主题设置
        ThemeController.load(applicationContext)
        setContent {
            val themeState by ThemeController.state.collectAsState()
            DoseCareTheme(themeState = themeState) {
                AppRoot(catalog = catalog)
            }
        }
    }

    override fun onDestroy() {
        LocaleController.unregisterActivity(this)
        super.onDestroy()
    }
}
