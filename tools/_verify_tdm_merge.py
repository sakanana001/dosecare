#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
TDM 合并收尾自检:
  1. 5 个语言目录的 27 个 TDM/chart key 全部存在
  2. 占位符与英文版逐个一致
  3. 无重复 key / 无 BOM / 行尾风格与原文件一致
  4. 代码里没有 "TDM TDM" 重复 Text
  5. 每个带占位符的 key 都真的带参调用 (防字面量 %1$d 显示在 UI)
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SRC = os.path.join(ROOT, "app", "src", "main", "java", "com", "dosecare", "app")

LOCALES = {
    "values": "en",
    "values-b+zh+Hans": "zh-Hans",
    "values-b+zh+Hant": "zh-Hant",
    "values-b+yue+Hant+HK": "yue-Hant-HK",
    "values-ja": "ja",
}

KEYS = [
    "tdm_entry_title", "tdm_title", "tdm_data_source_info", "tdm_no_drugs",
    "tdm_data_source_title", "tdm_data_source_lines", "tdm_limitations",
    "tdm_limitations_lines", "tdm_predict_n_drugs", "tdm_add_drug_to_chart",
    "tdm_add_drug_n", "tdm_add", "tdm_dose_mg", "tdm_weight_kg", "tdm_freq",
    "tdm_chart_title", "tdm_hide_cd", "tdm_show_cd", "tdm_no_window",
    "chart_title", "chart_window_label", "chart_reset_view",
    "chart_status_format", "chart_status_low", "chart_status_high",
    "chart_status_in_window", "chart_status_not_comparable",
]

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')

errors = []
warnings = []


def read_bytes(path):
    with io.open(path, "rb") as f:
        return f.read()


def parse(path):
    raw = read_bytes(path)
    txt = raw.decode("utf-8")
    d = {}
    for m in STR_RE.finditer(txt):
        d.setdefault(m.group(1), []).append(m.group(2))
    return d, raw, txt


def ph(s):
    return [m.group(0) for m in PH_RE.finditer(s)]


print("=" * 66)
print("1/5  五语言 key 齐全性 + 占位符一致性")
print("=" * 66)
en_map = None
for d, name in LOCALES.items():
    path = os.path.join(RES, d, "strings.xml")
    if not os.path.exists(path):
        errors.append("%s: 目录不存在" % d)
        continue
    m, raw, txt = parse(path)
    missing = [k for k in KEYS if k not in m]
    dup = [k for k in KEYS if k in m and len(m[k]) > 1]
    if missing:
        errors.append("%s: 缺 key %s" % (name, missing))
    if dup:
        errors.append("%s: 重复 key %s" % (name, dup))
    if d == "values":
        en_map = {k: v[0] for k, v in m.items()}
    else:
        for k in KEYS:
            if k in m and k in en_map:
                if ph(m[k][0]) != ph(en_map[k]):
                    errors.append("%s/%s: 占位符不一致 en=%s tr=%s"
                                  % (name, k, ph(en_map[k]), ph(m[k][0])))
    print("  %-18s key %2d/%d  缺失=%d 重复=%d"
          % (name, len(KEYS) - len(missing), len(KEYS), len(missing), len(dup)))

print()
print("=" * 66)
print("2/5  XML 合法 / BOM / 行尾")
print("=" * 66)
import xml.etree.ElementTree as ET
for d, name in LOCALES.items():
    path = os.path.join(RES, d, "strings.xml")
    if not os.path.exists(path):
        continue
    raw = read_bytes(path)
    bom = raw[:3] == b"\xef\xbb\xbf"
    crlf = raw.count(b"\r\n")
    bare_lf = raw.count(b"\n") - crlf
    try:
        ET.fromstring(raw.decode("utf-8-sig"))
        xml_ok = "OK"
    except Exception as e:
        xml_ok = "FAIL(%s)" % e
        errors.append("%s: XML 解析失败 %s" % (name, e))
    if bom:
        errors.append("%s: 有 BOM" % name)
    # 底座 res XML 统一 LF,只查"混用"(同文件里 CRLF 与 bare LF 并存)
    if crlf and bare_lf:
        errors.append("%s: 混行尾 CRLF=%d bareLF=%d" % (name, crlf, bare_lf))
    print("  %-18s XML=%-6s BOM=%-5s CRLF=%-5d bareLF=%d"
          % (name, xml_ok, bom, crlf, bare_lf))

print()
print("=" * 66)
print("3/5  源码里带参调用检查 (防 %1$d 字面量显示到 UI)")
print("=" * 66)
kt = []
for dirpath, _, files in os.walk(SRC):
    for f in files:
        if f.endswith(".kt"):
            with io.open(os.path.join(dirpath, f), "r", encoding="utf-8") as fh:
                kt.append((os.path.join(dirpath, f), fh.read()))

need_args = {}
for path, text in kt:
    for m in re.finditer(r'stringResource\(\s*R\.string\.(\w+)\s*\)', text):
        need_args.setdefault(m.group(1), []).append((path, "no-arg"))
    for m in re.finditer(r'stringResource\(\s*R\.string\.(\w+)\s*,', text):
        need_args.setdefault(m.group(1), []).append((path, "with-arg"))

bad_call = []
for k in KEYS:
    calls = need_args.get(k, [])
    if not calls:
        continue
    has_with = any(c[1] == "with-arg" for c in calls)
    has_no = any(c[1] == "no-arg" for c in calls)
    if has_with and has_no:
        bad_call.append("%s: 同时存在带参和不带参调用" % k)
    elif has_no and ph(en_map.get(k, "")):
        bad_call.append("%s: 有占位符但调用没传参" % k)
    print("  %-24s 调用 %d 处 (%s)"
          % (k, len(calls), "带参" if has_with else ("不带参" if has_no else "-")))
if bad_call:
    errors.extend(bad_call)

print()
print("=" * 66)
print("4/5  重复 Text 检查 (TDM chip)")
print("=" * 66)
tdm = os.path.join(SRC, "ui", "tdm", "TdmScreen.kt")
if os.path.exists(tdm):
    with io.open(tdm, "r", encoding="utf-8") as f:
        t = f.read()
    n = t.count('if (selection.drug.therapeuticWindow != null) "TDM" else')
    print("  TDM chip Text 出现 %d 次 (期望 1)" % n)
    if n != 1:
        errors.append("TDM chip Text 出现 %d 次,期望 1" % n)
    # "TDM TDM" 只在注释里出现是正常的(修复说明),只看代码行
    code_only = "\n".join(
        ln for ln in t.splitlines()
        if not ln.strip().startswith("*") and not ln.strip().startswith("//")
    )
    if "TDM TDM" in code_only:
        errors.append("源码代码行里还有 'TDM TDM' 字样")
    else:
        print("  代码行无 'TDM TDM' (仅注释提及修复说明)")
else:
    errors.append("找不到 TdmScreen.kt")

print()
print("=" * 66)
print("5/5  worker 的三个工具脚本在位")
print("=" * 66)
for s in ("_add_tdm_strings.py", "_normalize_tdm_eol.py", "_verify_tdm_merge.py",
          "_add_tdm_ja_strings.py", "_add_tdm_hant_yue_strings.py"):
    p = os.path.join(ROOT, "tools", s)
    ok = os.path.exists(p)
    print("  %-32s %s" % (s, "在位" if ok else "缺失"))
    if not ok:
        warnings.append("%s 缺失" % s)

print()
print("=" * 66)
if errors:
    print("[FAIL] %d 个问题:" % len(errors))
    for e in errors:
        print("  - " + e)
    sys.exit(1)
print("[PASS] 全部检查通过")
if warnings:
    print("  提示: %s" % "; ".join(warnings))
sys.exit(0)
