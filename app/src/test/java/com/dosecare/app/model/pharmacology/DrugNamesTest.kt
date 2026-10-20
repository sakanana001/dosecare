package com.dosecare.app.model.pharmacology

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 药名的界面语言适配。
 *
 * 目录里每个药有三个名字: 英文 INN / 中文通用名 / 日文通用名 (INN 日本語)。
 * UI 层按当前界面语言挑主名, 搜索要能命中三种名字里的任意一种。
 *
 * 这些函数是纯 Kotlin (不依赖 Android), 所以在这里直接覆盖, 不必上仪器。
 */
class DrugNamesTest {

    private fun drug(
        id: String = "clozapine",
        en: String = "Clozapine",
        zh: String = "氯氮平",
        ja: String? = "クロザピン",
        brands: List<String> = listOf("Clozaril", "Leponex"),
        atc: String? = "N05AH02",
    ) = Drug(
        id = id,
        genericName = en,
        genericNameZh = zh,
        genericNameJa = ja,
        brandNames = brands,
        category = DrugCategory.ANTIPSYCHOTIC,
        atc = atc,
        pkModel = PkModel.OneCompartmentWithAbsorption(
            f = 0.6,
            kaPerHour = 1.2,
            kePerHour = 0.03,
            vdLiters = 20.0,
        ),
        forms = listOf(DrugForm(route = PkModel.Route.ORAL, f = 0.6)),
    )

    @Test
    fun chineseUiUsesTheChineseNameAsPrimary() {
        val d = drug()
        assertEquals("氯氮平", d.displayNameFor("zh"))
        assertEquals("氯氮平", d.displayNameFor("yue"))
        assertEquals("Clozapine", d.secondaryNameFor("zh"))
        assertEquals("Clozapine", d.secondaryNameFor("yue"))
    }

    @Test
    fun japaneseUiUsesTheJapaneseNameAsPrimary() {
        val d = drug()
        assertEquals("クロザピン", d.displayNameFor("ja"))
        assertEquals("Clozapine", d.secondaryNameFor("ja"))
    }

    @Test
    fun englishUiUsesTheEnglishNameAsPrimary() {
        val d = drug()
        assertEquals("Clozapine", d.displayNameFor("en"))
        assertEquals("氯氮平", d.secondaryNameFor("en"))
    }

    @Test
    fun japaneseFallsBackToEnglishWhenTheJapaneseNameIsMissing() {
        assertEquals("Clozapine", drug(ja = null).displayNameFor("ja"))
        // 空白也要回落, 不能把空串显示到界面上
        assertEquals("Clozapine", drug(ja = "  ").displayNameFor("ja"))
    }

    @Test
    fun unknownLanguageFallsBackToEnglish() {
        assertEquals("Clozapine", drug().displayNameFor("fr"))
    }

    @Test
    fun searchMatchesChineseJapaneseEnglishBrandAndAtc() {
        val d = drug()
        assertTrue("中文名应命中", d.matchesNameQuery("氯氮平"))
        assertTrue("日文名应命中", d.matchesNameQuery("クロザピン"))
        assertTrue("日文名片段应命中", d.matchesNameQuery("クロザ"))
        assertTrue("英文名应命中", d.matchesNameQuery("clozapine"))
        assertTrue("英文名大小写不敏感", d.matchesNameQuery("Clozapine"))
        assertTrue("品牌名应命中", d.matchesNameQuery("Clozaril"))
        assertTrue("ATC 码应命中", d.matchesNameQuery("N05AH02"))
        assertFalse("无关词不应命中", d.matchesNameQuery("ジプラシドン"))
    }

    @Test
    fun searchStillMatchesChinesePinyinInitials() {
        // 拼音首字母是中文界面的一贯用法, 收拢到共享函数后不能丢
        val zolpidem = drug(
            id = "zolpidem",
            en = "Zolpidem",
            zh = "唑吡坦",
            ja = "ゾルピデム",
            brands = listOf("Ambien"),
            atc = "N05CF02",
        )
        assertTrue("拼音首字母应命中", zolpidem.matchesNameQuery("zbt"))
        assertFalse("不相关拼音不应命中", zolpidem.matchesNameQuery("abc"))
    }

    @Test
    fun nonAsciiQueryIsNotTreatedAsPinyinInitials() {
        // "氯" 含非 ASCII, 不能被当成拼音首字母去匹配
        val d = drug()
        assertTrue("中文名仍应命中", d.matchesNameQuery("氯"))
        assertFalse("不相关中文不应命中", d.matchesNameQuery("锂"))
    }

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(drug().matchesNameQuery(""))
        assertTrue(drug().matchesNameQuery("   "))
    }
}
