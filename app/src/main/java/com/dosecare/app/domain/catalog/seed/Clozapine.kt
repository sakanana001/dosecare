package com.dosecare.app.domain.catalog.seed

import com.dosecare.app.domain.catalog.*
import com.dosecare.app.domain.pk.PkModel

/**
 * 氯氮平（Clozapine）种子数据
 *
 * 选它做示例的原因：
 * 1. 精神科最有代表性的"高警戒药"
 * 2. 治疗窗窄（350-600 ng/mL）
 * 3. CYP1A2 主代谢，吸烟影响巨大
 * 4. 经典致命相互作用：氟伏沙明、咖啡因、卡马西平
 * 5. 严重不良反应（粒缺、代谢综合征、心肌炎）
 * 6. PK 数据成熟、文献丰富
 *
 * 数据来源：
 * - FDA Clozapine Prescribing Information (2024)
 * - AGNP Consensus 2017 (Therapeutic Drug Monitoring)
 * - DJ Touw et al., Ther Drug Monit 2020
 * - 国内药品说明书
 */
object ClozapineSeed {

    val drug: Drug = Drug(
        id = "clozapine",
        genericName = "Clozapine",
        genericNameZh = "氯氮平",
        brandNames = listOf("Clozaril", "Leponex", "FazaClo"),
        category = DrugCategory.ANTIPSYCHOTIC,
        subcategory = "ATYPICAL_DIBENZODIAZEPINE",
        atc = "N05AH02",

        pkModel = PkModel.OneCompartmentWithAbsorption(
            f = 0.6,                          // 60% 口服生物利用度
            kaPerHour = 1.5,                  // Tmax ~2h
            kePerHour = 0.0495,               // t½ ≈ 14h
            vdLiters = 5.4 * 70.0             // 5.4 L/kg × 70 kg = 378 L
        ),

        forms = listOf(
            DrugForm(
                route = PkModel.Route.ORAL,
                f = 0.6,
                kaPerHour = 1.5,
                tMaxHours = 2.0,
                doseUnits = listOf("mg"),
                commonDoseRangeMg = 12.5 to 900.0
            )
        ),

        cypProfile = CypProfile(
            substrates = listOf(
                CypContribution(CypEnzyme.CYP1A2, 0.70),
                CypContribution(CypEnzyme.CYP3A4, 0.20),
                CypContribution(CypEnzyme.CYP2C19, 0.10)
            ),
            inhibitors = emptyList(),
            inducers = emptyList()
        ),

        therapeuticWindow = PkModel.TherapeuticWindow(
            low = 350.0,
            high = 600.0,
            unit = "ng/mL",
            guidelineSource = "AGNP 2017"
        ),

        activeMetabolites = listOf(
            Metabolite(
                id = "norclozapine",
                name = "Norclozapine (N-desmethylclozapine)",
                activityRatio = 0.5,
                note = "活性代谢物，5-HT2A > D2 亲和力；"
                     + "原药 + 代谢物比值是临床重要参考"
            )
        ),

        adverseEffects = AdverseEffects(
            qtcProlongation = RiskLevel.MEDIUM,
            metabolicSyndrome = RiskLevel.HIGH,
            anticholinergicLoad = 3,
            agranulocytosis = RiskLevel.HIGH,         // 1% 风险
            extrapyramidal = RiskLevel.VERY_LOW,
            sedation = RiskLevel.HIGH,
            sexual = RiskLevel.MEDIUM,
            hyperprolactinemia = RiskLevel.LOW
        ),

        adjustments = DoseAdjustments(
            renal = RenalAdjustment.NONE,
            hepatic = HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B,
            elderly = "起始低剂量，缓慢滴定；老年易体位性低血压",
            smoking = SmokingEffect(
                effect = "CYP1A2 强诱导",
                doseAdjustment = "吸烟者剂量需增加约 50%",
                abstinenceNote = "戒烟后 1 周内 CYP1A2 活性下降，剂量需减约 30-50%；"
                               + "否则可能中毒"
            )
        ),

        criticalInteractions = listOf(
            CriticalInteractionHint(
                triggerDrugId = "fluvoxamine",
                mechanism = Mechanism.CYP_INHIBITION_STRONG,
                aucFoldChange = 5.0 to 10.0,
                severity = "CONTRAINDICATED",
                clinicalNote = "氟伏沙明是 CYP1A2 强抑制剂，"
                             + "可使氯氮平 AUC 升高 5-10 倍，"
                             + "可能致死。需联用时氯氮平剂量减至 1/5-1/10，"
                             + "并密切 TDM 监测。"
            ),
            CriticalInteractionHint(
                triggerDrugId = "carbamazepine",
                mechanism = Mechanism.CYP_INDUCTION,
                aucFoldChange = 0.5 to 0.5,        // 诱导使浓度降低
                severity = "CONTRAINDICATED",
                clinicalNote = "卡马西平是 CYP3A4 强诱导剂，"
                             + "且与氯氮平联用增加粒缺风险（协同骨髓抑制）。"
                             + "应避免联用。"
            ),
            CriticalInteractionHint(
                triggerDrugId = "caffeine_high",
                mechanism = Mechanism.CYP_INHIBITION_MODERATE,
                aucFoldChange = 1.3 to 1.5,
                severity = "MEDIUM",
                clinicalNote = "高咖啡因摄入（>400mg/d）抑制 CYP1A2，"
                             + "可使氯氮平浓度升高。"
            )
        ),

        monitoring = MonitoringRequirements(
            frequency = "WEEKLY_FIRST_18_MONTHS_THEN_MONTHLY",
            items = listOf(
                "WBC（白细胞）",
                "ANC（中性粒细胞绝对值）",
                "空腹血糖",
                "血脂",
                "体重 / BMI",
                "氯氮平血药浓度（治疗稳态后）",
                "静息心电图（QTc）"
            )
        ),

        references = listOf(
            "PMID: 10210627 (Clozapine-fluvoxamine interaction)",
            "PMID: 29310193 (AGNP 2017 consensus)",
            "FDA Clozapine Prescribing Information 2024",
            "DJ Touw et al., Ther Drug Monit 2020"
        )
    )
}
