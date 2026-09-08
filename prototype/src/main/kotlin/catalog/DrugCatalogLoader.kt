package catalog

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
    val kePerHour: Double,
    val tHalfHours: Double,
    val tHalfRangeHours: List<Double> = emptyList(),
    val vdLPerKg: Double,
    val clLPerHour: Double,
    val proteinBindingPct: Int = 0,
    val notes: String? = null,

    val therapeuticWindow: TherapeuticWindowSchema? = null,

    val cypProfile: CypProfileSchema = CypProfileSchema(),
    val activeMetabolites: List<MetaboliteSchema> = emptyList(),

    val adverseEffects: AdverseEffectsSchema = AdverseEffectsSchema(),

    val adjustments: AdjustmentsSchema = AdjustmentsSchema(),

    val criticalInteractions: List<CriticalInteractionSchema> = emptyList(),

    val monitoring: MonitoringSchema = MonitoringSchema(),

    val references: List<String> = emptyList()
) {
    fun toInternal(): Drug {
        // v0.1 只支持一房室口服
        require(pkModel == "ONE_COMPARTMENT_ORAL") {
            "v0.1 only supports ONE_COMPARTMENT_ORAL; got $pkModel for $id"
        }
        val form = forms.firstOrNull()
            ?: error("Drug $id has no forms")
        require(form.route == "ORAL") {
            "v0.1 only supports ORAL route; got ${form.route} for $id"
        }

        val pk = PkModel.OneCompartmentWithAbsorption(
            f = form.f,
            kaPerHour = form.kaPerHour ?: error("ORAL drug $id needs kaPerHour"),
            kePerHour = kePerHour,
            vdLiters = vdLPerKg * 70.0,  // 假设体重 70kg 基准
            route = PkModel.Route.ORAL
        )

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
            cypProfile = cypProfile.toInternal(),
            therapeuticWindow = window,
            activeMetabolites = activeMetabolites.map { it.toInternal() },
            adverseEffects = adverseEffects.toInternal(),
            adjustments = adjustments.toInternal(),
            criticalInteractions = criticalInteractions.map { it.toInternal() },
            monitoring = monitoring.toInternal(),
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
    val note: String? = null
) {
    fun toInternal(): CypProfile = CypProfile(
        substrates = substrates.map { CypContribution(CypEnzyme.valueOf(it.cyp), it.fraction) },
        inhibitors = inhibitors.map {
            CypInhibition(CypEnzyme.valueOf(it.cyp), EffectStrength.valueOf(it.strength))
        },
        inducers = inducers.map {
            CypInduction(CypEnzyme.valueOf(it.cyp), EffectStrength.valueOf(it.strength))
        }
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
        qtcProlongation = catalog.RiskLevel.valueOf(qtcProlongation),
        metabolicSyndrome = catalog.RiskLevel.valueOf(metabolicSyndrome),
        anticholinergicLoad = anticholinergicLoad,
        agranulocytosis = catalog.RiskLevel.valueOf(agranulocytosis),
        extrapyramidal = catalog.RiskLevel.valueOf(extrapyramidal),
        sedation = catalog.RiskLevel.valueOf(sedation),
        sexual = catalog.RiskLevel.valueOf(sexual),
        hyperprolactinemia = catalog.RiskLevel.valueOf(hyperprolactinemia)
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
        renal = catalog.RenalAdjustment.valueOf(renal),
        hepatic = catalog.HepaticAdjustment.valueOf(hepatic),
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
    fun toInternal(): CriticalInteractionHint = CriticalInteractionHint(
        triggerDrugId = triggerDrugId,
        mechanism = Mechanism.valueOf(mechanism),
        aucFoldChange = aucFoldChange[0] to aucFoldChange[1],
        severity = severity,
        clinicalNote = clinicalNote
    )
}

@Serializable
internal data class MonitoringSchema(
    val frequency: String? = null,
    val items: List<String> = emptyList()
) {
    fun toInternal(): MonitoringRequirements = MonitoringRequirements(frequency, items)
}
