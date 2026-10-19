# -*- coding: utf-8 -*-
"""One-off probe: compare string keys across the 5 locales of the merged app."""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

DIRS = ["values", "values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]
KEY_RE = re.compile(r'<(?:string|plurals|string-array)\s+name="([^"]+)"')

keys = {}
for d in DIRS:
    p = os.path.join(RES, d, "strings.xml")
    with io.open(p, encoding="utf-8") as f:
        content = f.read()
    ks = KEY_RE.findall(content)
    keys[d] = set(ks)
    print("%-24s %d keys" % (d, len(ks)))

base = keys["values"]
ja = keys["values-ja"]
print("\nmissing in ja: %d" % len(base - ja))
print("sample missing in ja:", sorted(base - ja)[:15])
print("\npresent in ja:", sorted(ja)[:15])

# check a few representative keys
for k in ["app_name", "tab_main", "app_language_japanese", "dossier_section_title",
          "cmp_section_basic", "tdm_no_window", "interactions_title"]:
    print("  %-24s ja=%s" % (k, k in ja))
