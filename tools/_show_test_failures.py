#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""打印指定测试类的完整失败详情 (含 failure body)。"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

RES = sys.argv[1]
WANT = sys.argv[2:] if len(sys.argv) > 2 else []

for f in sorted(glob.glob(os.path.join(RES, "*.xml"))):
    try:
        root = ET.parse(f).getroot()
    except Exception:
        continue
    cn = root.get("name", "")
    if WANT and not any(w in cn for w in WANT):
        continue
    for tc in root.findall("testcase"):
        node = tc.find("failure")
        if node is None:
            node = tc.find("error")
        if node is None:
            continue
        print("=" * 78)
        print("[%s] %s" % (cn, tc.get("name")))
        body = (node.text or node.get("message") or "")
        # 只保留前 40 行, 去掉重复的堆栈
        lines = [ln for ln in body.splitlines() if not ln.strip().startswith("at ")]
        print("\n".join(lines[:40]))
        print()
