# -*- coding: utf-8 -*-
"""从 HEAD 原始字节重建 DrugCatalog.kt, 只保留本轮真正的 1 行新增。

背景:
  - HEAD blob 里这个文件有 2 行用的是 \\r\\r\\n (仓库既有状态)
  - 本轮编辑脚本把它规范化成了全 CRLF, 并意外多插了 2 个空行
  - git 对该文件按原始字节比较, 所以那 2 个空行 + 2 个行尾变化让 diff 变成整文件重写

做法: 逐行拆 HEAD blob (保留每行原始行尾), 在正确位置插入 1 行新字段, 原样拼回。
"""
import difflib
import io
import os
import subprocess

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
REL = "app/src/main/java/com/dosecare/app/model/pharmacology/DrugCatalog.kt"
ABS = os.path.join(REPO, REL.replace("/", os.sep))
BAK = ABS + ".bak_wt"

head = subprocess.run(
    ["git", "-C", REPO, "cat-file", "-p", "HEAD:" + REL],
    stdout=subprocess.PIPE, check=True).stdout

with io.open(BAK, "rb") as f:
    cur = f.read()


def split_keep_terminator(data):
    out = []
    i = 0
    n = len(data)
    while i < n:
        j = data.find(b"\n", i)
        if j == -1:
            out.append((data[i:], b""))
            break
        out.append((data[i:j], b"\n"))
        i = j + 1
    return out


h = split_keep_terminator(head)
w = split_keep_terminator(cur)

h_txt = [c.decode("utf-8").rstrip("\r") for c, _ in h]
w_txt = [c.decode("utf-8").rstrip("\r") for c, _ in w]

sm = difflib.SequenceMatcher(None, h_txt, w_txt, autojunk=False)
inserts = []
for tag, i1, i2, j1, j2 in sm.get_opcodes():
    if tag == "equal":
        continue
    for k in range(j1, j2):
        inserts.append((i1, w[k][0].decode("utf-8")))

print("插入点:", inserts)
assert len(inserts) == 3, "预期 3 处插入(2 个误插空行 + 1 个新字段)"

# 只保留真正的字段那一行 (内容非空)
real = [(pos, txt) for pos, txt in inserts if txt.strip()]
assert len(real) == 1, real
pos, new_text = real[0]
print("保留插入: pos=%d  %r" % (pos, new_text))

# 拼回: head 的原始行 + 1 行新字段
out = bytearray()
for idx, (content, term) in enumerate(h):
    if idx == pos:
        out += new_text.encode("utf-8") + b"\r\n"
    out += content + term

result = bytes(out)
with io.open(ABS, "wb") as f:
    f.write(result)

print("\n写入: bytes=%d CR=%d CRLF=%d LF_only=%d" % (
    len(result), result.count(b"\r"), result.count(b"\r\n"),
    result.count(b"\n") - result.count(b"\r\n")))
