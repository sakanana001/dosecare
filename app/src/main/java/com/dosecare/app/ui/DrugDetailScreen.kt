package com.dosecare.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.catalog.OverdoseSeverity
import com.dosecare.app.domain.catalog.RiskLevel
import com.dosecare.app.domain.pk.DoseEvent
import com.dosecare.app.domain.pk.PkEngine
import com.dosecare.app.domain.pk.PkModel

/**
 * 药物详情屏 (v0.2.2 → v0.5.1 加通俗模式, v0.9c 全 i18n)
 *
 * v0.5.1 升级:
 * - 顶部加"📖 通俗模式" toggle, 默认 OFF (专业人士)
 * - ON 时, 每个学术参数下用小字 + 💡 显示普通用户能懂的描述
 * - 学术 vs 通俗一键切换, 方便患者和家属自己看懂
 *
 * v0.9c i18n:
 * - Section titles (CYP 角色/PK/药理作用/关键不良反应/药物过量/剂量调整/监测要求) → drug_section_*
 * - CYP role rows (作为底物/抑制剂/诱导剂) → cyp_role_*
 * - Risk row labels (QTc 延长/代谢综合征/...) → risk_*
 * - Adj row labels (肾功能/肝功能/老年/吸烟/戒烟) → adj_*
 * - PK meta 格式 (t½ + 蛋白结合) / 抗胆碱能 (X / 3) → drug_pk_meta / drug_anticholinergic_score
 * - strengthZh / RiskLevel.displayName 改 @Composable + cyp_strength_* / risk_level_*
 * - Plain.kt 的 23 个通俗解释函数已改 @Composable (返回 String via stringResource)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugDetailScreen(
    drugId: String,
    catalog: DrugCatalogService,
    onBack: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBack)
    val drug = remember(drugId) { catalog.tryGetById(drugId) }
    var plainMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (drug?.let {
                val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                val isChinesePrimary = locale.language == "zh"
                if (isChinesePrimary) it.genericNameZh else it.genericName
            } ?: stringResource(R.string.drug_not_found, "")),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // 通俗模式 toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (plainMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(R.string.compare_lay_title),
                            tint = if (plainMode) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = plainMode,
                            onCheckedChange = { plainMode = it }
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        if (drug == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.drug_not_found, drugId))
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(drug, plainMode)
            CypRoleCard(drug, plainMode)
            if (drug.therapeuticWindow != null) WindowCard(drug, plainMode)
            PkPreviewCard(drug, plainMode)
            if (drug.pharmacology != null) PharmacologyCard(drug, plainMode)
            AdverseEffectsCard(drug, plainMode)
            if (drug.overdose != null) OverdoseCard(drug, plainMode)
            AdjustmentsCard(drug, plainMode)
            if (drug.criticalInteractions.isNotEmpty()) CriticalInteractionsCard(drug, catalog, plainMode)
            if (drug.monitoring.items.isNotEmpty()) MonitoringCard(drug, plainMode)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 通俗解释小条 */
@Composable
private fun PlainNote(text: String) {
    Row(modifier = Modifier.padding(top = 2.dp, start = 0.dp, end = 0.dp)) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun HeaderCard(drug: Drug, plain: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        val locale2 = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        val isChinesePrimary2 = locale2.language == "zh"
        val drugNamePrimary = if (isChinesePrimary2) drug.genericNameZh else drug.genericName
        val drugNameSecondary = if (isChinesePrimary2) drug.genericName else drug.genericNameZh
        Column(Modifier.padding(20.dp)) {
            Text(drugNamePrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                drugNameSecondary,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (drug.atc != null) {
                    AssistChip(onClick = {}, label = { Text("ATC ${drug.atc}") })
                    Spacer(Modifier.width(8.dp))
                }
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(drug.category.displayNameRes)) }
                )
            }
            if (drug.brandNames.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.drug_brand_names, drug.brandNames.joinToString("、")),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (plain) {
                Spacer(Modifier.height(8.dp))
                PlainNote(stringResource(R.string.plain_atc_note))
            }
        }
    }
}

@Composable
private fun CypRoleCard(drug: Drug, plain: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.drug_section_cyp))
            Spacer(Modifier.height(4.dp))
            val substrateLabels = drug.cypProfile.substrates.map { "${stringResource(it.cyp.displayNameRes)} ${(it.fraction * 100).toInt()}%" }
            CypRow(stringResource(R.string.cyp_role_substrate), substrateLabels)
            if (plain && drug.cypProfile.substrates.isNotEmpty()) PlainNote(Plain.cypSubstrate())
            val inhibitorLabels = drug.cypProfile.inhibitors.map { "${stringResource(it.cyp.displayNameRes)} ${strengthZh(it.strength.name)}" }
            CypRow(stringResource(R.string.cyp_role_inhibitor), inhibitorLabels)
            if (plain && drug.cypProfile.inhibitors.isNotEmpty()) {
                val strongest = drug.cypProfile.inhibitors.maxByOrNull { strengthRank(it.strength.name) }
                if (strongest != null) PlainNote(Plain.cypInhibitor(strongest.strength.name))
            }
            val inducerLabels = drug.cypProfile.inducers.map { "${stringResource(it.cyp.displayNameRes)} ${strengthZh(it.strength.name)}" }
            CypRow(stringResource(R.string.cyp_role_inducer), inducerLabels)
            if (plain && drug.cypProfile.inducers.isNotEmpty()) {
                val strongest = drug.cypProfile.inducers.maxByOrNull { strengthRank(it.strength.name) }
                if (strongest != null) PlainNote(Plain.cypInducer(strongest.strength.name))
            }
            // 主要代谢途径 (即使 CYP 也有结构化描述, 非 CYP 标 Phase II / 肾排 / 等)
            if (drug.cypProfile.primaryPathway != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.drug_metabolism), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (drug.cypProfile.pathwayType != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = pathwayColor(drug.cypProfile.pathwayType).copy(alpha = 0.15f)
                        ) {
                            // TODO(v0.9e done): 改 stringResource(pathway.displayNameRes)
                            Text(
                                stringResource(drug.cypProfile.pathwayType.displayNameRes),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = pathwayColor(drug.cypProfile.pathwayType),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                // TODO(v0.9b): primaryPathway 字段是数据 (database value), 不需要 i18n
                Text(drug.cypProfile.primaryPathway, style = MaterialTheme.typography.bodyMedium)
                if (plain) PlainNote(Plain.metabolism(drug.cypProfile.pathwayType))
            }
        }
    }
}

private fun pathwayColor(pt: com.dosecare.app.domain.catalog.PathwayType): Color = when (pt) {
    com.dosecare.app.domain.catalog.PathwayType.CYP450 -> Color(0xFF1976D2)               // 蓝: 主药代酶
    com.dosecare.app.domain.catalog.PathwayType.UGT_GLUCURONIDATION -> Color(0xFF388E3C)    // 绿: 葡萄糖苷酸化, 通常无显著 CYP 相互作用
    com.dosecare.app.domain.catalog.PathwayType.GLUCURONIDATION -> Color(0xFF388E3C)
    com.dosecare.app.domain.catalog.PathwayType.RENAL_EXCRETION -> Color(0xFF7B1FA2)       // 紫: 肾排泄, 通常无 CYP 相互作用
    com.dosecare.app.domain.catalog.PathwayType.HYDROLYSIS -> Color(0xFF0097A7)          // 青: 水解
    com.dosecare.app.domain.catalog.PathwayType.ESTERASE -> Color(0xFF0097A7)
    com.dosecare.app.domain.catalog.PathwayType.DEIODINATION -> Color(0xFF5D4037)        // 棕: 脱碘
    com.dosecare.app.domain.catalog.PathwayType.MAO -> Color(0xFFD32F2F)                  // 红: 单胺氧化酶
    com.dosecare.app.domain.catalog.PathwayType.DPP4 -> Color(0xFFF57C00)                  // 橙
    com.dosecare.app.domain.catalog.PathwayType.BETA_OXIDATION -> Color(0xFF7B1FA2)
    com.dosecare.app.domain.catalog.PathwayType.OTHER -> Color(0xFF888888)
}

@Composable
private fun CypRow(label: String, items: List<String>) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        if (items.isEmpty()) {
            Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(items.joinToString("、"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun strengthRank(s: String) = when (s) {
    "STRONG" -> 3
    "MODERATE" -> 2
    "WEAK" -> 1
    else -> 0
}

@Composable
private fun strengthZh(name: String): String = when (name) {
    "STRONG" -> stringResource(R.string.cyp_strength_strong)
    "MODERATE" -> stringResource(R.string.cyp_strength_moderate)
    "WEAK" -> stringResource(R.string.cyp_strength_weak)
    else -> name
}

@Composable
private fun WindowCard(drug: Drug, plain: Boolean) {
    val w = drug.therapeuticWindow!!
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.drug_therapeutic_window), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "${"%.0f".format(w.low)} - ${"%.0f".format(w.high)} ${w.unit}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.drug_window_ref, w.guidelineSource ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                if (plain) {
                    // TODO(v0.9b): Plain.kt 解释 (治疗窗 = ...) 需要 i18n
                    PlainNote(Plain.therapeuticWindow(w.low, w.high, w.unit))
                    w.guidelineSource?.let { PlainNote(Plain.guideline(it)) }
                }
            }
        }
    }
}

@Composable
private fun PkPreviewCard(drug: Drug, plain: Boolean) {
    val model = drug.pkModel
    if (model !is PkModel.OneCompartmentWithAbsorption) return
    val css = remember(drug.id) { computeCss(drug) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.drug_section_pk))
            Spacer(Modifier.height(8.dp))
            Text(css, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.drug_pk_meta, model.tHalfHours, drug.proteinBindingPct),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (plain) {
                Spacer(Modifier.height(4.dp))
                PlainNote(Plain.halfLife(model.tHalfHours))
                PlainNote(Plain.proteinBinding(drug.proteinBindingPct))
                // Cmax/Cmin/Cavg 通俗解释
                val parts = css.split("·").map { it.trim() }
                parts.forEach { p ->
                    when {
                        p.startsWith("Cmax=") -> {
                            val n = p.removePrefix("Cmax=").trim()
                            val (v, u) = splitNumUnit(n)
                            if (v != null) PlainNote(Plain.cmax(v, u))
                        }
                        p.startsWith("Cmin=") -> {
                            val n = p.removePrefix("Cmin=").trim()
                            val (v, u) = splitNumUnit(n)
                            if (v != null) PlainNote(Plain.cmin(v, u))
                        }
                        p.startsWith("Cavg=") -> {
                            val n = p.removePrefix("Cavg=").trim()
                            val (v, u) = splitNumUnit(n)
                            if (v != null) PlainNote(Plain.cavg(v, u))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PharmacologyCard(drug: Drug, plain: Boolean) {
    val text = drug.pharmacology ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.drug_section_pharmacology))
            Spacer(Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 解析 "1234 ng/mL" → (1234.0, "ng/mL") */
private fun splitNumUnit(s: String): Pair<Double?, String> {
    val m = Regex("([0-9.]+)\\s*(\\S+)").find(s)
    return if (m != null) {
        val v = m.groupValues[1].toDoubleOrNull()
        val u = m.groupValues[2]
        v to u
    } else null to ""
}

private fun computeCss(drug: Drug): String = runCatching {
    val model = drug.pkModel as PkModel.OneCompartmentWithAbsorption
    val engine = PkEngine()
    val doses = (0 until 10).map { DoseEvent(200.0, it * 12.0) }
    val curve = engine.multiDoseCurve(
        model = model,
        doseEvents = doses,
        tStart = 96.0, tEnd = 120.0, stepHours = 0.25
    )
    val factor = drug.cMaxUnitFactor
    val unit = drug.therapeuticWindow?.unit?.let { it.substringBefore(" ") } ?: "mg/L"
    "Cmax=${"%.1f".format(curve.cMax * factor)} · " +
        "Cmin=${"%.1f".format(curve.cMin * factor)} · " +
        "Cavg=${"%.1f".format(curve.cAvg * factor)} $unit"
}.getOrDefault("—")

@Composable
private fun AdverseEffectsCard(drug: Drug, plain: Boolean) {
    val ae = drug.adverseEffects
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.drug_section_adverse))
            Spacer(Modifier.height(8.dp))
            RiskRow(stringResource(R.string.risk_qtc), ae.qtcProlongation); if (plain) PlainNote(Plain.qtcProlongation(ae.qtcProlongation))
            RiskRow(stringResource(R.string.risk_metabolic), ae.metabolicSyndrome); if (plain) PlainNote(Plain.metabolicSyndrome(ae.metabolicSyndrome))
            RiskRow(stringResource(R.string.risk_agranulocytosis), ae.agranulocytosis); if (plain) PlainNote(Plain.agranulocytosis(ae.agranulocytosis))
            RiskRow(stringResource(R.string.risk_eps), ae.extrapyramidal); if (plain) PlainNote(Plain.extrapyramidal(ae.extrapyramidal))
            RiskRow(stringResource(R.string.risk_sedation), ae.sedation); if (plain) PlainNote(Plain.sedation(ae.sedation))
            RiskRow(stringResource(R.string.risk_sexual), ae.sexual); if (plain) PlainNote(Plain.sexual(ae.sexual))
            RiskRow(stringResource(R.string.risk_prolactin), ae.hyperprolactinemia); if (plain) PlainNote(Plain.hyperprolactinemia(ae.hyperprolactinemia))
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.drug_anticholinergic), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.drug_anticholinergic_score, ae.anticholinergicLoad),
                    style = MaterialTheme.typography.bodyMedium,
                    color = anticholinergicColor(ae.anticholinergicLoad)
                )
            }
            if (plain) PlainNote(Plain.anticholinergicLoad(ae.anticholinergicLoad))
        }
    }
}

@Composable
private fun RiskRow(label: String, level: RiskLevel) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = riskColor(level).copy(alpha = 0.18f)
        ) {
            Text(
                level.displayName(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = riskColor(level),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun OverdoseCard(drug: Drug, plain: Boolean) {
    val o = drug.overdose ?: return
    val sevColor = overdoseColor(o.severity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = sevColor.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = sevColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                SectionTitle(stringResource(R.string.drug_section_overdose))
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = sevColor.copy(alpha = 0.18f)
                ) {
                    // TODO(v0.9e done): 改 stringResource(o.severity.displayNameRes)
                    Text(
                        stringResource(o.severity.displayNameRes),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = sevColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.drug_typical_symptoms), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(o.symptoms, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            // 剂量估计
            if (o.toxicDoseEstimateMg != null || o.fatalDoseEstimateMg != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (o.toxicDoseEstimateMg != null) {
                        Text(
                            stringResource(R.string.drug_toxic_dose, o.toxicDoseEstimateMg.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFB8C00)
                        )
                    }
                    if (o.fatalDoseEstimateMg != null) {
                        if (o.toxicDoseEstimateMg != null) Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.drug_lethal_dose, o.fatalDoseEstimateMg.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(stringResource(R.string.drug_rescue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(o.management, style = MaterialTheme.typography.bodySmall)
            o.antidote?.let { ant ->
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.12f)
                ) {
                    Text(
                        stringResource(R.string.drug_antidote, ant),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.drug_data_source, o.dataSource), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun overdoseColor(sev: OverdoseSeverity): Color = when (sev) {
    OverdoseSeverity.LIFE_THREATENING -> Color(0xFFB71C1C)
    OverdoseSeverity.SEVERE -> Color(0xFFD32F2F)
    OverdoseSeverity.MODERATE -> Color(0xFFFB8C00)
    OverdoseSeverity.MILD -> Color(0xFF66BB6A)
}

private fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.VERY_HIGH -> Color(0xFFB71C1C)
    RiskLevel.HIGH -> Color(0xFFE53935)
    RiskLevel.MEDIUM -> Color(0xFFFB8C00)
    RiskLevel.LOW -> Color(0xFF66BB6A)
    RiskLevel.VERY_LOW -> Color(0xFF888888)
}

private fun anticholinergicColor(score: Int): Color = when {
    score >= 3 -> Color(0xFFB71C1C)
    score >= 2 -> Color(0xFFFB8C00)
    score >= 1 -> Color(0xFFFFA726)
    else -> Color(0xFF66BB6A)
}

// TODO(v0.9c done): RiskLevel.displayName() @Composable → i18n via R.string.risk_level_*
@Composable
private fun RiskLevel.displayName(): String = when (this) {
    RiskLevel.VERY_HIGH -> stringResource(R.string.risk_level_very_high)
    RiskLevel.HIGH -> stringResource(R.string.risk_level_high)
    RiskLevel.MEDIUM -> stringResource(R.string.risk_level_medium)
    RiskLevel.LOW -> stringResource(R.string.risk_level_low)
    RiskLevel.VERY_LOW -> stringResource(R.string.risk_level_very_low)
}

@Composable
private fun AdjustmentsCard(drug: Drug, plain: Boolean) {
    val a = drug.adjustments
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.drug_section_adjust))
            Spacer(Modifier.height(8.dp))
            KvRow(stringResource(R.string.adj_renal), a.renal.name.replace("_", " "))
            if (plain) PlainNote(Plain.renalAdj(a.renal))
            KvRow(stringResource(R.string.adj_hepatic), a.hepatic.name.replace("_", " "))
            if (plain) PlainNote(Plain.hepaticAdj(a.hepatic))
            a.elderly?.let {
                KvRow(stringResource(R.string.adj_elderly), it)
                if (plain) PlainNote(Plain.elderly())
            }
            a.smoking?.let {
                KvRow(stringResource(R.string.adj_smoking), "${it.effect}（${it.doseAdjustment}）")
                it.abstinenceNote?.let { n -> KvRow(stringResource(R.string.adj_quit_smoking), n) }
            }
        }
    }
}

@Composable
private fun KvRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CriticalInteractionsCard(drug: Drug, catalog: DrugCatalogService, plain: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.drug_critical_interactions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            drug.criticalInteractions.forEach { hint ->
                val trigger = catalog.tryGetById(hint.triggerDrugId)
                if (trigger != null) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(
                            "${trigger.genericNameZh} (${trigger.genericName})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${hint.mechanism.name.replace("_", " ")} · ${hint.severity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            stringResource(R.string.drug_auc_change, hint.aucFoldChange.first.toString(), hint.aucFoldChange.second.toString()),
                            style = MaterialTheme.typography.bodySmall
                        )
                        // TODO(v0.9c done): Plain.auc() @Composable → i18n; auc_extra 补 "倍数 = 联用时浓度变化倍数" 后缀
                        if (plain) PlainNote(Plain.auc() + stringResource(R.string.auc_extra))
                        Spacer(Modifier.height(2.dp))
                        Text(hint.clinicalNote, style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun MonitoringCard(drug: Drug, plain: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(stringResource(R.string.drug_section_monitor))
            Spacer(Modifier.height(4.dp))
            drug.monitoring.frequency?.let {
                Text(stringResource(R.string.drug_freq, it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                drug.monitoring.items.joinToString("、"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}
