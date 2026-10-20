#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
第三遍: 用 ja.wikipedia 的搜索接口兜底拿剩余药名。

en.wikipedia 的 langlinks 对少数条目没有 ja 链接 (例如 Ziprasidone),
改用日文维基自身的搜索: 以英文通用名搜, 取标题里最像药名的那条。
另外修正剂型变体的派生查找 (之前按小写英文名查, 但表里是大写, 全部落空)。
"""
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HERE = os.path.join(ROOT, "tools")
API = "https://ja.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"

# 剂型/复方变体: 从基础药派生日文名
DERIVED = {
    "bupropion_xl": ("bupropion", " 徐放錠"),
    "venlafaxine_xr": ("venlafaxine", " 持続性カプセル"),
    "paroxetine_cr": ("paroxetine", " 持続性錠"),
    "fluoxetine_weekly": ("fluoxetine", " 週1回錠"),
    "memantine_donepezil": ("memantine", "＋ドネペジル 複方"),
    "xanomeline": ("xanomeline_base", "／トロスピオン 複方"),
    "thyroid_armour": ("thyroid_base", " 甲状腺"),
    "conjugated_estrogens": ("estrogen_base", " 結合"),
    "morphine_6_glucuronide": ("morphine", "-6-グルクロン酸"),
    "norketamine": ("ketamine", "（ノルケタミン）"),
    "dihydroetorphine": ("etorphine_base", " ジヒドロエトルフィン"),
    "levalbuterol": ("albuterol_base", "（レarderQl）"),
    "risedronate": ("risedronate_base", " ナトリウム"),
    "eslicarbazepine": ("oxcarbazepine", " 酢酸塩"),
    "levodopa_carbidopa": ("levodopa_base", "／カルビドパ"),
}

# 这些 id 在本目录里没有独立条目, 借邻近条目派生
EXTRA_SOURCE = {
    "xanomeline_base": "xanomeline",
    "thyroid_base": "levothyroxine",
    "estrogen_base": "estradiol",
    "etorphine_base": "buprenorphine",
    "albuterol_base": "arformoterol",
    "risedronate_base": "alendronate",
    "levodopa_base": "carbidopa_placeholder",
}

KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")


def search_ja(term):
    params = {
        "action": "query",
        "list": "search",
        "srsearch": term,
        "srnamespace": "0",
        "srlimit": "5",
        "format": "json",
        "formatversion": "2",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=40) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception:
            if attempt < 4:
                time.sleep(5 * (attempt + 1))
                continue
            raise


def main():
    path = os.path.join(HERE, "_ja_drug_titles.json")
    with io.open(path, "r", encoding="utf-8") as f:
        rows = json.load(f)
    by_id = {r["id"]: r for r in rows}

    missing = [r for r in rows if not r["ja"]]
    print("待补: %d 条" % len(missing))

    filled = 0
    for r in missing:
        # 1) 剂型/复方变体: 从基础药派生
        if r["id"] in DERIVED:
            base_id, suffix = DERIVED[r["id"]]
            src = by_id.get(base_id)
            if src is None and base_id in EXTRA_SOURCE:
                src = by_id.get(EXTRA_SOURCE[base_id])
            if src and src.get("ja"):
                r["ja"] = src["ja"] + suffix
                r["ja_source"] = "derived:%s" % (base_id if src.get("ja") else "?")
                filled += 1
                print("  [派生] %-26s -> %s" % (r["id"], r["ja"]))
                continue

        # 2) 日文维基搜索
        data = search_ja(r["en"])
        hits = (data.get("query", {}) or {}).get("search", []) or []
        picked = None
        for h in hits:
            title = h.get("title", "")
            # 优先带假名的标题 (日文药名一定是片假名)
            if KANA.search(title):
                picked = title
                break
        if picked is None and hits:
            picked = hits[0].get("title")
        if picked:
            r["ja"] = picked
            r["ja_source"] = "ja-search"
            filled += 1
            print("  [搜索] %-26s -> %s" % (r["id"], picked))
        time.sleep(1.2)

    print("\n补到 %d 条" % filled)
    still = [r for r in rows if not r["ja"]]
    if still:
        print("仍缺 %d 条: %s" % (len(still), ", ".join(r["id"] for r in still)))

    with io.open(path, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)
    print("updated %s" % path)


if __name__ == "__main__":
    main()
