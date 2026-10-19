#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
给 MedicineDetailViewModelTest 的 ViewModel 构造补 `drugCatalog` 参数。

背景: 第 2 期给 MedicineDetailViewModel 加了 `drugCatalog: DrugCatalogService`
构造参数(药理档案绑定),但这份既有单测没跟着改,导致
`:app:compilePlayDebugUnitTestKotlin` 报 "No value passed for parameter
'drugCatalog'" —— 26 处构造全缺。

修法: 在测试类里加一个共享的空目录实例
`private val drugCatalog = DrugCatalogService(emptyList())`,
然后在每处 `settingsRepository = settingsRepository,` 之后插入
`drugCatalog = drugCatalog,`。

空目录是安全的: 这些用例测的是药品详情页的归档/库存/分组逻辑,
不碰药理档案; ViewModel 侧对空目录的行为是 `tryGetById` 返回 null
(即"未关联档案"), 不会崩。

默认 dry-run,加 --apply 才写盘。保持原文件行尾与 BOM 状态。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(
    ROOT, "app", "src", "test", "java", "com", "dosecare", "app", "ui", "catalog",
    "MedicineDetailViewModelTest.kt",
)

ANCHOR = "            settingsRepository = settingsRepository,\n"
INSERT = "            drugCatalog = drugCatalog,\n"
IMPORT = "import com.dosecare.app.model.pharmacology.DrugCatalogService\n"
FIELD = "    private val drugCatalog = DrugCatalogService(emptyList())\n"
FIELD_ANCHOR = "    private val settingsRepository: SettingsRepository = mockk()\n"


def read_bytes(path):
    with io.open(path, "rb") as f:
        return f.read()


def main():
    apply = "--apply" in sys.argv
    raw = read_bytes(TARGET)
    bom = raw[:3] == b"\xef\xbb\xbf"
    eol = b"\r\n" if b"\r\n" in raw else b"\n"
    text = raw.decode("utf-8-sig" if bom else "utf-8")

    # 1) import
    if "import com.dosecare.app.model.pharmacology.DrugCatalogService" not in text:
        # 放在 model.medication 之后, 保持字母序 (model.medication < model.pharmacology < model.settings)
        imp = IMPORT.replace("\n", eol.decode())
        anchor = "import com.dosecare.app.model.settings.SettingsState" + eol.decode()
        if anchor not in text:
            print("[ERROR] 找不到 import 锚点")
            return 1
        text = text.replace(anchor, imp + anchor, 1)
        print("  + import DrugCatalogService")

    # 2) 字段
    if "private val drugCatalog = DrugCatalogService(emptyList())" not in text:
        field = FIELD.replace("\n", eol.decode())
        anchor = FIELD_ANCHOR.replace("\n", eol.decode())
        if anchor not in text:
            print("[ERROR] 找不到字段锚点")
            return 1
        text = text.replace(anchor, anchor + field, 1)
        print("  + field drugCatalog")

    # 3) 26 处构造
    eol_s = eol.decode()
    anchor_eol = ANCHOR.replace("\n", eol_s)
    insert_eol = INSERT.replace("\n", eol_s)
    n = text.count(anchor_eol)
    if n == 0:
        print("[ERROR] 找不到构造锚点")
        return 1
    if "drugCatalog = drugCatalog," not in text:
        text = text.replace(anchor_eol, anchor_eol + insert_eol)
        print("  + %d 处构造注入 drugCatalog" % n)
    else:
        # 已有部分注入: 逐处检查
        already = 0
        for m in re.finditer(re.escape(anchor_eol), text):
            if text[m.end():].startswith(insert_eol):
                already += 1
        missing = n - already
        if missing:
            # 逐处补
            out = []
            idx = 0
            for m in re.finditer(re.escape(anchor_eol), text):
                out.append(text[idx:m.end()])
                if not text[m.end():].startswith(insert_eol):
                    out.append(insert_eol)
                idx = m.end()
            out.append(text[idx:])
            text = "".join(out)
            print("  + 补齐 %d 处缺失的 drugCatalog" % missing)
        else:
            print("  = %d 处构造已全部注入" % n)

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
        return 0

    out_bytes = text.encode("utf-8")
    if bom:
        out_bytes = b"\xef\xbb\xbf" + out_bytes
    with io.open(TARGET, "wb") as f:
        f.write(out_bytes)

    # 回读校验
    back = read_bytes(TARGET)
    bt = back.decode("utf-8-sig" if back[:3] == b"\xef\xbb\xbf" else "utf-8")
    cnt = bt.count("drugCatalog = drugCatalog,")
    ok = ("import com.dosecare.app.model.pharmacology.DrugCatalogService" in bt
          and "private val drugCatalog = DrugCatalogService(emptyList())" in bt
          and cnt == 26)
    print("[%s] 注入 %d 处构造" % ("OK" if ok else "FAIL", cnt))
    if not ok:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
