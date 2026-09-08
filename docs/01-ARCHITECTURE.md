# 01 · 架构

## 三层 + 多端预留

```
┌────────────────────────────────────────────────────────────┐
│                      UI LAYER (Android)                    │
│   Jetpack Compose + Material 3 + Navigation Compose         │
│   - 单 Activity 多 Composable                                │
│   - ViewModel + StateFlow                                    │
│   - Hilt 注入 UseCase 而非 Repository                        │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│              DOMAIN LAYER (Pure Kotlin / KMP-ready)         │
│   - 业务规则（纯函数，无 Android 依赖）                       │
│   - PK 引擎（多模型 + 数值积分）                              │
│   - 规则引擎（CYP 相互作用 / QTc / 抗胆碱能）                │
│   - 剂量调整策略                                              │
│   - 严重度分级                                                │
│   - UseCase 类（每个用户场景一个）                            │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│                DATA LAYER (Repository)                     │
│   - Repository 接口（KMP 兼容）                              │
│   - LocalFirstRepository（当前默认）                         │
│   - OnlineSecondRepository（未来切换）                       │
│   - DrugCatalog（offline 内置 JSON）                        │
│   - Room + SQLCipher（用户数据）                            │
│   - OutboxEventLog（未来同步基础）                          │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│                INFRASTRUCTURE (Android-only)               │
│   - AlarmManager + WorkManager（提醒）                     │
│   - NotificationCompat（通知）                             │
│   - EncryptedSharedPreferences（设置）                     │
│   - Vico / MPAndroidChart（图表）                          │
│   - BackupAgent（加密备份 v2 格式）                        │
└────────────────────────────────────────────────────────────┘
```

## 模块边界

| 层 | 可依赖 | 不可依赖 |
|---|--------|---------|
| UI | Domain, 自己, Hilt, Android | Data interface 实现（必须经 Domain 中转） |
| Domain | 自己, Kotlin stdlib | Android, Room, Hilt, 任何 Android API |
| Data | Domain, Kotlin stdlib, Room/SQLCipher | UI, ViewModel |
| Infrastructure | 自己, Android | Domain, UI |

## 关键决策与理由

### 1. 领域层完全脱离 Android
- 未来 KMP 抽离时，**整个 domain 层可直接搬到 iOS 端**
- PK 引擎、规则引擎、剂量策略都是**纯函数 + 纯数据**，零平台依赖
- UseCase 接受 Repository 接口注入（接口放 domain，实现在 data 层）

### 2. Repository 接口在 domain，实现在 data
- **CurrentDrugsRepository**（接口）：`observeUserDrugs(): Flow<List<UserDrug>>`
- **LocalCurrentDrugsRepository**（实现）：从 Room 读
- **未来 OnlineCurrentDrugsRepository**：从云端读
- **HybridCurrentDrugsRepository**：本地优先 + 后台同步

### 3. DrugCatalog 静态打进 APK
- 启动时一次性加载到内存
- 以 JSON 形式存放在 `assets/drugs/v1.json`
- v1.0 → v1.1 数据升级用 Room migration 配合应用升级

### 4. 提醒系统分级
- **强提醒**（AlarmManager 精确闹钟）：抗精神病药、心境稳定剂
- **弱提醒**（WorkManager 周期任务）：维生素、辅助药
- 双重保险：WorkManager 每天对账一次，确保重启后不丢失

### 5. 数据存储策略
- 用户数据（UserDrug, IntakeRecord, LabResult, PatientProfile）：**SQLCipher 加密 Room**
- 药物目录（DrugCatalog）：**明文 JSON**（敏感的是用户自己的数据，不是药典）
- 设置（提醒时间、UI 偏好）：**EncryptedSharedPreferences**
- 备份：**AES-256-GCM 加密** ZIP，用户自设密码

## 未来扩展预留接口

### KMP 准备
- 所有 `*.kt` 文件不使用 `android.*` / `java.util.concurrent.*` 之外的任何 Android API
- 不用 `Flow` 直接暴露给 UI（用 `Kotlinx Coroutines` 通用 Flow）
- 数据类避免 `@Parcelize`（Android-only）

### 联网准备
- `Repository` 接口**只声明** `observe / get / save`，不绑定实现
- 所有修改操作在 `OutboxEventLog` 表留一条事件（operation + payload + timestamp）
- 未来加 `SyncWorker` 时，遍历 outbox 上传 + 下载远端变更 + merge
- 用户隐私策略：**默认完全不上传任何数据**；用户主动开"分享给医生"才生成加密 token

### 账号准备
- `UserSession` 数据类（uid、displayName、email、role）
- 启动时查询本地 `UserSessionStore`（EncryptedSharedPreferences）
- 存在 → 进入主界面
- 不存在 → 进入"无账号模式"（所有功能可用，仅无同步）

## 关键流程图

### 服药打卡流程

```
用户点击"刚吃了药"按钮
  ↓
[UI] IntakeRecordSheet 显示
  ↓
[UI] 用户确认/调整剂量
  ↓
[ViewModel] IntakeRecordUseCase.recordIntake(drugId, dose, time)
  ↓
[Domain] 验证剂量合理性（与 UserDrug.doseMg 对比）
  ↓
[Domain] 调用 PkEngine 估算此次贡献的血药浓度
  ↓
[Data] 写入 Room.intake_records
  ↓
[Data] 写一条 outbox 事件（如果开启同步）
  ↓
[ViewModel] 重新计算"现在 → 下次服药"曲线
  ↓
[UI] 显示新曲线 + 治疗窗带
  ↓
[Infrastructure] 重新调度下次提醒
```

### 冲突检测流程

```
用户新增一种药 (或在后台定期任务触发)
  ↓
[Domain] ConflictDetectionUseCase.evaluate()
  ↓
[Domain] 加载所有 UserDrug + PatientProfile
  ↓
[Domain] 对每对药物检查：
   - CYP 配对（最多 O(n²) × CYP 矩阵）
   - QTc 加性
   - 抗胆碱能负荷
   - 5-HT 综合征风险
   - 治疗窗偏离（PK 曲线 vs 窗口）
   - 肾/肝调整
  ↓
[Domain] 输出 List<Interaction> 按严重度排序
  ↓
[UI] 严重者弹警示（红/橙/黄/灰），轻者折叠到列表
  ↓
[Data] 写一条 InteractionLog
```

## 性能预算

| 操作 | 目标 |
|------|------|
| 启动到主屏 | < 800ms（含 DrugCatalog 加载到内存） |
| 一次 PK 曲线绘制 | < 100ms |
| 一次冲突检测 | < 50ms |
| 一次数据库查询 | < 20ms |
| APK 体积 | < 30MB |

## 测试策略

| 层 | 工具 |
|---|------|
| Domain | JUnit5 + Kotest property testing |
| Data | Room in-memory + AndroidX Test |
| UI | Compose UI Test + Espresso |
| PK 引擎 | Property testing（解析解 vs 数值解误差 < 1e-4） |
| 规则引擎 | Snapshot test（每条规则的预期输出） |
| 端到端 | 录制 20 个真实病例 → 跑全流程 → 跟医师审核结果对比 |
