#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
第二遍: 对第一批没拿到日文标题的药逐个单查。

单条查询没有 lllimit 整批限额的问题, 能补回大部分。
另外处理两类特殊情况:
  - 标题里带 "/" 的 (Levodopa/Carbidopa): 拆成两个药分别查
  - 英文条目本身是重定向的: 用 redirects=1 跟到底
"""
import io
import json
import os
import sys
import time
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HERE = os.path.join(ROOT, "tools")
API = "https://en.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"


def query(titles):
    params = {
        "action": "query",
        "titles": "|".join(titles),
        "prop": "langlinks",
        "lllang": "ja",
        "lllimit": "500",
        "redirects": "1",
        "format": "json",
        "formatversion": "2",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=40) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception as e:
            if attempt < 4:
                time.sleep(5 * (attempt + 1))
                continue
            raise


def main():
    path = os.path.join(HERE, "_ja_drug_titles.json")
    with io.open(path, "r", encoding="utf-8") as f:
        rows = json.load(f)

    missing = [r for r in rows if not r["ja"]]
    print("待补: %d 条" % len(missing))

    # 组合药/剂型变体: 直接从基础药的日文名派生
    DERIVED = {
        "bupropion_xl": ("bupropion", " 徐放錠"),
        "venlafaxine_xr": ("venlafaxine", " 持続性カプセル"),
        "paroxetine_cr": ("paroxetine", " 持続性錠"),
        "fluoxetine_weekly": ("fluoxetine", " 週1回錠"),
        "memantine_donepezil": ("memantine", "＋ドネペジル 複方"),
    }

    by_en = {r["en"]: r for r in rows}
    filled = 0
    for r in missing:
        en = r["en"]
        if r["id"] in DERIVED:
            base, suffix = DERIVED[r["id"]]
            src = by_en.get(base)
            if src and src["ja"]:
                r["ja"] = src["ja"] + suffix
                r["ja_source"] = "derived:%s" % base
                filled += 1
                continue
        if "/" in en:
            # "Levodopa/Carbidopa" -> 分别查
            parts = [p.strip() for p in en.split("/") if p.strip()]
            got = []
            for p in parts:
                data = query([p])
                pages = data.get("query", {}).get("pages", []) or []
                ll = (pages[0].get("langlinks") or []) if pages else []
                if ll:
                    got.append(ll[0]["title"])
                time.sleep(1.0)
            if got:
                r["ja"] = "／".join(got)
                r["ja_source"] = "split:%s" % "/".join(parts)
                filled += 1
            continue
        data = query([en])
        pages = data.get("query", {}).get("pages", []) or []
        ll = (pages[0].get("langlinks") or []) if pages else []
        if ll:
            r["ja"] = ll[0]["title"]
            r["ja_source"] = "single"
            filled += 1
        time.sleep(1.0)

    print("补到 %d 条, 仍缺 %d 条" % (filled, len(missing) - filled))
    still = [r for r in rows if not r["ja"]]
    if still:
        print("仍缺:")
        for r in still:
            print("   %-28s | %-32s | %s" % (r["id"], r["en"], r["zh"]))

    with io.open(path, "w", encoding="utf-8") as f:
        json.dump(rows, f, ensure_ascii=False, indent=2)
    print("updated %s" % path)


if __name__ == "__main__":
    main()
