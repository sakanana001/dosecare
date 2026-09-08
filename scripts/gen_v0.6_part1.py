"""
v0.6 第一批: 缓释版 + 异构体 + 活性代谢物版 (12 药)

参考 FDA DailyMed + AGNP 2017 + 公开临床试验数据校准

数据校准来源 (主要参考):
- FDA 药品标签 (Cmax, t½, AUC)
- 公开临床试验 (PMID + ClinicalTrials.gov)
- AGNP 2017 TDM 共识 (治疗窗)
- Hiemke C. 2018 Pharmacopsychiatry
- 药品说明书国内版

设计:
- 缓释 (4): bupropion XL, venlafaxine XR, paroxetine CR, fluoxetine weekly
- 异构体 (4): arformoterol, levalbuterol, dextroamphetamine, levetiracetam
- 活性代谢物 (4): desvenlafaxine, norfluoxetine, norketamine, morphine-6-glucuronide
"""
BATCH1 = [
    # ============================================================
    # 缓释/控释版 (4)
    # ============================================================
    {
        "id": "bupropion_xl", "name": "Bupropion XL", "zh": "安非他酮缓释片",
        "brands": ["Wellbutrin XL", "悦婷"],
        "cat": "ANTIDEPRESSANT", "sub": "NDRI_ER", "atc": "N06AX12",
        "pk": {"f": 0.85, "ka": 1.2, "tmax": 5.0, "t12": 21.0, "t12r": [16, 30], "vdkg": 12.0, "pb": 84,
               "commonDoseRangeMg": [150.0, 450.0]},
        "cyp": {"subs": [{"cyp": "CYP2B6", "fraction": 0.95}], "inh": [{"cyp": "CYP2D6", "strength": "STRONG"}], "ind": [],
               "note": "NDRI 抗抑郁 + 戒烟; 缓释 XL 一天 1 次; 强 CYP2D6 抑制剂 (影响很多药); 降低癫痫阈值 (禁大剂量/贪食症)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 150mg qd, 缓滴定", "smoking": None},
        "ints": [
            {"id": "CYP2D6_substrates", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "强 CYP2D6 抑制剂: 帕罗西汀/氟西汀/曲马多/他莫昔芬/卡维地洛/美托洛尔 浓度升高 2-5 倍"},
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 高血压危象"},
            {"id": "seizure_threshold_lowering", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 抗精神病/抗抑郁/茶碱/曲马多 叠加降低癫痫阈值"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低癫痫阈值"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["抑郁评分", "血压", "肝功能", "癫痫阈值 (大剂量)"]},
        "refs": ["FDA Wellbutrin XL Prescribing Information", "AGNP 2017", "PMID: 11817525 (bupropion PK)"],
        "clinicalTrialRefs": ["FDA 标签: Cmax ~140 ng/mL at 150mg steady state", "PMID 11817525: t1/2 21±7h", "CL/F ~160 L/h"]
    },
    {
        "id": "venlafaxine_xr", "name": "Venlafaxine XR", "zh": "文拉法辛缓释胶囊",
        "brands": ["Effexor XR", "怡诺思"],
        "cat": "ANTIDEPRESSANT", "sub": "SNRI_ER", "atc": "N06AX16",
        "pk": {"f": 0.45, "ka": 0.7, "tmax": 6.0, "t12": 5.0, "t12r": [4, 8], "vdkg": 7.5, "pb": 27,
               "commonDoseRangeMg": [37.5, 225.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.7}, {"cyp": "CYP3A4", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "SNRI 抗抑郁; 缓释 XR 一天 1 次; 活性代谢物 O-去甲文拉法辛 (desvenlafaxine, 单独作为药上市)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 37.5mg qd", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征致命风险; 停 MAOI 14d"},
            {"id": "CYP2D6_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "帕罗西汀/氟西汀/安非他酮: 文拉法辛 + O-去甲代谢产物 浓度升高 2-3 倍"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "联用 5-HT 综合征"},
            {"id": "triptans", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用 5-HT 综合征 (虽真实发生率低)"}
        ],
        "mon": {"freq": "MONTHLY_FIRST_3_MONTHS_THEN_EVERY_3_MONTHS", "items": ["抑郁/焦虑评分", "血压 (剂量依赖升高)", "心率", "5-HT 综合征症状", "肝肾功能"]},
        "refs": ["FDA Effexor XR Prescribing Information", "AGNP 2017", "PMID: 8533959"],
        "clinicalTrialRefs": ["FDA 标签: 75mg XR Cmax ~150 ng/mL, t1/2 5h", "PMID 8533959: AUC 线性", "Steady state 3d"]
    },
    {
        "id": "paroxetine_cr", "name": "Paroxetine CR", "zh": "帕罗西汀控释片",
        "brands": ["Paxil CR", "赛乐特"],
        "cat": "ANTIDEPRESSANT", "sub": "SSRI_CR", "atc": "N06AB05",
        "pk": {"f": 0.5, "ka": 0.6, "tmax": 8.0, "t12": 21.0, "t12r": [16, 30], "vdkg": 17.0, "pb": 95,
               "commonDoseRangeMg": [12.5, 75.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.95}], "inh": [{"cyp": "CYP2D6", "strength": "STRONG"}], "ind": [],
               "note": "SSRI 抗抑郁; CR 控释一天 1 次 (普通片剂要 1 天 1 次但缓释); CYP2D6 自抑 (自己代谢自己, 稳态后清除减半); 高蛋白结合"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 12.5mg qd, 勿超 40mg/d (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征致命; 停 MAOI 14d"},
            {"id": "CYP2D6_substrates", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "强 CYP2D6 抑制: 可待因→吗啡失效; 曲马多失效; 他莫昔芬→endoxifen 失效"},
            {"id": "tamoxifen", "m": "CYP_INHIBITION_STRONG", "af": [0.3, 0.5], "s": "HIGH", "n": "乳腺癌患者禁忌: 减少 endoxifen (活性代谢物), 降低药效 50%+"},
            {"id": "MAOIs_TRYPTOPHAN", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 5-HT 综合征"},
            {"id": "anticholinergics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 抗胆碱能叠加"}
        ],
        "mon": {"freq": "MONTHLY_FIRST_3_MONTHS_THEN_EVERY_3_MONTHS", "items": ["抑郁评分", "5-HT 综合征症状", "骨密度 (长期 SSRI)", "钠 (低钠风险)", "出血风险"]},
        "refs": ["FDA Paxil CR Prescribing Information", "AGNP 2017", "CPIC CYP2D6"],
        "clinicalTrialRefs": ["FDA 标签: 25mg CR Cmax ~30 ng/mL, t1/2 21h", "PMID 7582517: CYP2D6 self-inhibition", "Steady state ~2 周"]
    },
    {
        "id": "fluoxetine_weekly", "name": "Fluoxetine Weekly", "zh": "氟西汀每周一次",
        "brands": ["Prozac Weekly", "百忧解周制剂"],
        "cat": "ANTIDEPRESSANT", "sub": "SSRI_WEEKLY", "atc": "N06AB03",
        "pk": {"f": 0.7, "ka": 0.4, "tmax": 8.0, "t12": 96.0, "t12r": [72, 144], "vdkg": 35.0, "pb": 95,
               "commonDoseRangeMg": [90.0, 90.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.7}], "inh": [{"cyp": "CYP2D6", "strength": "STRONG"}], "ind": [],
               "note": "氟西汀缓释 (enteric-coated) 90mg qw; 极长 t½ (4-6 天) + 活性代谢物 norfluoxetine (t½ 7-15d), 总半衰期可达 1-6 周; 停药需 5 周洗脱"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 90mg qw (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征致命; 因 t½ 长, 停 fluoxetine 后需 5 周才可开始 MAOI"},
            {"id": "CYP2D6_substrates", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "强 CYP2D6 抑制, 同 paroxetine"},
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.3, 1.5], "s": "MEDIUM", "n": "5-HT 抑制血小板 + 蛋白结合置换, INR 升高"},
            {"id": "tamoxifen", "m": "CYP_INHIBITION_STRONG", "af": [0.3, 0.5], "s": "HIGH", "n": "同 paroxetine, 乳腺癌禁忌"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["抑郁评分", "5-HT 综合征", "骨密度", "钠 (低钠)", "出血风险"]},
        "refs": ["FDA Prozac Weekly Prescribing Information", "PMID: 10867106", "AGNP 2017"],
        "clinicalTrialRefs": ["FDA 标签: 90mg qw Cmax 氟西汀+norfluoxetine 200-500 ng/mL", "PMID 10867106: 维持治疗等同于日服 20mg", "t1/2 母药 4-6d, 代谢物 7-15d"]
    },
    # ============================================================
    # 异构体 (4)
    # ============================================================
    {
        "id": "arformoterol", "name": "Arformoterol", "zh": "阿福特罗 (R-福莫特罗)",
        "brands": ["Brovana", "阿福特罗吸入液"],
        "cat": "OTHER", "sub": "LABA_R_ENANTIOMER", "atc": "R03AC13",
        "pk": {"f": 0.3, "ka": 1.5, "tmax": 1.0, "t12": 26.0, "t12r": [20, 35], "vdkg": 4.0, "pb": 52,
               "commonDoseRangeMg": [0.015, 0.03]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}, {"cyp": "CYP2C19", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "R-福莫特罗 (formoterol 的活性 R 对映体, S-对映体几乎无活性); 长效 β2 激动; COPD 雾化吸入"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "无需调整", "smoking": None},
        "ints": [
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "非选择性 β 阻滞 (普萘洛尔) 降低/抵消 β2 激动效"},
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用加重心血管反应 (心律失常/高血压危象)"},
            {"id": "TCAs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增强心血管反应"},
            {"id": "diuretics_loop_thiazide", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 低钾 → QT 延长风险叠加 β2"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["肺功能 (FEV1)", "心率", "血钾", "心电图 (QT)", "血糖 (DM 患者)"]},
        "refs": ["FDA Brovana Prescribing Information", "PMID: 12690655 (arformoterol 立体化学)", "ATS/ERS COPD 指南"],
        "clinicalTrialRefs": ["FDA 标签: 15 mcg bid 雾化 Cmax ~4 pg/mL, t1/2 26h", "PMID 12690655: R-对映体效力 1000×S-对映体"]
    },
    {
        "id": "levalbuterol", "name": "Levalbuterol", "zh": "左沙丁胺醇 (R-沙丁胺醇)",
        "brands": ["Xopenex", "左旋沙丁胺醇"],
        "cat": "OTHER", "sub": "SABA_R_ENANTIOMER", "atc": "R03AC02",
        "pk": {"f": 0.5, "ka": 2.0, "tmax": 0.5, "t12": 3.3, "t12r": [2.5, 4.5], "vdkg": 2.0, "pb": 12,
               "commonDoseRangeMg": [0.63, 1.25]},
        "cyp": {"subs": [{"cyp": "CYP2A6", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "R-沙丁胺醇 (albuterol 的活性对映体, S-对映体可能反而促气道收缩); 短效 β2 激动; 哮喘/喘息"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "NONE", "elderly": "起始低剂量, 注意震颤/心悸", "smoking": None},
        "ints": [
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "非选择性 β 阻滞抵消 β2 效"},
            {"id": "diuretics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 低钾 → 心律失常"},
            {"id": "MAOIs_TCAs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用增强心血管反应"}
        ],
        "mon": {"freq": "PER_PRESCRIPTION", "items": ["症状控制", "心率", "血钾", "震颤"]},
        "refs": ["FDA Xopenex Prescribing Information", "PMID: 12102631 (levalbuterol vs albuterol)"],
        "clinicalTrialRefs": ["FDA 标签: 1.25mg 雾化 Cmax ~1.1 ng/mL, t1/2 3.3h", "PMID 12102631: R 对映体选择性"]
    },
    {
        "id": "dextroamphetamine", "name": "Dextroamphetamine", "zh": "右旋安非他命",
        "brands": ["Dexedrine", "右旋苯丙胺"],
        "cat": "STIMULANT", "sub": "D_AMPHETAMINE", "atc": "N06BA02",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 2.0, "t12": 10.0, "t12r": [8, 12], "vdkg": 6.0, "pb": 20,
               "commonDoseRangeMg": [5.0, 40.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "D-苯丙胺 (amphetamine 的活性右旋对映体, L-对映体心血管副作用更多); ADHD/发作性睡病"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "N/A", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 高血压危象, 联用 14 天内禁用"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险"},
            {"id": "antipsychotics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低抽搐阈值, 可能拮抗抗精神病效"},
            {"id": "antihypertensives", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低降压效"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["ADHD 评分", "心血管", "精神状态", "滥用风险", "体重"]},
        "refs": ["FDA Dexedrine Prescribing Information", "AGNP 2017"],
        "clinicalTrialRefs": ["FDA 标签: 10mg IR Cmax ~40 ng/mL, t1/2 10h", "D-对映体活性比 L-: 2×中枢, 1×外周"]
    },
    {
        "id": "levetiracetam", "name": "Levetiracetam", "zh": "左乙拉西坦",
        "brands": ["Keppra", "开浦兰"],
        "cat": "ANTIEPILEPTIC", "sub": "LEV_ANTIEPILEPTIC", "atc": "N03AX14",
        "pk": {"f": 1.0, "ka": 1.5, "tmax": 1.0, "t12": 7.0, "t12r": [6, 8], "vdkg": 0.7, "pb": 10,
               "commonDoseRangeMg": [500.0, 3000.0]},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "S-对映体 (S-LEV), R-无活性; 独特机制 (SV2A 突触囊泡蛋白); 不经 CYP 代谢, 肾原型排泄; 无蛋白结合, 相互作用少"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "NONE", "elderly": "起始 500mg bid, 缓滴定", "smoking": None},
        "ints": [
            {"id": "other_antiepileptics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 协同但需监测 CNS 抑制; 拉莫三嗪/丙戊酸浓度可能变化"},
            {"id": "methotrexate", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "LOW", "n": "联用: 减少 MTX 清除 (罕见)"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["癫痫发作频率", "情绪/抑郁", "肾功能 (肾清)", "血常规"]},
        "refs": ["FDA Keppra Prescribing Information", "AGNP 2017"],
        "clinicalTrialRefs": ["FDA 标签: 500mg bid Cmax ~23 μg/mL, t1/2 7h", "肾清率 0.96 mL/min/kg", "无 CYP 代谢"]
    },
    # ============================================================
    # 活性代谢物版 (4)
    # ============================================================
    {
        "id": "desvenlafaxine", "name": "Desvenlafaxine", "zh": "去甲文拉法辛",
        "brands": ["Pristiq", "去甲文拉法辛缓释"],
        "cat": "ANTIDEPRESSANT", "sub": "SNRI_METABOLITE", "atc": "N06AX23",
        "pk": {"f": 0.8, "ka": 0.7, "tmax": 7.5, "t12": 11.0, "t12r": [9, 15], "vdkg": 7.0, "pb": 30,
               "commonDoseRangeMg": [50.0, 200.0]},
        "cyp": {"subs": [{"cyp": "UGT1A1", "fraction": 0.5}, {"cyp": "CYP3A4", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "O-去甲文拉法辛, 文拉法辛活性代谢物 (单独作为药上市); 主要 UGT 葡萄糖醛酸化代谢, 不经 CYP2D6"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 50mg qd", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征致命"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 5-HT 综合征"},
            {"id": "triptans", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 5-HT 综合征 (虽低发生率)"},
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用 INR 略升"}
        ],
        "mon": {"freq": "MONTHLY_FIRST_3_MONTHS_THEN_EVERY_3_MONTHS", "items": ["抑郁评分", "血压", "5-HT 综合征", "肝肾功能"]},
        "refs": ["FDA Pristiq Prescribing Information", "AGNP 2017", "PMID: 14593464"],
        "clinicalTrialRefs": ["FDA 标签: 100mg qd Cmax ~376 ng/mL, t1/2 11h", "UGT 代谢, CYP 影响小"]
    },
    {
        "id": "norfluoxetine", "name": "Norfluoxetine", "zh": "去甲氟西汀",
        "brands": ["活性代谢物 (无单独制剂)"],
        "cat": "ANTIDEPRESSANT", "sub": "SSRI_METABOLITE", "atc": "N06AB04",
        "pk": {"f": 0.7, "ka": 0.4, "tmax": 8.0, "t12": 240.0, "t12r": [168, 360], "vdkg": 35.0, "pb": 95,
               "commonDoseRangeMg": [10.0, 30.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.7}], "inh": [{"cyp": "CYP2D6", "strength": "STRONG"}], "ind": [],
               "note": "氟西汀的活性 N-去甲代谢物; 自身 t½ 极长 (7-15d, 个体差异大), 是氟西汀长 t½ 主因; 与母药同等 SSRI 活性; 临床多为母亲给药产生, 罕见单独给药"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "HIGH", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "Beers 慎用 (长 t½)", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征致命; 长 t½ → 停 norfluoxetine 5 周才可 MAOI"},
            {"id": "CYP2D6_substrates", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "HIGH", "n": "强 CYP2D6 抑制, 同 fluoxetine/paroxetine"},
            {"id": "tamoxifen", "m": "CYP_INHIBITION_STRONG", "af": [0.3, 0.5], "s": "HIGH", "n": "乳腺癌禁忌: 减少 endoxifen (活性代谢物)"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["血药浓度 (稳态 1-2 月)", "抑郁", "5-HT 综合征", "骨密度", "钠"]},
        "refs": ["PMID: 10867106 (fluoxetine PK 综述)", "AGNP 2017", "FDA Prozac (母药)"],
        "clinicalTrialRefs": ["PMID 10867106: norfluoxetine t1/2 7-15d, 个体差异 ~5×", "母药+代谢物 稳态 4 周", "CYP2D6 抑制 母药≈代谢物"]
    },
    {
        "id": "norketamine", "name": "Norketamine", "zh": "去甲氯胺酮",
        "brands": ["活性代谢物 (无单独制剂, 抗抑郁研究中)"],
        "cat": "OTHER", "sub": "NMDA_METABOLITE", "atc": "N/A",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 1.0, "t12": 12.0, "t12r": [8, 16], "vdkg": 4.0, "pb": 25,
               "commonDoseRangeMg": [10.0, 50.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2B6", "fraction": 0.4}], "inh": [], "ind": [],
               "note": "氯胺酮的活性 N-去甲代谢物; 母药 t½ 2-3h, 代谢物 t½ 12h, 抗抑郁效主要来自 norketamine; 鼻喷 esketamine (Spravato) 也产生 norketamine"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "起始低剂量, 监测镇静/解离", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 高血压危象"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重呼吸抑制 (μ-阿片协同)"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用镇静叠加, 恢复期延长"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险 (但临床发生率低)"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["抑郁评分", "血压", "解离/幻觉", "滥用风险", "肝肾"]},
        "refs": ["PMID: 19237834 (ketamine/norketamine PK)", "FDA Spravato (esketamine) 标签"],
        "clinicalTrialRefs": ["PMID 19237834: norketamine t1/2 12h, AUC 母药 3×, 抗抑郁效持续 1 周"]
    },
    {
        "id": "morphine_6_glucuronide", "name": "Morphine-6-Glucuronide", "zh": "吗啡-6-葡萄糖醛酸苷",
        "brands": ["活性代谢物 (无单独制剂, 肾衰蓄积)"],
        "cat": "ANALGESIC", "sub": "OPIOID_METABOLITE", "atc": "N02AX99",
        "pk": {"f": 0.3, "ka": 1.0, "tmax": 1.5, "t12": 1.5, "t12r": [1, 3], "vdkg": 0.15, "pb": 15,
               "commonDoseRangeMg": [5.0, 30.0]},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "吗啡活性 M6G 代谢物 (效价 2× 吗啡, 强 μ-阿片激动); UGT2B7 葡萄糖醛酸化; 肾衰时蓄积 (t½ 延长至 50h), 老年人/肾衰镇静/呼吸抑制主因"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "起始 2.5mg q6h prn, 缓滴定 (肾清 ↓)", "smoking": None},
        "ints": [
            {"id": "renal_failure", "m": "PHARMACOKINETIC_OTHER", "af": [2.0, 5.0], "s": "HIGH", "n": "肾衰蓄积: eGFR<30 时 t½ 50h, 严重呼吸抑制/镇静"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重镇静/呼吸抑制"},
            {"id": "naloxone", "m": "PHARMACODYNAMIC", "af": [0.0, 0.0], "s": "HIGH", "n": "逆转过量 (部分)"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["疼痛评分", "呼吸", "肾功能", "过量风险"]},
        "refs": ["PMID: 1459232 (M6G 临床药理)", "AGNP 2017", "FDA Morphine 标签"],
        "clinicalTrialRefs": ["PMID 1459232: M6G t1/2 1.5h (正常), 50h (肾衰)", "效价 母药 2-4×", "肾清率 母药 10-20×"]
    }
]
