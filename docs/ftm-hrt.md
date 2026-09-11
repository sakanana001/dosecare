# FtM 端 (Transmasculine HRT) 支持文档

> 范围：FtM 化（F-PR1~F-PR5）— HRT goal 枚举、睾酮 4 种剂型、模式开关、安全提示
> 提交区间：1ac4b78..HEAD（19 commits）
> 适用版本：PicoHRT v0.6+

## 1. 概览

PicoHRT 原本只服务 MtF 端（HRT 减害工具），主屏默认雌激素曲线。FtM 化在 v0.6
引入 **HRT goal** 概念，让用户（MtF / FtM / Non-binary / 未选）切换 app 的行为：

- **MtF** — 默认 E2 曲线，可选 CPA 抗雄
- **FtM** — 默认 T 曲线，隐藏 E2 / CPA chip；CPA 在 FtM 下完全禁用
- **Non-binary / 未选** — 3-way 激素选择，无 goal 相关的安全提示

切换点：Settings → Personalization → HRT goal。

## 2. HRT goal 枚举（F-PR1）

```kotlin
enum class HrtGoal { UNSET, MTF, FTM, NON_BINARY }
```

- **UNSET**：默认（保留 v0.5 行为 — E2 趋势 + 全 chip）
- **MTF**：MtF（transfeminine）端
- **FTM**：FtM（transmasculine）端
- **NON_BINARY**：非二元 / 自定义

数据存储：DataStore key `hrt_goal`（string preferences key，值 = enum name）。
UNSET 不写盘（key 不存在 → 重新落到 SettingsState 默认值），重置清空。

设置入口：Settings → Personalization → HRT goal 卡片（新加）。

## 3. 睾酮 4 种剂型（F-PR2）

F-PR2 把 4 个睾酮剂型从"custom-only"提升到 first-class MedicationKey：

| MedicationKey | PkCompound | 推荐剂型 | 半衰期 (依文献) | catalog entry |
|---|---|---|---|---|
| `TESTOSTERONE_ENANTHATE` | `TE` | 200 mg/mL IM | t½ ≈ 4.5 d | InjectionSingleUseVial 100/200 mg；MultiUseVial 100/200 mg/mL |
| `TESTOSTERONE_CYPIONATE` | `TC` | 200 mg/mL IM | t½ ≈ 8 d | 同上 |
| `TESTOSTERONE_UNDECANOATE` | `TU` | 1000 mg/4 mL IM (Nebido) | t½ ≈ 21 d | InjectionSingleUseVial 1000 mg；MultiUseVial 250 mg/mL × 4 mL |
| `TESTOSTERONE_GEL` | `T` (和rogel kinetics) | 5/10 g/day transdermal | Css 5.66 / 7.92 ng/mL | gelDoseAssistPresets (0.06-0.6% / 0.5-2.5 g) |

**临床校准 anchor**（PR3 retune + PR3-UI polish）：

| 剂型 / 剂量 | 文献 Cmax | 模型 Cmax (T+48h) | 误差 |
|---|---|---|---|
| TE 200 mg IM | ~1080 ng/dL @ 60h (Behre 1986 250 mg 缩放) | 1452 ng/dL @ 48h | +34% |
| TC 200 mg IM | 758 ± 289 ng/dL @ 71.7h (Slayback FDA) | 1076 ng/dL @ 48h | +42% |
| TU 1000 mg IM | 1100-1500 ng/mL @ 7d (Nebido SmPC) | 1981 ng/dL @ 120h | +32% |
| T gel 5 g/day | 5.66 ± 2.62 ng/mL Day 30 peak | 3.5 ng/mL | -38% |
| T gel 10 g/day | 7.92 ± 2.94 ng/mL Day 30 peak | 7.0 ng/mL | -12% |

**PkCompound routing**（F-PR2 新增）：
- `toTestosteronePkDoseEvent` 走两层 narrow：
  1. `category == TESTOSTERONE`（主过滤）
  2. `medicine.toTestosteronePkCompound()` → catalog key 映射 TE/TC/TU/T
- `IMPORTED_INJECTION_ESTER_KEYS` 集合扩到 T esters（ImportedInjection 路径）
- catalog selection 路径（新加）：`MedicineSelection.Catalog.medicationKey` → 走同样的映射

`PkCompound.T` 仍作为兜底（custom T 注射剂无 catalog key 的情况）。

## 4. 模式开关与默认值（F-PR3）

### 4.1 chart hormone per goal

| HrtGoal | chartHormone default | available chart hormones |
|---|---|---|
| MTF | ESTRADIOL | ESTRADIOL, CYPROTERONE_ACETATE |
| FTM | TESTOSTERONE | TESTOSTERONE |
| NON_BINARY | ESTRADIOL | ESTRADIOL, TESTOSTERONE, CYPROTERONE_ACETATE |
| UNSET | ESTRADIOL | ESTRADIOL, TESTOSTERONE, CYPROTERONE_ACETATE |

切换 goal 时，auto-picker 会：
1. 更新 `_currentHrtGoal: StateFlow<HrtGoal>`（用于 chip filter / 卡片过滤）
2. 如果当前 `selectedChartHormone` 不在新 goal 的 available set 里，**回退到 goal 默认**
3. chip 仍允许用户 per-session 覆盖

### 4.2 ANTIANDROGEN 卡片过滤（FTM）

FTM 用户的主屏 ANTIANDROGEN 卡片**自动隐藏**所有只含 CPA 的 group：

- spironolactone / bicalutamide / finasteride / dutasteride 仍显示
- CYPROTERONE_ACETATE 单一 group 隐藏（CPA 在 FtM 下是临床禁忌）

实现：`MainViewModel.buildHomeUiState` 检测 `currentHrtGoal == FTM` 时，过滤 `inputs.activeGroups` 中所有 medication 都是 CPA 的 group。

### 4.3 3-way chip selector

主屏 chart card header 上的 chip row 按 `availableChartHormones` 过滤：
- FTM 用户只看到 `T` 一个 chip
- MTF 用户看到 `E2` 和 `CPA` 两个 chip
- UNSET / NON_BINARY 用户看到全部 3 个

实现：`MainChartHormoneChipRow` 接收 `availableHormones: Set<PkHormone>`，缺则不渲染 chip。

## 5. 安全提示文案（F-PR4）

F-PR4 提供 `(goal, medication)` 临床安全提示矩阵 + MedicalDisclaimerKind 扩展，**调用方**在 medication editor 的 `disclaimerKinds` 传入即可。

### 5.1 提示矩阵

| HrtGoal | Medication | Severity | 提示 |
|---|---|---|---|
| FTM | CYPROTERONE_ACETATE | **STRONG** | "CPA is an MtF antiandrogen" — CPA 在 FtM 下临床禁忌 |
| FTM | ESTRADIOL_* | SOFT | "Estradiol is the MtF hormone" — E2 偶尔低剂量用于 FtM，soft prompt |
| MTF | TESTOSTERONE_* | SOFT | "Testosterone is the FtM hormone" — T 偶尔低剂量用于 MtF，soft prompt |
| UNSET / NON_BINARY | any | 无提示 | 用户主动选择不基于 goal 提示 |

### 5.2 集成方式

```kotlin
// MedicationLogEntryEditorSheet 的 call site
val safetyKinds = hrtGoalSafetyWarningKindsFor(
    goal = settings.hrtGoal,
    medicationKey = selectedKey,
    category = selectedCategory,
)
MedicationEditorSheetScaffold(
    ...,
    disclaimerKinds = safetyKinds,  // 与原有 disclaimer 合并
    ...
)
```

Helper：
- `hrtGoalSafetyWarningsFor(goal, key, category) → List<HrtGoalSafetyWarning>` — 矩阵源头
- `hrtGoalSafetyWarningKindsFor(...) → List<MedicalDisclaimerKind>` — 翻译成 disclaimer enum

### 5.3 已知未集成点（follow-up）

`MedicationLogEntryEditorSheet` 和 `MedicationGroupSlotEditorSheet` 的 `disclaimerKinds` 参数当前传 `emptyList()`。后续需要：
1. 把 `settingsState.hrtGoal` 通过 ViewModel 暴露给 sheet
2. 在 draft 改变时（用户切换 medication）实时计算 safetyKinds
3. 把 safetyKinds 合入 disclaimer 列表

PR F-PR4 提交了 helper + 字符串 + 测试 + enum 扩展；editor 集成是 1-2 个文件的小 follow-up。

## 6. 迁移指南

### 6.1 老用户（v0.5 → v0.6）

- **HrtGoal 默认 UNSET** — 行为完全保留（E2 趋势 + 全 chip + 无安全提示）
- 第一次进 Settings → Personalization 看到 "HRT goal" 卡片，可选可不选
- 不选则永远保持 UNSET 行为

### 6.2 已选 goal 的用户

- FTM 用户首次打开 app：主屏默认 T 曲线，E2 / CPA chip 消失，ANTIANDROGEN 卡片无 CPA
- MTF 用户首次打开 app：主屏默认 E2 曲线，T chip 消失（与 F-PR1 之前的"3-way"不同）
- NON_BINARY 用户：与 v0.5 行为一致
- 切换 goal：主屏 chip row 立即变化（无延迟，因为 `_currentHrtGoal` 直接接 settingsState flow）

### 6.3 老 custom-T 注射剂用户（F-PR2 之前）

- 老用户的 custom T 注射剂 group（`medicationKey = null`）继续走 `PkCompound.T` 兜底
- 不强制迁移到 TE/TC/TU catalog key，但建议在 medication editor 里切换以获得精确的 depot kinetics
- TE 200 mg 与 custom T 200 mg 的曲线**不完全相同**（TE 专属 fracFast=0.45 / k1Slow=0.0069；custom T 用平均 0.4 / 0.005）

## 7. 提交时间线

```
a1fa656  feat(safety): per-goal medication mismatch warnings (F-PR4 helper)
775fae9  feat(ui): per-goal chart default + chip filter + CPA-FtM exclusion (F-PR3)
3468a9d  feat(meds): 4 testosterone product keys + catalog-key compound routing (F-PR2)
6c8dd3a  feat(settings): HRT goal enum + DataStore persistence + dropdown (F-PR1)
```

加上 PR1-PR4（MtF 端 3 激素 PK 曲线），完整提交链见 `docs/mtf-pr-summary.md`：

```
ee9574e  docs: MtF 端 PR1-PR4 汇总报告
02e762f  feat(ui): 3-way home chart chip selector (PR4 UI)
5622c19  feat(viewmodel): wire CPA into home combine + buildHomeUiState (PR4 data)
42a5f4f  feat(pk): cyproterone acetate home-chart trend path (PR4 engine)
e17d742  refactor(ui): rename MainE2Chart* to MainChart* (PR3-UI rename)
d2bdee0  feat(ui): home chart E2/T chip selector (PR3-UI chip)
cab0fb1  feat(ui): home chart hormone selector state (PR3-UI selector)
a90e9ff  feat(pk): testosterone engine + dose-event mapping (PR3-UI data)
26039e7  docs(pk): testosterone clinical anchors + retune (PR3 docs)
e4318a3  feat(pk): testosterone clinical anchors + retune (PR3 engine)
d43de81  feat(pk): E2 patch products + injectable ester clinical anchors (PR3.5)
c2e953a  feat(snapshot): multi-hormone PK projection (PR2)
0fc3bc3  docs(pk): document cyproterone acetate calibration and non-linear caveats
d27c469  feat(pk): add cyproterone acetate to PkCatalog
```

## 8. 后续路线图

| 阶段 | 范围 | 状态 |
|---|---|---|
| F-PR4 follow-up | medication editor disclaimerKinds 接入 | 待办 |
| v2 血检追踪 | T 趋势 + Hb 警示（chip selector 已有，target band 走血检） | 待办 |
| Snapshot 升级 | `testosteronePkEntries` / `cpaPkEntries` 进 `HomeSnapshotInputs`，让 T/CPA 走 cache | 待办 |
| v2 T gel 校准 | 7 g/天 + 12.5 g/天 androgel 测试点 | 待办 |
| F-PR-end | MtF + FtM 完整 4-5 PR 汇总 | 进行中 |

## 9. 测试基线

| 阶段 | 总测试 | 失败 (baseline Windows flakes) |
|---|---|---|
| F-PR1 后 | 2411 + 3 = 2414 | 42 |
| F-PR2 后 | + 4 = 2418 | 42 |
| F-PR3 后 | + 9 = 2427 | 42 |
| F-PR4 后 | + 7 = 2434 | 42 |

**42 个失败 = baseline 已确认的 Windows FileStorage / jank-guard flakes**（在 `1ac4b78` 干净树上 stash check 验证），与本批 PR 无关。F-PR* 共加 23 个测试，全部绿。

## 10. 文件影响范围

```
app/src/main/java/com/mkx/hrttracker/
  model/settings/SettingsModels.kt        (F-PR1: HrtGoal enum + SettingsState 字段)
  model/settings/HrtGoalSafety.kt         (F-PR4: 矩阵 helper + 数据类)
  data/repository/SettingsRepository.kt   (F-PR1: DataStore 持久化)
  ui/settings/SettingsViewModel.kt        (F-PR1: setter)
  ui/settings/SettingsScreen.kt           (F-PR1: dropdown UI)
  ui/settings/SettingsUiText.kt           (F-PR1: 4 menu labels)
  ui/components/MedicalDisclaimerText.kt (F-PR4: 3 新 enum 种类)
  ui/main/MainViewModel.kt                (F-PR1 注入 / F-PR3 auto-picker / F-PR4 ANTIANDROGEN filter)
  ui/main/MainUiModels.kt                 (F-PR3 字段 + helper / F-PR4 翻译)
  ui/main/MainContent.kt                  (F-PR3 透传 availableChartHormones)
  ui/main/MainContentComponents.kt       (F-PR1+F-PR3 chip row filter)
  model/medication/MedicationCatalog.kt   (F-PR2 4 T keys + 注射剂 dose presets)
  model/medication/MedicineModels.kt      (F-PR2 IMPORTED_INJECTION_ESTER_KEYS 扩)
  model/pk/PkSimulation.kt                (F-PR2 catalog-key compound routing)
  util/MedicationCatalogText.kt           (F-PR2 4 labelRes 分支)

app/src/main/res/values/strings.xml       (F-PR1 4 + F-PR4 6 字符串)

app/src/test/java/com/mkx/hrttracker/
  model/settings/HrtGoalTest.kt           (F-PR1: 2 测试)
  model/settings/HrtGoalSafetyTest.kt     (F-PR4: 7 测试)
  ui/main/MainUiModelsChartHormoneTest.kt (F-PR3: 7 测试)
  ui/main/MainViewModelTest.kt            (F-PR1 setHrtGoal + F-PR3 hrtGoal × 2 测试)
  ui/settings/SettingsViewModelTest.kt    (F-PR1 setHrtGoal 1 测试)
  model/pk/PkSimulationTest.kt            (F-PR2 4 catalog-key 测试)
  ... 既有测试 (PR1-PR4)

docs/
  pk-differences.md                       (PR1+PR3 临床 anchor)
  mtf-pr-summary.md                       (MtF 端 4 PR 汇总)
  ftm-hrt.md                              (本文件)
```

—— END ——
