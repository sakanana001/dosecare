package com.dosecare.app.data.repository

import com.dosecare.app.data.db.AppDatabase
import com.dosecare.app.data.db.DoseTakenEntity
import com.dosecare.app.data.db.PrescribedDrugDao
import com.dosecare.app.data.db.PrescribedDrugEntity
import com.dosecare.app.data.db.PrescriptionGroupDao
import com.dosecare.app.data.db.PrescriptionGroupEntity
import com.dosecare.app.domain.prescription.DoseTaken
import com.dosecare.app.domain.prescription.PrescribedDrug
import com.dosecare.app.domain.prescription.PrescriptionGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 处方 Repository (Room 版) — v0.7 Phase C 启用
 *
 * 替换原来的 SharedPreferences 版本 (PrescriptionRepository in domain/prescription)
 *
 * 暴露 Flow + suspend 风格 API, UI 用 collectAsState() 订阅
 *
 * v0.7 设计: 3 张表 (group + drug + dose_taken) 在内存里 combine 拼成 nested domain 对象
 *   - 比 SQL JOIN 简单, 列表规模小 (个人用户 < 100 个处方), 性能足够
 *   - 也是这个标准模式
 */
@Singleton
class PrescriptionRepositoryRoom @Inject constructor(
    private val db: AppDatabase
) {
    private val groupDao: PrescriptionGroupDao = db.prescriptionGroupDao()
    private val drugDao: PrescribedDrugDao = db.prescribedDrugDao()
    private val doseDao = db.doseTakenDao()

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
            startDate = d.startDate ?: System.currentTimeMillis(),
            dosesTaken = doses
        )
    }

    // ============== 写操作 ==============

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
            startDate = if (drug.startDate > 0) drug.startDate else null,
            endDate = null,
            notes = null,
            active = true
        )
        drugDao.upsert(entity)
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

    suspend fun deleteGroup(id: String) = groupDao.deleteById(id)
    suspend fun deleteDrug(id: String) = drugDao.deleteById(id)
    suspend fun deleteDose(id: String) = doseDao.deleteById(id)
}
