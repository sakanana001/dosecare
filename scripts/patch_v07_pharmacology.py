#!/usr/bin/env python3
"""
v0.6.1: 给 159 药补 pharmacology 字段 (一两句话简短描述)

策略:
- ~50 个常用药: 个性化描述 (药理机制 + 治疗作用)
- ~109 个其它药: 按 DrugCategory 模板自动生成
- 数据源: FDA DailyMed Clinical Pharmacology / Goodman & Gilman / AGNP 2017
"""
import json
from pathlib import Path

p = Path("app/src/main/assets/drugs/v0.6.json")
data = json.loads(p.read_text(encoding="utf-8"))

# 50+ 常用药个性化描述
PERSONAL = {
    # 抗精神病
    "clozapine": "非典型抗精神病 (D2 + 5-HT2A 强拮抗), 对难治性精神分裂唯一有效, 但粒缺风险需常规血检",
    "olanzapine": "非典型抗精神病 (D2/5-HT2A 拮抗), 镇静强, 体重增加/代谢综合征风险高",
    "risperidone": "非典型抗精神病 (D2/5-HT2A 平衡拮抗), 锥体外系反应剂量依赖, 长效针剂可每月一次",
    "quetiapine": "非典型抗精神病 (D2/5-HT2A 弱拮抗 + H1 强拮抗), 镇静强, 治疗双相 + 失眠辅助",
    "aripiprazole": "第三代抗精神病 (D2 部分激动 + 5-HT2A 拮抗), 代谢副作用少, t½ 长 (75h)",
    "ziprasidone": "非典型抗精神病 (D2/5-HT2A 拮抗), 体重影响小但 QTc 延长风险",
    "amisulpride": "非典型抗精神病 (选择性 D2/D3 拮抗, 不经 CYP 代谢), 低剂量抗抑郁",
    "paliperidone": "9-OH 利培酮活性代谢物, 不经肝首过代谢, 长效针剂",
    "lurasidone": "非典型抗精神病 (D2/5-HT2A/5-HT7 拮抗), 代谢中性, 餐 > 350 kcal 服吸收增 2x",
    "cariprazine": "第三代抗精神病 (D3 部分激动 > D2), 治双相抑郁 + 精神分裂阴性症状",
    "brexpiprazole": "第三代抗精神病 (D2 部分激动), 副作用温和, 常作抗抑郁增效",

    # 心境稳定剂
    "valproic_acid": "钠通道阻滞 + GABA 增强 + NMDA 拮抗, 治双相/癫痫/偏头痛, 肝毒性 + 致畸",
    "lithium": "情感稳定剂金标准, 抑制 GSK-3β + 调节 5-HT/DA 神经传递, 中毒指数窄 (治疗窗 0.6-1.2 mmol/L)",
    "lamotrigine": "钠通道阻滞 (抑制谷氨酸释放), 治双相 + 局灶性癫痫, 需慢滴定避免 Stevens-Johnson",

    # 抗抑郁
    "fluoxetine": "SSRI, 半衰期长 (72h, 含活性代谢物诺氟西汀 4-16 天), 治疗抑郁/OCD/贪食",
    "sertraline": "SSRI, 治疗抑郁/OCD/PTSD/焦虑, 胃肠道副作用较其他 SSRI 温和",
    "paroxetine": "SSRI, 抗胆碱能最强 (口干/便秘/体重), 自身抑制 CYP2D6 显著",
    "escitalopram": "SSRI (S-西酞普兰, 最纯活性异构体), 副作用最少, 治疗抑郁/广泛性焦虑",
    "venlafaxine": "SNRI (5-HT/NE 再摄取抑制), 高剂量 NE 效应, 停药需慢滴定",
    "duloxetine": "SNRI, 治疗抑郁/广泛性焦虑/糖尿病周围神经痛/压力性尿失禁",
    "mirtazapine": "NaSSA (α2/H1 强拮抗), 镇静 + 增食欲, 适合失眠 + 食欲下降的抑郁",
    "bupropion": "NDRI (NE/DA 再摄取抑制), 不增体重, 不影响性功能, 戒烟辅助, 但降低癫痫阈值",
    "vortioxetine": "多模式 5-HT 调节 (抑制再摄取 + 多种受体), 改善认知",
    "trazodone": "SARI (5-HT 拮抗 + 再摄取抑制), 强镇静, 治失眠 (低剂量) + 抑郁",
    "amitriptyline": "TCA, 强抗胆碱能 (口干/便秘) + α1 阻滞 (体位性低血压) + QRS 增宽, 治三叉神经痛/偏头痛预防",

    # 抗焦虑/安眠
    "diazepam": "长效 BZD (t½ 20-50h, 含活性代谢物 N-去甲地西泮可达 100h), GABA-A 正向变构, 抗焦虑/肌松/抗惊厥",
    "lorazepam": "中效 BZD (t½ 10-20h), UGT 代谢不经 CYP, 肝病患者首选",
    "alprazolam": "短中效 BZD, 起效快抗惊恐, 依赖性较强 (短 t½ 6-12h)",
    "clonazepam": "长效 BZD, 治惊恐发作 + 癫痫, t½ 30-40h",
    "zolpidem": "非 BZD Z 药 (选择性 GABA-A ω1 受体), 短效助眠, 依赖性比 BZD 低",
    "buspirone": "5-HT1A 部分激动剂, 抗焦虑起效慢 (1-2 周), 无依赖无镇静, 适合慢性焦虑辅助",

    # 抗癫痫
    "carbamazepine": "钠通道阻滞 (失活态), 强 CYP3A4 诱导, 治三叉神经痛/癫痫/双相, 致粒缺 + SIADH",
    "phenytoin": "钠通道阻滞 (失活态), 非线性消除 (剂量微增 → 浓度急升), 治局灶性癫痫 + 三叉神经痛",
    "gabapentin": "α2δ 钙通道亚基结合 (减少突触前 Ca2+ 内流), 治神经病理性痛 + 癫痫",
    "pregabalin": "α2δ 钙通道亚基结合 (同 gabapentin 但效价高 3-10x), 治神经痛/广泛性焦虑/纤维肌痛",
    "lamotrigine": "见心境稳定剂, 也治癫痫",
    "levetiracetam": "SV2A 突触囊泡蛋白结合, 广谱抗癫痫, 副作用少",

    # 帕金森
    "levodopa_carbidopa": "左旋多巴 (DA 前体) + 卡比多巴 (外周多巴脱羧酶抑制剂, 提高中枢浓度), 金标准",
    "pramipexole": "D2/D3 受体激动剂 (非麦角), 治疗早晚期 PD + RLS",
    "ropinirole": "D2/D3 受体激动剂, 同 pramipexole",
    "rasagiline": "MAO-B 不可逆抑制剂, 治 PD, 缓释多巴胺降解, 早期单药有效",

    # 阿尔茨海默
    "donepezil": "中枢乙酰胆碱酯酶 (AChE) 抑制剂, 治轻中度阿尔茨海默, 长效 (t½ 70h)",
    "memantine": "NMDA 受体低亲和力拮抗剂, 治中重度 AD, 阻断谷氨酸兴奋毒性",

    # 抗凝/抗血小板
    "warfarin": "维生素 K 环氧化物还原酶抑制剂, 治华法林需 INR 2-3, 与食物/CYP 相互作用极多",
    "aspirin": "环氧化酶 (COX) 不可逆乙酰化, 抗血小板 (低剂量) + 抗炎 (高剂量), 心血管一级预防",

    # 糖尿病
    "metformin": "AMP 活化蛋白激酶 (AMPK) 激活, 抑制肝糖异生, 治 2 型糖尿病一线",
    "empagliflozin": "SGLT2 抑制剂 (肾小管排糖), 降糖 + 心肾保护",
    "liraglutide": "GLP-1 受体激动剂 (长效), 降糖 + 减重, 皮下注射",
    "sitagliptin": "DPP-4 抑制剂 (阻止内源 GLP-1 降解), 降糖不增重",

    # 降压/心血管
    "amlodipine": "二氢吡啶类 CCB (L 型钙通道阻滞), 长效降压",
    "lisinopril": "ACE 抑制剂, 降压 + 心衰, 干咳 (10%) + 血管性水肿副作用",
    "metoprolol": "β1 选择性阻滞剂, 降压 + 心衰 + 心律失常, 哮喘慎用",

    # 甲状腺
    "levothyroxine": "T4 外源补充, 治甲减, 半衰期 7 天",
    "methimazole": "甲状腺过氧化物酶 (TPO) 抑制剂, 阻断 T3/T4 合成, 治甲亢, 妊娠早期禁忌",

    # 抗生素
    "fluconazole": "三唑类抗真菌 (CYP 介导羊毛甾醇 14α-去甲基化酶抑制, 损真菌膜)",
    "rifampin": "DNA 依赖 RNA 聚合酶抑制, 强 CYP3A4 诱导 (减很多药效), 抗结核",
    "dolutegravir": "HIV 整合酶链转移抑制剂 (INSTI), 抗逆转录病毒一线",

    # 抗凝补充
    "rivaroxaban": "直接 Xa 因子抑制剂 (口服抗凝), 半衰期 5-9h, 肾部分清除",

    # 抗心律失常
    "amiodarone": "III 类抗心律失常 (K+ 通道阻滞, 兼 I/II/IV 效应), 治顽固性室性/房性心律失常, 半衰期 50 天 + 严重肝/肺/甲状腺毒性",
    "sotalol": "III 类抗心律失常 (K+ 阻滞 + β 阻滞), 治室速/房颤, QTc 延长风险",
}

# 按 DrugCategory 自动模板 (用于 109 个未列个性化药)
CATEGORY_TEMPLATE = {
    "ANTIPSYCHOTIC": "非典型/典型抗精神病药, 通过 D2 多巴胺受体拮抗 (+ 5-HT2A 调节) 治疗阳性症状, 部分药对阴性/认知症状有效",
    "MOOD_STABILIZER": "情感稳定剂, 调节神经元兴奋性, 治双相情感障碍躁狂/抑郁相, 部分有抗癫痫作用",
    "ANTIDEPRESSANT": "抗抑郁药, 通过调节 5-HT/NE/DA 单胺神经传递 (再摄取抑制或受体调节) 改善情绪",
    "ANXIOLYTIC": "抗焦虑/镇静药, 主要通过增强 GABA-A 受体活性 (BZD) 或 5-HT1A 受体 (丁螺环酮) 起效",
    "STIMULANT": "中枢兴奋剂, 增强 DA/NE 神经传递, 治 ADHD + 嗜睡症, 滥用风险 II 类管制",
    "ANTICHOLINERGIC": "中枢抗胆碱能药 (M 受体拮抗), 缓解抗精神病药引起的 EPS/肌张力障碍",
    "ANTIPARKINSONIAN": "抗帕金森药, 通过补充多巴胺前体 / 激动 D2 受体 / 抑制 MAO-B 等改善锥体外系症状",
    "ANTIEPILEPTIC": "抗癫痫药, 通过阻滞 Na+/Ca2+ 通道或增强 GABA 抑制谷氨酸释放, 减少神经元异常放电",
    "ALZHEIMERS": "抗痴呆药, 增强中枢胆碱能 (AChE 抑制) 或调节 NMDA 谷氨酸, 改善认知",
    "ANTIMIGRAINE": "抗偏头痛药, 5-HT1B/1D 受体激动收缩脑血管或钙通道阻滞预防",
    "MUSCLE_RELAXANT": "肌松药, 通过 GABA-B 受体激动 (巴氯芬) 或 α2 受体激动 (替扎尼定) 缓解肌痉挛",
    "ANESTHETIC": "麻醉药, 通过 NMDA 拮抗 (氯胺酮) / 钠通道阻滞 (局麻) 等机制产生麻醉/镇痛",
    "ANTIVERTIGO": "抗眩晕药, 改善内耳微循环或 H1/H3 受体调节",
    "ANTIDIABETIC": "降糖药, 机制多样 (AMPK/SGLT2/DPP-4/GLP-1/胰岛素促泌) 降血糖",
    "THYROID": "甲状腺药, 外源补充或抑制甲状腺素合成/释放",
    "CORTICOSTEROID": "糖皮质激素, 抗炎 + 免疫抑制, 广谱治疗自免/过敏/肾上腺功能不全",
    "ANTIHYPERTENSIVE": "降压药, 机制多样 (CCB/ACEI/ARB/β 阻滞/利尿) 降低血压",
    "STATIN": "HMG-CoA 还原酶抑制剂, 降低 LDL 胆固醇 + 心血管保护",
    "ANTIARRHYTHMIC": "抗心律失常药, 通过阻滞 Na+/K+/Ca2+ 通道或 β 受体调节心脏电活动",
    "ANTICOAGULANT": "抗凝/抗血小板药, 抑制凝血级联 (Vit K 依赖/Xa/IIa) 或血小板聚集 (COX1/P2Y12)",
    "PPI": "质子泵抑制剂, 不可逆抑制胃壁细胞 H+/K+ ATPase, 强效抑酸",
    "OSTEOPOROSIS_DRUG": "骨质疏松药, 抑制破骨细胞 (双膦酸盐) 或调节骨代谢",
    "GOUT": "抗痛风药, 抑制黄嘌呤氧化酶 (别嘌醇) 或抗炎 (秋水仙碱) 降尿酸/缓解发作",
    "BPH_AGENT": "前列腺增生药, 5α 还原酶抑制剂 (非那雄胺) 或 α1 受体阻滞 (坦索罗辛) 缩小前列腺/放松平滑肌",
    "HORMONE_REPLACEMENT": "激素替代药, 补充内源激素 (雌激素/孕激素/垂体后叶素)",
    "BRONCHODILATOR": "支气管扩张剂, β2 受体激动 (沙丁胺醇/福莫特罗) 松弛气道平滑肌",
    "ANALGESIC": "镇痛药, 阿片受体激动 (强阿片) 或其他机制 (度洛西汀 SNRI), 部分抗神经病理性痛",
    "ANTIHISTAMINE": "抗组胺药, H1 受体拮抗, 抗过敏/镇静/止吐",
    "ANTIBIOTIC": "抗感染药, 抗真菌 (麦角甾醇合成抑制) / 抗结核 (RNA 聚合酶) / 抗 HIV (整合酶抑制)",
    "SUBSTANCE_USE": "物质依赖治疗药, 阿片拮抗 (纳曲酮) / ALDH 抑制 (双硫仑) / GABA 调节 (阿坎酸)",
    "SUPPLEMENT": "营养补充剂, 补充矿物质/维生素 (钙/铁/叶酸)",
    "HERBAL": "草本/植物药, 替代/补充医学",
    "OTHER": "其他治疗药"
}

n = 0
import collections
cat_counts = collections.Counter()
for drug in data["drugs"]:
    if drug["id"] in PERSONAL:
        drug["pharmacology"] = PERSONAL[drug["id"]]
        n += 1
    else:
        cat = drug.get("category", "OTHER")
        tmpl = CATEGORY_TEMPLATE.get(cat, CATEGORY_TEMPLATE["OTHER"])
        # 用 subcategory 精确化 (e.g. SSRI, SNRI, TCA, BZD, CCB 等)
        subcat = drug.get("subcategory", "")
        # 简单细化: 在分类模板前加上 subcategory 类型
        subcat_zh = {
            "SSRI": "选择性 5-HT 再摄取抑制剂 (SSRI)",
            "SNRI": "5-HT/NE 双重重摄取抑制剂 (SNRI)",
            "TCA": "三环类抗抑郁 (TCA, 5-HT/NE 再摄取抑制 + 抗胆碱/抗 H1/抗 α1)",
            "MAOI_IRREVERSIBLE": "不可逆单胺氧化酶抑制剂 (MAOI, 需注意 tyramine 反应)",
            "MAOI_B_SELECTIVE": "选择性 MAO-B 不可逆抑制剂",
            "NaSSA": "α2 异质性拮抗 + 5-HT2/3 拮抗 (NaSSA)",
            "NDRI": "NE/DA 重摄取抑制剂 (NDRI)",
            "SARI": "5-HT 受体拮抗 + 再摄取抑制 (SARI)",
            "SSRE_5HT_REUPTAKE_ENHANCER": "5-HT 重摄取增强剂 (反向 SSRI)",
            "MULTIMODAL_5HT": "多模式 5-HT 调节 (再摄取 + 多受体)",
            "MT_AGONIST_5HT2C": "褪黑素受体激动 + 5-HT2C 拮抗",
            "5HT1A_PARTIAL_AGONIST": "5-HT1A 部分激动剂",
            "BENZODIAZEPINE_LONG": "长效苯二氮䓬 (GABA-A 正向变构, t½ 20-50h)",
            "BENZODIAZEPINE_INTERMEDIATE": "中效苯二氮䓬 (GABA-A 正向变构)",
            "BENZODIAZEPINE_IMIDAZO": "短效咪达唑仑类 (GABA-A)",
            "BZD_TRIAZOLO": "短效三唑仑类 BZD",
            "BZD_LONG": "长效 BZD",
            "HYPNOTIC_NON_BZD": "非 BZD Z 药助眠 (选择性 GABA-A ω1)",
            "NON_BZD_Z_DRUG_SHORT": "超短效 Z 药",
            "NON_BZD_Z_DRUG": "中效 Z 药",
            "CHLORAL_DERIVATIVE": "水合氯醛类镇静",
            "BARBITURATE_LONG": "长效巴比妥 (GABA-A 长效, 强依赖)",
            "BARBITURATE_INTERMEDIATE": "中效巴比妥",
            "BARBITURATE_SHORT": "短效巴比妥",
            "ANTIHISTAMINE_ANXIOLYTIC": "H1 抗组胺 + 抗焦虑 (羟嗪)",
            "AChE_INHIBITOR": "中枢乙酰胆碱酯酶 (AChE) 抑制剂",
            "AChE_BuChE_INHIBITOR": "AChE + 丁酰胆碱酯酶双重抑制剂",
            "NMDA_RECEPTOR_ANTAGONIST": "NMDA 受体拮抗剂",
            "DOPAMINE_PRECURSOR": "左旋多巴 (DA 前体)",
            "DOPAMINE_AGONIST": "D2 多巴胺受体激动剂",
            "MAO_B_INHIBITOR": "MAO-B 选择性抑制剂",
            "COMT_INHIBITOR": "COMT 抑制剂 (延长左旋多巴作用)",
            "NMDA_ANTAGONIST_DA_AGONIST": "NMDA 拮抗 + DA 激动 (金刚烷胺)",
            "GABA_ANALOG": "GABA 类似物 (α2δ 钙通道结合)",
            "AMPA_RECEPTOR_ANTAGONIST": "AMPA 受体拮抗 (吡仑帕奈)",
            "DIBENZAZEPINE": "二苯并氮䓬类 (钠通道阻滞)",
            "FRUCTOSE_DERIVATIVE": "果糖衍生物 (多机制: 钠通道 + GABA + 碳酸酐酶抑制)",
            "HYDANTOIN": "乙内酰脲类 (钠通道阻滞, 非线性代谢)",
            "PIRACETAM_ANALOG": "吡拉西坦类似物 (SV2A 结合)",
            "SLOW_INACTIVATION_NA_CHANNEL": "钠通道慢失活态阻滞 (拉科酰胺)",
            "ANTIEPILEPTIC": "广谱抗癫痫",
            "ALKALI_METAL": "碱金属离子 (锂, 情绪稳定, 治双相)",
            "ATYPICAL_DIBENZODIAZEPINE": "非典型二苯并氮䓬类抗精神病",
            "ATYPICAL_THIENOBENZODIAZEPINE": "非典型噻吩并苯二氮䓬类抗精神病",
            "ATYPICAL_BENZISOXAZOLE": "非典型苯并异噁唑类抗精神病",
            "ATYPICAL_DIBENZOTHIAZEPINE": "非典型二苯并硫氮䓬类抗精神病",
            "ATYPICAL_QUINOLINONE": "非典型喹诺酮类抗精神病",
            "ATYPICAL_BENZAMIDE": "非典型苯甲酰胺类抗精神病",
            "ATYPICAL_DIBENZOXAZEPINE": "非典型二苯并噁氮䓬类抗精神病",
            "ATYPICAL_PIPERIDINE": "非典型哌啶类抗精神病",
            "ATYPICAL_DERIVATIVE": "非典型衍生类抗精神病",
            "ANTIMUSCARINIC": "中枢 M 胆碱受体拮抗",
            "NSAID_ANTIPLATELET": "非甾体抗炎 + 抗血小板 (COX1 不可逆乙酰化)",
            "ANTIPLATELET": "抗血小板药",
            "OPIOID_FULL_AGONIST": "强阿片受体完全激动剂 (μ)",
            "OPIOID_PARTIAL_AGONIST": "阿片受体部分激动剂 (μ, 顶效应低, 依赖性低)",
            "OPIOID_WEAK": "弱阿片 (μ 部分激动 + 5-HT/NE 重摄取抑制)",
            "OPIOID_PRODRUG": "阿片前药 (代谢为吗啡)",
            "OPIOID_SUPER_AGONIST": "强效 μ 阿片激动剂",
            "OPIOID_METABOLITE": "阿片活性代谢物",
            "OPIOID_ANTAGONIST": "阿片受体拮抗剂",
            "5_ALPHA_REDUCTASE_INHIBITOR": "5α 还原酶抑制剂 (缩小前列腺)",
            "5_ALPHA_REDUCTASE_INHIBITOR_DUAL": "5α 还原酶双重抑制剂 (1 型 + 2 型)",
            "ALPHA_1A_BLOCKER_BPH": "α1A 受体阻滞 (放松前列腺/膀胱颈)",
            "ESTROGEN": "雌激素",
            "PROGESTIN_NATURAL": "天然孕激素",
            "VASOPRESSIN_ANALOG": "抗利尿激素 (V2 受体激动) 类似物",
            "DPP4_INHIBITOR": "DPP-4 抑制剂 (延长内源 GLP-1)",
            "GLP1_AGONIST": "GLP-1 受体激动剂 (长效)",
            "SULFONYLUREA_SHORT": "短效磺酰脲 (促胰岛素分泌)",
            "THIAZOLIDINEDIONE": "噻唑烷二酮 (PPARγ 激动, 增胰岛素敏感性)",
            "BIGUANIDE": "双胍类 (AMPK 激活, 抑制肝糖异生)",
            "SGLT2_INHIBITOR": "SGLT2 抑制剂 (肾小管排糖)",
            "ACE_INHIBITOR": "ACE 抑制剂 (降压 + 心衰)",
            "ARB": "血管紧张素 II 受体 (AT1) 阻滞剂",
            "CCB_DIHYDROPYRIDINE": "二氢吡啶类 CCB",
            "BETA_BLOCKER": "β 受体阻滞剂",
            "HMG_COA_REDUCTASE_INHIBITOR": "HMG-CoA 还原酶抑制剂 (他汀)",
            "T4_HORMONE": "T4 甲状腺激素",
            "ANTITHYROID_TIONAMIDE": "硫酰胺类抗甲状腺 (抑制 TPO)",
            "GLUCOCORTICOID_NATURAL": "天然糖皮质激素 (氢化可的松)",
            "GLUCOCORTICOID_INTERMEDIATE": "中效糖皮质激素",
            "GLUCOCORTICOID_LONG": "长效糖皮质激素",
            "GLUCOCORTICOID_ACTIVE": "活性糖皮质激素",
            "BISPHOSPHONATE": "双膦酸盐 (抑制破骨细胞, 治疗骨质疏松)",
            "VITAMIN_B9": "叶酸 (维生素 B9, DNA 合成 + 甲基化)",
            "IRON_SUPPLEMENT": "亚铁补充 (缺铁性贫血)",
            "CALCIUM_SUPPLEMENT_ANTACID": "钙补充 + 抗酸",
            "XANTHINE_OXIDASE_INHIBITOR": "黄嘌呤氧化酶抑制剂 (降尿酸)",
            "ANTI_INFLAMMATORY_GOUT": "抗炎 + 抗痛风 (微管抑制, 秋水仙碱)",
            "ALDEHYDE_DEHYDROGENASE_INHIBITOR": "乙醛脱氢酶抑制剂 (双硫仑, 戒酒)",
            "GLUTAMATE_MODULATOR": "谷氨酸系统调节剂 (阿坎酸, 戒酒)",
            "GABA_B_AGONIST": "GABA-B 受体激动剂 (肌松)",
            "ALPHA2_AGONIST": "α2 受体激动剂 (肌松, 中枢)",
            "H1_AGONIST_H3_ANTAGONIST": "H1 激动 + H3 拮抗 (倍他司汀, 抗眩晕)",
            "TRIPTAN_5HT1B_1D": "曲坦类 5-HT1B/1D 受体激动 (收缩脑血管)",
            "CALCIUM_CHANNEL_BLOCKER": "钙通道阻滞剂",
            "ANTICOAGULANT_VKA": "维生素 K 拮抗剂 (口服抗凝)",
            "ANTICOAGULANT_DOAC": "直接口服抗凝 (Xa/IIa 抑制)",
            "PPI": "质子泵抑制剂 (H+/K+ ATPase 不可逆抑制)",
            "ANTIARRHYTHMIC_CLASS_III": "III 类抗心律失常 (K+ 通道阻滞, 胺碘酮)",
            "ANTIARRHYTHMIC_BETA_BLOCKER": "III 类 + β 阻滞 (索他洛尔)",
            "LABA_R_ENANTIOMER": "长效 β2 激动剂 R-异构体",
            "SABA_R_ENANTIOMER": "短效 β2 激动剂 R-异构体",
            "STIMULANT_LOCAL_ANESTHETIC": "中枢兴奋 + 局麻 (可卡因)",
            "NMDA_ANTAGONIST_DISSOCIATIVE": "NMDA 拮抗 (分离麻醉, 氯胺酮)",
            "EMPATHOGEN_AMPHETAMINE": "MDMA (3,4-亚甲二氧基甲基苯丙胺, 致幻 + 促产, 治 PTSD)",
            "NMDA_METABOLITE": "氯胺酮活性代谢物 (去甲氯胺酮)",
            "D_AMPHETAMINE": "D-苯丙胺 (更强中枢兴奋)",
            "AMPHETAMINE_RELATED": "苯丙胺相关兴奋剂",
            "WAKING_PROMOTING": "觉醒促进剂 (莫达非尼, 不典型兴奋)",
            "SNRI_NRI": "选择性 NE 重摄取抑制剂 (托莫西汀)",
            "DIBENZAZEPINE": "二苯并氮䓬 (卡马西平/奥卡西平)",
            "SSRI_CR": "SSRI 控释剂",
            "SSRI_WEEKLY": "SSRI 周制剂 (氟西汀缓释)",
            "SNRI_ER": "SNRI 缓释剂",
            "NDRI_ER": "NDRI 缓释剂 (安非他酮 XL)",
            "SSRI_METABOLITE": "SSRI 活性代谢物",
            "SNRI_METABOLITE": "SNRI 活性代谢物",
            "GABA_AGONIST_NATRIUM_OXYBAT": "GABA-B 受体激动 (γ-羟丁酸, 治发作性睡病)",
            "ALKALI_METAL": "碱金属 (锂, 情感稳定)",
            "ANTIEPILEPTIC": "广谱抗癫痫"
        }.get(subcat, None)
        if subcat_zh:
            text = f"{subcat_zh}。{tmpl.split('。', 1)[-1]}"  # 合并: subcat 机制 + 模板治疗作用
            drug["pharmacology"] = text
        else:
            drug["pharmacology"] = tmpl
        n += 1
    cat_counts[drug.get("category", "OTHER")] += 1

p.write_text(
    json.dumps(data, ensure_ascii=False, indent=2),
    encoding="utf-8"
)
print(f"已补 {n} 个药 pharmacology 描述 (159 覆盖)")
print(f"\n按 DrugCategory 分布 (前 10):")
for k, v in cat_counts.most_common(10):
    print(f"  {k}: {v}")
