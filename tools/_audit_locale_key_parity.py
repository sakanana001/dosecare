#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
五语言 key 差异审计: 找出在某些语言下缺失、但在其他语言下存在的 key。

Android 资源 fallback 规则: 某个 key 在 values-b+zh+Hant 缺失, 运行时就会回落到
values (英文)。对"语言名""日文"这类内容来说回落是灾难(用户在自己的语言界面里
看到英文), 对普通功能文案则只是体验降级。

这个脚本把缺失分成三档:
  A. 合并引入的 key (cmp_/compare_/tdm_/chart_/risk_level_/cyp_strength_/
     renal_/hepatic_/monfreq_/ind_/sev_/pathway_/app_language_japanese)
     —— 缺一个就是本次合并的锅
  B. 底座自带的 key —— 与本次合并无关
  C. 只在日语目录缺的 key —— 日语还没翻完, 预期内
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

DIRS = ["values", "values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>')
PLURAL_RE = re.compile(r'(?s)<plurals name="([^"]+)"')
ARRAY_RE = re.compile(r'(?s)<string-array name="([^"]+)"')

# 本次合并引入的 key 前缀
MERGE_PREFIXES = (
    "cmp_", "compare_", "tdm_", "chart_", "risk_level_", "cyp_strength_",
    "renal_", "hepatic_", "monfreq_", "ind_", "sev_", "sevrule_", "pathway_",
    "app_language_", "interaction_", "dossier_", "search_select_",
)


def parse(path):
    with io.open(path, "r", encoding="utf-8") as f:
        t = f.read()
    keys = set(STR_RE.findall(t)) | set(PLURAL_RE.findall(t)) | set(ARRAY_RE.findall(t))
    return keys


def main():
    tables = {}
    for d in DIRS:
        p = os.path.join(RES, d, "strings.xml")
        tables[d] = parse(p) if os.path.exists(p) else set()

    base = tables["values"]
    print("各语言 key 总数:")
    for d in DIRS:
        print("  %-22s %d" % (d, len(tables[d])))
    print()

    merge_keys = set(k for k in base if k.startswith(MERGE_PREFIXES))
    print("合并引入的 key: %d 个" % len(merge_keys))
    print()

    bad = 0
    for d in DIRS[1:]:
        missing = base - tables[d]
        extra = tables[d] - base
        merge_missing = sorted(missing & merge_keys)
        other_missing = sorted(missing - merge_keys)
        if extra:
            print("[%s] 有 %d 个 base 里没有的 key" % (d, len(extra)))
        print("[%s] 相对 values 缺 %d 个 (其中合并引入的 %d 个)"
              % (d, len(missing), len(merge_missing)))
        if merge_missing:
            bad += len(merge_missing)
            print("   合并引入但缺失: %s" % ", ".join(merge_missing))
        if other_missing and d != "values-ja":
            print("   底座自带缺失(与本次合并无关) %d 个, 例: %s"
                  % (len(other_missing), ", ".join(other_missing[:6])))
        if other_missing and d == "values-ja":
            print("   日语未翻译 %d 个 (预期内, 底座 891 key 缺口)" % len(other_missing))
        print()

    if bad:
        print("[FAIL] 合并引入的 key 在某些语言下缺失: %d 处" % bad)
        return 1
    print("[PASS] 合并引入的 key 在全部 5 个语言下齐全")
    return 0


if __name__ == "__main__":
    sys.exit(main())
