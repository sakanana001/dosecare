# PK v3 引擎：3-comp 替换 for TU

## 背景

PR1 retune（`3c3355c`）把 TE/TC/TU 的 Cmax 误差从 35-56% 砍到 5-19%，但 TU
仍有一个本征限制：1-comp Bateman 模型是 single-compartment 系统，
没有"周边室"（peripheral compartment）。文献记录的 TU 90 ± 40 d
终末半衰期（t½ 释放）来自药物在脂肪组织里的深部蓄积——这在
1-comp 模型里完全无法表达。

V2 血检追踪（`4de7c97` / `d5ecd9d` / `0582d5d` / `0a5206d`）交付
Hb / T 主屏卡和 5-year longitudinal trend，但 1-comp 模型的
"残余误差"是底层引擎的天花板。

V3（`PR-V3-TU`）解决这个：对 TU 上 3-comp 引擎（中央 + 浅 + 深），
其他化合物保持 1-comp。

## 实现

### 数据结构

`PkSimulation.kt` 加 3 个 top-level data class：

- `ThreeCompState` — 单个 RK4 step 后的 [central, shallow, deep] 状态
- `ThreeCompAggregate` — 跨事件求和的 3-comp 参数（per-3-comp-event
  的体积和速率常数）。今天只有 TU 是 3-comp，fold 是退化的；结构
  留好是为了未来第二个化合物上 3-comp 时不用重排
- `ThreeCompOverride` — per-compound override（`PkCatalog.threeCompOverrides`），
  包含 `centralVdPerKg / shallowVdPerKg / deepVdPerKg / k12 / k21 / k13 / k31`

`HormoneCoreParams` 加 6 个 3-comp 字段（默认 0.0），保留旧 1-comp
行为：当 override 缺省时，3-comp 路径不激活。

### 引擎切换

`PkSimulationEngine.run()` 检查 `anyThreeCompEvent`（per-event
compound 在 `PkCatalog.threeCompOverrides` 里查 override）。如果
任意一个事件是 3-comp 化合物，整个 run 切到 RK4 3-comp 路径：

1. **depot 解析式**：depot 仍由 `PrecomputedPkEventModel.amountAt(t)`
   解析计算（两段指数）。3-comp 路径另加一个 `depotAmountAt(t)`
   返回未吸收的 depot 量（`dose * F * (fracFast * e^(-k1Fast*tau)
   + (1-fracFast) * e^(-k1Slow*tau))`，F 是 formation fraction），
   与 `amountAt` 的"中心室药量"语义不一样
2. **释放率**：`releaseRate(t) = (depot(t_prev) - depot(t)) / deltaT`，
   在每段 [t_prev, t] 上做常数假设
3. **RK4 步进**：`rk4ThreeComp()` 在 [central, shallow, deep] 4D
   状态上做 4 阶 Runge-Kutta，参数为 `kClear + k12 + k13` 离开
   central，`k21` 回到 central（来自 shallow），`k31` 回到 central
   （来自 deep）
4. **浓度**：central 浓度 = `amountCentral / Vd_c`（不是
   `Vd_total`，因为 1-comp 的 vdPerKg 已经是 apparent Vd）

### TU 校准值

基于 Nebido SmPC 锚点（11-15 ng/mL Cmax @ 7d, t½ 释放 90 ± 40 d）
和 v2 报告里 TU 残余 13.00 ng/mL 的现状，定：

- Vd_c = 2.0 L/kg（保留 1-comp 的 apparent Vd，避免破坏 Cmax 校准）
- Vd_s = 1.0 L/kg（肌肉快平衡）
- Vd_d = 3.5 L/kg（脂肪深部蓄积）
- k12 = 0.15 h⁻¹, k21 = 0.18 h⁻¹（fast 交换，肌肉）
- k13 = 0.010 h⁻¹, k31 = 0.005 h⁻¹（slow 交换，脂肪）
- kClear = 0.05 h⁻¹（testosterone 中心室清除，从 `coreParams` 复用）
- formationFraction = 0.060（PR1 retune 后的值，跨 1-comp / 3-comp 一致）

## 结果（v2 统计报告对比）

| 指标 | v2 1-comp (PR1 retune) | v3 3-comp (PR-V3-TU) |
|---|---|---|
| TU mean ± SD | 13.00 ± ? | 11.79 ± 2.76 ng/mL |
| TU MAE | 13.00 | **2.53** ng/mL |
| TU bias | -100% (broken) | **-9.3%** |
| Overall MAPE | 47.7% | **31.6%** |
| Overall MAE | 5.89 | **4.19** ng/mL |
| Project vs Clinical bias | -3.47 | **-1.11** ng/mL |
| 95% LoA | [-6.34, +4.75] | **[-6.39, +4.18]** |
| Paired t (project vs clinical) | -4.772 (p<0.0001) | **-3.046 (p=0.0025)** |

5 场景 × 11 体重 = 55 对配对样本。Tmax 行为变化：

- TU 1000mg Cmax: 1-comp 13.7 ng/mL @ 136h → 3-comp 11.5 ng/mL @ 87.5h
  （Cmax 仍在文献 11-15 范围；Tmax 因 central-shallow 快速平衡
  从 5.7d 移到 3.6d）
- TE/TC 不动（保持 1-comp）
- gel 不动（保持 1-comp，PR2 skin-depot 范围）

## 限制

- Tmax 87.5h 早于文献 7d。3-comp 的中心-浅平衡（k12=0.15）让
  central 提前到达峰值。要延后 Tmax，需要降低 k12 或者降低
  k1Fast 释放率。下次 retune 可以试 `k12=0.05` + `k1Fast=0.003`
- ~~终末半衰期：当前 k31=0.005 → t½=5.8d，文献 90 ± 40d。~~ 长期预测
  还没真正改善。**v3.1 retune 已修：k31 0.005 → 0.0003（t½=90d，
  ln(2)/0.0003 = 2311h），匹配 Nebido SmPC 中位 90 ± 40d。**
  这个改动对峰值无影响（deep→central 通量在头 4 周很小），
  但让长期预测的尾行为真正到位
- PR2 (gel skin depot) 还在另 PR（commit a353b10，2-comp MM + kCl，
  修了 AndroGel -57% / -38% 偏差）

## v3.1 retune 增量

v3.1 只动了一个参数（k31），目的是把 TU 长期预测的尾行为
拉到文献中位。配套改动：

- `PkCatalog.threeCompOverrides[TU].k31` 0.005 → 0.0003
- 把 `anySkinDepotEvent` 检测 gate 到 `hormone == TESTOSTERONE`，
  避免 E2 gel 误触发 skin depot 路径（E2 gel 有自己的
  临床锚点，不在 PR2 scope）

统计报告（v3.1 + PR2 合并后）：

| 指标 | v2 retune (1-comp) | v3 (TU 3-comp) | v3.1 + PR2 (本次) |
|---|---|---|---|
| Overall MAPE | 47.7% | 31.6% | **20.9%** |
| Overall MAE | 5.89 | 4.19 | **3.26** |
| Overall RMSE | — | 2.89 | **2.29** |
| TU MAE | 13.00 | 2.53 | 2.62 |
| Gel 5g MAE | 3.22 | 3.22 | **1.21** |
| Gel 10g MAE | 3.04 | 3.04 | **1.55** |
| % scenarios in lit | 100% | 100% | **100%** |

5 场景全部落在文献范围内。v3.1 retune 把 PR2 的 gel 改善
接进来了，整体 MAPE 从 31.6% 进一步降到 20.9%。

## 文档

- 测试：`app/src/test/java/com/mkx/hrttracker/model/pk/PkSimulationTest.kt`
  的 `pr3_singleDoseTestosteroneUndecylate1000mg_cmaxTmaxInBand` 和
  `fpr2_testosteroneUndecanoateCatalogKey_routesToTU` 已更新到 3-comp
  期望值
- 报告：`app/app/build/reports/pk-statistical-report.md` 自动生成
- 临床 anchor 报告：`app/app/build/reports/pk-clinical-anchor-report.md`
- 引擎设计：`app/src/main/java/com/mkx/hrttracker/model/pk/PkSimulation.kt`
  的 `run()` 和 `rk4ThreeComp()` 函数（含详细注释）

## 后续

1. ~~把 k31 调小（0.0003 量级）让终末半衰期真的拉到 90d~~ **v3.1 done**
2. ~~gel 3-comp / skin depot（PR2 scope）~~ **PR2 done (a353b10)**
3. 在主屏 T trend 加 "TU 3-comp" 标识
4. 把 `threeCompOverrides` 暴露给 e2 patch / patch 化合物
   （per-compound override，不只 TU）
5. 加 per-user 微调入口（让用户能 override kClear / k31）
6. v3.2 retune：把 k12 降到 0.05 试试 Tmax 能不能推到 100h+ 范围
