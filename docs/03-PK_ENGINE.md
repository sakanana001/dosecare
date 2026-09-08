# 03 · PK 引擎

## 选型决策

精神科 + 内分泌共病药，绝大多数（约 90%）的临床用药用**一房室口服模型**就能给出足够精确的估算。剩下需要更复杂模型：

| 药物 | 选模型 | 理由 |
|------|--------|------|
| 大多数口服药（氯氮平、奥氮平、利培酮、SSRI、SNRI、苯二氮䓬等） | **一房室口服** | 数据成熟、公式简单、误差可控 |
| 锂盐 | **一房室口服 + 肾清除校正** | 治疗窗窄（0.6-1.2 mmol/L），但动力学简单 |
| 丙戊酸钠 | **一房室口服** | 但有非线性蛋白结合，浓度高时游离比例增加 |
| 卡马西平 | **一房室口服 + 自身诱导** | 长期用药会诱导自身代谢，ke 增大 |
| 苯妥英 | **Michaelis-Menten** | 经典非线性动力学，剂量稍变浓度巨变 |
| 静脉/肌肉（氟哌啶醇 IM、某些长效针剂） | **一房室 IM/IV** | 直接进中央室，无吸收相 |
| 长效注射剂（Paliperidone LAI, Aripiprazole LAI） | **三房室 + 释放相** | 释放速率限制，多室模型 |

**v0.1-v0.5 只做一房室（口服 + IM/IV）**。Michaelis-Menten 和三房室放到 v0.8+。

## 核心公式

### 一房室口服（口服给药最常用）

单次给药后 \( t \) 小时血药浓度：

$$
C(t) = \frac{F \cdot D \cdot k_a}{V_d (k_a - k_e)} \cdot (e^{-k_e t} - e^{-k_a t})
$$

其中：
- \( F \): 生物利用度（0-1）
- \( D \): 剂量（mg）
- \( k_a \): 吸收速率常数（h⁻¹），\( t_{max} \approx \frac{\ln(k_a/k_e)}{k_a - k_e} \)
- \( V_d \): 表观分布容积（L 或 L/kg）
- \( k_e \): 消除速率常数（h⁻¹），\( t_{1/2} = \ln 2 / k_e \)
- \( CL = V_d \cdot k_e \): 清除率

**多次给药**（每 τ 小时一次，n 次后）：

$$
C(t) = \sum_{i=0}^{n-1} C_{\text{single}}(t - i\tau) \cdot \mathbb{1}[t \geq i\tau]
$$

### 稳态平均浓度

$$
C_{ss,\text{avg}} = \frac{F \cdot D}{CL \cdot \tau}
$$

### 稳态峰浓度（Css,max）

最常用近似：\( C_{ss,\text{max}} = \frac{F \cdot D}{V_d} \cdot \frac{1}{1 - e^{-k_e \tau}} \)

（实际更精确用一房室稳态方程的导数找峰值。）

### 累积比

$$
R = \frac{1}{1 - e^{-k_e \tau}}
$$

> 例：t½ = 14h（氯氮平），每天一次（τ = 24h），R = 1 / (1 - e^(-0.0495 × 24)) ≈ 1 / (1 - 0.305) ≈ 1.44
> 即稳态峰值比首次峰值高 44%。

## 数值积分

### 为什么需要

- 单次解析公式虽然简洁，但**多药叠加**时用解析解很慢（每对 (t, drug) 都要算 \( \sum e^{-k(t-i\tau)} \)）
- **酶池模型**（v0.8+）有微分方程组，解析解不存在
- **三房室**也是 ODE 系统

→ 用 **RK4 (4阶 Runge-Kutta)** 统一所有模型。

### RK4 模板

```kotlin
fun integrate(
    initial: DoubleArray,            // 初始状态 [A_gut, A_central]
    rates: (DoubleArray, Double) -> DoubleArray,  // dx/dt = f(x, t)
    tStart: Double,
    tEnd: Double,
    stepHours: Double = 0.1
): List<Pair<Double, DoubleArray>>
```

- 步长 0.1h 在中等 ke 下误差 < 1e-5（与解析解对比）
- 对 Michaelis-Menten 等刚性系统将来用 RK45 自适应步长

## 多药叠加策略（v0.5）

### 简化方案（推荐起步）

每药独立计算自己的浓度曲线，最后**代数叠加**做可视化显示。

```kotlin
data class PolyPharmCurve(
    val t: List<Double>,                  // 时间轴
    val components: Map<String, List<Double>>,  // drugId -> 浓度
    val total: List<Double>,              // 总浓度
    val windowBreaches: List<TimeRange>   // 总浓度超治疗窗的时间段
)
```

### 完整方案（v0.8+）

把"酶池"作为共享状态，每个药按"经过该酶池清除"建模：

```
        ┌──────────────────┐
        │   Gut (口服)      │  → ka
        └──────────────────┘
                ↓
        ┌──────────────────┐  ←── 底物 A
        │   Plasma (中央室) │
        │                  │  ←── 底物 B (竞争)
        └──────────────────┘
                ↓
        ┌──────────────────┐
        │   CYP3A4 池      │  ← 抑制剂（降低清除）
        │   容量 = 1.0     │  ← 诱导剂（增加清除酶量）
        └──────────────────┘
                ↓
            代谢产物 + 清除
```

模型（Michaelis-Menten 简化）：

$$
\frac{dA_i}{dt} = -k_{e,i} \cdot \frac{A_i}{K_m + A_i + \sum_{j \neq i} A_j \cdot \frac{K_m}{K_{m,j}}} \cdot \frac{1}{1 + [I]/K_i}
$$

复杂但能反映"CYP3A4 多个底物竞争 + 抑制剂影响"。v0.8 再上。

## 浓度单位

| 药 | 常用单位 | 转换 |
|---|---|---|
| 氯氮平、奥氮平、利培酮、阿立哌唑 | ng/mL | 1 ng/mL = 1 μg/L |
| 锂盐 | mmol/L | 0.6-1.2 mmol/L = 0.42-0.84 mg/L (× 1.42) |
| 丙戊酸 | μg/mL | 50-100 μg/mL = 50-100 mg/L = 50-100 mg/dL (× 1) |
| 卡马西平 | μg/mL | 4-12 μg/mL = 4-12 mg/L |
| 苯妥英 | μg/mL | 10-20 μg/mL（总），1-2 μg/mL（游离） |
| 茶碱 | μg/mL | 10-20 μg/mL |

## 输出曲线格式

```kotlin
data class PkCurve(
    val drugId: String,
    val t: List<Double>,                  // [0, 0.5, 1.0, 1.5, ...] hours
    val c: List<Double>,                  // 浓度
    val cMax: Double,                     // Cmax
    val cMaxT: Double,                    // 达峰时间
    val cMin: Double,                     // Cmin (谷浓度)
    val cAvg: Double,                     // 平均浓度
    val auc: Double,                      // 曲线下面积
    val windowStatus: WindowStatus        // 治疗窗状态
)

enum class WindowStatus {
    BELOW,        // 低于治疗窗（次效）
    IN_WINDOW,    // 在治疗窗内（理想）
    ABOVE,        // 高于治疗窗（中毒风险）
    NO_WINDOW,    // 该药无治疗窗
    UNKNOWN       // 数据不足
}
```

## UI 渲染（Vico / MPAndroidChart）

- X 轴：时间（过去 24h + 未来 12h）
- Y 轴：浓度（ng/mL）
- 主曲线：实际估算浓度
- 灰色阴影：治疗窗（low-high 区间）
- 红色水平线：毒性阈值
- 蓝色虚线：服药时刻（marker）
- 多药时每药不同颜色叠加

## 校准：TDM 数据回算

当用户输入血药浓度实测值时，可调整 Vd / ke 估计（v0.8+）：

```
实测 C(td) = f(Vd, ke, ...)
↓
最小二乘拟合 → 个体化 Vd, ke
↓
更新本药后续所有计算
```

需要至少 2 个不同时间点的 TDM 才能稳定解算（稳态峰 + 稳态谷）。

## 校验

- 单药一房室解析解 vs 数值积分 RK4 误差 < 1e-4
- 稳态 vs 单次比例 R 在 t½/τ → ∞ 时 → ∞
- t½/τ → 0 时 R → 1
- 5 种典型药物的文献 Cmax 复现率 > 90%（在 ±30% 范围内）

## 性能

- 1000 个时间点 + 10 个药 = 10000 次浓度计算 < 5ms（RK4）
- 24h × 0.1h 步长 = 240 点是默认显示精度
- 长期曲线（30 天）用更粗步长（1h）

## 已知限制

- **不模拟食物效应**（虽然很多药空腹/餐后差很大，v0.5+ 再加食物系数）
- **不模拟肝肠循环**
- **不模拟活性代谢物对总效应的贡献**（v0.8+ 用 metabolite 池扩展）
- **不模拟肾功能动态变化**（只用静态 eGFR）
- 这些都是有意识取舍，不是不懂
