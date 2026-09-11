"""Patch positional MedicationLogEntryEntity() and MedicationLogEntryInput() call sites
in test sources to add `note = null` so the new schema column doesn't break the build.

Heuristic: only patch the constructor argument list when:
- The call is a top-level constructor (not preceded by a dot)
- All args are positional (no `=` in the args)
- `note` is not already an arg
"""
import re
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\featherline\app\src\test")
files = list(ROOT.rglob("*.kt"))

count_total = 0
count_changed = 0

for f in files:
    text = f.read_text(encoding="utf-8")
    orig = text
    for cls in ("MedicationLogEntryEntity", "MedicationLogEntryInput"):
        i = 0
        while True:
            idx = text.find(f"{cls}(", i)
            if idx < 0:
                break
            # skip method-chain calls (preceded by `.`)
            if idx > 0 and text[idx-1] == ".":
                i = idx + len(cls) + 1
                continue
            start = idx + len(cls) + 1
            depth = 1
            j = start
            while j < len(text) and depth > 0:
                c = text[j]
                if c == "(":
                    depth += 1
                elif c == ")":
                    depth -= 1
                j += 1
            if depth != 0:
                i = idx + 1
                continue
            # j points right after the matching `)`. Args are text[start:j-1].
            args = text[start:j-1]
            if "=" in args or "note" in args:
                i = j
                continue
            count_total += 1
            # Insert `note = null, ` at the start of args (after leading whitespace/newlines).
            stripped_lead = re.match(r"[\s\n]*", args).group(0)
            new_args = stripped_lead + "note = null, " + args[len(stripped_lead):]
            text = text[:start] + new_args + text[j-1:]
            count_changed += 1
            i = start + len(new_args) + 1
    if text != orig:
        f.write_text(text, encoding="utf-8")
        print("updated:", f.relative_to(ROOT))

print(f"total positional call sites patched: {count_changed}")
