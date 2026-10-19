package com.dosecare.app.ui.interaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dosecare.app.R
import com.dosecare.app.model.pharmacology.AnticholinergicLoadInteraction
import com.dosecare.app.model.pharmacology.CypInteraction
import com.dosecare.app.model.pharmacology.Interaction
import com.dosecare.app.model.pharmacology.OverdoseSeverity
import com.dosecare.app.model.pharmacology.QtcInteraction
import com.dosecare.app.model.pharmacology.SerotoninSyndromeRisk
import com.dosecare.app.model.pharmacology.Severity

/**
 * 药物相互作用检测：选药 → 评估 → 展示结果。
 *
 * Ported from DoseCare's `InteractionsTab` (A: ui/BottomNavTabs.kt), with
 * `InteractionRow` / `MechanismLine` / `severityColor` from the same file.
 *
 * Differences from A, all mechanical:
 * - `InteractionsTab(catalog: DrugCatalogService, onSettingsClick)` became
 *   `InteractionScreen(onSettingsClick, viewModel: InteractionViewModel =
 *   hiltViewModel())`, so the catalog and the RuleEngine arrive through Hilt.
 * - A built `RuleEngine(catalog)` inline in the button's onClick; B's RuleEngine
 *   is a Hilt singleton, so [InteractionViewModel.evaluate] owns that call.
 * - A's info dialog read `CompareViewModel.infoDialogOpen` (shared with the
 *   compare tab); it is now [InteractionViewModel.infoDialogOpen].
 * - `collectAsState` -> `collectAsStateWithLifecycle`, the convention elsewhere
 *   in B (e.g. ui/navigation/HrtTrackerNavHost.kt).
 * - `displayNameRes` (an enum constructor property in A) -> `labelRes`, see
 *   InteractionLabels.kt.
 *
 * Visual structure and interaction logic are otherwise unchanged.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InteractionScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InteractionViewModel = hiltViewModel(),
) {
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val hasEvaluated by viewModel.hasEvaluated.collectAsStateWithLifecycle()
    val infoDialogOpen by viewModel.infoDialogOpen.collectAsStateWithLifecycle()
    val availableDrugs = remember(selectedIds) { viewModel.allDrugs.filter { it.id !in selectedIds } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interaction_title, selectedIds.size)) },
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
            // Intro (说明搬入感叹号 AlertDialog)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.interaction_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.setInfoDialogOpen(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.interaction_info),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (infoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { viewModel.setInfoDialogOpen(false) },
                    title = { Text(stringResource(R.string.interaction_info_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.interaction_info_lines), style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.setInfoDialogOpen(false) }) { Text(stringResource(R.string.common_known)) }
                    }
                )
            }

            // 已选药 (chips 可删)
            if (selectedIds.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.interaction_selected_n, selectedIds.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedIds.forEach { id ->
                                val drug = viewModel.tryGetDrug(id)
                                if (drug != null) {
                                    InputChip(
                                        selected = false,
                                        onClick = { viewModel.removeDrug(id) },
                                        label = { Text(drug.genericNameZh) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.common_remove),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // v0.9g merge: import the user's own regimen in one tap. Without this the
            // selection starts empty every visit and the user re-picks their own
            // medications out of a 202-entry catalog by hand. Hidden when nothing in
            // the regimen is linked to the catalog — it would dead-end.
            if (viewModel.regimenDrugCount > 0) {
                OutlinedButton(
                    onClick = { viewModel.selectMyRegimen() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            R.string.interaction_use_my_regimen_n,
                            viewModel.regimenDrugCount,
                        )
                    )
                }
            }

            // 搜索选药 (替代之前 take(50) 的 dropdown,修复"剩余 73 不可点"bug)
            SearchableDrugPicker(
                label = stringResource(R.string.interaction_add_drug_n, availableDrugs.size),
                selected = null,
                options = availableDrugs,
                onSelect = { drug -> viewModel.selectDrug(drug.id) },
                placeholder = stringResource(R.string.interaction_search_hint)
            )

            // 评估按钮
            Button(
                onClick = { viewModel.evaluate() },
                enabled = selectedIds.size >= 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasEvaluated) stringResource(R.string.interaction_reevaluate_n, selectedIds.size)
                    else stringResource(R.string.interaction_evaluate_n, selectedIds.size)
                )
            }

            // 结果
            if (hasEvaluated) {
                if (results.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.interaction_no_alerts_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.interaction_no_alerts_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.interaction_alerts_count, results.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))
                            results.forEach { InteractionRow(it) }
                        }
                    }
                }
                // 过量警示汇总 (与 RuleEngine 警示并行显示)
                val highOverdose = selectedIds.mapNotNull { id ->
                    val d = viewModel.tryGetDrug(id) ?: return@mapNotNull null
                    d.overdose?.takeIf { it.severity == OverdoseSeverity.LIFE_THREATENING || it.severity == OverdoseSeverity.SEVERE }
                }
                if (highOverdose.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFB71C1C),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.interaction_high_overdose_n, highOverdose.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            highOverdose.forEach { o ->
                                val drugName = selectedIds.firstNotNullOfOrNull { id ->
                                    viewModel.tryGetDrug(id)?.takeIf { it.overdose === o }?.genericNameZh
                                } ?: "?"
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        "• $drugName (${stringResource(o.severity.labelRes)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB71C1C),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    o.antidote?.let { ant ->
                                        Text(
                                            " 💉 $ant",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB71C1C)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.interaction_overdose_help),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InteractionRow(interaction: Interaction) {
    val color = severityColor(interaction.severity)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        stringResource(interaction.severity.labelRes),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    interaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(interaction.detail, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.interaction_advice, interaction.advice),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            MechanismLine(interaction)
            if (interaction.references.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.interaction_references, interaction.references.take(2).joinToString(";")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MechanismLine(interaction: Interaction) {
    val text = when (interaction) {
        is CypInteraction -> stringResource(
            R.string.compare_mechanism_auc,
            interaction.cyp.displayName,  // CYP1A2 等是通用代码, 无需 i18n
            "%.1f".format(interaction.patientAdjustedFold),
            "%.1f".format(interaction.patientAdjustedFold)
        )
        // TODO(v0.9d done): 3 行改 R.string.interaction_qtc_format / interaction_acb_format / interaction_serotonin_format
        is QtcInteraction -> stringResource(R.string.interaction_qtc_format, interaction.finalEstimatedQtcMs)
        is AnticholinergicLoadInteraction -> stringResource(R.string.interaction_acb_format, interaction.totalScore)
        is SerotoninSyndromeRisk -> stringResource(R.string.interaction_serotonin_format, interaction.serotonergicDrugs.size)
        else -> null
    }
    if (text != null) {
        Spacer(Modifier.height(2.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun severityColor(severity: Severity): Color = Color(android.graphics.Color.parseColor(severity.color))
