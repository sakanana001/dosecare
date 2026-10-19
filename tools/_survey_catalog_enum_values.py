#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
统计 202 药目录里 adjustments / monitoring 的枚举值全集, 以及它们在 UI 里
当前是怎么显示的 —— 用来决定要补哪些本地化 label。
"""
import collections
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")

with io.open(CATALOG, "r", encoding="utf-8") as f:
    data = json.load(f)

drugs = data.get("drugs", [])
print("药品数: %d" % len(drugs))

renal = collections.Counter()
hepatic = collections.Counter()
freq = collections.Counter()
smoke_effect = collections.Counter()
smoke_adj = collections.Counter()
elderly = collections.Counter()
pathway = collections.Counter()

for d in drugs:
    adj = d.get("adjustments") or {}
    renal[adj.get("renal", "NONE")] += 1
    hepatic[adj.get("hepatic", "NONE")] += 1
    elderly[adj.get("elderly")] += 1
    sm = adj.get("smoking")
    if sm:
        smoke_effect[sm.get("effect")] += 1
        smoke_adj[sm.get("doseAdjustment")] += 1
    mon = d.get("monitoring") or {}
    freq[mon.get("frequency")] += 1
    cyp = d.get("cypProfile") or {}
    pathway[cyp.get("pathwayType")] += 1

print()
print("=== renal ===")
for k, v in renal.most_common():
    print("  %-40s %d" % (k, v))
print("=== hepatic ===")
for k, v in hepatic.most_common():
    print("  %-40s %d" % (k, v))
print("=== monitoring.frequency ===")
for k, v in freq.most_common():
    print("  %-40s %d" % (k, v))
print("=== smoking.effect ===")
for k, v in smoke_effect.most_common():
    print("  %-40s %d" % (k, v))
print("=== smoking.doseAdjustment ===")
for k, v in smoke_adj.most_common():
    print("  %-40s %d" % (k, v))
print("=== adjustments.elderly (前 12) ===")
for k, v in elderly.most_common(12):
    print("  %-40s %d" % (k, v))
print("=== cypProfile.pathwayType ===")
for k, v in pathway.most_common():
    print("  %-40s %d" % (k, v))
