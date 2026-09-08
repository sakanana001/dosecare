package com.dosecare.app

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
 *   AppCompatDelegate.setApplicationLocales 内部会自动重启 Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var catalog: DrugCatalogService

    override fun onCreate(savedInstanceState: Bundle?) {
        // v0.9a: locale 必须在 super.onCreate 之前 apply
        LocaleController.load(applicationContext)
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
}
