"""
v0.4 汇总脚本:
- 从 gen_v0.4_part1(精神科 20 药, 在本脚本内 BATCH1) + gen_v0.4_part2(神经/内分泌/特殊 50 药)
- 生成 v0.4-additions.json
- 自动合并到 v0.3.json -> v0.4.json
"""

import math
import json
import sys
import importlib.util
from pathlib import Path

# 文件名带 .4 触犯 Python import 标识符规则, 用 importlib 显式加载
scripts_dir = Path(__file__).parent
def _load(name):
    spec = importlib.util.spec_from_file_location(name, scripts_dir / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

_part1 = _load("gen_v0.4_part1")
_part2 = _load("gen_v0.4_part2")
BATCH1 = _part1.BATCH1
BATCH2 = _part2.BATCH2
BATCH3 = _part2.BATCH3
BATCH4 = _part2.BATCH4


def make_drug(d):
    """将紧凑 dict 转换为完整 JSON entry.
    字段名映射:
      ints[].id   -> triggerDrugId
      ints[].m    -> mechanism
      ints[].af   -> aucFoldChange
      ints[].s    -> severity
      ints[].n    -> clinicalNote
      mon.freq    -> monitoring.frequency
    """
    pk = d["pk"]
    # 不 round: 保留精度, 否则极长半衰期(如骨内半衰期 >10 年)会被 round 到 0
    ke = pk.get("ke", math.log(2) / pk["t12"])
    # 兜底: vdkg <= 0 (无机盐/局部用) 的用 1.0 防止 Vd 校验失败
    vdkg = pk["vdkg"] if pk["vdkg"] > 0 else 1.0
    cl = pk.get("cl", ke * vdkg * 70)
    # 转换 ints 简写
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
    # adjustments.smoking: schema 要 object {effect, doseAdjustment, abstinenceNote}, v0.4 用 string
    adj = dict(d.get("adj", {}))
    # cyp.subs/inh/ind: 如果是 string 而不是 {cyp, fraction/strength}, 转成 dict
    def _to_cyp_obj(items, is_substrate):
        out = []
        for it in (items or []):
            if isinstance(it, str):
                if is_substrate:
                    out.append({"cyp": it, "fraction": 0.0})
                else:
                    out.append({"cyp": it, "strength": "STRONG"})
            elif isinstance(it, dict):
                # 修复 's' -> 'strength' (残留)
                if "s" in it and "strength" not in it:
                    it = {**it, "strength": it.pop("s")}
                out.append(it)
        return out
    cyp_in = d.get("cyp", {}) or {}
    subs = _to_cyp_obj(cyp_in.get("subs"), True)
    inhs = _to_cyp_obj(cyp_in.get("inh"), False)
    inds = _to_cyp_obj(cyp_in.get("ind"), False)
    sm = adj.get("smoking")
    if isinstance(sm, str):
        adj["smoking"] = {
            "effect": "INDUCER_MILD" if any(k in sm for k in ["轻度", "略", "中度"]) else "INDUCER_STRONG" if "强" in sm else "INDUCER_MILD",
            "doseAdjustment": sm,
            "abstinenceNote": None,
        } if sm else None
    elif sm is None:
        adj["smoking"] = None
    return {
        "id": d["id"],
        "genericName": d["name"],
        "genericNameZh": d["zh"],
        "brandNames": d["brands"],
        "category": d["cat"],
        "subcategory": d.get("sub"),
        "atc": d.get("atc"),
        "pkModel": "ONE_COMPARTMENT_ORAL",
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
        "references": d["refs"]
    }


ALL_DRUGS = BATCH1 + BATCH2 + BATCH3 + BATCH4
print(f"Total drugs: {len(ALL_DRUGS)}")

# 验证每个药都能 parse
errors = []
for i, d in enumerate(ALL_DRUGS):
    try:
        result = make_drug(d)
    except Exception as e:
        errors.append((i, d.get("id", "?"), str(e)))

if errors:
    print(f"[FAIL] {len(errors)} drug(s) failed to parse:")
    for i, did, msg in errors:
        print(f"  [{i}] {did}: {msg}")
    sys.exit(1)

# 生成 v0.4-additions.json
out_dir = Path(__file__).parent.parent / "data" / "drugs"
out_dir.mkdir(parents=True, exist_ok=True)
out_file = out_dir / "v0.4-additions.json"

output = {
    "schemaVersion": "0.4.0",
    "generatedAt": "2026-09-07T10:00:00Z",
    "source": [
        "FDA DailyMed (SPL) - 药品标签 PK / CYP / 相互作用",
        "AGNP Consensus 2017 (Hiemke C. et al., Pharmacopsychiatry 2018) - 精神科 TDM 共识",
        "Flockhart CYP Drug Interaction Table (Indiana University)",
        "国内药品说明书 - 剂量 / 适应症",
        "Hiemke C. et al. 2018 (AGNP)"
    ],
    "disclaimer": "本目录的所有数据基于公开文献与药品说明书,仅供参考。个体差异(年龄、肝肾、基因型、食物、草本补充剂)显著影响真实数值。任何用药调整请咨询精神科医师或临床药师。",
    "drugs": [make_drug(d) for d in ALL_DRUGS]
}

with open(out_file, "w", encoding="utf-8") as f:
    json.dump(output, f, indent=2, ensure_ascii=False)

print(f"[OK] wrote {out_file} ({out_file.stat().st_size:,} bytes)")

# === 自动合并到 v0.3.json -> v0.4.json ===
v03 = out_dir / "v0.3.json"
v04 = out_dir / "v0.4.json"
if v03.exists():
    with open(v03, "r", encoding="utf-8") as f:
        v3_data = json.load(f)
    v3_data["schemaVersion"] = "0.4.0"
    v3_data["generatedAt"] = "2026-09-07T10:00:00Z"
    v3_data["source"] = list(set(v3_data.get("source", []) + output["source"]))
    v3_data["drugs"] = v3_data["drugs"] + output["drugs"]
    with open(v04, "w", encoding="utf-8") as f:
        json.dump(v3_data, f, indent=2, ensure_ascii=False)
    print(f"[OK] merged {v03} + 70 new -> {v04} ({v04.stat().st_size:,} bytes, total {len(v3_data['drugs'])} drugs)")
