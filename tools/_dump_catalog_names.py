#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出 202 个药的 id / 英文通用名 / 中文通用名, 供补日文通用名。"""
import io
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")

with io.open(CATALOG, "r", encoding="utf-8") as f:
    data = json.load(f)

drugs = data["drugs"]
print("总数: %d" % len(drugs))
print()
for i, d in enumerate(drugs, 1):
    print("%3d | %-28s | %-34s | %s" % (
        i, d["id"], d.get("genericName", ""), d.get("genericNameZh", "")))
