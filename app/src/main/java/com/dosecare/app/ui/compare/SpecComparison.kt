package com.dosecare.app.ui.compare

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.EffectStrength
import com.dosecare.app.model.pharmacology.PkModel
import com.dosecare.app.model.pharmacology.RiskLevel
import com.dosecare.app.model.pharmacology.labelRes
import com.dosecare.app.ui.pharmacology.displayName
import com.dosecare.app.ui.pharmacology.secondaryDisplayName
import com.dosecare.app.model.pharmacology.monitoringFrequencyRes
import kotlin.math.abs

/**
 * 双药规格对比 (Spec Comparison)
 *
 * 借鉴手机规格对比页的设计:
 * - 顶部: 药 A / 药 B 名称 header
 * - 免责声明
 * - 多个 section: 基本信息 / 药代动力学 / 治疗窗 / CYP 角色 / 关键不良反应 / 剂量调整 / 监测
 * - 同一 section 下: 相同值合并成单格全宽居中;不同值分两列对比
 * - 底部: 关键相互作用清单 (含 triggerDrugId 引用)
 *
 * 与 RuleEngine 评估的关系:
 * - SpecComparison 静态展示两药的所有字段
 * - RuleEngine 评估给出动态警示(代谢/相加/风险)
 * - 两者并存,SpecComparison 在前,RuleEngine 结果在后
 *
 * Ported from DoseCare's `ui/ComparisonView.kt` (A). Differences are all
 * mechanical, see the notes on each helper:
 * - Import paths `domain.catalog.*` / `domain.pk.*` -> `model.pharmacology.*`.
 * - A carried `@StringRes val displayNameRes: Int` on the enums; this base keeps
 *   the domain layer free of `R` references, so the localized labels come from
 *   the `labelRes()` mappings in `model/pharmacology/PharmaLabels.kt`:
 *   `drug.category.displayNameRes` -> `drug.category.labelRes()`,
 *   `cyp.displayNameRes` -> `cyp.labelRes()`,
 *   `pathwayType.displayNameRes` -> `pathwayType.labelRes()`,
 *   `overdose.severity.displayNameRes` -> `overdose.severity.labelRes()`.
 * - `strengthZh(name: String)` matched on the enum's *name*; it now takes the
 *   `EffectStrength` value itself, which is the same mapping with the compiler
 *   checking it.
 * - `SpecSection` had two `drugA` / `drugB` parameters that were never read;
 *   they are gone.
 * - The therapeutic-window row rendered an empty pair of parentheses when a
 *   drug has no guideline source; the source is now appended only when present.
 * - The renal / hepatic dose-adjustment rows and the monitoring-frequency row
 *   printed the raw enum name ("CONTRAINDICATED IN SEVERE", "MONITOR",
 *   "EVERY_3_MONTHS"); they now go through the localized label mappings in
 *   `model/pharmacology/PharmaLabels.kt`.
 */
@Composable
fun SpecComparison(drugA: Drug, drugB: Drug) {
    val sections = buildComparisonSections(drugA, drugB)
    var plainMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        ComparisonHeader(drugA, drugB)

        Spacer(Modifier.height(12.dp))

        // 通俗模式 toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                if (plainMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = stringResource(R.string.compare_lay_title),
                tint = if (plainMode) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.compare_lay_title), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(4.dp))
            Switch(checked = plainMode, onCheckedChange = { plainMode = it })
        }

        if (plainMode) {
            Spacer(Modifier.height(4.dp))
            PlainKeyDiffCard(drugA, drugB)
            Spacer(Modifier.height(8.dp))
        }

        // Disclaimer
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.compare_lay_disclaimer),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Spacer(Modifier.height(12.dp))

        sections.forEach { (title, rows) ->
            SpecSection(title, rows, plainMode)
        }

        // 关键相互作用清单(引用对方 triggerDrugId 的)
        Spacer(Modifier.height(12.dp))
        CriticalInteractionsSection(drugA, drugB, plainMode)
    }
}

/** 通俗模式摘要: 列出两药关键差异, 一句话给患者/家属 */
@Composable
private fun PlainKeyDiffCard(drugA: Drug, drugB: Drug) {
    val diffs = mutableListOf<String>()
    // 治疗窗
    if (drugA.therapeuticWindow != null && drugB.therapeuticWindow != null) {
        val aLowHigh = "${drugA.therapeuticWindow!!.low.toInt()}-${drugA.therapeuticWindow!!.high.toInt()}"
        val bLowHigh = "${drugB.therapeuticWindow!!.low.toInt()}-${drugB.therapeuticWindow!!.high.toInt()}"
        if (aLowHigh != bLowHigh) {
            diffs += stringResource(R.string.cmp_diff_window_format, drugA.displayName(), aLowHigh, drugB.displayName(), bLowHigh)
        }
    } else if (drugA.therapeuticWindow == null && drugB.therapeuticWindow != null) {
        diffs += stringResource(R.string.cmp_diff_window_no_a, drugA.displayName(), drugB.displayName())
    } else if (drugB.therapeuticWindow == null && drugA.therapeuticWindow != null) {
        diffs += stringResource(R.string.cmp_diff_window_no_b, drugB.displayName(), drugA.displayName())
    }
    // 半衰期
    if (drugA.pkModel is PkModel.OneCompartmentWithAbsorption && drugB.pkModel is PkModel.OneCompartmentWithAbsorption) {
        val aT12 = drugA.pkModel.tHalfHours
        val bT12 = drugB.pkModel.tHalfHours
        if (abs(aT12 - bT12) > 6) {
            val aDesc = if (aT12 < 24) stringResource(R.string.cmp_diff_half_life_short)
                        else if (aT12 < 72) stringResource(R.string.cmp_diff_half_life_medium)
                        else stringResource(R.string.cmp_diff_half_life_xlong)
            val bDesc = if (bT12 < 24) stringResource(R.string.cmp_diff_half_life_short)
                        else if (bT12 < 72) stringResource(R.string.cmp_diff_half_life_medium)
                        else stringResource(R.string.cmp_diff_half_life_xlong)
            diffs += stringResource(R.string.cmp_diff_half_life_format, drugA.displayName(), aT12.toInt(), aDesc, drugB.displayName(), bT12.toInt(), bDesc)
        }
    }
    // 风险
    val aHigh = mutableListOf<String>().apply {
        if (drugA.adverseEffects.qtcProlongation == RiskLevel.HIGH || drugA.adverseEffects.qtcProlongation == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_qt))
        if (drugA.adverseEffects.agranulocytosis == RiskLevel.HIGH || drugA.adverseEffects.agranulocytosis == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_agran))
        if (drugA.adverseEffects.extrapyramidal == RiskLevel.HIGH || drugA.adverseEffects.extrapyramidal == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_eps))
        if (drugA.adverseEffects.sedation == RiskLevel.HIGH || drugA.adverseEffects.sedation == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_sedation))
    }
    val bHigh = mutableListOf<String>().apply {
        if (drugB.adverseEffects.qtcProlongation == RiskLevel.HIGH || drugB.adverseEffects.qtcProlongation == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_qt))
        if (drugB.adverseEffects.agranulocytosis == RiskLevel.HIGH || drugB.adverseEffects.agranulocytosis == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_agran))
        if (drugB.adverseEffects.extrapyramidal == RiskLevel.HIGH || drugB.adverseEffects.extrapyramidal == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_eps))
        if (drugB.adverseEffects.sedation == RiskLevel.HIGH || drugB.adverseEffects.sedation == RiskLevel.VERY_HIGH) add(stringResource(R.string.cmp_diff_highrisk_sedation))
    }
    val onlyA = aHigh - bHigh.toSet()
    val onlyB = bHigh - aHigh.toSet()
    if (onlyA.isNotEmpty()) diffs += stringResource(R.string.cmp_diff_only_a_format, drugA.displayName(), onlyA.joinToString("、"))
    if (onlyB.isNotEmpty()) diffs += stringResource(R.string.cmp_diff_only_b_format, drugB.displayName(), onlyB.joinToString("、"))
    // CYP 差异
    if (drugA.cypProfile.inhibitors.isNotEmpty() && drugB.cypProfile.inhibitors.isEmpty()) {
        diffs += stringResource(R.string.cmp_diff_cyp_inh_a, drugA.displayName(), drugB.displayName(), drugA.displayName(), drugB.displayName())
    } else if (drugB.cypProfile.inhibitors.isNotEmpty() && drugA.cypProfile.inhibitors.isEmpty()) {
        diffs += stringResource(R.string.cmp_diff_cyp_inh_b, drugB.displayName(), drugA.displayName(), drugB.displayName(), drugA.displayName())
    } else if (drugA.cypProfile.inducers.isNotEmpty() && drugB.cypProfile.inducers.isEmpty()) {
        diffs += stringResource(R.string.cmp_diff_cyp_ind_a, drugA.displayName(), drugB.displayName())
    } else if (drugB.cypProfile.inducers.isNotEmpty() && drugA.cypProfile.inducers.isEmpty()) {
        diffs += stringResource(R.string.cmp_diff_cyp_ind_b, drugB.displayName(), drugA.displayName())
    }
    // 老年/肾/肝
    if (drugA.adjustments.elderly != drugB.adjustments.elderly && (drugA.adjustments.elderly != null || drugB.adjustments.elderly != null)) {
        diffs += stringResource(R.string.cmp_diff_elderly_diff)
    }

    if (diffs.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.compare_diff_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.compare_diff_empty), style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(stringResource(R.string.compare_diff_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                diffs.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeader(drugA: Drug, drugB: Drug) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DrugHeaderCard(drugA, modifier = Modifier.weight(1f))
        DrugHeaderCard(drugB, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DrugHeaderCard(drug: Drug, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                drug.displayName(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                drug.secondaryDisplayName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(drug.category.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SpecSection(
    @StringRes titleRes: Int,
    rows: List<Pair<String, String>>,
    plain: Boolean = false
) {
    val plainNote = if (plain) sectionPlainNote(titleRes) else null
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plainNote != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "💡 $plainNote",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // Rows
        rows.forEach { (valueA, valueB) ->
            SpecRow(valueA, valueB)
        }
        HorizontalDivider()
    }
}

/** Section 标题对应的通俗解释 (i18n via cmp_section_desc_*) */
@Composable
private fun sectionPlainNote(@StringRes titleRes: Int): String? = when (titleRes) {
    R.string.cmp_section_basic -> stringResource(R.string.cmp_section_desc_basic)
    R.string.cmp_section_pk -> stringResource(R.string.cmp_section_desc_pk)
    R.string.cmp_section_window -> stringResource(R.string.cmp_section_desc_window)
    R.string.cmp_section_cyp -> stringResource(R.string.cmp_section_desc_cyp)
    R.string.cmp_section_adverse -> stringResource(R.string.cmp_section_desc_adverse)
    R.string.cmp_section_adjust -> stringResource(R.string.cmp_section_desc_adjust)
    R.string.cmp_section_monitor -> stringResource(R.string.cmp_section_desc_monitor)
    else -> null
}

@Composable
private fun SpecRow(valueA: String, valueB: String) {
    if (valueA.isNotEmpty() && valueA == valueB) {
        // 相同: 合并成单格全宽居中
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                valueA,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        // 不同: 分两列
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            ValueCell(valueA, modifier = Modifier.weight(1f), isLeft = true)
            ValueCell(valueB, modifier = Modifier.weight(1f), isLeft = false)
        }
    }
}

@Composable
private fun ValueCell(value: String, modifier: Modifier = Modifier, isLeft: Boolean) {
    Surface(
        modifier = modifier,
        color = if (isLeft) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface
    ) {
        Text(
            value.ifEmpty { "—" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CriticalInteractionsSection(drugA: Drug, drugB: Drug, plain: Boolean = false) {
    val intersectA = drugA.criticalInteractions.filter { it.triggerDrugId == drugB.id }
    val intersectB = drugB.criticalInteractions.filter { it.triggerDrugId == drugA.id }
    if (intersectA.isEmpty() && intersectB.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.compare_critical_inbound),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plain) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.compare_hint_legend),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // drugA → drugB (drugA 写「我会影响 drugB」)
        intersectA.forEach { hint ->
            InteractionCard(
                title = "${drugA.displayName()} → ${drugB.displayName()}",
                mechanism = hint.mechanism.name.replace("_", " "),
                severity = hint.severity,
                note = hint.clinicalNote,
                auc = hint.aucFoldChange
            )
        }
        // drugB → drugA
        intersectB.forEach { hint ->
            InteractionCard(
                title = "${drugB.displayName()} → ${drugA.displayName()}",
                mechanism = hint.mechanism.name.replace("_", " "),
                severity = hint.severity,
                note = hint.clinicalNote,
                auc = hint.aucFoldChange
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun InteractionCard(
    title: String,
    mechanism: String,
    severity: String,
    note: String,
    auc: Pair<Double, Double>
) {
    val sevColor = when (severity) {
        "CONTRAINDICATED" -> Color(0xFFB71C1C)
        "HIGH" -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFEF6C00)
        "LOW" -> Color(0xFFFBC02D)
        else -> Color(0xFF9E9E9E)
    }
    Surface(
        color = sevColor.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, sevColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = sevColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        severity,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.compare_mechanism_auc, mechanism, auc.first.toString(), auc.second.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = sevColor
            )
            Spacer(Modifier.height(4.dp))
            Text(note, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ============================================================
// Data builder
// ============================================================
//
// buildComparisonSections 改为 @Composable, 返回 List<Pair<@StringRes Int, ...>>,
// section 标题用 cmp_section_*, 字段 label 用 cmp_label_*, RiskLevel/CypStrength
// 翻译用 risk_level_* / cyp_strength_*.

@Composable
private fun buildComparisonSections(
    drugA: Drug,
    drugB: Drug
): List<Pair<Int, List<Pair<String, String>>>> {
    val pkA = drugA.pkModel as? PkModel.OneCompartmentWithAbsorption
    val pkB = drugB.pkModel as? PkModel.OneCompartmentWithAbsorption

    val wA = drugA.therapeuticWindow
    val wB = drugB.therapeuticWindow
    val windowStr = when {
        wA != null && wB != null -> fmtWindow(wA) to fmtWindow(wB)
        wA != null -> fmtWindow(wA) to "—"
        wB != null -> "—" to fmtWindow(wB)
        else -> "—" to "—"
    }

    return listOf(
        R.string.cmp_section_basic to listOf(
            drugA.displayName() to drugB.displayName(),
            drugA.secondaryDisplayName() to drugB.secondaryDisplayName(),
            (drugA.atc ?: "—") to (drugB.atc ?: "—"),
            stringResource(drugA.category.labelRes()) to stringResource(drugB.category.labelRes()),
            (drugA.subcategory ?: "—") to (drugB.subcategory ?: "—"),
        ),
        R.string.cmp_section_pk to listOf(
            "${pkA?.tHalfHours?.let { "%.1f".format(it) } ?: "—"} h" to
                "${pkB?.tHalfHours?.let { "%.1f".format(it) } ?: "—"} h",
            "F ${pkA?.f ?: "—"}" to "F ${pkB?.f ?: "—"}",
            "ka ${pkA?.kaPerHour ?: "—"}" to "ka ${pkB?.kaPerHour ?: "—"}",
            "ke ${pkA?.kePerHour ?: "—"}" to "ke ${pkB?.kePerHour ?: "—"}",
            "Vd ${pkA?.vdLiters?.toInt() ?: "—"} L" to "Vd ${pkB?.vdLiters?.toInt() ?: "—"} L",
            stringResource(R.string.cmp_label_protein_binding, drugA.proteinBindingPct) to
                stringResource(R.string.cmp_label_protein_binding, drugB.proteinBindingPct),
        ),
        R.string.cmp_section_window to listOf(windowStr),
        R.string.cmp_section_cyp to listOf(
            fmtCypSubs(drugA) to fmtCypSubs(drugB),
            fmtCypInh(drugA) to fmtCypInh(drugB),
            fmtCypInd(drugA) to fmtCypInd(drugB),
            stringResource(R.string.cmp_label_pathway, fmtPathway(drugA)) to
                stringResource(R.string.cmp_label_pathway, fmtPathway(drugB))
        ),
        R.string.cmp_section_adverse to listOf(
            stringResource(R.string.cmp_label_qtc, riskLabel(drugA.adverseEffects.qtcProlongation)) to
                stringResource(R.string.cmp_label_qtc, riskLabel(drugB.adverseEffects.qtcProlongation)),
            stringResource(R.string.cmp_label_metabolic, riskLabel(drugA.adverseEffects.metabolicSyndrome)) to
                stringResource(R.string.cmp_label_metabolic, riskLabel(drugB.adverseEffects.metabolicSyndrome)),
            stringResource(R.string.cmp_label_agranulocytosis, riskLabel(drugA.adverseEffects.agranulocytosis)) to
                stringResource(R.string.cmp_label_agranulocytosis, riskLabel(drugB.adverseEffects.agranulocytosis)),
            stringResource(R.string.cmp_label_eps, riskLabel(drugA.adverseEffects.extrapyramidal)) to
                stringResource(R.string.cmp_label_eps, riskLabel(drugB.adverseEffects.extrapyramidal)),
            stringResource(R.string.cmp_label_sedation, riskLabel(drugA.adverseEffects.sedation)) to
                stringResource(R.string.cmp_label_sedation, riskLabel(drugB.adverseEffects.sedation)),
            stringResource(R.string.cmp_label_sexual, riskLabel(drugA.adverseEffects.sexual)) to
                stringResource(R.string.cmp_label_sexual, riskLabel(drugB.adverseEffects.sexual)),
            stringResource(R.string.cmp_label_prolactin, riskLabel(drugA.adverseEffects.hyperprolactinemia)) to
                stringResource(R.string.cmp_label_prolactin, riskLabel(drugB.adverseEffects.hyperprolactinemia)),
            stringResource(R.string.cmp_label_anticholinergic, drugA.adverseEffects.anticholinergicLoad) to
                stringResource(R.string.cmp_label_anticholinergic, drugB.adverseEffects.anticholinergicLoad)
        ),
        R.string.cmp_section_adjust to listOf(
            stringResource(R.string.cmp_label_renal, stringResource(drugA.adjustments.renal.labelRes())) to
                stringResource(R.string.cmp_label_renal, stringResource(drugB.adjustments.renal.labelRes())),
            stringResource(R.string.cmp_label_hepatic, stringResource(drugA.adjustments.hepatic.labelRes())) to
                stringResource(R.string.cmp_label_hepatic, stringResource(drugB.adjustments.hepatic.labelRes())),
            (drugA.adjustments.elderly?.let { stringResource(R.string.cmp_label_elderly_val, it) }
                ?: stringResource(R.string.cmp_label_elderly_empty)) to
                (drugB.adjustments.elderly?.let { stringResource(R.string.cmp_label_elderly_val, it) }
                    ?: stringResource(R.string.cmp_label_elderly_empty))
        ),
        R.string.cmp_section_monitor to listOf(
            (drugA.monitoring.frequency?.let { monitoringFrequencyText(it) }
                ?: stringResource(R.string.cmp_label_freq_empty)) to
                (drugB.monitoring.frequency?.let { monitoringFrequencyText(it) }
                    ?: stringResource(R.string.cmp_label_freq_empty)),
            (if (drugA.monitoring.items.isEmpty()) stringResource(R.string.cmp_label_items_empty)
             else stringResource(R.string.cmp_label_items, drugA.monitoring.items.joinToString("、"))) to
                (if (drugB.monitoring.items.isEmpty()) stringResource(R.string.cmp_label_items_empty)
                 else stringResource(R.string.cmp_label_items, drugB.monitoring.items.joinToString("、")))
        ),
        R.string.cmp_section_overdose to listOf(
            fmtOverdoseShort(drugA) to fmtOverdoseShort(drugB)
        )
    )
}

/**
 * 治疗窗字符串。指南来源为空时不渲染空括号 —— 原实现会输出 "0.5-1.5 ng/mL ()"。
 */
private fun fmtWindow(w: PkModel.TherapeuticWindow): String {
    val range = "${w.low}-${w.high} ${w.unit}"
    val source = w.guidelineSource
    return if (source.isNullOrBlank()) range else "$range ($source)"
}

@Composable
private fun fmtOverdoseShort(d: Drug): String {
    val o = d.overdose ?: return stringResource(R.string.cmp_label_no_entry)
    val toxic = o.toxicDoseEstimateMg?.toInt()?.let { stringResource(R.string.cmp_label_toxic, it) } ?: ""
    val fatal = o.fatalDoseEstimateMg?.toInt()?.let { stringResource(R.string.cmp_label_fatal, it) } ?: ""
    val doses = listOf(toxic, fatal).filter { it.isNotEmpty() }.joinToString(" / ")
    val sev = stringResource(o.severity.labelRes())
    val antid = o.antidote?.let { stringResource(R.string.cmp_label_antidote, it) } ?: ""
    return when {
        doses.isEmpty() && antid.isEmpty() -> stringResource(R.string.cmp_label_severity, sev)
        doses.isEmpty() -> stringResource(R.string.cmp_label_severity, sev) + antid
        else -> stringResource(R.string.cmp_label_severity_with_doses, sev, doses, antid)
    }
}

private fun fmtCypSubs(d: Drug): String = d.cypProfile.substrates
    .joinToString("、") { "${it.cyp.displayName} ${(it.fraction * 100).toInt()}%" }
    .ifEmpty { "—" }  // NOTE: cyp.displayName 是国际化无关的枚举名 (CYP1A2 等), 无需 i18n

@Composable
private fun fmtCypInh(d: Drug): String {
    val labels = d.cypProfile.inhibitors.map { "${stringResource(it.cyp.labelRes())} ${strengthLabel(it.strength)}" }
    return labels.joinToString("、").ifEmpty { "—" }
}

@Composable
private fun fmtCypInd(d: Drug): String {
    val labels = d.cypProfile.inducers.map { "${stringResource(it.cyp.labelRes())} ${strengthLabel(it.strength)}" }
    return labels.joinToString("、").ifEmpty { "—" }
}

@Composable
private fun fmtPathway(d: Drug): String {
    val cp = d.cypProfile
    val pt = cp.pathwayType?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.cmp_label_pathway_empty)
    val pp = cp.primaryPathway ?: "—"
    return "$pt · $pp"
}

@Composable
private fun strengthLabel(strength: EffectStrength): String = when (strength) {
    EffectStrength.STRONG -> stringResource(R.string.cyp_strength_strong)
    EffectStrength.MODERATE -> stringResource(R.string.cyp_strength_moderate)
    EffectStrength.WEAK -> stringResource(R.string.cyp_strength_weak)
}

@Composable
private fun riskLabel(level: RiskLevel): String = when (level) {
    RiskLevel.VERY_HIGH -> stringResource(R.string.risk_level_very_high)
    RiskLevel.HIGH -> stringResource(R.string.risk_level_high)
    RiskLevel.MEDIUM -> stringResource(R.string.risk_level_medium)
    RiskLevel.LOW -> stringResource(R.string.risk_level_low)
    RiskLevel.VERY_LOW -> stringResource(R.string.risk_level_very_low)
}

/**
 * 监测频次: 目录里是自由字符串, 查表命中就本地化, 没命中就原样显示
 * (目录更新后可能出现新值, 宁可显示原值也不要丢内容)。
 */
@Composable
private fun monitoringFrequencyText(raw: String): String {
    val res = monitoringFrequencyRes(raw)
    return if (res != null) {
        stringResource(R.string.cmp_label_freq_val, stringResource(res))
    } else {
        stringResource(R.string.cmp_label_freq_val, raw)
    }
}
