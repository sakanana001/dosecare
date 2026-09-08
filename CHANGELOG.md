# Changelog

DoseCare 的所有重要变更都记录在这里。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### Planned
- v0.9b: WorkManager 提醒 + 通知权限 + 系统日历写入
- v0.9b: 日历事件点实时刷新 (接 `dose_taken` 表后,跨 tab 状态变化触发 dayEvents 重算)
- v0.9b: TDM 个体化校准 (基于历史血药浓度回算 ke / Vd)
- v1.0: 性能优化 (启动 < 800ms)、可访问性 (a11y) 通过、隐私政策上线

---

## [0.9b] - 2026-09-08 · 多语言系统 bugfix (localeConfig + attachBaseContext)

### Fixed
- **API 33+ per-app language 需要 `localeConfig`** — v0.9a 在 Android 17 emulator (API 37) 上 setApplicationLocales 不完整生效
  - 加 `app/src/main/res/xml/locales_config.xml` (3 locale: zh-CN / en / ja)
  - manifest `<application>` 加 `android:localeConfig="@xml/locales_config"`
- **Activity recreate 后 Resources Configuration 不刷新** — 之前 Activity recreate 后 stringResource() 还是返回旧 locale
  - MainActivity 加 `attachBaseContext()` override,根据 SharedPreferences 选的语言强制 createConfigurationContext
  - 这样无论 AppCompat 是否触发 Activity recreate,新 Activity 第一时间拿到正确 Resources
- **JVM Locale (java.time / java.text) 跟 Android framework 错位** — emulator 上 framework=en-US 但 JVM=zh-CN
  - LocaleController.applyLocale 加 `Locale.setDefault()` 同步 (无论显式还是 SYSTEM 模式)
  - 解决 CalendarCompose 的月/日/星期名始终是 emulator 默认值的问题

### Changed
- LocaleController:
  - 加 `peekLanguage(context)` 静态读 SharedPreferences (无副作用)
  - 加 `currentActivity` 引用 + `registerActivity/unregisterActivity` 生命周期管理
- MainActivity:
  - 加 `attachBaseContext()` override (必须在 super 之前)
  - 加 `registerActivity/unregisterActivity(this)`
  - 改用 v0.9a 的 peekLanguage(context) 而非依赖 StateFlow
- AndroidManifest:
  - 加 `android:localeConfig="@xml/locales_config"` 到 `<application>`
- New file: `app/src/main/res/xml/locales_config.xml`

### Verified (emulator API 37 Android 17)
- ✅ 启动默认 (SYSTEM 模式) → 跟 emulator 框架 locale (en-US) 走英文
- ✅ 切到日本語 → 立刻 stringResource 走日文 + Activity 自动 recreate
- ✅ 切到简体中文 → 立刻中文
- ✅ 切到 English → 立刻英文
- ✅ force-stop + 重开 → 持久化语言 (SharedPreferences + attachBaseContext 双重保险)
- ✅ Language Picker Dialog 4 选项 (跟随系统 / 简体中文 / English / 日本語)

### Known Limitations
- 日历 widget 内的 "2026 年 9 月" / "周二" 等系统级时间格式 (Compose Material 3 内置) 仍跟 system JVM locale 走
  实际设备上 framework + JVM 一致所以正常,emulator 才有这个矛盾
  v0.9c 计划把 CalendarCompose.kt 重写,完全用 stringResource 控制月/日显示

---

## [0.9a] - 2026-09-08 · 多语言系统 (zh-CN / en / ja)

### Added
- **多语言系统 (i18n)** — 3 语言
  - 简体中文 (zh-CN, 默认)
  - English (en)
  - 日本語 (ja)
  - 抽 221 个 UI 字符串到 `res/values{,-en,-ja}/strings.xml`,全量 3 语言对照翻译
- **语言设置入口** — 在「更多 → 设置」页面新增 `🌐 语言` 卡片
  - 点击弹 RadioButton Dialog: 跟随系统 / 简体中文 / English / 日本語
  - 选择后落盘 SharedPreferences,AppCompatDelegate.setApplicationLocales 应用
  - 旧版本 (API 26-32) 由 AppCompat 1.7+ 兼容层处理,Android 13+ 走系统 Per-App Language
- **`LocaleController` 状态机** (`ui/locale/LocaleController.kt`)
  - `AppLanguage` enum (SYSTEM / ZH_CN / EN / JA)
  - `LocaleController` singleton 模式同 ThemeController: load + setLanguage + StateFlow
  - MainActivity.onCreate 在 super 之前 load,确保新 locale 立即生效
- **LanguageSettingsCard Composable** (`ui/locale/LanguagePicker.kt`)
  - 卡片显示当前语言,点开弹 4 选 1 Dialog
  - 选完自动触发 Activity recreate (AppCompat 内部做)

### Changed
- **`DarkModePref.displayName`** 字段从 `String` 改为 `@StringRes displayNameRes: Int`,所有 DarkModePref 显示走 stringResource
- **`AccentColor.name`** 字段从 `String` 改为 `@StringRes nameRes: Int`,主题菜单/已选徽章显示走 stringResource
- **所有 13 个 UI 文件** 重构:硬编码中文字符串 → `stringResource(R.string.xxx)`,import `androidx.compose.ui.res.stringResource` + `com.dosecare.app.R`
- **依赖**: 加 `androidx.appcompat:appcompat:1.7.0` (per-app language API 兼容层)
- **AboutScreen / ContactScreen** 文案保留品牌调性,多语言显示

### Verified
- ✅ Build: 27s / 8 tasks / 0 error
- ✅ 13 个 UI 文件全部 stringResource() 化,无遗漏
- ✅ 3 语言 strings.xml 221 key 完全对齐
- ✅ compileDebugKotlin 通过,只有 deprecation warnings
- ⏳ Emulator 验证 3 语言切换 (待 v0.9a build APK 测试)

### Migration
- 旧用户 v0.8d 升级到 v0.9a: 数据无变化,只是 UI 文本多语言
- 首次安装用户: 默认跟随系统语言 (中文/英文/日文 → 对应 locale;其他 → 简体中文)
- 设置改语言后: 立即生效 (AppCompat 自动 recreate Activity)

---

---

## [0.8d] - 2026-09-08 · 自定义调色 + 关于页增强

### Added
- **自定义主题色调色盘** (`ui/ColorPickerDialog.kt`)
  - 240dp SV 圆盘 (横向 saturation / 纵向 value,3 层渐变叠加)
  - Hue 横向滑块 (rainbow 渐变轨道)
  - Value 横向滑块 (当前 hue+sat,黑→纯色)
  - HEX 输入框 (#RRGGBB,输入即时同步 HSV)
  - 实时 R/G/B 数值显示
  - OK / Cancel 按钮,确认后写入 SharedPreferences
- **ThemeController 扩展**
  - `ThemeState.customColor: Color?` 字段,优先级高于 5 预设
  - `AccentColors.deriveFromColor(Color)` — RGB→HSV 派生完整 light/dark palette (primary / primaryContainer / onColors),灰度主色强制 0.4+ 饱和度避免 container 灰塌
  - `setCustomColor()` / `clearCustomColor()` API
- **ThemeMenu 增强**
  - 末尾新增"+" 圆点 → 打开 ColorPickerDialog
  - 选中 customColor 时,菜单内显示"自定义"色块(带 ✓) + 旁侧 × 清除按钮
  - 5 预设圆点选中态在 `customColor != null` 时取消(互斥)

### Changed
- **AboutScreen 增强** (`ui/InfoScreens.kt`)
  - 新增 "🔗 仓库源代码" 卡片 — `https://github.com/sakanana001/dosecare` (clickable, LocalUriHandler)
  - 新增 "💌 联系作者" 卡片 — `daisukikiki01@gmail.com` (clickable, mailto: intent)
  - 顶部版本号: `v0.8a · Debug` → `v0.8d · Debug`
- **ContactScreen 更新**
  - 邮件地址: `dosecare@example.com` → `daisukikiki01@gmail.com`
  - 邮件 / GitHub Issues URL 均改为 clickable 链接 (LocalUriHandler)
  - Issues URL 保持: `https://github.com/sakanana001/dosecare/issues`

### Verified
- ✅ Build: 21s / 42 tasks / 0 error
- ✅ Emulator (Pixel 6 AVD, 1080×2400) install + launch
- ✅ ThemeMenu 渲染 5 预设 + "+" 圆点 (v0.8c 持久化 → 重启 App 自动恢复墨绿主题)
- ✅ ColorPickerDialog 弹窗渲染 SV 圆盘 + 滑块 + HEX
- ✅ Long-press 药行 → EditDrugDialog / 分组 → EditGroupDialog
- ✅ Dark mode (跟随系统 / 亮色 / 暗色 3 模式) 切换正常
- ✅ About 页新 2 个卡片 + mailto 链接验证可见

---

## [0.8c] - 2026-09-07 · 主题持久化

### Added
- **ThemeController singleton + StateFlow** (`ui/theme/ThemePreferences.kt`)
  - `DarkModePref` enum (SYSTEM / LIGHT / DARK) + displayName
  - `AccentColor` data class (5 预设) + `AccentColors.all` 列表
  - `ThemeState(darkMode, accentIndex)` immutable state
  - `load(context)` 从 SharedPreferences 读,`setDarkMode()` / `setAccent()` 落盘 + 触发 StateFlow
- **Theme.kt 圆角加大**
  - extraSmall 12dp / small 14dp / medium 18dp / large 24dp / extraLarge 32dp
  - 比 Material 3 默认 (4/8/16/20) 更柔和,医疗 App 安心感
- **NeutralLight / NeutralDark 抽离** — secondary/tertiary/error/background/surface 不受 accent 影响
- **ThemeMenu Composable** (`CalendarTab.kt`)
  - 3 主题模式 RadioButton (跟随系统 / 亮色 / 暗色)
  - 5 主题色圆点 (浅蓝 / 暖橙 / 墨绿 / 玫瑰红 / 深紫) — 选中带勾
- **Palette IconButton** — 右上角 🎨 触发 ThemeMenu DropdownMenu

### Planned (deferred to v0.9)
- v0.9: WorkManager 提醒 + 通知权限 + 系统日历写入
- v0.9: 日历事件点实时刷新

---

## [0.8b] - 2026-09-05 · 体验升级

## [0.8b] - 2025-XX-XX · 体验升级

### Added
- **拼音首字母搜索**：AddDrugDialog 搜索框支持中文 / 英文 / 品牌 / ATC / 中文→拼音首字母 (e.g. "lpt" → 利培酮/帕利哌酮/普罗帕酮) 五维匹配，与 SearchableDrugPicker 行为一致
- **多 slot 用药时间**：`PrescribedDrug.times: List<String>` 字段，`freq=1/2/3/4` 时分别装 1-4 个独立时间按钮 (Material 3 旋转时钟 TimePicker)，为 v0.8c WorkManager 提醒预埋
- **UI 主题升级**：
  - 主色 `#5B8DBE` (柔和天蓝，比 v0.7 的 `#2C5F8D` 更亮)
  - Secondary `#8E6FBE` (柔和紫) + Tertiary `#6BBEA8` (柔和绿)
  - 背景 `#FAFBFC` (近白微暖)
  - 圆角加大: 按钮 12dp / 卡片 16dp / chip 8dp

### Fixed
- **打卡 bug**：`onConfirm` 改用 `target.scheduledMillis` 作为 `DoseTaken.timestamp` (而非 `System.currentTimeMillis()`)，用户实际打卡时间只写进 `note` 字段，匹配窗口从 1h 放宽到 6h
- **返回栈泄漏**：AppRoot 加 8 个 `BackHandler(enabled=xxx)` 拦截 DrugDetail / Catalog / Compare / Interactions / TDM / Settings / About / Contact 链，避免子页返回到桌面
- **跨单位浓度比较**：DrugCatalog 加 `cMaxUnitFactor: Double` + `skipTherapeuticWindowBand: Boolean` 字段，Chart legend/yMax/治疗窗带 3 处统一乘 factor

### Changed
- `OpenSourceScreen` 内容并入 `AboutScreen` (AGPL-3.0 + Copyright + 6 个开源依赖)
- MoreTab 删"开源许可证"独立项，`关于` 副标题改为"数据源 / 许可证 / 局限性"
- 全局移除 UI 字样中外部设计参考的命名（保留技术 comments 致谢）

### Verified
- Build + install + Pixel 6 emulator 全过
- 输入 "lpt" 过滤出 3 个匹配药 ✅
- 选 利培酮 → 3 次/日 → 3 个独立 time slot 按钮 (08:00/14:00/20:00) ✅
- UI 色调清淡 + 圆角加大 ✅

---

## [0.8a] - 2025-XX-XX · 3-tab 月历重构

### Added
- **3-tab 底栏**：`Calendar` (月历 + 用药计划) / `Diary` (心情日记) / `More` (8 子项: 药物目录 / 双药对比 / 相互作用 / 血药浓度推测 / 化验 / 设置 / 关于 / 联系)
- **跨 tab 共享月历**：`CalendarViewModel` (object 单例) 持 `selectedDate` / `currentMonth` / `dayEvents` 状态，切换 tab 保留选中日和当月
- **心情日记**：`DiaryEntryEntity` + `DiaryMood` enum (Happy / Calm / Anxious / Depressed / Angry / Tired 6 选，默认 Calm 😌)
- **DiaryRepository** (Hilt @Singleton)：add / delete / getForDay / getBetween / observeBetween
- **月历格子事件点**：蓝=med / 橙=diary，选中态蓝实心 + 今日蓝边
- **InfoScreens**：AboutScreen / OpenSourceScreen / ContactScreen

### Database
- AppDatabase **version 3 → 4**
- 新增 `diary_entry` 表 (`id` String UUID PK, `timestamp` index, `mood` String, `text` String?)

### Changed
- 旧 4-tab 布局 (血药浓度 / 处方 / 目录 / 对比 + 相互作用) → 3-tab 大重构
- `PrescriptionTab` 移到 `.minimax/.trash/` (功能已并入 CalendarTab)

---

## [0.7] - 2025-XX-XX · Room + SQLCipher 加密栈

### Added
- **Hilt 2.59.2** + **KSP 2.2.10-2.0.2** 依赖注入
- **Room 2.7.0** 数据库
- **SQLCipher 4.6.1** (新 artifact `net.zetetic:sqlcipher-android`；旧 `android-database-sqlcipher` 已弃用)
- **DbKeyProvider**：`SecureRandom` 生成 32 字节 passphrase → 存 EncryptedSharedPreferences (AES256_GCM, key wrapped by Android Keystore)
- **AppDatabase v1**：11 张静态表 (drug / drug_brand / drug_indication / drug_active_metabolite / drug_pk / drug_therapeutic_window / drug_overdose / drug_clinical / cyp_role / drug_critical_interaction / app_metadata)
- **11 DAOs** + **DataSeeder.seedIfNeeded()** 从 `assets/drugs/v0.6.json` 异步灌库
- **DrugEntityMapper**：11 张静态表拼回领域 `Drug` 对象
- **DatabaseModule** (Hilt) 同步灌库 + `loadFromDb()`

### Database
- AppDatabase v1 → v3 (新增 6 张用户表)
- 新增 `prescription_group` / `prescribed_drug` / `dose_taken` / `tdm_history` / `lab_result` / `user_preference` (全部 String UUID PK)
- `fallbackToDestructiveMigration()` (开发期)

### Migration
- **PrescriptionMigrator**：从 v0.6 SharedPreferences JSON 迁移老数据到 Room

### Fixed
- KSP `MissingType`：Room `@Index` 不能加在外键列，只加在非外键列 (`prescribed_drug_id` 等)

### Changed
- `PrescriptionRepository` (JSON-based) → `PrescriptionRepositoryRoom` (Hilt)
- `DrugCatalogLoader` (单例) → `DrugCatalogService` (Hilt @Singleton from Room)
- `Application` 加 `@HiltAndroidApp`
- `MainActivity` 加 `@AndroidEntryPoint`

---

## [0.6] - 2025-XX-XX · 200+ 药 + 跨单位治疗窗

### Added
- **DrugCatalog JSON v0.6.0** (880KB, **202 药**)
- 缓释 / 控释 / 周制剂：`bupropion_xl` / `venlafaxine_xr` / `paroxetine_cr` / `fluoxetine_weekly`
- 异构体：`arformoterol` / `levalbuterol` / `dextroamphetamine` / `levetiracetam`
- 活性代谢物：`desvenlafaxine` / `norketamine` / `morphine_6_glucuronide`
- 抗精神病新药：`cariprazine` / `lumateperone` / `xanomeline+trospium`
- **跨单位治疗窗**：每个药加 `cMaxUnitFactor: Double`，chart legend / yMax / 治疗窗带 3 处统一乘 factor
- **TDM 多药曲线**：跨单位浓度叠加 + 剂量调整建议
- **DoseAdjustment 字段** (Kidney / Liver / Age 调整建议)

### Verified
- `DrugCatalogLoaderTest`：159 药 v0.6 加载通过
- `LorazepamCheckTest`：跨单位 (mg/L ↔ ng/mL) 浓度比较正确
- `RuleEngineSmokeTest`：CYP 抑制/诱导规则、QTc 累积、5-HT 综合征规则通过

---

## [0.5] - 2025-XX-XX · 完整覆盖 (200 药前)

### Added
- 200 药（覆盖精神科 / 内分泌 / 心血管 / 抗癫痫 / 止痛 / 抗组胺 / 草本）
- critical interactions 表格完成
- 接入吸烟状态、食物/咖啡因效应
- 多药 PK 曲线叠加 (代数和)
- 漏服智能补服决策卡
- 依从性热力图
- 症状打卡 (情绪 / 睡眠 / 自杀意念筛查 PHQ-9 / GAD-7)
- 化验结果时间线
- 备份恢复 (带密码)

---

## [0.3] - 2025-XX-XX · 核心扩展 (100 药)

### Added
- 100 药 (补齐大部分抗精神病 + 抗抑郁 + 心境稳定剂 + 苯二氮䓬)
- CYP 代谢相互作用检测 (基本规则)
- QTc 累积风险
- 抗胆碱能负荷
- 5-HT 综合征风险
- 治疗窗偏离报警
- 多药并排对比页 (一加风格)
- 肾/肝剂量调整建议

### Verified
- 50 条规则测试用例通过
- 5 个真实冲突场景检测率 100%

---

## [0.1] - 2025-XX-XX · MVP (20 药)

### Added
- 20 个核心药：氯氮平、奥氮平、利培酮、阿立哌唑、喹硫平、氟哌啶醇、齐拉西酮、碳酸锂、丙戊酸钠、氟西汀、舍曲林、帕罗西汀、文拉法辛、米氮平、地西泮、阿普唑唑、苯海索、劳拉西泮、奥卡西平、拉莫三嗪
- 单药 PK 曲线 (一房室)
- 治疗窗显示 + 状态 (低于 / 在窗 / 高于)
- 服药打卡 (时间 + 剂量)
- 提醒 (AlarmManager)
- 主屏极简
- 加密 Room 存储 (SharedPreferences 时代，后 v0.7 升级为 SQLCipher)
- 数据备份导出 (手动 JSON)

---

## 版本号约定

本项目使用 [Semantic Versioning](https://semver.org/lang/zh-CN/)：

- **MAJOR** (1.0 → 2.0): 架构级变更 / 联网同步 / 跨端
- **MINOR** (0.1 → 0.3): 重大功能 (加密栈 / 3-tab 重构)
- **PATCH** (0.8a → 0.8b): 体验升级 (UI 调色 / 搜索优化 / bug 修复)

`a` / `b` / `c` 后缀代表 MINOR 内的迭代 (e.g. v0.8a 月历重构 → v0.8b UI 升级 → v0.8c 提醒)。
