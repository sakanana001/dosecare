package com.dosecare.app.ui

import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.HepaticAdjustment
import com.dosecare.app.domain.catalog.RenalAdjustment
import com.dosecare.app.domain.catalog.RiskLevel

/**
 * 学术化参数 → 普通用户能懂的描述
 *
 * 设计原则:
 * 1. 不取代医学事实,只是换种说法
 * 2. 短句,生活化比喻优先
 * 3. 关键风险(肝/肾/老年)给一句"为什么要小心"
 *
 * 何时显示: DrugDetailScreen 和 ComparisonView 的 toggle "📖 通俗模式" ON
 */
object Plain {

    // ============================================================
    // PK 参数 (体内过程)
    // ============================================================

    /** 半衰期 (h): 体内药物浓度降到一半所需时间 */
    fun halfLife(t12h: Double): String = when {
        t12h < 6 -> "短效 (体内代谢快, 通常一天 3-4 次服药)"
        t12h < 24 -> "中效 (一天 1-2 次服药即可)"
        t12h < 72 -> "长效 (一天 1 次服药, 偶尔漏服问题不大)"
        else -> "超长效 (体内停留很久, 停药后 1-4 周还有残余效应)"
    }

    /** 蛋白结合率: 留在血液里 vs 进入组织 */
    fun proteinBinding(pct: Int): String = when {
        pct >= 90 -> "高度锁在血液里 (剩余 ${100 - pct}% 进入细胞起效, 难被透析)"
        pct >= 70 -> "中度结合 (剩下 ${100 - pct}% 游离起效)"
        else -> "低度结合 (大部分 ${100 - pct}% 游离, 容易分布到全身)"
    }

    /** 治疗窗: 浓度低于 → 无效, 高于 → 中毒 */
    fun therapeuticWindow(low: Double, high: Double, unit: String): String =
        "血药浓度低于 ${formatNum(low)} $unit 时药效不足, 高于 ${formatNum(high)} $unit 时容易中毒; 抽血化验应落在这个范围内"

    /** Cmax: 服药后血药浓度峰值 */
    fun cmax(cmax: Double, unit: String): String =
        "服药后血药浓度峰值 ${formatNum(cmax)} $unit (刚吃完药时最高)"

    /** Cmin: 下次服药前最低浓度 */
    fun cmin(cmin: Double, unit: String): String =
        "下次服药前血药浓度 ${formatNum(cmin)} $unit (药效最弱的时刻)"

    /** Cavg: 平均稳态浓度 */
    fun cavg(cavg: Double, unit: String): String =
        "稳态平均浓度 ${formatNum(cavg)} $unit (长期服用后血液里的平均水平)"

    /** AUC: 浓度-时间曲线下面积 (总暴露量) */
    fun auc(): String = "AUC: 药物在体内\"总暴露量\", 值越大表示身体接触药物越多"

    // ============================================================
    // CYP 角色 (肝脏代谢酶)
    // ============================================================

    /** 底物 (被代谢): 这个药需要哪些酶拆掉 */
    fun cypSubstrate(): String =
        "这个药在肝脏里被某些酶拆解。吃的剂量越多、酶被抑制 → 浓度升高, 容易过量"

    /** 主要代谢途径 (含 CYP 比例 / 非 CYP 标注) */
    fun metabolism(pathwayType: com.dosecare.app.domain.catalog.PathwayType?): String = when (pathwayType) {
        com.dosecare.app.domain.catalog.PathwayType.CYP450 ->
            "这个药主要被肝脏 CYP 酶系统代谢, 容易跟其他 CYP 抑制剂/诱导剂产生相互作用"
        com.dosecare.app.domain.catalog.PathwayType.UGT_GLUCURONIDATION, com.dosecare.app.domain.catalog.PathwayType.GLUCURONIDATION ->
            "经葡萄糖苷酸化代谢 (Phase II), 通常不跟 CYP 抑制剂/诱导剂相互作用, 相互作用少"
        com.dosecare.app.domain.catalog.PathwayType.RENAL_EXCRETION ->
            "原型经肾排出, 不经肝代谢, 几乎无 CYP 介导的药物相互作用, 肾衰时需减量"
        com.dosecare.app.domain.catalog.PathwayType.HYDROLYSIS, com.dosecare.app.domain.catalog.PathwayType.ESTERASE ->
            "经血浆/组织酯酶水解, 不依赖肝肾功能, 几乎无药物相互作用"
        com.dosecare.app.domain.catalog.PathwayType.DEIODINATION ->
            "经脱碘酶代谢 (甲状腺素特殊路径), 相互作用少"
        com.dosecare.app.domain.catalog.PathwayType.MAO ->
            "经单胺氧化酶代谢, 注意 tyramine 反应 (奶酪效应)"
        com.dosecare.app.domain.catalog.PathwayType.DPP4 ->
            "DPP-IV 酶降解 (GLP-1 类似物), 注射给药"
        com.dosecare.app.domain.catalog.PathwayType.BETA_OXIDATION ->
            "经脂肪酸 β-氧化 + UGT 葡萄糖苷酸化, 多途径代谢"
        else ->
            "其他途径代谢, 详见 catalog"
    }

    /** 抑制剂 (影响其他药): 这个药会抑制某些酶 */
    fun cypInhibitor(strength: String): String = when (strength) {
        "STRONG" -> "强抑制剂: 让其它药在体内停留更久, 浓度可升高 5 倍以上 (联用必须减量)"
        "MODERATE" -> "中度抑制剂: 让其它药浓度升高 2-5 倍 (联用需要观察)"
        "WEAK" -> "弱抑制剂: 影响很小, 一般无需调整"
        else -> ""
    }

    /** 诱导剂: 这个药会加速某些酶, 其它药代谢加快 */
    fun cypInducer(strength: String): String = when (strength) {
        "STRONG" -> "强诱导剂: 让其它药在体内停留变短, 浓度可降低 50% 以上 (联用常需加量或换药, 避孕药会失效)"
        "MODERATE" -> "中度诱导剂: 让其它药浓度降低 30-50%"
        "WEAK" -> "弱诱导剂: 影响较小"
        else -> ""
    }

    // ============================================================
    // 调整 (renal/hepatic/elderly/smoking)
    // ============================================================

    fun renalAdj(adj: RenalAdjustment): String = when (adj) {
        RenalAdjustment.NONE -> "肾功能正常无需调整"
        RenalAdjustment.MONITOR -> "肾功能下降时需密切监测肾功和血药浓度"
        RenalAdjustment.REDUCE_50_PCT_EGFR_BELOW_30 ->
            "肾小球滤过率 (eGFR) 低于 30 时, 剂量减半 (严重肾衰禁用)"
        RenalAdjustment.CONTRAINDICATED_EGFR_BELOW_30 ->
            "eGFR 低于 30 时禁用 (会进一步损害肾功能)"
    }

    fun hepaticAdj(adj: HepaticAdjustment): String = when (adj) {
        HepaticAdjustment.NONE -> "肝功能正常无需调整"
        HepaticAdjustment.MONITOR -> "肝功能异常时需监测肝功"
        HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_B ->
            "中度肝损 (Child-Pugh B) 时, 剂量减半"
        HepaticAdjustment.REDUCE_50_PCT_CHILD_PUGH_C ->
            "重度肝损 (Child-Pugh C) 时, 剂量减半或停用"
        HepaticAdjustment.CONTRAINDICATED_IN_SEVERE ->
            "严重肝病禁用 (肝脏代谢不了会蓄积中毒)"
    }

    fun elderly(): String =
        "老年人代谢变慢 + 肝肾衰退, 起始剂量要低, 缓慢加量, 注意跌倒/认知变化"

    // ============================================================
    // 不良反应 (RiskLevel)
    // ============================================================

    fun qtcProlongation(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> "基本不延长心电图 QT 间期, 心脏安全性好"
        RiskLevel.MEDIUM -> "中度延长 QT 间期, 联用其他 QT 延长药或低钾低镁时心律失常风险升高"
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> "显著延长 QT 间期, 可能诱发尖端扭转型室速 (致命心律失常); 联用其他 QT 药属禁忌"
    }

    fun metabolicSyndrome(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> "不影响血糖/血脂/体重"
        RiskLevel.MEDIUM -> "可能引起血糖升高、体重增加 (几个月内 +5-10 斤常见)"
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> "明显代谢紊乱: 显著增重 + 血脂异常 + 糖尿病风险; 用药期间需监测体重/血糖/血脂"
    }

    fun agranulocytosis(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW -> "粒细胞缺乏风险极低 (无特殊监测要求)"
        RiskLevel.LOW -> "低风险, 一般无需频繁验血"
        RiskLevel.MEDIUM -> "可能引起白细胞下降, 需定期验血常规"
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> "显著粒细胞缺乏风险 (致命感染), 必须每 1-4 周验血常规; 一旦发热立即就诊"
    }

    fun extrapyramidal(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> "基本无锥体外系反应 (肌张力异常/震颤/静坐不能)"
        RiskLevel.MEDIUM -> "可能引起肌强直、震颤、静坐不能、急性肌张力障碍"
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> "显著 EPS 风险: 帕金森样症状/静坐不能/迟发性运动障碍 (不可逆); 联用多种抗精神病叠加"
    }

    fun sedation(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> "基本不引起嗜睡, 可正常驾驶"
        RiskLevel.MEDIUM -> "可能引起轻度嗜睡, 服药初期避免驾驶/操作机械"
        RiskLevel.HIGH -> "明显镇静, 服药后避免驾驶/登高; 老年人增加跌倒/髋部骨折风险"
        RiskLevel.VERY_HIGH -> "深度镇静, 严重影响日常; 联用其他镇静药 → 呼吸抑制风险"
    }

    fun sexual(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> "基本不影响性功能"
        RiskLevel.MEDIUM -> "可能引起性欲下降/勃起障碍; 部分患者会换药"
        RiskLevel.HIGH, RiskLevel.VERY_HIGH -> "明显性功能影响 (性欲↓、勃起障碍、射精障碍), 是患者停药主因之一"
    }

    fun hyperprolactinemia(level: RiskLevel): String = when (level) {
        RiskLevel.VERY_LOW, RiskLevel.LOW -> "不影响泌乳素水平"
        RiskLevel.MEDIUM -> "轻度泌乳素升高, 一般无症状"
        RiskLevel.HIGH, RiskLevel.VERY_HIGH ->
            "显著升高泌乳素: 女性月经紊乱/闭经/溢乳, 男性乳房发育/性功能下降; 长期高泌乳素增加骨密度下降风险"
    }

    fun anticholinergicLoad(score: Int): String = when {
        score == 0 -> "无抗胆碱能负担, 不会引起口干/便秘/认知下降"
        score == 1 -> "轻度抗胆碱能: 可能口干或轻度便秘"
        score == 2 -> "中度: 明显口干/便秘/视物模糊, 老年人认知下降风险"
        else -> "重度: 严重口干/便秘/视物模糊/尿潴留/谵妄; 老年人必须避免 (Beers 慎用)"
    }

    // ============================================================
    // 引用 / 指南 (sources)
    // ============================================================

    fun guideline(name: String): String = when {
        name.contains("AGNP") -> "2017 国际精神科 TDM 共识 (Hiemke 等, Pharmacopsychiatry 2018)"
        name.contains("FDA") -> "FDA 药品标签 (美国食品药品监督管理局批准说明书)"
        name.contains("CPIC") -> "CPIC 药物基因剂量指南 (临床药物基因组学联盟)"
        name.contains("PMID") -> "PubMed 医学文献数据库收录的同行评议研究"
        name.contains("ACR") -> "美国风湿病学会指南"
        name.contains("AUA") -> "美国泌尿外科学会指南"
        name.contains("Beers") -> "Beers 老年人潜在不适当用药标准 (2019 美国老年医学会)"
        name.contains("国家") || name.contains("国麻") -> "国家药品监督管理局《麻醉药品和精神药品品种目录》"
        name.contains("中国") -> "中国药典 / 国内药品说明书"
        else -> name
    }

    // ============================================================
    // 工具
    // ============================================================

    private fun formatNum(n: Double): String =
        if (n >= 100) "%.0f".format(n) else if (n >= 10) "%.1f".format(n) else "%.2f".format(n)
}
