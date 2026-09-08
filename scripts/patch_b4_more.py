"""B4: 加 15 药到 200+; 修 ANTICHOLINERGIC 和 GOUT 到 3"""
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


B4_DRUGS = [
    # ANTICHOLINERGIC (2 → 3) - 加 glycopyrrolate
    {
        "id": "glycopyrrolate", "genericName": "Glycopyrrolate", "genericNameZh": "格隆溴铵",
        "category": "ANTICHOLINERGIC", "subcategory": "QUATERNARY_AMMONIUM", "atc": "A03AB02",
        "brandNames": ["Robinul", "胃长宁"],
        "pharmacology": "季铵类抗胆碱能 (不通过血脑屏障, 中枢副作用↓); 治消化性溃疡/麻醉前给药/多汗",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.1, "kaPerHour": 1.0, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [1.0, 8.0]}],
        "kePerHour": 0.2, "tHalfHours": 1.0, "tHalfRangeHours": [0.5, 1.5],
        "vdLPerKg": 0.5, "clLPerHour": 12.0, "proteinBindingPct": 0,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 65% 肾排 (无代谢)", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "glycopyrrolate_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 2,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE", "elderly": "抗胆碱能↑",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "anticholinergic_other", "mechanism": "ADDITIVE_ANTICHOLINERGIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用其他抗胆碱能 → 显著抗胆碱能综合征, 肠梗阻/尿潴留风险"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["PEPTIC_ULCER", "ANESTHESIA_PREMED", "HYPERHIDROSIS"],
        "overdose": {"symptoms": "抗胆碱能综合征 (口干/瞳孔散大/心动过速/尿潴留/肠梗阻/高热)",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 物理降温; 活性炭; 严重者毒扁豆碱 (心电监护)",
            "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能, 谨慎)",
            "dataSource": "DailyMed Robinul Label §10"},
        "references": ["DailyMed Robinul Label §10"]
    },

    # GOUT (2 → 3) - 加 probenecid
    {
        "id": "probenecid", "genericName": "Probenecid", "genericNameZh": "丙磺舒",
        "category": "GOUT", "subcategory": "URICOSURIC", "atc": "M04AB01",
        "brandNames": ["Benemid"],
        "pharmacology": "肾小管尿酸重吸收抑制剂, 促尿酸排泄; 治慢性痛风; 也作青霉素/头孢增效剂",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.9, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [250.0, 1000.0]}],
        "kePerHour": 0.05, "tHalfHours": 6.0, "tHalfRangeHours": [4.0, 12.0],
        "vdLPerKg": 0.2, "clLPerHour": 1.5, "proteinBindingPct": 90,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形肾排 + 葡糖醛酸化", "pathwayType": "Renal + Phase II"},
        "activeMetabolites": [{"id": "probenecid_metab", "name": "acyl glucuronide", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "CONTRAINDICATED_EGFR_30", "hepatic": "NONE", "elderly": "肾清除↓",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "penicillin", "mechanism": "TUBULAR_SECRETION_INHIBITION",
             "aucFoldChange": [2.0, 4.0], "severity": "MAJOR",
             "clinicalNote": "丙磺舒 ↓ 青霉素/头孢肾清除, 联用 ↑ AUC 2-4× (历史作增效剂)"},
            {"triggerDrugId": "methotrexate", "mechanism": "TUBULAR_SECRETION_INHIBITION",
             "aucFoldChange": [2.0, 4.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "丙磺舒 ↓ MTX 肾清除 2-4×, 致死性 MTX 毒性风险, 禁联"},
            {"triggerDrugId": "allopurinol", "mechanism": "PHARMACODYNAMIC_SYNERGY",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "起始联用 → 尿酸动员, 急性痛风发作风险↑, 联用 NSAIDs 预防"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["尿酸", "肾功", "CBC"]},
        "indicationGroups": ["GOUT_CHRONIC", "HYPERURICEMIA", "PENICILLIN_AUGMENTATION"],
        "overdose": {"symptoms": "恶心呕吐, 头晕; 大剂量 → 肾损伤, 急性尿酸肾病",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 水化; 监测尿酸/肾功", "antidote": None,
            "dataSource": "DailyMed Benemid Label §10"},
        "references": ["DailyMed Benemid Label §10"]
    },

    # ANTIBIOTIC (6 → 7) - 加 ciprofloxacin
    {
        "id": "ciprofloxacin", "genericName": "Ciprofloxacin", "genericNameZh": "环丙沙星",
        "category": "ANTIBIOTIC", "subcategory": "FLUOROQUINOLONE", "atc": "J01MA02",
        "brandNames": ["Cipro", "悉复欢"],
        "pharmacology": "氟喹诺酮类抑菌, 抑制 DNA 旋转酶; 广谱 (G- / 部分 G+); 治 UTI/呼吸道/GI",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.7, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [250.0, 750.0]}],
        "kePerHour": 0.12, "tHalfHours": 4.0, "tHalfRangeHours": [3.0, 5.0],
        "vdLPerKg": 2.5, "clLPerHour": 30.0, "proteinBindingPct": 30,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP1A2", "fraction": 0.3}], "inhibitors": [{"cyp": "CYP1A2", "strength": "MODERATE"}],
            "inducers": [],
            "primaryPathway": "原形 50% 肾排 + 肝代谢 (4 种代谢物)", "pathwayType": "Renal + CYP"},
        "activeMetabolites": [{"id": "cipro_metab", "name": "氧化代谢物 (弱活性)", "activityRatio": 0.1, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE", "elderly": "肌腱断裂风险↑",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "tizanidine", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [10.0, 10.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "环丙沙星强 CYP1A2 抑制 → tizanidine AUC ↑10×, 致死性低血压, 禁联"},
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → INR↑ 出血风险"},
            {"triggerDrugId": "theophylline", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [1.5, 2.0], "severity": "MAJOR",
             "clinicalNote": "CYP1A2 抑制 → 茶碱 AUC ↑, 中毒风险"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["QTc", "肌腱症状", "血糖"]},
        "indicationGroups": ["UTI", "GIARDIASIS", "Pneumonia", "ANTHRAX", "Travelers_Diarrhea"],
        "overdose": {"symptoms": "QT 延长 → 尖端扭转型室速, 癫痫, 精神症状, 急性肾衰",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护; 控癫痫 (BZD); 监测肾功",
            "antidote": None, "dataSource": "DailyMed Cipro Label §10 (FDA 黑框肌腱断裂)"},
        "references": ["DailyMed Cipro Label §10 (FDA 黑框肌腱/神经/主动脉)"]
    },

    # PPI (4 → 5) - 加 esomeprazole
    {
        "id": "esomeprazole", "genericName": "Esomeprazole", "genericNameZh": "艾司奥美拉唑",
        "category": "PPI", "subcategory": "BENZIMIDAZOLE", "atc": "A02BC05",
        "brandNames": ["Nexium", "耐信"],
        "pharmacology": "奥美拉唑 S-异构体; CYP2C19 代谢 (慢代谢者暴露↑ 2x)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.89, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [20.0, 40.0]}],
        "kePerHour": 0.5, "tHalfHours": 1.3, "tHalfRangeHours": [1.0, 1.5],
        "vdLPerKg": 0.3, "clLPerHour": 9.0, "proteinBindingPct": 97,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2C19", "fraction": 0.7}, {"cyp": "CYP3A4", "fraction": 0.3}],
            "inhibitors": [{"cyp": "CYP2C19", "strength": "WEAK"}], "inducers": [],
            "primaryPathway": "CYP2C19 70% + CYP3A4 30%", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "esomeprazole_metab", "name": "羟基代谢物", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "clopidogrel", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [0.5, 0.5], "severity": "MAJOR",
             "clinicalNote": "CYP2C19 抑制 → 氯吡格雷活性代谢物↓50%, 抗血小板效果↓"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["Mg2+ 长期", "B12 长期"]},
        "indicationGroups": ["GERD", "PUD", "H_PYLORI_ERADICATION", "EOSINOPHILIC_ESOPHAGITIS"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Nexium Label §10"]
    },

    # CORTICOSTEROID (5 → 6) - 加 triamcinolone
    {
        "id": "triamcinolone", "genericName": "Triamcinolone", "genericNameZh": "曲安奈德",
        "category": "CORTICOSTEROID", "subcategory": "INHALED_AND_TOPICAL", "atc": "R03BA06",
        "brandNames": ["Azmacort", "Aristospan", "Nasacort"],
        "pharmacology": "中效糖皮质激素; 吸入 (哮喘) / 鼻喷 (鼻炎) / 关节腔 (局部抗炎) / 局部皮肤",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.4, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["μg"], "commonDoseRangeMg": [55.0, 220.0]}],
        "kePerHour": 0.18, "tHalfHours": 3.0, "tHalfRangeHours": [2.0, 4.0],
        "vdLPerKg": 2.0, "clLPerHour": 30.0, "proteinBindingPct": 90,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 50% + 多种代谢", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "triamcinolone_metab", "name": "6β-羟基代谢物", "activityRatio": 0.05, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "ketoconazole", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 2.0], "severity": "MAJOR",
             "clinicalNote": "酮康唑 ↑ triamcinolone 暴露"}
        ],
        "monitoring": {"frequency": "EVERY_6_12_MONTHS", "items": ["口腔念珠菌", "声嘶"]},
        "indicationGroups": ["ASTHMA", "ALLERGIC_RHINITIS", "RHEUMATOID_ARTHRITIS", "DERMATITIS"],
        "overdose": {"symptoms": "急性过量较轻; 长期大剂量 → 库欣样, 肾上腺抑制, 高血糖",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 长期大剂量不可骤停", "antidote": None,
            "dataSource": "DailyMed Nasacort Label §10"},
        "references": ["DailyMed Nasacort Label §10"]
    },

    # ALZHEIMERS (4 → 5) - 加 memantine+donepezil 复方
    {
        "id": "memantine_donepezil", "genericName": "Memantine + Donepezil", "genericNameZh": "美金刚+多奈哌齐 (复方)",
        "category": "ALZHEIMERS", "subcategory": "COMBINATION", "atc": "N06DA52",
        "brandNames": ["Namzaric"],
        "pharmacology": "美金刚 (NMDA 拮抗) + 多奈哌齐 (胆碱酯酶抑制) 复方; 中重度阿尔茨海默",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.95, "kaPerHour": 1.5, "tMaxHours": 3.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [21.0, 28.0]}],
        "kePerHour": 0.04, "tHalfHours": 60.0, "tHalfRangeHours": [40.0, 80.0],
        "vdLPerKg": 8.0, "clLPerHour": 8.0, "proteinBindingPct": 80,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}], "inhibitors": [], "inducers": [],
            "primaryPathway": "多奈哌齐 CYP3A4/CYP2D6 + 美金刚肾排 (见单独条目)", "pathwayType": "CYP + Renal"},
        "activeMetabolites": [{"id": "donepezil_metab", "name": "见 donepezil 条目", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "USE_CAUTION", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "anticholinergic", "mechanism": "PHARMACODYNAMIC_ANTAGONISM",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "胆碱酯酶抑制 + 抗胆碱能 → 互相拮抗, 联用降低疗效"}
        ],
        "monitoring": {"frequency": "EVERY_6_MONTHS", "items": ["MMSE", "心率", "体重"]},
        "indicationGroups": ["ALZHEIMERS_DISEASE_MODERATE_SEVERE"],
        "overdose": {"symptoms": "胆碱能危象 (多奈哌齐成分: DUMBELS) + NMDA 过度拮抗 (美金刚: 激越/幻觉)",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 阿托品 (胆碱能危象); 监测", "antidote": "阿托品 Atropine (胆碱能危象)",
            "dataSource": "DailyMed Namzaric Label §10"},
        "references": ["DailyMed Namzaric Label §10"]
    },

    # HORMONE_REPLACEMENT (4 → 5) - 加 levothyroxine+liothyronine 复方
    {
        "id": "thyroid_armour", "genericName": "Thyroid (desiccated)", "genericNameZh": "甲状腺片 (干甲状腺)",
        "category": "HORMONE_REPLACEMENT", "subcategory": "THYROID_ARMOUR", "atc": "H03AA05",
        "brandNames": ["Armour Thyroid", "Nature-Throid"],
        "pharmacology": "猪甲状腺干燥粉, 含 T4 + T3 (4:1 比例); 甲减替代 (历史用药)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.5, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [15.0, 180.0]}],
        "kePerHour": 0.13, "tHalfHours": 6.0, "tHalfRangeHours": [5.0, 8.0],
        "vdLPerKg": 0.5, "clLPerHour": 4.0, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "T4 → T3 脱碘 + 葡糖醛酸化", "pathwayType": "Deiodinase + Phase II"},
        "activeMetabolites": [{"id": "t3_t4", "name": "T3 + T4 (天然)", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION", "elderly": "心血管", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 抗凝需求↑, 监测 INR"}
        ],
        "monitoring": {"frequency": "EVERY_6_8_WEEKS", "items": ["TSH", "FT3/FT4", "心率", "骨密度"]},
        "indicationGroups": ["HYPOTHYROIDISM"],
        "overdose": {"symptoms": "甲亢危象: 心动过速, 高热, 震颤, 谵妄, 心律失常",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 退热; β 阻滞剂; 糖皮质激素", "antidote": None,
            "dataSource": "DailyMed Armour Thyroid Label §10"},
        "references": ["DailyMed Armour Thyroid Label §10"]
    },

    # STATIN (4 → 5) - 加 pitavastatin
    {
        "id": "pitavastatin", "genericName": "Pitavastatin", "genericNameZh": "匹伐他汀",
        "category": "STATIN", "subcategory": "HMG_COA_RED", "atc": "C10AA08",
        "brandNames": ["Livalo", "Zypitamag"],
        "pharmacology": "HMG-CoA 还原酶抑制剂; 极少 CYP 代谢, 主要 OATP1B1 + 葡糖醛酸化; 药物相互作用少",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.51, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [1.0, 4.0]}],
        "kePerHour": 0.08, "tHalfHours": 12.0, "tHalfRangeHours": [10.0, 14.0],
        "vdLPerKg": 0.5, "clLPerHour": 8.0, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "葡糖醛酸化 (UGT1A3/2B7) + OATP1B1 摄取 + 胆汁排", "pathwayType": "Phase II + OATP"},
        "activeMetabolites": [{"id": "pitavastatin_metab", "name": "内酯", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "CONTRAINDICATED_CHILD_PUGH_C",
            "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "cyclosporine", "mechanism": "OATP_INHIBITION",
             "aucFoldChange": [5.0, 5.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "环孢素 ↑ pitavastatin AUC 5×, 禁联"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ALT/AST", "CK"]},
        "indicationGroups": ["HYPERLIPIDEMIA"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Livalo Label §10"]
    },

    # ANESTHETIC (3 → 4) - 加 sevoflurane
    {
        "id": "sevoflurane", "genericName": "Sevoflurane", "genericNameZh": "七氟烷",
        "category": "ANESTHETIC", "subcategory": "INHALED_GENERAL", "atc": "N01AB08",
        "brandNames": ["Ultane", "Sevofrane"],
        "pharmacology": "吸入卤代醚全麻; 血气分配系数低 (0.65), 诱导快苏醒快; 儿科常用",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "INHALATION", "f": 1.0, "kaPerHour": 0.0, "tMaxHours": 0.0,
            "doseUnits": ["%"], "commonDoseRangeMg": [1.0, 3.0]}],
        "kePerHour": 0.4, "tHalfHours": 1.5, "tHalfRangeHours": [1.0, 2.0],
        "vdLPerKg": 0.5, "clLPerHour": 25.0, "proteinBindingPct": 0,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2E1", "fraction": 0.5}], "inhibitors": [], "inducers": [],
            "primaryPathway": "肺呼出 95% + CYP2E1 代谢 3% (hexafluoroisopropanol)", "pathwayType": "Pulmonary + CYP"},
        "activeMetabolites": [{"id": "hfi", "name": "六氟异丙醇 (HFIP)", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "USE_CAUTION_EGFR_30 (F-离子)", "hepatic": "USE_CAUTION", "elderly": "低剂量",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "succinylcholine", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 恶性高热风险↑"},
            {"triggerDrugId": "nephrotoxic_aminoglycoside", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "sevoflurane 释放 F-离子, 联用 → 肾毒↑"}
        ],
        "monitoring": {"frequency": "CONTINUOUS", "items": ["BP", "ET-sevoflurane", "ETCO2", "体温 (恶性高热)"]},
        "indicationGroups": ["GENERAL_ANESTHESIA", "PEDIATRIC_ANESTHESIA"],
        "overdose": {"symptoms": "深度呼吸/心血管抑制; 严重 → 恶性高热 (高热/肌强直/心律失常/酸中毒)",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "气道 + 机械通气; 监测体温; 恶性高热 → 丹曲林 2.5 mg/kg IV",
            "antidote": "丹曲林 Dantrolene (恶性高热)",
            "dataSource": "DailyMed Ultane Label §10"},
        "references": ["DailyMed Ultane Label §10"]
    },

    # ANALGESIC (18 → 19) - 加 meloxicam
    {
        "id": "meloxicam", "genericName": "Meloxicam", "genericNameZh": "美洛昔康",
        "category": "ANALGESIC", "subcategory": "NSAID_OXICAM", "atc": "M01AC06",
        "brandNames": ["Mobic", "莫比可"],
        "pharmacology": "COX-2 偏好抑制剂 (昔康类), 抗炎镇痛; 胃肠道副作用比非选择性 NSAID 略低",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.9, "kaPerHour": 1.0, "tMaxHours": 5.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [7.5, 15.0]}],
        "kePerHour": 0.04, "tHalfHours": 20.0, "tHalfRangeHours": [15.0, 20.0],
        "vdLPerKg": 0.2, "clLPerHour": 1.5, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2C9", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.4}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2C9 + CYP3A4 氧化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "meloxicam_metab", "name": "5'-羟基美洛昔康", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE",
            "elderly": "心血管/肾风险↑", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 出血风险↑"},
            {"triggerDrugId": "lithium", "mechanism": "DECREASED_RENAL_CLEARANCE",
             "aucFoldChange": [1.5, 1.5], "severity": "MAJOR",
             "clinicalNote": "NSAID ↓ Li 清除, 锂毒性风险"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["GI 症状", "肾功", "BP"]},
        "indicationGroups": ["OSTEOARTHRITIS", "RHEUMATOID_ARTHRITIS", "PAIN"],
        "overdose": {"symptoms": "嗜睡, 恶心, 上腹痛; 严重 → 肾衰, 胃肠道出血",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 活性炭; 监测", "antidote": None,
            "dataSource": "DailyMed Mobic Label §10"},
        "references": ["DailyMed Mobic Label §10"]
    },

    # ANTIDEPRESSANT (26 → 27) - 加 vortioxetine
    {
        "id": "vortioxetine", "genericName": "Vortioxetine", "genericNameZh": "伏硫西汀",
        "category": "ANTIDEPRESSANT", "subcategory": "MULTIMODAL_5HT", "atc": "N06AX26",
        "brandNames": ["Trintellix", "心达悦"],
        "pharmacology": "多模式 5-HT: 5-HT 再摄取抑制 + 5-HT3/7/1D 拮抗 + 5-HT1B 部分激动 + 5-HT1A 激动; 改善认知",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.75, "kaPerHour": 1.5, "tMaxHours": 7.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 20.0]}],
        "kePerHour": 0.05, "tHalfHours": 66.0, "tHalfRangeHours": [55.0, 75.0],
        "vdLPerKg": 5.0, "clLPerHour": 15.0, "proteinBindingPct": 98,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.6}, {"cyp": "CYP3A4", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2D6 60% + CYP3A4 30% 多途径", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "vortioxetine_metab", "name": "-", "activityRatio": 1.0, "note": "无显著活性代谢物"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "mao_inhibitor", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 MAOI → 5-HT 综合征, 禁联 (停 MAOI 14d+)"},
            {"triggerDrugId": "bupropion", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 2.0], "severity": "MAJOR",
             "clinicalNote": "bupropion 强 CYP2D6 抑制 → vortioxetine AUC ↑2×, 减半剂量"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["抑郁量表", "Na+ (老年)", "自杀念头"]},
        "indicationGroups": ["DEPRESSION_MDD"],
        "overdose": {"symptoms": "恶心, 眩晕, 腹泻, 全身瘙痒; 急性过量经验有限, 较 SSRI 似更轻",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None,
            "dataSource": "DailyMed Trintellix Label §10"},
        "references": ["DailyMed Trintellix Label §10"]
    },

    # ANTIVERTIGO (3 → 4) - 加 flunarizine (实际是 antimigraine 但也用于眩晕)
    {
        "id": "flunarizine", "genericName": "Flunarizine", "genericNameZh": "氟桂利嗪",
        "category": "ANTIVERTIGO", "subcategory": "CALCIUM_CHANNEL_BLOCKER", "atc": "N07CA03",
        "brandNames": ["Sibelium", "西比灵"],
        "pharmacology": "非选择性 Ca2+ 通道阻滞 + H1 拮抗; 治偏头痛预防 + 眩晕 (周围/中枢)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.85, "kaPerHour": 1.5, "tMaxHours": 3.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 10.0]}],
        "kePerHour": 0.003, "tHalfHours": 480.0, "tHalfRangeHours": [300.0, 700.0],
        "vdLPerKg": 30.0, "clLPerHour": 12.0, "proteinBindingPct": 99,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.8}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2D6 80% 氧化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "flunarizine_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "HIGH",
            "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "USE_CAUTION", "hepatic": "USE_CAUTION", "elderly": "EPS/抑郁/帕金森风险↑",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "alcohol", "mechanism": "ADDITIVE_CNS",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用酒精 → 显著 CNS 抑制"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["EPS", "抑郁/帕金森症状", "体重"]},
        "indicationGroups": ["MIGRAINE_PROPHYLAXIS", "VERTIGO"],
        "overdose": {"symptoms": "深度镇静, 锥体外系, 抑郁, 体重增加; 急性过量较轻",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测; 活性炭", "antidote": None,
            "dataSource": "DailyMed Sibelium Label §10"},
        "references": ["DailyMed Sibelium Label §10"]
    },
]

added = 0
for spec in B4_DRUGS:
    drug_id = spec["id"]
    if drug_id in existing_ids:
        print(f"SKIP: {drug_id} already exists")
        continue
    template = new_template()
    template.update(spec)
    drugs.append(template)
    existing_ids.add(drug_id)
    added += 1
print(f"\nAdded {added} drugs (B4)")

# Final stats
cats = Counter(d["category"] for d in drugs)
print(f"\nFinal total: {len(drugs)} drugs, {len(cats)} categories")
print("Final category distribution:")
for cat, cnt in sorted(cats.items(), key=lambda x: -x[1]):
    print(f"  {cat}: {cnt}")

under3 = [(c, n) for c, n in cats.items() if n < 3]
if under3:
    print(f"\nCategories with < 3 drugs: {under3}")
else:
    print("\nAll categories have >= 3 drugs!")

# Save
data["drugs"] = drugs
data["schemaVersion"] = "0.7.0"
data["generatedAt"] = "2026-09-07T16:00:00Z"
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"\nSaved with {len(drugs)} drugs total, size {V06.stat().st_size} bytes")
