package com.dosecare.app.model.pharmacology

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v0.9g merge: the pharmacology catalog asset must actually parse.
 *
 * The catalog is lazy — Hilt only builds `DrugCatalogService` when something injects it, and
 * no UI consumes it until phase 2. Without this test a broken asset (wrong path, a schema
 * field renamed upstream, a kotlinx-serialization opt-in mismatch) would ship silently and
 * only surface as "interaction detection finds nothing" much later.
 *
 * These assertions pin the parts the rule engine depends on: the drug id space, and the
 * CYP450 profile that `RuleEngine.checkCypInteractions` reads. Ids are load-bearing — the
 * engine's own id sets (`ANTIPSYCHOTIC_IDS`, the QTc table, …) are written against them.
 */
@RunWith(AndroidJUnit4::class)
class PharmacologyCatalogAssetTest {

    // targetContext, not instrumentation.context: the catalog asset ships inside the app
    // under test, while the test APK carries only the test classes. Reading from the test
    // APK's own asset manager throws FileNotFoundException.
    private val assets
        get() = InstrumentationRegistry.getInstrumentation().targetContext.assets

    private fun loadCatalog(): DrugCatalogService {
        val raw = assets.open("pharmacology/catalog.json").use { it.readBytes().decodeToString() }
        return DrugCatalogService(DrugCatalogLoader.fromJson(raw))
    }

    @Test
    fun assetParsesAndHoldsTheFullCatalog() {
        val catalog = loadCatalog()
        assertEquals(
            "catalog should carry all 202 drugs carried over from DoseCare",
            202,
            catalog.all().size,
        )
    }

    @Test
    fun everyDrugHasAStableIdAndBothNames() {
        val catalog = loadCatalog()
        val ids = catalog.all().map { it.id }
        assertEquals("drug ids must be unique", ids.size, ids.toSet().size)
        catalog.all().forEach { drug ->
            assertTrue("${drug.id} has a blank id", drug.id.isNotBlank())
            assertTrue("${drug.id} missing genericName", drug.genericName.isNotBlank())
            assertTrue("${drug.id} missing genericNameZh", drug.genericNameZh.isNotBlank())
            assertTrue("${drug.id} missing genericNameJa", !drug.genericNameJa.isNullOrBlank())
        }
    }

    /**
     * 日文通用名必须**已经被本地化**—— 之前目录里没有这个字段时, 日语界面的药名
     * 列表显示的是中文名, 搜索也搜不到日文名。
     *
     * 判定标准: 日文名既不能是英文 INN 的复制, 也不能是中文通用名的复制。
     * (不能简单要求"必须含假名": 叶酸 / 硫酸鉄(II) / 乾燥甲状腺 这类日本医药品
     * 一般名本来就是纯汉字写法。)
     */
    @Test
    fun everyJapaneseNameIsLocalizedRatherThanCopied() {
        loadCatalog().all().forEach { drug ->
            val ja = drug.genericNameJa
            assertTrue("${drug.id} missing genericNameJa", !ja.isNullOrBlank())
            assertTrue(
                "${drug.id} Japanese name '$ja' is just the English INN",
                !ja!!.equals(drug.genericName, ignoreCase = true),
            )
            assertTrue(
                "${drug.id} Japanese name '$ja' is just the Chinese generic name",
                ja != drug.genericNameZh,
            )
        }
    }

    /**
     * 反向防线: 如果以后有人把日文名批量填成中文名 (靠上一条只能拦下"完全相同",
     * 拦不住"换成简体字形"), 那 202 个名字里绝大多数会不含假名。
     * 纯汉字的合法日文名目前只有 3 个, 所以留 5% 余量。
     */
    @Test
    fun mostJapaneseNamesContainKana() {
        val kana = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF]")
        val all = loadCatalog().all()
        val withKana = all.count { kana.containsMatchIn(it.genericNameJa.orEmpty()) }
        val threshold = (all.size * 0.95).toInt()
        assertTrue(
            "only $withKana/${all.size} Japanese names contain kana (expected >= $threshold)",
            withKana >= threshold,
        )
    }

    /**
     * Clozapine is the densest CYP entry in the catalog and the one the rule engine's
     * inhibition path is easiest to regress on, so it doubles as the parse smoke test for
     * the nested `cypProfile` shape.
     */
    @Test
    fun clozapineCarriesACypProfile() {
        val catalog = loadCatalog()
        val clozapine = catalog.tryGetById("clozapine")
        assertNotNull("clozapine missing from catalog", clozapine)
        val profile = clozapine!!.cypProfile
        assertTrue(
            "clozapine should have at least one CYP substrate entry",
            profile.substrates.isNotEmpty() || profile.inhibitors.isNotEmpty(),
        )
    }

    @Test
    fun unknownIdResolvesToNullInsteadOfThrowing() {
        val catalog = loadCatalog()
        assertEquals(null, catalog.tryGetById("definitely_not_a_real_drug"))
    }
}
