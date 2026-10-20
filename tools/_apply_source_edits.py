#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
按行做源码替换 (自动探测并保留原文件行尾风格)。

为什么不用 bytes.replace: 这些 Kotlin 源文件行尾不统一
(SearchableDrugPicker.kt 是 CRLF, PharmacologyDossierSection.kt 是 LF),
而且 edit 工具的多行 old_string 在 CRLF 文件上匹配不上。

做法: 读成文本 -> 拆成行 (去行尾) -> 在"行列表"上做替换 -> 用原文件探测到的
行尾拼回去 -> 写盘。CRLF/LF/BOM 都原样保留。

用法: python _apply_source_edits.py [--dry]
"""
import io
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APP = os.path.join(ROOT, "app", "src", "main", "java", "com", "dosecare", "app")

# (相对路径, 旧行列表, 新行列表) —— 行必须完全一致 (含缩进)
EDITS = [
    ("ui/interaction/SearchableDrugPicker.kt",
     ["    val isChinesePrimary = locale.language == \"zh\""],
     []),

    ("ui/tdm/TdmScreen.kt",
     ["                val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]",
      "                val isChinesePrimary = locale.language == \"zh\"",
      "                val drugNamePrimary = if (isChinesePrimary) selection.drug.genericNameZh else selection.drug.genericName"],
     ["                val drugNamePrimary = selection.drug.displayName()"]),

    ("ui/tdm/InteractiveConcentrationChart.kt",
     ["                    \"${s.drug.genericNameZh} Cmax ${\"%.1f\".format(cmaxD)} ${s.drug.therapeuticWindow?.unit?.let { if (s.drug.skipTherapeuticWindowBand) it else it.substringBefore(\" \") }}\","],
     ["                    \"${s.drug.displayName()} Cmax ${\"%.1f\".format(cmaxD)} ${s.drug.therapeuticWindow?.unit?.let { if (s.drug.skipTherapeuticWindowBand) it else it.substringBefore(\" \") }}\","]),

    ("ui/tdm/InteractiveConcentrationChart.kt",
     ["                                val name = s.drug.genericNameZh"],
     ["                                val name = s.drug.displayName()"]),

    ("ui/catalog/dossier/PharmacologyDossierSection.kt",
     ["        title = drug.genericNameZh,"],
     ["        title = drug.displayName(),"]),

    ("ui/interaction/InteractionScreen.kt",
     ["                                        label = { Text(drug.genericNameZh) },"],
     ["                                        label = { Text(drug.displayName()) },"]),
]


def main():
    dry = "--dry" in sys.argv
    ok = True
    for rel, old_lines, new_lines in EDITS:
        path = os.path.join(APP, rel)
        with open(path, "rb") as f:
            raw = f.read()
        has_bom = raw[:3] == b"\xef\xbb\xbf"
        text = raw.decode("utf-8-sig")
        eol = "\r\n" if "\r\n" in text else "\n"
        lines = text.split(eol)
        n = len(old_lines)
        hits = 0
        out = []
        i = 0
        while i < len(lines):
            if lines[i:i + n] == old_lines:
                out.extend(new_lines)
                hits += 1
                i += n
            else:
                out.append(lines[i])
                i += 1
        if hits == 0:
            print("[MISS] %s" % rel)
            for l in old_lines:
                print("      找不到: %r" % l[:100])
            ok = False
            continue
        if not dry:
            new_text = eol.join(out)
            data = new_text.encode("utf-8")
            if has_bom:
                data = b"\xef\xbb\xbf" + data
            with open(path, "wb") as f:
                f.write(data)
        print("[%s] %s (x%d, 行尾=%s)" % ("DRY" if dry else "OK ", rel, hits, "CRLF" if eol == "\r\n" else "LF"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
