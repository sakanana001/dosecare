package com.dosecare.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.RiskLevel
import com.dosecare.app.domain.pk.PkModel

/**
 * 双药规格对比 (Spec Comparison)
 *
 * 借鉴手机规格对比页(参考截图)的设计:
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
                contentDescription = "通俗模式",
                tint = if (plainMode) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("📖 通俗模式", style = MaterialTheme.typography.labelMedium)
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
                "⚠️ 各项参数请以实际药品说明书/化验值为准。\n用药调整、相互作用警示请遵医嘱或咨询临床药师。",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Spacer(Modifier.height(12.dp))

        sections.forEach { (title, rows) ->
            SpecSection(title, rows, plainMode, drugA, drugB)
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
            diffs += "• 治疗窗 (血药安全范围): ${drugA.genericNameZh} = $aLowHigh, ${drugB.genericNameZh} = $bLowHigh (单位相同)"
        }
    } else if (drugA.therapeuticWindow == null && drugB.therapeuticWindow != null) {
        diffs += "• ${drugA.genericNameZh} 无明确治疗窗, ${drugB.genericNameZh} 需要 TDM 监测"
    } else if (drugB.therapeuticWindow == null && drugA.therapeuticWindow != null) {
        diffs += "• ${drugB.genericNameZh} 无明确治疗窗, ${drugA.genericNameZh} 需要 TDM 监测"
    }
    // 半衰期
    if (drugA.pkModel is PkModel.OneCompartmentWithAbsorption && drugB.pkModel is PkModel.OneCompartmentWithAbsorption) {
        val aT12 = drugA.pkModel.tHalfHours
        val bT12 = drugB.pkModel.tHalfHours
        if (kotlin.math.abs(aT12 - bT12) > 6) {
            val aDesc = if (aT12 < 24) "短效" else if (aT12 < 72) "长效" else "超长效"
            val bDesc = if (bT12 < 24) "短效" else if (bT12 < 72) "长效" else "超长效"
            diffs += "• 半衰期: ${drugA.genericNameZh} ≈ ${aT12.toInt()}h ($aDesc), ${drugB.genericNameZh} ≈ ${bT12.toInt()}h ($bDesc)"
        }
    }
    // 风险
    val aHigh = mutableListOf<String>().apply {
        if (drugA.adverseEffects.qtcProlongation == RiskLevel.HIGH || drugA.adverseEffects.qtcProlongation == RiskLevel.VERY_HIGH) add("QT 延长")
        if (drugA.adverseEffects.agranulocytosis == RiskLevel.HIGH || drugA.adverseEffects.agranulocytosis == RiskLevel.VERY_HIGH) add("粒缺")
        if (drugA.adverseEffects.extrapyramidal == RiskLevel.HIGH || drugA.adverseEffects.extrapyramidal == RiskLevel.VERY_HIGH) add("EPS")
        if (drugA.adverseEffects.sedation == RiskLevel.HIGH || drugA.adverseEffects.sedation == RiskLevel.VERY_HIGH) add("镇静")
    }
    val bHigh = mutableListOf<String>().apply {
        if (drugB.adverseEffects.qtcProlongation == RiskLevel.HIGH || drugB.adverseEffects.qtcProlongation == RiskLevel.VERY_HIGH) add("QT 延长")
        if (drugB.adverseEffects.agranulocytosis == RiskLevel.HIGH || drugB.adverseEffects.agranulocytosis == RiskLevel.VERY_HIGH) add("粒缺")
        if (drugB.adverseEffects.extrapyramidal == RiskLevel.HIGH || drugB.adverseEffects.extrapyramidal == RiskLevel.VERY_HIGH) add("EPS")
        if (drugB.adverseEffects.sedation == RiskLevel.HIGH || drugB.adverseEffects.sedation == RiskLevel.VERY_HIGH) add("镇静")
    }
    val onlyA = aHigh - bHigh.toSet()
    val onlyB = bHigh - aHigh.toSet()
    if (onlyA.isNotEmpty()) diffs += "• ${drugA.genericNameZh} 独有的高风险: ${onlyA.joinToString("、")}"
    if (onlyB.isNotEmpty()) diffs += "• ${drugB.genericNameZh} 独有的高风险: ${onlyB.joinToString("、")}"
    // CYP 差异
    if (drugA.cypProfile.inhibitors.isNotEmpty() && drugB.cypProfile.inhibitors.isEmpty()) {
        diffs += "• ${drugA.genericNameZh} 是 CYP 抑制剂, ${drugB.genericNameZh} 不是 → 联用时 ${drugA.genericNameZh} 会让 ${drugB.genericNameZh} 浓度升高"
    } else if (drugB.cypProfile.inhibitors.isNotEmpty() && drugA.cypProfile.inhibitors.isEmpty()) {
        diffs += "• ${drugB.genericNameZh} 是 CYP 抑制剂, ${drugA.genericNameZh} 不是 → 联用时 ${drugB.genericNameZh} 会让 ${drugA.genericNameZh} 浓度升高"
    } else if (drugA.cypProfile.inducers.isNotEmpty() && drugB.cypProfile.inducers.isEmpty()) {
        diffs += "• ${drugA.genericNameZh} 是 CYP 强诱导剂, 会让 ${drugB.genericNameZh} 失效 (需加量或换药)"
    } else if (drugB.cypProfile.inducers.isNotEmpty() && drugA.cypProfile.inducers.isEmpty()) {
        diffs += "• ${drugB.genericNameZh} 是 CYP 强诱导剂, 会让 ${drugA.genericNameZh} 失效 (需加量或换药)"
    }
    // 老年/肾/肝
    if (drugA.adjustments.elderly != drugB.adjustments.elderly && (drugA.adjustments.elderly != null || drugB.adjustments.elderly != null)) {
        diffs += "• 老年人使用注意: 两药说明不一致, 请遵医嘱"
    }

    if (diffs.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("📖 关键差异 (通俗版)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("两药在主要指标上没有明显差异。具体使用仍请遵医嘱。", style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("📖 关键差异 (通俗版)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                drug.genericNameZh,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                drug.genericName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                drug.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SpecSection(
    title: String,
    rows: List<Pair<String, String>>,
    plain: Boolean = false,
    drugA: Drug? = null,
    drugB: Drug? = null
) {
    val plainNote = if (plain) sectionPlainNote(title) else null
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
                    title,
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

/** Section 标题对应的通俗解释 */
private fun sectionPlainNote(title: String): String? = when (title) {
    "基本信息" -> "药物的中文/英文通用名、商品名、世界卫生组织分类编号 (ATC)"
    "药代动力学" -> "药物在体内怎么被吸收、分布、代谢、排泄。F=生物利用度,t½=半衰期 (血药浓度降到一半的时间)"
    "治疗窗" -> "血药浓度低 = 药效不足, 高 = 中毒。抽血化验应落在这个区间内"
    "CYP 角色" -> "CYP = 肝脏里拆解药物的酶。底物=被拆的, 抑制剂=会拖慢别的药, 诱导剂=会加快别的药 (让别的药失效)"
    "关键不良反应" -> "服药后可能出现的副作用。极低=基本不用担心, 极高=必须监测"
    "剂量调整" -> "肾/肝/老年人是否需要减量。eGFR < 30 是肾衰警示线"
    "监测要求" -> "服药期间要做的检查 (血常规/肝功/心电图 等), 频次根据风险定"
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
                    "互相影响 (对方在 criticalInteractions 中引用了我)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plain) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "💡 A → B 表示 A 药会让 B 药浓度变化; 数字越大变化越剧烈 (1.5 倍=轻, 5 倍以上=严重)",
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
                title = "${drugA.genericNameZh} → ${drugB.genericNameZh}",
                mechanism = hint.mechanism.name.replace("_", " "),
                severity = hint.severity,
                note = hint.clinicalNote,
                auc = hint.aucFoldChange,
                plain = plain
            )
        }
        // drugB → drugA
        intersectB.forEach { hint ->
            InteractionCard(
                title = "${drugB.genericNameZh} → ${drugA.genericNameZh}",
                mechanism = hint.mechanism.name.replace("_", " "),
                severity = hint.severity,
                note = hint.clinicalNote,
                auc = hint.aucFoldChange,
                plain = plain
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
    auc: Pair<Double, Double>,
    plain: Boolean = false
) {
    val sevColor = when (severity) {
        "CONTRAINDICATED" -> androidx.compose.ui.graphics.Color(0xFFB71C1C)
        "HIGH" -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
        "MEDIUM" -> androidx.compose.ui.graphics.Color(0xFFEF6C00)
        "LOW" -> androidx.compose.ui.graphics.Color(0xFFFBC02D)
        else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    }
    Surface(
        color = sevColor.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, sevColor.copy(alpha = 0.4f))
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
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$mechanism · AUC 变化 ${auc.first} - ${auc.second} 倍",
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

private fun buildComparisonSections(
    drugA: Drug,
    drugB: Drug
): List<Pair<String, List<Pair<String, String>>>> {
    val pkA = drugA.pkModel as? PkModel.OneCompartmentWithAbsorption
    val pkB = drugB.pkModel as? PkModel.OneCompartmentWithAbsorption

    val wA = drugA.therapeuticWindow
    val wB = drugB.therapeuticWindow
    val windowStr = when {
        wA != null && wB != null -> "${wA.low}-${wA.high} ${wA.unit} (${wA.guidelineSource ?: ""})" to
            "${wB.low}-${wB.high} ${wB.unit} (${wB.guidelineSource ?: ""})"
        wA != null -> "${wA.low}-${wA.high} ${wA.unit} (${wA.guidelineSource ?: ""})" to "—"
        wB != null -> "—" to "${wB.low}-${wB.high} ${wB.unit} (${wB.guidelineSource ?: ""})"
        else -> "—" to "—"
    }

    return listOf(
        "基本信息" to listOf(
            drugA.genericNameZh to drugB.genericNameZh,
            drugA.genericName to drugB.genericName,
            (drugA.atc ?: "—") to (drugB.atc ?: "—"),
            drugA.category.displayName to drugB.category.displayName,
            (drugA.subcategory ?: "—") to (drugB.subcategory ?: "—"),
        ),
        "药代动力学" to listOf(
            "${pkA?.tHalfHours?.let { "%.1f".format(it) } ?: "—"} h" to
                "${pkB?.tHalfHours?.let { "%.1f".format(it) } ?: "—"} h",
            "F ${pkA?.f ?: "—"}" to "F ${pkB?.f ?: "—"}",
            "ka ${pkA?.kaPerHour ?: "—"}" to "ka ${pkB?.kaPerHour ?: "—"}",
            "ke ${pkA?.kePerHour ?: "—"}" to "ke ${pkB?.kePerHour ?: "—"}",
            "Vd ${pkA?.vdLiters?.toInt() ?: "—"} L" to "Vd ${pkB?.vdLiters?.toInt() ?: "—"} L",
            "蛋白结合 ${drugA.proteinBindingPct}%" to "蛋白结合 ${drugB.proteinBindingPct}%",
        ),
        "治疗窗" to listOf(windowStr),
        "CYP 角色" to listOf(
            fmtCypSubs(drugA) to fmtCypSubs(drugB),
            fmtCypInh(drugA) to fmtCypInh(drugB),
            fmtCypInd(drugA) to fmtCypInd(drugB),
            "代谢途径: ${fmtPathway(drugA)}" to "代谢途径: ${fmtPathway(drugB)}"
        ),
        "关键不良反应" to listOf(
            "QTc 延长: ${riskZh(drugA.adverseEffects.qtcProlongation)}" to
                "QTc 延长: ${riskZh(drugB.adverseEffects.qtcProlongation)}",
            "代谢综合征: ${riskZh(drugA.adverseEffects.metabolicSyndrome)}" to
                "代谢综合征: ${riskZh(drugB.adverseEffects.metabolicSyndrome)}",
            "粒细胞缺乏: ${riskZh(drugA.adverseEffects.agranulocytosis)}" to
                "粒细胞缺乏: ${riskZh(drugB.adverseEffects.agranulocytosis)}",
            "EPS: ${riskZh(drugA.adverseEffects.extrapyramidal)}" to
                "EPS: ${riskZh(drugB.adverseEffects.extrapyramidal)}",
            "镇静: ${riskZh(drugA.adverseEffects.sedation)}" to
                "镇静: ${riskZh(drugB.adverseEffects.sedation)}",
            "性功能: ${riskZh(drugA.adverseEffects.sexual)}" to
                "性功能: ${riskZh(drugB.adverseEffects.sexual)}",
            "高泌乳素: ${riskZh(drugA.adverseEffects.hyperprolactinemia)}" to
                "高泌乳素: ${riskZh(drugB.adverseEffects.hyperprolactinemia)}",
            "抗胆碱能: ${drugA.adverseEffects.anticholinergicLoad} / 3" to
                "抗胆碱能: ${drugB.adverseEffects.anticholinergicLoad} / 3"
        ),
        "剂量调整" to listOf(
            "肾: ${drugA.adjustments.renal.name.replace("_", " ")}" to
                "肾: ${drugB.adjustments.renal.name.replace("_", " ")}",
            "肝: ${drugA.adjustments.hepatic.name.replace("_", " ")}" to
                "肝: ${drugB.adjustments.hepatic.name.replace("_", " ")}",
            (drugA.adjustments.elderly?.let { "老年: $it" } ?: "老年: —") to
                (drugB.adjustments.elderly?.let { "老年: $it" } ?: "老年: —")
        ),
        "监测" to listOf(
            (drugA.monitoring.frequency?.let { "频次: $it" } ?: "频次: —") to
                (drugB.monitoring.frequency?.let { "频次: $it" } ?: "频次: —"),
            ("项目: " + drugA.monitoring.items.joinToString("、").ifEmpty { "—" }) to
                ("项目: " + drugB.monitoring.items.joinToString("、").ifEmpty { "—" })
        ),
        "药物过量" to listOf(
            fmtOverdoseShort(drugA) to fmtOverdoseShort(drugB)
        )
    )
}

private fun fmtOverdoseShort(d: Drug): String {
    val o = d.overdose ?: return "未录入"
    val toxic = o.toxicDoseEstimateMg?.toInt()?.let { "中毒 ≥$it mg" } ?: ""
    val fatal = o.fatalDoseEstimateMg?.toInt()?.let { "致死 ≥$it mg" } ?: ""
    val doses = listOf(toxic, fatal).filter { it.isNotEmpty() }.joinToString(" / ")
    val sev = o.severity.displayName
    val antid = o.antidote?.let { " · 解毒剂:$it" } ?: ""
    return if (doses.isEmpty()) "严重度: $sev$antid" else "严重度: $sev · $doses$antid"
}

private fun fmtCypSubs(d: Drug): String = d.cypProfile.substrates
    .joinToString("、") { "${it.cyp.displayName} ${(it.fraction * 100).toInt()}%" }
    .ifEmpty { "—" }

private fun fmtCypInh(d: Drug): String = d.cypProfile.inhibitors
    .joinToString("、") { "${it.cyp.displayName} ${strengthZh(it.strength.name)}" }
    .ifEmpty { "—" }

private fun fmtCypInd(d: Drug): String = d.cypProfile.inducers
    .joinToString("、") { "${it.cyp.displayName} ${strengthZh(it.strength.name)}" }
    .ifEmpty { "—" }

private fun fmtPathway(d: Drug): String {
    val cp = d.cypProfile
    val pt = cp.pathwayType?.displayName ?: "未标"
    val pp = cp.primaryPathway ?: "—"
    return "$pt · $pp"
}

private fun strengthZh(name: String): String = when (name) {
    "STRONG" -> "强"
    "MODERATE" -> "中"
    "WEAK" -> "弱"
    else -> name
}

private fun riskZh(level: RiskLevel): String = when (level) {
    RiskLevel.VERY_HIGH -> "极高"
    RiskLevel.HIGH -> "高"
    RiskLevel.MEDIUM -> "中"
    RiskLevel.LOW -> "低"
    RiskLevel.VERY_LOW -> "极低"
}
