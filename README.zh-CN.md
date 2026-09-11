<p align="center">
  <img src=".github/app-icon.png" alt="PicoHRT 图标" width="120" />
</p>

# PicoHRT

**简体中文** · [English](README.md)

> **PicoHRT 是 [mkx173/Featherline](https://github.com/mkx173/Featherline) 的二次开发分支，仓库现位于 [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT)。**
> 原始应用、设计以及 `v0.6.0` 之前的全部工程归上游维护者所有。PicoHRT
> 从 `v0.6.0` 继续，主要改动：品牌重命名、为 testosterone undecanoate 重做的
> 三室 PK 引擎、gel 皮肤储库，以及 `kClearInjection` / k31 的 per-user override
> 数据层。完整 fork 说明见 [docs/PROJECT.zh-Hans.md](docs/PROJECT.zh-Hans.md)，
> 合并后的版本历史见 [docs/CHANGELOG.zh-Hans.md](docs/CHANGELOG.zh-Hans.md)。

面向 Android 的 HRT 用药记录工具，附带 PK 估算和血检追踪——支持跨性别女性（MtF）、跨性别男性（FtM）和非二元用户。本地加密存储，无需账号。

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Build](https://github.com/sakanana001/PicoHRT/actions/workflows/android-release.yml/badge.svg)](https://github.com/sakanana001/PicoHRT/actions/workflows/android-release.yml)
![minSdk](https://img.shields.io/badge/minSdk-26-blue.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-37-blue.svg)
[![Fork of Featherline](https://img.shields.io/badge/fork%20of-mkx173%2FFeatherline-1f6feb)](https://github.com/mkx173/Featherline)

PicoHRT 覆盖注射、贴片、凝胶、口服、舌下等给药途径；按你的用药历史通过三室 PK 模型估算雌二醇水平；并以自动单位换算追踪血检结果（pg/mL ↔ pmol/L、ng/dL ↔ nmol/L）。所有数据保存在本地加密数据库里——不注册账号、不上报、不联网。备份加密压缩。提供英文和简体中文。

> ⚠️ **不构成医疗建议。** PicoHRT 是记录工具，不是医疗设备，使用它也不构成医患关系。PK 估算只是基于你记录的剂量的粗略人群均值估计，不能替代血检或临床医生的判断，请勿据此自行调药。完整免责声明见 [docs/safety.md](docs/safety.md)。

## 获取应用

- **GitHub Releases**（签名 APK，自行 sideload）：[releases 页面](https://github.com/sakanana001/PicoHRT/releases)
- 或从源码构建：见 [docs/building.md](docs/building.md)

> 上游 Featherline 在 Play Store 和 F-Droid 上的包名是 `com.mkx.hrttracker`；
> PicoHRT 使用独立包名 `com.picohrt`，不会从上游版本自动更新。请自行
> sideload 最新 release APK 或从源码构建。

## 功能

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
- 加密压缩备份格式，含恢复校验
- 应用锁 + 生物识别解锁
- 主屏快速记录 widget，两种尺寸，含进度、下次剂量、点击记录
- Journal 页签：里程碑时间线 + 每日备注，可把任意日期 pin 到主屏作为 anchor widget 或快捷方式，显示累计天数
- 无账号、无遥测、无网络调用——所有数据留在本地
- 英文、简体中文、繁体中文、繁体中文（香港，广东话）
- Material 3 + dynamic color

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

## 激素模式（MtF / FtM / Non-binary）

PicoHRT 通过一个 `HrtGoal` 枚举（`UNSET` / `MTF` / `FTM` / `NON_BINARY`）
覆盖跨性别 HRT 的两端用户。选定的目标同时控制三件事：

1. **主屏默认曲线**——启动 app 时默认打开的走势图。
2. **可用药物**——目录和计划流程里显示的 medication key。
3. **安全提示**——围绕剂量和血检显示的"注意这个"消息。

设置/修改入口：**Settings → Personalization → HRT goal**。默认是
`UNSET`，保留 v0.6 之前的行为（E2 曲线、全部 chip 可见、无
目标相关提示），老用户升级不感知。

| 模式 | 主屏默认曲线 | 可用药物 | 安全提示 | 备注 |
|---|---|---|---|---|
| **MtF**（跨性别女性） | E2（雌二醇） | E2 贴片、凝胶、口服、舌下；cyproterone acetate（CPA）抗雄 | CPA 相互作用警告；E2 各途径 Css 锚点 | 上游 Featherline 默认行为；本 fork 保留。 |
| **FtM**（跨性别男性） | T（睾酮） | TE（testosterone enanthate）、TC（testosterone cypionate）、TU（testosterone undecanoate，Nebido）、T gel；**CPA 完全禁用** | TU 90 ± 40 d 终末半衰期警告；T gel Css 锚点 | v3 三室引擎、gel 皮肤储库、per-user kClear / k31 override 都为此模式设计。 |
| **Non-binary** | 3-way 激素自选（E2 / T / 双） | 全部 | 无目标相关提示 | 让用户自组方案，不被锁进二元默认。 |
| **Unset**（默认） | E2（v0.6 之前遗留） | 全部 | 无目标相关提示 | 向后兼容的默认；v0.6 迁移后首次启动会提示用户选目标。 |

### FtM medication key

本 fork 把 4 个睾酮剂型从上游 Featherline 的"custom-only"提升到
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

PicoHRT 是单模块 Android 应用，Kotlin + Jetpack Compose。剂量、提醒、化验结果存储在 SQLCipher 加密的 Room 数据库里。PK 引擎采用三室模型，把每次记录的剂量转换为随时间变化的雌二醇贡献，再把所有给药途径的贡献加总得到估算曲线。testosterone undecanoate 路径使用独立的三室 RK4 求解器，让深部外周储库匹配 90 ± 40 天的长终末半衰期；gel 路径使用透皮皮肤储库滞后，而不是 2 室肌肉储库。提醒基于 AlarmManager，支持精确闹钟权限和 snooze；通知在重启和时区/时间变更后通过 reconciliation 层恢复。血检目录通过规范因子表实现双向单位换算。备份加密压缩，使用带恢复校验的版本化格式。

完整架构、数据模型、提醒流水线见 [docs/architecture.md](docs/architecture.md)。三室引擎、gel 储库、per-user override 数据层见 [docs/PROJECT.zh-Hans.md](docs/PROJECT.zh-Hans.md) → "v0.6 之后的 PK 引擎改动"。

## Fork 关系

| | 上游 (Featherline) | 本 fork (PicoHRT) |
|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` |
| 仓库 | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) |
| 应用名 | Featherline | PicoHRT |
| 隐私政策 | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | 暂用仓内 [docs/privacy.md](docs/privacy.md)，托管政策待发布 |
| 三室 PK 引擎 | 无 | 本 fork 新增 |
| Gel 皮肤储库 | 无 | 本 fork 新增 |
| Per-user kClear / k31 override | 无 | 本 fork 新增 |

本仓首条 fork commit 即上游 `v0.6.0`。[docs/CHANGELOG.zh-Hans.md](docs/CHANGELOG.zh-Hans.md)
里 `## [Unreleased]` 段落为本 fork 新增；`## [0.6.0]` 及之前为上游历史，原文保留。

## 路线图

- 把 PK 引擎替换为更通用的多药模型
- 基于个人化验结果做个人化 PK 校准
- 可选加密云备份（默认关闭，端到端加密）
- 更多语言——欢迎到上游 [Hosted Weblate](https://hosted.weblate.org/projects/featherline/) 贡献翻译（见 [docs/localization.md](docs/localization.md)），等 PicoHRT 项目注册后再迁

## 技术栈

Kotlin、Jetpack Compose + Material 3、Hilt 依赖注入、Room + SQLCipher 加密持久化、Coroutines + Flow。版本号见 [gradle/libs.versions.toml](gradle/libs.versions.toml)。

## 从源码构建

前置：JDK 17、近期版 Android Studio、API level 匹配 `compileSdk` 的 Android SDK。Android Studio 标准导入；运行 `./gradlew assembleDebug` 或用 IDE。

详细说明、flavor、CI 行为：见 [docs/building.md](docs/building.md)。

## 贡献

欢迎贡献。先读 [Code of Conduct](CODE_OF_CONDUCT.md)，再看 [CONTRIBUTING.md](CONTRIBUTING.md) 的开发工作流、分支约定和提案流程。

## 隐私

PicoHRT 把所有数据存在你设备的加密数据库里。无账号、无遥测、无网络调用。完整数据处理说明见 [docs/privacy.md](docs/privacy.md)。仓内文档面向贡献者，作为技术参考；PicoHRT 托管隐私政策发布前，参照上游 [Featherline 隐私政策](https://asterismlabs.io/featherline/privacy/)。

## 许可

PicoHRT 以 GNU 通用公共许可证第 3 版发布。完整文本见 [LICENSE](LICENSE)。
第三方依赖、资产、衍生代码声明见 [docs/third-party-notices.md](docs/third-party-notices.md)。

## 致谢

- [Material Symbols](https://fonts.google.com/icons) 图标集。
- PK 估算参考了 [HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test) 的数学参考。
- 雌二醇十一酸酯 PK 参数来自 [Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker)。
- 图表渲染逻辑改编自 [Oyama's HRT Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker)。
- 上游 [Featherline](https://github.com/mkx173/Featherline) 项目——PicoHRT 的一切起点。
- 跨性别健康社区的测试、反馈、以及让这类工具成为可能的前置工作。
