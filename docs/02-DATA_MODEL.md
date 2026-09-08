# 02 · 数据模型

## 两类数据

### A. 静态数据：DrugCatalog（药物目录）
打包在 APK 内（`assets/drugs/v1.json`），不加密。涵盖**~250-300 个药**的精神科 + 内分泌 + 必要共病药（心血管、糖尿病、甲状腺、抗帕金森、抗胆碱能）。

### B. 动态数据：用户数据
- `UserDrug`（用户当前在用的药）
- `IntakeRecord`（每次服药记录）
- `LabResult`（化验/血药浓度结果）
- `PatientProfile`（患者基础信息）
- `InteractionLog`（冲突检测历史）
- `OutboxEvent`（未来同步基础）
- `SymptomJournal`（症状打卡，v0.5+）

## A. DrugCatalog JSON 模式

```json
{
  "schemaVersion": "1.0.0",
  "generatedAt": "2026-09-01T00:00:00Z",
  "source": "DrugBank/Flockhart/AGNP/说明书",
  "drugs": [
    {
      "id": "clozapine",
      "genericName": "Clozapine",
      "genericNameZh": "氯氮平",
      "brandNames": ["Clozaril", "Leponex"],
      "category": "ANTIPSYCHOTIC",
      "subcategory": "ATYPICAL_DIBENZODIAZEPINE",
      "atc": "N05AH02",
      
      "forms": [
        {
          "route": "ORAL",
          "f": 0.6,
          "kaPerHour": 1.5,
          "tmaxHours": 2.0,
          "doseUnits": ["mg"],
          "commonDoseRange": [12.5, 900]
        }
      ],
      
      "pkModel": "ONE_COMPARTMENT_ORAL",
      "kePerHour": 0.0495,
      "tHalfHours": 14,
      "tHalfRangeHours": [8, 20],
      "vdLPerKg": 5.4,
      "clLPerHour": 50,
      "proteinBindingPct": 95,
      
      "therapeuticWindow": {
        "low": 350,
        "high": 600,
        "unit": "ng/mL",
        "lab": "TDM_SERUM_TROUGH",
        "guidelineSource": "AGNP 2017"
      },
      
      "cypProfile": {
        "substrates": [
          {"cyp": "CYP1A2", "fraction": 0.7},
          {"cyp": "CYP3A4", "fraction": 0.2},
          {"cyp": "CYP2C19", "fraction": 0.1}
        ],
        "inhibitors": [],
        "inducers": []
      },
      
      "activeMetabolites": [
        {
          "id": "norclozapine",
          "name": "Norclozapine",
          "activityRatio": 0.5,
          "note": "活性代谢物，毒性低于原药"
        }
      ],
      
      "adverseEffects": {
        "qtcProlongation": "MEDIUM",
        "metabolicSyndrome": "HIGH",
        "anticholinergicLoad": 3,
        "agranulocytosis": "HIGH",
        "extrapyramidal": "VERY_LOW",
        "sedation": "HIGH",
        "sexual": "MEDIUM",
        "hyperprolactinemia": "LOW"
      },
      
      "adjustments": {
        "renal": "NONE",
        "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
        "elderly": "START_LOW_TITRATE_SLOW",
        "smoking": {
          "effect": "CYP1A2 induction",
          "doseAdjustment": "INCREASE_DOSE_50_PCT_IN_SMOKERS",
          "abstinence": "DECREASE_DOSE_ON_SMOKING_CESSATION_30_PCT"
        }
      },
      
      "criticalInteractions": [
        {
          "trigger": "fluvoxamine",
          "mechanism": "CYP1A2_STRONG_INHIBITION",
          "aucFoldChange": [5, 10],
          "severity": "CONTRAINDICATED",
          "clinicalNote": "氟伏沙明可使氯氮平浓度升高 5-10 倍"
        }
      ],
      
      "monitoring": {
        "frequency": "WEEKLY_FIRST_18WEEKS_THEN_MONTHLY",
        "items": ["WBC", "ANC", "血糖", "血脂", "体重", "BMI"]
      }
    }
  ]
}
```

### 关键字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | ✓ | 英文小写 + 下划线，作为唯一键 |
| `genericName` | ✓ | INN 通用名英文 |
| `genericNameZh` | ✓ | 中文通用名 |
| `category` | ✓ | 见下表枚举 |
| `forms` | ✓ | 至少 1 个（ORAL 是默认） |
| `pkModel` | ✓ | 决定用哪个 PK 公式 |
| `cypProfile.substrates` | ✓ | 总和应 ≈ 1.0（部分代谢走非 CYP 路径可不足 1） |
| `therapeuticWindow` | ⨯ | 治疗窗窄的药必填，NO_WINDOW 也明确标 |
| `criticalInteractions` | ⨯ | 只有严重或禁忌的才列，常规的在规则引擎里 |

### Category 枚举

```
ANTIPSYCHOTIC (FIRST_GEN, SECOND_GEN_ATYPICAL)
MOOD_STABILIZER  # 锂盐、丙戊酸、卡马西平、拉莫三嗪
ANTIDEPRESSANT_SSRI
ANTIDEPRESSANT_SNRI
ANTIDEPRESSANT_TCA
ANTIDEPRESSANT_MAOI
ANTIDEPRESSANT_OTHER  # 米氮平、安非他酮
ANXIOLYTIC_BENZODIAZEPINE
ANXIOLYTIC_NON_BENZODIAZEPINE
STIMULANT_ADHD  # 哌甲酯、阿托莫西汀
ANTICHOLINERGIC  # 苯海索
ANTIPARKINSONIAN

# 内分泌
ANTIDIABETIC_BIGUANIDE  # 二甲双胍
ANTIDIABETIC_SULFONYLUREA
ANTIDIABETIC_DPP4
ANTIDIABETIC_SGLT2
ANTIDIABETIC_GLP1
ANTIDIABETIC_INSULIN
THYROID_HORMONE
ANTITHYROID

# 心血管（精神科共病必需）
ANTIHYPERTENSIVE
STATIN
ANTIPLATELET
ANTICOAGULANT

# 其他
ANTIEPILEPTIC
ANALGESIC_OPIOID
ANALGESIC_NSAID
ANTIHISTAMINE
HERBAL  # 圣约翰草等
```

## B. Room Schema

```kotlin
@Entity(
    tableName = "user_drugs",
    indices = [Index("drugCatalogId")]
)
data class UserDrug(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drugCatalogId: String,        // FK to DrugCatalog.id
    val nickname: String?,            // 用户自定义昵称
    val route: String,                // ORAL/IM/IV/SL...
    val doseMg: Double,               // 单次剂量
    val doseUnit: String,             // mg / mL / IU
    val frequencyPerDay: Double,      // 每日次数 (0.5 = 每两天一次)
    val scheduleCron: String?,        // "08:00,14:00,20:00" 或更复杂
    val startDate: Long,              // 起始时间 epoch ms
    val endDate: Long? = null,        // 结束时间（停药后填）
    val prn: Boolean = false,         // 必要时服用 (pro re nata)
    val prnMaxDosePerDay: Double? = null,
    val prescriber: String? = null,   // 主治医师
    val notes: String = "",
    val archived: Boolean = false
)

@Entity(
    tableName = "intake_records",
    indices = [Index("userDrugId"), Index("actualAt")]
)
data class IntakeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userDrugId: Long,
    val scheduledAt: Long? = null,    // 计划时间（提醒触发点）
    val actualAt: Long,               // 实际服药/漏服时间
    val doseMg: Double,               // 实际服用剂量
    val status: IntakeStatus,         // TAKEN / MISSED / SKIPPED / PARTIAL
    val notes: String? = null
)

enum class IntakeStatus { TAKEN, MISSED, SKIPPED, PARTIAL }

@Entity(
    tableName = "lab_results",
    indices = [Index("analyte"), Index("measuredAt")]
)
data class LabResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userDrugId: Long? = null,     // 关联到具体药
    val analyte: String,              // "CLOZAPINE_SERUM", "LITHIUM_SERUM", "HbA1c", "eGFR"
    val value: Double,
    val unit: String,                 // "ng/mL", "mmol/L", "%"
    val measuredAt: Long,
    val labName: String? = null,
    val notes: String? = null
)

@Entity(tableName = "patient_profile")
data class PatientProfile(
    @PrimaryKey val id: Int = 1,      // 单例
    val displayName: String,
    val birthDate: Long? = null,      // 用于计算年龄
    val biologicalSex: String,        // M / F / X
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val smoker: Boolean = false,
    val cigarettesPerDay: Int = 0,
    val caffeineMgPerDay: Int = 0,
    val alcoholUnitsPerWeek: Int = 0,
    val egfr: Double? = null,         // mL/min/1.73m²
    val childPugh: String? = null,    // A / B / C
    val conditions: String = "[]",    // JSON 数组: ["SCHIZOPHRENIA", "T2DM", "HYPERTENSION"]
    val cyp2D6Genotype: String? = null,   // PM / IM / EM / UM
    val cyp2C19Genotype: String? = null,
    val updatedAt: Long
)

@Entity(
    tableName = "interaction_log",
    indices = [Index("userId"), Index("detectedAt")]
)
data class InteractionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int = 1,              // 单用户时固定 1
    val interactionType: String,      // CYP / QTc / ANTICHOLINERGIC / 5HT / WINDOW
    val severity: String,             // INFO / LOW / MEDIUM / HIGH / CONTRAINDICATED
    val title: String,
    val detail: String,
    val involvedDrugIds: String,      // JSON 数组
    val detectedAt: Long,
    val acknowledged: Boolean = false,
    val resolved: Boolean = false
)

@Entity(
    tableName = "outbox_events",
    indices = [Index("createdAt")]
)
data class OutboxEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,            // "DRUG_ADDED", "INTAKE_RECORDED", "INTERACTION_DETECTED"
    val payload: String,              // JSON
    val createdAt: Long,
    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null
)
```

## 迁移与升级

- DrugCatalog 用 `schemaVersion` 字段，应用内检测版本不一致时提示用户"更新药物目录"
- Room schema 用 `AutoMigration` 处理列新增
- 用户数据**永远不删除**：停药/换药只是 `archived = true` 或 `endDate` 填上
- 备份格式带 `version` 字段，restore 时按版本适配

## 加密

- 整个 Room database 用 **SQLCipher** 加密（密钥由 `EncryptedSharedPreferences` 管理）
- 密钥本身又用 **Android Keystore** 的 hardware-backed key 包装
- 备份导出用 **AES-256-GCM** + 用户密码派生的密钥（PBKDF2 / Argon2id）
- 不做端到端加密同步时，所有数据只在本地
