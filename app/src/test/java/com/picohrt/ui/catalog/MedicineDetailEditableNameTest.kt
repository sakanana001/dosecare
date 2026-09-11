package com.picohrt.ui.catalog

import com.picohrt.model.medication.MedicationKey
import com.picohrt.model.medication.MedicineSelection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicineDetailEditableNameTest {

    // Custom medicines edit their name through the display-name override, which
    // is a presentation field — so editing must stay available even once logs
    // lock the medicine. The predicate is intentionally lock-independent; this
    // test guards against regressing back to a catalog-only / unlocked-only gate.
    @Test
    fun customMedicineExposesEditableName() {
        assertTrue(
            medicineDetailExposesEditableName(
                MedicineSelection.Custom(medicationName = "My estradiol"),
            ),
        )
    }

    @Test
    fun catalogMedicineExposesEditableName() {
        assertTrue(
            medicineDetailExposesEditableName(
                MedicineSelection.Catalog(MedicationKey.ESTRADIOL),
            ),
        )
    }

    @Test
    fun patchOffSingletonHasNoEditableName() {
        assertFalse(medicineDetailExposesEditableName(MedicineSelection.PatchOff))
    }
}
