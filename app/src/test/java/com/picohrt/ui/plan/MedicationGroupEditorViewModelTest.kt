package com.picohrt.ui.plan

import com.picohrt.model.medication.MedicationApplicationType
import com.picohrt.model.medication.MedicinePreparationType
import com.picohrt.ui.medication.DoseInstructionDraftUiState
import com.picohrt.ui.medication.defaultMedicineDraft
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
