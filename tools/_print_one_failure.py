# -*- coding: utf-8 -*-
"""打印一个 Robolectric 单测失败的完整 stacktrace, 定位 'major version 69' 的来源。"""
import glob
import io
import os
import xml.etree.ElementTree as ET

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
DIR = os.path.join(REPO, "app", "build", "test-results", "testPlayDebugUnitTest")

TARGET = "com.dosecare.app.ui.components.StockUnitResourcesTest"
for path in glob.glob(os.path.join(DIR, "*.xml")):
    root = ET.parse(path).getroot()
    for tc in root.iter("testcase"):
        fails = list(tc.findall("failure")) + list(tc.findall("error"))
        if not fails:
            continue
        print("=" * 90)
        print("class:", tc.get("classname"))
        print("test :", tc.get("name"))
        print("=" * 90)
        txt = fails[0].text or ""
        print(txt[:4000])
        print()
        break
    else:
        continue
    break
