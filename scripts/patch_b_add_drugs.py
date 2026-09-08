"""
B 阶段: 加药, 扩充到 200+ 种
- B1: 9 个小类补到 ≥3 药 (15 新药)
- B2: 中类各 +1-2 药 (~25 新药)
- 总目标: 159 + 40 = 199+
- 关键: PK 参数必需 (DrugCard 渲染 + PK curve)
- 没数据的字段填 "-"
"""
import json
import copy
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")

# Load
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]
existing_ids = {d["id"] for d in drugs}
print(f"Loaded {len(drugs)} drugs, {len(existing_ids)} unique IDs")

# ============================================================
# 模板: 新药默认结构 (字段全填占位, 待覆盖)
# ============================================================
def new_drug_template() -> dict:
    return {
        "id": "",
        "genericName": "",
        "genericNameZh": "",
        "brandNames": [],
        "category": "OTHER",
        "subcategory": None,
        "atc": None,
        "pkModel": "ONE_COMPARTMENT_ORAL",
        "forms": [{
            "route": "ORAL",
            "f": 0.7,
            "kaPerHour": 1.5,
            "tMaxHours": 2.0,
            "doseUnits": ["mg"],
            "commonDoseRangeMg": [10.0, 100.0]
        }],
        # PK (typical)
        "kePerHour": 0.1,            # ~7h t½
        "tHalfHours": 7.0,
        "tHalfRangeHours": [5.0, 12.0],
        "vdLPerKg": 1.5,
        "clLPerHour": 10.0,
        "proteinBindingPct": 50,
        "therapeuticWindow": None,  # 多数无明确治疗窗
        "cypProfile": {
            "substrates": [],
            "inhibitors": [],
            "inducers": [],
            "primaryPathway": "-",
            "pathwayType": "CYP450"
        },
        "activeMetabolites": [{
            "id": "-", "name": "-", "activityRatio": 1.0, "note": "-"
        }],
        "adverseEffects": {
            "qtcProlongation": "LOW",
            "metabolicSyndrome": "LOW",
            "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW",
            "extrapyramidal": "LOW",
            "sedation": "LOW",
            "sexual": "LOW",
            "hyperprolactinemia": "LOW"
        },
        "indicationGroups": [],
        "adjustments": {
            "renal": "NONE",
            "hepatic": "NONE",
            "elderly": "-",
            "smoking": None
        },
        "criticalInteractions": [],
        "monitoring": {
            "frequency": "AS_NEEDED",
            "items": []
        },
        "pharmacology": "-",
        "overdose": {
            "symptoms": "-",
            "severity": "MILD",
            "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None,
            "management": "-",
            "antidote": None,
            "dataSource": "-"
        },
        "references": [],
        "cMaxUnitFactor": 1.0,
        "skipTherapeuticWindowBand": False
    }


# ============================================================
# 新药清单 (按 category 分组)
# PK 参数基于说明书典型值; 没有的字段填 "-"
# 全部中文翻译 + 英文 INN
# ============================================================

NEW_DRUGS = [
    # ============ B1: 9 个小类补到 ≥3 ============

    # STATIN (现 1 药 → +3)
    {
        "id": "rosuvastatin", "genericName": "Rosuvastatin", "genericNameZh": "瑞舒伐他汀",
        "category": "STATIN", "subcategory": "HMG_COA_RED", "atc": "C10AA07",
        "brandNames": ["Crestor", "可定"],
        "pharmacology": "HMG-CoA 还原酶抑制剂, 抑制胆固醇合成; 亲水性, 主要经 OATP1B1 入肝",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.2, "kaPerHour": 1.0, "tMaxHours": 5.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 40.0]}],
        "kePerHour": 0.18, "tHalfHours": 19.0, "tHalfRangeHours": [13.0, 20.0],
        "vdLPerKg": 1.0, "clLPerHour": 25.0, "proteinBindingPct": 88,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "OATP1B1 + CYP2C9 (10%) + 原形 70%", "pathwayType": "OATP"},
        "activeMetabolites": [{"id": "rosuvastatin_lactone", "name": "内酯代谢物", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "CONTRAINDICATED_CHILD_PUGH_C",
            "elderly": "≥75 岁肌病风险↑, 起始低剂量",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "cyclosporine", "mechanism": "OATP_INHIBITION",
             "aucFoldChange": [7.0, 10.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "环孢素强 OATP1B1 抑制, 瑞舒伐他汀 AUC ↑7-10×, 限 5 mg"},
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ALT/AST", "CK", "肌痛监测", "肾功能"]},
        "indicationGroups": ["HYPERLIPIDEMIA", "PRIMARY_PREVENTION_CVD", "SECONDARY_PREVENTION_CVD"],
        "overdose": {
            "symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "-", "antidote": None, "dataSource": "-"
        },
        "references": ["DailyMed Crestor Label §10", "ACR/AHA 2018 血脂指南"]
    },
    {
        "id": "simvastatin", "genericName": "Simvastatin", "genericNameZh": "辛伐他汀",
        "category": "STATIN", "subcategory": "HMG_COA_RED", "atc": "C10AA01",
        "brandNames": ["Zocor", "舒降之"],
        "pharmacology": "HMG-CoA 还原酶抑制剂, 脂溶性前药, 经 CYP3A4 大量代谢为活性 β-羟基酸",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.05, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [10.0, 40.0]}],
        "kePerHour": 0.3, "tHalfHours": 2.0, "tHalfRangeHours": [1.4, 3.0],
        "vdLPerKg": 4.0, "clLPerHour": 30.0, "proteinBindingPct": 95,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.95}], "inhibitors": [],
            "inducers": [],
            "primaryPathway": "CYP3A4 95% 氧化为 β-羟基酸 (活性)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "beta_hydroxyacid_simvastatin", "name": "β-羟基酸", "activityRatio": 1.0, "note": "CYP3A4; 活性等同, 实际作用物"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "CONTRAINDICATED_CHILD_PUGH_C",
            "elderly": "肌病风险↑", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "clarithromycin", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [5.0, 10.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "克拉霉素强 CYP3A4 抑制, simvastatin AUC ↑↑, 急性横纹肌溶解风险 → 暂停 statin"},
            {"triggerDrugId": "amiodarone", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [2.0, 3.0], "severity": "MAJOR",
             "clinicalNote": "联用限 simvastatin 20 mg/d, 否则肌病风险↑"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ALT/AST", "CK", "肌痛监测"]},
        "indicationGroups": ["HYPERLIPIDEMIA", "PRIMARY_PREVENTION_CVD", "SECONDARY_PREVENTION_CVD"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Zocor Label §10", "FDA 2011 simvastatin 80 mg 黑框下调"]
    },
    {
        "id": "pravastatin", "genericName": "Pravastatin", "genericNameZh": "普伐他汀",
        "category": "STATIN", "subcategory": "HMG_COA_RED", "atc": "C10AA03",
        "brandNames": ["Pravachol", "美百乐镇"],
        "pharmacology": "HMG-CoA 还原酶抑制剂, 亲水性, 不经 CYP3A4 代谢, 药物相互作用最少",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.18, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [10.0, 80.0]}],
        "kePerHour": 0.27, "tHalfHours": 2.6, "tHalfRangeHours": [1.5, 4.0],
        "vdLPerKg": 0.8, "clLPerHour": 26.0, "proteinBindingPct": 50,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 50% + 硫酸化 25% + 葡糖醛酸化 15%", "pathwayType": "Phase II + 原形"},
        "activeMetabolites": [{"id": "pravastatin_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "CONTRAINDICATED_CHILD_PUGH_C",
            "elderly": "肌病风险↑", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ALT/AST", "CK"]},
        "indicationGroups": ["HYPERLIPIDEMIA", "PRIMARY_PREVENTION_CVD"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Pravachol Label §10"]
    },

    # PPI (现 1 → +3)
    {
        "id": "lansoprazole", "genericName": "Lansoprazole", "genericNameZh": "兰索拉唑",
        "category": "PPI", "subcategory": "BENZIMIDAZOLE", "atc": "A02BC03",
        "brandNames": ["Prevacid", "达克普隆"],
        "pharmacology": "质子泵抑制剂, 共价结合胃壁细胞 H+/K+-ATPase 不可逆抑制胃酸分泌",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.85, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [15.0, 60.0]}],
        "kePerHour": 0.46, "tHalfHours": 1.5, "tHalfRangeHours": [1.0, 2.0],
        "vdLPerKg": 0.5, "clLPerHour": 14.0, "proteinBindingPct": 97,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2C19", "fraction": 0.5}],
            "inhibitors": [{"cyp": "CYP2C19", "strength": "WEAK"}], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2C19", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "lansoprazole_sulfone", "name": "砜代谢物", "activityRatio": 0.0, "note": "无活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "clopidogrel", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [0.5, 0.5], "severity": "MAJOR",
             "clinicalNote": "CYP2C19 抑制 → 氯吡格雷活性代谢物↓50%, 抗血小板效果↓"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["Mg2+ 长期用", "B12 长期用", "骨折风险"]},
        "indicationGroups": ["GERD", "PUD", "H_PYLORI_ERADICATION"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Prevacid Label §10"]
    },
    {
        "id": "rabeprazole", "genericName": "Rabeprazole", "genericNameZh": "雷贝拉唑",
        "category": "PPI", "subcategory": "BENZIMIDAZOLE", "atc": "A02BC04",
        "brandNames": ["AcipHex", "波利特"],
        "pharmacology": "PPI; 主要非酶代谢 (硫醚化), 受 CYP2C19 慢代谢者影响小",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.52, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [10.0, 40.0]}],
        "kePerHour": 0.66, "tHalfHours": 1.05, "tHalfRangeHours": [0.7, 1.5],
        "vdLPerKg": 0.4, "clLPerHour": 9.0, "proteinBindingPct": 96,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.3}, {"cyp": "CYP2C19", "fraction": 0.1}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "非酶硫醚化 80% + CYP3A4", "pathwayType": "Non-enzymatic + CYP"},
        "activeMetabolites": [{"id": "rabeprazole_thioether", "name": "硫醚代谢物", "activityRatio": 0.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["Mg2+ 长期", "B12 长期"]},
        "indicationGroups": ["GERD", "PUD", "H_PYLORI_ERADICATION"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed AcipHex Label §10"]
    },
    {
        "id": "pantoprazole", "genericName": "Pantoprazole", "genericNameZh": "泮托拉唑",
        "category": "PPI", "subcategory": "BENZIMIDAZOLE", "atc": "A02BC02",
        "brandNames": ["Protonix", "潘妥洛克"],
        "pharmacology": "PPI; 主要经 CYP2C19 代谢, 对 CYP 抑制作用最弱 (对氯吡格雷影响最小)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.77, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [20.0, 80.0]}],
        "kePerHour": 0.5, "tHalfHours": 1.4, "tHalfRangeHours": [1.0, 2.0],
        "vdLPerKg": 0.3, "clLPerHour": 8.0, "proteinBindingPct": 98,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2C19", "fraction": 0.8}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2C19 80%", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "pantoprazole_sulfone", "name": "砜代谢物", "activityRatio": 0.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["Mg2+ 长期", "B12 长期"]},
        "indicationGroups": ["GERD", "PUD", "H_PYLORI_ERADICATION", "UPPER_GI_BLEED"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Protonix Label §10"]
    },

    # ANTIVERTIGO (现 1 → +2)
    {
        "id": "dimenhydrinate", "genericName": "Dimenhydrinate", "genericNameZh": "茶苯海明",
        "category": "ANTIVERTIGO", "subcategory": "H1_ANTAG", "atc": "R06AA52",
        "brandNames": ["Dramamine", "晕海宁"],
        "pharmacology": "H1 受体拮抗剂 (茶苯海明 = 苯海拉明 55% + 8-chlorotheophylline 45%), 抗晕动病",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.5, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [50.0, 200.0]}],
        "kePerHour": 0.1, "tHalfHours": 7.0, "tHalfRangeHours": [4.0, 10.0],
        "vdLPerKg": 3.0, "clLPerHour": 25.0, "proteinBindingPct": 80,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.5}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2D6 50% + 葡糖醛酸化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "diphenhydramine_dim", "name": "苯海拉明 (代谢物)", "activityRatio": 1.0, "note": "茶苯海明 55% 为苯海拉明"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 3,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "抗胆碱能↑, 谵妄风险", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "alprazolam", "mechanism": "ADDITIVE_CNS_DEPRESSION",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 显著 CNS 抑制, 避免驾车"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["MOTION_SICKNESS", "VERTIGO"],
        "overdose": {"symptoms": "严重抗胆碱能综合征 (经典: hot/dry/red/blind/mad), 癫痫, 昏迷; 儿童更危险",
            "severity": "LIFE_THREATENING",
            "toxicDoseEstimateMg": 750.0, "fatalDoseEstimateMg": 1500.0,
            "management": "支持; 物理降温; 活性炭; 严重中枢抗胆碱能用毒扁豆碱 (ICU 监护); 控癫痫 (BZD)",
            "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能, 谨慎)",
            "dataSource": "DailyMed Dramamine Label §10; Goldfrank's Ch47"},
        "references": ["DailyMed Dramamine Label §10"]
    },
    {
        "id": "betahistine", "genericName": "Betahistine", "genericNameZh": "倍他司汀",
        "category": "ANTIVERTIGO", "subcategory": "H3_ANTAG", "atc": "N07CA01",
        "brandNames": ["Serc", "敏使朗"],
        "pharmacology": "H3 受体拮抗剂 + H1 受体弱激动剂, 增加内耳血流, 治疗梅尼埃病/眩晕",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.99, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [8.0, 48.0]}],
        "kePerHour": 0.7, "tHalfHours": 1.0, "tHalfRangeHours": [0.5, 1.5],
        "vdLPerKg": 1.0, "clLPerHour": 18.0, "proteinBindingPct": 5,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 90% 肾排 + 氧化代谢", "pathwayType": "Renal + minor CYP"},
        "activeMetabolites": [{"id": "betahistine_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["VERTIGO", "MENIERES_DISEASE"],
        "overdose": {"symptoms": "-", "severity": "MILD", "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None, "management": "-", "antidote": None, "dataSource": "-"},
        "references": ["DailyMed Serc Label §10"]
    },

    # ANTICHOLINERGIC (现 2 → +1)
    {
        "id": "amantadine", "genericName": "Amantadine", "genericNameZh": "金刚烷胺",
        "category": "ANTICHOLINERGIC", "subcategory": "NMDA_ANTAG", "atc": "N04BB01",
        "brandNames": ["Symmetrel", "金刚烷胺"],
        "pharmacology": "NMDA 受体拮抗剂 + 多巴胺释放促进 + 抗流感 A; 治疗帕金森 + 流感",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.9, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [100.0, 300.0]}],
        "kePerHour": 0.08, "tHalfHours": 15.0, "tHalfRangeHours": [10.0, 30.0],
        "vdLPerKg": 5.0, "clLPerHour": 17.0, "proteinBindingPct": 67,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 90% 肾排 (肾小管分泌)", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "amantadine_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 2,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM",
            "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "NONE",
            "elderly": "抗胆碱能, 谵妄", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "memantine", "mechanism": "PHARMACODYNAMIC_SYNERGY",
             "aucFoldChange": [1.0, 1.0], "severity": "MODERATE",
             "clinicalNote": "两药皆 NMDA 拮抗, 联用增加 CNS 副作用"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["肾功能"]},
        "indicationGroups": ["PARKINSONS_DISEASE", "INFLUENZA_A", "FATIGUE_MS"],
        "overdose": {"symptoms": "严重抗胆碱能 (混淆/幻觉/高热/尿潴留/瞳孔散大/心律失常), 癫痫, 昏迷",
            "severity": "LIFE_THREATENING",
            "toxicDoseEstimateMg": 1500.0, "fatalDoseEstimateMg": 3000.0,
            "management": "支持; 物理降温; 严重者毒扁豆碱 (心电监护, 谨慎); 控癫痫 (BZD)",
            "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能, 谨慎)",
            "dataSource": "DailyMed Symmetrel Label §10"},
        "references": ["DailyMed Symmetrel Label §10"]
    },

    # ANTICOAGULANT (现 2 → +2)
    {
        "id": "dabigatran", "genericName": "Dabigatran", "genericNameZh": "达比加群",
        "category": "ANTICOAGULANT", "subcategory": "DIRECT_THROMBIN_INH", "atc": "B01AE07",
        "brandNames": ["Pradaxa", "泰毕全"],
        "pharmacology": "直接凝血酶 (IIa) 抑制剂; 前药, 酯酶水解为活性 dabigatran",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.065, "kaPerHour": 1.0, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [75.0, 220.0]}],
        "kePerHour": 0.1, "tHalfHours": 14.0, "tHalfRangeHours": [12.0, 17.0],
        "vdLPerKg": 1.0, "clLPerHour": 17.0, "proteinBindingPct": 35,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 80% 肾排 (P-gp 外排)", "pathwayType": "Renal + P-gp"},
        "activeMetabolites": [{"id": "dabigatran_ace", "name": "(前药水解为活性形式)", "activityRatio": 1.0,
            "note": "dabigatran etexilate → dabigatran (活性)"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "CONTRAINDICATED_EGFR_15", "hepatic": "NONE",
            "elderly": "肾清除↓, 出血风险↑", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "rifampin", "mechanism": "P_GP_INDUCTION",
             "aucFoldChange": [0.4, 0.4], "severity": "MAJOR",
             "clinicalNote": "利福平诱导 P-gp, dabigatran 暴露↓60%, 联用需谨慎"},
            {"triggerDrugId": "dronedarone", "mechanism": "P_GP_INHIBITION",
             "aucFoldChange": [2.0, 3.0], "severity": "MAJOR",
             "clinicalNote": "决奈达隆 P-gp 抑制, dabigatran AUC↑2-3×"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["肾功能", "血红蛋白", "粪隐血"]},
        "indicationGroups": ["ATRIAL_FIBRILLATION", "VTE_TREATMENT", "VTE_PREVENTION"],
        "overdose": {
            "symptoms": "出血 (皮肤瘀斑/血尿/消化道/脑); 早期无明显症状",
            "severity": "LIFE_THREATENING",
            "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "停药; 评估出血; 活性炭 (2h 内); 严重出血 → Idarucizumab (特异性解药)",
            "antidote": "Idarucizumab (依达赛珠单抗, 5 g IV, 直接结合 dabigatran 失活 — 2015 FDA 批准)",
            "dataSource": "DailyMed Pradaxa Label §10; NEJM 2017 RE-VERSE AD"
        },
        "references": ["DailyMed Pradaxa Label §10", "RE-VERSE AD (Pollack CV et al., NEJM 2017)"]
    },
    {
        "id": "apixaban", "genericName": "Apixaban", "genericNameZh": "阿哌沙班",
        "category": "ANTICOAGULANT", "subcategory": "FACTOR_XA_INH", "atc": "B01AF02",
        "brandNames": ["Eliquis", "艾乐妥"],
        "pharmacology": "直接 Xa 因子抑制剂; 口服生物利用度高, 部分肝代谢, 部分肾排",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.5, "kaPerHour": 1.5, "tMaxHours": 3.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [2.5, 10.0]}],
        "kePerHour": 0.12, "tHalfHours": 12.0, "tHalfRangeHours": [8.0, 15.0],
        "vdLPerKg": 0.5, "clLPerHour": 7.0, "proteinBindingPct": 87,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.25}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 25% + 原形 30% 肾排 + 多重代谢", "pathwayType": "CYP + Renal"},
        "activeMetabolites": [{"id": "apixaban_metab", "name": "氧化代谢物", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_DOSE_EGFR_15_30", "hepatic": "USE_CAUTION_CHILD_PUGH_B",
            "elderly": "出血风险↑", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "ketoconazole", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 2.0], "severity": "MAJOR",
             "clinicalNote": "酮康唑 ↑ apixaban AUC 2×, 出血风险"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["肾功能", "血红蛋白", "粪隐血"]},
        "indicationGroups": ["ATRIAL_FIBRILLATION", "VTE_TREATMENT", "VTE_PREVENTION"],
        "overdose": {
            "symptoms": "出血; 早期无明显症状",
            "severity": "LIFE_THREATENING",
            "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "停药; 评估出血; 严重 → Andexanet alfa (Xa 因子抑制剂逆转剂)",
            "antidote": "Andexanet alfa (重组 Xa 因子, 重大/危及生命出血 — FDA 2018 批准, 但 apixaban 适应证 2018 ANNEXA-FXa 研究支持)",
            "dataSource": "DailyMed Eliquis Label §10; ANNEXA-FXa"
        },
        "references": ["DailyMed Eliquis Label §10"]
    },

    # ANTIHISTAMINE (现 2 → +2)
    {
        "id": "loratadine", "genericName": "Loratadine", "genericNameZh": "氯雷他定",
        "category": "ANTIHISTAMINE", "subcategory": "2ND_GEN_H1", "atc": "R06AX13",
        "brandNames": ["Claritin", "开瑞坦"],
        "pharmacology": "2 代 H1 受体选择性拮抗剂, 不通过血脑屏障, 几乎无嗜睡",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.8, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 20.0]}],
        "kePerHour": 0.1, "tHalfHours": 8.0, "tHalfRangeHours": [5.0, 12.0],
        "vdLPerKg": 1.2, "clLPerHour": 12.0, "proteinBindingPct": 97,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2D6", "fraction": 0.5}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP2D6 → desloratadine (活性等同)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "desloratadine", "name": "地氯雷他定 (desloratadine)", "activityRatio": 1.0,
            "note": "CYP3A4/2D6 脱羧; 活性等同, t½ 27h, 本身也作药用"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C",
            "elderly": "-", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["ALLERGIC_RHINITIS", "URTICARIA"],
        "overdose": {"symptoms": "嗜睡, 心动过速, 头痛; 急性过量较轻",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed Claritin Label §10"},
        "references": ["DailyMed Claritin Label §10"]
    },
    {
        "id": "cetirizine", "genericName": "Cetirizine", "genericNameZh": "西替利嗪",
        "category": "ANTIHISTAMINE", "subcategory": "2ND_GEN_H1", "atc": "R06AE07",
        "brandNames": ["Zyrtec", "仙特明"],
        "pharmacology": "2 代 H1 拮抗剂 (hydroxyzine 活性代谢物), 不通过血脑屏障",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.7, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 20.0]}],
        "kePerHour": 0.13, "tHalfHours": 8.3, "tHalfRangeHours": [6.0, 10.0],
        "vdLPerKg": 0.7, "clLPerHour": 6.0, "proteinBindingPct": 93,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 50% 肾排 + 轻微氧化", "pathwayType": "Renal + minor"},
        "activeMetabolites": [{"id": "cetirizine_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "NONE",
            "elderly": "嗜睡/口干略↑", "smoking": None},
        "criticalInteractions": [],
        "monitoring": {"frequency": "AS_NEEDED", "items": []},
        "indicationGroups": ["ALLERGIC_RHINITIS", "URTICARIA"],
        "overdose": {"symptoms": "嗜睡, 头晕, 口干, 尿潴留; 急性过量较轻",
            "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed Zyrtec Label §10"},
        "references": ["DailyMed Zyrtec Label §10"]
    },

    # ANTIARRHYTHMIC (现 2 → +2)
    {
        "id": "lidocaine", "genericName": "Lidocaine", "genericNameZh": "利多卡因",
        "category": "ANTIARRHYTHMIC", "subcategory": "IB_CLASS", "atc": "C01BB01",
        "brandNames": ["Xylocaine", "赛罗卡因"],
        "pharmacology": "IB 类抗心律失常, 钠通道阻滞; 局麻; 肝首过强, 口服无效 (IV 用)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.35, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [100.0, 300.0]}],
        "kePerHour": 0.7, "tHalfHours": 1.0, "tHalfRangeHours": [0.5, 2.0],
        "vdLPerKg": 1.3, "clLPerHour": 38.0, "proteinBindingPct": 70,
        "therapeuticWindow": {"low": 1.5, "high": 5.0, "unit": "μg/mL", "guidelineSource": "DailyMed"},
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP1A2", "fraction": 0.3}],
            "inhibitors": [{"cyp": "CYP1A2", "strength": "WEAK"}], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP1A2 + CYP2D6 多种 CYP", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "mega_xylidine", "name": "monoethylglycinexylidide (MEGX)", "activityRatio": 0.8,
            "note": "CYP1A2; 活性约 80%, t½ 2h, 蓄积"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
            "elderly": "心脏传导敏感, 低剂量起始", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "propranolol", "mechanism": "CYP_INHIBITION",
             "aucFoldChange": [2.0, 2.0], "severity": "MAJOR",
             "clinicalNote": "普萘洛尔 ↓ 肝血流, lidocaine 清除↓ 50%, 毒性↑"}
        ],
        "monitoring": {"frequency": "DURING_IV_INFUSION", "items": ["ECG", "lidocaine 血药 1.5-5 μg/mL", "中枢症状"]},
        "indicationGroups": ["VENTRICULAR_ARRHYTHMIA", "LOCAL_ANESTHESIA"],
        "overdose": {
            "symptoms": "LAST (局麻药全身中毒): 早期口周麻木/金属味/耳鸣 → 中毒: 癫痫, 心律失常, 心搏骤停, CNS 抑制",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "气道; 控癫痫 (BZD); 心律失常 → 胺碘酮 (避免 1A/1C); CPR 标准 + 肾上腺素",
            "antidote": "脂肪乳剂 Intralipid 20% (1.5 mL/kg IV bolus, 然后 0.25 mL/kg/min 输注) — LAST 相对特异",
            "dataSource": "Reg Anesth Pain Med 2018 LAST checklist; DailyMed Xylocaine Label"
        },
        "references": ["DailyMed Xylocaine Label §10"]
    },
    {
        "id": "flecainide", "genericName": "Flecainide", "genericNameZh": "氟卡尼",
        "category": "ANTIARRHYTHMIC", "subcategory": "IC_CLASS", "atc": "C01BC04",
        "brandNames": ["Tambocor"],
        "pharmacology": "IC 类抗心律失常, 钠通道强阻滞; 治疗室上性/室性心律失常, 但 CAST 研究显示 MI 后患者 ↑ 死亡率",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.95, "kaPerHour": 1.5, "tMaxHours": 2.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [50.0, 200.0]}],
        "kePerHour": 0.07, "tHalfHours": 12.0, "tHalfRangeHours": [10.0, 16.0],
        "vdLPerKg": 5.0, "clLPerHour": 25.0, "proteinBindingPct": 40,
        "therapeuticWindow": {"low": 0.2, "high": 1.0, "unit": "μg/mL", "guidelineSource": "DailyMed"},
        "cypProfile": {"substrates": [{"cyp": "CYP2D6", "fraction": 0.7}, {"cyp": "CYP1A2", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP2D6 70% (CYP2D6 慢代谢者毒性↑)", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "flecainide_metab", "name": "m-O-dealkyl + lactam", "activityRatio": 0.05, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B",
            "elderly": "低剂量起始", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "amiodarone", "mechanism": "ADDITIVE_CARDIAC",
             "aucFoldChange": [1.5, 2.0], "severity": "MAJOR",
             "clinicalNote": "联用显著心脏传导抑制, 监测 ECG"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["ECG", "QRS 宽度", "flecainide 血药"]},
        "indicationGroups": ["VENTRICULAR_ARRHYTHMIA", "SV_TACHYCARDIA", "AFLUTTER"],
        "overdose": {
            "symptoms": "QRS 增宽, 严重心动过缓, 低血压, 室性心律失常, 晕厥, 心脏停搏; 强钠通道阻滞效应",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 1500.0, "fatalDoseEstimateMg": None,
            "management": "支持; 心电监护; **QRS 增宽给 NaHCO3** (类 TCA 机制); 临时起搏; 避免 1A/1C 抗心律失常",
            "antidote": "碳酸氢钠 NaHCO3 (QRS 增宽时 — 钠通道阻滞过量相对特异, 类似 TCA 机制)",
            "dataSource": "DailyMed Tambocor Label §10; CAST 试验 Echt 1991"
        },
        "references": ["DailyMed Tambocor Label §10", "CAST (Echt DS et al., NEJM 1991)"]
    },

    # ANTIMIGRAINE (现 2 → +1)
    {
        "id": "rizatriptan", "genericName": "Rizatriptan", "genericNameZh": "利扎曲普坦",
        "category": "ANTIMIGRAINE", "subcategory": "TRIPTAN_5HT1B_1D", "atc": "N02CC04",
        "brandNames": ["Maxalt", "欧立停"],
        "pharmacology": "5-HT1B/1D 受体激动剂, 收缩颅内血管, 抑制三叉神经释放",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.45, "kaPerHour": 1.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 10.0]}],
        "kePerHour": 0.2, "tHalfHours": 2.5, "tHalfRangeHours": [2.0, 3.0],
        "vdLPerKg": 1.5, "clLPerHour": 25.0, "proteinBindingPct": 14,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "MAOA", "fraction": 0.7}], "inhibitors": [], "inducers": [],
            "primaryPathway": "MAOA 氧化脱氨", "pathwayType": "MAO"},
        "activeMetabolites": [{"id": "rizatriptan_metab", "name": "氧化代谢物", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "VERY_LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "propranolol", "mechanism": "CYP_MAJOR",
             "aucFoldChange": [1.7, 1.7], "severity": "MAJOR",
             "clinicalNote": "普萘洛尔 ↑ rizatriptan AUC 70%, 限 5 mg/dose, 间隔 ≥2h"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["心电图首次用", "血压"]},
        "indicationGroups": ["MIGRAINE_ACUTE"],
        "overdose": {
            "symptoms": "血压↑/↓ 显著, 冠脉痉挛 (心绞痛/MI), 5-HT 综合征 (联用 SSRI), 嗜睡, 心动过缓",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 冠脉事件 (阿司匹林 + 硝酸甘油); 5-HT 综合征处理; 监测 ECG/血压",
            "antidote": None, "dataSource": "DailyMed Maxalt Label §10"
        },
        "references": ["DailyMed Maxalt Label §10"]
    },

    # MUSCLE_RELAXANT (现 2 → +1)
    {
        "id": "cyclobenzaprine", "genericName": "Cyclobenzaprine", "genericNameZh": "环苯扎林",
        "category": "MUSCLE_RELAXANT", "subcategory": "CENTRAL_ACTING", "atc": "M03BX08",
        "brandNames": ["Flexeril", "Amrix"],
        "pharmacology": "中枢性肌松, 结构似 TCA, 抗胆碱能明显; 用于急性肌肉骨骼疼痛 (短期 ≤3 周)",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.55, "kaPerHour": 1.5, "tMaxHours": 4.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [5.0, 30.0]}],
        "kePerHour": 0.13, "tHalfHours": 18.0, "tHalfRangeHours": [8.0, 37.0],
        "vdLPerKg": 7.0, "clLPerHour": 50.0, "proteinBindingPct": 93,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP1A2", "fraction": 0.3}],
            "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 + CYP1A2 + 葡糖醛酸化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "cyclobenzaprine_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 3,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "HIGH",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_B", "elderly": "抗胆碱能↑, 谵妄",
            "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "mao_inhibitor", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "TCA 样结构 → 联用 MAOI 致 5-HT 综合征 / 高热危象, 禁联用"},
            {"triggerDrugId": "tramadol", "mechanism": "PHARMACODYNAMIC",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 ↑ 5-HT 综合征 + 癫痫风险"}
        ],
        "monitoring": {"frequency": "AS_NEEDED", "items": ["ECG 老年/心血管病"]},
        "indicationGroups": ["ACUTE_MUSCULOSKELETAL_PAIN"],
        "overdose": {
            "symptoms": "TCA 样: 严重抗胆碱能 (干燥/瞳孔散大/尿潴留) + QRS 增宽/室速 + 癫痫 + CNS 抑制",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 1000.0, "fatalDoseEstimateMg": None,
            "management": "支持; QRS>100 给 NaHCO3; 控癫痫 (BZD); 活性炭",
            "antidote": "碳酸氢钠 NaHCO3 (QRS 增宽时 — TCA 样钠通道阻滞机制)",
            "dataSource": "DailyMed Flexeril Label §10"
        },
        "references": ["DailyMed Flexeril Label §10"]
    },

    # OSTEOPOROSIS_DRUG (现 2 → +1)
    {
        "id": "zoledronic_acid", "genericName": "Zoledronic acid", "genericNameZh": "唑来膦酸",
        "category": "OSTEOPOROSIS_DRUG", "subcategory": "BISPHOSPHONATE_IV", "atc": "M05BA08",
        "brandNames": ["Reclast", "Aclasta", "密固达"],
        "pharmacology": "含氮双膦酸盐, 抑制破骨细胞骨吸收; IV 给药, 1 年 1 次",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.01, "kaPerHour": 0.5, "tMaxHours": 1.5,
            "doseUnits": ["mg"], "commonDoseRangeMg": [4.0, 5.0]}],
        "kePerHour": 0.04, "tHalfHours": 167.0, "tHalfRangeHours": [100.0, 250.0],
        "vdLPerKg": 0.2, "clLPerHour": 1.5, "proteinBindingPct": 22,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [], "inhibitors": [], "inducers": [],
            "primaryPathway": "原形 100% 肾排 (不代谢)", "pathwayType": "Renal"},
        "activeMetabolites": [{"id": "zoledronic_metab", "name": "-", "activityRatio": 1.0, "note": "-"}],
        "adverseEffects": {"qtcProlongation": "VERY_LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "VERY_LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "CONTRAINDICATED_EGFR_35", "hepatic": "NONE", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "aminoglycoside", "mechanism": "ADDITIVE_TOXICITY",
             "aucFoldChange": [1.0, 1.0], "severity": "MAJOR",
             "clinicalNote": "联用 → 显著低钙血症"}
        ],
        "monitoring": {"frequency": "BEFORE_EACH_INFUSION", "items": ["肾功能", "Ca++", "Vit D", "下颌骨体检"]},
        "indicationGroups": ["OSTEOPOROSIS", "BONE_METASTASIS", "PAGET_DISEASE", "HYPERCALCEMIA"],
        "overdose": {
            "symptoms": "严重低钙血症, 低镁, 低磷; 肾毒性; 急性期相反应 (流感样)",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; 静脉补 Ca++; 监测 Mg2+/PO4 3-; 充分水化保护肾; 监测肾功",
            "antidote": None, "dataSource": "DailyMed Reclast Label §10"
        },
        "references": ["DailyMed Reclast Label §10"]
    },

    # GOUT (现 2 → +1)
    {
        "id": "colchicine", "genericName": "Colchicine", "genericNameZh": "秋水仙碱",
        "category": "GOUT", "subcategory": "MICROTUBULE_INH", "atc": "M04AC01",
        "brandNames": ["Colcrys", "Mitigare"],
        "pharmacology": "微管蛋白抑制剂, 抑制中性粒细胞趋化/激活; 治疗急性痛风/家族性地中海热/心包炎",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.45, "kaPerHour": 1.5, "tMaxHours": 1.0,
            "doseUnits": ["mg"], "commonDoseRangeMg": [0.6, 1.2]}],
        "kePerHour": 0.13, "tHalfHours": 27.0, "tHalfRangeHours": [20.0, 40.0],
        "vdLPerKg": 7.0, "clLPerHour": 30.0, "proteinBindingPct": 39,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.4}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 40% + P-gp 外排", "pathwayType": "CYP450 + P-gp"},
        "activeMetabolites": [{"id": "colchicine_metab", "name": "2-O-去甲基, 3-O-去甲基", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "REDUCE_50_PCT_EGFR_30", "hepatic": "USE_CAUTION_CHILD_PUGH_B",
            "elderly": "老年 + 肾↓ = 毒性↑", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "clarithromycin", "mechanism": "CYP_INHIBITION_STRONG + P_GP_INHIBITION",
             "aucFoldChange": [3.0, 5.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 → 秋水仙碱中毒/致死, FDA 黑框: 禁联 clarithromycin"},
            {"triggerDrugId": "cyclosporine", "mechanism": "CYP_INHIBITION + P_GP_INHIBITION",
             "aucFoldChange": [2.0, 3.0], "severity": "CONTRAINDICATED",
             "clinicalNote": "联用 → 秋水仙碱毒性"}
        ],
        "monitoring": {"frequency": "EVERY_3_MONTHS", "items": ["CBC", "CK", "肾功", "肌病监测"]},
        "indicationGroups": ["GOUT_ACUTE", "GOUT_PROPHYLAXIS", "FAMILIAL_MEDITERRANEAN_FEVER", "PERICARDITIS"],
        "overdose": {
            "symptoms": "胃肠道先兆 (恶心/呕吐/腹痛/腹泻) → 24-72h 后多器官衰 (骨髓抑制/横纹肌溶解/DIC/肾衰/呼吸衰竭)",
            "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 10.0, "fatalDoseEstimateMg": 50.0,
            "management": "支持; 活性炭 (即使延迟服, 重复给); 水化; 监测血象/肾/凝血; 粒细胞缺乏 → G-CSF",
            "antidote": "无特异性; G-CSF (粒细胞缺乏时); 血液透析 (部分有效)",
            "dataSource": "DailyMed Colcrys Label §10; Clin Toxicol 2017 Colchicine OD"
        },
        "references": ["DailyMed Colcrys Label §10 (FDA 黑框 CYP3A4/P-gp 抑制剂)"]
    },

    # BRONCHODILATOR (现 2 → +1, 不重复已有)
    {
        "id": "salmeterol", "genericName": "Salmeterol", "genericNameZh": "沙美特罗",
        "category": "BRONCHODILATOR", "subcategory": "LABA", "atc": "R03AC12",
        "brandNames": ["Serevent"],
        "pharmacology": "长效 β2 激动剂 (LABA, 12h); 吸入; 单用 ↑ 哮喘死亡风险, 必联 ICS",
        "pkModel": "ONE_COMPARTMENT_ORAL", "forms": [{"route": "ORAL", "f": 0.05, "kaPerHour": 1.0, "tMaxHours": 2.0,
            "doseUnits": ["μg"], "commonDoseRangeMg": [25.0, 50.0]}],
        "kePerHour": 0.05, "tHalfHours": 12.0, "tHalfRangeHours": [8.0, 16.0],
        "vdLPerKg": 1.0, "clLPerHour": 8.0, "proteinBindingPct": 95,
        "therapeuticWindow": None,
        "cypProfile": {"substrates": [{"cyp": "CYP3A4", "fraction": 0.8}], "inhibitors": [], "inducers": [],
            "primaryPathway": "CYP3A4 氧化", "pathwayType": "CYP450"},
        "activeMetabolites": [{"id": "salmeterol_metab", "name": "α-羟基沙美特罗", "activityRatio": 0.1, "note": "弱活性"}],
        "adverseEffects": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0,
            "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "LOW",
            "sexual": "VERY_LOW", "hyperprolactinemia": "VERY_LOW"},
        "adjustments": {"renal": "NONE", "hepatic": "USE_CAUTION_CHILD_PUGH_C", "elderly": "-", "smoking": None},
        "criticalInteractions": [
            {"triggerDrugId": "ketoconazole", "mechanism": "CYP_INHIBITION_STRONG",
             "aucFoldChange": [2.0, 2.0], "severity": "MAJOR",
             "clinicalNote": "酮康唑 ↑ salmeterol AUC, 心血管副作用↑"}
        ],
        "monitoring": {"frequency": "EVERY_3_6_MONTHS", "items": ["FEV1", "心率", "K+", "QTc"]},
        "indicationGroups": ["ASTHMA", "COPD", "EXERCISE_INDUCED_BRONCHOSPASM"],
        "overdose": {
            "symptoms": "显著 β2 过量: 心动过速, 震颤, 低钾, 高血糖, 室性心律失常, 心绞痛",
            "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
            "management": "支持; β 阻滞剂 (艾司洛尔, 监测气道); 补 K+; 心电监护",
            "antidote": "无特异性; β 阻滞剂 (心脏选择型, 监测气道痉挛)",
            "dataSource": "DailyMed Serevent Label §10 (FDA 黑框单用)"
        },
        "references": ["DailyMed Serevent Label §10 (FDA 黑框单用哮喘致死)"]
    },
]

# Apply
added = 0
for spec in NEW_DRUGS:
    drug_id = spec["id"]
    if drug_id in existing_ids:
        print(f"SKIP: {drug_id} already exists")
        continue
    template = new_drug_template()
    template.update(spec)
    drugs.append(template)
    existing_ids.add(drug_id)
    added += 1
print(f"\nAdded {added} drugs (B1)")

# Save intermediate
data["drugs"] = drugs
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Saved with {len(drugs)} drugs total")
