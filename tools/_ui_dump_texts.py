# -*- coding: utf-8 -*-
"""打印当前页面上所有有文字的节点。"""
import io, re, subprocess, sys, xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
ADB = r"C:\Users\yuwen\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5554"
def adb(*a, binary=False):
    cmd = [ADB, "-s", SERIAL] + list(a)
    if binary:
        return subprocess.run(cmd, capture_output=True).stdout
    return subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace").stdout
raw = b""
for _ in range(3):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    raw = adb("shell", "cat", "/sdcard/ui.xml", binary=True)
    if b"<hierarchy" in raw:
        break
root = ET.fromstring(raw.decode("utf-8", errors="replace"))
for el in root.iter("node"):
    t = (el.get("text") or "").strip()
    d = (el.get("content-desc") or "").strip()
    if t or d:
        b = re.findall(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", el.get("bounds") or "")
        pos = ""
        if b:
            x1, y1, x2, y2 = map(int, b[0])
            pos = "(%d,%d)" % ((x1 + x2) // 2, (y1 + y2) // 2)
        print("%-14s %s" % (pos, (t or d)[:90]))
