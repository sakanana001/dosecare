# -*- coding: utf-8 -*-
"""给 CHANGELOG.md / docs/CHANGELOG.zh-Hans.md 的"未发布"小节补上本轮的用户可见变更。

按 docs/release-process.md 的约定: 用户可见的变更积累在未发布小节下,
发版时再把未发布改名为具体版本。
"""
import io
import os

REPO = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged"

EN = (
    "### Pharmacology catalog / Japanese localisation\n"
    "\n"
    "#### Added\n"
    "\n"
    "- The 202-drug pharmacology catalog now carries a Japanese generic name for every "
    "medication, so the Japanese UI shows and searches drugs by their Japanese INN "
    "(\u30af\u30ed\u30b6\u30d1\u30f3, \u30ec\u30dc\u30c1\u30ed\u30ad\u30b7\u30f3, "
    "\u30ef\u30eb\u30d5\u30a1\u30ea\u30f3, ...) instead of the Chinese name. "
    "The names come from the Japanese INN list and were cross-checked against KEGG "
    "MEDICUS.\n"
    "- Drug names now follow the UI language everywhere they appear: the drug picker, "
    "the interaction checker, the plasma-level (TDM) screen, the two-drug comparison and "
    "the pharmacology dossier. The picker also sorts by the name of the current UI "
    "language.\n"
    "\n"
    "#### Fixed\n"
    "\n"
    "- The Traditional Chinese and Hong Kong Cantonese UIs no longer fall back to English for "
    "drug categories, interaction results, metabolic pathways, dose-assist chips and "
    "severity labels - 91 strings that were still English while the Simplified Chinese UI "
    "had them translated.\n"
)

ZH = (
    "### \u836f\u7406\u76ee\u5f55 \u00b7 \u65e5\u8bed\u672c\u5730\u5316\n"
    "\n"
    "#### \u65b0\u589e\n"
    "\n"
    "- 202 \u836f\u7684\u836f\u7406\u76ee\u5f55\u6bcf\u4e2a\u836f\u90fd\u8865\u4e0a\u4e86\u65e5\u6587\u901a\u7528\u540d, "
    "\u65e5\u8bed\u754c\u9762\u6309\u65e5\u672c\u533b\u836f\u54c1\u4e00\u822c\u540d\u663e\u793a\u548c\u641c\u7d22"
    "(\u30af\u30ed\u30b6\u30d1\u30f3 / \u30ec\u30dc\u30c1\u30ed\u30ad\u30b7\u30f3 / "
    "\u30ef\u30eb\u30d5\u30a1\u30ea\u30f3 ...), \u4e0d\u518d\u663e\u793a\u4e2d\u6587\u540d; "
    "\u65e5\u6587\u540d\u53d6\u81ea\u65e5\u672c\u533b\u836f\u54c1\u4e00\u822c\u540d, "
    "\u5e76\u4e0e KEGG MEDICUS \u4ea4\u53c9\u6838\u5bf9\u3002\n"
    "- \u836f\u540d\u663e\u793a\u7edf\u4e00\u8ddf\u968f\u754c\u9762\u8bed\u8a00: \u9009\u836f\u5668\u3001"
    "\u836f\u7269\u76f8\u4e92\u4f5c\u7528\u3001\u8840\u836f\u6d53\u5ea6(TDM)\u3001\u53cc\u836f\u89c4\u683c\u5bf9\u6bd4\u3001"
    "\u836f\u7406\u6863\u6848; \u9009\u836f\u5217\u8868\u4e5f\u6539\u6210\u6309\u5f53\u524d\u754c\u9762\u8bed\u8a00\u7684\u4e3b\u540d\u6392\u5e8f\u3002\n"
    "\n"
    "#### \u6539\u53d8\n"
    "\n"
    "- \u7e41\u4e2d / \u6e2f\u7e41\u754c\u9762\u4e0d\u518d\u6709\u4e00\u6279\u6587\u6848\u56de\u843d\u5230\u82f1\u6587: "
    "\u836f\u7406\u5206\u7c7b\u3001\u76f8\u4e92\u4f5c\u7528\u7ed3\u679c\u3001\u4ee3\u8c22\u9014\u5f84\u3001"
    "\u5242\u91cf\u8f85\u52a9\u9009\u9879\u3001\u4e25\u91cd\u5ea6\u7b49 91 \u6761 \u2014\u2014 "
    "\u7b80\u4e2d\u65e9\u5df2\u7ffb\u8bd1, \u7e41\u4e2d/\u6e2f\u7e41\u8fd8\u505c\u5728\u82f1\u6587\u3002\n"
)


def patch(path, heading_unreleased, body, next_heading):
    with io.open(path, "r", encoding="utf-8", newline="") as f:
        text = f.read()
    nl = "\r\n" if "\r\n" in text else "\n"

    def n(s):
        return s.replace("\n", nl)

    anchor = n(heading_unreleased) + nl + nl
    assert text.count(anchor) == 1, "%s: 没找到未发布小节" % path
    start = text.index(anchor)
    end = text.find(n(next_heading), start + len(anchor))
    assert end != -1, "%s: 没找到下一个版本小节" % path

    section = text[start:end]
    section = section + n(body) + nl
    text = text[:start] + section + text[end:]

    with io.open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    print("updated %s (newline=%r)" % (os.path.basename(path), nl))


patch(
    os.path.join(REPO, "CHANGELOG.md"),
    "## [Unreleased]",
    EN,
    "## [2.0.0]",
)
patch(
    os.path.join(REPO, "docs", "CHANGELOG.zh-Hans.md"),
    "## [\u672a\u53d1\u5e03]",
    ZH,
    "## [2.0.0]",
)
