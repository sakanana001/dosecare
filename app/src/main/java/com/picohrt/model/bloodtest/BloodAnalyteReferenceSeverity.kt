package com.picohrt.model.bloodtest

import com.picohrt.model.settings.HrtGoal

/**
 * Classification of a single blood analyte value against a
 * per-goal reference profile. Used by the main screen Hb chip and
 * the T trend card to decide the chip color and the "in range" /
 * "out of range" label.
 *
 * - NORMAL: value is within the [BloodAnalyteReferenceProfile.normalRange] band.
 * - LOW: value is below the [BloodAnalyteReferenceProfile.lowThreshold] (if set).
 * - HIGH: value is above the [BloodAnalyteReferenceProfile.highThreshold] (if set)
 *   but below the critical threshold.
 * - CRITICAL: value is above the [BloodAnalyteReferenceProfile.criticalThreshold] (if set).
 * - UNKNOWN: no reference profile exists for this (analyte, goal) pair, or the value
 *   is non-finite (NaN / infinity).
 */
enum class BloodAnalyteReferenceSeverity {
    NORMAL,
    LOW,
    HIGH,
    CRITICAL,
    UNKNOWN;

    /**
     * Whether the chip should show a colored badge (LOW / HIGH /
     * CRITICAL) vs a neutral one (NORMAL / UNKNOWN). The Hb chip
     * uses this to switch from the soft "in range" state to the
     * amber/red "out of range" state.
     */
    val isOutOfRange: Boolean
        get() = this == LOW || this == HIGH || this == CRITICAL
}

/**
 * Classify [canonicalValue] of [analyteKey] for [goal] against the
 * per-goal reference profile.
 *
 * Callers must pass a value in the analyte's canonical unit (e.g.
 * ng/dL for T, g/dL for Hb). The classifier does not convert units
 * — that's a separate concern handled by [BloodTestCatalog.toCanonical].
 * Non-finite values always classify as UNKNOWN.
 */
fun classifyBloodAnalyteValue(
    analyteKey: BloodAnalyteKey,
    goal: HrtGoal,
    canonicalValue: Double,
): BloodAnalyteReferenceSeverity {
    if (!canonicalValue.isFinite()) return BloodAnalyteReferenceSeverity.UNKNOWN
    val profile = bloodAnalyteReferenceProfileFor(analyteKey, goal)
        ?: return BloodAnalyteReferenceSeverity.UNKNOWN
    return classifyAgainstProfile(canonicalValue, profile)
}

private fun classifyAgainstProfile(
    value: Double,
    profile: BloodAnalyteReferenceProfile,
): BloodAnalyteReferenceSeverity {
    // CRITICAL takes priority over HIGH so a supra-physiological
    // value still surfaces the louder "critical" label even when
    // it's also above the high threshold.
    profile.criticalThreshold?.let { threshold ->
        if (value > threshold) return BloodAnalyteReferenceSeverity.CRITICAL
    }
    profile.highThreshold?.let { threshold ->
        if (value > threshold) return BloodAnalyteReferenceSeverity.HIGH
    }
    profile.lowThreshold?.let { threshold ->
        if (value < threshold) return BloodAnalyteReferenceSeverity.LOW
    }
    return if (value in profile.normalRange) {
        BloodAnalyteReferenceSeverity.NORMAL
    } else {
        // Value is outside the normal range but didn't trip any
        // explicit low/high threshold. This can happen when the
        // normal range is wider than the (low, high) thresholds
        // (e.g. UNSET T at 200-800 with no low cutoff but a high
        // cutoff at 1000). In that case the value is in the
        // "unspecified" gap between normal and out-of-range; we
        // classify it as NORMAL since it's inside the explicit
        // normal band.
        BloodAnalyteReferenceSeverity.NORMAL
    }
}
