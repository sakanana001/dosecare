package com.dosecare.app.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dosecare.app.R
import com.dosecare.app.ui.components.HazeAlertDialog
import com.dosecare.app.ui.components.HazeTopAppBar
import com.dosecare.app.ui.components.pinnedTopAppBarScrollBehavior
import com.dosecare.app.ui.components.topAppBarScrollToTop
import com.dosecare.app.ui.interaction.SearchableDrugPicker

/**
 * 双药规格对比 (two-drug spec comparison) — 选两药 → 逐字段对比。
 *
 * Ported from DoseCare's `CompareTab` (A: ui/BottomNavTabs.kt, ~lines 690-770),
 * which was a bottom-navigation tab. In this base the pharmacology features are
 * pushed routes reached from Settings, so the tab became a screen:
 *
 * - `CompareTab(catalog: DrugCatalogService, onSettingsClick)` became
 *   `CompareScreen(onSettingsClick, viewModel: CompareViewModel = hiltViewModel())`,
 *   so the catalog arrives through Hilt.
 * - A read `CompareViewModel.drugAId` / `.drugBId` / `.infoDialogOpen` off a
 *   process-wide `object`; those are now owned by [CompareViewModel], scoped to
 *   this route's back-stack entry — same treatment as
 *   `ui/interaction/InteractionViewModel.kt`.
 * - A's `TopAppBar` / `AlertDialog` became the shared Haze wrappers
 *   (`HazeTopAppBar` / `HazeAlertDialog`), because the base renders every piece
 *   of chrome through them so one app-wide blur switch controls all of it.
 *   Calling Material3's TopAppBar/AlertDialog directly would paint an opaque
 *   bar over the blur and trip HazeChromeIntegrationTest.
 * - `SearchableDrugPicker` was already merged as `ui.interaction.SearchableDrugPicker`
 *   with an identical signature, so it is reused as-is.
 * - The title uses `compare_card_title`; A's `compare_title` key was not part of
 *   the 79 merged string keys, and `compare_card_title` says the same thing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompareViewModel = hiltViewModel(),
) {
    val drugAId by viewModel.drugAId.collectAsStateWithLifecycle()
    val drugBId by viewModel.drugBId.collectAsStateWithLifecycle()
    val infoDialogOpen by viewModel.infoDialogOpen.collectAsStateWithLifecycle()
    val drugA = viewModel.tryGetDrug(drugAId)
    val drugB = viewModel.tryGetDrug(drugBId)
    val allDrugs = viewModel.allDrugs

    // v0.9g merge: the base app renders every top bar / dialog through the shared
    // Haze wrappers (ui/components/HazeChrome.kt) so one blur switch controls all
    // chrome. Calling Material3's TopAppBar/AlertDialog directly here would paint
    // an opaque bar over the blur and trip HazeChromeIntegrationTest.
    val listState = rememberLazyListState()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = pinnedTopAppBarScrollBehavior(
        lazyListState = listState,
        state = topAppBarState
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HazeTopAppBar(
                modifier = Modifier.topAppBarScrollToTop(scrollBehavior, listState),
                title = { Text(stringResource(R.string.compare_card_title)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.common_settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 说明卡 (把说明搬进感叹号对话框)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.compare_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.setInfoDialogOpen(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.compare_info),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (infoDialogOpen) {
                HazeAlertDialog(
                    onDismissRequest = { viewModel.setInfoDialogOpen(false) },
                    title = { Text(stringResource(R.string.compare_info_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.compare_info_lines), style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.setInfoDialogOpen(false) }) { Text(stringResource(R.string.common_known)) }
                    }
                )
            }

            SearchableDrugPicker(
                label = stringResource(R.string.compare_drug_a),
                selected = drugA,
                options = allDrugs,
                onSelect = {
                    // 换了药 A 就清掉药 B:原来的 B 可能是按旧的 A 选的,留着会让人以为
                    // 这组对比还有意义。来源: DoseCare CompareTab 同样会清空 B。
                    viewModel.setDrugAId(it.id)
                    viewModel.setDrugBId(null)
                }
            )
            SearchableDrugPicker(
                label = stringResource(R.string.compare_drug_b),
                selected = drugB,
                options = allDrugs.filter { it.id != drugAId },
                onSelect = { viewModel.setDrugBId(it.id) }
            )
            if (drugA != null && drugB != null) {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                SpecComparison(drugA, drugB)
            }
        }
    }
}
