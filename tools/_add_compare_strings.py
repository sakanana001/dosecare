#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
搬运"双药对比"功能的 string 资源到合并底座。

来源 (DoseCare 原仓库):
  app/src/main/res/values/strings.xml      = 简体中文
  app/src/main/res/values-en/strings.xml   = 英文
  app/src/main/res/values-ja/strings.xml   = 日文

目标 (合并底座, 语言布局相反):
  values/                 <- values-en   (底座默认语言是英文)
  values-b+zh+Hans/       <- values      (简体)
  values-b+zh+Hant/       <- 繁中 (手工译, 底座有真繁中)
  values-b+yue+Hant+HK/   <- 港繁 (手工译, 底座 yue 是真粤语书面化)
  values-ja/              <- values-ja

79 个 key: 73 个来自 ComparisonView.kt, 6 个来自 DoseCare 的 BottomNavTabs.kt
(对比 tab 的标题/信息按钮/药 A/药 B 选择器标签)。

占位符必须与英文版逐个一致, 脚本会校验。
默认 dry-run, 加 --apply 才写盘。保留各文件自身行尾风格。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WS = os.path.dirname(ROOT)
DOSECARE_RES = os.path.join(WS, "dosecare", "app", "src", "main", "res")
BASE_RES = os.path.join(ROOT, "app", "src", "main", "res")

# 73 个来自 ComparisonView.kt
VIEW_KEYS = [
    "cmp_diff_cyp_ind_a", "cmp_diff_cyp_ind_b", "cmp_diff_cyp_inh_a", "cmp_diff_cyp_inh_b",
    "cmp_diff_elderly_diff", "cmp_diff_half_life_format", "cmp_diff_half_life_medium",
    "cmp_diff_half_life_short", "cmp_diff_half_life_xlong", "cmp_diff_highrisk_agran",
    "cmp_diff_highrisk_eps", "cmp_diff_highrisk_qt", "cmp_diff_highrisk_sedation",
    "cmp_diff_only_a_format", "cmp_diff_only_b_format", "cmp_diff_window_format",
    "cmp_diff_window_no_a", "cmp_diff_window_no_b",
    "cmp_label_agranulocytosis", "cmp_label_anticholinergic", "cmp_label_antidote",
    "cmp_label_elderly_empty", "cmp_label_elderly_val", "cmp_label_eps", "cmp_label_fatal",
    "cmp_label_freq_empty", "cmp_label_freq_val", "cmp_label_hepatic", "cmp_label_items",
    "cmp_label_items_empty", "cmp_label_metabolic", "cmp_label_no_entry", "cmp_label_pathway",
    "cmp_label_pathway_empty", "cmp_label_prolactin", "cmp_label_protein_binding",
    "cmp_label_qtc", "cmp_label_renal", "cmp_label_sedation", "cmp_label_severity",
    "cmp_label_severity_with_doses", "cmp_label_sexual", "cmp_label_toxic",
    "cmp_section_adjust", "cmp_section_adverse", "cmp_section_basic", "cmp_section_cyp",
    "cmp_section_desc_adjust", "cmp_section_desc_adverse", "cmp_section_desc_basic",
    "cmp_section_desc_cyp", "cmp_section_desc_monitor", "cmp_section_desc_pk",
    "cmp_section_desc_window", "cmp_section_monitor", "cmp_section_overdose", "cmp_section_pk",
    "cmp_section_window",
    "compare_critical_inbound", "compare_diff_empty", "compare_diff_title",
    "compare_hint_legend", "compare_lay_disclaimer", "compare_lay_title", "compare_mechanism_auc",
    "cyp_strength_moderate", "cyp_strength_strong", "cyp_strength_weak",
    "risk_level_high", "risk_level_low", "risk_level_medium", "risk_level_very_high",
    "risk_level_very_low",
]

# 6 个来自 DoseCare BottomNavTabs.kt 的对比 tab
TAB_KEYS = [
    "compare_card_title", "compare_info", "compare_info_title", "compare_info_lines",
    "compare_drug_a", "compare_drug_b",
]

KEYS = VIEW_KEYS + TAB_KEYS

# 源 -> 目标 的语言映射
SOURCES = {
    "values-en": "values",                # 英文 = 底座默认
    "values": "values-b+zh+Hans",          # 简体
    "values-ja": "values-ja",              # 日文
}

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')


def read_text(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def parse(path):
    d = {}
    for m in STR_RE.finditer(read_text(path)):
        d.setdefault(m.group(1), m.group(2))
    return d


def ph(s):
    """占位符编号多重集 (排序后)。

    注意: 源仓库三语的同一个 key 里, 占位符的**出现顺序**可能不同
    (例如 cmp_diff_cyp_inh_a: 英文是 %1$s %2$s %4$s %3$s, 中文是 %1$s %2$s %3$s %4$s),
    但编号对应的语义一致 —— Android 的 stringResource(id, a, b, c, d) 按编号取参,
    正文里的出现顺序不影响取值。所以这里比较编号多重集, 不比较顺序。
    """
    return sorted(m.group(0) for m in PH_RE.finditer(s))


def main():
    apply = "--apply" in sys.argv
    if not os.path.isdir(DOSECARE_RES):
        print("[ERROR] 找不到 DoseCare 资源目录: %s" % DOSECARE_RES)
        return 1

    src_cache = {}
    for d in SOURCES:
        p = os.path.join(DOSECARE_RES, d, "strings.xml")
        if not os.path.exists(p):
            print("[ERROR] 找不到源文件: %s" % p)
            return 1
        src_cache[d] = parse(p)

    # 1) 源必须齐全
    for d, table in src_cache.items():
        missing = [k for k in KEYS if k not in table]
        if missing:
            print("[ERROR] DoseCare %s 缺 %d 个 key: %s" % (d, len(missing), missing))
            return 1
    print("源文件齐全: 3 个语言 x %d 个 key" % len(KEYS))

    # 2) 占位符一致性 (以英文为基准)
    en = src_cache["values-en"]
    for d, table in src_cache.items():
        for k in KEYS:
            if ph(en[k]) != ph(table[k]):
                print("[ERROR] %s/%s 占位符不一致 en=%s tr=%s"
                      % (d, k, ph(en[k]), ph(table[k])))
                return 1
    print("占位符校验通过")

    # 3) 写入 3 个可直接映射的语言 (英/简/日)
    for src_dir, dst_dir in SOURCES.items():
        table = src_cache[src_dir]
        path = os.path.join(BASE_RES, dst_dir, "strings.xml")
        if not os.path.exists(path):
            print("[ERROR] 找不到目标文件: %s" % path)
            return 1
        text = read_text(path)
        eol = "\r\n" if "\r\n" in text else "\n"
        existing = parse(path)
        add = [k for k in KEYS if k not in existing]
        over = [k for k in KEYS if k in existing and existing[k] != table[k]]
        same = [k for k in KEYS if k in existing and existing[k] == table[k]]
        print("[%-18s] 新增 %d / 覆盖 %d / 已一致 %d" % (dst_dir, len(add), len(over), len(same)))
        if not apply:
            continue
        if add:
            close = text.rfind("</resources>")
            if close < 0:
                print("[ERROR] %s 找不到 </resources>" % path)
                return 1
            block = ['    <string name="%s">%s</string>' % (k, table[k]) for k in add]
            text = text[:close] + eol.join(block) + eol + text[close:]
        if over:
            for k in over:
                pat = re.compile(r'(?s)(<string name="%s"\s*>)(.*?)(</string>)' % re.escape(k))
                text = pat.sub(lambda m: m.group(1) + table[k] + m.group(3), text, count=1)
        with io.open(path, "w", encoding="utf-8", newline="") as f:
            f.write(text)
        back = parse(path)
        bad = [k for k in KEYS if back.get(k) != table[k]]
        if bad:
            print("[ERROR] %s 回读不一致: %s" % (dst_dir, bad))
            return 1

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
        return 0
    print("\n[OK] 英文/简中/日文 3 个语言已写入并回读校验通过")
    print("     繁中/港繁由 _add_compare_hant_yue_strings.py 单独补")
    return 0


if __name__ == "__main__":
    sys.exit(main())
