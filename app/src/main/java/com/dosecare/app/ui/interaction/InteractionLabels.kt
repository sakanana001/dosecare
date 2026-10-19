package com.dosecare.app.ui.interaction

import androidx.annotation.StringRes
import com.dosecare.app.R
import com.dosecare.app.model.pharmacology.DrugCategory
import com.dosecare.app.model.pharmacology.OverdoseSeverity
import com.dosecare.app.model.pharmacology.Severity

/**
 * Enum -> localized label mapping.
 *
 * In DoseCare (A) this lived on the enums themselves, as a
 * `@StringRes val displayNameRes: Int` constructor property
 * (A: domain/catalog/DrugCatalog.kt, domain/rules/Interactions.kt).
 *
 * Merge phase 1 deliberately kept the domain layer free of `R` references, so
 * `model/pharmacology/` shipped the enums carrying only their non-localized
 * `displayName: String`, while the matching `sev_*` / `sevrule_*` / `drug_cat_*`
 * string resources were already merged into B's `res/values*`. The mapping code
 * was the piece still missing; it lives here, at the UI edge, where the R class
 * belongs. Consumers use `labelRes` instead of A's `displayNameRes`.
 *
 * Kept as exhaustive `when` blocks rather than reflection or name mangling so
 * that adding an enum constant is a compile error until it gets a label.
 */

@get:StringRes
val Severity.labelRes: Int
    get() = when (this) {
        Severity.CONTRAINDICATED -> R.string.sevrule_contraindicated
        Severity.HIGH -> R.string.sevrule_high
        Severity.MEDIUM -> R.string.sevrule_medium
        Severity.LOW -> R.string.sevrule_low
        Severity.INFO -> R.string.sevrule_info
    }

@get:StringRes
val OverdoseSeverity.labelRes: Int
    get() = when (this) {
        OverdoseSeverity.MILD -> R.string.sev_mild
        OverdoseSeverity.MODERATE -> R.string.sev_moderate
        OverdoseSeverity.SEVERE -> R.string.sev_severe
        OverdoseSeverity.LIFE_THREATENING -> R.string.sev_life_threatening
    }

@get:StringRes
val DrugCategory.labelRes: Int
    get() = when (this) {
        // 精神神经
        DrugCategory.ANTIPSYCHOTIC -> R.string.drug_cat_antipsychotic
        DrugCategory.MOOD_STABILIZER -> R.string.drug_cat_mood_stabilizer
        DrugCategory.ANTIDEPRESSANT -> R.string.drug_cat_antidepressant
        DrugCategory.ANXIOLYTIC -> R.string.drug_cat_anxiolytic
        DrugCategory.STIMULANT -> R.string.drug_cat_stimulant
        DrugCategory.ANTICHOLINERGIC -> R.string.drug_cat_anticholinergic
        DrugCategory.ANTIPARKINSONIAN -> R.string.drug_cat_antiparkinsonian
        DrugCategory.ANTIEPILEPTIC -> R.string.drug_cat_antiepileptic
        DrugCategory.ALZHEIMERS -> R.string.drug_cat_alzheimers
        DrugCategory.ANTIMIGRAINE -> R.string.drug_cat_antimigraine
        DrugCategory.MUSCLE_RELAXANT -> R.string.drug_cat_muscle_relaxant
        DrugCategory.ANESTHETIC -> R.string.drug_cat_anesthetic
        DrugCategory.ANTIVERTIGO -> R.string.drug_cat_antivertigo
        // 内科
        DrugCategory.ANTIDIABETIC -> R.string.drug_cat_antidiabetic
        DrugCategory.THYROID -> R.string.drug_cat_thyroid
        DrugCategory.CORTICOSTEROID -> R.string.drug_cat_corticosteroid
        DrugCategory.ANTIHYPERTENSIVE -> R.string.drug_cat_antihypertensive
        DrugCategory.STATIN -> R.string.drug_cat_statin
        DrugCategory.ANTIARRHYTHMIC -> R.string.drug_cat_antiarrhythmic
        DrugCategory.ANTICOAGULANT -> R.string.drug_cat_anticoagulant
        DrugCategory.PPI -> R.string.drug_cat_ppi
        DrugCategory.OSTEOPOROSIS_DRUG -> R.string.drug_cat_osteoporosis_drug
        DrugCategory.GOUT -> R.string.drug_cat_gout
        DrugCategory.BPH_AGENT -> R.string.drug_cat_bph_agent
        DrugCategory.HORMONE_REPLACEMENT -> R.string.drug_cat_hormone_replacement
        DrugCategory.BRONCHODILATOR -> R.string.drug_cat_bronchodilator
        // 疼痛
        DrugCategory.ANALGESIC -> R.string.drug_cat_analgesic
        // 抗感染
        DrugCategory.ANTIHISTAMINE -> R.string.drug_cat_antihistamine
        DrugCategory.ANTIBIOTIC -> R.string.drug_cat_antibiotic
        // 其他
        DrugCategory.SUBSTANCE_USE -> R.string.drug_cat_substance_use
        DrugCategory.SUPPLEMENT -> R.string.drug_cat_supplement
        DrugCategory.HERBAL -> R.string.drug_cat_herbal
        DrugCategory.OTHER -> R.string.drug_cat_other
    }
