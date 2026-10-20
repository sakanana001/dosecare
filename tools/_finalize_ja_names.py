#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
人工定稿日文通用名: 修正自动抓取里抓错的、派生出错的、以及漏掉的。

来源说明:
  - 170 条来自 en.wikipedia 的 langlinks (日文条目标题), 已用"日文条目首段提到对应英文药名"验证
  - 下面这张表是人工按日本医药品一般名(INN 日本語)核定的修正项
"""
import io
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HERE = os.path.join(ROOT, "tools")

# id -> 日文通用名 (人工核定)
MANUAL = {
    # --- 自动搜索抓错的 ---
    "ziprasidone": "ジプラシドン",
    "asenapine": "アセナピン",
    "iloperidone": "イロペリドン",
    "phenelzine": "フェネルジン",
    "tolcapone": "トルカポン",
    "lacosamide": "ラコサミド",
    "betahistine": "ベタヒスチン",
    "zaleplon": "ザレプロン",
    "pravastatin": "プラバスタチン",
    "fluvastatin": "フルバスタチン",

    # --- 派生错的 (基药选错 / 后缀不自然) ---
    "eslicarbazepine": "エスシリカルバゼピン酢酸塩",
    "risedronate": "リセドロン酸ナトリウム",
    "dihydroetorphine": "ジヒドロエトルフィン",
    "norketamine": "ノルケタミン",
    "levalbuterol": "レボアルブテロール",
    "conjugated_estrogens": "結合型女性ホルモン",
    "thyroid_armour": "乾燥甲状腺",
    "memantine_donepezil": "メマンチン・ドネペジル配合剤",

    # --- 剂型变体 (基药名 + 剂型后缀) ---
    "bupropion_xl": "ブプロピオン徐放錠",
    "venlafaxine_xr": "ベンラファキシン持続性カプセル",
    "paroxetine_cr": "パロキセチン持続性錠",
    "fluoxetine_weekly": "フルオキセチン週1回錠",
    "morphine_6_glucuronide": "モルヒネ-6-グルクロン酸",

    # --- 漏掉的 ---
    "flunarizine": "フルナリジン",
    "tianeptine": "チアネプチン",
}


def main():
    path = os.path.join(HERE, "_ja_drug_titles.json")
    with io.open(path, "r", encoding="utf-8") as f:
        rows = json.load(f)

    applied = 0
    for r in rows:
        if r["id"] in MANUAL:
            old = r.get("ja")
            r["ja"] = MANUAL[r["id"]]
            r["ja_source"] = "manual"
            r["ja_prev"] = old
            applied += 1

    with io.open(path, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)

    missing = [r for r in rows if not r.get("ja")]
    print("人工修正 %d 条; 仍无日文名 %d 条" % (applied, len(missing)))
    for r in missing:
        print("   缺: %s (%s)" % (r["id"], r["en"]))

    # 全表输出, 供人工通读
    print("\n=== 202 药日文名全表 ===")
    for r in rows:
        flag = "*" if r.get("ja_source") == "manual" else " "
        print("%s%-26s | %-30s | %-20s | %-24s | %s"
              % (flag, r["id"], r["en"], r["zh"], r.get("ja") or "(缺)", r.get("ja_source")))


if __name__ == "__main__":
    main()
