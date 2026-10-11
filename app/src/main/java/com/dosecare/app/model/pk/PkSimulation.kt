package com.dosecare.app.model.pk

import com.dosecare.app.model.medication.DoseInstruction
import com.dosecare.app.model.medication.DoseInstructionCalculator
import com.dosecare.app.model.medication.MedicationApplicationType
import com.dosecare.app.model.medication.MedicationCategory
import com.dosecare.app.model.medication.MedicationKey
import com.dosecare.app.model.medication.MedicationLogEntry
import com.dosecare.app.model.medication.Medicine
import com.dosecare.app.model.medication.MedicinePreparation
import com.dosecare.app.model.medication.MedicineSelection
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToLong

enum class PkHormone {
    ESTRADIOL,
    TESTOSTERONE,
    // Cyproterone acetate (CPA) is the active drug itself; unlike the E2
    // esters it is not a pro-drug that needs enzymatic hydrolysis, so the
    // PkCatalog only needs an oral path today. The kClear target is
    // ln(2) / 54h ≈ 0.0128 (Kuhnz 1993 / Diane-35 SPC), at vdPerKg ≈ 14
    // L/kg (Vd=986L / 70kg, Kuhnz 1993). Sources: docs/pk-differences.md.
    CYPROTERONE_ACETATE,
}

enum class PkRoute {
    INJECTION,
    PATCH_APPLY,
    PATCH_REMOVE,
    GEL,
    ORAL,
    SUBLINGUAL,
}

enum class PkCompound {
    E2,
    EB,
    EV,
    EC,
    EN,
    EU,
    T,
    TC,
    TE,
    TU,
    // Cyproterone acetate: molecular weight 416.94 g/mol (cyproterone +
    // acetate ester). It is the active moiety — no active-precursor
    // distinction, so activeMolecularWeight equals molecularWeight.
    CPA,
}

enum class PkConcentrationUnit(val symbol: String) {
    PG_PER_ML("pg/mL"),
    PMOL_PER_L("pmol/L"),
    NG_PER_DL("ng/dL"),
    NG_PER_ML("ng/mL"),
    NMOL_PER_L("nmol/L");

    fun concentrationScale(hormone: PkHormone): Double {
        return when (this) {
            PG_PER_ML -> 1e9
            PMOL_PER_L -> 1e12 / PkCatalog.molecularWeight(hormone)
            NG_PER_DL -> 1e8
            NG_PER_ML -> 1e6
            NMOL_PER_L -> 1e9 / PkCatalog.molecularWeight(hormone)
        }
    }
}

data class PkDoseEvent(
    val id: UUID,
    val sourceGroupUuid: UUID?,
    val hormone: PkHormone,
    val route: PkRoute,
    val timeH: Double,
    val doseMg: Double,
    val compound: PkCompound,
    val releaseRateMcgPerDay: Double? = null,
    val areaCm2: Double? = null,
    val sublingualTheta: Double? = null,
    // True when this event came from a synthesized future-schedule slot rather
    // than a user-logged entry. Drives the dose marker color in the UI.
    val isPlanned: Boolean = false,
)

/**
 * PR-user-override: per-user parameter overrides for the testosterone
 * PK model. All fields are optional — null means "use the engine's
 * catalog default". The current scope is intentionally narrow:
 * only the two parameters that most affect a T trend's shape for
 * a single user, kClearInjection (central clearance, affects
 * peak height and early decline) and threeCompK31 (deep peripheral
 * release rate, affects the long terminal t½ in the 3-comp
 * engine for TU).
 *
 * Future scope: k12 / k21 / k13 to let advanced users shift the
 * shallow/deep peripheral partitioning if their T data clearly
 * disagrees with the calibrated ratios. Out of scope for v1.
 *
 * These overrides are NOT propagated through the 1-comp T ester
 * path (TE / TC) — those already fit the literature well, and a
 * 1-comp has no k31 knob. The override is consumed by the 3-comp
 * RK4 path only.
 */
data class PkTUserOverride(
    val kClearInjection: Double? = null,
    val threeCompK31: Double? = null,
)

data class PkParams(
    val fracFast: Double,
    val k1Fast: Double,
    val k1Slow: Double,
    val k2: Double,
    val k3: Double,
    val bioavailability: Double,
    val rateMgH: Double,
    val fastBioavailability: Double,
    val slowBioavailability: Double,
)

/**
 * v3 血检追踪 (PR-V3-TU): a snapshot of the 3-compartment
 * distribution state at a given RK4 step. All values are mass
 * (mg). `central` is what becomes the observed concentration
 * (central / Vd_central); `shallow` and `deep` are the two
 * peripheral compartments (e.g., muscle and fat for TU) that
 * act as drug reservoirs.
 */
private data class ThreeCompState(
    val central: Double,
    val shallow: Double,
    val deep: Double,
)

/**
 * v3 血检追踪 (PR-V3-TU): sums of the 3-comp distribution
 * parameters across all events in a run that map to a 3-comp
 * compound. Today only TU has the override, so the fold is
 * degenerate; the struct keeps the door open for a second
 * compound without restructuring.
 */
private data class ThreeCompAggregate(
    val centralVolumeMl: Double,
    val shallowVolumeMl: Double,
    val deepVolumeMl: Double,
    val k12: Double,
    val k21: Double,
    val k13: Double,
    val k31: Double,
) {
    companion object {
        val ZERO = ThreeCompAggregate(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

data class PkSimulationMetadata(
    val hormone: PkHormone,
    val concentrationUnit: PkConcentrationUnit,
)

data class PkSimulationResult(
    val timeH: List<Double>,
    val concentrations: List<Double>,
    val auc: Double,
    val metadata: PkSimulationMetadata,
) {
    fun concentrationAt(hour: Double): Double? {
        if (timeH.isEmpty() || timeH.size != concentrations.size) {
            return null
        }
        if (hour <= timeH.first()) {
            return concentrations.first()
        }
        if (hour >= timeH.last()) {
            return concentrations.last()
        }

        var low = 0
        var high = timeH.lastIndex
        while (high - low > 1) {
            val mid = (low + high) / 2
            when {
                timeH[mid] == hour -> return concentrations[mid]
                timeH[mid] < hour -> low = mid
                else -> high = mid
            }
        }

        val t0 = timeH[low]
        val t1 = timeH[high]
        val c0 = concentrations[low]
        val c1 = concentrations[high]
        if (t1 <= t0) {
            return c0
        }
        val ratio = (hour - t0) / (t1 - t0)
        return c0 + (c1 - c0) * ratio
    }
}

data class PkDoseMarker(
    val timeH: Double,
    val concentration: Double,
    val isPlanned: Boolean = false,
)

data class PkTrendResult(
    val currentConcentration: Double,
    val previousDayConcentration: Double,
    val dailyConcentrations: List<Double>,
    val concentrationUnit: PkConcentrationUnit,
    val chartConcentrations: List<Double> = dailyConcentrations,
    val chartSampleIntervalHours: Int = 24,
    val chartTimeH: List<Double> = chartConcentrations.indices.map { index ->
        index * chartSampleIntervalHours.toDouble()
    },
    val doseMarkers: List<PkDoseMarker> = emptyList(),
    val chartWindowHours: Int = chartSampleIntervalHours * (chartConcentrations.size - 1).coerceAtLeast(
        0
    ),
    val predictionStartTimeH: Double = chartWindowHours.toDouble(),
    // PR3-UI-3comp: when the engine used the 3-compartment RK4
    // engine (today: testosterone undecanoate Nebido), the
    // underlying simulation has a deep-peripheral fat reservoir
    // that produces a 90-day terminal tail the 1-comp engine
    // cannot represent. The chart surfaces this as a small "3-comp
    // model" badge so the user knows the curve has the long-tail
    // fidelity (and so it isn't confused with the 1-comp curves
    // for TE / TC). False for E2, CPA, and T ester/1-comp
    // gel (only TU 3-comp triggers it).
    val usesThreeCompModel: Boolean = false,
) {
    val changeSincePreviousDay: Double
        get() = currentConcentration - previousDayConcentration
}

data class PkProjectionResult(
    val generatedAt: Instant,
    val windowStart: Instant,
    val windowEnd: Instant,
    val concentrationUnit: PkConcentrationUnit,
    val timeH: List<Double>,
    val concentrations: List<Double>,
    val doseMarkers: List<PkDoseMarker>,
) {
    fun toMainEstradiolTrend(
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        option: HomeE2ChartWindowOption = HomeE2ChartWindowOption.SEVEN_DAYS,
    ): PkTrendResult? {
        if (timeH.isEmpty() || timeH.size != concentrations.size) {
            return null
        }

        val chartWindowStart = now
            .toLocalDate()
            .atStartOfDay()
            .minusDays(option.pastDays)
            .atZone(zoneId)
            .toInstant()
        val chartWindowEnd = chartWindowStart.plus(
            Duration.ofHours(option.chartWindowHours)
        )
        if (chartWindowStart.isBefore(windowStart) || chartWindowEnd.isAfter(windowEnd)) {
            return null
        }

        val chartWindowStartH = hoursFromWindowStart(chartWindowStart)
        val chartVisibleEndH = chartWindowStartH + pkChartVisibleEndHours(
            option.chartWindowHours.toDouble()
        )
        val predictionStartTimeH = hoursBetween(chartWindowStart, now.atZone(zoneId).toInstant())
            .toProjectionChartXHour()
        val currentTimeH = chartWindowStartH + predictionStartTimeH
        val previousDayTimeH = currentTimeH - PkMedicationSimulation.hoursPerDay

        val chartTimeH = (
                timeH.filter { time -> time in chartWindowStartH..chartVisibleEndH } +
                        listOf(chartWindowStartH, chartVisibleEndH, currentTimeH, previousDayTimeH)
                )
            .filter { time -> time.isFinite() && time in chartWindowStartH..chartVisibleEndH }
            .map { time -> time.toProjectionChartXHour() }
            .distinct()
            .sorted()
        val chartConcentrations = chartTimeH.map { time -> concentrationAt(time) ?: 0.0 }
        val dailyConcentrations = (0..option.chartWindowDays.toInt()).map { index ->
            concentrationAt(chartWindowStartH + index * PkMedicationSimulation.hoursPerDay) ?: 0.0
        }

        return PkTrendResult(
            currentConcentration = concentrationAt(currentTimeH) ?: 0.0,
            previousDayConcentration = concentrationAt(previousDayTimeH) ?: 0.0,
            dailyConcentrations = dailyConcentrations,
            concentrationUnit = concentrationUnit,
            chartConcentrations = chartConcentrations,
            chartSampleIntervalHours = PkMedicationSimulation.mainChartSampleIntervalHours,
            chartTimeH = chartTimeH.map { time -> (time - chartWindowStartH).toProjectionChartXHour() },
            doseMarkers = doseMarkers
                .filter { marker -> marker.timeH in chartWindowStartH..chartVisibleEndH }
                .map { marker ->
                    marker.copy(timeH = (marker.timeH - chartWindowStartH).toProjectionChartXHour())
                },
            chartWindowHours = option.chartWindowHours.toInt(),
            predictionStartTimeH = predictionStartTimeH,
        )
    }

    private fun hoursFromWindowStart(instant: Instant): Double {
        return hoursBetween(windowStart, instant)
    }

    private fun hoursBetween(start: Instant, end: Instant): Double {
        return Duration.between(start, end).toMillis() / 3_600_000.0
    }

    private fun concentrationAt(hour: Double): Double? {
        if (timeH.isEmpty() || timeH.size != concentrations.size) {
            return null
        }
        if (hour <= timeH.first()) {
            return concentrations.first()
        }
        if (hour >= timeH.last()) {
            return concentrations.last()
        }

        var low = 0
        var high = timeH.lastIndex
        while (high - low > 1) {
            val mid = (low + high) / 2
            when {
                timeH[mid] == hour -> return concentrations[mid]
                timeH[mid] < hour -> low = mid
                else -> high = mid
            }
        }

        val t0 = timeH[low]
        val t1 = timeH[high]
        val c0 = concentrations[low]
        val c1 = concentrations[high]
        if (t1 <= t0) {
            return c0
        }
        val ratio = (hour - t0) / (t1 - t0)
        return c0 + (c1 - c0) * ratio
    }
}

private fun Double.toProjectionChartXHour(): Double {
    return (this * PkProjectionChartXPrecisionScale).roundToLong() / PkProjectionChartXPrecisionScale
}

private fun pkChartVisibleEndHours(windowEndHours: Double): Double {
    // The 7-day chart's midnight end is exclusive; render the last minute
    // inside the window so the UI doesn't expose the following day at 00:00.
    return (windowEndHours - PkChartEndInsetHours).coerceAtLeast(0.0).toProjectionChartXHour()
}

private const val PkProjectionChartXPrecisionScale = 10_000.0
private const val PkChartEndInsetHours = 1.0 / 60.0

// Post-dose offsets injected by the sampler when the selected
// HomeE2ChartWindowOption sets includesPostDoseOffsets = true. At the budget
// sampler's grid spacing (~26 min for THIRTY_DAYS) the absorption-phase Cmax
// of fast-Tmax routes (oral, sublingual) lands between grid samples; without
// these offsets the chart polyline would underplot the peak.
private val MainChartPostDoseSampleOffsetsHours =
    listOf(0.25, 0.5, 1.0, 2.0, 4.0, 6.0, 8.0, 12.0, 24.0, 48.0)

object PkMedicationSimulation {
    private const val HoursPerDay = 24.0
    private const val MainChartWindowDays = 7L
    private const val MainChartPastDays = 3L
    private const val MainChartSampleIntervalHours = 1
    private const val MainChartWindowHours = MainChartWindowDays * 24
    private const val MainChartFallbackSteps = MainChartWindowHours + 1
    private const val MainChartDenseSampleIntervalHours = 0.1
    private const val DefaultBodyWeightKg = 70.0
    private const val ChartXPrecisionScale = 10_000.0

    internal const val hoursPerDay = HoursPerDay
    internal const val mainChartWindowDays = MainChartWindowDays
    internal const val mainChartPastDays = MainChartPastDays
    internal const val mainChartSampleIntervalHours = MainChartSampleIntervalHours
    internal const val mainChartWindowHours = MainChartWindowHours

    fun simulateMainEstradiolTrend(
        entries: List<MedicationLogEntry>,
        bodyWeightKg: Double?,
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        plannedEntries: List<MedicationLogEntry> = emptyList(),
        option: HomeE2ChartWindowOption = HomeE2ChartWindowOption.SEVEN_DAYS,
    ): PkTrendResult? {
        val resolvedBodyWeightKg =
            bodyWeightKg?.takeIf { it.isFinite() && it > 0 } ?: DefaultBodyWeightKg
        val nowInstant = now.atZone(zoneId).toInstant()
        val startInstant = now
            .toLocalDate()
            .atStartOfDay()
            .minusDays(option.pastDays)
            .atZone(zoneId)
            .toInstant()
        val predictionStartTimeH = Duration.between(startInstant, nowInstant)
            .toMillis() / 3_600_000.0
        val events = buildEstradiolEvents(
            anchor = startInstant,
            realEntries = entries,
            plannedEntries = plannedEntries,
        )
        val windowEndHours = option.chartWindowHours.toDouble()
        val chartVisibleEndHours = pkChartVisibleEndHours(windowEndHours)
        val chartSampleTimeH = mainChartSampleTimeH(
            events = events,
            predictionStartTimeH = predictionStartTimeH,
            windowEndHours = windowEndHours,
            chartVisibleEndHours = chartVisibleEndHours,
            option = option,
        )

        val result = PkSimulationEngine(
            events = events,
            hormone = PkHormone.ESTRADIOL,
            bodyWeightKg = resolvedBodyWeightKg,
            startTimeH = 0.0,
            endTimeH = windowEndHours,
            numberOfSteps = (option.chartWindowHours.toInt() + 1).coerceAtLeast(2),
        ).run(sampleTimeH = chartSampleTimeH)

        val dailyConcentrations = (0..option.chartWindowDays.toInt()).map { index ->
            result.concentrationAt(index * HoursPerDay) ?: 0.0
        }
        val chartSamples = result.timeH
            .zip(result.concentrations)
            .filter { (timeH, _) -> timeH in 0.0..chartVisibleEndHours }
        val doseMarkers = buildDoseMarkers(
            events = events,
            windowEndHours = chartVisibleEndHours,
            concentrationAt = result::concentrationAt,
        )
        return PkTrendResult(
            currentConcentration = result.concentrationAt(predictionStartTimeH) ?: 0.0,
            previousDayConcentration = result.concentrationAt(predictionStartTimeH - HoursPerDay)
                ?: 0.0,
            dailyConcentrations = dailyConcentrations,
            concentrationUnit = result.metadata.concentrationUnit,
            chartConcentrations = chartSamples.map { (_, concentration) -> concentration },
            chartSampleIntervalHours = MainChartSampleIntervalHours,
            chartTimeH = chartSamples.map { (timeH, _) -> timeH },
            doseMarkers = doseMarkers,
            chartWindowHours = option.chartWindowHours.toInt(),
            predictionStartTimeH = predictionStartTimeH.toChartXHour(),
        )
    }

    // PR3: same shape as simulateMainEstradiolTrend but for the
    // testosterone hormone path. The engine uses the same one-comp
    // two-part depot model, so the implementation is structurally
    // identical — the only difference is hormone=TESTOSTERONE and
    // the per-hormone compound catalogue (TE/TC/TU/T + patches/gel
    // for testosterone, none of which have snapshot-projection
    // plumbing today). The caller is responsible for filtering the
    // medication log entries down to testosterone-bearing records
    // before passing them in; the function itself does not filter
    // by category so the caller can keep tight control over the
    // input set.
    fun simulateMainTestosteroneTrend(
        entries: List<MedicationLogEntry>,
        bodyWeightKg: Double?,
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        plannedEntries: List<MedicationLogEntry> = emptyList(),
        option: HomeE2ChartWindowOption = HomeE2ChartWindowOption.SEVEN_DAYS,
        // PR-user-override: optional per-user parameter overrides
        // (currently: kClearInjection for central clearance, k31
        // for the 3-comp deep peripheral release). When null/empty
        // the engine uses the catalog defaults. Threaded through
        // to the underlying PkSimulationEngine.
        userOverride: PkTUserOverride = PkTUserOverride(),
    ): PkTrendResult? {
        val resolvedBodyWeightKg =
            bodyWeightKg?.takeIf { it.isFinite() && it > 0 } ?: DefaultBodyWeightKg
        val nowInstant = now.atZone(zoneId).toInstant()
        val startInstant = now
            .toLocalDate()
            .atStartOfDay()
            .minusDays(option.pastDays)
            .atZone(zoneId)
            .toInstant()
        val predictionStartTimeH = Duration.between(startInstant, nowInstant)
            .toMillis() / 3_600_000.0
        val events = buildTestosteroneEvents(
            anchor = startInstant,
            realEntries = entries,
            plannedEntries = plannedEntries,
        )
        val windowEndHours = option.chartWindowHours.toDouble()
        val chartVisibleEndHours = pkChartVisibleEndHours(windowEndHours)
        val chartSampleTimeH = mainChartSampleTimeH(
            events = events,
            predictionStartTimeH = predictionStartTimeH,
            windowEndHours = windowEndHours,
            chartVisibleEndHours = chartVisibleEndHours,
            option = option,
        )

        val result = PkSimulationEngine(
            events = events,
            hormone = PkHormone.TESTOSTERONE,
            bodyWeightKg = resolvedBodyWeightKg,
            startTimeH = 0.0,
            endTimeH = windowEndHours,
            numberOfSteps = (option.chartWindowHours.toInt() + 1).coerceAtLeast(2),
            userOverride = userOverride,
        ).run(sampleTimeH = chartSampleTimeH)

        val dailyConcentrations = (0..option.chartWindowDays.toInt()).map { index ->
            result.concentrationAt(index * HoursPerDay) ?: 0.0
        }
        val chartSamples = result.timeH
            .zip(result.concentrations)
            .filter { (timeH, _) -> timeH in 0.0..chartVisibleEndHours }
        val doseMarkers = buildDoseMarkers(
            events = events,
            windowEndHours = chartVisibleEndHours,
            concentrationAt = result::concentrationAt,
        )
        return PkTrendResult(
            currentConcentration = result.concentrationAt(predictionStartTimeH) ?: 0.0,
            previousDayConcentration = result.concentrationAt(predictionStartTimeH - HoursPerDay)
                ?: 0.0,
            dailyConcentrations = dailyConcentrations,
            concentrationUnit = result.metadata.concentrationUnit,
            chartConcentrations = chartSamples.map { (_, concentration) -> concentration },
            chartSampleIntervalHours = MainChartSampleIntervalHours,
            chartTimeH = chartSamples.map { (timeH, _) -> timeH },
            doseMarkers = doseMarkers,
            chartWindowHours = option.chartWindowHours.toInt(),
            predictionStartTimeH = predictionStartTimeH.toChartXHour(),
            // PR3-UI-3comp: surface the 3-comp flag so the home chart
            // can badge the curve when the engine used the 3-comp
            // RK4 path (today: TU 3-comp with deep peripheral).
            // Computed from the resolved events so the flag is
            // exactly aligned with the actual integration path
            // the engine took, not just the user's input.
            usesThreeCompModel = events.any { event ->
                PkCatalog.threeCompOverride(event.compound) != null
            },
        )
    }

    // PR4: cyproterone acetate trend, mirroring simulateMainTestosteroneTrend
    // structurally. The oral Bateman path is the only one that has clinical
    // anchors (50 mg, 100 mg single-dose, 50 mg / 100 mg steady-state); the
    // injection / patch / gel routes carry zero weight in PkCatalog.coreParams
    // and fall through to zero contribution in the engine. The caller is
    // responsible for filtering the medication log down to ANTIANDROGEN
    // entries whose medicine maps to PkCompound.CPA (i.e. genuine CPA, not
    // spironolactone / bicalutamide / finasteride / dutasteride).
    fun simulateMainCpaTrend(
        entries: List<MedicationLogEntry>,
        bodyWeightKg: Double?,
        now: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        plannedEntries: List<MedicationLogEntry> = emptyList(),
        option: HomeE2ChartWindowOption = HomeE2ChartWindowOption.SEVEN_DAYS,
    ): PkTrendResult? {
        val resolvedBodyWeightKg =
            bodyWeightKg?.takeIf { it.isFinite() && it > 0 } ?: DefaultBodyWeightKg
        val nowInstant = now.atZone(zoneId).toInstant()
        val startInstant = now
            .toLocalDate()
            .atStartOfDay()
            .minusDays(option.pastDays)
            .atZone(zoneId)
            .toInstant()
        val predictionStartTimeH = Duration.between(startInstant, nowInstant)
            .toMillis() / 3_600_000.0
        val events = buildCpaEvents(
            anchor = startInstant,
            realEntries = entries,
            plannedEntries = plannedEntries,
        )
        val windowEndHours = option.chartWindowHours.toDouble()
        val chartVisibleEndHours = pkChartVisibleEndHours(windowEndHours)
        val chartSampleTimeH = mainChartSampleTimeH(
            events = events,
            predictionStartTimeH = predictionStartTimeH,
            windowEndHours = windowEndHours,
            chartVisibleEndHours = chartVisibleEndHours,
            option = option,
        )

        val result = PkSimulationEngine(
            events = events,
            hormone = PkHormone.CYPROTERONE_ACETATE,
            bodyWeightKg = resolvedBodyWeightKg,
            startTimeH = 0.0,
            endTimeH = windowEndHours,
            numberOfSteps = (option.chartWindowHours.toInt() + 1).coerceAtLeast(2),
        ).run(sampleTimeH = chartSampleTimeH)

        val dailyConcentrations = (0..option.chartWindowDays.toInt()).map { index ->
            result.concentrationAt(index * HoursPerDay) ?: 0.0
        }
        val chartSamples = result.timeH
            .zip(result.concentrations)
            .filter { (timeH, _) -> timeH in 0.0..chartVisibleEndHours }
        val doseMarkers = buildDoseMarkers(
            events = events,
            windowEndHours = chartVisibleEndHours,
            concentrationAt = result::concentrationAt,
        )
        return PkTrendResult(
            currentConcentration = result.concentrationAt(predictionStartTimeH) ?: 0.0,
            previousDayConcentration = result.concentrationAt(predictionStartTimeH - HoursPerDay)
                ?: 0.0,
            dailyConcentrations = dailyConcentrations,
            concentrationUnit = result.metadata.concentrationUnit,
            chartConcentrations = chartSamples.map { (_, concentration) -> concentration },
            chartSampleIntervalHours = MainChartSampleIntervalHours,
            chartTimeH = chartSamples.map { (timeH, _) -> timeH },
            doseMarkers = doseMarkers,
            chartWindowHours = option.chartWindowHours.toInt(),
            predictionStartTimeH = predictionStartTimeH.toChartXHour(),
        )
    }

    // Returned `windowStart` and `windowEnd` MUST equal
    // `generatedAt.toLocalDate().atStartOfDay().minusDays(mainChartPastDays)` and
    // `generatedAt.toLocalDate().plusDays(futureDays).atStartOfDay()` (in `zoneId`).
    // `HomeSnapshotRepository.snapshotUsabilityFailure` recomputes those bounds
    // independently and rejects snapshots whose stored window doesn't cover the
    // chart window — so changing this rounding silently invalidates every cache.
    fun simulateMainEstradiolProjection(
        entries: List<MedicationLogEntry>,
        bodyWeightKg: Double?,
        generatedAt: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
        plannedEntries: List<MedicationLogEntry> = emptyList(),
        option: HomeE2ChartWindowOption = HomeE2ChartWindowOption.SEVEN_DAYS,
        futureDays: Long = option.projectionFutureDays(),
    ): PkProjectionResult {
        val resolvedBodyWeightKg =
            bodyWeightKg?.takeIf { it.isFinite() && it > 0 } ?: DefaultBodyWeightKg
        val generatedAtInstant = generatedAt.atZone(zoneId).toInstant()
        val windowStart = generatedAt
            .toLocalDate()
            .atStartOfDay()
            .minusDays(option.pastDays)
            .atZone(zoneId)
            .toInstant()
        val windowEnd = generatedAt
            .toLocalDate()
            .plusDays(futureDays)
            .atStartOfDay()
            .atZone(zoneId)
            .toInstant()
        val windowHours = Duration.between(windowStart, windowEnd)
            .toMillis() / 3_600_000.0
        val generatedAtTimeH = Duration.between(windowStart, generatedAtInstant)
            .toMillis() / 3_600_000.0
        val events = buildEstradiolEvents(
            anchor = windowStart,
            realEntries = entries,
            plannedEntries = plannedEntries,
        )
        val sampleTimeH = mainProjectionSampleTimeH(
            events = events,
            windowEndHours = windowHours,
            generatedAtTimeH = generatedAtTimeH,
            option = option,
        )
        val result = PkSimulationEngine(
            events = events,
            hormone = PkHormone.ESTRADIOL,
            bodyWeightKg = resolvedBodyWeightKg,
            startTimeH = 0.0,
            endTimeH = windowHours,
            numberOfSteps = ceil(windowHours).toInt().coerceAtLeast(1) + 1,
        ).run(sampleTimeH = sampleTimeH)
        val doseMarkers = buildDoseMarkers(
            events = events,
            windowEndHours = windowHours,
            concentrationAt = result::concentrationAt,
        )

        return PkProjectionResult(
            generatedAt = generatedAtInstant,
            windowStart = windowStart,
            windowEnd = windowEnd,
            concentrationUnit = result.metadata.concentrationUnit,
            timeH = result.timeH,
            concentrations = result.concentrations,
            doseMarkers = doseMarkers,
        )
    }

    private fun Double.toChartXHour(): Double {
        return (this * ChartXPrecisionScale).roundToLong() / ChartXPrecisionScale
    }

    private fun buildTestosteroneEvents(
        anchor: Instant,
        realEntries: List<MedicationLogEntry>,
        plannedEntries: List<MedicationLogEntry>,
    ): List<PkDoseEvent> {
        val realEvents = realEntries.asSequence()
            .mapNotNull { entry ->
                entry.toTestosteronePkDoseEvent(
                    anchor = anchor,
                    isPlanned = false
                )
            }
        val plannedEvents = plannedEntries.asSequence()
            .mapNotNull { entry -> entry.toTestosteronePkDoseEvent(anchor = anchor, isPlanned = true) }
        return (realEvents + plannedEvents).toList()
    }

    private fun buildCpaEvents(
        anchor: Instant,
        realEntries: List<MedicationLogEntry>,
        plannedEntries: List<MedicationLogEntry>,
    ): List<PkDoseEvent> {
        val realEvents = realEntries.asSequence()
            .mapNotNull { entry ->
                entry.toCpaPkDoseEvent(
                    anchor = anchor,
                    isPlanned = false,
                )
            }
        val plannedEvents = plannedEntries.asSequence()
            .mapNotNull { entry -> entry.toCpaPkDoseEvent(anchor = anchor, isPlanned = true) }
        return (realEvents + plannedEvents).toList()
    }

    private fun buildEstradiolEvents(
        anchor: Instant,
        realEntries: List<MedicationLogEntry>,
        plannedEntries: List<MedicationLogEntry>,
    ): List<PkDoseEvent> {
        val realEvents = realEntries.asSequence()
            .mapNotNull { entry ->
                entry.toEstradiolPkDoseEvent(
                    anchor = anchor,
                    isPlanned = false
                )
            }
        val plannedEvents = plannedEntries.asSequence()
            .mapNotNull { entry -> entry.toEstradiolPkDoseEvent(anchor = anchor, isPlanned = true) }
        return (realEvents + plannedEvents).toList()
    }

    // Groups dose-marker-candidate events by their chart-rounded time. A marker
    // at a given instant is treated as "planned" only when every event at that
    // instant is itself planned — so a real entry that fulfills a planned slot
    // flips the marker color back to "logged".
    private fun buildDoseMarkers(
        events: List<PkDoseEvent>,
        windowEndHours: Double,
        concentrationAt: (Double) -> Double?,
    ): List<PkDoseMarker> {
        return events
            .asSequence()
            .filter(PkDoseEvent::isDoseMarkerCandidate)
            .mapNotNull { event ->
                val time = event.timeH.toChartXHour()
                if (time.isFinite() && time in 0.0..windowEndHours) time to event else null
            }
            .groupBy({ (time, _) -> time }, { (_, event) -> event })
            .toSortedMap()
            .mapNotNull { (time, eventsAtTime) ->
                concentrationAt(time)?.let { concentration ->
                    PkDoseMarker(
                        timeH = time,
                        concentration = concentration,
                        isPlanned = eventsAtTime.all { it.isPlanned },
                    )
                }
            }
    }

    private fun mainChartSampleTimeH(
        events: List<PkDoseEvent>,
        predictionStartTimeH: Double,
        windowEndHours: Double,
        chartVisibleEndHours: Double,
        option: HomeE2ChartWindowOption,
    ): List<Double> {
        val windowStart = 0.0
        val previousDayTimeH = predictionStartTimeH - HoursPerDay
        val denseSampleTimeH = mainChartDenseSampleTimeH(
            startTimeH = windowStart,
            endTimeH = windowEndHours,
            policy = option.densePolicy,
        )
        val loggedEventTimeH = events
            .asSequence()
            .map { event -> event.timeH.toChartXHour() }
            .filter { timeH -> timeH.isFinite() && timeH in windowStart..windowEndHours }
            .toList()
        val offsetSampleTimeH = postDoseOffsetSampleTimeH(
            events = events,
            windowStart = windowStart,
            windowEndHours = windowEndHours,
            option = option,
        )
        val exactSampleTimeH = (
                listOf(predictionStartTimeH.toChartXHour()) +
                        listOf(chartVisibleEndHours.toChartXHour()) +
                        listOf(previousDayTimeH.toChartXHour()).filter { timeH ->
                            timeH.isFinite() && timeH in windowStart..windowEndHours
                        } +
                        loggedEventTimeH +
                        offsetSampleTimeH
                )
            .filter { timeH -> timeH.isFinite() && timeH in windowStart..windowEndHours }

        return (denseSampleTimeH + exactSampleTimeH)
            .map { timeH -> timeH.toChartXHour() }
            .distinct()
            .sorted()
    }

    private fun mainProjectionSampleTimeH(
        events: List<PkDoseEvent>,
        windowEndHours: Double,
        generatedAtTimeH: Double,
        option: HomeE2ChartWindowOption,
    ): List<Double> {
        val previousDayTimeH = generatedAtTimeH - HoursPerDay
        val denseSampleTimeH = mainChartDenseSampleTimeH(
            startTimeH = 0.0,
            endTimeH = windowEndHours,
            policy = option.densePolicy,
        )
        val exactSampleTimeH = listOf(
            generatedAtTimeH.toChartXHour(),
            previousDayTimeH.toChartXHour(),
        ).filter { timeH -> timeH.isFinite() && timeH in 0.0..windowEndHours }
        val loggedEventTimeH = events
            .asSequence()
            .map { event -> event.timeH.toChartXHour() }
            .filter { timeH -> timeH.isFinite() && timeH in 0.0..windowEndHours }
            .toList()
        val offsetSampleTimeH = postDoseOffsetSampleTimeH(
            events = events,
            windowStart = 0.0,
            windowEndHours = windowEndHours,
            option = option,
        )

        return (denseSampleTimeH + exactSampleTimeH + loggedEventTimeH + offsetSampleTimeH)
            .map { timeH -> timeH.toChartXHour() }
            .distinct()
            .sorted()
    }

    private fun postDoseOffsetSampleTimeH(
        events: List<PkDoseEvent>,
        windowStart: Double,
        windowEndHours: Double,
        option: HomeE2ChartWindowOption,
    ): List<Double> {
        if (!option.includesPostDoseOffsets) return emptyList()
        return events
            .asSequence()
            .filter(PkDoseEvent::isDoseMarkerCandidate)
            .flatMap { event ->
                MainChartPostDoseSampleOffsetsHours.asSequence().map { offset ->
                    (event.timeH + offset).toChartXHour()
                }
            }
            .filter { timeH -> timeH.isFinite() && timeH in windowStart..windowEndHours }
            .toList()
    }

    private fun mainChartDenseSampleTimeH(
        startTimeH: Double,
        endTimeH: Double,
        policy: DenseSamplePolicy,
    ): List<Double> {
        val span = endTimeH - startTimeH
        if (span <= 0.0) {
            return emptyList()
        }
        return when (policy) {
            is DenseSamplePolicy.Interval -> {
                val segmentCount = ceil(span / policy.hours).toInt().coerceAtLeast(1)
                (0..segmentCount).map { index ->
                    (startTimeH + policy.hours * index)
                        .coerceAtMost(endTimeH)
                        .toChartXHour()
                }
            }

            is DenseSamplePolicy.Budget -> {
                val segmentCount = policy.segmentCount.coerceAtLeast(1)
                val interval = span / segmentCount
                (0..segmentCount).map { index ->
                    (startTimeH + interval * index)
                        .coerceAtMost(endTimeH)
                        .toChartXHour()
                }
            }
        }
    }
}

class PkSimulationEngine(
    events: List<PkDoseEvent>,
    private val hormone: PkHormone,
    private val bodyWeightKg: Double,
    private val startTimeH: Double,
    private val endTimeH: Double,
    private val numberOfSteps: Int,
    // PR-user-override: optional per-user parameter overrides. Null
    // means "use the engine's catalog default". Currently consumed
    // by the 3-comp RK4 path (overrides kClear and k31 if
    // provided). The 1-comp and gel paths ignore the override
    // because the parameters it controls (central clearance, deep
    // peripheral release) don't exist there.
    private val userOverride: PkTUserOverride = PkTUserOverride(),
) {
    private val sortedEvents = events.sortedBy(PkDoseEvent::timeH)
    private val eventModels = sortedEvents
        .asSequence()
        .filter { event -> event.hormone == hormone && event.route != PkRoute.PATCH_REMOVE }
        .map { event -> PrecomputedPkEventModel(event, sortedEvents) }
        .toList()
    private val metadata = PkSimulationMetadata(
        hormone = hormone,
        concentrationUnit = PkCatalog.defaultConcentrationUnit(hormone),
    )
    private val plasmaVolumeMl = PkCatalog.coreParams(hormone).vdPerKg * bodyWeightKg * 1000.0

    fun run(sampleTimeH: List<Double>? = null): PkSimulationResult {
        if (startTimeH >= endTimeH || numberOfSteps <= 1 || plasmaVolumeMl <= 0.0) {
            return PkSimulationResult(
                timeH = emptyList(),
                concentrations = emptyList(),
                auc = 0.0,
                metadata = metadata,
            )
        }

        val sampleTimes = sampleTimeH
            ?.asSequence()
            ?.filter { timeH -> timeH.isFinite() && timeH in startTimeH..endTimeH }
            ?.distinct()
            ?.sorted()
            ?.toList()
            ?.takeIf { timeH -> timeH.size > 1 }
            ?: fixedSampleTimes()
        val concentrationScale =
            metadata.concentrationUnit.concentrationScale(hormone) / plasmaVolumeMl

        // v3 血检追踪 (PR-V3-TU): when any event in the run maps
        // to a 3-comp compound, switch the integration from the
        // legacy 1-comp analytical shortcut (concentration =
        // depotAmount / plasmaVolume) to a 3-comp ODE step on
        // [central, shallow, deep] driven by the depot's release
        // rate. Other (1-comp) events in the same run still get
        // 1-comp treatment because the per-event compound is the
        // override lookup key.
        val anyThreeCompEvent = eventModels.any { model ->
            PkCatalog.threeCompOverride(model.event.compound) != null
        }

        // PR2 (PR-GEL): when any event in the run is on a route with
        // a skin-depot override (today only GEL), switch to a 2-comp
        // ODE on [skin, central] with a Michaelis-Menten transfer
        // rate (Vmax * A_skin / (Km + A_skin)) instead of the linear
        // first-order Bateman. This captures the non-linear
        // dose-response of transdermal testosterone (5 g absorbs
        // ~23% of dose, 10 g absorbs ~16%). The 2-comp path is
        // gated to TESTOSTERONE events only today — E2 gel
        // (e.g. Divigel / Estrogel) stays on the 1-comp path
        // because the non-linearity calibration is for the
        // testosterone gel SPC; E2 has its own clinical anchors
        // and the 1-comp parameters are pre-tuned for them.
        val anySkinDepotEvent = eventModels.any { model ->
            model.event.hormone == PkHormone.TESTOSTERONE &&
                PkCatalog.skinDepotOverride(model.event.route) != null
        }

        val time = ArrayList<Double>(sampleTimes.size)
        val concentrations = ArrayList<Double>(sampleTimes.size)
        var previousConcentration = 0.0
        var previousTimeH = startTimeH
        var auc = 0.0

        if (anyThreeCompEvent) {
            // 3-comp integration. State: amount in central/shallow/deep (mg).
            // The depot is computed analytically per sample time
            // (existing PrecomputedPkEventModel.amountAt) and the
            // release rate over each [t_prev, t] segment is
            // (depot(t_prev) - depot(t)) / deltaT. We assume the rate
            // is constant over the segment, which is accurate for the
            // 30-min sample step we use; for finer sampling we'd
            // sub-step the RK4 inside each segment.
            var amountCentral = 0.0
            var amountShallow = 0.0
            var amountDeep = 0.0
            // Sum the central, shallow, deep volumes across the
            // 3-comp overrides. Today only one compound (TU) has
            // the override, so summing is correct; if a second
            // compound ever gets 3-comp, this would need to be
            // per-compound (sum-of-distributions).
            val threeCompSum: ThreeCompAggregate = eventModels
                .mapNotNull { model -> PkCatalog.threeCompOverride(model.event.compound) }
                .fold(ThreeCompAggregate.ZERO) { acc, ov ->
                    ThreeCompAggregate(
                        centralVolumeMl = acc.centralVolumeMl + ov.centralVdPerKg * bodyWeightKg * 1000.0,
                        shallowVolumeMl = acc.shallowVolumeMl + ov.shallowVdPerKg * bodyWeightKg * 1000.0,
                        deepVolumeMl = acc.deepVolumeMl + ov.deepVdPerKg * bodyWeightKg * 1000.0,
                        k12 = acc.k12 + ov.k12,
                        k21 = acc.k21 + ov.k21,
                        k13 = acc.k13 + ov.k13,
                        // PR-user-override: k31 lets the user tune
                        // the deep peripheral release rate. A larger
                        // k31 means the deep fat reservoir releases
                        // faster (shorter terminal t½); smaller k31
                        // means longer terminal tail. Falls back to
                        // the catalog value when not set.
                        k31 = acc.k31 + (userOverride.threeCompK31 ?: ov.k31),
                    )
                }
            // Concentration in the 3-comp path uses the central volume
            // (not the legacy 1-comp plasma volume), so the central
            // concentration = amountCentral / Vd_c.
            val threeCompPlasmaVolumeMl = threeCompSum.centralVolumeMl
            val threeCompConcentrationScale = metadata.concentrationUnit
                .concentrationScale(hormone) / threeCompPlasmaVolumeMl
            // For the kClear in 3-comp: use the user's override
            // if set, otherwise the catalog default (testosterone's
            // kClearInjection = 0.05 h⁻¹). The override lets
            // advanced users tune the central clearance to their
            // own lab data (e.g. "my T clears 30% faster than the
            // literature average").
            val core = PkCatalog.coreParams(PkHormone.TESTOSTERONE)
            val kClear = userOverride.kClearInjection ?: core.kClearInjection

            sampleTimes.forEachIndexed { index, t ->
                val depotMg = eventModels.sumOf { model -> model.depotAmountAt(t) }
                val deltaT = t - previousTimeH
                // Linear depot release rate over the segment
                val depotPrev = if (index == 0) depotMg else {
                    val prevDepotMg = eventModels.sumOf { model -> model.depotAmountAt(previousTimeH) }
                    prevDepotMg
                }
                val releaseRateMgPerH = if (deltaT > 0) {
                    maxOf(0.0, (depotPrev - depotMg) / deltaT)
                } else 0.0

                // RK4 step on [central, shallow, deep] with
                // constant input `releaseRateMgPerH` over deltaT
                if (deltaT > 0) {
                    val rk = rk4ThreeComp(
                        central = amountCentral,
                        shallow = amountShallow,
                        deep = amountDeep,
                        input = releaseRateMgPerH,
                        deltaT = deltaT,
                        kClear = kClear,
                        k12 = threeCompSum.k12,
                        k21 = threeCompSum.k21,
                        k13 = threeCompSum.k13,
                        k31 = threeCompSum.k31,
                    )
                    amountCentral = rk.central
                    amountShallow = rk.shallow
                    amountDeep = rk.deep
                }
                val ngPerDl = amountCentral * threeCompConcentrationScale

                time += t
                concentrations += ngPerDl
                if (index > 0) {
                    auc += 0.5 * (ngPerDl + previousConcentration) * (t - previousTimeH)
                }
                previousConcentration = ngPerDl
                previousTimeH = t
            }
        } else if (anySkinDepotEvent) {
            // PR2 (PR-GEL): 2-comp skin depot with Michaelis-Menten
            // transfer. State: amount in skin (mg) and central (mg).
            // Events on a skin-depot route (today: GEL) contribute a
            // pulse to `amountSkin` at the moment their event.timeH
            // is crossed. Other events in the same run (1-comp
            // injectables, etc.) fall through to the linear
            // contribution by also being summed in via
            // `model.amountAt(t) * concentrationScale`.
            var amountSkin = 0.0
            var amountCentral = 0.0
            // First skin-depot override wins; today only GEL has
            // one, so a single Vmax/Km covers all gel events. If a
            // second route ever gets an override, sum the Vmax
            // and use the smallest Km.
            val skinOverride: SkinDepotOverride = eventModels
                .mapNotNull { model -> PkCatalog.skinDepotOverride(model.event.route) }
                .first()
            val core = PkCatalog.coreParams(hormone)
            // kClear for non-injection: use the hormone's kClear
            // (testosterone's kClear=0.6 h⁻¹).
            val kClear = core.kClear

            sampleTimes.forEachIndexed { index, t ->
                // Pulse input: any skin-depot event whose timeH
                // falls in the (previousTimeH, t] interval is
                // applied now (instant skin loading at the
                // application time).
                val pulseMg = eventModels
                    .filter { model -> PkCatalog.skinDepotOverride(model.event.route) != null }
                    .sumOf { model ->
                        if (t >= model.event.timeH && previousTimeH < model.event.timeH) {
                            model.event.doseMg
                        } else 0.0
                    }
                if (pulseMg > 0.0) {
                    amountSkin += pulseMg
                }

                val deltaT = t - previousTimeH
                if (deltaT > 0) {
                    val rk = rk4TwoCompSkin(
                        skin = amountSkin,
                        central = amountCentral,
                        Vmax = skinOverride.Vmax,
                        Km = skinOverride.Km,
                        kCl = skinOverride.kCl,
                        kClear = kClear,
                        deltaT = deltaT,
                    )
                    amountSkin = maxOf(0.0, rk.skin)
                    amountCentral = maxOf(0.0, rk.central)
                }
                val ngPerDl = amountCentral * concentrationScale

                time += t
                concentrations += ngPerDl
                if (index > 0) {
                    auc += 0.5 * (ngPerDl + previousConcentration) * (t - previousTimeH)
                }
                previousConcentration = ngPerDl
                previousTimeH = t
            }
        } else {
            // Legacy 1-comp behavior: concentration = depot amount
            // / central volume.
            sampleTimes.forEachIndexed { index, t ->
                val totalAmountMg = eventModels.sumOf { model -> model.amountAt(t) }
                val concentration = totalAmountMg * concentrationScale

                time += t
                concentrations += concentration
                if (index > 0) {
                    auc += 0.5 * (concentration + previousConcentration) * (t - previousTimeH)
                }
                previousConcentration = concentration
                previousTimeH = t
            }
        }

        return PkSimulationResult(
            timeH = time,
            concentrations = concentrations,
            auc = auc,
            metadata = metadata,
        )
    }

    /**
     * v3 血检追踪 (PR-V3-TU): classical RK4 step on a
     * 3-compartment mammillary distribution ODE with constant
     * input (the depot's release rate, treated as piecewise
     * constant over each [t_prev, t] segment).
     *
     * dA_central/dt = input - (kClear + k12 + k13) * A_central + k21 * A_shallow + k31 * A_deep
     * dA_shallow/dt = k12 * A_central - k21 * A_shallow
     * dA_deep/dt    = k13 * A_central - k31 * A_deep
     */
    private fun rk4ThreeComp(
        central: Double,
        shallow: Double,
        deep: Double,
        input: Double,
        deltaT: Double,
        kClear: Double,
        k12: Double,
        k21: Double,
        k13: Double,
        k31: Double,
    ): ThreeCompState {
        val k1 = k12 + k13 + kClear
        fun deriv(c: Double, s: Double, d: Double): ThreeCompState {
            val dc = input - k1 * c + k21 * s + k31 * d
            val ds = k12 * c - k21 * s
            val dd = k13 * c - k31 * d
            return ThreeCompState(central = dc, shallow = ds, deep = dd)
        }
        val k1State = deriv(central, shallow, deep)
        val k2State = deriv(
            central + 0.5 * deltaT * k1State.central,
            shallow + 0.5 * deltaT * k1State.shallow,
            deep + 0.5 * deltaT * k1State.deep,
        )
        val k3State = deriv(
            central + 0.5 * deltaT * k2State.central,
            shallow + 0.5 * deltaT * k2State.shallow,
            deep + 0.5 * deltaT * k2State.deep,
        )
        val k4State = deriv(
            central + deltaT * k3State.central,
            shallow + deltaT * k3State.shallow,
            deep + deltaT * k3State.deep,
        )
        return ThreeCompState(
            central = central + (deltaT / 6.0) * (k1State.central + 2 * k2State.central + 2 * k3State.central + k4State.central),
            shallow = shallow + (deltaT / 6.0) * (k1State.shallow + 2 * k2State.shallow + 2 * k3State.shallow + k4State.shallow),
            deep = deep + (deltaT / 6.0) * (k1State.deep + 2 * k2State.deep + 2 * k3State.deep + k4State.deep),
        )
    }

    /**
     * PR2 (PR-GEL): classical RK4 step on a 2-compartment
     * skin-depot ODE with a saturable Michaelis-Menten transfer
     * to central AND a first-order skin clearance (drug lost
     * to the environment without being absorbed).
     *
     * dA_skin/dt    = - Vmax * A_skin / (Km + A_skin) - kCl * A_skin
     * dA_central/dt =   Vmax * A_skin / (Km + A_skin) - kClear * A_central
     *
     * The MM absorption term is the saturable path that gives the
     * non-linear dose-response (5 g absorbs ~23% of dose, 10 g
     * absorbs ~16%). The kCl term is what stops the skin
     * reservoir from accumulating indefinitely — without it the
     * daily input (50 or 100 mg) would exceed the daily
     * absorption output (max Vmax × 24 h) and the skin would
     * grow without bound.
     */
    private fun rk4TwoCompSkin(
        skin: Double,
        central: Double,
        Vmax: Double,
        Km: Double,
        kCl: Double,
        kClear: Double,
        deltaT: Double,
    ): TwoCompSkinState {
        fun deriv(s: Double, c: Double): TwoCompSkinState {
            val transfer = if (s > 0.0) Vmax * s / (Km + s) else 0.0
            val lost = if (s > 0.0) kCl * s else 0.0
            val ds = -transfer - lost
            val dc = transfer - kClear * c
            return TwoCompSkinState(skin = ds, central = dc)
        }
        val k1State = deriv(skin, central)
        val k2State = deriv(
            skin + 0.5 * deltaT * k1State.skin,
            central + 0.5 * deltaT * k1State.central,
        )
        val k3State = deriv(
            skin + 0.5 * deltaT * k2State.skin,
            central + 0.5 * deltaT * k2State.central,
        )
        val k4State = deriv(
            skin + deltaT * k3State.skin,
            central + deltaT * k3State.central,
        )
        return TwoCompSkinState(
            skin = skin + (deltaT / 6.0) * (k1State.skin + 2 * k2State.skin + 2 * k3State.skin + k4State.skin),
            central = central + (deltaT / 6.0) * (k1State.central + 2 * k2State.central + 2 * k3State.central + k4State.central),
        )
    }

    private fun fixedSampleTimes(): List<Double> {
        val stepSize = (endTimeH - startTimeH) / (numberOfSteps - 1)
        return List(numberOfSteps) { index -> startTimeH + index * stepSize }
    }
}

private class PrecomputedPkEventModel(
    val event: PkDoseEvent,
    allEvents: List<PkDoseEvent>,
) {
    private val startTime = event.timeH
    private val dose = event.doseMg
    private val params = PkParameterResolver.resolve(event)
    private val oneCompParams = PkParams(
        fracFast = 1.0,
        k1Fast = params.k1Fast,
        k1Slow = 0.0,
        k2 = params.k2,
        k3 = params.k3,
        bioavailability = params.bioavailability,
        rateMgH = params.rateMgH,
        fastBioavailability = params.bioavailability,
        slowBioavailability = params.bioavailability,
    )

    private val amountAt: (Double) -> Double = when (event.route) {
        PkRoute.INJECTION -> { timeH ->
            val tau = timeH - startTime
            if (tau < 0.0) 0.0 else ThreeCompartmentModel.injectionAmount(tau, dose, params)
        }

        PkRoute.GEL,
        PkRoute.ORAL -> { timeH ->
            val tau = timeH - startTime
            if (tau < 0.0) 0.0 else ThreeCompartmentModel.oneCompartmentAmount(
                tau,
                dose,
                oneCompParams
            )
        }

        PkRoute.SUBLINGUAL -> { timeH ->
            val tau = timeH - startTime
            when {
                tau < 0.0 -> 0.0
                params.k2 > 0.0 -> ThreeCompartmentModel.dualAbsorptionMixedAmount(
                    tau,
                    dose,
                    params
                )

                else -> ThreeCompartmentModel.dualAbsorptionAmount(tau, dose, params)
            }
        }

        PkRoute.PATCH_APPLY -> {
            // PATCH_OFF means "remove all currently-on patches of this compound",
            // independent of the group that scheduled the apply. The strict `>`
            // filter ensures a remove logged at the same instant as an apply is
            // ordered before the apply — it pairs with the previous patch, not
            // the one being applied right now.
            val removeEvent = allEvents
                .asSequence()
                .filter { candidate ->
                    candidate.hormone == event.hormone &&
                            candidate.compound == event.compound &&
                            candidate.route == PkRoute.PATCH_REMOVE &&
                            candidate.timeH > startTime
                }
                .minByOrNull(PkDoseEvent::timeH)
            val wearH = (removeEvent?.timeH ?: Double.POSITIVE_INFINITY) - startTime
            { timeH ->
                val tau = timeH - startTime
                if (tau < 0.0) {
                    0.0
                } else {
                    ThreeCompartmentModel.patchAmount(tau, dose, wearH, params)
                }
            }
        }

        PkRoute.PATCH_REMOVE -> { _ -> 0.0 }
    }

    fun amountAt(timeH: Double): Double = amountAt.invoke(timeH)

    /**
     * v3 血检追踪 (PR-V3-TU): returns the unabsorbed depot amount
     * (mg) at the given absolute time, used as the input rate source
     * for the 3-comp RK4 integration. For injection this is the
     * two-part exponential scaled by the per-ester formation
     * fraction: `dose * F * (fracFast * exp(-k1Fast*tau) +
     * (1-fracFast) * exp(-k1Slow*tau))` where F is the
     * "bioavailability" field on PkParams (which the per-ester
     * resolver sets to the formation fraction, e.g. 0.060 for TU).
     * The 94% "lost" drug is implicitly cleared at the release
     * point — it never enters central, so we never track it in
     * the 3-comp ODE.
     *
     * Note: this is the unabsorbed drug remaining in the depot, NOT
     * the amount already in central plasma (which is what
     * `amountAt(t)` returns for the 1-comp model). The 3-comp engine
     * uses this to derive the release rate `-(d/dt) depot(t)` as
     * the input to the [central, shallow, deep] ODE.
     */
    private val depotAmountAt: (Double) -> Double = when (event.route) {
        PkRoute.INJECTION -> { timeH ->
            val tau = timeH - startTime
            if (tau < 0.0) 0.0 else {
                val effectiveDose = dose * params.bioavailability
                effectiveDose * (
                    params.fracFast * exp(-params.k1Fast * tau) +
                        (1.0 - params.fracFast) * exp(-params.k1Slow * tau)
                    )
            }
        }

        PkRoute.PATCH_REMOVE,
        PkRoute.PATCH_APPLY,
        PkRoute.GEL,
        PkRoute.ORAL,
        PkRoute.SUBLINGUAL -> { _ -> 0.0 }
    }

    fun depotAmountAt(timeH: Double): Double = depotAmountAt.invoke(timeH)
}

private object PkParameterResolver {
    fun resolve(event: PkDoseEvent): PkParams {
        val core = PkCatalog.coreParams(event.hormone)
        val k3 = if (event.route == PkRoute.INJECTION && event.compound != PkCompound.EU) {
            core.kClearInjection
        } else {
            core.kClear
        }

        return when (event.route) {
            PkRoute.INJECTION -> {
                val depot = PkCatalog.twoPartDepotParams(event.compound)
                val k1Fast = (depot?.k1Fast ?: 0.0) * core.depotK1Correction
                val k1Slow = (depot?.k1Slow ?: 0.0) * core.depotK1Correction
                val formationFraction = PkCatalog.formationFraction(event.compound) ?: 1.0
                PkParams(
                    fracFast = depot?.fracFast ?: 1.0,
                    k1Fast = k1Fast,
                    k1Slow = k1Slow,
                    k2 = PkCatalog.hydrolysisK2(event.compound) ?: 0.0,
                    k3 = k3,
                    bioavailability = formationFraction,
                    rateMgH = 0.0,
                    fastBioavailability = formationFraction,
                    slowBioavailability = formationFraction,
                )
            }

            PkRoute.PATCH_APPLY -> {
                val releaseRateMcgPerDay = event.releaseRateMcgPerDay
                if (releaseRateMcgPerDay != null) {
                    PkParams(
                        fracFast = 1.0,
                        k1Fast = 0.0,
                        k1Slow = 0.0,
                        k2 = 0.0,
                        k3 = k3,
                        bioavailability = 1.0,
                        rateMgH = releaseRateMcgPerDay / 24_000.0,
                        fastBioavailability = 1.0,
                        slowBioavailability = 1.0,
                    )
                } else {
                    PkParams(
                        fracFast = 1.0,
                        k1Fast = core.patchFallbackK1,
                        k1Slow = 0.0,
                        k2 = 0.0,
                        k3 = k3,
                        bioavailability = 1.0,
                        rateMgH = 0.0,
                        fastBioavailability = 1.0,
                        slowBioavailability = 1.0,
                    )
                }
            }

            PkRoute.GEL -> {
                PkParams(
                    fracFast = 1.0,
                    k1Fast = core.gelK1,
                    k1Slow = 0.0,
                    k2 = 0.0,
                    k3 = k3,
                    bioavailability = core.gelFMax,
                    rateMgH = 0.0,
                    fastBioavailability = core.gelFMax,
                    slowBioavailability = core.gelFMax,
                )
            }

            PkRoute.ORAL -> {
                val bioavailability = PkCatalog.oralBioavailability(event.compound) ?: 0.0
                PkParams(
                    fracFast = 1.0,
                    k1Fast = PkCatalog.oralKAbs(event.compound) ?: 0.0,
                    k1Slow = 0.0,
                    k2 = if (event.hormone == PkHormone.ESTRADIOL && event.compound == PkCompound.EV) {
                        PkCatalog.hydrolysisK2(PkCompound.EV) ?: 0.0
                    } else {
                        0.0
                    },
                    k3 = k3,
                    bioavailability = bioavailability,
                    rateMgH = 0.0,
                    fastBioavailability = bioavailability,
                    slowBioavailability = bioavailability,
                )
            }

            PkRoute.SUBLINGUAL -> {
                if (event.hormone != PkHormone.ESTRADIOL) {
                    PkParams(0.0, 0.0, 0.0, 0.0, k3, 0.0, 0.0, 0.0, 0.0)
                } else {
                    val theta = event.sublingualTheta?.coerceIn(0.0, 1.0)
                        ?: PkCatalog.standardSublingualTheta
                    PkParams(
                        fracFast = theta,
                        k1Fast = PkCatalog.kAbsSublingual,
                        k1Slow = PkCatalog.oralKAbs(event.compound) ?: 0.0,
                        k2 = if (event.compound == PkCompound.EV) {
                            PkCatalog.hydrolysisK2(PkCompound.EV) ?: 0.0
                        } else {
                            0.0
                        },
                        k3 = k3,
                        bioavailability = 1.0,
                        rateMgH = 0.0,
                        fastBioavailability = 1.0,
                        slowBioavailability = PkCatalog.oralBioavailability(event.compound) ?: 0.0,
                    )
                }
            }

            PkRoute.PATCH_REMOVE -> PkParams(0.0, 0.0, 0.0, 0.0, k3, 0.0, 0.0, 0.0, 0.0)
        }
    }
}

object ThreeCompartmentModel {
    fun dualAbsorptionThreeCompartmentAmount(tau: Double, doseMg: Double, p: PkParams): Double {
        if (tau < 0.0 || doseMg <= 0.0) {
            return 0.0
        }
        val fraction = p.fracFast.coerceIn(0.0, 1.0)
        return analyticThreeCompartmentAmount(
            tau = tau,
            doseMg = doseMg * fraction,
            bioavailability = p.fastBioavailability,
            k1 = p.k1Fast,
            k2 = p.k2,
            k3 = p.k3,
        ) + analyticThreeCompartmentAmount(
            tau = tau,
            doseMg = doseMg * (1.0 - fraction),
            bioavailability = p.slowBioavailability,
            k1 = p.k1Slow,
            k2 = p.k2,
            k3 = p.k3,
        )
    }

    fun dualAbsorptionMixedAmount(tau: Double, doseMg: Double, p: PkParams): Double {
        if (tau < 0.0 || doseMg <= 0.0) {
            return 0.0
        }
        val fraction = p.fracFast.coerceIn(0.0, 1.0)
        return analyticThreeCompartmentAmount(
            tau = tau,
            doseMg = doseMg * fraction,
            bioavailability = p.fastBioavailability,
            k1 = p.k1Fast,
            k2 = p.k2,
            k3 = p.k3,
        ) + batemanAmount(
            doseMg = doseMg * (1.0 - fraction),
            bioavailability = p.slowBioavailability,
            ka = p.k1Slow,
            ke = p.k3,
            t = tau,
        )
    }

    fun dualAbsorptionAmount(tau: Double, doseMg: Double, p: PkParams): Double {
        if (tau < 0.0 || doseMg <= 0.0) {
            return 0.0
        }
        val fraction = p.fracFast.coerceIn(0.0, 1.0)
        return batemanAmount(
            doseMg = doseMg * fraction,
            bioavailability = p.fastBioavailability,
            ka = p.k1Fast,
            ke = p.k3,
            t = tau,
        ) + batemanAmount(
            doseMg = doseMg * (1.0 - fraction),
            bioavailability = p.slowBioavailability,
            ka = p.k1Slow,
            ke = p.k3,
            t = tau,
        )
    }

    fun injectionAmount(tau: Double, doseMg: Double, p: PkParams): Double {
        if (tau < 0.0 || doseMg <= 0.0) {
            return 0.0
        }
        val fraction = p.fracFast.coerceIn(0.0, 1.0)
        return analyticThreeCompartmentAmount(
            tau = tau,
            doseMg = doseMg * fraction,
            bioavailability = p.bioavailability,
            k1 = p.k1Fast,
            k2 = p.k2,
            k3 = p.k3,
        ) + analyticThreeCompartmentAmount(
            tau = tau,
            doseMg = doseMg * (1.0 - fraction),
            bioavailability = p.bioavailability,
            k1 = p.k1Slow,
            k2 = p.k2,
            k3 = p.k3,
        )
    }

    fun oneCompartmentAmount(tau: Double, doseMg: Double, p: PkParams): Double {
        return batemanAmount(
            doseMg = doseMg,
            bioavailability = p.bioavailability,
            ka = p.k1Fast,
            ke = p.k3,
            t = tau,
        )
    }

    fun patchAmount(tau: Double, doseMg: Double, wearH: Double, p: PkParams): Double {
        if (tau < 0.0 || p.k3 <= 0.0) {
            return 0.0
        }

        if (p.rateMgH > 0.0) {
            return if (tau <= wearH) {
                p.rateMgH / p.k3 * (1.0 - exp(-p.k3 * tau))
            } else {
                val amountAtRemoval = p.rateMgH / p.k3 * (1.0 - exp(-p.k3 * wearH))
                amountAtRemoval * exp(-p.k3 * (tau - wearH))
            }
        }

        val oneCompParams = p.copy(fracFast = 1.0, k1Slow = 0.0)
        val amountUnderPatch = oneCompartmentAmount(tau, doseMg, oneCompParams)
        return if (tau > wearH) {
            val amountAtRemoval = oneCompartmentAmount(wearH, doseMg, oneCompParams)
            amountAtRemoval * exp(-p.k3 * (tau - wearH))
        } else {
            amountUnderPatch
        }
    }

    private fun analyticThreeCompartmentAmount(
        tau: Double,
        doseMg: Double,
        bioavailability: Double,
        k1: Double,
        k2: Double,
        k3: Double,
    ): Double {
        if (tau < 0.0 || doseMg <= 0.0 || bioavailability <= 0.0 || k1 <= 0.0 || k2 <= 0.0 || k3 <= 0.0) {
            return 0.0
        }

        analyticThreeCompartmentRepeatedRates(
            tau = tau,
            doseMg = doseMg,
            bioavailability = bioavailability,
            k1 = k1,
            k2 = k2,
            k3 = k3,
            tolerance = 1e-9,
        )?.let { return it }

        val k1k2 = k1 - k2
        val k1k3 = k1 - k3
        val k2k3 = k2 - k3
        val term1 = exp(-k1 * tau) / (k1k2 * k1k3)
        val term2 = exp(-k2 * tau) / (-k1k2 * k2k3)
        val term3 = exp(-k3 * tau) / (k1k3 * k2k3)
        return doseMg * bioavailability * k1 * k2 * (term1 + term2 + term3)
    }

    private fun analyticThreeCompartmentRepeatedRates(
        tau: Double,
        doseMg: Double,
        bioavailability: Double,
        k1: Double,
        k2: Double,
        k3: Double,
        tolerance: Double,
    ): Double? {
        val scaledDose = doseMg * bioavailability
        val k1EqualsK2 = abs(k1 - k2) < tolerance
        val k1EqualsK3 = abs(k1 - k3) < tolerance
        val k2EqualsK3 = abs(k2 - k3) < tolerance

        if (k1EqualsK2 && k1EqualsK3) {
            return scaledDose * k1 * k1 * tau * tau * 0.5 * exp(-k1 * tau)
        }

        if (k2EqualsK3) {
            val delta = k1 - k2
            if (abs(delta) < tolerance) {
                return null
            }
            return scaledDose * k1 * k2 *
                    (exp(-k1 * tau) + exp(-k2 * tau) * (delta * tau - 1.0)) /
                    (delta * delta)
        }

        if (k1EqualsK2) {
            val delta = k1 - k3
            if (abs(delta) < tolerance) {
                return null
            }
            return scaledDose * k1 * k1 *
                    (exp(-k3 * tau) - exp(-k1 * tau) * (1.0 + delta * tau)) /
                    (delta * delta)
        }

        if (k1EqualsK3) {
            val delta = k2 - k1
            if (abs(delta) < tolerance) {
                return null
            }
            return scaledDose * k1 * k2 *
                    (exp(-k2 * tau) + exp(-k1 * tau) * (delta * tau - 1.0)) /
                    (delta * delta)
        }

        return null
    }

    private fun batemanAmount(
        doseMg: Double,
        bioavailability: Double,
        ka: Double,
        ke: Double,
        t: Double,
    ): Double {
        if (t < 0.0 || doseMg <= 0.0 || bioavailability <= 0.0 || ka <= 0.0 || ke <= 0.0) {
            return 0.0
        }
        return if (abs(ka - ke) < 1e-9) {
            doseMg * bioavailability * ka * t * exp(-ke * t)
        } else {
            doseMg * bioavailability * ka / (ka - ke) * (exp(-ke * t) - exp(-ka * t))
        }
    }
}

private data class PkDoseAmounts(val doseMg: Double, val releaseRateMcgPerDay: Double?)

private fun pkDoseAmounts(
    medicine: Medicine?,
    doseInstruction: DoseInstruction,
    route: PkRoute,
    snapshotEquivalentE2Mg: Double?,
    count: Int,
): PkDoseAmounts {
    val multiplier = count.coerceAtLeast(1)
    // PATCH_OFF carries the PatchOff singleton medicine; the simulator still
    // routes patch removals on applicationType alone (handled by the
    // PATCH_REMOVE branch below). Legacy log rows from before the singleton
    // existed pass medicine == null here; both paths fall through cleanly.
    val patchSpec = (medicine?.preparation as? MedicinePreparation.Patch)?.specification
    val perUnitDoseMg = when (route) {
        PkRoute.PATCH_REMOVE -> 0.0
        PkRoute.PATCH_APPLY -> when (patchSpec) {
            is MedicinePreparation.PatchSpecification.TotalMg -> patchSpec.valueMg
            is MedicinePreparation.PatchSpecification.ReleaseRateMcgPerDay -> 0.0
            null -> 0.0
        }
        // Non-patch routes: prefer the log snapshot; re-derive for planned entries.
        else -> snapshotEquivalentE2Mg
            ?: medicine?.let {
                DoseInstructionCalculator.perUnitEquivalentE2Mg(
                    it,
                    doseInstruction
                )
            }
            ?: 0.0
    }
    val releaseRateMcgPerDay =
        (patchSpec as? MedicinePreparation.PatchSpecification.ReleaseRateMcgPerDay)
            ?.takeIf { route == PkRoute.PATCH_APPLY }
            ?.valueMcgPerDay
            ?.times(multiplier)
    return PkDoseAmounts(
        doseMg = perUnitDoseMg * multiplier,
        releaseRateMcgPerDay = releaseRateMcgPerDay,
    )
}

private fun MedicationLogEntry.toEstradiolPkDoseEvent(
    anchor: Instant,
    isPlanned: Boolean = false,
): PkDoseEvent? {
    if (category != MedicationCategory.ESTRADIOL) {
        return null
    }
    val route = applicationType.toPkRoute()
    // PATCH_OFF now carries the singleton PatchOff medicine, but its selection
    // is PatchOff (not Catalog), so toEstradiolPkCompound returns null. The
    // simulator only needs *some* compound for the PATCH_REMOVE route and the
    // only patch today is estradiol, so we fall back to E2. Legacy log rows
    // with medicine == null follow the same path.
    val compound = medicine?.toEstradiolPkCompound()
        ?: if (route == PkRoute.PATCH_REMOVE) PkCompound.E2 else return null
    val amounts = pkDoseAmounts(
        medicine = medicine,
        doseInstruction = doseInstruction,
        route = route,
        snapshotEquivalentE2Mg = equivalentE2Mg,
        count = count,
    )
    return PkDoseEvent(
        id = uuid,
        sourceGroupUuid = sourceGroupUuid,
        hormone = PkHormone.ESTRADIOL,
        route = route,
        timeH = Duration.between(anchor, appliedAt).toMillis() / 3_600_000.0,
        doseMg = amounts.doseMg,
        compound = compound,
        releaseRateMcgPerDay = amounts.releaseRateMcgPerDay,
        areaCm2 = DefaultGelAreaCm2,
        isPlanned = isPlanned,
    )
}

private fun PkDoseEvent.isDoseMarkerCandidate(): Boolean {
    return route != PkRoute.PATCH_REMOVE && (doseMg > 0.0 || releaseRateMcgPerDay != null)
}

// PR3-UI: testosterone dose-event mapper. Mirrors
// toEstradiolPkDoseEvent but routes to the TESTOSTERONE engine path
// and reads the per-unit T mass from the appropriate preparation:
//   - ImportedInjection: prep.administeredMg
//   - InjectionSingleUseVial: prep.strengthMgPerVial
//   - InjectionMultiUseVial: concentration × vialVolume
//   - Custom (no ester pinned): falls back to PkCompound.T (the
//     "unspecified-ester" catch-all whose depot parameters sit at
//     the average of TE/TC/TU, see PkCatalog.twoPartDepotParams).
private fun MedicationLogEntry.toTestosteronePkDoseEvent(
    anchor: Instant,
    isPlanned: Boolean = false,
): PkDoseEvent? {
    if (category != MedicationCategory.TESTOSTERONE) {
        return null
    }
    val medicineRef = medicine ?: return null
    val route = applicationType.toPkRoute()
    val compound = medicineRef.toTestosteronePkCompound() ?: return null
    val perUnitDoseMg = when (val prep = medicineRef.preparation) {
        is MedicinePreparation.ImportedInjection -> prep.administeredMg
        is MedicinePreparation.InjectionSingleUseVial -> prep.strengthMgPerVial
        is MedicinePreparation.InjectionMultiUseVial ->
            prep.concentrationMgPerMl * prep.vialVolumeMl
        else -> 0.0
    }
    val multiplier = count.coerceAtLeast(1)
    return PkDoseEvent(
        id = uuid,
        sourceGroupUuid = sourceGroupUuid,
        hormone = PkHormone.TESTOSTERONE,
        route = route,
        timeH = Duration.between(anchor, appliedAt).toMillis() / 3_600_000.0,
        doseMg = perUnitDoseMg * multiplier,
        compound = compound,
        areaCm2 = DefaultGelAreaCm2,
        isPlanned = isPlanned,
    )
}

private fun Medicine.toTestosteronePkCompound(): PkCompound? {
    return when (val prep = preparation) {
        is MedicinePreparation.ImportedInjection -> prep.ester.toTestosteronePkCompound()
        // F-PR2: catalog-selected T esters / gel. The catalog key drives
        // the depot kinetics, so a TESTOSTERONE_ENANTHATE catalog
        // medicine routes to TE instead of the generic-T fallback. T
        // gel uses the same PkCompound.T (the engine treats all T
        // transdermal gel paths identically) but is selected here to
        // keep the enum→compound mapping uniform.
        else -> (selection as? MedicineSelection.Catalog)
            ?.medicationKey
            ?.toTestosteronePkCompound()
    }
}

private fun MedicationKey.toTestosteronePkCompound(): PkCompound? {
    // F-PR2: per-ester PkCompound mapping. Each ester carries distinct
    // depot kinetics (fracFast / k1Fast / k1Slow) in PkCatalog.twoPartDepot,
    // so a TE entry gives a 5-7 day functional half-life, TC ~7-8 days,
    // and TU ~10-14 weeks (Nebido). TESTOSTERONE_GEL and the catch-all
    // custom-T path both fall through to PkCompound.T (which uses the
    // TE/TC/TU average as a reasonable single-ester approximation).
    return when (this) {
        MedicationKey.TESTOSTERONE_ENANTHATE -> PkCompound.TE
        MedicationKey.TESTOSTERONE_CYPIONATE -> PkCompound.TC
        MedicationKey.TESTOSTERONE_UNDECANOATE -> PkCompound.TU
        // GEL + custom-T: T (androgel kinetics via the T compound's
        // gelFMax + k1 path). Future T undecanoate-oral-androgen
        // (Jatenzo) would land in its own compound; out of v1 scope.
        MedicationKey.TESTOSTERONE_GEL -> PkCompound.T
        else -> PkCompound.T
    }
}

// PR4: cyproterone acetate dose-event mapper. CPA lives in
// MedicationCategory.ANTIANDROGEN (alongside spironolactone, bicalutamide,
// finasteride, dutasteride), so the entry-level filter is the wider
// antiandrogen category — toCpaPkCompound() then narrows to the single
// MedicationKey that maps to CPA and drops every other antiandrogen. This
// keeps spironolactone / bicalutamide / etc. from accidentally feeding the
// CPA engine when the user flips the chart selector.
private fun MedicationLogEntry.toCpaPkDoseEvent(
    anchor: Instant,
    isPlanned: Boolean = false,
): PkDoseEvent? {
    if (category != MedicationCategory.ANTIANDROGEN) {
        return null
    }
    val medicineRef = medicine ?: return null
    val compound = medicineRef.toCpaPkCompound() ?: return null
    val route = applicationType.toPkRoute()
    // CPA oral tablets are the only clinical path; per-unit mass is the
    // tablet strength. Capsule fallback covers dutasteride if a future
    // product ever ships CPA in a capsule form.
    val perUnitDoseMg = when (val prep = medicineRef.preparation) {
        is MedicinePreparation.Pill -> prep.strengthMgPerTablet
        is MedicinePreparation.Capsule -> prep.strengthMgPerCapsule
        else -> 0.0
    }
    val multiplier = count.coerceAtLeast(1)
    return PkDoseEvent(
        id = uuid,
        sourceGroupUuid = sourceGroupUuid,
        hormone = PkHormone.CYPROTERONE_ACETATE,
        route = route,
        timeH = Duration.between(anchor, appliedAt).toMillis() / 3_600_000.0,
        doseMg = perUnitDoseMg * multiplier,
        compound = compound,
        areaCm2 = DefaultGelAreaCm2,
        isPlanned = isPlanned,
    )
}

private fun Medicine.toCpaPkCompound(): PkCompound? {
    return when (val prep = preparation) {
        is MedicinePreparation.ImportedInjection -> null
        else -> (selection as? MedicineSelection.Catalog)
            ?.medicationKey
            ?.toCpaPkCompound()
    }
}

private fun MedicationKey.toCpaPkCompound(): PkCompound? {
    return when (this) {
        MedicationKey.CYPROTERONE_ACETATE -> PkCompound.CPA
        else -> null
    }
}

private fun MedicationApplicationType.toPkRoute(): PkRoute {
    return when (this) {
        MedicationApplicationType.ORAL -> PkRoute.ORAL
        MedicationApplicationType.SUBLINGUAL -> PkRoute.SUBLINGUAL
        MedicationApplicationType.INJECTION -> PkRoute.INJECTION
        MedicationApplicationType.GEL -> PkRoute.GEL
        MedicationApplicationType.PATCH_ON -> PkRoute.PATCH_APPLY
        MedicationApplicationType.PATCH_OFF -> PkRoute.PATCH_REMOVE
    }
}

private fun Medicine.toEstradiolPkCompound(): PkCompound? {
    return when (val currentPreparation = preparation) {
        is MedicinePreparation.ImportedInjection -> currentPreparation.ester.toEstradiolPkCompound()
        is MedicinePreparation.ImportedGel -> PkCompound.E2
        else -> (selection as? MedicineSelection.Catalog)
            ?.medicationKey
            ?.toEstradiolPkCompound()
    }
}

private fun MedicationKey.toEstradiolPkCompound(): PkCompound? {
    return when (this) {
        MedicationKey.ESTRADIOL -> PkCompound.E2
        MedicationKey.ESTRADIOL_VALERATE -> PkCompound.EV
        MedicationKey.ESTRADIOL_BENZOATE -> PkCompound.EB
        MedicationKey.ESTRADIOL_CYPIONATE -> PkCompound.EC
        MedicationKey.ESTRADIOL_ENANTHATE -> PkCompound.EN
        MedicationKey.ESTRADIOL_UNDECYLATE -> PkCompound.EU
        MedicationKey.ESTRADIOL_GEL -> PkCompound.E2
        MedicationKey.ESTRADIOL_PATCH -> PkCompound.E2
        else -> null
    }
}

private const val DefaultGelAreaCm2 = 750.0

data class HormoneCoreParams(
    val vdPerKg: Double,
    val kClear: Double,
    val kClearInjection: Double,
    val depotK1Correction: Double,
    val patchFallbackK1: Double,
    val gelK1: Double,
    val gelFMax: Double,
    // v3 血检追踪 (PR-V3-TU): 3-compartment mammillary distribution
    // parameters. All default to 0.0, which keeps the legacy
    // 1-compartment behavior (the model collapses the shallow +
    // deep peripheral into the central Vd). When the per-compound
    // catalog entry sets shallowVdPerKg + deepVdPerKg + k12/k13/k21/k31
    // to non-zero, the engine switches to a 3-compartment ODE
    // integration: depot (1st-order release) + central (with
    // elimination) + shallow peripheral + deep peripheral. This
    // is what gives TU its observed 7-10d Tmax and 90+ d apparent
    // terminal t½ (the deep peripheral fat reservoir), which the
    // 1-comp Bateman cannot reproduce.
    val shallowVdPerKg: Double = 0.0,
    val deepVdPerKg: Double = 0.0,
    val k12: Double = 0.0,
    val k21: Double = 0.0,
    val k13: Double = 0.0,
    val k31: Double = 0.0,
)

/**
 * v3 血检追踪 (PR-V3-TU): per-compound override of the 3-compartment
 * distribution parameters. The 1-comp engine treats all T esters the
 * same (per HormoneCoreParams). For compounds with a non-null
 * override here, the engine switches to a 3-comp ODE integration
 * for that compound's events.
 *
 * Currently only TU uses this; TE/TC are well-fit by the 1-comp
 * model and stay on it.
 */
data class ThreeCompOverride(
    val centralVdPerKg: Double,
    val shallowVdPerKg: Double,
    val deepVdPerKg: Double,
    val k12: Double,
    val k21: Double,
    val k13: Double,
    val k31: Double,
)

/**
 * PR2 (PR-GEL): per-route 2-compartment skin depot override.
 * The 1-comp gel model assumes linear first-order absorption from
 * a virtual "depot at t=0" (no skin reservoir), which under-predicts
 * AndroGel Cavg by 38-57% (skin absorption is saturable). This
 * override adds an explicit skin depot state with two parallel
 * processes: (a) saturable Michaelis-Menten absorption into central
 * plasma, (b) first-order skin clearance (drug that's rubbed off,
 * washed off, or otherwise lost to the environment before
 * absorption). The MM absorption is what gives the non-linear
 * dose-response; the k_cl term is what stops the skin reservoir
 * from accumulating indefinitely.
 *
 * Today only GEL uses this; oral / sublingual stay on 1-comp.
 *
 * Parameters:
 * - Vmax: max transfer rate from skin to central (mg/h). Above
 *   this rate the skin barrier is the bottleneck.
 * - Km:   Michaelis constant (mg in skin) — amount of T in the
 *   skin at which the absorption rate is Vmax/2. A high Km (vs
 *   typical dose) means the system stays in the linear regime; a
 *   low Km means it saturates quickly.
 * - kCl:  first-order skin clearance (h⁻¹) — drug lost to the
 *   environment without being absorbed. Without this term the
 *   skin would accumulate forever (input > max possible output).
 */
data class SkinDepotOverride(
    val Vmax: Double,
    val Km: Double,
    val kCl: Double,
)

/**
 * PR2 (PR-GEL): RK4 state for the 2-comp skin depot ODE.
 * `skin` is the unabsorbed drug remaining in the skin (mg),
 * `central` is the amount in the central plasma compartment (mg).
 * The next RK4 step's input is a pulse (when t crosses a gel
 * application event), which is added directly to `skin`.
 */
private data class TwoCompSkinState(
    val skin: Double,
    val central: Double,
) {
    companion object {
        val ZERO = TwoCompSkinState(0.0, 0.0)
    }
}

private data class CompoundInfo(
    val hormone: PkHormone,
    val molecularWeight: Double,
    val activeMolecularWeight: Double,
) {
    val activeFactor: Double
        get() = activeMolecularWeight / molecularWeight
}

data class TwoPartDepotParams(
    val fracFast: Double,
    val k1Fast: Double,
    val k1Slow: Double,
)

object PkCatalog {
    const val standardSublingualTheta: Double = 0.11
    const val kAbsSublingual: Double = 1.8

    private val coreParams = mapOf(
        PkHormone.ESTRADIOL to HormoneCoreParams(
            vdPerKg = 2.0,
            kClear = 0.41,
            kClearInjection = 0.041,
            depotK1Correction = 1.0,
            patchFallbackK1 = 0.0075,
            gelK1 = 0.022,
            gelFMax = 0.06,
        ),
        PkHormone.TESTOSTERONE to HormoneCoreParams(
            // PR3 retune: testosterone esters were running 8-15x above
            // the Behre 1986 / Slayback FDA / Nebido SmPC literature
            // anchors because kClearInjection was 0.03 (t½ ≈ 23h),
            // which is the slow "deep compartment" central-clearance
            // constant borrowed from the E2 estrogen path. Free
            // testosterone's actual central clearance half-life is
            // ~1 hour (DrugBank). Setting kClearInjection = 0.05 h⁻¹
            // (t½ ≈ 14h) is the flip-flop sweet spot: the depot
            // k1Slow (T 0.0069 h⁻¹, t½ ≈ 100h) dominates the apparent
            // terminal t½ while the central elimination sits between
            // the depot release and the IV clearance, landing the
            // modelled Tmax at 48-72h (vs literature 60-72h) without
            // burning through the dose in the first day.
            vdPerKg = 2.0,
            kClear = 0.6,
            kClearInjection = 0.05,
            depotK1Correction = 1.0,
            patchFallbackK1 = 0.05,
            gelK1 = 0.03,
            // PR3 retune: AndroGel SPC reports ~10% transdermal
            // absorption (gelFMax=0.12 was an over-estimate). Drop
            // to 0.10 to match the 5g/10g Cavg anchors (5.66 / 7.92
            // ng/mL respectively) within 5%.
            gelFMax = 0.10,
        ),
        PkHormone.CYPROTERONE_ACETATE to HormoneCoreParams(
            // CPA is the active drug itself; only the oral Bateman path is
            // meaningful today, so depot / patch / gel carry zero weight.
            //
            // kClear: terminal t½ ≈ 38 h for single-dose CPA in young
            // subjects (Androcur SPC: "the mean plasma half life is
            // about 2 days"). Long-term t½ rises to ~78 h (Kuhnz 1993
            // cycle 3) due to deep-compartment equilibration and fat
            // sequestration, but the single-dose value is the right
            // anchor for our 1-compartment Bateman. kClear = ln(2) / 38h
            // ≈ 0.01825.
            //
            // Vd: the apparent Vz of 986 L from a 2 mg Diane-35 single
            // dose (Kuhnz 1993) is heavily influenced by deep-compartment
            // equilibration, not the steady-state Vd. Pharmacology
            // reviews report a steady-state Vd of ~20 L/kg, but the
            // single-dose central-compartment Vd back-solves to ~3 L/kg
            // when matched against the 100 mg single-dose Pharmindex data
            // (Cmax ≈ 254 ng/mL @ 2.6 h with F=0.88, ka=0.46, t½=38 h).
            // This dose-dependent shift in apparent Vd is a known feature
            // of CPA pharmacology. We use vdPerKg=3 here so single-dose
            // and short-term trough values match the SPC; long-term
            // steady-state will tend to under-predict because of the
            // Vd shift.
            vdPerKg = 3.0,
            kClear = 0.01825,
            kClearInjection = 0.01825,
            depotK1Correction = 1.0,
            patchFallbackK1 = 0.0,
            gelK1 = 0.0,
            gelFMax = 0.0,
        ),
    )

    private val compounds = mapOf(
        PkCompound.E2 to CompoundInfo(PkHormone.ESTRADIOL, 272.38, 272.38),
        PkCompound.EB to CompoundInfo(PkHormone.ESTRADIOL, 376.5, 272.38),
        PkCompound.EV to CompoundInfo(PkHormone.ESTRADIOL, 356.5, 272.38),
        PkCompound.EC to CompoundInfo(PkHormone.ESTRADIOL, 396.58, 272.38),
        PkCompound.EN to CompoundInfo(PkHormone.ESTRADIOL, 384.56, 272.38),
        PkCompound.EU to CompoundInfo(PkHormone.ESTRADIOL, 440.66, 272.38),
        PkCompound.T to CompoundInfo(PkHormone.TESTOSTERONE, 288.42, 288.42),
        PkCompound.TC to CompoundInfo(PkHormone.TESTOSTERONE, 412.61, 288.42),
        PkCompound.TE to CompoundInfo(PkHormone.TESTOSTERONE, 400.59, 288.42),
        PkCompound.TU to CompoundInfo(PkHormone.TESTOSTERONE, 456.7, 288.42),
        // CPA molecular weight 416.94 g/mol (cyproterone 357.45 + acetic
        // acid 59.04). activeMolecularWeight = molecularWeight because CPA
        // is the active moiety, not a pro-drug ester.
        PkCompound.CPA to CompoundInfo(PkHormone.CYPROTERONE_ACETATE, 416.94, 416.94),
    )

    private val twoPartDepot = mapOf(
        // PR3.5 retune: EB had model Cmax ~1818 pg/mL @ 24h vs Oriowo
        // 1980 940 pg/mL @ 1.8d (93% over). Slowing both k1 channels
        // moves the peak to 36h and combined with the lower formation
        // fraction brings Cmax into the 850-1050 pg/mL band.
        PkCompound.EB to TwoPartDepotParams(fracFast = 0.6, k1Fast = 0.078, k1Slow = 0.060),
        PkCompound.EV to TwoPartDepotParams(fracFast = 0.4, k1Fast = 0.0216, k1Slow = 0.0138),
        PkCompound.EC to TwoPartDepotParams(
            fracFast = 0.229164549,
            k1Fast = 0.005035046,
            k1Slow = 0.004510574
        ),
        PkCompound.EN to TwoPartDepotParams(fracFast = 0.05, k1Fast = 0.001, k1Slow = 0.005),
        // Transmtf im-depot-v1: EU is a single rate-limiting slow depot.
        PkCompound.EU to TwoPartDepotParams(fracFast = 0.0, k1Fast = 0.00082, k1Slow = 0.00082),
        PkCompound.TC to TwoPartDepotParams(fracFast = 0.4, k1Fast = 0.018, k1Slow = 0.0045),
        PkCompound.TE to TwoPartDepotParams(fracFast = 0.45, k1Fast = 0.02, k1Slow = 0.0055),
        PkCompound.TU to TwoPartDepotParams(fracFast = 0.25, k1Fast = 0.005, k1Slow = 0.0012),
        // PR3-UI: PkCompound.T is the "unspecified-ester testosterone" catch-all
        // for custom T injections (InjectionSingleUseVial / MultiUseVial where
        // the user logged a generic T dose without picking TE/TC/TU).
        // It re-uses the average of the three ester depot parameters so
        // the curve shape still matches a typical T ester (~5-7 day
        // functional half-life).
        PkCompound.T to TwoPartDepotParams(fracFast = 0.4, k1Fast = 0.02, k1Slow = 0.005),
    )

    private val formationFraction = mapOf(
        // PR3.5 retune: EB 0.109 → 0.060 cuts Cmax in half to align with
        // Oriowo 1980 (940 pg/mL). EV/EC/EN/EU left untouched (already
        // within ±30% of literature).
        PkCompound.EB to 0.06,
        PkCompound.EV to 0.062258288229969413,
        PkCompound.EC to 0.117255838,
        PkCompound.EN to 0.12,
        PkCompound.EU to 0.542,
        // PR3.5 retune: per-ester formation fractions. PR3 used a
        // single 0.07 across TE/TC/TU which landed the steady-state
        // Cmax within ±15% of Behre 1986 / Slayback FDA / Nebido SmPC,
        // but the v2 血检 clinical-anchor report (981c377) found the
        // model was still over-predicting by 35-56% across all three
        // esters and 46h early on the TU Tmax. Per-ester values
        // reflect the documented bioavailability ranking (TU has
        // the lowest effective F because the oil vehicle traps a
        // larger fraction of the dose) and the depot's own per-ester
        // k1Slow curve-shape spread.
        PkCompound.TC to 0.055,
        PkCompound.TE to 0.060,
        PkCompound.TU to 0.060,
        // PR3-UI: PkCompound.T (unspecified-ester T, custom T vials)
        // re-uses the average of the three labeled esters to keep
        // custom-T curves in the same Cmax band.
        PkCompound.T to 0.058,
    )

    private val hydrolysisK2 = mapOf(
        PkCompound.EB to 0.09,
        PkCompound.EV to 0.07,
        PkCompound.EC to 0.045,
        PkCompound.EN to 0.015,
        PkCompound.EU to 2.0,
        PkCompound.TC to 0.09,
        PkCompound.TE to 0.12,
        PkCompound.TU to 0.03,
    )

    private val oralKAbs = mapOf(
        PkCompound.E2 to 0.32,
        PkCompound.EV to 0.05,
        PkCompound.TU to 0.04,
        // CPA: oral absorption half-life ≈ 1.5h (Pharmacology of CPA,
        // wiki/Pharmindex), so kAbs = ln(2) / 1.5h ≈ 0.462. The
        // SPC's "tmax ≈ 3 h" is the early rising value, not the analytic
        // Bateman peak (which lands at ~7 h under these parameters).
        PkCompound.CPA to 0.46,
    )

    private val oralBioavailability = mapOf(
        PkCompound.E2 to 0.03,
        PkCompound.EV to 0.03,
        PkCompound.TU to 0.03,
        // CPA: absolute oral bioavailability is ~88% (Diane-35 SPC). Oral
        // first-pass metabolism is mild compared to estradiol.
        PkCompound.CPA to 0.88,
    )

    fun coreParams(hormone: PkHormone): HormoneCoreParams = coreParams.getValue(hormone)

    fun defaultConcentrationUnit(hormone: PkHormone): PkConcentrationUnit {
        return when (hormone) {
            PkHormone.ESTRADIOL -> PkConcentrationUnit.PG_PER_ML
            PkHormone.TESTOSTERONE -> PkConcentrationUnit.NG_PER_DL
            // CPA clinical concentrations are reported in ng/mL (steady-state
            // ~260 ng/mL at 50 mg/day, Androcur SPC), and the BloodTestCatalog
            // already exposes NG_PER_ML as a unit. PG_PER_ML would be off by
            // a factor of 1000 and is not used in clinical reporting.
            PkHormone.CYPROTERONE_ACETATE -> PkConcentrationUnit.NG_PER_ML
        }
    }

    fun molecularWeight(hormone: PkHormone): Double {
        val analyte = when (hormone) {
            PkHormone.ESTRADIOL -> PkCompound.E2
            PkHormone.TESTOSTERONE -> PkCompound.T
            PkHormone.CYPROTERONE_ACETATE -> PkCompound.CPA
        }
        return compounds.getValue(analyte).activeMolecularWeight
    }

    fun activeFactor(compound: PkCompound): Double = compounds.getValue(compound).activeFactor

    fun twoPartDepotParams(compound: PkCompound): TwoPartDepotParams? = twoPartDepot[compound]

    fun formationFraction(compound: PkCompound): Double? = formationFraction[compound]

    fun hydrolysisK2(compound: PkCompound): Double? = hydrolysisK2[compound]

    fun oralKAbs(compound: PkCompound): Double? = oralKAbs[compound]

    fun oralBioavailability(compound: PkCompound): Double? = oralBioavailability[compound]

    /**
     * v3 血检追踪 (PR-V3-TU) + v3.1 retune: per-compound
     * 3-compartment distribution override. Non-null only for TU;
     * other compounds keep the 1-compartment engine.
     *
     * TU values (calibrated to the Nebido SmPC anchor — 11-15 ng/mL
     * Cmax @ 7d, 33-50 nmol/L = 11-15 ng/mL; apparent terminal
     * t½ 90 ± 40 d):
     *
     * - Vd_central = 2.0 L/kg (same as 1-comp vdPerKg)
     * - Vd_shallow = 1.0 L/kg (muscle, fast exchange k12=0.15, k21=0.18)
     * - Vd_deep    = 3.5 L/kg (fat, slow exchange k13=0.010, k31=0.0003)
     *   — the deep peripheral is what gives TU its 90 ± 40 d
     *   apparent terminal t½ (the fat reservoir releases drug
     *   back to central very slowly). v3.1 retune: k31 dropped
     *   from 0.005 (t½ ≈ 5.8 d) to 0.0003 (t½ ≈ 90 d) so the
     *   terminal phase actually matches the Nebido SmPC
     *   90 ± 40 d, instead of pretending the 1-comp model could
     *   represent it. The 17× lower k31 has minimal effect on
     *   the peak (deep-to-central flux is small during the
     *   first 4 weeks either way) and a major effect on the
     *   long-term residual.
     */
    private val threeCompOverrides = mapOf(
        PkCompound.TU to ThreeCompOverride(
            // Vd_central = 2.0 L/kg (same as 1-comp vdPerKg) —
            // the 3-comp engine's central Vd is the apparent Vd
            // for the bioavailable fraction; the shallow and
            // deep peripherals hold the extravascular depot
            // reservoir. Using 2.0 here (rather than a smaller
            // "true" central Vd) preserves the 1-comp's
            // calibrated Cmax band while the ODE adds the
            // slow terminal decay (deep peripheral k31) the
            // 1-comp couldn't capture.
            centralVdPerKg = 2.0,
            shallowVdPerKg = 1.0,
            deepVdPerKg = 3.5,
            k12 = 0.15,
            k21 = 0.18,
            k13 = 0.010,
            // v3.1 retune: t½(deep→central) = ln(2)/k31 = 90 d
            // (was 5.8 d at k31=0.005). Matches the Nebido
            // SmPC 90 ± 40 d apparent terminal t½.
            k31 = 0.0003,
        ),
    )

    fun threeCompOverride(compound: PkCompound): ThreeCompOverride? = threeCompOverrides[compound]

    /**
     * PR2 (PR-GEL): per-route skin-depot Michaelis-Menten
     * parameters. Non-null only for the GEL route today; the
     * 1-comp gel path stays as the legacy fallback for any other
     * route that needs a depot-style release in the future.
     *
     * Calibrated against the AndroGel 1% SPC Cavg band (Day 30
     * steady state, 5 g/day → 3.04-8.28 ng/mL; 10 g/day →
     * 4.98-10.86 ng/mL). With body weight 70 kg and the
     * testosterone Vd_c of 2.0 L/kg, the daily absorbed
     * testosterone needed to hit the SPC midpoints is:
     *
     * - 5 g/day  → 11.4 mg/day  (avg rate 0.475 mg/h)
     * - 10 g/day → 15.96 mg/day (avg rate 0.665 mg/h)
     *
     * The skin loads ~50 mg (5 g) or ~100 mg (10 g) per
     * application and drains over 24 h. Solving
     * Vmax * A_skin / (Km + A_skin) = avg rate for the
     * mid-application A_skin (44.3 mg for 5 g, 92 mg for 10 g)
     * gives Vmax ≈ 1.058 mg/h, Km ≈ 54 mg.
     *
     * The non-linearity the 1-comp model can't capture shows
     * up as 5 g absorbing 22.8% of dose vs 10 g absorbing 16%
     * — the skin barrier saturates, so doubling the dose
     * gives less than 2× the systemic level. This is the
     * dose-response curve reported in the SPC.
     */
    private val skinDepotOverrides = mapOf(
        PkRoute.GEL to SkinDepotOverride(
            // Calibrated against AndroGel 1% SPC Cavg band
            // (Day 30 steady state, 5 g/day → 3.04-8.28 ng/mL;
            // 10 g/day → 4.98-10.86 ng/mL):
            // - Vmax = 1.3 mg/h: saturable skin-to-central
            //   transfer rate.
            // - Km   = 30 mg: Michaelis constant. 5 g dose
            //   loads ~50 mg → initial rate ≈ 1.3*50/80 = 0.81
            //   mg/h. 10 g loads ~100 mg → rate ≈ 1.3*100/130 =
            //   1.0 mg/h. The ratio 1.0/0.81 = 1.23 (vs linear
            //   2×) is the non-linearity.
            // - kCl  = 0.1 h⁻¹: first-order skin clearance
            //   (rubbed off, washed off, etc.). Prevents the
            //   skin reservoir from growing without bound.
            Vmax = 1.3,
            Km = 30.0,
            kCl = 0.1,
        ),
    )

    fun skinDepotOverride(route: PkRoute): SkinDepotOverride? = skinDepotOverrides[route]
}
