# -*- coding: utf-8 -*-
"""把拼音首字母表从 UI 层搬到 model 层, 并让选药器用共享的搜索过滤函数。

为什么要搬:
  搜索规则 (中文名 / 日文名 / 英文名 / 品牌名 / ATC / 拼音首字母) 是**领域逻辑**,
  之前内联在 SearchableDrugPicker 里, 无法被单元测试覆盖;
  model 层新增的 Drug.matchesNameQuery 因此成了死代码。
  把 PINYIN_INITIAL / toPinyinInitials 搬到 model 层后, 过滤规则收敛到单一出处,
  单元测试能直接覆盖真正上线的行为。

本脚本只做字节级搬运, 保留原文件行尾风格 (CRLF)。
"""
import io
import os
import re
import sys

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
UI = os.path.join(
    REPO, "app", "src", "main", "java", "com", "dosecare", "app",
    "ui", "interaction", "SearchableDrugPicker.kt",
)
MODEL_DIR = os.path.join(
    REPO, "app", "src", "main", "java", "com", "dosecare", "app", "model", "pharmacology",
)
NEW = os.path.join(MODEL_DIR, "PinyinInitials.kt")

with io.open(UI, "r", encoding="utf-8", newline="") as f:
    text = f.read()
nl = "\r\n" if "\r\n" in text else "\n"

# 1) 切出拼音表 + 工具函数
start_marker = "/**\n * 拼音首字母工具 (定义在 PinyinInitial.kt)".replace("\n", nl)
end_marker = (
    "/** 中文 → 拼音首字母 (例: \"唑吡坦\" -> \"zbt\") */" + nl
    + "internal fun toPinyinInitials(chinese: String): String =" + nl
    + "    chinese.map { c -> PINYIN_INITIAL[c.toString()] ?: c.toString() }.joinToString(\"\")" + nl
)
si = text.find(start_marker)
ei = text.find(end_marker)
assert si != -1 and ei != -1, (si, ei)
assert ei > si

block = text[si:ei + len(end_marker)]
# 去掉原来的说明注释 (在新文件里重写)
block_body = block[block.find("// 自动从 scripts/pinyin_dict.py 生成"):]

new_file = (
    "package com.dosecare.app.model.pharmacology" + nl + nl
    + "/**" + nl
    + " * 中文 → 拼音首字母。" + nl
    + " *" + nl
    + " * toPinyinInitials(\"唑吡坦\") == \"zbt\"" + nl
    + " * toPinyinInitials(\"利培酮\") == \"lpt\"" + nl
    + " * toPinyinInitials(\"锂\") == \"l\"" + nl
    + " *" + nl
    + " * 放在 model 层: 搜索规则是领域逻辑, 需要能被单元测试直接覆盖。" + nl
    + " * 字典由 scripts/pinyin_dict.py 生成。" + nl
    + " */" + nl
    + nl
    + block_body
)
with io.open(NEW, "w", encoding="utf-8", newline="") as f:
    f.write(new_file)
print("wrote %s (%d bytes)" % (NEW, len(new_file.encode("utf-8"))))

# 2) 从 UI 文件里删掉这一段
text = text[:si] + text[si + len(block):]

# 3) UI 改成调用共享过滤函数
old_filter = (
    "    val filtered = remember(query, options) {" + nl
    + "        if (query.isBlank()) options" + nl
    + "        else {" + nl
    + "            val q = query.trim()" + nl
    + "            val ql = q.lowercase()" + nl
    + "            val isPinyinInitialsQuery = q.all { it in 'a'..'z' || it in 'A'..'Z' }" + nl
    + "            options.filter { drug ->" + nl
    + "                // 1. 中文名 substring" + nl
    + "                drug.genericNameZh.contains(q, ignoreCase = true) ||" + nl
    + "                // 1b. 日文名 substring (日语界面下的\"中文名\"对应日文 INN)" + nl
    + "                drug.genericNameJa?.contains(q, ignoreCase = true) == true ||" + nl
    + "                // 2. 英文名 substring" + nl
    + "                drug.genericName.lowercase().contains(ql) ||" + nl
    + "                // 3. 品牌名 substring" + nl
    + "                drug.brandNames.any { it.lowercase().contains(ql) } ||" + nl
    + "                // 4. ATC 码 substring" + nl
    + "                drug.atc?.lowercase()?.contains(ql) == true ||" + nl
    + "                // 5. 中文 → 拼音首字母 substring (e.g. \"zbt\" -> 唑吡坦)" + nl
    + "                //    关键修复: 用 toPinyinInitials 真正把中文名转成首字母" + nl
    + "                (isPinyinInitialsQuery && toPinyinInitials(drug.genericNameZh).lowercase().contains(ql))" + nl
    + "            }" + nl
    + "        }" + nl
    + "    }" + nl
)
assert text.count(old_filter) == 1, "filter block not found (%d)" % text.count(old_filter)
new_filter = (
    "    // 搜索规则集中在 model 层 (Drug.matchesNameQuery): 中文名 / 日文名 / 英文名 /" + nl
    + "    // 品牌名 / ATC 码 / 拼音首字母。" + nl
    + "    val filtered = remember(query, options) {" + nl
    + "        if (query.isBlank()) options else options.filter { it.matchesNameQuery(query) }" + nl
    + "    }" + nl
)
text = text.replace(old_filter, new_filter, 1)

# 4) 补 import
old_import = "import com.dosecare.app.model.pharmacology.displayNameFor" + nl
assert text.count(old_import) == 1
text = text.replace(
    old_import,
    old_import + "import com.dosecare.app.model.pharmacology.matchesNameQuery" + nl,
    1,
)

with io.open(UI, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("patched %s (newline=%r)" % (os.path.basename(UI), nl))
