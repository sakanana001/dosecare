package com.dosecare.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

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
    version = 4,
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
    }
}
