package com.dosecare.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DrugEntity::class,
        DrugBrandEntity::class,
        DrugIndicationEntity::class,
        DrugActiveMetaboliteEntity::class,
        DrugPkEntity::class,
        DrugTherapeuticWindowEntity::class,
        DrugOverdoseEntity::class,
        DrugClinicalEntity::class,
        CypRoleEntity::class,
        DrugCriticalInteractionEntity::class,
        AppMetadataEntity::class,
        PrescriptionGroupEntity::class,
        PrescribedDrugEntity::class,
        DoseTakenEntity::class,
        TdmHistoryEntity::class,
        LabResultEntity::class,
        UserPreferenceEntity::class,
        DiaryEntryEntity::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drugDao(): DrugDao
    abstract fun drugBrandDao(): DrugBrandDao
    abstract fun drugIndicationDao(): DrugIndicationDao
    abstract fun drugActiveMetaboliteDao(): DrugActiveMetaboliteDao
    abstract fun drugPkDao(): DrugPkDao
    abstract fun drugTherapeuticWindowDao(): DrugTherapeuticWindowDao
    abstract fun drugOverdoseDao(): DrugOverdoseDao
    abstract fun drugClinicalDao(): DrugClinicalDao
    abstract fun cypRoleDao(): CypRoleDao
    abstract fun drugCriticalInteractionDao(): DrugCriticalInteractionDao
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun prescriptionGroupDao(): PrescriptionGroupDao
    abstract fun prescribedDrugDao(): PrescribedDrugDao
    abstract fun doseTakenDao(): DoseTakenDao
    abstract fun tdmHistoryDao(): TdmHistoryDao
    abstract fun labResultDao(): LabResultDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun diaryDao(): DiaryDao

    companion object {
        const val DB_NAME = "dosecare.db"

        /**
         * v0.9g Migration 4 → 5: prescribed_drug 加 2 字段
         * - times:       JSON 序列化的 List<String> "HH:mm", 默认 "[]" (= 按 frequencyPerDay 用默认时段)
         * - target_date: 临时用药目标日期 (epoch day 0:00 ms), null = 周期用药
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prescribed_drug ADD COLUMN times TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE prescribed_drug ADD COLUMN target_date INTEGER")
            }
        }
    }
}
