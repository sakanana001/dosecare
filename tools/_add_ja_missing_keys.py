#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补日语目录漏掉的 13 个 key。

来源: 第 3 期日语搬运时按当时的清单做了 144 个 key, 但没覆盖到:
  - 第 2 期引入的 药理档案绑定 (dossier_*) 与 相互作用页的一键导入
    (interaction_use_my_regimen*) —— 8 个
  - 语言下拉的 4 个语言名 (app_language_*) —— 语言名各语言写法一致
    (English / 简体中文 / 繁體中文 / 粵語), 日语目录直接用同样的值,
    免得日语界面里语言下拉只有一项是本地化的、其余回落英文

app_language_japanese 已经由 _add_app_language_japanese.py 加过了。

默认 dry-run, 加 --apply 才写盘。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
JA = os.path.join(RES, "values-ja", "strings.xml")
EN = os.path.join(RES, "values", "strings.xml")

JA_TABLE = {
    # 药理档案绑定 (第 2 期)
    "dossier_section_title": "薬理情報",
    "dossier_picker_label": "カタログと紐づけ",
    "dossier_search_placeholder": "202 種の薬を名前か拼音で検索",
    "dossier_summary_format": "%1$s · %2$s",
    "dossier_no_indications": "適応症記載なし",
    "dossier_unlink_action": "カタログとの紐づけを解除",
    # 相互作用页的一键导入 (第 2 期)
    "interaction_entry_title": "薬物相互作用",
    "interaction_use_my_regimen": "現在の処方を使う",
    "interaction_use_my_regimen_n": "現在の処方を使う(%1$d)",
    # 语言名 (各语言写法一致)
    "app_language_english": "English",
    "app_language_simplified_chinese": "简体中文",
    "app_language_traditional_chinese": "繁體中文",
    "app_language_cantonese": "粵語",
}

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')


def read_text(p):
    with io.open(p, "r", encoding="utf-8", newline="") as f:
        return f.read()


def parse(p):
    d = {}
    for m in STR_RE.finditer(read_text(p)):
        d.setdefault(m.group(1), m.group(2))
    return d


def main():
    apply = "--apply" in sys.argv
    ja = parse(JA)
    en = parse(EN)

    add = [k for k in JA_TABLE if k not in ja]
    over = [k for k in JA_TABLE if k in ja and ja[k] != JA_TABLE[k]]
    same = [k for k in JA_TABLE if k in ja and ja[k] == JA_TABLE[k]]
    print("新增 %d / 覆盖 %d / 已一致 %d" % (len(add), len(over), len(same)))

    # 占位符校验
    bad = []
    for k in JA_TABLE:
        if k not in en:
            bad.append("%s: 英文版缺" % k)
            continue
        a = sorted(m.group(0) for m in PH_RE.finditer(en[k]))
        b = sorted(m.group(0) for m in PH_RE.finditer(JA_TABLE[k]))
        if a != b:
            bad.append("%s: 占位符不一致 en=%s ja=%s" % (k, a, b))
    if bad:
        print("[ERROR] 校验失败:")
        for b in bad:
            print("  - " + b)
        return 1
    print("占位符校验通过")

    if not apply:
        print("[dry-run] 加 --apply 写入")
        return 0

    text = read_text(JA)
    eol = "\r\n" if "\r\n" in text else "\n"
    if add:
        close = text.rfind("</resources>")
        block = ['    <string name="%s">%s</string>' % (k, JA_TABLE[k]) for k in add]
        text = text[:close] + eol.join(block) + eol + text[close:]
    if over:
        for k in over:
            pat = re.compile(r'(?s)(<string name="%s"\s*>)(.*?)(</string>)' % re.escape(k))
            text = pat.sub(lambda m: m.group(1) + JA_TABLE[k] + m.group(3), text, count=1)
    with io.open(JA, "w", encoding="utf-8", newline="") as f:
        f.write(text)

    back = parse(JA)
    miss = [k for k in JA_TABLE if back.get(k) != JA_TABLE[k]]
    if miss:
        print("[ERROR] 回读不一致: %s" % miss)
        return 1
    print("[OK] 日语目录已补齐并回读校验通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
