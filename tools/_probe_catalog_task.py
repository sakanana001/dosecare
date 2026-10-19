# -*- coding: utf-8 -*-
"""One-off probe: count drugs in merged pharmacology catalog + inspect structure."""
import io
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
p = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")
with io.open(p, encoding="utf-8") as f:
    data = json.load(f)

print("top-level type:", type(data).__name__)
if isinstance(data, dict):
    for k, v in data.items():
        print("  key=%-24s type=%-6s len=%s" % (k, type(v).__name__, len(v) if isinstance(v, (list, dict)) else "-"))
    drugs = data.get("drugs") or data.get("catalog") or []
else:
    drugs = data

print("drug count:", len(drugs))
if drugs:
    d0 = drugs[0]
    print("first drug keys:", sorted(d0.keys()))
    cats = set()
    for d in drugs:
        c = d.get("category")
        if isinstance(c, dict):
            c = c.get("name") or c.get("value")
        cats.add(c)
    print("distinct categories:", len(cats))
    qtc = sum(1 for d in drugs if (d.get("adverseEffects") or {}).get("qtcProlongation") not in (None, "LOW", "VERY_LOW"))
    acb = sum(1 for d in drugs if (d.get("adverseEffects") or {}).get("anticholinergicLoad", 0))
    print("drugs with QTc>LOW:", qtc, " with ACB>0:", acb)
    print("sample names:", [d.get("genericName") for d in drugs[:8]])
