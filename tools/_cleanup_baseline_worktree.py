#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
删掉验证用的临时基线副本 baseline-picohrt (剩下的深层 build 缓存)。

难点: 路径太长, shutil.rmtree 直接删会失败 (Filename too long)。
办法: 先把最深的目录用长路径前缀重命名成短名字, 再从深到浅逐层删。

只处理这个明确指定的临时目录。
"""
import os
import shutil
import sys

TARGET = r"C:\Users\yuwen\Desktop\dosecare-workspace\baseline-picohrt"
LONG = "\\\\?\\"


def long(p):
    return LONG + os.path.abspath(p)


def rm_walk(path):
    """从深到浅删, 每层都走长路径前缀。"""
    deleted = 0
    for dirpath, dirnames, filenames in os.walk(long(path)):
        for fn in filenames:
            fp = os.path.join(dirpath, fn)
            try:
                os.chmod(fp, 0o700)
                os.remove(fp)
                deleted += 1
            except Exception:
                pass
        for dn in dirnames:
            dp = os.path.join(dirpath, dn)
            try:
                os.rmdir(dp)
            except Exception:
                pass
    # 自己
    try:
        os.rmdir(long(path))
        deleted += 1
    except Exception:
        pass
    return deleted


def main():
    if not os.path.isdir(TARGET):
        print("目录不存在, 无需清理")
        return 0
    n = rm_walk(TARGET)
    left = os.path.isdir(TARGET)
    if left:
        # 再试一次普通 rmtree
        try:
            shutil.rmtree(TARGET, ignore_errors=True)
        except Exception:
            pass
        left = os.path.isdir(TARGET)
    print("删除 %d 项; 目录仍存在? %s" % (n, left))
    if left:
        for dp, dn, fn in os.walk(TARGET):
            for f in fn[:3]:
                print("  残留: " + os.path.join(dp, f))
            break
        return 1
    print("[OK] 临时基线副本已清理干净")
    return 0


if __name__ == "__main__":
    sys.exit(main())
