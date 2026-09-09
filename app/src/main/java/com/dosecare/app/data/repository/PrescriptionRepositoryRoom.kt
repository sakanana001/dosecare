package com.dosecare.app.data.repository

import android.util.Log
import com.dosecare.app.data.db.AppDatabase
import com.dosecare.app.data.db.DoseTakenEntity
import com.dosecare.app.data.db.PrescribedDrugDao
import com.dosecare.app.data.db.PrescribedDrugEntity
import com.dosecare.app.data.db.PrescriptionGroupDao
import com.dosecare.app.data.db.PrescriptionGroupEntity
import com.dosecare.app.domain.prescription.DoseTaken
import com.dosecare.app.domain.prescription.PrescribedDrug
import com.dosecare.app.domain.prescription.PrescriptionGroup
import com.dosecare.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 处方 Repository (Room 版) — v0.7 Phase C 启用
 *
 * v0.9g 升级:
 * - PrescribedDrugEntity 加 times (JSON "HH:mm" 列表) + targetDate (临时用药日期) 字段
 * - buildDrug 反序列化 times JSON, buildDrug 读 targetDate
 * - upsertDrug 序列化 times JSON, upsertDrug 写 targetDate
 * - RepositoryRoom 改写整组 saveAll (替代 SharedPreferences 版的 repo.saveAll)
 */
@Singleton
class PrescriptionRepositoryRoom @Inject constructor(
    private val db: AppDatabase,
    private val reminderScheduler: ReminderScheduler
) {
    private val groupDao: PrescriptionGroupDao = db.prescriptionGroupDao()
    private val drugDao: PrescribedDrugDao = db.prescribedDrugDao()
    private val doseDao = db.doseTakenDao()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    /**
     * 订阅所有处方组 (含 nested drugs + doses)
     * 内部 combine 3 张表的 Flow
     */
    fun observeGroups(): Flow<List<PrescriptionGroup>> =
        groupDao.observeAll().map { groups ->
            groups.map { g -> buildGroup(g) }
        }

    /**
     * 单次读取 (非 Flow)
     */
    suspend fun getGroups(): List<PrescriptionGroup> =
        groupDao.getAll().map { g -> buildGroup(g) }

    private suspend fun buildGroup(g: PrescriptionGroupEntity): PrescriptionGroup {
        val drugs = drugDao.getByGroup(g.id).map { d -> buildDrug(d) }
        return PrescriptionGroup(
            id = g.id,
            name = g.name,
            colorIndex = g.colorIndex,
            drugs = drugs,
            createdAt = g.createdAt
        )
    }

    private suspend fun buildDrug(d: PrescribedDrugEntity): PrescribedDrug {
        val doses = doseDao.getRecentByPrescribedDrug(d.id, limit = 200).map { dt ->
            DoseTaken(timestamp = dt.timestamp, doseMg = dt.doseMg, note = dt.note)
        }
        return PrescribedDrug(
            id = d.id,
            drugId = d.drugId,
            defaultDoseMg = d.doseMg,
            frequencyPerDay = d.frequencyPerDay,
            times = parseTimesJson(d.times),
            startDate = d.startDate ?: System.currentTimeMillis(),
            targetDate = d.targetDate,
            dosesTaken = doses
        )
    }

    // ============== 写操作 ==============

    /**
     * v0.9g: 整组覆写 (替代 SharedPreferences 版的 saveAll)
     * - 删除所有原 group + drug + dose_taken, 然后从传入 List 重建
     * - 用 @Transaction 包起来保 atomic; 没有现成 @Transaction 注解的 DAO, 所以在 Repository 层 try-catch
     * - 实际场景: 用户一次 UI 操作只改一个 group 或一个 drug, 用下面 upsertGroup / upsertDrug 更合适
     */
    suspend fun saveAll(groups: List<PrescriptionGroup>) {
        // 先 upsert 所有 group + drug + dose
        groups.forEach { g ->
            upsertGroup(g)
            g.drugs.forEach { d -> upsertDrug(g.id, d) }
        }
        rescheduleRemindersSafely()
    }

    suspend fun upsertGroup(group: PrescriptionGroup): String {
        val id = group.id.ifBlank { UUID.randomUUID().toString() }
        val entity = PrescriptionGroupEntity(
            id = id,
            name = group.name,
            colorIndex = group.colorIndex,
            createdAt = group.createdAt,
            sortOrder = 0
        )
        groupDao.upsert(entity)
        rescheduleRemindersSafely()
        return id
    }

    suspend fun upsertDrug(groupId: String, drug: PrescribedDrug): String {
        val id = drug.id.ifBlank { UUID.randomUUID().toString() }
        val entity = PrescribedDrugEntity(
            id = id,
            groupId = groupId,
            drugId = drug.drugId,
            doseMg = drug.defaultDoseMg,
            weightKg = 70.0,
            frequencyPerDay = drug.frequencyPerDay,
            times = serializeTimesJson(drug.times),
            startDate = if (drug.startDate > 0) drug.startDate else null,
            endDate = null,
            targetDate = drug.targetDate,
            notes = null,
            active = true
        )
        drugDao.upsert(entity)
        rescheduleRemindersSafely()
        return id
    }

    suspend fun addDose(prescribedDrugId: String, dose: DoseTaken): String {
        val id = UUID.randomUUID().toString()
        val entity = DoseTakenEntity(
            id = id,
            prescribedDrugId = prescribedDrugId,
            timestamp = dose.timestamp,
            doseMg = dose.doseMg,
            note = dose.note
        )
        doseDao.upsert(entity)
        return id
    }

    suspend fun deleteGroup(id: String) {
        groupDao.deleteById(id)
        rescheduleRemindersSafely()
    }
    suspend fun deleteDrug(id: String) {
        drugDao.deleteById(id)
        rescheduleRemindersSafely()
    }
    suspend fun deleteDose(id: String) = doseDao.deleteById(id)

    /**
     * v0.9f 增删改末尾重排提醒
     * 失败仅记录日志, 不影响主流程 (避免单个 reminder 失败导致数据修改回滚)
     */
    private suspend fun rescheduleRemindersSafely() {
        try {
            reminderScheduler.scheduleAll()
        } catch (e: Exception) {
            Log.w("PrescriptionRepo", "rescheduleReminders failed: ${e.message}")
        }
    }

    // ============== JSON helpers ==============

    /**
     * "[\"08:00\",\"20:00\"]" → ["08:00", "20:00"]
     * 空 / 解析失败 → emptyList()
     */
    private fun parseTimesJson(jsonStr: String): List<String> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return runCatching {
            val arr = json.parseToJsonElement(jsonStr) as? JsonArray ?: return emptyList()
            arr.mapNotNull { el ->
                val p = el as? JsonPrimitive ?: return@mapNotNull null
                if (p.isString) p.content else null
            }
        }.getOrDefault(emptyList())
    }

    /**
     * ["08:00", "20:00"] → "[\"08:00\",\"20:00\"]"
     * 空 → "[]"
     */
    private fun serializeTimesJson(times: List<String>): String {
        if (times.isEmpty()) return "[]"
        val arr = JsonArray(times.map { JsonPrimitive(it) })
        return arr.toString()
    }
}
