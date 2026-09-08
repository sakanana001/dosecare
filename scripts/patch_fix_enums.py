"""Fix invalid RenalAdjustment / HepaticAdjustment enum values in v0.6.json"""
import json
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]

# Valid values:
# RenalAdjustment: NONE, MONITOR, REDUCE_50_PCT_EGFR_BELOW_30, CONTRAINDICATED_EGFR_BELOW_30
# HepaticAdjustment: NONE, MONITOR, REDUCE_50_PCT_CHILD_PUGH_B, REDUCE_50_PCT_CHILD_PUGH_C, CONTRAINDICATED_IN_SEVERE

RENAL_MAP = {
    "REDUCE_50_PCT_EGFR_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_25": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_15_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_50_PCT_EGFR_15_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "CONTRAINDICATED_EGFR_15": "CONTRAINDICATED_EGFR_BELOW_30",
    "CONTRAINDICATED_EGFR_35": "CONTRAINDICATED_EGFR_BELOW_30",
    "CONTRAINDICATED_EGFR_30": "CONTRAINDICATED_EGFR_BELOW_30",
    "USE_CAUTION_EGFR_30": "MONITOR",
    "USE_CAUTION_EGFR_30 (低钙风险)": "MONITOR",
    "USE_CAUTION_EGFR_30 (F-离子)": "MONITOR",
    "USE_CAUTION": "MONITOR",
}

HEPATIC_MAP = {
    "USE_CAUTION_CHILD_PUGH_C": "MONITOR",
    "USE_CAUTION_CHILD_PUGH_B": "MONITOR",
    "USE_CAUTION": "MONITOR",
    "CONTRAINDICATED_CHILD_PUGH_C": "CONTRAINDICATED_IN_SEVERE",
    "CONTRAINDICATED_CHILD_PUGH_B": "CONTRAINDICATED_IN_SEVERE",
    "REDUCE_DOSE_CHILD_PUGH_B": "REDUCE_50_PCT_CHILD_PUGH_B",
}

fixed_renal = 0
fixed_hepatic = 0
invalid_renal = []
invalid_hepatic = []

VALID_RENAL = {"NONE", "MONITOR", "REDUCE_50_PCT_EGFR_BELOW_30", "CONTRAINDICATED_EGFR_BELOW_30"}
VALID_HEPATIC = {"NONE", "MONITOR", "REDUCE_50_PCT_CHILD_PUGH_B", "REDUCE_50_PCT_CHILD_PUGH_C", "CONTRAINDICATED_IN_SEVERE"}

for d in drugs:
    adj = d.get("adjustments", {})
    renal = adj.get("renal", "NONE")
    hepatic = adj.get("hepatic", "NONE")

    if renal in RENAL_MAP:
        new_renal = RENAL_MAP[renal]
        adj["renal"] = new_renal
        fixed_renal += 1
    elif renal not in VALID_RENAL:
        invalid_renal.append((d["id"], renal))
        adj["renal"] = "NONE"

    if hepatic in HEPATIC_MAP:
        new_hepatic = HEPATIC_MAP[hepatic]
        adj["hepatic"] = new_hepatic
        fixed_hepatic += 1
    elif hepatic not in VALID_HEPATIC:
        invalid_hepatic.append((d["id"], hepatic))
        adj["hepatic"] = "NONE"

    d["adjustments"] = adj

print(f"Fixed renal: {fixed_renal}")
print(f"Fixed hepatic: {fixed_hepatic}")
print(f"Invalid renal still: {invalid_renal[:10]}")
print(f"Invalid hepatic still: {invalid_hepatic[:10]}")

# Save
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"\nSaved {V06}")
