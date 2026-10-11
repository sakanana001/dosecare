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
        }
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
