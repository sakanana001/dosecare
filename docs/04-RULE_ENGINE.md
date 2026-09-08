# 04 · 规则引擎

## 目标

给定当前用药列表 + 患者画像，输出**按严重度排序的相互作用清单**。

```
Input:  List<UserDrug> + PatientProfile
Output: List<Interaction>
        排序：CONTRAINDICATED > HIGH > MEDIUM > LOW > INFO
```

## 严重度分级

| 等级 | 颜色 | 触发 | UI 表现 |
|------|------|------|---------|
| **CONTRAINDICATED** | 红 | 致死/不可逆风险；任何情况下都不应联用 | 弹窗 + 强制阅读 + 振动提醒 |
| **HIGH** | 橙 | 显著增加不良反应；需要医师立即评估 | 列表顶部 + 推送通知 |
| **MEDIUM** | 黄 | 临床重要；可能需要剂量调整 | 折叠区，默认隐藏详情 |
| **LOW** | 灰 | 理论可能性；多数患者无碍 | 仅"信息"区可见 |
| **INFO** | 灰白 | 监测建议、相互作用机制教学 | "了解更多"链接 |

## 6 类规则

### 1. CYP 代谢相互作用

**核心**——精神科最致命的一类。

#### 数据结构

```kotlin
// 在 DrugCatalog.cypProfile 中：
data class CypProfile(
    val substrates: List<CypContribution>,  // [{cyp, fraction}]
    val inhibitors: List<CypEffect>,        // [{cyp, strength}]
    val inducers: List<CypEffect>           // [{cyp, strength}]
)

enum class CypEnzyme(val displayName: String, val displayNameZh: String) {
    CYP1A2("CYP1A2", "CYP1A2"),
    CYP2D6("CYP2D6", "CYP2D6"),
    CYP3A4("CYP3A4", "CYP3A4"),
    CYP2C9("CYP2C9", "CYP2C9"),
    CYP2C19("CYP2C19", "CYP2C19"),
    CYP2E1("CYP2E1", "CYP2E1"),
    P_GP("P-gp", "P-糖蛋白")
}

enum class EffectStrength { WEAK, MODERATE, STRONG }
```

#### 配对逻辑

```kotlin
fun checkCyp(
    drugs: List<Drug>, 
    profile: PatientProfile
): List<Interaction> {
    val results = mutableListOf<Interaction>()
    
    for (i in drugs.indices) for (j in drugs.indices) {
        if (i == j) continue
        val perpetrator = drugs[i]  // 触发药
        val victim = drugs[j]        // 受影响药
        
        // perpetrator 抑制 victim 的代谢酶
        for (inh in perpetrator.cypProfile.inhibitors) {
            val victimSubstrate = victim.cypProfile.substrates
                .find { it.cyp == inh.cyp }
            if (victimSubstrate != null) {
                val aucFold = estimateAucFold(inh, victimSubstrate)
                val severity = classifyAuc(aucFold)
                results.add(CypInteraction(
                    perpetrator, victim, inh.cyp,
                    Mechanism.INHIBITION, aucFold, severity
                ))
            }
        }
        
        // 诱导同上
        for (ind in perpetrator.cypProfile.inducers) {
            val victimSubstrate = victim.cypProfile.substrates
                .find { it.cyp == ind.cyp }
            if (victimSubstrate != null) {
                val aucFold = estimateAucFoldInd(ind, victimSubstrate)
                val severity = classifyAucInverse(aucFold)
                results.add(CypInteraction(
                    perpetrator, victim, ind.cyp,
                    Mechanism.INDUCTION, aucFold, severity
                ))
            }
        }
    }
    
    return results
}
```

#### 强度判断（FDA 指南）

| 强度 | AUC 倍数 | 临床意义 |
|------|---------|---------|
| STRONG | ≥ 5 倍 | 严重，需要换药或大幅减量 |
| MODERATE | 2-5 倍 | 显著，需要剂量调整 |
| WEAK | 1.25-2 倍 | 轻微，监测即可 |

#### 患者调整系数

```kotlin
fun adjustAucForPatient(
    baseFold: Double, 
    patient: PatientProfile
): Double {
    var fold = baseFold
    if (patient.egfr != null && patient.egfr < 30) fold *= 1.2  // 肾损伤加重蓄积
    if (patient.childPugh == "B") fold *= 1.5
    if (patient.childPugh == "C") fold *= 2.0
    return fold
}
```

### 2. QTc 累积风险

精神科多种药物延长 QTc：
- 抗精神病：齐拉西酮、喹硫平、氟哌啶醇 IV、氯丙嗪
- 抗抑郁：西酞普兰、艾司西酞普兰（FDA 警告 > 40mg）
- 抗生素、心律失常药（精神科共病）

#### 评分

```kotlin
data class QtcRiskProfile(
    val drugId: String,
    val qtcEffectMs: Double  // 平均 QTc 延长（ms）
)

val qtcProfiles = mapOf(
    "ziprasidone" to QtcRisk("ziprasidone", 20.3),
    "haloperidol_iv" to QtcRisk("haloperidol_iv", 14.7),
    "citalopram_high_dose" to QtcRisk("citalopram_high_dose", 18.0),
    // ...
)
```

```kotlin
fun checkQtc(drugs: List<Drug>, profile: PatientProfile): QtcInteraction? {
    var totalQtc = 0.0
    val contributors = mutableListOf<Drug>()
    for (drug in drugs) {
        val effect = qtcProfiles[drug.id] ?: continue
        // 剂量调整：低剂量按比例缩放
        val adjusted = effect.qtcEffectMs * (drug.doseMg / drug.maxDoseForEffect)
        totalQtc += adjusted
        contributors.add(drug)
    }
    
    val baseQtc = 400.0
    val finalQtc = baseQtc + totalQtc
    val severity = when {
        finalQtc > 500 -> Severity.CONTRAINDICATED
        finalQtc > 480 -> Severity.HIGH
        finalQtc > 460 -> Severity.MEDIUM
        finalQtc > 440 -> Severity.LOW
        else -> Severity.INFO
    }
    
    // 风险因素加权
    val femaleBonus = if (profile.biologicalSex == "F") 10.0 else 0.0
    val electrolyteBonus = if (profile.hasElectrolyteDisorder) 15.0 else 0.0
    val bradycardiaBonus = if (profile.hasBradycardia) 10.0 else 0.0
    
    finalQtc + femaleBonus + electrolyteBonus + bradycardiaBonus
    
    return QtcInteraction(severity, contributors, finalQtc)
}
```

### 3. 抗胆碱能负荷

老年精神科患者高风险。多个药叠加 → 谵妄、跌倒、尿潴留、认知下降。

#### 评分表（基于 ACB scale / 韩国版）

```kotlin
val anticholinergicScores = mapOf(
    // 精神科
    "clozapine" to 3,
    "olanzapine" to 2,
    "quetiapine" to 2,
    "chlorpromazine" to 3,
    "amitriptyline" to 3,
    "paroxetine" to 2,
    "benztropine" to 3,
    // 老年共病
    "oxybutynin" to 3,
    "diphenhydramine" to 3,
    "chlorpheniramine" to 3,
    // ...
)

fun checkAnticholinergic(drugs: List<Drug>): AnticholinergicInteraction? {
    val total = drugs.sumOf { anticholinergicScores[it.id] ?: 0 }
    val severity = when {
        total >= 6 -> Severity.HIGH
        total >= 4 -> Severity.MEDIUM
        total >= 2 -> Severity.LOW
        else -> null  // 不到 2 分不报
    }
    return severity?.let { AnticholinergicInteraction(it, drugs, total) }
}
```

### 4. 5-HT 综合征风险

多种增加 5-HT 的药联用 → 5-HT 综合征（可致死）。

#### 5-HT 机制标签

```kotlin
enum class SerotonergicMechanism {
    SSRI,
    SNRI,
    MAOI_REVERSIBLE,
    MAOI_IRREVERSIBLE,
    TRYPTOPHAN_REUPTAKE_INHIBITION,
    RECEPTOR_AGONIST,
    RELEASE_STIMULANT,
    METABOLIC_INHIBITOR
}
```

#### 危险组合表

```kotlin
val serotoninDanger = listOf(
    // MAOI + 任何 serotonergic → 必死
    DangerousCombo(
        listOf("phenelzine", "tranylcypromine", "selegiline_high_dose"),
        SerotonergicMechanism.MAOI_IRREVERSIBLE,
        Severity.CONTRAINDICATED,
        "MAOI 14 天内禁用 serotonergic 药"
    ),
    // SSRI + MAOI → 5-HT 综合征
    DangerousCombo(
        listOf("fluoxetine", "sertraline", "paroxetine"),
        SerotonergicMechanism.SSRI,
        Severity.CONTRAINDICATED
    ),
    // SSRI + 曲马多 / 度洛西汀 / 三环类
    DangerousCombo(
        listOf("fluoxetine", "sertraline", "paroxetine"),
        SerotonergicMechanism.SSRI,
        Severity.HIGH
    ),
    // 锂盐 + SSRI → 5-HT 增强
    DangerousCombo(
        listOf("lithium"),
        SerotonergicMechanism.OTHER,
        Severity.MEDIUM
    )
)
```

### 5. 治疗窗偏离

PK 引擎算出当前浓度 vs 治疗窗，超窗报警。

```kotlin
fun checkTherapeuticWindow(
    curves: List<PkCurve>,
    drugs: List<Drug>
): List<Interaction> {
    return curves.zip(drugs).mapNotNull { (curve, drug) ->
        val window = drug.therapeuticWindow ?: return@mapNotNull null
        if (curve.cAvg < window.low) {
            TherapeuticWindowInteraction(
                drug, curve.cAvg, window.low, window.high,
                Status.BELOW, severity = Severity.MEDIUM
            )
        } else if (curve.cMax > window.high) {
            TherapeuticWindowInteraction(
                drug, curve.cMax, window.low, window.high,
                Status.ABOVE, severity = Severity.HIGH
            )
        } else null
    }
}
```

### 6. 肾/肝剂量调整

```kotlin
fun checkRenalAdjustment(drugs: List<Drug>, patient: PatientProfile): List<Interaction> {
    if (patient.egfr == null) return emptyList()
    return drugs.mapNotNull { drug ->
        val rule = renalAdjustmentTable[drug.id] ?: return@mapNotNull null
        when {
            patient.egfr < rule.contraindicated && rule.contraindicated > 0 ->
                Interaction(Severity.CONTRAINDICATED, "eGFR 过低禁用")
            patient.egfr < rule.reduce50 ->
                Interaction(Severity.HIGH, "eGFR 较低，建议剂量减半")
            patient.egfr < rule.monitor ->
                Interaction(Severity.MEDIUM, "eGFR 下降，监测肾功")
            else -> null
        }
    }
}

// 例子
val renalAdjustmentTable = mapOf(
    "lithium" to RenalRule(contraindicated = 30.0, reduce50 = 50.0, monitor = 60.0),
    "valproic_acid" to RenalRule(contraindicated = 0.0, reduce50 = 30.0, monitor = 60.0),
    "gabapentin" to RenalRule(contraindicated = 0.0, reduce50 = 30.0, monitor = 60.0),
    "metformin" to RenalRule(contraindicated = 30.0, reduce50 = 45.0, monitor = 60.0)
)
```

## 引擎实现

```kotlin
class RuleEngine(
    private val catalog: DrugCatalog,
    private val pkEngine: PkEngine
) {
    fun evaluate(
        userDrugs: List<UserDrug>,
        patient: PatientProfile,
        timeRange: ClosedFloatingPointRange<Double>
    ): List<Interaction> {
        val drugs = userDrugs.map { catalog.getById(it.drugCatalogId) }
        
        val curves = drugs.map { drug ->
            pkEngine.multiDoseCurve(
                drug, userDrugs.find { it.drugCatalogId == drug.id }!!, 
                timeRange
            )
        }
        
        val results = mutableListOf<Interaction>()
        results += checkCypInteractions(drugs, patient)
        results += checkQtcAdditive(drugs, patient)
        results += checkAnticholinergicLoad(drugs)
        results += checkSerotoninSyndrome(drugs)
        results += checkTherapeuticWindow(curves, drugs)
        results += checkRenalAdjustment(drugs, patient)
        results += checkHepaticAdjustment(drugs, patient)
        
        return results
            .distinctBy { it.signature }
            .sortedByDescending { it.severity.rank }
    }
}
```

## 数据源

每条规则的强度系数来自：

- **FDA Drug Interaction Table**（公开）
- **Flockhart CYP 相互作用表**（Indiana 大学公开 Excel）
- **Lexicomp / Micromedex**（商用，机构订阅）
- **AGNP 精神科共识**（公开）
- **UpToDate 表格**（机构订阅）
- **药品说明书**（国内 + FDA 标签）
- **本地药学专家 review**（v1.0 之后，**强烈建议**找一位临床药师做最终审核）

## 持续维护

- DrugCatalog 每次更新要**带 version + changelog**
- 规则表更新记录到 `CHANGELOG_RULES.md`
- 任何严重度变更需要**附文献引用**

## 单元测试

每条规则写 snapshot test：

```kotlin
@Test
fun `fluvoxamine + clozapine should be contraindicated`() {
    val interaction = checkCyp(
        drugs = listOf(
            catalog.getById("fluvoxamine"), 
            catalog.getById("clozapine")
        ),
        profile = PatientProfile(...)
    )
    assertThat(interaction).anyMatch {
        it.severity == Severity.CONTRAINDICATED &&
        (it as CypInteraction).aucFold > 5.0
    }
}
```

至少要 100 条规则测试覆盖。
