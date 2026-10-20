# -*- coding: utf-8 -*-
"""统计单测结果: 总数 / 失败数 / 跳过数。"""
import glob
import os
import xml.etree.ElementTree as ET

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
DIR = os.path.join(REPO, "app", "build", "test-results", "testPlayDebugUnitTest")

total = failed = skipped = 0
classes = 0
bad = []
for path in glob.glob(os.path.join(DIR, "*.xml")):
    root = ET.parse(path).getroot()
    classes += 1
    for tc in root.iter("testcase"):
        total += 1
        fails = list(tc.findall("failure")) + list(tc.findall("error"))
        if fails:
            failed += 1
            bad.append("%s :: %s" % (tc.get("classname"), tc.get("name")))
        if tc.find("skipped") is not None:
            skipped += 1

print("classes=%d tests=%d failed=%d skipped=%d" % (classes, total, failed, skipped))
for b in bad[:20]:
    print("  FAIL", b)
