# -*- coding: utf-8 -*-
"""One-off: rebrand residual PicoHRT strings in the docs/ set and fastlane metadata.

Byte-level read/write with newline='' so the original CRLF/LF mix is preserved
byte-for-byte. UTF-8, no BOM.
"""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

FILES = [
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
]

# Ordered: longest / most specific first.
PAIRS = [
    ("sakanana001/PicoHRT", "sakanana001/dosecare"),
    ("com/picohrt", "com/dosecare/app"),
    ("com.picohrt", "com.dosecare.app"),
    ("com/mkx/hrttracker", "com/dosecare/app"),
    ("com.mkx.hrttracker", "com.dosecare.app"),
    ("PicoHRT", "DoseCare"),
    ("picohrt-", "dosecare-"),
    ("GNU General Public License, version 3.0", "GNU Affero General Public License, version 3"),
    ("GPL-3.0", "AGPL-3.0"),
    ("GPL v3", "AGPL v3"),
]

# Commit-pinned GitHub URLs point at SHAs that do not exist in the renamed repo.
SHA_TREE = re.compile(r"/(blob|tree)/[0-9a-f]{7,40}/")


def rebrand(text):
    for old, new in PAIRS:
        text = text.replace(old, new)
    text = SHA_TREE.sub(r"/\1/main/", text)
    return text


def main():
    for rel in FILES:
        path = os.path.join(ROOT, rel)
        with io.open(path, "r", encoding="utf-8", newline="") as f:
            original = f.read()
        updated = rebrand(original)
        if updated == original:
            print("unchanged  %s" % rel)
            continue
        with io.open(path, "w", encoding="utf-8", newline="") as f:
            f.write(updated)
        print("rewritten  %s" % rel)


if __name__ == "__main__":
    main()
