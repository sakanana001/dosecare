# -*- coding: utf-8 -*-
"""让 3 个"源码守卫"单测与行尾风格无关。

这些守卫用**多行字符串**去匹配源文件 (`hrtSection(\\n    key = ...`), 而源文件在
Windows 工作区里是 CRLF, `Files.readString` / `File.readText()` 都不会做换行归一化 ——
于是 `\\n` 匹配不上 `\\r\\n`, 断言在 Windows 上必挂 (Linux 上却是绿的)。

修法: 读进来之后把 CRLF 归一成 LF, 守卫只关心代码结构, 不该关心行尾。
"""
import io
import os

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
TEST = os.path.join(REPO, "app", "src", "test", "java", "com", "dosecare", "app")

FILES = [
    os.path.join("ui", "components", "LazySectionJankGuardTest.kt"),
    os.path.join("ui", "journal", "PinnedTraySourceTest.kt"),
    os.path.join("ui", "medication", "MedicationEditorSheetsTest.kt"),
]

NORMALIZE = '.replace("\\r\\n", "\\n")'


def read(path):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        return f.read()


def write(path, text):
    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)


for rel in FILES:
    path = os.path.join(TEST, rel)
    text = read(path)
    nl = "\r\n" if "\r\n" in text else "\n"
    orig = text

    # 1) File(...).readText()  -> 加归一化
    text = text.replace(".readText()", ".readText()" + NORMALIZE)
    # 2) Files.readString(...)  -> 加归一化
    text = text.replace("Files.readString(", "Files.readString(")  # 保持, 下面单独处理

    # Files.readString(x) -> Files.readString(x).replace(...)
    import re
    text = re.sub(
        r"Files\.readString\(([^()]*)\)",
        lambda m: "Files.readString(%s)%s" % (m.group(1), NORMALIZE),
        text,
    )

    if text != orig:
        write(path, text)
        print("patched %s (newline=%r)" % (rel, nl))
    else:
        print("unchanged %s" % rel)
