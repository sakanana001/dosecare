package com.dosecare.app.model.pharmacology

import com.dosecare.app.R

/**
 * v0.9g merge: 药理枚举 -> 本地化 string 资源的映射。
 *
 * 这是 model/pharmacology 包里**唯一**允许出现 R 引用的文件。
 * enum 本身保持纯 Kotlin(架构规矩见 docs/architecture.md:`model` is pure Kotlin),
 * 只保留硬编码 displayName;UI 层需要本地化时调这里的 labelRes()。
 *
 * 资源 key 与 DoseCare 原仓库逐字一致(见 docs/third-party-notices.md 的数据源声明),
 * 由 tools/_merge_pharma_strings.py 从 DoseCare 机械搬运,不要手工改名。
 */

fun CypEnzyme.labelRes(): Int = when (this) {
    CypEnzyme.CYP1A2 -> R.string.cyp_cyp1a2
    CypEnzyme.CYP2B6 -> R.string.cyp_cyp2b6
    CypEnzyme.CYP2D6 -> R.string.cyp_cyp2d6
    CypEnzyme.CYP3A4 -> R.string.cyp_cyp3a4
    CypEnzyme.CYP2C9 -> R.string.cyp_cyp2c9
    CypEnzyme.CYP2C19 -> R.string.cyp_cyp2c19
    CypEnzyme.CYP2E1 -> R.string.cyp_cyp2e1
    CypEnzyme.P_GP -> R.string.cyp_p_gp
}

fun DrugCategory.labelRes(): Int = when (this) {
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
    DrugCategory.ANALGESIC -> R.string.drug_cat_analgesic
    DrugCategory.ANTIHISTAMINE -> R.string.drug_cat_antihistamine
    DrugCategory.ANTIBIOTIC -> R.string.drug_cat_antibiotic
    DrugCategory.SUBSTANCE_USE -> R.string.drug_cat_substance_use
    DrugCategory.SUPPLEMENT -> R.string.drug_cat_supplement
    DrugCategory.HERBAL -> R.string.drug_cat_herbal
    DrugCategory.OTHER -> R.string.drug_cat_other
}

fun IndicationGroup.labelRes(): Int = when (this) {
    IndicationGroup.DEPRESSION -> R.string.ind_depression
    IndicationGroup.SCHIZOPHRENIA -> R.string.ind_schizophrenia
    IndicationGroup.BIPOLAR -> R.string.ind_bipolar
    IndicationGroup.ANXIETY -> R.string.ind_anxiety
    IndicationGroup.INSOMNIA -> R.string.ind_insomnia
    IndicationGroup.ADHD -> R.string.ind_adhd
    IndicationGroup.OCD -> R.string.ind_ocd
    IndicationGroup.PTSD -> R.string.ind_ptsd
    IndicationGroup.EATING_DISORDER -> R.string.ind_eating_disorder
    IndicationGroup.EPILEPSY -> R.string.ind_epilepsy
    IndicationGroup.PARKINSON -> R.string.ind_parkinson
    IndicationGroup.ALZHEIMERS -> R.string.ind_alzheimers
    IndicationGroup.MIGRAINE -> R.string.ind_migraine
    IndicationGroup.NEUROPATHIC_PAIN -> R.string.ind_neuropathic_pain
    IndicationGroup.PAIN -> R.string.ind_pain
    IndicationGroup.ANESTHESIA -> R.string.ind_anesthesia
    IndicationGroup.DIABETES -> R.string.ind_diabetes
    IndicationGroup.HYPERTENSION -> R.string.ind_hypertension
    IndicationGroup.DYSLIPIDEMIA -> R.string.ind_dyslipidemia
    IndicationGroup.THYROID -> R.string.ind_thyroid
    IndicationGroup.ANTICOAGULATION -> R.string.ind_anticoagulation
    IndicationGroup.ASTHMA_COPD -> R.string.ind_asthma_copd
    IndicationGroup.OSTEOPOROSIS -> R.string.ind_osteoporosis
    IndicationGroup.HEART_FAILURE -> R.string.ind_heart_failure
    IndicationGroup.ARRHYTHMIA -> R.string.ind_arrhythmia
    IndicationGroup.BPH -> R.string.ind_bph
    IndicationGroup.GERD -> R.string.ind_gerd
    IndicationGroup.SUBSTANCE_USE -> R.string.ind_substance_use
    IndicationGroup.NICOTINE -> R.string.ind_nicotine
    IndicationGroup.INFECTION -> R.string.ind_infection
    IndicationGroup.GOUT -> R.string.ind_gout
    IndicationGroup.INFLAMMATION -> R.string.ind_inflammation
    IndicationGroup.ALLERGY -> R.string.ind_allergy
    IndicationGroup.ADRENAL -> R.string.ind_adrenal
    IndicationGroup.HORMONE -> R.string.ind_hormone
    IndicationGroup.SUPPLEMENT -> R.string.ind_supplement
    IndicationGroup.DI -> R.string.ind_di
    IndicationGroup.ANTICHOLINERGIC -> R.string.ind_anticholinergic
    IndicationGroup.ANTIDEPRESSANT_AUGMENTATION -> R.string.ind_antidepressant_augmentation
    IndicationGroup.WITHDRAWAL -> R.string.ind_withdrawal
    IndicationGroup.NAUSEA -> R.string.ind_nausea
    IndicationGroup.SEDATION -> R.string.ind_sedation
    IndicationGroup.OTHER -> R.string.ind_other
}

fun OverdoseSeverity.labelRes(): Int = when (this) {
    OverdoseSeverity.MILD -> R.string.sev_mild
    OverdoseSeverity.MODERATE -> R.string.sev_moderate
    OverdoseSeverity.SEVERE -> R.string.sev_severe
    OverdoseSeverity.LIFE_THREATENING -> R.string.sev_life_threatening
}

fun PathwayType.labelRes(): Int = when (this) {
    PathwayType.CYP450 -> R.string.pathway_cyp450
    PathwayType.UGT_GLUCURONIDATION -> R.string.pathway_ugt_glucuronidation
    PathwayType.RENAL_EXCRETION -> R.string.pathway_renal_excretion
    PathwayType.HYDROLYSIS -> R.string.pathway_hydrolysis
    PathwayType.ESTERASE -> R.string.pathway_esterase
    PathwayType.DEIODINATION -> R.string.pathway_deiodination
    PathwayType.MAO -> R.string.pathway_mao
    PathwayType.DPP4 -> R.string.pathway_dpp4
    PathwayType.GLUCURONIDATION -> R.string.pathway_glucuronidation
    PathwayType.BETA_OXIDATION -> R.string.pathway_beta_oxidation
    PathwayType.OTHER -> R.string.pathway_other
}

fun Severity.labelRes(): Int = when (this) {
    Severity.CONTRAINDICATED -> R.string.sevrule_contraindicated
    Severity.HIGH -> R.string.sevrule_high
    Severity.MEDIUM -> R.string.sevrule_medium
    Severity.LOW -> R.string.sevrule_low
    Severity.INFO -> R.string.sevrule_info
}

/**
 * v2.0.0 merge: 剂量调整 (肾/肝) 的本地化。
 *
 * DoseCare 原版同样没给这两个枚举做标签, 界面上直接显示枚举名
 * ("CONTRAINDICATED IN SEVERE" / "MONITOR"), 中文和日语界面里都是一串
 * 大写英文。这里补上映射。
 */
fun RenalAdjustment.labelRes(): Int = when (this) {
    RenalAdjustment.NONE -> R.string.renal_none
    RenalAdjustment.MONITOR -> R.string.renal_monitor
    RenalAdjustment.REDUCE_50_PCT_EGFR_BELOW_30 -> R.string.renal_reduce_egfr30
    RenalAdjustment.CONTRAINDICATED_EGFR_BELOW_30 -> R.string.renal_contraindicated_egfr30
}

fun HepaticAdjustment.labelRes(): Int = when (this) {
    HepaticAdjustment.NONE -> R.string.hepatic_none
    HepaticAdjustment.MONITOR -> R.string.hepatic_monitor
    HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B -> R.string.hepatic_reduce_pugh_b
    HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_C -> R.string.hepatic_reduce_pugh_c
    HepaticAdjustment.CONTRAINDICATED_IN_SEVERE -> R.string.hepatic_contraindicated_severe
}

/**
 * 监测频次在目录里是自由字符串 (WEEKLY_FIRST_18WEEKS_THEN_MONTHLY 之类),
 * 不是枚举, 所以按原值查表。
 *
 * 返回 null 表示这个值没收录 (目录更新后可能出现新值), 调用方应回落到原字符串
 * 而不是丢内容。
 */
fun monitoringFrequencyRes(raw: String): Int? = when (raw) {
    "MONTHLY" -> R.string.monfreq_monthly
    "EVERY_3_MONTHS" -> R.string.monfreq_every_3_months
    "AS_NEEDED" -> R.string.monfreq_as_needed
    "EVERY_6_MONTHS" -> R.string.monfreq_every_6_months
    "EVERY_3_6_MONTHS" -> R.string.monfreq_every_3_6_months
    "MONTHLY_FIRST_3_MONTHS_THEN_EVERY_3_MONTHS" ->
        R.string.monfreq_monthly_first_3_then_every_3
    "MONTHLY_INITIALLY" -> R.string.monfreq_monthly_initially
    "PER_PROCEDURE" -> R.string.monfreq_per_procedure
    "EVERY_6_12_MONTHS" -> R.string.monfreq_every_6_12_months
    "WEEKLY_FIRST_18WEEKS_THEN_MONTHLY" -> R.string.monfreq_weekly_18_then_monthly
    "WEEKLY_INITIALLY_THEN_MONTHLY" -> R.string.monfreq_weekly_then_monthly
    "WEEKLY_INITIALLY" -> R.string.monfreq_weekly_initially
    "PER_PRESCRIPTION" -> R.string.monfreq_per_prescription
    "DURING_IV_INFUSION" -> R.string.monfreq_during_iv_infusion
    "BEFORE_EACH_INFUSION" -> R.string.monfreq_before_each_infusion
    "WEEKLY" -> R.string.monfreq_weekly
    "BEFORE_EACH_DOSE" -> R.string.monfreq_before_each_dose
    "EVERY_6_8_WEEKS" -> R.string.monfreq_every_6_8_weeks
    "CONTINUOUS" -> R.string.monfreq_continuous
    else -> null
}
