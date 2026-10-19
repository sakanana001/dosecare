package com.dosecare.app.ui.interaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosecare.app.data.repository.InteractionAnalysisRepository
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.DrugCatalogService
import com.dosecare.app.model.pharmacology.Interaction
import com.dosecare.app.model.pharmacology.PatientProfile
import com.dosecare.app.model.pharmacology.RuleEngine
import com.dosecare.app.model.pharmacology.UserDrugForRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State holder for the drug interaction checker.
 *
 * Ported from DoseCare's `object InteractionsViewModel` (A: ui/ViewModels.kt),
 * which was a process-wide singleton of MutableStateFlows. B's convention is
 * `@HiltViewModel` + `hiltViewModel()`, so this is now scoped to the screen's
 * nav back-stack entry. The three states and their setters carried over as-is;
 * the fourth (`infoDialogOpen`) also existed in A but the tab read it off
 * `CompareViewModel` instead, so it is now owned here where it is used.
 *
 * `catalog` and `ruleEngine` are Hilt singletons (di/PharmacologyModule.kt), so
 * they are injected rather than threaded through as Composable parameters.
 */
@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val catalog: DrugCatalogService,
    private val ruleEngine: RuleEngine,
    private val analysisRepository: InteractionAnalysisRepository,
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<List<String>>(emptyList())
    val selectedIds: StateFlow<List<String>> = _selectedIds.asStateFlow()
    fun setSelectedIds(v: List<String>) { _selectedIds.value = v }

    private val _results = MutableStateFlow<List<Interaction>>(emptyList())
    val results: StateFlow<List<Interaction>> = _results.asStateFlow()
    fun setResults(v: List<Interaction>) { _results.value = v }

    private val _hasEvaluated = MutableStateFlow(false)
    val hasEvaluated: StateFlow<Boolean> = _hasEvaluated.asStateFlow()
    fun setHasEvaluated(v: Boolean) { _hasEvaluated.value = v }

    private val _infoDialogOpen = MutableStateFlow(false)
    val infoDialogOpen: StateFlow<Boolean> = _infoDialogOpen.asStateFlow()
    fun setInfoDialogOpen(v: Boolean) { _infoDialogOpen.value = v }

    /**
     * The whole catalog, name-sorted for the picker. Computed once: the catalog
     * is an immutable in-memory snapshot loaded at startup.
     */
    val allDrugs: List<Drug> by lazy { catalog.all().sortedBy { it.genericNameZh } }

    fun tryGetDrug(id: String): Drug? = catalog.tryGetById(id)

    /** Adding or removing a drug invalidates the previous run's result. */
    fun selectDrug(drugId: String) {
        _selectedIds.value = _selectedIds.value + drugId
        invalidateResult()
    }

    fun removeDrug(drugId: String) {
        _selectedIds.value = _selectedIds.value - drugId
        invalidateResult()
    }

    private fun invalidateResult() {
        _hasEvaluated.value = false
        _results.value = emptyList()
    }

    /**
     * How many catalog-linked medicines the user's regimen currently holds.
     *
     * Drives whether the "use my regimen" shortcut is worth showing: with nothing
     * linked there is nothing to import, and offering the button would dead-end.
     */
    var regimenDrugCount: Int = 0
        private set

    /**
     * Replace the selection with everything the user's regimen links to the catalog.
     *
     * This is the shortcut that makes the screen worth having: without it the user
     * re-picks their own medications from a 202-entry catalog on every visit. The
     * selection stays editable afterwards — adding or removing from the picker still
     * works, because "what am I evaluating" and "what am I taking" are not the same
     * question.
     */
    fun selectMyRegimen() {
        viewModelScope.launch {
            val ids = analysisRepository.currentUserDrugs().map { it.drugCatalogId }
            _selectedIds.value = ids
            invalidateResult()
        }
    }

    init {
        // Count once so the shortcut can be hidden on an empty regimen. Recomputing on
        // every composition would hit the database per frame.
        viewModelScope.launch {
            regimenDrugCount = analysisRepository.currentUserDrugs().size
        }
    }

    /**
     * Evaluate the current selection.
     *
     * Doses and frequencies come from the user's own regimen wherever the app can
     * supply them — the bridge maps each linked medicine's preparation strength and
     * its group's schedule into `UserDrugForRule`. That matters because the engine
     * scales AUC folds by dose and frequency, so a synthetic dose would grade a
     * real interaction at the wrong severity.
     *
     * Ids the user picked by hand that no regimen slot backs fall back to the
     * catalog's midpoint dose at twice daily, which is what DoseCare did for
     * every drug. Ids the catalog does not know are dropped rather than thrown on:
     * `RuleEngine.evaluate` resolves via `getById`, which throws on a miss.
     */
    fun evaluate() {
        val selected = _selectedIds.value
        if (selected.isEmpty()) {
            _results.value = emptyList()
            _hasEvaluated.value = true
            return
        }
        // Reading the regimen hits the database, so the whole evaluation runs on
        // viewModelScope rather than on the caller.
        viewModelScope.launch {
            val fromRegimen = analysisRepository.currentUserDrugs().associateBy { it.drugCatalogId }
            val userDrugs = selected.mapNotNull { id ->
                val drug = catalog.tryGetById(id) ?: return@mapNotNull null
                fromRegimen[id] ?: UserDrugForRule(
                    drug.id,
                    defaultDose(drug),
                    DEFAULT_FREQUENCY_PER_DAY,
                )
            }
            _results.value = ruleEngine.evaluate(userDrugs, PatientProfile())
            _hasEvaluated.value = true
        }
    }

    private companion object {
        const val DEFAULT_FREQUENCY_PER_DAY = 2.0
    }
}

/**
 * Dose used when evaluating: midpoint of the drug's first form's common dose
 * range. Same rule as A (A: ui/BottomNavTabs.kt `defaultDose`).
 */
private fun defaultDose(drug: Drug): Double {
    val range = drug.forms.firstOrNull()?.commonDoseRangeMg
    return if (range != null) (range.first + range.second) / 2.0 else 100.0
}
