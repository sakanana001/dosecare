package com.dosecare.app.di

import android.content.Context
import com.dosecare.app.model.pharmacology.DrugCatalogLoader
import com.dosecare.app.model.pharmacology.DrugCatalogService
import com.dosecare.app.model.pharmacology.PatientProfile
import com.dosecare.app.model.pharmacology.RuleEngine
import com.dosecare.app.util.AppDiagnosticsLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Singleton

/**
 * v0.9g merge: pharmacology catalog + interaction rule engine wiring.
 *
 * The catalog ships as an asset (`app/src/main/assets/pharmacology/catalog.json`, 202 drugs)
 * rather than as Room tables — it is read-only reference data that the user never edits, and
 * 202 entries index in memory in microseconds. Keeping it out of Room also means the schema
 * migration for the merge is a single additive column on `medicines` rather than eleven new
 * tables.
 *
 * The asset is read once at first injection and the parsed result is held as a singleton.
 * Parsing 800 KB of JSON costs a few milliseconds; doing it lazily on first access keeps it
 * off the cold-start critical path for screens that never touch the catalog.
 *
 * If the asset is missing or malformed the catalog degrades to an empty list rather than
 * crashing the app: interaction detection is an advisory feature, and a missing catalog must
 * not take down dose logging, reminders, or the PK projection.
 */
@Module
@InstallIn(SingletonComponent::class)
object PharmacologyModule {

    private const val TAG = "PharmacologyModule"
    private const val CATALOG_ASSET = "pharmacology/catalog.json"

    @Provides
    @Singleton
    fun provideDrugCatalogService(
        @ApplicationContext context: Context,
        diagnosticsLogger: AppDiagnosticsLogger,
    ): DrugCatalogService {
        val drugs = runCatching {
            context.assets.open(CATALOG_ASSET).use { stream ->
                DrugCatalogLoader.fromJson(stream.readBytes().decodeToString())
            }
        }.getOrElse { error ->
            diagnosticsLogger.warning(
                TAG,
                "pharmacology_catalog_load_failed: asset=$CATALOG_ASSET",
                error,
            )
            emptyList()
        }
        diagnosticsLogger.info(TAG, "pharmacology_catalog_loaded count=${drugs.size}")
        return DrugCatalogService(drugs)
    }

    /**
     * The rule engine is stateless — every call takes the drug list and a patient profile as
     * parameters — so it is safe to share the singleton. Patient personalization (CYP
     * genotype, hepatic/renal status) is passed per call, not baked in.
     */
    @Provides
    @Singleton
    fun provideRuleEngine(
        catalog: DrugCatalogService,
    ): RuleEngine = RuleEngine(catalog)

    /**
     * Default patient profile for the rule engine: no genotype known, no hepatic/renal
     * impairment recorded. Every adjustment the engine applies is therefore the unmodified
     * population-average one, which is the correct conservative default.
     */
    @Provides
    @Singleton
    fun provideDefaultPatientProfile(): PatientProfile = PatientProfile()
}
