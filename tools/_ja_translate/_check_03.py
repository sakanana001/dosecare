# -*- coding: utf-8 -*-
"""Validate ja_gap_03.ja.json against ja_gap_03.json.

Checks:
  1. key sets are identical (no missing / no extra keys)
  2. placeholder multiset per key matches the source `placeholders` field
  3. every output value is a non-empty string
  4. no duplicate keys in either JSON file
  5. no raw XML-special characters (& < > ') left unescaped in any value
"""
import json
import pathlib
import re
import sys

BASE = pathlib.Path(__file__).resolve().parent
SRC_PATH = BASE / "ja_gap_03.json"
OUT_PATH = BASE / "ja_gap_03.ja.json"

# Android format specifier: %1$s / %2$.2f / %3$d ...
PH_RE = re.compile(r"%\d+\$[sdf]|%\d+[sdf]")
XML_SPECIAL_RE = re.compile(r"[&<>'\"]")


def load_with_dup_check(path):
    """Load JSON and report duplicate object keys."""
    dups = []

    def hook(pairs):
        seen = set()
        for k, _ in pairs:
            if k in seen:
                dups.append(k)
            seen.add(k)
        return dict(pairs)

    data = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=hook)
    return data, dups


def main():
    errors = []

    src, src_dups = load_with_dup_check(SRC_PATH)
    out, out_dups = load_with_dup_check(OUT_PATH)

    if src_dups:
        errors.append("duplicate keys in source: %s" % sorted(set(src_dups)))
    if out_dups:
        errors.append("duplicate keys in output: %s" % sorted(set(out_dups)))

    src_keys = set(src)
    out_keys = set(out)
    missing = sorted(src_keys - out_keys)
    extra = sorted(out_keys - src_keys)
    if missing:
        errors.append("missing keys (%d): %s" % (len(missing), missing))
    if extra:
        errors.append("extra keys (%d): %s" % (len(extra), extra))

    empty = [k for k, v in out.items() if not isinstance(v, str) or not v.strip()]
    if empty:
        errors.append("empty / non-string values (%d): %s" % (len(empty), empty))

    bad_ph = []
    for key in sorted(src_keys & out_keys):
        expected = sorted(src[key]["placeholders"])
        actual = sorted(PH_RE.findall(out[key]))
        if expected != actual:
            bad_ph.append((key, expected, actual))
    if bad_ph:
        for key, expected, actual in bad_ph:
            errors.append("placeholder mismatch %s: expected %s got %s" % (key, expected, actual))

    raw_xml = []
    for key, value in out.items():
        if isinstance(value, str) and XML_SPECIAL_RE.search(value):
            raw_xml.append((key, XML_SPECIAL_RE.findall(value)))
    if raw_xml:
        errors.append("raw XML-special chars needing escaping: %s" % raw_xml)

    print("source keys : %d" % len(src_keys))
    print("output keys : %d" % len(out_keys))
    print("with placeholders (source): %d" % sum(1 for v in src.values() if v["placeholders"]))
    print("with placeholders (output): %d" % sum(1 for v in out.values() if PH_RE.findall(v)))

    if errors:
        print("\nFAILED with %d problem(s):" % len(errors))
        for e in errors:
            print("  - %s" % e)
        return 1

    print("\nALL CHECKS PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
