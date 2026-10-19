#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""统计各 locale 里"key 存在但内容与英文完全相同"的条目 (底座翻译债的规模)。"""
import io
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')

LOCALES = ["values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]


def load(loc):
    p = os.path.join(RES, loc, "strings.xml")
    t = io.open(p, "r", encoding="utf-8").read()
    return {k: v for k, v in STR_RE.findall(t)}


en = load("values")
# 这些是标识符/品牌/单位, 保留英文是正常的
OK_ENGLISH = re.compile(
    r'^[\sA-Za-z0-9%.,:/·+\-–—()\[\]<>=!&;\'"@#$^*_|~]*$'
)

for loc in LOCALES:
    d = load(loc)
    same = [k for k, v in d.items() if k in en and v == en[k]]
    # 排除纯 ASCII 标识符类
    real = [k for k in same if not OK_ENGLISH.match(en[k] or "x")]
    # 含中日文字符的一律算"未翻译"
    with_cjk = [k for k in same if re.search(r'[\u3040-\u30ff\u4e00-\u9fff]', en[k])]
    print("%-24s 总 key %4d | 与英文逐字相同 %3d | 其中含中日文字符(真未译) %3d"
          % (loc, len(d), len(same), len(with_cjk)))
    if with_cjk:
        print("    例: %s" % ", ".join(with_cjk[:12]))
    print()
