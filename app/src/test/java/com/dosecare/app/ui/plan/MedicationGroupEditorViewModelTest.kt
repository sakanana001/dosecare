package com.dosecare.app.ui.plan

import com.dosecare.app.model.medication.MedicationApplicationType
import com.dosecare.app.model.medication.MedicinePreparationType
import com.dosecare.app.ui.medication.DoseInstructionDraftUiState
import com.dosecare.app.ui.medication.defaultMedicineDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationGroupEditorViewModelTest {
    @Test
    fun resolvedApplicationType_usesDoseDraftPreparationForNullMedicinePatchOff() {
        val medication = MedicationGroupMedicationEditorUiState(
            resolvedMedicine = null,
            medicineDraft = defaultMedicineDraft(
                applicationType = MedicationApplicationType.PATCH_OFF,
            ),
            doseInstructionDraft = DoseInstructionDraftUiState(
                applicationType = MedicationApplicationType.PATCH_OFF,
                preparationType = MedicinePreparationType.PATCH_OFF,
            ),
        )

        assertEquals(
            MedicationApplicationType.PATCH_OFF,
            medication.resolvedApplicationType(),
        )
    }
}
