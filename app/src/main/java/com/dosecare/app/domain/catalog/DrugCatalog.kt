package com.dosecare.app.domain.catalog

import com.dosecare.app.domain.pk.PkModel
import com.dosecare.app.domain.pk.PkModel.Route

/**
 * 药物目录（DrugCatalog）数据模型
 *
 * 这是从 assets/drugs/v1.json 反序列化出来的内存对象。
 * 启动时一次性加载到内存（DrugCatalogService），后续访问 O(1)。
 *
 * 数据源：
 * - DrugBank (学术许可)
 * - OpenFDA / DailyMed
 * - AGNP 共识
 * - Flockhart CYP Table
 * - 国内药品说明书
 *
 * ⚠️ 任何字段错误都可能导致用户决策偏差。所有 critical interactions
 *    字段在发布前必须经过临床药师 review（见 docs/05-DRUG_CATALOG.md）。
 */

/**
 * 药物分类（顶层）
 */
enum class DrugCategory(@androidx.annotation.StringRes val displayNameRes: Int, val displayName: String) {
    // 精神神经
    ANTIPSYCHOTIC(com.dosecare.app.R.string.drug_cat_antipsychotic, "抗精神病药"),
    MOOD_STABILIZER(com.dosecare.app.R.string.drug_cat_mood_stabilizer, "心境稳定剂"),
    ANTIDEPRESSANT(com.dosecare.app.R.string.drug_cat_antidepressant, "抗抑郁药"),
    ANXIOLYTIC(com.dosecare.app.R.string.drug_cat_anxiolytic, "抗焦虑/苯二氮䓬类"),
    STIMULANT(com.dosecare.app.R.string.drug_cat_stimulant, "ADHD 兴奋剂"),
    ANTICHOLINERGIC(com.dosecare.app.R.string.drug_cat_anticholinergic, "抗胆碱能"),
    ANTIPARKINSONIAN(com.dosecare.app.R.string.drug_cat_antiparkinsonian, "抗帕金森"),
    ANTIEPILEPTIC(com.dosecare.app.R.string.drug_cat_antiepileptic, "抗癫痫"),
    ALZHEIMERS(com.dosecare.app.R.string.drug_cat_alzheimers, "抗痴呆/认知改善"),
    ANTIMIGRAINE(com.dosecare.app.R.string.drug_cat_antimigraine, "抗偏头痛"),
    MUSCLE_RELAXANT(com.dosecare.app.R.string.drug_cat_muscle_relaxant, "肌松剂"),
    ANESTHETIC(com.dosecare.app.R.string.drug_cat_anesthetic, "麻醉 (局麻/全麻)"),
    ANTIVERTIGO(com.dosecare.app.R.string.drug_cat_antivertigo, "抗眩晕"),
    // 内科
    ANTIDIABETIC(com.dosecare.app.R.string.drug_cat_antidiabetic, "降糖药"),
    THYROID(com.dosecare.app.R.string.drug_cat_thyroid, "甲状腺"),
    CORTICOSTEROID(com.dosecare.app.R.string.drug_cat_corticosteroid, "肾上腺皮质激素"),
    ANTIHYPERTENSIVE(com.dosecare.app.R.string.drug_cat_antihypertensive, "降压药"),
    STATIN(com.dosecare.app.R.string.drug_cat_statin, "调脂药"),
    ANTIARRHYTHMIC(com.dosecare.app.R.string.drug_cat_antiarrhythmic, "抗心律失常"),
    ANTICOAGULANT(com.dosecare.app.R.string.drug_cat_anticoagulant, "抗凝/抗血小板"),
    PPI(com.dosecare.app.R.string.drug_cat_ppi, "质子泵抑制剂 (PPI)"),
    OSTEOPOROSIS_DRUG(com.dosecare.app.R.string.drug_cat_osteoporosis_drug, "骨质疏松药"),
    GOUT(com.dosecare.app.R.string.drug_cat_gout, "抗痛风"),
    BPH_AGENT(com.dosecare.app.R.string.drug_cat_bph_agent, "前列腺增生药"),
    HORMONE_REPLACEMENT(com.dosecare.app.R.string.drug_cat_hormone_replacement, "激素替代"),
    BRONCHODILATOR(com.dosecare.app.R.string.drug_cat_bronchodilator, "支气管扩张剂"),
    // 疼痛
    ANALGESIC(com.dosecare.app.R.string.drug_cat_analgesic, "止痛药"),
    // 抗感染
    ANTIHISTAMINE(com.dosecare.app.R.string.drug_cat_antihistamine, "抗组胺"),
    ANTIBIOTIC(com.dosecare.app.R.string.drug_cat_antibiotic, "抗感染 (抗真菌/抗结核/抗 HIV)"),
    // 其他
    SUBSTANCE_USE(com.dosecare.app.R.string.drug_cat_substance_use, "物质依赖治疗"),
    SUPPLEMENT(com.dosecare.app.R.string.drug_cat_supplement, "营养补充剂"),
    HERBAL(com.dosecare.app.R.string.drug_cat_herbal, "草本补充剂"),
    OTHER(com.dosecare.app.R.string.drug_cat_other, "其他")
}

/**
 * 关键不良反应等级
 */
enum class RiskLevel { VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH }

/**
 * 单个药物的完整定义
 */
data class Drug(
    val id: String,                       // 英文小写下划线
    val genericName: String,              // INN 英文
    val genericNameZh: String,            // 中文通用名
    val brandNames: List<String> = emptyList(),
    val category: DrugCategory,
    val subcategory: String? = null,      // "ATYPICAL", "SSRI", "TCA" ...
    val atc: String? = null,              // "N05AH02"

    val pkModel: PkModel,
    val forms: List<DrugForm>,

    /**
     * 按主治症状/疾病的分类 (一个药可归多个)
     * 用于"按主治症状"视图, 与 category (按药理学分类) 并列
     */
    val indicationGroups: List<IndicationGroup> = emptyList(),

    val cypProfile: CypProfile = CypProfile(),

    val therapeuticWindow: PkModel.TherapeuticWindow? = null,

    /**
     * 浓度单位换算因子: cMax(mg/L) × factor = therapeuticWindow.unit 的数值
     * - 默认 1.0: 治疗窗单位 = mg/L (同 PkCurve 内部单位)
     * - 1000.0: 治疗窗单位 = ng/mL (精神科常用, 1 mg/L = 1000 ng/mL)
     * - 0.1441: 治疗窗单位 = mmol/L (锂 1 mmol/L = 6.94 mg/L)
     * - 0.001: 治疗窗单位 = μg/mL (= mg/L) — 已用默认 1.0
     */
    val cMaxUnitFactor: Double = 1.0,

    /**
     * 治疗窗"条带"是否不绘制 (用于 INR / ng/dL 这类非浓度单位, 数值跟 cMax 不可比)
     * true 时 chart 不画 win.low..win.high 背景带, 但 legend 仍显示文字
     */
    val skipTherapeuticWindowBand: Boolean = false,

    /** 蛋白结合率 %（0-100），来自说明书 */
    val proteinBindingPct: Int = 0,

    val activeMetabolites: List<Metabolite> = emptyList(),

    val adverseEffects: AdverseEffects = AdverseEffects(),

    /** 药物过量: 典型症状 / 严重度 / 估计中毒/致死剂量 / 抢救要点 / 特异性解毒剂 / 数据源 */
    val overdose: OverdoseInfo? = null,

    val adjustments: DoseAdjustments = DoseAdjustments(),

    val criticalInteractions: List<CriticalInteractionHint> = emptyList(),

    val monitoring: MonitoringRequirements = MonitoringRequirements(),

    /**
     * 药理作用 (一两句话简短描述): 主要作用机制 + 治疗作用
     * 例: "选择性 5-HT 再摄取抑制剂 (SSRI), 增强 5-HT 神经传递, 抗抑郁/抗焦虑"
     */
    val pharmacology: String? = null,

    val references: List<String> = emptyList()  // PMID / 说明书 / 共识指南
)

/** 药物过量严重度 */
enum class OverdoseSeverity(@androidx.annotation.StringRes val displayNameRes: Int, val displayName: String) {
    MILD(com.dosecare.app.R.string.sev_mild, "轻度"),
    MODERATE(com.dosecare.app.R.string.sev_moderate, "中度"),
    SEVERE(com.dosecare.app.R.string.sev_severe, "重度"),
    LIFE_THREATENING(com.dosecare.app.R.string.sev_life_threatening, "危及生命")
}

/** 药物过量信息 */
data class OverdoseInfo(
    val symptoms: String,                                // 典型症状描述
    val severity: OverdoseSeverity,                     // 严重度分级
    val toxicDoseEstimateMg: Double? = null,             // 中毒剂量估计 (mg, 70kg 成人)
    val fatalDoseEstimateMg: Double? = null,             // 致死剂量估计 (mg, 70kg 成人)
    val management: String,                              // 抢救要点
    val antidote: String? = null,                        // 特异性解毒剂 (e.g. 氟马西尼 for BZD)
    val dataSource: String                               // FDA DailyMed / 临床指南
)

/**
 * 给药剂型
 */
data class DrugForm(
    val route: Route,
    val f: Double,                        // 生物利用度（IV = 1.0）
    val kaPerHour: Double? = null,        // IV 无
    val tMaxHours: Double? = null,
    val doseUnits: List<String> = listOf("mg"),
    val commonDoseRangeMg: Pair<Double, Double>? = null
)

/**
 * 活性代谢物
 */
data class Metabolite(
    val id: String,
    val name: String,
    val activityRatio: Double = 1.0,      // 相对原药活性
    val note: String? = null
)

/**
 * 关键不良反应
 */
data class AdverseEffects(
    val qtcProlongation: RiskLevel = RiskLevel.LOW,
    val metabolicSyndrome: RiskLevel = RiskLevel.LOW,
    val anticholinergicLoad: Int = 0,     // 0-3（ACB scale）
    val agranulocytosis: RiskLevel = RiskLevel.VERY_LOW,
    val extrapyramidal: RiskLevel = RiskLevel.LOW,
    val sedation: RiskLevel = RiskLevel.LOW,
    val sexual: RiskLevel = RiskLevel.LOW,
    val hyperprolactinemia: RiskLevel = RiskLevel.LOW
)

/**
 * 剂量调整（肾/肝/年龄/吸烟）
 */
data class DoseAdjustments(
    val renal: RenalAdjustment = RenalAdjustment.NONE,
    val hepatic: HepaticAdjustment = HepaticAdjustment.NONE,
    val elderly: String? = null,
    val smoking: SmokingEffect? = null
)

enum class RenalAdjustment {
    NONE,
    MONITOR,
    REDUCE_50_PCT_EGFR_BELOW_30,
    CONTRAINDICATED_EGFR_BELOW_30
}
enum class HepaticAdjustment {
    NONE,
    MONITOR,
    REDUCE_50_PCT_CHILD_PUGH_B,
    REDUCE_50_PCT_CHILD_PUGH_C,
    CONTRAINDICATED_IN_SEVERE
}

data class SmokingEffect(
    val effect: String,                   // "CYP1A2 induction"
    val doseAdjustment: String,           // "INCREASE_DOSE_50_PCT_IN_SMOKERS"
    val abstinenceNote: String? = null    // 戒烟后 1 周内需要减量
)

/**
 * 关键相互作用提示（在 DrugCatalog 内的预置提示）
 *
 * 这只是 hint。实际的相互作用检测由 RuleEngine 在运行时计算。
 * 这里给"经典教科书级"的警示，方便在没有规则引擎时也能提示。
 */
data class CriticalInteractionHint(
    val triggerDrugId: String,            // 触发药的 id
    val mechanism: Mechanism,
    val aucFoldChange: Pair<Double, Double>,  // [low, high]
    val severity: String,                // CONTRAINDICATED / HIGH / MEDIUM
    val clinicalNote: String
)

enum class Mechanism {
    CYP_INHIBITION_STRONG,
    CYP_INHIBITION_MODERATE,
    CYP_INHIBITION_WEAK,
    CYP_INDUCTION,
    CYP2D6_COMPETITION,
    QTc_ADDITIVE,
    SEROTONIN_ADDITIVE,
    ANTICHOLINERGIC_ADDITIVE,
    PHARMACODYNAMIC,
    PHARMACOKINETIC_OTHER,
    OTHER
}

/**
 * 监测要求
 */
data class MonitoringRequirements(
    val frequency: String? = null,        // "WEEKLY_FIRST_18WEEKS_THEN_MONTHLY"
    val items: List<String> = emptyList() // ["WBC", "ANC", "血糖"]
)

/**
 * 药物目录服务（运行时）
 */
class DrugCatalogService(private val drugs: List<Drug>) {

    private val byId: Map<String, Drug> = drugs.associateBy { it.id }

    fun getById(id: String): Drug =
        byId[id] ?: throw NoSuchElementException("Drug not found: $id")

    fun tryGetById(id: String): Drug? = byId[id]

    fun all(): List<Drug> = drugs

    fun search(query: String, limit: Int = 20): List<Drug> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return drugs.take(limit)
        return drugs.asSequence()
            .filter {
                it.genericName.lowercase().contains(q) ||
                it.genericNameZh.contains(q) ||
                it.id.contains(q) ||
                it.brandNames.any { n -> n.lowercase().contains(q) }
            }
            .take(limit)
            .toList()
    }

    fun byCategory(category: DrugCategory): List<Drug> =
        drugs.filter { it.category == category }
}
