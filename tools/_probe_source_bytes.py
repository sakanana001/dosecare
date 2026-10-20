#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""打印指定文件里某段文本的真实字节上下文 (确认行尾/缩进)。"""
import io
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src", "main", "java", "com", "dosecare", "app")

TARGETS = [
    ("ui/interaction/SearchableDrugPicker.kt", "isChinesePrimary"),
    ("ui/catalog/dossier/PharmacologyDossierSection.kt", "genericNameZh"),
]


def main():
    for rel, needle in TARGETS:
        path = os.path.join(APP, rel)
        with open(path, "rb") as f:
            data = f.read()
        print("======== %s ========" % rel)
        print("BOM=%s  CRLF=%d  bareLF=%d" % (
            data[:3] == b"\xef\xbb\xbf",
            data.count(b"\r\n"),
            data.count(b"\n") - data.count(b"\r\n"),
        ))
        i = 0
        while True:
            j = data.find(needle.encode("utf-8"), i)
            if j < 0:
                break
            s = max(0, j - 120)
            e = min(len(data), j + 160)
            print("  ---")
            print("  " + repr(data[s:e].decode("utf-8", errors="replace")))
            i = j + 1
        print()


if __name__ == "__main__":
    main()
