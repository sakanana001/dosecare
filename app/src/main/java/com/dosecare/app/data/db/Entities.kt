package com.dosecare.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 静态参考表 Entity (v0.7 启用)
 *
 * 11 表 schema:
 * 1.  drug                     药物主表
 * 2.  drug_brand               品牌名 (1:N)
 * 3.  drug_indication          适应症 (1:N)
 * 4.  drug_active_metabolite   活性代谢物 (1:N)
 * 5.  drug_pk                  PK 模型 (0:1, 单行, 多房室字段按 model_type 选填)
 * 6.  drug_therapeutic_window  治疗窗 (0:1)
 * 7.  drug_overdose            过量 (0:1)
 * 8.  drug_clinical            临床调整/不良反应/监测 (0:1, 合并)
 * 9.  cyp_role                 CYP 角色 (1:N, 多态: SUBSTRATE/INHIBITOR/INDUCER)
 * 10. drug_critical_interaction 关键相互作用 hint (1:N, 来自 v0.6 的 criticalInteractions)
 * 11. app_metadata             全局 KV (灌库版本号等)
 *
 * v0.7 Room 2.7.0: 所有字段显式 @ColumnInfo(name = ...) 标 snake_case,
 *                  否则 Room 用 Kotlin 字段名 (camelCase) 做列名,跟 SQL 错位
 */

// ============================================================
// 1. drug — 药物主表
// ============================================================
@Entity(tableName = "drug")
data class DrugEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "generic_name") val genericName: String,
    @ColumnInfo(name = "generic_name_zh") val genericNameZh: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "subcategory") val subcategory: String? = null,
    @ColumnInfo(name = "atc") val atc: String? = null,

    @ColumnInfo(name = "f") val f: Double,
    @ColumnInfo(name = "ka_per_hour") val kaPerHour: Double?,
    @ColumnInfo(name = "t_max_hours") val tMaxHours: Double?,
    @ColumnInfo(name = "dose_units") val doseUnits: String,
    @ColumnInfo(name = "common_dose_low") val commonDoseLow: Double?,
    @ColumnInfo(name = "common_dose_high") val commonDoseHigh: Double?,
    @ColumnInfo(name = "route") val route: String,

    @ColumnInfo(name = "c_max_unit_factor") val cMaxUnitFactor: Double = 1.0,
    @ColumnInfo(name = "skip_window_band") val skipWindowBand: Boolean = false,
    @ColumnInfo(name = "protein_binding_pct") val proteinBindingPct: Int = 0,

    @ColumnInfo(name = "pharmacology") val pharmacology: String? = null,
    @ColumnInfo(name = "references_json") val referencesJson: String = "[]",
    @ColumnInfo(name = "clinical_trial_refs_json") val clinicalTrialRefsJson: String = "[]"
)

// ============================================================
// 2. drug_brand — 品牌名
// ============================================================
@Entity(
    tableName = "drug_brand",
    primaryKeys = ["drug_id", "brand_name"],
    indices = [Index("drug_id")],
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugBrandEntity(
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "brand_name") val brandName: String
)

// ============================================================
// 3. drug_indication — 主治症状/疾病
// ============================================================
@Entity(
    tableName = "drug_indication",
    primaryKeys = ["drug_id", "indication_name"],
    indices = [Index("drug_id"), Index("indication_name")],
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugIndicationEntity(
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "indication_name") val indicationName: String
)

// ============================================================
// 4. drug_active_metabolite — 活性代谢物
// ============================================================
@Entity(
    tableName = "drug_active_metabolite",
    primaryKeys = ["drug_id", "metabolite_id"],
    indices = [Index("drug_id")],
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugActiveMetaboliteEntity(
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "metabolite_id") val metaboliteId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "activity_ratio") val activityRatio: Double = 1.0,
    @ColumnInfo(name = "note") val note: String? = null
)

// ============================================================
// 5. drug_pk — PK 模型 (1:1 with drug)
// ============================================================
@Entity(
    tableName = "drug_pk",
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugPkEntity(
    @PrimaryKey @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "model_type") val modelType: String,
    @ColumnInfo(name = "ke_per_hour") val kePerHour: Double,
    @ColumnInfo(name = "t_half_hours") val tHalfHours: Double,
    @ColumnInfo(name = "t_half_low") val tHalfLow: Double? = null,
    @ColumnInfo(name = "t_half_high") val tHalfHigh: Double? = null,
    @ColumnInfo(name = "vd_l_per_kg") val vdLPerKg: Double,
    @ColumnInfo(name = "cl_l_per_hour") val clLPerHour: Double,
    @ColumnInfo(name = "k10_per_hour") val k10PerHour: Double? = null,
    @ColumnInfo(name = "k12_per_hour") val k12PerHour: Double? = null,
    @ColumnInfo(name = "k21_per_hour") val k21PerHour: Double? = null,
    @ColumnInfo(name = "k13_per_hour") val k13PerHour: Double? = null,
    @ColumnInfo(name = "k31_per_hour") val k31PerHour: Double? = null,
    @ColumnInfo(name = "v1_liters") val v1Liters: Double? = null
)

// ============================================================
// 6. drug_therapeutic_window — 治疗窗 (0:1)
// ============================================================
@Entity(
    tableName = "drug_therapeutic_window",
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugTherapeuticWindowEntity(
    @PrimaryKey @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "low") val low: Double,
    @ColumnInfo(name = "high") val high: Double,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "guideline_source") val guidelineSource: String? = null
)

// ============================================================
// 7. drug_overdose — 过量 (0:1)
// ============================================================
@Entity(
    tableName = "drug_overdose",
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugOverdoseEntity(
    @PrimaryKey @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "symptoms") val symptoms: String,
    @ColumnInfo(name = "severity") val severity: String,
    @ColumnInfo(name = "toxic_dose_mg") val toxicDoseMg: Double? = null,
    @ColumnInfo(name = "fatal_dose_mg") val fatalDoseMg: Double? = null,
    @ColumnInfo(name = "management") val management: String,
    @ColumnInfo(name = "antidote") val antidote: String? = null,
    @ColumnInfo(name = "data_source") val dataSource: String
)

// ============================================================
// 8. drug_clinical — 临床调整 + 不良反应 + 监测 (0:1, 合并)
// ============================================================
@Entity(
    tableName = "drug_clinical",
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugClinicalEntity(
    @PrimaryKey @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "renal") val renal: String = "NONE",
    @ColumnInfo(name = "hepatic") val hepatic: String = "NONE",
    @ColumnInfo(name = "elderly") val elderly: String? = null,
    @ColumnInfo(name = "smoking_effect") val smokingEffect: String? = null,
    @ColumnInfo(name = "smoking_dose_adjustment") val smokingDoseAdjustment: String? = null,
    @ColumnInfo(name = "smoking_abstinence_note") val smokingAbstinenceNote: String? = null,
    @ColumnInfo(name = "qtc_prolongation") val qtcProlongation: String = "LOW",
    @ColumnInfo(name = "metabolic_syndrome") val metabolicSyndrome: String = "LOW",
    @ColumnInfo(name = "anticholinergic_load") val anticholinergicLoad: Int = 0,
    @ColumnInfo(name = "agranulocytosis") val agranulocytosis: String = "VERY_LOW",
    @ColumnInfo(name = "extrapyramidal") val extrapyramidal: String = "LOW",
    @ColumnInfo(name = "sedation") val sedation: String = "LOW",
    @ColumnInfo(name = "sexual") val sexual: String = "LOW",
    @ColumnInfo(name = "hyperprolactinemia") val hyperprolactinemia: String = "LOW",
    @ColumnInfo(name = "monitoring_frequency") val monitoringFrequency: String? = null,
    @ColumnInfo(name = "monitoring_items_json") val monitoringItemsJson: String = "[]"
)

// ============================================================
// 9. cyp_role — CYP 角色 (1:N, 多态: SUBSTRATE/INHIBITOR/INDUCER)
// ============================================================
@Entity(
    tableName = "cyp_role",
    primaryKeys = ["drug_id", "cyp", "role_type"],
    indices = [Index("drug_id"), Index("cyp"), Index("role_type")],
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CypRoleEntity(
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "cyp") val cyp: String,
    @ColumnInfo(name = "role_type") val roleType: String,
    @ColumnInfo(name = "fraction") val fraction: Double? = null,
    @ColumnInfo(name = "strength") val strength: String? = null
)

// ============================================================
// 10. drug_critical_interaction — 关键相互作用 hint (1:N)
// ============================================================
@Entity(
    tableName = "drug_critical_interaction",
    primaryKeys = ["drug_id", "trigger_drug_id", "mechanism"],
    indices = [Index("drug_id"), Index("trigger_drug_id")],
    foreignKeys = [ForeignKey(
        entity = DrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DrugCriticalInteractionEntity(
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "trigger_drug_id") val triggerDrugId: String,
    @ColumnInfo(name = "mechanism") val mechanism: String,
    @ColumnInfo(name = "auc_fold_low") val aucFoldLow: Double,
    @ColumnInfo(name = "auc_fold_high") val aucFoldHigh: Double,
    @ColumnInfo(name = "severity") val severity: String,
    @ColumnInfo(name = "clinical_note") val clinicalNote: String
)

// ============================================================
// 11. app_metadata — 全局 KV
// ============================================================
@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String
)

// 顶层 const (避免 companion object 跟 KSP 兼容问题)
object AppMetadataKeys {
    const val CATALOG_VERSION = "catalog_version"
    const val CATALOG_GENERATED_AT = "catalog_generated_at"
    const val LAST_SEEDED_AT = "last_seeded_at"
}
