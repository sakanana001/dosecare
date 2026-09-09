package com.dosecare.app.data.migration

import android.content.Context
import android.util.Log
import com.dosecare.app.data.db.DoseTakenEntity
import com.dosecare.app.data.db.PrescribedDrugDao
import com.dosecare.app.data.db.PrescribedDrugEntity
import com.dosecare.app.data.db.PrescriptionGroupDao
import com.dosecare.app.data.db.PrescriptionGroupEntity
import com.dosecare.app.data.db.UserPreferenceDao
import com.dosecare.app.data.db.UserPreferenceEntity
import com.dosecare.app.data.db.UserPreferenceKeys
import com.dosecare.app.domain.prescription.PrescriptionRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * 把 v0.6 SharedPreferences-based 处方迁移到 Room
 *
 * 两阶段 (各跑一次, idempotent):
 * - v0.7 first run:   第一次 Room 启动, 把 SharedPreferences 数据拷到 Room (snapshot)
 * - v0.9g final run:  v0.9g 切换 UI 到 Room, 终极迁移 (把 SharedPreferences 最新数据覆写 Room)
 *                     v0.7 之后用户用 SharedPreferences 加/改的所有内容都在这里
 *
 * 标记:
 * - LAST_PRESCRIPTION_MIGRATION       v0.7 first run 标志
 * - LAST_PRESCRIPTION_V09G_MIGRATION  v0.9g final run 标志
 */
object PrescriptionMigrator {
    private const val TAG = "PrescriptionMigrator"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    /**
     * 入口: 一次启动跑 2 个 migration, 各自 idempotent
     */
    suspend fun migrateIfNeeded(
        context: Context,
        groupDao: PrescriptionGroupDao,
        drugDao: PrescribedDrugDao,
        doseDao: com.dosecare.app.data.db.DoseTakenDao,
        prefDao: UserPreferenceDao
    ) {
        migrateV07IfNeeded(context, groupDao, drugDao, doseDao, prefDao)
        migrateV09gFinalIfNeeded(context, groupDao, drugDao, doseDao, prefDao)
    }

    /**
     * v0.7 第一次迁移: 跟旧版完全一样
     */
    private suspend fun migrateV07IfNeeded(
        context: Context,
        groupDao: PrescriptionGroupDao,
        drugDao: PrescribedDrugDao,
        doseDao: com.dosecare.app.data.db.DoseTakenDao,
        prefDao: UserPreferenceDao
    ) {
        val migratedFlag = prefDao.get(UserPreferenceKeys.LAST_PRESCRIPTION_MIGRATION)
        if (migratedFlag != null) {
            Log.i(TAG, "v0.7 already migrated at $migratedFlag, skipping")
            return
        }

        val legacy: PrescriptionRepository = PrescriptionRepository(context)
        val groups: List<com.dosecare.app.domain.prescription.PrescriptionGroup> =
            runCatching { legacy.loadAll() }.getOrElse {
                Log.e(TAG, "Failed to read legacy SharedPreferences (v0.7)", it)
                emptyList()
            }
        if (groups.isEmpty()) {
            Log.i(TAG, "v0.7: no legacy data, marking migrated")
            prefDao.upsert(
                UserPreferenceEntity(
                    UserPreferenceKeys.LAST_PRESCRIPTION_MIGRATION,
                    System.currentTimeMillis().toString()
                )
            )
            return
        }

        writeGroupsToRoom(groups, groupDao, drugDao, doseDao, "v0.7")

        prefDao.upsert(
            UserPreferenceEntity(
                UserPreferenceKeys.LAST_PRESCRIPTION_MIGRATION,
                System.currentTimeMillis().toString()
            )
        )
    }

    /**
     * v0.9g 终极迁移: 把 SharedPreferences 最新数据覆写 Room
     * - v0.7 之后 UI 一直用 SharedPreferences, 期间用户加的 drug / 改的 times / 加的打卡都没进 Room
     * - v0.9g 把 UI 切到 Room 之前, 必须把 SharedPreferences 整盘重迁一次, 保证 Room 是最新数据
     * - 标志位独立, 跟 v0.7 的不冲突 (v0.7 已跑过的会 skip, 没跑过的会先跑 v0.7)
     */
    private suspend fun migrateV09gFinalIfNeeded(
        context: Context,
        groupDao: PrescriptionGroupDao,
        drugDao: PrescribedDrugDao,
        doseDao: com.dosecare.app.data.db.DoseTakenDao,
        prefDao: UserPreferenceDao
    ) {
        val flag = prefDao.get(UserPreferenceKeys.LAST_PRESCRIPTION_V09G_MIGRATION)
        if (flag != null) {
            Log.i(TAG, "v0.9g final migration already done at $flag, skipping")
            return
        }

        val legacy: PrescriptionRepository = PrescriptionRepository(context)
        val groups: List<com.dosecare.app.domain.prescription.PrescriptionGroup> =
            runCatching { legacy.loadAll() }.getOrElse {
                Log.e(TAG, "Failed to read legacy SharedPreferences (v0.9g)", it)
                emptyList()
            }
        if (groups.isEmpty()) {
            Log.i(TAG, "v0.9g: no legacy data, marking migrated")
            prefDao.upsert(
                UserPreferenceEntity(
                    UserPreferenceKeys.LAST_PRESCRIPTION_V09G_MIGRATION,
                    System.currentTimeMillis().toString()
                )
            )
            return
        }

        // 清掉旧 drug + dose (保留 group id), 重建 drug + dose, 这样保留 doseTaken 历史
        //   - drug 改 times 字段是迁移时按 frequencyPerDay 推默认时段
        //   - 实际场景: UI 切到 Room 后用户用 Room 改 times, ReminderScheduler 会读到
        val allOldDrugs = drugDao.getAll()
        allOldDrugs.forEach { drugDao.deleteById(it.id) }
        // dose_taken 有外键 CASCADE 到 prescribed_drug, drug 删了 dose 也清, 下面再重建
        writeGroupsToRoom(groups, groupDao, drugDao, doseDao, "v0.9g-final")

        prefDao.upsert(
            UserPreferenceEntity(
                UserPreferenceKeys.LAST_PRESCRIPTION_V09G_MIGRATION,
                System.currentTimeMillis().toString()
            )
        )
    }

    private suspend fun writeGroupsToRoom(
        groups: List<com.dosecare.app.domain.prescription.PrescriptionGroup>,
        groupDao: PrescriptionGroupDao,
        drugDao: PrescribedDrugDao,
        doseDao: com.dosecare.app.data.db.DoseTakenDao,
        phase: String
    ) {
        val groupEntities: List<PrescriptionGroupEntity> = groups.map { g ->
            PrescriptionGroupEntity(
                id = g.id,
                name = g.name,
                colorIndex = g.colorIndex,
                createdAt = g.createdAt,
                sortOrder = 0
            )
        }
        val drugEntities: List<PrescribedDrugEntity> = groups.flatMap { g ->
            g.drugs.map { d ->
                PrescribedDrugEntity(
                    id = d.id,
                    groupId = g.id,
                    drugId = d.drugId,
                    doseMg = d.defaultDoseMg,
                    weightKg = 70.0,
                    frequencyPerDay = d.frequencyPerDay,
                    times = serializeTimesJson(d.times),
                    startDate = if (d.startDate > 0) d.startDate else null,
                    endDate = null,
                    targetDate = d.targetDate,
                    notes = null,
                    active = true
                )
            }
        }
        val doseEntities: List<DoseTakenEntity> = groups.flatMap { g ->
            g.drugs.flatMap { d ->
                d.dosesTaken.map { dt ->
                    DoseTakenEntity(
                        id = UUID.randomUUID().toString(),
                        prescribedDrugId = d.id,
                        timestamp = dt.timestamp,
                        doseMg = dt.doseMg,
                        note = dt.note
                    )
                }
            }
        }

        runCatching {
            groupEntities.forEach { groupDao.upsert(it) }
            drugEntities.forEach { drugDao.upsert(it) }
            doseEntities.forEach { doseDao.upsert(it) }
        }.onFailure {
            Log.e(TAG, "Migration $phase failed", it)
            return
        }

        Log.i(TAG, "$phase: migrated ${groups.size} groups, ${drugEntities.size} drugs, ${doseEntities.size} doses")
    }

    /**
     * ["08:00", "20:00"] → "[\"08:00\",\"20:00\"]"; 空 → "[]"
     */
    private fun serializeTimesJson(times: List<String>): String {
        if (times.isEmpty()) return "[]"
        val arr = JsonArray(times.map { JsonPrimitive(it) })
        return arr.toString()
    }
}
