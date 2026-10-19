#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Byte-copy the generated/source Kotlin helpers that the interaction UI needs.

Merge phase 2 - "interaction detection UI".

InteractionsTab (A: ui/BottomNavTabs.kt) renders its drug picker through
A's SearchableDrugPicker (A: ui/SearchableDrugPicker.kt). B had no equivalent,
so the file comes across verbatim; only the package declaration, the Drug
import and the enum->string-resource accessor are retargeted.

Why a script and not a manual copy: the file is 1372 lines, ~1080 of which are
a generated pinyin-initial dictionary ("\\u4e00" to "y", ...). Re-typing it would
risk silent data corruption; copying the bytes cannot.

Substitutions applied (everything else is byte-identical to A):
  package com.dosecare.app.ui                      -> ...ui.interaction
  import com.dosecare.app.domain.catalog.Drug      -> ...model.pharmacology.Drug
  drug.category.displayNameRes                     -> drug.category.labelRes
     (phase 1 moved the enum -> @StringRes mapping out of the domain layer and
      into ui/interaction/InteractionLabels.kt, so the domain `displayNameRes`
      constructor property no longer exists in B)

Usage:
    python tools/_merge_interaction_sources.py            # dry-run report
    python tools/_merge_interaction_sources.py --apply    # write files
"""

from __future__ import annotations

import argparse
import io
import os
import sys

REPO_A = r"C:\Users\yuwen\Desktop\dosecare-workspace\dosecare"
REPO_B = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"

A_SRC = os.path.join(REPO_A, "app", "src", "main", "java", "com", "dosecare", "app", "ui")
B_PKG = os.path.join(REPO_B, "app", "src", "main", "java", "com", "dosecare", "app", "ui", "interaction")

# (source file in A, destination file name in B)
COPIES = [
    ("SearchableDrugPicker.kt", "SearchableDrugPicker.kt"),
]

REPLACEMENTS = [
    ("package com.dosecare.app.ui\n", "package com.dosecare.app.ui.interaction\n", 1),
    (
        "import com.dosecare.app.domain.catalog.Drug\n",
        "import com.dosecare.app.model.pharmacology.Drug\n",
        1,
    ),
    ("drug.category.displayNameRes", "drug.category.labelRes", None),
]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="write files (default: dry-run)")
    args = parser.parse_args()

    if not os.path.isdir(B_PKG):
        print("destination package missing: %s" % B_PKG)
        return 1

    for src_name, dst_name in COPIES:
        src = os.path.join(A_SRC, src_name)
        dst = os.path.join(B_PKG, dst_name)
        with io.open(src, "r", encoding="utf-8-sig", newline="") as fh:
            text = fh.read()

        # Work in LF internally so the patterns below stay readable, then write
        # back CRLF (A's sources and B's .kt sources are both CRLF).
        crlf_source = "\r\n" in text
        text = text.replace("\r\n", "\n")

        for old, new, expected in REPLACEMENTS:
            count = text.count(old)
            if expected is not None and count != expected:
                print("  !! %s: expected %d occurrence(s) of %r, found %d"
                      % (src_name, expected, old.strip(), count))
                return 1
            text = text.replace(old, new)
            print("  %-28s %r -> %r  x%d" % (src_name, old.strip()[:48], new.strip()[:48], count))

        out = text.replace("\n", "\r\n") if crlf_source else text
        print("  %-28s -> %s  (%d bytes, source_crlf=%s)"
              % (src_name, dst_name, len(out.encode("utf-8")), crlf_source))

        if args.apply:
            with io.open(dst, "w", encoding="utf-8", newline="") as fh:
                fh.write(out)
            print("  wrote %s" % dst)
        else:
            print("  (dry-run) would write %s" % dst)

    return 0


if __name__ == "__main__":
    sys.exit(main())
