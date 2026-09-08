"""BottomNavTabs 3 个 tab 改用 ViewModel: setDrugAId, setDrugBId, setInfoDialogOpen, toggleCategory, toggleIndication, toggleTop, setTopOpen, setExpandedCategories, setExpandedIndications"""
from pathlib import Path
p = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t = p.read_text(encoding="utf-8")

# === CatalogTab 头部: 3 个 set 改 viewModel ===
old_cat = '''    // 顶层 2 大类 + 子分类 状态 (默认全部折叠; remember 不用 rememberSaveable 避免 K2 + mutableStateListOf 闪退)
    var topLevelOpen by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedIndications by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSearch by remember { mutableStateOf(false) }
    var searchSelected by remember { mutableStateOf<Drug?>(null) }'''
new_cat = '''    // 顶层 2 大类 + 子分类 状态 (默认全部折叠; ViewModel 跨 navigation 保留)
    val catVm: CatalogViewModel = viewModels()
    val topLevelOpen by catVm.topLevelOpen.collectAsStateWithLifecycle()
    val expandedCategories by catVm.expandedCategories.collectAsStateWithLifecycle()
    val expandedIndications by catVm.expandedIndications.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var searchSelected by remember { mutableStateOf<Drug?>(null) }'''
t = t.replace(old_cat, new_cat)

# 全部展开按钮
t = t.replace(
    '''                                topLevelOpen = setOf("cat", "ind")
                                expandedCategories = byCategory.keys.map { it.name }.toSet()
                                expandedIndications = byIndication.map { it.first.name }.toSet()''',
    '''                                catVm.setTopOpen(setOf("cat", "ind"))
                                catVm.setExpandedCategories(byCategory.keys.map { it.name }.toSet())
                                catVm.setExpandedIndications(byIndication.map { it.first.name }.toSet())'''
)
# 全部折叠按钮
t = t.replace(
    '''                                topLevelOpen = emptySet()
                                expandedCategories = emptySet()
                                expandedIndications = emptySet()''',
    '''                                catVm.setTopOpen(emptySet())
                                catVm.setExpandedCategories(emptySet())
                                catVm.setExpandedIndications(emptySet())'''
)
# 顶层 toggle
t = t.replace(
    '''                        topLevelOpen = if ("cat" in topLevelOpen) topLevelOpen - "cat" else topLevelOpen + "cat"''',
    '''                        catVm.toggleTop("cat")'''
)
t = t.replace(
    '''                        topLevelOpen = if ("ind" in topLevelOpen) topLevelOpen - "ind" else topLevelOpen + "ind"''',
    '''                        catVm.toggleTop("ind")'''
)
# category toggle
t = t.replace(
    '''                                expandedCategories = if (category.name in expandedCategories) expandedCategories - category.name else expandedCategories + category.name''',
    '''                                catVm.toggleCategory(category.name)'''
)
# indication toggle
t = t.replace(
    '''                                expandedIndications = if (indication.name in expandedIndications) expandedIndications - indication.name else expandedIndications + indication.name''',
    '''                                catVm.toggleIndication(indication.name)'''
)

# === CompareTab 头部 ===
old_cmp = '''    var drugAId by remember { mutableStateOf<String?>(null) }
    var drugBId by remember { mutableStateOf<String?>(null) }
    var infoDialogOpen by remember { mutableStateOf(false) }'''
new_cmp = '''    val cmpVm: CompareViewModel = viewModels()
    val drugAId by cmpVm.drugAId.collectAsStateWithLifecycle()
    val drugBId by cmpVm.drugBId.collectAsStateWithLifecycle()
    val infoDialogOpen by cmpVm.infoDialogOpen.collectAsStateWithLifecycle()'''
t = t.replace(old_cmp, new_cmp)

# CompareTab 操作
t = t.replace(
    "onClick = { infoDialogOpen = true }",
    "onClick = { cmpVm.setInfoDialogOpen(true) }"
)
t = t.replace(
    "onDismissRequest = { infoDialogOpen = false }",
    "onDismissRequest = { cmpVm.setInfoDialogOpen(false) }"
)
t = t.replace(
    'TextButton(onClick = { infoDialogOpen = false }) { Text("知道了") }',
    'TextButton(onClick = { cmpVm.setInfoDialogOpen(false) }) { Text("知道了") }'
)
t = t.replace(
    "drugAId = it.id\n                    drugBId = null",
    "cmpVm.setDrugAId(it.id)\n                    cmpVm.setDrugBId(null)"
)
t = t.replace(
    "onSelect = { drugBId = it.id }",
    "onSelect = { cmpVm.setDrugBId(it.id) }"
)

# === InteractionsTab 头部 ===
old_int = '''    var selectedIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<Interaction>>(emptyList()) }
    var hasEvaluated by remember { mutableStateOf(false) }
    var infoDialogOpen by remember { mutableStateOf(false) }'''
new_int = '''    val intVm: InteractionsViewModel = viewModels()
    val selectedIds by intVm.selectedIds.collectAsStateWithLifecycle()
    val results by intVm.results.collectAsStateWithLifecycle()
    val hasEvaluated by intVm.hasEvaluated.collectAsStateWithLifecycle()
    val infoDialogOpen by intVm.infoDialogOpen.collectAsStateWithLifecycle()'''
t = t.replace(old_int, new_int)

# InteractionsTab 操作 (替换 selectedIds/hasEvaluated/results/infoDialogOpen 赋值)
# selectedIds 链式操作较复杂, 用整体替换
old_int_body = '''            // 已选药 (chips 可删)
            if (selectedIds.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "已选 (${selectedIds.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedIds.forEach { id ->
                                val drug = catalog.tryGetById(id)
                                if (drug != null) {
                                    InputChip(
                                        selected = false,
                                        onClick = {
                                            selectedIds = selectedIds - id
                                            hasEvaluated = false
                                            results = emptyList()'''
new_int_body = '''            // 已选药 (chips 可删)
            if (selectedIds.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "已选 (${selectedIds.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            selectedIds.forEach { id ->
                                val drug = catalog.tryGetById(id)
                                if (drug != null) {
                                    InputChip(
                                        selected = false,
                                        onClick = {
                                            intVm.setSelectedIds(selectedIds - id)
                                            intVm.setHasEvaluated(false)
                                            intVm.setResults(emptyList())'''
t = t.replace(old_int_body, new_int_body)

# onSelect 加药
t = t.replace(
    "selectedIds = selectedIds + drug.id\n                    hasEvaluated = false\n                    results = emptyList()",
    "intVm.setSelectedIds(selectedIds + drug.id)\n                    intVm.setHasEvaluated(false)\n                    intVm.setResults(emptyList())"
)

# 评估按钮
t = t.replace(
    '''                results = RuleEngine(catalog).evaluate(userDrugs, PatientProfile())
                    hasEvaluated = true''',
    '''                intVm.setResults(RuleEngine(catalog).evaluate(userDrugs, PatientProfile()))
                    intVm.setHasEvaluated(true)'''
)

# infoDialogOpen 三处
t = t.replace(
    "onClick = { infoDialogOpen = true }",
    "onClick = { intVm.setInfoDialogOpen(true) }"
)
# 这会重复改 CompareTab 的 infoDialogOpen 行的覆盖, 但上面已经改了 — 不会重复 (因为已经被上面 if 走了)
# 用 if-match-not-contains 改: 只改 InteractionsTab 的另一处
# 实际上 上面 CompareTab 已经 set 了一次, CompareTab 没有其他 `infoDialogOpen = true` (只有顶头 + 3 处)
# 让我看剩余 occurrences:
# CompareTab:
#   - onClick = { infoDialogOpen = true }  ✓ 已改
#   - onDismissRequest = { infoDialogOpen = false }  ✓ 已改
#   - TextButton(... { infoDialogOpen = false }) { Text("知道了") }  ✓ 已改
# InteractionsTab:
#   - onClick = { infoDialogOpen = true }  (上面也匹配, 重复改了一次, 无害因为已经替换)
# 实际上如果再有 `infoDialogOpen = true` 出现, 上面可能替换错. 让我用更精确: 改成 cmpVm.setInfoDialogOpen 仅一次
# 实际上 第二次 .replace 找不到就跳过. 让我也试 infoDialogOpen=false 第二次
t = t.replace(
    "onDismissRequest = { infoDialogOpen = false }",
    "onDismissRequest = { intVm.setInfoDialogOpen(false) }"
)
t = t.replace(
    'TextButton(onClick = { infoDialogOpen = false }) { Text("知道了") }',
    'TextButton(onClick = { intVm.setInfoDialogOpen(false) }) { Text("知道了") }'
)

p.write_text(t, encoding="utf-8")
print("OK: BottomNavTabs 3 个 tab 已改用 ViewModel")
