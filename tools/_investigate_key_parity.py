#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
查证 key 齐平的真相:
  1. 正则计数 vs ET 计数 差在哪(注释块? 额外属性?)
  2. 日语真缺的 2 个 key 在各 locale 文件里到底长什么样
  3. 中文三语言缺的那 23~25 个 key 是不是在注释块里
"""
import io
import os
import re
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

LOCALES = ["values", "values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')


def raw(locale):
    p = os.path.join(RES, locale, "strings.xml")
    with io.open(p, "r", encoding="utf-8") as f:
        return f.read()


def et_keys(text):
    tree = ET.fromstring(text)
    d = {}
    for el in tree.iter():
        if el.tag == "string" and el.get("name"):
            d.setdefault(el.get("name"), el.text or "")
    return d


def strip_comments(text):
    return re.sub(r'<!--.*?-->', '', text, flags=re.S)


def main():
    texts = {loc: raw(loc) for loc in LOCALES}

    print("=== 正则 vs ET ===")
    for loc in LOCALES:
        t = texts[loc]
        rx = set(STR_RE.findall(t))
        rx_nc = set(STR_RE.findall(strip_comments(t)))
        et = set(et_keys(t))
        print("%-22s regex=%4d  regex(去注释)=%4d  ET=%4d" % (loc, len(rx), len(rx_nc), len(et)))
        if rx_nc != et:
            only_rx = sorted(rx_nc - et)
            only_et = sorted(et - rx_nc)
            if only_rx:
                print("      仅正则有: %s" % only_rx[:8])
            if only_et:
                print("      仅 ET 有 : %s" % only_et[:8])

    # 英文文件里的注释块, 看看藏了多少 string
    print("\n=== values/strings.xml 里的注释块 ===")
    for i, m in enumerate(re.finditer(r'<!--(.*?)-->', texts["values"], re.S), 1):
        body = m.group(1)
        keys = STR_RE.findall(body)
        if keys:
            print("  注释块 #%d: %d 行, 内含 %d 个 string" % (i, body.count("\n"), len(keys)))
            print("    首个: %s = %s" % (keys[0][0], keys[0][1][:50]))

    # 日语真缺的 2 个 key, 在各文件里的原始行
    print("\n=== 关键 key 原始行 ===")
    probe = ["privacy_policy_url", "anchor_widget_preview_name",
             "hrt_goal_ftm", "main_chart_hormone_cpa", "medication_name_testosterone_gel"]
    for k in probe:
        print("\n--- %s ---" % k)
        for loc in LOCALES:
            t = texts[loc]
            lines = [ln.strip() for ln in t.splitlines() if ('name="%s"' % k) in ln]
            if not lines:
                print("  %-22s (无)" % loc)
                continue
            for ln in lines:
                # 判断是否在注释里
                idx = t.find(ln)
                before = t[:idx]
                in_comment = before.count("<!--") > before.count("-->")
                print("  %-22s %s %s" % (loc, "[注释内]" if in_comment else "[真实元素]", ln[:88]))


if __name__ == "__main__":
    main()
