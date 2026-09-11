package com.picohrt.model.bloodtest

import com.picohrt.model.settings.HrtGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodAnalyteReferenceRangesTest {
    @Test
    fun mtf_testosterone_range_is_suppressed() {
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.T, HrtGoal.MTF)!!

        assertEquals(8.0, profile.normalRange.start, 1e-9)
        assertEquals(60.0, profile.normalRange.endInclusive, 1e-9)
        assertNull("MtF T has no actionable low cutoff", profile.lowThreshold)
        assertEquals(60.0, profile.highThreshold!!, 1e-9)
        assertEquals(200.0, profile.criticalThreshold!!, 1e-9)
    }

    @Test
    fun ftm_testosterone_range_is_masculine() {
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.T, HrtGoal.FTM)!!

        assertEquals(300.0, profile.normalRange.start, 1e-9)
        assertEquals(1000.0, profile.normalRange.endInclusive, 1e-9)
        assertEquals(200.0, profile.lowThreshold!!, 1e-9)
        assertEquals(1000.0, profile.highThreshold!!, 1e-9)
        assertEquals(1500.0, profile.criticalThreshold!!, 1e-9)
    }

    @Test
    fun non_binary_testosterone_range_is_low_dose() {
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.T, HrtGoal.NON_BINARY)!!

        assertEquals(50.0, profile.normalRange.start, 1e-9)
        assertEquals(200.0, profile.normalRange.endInclusive, 1e-9)
    }

    @Test
    fun unset_testosterone_range_is_unisex() {
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.T, HrtGoal.UNSET)!!

        assertEquals(200.0, profile.normalRange.start, 1e-9)
        assertEquals(800.0, profile.normalRange.endInclusive, 1e-9)
    }

    @Test
    fun mtf_hemoglobin_profile_flags_anemia_and_polycythemia_watch() {
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF)!!

        assertEquals(12.0, profile.normalRange.start, 1e-9)
        assertEquals(16.0, profile.normalRange.endInclusive, 1e-9)
        assertEquals(12.0, profile.lowThreshold!!, 1e-9)
        assertEquals(16.5, profile.highThreshold!!, 1e-9)
        assertEquals(18.0, profile.criticalThreshold!!, 1e-9)
    }

    @Test
    fun ftm_hemoglobin_profile_flags_polycythemia_first() {
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.FTM)!!

        assertEquals(13.5, profile.normalRange.start, 1e-9)
        assertEquals(17.5, profile.normalRange.endInclusive, 1e-9)
        assertEquals(12.0, profile.lowThreshold!!, 1e-9)
        assertEquals(17.5, profile.highThreshold!!, 1e-9)
        assertEquals(18.0, profile.criticalThreshold!!, 1e-9)
    }

    @Test
    fun non_binary_and_unset_hemoglobin_share_unisex_profile() {
        val nonBinary = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.NON_BINARY)!!
        val unset = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.UNSET)!!

        assertEquals(nonBinary.normalRange.start, unset.normalRange.start, 1e-9)
        assertEquals(nonBinary.normalRange.endInclusive, unset.normalRange.endInclusive, 1e-9)
        assertEquals(nonBinary.highThreshold!!, unset.highThreshold!!, 1e-9)
    }

    @Test
    fun e2_has_no_reference_profile_in_v2() {
        assertNull(bloodAnalyteReferenceProfileFor(BloodAnalyteKey.E2, HrtGoal.MTF))
        assertNull(bloodAnalyteReferenceProfileFor(BloodAnalyteKey.E2, HrtGoal.FTM))
    }

    @Test
    fun prog_prl_fsh_lh_have_no_reference_profile_in_v2() {
        listOf(BloodAnalyteKey.PROG, BloodAnalyteKey.PRL, BloodAnalyteKey.FSH, BloodAnalyteKey.LH)
            .forEach { analyte ->
                assertNull(
                    "Expected no reference profile for $analyte in v2",
                    bloodAnalyteReferenceProfileFor(analyte, HrtGoal.MTF),
                )
            }
    }
}

class BloodAnalyteReferenceSeverityTest {
    @Test
    fun mtf_hb_in_normal_range_classifies_normal() {
        assertEquals(
            BloodAnalyteReferenceSeverity.NORMAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF, 14.0),
        )
    }

    @Test
    fun mtf_hb_below_low_threshold_classifies_low() {
        assertEquals(
            BloodAnalyteReferenceSeverity.LOW,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF, 11.5),
        )
    }

    @Test
    fun mtf_hb_above_high_threshold_classifies_high() {
        assertEquals(
            BloodAnalyteReferenceSeverity.HIGH,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF, 16.8),
        )
    }

    @Test
    fun mtf_hb_above_critical_threshold_classifies_critical() {
        assertEquals(
            BloodAnalyteReferenceSeverity.CRITICAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF, 18.5),
        )
    }

    @Test
    fun ftm_hb_at_high_threshold_is_still_high_not_critical() {
        // Critical check is strict-greater; 18.0 is exactly the critical
        // threshold so it stays HIGH (17.5 < 18.0 <= 18.0).
        assertEquals(
            BloodAnalyteReferenceSeverity.HIGH,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.FTM, 18.0),
        )
    }

    @Test
    fun ftm_hb_just_above_critical_classifies_critical() {
        assertEquals(
            BloodAnalyteReferenceSeverity.CRITICAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.FTM, 18.01),
        )
    }

    @Test
    fun ftm_hb_low_for_mtf_range_but_in_ftm_normal_classifies_normal() {
        // FTM normal range is 13.5-17.5; a 13.5 value is at the low edge
        // and is NORMAL (not LOW) because lowThreshold is 12.0 and
        // 13.5 >= 12.0.
        assertEquals(
            BloodAnalyteReferenceSeverity.NORMAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.FTM, 13.5),
        )
    }

    @Test
    fun ftm_t_in_normal_range_classifies_normal() {
        assertEquals(
            BloodAnalyteReferenceSeverity.NORMAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.T, HrtGoal.FTM, 500.0),
        )
    }

    @Test
    fun ftm_t_below_low_threshold_classifies_low() {
        assertEquals(
            BloodAnalyteReferenceSeverity.LOW,
            classifyBloodAnalyteValue(BloodAnalyteKey.T, HrtGoal.FTM, 150.0),
        )
    }

    @Test
    fun ftm_t_above_critical_threshold_classifies_critical() {
        assertEquals(
            BloodAnalyteReferenceSeverity.CRITICAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.T, HrtGoal.FTM, 1600.0),
        )
    }

    @Test
    fun mtf_t_above_high_but_below_critical_classifies_high() {
        assertEquals(
            BloodAnalyteReferenceSeverity.HIGH,
            classifyBloodAnalyteValue(BloodAnalyteKey.T, HrtGoal.MTF, 100.0),
        )
    }

    @Test
    fun mtf_t_above_critical_threshold_classifies_critical() {
        assertEquals(
            BloodAnalyteReferenceSeverity.CRITICAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.T, HrtGoal.MTF, 250.0),
        )
    }

    @Test
    fun non_binary_t_in_low_dose_band_classifies_normal() {
        assertEquals(
            BloodAnalyteReferenceSeverity.NORMAL,
            classifyBloodAnalyteValue(BloodAnalyteKey.T, HrtGoal.NON_BINARY, 100.0),
        )
    }

    @Test
    fun unknown_profile_returns_unknown() {
        // E2 has no v2 reference profile, so any value classifies as UNKNOWN
        assertEquals(
            BloodAnalyteReferenceSeverity.UNKNOWN,
            classifyBloodAnalyteValue(BloodAnalyteKey.E2, HrtGoal.MTF, 100.0),
        )
    }

    @Test
    fun non_finite_value_returns_unknown() {
        assertEquals(
            BloodAnalyteReferenceSeverity.UNKNOWN,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF, Double.NaN),
        )
        assertEquals(
            BloodAnalyteReferenceSeverity.UNKNOWN,
            classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.MTF, Double.POSITIVE_INFINITY),
        )
    }

    @Test
    fun isOutOfRange_is_true_for_low_high_critical_only() {
        assertTrue(BloodAnalyteReferenceSeverity.LOW.isOutOfRange)
        assertTrue(BloodAnalyteReferenceSeverity.HIGH.isOutOfRange)
        assertTrue(BloodAnalyteReferenceSeverity.CRITICAL.isOutOfRange)
        assertEquals(false, BloodAnalyteReferenceSeverity.NORMAL.isOutOfRange)
        assertEquals(false, BloodAnalyteReferenceSeverity.UNKNOWN.isOutOfRange)
    }
}
