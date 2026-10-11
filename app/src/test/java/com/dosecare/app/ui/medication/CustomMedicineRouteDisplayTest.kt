package com.dosecare.app.ui.medication

import androidx.compose.ui.test.junit4.createComposeRule
import com.dosecare.app.model.medication.DoseInstruction
import com.dosecare.app.model.medication.MedicationApplicationType
import com.dosecare.app.model.medication.MedicinePreparation
import com.dosecare.app.model.medication.testCustomMedicine
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CustomMedicineRouteDisplayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun customMedicine_showsCustomLabelInsteadOfInferredRoute() {
        var text: String? = null

        composeRule.setContent {
            text = medicationEntrySupportingText(
                medicine = testCustomMedicine(
                    medicationName = "Progesterone",
                    preparation = MedicinePreparation.Capsule(strengthMgPerCapsule = 100.0),
                ),
                doseInstruction = DoseInstruction.WholeUnit,
                applicationType = MedicationApplicationType.ORAL,
                count = 1,
            )
        }
        composeRule.waitForIdle()

        assertEquals("Custom · 100 mg", text)
    }
}
