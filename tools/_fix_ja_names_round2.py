#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按 KEGG 日文版交叉验证的结果, 修正 3 条日文通用名。"""
import io
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
p = os.path.join(ROOT, "tools", "_ja_drug_titles.json")

FIX = {
    # KEGG 一般名: エスリカルバゼピン (USAN/INN)
    "eslicarbazepine": "エスリカルバゼピン",
    # KEGG 一般名: 塩酸レバルブテロール -> 通用名本体
    "levalbuterol": "レバルブテロール",
    # 复方: 左旋多巴 + 卡比多巴
    "levodopa_carbidopa": "レボドパ／カルビドパ",
}

rows = json.load(io.open(p, "r", encoding="utf-8"))
n = 0
for r in rows:
    if r["id"] in FIX:
        old = r.get("ja")
        r["ja"] = FIX[r["id"]]
        r["ja_source"] = "manual+kegg"
        r["ja_prev"] = old
        n += 1
        print("  %-24s %s -> %s" % (r["id"], old, FIX[r["id"]]))

with io.open(p, "w", encoding="utf-8") as f:
    json.dump(rows, f, ensure_ascii=False, indent=2)
print("修正 %d 条" % n)
print("仍缺: %s" % [r["id"] for r in rows if not r.get("ja")])
