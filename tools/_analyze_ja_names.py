# -*- coding: utf-8 -*-
"""分析 catalog.json 里 202 个药的日文名: 哪些没有假名 / 与中文名或英文名重名。"""
import io
import json
import os
import re

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
PATH = os.path.join(REPO, "app", "src", "main", "assets", "pharmacology", "catalog.json")

with io.open(PATH, "r", encoding="utf-8") as f:
    data = json.load(f)

drugs = data["drugs"] if isinstance(data, dict) and "drugs" in data else data
KANA = re.compile(r"[\u3040-\u309F\u30A0-\u30FF]")

no_kana = []
same_zh = []
same_en = []
blank = []
for d in drugs:
    ja = (d.get("genericNameJa") or "").strip()
    zh = (d.get("genericNameZh") or "").strip()
    en = (d.get("genericName") or "").strip()
    if not ja:
        blank.append(d.get("id"))
        continue
    if not KANA.search(ja):
        no_kana.append((d.get("id"), en, zh, ja))
    if ja == zh:
        same_zh.append((d.get("id"), en, zh, ja))
    if ja.lower() == en.lower():
        same_en.append((d.get("id"), en, zh, ja))

print("总药数: %d" % len(drugs))
print("日文名空白: %d %s" % (len(blank), blank[:10]))
print()
print("日文名无假名 (纯汉字/片假名以外): %d" % len(no_kana))
for x in no_kana:
    print("   %-28s en=%-28s zh=%-16s ja=%s" % x)
print()
print("日文名 == 中文名: %d" % len(same_zh))
for x in same_zh:
    print("   %-28s en=%-28s zh=%-16s ja=%s" % x)
print()
print("日文名 == 英文名: %d" % len(same_en))
for x in same_en:
    print("   %-28s en=%-28s zh=%-16s ja=%s" % x)
