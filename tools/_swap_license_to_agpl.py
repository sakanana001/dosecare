#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 LICENSE 从 GPL-3.0 换成 AGPL-3.0。

背景: 合并后的 DoseCare 血统是
  mkx173/Featherline (GPL-3.0) -> PicoHRT (GPL-3.0) -> DoseCare
AGPL-3.0 是 GPL-3.0 的超集 (GPL-3.0 的所有条款都包含在内), 所以
GPL-3.0 -> AGPL-3.0 是合法的单向上升; 反向 (AGPL 合进 GPL) 不合法。

文件结构:
  1. 顶部说明块 (copyright / SPDX / 为什么选 AGPL / fork 谱系)
  2. AGPL-3.0 完整全文 (从 gnu.org 授权的 license-templates 镜像取得)

底座 LICENSE 是 CRLF / 无 BOM, 新文件保持一致。
"""
import io
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, "LICENSE")
AGPL_SRC = r"C:\Users\yuwen\AppData\Local\Temp\agpl-try.txt"

HEADER = """                    GNU AFFERO GENERAL PUBLIC LICENSE
                       Version 3, 19 November 2007

 Copyright (C) 2026 DoseCare contributors

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published
 by the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.

 SPDX-License-Identifier: AGPL-3.0-or-later

 ---

 选用 AGPL-3.0 的理由:
 - GPL-3.0 在"网络服务"上有漏洞:医院可以私有部署、修改代码、不开源
 - AGPL-3.0 明确把"通过网络提供服务"也算"分发",强制要求衍生代码开源
 - 保护"用户贡献":如果医院部署后加了重要功能,必须回馈社区
 - 与未来医院合作的常见 AGPL 实践 (Nextcloud、Mastodon 等) 一致
 - 不影响个人使用、商业化集成(只要遵守条款)

 ---

 Fork 谱系与许可证沿革:
   mkx173/Featherline  —  GPL-3.0   (原始项目)
   sakanana001/PicoHRT —  GPL-3.0   (fork: 换品牌 + 三室 PK 引擎 + 凝胶缓释)
   sakanana001/DoseCare — AGPL-3.0  (合并 DoseCare 药理目录 / 规则引擎 / TDM)

 AGPL-3.0 是 GPL-3.0 的超集: GPL-3.0 的全部条款都包含在 AGPL-3.0 之内,
 所以 GPL-3.0 -> AGPL-3.0 是合法的单向上升(反向不合法)。完整条款见下方全文。

 ---------------------------------------------------------------------------
"""


def main():
    if not os.path.exists(AGPL_SRC):
        print("[ERROR] 找不到 AGPL 全文: %s" % AGPL_SRC)
        return 1
    with io.open(AGPL_SRC, "r", encoding="utf-8", newline="") as f:
        body = f.read()
    body = body.replace("\r\n", "\n").replace("\n", "\r\n")
    # 全文本身已含 "GNU AFFERO GENERAL PUBLIC LICENSE" 标题, 去掉重复的头部标题行
    body = body.strip("\r\n")
    lines = body.split("\r\n")
    # 保留全文从 "Preamble" 之前的许可声明开始, 去掉与 HEADER 重复的标题三行
    if lines and "GNU AFFERO GENERAL PUBLIC LICENSE" in lines[0]:
        body = "\r\n".join(lines[3:]).lstrip("\r\n")
    out = HEADER.replace("\n", "\r\n") + body + "\r\n"

    with io.open(TARGET, "w", encoding="utf-8", newline="") as f:
        f.write(out)

    with io.open(TARGET, "rb") as f:
        raw = f.read()
    text = raw.decode("utf-8")
    ok = ("GNU AFFERO GENERAL PUBLIC LICENSE" in text
          and "13. Remote Network Interaction" in text
          and "GPL-3.0 的超集" in text
          and not raw.startswith(b"\xef\xbb\xbf"))
    crlf = raw.count(b"\r\n")
    bare = raw.count(b"\n") - crlf
    print("LICENSE: %d bytes, CRLF=%d bareLF=%d" % (len(raw), crlf, bare))
    print("[%s] 校验" % ("OK" if ok else "FAIL"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
