# -*- coding: utf-8 -*-
"""One-off: normalise the given text files to CRLF (UTF-8, no BOM).

Used for the hand-written docs that were originally CRLF-terminated. Reads
bytes, so nothing else about the file is touched.
"""
import io
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

FILES = [
    "README.md",
    "README.zh-CN.md",
    "CONTRIBUTING.md",
    "docs/README.md",
    "docs/PROJECT.md",
    "docs/PROJECT.zh-Hans.md",
    "docs/architecture.md",
    "docs/data-model.md",
    "docs/localization.md",
    "docs/privacy.md",
    "docs/safety.md",
    "fastlane/metadata/android/en-US/title.txt",
    "fastlane/metadata/android/en-US/full_description.txt",
    "fastlane/metadata/android/zh-CN/title.txt",
    "fastlane/metadata/android/zh-CN/full_description.txt",
]


def to_crlf(rel):
    path = os.path.join(ROOT, rel)
    with io.open(path, "rb") as f:
        raw = f.read()
    had_bom = raw.startswith(b"\xef\xbb\xbf")
    if had_bom:
        raw = raw[3:]
    # Normalise every line ending to CRLF.
    body = raw.replace(b"\r\n", b"\n").replace(b"\r", b"\n").replace(b"\n", b"\r\n")
    with io.open(path, "wb") as f:
        f.write(body)
    print("CRLF  %s (bom stripped: %s)" % (rel, had_bom))


if __name__ == "__main__":
    for rel in (sys.argv[1:] or FILES):
        to_crlf(rel)
