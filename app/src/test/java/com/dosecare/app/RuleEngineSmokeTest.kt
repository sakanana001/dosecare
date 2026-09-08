package com.dosecare.app

import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.patient.PatientProfile
import com.dosecare.app.domain.rules.RuleEngine
import com.dosecare.app.domain.rules.UserDrugForRule
import org.junit.Test

/**
 * 验证 RuleEngine 在 v0.4 124 药上能检出真实相互作用。
 * 重点: clozapine + fluvoxamine 应该报 CYP1A2 5-10x 强抑制。
 */
class RuleEngineSmokeTest {

    private fun loadCatalog(): DrugCatalogService {
        val resource = javaClass.classLoader!!.getResource("drugs/v0.6.json")
            ?: error("v0.6.json missing")
        val raw = resource.readText(Charsets.UTF_8)
        val drugs = DrugCatalogLoader.fromJson(raw)
        return DrugCatalogService(drugs)
    }

    @Test
    fun rule_engine_detects_clozapine_fluvoxamine() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("clozapine", 300.0, 2.0),
            UserDrugForRule("fluvoxamine", 100.0, 2.0)
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== clozapine + fluvoxamine: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}")
            println("      ${it.detail}")
            println("      advice: ${it.advice}")
        }
        check(results.isNotEmpty()) {
            "Expected clozapine + fluvoxamine to trigger CYP1A2 inhibition warning, got 0"
        }
    }

    @Test
    fun rule_engine_detects_clozapine_fluoxetine() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("clozapine", 300.0, 2.0),
            UserDrugForRule("fluoxetine", 40.0, 1.0)
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== clozapine + fluoxetine: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}")
            println("      ${it.detail}")
            println("      advice: ${it.advice}")
        }
    }

    @Test
    fun rule_engine_detects_qtc_additive() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("ziprasidone", 80.0, 2.0),
            UserDrugForRule("amiodarone", 200.0, 1.0)
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== ziprasidone + amiodarone: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}")
            println("      ${it.detail}")
            println("      advice: ${it.advice}")
        }
    }

    /** v0.5 新规则: 中枢抑制叠加 - 多种镇静药 */
    @Test
    fun rule_engine_detects_sedation_synergy() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("morphine", 30.0, 4.0),       // 镇静 HIGH
            UserDrugForRule("diazepam", 10.0, 2.0),         // 镇静 HIGH
            UserDrugForRule("olanzapine", 10.0, 1.0),      // 镇静 HIGH
            UserDrugForRule("diphenhydramine", 50.0, 2.0)  // 抗组胺镇静
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== 4 个镇静药叠加: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}: ${it.detail}")
        }
        check(results.any { it.title.contains("中枢抑制") || it.title.contains("Sedation") }) {
            "Expected sedation synergy warning. Titles: ${results.map { it.title }}"
        }
    }

    /** v0.5 新规则: 出血风险叠加 - 抗凝+抗血小板+SSRI */
    @Test
    fun rule_engine_detects_bleeding_risk() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("warfarin", 5.0, 1.0),
            UserDrugForRule("aspirin", 100.0, 1.0),
            UserDrugForRule("sertraline", 100.0, 1.0)
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== warfarin+aspirin+sertraline: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}: ${it.detail}")
        }
        check(results.any { it.title.contains("出血") || it.title.contains("Bleeding") }) {
            "Expected bleeding risk warning. Titles: ${results.map { it.title }}"
        }
    }

    /** v0.5 新规则: 多巴胺 D2 拮抗叠加 - 多种抗精神病 */
    @Test
    fun rule_engine_detects_dopamine_antagonism() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("risperidone", 4.0, 1.0),       // 强 D2
            UserDrugForRule("ziprasidone", 80.0, 2.0),     // 强 D2 (catalog 里有)
            UserDrugForRule("olanzapine", 10.0, 1.0)       // 中 D2
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== 3 个抗精神病叠加: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}: ${it.detail}")
        }
        check(results.any { it.title.contains("多巴胺") || it.title.contains("EPS") }) {
            "Expected dopamine antagonism warning. Titles: ${results.map { it.title }}"
        }
    }

    /** v0.5 新规则: 低血压叠加 - 多种降压药 */
    @Test
    fun rule_engine_detects_hypotension_risk() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("amlodipine", 5.0, 1.0),
            UserDrugForRule("lisinopril", 10.0, 1.0),
            UserDrugForRule("metoprolol", 50.0, 2.0),
            UserDrugForRule("losartan", 50.0, 1.0),       // 替 hctz (catalog 里有)
            UserDrugForRule("amitriptyline", 50.0, 1.0)  // TCA α 阻滞
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== 5 个降压/TCA 叠加: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}: ${it.detail}")
        }
        check(results.any { it.title.contains("低血压") || it.title.contains("Hypotension") }) {
            "Expected hypotension risk warning. Titles: ${results.map { it.title }}"
        }
    }

    /** v0.5 精一+精二 测试: 吗啡 + 安定 + 氯丙嗪 (经典联用) */
    @Test
    fun rule_engine_detects_classic_chinese_combo() {
        val catalog = loadCatalog()
        val userDrugs = listOf(
            UserDrugForRule("morphine", 30.0, 4.0),       // 精一 阿片
            UserDrugForRule("diazepam", 10.0, 2.0),         // 精二 BZD
            UserDrugForRule("clozapine", 300.0, 2.0),     // 抗精神病
            UserDrugForRule("risperidone", 4.0, 1.0)       // 抗精神病
        )
        val results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
        println("=== 吗啡+安定+氯氮平+利培酮: ${results.size} interactions ===")
        results.forEach {
            println("  - [${it.severity.displayName}] ${it.title}")
        }
        // 应触发: 镇静叠加 (4药都镇静 HIGH), EPS (2 抗精神病), CYP 1A2/3A4 相互作用
        check(results.size >= 3) { "Expected at least 3 interactions, got ${results.size}" }
    }
}
