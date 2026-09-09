"""Generate i18n additions for DrugCategory, IndicationGroup, OverdoseSeverity, CypEnzyme, PathwayType enums."""

# English translations for each enum value
DRUG_CAT_EN = {
    'ANTIPSYCHOTIC': 'Antipsychotic',
    'MOOD_STABILIZER': 'Mood Stabilizer',
    'ANTIDEPRESSANT': 'Antidepressant',
    'ANXIOLYTIC': 'Anxiolytic / Benzodiazepine',
    'STIMULANT': 'ADHD Stimulant',
    'ANTICHOLINERGIC': 'Anticholinergic',
    'ANTIPARKINSONIAN': 'Antiparkinsonian',
    'ANTIEPILEPTIC': 'Antiepileptic',
    'ALZHEIMERS': 'Anti-dementia / Cognition',
    'ANTIMIGRAINE': 'Antimigraine',
    'MUSCLE_RELAXANT': 'Muscle Relaxant',
    'ANESTHETIC': 'Anesthetic (Local/General)',
    'ANTIVERTIGO': 'Antivertigo',
    'ANTIDIABETIC': 'Antidiabetic',
    'THYROID': 'Thyroid',
    'CORTICOSTEROID': 'Corticosteroid',
    'ANTIHYPERTENSIVE': 'Antihypertensive',
    'STATIN': 'Statin (Lipid-lowering)',
    'ANTIARRHYTHMIC': 'Antiarrhythmic',
    'ANTICOAGULANT': 'Anticoagulant / Antiplatelet',
    'PPI': 'Proton Pump Inhibitor (PPI)',
    'OSTEOPOROSIS_DRUG': 'Osteoporosis Drug',
    'GOUT': 'Antigout',
    'BPH_AGENT': 'BPH Agent',
    'HORMONE_REPLACEMENT': 'Hormone Replacement',
    'BRONCHODILATOR': 'Bronchodilator',
    'ANALGESIC': 'Analgesic',
    'ANTIHISTAMINE': 'Antihistamine',
    'ANTIBIOTIC': 'Anti-infective (Antifungal / TB / HIV)',
    'SUBSTANCE_USE': 'Substance Use Disorder Tx',
    'SUPPLEMENT': 'Supplement',
    'HERBAL': 'Herbal Supplement',
    'OTHER': 'Other',
}

# Japanese translations
DRUG_CAT_JA = {
    'ANTIPSYCHOTIC': '抗精神病薬',
    'MOOD_STABILIZER': '気分安定薬',
    'ANTIDEPRESSANT': '抗うつ薬',
    'ANXIOLYTIC': '抗不安薬 / ベンゾジアゼピン',
    'STIMULANT': 'ADHD 興奮剤',
    'ANTICHOLINERGIC': '抗コリン薬',
    'ANTIPARKINSONIAN': '抗パーキンソン薬',
    'ANTIEPILEPTIC': '抗てんかん薬',
    'ALZHEIMERS': '抗認知症薬',
    'ANTIMIGRAINE': '抗片頭痛薬',
    'MUSCLE_RELAXANT': '筋弛緩薬',
    'ANESTHETIC': '麻酔薬 (局所/全身)',
    'ANTIVERTIGO': '抗めまい薬',
    'ANTIDIABETIC': '糖尿病治療薬',
    'THYROID': '甲状腺',
    'CORTICOSTEROID': '副腎皮質ステロイド',
    'ANTIHYPERTENSIVE': '降圧薬',
    'STATIN': '脂質異常症治療薬 (スタチン)',
    'ANTIARRHYTHMIC': '抗不整脈薬',
    'ANTICOAGULANT': '抗凝固/抗血小板薬',
    'PPI': 'プロトンポンプ阻害薬 (PPI)',
    'OSTEOPOROSIS_DRUG': '骨粗鬆症治療薬',
    'GOUT': '抗痛風薬',
    'BPH_AGENT': '前立腺肥大症治療薬',
    'HORMONE_REPLACEMENT': 'ホルモン補充療法',
    'BRONCHODILATOR': '気管支拡張薬',
    'ANALGESIC': '鎮痛薬',
    'ANTIHISTAMINE': '抗ヒスタミン薬',
    'ANTIBIOTIC': '抗感染症薬 (抗真菌/TB/HIV)',
    'SUBSTANCE_USE': '物質依存治療薬',
    'SUPPLEMENT': '栄養補助食品',
    'HERBAL': 'ハーブサプリメント',
    'OTHER': 'その他',
}

# IndicationGroup English
INDICATION_EN = {
    'DEPRESSION': 'Depression',
    'SCHIZOPHRENIA': 'Schizophrenia',
    'BIPOLAR': 'Bipolar Disorder',
    'ANXIETY': 'Anxiety Disorder',
    'INSOMNIA': 'Insomnia',
    'ADHD': 'ADHD',
    'OCD': 'OCD',
    'PTSD': 'PTSD',
    'EATING_DISORDER': 'Eating Disorder',
    'EPILEPSY': 'Epilepsy',
    'PARKINSON': "Parkinson's Disease",
    'ALZHEIMERS': "Alzheimer's Disease",
    'MIGRAINE': 'Migraine',
    'NEUROPATHIC_PAIN': 'Neuropathic Pain',
    'PAIN': 'Pain',
    'ANESTHESIA': 'Anesthesia',
    'DIABETES': 'Diabetes',
    'HYPERTENSION': 'Hypertension',
    'DYSLIPIDEMIA': 'Dyslipidemia',
    'THYROID': 'Thyroid Disease',
    'ANTICOAGULATION': 'Anticoagulation / Antiplatelet',
    'ASTHMA_COPD': 'Asthma / COPD',
    'OSTEOPOROSIS': 'Osteoporosis',
    'HEART_FAILURE': 'Heart Failure',
    'ARRHYTHMIA': 'Arrhythmia',
    'BPH': 'BPH',
    'GERD': 'GERD',
    'SUBSTANCE_USE': 'Substance Use (Alcohol/Opioid)',
    'NICOTINE': 'Nicotine Dependence',
    'INFECTION': 'Infection (Bacterial/Fungal/Viral)',
    'GOUT': 'Gout',
    'INFLAMMATION': 'Inflammation / Immune',
    'ALLERGY': 'Allergy',
    'ADRENAL': 'Adrenal Cortex',
    'HORMONE': 'Hormone Replacement',
    'SUPPLEMENT': 'Supplement',
    'DI': 'Diabetes Insipidus',
    'ANTICHOLINERGIC': 'Anticholinergic (EPS)',
    'ANTIDEPRESSANT_AUGMENTATION': 'Antidepressant Augmentation',
    'WITHDRAWAL': 'Withdrawal',
    'NAUSEA': 'Nausea / Vomiting',
    'SEDATION': 'Sedation',
    'OTHER': 'Other',
}

INDICATION_JA = {
    'DEPRESSION': 'うつ病',
    'SCHIZOPHRENIA': '統合失調症',
    'BIPOLAR': '双極性障害',
    'ANXIETY': '不安障害',
    'INSOMNIA': '不眠症',
    'ADHD': '注意欠陥多動性障害',
    'OCD': '強迫性障害',
    'PTSD': 'PTSD (心的外傷後ストレス障害)',
    'EATING_DISORDER': '摂食障害',
    'EPILEPSY': 'てんかん',
    'PARKINSON': 'パーキンソン病',
    'ALZHEIMERS': 'アルツハイマー病',
    'MIGRAINE': '片頭痛',
    'NEUROPATHIC_PAIN': '神経障害性疼痛',
    'PAIN': '疼痛',
    'ANESTHESIA': '麻酔',
    'DIABETES': '糖尿病',
    'HYPERTENSION': '高血圧',
    'DYSLIPIDEMIA': '脂質異常症',
    'THYROID': '甲状腺疾患',
    'ANTICOAGULATION': '抗凝固/抗血小板',
    'ASTHMA_COPD': '喘息 / COPD',
    'OSTEOPOROSIS': '骨粗鬆症',
    'HEART_FAILURE': '心不全',
    'ARRHYTHMIA': '不整脈',
    'BPH': '前立腺肥大症',
    'GERD': '胃食道逆流症',
    'SUBSTANCE_USE': '物質依存 (アルコール/オピオイド)',
    'NICOTINE': 'ニコチン依存',
    'INFECTION': '感染症 (細菌/真菌/ウイルス)',
    'GOUT': '痛風',
    'INFLAMMATION': '炎症/免疫',
    'ALLERGY': 'アレルギー',
    'ADRENAL': '副腎皮質',
    'HORMONE': 'ホルモン補充',
    'SUPPLEMENT': 'サプリメント',
    'DI': '尿崩症',
    'ANTICHOLINERGIC': '抗コリン (EPS)',
    'ANTIDEPRESSANT_AUGMENTATION': '抗うつ増強',
    'WITHDRAWAL': '離脱症状',
    'NAUSEA': '悪心・嘔吐',
    'SEDATION': '鎮静',
    'OTHER': 'その他',
}

SEVERITY_EN = {'MILD': 'Mild', 'MODERATE': 'Moderate', 'SEVERE': 'Severe', 'LIFE_THREATENING': 'Life-threatening'}
SEVERITY_JA = {'MILD': '軽度', 'MODERATE': '中等度', 'SEVERE': '重度', 'LIFE_THREATENING': '生命を脅かす'}

# CYP enzyme names are mostly universal (CYP1A2 etc.); only P_GP needs translation
CYP_EN = {
    'CYP1A2': 'CYP1A2', 'CYP2B6': 'CYP2B6', 'CYP2D6': 'CYP2D6', 'CYP3A4': 'CYP3A4',
    'CYP2C9': 'CYP2C9', 'CYP2C19': 'CYP2C19', 'CYP2E1': 'CYP2E1', 'P_GP': 'P-glycoprotein',
}
CYP_JA = {
    'CYP1A2': 'CYP1A2', 'CYP2B6': 'CYP2B6', 'CYP2D6': 'CYP2D6', 'CYP3A4': 'CYP3A4',
    'CYP2C9': 'CYP2C9', 'CYP2C19': 'CYP2C19', 'CYP2E1': 'CYP2E1', 'P_GP': 'P-糖タンパク質',
}

PATHWAY_EN = {
    'CYP450': 'CYP450 oxidation',
    'UGT_GLUCURONIDATION': 'UGT glucuronidation',
    'RENAL_EXCRETION': 'Renal excretion (unchanged)',
    'HYDROLYSIS': 'Hydrolysis',
    'ESTERASE': 'Esterase hydrolysis',
    'DEIODINATION': 'Deiodination',
    'MAO': 'Monoamine oxidase',
    'DPP4': 'DPP-IV degradation',
    'GLUCURONIDATION': 'Glucuronidation',
    'BETA_OXIDATION': 'β-oxidation',
    'OTHER': 'Other',
}
PATHWAY_JA = {
    'CYP450': 'CYP450 酸化',
    'UGT_GLUCURONIDATION': 'UGT グルクロン酸抱合',
    'RENAL_EXCRETION': '腎排泄 (未変化体)',
    'HYDROLYSIS': '加水分解',
    'ESTERASE': 'エステラーゼ加水分解',
    'DEIODINATION': '脱ヨウ素',
    'MAO': 'モノアミンオキシダーゼ',
    'DPP4': 'DPP-IV 分解',
    'GLUCURONIDATION': 'グルクロン酸抱合',
    'BETA_OXIDATION': 'β酸化',
    'OTHER': 'その他',
}

# Build per-key translations: (key, zh, en, ja)
keys = []

for en_name, zh in [
    ('ANTIPSYCHOTIC', '抗精神病药'), ('MOOD_STABILIZER', '心境稳定剂'), ('ANTIDEPRESSANT', '抗抑郁药'),
    ('ANXIOLYTIC', '抗焦虑/苯二氮䓬类'), ('STIMULANT', 'ADHD 兴奋剂'), ('ANTICHOLINERGIC', '抗胆碱能'),
    ('ANTIPARKINSONIAN', '抗帕金森'), ('ANTIEPILEPTIC', '抗癫痫'), ('ALZHEIMERS', '抗痴呆/认知改善'),
    ('ANTIMIGRAINE', '抗偏头痛'), ('MUSCLE_RELAXANT', '肌松剂'), ('ANESTHETIC', '麻醉 (局麻/全麻)'),
    ('ANTIVERTIGO', '抗眩晕'), ('ANTIDIABETIC', '降糖药'), ('THYROID', '甲状腺'),
    ('CORTICOSTEROID', '肾上腺皮质激素'), ('ANTIHYPERTENSIVE', '降压药'), ('STATIN', '调脂药'),
    ('ANTIARRHYTHMIC', '抗心律失常'), ('ANTICOAGULANT', '抗凝/抗血小板'), ('PPI', '质子泵抑制剂 (PPI)'),
    ('OSTEOPOROSIS_DRUG', '骨质疏松药'), ('GOUT', '抗痛风'), ('BPH_AGENT', '前列腺增生药'),
    ('HORMONE_REPLACEMENT', '激素替代'), ('BRONCHODILATOR', '支气管扩张剂'), ('ANALGESIC', '止痛药'),
    ('ANTIHISTAMINE', '抗组胺'), ('ANTIBIOTIC', '抗感染 (抗真菌/抗结核/抗 HIV)'),
    ('SUBSTANCE_USE', '物质依赖治疗'), ('SUPPLEMENT', '营养补充剂'), ('HERBAL', '草本补充剂'),
    ('OTHER', '其他'),
]:
    k = f'drug_cat_{en_name.lower()}'
    keys.append((k, zh, DRUG_CAT_EN[en_name], DRUG_CAT_JA[en_name]))

for en_name in [
    'DEPRESSION', 'SCHIZOPHRENIA', 'BIPOLAR', 'ANXIETY', 'INSOMNIA', 'ADHD', 'OCD', 'PTSD',
    'EATING_DISORDER', 'EPILEPSY', 'PARKINSON', 'ALZHEIMERS', 'MIGRAINE', 'NEUROPATHIC_PAIN',
    'PAIN', 'ANESTHESIA', 'DIABETES', 'HYPERTENSION', 'DYSLIPIDEMIA', 'THYROID',
    'ANTICOAGULATION', 'ASTHMA_COPD', 'OSTEOPOROSIS', 'HEART_FAILURE', 'ARRHYTHMIA', 'BPH',
    'GERD', 'SUBSTANCE_USE', 'NICOTINE', 'INFECTION', 'GOUT', 'INFLAMMATION', 'ALLERGY',
    'ADRENAL', 'HORMONE', 'SUPPLEMENT', 'DI', 'ANTICHOLINERGIC', 'ANTIDEPRESSANT_AUGMENTATION',
    'WITHDRAWAL', 'NAUSEA', 'SEDATION', 'OTHER',
]:
    k = f'ind_{en_name.lower()}'
    keys.append((k, None, INDICATION_EN[en_name], INDICATION_JA[en_name]))

for en_name, zh in [('MILD', '轻度'), ('MODERATE', '中度'), ('SEVERE', '重度'), ('LIFE_THREATENING', '危及生命')]:
    k = f'sev_{en_name.lower()}'
    keys.append((k, zh, SEVERITY_EN[en_name], SEVERITY_JA[en_name]))

for en_name, zh in [
    ('CYP1A2', 'CYP1A2'), ('CYP2B6', 'CYP2B6'), ('CYP2D6', 'CYP2D6'), ('CYP3A4', 'CYP3A4'),
    ('CYP2C9', 'CYP2C9'), ('CYP2C19', 'CYP2C19'), ('CYP2E1', 'CYP2E1'), ('P_GP', 'P-糖蛋白'),
]:
    k = f'cyp_{en_name.lower()}'
    keys.append((k, zh, CYP_EN[en_name], CYP_JA[en_name]))

for en_name, zh in [
    ('CYP450', 'CYP450 氧化'), ('UGT_GLUCURONIDATION', 'UGT 葡萄糖苷酸化'),
    ('RENAL_EXCRETION', '肾排泄 (原型)'), ('HYDROLYSIS', '水解'),
    ('ESTERASE', '酯酶水解'), ('DEIODINATION', '脱碘'),
    ('MAO', '单胺氧化酶'), ('DPP4', 'DPP-IV 酶降解'),
    ('GLUCURONIDATION', '葡萄糖醛酸化'), ('BETA_OXIDATION', 'β-氧化'),
    ('OTHER', '其他'),
]:
    k = f'pathway_{en_name.lower()}'
    keys.append((k, zh, PATHWAY_EN[en_name], PATHWAY_JA[en_name]))

# Severity (rules/Interactions.kt) — interaction alerts
SEVERITY_RULES_EN = {'CONTRAINDICATED': 'Contraindicated', 'HIGH': 'High Alert', 'MEDIUM': 'Medium Alert', 'LOW': 'Low Notice', 'INFO': 'Info'}
SEVERITY_RULES_JA = {'CONTRAINDICATED': '禁忌', 'HIGH': '高度の警告', 'MEDIUM': '中程度の警告', 'LOW': '軽度の注意', 'INFO': '情報'}
for en_name, zh in [('CONTRAINDICATED', '禁忌'), ('HIGH', '高度警示'), ('MEDIUM', '中度警示'), ('LOW', '轻度提示'), ('INFO', '信息')]:
    k = f'sevrule_{en_name.lower()}'
    keys.append((k, zh, SEVERITY_RULES_EN[en_name], SEVERITY_RULES_JA[en_name]))

print(f'# Total keys: {len(keys)}')
for k, zh, en, ja in keys:
    print(f'{k}|{zh or ""}|{en}|{ja}')
