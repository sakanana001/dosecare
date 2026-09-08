package com.dosecare.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.pk.DoseEvent
import com.dosecare.app.domain.pk.PkCurve
import com.dosecare.app.domain.pk.PkEngine
import com.dosecare.app.domain.pk.PkModel

/**
 * 血药浓度推测模块 (TDM, Therapeutic Drug Monitoring)
 *
 * 借鉴参考 App 的设计:
 * - 选有治疗窗的药(精神科优先)
 * - 输入剂量 + 频次
 * - 跑一房室口服 PK 模型推测稳态浓度曲线
 * - 治疗窗对比,给出"在窗内/低/高"警示
 *
 * v0.4 多药扩展 (本次更新):
 * - 支持 1-N 种药同图比较
 * - 每药独立颜色 + 治疗窗色带
 * - 估计给药时间点用实心圆标记
 * - 每药可单独 toggle 显隐
 * - 横轴 0-48h 或 0-96h (2-4 个稳态周期)
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
fun TdmModule(catalog: DrugCatalogService, modifier: Modifier = Modifier) {
    val tdmDrugs = remember {
        catalog.all()
            .filter { it.therapeuticWindow != null }
            .sortedBy { it.genericNameZh }
    }
    val allDrugs = remember { catalog.all().sortedBy { it.genericNameZh } }

    var selections by remember {
        mutableStateOf<List<TdmSelection>>(emptyList())
    }
    var addExpanded by remember { mutableStateOf(false) }
    var infoDialogOpen by remember { mutableStateOf(false) }
    val availableForAdd = remember(selections) {
        allDrugs.filter { d -> selections.none { it.drug.id == d.id } }
    }

    // 添加药时:弹搜索框 → 选完后给个新颜色、填默认剂量
    Card(
        modifier = modifier.fillMaxWidth(),
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
                "血药浓度推测 (TDM)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { infoDialogOpen = true }) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "数据源与校准说明",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (tdmDrugs.isEmpty()) {
            Text(
                "暂无可 TDM 监测的药物(需要 therapeuticWindow 字段)",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (infoDialogOpen) {
        AlertDialog(
            onDismissRequest = { infoDialogOpen = false },
            title = { Text("📚 数据源 & 校准说明") },
            text = {
                Column {
                    Text("• PK 参数: AGNP 2017 (Therapeutic Drug Monitoring in Psychiatry) + FDA DailyMed 药品标签", style = MaterialTheme.typography.bodySmall)
                    Text("• CYP 谱: Flockhart Table (Indiana University)", style = MaterialTheme.typography.bodySmall)
                    Text("• 治疗窗: AGNP 2017 共识指南", style = MaterialTheme.typography.bodySmall)
                    Text("• 体重调整: Vd = vdLPerKg × weight(假设线性)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ 局限性",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• 1 房室口服模型 — 对非线性药(苯妥英)不准确,对肾清除药(锂)仅近似", style = MaterialTheme.typography.bodySmall)
                    Text("• 群体平均参数 — 个体差异 2-3 倍(CYP 基因型 / 吸烟 / 年龄 / 肾 / 肝)", style = MaterialTheme.typography.bodySmall)
                    Text("• 多药曲线仅显示浓度叠加,不模拟 CYP 竞争/酶抑制动力学", style = MaterialTheme.typography.bodySmall)
                    Text("• v0.4 不替代医师判断;实际用药请遵医嘱或咨询临床药师", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogOpen = false }) { Text("知道了") }
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
                    // 10 次给药,稳态后画 0-48h (2 个周期) 或 0-96h (4 个周期)
                    val doses = (0 until 14).map { DoseEvent(dose, it * tau) }
                    val curve = engine.multiDoseCurve(
                        model = adjusted,
                        doseEvents = doses,
                        tStart = 0.0,   // 包含早期给药,看稳态形成
                        tEnd = tau * 5, // 5 个稳态周期
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
            Text("推测 ${selections.size} 药血药浓度")
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
                        "添加药到图表",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${availableForAdd.size} 个药可选 (有/无 TDM 均可)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { addExpanded = true }) {
                    Text("添加")
                }
            }
        }
    }

    if (addExpanded) {
        SearchableDrugPicker(
            label = "添加药",
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
                Text(
                    selection.drug.genericNameZh,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "TDM" + if (selection.drug.therapeuticWindow != null) "" else " (无窗)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selection.drug.therapeuticWindow != null) selection.color else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = {
                    onUpdate(selection.copy(visible = !selection.visible))
                }) {
                    Icon(
                        if (selection.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (selection.visible) "隐藏" else "显示",
                        tint = if (selection.visible) selection.color else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "移除", tint = MaterialTheme.colorScheme.error)
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
                    label = { Text("剂量 (mg)", style = MaterialTheme.typography.labelSmall) },
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
                    label = { Text("体重 (kg)", style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            // 频次
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "频次:",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                listOf(1, 2, 3, 4).forEach { freq ->
                    FilterChip(
                        selected = selection.frequencyPerDay == freq,
                        onClick = { onUpdate(selection.copy(frequencyPerDay = freq)) },
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
    // v0.7: 改用 InteractiveConcentrationChart (可点击/拖动/缩放 + 修复 yMax bug)
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
        title = "多药稳态浓度-时间曲线 (可点击/拖动/缩放)",
        height = 320.dp
    )
}

private fun Double.formatTrimmed(): String = if (this >= 10) "%.0f".format(this) else "%.1f".format(this)

/**
 * 在 PkCurve 上求时间 t 时的浓度 (线性插值)
 * 曲线离散 (step 0.25h),用最近两点插值
 */
private fun predictCAt(curve: PkCurve, t: Double): Double {
    if (curve.t.isEmpty()) return 0.0
    if (t <= curve.t.first()) return curve.c.first()
    if (t >= curve.t.last()) return curve.c.last()
    // 二分查找
    var lo = 0
    var hi = curve.t.size - 1
    while (lo < hi - 1) {
        val mid = (lo + hi) / 2
        if (curve.t[mid] < t) lo = mid else hi = mid
    }
    val t0 = curve.t[lo]
    val t1 = curve.t[hi]
    val c0 = curve.c[lo]
    val c1 = curve.c[hi]
    val dt = t1 - t0
    return if (dt < 1e-9) c0 else c0 + (c1 - c0) * (t - t0) / dt
}
