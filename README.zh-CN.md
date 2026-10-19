<p align="center">
  <img src=".github/app-icon.png" alt="DoseCare 图标" width="120" />
</p>

# DoseCare

**简体中文** · [English](README.md)

> **DoseCare 仓库在 [sakanana001/dosecare](https://github.com/sakanana001/dosecare)。它由两条上游工作线合成：[PicoHRT](https://github.com/sakanana001/PicoHRT)（[mkx173/Featherline](https://github.com/mkx173/Featherline) 的分支，HRT 用药追踪底座），以及原独立的 [DoseCare](https://github.com/sakanana001/dosecare) 药理 App（202 药目录、相互作用规则、血药浓度估算），后者被并入了这个底座。**
> 原始应用、设计以及 `v0.6.0` 之前的全部工程归上游 Featherline 维护者所有。PicoHRT
> 这一层做了品牌重命名、为 testosterone undecanoate 重做的三室 PK 引擎、gel 皮肤
> 储库，以及 `kClearInjection` / k31 的 per-user override 数据层。DoseCare 这一层
> 带来了药理能力。完整谱系与理由见 [docs/PROJECT.zh-Hans.md](docs/PROJECT.zh-Hans.md)，
> 合并后的版本历史见 [docs/CHANGELOG.zh-Hans.md](docs/CHANGELOG.zh-Hans.md)。

面向 Android 的**多药**用药记录工具，主打激素治疗追踪 + 药理学检查：剂量打卡、PK 估算、血检追踪、202 药理目录、相互作用检测、血药浓度估算。既服务于跨性别女性（MtF）、跨性别男性（FtM）和非二元用户的激素治疗，也服务于同时管理好几种精神科或慢病用药的人。本地加密存储，无需账号。

[![License: AGPL v3](https://img.shields.io/badge/License-AGPLv3-blue.svg)](LICENSE)
[![Build](https://github.com/sakanana001/dosecare/actions/workflows/android-release.yml/badge.svg)](https://github.com/sakanana001/dosecare/actions/workflows/android-release.yml)
![minSdk](https://img.shields.io/badge/minSdk-26-blue.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-37-blue.svg)
[![Fork of Featherline](https://img.shields.io/badge/fork%20of-mkx173%2FFeatherline-1f6feb)](https://github.com/mkx173/Featherline)

DoseCare 覆盖注射、贴片、凝胶、口服、舌下等给药途径；按你的用药历史通过三室 PK 模型估算雌二醇水平；并以自动单位换算追踪血检结果（pg/mL ↔ pmol/L、ng/dL ↔ nmol/L）。药理这一侧内置 202 条药品目录，带 CYP450 谱、QTc 风险、不良反应标记和治疗窗；用本地规则引擎检查你当前方案里的相互作用；估算血药浓度；任意两药并排对比。所有数据保存在本地加密数据库里——不注册账号、不上报、不联网。备份加密压缩。提供英文、简体中文、繁体中文、繁体中文（香港，广东话）和日语（翻译进行中）。

> ⚠️ **不构成医疗建议。** DoseCare 是记录工具，不是医疗设备，使用它也不构成医患关系。PK 估算和血药浓度估算都只是基于你记录的剂量的粗略人群均值估计，不能替代血检或临床医生的判断，请勿据此自行调药。相互作用检测是跑在公开参考数据上的规则引擎，不是药师。完整免责声明见 [docs/safety.md](docs/safety.md)。

## 获取应用

- **GitHub Releases**（签名 APK，自行 sideload）：[releases 页面](https://github.com/sakanana001/dosecare/releases)
- 或从源码构建：见 [docs/building.md](docs/building.md)

> 上游 Featherline 在 Play Store 和 F-Droid 上的包名是 `com.mkx.hrttracker`；
> DoseCare 使用独立包名 `com.dosecare.app`，不会从上游版本自动更新。请自行
> sideload 最新 release APK 或从源码构建。

## 功能

### 激素治疗追踪

- 覆盖注射、贴片、凝胶、口服、舌下等给药途径的用药记录
- 每条手动用药记录可附 2-6 行**自由备注** —— 副作用、心情、饮食、注射部位，或日后翻看历史时想得起来的任何事；空白备注存为 `NULL`
- **激素模式（MtF / FtM / Non-binary）**——在 设置 → 个性化 → HRT goal 选目标；主屏默认曲线、可用药物、安全提示随之切换（见 [激素模式](#激素模式mtf--ftm--non-binary) 段）
- **MtF**——雌二醇贴片、凝胶、口服、舌下；可选 cyproterone acetate（CPA）抗雄
- **FtM**——testosterone enanthate（TE）、testosterone cypionate（TC）、testosterone undecanoate（TU，Nebido）、testosterone gel 一线 medication key；CPA 完全禁用
- 可配置提醒计划，支持 snooze 和精确闹钟权限
- 药物分组，并把计划应用到组内剂量
- 可选药物库存跟踪，含低库存提醒和按计划推算的"剩余天数"
- 按用药历史估算雌二醇浓度
- **testosterone undecanoate 的三室 PK 引擎**（Runge-Kutta 4；深部外周匹配 Nebido SmPC 给出的 90 ± 40 天终末半衰期）
- **testosterone gel 的皮肤储库**（透皮吸收滞后，不是 2 室肌肉储库）
- **`kClearInjection` 和三室 `k31` 的 per-user override**（home chart "3-comp" 徽章菜单可调；面向 FtM 用户，血检跟人群均值有偏差时可以微调）
- 血检目录与自动单位换算（pg/mL ↔ pmol/L、ng/dL ↔ nmol/L）
- Journal 页签：里程碑时间线 + 每日备注，可把任意日期 pin 到主屏作为 anchor widget 或快捷方式，显示累计天数
- 主屏快速记录 widget，两种尺寸，含进度、下次剂量、点击记录
- 加密压缩备份格式，含恢复校验
- 应用锁 + 生物识别解锁
- Material 3 + dynamic color

### 药理与多药管理

- **202 条药理目录，离线内置** —— 31 个药理学分类，覆盖抗精神病药、心境稳定剂、抗抑郁药、抗焦虑药，以及随之而来的慢病用药类别（降糖药、降压药、调脂药、抗癫痫药、PPI 等）。每条都带 CYP450 谱（底物 / 抑制剂 / 诱导剂）、QTc 延长风险等级、不良反应标记（含抗胆碱负荷）、适应症分组、过量信息与监测要求；有公开治疗窗的药还带治疗窗本身
- **药物相互作用检测** —— 本地规则引擎，跑在你实际在吃的药上：CYP450 抑制与诱导、QTc 延长叠加、5-HT 综合征风险、抗胆碱负荷，同一轮里还会查治疗窗偏离和肾/肝剂量调整。严重度分 `LOW` / `MEDIUM` / `HIGH` / `CONTRAINDICATED`。剂量和频次取自你自己的方案，暴露倍数按你实际吃的量算，不是目录里的默认值。入口：设置 → Interactions
- **血药浓度估算（TDM）** —— 一房室口服 PK 模型按剂量、频次和体重估算浓度曲线，并叠加该药的治疗窗，一眼看出估算值是否落在窗内。多个药可以画在同一张图上，各有各的颜色和治疗窗色带。入口：设置 → Blood level monitoring
- **双药规格对比** —— 任选两条目录条目并排看，共 8 个 section：基本身份、PK 参数、治疗窗、CYP 角色、关键不良反应、剂量调整、监测要求、关键相互作用。还有一个"白话模式"把同样的对比去掉术语重讲一遍。入口：设置 → Two-drug spec comparison
- **药品详情页可绑定药理档案** —— 把方案里的某个药绑到目录条目上，详情页就会多出它的药理档案：适应症分组、CYP 角色、PK 估算、过量信息、监测要求。绑定按药存储，也可以随时解绑

### 两边都有

- 无账号、无遥测、无网络调用——所有数据留在本地
- 英文、简体中文、繁体中文、繁体中文（香港，广东话）、日语——日语翻译还在补

## 截图

<table>
  <tr>
    <td width="25%"><img src=".github/screenshots/home.png" alt="主屏"></td>
    <td width="25%"><img src=".github/screenshots/plan.png" alt="计划视图"></td>
    <td width="25%"><img src=".github/screenshots/history.png" alt="历史日志"></td>
    <td width="25%"><img src=".github/screenshots/calibration.png" alt="校准屏"></td>
  </tr>
  <tr>
    <td align="center">主屏</td>
    <td align="center">计划</td>
    <td align="center">历史</td>
    <td align="center">校准</td>
  </tr>
</table>

## 药理工具箱

药理这一半是一个自包含、离线的参考工具。目录随 APK 一起打包，不需要联网拉取；
它给出的每个数字都能追到公开出处。

### 目录

`app/src/main/assets/pharmacology/catalog.json` 里有 202 条药品、31 个药理学分类，每条带：

- **PK 模型**（一房室口服）：`ke`、`Vd`、生物利用度、半衰期、蛋白结合率
- **CYP450 谱**：是哪些酶的底物，以及它抑制或诱导哪些酶
- **风险画像**：QTc 延长、代谢综合征、粒细胞缺乏、锥体外系、嗜睡、性功能影响、高催乳素血症，以及 0-3 的抗胆碱负荷
- **治疗窗**（有公开数据时），并带单位换算因子，好跟模型浓度对齐比较
- **肾/肝剂量调整**、老年人提示、吸烟影响
- **过量信息**：典型症状、严重度、70 kg 成人的估计中毒/致死剂量、抢救要点、特异性解毒剂
- **监测要求**和**关键相互作用**，都指回各自的出处

目录由公开参考材料汇编而成——DrugBank、OpenFDA / DailyMed、AGNP 2017 共识、
Flockhart CYP 药物相互作用表（Indiana University）、治疗药物监测指南，以及
国内药品说明书。完整出处见 [docs/third-party-notices.md](docs/third-party-notices.md)。

### 规则引擎

相互作用引擎是自研的，完全在端侧跑。每条规则都是"药品列表 + 患者画像"上的纯函数，
所以单条规则可以独立做单元测试，引擎本身不存状态。规则覆盖：

| 规则 | 看什么 |
|---|---|
| CYP450 相互作用 | 某个药对另一个药主代谢通路的抑制或诱导作用，按剂量和频次缩放 |
| QTc 叠加 | 方案里所有药的 QTc 效应求和，再加一个性别基线 |
| 抗胆碱负荷 | 方案里 0-3 抗胆碱分求和 |
| 5-HT 综合征 | 血清素能药物叠了几层，其中有没有强效药 |
| 治疗窗偏离 | 建模浓度是否落在该药的公开治疗窗之外 |
| 肾/肝剂量调整 | 方案是否需要按肾功能或肝功能减量或监测 |
| 镇静叠加、出血风险、多巴胺拮抗、低血压 | 代谢类规则覆盖不到的叠加药理作用 |

这些规则是人群层面的启发式判断，有记录在案的假阳性/假阴性行为。它们不替代
临床医生的判断，也不替代治疗药物监测方案。

### 三个页面怎么用

- **Interactions**（设置 → Interactions）——选药，或者一键导入你自己的方案，然后拿回一份按严重度排序的清单
- **Blood level monitoring**（设置 → Blood level monitoring）——填剂量、频次、体重，把估算浓度和治疗窗对着看
- **Two-drug spec comparison**（设置 → Two-drug spec comparison）——两药并排，可选白话层

## 激素模式（MtF / FtM / Non-binary）

DoseCare 通过一个 `HrtGoal` 枚举（`UNSET` / `MTF` / `FTM` / `NON_BINARY`）
覆盖跨性别 HRT 的两端用户。选定的目标同时控制三件事：

1. **主屏默认曲线**——启动 app 时默认打开的走势图。
2. **可用药物**——目录和计划流程里显示的 medication key。
3. **安全提示**——围绕剂量和血检显示的"注意这个"消息。

设置/修改入口：**Settings → Personalization → HRT goal**。默认是
`UNSET`，保留 v0.6 之前的行为（E2 曲线、全部 chip 可见、无
目标相关提示），老用户升级不感知。

| 模式 | 主屏默认曲线 | 可用药物 | 安全提示 | 备注 |
|---|---|---|---|---|
| **MtF**（跨性别女性） | E2（雌二醇） | E2 贴片、凝胶、口服、舌下；cyproterone acetate（CPA）抗雄 | CPA 相互作用警告；E2 各途径 Css 锚点 | 上游 Featherline 默认行为；本项目保留。 |
| **FtM**（跨性别男性） | T（睾酮） | TE（testosterone enanthate）、TC（testosterone cypionate）、TU（testosterone undecanoate，Nebido）、T gel；**CPA 完全禁用** | TU 90 ± 40 d 终末半衰期警告；T gel Css 锚点 | v3 三室引擎、gel 皮肤储库、per-user kClear / k31 override 都为此模式设计。 |
| **Non-binary** | 3-way 激素自选（E2 / T / 双） | 全部 | 无目标相关提示 | 让用户自组方案，不被锁进二元默认。 |
| **Unset**（默认） | E2（v0.6 之前遗留） | 全部 | 无目标相关提示 | 向后兼容的默认；v0.6 迁移后首次启动会提示用户选目标。 |

### FtM medication key

DoseCare 把 4 个睾酮剂型从上游 Featherline 的"custom-only"提升到
一线 medication key。catalog entry 预加载，新用户切到 FtM 直接看到
全套，不用在 `custom` 槽里手填。

| `MedicationKey` | `PkCompound` | 途径 | 半衰期（文献） | 备注 |
|---|---|---|---|---|
| `TESTOSTERONE_ENANTHATE` | `TE` | IM（200 mg/mL） | t½ ≈ 4.5 d | 短效酯；每周或两周一次 |
| `TESTOSTERONE_CYPIONATE` | `TC` | IM（200 mg/mL） | t½ ≈ 8 d | 略长于 TE；每周或两周一次 |
| `TESTOSTERONE_UNDECANOATE` | `TU` | IM（1000 mg/4 mL，Nebido） | t½ ≈ 21 d 吸收 + **90 ± 40 d** 终末 | 用 v3 三室 RK4 引擎，深部外周储库 |
| `TESTOSTERONE_GEL` | `T`（AndroGel 动力学） | 透皮（5 / 10 g/day） | 稳态 Css 锚点 | 用 PR2 双室皮肤储库（MM + kCl），不是 2 室肌肉储库 |

完整 FtM 设计理由、锚点数据、PR 拆分（F-PR1 goal 枚举 → F-PR2
一线 key → F-PR3 chart 默认 → F-PR4 安全提示）见
[docs/ftm-hrt.md](docs/ftm-hrt.md)。PR 总结在
[docs/ftm-pr-summary.md](docs/ftm-pr-summary.md)。

### 这里"model"是什么意思

"激素模式"是 app 在三处读取的单一开关：主屏曲线的默认选择、
药物目录的 `PkCompound` 过滤、安全提示注册列表。没有 per-user
"我要不要这个安全提示"开关——选定目标，对应的提示就开。设计上是
有意的：每位用户单一一致的模式，不是一堆独立 toggle。

## 工作原理

DoseCare 是单模块 Android 应用，Kotlin + Jetpack Compose。剂量、提醒、化验结果存储在 SQLCipher 加密的 Room 数据库里。PK 引擎采用三室模型，把每次记录的剂量转换为随时间变化的雌二醇贡献，再把所有给药途径的贡献加总得到估算曲线。testosterone undecanoate 路径使用独立的三室 RK4 求解器，让深部外周储库匹配 90 ± 40 天的长终末半衰期；gel 路径使用透皮皮肤储库滞后，而不是 2 室肌肉储库。提醒基于 AlarmManager，支持精确闹钟权限和 snooze；通知在重启和时区/时间变更后通过 reconciliation 层恢复。血检目录通过规范因子表实现双向单位换算。备份加密压缩，使用带恢复校验的版本化格式。

药理这一半跟上面是并行的：药品目录在启动时从内置 asset 一次性加载进内存服务，
规则引擎在评估前先把当前方案解析成每个药的剂量和频次，血药浓度估算则对选中的
药各跑一遍目录里的一房室模型。这条链路同样不碰网络。

完整架构、数据模型、提醒流水线见 [docs/architecture.md](docs/architecture.md)。三室引擎、gel 储库、per-user override 数据层见 [docs/PROJECT.zh-Hans.md](docs/PROJECT.zh-Hans.md) → "v0.6 之后的 PK 引擎改动"。

## 谱系

| | 上游 (Featherline) | 中间层 (PicoHRT) | 本项目 (DoseCare) |
|---|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` | `com.dosecare.app` |
| 仓库 | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) |
| 应用名 | Featherline | PicoHRT | DoseCare |
| 许可证 | GPL-3.0 | GPL-3.0 | AGPL-3.0 |
| 隐私政策 | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | 暂用仓内 [docs/privacy.md](docs/privacy.md) | 暂用仓内 [docs/privacy.md](docs/privacy.md)，托管政策待发布 |
| TU 三室 PK 引擎 | 无 | PicoHRT 层新增 | 沿用 |
| Gel 皮肤储库 | 无 | PicoHRT 层新增 | 沿用 |
| Per-user kClear / k31 override | 无 | PicoHRT 层新增 | 沿用 |
| 202 药理目录 | 无 | 无 | DoseCare 层新增 |
| CYP450 / QTc 相互作用规则 | 无 | 无 | DoseCare 层新增 |
| 血药浓度 (TDM) 估算 | 无 | 无 | DoseCare 层新增 |
| 双药规格对比 | 无 | 无 | DoseCare 层新增 |

PicoHRT 仓的首条 fork commit 即上游 `v0.6.0`。[docs/CHANGELOG.zh-Hans.md](docs/CHANGELOG.zh-Hans.md)
里 `## [Unreleased]` 段落是新增工作；`## [0.6.0]` 及之前为上游历史，原文保留。
AGPL-3.0 是 GPL-3.0 的超集，所以从 PicoHRT 层的 GPL-3.0 到本项目的 AGPL-3.0
是单向上升；两条上游线都是 GPL 系，与 AGPL-3.0 兼容。

## 路线图

- 把 PK 引擎替换为更通用的多药模型
- 基于个人化验结果做个人化 PK 校准
- 可选加密云备份（默认关闭，端到端加密）
- 补完日语翻译——欢迎到上游 [Hosted Weblate](https://hosted.weblate.org/projects/featherline/) 贡献翻译（见 [docs/localization.md](docs/localization.md)）
- 药理目录条目和相互作用规则在超出个人使用范围的发行前做临床复核

## 技术栈

Kotlin、Jetpack Compose + Material 3、Hilt 依赖注入、Room + SQLCipher 加密持久化、Coroutines + Flow。版本号见 [gradle/libs.versions.toml](gradle/libs.versions.toml)。

## 从源码构建

前置：JDK 17、近期版 Android Studio、API level 匹配 `compileSdk` 的 Android SDK。Android Studio 标准导入；运行 `./gradlew assembleDebug` 或用 IDE。

详细说明、flavor、CI 行为：见 [docs/building.md](docs/building.md)。

## 贡献

欢迎贡献。先读 [Code of Conduct](CODE_OF_CONDUCT.md)，再看 [CONTRIBUTING.md](CONTRIBUTING.md) 的开发工作流、分支约定和提案流程。

## 隐私

DoseCare 把所有数据存在你设备的加密数据库里。无账号、无遥测、无网络调用。药理目录作为静态资源随 APK 打包，也不依赖任何后端。完整数据处理说明见 [docs/privacy.md](docs/privacy.md)。仓内文档面向贡献者，作为技术参考；DoseCare 托管隐私政策发布前，参照上游 [Featherline 隐私政策](https://asterismlabs.io/featherline/privacy/)。

## 许可

DoseCare 以 GNU Affero 通用公共许可证第 3 版发布。完整文本见 [LICENSE](LICENSE)。
第三方依赖、资产、衍生代码与数据来源声明见 [docs/third-party-notices.md](docs/third-party-notices.md)。

## 致谢

- [Material Symbols](https://fonts.google.com/icons) 图标集。
- PK 估算参考了 [HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test) 的数学参考。
- 雌二醇十一酸酯 PK 参数来自 [Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker)。
- 图表渲染逻辑改编自 [Oyama's HRT Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker)。
- 上游 [Featherline](https://github.com/mkx173/Featherline) 项目——本 app 的一切起点。
- AGNP 指南作者、Flockhart CYP 表维护者，以及药品目录所依据的 DailyMed / OpenFDA 说明书语料。
- 跨性别健康社区的测试、反馈、以及让这类工具成为可能的前置工作。
