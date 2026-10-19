# -*- coding: utf-8 -*-
"""Validate ja_gap_01.ja.json against ja_gap_01.json (keys, placeholders, non-empty)."""
import json
import re
import sys
from collections import Counter

BASE = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\tools\_ja_translate"
SRC = BASE + r"\ja_gap_01.json"
OUT = BASE + r"\ja_gap_01.ja.json"

PLACEHOLDER_RE = re.compile(r"%\d+\$[sd]|%(?:\d+\$)?[sd]|%\d+\$")

errors = []


def read_json(path):
    with open(path, "rb") as fh:
        raw = fh.read()
    if raw.startswith(b"\xef\xbb\xbf"):
        raw = raw[3:]
    return json.loads(raw.decode("utf-8"))


src = read_json(SRC)
out = read_json(OUT)

# 1) key sets identical
src_keys = set(src)
out_keys = set(out)
missing = sorted(src_keys - out_keys)
extra = sorted(out_keys - src_keys)
if missing:
    errors.append("missing keys: %s" % missing)
if extra:
    errors.append("extra keys: %s" % extra)
if not missing and not extra:
    print("OK  key sets identical (%d keys)" % len(src_keys))

# 2) placeholder multiset identical, 3) non-empty string values
for key in sorted(src_keys & out_keys):
    value = out[key]
    if not isinstance(value, str):
        errors.append("%s: value is not a string (%r)" % (key, type(value).__name__))
        continue
    if not value.strip():
        errors.append("%s: value is empty" % key)
        continue
    expected = Counter(src[key].get("placeholders", []))
    actual = Counter(PLACEHOLDER_RE.findall(value))
    if expected != actual:
        errors.append(
            "%s: placeholder mismatch expected=%s actual=%s (value=%r)"
            % (key, dict(expected), dict(actual), value)
        )
    # placeholder numbers present in value must be a subset of declared ones
    for ph in actual:
        if ph not in expected:
            errors.append("%s: undeclared placeholder %s" % (key, ph))

if not any("placeholder mismatch" in e or "undeclared" in e for e in errors):
    print("OK  placeholders match for all %d keys" % len(src_keys & out_keys))

if not any("empty" in e or "not a string" in e for e in errors):
    print("OK  all values are non-empty strings")

# 4) XML escaping sanity: no raw & < > ' in translated values
raw_bad = []
for key, value in out.items():
    if not isinstance(value, str):
        continue
    stripped = value.replace("&amp;", "").replace("&lt;", "").replace("&gt;", "").replace("&apos;", "")
    for ch in ("&", "<", ">", "'"):
        if ch in stripped:
            raw_bad.append((key, ch, value))
if raw_bad:
    for key, ch, value in raw_bad:
        errors.append("%s: unescaped %r in %r" % (key, ch, value))
else:
    print("OK  all values XML-escaped (& < > ' )")

if errors:
    print("\nFAILED (%d problem(s)):" % len(errors))
    for e in errors:
        print("  - %s" % e)
    sys.exit(1)

print("\nALL CHECKS PASSED")
