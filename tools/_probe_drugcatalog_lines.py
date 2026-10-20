# -*- coding: utf-8 -*-
"""精确定位 DrugCatalog.kt: HEAD blob 的行尾结构 + 本轮新增的 3 行位置。"""
import difflib
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


def split_keep_terminator(data):
    """返回 [(content_without_CR, terminator_bytes), ...]"""
    out = []
    i = 0
    n = len(data)
    while i < n:
        j = data.find(b"\n", i)
        if j == -1:
            out.append((data[i:], b""))
            break
        content = data[i:j]
        # 去掉结尾的 CR (可能一个或两个)
        crs = 0
        while content.endswith(b"\r"):
            content = content[:-1]
            crs += 1
        out.append((content, b"\r" * crs + b"\n"))
        i = j + 1
    return out


h = split_keep_terminator(head)
w = split_keep_terminator(wt)
print("head lines=%d  wt lines=%d" % (len(h), len(w)))
from collections import Counter
print("head terminators:", Counter(t for _, t in h))
print("wt   terminators:", Counter(t for _, t in w))

h_txt = [c.decode("utf-8") for c, _ in h]
w_txt = [c.decode("utf-8") for c, _ in w]
sm = difflib.SequenceMatcher(None, h_txt, w_txt, autojunk=False)
for tag, i1, i2, j1, j2 in sm.get_opcodes():
    if tag == "equal":
        continue
    print("\n%s  head[%d:%d] -> wt[%d:%d]" % (tag, i1, i2, j1, j2))
    for k in range(i1, i2):
        print("  - [%d] %r (term=%r)" % (k, h_txt[k][:90], h[k][1]))
    for k in range(j1, j2):
        print("  + [%d] %r (term=%r)" % (k, w_txt[k][:90], w[k][1]))
