#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
uiautomator dump 辅助: 按文本查找控件并输出点击坐标。

用法:
  python _ui_tap.py find "<文本>"          # 打印匹配到的所有节点的中心坐标 + 文本
  python _ui_tap.py tap  "<文本>"          # 直接点击第一个匹配节点
  python _ui_tap.py dump  "<输出xml路径>"  # 只 dump

注意: 文本匹配是 "包含" 关系; 多个节点都匹配时按屏幕从上到下排序。
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ADB = r"C:\Users\yuwen\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "emulator-5554"
PKG = "com.dosecare.app.debug"


def adb(*args, binary=False):
    cmd = [ADB, "-s", SERIAL] + list(args)
    if binary:
        return subprocess.run(cmd, capture_output=True).stdout
    return subprocess.run(cmd, capture_output=True, text=True,
                          encoding="utf-8", errors="replace").stdout


def dump_xml():
    """dump 到 /sdcard/ui.xml 再 pull, 比直接 stdout 更稳 (stdout 会被杂字符污染)。"""
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        raw = adb("shell", "cat", "/sdcard/ui.xml", binary=True)
        if raw and b"<hierarchy" in raw:
            return raw.decode("utf-8", errors="replace")
        time.sleep(1.0)
    return ""


def find_nodes(xml, needle):
    try:
        root = ET.fromstring(xml)
    except Exception as e:
        print("[ERROR] XML 解析失败: %s" % e)
        return []
    out = []
    for el in root.iter("node"):
        text = el.get("text") or ""
        desc = el.get("content-desc") or ""
        if needle in text or needle in desc:
            b = re.findall(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", el.get("bounds") or "")
            if not b:
                continue
            x1, y1, x2, y2 = map(int, b[0])
            out.append({
                "text": text or desc,
                "cx": (x1 + x2) // 2,
                "cy": (y1 + y2) // 2,
                "bounds": (x1, y1, x2, y2),
            })
    out.sort(key=lambda n: n["bounds"][1])
    return out


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    mode = sys.argv[1]

    if mode == "dump":
        xml = dump_xml()
        out = sys.argv[2] if len(sys.argv) > 2 else r"D:\Temp\shots\ui.xml"
        with open(out, "w", encoding="utf-8") as f:
            f.write(xml)
        print("dumped -> %s (%d bytes)" % (out, len(xml)))
        return 0

    needle = sys.argv[2]
    # 支持 \uXXXX 转义 (PowerShell 传中文参数容易乱码, 用转义更稳)
    needle = re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), needle)
    xml = dump_xml()

    # tap: 找不到或不在安全区就自动滚动, 找到后点击
    if mode in ("tap", "scrollto"):
        SAFE_TOP, SAFE_BOTTOM = 260, 2050
        for attempt in range(12):
            nodes = find_nodes(xml, needle)
            if nodes:
                n = nodes[0]
                y = n["cy"]
                if SAFE_TOP <= y <= SAFE_BOTTOM:
                    adb("shell", "input", "tap", str(n["cx"]), str(n["cy"]))
                    print("-> tapped (%d,%d)  %s" % (n["cx"], n["cy"], n["text"][:50]))
                    return 0
                # 在安全区外: 滚动把它带进视野
                if y < SAFE_TOP:
                    adb("shell", "input", "swipe", "540", "900", "540", "1500", "300")
                else:
                    adb("shell", "input", "swipe", "540", "1600", "540", "900", "300")
                time.sleep(1.2)
                xml = dump_xml()
                continue
            adb("shell", "input", "swipe", "540", "1600", "540", "900", "300")
            time.sleep(1.2)
            xml = dump_xml()
        print("[MISS] 滚动 12 次后仍没找到含 %r 的节点" % needle)
        return 1
    if not xml:
        print("[ERROR] dump 失败")
        return 1
    nodes = find_nodes(xml, needle)
    if not nodes:
        print("[MISS] 没找到含 %r 的节点" % needle)
        # 打印页面上所有有文字的节点, 方便定位
        try:
            root = ET.fromstring(xml)
            texts = [(el.get("text") or el.get("content-desc") or "")
                     for el in root.iter("node")]
            texts = [t for t in texts if t.strip()]
            print("页面上的文本:")
            for t in texts[:60]:
                print("   - %s" % t)
        except Exception:
            pass
        return 1

    for n in nodes:
        print("%-8s (%4d,%4d)  %s" % (mode, n["cx"], n["cy"], n["text"][:60]))

    if mode == "tap":
        n = nodes[0]
        adb("shell", "input", "tap", str(n["cx"]), str(n["cy"]))
        print("-> tapped (%d,%d)" % (n["cx"], n["cy"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
