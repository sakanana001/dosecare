#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补剂量调整 (肾/肝) 与监测频次的本地化资源, 5 个语言。

背景: 202 药目录里这批值在 DoseCare 原版就没有标签, 界面上直接显示枚举名
("CONTRAINDICATED IN SEVERE" / "MONITOR" / "EVERY_3_MONTHS"), 中文和日语
界面里都是一串大写英文。

对应:
  renal_*  (4)  RenalAdjustment
  hepatic_*(5)  HepaticAdjustment
  monfreq_*(19) monitoring.frequency (自由字符串, 按原值查表)

这些 key 由 model/pharmacology/PharmaLabels.kt 的 labelRes() /
monitoringFrequencyRes() 引用。

默认 dry-run, 加 --apply 才写盘。保持各文件自身行尾风格。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

DIRS = ["values", "values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]

EN = {
    "renal_none": "No adjustment",
    "renal_monitor": "Monitor",
    "renal_reduce_egfr30": "Reduce 50% if eGFR < 30",
    "renal_contraindicated_egfr30": "Contraindicated if eGFR < 30",
    "hepatic_none": "No adjustment",
    "hepatic_monitor": "Monitor",
    "hepatic_reduce_pugh_b": "Reduce 50% in Child-Pugh B",
    "hepatic_reduce_pugh_c": "Reduce 50% in Child-Pugh C",
    "hepatic_contraindicated_severe": "Contraindicated in severe impairment",
    "monfreq_monthly": "Monthly",
    "monfreq_every_3_months": "Every 3 months",
    "monfreq_as_needed": "As needed",
    "monfreq_every_6_months": "Every 6 months",
    "monfreq_every_3_6_months": "Every 3-6 months",
    "monfreq_monthly_first_3_then_every_3": "Monthly for the first 3 months, then every 3 months",
    "monfreq_monthly_initially": "Monthly initially",
    "monfreq_per_procedure": "Per procedure",
    "monfreq_every_6_12_months": "Every 6-12 months",
    "monfreq_weekly_18_then_monthly": "Weekly for the first 18 weeks, then monthly",
    "monfreq_weekly_then_monthly": "Weekly initially, then monthly",
    "monfreq_weekly_initially": "Weekly initially",
    "monfreq_per_prescription": "Per prescription",
    "monfreq_during_iv_infusion": "During IV infusion",
    "monfreq_before_each_infusion": "Before each infusion",
    "monfreq_weekly": "Weekly",
    "monfreq_before_each_dose": "Before each dose",
    "monfreq_every_6_8_weeks": "Every 6-8 weeks",
    "monfreq_continuous": "Continuous",
}

ZH_HANS = {
    "renal_none": "无需调整",
    "renal_monitor": "监测",
    "renal_reduce_egfr30": "eGFR < 30 时减量 50%",
    "renal_contraindicated_egfr30": "eGFR < 30 时禁用",
    "hepatic_none": "无需调整",
    "hepatic_monitor": "监测",
    "hepatic_reduce_pugh_b": "Child-Pugh B 时减量 50%",
    "hepatic_reduce_pugh_c": "Child-Pugh C 时减量 50%",
    "hepatic_contraindicated_severe": "重度肝损时禁用",
    "monfreq_monthly": "每月",
    "monfreq_every_3_months": "每 3 个月",
    "monfreq_as_needed": "按需",
    "monfreq_every_6_months": "每 6 个月",
    "monfreq_every_3_6_months": "每 3-6 个月",
    "monfreq_monthly_first_3_then_every_3": "前 3 个月每月,之后每 3 个月",
    "monfreq_monthly_initially": "起始每月",
    "monfreq_per_procedure": "每次操作",
    "monfreq_every_6_12_months": "每 6-12 个月",
    "monfreq_weekly_18_then_monthly": "前 18 周每周,之后每月",
    "monfreq_weekly_then_monthly": "起始每周,之后每月",
    "monfreq_weekly_initially": "起始每周",
    "monfreq_per_prescription": "按处方",
    "monfreq_during_iv_infusion": "静脉滴注期间",
    "monfreq_before_each_infusion": "每次滴注前",
    "monfreq_weekly": "每周",
    "monfreq_before_each_dose": "每次给药前",
    "monfreq_every_6_8_weeks": "每 6-8 周",
    "monfreq_continuous": "持续监测",
}

ZH_HANT = {
    "renal_none": "無需調整",
    "renal_monitor": "監測",
    "renal_reduce_egfr30": "eGFR < 30 時減量 50%",
    "renal_contraindicated_egfr30": "eGFR < 30 時禁用",
    "hepatic_none": "無需調整",
    "hepatic_monitor": "監測",
    "hepatic_reduce_pugh_b": "Child-Pugh B 時減量 50%",
    "hepatic_reduce_pugh_c": "Child-Pugh C 時減量 50%",
    "hepatic_contraindicated_severe": "重度肝損時禁用",
    "monfreq_monthly": "每月",
    "monfreq_every_3_months": "每 3 個月",
    "monfreq_as_needed": "按需",
    "monfreq_every_6_months": "每 6 個月",
    "monfreq_every_3_6_months": "每 3-6 個月",
    "monfreq_monthly_first_3_then_every_3": "前 3 個月每月,之後每 3 個月",
    "monfreq_monthly_initially": "起始每月",
    "monfreq_per_procedure": "每次操作",
    "monfreq_every_6_12_months": "每 6-12 個月",
    "monfreq_weekly_18_then_monthly": "前 18 週每週,之後每月",
    "monfreq_weekly_then_monthly": "起始每週,之後每月",
    "monfreq_weekly_initially": "起始每週",
    "monfreq_per_prescription": "按處方",
    "monfreq_during_iv_infusion": "靜脈滴注期間",
    "monfreq_before_each_infusion": "每次滴注前",
    "monfreq_weekly": "每週",
    "monfreq_before_each_dose": "每次給藥前",
    "monfreq_every_6_8_weeks": "每 6-8 週",
    "monfreq_continuous": "持續監測",
}

YUE = {
    "renal_none": "唔使調整",
    "renal_monitor": "監測",
    "renal_reduce_egfr30": "eGFR < 30 就要減量 50%",
    "renal_contraindicated_egfr30": "eGFR < 30 禁用",
    "hepatic_none": "唔使調整",
    "hepatic_monitor": "監測",
    "hepatic_reduce_pugh_b": "Child-Pugh B 要減量 50%",
    "hepatic_reduce_pugh_c": "Child-Pugh C 要減量 50%",
    "hepatic_contraindicated_severe": "重度肝損禁用",
    "monfreq_monthly": "每月",
    "monfreq_every_3_months": "每 3 個月",
    "monfreq_as_needed": "按需",
    "monfreq_every_6_months": "每 6 個月",
    "monfreq_every_3_6_months": "每 3-6 個月",
    "monfreq_monthly_first_3_then_every_3": "頭 3 個月每月,之後每 3 個月",
    "monfreq_monthly_initially": "起初每月",
    "monfreq_per_procedure": "每次做之前",
    "monfreq_every_6_12_months": "每 6-12 個月",
    "monfreq_weekly_18_then_monthly": "頭 18 個星期每星期,之後每月",
    "monfreq_weekly_then_monthly": "起初每星期,之後每月",
    "monfreq_weekly_initially": "起初每星期",
    "monfreq_per_prescription": "按處方",
    "monfreq_during_iv_infusion": "靜脈滴注期間",
    "monfreq_before_each_infusion": "每次滴注之前",
    "monfreq_weekly": "每星期",
    "monfreq_before_each_dose": "每次食藥之前",
    "monfreq_every_6_8_weeks": "每 6-8 個星期",
    "monfreq_continuous": "持續監測",
}

JA = {
    "renal_none": "調整不要",
    "renal_monitor": "モニタリング",
    "renal_reduce_egfr30": "eGFR < 30 で 50% 減量",
    "renal_contraindicated_egfr30": "eGFR < 30 で禁忌",
    "hepatic_none": "調整不要",
    "hepatic_monitor": "モニタリング",
    "hepatic_reduce_pugh_b": "Child-Pugh B で 50% 減量",
    "hepatic_reduce_pugh_c": "Child-Pugh C で 50% 減量",
    "hepatic_contraindicated_severe": "重症肝障害では禁忌",
    "monfreq_monthly": "毎月",
    "monfreq_every_3_months": "3 か月ごと",
    "monfreq_as_needed": "必要時",
    "monfreq_every_6_months": "6 か月ごと",
    "monfreq_every_3_6_months": "3〜6 か月ごと",
    "monfreq_monthly_first_3_then_every_3": "最初の 3 か月は毎月、その後は 3 か月ごと",
    "monfreq_monthly_initially": "当初は毎月",
    "monfreq_per_procedure": "手技ごと",
    "monfreq_every_6_12_months": "6〜12 か月ごと",
    "monfreq_weekly_18_then_monthly": "最初の 18 週間は毎週、その後は毎月",
    "monfreq_weekly_then_monthly": "当初は毎週、その後は毎月",
    "monfreq_weekly_initially": "当初は毎週",
    "monfreq_per_prescription": "処方ごと",
    "monfreq_during_iv_infusion": "点滴中",
    "monfreq_before_each_infusion": "各点滴前",
    "monfreq_weekly": "毎週",
    "monfreq_before_each_dose": "各投与前",
    "monfreq_every_6_8_weeks": "6〜8 週間ごと",
    "monfreq_continuous": "継続的",
}

TABLES = {
    "values": EN,
    "values-b+zh+Hans": ZH_HANS,
    "values-b+zh+Hant": ZH_HANT,
    "values-b+yue+Hant+HK": YUE,
    "values-ja": JA,
}

STR_RE = re.compile(r'(?s)<string name="([^"]+)"\s*>(.*?)</string>')


def read_text(p):
    with io.open(p, "r", encoding="utf-8", newline="") as f:
        return f.read()


def main():
    apply = "--apply" in sys.argv
    total_add = 0
    for d in DIRS:
        table = TABLES[d]
        path = os.path.join(RES, d, "strings.xml")
        text = read_text(path)
        eol = "\r\n" if "\r\n" in text else "\n"
        existing = {m.group(1) for m in STR_RE.finditer(text)}
        add = [k for k in table if k not in existing]
        print("[%-18s] 新增 %d / 共 %d" % (d, len(add), len(table)))
        total_add += len(add)
        if not apply or not add:
            continue
        close = text.rfind("</resources>")
        block = ['    <string name="%s">%s</string>' % (k, table[k]) for k in add]
        text = text[:close] + eol.join(block) + eol + text[close:]
        with io.open(path, "w", encoding="utf-8", newline="") as f:
            f.write(text)
        back = {m.group(1): m.group(2) for m in STR_RE.finditer(read_text(path))}
        bad = [k for k in table if back.get(k) != table[k]]
        if bad:
            print("[ERROR] %s 回读不一致: %s" % (d, bad))
            return 1
        print("  -> 写入完成,回读校验通过")

    print()
    print("合计新增 %d 个 (5 语言 x %d key)" % (total_add, len(EN)))
    if not apply:
        print("[dry-run] 加 --apply 写入")
    else:
        print("[OK] 全部写入并回读校验通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
