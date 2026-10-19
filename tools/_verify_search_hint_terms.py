#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
核实搜索提示里的示例词真的能被搜到, 以及目录里确实没有日文名。

对照 SearchableDrugPicker 的 5 条匹配规则:
  1. genericNameZh  中文名
  2. genericName    英文名
  3. brandNames     品牌名
  4. atc            ATC 码
  5. 拼音首字母      (仅纯字母查询时, 把中文名转拼音首字母)
"""
import io
import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")

with io.open(CATALOG, "r", encoding="utf-8") as f:
    data = json.load(f)
drugs = data["drugs"]
print("目录药品数: %d" % len(drugs))

# 1. 目录里有没有日文字符 (平假名/片假名)
KANA = re.compile(r'[\u3040-\u309f\u30a0-\u30ff]')
kana_hits = []
for d in drugs:
    blob = " ".join(filter(None, [
        d.get("genericName", ""), d.get("genericNameZh", ""),
        " ".join(d.get("brandNames") or []),
    ]))
    if KANA.search(blob):
        kana_hits.append((d["id"], blob))
print("含假名的药品条目: %d  -> 目录里没有日文药名" % len(kana_hits))
for i, b in kana_hits[:5]:
    print("   %s: %s" % (i, b[:80]))

# 2. 提示里的示例词逐个验证
TERMS = [
    ("氯氮平", "中文名"),
    ("clozapine", "英文名"),
    ("Clozaril", "品牌名"),
    ("fluvoxamine", "英文名"),
    ("氟", "中文名(氟西汀)"),
    ("F", "ATC 首字母"),
]


def matches(drug, q):
    ql = q.lower()
    if drug.get("genericNameZh", "").lower().find(ql) >= 0:
        return True
    if drug.get("genericName", "").lower().find(ql) >= 0:
        return True
    if any(n.lower().find(ql) >= 0 for n in (drug.get("brandNames") or [])):
        return True
    atc = (drug.get("atc") or "").lower()
    if atc and atc.find(ql) >= 0:
        return True
    return False


print("\n示例词命中检查:")
for term, kind in TERMS:
    hits = [d for d in drugs if matches(d, term)]
    ids = ", ".join(d["id"] for d in hits[:4])
    status = "OK  " if hits else "MISS"
    print("  [%s] %-14s (%-18s) 命中 %3d 个  %s" % (status, term, kind, len(hits), ids))

# 3. 反例: 旧的日语示例词
print("\n旧日语示例词(确认搜不到):")
for term in ["フル", "クロザピン", "フルボキサミン"]:
    hits = [d for d in drugs if matches(d, term)]
    print("  [%s] %-12s 命中 %d 个" % ("OK  " if not hits else "HIT!", term, len(hits)))
