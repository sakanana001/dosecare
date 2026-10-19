#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
给三个 CHANGELOG 插入 v2.0.0 条目:
  CHANGELOG.md              (根, 英文)
  docs/CHANGELOG.md         (英文)
  docs/CHANGELOG.zh-Hans.md (中文)

插入位置: 在 `## [Unreleased]` 段落之后、第一个 `## [x.y.z]` 之前。
如果文件里没有 [Unreleased], 就插到文件开头的引言块之后。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EN = r"C:\Users\yuwen\AppData\Local\Temp\changelog-2.0.0-en.md"
ZH = r"C:\Users\yuwen\AppData\Local\Temp\changelog-2.0.0-zh.md"

TARGETS = [
    ("CHANGELOG.md", EN),
    ("docs/CHANGELOG.md", EN),
    ("docs/CHANGELOG.zh-Hans.md", ZH),
]


def read_text(p):
    with io.open(p, "r", encoding="utf-8", newline="") as f:
        return f.read()


def main():
    apply = "--apply" in sys.argv
    en_block = read_text(EN).rstrip("\r\n")
    zh_block = read_text(ZH).rstrip("\r\n")

    for rel, src in TARGETS:
        path = os.path.join(ROOT, rel.replace("/", os.sep))
        text = read_text(path)
        eol = "\r\n" if "\r\n" in text else "\n"
        block = (en_block if src == EN else zh_block).replace("\n", eol)

        if "## [2.0.0]" in text:
            print("[SKIP] %s 已有 2.0.0 条目" % rel)
            continue

        # 找第一个版本标题 (## [x.y.z]), 插在它前面
        m = re.search(r"^## \[(\d+\.\d+\.\d+)\]", text, re.M)
        if not m:
            print("[ERROR] %s 找不到版本标题锚点" % rel)
            return 1
        at = m.start()
        new_text = text[:at] + block + eol + eol + text[at:]
        print("%-30s 在第 %d 行前插入 2.0.0 条目" % (rel, text[:at].count("\n") + 1))
        if apply:
            with io.open(path, "w", encoding="utf-8", newline="") as f:
                f.write(new_text)
            back = read_text(path)
            if "## [2.0.0]" not in back:
                print("[ERROR] %s 回读失败" % rel)
                return 1
            if back.count("## [2.0.0]") != 1:
                print("[ERROR] %s 2.0.0 条目重复" % rel)
                return 1

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
    else:
        print("\n[OK] 三个 CHANGELOG 已插入 v2.0.0 条目并回读校验通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
