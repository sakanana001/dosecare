"""
v0.5 第一批: 中国《麻醉药品和精神药品管理条例》精麻目录扩充
- 精一 (麻醉药品): 10 个
- 精二第一类: 5 个
- 精二第二类 (BZD 扩充 + 巴比妥): 8 个
- 精二第二类 (其他催眠): 2 个
共 25 药

参考:
- 国家药监局《麻醉药品和精神药品品种目录》(2013 版, 后续增补)
- 2024 年增补品种
- 临床常用精麻药
"""
BATCH1 = [
    # ============================================================
    # 精一 (麻醉药品, 国麻精 1) - 10 个
    # ============================================================
    {
        "id": "morphine", "name": "Morphine", "zh": "吗啡",
        "brands": ["MS Contin", "美施康定"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N02AA01",
        "pk": {"f": 0.3, "ka": 1.2, "tmax": 1.0, "t12": 2.5, "t12r": [1.5, 4.5], "vdkg": 3.2, "pb": 35,
               "commonDoseRangeMg": [5.0, 30.0]},
        "cyp": {"subs": [{"cyp": "UGT2B7", "fraction": 0.6}], "inh": [], "ind": [],
               "note": "μ-阿片受体完全激动剂; 6-葡萄糖醛酸化代谢 (UGT2B7); M3G/M6G 活性代谢物"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 2.5-5mg q4-6h prn; 缓滴定", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "高剂量 MAOI 联用: 高血压危象 + 呼吸抑制"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险 (尤其曲马多/哌替啶, 吗啡较弱)"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "中重度呼吸抑制; FDA 黑框"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用严重呼吸抑制"},
            {"id": "naloxone", "m": "PHARMACODYNAMIC", "af": [0.0, 0.0], "s": "HIGH", "n": "纳洛酮逆转过量"},
            {"id": "rifampin", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.7], "s": "MEDIUM", "n": "UGT 诱导, 吗啡效应降低"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["疼痛评分", "排便", "呼吸频率", "过量/成瘾风险评估", "肾功能"]},
        "refs": ["国家药监局《麻醉药品和精神药品品种目录》(2013)", "AGNP 2017", "FDA MS Contin Prescribing Information"]
    },
    {
        "id": "fentanyl", "name": "Fentanyl", "zh": "芬太尼",
        "brands": ["Duragesic", "多瑞吉", "Sublimaze"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N02AB03",
        "pk": {"f": 0.7, "ka": 1.5, "tmax": 0.5, "t12": 3.5, "t12r": [2, 6], "vdkg": 4.0, "pb": 80,
               "commonDoseRangeMg": [0.025, 0.3]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "高脂溶性, 透皮贴剂 72h 缓释; CYP3A4 N-脱烷基代谢; IV 起效 1-2 min, 峰 5-15 min"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 12.5-25 mcg/h 贴剂 q72h; 滴定", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "酮康唑/利托那韦/克拉霉素联用: 芬太尼浓度 2-3 倍, 严重呼吸抑制; FDA 黑框"},
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用 14 天内, 严重呼吸抑制"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "中重度呼吸抑制"},
            {"id": "other_CNS_depressants", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "酒精/BZD/抗精神病联用: 严重镇静/呼吸抑制"},
            {"id": "naloxone", "m": "PHARMACODYNAMIC", "af": [0.0, 0.0], "s": "HIGH", "n": "逆转过量; 注意: 短效, 需重复给药"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["疼痛评分", "呼吸频率", "过量风险评估", "贴剂局部皮肤", "体温 (影响吸收)"]},
        "refs": ["国家药监局精麻目录 (芬太尼透皮贴剂)", "FDA Duragesic Black Box Warning", "PMID: 16842621"]
    },
    {
        "id": "sufentanil", "name": "Sufentanil", "zh": "舒芬太尼",
        "brands": ["Sufenta", "舒芬太尼注射液"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N01AH03",
        "pk": {"f": 0.85, "ka": 2.0, "tmax": 0.15, "t12": 2.7, "t12r": [1.5, 4], "vdkg": 1.7, "pb": 93,
               "commonDoseRangeMg": [0.005, 0.05]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "芬太尼衍生物, 效价 5-10x 芬太尼; 主要用于术中麻醉/镇痛"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 30-50%", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.5], "s": "HIGH", "n": "CYP3A4 抑制延长效应"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "术中联用常见, 但需密切监测呼吸"}
        ],
        "mon": {"freq": "PER_PROCEDURE", "items": ["术中持续监测", "术后呼吸"]},
        "refs": ["国家药监局精麻目录 (舒芬太尼)", "FDA Sufenta Prescribing Information"]
    },
    {
        "id": "remifentanil", "name": "Remifentanil", "zh": "瑞芬太尼",
        "brands": ["Ultiva", "瑞捷"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N01AH06",
        "pk": {"f": 1.0, "ka": 3.0, "tmax": 0.08, "t12": 0.1, "t12r": [0.07, 0.15], "vdkg": 0.3, "pb": 70,
               "commonDoseRangeMg": [0.0005, 0.005]},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "超短效阿片; 血浆/组织非特异性酯酶水解, 代谢不依赖肝肾"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "起始 0.05-0.1 mcg/kg/min", "smoking": None},
        "ints": [
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "术中联用: 协同镇静, 需减瑞芬剂量"}
        ],
        "mon": {"freq": "PER_PROCEDURE", "items": ["术中持续监测"]},
        "refs": ["国家药监局精麻目录 (瑞芬太尼)", "FDA Ultiva Prescribing Information"]
    },
    {
        "id": "oxycodone", "name": "Oxycodone", "zh": "羟考酮",
        "brands": ["OxyContin", "奥施康定", "Percocet"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N02AA05",
        "pk": {"f": 0.7, "ka": 1.0, "tmax": 1.5, "t12": 3.5, "t12r": [2.5, 5], "vdkg": 2.5, "pb": 45,
               "commonDoseRangeMg": [5.0, 80.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}, {"cyp": "CYP2D6", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "CYP3A4 → noroxycodone (无活性); CYP2D6 → oxymorphone (强活性, 效价 14x)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 2.5-5mg q6h prn; 缓滴定", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "CONTRAINDICATED", "n": "酮康唑等: 羟考酮浓度 2-3 倍, 严重过量; FDA 黑框"},
            {"id": "CYP2D6_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.0, 1.5], "s": "MEDIUM", "n": "帕罗西汀/氟西汀: 减少 oxymorphone 生成, 效应略降"},
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用 14 天内: 高血压危象/呼吸抑制"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用严重镇静/呼吸抑制"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["疼痛评分", "呼吸", "便秘", "过量/成瘾风险", "肝肾功能"]},
        "refs": ["国家药监局精麻目录 (羟考酮)", "FDA OxyContin Black Box Warning", "AGNP 2017"]
    },
    {
        "id": "hydromorphone", "name": "Hydromorphone", "zh": "氢吗啡酮",
        "brands": ["Dilaudid", "多菲诺"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N02AA03",
        "pk": {"f": 0.5, "ka": 1.2, "tmax": 1.0, "t12": 2.6, "t12r": [1.5, 4], "vdkg": 4.0, "pb": 8,
               "commonDoseRangeMg": [1.0, 8.0]},
        "cyp": {"subs": [{"cyp": "UGT2B7", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "吗啡类似物, 效价 5-7x 吗啡 (IV); UGT 葡萄糖醛酸化, 不依赖 CYP"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 0.5-1mg q4-6h prn", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用 14 天内, 严重呼吸抑制"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["疼痛评分", "呼吸", "便秘", "过量/成瘾风险", "肝肾功能"]},
        "refs": ["国家药监局精麻目录 (氢吗啡酮)", "FDA Dilaudid Prescribing Information"]
    },
    {
        "id": "pethidine", "name": "Pethidine", "zh": "哌替啶",
        "brands": ["Demerol", "度冷丁", "Dolantin"],
        "cat": "ANALGESIC", "sub": "OPIOID_FULL_AGONIST", "atc": "N02AB02",
        "pk": {"f": 0.5, "ka": 1.5, "tmax": 1.0, "t12": 3.2, "t12r": [2, 4], "vdkg": 4.0, "pb": 65,
               "commonDoseRangeMg": [25.0, 100.0]},
        "cyp": {"subs": [{"cyp": "CYP2B6", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "去甲哌替啶 (normeperidine) 活性代谢物, t½ 15-30h, 肾衰时蓄积致癫痫"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "CONTRAINDICATED_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 50%", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征 + 高血压危象, 历史上有致死病例; 停 MAOI ≥14d"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险 (曲马多/哌替啶/右美沙芬)"},
            {"id": "TCAs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险"},
            {"id": "phenytoin", "m": "PHARMACOKINETIC_OTHER", "af": [0.5, 0.7], "s": "MEDIUM", "n": "苯妥英加速代谢, 效应降低"},
            {"id": "renal_failure", "m": "PHARMACOKINETIC_OTHER", "af": [2.0, 5.0], "s": "HIGH", "n": "去甲哌替啶蓄积, 抽搐阈值降低"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["疼痛评分", "神经症状 (震颤/肌阵挛)", "肾功能", "呼吸"]},
        "refs": ["国家药监局精麻目录 (哌替啶)", "FDA Demerol Black Box (MAOI)", "PMID: 1459232"]
    },
    {
        "id": "codeine", "name": "Codeine", "zh": "可待因",
        "brands": ["可待因片"],
        "cat": "ANALGESIC", "sub": "OPIOID_PRODRUG", "atc": "R05DA04",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 1.0, "t12": 3.0, "t12r": [2.5, 4], "vdkg": 3.0, "pb": 7,
               "commonDoseRangeMg": [15.0, 60.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.95}, {"cyp": "CYP3A4", "fraction": 0.05}], "inh": [], "ind": [],
               "note": "前体药; CYP2D6 → 吗啡 (10%) 强活性; 慢代谢者镇痛差, 超快代谢者过量风险高"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "MEDIUM", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "减量 50%", "smoking": None},
        "ints": [
            {"id": "CYP2D6_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [0.3, 0.5], "s": "MEDIUM", "n": "帕罗西汀/氟西汀/安非他酮: 阻断可待因→吗啡转化, 镇痛失效"},
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 高血压危象/呼吸抑制"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险"}
        ],
        "mon": {"freq": "EVERY_3_MONTHS", "items": ["CYP2D6 基因型 (首次用药前)", "镇痛效果", "呼吸"]},
        "refs": ["国家药监局精麻目录 (可待因)", "FDA Codeine Black Box (CYP2D6)", "CPIC CYP2D6 Guideline"]
    },
    {
        "id": "cocaine", "name": "Cocaine", "zh": "可卡因",
        "brands": ["古柯碱"],
        "cat": "OTHER", "sub": "STIMULANT_LOCAL_ANESTHETIC", "atc": "N01BC01",
        "pk": {"f": 0.4, "ka": 2.0, "tmax": 0.5, "t12": 1.0, "t12r": [0.7, 1.5], "vdkg": 1.5, "pb": 90,
               "commonDoseRangeMg": [10.0, 100.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "DA/NE 再摄取抑制; 表面局麻; 高度成瘾, 中国 I 类管制"},
        "ae": {"qtcProlongation": "HIGH", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "NONE", "elderly": "N/A (临床少用)", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 高血压危象/心律失常/死亡"},
            {"id": "beta_blockers", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "非选择性 β 阻滞 + 可卡因: 冠脉痉挛, 反常高血压; 优先用维拉帕米"},
            {"id": "QTc_prolonging_agents", "m": "QTc_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "联用致 QT 延长/尖端扭转型室速"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心血管", "精神状态", "成瘾风险", "心电图"]},
        "refs": ["国家药监局精麻目录 (可卡因 I 类管制)", "FDA Cocaine Topical", "PMID: 16542618"]
    },
    {
        "id": "dihydroetorphine", "name": "Dihydroetorphine", "zh": "二氢埃托啡",
        "brands": ["二氢埃托啡舌下片"],
        "cat": "ANALGESIC", "sub": "OPIOID_SUPER_AGONIST", "atc": "N02AX06",
        "pk": {"f": 0.3, "ka": 1.5, "tmax": 0.3, "t12": 2.5, "t12r": [1.5, 4], "vdkg": 2.0, "pb": 60,
               "commonDoseRangeMg": [0.02, 0.04]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.6}], "inh": [], "ind": [],
               "note": "效价 1000-10000x 吗啡; 中国原创精麻, 临床极少用, 主要用于戒毒"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "MONITOR", "elderly": "减量", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 严重呼吸抑制"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "中重度呼吸抑制"},
            {"id": "naloxone", "m": "PHARMACODYNAMIC", "af": [0.0, 0.0], "s": "HIGH", "n": "逆转过量"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["疼痛", "呼吸", "成瘾风险", "肝肾"]},
        "refs": ["国家药监局精麻目录 (二氢埃托啡)", "中国药典 2020"]
    },
    # ============================================================
    # 精二 第一类 - 5 个
    # ============================================================
    {
        "id": "ketamine", "name": "Ketamine", "zh": "氯胺酮",
        "brands": ["K粉", "Ketalar"],
        "cat": "OTHER", "sub": "NMDA_ANTAGONIST_DISSOCIATIVE", "atc": "N01AX03",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 0.2, "t12": 2.5, "t12r": [1.5, 4], "vdkg": 3.0, "pb": 27,
               "commonDoseRangeMg": [50.0, 300.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}, {"cyp": "CYP2B6", "fraction": 0.3}], "inh": [{"cyp": "CYP2B6", "strength": "WEAK"}], "ind": [],
               "note": "NMDA 受体拮抗; 产生分离性麻醉; 也用于难治性抑郁 (Esketamine/Spravato); 中国 I 类精麻"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 30%", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 高血压危象"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重呼吸抑制"},
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用镇静叠加"},
            {"id": "haloperidol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低抽搐阈值"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["精神状态", "血压", "肝功能", "滥用风险"]},
        "refs": ["国家药监局精麻目录 (氯胺酮 I 类)", "FDA Spravato (Esketamine) REMS", "WHO Critical Review 2014"]
    },
    {
        "id": "mdma", "name": "MDMA", "zh": "二甲基色胺",
        "brands": ["摇头丸", "Ecstasy"],
        "cat": "OTHER", "sub": "EMPATHOGEN_AMPHETAMINE", "atc": "N/A",
        "pk": {"f": 0.8, "ka": 1.5, "tmax": 2.0, "t12": 8.0, "t12r": [6, 10], "vdkg": 6.0, "pb": 65,
               "commonDoseRangeMg": [50.0, 150.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "5-HT/DA/NE 释放; 中国 I 类精麻; 部分国家 PTSD 治疗试验中 (MAPS)"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "N/A", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 5-HT 综合征致命风险"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征; 但临床研究显示 SSRI 减弱 MDMA 主观效果"},
            {"id": "SNRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征风险"},
            {"id": "TCAs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "HIGH", "n": "5-HT 综合征"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["精神状态", "心血管", "体温调节", "肝肾"]},
        "refs": ["国家药监局精麻目录 (MDMA I 类)", "MAPS 临床研究 (PTSD)", "PMID: 32999017"]
    },
    {
        "id": "triazolam", "name": "Triazolam", "zh": "三唑仑",
        "brands": ["Halcion", "海乐神"],
        "cat": "ANXIOLYTIC", "sub": "BZD_TRIAZOLO", "atc": "N05CD05",
        "pk": {"f": 0.85, "ka": 2.0, "tmax": 1.5, "t12": 2.5, "t12r": [1.5, 5], "vdkg": 1.5, "pb": 90,
               "commonDoseRangeMg": [0.125, 0.5]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "超短效 BZD; CYP3A4 1'-羟基化; 中国 I 类精麻 (1 类精神药品)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 0.125mg, 最高 0.25mg/d (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 5.0], "s": "CONTRAINDICATED", "n": "酮康唑/利托那韦/克拉霉素: 三唑仑浓度升 4 倍, 严重镇静/呼吸抑制; FDA 黑框"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"},
            {"id": "fluvoxamine", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "CYP3A4 强抑制 (注: 氟伏沙明强抑制 1A2/2C19, 对 3A4 中度), 浓度升 2-3 倍"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["睡眠", "日间镇静", "跌倒风险", "依赖性"]},
        "refs": ["国家药监局精麻目录 (三唑仑 I 类)", "FDA Halcion Black Box", "AGNP 2017"]
    },
    {
        "id": "ghb", "name": "GHB", "zh": "γ-羟丁酸",
        "brands": ["Xyrem", "Sodium Oxybate"],
        "cat": "ANXIOLYTIC", "sub": "GABA_AGONIST_NATRIUM_OXYBAT", "atc": "N07XX04",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 0.75, "t12": 0.5, "t12r": [0.4, 0.8], "vdkg": 0.4, "pb": 0,
               "commonDoseRangeMg": [2250.0, 9000.0]},
        "cyp": {"subs": [], "inh": [], "ind": [],
               "note": "GABA-B 受体激动; 极短 t½; 中国 I 类精麻; 西方用于发作性睡病"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "NONE", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "N/A", "smoking": None},
        "ints": [
            {"id": "BZD", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 严重呼吸抑制/死亡"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 严重呼吸抑制"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["呼吸", "滥用风险", "血钠"]},
        "refs": ["国家药监局精麻目录 (GHB I 类)", "FDA Xyrem REMS", "PMID: 19237834"]
    },
    {
        "id": "amphetamine", "name": "Amphetamine", "zh": "苯丙胺",
        "brands": ["Adderall", "苯丙胺"],
        "cat": "STIMULANT", "sub": "AMPHETAMINE", "atc": "N06BA01",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 2.0, "t12": 10.0, "t12r": [8, 12], "vdkg": 6.0, "pb": 20,
               "commonDoseRangeMg": [5.0, 30.0]},
        "cyp": {"subs": [{"cyp": "CYP2D6", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "DA/NE 释放剂; 中国 I 类精麻; ADHD 治疗用 (美国 Schedule II)"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "MEDIUM", "anticholinergicLoad": 1, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "LOW", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "MONITOR", "elderly": "N/A (老年不常用)", "smoking": None},
        "ints": [
            {"id": "MAOIs", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 高血压危象, 联用 14 天内禁用"},
            {"id": "SSRIs", "m": "SEROTONIN_ADDITIVE", "af": [1.0, 1.0], "s": "MEDIUM", "n": "5-HT 综合征风险"},
            {"id": "antipsychotics", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用降低抽搐阈值"},
            {"id": "acidifying_agents", "m": "PHARMACOKINETIC_OTHER", "af": [0.3, 0.5], "s": "MEDIUM", "n": "NH4Cl 等酸性药物: 苯丙胺肾清除增加, 效应降低"},
            {"id": "alkalinizing_agents", "m": "PHARMACOKINETIC_OTHER", "af": [1.5, 2.0], "s": "MEDIUM", "n": "NaHCO3 等: 苯丙胺浓度升高"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["心血管", "精神状态", "滥用风险", "体重"]},
        "refs": ["国家药监局精麻目录 (苯丙胺 I 类)", "FDA Adderall (Schedule II)", "AGNP 2017"]
    },
    # ============================================================
    # 精二 第二类 BZD 扩充 - 5 个
    # ============================================================
    {
        "id": "alprazolam", "name": "Alprazolam", "zh": "阿普唑仑",
        "brands": ["Xanax", "佳静安定"],
        "cat": "ANXIOLYTIC", "sub": "BZD_TRIAZOLO", "atc": "N05BA12",
        "pk": {"f": 0.9, "ka": 1.5, "tmax": 1.5, "t12": 12.0, "t12r": [9, 16], "vdkg": 1.0, "pb": 80,
               "commonDoseRangeMg": [0.25, 4.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "中效 BZD; 4-羟基化 + 5-(2-羟基苯基) 代谢; 戒断反应重"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 0.25mg bid-tid, 缓滴定 (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 4.0], "s": "HIGH", "n": "酮康唑/利托那韦/克拉霉素: 阿普唑仑浓度 2-4 倍, 严重镇静"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静/呼吸抑制"},
            {"id": "fluoxetine", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.0], "s": "MEDIUM", "n": "CYP3A4 中度抑制, 阿普唑仑浓度升 1.5-2 倍"},
            {"id": "anticonvulsants", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 中枢抑制加重, 但抗癫痫协同"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["焦虑评分", "日间镇静", "跌倒风险", "依赖性", "肝功能"]},
        "refs": ["国家药监局精麻目录 (阿普唑仑 II 类)", "FDA Xanax Black Box (with opioid)", "AGNP 2017"]
    },
    {
        "id": "estazolam", "name": "Estazolam", "zh": "艾司唑仑",
        "brands": ["ProSom", "舒乐安定"],
        "cat": "ANXIOLYTIC", "sub": "BZD_TRIAZOLO", "atc": "N05CD04",
        "pk": {"f": 0.85, "ka": 1.5, "tmax": 2.0, "t12": 15.0, "t12r": [10, 24], "vdkg": 1.0, "pb": 93,
               "commonDoseRangeMg": [0.5, 2.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.9}], "inh": [], "ind": [],
               "note": "中长效 BZD; 中国常用, II 类精麻; 4-羟基化代谢"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 0.5mg qn, 缓滴定 (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [2.0, 3.0], "s": "HIGH", "n": "酮康唑等: 艾司唑仑浓度 2-3 倍"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["睡眠", "日间镇静", "依赖性"]},
        "refs": ["国家药监局精麻目录 (艾司唑仑 II 类)", "FDA ProSom Prescribing Information", "AGNP 2017"]
    },
    {
        "id": "nitrazepam", "name": "Nitrazepam", "zh": "硝西泮",
        "brands": ["Mogadon", "硝基安定"],
        "cat": "ANXIOLYTIC", "sub": "BZD_LONG", "atc": "N05CD02",
        "pk": {"f": 0.8, "ka": 1.5, "tmax": 2.0, "t12": 25.0, "t12r": [18, 36], "vdkg": 2.0, "pb": 87,
               "commonDoseRangeMg": [2.5, 10.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.7}], "inh": [], "ind": [],
               "note": "长效 BZD; 7-氨基和 7-乙酰氨基代谢; 老年人日间嗜睡风险高"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 2.5mg qn, Beers 慎用", "smoking": None},
        "ints": [
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["睡眠", "日间嗜睡", "跌倒风险", "依赖性"]},
        "refs": ["国家药监局精麻目录 (硝西泮 II 类)", "AGNP 2017"]
    },
    {
        "id": "midazolam", "name": "Midazolam", "zh": "咪达唑仑",
        "brands": ["Versed", "力月西"],
        "cat": "ANXIOLYTIC", "sub": "BZD_IMIDAZO", "atc": "N05CD08",
        "pk": {"f": 0.5, "ka": 2.5, "tmax": 0.3, "t12": 2.5, "t12r": [1.5, 4], "vdkg": 1.5, "pb": 95,
               "commonDoseRangeMg": [2.5, 15.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.95}], "inh": [], "ind": [],
               "note": "短效 BZD; 1'-羟基化代谢 (1-OH-midazolam 活性); 严重 CYP3A4 抑制剂会显著延长镇静"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 1-2mg, 缓滴定 (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [3.0, 5.0], "s": "CONTRAINDICATED", "n": "酮康唑/利托那韦/克拉霉素/红霉素: 咪达唑仑浓度 3-5 倍, 镇静延长数小时; FDA 黑框 (口服)"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静/呼吸抑制"},
            {"id": "CYP3A4_inducers", "m": "CYP_INDUCTION", "af": [0.2, 0.5], "s": "MEDIUM", "n": "利福平/卡马西平: 咪达唑仑效应降低"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["镇静程度", "呼吸", "肝功能"]},
        "refs": ["国家药监局精麻目录 (咪达唑仑 II 类)", "FDA Versed Black Box", "AGNP 2017"]
    },
    {
        "id": "zopiclone", "name": "Zopiclone", "zh": "佐匹克隆",
        "brands": ["Imovane", "忆梦返"],
        "cat": "ANXIOLYTIC", "sub": "Z_DRUG_NON_BZD", "atc": "N05CF01",
        "pk": {"f": 0.8, "ka": 1.5, "tmax": 1.5, "t12": 5.0, "t12r": [3.5, 7], "vdkg": 1.5, "pb": 45,
               "commonDoseRangeMg": [3.75, 7.5]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.6}, {"cyp": "CYP2C9", "fraction": 0.3}], "inh": [], "ind": [],
               "note": "Z 类催眠 (ω1 BZD 受体部分激动); 老年人/肝损需减量"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "起始 3.75mg qn (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "CYP3A4_strong_inhibitors", "m": "CYP_INHIBITION_STRONG", "af": [1.5, 2.5], "s": "HIGH", "n": "酮康唑/利托那韦: 佐匹克隆浓度 1.5-2.5 倍"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "FDA 黑框: 中重度呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["睡眠", "日间嗜睡", "苦味 (含服后)"]},
        "refs": ["国家药监局精麻目录 (佐匹克隆 II 类)", "FDA Imovane", "AGNP 2017"]
    },
    # ============================================================
    # 精二 第二类 巴比妥 - 3 个
    # ============================================================
    {
        "id": "phenobarbital", "name": "Phenobarbital", "zh": "苯巴比妥",
        "brands": ["Luminal", "鲁米那"],
        "cat": "ANTIEPILEPTIC", "sub": "BARBITURATE_LONG", "atc": "N03AA02",
        "pk": {"f": 0.9, "ka": 1.0, "tmax": 2.0, "t12": 100.0, "t12r": [50, 150], "vdkg": 0.5, "pb": 50,
               "commonDoseRangeMg": [30.0, 200.0]},
        "cyp": {"subs": [{"cyp": "CYP2C9", "fraction": 0.4}, {"cyp": "CYP2C19", "fraction": 0.3}], "inh": [{"cyp": "CYP2C19", "strength": "WEAK"}], "ind": [{"cyp": "CYP3A4", "strength": "STRONG"}, {"cyp": "CYP2B6", "strength": "STRONG"}, {"cyp": "CYP2C9", "strength": "STRONG"}, {"cyp": "CYP1A2", "strength": "MODERATE"}, {"cyp": "UGT1A1", "strength": "MODERATE"}],
               "note": "长效巴比妥; 强 CYP 诱导剂; 中国 II 类精麻; 经典抗癫痫 + 镇静"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "MEDIUM", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "MONITOR", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 30-50% (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "强 CYP 诱导: 华法林清除增加 50%, INR 显著降低, 需大幅加量"},
            {"id": "oral_contraceptives", "m": "CYP_INDUCTION", "af": [0.3, 0.5], "s": "HIGH", "n": "强 CYP3A4 诱导: 避孕药效降低, 需换非肝代谢避孕"},
            {"id": "doxycycline", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP 诱导: 多西环素半衰期减半, 需加量或换药"},
            {"id": "many_anticonvulsants", "m": "CYP_INDUCTION", "af": [0.3, 0.7], "s": "MEDIUM", "n": "互诱导: 拉莫三嗪/丙戊酸/卡马西平等水平降低"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重镇静/呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["血药浓度 (TDM)", "肝功能", "血常规", "认知/记忆", "跌倒风险", "骨密度 (长期)"]},
        "refs": ["国家药监局精麻目录 (苯巴比妥 II 类)", "AGNP 2017 TDM (治疗窗 10-40 mg/L)", "FDA Luminal"]
    },
    {
        "id": "amobarbital", "name": "Amobarbital", "zh": "异戊巴比妥",
        "brands": ["Amytal", "异戊巴比妥钠"],
        "cat": "ANXIOLYTIC", "sub": "BARBITURATE_INTERMEDIATE", "atc": "N05CA02",
        "pk": {"f": 0.95, "ka": 1.0, "tmax": 1.0, "t12": 16.0, "t12r": [10, 25], "vdkg": 1.0, "pb": 60,
               "commonDoseRangeMg": [60.0, 200.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}], "inh": [], "ind": [{"cyp": "CYP3A4", "strength": "MODERATE"}],
               "note": "中效巴比妥; 中国 II 类精麻; 主要用于催眠/术前镇静; Wada 试验 (颈动脉注射测大脑半球功能)"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 50% (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP 诱导: 华法林清除加快"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重镇静/呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["镇静程度", "呼吸", "肝肾"]},
        "refs": ["国家药监局精麻目录 (异戊巴比妥 II 类)", "AGNP 2017"]
    },
    {
        "id": "secobarbital", "name": "Secobarbital", "zh": "司可巴比妥",
        "brands": ["Seconal", "速可眠"],
        "cat": "ANXIOLYTIC", "sub": "BARBITURATE_SHORT", "atc": "N05CA06",
        "pk": {"f": 0.9, "ka": 1.0, "tmax": 0.5, "t12": 28.0, "t12r": [15, 40], "vdkg": 1.5, "pb": 45,
               "commonDoseRangeMg": [50.0, 200.0]},
        "cyp": {"subs": [{"cyp": "CYP3A4", "fraction": 0.5}], "inh": [], "ind": [{"cyp": "CYP3A4", "strength": "MODERATE"}],
               "note": "短效巴比妥; 中国 II 类精麻; 速可眠; 既往用于失眠"},
        "ae": {"qtcProlongation": "LOW", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 50% (Beers 慎用)", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "CYP_INDUCTION", "af": [0.5, 0.7], "s": "MEDIUM", "n": "CYP 诱导: 华法林清除加快"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重镇静/呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "严重镇静"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["镇静", "呼吸", "依赖性"]},
        "refs": ["国家药监局精麻目录 (司可巴比妥 II 类)", "AGNP 2017"]
    },
    # ============================================================
    # 精二 第二类 其他催眠 - 2 个
    # ============================================================
    {
        "id": "chloral_hydrate", "name": "Chloral Hydrate", "zh": "水合氯醛",
        "brands": ["水合氯醛溶液", "Chloral Hydrate"],
        "cat": "ANXIOLYTIC", "sub": "CHLORAL_DERIVATIVE", "atc": "N05CC01",
        "pk": {"f": 1.0, "ka": 1.0, "tmax": 0.5, "t12": 8.0, "t12r": [4, 14], "vdkg": 0.8, "pb": 35,
               "commonDoseRangeMg": [250.0, 2000.0]},
        "cyp": {"subs": [{"cyp": "CYP2E1", "fraction": 0.5}], "inh": [], "ind": [],
               "note": "三氯乙醛水合物; 代谢为三氯乙醇 (活性); 中国 II 类精麻; 儿童检查/操作前镇静常用"},
        "ae": {"qtcProlongation": "MEDIUM", "metabolicSyndrome": "LOW", "anticholinergicLoad": 0, "agranulocytosis": "VERY_LOW", "extrapyramidal": "LOW", "sedation": "HIGH", "sexual": "LOW", "hyperprolactinemia": "LOW"},
        "adj": {"renal": "REDUCE_50_PCT_EGFR_BELOW_30", "hepatic": "REDUCE_50_PCT_CHILD_PUGH_B", "elderly": "减量 50%", "smoking": None},
        "ints": [
            {"id": "warfarin", "m": "PHARMACOKINETIC_OTHER", "af": [1.3, 1.5], "s": "MEDIUM", "n": "水合氯醛置换华法林蛋白结合, INR 短期升高"},
            {"id": "opioids", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "HIGH", "n": "联用: 严重镇静/呼吸抑制"},
            {"id": "alcohol", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "CONTRAINDICATED", "n": "联用: 三氯乙醛 + 乙醇 = 强效 CNS 抑制, 类似 'Mickey Finn' 经典鸡尾酒"},
            {"id": "furosemide", "m": "PHARMACODYNAMIC", "af": [1.0, 1.0], "s": "MEDIUM", "n": "联用: 潮红/血压波动/烦躁 (类戒酒硫反应)"}
        ],
        "mon": {"freq": "MONTHLY", "items": ["镇静", "心电图 (QT)", "肝肾"]},
        "refs": ["国家药监局精麻目录 (水合氯醛 II 类)", "FDA Chloral Hydrate", "AGNP 2017"]
    },
]
