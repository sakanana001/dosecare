#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 5 个批次的日语译文合并进 app/src/main/res/values-ja/strings.xml。

流程:
  1. 读 5 个 ja_gap_NN.ja.json
  2. 全量校验:
     - 批次之间 key 不重叠
     - 合并后覆盖了全部缺口 key (与导出的 gap 清单比对)
     - 占位符与英文版逐 key 一致
     - 没有裸的 & < > ' (XML 转义检查)
     - 译文非空
  3. 写进 values-ja/strings.xml (在 </resources> 前插入), 保持 LF + 无 BOM
  4. 回读 + XML 合法性校验

默认 dry-run, 加 --apply 才写盘。
"""
import io
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
JA_FILE = os.path.join(RES, "values-ja", "strings.xml")
EN_FILE = os.path.join(RES, "values", "strings.xml")
TRANS_DIR = os.path.join(ROOT, "tools", "_ja_translate")

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')
BARE = re.compile(r'&(?!amp;|lt;|gt;|quot;|apos;)|<(?![a-zA-Z/!?])|(?<![a-zA-Z])>(?![a-zA-Z])')


def read_text(p):
    with io.open(p, "r", encoding="utf-8", newline="") as f:
        return f.read()


def parse_text(t):
    d = {}
    for m in STR_RE.finditer(t):
        d.setdefault(m.group(1), m.group(2))
    return d


def ph(s):
    return sorted(m.group(0) for m in PH_RE.finditer(s))


def main():
    apply = "--apply" in sys.argv
    en = parse_text(read_text(EN_FILE))
    ja_text = read_text(JA_FILE)
    ja = parse_text(ja_text)

    batch_files = sorted(f for f in os.listdir(TRANS_DIR)
                         if re.match(r"^ja_gap_\d+\.ja\.json$", f))
    if not batch_files:
        print("[ERROR] 找不到任何 ja_gap_NN.ja.json")
        return 1

    merged = {}
    errors = []
    for bf in batch_files:
        with io.open(os.path.join(TRANS_DIR, bf), "r", encoding="utf-8") as f:
            data = json.load(f)
        for k, v in data.items():
            if not isinstance(v, str) or not v.strip():
                errors.append("%s: 译文为空或非字符串" % k)
                continue
            if k in merged:
                errors.append("%s: 批次之间 key 重复" % k)
            merged[k] = v
        print("  %s: %d 条" % (bf, len(data)))

    print("\n合并后共 %d 条" % len(merged))

    # 1) 占位符与英文版一致
    for k, v in merged.items():
        if k not in en:
            errors.append("%s: 英文版没有这个 key" % k)
            continue
        if ph(en[k]) != ph(v):
            errors.append("%s: 占位符不一致 en=%s ja=%s" % (k, ph(en[k]), ph(v)))
    # 2) 覆盖全部缺口
    gap = set(k for k in en if k not in ja)
    not_covered = gap - set(merged)
    if not_covered:
        errors.append("还有 %d 个缺口 key 没译: %s"
                      % (len(not_covered), sorted(not_covered)[:10]))
    extra = set(merged) - set(en)
    if extra:
        errors.append("译文里有英文版不存在的 key: %s" % sorted(extra)[:10])
    # 3) XML 转义检查
    for k, v in merged.items():
        if BARE.search(v):
            errors.append("%s: 有未转义的 & < > : %s" % (k, v[:60]))

    if errors:
        print("\n[ERROR] %d 个问题:" % len(errors))
        for e in errors[:30]:
            print("  - " + e)
        return 1
    print("\n全部校验通过 (占位符 / 覆盖率 / XML 转义)")

    if not apply:
        print("[dry-run] 加 --apply 写入")
        return 0

    eol = "\r\n" if "\r\n" in ja_text else "\n"
    close = ja_text.rfind("</resources>")
    if close < 0:
        print("[ERROR] 找不到 </resources>")
        return 1
    block = ['    <string name="%s">%s</string>' % (k, merged[k])
             for k in sorted(merged.keys())]
    new_text = ja_text[:close] + eol.join(block) + eol + ja_text[close:]

    with io.open(JA_FILE, "w", encoding="utf-8", newline="") as f:
        f.write(new_text)

    # 回读校验
    back_raw = read_text(JA_FILE)
    back = parse_text(back_raw)
    bad = [k for k in merged if back.get(k) != merged[k]]
    if bad:
        print("[ERROR] 回读不一致: %s" % bad[:10])
        return 1
    try:
        ET.fromstring(back_raw)
    except Exception as e:
        print("[ERROR] XML 不合法: %s" % e)
        return 1
    total = len(back)
    print("[OK] 已写入 %d 条; 日语目录现有 %d 个 key; XML 合法" % (len(merged), total))
    return 0


if __name__ == "__main__":
    sys.exit(main())
