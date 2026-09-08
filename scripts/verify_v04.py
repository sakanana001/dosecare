"""验证 v0.4.json 在三处的完整性"""
import json
from pathlib import Path

paths = [
    "C:/Users/yuwen/Desktop/精品神药/data/drugs/v0.4.json",
    "C:/Users/yuwen/Desktop/精品神药/app/src/main/assets/drugs/v0.4.json",
    "C:/Users/yuwen/Desktop/精品神药/app/src/test/resources/drugs/v0.4.json",
]
for p in paths:
    d = json.load(open(p, "r", encoding="utf-8-sig"))
    drugs = d["drugs"]
    cats = {}
    for x in drugs:
        c = x.get("category", "?")
        cats[c] = cats.get(c, 0) + 1
    print(f"== {p}")
    print(f"   total: {len(drugs)}")
    print(f"   schema: {d.get('schemaVersion')}")
    print(f"   categories: {dict(sorted(cats.items(), key=lambda kv: -kv[1]))}")
    print(f"   first 3 ids: {[x['id'] for x in drugs[:3]]}")
    print(f"   last 3 ids:  {[x['id'] for x in drugs[-3:]]}")
