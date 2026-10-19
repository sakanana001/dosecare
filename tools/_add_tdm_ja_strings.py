#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 TDM / chart 相关的 27 个 string key 补进底座日语目录 values-ja/strings.xml。

来源:
  - 26 个 key: DoseCare 原仓库 app/src/main/res/values-ja/strings.xml (原样搬,含 emoji / \\n / &amp; 转义)
  -  1 个 key: tdm_entry_title (本次合并新增的设置页入口,源仓库无此 key) -> 手工译

脚本幂等: 已存在且内容一致则跳过; 已存在但内容不同则覆盖(以源为准)。
默认 dry-run,加 --apply 才写盘。

写法约定: 保留文件自身行尾风格(底座 res XML 为 LF),不写 BOM。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MERGED = ROOT
DOSECARE = os.path.join(os.path.dirname(ROOT), "dosecare")

TARGET = os.path.join(MERGED, "app", "src", "main", "res", "values-ja", "strings.xml")
SOURCE = os.path.join(DOSECARE, "app", "src", "main", "res", "values-ja", "strings.xml")

# 26 个从 DoseCare values-ja 原样搬运的 key
PORT_KEYS = [
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

# 本次合并新增的 key(源仓库无),手工译
MANUAL = {
    "tdm_entry_title": "血中濃度モニタリング",
}

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')


def read_text(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def write_text(path, content):
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(content)


def detect_eol(text):
    return "\r\n" if "\r\n" in text else "\n"


def main():
    apply = "--apply" in sys.argv

    src_text = read_text(SOURCE)
    src = {}
    for m in STR_RE.finditer(src_text):
        src[m.group(1)] = m.group(2)

    missing = [k for k in PORT_KEYS if k not in src]
    if missing:
        print("[ERROR] DoseCare values-ja 缺少这些 key: %s" % ", ".join(missing))
        return 1

    tgt_text = read_text(TARGET)
    eol = detect_eol(tgt_text)
    existing = {}
    for m in STR_RE.finditer(tgt_text):
        existing[m.group(1)] = m.group(2)

    wanted = {}
    for k in PORT_KEYS:
        wanted[k] = src[k]
    for k, v in MANUAL.items():
        wanted[k] = v

    add, skip, over = [], [], []
    for k in PORT_KEYS + list(MANUAL.keys()):
        if k not in existing:
            add.append(k)
        elif existing[k] == wanted[k]:
            skip.append(k)
        else:
            over.append(k)

    print("目标文件: %s" % TARGET)
    print("行尾风格: %s" % ("CRLF" if eol == "\r\n" else "LF"))
    print("新增 %d / 已一致 %d / 需覆盖 %d" % (len(add), len(skip), len(over)))
    if add:
        print("  新增: %s" % ", ".join(add))
    if over:
        print("  覆盖: %s" % ", ".join(over))

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
        return 0

    if add:
        # 在 </resources> 前插入
        close = tgt_text.rfind("</resources>")
        if close < 0:
            print("[ERROR] 找不到 </resources>")
            return 1
        block = []
        for k in add:
            block.append('    <string name="%s">%s</string>' % (k, wanted[k]))
        ins = eol.join(block) + eol
        tgt_text = tgt_text[:close] + ins + tgt_text[close:]

    if over:
        for k in over:
            pat = re.compile(
                r'(?s)(<string name="%s"\s*>)(.*?)(</string>)' % re.escape(k)
            )
            tgt_text = pat.sub(lambda m: m.group(1) + wanted[k] + m.group(3), tgt_text, count=1)

    write_text(TARGET, tgt_text)

    # 回读校验
    back = {}
    for m in STR_RE.finditer(read_text(TARGET)):
        back[m.group(1)] = m.group(2)
    bad = [k for k in wanted if back.get(k) != wanted[k]]
    if bad:
        print("[ERROR] 回读不一致: %s" % ", ".join(bad))
        return 1
    print("\n[OK] 已写入 %d 个 key,回读校验通过" % len(wanted))
    return 0


if __name__ == "__main__":
    sys.exit(main())
