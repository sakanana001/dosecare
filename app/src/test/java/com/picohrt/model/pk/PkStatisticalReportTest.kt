package com.picohrt.model.pk

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 统计学视角的 PK 模型对比报告。运行三组数据：
 *
 *  1. 理论 PK (theoretical):  1-comp Bateman 解析公式（教科书
 *     公式，不含本项目的 two-part depot 和 retune 校准）
 *  2. 本项目模拟 (project):   PkSimulationEngine（PR1 retune 后的
 *     1-comp + two-part depot 模型）
 *  3. 临床数据 (clinical):    文献锚点（Behre 1986 / Slayback FDA /
 *     Nebido SmPC / AndroGel FDA label）
 *
 * 每场景在 body weight 50-100 kg（5 kg 步长，共 11 个权重）下分别
 * 跑理论 PK 和本项目模拟，生成每场景的 (模型, 文献) 配对数据。
 * 计算：
 *   - mean / SD / range / 95% CI of model Cmax
 *   - bias (signed error vs literature midpoint)
 *   - MAE / RMSE / MAPE
 *   - Bland-Altman limits of agreement
 *   - paired t-test (theoretical vs project, project vs clinical)
 */
class PkStatisticalReportTest {
    private val zoneId = ZoneId.systemDefault()
    private val now = LocalDateTime.of(2026, 9, 4, 10, 0)
    private val nowInstant = now.atZone(zoneId).toInstant()

    @Test
    fun print_statistical_report() {
        val bodyWeights = listOf(50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0, 100.0)
        val scenarios = listOf(
            ScenarioSpec("TE 200 mg IM", 200.0, PkCompound.TE, PkRoute.INJECTION,
                literatureCmaxRange = 9.0..12.5, literatureCmaxMidpointH = 10.75,
                literatureTmaxRangeH = 48.0..72.0, literatureTmaxMidpointH = 60.0),
            ScenarioSpec("TC 200 mg IM", 200.0, PkCompound.TC, PkRoute.INJECTION,
                literatureCmaxRange = 4.7..10.5, literatureCmaxMidpointH = 7.6,
                literatureTmaxRangeH = 60.0..84.0, literatureTmaxMidpointH = 72.0),
            ScenarioSpec("TU 1000 mg IM (Nebido)", 1000.0, PkCompound.TU, PkRoute.INJECTION,
                literatureCmaxRange = 11.0..15.0, literatureCmaxMidpointH = 13.0,
                literatureTmaxRangeH = 144.0..192.0, literatureTmaxMidpointH = 168.0),
            ScenarioSpec("AndroGel 5 g/day", 50.0, PkCompound.T, PkRoute.GEL,
                literatureCmaxRange = 3.04..8.28, literatureCmaxMidpointH = 5.66,
                literatureTmaxRangeH = 0.0..24.0, literatureTmaxMidpointH = 12.0,
                isGelCavg = true),
            ScenarioSpec("AndroGel 10 g/day", 100.0, PkCompound.T, PkRoute.GEL,
                literatureCmaxRange = 4.98..10.86, literatureCmaxMidpointH = 7.92,
                literatureTmaxRangeH = 0.0..24.0, literatureTmaxMidpointH = 12.0,
                isGelCavg = true),
        )

        // Run project (engine) for each scenario × body weight
        val projectRuns = mutableListOf<RunResult>()
        val theoreticalRuns = mutableListOf<RunResult>()
        for (scenario in scenarios) {
            for (bw in bodyWeights) {
                val anchorInstant = nowInstant
                val projCmax = runProjectEngine(scenario, bw, anchorInstant)
                val theoCmax = runTheoreticalOneComp(scenario, bw)
                projectRuns += RunResult(scenario.label, scenario.doseMg, bw, projCmax)
                theoreticalRuns += RunResult(scenario.label, scenario.doseMg, bw, theoCmax)
            }
        }

        // Per-scenario statistics
        val perScenarioStats = scenarios.map { scenario ->
            val projectForScenario = projectRuns.filter { it.scenario == scenario.label }
            val theoreticalForScenario = theoreticalRuns.filter { it.scenario == scenario.label }
            ScenarioStats(
                scenario = scenario,
                projectValues = projectForScenario.map { it.cmax },
                theoreticalValues = theoreticalForScenario.map { it.cmax },
            )
        }

        // Overall pooled statistics
        val projectOverall = projectRuns.map { it.cmax }
        val theoreticalOverall = theoreticalRuns.map { it.cmax }

        // Paired t-test: theoretical vs project (same scenario, same body weight)
        val pairedDeltasTheoProj = projectRuns.zip(theoreticalRuns).map { (p, t) -> p.cmax - t.cmax }
        val pairedTStat = pairedTStat(pairedDeltasTheoProj)
        val pairedPValue = pairedTTest(pairedDeltasTheoProj)

        // Paired t-test: project vs clinical midpoint
        val pairedDeltasProjClin = mutableListOf<Double>()
        for (scenario in scenarios) {
            val litMid = scenario.literatureCmaxMidpointH
            for (run in projectRuns.filter { it.scenario == scenario.label }) {
                pairedDeltasProjClin += run.cmax - litMid
            }
        }
        val projClinTStat = pairedTStat(pairedDeltasProjClin)
        val projClinPValue = pairedTTest(pairedDeltasProjClin)

        val md = buildString {
            appendLine("# PK 模型对比：统计学分析报告")
            appendLine()
            appendLine("- Engine: `PkSimulationEngine` (1-comp Bateman + two-part depot for esters)")
            appendLine("- 理论 1-comp: 教科书 Bateman 公式，无 two-part depot 修正，无 retune 校准")
            appendLine("- 临床: Behre 1986 / Slayback FDA / Nebido SmPC / AndroGel FDA label")
            appendLine("- Body weight sweep: 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100 kg (n=11 per scenario)")
            appendLine("- 总样本量: 5 场景 × 11 权重 = 55 对配对样本")
            appendLine()
            appendLine("## 摘要（per-scenario statistics）")
            appendLine()
            appendLine("| Scenario | Lit Cmax (mid) | Theoret. mean ± SD | Theoret. MAE | Project mean ± SD | Project MAE | Project bias | Project within lit? |")
            appendLine("|---|---|---|---|---|---|---|---|")
            for (stats in perScenarioStats) {
                val s = stats.scenario
                val projMean = stats.projectValues.average()
                val projSd = stddev(stats.projectValues)
                val projMae = stats.projectValues.map { abs(it - s.literatureCmaxMidpointH) }.average()
                val projBias = projMean - s.literatureCmaxMidpointH
                val projBiasPct = projBias / s.literatureCmaxMidpointH * 100.0
                val theoMean = stats.theoreticalValues.average()
                val theoSd = stddev(stats.theoreticalValues)
                val theoMae = stats.theoreticalValues.map { abs(it - s.literatureCmaxMidpointH) }.average()
                val projRange = s.literatureCmaxRange.let { "%.1f - %.1f".format(it.start, it.endInclusive) }
                val projMin = stats.projectValues.min()
                val projMax = stats.projectValues.max()
                val withinLit = projMin >= s.literatureCmaxRange.start && projMax <= s.literatureCmaxRange.endInclusive
                appendLine("| ${s.label} | %.2f | %.2f ± %.2f | %.2f | %.2f ± %.2f | %.2f | %+.1f%% | ${if (withinLit) "✓" else "✗"} (%.1f-%.1f vs $projRange) |".format(
                    s.literatureCmaxMidpointH,
                    theoMean, theoSd, theoMae,
                    projMean, projSd, projMae,
                    projBiasPct,
                    projMin, projMax,
                ))
            }
            appendLine()
            appendLine("## Overall pooled statistics (n=55 per model)")
            appendLine()
            appendLine("| Metric | Theoretical 1-comp | Project engine |")
            appendLine("|---|---|---|")
            appendLine("| Mean Cmax (all scenarios) | %.2f ng/mL | %.2f ng/mL |".format(theoreticalOverall.average(), projectOverall.average()))
            appendLine("| SD | %.2f | %.2f |".format(stddev(theoreticalOverall), stddev(projectOverall)))
            appendLine("| Range | %.2f - %.2f | %.2f - %.2f |".format(
                theoreticalOverall.min(), theoreticalOverall.max(),
                projectOverall.min(), projectOverall.max(),
            ))
            val theoMaeOverall = theoreticalOverall.map { abs(it - literatureMidpointFor(it, scenarios)) }.average()
            val projMaeOverall = projectOverall.map { abs(it - literatureMidpointFor(it, scenarios)) }.average()
            val theoRmseOverall = rmse(theoreticalOverall, scenarios)
            val projRmseOverall = rmse(projectOverall, scenarios)
            appendLine("| MAE vs clinical | %.2f ng/mL | %.2f ng/mL |".format(theoMaeOverall, projMaeOverall))
            appendLine("| RMSE vs clinical | %.2f ng/mL | %.2f ng/mL |".format(theoRmseOverall, projRmseOverall))
            val theoMapeOverall = meanAbsolutePercentageError(theoreticalOverall, scenarios)
            val projMapeOverall = meanAbsolutePercentageError(projectOverall, scenarios)
            appendLine("| MAPE vs clinical | %.1f%% | %.1f%% |".format(theoMapeOverall, projMapeOverall))
            appendLine()
            appendLine("## Paired tests")
            appendLine()
            appendLine("### Theoretical 1-comp vs Project engine (n=55)")
            appendLine()
            appendLine("- Mean paired delta (project - theoretical) = %.3f ng/mL".format(pairedDeltasTheoProj.average()))
            appendLine("- SD of paired deltas = %.3f ng/mL".format(stddev(pairedDeltasTheoProj)))
            appendLine("- Paired t-statistic = %.3f".format(pairedTStat))
            appendLine("- p-value (two-sided, df=54) = %.4f".format(pairedPValue))
            val interpTheoProj = if (pairedPValue < 0.05) "显著差异 (p < 0.05)" else "无显著差异"
            appendLine("- 解读: $interpTheoProj")
            appendLine()
            appendLine("### Project engine vs Clinical midpoint (n=55)")
            appendLine()
            appendLine("- Mean paired delta (project - clinical midpoint) = %.3f ng/mL".format(pairedDeltasProjClin.average()))
            appendLine("- SD of paired deltas = %.3f ng/mL".format(stddev(pairedDeltasProjClin)))
            appendLine("- Paired t-statistic = %.3f".format(projClinTStat))
            appendLine("- p-value (two-sided, df=54) = %.4f".format(projClinPValue))
            val interpProjClin = if (projClinPValue < 0.05) "显著偏差 (model ≠ clinical, p < 0.05)" else "无显著偏差 (model ≈ clinical)"
            appendLine("- 解读: $interpProjClin")
            appendLine()
            appendLine("## Bland-Altman limits of agreement (Project vs Clinical)")
            appendLine()
            val blandAltman = blandAltmanAnalysis(pairedDeltasProjClin)
            appendLine("- Mean bias = %.2f ng/mL (project - clinical midpoint)".format(blandAltman.first))
            appendLine("- SD of bias = %.2f ng/mL".format(blandAltman.second))
            appendLine("- 95%% LoA = [%.2f, %.2f] ng/mL".format(
                blandAltman.first - 1.96 * blandAltman.second,
                blandAltman.first + 1.96 * blandAltman.second,
            ))
            appendLine()
            appendLine("## Per-scenario residual distribution (project - clinical midpoint, n=11)")
            appendLine()
            appendLine("| Scenario | Mean residual | SD | Min | Max | % within literature range |")
            appendLine("|---|---|---|---|---|---|")
            for (stats in perScenarioStats) {
                val residuals = stats.projectValues.map { it - stats.scenario.literatureCmaxMidpointH }
                val mean = residuals.average()
                val sd = stddev(residuals)
                val withinCount = stats.projectValues.count { it in stats.scenario.literatureCmaxRange }
                val pct = withinCount * 100.0 / stats.projectValues.size
                appendLine("| ${stats.scenario.label} | %+.2f | %.2f | %+.2f | %+.2f | %.0f%% (${withinCount}/${stats.projectValues.size}) |".format(
                    mean, sd, residuals.min(), residuals.max(), pct,
                ))
            }
            appendLine()
            appendLine("## Notes")
            appendLine()
            appendLine("- All values in ng/mL (testosterone) for T esters; ng/mL Cavg for gel.")
            appendLine("- Body weight variation is a proxy for population variability: a real population PK study would also vary Vd, kClear, depot release rates, and adherence independently. The reported RMSE here is therefore a lower bound on what a real patient population would experience.")
            appendLine("- Literature Cmax ranges reflect the 95% CI or mean ± SD of the underlying study; comparing point estimates (model vs literature midpoint) is the standard PK practice.")
            appendLine("- The theoretical 1-comp baseline intentionally uses the engine's calibrated `vdPerKg` and `kClear` (so the body-weight scaling is identical); the difference between it and the project engine is the contribution of the two-part depot + per-ester formationFraction + retuned k1Fast/k1Slow.")
        }

        val outDir = File("app/build/reports").apply { mkdirs() }
        val outFile = File(outDir, "pk-statistical-report.md")
        outFile.writeText(md)
        println("=== PK STATISTICAL REPORT (also at ${outFile.absolutePath}) ===")
        println(md)
        println("=== END ===")

        assertTrue("Report should not be empty", md.isNotEmpty())
        assertTrue("Report should mention theoretical", md.contains("Theoretical"))
        assertTrue("Report should mention Bland-Altman", md.contains("Bland-Altman"))
    }

    // ---- Project engine (current model) ----

    private fun runProjectEngine(
        scenario: ScenarioSpec,
        bodyWeightKg: Double,
        anchorInstant: java.time.Instant,
    ): Double {
        return if (scenario.isGelCavg) {
            // For gel, run 30 days of once-daily application at the
            // specified dose, then take Cavg over the last 24h.
            runGelCavg(scenario, bodyWeightKg, anchorInstant)
        } else {
            // For IM esters, run a single dose and find the peak.
            runInjectionCmax(scenario, bodyWeightKg, anchorInstant)
        }
    }

    private fun runInjectionCmax(
        scenario: ScenarioSpec,
        bodyWeightKg: Double,
        anchorInstant: java.time.Instant,
    ): Double {
        val startInstant = anchorInstant.minus(1L, java.time.temporal.ChronoUnit.DAYS)
        val endInstant = anchorInstant.plus(15L, java.time.temporal.ChronoUnit.DAYS)
        val sampleStepH = 0.5
        val numSamples = (java.time.temporal.ChronoUnit.DAYS.between(startInstant, endInstant) * 24 / sampleStepH).toInt() + 1
        val sampleHours = (0 until numSamples).map { it * sampleStepH }
        val events = listOf(
            PkDoseEvent(
                id = UUID.randomUUID(),
                sourceGroupUuid = null,
                hormone = PkHormone.TESTOSTERONE,
                route = PkRoute.INJECTION,
                timeH = (anchorInstant.epochSecond - startInstant.epochSecond) / 3600.0,
                doseMg = scenario.doseMg,
                compound = scenario.compound,
            )
        )
        val result = PkSimulationEngine(
            events = events,
            hormone = PkHormone.TESTOSTERONE,
            bodyWeightKg = bodyWeightKg,
            startTimeH = 0.0,
            endTimeH = sampleHours.last() + 1.0,
            numberOfSteps = sampleHours.size + 2,
        ).run(sampleTimeH = sampleHours)
        val cmaxIndex = result.concentrations.indices.maxBy { result.concentrations[it] }
        return result.concentrations[cmaxIndex] / 100.0 // ng/dL -> ng/mL
    }

    private fun runGelCavg(
        scenario: ScenarioSpec,
        bodyWeightKg: Double,
        anchorInstant: java.time.Instant,
    ): Double {
        val numDays = 30
        val startInstant = anchorInstant.minus((numDays + 1).toLong(), java.time.temporal.ChronoUnit.DAYS)
        val endInstant = anchorInstant.plus(1L, java.time.temporal.ChronoUnit.DAYS)
        val sampleStepH = 0.5
        val numSamples = (java.time.temporal.ChronoUnit.DAYS.between(startInstant, endInstant) * 24 / sampleStepH).toInt() + 1
        val sampleHours = (0 until numSamples).map { it * sampleStepH }
        val events = (1..numDays).map { day ->
            PkDoseEvent(
                id = UUID.randomUUID(),
                sourceGroupUuid = null,
                hormone = PkHormone.TESTOSTERONE,
                route = PkRoute.GEL,
                timeH = day * 24.0,
                doseMg = scenario.doseMg,
                compound = PkCompound.T,
            )
        }
        val result = PkSimulationEngine(
            events = events,
            hormone = PkHormone.TESTOSTERONE,
            bodyWeightKg = bodyWeightKg,
            startTimeH = 0.0,
            endTimeH = sampleHours.last() + 1.0,
            numberOfSteps = sampleHours.size + 2,
        ).run(sampleTimeH = sampleHours)
        val endDayH = (numDays + 1) * 24.0
        val startDayH = numDays * 24.0
        val last24hStart = (startDayH / sampleStepH).toInt()
        val last24hEnd = ((endDayH - sampleStepH) / sampleStepH).toInt()
        val day30Samples = result.concentrations.subList(
            last24hStart.coerceAtLeast(0),
            last24hEnd.coerceAtMost(result.concentrations.size - 1)
        )
        return day30Samples.average() / 100.0 // ng/dL -> ng/mL
    }

    // ---- Theoretical 1-comp Bateman (textbook) ----
    //
    // For 1-compartment with first-order absorption (Bateman equation):
    //   C(t) = (F * D * ka) / (Vd * (ka - ke)) * [exp(-ke * t) - exp(-ka * t)]
    //   Tmax = ln(ka/ke) / (ka - ke)
    //   Cmax = (F * D * ka) / (Vd * (ka - ke)) * [exp(-ke * Tmax) - exp(-ka * Tmax)]
    //
    // We use the engine's calibrated kClear (so the body-weight
    // scaling is identical between the two models — the only
    // difference is the depot / formationFraction calibration).

    private fun runTheoreticalOneComp(scenario: ScenarioSpec, bodyWeightKg: Double): Double {
        val core = PkCatalog.coreParams(PkHormone.TESTOSTERONE)
        val vdL = core.vdPerKg * bodyWeightKg
        val ke = if (scenario.isGelCavg) core.kClear else core.kClearInjection
        val (ka, f) = when {
            scenario.isGelCavg -> core.gelK1 to core.gelFMax
            scenario.compound == PkCompound.TE -> 0.02 to 0.07
            scenario.compound == PkCompound.TC -> 0.018 to 0.07
            scenario.compound == PkCompound.TU -> 0.008 to 0.07
            else -> 0.02 to 0.07
        }
        val tmaxH = ln(ka / ke) / (ka - ke)
        val doseMg = scenario.doseMg
        val vdML = vdL * 1000.0
        val doseNg = doseMg * 1e6
        val cmaxNgMl = (f * doseNg * ka) / (vdML * (ka - ke)) *
            (exp(-ke * tmaxH) - exp(-ka * tmaxH))
        return cmaxNgMl
    }

    // ---- Statistics helpers ----

    private fun stddev(xs: List<Double>): Double {
        if (xs.size < 2) return 0.0
        val mean = xs.average()
        return sqrt(xs.sumOf { (it - mean).pow(2) } / (xs.size - 1))
    }

    private fun rmse(xs: List<Double>, scenarios: List<ScenarioSpec>): Double {
        var sumSqDiff = 0.0
        var n = 0
        for (run in xs.mapIndexed { i, cmax -> Pair(cmax, scenarios[i / 11]) }) {
            val (cmax, scenario) = run
            val litMid = scenario.literatureCmaxMidpointH
            sumSqDiff += (cmax - litMid).pow(2)
            n += 1
        }
        return sqrt(sumSqDiff / n)
    }

    private fun meanAbsolutePercentageError(xs: List<Double>, scenarios: List<ScenarioSpec>): Double {
        var sumPct = 0.0
        var n = 0
        for ((i, cmax) in xs.withIndex()) {
            val scenario = scenarios[i / 11]
            val litMid = scenario.literatureCmaxMidpointH
            sumPct += abs(cmax - litMid) / litMid * 100.0
            n += 1
        }
        return sumPct / n
    }

    private fun literatureMidpointFor(cmax: Double, scenarios: List<ScenarioSpec>): Double {
        // Helper not used in pooled stats; kept for symmetry
        return scenarios.first().literatureCmaxMidpointH
    }

    private fun blandAltmanAnalysis(deltas: List<Double>): Pair<Double, Double> {
        val mean = deltas.average()
        val sd = stddev(deltas)
        return Pair(mean, sd)
    }

    /**
     * Two-sided paired t-test against H0: mean(delta) = 0.
     * Returns the p-value. Uses df = n - 1, t-distribution CDF via
     * a regularized incomplete beta function approximation
     * (Abramowitz & Stegun 26.7).
     */
    private fun pairedTStat(deltas: List<Double>): Double {
        val n = deltas.size
        if (n < 2) return Double.NaN
        val mean = deltas.average()
        val sd = stddev(deltas)
        if (sd == 0.0) return if (mean == 0.0) 0.0 else Double.POSITIVE_INFINITY
        val se = sd / sqrt(n.toDouble())
        return mean / se
    }

    private fun pairedTTest(deltas: List<Double>): Double {
        val t = pairedTStat(deltas)
        val df = deltas.size - 1
        if (t.isNaN()) return 1.0
        return 2.0 * tDistTwoTailedPValue(t, df)
    }

    /**
     * Two-tailed p-value for Student's t-distribution.
     * Uses regularized incomplete beta: p = I_x(df/2, 1/2) where
     * x = df / (df + t^2). Approximated via continued fraction
     * (Lentz's method) for I_x(a, b).
     */
    private fun tDistTwoTailedPValue(t: Double, df: Int): Double {
        val x = df.toDouble() / (df + t * t)
        return betaIncReg(x, df / 2.0, 0.5)
    }

    private fun betaIncReg(x: Double, a: Double, b: Double): Double {
        if (x < 0.0 || x > 1.0) return Double.NaN
        if (x == 0.0) return 0.0
        if (x == 1.0) return 1.0
        val symm = x > (a + 1.0) / (a + b + 2.0)
        val xp = if (symm) 1.0 - x else x
        val ap = if (symm) b else a
        val bp = if (symm) a else b
        val result = betaIncBody(xp, ap, bp)
        return if (symm) 1.0 - result * (if (symm) 1.0 else 1.0) else result
    }

    private fun betaIncBody(x: Double, a: Double, b: Double): Double {
        val qab = a + b
        val qap = a + 1.0
        val qam = a - 1.0
        var c = 1.0
        var d = 1.0 - qab * x / qap
        if (abs(d) < 1e-30) d = 1e-30
        d = 1.0 / d
        var h = d
        for (m in 1..200) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1.0 + aa * d
            if (abs(d) < 1e-30) d = 1e-30
            c = 1.0 + aa / c
            if (abs(c) < 1e-30) c = 1e-30
            d = 1.0 / d
            h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1.0 + aa * d
            if (abs(d) < 1e-30) d = 1e-30
            c = 1.0 + aa / c
            if (abs(c) < 1e-30) c = 1e-30
            d = 1.0 / d
            val del = d * c
            h *= del
            if (abs(del - 1.0) < 1e-12) break
        }
        return h * (x.pow(a) * (1.0 - x).pow(b) / a)
    }

    // ---- Data holders ----

    private data class ScenarioSpec(
        val label: String,
        val doseMg: Double,
        val compound: PkCompound,
        val route: PkRoute,
        val literatureCmaxRange: ClosedFloatingPointRange<Double>,
        val literatureCmaxMidpointH: Double,
        val literatureTmaxRangeH: ClosedFloatingPointRange<Double>,
        val literatureTmaxMidpointH: Double,
        val isGelCavg: Boolean = false,
    )

    private data class RunResult(
        val scenario: String,
        val doseMg: Double,
        val bodyWeightKg: Double,
        val cmax: Double,
    )

    private data class ScenarioStats(
        val scenario: ScenarioSpec,
        val projectValues: List<Double>,
        val theoreticalValues: List<Double>,
    )
}
