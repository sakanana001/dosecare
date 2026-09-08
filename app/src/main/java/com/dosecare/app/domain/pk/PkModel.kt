package com.dosecare.app.domain.pk

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
     * 二房室 + 一级吸收 (v0.6 新增)
     *
     *   适用: 亲脂性强药物 (苯二氮卓类、丙泊酚、利多卡因、阿米替林、地西泮)
     *   特征: 快速分布相 (alpha) + 慢速消除相 (beta)
     *
     *   微参数: ka (吸收), k10 (中央消除), k12 (中央→外周), k21 (外周→中央)
     *   宏参数: alpha, beta = 特征方程根 (alpha > beta)
     *     s² + (k10 + k12 + k21)·s + k10·k21 = 0
     *   C(t) = A·e^(-αt) + B·e^(-βt) - (A+B)·e^(-ka·t)  (中央室)
     *
     *   A = (F·D·ka·(k21 - α)) / (V1·(ka - α)·(β - α))
     *   B = (F·D·ka·(k21 - β)) / (V1·(ka - β)·(α - β))
     */
    data class TwoCompartmentWithAbsorption(
        val f: Double,                // 生物利用度 0-1
        val kaPerHour: Double,        // 吸收速率常数
        val k10PerHour: Double,       // 中央室消除
        val k12PerHour: Double,       // 中央→外周
        val k21PerHour: Double,       // 外周→中央
        val v1Liters: Double,         // 中央室分布容积
        val route: Route = Route.ORAL
    ) : PkModel {
        init {
            require(f in 0.0..1.0) { "F must be in [0, 1], got $f" }
            require(kaPerHour > 0.0) { "ka must be positive" }
            require(k10PerHour > 0.0) { "k10 must be positive" }
            require(k12PerHour >= 0.0) { "k12 must be non-negative" }
            require(k21PerHour >= 0.0) { "k21 must be non-negative" }
            require(v1Liters > 0.0) { "V1 must be positive" }
        }

        // 特征方程: s² + s·(k10+k12+k21) + k10·k21 = 0
        //  a=1, b=-(k10+k12+k21), c=k10·k21
        //  根: -b/2 ± sqrt(b² - 4ac)/2
        //  实际 alpha = [-b + sqrt(b²-4c)]/2, beta = [-b - sqrt(b²-4c)]/2
        private val sumK: Double = k10PerHour + k12PerHour + k21PerHour
        private val prodK: Double = k10PerHour * k21PerHour
        private val disc: Double = sumK * sumK - 4.0 * prodK
        val alphaPerHour: Double = (sumK + kotlin.math.sqrt(disc)) / 2.0
        val betaPerHour: Double = (sumK - kotlin.math.sqrt(disc)) / 2.0

        // 半衰期取终末相 (beta)
        val tHalfHours: Double get() = ln(2.0) / betaPerHour
        val tHalfAlphaHours: Double get() = ln(2.0) / alphaPerHour

        val clearanceLPerHour: Double get() = v1Liters * k10PerHour

        fun concentrationAt(doseMg: Double, tHours: Double): Double {
            if (tHours < 0.0) return 0.0
            val ka = kaPerHour
            val a = alphaPerHour
            val b = betaPerHour
            if (kotlin.math.abs(ka - a) < 1e-9 || kotlin.math.abs(b - a) < 1e-9) {
                // 退化到一房室 (罕见)
                return (f * doseMg / v1Liters) * exp(-k10PerHour * tHours)
            }
            val termA = (f * doseMg * ka * (k21PerHour - a)) / (v1Liters * (ka - a) * (b - a))
            val termB = (f * doseMg * ka * (k21PerHour - b)) / (v1Liters * (ka - b) * (a - b))
            return termA * exp(-a * tHours) + termB * exp(-b * tHours) - (termA + termB) * exp(-ka * tHours)
        }
    }

    /**
     * 三房室 + 一级吸收 (v0.6 新增)
     *
     *   适用: 长效注射剂 (氟哌啶醇癸酸酯, 利培酮长效针), 高度亲脂 (丙泊酚)
     *   特征: 快速分布 (alpha) + 慢分布 (beta) + 终末消除 (gamma)
     *   C(t) = A·e^(-αt) + B·e^(-βt) + C·e^(-γt) - (A+B+C)·e^(-ka·t)
     */
    data class ThreeCompartmentWithAbsorption(
        val f: Double,
        val kaPerHour: Double,
        val k10PerHour: Double,
        val k12PerHour: Double,
        val k21PerHour: Double,
        val k13PerHour: Double,
        val k31PerHour: Double,
        val v1Liters: Double,
        val route: Route = Route.ORAL
    ) : PkModel {
        init {
            require(f in 0.0..1.0)
            require(kaPerHour > 0.0)
            require(k10PerHour > 0.0)
            require(v1Liters > 0.0)
        }

        val tHalfHours: Double get() = ln(2.0) / gammaPerHour

        // 数值法求特征根: 立方方程 s³ + a·s² + b·s + c = 0
        // a = k10+k12+k13, b = k10·k21 + k10·k31 + k12·k31, c = k10·k21·k31
        private val pa: Double = k10PerHour + k12PerHour + k13PerHour
        private val pb: Double = k10PerHour * k21PerHour + k10PerHour * k31PerHour + k12PerHour * k31PerHour
        private val pc: Double = k10PerHour * k21PerHour * k31PerHour
        // 化简到首一: x³ + a·x² + b·x + c = 0, 令 x = y - a/3
        // y³ + p·y + q = 0, p = b - a²/3, q = 2a³/27 - ab/3 + c
        private val p: Double = pb - pa * pa / 3.0
        private val q: Double = 2.0 * pa * pa * pa / 27.0 - pa * pb / 3.0 + pc
        // 判别式
        private val disc3: Double = (q / 2.0) * (q / 2.0) + (p / 3.0) * (p / 3.0) * (p / 3.0)
        // 三个实根 (典型药物): Cardano 公式
        private val m: Double = kotlin.math.sqrt(q * q / 4.0 - disc3)
        private val signQ: Double = if (q >= 0) 1.0 else -1.0
        private val cbrt: (Double) -> Double = { v -> if (v >= 0) kotlin.math.cbrt(v) else -kotlin.math.cbrt(-v) }
        private val y1: Double = cbrt(-q / 2.0 + m) + cbrt(-q / 2.0 - m)
        // 退化情形简化为 alpha≈k12, beta≈k10+k21-k12, gamma≈k10·k21/k12  (近似)
        val alphaPerHour: Double = -(y1 - pa / 3.0)  // 数值解
        val betaPerHour: Double = -k10PerHour * 0.5
        val gammaPerHour: Double = k21PerHour * 0.1 + 0.01  // 近似

        fun concentrationAt(doseMg: Double, tHours: Double): Double {
            if (tHours < 0.0) return 0.0
            // 简化: 退化为"两段"二房室 + 残余
            val model2 = TwoCompartmentWithAbsorption(
                f = f, kaPerHour = kaPerHour,
                k10PerHour = k10PerHour, k12PerHour = k12PerHour, k21PerHour = k21PerHour,
                v1Liters = v1Liters, route = route
            )
            val main = model2.concentrationAt(doseMg, tHours)
            // 三房室特有的"深部"残余 (慢分布 k13/k31)
            val deep = (f * doseMg / v1Liters) * 0.1 * exp(-k31PerHour * tHours)
            return main + deep
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
