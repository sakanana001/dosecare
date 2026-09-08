package patient

/**
 * 患者画像
 *
 * 决定 PK 估算和警示的"个体化"程度。
 * 字段应尽量简洁（避免给用户造成填写负担），但关键的（肝/肾/吸烟/CYP 基因）必须有。
 */
data class PatientProfile(
    val displayName: String = "我",
    val ageYears: Int? = null,
    val biologicalSex: BiologicalSex = BiologicalSex.UNKNOWN,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val smoker: Boolean = false,
    val cigarettesPerDay: Int = 0,
    val caffeineMgPerDay: Int = 0,
    val alcoholUnitsPerWeek: Int = 0,
    val egfr: Double? = null,                  // mL/min/1.73m²
    val childPugh: ChildPugh? = null,
    val conditions: List<Condition> = emptyList(),
    val cyp2D6Genotype: CypGenotype? = null,   // PM/IM/EM/UM
    val cyp2C19Genotype: CypGenotype? = null
) {
    val bmi: Double?
        get() = if (weightKg != null && heightCm != null) {
            weightKg / ((heightCm / 100.0).let { it * it })
        } else null

    val isElderly: Boolean
        get() = (ageYears ?: 0) >= 65
}

enum class BiologicalSex(val displayName: String) {
    MALE("男"),
    FEMALE("女"),
    X("其他 / 不愿透露"),
    UNKNOWN("未填写")
}

enum class ChildPugh(val displayName: String) {
    A("A"),
    B("B"),
    C("C")
}

/**
 * CYP 基因多态性
 *
 * - PM: poor metabolizer（慢代谢） → 底物浓度 ↑；抑制剂更危险
 * - IM: intermediate metabolizer
 * - EM: extensive metabolizer（正常）
 * - UM: ultra-rapid metabolizer（超快） → 底物浓度 ↓
 */
enum class CypGenotype(val displayName: String) {
    PM("慢代谢 (PM)"),
    IM("中代谢 (IM)"),
    EM("正常 (EM)"),
    UM("超快代谢 (UM)")
}

/**
 * 既有疾病（用于规则引擎、警示文案）
 */
enum class Condition(val displayName: String) {
    SCHIZOPHRENIA("精神分裂症"),
    BIPOLAR("双相情感障碍"),
    MAJOR_DEPRESSION("重度抑郁"),
    ANXIETY_DISORDER("焦虑障碍"),
    OCD("强迫症"),
    PTSD("创伤后应激障碍"),
    ADHD("注意缺陷多动障碍"),

    T2DM("2 型糖尿病"),
    T1DM("1 型糖尿病"),
    HYPOTHYROIDISM("甲状腺功能减退"),
    HYPERTHYROIDISM("甲状腺功能亢进"),
    HYPERTENSION("高血压"),
    DYSLIPIDEMIA("血脂异常"),
    CORONARY_ARTERY_DISEASE("冠心病"),
    CKD("慢性肾病"),
    NAFLD("非酒精性脂肪肝"),
    OSTEOPOROSIS("骨质疏松"),

    OTHER("其他")
}
