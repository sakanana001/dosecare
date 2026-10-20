#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""用 KEGG 日文版核验剩下几条 (支持用日文名检索, 输出用 repr 避免控制台误读)。"""
import io
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from _verify_ja_names_kegg import find_drug, japanese_generic  # noqa: E402

TERMS = [
    ("Levalbuterol", "levalbuterol"),
    ("Norketamine", "norketamine"),
    ("Norketamine(ja)", "ノルケタミン"),
    ("Dihydroetorphine", "dihydroetorphine"),
    ("Dihydroetorphine(ja)", "ジヒドロエトルフィン"),
    ("Conjugated estrogens", "conjugated estrogens"),
    ("Estrone", "estrone"),
    ("Estradiol", "estradiol"),
    ("Morphine-6-glucuronide", "morphine glucuronide"),
    ("Morphine glucuronide(ja)", "グルクロン酸"),
    ("Levodopa/Carbidopa", "carbidopa"),
    ("Xanomeline", "xanomeline"),
    ("Memantine+Donepezil", "memantine"),
    ("Ferrous sulfate", "ferrous sulfate"),
    ("Ferrous sulfate(ja)", "硫酸鉄"),
    ("Eslicarbazepine", "eslicarbazepine"),
    ("Bupropion", "bupropion"),
    ("Venlafaxine", "venlafaxine"),
    ("Paroxetine", "paroxetine"),
    ("Fluoxetine", "fluoxetine"),
]


def main():
    for label, term in TERMS:
        did = find_drug(term)
        if not did:
            print("%-24s %-24s -> KEGG 查不到" % (label, term))
            continue
        try:
            gen = japanese_generic(did)
        except Exception as e:
            gen = "抓取失败: %s" % e
        print("%-24s %-24s -> %s : %s" % (label, term, did, gen))


if __name__ == "__main__":
    main()
