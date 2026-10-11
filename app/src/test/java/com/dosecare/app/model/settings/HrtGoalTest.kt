package com.dosecare.app.model.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class HrtGoalTest {
    // F-PR1: fromStorageValue round-trips the four enum values via their
    // .name strings (which is what DataStore persists) and falls back
    // to UNSET for any null/unknown value. The unknown-case is the
    // forward-compat fallback for users upgrading from a build that
    // shipped a future HrtGoal case we don't recognise yet — they
    // stay on the pre-F-PR1 default (E2 trend, no FtM safety gating)
    // rather than crashing the settings flow.

    @Test
    fun fromStorageValue_roundTripsEachGoal() {
        HrtGoal.entries.forEach { goal ->
            assertEquals(goal, HrtGoal.fromStorageValue(goal.name))
        }
    }

    @Test
    fun fromStorageValue_unknownOrNullDefaultsToUnset() {
        assertEquals(HrtGoal.UNSET, HrtGoal.fromStorageValue(null))
        assertEquals(HrtGoal.UNSET, HrtGoal.fromStorageValue(""))
        assertEquals(HrtGoal.UNSET, HrtGoal.fromStorageValue("unknown_value"))
    }
}
