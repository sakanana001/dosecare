# -*- coding: utf-8 -*-
"""只挑"英文散文"型漏译: 同一 locale 里与英文逐字相同、且含多个英文单词的字符串。"""
import io
import os
import re
import xml.etree.ElementTree as ET

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
RES = os.path.join(REPO, "app", "src", "main", "res")


def load(folder):
    path = os.path.join(RES, folder, "strings.xml")
    if not os.path.exists(path):
        return {}
    tree = ET.parse(path)
    out = {}
    for el in tree.iter():
        if el.tag == "string" and el.get("name"):
            out[el.get("name")] = (el.text or "").strip()
    return out


en = load("values")
# 英文散文启发: 至少两个英文单词 (含小写单词), 长度 >= 6
PROSE = re.compile(r"(?:\b[A-Za-z][a-z]{2,}\b.*){2,}")

for loc in ["values-b+zh+Hant", "values-b+yue+Hant+HK", "values-b+zh+Hans", "values-ja"]:
    d = load(loc)
    hits = []
    for k, v in d.items():
        if k not in en or en[k] != v:
            continue
        if not v or not PROSE.search(v):
            continue
        hits.append((k, v))
    print("=" * 84)
    print("%s : 英文散文型漏译 %d 条" % (loc, len(hits)))
    for k, v in sorted(hits):
        print("   %-40s = %s" % (k, v[:100]))
