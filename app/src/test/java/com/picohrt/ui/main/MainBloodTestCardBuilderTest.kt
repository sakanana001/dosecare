package com.picohrt.ui.main

import com.picohrt.model.bloodtest.BloodAnalyteKey
import com.picohrt.model.bloodtest.BloodAnalyteReferenceSeverity
import com.picohrt.model.bloodtest.BloodTestPanel
import com.picohrt.model.bloodtest.BloodTestResult
import com.picohrt.model.bloodtest.BloodTestResultAnalyte
import com.picohrt.model.bloodtest.BloodUnitKey
import com.picohrt.model.settings.HrtGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MainBloodTestCardBuilderTest {
    private val now: Instant = Instant.parse("2026-09-04T10:00:00Z")

    @Test
    fun returns_null_when_no_panels() {
        val card = buildMainBloodTestCard(
            panels = emptyList(),
            goal = HrtGoal.MTF,
            now = now,
        )

        assertNull(card)
    }

    @Test
    fun populates_t_trend_and_hb_chip_from_panels() {
        val panels = listOf(
            panel(
                collectedAt = now.minus(30L, ChronoUnit.DAYS),
                results = listOf(
                    builtinResult(BloodAnalyteKey.T, value = 500.0, unit = BloodUnitKey.NG_DL),
                    builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 14.5, unit = BloodUnitKey.G_DL),
                ),
            ),
            panel(
                collectedAt = now.minus(60L, ChronoUnit.DAYS),
                results = listOf(
                    builtinResult(BloodAnalyteKey.T, value = 480.0, unit = BloodUnitKey.NG_DL),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.FTM, now = now)!!

        assertNotNull(card.testosteroneTrend)
        assertEquals(2, card.testosteroneTrend!!.sampleCount)
        assertNotNull(card.hemoglobinChip)
        assertEquals(1, card.hemoglobinChip!!.sampleCount)
        assertEquals(2, card.totalPanelCount)
    }

    @Test
    fun omits_t_trend_when_user_has_no_t_data() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(
                    builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 13.0, unit = BloodUnitKey.G_DL),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        assertNull(card.testosteroneTrend)
        assertNotNull(card.hemoglobinChip)
    }

    @Test
    fun omits_hb_chip_when_user_has_no_hb_data() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(
                    builtinResult(BloodAnalyteKey.T, value = 50.0, unit = BloodUnitKey.NG_DL),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        assertNotNull(card.testosteroneTrend)
        assertNull(card.hemoglobinChip)
    }

    @Test
    fun filters_t_trend_to_6m_window() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(builtinResult(BloodAnalyteKey.T, value = 100.0, unit = BloodUnitKey.NG_DL)),
            ),
            panel(
                // 7 months ago — outside the default 180-day window
                collectedAt = now.minus(210L, ChronoUnit.DAYS),
                results = listOf(builtinResult(BloodAnalyteKey.T, value = 200.0, unit = BloodUnitKey.NG_DL)),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        assertNotNull(card.testosteroneTrend)
        assertEquals("Only the in-window T point should remain", 1, card.testosteroneTrend!!.sampleCount)
        assertEquals(100.0, card.testosteroneTrend!!.latestValueCanonical, 1e-9)
    }

    @Test
    fun mt_goal_classifies_low_hb_as_LOW() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 11.0, unit = BloodUnitKey.G_DL)),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        assertEquals(BloodAnalyteReferenceSeverity.LOW, card.hemoglobinChip!!.severity)
    }

    @Test
    fun ftm_goal_classifies_high_hb_as_HIGH() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 17.8, unit = BloodUnitKey.G_DL)),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.FTM, now = now)!!

        assertEquals(BloodAnalyteReferenceSeverity.HIGH, card.hemoglobinChip!!.severity)
    }

    @Test
    fun ftm_goal_classifies_critical_hb_as_CRITICAL() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(builtinResult(BloodAnalyteKey.HEMOGLOBIN, value = 19.0, unit = BloodUnitKey.G_DL)),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.FTM, now = now)!!

        assertEquals(BloodAnalyteReferenceSeverity.CRITICAL, card.hemoglobinChip!!.severity)
    }

    @Test
    fun t_trend_pads_y_axis() {
        val panels = (0..4).map { i ->
            panel(
                collectedAt = now.minus((i * 10L), ChronoUnit.DAYS),
                results = listOf(builtinResult(BloodAnalyteKey.T, value = 500.0 + i, unit = BloodUnitKey.NG_DL)),
            )
        }

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        val trend = card.testosteroneTrend!!
        // Y axis should be padded by 15% of the data range, so
        // minY is below the smallest data point and maxY is above
        // the largest. The pad is 15% of 4 = 0.6.
        assertTrue("minY < data min: ${trend.minY}", trend.minY < 500.0)
        assertTrue("maxY > data max: ${trend.maxY}", trend.maxY > 504.0)
    }

    @Test
    fun panel_with_only_custom_analytes_yields_empty_trends_but_non_null_card() {
        val panels = listOf(
            panel(
                collectedAt = now,
                results = listOf(
                    customResult(name = "DHT", unit = "ng/dL", value = 30.0),
                ),
            ),
        )

        val card = buildMainBloodTestCard(panels = panels, goal = HrtGoal.MTF, now = now)!!

        // Card is non-null (panels exist), but no T trend + no Hb chip
        // because neither analyte is built-in.
        assertNull(card.testosteroneTrend)
        assertNull(card.hemoglobinChip)
    }

    // ---- helpers ----

    private fun panel(
        collectedAt: Instant,
        results: List<BloodTestResult>,
    ): BloodTestPanel = BloodTestPanel(
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
    ): BloodTestResult = BloodTestResult(
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
    ): BloodTestResult = BloodTestResult(
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
