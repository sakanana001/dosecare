#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
删掉验证用的临时基线副本 (git worktree remove 删不掉的那些)。

用法: python _cleanup_baseline_worktree.py [目录路径]
默认 C:\\Users\\yuwen\\Desktop\\dosecare-workspace\\baseline-picohrt

难点: gradle build 目录里的路径太长, 普通删除会报 "Filename too long";
另外 gradle daemon 可能还持有 classes.jar 的句柄 (WinError 32), 需要先
`gradlew --stop`。

只处理明确指定的临时目录, 不碰任何仓库。
"""
import os
import shutil
import sys

LONG = "\\\\?\\"


def rm_walk(path):
    """从深到浅删, 每层都走长路径前缀。"""
    deleted = 0
    for dirpath, dirnames, filenames in os.walk(LONG + os.path.abspath(path)):
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
    try:
        os.rmdir(LONG + os.path.abspath(path))
        deleted += 1
    except Exception:
        pass
    return deleted


def main():
    target = sys.argv[1] if len(sys.argv) > 1 else \
        r"C:\Users\yuwen\Desktop\dosecare-workspace\baseline-picohrt"
    target = os.path.abspath(target)
    if not os.path.isdir(target):
        print("目录不存在, 无需清理: %s" % target)
        return 0

    n = rm_walk(target)
    if os.path.isdir(target):
        shutil.rmtree(target, ignore_errors=True)
    left = os.path.isdir(target)
    print("删除 %d 项; 目录仍存在? %s" % (n, left))
    if left:
        return 1
    print("[OK] 临时基线副本已清理干净: %s" % target)
    return 0


if __name__ == "__main__":
    sys.exit(main())
