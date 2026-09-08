"""B5: 加 3 药到 200+, 覆盖重要空白 (analgesic 阿片 oxycodone, statin lovastatin)"""
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


B5 = [
    # ANALGESIC +1: oxycodone (强阿片)
    {
        "id": "oxycodone", "genericName": "Oxycodone", "genericNameZh": "羟考酮",
        "category": "ANALGESIC", "subcategory": "OPIOID_AGONIST", "atc": "N02AA05",
        "brandNames": ["OxyContin", "Percocet", "奥施康"],
        "pharmacology": "强 μ 阿片受体激动剂; 治中重度疼痛; 控释/速释多种剂型",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.75, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 80.0]}],
        "kePerHour": 0.12, "tHalfHours": 3.5, "tHalfRangeHours": [2.5, 5.0],
        "vdLPerKg": 2.5, "clLPerHour": 25.0, "proteinBindingPct": 45,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.7}, {"cyp": "CYP2D6", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 70% (N-去甲基) + CYP2D6 30% (O-去甲基 → oxymorphone)",
            "pathwayType": "CYP450"},
        "activeMetabolites": [
            {"id": "oxymorphone", "name": "羟吗啡酮 (oxymorphone)", "activityRatio": 14.0,
                "note": "CYP2D6; 活性 14×, 强 μ 激动"},
            {"id": "noroxycodone", "name": "去甲羟考酮", "activityRatio": 0.05, "note": "弱活性"}
        ],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
            "elderly": "低剂量起始, 呼吸抑制风险", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "benzodiazepine", "mechanism": "ADDITIVE_RESPIRATORY",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "BZD + 阿片 → 致死性呼吸抑制, FDA 黑框"},
            {"triggerDrugId": "maoi", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 MAOI → 5-HT 综合征 / 严重阿片反应"}
        ],
        "monitoring": {"frequency": "MONTHLY", "items": ["疼痛量表", "滥用 / 转移", "通气功能"]},
        "indicationGroups": ["PAIN_MODERATE_SEVERE", "CHRONIC_PAIN", "CANCER_PAIN"],
        "overdose": {
            "symptoms": "呼吸抑制 (主因, 致死), 瞳孔缩小, 意识抑制, 低血压, 紫绀, 肌松",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 80.0, "fatalDoseEstimateMg": 200.0,
            "management": "气道 + 机械通气; 纳洛酮 0.4-2 mg IV, 重复给 (t½ ~3.5h, 需监测)",
            "antidote": "纳洛酮 Naloxone",
            "dataSource": "DailyMed OxyContin Label §10 (Schedule II)"
        },
        "references": ["DailyMed OxyContin Label §10 (Schedule II, FDA 黑框)"]
    },
    # STATIN +1: lovastatin
    {
        "id": "lovastatin", "genericName": "Lovastatin", "genericNameZh": "洛伐他汀",
        "category": "STATIN", "subcategory": "HMG_COA_RED", "atc": "C10AA02",
        "brandNames": ["Mevacor", "Altoprev"],
        "pharmacology": "HMG-CoA 还原酶抑制剂; 天然他汀, 脂溶, CYP3A4 大量代谢; simvastatin 前体",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.05, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [10.0, 80.0]}],
        "kePerHour": 0.3, "tHalfHours": 3.0, "tHalfRangeHours": [2.0, 5.0],
        "vdLPerKg": 4.0, "clLPerHour": 30.0, "proteinBindingPct": 95,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.95}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 95% 氧化为活性 β-羟基酸", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "lovastatin_hydroxyacid", "name": "β-羟基酸", "activityRatio": 1.0,
            "note": "CYP3A4; 活性等同, 实际作用物"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "CONTRAINDICATED_CHILD_PUGH_C",
            "elderly": "肌病风险", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "clarithromycin", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [5.0, 10.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "强 CYP3A4 抑制 → lovastatin AUC ↑↑, 横纹肌溶解风险"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ALT/AST", "CK"]},
        "indicationGroups": ["HYPERLIPIDEMIA", "PRIMARY_PREVENTION_CVD"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Mevacor Label §10"]
    },
    # ANTIVERTIGO +1: ondansetron (5-HT3 拮抗, 抗恶心, 化疗/术后)
    {
        "id": "ondansetron", "genericName": "Ondansetron", "genericNameZh": "昂丹司琼",
        "category": "ANTIVERTIGO", "subcategory": "5HT3_ANTAG", "atc": "A04AA01",
        "brandNames": ["Zofran", "枢复宁"],
        "pharmacology": "5-HT3 受体拮抗剂, 阻断迷走神经 + 中枢 CTZ, 强效止吐; 化疗/术后/放疗呕吐",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.6, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [4.0, 24.0]}],
        "kePerHour": 0.21, "tHalfHours": 3.0, "tHalfRangeHours": [2.5, 4.0],
        "vdLPerKg": 1.5, "clLPerHour": 16.0, "proteinBindingPct": 70,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2D6", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2D6 + CYP1A2 多途径羟化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "ondansetron_metab", "name": "8-OH-ondansetron", "activityRatio": 0.05, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "apomorphine", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 → 严重低血压, 禁联"},
            {"triggerDrugId": "qt_prolonging", "mechanism": "PHARMACODYNAMIC_QT",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用其他 QT 延长药 (氟哌啶醇/胺碘酮) → 尖端扭转型室速风险↑, 监测 ECG"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["ECG 老年/电解质紊乱"]},
        "indicationGroups": ["CHEMO_NAUSEA", "POSTOP_NAUSEA", "RADIATION_NAUSEA"],
        "overdose": {"symptoms": "视力模糊, 严重便秘, 低血压, 晕厥; 罕见 5-HT 综合征 (联用)",
            "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测 ECG; 对症", "antidote": None,
            "dataSource": "DailyMed Zofran Label §10"},
        "references": ["DailyMed Zofran Label §10"]
    },
    # ANTIBIOTIC +1: levofloxacin
    {
        "id": "levofloxacin", "genericName": "Levofloxacin", "genericNameZh": "左氧氟沙星",
        "category": "ANTIBIOTIC", "subcategory": "FLUOROQUINOLONE", "atc": "J01MA12",
        "brandNames": ["Levaquin", "可乐必妥"],
        "pharmacology": "氧氟沙星 L-异构体, 氟喹诺酮类; 抗菌活性 2x; 治疗呼吸道/UTI/皮肤",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.99, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [250.0, 750.0]}],
        "kePerHour": 0.07, "tHalfHours": 6.0, "tHalfRangeHours": [5.0, 8.0],
        "vdLPerKg": 1.3, "clLPerHour": 9.0, "proteinBindingPct": 30,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 87% 肾排 + 少量代谢", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "levofloxacin_metab", "name": "N-oxide + desmethyl", "activityRatio": 0.1, "note": "弱"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_30", "hepatic": "NONE",
            "elderly": "肌腱/主动脉/神经副作用↑ (FDA 黑框)", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "antacid_iron", "mechanism": "CHELATION",
             "aucFoldChange": [0.5, 0.5], "severity": "MAJOR",
             "clinicalNote": "二价/三价阳离子螯合, 吸收↓, 间隔 2h"},
            {"triggerDrugId": "warfarin", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → INR↑ 出血风险"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["QTc", "肌腱", "血糖", "神经症状"]},
        "indicationGroups": ["Pneumonia_CAP", "Pneumonia_NOSOCOMIAL", "UTI", "Sinusitis", "Skin_Soft_Tissue"],
        "overdose": {"symptoms": "QT 延长, 癫痫, 精神症状, 急性肾衰",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护; 控癫痫 (BZD); 监测", "antidote": None,
            "dataSource": "DailyMed Levaquin Label §10 (FDA 黑框肌腱/神经/主动脉/低血糖)"},
        "references": ["DailyMed Levaquin Label §10 (FDA 黑框)"]
    },
]

added = 0
for spec in B5:
    if spec["id"] in existing_ids:
        print(f"SKIP: {spec['id']}")
        continue
    t = new_template()
    t.update(spec)
    drugs.append(t)
    existing_ids.add(spec["id"])
    added += 1
print(f"\nAdded {added} drugs (B5)")

cats = Counter(d["category"] for d in drugs)
print(f"\nFinal: {len(drugs)} drugs, {len(cats)} categories")

data["drugs"] = drugs
data["schemaVersion"] = "0.7.0"
data["generatedAt"] = "2026-09-07T16:05:00Z"
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Saved, size {V06.stat().st_size} bytes")
