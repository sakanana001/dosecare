# -*- coding: utf-8 -*-
"""判定: DrugCatalog.kt 的整文件 diff 是"既有仓库状态"还是"本轮引入"。

做法: 把工作区还原成 HEAD blob 的原始字节, 再看 git diff --numstat。
  - 若 0/0  => git 对该文件按原始字节比较, 之前的整文件 diff 是本轮引入的
  - 若非 0  => 说明 HEAD 里存的是 CRLF 而 git 会把工作区 CRLF 归一成 LF,
               导致任何状态都显示整文件改动 (仓库既有问题, 与本轮无关)
"""
import io
import os
import shutil
import subprocess

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
REL = "app/src/main/java/com/dosecare/app/model/pharmacology/DrugCatalog.kt"
ABS = os.path.join(REPO, REL.replace("/", os.sep))
BAK = ABS + ".bak_wt"

head = subprocess.run(
    ["git", "-C", REPO, "cat-file", "-p", "HEAD:" + REL],
    stdout=subprocess.PIPE, check=True).stdout

with io.open(ABS, "rb") as f:
    cur = f.read()

if not os.path.exists(BAK):
    shutil.copyfile(ABS, BAK)
    print("backup ->", BAK)

# 还原成 HEAD 原始字节
with io.open(ABS, "wb") as f:
    f.write(head)

p = subprocess.run(
    ["git", "-C", REPO, "diff", "--numstat", "--", REL],
    stdout=subprocess.PIPE, stderr=subprocess.PIPE)
print("numstat with worktree == HEAD bytes:", p.stdout.decode().strip() or "(clean)")

# 还原回来
with io.open(ABS, "wb") as f:
    f.write(cur)
print("worktree restored from backup")
