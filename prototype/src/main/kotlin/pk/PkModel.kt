package pk

import kotlin.math.exp
import kotlin.math.ln

/**
 * PK（药代动力学）模型定义
 *
 * 选择原则：
 * - 精神科+内分泌共病 90%+ 药物 → 一房室口服 即可
 * - 静脉/肌肉 → 一房室 IV/IM
 * - 苯妥英等非线性 → Michaelis-Menten（v0.8）
 * - 长效注射剂 → 三房室（v0.8）
 *
 * 所有模型参数都已对齐 AGNP 共识与药品说明书常见 PK 报告格式。
 */
sealed interface PkModel {

    /** 剂型与给药途径。影响 F 字段是否参与。 */
    enum class Route(val displayName: String) {
        ORAL("口服"),
        IM("肌肉注射"),
        IV("静脉注射"),
        SL("舌下"),
        SC("皮下")
    }

    /**
     * 一房室 + 一级吸收（口服/IM/SC 等有吸收相的剂型）
     *
     *   C(t) = (F·D·ka) / (Vd·(ka-ke)) · (e^(-ke·t) - e^(-ka·t))    t ≥ 0
     *
     * 假设：药物在体内瞬间均匀分布（单房室），按一级速率消除。
     */
    data class OneCompartmentWithAbsorption(
        val f: Double,                // 生物利用度 0-1（IV = 1.0）
        val kaPerHour: Double,        // 吸收速率常数 h⁻¹
        val kePerHour: Double,        // 消除速率常数 h⁻¹
        val vdLiters: Double,         // 表观分布容积 L（或 L/kg 视用户输入体重）
        val route: Route = Route.ORAL
    ) : PkModel {
        init {
            require(f in 0.0..1.0) { "F must be in [0, 1], got $f" }
            require(kaPerHour > 0.0) { "ka must be positive" }
            require(kePerHour > 0.0) { "ke must be positive" }
            require(vdLiters > 0.0) { "Vd must be positive" }
        }

        /** 理论 Tmax（达峰时间），h。仅当 ka > ke 时有意义。 */
        val tMaxHours: Double
            get() = if (kaPerHour > kePerHour) {
                ln(kaPerHour / kePerHour) / (kaPerHour - kePerHour)
            } else Double.POSITIVE_INFINITY

        /** 半衰期，h。 */
        val tHalfHours: Double
            get() = ln(2.0) / kePerHour

        /** 清除率 L/h。 */
        val clearanceLPerHour: Double
            get() = vdLiters * kePerHour

        /**
         * 单次给药后 t 时刻的血药浓度。
         *
         * @param doseMg 剂量（mg）
         * @param tHours 给药后时间（h），可为负（表示给药前）但结果视为 0
         */
        fun concentrationAt(doseMg: Double, tHours: Double): Double {
            if (tHours < 0.0) return 0.0
            if (kaPerHour == kePerHour) {
                // 简化的极限情况（C(t) = F·D·ka·t·e^(-ke·t) / Vd）
                return f * doseMg * kaPerHour * tHours * exp(-kePerHour * tHours) / vdLiters
            }
            val coeff = (f * doseMg * kaPerHour) / (vdLiters * (kaPerHour - kePerHour))
            return coeff * (exp(-kePerHour * tHours) - exp(-kaPerHour * tHours))
        }
    }

    /**
     * 一房室静脉推注
     *
     *   C(t) = (D / Vd) · e^(-ke·t)
     */
    data class OneCompartmentIv(
        val kePerHour: Double,
        val vdLiters: Double
    ) : PkModel {
        init {
            require(kePerHour > 0.0)
            require(vdLiters > 0.0)
        }

        val tHalfHours: Double get() = ln(2.0) / kePerHour

        fun concentrationAt(doseMg: Double, tHours: Double): Double {
            if (tHours < 0.0) return 0.0
            return (doseMg / vdLiters) * exp(-kePerHour * tHours)
        }
    }

    /**
     * 治疗窗定义（可选：部分药无明确治疗窗）
     */
    data class TherapeuticWindow(
        val low: Double,
        val high: Double,
        val unit: String,             // "ng/mL", "mmol/L", "μg/mL"
        val guidelineSource: String? = null  // "AGNP 2017"
    ) {
        fun contains(concentration: Double): Boolean =
            concentration in low..high
    }
}

/**
 * 给药事件：剂量 + 时刻。
 */
data class DoseEvent(
    val doseMg: Double,
    val tHours: Double   // 自参考时间起的小时数
)

/**
 * PK 曲线计算结果
 */
data class PkCurve(
    val drugId: String,
    val t: List<Double>,            // 时间轴（h）
    val c: List<Double>,            // 浓度
    val cMax: Double,               // 峰值浓度
    val cMaxT: Double,              // 达峰时间
    val cMin: Double,               // 谷浓度（最后一个时间点或稳态最低）
    val cAvg: Double,               // 曲线下面积 / 总时长
    val auc: Double,                // 曲线下面积
    val window: PkModel.TherapeuticWindow? = null
) {
    val windowStatus: WindowStatus = when {
        window == null -> WindowStatus.NO_WINDOW
        cMin < window.low -> WindowStatus.BELOW
        cMax > window.high -> WindowStatus.ABOVE
        else -> WindowStatus.IN_WINDOW
    }
}

enum class WindowStatus {
    BELOW,        // 谷浓度低于治疗窗（可能次效）
    IN_WINDOW,    // 在治疗窗内
    ABOVE,        // 峰值超过治疗窗（可能中毒）
    NO_WINDOW,    // 该药无治疗窗
    UNKNOWN       // 数据不足
}
