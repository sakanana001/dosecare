package com.picohrt.model.medication

enum class MedicationCategory {
    ESTRADIOL,
    TESTOSTERONE,
    ANTIANDROGEN,
    SERM,
    GNRH_AGONIST,
    CUSTOM;

    companion object {
        fun fromStorageValue(value: String?): MedicationCategory {
            return entries.firstOrNull { it.name == value } ?: CUSTOM
        }
    }
}

enum class MedicationApplicationType {
    ORAL,
    SUBLINGUAL,
    INJECTION,
    GEL,
    PATCH_ON,
    PATCH_OFF;

    companion object {
        fun fromStorageValue(value: String?): MedicationApplicationType {
            return entries.firstOrNull { it.name == value } ?: ORAL
        }
    }
}

enum class MedicationGelApplicationArea {
    DEFAULT;

    companion object {
        fun fromStorageValue(value: String?): MedicationGelApplicationArea {
            return entries.firstOrNull { it.name == value } ?: DEFAULT
        }
    }
}

enum class MedicationSelectionKind {
    CATALOG,
    CUSTOM;

    companion object {
        fun fromStorageValue(value: String?): MedicationSelectionKind {
            return entries.firstOrNull { it.name == value } ?: CUSTOM
        }
    }
}

enum class MedicationKey(val category: MedicationCategory) {
    SPIRONOLACTONE(category = MedicationCategory.ANTIANDROGEN),
    CYPROTERONE_ACETATE(category = MedicationCategory.ANTIANDROGEN),
    BICALUTAMIDE(category = MedicationCategory.ANTIANDROGEN),
    FINASTERIDE(category = MedicationCategory.ANTIANDROGEN),
    DUTASTERIDE(category = MedicationCategory.ANTIANDROGEN),
    RALOXIFENE(category = MedicationCategory.SERM),
    TAMOXIFEN(category = MedicationCategory.SERM),
    TRIPTORELIN(category = MedicationCategory.GNRH_AGONIST),
    LEUPRORELIN(category = MedicationCategory.GNRH_AGONIST),
    GOSERELIN(category = MedicationCategory.GNRH_AGONIST),
    ESTRADIOL(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_VALERATE(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_BENZOATE(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_CYPIONATE(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_ENANTHATE(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_UNDECYLATE(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_GEL(category = MedicationCategory.ESTRADIOL),
    ESTRADIOL_PATCH(category = MedicationCategory.ESTRADIOL),
    // F-PR2: testosterone esters + transdermal gel. Drives the
    // toTestosteronePkCompound mapping to PkCompound.{TE,TC,TU,T} so the
    // FtM chart picks the right depot kinetics per ester (androgel
    // kinetics for GEL) instead of falling back to the custom-T average.
    TESTOSTERONE_ENANTHATE(category = MedicationCategory.TESTOSTERONE),
    TESTOSTERONE_CYPIONATE(category = MedicationCategory.TESTOSTERONE),
    TESTOSTERONE_UNDECANOATE(category = MedicationCategory.TESTOSTERONE),
    TESTOSTERONE_GEL(category = MedicationCategory.TESTOSTERONE);

    companion object {
        fun fromStorageValue(value: String?): MedicationKey? {
            return entries.firstOrNull { it.name == value }
        }
    }
}

// Catalog entries carry medication-key identity plus assist-chip presets for
// raw preparation/dose fields. The actual preparation and dose values still
// live in the picker drafts until save.
data class MedicationCatalogEntry(
    val medicationKey: MedicationKey?,
    val doseAssistPresets: Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> = emptyMap(),
)

sealed interface MedicationDoseAssistPreset {
    data class MgAsMedicine(
        val valueMg: String,
    ) : MedicationDoseAssistPreset

    data class GelPercent(
        val percent: String,
    ) : MedicationDoseAssistPreset

    data class GelWeightGrams(
        val weightGrams: String,
    ) : MedicationDoseAssistPreset

    data class GelContainerSizeGrams(
        val weightGrams: String,
    ) : MedicationDoseAssistPreset

    data class MultiUseVialConcentrationMgPerMl(
        val mgPerMl: String,
    ) : MedicationDoseAssistPreset

    data class MultiUseVialVolumeMl(
        val volumeMl: String,
    ) : MedicationDoseAssistPreset

    data class PatchTotalMg(
        val valueMg: String,
    ) : MedicationDoseAssistPreset

    data class PatchReleaseRateMcgPerDay(
        val valueMcgPerDay: String,
        // PR3.5: optional product label so the UI can group presets by
        // brand. Examples: "Climara 25", "Climara 50", "Estrana Tape 50".
        // Null keeps the legacy "plain mcg/day" tile working for users
        // who just type a release rate without picking a product.
        val productLabel: String? = null,
        // PR3.5: optional wear duration hint for the picker. The engine
        // already binds PATCH_OFF to its prior PATCH_ON; this is display-
        // only metadata (e.g. "7 days" for Climara, "3-4 days" for
        // Vivelle-Dot) so the picker can show the cadence up-front.
        val wearDays: Int? = null,
    ) : MedicationDoseAssistPreset
}

data class MedicationApplicationCatalog(
    val category: MedicationCategory,
    val applicationType: MedicationApplicationType,
    val entries: List<MedicationCatalogEntry>,
    val allowCustomMedicationName: Boolean,
)

object MedicationCatalog {
    private val estradiolCatalog = listOf(
        MedicationApplicationCatalog(
            category = MedicationCategory.ESTRADIOL,
            applicationType = MedicationApplicationType.ORAL,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_VALERATE,
                    doseAssistPresets = pillMgDoseAssistPresets("1", "2"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL,
                    doseAssistPresets = pillMgDoseAssistPresets("1", "2"),
                ),
            ),
            allowCustomMedicationName = false,
        ),
        MedicationApplicationCatalog(
            category = MedicationCategory.ESTRADIOL,
            applicationType = MedicationApplicationType.SUBLINGUAL,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_VALERATE,
                    doseAssistPresets = pillMgDoseAssistPresets("1", "2"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL,
                    doseAssistPresets = pillMgDoseAssistPresets("1", "2"),
                ),
            ),
            allowCustomMedicationName = false,
        ),
        MedicationApplicationCatalog(
            category = MedicationCategory.ESTRADIOL,
            applicationType = MedicationApplicationType.INJECTION,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_VALERATE,
                    doseAssistPresets = injectableEstradiolDoseAssistPresets(),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_CYPIONATE,
                    doseAssistPresets = injectableEstradiolDoseAssistPresets(),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_ENANTHATE,
                    doseAssistPresets = injectableEstradiolDoseAssistPresets(),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_BENZOATE,
                    doseAssistPresets = injectableEstradiolDoseAssistPresets(),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_UNDECYLATE,
                    doseAssistPresets = injectableEstradiolDoseAssistPresets(),
                ),
            ),
            allowCustomMedicationName = false,
        ),
        MedicationApplicationCatalog(
            category = MedicationCategory.ESTRADIOL,
            applicationType = MedicationApplicationType.GEL,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_GEL,
                    doseAssistPresets = gelDoseAssistPresets(),
                ),
            ),
            allowCustomMedicationName = false,
        ),
        MedicationApplicationCatalog(
            category = MedicationCategory.ESTRADIOL,
            applicationType = MedicationApplicationType.PATCH_ON,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.ESTRADIOL_PATCH,
                    doseAssistPresets = patchDoseAssistPresets(),
                ),
            ),
            allowCustomMedicationName = false,
        ),
        // PATCH_OFF retains its catalog entry so legacy slot/log code paths
        // that look up "the catalog for ESTRADIOL + PATCH_OFF" don't NPE.
        // Creating new medicines via this route is blocked by the creation
        // form picker; the PATCH_OFF singleton is the only patch-off Medicine
        // the app produces, and it's auto-created from MedicineRepository.
        MedicationApplicationCatalog(
            category = MedicationCategory.ESTRADIOL,
            applicationType = MedicationApplicationType.PATCH_OFF,
            entries = listOf(
                MedicationCatalogEntry(medicationKey = MedicationKey.ESTRADIOL_PATCH),
            ),
            allowCustomMedicationName = false,
        ),
    )

    private val antiandrogenCatalog = listOf(
        MedicationApplicationCatalog(
            category = MedicationCategory.ANTIANDROGEN,
            applicationType = MedicationApplicationType.ORAL,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.SPIRONOLACTONE,
                    doseAssistPresets = pillMgDoseAssistPresets("20", "25", "50", "100"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.CYPROTERONE_ACETATE,
                    doseAssistPresets = pillMgDoseAssistPresets("50", "100"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.BICALUTAMIDE,
                    doseAssistPresets = pillMgDoseAssistPresets("50", "80"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.FINASTERIDE,
                    doseAssistPresets = pillMgDoseAssistPresets("1", "5"),
                ),
            ),
            allowCustomMedicationName = false,
        ),
    )

    // Dutasteride ships as a soft-gel capsule swallowed whole, so it lives in
    // the CAPSULE form bucket rather than the ORAL tablet catalog — the two
    // form lists must not leak into each other.
    private val antiandrogenCapsuleEntries = listOf(
        MedicationCatalogEntry(
            medicationKey = MedicationKey.DUTASTERIDE,
            doseAssistPresets = capsuleMgDoseAssistPresets("0.5"),
        ),
    )

    private val sermCatalog = listOf(
        MedicationApplicationCatalog(
            category = MedicationCategory.SERM,
            applicationType = MedicationApplicationType.ORAL,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.RALOXIFENE,
                    doseAssistPresets = pillMgDoseAssistPresets("60"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.TAMOXIFEN,
                    doseAssistPresets = pillMgDoseAssistPresets("10", "20"),
                ),
            ),
            allowCustomMedicationName = false,
        ),
    )

    private val gnrhAgonistCatalog = listOf(
        MedicationApplicationCatalog(
            category = MedicationCategory.GNRH_AGONIST,
            applicationType = MedicationApplicationType.INJECTION,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.TRIPTORELIN,
                    doseAssistPresets = depotInjectionMgDoseAssistPresets("3.75", "11.25", "22.5"),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.LEUPRORELIN,
                    doseAssistPresets = depotInjectionMgDoseAssistPresets(
                        "1.88", "3.75", "11.25", "22.5", "30",
                    ),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.GOSERELIN,
                    doseAssistPresets = depotInjectionMgDoseAssistPresets("3.6", "10.8"),
                ),
            ),
            allowCustomMedicationName = false,
        ),
    )

    private fun depotInjectionMgDoseAssistPresets(
        vararg valuesMg: String,
    ): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return mapOf(
            MedicinePreparationType.INJECTION_SINGLE_USE_VIAL to valuesMg.map {
                MedicationDoseAssistPreset.MgAsMedicine(it)
            },
        )
    }

    // F-PR2: testosterone catalog. INJECTION + GEL now expose the four
    // catalog keys (TE / TC / TU + GEL) so the user can pick the
    // specific ester and have the right depot kinetics land in the
    // PkCompound mapping (instead of the custom-T average). ORAL +
    // SUBLINGUAL stay custom-only because the clinically used T
    // products today are all injectable (TU Nebido, TE Delatestryl,
    // TC Depo-Testosterone) or transdermal (AndroGel, Testim); an
    // oral-T product (Jatenzo) is post-v1 scope.
    private val testosteroneCatalog = listOf(
        MedicationApplicationCatalog(
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.INJECTION,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.TESTOSTERONE_ENANTHATE,
                    doseAssistPresets = injectableTestosteroneDoseAssistPresets(),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.TESTOSTERONE_CYPIONATE,
                    doseAssistPresets = injectableTestosteroneDoseAssistPresets(),
                ),
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.TESTOSTERONE_UNDECANOATE,
                    doseAssistPresets = injectableTestosteroneUndecanoateDoseAssistPresets(),
                ),
            ),
            allowCustomMedicationName = true,
        ),
        MedicationApplicationCatalog(
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.GEL,
            entries = listOf(
                MedicationCatalogEntry(
                    medicationKey = MedicationKey.TESTOSTERONE_GEL,
                    doseAssistPresets = gelDoseAssistPresets(),
                ),
            ),
            allowCustomMedicationName = true,
        ),
        // ORAL + SUBLINGUAL keep the pre-F-PR2 custom-only fallback.
        MedicationApplicationCatalog(
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.ORAL,
            entries = listOf(MedicationCatalogEntry(medicationKey = null)),
            allowCustomMedicationName = true,
        ),
        MedicationApplicationCatalog(
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.SUBLINGUAL,
            entries = listOf(MedicationCatalogEntry(medicationKey = null)),
            allowCustomMedicationName = true,
        ),
    )

    private val customCatalog = listOf(
        MedicationApplicationCatalog(
            category = MedicationCategory.CUSTOM,
            applicationType = MedicationApplicationType.ORAL,
            entries = listOf(MedicationCatalogEntry(medicationKey = null)),
            allowCustomMedicationName = true,
        ),
    )

    private val catalogs =
        estradiolCatalog + antiandrogenCatalog + sermCatalog + gnrhAgonistCatalog +
                testosteroneCatalog + customCatalog

    fun applicationTypesFor(category: MedicationCategory): List<MedicationApplicationType> {
        return catalogs
            .filter { it.category == category }
            .map(MedicationApplicationCatalog::applicationType)
    }

    fun catalogFor(
        category: MedicationCategory,
        applicationType: MedicationApplicationType,
    ): MedicationApplicationCatalog {
        return catalogs.first { catalog ->
            catalog.category == category && catalog.applicationType == applicationType
        }
    }

    fun defaultEntryFor(
        category: MedicationCategory,
        applicationType: MedicationApplicationType,
    ): MedicationCatalogEntry {
        return catalogFor(category, applicationType).entries.first()
    }

    // Routes the user can choose for a TABLET-form medicine in this category.
    // Antiandrogen and custom catalogs only register ORAL pills, so picking
    // sublingual for them would be unsupported; callers use this to suppress
    // the route picker when there is no real choice.
    fun tabletRoutesFor(category: MedicationCategory): List<MedicationApplicationType> {
        return applicationTypesFor(category).filter { applicationType ->
            applicationType == MedicationApplicationType.ORAL ||
                    applicationType == MedicationApplicationType.SUBLINGUAL
        }
    }

    fun preparationFormsFor(category: MedicationCategory): List<MedicinePreparationForm> {
        val forms = applicationTypesFor(category)
            .flatMap { applicationType -> applicationType.preparationForms() }
        val capsuleForms = if (
            category == MedicationCategory.CUSTOM ||
            category == MedicationCategory.ANTIANDROGEN
        ) {
            listOf(MedicinePreparationForm.CAPSULE)
        } else {
            emptyList()
        }

        return (forms + capsuleForms).distinct()
    }

    fun entriesForForm(
        category: MedicationCategory,
        form: MedicinePreparationForm,
    ): List<MedicationCatalogEntry> {
        if (form == MedicinePreparationForm.CAPSULE) {
            return when (category) {
                MedicationCategory.CUSTOM -> listOf(MedicationCatalogEntry(medicationKey = null))
                MedicationCategory.ANTIANDROGEN -> antiandrogenCapsuleEntries
                else -> emptyList()
            }
        }

        val entries = applicationTypesFor(category)
            .filter { applicationType -> form in applicationType.preparationForms() }
            .flatMap { applicationType ->
                catalogFor(
                    category = category,
                    applicationType = applicationType,
                ).entries
            }

        return mergeCatalogEntries(entries)
    }

    private fun mergeCatalogEntries(entries: List<MedicationCatalogEntry>): List<MedicationCatalogEntry> {
        return entries
            .groupBy(MedicationCatalogEntry::medicationKey)
            .map { (medicationKey, entriesForMedication) ->
                MedicationCatalogEntry(
                    medicationKey = medicationKey,
                    doseAssistPresets = mergeDoseAssistPresets(
                        entriesForMedication.map(MedicationCatalogEntry::doseAssistPresets),
                    ),
                )
            }
    }

    private fun mergeDoseAssistPresets(
        presetMaps: List<Map<MedicinePreparationType, List<MedicationDoseAssistPreset>>>,
    ): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return presetMaps
            .flatMap { presetsByPreparation -> presetsByPreparation.entries }
            .groupBy(
                keySelector = { entry -> entry.key },
                valueTransform = { entry -> entry.value },
            )
            .mapValues { (_, presetLists) -> presetLists.flatten().distinct() }
    }

    private fun pillMgDoseAssistPresets(
        vararg valuesMg: String,
    ): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return mapOf(
            MedicinePreparationType.PILL to valuesMg.map {
                MedicationDoseAssistPreset.MgAsMedicine(it)
            },
        )
    }

    private fun capsuleMgDoseAssistPresets(
        vararg valuesMg: String,
    ): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return mapOf(
            MedicinePreparationType.CAPSULE to valuesMg.map {
                MedicationDoseAssistPreset.MgAsMedicine(it)
            },
        )
    }

    private fun singleUseVialMgDoseAssistPresets(
        vararg valuesMg: String,
    ): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return mapOf(
            MedicinePreparationType.INJECTION_SINGLE_USE_VIAL to valuesMg.map {
                MedicationDoseAssistPreset.MgAsMedicine(it)
            },
        )
    }

    private fun injectableEstradiolDoseAssistPresets(): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return singleUseVialMgDoseAssistPresets("5", "10") +
                multiUseVialDoseAssistPresets(
                    concentrationsMgPerMl = listOf("20", "40", "50", "80", "100"),
                    volumesMl = listOf("5", "10"),
                )
    }

    // F-PR2: testosterone injection dose presets. TE/TC products are
    // 100-200 mg/mL single-use or 200 mg/mL multi-use; TU (Nebido) is
    // 250 mg/mL in a 4 mL vial (1000 mg total dose, q10-14 weeks).
    // Single-use vial is the most common form for TE/TC.
    private fun injectableTestosteroneDoseAssistPresets(): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return singleUseVialMgDoseAssistPresets("100", "200") +
                multiUseVialDoseAssistPresets(
                    concentrationsMgPerMl = listOf("100", "200"),
                    volumesMl = listOf("1", "5", "10"),
                )
    }

    // F-PR2: TU (Nebido) is sold as 1000 mg in 4 mL (250 mg/mL); the
    // single-use vial is the standard form. Multi-use options stay
    // available for off-label or compounded TU products.
    private fun injectableTestosteroneUndecanoateDoseAssistPresets(): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return singleUseVialMgDoseAssistPresets("1000") +
                multiUseVialDoseAssistPresets(
                    concentrationsMgPerMl = listOf("250"),
                    volumesMl = listOf("4"),
                )
    }

    private fun multiUseVialDoseAssistPresets(
        concentrationsMgPerMl: List<String>,
        volumesMl: List<String>,
    ): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        return mapOf(
            MedicinePreparationType.INJECTION_MULTI_USE_VIAL to (
                    concentrationsMgPerMl.map {
                        MedicationDoseAssistPreset.MultiUseVialConcentrationMgPerMl(it)
                    } + volumesMl.map {
                        MedicationDoseAssistPreset.MultiUseVialVolumeMl(it)
                    }
                    ),
        )
    }

    private fun gelDoseAssistPresets(): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        val percentPresets = listOf(
            MedicationDoseAssistPreset.GelPercent("0.06"),
            MedicationDoseAssistPreset.GelPercent("0.1"),
            MedicationDoseAssistPreset.GelPercent("0.3"),
            MedicationDoseAssistPreset.GelPercent("0.6"),
        )
        val sachetWeightPresets = listOf(
            MedicationDoseAssistPreset.GelWeightGrams("0.5"),
            MedicationDoseAssistPreset.GelWeightGrams("1.0"),
            MedicationDoseAssistPreset.GelWeightGrams("1.25"),
            MedicationDoseAssistPreset.GelWeightGrams("2.5"),
        )
        val containerSizePresets = listOf(
            MedicationDoseAssistPreset.GelContainerSizeGrams("80"),
        )
        return mapOf(
            MedicinePreparationType.GEL_SACHET to percentPresets + sachetWeightPresets,
            MedicinePreparationType.GEL_CONTAINER to percentPresets + containerSizePresets,
        )
    }

    private fun patchDoseAssistPresets(): Map<MedicinePreparationType, List<MedicationDoseAssistPreset>> {
        // PR3.5: surface the four transdermal E2 products that have
        // measured steady-state Css anchors in the literature, plus the
        // legacy unbranded "plain mcg/day" tiles for users who want to
        // type a custom release rate. Each branded preset carries a
        // `productLabel` (picker grouping) and `wearDays` (cadence hint).
        //
        // PK references:
        //   - Climara (Bayer) 7-day patch: 25/50/75/100 mcg/day, 0.05 mg
        //     Cavg ~41 pg/mL (DailyMed 310178; Buch 1998).
        //   - Estraderm MX 3-4 day patch: 25/50/75/100 mcg/day.
        //   - Vivelle-Dot 3-4 day patch: 25/50/75/100 mcg/day.
        //   - Estrana Tape (Hisamitsu, 7-day JP patch): 0.72 mg total,
        //     ~50 mcg/day (KPSC 1 mg 添付文書 equivalent).
        val branded = listOf(
            // Climara
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "25", productLabel = "Climara 25", wearDays = 7,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "50", productLabel = "Climara 50", wearDays = 7,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "75", productLabel = "Climara 75", wearDays = 7,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "100", productLabel = "Climara 100", wearDays = 7,
            ),
            // Estraderm MX
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "25", productLabel = "Estraderm MX 25", wearDays = 4,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "50", productLabel = "Estraderm MX 50", wearDays = 4,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "100", productLabel = "Estraderm MX 100", wearDays = 4,
            ),
            // Vivelle-Dot
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "25", productLabel = "Vivelle-Dot 25", wearDays = 4,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "50", productLabel = "Vivelle-Dot 50", wearDays = 4,
            ),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "100", productLabel = "Vivelle-Dot 100", wearDays = 4,
            ),
            // Estrana Tape (久光, 7-day JP patch)
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(
                "50", productLabel = "Estrana Tape 50", wearDays = 7,
            ),
        )
        val plain = listOf(
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay("50"),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay("75"),
            MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay("100"),
            MedicationDoseAssistPreset.PatchTotalMg("0.36"),
            MedicationDoseAssistPreset.PatchTotalMg("0.72"),
        )
        return mapOf(
            MedicinePreparationType.PATCH to branded + plain,
        )
    }
}

fun MedicationApplicationType.preparationForms(): List<MedicinePreparationForm> {
    return when (this) {
        MedicationApplicationType.ORAL,
        MedicationApplicationType.SUBLINGUAL,
            -> listOf(MedicinePreparationForm.TABLET)

        MedicationApplicationType.INJECTION -> listOf(MedicinePreparationForm.INJECTION)
        MedicationApplicationType.GEL -> listOf(MedicinePreparationForm.GEL)
        MedicationApplicationType.PATCH_ON -> listOf(MedicinePreparationForm.PATCH)
        MedicationApplicationType.PATCH_OFF -> emptyList()
    }
}
