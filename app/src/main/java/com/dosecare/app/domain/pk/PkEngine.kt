package com.dosecare.app.domain.pk

import kotlin.math.exp
import kotlin.math.ln

/**
 * PK 计算引擎
 *
 * 职责：
 * 1. 单次给药：解析公式（[PkModel.OneCompartmentWithAbsorption.concentrationAt]）
 * 2. 多次给药：卷积叠加
 * 3. 稳态估算：Css_peak、Css_trough、Css_avg、累积比
 * 4. （v0.8+）多药酶池竞争模型
 *
 * 全部纯函数，无副作用。线程安全。
 */
class PkEngine {

    /**
     * 多次给药后的浓度曲线。
     *
     * 对每个时间点 t，把所有 t ≤ t 的历次给药贡献累加。
     *
     * @param model PK 模型
     * @param doseEvents 给药事件序列（无需排序，内部会排）
     * @param tStart 曲线起点（h）
     * @param tEnd 曲线终点（h）
     * @param stepHours 时间步长（h），默认 0.1（24h → 240 点）
     * @param useDoseUnit 是否按 doseEvent 的单位计算（默认 mg）
     */
    fun multiDoseCurve(
        model: PkModel,
        doseEvents: List<DoseEvent>,
        tStart: Double = 0.0,
        tEnd: Double = 24.0,
        stepHours: Double = 0.1
    ): PkCurve {
        require(tEnd > tStart) { "tEnd must be > tStart" }
        require(stepHours > 0) { "stepHours must be > 0" }

        val sortedDoses = doseEvents.sortedBy { it.tHours }
        val tAxis = mutableListOf<Double>()
        val cAxis = mutableListOf<Double>()

        var t = tStart
        while (t <= tEnd + 1e-9) {
            var total = 0.0
            for (event in sortedDoses) {
                if (event.tHours <= t) {
                    val tau = t - event.tHours
                    total += when (model) {
                        is PkModel.OneCompartmentWithAbsorption ->
                            model.concentrationAt(event.doseMg, tau)
                        is PkModel.OneCompartmentIv ->
                            model.concentrationAt(event.doseMg, tau)
                        is PkModel.TwoCompartmentWithAbsorption ->
                            model.concentrationAt(event.doseMg, tau)
                        is PkModel.ThreeCompartmentWithAbsorption ->
                            model.concentrationAt(event.doseMg, tau)
                    }
                }
            }
            tAxis.add(t)
            cAxis.add(total)
            t += stepHours
        }

        val cMax = cAxis.max()
        val cMaxT = tAxis[cAxis.indexOf(cMax)]
        val cMin = cAxis.min()
        val cAvg = cAxis.average()
        val auc = trapezoid(tAxis, cAxis)

        return PkCurve(
            drugId = "",
            t = tAxis,
            c = cAxis,
            cMax = cMax,
            cMaxT = cMaxT,
            cMin = cMin,
            cAvg = cAvg,
            auc = auc
        )
    }

    /**
     * 稳态估算：等间隔给药达到稳态后的峰/谷/平均浓度。
     *
     * 假设所有剂量完全相同、给药间隔固定。
     *
     * 稳态峰 = 单次峰 × 累积比 R
     * 稳态谷 = 单次峰 × R × e^(-ke·τ)   (口服时近似)
     * 稳态平均 = F·D / (CL·τ)
     */
    fun steadyState(
        model: PkModel,
        doseMg: Double,
        tauHours: Double,
        numDoses: Int = 30  // 默认 30 次给药达到稳态
    ): SteadyStateResult {
        require(tauHours > 0)
        require(doseMg > 0)

        val ke = when (model) {
            is PkModel.OneCompartmentWithAbsorption -> model.kePerHour
            is PkModel.OneCompartmentIv -> model.kePerHour
            is PkModel.TwoCompartmentWithAbsorption -> model.betaPerHour
            is PkModel.ThreeCompartmentWithAbsorption -> model.gammaPerHour
        }
        require(ke > 0) { "Cannot compute steady state without ke" }

        // 累积比 R = 1 / (1 - e^(-ke·τ))
        val r = 1.0 / (1.0 - exp(-ke * tauHours))

        // 模拟末次给药后的 0..τ 浓度变化作为稳态一周期
        val doses = (0 until numDoses).map { DoseEvent(doseMg, it * tauHours) }
        val curve = multiDoseCurve(
            model = model,
            doseEvents = doses,
            tStart = (numDoses - 1) * tauHours,
            tEnd = numDoses * tauHours,
            stepHours = tauHours / 100.0  // 周期内 100 个点
        )

        return SteadyStateResult(
            cMax = curve.cMax,
            cMaxT = curve.cMaxT,
            cMin = curve.cMin,
            cAvg = curve.cAvg,
            accumulationRatio = r,
            halfLivesToSteadyState = computeHalfLivesToSteadyState(ke, tauHours)
        )
    }

    /**
     * 达到稳态 90% 所需的半衰期数。
     * 公式：n = -log(1 - 0.9) / log(2) · t½ / τ  → 约 3.32 · t½ / τ
     */
    private fun computeHalfLivesToSteadyState(ke: Double, tauHours: Double): Double {
        val tHalf = ln(2.0) / ke
        return 3.32 * tHalf / tauHours
    }

    /**
     * 多药浓度曲线叠加（简化版：代数和，v0.5）
     *
     * 注意：代数和不等同于"血药浓度叠加的临床效应"。
     * 同类药叠加可能协同（1+1=3）或拮抗（1+1=0.5）。
     * 真实多药效应用"受体占有率模型"或"酶池模型"。
     * v0.5 仅做可视化叠加，警示由规则引擎独立处理。
     */
    fun polyPharmCurveSum(
        curves: List<Pair<String, PkCurve>>
    ): PkCurve {
        require(curves.isNotEmpty())
        val tAxis = curves.first().second.t
        val n = tAxis.size
        val totalC = DoubleArray(n)

        for ((_, curve) in curves) {
            require(curve.t.size == n) { "All curves must share same t axis" }
            for (i in 0 until n) totalC[i] += curve.c[i]
        }

        return PkCurve(
            drugId = "TOTAL",
            t = tAxis,
            c = totalC.toList(),
            cMax = totalC.max(),
            cMaxT = tAxis[totalC.indexOfFirst { it == totalC.max() }],
            cMin = totalC.min(),
            cAvg = totalC.average(),
            auc = trapezoid(tAxis, totalC.toList())
        )
    }

    /**
     * 梯形积分求 AUC
     */
    private fun trapezoid(t: List<Double>, c: List<Double>): Double {
        if (t.size < 2) return 0.0
        var sum = 0.0
        for (i in 0 until t.size - 1) {
            val dt = t[i + 1] - t[i]
            sum += (c[i] + c[i + 1]) / 2.0 * dt
        }
        return sum
    }
}

/**
 * 稳态计算结果
 */
data class SteadyStateResult(
    val cMax: Double,
    val cMaxT: Double,
    val cMin: Double,
    val cAvg: Double,
    val accumulationRatio: Double,
    val halfLivesToSteadyState: Double
)
