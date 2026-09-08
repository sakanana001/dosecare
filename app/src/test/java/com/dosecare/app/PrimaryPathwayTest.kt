package com.dosecare.app

import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.catalog.PathwayType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimaryPathwayTest {
    @Test fun primary_pathway_159_drugs() {
        val raw = javaClass.classLoader.getResource("drugs/v0.6.json")!!.readText()
        val drugs = DrugCatalogLoader.fromJson(raw)

        // 1. 所有药都补了 primaryPathway
        val withPath = drugs.count { it.cypProfile.primaryPathway != null }
        println("=== primaryPathway 覆盖率 ===")
        println("  总药数: ${drugs.size}")
        println("  有 primaryPathway: $withPath / ${drugs.size}")
        assertTrue("所有药应有 primaryPathway", withPath == drugs.size)

        // 2. 重点药验证
        val cases = mapOf(
            "lorazepam" to PathwayType.UGT_GLUCURONIDATION,
            "lithium" to PathwayType.RENAL_EXCRETION,
            "gabapentin" to PathwayType.RENAL_EXCRETION,
            "valproic_acid" to PathwayType.BETA_OXIDATION,
            "lamotrigine" to PathwayType.UGT_GLUCURONIDATION,
            "aspirin" to PathwayType.HYDROLYSIS,
            "metformin" to PathwayType.RENAL_EXCRETION,
            "levothyroxine" to PathwayType.DEIODINATION,
            "clozapine" to PathwayType.CYP450,    // 有 substrates
            "paroxetine" to PathwayType.CYP450    // paroxetine 之前 substrates 是空, 实际是 CYP2D6
        )
        println("=== 重点药验证 ===")
        cases.forEach { (id, expectedType) ->
            val drug = drugs.first { it.id == id }
            val pp = drug.cypProfile.primaryPathway
            val pt = drug.cypProfile.pathwayType
            assertNotNull("$id primaryPathway 应有", pp)
            assertTrue(
                "$id pathwayType 期望 $expectedType 实际 $pt",
                pt == expectedType
            )
            println("  $id: $pt — $pp")
        }
        println("✅ 159 药 primaryPathway 全覆盖 + 重点药 pathwayType 正确")
    }
}
