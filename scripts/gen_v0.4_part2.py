"""
v0.4 第二/三/四批 (50 药):
- BATCH2 神经内科 (18)
- BATCH3 内分泌 + 激素 (18)
- BATCH4 特殊 (14)
"""

# ==================== 神经内科扩充 (18) ====================
BATCH2 = [
    # 抗帕金森 (5)
    {
        "id": "ropinirole", "name": "Ropinirole", "zh": "罗匹尼罗",
        "brands": ["Requip", "力必平"],
        "cat": "ANTIPARKINSONIAN", "sub": "DOPAMINE_AGONIST", "atc": "N04BC04",
        "pk": {"f": 0.55, "ka": 1.0, "tmax": 1.5, "t12": 6.0, "t12r": [5, 8], "vdkg": 7.5, "pb": 40},
        "cyp": {"subs": [{"cyp": "CYP1A2", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "CYP1A2 主导,吸烟者浓度降低"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "起始 0.25mg tid,缓滴定;白天嗜睡/幻觉", "smoking": "CYP1A2 诱导,需加剂量"},
        "ints": [
            {"id": "ciprofloxacin", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP1A2 抑制,浓度升高,幻觉/嗜睡风险"},
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP1A2 强抑制"},
            {"id": "antipsychotics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "D2 受体拮抗,降低罗匹尼罗效,联用精神分裂症恶化"},
            {"id": "MAOI", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "高血压危象"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["BP(尤其直立)", "白天嗜睡(可致驾驶事故)", "冲动控制(赌博/性)", "幻觉"]},
        "refs": ["AGNP 2017", "FDA Requip Prescribing Information"]
    },
    {
        "id": "rasagiline", "name": "Rasagiline", "zh": "雷沙吉兰",
        "brands": ["Azilect", "安齐来"],
        "cat": "ANTIPARKINSONIAN", "sub": "MAO_B_INHIBITOR", "atc": "N04BD02",
        "pk": {"f": 0.36, "ka": 0.5, "tmax": 1.0, "t12": 2.0, "t12r": [1, 3], "vdkg": 9.0, "pb": 88},
        "cyp": {"subs": [{"cyp": "CYP1A2", "fraction": 0.7}], "inh": ["MAO-B(选择性,不可逆)"], "ind": [],
               "note": "低剂量(1mg)选 MAO-B,无 tyramine 饮食限制;高剂量失选择性"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 0.5mg qd,2 周后 1mg qd", "smoking": "CYP1A2 诱导,可能需加剂量"},
        "ints": [
            {"id": "ciprofloxacin", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP1A2 抑制,浓度升高 2 倍"},
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "CYP1A2 强抑制,可能失选择性,需低 tyramine 饮食"},
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征,氟西汀停 5 周"},
            {"id": "snris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征"},
            {"id": "tyramine_rich_food", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "0.5mg 选 MAO-B,无需饮食限制;1mg 可能需注意"},
            {"id": "levodopa", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "联用增强左旋多巴效,常规组合"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["BP", "肝功", "5-HT 综合征症状"]},
        "refs": ["AGNP 2017", "FDA Azilect Prescribing Information"]
    },
    {
        "id": "entacapone", "name": "Entacapone", "zh": "恩他卡朋",
        "brands": ["Comtan", "珂丹"],
        "cat": "ANTIPARKINSONIAN", "sub": "COMT_INHIBITOR", "atc": "N04BX02",
        "pk": {"f": 0.35, "ka": 1.0, "tmax": 1.0, "t12": 0.8, "t12r": [0.5, 1.0], "vdkg": 0.5, "pb": 98},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "葡萄糖苷酸化代谢,无 CYP;尿液变橙色无害"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "200mg 与左旋多巴同服", "smoking": None},
        "ints": [
            {"id": "iron_sulfate", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.6], "s": "MEDIUM", "n": "铁剂螯合,吸收降低,需间隔 2-3h"},
            {"id": "MAOI", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用可能高血压危象,谨慎"},
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "罕见 INR 改变"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功", "血压", "腹泻", "尿液变色(无害)"]},
        "refs": ["AGNP 2017", "FDA Comtan Prescribing Information"]
    },
    {
        "id": "amantadine", "name": "Amantadine", "zh": "金刚烷胺",
        "brands": ["Symmetrel", "Gocovri"],
        "cat": "ANTIPARKINSONIAN", "sub": "NMDA_ANTAGONIST_DA_AGONIST", "atc": "N04BB01",
        "pk": {"f": 0.86, "ka": 1.5, "tmax": 4.0, "t12": 15.0, "t12r": [10, 31], "vdkg": 8.0, "pb": 67},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "100% 肾原型排泄,无代谢;亦用于流感 A"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 100mg qd", "smoking": None},
        "ints": [
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加抗胆碱能效应"},
            {"id": "thiazide_diuretics", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "MEDIUM", "n": "氢氯噻嗪减少金刚烷胺肾清除,中毒风险"},
            {"id": "memantine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "机制相似,联用可能增加副作用,谨慎"},
            {"id": "live_attenuated_vaccine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用减弱流感疫苗效;停药前后 2 周避免接种"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肾功", "网状青斑(livedo reticularis)", "幻觉(尤其老年)", "踝部水肿"]},
        "refs": ["AGNP 2017", "FDA Symmetrel Prescribing Information"]
    },
    {
        "id": "tolcapone", "name": "Tolcapone", "zh": "托卡朋",
        "brands": ["Tasmar"],
        "cat": "ANTIPARKINSONIAN", "sub": "COMT_INHIBITOR", "atc": "N04BX01",
        "pk": {"f": 0.65, "ka": 1.0, "tmax": 2.0, "t12": 2.0, "t12r": [1, 3], "vdkg": 0.4, "pb": 99},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "葡萄糖苷酸化;FDA 黑框警告:急性肝坏死风险,仅在其他治疗失败时使用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "100mg tid", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "MEDIUM", "n": "INR 略升,监测"},
            {"id": "MAOI", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用高血压危象"},
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征罕见报道"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功(ALT/AST/胆红素):基线 + 2 周 + 1 月 + 之后每 2 周", "BP", "腹泻"]},
        "refs": ["AGNP 2017", "FDA Tasmar Black Box Warning (急性肝坏死)"]
    },
    # 阿尔茨海默 (4)
    {
        "id": "donepezil", "name": "Donepezil", "zh": "多奈哌齐",
        "brands": ["Aricept", "安理申"],
        "cat": "OTHER", "sub": "AChE_INHIBITOR", "atc": "N06DA02",
        "pk": {"f": 1.0, "ka": 0.5, "tmax": 4.0, "t12": 70.0, "t12r": [60, 90], "vdkg": 12.0, "pb": 96},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}, {"cyp": "CYP2D6", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "t½ 极长(70h),5mg qd 4 周后达稳态"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 5mg qn,4 周后可加 10mg", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP3A4 强抑制,浓度略升"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP2D6 强抑制"},
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低多奈哌齐效(机制相反)"},
            {"id": "succinylcholine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "AChE 抑制增强肌松效应"},
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用可能心动过缓/晕厥"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["认知量表(MMSE/MoCA)", "心率(尤其病窦综合征)", "体重", "胃肠反应"]},
        "refs": ["AGNP 2017", "FDA Aricept Prescribing Information"]
    },
    {
        "id": "rivastigmine", "name": "Rivastigmine", "zh": "卡巴拉汀",
        "brands": ["Exelon", "艾斯能"],
        "cat": "OTHER", "sub": "AChE_BuChE_INHIBITOR", "atc": "N06DA03",
        "pk": {"f": 0.4, "ka": 0.5, "tmax": 1.0, "t12": 1.5, "t12r": [1, 2], "vdkg": 1.5, "pb": 40},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "血浆胆碱酯酶水解,无 CYP 介导;无显著药物相互作用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "起始 1.5mg bid,缓滴定;贴片起始 4.6mg/24h", "smoking": None},
        "ints": [
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低卡巴拉汀效"},
            {"id": "succinylcholine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "AChE 抑制增强肌松效应"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["认知量表", "心率", "皮肤反应(贴片)"]},
        "refs": ["AGNP 2017", "FDA Exelon Prescribing Information"]
    },
    {
        "id": "galantamine", "name": "Galantamine", "zh": "加兰他敏",
        "brands": ["Razadyne", "Reminyl", "利忆灵"],
        "cat": "OTHER", "sub": "AChE_INHIBITOR", "atc": "N06DA04",
        "pk": {"f": 0.9, "ka": 1.0, "tmax": 1.0, "t12": 7.0, "t12r": [5, 10], "vdkg": 2.5, "pb": 18},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "CYP2D6/3A4 代谢;慢代谢者浓度高"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 4mg bid,4 周后可加 8mg bid", "smoking": None},
        "ints": [
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP2D6 强抑制,浓度升高"},
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP2D6 强抑制"},
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP3A4 强抑制"},
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低加兰他敏效"},
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "心动过缓/晕厥风险"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["认知量表", "心率", "体重"]},
        "refs": ["AGNP 2017", "FDA Razadyne Prescribing Information"]
    },
    {
        "id": "memantine", "name": "Memantine", "zh": "美金刚",
        "brands": ["Namenda", "Namenda XR", "易倍申"],
        "cat": "OTHER", "sub": "NMDA_RECEPTOR_ANTAGONIST", "atc": "N06DX01",
        "pk": {"f": 1.0, "ka": 0.7, "tmax": 3.0, "t12": 70.0, "t12r": [60, 80], "vdkg": 10.0, "pb": 45},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "70% 原型肾排泄,部分 CYP3A4 代谢"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 5mg qd,每周加 5mg 至 10mg bid", "smoking": None},
        "ints": [
            {"id": "amantadine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "机制相似,联用可能增加副作用"},
            {"id": "dextromethorphan", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "NMDA 受体拮抗叠加,联用应谨慎"},
            {"id": "carbonate_antacids", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.8], "s": "MEDIUM", "n": "碱性尿液减少美金刚肾清除,可能升高浓度"},
            {"id": "hydrochlorothiazide", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.8], "s": "MEDIUM", "n": "改变尿液 pH,可能影响美金刚清除"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["认知量表", "肾功", "意识模糊/幻觉(老年)"]},
        "refs": ["AGNP 2017", "FDA Namenda Prescribing Information"]
    },
    # 抗癫痫 (3)
    {
        "id": "eslicarbazepine", "name": "Eslicarbazepine", "zh": "醋酸艾司利卡西平",
        "brands": ["Aptiom", "Zebinix", "奕欣"],
        "cat": "ANTIEPILEPTIC", "sub": "DIBENZAZEPINE", "atc": "N03AF04",
        "pk": {"f": 0.9, "ka": 0.5, "tmax": 3.0, "t12": 16.0, "t12r": [13, 20], "vdkg": 1.0, "pb": 40},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "UGT2B4 葡萄糖苷酸化水解,无 CYP 介导"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "LOW", "extrapyramidal": "MEDIUM", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 400mg qd,2 周后可加 800mg qd", "smoking": None},
        "ints": [
            {"id": "carbamazepine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "同系物,联用增加皮疹/SJS 风险"},
            {"id": "oxcarbazepine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "同系物,联用增加副作用"},
            {"id": "phenytoin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加苯妥英毒性,需监测"},
            {"id": "carbamazepine_strong_inducer", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "MEDIUM", "n": "OATP 抑制,影响某些药转运"},
            {"id": "oral_contraceptives", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "降低口服避孕药效"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["WBC", "血钠(SIADH)", "肝功", "皮疹(SJS 风险)"]},
        "refs": ["AGNP 2017", "FDA Aptiom Prescribing Information"]
    },
    {
        "id": "perampanel", "name": "Perampanel", "zh": "吡仑帕奈",
        "brands": ["Fycompa", "卫克启"],
        "cat": "ANTIEPILEPTIC", "sub": "AMPA_RECEPTOR_ANTAGONIST", "atc": "N03AX22",
        "pk": {"f": 1.0, "ka": 0.3, "tmax": 1.0, "t12": 105.0, "t12r": [70, 140], "vdkg": 1.5, "pb": 95},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "t½ 极长(105h),停药后效应持续 2-3 周"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 2mg qn,缓滴定", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 强抑制,浓度升高"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4 强诱导"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4 诱导"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加攻击性/愤怒/意识混乱(FDA 黑框)"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["行为/情绪(攻击性 FDA 黑框)", "自杀倾向", "黄疸", "肝功"]},
        "refs": ["AGNP 2017", "FDA Fycompa Black Box Warning (严重精神症状)"]
    },
    {
        "id": "lacosamide", "name": "Lacosamide", "zh": "拉科酰胺",
        "brands": ["Vimpat", "维派特"],
        "cat": "ANTIEPILEPTIC", "sub": "SLOW_INACTIVATION_NA_CHANNEL", "atc": "N03AX18",
        "pk": {"f": 1.0, "ka": 0.5, "tmax": 1.0, "t12": 13.0, "t12r": [12, 16], "vdkg": 0.6, "pb": 15},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "CYP2C19 代谢为无活性 O-desmethyl 代谢物;95% 经尿排泄"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 50mg bid,缓滴定", "smoking": None},
        "ints": [
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 PR 间期延长,房室阻滞风险"},
            {"id": "calcium_channel_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 PR 延长/心动过缓风险"},
            {"id": "digoxin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 PR 延长"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(PR 间期)", "肝功", "自杀倾向", "DILI 早期症状"]},
        "refs": ["AGNP 2017", "FDA Vimpat Prescribing Information"]
    },
    # 偏头痛 (2)
    {
        "id": "sumatriptan", "name": "Sumatriptan", "zh": "舒马普坦",
        "brands": ["Imitrex", "Imigran", "英明格"],
        "cat": "OTHER", "sub": "TRIPTAN_5HT1B_1D", "atc": "N02CC01",
        "pk": {"f": 0.14, "ka": 1.0, "tmax": 2.0, "t12": 2.0, "t12r": [1, 3], "vdkg": 2.0, "pb": 14},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "MAO-A 代谢(非 CYP);65% 原型经尿排泄"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 25mg PO prn,最大 100mg/d", "smoking": None},
        "ints": [
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险(FDA 警示),虽然发生率低但应谨慎"},
            {"id": "snris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征;MAOI 停 14 天"},
            {"id": "ergotamine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "血管痉挛叠加,5-HT 双重激动;至少间隔 24h"},
            {"id": "propranolol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "联用可预防偏头痛,安全"}
        ],
        "mon": {"freq": "AS_NEEDED", "items": ["胸痛(冠状动脉痉挛)", "血压", "头痛频率(避免滥用)"]},
        "refs": ["AGNP 2017", "FDA Imitrex Prescribing Information"]
    },
    {
        "id": "flunarizine", "name": "Flunarizine", "zh": "氟桂利嗪",
        "brands": ["Sibelium", "西比灵"],
        "cat": "OTHER", "sub": "CALCIUM_CHANNEL_BLOCKER", "atc": "N07CA03",
        "pk": {"f": 0.9, "ka": 0.3, "tmax": 5.0, "t12": 432.0, "t12r": [300, 600], "vdkg": 50.0, "pb": 99},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "t½ 极长(18 天),达稳态需 2-3 月,停药后效应持续 5 周"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "MEDIUM"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "5-10mg qn;Beers 慎用(抑郁/锥体外系风险)", "smoking": None},
        "ints": [
            {"id": "antiparkinsonian", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 EPS 风险(氟桂利嗪可致帕金森综合征)"},
            {"id": "antipsychotics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 EPS/迟发性运动障碍风险"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加镇静"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP2D6 强抑制,浓度升高,EPS 风险"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["抑郁症状", "EPS 评估", "体重/BMI", "Beers 慎用(老年)"]},
        "refs": ["AGNP 2017", "FDA Sibelium Prescribing Information"]
    },
    # 肌松 (2)
    {
        "id": "baclofen", "name": "Baclofen", "zh": "巴氯芬",
        "brands": ["Lioresal", "力奥来素"],
        "cat": "OTHER", "sub": "GABA_B_AGONIST", "atc": "M03BX01",
        "pk": {"f": 0.85, "ka": 1.0, "tmax": 1.0, "t12": 3.5, "t12r": [2, 4], "vdkg": 0.8, "pb": 30},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "85% 肾原型排泄,肝代谢极小;鞘内给药绕过血脑屏障"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 5mg tid,缓滴定;Beers 慎用(老年肾清除降低)", "smoking": None},
        "ints": [
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加 CNS 抑制/呼吸抑制"},
            {"id": "antidepressants", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加镇静,尤其三环类"},
            {"id": "antihypertensives", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用可能低血压"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用呼吸抑制风险"},
            {"id": "abrupt_discontinuation", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "突然停药致幻觉/癫痫发作/戒断,必须缓慢减量"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肌张力", "镇静", "幻觉(尤其老年)", "肾功"]},
        "refs": ["AGNP 2017", "FDA Lioresal Prescribing Information"]
    },
    {
        "id": "tizanidine", "name": "Tizanidine", "zh": "替扎尼定",
        "brands": ["Zanaflex", "Sirdalud", "松得乐"],
        "cat": "OTHER", "sub": "ALPHA2_AGONIST", "atc": "M03BX02",
        "pk": {"f": 0.21, "ka": 1.5, "tmax": 1.5, "t12": 2.5, "t12r": [2, 4], "vdkg": 2.0, "pb": 30},
        "cyp": {"subs": [{"cyp": "CYP1A2", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "CYP1A2 主导,吸烟/苯巴比妥/卡马西平诱导降低浓度"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 2mg q6h,缓滴定", "smoking": "CYP1A2 诱导,需加剂量"},
        "ints": [
            {"id": "ciprofloxacin", "m": "CYP_INHIBITION_MODERATE", "af": [5.0, 10.0], "s": "CONTRAINDICATED", "n": "CYP1A2 抑制,血药浓度剧增,严重低血压/镇静"},
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [10.0, 33.0], "s": "CONTRAINDICATED", "n": "CYP1A2 强抑制,血药浓度升 10-33 倍,严重低血压"},
            {"id": "oral_contraceptives", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP1A2 抑制,浓度升高"},
            {"id": "antihypertensives", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加低血压/晕厥"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加 CNS 抑制"},
            {"id": "liver_failure_drugs", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "HIGH", "n": "肝毒性药物联用增加肝衰风险"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["BP(尤其直立)", "肝功", "镇静程度", "肌张力"]},
        "refs": ["AGNP 2017", "FDA Zanaflex Prescribing Information"]
    },
    # 抗眩晕 (2)
    {
        "id": "betahistine", "name": "Betahistine", "zh": "倍他司汀",
        "brands": ["Serc", "敏使朗"],
        "cat": "OTHER", "sub": "H1_AGONIST_H3_ANTAGONIST", "atc": "N07CA01",
        "pk": {"f": 0.97, "ka": 1.0, "tmax": 1.0, "t12": 3.5, "t12r": [2, 4], "vdkg": 1.0, "pb": 5},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "MAOB 代谢为无活性 2-pyridylacetic acid;无显著 CYP"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "起始 8-16mg tid,缓滴定", "smoking": None},
        "ints": [
            {"id": "antihistamines", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "H1 双重激动,可能增加镇静"},
            {"id": "MAOIs", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "LOW", "n": "倍他司汀被 MAOB 代谢,联用可能蓄积"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["眩晕/头晕日记", "肝功"]},
        "refs": ["AGNP 2017", "FDA Serc Prescribing Information"]
    },
    {
        "id": "meclizine", "name": "Meclizine", "zh": "美克洛嗪",
        "brands": ["Antivert", "Bonine", "美其敏"],
        "cat": "ANTIHISTAMINE", "sub": "FIRST_GEN_ANTIHISTAMINE", "atc": "R06AE05",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 3.0, "t12": 6.0, "t12r": [5, 8], "vdkg": 7.0, "pb": 75},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "肝代谢,但无显著 CYP 介导相互作用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 2, "agranulocytosis": "VERY_LOW", "extrapyramidal": "VERY_LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "12.5-25mg q6-8h prn;Beers 慎用(老年)", "smoking": None},
        "ints": [
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加抗胆碱能负荷"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加镇静"},
            {"id": "CNS_depressants", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 CNS 抑制"}
        ],
        "mon": {"freq": "AS_NEEDED", "items": ["镇静", "口干/便秘/视物模糊(抗胆碱能)", "老年认知/跌倒"]},
        "refs": ["AGNP 2017", "FDA Antivert Prescribing Information"]
    },
]

# ==================== 内分泌 + 激素扩充 (18) ====================
BATCH3 = [
    # 降糖 (4)
    {
        "id": "glipizide", "name": "Glipizide", "zh": "格列吡嗪",
        "brands": ["Glucotrol", "瑞易宁"],
        "cat": "ANTIDIABETIC", "sub": "SULFONYLUREA_SHORT", "atc": "A10BB07",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 1.0, "t12": 3.0, "t12r": [2, 4], "vdkg": 0.2, "pb": 98},
        "cyp": {"subs": [{"cyp": "CYP2C9", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "无活性代谢物,肾排泄"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 5mg qd,缓滴定", "smoking": None},
        "ints": [
            {"id": "fluconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2C9 强抑制,低血糖风险"},
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖/INR 改变"},
            {"id": "sulfonamides", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖风险"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "双硫仑样反应(罕见)"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["空腹血糖", "HbA1c", "低血糖症状", "肾功", "肝功"]},
        "refs": ["AGNP 2017", "FDA Glucotrol Prescribing Information"]
    },
    {
        "id": "sitagliptin", "name": "Sitagliptin", "zh": "西格列汀",
        "brands": ["Januvia", "捷诺维"],
        "cat": "ANTIDIABETIC", "sub": "DPP4_INHIBITOR", "atc": "A10BH01",
        "pk": {"f": 0.87, "ka": 0.7, "tmax": 2.0, "t12": 12.4, "t12r": [8, 16], "vdkg": 1.5, "pb": 38},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.3}, {"cyp": "CYP2C8", "fraction": 0.2}], "inh": [], "ind": [],
               "note": "80% 原型肾排泄,CYP 介导有限"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 100mg qd;eGFR 30-50: 50mg qd", "smoking": None},
        "ints": [
            {"id": "digoxin", "m": "PHARMACOKINETIC_OTHER", "af": [1.1, 1.3], "s": "LOW", "n": "P-gp 弱抑制,地高辛浓度略升"},
            {"id": "sulfonylureas", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖风险"},
            {"id": "insulin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖风险"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["空腹血糖", "HbA1c", "肾功(剂量调整)", "关节痛(罕见)"]},
        "refs": ["AGNP 2017", "FDA Januvia Prescribing Information"]
    },
    {
        "id": "liraglutide", "name": "Liraglutide", "zh": "利拉鲁肽",
        "brands": ["Victoza", "诺和力", "Saxenda(减肥)"],
        "cat": "ANTIDIABETIC", "sub": "GLP1_AGONIST", "atc": "A10BJ02",
        "pk": {"f": 0.55, "ka": 0.05, "tmax": 11.0, "t12": 13.0, "t12r": [11, 15], "vdkg": 0.2, "pb": 98},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "注射剂,皮下给药;被 DPP-IV 酶降解,无 CYP 介导"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "起始 0.6mg sc qd,1 周后加 1.2mg", "smoking": None},
        "ints": [
            {"id": "sulfonylureas", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖"},
            {"id": "insulin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖"},
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "LOW", "n": "INR 略升,监测"},
            {"id": "oral_contraceptives", "m": "PHARMACOKINETIC_OTHER", "af": [0.8, 0.9], "s": "LOW", "n": "延缓胃排空,可能减少吸收"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["空腹血糖", "HbA1c", "体重", "胃肠反应", "急性胰腺炎症状"]},
        "refs": ["AGNP 2017", "FDA Victoza Black Box Warning (甲状腺髓样癌/胰腺炎)"]
    },
    {
        "id": "pioglitazone", "name": "Pioglitazone", "zh": "吡格列酮",
        "brands": ["Actos", "艾可拓"],
        "cat": "ANTIDIABETIC", "sub": "THIAZOLIDINEDIONE", "atc": "A10BG03",
        "pk": {"f": 1.0, "ka": 0.7, "tmax": 2.0, "t12": 20.0, "t12r": [16, 24], "vdkg": 0.2, "pb": 99},
        "cyp": {"subs": [{"cyp": "CYP2C8", "fraction": 0.7}, {"cyp": "CYP3A4", "fraction": 0.3}], "inh": [{"cyp": "CYP2C8", "strength": "MODERATE"}], "ind": [],
               "note": "活性代谢物 M-IV(M-2/M-3)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 15mg qd,缓滴定;心衰 NYHA III-IV 禁用", "smoking": None},
        "ints": [
            {"id": "gemfibrozil", "m": "CYP_INHIBITION_STRONG", "af": [3.0, 5.0], "s": "HIGH", "n": "CYP2C8 强抑制,浓度剧增,低血糖风险"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.4, 0.6], "s": "MEDIUM", "n": "CYP2C8 强诱导,失效"},
            {"id": "insulin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加水钠潴留,心衰风险"},
            {"id": "sulfonylureas", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血糖风险"},
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加水钠潴留,急性心衰风险"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["体重(增加)", "水肿/心衰", "肝功", "骨折风险(绝经后女性)", "膀胱癌症状(血尿)"]},
        "refs": ["AGNP 2017", "FDA Actos Black Box Warning (心衰)"]
    },
    # 抗甲状腺 (2)
    {
        "id": "methimazole", "name": "Methimazole", "zh": "甲巯咪唑",
        "brands": ["Tapazole", "赛治"],
        "cat": "THYROID", "sub": "ANTITHYROID_TIONAMIDE", "atc": "H03BB02",
        "pk": {"f": 0.93, "ka": 1.5, "tmax": 1.0, "t12": 6.0, "t12r": [4, 8], "vdkg": 0.5, "pb": 5},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "几乎无 CYP 介导;治疗 Graves 病首选(除妊娠早期)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "HIGH", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 15-30mg qd(分次),维持 5-15mg qd", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "甲亢治疗时 INR 升高,需减抗凝;甲亢未控时 INR 反而低"},
            {"id": "digoxin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "甲亢治疗时地高辛浓度降低"},
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "联用控制心率,常规组合"},
            {"id": "agranulocytosis_inducing_drugs", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加粒细胞缺乏风险(氯霉素/磺胺等)谨慎联用"}
        ],
        "mon": {"freq": "MONTHLY_INITIALLY", "items": ["WBC(尤其粒细胞)", "肝功", "TSH/FT3/FT4", "甲状腺功能", "皮疹"]},
        "refs": ["AGNP 2017", "FDA Tapazole Prescribing Information (含肝毒性黑框)"]
    },
    {
        "id": "propylthiouracil", "name": "Propylthiouracil", "zh": "丙硫氧嘧啶",
        "brands": ["PTU"],
        "cat": "THYROID", "sub": "ANTITHYROID_TIONAMIDE", "atc": "H03BA02",
        "pk": {"f": 0.77, "ka": 1.0, "tmax": 1.5, "t12": 1.5, "t12r": [1, 2], "vdkg": 0.4, "pb": 80},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "FDA 限制使用:仅限甲巯咪唑不耐受/妊娠早期/甲状腺危象"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "HIGH", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 300-400mg qd,维持 100-150mg qd", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "INR 变化(双向,密切监测)"},
            {"id": "digoxin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "甲亢治疗时地高辛浓度变化"},
            {"id": "lithium", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加甲状腺功能减退风险,锂本身可致甲减"}
        ],
        "mon": {"freq": "MONTHLY_INITIALLY", "items": ["WBC(粒细胞缺乏)", "肝功(肝毒性,FDA 黑框)", "TSH/FT3/FT4", "ANCA(罕见血管炎)"]},
        "refs": ["AGNP 2017", "FDA PTU Black Box Warning (急性肝坏死)"]
    },
    # 糖皮质激素 (5)
    {
        "id": "prednisone", "name": "Prednisone", "zh": "泼尼松",
        "brands": ["Deltasone", "Rayos", "强的松"],
        "cat": "CORTICOSTEROID", "sub": "GLUCOCORTICOID_INTERMEDIATE", "atc": "H02AB07",
        "pk": {"f": 0.8, "ka": 1.0, "tmax": 1.5, "t12": 3.0, "t12r": [2, 4], "vdkg": 0.5, "pb": 90},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "前药:11-β-HSD 在肝内转化为活性泼尼松龙(prednisolone)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "HIGH", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 5-60mg qd(疾病决定);长期用最小有效剂量", "smoking": None},
        "ints": [
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加胃溃疡/出血风险"},
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用 INR 变化(双向)"},
            {"id": "vaccines_live", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "免疫抑制下接种活疫苗可能致严重感染,禁忌"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP3A4 强抑制(酮康唑等)降低泼尼松清除,需减量"},
            {"id": "CYP3A4_strong_inducers", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4 强诱导(利福平等)加速代谢,需加量"},
            {"id": "diabetes_drugs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "糖皮质激素升高血糖,需调降糖药"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血压", "血糖", "体重", "骨质密度(长期)", "白内障/青光眼(长期)", "感染迹象"]},
        "refs": ["AGNP 2017", "FDA Deltasone Prescribing Information"]
    },
    {
        "id": "prednisolone", "name": "Prednisolone", "zh": "泼尼松龙",
        "brands": ["Orapred", "Pediapred", "强的松龙"],
        "cat": "CORTICOSTEROID", "sub": "GLUCOCORTICOID_ACTIVE", "atc": "H02AB06",
        "pk": {"f": 0.9, "ka": 1.0, "tmax": 1.5, "t12": 18.0, "t12r": [12, 36], "vdkg": 0.7, "pb": 90},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "泼尼松活性形式(无需 11-β-HSD 转化),肝损时优选"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "HIGH", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "5-60mg qd", "smoking": None},
        "ints": [
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加胃溃疡/出血风险"},
            {"id": "vaccines_live", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "免疫抑制禁忌活疫苗"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "需减量"},
            {"id": "CYP3A4_strong_inducers", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "需加量"},
            {"id": "diabetes_drugs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "升高血糖,需调降糖药"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血压", "血糖", "体重", "骨质密度(长期)", "感染迹象"]},
        "refs": ["AGNP 2017", "FDA Orapred Prescribing Information"]
    },
    {
        "id": "hydrocortisone", "name": "Hydrocortisone", "zh": "氢化可的松",
        "brands": ["Cortef", "Solu-Cortef", "氢化可的松"],
        "cat": "CORTICOSTEROID", "sub": "GLUCOCORTICOID_NATURAL", "atc": "H02AB09",
        "pk": {"f": 0.96, "ka": 1.0, "tmax": 1.0, "t12": 1.5, "t12r": [1, 2], "vdkg": 0.4, "pb": 90},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "内源性糖皮质激素(皮质醇)等同物;生理替代用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "HIGH", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "生理替代 15-25mg qd(分次);应激 50-100mg IV", "smoking": None},
        "ints": [
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加胃溃疡/出血"},
            {"id": "vaccines_live", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "免疫抑制禁忌活疫苗"},
            {"id": "diabetes_drugs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "升高血糖,需调降糖药"},
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用 INR 变化"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血压", "血糖", "电解质(K+/Na+)", "体重", "骨质密度(长期)"]},
        "refs": ["AGNP 2017", "FDA Cortef Prescribing Information"]
    },
    {
        "id": "dexamethasone", "name": "Dexamethasone", "zh": "地塞米松",
        "brands": ["Decadron", "Dexamethasone Intensol", "德沙美松"],
        "cat": "CORTICOSTEROID", "sub": "GLUCOCORTICOID_LONG", "atc": "H02AB02",
        "pk": {"f": 0.86, "ka": 1.0, "tmax": 1.5, "t12": 36.0, "t12r": [24, 54], "vdkg": 0.8, "pb": 77},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "无盐皮质激素活性;t½ 长;无 HPA 抑制可作肾上腺功能测试"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "HIGH", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 0.5-9mg qd,疾病决定", "smoking": None},
        "ints": [
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加胃溃疡/出血"},
            {"id": "vaccines_live", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "免疫抑制禁忌活疫苗"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "酮康唑等抑制代谢,需减量"},
            {"id": "CYP3A4_strong_inducers", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "利福平等诱导代谢,需加量"},
            {"id": "diabetes_drugs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "显著升高血糖"},
            {"id": "mifepristone", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "抗孕激素作用拮抗,联用降糖效果"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血压", "血糖", "电解质", "体重", "骨质密度(长期)", "感染迹象"]},
        "refs": ["AGNP 2017", "FDA Decadron Prescribing Information"]
    },
    {
        "id": "methylprednisolone", "name": "Methylprednisolone", "zh": "甲泼尼龙",
        "brands": ["Medrol", "Solu-Medrol", "美卓乐"],
        "cat": "CORTICOSTEROID", "sub": "GLUCOCORTICOID_INTERMEDIATE", "atc": "H02AB04",
        "pk": {"f": 0.98, "ka": 1.0, "tmax": 1.5, "t12": 24.0, "t12r": [18, 36], "vdkg": 0.7, "pb": 78},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.4}], "inh": [], "ind": [],
               "note": "无盐皮质激素活性;脉冲大剂量治疗 MS/IBD 常用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "HIGH", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "4-48mg qd;大剂量脉冲 500-1000mg IV qd ×3-5d", "smoking": None},
        "ints": [
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加胃溃疡/出血"},
            {"id": "vaccines_live", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "免疫抑制禁忌活疫苗"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "需减量"},
            {"id": "diabetes_drugs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "升高血糖,需调降糖药"},
            {"id": "mycophenolate", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加免疫抑制,感染风险"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血压", "血糖", "电解质", "体重", "骨质密度", "感染迹象"]},
        "refs": ["AGNP 2017", "FDA Medrol Prescribing Information"]
    },
    # 抗骨质疏松 (2)
    {
        "id": "alendronate", "name": "Alendronate", "zh": "阿仑膦酸钠",
        "brands": ["Fosamax", "福善美"],
        "cat": "OTHER", "sub": "BISPHOSPHONATE", "atc": "M05BA04",
        "pk": {"f": 0.006, "ka": 1.0, "tmax": 1.0, "t12": 87600.0, "t12r": [1000, 100000], "vdkg": 0.15, "pb": 78},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "吸收极差(~0.6%),必须空腹直立位服;骨内半衰期 >10 年"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "CONTRAINDICATED_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "70mg qd(口服)或 70mg IV q4w;Beers 慎用", "smoking": None},
        "ints": [
            {"id": "calcium_supplements", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "MEDIUM", "n": "钙剂螯合,降低阿仑膦酸吸收,需间隔至少 30 min"},
            {"id": "iron_sulfate", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "MEDIUM", "n": "铁剂螯合,需间隔"},
            {"id": "antacids", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "MEDIUM", "n": "含铝/镁抗酸剂降低吸收,需间隔 2h"},
            {"id": "PPIs", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.6], "s": "MEDIUM", "n": "抑酸降低阿仑膦酸吸收,需间隔 30 min"},
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 GI 不良反应"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["BMD(DXA 1-2 年)", "血钙/磷", "肾功能", "颌骨坏死症状(牙痛/口疮)"]},
        "refs": ["AGNP 2017", "FDA Fosamax Prescribing Information"]
    },
    {
        "id": "risedronate", "name": "Risedronate", "zh": "利塞膦酸钠",
        "brands": ["Actonel", "Atelvia", "易维特"],
        "cat": "OTHER", "sub": "BISPHOSPHONATE", "atc": "M05BA07",
        "pk": {"f": 0.006, "ka": 0.5, "tmax": 1.0, "t12": 480.0, "t12r": [300, 1000], "vdkg": 0.2, "pb": 24},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "与阿仑膦酸类似,空肠吸收"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "CONTRAINDICATED_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "35mg qw(口服)", "smoking": None},
        "ints": [
            {"id": "calcium_supplements", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "MEDIUM", "n": "钙剂螯合,需间隔 30 min"},
            {"id": "iron_sulfate", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "MEDIUM", "n": "铁剂螯合,需间隔"},
            {"id": "antacids", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "MEDIUM", "n": "含铝/镁抗酸剂降低吸收"},
            {"id": "PPIs", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.6], "s": "MEDIUM", "n": "抑酸降低吸收,需间隔 30 min"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["BMD", "血钙/磷", "肾功能"]},
        "refs": ["AGNP 2017", "FDA Actonel Prescribing Information"]
    },
    # 抗雄激素 (2)
    {
        "id": "finasteride", "name": "Finasteride", "zh": "非那雄胺",
        "brands": ["Proscar", "Propecia", "保列治", "保法止"],
        "cat": "OTHER", "sub": "5_ALPHA_REDUCTASE_INHIBITOR", "atc": "G04CB01",
        "pk": {"f": 0.65, "ka": 0.5, "tmax": 2.0, "t12": 6.0, "t12r": [4, 12], "vdkg": 0.7, "pb": 90},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "1mg(脱发)与 5mg(BPH)剂量不同;孕妇避免接触(致男胎生殖器畸形)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "BPH 5mg qd;脱发 1mg qd", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "LOW", "n": "CYP3A4 强抑制略升非那雄胺浓度"},
            {"id": "saw_palmetto", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "机制相似(5-α还原酶),联用作用叠加,可能副作用也叠加"},
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "BPH 治疗,联用改善症状"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["PSA(降低 50% 需调整)", "性功能", "肝功", "乳腺增生"]},
        "refs": ["AGNP 2017", "FDA Proscar/Propecia Prescribing Information"]
    },
    {
        "id": "dutasteride", "name": "Dutasteride", "zh": "度他雄胺",
        "brands": ["Avodart", "适必达"],
        "cat": "OTHER", "sub": "5_ALPHA_REDUCTASE_INHIBITOR_DUAL", "atc": "G04CB02",
        "pk": {"f": 0.6, "ka": 0.5, "tmax": 2.0, "t12": 840.0, "t12r": [168, 1680], "vdkg": 5.0, "pb": 99},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "双重抑制 5-α-还原酶 1/2 型;t½ 极长(5 周),停药后 6 月精液仍有"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "0.5mg qd", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "LOW", "n": "浓度升高"},
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "BPH 联用改善症状"},
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "LOW", "n": "INR 略升,监测"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["PSA(降低 50%)", "性功能", "肝功", "男性乳腺癌症状"]},
        "refs": ["AGNP 2017", "FDA Avodart Prescribing Information"]
    },
    # 性激素 (2)
    {
        "id": "estradiol", "name": "Estradiol", "zh": "雌二醇",
        "brands": ["Estrace", "Climara", "Vivelle-Dot", "补佳乐"],
        "cat": "OTHER", "sub": "ESTROGEN", "atc": "G03CA03",
        "pk": {"f": 0.05, "ka": 0.5, "tmax": 4.0, "t12": 16.0, "t12r": [12, 24], "vdkg": 4.0, "pb": 98},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP1A2", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "肝肠循环显著;贴片/凝胶可避免首过"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "MEDIUM"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始最低有效剂量(贴片 0.025-0.05mg/d)", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "VTE 风险 ↑,INR 可能 ↑"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP3A4 强抑制升高雌二醇,副作用增加"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4 强诱导,浓度降低,失效"},
            {"id": "thyroid_hormone", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "增加 TBG,可能需要增加甲状腺素剂量"},
            {"id": "tamoxifen", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低 tamoxifen 效(CYP 通路竞争)"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["血压", "肝功", "血脂", "乳腺体检", "VTE 症状", "子宫内膜(子宫未切者)"]},
        "refs": ["AGNP 2017", "FDA Estrace Prescribing Information Black Box Warning (VTE/乳腺癌/子宫内膜癌)"]
    },
    {
        "id": "progesterone", "name": "Progesterone", "zh": "孕酮",
        "brands": ["Prometrium", "Crinone", "Utrogestan", "益玛欣"],
        "cat": "OTHER", "sub": "PROGESTIN_NATURAL", "atc": "G03DA04",
        "pk": {"f": 0.1, "ka": 0.5, "tmax": 3.0, "t12": 16.0, "t12r": [8, 36], "vdkg": 1.5, "pb": 96},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}, {"cyp": "CYP2C19", "fraction": 0.2}], "inh": [], "ind": [],
               "note": "首过效应显著;口服剂型应睡前服用(镇静副作用)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "100-200mg qhs;HRT 周期使用", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "INR 可能升高(尤其启动时)"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "酮康唑等升高孕酮浓度"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,避孕效降低"},
            {"id": "phenytoin", "m": "CYP_INDUCTION", "af": [0.4, 0.6], "s": "HIGH", "n": "CYP 强诱导,孕酮代谢加速"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 CNS 抑制"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["血压", "肝功", "血脂", "VTE 症状", "乳腺检查"]},
        "refs": ["AGNP 2017", "FDA Prometrium Prescribing Information Black Box Warning (VTE/CVD)"]
    },
    # 抗利尿 (1)
    {
        "id": "desmopressin", "name": "Desmopressin", "zh": "去氨加压素",
        "brands": ["DDAVP", "Minirin", "弥凝"],
        "cat": "OTHER", "sub": "VASOPRESSIN_ANALOG", "atc": "H01BA02",
        "pk": {"f": 0.05, "ka": 0.5, "tmax": 1.0, "t12": 2.5, "t12r": [2, 3], "vdkg": 0.2, "pb": 0},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "抗利尿激素 V2 受体激动剂;无血管加压作用;不通过胎盘"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "NONE", "elderly": "起始 0.1mg qhs,缓滴定;尿崩症/夜间遗尿", "smoking": None},
        "ints": [
            {"id": "NSAIDs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加水潴留/低钠血症(尤其吲哚美辛)"},
            {"id": "SSRIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "SSRI 本身可致 SIADH/低钠,联用增加低钠血症风险"},
            {"id": "carbamazepine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用可致 SIADH,加重低钠"},
            {"id": "thiazide_diuretics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低钠血症风险"},
            {"id": "loperamide", "m": "PHARMACOKINETIC_OTHER", "af": [3.0, 5.0], "s": "HIGH", "n": "P-gp 抑制,去氨加压素浓度剧增(口服制剂),低钠血症风险"}
        ],
        "mon": {"freq": "WEEKLY_INITIALLY", "items": ["血钠(尤其启动/加量时)", "血渗", "尿量/尿比重"]},
        "refs": ["AGNP 2017", "FDA DDAVP Prescribing Information"]
    },
]

# ==================== 特殊 / 其他 (14) ====================
BATCH4 = [
    {
        "id": "naltrexone", "name": "Naltrexone", "zh": "纳曲酮",
        "brands": ["ReVia", "Vivitrol"],
        "cat": "OTHER", "sub": "OPIOID_ANTAGONIST", "atc": "N07BB04",
        "pk": {"f": 0.96, "ka": 0.5, "tmax": 1.0, "t12": 4.0, "t12r": [3, 6], "vdkg": 16.0, "pb": 21},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "活性代谢物 6-β-naltrexol;几乎不经 CYP(还原为 dihydrodiol)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "25-50mg qd;Vivitrol 380mg IM q4w", "smoking": None},
        "ints": [
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "μ 受体拮抗,可能诱发阿片戒断;戒毒/戒酒前至少停 7-10 天无阿片"},
            {"id": "thioridazine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加镇静"},
            {"id": "yohimbine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加焦虑/血压"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功(尤其启动 3 月内)", "自杀倾向", "戒断症状"]},
        "refs": ["AGNP 2017", "FDA ReVia/Vivitrol Prescribing Information (含肝毒性黑框)"]
    },
    {
        "id": "buprenorphine", "name": "Buprenorphine", "zh": "丁丙诺啡",
        "brands": ["Subutex", "Suboxone", "Temgesic", "舒普诺"],
        "cat": "ANALGESIC", "sub": "OPIOID_PARTIAL_AGONIST", "atc": "N07BC01",
        "pk": {"f": 0.35, "ka": 0.5, "tmax": 1.5, "t12": 33.0, "t12r": [24, 60], "vdkg": 12.0, "pb": 96},
        "win": {"low": 0.5, "high": 2.0, "unit": "ng/mL", "guidelineSource": "EAPC 2014"},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "活性代谢物 norbuprenorphine(CYP3A4);舌下/贴片可避免首过"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "起始 2-4mg 舌下 qd,缓滴定;肝损患者慎用", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "CYP3A4 强抑制,血药浓度剧增,呼吸抑制风险"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,失效,戒断风险"},
            {"id": "benzodiazepines", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用呼吸抑制/致死(尤其是舌下片 + BZD)"},
            {"id": "naloxone", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用降低丁丙诺啡效,可能戒断"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用呼吸抑制/致死"},
            {"id": "QTc_drugs", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "丁丙诺啡单药低 QT 风险,但高剂量或联用 QTc 药需注意"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功", "呼吸功能", "成瘾/滥用迹象", "QTc(高剂量/联用时)", "注射部位反应(Vivitrol)"]},
        "refs": ["AGNP 2017", "FDA Suboxone/Subutex Prescribing Information"]
    },
    {
        "id": "methadone", "name": "Methadone", "zh": "美沙酮",
        "brands": ["Methadone", "Dolophine", "美沙酮"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N07BC02",
        "pk": {"f": 0.7, "ka": 0.7, "tmax": 2.5, "t12": 30.0, "t12r": [8, 59], "vdkg": 6.0, "pb": 85},
        "win": {"low": 200.0, "high": 600.0, "unit": "ng/mL", "guidelineSource": "EAPC 2014"},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2B6", "fraction": 0.3}, {"cyp": "CYP2D6", "fraction": 0.1}], "inh": [], "ind": [],
               "note": "R-美沙酮是 μ 受体激动剂;S-美沙酮是 NMDA 拮抗;t½ 个体差异极大"},
        "ae": {"qtcProlongation": "HIGH", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 2.5mg q8-12h,缓滴定", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 强抑制,呼吸抑制"},
            {"id": "efavirenz", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,戒断风险"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,戒断"},
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2D6 强抑制,血药浓度升高"},
            {"id": "QTc_drugs", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积,尖端扭转风险(联用氟哌啶醇/索他洛尔/胺碘酮)"},
            {"id": "benzodiazepines", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "呼吸抑制/致死(经典致死组合)"},
            {"id": "naloxone", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用降低美沙酮效,戒断"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用呼吸抑制/致死"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(QTc)", "肝功", "呼吸功能", "滥用/成瘾迹象", "美沙酮血药浓度"]},
        "refs": ["AGNP 2017", "FDA Methadone Prescribing Information (含 QTc 黑框)"]
    },
    {
        "id": "fluconazole", "name": "Fluconazole", "zh": "氟康唑",
        "brands": ["Diflucan", "三维康"],
        "cat": "OTHER", "sub": "AZOLE_ANTIFUNGAL", "atc": "J02AC01",
        "pk": {"f": 0.9, "ka": 0.7, "tmax": 2.0, "t12": 30.0, "t12r": [20, 50], "vdkg": 0.7, "pb": 11},
        "cyp": {"subs": [{"cyp": "CYP2C9", "fraction": 0.5}], "inh": [{"cyp": "CYP2C9", "strength": "STRONG"}, {"cyp": "CYP3A4", "strength": "MODERATE"}], "ind": [],
               "note": "肾原型排泄 80%,CYP 介导有限,但抑制作用强"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 100mg qd;严重感染负荷剂量 400-800mg", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.5], "s": "HIGH", "n": "CYP2C9 强抑制,INR 显著升高,出血风险(氟康唑最危险的相互作用之一)"},
            {"id": "phenytoin", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2C9 强抑制,苯妥英中毒"},
            {"id": "cyclosporine", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 中度抑制,环孢素浓度升高,肾毒性"},
            {"id": "tacrolimus", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 中度抑制,FK506 浓度升高"},
            {"id": "sulfonylureas", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2C9 强抑制,低血糖"},
            {"id": "statins", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP3A4 中度抑制,他汀浓度升高,横纹肌溶解风险(尤其辛伐他汀)"},
            {"id": "methadone", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 抑制,美沙酮浓度升高,呼吸抑制"},
            {"id": "ondansetron", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积"},
            {"id": "amiodarone", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积,尖端扭转风险"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功", "肾功能(剂量调整)", "心电图(QTc 大剂量时)", "血钾/镁(高剂量)"]},
        "refs": ["AGNP 2017", "FDA Diflucan Prescribing Information"]
    },
    {
        "id": "rifampin", "name": "Rifampin", "zh": "利福平",
        "brands": ["Rifadin", "Rimactane", "利福平"],
        "cat": "OTHER", "sub": "RIFAMYCIN_ANTIBIOTIC", "atc": "J04AB02",
        "pk": {"f": 0.5, "ka": 1.5, "tmax": 2.0, "t12": 3.0, "t12r": [2, 5], "vdkg": 1.0, "pb": 80},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [{"cyp": "CYP3A4", "strength": "STRONG"}, {"cyp": "CYP2C9", "strength": "STRONG"}, {"cyp": "CYP2C19", "strength": "STRONG"}, {"cyp": "CYP2B6", "strength": "MODERATE"}, {"cyp": "UGT1A1", "strength": "MODERATE"}],
               "note": "全球最强的 CYP 诱导剂之一;体液变橙色/红色无害(泪/汗/尿);P-gp 诱导"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 300-600mg qd(结核),缓滴定", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP 强诱导,INR 显著降低,需加抗凝剂量"},
            {"id": "clozapine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,氯氮平浓度显著降低,复发风险"},
            {"id": "olanzapine", "m": "CYP_INDUCTION", "af": [0.4, 0.6], "s": "MEDIUM", "n": "CYP 诱导,失效"},
            {"id": "oral_contraceptives", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP 强诱导,避孕失效,建议用屏障避孕"},
            {"id": "methadone", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,美沙酮戒断风险"},
            {"id": "cyclosporine", "m": "CYP_INDUCTION", "af": [0.2, 0.4], "s": "HIGH", "n": "CYP3A4 + P-gp 强诱导,环孢素浓度剧降,排斥风险"},
            {"id": "tacrolimus", "m": "CYP_INDUCTION", "af": [0.2, 0.4], "s": "HIGH", "n": "CYP3A4 强诱导,FK506 浓度剧降"},
            {"id": "HIV_protease_inhibitors", "m": "CYP_INDUCTION", "af": [0.1, 0.3], "s": "CONTRAINDICATED", "n": "PI 类抗 HIV 浓度剧降,治疗失败"},
            {"id": "dihydroergotamine", "m": "CYP_INDUCTION", "af": [0.2, 0.4], "s": "HIGH", "n": "CYP 诱导 + 药效学叠加,麦角中毒风险"},
            {"id": "lurasidone", "m": "CYP_INDUCTION", "af": [0.1, 0.3], "s": "CONTRAINDICATED", "n": "CYP3A4 强诱导,失效"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功(尤其启动)", "WBC", "血尿酸", "视物颜色(橘色泪/汗无害,但视物变化需评估)", "CYP 诱导相关药物浓度"]},
        "refs": ["AGNP 2017", "FDA Rifadin Prescribing Information"]
    },
    {
        "id": "dolutegravir", "name": "Dolutegravir", "zh": "多替拉韦",
        "brands": ["Tivicay", "Triumeq(复方)", "特威凯"],
        "cat": "OTHER", "sub": "HIV_INTEGRASE_INHIBITOR", "atc": "J05AJ03",
        "pk": {"f": 0.33, "ka": 0.5, "tmax": 2.5, "t12": 14.0, "t12r": [11, 17], "vdkg": 0.3, "pb": 99},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.3}, {"cyp": "UGT1A1", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "UGT1A1 主导,部分 CYP3A4;CYP 相互作用少,但 P-gp/BCRP/OCT2/MATE 抑制"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "50mg qd(初治);INSTI 耐药 50mg bid", "smoking": None},
        "ints": [
            {"id": "dofetilide", "m": "PHARMACOKINETIC_OTHER", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "OCT2/MATE 抑制,多非利特浓度剧增,QTc 延长"},
            {"id": "metformin", "m": "PHARMACOKINETIC_OTHER", "af": [1.5, 2.0], "s": "MEDIUM", "n": "OCT2/MATE 抑制,二甲双胍浓度升高"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4/UGT1A1 诱导,浓度降低,INSTI 耐药风险"},
            {"id": "antacids_cation", "m": "PHARMACOKINETIC_OTHER", "af": [0.2, 0.5], "s": "HIGH", "n": "Mg/Al/Ca 多价阳离子螯合,降低吸收,需间隔 2-6h"},
            {"id": "sucralfate", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.7], "s": "MEDIUM", "n": "含 Al 多价阳离子,降低吸收"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4/UGT 诱导"},
            {"id": "phenytoin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP 诱导"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["HIV 病毒载量", "CD4", "肝功", "肾功能", "体重", "神经精神症状"]},
        "refs": ["AGNP 2017", "FDA Tivicay Prescribing Information"]
    },
    {
        "id": "zaleplon", "name": "Zaleplon", "zh": "扎来普隆",
        "brands": ["Sonata", "柠眠"],
        "cat": "ANXIOLYTIC", "sub": "NON_BZD_Z_DRUG_SHORT", "atc": "N05CF03",
        "pk": {"f": 0.3, "ka": 1.5, "tmax": 1.0, "t12": 1.0, "t12r": [0.9, 1.1], "vdkg": 1.3, "pb": 60},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "t½ 极短(1h),仅维持睡眠起始;醛氧化酶代谢"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 5mg qn,最大 10mg", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP3A4 强抑制,血药浓度升高,过度镇静"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4 强诱导,失效"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加 CNS 抑制"},
            {"id": "CYP3A4_modulators", "m": "CYP_INHIBITION_MODERATE", "af": [1.2, 1.4], "s": "LOW", "n": "中度抑制/诱导略影响浓度"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["依赖性(长期)", "复杂睡眠行为", "次晨镇静", "肝功"]},
        "refs": ["AGNP 2017", "FDA Sonata Prescribing Information"]
    },
    {
        "id": "eszopiclone", "name": "Eszopiclone", "zh": "右佐匹克隆",
        "brands": ["Lunesta", "依曲诺"],
        "cat": "ANXIOLYTIC", "sub": "NON_BZD_Z_DRUG", "atc": "N05CF04",
        "pk": {"f": 0.75, "ka": 1.0, "tmax": 1.0, "t12": 6.0, "t12r": [5, 8], "vdkg": 1.4, "pb": 55},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.6}, {"cyp": "CYP2E1", "fraction": 0.4}], "inh": [], "ind": [],
               "note": "S-佐匹克隆异构体(活性);可长期使用,FDA 批准无疗程限制"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 1mg qn,最大 2-3mg", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 2.5], "s": "HIGH", "n": "CYP3A4 强抑制,血药浓度剧增"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP3A4 强诱导,失效"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加 CNS 抑制"},
            {"id": "olanzapine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加次日镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["依赖性(长期)", "次晨镇静/口苦", "复杂睡眠行为"]},
        "refs": ["AGNP 2017", "FDA Lunesta Prescribing Information"]
    },
    {
        "id": "acamprosate", "name": "Acamprosate", "zh": "阿坎酸",
        "brands": ["Campral", "坎普拉尔"],
        "cat": "OTHER", "sub": "GLUTAMATE_MODULATOR", "atc": "N07BB03",
        "pk": {"f": 0.11, "ka": 0.3, "tmax": 4.0, "t12": 26.0, "t12r": [20, 33], "vdkg": 0.5, "pb": 0},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "100% 肾原型排泄,无 CYP 介导,无显著药物相互作用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "666mg tid", "smoking": None},
        "ints": [
            {"id": "naltrexone", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "联用安全,可能协同降低渴求"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肾功能(剂量调整)", "腹泻", "抑郁/自杀倾向"]},
        "refs": ["AGNP 2017", "FDA Campral Prescribing Information"]
    },
    {
        "id": "disulfiram", "name": "Disulfiram", "zh": "双硫仑",
        "brands": ["Antabuse", "戒酒硫"],
        "cat": "OTHER", "sub": "ALDEHYDE_DEHYDROGENASE_INHIBITOR", "atc": "N07BB01",
        "pk": {"f": 0.8, "ka": 0.5, "tmax": 12.0, "t12": 90.0, "t12r": [60, 120], "vdkg": 5.0, "pb": 96},
        "cyp": {"subs": [], "inh": [{"cyp": "CYP2E1", "strength": "MODERATE"}], "ind": [],
               "note": "ALDH 不可逆抑制,作用持续至酶重新合成(7-14 天);CYP2E1 抑制"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 250mg qd,缓滴定", "smoking": None},
        "ints": [
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "双硫仑反应:严重恶心/呕吐/心悸/低血压,可能致命;治疗核心机制"},
            {"id": "metronidazole", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用也可致双硫仑样反应,患者需避免酒精"},
            {"id": "warfarin", "m": "CYP2E1_INHIBITION", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2E1 抑制,INR 升高,出血风险"},
            {"id": "phenytoin", "m": "CYP2E1_INHIBITION", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2E1 抑制,苯妥英中毒"},
            {"id": "benzodiazepines", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 CNS 抑制"},
            {"id": "isoniazid", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加神经/精神副作用"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功(尤其启动 2 月)", "WBC", "神经精神症状", "酒精摄入迹象"]},
        "refs": ["AGNP 2017", "FDA Antabuse Prescribing Information"]
    },
    {
        "id": "modafinil", "name": "Modafinil", "zh": "莫达非尼",
        "brands": ["Provigil", "Nuvigil", "Alertec"],
        "cat": "STIMULANT", "sub": "WAKING_PROMOTING", "atc": "N06BA07",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 2.5, "t12": 13.0, "t12r": [10, 15], "vdkg": 0.9, "pb": 60},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [{"cyp": "CYP2C19", "strength": "MODERATE"}], "ind": [{"cyp": "CYP3A4", "strength": "STRONG"}],
               "note": "时间依赖性 CYP3A4 诱导(连续 7-10 天后最强);也轻度诱导 1A2/2B6"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 100-200mg qd,最大 400mg/d", "smoking": None},
        "ints": [
            {"id": "oral_contraceptives", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "HIGH", "n": "CYP3A4 诱导,降低避孕效,建议屏障避孕"},
            {"id": "cyclosporine", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "HIGH", "n": "CYP3A4 诱导,环孢素浓度降低,排斥风险"},
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "INR 变化,监测"},
            {"id": "clomipramine", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP2C19 中度抑制,氯米帕明浓度升高"},
            {"id": "diazepam", "m": "CYP_INHIBITION_MODERATE", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP2C19 中度抑制,地西泮清除减慢"},
            {"id": "anticonvulsants", "m": "CYP_INDUCTION", "af": [0.5, 0.8], "s": "MEDIUM", "n": "CYP 诱导,降低苯妥英/卡马西平/丙戊酸/苯巴比妥浓度"}
        ],
        "mon": {"freq": "EVERY_6_MONTHS", "items": ["血压", "心率", "精神症状(尤其精神病史)", "肝功", "皮疹(SJS)"]},
        "refs": ["AGNP 2017", "FDA Provigil Prescribing Information"]
    },
    {
        "id": "calcium_carbonate", "name": "Calcium Carbonate", "zh": "碳酸钙",
        "brands": ["Tums", "Caltrate", "钙尔奇"],
        "cat": "OTHER", "sub": "CALCIUM_SUPPLEMENT_ANTACID", "atc": "A02AC01",
        "pk": {"f": 0.3, "ka": 0.5, "tmax": 1.0, "t12": 6.0, "t12r": [3, 8], "vdkg": 1.0, "pb": 40},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "Ca2+ 经小肠吸收,VDR 调节;碳酸氢根中和胃酸"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "NONE", "elderly": "500-1500mg Ca2+/d(分次,与餐同服)", "smoking": None},
        "ints": [
            {"id": "bisphosphonates", "m": "PHARMACOKINETIC_OTHER", "af": [0.1, 0.3], "s": "HIGH", "n": "钙剂螯合阿仑膦酸/利塞膦酸,降低吸收,需间隔 30-60 min"},
            {"id": "levothyroxine", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "钙剂螯合甲状腺素,降低吸收,需间隔 4h"},
            {"id": "iron_sulfate", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "钙/铁竞争吸收,需间隔 2h"},
            {"id": "tetracyclines", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "多价阳离子螯合,降低四环素/喹诺酮吸收"},
            {"id": "PPIs", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.7], "s": "MEDIUM", "n": "抑酸降低 CaCO3 转离子 Ca2+ 吸收"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血钙", "便秘(尤其老年)", "肾结石病史", "Vd 监测(骨密度)"]},
        "refs": ["AGNP 2017", "FDA Tums Prescribing Information"]
    },
    {
        "id": "ferrous_sulfate", "name": "Ferrous Sulfate", "zh": "硫酸亚铁",
        "brands": ["Feosol", "Slow Fe", "速力菲"],
        "cat": "OTHER", "sub": "IRON_SUPPLEMENT", "atc": "B03AA07",
        "pk": {"f": 0.1, "ka": 1.0, "tmax": 2.0, "t12": 6.0, "t12r": [4, 8], "vdkg": 1.0, "pb": 95},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "Fe2+ 经 DMT1 转运,胃酸促进吸收,需空腹或维 C 同服"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "100-200mg Fe2+/d(分次,与维 C 同服)", "smoking": None},
        "ints": [
            {"id": "calcium_supplements", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "钙/铁竞争吸收,需间隔 2h"},
            {"id": "tetracyclines", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "多价阳离子螯合,降低四环素/喹诺酮吸收,需间隔 2-3h"},
            {"id": "bisphosphonates", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "HIGH", "n": "铁剂螯合,降低阿仑膦酸吸收,需间隔"},
            {"id": "levothyroxine", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "铁剂螯合,需间隔 4h"},
            {"id": "PPIs", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "HIGH", "n": "抑酸显著降低铁吸收(Fe2+ 需要酸性环境)"},
            {"id": "entacapone", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.6], "s": "MEDIUM", "n": "铁剂螯合,降低恩他卡朋吸收"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["血红蛋白/铁蛋白", "便秘/胃肠反应", "牙齿染色(液体制剂)"]},
        "refs": ["AGNP 2017", "FDA Feosol Prescribing Information"]
    },
    {
        "id": "folic_acid", "name": "Folic Acid", "zh": "叶酸",
        "brands": ["Folvite", "叶酸片"],
        "cat": "OTHER", "sub": "VITAMIN_B9", "atc": "B03BB01",
        "pk": {"f": 0.77, "ka": 0.5, "tmax": 1.0, "t12": 3.0, "t12r": [2, 4], "vdkg": 0.4, "pb": 70},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "代谢为 5-MTHF(活性形式);妊娠/MTX 治疗常用"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "NONE", "elderly": "0.4-1mg qd(贫血);0.4-0.8mg qd(妊娠)", "smoking": None},
        "ints": [
            {"id": "methotrexate", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "降低 MTX 抗炎效;MTX 治疗期间需补叶酸减轻副作用"},
            {"id": "phenytoin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "叶酸降低苯妥英浓度,癫痫患者需监测"},
            {"id": "sulfasalazine", "m": "PHARMACODYNAMIC", "af": [0.5, 0.7], "s": "MEDIUM", "n": "SASP 致叶酸吸收降低,可能缺乏"},
            {"id": "bile_acid_sequestrants", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.7], "s": "MEDIUM", "n": "考来烯胺结合叶酸,降低吸收"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["血红蛋白/红细胞叶酸", "B12 水平(掩盖缺乏)"]},
        "refs": ["AGNP 2017", "FDA Folvite Prescribing Information"]
    },
    # === v0.4 补漏: 用户要求的 4 个临床常用药 ===
    {
        "id": "tianeptine", "name": "Tianeptine", "zh": "噻奈普汀",
        "brands": ["Stablon", "Tianeptine", "达力新"],
        "cat": "ANTIDEPRESSANT", "sub": "SSRE_5HT_REUPTAKE_ENHANCER", "atc": "N06AX14",
        "pk": {"f": 0.99, "ka": 1.5, "tmax": 1.0, "t12": 2.5, "t12r": [2, 3], "vdkg": 0.8, "pb": 95,
               "commonDoseRangeMg": [12.5, 50.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "不通过 CYP 显著代谢;β-氧化为主要途径;不抑制 CYP"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "12.5mg tid,缓滴定;勿晚于 16:00", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "高血压危象风险;MAOI 停 2 周"},
            {"id": "triptans", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险(虽然噻奈普汀作用机制不同)"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "μ-阿片受体激动,联用增加呼吸抑制风险"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加镇静"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["情绪/抑郁量表", "肝功能", "肾功能", "滥用风险评估"]},
        "refs": ["AGNP 2017", "PMID: 16523303 (Tianeptine review)", "Stablon Prescribing Information"]
    },
    {
        "id": "allopurinol", "name": "Allopurinol", "zh": "别嘌醇",
        "brands": ["Zyloprim", "Lopurin", "赛来力"],
        "cat": "OTHER", "sub": "XANTHINE_OXIDASE_INHIBITOR", "atc": "M04AA01",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 1.5, "t12": 1.5, "t12r": [1, 2], "vdkg": 0.6, "pb": 0,
               "commonDoseRangeMg": [100.0, 800.0]},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "活性代谢物 oxypurinol (半衰期 18-30h) 是实际起效分子;不通过 CYP 代谢"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 50-100mg qd,缓加", "smoking": None},
        "ints": [
            {"id": "azathioprine", "m": "PHARMACOKINETIC_OTHER", "af": [2.0, 4.0], "s": "CONTRAINDICATED", "n": "黄嘌呤氧化酶抑制使 6-MP/AZA 浓度升高 3-4 倍,严重骨髓抑制"},
            {"id": "mercaptopurine", "m": "PHARMACOKINETIC_OTHER", "af": [2.0, 4.0], "s": "CONTRAINDICATED", "n": "同 azathioprine"},
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.3], "s": "MEDIUM", "n": "INR 可能略升,需监测"},
            {"id": "amoxicillin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "皮疹风险增加(机制不明)"},
            {"id": "ampicillin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "皮疹风险增加"},
            {"id": "ACE_inhibitors", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "联用可能增加过敏反应风险"},
            {"id": "thiazide_diuretics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加严重皮肤反应(SJS/TEN)风险"}
        ],
        "mon": {"freq": "MONTHLY_FIRST_3_MONTHS_THEN_EVERY_3_MONTHS", "items": ["尿酸", "肝功能", "肾功能", "皮疹/过敏反应(前 3 月)"]},
        "refs": ["AGNP 2017", "FDA Zyloprim Prescribing Information", "ACR Gout Guidelines 2020"]
    },
    {
        "id": "colchicine", "name": "Colchicine", "zh": "秋水仙碱",
        "brands": ["Colcrys", "Mitigare", "秋水仙碱片"],
        "cat": "OTHER", "sub": "ANTI_INFLAMMATORY_GOUT", "atc": "M04AC01",
        "pk": {"f": 0.45, "ka": 1.0, "tmax": 1.0, "t12": 27.0, "t12r": [20, 35], "vdkg": 5.0, "pb": 39,
               "commonDoseRangeMg": [0.6, 2.4]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}], "inh": [{"cyp": "P_GP", "strength": "STRONG"}], "ind": [],
               "note": "P-gp 强抑制剂;也是 CYP3A4 + P-gp 双底物"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 0.3mg bid,勿超 1.2mg/d", "smoking": None},
        "ints": [
            {"id": "clarithromycin", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "CYP3A4 强抑制 + P-gp 抑制,秋水仙碱浓度升 3 倍,严重中毒"},
            {"id": "erythromycin", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "同 clarithromycin"},
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "CYP3A4 强抑制"},
            {"id": "ritonavir", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "CYP3A4 强抑制"},
            {"id": "verapamil", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "P-gp 抑制,需减量"},
            {"id": "diltiazem", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "HIGH", "n": "P-gp 抑制,需减量"},
            {"id": "cyclosporine", "m": "PHARMACOKINETIC_OTHER", "af": [2.0, 3.0], "s": "HIGH", "n": "P-gp 抑制,联用增加肌毒性"},
            {"id": "statins", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加肌病/横纹肌溶解风险(尤其 simvastatin)"},
            {"id": "grapefruit_juice", "m": "CYP_INHIBITION_MODERATE", "af": [1.3, 1.5], "s": "MEDIUM", "n": "CYP3A4 抑制,避免"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血常规", "肌酶(CK)", "肾功能", "胃肠反应", "周围神经病变"]},
        "refs": ["AGNP 2017", "FDA Colcrys Prescribing Information", "ACR Gout Guidelines 2020"]
    },
    {
        "id": "tamsulosin", "name": "Tamsulosin", "zh": "坦索罗辛",
        "brands": ["Flomax", "Harnal", "必坦"],
        "cat": "OTHER", "sub": "ALPHA_1A_BLOCKER_BPH", "atc": "G04CA02",
        "pk": {"f": 1.0, "ka": 1.5, "tmax": 4.0, "t12": 11.0, "t12r": [9, 15], "vdkg": 0.2, "pb": 99,
               "commonDoseRangeMg": [0.2, 0.8]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2D6", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "广泛首过代谢;食物降低吸收但减轻体位性低血压"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 0.4mg qd(餐后),缓加", "smoking": None},
        "ints": [
            {"id": "PDE5_inhibitors", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用显著低血压(西地那非/他达拉非);需剂量调整"},
            {"id": "alpha_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用严重低血压"},
            {"id": "antihypertensives", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加低血压/晕厥风险"},
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "酮康唑等强 CYP3A4 抑制剂升高浓度"},
            {"id": "warfarin", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "INR 略升,监测"},
            {"id": "fluoroquinolones", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "LOW", "n": "坦索罗辛轻微 QT 延长,联用沙星类注意"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["血压(体位性)", "IPSS 评分", "尿流率", "PSA"]},
        "refs": ["AGNP 2017", "FDA Flomax Prescribing Information", "AUA BPH Guidelines 2021"]
    },
]
