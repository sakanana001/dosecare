#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
第 4 期品牌收尾: site/ 官网 + 用户可见的运行时字符串 + 一个失效的架构测试。

三件事:

A. site/ (Astro 官网, 50+ 文件)
   机械替换:
     sakanana001/PicoHRT -> sakanana001/dosecare
     PicoHRT              -> DoseCare
     picohrt              -> dosecare
   含 33 张截图 SVG 里的标题文字, 以及 astro.config.mjs / config.ts 里的
   GitHub Pages base path ('/PicoHRT' -> '/dosecare', 跟随仓库名)。

B. 用户可见的运行时字符串 (第 0 期 rebrand 漏改)
   - AppDiagnosticsExportService: 导出文件名前缀 "picohrt-diagnostics-" 和
     首行 "PicoHRT diagnostic logs" —— 用户导出诊断日志时直接看到
   - BackupExportService: 备份文件名前缀 "picohrt-backup-" —— 用户看到

C. 一个失效的架构测试 (第 0 期 rebrand 漏改, 比 B 更隐蔽)
   ModelLayerPurityTest.FORBIDDEN_IMPORT 的正则是
     ^\s*import\s+com\.picohrt\.data\.
   rebrand 后源码包名已经是 com.dosecare.app, 这个正则永远匹配不到任何
   import —— 也就是说"模型层不许 import 数据层"这条架构约束实际上是
   形同虚设, 永远不会触发。改成 com\.dosecare\.app\.data\.。

   顺带把测试里对旧文件名的断言同步改掉。

默认 dry-run, 加 --apply 才写盘。保持各文件原有行尾与 BOM 状态。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SITE = os.path.join(ROOT, "site")
APP_SRC = os.path.join(ROOT, "app", "src")

SITE_RULES = [
    ("sakanana001/PicoHRT", "sakanana001/dosecare"),
    ("PicoHRT", "DoseCare"),
    ("picohrt", "dosecare"),
]

# 用户可见的运行时字符串 (精确对, 避免误伤)
RUNTIME_RULES = [
    ("app/src/main/java/com/dosecare/app/util/AppDiagnosticsExportService.kt", [
        ('"picohrt-diagnostics-$timestamp.txt"', '"dosecare-diagnostics-$timestamp.txt"'),
        ('appendLine("PicoHRT diagnostic logs")', 'appendLine("DoseCare diagnostic logs")'),
    ]),
    ("app/src/main/java/com/dosecare/app/data/backup/BackupExportService.kt", [
        ('"picohrt-backup-$timestamp.hrtbackup"', '"dosecare-backup-$timestamp.hrtbackup"'),
    ]),
    # 失效的架构测试正则
    ("app/src/test/java/com/dosecare/app/architecture/ModelLayerPurityTest.kt", [
        (r'Regex("""^\s*import\s+com\.picohrt\.data\.""")',
         r'Regex("""^\s*import\s+com\.dosecare\.app\.data\.""")'),
    ]),
    # 跟着改的断言
    ("app/src/test/java/com/dosecare/app/util/AppDiagnosticsExportServiceTest.kt", [
        ('"PicoHRT diagnostic logs"', '"DoseCare diagnostic logs"'),
        ('"picohrt-diagnostics-2026-05-07_12-04-05.txt"',
         '"dosecare-diagnostics-2026-05-07_12-04-05.txt"'),
    ]),
    ("app/src/test/java/com/dosecare/app/data/backup/BackupExportServiceTest.kt", [
        ('"picohrt-backup-2026-04-26_12-04-05.hrtbackup"',
         '"dosecare-backup-2026-04-26_12-04-05.hrtbackup"'),
    ]),
    ("app/src/test/java/com/dosecare/app/ui/settings/SettingsViewModelTest.kt", [
        ('"picohrt-backup.hrtbackup"', '"dosecare-backup.hrtbackup"'),
    ]),
]

SKIP_DIRS = {"node_modules", ".git", "dist", ".astro", ".output"}
TEXT_EXT = {".astro", ".ts", ".tsx", ".json", ".mjs", ".js", ".css", ".svg", ".html", ".txt", ".md"}


def read_raw(path):
    with io.open(path, "rb") as f:
        return f.read()


def decode(raw):
    bom = raw[:3] == b"\xef\xbb\xbf"
    return raw.decode("utf-8-sig" if bom else "utf-8"), bom


def write_raw(path, text, bom):
    out = text.encode("utf-8")
    if bom:
        out = b"\xef\xbb\xbf" + out
    with io.open(path, "wb") as f:
        f.write(out)


def main():
    apply = "--apply" in sys.argv
    total_files = 0
    total_hits = 0

    # ---- A. site/ ----
    print("=== A. site/ 官网 ===")
    site_hits = 0
    site_files = 0
    for dirpath, dirnames, files in os.walk(SITE):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in files:
            if os.path.splitext(fn)[1].lower() not in TEXT_EXT:
                continue
            path = os.path.join(dirpath, fn)
            raw = read_raw(path)
            text, bom = decode(raw)
            new = text
            n = 0
            for old, rep in SITE_RULES:
                c = new.count(old)
                if c:
                    new = new.replace(old, rep)
                    n += c
            if n:
                site_files += 1
                site_hits += n
                print("  %3d  %s" % (n, os.path.relpath(path, ROOT)))
                if apply:
                    write_raw(path, new, bom)
    print("  -> %d 个文件, %d 处" % (site_files, site_hits))
    total_files += site_files
    total_hits += site_hits

    # ---- B/C. 运行时字符串 + 架构测试 ----
    print()
    print("=== B/C. 用户可见字符串 + 失效的架构测试 ===")
    for rel, rules in RUNTIME_RULES:
        path = os.path.join(ROOT, rel)
        if not os.path.exists(path):
            print("  [SKIP] %s 不存在" % rel)
            continue
        raw = read_raw(path)
        text, bom = decode(raw)
        new = text
        n = 0
        for old, rep in rules:
            c = new.count(old)
            if c:
                new = new.replace(old, rep)
                n += c
            else:
                print("  [WARN] %s 没找到: %s" % (rel, old[:70]))
        if n:
            print("  %3d  %s" % (n, rel))
            if apply:
                write_raw(path, new, bom)
            total_files += 1
            total_hits += n

    print()
    print("合计: %d 个文件, %d 处" % (total_files, total_hits))
    if not apply:
        print("[dry-run] 加 --apply 写入")
        return 0

    # ---- 回读校验 ----
    problems = []
    for dirpath, dirnames, files in os.walk(SITE):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fn in files:
            if os.path.splitext(fn)[1].lower() not in TEXT_EXT:
                continue
            path = os.path.join(dirpath, fn)
            text, _ = decode(read_raw(path))
            if "PicoHRT" in text or "picohrt" in text:
                problems.append(os.path.relpath(path, ROOT))
    for rel, _ in RUNTIME_RULES:
        path = os.path.join(ROOT, rel)
        if os.path.exists(path):
            text, _ = decode(read_raw(path))
            for bad in ("PicoHRT diagnostic", "picohrt-diagnostics", "picohrt-backup",
                        "com\\.picohrt\\.data"):
                if bad in text:
                    problems.append("%s (残留 %s)" % (rel, bad))
    if problems:
        print("[FAIL] 仍有残留:")
        for p in problems:
            print("  - " + p)
        return 1
    print("[OK] site/ 与运行时字符串已无 PicoHRT 残留")
    return 0


if __name__ == "__main__":
    sys.exit(main())
