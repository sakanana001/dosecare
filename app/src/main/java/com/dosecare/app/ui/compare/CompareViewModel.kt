package com.dosecare.app.ui.compare

import androidx.lifecycle.ViewModel
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.DrugCatalogService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * State holder for the two-drug spec comparison screen.
 *
 * Ported from DoseCare's `object CompareViewModel` (A: ui/ViewModels.kt), which
 * was a process-wide singleton of MutableStateFlows. This base's convention is
 * `@HiltViewModel` + `hiltViewModel()`, so the state is now scoped to the
 * screen's nav back-stack entry — same treatment as
 * `ui/interaction/InteractionViewModel.kt`.
 *
 * The catalog is a Hilt singleton (di/PharmacologyModule.kt), so it is injected
 * rather than threaded through as a Composable parameter.
 */
@HiltViewModel
class CompareViewModel @Inject constructor(
    private val catalog: DrugCatalogService,
) : ViewModel() {

    private val _drugAId = MutableStateFlow<String?>(null)
    val drugAId: StateFlow<String?> = _drugAId.asStateFlow()
    fun setDrugAId(id: String?) { _drugAId.value = id }

    private val _drugBId = MutableStateFlow<String?>(null)
    val drugBId: StateFlow<String?> = _drugBId.asStateFlow()
    fun setDrugBId(id: String?) { _drugBId.value = id }

    private val _infoDialogOpen = MutableStateFlow(false)
    val infoDialogOpen: StateFlow<Boolean> = _infoDialogOpen.asStateFlow()
    fun setInfoDialogOpen(v: Boolean) { _infoDialogOpen.value = v }

    /**
     * The whole catalog, name-sorted for the picker. Computed once: the catalog
     * is an immutable in-memory snapshot loaded at startup.
     */
    val allDrugs: List<Drug> by lazy { catalog.all().sortedBy { it.genericNameZh } }

    /**
     * Null-safe catalog lookup. A selected id can outlive its catalog entry
     * (e.g. the catalog asset failed to load), so the screen must not assume
     * the id resolves.
     */
    fun tryGetDrug(id: String?): Drug? = id?.let { catalog.tryGetById(it) }
}
