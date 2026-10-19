# -*- coding: utf-8 -*-
"""One-off probe: catalog coverage facts + ja locale key prefixes."""
import io
import json
import os
import re
from collections import Counter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

with io.open(os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json"), encoding="utf-8") as f:
    cat = json.load(f)
drugs = cat["drugs"]
print("schemaVersion:", cat.get("schemaVersion"), "generatedAt:", cat.get("generatedAt"))
print("source list (%d):" % len(cat.get("source", [])))
for s in cat.get("source", []):
    print("   -", s)
print("disclaimer:", (cat.get("disclaimer") or "")[:200])

def nonempty_cyp(d):
    c = d.get("cypProfile") or {}
    return any(c.get(k) for k in ("substrates", "inhibitors", "inducers", "primaryPathway"))

print("\ntotal drugs:", len(drugs))
print("with non-empty cypProfile:", sum(1 for d in drugs if nonempty_cyp(d)))
print("with therapeuticWindow:", sum(1 for d in drugs if d.get("therapeuticWindow")))
print("with indicationGroups:", sum(1 for d in drugs if d.get("indicationGroups")))
print("with overdose:", sum(1 for d in drugs if d.get("overdose")))
print("with monitoring:", sum(1 for d in drugs if d.get("monitoring")))
print("with criticalInteractions:", sum(1 for d in drugs if d.get("criticalInteractions")))
print("with pharmacology text:", sum(1 for d in drugs if d.get("pharmacology")))
print("with proteinBindingPct>0:", sum(1 for d in drugs if (d.get("proteinBindingPct") or 0) > 0))
pkmodels = Counter()
for d in drugs:
    m = d.get("pkModel")
    if isinstance(m, dict):
        m = m.get("type") or m.get("model") or "?"
    pkmodels[m] += 1
print("pkModel kinds:", dict(pkmodels))
print("indication group count:", len({g for d in drugs for g in (d.get("indicationGroups") or [])}))

# ja locale key prefixes
RES = os.path.join(ROOT, "app", "src", "main", "res")
KEY_RE = re.compile(r'<(?:string|plurals|string-array)\s+name="([^"]+)"')
with io.open(os.path.join(RES, "values-ja", "strings.xml"), encoding="utf-8") as f:
    ja = KEY_RE.findall(f.read())
prefixes = Counter(k.split("_")[0] for k in ja)
print("\nja keys: %d" % len(ja))
print("ja prefixes:", dict(prefixes))
