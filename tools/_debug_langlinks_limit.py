#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""调试 langlinks 的 per-query 限额: 对比 lllimit 与批量的影响。"""
import json
import sys
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

API = "https://en.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"

TITLES = ["Clozapine", "Olanzapine", "Risperidone", "Quetiapine", "Aripiprazole",
          "Ziprasidone", "Amisulpride", "Valproic acid", "Lithium carbonate",
          "Lamotrigine", "Fluoxetine", "Sertraline", "Paroxetine", "Escitalopram",
          "Fluvoxamine", "Citalopram", "Venlafaxine", "Duloxetine", "Diazepam",
          "Lorazepam", "Carbamazepine", "Oxcarbazepine", "Topiramate", "Gabapentin",
          "Pregabalin"]


def run(titles, lllimit=None):
    params = {
        "action": "query",
        "titles": "|".join(titles),
        "prop": "langlinks",
        "lllang": "ja",
        "redirects": "1",
        "format": "json",
        "formatversion": "2",
    }
    if lllimit is not None:
        params["lllimit"] = str(lllimit)
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=40) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    pages = data.get("query", {}).get("pages", [])
    got = [p for p in pages if p.get("langlinks")]
    return len(pages), len(got), [p["title"] for p in got]


for label, titles, ll in [
    ("25 条, 不带 lllimit", TITLES, None),
    ("25 条, lllimit=500", TITLES, 500),
    ("25 条, lllimit=20", TITLES, 20),
    ("10 条, lllimit=500", TITLES[:10], 500),
    ("5 条,  lllimit=500", TITLES[:5], 500),
]:
    try:
        n, g, who = run(titles, ll)
        print("%-22s pages=%2d  有日文=%2d  %s" % (label, n, g, who))
    except Exception as e:
        print("%-22s ERROR %s" % (label, e))
