#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扫描仓库里的品牌残留, 输出第 4 期收尾(文档/版本号)要处理的清单。

分两类:
  A. 文档类 (README/CHANGELOG/docs/**) —— 需要重写
  B. 源码/配置类 —— 第 0 期已清过一轮, 这里看还有没有漏的

同时报告:
  - LICENSE 的许可证类型
  - versionName / versionCode
  - release workflow 里引用的包名/品牌
"""
import io
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 允许出现 PicoHRT 的地方: fork 谱系说明 / 代码注释 / 历史变更记录
BRAND = re.compile(r'PicoHRT|PicoHrt|picohrt|Pico Hrt', re.I)

SKIP_DIRS = {".git", "build", ".gradle", ".idea", "node_modules", "captures"}
SKIP_EXT = {".png", ".jpg", ".jpeg", ".webp", ".ico", ".jar", ".apk", ".aab", ".so", ".zip", ".keystore", ".jks"}


def is_doc(path):
    p = path.replace("\\", "/").lower()
    return (p.endswith(".md") or "/docs/" in p or p.endswith(".txt")
            or "/.github/" in p or "fastlane" in p)


def main():
    rows = []
    for dirpath, dirnames, files in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in files:
            if os.path.splitext(fn)[1].lower() in SKIP_EXT:
                continue
            path = os.path.join(dirpath, fn)
            rel = os.path.relpath(path, ROOT)
            try:
                with io.open(path, "r", encoding="utf-8") as f:
                    text = f.read()
            except Exception:
                continue
            hits = []
            for i, line in enumerate(text.splitlines(), 1):
                if BRAND.search(line):
                    hits.append((i, line.strip()[:150]))
            if hits:
                rows.append((rel, is_doc(rel), hits))

    docs = [r for r in rows if r[1]]
    code = [r for r in rows if not r[1]]

    print("=" * 78)
    print("A. 文档类残留 (README / docs / .github / fastlane) —— 第 4 期要重写")
    print("=" * 78)
    for rel, _, hits in sorted(docs):
        print("\n--- %s  (%d 处)" % (rel, len(hits)))
        for i, line in hits[:14]:
            print("   %4d  %s" % (i, line))
        if len(hits) > 14:
            print("   ... 还有 %d 处" % (len(hits) - 14))

    print()
    print("=" * 78)
    print("B. 源码/配置类残留 —— 第 0 期清过, 看剩下的")
    print("=" * 78)
    if not code:
        print("(无)")
    for rel, _, hits in sorted(code):
        print("\n--- %s  (%d 处)" % (rel, len(hits)))
        for i, line in hits[:8]:
            print("   %4d  %s" % (i, line))
        if len(hits) > 8:
            print("   ... 还有 %d 处" % (len(hits) - 8))

    print()
    print("=" * 78)
    print("C. 许可证 / 版本 / 仓库地址")
    print("=" * 78)
    lic = os.path.join(ROOT, "LICENSE")
    if os.path.exists(lic):
        with io.open(lic, "r", encoding="utf-8") as f:
            head = f.read(4000)
        if "GNU AFFERO GENERAL PUBLIC LICENSE" in head:
            kind = "AGPL-3.0"
        elif "GNU GENERAL PUBLIC LICENSE" in head:
            kind = "GPL-3.0  <-- 合并后必须升到 AGPL-3.0"
        else:
            kind = "未知"
        print("  LICENSE: %s" % kind)

    bg = os.path.join(ROOT, "app", "build.gradle.kts")
    if os.path.exists(bg):
        with io.open(bg, "r", encoding="utf-8") as f:
            t = f.read()
        for pat in (r'versionCode\s*=\s*(.+)', r'versionName\s*=\s*"([^"]+)"',
                    r'namespace\s*=\s*"([^"]+)"', r'applicationId\s*=\s*"([^"]+)"'):
            m = re.search(pat, t)
            if m:
                print("  build.gradle.kts: %s" % m.group(0).strip())

    # release workflow / 仓库地址
    for rel, _, hits in rows:
        if ".github" in rel.replace("\\", "/") or "fastlane" in rel.replace("\\", "/"):
            for i, line in hits:
                if "github.com" in line or "sakanana" in line:
                    print("  %s:%d  %s" % (rel, i, line[:120]))
                    break
    return 0


if __name__ == "__main__":
    sys.exit(main())
