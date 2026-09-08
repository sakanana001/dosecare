package com.dosecare.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: PrescriptionGroupEntity)

    @Update
    suspend fun update(group: PrescriptionGroupEntity)

    @Query("DELETE FROM prescription_group WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM prescription_group ORDER BY sort_order, created_at")
    fun observeAll(): Flow<List<PrescriptionGroupEntity>>

    @Query("SELECT * FROM prescription_group ORDER BY sort_order, created_at")
    suspend fun getAll(): List<PrescriptionGroupEntity>

    @Query("SELECT COUNT(*) FROM prescription_group")
    suspend fun count(): Int
}

@Dao
interface PrescribedDrugDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(drug: PrescribedDrugEntity)

    @Update
    suspend fun update(drug: PrescribedDrugEntity)

    @Query("DELETE FROM prescribed_drug WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM prescribed_drug WHERE group_id = :groupId ORDER BY id")
    fun observeByGroup(groupId: String): Flow<List<PrescribedDrugEntity>>

    @Query("SELECT * FROM prescribed_drug WHERE group_id = :groupId ORDER BY id")
    suspend fun getByGroup(groupId: String): List<PrescribedDrugEntity>

    @Query("SELECT * FROM prescribed_drug WHERE id = :id")
    suspend fun getById(id: String): PrescribedDrugEntity?

    @Query("SELECT * FROM prescribed_drug ORDER BY id")
    fun observeAll(): Flow<List<PrescribedDrugEntity>>

    @Query("SELECT * FROM prescribed_drug ORDER BY id")
    suspend fun getAll(): List<PrescribedDrugEntity>

    @Query("SELECT COUNT(*) FROM prescribed_drug")
    suspend fun count(): Int
}

@Dao
interface DoseTakenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dose: DoseTakenEntity)

    @Query("DELETE FROM dose_taken WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM dose_taken WHERE prescribed_drug_id = :prescribedDrugId ORDER BY timestamp DESC")
    fun observeByPrescribedDrug(prescribedDrugId: String): Flow<List<DoseTakenEntity>>

    @Query("SELECT * FROM dose_taken WHERE prescribed_drug_id = :prescribedDrugId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByPrescribedDrug(prescribedDrugId: String, limit: Int = 50): List<DoseTakenEntity>

    @Query("SELECT * FROM dose_taken WHERE prescribed_drug_id = :prescribedDrugId AND timestamp >= :sinceMillis ORDER BY timestamp DESC")
    suspend fun getSince(prescribedDrugId: String, sinceMillis: Long): List<DoseTakenEntity>

    @Query("SELECT * FROM dose_taken WHERE timestamp >= :sinceMillis ORDER BY timestamp DESC")
    fun observeSince(sinceMillis: Long): Flow<List<DoseTakenEntity>>

    @Query("SELECT COUNT(*) FROM dose_taken")
    suspend fun count(): Int
}

@Dao
interface TdmHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: TdmHistoryEntity)

    @Query("SELECT * FROM tdm_history ORDER BY computed_at DESC")
    fun observeAll(): Flow<List<TdmHistoryEntity>>

    @Query("SELECT * FROM tdm_history WHERE drug_id = :drugId ORDER BY computed_at DESC LIMIT :limit")
    suspend fun getRecentForDrug(drugId: String, limit: Int = 20): List<TdmHistoryEntity>

    @Query("DELETE FROM tdm_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM tdm_history")
    suspend fun count(): Int
}

@Dao
interface LabResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: LabResultEntity)

    @Query("SELECT * FROM lab_result ORDER BY taken_at DESC")
    fun observeAll(): Flow<List<LabResultEntity>>

    @Query("SELECT * FROM lab_result WHERE drug_id = :drugId ORDER BY taken_at DESC")
    fun observeForDrug(drugId: String): Flow<List<LabResultEntity>>

    @Query("SELECT * FROM lab_result WHERE analyte = :analyte ORDER BY taken_at DESC")
    fun observeForAnalyte(analyte: String): Flow<List<LabResultEntity>>

    @Query("DELETE FROM lab_result WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM lab_result")
    suspend fun count(): Int
}

@Dao
interface UserPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: UserPreferenceEntity)

    @Query("SELECT value FROM user_preference WHERE key = :key")
    suspend fun get(key: String): String?

    @Query("SELECT * FROM user_preference")
    suspend fun getAll(): List<UserPreferenceEntity>

    @Query("DELETE FROM user_preference WHERE key = :key")
    suspend fun delete(key: String)
}

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entry WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM diary_entry ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entry WHERE timestamp >= :sinceMillis AND timestamp < :untilMillis ORDER BY timestamp ASC")
    fun observeBetween(sinceMillis: Long, untilMillis: Long): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entry WHERE timestamp >= :sinceMillis AND timestamp < :untilMillis ORDER BY timestamp ASC")
    suspend fun getBetween(sinceMillis: Long, untilMillis: Long): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entry WHERE timestamp >= :sinceMillis ORDER BY timestamp ASC")
    fun observeSince(sinceMillis: Long): Flow<List<DiaryEntryEntity>>

    @Query("SELECT COUNT(*) FROM diary_entry")
    suspend fun count(): Int
}
