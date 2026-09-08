package app.repository

import catalog.Drug
import catalog.DrugCatalogService
import kotlinx.coroutines.flow.Flow
import patient.PatientProfile
import rules.Interaction
import rules.UserDrugForRule

/**
 * Repository 接口（KMP-ready，预留多端复用）
 *
 * 关键设计：
 * - 接口在 domain 层（这里只是定义，实现在 data 层）
 * - LocalFirst 实现用 Room
 * - OnlineSecond 实现（未来）用 HTTP client + 加密 blob
 * - 切换由 UseCase 决定，ViewModel 不感知
 *
 * 全部方法返回 Flow 或 suspend，KMP 友好。
 */

/**
 * 药物目录访问
 */
interface DrugCatalogRepository {
    fun observeCatalog(): Flow<DrugCatalogService>
    suspend fun getById(id: String): Drug
    suspend fun search(query: String, limit: Int = 20): List<Drug>
}

/**
 * 用户在用药物
 */
interface UserDrugsRepository {
    fun observeActive(): Flow<List<UserDrugForRule>>
    suspend fun add(drugId: String, doseMg: Double, frequencyPerDay: Double)
    suspend fun update(userDrug: UserDrugForRule)
    suspend fun remove(id: String)
}

/**
 * 服药记录
 */
interface IntakeRecordsRepository {
    fun observeRecent(limit: Int = 50): Flow<List<IntakeRecord>>
    suspend fun recordIntake(userDrugId: Long, doseMg: Double, atMillis: Long)
}

/**
 * 化验结果
 */
interface LabResultsRepository {
    fun observeAll(): Flow<List<LabResult>>
    suspend fun add(analyte: String, value: Double, unit: String, atMillis: Long)
}

/**
 * 患者画像
 */
interface PatientProfileRepository {
    fun observe(): Flow<PatientProfile>
    suspend fun update(profile: PatientProfile)
}

/**
 * 相互作用评估（聚合根）
 */
interface InteractionsRepository {
    fun observe(): Flow<List<Interaction>>
    suspend fun reEvaluate()
}

/**
 * 同步（未来）
 */
interface SyncRepository {
    val isEnabled: Boolean
    suspend fun push(): SyncResult
    suspend fun pull(): SyncResult
}

data class SyncResult(
    val success: Boolean,
    val uploaded: Int,
    val downloaded: Int,
    val error: String? = null
)

// ============================================================
// 下面是 data record 定义（实际写在 data 层的 Room entity 中）
// 这里只放传递用的纯数据类
// ============================================================

data class IntakeRecord(
    val id: Long = 0,
    val userDrugId: Long,
    val actualAtMillis: Long,
    val doseMg: Double,
    val status: IntakeStatus
)

enum class IntakeStatus { TAKEN, MISSED, SKIPPED, PARTIAL }

data class LabResult(
    val id: Long = 0,
    val analyte: String,
    val value: Double,
    val unit: String,
    val measuredAtMillis: Long
)
