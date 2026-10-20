#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""对指定药名逐个在日文维基搜索, 列出候选条目标题 (人工挑)。"""
import io
import json
import os
import sys
import time
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

API = "https://ja.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"

PROBE = [
    "Ziprasidone", "Asenapine", "Iloperidone", "Phenelzine", "Tolcapone",
    "Lacosamide", "Betahistine", "Zaleplon", "Fluvastatin", "Pravastatin",
    "Perampanel", "Levodopa Carbidopa", "Norketamine", "Dihydroetorphine",
    "Levalbuterol", "Eslicarbazepine", "Conjugated estrogens", "Desiccated thyroid",
    "Memantine Donepezil", "Norketamine",
]


def search(term):
    params = {
        "action": "query",
        "list": "search",
        "srsearch": term,
        "srnamespace": "0",
        "srlimit": "4",
        "format": "json",
        "formatversion": "2",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    for a in range(5):
        try:
            with urllib.request.urlopen(req, timeout=40) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception:
            if a < 4:
                time.sleep(5 * (a + 1))
                continue
            raise


def main():
    for term in PROBE:
        data = search(term)
        hits = (data.get("query", {}) or {}).get("search", []) or []
        titles = [h.get("title", "") for h in hits]
        print("%-24s -> %s" % (term, " | ".join(titles) if titles else "(无结果)"))
        time.sleep(1.2)


if __name__ == "__main__":
    main()
