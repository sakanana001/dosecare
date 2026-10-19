# -*- coding: utf-8 -*-
"""Validate ja_gap_05.ja.json against the source gap list.

Checks:
  1. key sets are identical (no missing / no extra)
  2. placeholder multiset per key matches the expected set
  3. every output value is a non-empty string
  4. no raw XML-special chars (&, <, >, ') left unescaped
  5. file is valid UTF-8 without BOM
  6. no U+FFFD replacement characters left in the output

The expected placeholder set per key is the union of the export tool's
"placeholders" list and every format specifier found in the source "en"
string (the export regex only captures numbered specifiers, so this union
is a superset safety net).
"""
import json
import re
import sys
from collections import Counter
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC = BASE / "ja_gap_05.json"
OUT = BASE / "ja_gap_05.ja.json"

# any android/java format specifier: %s %d %1$s %2$.1f ...
PLACEHOLDER_RE = re.compile(r"%\d*\$?[\d.]*[a-zA-Z]")
RAW_XML_RE = re.compile(r"[&<>'](?![a-zA-Z]+;)")


def fail(msg):
    print("FAIL: " + msg)
    sys.exit(1)


def specifiers(text):
    return Counter(m.group(0) for m in PLACEHOLDER_RE.finditer(text))


def main():
    raw = OUT.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        fail("output has a UTF-8 BOM")
    try:
        out = json.loads(raw.decode("utf-8"))
    except Exception as exc:  # noqa: BLE001
        fail("output is not valid UTF-8 JSON: %s" % exc)

    src = json.loads(SRC.read_bytes().decode("utf-8-sig"))

    if not isinstance(out, dict):
        fail("output JSON must be a flat dict")

    src_keys = set(src.keys())
    out_keys = set(out.keys())

    missing = sorted(src_keys - out_keys)
    extra = sorted(out_keys - src_keys)
    if missing:
        fail("missing keys (%d): %s" % (len(missing), missing))
    if extra:
        fail("extra keys (%d): %s" % (len(extra), extra))

    bad_ph = []
    empty = []
    bad_xml = []
    unlisted = []
    bad_chars = []

    for key in sorted(src_keys):
        value = out[key]
        if not isinstance(value, str) or not value.strip():
            empty.append(key)
            continue

        listed = Counter(src[key].get("placeholders", []))
        in_source = specifiers(src[key].get("en", ""))
        expected = listed | in_source  # union: max count per token
        actual = specifiers(value)

        if in_source - listed:
            unlisted.append((key, sorted((in_source - listed).elements())))

        if actual != expected:
            bad_ph.append((key, sorted(expected.elements()), sorted(actual.elements())))

        if RAW_XML_RE.search(value):
            bad_xml.append((key, value))

        if "\ufffd" in value:
            bad_chars.append((key, value))

    if empty:
        fail("empty / non-string values: %s" % empty)
    if bad_ph:
        for key, exp, act in bad_ph:
            print("  placeholder mismatch %s: expected %s got %s" % (key, exp, act))
        fail("placeholder mismatch in %d keys" % len(bad_ph))
    if bad_xml:
        for key, value in bad_xml:
            print("  raw XML char in %s: %s" % (key, value))
        fail("unescaped XML special characters in %d values" % len(bad_xml))
    if bad_chars:
        for key, value in bad_chars:
            print("  replacement char in %s: %s" % (key, value))
        fail("U+FFFD replacement characters in %d values" % len(bad_chars))

    print("OK: %d keys" % len(out))
    print("  key sets identical: yes")
    print("  placeholders matched: yes")
    print("  all values non-empty strings: yes")
    print("  no unescaped XML chars: yes")
    print("  no replacement characters: yes")
    print("  utf-8 without BOM: yes")
    if unlisted:
        print("  note: specifiers present in source 'en' but absent from the")
        print("        export tool's 'placeholders' list (kept in translation):")
        for key, toks in unlisted:
            print("        %s -> %s" % (key, toks))


if __name__ == "__main__":
    main()
