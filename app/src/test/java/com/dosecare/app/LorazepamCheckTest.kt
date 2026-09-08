package com.dosecare.app

import com.dosecare.app.domain.catalog.DrugCatalogLoader
import com.dosecare.app.domain.pk.DoseEvent
import com.dosecare.app.domain.pk.PkEngine
import com.dosecare.app.domain.pk.PkModel
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln

class LorazepamCheckTest {
    @Test fun lorazepam_5mg_qd_actual_cmax_vs_window() {
        val raw = javaClass.classLoader.getResource("drugs/v0.6.json")!!.readText()
        val drugs = DrugCatalogLoader.fromJson(raw)
        val lorazepam = drugs.first { it.id == "lorazepam" }
        println("=== 劳拉西泮 PK 数据 ===")
        println("genericNameZh: ${lorazepam.genericNameZh}")
        println("forms[0].f: ${lorazepam.forms[0].f}")
        println("forms[0].kaPerHour: ${lorazepam.forms[0].kaPerHour}")
        println("forms[0].commonDoseRangeMg: ${lorazepam.forms[0].commonDoseRangeMg}")
        println("therapeuticWindow: ${lorazepam.therapeuticWindow}")
        val pk = lorazepam.pkModel
        println("pkModel class: ${pk::class.simpleName}")
        if (pk is PkModel.OneCompartmentWithAbsorption) {
            println("  f=${pk.f} ka=${pk.kaPerHour} ke=${pk.kePerHour} Vd=${pk.vdLiters} route=${pk.route}")
            println("  tHalf=${pk.tHalfHours} CL=${pk.clearanceLPerHour} tMax=${pk.tMaxHours}")
        }
        val doseMg = 5.0
        val weight = 70.0
        val baseVdPerKg = (pk as PkModel.OneCompartmentWithAbsorption).vdLiters / 70.0
        val adjusted = pk.copy(vdLiters = baseVdPerKg * weight)
        val tau = 24.0
        val doses = (0 until 14).map { DoseEvent(doseMg, it * tau) }
        val curve = PkEngine().multiDoseCurve(
            model = adjusted,
            doseEvents = doses,
            tStart = 0.0, tEnd = tau * 5, stepHours = 0.25
        )
        println("=== TdmModule 复刻: ${doseMg}mg qd 70kg ===")
        println("  cMax=${curve.cMax}  cMaxT=${curve.cMaxT}  cMin=${curve.cMin}")
        println("  cMax mg/L = ${curve.cMax}  cMax ng/mL = ${curve.cMax * 1000}")
        println("  Window: ${lorazepam.therapeuticWindow}")
        println("  cMaxUnitFactor=${lorazepam.cMaxUnitFactor}  skipBand=${lorazepam.skipTherapeuticWindowBand}")
        val win = lorazepam.therapeuticWindow
        if (win != null) {
            val cmaxD = curve.cMax * lorazepam.cMaxUnitFactor
            val inWin = cmaxD in win.low..win.high
            println("  修正后: cmaxD=${cmaxD} ${win.unit}  是否在 治疗窗 ${win.low}-${win.high} 内: $inWin")
        }
        println("=== 手动算: 单次 5mg F=0.93 ka=3 ke=0.0693 Vd=70L ===")
        val f = 0.93; val ka = 3.0; val ke = 0.0693; val Vd = 70.0
        val tmax = ln(ka/ke) / (ka - ke)
        val coeff = (f * doseMg * ka) / (Vd * (ka - ke))
        val cmax1 = coeff * (exp(-ke*tmax) - exp(-ka*tmax))
        val r = 1.0 / (1.0 - exp(-ke*tau))
        val cmaxSS = cmax1 * r
        println("  理论 tmax=${tmax} cmax_单=${cmax1} mg/L = ${cmax1*1000} ng/mL")
        println("  累积比 R=$r  cmax_稳态=${cmaxSS*1000} ng/mL")
    }
}
