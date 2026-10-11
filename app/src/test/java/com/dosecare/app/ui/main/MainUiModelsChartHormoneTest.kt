package com.dosecare.app.ui.main

import com.dosecare.app.model.pk.PkHormone
import com.dosecare.app.model.settings.HrtGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// F-PR3: per-goal chart hormone whitelist + default. These two
// helpers are the source of truth for both the MainViewModel auto-
// picker and the MainChartHormoneChipRow filter, so locking them
// down in a unit test guards the whole F-PR3 contract.
class MainUiModelsChartHormoneTest {
    @Test
    fun availableChartHormonesFor_FTM_isTestosteroneOnly() {
        val available = availableChartHormonesFor(HrtGoal.FTM)
        assertEquals(setOf(PkHormone.TESTOSTERONE), available)
        assertFalse("FTM must not show E2", PkHormone.ESTRADIOL in available)
        assertFalse("FTM must not show CPA", PkHormone.CYPROTERONE_ACETATE in available)
    }

    @Test
    fun availableChartHormonesFor_MTF_isE2PlusCpa() {
        val available = availableChartHormonesFor(HrtGoal.MTF)
        assertEquals(
            setOf(PkHormone.ESTRADIOL, PkHormone.CYPROTERONE_ACETATE),
            available,
        )
        assertFalse("MTF must not show T", PkHormone.TESTOSTERONE in available)
    }

    @Test
    fun availableChartHormonesFor_NonBinaryAndUnset_keepThreeWay() {
        val nonBinary = availableChartHormonesFor(HrtGoal.NON_BINARY)
        val unset = availableChartHormonesFor(HrtGoal.UNSET)
        val expected = setOf(
            PkHormone.ESTRADIOL,
            PkHormone.TESTOSTERONE,
            PkHormone.CYPROTERONE_ACETATE,
        )
        assertEquals(expected, nonBinary)
        assertEquals(expected, unset)
    }

    @Test
    fun defaultChartHormoneFor_FTM_isTestosterone() {
        assertEquals(PkHormone.TESTOSTERONE, defaultChartHormoneFor(HrtGoal.FTM))
    }

    @Test
    fun defaultChartHormoneFor_MTF_isEstradiol() {
        assertEquals(PkHormone.ESTRADIOL, defaultChartHormoneFor(HrtGoal.MTF))
    }

    @Test
    fun defaultChartHormoneFor_NonBinaryAndUnset_defaultToEstradiol() {
        // Non-binary / unset pick a neutral E2 default so the chart
        // still renders something on first launch. The user can
        // override with the chip row once the data is available.
        assertEquals(PkHormone.ESTRADIOL, defaultChartHormoneFor(HrtGoal.NON_BINARY))
        assertEquals(PkHormone.ESTRADIOL, defaultChartHormoneFor(HrtGoal.UNSET))
    }

    @Test
    fun defaultChartHormoneFor_isAlwaysInAvailableSet() {
        // The default must always be in the available set, so the
        // post-goal-change chart never falls into "no chips visible".
        HrtGoal.entries.forEach { goal ->
            val available = availableChartHormonesFor(goal)
            val default = defaultChartHormoneFor(goal)
            assertTrue(
                "default $default for goal $goal must be in available $available",
                default in available,
            )
        }
    }
}
