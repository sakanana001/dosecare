#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""直接删那个超长路径的 classes.jar, 打印真实错误。"""
import os
import sys

TARGET = r"C:\Users\yuwen\Desktop\dosecare-workspace\baseline-picohrt\app\build\intermediates\compile_app_classes_jar\playDebug\bundlePlayDebugClassesToCompileJar\classes.jar"
LONG = "\\\\?\\"

for label, p in (("普通路径", TARGET), ("长路径前缀", LONG + TARGET)):
    print("--- %s ---" % label)
    print("  exists:", os.path.exists(p))
    if not os.path.exists(p):
        continue
    try:
        os.chmod(p, 0o700)
    except Exception as e:
        print("  chmod 失败:", e)
    try:
        os.remove(p)
        print("  [OK] 已删除")
    except Exception as e:
        print("  remove 失败: %s (%s)" % (e, type(e).__name__))
