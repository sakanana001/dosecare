# FtM 端 (Transmasculine HRT) 5 PR 汇总报告

> 提交区间：ee9574e..d9616e0（5 commits）
> 范围：FtM 化 — HrtGoal 枚举、睾酮 4 种剂型、模式开关、安全提示、文档
> 策略：HrtGoal-driven 行为切换；MtF 默认保留，FTM/Non-binary 显式切换

---

## 1. PR 列表

| PR | 主题 | commits | 文件 | +/− | 测试 |
|---|---|---|---|---|---|
| **F-PR1** | HrtGoal 枚举 + DataStore + Settings UI | 6c8dd3a | 8 | +168 / 0 | +3 |
| **F-PR2** | 睾酮 4 种剂型 + PkCompound routing | 3468a9d | 6 | +286 / −12 | +4 |
| **F-PR3** | per-goal chart default + chip filter + CPA 隐藏 | 775fae9 | 6 | +367 / −17 | +9 |
| **F-PR4** | safety warning helper + MedicalDisclaimerKind | a1fa656 | 4 | +275 / 0 | +7 |
| **F-PR5** | docs/ftm-hrt.md 完整文档 | d9616e0 | 1 | +247 / 0 | — |
| **总计** | 5 commits | — | 25 文件 | **+1343 / −29** | +23 |

FtM 端完整 ship：HRT goal 4 case、睾酮 4 剂型 first-class、per-goal 主屏
切换、(goal, medication) 临床安全提示、完整文档。

## 2. 提交时间线

```
d9616e0  docs: FtM 端 PR 完整文档 (F-PR5)
a1fa656  feat(safety): per-goal medication mismatch warnings (F-PR4 helper)
775fae9  feat(ui): per-goal chart default + chip filter + CPA-FtM exclusion (F-PR3)
3468a9d  feat(meds): 4 testosterone product keys + catalog-key compound routing (F-PR2)
6c8dd3a  feat(settings): HRT goal enum + DataStore persistence + dropdown (F-PR1)
```

完整 19 commits（MtF + FtM）见 `docs/mtf-pr-summary.md`：

```
d9616e0  docs: FtM 端 PR 完整文档 (F-PR5)               ← FtM 端最末
a1fa656  feat(safety): per-goal medication mismatch warnings (F-PR4 helper)
775fae9  feat(ui): per-goal chart default + chip filter + CPA-FtM exclusion (F-PR3)
3468a9d  feat(meds): 4 testosterone product keys + catalog-key compound routing (F-PR2)
6c8dd3a  feat(settings): HRT goal enum + DataStore persistence + dropdown (F-PR1)
ee9574e  docs: MtF 端 PR1-PR4 汇总报告                   ← MtF 端 / FtM 端 边界
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

## 3. F-PR1 — HrtGoal 枚举 + DataStore + Settings UI

**commits**：`6c8dd3a`

**核心改动**：
- `HrtGoal` 枚举：UNSET（默认）/ MTF / FTM / NON_BINARY
- `SettingsState.hrtGoal: HrtGoal = HrtGoal.UNSET`（字段，backward-compatible）
- DataStore key `hrt_goal`（string preferences key），UNSET 不写盘
- `SettingsRepository.setHrtGoal(goal)` / `preferencesToStoredSettingsState` round-trip
- `SettingsViewModel.setHrtGoal(goal)` 走 `launchSettingsMutation`
- `HrtGoal.menuLabelRes` 映射 4 个 case
- Settings → Personalization 加 dropdown（mirror FirstDayOfWeek 模式）
- 字符串：4 个 hrt_goal_* label + 1 个 settings_hrt_goal_summary

**关键设计决策**：
- UNSET 是默认值（保留 v0.5 行为 — E2 趋势 + 全 chip + 无安全提示）
- NON_BINARY 主动跳过 goal-based 安全提示（用户显式 opt out）
- DataStore 不存 UNSET（key 不存在 → 重新落到 SettingsState 默认值），重置清空

**测试**：3 个新测试
- `HrtGoalTest.fromStorageValue_roundTripsEachGoal` — 4 case 全部 round-trip
- `HrtGoalTest.fromStorageValue_unknownOrNullDefaultsToUnset` — 未知 / null / 空 → UNSET
- `SettingsViewModelTest.setHrtGoal_forwardsEachValueToRepository` — ViewModel → Repository 透传

## 4. F-PR2 — 睾酮 4 种剂型 + PkCompound routing

**commits**：`3468a9d`

**核心改动**：
- `MedicationKey` 加 4 个 enum case（TE/TC/TU/GEL）
- `IMPORTED_INJECTION_ESTER_KEYS` 集合扩到 3 个 esters（GEL 不进，因为不是注射剂）
- `toTestosteronePkDoseEvent` 走两层 narrow：
  1. `category == TESTOSTERONE`（主过滤）
  2. `medicine.toTestosteronePkCompound()` → catalog key 映射 TE/TC/TU/T
- `toTestosteronePkCompound(MedicationKey)` 映射 TE/TC/TU → PkCompound.TE/TC/TU，GEL/custom-T → PkCompound.T
- `toTestosteronePkCompound(Medicine)` 同时检查 catalog selection（不只是 ImportedInjection 路径）
- `MedicationCatalog` 重构：testosteroneCatalog INJECTION 暴露 TE/TC/TU catalog entries + 剂量预设；GEL 暴露 TESTOSTERONE_GEL；ORAL/SUBLINGUAL 保持 custom-only（Jatenzo 是 post-v1 scope）
- `injectableTestosteroneDoseAssistPresets`：100/200 mg single-use + 100/200 mg/mL multi-use
- `injectableTestosteroneUndecanoateDoseAssistPresets`：1000 mg single-use + 250 mg/mL × 4 mL multi-use（Nebido 标准 form）
- `MedicationCatalogText.labelRes`：4 个新分支

**关键设计决策**：
- 4 剂型是 F-PR2 + F-PR3 的双重基础：F-PR2 让用户能选精确剂型（避免 "custom-T" 兜底），F-PR3 让 FTM 用户能开箱即用睾酮曲线
- 老的 custom-T 注射剂用户**继续走 `PkCompound.T` 兜底**（不强制迁移）
- TE 200 mg 与 custom T 200 mg 的曲线**不完全相同**（TE 专属 fracFast=0.45 / k1Slow=0.0069；custom T 用平均 0.4 / 0.005）

**临床校准 anchor**（与 PR3 引擎 retune 一致）：

| 剂型 / 剂量 | 文献 Cmax | 模型 Cmax (T+48h) | 误差 |
|---|---|---|---|
| TE 200 mg IM | ~1080 ng/dL @ 60h (Behre 1986 250 mg 缩放) | 1452 ng/dL @ 48h | +34% |
| TC 200 mg IM | 758 ± 289 ng/dL @ 71.7h (Slayback FDA) | 1076 ng/dL @ 48h | +42% |
| TU 1000 mg IM | 1100-1500 ng/mL @ 7d (Nebido SmPC) | 1981 ng/dL @ 120h | +32% |

**测试**：4 个新测试
- `fpr2_testosteroneEnanthateCatalogKey_routesToTE` — catalog-key 路径与 ester 显式路径在同 band
- `fpr2_testosteroneCypionateCatalogKey_routesToTC` — 同上
- `fpr2_testosteroneUndecanoateCatalogKey_routesToTU` — 同上
- `fpr2_testosteroneCatalogKey_dropsE2Entries` — E2 catalog entry 不进 T 引擎（category 过滤保活）

## 5. F-PR3 — per-goal chart default + chip filter + CPA 隐藏

**commits**：`775fae9`

**核心改动**：
- `MainUiState` 加 `hrtGoal: HrtGoal` + `availableChartHormones: Set<PkHormone>` 字段
- `availableChartHormonesFor(goal)` + `defaultChartHormoneFor(goal)` 助手
- `MainViewModel._currentHrtGoal: StateFlow<HrtHormone>`（auto-picker）
- `MainViewModel` init 加监听 settingsRepository.settingsState 的协程
- combine 扩到 9 参（加 `_currentHrtGoal`）— vararg + Array<Any?>
- `buildHomeUiState` 接收 `currentHrtGoal: HrtGoal` 参数（不再走 `inputs.settings.hrtGoal`）
- ANTIANDROGEN 卡片过滤：FTM 时排除全 CPA 的 group（spironolactone / bicalutamide / finasteride / dutasteride 仍显示）
- `MainChartCard` / `MainChartCardHeader` / `MainChartHormoneChipRow` 接收 `availableChartHormones: Set<PkHormone>`

**关键设计决策**：
- **per-goal chart hormone**：
  - FTM → TESTOSTERONE（唯一选项）
  - MTF → ESTRADIOL
  - NON_BINARY / UNSET → ESTRADIOL（中性默认）
- 切换 goal 时 auto-picker：
  - 如果当前 selectedChartHormone 不在新 goal 的 available set 里 → 回退到 goal 默认
  - 否则保留用户选择（per-session override）
- 来源用 `_currentHrtGoal`（settingsRepository.settingsState flow 驱动），不用 `inputs.settings.hrtGoal`（HomeSnapshot 缓存）— 保证 goal 切换**立即**生效

**测试**：9 个新测试
- `MainUiModelsChartHormoneTest` 7 个：per-goal whitelist 矩阵 + 默认永远在 available set
- `MainViewModelTest.hrtGoalChange_picksPerGoalDefaultChartHormone` — UNSET → E2, FTM → T, MTF → E2
- `MainViewModelTest.hrtGoalChange_surfacesAvailableChartHormonesInUiState` — UNSET → 3-way, MTF → E2+CPA, FTM → T-only

## 6. F-PR4 — per-goal medication mismatch warnings

**commits**：`a1fa656`

**核心改动**：
- `HrtGoalSafetyWarning` 数据类（titleRes, bodyRes, severity）
- `HrtGoalSafetySeverity` 枚举：STRONG / SOFT
- `hrtGoalSafetyWarningsFor(goal, key, category) → List<HrtGoalSafetyWarning>` 矩阵
- `hrtGoalSafetyWarningKindsFor(...) → List<MedicalDisclaimerKind>` 翻译
- `MedicalDisclaimerKind` 扩 3 个新 case：HRT_GOAL_MISMATCH_CPA_FTM / E2_FTM / T_MTF
- 字符串：3 个 title + 3 个 body

**关键设计决策**：
- **STRONG**：CPA 在 FtM 下临床禁忌（it is the antithesis of FtM）
- **SOFT**：E2 偶尔低剂量用于 FtM；T 偶尔低剂量用于 MtF — 都是 off-label 但可能合理，soft prompt 不阻塞保存
- **UNSET / NON_BINARY / custom medication**：无提示（用户主动 opt out 或无 catalog key 可路由）
- 编辑器集成（disclaimerKinds 传入）是 follow-up，**helper + 字符串 + 3 个新 MedicalDisclaimerKind 已 ship**

**矩阵**：

| HrtGoal | Medication | Severity |
|---|---|---|
| FTM | CYPROTERONE_ACETATE | **STRONG** |
| FTM | ESTRADIOL family | SOFT |
| MTF | TESTOSTERONE family | SOFT |
| UNSET / NON_BINARY | any | 无 |

**测试**：7 个新测试
- `HrtGoalSafetyTest.ftm_cpa_emitsStrongSafetyWarning` — FTM + CPA = STRONG
- `HrtGoalSafetyTest.ftm_estradiolFamily_emitsSoftSafetyWarning` — sweep 所有 ESTRADIOL_* keys
- `HrtGoalSafetyTest.mtf_testosteroneFamily_emitsSoftSafetyWarning` — sweep 所有 TESTOSTERONE_* keys
- `HrtGoalSafetyTest.ftm_testosterone_emitsNoWarning` — 正常 on-regimen 不警告
- `HrtGoalSafetyTest.mtf_cpa_emitsNoWarning` — 正常 on-regimen 不警告
- `HrtGoalSafetyTest.unsetAndNonBinary_emitNoWarning` — UNSET / NON_BINARY 静默
- `HrtGoalSafetyTest.customMedication_noKey_emitsNoWarning` — 无 catalog key 静默

**已知未集成点**：medication editor 的 `disclaimerKinds` 参数当前传 `emptyList()`。后续需要：
1. 把 `settingsState.hrtGoal` 通过 ViewModel 暴露给 sheet
2. 在 draft 改变时实时计算 safetyKinds
3. 把 safetyKinds 合入 disclaimer 列表

## 7. F-PR5 — docs/ftm-hrt.md 完整文档

**commits**：`d9616e0`

**新增** `docs/ftm-hrt.md`（247 行）：
1. 概览：HrtGoal 概念、4 激素选择 vs goal 的关系
2. HrtGoal 枚举详解（F-PR1）：4 个 enum case、DataStore 持久化
3. 睾酮 4 种剂型（F-PR2）：TE/TC/TU/GEL 完整表 + 临床校准 anchor
4. 模式开关 + 默认值（F-PR3）：per-goal chart hormone + ANTIANDROGEN 卡片过滤 + 3-way chip selector
5. 安全提示文案（F-PR4）：(goal, medication) 矩阵 + 集成方式 + 已知未集成点 follow-up
6. 迁移指南：v0.5 → v0.6 老用户、custom-T 注射剂用户
7. 提交时间线：F-PR1~F-PR4 + MtF 端 PR1~PR4 完整 19 commits
8. 后续路线图：F-PR4 follow-up + v2 血检 + snapshot 升级
9. 测试基线：F-PR1~F-PR4 共加 23 测试，0 回归
10. 文件影响范围：主代码 14 文件 + 测试 5 文件 + docs 3 文件

## 8. 测试基线

| 阶段 | 总测试 | 新增 | 失败 (baseline Windows flakes) |
|---|---|---|---|
| MtF PR1-PR4 末 | 2411 | — | 42 |
| F-PR1 后 | 2414 | 3 | 42 |
| F-PR2 后 | 2418 | 4 | 42 |
| F-PR3 后 | 2427 | 9 | 42 |
| F-PR4 后 | 2434 | 7 | 42 |
| F-PR5 后 | 2434 | 0 | 42 |

**关键观察**：
- 失败数 42 在整个 PR 序列中**恒定不变**（baseline verified by stashed-clean tree re-run）
- 失败全部是 Windows 平台的 FileStorage 文件锁问题 + UI 抖动 guard 测试（与本批 PR 无关）
- F-PR* 共加 23 个新测试，全部绿

## 9. 提交建议

如果按 PR 提交到 GitHub，每个 PR 建议这样命名：

| PR | 标题 | 描述 |
|---|---|---|
| **F-PR1** | feat(settings): HRT goal enum + DataStore persistence + dropdown | HRT goal 4 case；backward-compatible；UNSET 默认；可独立 ship |
| **F-PR2** | feat(meds): 4 testosterone product keys + catalog-key compound routing | T 4 剂型 first-class；te/tc/tu 临床校准 + 测试；engine 不变 |
| **F-PR3** | feat(ui): per-goal chart default + chip filter + CPA-FtM exclusion | FTM 默认 T；CPA 隐藏；chip 按 goal 过滤；antiandrogen 卡片过滤 |
| **F-PR4** | feat(safety): per-goal medication mismatch warnings (helper) | safety matrix helper + 字符串 + MedicalDisclaimerKind；editor 集成是 follow-up |
| **F-PR5** | docs: FtM 端 PR 完整文档 | docs/ftm-hrt.md（247 行） |

**注意点**：
- F-PR4 故意只 ship helper 不接 editor：UI 集成是 1-2 文件的小 follow-up，留在 release notes 标注
- F-PR1 是 SettingsState schema 变更：UNSET 默认保留老用户行为
- F-PR3 是行为变更最大（per-goal UI 变化），需要在 release notes 显式说明

## 10. 后续路线图（不在本批 PR 范围）

| 阶段 | 范围 |
|---|---|
| **F-PR4 follow-up** | medication editor 的 `disclaimerKinds` 接入 safetyKinds |
| **v2 血检追踪** | T 趋势 + Hb 警示（chip selector 已有，target band 走血检） |
| **Snapshot 升级** | `testosteronePkEntries` / `cpaPkEntries` 进 `HomeSnapshotInputs`，让 T/CPA 走 cache |
| **v2 T gel 校准** | 7 g/天 + 12.5 g/天 androgel 测试点 |
| **v2 抗雄剂校准** | spironolactone / bicalutamide / finasteride 临床 anchor（如果 FTM 用户有需要） |

## 11. 文件影响范围

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
  ftm-hrt.md                              (F-PR5: FtM 端详细文档)
  ftm-pr-summary.md                       (本文件)
```

—— END ——
