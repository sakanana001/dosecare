#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从英文维基百科的 langlinks 批量取日文条目标题, 作为日文药名的候选来源。

日文维基的药类条目标题基本就是该药的日文通用名 (INN 日语),
例如 Clozapine -> クロザピン, Fluvoxamine -> フルボキサミン。

用法:
  python _fetch_ja_drug_titles.py            # 输出候选表 (id -> ja title)
  python _fetch_ja_drug_titles.py --json out.json
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
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")

API = "https://en.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"
BATCH = 25


def fetch(titles, attempt=0):
    params = {
        "action": "query",
        "titles": "|".join(titles),
        "prop": "langlinks",
        "lllang": "ja",
        # lllimit 是**整批查询**的 langlinks 总数上限, 匿名用户默认只有 10,
        # 不显式设成 500 的话 25 条里只有 10 条能拿到日文链接 (实测)。
        "lllimit": "500",
        "redirects": "1",
        "format": "json",
        "formatversion": "2",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=40) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        if attempt < 5:
            wait = 4 * (attempt + 1)
            print("  retry in %ds (%s)" % (wait, e))
            time.sleep(wait)
            return fetch(titles, attempt + 1)
        raise


def main():
    with io.open(CATALOG, "r", encoding="utf-8") as f:
        drugs = json.load(f)["drugs"]

    # 用英文通用名去查 (少数几条 genericName 是复方/盐型, 单独处理)
    by_en = {}
    for d in drugs:
        by_en.setdefault(d["genericName"], []).append(d["id"])

    en_names = list(by_en.keys())
    print("英文名 %d 个 (对应 %d 个药)" % (len(en_names), len(drugs)))

    result = {}
    for i in range(0, len(en_names), BATCH):
        chunk = en_names[i:i + BATCH]
        data = fetch(chunk)
        norm = {}
        for r in data.get("query", {}).get("redirects", []) or []:
            norm[r["to"]] = r["from"]
        for r in data.get("query", {}).get("normalized", []) or []:
            norm[r["to"]] = r["from"]
        for page in data.get("query", {}).get("pages", []) or []:
            title = page.get("title", "")
            src = norm.get(title, title)
            lls = page.get("langlinks") or []
            ja = lls[0]["title"] if lls else None
            result[src] = ja
        print("  batch %d/%d done" % (min(i + BATCH, len(en_names)), len(en_names)))
        time.sleep(2.0)

    rows = []
    missing = []
    for d in drugs:
        ja = result.get(d["genericName"])
        rows.append({
            "id": d["id"],
            "en": d["genericName"],
            "zh": d["genericNameZh"],
            "ja": ja,
        })
        if not ja:
            missing.append(d["id"])

    print("\n拿到日文标题 %d / %d" % (len(drugs) - len(missing), len(drugs)))
    if missing:
        print("缺日文条目的药 id (%d): %s" % (len(missing), ", ".join(missing)))

    if "--json" in sys.argv:
        out = sys.argv[sys.argv.index("--json") + 1]
        with io.open(out, "w", encoding="utf-8") as f:
            json.dump(rows, f, ensure_ascii=False, indent=2)
        print("wrote %s" % out)
    else:
        for r in rows:
            print("%-28s | %-32s | %-22s | %s" % (r["id"], r["en"], r["zh"], r["ja"] or "(无)"))


if __name__ == "__main__":
    main()
