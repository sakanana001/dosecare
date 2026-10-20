#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补 / 改药名相关字符串。

1. 新增 drug_name_with_ja_subtitle (五个语言都加, 保持 key 齐平)
   —— 语义与既有的 drug_name_with_zh_subtitle / drug_name_with_en_subtitle 一致:
      "主名 · 副名" 的双名拼接格式, 按"主名是哪种语言"来选 key。
2. 日语 search_hint 改成如实描述现在能搜的字段
   (原来写「中国語 / 英語 / ブランド / 拼音頭文字」, 漏了日文名本身)
3. 日语 interaction_search_hint 改成日文名示例
   (原来「フル / fluvoxamine / クロザピン / F」里「フル」「クロザピン」根本搜不到;
     现在目录里补了 genericNameJa, クロザピン 真的能搜到了)

用法: python _add_ja_name_strings.py [--apply]
"""
import io
import os
import re
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

NEW_KEY = "drug_name_with_ja_subtitle"
NEW_VALUE = "%1$s (%2$s)"

REPLACE = {
    "values-ja": {
        "search_hint": "日本語名 / 英文名 / 中国語名 / ブランド名",
        "interaction_search_hint": "🔍 検索: クロザピン / clozapine / Clozaril / F",
    },
}


def main():
    apply = "--apply" in sys.argv
    errors = []

    for loc in ["values", "values-b+zh+Hans", "values-b+zh+Hant",
                "values-b+yue+Hant+HK", "values-ja"]:
        path = os.path.join(RES, loc, "strings.xml")
        with open(path, "rb") as f:
            raw = f.read()
        has_bom = raw[:3] == b"\xef\xbb\xbf"
        text = raw.decode("utf-8-sig")
        eol = "\r\n" if "\r\n" in text else "\n"
        lines = text.split(eol)

        # 1) 新增 key (插在 drug_name_with_en_subtitle 后面, 保持字母序)
        if not any(('name="%s"' % NEW_KEY) in l for l in lines):
            anchor = None
            for i, l in enumerate(lines):
                if 'name="drug_name_with_en_subtitle"' in l:
                    anchor = i
                    break
            if anchor is None:
                errors.append("%s: 找不到 drug_name_with_en_subtitle" % loc)
                continue
            indent = re.match(r"^(\s*)", lines[anchor]).group(1)
            lines.insert(anchor + 1, '%s<string name="%s">%s</string>' % (indent, NEW_KEY, NEW_VALUE))
            print("[%s] %s + %s" % ("APPLY" if apply else "DRY", loc, NEW_KEY))

        # 2) 替换指定 key 的值
        for key, val in REPLACE.get(loc, {}).items():
            hit = False
            for i, l in enumerate(lines):
                if 'name="%s"' % key in l:
                    m = re.match(r"^(\s*)(<string name=\"%s\">)(.*)(</string>)$" % re.escape(key), l)
                    if not m:
                        errors.append("%s: %s 行格式不认识: %r" % (loc, key, l))
                        break
                    lines[i] = "%s%s%s%s" % (m.group(1), m.group(2), val, m.group(4))
                    hit = True
                    print("[%s] %s %s = %s" % ("APPLY" if apply else "DRY", loc, key, val))
                    break
            if not hit:
                errors.append("%s: 找不到 %s" % (loc, key))

        if errors:
            continue

        new_text = eol.join(lines)
        # 校验
        tree = ET.fromstring(new_text)
        d = {}
        for el in tree.iter():
            if el.tag == "string" and el.get("name"):
                d.setdefault(el.get("name"), el.text or "")
        if d.get(NEW_KEY) != NEW_VALUE:
            errors.append("%s: %s 值不对: %r" % (loc, NEW_KEY, d.get(NEW_KEY)))
        for key, val in REPLACE.get(loc, {}).items():
            if d.get(key) != val:
                errors.append("%s: %s 值不对: %r" % (loc, key, d.get(key)))

        if apply and not errors:
            data = new_text.encode("utf-8")
            if has_bom:
                data = b"\xef\xbb\xbf" + data
            with open(path, "wb") as f:
                f.write(data)

    if errors:
        print("\n[ERROR] %d 个问题:" % len(errors))
        for e in errors:
            print("  - " + e)
        return 1
    print("\n[OK] %s" % ("已写入并回读校验通过" if apply else "dry-run 通过"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
