#!/usr/bin/env python3
"""把 BottomNavTabs.kt 里 Set 操作改 PersistentList 操作 (因为 rememberSaveable + mutableStateListOf)"""
from pathlib import Path

p = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
text = p.read_text(encoding="utf-8")

# 1) 简单 setOf 赋值 (setOf("cat", "ind"))
text = text.replace(
    'topLevelOpen = setOf("cat", "ind")',
    'topLevelOpen.clear(); topLevelOpen.add("cat"); topLevelOpen.add("ind")'
)

# 2) emptySet
text = text.replace("topLevelOpen = emptySet()", "topLevelOpen.clear()")
text = text.replace("expandedCategories = emptySet()", "expandedCategories.clear()")
text = text.replace("expandedIndications = emptySet()", "expandedIndications.clear()")

# 3) 复杂 setOf (from list of names)
text = text.replace(
    'expandedCategories = byCategory.keys.map { it.name }.toSet()',
    'expandedCategories.clear(); expandedCategories.addAll(byCategory.keys.map { it.name })'
)
text = text.replace(
    'expandedIndications = byIndication.map { it.first.name }.toSet()',
    'expandedIndications.clear(); expandedIndications.addAll(byIndication.map { it.first.name })'
)

# 4) `var X = if (X.contains(y)) X - y else X + y` 改成 PersistentList 操作
# 注意这些是 if (X in Y) 格式, 包含 "X in Y" 判断
# topLevelOpen "cat" / "ind" toggle
text = text.replace(
    'topLevelOpen = if ("cat" in topLevelOpen) topLevelOpen - "cat" else topLevelOpen + "cat"',
    'if ("cat" in topLevelOpen) topLevelOpen.remove("cat") else topLevelOpen.add("cat")'
)
text = text.replace(
    'topLevelOpen = if ("ind" in topLevelOpen) topLevelOpen - "ind" else topLevelOpen + "ind"',
    'if ("ind" in topLevelOpen) topLevelOpen.remove("ind") else topLevelOpen.add("ind")'
)

# 5) expandedCategories / expandedIndications 内的 if-toggle
# Pattern: expandedX = if (X in expandedY) { expandedY - X } else { expandedY + X }
# 注意这是块表达式, 旧版可能跨多行
old1 = '''expandedCategories = if (category.name in expandedCategories) {
                                    expandedCategories - category.name
                                } else {
                                    expandedCategories + category.name
                                }'''
new1 = '''if (category.name in expandedCategories) {
                                    expandedCategories.remove(category.name)
                                } else {
                                    expandedCategories.add(category.name)
                                }'''
text = text.replace(old1, new1)

old2 = '''expandedIndications = if (indication.name in expandedIndications) {
                                    expandedIndications - indication.name
                                } else {
                                    expandedIndications + indication.name
                                }'''
new2 = '''if (indication.name in expandedIndications) {
                                    expandedIndications.remove(indication.name)
                                } else {
                                    expandedIndications.add(indication.name)
                                }'''
text = text.replace(old2, new2)

p.write_text(text, encoding="utf-8")
print("OK: BottomNavTabs.kt PersistentList 操作改完")
