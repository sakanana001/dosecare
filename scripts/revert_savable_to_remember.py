#!/usr/bin/env python3
"""回退 mutableStateListOf + rememberSaveable 改动 -> mutableStateOf<Set<String>> + remember

原因: mutableStateListOf + rememberSaveable 在某些 K2 编译路径下可能引起 IllegalStateException
      闪退优先级 > 状态保留, 先保证不崩
"""
from pathlib import Path

# === BottomNavTabs.kt: CatalogTab 头部 3 个 set + byIndication 操作恢复 ===
p1 = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t = p1.read_text(encoding="utf-8")

# 1) 声明: mutableStateListOf -> mutableStateOf<Set<String>> + var by
old1 = '''    // 顶层 2 大类 + 子分类 状态 (全部默认折叠, 跨 navigation 用 rememberSaveable 保留)
    // mutableStateListOf + rememberSaveable 配内置 saver, K2 编译稳
    val topLevelOpen = rememberSaveable { mutableStateListOf<String>() }
    val expandedCategories = rememberSaveable { mutableStateListOf<String>() }
    val expandedIndications = rememberSaveable { mutableStateListOf<String>() }'''
new1 = '''    // 顶层 2 大类 + 子分类 状态 (默认全部折叠; remember 不用 rememberSaveable 避免 K2 + mutableStateListOf 闪退)
    var topLevelOpen by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedIndications by remember { mutableStateOf<Set<String>>(emptySet()) }'''
t = t.replace(old1, new1)

# 2) 全部展开按钮: .clear()+.add() -> = setOf
t = t.replace(
    'topLevelOpen.clear(); topLevelOpen.add("cat"); topLevelOpen.add("ind")',
    'topLevelOpen = setOf("cat", "ind")'
)
t = t.replace(
    'topLevelOpen.clear()',
    'topLevelOpen = emptySet()'
)
t = t.replace(
    'expandedCategories.clear(); expandedCategories.addAll(byCategory.keys.map { it.name })',
    'expandedCategories = byCategory.keys.map { it.name }.toSet()'
)
t = t.replace(
    'expandedIndications.clear(); expandedIndications.addAll(byIndication.map { it.first.name })',
    'expandedIndications = byIndication.map { it.first.name }.toSet()'
)
t = t.replace(
    'expandedCategories.clear()',
    'expandedCategories = emptySet()'
)
t = t.replace(
    'expandedIndications.clear()',
    'expandedIndications = emptySet()'
)

# 3) toggle: if (... in list).remove/.add -> if (... in set) set - + set
t = t.replace(
    'if ("cat" in topLevelOpen) topLevelOpen.remove("cat") else topLevelOpen.add("cat")',
    'topLevelOpen = if ("cat" in topLevelOpen) topLevelOpen - "cat" else topLevelOpen + "cat"'
)
t = t.replace(
    'if ("ind" in topLevelOpen) topLevelOpen.remove("ind") else topLevelOpen.add("ind")',
    'topLevelOpen = if ("ind" in topLevelOpen) topLevelOpen - "ind" else topLevelOpen + "ind"'
)
t = t.replace(
    'if (category.name in expandedCategories) {\n                                    expandedCategories.remove(category.name)\n                                } else {\n                                    expandedCategories.add(category.name)\n                                }',
    'expandedCategories = if (category.name in expandedCategories) expandedCategories - category.name else expandedCategories + category.name'
)
t = t.replace(
    'if (indication.name in expandedIndications) {\n                                    expandedIndications.remove(indication.name)\n                                } else {\n                                    expandedIndications.add(indication.name)\n                                }',
    'expandedIndications = if (indication.name in expandedIndications) expandedIndications - indication.name else expandedIndications + indication.name'
)

# 4) CompareTab/InteractionsTab 之前用 rememberSaveable, 看是否真的崩
# 实际上 CompareTab 用 `rememberSaveable { mutableStateOf<String?>(null) }` (单值, not List), 应该 work
# 留着不动 — 但保险起见回退到 remember
# drugAId
old2 = '    var drugAId by rememberSaveable { mutableStateOf<String?>(null) }\n    var drugBId by rememberSaveable { mutableStateOf<String?>(null) }\n    var infoDialogOpen by rememberSaveable { mutableStateOf(false) }'
new2 = '    var drugAId by remember { mutableStateOf<String?>(null) }\n    var drugBId by remember { mutableStateOf<String?>(null) }\n    var infoDialogOpen by remember { mutableStateOf(false) }'
if old2 in t:
    t = t.replace(old2, new2)
    print("OK: CompareTab drugAId/drugBId/infoDialogOpen 回退到 remember")

# InteractionsTab
old3 = '    // 跨 navigation 保留 (评估后跳详情, 返回已选药 / 已评估状态)\n    var selectedIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }\n    var results by remember { mutableStateOf<List<Interaction>>(emptyList()) }  // Interaction 不易存, 用 remember\n    var hasEvaluated by rememberSaveable { mutableStateOf(false) }\n    var infoDialogOpen by rememberSaveable { mutableStateOf(false) }'
new3 = '    var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }\n    var results by remember { mutableStateOf<List<Interaction>>(emptyList()) }\n    var hasEvaluated by remember { mutableStateOf(false) }\n    var infoDialogOpen by remember { mutableStateOf(false) }'
if old3 in t:
    t = t.replace(old3, new3)
    print("OK: InteractionsTab selectedIds/hasEvaluated/infoDialogOpen 回退到 remember")

p1.write_text(t, encoding="utf-8")
print("OK: BottomNavTabs.kt 全部回退")

# === PrescriptionTab.kt: expandedGroups 回退 ===
p2 = Path("app/src/main/java/com/dosecare/app/ui/PrescriptionTab.kt")
t2 = p2.read_text(encoding="utf-8")

old4 = '    // 跨 navigation 保留 (返回 DrugDetailScreen 不丢折叠状态)\n    val expandedGroups = rememberSaveable { mutableStateListOf<String>() }'
new4 = '    // 注意: 之前用 rememberSaveable + mutableStateListOf 在某些 K2 路径下闪退, 改回普通 remember\n    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }'
if old4 in t2:
    t2 = t2.replace(old4, new4)
    # toggle 改回
    t2 = t2.replace(
        '                        onToggleExpand = {\n                            if (group.id in expandedGroups) expandedGroups.remove(group.id)\n                            else expandedGroups.add(group.id)\n                        },',
        '                        onToggleExpand = {\n                            expandedGroups = if (group.id in expandedGroups) expandedGroups - group.id else expandedGroups + group.id\n                        },'
    )
    t2 = t2.replace(
        '    var infoDialogOpen by rememberSaveable { mutableStateOf(false) }',
        '    var infoDialogOpen by remember { mutableStateOf(false) }'
    )
    # 移除不再需要的 rememberSaveable import
    t2 = t2.replace(
        'import androidx.compose.runtime.saveable.rememberSaveable\n',
        ''
    )
    p2.write_text(t2, encoding="utf-8")
    print("OK: PrescriptionTab.kt expandedGroups 回退")
else:
    print("WARN: PrescriptionTab expandedGroups 未找到, 跳过")

# BottomNavTabs: 移除 rememberSaveable / Saver imports (不再用)
p1 = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t1 = p1.read_text(encoding="utf-8")
# 移除 saver 字段
t1 = t1.replace('''/** Set<String> 跨 navigation 保存的 Saver (CatalogTab / PrescriptionTab 等需要) */
private val SetStringSaver: Saver<Set<String>, ArrayList<String>> = Saver(
    save = { ArrayList(it) },
    restore = { it.toSet() }
)

''', '')
t1 = t1.replace(
    'import androidx.compose.runtime.saveable.rememberSaveable\n',
    ''
)
t1 = t1.replace(
    'import androidx.compose.runtime.saveable.Saver\n',
    ''
)
p1.write_text(t1, encoding="utf-8")
print("OK: BottomNavTabs.kt 清理 import")
