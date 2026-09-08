package catalog

import pk.PkModel
import pk.PkModel.Route

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
enum class DrugCategory(val displayName: String) {
    ANTIPSYCHOTIC("抗精神病药"),
    MOOD_STABILIZER("心境稳定剂"),
    ANTIDEPRESSANT("抗抑郁药"),
    ANXIOLYTIC("抗焦虑/苯二氮䓬类"),
    STIMULANT("ADHD 兴奋剂"),
    ANTICHOLINERGIC("抗胆碱能"),
    ANTIPARKINSONIAN("抗帕金森"),
    ANTIDIABETIC("降糖药"),
    THYROID("甲状腺"),
    CORTICOSTEROID("肾上腺皮质激素"),
    ANTIHYPERTENSIVE("降压药"),
    STATIN("调脂药"),
    ANTIEPILEPTIC("抗癫痫"),
    ANALGESIC("止痛药"),
    ANTIHISTAMINE("抗组胺"),
    HERBAL("草本补充剂"),
    OTHER("其他")
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

    val cypProfile: CypProfile = CypProfile(),

    val therapeuticWindow: PkModel.TherapeuticWindow? = null,

    val activeMetabolites: List<Metabolite> = emptyList(),

    val adverseEffects: AdverseEffects = AdverseEffects(),

    val adjustments: DoseAdjustments = DoseAdjustments(),

    val criticalInteractions: List<CriticalInteractionHint> = emptyList(),

    val monitoring: MonitoringRequirements = MonitoringRequirements(),

    val references: List<String> = emptyList()  // PMID / 说明书 / 共识指南
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

enum class RenalAdjustment { NONE, MONITOR, REDUCE_50_PCT, CONTRAINDICATED }
enum class HepaticAdjustment { NONE, REDUCE_50_PCT_CHILD_PUGH_B, REDUCE_50_PCT_CHILD_PUGH_C, CONTRAINDICATED }

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
    QTc_ADDITIVE,
    SEROTONIN_ADDITIVE,
    ANTICHOLINERGIC_ADDITIVE,
    PHARMACODYNAMIC,
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
