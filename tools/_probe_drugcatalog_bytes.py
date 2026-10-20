# -*- coding: utf-8 -*-
"""彻底搞清楚 DrugCatalog.kt 的整文件 diff 根因: HEAD blob / index blob / 工作区 三方对比。"""
import io
import os
import subprocess

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"
REL = "app/src/main/java/com/dosecare/app/model/pharmacology/DrugCatalog.kt"


def git_bytes(args):
    p = subprocess.run(["git", "-C", REPO] + args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return p.stdout


def describe(name, data):
    crlf = data.count(b"\r\n")
    lf = data.count(b"\n") - crlf
    nul = data.count(b"\x00")
    print("%-14s len=%-8d CRLF=%-5d LF_only=%-5d NUL=%-4d" % (name, len(data), crlf, lf, nul))
    return data


head = describe("HEAD-blob", git_bytes(["cat-file", "-p", "HEAD:" + REL]))
idx = describe("index-blob", git_bytes(["cat-file", "-p", ":" + REL]))
with io.open(os.path.join(REPO, REL.replace("/", os.sep)), "rb") as f:
    wt = describe("worktree", f.read())

print()
print("head == idx ?", head == idx)
print("idx == wt  ?", idx == wt)
print("head CRLF-normalized == idx ?", head.replace(b"\r\n", b"\n") == idx.replace(b"\r\n", b"\n"))
print()
print("--- head first 200 bytes repr ---")
print(repr(head[:200]))
print("--- idx first 200 bytes repr ---")
print(repr(idx[:200]))
print("--- wt first 200 bytes repr ---")
print(repr(open(os.path.join(REPO, REL.replace("/", os.sep)), "rb").read()[:200]))
