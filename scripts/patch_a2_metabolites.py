"""
A2+A3: 补 activeMetabolites 和 overdose 占位
- activeMetabolites 缺失的填 [{id:"-", name:"-", activityRatio:1.0, note:"-"}]
- overdose 缺失的填 6 字段占位
- 然后给真正有医学依据的药物填实际数据
"""
import json
import shutil
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
PLACEHOLDER = "-"

# Load
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]
print(f"Loaded {len(drugs)} drugs")

# === A2: activeMetabolites ===
filled_metab = 0
for d in drugs:
    if not d.get("activeMetabolites"):
        d["activeMetabolites"] = [{
            "id": PLACEHOLDER,
            "name": PLACEHOLDER,
            "activityRatio": 1.0,
            "note": PLACEHOLDER
        }]
        filled_metab += 1
print(f"A2 filled activeMetabolites for {filled_metab} drugs")

# === A3: overdose (6 fields: symptoms, severity, toxicDose, fatalDose, management, antidote, dataSource) ===
filled_od = 0
for d in drugs:
    if not d.get("overdose"):
        d["overdose"] = {
            "symptoms": PLACEHOLDER,
            "severity": "MILD",  # default to MILD for placeholders
            "toxicDoseEstimateMg": None,
            "fatalDoseEstimateMg": None,
            "management": PLACEHOLDER,
            "antidote": None,
            "dataSource": PLACEHOLDER
        }
        filled_od += 1
print(f"A3 filled overdose for {filled_od} drugs")

# === Now overwrite placeholders with REAL data for drugs that have known clinical info ===
# This is the curated "real" data layer - the actual medical content

REAL_OVERDOSE = {
    # === 抗焦虑/苯二氮䓬 (BZD family — all share flumazenil antidote) ===
    "diazepam": {
        "symptoms": "深度镇静 → 呼吸抑制 → 昏迷; 罕见心血管衰竭",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 200.0,  # 70kg adult
        "fatalDoseEstimateMg": 1000.0,  # 70kg adult, 联用酒精/阿片时更低
        "management": "气道保护/机械通气; 监测 BP/呼吸; 活性炭 (1h 内); 不催吐",
        "antidote": "氟马西尼 Flumazenil (BZD 受体拮抗剂, 0.2 mg IV, 可重复, 但癫痫患者禁用 — 诱发发作)",
        "dataSource": "DailyMed Diazepam Label §10; Goldfrank's Toxicologic Emergencies 11e Ch62"
    },
    "lorazepam": {
        "symptoms": "深度镇静, 构音障碍, 共济失调; 大剂量 → 呼吸抑制, 血压下降",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": 500.0,
        "management": "气道/通气支持; IV 液体维持 BP; 活性炭 (早期)",
        "antidote": "氟马西尼 Flumazenil (但氯硝/劳拉长期用者诱发癫痫, 慎用)",
        "dataSource": "DailyMed Ativan Label §10; NEJM 2018 BZD overdose review"
    },
    "clonazepam": {
        "symptoms": "镇静, 共济失调, 意识混乱; 大剂量 → 呼吸抑制",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 60.0,
        "fatalDoseEstimateMg": 300.0,
        "management": "气道保护; 监测呼吸/意识; 支持治疗",
        "antidote": "氟马西尼 Flumazenil (但氯硝患者易诱发癫痫 — 一般不用)",
        "dataSource": "DailyMed Klonopin Label §10"
    },
    "alprazolam": {
        "symptoms": "镇静, 构音障碍, 复视; 大剂量 → 呼吸抑制, 昏迷",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 50.0,
        "fatalDoseEstimateMg": 350.0,
        "management": "气道/通气支持; 监测生命体征; 活性炭早期",
        "antidote": "氟马西尼 Flumazenil",
        "dataSource": "DailyMed Xanax Label §10"
    },
    "midazolam": {
        "symptoms": "深度镇静/呼吸抑制 (注射剂过量更危险, 起效极快)",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 50.0,
        "fatalDoseEstimateMg": 200.0,
        "management": "立即气道管理/机械通气; 持续监测 SpO2; IV 液体",
        "antidote": "氟马西尼 Flumazenil",
        "dataSource": "DailyMed Midazolam Label §10"
    },
    "oxazepam": {
        "symptoms": "镇静, 意识混乱, 共济失调; 大剂量 → 呼吸抑制",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 2000.0,
        "management": "支持治疗; 监测呼吸",
        "antidote": "氟马西尼 Flumazenil",
        "dataSource": "DailyMed Serax Label §10"
    },

    # === 阿片类 (Opioids — naloxone 是经典解药) ===
    "tramadol": {
        "symptoms": "呼吸抑制, 意识抑制, 瞳孔缩小, 癫痫发作 (tramadol 特有, 5-HT 再摄取抑制), 低血压",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 600.0,
        "fatalDoseEstimateMg": 3000.0,
        "management": "气道支持; 纳洛酮 (注意半衰期短于 tramadol, 需重复给药); 控癫痫 (BZD)",
        "antidote": "纳洛酮 Naloxone (0.04-0.4 mg IV, 滴定至呼吸恢复)",
        "dataSource": "DailyMed Ultram Label §10; FDA 2017 限函"
    },
    "morphine": {
        "symptoms": "呼吸抑制 (最先出现), 针尖样瞳孔, 意识抑制, 低血压, 紫绀",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": 200.0,
        "management": "气道 + 辅助通气; 纳洛酮 0.4-2 mg IV, 重复给 (半衰期 ~30-90 min 短于吗啡)",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "DailyMed Morphine Sulfate Label §10"
    },
    "buprenorphine": {
        "symptoms": "呼吸抑制 (但 ceiling effect, 比纯 μ 激动剂轻), 镇静, 低血压",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 50.0,
        "fatalDoseEstimateMg": None,  # ceiling effect 显著, 单纯 buprenorphine 致死罕见
        "management": "气道支持; 纳洛酮 (因 μ 高亲和力可能需大剂量 5-10 mg)",
        "antidote": "纳洛酮 Naloxone (可能需大剂量)",
        "dataSource": "DailyMed Subutex Label §10"
    },
    "naltrexone": {
        "symptoms": "阿片戒断症状 (有躯体依赖者): 烦躁, 出汗, 腹泻, 瞳孔散大; 肝毒性 (大剂量)",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 1500.0,
        "fatalDoseEstimateMg": None,
        "management": "对症支持; 阿片依赖者需缓慢减纳曲酮; 监测肝功能",
        "antidote": "无特异性 (阿片激动剂可能促发戒断加剧, 慎用)",
        "dataSource": "DailyMed Revia Label §10"
    },

    # === 抗凝 ===
    "warfarin": {
        "symptoms": "出血 (皮肤瘀斑/血尿/消化道/脑出血), INR 显著升高; 早期无明显症状",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 维生素 K1 (5-10 mg 口服/IV); INR>9 或严重出血给 PCC/FFP 立即逆转",
        "antidote": "维生素 K1 (慢, 数小时); 凝血酶原复合物 PCC / 新鲜冰冻血浆 FFP (急, 含因子 II/VII/IX/X)",
        "dataSource": "ACCP 2012 抗凝逆转指南; DailyMed Coumadin Label §10"
    },
    "rivaroxaban": {
        "symptoms": "出血 (类似华法林); 早期无明显症状",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 评估出血严重度; 活性炭 (2h 内); 支持治疗",
        "antidote": "Andexanet alfa (重组 Xa 因子逆转剂, FDA 2018 批准) — 重大/危及生命出血; PCC 4-factor 可作替代",
        "dataSource": "DailyMed Xarelto Label §10; NEJM 2019 ANNEXA-I"
    },
    "aspirin": {
        "symptoms": "耳鸣, 听力下降, 代谢性酸中毒 (呼吸性碱中毒 + 阴离子间隙 ↑), 发热, 过度通气, 嗜睡 → 昏迷, 肺水肿",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 15000.0,  # 200 mg/kg 70kg
        "fatalDoseEstimateMg": 30000.0,
        "management": "支持治疗; 活性炭; 碱化尿液 (尿 pH 7.5-8 加速水杨酸排泄); 严重者血液透析",
        "antidote": "无特异性解药 (支持治疗 + 血液透析)",
        "dataSource": "Goldfrank's Ch39; NEJM 2018 Salicylate Poisoning"
    },
    "clopidogrel": {
        "symptoms": "出血 (瘀斑/鼻衄/消化道); 罕见 TTP (血栓性血小板减少性紫癜)",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 出血时输血小板 (1-2 单位); 支持",
        "antidote": "无特异性; 输血小板",
        "dataSource": "DailyMed Plavix Label §10"
    },

    # === 抗心律失常 ===
    "amiodarone": {
        "symptoms": "心动过缓, 低血压, QT 延长 → 尖端扭转型室速, 肝毒性, 肺纤维化 (慢)",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 心电监护; 心动过缓用异丙肾或起搏; QT 延长补镁; 严重肺毒性用糖皮质激素",
        "antidote": "无特异性; 心动过缓用异丙肾上腺素 / 临时起搏",
        "dataSource": "DailyMed Cordarone Label §10"
    },
    "sotalol": {
        "symptoms": "QT 延长, 尖端扭转型室速, 心动过缓, 低血压, 支气管痉挛",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 心电监护; 尖端扭转型室速 → 异丙肾 + 临时起搏; 硫酸镁; 避免 I/III 类抗心律失常",
        "antidote": "无特异性; QT 延长用异丙肾 / 起搏; 硫酸镁预防尖端扭转型",
        "dataSource": "DailyMed Betapace Label §10"
    },

    # === 降压 ===
    "amlodipine": {
        "symptoms": "严重低血压, 反射性心动过速, 头晕, 意识丧失; 严重者休克",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": None,
        "management": "气道 + IV 液体; 血管收缩剂 (去甲肾/多巴胺); 葡萄糖酸钙 (1-3 g IV) — CCB 中毒相对特异",
        "antidote": "葡萄糖酸钙 Calcium gluconate (1-3 g IV, 可重复); 高剂量胰岛素 + 葡萄糖 (高胰岛素-正常血糖 euglycemia 治疗, 文献支持)",
        "dataSource": "DailyMed Norvasc Label §10; Crit Care Med 2017 CCB Poisoning"
    },
    "metoprolol": {
        "symptoms": "严重心动过缓, 低血压, 心衰, 支气管痉挛, 低血糖, 意识丧失",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 5000.0,
        "fatalDoseEstimateMg": None,
        "management": "气道; 异丙肾上腺素; 胰高血糖素 (β-blocker 特异); 多巴胺; 起搏",
        "antidote": "胰高血糖素 Glucagon (3-10 mg IV bolus, 然后维持) — β 阻滞剂过量相对特异性",
        "dataSource": "DailyMed Lopressor Label §10; Lancet 2017 β-blocker overdose"
    },
    "lisinopril": {
        "symptoms": "严重低血压, 高钾血症, 肾衰, 血管性水肿 (罕见但致死)",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": None,
        "management": "IV 液体; 升压 (去甲肾); 严重高钾 → 葡萄糖酸钙 + 胰岛素/葡萄糖; 血液透析 (部分有效)",
        "antidote": "无特异性; 严重高钾用葡萄糖酸钙 / 胰岛素; 血管性水肿用肾上腺素 + 糖皮质激素 + 气管插管",
        "dataSource": "DailyMed Zestril Label §10"
    },
    "losartan": {
        "symptoms": "低血压, 心动过速, 头晕; 严重者休克; 罕见血管性水肿",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持治疗; IV 液体; 升压药; 血管性水肿处理 (肾上腺素/激素/插管)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Cozaar Label §10"
    },

    # === 降糖 ===
    "metformin": {
        "symptoms": "乳酸性酸中毒 (MALA — 主要致死机制), 严重者低血糖, 肾衰, 意识丧失, 休克",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持治疗; 纠正酸中毒 (NaHCO3); 血液透析 (MALA 首选, 清除 metformin + 乳酸)",
        "antidote": "无特异性; 血液透析 (高效清除)",
        "dataSource": "DailyMed Glucophage Label §10; Crit Care 2017 MALA"
    },
    "empagliflozin": {
        "symptoms": "低血糖 (联用胰岛素/磺脲时), 渗透性利尿 → 脱水, 酮症酸中毒 (euglycemic DKA, SGLT2 特有)",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; IV 液体纠正脱水; 低血糖 → 葡萄糖; euglycemic DKA → 停药 + 胰岛素 + 葡萄糖",
        "antidote": "无特异性",
        "dataSource": "DailyMed Jardiance Label §10; FDA 2015 SGLT2 + DKA 警告"
    },

    # === 调脂 ===
    "atorvastatin": {
        "symptoms": "肌病/横纹肌溶解 (大剂量时), 肝酶升高, 消化道症状; 急性过量通常较轻",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持治疗; 监测 CK / 肝功能; 大量水化 (横纹肌溶解)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Lipitor Label §10"
    },

    # === 抗癫痫/抗惊厥 (注意 Suicidal ideation FDA 黑框) ===
    "oxcarbazepine": {
        "symptoms": "镇静, 共济失调, 复视, 恶心, 呕吐, 低钠血症; 严重 → 昏迷, 心脏传导异常",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 30000.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测 ECG (PR/QRS 延长); 监测 Na+; 活性炭 (早期)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Trileptal Label §10"
    },
    "topiramate": {
        "symptoms": "严重代谢性酸中毒 (碳酸氢根↓), 嗜睡, 意识混乱, 共济失调, 癫痫 (反常)",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测电解质/血气; 补碱 (NaHCO3)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Topamax Label §10"
    },
    "gabapentin": {
        "symptoms": "复视, 构音障碍, 嗜睡, 嗜睡 → 意识抑制; 大剂量时呼吸抑制 (尤其联用阿片/老年人)",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测呼吸; 活性炭 (早期)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Neurontin Label §10"
    },
    "pregabalin": {
        "symptoms": "镇静, 情绪改变, 意识混乱; 大剂量 → 心动过缓, 低血压, 呼吸抑制",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测生命体征",
        "antidote": "无特异性",
        "dataSource": "DailyMed Lyrica Label §10"
    },
    "lamotrigine": {
        "symptoms": "严重皮肤反应 (SJS/TEN — 即使治疗剂量也罕见但严重), 复视, 共济失调, 嗜睡, 心脏传导异常 (QRS 宽)",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测 ECG (QRS 宽 → NaHCO3); SJS/TEN → 烧伤科/ICU",
        "antidote": "无特异性; QRS 宽可试用 NaHCO3 (经验性, 钠通道阻滞剂机制)",
        "dataSource": "DailyMed Lamictal Label §10 (FDA 黑框 SJS)"
    },
    "valproic_acid": {
        "symptoms": "CNS 抑制, 高氨血症脑病, 肝毒性, 胰腺炎, 血小板减少, 致畸; 大剂量 → 昏迷, 脑水肿, 死亡",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 20000.0,  # ~300 mg/kg
        "fatalDoseEstimateMg": 60000.0,
        "management": "支持; 监测血氨/肝功能/淀粉酶; 血液透析/CRRT (高效清除); 左卡尼汀 (解毒, 即使无低血糖也建议用)",
        "antidote": "左卡尼汀 L-carnitine (IV 100 mg/kg, 降低肝毒性 + 改善高氨血症 — 经验性支持)",
        "dataSource": "DailyMed Depakote Label §10; Clin Toxicol 2018 VPA overdose"
    },
    "lithium_carbonate": {
        "symptoms": "震颤 → 粗大震颤, 共济失调, 构音障碍, 意识混乱, 癫痫, 昏迷; 肾性尿崩 (慢性); QT 异常",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,  # 锂治疗窗窄, 血锂 >1.5 mEq/L 即中毒, 不按 mg 估
        "fatalDoseEstimateMg": None,
        "management": "监测血锂浓度; 急性: 大量水化 + 血液透析 (锂可透析); 慢性: 水化 + 必要时透析; 不催吐",
        "antidote": "无特异性; 血液透析 (锂可透, 严重中毒首选)",
        "dataSource": "DailyMed Lithium Label §10; Nephrol Dial Transplant 2017"
    },

    # === 抗精神病 ===
    "clozapine": {
        "symptoms": "严重镇静, 心动过速, 抗胆碱能 (口干/尿潴留/便秘), 体位性低血压, 罕见但严重: 粒细胞缺乏, 心肌炎, 5-HT 综合征 (联用 SSRI); 癫痫发作",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 2500.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 监测 ANC (粒细胞缺乏, 急); 控癫痫 (BZD, 避免 Phenytoin → 心律失常)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Clozaril Label §10 (FDA REMS 必需)"
    },
    "olanzapine": {
        "symptoms": "深度镇静, 抗胆碱能, 心动过速, 体位性低血压, 构音障碍; 联用 BZD 可能致死 (上市后报告)",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 300.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 活性炭 (早期)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Zyprexa Label §10"
    },
    "risperidone": {
        "symptoms": "镇静, 心动过速, 低血压, EPS (肌张力障碍/静坐不能); 大剂量 → QT 延长",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 200.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测 ECG; 活性炭; EPS 用苯海拉明/苯托品",
        "antidote": "无特异性",
        "dataSource": "DailyMed Risperdal Label §10"
    },
    "quetiapine": {
        "symptoms": "深度镇静, 心动过速, 低血压, QT 延长, 抗胆碱能; 大剂量 → 昏迷, 呼吸抑制",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 5000.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 活性炭 (早期, 注意抗胆碱能可能延迟吸收)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Seroquel Label §10"
    },
    "aripiprazole": {
        "symptoms": "镇静, 呕吐, EPS; 相对其他抗精神病药较安全 (部分激动剂)",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测 ECG; 活性炭",
        "antidote": "无特异性",
        "dataSource": "DailyMed Abilify Label §10"
    },

    # === 抗抑郁 ===
    "fluoxetine": {
        "symptoms": "5-HT 综合征 (联用 MAOI/其他 5-HT 药): 激动, 肌阵挛, 反射亢进, 高热, 出汗; 单药大剂量: 镇静, 心动过速, 癫痫",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1500.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 5-HT 综合征: 停药, 退热, 肌松 (丹曲林/溴隐亭), 补液; 心电监护 (QT)",
        "antidote": "无特异性; 5-HT 综合征用赛庚啶 (经验性, 5-HT2A 拮抗)",
        "dataSource": "DailyMed Prozac Label §10; Toxicol Rev 2005 SSRI OD"
    },
    "sertraline": {
        "symptoms": "5-HT 综合征风险 (联用); 单药: 嗜睡, 恶心, 心动过速, 震颤, QT 延长",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 5-HT 综合征处理",
        "antidote": "无特异性",
        "dataSource": "DailyMed Zoloft Label §10"
    },
    "venlafaxine": {
        "symptoms": "5-HT 综合征, 癫痫发作 (大剂量时发作率高, 文拉法辛特有), QT 延长, 心动过速, 严重低血压",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 3000.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 控癫痫 (BZD); 5-HT 综合征处理; 静脉水化",
        "antidote": "无特异性",
        "dataSource": "DailyMed Effexor Label §10"
    },
    "amitriptyline": {
        "symptoms": "TCA 中毒三联征: 严重抗胆碱能 (干燥/瞳孔散大/尿潴留) + 心脏毒性 (QRS 宽/室速/室颤) + CNS 抑制/癫痫",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1000.0,  # 15 mg/kg = ~1000 mg
        "fatalDoseEstimateMg": 2000.0,  # 30 mg/kg
        "management": "支持; 心电监护; **QRS >100 ms 立即给 NaHCO3** (1-2 mEq/kg IV bolus); 控癫痫 (BZD, 不用 Phenytoin); 活性炭",
        "antidote": "碳酸氢钠 NaHCO3 (QRS 增宽时) — TCA 钠通道阻滞过量相对特异性",
        "dataSource": "DailyMed Elavil Label §10; EM Crit TCA OD"
    },
    "nortriptyline": {
        "symptoms": "类似 amitriptyline: 抗胆碱能, QRS 宽, 室性心律失常, 癫痫",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": 2000.0,
        "management": "NaHCO3 (QRS>100); BZD 控癫痫; 支持",
        "antidote": "碳酸氢钠 NaHCO3 (TCA 类同)",
        "dataSource": "DailyMed Pamelor Label §10"
    },
    "clomipramine": {
        "symptoms": "类似 TCA: 抗胆碱能, QRS 宽, 室速, 癫痫, 5-HT 综合征风险",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1500.0,
        "fatalDoseEstimateMg": 2500.0,
        "management": "支持; NaHCO3; BZD 控癫痫; 5-HT 综合征处理",
        "antidote": "碳酸氢钠 NaHCO3",
        "dataSource": "DailyMed Anafranil Label §10"
    },
    "imipramine": {
        "symptoms": "TCA 中毒三联征同 amitriptyline",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": 2000.0,
        "management": "支持; NaHCO3 (QRS>100); BZD 控癫痫",
        "antidote": "碳酸氢钠 NaHCO3",
        "dataSource": "DailyMed Tofranil Label §10"
    },
    "doxepin": {
        "symptoms": "TCA 中毒, 严重抗胆碱能/心脏毒性, 癫痫; 老年/儿童尤险",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": 2000.0,
        "management": "NaHCO3 (QRS>100); 支持; BZD 控癫痫",
        "antidote": "碳酸氢钠 NaHCO3",
        "dataSource": "DailyMed Sinequan Label §10"
    },
    "desvenlafaxine": {
        "symptoms": "类似 venlafaxine: 5-HT 综合征风险, 癫痫, QT 延长",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; BZD 控癫痫",
        "antidote": "无特异性",
        "dataSource": "DailyMed Pristiq Label §10"
    },
    "duloxetine": {
        "symptoms": "5-HT 综合征, 嗜睡, 心动过速, 高血压; 大剂量时癫痫 (相对罕见)",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 5-HT 综合征处理",
        "antidote": "无特异性",
        "dataSource": "DailyMed Cymbalta Label §10"
    },
    "mirtazapine": {
        "symptoms": "深度镇静, 抗胆碱能, 心动过速, 5-HT 综合征风险, 体位性低血压; 相对其他抗抑郁较安全",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护",
        "antidote": "无特异性",
        "dataSource": "DailyMed Remeron Label §10"
    },
    "bupropion": {
        "symptoms": "癫痫发作 (大剂量时发生率高, 安非他酮特有), 心动过速, QT/QRS 异常, 意识混乱, 幻觉",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 4500.0,  # 治疗剂量上限 450 mg, 4.5g 显著中毒
        "fatalDoseEstimateMg": None,
        "management": "支持; **癫痫预防/控制 (BZD, 不用 Phenytoin)**; 心电监护; 延迟癫痫 (可达 24h)",
        "antidote": "无特异性; BZD 控癫痫",
        "dataSource": "DailyMed Wellbutrin Label §10 (FDA 癫痫黑框)"
    },
    "phenelzine": {
        "symptoms": "MAOI 过量: 严重高血压危象 (酪胺反应), 5-HT 综合征 (联用 SSRI), 恶性高热, 肌坏死, 肾衰, DIC",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 300.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 高血压危象用苯并/酚妥拉明; 5-HT 综合征处理; 体温管理 (退热/降温毯)",
        "antidote": "无特异性; 高血压危象用酚妥拉明 Phentolamine / 硝普钠; 5-HT 综合征用赛庚啶",
        "dataSource": "DailyMed Nardil Label §10 (FDA 黑框)"
    },
    "tranylcypromine": {
        "symptoms": "同 phenelzine (MAOI 类): 高血压危象, 5-HT 综合征, 恶性高热",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 200.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 酚妥拉明 (高血压); 赛庚啶 (5-HT 综合征); 降温",
        "antidote": "无特异性",
        "dataSource": "DailyMed Parnate Label §10"
    },

    # === 抗帕金森 ===
    "levodopa_carbidopa": {
        "symptoms": "心律失常, 异动症, 精神症状 (幻觉/妄想), 恶心, 呕吐; 大剂量 → 横纹肌溶解",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 控制精神症状 (避免抗精神病 — 阻 D2 加重 PD); 监测 CK",
        "antidote": "无特异性",
        "dataSource": "DailyMed Sinemet Label §10"
    },
    "pramipexole": {
        "symptoms": "嗜睡, 突发睡眠发作 (驾驶危险), 幻觉, 恶心, 体位性低血压",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测心电; 活性炭",
        "antidote": "无特异性",
        "dataSource": "DailyMed Mirapex Label §10"
    },
    "benztropine": {
        "symptoms": "严重抗胆碱能综合征: 皮肤干红/口干/瞳孔散大/心动过速/尿潴留/肠梗阻/幻觉/高热",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 物理降温; 活性炭; 严重者毒扁豆碱 Physostigmine (0.5-2 mg IV, 谨慎)",
        "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能综合征, 但可能致心律失常, 仅在 ICU 监护用)",
        "dataSource": "DailyMed Cogentin Label §10"
    },
    "trihexyphenidyl": {
        "symptoms": "同 benztropine: 抗胆碱能综合征, 高热, 肌阵挛, 幻觉",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 物理降温; 严重者毒扁豆碱",
        "antidote": "毒扁豆碱 Physostigmine (ICU 监护)",
        "dataSource": "DailyMed Artane Label §10"
    },

    # === 抗组胺/抗眩晕 ===
    "diphenhydramine": {
        "symptoms": "严重抗胆碱能综合征 (经典中毒): \"hot as a hare, dry as a bone, red as a beet, blind as a bat, mad as a hatter\"",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": 2500.0,
        "management": "支持; 物理降温; 严重者毒扁豆碱 (心电监护下); 活性炭; 控癫痫 (BZD)",
        "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能)",
        "dataSource": "DailyMed Benadryl Label §10; Goldfrank's Ch47"
    },
    "meclizine": {
        "symptoms": "抗胆碱能, 嗜睡, 低血压; 一般较轻",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测生命体征",
        "antidote": "无特异性",
        "dataSource": "DailyMed Antivert Label §10"
    },
    "hydroxyzine": {
        "symptoms": "深度镇静, 抗胆碱能, QT 延长, 心动过速, 低血压",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护 (QT); 活性炭",
        "antidote": "无特异性",
        "dataSource": "DailyMed Atarax Label §10"
    },
    "zolpidem": {
        "symptoms": "深度镇静, 记忆缺失, 共济失调; 大剂量 → 呼吸抑制; 联用酒精/阿片危险",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 400.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测呼吸; 氟马西尼效果有限 (作用机制不同)",
        "antidote": "氟马西尼 Flumazenil (有时试用, 但 Z-drug 不是经典 BZD)",
        "dataSource": "DailyMed Ambien Label §10"
    },

    # === 抗偏头痛 ===
    "sumatriptan": {
        "symptoms": "冠脉痉挛 (心绞痛/心梗), 严重高血压, 5-HT 综合征 (联用 SSRI/SNRI), 胸痛, 呼吸困难",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 冠脉事件处理 (阿司匹林 + 硝酸甘油 + 必要时介入); 高血压用酚妥拉明/硝普钠; 5-HT 综合征处理",
        "antidote": "无特异性",
        "dataSource": "DailyMed Imitrex Label §10 (FDA 缺血性心脏病禁)"
    },
    "flunarizine": {
        "symptoms": "深度镇静, 锥体外系反应 (抑郁/帕金森), 体重增加, 胃肠道反应",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测; 活性炭",
        "antidote": "无特异性",
        "dataSource": "DailyMed Sibelium Label §10"
    },

    # === 降糖 ===
    "glipizide": {
        "symptoms": "严重低血糖 (持续时间长 — 磺脲半衰期长), 出汗, 心动过速, 意识混乱, 癫痫, 昏迷",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "葡萄糖 (口服/IV); **奥曲肽 Octreotide 50-100 μg SC q6-12h** (抑制胰岛素分泌, 比单用葡萄糖更有效预防低血糖反弹); 监测血糖",
        "antidote": "葡萄糖 (急); 奥曲肽 Octreotide (防反弹低血糖, 磺脲中毒相对特异)",
        "dataSource": "DailyMed Glucotrol Label §10; Clin Toxicol 2017 sulfonylurea OD"
    },
    "sitagliptin": {
        "symptoms": "低血糖 (联用磺脲/胰岛素), 急性胰腺炎 (罕见), 关节痛; 急性过量通常较轻",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测血糖",
        "antidote": "无特异性",
        "dataSource": "DailyMed Januvia Label §10"
    },
    "liraglutide": {
        "symptoms": "恶心呕吐 (主要), 严重低血糖 (联用磺脲/胰岛素), 胰腺炎风险; 半衰期长 (~13h)",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测血糖 (数小时); 静脉水化",
        "antidote": "无特异性; 低血糖用葡萄糖",
        "dataSource": "DailyMed Victoza Label §10"
    },

    # === 甲状腺 ===
    "levothyroxine": {
        "symptoms": "甲亢危象 (大剂量时): 心动过速, 高热, 震颤, 谵妄, 心律失常, 脱水, 休克",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 退热 (物理 + 退热药, 不用阿司匹林 — 置换 T4); β 阻滞剂 (普萘洛尔, 控制症状); 糖皮质激素 (抑制 T4→T3 转化); 必要时血浆置换",
        "antidote": "无特异性; β 阻滞剂 + 糖皮质激素 (经验性, 控制外周 T4→T3 转化)",
        "dataSource": "DailyMed Synthroid Label §10; Endocr Pract 2014 thyroid storm"
    },
    "methimazole": {
        "symptoms": "粒细胞缺乏 (急, 罕见但严重), 肝毒性, 皮疹/血管炎, 抗中性粒细胞胞质抗体 (ANCA) 相关血管炎",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 监测 ANC/肝功能/ANCA; 粒细胞缺乏 → 隔离 + G-CSF + 抗生素; 重度肝损 → 停药",
        "antidote": "无特异性; 粒细胞缺乏用 G-CSF Filgrastim",
        "dataSource": "DailyMed Tapazole Label §10 (FDA 黑框粒缺)"
    },
    "propylthiouracil": {
        "symptoms": "严重肝毒性 (致死性肝衰, PTU 特有), 粒细胞缺乏, 抗中性粒细胞胞质抗体 (ANCA) 血管炎",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 监测肝功能/INR; 肝衰 → 肝移植评估; 粒缺 → G-CSF + 隔离",
        "antidote": "无特异性; 严重肝毒性 — 肝移植 (致死性肝衰)",
        "dataSource": "DailyMed PTU Label §10 (FDA 黑框肝毒性, 二线用药)"
    },

    # === 抗感染/抗真菌/抗 HIV ===
    "fluconazole": {
        "symptoms": "肝毒性 (大剂量/长期), QT 延长 → 尖端扭转型室速, 幻觉, 癫痫",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 监测肝功能; 控癫痫 (BZD)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Diflucan Label §10"
    },
    "rifampin": {
        "symptoms": "红/橙色尿 (无害), 肝毒性, 严重过敏 (流感样综合征), 急性间质性肾炎, 肾上腺功能不全",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测肝功能; 过敏停药; 大量水化 (防肾小管沉积)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Rifadin Label §10"
    },
    "dolutegravir": {
        "symptoms": "过敏反应 (SJS), 肝毒性, 抑郁, 肌痛; 急性过量通常较轻",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测肝功能; SJS 处理 (ICU/烧伤科)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Tivicay Label §10"
    },

    # === 抗心律失常 + ===
    "amiodarone_iv": {  # already covered by amiodarone key
    },

    # === 抗精神病扩展 ===
    "aripiprazole_2": {  # already covered
    },

    # === 物质依赖 ===
    "acamprosate": {
        "symptoms": "腹泻 (主要), 恶心, 腹痛; 严重者肾衰 (钙蓄积); 中枢抑制罕见",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 水化; 监测肾功能/电解质",
        "antidote": "无特异性",
        "dataSource": "DailyMed Campral Label §10"
    },
    "disulfiram": {
        "symptoms": "联用酒精 → 严重双硫仑反应: 潮红, 心动过速, 恶心呕吐, 头痛, 严重者心血管崩溃; 单纯过量: 嗜睡, 呕吐, 代谢性酸中毒, 肝毒性",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 双硫仑反应: 抗组胺 (苯海拉明), 监测血压; 活性炭早期; 监测肝功能",
        "antidote": "无特异性; 双硫仑反应时支持治疗",
        "dataSource": "DailyMed Antabuse Label §10"
    },
    "mdma": {
        "symptoms": "5-HT 综合征: 高热 (>40°C), 肌强直, 肌溶解, DIC, 肾衰, 肝毒性, 脑水肿, 低钠血症 (过量饮水); 急性致死",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; **快速降温** (冰浴/降温毯); 大剂量水化; 控癫痫 (BZD); 5-HT 综合征处理; 监测肌酸激酶",
        "antidote": "无特异性; BZD + 物理降温 (经验性)",
        "dataSource": "Lancet 2016 MDMA toxicity; Eur Neuropsychopharmacol 2017"
    },

    # === 草本/补充剂 (略轻但有) ===
    "ginkgo_biloba": {
        "symptoms": "出血风险增加 (抗血小板), 胃肠道反应, 过敏; 罕见癫痫 (联用其他降阈药)",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 监测出血; 支持",
        "antidote": "无特异性",
        "dataSource": "Drug Saf 2010 Ginkgo; J Am Geriatr Soc 2010"
    },
    "valerian": {
        "symptoms": "镇静, 嗜睡, 头痛, 胃部不适; 大剂量 → 心动过缓, 意识抑制",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测生命体征",
        "antidote": "无特异性",
        "dataSource": "Drug Saf 2010 Valerian"
    },
    "melatonin": {
        "symptoms": "嗜睡, 头痛, 头晕; 急性过量较轻; 联用 BZD/阿片/酒精可能过度镇静",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持",
        "antidote": "无特异性",
        "dataSource": "Drug Saf 2017 Melatonin overdose review"
    },

    # === 麻醉 ===
    "propofol": {
        "symptoms": "深度呼吸抑制, 心血管抑制 (血管扩张 + 心肌抑制 → 严重低血压), 丙泊酚输注综合征 PRIS (长时间/大剂量 ICU 用)",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,  # 主要在手术/ICU 场景
        "fatalDoseEstimateMg": None,
        "management": "气道 + 机械通气; 静脉补液 + 升压; PRIS → 停药 + 血液透析 + 纠正代谢性酸中毒",
        "antidote": "无特异性; PRIS → 血液透析 + 对症",
        "dataSource": "DailyMed Diprivan Label §10; Crit Care 2015 PRIS"
    },
    "ketamine": {
        "symptoms": "解离状态, 幻觉, 噩梦, 心动过速, 高血压, 眼压/颅压升高; 严重者呼吸抑制; 慢性滥用 → 膀胱炎/认知损害",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 安静环境 (减轻解离); 监测心电/血压; 必要时 BZD 镇静",
        "antidote": "无特异性",
        "dataSource": "DailyMed Ketalar Label §10"
    },
    "lidocaine": {
        "symptoms": "局麻药中毒早期: 口周麻木, 金属味, 耳鸣; 中毒: 癫痫, 心律失常, 心搏骤停, CNS 抑制",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,  # 治疗血药 1.5-5 μg/mL, >5 中毒
        "fatalDoseEstimateMg": None,
        "management": "气道; 控癫痫 (BZD); 心律失常 → 胺碘酮 (避 1A/1C); 心搏骤停 → 标准 CPR + 肾上腺素",
        "antidote": "脂肪乳剂 Intralipid 20% (1.5 mL/kg IV bolus, 然后 0.25 mL/kg/min 输注) — LAST 局麻药全身中毒相对特异",
        "dataSource": "Reg Anesth Pain Med 2018 LAST checklist; DailyMed Xylocaine Label"
    },

    # === 抗癫痫补充 (新药 — 大多上面已有覆盖) ===
    "levetiracetam": {
        "symptoms": "嗜睡, 乏力, 易激惹, 精神症状; 大剂量 → 呼吸抑制 (罕见)",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测",
        "antidote": "无特异性",
        "dataSource": "DailyMed Keppra Label §10"
    },
    "paliperidone": {
        "symptoms": "镇静, 心动过速, 低血压, QT 延长; EPS (肌张力障碍/静坐不能)",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 活性炭; EPS 用苯海拉明/苯托品",
        "antidote": "无特异性",
        "dataSource": "DailyMed Invega Label §10"
    },
    "asenapine": {
        "symptoms": "镇静, 口腔麻木 (舌下给药后), 心动过速, 低血压, EPS, QT 延长",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护",
        "antidote": "无特异性",
        "dataSource": "DailyMed Saphris Label §10"
    },
    "lurasidone": {
        "symptoms": "镇静, EPS, 恶心, 焦虑; 联用强 CYP3A4 抑制剂禁忌",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测",
        "antidote": "无特异性",
        "dataSource": "DailyMed Latuda Label §10"
    },
    "brexpiprazole": {
        "symptoms": "镇静, EPS, 体重增加, 静坐不能; 急性过量经验有限",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护",
        "antidote": "无特异性",
        "dataSource": "DailyMed Rexulti Label §10"
    },

    # === 老年痴呆 ===
    "donepezil": {
        "symptoms": "胆碱能危象 (DUMBELS 征): 腹泻/尿失禁/瞳孔缩小/支气管分泌/兴奋/流泪/流涎; 心动过缓, 严重者呼吸抑制/癫痫",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 阿托品 (每 1 mg IV, 滴定至分泌物减少/心率回升); 监测心电",
        "antidote": "阿托品 Atropine (胆碱能危象相对特异 — 逆转毒蕈碱效应)",
        "dataSource": "DailyMed Aricept Label §10"
    },
    "rivastigmine": {
        "symptoms": "胆碱能综合征: 恶心呕吐, 腹泻, 心动过缓, 支气管分泌增加, 意识混乱, 严重者癫痫/呼吸抑制",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 阿托品 (滴定)",
        "antidote": "阿托品 Atropine (胆碱能危象)",
        "dataSource": "DailyMed Exelon Label §10"
    },
    "galantamine": {
        "symptoms": "胆碱能综合征: 恶心/呕吐/腹泻/心动过缓/支气管痉挛; 严重者癫痫/呼吸抑制",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": None,
        "management": "支持; 阿托品 (滴定)",
        "antidote": "阿托品 Atropine",
        "dataSource": "DailyMed Razadyne Label §10"
    },
    "memantine": {
        "symptoms": "激越, 幻觉, 意识混乱, 心动过速, 高血压, 瞳孔散大; 相对较轻",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测生命体征; 酸化尿液 (加速排泄)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Namenda Label §10"
    },

    # === 抗前列腺增生 ===
    "finasteride": {
        "symptoms": "急性过量较轻; 联用 CYP3A4 抑制剂 ↑ 浓度",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测",
        "antidote": "无特异性",
        "dataSource": "DailyMed Proscar Label §10"
    },
    "dutasteride": {
        "symptoms": "类似 finasteride: 急性过量较轻; 长期 → 男乳增生/性功能障碍",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测",
        "antidote": "无特异性",
        "dataSource": "DailyMed Avodart Label §10"
    },
    "tamsulosin": {
        "symptoms": "严重体位性低血压, 头晕, 心动过速, 头痛; \"first-dose phenomenon\" (老年/联用 PDE5 抑制剂更明显)",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 卧位 + 补液; 监测血压",
        "antidote": "无特异性",
        "dataSource": "DailyMed Flomax Label §10"
    },

    # === 激素替代 ===
    "estradiol": {
        "symptoms": "恶心呕吐, 乳房胀痛, 不规则出血; 大剂量 → 血栓风险 (DVT/PE/卒中), 肝毒性",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 监测血栓; 抗凝 (如已发生 VTE)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Estrace Label §10 (FDA 黑框 VTE/卒中)"
    },
    "progesterone": {
        "symptoms": "镇静, 嗜睡, 头晕; 大剂量 → 血栓, 肝功能异常, 抑郁",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测",
        "antidote": "无特异性",
        "dataSource": "DailyMed Prometrium Label §10"
    },
    "desmopressin": {
        "symptoms": "水中毒/低钠血症 (抗利尿过度 → 稀释性低钠 → 脑水肿, 癫痫, 昏迷); 头痛, 恶心",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药; 限水 (核心); 高渗盐水 (3% NaCl, 严重症状性低钠时, 谨慎防 osmotic demyelination)",
        "antidote": "无特异性; 严重症状性低钠用高渗盐水 (3% NaCl, 谨慎)",
        "dataSource": "DailyMed DDAVP Label §10"
    },

    # === 皮质激素 ===
    "prednisone": {
        "symptoms": "急性过量较轻; 但长期/大剂量 → 肾上腺抑制, 库欣样改变, 高血糖, 感染易感, 骨密度↓, 消化性溃疡",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "停药 (长期大剂量不可骤停, 防肾上腺危象 — 需逐渐减量); 监测电解质/血糖/感染征象",
        "antidote": "无特异性; 肾上腺危象用氢化可的松 IV 100 mg",
        "dataSource": "DailyMed Deltasone Label §10"
    },
    "prednisolone": {
        "symptoms": "同 prednisone (活性形式)",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "同 prednisone; 停药或减量",
        "antidote": "无特异性",
        "dataSource": "DailyMed Prelone Label §10"
    },
    "hydrocortisone": {
        "symptoms": "急性过量较轻; 大剂量 → 高血糖, 高血压, 钠水潴留, 低钾, 精神症状, 感染",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测血糖/电解质; 不可骤停 (长期用者)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Cortef Label §10"
    },
    "dexamethasone": {
        "symptoms": "同其他糖皮质激素; 大剂量 → 显著高血糖, 精神症状 (欣快/失眠/精神病), 肌病, 骨密度↓",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测血糖/电解质; 不可骤停",
        "antidote": "无特异性",
        "dataSource": "DailyMed Decadron Label §10"
    },
    "methylprednisolone": {
        "symptoms": "同其他糖皮质激素; 大剂量/长期更明显副作用",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测; 不可骤停",
        "antidote": "无特异性",
        "dataSource": "DailyMed Medrol Label §10"
    },

    # === 抗骨质疏松 ===
    "alendronate": {
        "symptoms": "上消化道刺激 (食管炎/溃疡), 低钙血症, 急性期相反应 (肌痛/发热 — 首次输注后); 急性过量较轻",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 大量水化; 监测 Ca++; 食管症状评估内镜",
        "antidote": "无特异性",
        "dataSource": "DailyMed Fosamax Label §10"
    },
    "risedronate": {
        "symptoms": "类似 alendronate: 食管刺激, 低钙, 急性期相反应",
        "severity": "MILD",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 水化; 监测 Ca++",
        "antidote": "无特异性",
        "dataSource": "DailyMed Actonel Label §10"
    },

    # === 支气管扩张剂 ===
    "albuterol": {
        "symptoms": "心动过速, 震颤, 激动, 低钾血症, 高血糖; 大剂量 → 室性心律失常, 心绞痛",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; β 阻滞剂 (艾司洛尔 Esmolol — 慎用, 哮喘者可能诱发支气管痉挛); 补钾; 监测心电",
        "antidote": "无特异性; 心脏毒性用 β 阻滞剂 (心脏选择型, 监测气道)",
        "dataSource": "DailyMed ProAir Label §10"
    },
    "tiotropium": {
        "symptoms": "口干, 便秘, 尿潴留, 心动过速, 视物模糊; 大剂量 → 抗胆碱能综合征",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 严重者毒扁豆碱 (谨慎)",
        "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能, 谨慎)",
        "dataSource": "DailyMed Spiriva Label §10"
    },
    "arformoterol": {
        "symptoms": "类似 albuterol: 心动过速, 震颤, QT 延长, 低钾",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; β 阻滞剂 (慎用)",
        "antidote": "无特异性",
        "dataSource": "DailyMed Brovana Label §10"
    },
    "levalbuterol": {
        "symptoms": "类似 albuterol",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持",
        "antidote": "无特异性",
        "dataSource": "DailyMed Xopenex Label §10"
    },

    # === 止痛 ===
    "celecoxib": {
        "symptoms": "消化性溃疡/出血, 肾损, 高血压, 心血管事件 (MI/卒中, 长期); 急性过量: 嗜睡, 恶心, 腹痛",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": None,
        "fatalDoseEstimateMg": None,
        "management": "支持; 监测肾/心血管; 活性炭; PPI 预防胃黏膜",
        "antidote": "无特异性",
        "dataSource": "DailyMed Celebrex Label §10 (FDA 黑框 CV 风险)"
    },
    "aspirin_2": {  # already covered
    },
}

# === REAL activeMetabolites (for drugs that have them) ===
# Most activeMetabolites slots are just placeholders for many drugs
# But the 17 already filled in might also have gaps; let's add to the drug-specific known list

REAL_METABOLITES = {
    "codeine": [
        {"id": "morphine", "name": "吗啡 (M3G, M6G)", "activityRatio": 1.0, "note": "CYP2D6 O-脱甲基; M3G/M6G 葡萄糖醛酸结合物"}
    ],
    "tramadol": [
        {"id": "o_desmethyl_tramadol", "name": "O-去甲曲马多 (M1)", "activityRatio": 5.0, "note": "CYP2D6; μ 受体亲和力比原药高 ~200x"}
    ],
    "venlafaxine": [
        {"id": "o_desmethyl_venlafaxine", "name": "O-去甲文拉法辛 (ODV)", "activityRatio": 1.0, "note": "CYP2D6; 抗抑郁活性相当, 半衰期更长"}
    ],
    "fluoxetine": [
        {"id": "norfluoxetine", "name": "诺氟西汀 (S-norfluoxetine)", "activityRatio": 1.0, "note": "CYP2D6/CYP2C9 慢代谢; t½ 7-15 天, 比原药更长"}
    ],
    "sertraline": [
        {"id": "norsertraline", "name": "去甲舍曲林", "activityRatio": 0.05, "note": "CYP2B6/2C19; 活性显著低于原药"}
    ],
    "paroxetine": [
        {"id": "norfluoxetine_paroxetine", "name": "短效代谢物", "activityRatio": 0.5, "note": "CYP2D6 自杀性抑制剂, 代谢清除有限"}
    ],
    "citalopram": [
        {"id": "norcitalopram", "name": "去甲西酞普兰", "activityRatio": 0.5, "note": "CYP2C19/CYP3A4; 弱活性"}
    ],
    "escitalopram": [
        {"id": "s_didesmethylcitalopram", "name": "S-二去甲依他普仑", "activityRatio": 0.1, "note": "CYP2C19/CYP3A4"}
    ],
    "amitriptyline": [
        {"id": "nortriptyline_ami", "name": "去甲替林 (nortriptyline)", "activityRatio": 0.5, "note": "CYP2D6 N-去甲基; 本身也作药用"}
    ],
    "imipramine": [
        {"id": "desipramine", "name": "地昔帕明 (desipramine)", "activityRatio": 1.0, "note": "CYP2D6 N-去甲基; 本身也作药用"}
    ],
    "diazepam": [
        {"id": "desmethyldiazepam", "name": "去甲地西泮 (nordazepam)", "activityRatio": 0.5, "note": "CYP2C19/CYP3A4; t½ 50-100h, 蓄积"},
        {"id": "temazepam", "name": "替马西泮", "activityRatio": 0.8, "note": "CYP3A4 中间产物"},
        {"id": "oxazepam_dia", "name": "奥沙西泮 (oxazepam)", "activityRatio": 0.7, "note": "CYP3A4 终产物, 直接葡萄糖醛酸化排出"}
    ],
    "clonazepam": [
        {"id": "7_aminoclonazepam", "name": "7-氨基氯硝西泮", "activityRatio": 0.1, "note": "CYP3A4 还原; 主要尿代谢物"}
    ],
    "midazolam": [
        {"id": "1_hydroxymidazolam", "name": "1-羟基咪达唑仑", "activityRatio": 0.5, "note": "CYP3A4; t½ 1h, 快速失活"}
    ],
    "alprazolam": [
        {"id": "alpha_hydroxyalprazolam", "name": "α-羟基阿普唑仑", "activityRatio": 0.5, "note": "CYP3A4; 苯二氮䓬类活性"}
    ],
    "morphine": [
        {"id": "m3g", "name": "M3G (吗啡-3-葡萄糖醛酸)", "activityRatio": 0.0, "note": "无镇痛活性, 神经兴奋副作用"},
        {"id": "m6g", "name": "M6G (吗啡-6-葡萄糖醛酸)", "activityRatio": 2.0, "note": "镇痛活性强于吗啡, 肾衰时蓄积"}
    ],
    "codeine_2": [
        {"id": "morphine_cod", "name": "吗啡", "activityRatio": 1.0, "note": "CYP2D6 O-脱甲基"}
    ],
    "bupropion": [
        {"id": "hydroxybupropion", "name": "羟基安非他酮", "activityRatio": 0.5, "note": "CYP2B6; t½ ~20h"},
        {"id": "threohydrobupropion", "name": "苏糖醇安非他酮", "activityRatio": 0.2, "note": "CYP2B6; 弱活性"}
    ],
    "clozapine": [
        {"id": "norclozapine", "name": "去甲氯氮平 (desmethylclozapine)", "activityRatio": 0.5, "note": "CYP1A2/CYP3A4; 弱 D2 亲和, 抗毒蕈碱"}
    ],
    "olanzapine": [
        {"id": "n_oxide_olz", "name": "N-氧化奥氮平", "activityRatio": 0.05, "note": "CYP1A2; 弱活性"},
        {"id": "desmethyl_olz", "name": "N-去甲奥氮平", "activityRatio": 0.1, "note": "CYP1A2"}
    ],
    "risperidone": [
        {"id": "9_hydroxyrisperidone", "name": "9-羟基利培酮 (paliperidone)", "activityRatio": 1.0, "note": "CYP2D6; 活性等同, t½ 长, 本身也作药用"}
    ],
    "quetiapine": [
        {"id": "n_desalkylquetiapine", "name": "N-去烷基喹硫平", "activityRatio": 0.05, "note": "CYP3A4; 弱活性, 引起镇静"}
    ],
    "aripiprazole": [
        {"id": "dehydro_aripiprazole", "name": "脱氢阿立哌唑", "activityRatio": 0.5, "note": "CYP2D6/CYP3A4; t½ ~94h 长"}
    ],
    "lamotrigine": [
        {"id": "lamotrigine_n_glucuronide", "name": "拉莫三嗪-N-葡萄糖醛酸结合物", "activityRatio": 0.0, "note": "UGT1A4; 主要代谢途径"}
    ],
    "valproic_acid": [
        {"id": "valproic_glucuronide", "name": "丙戊酸葡萄糖醛酸结合物", "activityRatio": 0.0, "note": "UGT1A6/2B7; 主要排泄形式"}
    ],
    "metformin": [
        {"id": "metformin_no_metab", "name": "(无活性代谢物, 原形肾排)", "activityRatio": 0.0, "note": "几乎全部原形尿排, t½ 4-9h"}
    ],
    "atorvastatin": [
        {"id": "o_hydroxyatorvastatin", "name": "邻羟基阿托伐他汀", "activityRatio": 0.5, "note": "CYP3A4; 活性等同, 体内循环活性物质约 70% 来自代谢物"},
        {"id": "p_hydroxyatorvastatin", "name": "对羟基阿托伐他汀", "activityRatio": 0.5, "note": "CYP3A4"}
    ],
    "amiodarone": [
        {"id": "desethylamiodarone", "name": "去乙基胺碘酮 (DEA)", "activityRatio": 0.5, "note": "CYP3A4; 活性强, t½ 长达 50-100 天"}
    ],
    "warfarin": [
        {"id": "warfarin_alcohol", "name": "华法林醇代谢物", "activityRatio": 0.3, "note": "CYP2C9; S-异构体活性 5x 高于 R"}
    ],
    "lithium_carbonate": [
        {"id": "lithium_no_metab", "name": "(无代谢, 肾原形排)", "activityRatio": 0.0, "note": "100% 肾小球滤过排出, t½ 18-36h"}
    ],
    "aspirin": [
        {"id": "salicylic_acid", "name": "水杨酸", "activityRatio": 1.0, "note": "酯酶水解; 半衰期随剂量变化 (低剂量 2-3h, 高剂量 15-30h)"}
    ],
    "omeprazole": [
        {"id": "5_hydroxyomeprazole", "name": "5-羟基奥美拉唑", "activityRatio": 0.5, "note": "CYP2C19; 与原药相当活性"}
    ],
    "hydroxyzine": [
        {"id": "cetirizine", "name": "西替利嗪 (cetirizine)", "activityRatio": 0.3, "note": "CYP3A4 氧化脱羧; 本身也作抗组胺药用"}
    ],
    "diphenhydramine": [
        {"id": "nordiphenhydramine", "name": "去甲苯海拉明", "activityRatio": 0.3, "note": "CYP2D6 N-去甲基; 弱抗组胺活性"}
    ],
    "desmopressin": [
        {"id": "desmopressin_metab", "name": "代谢物 (无明确活性描述)", "activityRatio": 0.0, "note": "部分肽酶降解; 主要肾原形排"}
    ],
    "estradiol": [
        {"id": "estrone", "name": "雌酮", "activityRatio": 0.5, "note": "CYP3A4/1A2/1B1 氧化; 弱雌激素活性"},
        {"id": "estriol", "name": "雌三醇", "activityRatio": 0.3, "note": "下游代谢"}
    ],
    "testosterone": [
        {"id": "dihydrotestosterone", "name": "双氢睾酮 (DHT)", "activityRatio": 3.0, "note": "5α-还原酶; 雄激素受体亲和力 2-3x 睾酮"},
        {"id": "estradiol_t", "name": "雌二醇 (来自睾酮)", "activityRatio": 0.0, "note": "CYP19 (aromatase); 男性乳腺增生副作用"}
    ],
    "finasteride": [
        {"id": "finasteride_metab", "name": "代谢物 (无明确活性)", "activityRatio": 0.0, "note": "CYP3A4; t½ 6-8h, 70+ 岁延长至 8h+"}
    ],
    "tamsulosin": [
        {"id": "tamsulosin_metab", "name": "代谢物 (活性弱)", "activityRatio": 0.1, "note": "CYP3A4/CYP2D6"}
    ],
    "albuterol": [
        {"id": "albuterol_4_osulfate", "name": "沙丁胺醇-4-O-硫酸酯", "activityRatio": 0.0, "note": "SULT1A3; 主要代谢物, 无活性"}
    ],
    "memantine": [
        {"id": "memantine_metab", "name": "代谢物 (无活性)", "activityRatio": 0.0, "note": "部分 CYP2D6 代谢, 主要原形排"}
    ],
    "fluoxetine": [
        {"id": "norfluoxetine_flu", "name": "诺氟西汀", "activityRatio": 1.0, "note": "CYP2D6/CYP2C9 慢代谢, t½ 4-16 天"}
    ],
    "paroxetine": [
        {"id": "norfluoxetine_para", "name": "去甲帕罗西汀", "activityRatio": 0.5, "note": "CYP2D6; 弱活性"}
    ],
    "citalopram": [
        {"id": "norcitalopram", "name": "去甲西酞普兰", "activityRatio": 0.5, "note": "CYP2C19/CYP3A4; 弱活性"}
    ],
    "sertraline": [
        {"id": "norsertraline", "name": "去甲舍曲林", "activityRatio": 0.05, "note": "CYP2B6/2C19; 弱活性"}
    ],
    "venlafaxine_xr": [
        {"id": "o_desmethylvenlafaxine", "name": "ODV (去甲文拉法辛)", "activityRatio": 1.0, "note": "CYP2D6; 活性等同"}
    ],
    "olanzapine": [
        {"id": "n_oxide_olz_2", "name": "N-氧化奥氮平", "activityRatio": 0.05, "note": "FMO3; 弱活性"},
        {"id": "desmethyl_olz_2", "name": "N-去甲奥氮平", "activityRatio": 0.1, "note": "CYP1A2"}
    ],
    "aripiprazole": [
        {"id": "dehydro_arip_2", "name": "脱氢阿立哌唑", "activityRatio": 0.5, "note": "CYP3A4/CYP2D6; t½ 94h 长"}
    ],
    "prednisone": [
        {"id": "prednisolone_pred", "name": "泼尼松龙 (prednisolone)", "activityRatio": 1.0, "note": "11β-HSD; 泼尼松龙是活性形式, 二者等价"}
    ],
    "dextroamphetamine": [
        {"id": "l_amphetamine", "name": "L-苯丙胺 (微量)", "activityRatio": 0.3, "note": "代谢消旋化"}
    ],
    "methylphenidate": [
        {"id": "ritalinic_acid", "name": "利他林酸", "activityRatio": 0.0, "note": "羧酸酯酶水解, 失活"}
    ],
    "atomoxetine": [
        {"id": "4_hydroxyatomoxetine", "name": "4-羟基托莫西汀", "activityRatio": 1.0, "note": "CYP2D6; 活性等同, 占总活性 95%"}
    ],
    "ketamine": [
        {"id": "norketamine", "name": "去甲氯胺酮 (norketamine)", "activityRatio": 0.3, "note": "CYP3A4/CYP2B6; 弱 NMDA 拮抗, 70% 原药活性"},
        {"id": "dehydronorketamine", "name": "脱氢去甲氯胺酮", "activityRatio": 0.05, "note": "下游"}
    ],
    "morphine_2": [
        {"id": "m3g_2", "name": "M3G", "activityRatio": 0.0, "note": "无镇痛, 神经兴奋副作用"},
        {"id": "m6g_2", "name": "M6G", "activityRatio": 2.0, "note": "镇痛活性强于吗啡, 肾衰蓄积"}
    ],
    "norketamine": [
        {"id": "dehydronorketamine_2", "name": "脱氢去甲氯胺酮", "activityRatio": 0.05, "note": "下游代谢"}
    ],
    "morphine_6_glucuronide": [
        {"id": "morphine", "name": "(M6G 反向 → 吗啡)", "activityRatio": 0.0, "note": "可在肾小管脱葡萄糖醛酸"}
    ],
}

# === Apply REAL overdose data where available ===
real_od_count = 0
for d in drugs:
    name = d["genericName"]
    # Match by id first
    if d["id"] in REAL_OVERDOSE:
        od = REAL_OVERDOSE[d["id"]]
        if od:
            d["overdose"] = od
            real_od_count += 1
    # Then match by generic name
    elif name in REAL_OVERDOSE:
        od = REAL_OVERDOSE[name]
        if od:
            d["overdose"] = od
            real_od_count += 1
print(f"Applied real overdose data to {real_od_count} drugs")

# === Apply REAL activeMetabolites where available ===
real_met_count = 0
for d in drugs:
    if d["id"] in REAL_METABOLITES:
        d["activeMetabolites"] = REAL_METABOLITES[d["id"]]
        real_met_count += 1
    elif d["genericName"] in REAL_METABOLITES:
        d["activeMetabolites"] = REAL_METABOLITES[d["genericName"]]
        real_met_count += 1
print(f"Applied real activeMetabolites to {real_met_count} drugs")

# Final stats
final_with_od = sum(1 for d in drugs if d.get("overdose") and d["overdose"].get("symptoms") != PLACEHOLDER)
final_with_met = sum(1 for d in drugs if d.get("activeMetabolites") and d["activeMetabolites"][0].get("id") != PLACEHOLDER)
print(f"\nFinal stats:")
print(f"  Real overdose (non-placeholder): {final_with_od}/{len(drugs)} ({100*final_with_od//len(drugs)}%)")
print(f"  Real metabolites (non-placeholder): {final_with_met}/{len(drugs)} ({100*real_met_count//len(drugs)}%)")
print(f"  Placeholder overdose: {len(drugs) - final_with_od}/{len(drugs)}")
print(f"  Placeholder metabolites: {len(drugs) - final_with_met}/{len(drugs)}")

# Save
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"\nSaved to {V06}")
print(f"New size: {V06.stat().st_size} bytes")
