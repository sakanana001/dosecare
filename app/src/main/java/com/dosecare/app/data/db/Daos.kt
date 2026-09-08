package com.dosecare.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 11 个静态表 Dao (v0.7 Phase B)
 *
 * v0.7 设计原则:
 * - 灌库用: bulk insert (REPLACE 策略覆盖), 一次 transaction 提交
 * - 读取用: suspend fun (单次) 或 Flow (响应式, v0.8 后切)
 * - 计数: 灌库时防漏, 验证 drug count == 202
 */

// ============================================================
// 1. drug Dao
// ============================================================
@Dao
interface DrugDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(drugs: List<DrugEntity>)

    @Query("SELECT * FROM drug ORDER BY generic_name_zh")
    suspend fun getAll(): List<DrugEntity>

    @Query("SELECT * FROM drug WHERE id = :id")
    suspend fun getById(id: String): DrugEntity?

    @Query("SELECT * FROM drug WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DrugEntity>

    @Query("SELECT * FROM drug WHERE category = :category ORDER BY generic_name_zh")
    suspend fun getByCategory(category: String): List<DrugEntity>

    @Query(
        "SELECT * FROM drug WHERE " +
        "generic_name_zh LIKE '%' || :q || '%' OR " +
        "generic_name LIKE '%' || :q || '%' OR " +
        "id LIKE '%' || :q || '%' " +
        "ORDER BY generic_name_zh LIMIT :limit"
    )
    suspend fun search(q: String, limit: Int = 50): List<DrugEntity>

    @Query("SELECT COUNT(*) FROM drug")
    suspend fun count(): Int

    @Query("DELETE FROM drug")
    suspend fun deleteAll()
}

// ============================================================
// 2. drug_brand
// ============================================================
@Dao
interface DrugBrandDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(brands: List<DrugBrandEntity>)

    @Query("SELECT brand_name FROM drug_brand WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): List<String>

    @Query("DELETE FROM drug_brand")
    suspend fun deleteAll()
}

// ============================================================
// 3. drug_indication
// ============================================================
@Dao
interface DrugIndicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugIndicationEntity>)

    @Query("SELECT indication_name FROM drug_indication WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): List<String>

    @Query("SELECT DISTINCT drug_id FROM drug_indication WHERE indication_name = :indicationName")
    suspend fun getDrugIdsByIndication(indicationName: String): List<String>

    @Query("DELETE FROM drug_indication")
    suspend fun deleteAll()
}

// ============================================================
// 4. drug_active_metabolite
// ============================================================
@Dao
interface DrugActiveMetaboliteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugActiveMetaboliteEntity>)

    @Query("SELECT * FROM drug_active_metabolite WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): List<DrugActiveMetaboliteEntity>

    @Query("DELETE FROM drug_active_metabolite")
    suspend fun deleteAll()
}

// ============================================================
// 5. drug_pk
// ============================================================
@Dao
interface DrugPkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugPkEntity>)

    @Query("SELECT * FROM drug_pk WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): DrugPkEntity?

    @Query("DELETE FROM drug_pk")
    suspend fun deleteAll()
}

// ============================================================
// 6. drug_therapeutic_window
// ============================================================
@Dao
interface DrugTherapeuticWindowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugTherapeuticWindowEntity>)

    @Query("SELECT * FROM drug_therapeutic_window WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): DrugTherapeuticWindowEntity?

    @Query("DELETE FROM drug_therapeutic_window")
    suspend fun deleteAll()
}

// ============================================================
// 7. drug_overdose
// ============================================================
@Dao
interface DrugOverdoseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugOverdoseEntity>)

    @Query("SELECT * FROM drug_overdose WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): DrugOverdoseEntity?

    @Query("DELETE FROM drug_overdose")
    suspend fun deleteAll()
}

// ============================================================
// 8. drug_clinical
// ============================================================
@Dao
interface DrugClinicalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugClinicalEntity>)

    @Query("SELECT * FROM drug_clinical WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): DrugClinicalEntity?

    @Query("DELETE FROM drug_clinical")
    suspend fun deleteAll()
}

// ============================================================
// 9. cyp_role (多态: SUBSTRATE/INHIBITOR/INDUCER)
// ============================================================
@Dao
interface CypRoleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CypRoleEntity>)

    @Query("SELECT * FROM cyp_role WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): List<CypRoleEntity>

    @Query("SELECT * FROM cyp_role WHERE cyp = :cyp AND role_type = :roleType")
    suspend fun findByCypAndRole(cyp: String, roleType: String): List<CypRoleEntity>

    @Query("DELETE FROM cyp_role")
    suspend fun deleteAll()
}

// ============================================================
// 10. drug_critical_interaction
// ============================================================
@Dao
interface DrugCriticalInteractionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DrugCriticalInteractionEntity>)

    @Query("SELECT * FROM drug_critical_interaction WHERE drug_id = :drugId")
    suspend fun getByDrugId(drugId: String): List<DrugCriticalInteractionEntity>

    @Query("DELETE FROM drug_critical_interaction")
    suspend fun deleteAll()
}

// ============================================================
// 11. app_metadata
// ============================================================
@Dao
interface AppMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AppMetadataEntity>)

    @Query("SELECT value FROM app_metadata WHERE key = :key")
    suspend fun get(key: String): String?

    @Query("SELECT * FROM app_metadata")
    suspend fun getAll(): List<AppMetadataEntity>

    @Query("DELETE FROM app_metadata")
    suspend fun deleteAll()
}
