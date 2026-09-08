#!/usr/bin/env python3
"""v0.6.1 PK 参数校准: 修 4 个明显错的 PK 参数 (权威源: AGNP 2017 / FDA DailyMed)"""
import json
from pathlib import Path

p = Path("app/src/main/assets/drugs/v0.6.json")
data = json.loads(p.read_text(encoding="utf-8"))

# 权威源: AGNP 2017 共识指南 (Therapeutic Drug Monitoring in Psychiatry)
#          + FDA DailyMed 药品标签 (Abilify / Risperdal / Prozac / Depakote)
#          + 公开临床试验 PMID
fixes = {
    "valproic_acid": {
        "vdLPerKg": 0.18,       # 之前 18 错 100x; 真实 0.13-0.23 L/kg (AGNP 2017 + 丙戊酸说明书)
        "clLPerHour": 0.5,      # ~0.5 L/h/70kg, 即 35 L/h
        "note": "VdLPerKg 0.18 (之前 18 错 100x); 真实 0.13-0.23 L/kg (AGNP 2017 + Depakote 说明书)"
    },
    "aripiprazole": {
        "tHalfHours": 75.0,     # 之前 14.8h 错; 真实 75h (dehaan 2010, Abilify Maintena 说明书)
        "kePerHour": 0.00924,   # 0.693/75
        "clLPerHour": 3.17,     # vdLPerKg=4.9 * 70 * ke = 343 * 0.00924
        "note": "t1/2 75h (之前 14.8h 错 5x); 真实 75h (dehaan 2010, Abilify Maintena FDA 标签)"
    },
    "risperidone": {
        "tHalfHours": 17.0,     # 之前 3h 错; 利培酮+9-OH-risperidone 总 t1/2 17-22h
        "kePerHour": 0.0408,    # 0.693/17
        "clLPerHour": 4.3,      # 1.5 * 70 * 0.0408
        "note": "t1/2 17h 利培酮+9-OH 总 (之前 3h 错); 真实 17-22h (AGNP 2017, Risperdal Consta 标签)"
    },
    "fluoxetine": {
        "tHalfHours": 72.0,     # 之前 25h 错; 含活性代谢物诺氟西汀 7-15 天
        "kePerHour": 0.00963,   # 0.693/72
        "clLPerHour": 23.6,     # 35 * 70 * 0.00963
        "note": "t1/2 72h (之前 25h 错 3x); 真实 1-4 天 mean 72h, 含诺氟西汀 4-16 天 (FDA Prozac 标签)"
    },
}

for drug in data["drugs"]:
    if drug["id"] in fixes:
        f = fixes[drug["id"]]
        for k, v in f.items():
            if k == "note":
                if "calibrationNotes" not in drug:
                    drug["calibrationNotes"] = []
                drug["calibrationNotes"].append(v)
            elif k in drug:
                old = drug[k]
                drug[k] = v
                print(f"{drug['id']}.{k}: {old} -> {v}")
            else:
                drug[k] = v
                print(f"{drug['id']}.{k}: NEW = {v}")

p.write_text(
    json.dumps(data, ensure_ascii=False, indent=2),
    encoding="utf-8"
)
print(f"\n已校准 {len(fixes)} 个药, 写入 {p}")
