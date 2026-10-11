package com.dosecare.app.ui.main

import com.dosecare.app.model.bloodtest.BloodAnalyteKey
import com.dosecare.app.model.bloodtest.BloodAnalyteReferenceSeverity
import com.dosecare.app.model.bloodtest.BloodTestResult
import com.dosecare.app.model.bloodtest.BloodTestResultAnalyte
import com.dosecare.app.model.bloodtest.BloodUnitKey
import com.dosecare.app.model.bloodtest.bloodAnalyteReferenceProfileFor
import com.dosecare.app.model.bloodtest.classifyBloodAnalyteValue
import com.dosecare.app.model.settings.HrtGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * v2 血检追踪 (BT-PR4) end-to-end integration smoke test.
 *
 * Walks the full pipeline the home card consumes:
 * panels + per-goal profile + severity classifier +
 * trend builder. The tests are intentionally end-to-end
 * (no mocks) so a future change to any of those four
 * pieces surfaces here before it ships.
 *
 * Unit-level coverage lives in:
 * - BloodAnalyteReferenceTest (ranges + severity)
 * - BloodTestCatalogTest (unit conversion)
 * - MainBloodTestCardBuilderTest (state builder)
 */
class BloodTestCardIntegrationTest {
    private val now: Instant = Instant.parse("2026-09-04T10:00:00Z")

    @Test
    fun ftm_user_sees_t_trend_and_critical_hb_chip() {
        val panels = listOf(
            panel(
                collectedAt = now.minus(30L, ChronoUnit.DAYS),
                results = listOf(
                    builtinResult(BloodAnalyteKey.T, value = 600.0, unit = BloodUnitKey.NG_DL),
                    builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 19.2, unit = BloodUnitKey.G_DL),
                ),
            ),
            panel(
                collectedAt = now.minus(60L, ChronoUnit.DAYS),
                results = listOf(
                    builtinResult(BloodAnalyteKey.T, value = 550.0, unit = BloodUnitKey.NG_DL),
                    builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 18.4, unit = BloodUnitKey.G_DL),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.FTM, now = now)!!

        // T trend
        val trend = card.testosteroneTrend!!
        assertEquals(2, trend.sampleCount)
        assertEquals(600.0, trend.latestValueCanonical, 1e-9)

        // Hb chip
        val chip = card.hemoglobinChip!!
        assertEquals(BloodAnalyteReferenceSeverity.CRITICAL, chip.severity)
        assertEquals(19.2, chip.valueCanonical, 1e-9)

        // Per-goal profile lookup stays consistent with the
        // severity classification (the FTM Hb profile is the
        // same one the classifier consults).
        val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.HEMOGLOBIN, HrtGoal.FTM)!!
        assertTrue("19.2 should be above the FTM critical threshold (${profile.criticalThreshold})",
            profile.criticalThreshold?.let { 19.2 > it } == true)
    }

    @Test
    fun mtf_user_sees_normal_hb_chip_and_trend_chip_for_suppressed_t() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(
                    builtinResult(BloodAnalyteKey.T, value = 30.0, unit = BloodUnitKey.NG_DL),
                    builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 14.0, unit = BloodUnitKey.G_DL),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        val trend = card.testosteroneTrend!!
        assertEquals(30.0, trend.latestValueCanonical, 1e-9)

        val chip = card.hemoglobinChip!!
        assertEquals(BloodAnalyteReferenceSeverity.NORMAL, chip.severity)
    }

    @Test
    fun non_binary_user_uses_unisex_hb_band() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(
                    builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 17.6, unit = BloodUnitKey.G_DL),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.NON_BINARY, now = now)!!

        assertEquals(BloodAnalyteReferenceSeverity.HIGH, card.hemoglobinChip!!.severity)
    }

    @Test
    fun classifier_and_profile_share_consistent_thresholds() {
        // Sanity: the per-goal profile's critical threshold
        // matches what the classifier considers critical.
        // If a future change desyncs them, this test breaks.
        listOf(
            HrtGoal.MTF to 18.0,
            HrtGoal.FTM to 18.0,
            HrtGoal.NON_BINARY to 18.0,
            HrtGoal.UNSET to 18.0,
        ).forEach { (goal, expectedCritical) ->
            val profile = bloodAnalyteReferenceProfileFor(BloodAnalyteKey.HEMOGLOBIN, goal)!!
            assertEquals(
                "Critical threshold for $goal Hb profile",
                expectedCritical,
                profile.criticalThreshold!!,
                1e-9,
            )
            // Classifier says CRITICAL iff value > threshold
            val atThreshold = classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, goal, expectedCritical)
            val justAbove = classifyBloodAnalyteValue(BloodAnalyteKey.HEMOGLOBIN, goal, expectedCritical + 0.1)
            assertEquals("Threshold-at value should NOT be critical (strict >)", BloodAnalyteReferenceSeverity.HIGH, atThreshold)
            assertEquals("Just-above threshold should be CRITICAL", BloodAnalyteReferenceSeverity.CRITICAL, justAbove)
        }
    }

    @Test
    fun unset_user_with_only_custom_analytes_yields_null_trend_and_null_chip_but_non_null_card() {
        // A user with a custom DHT-only panel: the card
        // exists (panels > 0) but the per-goal T / Hb
        // trend + chip are null because no built-in
        // analyte matches.
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(
                    customResult("DHT", "ng/dL", 35.0),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.UNSET, now = now)

        assertNotNull(card)
        assertNull(card!!.testosteroneTrend)
        assertNull(card.hemoglobinChip)
    }

    // ---- helpers ----

    private fun panel(
        collectedAt: Instant,
        results: List<BloodTestResult>,
    ) = com.dosecare.app.model.bloodtest.BloodTestPanel(
        uuid = UUID.randomUUID(),
        collectedAt = collectedAt,
        collectedAtTimeZoneId = "UTC",
        notes = null,
        timeSinceLastEstradiolDoseMillis = null,
        timeSinceLastTestosteroneDoseMillis = null,
        results = results,
        createdAt = collectedAt,
        updatedAt = collectedAt,
    )

    private fun builtinResult(
        analyteKey: BloodAnalyteKey,
        value: Double,
        unit: BloodUnitKey,
    ) = BloodTestResult(
        uuid = UUID.randomUUID(),
        createdAt = Instant.EPOCH,
        displayOrder = 0,
        analyte = BloodTestResultAnalyte.Builtin(analyteKey),
        value = value,
        unitSnapshot = unit.storageValue,
        canonicalValue = value,
    )

    private fun customResult(
        name: String,
        unit: String,
        value: Double,
    ) = BloodTestResult(
        uuid = UUID.randomUUID(),
        createdAt = Instant.EPOCH,
        displayOrder = 0,
        analyte = BloodTestResultAnalyte.Custom(
            uuid = UUID.randomUUID(),
            abbreviation = name.take(3).uppercase(),
            name = name,
        ),
        value = value,
        unitSnapshot = unit,
        canonicalValue = value,
    )
}
