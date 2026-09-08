package com.dosecare.app.domain.catalog

import com.dosecare.app.domain.pk.PkModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DrugCatalog JSON 反序列化器
 *
 * 使用 kotlinx.serialization 解析 `assets/drugs/vX.X.json`。
 * 反序列化后转成内部 [Drug] 数据结构。
 *
 * 用法（Android 端）：
 * ```
 * val raw = context.assets.open("drugs/v0.1.json")
 *     .bufferedReader(Charsets.UTF_8).use { it.readText() }
 * val drugs = DrugCatalogLoader.fromJson(raw)
 * val service = DrugCatalogService(drugs)
 * ```
 *
 * 用法（单元测试）：
 * ```
 * val raw = javaClass.classLoader.getResource("drugs/v0.1.json")!!.readText()
 * val drugs = DrugCatalogLoader.fromJson(raw)
 * ```
 *
 * 依赖：org.jetbrains.kotlinx:kotlinx-serialization-json
 */
object DrugCatalogLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun fromJson(raw: String): List<Drug> {
        val schema = json.decodeFromString<CatalogSchema>(raw)
        return schema.drugs.map { it.toInternal() }
    }
}

// ============================================================
// Wire-format（与 JSON 1:1 对应）
// ============================================================

@Serializable
internal data class CatalogSchema(
    val schemaVersion: String,
    val generatedAt: String,
    val source: List<String> = emptyList(),
    val disclaimer: String? = null,
    val drugs: List<DrugSchema>
)

@Serializable
internal data class DrugSchema(
    val id: String,
    val genericName: String,
    val genericNameZh: String,
    val brandNames: List<String> = emptyList(),
    val category: String,
    val subcategory: String? = null,
    val atc: String? = null,

    val pkModel: String,
    val forms: List<DrugFormSchema>,
    val indicationGroups: List<String> = emptyList(),
    val kePerHour: Double,
    val tHalfHours: Double,
    val tHalfRangeHours: List<Double> = emptyList(),
    val vdLPerKg: Double,
    val clLPerHour: Double,
    val proteinBindingPct: Int = 0,
    val notes: String? = null,

    val therapeuticWindow: TherapeuticWindowSchema? = null,
    val cMaxUnitFactor: Double = 1.0,
    val skipTherapeuticWindowBand: Boolean = false,

    val cypProfile: CypProfileSchema = CypProfileSchema(),
    val activeMetabolites: List<MetaboliteSchema> = emptyList(),

    val adverseEffects: AdverseEffectsSchema = AdverseEffectsSchema(),

    val overdose: OverdoseSchema? = null,

    val adjustments: AdjustmentsSchema = AdjustmentsSchema(),

    val criticalInteractions: List<CriticalInteractionSchema> = emptyList(),

    val monitoring: MonitoringSchema = MonitoringSchema(),

    val pharmacology: String? = null,

    val references: List<String> = emptyList(),

    // === v0.6 新增: 多房室模型参数 (二房室/三房室必填, 一房室忽略) ===
    val k10PerHour: Double? = null,
    val k12PerHour: Double? = null,
    val k21PerHour: Double? = null,
    val k13PerHour: Double? = null,
    val k31PerHour: Double? = null,
    val v1Liters: Double? = null,
    // 临床试验 PK 数据校准 (Cmax/AUC/t½ 实际测量 vs 模型预测)
    val clinicalTrialRefs: List<String> = emptyList()
) {
    fun toInternal(): Drug {
        // v0.6 支持 1/2/3 房室口服
        require(pkModel in setOf("ONE_COMPARTMENT_ORAL", "TWO_COMPARTMENT_ORAL", "THREE_COMPARTMENT_ORAL")) {
            "Unsupported pkModel '$pkModel' for $id"
        }
        val form = forms.firstOrNull()
            ?: error("Drug $id has no forms")
        require(form.route == "ORAL") {
            "v0.6 only supports ORAL route; got ${form.route} for $id"
        }

        val pk: PkModel = when (pkModel) {
            "ONE_COMPARTMENT_ORAL" -> PkModel.OneCompartmentWithAbsorption(
                f = form.f,
                kaPerHour = form.kaPerHour ?: error("ORAL drug $id needs kaPerHour"),
                kePerHour = kePerHour,
                vdLiters = vdLPerKg * 70.0,
                route = PkModel.Route.ORAL
            )
            "TWO_COMPARTMENT_ORAL" -> {
                val k10 = k10PerHour ?: error("2-comp drug $id needs k10PerHour")
                val k12 = k12PerHour ?: error("2-comp drug $id needs k12PerHour")
                val k21 = k21PerHour ?: error("2-comp drug $id needs k21PerHour")
                PkModel.TwoCompartmentWithAbsorption(
                    f = form.f,
                    kaPerHour = form.kaPerHour ?: error("2-comp ORAL drug $id needs kaPerHour"),
                    k10PerHour = k10,
                    k12PerHour = k12,
                    k21PerHour = k21,
                    v1Liters = v1Liters ?: (vdLPerKg * 70.0),
                    route = PkModel.Route.ORAL
                )
            }
            "THREE_COMPARTMENT_ORAL" -> {
                val k10 = k10PerHour ?: error("3-comp drug $id needs k10PerHour")
                val k12 = k12PerHour ?: error("3-comp drug $id needs k12PerHour")
                val k21 = k21PerHour ?: error("3-comp drug $id needs k21PerHour")
                val k13 = k13PerHour ?: error("3-comp drug $id needs k13PerHour")
                val k31 = k31PerHour ?: error("3-comp drug $id needs k31PerHour")
                PkModel.ThreeCompartmentWithAbsorption(
                    f = form.f,
                    kaPerHour = form.kaPerHour ?: error("3-comp ORAL drug $id needs kaPerHour"),
                    k10PerHour = k10,
                    k12PerHour = k12,
                    k21PerHour = k21,
                    k13PerHour = k13,
                    k31PerHour = k31,
                    v1Liters = v1Liters ?: (vdLPerKg * 70.0),
                    route = PkModel.Route.ORAL
                )
            }
            else -> error("unreachable")
        }

        val window = therapeuticWindow?.toInternal()

        return Drug(
            id = id,
            genericName = genericName,
            genericNameZh = genericNameZh,
            brandNames = brandNames,
            category = DrugCategory.valueOf(category),
            subcategory = subcategory,
            atc = atc,
            pkModel = pk,
            forms = forms.map { it.toInternal() },
            indicationGroups = indicationGroups.mapNotNull { runCatching { IndicationGroup.valueOf(it) }.getOrNull() },
            cypProfile = cypProfile.toInternal(),
            therapeuticWindow = window,
            cMaxUnitFactor = cMaxUnitFactor,
            skipTherapeuticWindowBand = skipTherapeuticWindowBand,
            proteinBindingPct = proteinBindingPct,
            activeMetabolites = activeMetabolites.map { it.toInternal() },
            adverseEffects = adverseEffects.toInternal(),
            overdose = overdose?.toInternal(),
            adjustments = adjustments.toInternal(),
            criticalInteractions = criticalInteractions.mapNotNull { it.toInternal() },
            monitoring = monitoring.toInternal(),
            pharmacology = pharmacology,
            references = references
        )
    }
}

@Serializable
internal data class DrugFormSchema(
    val route: String,
    val f: Double,
    val kaPerHour: Double? = null,
    val tMaxHours: Double? = null,
    val doseUnits: List<String> = listOf("mg"),
    val commonDoseRangeMg: List<Double>? = null
) {
    fun toInternal(): DrugForm = DrugForm(
        route = PkModel.Route.valueOf(route),
        f = f,
        kaPerHour = kaPerHour,
        tMaxHours = tMaxHours,
        doseUnits = doseUnits,
        commonDoseRangeMg = commonDoseRangeMg?.let { it[0] to it[1] }
    )
}

@Serializable
internal data class TherapeuticWindowSchema(
    val low: Double,
    val high: Double,
    val unit: String,
    val guidelineSource: String? = null
) {
    fun toInternal(): PkModel.TherapeuticWindow =
        PkModel.TherapeuticWindow(low, high, unit, guidelineSource)
}

@Serializable
internal data class CypProfileSchema(
    val substrates: List<CypContributionSchema> = emptyList(),
    val inhibitors: List<CypInhibitionSchema> = emptyList(),
    val inducers: List<CypInductionSchema> = emptyList(),
    val note: String? = null,
    val primaryPathway: String? = null,
    val pathwayType: String? = null
) {
    fun toInternal(): CypProfile = CypProfile(
        substrates = substrates.mapNotNull { sub ->
            runCatching { CypContribution(CypEnzyme.valueOf(sub.cyp), sub.fraction) }.getOrNull()
        },
        inhibitors = inhibitors.mapNotNull { inh ->
            runCatching {
                CypInhibition(CypEnzyme.valueOf(inh.cyp), EffectStrength.valueOf(inh.strength))
            }.getOrNull()
        },
        inducers = inducers.mapNotNull { ind ->
            runCatching {
                CypInduction(CypEnzyme.valueOf(ind.cyp), EffectStrength.valueOf(ind.strength))
            }.getOrNull()
        },
        note = note,
        primaryPathway = primaryPathway,
        pathwayType = pathwayType?.let { runCatching { PathwayType.valueOf(it) }.getOrNull() }
    )
}

@Serializable
internal data class CypContributionSchema(val cyp: String, val fraction: Double)

@Serializable
internal data class CypInhibitionSchema(val cyp: String, val strength: String)

@Serializable
internal data class CypInductionSchema(val cyp: String, val strength: String)

@Serializable
internal data class MetaboliteSchema(
    val id: String,
    val name: String,
    val activityRatio: Double = 1.0,
    val note: String? = null
) {
    fun toInternal(): Metabolite = Metabolite(id, name, activityRatio, note)
}

@Serializable
internal data class AdverseEffectsSchema(
    val qtcProlongation: String = "LOW",
    val metabolicSyndrome: String = "LOW",
    val anticholinergicLoad: Int = 0,
    val agranulocytosis: String = "VERY_LOW",
    val extrapyramidal: String = "LOW",
    val sedation: String = "LOW",
    val sexual: String = "LOW",
    val hyperprolactinemia: String = "LOW"
) {
    fun toInternal(): AdverseEffects = AdverseEffects(
        qtcProlongation = RiskLevel.valueOf(qtcProlongation),
        metabolicSyndrome = RiskLevel.valueOf(metabolicSyndrome),
        anticholinergicLoad = anticholinergicLoad,
        agranulocytosis = RiskLevel.valueOf(agranulocytosis),
        extrapyramidal = RiskLevel.valueOf(extrapyramidal),
        sedation = RiskLevel.valueOf(sedation),
        sexual = RiskLevel.valueOf(sexual),
        hyperprolactinemia = RiskLevel.valueOf(hyperprolactinemia)
    )
}

@Serializable
internal data class OverdoseSchema(
    val symptoms: String,                       // 典型症状
    val severity: String,                       // MILD / MODERATE / SEVERE / LIFE_THREATENING
    val toxicDoseEstimateMg: Double? = null,    // 中毒剂量估计 (mg, 70kg)
    val fatalDoseEstimateMg: Double? = null,    // 致死剂量估计 (mg, 70kg)
    val management: String,                     // 抢救要点
    val antidote: String? = null,               // 特异性解毒剂
    val dataSource: String                      // FDA DailyMed / 临床指南
) {
    fun toInternal(): OverdoseInfo = OverdoseInfo(
        symptoms = symptoms,
        severity = OverdoseSeverity.valueOf(severity),
        toxicDoseEstimateMg = toxicDoseEstimateMg,
        fatalDoseEstimateMg = fatalDoseEstimateMg,
        management = management,
        antidote = antidote,
        dataSource = dataSource
    )
}

@Serializable
internal data class AdjustmentsSchema(
    val renal: String = "NONE",
    val hepatic: String = "NONE",
    val elderly: String? = null,
    val smoking: SmokingEffectSchema? = null
) {
    fun toInternal(): DoseAdjustments = DoseAdjustments(
        renal = RenalAdjustment.valueOf(renal),
        hepatic = HepaticAdjustment.valueOf(hepatic),
        elderly = elderly,
        smoking = smoking?.toInternal()
    )
}

@Serializable
internal data class SmokingEffectSchema(
    val effect: String,
    val doseAdjustment: String,
    val abstinenceNote: String? = null
) {
    fun toInternal(): SmokingEffect = SmokingEffect(effect, doseAdjustment, abstinenceNote)
}

@Serializable
internal data class CriticalInteractionSchema(
    val triggerDrugId: String,
    val mechanism: String,
    val aucFoldChange: List<Double>,
    val severity: String,
    val clinicalNote: String
) {
    fun toInternal(): CriticalInteractionHint? {
        val m = runCatching { Mechanism.valueOf(mechanism) }.getOrNull() ?: return null
        if (aucFoldChange.size < 2) return null
        return CriticalInteractionHint(
            triggerDrugId = triggerDrugId,
            mechanism = m,
            aucFoldChange = aucFoldChange[0] to aucFoldChange[1],
            severity = severity,
            clinicalNote = clinicalNote
        )
    }
}

@Serializable
internal data class MonitoringSchema(
    val frequency: String? = null,
    val items: List<String> = emptyList()
) {
    fun toInternal(): MonitoringRequirements = MonitoringRequirements(frequency, items)
}
