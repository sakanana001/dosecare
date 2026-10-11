package com.dosecare.app.model.bloodtest

import com.dosecare.app.model.settings.HrtGoal

/**
 * Closed numeric interval (inclusive on both ends) for a single
 * reference range band. We use [ClosedFloatingPointRange] directly
 * rather than introducing our own Pair/Range type so callers can
 * `low..high in this` and rely on the standard library's `contains`
 * semantics.
 */
typealias ReferenceRange = ClosedFloatingPointRange<Double>

/**
 * Goal-aware reference ranges and Hb-specific polycythemia / anemia
 * warning thresholds for the built-in blood analytes.
 *
 * v2 血检追踪 (BT-PR1): the per-(goal, analyte) numbers are
 * intentionally broad clinical anchors, not lab-specific cutoffs.
 * They power two surfaces:
 *
 * - The main screen's T trend card draws the "normal" range as a
 *   green band behind the trend line, so a glance at the chart shows
 *   whether the user is in or out of range.
 * - The main screen's Hb chip classifies the most recent Hb value
 *   into a severity (NORMAL / LOW / HIGH / CRITICAL) using the
 *   per-goal cutoffs below.
 *
 * The numbers are conservative defaults derived from general HRT
 * clinical guidance; they are not a substitute for the user's lab
 * reference range, and the chip deliberately says "above lab range"
 * / "below lab range" rather than "dangerous" / "fine" to keep the
 * framing non-prescriptive.
 */
data class BloodAnalyteReferenceProfile(
    /** "In range" band drawn on the trend chart. */
    val normalRange: ReferenceRange,
    /** Below this is the LOW / anemia direction. Null means the analyte has no meaningful low cutoff. */
    val lowThreshold: Double?,
    /** Above this is the HIGH / polycythemia-watch direction. Null means no meaningful high cutoff. */
    val highThreshold: Double?,
    /** Above this is the CRITICAL direction. Null means no critical cutoff. */
    val criticalThreshold: Double?,
)

/**
 * Returns the reference profile for [analyteKey] under [goal].
 *
 * - T: range is per-goal (suppressed for MtF, masculine for FtM,
 *   mid for non-binary, unisex for unset).
 * - Hb: per-goal cutoffs. T users (FtM) get tighter low + high
 *   watch (polycythemia risk); E2 users (MtF) get a low
 *   (anemia risk) + high watch.
 * - All other built-in analytes: null (no range logic in v2).
 *
 * The values are in the analyte's canonical unit (ng/dL for T,
 * g/dL for Hb). Callers should normalize via
 * [BloodTestCatalog.canonicalUnitFor] before classifying.
 */
fun bloodAnalyteReferenceProfileFor(
    analyteKey: BloodAnalyteKey,
    goal: HrtGoal,
): BloodAnalyteReferenceProfile? {
    return when (analyteKey) {
        BloodAnalyteKey.T -> testosteroneReferenceProfileFor(goal)
        BloodAnalyteKey.HEMOGLOBIN -> hemoglobinReferenceProfileFor(goal)
        // No reference range surfaced for E2 / PROG / PRL / FSH / LH in v2;
        // each has its own clinical context that warrants separate modeling
        // and we don't want to ship a chart band with a misleading
        // "normal range" derived from population averages.
        else -> null
    }
}

/**
 * Per-goal T reference profile. Canonical unit = ng/dL.
 *
 * - MTF: 8-60 ng/dL is the typical suppressed-T target on E2 +
 *   antiandrogen. <8 is over-suppressed, >60 is breakthrough.
 * - FTM: 300-1000 ng/dL is the typical masculine target on T.
 *   <300 is under-dosed, >1000 is supra-physiological.
 * - NON_BINARY: 50-200 ng/dL covers a low-dose T regimen range.
 * - UNSET: unisex middle 200-800 ng/dL.
 */
private fun testosteroneReferenceProfileFor(goal: HrtGoal): BloodAnalyteReferenceProfile {
    return when (goal) {
        HrtGoal.MTF -> BloodAnalyteReferenceProfile(
            normalRange = 8.0..60.0,
            lowThreshold = null, // below 8 is "very suppressed" but not clinically alarming
            highThreshold = 60.0,
            criticalThreshold = 200.0,
        )
        HrtGoal.FTM -> BloodAnalyteReferenceProfile(
            normalRange = 300.0..1000.0,
            lowThreshold = 200.0,
            highThreshold = 1000.0,
            criticalThreshold = 1500.0,
        )
        HrtGoal.NON_BINARY -> BloodAnalyteReferenceProfile(
            normalRange = 50.0..200.0,
            lowThreshold = null,
            highThreshold = 200.0,
            criticalThreshold = 1000.0,
        )
        HrtGoal.UNSET -> BloodAnalyteReferenceProfile(
            normalRange = 200.0..800.0,
            lowThreshold = null,
            highThreshold = 1000.0,
            criticalThreshold = 1500.0,
        )
    }
}

/**
 * Per-goal hemoglobin reference profile. Canonical unit = g/dL.
 *
 * The polycythemia direction (high) is the one most clinically
 * actionable for T users (FtM); the anemia direction (low) is the
 * one most clinically actionable for E2 users (MtF). Both
 * directions are surfaced so the chip can flag the relevant
 * direction for the user's regimen.
 */
private fun hemoglobinReferenceProfileFor(goal: HrtGoal): BloodAnalyteReferenceProfile {
    return when (goal) {
        HrtGoal.MTF -> BloodAnalyteReferenceProfile(
            // E2-suppressed T users: Hb tends to track toward female ranges
            normalRange = 12.0..16.0,
            lowThreshold = 12.0,
            highThreshold = 16.5,
            criticalThreshold = 18.0,
        )
        HrtGoal.FTM -> BloodAnalyteReferenceProfile(
            // T users: Hb tends to track toward male ranges; polycythemia is the risk
            normalRange = 13.5..17.5,
            lowThreshold = 12.0,
            highThreshold = 17.5,
            criticalThreshold = 18.0,
        )
        HrtGoal.NON_BINARY -> BloodAnalyteReferenceProfile(
            // Unisex range, polycythemia-watch threshold mirrors FtM
            normalRange = 12.0..17.5,
            lowThreshold = 12.0,
            highThreshold = 17.5,
            criticalThreshold = 18.0,
        )
        HrtGoal.UNSET -> BloodAnalyteReferenceProfile(
            // Fresh install: unisex range, identical to NON_BINARY
            normalRange = 12.0..17.5,
            lowThreshold = 12.0,
            highThreshold = 17.5,
            criticalThreshold = 18.0,
        )
    }
}
