# -*- coding: utf-8 -*-
"""DrugCatalog.kt 的 HEAD blob 用的是 \\r\\r\\n (双 CR) 行尾 —— 仓库既有状态。

本轮编辑把它规范化成了干净 CRLF, 导致 git 认为整个文件被重写 (291/288)。
这里把行尾还原成 \\r\\r\\n 约定, 让 diff 只剩真正的 3 行新增。

流程:
  1. 取 HEAD blob
  2. 验证假设: HEAD blob 把 \\r\\r\\n 换成 \\r\\n 之后, 与"去掉我新增的 3 行"的工作区文件一致
  3. 把工作区文件所有 \\r\\n 换回 \\r\\r\\n (新增的 3 行也一起, 保持与全文件一致)
"""
import io
import os
import subprocess

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
REL = "app/src/main/java/com/dosecare/app/model/pharmacology/DrugCatalog.kt"
ABS = os.path.join(REPO, REL.replace("/", os.sep))

head = subprocess.run(
    ["git", "-C", REPO, "cat-file", "-p", "HEAD:" + REL],
    stdout=subprocess.PIPE, check=True).stdout

with io.open(ABS, "rb") as f:
    wt = f.read()

print("head CR count   =", head.count(b"\r"))
print("head CRLF count =", head.count(b"\r\n"))
print("wt   CR count   =", wt.count(b"\r"))
print("wt   CRLF count =", wt.count(b"\r\n"))

# 1) 假设: HEAD 用 \\r\\r\\n
norm = head.replace(b"\r\r\n", b"\r\n")
print("\nafter norm, head CR =", norm.count(b"\r"), " CRLF =", norm.count(b"\r\n"))

# 2) 逐行对比 norm 与 wt
h_lines = norm.split(b"\r\n")
w_lines = wt.split(b"\r\n")
print("norm lines =", len(h_lines), " wt lines =", len(w_lines))

import difflib
added = []
for d in difflib.unified_diff(h_lines, w_lines, lineterm=b"", n=1):
    if d.startswith("+") and not d.startswith("+++"):
        added.append(d)
print("\n--- 相对 HEAD 的新增行 (%d) ---" % len(added))
for a in added:
    print(a)

# 3) 还原行尾: 工作区 \\r\\n -> \\r\\r\\n
restored = wt.replace(b"\r\n", b"\r\r\n")
with io.open(ABS, "wb") as f:
    f.write(restored)
print("\nrestored: CR=%d CRLF=%d bytes=%d" % (
    restored.count(b"\r"), restored.count(b"\r\n"), len(restored)))
