package com.dosecare.app.ui.interaction

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.DrugCatalogLoader
import com.dosecare.app.model.pharmacology.DrugCatalogService
import com.dosecare.app.ui.theme.HrtTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * 202 药药理目录的日文通用名 —— 界面层验证。
 *
 * 目录里每个药现在有三个名字: 英文 INN / 中文通用名 / 日文通用名。
 * 日语界面下主名必须是日文通用名。之前目录里没有日文名时, 日语界面显示的是
 * 中文名, 搜索也只认中英文 —— 这里把这条钉住, 防止以后有人把字段删了/填错了。
 *
 * 为什么只测"选中态 chip"而不测弹窗里的列表:
 * Compose 的 `LocalConfiguration` 由**每个 ComposeView 各自提供**, Dialog 有自己的
 * View, 会用自己的 context 配置覆盖掉外面注入的值 —— 弹窗里的语言由设备 per-app
 * locale 决定, 测试里注入改不动。
 * chip 在外层 composition 里, 所以这里用注入日语 Configuration 的方式验证显示逻辑;
 * 搜索规则由 model 层的 `Drug.matchesNameQuery` 负责, 由单元测试覆盖。
 */
@RunWith(AndroidJUnit4::class)
class JapaneseDrugNamePickerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun loadCatalog(): DrugCatalogService {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        val raw = assets.open("pharmacology/catalog.json").use { it.readBytes().decodeToString() }
        return DrugCatalogService(DrugCatalogLoader.fromJson(raw))
    }

    /** 把 LocalConfiguration 的语言强制成日语, 让 `currentUiLanguage()` 返回 "ja"。 */
    private fun setJapaneseContent(options: List<Drug>, selected: Drug?) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val jaConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.JAPAN)
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides jaConfig) {
                HrtTrackerTheme(dynamicColor = false) {
                    SearchableDrugPicker(
                        label = "薬を追加",
                        selected = selected,
                        options = options,
                        onSelect = { },
                        placeholder = "OPEN_PICKER",
                    )
                }
            }
        }
    }

    @Test
    fun selectedDrugShowsItsJapaneseGenericName() {
        val catalog = loadCatalog()
        val clozapine = catalog.all().first { it.id == "clozapine" }
        setJapaneseContent(catalog.all(), selected = clozapine)

        // 主名是日文通用名, 副名是英文 INN
        composeRule.onNodeWithText("クロザピン", substring = true).assertIsDisplayed()
    }

    @Test
    fun selectedDrugDoesNotShowTheChineseName() {
        val catalog = loadCatalog()
        val clozapine = catalog.all().first { it.id == "clozapine" }
        setJapaneseContent(catalog.all(), selected = clozapine)

        val chinese = composeRule.onAllNodesWithText("氯氮平", substring = true)
        assertTrue(
            "日语界面不应把中文名当主名",
            chinese.fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun chineseUiStillShowsTheChineseName() {
        // 反向确认: 改回简体中文后, 同一个药又该显示中文名 (别把中文界面改坏了)
        val catalog = loadCatalog()
        val clozapine = catalog.all().first { it.id == "clozapine" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val zhConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.SIMPLIFIED_CHINESE)
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides zhConfig) {
                HrtTrackerTheme(dynamicColor = false) {
                    SearchableDrugPicker(
                        label = "添加药",
                        selected = clozapine,
                        options = catalog.all(),
                        onSelect = { },
                        placeholder = "OPEN_PICKER",
                    )
                }
            }
        }
        composeRule.onNodeWithText("氯氮平", substring = true).assertIsDisplayed()
    }

    @Test
    fun openingThePickerShowsTheSearchField() {
        val catalog = loadCatalog()
        setJapaneseContent(catalog.all(), selected = null)

        // 外层搜索框 (点击后弹窗)
        composeRule.onNodeWithText("OPEN_PICKER").assertIsDisplayed()
    }

    @Test
    fun catalogCarriesAJapaneseNameForEveryDrug() {
        val catalog = loadCatalog()
        assertEquals(202, catalog.all().size)
        catalog.all().forEach { drug ->
            assertTrue("${drug.id} missing genericNameJa", !drug.genericNameJa.isNullOrBlank())
        }
    }
}

