package com.dosecare.app.domain.catalog

/**
 * 按主治症状/疾病的分类 — 一个药可归多个 IndicationGroup
 *
 * 用于"按主治症状"视图, 与 DrugCategory (按药理学分类) 并列
 *
 * 例:
 * - 舍曲林: ANTIDEPRESSANT, ANXIOLYTIC category → DEPRESSION + ANXIETY + OCD indication
 * - 喹硫平: ANTIPSYCHOTIC category → SCHIZOPHRENIA + BIPOLAR (治疗双相抑郁)
 * - 加巴喷丁: ANTIEPILEPTIC category → EPILEPSY + NEUROPATHIC_PAIN
 */
enum class IndicationGroup(val displayName: String) {
    // 精神科
    DEPRESSION("抑郁"),                              // SSRI/SNRI/TCA/MAOI/NaSSA/NDRI
    SCHIZOPHRENIA("精神分裂症"),                       // 抗精神病药
    BIPOLAR("双相情感障碍"),                           // 心境稳定剂 + 部分抗精神病
    ANXIETY("焦虑障碍"),                              // BZD 抗焦虑/丁螺环酮/SSRI 部分
    INSOMNIA("失眠"),                                 // 助眠药
    ADHD("注意缺陷多动障碍"),                          // 兴奋剂/托莫西汀
    OCD("强迫症"),                                   // SSRI 强迫
    PTSD("创伤后应激障碍"),                           // SSRI (舍曲林/帕罗西汀)
    EATING_DISORDER("进食障碍"),                       // 氟西汀

    // 神经科
    EPILEPSY("癫痫"),                                 // 抗癫痫药
    PARKINSON("帕金森病"),                            // 多巴胺能药
    ALZHEIMERS("阿尔茨海默病"),                       // 胆碱酯酶抑制剂
    MIGRAINE("偏头痛"),                               // 曲坦类/氟桂利嗪
    NEUROPATHIC_PAIN("神经病理性疼痛"),                // 加巴喷丁/普瑞巴林/度洛西汀/TCA

    // 疼痛/麻醉
    PAIN("疼痛"),                                    // 阿片/TCA 镇痛
    ANESTHESIA("麻醉"),                              // 局麻/全麻 (氯胺酮/丙泊酚等)

    // 内科
    DIABETES("糖尿病"),                              // 二甲双胍/恩格列净/SU 类等
    HYPERTENSION("高血压"),                          // 降压药
    DYSLIPIDEMIA("血脂异常"),                        // 他汀类
    THYROID("甲状腺疾病"),                           // 甲状腺素/抗甲状腺药
    ANTICOAGULATION("抗凝/抗血小板"),                  // 华法林/利伐沙班/阿司匹林/氯吡格雷
    ASTHMA_COPD("哮喘 / COPD"),                       // 支气管扩张剂/吸入激素
    OSTEOPOROSIS("骨质疏松"),                        // 双膦酸盐
    HEART_FAILURE("心力衰竭"),                       // β受体阻滞剂/ACEI 等
    ARRHYTHMIA("心律失常"),                          // 胺碘酮/索他洛尔
    BPH("前列腺增生"),                              // α1 阻滞剂/5α还原酶抑制剂
    GERD("胃食管反流"),                             // PPI

    // 物质依赖
    SUBSTANCE_USE("物质依赖 (酒精/阿片)"),             // 纳曲酮/双硫仑/阿坎酸/美沙酮维持
    NICOTINE("烟碱依赖"),                           // 伐尼克兰等

    // 感染/炎症
    INFECTION("感染 (细菌/真菌/病毒)"),               // 氟康唑/利福平/多替拉韦
    GOUT("痛风"),                                   // 别嘌醇/秋水仙碱
    INFLAMMATION("炎症/免疫"),                        // 皮质激素/NSAID
    ALLERGY("过敏"),                                // 抗组胺药

    // 内分泌
    ADRENAL("肾上腺皮质"),                           // 糖皮质激素
    HORMONE("激素替代"),                            // 雌二醇/孕酮/睾酮

    // 辅助
    SUPPLEMENT("补充剂"),                           // 钙/铁/叶酸
    DI("尿崩症"),                                   // 去氨加压素
    ANTICHOLINERGIC("抗胆碱 (EPS)"),                  // 苯扎托品/苯海索
    ANTIDEPRESSANT_AUGMENTATION("抗抑郁增效"),         // 锂/甲状腺素/阿托品 (增效抗抑郁)
    WITHDRAWAL("戒断"),                             // 苯二氮卓戒断
    NAUSEA("恶心呕吐"),                              // 昂丹司琼等
    SEDATION("镇静"),                               // 苯海拉明镇静

    // 兜底
    OTHER("其他")
}
