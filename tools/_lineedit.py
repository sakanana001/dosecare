# -*- coding: utf-8 -*-
"""字节级行编辑器: 保留每个文件原有的行尾风格 (含本仓库特有的 \\r\\r\\n 双 CR)。

为什么需要它:
  仓库里有一批文件在 HEAD 里是用 \\r\\r\\n (双 CR) 存的 (历史脚本 bug)。
  用文本模式读 (universal newlines) 会把 \\r\\r\\n 拆成两个换行, 写回去就变成
  普通 \\n, 于是 git 认为整个文件被重写 (几百行假 diff)。

本模块只做字节级操作:
  - split_lines(data) -> [(content_bytes, terminator_bytes), ...]
  - join_lines(lines) -> bytes
  - replace_line_content(data, predicate, fn) -> bytes
"""
import io
import os


def split_lines(data):
    """按 b'\\n' 切分, 保留每行内容与原始行尾 (行尾含所有前置的 \\r)。"""
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


def join_lines(lines):
    return b"".join(content + term for content, term in lines)


def read_bytes(path):
    with io.open(path, "rb") as f:
        return f.read()


def write_bytes(path, data):
    with io.open(path, "wb") as f:
        f.write(data)


def replace_line_content(data, key, new_content, name_attr=b'name="'):
    """把 <string name="KEY">...</string> 这类单行元素的整行内容替换掉。

    key: 形如 b'name="interaction_title"'
    new_content: 该元素的完整内部文本 (bytes), 会做 XML 实体转义由调用方负责
    返回 (新数据, 是否命中)
    """
    needle = name_attr + key + b'"'
    lines = split_lines(data)
    for idx, (content, term) in enumerate(lines):
        if needle not in content:
            continue
        head, sep, tail = content.partition(b">")
        if not sep:
            continue
        # 形如 <string name="x">VALUE</string>
        end = tail.rfind(b"</")
        if end == -1:
            continue
        new_line = head + sep + new_content + tail[end:]
        lines[idx] = (new_line, term)
        return join_lines(lines), True
    return data, False


def describe(path):
    data = read_bytes(path)
    crlf = data.count(b"\r\n")
    lf = data.count(b"\n")
    lones = data.count(b"\r") - crlf
    return "CRLF=%d LF=%d 额外CR=%d bytes=%d" % (crlf, lf, lones, len(data))


if __name__ == "__main__":
    import sys
    for p in sys.argv[1:]:
        print("%-70s %s" % (os.path.basename(p), describe(p)))
