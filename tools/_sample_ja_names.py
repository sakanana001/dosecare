# -*- coding: utf-8 -*-
"""抽检 catalog.json 里的日文名: 打印若干代表性药的三语名字。"""
import io
import json
import os

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
PATH = os.path.join(REPO, "app", "src", "main", "assets", "pharmacology", "catalog.json")

with io.open(PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

drugs = data["drugs"] if isinstance(data, dict) and "drugs" in data else data
by_id = {d["id"]: d for d in drugs}

SAMPLE = [
    "clozapine", "estradiol", "testosterone", "zolpidem", "sertraline",
    "fluoxetine", "warfarin", "tamoxifen", "raloxifene", "levothyroxine",
    "finasteride", "dutasteride", "spironolactone", "progesterone",
    "perampanel", "eslicarbazepine", "levalbuterol", "morphine", "colchicine",
    "amitriptyline", "quetiapine", "omeprazole", "atorvastatin", "ferrous_sulfate",
]
print("%-20s %-26s %-22s %s" % ("id", "EN (INN)", "ZH", "JA"))
for sid in SAMPLE:
    d = by_id.get(sid)
    if not d:
        print("%-20s (not found)" % sid)
        continue
    print("%-20s %-26s %-22s %s" % (
        sid, d.get("genericName"), d.get("genericNameZh"), d.get("genericNameJa")))
