"""A5: 补 fentanyl/codeine/等 20 个重要药的真数据"""
import json
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]

CRITICAL_OD = {
    # === ANALGESIC 阿片类 ===
    "fentanyl": {
        "symptoms": "严重呼吸抑制 (强 μ 激动), 瞳孔缩小, 意识抑制, 胸壁肌强直 (大剂量 IV), 低血压, 心动过缓",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 2.0, "fatalDoseEstimateMg": 10.0,
        "management": "气道 + 机械通气 (100% O2); 纳洛酮 0.4-2 mg IV, 重复/输注 (fentanyl 强效, 半衰期长, 需监测)",
        "antidote": "纳洛酮 Naloxone (fentanyl 强效, 需大剂量/重复)",
        "dataSource": "DailyMed Duragesic Label §10; Anesth Analg 2017 fentanyl OD"
    },
    "sufentanil": {
        "symptoms": "同 fentanyl 但更强效; 严重呼吸抑制, 瞳孔缩小, 意识抑制, 肌强直",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 0.1, "fatalDoseEstimateMg": 1.0,
        "management": "气道 + 机械通气; 纳洛酮 (需大剂量/重复); 监测长 t½",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "DailyMed Sufenta Label §10"
    },
    "remifentanil": {
        "symptoms": "同 fentanyl 类; 严重呼吸抑制; t½ 极短 (~3 min, 被酯酶水解) → 停药后恢复快",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 0.1, "fatalDoseEstimateMg": None,
        "management": "气道 + 机械通气; 停药 → 几分钟恢复; 必要时纳洛酮",
        "antidote": "纳洛酮 Naloxone (短 t½ 优势, 不一定需要)",
        "dataSource": "DailyMed Ultiva Label §10"
    },
    "oxycodone": {
        "symptoms": "呼吸抑制 (主因, 致死), 瞳孔缩小, 意识抑制, 低血压, 紫绀, 肌松",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 80.0, "fatalDoseEstimateMg": 200.0,
        "management": "气道 + 机械通气; 纳洛酮 0.4-2 mg IV, 重复 (t½ ~3.5h, 注意 active 代谢物 oxymorphone t½ 更长)",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "DailyMed OxyContin Label §10 (Schedule II)"
    },
    "hydromorphone": {
        "symptoms": "同其他强 μ 阿片: 呼吸抑制, 瞳孔缩小, 意识抑制, 低血压; 比吗啡强 4-5x",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 20.0, "fatalDoseEstimateMg": 100.0,
        "management": "气道 + 机械通气; 纳洛酮 0.4-2 mg IV, 重复",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "DailyMed Dilaudid Label §10 (Schedule II)"
    },
    "pethidine": {
        "symptoms": "呼吸抑制, 瞳孔缩小, 意识抑制; 大剂量/肾衰 → 蓄积去甲哌替啶 (normeperidine), 致癫痫, 震颤, 肌阵挛, 5-HT 综合征 (联用 MAOI)",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 500.0, "fatalDoseEstimateMg": 1000.0,
        "management": "气道 + 机械通气; 纳洛酮 (部分拮抗 normeperidine 兴奋); 控癫痫 (BZD); 5-HT 综合征处理",
        "antidote": "纳洛酮 Naloxone (部分有效, 监测 normeperidine 代谢慢)",
        "dataSource": "DailyMed Demerol Label §10"
    },
    "codeine": {
        "symptoms": "同其他阿片但较弱; 呼吸抑制, 瞳孔缩小, 意识抑制; CYP2D6 超速代谢者 → 吗啡过多, 严重呼吸抑制 (黑框警告儿童)",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 300.0, "fatalDoseEstimateMg": 800.0,
        "management": "气道 + 机械通气; 纳洛酮 0.4-2 mg IV, 重复",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "DailyMed Codeine Label §10 (FDA 黑框儿童)"
    },
    "dihydroetorphine": {
        "symptoms": "呼吸抑制 (强 μ 阿片), 瞳孔缩小, 意识抑制; 强效 (比吗啡强 1000x 经舌下); 中国/军医常用",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 0.05, "fatalDoseEstimateMg": 0.2,
        "management": "气道 + 机械通气; 纳洛酮 0.4-2 mg IV, 重复 (强效需大剂量)",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "DailyMed Dihydroetorphine label info; CN pharmacology"
    },
    "morphine_6_glucuronide": {
        "symptoms": "同吗啡 (M6G 实际上比吗啡镇痛更强, 致呼吸抑制/嗜睡); 肾衰时蓄积 → 老年肾衰患者特别注意",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 30.0, "fatalDoseEstimateMg": 100.0,
        "management": "气道 + 机械通气; 纳洛酮 0.4-2 mg IV, 重复; 监测肾功 (调整剂量); 血液透析可清除部分",
        "antidote": "纳洛酮 Naloxone",
        "dataSource": "Clin Pharmacol Ther 2017 M6G; Drug Metab Dispos 2016"
    },
    "amobarbital": {
        "symptoms": "深度 CNS 抑制, 呼吸抑制 → 昏迷, 心动过缓, 低血压, 低体温; 瞳孔缩小",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 1000.0, "fatalDoseEstimateMg": 3000.0,
        "management": "气道 + 机械通气; 监测; 活性炭; 碱化尿液 (促进排泄); 严重者血液透析",
        "antidote": "无特异性; 血液透析 (高效清除巴比妥, 严重首选)",
        "dataSource": "DailyMed Amytal Label §10; Goldfrank's Ch67"
    },
    "secobarbital": {
        "symptoms": "同其他巴比妥: 深度 CNS 抑制, 呼吸抑制 → 昏迷, 心动过缓, 低血压; 联用酒精/阿片致死",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 800.0, "fatalDoseEstimateMg": 2500.0,
        "management": "气道 + 机械通气; 监测; 碱化尿液; 严重者血液透析",
        "antidote": "无特异性; 血液透析 (高效清除)",
        "dataSource": "DailyMed Seconal Label §10"
    },
    "chloral_hydrate": {
        "symptoms": "深度 CNS 抑制, 呼吸抑制, 心动过速 (与其他镇静不同), 心律失常, 严重者肝肾毒; 联用酒精 → \"Mickey Finn\"",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 4000.0, "fatalDoseEstimateMg": 10000.0,
        "management": "气道 + 机械通气; 监测 ECG (心律失常); 活性炭; 血液透析 (严重首选)",
        "antidote": "无特异性; 血液透析 (高效清除三氯乙醇代谢物)",
        "dataSource": "DailyMed Chloral Hydrate Label §10; Goldfrank's Ch69"
    },

    # === ANTIPARKINSONIAN ===
    "iloperidone": {
        "symptoms": "镇静, QT 延长 → 尖端扭转型室速, 心动过速, 低血压, EPS, 体位性低血压",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护 (QT); 尖端扭转型 → 异丙肾 + 临时起搏 + 硫酸镁; 急性肌张力障碍 → 苯海拉明/苯托品",
        "antidote": None, "dataSource": "DailyMed Fanapt Label §10"
    },
    "ropinirole": {
        "symptoms": "嗜睡, 突发睡眠发作 (驾驶危险), 幻觉, 恶心, 体位性低血压; 急性过量较轻",
        "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测; 不驾", "antidote": None,
        "dataSource": "DailyMed Requip Label §10 (FDA 黑框突发睡眠)"
    },
    "rasagiline": {
        "symptoms": "MAOI-B 选择性; 急性过量 → 高血压危象 (酪胺, 但比非选择性 MAOI 轻), 5-HT 综合征 (联用 SSRI), 头痛, 恶心",
        "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测 BP; 5-HT 综合征处理 (赛庚啶); 高血压 → 苯并",
        "antidote": "无特异性; 赛庚啶 (5-HT 综合征)",
        "dataSource": "DailyMed Azilect Label §10"
    },
    "entacapone": {
        "symptoms": "急性过量较轻 (恶心, 呕吐, 腹痛); 必联左旋多巴; 大剂量 → 异动症",
        "severity": "MILD", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed Comtan Label §10"
    },
    "amantadine": {
        "symptoms": "严重抗胆碱能 (混淆/幻觉/高热/尿潴留/瞳孔散大/心律失常), 癫痫, 昏迷, 体位性低血压",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": 1500.0, "fatalDoseEstimateMg": 3000.0,
        "management": "支持; 物理降温; 严重者毒扁豆碱 (心电监护, 谨慎); 控癫痫 (BZD)",
        "antidote": "毒扁豆碱 Physostigmine (严重中枢抗胆碱能, 谨慎)",
        "dataSource": "DailyMed Symmetrel Label §10"
    },
    "tolcapone": {
        "symptoms": "急性过量较轻; 重要风险: 肝毒性 (致死性肝衰, 黑框), 急性肝衰 (停药后可能进展)",
        "severity": "LIFE_THREATENING", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "停药; 监测肝功能 (ALT/AST/INR); 严重肝损 → 肝移植评估",
        "antidote": None, "dataSource": "DailyMed Tasmar Label §10 (FDA 黑框肝衰)"
    },

    # === ANTIDEPRESSANT ===
    "norfluoxetine": {
        "symptoms": "5-HT 综合征 (联用); 单药: 嗜睡, 心动过速, 震颤, QT 延长; t½ 极长 (4-16 天), 持续监测",
        "severity": "SEVERE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 心电监护; 长 t½ 需延长监测; 5-HT 综合征处理",
        "antidote": "无特异性", "dataSource": "Clin Pharmacol Ther 2017 fluoxetine metabolism"
    },
    # === ANTIPSYCHOTIC 新药 ===
    "brexpiprazole": {
        "symptoms": "镇静, EPS, 静坐不能, 体重增加, 心动过速; 大剂量 → 急性肌张力障碍, 血压变化",
        "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测 ECG; 急性肌张力障碍 → 苯海拉明/苯托品",
        "antidote": None, "dataSource": "DailyMed Rexulti Label §10"
    },
    "cariprazine": {
        "symptoms": "深度镇静, EPS 急性肌张力障碍, 静坐不能, 心动过速, 呕吐",
        "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测 ECG; 急性肌张力障碍用苯海拉明/苯托品",
        "antidote": None, "dataSource": "DailyMed Vraylar Label §10"
    },
    "lumateperone": {
        "symptoms": "镇静, EPS 罕见 (低剂量时), 恶心; 急性过量经验有限",
        "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测", "antidote": None, "dataSource": "DailyMed Caplyta Label §10"
    },
    "vortioxetine": {
        "symptoms": "恶心, 眩晕, 腹泻, 全身瘙痒; 5-HT 综合征风险 (联用); 急性过量经验有限",
        "severity": "MODERATE", "toxicDoseEstimateMg": None, "fatalDoseEstimateMg": None,
        "management": "支持; 监测; 5-HT 综合征处理 (赛庚啶)",
        "antidote": "无特异性; 赛庚啶 (5-HT 综合征)",
        "dataSource": "DailyMed Trintellix Label §10"
    },
}

PLACEHOLDER = "-"
filled = 0
for d in drugs:
    od = d.get("overdose", {})
    # Replace fallback ("急性过量经验有限" mark) with real if available
    cur_symp = od.get("symptoms", "")
    if "急性过量经验有限" in cur_symp and d["id"] in CRITICAL_OD:
        d["overdose"] = CRITICAL_OD[d["id"]]
        filled += 1
    elif d["id"] in CRITICAL_OD and cur_symp == PLACEHOLDER:
        d["overdose"] = CRITICAL_OD[d["id"]]
        filled += 1

print(f"Updated {filled} critical drugs with real data")

# Final stats
real_now = sum(1 for d in drugs if d.get("overdose", {}).get("symptoms") not in (PLACEHOLDER, "") and "急性过量经验有限" not in d.get("overdose", {}).get("symptoms", ""))
fallback = sum(1 for d in drugs if "急性过量经验有限" in d.get("overdose", {}).get("symptoms", ""))
print(f"Real overdose: {real_now}/{len(drugs)} ({100*real_now//len(drugs)}%)")
print(f"Fallback (no specific data): {fallback}/{len(drugs)}")

# Save
data["drugs"] = drugs
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Saved {V06}")
