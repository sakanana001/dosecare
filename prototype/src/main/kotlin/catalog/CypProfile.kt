package catalog

/**
 * CYP 酶及其效应定义
 *
 * 精神科的"致命一半"都在 CYP 上。
 * - 氟伏沙明（强 CYP1A2 抑制）+ 氯氮平 → 5-10 倍 AUC 升高
 * - 氟康唑（强 CYP3A4 抑制）+ 喹硫平 → 显著延长 QT
 * - 卡马西平（CYP3A4 强诱导）+ 很多药 → 失效
 *
 * 数据源：Flockhart Table (Indiana University), FDA Guidance
 */

enum class CypEnzyme(val displayName: String) {
    CYP1A2("CYP1A2"),
    CYP2D6("CYP2D6"),
    CYP3A4("CYP3A4"),
    CYP2C9("CYP2C9"),
    CYP2C19("CYP2C19"),
    CYP2E1("CYP2E1"),
    P_GP("P-糖蛋白")  // 转运体；不是 CYP 但常与 CYP 效应联动
}

enum class EffectStrength {
    WEAK,        // AUC 变化 1.25-2 倍
    MODERATE,    // 2-5 倍
    STRONG;      // ≥ 5 倍

    companion object {
        /** 根据 AUC 倍数查 FDA 指南 */
        fun fromAucFold(fold: Double): EffectStrength = when {
            fold >= 5.0 -> STRONG
            fold >= 2.0 -> MODERATE
            fold >= 1.25 -> WEAK
            else -> WEAK
        }
    }
}

/**
 * 该药作为代谢底物时，每个 CYP 酶贡献的比例。
 *
 * sum(substrates.fraction) ≤ 1.0，剩余可能走非 CYP 路径（UGT、SULT、肾排等）。
 *
 * 例：氯氮平的 substrates = [{CYP1A2, 0.7}, {CYP3A4, 0.2}, {CYP2C19, 0.1}]
 *     意味着 70% 经 CYP1A2、20% 经 CYP3A4、10% 经 CYP2C19 代谢。
 */
data class CypContribution(
    val cyp: CypEnzyme,
    val fraction: Double  // 0-1
)

/**
 * 该药作为其他药的抑制剂时，对每个 CYP 的抑制强度。
 */
data class CypInhibition(
    val cyp: CypEnzyme,
    val strength: EffectStrength
)

/**
 * 该药作为其他药的诱导剂时，对每个 CYP 的诱导强度。
 */
data class CypInduction(
    val cyp: CypEnzyme,
    val strength: EffectStrength
)

/**
 * 完整 CYP 谱（一个药在 CYP 网络中的"角色"）
 */
data class CypProfile(
    val substrates: List<CypContribution> = emptyList(),
    val inhibitors: List<CypInhibition> = emptyList(),
    val inducers: List<CypInduction> = emptyList()
) {
    init {
        val sum = substrates.sumOf { it.fraction }
        require(sum <= 1.01) { "Substrate fractions sum to $sum, must be ≤ 1.0" }
    }

    /** 该药是否被某酶代谢（是某酶的底物） */
    fun isSubstrateOf(cyp: CypEnzyme): Boolean =
        substrates.any { it.cyp == cyp }

    /** 取得该药作为某酶底物的代谢贡献比例 */
    fun substrateFraction(cyp: CypEnzyme): Double =
        substrates.find { it.cyp == cyp }?.fraction ?: 0.0

    /** 该药是否抑制某酶 */
    fun inhibits(cyp: CypEnzyme): EffectStrength? =
        inhibitors.find { it.cyp == cyp }?.strength

    /** 该药是否诱导某酶 */
    fun induces(cyp: CypEnzyme): EffectStrength? =
        inducers.find { it.cyp == cyp }?.strength
}
