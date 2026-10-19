# -*- coding: utf-8 -*-
"""One-off probe: report line endings / BOM / size for the docs task file set."""
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

TARGETS = [
    "README.md",
    "README.zh-CN.md",
    "CONTRIBUTING.md",
    "docs/README.md",
    "docs/PROJECT.md",
    "docs/PROJECT.zh-Hans.md",
    "docs/architecture.md",
    "docs/backup-format.md",
    "docs/blood-tests.md",
    "docs/building.md",
    "docs/data-model.md",
    "docs/ftm-hrt.md",
    "docs/localization.md",
    "docs/pk-differences.md",
    "docs/privacy.md",
    "docs/release-process.md",
    "docs/reminders.md",
    "docs/safety.md",
    "docs/testing.md",
    "docs/widget.md",
    "docs/third-party-notices.md",
    "fastlane/metadata/android/en-US/title.txt",
    "fastlane/metadata/android/en-US/full_description.txt",
    "fastlane/metadata/android/zh-CN/title.txt",
    "fastlane/metadata/android/zh-CN/full_description.txt",
]

for rel in TARGETS:
    p = os.path.join(ROOT, rel)
    if not os.path.exists(p):
        print("MISSING  %s" % rel)
        continue
    with io.open(p, "rb") as f:
        raw = f.read()
    bom = raw.startswith(b"\xef\xbb\xbf")
    crlf = raw.count(b"\r\n")
    lf_total = raw.count(b"\n")
    lone_lf = lf_total - crlf
    kind = "CRLF" if crlf and not lone_lf else ("LF" if lone_lf and not crlf else "MIXED")
    print("%-10s bom=%-5s crlf=%-5d loneLF=%-5d bytes=%-7d %s" % (kind, bom, crlf, lone_lf, len(raw), rel))
