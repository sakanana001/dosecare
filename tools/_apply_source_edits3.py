#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
第三批源码改动:
  1. InteractionScreen 过量提示里的药名 -> 界面语言
  2. 选药列表按界面语言排序 (原来固定按中文名排, 日文/英文界面下顺序是乱的)
用法: python _apply_source_edits3.py [--dry]
"""
import io
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src", "main", "java", "com", "dosecare", "app")

# (相对路径, 旧行, 新行)
LINE_EDITS = [
    ("ui/interaction/InteractionScreen.kt",
     "                                    viewModel.tryGetDrug(id)?.takeIf { it.overdose === o }?.genericNameZh",
     "                                    viewModel.tryGetDrug(id)?.takeIf { it.overdose === o }?.displayName()"),
]

# (相对路径, 旧行, 新行) —— 选药器排序
PICKER_EDITS = [
    ("ui/interaction/SearchableDrugPicker.kt",
     "    if (showDialog) {",
     "    // 列表按当前界面语言的主名排序: 中文界面按中文名 (与底座一致),\n"
     "    // 日文/英文界面按各自的通用名 —— 原来固定按中文名排, 在这两种界面下顺序是乱的。\n"
     "    val language = currentUiLanguage()\n"
     "    val sortedOptions = remember(options, language) {\n"
     "        options.sortedBy { it.displayNameFor(language) }\n"
     "    }\n"
     "\n"
     "    if (showDialog) {"),
    ("ui/interaction/SearchableDrugPicker.kt",
     "            options = options.filter(prefilter),",
     "            options = sortedOptions.filter(prefilter),"),
]


def load(path):
    with open(path, "rb") as f:
        raw = f.read()
    has_bom = raw[:3] == b"\xef\xbb\xbf"
    text = raw.decode("utf-8-sig")
    eol = "\r\n" if "\r\n" in text else "\n"
    return has_bom, eol, text.split(eol)


def save(path, has_bom, eol, lines):
    data = eol.join(lines).encode("utf-8")
    if has_bom:
        data = b"\xef\xbb\xbf" + data
    with open(path, "wb") as f:
        f.write(data)


def main():
    dry = "--dry" in sys.argv
    ok = True
    for rel, old, new in LINE_EDITS + PICKER_EDITS:
        path = os.path.join(APP, rel)
        has_bom, eol, lines = load(path)
        hits = [i for i, l in enumerate(lines) if l == old]
        if not hits:
            print("[MISS] %s: %r" % (rel, old[:90]))
            ok = False
            continue
        if len(hits) > 1:
            print("[MULTI x%d] %s" % (len(hits), rel))
        i = hits[0]
        new_lines = new.split("\n")
        lines[i:i + 1] = new_lines
        if not dry:
            save(path, has_bom, eol, lines)
        print("[%s] %s" % ("DRY" if dry else "OK ", rel))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
