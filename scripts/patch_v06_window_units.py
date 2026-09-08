#!/usr/bin/env python3
"""
v0.6.json catalog patch: 给 32 个有治疗窗的药加 cMaxUnitFactor / skipTherapeuticWindowBand

PicoHrt / PK 教科书浓度单位惯例:
- PkCurve.c (mg/L) 是内部单位
- therapeuticWindow.unit 可能是 ng/mL (精神科常用), mg/L (= μg/mL), mmol/L (锂), INR, ng/dL

cMaxUnitFactor 规则:
- ng/mL (含变体如 "ng/mL (xxx 总浓度)"): 1000.0  (mg/L × 1000 = ng/mL)
- mg/L (含 "mg/L (μg/mL)"): 1.0
- μg/mL (含变体): 1.0
- mmol/L (锂): 0.1441  (1/6.94, 因为 1 mmol 锂 = 6.94 mg)
- INR (华法林): skipTherapeuticWindowBand=true
- ng/dL (T4): skipTherapeuticWindowBand=true
"""
import json
import sys
from pathlib import Path

LITHIUM_FACTOR = 1.0 / 6.94  # 锂: 1 mmol/L = 6.94 mg/L, 反向: 1 mg/L = 0.1441 mmol/L

def patch_drug(d):
    win = d.get("therapeuticWindow")
    if not win:
        return
    unit = win.get("unit", "")
    if "INR" in unit or "ng/dL" in unit:
        d["skipTherapeuticWindowBand"] = True
        d["cMaxUnitFactor"] = 1.0  # 占位, 不画带
    elif "mmol/L" in unit:
        d["cMaxUnitFactor"] = round(LITHIUM_FACTOR, 4)
        d["skipTherapeuticWindowBand"] = False
    elif unit.startswith("ng/mL"):
        d["cMaxUnitFactor"] = 1000.0
        d["skipTherapeuticWindowBand"] = False
    elif unit.startswith("mg/L") or unit.startswith("μg/mL"):
        # 1 mg/L = 1 μg/mL, 数值等价
        d["cMaxUnitFactor"] = 1.0
        d["skipTherapeuticWindowBand"] = False
    else:
        print(f"!! 未识别单位: {d['id']} -> {unit}", file=sys.stderr)
        d["cMaxUnitFactor"] = 1.0
        d["skipTherapeuticWindowBand"] = False

def main():
    p = Path("app/src/main/assets/drugs/v0.6.json")
    raw = p.read_text(encoding="utf-8")
    data = json.loads(raw)
    n = 0
    for d in data["drugs"]:
        if d.get("therapeuticWindow"):
            patch_drug(d)
            n += 1
    print(f"已处理 {n} 个有治疗窗的药")
    p.write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )
    print(f"已写入 {p}")

if __name__ == "__main__":
    main()
