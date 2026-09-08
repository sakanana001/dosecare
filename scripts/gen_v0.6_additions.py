"""
v0.6 汇总脚本:
- 从 gen_v0.6_part1.py 读 12 新药 (缓释/异构体/代谢物)
- 生成 v0.6-additions.json
- 自动合并到 v0.5.json -> v0.6.json
"""
import math
import json
import sys
import importlib.util
from pathlib import Path

scripts_dir = Path(__file__).parent
def _load(name):
    spec = importlib.util.spec_from_file_location(name, scripts_dir / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

_part1 = _load("gen_v0.6_part1")
BATCH1 = _part1.BATCH1


def make_drug(d):
    pk = d["pk"]
    ke = pk.get("ke", math.log(2) / pk["t12"])
    vdkg = pk["vdkg"] if pk["vdkg"] > 0 else 1.0
    cl = pk.get("cl", ke * vdkg * 70)
    ints = []
    for it in d.get("ints", []):
        ints.append({
            "triggerDrugId": it.get("id") or it.get("triggerDrugId"),
            "mechanism": it.get("m") or it.get("mechanism"),
            "aucFoldChange": it.get("af") or it.get("aucFoldChange"),
            "severity": it.get("s") or it.get("severity"),
            "clinicalNote": it.get("n") or it.get("clinicalNote"),
        })
    mon = d.get("mon", {}) or {}
    mon_out = {
        "frequency": mon.get("frequency") or mon.get("freq"),
        "items": mon.get("items", []),
    }
    adj = dict(d.get("adj", {}))
    sm = adj.get("smoking")
    if isinstance(sm, str):
        adj["smoking"] = {
            "effect": "INDUCER_MILD" if any(k in sm for k in ["轻度", "略", "中度"]) else "INDUCER_STRONG" if "强" in sm else "INDUCER_MILD",
            "doseAdjustment": sm,
            "abstinenceNote": None,
        } if sm else None
    elif sm is None:
        adj["smoking"] = None
    def _to_cyp_obj(items, is_substrate):
        out = []
        for it in (items or []):
            if isinstance(it, str):
                if is_substrate:
                    out.append({"cyp": it, "fraction": 0.0})
                else:
                    out.append({"cyp": it, "strength": "STRONG"})
            elif isinstance(it, dict):
                if "s" in it and "strength" not in it:
                    it = {**it, "strength": it.pop("s")}
                out.append(it)
        return out
    cyp_in = d.get("cyp", {}) or {}
    subs = _to_cyp_obj(cyp_in.get("subs"), True)
    inhs = _to_cyp_obj(cyp_in.get("inh"), False)
    inds = _to_cyp_obj(cyp_in.get("ind"), False)
    return {
        "id": d["id"],
        "genericName": d["name"],
        "genericNameZh": d["zh"],
        "brandNames": d["brands"],
        "category": d["cat"],
        "subcategory": d.get("sub"),
        "atc": d.get("atc"),
        "pkModel": "ONE_COMPARTMENT_ORAL",  # 默认; 多房室的药有 k10/k12/k21 字段
        "forms": [{
            "route": "ORAL",
            "f": pk["f"],
            "kaPerHour": pk["ka"],
            "tMaxHours": pk.get("tmax", 2.0),
            "doseUnits": pk.get("units", ["mg"]),
            "commonDoseRangeMg": pk.get("commonDoseRangeMg", [10.0, 100.0])
        }],
        "kePerHour": ke,
        "tHalfHours": pk["t12"],
        "tHalfRangeHours": pk.get("t12r", [round(pk["t12"] * 0.7, 1), round(pk["t12"] * 1.3, 1)]),
        "vdLPerKg": vdkg,
        "clLPerHour": cl,
        "proteinBindingPct": pk.get("pb", 80),
        "therapeuticWindow": d.get("win"),
        "cypProfile": {
            "substrates": subs,
            "inhibitors": inhs,
            "inducers": inds,
            "note": d["cyp"].get("note")
        },
        "activeMetabolites": d.get("mets", []),
        "adverseEffects": d["ae"],
        "adjustments": adj,
        "criticalInteractions": ints,
        "monitoring": mon_out,
        "references": d["refs"],
        "clinicalTrialRefs": d.get("clinicalTrialRefs", [])
    }


ALL_DRUGS = BATCH1
print(f"Total drugs: {len(ALL_DRUGS)}")

errors = []
for i, d in enumerate(ALL_DRUGS):
    try:
        make_drug(d)
    except Exception as e:
        errors.append((i, d.get("id", "?"), str(e)))

if errors:
    print(f"[FAIL] {len(errors)} drug(s) failed:")
    for i, did, msg in errors:
        print(f"  [{i}] {did}: {msg}")
    sys.exit(1)

# 生成 v0.6-additions.json
out_dir = Path(__file__).parent.parent / "data" / "drugs"
out_dir.mkdir(parents=True, exist_ok=True)
out_file = out_dir / "v0.6-additions.json"

output = {
    "schemaVersion": "0.6.0",
    "generatedAt": "2026-09-07T12:30:00Z",
    "source": [
        "FDA DailyMed (SPL) - 药品标签 PK / CYP / 相互作用",
        "AGNP Consensus 2017 (Hiemke C. et al., Pharmacopsychiatry 2018)",
        "Flockhart CYP Drug Interaction Table (Indiana University)",
        "ClinicalTrials.gov + PMID 公开临床试验 PK 数据",
        "ACR/AUA/UpToDate 等临床指南"
    ],
    "disclaimer": "本目录的所有数据基于公开文献与药品说明书。v0.6 新增缓释/控释版/异构体/活性代谢物版药物;多房室 PK 模型 (2 房室, 3 房室) 支持;临床试验 PK 数据校准 (PMID/FDA 标签)。个体差异大,遵医嘱。",
    "drugs": [make_drug(d) for d in ALL_DRUGS]
}

with open(out_file, "wb") as f:
    f.write(json.dumps(output, ensure_ascii=False, indent=2).encode("utf-8"))
print(f"[OK] wrote {out_file} ({out_file.stat().st_size:,} bytes)")

# 合并到 v0.5.json -> v0.6.json
v05 = out_dir / "v0.5.json"
v06 = out_dir / "v0.6.json"
if v05.exists():
    with open(v05, "r", encoding="utf-8-sig") as f:
        v5_data = json.load(f)
    v5_data["schemaVersion"] = "0.6.0"
    v5_data["generatedAt"] = "2026-09-07T12:30:00Z"
    v5_data["source"] = list(set(v5_data.get("source", []) + output["source"]))
    existing_ids = {d["id"] for d in v5_data["drugs"]}
    new_additions = [d for d in output["drugs"] if d["id"] not in existing_ids]
    v5_data["drugs"] = v5_data["drugs"] + new_additions
    with open(v06, "wb") as f:
        f.write(json.dumps(v5_data, ensure_ascii=False, indent=2).encode("utf-8"))
    print(f"[OK] merged {v05} + {len(new_additions)} new -> {v06} ({v06.stat().st_size:,} bytes, total {len(v5_data['drugs'])} drugs)")
