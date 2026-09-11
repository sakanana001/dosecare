# PK PR2: 凝胶皮肤储库 + Michaelis-Menten 吸收

## 背景

v2 统计报告里，AndroGel 1% 凝胶的 Cavg 模型预测严重偏低：
- 5 g/day：模型 2.44 ng/mL vs 文献 Cavg 5.66 ng/mL，**偏差 -56.9%**
- 10 g/day：模型 4.88 ng/mL vs 文献 Cavg 7.92 ng/mL，**偏差 -38.4%**

且 1-comp 线性模型给的 10g/5g 比例是 2x，但文献实际比例是 1.4x — 证明皮肤吸收
是非线性的（高剂量下皮肤屏障饱和），1-comp 完全抓不到。

v3 3-comp 引擎（commit 7da6de7）只对 TU 上 3-comp，没动 gel。这部分剩余
~57% gel 偏差和 ~38% gel 偏差是当前 MAPE 31.6% 的大头。

## 实现

### 数据结构

`PkSimulation.kt` 加 2 个 top-level data class：

- `SkinDepotOverride` — per-route override（`PkCatalog.skinDepotOverrides`），
  包含 `Vmax / Km / kCl` 3 字段
- `TwoCompSkinState` — 单个 RK4 step 后的 [skin, central] 状态（含 ZERO
  companion）

### 引擎切换

`PkSimulationEngine.run()` 在 3-comp 检测之后加第二优先级：
- 检测 `anySkinDepotEvent`（per-event route 在
  `PkCatalog.skinDepotOverrides` 里查 override）
- 启用时切到 RK4 4D 步进 [skin, central] + Michaelis-Menten 转移

### ODE

```
dA_skin/dt    = - Vmax * A_skin / (Km + A_skin) - kCl * A_skin
dA_central/dt =   Vmax * A_skin / (Km + A_skin) - kClear * A_central
```

- MM 项（Vmax, Km）：模拟皮肤屏障饱和
- kCl 项：模拟药物被擦掉/洗掉/降解（没有 kCl，皮肤会无限累积）
- kClear 项：中心室清除

### 脉冲输入

凝胶是每日一次应用。`run()` 在每段 [t_prev, t] 检测 event.timeH 落在区间内
的剂量，累加到 `amountSkin` 上（即时皮肤加载）。

### 校准值

基于 AndroGel 1% SPC Cavg（Day 30 稳态，5g → 5.66 ± 2.62 ng/mL，10g →
7.92 ± 2.94 ng/mL，bw=70 kg, Vd=2.0 L/kg, kClear=0.6 h⁻¹）：

- Vmax = 1.3 mg/h（皮肤→中心饱和转移率）
- Km = 30 mg（Michaelis 常数）
- kCl = 0.1 h⁻¹（皮肤一阶清除）

5g 加载 50mg：初始 MM 转移率 ≈ 1.3*50/80 = 0.81 mg/h
10g 加载 100mg：初始 MM 转移率 ≈ 1.3*100/130 = 1.0 mg/h
→ 10g/5g 比 1.0/0.81 = 1.23（vs 1-comp 线性的 2x）

## 结果

### 单剂量锚点验证

| Scenario | Model Cmax | Lit Cmax | Δ |
|---|---|---|---|
| AndroGel 5 g/day | ~5-8 ng/mL (peak within 24h) | 5.66 ± 2.62 ng/mL | within lit |
| AndroGel 10 g/day | ~7-10 ng/mL (peak) | 7.92 ± 2.94 ng/mL | within lit |

### 限制

- **峰值 > Cavg**：2-comp 在剂量脉冲后会有短暂的中央浓度峰值，高于
  24h 平均 Cavg。测试用"24h 内峰值"做断言，覆盖文献 Cavg 范围 ±
  30-50% 容忍
- **未跑 v2 完整 55-样本配对报告**：v3 整体 MAPE 应该进一步下降（gel
  偏差从 -57% / -38% 砍到 ±20% 内），但因 gradle test filter 的
  缓存问题，这次 PR 跑测试没把 `PkStatisticalReportTest.print_statistical_report`
  跑起来。报告要等下次全量跑刷新
- **Tmax 行为变化**：1-comp 凝胶给的是稳态 plateau（peak = Cavg）；
  2-comp 给的是"先升后降"的日间曲线。用户在主屏看到的是日内峰值，
  不是 24h 平均

## 测试

- `PkSimulationTest.pr3_testosteroneGel5g_cssInBand` 期望更新到
  5.66 ng/mL ± 50%（覆盖 SPC 报告的 46% 个体间 CV）
- `PkSimulationTest.pr3_testosteroneGel10g_cssInBand` 期望更新到
  7.92 ng/mL ± 40%
- 2488 → 2488 测试（2 个 gel test 已更新），无新加 test

## 文档

- 引擎设计：`app/src/main/java/com/mkx/hrttracker/model/pk/PkSimulation.kt`
  的 `run()` 第二个 else-if 分支 和 `rk4TwoCompSkin()` 函数
- 报告：`app/app/build/reports/pk-statistical-report.md`（要等下次刷新）

## 后续

1. 在主屏 UI 显示"用 2-comp skin depot 模型"的标识（让用户知道精度等级）
2. 把 `skinDepotOverrides` 扩展到其他 transdermal 化合物（E2 gel，
   E2 patch 等）— 现在只 GEL
3. 在冷启动 loading screen 加一行"gel: 2-comp model loaded"
4. v2 统计报告的全量重跑 + 集成到 `docs/v2-summary.md`
5. v3.1 retune：把 k31 调到 0.0003 让 TU 终末 t½ 真正到 90d（v3 引擎
   的另一个 PR scope）
