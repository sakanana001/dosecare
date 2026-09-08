package com.dosecare.app.domain.rules

import com.dosecare.app.domain.catalog.CypEnzyme
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.catalog.EffectStrength
import com.dosecare.app.domain.catalog.HepaticAdjustment
import com.dosecare.app.domain.catalog.Mechanism as CatalogMechanism
import com.dosecare.app.domain.patient.BiologicalSex
import com.dosecare.app.domain.patient.ChildPugh
import com.dosecare.app.domain.patient.CypGenotype
import com.dosecare.app.domain.patient.PatientProfile
import com.dosecare.app.domain.pk.PkCurve
import com.dosecare.app.domain.catalog.RiskLevel

/**
 * 规则引擎
 *
 * 职责：给定当前用药 + 患者画像 → 严重度分级的相互作用清单。
 *
 * 包含 6 类规则：
 * 1. CYP 代谢相互作用
 * 2. QTc 累积风险
 * 3. 抗胆碱能负荷
 * 4. 5-HT 综合征风险
 * 5. 治疗窗偏离（基于 PK 曲线）
 * 6. 肾/肝剂量调整
 *
 * 设计取舍：
 * - 自研 ~400 行不上 Drools（不重不重的）
 * - 每条规则纯函数，可独立单元测试
 * - 引擎内不存储状态（无副作用），可并发调用
 */
class RuleEngine(
    private val catalog: DrugCatalogService
) {

    /**
     * 评估所有规则。
     *
     * @param userDrugs 当前在用药物（含剂量、给药频次）
     * @param patient 患者画像
     * @param curves 对应每药的 PK 曲线（治疗窗检查需要）
     * @return 严重度降序的相互作用列表
     */
    fun evaluate(
        userDrugs: List<UserDrugForRule>,
        patient: PatientProfile,
        curves: Map<String, PkCurve> = emptyMap()
    ): List<Interaction> {
        val drugs = userDrugs.map { catalog.getById(it.drugCatalogId) }

        val results = mutableListOf<Interaction>()

        results.addAll(checkCypInteractions(drugs, patient))
        checkQtcAdditive(drugs, patient)?.let { results.add(it) }
        checkAnticholinergicLoad(drugs)?.let { results.add(it) }
        checkSerotoninSyndrome(drugs)?.let { results.add(it) }
        results.addAll(checkTherapeuticWindow(drugs, curves))
        results.addAll(checkRenalAdjustment(drugs, patient))
        results.addAll(checkHepaticAdjustment(drugs, patient))
        // v0.5: 4 个新规则 - 药理作用叠加
        checkSedationSynergy(drugs)?.let { results.add(it) }
        checkBleedingRisk(drugs)?.let { results.add(it) }
        checkDopamineAntagonism(drugs)?.let { results.add(it) }
        checkHypotensionRisk(drugs)?.let { results.add(it) }

        return results
            .distinctBy { it.signature }
            .sortedByDescending { it.severity.rank }
    }

    // ============================================================
    // 规则 1：CYP 代谢相互作用
    // ============================================================

    internal fun checkCypInteractions(
        drugs: List<Drug>,
        patient: PatientProfile
    ): List<CypInteraction> {
        val results = mutableListOf<CypInteraction>()

        for (perpetrator in drugs) {
            for (victim in drugs) {
                if (perpetrator.id == victim.id) continue

                // 抑制方向：perpetrator 抑制 victim 的代谢酶
                for (inh in perpetrator.cypProfile.inhibitors) {
                    val sub = victim.cypProfile.substrates.find { it.cyp == inh.cyp }
                    if (sub != null) {
                        val baseFold = estimateAucFoldForInhibition(inh.strength, sub.fraction)
                        val adjustedFold = adjustForPatient(baseFold, patient)
                        val mechanism = when (inh.strength) {
                            EffectStrength.STRONG -> CatalogMechanism.CYP_INHIBITION_STRONG
                            EffectStrength.MODERATE -> CatalogMechanism.CYP_INHIBITION_MODERATE
                            EffectStrength.WEAK -> CatalogMechanism.CYP_INHIBITION_WEAK
                        }
                        results.add(
                            CypInteraction(
                                perpetrator = perpetrator,
                                victim = victim,
                                cyp = inh.cyp,
                                mechanism = when (inh.strength) {
                                    EffectStrength.STRONG -> Mechanism.CYP_INHIBITION_STRONG
                                    EffectStrength.MODERATE -> Mechanism.CYP_INHIBITION_MODERATE
                                    EffectStrength.WEAK -> Mechanism.CYP_INHIBITION_WEAK
                                },
                                aucFoldChange = baseFold,
                                patientAdjustedFold = adjustedFold
                            )
                        )
                    }
                }

                // 诱导方向
                for (ind in perpetrator.cypProfile.inducers) {
                    val sub = victim.cypProfile.substrates.find { it.cyp == ind.cyp }
                    if (sub != null) {
                        val baseFold = estimateAucFoldForInduction(ind.strength)
                        val adjustedFold = adjustForPatientInverse(baseFold, patient)
                        results.add(
                            CypInteraction(
                                perpetrator = perpetrator,
                                victim = victim,
                                cyp = ind.cyp,
                                mechanism = Mechanism.CYP_INDUCTION,
                                aucFoldChange = baseFold,
                                patientAdjustedFold = adjustedFold
                            )
                        )
                    }
                }
            }
        }

        return results
    }

    /**
     * 估算抑制后的 AUC 倍数。
     * 简化：STRONG=5x, MODERATE=2.5x, WEAK=1.3x（与底物占比无关）
     * 实际更精确的公式应考虑底物占比，但 FDA 指南简化版够用。
     */
    private fun estimateAucFoldForInhibition(
        strength: EffectStrength,
        @Suppress("UNUSED_PARAMETER") substrateFraction: Double
    ): Double = when (strength) {
        EffectStrength.STRONG -> 5.0
        EffectStrength.MODERATE -> 2.5
        EffectStrength.WEAK -> 1.3
    }

    /**
     * 估算诱导后的 AUC 倍数（小于 1）。
     */
    private fun estimateAucFoldForInduction(strength: EffectStrength): Double = when (strength) {
        EffectStrength.STRONG -> 0.2
        EffectStrength.MODERATE -> 0.5
        EffectStrength.WEAK -> 0.7
    }

    /**
     * 患者特征调整（肾损伤、肝损伤加重蓄积）
     */
    private fun adjustForPatient(baseFold: Double, patient: PatientProfile): Double {
        var fold = baseFold
        if (patient.egfr != null && patient.egfr < 30.0) fold *= 1.2
        if (patient.childPugh == ChildPugh.B) fold *= 1.3
        if (patient.childPugh == ChildPugh.C) fold *= 1.6
        // CYP 多态性
        if (patient.cyp2D6Genotype == CypGenotype.PM && baseFold > 1.0) fold *= 1.2
        if (patient.cyp2C19Genotype == CypGenotype.PM && baseFold > 1.0) fold *= 1.2
        return fold
    }

    private fun adjustForPatientInverse(baseFold: Double, patient: PatientProfile): Double {
        var fold = baseFold
        // 诱导：吸烟者更强
        if (patient.smoker) fold *= 0.85
        return fold
    }

    // ============================================================
    // 规则 2：QTc 累积风险
    // ============================================================

    internal fun checkQtcAdditive(
        drugs: List<Drug>,
        patient: PatientProfile
    ): QtcInteraction? {
        if (drugs.isEmpty()) return null

        // 各药的 QTc 延长效应（ms），来自 CredibleMeds + 药品说明书
        val qtcTable = mapOf(
            "ziprasidone" to 20.3,
            "haloperidol_iv" to 14.7,
            "citalopram" to 8.5,
            "escitalopram" to 4.5,
            "quetiapine" to 5.7,
            "olanzapine" to 1.5,
            "risperidone" to 2.0,
            "aripiprazole" to 1.0,
            "amiodarone" to 35.0,
            "sotalol" to 25.0,
            "ondansetron" to 5.0
        )

        var totalQtc = 0.0
        val contributors = mutableListOf<Drug>()

        for (drug in drugs) {
            val effect = qtcTable[drug.id] ?: continue
            totalQtc += effect
            contributors.add(drug)
        }

        if (contributors.isEmpty()) return null

        // 患者风险加权
        val femaleBonus = if (patient.biologicalSex == BiologicalSex.FEMALE) 10.0 else 0.0
        val finalQtc = 400.0 + totalQtc + femaleBonus

        return QtcInteraction(
            contributors = contributors,
            totalQtcProlongationMs = totalQtc,
            finalEstimatedQtcMs = finalQtc
        )
    }

    // ============================================================
    // 规则 3：抗胆碱能负荷
    // ============================================================

    internal fun checkAnticholinergicLoad(drugs: List<Drug>): AnticholinergicLoadInteraction? {
        if (drugs.isEmpty()) return null

        val total = drugs.sumOf { it.adverseEffects.anticholinergicLoad }
        if (total < 2) return null

        val contributors = drugs.filter { it.adverseEffects.anticholinergicLoad > 0 }
        return AnticholinergicLoadInteraction(contributors, total)
    }

    // ============================================================
    // 规则 4：5-HT 综合征
    // ============================================================

    internal fun checkSerotoninSyndrome(drugs: List<Drug>): SerotoninSyndromeRisk? {
        // 简化：标记"5-HT 增强"标签的药
        val serotonergic = drugs.filter { drug ->
            SEROTONERGIC_IDS.any { it == drug.id }
        }
        if (serotonergic.size < 2) return null

        // 危险组合：包含 MAOI
        val hasMaoI = serotonergic.any { it.id in MAOI_IDS }
        val hasSsri = serotonergic.any { it.id in SSRI_IDS }

        val severity = when {
            hasMaoI && (hasSsri || serotonergic.size >= 2) -> Severity.CONTRAINDICATED
            hasSsri && serotonergic.size >= 3 -> Severity.HIGH
            hasSsri && serotonergic.size >= 2 -> Severity.MEDIUM
            else -> Severity.LOW
        }

        return SerotoninSyndromeRisk(serotonergic, severity)
    }

    companion object {
        // 简化的 5-HT 增强药集合（实际应在 DrugCatalog 中加 serotonergicMechanism 字段）
        private val SEROTONERGIC_IDS = setOf(
            // SSRI
            "fluoxetine", "sertraline", "paroxetine", "fluvoxamine", "citalopram", "escitalopram",
            // SNRI
            "venlafaxine", "duloxetine", "desvenlafaxine",
            // TCA
            "amitriptyline", "clomipramine", "imipramine",
            // MAOI
            "phenelzine", "tranylcypromine", "selegiline",
            // 其他
            "trazodone", "mirtazapine", "buspirone", "lithium", "tryptophan",
            // 阿片类
            "tramadol", "meperidine", "methadone",
            // 其他
            "triptans", "ondansetron"
        )

        private val MAOI_IDS = setOf("phenelzine", "tranylcypromine", "selegiline")
        private val SSRI_IDS = setOf("fluoxetine", "sertraline", "paroxetine", "fluvoxamine", "citalopram", "escitalopram")

        // === v0.5 新增: 出血风险相关 ===
        private val ANTICOAGULANT_IDS = setOf(
            "warfarin", "rivaroxaban", "apixaban", "dabigatran",
            "heparin", "enoxaparin", "fondaparinux"
        )
        private val ANTIPLATELET_IDS = setOf(
            "aspirin", "clopidogrel", "ticagrelor", "prasugrel", "dipyridamole",
            "cilostazol"
        )
        private val SSRI_SNRI_IDS = setOf(
            "fluoxetine", "sertraline", "paroxetine", "fluvoxamine",
            "citalopram", "escitalopram",
            "venlafaxine", "duloxetine", "desvenlafaxine"
        )
        private val NSAID_IDS = setOf(
            "ibuprofen", "naproxen", "celecoxib", "diclofenac",
            "indomethacin", "piroxicam", "ketorolac"
        )
        private val AZOLE_ANTIFUNGAL_IDS = setOf(
            "fluconazole", "ketoconazole", "itraconazole", "voriconazole",
            "posaconazole", "miconazole"
        )

        // === v0.5 新增: 多巴胺 D2 拮抗 (抗精神病) ===
        // 注: 目录中 antipsychotics 类别都纳入
        private val ANTIPSYCHOTIC_IDS = setOf(
            "clozapine", "olanzapine", "risperidone", "quetiapine", "aripiprazole",
            "paliperidone", "asenapine", "lurasidone", "brexpiprazole",
            "cariprazine", "iloperidone", "ziprasidone", "amisulpride"
        )
        private val STRONG_D2_IDS = setOf(
            "risperidone", "paliperidone", "ziprasidone", "amisulpride"
        )
        private val MODERATE_D2_IDS = setOf(
            "olanzapine", "aripiprazole", "asenapine", "lurasidone",
            "brexpiprazole", "cariprazine", "iloperidone"
        )

        // === v0.5 新增: 低血压相关 ===
        private val HYPOTENSIVE_IDS = setOf(
            // α 阻滞
            "tamsulosin", "prazosin", "doxazosin", "terazosin",
            // 抗高血压
            "amlodipine", "lisinopril", "losartan", "metoprolol",
            "atenolol", "carvedilol", "hydrochlorothiazide", "furosemide",
            "valsartan", "enalapril", "diltiazem", "verapamil",
            // 三环类 (α 阻滞)
            "amitriptyline", "nortriptyline", "clomipramine", "imipramine",
            "doxepin", "desipramine", "trimipramine",
            // PDE5
            "sildenafil", "tadalafil", "vardenafil",
            // 抗精神病 (部分)
            "chlorpromazine", "clozapine", "quetiapine", "risperidone",
            "olanzapine"
        )
    }

    // ============================================================
    // 规则 5：治疗窗偏离
    // ============================================================

    internal fun checkTherapeuticWindow(
        drugs: List<Drug>,
        curves: Map<String, PkCurve>
    ): List<TherapeuticWindowBreach> {
        return drugs.mapNotNull { drug ->
            val window = drug.therapeuticWindow ?: return@mapNotNull null
            val curve = curves[drug.id] ?: return@mapNotNull null
            val estimated = curve.cAvg  // 用曲线下面积平均浓度

            TherapeuticWindowBreach(
                drug = drug,
                estimatedConcentration = estimated,
                windowLow = window.low,
                windowHigh = window.high,
                unit = window.unit
            ).takeIf { it.status != TherapeuticWindowBreach.Status.IN_WINDOW }
        }
    }

    // ============================================================
    // 规则 6：肾剂量调整
    // ============================================================

    internal fun checkRenalAdjustment(
        drugs: List<Drug>,
        patient: PatientProfile
    ): List<Interaction> {
        val egfr = patient.egfr ?: return emptyList()
        if (egfr >= 60.0) return emptyList()  // 正常

        val renalTable = mapOf(
            "lithium" to RenalRule(contraindicatedBelow = 30.0, reduce50Below = 50.0, monitorBelow = 60.0),
            "metformin" to RenalRule(contraindicatedBelow = 30.0, reduce50Below = 45.0, monitorBelow = 60.0),
            "gabapentin" to RenalRule(contraindicatedBelow = 0.0, reduce50Below = 30.0, monitorBelow = 60.0),
            "valproic_acid" to RenalRule(contraindicatedBelow = 0.0, reduce50Below = 30.0, monitorBelow = 60.0)
        )

        return drugs.mapNotNull { drug ->
            val rule = renalTable[drug.id] ?: return@mapNotNull null
            when {
                rule.contraindicatedBelow > 0 && egfr < rule.contraindicatedBelow ->
                    RenalInteraction(drug, egfr, Severity.CONTRAINDICATED,
                        "eGFR 低于 ${rule.contraindicatedBelow}，禁用")
                egfr < rule.reduce50Below ->
                    RenalInteraction(drug, egfr, Severity.HIGH,
                        "eGFR 较低，建议剂量减半")
                egfr < rule.monitorBelow ->
                    RenalInteraction(drug, egfr, Severity.MEDIUM,
                        "eGFR 下降，监测肾功")
                else -> null
            }
        }
    }

    // ============================================================
    // 规则 7：肝剂量调整
    // ============================================================

    internal fun checkHepaticAdjustment(
        drugs: List<Drug>,
        patient: PatientProfile
    ): List<Interaction> {
        val cp = patient.childPugh ?: return emptyList()
        if (cp == ChildPugh.A) return emptyList()

        return drugs.mapNotNull { drug ->
            val needsReduction = when (cp) {
                ChildPugh.B -> drug.adjustments.hepatic == HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B
                ChildPugh.C -> drug.adjustments.hepatic in listOf(
                    HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B,
                    HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_C
                )
                else -> false
            }
            if (!needsReduction) return@mapNotNull null

            HepaticInteraction(drug, cp.toString(), Severity.HIGH,
                "Child-Pugh $cp，建议剂量减半")
        }
    }

    // ============================================================
    // 规则 8 (v0.5 新增): 中枢抑制叠加 (Sedation Synergy)
    // ============================================================
    //
    // 评分: 累加每个药的 sedation AE 字段
    // LOW=0, MEDIUM=1, HIGH=2, VERY_HIGH=3
    // 阈值: 总分 ≥3 触发, 药数 ≥2 触发 (取更严)
    //
    // 适用药: BZD, Z-drug, 阿片, 抗精神病, 抗癫痫, 巴比妥, 抗组胺, 肌松剂
    internal fun checkSedationSynergy(drugs: List<Drug>): SedationSynergyInteraction? {
        val sedating = drugs.filter { it.adverseEffects.sedation != RiskLevel.LOW && it.adverseEffects.sedation != RiskLevel.VERY_LOW }
        if (sedating.size < 2) return null
        val score = sedating.sumOf { d ->
            when (d.adverseEffects.sedation) {
                RiskLevel.VERY_HIGH -> 3
                RiskLevel.HIGH -> 2
                RiskLevel.MEDIUM -> 1
                else -> 0
            }
        }
        if (score < 3) return null
        return SedationSynergyInteraction(
            contributors = sedating,
            totalScore = score,
            drugCount = sedating.size
        )
    }

    // ============================================================
    // 规则 9 (v0.5 新增): 出血风险叠加 (Bleeding Risk)
    // ============================================================
    //
    // 累加出血风险药:
    // - 抗凝 (华法林/DOAC/肝素): 3 分
    // - 抗血小板 (阿司匹林/氯吡格雷): 2 分
    // - SSRI/SNRI: 2 分 (5-HT 抑制血小板聚集)
    // - NSAID: 2 分 (TXA2 抑制)
    // - 抗真菌唑类: 1 分 (CYP 抑制 → 华法林效应↑)
    // 阈值: ≥3 触发
    internal fun checkBleedingRisk(drugs: List<Drug>): BleedingRiskInteraction? {
        val score = drugs.sumOf { drug ->
            val id = drug.id
            when {
                id in ANTICOAGULANT_IDS -> 3
                id in ANTIPLATELET_IDS -> 2
                id in SSRI_SNRI_IDS -> 2
                id in NSAID_IDS -> 2
                id in AZOLE_ANTIFUNGAL_IDS -> 1
                else -> 0
            }
        }
        if (score < 3) return null
        val contributors = drugs.filter { drug ->
            drug.id in ANTICOAGULANT_IDS ||
            drug.id in ANTIPLATELET_IDS ||
            drug.id in SSRI_SNRI_IDS ||
            drug.id in NSAID_IDS ||
            drug.id in AZOLE_ANTIFUNGAL_IDS
        }
        return BleedingRiskInteraction(contributors = contributors, totalScore = score)
    }

    // ============================================================
    // 规则 10 (v0.5 新增): 多巴胺 D2 拮抗叠加 (EPS)
    // ============================================================
    //
    // 抗精神病药 D2 阻滞强度 (Stahl 分类):
    // - STRONG: 氟哌啶醇、利培酮、帕利哌酮、齐拉西酮、阿米舒必利
    // - MODERATE: 奥氮平、喹硫平中等剂量、阿塞那平、伊潘立酮、鲁拉西酮
    // - WEAK: 喹硫平低剂量、氯氮平
    //
    // 多药联用 (≥2) 显著增加 EPS, 高催乳素, TD
    internal fun checkDopamineAntagonism(drugs: List<Drug>): DopamineAntagonismInteraction? {
        val antipsychotics = drugs.filter { it.id in ANTIPSYCHOTIC_IDS }
        if (antipsychotics.size < 2) return null
        val strongest = when {
            antipsychotics.any { it.id in STRONG_D2_IDS } -> "STRONG"
            antipsychotics.any { it.id in MODERATE_D2_IDS } -> "MODERATE"
            else -> "WEAK"
        }
        return DopamineAntagonismInteraction(
            contributors = antipsychotics,
            drugCount = antipsychotics.size,
            strongestBlockade = strongest
        )
    }

    // ============================================================
    // 规则 11 (v0.5 新增): 低血压叠加 (Hypotension)
    // ============================================================
    //
    // α 阻滞 + 抗高血压 + 三环类抗抑郁 (α 阻滞作用) + PDE5 + 利尿剂
    // 直立性低血压/晕厥/跌倒 (髋部骨折)
    internal fun checkHypotensionRisk(drugs: List<Drug>): HypotensionRiskInteraction? {
        val count = drugs.count { drug ->
            drug.id in HYPOTENSIVE_IDS
        }
        if (count < 3) return null
        val contributors = drugs.filter { it.id in HYPOTENSIVE_IDS }
        return HypotensionRiskInteraction(contributors = contributors, drugCount = count)
    }
}

/**
 * 给规则引擎用的"用户在用药物"简化结构
 *
 * （完整 UserDrug 在 data 层定义；这里只取规则引擎关心的字段。）
 */
data class UserDrugForRule(
    val drugCatalogId: String,
    val doseMg: Double,
    val frequencyPerDay: Double
)

/**
 * 肾剂量规则
 */
data class RenalRule(
    val contraindicatedBelow: Double,
    val reduce50Below: Double,
    val monitorBelow: Double
)

/**
 * 肾剂量调整交互
 */
data class RenalInteraction(
    val drug: Drug,
    val egfr: Double,
    override val severity: Severity,
    val message: String
) : Interaction {
    override val title: String = "${drug.genericNameZh}：肾功能调整"
    override val detail: String = "eGFR = $egfr mL/min/1.73m²。"
    override val advice: String = message
    override val references: List<String> = drug.references
    override val signature: String = "RENAL|${drug.id}"
}

/**
 * 肝剂量调整交互
 */
data class HepaticInteraction(
    val drug: Drug,
    val childPugh: String,
    override val severity: Severity,
    val message: String
) : Interaction {
    override val title: String = "${drug.genericNameZh}：肝功能调整"
    override val detail: String = "Child-Pugh 分级 = $childPugh。"
    override val advice: String = message
    override val references: List<String> = drug.references
    override val signature: String = "HEP|${drug.id}"
}
