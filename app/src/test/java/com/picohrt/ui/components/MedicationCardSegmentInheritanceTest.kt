package com.picohrt.ui.components

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import com.picohrt.model.medication.DoseInstruction
import com.picohrt.model.medication.MedicationApplicationType
import com.picohrt.model.medication.MedicineStockProjection
import com.picohrt.model.medication.MedicineStockState
import com.picohrt.model.medication.RunwayProjection
import com.picohrt.model.medication.testMedicine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class MedicationCardSegmentInheritanceTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun medicationCards_inSection_inheritPositions() {
        composeRule.setContent {
            HrtSection(title = null) {
                item {
                    MedicationCard(
                        medicine = testMedicine(),
                        doseInstruction = DoseInstruction.Noop,
                        applicationType = MedicationApplicationType.ORAL,
                        medicationCount = 1,
                        groupColorKey = null,
                        onClick = {},
                    )
                }
                item {
                    MedicationCard(
                        medicine = testMedicine(),
                        doseInstruction = DoseInstruction.Noop,
                        applicationType = MedicationApplicationType.ORAL,
                        medicationCount = 1,
                        groupColorKey = null,
                        onClick = {},
                    )
                }
            }
        }
        composeRule.onNode(
            SemanticsMatcher.expectValue(SegmentPositionSemanticsKey, SegmentPosition(0, 2))
        ).assertExists()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SegmentPositionSemanticsKey, SegmentPosition(1, 2))
        ).assertExists()
    }

    @Test
    fun stockSubcards_inSection_inheritPositions() {
        // MedicinesScreen rows are MedicationCardWithStockSubcard, which computes
        // its own cardSegment from index/itemCount before delegating to MedicationCard.
        // This is the real regression guard: it must not force 0/1.
        val projection = stockProjection()
        composeRule.setContent {
            HrtSection(title = null) {
                item {
                    MedicationCardWithStockSubcard(
                        medicine = projection.medicine,
                        doseInstruction = DoseInstruction.Noop,
                        applicationType = MedicationApplicationType.ORAL,
                        medicationCount = 1,
                        groupColorKey = null,
                        stockProjection = projection,
                        onClick = {},
                    )
                }
                item {
                    MedicationCardWithStockSubcard(
                        medicine = projection.medicine,
                        doseInstruction = DoseInstruction.Noop,
                        applicationType = MedicationApplicationType.ORAL,
                        medicationCount = 1,
                        groupColorKey = null,
                        stockProjection = projection,
                        onClick = {},
                    )
                }
                item {
                    MedicationCardWithStockSubcard(
                        medicine = projection.medicine,
                        doseInstruction = DoseInstruction.Noop,
                        applicationType = MedicationApplicationType.ORAL,
                        medicationCount = 1,
                        groupColorKey = null,
                        stockProjection = projection,
                        onClick = {},
                    )
                }
            }
        }
        listOf(
            SegmentPosition(0, 3),
            SegmentPosition(1, 3),
            SegmentPosition(2, 3),
        ).forEach {
            composeRule.onNode(SemanticsMatcher.expectValue(SegmentPositionSemanticsKey, it))
                .assertExists()
        }
    }

    private fun stockProjection(): MedicineStockProjection {
        val medicine = testMedicine()
        return MedicineStockProjection(
            medicine = medicine,
            dosesPerDayMagnitude = 1.0,
            totalStockUnits = 12.0,
            runway = RunwayProjection.Days(
                days = 11,
                lastFulfillable = LocalDate.of(2026, 1, 12),
            ),
            intervalDays = null,
            maxPerAdministration = 1.0,
            state = MedicineStockState.HEALTHY,
        )
    }
}
