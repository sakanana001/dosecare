#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
核实 README 里写的几个事实性说法, 防止文档夸大:
  1. "31 pharmacological categories"  —— catalog.json 里实际的 category 数
  2. "202-entry drug catalog"            —— 药品数
  3. "therapeutic-window deviation and renal/hepatic dose adjustment in the
     same pass" —— RuleEngine 是否真的检查这两项
  4. Severity 分级是否真是 LOW/MEDIUM/HIGH/CONTRAINDICATED
"""
import collections
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")
PKG = os.path.join(ROOT, "app", "src", "main", "java", "com", "dosecare", "app")

with io.open(CATALOG, "r", encoding="utf-8") as f:
    data = json.load(f)
drugs = data.get("drugs", [])

cats = collections.Counter(d.get("category") for d in drugs)
print("药品数: %d" % len(drugs))
print("category 数: %d" % len(cats))
print("  " + ", ".join(sorted(cats.keys())))
print()

subs = collections.Counter(d.get("subcategory") for d in drugs if d.get("subcategory"))
print("subcategory 数: %d" % len(subs))

win = [d for d in drugs if d.get("therapeuticWindow")]
print("有治疗窗的药: %d" % len(win))

print()
print("=== RuleEngine 实际检查项 ===")
re_path = os.path.join(PKG, "model", "pharmacology", "RuleEngine.kt")
if os.path.exists(re_path):
    with io.open(re_path, "r", encoding="utf-8") as f:
        t = f.read()
    # 找产出的 Interaction 类型
    kinds = sorted(set(re.findall(r'\b(AnticholinergicLoadInteraction|CypInteraction|QtcInteraction|SerotoninSyndromeRisk|OverdoseSeverity)\b', t)))
    print("  产出的交互类型: %s" % ", ".join(kinds))
    for kw in ("therapeuticWindow", "renal", "hepatic", "adjustments"):
        print("  提到 %-18s %d 次" % (kw, len(re.findall(kw, t))))
else:
    print("  [MISSING] %s" % re_path)

print()
print("=== Severity 枚举值 ===")
for name in ("Severity", "OverdoseSeverity"):
    p = os.path.join(PKG, "model", "pharmacology", "DrugCatalog.kt")
    with io.open(p, "r", encoding="utf-8") as f:
        t = f.read()
    m = re.search(r'enum class %s\s*\{([^}]*)\}' % name, t)
    if m:
        vals = [v.strip() for v in m.group(1).split(",") if v.strip()]
        print("  %-16s %s" % (name, vals))
