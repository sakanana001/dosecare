# -*- coding: utf-8 -*-
"""校验: 1) XML 仍可解析  2) 占位符编号多重集与英文一致  3) 繁中/港繁不再有英文散文漏译。"""
import io
import os
import re
import xml.etree.ElementTree as ET
from collections import Counter

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
RES = os.path.join(REPO, "app", "src", "main", "res")
LOCALES = ["values", "values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]

PH = re.compile(r"%(\d+\$)?[sd]")


def load(folder):
    path = os.path.join(RES, folder, "strings.xml")
    tree = ET.parse(path)                      # 解析失败会直接抛异常
    return {el.get("name"): (el.text or "")
            for el in tree.iter() if el.tag == "string" and el.get("name")}


en = load("values")
print("== XML 全部可解析 ==")

bad = []
for loc in LOCALES:
    d = load(loc)
    for k, v in d.items():
        if k not in en:
            continue
        a = Counter(PH.findall(v))
        b = Counter(PH.findall(en[k]))
        if a != b:
            bad.append((loc, k, sorted(a.elements()), sorted(b.elements())))
print("占位符不一致: %d 条" % len(bad))
for x in bad:
    print("   ", x)

print()
print("== 繁中/港繁英文散文残留 ==")
PROSE = re.compile(r"(?:\b[A-Za-z][a-z]{2,}\b.*){2,}")
for loc in ["values-b+zh+Hant", "values-b+yue+Hant+HK"]:
    d = load(loc)
    hits = [(k, v) for k, v in d.items()
            if k in en and en[k].strip() == v.strip() and v.strip() and PROSE.search(v)]
    print("%s : %d 条" % (loc, len(hits)))
    for k, v in sorted(hits):
        print("   %-40s = %s" % (k, v[:90]))

print()
print("== key 齐平 (英文 vs 各译文) ==")
for loc in LOCALES[1:]:
    d = load(loc)
    only_en = sorted(set(en) - set(d))
    only_lo = sorted(set(d) - set(en))
    print("%-22s 缺 %d / 多 %d" % (loc, len(only_en), len(only_lo)))
    if only_en:
        print("     缺:", only_en[:10])
    if only_lo:
        print("     多:", only_lo[:10])
