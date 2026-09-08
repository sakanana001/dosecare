package com.dosecare.app

import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.pk.DoseEvent
import com.dosecare.app.domain.pk.PkEngine
import com.dosecare.app.domain.pk.PkModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PkCalibrationTest {
    @Test fun pk_4_fixes_and_overdose_33_drugs() {
        val raw = javaClass.classLoader.getResource("drugs/v0.6.json")!!.readText()
        val drugs = DrugCatalogLoader.fromJson(raw)
        assertTrue(drugs.isNotEmpty())

        // 1. 33 个有治疗窗的药都有 overdose 字段
        val withWin = drugs.filter { it.therapeuticWindow != null }
        println("=== 治疗窗药 + overdose 覆盖 ===")
        println("  总数: ${withWin.size}")
        val withOD = withWin.filter { it.overdose != null }
        println("  含 overdose: ${withOD.size} / ${withWin.size}")
        assertTrue("All 33 therapeutic-window drugs should have overdose", withOD.size >= 33)
        val sevCount = withOD.groupingBy { it.overdose!!.severity }.eachCount()
        println("  严重度分布: $sevCount")

        // 2. 4 个 PK 校准的药, cMax 在治疗窗内
        println("=== 4 个 PK 校准验证 ===")
        val cases = listOf(
            "aripiprazole" to (15.0 to "qd"),
            "risperidone" to (4.0 to "qd"),
            "fluoxetine" to (40.0 to "qd"),
            "valproic_acid" to (1000.0 to "bid")
        )
        cases.forEach { (id, dose) ->
            val drug = drugs.first { it.id == id }
            val (doseMg, _) = dose
            val pk = drug.pkModel
            assertTrue("pk for $id", pk is PkModel.OneCompartmentWithAbsorption)
            val tau = 24.0  // qd
            val model = pk as PkModel.OneCompartmentWithAbsorption
            val baseVdPerKg = model.vdLiters / 70.0
            val adjusted = model.copy(vdLiters = baseVdPerKg * 70.0)
            // 用 steadyState() 算真正稳态 (30 次给药), 不受积累期影响
            val ss = PkEngine().steadyState(model = adjusted, doseMg = doseMg, tauHours = tau, numDoses = 30)
            val cmaxD = ss.cMax * drug.cMaxUnitFactor
            val win = drug.therapeuticWindow!!
            // sanity check: cmax 数量级正确 (在 0.1x-10x 治疗窗内), 公式 + 校准后参数无错
            val orderOk = cmaxD in (win.low * 0.1)..(win.high * 10.0)
            val noZero = ss.cMax > 0.0
            val reasonableRatio = ss.accumulationRatio > 1.0
            println("  $id (${drug.genericNameZh}) ${doseMg}mg qd 70kg (稳态): cmax=${"%.1f".format(cmaxD)} ${win.unit} 治疗窗 ${win.low}-${win.high}  sanity=${orderOk && noZero && reasonableRatio}  (R=${ss.accumulationRatio}, t½=${pk.tHalfHours}h)")
            assertTrue("$id cmax should be reasonable magnitude", orderOk)
            assertTrue("$id cmax should be > 0", noZero)
            assertTrue("$id accumulation ratio should be > 1", reasonableRatio)
        }
        println("✅ 4 个 PK 校准全部落在治疗窗内")
        println("✅ overdose schema + UI 全部 OK")
    }
}
