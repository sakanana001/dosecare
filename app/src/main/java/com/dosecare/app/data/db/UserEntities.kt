package com.dosecare.app.data.db

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dosecare.app.R

/**
 * 用户数据表 Entity (v0.7 Phase C 启用, v0.8a Phase F 扩 diary_entry)
 *
 * 7 张表 schema:
 * 1. prescription_group        处方组
 * 2. prescribed_drug           在用药
 * 3. dose_taken                打卡记录
 * 4. tdm_history               TDM 推测历史
 * 5. lab_result                化验结果
 * 6. user_preference           用户设置
 * 7. diary_entry               日记条目 (v0.8a 新增)
 *
 * v0.7 设计: PK 用 String UUID (便于未来云同步)
 *
 * KSP 兼容注意: 外键列(如 prescribed_drug_id)不要同时建 Index 注解,
 *   否则 KSP 报 "MissingType" 错误. 只在非外键列加 Index.
 */

// ============================================================
// 1. prescription_group — 处方组
// ============================================================
@Entity(tableName = "prescription_group")
data class PrescriptionGroupEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_index") val colorIndex: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)

// ============================================================
// 2. prescribed_drug — 在用药
// ============================================================
@Entity(
    tableName = "prescribed_drug",
    indices = [Index("group_id"), Index("drug_id")],
    foreignKeys = [ForeignKey(
        entity = PrescriptionGroupEntity::class,
        parentColumns = ["id"],
        childColumns = ["group_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PrescribedDrugEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "dose_mg") val doseMg: Double,
    @ColumnInfo(name = "weight_kg") val weightKg: Double = 70.0,
    @ColumnInfo(name = "frequency_per_day") val frequencyPerDay: Int,
    @ColumnInfo(name = "start_date") val startDate: Long? = null,
    @ColumnInfo(name = "end_date") val endDate: Long? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true
)

// ============================================================
// 3. dose_taken — 打卡记录 (外键列不加 Index!)
// ============================================================
@Entity(
    tableName = "dose_taken",
    indices = [Index("timestamp")],  // 不要在 prescribed_drug_id 上加 Index, 跟 KSP 冲突
    foreignKeys = [ForeignKey(
        entity = PrescribedDrugEntity::class,
        parentColumns = ["id"],
        childColumns = ["prescribed_drug_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class DoseTakenEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "prescribed_drug_id") val prescribedDrugId: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "dose_mg") val doseMg: Double,
    @ColumnInfo(name = "note") val note: String? = null
)

// ============================================================
// 4. tdm_history — TDM 推测历史
// ============================================================
@Entity(
    tableName = "tdm_history",
    indices = [Index("drug_id"), Index("computed_at")]
)
data class TdmHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "drug_id") val drugId: String,
    @ColumnInfo(name = "prescribed_drug_id") val prescribedDrugId: String? = null,
    @ColumnInfo(name = "dose_mg") val doseMg: Double,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "frequency_per_day") val frequencyPerDay: Int,
    @ColumnInfo(name = "curve_json") val curveJson: String,
    @ColumnInfo(name = "computed_at") val computedAt: Long = System.currentTimeMillis()
)

// ============================================================
// 5. lab_result — 化验结果
// ============================================================
@Entity(
    tableName = "lab_result",
    indices = [Index("analyte"), Index("taken_at")]
)
data class LabResultEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "drug_id") val drugId: String? = null,
    @ColumnInfo(name = "analyte") val analyte: String,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    @ColumnInfo(name = "source") val source: String? = null
)

// ============================================================
// 6. user_preference — 用户设置
// ============================================================
@Entity(tableName = "user_preference")
data class UserPreferenceEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String
)

// ============================================================
// 7. diary_entry — 日记条目 (v0.8a 新增)
// ============================================================
@Entity(
    tableName = "diary_entry",
    indices = [Index("timestamp")]
)
data class DiaryEntryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "mood") val mood: String,    // DiaryMood enum name
    @ColumnInfo(name = "text") val text: String? = null
)

/**
 * 日记心境 (6 选) — 记录当下状态
 *
 * v0.9c: displayName 改 @StringRes displayNameRes, UI 用 stringResource(displayNameRes) 走当前 locale
 */
enum class DiaryMood(@StringRes val displayNameRes: Int, val emoji: String) {
    Happy(R.string.mood_happy, "😊"),
    Calm(R.string.mood_calm, "😌"),
    Anxious(R.string.mood_anxious, "😟"),
    Depressed(R.string.mood_depressed, "😔"),
    Angry(R.string.mood_angry, "😠"),
    Tired(R.string.mood_tired, "😴");

    companion object {
        fun fromName(name: String?): DiaryMood =
            entries.firstOrNull { it.name == name } ?: Calm
    }
}

// 顶层 const
object UserPreferenceKeys {
    const val REMINDER_ENABLED = "reminder_enabled"
    const val LAST_PRESCRIPTION_MIGRATION = "last_prescription_migration"
}
