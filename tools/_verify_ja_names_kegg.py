#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 KEGG DRUG 的日文版页面核验日文通用名。

KEGG 日文版 (dr_ja:Dxxxxx) 的 "一般名" 字段是日本承认的 INN 日本語,
比维基百科条目标题更权威 (维基标题偶尔不是一般名, 例如 perampanel)。

用法: python _verify_ja_names_kegg.py <英文名> ...
"""
import io
import re
import sys
import time
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"


def http(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    for a in range(5):
        try:
            with urllib.request.urlopen(req, timeout=40) as resp:
                return resp.read().decode("utf-8", errors="replace")
        except Exception:
            if a < 4:
                time.sleep(4 * (a + 1))
                continue
            raise


def find_drug(name):
    url = "https://rest.kegg.jp/find/drug/" + urllib.parse.quote(name)
    txt = http(url)
    # KEGG REST find 返回 "D08964\tPerampanel" 这样的行
    for line in txt.splitlines():
        m = re.match(r"^(D\d{5})\b", line.strip())
        if m:
            return m.group(1)
    m = re.search(r"\b(D\d{5})\b", txt)
    return m.group(1) if m else None


def japanese_generic(drug_id):
    url = "https://www.kegg.jp/entry/dr_ja:%s" % drug_id
    html = http(url)
    # 日文页的 "一般名" 行: <span class="nowrap">一般名&nbsp;...</span></th><td...><div class="cel">...</div>
    m = re.search(r"一般名.*?</span></th>\s*<td[^>]*>.*?<div class=\"cel\">(.*?)</div>", html, re.S)
    if not m:
        m = re.search(r"Name</span></th>\s*<td[^>]*>.*?<div class=\"cel\">(.*?)</div>", html, re.S)
    if not m:
        return None
    txt = re.sub(r"<[^>]+>", " ", m.group(1))
    txt = txt.replace("&nbsp;", " ").replace("&amp;", "&")
    return re.sub(r"\s+", " ", txt).strip()


def main():
    names = sys.argv[1:]
    for n in names:
        did = find_drug(n)
        if not did:
            print("%-24s -> KEGG 查不到" % n)
            continue
        try:
            gen = japanese_generic(did)
        except Exception as e:
            gen = "抓取失败: %s" % e
        print("%-24s -> %s : %s" % (n, did, gen))
        time.sleep(1.0)


if __name__ == "__main__":
    main()
