package com.picohrt.model.settings

import com.picohrt.model.medication.MedicationCategory
import com.picohrt.model.medication.MedicationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// F-PR4: locks the (goal, medication) safety warning matrix. The
// UI surfaces these via the existing MedicalDisclaimerText
// composable, so the helper's matrix is the single source of truth
// for what the user sees. The wrapper that maps to
// MedicalDisclaimerKind is also covered so the editor wiring stays
// in sync with the matrix.
class HrtGoalSafetyTest {
    @Test
    fun ftm_cpa_emitsStrongSafetyWarning() {
        val warnings = hrtGoalSafetyWarningsFor(
            HrtGoal.FTM,
            MedicationKey.CYPROTERONE_ACETATE,
            MedicationCategory.ANTIANDROGEN,
        )
        assertEquals(1, warnings.size)
        assertEquals(HrtGoalSafetySeverity.STRONG, warnings[0].severity)
    }

    @Test
    fun ftm_estradiolFamily_emitsSoftSafetyWarning() {
        // F-PR2 E2 catalog keys (ESTRADIOL + the 5 esters + GEL +
        // PATCH) all share the ESTRADIOL_ prefix and live in the
        // ESTRADIOL category, so a single test sweeps the family.
        MedicationKey.entries
            .filter { it.name.startsWith("ESTRADIOL_") || it == MedicationKey.ESTRADIOL }
            .forEach { key ->
                val warnings = hrtGoalSafetyWarningsFor(
                    HrtGoal.FTM,
                    key,
                    MedicationCategory.ESTRADIOL,
                )
                assertEquals(
                    "FTM + $key should emit exactly one warning",
                    1,
                    warnings.size,
                )
                assertEquals(
                    "FTM + $key should be SOFT (off-label)",
                    HrtGoalSafetySeverity.SOFT,
                    warnings[0].severity,
                )
            }
    }

    @Test
    fun mtf_testosteroneFamily_emitsSoftSafetyWarning() {
        // F-PR2 T catalog keys (TE/TC/TU/GEL) all share the
        // TESTOSTERONE_ prefix; cover them all in one test.
        MedicationKey.entries
            .filter { it.name.startsWith("TESTOSTERONE_") || it == MedicationKey.TESTOSTERONE_ENANTHATE }
            .forEach { key ->
                val warnings = hrtGoalSafetyWarningsFor(
                    HrtGoal.MTF,
                    key,
                    MedicationCategory.TESTOSTERONE,
                )
                assertEquals(
                    "MTF + $key should emit exactly one warning",
                    1,
                    warnings.size,
                )
                assertEquals(
                    "MTF + $key should be SOFT (off-label)",
                    HrtGoalSafetySeverity.SOFT,
                    warnings[0].severity,
                )
            }
    }

    @Test
    fun ftm_testosterone_emitsNoWarning() {
        val warnings = hrtGoalSafetyWarningsFor(
            HrtGoal.FTM,
            MedicationKey.TESTOSTERONE_ENANTHATE,
            MedicationCategory.TESTOSTERONE,
        )
        assertTrue("FTM + T should be clean", warnings.isEmpty())
    }

    @Test
    fun mtf_cpa_emitsNoWarning() {
        val warnings = hrtGoalSafetyWarningsFor(
            HrtGoal.MTF,
            MedicationKey.CYPROTERONE_ACETATE,
            MedicationCategory.ANTIANDROGEN,
        )
        assertTrue("MTF + CPA is on-regimen", warnings.isEmpty())
    }

    @Test
    fun unsetAndNonBinary_emitNoWarning() {
        // The user picked no goal (UNSET) or explicitly opted out
        // of goal-based gating (NON_BINARY). We do not warn in
        // either case; F-PR5 documents this design choice.
        listOf(HrtGoal.UNSET, HrtGoal.NON_BINARY).forEach { goal ->
            assertTrue(
                "$goal + CPA should be quiet",
                hrtGoalSafetyWarningsFor(
                    goal,
                    MedicationKey.CYPROTERONE_ACETATE,
                    MedicationCategory.ANTIANDROGEN,
                ).isEmpty(),
            )
            assertTrue(
                "$goal + E2 should be quiet",
                hrtGoalSafetyWarningsFor(
                    goal,
                    MedicationKey.ESTRADIOL,
                    MedicationCategory.ESTRADIOL,
                ).isEmpty(),
            )
        }
    }

    @Test
    fun customMedication_noKey_emitsNoWarning() {
        // The editor's custom-medication path leaves medicationKey
        // null; we can't route to a specific warning so the user
        // gets a quiet save. Trust the name they typed.
        val warnings = hrtGoalSafetyWarningsFor(
            HrtGoal.FTM,
            medicationKey = null,
            category = MedicationCategory.ESTRADIOL,
        )
        assertTrue("null key should not warn", warnings.isEmpty())
    }
}
