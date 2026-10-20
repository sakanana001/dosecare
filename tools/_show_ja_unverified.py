#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""打印 _ja_name_verification.json 里的未通过清单。"""
import io
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
p = os.path.join(ROOT, "tools", "_ja_name_verification.json")
d = json.load(io.open(p, "r", encoding="utf-8"))
print("通过: %d / 未通过: %d\n" % (len(d["verified"]), len(d["unverified"])))
for u in d["unverified"]:
    print("%-28s | %-30s | %-22s | %-22s [%s]" % (
        u["id"], u["en"], u["zh"], u["candidate"], u["source"]))
