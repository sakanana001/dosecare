#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""调试: 批量 langlinks 请求的原始响应。"""
import json
import sys
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

API = "https://en.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"

titles = sys.argv[1:]
params = {
    "action": "query",
    "titles": "|".join(titles),
    "prop": "langlinks",
    "lllang": "ja",
    "redirects": "1",
    "format": "json",
    "formatversion": "2",
}
url = API + "?" + urllib.parse.urlencode(params)
print("URL len = %d" % len(url))
req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
with urllib.request.urlopen(req, timeout=40) as resp:
    data = json.loads(resp.read().decode("utf-8"))

q = data.get("query", {})
print("redirects:", json.dumps(q.get("redirects"), ensure_ascii=False)[:400])
print("normalized:", json.dumps(q.get("normalized"), ensure_ascii=False)[:200])
print("pages count:", len(q.get("pages", [])))
for p in q.get("pages", []):
    ll = p.get("langlinks") or []
    print("  %-26s missing=%-5s langlinks=%s" % (
        p.get("title", "")[:26], p.get("missing", False),
        [x["title"] for x in ll]))
