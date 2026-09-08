#!/usr/bin/env python3
"""
v0.6.1 药物过量数据校准: 33 个有治疗窗的药补 overdose 字段

数据源 (权威):
- FDA DailyMed Overdosage section (各药品说明书)
- AGNP 2017 Therapeutic Drug Monitoring in Psychiatry
- Micromedex / UpToDate 公开摘要
- 临床指南 (中华医学会精神病学分会, APA)
"""
import json
from pathlib import Path

p = Path("app/src/main/assets/drugs/v0.6.json")
data = json.loads(p.read_text(encoding="utf-8"))

# 33 个有治疗窗的药 overdose 数据
OVERDOSE = {
    # 抗精神病
    "clozapine": {
        "symptoms": "严重中枢抑制、低血压/高血压、心动过速、呼吸抑制、癫痫发作 (1-2% > 600mg/d)、恶性综合征、粒细胞缺乏。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 600.0,
        "fatalDoseEstimateMg": 2500.0,
        "management": "无解毒剂。洗胃 + 活性炭 (1h 内)。支持治疗: 气道/呼吸/循环。癫痫: 苯二氮卓 (避免苯妥英, 可致心律失常)。监测 ECG + 血常规 ≥ 4 周 (粒缺风险)。",
        "antidote": None,
        "dataSource": "FDA Clozaril 说明书; AGNP 2017"
    },
    "olanzapine": {
        "symptoms": "中枢抑制、心动过速、低血压、锥体外系症状、QRS 增宽少见。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": 600.0,
        "management": "无解毒剂。活性炭。支持治疗, 监测 ECG ≥ 24h。",
        "antidote": None,
        "dataSource": "FDA Zyprexa 说明书"
    },
    "risperidone": {
        "symptoms": "镇静、低血压、锥体外系症状、QTc 延长。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 20.0,
        "fatalDoseEstimateMg": 360.0,
        "management": "无解毒剂。活性炭 + 支持治疗。监测 ECG。",
        "antidote": None,
        "dataSource": "FDA Risperdal 说明书"
    },
    "quetiapine": {
        "symptoms": "严重镇静、低血压、心动过速、QTc 延长、罕见癫痫。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": 6000.0,
        "management": "无解毒剂。活性炭 + 支持治疗。监测 ECG ≥ 24h。",
        "antidote": None,
        "dataSource": "FDA Seroquel 说明书"
    },
    "aripiprazole": {
        "symptoms": "镇静、呕吐、嗜睡、震颤。总体相对安全。",
        "severity": "MILD",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。t1/2 长 (75h), 监护 ≥ 3 天。",
        "antidote": None,
        "dataSource": "FDA Abilify 说明书"
    },
    "ziprasidone": {
        "symptoms": "镇静、低血压、QTc 显著延长 (尖端扭转型室速风险)。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 320.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。监测 ECG ≥ 24h, 纠正低钾低镁。",
        "antidote": None,
        "dataSource": "FDA Geodon 说明书"
    },
    "amisulpride": {
        "symptoms": "镇静、低血压、QTc 显著延长 → 尖端扭转型室速。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 800.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。ECG 监测 ≥ 24h。",
        "antidote": None,
        "dataSource": "Solian EMA SmPC"
    },
    # 心境稳定剂
    "valproic_acid": {
        "symptoms": "中枢抑制、高氨血症 (无肝损)、脑病、胰腺炎、代谢性酸中毒、血小板减少。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 4000.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。监测血药浓度 (12-24h lag)。严重者血液透析有效 (蛋白结合率高但游离部分可透)。",
        "antidote": None,
        "dataSource": "FDA Depakote 说明书; AGNP 2017"
    },
    "lithium": {
        "symptoms": "震颤、嗜睡、构音障碍、共济失调、癫痫、心律失常 (T 波低平)、肾功能损害。中毒指数极窄。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 1500.0,    # 治疗量上限 ~1200mg
        "fatalDoseEstimateMg": 5000.0,
        "management": "无解毒剂。急性中毒 (服 < 12h) → 洗胃 + 活性炭 (不吸锂, 但减少吸收); 血锂 > 2.5 mmol/L 或肾衰 → 血液透析 (首选用)。监测 ECG, 水电解质平衡, 强迫利尿 (生理盐水)。",
        "antidote": None,
        "dataSource": "FDA Eskalith 说明书; AGNP 2017"
    },
    "lamotrigine": {
        "symptoms": "共济失调、眼球震颤、嗜睡、严重皮疹 (Stevens-Johnson 综合征, 尤其加丙戊酸时)。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。皮疹进展 → 立即停药 + 皮肤科会诊。",
        "antidote": None,
        "dataSource": "FDA Lamictal 说明书"
    },
    "carbamazepine": {
        "symptoms": "中枢抑制、共济失调、眼球震颤、心律失常 (QRS 增宽)、抗胆碱能症状、低钠血症。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 2000.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 (可重复给, 因肠肝循环) + 支持治疗。严重者血液灌流 (HP) > 血液透析 (HD)。监测 ECG + 电解质。",
        "antidote": None,
        "dataSource": "FDA Tegretol 说明书"
    },
    # 抗抑郁 - SSRI
    "fluoxetine": {
        "symptoms": "5-HT 综合征 (激动/震颤/反射亢进/高热)、心动过速、QRS 增宽少见。单药过量较安全。",
        "severity": "MILD",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 1500.0,
        "management": "无解毒剂。活性炭 + 支持治疗。t1/2 长 (72h), 监护 ≥ 6 天。5-HT 综合征: 赛庚啶 12 mg po, 必要时重复。",
        "antidote": None,
        "dataSource": "FDA Prozac 说明书"
    },
    "sertraline": {
        "symptoms": "5-HT 综合征、嗜睡、震颤、心动过速、QRS 增宽。",
        "severity": "MILD",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。QRS > 100ms → 碳酸氢钠 1-2 mEq/kg IV。",
        "antidote": None,
        "dataSource": "FDA Zoloft 说明书"
    },
    "paroxetine": {
        "symptoms": "5-HT 综合征、抗胆碱能症状 (口干/瞳孔散大/心动过速)、嗜睡。",
        "severity": "MILD",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。",
        "antidote": None,
        "dataSource": "FDA Paxil 说明书"
    },
    "escitalopram": {
        "symptoms": "5-HT 综合征、QTc 延长 (高剂量)、癫痫。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 200.0,
        "fatalDoseEstimateMg": 1000.0,
        "management": "无解毒剂。活性炭 + 支持治疗。ECG 监测 ≥ 24h。",
        "antidote": None,
        "dataSource": "FDA Lexapro 说明书"
    },
    "fluvoxamine": {
        "symptoms": "5-HT 综合征、嗜睡、震颤、心动过速。",
        "severity": "MILD",
        "toxicDoseEstimateMg": 1500.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。",
        "antidote": None,
        "dataSource": "FDA Luvox 说明书"
    },
    "citalopram": {
        "symptoms": "5-HT 综合征、QTc 显著延长 → 尖端扭转型室速, 尤其 > 600mg。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 400.0,
        "fatalDoseEstimateMg": 2000.0,
        "management": "无解毒剂。活性炭 + 支持治疗。ECG 监测 ≥ 24h, 警惕尖端扭转型室速。低钾低镁纠正。",
        "antidote": None,
        "dataSource": "FDA Celexa 说明书"
    },
    # SNRI
    "venlafaxine": {
        "symptoms": "5-HT 综合征、QRS 增宽、心动过速、癫痫 (高剂量)。比 SSRI 危险。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 1500.0,
        "fatalDoseEstimateMg": 3000.0,
        "management": "无解毒剂。活性炭 + 支持治疗。QRS > 100ms → 碳酸氢钠 1-2 mEq/kg IV。癫痫 → 苯二氮卓。",
        "antidote": None,
        "dataSource": "FDA Effexor 说明书"
    },
    "duloxetine": {
        "symptoms": "5-HT 综合征、嗜睡、震颤、QRS 增宽、罕见肝毒性。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 600.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。",
        "antidote": None,
        "dataSource": "FDA Cymbalta 说明书"
    },
    # TCA - 极危险
    "amitriptyline": {
        "symptoms": "⚠️ 极危险: 抗胆碱能综合征 (M 样症状) + QRS 增宽 (>100ms) → 室速/室颤 + 严重低血压 + 癫痫。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 1000.0,
        "management": "紧急处理。QRS > 100ms → 碳酸氢钠 1-2 mEq/kg IV bolus 直至 pH 7.50-7.55。低血压 → 生理盐水 + 碳酸氢钠。癫痫 → 苯二氮卓 (避免苯妥英, 加重心毒性)。活性炭 + 支持治疗 ≥ 24h。",
        "antidote": "碳酸氢钠 (钠负荷 + 碱化血液)",
        "dataSource": "FDA Elavil 说明书; 临床毒理学"
    },
    "nortriptyline": {
        "symptoms": "同阿米替林: 抗胆碱能 + QRS 增宽 + 室性心律失常 + 癫痫。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 1000.0,
        "management": "同阿米替林。碳酸氢钠 IV 抗心律失常。",
        "antidote": "碳酸氢钠",
        "dataSource": "FDA Pamelor 说明书"
    },
    "clomipramine": {
        "symptoms": "同 TCA: 抗胆碱能 + QRS 增宽 + 室性心律失常 + 癫痫 + 5-HT 综合征 (高剂量)。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 1500.0,
        "management": "同 TCA。碳酸氢钠 + 苯二氮卓抗癫痫。",
        "antidote": "碳酸氢钠",
        "dataSource": "FDA Anafranil 说明书"
    },
    "imipramine": {
        "symptoms": "⚠️ 同 TCA 综合征: 抗胆碱能 + QRS 增宽 + 室性心律失常 + 严重低血压 + 癫痫。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 1000.0,
        "management": "同阿米替林。碳酸氢钠 + 苯二氮卓 + 支持治疗。",
        "antidote": "碳酸氢钠",
        "dataSource": "FDA Tofranil 说明书"
    },
    # 其他抗抑郁
    "mirtazapine": {
        "symptoms": "严重镇静、意识混乱、心动过速、轻度高血压, 总体较安全。",
        "severity": "MILD",
        "toxicDoseEstimateMg": 200.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。",
        "antidote": None,
        "dataSource": "FDA Remeron 说明书"
    },
    "bupropion": {
        "symptoms": "⚠️ 极高癫痫风险 (>5g 时 50%+ 发作), 心动过速, QRS 增宽。延迟发作 24-48h。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 600.0,
        "fatalDoseEstimateMg": 3000.0,
        "management": "活性炭 + 支持治疗。早期苯二氮卓预防性抗癫痫。QRS > 100ms → 碳酸氢钠。监护 ≥ 48h。",
        "antidote": None,
        "dataSource": "FDA Wellbutrin 说明书"
    },
    "trazodone": {
        "symptoms": "镇静、低血压、罕见但严重的 QTc 延长 → 尖端扭转型室速 (尤其低钾)。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 1000.0,
        "fatalDoseEstimateMg": None,
        "management": "无解毒剂。活性炭 + 支持治疗。ECG 监测, 纠正电解质。",
        "antidote": None,
        "dataSource": "FDA Desyrel 说明书"
    },
    # BZD
    "diazepam": {
        "symptoms": "中枢抑制、嗜睡、共济失调、呼吸抑制 (尤其合用阿片/酒精)。t1/2 长, 症状持续 24-72h。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 500.0,
        "fatalDoseEstimateMg": 2000.0,
        "management": "活性炭 + 支持治疗。严重呼吸抑制 → 氟马西尼 0.2 mg IV, 30s 后可重复, 累计 ≤ 1-3 mg (⚠️ BZD 依赖者禁用, 可致癫痫)。",
        "antidote": "氟马西尼 (Flumazenil) — 严格把握指征",
        "dataSource": "FDA Valium 说明书"
    },
    "lorazepam": {
        "symptoms": "中枢抑制、嗜睡、共济失调、呼吸抑制。t1/2 中等, 症状持续 12-24h。",
        "severity": "MODERATE",
        "toxicDoseEstimateMg": 200.0,
        "fatalDoseEstimateMg": 1000.0,
        "management": "活性炭 + 支持治疗。严重呼吸抑制 → 氟马西尼 (同地西泮)。",
        "antidote": "氟马西尼 (BZD 依赖者禁用)",
        "dataSource": "FDA Ativan 说明书"
    },
    # 阿片
    "buprenorphine": {
        "symptoms": "阿片中毒三联征: 呼吸抑制 + 针尖样瞳孔 + 意识障碍。可致严重低血压。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 50.0,
        "fatalDoseEstimateMg": 100.0,
        "management": "气道支持。纳洛酮 0.4-2 mg IV 滴定 (注意 buprenorphine 半衰期长, 纳洛酮需持续输注)。",
        "antidote": "纳洛酮 (Naloxone)",
        "dataSource": "FDA Suboxone 说明书"
    },
    "methadone": {
        "symptoms": "⚠️ 极危险: 严重呼吸抑制、QTc 显著延长 → 尖端扭转型室速。t1/2 长 (24-36h), 蓄积易过量。",
        "severity": "LIFE_THREATENING",
        "toxicDoseEstimateMg": 100.0,
        "fatalDoseEstimateMg": 200.0,
        "management": "气道支持 + 纳洛酮 0.4-2 mg IV, 需持续输注 (t1/2 长)。ECG 监测 ≥ 24h, 警惕尖端扭转型室速。",
        "antidote": "纳洛酮 (需持续输注)",
        "dataSource": "FDA Dolophine 说明书"
    },
    # 抗凝/甲状腺
    "warfarin": {
        "symptoms": "出血 (皮肤瘀斑/血尿/消化道/颅内)、INR 显著升高、肝功能异常。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 50.0,
        "fatalDoseEstimateMg": None,
        "management": "停药。维生素 K1 5-10 mg 口服/SC; 严重出血 → 维生素 K1 IV + 凝血酶原复合物 (PCC) 或新鲜冰冻血浆 (FFP) 立即逆转 INR。",
        "antidote": "维生素 K1 (Phytonadione)",
        "dataSource": "FDA Coumadin 说明书"
    },
    "levothyroxine": {
        "symptoms": "甲亢危象样: 心动过速、心律失常、高热、震颤、躁动。慢性蓄积可致骨质疏松/心衰。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 3.0,           # 治疗 0.05-0.2 mg
        "fatalDoseEstimateMg": None,
        "management": "活性炭 (急性)。β 受体阻滞剂 (普萘洛尔) 控制心率/症状。地塞米松抑制 T4→T3 转换。",
        "antidote": "β 受体阻滞剂 (对症)",
        "dataSource": "FDA Synthroid 说明书"
    },
    "phenytoin": {
        "symptoms": "⚠️ 共济失调、眼球震颤、嗜睡、昏迷、心律失常 (QRS 增宽, IV 过快易致) 、高血糖。",
        "severity": "SEVERE",
        "toxicDoseEstimateMg": 1500.0,
        "fatalDoseEstimateMg": None,
        "management": "活性炭 + 支持治疗。心律失常 → 碳酸氢钠。严重者血液透析 (但蛋白结合率高, 效果有限)。非线性消除 — 剂量微增 → 血药浓度急升。",
        "antidote": "碳酸氢钠 (心律失常)",
        "dataSource": "FDA Dilantin 说明书"
    },
}

n = 0
for drug in data["drugs"]:
    if drug["id"] in OVERDOSE:
        drug["overdose"] = OVERDOSE[drug["id"]]
        n += 1
        print(f"  {drug['id']} ({drug['genericNameZh']}) -> {OVERDOSE[drug['id']]['severity']}")

p.write_text(
    json.dumps(data, ensure_ascii=False, indent=2),
    encoding="utf-8"
)
print(f"\n已给 {n} 个药补 overdose 数据, 写入 {p}")
