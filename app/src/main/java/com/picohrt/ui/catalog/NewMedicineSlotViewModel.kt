package com.picohrt.ui.catalog

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.picohrt.data.repository.MedicationLogRepository
import com.picohrt.data.repository.MedicineRepository
import com.picohrt.data.repository.MedicineStockRepository
import com.picohrt.model.medication.DoseInstruction
import com.picohrt.model.medication.MedicationApplicationType
import com.picohrt.model.medication.Medicine
import com.picohrt.model.medication.MedicinePreparationType
import com.picohrt.reminder.MedicationReminderScheduler
import com.picohrt.reminder.PostLogStockWarning
import com.picohrt.ui.medication.DoseInstructionDraftUiState
import com.picohrt.ui.medication.MedicationDoseDraft
import com.picohrt.ui.medication.MedicinePickerUiState
import com.picohrt.ui.medication.applyMedicinePicker
import com.picohrt.ui.medication.defaultMedicineDraft
import com.picohrt.ui.medication.doseAmountDeltaForActual
import com.picohrt.ui.medication.effectiveActualDoseAmount
import com.picohrt.ui.medication.inferredOrSelectedPreparationType
import com.picohrt.ui.medication.parsePositiveDouble
import com.picohrt.ui.medication.resolvedApplicationTypeForDose
import com.picohrt.ui.medication.resolvedMedicationCountForSave
import com.picohrt.ui.medication.toDoseInstruction
import com.picohrt.ui.medication.toDoseInstructionDraft
import com.picohrt.ui.medication.validatedWith
import com.picohrt.ui.medication.withCountText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class NewMedicineSlotViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val medicineStockRepository: MedicineStockRepository,
    private val medicationReminderScheduler: MedicationReminderScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        NewMedicineSlotUiState(appliedZoneId = ZoneId.systemDefault())
    )
    val uiState: StateFlow<NewMedicineSlotUiState> = _uiState.asStateFlow()
    private var saveGeneration = 0

    // Medicine creation and the manual-log write are not one transaction: a newly
    // created medicine persists even if its log save fails, so a retry sees it as
    // pre-existing (createdNew == false). Remember it here so the stock-tracking
    // nudge still fires once the log finally succeeds on a later attempt.
    private var pendingManualLogCreatedMedicineUuid: java.util.UUID? = null

    internal constructor(
        medicineRepository: MedicineRepository,
        medicationLogRepository: MedicationLogRepository,
        medicineStockRepository: MedicineStockRepository,
        medicationReminderScheduler: MedicationReminderScheduler,
        initialAppliedZoneId: ZoneId,
    ) : this(
        medicineRepository = medicineRepository,
        medicationLogRepository = medicationLogRepository,
        medicineStockRepository = medicineStockRepository,
        medicationReminderScheduler = medicationReminderScheduler,
    ) {
        _uiState.value = NewMedicineSlotUiState(appliedZoneId = initialAppliedZoneId)
    }

    fun reset() {
        saveGeneration += 1
        pendingManualLogCreatedMedicineUuid = null
        _uiState.update {
            NewMedicineSlotUiState(appliedZoneId = it.appliedZoneId)
        }
    }

    fun updateMedicineDraft(transform: (MedicinePickerUiState) -> MedicinePickerUiState) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                state
            } else {
                val reduced = MedicationDoseDraft(
                    medicineDraft = state.medicineDraft,
                    doseInstructionDraft = state.doseInstructionDraft,
                    countText = state.countText,
                ).applyMedicinePicker(transform)
                state.copy(
                    medicineDraft = reduced.medicineDraft,
                    doseInstructionDraft = reduced.doseInstructionDraft,
                    countText = reduced.countText,
                    doseAmountDelta = null,
                    errorMessageRes = reduced.errorMessageRes,
                    createSaveResult = null,
                    manualLogSaveResult = null,
                    slotResult = null,
                )
            }
        }
    }

    fun updateDoseInstructionDraft(
        transform: (DoseInstructionDraftUiState) -> DoseInstructionDraftUiState,
    ) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                state
            } else {
                state.copy(
                    doseInstructionDraft = transform(state.doseInstructionDraft),
                    doseAmountDelta = null,
                    errorMessageRes = null,
                    createSaveResult = null,
                    manualLogSaveResult = null,
                    slotResult = null,
                )
            }
        }
    }

    fun updateCountText(countText: String) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                state
            } else {
                val reduced = MedicationDoseDraft(
                    medicineDraft = state.medicineDraft,
                    doseInstructionDraft = state.doseInstructionDraft,
                    countText = state.countText,
                ).withCountText(countText)
                state.copy(
                    countText = reduced.countText,
                    errorMessageRes = reduced.errorMessageRes,
                    createSaveResult = null,
                    manualLogSaveResult = null,
                    slotResult = null,
                )
            }
        }
    }

    fun updateAppliedDate(appliedDate: LocalDate) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                state
            } else {
                state.copy(
                    appliedDate = appliedDate,
                    manualLogSaveResult = null,
                )
            }
        }
    }

    fun updateAppliedTime(appliedTime: LocalTime) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                state
            } else {
                state.copy(
                    appliedTime = appliedTime.withSecond(0).withNano(0),
                    manualLogSaveResult = null,
                )
            }
        }
    }

    fun updateNote(note: String) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                state
            } else {
                state.copy(
                    note = note,
                    errorMessageRes = null,
                )
            }
        }
    }

    fun setDoseAmountDelta(delta: Double?) {
        _uiState.update { state ->
            if (state.isLockedForUpdates()) {
                return@update state
            }
            val scheduledAmount = state.scheduledNativeAmount ?: return@update state
            if (!state.allowsActualDoseDelta) {
                return@update state
            }
            state.copy(
                doseAmountDelta = doseAmountDeltaForActual(
                    scheduledAmount = scheduledAmount,
                    actualAmount = scheduledAmount + (delta ?: 0.0),
                ),
                manualLogSaveResult = null,
            )
        }
    }

    fun saveGroupSlot(): Job? {
        val currentState = _uiState.value
        if (currentState.isSaving || currentState.isSaved) {
            return null
        }
        validateSlot(currentState)?.let { errorMessageRes ->
            _uiState.update {
                it.copy(
                    errorMessageRes = errorMessageRes,
                    createSaveResult = null,
                    manualLogSaveResult = null,
                    createdMedicineUuid = null,
                    slotResult = null,
                )
            }
            return null
        }
        val operationGeneration = nextSaveGeneration()

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessageRes = null,
                createSaveResult = null,
                manualLogSaveResult = null,
                createdMedicineUuid = null,
                slotResult = null,
            )
        }
        return viewModelScope.launch {
            try {
                saveMedicineThen(currentState, operationGeneration) { medicine, createdNew ->
                    val applicationType = resolvedApplicationType(currentState)
                    updateIfCurrent(operationGeneration) {
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            createdMedicineUuid = medicine.uuid.takeIf { createdNew },
                            slotResult = MedicineSlotResult(
                                medicineUuid = medicine.uuid,
                                applicationType = applicationType,
                                doseInstruction = resolvedDoseInstruction(currentState),
                                count = resolvedMedicationCountForSave(
                                    applicationType = applicationType,
                                    countText = currentState.countText,
                                    preparationType = resolvedPreparationType(currentState),
                                ),
                            ),
                        )
                    }
                }
            } catch (exception: CancellationException) {
                clearSavingIfCurrent(operationGeneration)
                throw exception
            }
        }
    }

    fun saveManualLog(): Job? {
        val currentState = _uiState.value
        if (currentState.isSaving || currentState.isSaved) {
            return null
        }
        validateSlot(currentState)?.let { errorMessageRes ->
            _uiState.update {
                it.copy(
                    errorMessageRes = errorMessageRes,
                    createSaveResult = null,
                    manualLogSaveResult = null,
                    createdMedicineUuid = null,
                    slotResult = null,
                )
            }
            return null
        }
        val operationGeneration = nextSaveGeneration()

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessageRes = null,
                createSaveResult = null,
                manualLogSaveResult = null,
                createdMedicineUuid = null,
                slotResult = null,
            )
        }
        return viewModelScope.launch {
            try {
                saveMedicineThen(currentState, operationGeneration) { medicine, createdNew ->
                    if (createdNew) {
                        pendingManualLogCreatedMedicineUuid = medicine.uuid
                    }
                    // A prior attempt may have created this medicine before its log
                    // save failed, so treat the matching pending UUID as newly created too.
                    val isNewlyCreatedMedicine =
                        createdNew || pendingManualLogCreatedMedicineUuid == medicine.uuid
                    val applicationType = resolvedApplicationType(currentState)
                    val saveResult = saveManualMedicineLog(
                        medicationLogRepository = medicationLogRepository,
                        medicineStockRepository = medicineStockRepository,
                        medicationReminderScheduler = medicationReminderScheduler,
                        medicineUuid = medicine.uuid,
                        resolvedApplicationType = applicationType,
                        doseInstruction = resolvedDoseInstruction(currentState),
                        count = resolvedMedicationCountForSave(
                            applicationType = applicationType,
                            countText = currentState.countText,
                            preparationType = resolvedPreparationType(currentState),
                        ),
                        doseAmountDelta = currentState.doseAmountDelta
                            ?.takeIf { currentState.allowsActualDoseDelta },
                        appliedDate = currentState.appliedDate,
                        appliedTime = currentState.appliedTime,
                        appliedZoneId = currentState.appliedZoneId,
                        note = currentState.note.takeIf { it.isNotBlank() },
                    )
                    if (saveResult.isSuccess) {
                        pendingManualLogCreatedMedicineUuid = null
                    }
                    updateIfCurrent(operationGeneration) {
                        it.copy(
                            isSaving = false,
                            isSaved = saveResult.isSuccess,
                            manualLogSaveResult = saveResult.saveResult,
                            postLogStockWarning = saveResult.postLogStockWarning,
                            createdMedicineUuid = medicine.uuid.takeIf {
                                saveResult.isSuccess && isNewlyCreatedMedicine
                            },
                        )
                    }
                }
            } catch (exception: CancellationException) {
                clearSavingIfCurrent(operationGeneration)
                throw exception
            }
        }
    }

    fun consumeSavedState() {
        _uiState.update {
            it.copy(
                isSaved = false,
                postLogStockWarning = null,
                createdMedicineUuid = null,
                slotResult = null,
            )
        }
    }

    fun consumeManualLogSaveResult() {
        _uiState.update { it.copy(manualLogSaveResult = null) }
    }

    // Clears the create-medicine save result after a failure has been surfaced as
    // a toast so it doesn't re-trigger on recomposition or configuration change.
    fun consumeCreateSaveResult() {
        _uiState.update { it.copy(createSaveResult = null) }
    }

    private suspend fun saveMedicineThen(
        state: NewMedicineSlotUiState,
        operationGeneration: Int,
        onCreated: suspend (Medicine, Boolean) -> Unit,
    ) {
        // validateSlot already ran before this point; validate again inside the shared create helper as a defensive guard.
        when (
            val result = createMedicineFromDraft(
                medicineRepository = medicineRepository,
                draft = state.medicineDraft,
            )
        ) {
            is MedicineCreateResult.Success -> {
                if (isCurrentSave(operationGeneration)) {
                    onCreated(result.medicine, result.createdNew)
                }
            }

            is MedicineCreateResult.ValidationError -> {
                updateIfCurrent(operationGeneration) {
                    it.copy(
                        isSaving = false,
                        errorMessageRes = result.messageRes,
                        createSaveResult = null,
                        createdMedicineUuid = null,
                    )
                }
            }

            is MedicineCreateResult.SaveFailure -> {
                updateIfCurrent(operationGeneration) {
                    it.copy(
                        isSaving = false,
                        createSaveResult = result.saveResult,
                        createdMedicineUuid = null,
                    )
                }
            }
        }
    }

    @StringRes
    private fun validateSlot(state: NewMedicineSlotUiState): Int? {
        return MedicationDoseDraft(
            medicineDraft = state.medicineDraft,
            doseInstructionDraft = state.doseInstructionDraft,
            countText = state.countText,
        ).validatedWith(
            preparationType = resolvedPreparationType(state),
            validateMedicineDraft = ::validateMedicineDraftForCreate,
        ).errorMessageRes
    }

    private fun resolvedApplicationType(state: NewMedicineSlotUiState): MedicationApplicationType {
        return resolvedApplicationTypeForDose(
            preparationType = resolvedPreparationType(state),
            doseInstructionDraft = state.doseInstructionDraft,
        )
    }

    private fun resolvedPreparationType(state: NewMedicineSlotUiState) =
        state.medicineDraft.inferredOrSelectedPreparationType()
            ?: state.doseInstructionDraft.preparationType

    private fun resolvedDoseInstruction(state: NewMedicineSlotUiState): DoseInstruction {
        val resolvedApplicationType = resolvedApplicationType(state)
        return if (resolvedApplicationType == MedicationApplicationType.PATCH_OFF) {
            DoseInstruction.Noop
        } else {
            state.doseInstructionDraft.toDoseInstruction()
        }
    }

    private fun nextSaveGeneration(): Int {
        saveGeneration += 1
        return saveGeneration
    }

    private fun isCurrentSave(operationGeneration: Int): Boolean {
        return operationGeneration == saveGeneration
    }

    private fun updateIfCurrent(
        operationGeneration: Int,
        transform: (NewMedicineSlotUiState) -> NewMedicineSlotUiState,
    ) {
        if (isCurrentSave(operationGeneration)) {
            _uiState.update(transform)
        }
    }

    private fun clearSavingIfCurrent(operationGeneration: Int) {
        updateIfCurrent(operationGeneration) {
            it.copy(isSaving = false)
        }
    }

    private fun NewMedicineSlotUiState.isLockedForUpdates(): Boolean {
        return isSaving || isSaved
    }
}

data class NewMedicineSlotUiState(
    val medicineDraft: MedicinePickerUiState = defaultMedicineDraft(),
    val doseInstructionDraft: DoseInstructionDraftUiState =
        defaultMedicineDraft().toDoseInstructionDraft(),
    val countText: String = "1",
    val appliedDate: LocalDate = LocalDate.now(),
    val appliedTime: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val appliedZoneId: ZoneId,
    val doseAmountDelta: Double? = null,
    val note: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @param:StringRes val errorMessageRes: Int? = null,
    val createSaveResult: CreateMedicineSaveResult? = null,
    val manualLogSaveResult: MedicineSlotDraftSaveResult? = null,
    val postLogStockWarning: PostLogStockWarning? = null,
    val createdMedicineUuid: java.util.UUID? = null,
    val slotResult: MedicineSlotResult? = null,
) {
    val allowsActualDoseDelta: Boolean
        get() {
            val preparationType = resolvedPreparationType
            // Manual logs capture the dose directly for measured forms (mL, g),
            // so only ampules — which have no amount field — need the mg delta.
            if (preparationType != MedicinePreparationType.INJECTION_SINGLE_USE_VIAL) {
                return false
            }
            val applicationType = resolvedApplicationTypeForDose(
                preparationType = preparationType,
                doseInstructionDraft = doseInstructionDraft,
            )
            return applicationType == preparationType.requiredActualDoseApplicationType &&
                    scheduledNativeAmount != null
        }

    val effectiveActualAmount: Double?
        get() {
            val scheduledAmount = scheduledNativeAmount ?: return null
            return effectiveActualDoseAmount(
                scheduledAmount = scheduledAmount,
                doseAmountDelta = doseAmountDelta,
            )
        }

    private val resolvedPreparationType: MedicinePreparationType
        get() = medicineDraft.inferredOrSelectedPreparationType()
            ?: doseInstructionDraft.preparationType

    internal val scheduledNativeAmount: Double?
        get() {
            val instruction = runCatching { doseInstructionDraft.toDoseInstruction() }.getOrNull()
                ?: return null
            return when (resolvedPreparationType) {
                MedicinePreparationType.INJECTION_SINGLE_USE_VIAL ->
                    if (instruction == DoseInstruction.WholeUnit) {
                        parsePositiveDouble(medicineDraft.singleUseVialStrengthMg)
                            ?.let(medicineDraft.customDoseUnit::toMg)
                    } else {
                        null
                    }

                MedicinePreparationType.INJECTION_MULTI_USE_VIAL ->
                    (instruction as? DoseInstruction.VolumeMl)?.valueMl

                MedicinePreparationType.GEL_CONTAINER ->
                    (instruction as? DoseInstruction.WeightGrams)?.valueGrams

                else -> null
            }
        }
}

private val MedicinePreparationType.requiredActualDoseApplicationType: MedicationApplicationType?
    get() = when (this) {
        MedicinePreparationType.INJECTION_SINGLE_USE_VIAL,
        MedicinePreparationType.INJECTION_MULTI_USE_VIAL -> MedicationApplicationType.INJECTION

        MedicinePreparationType.GEL_CONTAINER -> MedicationApplicationType.GEL
        else -> null
    }
