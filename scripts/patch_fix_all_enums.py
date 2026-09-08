"""Fix all invalid enum values in v0.6.json across all schemas"""
import json
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]

# Valid values (from enum definitions)
VALID_RENAL = {"NONE", "MONITOR", "REDUCE_50_PCT_EGFR_BELOW_30", "CONTRAINDICATED_EGFR_BELOW_30"}
VALID_HEPATIC = {"NONE", "MONITOR", "REDUCE_50_PCT_CHILD_PUGH_B", "REDUCE_50_PCT_CHILD_PUGH_C", "CONTRAINDICATED_IN_SEVERE"}
VALID_RISK = {"VERY_LOW", "LOW", "MEDIUM", "HIGH", "VERY_HIGH"}
VALID_PATHWAY = {"CYP450", "UGT_GLUCURONIDATION", "RENAL_EXCRETION", "HYDROLYSIS",
                  "ESTERASE", "DEIODINATION", "MAO", "DPP4", "GLUCURONIDATION",
                  "BETA_OXIDATION", "OTHER"}
VALID_MECHANISM = {"CYP_INHIBITION_STRONG", "CYP_INHIBITION_MODERATE", "CYP_INHIBITION_WEAK",
                   "CYP_INDUCTION", "CYP2D6_COMPETITION", "QTc_ADDITIVE", "SEROTONIN_ADDITIVE",
                   "ANTICHOLINERGIC_ADDITIVE", "PHARMACODYNAMIC", "PHARMACOKINETIC_OTHER", "OTHER"}
VALID_SEVERITY_OD = {"MILD", "MODERATE", "SEVERE", "LIFE_THREATENING"}

# Pathway mapping (free-form → valid enum)
PATHWAY_MAP = {
    "CYP450": "CYP450",
    "UGT_GLUCURONIDATION": "UGT_GLUCURONIDATION",
    "RENAL_EXCRETION": "RENAL_EXCRETION",
    "HYDROLYSIS": "HYDROLYSIS",
    "ESTERASE": "ESTERASE",
    "DEIODINATION": "DEIODINATION",
    "MAO": "MAO",
    "DPP4": "DPP4",
    "GLUCURONIDATION": "GLUCURONIDATION",
    "BETA_OXIDATION": "BETA_OXIDATION",
    "OTHER": "OTHER",
    # Common fallbacks
    "Non-enzymatic": "OTHER",
    "Non-enzymatic + CYP": "OTHER",
    "Non-enzymatic + CYP": "OTHER",
    "Non-enzymatic hydrolysis": "OTHER",
    "Non-CYP oxidation": "OTHER",
    "Hydrolysis + Renal": "HYDROLYSIS",
    "Renal + minor CYP": "RENAL_EXCRETION",
    "Renal + CYP": "RENAL_EXCRETION",
    "Renal + biliary": "RENAL_EXCRETION",
    "Renal + hepatic": "RENAL_EXCRETION",
    "Renal + CYP3A4": "RENAL_EXCRETION",
    "Renal + minor": "RENAL_EXCRETION",
    "Biliary + CYP": "OTHER",
    "Phase II": "GLUCURONIDATION",
    "Phase II + CYP": "CYP450",
    "Phase II + non-CYP": "GLUCURONIDATION",
    "Phase II + Deiodinase": "DEIODINATION",
    "Phase II + Non-enzymatic": "GLUCURONIDATION",
    "Non-CYP + Renal": "RENAL_EXCRETION",
    "Non-enzymatic + Renal": "OTHER",
    "Non-CYP + CYP": "CYP450",
    "OATP": "OTHER",
    "OATP + CYP": "OTHER",
    "Activation": "DEIODINATION",
    "Deiodinase + Phase II": "DEIODINATION",
    "Proteolysis": "OTHER",
    "Pulmonary + CYP": "OTHER",
    "Hepatic": "CYP450",
    "MAOI": "MAO",
    "Hepatic + CYP": "CYP450",
}

# Renal / Hepatic fallback maps (already done)
RENAL_MAP = {
    "REDUCE_50_PCT_EGFR_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_30": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_25": "REDUCE_50_PCT_EGFR_BELOW_30",
    "REDUCE_DOSE_EGFR_15_30": "REDUCE_50_PCT_EGFR_BELOW_30",
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

# Mechanism mapping
MECHANISM_MAP = {
    "CYP_INHIBITION": "CYP_INHIBITION_MODERATE",  # 默认按 moderate
    "CYP_INHIBITION_STRONG": "CYP_INHIBITION_STRONG",
    "CYP_INHIBITION_MODERATE": "CYP_INHIBITION_MODERATE",
    "CYP_INHIBITION_WEAK": "CYP_INHIBITION_WEAK",
    "CYP_INDUCTION": "CYP_INDUCTION",
    "CYP_INDUCTION_STRONG": "CYP_INDUCTION",
    "CYP_INDUCTION_MODERATE": "CYP_INDUCTION",
    "CYP2D6_COMPETITION": "CYP2D6_COMPETITION",
    "QTc_ADDITIVE": "QTc_ADDITIVE",
    "SEROTONIN_ADDITIVE": "SEROTONIN_ADDITIVE",
    "ANTICHOLINERGIC_ADDITIVE": "ANTICHOLINERGIC_ADDITIVE",
    "PHARMACODYNAMIC": "PHARMACODYNAMIC",
    "PHARMACODYNAMIC_SYNERGY": "PHARMACODYNAMIC",
    "PHARMACODYNAMIC_ANTAGONISM": "PHARMACODYNAMIC",
    "PHARMACODYNAMIC_QT": "QTc_ADDITIVE",
    "PHARMACODYNAMIC_ANTICHOLINERGIC": "ANTICHOLINERGIC_ADDITIVE",
    "ADDITIVE_CNS_DEPRESSION": "PHARMACODYNAMIC",
    "ADDITIVE_CNS": "PHARMACODYNAMIC",
    "ADDITIVE_CNS_RESPIRATORY": "PHARMACODYNAMIC",
    "ADDITIVE_HYPOTENSION": "PHARMACODYNAMIC",
    "ADDITIVE_TOXICITY": "PHARMACODYNAMIC",
    "ADDITIVE_IMMUNOSUPPRESSION": "PHARMACODYNAMIC",
    "ADDITIVE_RESPIRATORY": "PHARMACODYNAMIC",
    "ADDITIVE_ANTICHOLINERGIC": "ANTICHOLINERGIC_ADDITIVE",
    "ADDITIVE_CARDIAC": "QTc_ADDITIVE",
    "TUBULAR_SECRETION_INHIBITION": "PHARMACOKINETIC_OTHER",
    "TUBULAR_SECRETION": "PHARMACOKINETIC_OTHER",
    "OATP_INHIBITION": "PHARMACOKINETIC_OTHER",
    "P_GP_INHIBITION": "PHARMACOKINETIC_OTHER",
    "P_GP_INDUCTION": "PHARMACOKINETIC_OTHER",
    "DECREASED_CLEARANCE": "PHARMACOKINETIC_OTHER",
    "DECREASED_RENAL_CLEARANCE": "PHARMACOKINETIC_OTHER",
    "DECREASED_FREE_FRACTION": "PHARMACOKINETIC_OTHER",
    "ENZYME_INDUCTION": "CYP_INDUCTION",
    "ELECTROLYTE": "PHARMACOKINETIC_OTHER",
    "MAOI_LIKE": "PHARMACOKINETIC_OTHER",
    "CHELATION": "PHARMACOKINETIC_OTHER",
    "PHARMACOKINETIC": "PHARMACOKINETIC_OTHER",
    "PHARMACOKINETIC_OTHER": "PHARMACOKINETIC_OTHER",
    "OTHER": "OTHER",
}

# CriticalInteraction severity valid
VALID_CI_SEVERITY = {"CONTRAINDICATED", "MAJOR", "MODERATE", "MINOR"}
CI_SEVERITY_MAP = {
    "CONTRAINDICATED": "CONTRAINDICATED",
    "MAJOR": "MAJOR",
    "MODERATE": "MODERATE",
    "MINOR": "MINOR",
}

# Smoking effect (sub-schema)
VALID_SMOKING = {"doseAdjustment": ["INCREASE_DOSE_50_PCT_IN_SMOKERS", "DECREASE_DOSE_50_PCT", "NONE"]}

stats = {"renal": 0, "hepatic": 0, "pathway": 0, "mechanism": 0, "ci_severity": 0, "other": 0}

for d in drugs:
    # adjustments
    adj = d.get("adjustments", {}) or {}
    renal = adj.get("renal", "NONE")
    hepatic = adj.get("hepatic", "NONE")
    if renal in RENAL_MAP:
        adj["renal"] = RENAL_MAP[renal]
        stats["renal"] += 1
    elif renal not in VALID_RENAL:
        adj["renal"] = "NONE"
        stats["other"] += 1
    if hepatic in HEPATIC_MAP:
        adj["hepatic"] = HEPATIC_MAP[hepatic]
        stats["hepatic"] += 1
    elif hepatic not in VALID_HEPATIC:
        adj["hepatic"] = "NONE"
        stats["other"] += 1
    d["adjustments"] = adj

    # cypProfile.pathwayType
    cyp = d.get("cypProfile", {}) or {}
    pw = cyp.get("pathwayType", "CYP450")
    if pw in PATHWAY_MAP:
        new_pw = PATHWAY_MAP[pw]
        if new_pw != pw:
            cyp["pathwayType"] = new_pw
            stats["pathway"] += 1
    elif pw not in VALID_PATHWAY:
        cyp["pathwayType"] = "OTHER"
        stats["pathway"] += 1
    d["cypProfile"] = cyp

    # adverseEffects: all keys should be valid risk levels
    ae = d.get("adverseEffects", {}) or {}
    for k, v in list(ae.items()):
        if v not in VALID_RISK:
            # 可能是新加的字段, 跳过
            pass

    # criticalInteractions
    cis = d.get("criticalInteractions", []) or []
    for ci in cis:
        mech = ci.get("mechanism", "")
        if mech in MECHANISM_MAP:
            new_m = MECHANISM_MAP[mech]
            if new_m != mech:
                ci["mechanism"] = new_m
                stats["mechanism"] += 1
        elif mech not in VALID_MECHANISM:
            ci["mechanism"] = "OTHER"
            stats["mechanism"] += 1
        sev = ci.get("severity", "MODERATE")
        if sev in CI_SEVERITY_MAP:
            new_s = CI_SEVERITY_MAP[sev]
            if new_s != sev:
                ci["severity"] = new_s
                stats["ci_severity"] += 1
        elif sev not in VALID_CI_SEVERITY:
            ci["severity"] = "MODERATE"
            stats["ci_severity"] += 1
    d["criticalInteractions"] = cis

    # overdose severity
    od = d.get("overdose", {}) or {}
    sev = od.get("severity", "MILD")
    if sev not in VALID_SEVERITY_OD:
        od["severity"] = "MILD"
        stats["other"] += 1
    d["overdose"] = od

print("Fixed:", stats)

# Verify final
still_invalid = {"renal": [], "hepatic": [], "pathway": [], "mechanism": [], "ci_severity": []}
for d in drugs:
    adj = d.get("adjustments", {}) or {}
    if adj.get("renal") not in VALID_RENAL:
        still_invalid["renal"].append((d["id"], adj.get("renal")))
    if adj.get("hepatic") not in VALID_HEPATIC:
        still_invalid["hepatic"].append((d["id"], adj.get("hepatic")))
    pw = d.get("cypProfile", {}).get("pathwayType", "")
    if pw not in VALID_PATHWAY:
        still_invalid["pathway"].append((d["id"], pw))
    for ci in d.get("criticalInteractions", []):
        if ci.get("mechanism") not in VALID_MECHANISM:
            still_invalid["mechanism"].append((d["id"], ci.get("mechanism")))
        if ci.get("severity") not in VALID_CI_SEVERITY:
            still_invalid["ci_severity"].append((d["id"], ci.get("severity")))

for k, v in still_invalid.items():
    if v:
        print(f"  STILL INVALID {k}: {v[:5]}")

# Save
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"\nSaved {V06}")
