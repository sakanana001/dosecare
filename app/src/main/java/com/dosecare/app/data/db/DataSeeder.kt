package com.dosecare.app.data.db

import android.content.Context
import android.util.Log
import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.catalog.IndicationGroup
import com.dosecare.app.domain.catalog.CypEnzyme
import com.dosecare.app.domain.catalog.EffectStrength
import com.dosecare.app.domain.catalog.PathwayType
import com.dosecare.app.domain.catalog.RiskLevel
import com.dosecare.app.domain.catalog.RenalAdjustment
import com.dosecare.app.domain.catalog.HepaticAdjustment
import com.dosecare.app.domain.catalog.Mechanism
import com.dosecare.app.domain.catalog.OverdoseSeverity
import com.dosecare.app.domain.catalog.SmokingEffect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 静态数据灌库器 (v0.7 Phase B)
 *
 * 流程:
 * 1. 检查 app_metadata.catalog_version
 *    - 不存在 → 首次启动,需要灌库
 *    - 存在但 version 不匹配 → 删除老表数据,重新灌库
 *    - 匹配 → 跳过
 *
 * 2. 读 assets/drugs/v0.6.json, 解析成 DrugCatalogLoader.fromJson() 的 List<Drug>
 *
 * 3. 把 List<Drug> 拆成 11 张表的 List<Entity>, 单事务写入
 *
 * 4. 写 app_metadata: catalog_version = 当前 JSON 的 schemaVersion
 *
 * 设计选择: 灌库在 Hilt 单例构造时同步执行 (runBlocking).
 *   - 202 药 × 5 关联表 ≈ 1500 行, < 500ms
 *   - 后续启动 (metadata 匹配) → 200ms 跳完
 *   - 不影响 UI 响应 (Application.onCreate 已经异步)
 *
 * v0.8 计划: 改成后台 Coroutine + 启动屏, 避免阻塞 Application.onCreate
 */
object DataSeeder {
    private const val TAG = "DataSeeder"
    private const val EXPECTED_CATALOG_VERSION = "0.7.0"  // v0.7 改 schema, 升 version

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    /**
     * 同步入口: 灌库如果需要, 否则什么都不做.
     * 在 Hilt 单例构造时调用, 阻塞主线程 ~500ms (一次性).
     */
    suspend fun seedIfNeeded(context: Context, db: AppDatabase) {
        val currentVersion = db.appMetadataDao().get(AppMetadataKeys.CATALOG_VERSION)
        if (currentVersion == EXPECTED_CATALOG_VERSION) {
            Log.i(TAG, "Catalog already at version $currentVersion, skipping seed")
            return
        }
        if (currentVersion != null) {
            Log.w(TAG, "Catalog version mismatch: db=$currentVersion, expected=$EXPECTED_CATALOG_VERSION, re-seeding")
        } else {
            Log.i(TAG, "First launch, seeding catalog...")
        }
        val start = System.currentTimeMillis()
        val raw = context.assets.open(CATALOG_ASSET)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val schema = json.parseToJsonElement(raw) as JsonObject
        val schemaVersion = schema["schemaVersion"]?.let { (it as JsonPrimitive).content } ?: "?"
        val drugs = DrugCatalogLoader.fromJson(raw)
        Log.i(TAG, "Parsed ${drugs.size} drugs from JSON v$schemaVersion in ${System.currentTimeMillis() - start}ms")
        writeToDb(db, drugs)
        db.appMetadataDao().upsertAll(listOf(
            AppMetadataEntity(AppMetadataKeys.CATALOG_VERSION, EXPECTED_CATALOG_VERSION),
            AppMetadataEntity(AppMetadataKeys.CATALOG_GENERATED_AT, schemaVersion),
            AppMetadataEntity(AppMetadataKeys.LAST_SEEDED_AT, System.currentTimeMillis().toString())
        ))
        Log.i(TAG, "Seeded ${drugs.size} drugs in ${System.currentTimeMillis() - start}ms total")
    }

    private suspend fun writeToDb(db: AppDatabase, drugs: List<com.dosecare.app.domain.catalog.Drug>) {
        // 1. drug 主表
        val drugEntities = drugs.map { d ->
            val form = d.forms.firstOrNull() ?: error("Drug ${d.id} has no forms")
            val doseRange = form.commonDoseRangeMg
            DrugEntity(
                id = d.id,
                genericName = d.genericName,
                genericNameZh = d.genericNameZh,
                category = d.category.name,
                subcategory = d.subcategory,
                atc = d.atc,
                f = form.f,
                kaPerHour = form.kaPerHour,
                tMaxHours = form.tMaxHours,
                doseUnits = form.doseUnits.joinToString(","),
                commonDoseLow = doseRange?.first,
                commonDoseHigh = doseRange?.second,
                route = form.route.name,
                cMaxUnitFactor = d.cMaxUnitFactor,
                skipWindowBand = d.skipTherapeuticWindowBand,
                proteinBindingPct = d.proteinBindingPct,
                pharmacology = d.pharmacology,
                referencesJson = json.encodeToString(JsonArray.serializer(), JsonArray(d.references.map { JsonPrimitive(it) })),
                clinicalTrialRefsJson = "[]"
            )
        }

        // 2. drug_brand
        val brandEntities = drugs.flatMap { d ->
            d.brandNames.map { DrugBrandEntity(d.id, it) }
        }

        // 3. drug_indication
        val indicationEntities = drugs.flatMap { d ->
            d.indicationGroups.map { DrugIndicationEntity(d.id, it.name) }
        }

        // 4. drug_active_metabolite
        val metaboliteEntities = drugs.flatMap { d ->
            d.activeMetabolites.map { m ->
                DrugActiveMetaboliteEntity(
                    drugId = d.id,
                    metaboliteId = m.id,
                    name = m.name,
                    activityRatio = m.activityRatio,
                    note = m.note
                )
            }
        }

        // 5. drug_pk
        val pkEntities = drugs.map { d ->
            val pk = d.pkModel
            val (ke, tHalf, vdPerKg, cl, modelType, k10, k12, k21, k13, k31, v1) = when (pk) {
                is com.dosecare.app.domain.pk.PkModel.OneCompartmentWithAbsorption -> {
                    Tuple6(
                        ke = pk.kePerHour,
                        tHalf = pk.tHalfHours,
                        vdPerKg = pk.vdLiters / 70.0,
                        cl = pk.clearanceLPerHour,
                        modelType = "ONE_COMPARTMENT_ORAL",
                        k10 = null, k12 = null, k21 = null, k13 = null, k31 = null, v1 = null
                    )
                }
                is com.dosecare.app.domain.pk.PkModel.OneCompartmentIv -> {
                    Tuple6(
                        ke = pk.kePerHour,
                        tHalf = pk.tHalfHours,
                        vdPerKg = pk.vdLiters / 70.0,
                        cl = pk.vdLiters * pk.kePerHour,
                        modelType = "ONE_COMPARTMENT_IV",
                        k10 = null, k12 = null, k21 = null, k13 = null, k31 = null, v1 = null
                    )
                }
                is com.dosecare.app.domain.pk.PkModel.TwoCompartmentWithAbsorption -> {
                    Tuple6(
                        ke = pk.alphaPerHour,
                        tHalf = pk.tHalfHours,
                        vdPerKg = pk.v1Liters / 70.0,
                        cl = pk.clearanceLPerHour,
                        modelType = "TWO_COMPARTMENT_ORAL",
                        k10 = pk.k10PerHour, k12 = pk.k12PerHour, k21 = pk.k21PerHour,
                        k13 = null, k31 = null, v1 = pk.v1Liters
                    )
                }
                is com.dosecare.app.domain.pk.PkModel.ThreeCompartmentWithAbsorption -> {
                    Tuple6(
                        ke = pk.alphaPerHour,
                        tHalf = pk.tHalfHours,
                        vdPerKg = pk.v1Liters / 70.0,
                        cl = pk.v1Liters * pk.k10PerHour,  // CL = V1 * k10
                        modelType = "THREE_COMPARTMENT_ORAL",
                        k10 = pk.k10PerHour, k12 = pk.k12PerHour, k21 = pk.k21PerHour,
                        k13 = pk.k13PerHour, k31 = pk.k31PerHour, v1 = pk.v1Liters
                    )
                }
            }
            DrugPkEntity(
                drugId = d.id,
                modelType = modelType,
                kePerHour = ke,
                tHalfHours = tHalf,
                tHalfLow = null,
                tHalfHigh = null,
                vdLPerKg = vdPerKg,
                clLPerHour = cl,
                k10PerHour = k10, k12PerHour = k12, k21PerHour = k21,
                k13PerHour = k13, k31PerHour = k31, v1Liters = v1
            )
        }

        // 6. drug_therapeutic_window
        val windowEntities = drugs.mapNotNull { d ->
            d.therapeuticWindow?.let { w ->
                DrugTherapeuticWindowEntity(d.id, w.low, w.high, w.unit, w.guidelineSource)
            }
        }

        // 7. drug_overdose
        val overdoseEntities = drugs.mapNotNull { d ->
            d.overdose?.let { o ->
                DrugOverdoseEntity(
                    drugId = d.id,
                    symptoms = o.symptoms,
                    severity = o.severity.name,
                    toxicDoseMg = o.toxicDoseEstimateMg,
                    fatalDoseMg = o.fatalDoseEstimateMg,
                    management = o.management,
                    antidote = o.antidote,
                    dataSource = o.dataSource
                )
            }
        }

        // 8. drug_clinical (合并 adjustments + adverse + monitoring)
        val clinicalEntities = drugs.map { d ->
            val adj = d.adjustments
            val ae = d.adverseEffects
            val mon = d.monitoring
            DrugClinicalEntity(
                drugId = d.id,
                renal = adj.renal.name,
                hepatic = adj.hepatic.name,
                elderly = adj.elderly,
                smokingEffect = adj.smoking?.effect,
                smokingDoseAdjustment = adj.smoking?.doseAdjustment,
                smokingAbstinenceNote = adj.smoking?.abstinenceNote,
                qtcProlongation = ae.qtcProlongation.name,
                metabolicSyndrome = ae.metabolicSyndrome.name,
                anticholinergicLoad = ae.anticholinergicLoad,
                agranulocytosis = ae.agranulocytosis.name,
                extrapyramidal = ae.extrapyramidal.name,
                sedation = ae.sedation.name,
                sexual = ae.sexual.name,
                hyperprolactinemia = ae.hyperprolactinemia.name,
                monitoringFrequency = mon.frequency,
                monitoringItemsJson = json.encodeToString(JsonArray.serializer(), JsonArray(mon.items.map { JsonPrimitive(it) }))
            )
        }

        // 9. cyp_role (合并 substrate/inhibitor/inducer)
        val cypRoleEntities = drugs.flatMap { d ->
            val p = d.cypProfile
            buildList {
                p.substrates.forEach { c ->
                    add(CypRoleEntity(d.id, c.cyp.name, "SUBSTRATE", fraction = c.fraction, strength = null))
                }
                p.inhibitors.forEach { c ->
                    add(CypRoleEntity(d.id, c.cyp.name, "INHIBITOR", fraction = null, strength = c.strength.name))
                }
                p.inducers.forEach { c ->
                    add(CypRoleEntity(d.id, c.cyp.name, "INDUCER", fraction = null, strength = c.strength.name))
                }
                // 把 primaryPathway 和 pathwayType 写到 drug_clinical (但那里没字段, 后续加)
                // v0.7 暂时忽略, 主表里加 2 列后续再说
            }
        }

        // 10. drug_critical_interaction
        val interactionEntities = drugs.flatMap { d ->
            d.criticalInteractions.map { ci ->
                DrugCriticalInteractionEntity(
                    drugId = d.id,
                    triggerDrugId = ci.triggerDrugId,
                    mechanism = ci.mechanism.name,
                    aucFoldLow = ci.aucFoldChange.first,
                    aucFoldHigh = ci.aucFoldChange.second,
                    severity = ci.severity,
                    clinicalNote = ci.clinicalNote
                )
            }
        }

        // 全部 REPLACE 写入 (PK 重复就覆盖)
        db.drugDao().upsertAll(drugEntities)
        db.drugBrandDao().upsertAll(brandEntities)
        db.drugIndicationDao().upsertAll(indicationEntities)
        db.drugActiveMetaboliteDao().upsertAll(metaboliteEntities)
        db.drugPkDao().upsertAll(pkEntities)
        db.drugTherapeuticWindowDao().upsertAll(windowEntities)
        db.drugOverdoseDao().upsertAll(overdoseEntities)
        db.drugClinicalDao().upsertAll(clinicalEntities)
        db.cypRoleDao().upsertAll(cypRoleEntities)
        db.drugCriticalInteractionDao().upsertAll(interactionEntities)
    }

    /**
     * 测试 / 维护用: 清空所有静态表 (用户表不动)
     */
    suspend fun clearAll(db: AppDatabase) {
        db.drugCriticalInteractionDao().deleteAll()
        db.cypRoleDao().deleteAll()
        db.drugClinicalDao().deleteAll()
        db.drugOverdoseDao().deleteAll()
        db.drugTherapeuticWindowDao().deleteAll()
        db.drugPkDao().deleteAll()
        db.drugActiveMetaboliteDao().deleteAll()
        db.drugIndicationDao().deleteAll()
        db.drugBrandDao().deleteAll()
        db.drugDao().deleteAll()
        db.appMetadataDao().deleteAll()
    }

    private const val CATALOG_ASSET = "drugs/v0.6.json"

    // ============== 测试专用入口 (JVM unit test 用) ==============

    /**
     * 测试用: 解析 JSON 字符串到 List<Drug> domain 对象
     * (生产代码用 seedIfNeeded, 测试代码用这个避免 Context 依赖)
     */
    fun fromJsonForTest(raw: String): List<com.dosecare.app.domain.catalog.Drug> {
        return DrugCatalogLoader.fromJson(raw)
    }

    /**
     * 测试用: 把 List<Drug> 写到 in-memory DB (跟生产 writeToDb 一样的逻辑)
     */
    suspend fun writeToDbForTest(db: AppDatabase, drugs: List<com.dosecare.app.domain.catalog.Drug>) {
        writeToDb(db, drugs)
    }
}

/**
 * 内部辅助 data class, 让 when 表达式能返回多个值
 */
private data class Tuple6(
    val ke: Double,
    val tHalf: Double,
    val vdPerKg: Double,
    val cl: Double,
    val modelType: String,
    val k10: Double?, val k12: Double?, val k21: Double?,
    val k13: Double?, val k31: Double?, val v1: Double?
)
