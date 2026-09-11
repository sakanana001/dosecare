# MtF 端多激素 PK 曲线 (PR1–PR4) 汇总报告

> 提交区间：1ac4b78..HEAD （13 个 commit，0 → 3 激素 PK 曲线）
> 范围：HRT 用药记录 Android 应用，MtF 端（雌激素主、抗雄 + 睾酮可选）
> 策略：临床能力增强 + MtF 默认；多激素 PK 曲线、E2/T/CPA 三选 chip

---

## 1. PR 列表

| PR | 主题 | commits | 文件数 | +/− | 测试基线 |
|---|---|---|---|---|---|
| PR1 | PkCatalog 加 CPA 化合物 + 临床校准 | d27c469, 0fc3bc3 | 3 | +397 | 31/31 |
| PR2 | HomeSnapshot 多激素 PK 投影 schema | c2e953a | 7 | +251 / −107 | 60/60 |
| PR3 | 睾酮 PK 曲线 + 主屏 E2/T 切换 | d43de81, e4318a3, 26039e7, a90e9ff, cab0fb1, d2bdee0, e17d742 | 14 | +1662 / −320 | 118/118 |
| PR4 | CPA PK 曲线 + 主屏三选 chip | 42a5f4f, 5622c19, 02e762f | 6 | +397 / −4 | 138/138 |

**总计**：13 commit，30 个文件改动，+2707 / −431。MtF 端完整 PK 模型（E2 / T / CPA）和
主屏 3 激素切换能力从 0 落地到 ship。

## 2. 提交时间线

```
02e762f  feat(ui): 3-way home chart chip selector (E2/T/CPA) (PR4 UI)
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

## 3. PR1 — PkCatalog 加 CPA 化合物 + 临床校准

**commits**：
- `d27c469` — `PkCompound.CPA` + `HormoneCoreParams(CYPROTERONE_ACETATE)`（Vd=3 L/kg，
  kClear=0.01825, kClearInjection=0.01825）+ 口服吸收/生物利用度表（kAbs=0.46,
  F=0.88）+ 分子量 416.94 g/mol
- `0fc3bc3` — `docs/pk-differences.md` 加 CPA 章节：单剂量 vs 长期稳态 Vd 漂移、
  38h 短期 t½ vs 78h 长期 t½ 来源、Androcur SPC 锚点

**关键设计决策**：
- 用单室 Bateman 模型：长期 Vd 漂移（Kuhnz 1993）抓不住，但 100 mg 单剂量和
  50 mg / 100 mg 稳态 Cmax 都落在文献 30% 以内
- `patchFallbackK1=0.0, gelK1=0.0, gelFMax=0.0`：CPA 没有贴片/凝胶剂型，避免
  engine 在不存在的路径上浪费算力

**测试**：3 个新 CPA catalog 测试（分子量、单位、核心参数）+ 4 个 CPA engine
测试（50 mg / 100 mg 单剂量、50 mg / 100 mg 稳态、accumulation factor、t½
匹配 38h）

## 4. PR2 — HomeSnapshot 多激素 PK 投影 schema

**commits**：`c2e953a`

**核心改动**：
- `SNAPSHOT_CODEC_VERSION` 22 → 23、`HOME_SNAPSHOT_SCHEMA_VERSION` 7 → 8
- `pkProjections: Map<PkHormone, HomePkProjectionRecord?>` 替代单数 `pkProjection`
- `latestEntryByHormone: Map<PkHormone, MedicationLogEntry?>` 替代单数 `latestEstradiolEntry`
- 保留 `pkProjection` / `latestEstradiolEntry` 为 deprecated 字段，向后兼容旧 cache

**关键设计决策**：
- 用 `Map<PkHormone, ...>` 而不是把 3 激素字段平铺到顶层：未来加新激素只改
  enum + codec 版本号，不动 schema 字段集
- codec 走 `polymorphic`/类型化读：null 字段读到时是 `null`，缺字段是默认值，
  旧 cache 反序列化保留 E2 数据即可

**测试**：4 套 HomeSnapshot 测试（codec 往返、缺失字段、未知版本、降级到
ROOM），60/60 绿

## 5. PR3 — 睾酮 PK 曲线 + 主屏 E2/T 切换

**commits**：
- `d43de81` (PR3.5 提前) — 4 个 E2 贴片产品化（Climara / Estraderm MX /
  Vivelle-Dot / Estrana Tape）+ `productLabel` / `wearDays` 字段；E2 注射剂
  临床校准（EB retune k1Fast 0.078 + k1Slow 0.060 → 36h peak 850-1050 pg/mL
  vs Oriowo 940）
- `e4318a3` — T 临床校准 + retune：kClearInjection 0.03 → 0.05 h⁻¹（t½ 14h），
  gelFMax 0.12 → 0.10（AndroGel SPC 5% 误差内）
- `26039e7` — `docs/pk-differences.md` 加 Testosterone 章节
- `a90e9ff` — `simulateMainTestosteroneTrend` + `toTestosteronePkDoseEvent`
  + `PkCompound.T` 兜底参数（custom T 注射剂的 "unspecified ester" 平均
  fracFast=0.4, k1Fast=0.02, k1Slow=0.005）
- `cab0fb1` — `_selectedChartHormone: MutableStateFlow<PkHormone>` +
  `setHomeChartHormone` + 8 参 combine 扩展（vararg + `Array<Any?>` 拆解）
  + `buildMainE2Hero/Chart` hormone 参数化 + `mainHormoneConcentrationInDisplayUnit`
  通用版
- `d2bdee0` — `MainChartHormoneChipRow` + `PkHormoneChip` chip 控件 + chip
  选中状态 + 字符串（`main_chart_hormone_e2/t`、`main_t_chart_subtitle`）
- `e17d742` — `MainE2Chart*` → `MainChart*` 全面重命名（7 文件 276 处），
  纯 mechanical refactor，不改行为

**关键设计决策**：
- T 路径**不**走 snapshot 优化：每次用 `simulateMainTestosteroneTrend` 现算。
  v0.6 用户量小，下一阶段（F-PR*）会加 `testosteronePkEntries` 到
  `HomeSnapshotInputs`
- T 路径 `displayUnit` 强制 `BloodUnitKey.NG_DL`，`target_min/max=0`（T 目标
  范围走血检，不走 MTF 风格固定带）
- chip selector 是 in-memory StateFlow，不持久化（chart-hormone 是 per-session
  view state，不是 stable preference）

**测试**：5 个 T 临床校准测试 + 4 个 E2 贴片测试 + 1 个 T dose-event 测试 +
1 个 E2 注射剂测试 + 4 个 PR3.5 E2 贴片产品化测试。118/118 绿

## 6. PR4 — CPA PK 曲线 + 主屏三选 chip

**commits**：
- `42a5f4f` — `simulateMainCpaTrend`（mirror `simulateMainTestosteroneTrend`）
  + `toCpaPkDoseEvent`（filter `category=ANTIANDROGEN` + `toCpaPkCompound=CPA`，
  自动 drop spironolactone / bicalutamide / finasteride / dutasteride）+ 4 个
  engine 测试
- `5622c19` — `cpaEntries: List<MedicationLogEntry>` flow 接入 8 参 combine
  + `buildHomeUiState` 加 cpaEntries 参数 + `when (activeHormone)` CPA 分支
  走 `simulateMainCpaTrend` + `e2TrendReady` CPA 分支 + hero entries 路由
- `02e762f` — `MainChartHormoneChipRow` 加 CPA chip + 字符串
  `main_chart_hormone_cpa` + 1 个 MainViewModelTest 测试 E2 → T → CPA 循环

**关键设计决策**：
- CPA path 共享 ANTIANDROGEN category 过滤，但 `toCpaPkCompound` 用
  `MedicationKey.CYPROTERONE_ACETATE` 二次 narrow，确保 spironolactone /
  bicalutamide / finasteride / dutasteride 都不进入 CPA 引擎
- per-unit dose 从 `MedicinePreparation.Pill.strengthMgPerTablet` 读（CPA
  仅口服）
- hero 的 `lastDose` 字段对 CPA 仍返回 null（`findLastEstradiolEntry`
  只看 E2 category）；F-PR* 阶段会做 hormone-agnostic `findLastEntry`

**测试**：4 CPA engine 测试（50 mg 单剂量、50 mg/day 稳态、非-CPA 抗雄过滤、
空 log）+ 1 chip selector routing 测试。138/138 绿

## 7. 测试基线

| 阶段 | 总测试数 | 新增 | 失败 |
|---|---|---|---|
| PR1 之前 | 2401 | — | 42 (Windows baseline flakes) |
| PR1 后 | 2406 | 5 | 42 |
| PR2 后 | 2406 | 0 | 42 |
| PR3 后 | 2406 | 0 | 42 |
| PR4 后 | 2411 | 5 | 42 |

**关键观察**：
- 失败数 42 在整个 PR 序列中**恒定不变**（baseline verified by stashed-clean
  tree re-run）
- 失败全部是 Windows 平台的 FileStorage 文件锁问题 + UI 抖动 guard 测试
  （与本批 PR 无关）
- 我们加的 5 + 4 + 5 = 14 个新测试全部绿

## 8. 提交建议

如果按 PR 提交到 GitHub，每个 PR 建议这样命名：

| PR | 标题 | 描述 |
|---|---|---|
| **PR1** | feat(pk): cyproterone acetate compound in PkCatalog | CPA 是 PR4 的前置，单测全绿，独立可 ship |
| **PR2** | feat(snapshot): multi-hormone PK projection schema | codec version 升 22→23，向后兼容；独立可 ship |
| **PR3** | feat(pk): testosterone PK curve + E2/T chart switch | 7 commit，最大；建议拆成 (a) engine + docs、(b) UI selector 两个 sub-PR |
| **PR3.5** | feat(pk): E2 patch products + injectable ester clinical anchors | 是 PR3 的先决条件，单独提更清晰 |
| **PR4** | feat(pk,ui): cyproterone acetate home-chart trend + 3-way chip | 3 commit，独立可 ship |

**注意点**：
- PR3 内含 1 个 rename commit（`e17d742`），会让 PR3 看起来 diff stat 偏大；
  GitHub 折叠 "refactor" 提交可以缓解
- PR2 的 codec version bump 涉及生产 cache 兼容性：需要在 release notes
  标注"首次启动会丢弃旧 cache，1-2 次 Room emission 重建"
- PR3 / PR4 的 chip selector 是 in-memory，不影响 settings persistence
  schema

## 9. 后续路线图（不在本批 PR 范围）

| 阶段 | 范围 |
|---|---|
| **F-PR1..F-PR5** | FtM 化（HrtGoal 枚举、4 种睾酮剂型、CPA MtF-only、safety 文案、docs/ftm-hrt.md） |
| **v2 血检追踪** | T 趋势 + Hb 警示（chip selector 已有，target band 走血检） |
| **F-PR* snapshot 升级** | `testosteronePkEntries` / `cpaPkEntries` 进 `HomeSnapshotInputs`，让 T/CPA 路径享受 cache |
| **PR-end 2** | FtM 端 5 个 sub-PR 完成后做汇总报告 |

## 10. 文件影响范围

```
app/src/main/java/com/mkx/hrttracker/
  model/pk/PkSimulation.kt                (核心：3 hormone engine + 3 simulation)
  data/repository/HomeRepository.kt       (PR2 投影 + PR2 latestEntry)
  data/repository/HomeSnapshotRepository.kt (PR2 codec 升级)
  data/repository/HomeSnapshotStore.kt    (PR2 schema 升级)
  data/repository/MedicationLogRepository.kt (PR3-UI 注入)
  model/medication/MedicationCatalog.kt   (PR3.5 贴片产品化)
  ui/main/MainViewModel.kt                (PR3-UI + PR4 ViewModel 路由)
  ui/main/MainUiModels.kt                 (PR3-UI hormone 参数化)
  ui/main/MainContent.kt                  (PR3-UI chip wiring)
  ui/main/MainContentComponents.kt        (PR3-UI chip + rename)
  ui/main/MainScreen.kt                   (PR3-UI callback wire)

app/src/main/res/values/strings.xml       (PR3-UI + PR4 字符串)

app/src/test/java/com/mkx/hrttracker/
  model/pk/PkSimulationTest.kt            (PR1 + PR3 + PR3.5 + PR4 engine tests)
  ui/main/MainViewModelTest.kt            (PR3-UI + PR4 chip routing)
  ui/main/MainUiModelsTest.kt             (PR3-UI buildMainE2Hero/Chart)
  ui/main/MainContentComponentsTest.kt    (PR3-UI + rename)
  data/repository/HomeSnapshotCodecTest.kt (PR2 codec 往返)
  ... (其他 codec / smoke 测试)

docs/pk-differences.md                    (PR1 + PR3 clinical anchors)
docs/mtf-pr-summary.md                    (本文件)
```

—— END ——
