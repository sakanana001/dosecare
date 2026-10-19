package com.dosecare.app.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosecare.app.data.repository.MedicationGroupRepository
import com.dosecare.app.data.repository.MedicineIdentityCollisionException
import com.dosecare.app.data.repository.MedicineLockedException
import com.dosecare.app.data.repository.MedicineReferencedByActiveGroupException
import com.dosecare.app.data.repository.MedicineRepository
import com.dosecare.app.data.repository.MedicineStockRepository
import com.dosecare.app.data.repository.SettingsRepository
import com.dosecare.app.data.repository.StockReceived
import com.dosecare.app.data.repository.StockRecount
import com.dosecare.app.model.medication.DoseInstruction
import com.dosecare.app.model.medication.MedicationApplicationType
import com.dosecare.app.model.medication.MedicationGroup
import com.dosecare.app.model.medication.Medicine
import com.dosecare.app.model.medication.MedicinePreparation
import com.dosecare.app.model.medication.MedicineStock
import com.dosecare.app.model.medication.MedicineStockProjection
import com.dosecare.app.model.medication.RunwayProjection
import com.dosecare.app.model.medication.isArchived
import com.dosecare.app.model.pharmacology.DrugCatalogService
import com.dosecare.app.ui.medication.PatchSpecKind
import com.dosecare.app.util.systemLocale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject

/**
 * Backs `MedicineDetailScreen`. Owns:
 *  - the currently-observed [Medicine] (live from [MedicineRepository.observeByUuid]);
 *  - the list of active groups that still reference the medicine — the input
 *    to the archive-blocking guard;
 *  - the locked flag (true if any log row references the medicine, which the
 *    repo enforces server-side for `updatePreparation`).
 *
 * Display-name edits and preparation edits each go through their own typed
 * result so the screen can render distinct messages — locked vs identity
 * collision vs referenced-by-group are each user-actionable in different
 * ways.
 */
@HiltViewModel
class MedicineDetailViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
    medicationGroupRepository: MedicationGroupRepository,
    private val stockRepository: MedicineStockRepository,
    private val settingsRepository: SettingsRepository,
    // v0.9g merge: resolves medicine.drugCatalogId into a catalog Drug for display.
    private val drugCatalog: com.dosecare.app.model.pharmacology.DrugCatalogService,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val medicineUuid: UUID = run {
        val raw = checkNotNull(savedStateHandle.get<String>(MEDICINE_ID_ARG)) {
            "MedicineDetailViewModel requires a medicineId nav argument."
        }
        UUID.fromString(raw)
    }

    // Tracks the lock state independently of the live medicine flow so a
    // user-initiated refresh re-asks the repo without waiting for an
    // upstream emission.
    private val isLockedFlow = MutableStateFlow(false)
    private val displayNameTextFlow = MutableStateFlow<String?>(null)
    private val archiveResultFlow = MutableStateFlow<MedicineArchiveResult?>(null)
    private val saveResultFlow = MutableStateFlow<MedicineDetailSaveResult?>(null)
    private val stockMutationResultFlow = MutableStateFlow<MedicineStockMutationResult?>(null)
    private var stockMutationJob: Job? = null

    // Seed from the eagerly-cached repo StateFlows so the first composition
    // lands on a populated screen (medicine != null, stock subcard present)
    // instead of a centered loading spinner that pops to populated content.
    private val _uiState = MutableStateFlow(initialUiStateFromCaches(medicationGroupRepository))
    val uiState: StateFlow<MedicineDetailUiState> = _uiState.asStateFlow()

    init {
        // One collector keeps the assembled UI state in lockstep with every
        // upstream flow — there is no second writer to _uiState other than
        // setPreparationDraft (UI-owned local state).
        viewModelScope.launch {
            combine(
                medicineRepository.observeByUuid(medicineUuid),
                medicationGroupRepository.observeGroups().map { it.orEmpty() },
                isLockedFlow,
                combine(
                    displayNameTextFlow,
                    combine(
                        archiveResultFlow,
                        saveResultFlow,
                        stockMutationResultFlow,
                    ) { archiveResult, saveResult, stockMutationResult ->
                        MutationResults(
                            archiveResult = archiveResult,
                            saveResult = saveResult,
                            stockMutationResult = stockMutationResult,
                        )
                    },
                    settingsRepository.settingsState,
                    stockRepository.observeProjections()
                        .map { projections ->
                            projections.firstOrNull { projection ->
                                projection.medicine.uuid == medicineUuid
                            }
                        },
                ) { displayNameDraft, mutationResults, settingsState, stockProjection ->
                    ResultsAndSettings(
                        displayNameDraft = displayNameDraft,
                        archiveResult = mutationResults.archiveResult,
                        saveResult = mutationResults.saveResult,
                        stockMutationResult = mutationResults.stockMutationResult,
                        firstDayOfWeek = settingsState.firstDayOfWeekOption
                            .resolve(systemLocale()),
                        stockProjection = stockProjection,
                    )
                },
            ) { medicine, groups, isLocked, derived ->
                buildState(
                    medicine = medicine,
                    groups = groups,
                    isLocked = isLocked,
                    displayNameDraft = derived.displayNameDraft,
                    archiveResult = derived.archiveResult,
                    saveResult = derived.saveResult,
                    stockMutationResult = derived.stockMutationResult,
                    firstDayOfWeek = derived.firstDayOfWeek,
                    stockProjection = derived.stockProjection,
                )
            }.collect { state ->
                _uiState.update { current ->
                    // Preserve preparationDraft from the UI side; nothing
                    // upstream owns it.
                    val stockProjection = if (current.archiveInProgress) {
                        current.stockProjection
                    } else {
                        state.stockProjection
                    }
                    val preservedState = state.copy(
                        preparationDraft = current.preparationDraft,
                        stockProjection = stockProjection,
                        showAdjustSheet = current.showAdjustSheet,
                        showDisableConfirmation = current.showDisableConfirmation,
                        adjustSheetActiveTab = current.adjustSheetActiveTab,
                        pendingEnableTracking = current.pendingEnableTracking,
                        archiveInProgress = current.archiveInProgress,
                    )
                    consumeOpenOptInRequestIfReady(preservedState)
                }
            }
        }

        refreshLockState()
    }

    fun refreshLockState() {
        viewModelScope.launch {
            isLockedFlow.value = medicineRepository.isLocked(medicineUuid)
        }
    }

    fun updateDisplayNameText(text: String) {
        displayNameTextFlow.value = text
    }

    /** Drops the result toast/snackbar after the UI consumes it. */
    fun clearSaveResult() {
        saveResultFlow.value = null
    }

    fun clearArchiveResult() {
        archiveResultFlow.value = null
    }

    fun clearStockMutationResult() {
        stockMutationResultFlow.value = null
    }

    fun setPreparationDraft(draft: MedicinePreparationDraftUiState?) {
        _uiState.update { it.copy(preparationDraft = draft) }
    }

    fun openAdjustSheet(initialTab: AdjustSheetTab = AdjustSheetTab.RECEIVED) {
        _uiState.update {
            it.copy(
                showAdjustSheet = true,
                adjustSheetActiveTab = initialTab,
                pendingEnableTracking = false,
            )
        }
    }

    fun closeAdjustSheet() {
        _uiState.update {
            it.copy(
                showAdjustSheet = false,
                pendingEnableTracking = false,
            )
        }
    }

    fun openOptIn() {
        _uiState.update {
            it.copy(
                showAdjustSheet = true,
                adjustSheetActiveTab = AdjustSheetTab.RECEIVED,
                pendingEnableTracking = true,
            )
        }
    }

    fun openOpenContainerDialog() {
        _uiState.update { it.copy(showOpenContainerDialog = true) }
    }

    fun closeOpenContainerDialog() {
        _uiState.update { it.copy(showOpenContainerDialog = false) }
    }

    fun submitOpenContainerAmount(amount: Double): Job = launchStockMutation {
        medicineRepository.applySetOpenContainerAmount(medicineUuid, amount)
        _uiState.update { it.copy(showOpenContainerDialog = false) }
    }

    fun openDisableConfirmation() {
        _uiState.update { it.copy(showDisableConfirmation = true) }
    }

    fun closeDisableConfirmation() {
        _uiState.update { it.copy(showDisableConfirmation = false) }
    }

    fun confirmDisableTracking(): Job {
        _uiState.update { it.copy(showDisableConfirmation = false) }
        return launchStockMutation(
            onFailure = {
                _uiState.update { it.copy(showDisableConfirmation = true) }
            },
        ) {
            medicineRepository.disableTracking(medicineUuid)
        }
    }

    fun confirmEnableTracking(
        unitsRemaining: Double?,
        openContainerAmount: Double?,
        unitsLastTotal: Double?,
    ): Job = launchStockMutation {
        medicineRepository.enableTracking(
            uuid = medicineUuid,
            initialUnitsRemaining = unitsRemaining,
            initialOpenContainerAmount = openContainerAmount,
            initialUnitsLastTotal = unitsLastTotal,
        )
    }

    fun submitWarnAt(days: Int): Job = launchStockMutation {
        medicineRepository.updateWarnAtDaysRemaining(medicineUuid, days)
    }

    fun previewRunway(hypotheticalStock: MedicineStock): RunwayProjection? {
        return stockRepository.previewRunway(medicineUuid, hypotheticalStock)
    }

    fun submitRecount(recount: StockRecount): Job {
        if (!_uiState.value.showAdjustSheet) return ignoredStockMutation()
        return launchStockMutation {
            if (closePendingEnableAdjustSheet()) return@launchStockMutation
            medicineRepository.applyRecount(medicineUuid, recount)
            _uiState.update { it.copy(showAdjustSheet = false) }
        }
    }

    fun submitReceived(received: StockReceived): Job {
        if (!_uiState.value.showAdjustSheet) return ignoredStockMutation()
        return launchStockMutation {
            if (_uiState.value.pendingEnableTracking) {
                val medicine = _uiState.value.stockProjection?.medicine
                val initialUnitsRemaining = (medicine?.stock?.unitsRemaining ?: 0.0) +
                        received.unitsReceived
                // The stock mutator owns container topology details, including
                // auto-opening the first received container when no open amount
                // exists. The sheet passes the counted total and leaves the open
                // amount unset so that behavior stays centralized.
                medicineRepository.enableTracking(
                    uuid = medicineUuid,
                    initialUnitsRemaining = initialUnitsRemaining,
                    initialOpenContainerAmount = null,
                    initialUnitsLastTotal = initialUnitsRemaining,
                )
                _uiState.update {
                    it.copy(
                        showAdjustSheet = false,
                        pendingEnableTracking = false,
                    )
                }
            } else {
                medicineRepository.applyReceived(medicineUuid, received)
                _uiState.update { it.copy(showAdjustSheet = false) }
            }
        }
    }

    private fun ignoredStockMutation(): Job = viewModelScope.launch {}

    private fun closePendingEnableAdjustSheet(): Boolean {
        if (!_uiState.value.pendingEnableTracking) return false
        _uiState.update {
            it.copy(
                showAdjustSheet = false,
                pendingEnableTracking = false,
            )
        }
        return true
    }

    private fun consumeOpenOptInRequestIfReady(
        state: MedicineDetailUiState,
    ): MedicineDetailUiState {
        if (savedStateHandle.get<Boolean>(OPEN_OPT_IN_ARG) != true) return state
        val projection = state.stockProjection ?: return state
        savedStateHandle[OPEN_OPT_IN_ARG] = false
        if (projection.medicine.stock.trackingEnabled) return state
        if (projection.medicine.preparation is MedicinePreparation.PatchOff) return state
        return state.copy(
            showAdjustSheet = true,
            adjustSheetActiveTab = AdjustSheetTab.RECEIVED,
            pendingEnableTracking = true,
        )
    }

    private fun launchStockMutation(
        onFailure: (() -> Unit)? = null,
        block: suspend () -> Unit,
    ): Job {
        stockMutationJob?.takeIf(Job::isActive)?.let { return it }

        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onFailure?.invoke()
                stockMutationResultFlow.value = MedicineStockMutationResult.FAILURE
            } finally {
                if (stockMutationJob == job) {
                    stockMutationJob = null
                }
            }
        }
        stockMutationJob = job
        job.start()
        return job
    }

    fun saveDisplayName(): Job = viewModelScope.launch {
        val draft = displayNameTextFlow.value ?: return@launch
        val sanitized = draft.trim().takeIf(String::isNotBlank)
        runCatching {
            medicineRepository.setDisplayName(medicineUuid, sanitized)
        }.onSuccess {
            saveResultFlow.value = MedicineDetailSaveResult.SUCCESS
        }.onFailure {
            saveResultFlow.value = MedicineDetailSaveResult.FAILURE_OTHER
        }
    }

    // One-shot orchestration for the merged display-name + preparation sheet:
    // saves the buffered display-name and, when the medicine is unlocked, the
    // user-edited preparation. Emits a single saveResult so the toast reflects
    // the final outcome instead of racing two independent saves.
    fun saveAll(
        preparation: MedicinePreparation?,
        displayDoseUnit: com.dosecare.app.model.medication.MedicineDisplayDoseUnit? = null,
    ): Job = viewModelScope.launch {
        val displayNameDraft = displayNameTextFlow.value
        if (displayNameDraft != null) {
            val sanitized = displayNameDraft.trim().takeIf(String::isNotBlank)
            val result = runCatching {
                medicineRepository.setDisplayName(medicineUuid, sanitized)
            }
            if (result.isFailure) {
                saveResultFlow.value = MedicineDetailSaveResult.FAILURE_OTHER
                return@launch
            }
        }
        if (preparation != null) {
            val result = runCatching {
                medicineRepository.updatePreparation(
                    uuid = medicineUuid,
                    preparation = preparation,
                    displayDoseUnit = displayDoseUnit,
                )
            }
            if (result.isFailure) {
                saveResultFlow.value = when (result.exceptionOrNull()) {
                    is MedicineLockedException -> MedicineDetailSaveResult.FAILURE_LOCKED
                    is MedicineIdentityCollisionException ->
                        MedicineDetailSaveResult.FAILURE_IDENTITY_COLLISION

                    else -> MedicineDetailSaveResult.FAILURE_OTHER
                }
                return@launch
            }
            isLockedFlow.value = medicineRepository.isLocked(medicineUuid)
        }
        saveResultFlow.value = MedicineDetailSaveResult.SUCCESS
    }

    fun savePreparation(
        preparation: MedicinePreparation,
        displayDoseUnit: com.dosecare.app.model.medication.MedicineDisplayDoseUnit? = null,
    ): Job = viewModelScope.launch {
        runCatching {
            medicineRepository.updatePreparation(
                uuid = medicineUuid,
                preparation = preparation,
                displayDoseUnit = displayDoseUnit,
            )
        }.onSuccess {
            saveResultFlow.value = MedicineDetailSaveResult.SUCCESS
            // The preparation update may have unlocked nothing, but the
            // medicine's identityKey changed — re-read isLocked to be safe.
            isLockedFlow.value = medicineRepository.isLocked(medicineUuid)
        }.onFailure { error ->
            saveResultFlow.value = when (error) {
                is MedicineLockedException -> MedicineDetailSaveResult.FAILURE_LOCKED
                is MedicineIdentityCollisionException ->
                    MedicineDetailSaveResult.FAILURE_IDENTITY_COLLISION

                else -> MedicineDetailSaveResult.FAILURE_OTHER
            }
        }
    }

    fun archive(): Job? {
        // Defensive client-side guard: even if the user taps archive when
        // the button was momentarily enabled (between an upstream emission
        // and the disabled state propagating), do not submit. The repo
        // still throws if a group is added concurrently — handled below.
        if (_uiState.value.linkedActiveSlots.isNotEmpty()) {
            return null
        }
        _uiState.update { it.copy(archiveInProgress = true) }
        return viewModelScope.launch {
            // The archive is irreversible; complete it and publish its result
            // even if the entry is torn down mid-write. The nav lock already
            // blocks the back/chrome teardown vectors on this pushed route, so
            // this is belt-and-suspenders matching the bulk-delete convention.
            withContext(NonCancellable) {
                runCatching { medicineRepository.archive(medicineUuid) }
                    .onSuccess { archiveResultFlow.value = MedicineArchiveResult.SUCCESS }
                    .onFailure { error ->
                        _uiState.update { it.copy(archiveInProgress = false) }
                        archiveResultFlow.value = when (error) {
                            is MedicineReferencedByActiveGroupException ->
                                MedicineArchiveResult.FAILURE_REFERENCED_BY_ACTIVE_GROUP

                            else -> MedicineArchiveResult.FAILURE_OTHER
                        }
                    }
            }
        }
    }

    private fun initialUiStateFromCaches(
        medicationGroupRepository: MedicationGroupRepository,
    ): MedicineDetailUiState {
        val medicine = medicineRepository.getCachedActiveMedicine(medicineUuid)
        if (medicine == null) {
            return MedicineDetailUiState()
        }
        return buildState(
            medicine = medicine,
            groups = medicationGroupRepository.getCachedGroups().orEmpty(),
            isLocked = false,
            displayNameDraft = null,
            archiveResult = null,
            saveResult = null,
            stockMutationResult = null,
            firstDayOfWeek = settingsRepository.settingsState.value.firstDayOfWeekOption
                .resolve(systemLocale()),
            stockProjection = stockRepository.getCachedProjection(medicineUuid),
        )
    }

    private fun buildState(
        medicine: Medicine?,
        groups: List<MedicationGroup>,
        isLocked: Boolean,
        displayNameDraft: String?,
        archiveResult: MedicineArchiveResult?,
        saveResult: MedicineDetailSaveResult?,
        stockMutationResult: MedicineStockMutationResult?,
        firstDayOfWeek: DayOfWeek,
        stockProjection: MedicineStockProjection?,
    ): MedicineDetailUiState {
        val linkedActiveSlots = if (medicine == null) {
            emptyList()
        } else {
            groups.asSequence()
                .filterNot(MedicationGroup::isArchived)
                .flatMap { group ->
                    group.medications.asSequence()
                        .filter { medication -> medication.medicineUuid == medicineUuid }
                        .map { medication ->
                            LinkedSlotRow(
                                group = group,
                                doseInstruction = medication.doseInstruction,
                                applicationType = medication.applicationType,
                                count = medication.count,
                            )
                        }
                }
                .sortedWith(
                    compareBy<LinkedSlotRow> { it.group.name.lowercase() }
                        .thenBy { it.applicationType }
                )
                .toList()
        }
        val displayName = displayNameDraft ?: medicine?.displayName.orEmpty()
        return MedicineDetailUiState(
            medicine = medicine,
            isLocked = isLocked,
            linkedActiveSlots = linkedActiveSlots,
            displayNameText = displayName,
            archiveResult = archiveResult,
            saveResult = saveResult,
            stockMutationResult = stockMutationResult,
            firstDayOfWeek = firstDayOfWeek,
            stockProjection = stockProjection,
            // v0.9g merge: resolve the link through the catalog. A blank id or one the
            // catalog no longer carries both read as "no dossier", so a stale link is
            // visible and clearable instead of looking like a broken row.
            dossier = medicine?.drugCatalogId
                ?.takeIf { it.isNotBlank() }
                ?.let(drugCatalog::tryGetById),
        )
    }

    /**
     * The whole 202-drug catalog, name-sorted for the dossier picker.
     *
     * Computed once: the catalog is an immutable in-memory snapshot loaded at startup,
     * so there is nothing to invalidate.
     */
    val catalogDrugs: List<com.dosecare.app.model.pharmacology.Drug> by lazy {
        drugCatalog.all().sortedBy { it.genericNameZh }
    }

    fun openDossierPicker() {
        _uiState.value = _uiState.value.copy(showDossierPicker = true)
    }

    fun closeDossierPicker() {
        _uiState.value = _uiState.value.copy(showDossierPicker = false)
    }

    /** Link the medicine to a catalog entry. Empty string unlinks. */
    fun linkDossier(drugId: String) {
        if (_uiState.value.dossierMutationInProgress) return
        _uiState.value = _uiState.value.copy(dossierMutationInProgress = true)
        viewModelScope.launch {
            runCatching {
                medicineRepository.updateDrugCatalogId(medicineUuid, drugId)
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    dossierMutationInProgress = false,
                    showDossierPicker = false,
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                // The medicine row disappears only if it was deleted underneath us, in
                // which case the detail screen is already tearing down. Leaving the
                // in-progress flag would strand the section, so clear it either way.
                _uiState.value = _uiState.value.copy(dossierMutationInProgress = false)
            }
        }
    }

    fun unlinkDossier() = linkDossier("")

    private data class MutationResults(
        val archiveResult: MedicineArchiveResult?,
        val saveResult: MedicineDetailSaveResult?,
        val stockMutationResult: MedicineStockMutationResult?,
    )

    private data class ResultsAndSettings(
        val displayNameDraft: String?,
        val archiveResult: MedicineArchiveResult?,
        val saveResult: MedicineDetailSaveResult?,
        val stockMutationResult: MedicineStockMutationResult?,
        val firstDayOfWeek: DayOfWeek,
        val stockProjection: MedicineStockProjection?,
    )

    companion object {
        const val MEDICINE_ID_ARG = "medicineId"
        const val OPEN_OPT_IN_ARG = "openOptIn"
    }
}


data class MedicineDetailUiState(
    val medicine: Medicine? = null,
    val isLocked: Boolean = false,
    val linkedActiveSlots: List<LinkedSlotRow> = emptyList(),
    val displayNameText: String = "",
    val preparationDraft: MedicinePreparationDraftUiState? = null,
    val archiveResult: MedicineArchiveResult? = null,
    val saveResult: MedicineDetailSaveResult? = null,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val stockProjection: MedicineStockProjection? = null,
    val stockMutationResult: MedicineStockMutationResult? = null,
    val showAdjustSheet: Boolean = false,
    val showDisableConfirmation: Boolean = false,
    val showOpenContainerDialog: Boolean = false,
    val adjustSheetActiveTab: AdjustSheetTab = AdjustSheetTab.RECEIVED,
    val pendingEnableTracking: Boolean = false,
    val archiveInProgress: Boolean = false,
    // v0.9g merge: the pharmacology dossier linked to this medicine, resolved from
    // medicine.drugCatalogId through the catalog service. Null means "not linked" OR
    // "linked to an id the catalog no longer has" — the two are indistinguishable to the
    // UI and are deliberately treated the same, so a stale link reads as "no dossier"
    // rather than as an error the user cannot clear.
    val dossier: com.dosecare.app.model.pharmacology.Drug? = null,
    val showDossierPicker: Boolean = false,
    val dossierMutationInProgress: Boolean = false,
)

enum class AdjustSheetTab {
    RECEIVED,
    RECOUNT,
}

data class LinkedSlotRow(
    val group: MedicationGroup,
    val doseInstruction: DoseInstruction,
    val applicationType: MedicationApplicationType,
    val count: Int,
)

/**
 * Mirrors the picker's preparation draft but stripped to what the detail
 * editor owns — the medicine's identity (selection + category) is fixed
 * here, only the physical preparation can change.
 */
data class MedicinePreparationDraftUiState(
    val preparationType: com.dosecare.app.model.medication.MedicinePreparationType,
    val pillStrengthMg: String = "",
    val singleUseVialStrengthMg: String = "",
    val concentrationMgPerMl: String = "",
    val vialVolumeMl: String = "",
    val gelConcentrationPercent: String = "",
    val sachetWeightGrams: String = "",
    val containerWeightGrams: String = "",
    val patchSpecKind: PatchSpecKind = PatchSpecKind.TOTAL_MG,
    val patchTotalMg: String = "",
    val patchReleaseRateMcgPerDay: String = "",
    // Custom-medicine raw-mass unit; ignored on catalog medicines (always MG).
    // The typed mass string is interpreted in this unit on save and rescaled
    // back from stored mg when the dialog re-opens.
    val displayDoseUnit: com.dosecare.app.model.medication.MedicineDisplayDoseUnit =
        com.dosecare.app.model.medication.MedicineDisplayDoseUnit.MG,
)

enum class MedicineArchiveResult {
    SUCCESS,
    FAILURE_REFERENCED_BY_ACTIVE_GROUP,
    FAILURE_OTHER,
}

enum class MedicineDetailSaveResult {
    SUCCESS,
    FAILURE_LOCKED,
    FAILURE_IDENTITY_COLLISION,
    FAILURE_OTHER,
}

enum class MedicineStockMutationResult {
    SUCCESS,
    FAILURE,
}
