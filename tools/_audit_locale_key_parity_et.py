#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用真正的 XML 解析器 (ElementTree) 复核五语言 key 齐平。

为什么需要它:
    正则 `<string name="x"\\s*>(.*?)</string>` 匹配不到带额外属性的元素, 例如
    `<string name="x" translatable="false">` / `<string formatted="false" name="x">`。
    之前的审计脚本用正则, 因此报出的"缺 54 个"是假缺口。

本脚本:
    1. 用 ET 解析每个 locale 的 strings.xml, 取所有 <string name=...>
    2. 报告每个 locale 的真实 key 数
    3. 报告相对 values(英文) 的真实缺失 key
    4. 对缺失的 key 打印英文原文, 便于人工判断
    5. 顺带检查各 locale 里"带属性但没 name"的异常元素
"""
import io
import os
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

LOCALES = [
    ("values", "English"),
    ("values-b+zh+Hans", "简体中文"),
    ("values-b+zh+Hant", "繁體中文"),
    ("values-b+yue+Hant+HK", "港繁粵語"),
    ("values-ja", "日本語"),
]


def load(locale):
    path = os.path.join(RES, locale, "strings.xml")
    if not os.path.exists(path):
        return None, None
    with io.open(path, "r", encoding="utf-8") as f:
        raw = f.read()
    tree = ET.fromstring(raw)
    strings = {}
    attrs_seen = set()
    for el in tree.iter():
        if el.tag != "string":
            continue
        name = el.get("name")
        if not name:
            attrs_seen.add("<string> 缺少 name 属性")
            continue
        # 文本可能是 None (空元素)
        strings.setdefault(name, el.text or "")
    return strings, attrs_seen


def main():
    data = {}
    print("各语言真实 key 数 (ElementTree):")
    for loc, label in LOCALES:
        s, bad = load(loc)
        if s is None:
            print("  %-20s %-10s [文件不存在]" % (loc, label))
            continue
        data[loc] = s
        print("  %-20s %-10s %d" % (loc, label, len(s)))
        for b in bad:
            print("      ! %s" % b)

    en = data.get("values")
    if not en:
        print("[ERROR] 读不到英文版")
        return 1

    print("\n相对 values(英文) 的真实缺失:")
    total_missing = 0
    for loc, label in LOCALES:
        if loc == "values":
            continue
        s = data.get(loc)
        if s is None:
            continue
        missing = [k for k in en if k not in s]
        extra = [k for k in s if k not in en]
        total_missing += len(missing)
        print("\n  [%s / %s] 缺 %d 个, 多 %d 个" % (loc, label, len(missing), len(extra)))
        for k in missing:
            print("      - %-52s | %s" % (k, en[k].replace("\n", "\\n")[:70]))
        for k in extra:
            print("      + %-52s | %s" % (k, s[k].replace("\n", "\\n")[:70]))

    print("\n合计缺失: %d" % total_missing)
    return 0


if __name__ == "__main__":
    sys.exit(main())
