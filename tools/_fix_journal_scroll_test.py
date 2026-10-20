# -*- coding: utf-8 -*-
"""修 JournalScreenTest 的"回到顶部"用例: 区块标题由 HrtSectionHeader 渲染, 会统一转大写。

`journal_milestones_section` = "Timeline", 而 HrtSectionHeader 里是 `text.uppercase()`,
所以语义树里是 "TIMELINE" —— 断言拿原文 "Timeline" 去找, 永远找不到, 只能等超时。
MilestonesScreenTest 里早就有 `.uppercase(locale)` 的写法, 这里对齐。
"""
import io
import os

PATH = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\src\androidTest\java\com\dosecare\app\ui\journal\JournalScreenTest.kt"

with io.open(PATH, "r", encoding="utf-8", newline="") as f:
    text = f.read()
nl = "\r\n" if "\r\n" in text else "\n"

OLD = (
    "        val milestonesHeader = context.getString(R.string.journal_milestones_section)" + nl
)
assert text.count(OLD) == 1, "没找到 milestonesHeader 那一行"
NEW = (
    "        // 区块标题由 HrtSectionHeader 渲染, 会统一转成大写" + nl +
    "        val milestonesHeader = context.getString(R.string.journal_milestones_section)" + nl +
    "            .uppercase(Locale.ROOT)" + nl
)
text = text.replace(OLD, NEW, 1)

# Locale 的 import
if "import java.util.Locale" not in text:
    anchor = "import java.util.UUID" + nl
    if anchor in text:
        text = text.replace(anchor, "import java.util.Locale" + nl + anchor, 1)
    else:
        m = __import__("re").search(r"^import .+$", text, __import__("re").M)
        text = text[:m.start()] + "import java.util.Locale" + nl + text[m.start():]

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("patched JournalScreenTest.kt (newline=%r)" % nl)
