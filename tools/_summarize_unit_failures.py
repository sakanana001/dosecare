# -*- coding: utf-8 -*-
"""汇总单测失败: 按测试类聚合 + 打印首个失败的 stacktrace 片段。"""
import glob
import io
import os
import re
import xml.etree.ElementTree as ET
from collections import Counter

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
DIR = os.path.join(REPO, "app", "build", "test-results", "testPlayDebugUnitTest")

per_class = Counter()
first_fail = {}
total = 0
failed = 0
for path in glob.glob(os.path.join(DIR, "*.xml")):
    tree = ET.parse(path)
    root = tree.getroot()
    for tc in root.iter("testcase"):
        total += 1
        fails = list(tc.findall("failure")) + list(tc.findall("error"))
        if not fails:
            continue
        failed += 1
        cls = tc.get("classname", "?")
        per_class[cls] += 1
        if cls not in first_fail:
            msg = (fails[0].get("message") or "")[:600]
            first_fail[cls] = (tc.get("name"), msg)

print("total=%d failed=%d classes_with_failures=%d" % (total, failed, len(per_class)))
print()
for cls, n in per_class.most_common():
    print("%-70s %d" % (cls, n))

print("\n\n========== 各类首个失败消息 ==========")
for cls, (name, msg) in sorted(first_fail.items()):
    print("\n--- %s :: %s" % (cls, name))
    print(msg)
