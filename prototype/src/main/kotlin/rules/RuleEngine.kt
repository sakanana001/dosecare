package rules

import catalog.CypEnzyme
import catalog.Drug
import catalog.DrugCatalogService
import catalog.EffectStrength
import catalog.Mechanism as CatalogMechanism
import patient.PatientProfile
import pk.PkCurve

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

        results += checkCypInteractions(drugs, patient)
        results += checkQtcAdditive(drugs, patient)
        results += checkAnticholinergicLoad(drugs)
        results += checkSerotoninSyndrome(drugs)
        results += checkTherapeuticWindow(drugs, curves)
        results += checkRenalAdjustment(drugs, patient)
        results += checkHepaticAdjustment(drugs, patient)

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
        if (patient.childPugh == "B") fold *= 1.3
        if (patient.childPugh == "C") fold *= 1.6
        // CYP 多态性
        if (patient.cyp2D6Genotype == "PM" && baseFold > 1.0) fold *= 1.2
        if (patient.cyp2C19Genotype == "PM" && baseFold > 1.0) fold *= 1.2
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
        val femaleBonus = if (patient.biologicalSex == "F") 10.0 else 0.0
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
        if (cp == "A") return emptyList()

        return drugs.mapNotNull { drug ->
            val needsReduction = when (cp) {
                "B" -> drug.adjustments.hepatic == catalog.HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B
                "C" -> drug.adjustments.hepatic in listOf(
                    catalog.HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B,
                    catalog.HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_C
                )
                else -> false
            }
            if (!needsReduction) return@mapNotNull null

            HepaticInteraction(drug, cp, Severity.HIGH,
                "Child-Pugh $cp，建议剂量减半")
        }
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
