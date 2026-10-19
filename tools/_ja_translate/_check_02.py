#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Validate ja_gap_02.ja.json against the source batch ja_gap_02.json.

Checks:
  1. key sets are identical (no missing / no extra)
  2. per key, the multiset of format placeholders in the translation
     matches the source "placeholders" list exactly (same strings, same count)
  3. every translation is a non-empty string
  4. no raw XML-special characters (unescaped & < > ') in the values
  5. the file is valid UTF-8 without a BOM
"""

import json
import re
import sys
from collections import Counter
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC = BASE / "ja_gap_02.json"
OUT = BASE / "ja_gap_02.ja.json"

PLACEHOLDER_RE = re.compile(r"%\d+\$[sd]")


def fail(msg):
    print("FAIL: " + msg)
    sys.exit(1)


def main():
    raw = OUT.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        fail("output file starts with a UTF-8 BOM")
    try:
        raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        fail("output file is not valid UTF-8: %s" % exc)

    src = json.loads(SRC.read_text(encoding="utf-8"))
    out = json.loads(raw.decode("utf-8"))

    if not isinstance(out, dict):
        fail("output JSON is not an object")

    src_keys = set(src)
    out_keys = set(out)
    missing = sorted(src_keys - out_keys)
    extra = sorted(out_keys - src_keys)
    if missing:
        fail("missing keys (%d): %s" % (len(missing), missing))
    if extra:
        fail("extra keys (%d): %s" % (len(extra), extra))

    bad_placeholder = []
    empty = []
    bad_xml = []

    for key in sorted(src_keys):
        value = out[key]
        expected = src[key].get("placeholders", []) or []

        if not isinstance(value, str) or not value.strip():
            empty.append(key)
            continue

        got = Counter(PLACEHOLDER_RE.findall(value))
        want = Counter(expected)
        if got != want:
            bad_placeholder.append((key, sorted(want.elements()), sorted(got.elements())))

        for ch in ("&", "<", ">", "'"):
            if ch in value and not re.search(
                r"&(amp|lt|gt|apos|quot|#\d+|#x[0-9A-Fa-f]+);", value
            ):
                bad_xml.append((key, ch))

    if empty:
        fail("empty / non-string values (%d): %s" % (len(empty), empty))
    if bad_placeholder:
        for key, want, got in bad_placeholder:
            print("  placeholder mismatch: %s want=%s got=%s" % (key, want, got))
        fail("placeholder mismatch in %d key(s)" % len(bad_placeholder))
    if bad_xml:
        for key, ch in bad_xml:
            print("  unescaped XML char: %s -> %r" % (key, ch))
        fail("unescaped XML characters in %d key(s)" % len(bad_xml))

    with_ph = sum(1 for k in src_keys if src[k].get("placeholders"))
    print("OK  keys=%d (source=%d, identical)  with_placeholders=%d" % (
        len(out_keys), len(src_keys), with_ph))
    print("OK  all values non-empty strings")
    print("OK  placeholder multisets match for every key")
    print("OK  no raw XML-special characters")
    print("OK  valid UTF-8, no BOM")


if __name__ == "__main__":
    main()
