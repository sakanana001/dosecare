#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Replace 慎药 with DoseCare across all doc/text files in the project.
Special cases (bilingual mentions) get cleaned up to drop the redundant Chinese name.
"""
from pathlib import Path
import re

PROJECT_ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")

# Files to update (relative to project root)
TARGETS = [
    "CHANGELOG.md",
    "CONTRIBUTING.md",
    "README.md",
    "SECURITY.md",
    ".gitignore",
    "docs/00-VISION.md",
    "docs/08-SYNC_ACCOUNT_FUTURE.md",
    "docs/SETUP.md",
    # progress.html is excluded by .gitignore and stale; leaving it alone
]

# Specific patterns to clean up (in addition to plain 慎药 -> DoseCare)
SPECIAL_PATTERNS = [
    # 慎药 / DoseCare -> DoseCare
    (r"慎药\s*/\s*DoseCare", "DoseCare"),
    # DoseCare（中文名：慎药） -> DoseCare  (with full-width parens)
    (r"DoseCare[（(]中文名[：:]\s*慎药[）)]", "DoseCare"),
    # （慎药）  -> (DoseCare)
    (r"[（(]慎药[）)]", "DoseCare"),
    # DoseCare(慎药)  -> DoseCare
    (r"DoseCare\s*[（(]慎药[）)]", "DoseCare"),
    # Just 慎药 -> DoseCare (catch-all last)
    (r"慎药", "DoseCare"),
]


def process_file(path: Path) -> tuple[int, int]:
    """Return (changes_made, original_size) tuple."""
    if not path.exists():
        return (0, 0)
    original = path.read_text(encoding="utf-8")
    if "慎药" not in original:
        return (0, len(original))

    new = original
    for pat, repl in SPECIAL_PATTERNS:
        new = re.sub(pat, repl, new)

    if new != original:
        path.write_text(new, encoding="utf-8")
        return (1, len(original))
    return (0, len(original))


def main():
    total_changes = 0
    for rel in TARGETS:
        path = PROJECT_ROOT / rel
        changed, size = process_file(path)
        status = "OK " if changed else "-- "
        total_changes += changed
        print(f"  {status} {rel:40s} {size:>6} bytes")
    print(f"\nTotal files changed: {total_changes} / {len(TARGETS)}")


if __name__ == "__main__":
    main()
