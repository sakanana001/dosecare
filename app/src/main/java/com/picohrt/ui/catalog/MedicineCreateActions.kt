package com.picohrt.ui.catalog

import androidx.annotation.StringRes
import com.picohrt.R
import com.picohrt.data.repository.MedicineIdentityCollisionException
import com.picohrt.data.repository.MedicineRepository
import com.picohrt.model.medication.MedicationApplicationType
import com.picohrt.model.medication.MedicationSelectionKind
import com.picohrt.model.medication.Medicine
import com.picohrt.ui.medication.MedicinePickerUiState
import com.picohrt.ui.medication.toNewMedicineRequest
import com.picohrt.ui.medication.validationErrorRes
import kotlinx.coroutines.CancellationException
import java.time.Instant

internal sealed interface MedicineCreateResult {
    data class Success(
        val medicine: Medicine,
        val createdNew: Boolean = true,
    ) : MedicineCreateResult

    data class ValidationError(@param:StringRes val messageRes: Int) : MedicineCreateResult
    data class SaveFailure(val saveResult: CreateMedicineSaveResult) : MedicineCreateResult
}

internal fun validateMedicineDraftForCreate(draft: MedicinePickerUiState): Int? {
    if (draft.catalogFilterApplicationType == MedicationApplicationType.PATCH_OFF) {
        return R.string.validation_preparation_type_required
    }
    return draft.validationErrorRes()
}

internal suspend fun createMedicineFromDraft(
    medicineRepository: MedicineRepository,
    draft: MedicinePickerUiState,
): MedicineCreateResult {
    validateMedicineDraftForCreate(draft)?.let { error ->
        return MedicineCreateResult.ValidationError(error)
    }

    return runCatching {
        val request = draft.toNewMedicineRequest()
        val requestedAt = Instant.now()
        val medicine = when (request.selectionKind) {
            MedicationSelectionKind.CATALOG -> medicineRepository.findOrCreateForCatalog(
                medicationKey = checkNotNull(request.medicationKey),
                preparation = request.preparation,
                now = requestedAt,
            )

            MedicationSelectionKind.CUSTOM -> medicineRepository.findOrCreateForCustom(
                customMedicationName = checkNotNull(request.customMedicationName),
                displayName = request.displayName,
                category = request.category,
                preparation = request.preparation,
                displayDoseUnit = request.displayDoseUnit,
                now = requestedAt,
            )
        }
        MedicineCreateResult.Success(
            medicine = medicine,
            createdNew = medicine.createdAt == requestedAt,
        )
    }.fold(
        onSuccess = { result -> result },
        onFailure = { error ->
            if (error is CancellationException) {
                throw error
            }
            MedicineCreateResult.SaveFailure(
                when (error) {
                    is MedicineIdentityCollisionException ->
                        CreateMedicineSaveResult.FAILURE_IDENTITY_COLLISION

                    else -> CreateMedicineSaveResult.FAILURE_OTHER
                },
            )
        },
    )
}
