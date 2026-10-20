#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
给 catalog.json 的每个药加 "genericNameJa" 字段。

做法: 行级插入 —— 逐行扫描, 记住当前药 id, 遇到 "genericNameZh" 那一行就在
它下面插一行 "genericNameJa"。这样 JSON 的其它内容(缩进、字段顺序、换行)
全部原样保留, 不受 json.dump 重新序列化影响。

用法: python _add_ja_names_to_catalog.py [--dry]
"""
import io
import json
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "pharmacology", "catalog.json")
TITLES = os.path.join(ROOT, "tools", "_ja_drug_titles.json")

ID_RE = re.compile(r'^\s*"id":\s*"([^"]+)"')
ZH_RE = re.compile(r'^(\s*)"genericNameZh":\s*"((?:[^"\\]|\\.)*)",?\s*$')


def main():
    dry = "--dry" in sys.argv
    rows = json.load(io.open(TITLES, "r", encoding="utf-8"))
    names = {r["id"]: r["ja"] for r in rows if r.get("ja")}
    print("待写入日文名: %d 条" % len(names))

    with open(CATALOG, "rb") as f:
        raw = f.read()
    has_bom = raw[:3] == b"\xef\xbb\xbf"
    text = raw.decode("utf-8-sig")
    eol = "\r\n" if "\r\n" in text else "\n"
    lines = text.split(eol)

    out = []
    cur_id = None
    written = set()
    for line in lines:
        m = ID_RE.match(line)
        if m:
            cur_id = m.group(1)
        out.append(line)
        m = ZH_RE.match(line)
        if not m:
            continue
        if cur_id is None or cur_id not in names:
            continue
        if cur_id in written:
            continue
        written.add(cur_id)
        indent = m.group(1)
        ja = names[cur_id].replace("\\", "\\\\").replace('"', '\\"')
        out.append('%s"genericNameJa": "%s",' % (indent, ja))

    missing = set(names) - written
    if missing:
        print("[ERROR] 这几个 id 没插进去: %s" % sorted(missing))
        return 1

    # 校验: 重新解析, 确认每个药都有 genericNameJa 且值正确
    new_text = eol.join(out)
    data = json.loads(new_text)
    for d in data["drugs"]:
        got = d.get("genericNameJa")
        want = names.get(d["id"])
        if got != want:
            print("[ERROR] %s: 写入 %r != 期望 %r" % (d["id"], got, want))
            return 1
    print("校验通过: %d 个药都有 genericNameJa" % len(data["drugs"]))

    if dry:
        print("[dry-run] 加 --apply 写入")
        return 0

    data_out = new_text.encode("utf-8")
    if has_bom:
        data_out = b"\xef\xbb\xbf" + data_out
    with open(CATALOG, "wb") as f:
        f.write(data_out)
    print("已写入 %s (%d 字节)" % (CATALOG, len(data_out)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
