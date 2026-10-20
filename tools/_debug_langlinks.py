#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""调试: 单条 langlinks 请求的原始响应。"""
import json
import sys
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

API = "https://en.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"

for title in sys.argv[1:]:
    params = {
        "action": "query",
        "titles": title,
        "prop": "langlinks",
        "lllang": "ja",
        "redirects": "1",
        "format": "json",
        "formatversion": "2",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=40) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    print("=== %s ===" % title)
    print(json.dumps(data, ensure_ascii=False)[:1200])
    print()
