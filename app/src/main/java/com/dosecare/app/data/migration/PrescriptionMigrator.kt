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
import java.util.UUID

// Migrates v0.6 SharedPreferences-based prescription to Room.
// Idempotent: skips if migration already done.
object PrescriptionMigrator {
    private const val TAG = "PrescriptionMigrator"

    suspend fun migrateIfNeeded(
        context: Context,
        groupDao: PrescriptionGroupDao,
        drugDao: PrescribedDrugDao,
        doseDao: com.dosecare.app.data.db.DoseTakenDao,
        prefDao: UserPreferenceDao
    ) {
        // 1. Skip if already migrated
        val migratedFlag = prefDao.get(UserPreferenceKeys.LAST_PRESCRIPTION_MIGRATION)
        if (migratedFlag != null) {
            Log.i(TAG, "Prescription already migrated at $migratedFlag, skipping")
            return
        }

        // 2. Read legacy data
        val legacy: PrescriptionRepository = PrescriptionRepository(context)
        val groups: List<com.dosecare.app.domain.prescription.PrescriptionGroup> =
            runCatching { legacy.loadAll() }.getOrElse {
                Log.e(TAG, "Failed to read legacy SharedPreferences", it)
                emptyList()
            }
        if (groups.isEmpty()) {
            Log.i(TAG, "No legacy prescription data, marking migrated")
            prefDao.upsert(
                UserPreferenceEntity(
                    UserPreferenceKeys.LAST_PRESCRIPTION_MIGRATION,
                    System.currentTimeMillis().toString()
                )
            )
            return
        }

        // 3. Flatten into 3 tables (preserve UUIDs from v0.6)
        val groupEntities: List<PrescriptionGroupEntity> = groups.map { g: com.dosecare.app.domain.prescription.PrescriptionGroup ->
            PrescriptionGroupEntity(
                id = g.id,
                name = g.name,
                colorIndex = g.colorIndex,
                createdAt = g.createdAt,
                sortOrder = 0
            )
        }
        val drugEntities: List<PrescribedDrugEntity> = groups.flatMap { g ->
            g.drugs.map { d: com.dosecare.app.domain.prescription.PrescribedDrug ->
                PrescribedDrugEntity(
                    id = d.id,
                    groupId = g.id,
                    drugId = d.drugId,
                    doseMg = d.defaultDoseMg,
                    weightKg = 70.0,
                    frequencyPerDay = d.frequencyPerDay,
                    startDate = if (d.startDate > 0) d.startDate else null,
                    endDate = null,
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

        // 4. Write to DB
        runCatching {
            groupEntities.forEach { groupDao.upsert(it) }
            drugEntities.forEach { drugDao.upsert(it) }
            doseEntities.forEach { doseDao.upsert(it) }
        }.onFailure {
            Log.e(TAG, "Migration failed", it)
            return
        }

        // 5. Mark done
        prefDao.upsert(
            UserPreferenceEntity(
                UserPreferenceKeys.LAST_PRESCRIPTION_MIGRATION,
                System.currentTimeMillis().toString()
            )
        )
        Log.i(TAG, "Migrated ${groups.size} groups, ${drugEntities.size} drugs, ${doseEntities.size} doses")
    }
}
