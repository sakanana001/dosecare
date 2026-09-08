#!/usr/bin/env python3
"""v0.6.1: 把 39 个 DrugCategory=OTHER 的药全部划入明确组
数据源: ATC 分类 / FDA DailyMed / 中国药品说明书
"""
import json
from pathlib import Path

p = Path("app/src/main/assets/drugs/v0.6.json")
data = json.loads(p.read_text(encoding="utf-8"))

# 39 个 OTHER 药 → 新 DrugCategory (按 ATC / FDA 药理分类)
RECAT = {
    # 抗凝
    "warfarin": "ANTICOAGULANT",
    "rivaroxaban": "ANTICOAGULANT",
    # PPI
    "omeprazole": "PPI",
    # 抗心律失常
    "amiodarone": "ANTIARRHYTHMIC",
    "sotalol": "ANTIARRHYTHMIC",
    # 阿尔茨海默
    "donepezil": "ALZHEIMERS",
    "rivastigmine": "ALZHEIMERS",
    "galantamine": "ALZHEIMERS",
    "memantine": "ALZHEIMERS",
    # 抗偏头痛
    "sumatriptan": "ANTIMIGRAINE",
    "flunarizine": "ANTIMIGRAINE",
    # 肌松
    "baclofen": "MUSCLE_RELAXANT",
    "tizanidine": "MUSCLE_RELAXANT",
    # 抗眩晕
    "betahistine": "ANTIVERTIGO",
    # 骨质疏松
    "alendronate": "OSTEOPOROSIS_DRUG",
    "risedronate": "OSTEOPOROSIS_DRUG",
    # BPH
    "finasteride": "BPH_AGENT",
    "dutasteride": "BPH_AGENT",
    "tamsulosin": "BPH_AGENT",
    # 激素 (含 ADH 垂体后叶素)
    "estradiol": "HORMONE_REPLACEMENT",
    "progesterone": "HORMONE_REPLACEMENT",
    "desmopressin": "HORMONE_REPLACEMENT",
    # 抗感染
    "fluconazole": "ANTIBIOTIC",
    "rifampin": "ANTIBIOTIC",
    "dolutegravir": "ANTIBIOTIC",
    # 阿片拮抗 → 归入 ANALGESIC (治疗阿片相关)
    "naltrexone": "ANALGESIC",
    # 物质依赖治疗
    "acamprosate": "SUBSTANCE_USE",
    "disulfiram": "SUBSTANCE_USE",
    # 营养补充
    "calcium_carbonate": "SUPPLEMENT",
    "ferrous_sulfate": "SUPPLEMENT",
    "folic_acid": "SUPPLEMENT",
    # 抗痛风
    "allopurinol": "GOUT",
    "colchicine": "GOUT",
    # 麻醉
    "cocaine": "ANESTHETIC",
    "ketamine": "ANESTHETIC",
    "norketamine": "ANESTHETIC",
    # 支气管扩张剂
    "arformoterol": "BRONCHODILATOR",
    "levalbuterol": "BRONCHODILATOR",
    # MDMA — 治 PTSD 创伤后应激, 归入 SUBSTANCE_USE
    "mdma": "SUBSTANCE_USE",
}

n = 0
for drug in data["drugs"]:
    if drug["id"] in RECAT:
        old_cat = drug["category"]
        new_cat = RECAT[drug["id"]]
        if old_cat != "OTHER":
            print(f"WARN: {drug['id']} 不是 OTHER (是 {old_cat}) 但仍重分类到 {new_cat}")
        drug["category"] = new_cat
        n += 1

# 验证: 剩余 OTHER
remaining_other = [d["id"] for d in data["drugs"] if d["category"] == "OTHER"]
print(f"已重分类 {n} 个药")
print(f"剩余 OTHER: {len(remaining_other)}")
if remaining_other:
    print(f"  IDs: {remaining_other}")

p.write_text(
    json.dumps(data, ensure_ascii=False, indent=2),
    encoding="utf-8"
)
print(f"\n已写入 {p}")

# 统计
import collections
counts = collections.Counter(d["category"] for d in data["drugs"])
print(f"\n=== DrugCategory 最终分布 ===")
for k, v in counts.most_common():
    print(f"  {k}: {v}")
