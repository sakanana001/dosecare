package com.dosecare.app.data.db

import com.dosecare.app.domain.catalog.AdverseEffects
import com.dosecare.app.domain.catalog.CypContribution
import com.dosecare.app.domain.catalog.CypEnzyme
import com.dosecare.app.domain.catalog.CypInduction
import com.dosecare.app.domain.catalog.CypInhibition
import com.dosecare.app.domain.catalog.CypProfile
import com.dosecare.app.domain.catalog.CriticalInteractionHint
import com.dosecare.app.domain.catalog.DoseAdjustments
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCategory
import com.dosecare.app.domain.catalog.DrugForm
import com.dosecare.app.domain.catalog.EffectStrength
import com.dosecare.app.domain.catalog.HepaticAdjustment
import com.dosecare.app.domain.catalog.IndicationGroup
import com.dosecare.app.domain.catalog.Mechanism
import com.dosecare.app.domain.catalog.Metabolite
import com.dosecare.app.domain.catalog.MonitoringRequirements
import com.dosecare.app.domain.catalog.OverdoseInfo
import com.dosecare.app.domain.catalog.OverdoseSeverity
import com.dosecare.app.domain.catalog.PathwayType
import com.dosecare.app.domain.catalog.RenalAdjustment
import com.dosecare.app.domain.catalog.RiskLevel
import com.dosecare.app.domain.catalog.SmokingEffect
import com.dosecare.app.domain.pk.PkModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * DB Entity ↔ Domain Drug 互转
 *
 * v0.7 Phase B: DataSeeder 写入 DB, DrugCatalogRepository 用这个 mapper 读出来
 * 拼回 Drug 对象, 然后构造 DrugCatalogService (跟现在一样的内存 API)
 */
object DrugEntityMapper {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    /**
     * 把一组 DB Entity 拼回 1 个 Drug (domain)
     */
    fun toDomain(
        drug: DrugEntity,
        brands: List<String>,
        indications: List<String>,
        metabolites: List<Metabolite>,
        pk: DrugPkEntity?,
        window: PkModel.TherapeuticWindow?,
        overdose: OverdoseInfo?,
        clinical: DrugClinicalEntity,
        cypRoles: List<CypRoleEntity>,
        criticalInteractions: List<DrugCriticalInteractionEntity>
    ): Drug {
        val pkModel = buildPkModel(drug, pk)
        val form = DrugForm(
            route = PkModel.Route.valueOf(drug.route),
            f = drug.f,
            kaPerHour = drug.kaPerHour,
            tMaxHours = drug.tMaxHours,
            doseUnits = drug.doseUnits.split(",").filter { it.isNotBlank() },
            commonDoseRangeMg = if (drug.commonDoseLow != null && drug.commonDoseHigh != null)
                drug.commonDoseLow to drug.commonDoseHigh else null
        )
        val cypProfile = buildCypProfile(cypRoles)
        val adjustments = DoseAdjustments(
            renal = runCatching { RenalAdjustment.valueOf(clinical.renal) }.getOrDefault(RenalAdjustment.NONE),
            hepatic = runCatching { HepaticAdjustment.valueOf(clinical.hepatic) }.getOrDefault(HepaticAdjustment.NONE),
            elderly = clinical.elderly,
            smoking = if (clinical.smokingEffect != null && clinical.smokingDoseAdjustment != null) {
                SmokingEffect(
                    effect = clinical.smokingEffect,
                    doseAdjustment = clinical.smokingDoseAdjustment,
                    abstinenceNote = clinical.smokingAbstinenceNote
                )
            } else null
        )
        val adverse = AdverseEffects(
            qtcProlongation = runCatching { RiskLevel.valueOf(clinical.qtcProlongation) }.getOrDefault(RiskLevel.LOW),
            metabolicSyndrome = runCatching { RiskLevel.valueOf(clinical.metabolicSyndrome) }.getOrDefault(RiskLevel.LOW),
            anticholinergicLoad = clinical.anticholinergicLoad,
            agranulocytosis = runCatching { RiskLevel.valueOf(clinical.agranulocytosis) }.getOrDefault(RiskLevel.VERY_LOW),
            extrapyramidal = runCatching { RiskLevel.valueOf(clinical.extrapyramidal) }.getOrDefault(RiskLevel.LOW),
            sedation = runCatching { RiskLevel.valueOf(clinical.sedation) }.getOrDefault(RiskLevel.LOW),
            sexual = runCatching { RiskLevel.valueOf(clinical.sexual) }.getOrDefault(RiskLevel.LOW),
            hyperprolactinemia = runCatching { RiskLevel.valueOf(clinical.hyperprolactinemia) }.getOrDefault(RiskLevel.LOW)
        )
        val monitoring = MonitoringRequirements(
            frequency = clinical.monitoringFrequency,
            items = parseStringList(clinical.monitoringItemsJson)
        )
        val references = parseStringList(drug.referencesJson)

        return Drug(
            id = drug.id,
            genericName = drug.genericName,
            genericNameZh = drug.genericNameZh,
            brandNames = brands,
            category = runCatching { DrugCategory.valueOf(drug.category) }.getOrDefault(DrugCategory.OTHER),
            subcategory = drug.subcategory,
            atc = drug.atc,
            pkModel = pkModel,
            forms = listOf(form),
            indicationGroups = indications.mapNotNull { runCatching { IndicationGroup.valueOf(it) }.getOrNull() },
            cypProfile = cypProfile,
            therapeuticWindow = window,
            cMaxUnitFactor = drug.cMaxUnitFactor,
            skipTherapeuticWindowBand = drug.skipWindowBand,
            proteinBindingPct = drug.proteinBindingPct,
            activeMetabolites = metabolites,
            adverseEffects = adverse,
            overdose = overdose,
            adjustments = adjustments,
            criticalInteractions = criticalInteractions.mapNotNull { it.toDomain() },
            monitoring = monitoring,
            pharmacology = drug.pharmacology,
            references = references
        )
    }

    private fun buildPkModel(drug: DrugEntity, pk: DrugPkEntity?): PkModel {
        if (pk == null) {
            // Fallback: 用主表 f/ka/ke/vd 推一房室
            return PkModel.OneCompartmentWithAbsorption(
                f = drug.f,
                kaPerHour = drug.kaPerHour ?: 1.0,
                kePerHour = 0.05,
                vdLiters = drug.f,  // 占位
                route = PkModel.Route.valueOf(drug.route)
            )
        }
        return when (pk.modelType) {
            "ONE_COMPARTMENT_ORAL" -> PkModel.OneCompartmentWithAbsorption(
                f = drug.f,
                kaPerHour = drug.kaPerHour ?: 1.0,
                kePerHour = pk.kePerHour,
                vdLiters = pk.vdLPerKg * 70.0,
                route = PkModel.Route.valueOf(drug.route)
            )
            "ONE_COMPARTMENT_IV" -> PkModel.OneCompartmentIv(
                kePerHour = pk.kePerHour,
                vdLiters = pk.vdLPerKg * 70.0
            )
            "TWO_COMPARTMENT_ORAL" -> PkModel.TwoCompartmentWithAbsorption(
                f = drug.f,
                kaPerHour = drug.kaPerHour ?: 1.0,
                k10PerHour = pk.k10PerHour ?: pk.kePerHour,
                k12PerHour = pk.k12PerHour ?: 0.0,
                k21PerHour = pk.k21PerHour ?: 0.0,
                v1Liters = pk.v1Liters ?: (pk.vdLPerKg * 70.0),
                route = PkModel.Route.valueOf(drug.route)
            )
            "THREE_COMPARTMENT_ORAL" -> PkModel.ThreeCompartmentWithAbsorption(
                f = drug.f,
                kaPerHour = drug.kaPerHour ?: 1.0,
                k10PerHour = pk.k10PerHour ?: pk.kePerHour,
                k12PerHour = pk.k12PerHour ?: 0.0,
                k21PerHour = pk.k21PerHour ?: 0.0,
                k13PerHour = pk.k13PerHour ?: 0.0,
                k31PerHour = pk.k31PerHour ?: 0.0,
                v1Liters = pk.v1Liters ?: (pk.vdLPerKg * 70.0),
                route = PkModel.Route.valueOf(drug.route)
            )
            else -> PkModel.OneCompartmentWithAbsorption(
                f = drug.f, kaPerHour = drug.kaPerHour ?: 1.0,
                kePerHour = pk.kePerHour, vdLiters = pk.vdLPerKg * 70.0,
                route = PkModel.Route.valueOf(drug.route)
            )
        }
    }

    private fun buildCypProfile(roles: List<CypRoleEntity>): CypProfile {
        val substrates = roles.filter { it.roleType == "SUBSTRATE" }.mapNotNull { r ->
            runCatching { CypContribution(CypEnzyme.valueOf(r.cyp), r.fraction ?: 0.0) }.getOrNull()
        }
        val inhibitors = roles.filter { it.roleType == "INHIBITOR" }.mapNotNull { r ->
            runCatching { CypInhibition(CypEnzyme.valueOf(r.cyp), EffectStrength.valueOf(r.strength ?: "WEAK")) }.getOrNull()
        }
        val inducers = roles.filter { it.roleType == "INDUCER" }.mapNotNull { r ->
            runCatching { CypInduction(CypEnzyme.valueOf(r.cyp), EffectStrength.valueOf(r.strength ?: "WEAK")) }.getOrNull()
        }
        return CypProfile(substrates = substrates, inhibitors = inhibitors, inducers = inducers)
    }

    private fun parseStringList(jsonStr: String): List<String> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return runCatching {
            val arr = json.parseToJsonElement(jsonStr) as JsonArray
            arr.map { (it as JsonPrimitive).content }
        }.getOrDefault(emptyList())
    }
}

/**
 * 关键相互作用 Entity → Domain
 */
fun DrugCriticalInteractionEntity.toDomain(): CriticalInteractionHint? {
    val m = runCatching { Mechanism.valueOf(mechanism) }.getOrNull() ?: return null
    return CriticalInteractionHint(
        triggerDrugId = triggerDrugId,
        mechanism = m,
        aucFoldChange = aucFoldLow to aucFoldHigh,
        severity = severity,
        clinicalNote = clinicalNote
    )
}
