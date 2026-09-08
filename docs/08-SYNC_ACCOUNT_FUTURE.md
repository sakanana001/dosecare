# 08 · 联网 / 账号 / 医生端（未来扩展设计）

## 设计目标

> **联网不是默认开启的；账号不是必须注册的；同步是用户主动选择的能力。**

整个 v1.0 及之前完全无网。v1.5+ 才逐步引入联网能力，且全部为 opt-in。

## 联网分阶段

### Phase 1：DrugCatalog 增量更新（v1.0+）
- 用户在"关于 → 更新"中点击"检查药物目录更新"
- 应用下载 `v1.1.json` 增量包
- 提示用户"已下载 12 个新药、修正 3 个警示"
- 用户确认后导入
- 仍可完全离线使用

### Phase 2：跨设备同步（v2.0）
- 用户**主动**创建账号
- 上传加密 blob 到云端
- 其他设备登录后拉取

### Phase 3：医生端（v2.0+）
- 医生端是 Web 应用
- 患者用 QR 码"授权医生查看 30 天数据"
- 医生看到 PK 曲线 + 警示 + 依从性 + 化验

## 账号设计

### 三种身份

```
[本地用户]   ── 无账号，所有数据仅本机
[同步用户]   ── 注册账号，数据本地+云端加密
[医生]       ── 专业身份，能查看被授权的患者
```

### 账号数据

```kotlin
data class UserAccount(
    val userId: String,                 // UUID
    val email: String,                  // 登录用
    val displayName: String,
    val role: UserRole,                 // PATIENT / DOCTOR / CAREGIVER
    val publicKey: ByteArray,           // 用于端到端加密
    val createdAt: Long,
    val lastSyncAt: Long?
)

enum class UserRole { PATIENT, DOCTOR, CAREGIVER }
```

### 密码 → 密钥 派生

```
userPassword
  ↓
Argon2id (m=64MB, t=3, p=1)  // 强抗暴力
  ↓
masterKey (32 bytes)
  ↓
HKDF-SHA256 + 上下文
  ↓
dataEncryptionKey (32 bytes)  // 用于加密同步 blob
```

**关键**：
- 服务器**永远不接触** masterKey
- 密码丢失 = 同步数据丢失（这是 E2EE 的固有代价）
- 本地数据不受影响（用户仍可用本地模式）

## 同步架构

### 零知识同步

```
┌─────────────────┐                    ┌─────────────────┐
│  Client          │                    │  Server          │
│                  │                    │                  │
│  masterKey ───E2E───→ 加密 blob       │                  │
│  (本地派生)        │   ↓              │  存:             │
│                  │   AES-GCM          │  - userId        │
│  解密 ←───E2E────  │                   │  - encryptedBlob│
│                  │                    │  - updatedAt     │
│                  │                    │  - version       │
└─────────────────┘                    └─────────────────┘
```

服务器只看到：
- `userId`（明文，用于路由）
- 加密后的 blob（密文）
- 元数据（updatedAt, size）

**服务器完全无法读取数据。**

### Outbox 模式

```kotlin
@Entity(tableName = "outbox_events")
data class OutboxEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val payload: String,  // JSON
    val createdAt: Long,
    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null
)
```

每次本地修改 → 写一条 outbox 事件。SyncWorker 定期扫描 outbox → 加密 → 上传。

### 冲突合并

- 用 Lamport 时间戳 + 操作幂等
- 同一字段多端修改时**后写者胜**（不复杂化）
- 删除操作 = 软删（archived = true），不真删

## 医生端

### 架构

- Web 应用（React/Next.js）
- 后端只存加密 blob（与患者端共用 API）
- 医生用浏览器 PWA 打开，无需安装

### 授权流程

```
患者                           医生
 │                              │
 │  [生成授权 token]              │
 │  - 选定数据范围（30 天 PK）    │
 │  - 选定时间窗                 │
 │  - 生成一次性 token           │
 │  - 显示 QR 码                 │
 │                              │
 │ ────── 扫码 ──────────────────→│
 │                              │
 │                  [输入 token]  │
 │                  [拉取数据]    │
 │                              │
 │                  [查看 PK 曲线]│
 │                  [查看警示]   │
 │                  [写评注]     │
 │                              │
 │ ←──── 推送评注 (E2E 加密) ── │
 │                              │
 │ [看到医生评注]                │
```

### 医生看到什么

- 患者当前用药列表
- 过去 30 天 PK 曲线
- 严重警示历史
- 依从性统计
- 最近化验
- **不**看到：患者姓名（用匿名 ID）、其他设备信息、本地文件

### 评注

医生评注是明文（医师-患者沟通内容，常规医疗记录），可选择：
- 仅显示给患者
- 同步到医院 HIS（需用户授权 + 医院接入）
- 不上传（仅本地）

## 隐私与合规

### 同意书（首次开账号时强制阅读）

> **DoseCareDoseCare端到端加密同步服务**
> 1. 您的数据采用端到端加密（AES-256-GCM），服务器无法读取明文。
> 2. 您的密码派生出加密密钥，**密码丢失 = 数据丢失**。我们无法重置。
> 3. 您可随时停止同步、删除云端数据。删除后无法恢复。
> 4. 我们**不**收集您的 IP 地址、地理位置、设备指纹。
> 5. 您可随时导出本地数据为加密文件，自行保管。
> 6. 任何医师-患者授权分享可随时撤销。

### 数据保留

- 账号删除 → 7 天后云端 blob 真正删除（反悔期）
- 30 天未登录 → 提示；90 天未登录 → 自动删除
- 设备登出 → 该设备的 local cache 立即清除

### GDPR / 中国《个人信息保护法》

- 用户可导出全部数据（JSON 格式）
- 用户可请求删除（一键）
- 用户可撤回同意（不删账号则数据保留但不同步）
- 隐私政策明确列出所有处理目的

## 商业模式

### 不收费的项目
- APP 本身（GPL-3.0）
- 本地所有功能
- 自建 DrugCatalog（公开数据）

### 可能的收费
- 医生端（B2B 收费，给医院 / 诊所）
- 临床药师审核的"医院版"DrugCatalog
- 公益基金支持的开源版本
- 医院定制化接入（私有部署）

## 不做的事

- ❌ 不做"积分" / "排行榜" / "社交" 
- ❌ 不收集任何遥测（崩溃日志都本地留）
- ❌ 不接广告
- ❌ 不向保险公司等第三方共享数据
- ❌ 不做"AI 诊断"——AI 只用于辅助警示解读

## 与医院合作的可能模式

### 模式 A：临床药师审核
- 医院提供药师 review DrugCatalog 的 critical interactions
- 药师姓名/医院出现在"关于"页
- 收费：按 review 数量 / 年

### 模式 B：TDM 数据接入
- 医院 LIS（检验系统）支持导出用户血药浓度
- 患者扫码授权后，APP 自动导入
- 收费：API 接入费

### 模式 C：医生端医院版
- 医院部署医生端的私有版本
- 集成医院 HIS
- 收费：SaaS 订阅

### 模式 D：科研队列
- 征得同意的匿名数据用于 RWRS
- 学术合作，无直接商业化
- 申请 IRB 批准

## 总结

联网 / 账号 / 医生端是**未来的事**。v1.0 之前**完全不联网**。
等到那一天，会以"零知识 + 端到端加密 + 用户主动"的原则推进。
最坏情况：永远不联网。**APP 在完全本地模式下是有完整价值的。**
