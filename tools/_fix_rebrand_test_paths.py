#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修第 0 期包名 rebrand 遗漏: 测试文件里写死的源码路径字面量。

背景: 第 0 期把 `com.picohrt` 改成 `com.dosecare.app` 时, 源码目录搬了家
(app/src/main/java/com/picohrt/ -> app/src/main/java/com/dosecare/app/),
但 14 个测试文件里 53 处路径字面量还写着 `com/picohrt/...`, 于是这些
"读源码文件做断言" 的测试全部 FileNotFoundException。

这类测试不走 Robolectric, 所以不是 JDK 25 的锅, 是实打实的 rebrand 遗漏。

修法: 把测试里的 `com/picohrt/` 替换成 `com/dosecare/app/`。
只改路径字面量, 不碰任何其它文本。

默认 dry-run, 加 --apply 才写盘。保持原文件行尾与 BOM。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEST_DIR = os.path.join(ROOT, "app", "src", "test")

OLD = "com/picohrt/"
NEW = "com/dosecare/app/"

# 只替换出现在路径字面量里的形式:
#   "app/src/main/java/com/picohrt/..."   -> "app/src/main/java/com/dosecare/app/..."
#   "src/main/java/com/picohrt/..."       -> "src/main/java/com/dosecare/app/..."
#   File("src/main/java/com/picohrt/ui")  -> File("src/main/java/com/dosecare/app/ui")
PAT = re.compile(r'(java/com/)(picohrt/)')


def main():
    apply = "--apply" in sys.argv
    changed = []
    total_hits = 0

    for dirpath, _, files in os.walk(TEST_DIR):
        for fn in files:
            if not fn.endswith(".kt"):
                continue
            path = os.path.join(dirpath, fn)
            with io.open(path, "rb") as f:
                raw = f.read()
            bom = raw[:3] == b"\xef\xbb\xbf"
            text = raw.decode("utf-8-sig" if bom else "utf-8")
            new_text, n = PAT.subn(r"\1dosecare/app/", text)
            if n == 0:
                continue
            total_hits += n
            rel = os.path.relpath(path, ROOT)
            changed.append((rel, n))
            if apply:
                out = new_text.encode("utf-8")
                if bom:
                    out = b"\xef\xbb\xbf" + out
                with io.open(path, "wb") as f:
                    f.write(out)

    print("命中 %d 处, 涉及 %d 个文件" % (total_hits, len(changed)))
    for rel, n in changed:
        print("  %3d  %s" % (n, rel))

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
        return 0

    # 回读校验: 不应再有 java/com/picohrt/
    left = []
    for dirpath, _, files in os.walk(TEST_DIR):
        for fn in files:
            if not fn.endswith(".kt"):
                continue
            path = os.path.join(dirpath, fn)
            with io.open(path, "r", encoding="utf-8-sig") as f:
                if "java/com/picohrt/" in f.read():
                    left.append(os.path.relpath(path, ROOT))
    if left:
        print("[FAIL] 仍有残留: %s" % left)
        return 1
    print("[OK] 测试目录已无 java/com/picohrt/ 残留")
    return 0


if __name__ == "__main__":
    sys.exit(main())
