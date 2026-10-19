#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出中文三语言相对英文的真实缺口, 带英文原文 + 日语参考译文。"""
import io
import os
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")


def load(loc):
    p = os.path.join(RES, loc, "strings.xml")
    tree = ET.parse(p)
    d = {}
    for el in tree.iter():
        if el.tag == "string" and el.get("name"):
            d.setdefault(el.get("name"), (el.text or "", el.get("translatable")))
    return d


en = load("values")
ja = load("values-ja")
hans = load("values-b+zh+Hans")
hant = load("values-b+zh+Hant")
yue = load("values-b+yue+Hant+HK")

need = {}
for loc, data in [("简中", hans), ("繁中", hant), ("港繁", yue)]:
    miss = []
    for k, (text, trans) in en.items():
        if k in data:
            continue
        if trans == "false":
            continue  # translatable=false, 不该翻译
        miss.append(k)
    need[loc] = miss
    print("%s: 缺 %d 个(排除 translatable=false)" % (loc, len(miss)))

allkeys = []
for loc in need:
    for k in need[loc]:
        if k not in allkeys:
            allkeys.append(k)

print("\n共 %d 个不同 key 需要补\n" % len(allkeys))
for k in allkeys:
    e = en[k][0]
    j = ja.get(k, ("", None))[0]
    where = [loc for loc in need if k in need[loc]]
    print("### %s" % k)
    print("  缺于   : %s" % "/".join(where))
    print("  EN      : %s" % e.replace("\n", "\\n"))
    print("  JA(参考): %s" % (j.replace("\n", "\\n") if j else "(缺)"))
    print()
