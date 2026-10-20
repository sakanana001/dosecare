# -*- coding: utf-8 -*-
"""用字节级行编辑器重做 produceFile 修复, 保留文件原有行尾 (含 \\r\\r\\n 双 CR)。"""
import io
import os
import subprocess
import sys

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
sys.path.insert(0, os.path.join(REPO, "tools"))
from _lineedit import split_lines, join_lines, read_bytes, write_bytes, describe  # noqa: E402

TEST = os.path.join(REPO, "app", "src", "test", "java", "com", "dosecare", "app")
REL = [
    os.path.join("app", "src", "test", "java", "com", "dosecare", "app", "data", "repository", "SettingsRepositoryTest.kt"),
    os.path.join("app", "src", "test", "java", "com", "dosecare", "app", "data", "repository", "SettingsRepositoryLegacyWidgetAppearanceTest.kt"),
    os.path.join("app", "src", "test", "java", "com", "dosecare", "app", "ui", "catalog", "nudge", "StockNudgeGateTest.kt"),
    os.path.join("app", "src", "test", "java", "com", "dosecare", "app", "widget", "WidgetAppearanceRepositoryMigrationTest.kt"),
]

# 1) 先把文件还原到 HEAD 原始字节
for rel in REL:
    subprocess.run(["git", "-C", REPO, "checkout", "--", rel], check=True)
    print("reset %s  ->  %s" % (os.path.basename(rel), describe(os.path.join(REPO, rel))))

# 2) 字节级改写
for rel in REL:
    path = os.path.join(REPO, rel)
    data = read_bytes(path)
    lines = split_lines(data)
    changed = 0
    for idx, (content, term) in enumerate(lines):
        if b"tmpFolder.newFile(" not in content:
            continue
        new_content = content.replace(b"tmpFolder.newFile(", b"File(tmpFolder.root, ")
        # 补一个右括号: 原来是 newFile("x") -> File(tmpFolder.root, "x")
        new_content = new_content.replace(b'preferences_pb")', b'preferences_pb")')
        lines[idx] = (new_content, term)
        changed += 1
    assert changed == 1, (rel, changed)

    # 补 import java.io.File (放到 java.* 组的最前面, 保持字母序)
    if b"import java.io.File\r" not in b"".join(c for c, _ in lines) and \
       b"import java.io.File\n" not in b"".join(c for c, _ in lines):
        first_java = None
        last_import = None
        for idx, (content, term) in enumerate(lines):
            if content.startswith(b"import java."):
                if first_java is None:
                    first_java = idx
            if content.startswith(b"import "):
                last_import = idx
        assert first_java is not None or last_import is not None, rel
        if first_java is not None:
            at = first_java
        else:
            at = last_import + 1
        ref_content, ref_term = lines[at - 1] if at > 0 else lines[at]
        tail = b"\r" if ref_content.endswith(b"\r") else b""
        lines.insert(at, (b"import java.io.File" + tail, ref_term))

    write_bytes(path, join_lines(lines))
    print("patched %s  ->  %s" % (os.path.basename(rel), describe(path)))
