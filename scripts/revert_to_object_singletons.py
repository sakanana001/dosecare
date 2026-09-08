"""放弃 ViewModel delegate, 改用 object 单例 + mutableStateOf (K2 兼容, 跨 navigation 状态保留)"""
from pathlib import Path

# 1) ViewModels.kt: 4 个 class 改成 object, 字段用 mutableStateOf
p0 = Path("app/src/main/java/com/dosecare/app/ui/ViewModels.kt")
t0 = p0.read_text(encoding="utf-8")
# 替换 class 为 object
t0 = t0.replace("class PrescriptionViewModel : ViewModel() {", "object PrescriptionViewModel {")
t0 = t0.replace("class CatalogViewModel : ViewModel() {", "object CatalogViewModel {")
t0 = t0.replace("class CompareViewModel : ViewModel() {", "object CompareViewModel {")
t0 = t0.replace("class InteractionsViewModel : ViewModel() {", "object InteractionsViewModel {")
# 删除 import ViewModel (不再需要)
t0 = t0.replace("import androidx.lifecycle.ViewModel\n", "")
# StateFlow 还是保留 collectAsStateWithLifecycle 用 — 但 object 字段直接 mutableStateOf 也能用
# 实际上 collectAsStateWithLifecycle 接收 StateFlow, 我们的字段是 MutableState (有 .asStateFlow() 方法)
# 让 object 字段用 mutableStateOf, 然后 Composable 用 "by collectAsStateWithLifecycle()" 仍要 StateFlow
# 解决: 暴露 as StateFlow 的 get, 字段用 mutableStateFlow
# 重写整个 ViewModels.kt
new_vms = '''package com.dosecare.app.ui

import com.dosecare.app.domain.rules.Interaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 4 个 tab 的状态 object 单例 — 跨 Composable 重组保留状态
 *
 * 不依赖 ViewModel delegate (K2 编译器对 viewModels() 解析有 bug)
 * object 在 JVM 上是单例, App 进程不杀就一直活着
 * tab 切换 / 跳详情返回 / Composable 重组, 状态都在
 * 进程被杀丢 (没接 SavedStateHandle, 后续可加)
 */
object PrescriptionViewModel {
    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroups: StateFlow<Set<String>> = _expandedGroups.asStateFlow()
    fun toggleGroup(id: String) {
        _expandedGroups.value = if (id in _expandedGroups.value) _expandedGroups.value - id
                                else _expandedGroups.value + id
    }
    private val _infoDialogOpen = MutableStateFlow(false)
    val infoDialogOpen: StateFlow<Boolean> = _infoDialogOpen.asStateFlow()
    fun setInfoDialogOpen(v: Boolean) { _infoDialogOpen.value = v }
}

object CatalogViewModel {
    private val _topLevelOpen = MutableStateFlow<Set<String>>(emptySet())
    val topLevelOpen: StateFlow<Set<String>> = _topLevelOpen.asStateFlow()
    fun toggleTop(key: String) {
        _topLevelOpen.value = if (key in _topLevelOpen.value) _topLevelOpen.value - key
                              else _topLevelOpen.value + key
    }
    fun setTopOpen(keys: Set<String>) { _topLevelOpen.value = keys }
    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories: StateFlow<Set<String>> = _expandedCategories.asStateFlow()
    fun toggleCategory(name: String) {
        _expandedCategories.value = if (name in _expandedCategories.value) _expandedCategories.value - name
                                   else _expandedCategories.value + name
    }
    fun setExpandedCategories(keys: Set<String>) { _expandedCategories.value = keys }
    private val _expandedIndications = MutableStateFlow<Set<String>>(emptySet())
    val expandedIndications: StateFlow<Set<String>> = _expandedIndications.asStateFlow()
    fun toggleIndication(name: String) {
        _expandedIndications.value = if (name in _expandedIndications.value) _expandedIndications.value - name
                                     else _expandedIndications.value + name
    }
    fun setExpandedIndications(keys: Set<String>) { _expandedIndications.value = keys }
}

object CompareViewModel {
    private val _drugAId = MutableStateFlow<String?>(null)
    val drugAId: StateFlow<String?> = _drugAId.asStateFlow()
    fun setDrugAId(id: String?) { _drugAId.value = id }
    private val _drugBId = MutableStateFlow<String?>(null)
    val drugBId: StateFlow<String?> = _drugBId.asStateFlow()
    fun setDrugBId(id: String?) { _drugBId.value = id }
    private val _infoDialogOpen = MutableStateFlow(false)
    val infoDialogOpen: StateFlow<Boolean> = _infoDialogOpen.asStateFlow()
    fun setInfoDialogOpen(v: Boolean) { _infoDialogOpen.value = v }
}

object InteractionsViewModel {
    private val _selectedIds = MutableStateFlow<List<String>>(emptyList())
    val selectedIds: StateFlow<List<String>> = _selectedIds.asStateFlow()
    fun setSelectedIds(v: List<String>) { _selectedIds.value = v }
    private val _results = MutableStateFlow<List<Interaction>>(emptyList())
    val results: StateFlow<List<Interaction>> = _results.asStateFlow()
    fun setResults(v: List<Interaction>) { _results.value = v }
    private val _hasEvaluated = MutableStateFlow(false)
    val hasEvaluated: StateFlow<Boolean> = _hasEvaluated.asStateFlow()
    fun setHasEvaluated(v: Boolean) { _hasEvaluated.value = v }
    private val _infoDialogOpen = MutableStateFlow(false)
    val infoDialogOpen: StateFlow<Boolean> = _infoDialogOpen.asStateFlow()
    fun setInfoDialogOpen(v: Boolean) { _infoDialogOpen.value = v }
}
'''
p0.write_text(new_vms, encoding="utf-8")
print("OK: ViewModels.kt 4 个 class 改成 object 单例")

# 2) 移除 lifecycle-viewmodel-compose 依赖
p1 = Path("app/build.gradle.kts")
t1 = p1.read_text(encoding="utf-8")
t1 = t1.replace(
    "    implementation(libs.androidx.lifecycle.viewmodel.compose)\n",
    ""
)
p1.write_text(t1, encoding="utf-8")
print("OK: build.gradle.kts 移除 viewmodel-compose 依赖")

# 3) BottomNavTabs.kt 改回不用 viewModel delegate — 改用 object 引用
p2 = Path("app/src/main/java/com/dosecare/app/ui/BottomNavTabs.kt")
t2 = p2.read_text(encoding="utf-8")
# 移除 viewModel related imports
t2 = t2.replace(
    "import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner\nimport androidx.lifecycle.ViewModelProvider\n",
    ""
)
t2 = t2.replace(
    "import androidx.lifecycle.compose.collectAsStateWithLifecycle\n",
    ""
)
# 替换 val cmpVm: CompareViewModel = ViewModelProvider(...) 为 object 直接引用
t2 = t2.replace(
    "val catVm: CatalogViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[CatalogViewModel::class.java]\n",
    ""
)
t2 = t2.replace(
    "val cmpVm: CompareViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[CompareViewModel::class.java]\n",
    ""
)
t2 = t2.replace(
    "val intVm: InteractionsViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[InteractionsViewModel::class.java]\n",
    ""
)
# 替换 collectAsStateWithLifecycle 为直接 .value (object 是单例, 直接读 StateFlow.value)
# 简化: 用 .collectAsState() 也可以, 但需要 import
# 改用 by .collectAsState() — 需要 androidx.compose.runtime.collectAsState
# 或者最简单: 把 StateFlow 改回 mutableStateOf, Composable 用 by remember.collectAsState() 不行 (remember 创建新 state)
# 实际上 StateFlow.collectAsState() 是 Compose 标准做法, 不需要 import androidx.lifecycle.compose
# 让我加 import collectAsState (from runtime)
# 由
#   val topLevelOpen by catVm.topLevelOpen.collectAsStateWithLifecycle()
# 改为
#   val topLevelOpen by catVm.topLevelOpen.collectAsState()
t2 = t2.replace(".collectAsStateWithLifecycle()", ".collectAsState()")
# 加 import collectAsState
t2 = t2.replace(
    "import androidx.compose.runtime.*\n",
    "import androidx.compose.runtime.*\nimport androidx.compose.runtime.collectAsState\n"
)
p2.write_text(t2, encoding="utf-8")
print("OK: BottomNavTabs.kt 改用 object 单例 + collectAsState")

# 4) PrescriptionTab.kt 同上
p3 = Path("app/src/main/java/com/dosecare/app/ui/PrescriptionTab.kt")
t3 = p3.read_text(encoding="utf-8")
t3 = t3.replace(
    "import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner\nimport androidx.lifecycle.ViewModelProvider\n",
    ""
)
t3 = t3.replace(
    "import androidx.lifecycle.compose.collectAsStateWithLifecycle\n",
    ""
)
t3 = t3.replace(
    "val vm: PrescriptionViewModel = ViewModelProvider(LocalViewModelStoreOwner.current!!)[PrescriptionViewModel::class.java]\n",
    ""
)
t3 = t3.replace(".collectAsStateWithLifecycle()", ".collectAsState()")
t3 = t3.replace(
    "import androidx.compose.runtime.*\n",
    "import androidx.compose.runtime.*\nimport androidx.compose.runtime.collectAsState\n"
)
p3.write_text(t3, encoding="utf-8")
print("OK: PrescriptionTab.kt 改用 object 单例")
