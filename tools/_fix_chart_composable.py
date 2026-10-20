# -*- coding: utf-8 -*-
"""把 InteractiveConcentrationChart 的药名计算移出 Canvas 绘制作用域。

Canvas 的 draw lambda / drawIntoCanvas 都不是 @Composable 上下文,
不能在里面调用 s.drug.displayName()。
修法: 在 Composable 作用域预先算好短名数组, 绘制时只取用。
"""
import io
import sys

PATH = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\src\main\java\com\dosecare\app\ui\tdm\InteractiveConcentrationChart.kt"

with io.open(PATH, "r", encoding="utf-8", newline="") as f:
    text = f.read()

# 自动探测行尾
if "\r\n" in text:
    nl = "\r\n"
else:
    nl = "\n"

anchor = (
    "        maxOf(cmaxD * 1.6, winHigh * 1.15, cminD * 2.0, 1.0)" + nl +
    "    }" + nl
)
assert text.count(anchor) == 1, "anchor count=%d" % text.count(anchor)

insert = anchor + (
    nl +
    "    // 药名短名: 必须在 Composable 作用域预先算好" + nl +
    "    // (Canvas 的 draw lambda / drawIntoCanvas 不是 Composable 上下文, 里面不能调 displayName())" + nl +
    "    val seriesShortNames = series.map { s ->" + nl +
    "        val n = s.drug.displayName()" + nl +
    "        if (n.length > 4) n.substring(0, 4) + \"\u2026\" else n" + nl +
    "    }" + nl
)
text = text.replace(anchor, insert, 1)

old_draw = (
    "                                val name = s.drug.displayName()" + nl +
    "                                val shortName = if (name.length > 4) name.substring(0, 4) + \"\u2026\" else name" + nl
)
assert text.count(old_draw) == 1, "old_draw count=%d" % text.count(old_draw)
new_draw = (
    "                                val shortName = seriesShortNames[idx]" + nl
)
text = text.replace(old_draw, new_draw, 1)

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(text)

print("OK, newline=%r" % nl)
