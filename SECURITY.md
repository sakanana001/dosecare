# Security Policy

DoseCare 把"用户数据不出设备"作为核心承诺。本文档描述威胁模型、加密机制、和漏洞报告流程。

## 我们的安全模型

### 核心承诺

1. **零网络**：默认情况下，应用**完全不发任何网络请求**。代码层面无 `HttpURLConnection` / `OkHttp` / `Retrofit` 依赖。
2. **零账号零遥测**：不要求注册，不上报任何使用数据。
3. **零云端**：v1.0 之前没有任何云端存储。v2.0 引入端到端加密同步时，密钥仍由用户密码派生（服务端看不到明文）。
4. **数据全控**：用户在 Settings 屏可一键 export / import 全部数据（加密 ZIP）。

### 我们不收集什么

- ❌ 用户身份信息（姓名 / 邮箱 / 手机）
- ❌ 设备信息（型号 / IMEI / Android ID）
- ❌ 使用统计（页面访问 / 按钮点击 / crash 日志）
- ❌ 任何数据上传统计

---

## 加密机制

### 数据库加密（v0.7+）

```
Android Keystore (硬件级密钥)
       │
       ▼
   AES256-GCM 包装
       │
       ▼
EncryptedSharedPreferences
       │
       ▼
   32-byte passphrase (SecureRandom 生成)
       │
       ▼
SQLCipher (256-bit AES, page-level)
       │
       ▼
   Room / SQLite
```

- **密钥派生**：首次启动时 `java.security.SecureRandom` 生成 32 字节随机 passphrase
- **密钥存储**：passphrase 存到 `EncryptedSharedPreferences` (AndroidX Security 1.1.0-alpha06)，key 由 Android Keystore 派生 (AES256-GCM)
- **数据库**：Room 2.7.0 over SQLCipher 4.6.1 (新 artifact `net.zetetic:sqlcipher-android`)
- **应用范围**：所有用户表 (prescription / diary / dose_taken / tdm_history / lab_result / user_preference) + 11 张静态表 (drug catalog)
- **不加密**：不存任何敏感数据到 SharedPreferences (除了加密 passphrase)

### 备份禁用

- `AndroidManifest.xml` 显式 `android:allowBackup="false"`
- `xml/backup_rules.xml` + `xml/data_extraction_rules.xml` 双保险
- 防止 ADB backup / 云端 Auto Backup 泄露加密数据库副本

### 文件系统

- 全部数据存 `/data/data/com.dosecare.app[.debug]/databases/dosecare.db`
- 默认 Android sandbox 隔离（其他应用无法访问）
- Debug 构建仍受 sandbox 保护（仅 ADB 可访问）

---

## 威胁模型

### 我们的防护

| 威胁 | 防护 |
|------|------|
| **设备丢失 / 被盗** | 数据库全加密 (SQLCipher AES-256)；屏幕锁 + 设备级加密 (FBE) 强依赖 |
| **恶意应用读取数据** | Android sandbox 隔离；非 root 设备无法绕过 |
| **ADB 取数据** | 必须解锁 bootloader + enable root；SQLCipher 仍加密 |
| **物理攻击 (cold boot)** | FBE (File-Based Encryption) 在大多数现代设备上启用 |
| **网络嗅探** | 应用不发任何网络请求；无攻击面 |
| **备份泄露** | `allowBackup=false` + custom rules 双保险 |
| **厂商预装恶意代码** | 超出我们控制范围；建议用 Play Store 或 F-Droid 官方源安装 |

### 我们**不**防护的

- **Root 设备**：root 应用可绕过 Android sandbox 读到加密 DB 文件（仍需 SQLCipher passphrase 破解）
- **用户被胁迫解锁设备**：无技术防护（社会工程问题）
- **设备未启用屏幕锁**：SQLCipher 仍加密但设备锁屏被绕过
- **AGPL 私有部署方故意作恶**：AGPL §13 要求衍生代码开源，但无法阻止部署方添加恶意后门（用户应审计 fork 源码）

### v2.0 之后的变化

v2.0 引入端到端加密云同步时：

- 同步密钥由**用户密码**派生（PBKDF2 / Argon2），服务端**永远**拿不到
- 服务端只存加密 blob（**零知识**架构）
- 密码丢失 → 服务端数据无法恢复（**这是 E2EE 的代价**）

---

## 漏洞报告

### 私密报告

如果你发现**安全漏洞**（密钥派生问题、SQLCipher 配置错误、数据泄露路径等），**请不要在 GitHub Issues 公开**。

请用以下方式之一私密报告：

- **Email**: 详见应用 `About` 屏（在 app 里点 "联系"）
- **GitHub Security Advisories**: 仓库 → Security tab → "New draft security advisory"

我们承诺：

- **48 小时内**首次响应
- **7 天内**确认 + 给出修复时间表
- 修复后**公开致谢**（除非你要求匿名）

### 我们会修复的

- ✅ 任何能让未授权方读取用户数据的问题
- ✅ 任何能让用户数据离开设备的问题
- ✅ 任何让密码 / 密钥在内存中暴露时间过长的问题
- ✅ 任何依赖项的已知 CVE

### 不会立即修复的

- ⏳ 理论攻击（如需要 root + 物理访问 + 冷启动）
- ⏳ 已经废弃的功能
- ⏳ 第三方库的低危 CVE（评估后再说）

### 报告模板

```
### 漏洞描述
[一句话]

### 复现步骤
1. ...
2. ...

### 影响
- 攻击者需要什么条件？
- 影响哪些数据？
- 严重度估计：Critical / High / Medium / Low

### 环境
- 设备：Pixel 6 / Android 14
- App 版本：v0.8b (debug)
- 是否 root：否

### 建议修复
（可选）
```

---

## 已知安全考虑

### v0.7 SQLCipher 4.6.1

- 已知问题：[请查阅 SQLCipher CHANGELOG](https://github.com/sqlcipher/sqlcipher/blob/master/CHANGELOG.md)
- 我们用新 artifact `net.zetetic:sqlcipher-android`（旧 `android-database-sqlcipher` 已 EOL）

### EncryptedSharedPreferences 1.1.0-alpha06

- 是 alpha 版本，1.1.0-stable 尚未发布
- 备选方案：`Tink` 直接使用（待 v0.9 评估）

### WorkManager (v0.8c 计划)

- 提醒通知不包含任何患者数据
- 仅显示："该吃药了" + 药名（本地 Notification）

---

## 安全设计原则

我们遵循 [OWASP MASVS](https://mas.owasp.org/MASVS/) 的核心原则：

1. **存储安全** (MSTG-STORAGE-1/2): 加密敏感数据
2. **通信安全** (MSTG-NETWORK-1): 默认不发网络请求
3. **密码学** (MSTG-CRYPTO-1/2): 用 Keystore + SecureRandom，不硬编码密钥
4. **认证** (MSTG-AUTH-1): 无账号
5. **输入验证** (MSTG-INPUT-1): Compose 状态 hoisting + 类型安全
6. **构建安全** (MSTG-CODE-2): 不打包 debug 密钥到 release

---

## 安全审计历史

| 日期 | 审计方 | 范围 | 报告 |
|------|--------|------|------|
| 待 | — | v1.0 公测前 | — |

如果你有兴趣做安全审计，请用上面私密渠道联系。

---

## 致谢

感谢以下安全研究者（无）：
（本项目尚未收到第三方安全报告，期待你的贡献。）

---

## 免责声明

本应用**不是医疗器械**。所有 PK 估算、规则引擎输出、提醒通知仅供参考，**不构成医疗建议**。实际用药请遵医嘱。

开发者不为因使用本应用导致的任何医疗决策、用药错误、健康损害负责。
