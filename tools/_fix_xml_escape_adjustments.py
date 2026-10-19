#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修 28 个新加 key 里的 XML 转义问题。

背景: renal_reduce_egfr30 / renal_contraindicated_egfr30 的译文里有
"eGFR < 30", 裸的 "<" 在 Android 资源 XML 里是非法字符, aapt 会报
"元素内容必须由格式正确的字符数据或标记组成"。

修法: 只对这 28 个 key 的值做 XML 文本转义 (< -> &lt;, & -> &amp;),
不动其它 key, 保持行尾与 BOM。
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

KEYS = [
    "renal_none", "renal_monitor", "renal_reduce_egfr30", "renal_contraindicated_egfr30",
    "hepatic_none", "hepatic_monitor", "hepatic_reduce_pugh_b", "hepatic_reduce_pugh_c",
    "hepatic_contraindicated_severe",
    "monfreq_monthly", "monfreq_every_3_months", "monfreq_as_needed", "monfreq_every_6_months",
    "monfreq_every_3_6_months", "monfreq_monthly_first_3_then_every_3", "monfreq_monthly_initially",
    "monfreq_per_procedure", "monfreq_every_6_12_months", "monfreq_weekly_18_then_monthly",
    "monfreq_weekly_then_monthly", "monfreq_weekly_initially", "monfreq_per_prescription",
    "monfreq_during_iv_infusion", "monfreq_before_each_infusion", "monfreq_weekly",
    "monfreq_before_each_dose", "monfreq_every_6_8_weeks", "monfreq_continuous",
]

DIRS = ["values", "values-b+zh+Hans", "values-b+zh+Hant", "values-b+yue+Hant+HK", "values-ja"]


def escape(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main():
    apply = "--apply" in sys.argv
    import xml.etree.ElementTree as ET
    for d in DIRS:
        path = os.path.join(RES, d, "strings.xml")
        with io.open(path, "rb") as f:
            raw = f.read()
        bom = raw[:3] == b"\xef\xbb\xbf"
        text = raw.decode("utf-8-sig" if bom else "utf-8")

        n = 0
        for k in KEYS:
            pat = re.compile(r'(?s)(<string name="%s">)(.*?)(</string>)' % re.escape(k))
            m = pat.search(text)
            if not m:
                continue
            old = m.group(2)
            new = escape(old)
            if new != old:
                text = text[:m.start(2)] + new + text[m.end(2):]
                n += 1
        print("[%-18s] 转义 %d 个" % (d, n))
        if not apply or n == 0:
            continue
        out = text.encode("utf-8")
        if bom:
            out = b"\xef\xbb\xbf" + out
        with io.open(path, "wb") as f:
            f.write(out)
        # XML 合法性校验
        try:
            ET.fromstring(out.decode("utf-8-sig" if bom else "utf-8"))
            print("  -> 写入完成, XML 解析通过")
        except Exception as e:
            print("[ERROR] %s XML 仍不合法: %s" % (d, e))
            return 1

    if not apply:
        print("[dry-run] 加 --apply 写入")
    else:
        print("[OK] 全部转义完成并通过 XML 校验")
    return 0


if __name__ == "__main__":
    sys.exit(main())
