"""A7: Fix A4 leftover bad enum values (lumateperone + xanomeline)"""
import json
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]

RENAL_MAP = {
    "USE_CAUTION_EGFR_30 (低钙风险)": "MONITOR",
    "USE_CAUTION_EGFR_30 (F-离子)": "MONITOR",
    "USE_CAUTION_EGFR_30 (trospium)": "MONITOR",
    "USE_CAUTION": "MONITOR",
    "REDUCE_50_PCT_EGFR_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_25": "REDUCE_50_PCT_EGFR_BELOW_30",
    "CONTRAINDICATED_EGFR_15": "CONTRAINDICATED_EGFR_BELOW_30",
    "CONTRAINDICATED_EGFR_30": "CONTRAINDICATED_EGFR_BELOW_30",
    "CONTRAINDICATED_EGFR_35": "CONTRAINDICATED_EGFR_BELOW_30",
}
HEPATIC_MAP = {
    "USE_CAUTION_CHILD_PUGH_C": "MONITOR",
    "USE_CAUTION_CHILD_PUGH_B": "MONITOR",
    "USE_CAUTION": "MONITOR",
    "CONTRAINDICATED_CHILD_PUGH_C": "CONTRAINDICATED_IN_SEVERE",
    "CONTRAINDICATED_CHILD_PUGH_B": "CONTRAINDICATED_IN_SEVERE",
    "REDUCE_DOSE_CHILD_PUGH_B": "REDUCE_50_PCT_CHILD_PUGH_B",
}
VALID_RENAL = {"NONE", "MONITOR", "REDUCE_50_PCT_EGFR_BELOW_30", "CONTRAINDICATED_EGFR_BELOW_30"}
VALID_HEPATIC = {"NONE", "MONITOR", "REDUCE_50_PCT_CHILD_PUGH_B", "REDUCE_50_PCT_CHILD_PUGH_C", "CONTRAINDICATED_IN_SEVERE"}

fixed = 0
for d in drugs:
    adj = d.get("adjustments", {}) or {}
    r = adj.get("renal", "NONE")
    h = adj.get("hepatic", "NONE")
    if r in RENAL_MAP:
        adj["renal"] = RENAL_MAP[r]
        fixed += 1
    elif r not in VALID_RENAL:
        adj["renal"] = "NONE"
        fixed += 1
    if h in HEPATIC_MAP:
        adj["hepatic"] = HEPATIC_MAP[h]
        fixed += 1
    elif h not in VALID_HEPATIC:
        adj["hepatic"] = "NONE"
        fixed += 1
    d["adjustments"] = adj

print(f"Fixed {fixed} enum values")
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Saved {V06}")
