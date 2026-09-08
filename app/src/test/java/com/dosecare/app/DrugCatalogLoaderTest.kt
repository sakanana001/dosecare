package com.dosecare.app

import com.dosecare.app.domain.catalog.DrugCatalogLoader
import org.junit.Test

/**
 * 真实加载 test resources 里的 v0.4.json (120 药),验证 DrugCatalogLoader.fromJson 能不能解析。
 * 如果 v0.4.json 里有任何 enum 值在 Kotlin 这边不存在,这里就会失败并打印具体异常。
 *
 * 运行:
 *   gradle :app:testDebugUnitTest --tests DrugCatalogLoaderTest
 */
class DrugCatalogLoaderTest {

    @Test
    fun v0_6_catalog_loads_with_159_drugs() {
        val resource = javaClass.classLoader!!.getResource("drugs/v0.6.json")
            ?: error("v0.6.json 不在 test resources 里")
        val raw = resource.readText(Charsets.UTF_8)

        println("=== v0.6.json size: ${raw.length} chars ===")

        val drugs = try {
            DrugCatalogLoader.fromJson(raw)
        } catch (e: Exception) {
            println("=== LOAD FAILED ===")
            println("Exception: ${e::class.qualifiedName}")
            println("Message: ${e.message}")
            var cause = e.cause
            while (cause != null) {
                println("Caused by: ${cause::class.qualifiedName}: ${cause.message}")
                cause = cause.cause
            }
            e.printStackTrace()
            throw e
        }

        println("=== LOADED ${drugs.size} drugs ===")
        val byCat = drugs.groupBy { it.category }
        byCat.forEach { (cat, list) ->
            println("  [${cat}] ${list.size}: ${list.take(5).joinToString { it.genericName }}")
        }

        check(drugs.size >= 159) { "Expected >= 159 drugs, got ${drugs.size}" }

        // 验证 v0.6 新增药
        val ids = drugs.map { it.id }.toSet()
        val required = listOf(
            // v0.6 缓释版
            "bupropion_xl", "venlafaxine_xr", "paroxetine_cr", "fluoxetine_weekly",
            // v0.6 异构体
            "arformoterol", "levalbuterol", "dextroamphetamine", "levetiracetam",
            // v0.6 活性代谢物
            "desvenlafaxine", "norketamine", "morphine_6_glucuronide"
        )
        val missing = required.filter { it !in ids }
        check(missing.isEmpty()) { "Missing v0.6 drugs: $missing" }
    }
}
