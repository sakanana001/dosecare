package com.dosecare.app.ui

import com.dosecare.app.data.repository.PrescriptionRepositoryRoom
import com.dosecare.app.domain.prescription.PrescriptionGroup
import com.dosecare.app.domain.rules.Interaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 4 个 tab 的状态 object 单例 — 跨 Composable 重组保留状态
 *
 * 不依赖 ViewModel delegate (K2 编译器对 viewModels() 解析有 bug)
 * object 在 JVM 上是单例, App 进程不杀就一直活着
 * tab 切换 / 跳详情返回 / Composable 重组, 状态都在
 * 进程被杀丢 (没接 SavedStateHandle, 后续可加)
 *
 * v0.9g PrescriptionViewModel 增加 2 个字段:
 * - groups: StateFlow<List<PrescriptionGroup>> 从 Room observeGroups
 * - updateGroups(transform): 写 Room
 * 都需要先调 init(repo, scope) 注入, 由 DoseCareApp.onCreate 启动时调用
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

    // ============== v0.9g: Room-backed groups ==============

    @Volatile
    private var repoRef: PrescriptionRepositoryRoom? = null
    @Volatile
    private var appScopeRef: CoroutineScope? = null
    @Volatile
    private var _groups: StateFlow<List<PrescriptionGroup>>? = null

    /**
     * App.onCreate 阶段调一次: 注入 repo + 启动 observe groups
     * 重复调 noop
     */
    @Synchronized
    fun init(repo: PrescriptionRepositoryRoom, scope: CoroutineScope) {
        if (repoRef != null) return
        repoRef = repo
        appScopeRef = scope
        _groups = repo.observeGroups().stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    }

    /**
     * 处方组订阅流 (init 之后才能用)
     * 未 init 时返回空 list
     */
    val groups: StateFlow<List<PrescriptionGroup>>
        get() = _groups ?: MutableStateFlow<List<PrescriptionGroup>>(emptyList()).asStateFlow()

    /**
     * 写操作: 用 transform 算新 list, 落 Room
     * RepositoryRoom 的 saveAll/upsertDrug 末尾自动调 ReminderScheduler.scheduleAll(),
     * 所以改 times 后下一次 reminder 就会用自定义时段
     */
    fun updateGroups(transform: (List<PrescriptionGroup>) -> List<PrescriptionGroup>) {
        val repo = repoRef ?: return
        val scope = appScopeRef ?: return
        val current = _groups?.value ?: emptyList()
        val next = transform(current)
        if (next != current) {
            scope.launch(Dispatchers.IO) {
                repo.saveAll(next)
            }
        }
    }
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
