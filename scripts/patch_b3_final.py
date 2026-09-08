"""B3: 再加 15+ 药到 200+ (补 OTC / 常见药 / 抗感染 / 维生素)"""
import json
from pathlib import Path
from collections import Counter

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


B3_DRUGS = [
    # ANALGESIC +1
    {
        "id": "ibuprofen", "genericName": "Ibuprofen", "genericNameZh": "布洛芬",
        "category": "ANALGESIC", "subcategory": "NSAID_PROPIONIC", "atc": "M01AE01",
        "brandNames": ["Advil", "Motrin", "美林"],
        "pharmacology": "非选择性 COX 抑制剂 (NSAID), 抗炎镇痛退热; 比 aspirin 胃刺激小",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.85, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [200.0, 800.0]}],
        "kePerHour": 0.2, "tHalfHours": 2.0, "tHalfRangeHours": [1.5, 3.0],
        "vdLPerKg": 0.2, "clLPerHour": 4.0, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2C9", "fraction": 0.7}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2C9 70% 氧化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "ibuprofen_metab", "name": "羟基布洛芬", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION", "hepatic": "USE_CAUTION", "elderly": "GI 出血/肾损风险↑",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 出血风险↑, 阿司匹林 + ibuprofen 联用也降氯吡格雷效果"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["GI 症状", "肾功", "BP"]},
        "indicationGroups": ["PAIN", "FEVER", "DYSMENORRHEA", "OSTEOARTHRITIS", "RHEUMATOID_ARTHRITIS"],
        "overdose": {"symptoms": "恶心呕吐, 上腹痛, 嗜睡; 严重 → 肾衰, 代谢性酸中毒, 癫痫, 昏迷",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 活性炭; 监测肾功; 控癫痫 (BZD)",
            "antidote": None, "dataSource": "DailyMed Advil Label §10"},
        "references": ["DailyMed Advil Label §10"]
    },
    {
        "id": "naproxen", "genericName": "Naproxen", "genericNameZh": "萘普生",
        "category": "ANALGESIC", "subcategory": "NSAID_PROPIONIC", "atc": "M01AE02",
        "brandNames": ["Aleve", "Naprosyn"],
        "pharmacology": "NSAID (丙酸类), 长 t½ 适合 BID 服药; 心血管风险比 coxib 小但比 ibuprofen 大",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.95, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [250.0, 500.0]}],
        "kePerHour": 0.05, "tHalfHours": 12.0, "tHalfRangeHours": [10.0, 17.0],
        "vdLPerKg": 0.2, "clLPerHour": 0.5, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP1A2", "fraction": 0.3}, {"cyp": "CYP2C9", "fraction": 0.7}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2C9 70% + CYP1A2 30%", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "naproxen_metab", "name": "6-O-desmethylnaproxen", "activityRatio": 0.05, "note": "无显著活性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "USE_CAUTION", "elderly": "GI 出血/心血管风险",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 出血风险↑"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["GI 症状", "肾功", "BP"]},
        "indicationGroups": ["PAIN", "DYSMENORRHEA", "OSTEOARTHRITIS", "RHEUMATOID_ARTHRITIS", "GOUT_ACUTE"],
        "overdose": {"symptoms": "嗜睡, 上腹痛, 恶心呕吐; 严重 → 肾衰, 代谢性酸中毒, 癫痫",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 活性炭; 监测", "antidote": None,
            "dataSource": "DailyMed Aleve Label §10"},
        "references": ["DailyMed Aleve Label §10"]
    },

    # STATIN +1
    {
        "id": "fluvastatin", "genericName": "Fluvastatin", "genericNameZh": "氟伐他汀",
        "category": "STATIN", "subcategory": "HMG_COA_RED", "atc": "C10AA04",
        "brandNames": ["Lescol", "来适可"],
        "pharmacology": "HMG-CoA 还原酶抑制剂; 合成他汀, 主要 CYP2C9 代谢 (与其他 3A4 他汀相互作用少)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.3, "kaPerHour": 1.0, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [20.0, 80.0]}],
        "kePerHour": 0.2, "tHalfHours": 1.5, "tHalfRangeHours": [0.5, 3.0],
        "vdLPerKg": 0.4, "clLPerHour": 25.0, "proteinBindingPct": 98,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2C9", "fraction": 0.9}], "inhibitors": [{"cyp": "CYP2C9", "strength": "WEAK"}],
            "inducers": [],
            "primaryPathway": "CYP2C9 90%", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "fluvastatin_metab", "name": "氧化代谢物", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION_EGFR_30", "hepatic": "CONTRAINDICATED_CHILD_PUGH_C",
            "elderly": "-", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ALT/AST", "CK"]},
        "indicationGroups": ["HYPERLIPIDEMIA"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Lescol Label §10"]
    },

    # ANTIBIOTIC +2
    {
        "id": "amoxicillin", "genericName": "Amoxicillin", "genericNameZh": "阿莫西林",
        "category": "ANTIBIOTIC", "subcategory": "PENICILLIN", "atc": "J01CA04",
        "brandNames": ["Amoxil", "阿莫仙"],
        "pharmacology": "氨基青霉素, 抑制细菌细胞壁合成; 治疗 G+ / 部分 G- / 幽门螺杆菌",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.9, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [250.0, 875.0]}],
        "kePerHour": 0.25, "tHalfHours": 1.0, "tHalfRangeHours": [0.7, 1.5],
        "vdLPerKg": 0.3, "clLPerHour": 12.0, "proteinBindingPct": 20,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 60% 肾排 (肾小管分泌)", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "amox_acid", "name": "penicilloic acid (无活性)", "activityRatio": 0.0, "note": "水解代谢物"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "methotrexate", "mechanism": "TUBULAR_SECRETION_INHIBITION",
             "aucFoldChange": [1.5, 1.5], "severity": "MAJOR",
             "clinicalNote": "青霉素 ↓ MTX 肾清除, MTX 毒性↑"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["过敏反应", "肾功"]},
        "indicationGroups": ["STREP_PHARYNGITIS", "OTITIS_MEDIA", "PNEUMONIA", "UTI", "H_PYLORI_ERADICATION", "ENDOCARDITIS_PROPHY"],
        "overdose": {"symptoms": "恶心呕吐, 腹泻; 大剂量 → 肾衰, 神经毒性; 青霉素过敏者严重过敏",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 水化; 监测肾功; 过敏: 肾上腺素 + 抗组胺 + 糖皮质激素",
            "antidote": None, "dataSource": "DailyMed Amoxil Label §10"},
        "references": ["DailyMed Amoxil Label §10"]
    },
    {
        "id": "doxycycline", "genericName": "Doxycycline", "genericNameZh": "多西环素",
        "category": "ANTIBIOTIC", "subcategory": "TETRACYCLINE", "atc": "J01AA02",
        "brandNames": ["Vibramycin", "Doryx", "强力霉素"],
        "pharmacology": "四环素类抑菌, 抑制 30S 核糖体蛋白合成; 广谱 (G+ / G- / 衣原体 / 立克次体 / 疟原虫)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 1.0, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [50.0, 200.0]}],
        "kePerHour": 0.05, "tHalfHours": 16.0, "tHalfRangeHours": [14.0, 22.0],
        "vdLPerKg": 0.7, "clLPerHour": 3.0, "proteinBindingPct": 90,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 40% 肾排 + 胆汁排 + 肝代谢", "pathwayType": "Renal + biliary"},
        "activeMetabolites": [{"id": "doxy_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → INR↑ 出血风险"},
            {"triggerDrugId": "calcium_iron", "mechanism": "CHELATION",
             "aucFoldChange": [0.5, 0.5], "severity": "MAJOR",
             "clinicalNote": "二价/三价阳离子 (Ca/Fe/Mg/Al) 与多西环素螯合, 吸收↓50%, 间隔 2-3h 服"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["光敏反应", "肝功"]},
        "indicationGroups": ["ACNE", "LYME_DISEASE", "CHLAMYDIA", "MALARIA_PROPHYLAXIS", "Pneumonia_AtYPICAL", "RICKETTSIA"],
        "overdose": {"symptoms": "恶心呕吐, 上腹痛; 大剂量 → 肝毒性, 胰腺炎; 儿童牙齿黄染 (< 8 岁)",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测; 不可催吐 (食管刺激)", "antidote": None,
            "dataSource": "DailyMed Vibramycin Label §10"},
        "references": ["DailyMed Vibramycin Label §10"]
    },

    # ANTIARRHYTHMIC +1
    {
        "id": "propafenone", "genericName": "Propafenone", "genericNameZh": "普罗帕酮",
        "category": "ANTIARRHYTHMIC", "subcategory": "IC_CLASS", "atc": "C01BC03",
        "brandNames": ["Rythmol", "心律平"],
        "pharmacology": "IC 类钠通道阻滞 + 弱 β 阻滞; 治疗室上性/室性心律失常",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.3, "kaPerHour": 1.5, "tMaxHours": 3.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [150.0, 600.0]}],
        "kePerHour": 0.07, "tHalfHours": 6.0, "tHalfRangeHours": [3.0, 16.0],
        "vdLPerKg": 3.0, "clLPerHour": 50.0, "proteinBindingPct": 95,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.95}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2D6 95% (慢代谢者 t½ 17h, 快代谢者 6h)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "5_oh_propafenone", "name": "5-OH-普罗帕酮", "activityRatio": 1.0, "note": "CYP2D6; 活性等同, 慢代谢者此代谢物少"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "低剂量",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "amiodarone", "mechanism": "ADDITIVE_CARDIAC",
             "aucFoldChange": [1.5, 2.0], "severity": "MAJOR",
             "clinicalNote": "联用显著心脏传导抑制"},
            {"triggerDrugId": "digoxin", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [1.5, 1.5], "severity": "MAJOR",
             "clinicalNote": "propafenone ↑ digoxin 血药 50-85%"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["ECG", "QRS 宽度", "PR 间期"]},
        "indicationGroups": ["VENTRICULAR_ARRHYTHMIA", "AF", "SV_TACHYCARDIA"],
        "overdose": {"symptoms": "QRS 增宽, 严重心动过缓, 低血压, 室速, 晕厥, 心脏停搏; CNS 抑制, 癫痫",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 2000.0, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护; QRS 宽给 NaHCO3; 临时起搏",
            "antidote": "碳酸氢钠 NaHCO3 (QRS 增宽时, 钠通道阻滞机制)",
            "dataSource": "DailyMed Rythmol Label §10"},
        "references": ["DailyMed Rythmol Label §10"]
    },

    # SUPPLEMENT +2
    {
        "id": "cyanocobalamin", "genericName": "Cyanocobalamin", "genericNameZh": "维生素 B12",
        "category": "SUPPLEMENT", "subcategory": "VITAMIN", "atc": "B03BA01",
        "brandNames": ["B12", "钴胺素"],
        "pharmacology": "水溶性 B 族维生素, 含钴; DNA 合成 + 神经髓鞘维护 + 红细胞生成",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.01, "kaPerHour": 1.0, "tMaxHours": 8.0,
            "doseUnits": ["μg"], "commonDoseRangeMg": [1000.0, 5000.0]}],
        "kePerHour": 0.001, "tHalfHours": 480.0, "tHalfRangeHours": [200.0, 1000.0],
        "vdLPerKg": 0.5, "clLPerHour": 0.2, "proteinBindingPct": 90,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "肝代谢 (甲基化) + 肾原形排", "pathwayType": "Hepatic + renal"},
        "activeMetabolites": [{"id": "methylcobalamin", "name": "甲基钴胺素 (活性形式)", "activityRatio": 1.0,
            "note": "内源性活性形式"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "NONE", "elderly": "吸收↓, 萎缩性胃炎", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["B12 血药", "MCV", "Hct"]},
        "indicationGroups": ["B12_DEFICIENCY", "PERNICIOUS_ANEMIA", "NEUROPATHY"],
        "overdose": {"symptoms": "急性毒性极低 (水溶性, 尿排); 罕见过敏 (注射剂)",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持", "antidote": None, "dataSource": "DailyMed B12 Label §10"},
        "references": ["DailyMed B12 Label §10"]
    },
    {
        "id": "folic_acid", "genericName": "Folic acid", "genericNameZh": "叶酸",
        "category": "SUPPLEMENT", "subcategory": "VITAMIN", "atc": "B03BB01",
        "brandNames": ["Folicet", "叶酸"],
        "pharmacology": "水溶性 B 族 (B9); DNA 合成 + 红细胞成熟 + 神经管发育 (孕期预防)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 1.0, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [0.4, 5.0]}],
        "kePerHour": 0.1, "tHalfHours": 5.0, "tHalfRangeHours": [3.0, 8.0],
        "vdLPerKg": 0.6, "clLPerHour": 8.0, "proteinBindingPct": 70,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 30% 肾排 + 肝代谢", "pathwayType": "Renal + hepatic"},
        "activeMetabolites": [{"id": "5_mthf", "name": "5-甲基四氢叶酸 (5-MTHF, 活性)", "activityRatio": 1.0, "note": "MTHFR 还原"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION_EGFR_30", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "methotrexate", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "叶酸逆转 MTX 抗叶酸效应, 治 MTX 不良反应 (用亚叶酸钙 Leucovorin)"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["MCV", "Hct", "叶酸血药"]},
        "indicationGroups": ["FOLATE_DEFICIENCY", "MEGALOBLASTIC_ANEMIA", "PREGNANCY_SUPPLEMENT", "MTX_TOXICITY"],
        "overdose": {"symptoms": "急性毒性极低; 大剂量 (>15 mg/d) 可能掩盖 B12 缺乏诊断",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测 B12", "antidote": None, "dataSource": "DailyMed Folic Acid Label §10"},
        "references": ["DailyMed Folic Acid Label §10"]
    },

    # ANTIVERTIGO +1
    {
        "id": "promethazine", "genericName": "Promethazine", "genericNameZh": "异丙嗪",
        "category": "ANTIVERTIGO", "subcategory": "PHENOTHIAZINE_H1", "atc": "R06AD02",
        "brandNames": ["Phenergan", "非那根"],
        "pharmacology": "1 代 H1 拮抗剂 (吩噻嗪类) + 强抗胆碱能 + 止吐; 抗晕动病 + 镇静",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.25, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [12.5, 50.0]}],
        "kePerHour": 0.12, "tHalfHours": 9.0, "tHalfRangeHours": [5.0, 14.0],
        "vdLPerKg": 13.0, "clLPerHour": 70.0, "proteinBindingPct": 93,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.7}], "inhibitors": [{"cyp": "CYP2D6", "strength": "MODERATE"}],
            "inducers": [],
            "primaryPathway": "CYP2D6 70% + 硫氧化 + 葡糖醛酸化", "pathwayType": "CYP450 + Phase II"},
        "activeMetabolites": [{"id": "promethazine_metab", "name": "硫氧化物", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 3,
            "agranulocytosis": "LOW", "extrapyramidal": "MEDIUM", "sedation": "HIGH",
            "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C",
            "elderly": "抗胆碱能↑, 谵妄/摔倒", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "mao_inhibitor", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 → 显著 CNS 抑制 + 抗胆碱能, 禁联"},
            {"triggerDrugId": "opioid", "mechanism": "ADDITIVE_CNS_RESPIRATORY",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用阿片 → 显著呼吸抑制"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["ECG", "精神症状"]},
        "indicationGroups": ["MOTION_SICKNESS", "VERTIGO", "ALLERGIES", "NAUSEA_VOMITING", "SEDATION"],
        "overdose": {"symptoms": "严重抗胆碱能 (吩噻嗪类中毒): 高热, 抗胆碱能, EPS 急性肌张力障碍, 癫痫, QT 延长",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 500.0, "fatalDoseEstimateMg": 2000.0,
            "management": "支持; 物理降温; 严重者毒扁豆碱 (QT 监护); EPS 用苯海拉明/苯托品; 控癫痫 (BZD)",
            "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能, 谨慎)",
            "dataSource": "DailyMed Phenergan Label §10 (FDA 黑框 2 岁以下禁用)"},
        "references": ["DailyMed Phenergan Label §10 (FDA 黑框 2 岁以下)"]
    },

    # ANTIVERTIGO +1
    {
        "id": "meclizine", "genericName": "Meclizine", "genericNameZh": "美克洛嗪",
        "category": "ANTIVERTIGO", "subcategory": "H1_ANTAG", "atc": "R06AE05",
        "brandNames": ["Antivert", "Bonine"],
        "pharmacology": "1 代 H1 拮抗剂 + 抗胆碱能; 抗晕动病 + 眩晕",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.5, "kaPerHour": 1.5, "tMaxHours": 3.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [12.5, 50.0]}],
        "kePerHour": 0.07, "tHalfHours": 6.0, "tHalfRangeHours": [5.0, 8.0],
        "vdLPerKg": 5.0, "clLPerHour": 30.0, "proteinBindingPct": 75,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "肝代谢 (具体 CYP 不明) + 原形尿排", "pathwayType": "Hepatic"},
        "activeMetabolites": [{"id": "meclizine_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 2,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "抗胆碱能↑",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "alcohol", "mechanism": "ADDITIVE_CNS",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用酒精 → 显著 CNS 抑制"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["VERTIGO", "MOTION_SICKNESS"],
        "overdose": {"symptoms": "抗胆碱能, 嗜睡, 低血压; 一般较轻",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None,
            "dataSource": "DailyMed Antivert Label §10"},
        "references": ["DailyMed Antivert Label §10"]
    },

    # HORMONE_REPLACEMENT +1
    {
        "id": "conjugated_estrogens", "genericName": "Conjugated estrogens", "genericNameZh": "结合雌激素",
        "category": "HORMONE_REPLACEMENT", "subcategory": "ESTROGEN", "atc": "G03CA57",
        "brandNames": ["Premarin", "倍美力"],
        "pharmacology": "结合雌激素 (多种马雌激素), 治更年期综合征 / 骨质疏松 / 阴道萎缩",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.5, "kaPerHour": 1.0, "tMaxHours": 4.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [0.3, 1.25]}],
        "kePerHour": 0.15, "tHalfHours": 17.0, "tHalfRangeHours": [10.0, 24.0],
        "vdLPerKg": 1.0, "clLPerHour": 9.0, "proteinBindingPct": 95,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP1A2", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP1A2 → 雌酮 → 雌三醇", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "estrone_ce", "name": "雌酮", "activityRatio": 0.5, "note": "下游代谢"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "心血管/乳房/子宫风险",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "thyroid_hormone", "mechanism": "DECREASED_FREE_FRACTION",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "雌激素 ↑ TBG → 游离 T4↓, 需↑甲状腺素剂量"},
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 抗凝需求变化, 监测 INR"}
        ],
        "monitoring": {"frequency": "EVERY_6_12_MONTHS", "items": ["BP", "乳房检查", "妇科检查", "肝功", "血脂"]},
        "indicationGroups": ["MENOPAUSAL_SYMPTOMS", "OSTEOPOROSIS", "VAGINAL_ATROPHY", "HORMONE_REPLACEMENT"],
        "overdose": {"symptoms": "恶心呕吐, 乳房胀痛, 不规则出血; 大剂量 → 血栓 (DVT/PE/卒中)",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "停药; 监测血栓; 抗凝 (如已发生 VTE)",
            "antidote": None, "dataSource": "DailyMed Premarin Label §10 (FDA 黑框 VTE/卒中/乳腺/子宫)"},
        "references": ["DailyMed Premarin Label §10 (FDA 黑框)"]
    },

    # ANTIBIOTIC +1
    {
        "id": "linezolid", "genericName": "Linezolid", "genericNameZh": "利奈唑胺",
        "category": "ANTIBIOTIC", "subcategory": "OXAZOLIDINONE", "atc": "J01XX08",
        "brandNames": ["Zyvox", "斯沃"],
        "pharmacology": "噁唑烷酮类抑菌, 抑制 50S 核糖体亚基; 治疗 MRSA / VRE 等多重耐药 G+",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 1.0, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [400.0, 600.0]}],
        "kePerHour": 0.07, "tHalfHours": 5.0, "tHalfRangeHours": [4.0, 6.0],
        "vdLPerKg": 0.7, "clLPerHour": 8.0, "proteinBindingPct": 31,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [{"cyp": "MAOA", "strength": "WEAK_REVERSIBLE"}],
            "inducers": [],
            "primaryPathway": "氧化代谢 (非 CYP) → 氨基乙氧基乙酸 + 羟乙基甘氨酸", "pathwayType": "Non-CYP oxidation"},
        "activeMetabolites": [{"id": "linezolid_metab", "name": "无活性代谢物", "activityRatio": 0.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "serotonergic", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 SSRI/SNRI/MAOI/三环类 → 5-HT 综合征, 联用谨慎 (2 周间隔)"},
            {"triggerDrugId": "tyramine_food", "mechanism": "MAOI_LIKE",
             "aucFoldChange": [1.0, 1.0], "severity": "MODERATE",
             "clinicalNote": "可逆 MAO 抑制, 高酪胺食物 (陈奶酪/红酒) → 高血压反应"}
        ],
        "monitoring": {"frequency": "WEEKLY", "items": ["CBC (血小板↓)", "5-HT 综合征症状", "BP"]},
        "indicationGroups": ["MRSA", "VRE", "Pneumonia_NOSOCOMIAL", "SKIN_SOFT_TISSUE"],
        "overdose": {"symptoms": "5-HT 综合征 (联用时), 骨髓抑制 (血小板↓, 贫血)",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测 CBC; 5-HT 综合征处理", "antidote": None,
            "dataSource": "DailyMed Zyvox Label §10"},
        "references": ["DailyMed Zyvox Label §10"]
    },

    # ANTIEPILEPTIC +1
    {
        "id": "topiramate", "genericName": "Topiramate", "genericNameZh": "托吡酯",
        "category": "ANTIEPILEPTIC", "subcategory": "MULTIPLE_MECHANISM", "atc": "N03AX11",
        "brandNames": ["Topamax", "妥泰"],
        "pharmacology": "多种机制: 钠通道阻滞 + GABA 增强 + AMPA 谷氨酸拮抗 + 碳酸酐酶抑制; 抗癫痫 + 偏头痛预防",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.8, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [50.0, 400.0]}],
        "kePerHour": 0.04, "tHalfHours": 21.0, "tHalfRangeHours": [18.0, 24.0],
        "vdLPerKg": 0.7, "clLPerHour": 2.0, "proteinBindingPct": 15,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [{"cyp": "CYP2C19", "strength": "WEAK"}], "inducers": [],
            "primaryPathway": "原形 70% 肾排 + 肝代谢 30%", "pathwayType": "Renal + hepatic"},
        "activeMetabolites": [{"id": "topiramate_metab", "name": "无活性代谢物", "activityRatio": 0.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "MEDIUM", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "USE_CAUTION", "elderly": "-",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "oral_contraceptive", "mechanism": "ENZYME_INDUCTION",
             "aucFoldChange": [0.7, 0.7], "severity": "MAJOR",
             "clinicalNote": "topiramate 弱 CYP 3A4 诱导 → 激素避孕效果↓, 高剂量 (>200 mg) 显著"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["血气 / HCO3 (酸中毒)", "肾结石", "体重", "眼压"]},
        "indicationGroups": ["SEIZURES_PARTIAL", "SEIZURES_GENERALIZED", "MIGRAINE_PROPHYLAXIS"],
        "overdose": {"symptoms": "严重代谢性酸中毒, 嗜睡, 意识混乱, 共济失调, 癫痫 (反常), 视力模糊",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测电解质/血气; 补碱 (NaHCO3)", "antidote": None,
            "dataSource": "DailyMed Topamax Label §10"},
        "references": ["DailyMed Topamax Label §10"]
    },

    # OSTEOPOROSIS_DRUG +1
    {
        "id": "denosumab", "genericName": "Denosumab", "genericNameZh": "地舒单抗",
        "category": "OSTEOPOROSIS_DRUG", "subcategory": "RANKL_INHIBITOR", "atc": "M05BX04",
        "brandNames": ["Prolia", "Xgeva", "普罗力"],
        "pharmacology": "RANKL 单抗, 抑制破骨细胞形成/激活; 治疗骨质疏松/骨转移/骨巨细胞瘤",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.0, "kaPerHour": 0.0, "tMaxHours": 0.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [60.0, 120.0]}],
        "kePerHour": 0.001, "tHalfHours": 720.0, "tHalfRangeHours": [600.0, 900.0],
        "vdLPerKg": 0.05, "clLPerHour": 0.01, "proteinBindingPct": 0,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "蛋白酶降解 (单抗)", "pathwayType": "Proteolysis"},
        "activeMetabolites": [{"id": "denosumab_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION_EGFR_30 (低钙风险)", "hepatic": "NONE", "elderly": "-",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "immunosuppressant", "mechanism": "ADDITIVE_IMMUNOSUPPRESSION",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 严重感染风险↑, 警惕颌骨坏死 (ONJ)"}
        ],
        "monitoring": {"frequency": "BEFORE_EACH_DOSE", "items": ["Ca++", "Vit D", "肾功能", "口腔 (ONJ)"]},
        "indicationGroups": ["OSTEOPOROSIS", "BONE_METASTASIS", "GIANT_CELL_TUMOR_BONE", "HYPERCALCEMIA_REFRACTORY"],
        "overdose": {"symptoms": "严重低钙血症, 低磷, 低镁; 长期 → 颌骨坏死 ONJ, 非典型股骨骨折",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; IV 补 Ca++; 监测 Mg2+/PO4 3-; 口腔评估",
            "antidote": None, "dataSource": "DailyMed Prolia Label §10"},
        "references": ["DailyMed Prolia Label §10"]
    },
]


added = 0
for spec in B3_DRUGS:
    drug_id = spec["id"]
    if drug_id in existing_ids:
        print(f"SKIP: {drug_id} already exists")
        continue
    template = new_template()
    template.update(spec)
    drugs.append(template)
    existing_ids.add(drug_id)
    added += 1
print(f"\nAdded {added} drugs (B3)")

# Final stats
cats = Counter(d["category"] for d in drugs)
print(f"\nFinal total: {len(drugs)} drugs, {len(cats)} categories")
print("\nCategory distribution (count):")
for cat, cnt in sorted(cats.items(), key=lambda x: -x[1]):
    print(f"  {cat}: {cnt}")

under3 = [(c, n) for c, n in cats.items() if n < 3]
if under3:
    print(f"\nCategories with < 3 drugs: {under3}")
else:
    print("\nAll categories have >= 3 drugs!")

# Update metadata
data["schemaVersion"] = "0.7.0"
data["generatedAt"] = "2026-09-07T15:55:00Z"
data["drugs"] = drugs
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"\nSaved with {len(drugs)} drugs total, size {V06.stat().st_size} bytes")
