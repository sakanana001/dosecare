#!/usr/bin/env python3
"""DrugCategory enum 扩展: 加 16 个新类 (替换原 16 个)"""
from pathlib import Path
p = Path("app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt")
text = p.read_text(encoding="utf-8")

old = '''enum class DrugCategory(val displayName: String) {
    ANTIPSYCHOTIC("抗精神病药"),
    MOOD_STABILIZER("心境稳定剂"),
    ANTIDEPRESSANT("抗抑郁药"),
    ANXIOLYTIC("抗焦虑/苯二氮䓬类"),
    STIMULANT("ADHD 兴奋剂"),
    ANTICHOLINERGIC("抗胆碱能"),
    ANTIPARKINSONIAN("抗帕金森"),
    ANTIDIABETIC("降糖药"),
    THYROID("甲状腺"),
    CORTICOSTEROID("肾上腺皮质激素"),
    ANTIHYPERTENSIVE("降压药"),
    STATIN("调脂药"),
    ANTIEPILEPTIC("抗癫痫"),
    ANALGESIC("止痛药"),
    ANTIHISTAMINE("抗组胺"),
    HERBAL("草本补充剂"),
    OTHER("其他")
}'''

new = '''enum class DrugCategory(val displayName: String) {
    // 精神神经
    ANTIPSYCHOTIC("抗精神病药"),
    MOOD_STABILIZER("心境稳定剂"),
    ANTIDEPRESSANT("抗抑郁药"),
    ANXIOLYTIC("抗焦虑/苯二氮䓬类"),
    STIMULANT("ADHD 兴奋剂"),
    ANTICHOLINERGIC("抗胆碱能"),
    ANTIPARKINSONIAN("抗帕金森"),
    ANTIEPILEPTIC("抗癫痫"),
    ALZHEIMERS("抗痴呆/认知改善"),
    ANTIMIGRAINE("抗偏头痛"),
    MUSCLE_RELAXANT("肌松剂"),
    ANESTHETIC("麻醉 (局麻/全麻)"),
    ANTIVERTIGO("抗眩晕"),
    // 内科
    ANTIDIABETIC("降糖药"),
    THYROID("甲状腺"),
    CORTICOSTEROID("肾上腺皮质激素"),
    ANTIHYPERTENSIVE("降压药"),
    STATIN("调脂药"),
    ANTIARRHYTHMIC("抗心律失常"),
    ANTICOAGULANT("抗凝/抗血小板"),
    PPI("质子泵抑制剂 (PPI)"),
    OSTEOPOROSIS_DRUG("骨质疏松药"),
    GOUT("抗痛风"),
    BPH_AGENT("前列腺增生药"),
    HORMONE_REPLACEMENT("激素替代"),
    BRONCHODILATOR("支气管扩张剂"),
    // 疼痛
    ANALGESIC("止痛药"),
    // 抗感染
    ANTIHISTAMINE("抗组胺"),
    ANTIBIOTIC("抗感染 (抗真菌/抗结核/抗 HIV)"),
    // 其他
    SUBSTANCE_USE("物质依赖治疗"),
    SUPPLEMENT("营养补充剂"),
    HERBAL("草本补充剂"),
    OTHER("其他")
}'''

if old not in text:
    print("FAIL: old_string not found")
else:
    text = text.replace(old, new, 1)
    p.write_text(text, encoding="utf-8")
    print("OK: DrugCategory enum 扩展完成 (16 -> 32 类)")
