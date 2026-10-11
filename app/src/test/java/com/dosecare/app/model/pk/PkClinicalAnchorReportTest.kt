package com.dosecare.app.model.pk

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * One-shot PK validation report. Runs the testosterone engine
 * against the documented clinical anchors (Behre 1986, Slayback
 * FDA label, Nebido SmPC, AndroGel FDA label) and prints a
 * markdown report with Cmax / Tmax / Cavg + per-scenario error
 * % vs literature.
 *
 * Invoke via:
 *   ./gradlew :app:testPlayDebugUnitTest \
 *     --tests "com.dosecare.app.model.pk.PkClinicalAnchorReportTest"
 *
 * Writes the report to
 * `app/build/reports/pk-clinical-anchor-report.md` and prints it
 * to stdout. Assertions are loose so the test always passes
 * (any reasonable engine config produces non-empty output);
 * the value is the report, not the pass/fail.
 *
 * Bypasses MedicationLogEntry / DoseInstruction entirely:
 * constructs PkDoseEvent directly. The MedicationLog → engine
 * bridge is already covered by the F-PR2 catalog-key tests in
 * PkSimulationTest; this report isolates the engine math
 * behaviour.
 */
class PkClinicalAnchorReportTest {
    @Test
    fun print_t_pk_clinical_anchor_report() {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.of(2026, 9, 4, 10, 0)
        val nowInstant = now.atZone(zoneId).toInstant()
        val bodyWeightKg = 70.0

        val teReport = runInjectionSingleDose(
            compound = PkCompound.TE, doseMg = 200.0,
            anchorInstant = nowInstant,
            bodyWeightKg = bodyWeightKg,
        )
        val tcReport = runInjectionSingleDose(
            compound = PkCompound.TC, doseMg = 200.0,
            anchorInstant = nowInstant,
            bodyWeightKg = bodyWeightKg,
        )
        val tuReport = runInjectionSingleDose(
            compound = PkCompound.TU, doseMg = 1000.0,
            anchorInstant = nowInstant,
            bodyWeightKg = bodyWeightKg,
        )
        val gel5 = runGelSteadyState(
            doseMgPerDay = 50.0, numDays = 30,
            anchorInstant = nowInstant,
            bodyWeightKg = bodyWeightKg,
        )
        val gel10 = runGelSteadyState(
            doseMgPerDay = 100.0, numDays = 30,
            anchorInstant = nowInstant,
            bodyWeightKg = bodyWeightKg,
        )

        val md = buildString {
            appendLine("# PK validation: testosterone model vs clinical anchors")
            appendLine()
            appendLine("- Engine: `PkSimulationEngine` (1-comp Bateman with two-part depot for esters)")
            appendLine("- Body weight: 70 kg (typical androgen-trial reference)")
            appendLine("- Concentration unit: ng/mL (T), ng/dL internally")
            appendLine("- Sampling: 30 min resolution over 15 days (IM) or 31 days (gel)")
            appendLine()
            appendLine("## Single-dose IM testosterone esters")
            appendLine()
            appendLine("| Scenario | Model Cmax | Model Tmax | Literature Cmax | Literature Tmax | Cmax Δ | Tmax Δ |")
            appendLine("|---|---|---|---|---|---|---|")
            appendImRow("TE 200 mg IM", teReport, litCmaxTe, litTmaxTeH)
            appendImRow("TC 200 mg IM", tcReport, litCmaxTc, litTmaxTcH)
            appendImRow("TU 1000 mg IM (Nebido)", tuReport, litCmaxTu, litTmaxTuH)
            appendLine()
            appendLine("## Transdermal testosterone gel (30-day Cavg)")
            appendLine()
            appendLine("| Scenario | Model Cavg | Literature Cavg | Δ |")
            appendLine("|---|---|---|---|")
            appendGelRow("AndroGel 1% 5 g/day (50 mg T)", gel5, litGel5)
            appendGelRow("AndroGel 1% 10 g/day (100 mg T)", gel10, litGel10)
            appendLine()
            appendLine("## Notes")
            appendLine()
            appendLine("- Literature values come from `docs/pk-differences.md` (Behre 1986 / Slayback FDA label / Nebido SmPC / AndroGel FDA label).")
            appendLine("- Cmax Δ = (model - literature midpoint) / literature midpoint.")
            appendLine("- Tmax Δ = model - literature midpoint (hours).")
            appendLine("- The model uses a 1-comp Bateman with a single central elimination constant; the literature curves come from 2-3 comp PK models with depot-specific release kinetics, so 10-40% Cmax error is expected.")
            appendLine("- For transdermal gel, the SPC-reported Cavg is the 24-h AUC average. The 1-comp model reports the steady-state plateau Cmax within a dosing interval, which equals Cavg (AUC/Cmax ratio = 1 for 1-comp at plateau).")
        }

        val outDir = File("app/build/reports").apply { mkdirs() }
        val outFile = File(outDir, "pk-clinical-anchor-report.md")
        outFile.writeText(md)
        println("=== PK VALIDATION REPORT (also at ${outFile.absolutePath}) ===")
        println(md)
        println("=== END ===")

        assertTrue("Report should not be empty", md.isNotEmpty())
        assertTrue("Report should mention TE", md.contains("TE 200 mg IM"))
        assertTrue("Report should mention gel", md.contains("AndroGel 1% 5 g/day"))
    }

    private fun StringBuilder.appendImRow(
        label: String,
        result: TestosteroneSingleDoseResult,
        litCmax: ClosedFloatingPointRange<Double>,
        litTmaxH: ClosedFloatingPointRange<Double>,
    ) {
        val cmaxMid = (litCmax.start + litCmax.endInclusive) / 2.0
        val cmaxPct = (result.cmaxNgMl - cmaxMid) / cmaxMid * 100.0
        val cmaxStr = String.format(java.util.Locale.ROOT, "%+.1f%%", cmaxPct)
        val tmaxMid = (litTmaxH.start + litTmaxH.endInclusive) / 2.0
        val tmaxDeltaH = result.tmaxH - tmaxMid
        val tmaxStr = String.format(java.util.Locale.ROOT, "%+.0f h", tmaxDeltaH)
        val cmaxText = String.format(java.util.Locale.ROOT, "%.2f ng/mL", result.cmaxNgMl)
        val tmaxText = String.format(java.util.Locale.ROOT, "%.0f h", result.tmaxH)
        val litCmaxText = String.format(
            java.util.Locale.ROOT,
            "%.2f - %.2f ng/mL",
            litCmax.start, litCmax.endInclusive,
        )
        val litTmaxText = String.format(
            java.util.Locale.ROOT,
            "%s - %s d",
            (litTmaxH.start / 24.0).toString(),
            (litTmaxH.endInclusive / 24.0).toString(),
        )
        appendLine("| $label | $cmaxText | $tmaxText | $litCmaxText | $litTmaxText | $cmaxStr | $tmaxStr |")
    }

    private fun StringBuilder.appendGelRow(
        label: String,
        result: TestosteroneGelResult,
        litCavg: ClosedFloatingPointRange<Double>,
    ) {
        val cavgMid = (litCavg.start + litCavg.endInclusive) / 2.0
        val cavgDelta = (result.cavgNgMl - cavgMid) / cavgMid * 100.0
        val cavgStr = String.format(java.util.Locale.ROOT, "%+.1f%%", cavgDelta)
        val cavgText = String.format(java.util.Locale.ROOT, "%.2f ng/mL", result.cavgNgMl)
        val litCavgText = String.format(
            java.util.Locale.ROOT,
            "%.2f - %.2f ng/mL",
            litCavg.start, litCavg.endInclusive,
        )
        appendLine("| $label | $cavgText | $litCavgText | $cavgStr |")
    }

    // ---- engine runner ----

    private fun runInjectionSingleDose(
        compound: PkCompound,
        doseMg: Double,
        anchorInstant: Instant,
        bodyWeightKg: Double,
    ): TestosteroneSingleDoseResult {
        val startInstant = anchorInstant.minus(1L, ChronoUnit.DAYS)
        val endInstant = anchorInstant.plus(15L, ChronoUnit.DAYS)
        val sampleStepH = 0.5
        val numSamples = (ChronoUnit.DAYS.between(startInstant, endInstant) * 24 / sampleStepH).toInt() + 1
        val sampleHours = (0 until numSamples).map { it * sampleStepH }
        val events = listOf(
            PkDoseEvent(
                id = UUID.randomUUID(),
                sourceGroupUuid = null,
                hormone = PkHormone.TESTOSTERONE,
                route = PkRoute.INJECTION,
                timeH = (anchorInstant.epochSecond - startInstant.epochSecond) / 3600.0,
                doseMg = doseMg,
                compound = compound,
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
        val cmaxNgMl = result.concentrations[cmaxIndex] / 100.0
        val tmaxH = result.timeH[cmaxIndex]
        return TestosteroneSingleDoseResult(cmaxNgMl = cmaxNgMl, tmaxH = tmaxH)
    }

    private fun runGelSteadyState(
        doseMgPerDay: Double,
        numDays: Int,
        anchorInstant: Instant,
        bodyWeightKg: Double,
    ): TestosteroneGelResult {
        // Place the events at hours 24, 48, ..., numDays*24 so the
        // first dose lands at hour 24 (one day into the sample
        // window) and the last dose lands at hour numDays*24 (i.e.
        // one day before the Cavg sampling window starts). This
        // gives the gel enough time to reach plateau before we
        // sample the steady-state Cavg.
        val startInstant = anchorInstant.minus(numDays.toLong(), ChronoUnit.DAYS)
        val endInstant = anchorInstant.plus(2L, ChronoUnit.DAYS)
        val sampleStepH = 0.5
        val numSamples = (ChronoUnit.DAYS.between(startInstant, endInstant) * 24 / sampleStepH).toInt() + 1
        val sampleHours = (0 until numSamples).map { it * sampleStepH }
        val events = (1..numDays).map { day ->
            PkDoseEvent(
                id = UUID.randomUUID(),
                sourceGroupUuid = null,
                hormone = PkHormone.TESTOSTERONE,
                route = PkRoute.GEL,
                timeH = day * 24.0,
                doseMg = doseMgPerDay,
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
        // Cavg over the last 24h of the window (steady-state
        // plateau right before the final dose at the very end of
        // the day). The last 24h = samples at (numDays+1)*24 to
        // (numDays+2)*24 - sampleStepH.
        val endDayH = (numDays + 1) * 24.0
        val startDayH = numDays * 24.0
        val last24hStart = (startDayH / sampleStepH).toInt()
        val last24hEnd = ((endDayH - sampleStepH) / sampleStepH).toInt()
        val day30Samples = result.concentrations.subList(last24hStart.coerceAtLeast(0), last24hEnd.coerceAtMost(result.concentrations.size - 1))
        val cavgNgMl = day30Samples.average() / 100.0
        return TestosteroneGelResult(cavgNgMl = cavgNgMl)
    }

    private data class TestosteroneSingleDoseResult(
        val cmaxNgMl: Double,
        val tmaxH: Double,
    )

    private data class TestosteroneGelResult(
        val cavgNgMl: Double,
    )

    // ---- clinical anchors (from docs/pk-differences.md) ----

    // TE 200 mg: literature 1080 ng/dL @ 48-72h (scaled from Behre 1986 250 mg)
    private val litCmaxTe = 9.0..12.5
    private val litTmaxTeH = 48.0..72.0

    // TC 200 mg: literature 758 ± 289 ng/dL @ median 71.7h (Slayback FDA)
    private val litCmaxTc = 4.7..10.5
    private val litTmaxTcH = 60.0..84.0

    // TU 1000 mg (Nebido): literature 11-15 ng/mL @ 7d (SmPC)
    private val litCmaxTu = 11.0..15.0
    private val litTmaxTuH = 144.0..192.0

    // AndroGel 5 g/day: literature 5.66 ± 2.62 ng/mL Cavg
    private val litGel5 = 3.04..8.28

    // AndroGel 10 g/day: literature 7.92 ± 2.94 ng/mL Cavg
    private val litGel10 = 4.98..10.86
}
