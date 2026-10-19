#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Port the drug-interaction UI strings from DoseCare (A) into merged (B).

Merge phase 2 - "interaction detection UI".

Source : A/app/src/main/res/values{,-en,-ja}/strings.xml
Target : B/app/src/main/res/values*/strings.xml

Locale mapping (same rule used for the 104 enum strings in phase 1):

    B values              <- A values-en   (B default locale is English)
    B values-b+zh+Hans    <- A values      (Simplified Chinese)
    B values-b+zh+Hant    <- A values-en   (English fallback, translate later)
    B values-b+yue+Hant+HK<- A values-en   (English fallback, translate later)

Keys already present in a target file are skipped, so the script is idempotent.

Usage:
    python tools/_merge_interaction_strings.py            # dry-run report
    python tools/_merge_interaction_strings.py --apply    # write changes
"""

from __future__ import annotations

import argparse
import io
import os
import re
import sys

REPO_A = r"C:\Users\yuwen\Desktop\dosecare-workspace\dosecare"
REPO_B = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"

A_RES = os.path.join(REPO_A, "app", "src", "main", "res")
B_RES = os.path.join(REPO_B, "app", "src", "main", "res")

# ---------------------------------------------------------------------------
# Keys required by ui/interaction/ (InteractionsTab + InteractionRow +
# MechanismLine + SearchableDrugPicker, ported in phase 2).
#
# The first 27 are the keys phase 2 originally scoped; the remainder come in
# with SearchableDrugPicker, which InteractionsTab depends on and which had to
# be ported alongside it (it lives in A's ui/SearchableDrugPicker.kt).
# ---------------------------------------------------------------------------
KEYS = [
    # -- scoped in phase 2 -------------------------------------------------
    "interaction_acb_format",
    "interaction_add_drug_n",
    "interaction_advice",
    "interaction_alerts_count",
    "interaction_card_title",
    "interaction_evaluate_n",
    "interaction_high_overdose_n",
    "interaction_info",
    "interaction_info_lines",
    "interaction_info_title",
    "interaction_more_drugs",
    "interaction_n_drugs_available",
    "interaction_no_alerts_desc",
    "interaction_no_alerts_title",
    "interaction_overdose_help",
    "interaction_qtc_format",
    "interaction_reevaluate_n",
    "interaction_references",
    "interaction_search_hint",
    "interaction_selected_n",
    "interaction_serotonin_format",
    "interaction_title",
    "compare_mechanism_auc",
    "common_known",
    "common_remove",
    "common_search",
    "common_settings",
    "drug_name_with_zh_subtitle",
    # -- pulled in by SearchableDrugPicker --------------------------------
    "search_placeholder",
    "search_drug_count",
    "drug_name_with_en_subtitle",
    "common_close",
    "common_clear",
    "search_select_title",
    "search_hint",
    "search_filtered",
    "search_count_with_query",
    "search_no_match",
    "search_brand_prefix",
]

# B locale dir -> A locale dir
LOCALE_MAP = [
    ("values", "values-en"),
    ("values-b+zh+Hans", "values"),
    ("values-b+zh+Hant", "values-en"),
    ("values-b+yue+Hant+HK", "values-en"),
]

# One <string name="...">...</string> element, anchored at the start of a line.
STRING_RE = re.compile(
    r'^[ \t]*<string[ \t]+name="(?P<name>[^"]+)"[^\n]*?>(?:.|\n)*?</string>[ \t]*\r?\n?',
    re.MULTILINE,
)


def read_text(path: str) -> str:
    """Read UTF-8, tolerating a BOM (A's strings.xml files have one)."""
    with io.open(path, "r", encoding="utf-8-sig", newline="") as fh:
        return fh.read()


def dominant_newline(text: str) -> str:
    return "\r\n" if "\r\n" in text else "\n"


def extract(text: str) -> dict:
    """Map resource name -> the raw source block (comments/indent preserved)."""
    out = {}
    for match in STRING_RE.finditer(text):
        out[match.group("name")] = match.group(0)
    return out


def existing_keys(text: str) -> set:
    return set(re.findall(r'<string[ \t]+name="([^"]+)"', text))


def with_newline(block: str, newline: str) -> str:
    return block.replace("\r\n", "\n").replace("\n", newline)


def merge_target(a_values: dict, a_present: set, b_dir: str, a_dir: str, apply: bool):
    b_path = os.path.join(B_RES, b_dir, "strings.xml")
    b_text = read_text(b_path)
    b_keys = existing_keys(b_text)
    newline = dominant_newline(b_text)

    added, skipped = [], []
    for key in KEYS:
        if key in b_keys:
            skipped.append(key)
            continue
        if key not in a_present:
            print("  !! key missing in A/%s: %s" % (a_dir, key))
            continue
        added.append(with_newline(a_values[key], newline))

    print("  %-22s <- A/%-9s  added=%-3d skipped=%-3d" % (b_dir, a_dir, len(added), len(skipped)))
    if skipped:
        print("       already present: %s" % ", ".join(skipped))

    if not added:
        return 0

    if not apply:
        print("       (dry-run) would insert %d entries" % len(added))
        return len(added)

    marker = "</resources>"
    idx = b_text.rindex(marker)
    header = (
        "%s<!-- ==== merge phase 2: drug interaction UI (from DoseCare A, A/%s) ==== -->%s"
        % (newline, a_dir, newline)
    )
    b_text = b_text[:idx] + header + "".join(added) + newline + b_text[idx:]
    with io.open(b_path, "w", encoding="utf-8", newline="") as fh:
        fh.write(b_text)
    print("       wrote %s" % b_path)
    return len(added)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="write changes (default: dry-run)")
    args = parser.parse_args()

    cache = {}
    for _, a_dir in LOCALE_MAP:
        if a_dir in cache:
            continue
        a_text = read_text(os.path.join(A_RES, a_dir, "strings.xml"))
        cache[a_dir] = (extract(a_text), existing_keys(a_text))
        print("read A/%s/strings.xml -> %d <string> entries" % (a_dir, len(cache[a_dir][0])))

    print("")
    total = 0
    for b_dir, a_dir in LOCALE_MAP:
        a_values, a_present = cache[a_dir]
        total += merge_target(a_values, a_present, b_dir, a_dir, args.apply)

    print("")
    print("%s  total entries written: %d" % ("APPLIED" if args.apply else "DRY-RUN", total))
    return 0


if __name__ == "__main__":
    sys.exit(main())
