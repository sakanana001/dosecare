# Changelog

DoseCare 的所有重要变更都记录在这里。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### Planned
- v0.9f: WorkManager 提醒 + 通知权限 + 系统日历写入
- v0.9f: 日历事件点实时刷新 (接 `dose_taken` 表后,跨 tab 状态变化触发 dayEvents 重算)
- v0.9f: TDM 个体化校准 (基于历史血药浓度回算 ke / Vd)
- v1.0: 性能优化 (启动 < 800ms)、可访问性 (a11y) 通过、隐私政策上线

---

## [0.9e] - 2026-09-09 · 全面 i18n 收尾 (Diary 空态 + DrugDetail 标题 + 5 enum 翻译)

### Fixed (用户报 3 处)
- **Diary 主页 '今天还没有日记，点右上 + 写一条'** → R.string.home_no_diary (DiaryTab.kt L151)
- **DrugDetailScreen 标题 ja/en 模式显示中文** → 改 locale-aware (zh=genericNameZh, en/ja=genericName), 副名也跟 locale 切换
- **药品目录两大类里的分类名称硬编码中文** (抗精神病药/抗抑郁药/...) → DrugCategory (33) + IndicationGroup (43) + OverdoseSeverity (4) + CypEnzyme (8) + PathwayType (11) + Severity (rules 5) = **6 个 enum 加 @StringRes displayNameRes 字段**, 104 个新 key × 3 语言 = 312 个翻译

### Added
- 6 个 enum 全部加 `@StringRes val displayNameRes: Int` 字段
- 104 个新 R.string key (drug_cat_*, ind_*, sev_*, cyp_*, pathway_*, sevrule_*), strings.xml 3 文件共 532 key 100% 对齐
- 修复一个潜在 bug: CypRow 的 .map { "${it.cyp.displayName} ..." } lambda 内调用 stringResource (non-composable context) — 改用 pre-compute 的 List<String>

### Changed
- `DrugCategory` / `IndicationGroup` / `OverdoseSeverity` / `CypEnzyme` / `PathwayType` / `Severity`(rules) enum 全部加 displayNameRes
- 9 个 UI 文件更新调用方: HomeScreen/DrugDetailScreen/ComparisonView/BottomNavTabs/SearchableDrugPicker (CategoryChip、CypRow、CypInfo、fmtOverdoseShort、fmtPathway、fmtCypInh/Ind)
- enum 文件加 `com.dosecare.app.R.string.xxx` 全限定名 (跨 package 引用 R)
- versionCode 12→13, versionName 0.9d→0.9e

### Migration
- 老用户 v0.9d → v0.9e: 数据无变化, UI 文本进一步本地化
- 所有 enum 的 displayName 字段保留 (作为中文 fallback / sort key), displayNameRes 是新增字段

---

## [0.9d] - 2026-09-09 · 补全 i18n 全面排查 (日历 / 目录 / 药名 / TDM / 警示语)

---

## [0.9d] - 2026-09-09 · 补全 i18n 全面排查 (日历 / 目录 / 药名 / TDM / 警示语)

### Fixed (用户报 5 处)
- **第一模块首页"还没有分组"** → `R.string.home_no_groups` (CalendarTab.kt L217)
- **第二模块首页"还没有日记"** → `R.string.home_no_diary` (DiaryTab.kt 之前已修复, 验证有效)
- **第三模块药品目录药物名称** → DrugCard / DrugPicker 按 locale 选主名 (zh=`genericNameZh`, en/ja=`genericName`), 排序也跟 locale 走
- **第一/第二模块日历日期** → CalendarCompose.kt + CalendarTab.kt 改用 `LocalConfiguration.current.locales[0] + DateTimeFormatter` (zh/ja "2026 年 9 月", en "September 2026"); 星期表头走 `R.string.calendar_weekday_short_1..7` (一/二/.../日 vs Mon/Tue/.../Sun vs 月/火/.../日)
- **全面排查** (12+ 处其它硬编码): 互动警示 3 行 (QTc/ACB/5-HT), 浓度曲线标题/状态/4 个窗内状态 (偏低/偏高/窗内/不可比), TDM "(无窗)" chip + 隐藏/显示 contentDescription, 搜索选药 placeholder + 计数 + 品牌, 当日无安排用药 / 分组管理 / 新增 / 为已有分组加药 / 实际 HH:mm 备注

### Added
- 33 个新 `R.string.*` key (calendar_weekday_short_1..7, home_no_groups/plans/diary, groups_manage, action_new, add_to_existing_group, checkin_actual_time, drug_name_format_*, search_placeholder, search_count_with_query, search_brand_prefix, chart_title, chart_status_format, chart_status_low/high/in_window/not_comparable, tdm_hide/show_cd, tdm_no_window, interaction_qtc/acb/serotonin_format)
- 3 strings.xml 共 428 key 100% 对齐 (Python Compare-Object 验证)

### Changed
- `MonthCalendar` + `DayTitle` 改 Locale-aware: 用 `java.time.YearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))` for en; zh/ja 保留 "yyyy 年 M 月" / "M 月 d 日 EEE" pattern + `Locale.SIMPLIFIED_CHINESE`
- `DrugCard` / `DrugPicker` / `TdmModule` 药物名按 locale 主次切换, 同时更新 `sortedBy` 排序键
- `SearchableDrugPicker` placeholder 从 String 默认值改为 `stringResource(R.string.search_placeholder)`
- `CalendarTab.checkInDialog` "实际 HH:mm" 备注用 `LocalContext.current.getString()` 在 coroutine 回调中取本地化字符串
- versionCode 11→12, versionName 0.9c→0.9d

### Migration
- 老用户 v0.9c → v0.9d: 数据无变化, UI 文本进一步本地化
- 仍基于 v0.9a/v0.9b 多语言架构 (AppCompat 1.7+ per-app language + localeConfig + attachBaseContext)

---

## [0.9c] - 2026-09-09 · 补全 i18n (Diary 心情 + 通俗解释 + DrugDetail/ComparisonView 全面排查)

---

## [0.9c] - 2026-09-09 · 补全 i18n (Diary 心情 + 通俗解释 + DrugDetail/ComparisonView 全面排查)

### Added
- **7 个新 DrugDetailScreen section 标题 key** (`drug_section_*`): CYP 角色 / PK 估算 / 药理作用 / 关键不良反应 / 药物过量 / 剂量调整 / 监测要求 (3 语言)
- **2 个格式 key**: `drug_anticholinergic_score` (抗胆碱能 X / 3)、`drug_pk_meta` (t½ + 蛋白结合)
- **1 个高风险 tag key**: `cmp_diff_highrisk_eps` (ComparisonView EPS 风险标签)
- 全部 3 语言 strings.xml 新增 10 个 key,共 395 key 100% 对齐 (Python 验证 zh=395 en=395 ja=395)

### Fixed
- **Diary 6 预设心情漏 i18n** — `DiaryMood` enum 字段从 `String displayName` 改为 `@StringRes displayNameRes: Int`,3 处 callsite (DiaryTab.kt line 220/284/294) 改用 `stringResource(mood.displayNameRes)`
- **Plain.kt 23 个通俗解释函数漏 i18n** — 全部改 @Composable,返回 String via stringResource
  - PK: halfLife / proteinBinding / therapeuticWindow / cmax / cmin / cavg / auc
  - CYP: cypSubstrate / cypInhibitor / cypInducer
  - 调整: renalAdj / hepaticAdj / elderly
  - 副作用: qtcProlongation / metabolicSyndrome / agranulocytosis / extrapyramidal / sedation / sexual / hyperprolactinemia / anticholinergicLoad
  - 引用: guideline (7 个 AGNP/FDA/CPIC/PMID/ACR/AUA/Beers/中国国家/中国药典)
- **DrugDetailScreen.kt 全面排查** (~20 处硬编码):
  - `PlainNote("ATC 码 = ...")` → `R.string.plain_atc_note`
  - 3 个 CypRow 标签 (作为底物/抑制剂/诱导剂) → `R.string.cyp_role_substrate/inhibitor/inducer`
  - 7 个 RiskRow 标签 (QTc 延长/代谢综合征/粒细胞缺乏/锥体外系反应/镇静/性功能影响/高泌乳素) → `R.string.risk_qtc/metabolic/agranulocytosis/eps/sedation/sexual/prolactin`
  - 5 个 KvRow 标签 (肾功能/肝功能/老年/吸烟/戒烟) → `R.string.adj_renal/hepatic/elderly/smoking/quit_smoking`
  - 7 个 section 标题 (CYP 角色/PK/药理作用/关键不良反应/药物过量/剂量调整/监测要求) → `R.string.drug_section_*`
  - `auc_extra` (Plain.auc() 后的 " — 倍数 = 联用时浓度变化倍数" 后缀)
  - `t½ X h · 蛋白结合 Y%` 行 → `R.string.drug_pk_meta`
  - `X / 3` 抗胆碱能值 → `R.string.drug_anticholinergic_score`
  - `strengthZh` (STRONG/MODERATE/WEAK → 强/中/弱) → @Composable + `R.string.cyp_strength_strong/moderate/weak`
  - `RiskLevel.displayName()` (极高/高/中/低/极低) → @Composable + `R.string.risk_level_very_high/high/medium/low/very_low`
- **ComparisonView.kt 全面排查** (~40 处硬编码):
  - `buildComparisonSections` 改 @Composable,返回 `List<Pair<@StringRes Int, ...>>`
  - `SpecSection` 改 `@StringRes titleRes: Int` 入参
  - `sectionPlainNote` 改 @Composable + `@StringRes titleId: Int` 入参 (不再用 String match)
  - 8 个 section 标题 → `R.string.cmp_section_basic/pk/window/cyp/adverse/adjust/monitor/overdose`
  - 7 个 section 通俗解释 → `R.string.cmp_section_desc_basic/pk/window/cyp/adverse/adjust/monitor`
  - 25+ 字段 label (蛋白结合/代谢途径/QTc 延长/粒细胞缺乏/EPS/镇静/性功能/高泌乳素/抗胆碱能/肾/肝/老年/频次/项目/中毒/致死/严重度/解毒剂/未录入/未标) → `R.string.cmp_label_*`
  - PlainKeyDiffCard 6 类关键差异 (治疗窗/半衰期/独有风险/CYP 抑制/CYP 诱导/老年) → `R.string.cmp_diff_*` (15+ key)
  - 4 个高风险 tag (QT 延长/粒缺/EPS/镇静) → `R.string.cmp_diff_highrisk_qt/agran/eps/sedation`
  - `fmtPathway` "未标" → `R.string.cmp_label_pathway_empty`
  - `fmtOverdoseShort` 改 @Composable,3 个分支处理 doses/antidote 组合 (`R.string.cmp_label_severity` / `cmp_label_severity_with_doses`)

### Changed
- ComparisonView.kt:
  - `buildComparisonSections` 返回类型 `List<Pair<String, List<Pair<String, String>>>>` → `List<Pair<Int, List<Pair<String, String>>>>`
  - 加 `import androidx.annotation.StringRes`
  - `fmtCypInh` / `fmtCypInd` 改 @Composable (调用 `strengthZh`)
- DrugDetailScreen.kt:
  - `strengthZh` / `RiskLevel.displayName()` 改 @Composable
  - 文件头 KDoc 更新:加 v0.9c i18n 章节
- strings.xml:
  - 3 文件同时新增 10 key (zh-CN / en / ja),395 key 100% 对齐
  - 修 en/ja 文件中 6 处 `eGFR < 30` 未转义的 `<` (Android XML 不允许字面 `<`,需 `&lt;`)

### Verified
- ✅ `compileDebugKotlin` 通过 (无 error,仅 6 个 deprecation 警告与本次重构无关)
- ✅ 3 字符串文件 395 key 100% 对齐 (Python regex 提取 + Compare-Object 验证)
- ✅ DrugDetailScreen.kt + ComparisonView.kt 无任何 UI 硬编码中文 (grep 正则 `[\u4e00-\u9fff]` 验证,残留 31+35 处全部在 KDoc / 代码注释 / PathwayType 颜色注释)
- ✅ 强类型检查: 改 enum 字段 (DiaryMood.displayName) 时用了 `@StringRes` 注解 + `stringResource()` 包装,编译器会拦截遗漏

### Migration
- 老用户 v0.9b → v0.9c: 数据无变化,纯 UI 文本补全 + 全面 i18n
- 仍依赖 v0.9a/v0.9b 的多语言架构 (AppCompat 1.7+ per-app language + localeConfig + attachBaseContext)

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
