#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补 `app_language_japanese` 这个 key 到 5 个语言目录。

背景: 第 3 期给底座加了第 5 语言日语(values-ja),但 in-app 语言切换器
的 AppLanguageOption 枚举里没有 JAPANESE,导致:
  1. 设置页语言下拉里选不到日语
  2. AppLanguageOption.fromLocale() 遇到 ja 会 fallback 到 ENGLISH
     (系统 per-app locale 被 app 自己的 AppCompatDelegate 覆盖)
  3. values-ja 里的 171 个译文永远选不到

本脚本只补资源那一半;枚举/labelRes/fromLocale/CJK 偏移的代码改动在
SettingsModels.kt / SettingsUiText.kt / SettingsScreen.kt 里手工完成。

语言名称的惯例是"用母语写自己"(English / 简体中文 / 繁體中文 / 粵語),
所以日语那一格写「日本語」,各语言目录里都是同一个字面量。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

KEY = "app_language_japanese"
VALUE = "日本語"

# 与既有 app_language_* 的值保持一致:各语言目录里都写「日本語」
DIRS = [
    "values",
    "values-b+zh+Hans",
    "values-b+zh+Hant",
    "values-b+yue+Hant+HK",
    "values-ja",
]

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')


def read_text(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def main():
    apply = "--apply" in sys.argv
    for d in DIRS:
        path = os.path.join(RES, d, "strings.xml")
        text = read_text(path)
        eol = "\r\n" if "\r\n" in text else "\n"
        existing = {m.group(1): m.group(2) for m in STR_RE.finditer(text)}
        if KEY in existing:
            print("%-22s 已存在 = %s (跳过)" % (d, existing[KEY]))
            continue
        close = text.rfind("</resources>")
        if close < 0:
            print("[ERROR] %s 找不到 </resources>" % d)
            return 1
        ins = '    <string name="%s">%s</string>%s' % (KEY, VALUE, eol)
        text = text[:close] + ins + text[close:]
        if apply:
            with io.open(path, "w", encoding="utf-8", newline="") as f:
                f.write(text)
            back = read_text(path)
            m = re.search(r'(?s)<string name="%s">(.*?)</string>' % KEY, back)
            if not m or m.group(1) != VALUE:
                print("[ERROR] %s 回读校验失败" % d)
                return 1
            print("%-22s 已写入 + 回读 OK" % d)
        else:
            print("%-22s 待新增" % d)
    if not apply:
        print("\n[dry-run] 加 --apply 写入")
    return 0


if __name__ == "__main__":
    sys.exit(main())
