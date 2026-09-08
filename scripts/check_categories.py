import json
d = json.loads(open("app/src/main/assets/drugs/v0.6.json", encoding="utf-8").read())
cats = set()
for drug in d["drugs"]:
    cats.add(drug["category"])
print("实际使用 category:", sorted(cats))
# 对比 DrugCategory enum
expected = {
    "ANTIPSYCHOTIC", "MOOD_STABILIZER", "ANTIDEPRESSANT", "ANXIOLYTIC",
    "STIMULANT", "ANTICHOLINERGIC", "ANTIPARKINSONIAN", "ANTIEPILEPTIC",
    "ALZHEIMERS", "ANTIMIGRAINE", "MUSCLE_RELAXANT", "ANESTHETIC",
    "ANTIVERTIGO", "ANTIDIABETIC", "THYROID", "CORTICOSTEROID",
    "ANTIHYPERTENSIVE", "STATIN", "ANTIARRHYTHMIC", "ANTICOAGULANT",
    "PPI", "OSTEOPOROSIS_DRUG", "GOUT", "BPH_AGENT",
    "HORMONE_REPLACEMENT", "BRONCHODILATOR", "ANALGESIC", "ANTIHISTAMINE",
    "ANTIBIOTIC", "SUBSTANCE_USE", "SUPPLEMENT", "HERBAL", "OTHER"
}
missing = cats - expected
print("\nUNKNOWN categories (在 catalog 但不在 enum):", missing)
extra = expected - cats
print("UNUSED enum 类别:", extra)
