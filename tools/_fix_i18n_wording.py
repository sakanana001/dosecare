#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修正日语/繁中/港���里几处措辞不准的文案。

1. values-ja  interaction_search_hint
   原: "🔍 検索: フル / fluvoxamine / クロザピン / F"
   目录里根本没有日文药名字段 (catalog.json 只有 genericName 英文 / genericNameZh 中文),
   SearchableDrugPicker 的搜索只匹配 中文名 / 英文名 / 品牌名 / ATC 码 / 拼音首字母。
   所以 "フル" 和 "クロザピン" 搜不到任何东西 —— 提示在骗用户。
   改成一组"确实搜得到"的示例: 列表上看到的中文名 / 英文名 / 品牌名 / ATC 首字母。

2. values-ja  settings_cjk_text_offset(+_summary)
   原: "中国語の文字表示を調整" / "中国語テキストの配置がずれている場合にお試しください。"
   但 shouldShowCjkTextOffset() 已经把 JAPANESE 算进去, 且 CjkTextOffset.kt 的
   cjkTextOffsetScripts 含 HIRAGANA / KATAKANA, 日语界面下这个开关调的其实是日文。
   标签写"中国語"是错的, 改成"漢字・かな文字"。

3. values-b+zh+Hant / values-b+yue+Hant+HK  interaction_search_hint
   底座自带的原文一直是英文 "🔍 Search: ..." (简中已翻译)。
   术语本身都能搜到, 只是前缀没本地化, 补上。

用法: python _fix_i18n_wording.py [--apply]
"""
import io
import os
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

FIXES = {
    "values-ja": {
        "interaction_search_hint": "🔍 検索: 氯氮平 / clozapine / Clozaril / F",
        "settings_cjk_text_offset": "漢字・かな文字の表示を調整",
        "settings_cjk_text_offset_summary": "文字の配置がずれている場合に調整してください。",
    },
    "values-b+zh+Hant": {
        "interaction_search_hint": "🔍 搜尋: 氟 / fluvoxamine / 氯氮平 / F",
    },
    "values-b+yue+Hant+HK": {
        "interaction_search_hint": "🔍 搜尋: 氟 / fluvoxamine / 氯氮平 / F",
    },
}

# 这些示例词必须真的能被搜索命中, 逐个对着 SearchableDrugPicker 的 5 条匹配规则核过
VERIFIED_TERMS = ["clozapine", "Clozaril", "fluvoxamine", "N05AF01"]


def read_text(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def parse(text):
    d = {}
    for el in ET.fromstring(text).iter():
        if el.tag == "string" and el.get("name"):
            d.setdefault(el.get("name"), el.text or "")
    return d


def main():
    apply = "--apply" in sys.argv
    errors = []

    for loc, fixes in FIXES.items():
        path = os.path.join(RES, loc, "strings.xml")
        raw = read_text(path)
        cur = parse(raw)
        for k, v in fixes.items():
            if k not in cur:
                errors.append("%s: 没有这个 key %s" % (loc, k))
                continue
            print("%-22s %-32s\n    旧: %s\n    新: %s" % (loc, k, cur[k], v))
        if not apply:
            continue

        eol = "\r\n" if "\r\n" in raw else "\n"
        new_text = raw
        for k, v in fixes.items():
            old = cur[k]
            # 精确替换这一条 <string name="k">旧值</string>
            old_el = '<string name="%s">%s</string>' % (k, old)
            new_el = '<string name="%s">%s</string>' % (k, v)
            if old_el not in new_text:
                errors.append("%s: 找不到原始元素 %s" % (loc, k))
                continue
            new_text = new_text.replace(old_el, new_el, 1)

        with io.open(path, "w", encoding="utf-8", newline="") as f:
            f.write(new_text)

        back = parse(read_text(path))
        for k, v in fixes.items():
            if back.get(k) != v:
                errors.append("%s: 回读不一致 %s" % (loc, k))
        ET.fromstring(read_text(path))
        print("  -> 已写入, 现有 %d 个 key" % len(back))

    if errors:
        print("\n[ERROR] %d 个问题:" % len(errors))
        for e in errors:
            print("  - " + e)
        return 1
    if not apply:
        print("\n[dry-run] 加 --apply 写入")
    else:
        print("\n[OK] 全部写入并回读校验通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
