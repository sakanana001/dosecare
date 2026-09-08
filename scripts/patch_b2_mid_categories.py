"""
B2: 中类各 +1-2 药, 目标总数 200+
"""
import json
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]
existing_ids = {d["id"] for d in drugs}
print(f"Loaded {len(drugs)} drugs")


def new_template():
    return {
        "id": "", "genericName": "", "genericNameZh": "", "brandNames": [],
        "category": "OTHER", "subcategory": None, "atc": None,
        "pkModel": "ONE_COMPARTMENT_ORAL",
        "forms": [{"route": "ORAL", "f": 0.7, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [10.0, 100.0]}],
        "kePerHour": 0.1, "tHalfHours": 7.0, "tHalfRangeHours": [5.0, 12.0],
        "vdLPerKg": 1.5, "clLPerHour": 10.0, "proteinBindingPct": 50,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "-", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "-", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "indicationGroups": [],
        "adjustments": {"renal": "NONE", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "pharmacology": "-",
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": [],
        "cMaxUnitFactor": 1.0, "skipTherapeuticWindowBand": False
    }


B2_DRUGS = [
    # ANTIDEPRESSANT (26 → +2)
    {
        "id": "mirtazapine", "genericName": "Mirtazapine", "genericNameZh": "米氮平",
        "category": "ANTIDEPRESSANT", "subcategory": "NASSA", "atc": "N06AX11",
        "brandNames": ["Remeron", "瑞美隆"],
        "pharmacology": "去甲肾上腺素能和特异性 5-HT 能抗抑郁 (NaSSA); 拮抗 α2 / 5-HT2 / 5-HT3, 强 H1 拮抗 (镇静 + 体重↑)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.5, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [7.5, 45.0]}],
        "kePerHour": 0.06, "tHalfHours": 20.0, "tHalfRangeHours": [15.0, 30.0],
        "vdLPerKg": 4.5, "clLPerHour": 18.0, "proteinBindingPct": 85,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.4}, {"cyp": "CYP2D6", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2D6 + CYP1A2 多途径", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "desmethyl_mirtazapine", "name": "N-去甲米氮平", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C",
            "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "mao_inhibitor", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 MAOI → 5-HT 综合征 / 高热危象"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["体重", "血脂", "肝功能", "自杀念头"]},
        "indicationGroups": ["DEPRESSION", "PTSD", "INSOMNIA"],
        "overdose": {"symptoms": "深度镇静, 抗胆碱能, 心动过速, 5-HT 综合征风险; 致死罕见",
            "severity": "MODERATE", "toxicDoseEstimateMg": 2000.0, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护", "antidote": None,
            "dataSource": "DailyMed Remeron Label §10"},
        "references": ["DailyMed Remeron Label §10"]
    },
    {
        "id": "trazodone", "genericName": "Trazodone", "genericNameZh": "曲唑酮",
        "category": "ANTIDEPRESSANT", "subcategory": "SARI", "atc": "N06AX05",
        "brandNames": ["Desyrel", "美抒玉"],
        "pharmacology": "5-HT 再摄取抑制剂 + 5-HT2 拮抗 (SARI); 主要低剂量用于失眠 (镇静)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.8, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [50.0, 300.0]}],
        "kePerHour": 0.05, "tHalfHours": 7.0, "tHalfRangeHours": [4.0, 9.0],
        "vdLPerKg": 1.0, "clLPerHour": 8.0, "proteinBindingPct": 90,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.7}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 → m-chlorophenylpiperazine (mCPP, 活性代谢物)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "m_cpp", "name": "m-氯苯哌嗪 (mCPP)", "activityRatio": 0.3,
            "note": "CYP3A4; 5-HT2C 激动, 引起焦虑/失眠 (副作用)"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C", "elderly": "低剂量起始",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "mao_inhibitor", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 MAOI → 5-HT 综合征"},
            {"triggerDrugId": "clarithromycin", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 4.0], "severity": "MAJOR",
             "clinicalNote": "联用克拉霉素 → trazodone AUC ↑2-4×, 显著 CNS 抑制"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["ECG 老年/心血管病"]},
        "indicationGroups": ["DEPRESSION", "INSOMNIA"],
        "overdose": {"symptoms": "深度镇静, 低血压, QT 延长, 阴茎异常勃起 (priapism, 罕见)",
            "severity": "SEVERE", "toxicDoseEstimateMg": 2000.0, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护; 低血压补液 + 升压", "antidote": None,
            "dataSource": "DailyMed Desyrel Label §10"},
        "references": ["DailyMed Desyrel Label §10"]
    },

    # ANXIOLYTIC (17 → +2)
    {
        "id": "buspirone", "genericName": "Buspirone", "genericNameZh": "丁螺环酮",
        "category": "ANXIOLYTIC", "subcategory": "5HT1A_PARTIAL_AGONIST", "atc": "N05BE01",
        "brandNames": ["BuSpar", "布斯帕"],
        "pharmacology": "5-HT1A 受体部分激动剂; 抗焦虑但无镇静/依赖/呼吸抑制, 慢起效 (1-2 周)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.04, "kaPerHour": 1.0, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [7.5, 30.0]}],
        "kePerHour": 0.13, "tHalfHours": 2.5, "tHalfRangeHours": [2.0, 4.0],
        "vdLPerKg": 5.0, "clLPerHour": 70.0, "proteinBindingPct": 95,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.95}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 95%", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "buspirone_metab", "name": "5-OH-buspirone + 1-PP", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C",
            "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "clarithromycin", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [5.0, 13.0], "severity": "MAJOR",
             "clinicalNote": "联用克拉霉素 → buspirone AUC ↑5-13×, 严重嗜睡/眩晕"},
            {"triggerDrugId": "grapefruit_juice", "mechanism": "CYP_INHIBITION_MODERATE",
             "aucFoldChange": [2.0, 4.0], "severity": "MODERATE",
             "clinicalNote": "葡萄柚汁 ↑ buspirone AUC 2-4×"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["GAD"],
        "overdose": {"symptoms": "嗜睡, 眩晕, 头痛, 恶心; 罕见严重反应, 无呼吸抑制",
            "severity": "MILD", "toxicDoseEstimateMg": 1000.0, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed BuSpar Label §10"},
        "references": ["DailyMed BuSpar Label §10"]
    },
    {
        "id": "zopiclone", "genericName": "Zopiclone", "genericNameZh": "佐匹克隆",
        "category": "ANXIOLYTIC", "subcategory": "Z_DRUG", "atc": "N05CF01",
        "brandNames": ["Imovane", "Zimovane"],
        "pharmacology": "Z 药 (cyclopyrrolone 类, 似 BZD), 作用于 BZD 受体, 治疗失眠",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.8, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [3.75, 7.5]}],
        "kePerHour": 0.13, "tHalfHours": 5.0, "tHalfRangeHours": [3.5, 6.5],
        "vdLPerKg": 1.4, "clLPerHour": 14.0, "proteinBindingPct": 45,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.6}, {"cyp": "CYP2C8", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2C8 → N-氧化 + N-去甲基", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "zopiclone_n_oxide", "name": "N-氧化佐匹克隆", "activityRatio": 0.3, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C",
            "elderly": "认知↓ / 摔倒, 3.75 mg 起始", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "alcohol", "mechanism": "ADDITIVE_CNS",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用酒精 → 显著呼吸抑制, 禁联"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["依赖性", "认知功能"]},
        "indicationGroups": ["INSOMNIA"],
        "overdose": {"symptoms": "深度镇静, 嗜睡 → 昏迷, 呼吸抑制 (联用酒精/阿片显著↑)",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 150.0, "fatalDoseEstimateMg": 1000.0,
            "management": "气道 + 机械通气; 监测; 氟马西尼效果有限 (Z 药非典型 BZD)",
            "antidote": "氟马西尼 Flumazenil (试用, 效果有限)",
            "dataSource": "DailyMed Imovane Label §10"},
        "references": ["DailyMed Imovane Label §10"]
    },

    # ANALGESIC (16 → +2)
    {
        "id": "celecoxib", "genericName": "Celecoxib", "genericNameZh": "塞来昔布",
        "category": "ANALGESIC", "subcategory": "COX2_SELECTIVE", "atc": "M01AH01",
        "brandNames": ["Celebrex", "西乐葆"],
        "pharmacology": "COX-2 选择性抑制剂, 抗炎镇痛, 胃肠道副作用↓, 但心血管风险↑ (FDA 黑框)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.7, "kaPerHour": 1.5, "tMaxHours": 2.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [100.0, 400.0]}],
        "kePerHour": 0.08, "tHalfHours": 11.0, "tHalfRangeHours": [6.0, 12.0],
        "vdLPerKg": 1.5, "clLPerHour": 15.0, "proteinBindingPct": 97,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2C9", "fraction": 0.9}], "inhibitors": [{"cyp": "CYP2D6", "strength": "WEAK"}],
            "inducers": [],
            "primaryPathway": "CYP2C9 90%", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "celecoxib_metab", "name": "羧酸代谢物", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "CONTRAINDICATED_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
            "elderly": "心血管风险↑, 最低有效剂量", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → INR↑ 出血风险; S-华法林代谢抑制"},
            {"triggerDrugId": "fluconazole", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 2.0], "severity": "MAJOR",
             "clinicalNote": "氟康唑 ↑ celecoxib AUC 2×, 减半剂量"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["肾功", "BP", "心血管症状", "GI 出血"]},
        "indicationGroups": ["PAIN", "OSTEOARTHRITIS", "RHEUMATOID_ARTHRITIS", "FAP"],
        "overdose": {
            "symptoms": "嗜睡, 恶心, 腹痛, 消化性溃疡/出血, 肾损, 高血压, MI/卒中 (长期)",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测肾功/心血管; 活性炭; PPI 预防胃黏膜",
            "antidote": None, "dataSource": "DailyMed Celebrex Label §10 (FDA 黑框 CV 风险)"
        },
        "references": ["DailyMed Celebrex Label §10 (FDA 黑框 CV)"]
    },
    {
        "id": "gabapentin", "genericName": "Gabapentin", "genericNameZh": "加巴喷丁",
        "category": "ANALGESIC", "subcategory": "GABAPENTINOID", "atc": "N02BF01",
        "brandNames": ["Neurontin", "迭力"],
        "pharmacology": "α2δ 钙通道亚基配体, 抑制神经递质释放; 抗癫痫 + 神经病理性疼痛",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.6, "kaPerHour": 1.0, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [300.0, 1800.0]}],
        "kePerHour": 0.08, "tHalfHours": 6.0, "tHalfRangeHours": [5.0, 7.0],
        "vdLPerKg": 0.85, "clLPerHour": 6.0, "proteinBindingPct": 3,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 100% 肾排 (无代谢)", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "gabapentin_metab", "name": "-", "activityRatio": 1.0, "note": "无代谢"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE",
            "elderly": "肾清除↓, 摔倒/认知↓", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "opioid", "mechanism": "ADDITIVE_CNS_RESPIRATORY",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用阿片 → 显著呼吸抑制, FDA 黑框"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["肾功", "自杀念头", "呼吸"]},
        "indicationGroups": ["NEUROPATHIC_PAIN", "SEIZURES_ADJUNCT", "PHN"],
        "overdose": {"symptoms": "复视, 构音障碍, 嗜睡, 嗜睡 → 意识抑制; 大剂量时呼吸抑制 (联用阿片/老年人)",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测呼吸; 活性炭 (早期)", "antidote": None,
            "dataSource": "DailyMed Neurontin Label §10"},
        "references": ["DailyMed Neurontin Label §10"]
    },

    # ANTIPSYCHOTIC (13 → +1)
    {
        "id": "lurasidone", "genericName": "Lurasidone", "genericNameZh": "鲁拉西酮",
        "category": "ANTIPSYCHOTIC", "subcategory": "ATYPICAL_BENZOTHIAZOLE", "atc": "N05AE05",
        "brandNames": ["Latuda", "罗舒达"],
        "pharmacology": "D2/5-HT2A 拮抗 + 5-HT7 拮抗 + 部分 5-HT1A 激动; 代谢综合征风险低",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.15, "kaPerHour": 0.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [40.0, 160.0]}],
        "kePerHour": 0.13, "tHalfHours": 18.0, "tHalfRangeHours": [12.0, 24.0],
        "vdLPerKg": 4.0, "clLPerHour": 30.0, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.95}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 95% (强 3A4 抑制剂禁忌)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "lurasidone_metab", "name": "氧化代谢物", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "MEDIUM",
            "sexual": "MEDIUM", "hyperprolactinemia": "MEDIUM"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
            "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "ketoconazole", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [3.0, 6.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "酮康唑 ↑ lurasidone AUC 3-6×, 禁联 (CYP3A4 强抑制剂)"},
            {"triggerDrugId": "rifampin", "mechanism": "CYP_INDUCTION",
             "aucFoldChange": [0.3, 0.3], "severity": "CONTRAINDICATED",
             "clinicalNote": "利福平诱导 3A4, lurasidone 暴露↓70%, 效价↓"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["体重", "血脂", "空腹血糖", "EPS", "QTc"]},
        "indicationGroups": ["SCHIZOPHRENIA", "BIPOLAR_DEPRESSION"],
        "overdose": {"symptoms": "镇静, EPS, 恶心, 焦虑; 急性过量经验有限",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护; 活性炭", "antidote": None,
            "dataSource": "DailyMed Latuda Label §10"},
        "references": ["DailyMed Latuda Label §10"]
    },

    # ANTIEPILEPTIC (11 → +1)
    {
        "id": "levetiracetam", "genericName": "Levetiracetam", "genericNameZh": "左乙拉西坦",
        "category": "ANTIEPILEPTIC", "subcategory": "SV2A_BINDER", "atc": "N03AX14",
        "brandNames": ["Keppra", "开浦兰"],
        "pharmacology": "结合 SV2A (突触囊泡蛋白 2A) 抑制突触前神经递质释放; 不经 CYP 代谢, 相互作用少",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 1.0, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [500.0, 3000.0]}],
        "kePerHour": 0.1, "tHalfHours": 7.0, "tHalfRangeHours": [6.0, 8.0],
        "vdLPerKg": 0.6, "clLPerHour": 4.0, "proteinBindingPct": 10,
        "therapeuticWindow": {"low": 12.0, "high": 46.0, "unit": "μg/mL", "guidelineSource": "AGNP 2017"},
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "水解 (血浆/组织酯酶) 70% + 原形 30% 肾排", "pathwayType": "Non-CYP + Renal"},
        "activeMetabolites": [{"id": "levetiracetam_metab", "name": "(无活性代谢物)", "activityRatio": 0.0, "note": "水解失活"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE",
            "elderly": "肾清除↓, 行为副作用↑ (易激惹/抑郁)", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "methotrexate", "mechanism": "DECREASED_CLEARANCE",
             "aucFoldChange": [1.0, 1.0], "severity": "MODERATE",
             "clinicalNote": "联用 → MTX 清除↓, 需监测 MTX 浓度"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["肾功能", "行为/情绪", "自杀念头"]},
        "indicationGroups": ["SEIZURES_PARTIAL", "SEIZURES_GENERALIZED", "JME", "STATUS_EPILEPTICUS"],
        "overdose": {"symptoms": "嗜睡, 乏力, 易激惹, 精神症状; 大剂量 → 呼吸抑制 (罕见)",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed Keppra Label §10"},
        "references": ["DailyMed Keppra Label §10"]
    },

    # ANTIPARKINSONIAN (7 → +1)
    {
        "id": "entacapone", "genericName": "Entacapone", "genericNameZh": "恩他卡朋",
        "category": "ANTIPARKINSONIAN", "subcategory": "COMT_INHIBITOR", "atc": "N04BX02",
        "brandNames": ["Comtan", "珂丹"],
        "pharmacology": "COMT 抑制剂, 抑制左旋多巴外周代谢, 延长 t½; 必联左旋多巴",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.35, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [200.0, 1600.0]}],
        "kePerHour": 0.1, "tHalfHours": 1.0, "tHalfRangeHours": [0.5, 2.0],
        "vdLPerKg": 0.4, "clLPerHour": 3.0, "proteinBindingPct": 98,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [{"cyp": "CYP2C9", "strength": "WEAK"}], "inducers": [],
            "primaryPathway": "葡糖醛酸化 50% + 非 CYP 代谢", "pathwayType": "Phase II + 非酶"},
        "activeMetabolites": [{"id": "entacapone_metab", "name": "-", "activityRatio": 1.0, "note": "Z-异构体活性> E-异构体"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "maoi", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 MAOI + 左旋多巴 → 高血压危象"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["肝功", "异动症"]},
        "indicationGroups": ["PARKINSONS_DISEASE"],
        "overdose": {"symptoms": "腹部不适, 恶心, 呕吐; 急性过量通常较轻",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed Comtan Label §10"},
        "references": ["DailyMed Comtan Label §10"]
    },

    # ANTIDIABETIC (6 → +1)
    {
        "id": "linagliptin", "genericName": "Linagliptin", "genericNameZh": "利格列汀",
        "category": "ANTIDIABETIC", "subcategory": "DPP4_INHIBITOR", "atc": "A10BH05",
        "brandNames": ["Tradjenta", "欧唐宁"],
        "pharmacology": "DPP-4 抑制剂, 延长 GLP-1/GIP 作用, 葡萄糖依赖性促胰岛素分泌; 肾/肝不全无需调剂量",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.3, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [2.5, 5.0]}],
        "kePerHour": 0.1, "tHalfHours": 12.0, "tHalfRangeHours": [10.0, 14.0],
        "vdLPerKg": 9.0, "clLPerHour": 25.0, "proteinBindingPct": 75,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}], "inhibitors": [{"cyp": "P-gp", "strength": "WEAK"}],
            "inducers": [],
            "primaryPathway": "CYP3A4 50% (少量) + 原形胆汁排 85%", "pathwayType": "Biliary + CYP"},
        "activeMetabolites": [{"id": "linagliptin_metab", "name": "无显著活性代谢物", "activityRatio": 0.1, "note": "弱"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "rifampin", "mechanism": "CYP_INDUCTION + P_GP_INDUCTION",
             "aucFoldChange": [0.5, 0.5], "severity": "MAJOR",
             "clinicalNote": "利福平 ↓ linagliptin 暴露 40%, 联用需调整"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["HbA1c", "肾功"]},
        "indicationGroups": ["T2DM"],
        "overdose": {"symptoms": "低血糖 (联用磺脲/胰岛素时), 急性胰腺炎风险; 急性过量较轻",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测血糖", "antidote": None,
            "dataSource": "DailyMed Tradjenta Label §10"},
        "references": ["DailyMed Tradjenta Label §10"]
    },

    # STIMULANT (5 → +1)
    {
        "id": "lisdexamfetamine", "genericName": "Lisdexamfetamine", "genericNameZh": "赖氨酸安非他命",
        "category": "STIMULANT", "subcategory": "AMPHETAMINE_PRODRUG", "atc": "N06BA12",
        "brandNames": ["Vyvanse", "Vyvanse"],
        "pharmacology": "安非他命 + L-赖氨酸 前药; 肠道/血液水解缓释右旋安非他命; 治疗 ADHD + 暴食症",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.97, "kaPerHour": 1.0, "tMaxHours": 3.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [20.0, 70.0]}],
        "kePerHour": 0.1, "tHalfHours": 10.0, "tHalfRangeHours": [8.0, 14.0],
        "vdLPerKg": 4.0, "clLPerHour": 25.0, "proteinBindingPct": 20,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "水解 (红细胞/肠道) → 赖氨酸 + 右旋安非他命", "pathwayType": "Non-enzymatic hydrolysis"},
        "activeMetabolites": [{"id": "d_amfetamine", "name": "右旋安非他命", "activityRatio": 1.0, "note": "水解后活性物"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW",
            "sexual": "MEDIUM", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "mao_inhibitor", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 MAOI → 高血压危象, 禁联 (停 MAOI 14d+)"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["体重", "心率", "BP", "精神症状", "滥用风险"]},
        "indicationGroups": ["ADHD", "BINGE_EATING_DISORDER"],
        "overdose": {"symptoms": "中枢兴奋: 激动, 震颤, 反射亢进, 心动过速, 高血压, 高热, 癫痫, 心律失常, 横纹肌溶解",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 100.0, "fatalDoseEstimateMg": None,
            "management": "支持; 物理降温; BZD 控癫痫 / 镇静; 监测 CK; 升压用酚妥拉明; 避免 β 阻滞剂 (单用可 ↑ α)",
            "antidote": None, "dataSource": "DailyMed Vyvanse Label §10 (Schedule II 管制)"},
        "references": ["DailyMed Vyvanse Label §10 (Schedule II)"]
    },

    # CORTICOSTEROID (5 → +1)
    {
        "id": "budesonide", "genericName": "Budesonide", "genericNameZh": "布地奈德",
        "category": "CORTICOSTEROID", "subcategory": "INHALED", "atc": "R03BA02",
        "brandNames": ["Pulmicort", "普米克"],
        "pharmacology": "吸入糖皮质激素 (高首过代谢, 全身副作用↓); 治疗哮喘/COPD/鼻炎/Crohn",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.1, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["μg"], "commonDoseRangeMg": [200.0, 800.0]}],
        "kePerHour": 0.5, "tHalfHours": 2.0, "tHalfRangeHours": [1.5, 2.5],
        "vdLPerKg": 3.0, "clLPerHour": 80.0, "proteinBindingPct": 90,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.9}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 90% (高首过, 全身生物利用度低)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "budesonide_metab", "name": "16α-羟基泼尼松龙", "activityRatio": 0.01, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "骨质疏松监测",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "ketoconazole", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [4.0, 4.0], "severity": "MAJOR",
             "clinicalNote": "酮康唑 ↑ budesonide 全身暴露 4×, 增加肾上腺抑制"}
        ],
        "monitoring": {"frequency": "EVERY_6_12_MONTHS", "items": ["口腔念珠菌", "声嘶", "骨密度 (长期高剂量)"]},
        "indicationGroups": ["ASTHMA", "COPD", "ALLERGIC_RHINITIS", "CROHNS_DISEASE"],
        "overdose": {"symptoms": "急性过量较轻; 长期大剂量 → 库欣样, 肾上腺抑制, 骨质疏松",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 长期用不可骤停", "antidote": None,
            "dataSource": "DailyMed Pulmicort Label §10"},
        "references": ["DailyMed Pulmicort Label §10"]
    },

    # ANTIHYPERTENSIVE (4 → +1)
    {
        "id": "hydrochlorothiazide", "genericName": "Hydrochlorothiazide", "genericNameZh": "氢氯噻嗪",
        "category": "ANTIHYPERTENSIVE", "subcategory": "THIAZIDE_DIURETIC", "atc": "C03AA03",
        "brandNames": ["Microzide", "双氢克尿噻"],
        "pharmacology": "噻嗪类利尿剂, 抑制远曲小管 Na+/Cl- 共转运; 抗高血压 + 治水肿",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.7, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [12.5, 50.0]}],
        "kePerHour": 0.13, "tHalfHours": 6.0, "tHalfRangeHours": [5.0, 15.0],
        "vdLPerKg": 1.5, "clLPerHour": 20.0, "proteinBindingPct": 40,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 95% 肾排 (不代谢)", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "hctz_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "CONTRAINDICATED_EGFR_30", "hepatic": "NONE", "elderly": "电解质紊乱 / 脱水",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "lithium", "mechanism": "DECREASED_CLEARANCE",
             "aucFoldChange": [1.5, 1.5], "severity": "MAJOR",
             "clinicalNote": "噻嗪类 ↓ Li 清除 50%, 锂毒性风险↑, 需监测血锂"},
            {"triggerDrugId": "digoxin", "mechanism": "ELECTROLYTE",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "低钾 → 洋地黄毒性↑"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["电解质 (Na/K/Mg)", "尿酸", "血糖", "肾功"]},
        "indicationGroups": ["HYPERTENSION", "EDEMA", "HEART_FAILURE", "NEPHROGENIC_DI"],
        "overdose": {"symptoms": "低血压, 电解质紊乱 (低 Na/K/Mg), 脱水, 嗜睡, 肌痛",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; IV 液体; 纠正电解质; 监测", "antidote": None,
            "dataSource": "DailyMed Microzide Label §10"},
        "references": ["DailyMed Microzide Label §10"]
    },

    # THYROID (3 → +1)
    {
        "id": "liothyronine", "genericName": "Liothyronine", "genericNameZh": "碘塞罗宁 (T3)",
        "category": "THYROID", "subcategory": "T3_HORMONE", "atc": "H03AA02",
        "brandNames": ["Cytomel"],
        "pharmacology": "T3 (活性甲状腺激素), 比 T4 起效快, 半衰期短; 甲减替代 + TSH 抑制",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.95, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["μg"], "commonDoseRangeMg": [5.0, 50.0]}],
        "kePerHour": 0.4, "tHalfHours": 1.5, "tHalfRangeHours": [1.0, 2.0],
        "vdLPerKg": 0.5, "clLPerHour": 4.0, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "脱碘 (T4↔T3) + 葡糖醛酸化结合", "pathwayType": "Phase II + deiodinase"},
        "activeMetabolites": [{"id": "liothyronine_metab", "name": "T2 (二碘甲腺原氨酸)", "activityRatio": 0.01, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "NONE", "elderly": "低剂量起始, 心血管风险", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "T3 ↑ 凝血因子代谢, 联用 → 抗凝需求↑, 监测 INR"}
        ],
        "monitoring": {"frequency": "EVERY_6_8_WEEKS", "items": ["TSH", "FT3/FT4", "心率", "骨密度"]},
        "indicationGroups": ["HYPOTHYROIDISM", "TSH_SUPPRESSION", "MYXEDEMA_COMA"],
        "overdose": {"symptoms": "甲亢危象: 心动过速, 高热, 震颤, 谵妄, 心律失常, 脱水, 休克",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 退热 (物理 + 退热药, 不用阿司匹林); β 阻滞剂; 糖皮质激素; 必要时血浆置换",
            "antidote": None, "dataSource": "DailyMed Cytomel Label §10"},
        "references": ["DailyMed Cytomel Label §10"]
    },

    # BPH_AGENT (3 → +1)
    {
        "id": "silodosin", "genericName": "Silodosin", "genericNameZh": "西罗多辛",
        "category": "BPH_AGENT", "subcategory": "ALPHA1A_SELECTIVE", "atc": "G04CA04",
        "brandNames": ["Rapaflo", "优利福"],
        "pharmacology": "α1A 肾上腺素能受体高度选择性拮抗剂 (前列腺/膀胱颈/尿道); 治 BPH",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.32, "kaPerHour": 1.0, "tMaxHours": 2.6,
            "doseUnits": ["mg"], "commonDoseRangeMg": [4.0, 8.0]}],
        "kePerHour": 0.13, "tHalfHours": 13.0, "tHalfRangeHours": [10.0, 18.0],
        "vdLPerKg": 1.5, "clLPerHour": 10.0, "proteinBindingPct": 97,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.4}, {"cyp": "CYP2D6", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2D6 + 葡糖醛酸化", "pathwayType": "CYP450 + Phase II"},
        "activeMetabolites": [{"id": "silodosin_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "MEDIUM", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "pde5_inhibitor", "mechanism": "ADDITIVE_HYPOTENSION",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 PDE5 抑制剂 (他达拉非/西地那非) → 显著低血压, 禁联"},
            {"triggerDrugId": "clarithromycin", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 3.0], "severity": "MAJOR",
             "clinicalNote": "联用 → silodosin AUC ↑2-3×, 低血压风险"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["BP"]},
        "indicationGroups": ["BPH"],
        "overdose": {"symptoms": "严重体位性低血压, 反射性心动过速, 头晕, 晕厥",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 卧位; IV 液体; 必要时升压", "antidote": None,
            "dataSource": "DailyMed Rapaflo Label §10"},
        "references": ["DailyMed Rapaflo Label §10"]
    },

    # HORMONE_REPLACEMENT (3 → +1)
    {
        "id": "testosterone", "genericName": "Testosterone", "genericNameZh": "睾酮",
        "category": "HORMONE_REPLACEMENT", "subcategory": "ANDROGEN", "atc": "G03BA03",
        "brandNames": ["AndroGel", "Testim", "Testogel"],
        "pharmacology": "雄激素替代治疗, 凝胶/贴片/IM 多种剂型; 治男性性腺功能减退",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.05, "kaPerHour": 1.0, "tMaxHours": 4.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [40.0, 200.0]}],
        "kePerHour": 0.13, "tHalfHours": 2.5, "tHalfRangeHours": [1.5, 5.0],
        "vdLPerKg": 1.0, "clLPerHour": 12.0, "proteinBindingPct": 98,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 代谢 (5α 还原 → DHT; aromatase → 雌二醇)", "pathwayType": "CYP + enzyme"},
        "activeMetabolites": [
            {"id": "dht", "name": "双氢睾酮 (DHT)", "activityRatio": 3.0,
                "note": "5α-还原酶; AR 亲和力 2-3× 睾酮"},
            {"id": "estradiol_t", "name": "雌二醇", "activityRatio": 0.0,
                "note": "CYP19 (aromatase); 男性乳腺增生副作用"}
        ],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C",
            "elderly": "心血管风险↑, PSA 监测", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "雄激素 ↑ 抗凝效应, INR 监测"},
            {"triggerDrugId": "finasteride", "mechanism": "PHARMACODYNAMIC_ANTAGONISM",
             "aucFoldChange": [1.0, 1.0], "severity": "MODERATE",
             "clinicalNote": "非那雄胺 5α 还原酶抑制, ↓ DHT 形成"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["睾酮血药", "PSA", "Hct", "肝功", "骨密度"]},
        "indicationGroups": ["HYPOGONADISM", "GENDER_AFFIRMING_HRT_FTM", "DELAYED_PUBERTY"],
        "overdose": {"symptoms": "急性过量较轻; 长期大剂量 → 红细胞↑ (Hct↑), 痤疮, 男性乳腺增生, 攻击行为, 肝损",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测 Hct; 长期大剂量需减量", "antidote": None,
            "dataSource": "DailyMed AndroGel Label §10 (FDA 黑框 VTE/MI/卒中)"},
        "references": ["DailyMed AndroGel Label §10 (FDA 黑框)"]
    },

    # ANTIBIOTIC (3 → +2)
    {
        "id": "acyclovir", "genericName": "Acyclovir", "genericNameZh": "阿昔洛韦",
        "category": "ANTIBIOTIC", "subcategory": "ANTIVIRAL_HSV", "atc": "J05AB01",
        "brandNames": ["Zovirax", "克毒星"],
        "pharmacology": "核苷类似物, 病毒胸苷激酶磷酸化, 抑制 HSV/VZV DNA 聚合酶",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.2, "kaPerHour": 1.0, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [200.0, 800.0]}],
        "kePerHour": 0.27, "tHalfHours": 2.5, "tHalfRangeHours": [2.0, 4.0],
        "vdLPerKg": 0.8, "clLPerHour": 15.0, "proteinBindingPct": 15,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 80% 肾排 (肾小管分泌) + 少量代谢", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "acv_metab", "name": "9-carboxymethoxymethylguanine", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_25", "hepatic": "NONE", "elderly": "肾清除↓, 监测",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "probenecid", "mechanism": "TUBULAR_SECRETION_INHIBITION",
             "aucFoldChange": [1.4, 1.4], "severity": "MODERATE",
             "clinicalNote": "丙磺舒抑制肾小管分泌, acyclovir AUC ↑40%"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["肾功", "神经症状"]},
        "indicationGroups": ["HSV", "VZV", "EBV", "CMV_PROPHYLAXIS"],
        "overdose": {"symptoms": "肾小管结晶 (大剂量 IV) → 急性肾衰, 神经毒性 (嗜睡, 幻觉, 癫痫, 昏迷)",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 水化 (防结晶); 血液透析 (高效清除); 监测肾功",
            "antidote": "血液透析 (高效清除 acyclovir, 严重中毒首选)",
            "dataSource": "DailyMed Zovirax Label §10"},
        "references": ["DailyMed Zovirax Label §10"]
    },
    {
        "id": "valacyclovir", "genericName": "Valacyclovir", "genericNameZh": "伐昔洛韦",
        "category": "ANTIBIOTIC", "subcategory": "ANTIVIRAL_HSV", "atc": "J05AB11",
        "brandNames": ["Valtrex", "维德思"],
        "pharmacology": "阿昔洛韦 L-缬氨酸酯前药; 肠道/肝首过水解为 acyclovir, 口服生物利用度 ↑3-5×",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.55, "kaPerHour": 1.0, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [500.0, 1000.0]}],
        "kePerHour": 0.27, "tHalfHours": 2.5, "tHalfRangeHours": [2.0, 4.0],
        "vdLPerKg": 0.8, "clLPerHour": 15.0, "proteinBindingPct": 15,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "水解为 acyclovir (活性); acyclovir 肾排", "pathwayType": "Hydrolysis + Renal"},
        "activeMetabolites": [{"id": "acv_val", "name": "阿昔洛韦", "activityRatio": 1.0, "note": "水解后活性等同"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE", "elderly": "肾清除↓, 监测",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "probenecid", "mechanism": "TUBULAR_SECRETION_INHIBITION",
             "aucFoldChange": [1.4, 1.4], "severity": "MODERATE", "clinicalNote": "丙磺舒 ↓ acyclovir 肾清除"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["肾功"]},
        "indicationGroups": ["HSV", "VZV", "CMV_PROPHYLAXIS"],
        "overdose": {"symptoms": "同 acyclovir: 肾小管结晶, 急性肾衰, 神经毒性",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 水化; 血液透析", "antidote": "血液透析",
            "dataSource": "DailyMed Valtrex Label §10"},
        "references": ["DailyMed Valtrex Label §10"]
    },

    # SUPPLEMENT (3 → +1)
    {
        "id": "vitamin_d3", "genericName": "Cholecalciferol", "genericNameZh": "维生素 D3",
        "category": "SUPPLEMENT", "subcategory": "VITAMIN", "atc": "A11CC05",
        "brandNames": ["D3", "D Drops"],
        "pharmacology": "维生素 D3, 皮肤/饮食摄取; 肝 25-羟化 + 肾 1α-羟化 → 骨化三醇 (活性); 调节 Ca/P 代谢",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.6, "kaPerHour": 1.0, "tMaxHours": 8.0,
            "doseUnits": ["IU"], "commonDoseRangeMg": [1000.0, 5000.0]}],
        "kePerHour": 0.001, "tHalfHours": 600.0, "tHalfRangeHours": [300.0, 1000.0],
        "vdLPerKg": 0.5, "clLPerHour": 0.05, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "25-羟化 (CYP2R1/CYP27A1) → 1α-羟化 (CYP27B1) → 骨化三醇", "pathwayType": "Activation"},
        "activeMetabolites": [{"id": "calcitriol", "name": "骨化三醇 (1,25-(OH)2-D3)", "activityRatio": 100.0,
            "note": "活性形式, 100x 强于 D3"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION_EGFR_30", "hepatic": "USE_CAUTION", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "thiazide", "mechanism": "ADDITIVE_HYPERCALCEMIA",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用噻嗪类利尿剂 → 高钙血症风险↑"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["25-OH-D", "Ca++", "PO4"]},
        "indicationGroups": ["VITAMIN_D_DEFICIENCY", "OSTEOPOROSIS", "RICKETS", "PSORIASIS"],
        "overdose": {"symptoms": "高钙血症: 多尿, 烦渴, 便秘, 恶心, 肾钙质沉着, 肾石, 精神症状, 心律失常",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "停药; 静脉水化 + 生理盐水; 呋塞米促 Ca 排; 降钙素/双膦酸盐; 严重者血液透析",
            "antidote": None, "dataSource": "DailyMed D3 Label §10"},
        "references": ["DailyMed D3 Label §10"]
    },

    # ANESTHETIC (3 → +1)
    {
        "id": "propofol", "genericName": "Propofol", "genericNameZh": "丙泊酚",
        "category": "ANESTHETIC", "subcategory": "IV_GENERAL", "atc": "N01AX10",
        "brandNames": ["Diprivan", "得普利麻"],
        "pharmacology": "IV 短效全麻; 增强 GABA-A 受体; 诱导 + 维持麻醉 + ICU 镇静",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.0, "kaPerHour": 0.0, "tMaxHours": 0.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [100.0, 200.0]}],
        "kePerHour": 0.5, "tHalfHours": 1.5, "tHalfRangeHours": [1.0, 3.0],
        "vdLPerKg": 4.0, "clLPerHour": 120.0, "proteinBindingPct": 98,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2B6", "fraction": 0.4}], "inhibitors": [], "inducers": [],
            "primaryPathway": "肝 CYP2B6 + 葡糖醛酸化 4-OH 代谢", "pathwayType": "CYP450 + Phase II"},
        "activeMetabolites": [{"id": "propofol_metab", "name": "2,6-diisopropyl-1,4-quinol (无活性)", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_DOSE_CHILD_PUGH_B", "elderly": "低剂量, 心血管抑制敏感",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "opioid", "mechanism": "ADDITIVE_CNS_RESPIRATORY",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用阿片 → 显著呼吸抑制, 减少 propofol 剂量"}
        ],
        "monitoring": {"frequency": "CONTINUOUS_DURING_INFUSION", "items": ["BP", "SpO2", "ETCO2", "PRIS 监测 (长期)"]},
        "indicationGroups": ["GENERAL_ANESTHESIA", "SEDATION_ICU", "SEDATION_PROCEDURAL"],
        "overdose": {"symptoms": "深度呼吸抑制, 心血管抑制 (血管扩张 + 心肌抑制 → 严重低血压); 丙泊酚输注综合征 PRIS (长时间/大剂量 ICU 用)",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "气道 + 机械通气; 静脉补液 + 升压; PRIS → 停药 + 血液透析 + 纠正代谢性酸中毒",
            "antidote": "无特异性; PRIS → 血液透析 + 对症",
            "dataSource": "DailyMed Diprivan Label §10; Crit Care 2015 PRIS"},
        "references": ["DailyMed Diprivan Label §10"]
    },

    # ALZHEIMERS (4 → +1)
    {
        "id": "memantine_er", "genericName": "Memantine ER", "genericNameZh": "美金刚缓释",
        "category": "ALZHEIMERS", "subcategory": "NMDA_ANTAG_ER", "atc": "N06DX01",
        "brandNames": ["Namenda XR", "美金刚 ER"],
        "pharmacology": "NMDA 受体低亲和力拮抗剂; 治疗中重度阿尔茨海默; 缓释剂型 1 日 1 次",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.99, "kaPerHour": 1.0, "tMaxHours": 9.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [7.0, 28.0]}],
        "kePerHour": 0.04, "tHalfHours": 60.0, "tHalfRangeHours": [40.0, 80.0],
        "vdLPerKg": 9.0, "clLPerHour": 8.0, "proteinBindingPct": 45,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.3}], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 50-60% 肾排 + 部分 CYP2D6/3A4", "pathwayType": "Renal + minor CYP"},
        "activeMetabolites": [{"id": "memantine_metab", "name": "无活性代谢物", "activityRatio": 0.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "amantadine", "mechanism": "PHARMACODYNAMIC_SYNERGY",
             "aucFoldChange": [1.0, 1.0], "severity": "MODERATE",
             "clinicalNote": "两药皆 NMDA 拮抗, 联用增加 CNS 副作用"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["认知功能 MMSE", "肾功"]},
        "indicationGroups": ["ALZHEIMERS_DISEASE"],
        "overdose": {"symptoms": "激越, 幻觉, 意识混乱, 心动过速, 高血压, 瞳孔散大; 相对较轻",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测生命体征; 酸化尿液 (加速排泄, 经验性)",
            "antidote": None, "dataSource": "DailyMed Namenda XR Label §10"},
        "references": ["DailyMed Namenda XR Label §10"]
    },

    # MOOD_STABILIZER (3 → +1)
    {
        "id": "carbamazepine", "genericName": "Carbamazepine", "genericNameZh": "卡马西平",
        "category": "MOOD_STABILIZER", "subcategory": "NA_CHANNEL", "atc": "N03AF01",
        "brandNames": ["Tegretol", "得理多"],
        "pharmacology": "钠通道阻滞 + GABA 增强; 强 CYP3A4 诱导剂 (自身诱导 + 多种相互作用); 抗癫痫 + 双向 + 三叉神经痛",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.85, "kaPerHour": 1.5, "tMaxHours": 4.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [200.0, 1200.0]}],
        "kePerHour": 0.05, "tHalfHours": 14.0, "tHalfRangeHours": [10.0, 20.0],
        "vdLPerKg": 1.0, "clLPerHour": 8.0, "proteinBindingPct": 75,
        "therapeuticWindow": {"low": 4.0, "high": 12.0, "unit": "μg/mL", "guidelineSource": "AGNP 2017"},
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.95}],
            "inhibitors": [], "inducers": [{"cyp": "CYP3A4", "strength": "STRONG"}, {"cyp": "CYP1A2", "strength": "STRONG"}],
            "primaryPathway": "CYP3A4 → 10,11-环氧化物 (活性, CBZ-E)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "cbz_epoxide", "name": "卡马西平-10,11-环氧化物 (CBZ-E)", "activityRatio": 0.5,
            "note": "CYP3A4; 活性等同, 蓄积可致神经毒性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "MEDIUM", "extrapyramidal": "LOW", "sedation": "MEDIUM",
            "sexual": "MEDIUM", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "低剂量起始",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "CYP_INDUCTION_STRONG",
             "aucFoldChange": [0.4, 0.4], "severity": "MAJOR",
             "clinicalNote": "CBZ 强诱导 3A4 → 抗凝↓ 50%, 需↑华法林剂量, 监测 INR"},
            {"triggerDrugId": "diltiazem", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [0.4, 0.4], "severity": "MAJOR",
             "clinicalNote": "地尔硫卓 ↓ CBZ 代谢 → CBZ 毒性"}
        ],
        "monitoring": {"frequency": "EVERY_2_4_WEEKS_DURING_TITRATION", "items": ["CBZ 血药", "CBC", "肝功", "Na+", "眼科检查"]},
        "indicationGroups": ["SEIZURES_PARTIAL", "BIPOLAR_ACUTE", "TRIGEMINAL_NEURALGIA"],
        "overdose": {"symptoms": "严重 CNS 抑制, 嗜睡 → 昏迷, 共济失调, 癫痫 (反常), 呼吸抑制, 心动过速, 低血压, 抗利尿激素分泌异常 SIADH",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 气道; 控癫痫 (BZD); 监测 Na+ (SIADH); 活性炭; 血液透析效果有限",
            "antidote": None, "dataSource": "DailyMed Tegretol Label §10"},
        "references": ["DailyMed Tegretol Label §10 (FDA 黑框 APL)"]
    },

    # SUBSTANCE_USE (3 → +1)
    {
        "id": "methadone", "genericName": "Methadone", "genericNameZh": "美沙酮",
        "category": "SUBSTANCE_USE", "subcategory": "OPIOID_AGONIST_MAINTENANCE", "atc": "N07BC02",
        "brandNames": ["Dolophine", "Methadose"],
        "pharmacology": "μ 阿片受体激动剂 + NMDA 拮抗 + 5-HT 再摄取抑制; 镇痛 + 阿片成瘾维持治疗",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.7, "kaPerHour": 1.0, "tMaxHours": 2.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [20.0, 120.0]}],
        "kePerHour": 0.03, "tHalfHours": 24.0, "tHalfRangeHours": [15.0, 60.0],
        "vdLPerKg": 4.0, "clLPerHour": 8.0, "proteinBindingPct": 80,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2B6", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2B6 + CYP2D6 多途径 N-去甲基", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "eddp", "name": "2-ethylidene-1,5-dimethyl-3,3-diphenylpyrrolidine (EDDP)", "activityRatio": 0.0,
            "note": "无活性, 主要尿代谢物"}],
        "adverseEffects": {"qtcProlongation": "HIGH", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
            "elderly": "低剂量起始, 长 t½ 蓄积", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "citalopram", "mechanism": "PHARMACODYNAMIC_QT",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "两药皆 QT 延长, 联用 → 尖端扭转型室速风险, 监测 ECG"},
            {"triggerDrugId": "benzodiazepine", "mechanism": "ADDITIVE_RESPIRATORY",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "BZD + methadone → 致死性呼吸抑制风险↑, 联用谨慎, FDA 黑框"}
        ],
        "monitoring": {"frequency": "WEEKLY_FIRST_MONTH_THEN_MONTHLY", "items": ["ECG (QTc)", "methadone 血药", "滥用 / 转移"]},
        "indicationGroups": ["OPIOID_USE_DISORDER", "PAIN_CHRONIC"],
        "overdose": {"symptoms": "呼吸抑制 (主因, 长 t½ 蓄积), 瞳孔缩小, 意识抑制, 低血压, QT 延长 → 尖端扭转型室速",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "气道 + 机械通气; 纳洛酮 (半衰期 30-90min 短于 methadone, 需重复/输注); 监测 ECG (QTc)",
            "antidote": "纳洛酮 Naloxone (但 methadone t½ 24-60h 长, 需持续输注)",
            "dataSource": "DailyMed Dolophine Label §10; FDA 2006 methadone 黑框"},
        "references": ["DailyMed Dolophine Label §10 (FDA 黑框 QT/呼吸)"]
    },
]

added = 0
for spec in B2_DRUGS:
    drug_id = spec["id"]
    if drug_id in existing_ids:
        print(f"SKIP: {drug_id} already exists")
        continue
    template = new_template()
    template.update(spec)
    drugs.append(template)
    existing_ids.add(drug_id)
    added += 1
print(f"\nAdded {added} drugs (B2)")

# Update version metadata
data["schemaVersion"] = "0.7.0"
data["generatedAt"] = "2026-09-07T15:50:00Z"

# Final category stats
from collections import Counter
cats = Counter(d["category"] for d in drugs)
print(f"\nFinal total: {len(drugs)} drugs, {len(cats)} categories")
print("\nCategory distribution:")
for cat, cnt in sorted(cats.items(), key=lambda x: -x[1]):
    flag = " ⚠️<3" if cnt < 3 else ""
    print(f"  {cat}: {cnt}{flag}")

# Save
data["drugs"] = drugs
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"\nSaved with {len(drugs)} drugs total, size {V06.stat().st_size} bytes")
