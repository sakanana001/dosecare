#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""核查若干可疑的日文名: 该标题在日文维基是否存在, 以及是否重定向到别的标题。"""
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

CHECK = [
    "ペランパネル", "ピレンパネル", "ピレンパネル (抗てんかん薬)",
    "レボドパ／カルビドパ", "レボドパ", "カルビドパ",
    "Γ-ヒドロキシ酪酸", "γ-ヒドロキシ酪酸",
    "エスシリカルバゼピン酢酸塩", "リセドロン酸ナトリウム",
    "ジヒドロエトルフィン", "ノルケタミン", "レボアルブテロール",
    "結合型女性ホルモン", "乾燥甲状腺", "メマンチン・ドネペジル配合剤",
    "ジプラシドン", "アセナピン", "イロペリドン", "フェネルジン", "トルカポン",
    "ラコサミド", "ベタヒスチン", "ザレプロン", "プラバスタチン", "フルバスタチン",
    "アンフェタミン", "コルヒチン", "モルヒネ", "ブレクスピプラゾール",
    "セプロキセチン", "ボルチオキセチン", "キシナノメリン",
    "キサノメリン・トロスピウム", "четаミRoller",
]


def check(titles):
    params = {
        "action": "query",
        "titles": "|".join(titles),
        "redirects": "1",
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
    titles = [t for t in CHECK if not any("一" <= c <= "鿿" and c not in "药业" for c in t)]
    # 去掉明显是乱码的探测项
    titles = [t for t in titles if t not in ("工业大学", "かな漢字", "かな漢字")]
    data = check(titles)
    q = data.get("query", {})
    red = {r["from"]: r["to"] for r in (q.get("redirects") or [])}
    pages = {p.get("title", ""): p for p in (q.get("pages") or [])}
    for t in titles:
        tgt = red.get(t, t)
        p = pages.get(tgt, {})
        state = "缺失" if p.get("missing") else ("重定向->%s" % tgt if t in red else "存在")
        print("  %-28s %s" % (t, state))
    time.sleep(0.5)


if __name__ == "__main__":
    main()
