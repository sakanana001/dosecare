#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抽样查看港繁 / 繁中现有行文风格, 供新译文对齐。"""
import io
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')

PROBE = [
    "settings_hrt_goal", "settings_reminder_title", "settings_reminder_summary",
    "settings_theme_title", "settings_theme_system", "settings_backup_title",
    "settings_language_title", "settings_lock_title", "settings_lock_summary",
    "safety_medication_conflict_title", "safety_medication_conflict_body",
    "safety_overdose_title", "safety_interaction_title",
    "main_chart_hormone", "main_chart_hormone_e2", "main_chart_title",
    "medication_name_testosterone_enanthate", "medication_name_estradiol",
    "common_confirm", "common_cancel", "common_save", "common_close",
    "settings_about_title", "settings_about_summary", "settings_data_title",
]

for loc in ["values-b+zh+Hant", "values-b+yue+Hant+HK", "values-b+zh+Hans"]:
    p = os.path.join(RES, loc, "strings.xml")
    t = io.open(p, "r", encoding="utf-8").read()
    d = {k: v for k, v in STR_RE.findall(t)}
    print("======== %s ========" % loc)
    for k in PROBE:
        if k in d:
            print("  %-40s = %s" % (k, d[k].replace("\n", "\\n")[:150]))
    print()
