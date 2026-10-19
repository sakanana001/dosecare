# DoseCare — 项目档案

> 仓内 [README](../README.md) 是面向新用户的"这是什么"页面；本文档面向贡献者和
> 审阅者，回答的是"为什么这样做、演进到这一步经历了什么"。重点说明 rebrand
> 决策、基于上游 Featherline 之上叠加的 PK 改进，以及把 fork 变成多药管理工具的
> 药理合并。

## 谱系

DoseCare 仓库在 [sakanana001/dosecare](https://github.com/sakanana001/dosecare)，由两条
上游工作线合并进同一个代码库：

| 层 | 项目 | 许可证 | 贡献内容 |
|---|---|---|---|
| 底座 | [mkx173/Featherline](https://github.com/mkx173/Featherline) | GPL-3.0 | 原始应用架构、UI 体系、数据模型、提醒流水线、血检目录、备份格式、widget、journal、本地化 |
| 中间层 | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | GPL-3.0 | rebrand、testosterone undecanoate 的三室 PK 引擎（Runge-Kutta 4）、gel 皮肤储库、per-user `kClearInjection` / `k31` override、库存跟踪、加密备份、生物识别应用锁 |
| 药理层 | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) | AGPL-3.0 | 202 药目录、CYP450 / QTc 相互作用规则引擎、血药浓度（TDM）估算、双药规格对比、剂量调整与监测指导 |

PicoHRT 这一线从上游 Featherline 的 `v0.6.0` fork 出来；[CHANGELOG.md](CHANGELOG.md)
里 `## [Unreleased]` 段是新增工作，`## [0.6.0]` 及之前为上游历史，原文保留。
DoseCare 这一线在合并前是独立 App。

AGPL-3.0 是 GPL-3.0 的超集，所以从 PicoHRT 层的 GPL-3.0 到本项目的 AGPL-3.0
是单向上升；两条上游线都是 GPL 系，与 AGPL-3.0 兼容。分层归属见
[third-party-notices.md](third-party-notices.md)。

| | 上游 (Featherline) | 中间层 (PicoHRT) | 本项目 (DoseCare) |
|---|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` | `com.dosecare.app` |
| 仓库 | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) |
| 应用名 | Featherline | PicoHRT | DoseCare |
| 许可证 | GPL-3.0 | GPL-3.0 | AGPL-3.0 |
| 隐私政策 | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | 暂用仓内 [privacy.md](privacy.md) | 暂用仓内 [privacy.md](privacy.md)，托管政策待发布 |
| TU 三室 PK 引擎 | 无 | 本层新增 | 沿用 |
| Gel 皮肤储库 | 无 | 本层新增 | 沿用 |
| Per-user `kClearInjection` / 三室 `k31` override | 无 | 本层新增 | 沿用 |
| 202 药理目录 | 无 | 无 | 合并时新增 |
| CYP450 / QTc 相互作用规则 | 无 | 无 | 合并时新增 |
| 血药浓度 (TDM) 估算 | 无 | 无 | 合并时新增 |
| 双药规格对比 | 无 | 无 | 合并时新增 |

包名 rename 是完整 rebrand——`namespace` 和 `applicationId` 都改——所以几个
app 可以并存安装互不覆盖。启动页图标和启动主题沿用上游是有意的；只改了
`app_name`。如果本项目后续想换自己的图标，是个小改动。

## 药理合并

DoseCare 这一线带来的是 HRT 追踪原本没有对应物的药理引擎。合并的方式是把这套能力
作为新模块**并列**放进来，而不是重写 HRT 那一侧：

- **目录。** `app/src/main/assets/pharmacology/catalog.json` 内置 202 条药品、31 个
  药理学分类。每条带一房室口服 PK 模型、CYP450 谱、风险画像（QTc、代谢综合征、
  粒细胞缺乏、锥体外系、嗜睡、性功能、高催乳素、抗胆碱负荷）、肾/肝剂量调整、
  过量信息、监测要求和关键相互作用。启动时一次性加载进内存 `DrugCatalogService`，
  之后任何查询都不碰磁盘也不碰网络。
- **规则引擎。** `model/pharmacology/RuleEngine.kt` 按十类规则评估当前方案——CYP450
  相互作用（抑制与诱导）、QTc 叠加、抗胆碱负荷、5-HT 综合征、治疗窗偏离、
  肾/肝剂量调整，以及四类叠加药理作用（镇静、出血、多巴胺拮抗、低血压）。每条
  规则都是"药品列表 + 患者画像"上的纯函数，可以单测，引擎本身不存状态。
- **桥接。** 引擎需要的是每个药的剂量和频次，不只是 drug id。
  `InteractionAnalysisRepository` 读用户自己的方案，把每个已关联目录的药的规格强度
  和所在组的排班解析出来，交给引擎一个 `UserDrugForRule`。这正是暴露倍数能按用户
  实际吃的量缩放、而不是按目录默认值缩放的原因。关联本身是用户药品表上的一个列：
  `medicines.drugCatalogId`（schema `v11` 新增；空串表示"未关联"）。
- **页面。** 合并带进来三个页面，故意不放进底部导航——它们是压在 Settings 页签
  之上的：`Screen.Interactions`（设置 → Interactions）、`Screen.Tdm`
  （设置 → Blood level monitoring）、`Screen.Comparison`
  （设置 → Two-drug spec comparison）。药品详情页加了 `PharmacologyDossierSection`，
  让用户把某个药绑到目录条目上，就地读它的药理档案。

合并进来的页面遵循底座的约定，而不是独立 App 的约定：`@HiltViewModel` +
`hiltViewModel()`，而不是进程级单例 `MutableStateFlow`；所有用户可见文案走 string
资源；沿用底座的设计体系。202 药目录是静态资源，所以合并没有引入网络依赖。

## 项目使命

上游 Featherline 的使命原样保留：本地、加密、无账号的 HRT 用药记录工具，离线
运行，从你的用药历史推算 PK，尊重这类数据是健康数据、永远不离开设备。PicoHRT
这一层在此基础上增加：

- **长效注射酯的 PK 精度。** 上游的 1 室解析 Bateman 短路对短效途径够用，但
  testosterone undecanoate 文档给出的 90 ± 40 天终末半衰期是深部外周现象，
  1 室无法表达。该层把 TU 路径换成 3 室 RK4 求解器
  （[详情见下](#v06-之后的-pk-引擎改动)）。
- **透皮 testosterone gel 的皮肤储库路径。** 上游把 gel 当 1 室剂量处理，
  导致稳态 AndroGel 系统性低估 38–57 %。该层加入 2 室 Michaelis-Menten + kCl
  皮肤储库，让 gel 曲线匹配包装说明书
  （[详情见下](#v06-之后的-pk-引擎改动)）。
- **per-user override 数据层，让用户能调自己看得到的参数。** `kClearInjection` 和
  三室 `k31` 暴露给用户（DataStore 持久化），这样血检结果跟人群均值不一致的
  用户可以微调模型，不用改代码
  （[详情见下](#per-user-override-数据层)）。

药理合并加进来的是第四样东西，而且性质不同：不是把某种激素估得更准，而是给药
本身做参考。目录说明一个药靠什么代谢、和什么冲突、安全浓度长什么样、要盯什么；
规则引擎把这份参考应用到用户自己的方案上。其他一切——提醒流水线、widget、
journal、血检目录、备份格式、本地化、安全策略——都是上游行为，原样保留不动。

## v0.6 之后的 PK 引擎改动

本项目的 PK 引擎是 5 个 PR 在 `v0.6.0` 之上累积的结果，由 PicoHRT 层落地。
按落地顺序列出，标注 commit 和支撑改动的指标。每场景的完整数据表在自动生成的报告
`app/app/build/reports/pk-v3-statistical-report.docx` 里（每次跑
`PkStatisticalReportTest` 都会重生成）。

### PR-V3-TU：testosterone undecanoate 的三室引擎

**Commit：** 完整 data class + RK4 实现在
`app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt`；UI 层
commit `bb33a9e`。

**为什么。** 上游的 1 室解析 Bateman 短路是单个状态
`C(t) = (F·D/Vd) · (k_a / (k_a - k)) · (e^(-k·t) - e^(-k_a·t))`，
一个指数衰减加一个吸收。Nebido 的 SmPC 给出 90 ± 40 天终末半衰期，
这是 *深部外周* 室的时间常数，不是中心的。1 室模型下，无论参数怎么选，
要么峰值崩，要么 90 天尾拉不出来。

**做了什么。** TU 路径切换到 3 状态 ODE
`[central, shallow, deep]`，用 Runge-Kutta 4 积分。深部外周储库
（Vd_d = 3.5 L/kg）承载长尾药量。浅外周（Vd_s = 1.0 L/kg,
k12 = 0.15 h⁻¹, k21 = 0.18 h⁻¹）捕捉 Cmax 时序依赖的肌肉快平衡。
depot 室仍然解析求解（肌注 depot 的双指数吸收），解析的 depot 药量作为
RK4 步进器的源项。

**结果。** TU MAE 从 1 室 broken（v2 retune：13.00 ng/mL，bias −100 %）
降到 2.53 ng/mL，bias −9.3 %。整体 MAPE 从 47.7 % 降到 31.6 %。

### PR-V3.1-TU-retune：k31 0.005 → 0.0003

**为什么。** 第一个 PR 把深部外周半衰期留在
`ln(2)/0.005 ≈ 138 h ≈ 5.8 d`——尾部和短效酯一个量级。峰值拟合和单点
数据都还行，但长期预测没到位。

**做了什么。** `k31` 从 0.005 h⁻¹ 降到 0.0003 h⁻¹，深部外周半衰期变成
`ln(2)/0.0003 ≈ 2311 h ≈ 96 d`，落在 SmPC 90 ± 40 d 的中段。对 Cmax /
Tmax 几乎没影响（前 4 周 deep→central 通量很小），但尾行为到位了。

**结果。** v3.1 + PR2（下一个）整体 MAPE 落到 **20.9 %**（从 31.6 % 降），
TU MAE 2.62 ng/mL，gel 5g / 10g MAE 1.21 / 1.55 ng/mL（AndroGel 锚点）。

### PR2-gel：testosterone gel 的皮肤储库 2 室 MM + kCl

**Commit：** `a353b10`（在 rebrand 之前就在上游 `com.mkx.hrttracker` 包下落地，
原样保留，此后每次 rename 都只做机械改写）。

**为什么。** 上游把 gel 当普通 1 室剂量加 depot 吸收处理。透皮 testosterone gel
的实际 PK 主导因素是皮肤储库——角质层 depot 历经数小时释放入血循环，第一步
饱和，加一个清除项。

**做了什么。** testosterone gel 的吸收换成 2 室 Michaelis-Menten + kCl：
皮肤 depot → 系统 depot → central，`kCl` 从系统 depot 回流到皮肤。只对
TESTOSTERONE 启用此路径；estradiol gel 保持 1 室。

**结果。** AndroGel 5g 稳态低估从 3.22 ng/mL 误差（1 室）降到 1.21 ng/mL
（皮肤储库）。AndroGel 10g 从 3.04 ng/mL 降到 1.55 ng/mL。

### PR3-UI-3comp-badge：T 曲线上的 "3-comp" 徽章

**Commit：** `bb33a9e`。

**为什么。** 引擎在活动事件是 TU 时自动切 3 室，但用户没途径知道是哪个引擎
在跑。如果 3 室结果跟用户自己的血检不符，他们需要一个可视提示，才能决定要不要
去看 override 层。

**做了什么。** `PkTrendResult` 和 `MainChartUiState` 加 `usesThreeCompModel:
Boolean`。Home chart header 在 flag 为 true 时渲染 "3-comp" / "3 室" 徽章
（tertiaryContainer 底色）。点徽章是 per-user override 对话框的未来入口（本 PR
没接——override 数据层到位，UI 是下一步）。

**结果。** 194 个 main UI 测试 + 68 个 PK 测试全部通过新 flag 和新徽章。

### Per-user override 数据层

**Commit：** `c4b8957`。

**为什么。** 人群均值 PK 模型有用，但不是 *你的* PK。一个血检连续 3 次都比人群
曲线高 15 % 的用户，不会信任一个比他血检结果还低 15 % 的 tracker。没有
override 层，唯一的出路就是"不用了"或"fork 改代码"——对个人记录工具来说都
不可接受。

**做了什么。** 加了 `PkTUserOverride` data class，两个字段
（`kClearInjection: Double?`, `threeCompK31: Double?`）。引擎把它当作 catalog
默认值的可选 override 读取：
`userOverride.kClearInjection ?: core.kClearInjection` 和
`userOverride.threeCompK31 ?: threeCompOverrides[compound].k31`。值存在两个新
DataStore key（`pkClearInjectionOverrideKey`, `pkK31OverrideKey`），通过
`SettingsRepository` 和 `SettingsState` 暴露，`MainViewModel` 每次调
`simulateMainTestosteroneTrend()` 都读取。

**Scope。** v1 严格两个参数：`kClearInjection` 和三室 `k31`。k12 / k21 / k13
明确**不在 scope**——肌肉平衡速率影响早期峰值，调错的代价比中央清除或深部
回流速率更严重，所以留给未来的"高级 override"模式（带更强警告）。

**没做的（下个 PR）。** 从 home chart "3-comp" 徽章点进去编辑 override 的 UI。
数据层到位、徽章已挂，纯 Compose 工作，挂在现有入口上。

## 构建、测试、CI 改动

- `app/build.gradle.kts`：`namespace` 和 `applicationId` 都是 `com.dosecare.app`
  ——先由 PicoHRT 层从 `com.mkx.hrttracker` 改成 `com.picohrt`，合并时再改一次。
  `versionCode` 从 `gitCommitCount` 派生，读出来是累计 commit 数；`versionName`
  是 `2.0.0`。
- `settings.gradle.kts`：`rootProject.name` 设为 `DoseCare`。根项目名同时决定
  APK 文件名，所以产物是 `dosecare-<abi>-<versionName>-<versionCode>.apk`。
- `app/src/main/res/values/strings.xml`（含所有 locale 变体）：`app_name` 设为
  `DoseCare`。上游 `privacy_policy_url` 保留，在 [privacy.md](privacy.md) 加注释
  说明关系；托管 DoseCare 隐私政策发布前，仓内文档是技术参考。
- `app/build.gradle.kts`（`THIRD_PARTY_NOTICES_URL`）：运行 app 深链到
  third-party-notices 的 URL，现在指向本仓的当前 commit，不再指上游。
- `app/src/main/java/com/dosecare/app/data/backup/BackupExportService.kt`：
  默认备份文件名 `featherline-backup` → `dosecare-backup`。
- `app/src/main/java/com/dosecare/app/util/AppDiagnosticsExportService.kt`：
  诊断导出文件名 `dosecare-diagnostics-<timestamp>.txt`，header
  `DoseCare diagnostic logs`。
- `app/src/main/java/com/dosecare/app/ui/settings/SettingsScreen.kt`：
  `APP_REPOSITORY_URL` 指向 `https://github.com/sakanana001/dosecare`。
- `LICENSE`：AGPL-3.0，理由见[谱系](#谱系)。

## 文件图（rename 历史，汇总）

- `app/src/main/java/com/mkx/hrttracker/**/*.kt` 全部 →
  `app/src/main/java/com/dosecare/app/**/*.kt`（rename 跑过两轮——先由 PicoHRT 层
  改成 `com/picohrt`，再由合并改成 `com/dosecare/app`）。药理模块从独立 App 合入时
  本来就在 `com.dosecare.app.*` 命名空间下（`model/pharmacology`、
  `ui/interaction`、`ui/tdm`、`ui/compare`），这也是合并时的 rename 正好落在合并
  代码已经在用的目标命名空间上的原因
- `app/src/test/java/**` 和 `app/src/androidTest/java/**` 同样跟着包名移动
- `benchmark/src/main/java/com/mkx/hrttracker/macrobenchmark/**` →
  `benchmark/src/main/java/com/dosecare/app/macrobenchmark/**`
- `app/src/main/AndroidManifest.xml`：类名保持相对写法
  （`.HrtTrackerApplication`），解析到新 namespace
- `app/src/main/res/xml/hrt_widget_*.xml`：`android:configure` 等包名引用改写
- `app/src/main/res/values*/strings.xml`（5 个 locale）：`app_name` 和 onboarding
  文字 rebrand
- `app/src/main/assets/pharmacology/catalog.json`：202 药目录，合并时加入
- `fastlane/metadata/android/{en-US,zh-CN}/*`：title 和 description 为合并后的 App 重写
- `docs/PROJECT.md`, `docs/PROJECT.zh-Hans.md`：本谱系文档
- `tools/_rebrand_strings.py`, `tools/_rebrand_strings_v2.py`：
  用来跑批量 string-rename 的工具脚本。留在仓里，让 rename 历史从干净 checkout
  可复现。

## 致谢

- [mkx173/Featherline](https://github.com/mkx173/Featherline) — 上游项目。
  `v0.6.0` 以下的每一行代码、每个 UI 决策、每个测试都是上游工作。
- [LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test) — PK 估算的数学参考。
- [TransmtfTeam/Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker) — 雌二醇十一酸酯 PK 参数。
- [SmirnovaOyama/Oyama-s-HRT-Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker) — 图表渲染逻辑。
- AGNP 指南作者、Flockhart CYP 表维护者，以及药品目录所依据的 DailyMed / OpenFDA 说明书语料。
- 跨性别健康社区的测试、反馈，以及让这类工具成为可能的前置工作。
