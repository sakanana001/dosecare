#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抓日文维基条目的正文片段, 用来核对通用名 (例如 ペランパネル 条目里写的一般名)。"""
import io
import json
import sys
import time
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

API = "https://ja.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"


def extracts(titles):
    params = {
        "action": "query",
        "titles": "|".join(titles),
        "redirects": "1",
        "prop": "extracts",
        "explaintext": "1",
        "exchars": "700",
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
    titles = sys.argv[1:]
    data = extracts(titles)
    q = data.get("query", {})
    for p in (q.get("pages") or []):
        print("=== %s ===" % p.get("title"))
        print((p.get("extract") or "(无正文)").replace("\n", " ")[:700])
        print()


if __name__ == "__main__":
    main()
