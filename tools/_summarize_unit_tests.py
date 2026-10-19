#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""统计 unit test 结果, 按失败原因分类 (纯 XML 解析, 不用 PowerShell 的 [xml])。"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

RES = sys.argv[1] if len(sys.argv) > 1 else (
    r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\build\test-results\testPlayDebugUnitTest"
)

files = sorted(glob.glob(os.path.join(RES, "*.xml")))
total = skipped = failures = errors = 0
by_reason = {}
failed = []
for f in files:
    try:
        root = ET.parse(f).getroot()
    except Exception as e:
        print("[WARN] 解析失败 %s: %s" % (os.path.basename(f), e))
        continue
    total += int(root.get("tests", 0))
    skipped += int(root.get("skipped", 0))
    failures += int(root.get("failures", 0))
    errors += int(root.get("errors", 0))
    for tc in root.findall("testcase"):
        node = tc.find("failure")
        if node is None:
            node = tc.find("error")
        if node is None:
            continue
        msg = (node.get("message") or "") or ""
        first = msg.split("\n")[0].strip()
        # 归一化: 去掉路径和变量名
        key = first
        if "Unsupported class file major version" in first:
            key = "ROBOLECTRIC-JDK: Unsupported class file major version (环境: JDK 太新, ASM 太老)"
        elif "Expected model source directory" in first:
            key = "PKG-LEFTOVER: 测试里还写着旧包名 com.picohrt"
        elif "IllegalArgumentException" in first and "ClassReader" in msg:
            key = "ROBOLECTRIC-JDK: ClassReader IllegalArgumentException"
        failed.append((tc.get("classname", "?"), tc.get("name", "?"), key, first[:150]))

for _, _, key, _ in failed:
    by_reason[key] = by_reason.get(key, 0) + 1

print("=" * 78)
print("total=%d  failures=%d  errors=%d  skipped=%d  (suite files=%d)"
      % (total, failures, errors, skipped, len(files)))
print("=" * 78)
print("\n失败原因分布:")
for k, v in sorted(by_reason.items(), key=lambda kv: -kv[1]):
    print("  %4d  %s" % (v, k))

non_robo = [x for x in failed if not x[2].startswith("ROBOLECTRIC-JDK")]
print("\n" + "=" * 78)
print("非 Robolectric-JDK 类失败: %d 个" % len(non_robo))
print("=" * 78)
for cls, name, key, first in non_robo:
    print("\n  [%s]" % cls)
    print("    %s" % name)
    print("    -> %s" % first)
