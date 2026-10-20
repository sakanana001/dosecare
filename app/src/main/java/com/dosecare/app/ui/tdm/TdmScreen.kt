package com.dosecare.app.ui.tdm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dosecare.app.R
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.DoseEvent
import com.dosecare.app.model.pharmacology.PkCurve
import com.dosecare.app.model.pharmacology.PkEngine
import com.dosecare.app.model.pharmacology.PkModel
import com.dosecare.app.ui.pharmacology.displayName
import com.dosecare.app.ui.components.HazeAlertDialog
import com.dosecare.app.ui.components.HazeTopAppBar
import com.dosecare.app.ui.components.pinnedTopAppBarScrollBehavior
import com.dosecare.app.ui.components.topAppBarScrollToTop
import com.dosecare.app.ui.interaction.SearchableDrugPicker

/**
 * 血药浓度推测模块 (TDM, Therapeutic Drug Monitoring)
 *
 * Ported from DoseCare's `ui/TdmModule.kt`. Same intent and same UI:
 * - 选有治疗窗的药(精神科优先)
 * - 输入剂量 + 频次
 * - 跑一房室口服 PK 模型推测稳态浓度曲线
 * - 治疗窗对比,给出"在窗内/低/高"警示
 *
 * v0.4 多药扩展:
 * - 支持 1-N 种药同图比较
 * - 每药独立颜色 + 治疗窗色带
 * - 估计给药时间点用实心圆标记
 * - 每药可单独 toggle 显隐
 *
 * Merge differences from DoseCare, all mechanical:
 * - `TdmModule(catalog: DrugCatalogService, ...)` became
 *   `TdmScreen(onSettingsClick, viewModel: TdmViewModel = hiltViewModel())`, so the
 *   catalog arrives through Hilt (same treatment as ui/interaction/InteractionScreen).
 * - Wrapped in Scaffold + TopAppBar to match InteractionScreen's screen chrome,
 *   since this is now a pushed route rather than a bottom-nav tab.
 * - `SearchableDrugPicker` and `InteractiveConcentrationChart` moved to this base;
 *   the picker was already merged as `ui.interaction.SearchableDrugPicker` and is
 *   reused as-is rather than duplicated.
 * - Import paths `domain.catalog.*` / `domain.pk.*` -> `model.pharmacology.*`.
 * - Dropped the unused private `predictCAt` / `formatTrimmed` helpers; the chart
 *   has its own `predictCAtInCurve`.
 *
 * Fixed on merge: DoseCare's `TdmModule.kt` emitted the "TDM" chip `Text` twice
 * (a copy-paste slip), so the chip rendered "TDM TDM". The duplicate was dropped
 * here; the chip now renders a single "TDM" (or the no-window label).
 *
 * 频次 chip 的 "qXh" (q6h, q8h 等) 是国际通用医学缩写,四语言通用,保留原文。
 */

// 8 种可视区分的颜色 (按 RYB 顺序)
private val tdmDrugColors = listOf(
    Color(0xFF1976D2),  // 蓝
    Color(0xFFD32F2F),  // 红
    Color(0xFF388E3C),  // 绿
    Color(0xFF7B1FA2),  // 紫
    Color(0xFFF57C00),  // 橙
    Color(0xFF0097A7),  // 青
    Color(0xFFC2185B),  // 玫红
    Color(0xFF5D4037)   // 棕
)

/** 一药在 TDM 模块中的状态 */
data class TdmSelection(
    val drug: Drug,
    val doseText: String,
    val weightText: String,
    val frequencyPerDay: Int,
    val color: Color,
    val visible: Boolean = true,
    val curve: PkCurve? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TdmScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TdmViewModel = hiltViewModel(),
) {
    // v0.9g merge: the base app renders every top bar / dialog through the shared
    // Haze wrappers (ui/components/HazeChrome.kt) so one blur switch controls all
    // chrome. Calling Material3's TopAppBar/AlertDialog directly would paint an
    // opaque bar over the blur and trip HazeChromeIntegrationTest.
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
                title = { Text(stringResource(R.string.tdm_entry_title)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.common_settings)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TdmModuleContent(
                allDrugs = viewModel.allDrugs,
                hasTdmDrugs = viewModel.tdmDrugs.isNotEmpty()
            )
        }
    }
}

@Composable
private fun TdmModuleContent(
    allDrugs: List<Drug>,
    hasTdmDrugs: Boolean
) {
    var selections by remember {
        mutableStateOf<List<TdmSelection>>(emptyList())
    }
    var addExpanded by remember { mutableStateOf(false) }
    var infoDialogOpen by remember { mutableStateOf(false) }
    val availableForAdd = remember(selections, allDrugs) {
        allDrugs.filter { d -> selections.none { it.drug.id == d.id } }
    }

    // 添加药时:弹搜索框 → 选完后给个新颜色、填默认剂量
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.tdm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { infoDialogOpen = true }) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.tdm_data_source_info),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (!hasTdmDrugs) {
            Text(
                stringResource(R.string.tdm_no_drugs),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (infoDialogOpen) {
        HazeAlertDialog(
            onDismissRequest = { infoDialogOpen = false },
            title = { Text(stringResource(R.string.tdm_data_source_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.tdm_data_source_lines), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.tdm_limitations),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.tdm_limitations_lines), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogOpen = false }) { Text(stringResource(R.string.common_known)) }
            }
        )
    }

    Spacer(Modifier.height(12.dp))

    // 已选药列表
    if (selections.isNotEmpty()) {
        selections.forEachIndexed { idx, sel ->
            TdmSelectionCard(
                index = idx,
                selection = sel,
                onUpdate = { updated -> selections = selections.mapIndexed { i, s -> if (i == idx) updated else s } },
                onRemove = { selections = selections.filterIndexed { i, _ -> i != idx } }
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val engine = PkEngine()
                selections = selections.map { sel ->
                    val dose = sel.doseText.toDoubleOrNull()
                    if (dose == null || dose <= 0) return@map sel.copy(curve = null)
                    val weight = sel.weightText.toDoubleOrNull() ?: 70.0
                    val model = sel.drug.pkModel
                    if (model !is PkModel.OneCompartmentWithAbsorption) return@map sel.copy(curve = null)
                    val baseVdPerKg = model.vdLiters / 70.0
                    val adjusted = model.copy(vdLiters = baseVdPerKg * weight)
                    val tau = 24.0 / sel.frequencyPerDay
                    // 多次给药,画到 5 个稳态周期,便于看稳态形态
                    val doses = (0 until 14).map { DoseEvent(dose, it * tau) }
                    val curve = engine.multiDoseCurve(
                        model = adjusted,
                        doseEvents = doses,
                        tStart = 0.0,
                        tEnd = tau * 5,
                        stepHours = 0.25
                    )
                    sel.copy(curve = curve)
                }
            },
            enabled = selections.any { (it.doseText.toDoubleOrNull() ?: 0.0) > 0 },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.tdm_predict_n_drugs, selections.size))
        }
    }

    // 推测结果 - 多药同图
    val visibleCurves = selections.filter { it.visible && it.curve != null }
    if (visibleCurves.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        MultiConcentrationChart(selections = visibleCurves)
    }

    Spacer(Modifier.height(12.dp))

    // 添加药按钮
    if (availableForAdd.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.tdm_add_drug_to_chart),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.tdm_add_drug_n, availableForAdd.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { addExpanded = true }) {
                    Text(stringResource(R.string.tdm_add))
                }
            }
        }
    }

    if (addExpanded) {
        SearchableDrugPicker(
            label = stringResource(R.string.tdm_add),
            selected = null,
            options = availableForAdd,
            onSelect = { drug ->
                val idx = selections.size
                val color = tdmDrugColors[idx % tdmDrugColors.size]
                val range = drug.forms.firstOrNull()?.commonDoseRangeMg
                val defaultDose = if (range != null) "%.0f".format((range.first + range.second) / 2.0) else "100"
                selections = selections + TdmSelection(
                    drug = drug,
                    doseText = defaultDose,
                    weightText = "70",
                    frequencyPerDay = 2,
                    color = color,
                    visible = true,
                    curve = null
                )
                addExpanded = false
            },
            modifier = Modifier.padding(16.dp)
        )
    }

    Spacer(Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TdmSelectionCard(
    index: Int,
    selection: TdmSelection,
    onUpdate: (TdmSelection) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = selection.color.copy(alpha = 0.08f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            // 头部: 颜色点 + 药名 + 显隐 + 删除
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(selection.color, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                val drugNamePrimary = selection.drug.displayName()
                Text(
                    drugNamePrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (selection.drug.therapeuticWindow != null) "TDM" else stringResource(R.string.tdm_no_window),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selection.drug.therapeuticWindow != null) selection.color else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = {
                    onUpdate(selection.copy(visible = !selection.visible))
                }) {
                    Icon(
                        if (selection.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (selection.visible) stringResource(R.string.tdm_hide_cd) else stringResource(R.string.tdm_show_cd),
                        tint = if (selection.visible) selection.color else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            // 剂量 + 体重
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selection.doseText,
                    onValueChange = { input ->
                        val s = input.filter { c -> c.isDigit() || c == '.' }
                        if (s.count { it == '.' } <= 1) onUpdate(selection.copy(doseText = s))
                    },
                    label = { Text(stringResource(R.string.tdm_dose_mg), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = selection.weightText,
                    onValueChange = { input ->
                        val s = input.filter { c -> c.isDigit() || c == '.' }
                        if (s.count { it == '.' } <= 1) onUpdate(selection.copy(weightText = s))
                    },
                    label = { Text(stringResource(R.string.tdm_weight_kg), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            // 频次
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.tdm_freq),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                listOf(1, 2, 3, 4).forEach { freq ->
                    FilterChip(
                        selected = selection.frequencyPerDay == freq,
                        onClick = { onUpdate(selection.copy(frequencyPerDay = freq)) },
                        // "qXh" 是国际通用医学频次缩写 (q6h = 每 6 小时), 四语言通用, 保留原文
                        label = { Text("q${24 / freq}h", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

/**
 * 多药浓度曲线:不同颜色 + 多治疗窗色带 + 给药时间点实心圆
 * 形态参考: 稳态周期内几个剂量点用实心圆显著标出
 */
@Composable
private fun MultiConcentrationChart(selections: List<TdmSelection>) {
    val series = selections.mapNotNull { sel ->
        val curve = sel.curve ?: return@mapNotNull null
        // 预测给药点: 稳态后 3 个周期
        val tau = 24.0 / sel.frequencyPerDay
        val tMax = curve.t.lastOrNull() ?: 0.0
        val tMin = curve.t.firstOrNull() ?: 0.0
        val plannedDoses = mutableListOf<Double>()
        var dT = tMax - tau * 3
        while (dT <= tMax) {
            if (dT >= tMin) plannedDoses.add(dT)
            dT += tau
        }
        ChartSeries(
            drug = sel.drug,
            curve = curve,
            color = sel.color,
            plannedDoseTimes = plannedDoses
        )
    }
    InteractiveConcentrationChart(
        series = series,
        title = stringResource(R.string.tdm_chart_title),
        height = 320.dp
    )
}
