# -*- coding: utf-8 -*-
"""精确列出繁中/港繁的真漏译。

漏译判定: 简中该 key 的值与英文不同 (即简中确实翻译了),
          但繁中/港繁该 key 的值与英文逐字相同 (漏掉了)。
简中本身也用英文的 (如 ind_* 适应症分类) 属于三语言一致的设计, 不算漏译。
"""
import io
import os
import xml.etree.ElementTree as ET

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
RES = os.path.join(REPO, "app", "src", "main", "res")


def load(folder):
    path = os.path.join(RES, folder, "strings.xml")
    tree = ET.parse(path)
    return {el.get("name"): (el.text or "")
            for el in tree.iter() if el.tag == "string" and el.get("name")}


en = load("values")
hans = load("values-b+zh+Hans")
hant = load("values-b+zh+Hant")
yue = load("values-b+yue+Hant+HK")

gaps = []
for k, hv in hans.items():
    if k not in en:
        continue
    if hv.strip() == en.get(k, "").strip():
        continue                      # 简中也是英文 = 三语言一致, 不算漏译
    if hant.get(k, "").strip() == en.get(k, "").strip():
        gaps.append(k)

print("繁中/港繁 真漏译: %d 条" % len(gaps))
print()
for k in sorted(gaps):
    print("### %s" % k)
    print("  EN  : %s" % en.get(k, "").strip().replace("\n", "\\n"))
    print("  简中: %s" % hans[k].strip().replace("\n", "\\n"))
    print("  繁中==EN:%s  港繁==EN:%s" % (
        hant.get(k, "").strip() == en.get(k, "").strip(),
        yue.get(k, "").strip() == en.get(k, "").strip()))
    print()
