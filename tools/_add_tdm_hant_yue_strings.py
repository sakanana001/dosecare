#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补 TDM / chart 相关 27 个 key 的繁体中文 (values-b+zh+Hant) 与
粤语书面中文 (values-b+yue+Hant+HK) 译文。

背景: worker 只把 4 语言里的英文/简中做了,繁中/港繁回落成英文。
底座其它合并引入的功能 (如 interaction_entry_title) 在繁中是有译文的,
这里保持一致。港繁不是繁中的复制,底座 yue 目录是真正的粤语书面化
("嘅" "就快" "唔" 等),所以两份译文分别撰写。

占位符必须与英文版完全一致 (脚本会校验)。
默认 dry-run,加 --apply 才写盘。保留文件自身行尾风格 (底座 res XML 为 LF)。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

HANT_DIR = os.path.join(RES, "values-b+zh+Hant")
YUE_DIR = os.path.join(RES, "values-b+yue+Hant+HK")
EN_FILE = os.path.join(RES, "values", "strings.xml")

KEYS = [
    "tdm_entry_title",
    "tdm_title",
    "tdm_data_source_info",
    "tdm_no_drugs",
    "tdm_data_source_title",
    "tdm_data_source_lines",
    "tdm_limitations",
    "tdm_limitations_lines",
    "tdm_predict_n_drugs",
    "tdm_add_drug_to_chart",
    "tdm_add_drug_n",
    "tdm_add",
    "tdm_dose_mg",
    "tdm_weight_kg",
    "tdm_freq",
    "tdm_chart_title",
    "tdm_hide_cd",
    "tdm_show_cd",
    "tdm_no_window",
    "chart_title",
    "chart_window_label",
    "chart_reset_view",
    "chart_status_format",
    "chart_status_low",
    "chart_status_high",
    "chart_status_in_window",
    "chart_status_not_comparable",
]

HANT = {
    "tdm_entry_title": "血中濃度監測",
    "tdm_title": "血中濃度推估 (TDM)",
    "tdm_data_source_info": "資料來源與校準說明",
    "tdm_no_drugs": "暫無可 TDM 監測的藥物(需要 therapeuticWindow 欄位)",
    "tdm_data_source_title": "📚 資料來源與校準",
    "tdm_data_source_lines": "• PK 參數: AGNP 2017 (Therapeutic Drug Monitoring in Psychiatry) + FDA DailyMed 藥品標籤\\n• CYP 譜: Flockhart Table (Indiana University)\\n• 治療窗: AGNP 2017 共識指引\\n• 體重調整: Vd = vdLPerKg × weight(假設線性)",
    "tdm_limitations": "⚠️ 限制",
    "tdm_limitations_lines": "• 1 房室口服模型 — 對非線性藥物(苯妥英)不準確,對腎臟清除藥物(鋰)僅為近似\\n• 群體平均參數 — 個體差異 2-3 倍(CYP 基因型 / 抽菸 / 年齡 / 腎臟 / 肝臟)\\n• 多藥曲線僅顯示濃度疊加,不模擬 CYP 競爭/酵素抑制動力學\\n• v0.4 不替代醫師判斷;實際用藥請遵醫囑或諮詢臨床藥師",
    "tdm_predict_n_drugs": "推估 %1$d 藥血中濃度",
    "tdm_add_drug_to_chart": "將藥物加入圖表",
    "tdm_add_drug_n": "%1$d 個藥可選 (有/無 TDM 均可)",
    "tdm_add": "加入",
    "tdm_dose_mg": "劑量 (mg)",
    "tdm_weight_kg": "體重 (kg)",
    "tdm_freq": "頻率:",
    "tdm_chart_title": "多藥穩態濃度-時間曲線 (可點擊/拖曳/縮放)",
    "tdm_hide_cd": "隱藏",
    "tdm_show_cd": "顯示",
    "tdm_no_window": "TDM (無治療窗)",
    "chart_title": "濃度-時間曲線",
    "chart_window_label": "治療窗 %1$d-%2$d",
    "chart_reset_view": "🔄 重設視圖 (%1$s)",
    "chart_status_format": "🔍 %1$.1fx · 拖曳 / 縮放以放大",
    "chart_status_low": "⛔ 偏低",
    "chart_status_high": "⚠️ 偏高",
    "chart_status_in_window": "✓ 在治療窗內",
    "chart_status_not_comparable": "· 無法比較",
}

# 粤语书面化: "嘅" 代替 "的", "唔" 代替 "不", "就快/仲未" 等口语表达
YUE = {
    "tdm_entry_title": "血中濃度監測",
    "tdm_title": "血中濃度估算 (TDM)",
    "tdm_data_source_info": "資料來源同校準說明",
    "tdm_no_drugs": "暫時冇可做 TDM 監測嘅藥物(需要 therapeuticWindow 欄位)",
    "tdm_data_source_title": "📚 資料來源同校準",
    "tdm_data_source_lines": "• PK 參數: AGNP 2017 (Therapeutic Drug Monitoring in Psychiatry) + FDA DailyMed 藥品標籤\\n• CYP 譜: Flockhart Table (Indiana University)\\n• 治療窗: AGNP 2017 共識指引\\n• 體重調整: Vd = vdLPerKg × weight(假設線性)",
    "tdm_limitations": "⚠️ 限制",
    "tdm_limitations_lines": "• 1 房室口服模型 — 對非線性藥物(苯妥英)唔準確,對腎臟清除藥物(鋰)只係近似\\n• 群體平均參數 — 個體差異 2-3 倍(CYP 基因型 / 食煙 / 年齡 / 腎臟 / 肝臟)\\n• 多藥曲線只係顯示濃度疊加,唔會模擬 CYP 競爭/酵素抑制動力學\\n• v0.4 唔可以代替醫生判斷;實際食藥請聽醫生或者搵臨床藥劑師",
    "tdm_predict_n_drugs": "估算 %1$d 藥血中濃度",
    "tdm_add_drug_to_chart": "將藥物加入圖表",
    "tdm_add_drug_n": "%1$d 個藥可以揀 (有/無 TDM 都可以)",
    "tdm_add": "加入",
    "tdm_dose_mg": "劑量 (mg)",
    "tdm_weight_kg": "體重 (kg)",
    "tdm_freq": "頻率:",
    "tdm_chart_title": "多藥穩態濃度-時間曲線 (可以點擊/拖曳/縮放)",
    "tdm_hide_cd": "隱藏",
    "tdm_show_cd": "顯示",
    "tdm_no_window": "TDM (冇治療窗)",
    "chart_title": "濃度-時間曲線",
    "chart_window_label": "治療窗 %1$d-%2$d",
    "chart_reset_view": "🔄 重設畫面 (%1$s)",
    "chart_status_format": "🔍 %1$.1fx · 拖曳 / 縮放嚟放大",
    "chart_status_low": "⛔ 偏低",
    "chart_status_high": "⚠️ 偏高",
    "chart_status_in_window": "✓ 喺治療窗範圍內",
    "chart_status_not_comparable": "· 冇得比較",
}

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')
# 占位符: %1$d / %1$s / %1$.1f / %1$d-%2$d
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')


def read_text(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def write_text(path, content):
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(content)


def parse(path):
    d = {}
    for m in STR_RE.finditer(read_text(path)):
        d[m.group(1)] = m.group(2)
    return d


def ph_list(s):
    """按出现顺序返回占位符规格列表, 顺序和数量都要一致。"""
    out = []
    for m in PH_RE.finditer(s):
        out.append(m.group(0))
    return out


def apply_to_dir(dirpath, table, label):
    path = os.path.join(dirpath, "strings.xml")
    text = read_text(path)
    eol = "\r\n" if "\r\n" in text else "\n"
    existing = parse(path)

    add, over, same = [], [], []
    for k in KEYS:
        if k not in existing:
            add.append(k)
        elif existing[k] == table[k]:
            same.append(k)
        else:
            over.append(k)

    print("[%s] 行尾=%s | 新增 %d / 覆盖 %d / 已一致 %d"
          % (label, "CRLF" if eol == "\r\n" else "LF", len(add), len(over), len(same)))

    if add:
        close = text.rfind("</resources>")
        if close < 0:
            print("[ERROR] 找不到 </resources> in %s" % path)
            return None
        block = ['    <string name="%s">%s</string>' % (k, table[k]) for k in add]
        text = text[:close] + eol.join(block) + eol + text[close:]

    if over:
        for k in over:
            pat = re.compile(r'(?s)(<string name="%s"\s*>)(.*?)(</string>)' % re.escape(k))
            text = pat.sub(lambda m: m.group(1) + table[k] + m.group(3), text, count=1)

    return path, text, eol


def main():
    apply = "--apply" in sys.argv

    en = parse(EN_FILE)

    # 占位符一致性校验: 译文占位符必须与英文版逐个一致
    bad = []
    for k in KEYS:
        if k not in en:
            bad.append("%s: 英文版缺此 key" % k)
            continue
        for label, table in (("zh+Hant", HANT), ("yue+Hant+HK", YUE)):
            if ph_list(en[k]) != ph_list(table[k]):
                bad.append("%s/%s: 占位符不匹配 en=%s tr=%s"
                           % (k, label, ph_list(en[k]), ph_list(table[k])))
    if bad:
        print("[ERROR] 占位符校验失败:")
        for b in bad:
            print("  - " + b)
        return 1
    print("占位符校验通过 (%d 个 key)" % len(KEYS))

    results = []
    for dirpath, table, label in ((HANT_DIR, HANT, "zh+Hant"), (YUE_DIR, YUE, "yue+Hant+HK")):
        r = apply_to_dir(dirpath, table, label)
        if r is None:
            return 1
        results.append(r)

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
        return 0

    for path, text, eol in results:
        write_text(path, text)
        back = parse(path)
        miss = [k for k in KEYS if k not in back]
        if miss:
            print("[ERROR] %s 回读缺 key: %s" % (path, miss))
            return 1
        # 校验行尾没被搞乱
        raw = read_text(path)
        if ("\r\n" in raw) != (eol == "\r\n"):
            print("[ERROR] %s 行尾风格被改变" % path)
            return 1
        print("[OK] %s 已写入,回读校验通过" % os.path.basename(os.path.dirname(path)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
