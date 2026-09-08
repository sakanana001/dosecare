# P1-01 · Room + SQLCipher 接入方案

> **任务**：把用户数据（`UserDrug` / `IntakeRecord` / `LabResult` / `PatientProfile` / `InteractionLog` / `OutboxEvent`）从「概念定义」落成「加密 Room 数据库」，并把 `prototype/Repository.kt` 的 6 个接口用 LocalFirst 实现接进 Hilt。
> **范围**：只写方案，不写实现。给 coder 一份能直接照着抄命令 + 抄类签名的施工图。
> **不变量**（违反即返工）：离线优先 / 端侧加密 / 零账号 / KMP-ready / Repository 接口在 domain 层。

---

## 0. 决策一览（先看这页）

| 决策点 | 选择 | 一句话理由 |
|---|---|---|
| Room 注解处理器 | KSP（已配） | KAPT 在 Kotlin 2.0 已弃用，构建慢 2-3x |
| SQLCipher 集成方式 | `SupportFactory(passphrase)` 注入 Room | SQLCipher 官方推荐做法，4.6.1 稳定 |
| 本地 DB 加密密钥 | **设备绑定 256-bit 随机密钥**，由 Android Keystore 硬件保护 | 用户不需记密码、设备丢失 = 数据不可读（合理 trade-off） |
| 备份导出密钥 | 用户密码 → PBKDF2-HMAC-SHA256（100k iter, 32B salt）→ AES-256-GCM | 备份是用户资产，跨设备可读，靠密码保护 |
| 数据库位置 | `Context.getDatabasePath("dosecare.db")`（应用沙箱） | 卸载即销毁，无需额外清理 |
| 域层/数据层切分 | 接口在 `prototype/`，实体在 `data/local/entity/`，Mapper 在 `data/mapper/` | 严格遵循 `01-ARCHITECTURE.md` §1 |
| `IntakeRecord.userDrugId` | 用索引，**不**用外键 | 允许删 UserDrug 后保留历史打卡（医药审计需要） |
| `OutboxEvent` 写入时机 | 在 Repository 写操作的**同一个 transaction** 里 | 否则崩溃/断电会丢事件，未来同步会漏 |
| 单元测试 DB | `Room.inMemoryDatabaseBuilder`（不加密）跑业务；SQLCipher 路径用**真机/集成测试**单独覆盖 | 业务测速度，密码学测覆盖率 |

**唯一会需要用户裁决的事**（在第 6 节）：如果用户要求「我换设备/重装 APP 后还能用同一份本地数据」，那本地密钥方案要改成 PBKDF2 用户密码方案——这会把首次启动多一步"设置密码"流程。先按「设备绑定」推进，等 v1.5 再说。

---

## 1. 加密密钥管理

### 1.1 目标

- 数据库文件在任何情况下落盘都是密文。
- 密钥本身**不**以明文形式存在于文件系统 / SharedPreferences / 数据库。
- 设备被 root / 物理提取时，密钥应被 Keystore 硬件隔离保护（StrongBox 可选，Pixel 3+ / 三星 S8+ 支持）。

### 1.2 两套密钥（不同用途）

| 用途 | 来源 | 存储 | 失效场景 |
|---|---|---|---|
| **本地 DB 密钥** | 首次启动 `SecureRandom` 生成 32 字节 | 用 Keystore **AES/GCM 包装** 后存 `EncryptedSharedPreferences` | 卸载 APP / 清除数据 → 不可恢复（正常） |
| **备份导出密钥** | 用户密码 → PBKDF2 | 不存；每次导出时让用户输入 | 密码遗忘 → 备份不可恢复（正常） |

### 1.3 关键类设计

```
data/security/
├── KeystoreManager.kt          // Keystore key 生成 / 取别名引用（不接触明文密钥）
├── DbKeyProvider.kt            // 启动时获取或创建本地 DB 密钥，返回 ByteArray
├── PassphraseDerivation.kt     // PBKDF2 + 盐生成（仅备份场景用）
└── EncryptedPrefsFactory.kt    // EncryptedSharedPreferences 单例封装
```

**KeystoreManager** —— 只暴露 KeyGenParameterSpec 和 alias，不持有密钥：

```kotlin
// 文件：data/security/KeystoreManager.kt
object KeystoreManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val DB_KEY_WRAPPER_ALIAS = "dosecare_db_key_wrapper"

    fun ensureKeyWrapper() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(DB_KEY_WRAPPER_ALIAS)) return

        val spec = KeyGenParameterSpec.Builder(
            DB_KEY_WRAPPER_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // 关键：拒绝被导出
            .setRandomizedEncryptionRequired(true)
            .build()

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .init(spec)
            .generateKey()
    }
}
```

**DbKeyProvider** —— 启动时调用，保证本地 DB 密钥存在：

```kotlin
// 文件：data/security/DbKeyProvider.kt
class DbKeyProvider @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            ctx,
            "dosecare_secure_prefs",
            MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getOrCreatePassphrase(): ByteArray {
        KeystoreManager.ensureKeyWrapper()
        prefs.getString(KEY_DB_PASSPHRASE_B64, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_DB_PASSPHRASE_B64, Base64.encodeToString(raw, Base64.NO_WRAP)).apply()
        return raw
    }

    private companion object { const val KEY_DB_PASSPHRASE_B64 = "db_passphrase_b64" }
}
```

**PassphraseDerivation**（备份专用，本任务**不实现**，仅占位）：

```kotlin
// 文件：data/security/PassphraseDerivation.kt
object PassphraseDerivation {
    const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    const val ITERATIONS = 100_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16

    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(PBKDF2_ALGO).generateSecret(spec).encoded
    }
}
```

### 1.4 SQLCipher 接线

```kotlin
// 文件：data/local/DoseCareDatabase.kt
@Database(
    entities = [
        UserDrugEntity::class,
        IntakeRecordEntity::class,
        LabResultEntity::class,
        PatientProfileEntity::class,
        InteractionLogEntity::class,
        OutboxEventEntity::class,
    ],
    version = 1,
    exportSchema = true,  // 必须开 → 把 schema 提交进 git
)
@TypeConverters(RoomConverters::class)
abstract class DoseCareDatabase : RoomDatabase() {
    abstract fun userDrugDao(): UserDrugDao
    abstract fun intakeRecordDao(): IntakeRecordDao
    abstract fun labResultDao(): LabResultDao
    abstract fun patientProfileDao(): PatientProfileDao
    abstract fun interactionLogDao(): InteractionLogDao
    abstract fun outboxDao(): OutboxDao
}
```

```kotlin
// 文件：data/local/DatabaseModule.kt（Hilt module）
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext ctx: Context,
        keyProvider: DbKeyProvider
    ): DoseCareDatabase {
        System.loadLibrary("sqlcipher")  // SQLCipher 4.x 需要显式 load
        val factory = SupportFactory(keyProvider.getOrCreatePassphrase())
        return Room.databaseBuilder(ctx, DoseCareDatabase::class.java, "dosecare.db")
            .openHelperFactory(factory)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // 可选：写一份初始 audit log "DB created"
                }
            })
            .build()
    }

    @Provides fun provideUserDrugDao(db: DoseCareDatabase) = db.userDrugDao()
    @Provides fun provideIntakeRecordDao(db: DoseCareDatabase) = db.intakeRecordDao()
    @Provides fun provideLabResultDao(db: DoseCareDatabase) = db.labResultDao()
    @Provides fun providePatientProfileDao(db: DoseCareDatabase) = db.patientProfileDao()
    @Provides fun provideInteractionLogDao(db: DoseCareDatabase) = db.interactionLogDao()
    @Provides fun provideOutboxDao(db: DoseCareDatabase) = db.outboxDao()
}
```

### 1.5 不做的事

- ❌ 不上 `BiometricPrompt` 锁 APP（v2.0 再说，v0.1 用户量小、误锁风险高）
- ❌ 不上 `MasterKey` 之外的 KeyStore wrapper（避免自定义加密 bug）
- ❌ 不缓存 passphrase 到内存单例（每次从 EncryptedSharedPreferences 读，几十微秒）
- ❌ 不写密钥到 `BuildConfig` / 静态字段 / Log

---

## 2. Database Schema

### 2.1 总览

| 表 | 索引 | 外键 | 预计行数 | 说明 |
|---|---|---|---|---|
| `user_drugs` | `idx_drugCatalogId`, `idx_archived` | 无（软删） | < 50 | 当前在用 + 历史停药 |
| `intake_records` | `idx_userDrugId`, `idx_actualAt` | 无（保留历史） | < 50k/年 | 每次服药 / 漏服记录 |
| `lab_results` | `idx_analyte`, `idx_measuredAt`, `idx_userDrugId` | 无 | < 500/年 | TDM + 常规化验 |
| `patient_profile` | 主键恒为 1 | — | 1 | 单例 |
| `interaction_log` | `idx_detectedAt`, `idx_severity`, `idx_acknowledged` | 无 | < 1k/年 | 警示历史 |
| `outbox_events` | `idx_createdAt`, `idx_synced` | 无 | < 1k/月（v2.0 才有意义） | 同步基础 |

**为什么 IntakeRecord 不建外键到 UserDrug**：精神科患者可能停药，但历史打卡的「是否规律」数据在依从性分析里仍有价值。CASCADE DELETE 会让分析数据丢失。用 `archived = true` 软删 + 索引即可。

### 2.2 Entity 详细定义

```kotlin
// 文件：data/local/entity/UserDrugEntity.kt
@Entity(
    tableName = "user_drugs",
    indices = [
        Index("drugCatalogId"),
        Index("archived"),
    ],
)
data class UserDrugEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drugCatalogId: String,           // 对应 Drug.id（不是 Room 外键，只是引用）
    val nickname: String?,
    val route: String,                   // ORAL / IM / IV / SL
    val doseMg: Double,
    val doseUnit: String,                // mg / mL / IU
    val frequencyPerDay: Double,         // 0.5 = 每两天一次
    val scheduleCron: String?,           // "08:00,14:00,20:00" 或更复杂
    val startDate: Long,
    val endDate: Long?,
    val prn: Boolean = false,
    val prnMaxDosePerDay: Double?,
    val prescriber: String?,
    val notes: String = "",
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

```kotlin
// 文件：data/local/entity/IntakeRecordEntity.kt
@Entity(
    tableName = "intake_records",
    indices = [
        Index("userDrugId"),
        Index("actualAt"),
        Index(value = ["userDrugId", "actualAt"]),  // 复合索引：常用「某药近期记录」查询
    ],
)
data class IntakeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userDrugId: Long,
    val scheduledAt: Long?,
    val actualAt: Long,
    val doseMg: Double,
    val status: String,                  // IntakeStatus.name
    val notes: String?,
)
```

```kotlin
// 文件：data/local/entity/LabResultEntity.kt
@Entity(
    tableName = "lab_results",
    indices = [
        Index("analyte"),
        Index("measuredAt"),
        Index("userDrugId"),
    ],
)
data class LabResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userDrugId: Long?,
    val analyte: String,                 // "CLOZAPINE_SERUM", "HbA1c", "eGFR"
    val value: Double,
    val unit: String,
    val measuredAt: Long,
    val labName: String?,
    val notes: String?,
)
```

```kotlin
// 文件：data/local/entity/PatientProfileEntity.kt
@Entity(tableName = "patient_profile")
data class PatientProfileEntity(
    @PrimaryKey val id: Int = 1,        // 单例
    val displayName: String,
    val birthDate: Long?,
    val biologicalSex: String,          // MALE / FEMALE / X / UNKNOWN
    val weightKg: Double?,
    val heightCm: Double?,
    val smoker: Boolean = false,
    val cigarettesPerDay: Int = 0,
    val caffeineMgPerDay: Int = 0,
    val alcoholUnitsPerWeek: Int = 0,
    val egfr: Double?,
    val childPugh: String?,             // A / B / C
    val conditions: String = "[]",      // JSON: ["SCHIZOPHRENIA", "T2DM"]
    val cyp2D6Genotype: String?,        // PM / IM / EM / UM
    val cyp2C19Genotype: String?,
    val updatedAt: Long = System.currentTimeMillis(),
)
```

```kotlin
// 文件：data/local/entity/InteractionLogEntity.kt
@Entity(
    tableName = "interaction_log",
    indices = [
        Index("detectedAt"),
        Index("severity"),
        Index("acknowledged"),
    ],
)
data class InteractionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val interactionType: String,         // CYP / QTc / ANTICHOLINERGIC / 5HT / WINDOW
    val severity: String,                // INFO / LOW / MEDIUM / HIGH / CONTRAINDICATED
    val title: String,
    val detail: String,
    val involvedDrugIds: String,        // JSON 数组: ["clozapine", "fluvoxamine"]
    val detectedAt: Long,
    val acknowledged: Boolean = false,
    val resolved: Boolean = false,
)
```

```kotlin
// 文件：data/local/entity/OutboxEventEntity.kt
@Entity(
    tableName = "outbox_events",
    indices = [
        Index("createdAt"),
        Index("synced"),
    ],
)
data class OutboxEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,               // "DRUG_ADDED", "INTAKE_RECORDED", "INTERACTION_DETECTED"
    val aggregateId: String?,            // 业务主键，便于远端 dedupe
    val payload: String,                 // JSON
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncError: String?,
)
```

### 2.3 DAO 接口

每个 Entity 配一个 DAO；统一返回 `Flow`（观察） / `suspend`（写）。

```kotlin
// 文件：data/local/dao/UserDrugDao.kt
@Dao
interface UserDrugDao {
    @Query("SELECT * FROM user_drugs WHERE archived = 0 ORDER BY startDate DESC")
    fun observeActive(): Flow<List<UserDrugEntity>>

    @Query("SELECT * FROM user_drugs WHERE id = :id")
    suspend fun getById(id: Long): UserDrugEntity?

    @Query("SELECT * FROM user_drugs WHERE drugCatalogId = :catalogId AND archived = 0")
    suspend fun findActiveByCatalogId(catalogId: String): UserDrugEntity?

    @Insert
    suspend fun insert(entity: UserDrugEntity): Long

    @Update
    suspend fun update(entity: UserDrugEntity)

    @Query("UPDATE user_drugs SET archived = 1, endDate = :endDate, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: Long, endDate: Long, now: Long = System.currentTimeMillis())
}
```

`IntakeRecordDao` / `LabResultDao` / `PatientProfileDao` / `InteractionLogDao` / `OutboxDao` 模式相同，全部写在 `data/local/dao/` 下。

```kotlin
// IntakeRecordDao 关键方法
@Dao
interface IntakeRecordDao {
    @Query("SELECT * FROM intake_records ORDER BY actualAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<IntakeRecordEntity>>

    @Query("SELECT * FROM intake_records WHERE userDrugId = :userDrugId AND actualAt >= :sinceMillis ORDER BY actualAt ASC")
    fun observeForDrugSince(userDrugId: Long, sinceMillis: Long): Flow<List<IntakeRecordEntity>>

    @Insert
    suspend fun insert(entity: IntakeRecordEntity): Long

    @Query("UPDATE intake_records SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
```

```kotlin
// OutboxDao 关键方法（v2.0 同步用，v0.1 仅插入，不读）
@Dao
interface OutboxDao {
    @Insert
    suspend fun insert(event: OutboxEventEntity): Long

    @Query("SELECT * FROM outbox_events WHERE synced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun unsynced(limit: Int = 100): List<OutboxEventEntity>

    @Query("UPDATE outbox_events SET synced = 1, syncAttempts = syncAttempts + 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
```

### 2.4 Type Converters

```kotlin
// 文件：data/local/RoomConverters.kt
class RoomConverters {
    // 列表字段统一存 JSON 字符串（用 kotlinx.serialization）
    @TypeConverter fun fromStringList(list: List<String>): String =
        Json.encodeToString(list)

    @TypeConverter fun toStringList(json: String): List<String> =
        if (json.isBlank()) emptyList() else Json.decodeFromString(json)

    // Instant <-> Long 仅做 ms epoch（避免 java.time 在低版本 SDK 行为差异）
    // IntakeStatus 直接用 name() 字符串存取，省一个 converter
}
```

### 2.5 Schema 导出

`build.gradle.kts` 加：

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

`app/schemas/com.dosecare.app.data.local.DoseCareDatabase/1.json` 必须**提交进 git**——这是 Room 升级 migration 的依据。

### 2.6 Migration 策略

- v0.1 → v1.0：版本 1，无 migration。
- v1.x → v1.y：用 `AutoMigration` 处理列新增；列重命名 / 类型变更用显式 `Migration` 对象并写测试。
- 绝不自动 DROP 表——用户数据**永远不删除**。

---

## 3. Repository 实现策略

### 3.1 分层

```
prototype/  (domain, 纯 Kotlin)
   └─ Repository.kt           ← 接口定义
        ↓ 实现
data/repository/  (data, Android)
   ├─ LocalFirstUserDrugsRepository.kt
   ├─ LocalFirstIntakeRecordsRepository.kt
   ├─ LocalFirstLabResultsRepository.kt
   ├─ LocalFirstPatientProfileRepository.kt
   ├─ LocalFirstInteractionsRepository.kt
   └─ (SyncRepository 暂不实现)
        ↓ 依赖
data/local/  (Room)
   ├─ entity/  ·  dao/  ·  DoseCareDatabase.kt
data/mapper/  (Entity ↔ Domain 双向)
   ├─ UserDrugMapper.kt
   ├─ IntakeRecordMapper.kt
   ├─ LabResultMapper.kt
   └─ PatientProfileMapper.kt
```

### 3.2 Mapper 示例

```kotlin
// 文件：data/mapper/UserDrugMapper.kt
fun UserDrugEntity.toDomain(): UserDrugForRule = UserDrugForRule(
    id = id,
    drugCatalogId = drugCatalogId,
    doseMg = doseMg,
    frequencyPerDay = frequencyPerDay,
)

fun UserDrugForRule.toEntity(now: Long = System.currentTimeMillis()): UserDrugEntity =
    UserDrugEntity(
        id = id,
        drugCatalogId = drugCatalogId,
        doseMg = doseMg,
        frequencyPerDay = frequencyPerDay,
        startDate = now,
        updatedAt = now,
    )
```

### 3.3 Repository 实现（关键模式）

```kotlin
// 文件：data/repository/LocalFirstUserDrugsRepository.kt
class LocalFirstUserDrugsRepository @Inject constructor(
    private val dao: UserDrugDao,
    private val outboxDao: OutboxDao,
    private val clock: Clock = Clock.System,
) : UserDrugsRepository {

    override fun observeActive(): Flow<List<UserDrugForRule>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    @Transaction
    override suspend fun add(drugId: String, doseMg: Double, frequencyPerDay: Double) {
        val now = clock.now().toEpochMilliseconds()
        val newId = dao.insert(UserDrugEntity(
            drugCatalogId = drugId, doseMg = doseMg, frequencyPerDay = frequencyPerDay,
            route = "ORAL", doseUnit = "mg", startDate = now, updatedAt = now,
        ))
        outboxDao.insert(OutboxEventEntity(
            eventType = "DRUG_ADDED", aggregateId = newId.toString(),
            payload = """{"drugCatalogId":"$drugId","doseMg":$doseMg}""",
        ))
    }

    // update / remove 同模式
}
```

**两条铁律**：

1. **写操作必须 `@Transaction`**，Entity + OutboxEvent 同生共死。否则 APP 崩溃在两次写入之间，未来同步会少事件。
2. **DAO Flow 不在 Repository 里 `.flowOn(Dispatchers.IO)`**——Room 已经做线程切换，重复切浪费。

### 3.4 Hilt 绑定

```kotlin
// 文件：data/repository/RepositoryBindings.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds @Singleton
    abstract fun bindUserDrugs(impl: LocalFirstUserDrugsRepository): UserDrugsRepository

    @Binds @Singleton
    abstract fun bindIntakeRecords(impl: LocalFirstIntakeRecordsRepository): IntakeRecordsRepository

    @Binds @Singleton
    abstract fun bindLabResults(impl: LocalFirstLabResultsRepository): LabResultsRepository

    @Binds @Singleton
    abstract fun bindPatientProfile(impl: LocalFirstPatientProfileRepository): PatientProfileRepository

    @Binds @Singleton
    abstract fun bindInteractions(impl: LocalFirstInteractionsRepository): InteractionsRepository
}
```

### 3.5 ViewModel 注入

`HomeViewModel` 当前只注 `DrugCatalogService`。下一步加一个 `UserDrugsListViewModel`：

```kotlin
@HiltViewModel
class UserDrugsListViewModel @Inject constructor(
    private val userDrugs: UserDrugsRepository,
) : ViewModel() {
    val active = userDrugs.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addClozapine100() = viewModelScope.launch {
        userDrugs.add(drugId = "clozapine", doseMg = 100.0, frequencyPerDay = 2.0)
    }
}
```

**原则**：ViewModel 只接 Repository 接口，不接 DAO、不接 Database。

### 3.6 跟现有 prototype/ 的接口对齐

`prototype/Repository.kt` 已经定义好 6 个接口。本任务**不修改**这 6 个接口签名。

但有两处需要打补丁（写入下个 P 任务，不在本任务范围）：
- `UserDrugsRepository.add()` 目前只接 (drugId, doseMg, frequencyPerDay)，缺 `route / doseUnit / startDate / prn` 等字段。**临时方案**：本任务里 Repository 在 add() 里给这些字段写默认（ORAL / mg / now / false），并用 `notes` 字段留 `// TODO: enrich API`。
- `IntakeRecordsRepository.recordIntake()` 没接 `scheduledAt`，v0.1 写 null。P1-02（打卡 UI）会改。

---

## 4. 单元测试方案

### 4.1 测试分层

| 层 | 工具 | 跑在哪 | 覆盖目标 |
|---|---|---|---|
| **Entity / DAO 行为** | Room in-memory + JUnit4 + coroutines-test | JVM 单测 | CRUD、Flow 触发、索引命中 |
| **Repository 业务** | 假 DAO（手写或 MockK）+ JUnit4 | JVM 单测 | transaction 包装、outbox 写入 |
| **Mapper 双向** | JUnit4 | JVM 单测 | 字段一一对应，nullable 行为 |
| **加密** | Robolectric + 真 `SupportFactory` | JVM 单测 | DB 文件落盘为密文（验证 magic bytes） |
| **集成** | androidTest + 真机 / 模拟器 | Instrumentation | SQLCipher 真实性能、Keystore 真机表现 |

### 4.2 DAO 测试样例

```kotlin
// 文件：app/src/test/java/com/dosecare/app/data/local/UserDrugDaoTest.kt
@RunWith(AndroidJUnit4::class)
class UserDrugDaoTest {
    private lateinit var db: DoseCareDatabase
    private lateinit var dao: UserDrugDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DoseCareDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.userDrugDao()
    }

    @After fun teardown() { db.close() }

    @Test fun insert_then_observeActive_emits() = runTest {
        val id = dao.insert(UserDrugEntity(
            drugCatalogId = "clozapine", doseMg = 100.0, frequencyPerDay = 2.0,
            route = "ORAL", doseUnit = "mg", startDate = 1L, updatedAt = 1L,
        ))
        val active = dao.observeActive().first()
        assertEquals(1, active.size)
        assertEquals("clozapine", active[0].drugCatalogId)
    }

    @Test fun archive_excludesFromActive() = runTest {
        val id = dao.insert(/* ... */)
        dao.archive(id, endDate = 100L)
        assertEquals(0, dao.observeActive().first().size)
    }
}
```

### 4.3 Repository 测试样例

用 **假 DAO** 测事务边界和 outbox 写入，避免每次启 Room：

```kotlin
// 文件：app/src/test/java/com/dosecare/app/data/repository/LocalFirstUserDrugsRepositoryTest.kt
class LocalFirstUserDrugsRepositoryTest {
    private val fakeUserDrugDao = FakeUserDrugDao()
    private val fakeOutboxDao = FakeOutboxDao()
    private val repo = LocalFirstUserDrugsRepository(
        dao = fakeUserDrugDao,
        outboxDao = fakeOutboxDao,
    )

    @Test fun `add writes entity and outbox in one call`() = runTest {
        repo.add("clozapine", 100.0, 2.0)
        assertEquals(1, fakeUserDrugDao.all().size)
        assertEquals(1, fakeOutboxDao.all().size)
        assertEquals("DRUG_ADDED", fakeOutboxDao.all()[0].eventType)
    }

    @Test fun `add failure does not write outbox`() = runTest {
        fakeUserDrugDao.failNextInsert = true
        runCatching { repo.add("clozapine", 100.0, 2.0) }
        assertEquals(0, fakeOutboxDao.all().size)
    }
}
```

`FakeUserDrugDao` 手写实现 `UserDrugDao` 接口的所有方法（其他方法抛 `NotImplementedError`，只测的 3 个真实现）。

### 4.4 加密断言（关键安全测试）

```kotlin
// 文件：app/src/test/java/com/dosecare/app/data/local/EncryptedDatabaseTest.kt
@Test fun database_file_is_not_plaintext_sqlite() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val keyProvider = DbKeyProvider(ctx)
    val db = DatabaseModule.provideDatabase(ctx, keyProvider)
    db.close()  // 触发 flush

    val file = ctx.getDatabasePath("dosecare.db")
    val header = file.readBytes().copyOfRange(0, 16)
    // 纯 SQLite 文件头："SQLite format 3\0"
    val sqliteMagic = "SQLite format 3\u0000".toByteArray()
    assertFalse("DB file must be encrypted, not plain SQLite", header.contentEquals(sqliteMagic))
    // SQLCipher 4.x 加密文件头第一字节是 0x7F + random salt
    assertEquals(0x7F, header[0])
}
```

### 4.5 测试覆盖率目标

- Mapper 100%
- DAO CRUD 100%
- Repository 事务边界 100%（happy path + 失败回滚）
- 加密断言 100%（这条**必须**过 CI）
- 集成测试覆盖：1 个真实 7 天连续打卡场景

---

## 5. 与 prototype/ 现有代码的对接

### 5.1 不动的东西

- `prototype/Repository.kt` —— 6 个接口签名不变。
- `prototype/patient/PatientProfile.kt` —— 纯数据类，不动。
- `prototype/catalog/` —— 静态 DrugCatalog，不动。
- `prototype/pk/` / `prototype/rules/` —— 纯函数，不动。

### 5.2 必须改的东西（增量）

- `prototype/Repository.kt` 加一个文件 `RepositoryAdapters.kt`，**只放 mapper 函数**（不引入 Android 依赖，保留 KMP-ready）。本任务在 `data/mapper/` 写，在 1.5 KMP 抽离时把它们挪到 prototype/ 下的 `RepositoryAdapters.kt`。
- `prototype/patient/PatientProfile.kt` 加 `companion object { val EMPTY = PatientProfile() }`，方便「用户首次启动时往 DB 插一条空 profile」。

### 5.3 关键引用路径

```
app/.../ui/HomeViewModel.kt          ← 改：注入 DrugCatalogService + UserDrugsRepository
app/.../di/CatalogModule.kt          ← 不动
app/.../di/DatabaseModule.kt         ← 新增（Hilt 提供 Database + DAOs）
app/.../di/RepositoryBindings.kt     ← 新增（@Binds 接口 → 实现）
data/local/...                       ← 新增（entity / dao / database）
data/repository/...                  ← 新增（6 个 LocalFirst impl）
data/mapper/...                      ← 新增（4 个 mapper）
data/security/...                    ← 新增（Keystore + DbKeyProvider）
```

### 5.4 prototype/ 演化为 KMP 共享模块的检查清单（v1.5 时做，本任务不触发）

- [ ] mapper 文件无 Android 导入
- [ ] Repository 接口无 Android 导入
- [ ] `data/` 下所有 `Context` / `android.util.Log` 替换为接口
- [ ] Room 迁移到 SQLDelight

---

## 6. 待用户裁决的点

> ✅ **已确认：A 方案（设备绑定）** — 2026-09-06 用户拍板

| 问题 | 现状 | 备选 | 决策 | 影响 |
|---|---|---|---|---|
| **A · 本地 DB 是否需要「重装不丢」** | 设备绑定密钥（重装 = 丢） | 用户密码派生密钥（每次启动输密码） | ✅ A 采用 / ❌ B 否定 | 决定是否加「首次启动设置密码」流程 |
| **B · PatientProfile 是否允许多用户** | 单例（id=1） | 1:N，加 userId 字段 | ✅ A 采用 / ❌ B 否定 | 后期若做「家庭多人」需重构 |
| **C · OutboxEvent 是否在 v0.1 就要写** | 写（即使 v0.1 永远不读） | v0.1 不写，v1.5 一次性 backfill | ✅ A 采用 / ❌ B 否定 | 选「写」未来切换无迁移成本 |

**默认按现状推进**。A 已由用户于 2026-09-06 拍板；B/C 按方案默认推进。

---

## 7. 预估代码量 + 文件清单

### 7.1 总量

- **新增文件**：26 个
- **修改文件**：3 个（`HomeViewModel.kt` / `PatientProfile.kt` / `Repository.kt` 加 EMPTY）
- **新增代码行**：~1,500 行（含测试约 ~2,800 行）
- **实施工时**：3-4 个工作日（无阻碍情况下）

### 7.2 文件清单

| 路径 | 用途 | 估行 |
|---|---|---|
| `data/security/KeystoreManager.kt` | Keystore key 生成 | 35 |
| `data/security/DbKeyProvider.kt` | DB 密钥获取/创建 | 45 |
| `data/security/PassphraseDerivation.kt` | PBKDF2（备份用占位） | 25 |
| `data/local/entity/UserDrugEntity.kt` | Room 实体 | 30 |
| `data/local/entity/IntakeRecordEntity.kt` | Room 实体 | 25 |
| `data/local/entity/LabResultEntity.kt` | Room 实体 | 25 |
| `data/local/entity/PatientProfileEntity.kt` | Room 实体 | 30 |
| `data/local/entity/InteractionLogEntity.kt` | Room 实体 | 30 |
| `data/local/entity/OutboxEventEntity.kt` | Room 实体 | 25 |
| `data/local/dao/UserDrugDao.kt` | DAO | 40 |
| `data/local/dao/IntakeRecordDao.kt` | DAO | 35 |
| `data/local/dao/LabResultDao.kt` | DAO | 30 |
| `data/local/dao/PatientProfileDao.kt` | DAO | 35 |
| `data/local/dao/InteractionLogDao.kt` | DAO | 30 |
| `data/local/dao/OutboxDao.kt` | DAO | 25 |
| `data/local/RoomConverters.kt` | TypeConverter | 20 |
| `data/local/DoseCareDatabase.kt` | Database 聚合根 | 30 |
| `data/local/DatabaseModule.kt` | Hilt module | 45 |
| `data/mapper/UserDrugMapper.kt` | Entity ↔ Domain | 30 |
| `data/mapper/IntakeRecordMapper.kt` | Mapper | 25 |
| `data/mapper/LabResultMapper.kt` | Mapper | 20 |
| `data/mapper/PatientProfileMapper.kt` | Mapper | 40 |
| `data/repository/RepositoryBindings.kt` | Hilt @Binds | 25 |
| `data/repository/LocalFirstUserDrugsRepository.kt` | 实现 | 50 |
| `data/repository/LocalFirstIntakeRecordsRepository.kt` | 实现 | 45 |
| `data/repository/LocalFirstLabResultsRepository.kt` | 实现 | 35 |
| `data/repository/LocalFirstPatientProfileRepository.kt` | 实现 | 40 |
| `data/repository/LocalFirstInteractionsRepository.kt` | 实现 | 60 |
| `app/src/test/.../FakeDaos.kt` | 测试 fake | 80 |
| `app/src/test/.../UserDrugDaoTest.kt` | DAO 测试 | 60 |
| `app/src/test/.../IntakeRecordDaoTest.kt` | DAO 测试 | 50 |
| `app/src/test/.../UserDrugsRepositoryTest.kt` | Repo 测试 | 70 |
| `app/src/test/.../EncryptedDatabaseTest.kt` | 加密断言 | 35 |
| **修改** `app/.../ui/HomeViewModel.kt` | 注入 UserDrugsRepository | +15 |
| **修改** `prototype/patient/PatientProfile.kt` | 加 EMPTY | +3 |
| **修改** `app/build.gradle.kts` | 加 KSP `room.schemaLocation` | +3 |

### 7.3 实施顺序

1. **D1** — 6 个 Entity + 6 个 DAO + Database（无加密，先跑通 in-memory 单测）
2. **D1** — 6 个 Mapper
3. **D1** — 6 个 LocalFirst Repository
4. **D2** — `DatabaseModule` + `RepositoryBindings` + `KeystoreManager` + `DbKeyProvider` + `SupportFactory` 接线
5. **D2** — `HomeViewModel` 改造 + 跑一次真机 smoke：装 APP → 加药 → 杀进程重启 → 数据还在
6. **D3** — 单测补齐（DAO + Repository + 加密断言）
7. **D3** — `gradlew assembleDebug` 通过 + lint 无 error
8. **D4** — 文档收尾：把这次接入对架构的影响写进 `01-ARCHITECTURE.md` 的「关键流程」一节

---

## 8. 验收标准（DoD）

- [ ] `./gradlew assembleDebug` 通过，无 error
- [ ] DAO 6 套单测全绿，coverage 报告里 data/local 行覆盖 ≥ 85%
- [ ] Repository 事务测试 100% 通过（含失败回滚）
- [ ] `EncryptedDatabaseTest` 100% 通过
- [ ] 真机 smoke：装 APP → 加氯氮平 100mg bid → 杀进程 → 重启 → 数据在；数据库文件用 `sqlite3` 打开是密文
- [ ] `app/schemas/com.dosecare.app.data.local.DoseCareDatabase/1.json` 提交进 git
- [ ] `01-ARCHITECTURE.md` §1 的三层图保持不变；§5 数据存储策略段落更新为「本地 DB 密钥设备绑定」一句话
- [ ] prototype/ 目录**未被修改**（除 PatientProfile 加 EMPTY 这一行）

---

## 9. 风险与回退

| 风险 | 概率 | 影响 | 回退 |
|---|---|---|---|
| SQLCipher 4.6 在某些国产 ROM 上崩溃 | 低 | APP 启动失败 | 退到 SQLCipher 4.5.4（兼容矩阵已知） |
| Keystore StrongBox 在低端机缺失 | 中 | 密钥保护降级 | 自动回退到 TEE，软件保护仍优于明文 |
| `@Transaction` 在大表时性能差 | 极低 | 写操作慢 | v1.0 引入 `withTransaction` 包裹批量写 |
| Migration 把用户数据弄丢 | 低 | 不可逆 | 每次迁移前自动 export 加密备份到 `cacheDir` |
| Outbox 永远不同步膨胀 | 中 | 占用空间 | v2.0 上 `markSynced` + 定期 prune（>30 天已同步） |
