package com.dosecare.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dosecare.app.R
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.HepaticAdjustment
import com.dosecare.app.domain.catalog.RenalAdjustment
import com.dosecare.app.domain.catalog.RiskLevel

/**
 * 学术化参数 → 普通用户能懂的描述
 *
 * 设计原则:
 * 1. 不取代医学事实,只是换种说法
 * 2. 短句,生活化比喻优先
 * 3. 关键风险(肝/肾/老年)给一句"为什么要小心"
 *
 * 何时显示: DrugDetailScreen 和 ComparisonView 的 toggle "📖 通俗模式" ON
 *
 * v0.9c i18n: 所有函数改 @Composable, 用 stringResource() 走当前 locale
 *  - 静态描述: 直接 stringResource(R.string.xxx)
 *  - 带参数: stringResource(R.string.xxx, %1$d 等)
 *  - emoji / 单位等 runtime 计算的部分保留 Kotlin 拼接
 */
object Plain {

    // ============================================================
    // PK 参数 (体内过程)
    // ============================================================

    /** 半衰期 (h): 体内药物浓度降到一半所需时间 */
    @Composable
    fun halfLife(t12h: Double): String = when {
        t12h < 6 -> stringResource(R.string.plain_half_life_short)
        t12h < 24 -> stringResource(R.string.plain_half_life_medium)
        t12h < 72 -> stringResource(R.string.plain_half_life_long)
        else -> stringResource(R.string.plain_half_life_xlong)
    }

    /** 蛋白结合率: 留在血液里 vs 进入组织 */
    @Composable
    fun proteinBinding(pct: Int): String = when {
        pct >= 90 -> stringResource(R.string.plain_protein_high, pct)
        pct >= 70 -> stringResource(R.string.plain_protein_mid, pct)
        else -> stringResource(R.string.plain_protein_low, pct)
    }

    /** 治疗窗: 浓度低于 → 无效, 高于 → 中毒 */
    @Composable
    fun therapeuticWindow(low: Double, high: Double, unit: String): String =
        stringResource(
            R.string.plain_therapeutic_window,
            formatNum(low), unit, formatNum(high)
        )

    /** Cmax: 服药后血药浓度峰值 */
    @Composable
    fun cmax(cmax: Double, unit: String): String =
        stringResource(R.string.plain_cmax, formatNum(cmax), unit)

    /** Cmin: 下次服药前最低浓度 */
    @Composable
    fun cmin(cmin: Double, unit: String): String =
        stringResource(R.string.plain_cmin, formatNum(cmin), unit)

    /** Cavg: 平均稳态浓度 */
    @Composable
    fun cavg(cavg: Double, unit: String): String =
        stringResource(R.string.plain_cavg, formatNum(cavg), unit)

    /** AUC: 浓度-时间曲线下面积 (总暴露量) */
    @Composable
    fun auc(): String = stringResource(R.string.plain_auc)

    // ============================================================
    // CYP 角色 (肝脏代谢酶)
    // ============================================================

    /** 底物 (被代谢): 这个药需要哪些酶拆掉 */
    @Composable
    fun cypSubstrate(): String = stringResource(R.string.plain_cyp_substrate)

    /** 主要代谢途径 (含 CYP 比例 / 非 CYP 标注) */
    @Composable
    fun metabolism(pathwayType: com.dosecare.app.domain.catalog.PathwayType?): String = when (pathwayType) {
        com.dosecare.app.domain.catalog.PathwayType.CYP450 ->
            stringResource(R.string.plain_metabolism_cyp)
        com.dosecare.app.domain.catalog.PathwayType.UGT_GLUCURONIDATION, com.dosecare.app.domain.catalog.PathwayType.GLUCURONIDATION ->
            stringResource(R.string.plain_metabolism_ugt)
        com.dosecare.app.domain.catalog.PathwayType.RENAL_EXCRETION ->
            stringResource(R.string.plain_metabolism_renal)
        com.dosecare.app.domain.catalog.PathwayType.HYDROLYSIS, com.dosecare.app.domain.catalog.PathwayType.ESTERASE ->
            stringResource(R.string.plain_metabolism_hydrolysis)
        com.dosecare.app.domain.catalog.PathwayType.DEIODINATION ->
            stringResource(R.string.plain_metabolism_deiodination)
        com.dosecare.app.domain.catalog.PathwayType.MAO ->
            stringResource(R.string.plain_metabolism_mao)
        com.dosecare.app.domain.catalog.PathwayType.DPP4 ->
            stringResource(R.string.plain_metabolism_dpp4)
        com.dosecare.app.domain.catalog.PathwayType.BETA_OXIDATION ->
            stringResource(R.string.plain_metabolism_beta_oxidation)
        else ->
            stringResource(R.string.plain_metabolism_other)
    }

    /** 抑制剂 (影响其他药): 这个药会抑制某些酶 */
    @Composable
    fun cypInhibitor(strength: String): String = when (strength) {
        "STRONG" -> stringResource(R.string.plain_cyp_inhibitor_strong)
        "MODERATE" -> stringResource(R.string.plain_cyp_inhibitor_moderate)
        "WEAK" -> stringResource(R.string.plain_cyp_inhibitor_weak)
        else -> ""
    }

    /** 诱导剂: 这个药会加速某些酶, 其它药代谢加快 */
    @Composable
    fun cypInducer(strength: String): String = when (strength) {
        "STRONG" -> stringResource(R.string.plain_cyp_inducer_strong)
        "MODERATE" -> stringResource(R.string.plain_cyp_inducer_moderate)
        "WEAK" -> stringResource(R.string.plain_cyp_inducer_weak)
        else -> ""
    }

    // ============================================================
    // 调整 (renal/hepatic/elderly/smoking)
    // ============================================================

    @Composable
    fun renalAdj(adj: RenalAdjustment): String = when (adj) {
        RenalAdjustment.NONE -> stringResource(R.string.plain_renal_none)
        RenalAdjustment.MONITOR -> stringResource(R.string.plain_renal_monitor)
        RenalAdjustment.REDUCE_50_PCT_EGFR_BELOW_30 ->
            stringResource(R.string.plain_renal_reduce_egfr30)
        RenalAdjustment.CONTRAINDICATED_EGFR_BELOW_30 ->
            stringResource(R.string.plain_renal_contra_egfr30)
    }

    @Composable
    fun hepaticAdj(adj: HepaticAdjustment): String = when (adj) {
        HepaticAdjustment.NONE -> stringResource(R.string.plain_hepatic_none)
        HepaticAdjustment.MONITOR -> stringResource(R.string.plain_hepatic_monitor)
        HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B ->
            stringResource(R.string.plain_hepatic_reduce_pugh_b)
        HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_C ->
            stringResource(R.string.plain_hepatic_reduce_pugh_c)
        HepaticAdjustment.CONTRAINDICATED_IN_SEVERE ->
            stringResource(R.string.plain_hepatic_contra_severe)
    }

    @Composable
    fun elderly(): String = stringResource(R.string.plain_elderly)

    // ============================================================
    // 不良反应 (RiskLevel)
    // ============================================================

    @Composable
    fun qtcProlongation(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> stringResource(R.string.plain_qtc_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_qtc_medium)
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> stringResource(R.string.plain_qtc_high)
    }

    @Composable
    fun metabolicSyndrome(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> stringResource(R.string.plain_metabolic_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_metabolic_medium)
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> stringResource(R.string.plain_metabolic_high)
    }

    @Composable
    fun agranulocytosis(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW -> stringResource(R.string.plain_agran_low)
        RiskLevel.LOW -> stringResource(R.string.plain_agran_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_agran_medium)
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> stringResource(R.string.plain_agran_high)
    }

    @Composable
    fun extrapyramidal(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> stringResource(R.string.plain_eps_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_eps_medium)
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> stringResource(R.string.plain_eps_high)
    }

    @Composable
    fun sedation(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> stringResource(R.string.plain_sedation_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_sedation_medium)
        RiskLevel.HIGH -> stringResource(R.string.plain_sedation_high)
        RiskLevel.VERY_HIGH -> stringResource(R.string.plain_sedation_xhigh)
    }

    @Composable
    fun sexual(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> stringResource(R.string.plain_sexual_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_sexual_medium)
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> stringResource(R.string.plain_sexual_high)
    }

    @Composable
    fun hyperprolactinemia(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> stringResource(R.string.plain_prolactin_low)
        RiskLevel.MEDIUM -> stringResource(R.string.plain_prolactin_medium)
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> stringResource(R.string.plain_prolactin_high)
    }

    @Composable
    fun anticholinergicLoad(score: Int): String = when {
        score == 0 -> stringResource(R.string.plain_anticholinergic_0)
        score == 1 -> stringResource(R.string.plain_anticholinergic_1)
        score == 2 -> stringResource(R.string.plain_anticholinergic_2)
        else -> stringResource(R.string.plain_anticholinergic_3plus)
    }

    // ============================================================
    // 引用 / 指南 (sources)
    // ============================================================

    @Composable
    fun guideline(name: String): String = when {
        name.contains("AGNP") -> stringResource(R.string.plain_guideline_agnp)
        name.contains("FDA") -> stringResource(R.string.plain_guideline_fda)
        name.contains("CPIC") -> stringResource(R.string.plain_guideline_cpic)
        name.contains("PMID") -> stringResource(R.string.plain_guideline_pmid)
        name.contains("ACR") -> stringResource(R.string.plain_guideline_acr)
        name.contains("AUA") -> stringResource(R.string.plain_guideline_aua)
        name.contains("Beers") -> stringResource(R.string.plain_guideline_beers)
        name.contains("国家") || name.contains("国麻") -> stringResource(R.string.plain_guideline_china_national)
        name.contains("中国") -> stringResource(R.string.plain_guideline_china_general)
        else -> name
    }

    // ============================================================
    // 工具
    // ============================================================

    private fun formatNum(n: Double): String =
        if (n >= 100) "%.0f".format(n) else if (n >= 10) "%.1f".format(n) else "%.2f".format(n)
}
