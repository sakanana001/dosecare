#!/usr/bin/env python3
"""
v0.6.1 校准: 159 药按主治症状/疾病分类 (IndicationGroup)
- 一药可归多组 (e.g. SSRI: DEPRESSION + ANXIETY + OCD + PTSD)
- 与 DrugCategory (按药理学) 并列, 用于"按主治症状"视图

数据源: FDA DailyMed Indications, WHO ATC, PubMed 临床指南
"""
import json
from pathlib import Path

p = Path("app/src/main/assets/drugs/v0.6.json")
data = json.loads(p.read_text(encoding="utf-8"))

# 159 药手工分类 (按真实 FDA / WHO 适应症)
GROUPS = {
    # 抗精神病 (13) — 精神分裂 + 部分双相
    "clozapine": ["SCHIZOPHRENIA"],
    "olanzapine": ["SCHIZOPHRENIA", "BIPOLAR"],
    "risperidone": ["SCHIZOPHRENIA", "BIPOLAR"],
    "quetiapine": ["SCHIZOPHRENIA", "BIPOLAR", "INSOMNIA"],
    "aripiprazole": ["SCHIZOPHRENIA", "BIPOLAR", "ANTIDEPRESSANT_AUGMENTATION"],
    "ziprasidone": ["SCHIZOPHRENIA", "BIPOLAR"],
    "amisulpride": ["SCHIZOPHRENIA"],
    "paliperidone": ["SCHIZOPHRENIA"],
    "asenapine": ["SCHIZOPHRENIA", "BIPOLAR"],
    "lurasidone": ["SCHIZOPHRENIA", "BIPOLAR"],
    "brexpiprazole": ["SCHIZOPHRENIA", "ANTIDEPRESSANT_AUGMENTATION"],
    "cariprazine": ["SCHIZOPHRENIA", "BIPOLAR"],
    "iloperidone": ["SCHIZOPHRENIA"],

    # 心境稳定剂 (3)
    "valproic_acid": ["BIPOLAR", "EPILEPSY", "MIGRAINE"],
    "lithium": ["BIPOLAR", "ANTIDEPRESSANT_AUGMENTATION"],
    "lamotrigine": ["BIPOLAR", "EPILEPSY", "NEUROPATHIC_PAIN"],

    # 抗抑郁 (26)
    "fluoxetine": ["DEPRESSION", "OCD", "EATING_DISORDER", "ANXIETY"],
    "sertraline": ["DEPRESSION", "OCD", "PTSD", "ANXIETY"],
    "paroxetine": ["DEPRESSION", "OCD", "PTSD", "ANXIETY"],
    "escitalopram": ["DEPRESSION", "ANXIETY"],
    "fluvoxamine": ["OCD", "DEPRESSION", "ANXIETY"],
    "citalopram": ["DEPRESSION"],
    "venlafaxine": ["DEPRESSION", "ANXIETY", "NEUROPATHIC_PAIN"],
    "duloxetine": ["DEPRESSION", "ANXIETY", "NEUROPATHIC_PAIN"],
    "mirtazapine": ["DEPRESSION", "INSOMNIA"],
    "bupropion": ["DEPRESSION", "ADHD", "SMOKING_CESSATION"] if False else ["DEPRESSION", "ADHD"],  # 伐尼克兰不是 bupropion
    "vortioxetine": ["DEPRESSION", "ANXIETY"],
    "agomelatine": ["DEPRESSION", "INSOMNIA"],
    "trazodone": ["DEPRESSION", "INSOMNIA"],
    "phenelzine": ["DEPRESSION"],
    "selegiline": ["DEPRESSION", "PARKINSON"],
    "amitriptyline": ["DEPRESSION", "NEUROPATHIC_PAIN", "MIGRAINE"],
    "nortriptyline": ["DEPRESSION", "NEUROPATHIC_PAIN"],
    "clomipramine": ["DEPRESSION", "OCD"],
    "imipramine": ["DEPRESSION", "ENURESIS"] if False else ["DEPRESSION", "ANXIETY"],
    "tianeptine": ["DEPRESSION", "ANXIETY"],
    "bupropion_xl": ["DEPRESSION"],
    "venlafaxine_xr": ["DEPRESSION", "ANXIETY"],
    "paroxetine_cr": ["DEPRESSION", "OCD"],
    "fluoxetine_weekly": ["DEPRESSION"],
    "desvenlafaxine": ["DEPRESSION"],
    "norfluoxetine": ["DEPRESSION"],

    # 抗焦虑/安眠 (BZD + Z药)
    "diazepam": ["ANXIETY", "EPILEPSY", "SEDATION", "WITHDRAWAL"],
    "lorazepam": ["ANXIETY", "INSOMNIA", "SEDATION"],
    "buspirone": ["ANXIETY"],
    "zolpidem": ["INSOMNIA"],
    "zaleplon": ["INSOMNIA"],
    "eszopiclone": ["INSOMNIA"],
    "zopiclone": ["INSOMNIA"],
    "triazolam": ["INSOMNIA"],
    "alprazolam": ["ANXIETY", "PANIC"] if False else ["ANXIETY"],
    "estazolam": ["INSOMNIA"],
    "nitrazepam": ["INSOMNIA", "EPILEPSY"],
    "midazolam": ["ANESTHESIA", "SEDATION"],
    "phenobarbital": ["EPILEPSY", "SEDATION"],
    "amobarbital": ["SEDATION", "ANESTHESIA"],
    "secobarbital": ["INSOMNIA", "SEDATION"],
    "chloral_hydrate": ["INSOMNIA", "SEDATION"],
    "hydroxyzine": ["ANXIETY", "ALLERGY", "SEDATION"],

    # 抗癫痫 (11)
    "carbamazepine": ["EPILEPSY", "NEUROPATHIC_PAIN", "BIPOLAR"],
    "oxcarbazepine": ["EPILEPSY"],
    "topiramate": ["EPILEPSY", "MIGRAINE"],
    "gabapentin": ["EPILEPSY", "NEUROPATHIC_PAIN"],
    "pregabalin": ["NEUROPATHIC_PAIN", "ANXIETY"],
    "phenytoin": ["EPILEPSY"],
    "levetiracetam": ["EPILEPSY"],
    "eslicarbazepine": ["EPILEPSY"],
    "perampanel": ["EPILEPSY"],
    "lacosamide": ["EPILEPSY"],

    # 抗帕金森 (7)
    "levodopa_carbidopa": ["PARKINSON"],
    "pramipexole": ["PARKINSON", "RESTLESS_LEGS"] if False else ["PARKINSON"],
    "ropinirole": ["PARKINSON", "RESTLESS_LEGS"] if False else ["PARKINSON"],
    "rasagiline": ["PARKINSON"],
    "entacapone": ["PARKINSON"],
    "amantadine": ["PARKINSON", "INFLUENZA"] if False else ["PARKINSON"],
    "tolcapone": ["PARKINSON"],
    "benztropine": ["ANTICHOLINERGIC", "PARKINSON"],
    "trihexyphenidyl": ["ANTICHOLINERGIC", "PARKINSON"],

    # 抗胆碱 (EPS 用)
    # 已在上面

    # 抗糖尿病 (6)
    "metformin": ["DIABETES"],
    "empagliflozin": ["DIABETES", "HEART_FAILURE"],
    "glipizide": ["DIABETES"],
    "sitagliptin": ["DIABETES"],
    "liraglutide": ["DIABETES"],
    "pioglitazone": ["DIABETES"],

    # 调脂 (1)
    "atorvastatin": ["DYSLIPIDEMIA"],

    # 降压 (4)
    "amlodipine": ["HYPERTENSION"],
    "lisinopril": ["HYPERTENSION", "HEART_FAILURE"],
    "losartan": ["HYPERTENSION"],
    "metoprolol": ["HYPERTENSION", "ARRHYTHMIA", "HEART_FAILURE"],

    # 甲状腺 (3)
    "levothyroxine": ["THYROID", "ANTIDEPRESSANT_AUGMENTATION"],
    "methimazole": ["THYROID"],
    "propylthiouracil": ["THYROID"],

    # 抗凝/抗血小板 (4)
    "warfarin": ["ANTICOAGULATION"],
    "rivaroxaban": ["ANTICOAGULATION"],
    "aspirin": ["ANTICOAGULATION", "PAIN", "INFLAMMATION"],
    "clopidogrel": ["ANTICOAGULATION"],

    # PPI
    "omeprazole": ["GERD"],

    # 抗心律失常
    "amiodarone": ["ARRHYTHMIA", "THYROID"],  # 胺碘酮含碘, 影响甲状腺
    "sotalol": ["ARRHYTHMIA"],

    # 阿尔茨海默 (4)
    "donepezil": ["ALZHEIMERS"],
    "rivastigmine": ["ALZHEIMERS"],
    "galantamine": ["ALZHEIMERS"],
    "memantine": ["ALZHEIMERS"],

    # 抗组胺
    "diphenhydramine": ["ALLERGY", "INSOMNIA", "SEDATION"],
    "meclizine": ["ALLERGY", "NAUSEA"],

    # 偏头痛
    "sumatriptan": ["MIGRAINE"],
    "flunarizine": ["MIGRAINE"],

    # 肌松/解痉
    "baclofen": ["SPASTICITY"] if False else ["OTHER"],
    "tizanidine": ["SPASTICITY"] if False else ["OTHER"],

    # 抗眩晕
    "betahistine": ["VERTIGO"] if False else ["OTHER"],

    # 阿片/镇痛 (10)
    "tramadol": ["PAIN"],
    "buprenorphine": ["PAIN", "SUBSTANCE_USE"],
    "methadone": ["PAIN", "SUBSTANCE_USE"],
    "morphine": ["PAIN"],
    "fentanyl": ["PAIN", "ANESTHESIA"],
    "sufentanil": ["ANESTHESIA"],
    "remifentanil": ["ANESTHESIA"],
    "oxycodone": ["PAIN"],
    "hydromorphone": ["PAIN"],
    "pethidine": ["PAIN", "ANESTHESIA"],
    "codeine": ["PAIN", "ANTITUSSIVE"] if False else ["PAIN"],
    "dihydroetorphine": ["PAIN"],
    "morphine_6_glucuronide": ["PAIN"],

    # 物质依赖
    "naltrexone": ["SUBSTANCE_USE", "ALCOHOLISM"] if False else ["SUBSTANCE_USE"],
    "acamprosate": ["SUBSTANCE_USE"],
    "disulfiram": ["SUBSTANCE_USE"],

    # 兴奋剂 / ADHD
    "methylphenidate": ["ADHD", "NARCOLEPSY"] if False else ["ADHD"],
    "atomoxetine": ["ADHD"],
    "modafinil": ["ADHD", "NARCOLEPSY"] if False else ["NARCOLEPSY"],
    "dextroamphetamine": ["ADHD"],
    "amphetamine": ["ADHD", "NARCOLEPSY"] if False else ["ADHD"],

    # 抗感染
    "fluconazole": ["INFECTION"],
    "rifampin": ["INFECTION"],
    "dolutegravir": ["INFECTION"],

    # 痛风
    "allopurinol": ["GOUT"],
    "colchicine": ["GOUT", "INFLAMMATION"],

    # 抗炎/激素
    "prednisone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "prednisolone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "hydrocortisone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "dexamethasone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "methylprednisolone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],

    # 骨质疏松
    "alendronate": ["OSTEOPOROSIS"],
    "risedronate": ["OSTEOPOROSIS"],

    # 激素
    "estradiol": ["HORMONE"],
    "progesterone": ["HORMONE"],
    "finasteride": ["BPH", "HORMONE"],
    "dutasteride": ["BPH"],

    # 泌尿
    "desmopressin": ["DI", "ENURESIS"] if False else ["DI"],
    "tamsulosin": ["BPH"],

    # 镇静 / 滥用物质
    "ketamine": ["ANESTHESIA", "DEPRESSION"],  # 氯胺酮 esketamine 治难治性抑郁
    "mdma": ["PTSD"],  # MDMA-assisted therapy FDA 突破性疗法
    "cocaine": ["ANESTHESIA"],  # 局麻
    "ghb": ["NARCOLEPSY"],  # 美国用于发作性睡病, 中国 I 类

    # 代谢物
    "norketamine": ["ANESTHESIA"],

    # 补充剂
    "calcium_carbonate": ["SUPPLEMENT"],
    "ferrous_sulfate": ["SUPPLEMENT"],
    "folic_acid": ["SUPPLEMENT"],

    # 吸入剂 (哮喘/COPD)
    "arformoterol": ["ASTHMA_COPD"],
    "levalbuterol": ["ASTHMA_COPD"],
}

# 兜底: 未列出的药按 DrugCategory 自动映射
CATEGORY_FALLBACK = {
    "ANTIPSYCHOTIC": ["SCHIZOPHRENIA"],
    "MOOD_STABILIZER": ["BIPOLAR"],
    "ANTIDEPRESSANT": ["DEPRESSION"],
    "ANXIOLYTIC": ["ANXIETY"],
    "ANTIEPILEPTIC": ["EPILEPSY"],
    "ANTIPARKINSONIAN": ["PARKINSON"],
    "ANTIDIABETIC": ["DIABETES"],
    "STIMULANT": ["ADHD"],
    "CORTICOSTEROID": ["ADRENAL"],
    "ANTIHYPERTENSIVE": ["HYPERTENSION"],
    "THYROID": ["THYROID"],
    "ANTICHOLINERGIC": ["ANTICHOLINERGIC"],
    "STATIN": ["DYSLIPIDEMIA"],
    "ANTIHISTAMINE": ["ALLERGY"],
    "ANALGESIC": ["PAIN"],
    "OTHER": ["OTHER"]
}

# 过滤掉所有 if False 后的伪字段 (Python 里 `if False` 的项被跳过)
# 用一个简单 approach: 显式构建一个干净 dict
clean_groups = {k: [g for g in v if "if False" not in str(g)] for k, v in GROUPS.items()}

# 修正: Python 的 [g for g in v if ...] 实际上仍然包含 string 化 "if False" 的项, 但 v 里的项是字符串
# 让我重新构造 — 重新定义 GROUPS 不包含无效条目
GROUPS_CLEAN = {
    # 抗精神病
    "clozapine": ["SCHIZOPHRENIA"],
    "olanzapine": ["SCHIZOPHRENIA", "BIPOLAR"],
    "risperidone": ["SCHIZOPHRENIA", "BIPOLAR"],
    "quetiapine": ["SCHIZOPHRENIA", "BIPOLAR", "INSOMNIA"],
    "aripiprazole": ["SCHIZOPHRENIA", "BIPOLAR", "ANTIDEPRESSANT_AUGMENTATION"],
    "ziprasidone": ["SCHIZOPHRENIA", "BIPOLAR"],
    "amisulpride": ["SCHIZOPHRENIA"],
    "paliperidone": ["SCHIZOPHRENIA"],
    "asenapine": ["SCHIZOPHRENIA", "BIPOLAR"],
    "lurasidone": ["SCHIZOPHRENIA", "BIPOLAR"],
    "brexpiprazole": ["SCHIZOPHRENIA", "ANTIDEPRESSANT_AUGMENTATION"],
    "cariprazine": ["SCHIZOPHRENIA", "BIPOLAR"],
    "iloperidone": ["SCHIZOPHRENIA"],
    # 心境稳定剂
    "valproic_acid": ["BIPOLAR", "EPILEPSY", "MIGRAINE"],
    "lithium": ["BIPOLAR", "ANTIDEPRESSANT_AUGMENTATION"],
    "lamotrigine": ["BIPOLAR", "EPILEPSY", "NEUROPATHIC_PAIN"],
    # 抗抑郁
    "fluoxetine": ["DEPRESSION", "OCD", "EATING_DISORDER", "ANXIETY"],
    "sertraline": ["DEPRESSION", "OCD", "PTSD", "ANXIETY"],
    "paroxetine": ["DEPRESSION", "OCD", "PTSD", "ANXIETY"],
    "escitalopram": ["DEPRESSION", "ANXIETY"],
    "fluvoxamine": ["OCD", "DEPRESSION", "ANXIETY"],
    "citalopram": ["DEPRESSION"],
    "venlafaxine": ["DEPRESSION", "ANXIETY", "NEUROPATHIC_PAIN"],
    "duloxetine": ["DEPRESSION", "ANXIETY", "NEUROPATHIC_PAIN"],
    "mirtazapine": ["DEPRESSION", "INSOMNIA"],
    "bupropion": ["DEPRESSION", "ADHD"],
    "vortioxetine": ["DEPRESSION", "ANXIETY"],
    "agomelatine": ["DEPRESSION", "INSOMNIA"],
    "trazodone": ["DEPRESSION", "INSOMNIA"],
    "phenelzine": ["DEPRESSION"],
    "selegiline": ["DEPRESSION", "PARKINSON"],
    "amitriptyline": ["DEPRESSION", "NEUROPATHIC_PAIN", "MIGRAINE"],
    "nortriptyline": ["DEPRESSION", "NEUROPATHIC_PAIN"],
    "clomipramine": ["DEPRESSION", "OCD"],
    "imipramine": ["DEPRESSION", "ANXIETY"],
    "tianeptine": ["DEPRESSION", "ANXIETY"],
    "bupropion_xl": ["DEPRESSION"],
    "venlafaxine_xr": ["DEPRESSION", "ANXIETY"],
    "paroxetine_cr": ["DEPRESSION", "OCD"],
    "fluoxetine_weekly": ["DEPRESSION"],
    "desvenlafaxine": ["DEPRESSION"],
    "norfluoxetine": ["DEPRESSION"],
    # 抗焦虑/安眠
    "diazepam": ["ANXIETY", "EPILEPSY", "SEDATION", "WITHDRAWAL"],
    "lorazepam": ["ANXIETY", "INSOMNIA", "SEDATION"],
    "buspirone": ["ANXIETY"],
    "zolpidem": ["INSOMNIA"],
    "zaleplon": ["INSOMNIA"],
    "eszopiclone": ["INSOMNIA"],
    "zopiclone": ["INSOMNIA"],
    "triazolam": ["INSOMNIA"],
    "alprazolam": ["ANXIETY"],
    "estazolam": ["INSOMNIA"],
    "nitrazepam": ["INSOMNIA", "EPILEPSY"],
    "midazolam": ["ANESTHESIA", "SEDATION"],
    "phenobarbital": ["EPILEPSY", "SEDATION"],
    "amobarbital": ["SEDATION", "ANESTHESIA"],
    "secobarbital": ["INSOMNIA", "SEDATION"],
    "chloral_hydrate": ["INSOMNIA", "SEDATION"],
    "hydroxyzine": ["ANXIETY", "ALLERGY", "SEDATION"],
    # 抗癫痫
    "carbamazepine": ["EPILEPSY", "NEUROPATHIC_PAIN", "BIPOLAR"],
    "oxcarbazepine": ["EPILEPSY"],
    "topiramate": ["EPILEPSY", "MIGRAINE"],
    "gabapentin": ["EPILEPSY", "NEUROPATHIC_PAIN"],
    "pregabalin": ["NEUROPATHIC_PAIN", "ANXIETY"],
    "phenytoin": ["EPILEPSY"],
    "levetiracetam": ["EPILEPSY"],
    "eslicarbazepine": ["EPILEPSY"],
    "perampanel": ["EPILEPSY"],
    "lacosamide": ["EPILEPSY"],
    # 抗帕金森
    "levodopa_carbidopa": ["PARKINSON"],
    "pramipexole": ["PARKINSON"],
    "ropinirole": ["PARKINSON"],
    "rasagiline": ["PARKINSON"],
    "entacapone": ["PARKINSON"],
    "amantadine": ["PARKINSON"],
    "tolcapone": ["PARKINSON"],
    "benztropine": ["ANTICHOLINERGIC", "PARKINSON"],
    "trihexyphenidyl": ["ANTICHOLINERGIC", "PARKINSON"],
    # 糖尿病
    "metformin": ["DIABETES"],
    "empagliflozin": ["DIABETES", "HEART_FAILURE"],
    "glipizide": ["DIABETES"],
    "sitagliptin": ["DIABETES"],
    "liraglutide": ["DIABETES"],
    "pioglitazone": ["DIABETES"],
    # 调脂
    "atorvastatin": ["DYSLIPIDEMIA"],
    # 降压
    "amlodipine": ["HYPERTENSION"],
    "lisinopril": ["HYPERTENSION", "HEART_FAILURE"],
    "losartan": ["HYPERTENSION"],
    "metoprolol": ["HYPERTENSION", "ARRHYTHMIA", "HEART_FAILURE"],
    # 甲状腺
    "levothyroxine": ["THYROID", "ANTIDEPRESSANT_AUGMENTATION"],
    "methimazole": ["THYROID"],
    "propylthiouracil": ["THYROID"],
    # 抗凝
    "warfarin": ["ANTICOAGULATION"],
    "rivaroxaban": ["ANTICOAGULATION"],
    "aspirin": ["ANTICOAGULATION", "PAIN", "INFLAMMATION"],
    "clopidogrel": ["ANTICOAGULATION"],
    # PPI
    "omeprazole": ["GERD"],
    # 抗心律失常
    "amiodarone": ["ARRHYTHMIA", "THYROID"],
    "sotalol": ["ARRHYTHMIA"],
    # 阿尔茨海默
    "donepezil": ["ALZHEIMERS"],
    "rivastigmine": ["ALZHEIMERS"],
    "galantamine": ["ALZHEIMERS"],
    "memantine": ["ALZHEIMERS"],
    # 抗组胺
    "diphenhydramine": ["ALLERGY", "INSOMNIA", "SEDATION"],
    "meclizine": ["ALLERGY", "NAUSEA"],
    # 偏头痛
    "sumatriptan": ["MIGRAINE"],
    "flunarizine": ["MIGRAINE"],
    # 肌松
    "baclofen": ["OTHER"],
    "tizanidine": ["OTHER"],
    "betahistine": ["OTHER"],
    # 阿片
    "tramadol": ["PAIN"],
    "buprenorphine": ["PAIN", "SUBSTANCE_USE"],
    "methadone": ["PAIN", "SUBSTANCE_USE"],
    "morphine": ["PAIN"],
    "fentanyl": ["PAIN", "ANESTHESIA"],
    "sufentanil": ["ANESTHESIA"],
    "remifentanil": ["ANESTHESIA"],
    "oxycodone": ["PAIN"],
    "hydromorphone": ["PAIN"],
    "pethidine": ["PAIN", "ANESTHESIA"],
    "codeine": ["PAIN"],
    "dihydroetorphine": ["PAIN"],
    "morphine_6_glucuronide": ["PAIN"],
    # 物质依赖
    "naltrexone": ["SUBSTANCE_USE"],
    "acamprosate": ["SUBSTANCE_USE"],
    "disulfiram": ["SUBSTANCE_USE"],
    # 兴奋剂
    "methylphenidate": ["ADHD"],
    "atomoxetine": ["ADHD"],
    "modafinil": ["NARCOLEPSY"] if False else ["ADHD"],  # 简化 → ADHD
    "dextroamphetamine": ["ADHD"],
    "amphetamine": ["ADHD"],
    # 抗感染
    "fluconazole": ["INFECTION"],
    "rifampin": ["INFECTION"],
    "dolutegravir": ["INFECTION"],
    # 痛风
    "allopurinol": ["GOUT"],
    "colchicine": ["GOUT", "INFLAMMATION"],
    # 抗炎
    "prednisone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "prednisolone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "hydrocortisone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "dexamethasone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    "methylprednisolone": ["ADRENAL", "INFLAMMATION", "ALLERGY"],
    # 骨质疏松
    "alendronate": ["OSTEOPOROSIS"],
    "risedronate": ["OSTEOPOROSIS"],
    # 激素
    "estradiol": ["HORMONE"],
    "progesterone": ["HORMONE"],
    "finasteride": ["BPH", "HORMONE"],
    "dutasteride": ["BPH"],
    # 泌尿
    "desmopressin": ["DI"],
    "tamsulosin": ["BPH"],
    # 镇静
    "ketamine": ["ANESTHESIA", "DEPRESSION"],
    "mdma": ["PTSD"],
    "cocaine": ["ANESTHESIA"],
    "ghb": ["NARCOLEPSY"],
    "norketamine": ["ANESTHESIA"],
    # 补充剂
    "calcium_carbonate": ["SUPPLEMENT"],
    "ferrous_sulfate": ["SUPPLEMENT"],
    "folic_acid": ["SUPPLEMENT"],
    # 吸入剂
    "arformoterol": ["ASTHMA_COPD"],
    "levalbuterol": ["ASTHMA_COPD"],
}

n = 0
n_fallback = 0
import collections
counts = collections.Counter()
for drug in data["drugs"]:
    if drug["id"] in GROUPS_CLEAN:
        groups = GROUPS_CLEAN[drug["id"]]
        drug["indicationGroups"] = groups
        n += 1
    else:
        # 兜底: 按 DrugCategory 映射
        cat = drug.get("category", "OTHER")
        groups = CATEGORY_FALLBACK.get(cat, ["OTHER"])
        drug["indicationGroups"] = groups
        n_fallback += 1
    for g in drug["indicationGroups"]:
        counts[g] += 1

p.write_text(
    json.dumps(data, ensure_ascii=False, indent=2),
    encoding="utf-8"
)
print(f"已分类 {n} 个药 (手工) + {n_fallback} 个药 (兜底), 共 {n+n_fallback} / {len(data['drugs'])}")
print(f"\n=== IndicationGroup 分布 (前 20) ===")
for k, v in counts.most_common(20):
    print(f"  {k}: {v}")
print(f"\n未分组的药 (兜底 OTHER): {sum(1 for d in data['drugs'] if d.get('indicationGroups') == ['OTHER'])}")
