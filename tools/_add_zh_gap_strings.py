#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
补齐中文三语言相对英文缺失的 23 个 key。

背景:
    底座 PicoHRT 的 zh-Hans / zh-Hant / yue-Hant-HK 三个目录都缺这些 key
    (HRT 目标、图表副标题、睾酮药名等), 属于底座自带的老债, 本次合并原样继承。
    缺 key 时 Android 会回落到英文, 中文用户会看到大片英文。

做法:
    1. 用 ElementTree 复核每个 locale 真实缺哪些 key (排除 translatable=false)
    2. 写入译文, 保持原文件的行尾风格与 BOM 状态
    3. 回读 + XML 合法性 + 占位符一致性校验

用法:  python _add_zh_gap_strings.py [--apply]
"""
import io
import os
import re
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
PH_RE = re.compile(r'%\d+\$[\d.]*[a-zA-Z]')

# ---------------------------------------------------------------------------
# 译文表: key -> (简中, 繁中, 港繁)
#
# 繁中/港繁 的药名 = 简中药名的繁简转换 (与文件里既有的 螺內酯/醋酸環丙孕酮/
# 比卡魯胺 保持同一策略); 两者的差别只在句子的语体 —— 港繁是真粤语书面化
# (唔/喺/咗/嘅/呢個/試吓), 繁中用标准书面繁体。
# ---------------------------------------------------------------------------
TR = {
    "main_chart_hormone_e2": ("E2", "E2", "E2"),
    "main_chart_hormone_t": ("T", "T", "T"),
    "main_chart_hormone_cpa": ("CPA", "CPA", "CPA"),

    "main_t_chart_subtitle": (
        "睾酮 · 实测趋势",
        "睪酮 · 實測趨勢",
        "睪酮 · 實測趨勢",
    ),
    "main_cpa_chart_subtitle": (
        "醋酸环丙孕酮 · 实测趋势",
        "醋酸環丙孕酮 · 實測趨勢",
        "醋酸環丙孕酮 · 實測趨勢",
    ),

    "medication_name_testosterone_enanthate": ("睾酮庚酸酯", "睪酮庚酸酯", "睪酮庚酸酯"),
    "medication_name_testosterone_cypionate": ("睾酮环戊丙酸酯", "睪酮環戊丙酸酯", "睪酮環戊丙酸酯"),
    "medication_name_testosterone_undecanoate": ("睾酮十一酸酯（Nebido）", "睪酮十一酸酯（Nebido）", "睪酮十一酸酯（Nebido）"),
    "medication_name_testosterone_gel": ("睾酮凝胶", "睪酮凝膠", "睪酮凝膠"),

    "settings_hrt_goal": ("HRT 目标", "HRT 目標", "HRT 目標"),
    "settings_hrt_goal_summary": (
        "决定首页图表默认显示的激素，并控制临床安全提示。随时可以更改。",
        "決定首頁圖表預設顯示的荷爾蒙，並控制臨床安全提示。隨時可以更動。",
        "決定主頁圖表預設顯示嘅荷爾蒙，亦會控制臨床安全提示。隨時可以更改。",
    ),
    "hrt_goal_unset": ("尚未设置", "尚未設定", "仲未設定"),
    "hrt_goal_mtf": ("MtF（跨女性）", "MtF（跨女性）", "MtF（跨女性）"),
    "hrt_goal_ftm": ("FtM（跨男性）", "FtM（跨男性）", "FtM（跨男性）"),
    "hrt_goal_non_binary": ("非二元 / 自定义", "非二元 / 自訂", "非二元 / 自訂"),

    "safety_hrt_goal_mismatch_cpa_ftm_title": (
        "CPA 是跨女性使用的抗雄激素",
        "CPA 是跨女性使用的抗雄激素",
        "CPA 係跨女性用嘅抗雄激素",
    ),
    "safety_hrt_goal_mismatch_cpa_ftm_body": (
        "醋酸环丙孕酮用于跨女性方案中，用来阻断雄激素的作用。"
        "如果你使用 FtM 方案，而医生明确为你开了 CPA，请先确认剂量和原因再记录。",
        "醋酸環丙孕酮用於跨女性方案中，用來阻斷雄激素的作用。"
        "如果你使用 FtM 方案，而醫師明確為你開了 CPA，請先確認劑量與原因再記錄。",
        "醋酸環丙孕酮係用喺跨女性方案，作用係阻斷雄激素嘅影響。"
        "如果你行緊 FtM 方案，而且係醫生特登開咗 CPA 俾你，記錄之前請先確認劑量同原因。",
    ),
    "safety_hrt_goal_mismatch_e2_ftm_title": (
        "雌二醇是跨女性使用的激素",
        "雌二醇是跨女性使用的荷爾蒙",
        "雌二醇係跨女性用嘅荷爾蒙",
    ),
    "safety_hrt_goal_mismatch_e2_ftm_body": (
        "雌二醇是跨女性方案的主要激素。"
        "如果你使用 FtM 方案，而医生为你开了低剂量 E2，请先确认给药途径和剂量再记录。",
        "雌二醇是跨女性方案的主要荷爾蒙。"
        "如果你使用 FtM 方案，而醫師為你開了低劑量 E2，請先確認給藥途徑與劑量再記錄。",
        "雌二醇係跨女性方案嘅主要荷爾蒙。"
        "如果你行緊 FtM 方案，而且醫生俾咗低劑量 E2，記錄之前請先確認用藥途徑同劑量。",
    ),
    "safety_hrt_goal_mismatch_t_mtf_title": (
        "睾酮是跨男性使用的激素",
        "睪酮是跨男性使用的荷爾蒙",
        "睪酮係跨男性用嘅荷爾蒙",
    ),
    "safety_hrt_goal_mismatch_t_mtf_body": (
        "睾酮是跨男性方案的主要激素。"
        "如果你使用 MtF 方案，而医生为你开了低剂量 T，请先确认给药途径和剂量再记录。",
        "睪酮是跨男性方案的主要荷爾蒙。"
        "如果你使用 MtF 方案，而醫師為你開了低劑量 T，請先確認給藥途徑與劑量再記錄。",
        "睪酮係跨男性方案嘅主要荷爾蒙。"
        "如果你行緊 MtF 方案，而且醫生俾咗低劑量 T，記錄之前請先確認用藥途徑同劑量。",
    ),

    "main_t_chart_3comp_badge": ("3 室模型", "3 室模型", "3 室模型"),
    "main_t_chart_3comp_badge_cd": (
        "三室模型：深部外周脂肪蓄积，90 天终末半衰期",
        "三室模型：深部周邊脂肪儲存，90 天終末半衰期",
        "三室模型：深部周邊脂肪儲存，90 天終末半衰期",
    ),
}

LOCALES = [
    ("values-b+zh+Hans", 0),
    ("values-b+zh+Hant", 1),
    ("values-b+yue+Hant+HK", 2),
]

BARE = re.compile(r'&(?!amp;|lt;|gt;|quot;|apos;)|<(?![a-zA-Z/!?])|(?<![a-zA-Z])>(?![a-zA-Z])')


def read_bytes(path):
    with open(path, "rb") as f:
        return f.read()


def parse_keys(raw_bytes):
    text = raw_bytes.decode("utf-8-sig")
    tree = ET.fromstring(text)
    d = {}
    for el in tree.iter():
        if el.tag == "string" and el.get("name"):
            d.setdefault(el.get("name"), (el.text or "", el.get("translatable")))
    return d, text


def detect_eol(text):
    crlf = text.count("\r\n")
    lf = text.count("\n") - crlf
    return "\r\n" if crlf >= lf else "\n"


def main():
    apply = "--apply" in sys.argv
    en_raw = read_bytes(os.path.join(RES, "values", "strings.xml"))
    en, _ = parse_keys(en_raw)

    plan = {}
    errors = []
    for loc, idx in LOCALES:
        path = os.path.join(RES, loc, "strings.xml")
        raw = read_bytes(path)
        d, text = parse_keys(raw)
        todo = []
        for k, (e_text, trans) in en.items():
            if k in d:
                continue
            if trans == "false":
                continue          # translatable=false, 本就不该翻译
            if k not in TR:
                errors.append("%s: 缺译文 (locale=%s)" % (k, loc))
                continue
            v = TR[k][idx]
            if not v.strip():
                errors.append("%s: 译文为空" % k)
                continue
            if sorted(PH_RE.findall(e_text)) != sorted(PH_RE.findall(v)):
                errors.append("%s: 占位符不一致 en=%s zh=%s"
                              % (k, PH_RE.findall(e_text), PH_RE.findall(v)))
            if BARE.search(v):
                errors.append("%s: 未转义字符 %r" % (k, v[:60]))
            todo.append((k, v))
        plan[loc] = (path, raw, text, todo)
        print("%-22s 需补 %d 条" % (loc, len(todo)))

    if errors:
        print("\n[ERROR] %d 个问题:" % len(errors))
        for e in errors:
            print("  - " + e)
        return 1

    if not apply:
        print("\n[dry-run] 加 --apply 写入")
        return 0

    for loc, (path, raw, text, todo) in plan.items():
        has_bom = raw[:3] == b"\xef\xbb\xbf"
        eol = detect_eol(text)
        close = text.rfind("</resources>")
        if close < 0:
            print("[ERROR] %s 找不到 </resources>" % loc)
            return 1
        block = [u'    <string name="%s">%s</string>' % (k, v) for k, v in sorted(todo)]
        new_text = text[:close] + eol.join(block) + eol + text[close:]

        out = new_text.encode("utf-8")
        if has_bom:
            out = b"\xef\xbb\xbf" + out
        with open(path, "wb") as f:
            f.write(out)

        # 回读校验
        back_raw = read_bytes(path)
        back, back_text = parse_keys(back_raw)
        bad = [k for k, v in todo if back.get(k, ("", None))[0] != v]
        if bad:
            print("[ERROR] %s 回读不一致: %s" % (loc, bad))
            return 1
        try:
            ET.fromstring(back_text)
        except Exception as e:
            print("[ERROR] %s XML 不合法: %s" % (loc, e))
            return 1
        print("[OK] %-22s 写入 %d 条, 现有 %d 个 key, BOM=%s, 行尾=%s"
              % (loc, len(todo), len(back), has_bom, "CRLF" if eol == "\r\n" else "LF"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
