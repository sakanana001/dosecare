package com.dosecare.app.domain.rules

import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.CypEnzyme

/**
 * 相互作用类型
 *
 * 一条 Interaction 是规则引擎的输出。包含严重度、机制、临床建议、引用。
 */
sealed interface Interaction {
    val severity: Severity
    val title: String
    val detail: String
    val advice: String
    val references: List<String>

    /** 用于去重（同一规则同次评估只出现一次） */
    val signature: String
}

/**
 * 严重度等级
 */
enum class Severity(val rank: Int, @androidx.annotation.StringRes val displayNameRes: Int, val displayName: String, val color: String) {
    CONTRAINDICATED(5, com.dosecare.app.R.string.sevrule_contraindicated, "禁忌", "#B71C1C"),
    HIGH(4, com.dosecare.app.R.string.sevrule_high, "高度警示", "#EF6C00"),
    MEDIUM(3, com.dosecare.app.R.string.sevrule_medium, "中度警示", "#FBC02D"),
    LOW(2, com.dosecare.app.R.string.sevrule_low, "轻度提示", "#9E9E9E"),
    INFO(1, com.dosecare.app.R.string.sevrule_info, "信息", "#E0E0E0");

    companion object {
        fun fromAucFold(fold: Double): Severity = when {
            fold >= 5.0 -> CONTRAINDICATED
            fold >= 2.0 -> HIGH
            fold >= 1.25 -> MEDIUM
            fold > 1.0 -> LOW
            else -> INFO
        }

        /** 逆 AUC（诱导使浓度降低） */
        fun fromInductionFold(fold: Double): Severity = when {
            fold <= 0.2 -> CONTRAINDICATED  // 浓度降至 1/5 以下 = 失效
            fold <= 0.5 -> HIGH
            fold <= 0.8 -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * 机制类型
 */
enum class Mechanism {
    CYP_INHIBITION_STRONG,
    CYP_INHIBITION_MODERATE,
    CYP_INHIBITION_WEAK,
    CYP_INDUCTION,
    QTc_ADDITIVE,
    SEROTONIN_ADDITIVE,
    ANTICHOLINERGIC_ADDITIVE,
    PHARMACODYNAMIC,
    PHARMACOKINETIC_OTHER,
    OTHER
}

/**
 * CYP 代谢相互作用
 */
data class CypInteraction(
    val perpetrator: Drug,         // 触发药（抑制剂/诱导剂）
    val victim: Drug,              // 受影响药（底物）
    val cyp: CypEnzyme,
    val mechanism: Mechanism,
    val aucFoldChange: Double,
    val patientAdjustedFold: Double
) : Interaction {
    override val severity: Severity =
        if (mechanism == Mechanism.CYP_INDUCTION)
            Severity.fromInductionFold(patientAdjustedFold)
        else
            Severity.fromAucFold(patientAdjustedFold)

    override val title: String =
        "${perpetrator.genericNameZh} + ${victim.genericNameZh}：" +
        "${if (mechanism == Mechanism.CYP_INDUCTION) "酶诱导" else "酶抑制"}"

    override val detail: String =
        "${perpetrator.genericNameZh} " +
        "${if (mechanism == Mechanism.CYP_INDUCTION) "诱导" else "抑制"} " +
        "${cyp.displayName}，" +
        "可能使 ${victim.genericNameZh} 血药浓度" +
        "${if (mechanism == Mechanism.CYP_INDUCTION) "降低" else "升高"}" +
        "约 ${"%.1f".format(patientAdjustedFold)} 倍（AUC）。"

    override val advice: String =
        if (severity == Severity.CONTRAINDICATED)
            "建议避免联用。如必须联用，${victim.genericNameZh} 剂量需大幅调整并做 TDM。"
        else if (severity == Severity.HIGH)
            "需要医师评估是否换药或大幅减量，并做 TDM。"
        else if (severity == Severity.MEDIUM)
            "可能需要剂量调整，加强监测。"
        else
            "临床影响较小，保持监测即可。"

    override val references: List<String> = victim.references

    override val signature: String =
        "CYP|${perpetrator.id}|${victim.id}|${cyp.name}"
}

/**
 * QTc 累积风险
 */
data class QtcInteraction(
    val contributors: List<Drug>,
    val totalQtcProlongationMs: Double,
    val finalEstimatedQtcMs: Double
) : Interaction {
    override val severity: Severity = when {
        finalEstimatedQtcMs >= 500.0 -> Severity.CONTRAINDICATED
        finalEstimatedQtcMs >= 480.0 -> Severity.HIGH
        finalEstimatedQtcMs >= 460.0 -> Severity.MEDIUM
        finalEstimatedQtcMs >= 440.0 -> Severity.LOW
        else -> Severity.INFO
    }

    override val title: String = "QTc 累积延长风险"

    override val detail: String =
        "当前用药组合可能使 QTc 延长约 ${"%.1f".format(totalQtcProlongationMs)} ms，" +
        "估算基线后约 ${"%.0f".format(finalEstimatedQtcMs)} ms。"

    override val advice: String =
        if (severity == Severity.CONTRAINDICATED)
            "应立即评估，必要时减量或停药。"
        else if (severity == Severity.HIGH)
            "建议心内科会诊，评估停用其中延长 QT 的药物。"
        else
            "监测心电图，关注电解质（钾、镁）。"

    override val references: List<String> = listOf(
        "FDA QTc Guidance 2015",
        "CredibleMeds QTdrugs List"
    )

    override val signature: String =
        "QTc|${contributors.map { it.id }.sorted().joinToString(",")}"
}

/**
 * 抗胆碱能负荷
 */
data class AnticholinergicLoadInteraction(
    val contributors: List<Drug>,
    val totalScore: Int
) : Interaction {
    override val severity: Severity = when {
        totalScore >= 6 -> Severity.HIGH
        totalScore >= 4 -> Severity.MEDIUM
        totalScore >= 2 -> Severity.LOW
        else -> Severity.INFO
    }

    override val title: String = "抗胆碱能负荷过高"

    override val detail: String =
        "当前用药组合抗胆碱能负荷评分 = $totalScore，" +
        "可能引起口干、便秘、视物模糊、认知下降、谵妄。"

    override val advice: String =
        if (totalScore >= 4)
            "考虑替换评分较高的药物为同等疗效但低抗胆碱能负担的药物。"
        else
            "注意观察相关症状。"

    override val references: List<String> = listOf(
        "ACB Scale (Anticholinergic Cognitive Burden)",
        "PMID: 18332297 (Anticholinergic burden and cognitive decline)"
    )

    override val signature: String =
        "ACH|${contributors.map { it.id }.sorted().joinToString(",")}"
}

/**
 * 5-HT 综合征风险
 */
data class SerotoninSyndromeRisk(
    val serotonergicDrugs: List<Drug>,
    val maxSeverity: Severity
) : Interaction {
    override val severity: Severity = maxSeverity

    override val title: String = "5-HT 综合征风险"

    override val detail: String =
        "以下药物均增加 5-HT 活性，联用可能引发 5-HT 综合征" +
        "（激越、肌阵挛、反射亢进、出汗、发热，可致死）。" +
        "涉及：${serotonergicDrugs.joinToString { it.genericNameZh }}"

    override val advice: String = when (severity) {
        Severity.CONTRAINDICATED -> "禁止联用。"
        Severity.HIGH -> "禁止联用或仅在专科医师指导下使用。"
        else -> "注意早期症状，监测。"
    }

    override val references: List<String> = listOf(
        "Sternbach H, Am J Psychiatry 1991",
        "FDA MAOI Class Labeling"
    )

    override val signature: String =
        "5HT|${serotonergicDrugs.map { it.id }.sorted().joinToString(",")}"
}

/**
 * 治疗窗偏离
 */
data class TherapeuticWindowBreach(
    val drug: Drug,
    val estimatedConcentration: Double,
    val windowLow: Double,
    val windowHigh: Double,
    val unit: String
) : Interaction {
    val status: Status

    init {
        status = when {
            estimatedConcentration < windowLow -> Status.BELOW
            estimatedConcentration > windowHigh -> Status.ABOVE
            else -> Status.IN_WINDOW
        }
    }

    enum class Status { BELOW, IN_WINDOW, ABOVE }

    override val severity: Severity = when (status) {
        Status.ABOVE -> Severity.HIGH
        Status.BELOW -> Severity.MEDIUM
        Status.IN_WINDOW -> Severity.INFO
    }

    override val title: String = "${drug.genericNameZh}：血药浓度${when (status) {
        Status.ABOVE -> "超治疗窗"
        Status.BELOW -> "低于治疗窗"
        Status.IN_WINDOW -> "在治疗窗内"
    }}"

    override val detail: String =
        "估算浓度 ${"%.1f".format(estimatedConcentration)} $unit，" +
        "治疗窗 $windowLow - $windowHigh $unit。"

    override val advice: String = when (status) {
        Status.ABOVE -> "立即联系医师评估减量或停药。"
        Status.BELOW -> "可能需要增加剂量，但务必遵医嘱。"
        Status.IN_WINDOW -> "保持当前方案。"
    }

    override val references: List<String> = drug.references

    override val signature: String = "WIN|${drug.id}"
}

/**
 * 中枢抑制叠加 (Sedation Synergy)
 *
 * BZD + 阿片 + 抗精神病 + 抗癫痫 + 巴比妥 + 抗组胺 + 酒精 → 镇静/呼吸抑制叠加
 * FDA 黑框: BZD + 阿片类联用
 * Beers 准则: 多种 CNS 抑制联用增加跌倒/谵妄/呼吸抑制风险
 */
data class SedationSynergyInteraction(
    val contributors: List<Drug>,
    val totalScore: Int,
    val drugCount: Int
) : Interaction {
    override val severity: Severity = when {
        drugCount >= 3 -> Severity.HIGH
        drugCount == 2 && totalScore >= 4 -> Severity.HIGH
        drugCount == 2 -> Severity.MEDIUM
        else -> Severity.LOW
    }

    override val title: String = "中枢抑制叠加 (Sedation)"

    override val detail: String =
        "当前用药组合含 $drugCount 个具镇静/呼吸抑制作用的药,累计镇静评分 $totalScore 分。" +
        "联用显著增加深度镇静、呼吸抑制、跌倒、谵妄风险(尤其老年人)。"

    override val advice: String =
        if (severity == Severity.HIGH) "建议医师评估联用必要性,优先选无镇静替代药;若必须联用,最低有效剂量、最短疗程,备纳洛酮。"
        else if (severity == Severity.MEDIUM) "减少不必要联用;监测过度镇静;避免驾驶/操作机械。"
        else "注意观察日间嗜睡。"

    override val references: List<String> = listOf(
        "FDA Black Box: BZD + Opioid (2016)",
        "Beers Criteria 2023 (AGS)",
        "AGNP 2017"
    )

    override val signature: String =
        "SED|${contributors.map { it.id }.sorted().joinToString(",")}"
}

/**
 * 出血风险叠加 (Bleeding Risk)
 *
 * 抗凝 (华法林/DOAC) + 抗血小板 (阿司匹林/氯吡格雷) + NSAID + SSRI + 抗真菌(唑类)
 * 联用 → 出血风险增加
 * HAS-BLED 评分升高
 */
data class BleedingRiskInteraction(
    val contributors: List<Drug>,
    val totalScore: Int
) : Interaction {
    override val severity: Severity = when {
        totalScore >= 5 -> Severity.HIGH
        totalScore >= 3 -> Severity.MEDIUM
        else -> Severity.LOW
    }

    override val title: String = "出血风险叠加 (Bleeding)"

    override val detail: String =
        "当前用药组合含 $totalScore 种影响凝血/血小板功能的药。SSRI/SNRI 抑制血小板 5-HT 摄取;NSAID 抑制 TXA2;抗凝直接抑制凝血级联。" +
        "联用显著增加上消化道/颅内出血风险;与 HAS-BLED 评分独立相关。"

    override val advice: String =
        if (severity == Severity.HIGH) "建议加用 PPI 保护胃黏膜;监测大便潜血、血红蛋白、INR(若用华法林);重新评估联用必要性。"
        else if (severity == Severity.MEDIUM) "考虑最低有效剂量;评估胃肠道保护。"
        else "注意观察黑便/牙龈出血/皮下瘀斑。"

    override val references: List<String> = listOf(
        "HAS-BLED Score 2010",
        "ACG Guidelines GI Bleeding 2020",
        "FDA Bleeding Risk with NSAIDs + Anticoagulants"
    )

    override val signature: String =
        "BLEED|${contributors.map { it.id }.sorted().joinToString(",")}"
}

/**
 * 多巴胺 D2 拮抗叠加 (EPS Risk)
 *
 * 抗精神病 (强 D2 阻滞) 多药联用 → EPS 风险叠加
 * 急性肌张力障碍/静坐不能/帕金森综合征/迟发性运动障碍
 */
data class DopamineAntagonismInteraction(
    val contributors: List<Drug>,
    val drugCount: Int,
    val strongestBlockade: String
) : Interaction {
    override val severity: Severity = when {
        drugCount >= 3 -> Severity.HIGH
        drugCount == 2 && strongestBlockade == "STRONG" -> Severity.HIGH
        drugCount == 2 -> Severity.MEDIUM
        else -> Severity.LOW
    }

    override val title: String = "多巴胺 D2 拮抗叠加 (EPS)"

    override val detail: String =
        "当前用药含 $drugCount 个多巴胺 D2 受体拮抗剂(抗精神病/止吐)。多药联用显著增加 EPS 风险:" +
        "急性肌张力障碍、静坐不能、药源性帕金森综合征、迟发性运动障碍(TD);高催乳素血症、月经紊乱、性功能障碍亦叠加。"

    override val advice: String =
        if (severity == Severity.HIGH) "建议尽量单药抗精神病;若必须联用,选 D2 亲和力低药(喹硫平/氯氮平);定期 AIMS 评分评估 TD。"
        else if (severity == Severity.MEDIUM) "监测 EPS 症状(震颤/肌强直/静坐不能);高催乳素血症症状。"
        else "注意细微 EPS 表现。"

    override val references: List<String> = listOf(
        "Marder 2019 - Antipsychotic Polypharmacy",
        "AIMS (Abnormal Involuntary Movement Scale)",
        "AGNP 2017"
    )

    override val signature: String =
        "DOPA|${contributors.map { it.id }.sorted().joinToString(",")}"
}

/**
 * 低血压叠加 (Hypotension Risk)
 *
 * α 阻滞 + 抗高血压 (ACEI/ARB/CCB/β-blocker/利尿剂) + 抗抑郁(三环) + PDE5
 * 联用 → 直立性低血压/晕厥/跌倒
 * 老年人尤其危险(髋部骨折)
 */
data class HypotensionRiskInteraction(
    val contributors: List<Drug>,
    val drugCount: Int
) : Interaction {
    override val severity: Severity = when {
        drugCount >= 4 -> Severity.HIGH
        drugCount == 3 -> Severity.MEDIUM
        else -> Severity.LOW
    }

    override val title: String = "低血压叠加 (Hypotension)"

    override val detail: String =
        "当前用药含 $drugCount 个具降压/血管扩张作用的药。联用增加直立性低血压、晕厥、跌倒风险。" +
        "尤其老年人(自主神经反射减弱)和高剂量三环类抗抑郁。"

    override val advice: String =
        if (severity == Severity.HIGH) "建议监测卧立位血压;避免快速起身;防跌倒教育;评估减药。"
        else if (severity == Severity.MEDIUM) "监测血压(尤其首剂);夜间起床慢;避免驾驶。"
        else "注意起身头晕。"

    override val references: List<String> = listOf(
        "AGS Beers Criteria 2023",
        "JNC 8 Hypertension Guidelines"
    )

    override val signature: String =
        "HYPOT|${contributors.map { it.id }.sorted().joinToString(",")}"
}
