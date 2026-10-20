#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
第二批源码改动: 导入 + SpecComparison 的药名按界面语言。

用行级替换 (保留原文件行尾) + 正则替换。
用法: python _apply_source_edits2.py [--dry]
"""
import io
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src", "main", "java", "com", "dosecare", "app")

# 相对路径 -> (要插入的 import 行, 插在哪个 import 之后)
IMPORTS = [
    ("ui/tdm/TdmScreen.kt",
     "import com.dosecare.app.ui.pharmacology.displayName",
     "import com.dosecare.app.model.pharmacology.PkModel"),
    ("ui/tdm/InteractiveConcentrationChart.kt",
     "import com.dosecare.app.ui.pharmacology.displayName",
     "import com.dosecare.app.model.pharmacology.PkCurve"),
    ("ui/catalog/dossier/PharmacologyDossierSection.kt",
     "import com.dosecare.app.ui.pharmacology.displayName",
     "import com.dosecare.app.model.pharmacology.labelRes"),
    ("ui/interaction/InteractionScreen.kt",
     "import com.dosecare.app.ui.pharmacology.displayName",
     "import com.dosecare.app.model.pharmacology.AnticholinergicLoadInteraction"),
    ("ui/compare/SpecComparison.kt",
     "import com.dosecare.app.ui.pharmacology.displayName",
     "import com.dosecare.app.model.pharmacology.labelRes"),
    ("ui/compare/SpecComparison.kt",
     "import com.dosecare.app.ui.pharmacology.secondaryDisplayName",
     "import com.dosecare.app.model.pharmacology.labelRes"),
]

# (相对路径, 正则, 替换)
REGEXES = [
    # SpecComparison: 中文名 -> 界面语言主名; 英文名 -> 副名
    ("ui/compare/SpecComparison.kt",
     re.compile(r"\b(\w+)\.genericNameZh\b"),
     r"\1.displayName()"),
    ("ui/compare/SpecComparison.kt",
     re.compile(r"\b(\w+)\.genericName\b"),
     r"\1.secondaryDisplayName()"),
]


def load(path):
    with open(path, "rb") as f:
        raw = f.read()
    has_bom = raw[:3] == b"\xef\xbb\xbf"
    text = raw.decode("utf-8-sig")
    eol = "\r\n" if "\r\n" in text else "\n"
    return has_bom, eol, text.split(eol)


def save(path, has_bom, eol, lines):
    data = eol.join(lines).encode("utf-8")
    if has_bom:
        data = b"\xef\xbb\xbf" + data
    with open(path, "wb") as f:
        f.write(data)


def main():
    dry = "--dry" in sys.argv
    ok = True

    for rel, new_import, anchor in IMPORTS:
        path = os.path.join(APP, rel)
        has_bom, eol, lines = load(path)
        if new_import in lines:
            print("[SKIP] %s 已有 %s" % (rel, new_import.rsplit(".", 1)[-1]))
            continue
        if anchor not in lines:
            print("[MISS] %s 找不到锚点 %s" % (rel, anchor))
            ok = False
            continue
        i = lines.index(anchor)
        # 同一锚点下要插多条 import 时, 保持 IMPORTS 里的顺序
        inserted = [l for l in lines if l.startswith("import com.dosecare.app.ui.pharmacology.")]
        lines = [l for l in lines if l not in inserted]
        i = lines.index(anchor)
        block = sorted(set(inserted + [new_import]))
        lines[i + 1:i + 1] = block
        if not dry:
            save(path, has_bom, eol, lines)
        print("[%s] %s + %s" % ("DRY" if dry else "OK ", rel, new_import.rsplit(".", 1)[-1]))

    for rel, rx, repl in REGEXES:
        path = os.path.join(APP, rel)
        has_bom, eol, lines = load(path)
        text = eol.join(lines)
        new_text, n = rx.subn(repl, text)
        if n == 0:
            print("[MISS] %s 正则无命中 %s" % (rel, rx.pattern))
            ok = False
            continue
        if not dry:
            save(path, has_bom, eol, new_text.split(eol))
        print("[%s] %s 正则 %s -> %s (x%d)" % ("DRY" if dry else "OK ", rel, rx.pattern, repl, n))

    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
