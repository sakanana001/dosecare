package com.dosecare.app.ui.tdm

import androidx.lifecycle.ViewModel
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.DrugCatalogService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * State holder for the TDM (therapeutic drug monitoring) screen.
 *
 * Ported from DoseCare's `ui/TdmModule.kt`, where the 202-drug catalog was a
 * Composable parameter threaded down from the caller. In this base the catalog
 * is a Hilt singleton (di/PharmacologyModule.kt), so it is injected here instead,
 * matching how `ui/interaction/InteractionViewModel.kt` does it.
 *
 * The catalog itself is immutable reference data loaded once from
 * `assets/pharmacology/catalog.json`, so the two derived lists are computed
 * eagerly at construction rather than being recomputed per composition. The
 * per-screen selection state (which drugs, what dose, what frequency) stays in
 * `TdmScreen` as `remember` state, exactly as it was in the original module —
 * it is pure UI state with no reason to survive process death.
 */
@HiltViewModel
class TdmViewModel @Inject constructor(
    catalog: DrugCatalogService,
) : ViewModel() {

    /** 目录里全部药物，按中文通用名排序（选药器候选）。 */
    val allDrugs: List<Drug> = catalog.all().sortedBy { it.genericNameZh }

    /**
     * 带治疗窗的药物子集。底座目录里每个药都有 pkModel，所以这一集不为空时
     * 说明数据正常；为空说明 catalog 资产缺失或损坏，此时 UI 会给出提示而不是
     * 静默显示空页面。
     */
    val tdmDrugs: List<Drug> = catalog.all()
        .filter { it.therapeuticWindow != null }
        .sortedBy { it.genericNameZh }
}
