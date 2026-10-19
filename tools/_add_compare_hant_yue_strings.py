#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补"双药对比" 79 个 key 的繁体中文 (values-b+zh+Hant) 与
粤语书面中文 (values-b+yue+Hant+HK) 译文。

英文/简中/日文由 _add_compare_strings.py 从 DoseCare 原仓库搬完。
本脚本补剩下两个语言。

港繁不是繁中的复制 —— 底座 yue 目录本身就是真正的粤语书面化
("嘅" "就快" "唔" "仲未" 等), 所以两份译文分别撰写。

占位符编号必须与英文版完全一致 (多重集比较), 脚本会校验。
默认 dry-run, 加 --apply 才写盘。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
EN_FILE = os.path.join(RES, "values", "strings.xml")

KEYS = [
    "cmp_section_basic", "cmp_section_desc_basic", "cmp_section_pk", "cmp_section_desc_pk",
    "cmp_section_window", "cmp_section_desc_window", "cmp_section_cyp", "cmp_section_desc_cyp",
    "cmp_section_adverse", "cmp_section_desc_adverse", "cmp_section_adjust",
    "cmp_section_desc_adjust", "cmp_section_monitor", "cmp_section_desc_monitor",
    "cmp_section_overdose",
    "cmp_label_qtc", "cmp_label_renal", "cmp_label_hepatic", "cmp_label_severity",
    "cmp_label_severity_with_doses", "cmp_label_no_entry", "cmp_label_items",
    "cmp_label_items_empty", "cmp_label_freq_val", "cmp_label_freq_empty",
    "cmp_label_elderly_val", "cmp_label_elderly_empty", "cmp_label_pathway",
    "cmp_label_pathway_empty", "cmp_label_antidote", "cmp_label_toxic", "cmp_label_fatal",
    "cmp_label_anticholinergic", "cmp_label_agranulocytosis", "cmp_label_metabolic",
    "cmp_label_sedation", "cmp_label_sexual", "cmp_label_prolactin",
    "cmp_label_protein_binding", "cmp_label_eps",
    "cmp_diff_window_format", "cmp_diff_window_no_a", "cmp_diff_window_no_b",
    "cmp_diff_half_life_format", "cmp_diff_half_life_short", "cmp_diff_half_life_medium",
    "cmp_diff_half_life_xlong", "cmp_diff_only_a_format", "cmp_diff_only_b_format",
    "cmp_diff_cyp_ind_a", "cmp_diff_cyp_ind_b", "cmp_diff_cyp_inh_a", "cmp_diff_cyp_inh_b",
    "cmp_diff_elderly_diff", "cmp_diff_highrisk_qt", "cmp_diff_highrisk_eps",
    "cmp_diff_highrisk_agran", "cmp_diff_highrisk_sedation",
    "risk_level_very_low", "risk_level_low", "risk_level_medium", "risk_level_high",
    "risk_level_very_high",
    "cyp_strength_weak", "cyp_strength_moderate", "cyp_strength_strong",
    "compare_lay_title", "compare_lay_disclaimer", "compare_diff_title", "compare_diff_empty",
    "compare_hint_legend", "compare_critical_inbound", "compare_mechanism_auc",
    "compare_card_title", "compare_info", "compare_info_title", "compare_info_lines",
    "compare_drug_a", "compare_drug_b",
]

HANT = {
    "cmp_section_basic": "基本資訊",
    "cmp_section_desc_basic": "藥物generic/品牌名與WHO ATC分類代碼",
    "cmp_section_pk": "藥代動力學",
    "cmp_section_desc_pk": "吸收速率、清除速率與半衰期",
    "cmp_section_window": "治療窗",
    "cmp_section_desc_window": "安全血中濃度範圍",
    "cmp_section_cyp": "CYP角色",
    "cmp_section_desc_cyp": "受質、抑制劑與誘導劑",
    "cmp_section_adverse": "副作用",
    "cmp_section_desc_adverse": "QTc延長、代謝症候群、EPS與鎮靜等風險",
    "cmp_section_adjust": "劑量調整",
    "cmp_section_desc_adjust": "腎臟、肝臟與老年人族群調整",
    "cmp_section_monitor": "監測",
    "cmp_section_desc_monitor": "監測頻率與監測項目",
    "cmp_section_overdose": "藥物過量",
    "cmp_label_qtc": "QTc延長:%1$s",
    "cmp_label_renal": "腎臟:%1$s",
    "cmp_label_hepatic": "肝臟:%1$s",
    "cmp_label_severity": "嚴重度:%1$s",
    "cmp_label_severity_with_doses": "嚴重度:%1$s · %2$s%3$s",
    "cmp_label_no_entry": "未填寫",
    "cmp_label_items": "項目:%1$s",
    "cmp_label_items_empty": "項目:—",
    "cmp_label_freq_val": "頻率:%1$s",
    "cmp_label_freq_empty": "頻率:—",
    "cmp_label_elderly_val": "老年人:%1$s",
    "cmp_label_elderly_empty": "老年人:—",
    "cmp_label_pathway": "代謝途徑:%1$s",
    "cmp_label_pathway_empty": "未標註",
    "cmp_label_antidote": " · 解毒劑:%1$s",
    "cmp_label_toxic": "中毒 ≥%1$d mg",
    "cmp_label_fatal": "致死 ≥%1$d mg",
    "cmp_label_anticholinergic": "抗膽鹼負荷:%1$d / 3",
    "cmp_label_agranulocytosis": "顆粒球缺乏:%1$s",
    "cmp_label_metabolic": "代謝症候群:%1$s",
    "cmp_label_sedation": "鎮靜:%1$s",
    "cmp_label_sexual": "性功能障礙:%1$s",
    "cmp_label_prolactin": "高泌乳素血症:%1$s",
    "cmp_label_protein_binding": "蛋白質結合 %1$d%%",
    "cmp_label_eps": "EPS:%1$s",
    "cmp_diff_window_format": "• 治療窗(安全血中濃度範圍):%1$s = %2$s,%3$s = %4$s(同單位)",
    "cmp_diff_window_no_a": "• %1$s 沒有定義治療窗;%2$s 需要 TDM 監測",
    "cmp_diff_window_no_b": "• %1$s 沒有定義治療窗;%2$s 需要 TDM 監測",
    "cmp_diff_half_life_format": "• 半衰期:%1$s ≈ %2$dh(%3$s),%4$s ≈ %5$dh(%6$s)",
    "cmp_diff_half_life_short": "短效",
    "cmp_diff_half_life_medium": "長效",
    "cmp_diff_half_life_xlong": "超長效",
    "cmp_diff_only_a_format": "• %1$s 獨有高風險:%2$s",
    "cmp_diff_only_b_format": "• %1$s 獨有高風險:%2$s",
    "cmp_diff_cyp_ind_a": "• %1$s 是強效 CYP 誘導劑,會令 %2$s 失效(需增加劑量或改用其他方案)",
    "cmp_diff_cyp_ind_b": "• %1$s 是強效 CYP 誘導劑,會令 %2$s 失效(需增加劑量或改用其他方案)",
    "cmp_diff_cyp_inh_a": "• %1$s 是 CYP 抑制剂而 %2$s 不是 → 聯用時會透過 %3$s 令 %4$s 濃度升高",
    "cmp_diff_cyp_inh_b": "• %1$s 是 CYP 抑制剂而 %2$s 不是 → 聯用時會透過 %3$s 令 %4$s 濃度升高",
    "cmp_diff_elderly_diff": "• 老年人使用須小心:藥品標籤不一致;請諮詢醫師",
    "cmp_diff_highrisk_qt": "QT 延長",
    "cmp_diff_highrisk_eps": "EPS",
    "cmp_diff_highrisk_agran": "顆粒球缺乏",
    "cmp_diff_highrisk_sedation": "鎮靜",
    "risk_level_very_low": "極低",
    "risk_level_low": "低",
    "risk_level_medium": "中",
    "risk_level_high": "高",
    "risk_level_very_high": "極高",
    "cyp_strength_weak": "弱",
    "cyp_strength_moderate": "中度",
    "cyp_strength_strong": "強",
    "compare_lay_title": "📖 白話模式",
    "compare_lay_disclaimer": "⚠️ 所有參數都應以實際藥品標籤/化驗數值為準。\n用藥調整與相互作用警示,請諮詢醫師或臨床藥劑師。",
    "compare_diff_title": "📖 關鍵差異(白話)",
    "compare_diff_empty": "兩種藥物在主要指標上沒有明顯差異。實際使用請遵醫囑。",
    "compare_hint_legend": "💡 A → B 代表藥 A 會改變藥 B 的濃度;數字越大代表變化越大(1.5× 屬輕微,5×+ 屬嚴重)",
    "compare_critical_inbound": "相互影響(對方在 criticalInteractions 內引用了本藥)",
    "compare_mechanism_auc": "%1$s · AUC 變化 %2$s - %3$s ×",
    "compare_card_title": "雙藥規格對比",
    "compare_info": "對比說明",
    "compare_info_title": "ℹ️ 雙藥對比說明",
    "compare_info_lines": "• 選兩種藥,跨 7 個區塊對比(基本資訊/藥代動力學/治療窗/CYP 角色/副作用/劑量調整/監測)\n• 相同數值會自動合併,不同數值以兩欄並列\n• 治療窗/CYP 譜參考 AGNP 2017 + Flockhart Table\n• 副作用嚴重度:VERY_LOW / LOW / MEDIUM / HIGH / VERY_HIGH\n• 本工具只顯示規格差異;臨床決策仍要考慮患者實際狀況",
    "compare_drug_a": "藥物 A",
    "compare_drug_b": "藥物 B",
}

# 粤语书面化
YUE = {
    "cmp_section_basic": "基本資料",
    "cmp_section_desc_basic": "藥物generic/品牌名同WHO ATC分類代碼",
    "cmp_section_pk": "藥代動力學",
    "cmp_section_desc_pk": "吸收速率、清除速率同半衰期",
    "cmp_section_window": "治療窗",
    "cmp_section_desc_window": "安全血中濃度範圍",
    "cmp_section_cyp": "CYP角色",
    "cmp_section_desc_cyp": "受質、抑制劑同誘導劑",
    "cmp_section_adverse": "副作用",
    "cmp_section_desc_adverse": "QTc延長、代謝症候群、EPS同鎮靜等風險",
    "cmp_section_adjust": "劑量調整",
    "cmp_section_desc_adjust": "腎臟、肝臟同老年人族群調整",
    "cmp_section_monitor": "監測",
    "cmp_section_desc_monitor": "監測頻率同監測項目",
    "cmp_section_overdose": "藥物過量",
    "cmp_label_qtc": "QTc延長:%1$s",
    "cmp_label_renal": "腎臟:%1$s",
    "cmp_label_hepatic": "肝臟:%1$s",
    "cmp_label_severity": "嚴重程度:%1$s",
    "cmp_label_severity_with_doses": "嚴重程度:%1$s · %2$s%3$s",
    "cmp_label_no_entry": "未填",
    "cmp_label_items": "項目:%1$s",
    "cmp_label_items_empty": "項目:—",
    "cmp_label_freq_val": "頻率:%1$s",
    "cmp_label_freq_empty": "頻率:—",
    "cmp_label_elderly_val": "老年人:%1$s",
    "cmp_label_elderly_empty": "老年人:—",
    "cmp_label_pathway": "代謝途徑:%1$s",
    "cmp_label_pathway_empty": "未標註",
    "cmp_label_antidote": " · 解毒劑:%1$s",
    "cmp_label_toxic": "中毒 ≥%1$d mg",
    "cmp_label_fatal": "致死 ≥%1$d mg",
    "cmp_label_anticholinergic": "抗膽鹼負荷:%1$d / 3",
    "cmp_label_agranulocytosis": "顆粒球缺乏:%1$s",
    "cmp_label_metabolic": "代謝症候群:%1$s",
    "cmp_label_sedation": "鎮靜:%1$s",
    "cmp_label_sexual": "性功能障礙:%1$s",
    "cmp_label_prolactin": "高泌乳素血症:%1$s",
    "cmp_label_protein_binding": "蛋白質結合 %1$d%%",
    "cmp_label_eps": "EPS:%1$s",
    "cmp_diff_window_format": "• 治療窗(安全血中濃度範圍):%1$s = %2$s,%3$s = %4$s(同單位)",
    "cmp_diff_window_no_a": "• %1$s 冇定義治療窗;%2$s 要做 TDM 監測",
    "cmp_diff_window_no_b": "• %1$s 冇定義治療窗;%2$s 要做 TDM 監測",
    "cmp_diff_half_life_format": "• 半衰期:%1$s ≈ %2$dh(%3$s),%4$s ≈ %5$dh(%6$s)",
    "cmp_diff_half_life_short": "短效",
    "cmp_diff_half_life_medium": "長效",
    "cmp_diff_half_life_xlong": "超長效",
    "cmp_diff_only_a_format": "• %1$s 獨有高風險:%2$s",
    "cmp_diff_only_b_format": "• %1$s 獨有高風險:%2$s",
    "cmp_diff_cyp_ind_a": "• %1$s 係強效 CYP 誘導劑,會令 %2$s 失效(要加劑量或者轉用其他方案)",
    "cmp_diff_cyp_ind_b": "• %1$s 係強效 CYP 誘導劑,會令 %2$s 失效(要加劑量或者轉用其他方案)",
    "cmp_diff_cyp_inh_a": "• %1$s 係 CYP 抑制劑但 %2$s 唔係 → 併用時會經 %3$s 令 %4$s 濃度升高",
    "cmp_diff_cyp_inh_b": "• %1$s 係 CYP 抑制劑但 %2$s 唔係 → 併用時會經 %3$s 令 %4$s 濃度升高",
    "cmp_diff_elderly_diff": "• 老年人用藥要小心:藥品標籤唔一致;請問醫生",
    "cmp_diff_highrisk_qt": "QT 延長",
    "cmp_diff_highrisk_eps": "EPS",
    "cmp_diff_highrisk_agran": "顆粒球缺乏",
    "cmp_diff_highrisk_sedation": "鎮靜",
    "risk_level_very_low": "極低",
    "risk_level_low": "低",
    "risk_level_medium": "中",
    "risk_level_high": "高",
    "risk_level_very_high": "極高",
    "cyp_strength_weak": "弱",
    "cyp_strength_moderate": "中度",
    "cyp_strength_strong": "強",
    "compare_lay_title": "📖 白話模式",
    "compare_lay_disclaimer": "⚠️ 所有參數都應該以實際藥品標籤/化驗數值為準。\n調藥同相互作用警示,請問醫生或者臨床藥劑師。",
    "compare_diff_title": "📖 關鍵差異(白話)",
    "compare_diff_empty": "兩隻藥喺主要指標上冇明顯分別。實際食藥請聽醫生。",
    "compare_hint_legend": "💡 A → B 即係藥 A 會改變藥 B 嘅濃度;數字越大代表變化越大(1.5× 算輕微,5×+ 算嚴重)",
    "compare_critical_inbound": "互相影響(對方喺 criticalInteractions 入面引用咗呢隻藥)",
    "compare_mechanism_auc": "%1$s · AUC 變化 %2$s - %3$s ×",
    "compare_card_title": "雙藥規格對比",
    "compare_info": "對比說明",
    "compare_info_title": "ℹ️ 雙藥對比說明",
    "compare_info_lines": "• 揀兩隻藥,跨 7 個部分對比(基本資料/藥代動力學/治療窗/CYP 角色/副作用/劑量調整/監測)\n• 一樣嘅數值會自動合併,唔同嘅數值用兩欄並排\n• 治療窗/CYP 譜參考 AGNP 2017 + Flockhart Table\n• 副作用嚴重程度:VERY_LOW / LOW / MEDIUM / HIGH / VERY_HIGH\n• 呢個工具淨係顯示規格差異;臨床決定仲要睇返病人實際情況",
    "compare_drug_a": "藥物 A",
    "compare_drug_b": "藥物 B",
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


def ph(s):
    return sorted(m.group(0) for m in PH_RE.finditer(s))


def main():
    apply = "--apply" in sys.argv
    en = parse(EN_FILE)

    missing_en = [k for k in KEYS if k not in en]
    if missing_en:
        print("[ERROR] 英文版缺 %d 个 key: %s" % (len(missing_en), missing_en))
        return 1

    bad = []
    for label, table in (("zh+Hant", HANT), ("yue+Hant+HK", YUE)):
        for k in KEYS:
            if k not in table:
                bad.append("%s: 译文表缺 %s" % (label, k))
            elif ph(en[k]) != ph(table[k]):
                bad.append("%s/%s: 占位符不一致 en=%s tr=%s"
                           % (label, k, ph(en[k]), ph(table[k])))
    if bad:
        print("[ERROR] 校验失败:")
        for b in bad:
            print("  - " + b)
        return 1
    print("校验通过: %d 个 key, 繁中/港繁占位符与英文一致" % len(KEYS))

    for dirpath, table, label in (
        (os.path.join(RES, "values-b+zh+Hant"), HANT, "zh+Hant"),
        (os.path.join(RES, "values-b+yue+Hant+HK"), YUE, "yue+Hant+HK"),
    ):
        path = os.path.join(dirpath, "strings.xml")
        text = read_text(path)
        eol = "\r\n" if "\r\n" in text else "\n"
        existing = parse(path)
        add = [k for k in KEYS if k not in existing]
        over = [k for k in KEYS if k in existing and existing[k] != table[k]]
        same = [k for k in KEYS if k in existing and existing[k] == table[k]]
        print("[%-14s] 新增 %d / 覆盖 %d / 已一致 %d" % (label, len(add), len(over), len(same)))
        if not apply:
            continue
        if add:
            close = text.rfind("</resources>")
            if close < 0:
                print("[ERROR] 找不到 </resources> in %s" % path)
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
        b = [k for k in KEYS if back.get(k) != table[k]]
        if b:
            print("[ERROR] %s 回读不一致: %s" % (label, b))
            return 1
        print("  -> 写入完成,回读校验通过")

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
    return 0


if __name__ == "__main__":
    sys.exit(main())
