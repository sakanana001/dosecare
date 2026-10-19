#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
导出日语缺口清单, 供分批翻译用。

输出两个文件到 tools/_ja_translate/:
  ja_gap_NN.json  — 每批的 {key: 英文原文} 映射
  ja_manifest.json — 全部批次与 key 总数

同时做占位符标注: 带 % 的 key 会被标出来, 翻译时必须原样保留占位符。

分批规则:
  - 按功能分组的顺序切 (settings / journal / medication / stock / plan / ...)
  - 每批不超过 240 个 key
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
EN_FILE = os.path.join(RES, "values", "strings.xml")
JA_FILE = os.path.join(RES, "values-ja", "strings.xml")
OUT_DIR = os.path.join(ROOT, "tools", "_ja_translate")

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')

BATCH_SIZE = 240


def parse(path):
    d = {}
    for m in STR_RE.finditer(io.open(path, "r", encoding="utf-8").read()):
        d.setdefault(m.group(1), m.group(2))
    return d


def main():
    en = parse(EN_FILE)
    ja = parse(JA_FILE)
    gap = {k: v for k, v in en.items() if k not in ja}
    print("英文 key %d / 日语已有 %d / 缺口 %d" % (len(en), len(ja), len(gap)))

    # 按功能分组排序, 让同一批里的 key 属于同一块界面
    groups = [
        ("settings", ("settings_", "setting_", "app_language_", "dark_mode", "first_day_",
                      "lock_", "screen_lock", "biometric", "haze", "adaptive", "pure_black",
                      "cjk_", "reminder_", "notification_", "calibration_", "unit_",
                      "language_", "theme_", "accent", "seed_")),
        ("journal", ("journal_", "milestone", "anchor_", "note_", "hero_", "pinned_",
                     "timeline_", "all_notes", "add_date")),
        ("medication", ("medication_", "medicine_", "dose_", "stock_", "route_",
                        "preparation", "application_", "prefix_", "trade_name", "generic_",
                        "brand_", "strength", "form_", "patch_", "gel_", "oral_",
                        "sublingual", "inject_", "vial", "syringe", "ampoule", "blister",
                        "quantity", "expiry", "batch_", "lot_")),
        ("plan", ("plan_", "group_", "schedule_", "slot_", "reminder_", "take_",
                  "logged_", "dose_log", "entry_", "archive")),
        ("home", ("home_", "main_", "chart_", "widget_", "hrt_goal", "hormone",
                  "e2_", "t_", "cpa_", "trend_", "target_", "weight_", "body_")),
        ("common", ("common_", "cancel", "save", "delete", "edit", "add_", "remove",
                    "confirm", "back", "next", "done", "close", "yes", "no_", "ok",
                    "search", "filter", "sort", "today", "tomorrow", "yesterday",
                    "loading", "error", "empty_", "unknown", "all_", "none_", "more_",
                    "warning", "info", "success", "retry", "reset", "undo")),
    ]

    def bucket_of(key):
        for name, prefixes in groups:
            for p in prefixes:
                if key.startswith(p) or p in key:
                    return name
        return "misc"

    by_bucket = {}
    for k, v in gap.items():
        by_bucket.setdefault(bucket_of(k), []).append(k)

    # 输出每桶统计
    print("\n分组:")
    for name, _ in groups + [("misc", ())]:
        keys = by_bucket.get(name, [])
        if keys:
            print("  %-14s %d" % (name, len(keys)))

    # 切批: 同一组尽量不跨批
    batches = []
    current = []
    for name, _ in groups + [("misc", ())]:
        keys = sorted(by_bucket.get(name, []))
        i = 0
        while i < len(keys):
            take = keys[i:i + BATCH_SIZE]
            if current and len(current) + len(take) > BATCH_SIZE:
                batches.append(current)
                current = []
            current.extend(take)
            i += BATCH_SIZE
    if current:
        batches.append(current)

    if not os.path.isdir(OUT_DIR):
        os.makedirs(OUT_DIR)

    manifest = []
    for i, batch in enumerate(batches, 1):
        payload = {}
        for k in batch:
            payload[k] = {
                "en": gap[k],
                "placeholders": sorted(m.group(0) for m in PH_RE.finditer(gap[k])),
            }
        name = "ja_gap_%02d.json" % i
        path = os.path.join(OUT_DIR, name)
        with io.open(path, "w", encoding="utf-8") as f:
            f.write(json.dumps(payload, ensure_ascii=False, indent=2))
        manifest.append({"file": name, "count": len(batch),
                         "with_placeholders": sum(1 for k in batch if PH_RE.search(gap[k]))})
        print("\n%s: %d 个 key (%d 个带占位符) -> %s"
              % (name, len(batch),
                 sum(1 for k in batch if PH_RE.search(gap[k])), path))

    mpath = os.path.join(OUT_DIR, "ja_manifest.json")
    with io.open(mpath, "w", encoding="utf-8") as f:
        f.write(json.dumps({"total": len(gap), "batches": manifest}, ensure_ascii=False, indent=2))
    print("\nmanifest: %s" % mpath)
    return 0


if __name__ == "__main__":
    sys.exit(main())
