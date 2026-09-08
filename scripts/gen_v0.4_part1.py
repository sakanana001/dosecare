"""
v0.4 第一批: 精神科扩充 (20 药)
"""

BATCH1 = [
    # ---- 抗精神病 (6) ----
    {
        "id": "paliperidone", "name": "Paliperidone", "zh": "帕利哌酮",
        "brands": ["Invega", "善妥达", "Invega Sustenna"],
        "cat": "ANTIPSYCHOTIC", "sub": "ATYPICAL_BENZISOXAZOLE", "atc": "N05AX13",
        "pk": {"f": 0.28, "ka": 1.0, "tmax": 24.0, "t12": 23.0, "t12r": [20, 26], "vdkg": 4.4, "pb": 74,
               "commonDoseRangeMg": [3.0, 12.0]},
        "cyp": {"subs": [{"cyp": "P_GP", "fraction": 1.0}], "inh": [], "ind": [],
               "note": "66% 肾原型排泄,P-gp 转运,无明显 CYP 代谢"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "HIGH"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 3mg qd,缓滴定;体位性低血压风险", "smoking": None},
        "ints": [
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "P-gp 诱导,帕利哌酮浓度显著降低"},
            {"id": "divalproex", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 EPS 风险"},
            {"id": "risperidone", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "同活性代谢物 9-OH-利培酮,叠加毒性,禁止联用"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "血糖", "血脂", "体重/BMI", "EPS 评估", "高泌乳素症状(泌乳/闭经/性功能)"]},
        "refs": ["AGNP 2017", "FDA Invega Prescribing Information", "PMID: 16322114 (paliperidone ER formulation)"]
    },
    {
        "id": "asenapine", "name": "Asenapine", "zh": "阿塞那平",
        "brands": ["Saphris", "Sycrest", "金准康"],
        "cat": "ANTIPSYCHOTIC", "sub": "ATYPICAL_DIBENZOXAZEPINE", "atc": "N05AH05",
        "pk": {"f": 0.35, "ka": 0.5, "tmax": 1.0, "t12": 24.0, "t12r": [13, 39], "vdkg": 20.0, "pb": 95,
               "commonDoseRangeMg": [5.0, 20.0]},
        "cyp": {"subs": [{"cyp": "CYP1A2", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.3}, {"cyp": "UGT1A4", "fraction": 0.2}],
               "inh": [], "ind": [], "note": "舌下片首过效应低(<2% 吞咽),口腔含服必须"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "MEDIUM"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 5mg bid(舌下),缓滴定", "smoking": "CYP1A2 轻度诱导,可能需略增剂量"},
        "ints": [
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "CYP1A2 强抑制,阿塞那平浓度显著升高"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP1A2/3A4 诱导,失效"},
            {"id": "ciprofloxacin", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP1A2 抑制"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "血糖", "血脂", "体重/BMI", "EPS 评估", "口部感觉减退(局部反应)"]},
        "refs": ["AGNP 2017", "FDA Saphris Prescribing Information"]
    },
    {
        "id": "lurasidone", "name": "Lurasidone", "zh": "鲁拉西酮",
        "brands": ["Latuda", "罗舒达"],
        "cat": "ANTIPSYCHOTIC", "sub": "ATYPICAL_BENZISOXAZOLE", "atc": "N05AE05",
        "pk": {"f": 0.12, "ka": 1.0, "tmax": 3.0, "t12": 18.0, "t12r": [12, 37], "vdkg": 11.0, "pb": 99,
               "commonDoseRangeMg": [20.0, 160.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "必须随餐(>350 卡)服用,生物利用度提高 2 倍"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 20mg qd", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [3.0, 6.0], "s": "CONTRAINDICATED", "n": "CYP3A4 强抑制,鲁拉西酮浓度剧增"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.1, 0.3], "s": "CONTRAINDICATED", "n": "CYP3A4 强诱导,失效"},
            {"id": "diltiazem", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP3A4 中度抑制"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,失效"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "血糖", "血脂", "体重/BMI", "EPS/akathisia 评估", "肾功能(肾排泄为主)"]},
        "refs": ["AGNP 2017", "FDA Latuda Prescribing Information"]
    },
    {
        "id": "brexpiprazole", "name": "Brexpiprazole", "zh": "依匹哌唑",
        "brands": ["Rexulti", "Rxulti"],
        "cat": "ANTIPSYCHOTIC", "sub": "ATYPICAL_DIBENZOTHIAZEPINE", "atc": "N05AX16",
        "pk": {"f": 0.5, "ka": 0.5, "tmax": 4.0, "t12": 91.0, "t12r": [70, 100], "vdkg": 13.0, "pb": 99,
               "commonDoseRangeMg": [0.5, 4.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "t½ 极长(91h),稳态需 2 周,半衰期长停药后效应持续"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_C", "elderly": "起始 0.5-1mg qd,缓滴定", "smoking": None},
        "ints": [
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2D6 强抑制,半量使用"},
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "CYP3A4 强抑制,半量"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,剂量加倍"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "血糖", "血脂", "体重/BMI", "EPS 评估"]},
        "refs": ["AGNP 2017", "FDA Rexulti Prescribing Information"]
    },
    {
        "id": "cariprazine", "name": "Cariprazine", "zh": "卡利拉嗪",
        "brands": ["Vraylar", "瑞欣妥"],
        "cat": "ANTIPSYCHOTIC", "sub": "ATYPICAL_DERIVATIVE", "atc": "N05AX15",
        "pk": {"f": 0.65, "ka": 0.7, "tmax": 3.0, "t12": 168.0, "t12r": [120, 480], "vdkg": 18.0, "pb": 91,
               "commonDoseRangeMg": [1.5, 6.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "活性代谢物 desmethyl-cariprazine 和 DDCAR 半衰期 1-3 周"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "MEDIUM", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 1.5mg qd,缓滴定", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 强抑制,半量"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导,失效"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP3A4 强诱导"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "血糖", "血脂", "体重/BMI", "EPS/akathisia 评估"]},
        "refs": ["AGNP 2017", "FDA Vraylar Prescribing Information"]
    },
    {
        "id": "iloperidone", "name": "Iloperidone", "zh": "伊潘立酮",
        "brands": ["Fanapt", "泰妥生"],
        "cat": "ANTIPSYCHOTIC", "sub": "ATYPICAL_PIPERIDINE", "atc": "N05AX14",
        "pk": {"f": 0.96, "ka": 0.5, "tmax": 4.0, "t12": 18.0, "t12r": [12, 33], "vdkg": 8.0, "pb": 95,
               "commonDoseRangeMg": [2.0, 24.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "活性代谢物 P88,半衰期更长"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "MEDIUM"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 1mg bid,缓滴定;QTc 风险", "smoking": None},
        "ints": [
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2D6 强抑制,半量"},
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 强抑制"},
            {"id": "sotalol", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积,尖端扭转风险"},
            {"id": "amiodarone", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(QTc)", "WBC", "血糖", "血脂", "体重/BMI", "体位性低血压"]},
        "refs": ["AGNP 2017", "FDA Fanapt Prescribing Information"]
    },
    # ---- 抗抑郁 - NaSSA / NDRI / Multimodal / SARI / Melatonin (5) ----
    {
        "id": "mirtazapine", "name": "Mirtazapine", "zh": "米氮平",
        "brands": ["Remeron", "瑞美隆"],
        "cat": "ANTIDEPRESSANT", "sub": "NaSSA", "atc": "N06AX11",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 2.0, "t12": 30.0, "t12r": [20, 40], "vdkg": 14.0, "pb": 85,
               "commonDoseRangeMg": [15.0, 45.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.4}, {"cyp": "CYP1A2", "fraction": 0.3}, {"cyp": "CYP3A4", "fraction": 0.3}],
               "inh": [], "ind": [], "note": "去甲米氮平为活性代谢物"},
        "win": {"low": 30.0, "high": 80.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "HIGH", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 7.5mg qn,缓滴定", "smoking": None},
        "ints": [
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP2D6/1A2 抑制,浓度升高"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP 诱导,浓度降低"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险,MAOI 停 14 天"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 CNS 抑制"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["体重/BMI", "血脂", "空腹血糖", "WBC", "肝功", "米氮平血药浓度(剂量≥45mg)"]},
        "refs": ["AGNP 2017", "FDA Remeron Prescribing Information"]
    },
    {
        "id": "bupropion", "name": "Bupropion", "zh": "安非他酮",
        "brands": ["Wellbutrin", "Elontril", "悦亭"],
        "cat": "ANTIDEPRESSANT", "sub": "NDRI", "atc": "N06AX12",
        "pk": {"f": 0.87, "ka": 1.0, "tmax": 2.0, "t12": 21.0, "t12r": [12, 30], "vdkg": 20.0, "pb": 84,
               "commonDoseRangeMg": [75.0, 450.0]},
        "cyp": {"subs": [{"cyp": "CYP2B6", "fraction": 0.9}], "inh": [{"cyp": "CYP2D6", "strength": "STRONG"}], "ind": [],
               "note": "活性代谢物 hydroxybupropion (CYP2B6 产生)"},
        "win": {"low": 50.0, "high": 100.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 75mg bid(速释)或 150mg qd(XL),缓滴定", "smoking": None},
        "ints": [
            {"id": "tamoxifen", "m": "CYP_INHIBITION_STRONG", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP2D6 强抑制,降低 tamoxifen 活性代谢物(endoxifen),影响乳腺癌治疗"},
            {"id": "codeine", "m": "CYP_INHIBITION_STRONG", "af": [0.1, 0.2], "s": "HIGH", "n": "CYP2D6 强抑制,阻断 codeine → morphine 活化,镇痛失效"},
            {"id": "tramadol", "m": "CYP_INHIBITION_STRONG", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP2D6 强抑制,降低 M1 活性代谢物"},
            {"id": "metoprolol", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制,慢代谢者效应剧增,严重心动过缓"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "MEDIUM", "n": "CYP 诱导,降低 bupropion 浓度"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "肝功", "血压(高血压史)", "体重", "自杀倾向(抗抑郁共性)"]},
        "refs": ["AGNP 2017", "FDA Wellbutrin Black Box Warning (suicidality in young adults)"]
    },
    {
        "id": "vortioxetine", "name": "Vortioxetine", "zh": "伏硫西汀",
        "brands": ["Trintellix", "心达悦"],
        "cat": "ANTIDEPRESSANT", "sub": "MULTIMODAL_5HT", "atc": "N06AX26",
        "pk": {"f": 0.75, "ka": 0.5, "tmax": 7.0, "t12": 66.0, "t12r": [55, 75], "vdkg": 25.0, "pb": 98,
               "commonDoseRangeMg": [5.0, 20.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.6}, {"cyp": "CYP3A4", "fraction": 0.3}, {"cyp": "CYP2C9", "fraction": 0.1}],
               "inh": [], "ind": [], "note": "机制:5-HT 再摄取抑制 + 5-HT 受体多模式调节"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 5mg qd,缓滴定", "smoking": "CYP1A2 轻度诱导,可能略降浓度"},
        "ints": [
            {"id": "bupropion", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.5], "s": "HIGH", "n": "CYP2D6 强抑制,伏硫西汀剂量减半"},
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.5], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.5], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP3A4 强抑制"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP 诱导"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险,MAOI 停 14 天"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["WBC", "肝功", "5-HT 综合征症状", "自杀倾向"]},
        "refs": ["AGNP 2017", "FDA Trintellix Prescribing Information"]
    },
    {
        "id": "agomelatine", "name": "Agomelatine", "zh": "阿戈美拉汀",
        "brands": ["Valdoxan", "维度新"],
        "cat": "ANTIDEPRESSANT", "sub": "MT_AGONIST_5HT2C", "atc": "N06AX22",
        "pk": {"f": 0.05, "ka": 1.0, "tmax": 1.0, "t12": 1.5, "t12r": [1, 2], "vdkg": 8.0, "pb": 95,
               "commonDoseRangeMg": [25.0, 50.0]},
        "cyp": {"subs": [{"cyp": "CYP1A2", "fraction": 0.9}, {"cyp": "CYP2C19", "fraction": 0.1}], "inh": [], "ind": [],
               "note": "MT1/MT2 激动 + 5-HT2C 拮抗,无性功能障碍,改善睡眠"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 25mg qn,2 周后可加至 50mg", "smoking": "CYP1A2 强诱导,血药浓度可能降低 50%,剂量需加"},
        "ints": [
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [10.0, 50.0], "s": "CONTRAINDICATED", "n": "CYP1A2 强抑制,血药浓度升高 10-50 倍,肝毒性风险剧增,禁止联用"},
            {"id": "ciprofloxacin", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.5], "s": "HIGH", "n": "CYP1A2 抑制,肝毒性风险"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.2, 0.5], "s": "HIGH", "n": "CYP1A2 强诱导,浓度降低 50-80%,建议换药"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP 诱导,失效"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功(ALT/AST/胆红素)", "基线 + 3 周 + 6 周 + 12 周 + 之后每 6 月", "WBC", "自杀倾向"]},
        "refs": ["AGNP 2017", "EMA Valdoxan Prescribing Information (含肝毒性黑框警告)"]
    },
    {
        "id": "trazodone", "name": "Trazodone", "zh": "曲唑酮",
        "brands": ["Desyrel", "Trittico", "美时玉"],
        "cat": "ANTIDEPRESSANT", "sub": "SARI", "atc": "N06AX05",
        "pk": {"f": 0.65, "ka": 1.0, "tmax": 1.5, "t12": 8.0, "t12r": [6, 10], "vdkg": 1.0, "pb": 89,
               "commonDoseRangeMg": [50.0, 400.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}, {"cyp": "CYP2D6", "fraction": 0.2}], "inh": [], "ind": [],
               "note": "活性代谢物 mCPP(可能致焦虑),氯苯基哌嗪"},
        "win": {"low": 800.0, "high": 1600.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "VERY_HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 25mg qn,失眠可用 25-50mg 即可", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "HIGH", "n": "CYP3A4 强抑制,浓度升高"},
            {"id": "carbamazepine", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "CYP 诱导,失效"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险"},
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 5-HT 综合征风险,临床应避免"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加 CNS 抑制"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功", "BP(直立性低血压)", "自杀倾向", "5-HT 综合征症状"]},
        "refs": ["AGNP 2017", "FDA Desyrel Prescribing Information"]
    },
    # ---- MAOI + TCA (6) ----
    {
        "id": "phenelzine", "name": "Phenelzine", "zh": "苯乙肼",
        "brands": ["Nardil"],
        "cat": "ANTIDEPRESSANT", "sub": "MAOI_IRREVERSIBLE", "atc": "N06AF03",
        "pk": {"f": 0.9, "ka": 1.0, "tmax": 1.0, "t12": 2.0, "t12r": [1.5, 4.0], "vdkg": 1.5, "pb": 0,
               "commonDoseRangeMg": [15.0, 90.0]},
        "cyp": {"subs": [], "inh": ["MAOI"], "ind": [],
               "note": "不可逆抑制 MAO-A/B,效力持续 2 周;低 tyramine 饮食要求"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "CONTRAINDICATED_IN_SEVERE", "elderly": "起始 7.5mg tid,缓滴定;警惕低血压", "smoking": None},
        "ints": [
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征致命风险,SSRI 停 5 周(氟西汀)或 2 周"},
            {"id": "snris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征"},
            {"id": "tcas", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "高血压危象 + 5-HT 综合征"},
            {"id": "tyramine_rich_food", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "高血压危象(酪胺反应):陈年奶酪/红酒/腌肉/酱油/蚕豆"},
            {"id": "sympathomimetics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "高血压危象:伪麻黄碱/麻黄碱/去甲肾上腺素"},
            {"id": "meperidine", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征 + 呼吸抑制,致命"},
            {"id": "dextromethorphan", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["BP(直立位)", "肝功", "WBC", "自杀倾向", "饮食依从性(酪胺)"]},
        "refs": ["AGNP 2017", "FDA Nardil Prescribing Information (含 MAOI 警示)"]
    },
    {
        "id": "selegiline", "name": "Selegiline", "zh": "司来吉兰",
        "brands": ["Eldepryl", "Zelapar"],
        "cat": "ANTIDEPRESSANT", "sub": "MAOI_B_SELECTIVE", "atc": "N04BD01",
        "pk": {"f": 0.1, "ka": 1.0, "tmax": 0.5, "t12": 1.5, "t12r": [1, 2], "vdkg": 11.0, "pb": 85,
               "commonDoseRangeMg": [5.0, 10.0]},
        "cyp": {"subs": [{"cyp": "CYP2B6", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.3}, {"cyp": "CYP1A2", "fraction": 0.2}], "inh": ["MAO-B(选择性)"], "ind": [],
               "note": "舌下片 ODT 吸收快;低剂量(<10mg/d)选 MAO-B,高剂量失选择性"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 5mg bid,缓滴定", "smoking": None},
        "ints": [
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征,氟西汀停 5 周"},
            {"id": "snris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征"},
            {"id": "meperidine", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征"},
            {"id": "tyramine_rich_food", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "舌下片可避免首过,但剂量>10mg/d 仍需低 tyramine 饮食"},
            {"id": "levodopa", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用可增强左旋多巴效应,需减左旋多巴剂量"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["BP", "肝功", "5-HT 综合征症状", "自杀倾向"]},
        "refs": ["AGNP 2017", "FDA Eldepryl Prescribing Information"]
    },
    {
        "id": "amitriptyline", "name": "Amitriptyline", "zh": "阿米替林",
        "brands": ["Elavil", "Endep", "依拉维"],
        "cat": "ANTIDEPRESSANT", "sub": "TCA", "atc": "N06AA09",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 4.0, "t12": 20.0, "t12r": [10, 28], "vdkg": 14.0, "pb": 95,
               "commonDoseRangeMg": [25.0, 150.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.7}, {"cyp": "CYP2C19", "fraction": 0.2}, {"cyp": "CYP3A4", "fraction": 0.1}], "inh": [], "ind": [],
               "note": "活性代谢物 nortriptyline(强效 NE 再摄取抑制)"},
        "win": {"low": 80.0, "high": 200.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 3, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 10-25mg qn,Beers 列表慎用(老年)", "smoking": None},
        "ints": [
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 4.0], "s": "HIGH", "n": "CYP2D6 强抑制,浓度剧增,毒性"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 4.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征,MAOI 停 14 天"},
            {"id": "sotalol", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积,尖端扭转风险"},
            {"id": "amiodarone", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用增加 CNS 抑制"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(QTc)", "WBC", "肝功", "BP(直立性低血压)", "自杀倾向", "阿米替林血药浓度"]},
        "refs": ["AGNP 2017", "FDA Elavil Prescribing Information"]
    },
    {
        "id": "nortriptyline", "name": "Nortriptyline", "zh": "去甲替林",
        "brands": ["Pamelor", "Aventyl", "去甲替林"],
        "cat": "ANTIDEPRESSANT", "sub": "TCA", "atc": "N06AA10",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 7.0, "t12": 30.0, "t12r": [20, 40], "vdkg": 18.0, "pb": 92,
               "commonDoseRangeMg": [25.0, 150.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "阿米替林活性代谢物;独立用药"},
        "win": {"low": 50.0, "high": 150.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 2, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 10-25mg qn,缓滴定,Beers 慎用", "smoking": None},
        "ints": [
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制,慢代谢者中毒风险"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征"},
            {"id": "sotalol", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(QTc)", "WBC", "肝功", "BP", "去甲替林血药浓度"]},
        "refs": ["AGNP 2017", "FDA Pamelor Prescribing Information"]
    },
    {
        "id": "clomipramine", "name": "Clomipramine", "zh": "氯米帕明",
        "brands": ["Anafranil", "安拿芬尼"],
        "cat": "ANTIDEPRESSANT", "sub": "TCA", "atc": "N06AA04",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 2.0, "t12": 32.0, "t12r": [19, 37], "vdkg": 17.0, "pb": 97,
               "commonDoseRangeMg": [25.0, 250.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}, {"cyp": "CYP1A2", "fraction": 0.3}, {"cyp": "CYP3A4", "fraction": 0.2}], "inh": [], "ind": [],
               "note": "强 5-HT 再摄取抑制;用于 OCD"},
        "win": {"low": 230.0, "high": 450.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 3, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 25mg qn,缓滴定,Beers 慎用", "smoking": None},
        "ints": [
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制,中毒风险"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征"},
            {"id": "ssris", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险"},
            {"id": "sotalol", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(QTc)", "WBC", "肝功", "BP", "氯米帕明血药浓度"]},
        "refs": ["AGNP 2017", "FDA Anafranil Prescribing Information"]
    },
    {
        "id": "imipramine", "name": "Imipramine", "zh": "丙咪嗪",
        "brands": ["Tofranil", "依咪帕明"],
        "cat": "ANTIDEPRESSANT", "sub": "TCA", "atc": "N06AA02",
        "pk": {"f": 0.5, "ka": 1.0, "tmax": 3.0, "t12": 18.0, "t12r": [11, 25], "vdkg": 20.0, "pb": 90,
               "commonDoseRangeMg": [25.0, 200.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.7}, {"cyp": "CYP1A2", "fraction": 0.2}, {"cyp": "CYP2C19", "fraction": 0.1}], "inh": [], "ind": [],
               "note": "活性代谢物 desipramine"},
        "win": {"low": 150.0, "high": 300.0, "unit": "ng/mL", "guidelineSource": "AGNP 2017"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 3, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "MONITOR", "elderly": "起始 10-25mg qn,Beers 慎用", "smoking": None},
        "ints": [
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "MAOI", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "5-HT 综合征"},
            {"id": "sotalol", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "QTc 累积"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心电图(QTc)", "WBC", "肝功", "BP", "丙咪嗪血药浓度"]},
        "refs": ["AGNP 2017", "FDA Tofranil Prescribing Information"]
    },
    # ---- 抗焦虑 + ADHD (3) ----
    {
        "id": "buspirone", "name": "Buspirone", "zh": "丁螺环酮",
        "brands": ["BuSpar", "布斯帕"],
        "cat": "ANXIOLYTIC", "sub": "5HT1A_PARTIAL_AGONIST", "atc": "N05BE01",
        "pk": {"f": 0.04, "ka": 1.0, "tmax": 1.0, "t12": 2.5, "t12r": [2, 3], "vdkg": 5.0, "pb": 95,
               "commonDoseRangeMg": [15.0, 60.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "活性代谢物 1-PP(6-OH-buspirone);非苯二氮䓬,不依赖,无镇静"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 5mg bid,2 周后评估", "smoking": None},
        "ints": [
            {"id": "ketoconazole", "m": "CYP_INHIBITION_STRONG", "af": [5.0, 10.0], "s": "HIGH", "n": "CYP3A4 强抑制,血药浓度剧增,毒性"},
            {"id": "rifampin", "m": "CYP_INDUCTION", "af": [0.1, 0.3], "s": "HIGH", "n": "CYP3A4 强诱导,失效"},
            {"id": "grapefruit", "m": "CYP_INHIBITION_MODERATE", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP3A4 抑制"},
            {"id": "MAOI", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用可能高血压危象,谨慎"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肝功", "WBC", "自杀倾向", "焦虑量表"]},
        "refs": ["AGNP 2017", "FDA BuSpar Prescribing Information"]
    },
    {
        "id": "methylphenidate", "name": "Methylphenidate", "zh": "哌甲酯",
        "brands": ["Ritalin", "Concerta", "利他林"],
        "cat": "STIMULANT", "sub": "AMPHETAMINE_RELATED", "atc": "N06BA04",
        "pk": {"f": 0.3, "ka": 1.5, "tmax": 2.0, "t12": 2.5, "t12r": [2, 4], "vdkg": 13.0, "pb": 15,
               "commonDoseRangeMg": [10.0, 60.0]},
        "cyp": {"subs": [{"cyp": "CES1", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "血浆酯酶水解,无明显 CYP 代谢"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "起始 5mg bid,缓滴定", "smoking": None},
        "ints": [
            {"id": "MAOI", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "高血压危象,MAOI 停 14 天"},
            {"id": "ssris", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "LOW", "n": "5-HT 综合征罕见"},
            {"id": "antipsychotics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "拮抗,降低抗精神病药效"},
            {"id": "clonidine", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加心血管事件,罕见猝死报道"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["BP", "心率", "身高/体重(儿童)", "食欲", "睡眠", "自杀倾向"]},
        "refs": ["AGNP 2017", "FDA Ritalin/Concerta Prescribing Information (含滥用风险黑框)"]
    },
    {
        "id": "atomoxetine", "name": "Atomoxetine", "zh": "托莫西汀",
        "brands": ["Strattera", "择思达"],
        "cat": "STIMULANT", "sub": "SNRI_NRI", "atc": "N06BA09",
        "pk": {"f": 0.94, "ka": 0.5, "tmax": 1.5, "t12": 5.0, "t12r": [4, 6], "vdkg": 0.85, "pb": 98,
               "commonDoseRangeMg": [40.0, 100.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "CYP2D6 慢代谢者 t½ 延长至 21h,浓度高 5-10 倍"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 40mg qd,缓滴定", "smoking": None},
        "ints": [
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [3.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制,慢代谢者表型,中毒风险,需减量"},
            {"id": "paroxetine", "m": "CYP_INHIBITION_STRONG", "af": [3.0, 5.0], "s": "HIGH", "n": "CYP2D6 强抑制"},
            {"id": "MAOI", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "高血压危象,MAOI 停 14 天"},
            {"id": "salbutamol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用心血管反应增加"},
            {"id": "antipsychotics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增加 EPS 风险"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["BP", "心率", "肝功", "身高/体重(儿童)", "自杀倾向(黑框警告)"]},
        "refs": ["AGNP 2017", "FDA Strattera Black Box Warning (suicidality in children/adolescents)"]
    },
]
