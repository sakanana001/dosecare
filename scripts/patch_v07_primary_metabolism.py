#!/usr/bin/env python3
"""
v0.6.1 校准: 159 个药补 primaryPathway + pathwayType 字段

规则:
- 若 cypProfile.substrates 非空 (CYP 代谢):
  primaryPathway = "CYP1A2 70% + CYP3A4 20% + CYP2C19 10%"  (按 fraction 排序)
  pathwayType = "CYP450"
- 若 substrates 为空 (非 CYP 代谢):
  primaryPathway = 实际代谢途径
  pathwayType = 对应分类

数据源:
- FDA DailyMed Clinical Pharmacology / Pharmacokinetics section
- AGNP 2017
- Flockhart Table (Indiana University)
- PMID 公开临床药理
"""
import json
from pathlib import Path

p = Path("app/src/main/assets/drugs/v0.6.json")
data = json.loads(p.read_text(encoding="utf-8"))

# 49 个 substrates 为空的药 (经过梳理的真实代谢途径)
NON_CYP_PATHWAYS = {
    "amisulpride": {
        "primaryPathway": "原型经肾排出 (~50%), 少量肝代谢",
        "pathwayType": "RENAL_EXCRETION"
    },
    "valproic_acid": {
        "primaryPathway": "β-氧化 (~40%) + UGT1A6/1A9/2B7 葡萄糖苷酸化 (~30%) + CYP2C9 (~10%)",
        "pathwayType": "BETA_OXIDATION"
    },
    "lithium": {
        "primaryPathway": "100% 经肾小球滤过,原型排出,无肝代谢",
        "pathwayType": "RENAL_EXCRETION"
    },
    "lamotrigine": {
        "primaryPathway": "UGT1A4 葡萄糖苷酸化 (~80%) + N-葡萄糖醛酸化",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "paroxetine": {
        "primaryPathway": "CYP2D6 主导 (饱和代谢, 自身抑制) 体内非线性",
        "pathwayType": "CYP450"
    },
    "lorazepam": {
        "primaryPathway": "UGT2B7 葡萄糖苷酸化为无活性代谢物 (绕开 CYP, 无显著药物相互作用)",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "oxcarbazepine": {
        "primaryPathway": "酮还原酶 → 活性代谢物 MHD (10-羟基卡马西平) → UGT 葡萄糖苷酸化排泄",
        "pathwayType": "OTHER"
    },
    "topiramate": {
        "primaryPathway": "70% 经肾原型排泄, 肝代谢非主要 (CYP 介导极少)",
        "pathwayType": "RENAL_EXCRETION"
    },
    "gabapentin": {
        "primaryPathway": "100% 经肾原型排泄, 无肝代谢, 无 CYP 相互作用",
        "pathwayType": "RENAL_EXCRETION"
    },
    "pregabalin": {
        "primaryPathway": "98% 经肾原型排泄, 无肝代谢",
        "pathwayType": "RENAL_EXCRETION"
    },
    "benztropine": {
        "primaryPathway": "未知 (动物数据有限), 推测 N-脱甲基 + 水解",
        "pathwayType": "OTHER"
    },
    "trihexyphenidyl": {
        "primaryPathway": "未知 (动物数据有限)",
        "pathwayType": "OTHER"
    },
    "levodopa_carbidopa": {
        "primaryPathway": "左旋多巴: 多巴脱羧酶 + COMT + MAO-B 代谢; 卡比多巴抑制外周多巴脱羧酶",
        "pathwayType": "OTHER"
    },
    "pramipexole": {
        "primaryPathway": "100% 经肾原型排泄, 无肝代谢",
        "pathwayType": "RENAL_EXCRETION"
    },
    "metformin": {
        "primaryPathway": "100% 经肾原型排泄, OCT2/MATE1 转运 (无肝代谢)",
        "pathwayType": "RENAL_EXCRETION"
    },
    "empagliflozin": {
        "primaryPathway": "UGT2B7 葡萄糖苷酸化 (~86%) + 少量原型经尿",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "lisinopril": {
        "primaryPathway": "100% 原型经肾排泄, 无代谢",
        "pathwayType": "RENAL_EXCRETION"
    },
    "aspirin": {
        "primaryPathway": "血浆/肝酯酶水解为水杨酸 (活性), 半衰期剂量依赖",
        "pathwayType": "HYDROLYSIS"
    },
    "levothyroxine": {
        "primaryPathway": "脱碘酶 T4→T3 (活性) + 肝结合排泄",
        "pathwayType": "DEIODINATION"
    },
    "hydroxyzine": {
        "primaryPathway": "CYP3A4 (~70%) + CYP2D6 代谢为活性西替利嗪",
        "pathwayType": "CYP450"
    },
    "sotalol": {
        "primaryPathway": "100% 经肾原型排泄, 无代谢",
        "pathwayType": "RENAL_EXCRETION"
    },
    "levetiracetam": {
        "primaryPathway": "66% 经肾原型排泄 + 24% 酶水解 (非 CYP)",
        "pathwayType": "HYDROLYSIS"
    },
    "phenelzine": {
        "primaryPathway": "不可逆抑制 MAO-A/B, 乙酰化代谢, 效力持续 2 周 (酶重新合成)",
        "pathwayType": "MAO"
    },
    "entacapone": {
        "primaryPathway": "葡萄糖苷酸化代谢 (无 CYP), 尿液橙色无害",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "amantadine": {
        "primaryPathway": "100% 经肾原型排泄, 无代谢; 流感 A 用药",
        "pathwayType": "RENAL_EXCRETION"
    },
    "tolcapone": {
        "primaryPathway": "葡萄糖苷酸化 (无 CYP); FDA 黑框警告: 急性肝坏死",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "rivastigmine": {
        "primaryPathway": "血浆胆碱酯酶水解 (无 CYP), 无显著药物相互作用",
        "pathwayType": "ESTERASE"
    },
    "eslicarbazepine": {
        "primaryPathway": "UGT2B4 葡萄糖苷酸化水解, 无 CYP 介导",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "lacosamide": {
        "primaryPathway": "CYP2C19 ~60% 代谢为无活性 O-desmethyl 代谢物 + 30% 原型经尿",
        "pathwayType": "CYP450"
    },
    "sumatriptan": {
        "primaryPathway": "MAO-A 代谢 (非 CYP) + 65% 原型经尿排泄",
        "pathwayType": "MAO"
    },
    "baclofen": {
        "primaryPathway": "85% 经肾原型排泄, 肝代谢极小; 鞘内给药绕过血脑屏障",
        "pathwayType": "RENAL_EXCRETION"
    },
    "betahistine": {
        "primaryPathway": "MAOB 代谢为无活性 2-pyridylacetic acid; 无显著 CYP",
        "pathwayType": "MAO"
    },
    "meclizine": {
        "primaryPathway": "肝代谢, 但无显著 CYP 介导相互作用",
        "pathwayType": "OTHER"
    },
    "liraglutide": {
        "primaryPathway": "皮下注射, 被 DPP-IV 酶降解 (无 CYP), 代谢产物经尿/胆汁排泄",
        "pathwayType": "DPP4"
    },
    "methimazole": {
        "primaryPathway": "几乎无 CYP 介导; 治疗 Graves 病首选 (除妊娠早期)",
        "pathwayType": "OTHER"
    },
    "propylthiouracil": {
        "primaryPathway": "肝葡萄糖苷酸化; FDA 限制: 仅甲巯咪唑不耐受/妊娠早期/甲亢危象",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
    "alendronate": {
        "primaryPathway": "吸收极差 (~0.6%), 需空腹直立位服; 骨内半衰期 >10 年; 原型经肾排泄",
        "pathwayType": "RENAL_EXCRETION"
    },
    "risedronate": {
        "primaryPathway": "与阿仑膦酸类似, 空肠吸收, 原型经肾排泄",
        "pathwayType": "RENAL_EXCRETION"
    },
    "desmopressin": {
        "primaryPathway": "抗利尿激素 V2 受体激动剂; 主要肾小管重吸收/降解, 不通过胎盘",
        "pathwayType": "RENAL_EXCRETION"
    },
    "naltrexone": {
        "primaryPathway": "还原为 6-β-naltrexol (活性) + 二氢二醇; 几乎不经 CYP",
        "pathwayType": "OTHER"
    },
    "acamprosate": {
        "primaryPathway": "100% 肾原型排泄, 无 CYP 介导, 无显著药物相互作用",
        "pathwayType": "RENAL_EXCRETION"
    },
    "disulfiram": {
        "primaryPathway": "ALDH 不可逆抑制 (作用持续 7-14 天至酶重新合成); CYP2E1 抑制",
        "pathwayType": "OTHER"
    },
    "calcium_carbonate": {
        "primaryPathway": "Ca2+ 经小肠吸收 (VDR 调节); 碳酸氢根中和胃酸; 不经 CYP",
        "pathwayType": "OTHER"
    },
    "ferrous_sulfate": {
        "primaryPathway": "Fe2+ 经 DMT1 转运, 胃酸促进吸收, 需空腹或维 C 同服",
        "pathwayType": "OTHER"
    },
    "folic_acid": {
        "primaryPathway": "代谢为 5-MTHF (活性形式), 妊娠/MTX 治疗常用",
        "pathwayType": "OTHER"
    },
    "allopurinol": {
        "primaryPathway": "活性代谢物 oxypurinol (t½ 18-30h, 实际起效分子); 不经 CYP",
        "pathwayType": "OTHER"
    },
    "remifentanil": {
        "primaryPathway": "超短效阿片; 血浆/组织非特异性酯酶水解, 代谢不依赖肝肾",
        "pathwayType": "HYDROLYSIS"
    },
    "ghb": {
        "primaryPathway": "GABA-B 受体激动; 极短 t½; 西方用于发作性睡病; 中国 I 类精麻",
        "pathwayType": "OTHER"
    },
    "morphine_6_glucuronide": {
        "primaryPathway": "吗啡活性代谢物 M6G (效价 2× 吗啡, 强 μ-阿片激动); UGT2B7 葡萄糖醛酸化; 肾衰时蓄积 (t½ 延长至 50h)",
        "pathwayType": "UGT_GLUCURONIDATION"
    },
}

# CYP 比例补充 (有 substrates 的药, 优化 primaryPathway 描述, 标具体比例)
CYP_RATIO_REFINEMENTS = {
    # 大部分已经有 substrates, 但 primaryPathway 之前是 note, 没结构化
}

# CYP 比例描述模板: 同一药多个 CYP 底物, 生成 "CYP1A2 70% + CYP3A4 20% + ..." 形式
def cyp_ratio_text(subs):
    parts = [f"{s['cyp']} {(s['fraction']*100):.0f}%" for s in sorted(subs, key=lambda x: -x['fraction'])]
    return " + ".join(parts) if parts else None

n_non_cyp = 0
n_cyp_refined = 0
for drug in data["drugs"]:
    if "cypProfile" not in drug:
        continue
    cp = drug["cypProfile"]
    subs = cp.get("substrates", [])

    if drug["id"] in NON_CYP_PATHWAYS:
        # 非 CYP 代谢
        info = NON_CYP_PATHWAYS[drug["id"]]
        cp["primaryPathway"] = info["primaryPathway"]
        cp["pathwayType"] = info["pathwayType"]
        n_non_cyp += 1
    elif subs:
        # CYP 代谢, 用 substrates 生成比例描述
        ratio_text = cyp_ratio_text(subs)
        if ratio_text and not cp.get("primaryPathway"):
            cp["primaryPathway"] = ratio_text
            cp["pathwayType"] = "CYP450"
            n_cyp_refined += 1

p.write_text(
    json.dumps(data, ensure_ascii=False, indent=2),
    encoding="utf-8"
)
print(f"已补 {n_non_cyp} 个非 CYP 代谢药 + {n_cyp_refined} 个 CYP 比例药, 写入 {p}")
print(f"\n=== pathwayType 分布 ===")
import collections
counts = collections.Counter()
for d in data["drugs"]:
    pt = d.get("cypProfile", {}).get("pathwayType", "未标")
    counts[pt] += 1
for k, v in counts.most_common():
    print(f"  {k}: {v}")
