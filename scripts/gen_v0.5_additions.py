"""
v0.5 汇总脚本:
- 从 gen_v0.5_part1.py 读 25 精麻药 (BATCH1)
- 生成 v0.5-additions.json
- 自动合并到 v0.4.json -> v0.5.json

注意: BATCH1 用 int 字段名 (id/m/af/s/n) 已替换为 schema 字段名 (triggerDrugId/mechanism/aucFoldChange/severity/clinicalNote)
"""
import math
import json
import sys
import importlib.util
from pathlib import Path

# 文件名带 .5 触犯 Python import 标识符规则, 用 importlib 显式加载
scripts_dir = Path(__file__).parent
def _load(name):
    spec = importlib.util.spec_from_file_location(name, scripts_dir / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

_part1 = _load("gen_v0.5_part1")
BATCH1 = _part1.BATCH1  # 25 精麻药


def make_drug(d):
    """将紧凑 dict 转换为完整 JSON entry. 同 v0.4 字段名映射. """
    pk = d["pk"]
    ke = pk.get("ke", math.log(2) / pk["t12"])
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


ALL_DRUGS = BATCH1
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

# 生成 v0.5-additions.json
out_dir = Path(__file__).parent.parent / "data" / "drugs"
out_dir.mkdir(parents=True, exist_ok=True)
out_file = out_dir / "v0.5-additions.json"

output = {
    "schemaVersion": "0.5.0",
    "generatedAt": "2026-09-07T11:35:00Z",
    "source": [
        "国家药监局《麻醉药品和精神药品品种目录》(2013 版 + 后续增补)",
        "FDA DailyMed (SPL) - 药品标签 PK / CYP / 相互作用",
        "AGNP Consensus 2017 (Hiemke C. et al., Pharmacopsychiatry 2018) - 精神科 TDM 共识",
        "Flockhart CYP Drug Interaction Table (Indiana University)",
        "PMID 同行评议文献"
    ],
    "disclaimer": "本目录的所有数据基于公开文献与药品说明书,仅供参考。中国精麻目录品种根据国家药品监督管理局公告。个体差异显著。任何用药调整请咨询医师或临床药师。",
    "drugs": [make_drug(d) for d in ALL_DRUGS]
}

with open(out_file, "wb") as f:
    f.write(json.dumps(output, ensure_ascii=False, indent=2).encode("utf-8"))
print(f"[OK] wrote {out_file} ({out_file.stat().st_size:,} bytes)")

# === 自动合并到 v0.4.json -> v0.5.json ===
v04 = out_dir / "v0.4.json"
v05 = out_dir / "v0.5.json"
if v04.exists():
    # v0.4.json 是 UTF-8 (Python 写), 但保险起见用 utf-8-sig
    with open(v04, "r", encoding="utf-8-sig") as f:
        v4_data = json.load(f)
    v4_data["schemaVersion"] = "0.5.0"
    v4_data["generatedAt"] = "2026-09-07T11:35:00Z"
    v4_data["source"] = list(set(v4_data.get("source", []) + output["source"]))
    # 去重: 新药 id 已存在的覆盖, 不存在的追加
    existing_ids = {d["id"] for d in v4_data["drugs"]}
    new_additions = [d for d in output["drugs"] if d["id"] not in existing_ids]
    v4_data["drugs"] = v4_data["drugs"] + new_additions
    with open(v05, "wb") as f:
        f.write(json.dumps(v4_data, ensure_ascii=False, indent=2).encode("utf-8"))
    print(f"[OK] merged {v04} + {len(new_additions)} new -> {v05} ({v05.stat().st_size:,} bytes, total {len(v4_data['drugs'])} drugs)")
