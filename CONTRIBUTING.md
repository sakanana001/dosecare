# Contributing to DoseCare

感谢你对 DoseCare 的兴趣!我们欢迎任何形式的贡献：bug 报告、文档改进、翻译、新药数据、规则引擎规则、PK 模型校准、UI 改进、测试用例等。

## 📋 目录

- [行为准则](#行为准则)
- [我能贡献什么](#我能贡献什么)
- [开发环境搭建](#开发环境搭建)
- [项目结构](#项目结构)
- [代码规范](#代码规范)
- [提交流程](#提交流程)
- [数据贡献（药目录 / 规则）](#数据贡献药目录--规则)
- [测试](#测试)
- [翻译](#翻译)
- [Review 流程](#review-流程)

---

## 行为准则

本项目遵循 [Contributor Covenant 1.4](https://www.contributor-covenant.org/zh-cn/version/1/4/code-of-conduct.html)。
简版：

- 友善、包容、尊重不同背景
- 接受建设性批评
- 关注对社区最有利的事
- 展现同理心

---

## 我能贡献什么

### 适合新手
- 📝 改进文档 (错别字 / 翻译 / 例子)
- 🐛 报告 bug (提供复现步骤 + 设备/Android 版本)
- 🌍 翻译 UI 字符串 (见 `app/src/main/res/values/strings.xml`)
- ✅ 增加 JUnit 测试用例 (特别是 rule engine 边界 case)

### 适合经验开发者
- 💊 新增药物数据 (PK 参数 / CYP 谱 / 治疗窗 / 中毒症状)
- ⚖️ 新增规则 (新 CYP 相互作用 / 食物效应 / 老年剂量调整)
- 📊 改进 PK 模型 (Michaelis-Menten 多底物竞争 / TDM 校准)
- 🎨 UI/UX 改进 (Material 3 组件 / 无障碍)
- 🔐 安全加固 (加密模型 / 威胁分析)

### 不适合本仓库
- ❌ 联网功能 (v1.5 之前没有，schema 已预留但不要接)
- ❌ 任何云端账号系统
- ❌ 任何跟踪 / 遥测

---

## 开发环境搭建

### 必需
- **JDK 17** (建议 `C:\jdk-17` 或 Linux `/usr/lib/jvm/java-17`)
- **Android SDK** (compileSdk 36, build-tools 36, platform-tools 34+)
- **Gradle 9.x** (项目自带 wrapper，无需安装)

### 推荐
- **Android Studio Ladybug+** (2024.2)
- **git**
- **Python 3.10+** (用于 `scripts/` 下的数据生成 + 校验脚本)

### 步骤

```bash
# 1. Fork 并 clone
git clone https://github.com/<your-username>/dosecare.git
cd dosecare

# 2. 写 local.properties
cat > local.properties << EOF
sdk.dir=/path/to/Android/Sdk
EOF

# 3. 验证环境
./gradlew --version
./gradlew :app:assembleDebug

# 4. (可选) IDE 打开
studio64.exe .   # Android Studio
```

### 已知环境问题

- **项目路径含中文 (e.g. `精品神药`)**：JUnit test 跑不起来，因为 Gradle worker JVM 把 classpath 读成 GBK 乱码。**建议把项目挪到纯 ASCII 路径** (e.g. `~/projects/dosecare`)。Production code 不受影响 (emulator 验证全过)。
- **PowerShell 5.1** (Windows 默认)：用单引号 regex，禁用 Bash 转义。`scripts/` 下脚本兼容 PS5.1。

---

## 项目结构

```
app/src/main/java/com/dosecare/app/
├── domain/         # 纯 Kotlin 领域层 (无 Android 依赖，未来抽 KMP)
│   ├── catalog/    # DrugCatalog + CypProfile + DrugCatalogLoader + IndicationGroup
│   ├── pk/         # PkEngine + PkModel
│   ├── rules/      # RuleEngine + Interactions
│   ├── patient/    # PatientProfile
│   ├── prescription/ # Prescription + PrescriptionRepository
│   └── repository/ # Repository 接口 (KMP-friendly)
├── data/           # Android 数据层
│   ├── db/         # Room Entities + DAOs + AppDatabase + DataSeeder + DrugEntityMapper
│   ├── repository/ # DiaryRepository + DrugCatalogRepository + PrescriptionRepositoryRoom
│   ├── crypto/     # DbKeyProvider (Keystore)
│   └── migration/  # PrescriptionMigrator (v0.6 JSON → v0.7 Room)
├── di/             # Hilt Modules
├── ui/             # Compose UI
│   ├── theme/      # Color + Shape + Type
│   ├── CalendarTab.kt / DiaryTab.kt / BottomNavTabs.kt
│   ├── SearchableDrugPicker.kt / DrugDetailScreen.kt
│   ├── InteractiveConcentrationChart.kt / ComparisonView.kt
│   ├── InteractionScreen.kt / TdmModule.kt / InfoScreens.kt
│   ├── Plain.kt / PinyinInitial.kt (5 维搜索)
│   └── ViewModels.kt / CalendarViewModel.kt
├── DoseCareApp.kt  # @HiltAndroidApp
└── MainActivity.kt # 唯一 Activity
```

详细模块边界见 [`docs/01-ARCHITECTURE.md`](docs/01-ARCHITECTURE.md)。

---

## 代码规范

### Kotlin
- 遵循 [Kotlin 官方代码风格](https://kotlinlang.org/docs/coding-conventions.html)
- IDE 装 `ktlint` + `detekt` 插件 (建议)
- 函数 < 50 行优先，< 100 行硬上限
- `when` 表达式**穷举** enum，不要留 `else -> "其他"` 兜底 (用 `enum.name` / `enum.displayName`)

### Compose
- 优先用 Material 3 组件
- 自定义组件放 `ui/` 顶层，不嵌套在 screen 文件里
- 状态 hoisting：业务状态在 ViewModel / Repository，UI 只持 `remember` / `rememberSaveable` 本地态

### 数据库
- 新增 Room 实体：必须提供 `id: String = UUID.randomUUID().toString()` 默认值
- `@Index` **不要**加在外键列（KSP `MissingType`）— 只加在非外键列 (`timestamp` / `createdAt` 等)
- 改 schema 必须 bump `AppDatabase.version` + 写 `Migration`

### 测试
- 新增 drug 改 schema：跑 `DrugCatalogLoaderTest` 验证
- 新增 rule：写 `RuleEngineSmokeTest` case
- 重要 bug 修复：先写 JUnit 复现，再修

### 文档
- 改架构 / 改 schema：同步更新 `docs/`
- 新增 drug：填 `app/src/main/assets/drugs/v0.6.json` (用 `scripts/check_*.py` 校验)
- UI 字样不用外部设计参考命名

---

## 提交流程

### 1. 准备
```bash
# Fork 之后，加 upstream
git remote add upstream https://github.com/<upstream-org>/dosecare.git

# 拉最新
git fetch upstream
git checkout main
git rebase upstream/main

# 创建 feature 分支
git checkout -b feature/your-feature-name
# 或 fix/your-bug-name
# 或 docs/your-doc-update
```

### 2. 写代码
- 改完后跑：
  ```bash
  ./gradlew :app:assembleDebug
  ./gradlew :app:testDebugUnitTest
  ```
- 如果改 schema / 改 pk 模型：跑 `scripts/check_unmatched.py` 验证 JSON 数据

### 3. 提交
```bash
git add -A
git commit -m "feat: 添加 xxx"          # 新功能
# 或 "fix: 修复 xxx"
# 或 "docs: 更新 xxx"
# 或 "test: 覆盖 xxx"
# 或 "refactor: xxx"
# 或 "data: 新增药 xxx"

git push origin feature/your-feature-name
```

### 4. 开 PR
- 在 GitHub 上开 Pull Request
- 描述：
  - 改了什么 + 为什么
  - 测试方法（emulator 步骤 / JUnit case）
  - 截图（如果是 UI 改动）
  - 关联 Issue（如果有）
- 等 review

---

## 数据贡献（药目录 / 规则）

### 新增一个药

1. 在 `app/src/main/assets/drugs/v0.6.json` 加 entry
2. 必填字段：
   ```json
   {
     "id": "drug_id_snake_case",
     "genericName": "中文名",
     "englishName": "English Name",
     "category": "ANTIPSYCHOTIC_ATYPICAL",
     "atc": "N05AX12",
     "pk": {
       "ka": 1.5, "ke": 0.05, "vd": 50.0, "f": 0.8,
       "t_half_hours": 14.0, "primaryPathway": "CYP3A4"
     },
     "therapeuticWindow": {
       "low": 50, "high": 150, "unit": "ng/mL"
     },
     "cypProfile": {
       "substrates": ["CYP3A4", "CYP2D6"],
       "inhibitors": ["CYP2D6"],
       "inducers": []
     }
   }
   ```
3. 跑 `python scripts/check_unmatched.py` 验证
4. 跑 `./gradlew :app:testDebugUnitTest --tests DrugCatalogLoaderTest` 验证加载

**数据源**：必须来自 PubMed / FDA 标签 / Clinical Pharmacology 等可信源。在 commit message 注明来源。

### 新增一条规则

1. 在 `app/src/main/java/com/dosecare/app/domain/rules/Interactions.kt` 加
2. 模板：
   ```kotlin
   data class XxxRule(
       val id: String,
       val severity: Severity,
       val description: String,
       val mechanism: String,
       val evidenceSource: String   // PubMed PMID / FDA label
   ) : InteractionRule {
       override fun check(
           drugs: List<Drug>,
           patient: PatientProfile
       ): List<InteractionAlert> { ... }
   }
   ```
3. 在 `RuleEngine.kt` 注册
4. 在 `RuleEngineSmokeTest` 加 test case（必须有正/反例）

---

## 测试

```bash
# 全部单元测试
./gradlew :app:testDebugUnitTest

# 单个 class
./gradlew :app:testDebugUnitTest --tests DrugCatalogLoaderTest

# 单个方法
./gradlew :app:testDebugUnitTest --tests "DrugCatalogLoaderTest.v0_6_catalog_loads_with_159_drugs"

# 测 JSON 解析（绕过 Gradle 中文路径问题）
python scripts/check_unmatched.py
```

**测试覆盖目标**：
- domain 层 ≥ 90% (rule / pk / catalog)
- data 层 ≥ 60% (room dao / migrator)
- ui 层：手动 emulator 验证（暂不写 Compose UI test）

---

## 翻译

- UI 字符串：`app/src/main/res/values-<lang>/strings.xml`
- 文档：每篇 `docs/*.md` 顶部加英文摘要 + 中文正文
- 药物数据：每个药加 `englishName` 字段（中英双语）

---

## Review 流程

- 至少 1 个 maintainer approve 才能 merge
- 改 schema / 改 PK 模型：必须有 1 个医药专业 reviewer（用 `@reviewer-pharmacy` 标记）
- CI 必须过 (build + test)
- PR 不活动 30 天会被 close，rebase 后重开

---

## 致谢

感谢所有贡献者！你的名字会出现在 [`docs/CONTRIBUTORS.md`](docs/CONTRIBUTORS.md)。

## 联系方式

- GitHub Issues: 功能建议 / bug
- GitHub Discussions: 一般讨论 / 想法
- Email (见 `About` 屏)

医疗专业问题建议用 [Issues](https://github.com/issues) 标记 `pharmacy-review`。
