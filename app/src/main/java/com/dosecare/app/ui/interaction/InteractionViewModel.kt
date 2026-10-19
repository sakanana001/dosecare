package com.dosecare.app.ui.interaction

import androidx.lifecycle.ViewModel
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

    fun evaluate() {
        // RuleEngine.evaluate() resolves every id via catalog.getById(), which
        // throws NoSuchElementException on an unknown id. Ids are only ever
        // populated from the catalog itself, but mapNotNull keeps a stale
        // selection from turning into a crash.
        val userDrugs = _selectedIds.value.mapNotNull { id ->
            val drug = catalog.tryGetById(id) ?: return@mapNotNull null
            UserDrugForRule(drug.id, defaultDose(drug), 2.0)
        }
        _results.value = ruleEngine.evaluate(userDrugs, PatientProfile())
        _hasEvaluated.value = true
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
