package com.dosecare.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.catalog.IndicationGroup
import com.dosecare.app.domain.catalog.OverdoseSeverity
import com.dosecare.app.domain.patient.PatientProfile
import com.dosecare.app.domain.rules.AnticholinergicLoadInteraction
import com.dosecare.app.domain.rules.CypInteraction
import com.dosecare.app.domain.rules.Interaction
import com.dosecare.app.domain.rules.QtcInteraction
import com.dosecare.app.domain.rules.RuleEngine
import com.dosecare.app.domain.rules.SerotoninSyndromeRisk
import com.dosecare.app.domain.rules.Severity
import com.dosecare.app.domain.rules.UserDrugForRule

// ============================================================
// Tab 定义 + App 根 (v0.8a: 3 底栏 + 更多子页)
// ============================================================

enum class Tab(val title: String, val icon: ImageVector) {
    Calendar("我的用药", Icons.Default.Schedule),
    Diary("日记", Icons.Default.Edit),
    More("更多", Icons.Default.List)
}

/**
 * v0.8a Phase F9: 3 底栏 tab + 更多子页
 *
 * - CalendarTab (我的用药): 日历 + 用药计划 + 打卡
 * - DiaryTab (日记): 日历 + 日记条目
 * - MoreTab (更多): 药物目录 / 双药对比 / 相互作用 / 血药浓度推测 / 设置 / 关于 / 开源 / 联系
 *
 * 子页 (DrugDetail, CatalogTab, CompareTab, InteractionsTab, TdmScreen, AboutScreen,
 *      ContactScreen, SettingsScreen) 都从根 nav 推, AppRoot 控 state.
 *      (v0.8b: 开源许可证并入 AboutScreen, 删 OpenSourceScreen)
 */
@Composable
fun AppRoot(catalog: DrugCatalogService) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Calendar) }

    // 子页 nav state
    var detailDrugId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCatalog by rememberSaveable { mutableStateOf(false) }
    var showCompare by rememberSaveable { mutableStateOf(false) }
    var showInteractions by rememberSaveable { mutableStateOf(false) }
    var showTdm by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showContact by rememberSaveable { mutableStateOf(false) }

    // 系统返回键: 拦截子页 → 关闭子页, 回到主界面 (而不是退出 App 到桌面)
    // 用 BackHandler 链, 最后 enable 的优先
    BackHandler(enabled = detailDrugId != null) { detailDrugId = null }
    BackHandler(enabled = showCatalog) { showCatalog = false }
    BackHandler(enabled = showCompare) { showCompare = false }
    BackHandler(enabled = showInteractions) { showInteractions = false }
    BackHandler(enabled = showTdm) { showTdm = false }
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showAbout) { showAbout = false }
    BackHandler(enabled = showContact) { showContact = false }

    when {
        detailDrugId != null -> DrugDetailScreen(
            drugId = detailDrugId!!,
            catalog = catalog,
            onBack = { detailDrugId = null }
        )
        showCatalog -> CatalogTab(catalog, { detailDrugId = it }, { showCatalog = false })
        showCompare -> CompareTab(catalog, { showCompare = false })
        showInteractions -> InteractionsTab(catalog, { showInteractions = false })
        showTdm -> TdmScreen(catalog, { showTdm = false })
        showSettings -> SettingsScreen(onBack = { showSettings = false })
        showAbout -> AboutScreen(onBack = { showAbout = false })
        showContact -> ContactScreen(onBack = { showContact = false })
        else -> MainScaffold(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onDrugClick = { detailDrugId = it },
            onShowCatalog = { showCatalog = true },
            onShowCompare = { showCompare = true },
            onShowInteractions = { showInteractions = true },
            onShowTdm = { showTdm = true },
            onShowSettings = { showSettings = true },
            onShowAbout = { showAbout = true },
            onShowContact = { showContact = true },
            catalog = catalog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    onDrugClick: (String) -> Unit,
    onShowCatalog: () -> Unit,
    onShowCompare: () -> Unit,
    onShowInteractions: () -> Unit,
    onShowTdm: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    onShowContact: () -> Unit,
    catalog: DrugCatalogService
) {
    // 系统返回键: 在非 Calendar 底栏时退回 Calendar,在 Calendar 时退出 APP
    BackHandler(enabled = selectedTab != Tab.Calendar) {
        onTabSelected(Tab.Calendar)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                Tab.Calendar -> CalendarTab(catalog = catalog)
                Tab.Diary -> DiaryTab()
                Tab.More -> MoreTab(
                    onShowCatalog = onShowCatalog,
                    onShowCompare = onShowCompare,
                    onShowInteractions = onShowInteractions,
                    onShowTdm = onShowTdm,
                    onShowSettings = onShowSettings,
                    onShowAbout = onShowAbout,
                    onShowContact = onShowContact,
                    catalog = catalog
                )
            }
        }
    }
}

// ============================================================
// v0.8a Phase F8: 更多 Tab (sub-items 列表)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTab(
    onShowCatalog: () -> Unit,
    onShowCompare: () -> Unit,
    onShowInteractions: () -> Unit,
    onShowTdm: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    onShowContact: () -> Unit,
    catalog: DrugCatalogService
) {
    val totalDrugs = remember { catalog.all().size }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("更多") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 工具区
            Text(
                "🧰 工具",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            MoreItem("药物目录", "$totalDrugs 药 · 按类别 / 适应症浏览", Icons.Default.List, onClick = onShowCatalog)
            MoreItem("双药对比", "PK / 治疗窗 / CYP / 副作用 详细对比", Icons.AutoMirrored.Filled.CompareArrows, onClick = onShowCompare)
            MoreItem("相互作用", "当前用户所有药之间的相互作用检测", Icons.Default.Warning, onClick = onShowInteractions)
            MoreItem("血药浓度推测", "1/2/3 房室模型 · 治疗窗色带", Icons.Default.Science, onClick = onShowTdm)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Text(
                "⚙️ 设置 / 关于",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            MoreItem("设置", "个性化 / 提醒 / 提醒方式", Icons.Default.Settings, onClick = onShowSettings)
            MoreItem("关于", "数据源 / 许可证 / 局限性", Icons.Default.Info, onClick = onShowAbout)
            MoreItem("联系作者", "Bug 反馈 / 功能建议", Icons.Default.Edit, onClick = onShowContact)
        }
    }
}

@Composable
private fun MoreItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 血药浓度推测 (TDM) - 全屏 modal, 从「我的处方」顶部进入
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TdmScreen(
    catalog: DrugCatalogService,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("血药浓度推测 (TDM)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TdmModule(catalog = catalog)
        }
    }
}

// ============================================================
// Tab 2: 药物目录
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTab(
    catalog: DrugCatalogService,
    onDrugClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val allDrugs = remember { catalog.all() }
    val byCategory = remember(allDrugs) {
        allDrugs.groupBy { it.category }
            .toSortedMap(compareBy { it.displayName })
    }
    val byIndication = remember(allDrugs) {
        allDrugs.flatMap { d -> d.indicationGroups.map { it to d } }
            .groupBy({ it.first }, { it.second })
            .toList()
            .sortedBy { it.first.displayName }
    }
    // 顶层 2 大类 + 子分类 状态 (默认全部折叠; ViewModel 跨 navigation 保留)
        val topLevelOpen by CatalogViewModel.topLevelOpen.collectAsState()
    val expandedCategories by CatalogViewModel.expandedCategories.collectAsState()
    val expandedIndications by CatalogViewModel.expandedIndications.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var searchSelected by remember { mutableStateOf<Drug?>(null) }
    val totalCount = allDrugs.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("药物目录 · ${totalCount} 药") },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶部: 全展开/全折叠 + 搜索 (无红框容器, 3 个 OutlinedButton 横向排)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            CatalogViewModel.setTopOpen(setOf("cat", "ind"))
                            CatalogViewModel.setExpandedCategories(byCategory.keys.map { it.name }.toSet())
                            CatalogViewModel.setExpandedIndications(byIndication.map { it.first.name }.toSet())
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.UnfoldMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("全部展开", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = {
                            CatalogViewModel.setTopOpen(emptySet())
                            CatalogViewModel.setExpandedCategories(emptySet())
                            CatalogViewModel.setExpandedIndications(emptySet())
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.UnfoldLess,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("全部折叠", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                    FilledTonalButton(
                        onClick = { showSearch = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("搜索", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
            }

            // 大类 1: 按药物类别 (药理学分类)
            item(key = "top_cat") {
                TopLevelHeader(
                    title = "💊 按药物类别 (药理学分类)",
                    subtitle = "${byCategory.size} 类 · ${totalCount} 药",
                    isExpanded = "cat" in topLevelOpen,
                    onToggle = {
                        CatalogViewModel.toggleTop("cat")
                    }
                )
            }
            if ("cat" in topLevelOpen) {
                byCategory.forEach { (category, drugs) ->
                    item(key = "cat_${category.name}") {
                        CategoryHeader(
                            category = category.displayName,
                            count = drugs.size,
                            isExpanded = category.name in expandedCategories,
                            onToggle = {
                                CatalogViewModel.toggleCategory(category.name)
                            }
                        )
                    }
                    if (category.name in expandedCategories) {
                        items(drugs.sortedBy { it.genericNameZh }, key = { "cat_${category.name}_${it.id}" }) { drug ->
                            DrugCard(drug, onClick = { onDrugClick(drug.id) })
                        }
                    }
                }
            }

            // 大类 2: 按主治症状 (IndicationGroup)
            item(key = "top_ind") {
                TopLevelHeader(
                    title = "🩺 按主治症状 (疾病/适应症)",
                    subtitle = "${byIndication.size} 类 · ${totalCount} 药 (一药可属多类)",
                    isExpanded = "ind" in topLevelOpen,
                    onToggle = {
                        CatalogViewModel.toggleTop("ind")
                    }
                )
            }
            if ("ind" in topLevelOpen) {
                byIndication.forEach { (indication, drugs) ->
                    item(key = "ind_${indication.name}") {
                        CategoryHeader(
                            category = indication.displayName,
                            count = drugs.size,
                            isExpanded = indication.name in expandedIndications,
                            onToggle = {
                                CatalogViewModel.toggleIndication(indication.name)
                            }
                        )
                    }
                    if (indication.name in expandedIndications) {
                        items(drugs.sortedBy { it.genericNameZh }, key = { "ind_${indication.name}_${it.id}" }) { drug ->
                            DrugCard(drug, onClick = { onDrugClick(drug.id) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item { DisclaimerCard() }
        }
    }

    if (showSearch) {
        SearchableDrugPicker(
            label = "搜索 ${allDrugs.size} 个药",
            selected = null,
            options = allDrugs.sortedBy { it.genericNameZh },
            onSelect = { drug ->
                searchSelected = drug
                showSearch = false
                onDrugClick(drug.id)
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun TopLevelHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "$count 药",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ============================================================
// Tab 3: 双药对比 (Spec Comparison, 不含 RuleEngine)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareTab(
    catalog: DrugCatalogService,
    onSettingsClick: () -> Unit
) {
    val drugs = remember { catalog.all().sortedBy { it.genericNameZh } }
    // 跨 navigation 保留 (选好药 A/B 后跳详情, 返回 A/B 还在)
        val drugAId by CompareViewModel.drugAId.collectAsState()
    val drugBId by CompareViewModel.drugBId.collectAsState()
    val infoDialogOpen by CompareViewModel.infoDialogOpen.collectAsState()
    val drugA = drugAId?.let { catalog.tryGetById(it) }
    val drugB = drugBId?.let { catalog.tryGetById(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("双药对比") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "双药规格对比",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { CompareViewModel.setInfoDialogOpen(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "对比说明",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (infoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { CompareViewModel.setInfoDialogOpen(false) },
                    title = { Text("ℹ️ 双药对比说明") },
                    text = {
                        Column {
                            Text("• 选两个药,按 7 个 section 对比 (基本信息 / 药代动力学 / 治疗窗 / CYP 角色 / 不良反应 / 剂量调整 / 监测)", style = MaterialTheme.typography.bodySmall)
                            Text("• 同值自动合并,不同值分两列展示", style = MaterialTheme.typography.bodySmall)
                            Text("• 治疗窗 / CYP 谱参考 AGNP 2017 + Flockhart Table", style = MaterialTheme.typography.bodySmall)
                            Text("• 不良反应等级: VERY_LOW / LOW / MEDIUM / HIGH / VERY_HIGH", style = MaterialTheme.typography.bodySmall)
                            Text("• 本工具仅展示规格差异,临床决策请结合实际病人情况", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { CompareViewModel.setInfoDialogOpen(false) }) { Text("知道了") }
                    }
                )
            }
            SearchableDrugPicker(
                label = "药 A",
                selected = drugA,
                options = drugs,
                onSelect = {
                    CompareViewModel.setDrugAId(it.id)
                    CompareViewModel.setDrugBId(null)
                }
            )
            SearchableDrugPicker(
                label = "药 B",
                selected = drugB,
                options = drugs.filter { it.id != drugAId },
                onSelect = { CompareViewModel.setDrugBId(it.id) }
            )
            if (drugA != null && drugB != null) {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                SpecComparison(drugA, drugB)
            }
        }
    }
}

// ============================================================
// Tab 4: 相互作用 (多药 RuleEngine 警示)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun InteractionsTab(
    catalog: DrugCatalogService,
    onSettingsClick: () -> Unit
) {
    val drugs = remember { catalog.all().sortedBy { it.genericNameZh } }
        val selectedIds by InteractionsViewModel.selectedIds.collectAsState()
    val results by InteractionsViewModel.results.collectAsState()
    val hasEvaluated by InteractionsViewModel.hasEvaluated.collectAsState()
    val infoDialogOpen by InteractionsViewModel.infoDialogOpen.collectAsState()
    val availableDrugs = remember(selectedIds) { drugs.filter { it.id !in selectedIds } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("相互作用 · ${selectedIds.size} 药") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Intro (说明搬入感叹号 AlertDialog)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "多药相互作用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { CompareViewModel.setInfoDialogOpen(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "评估说明",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (infoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { CompareViewModel.setInfoDialogOpen(false) },
                    title = { Text("ℹ️ 多药相互作用说明") },
                    text = {
                        Column {
                            Text("• 添加 2+ 个药,RuleEngine 跑所有组合 (N×(N-1)/2 对)", style = MaterialTheme.typography.bodySmall)
                            Text("• 6 类规则: CYP 抑制/诱导、QTc 累积、抗胆碱能、5-HT 综合征、治疗窗偏离、肾/肝剂量调整", style = MaterialTheme.typography.bodySmall)
                            Text("• 严重度: LOW / MEDIUM / HIGH / CONTRAINDICATED", style = MaterialTheme.typography.bodySmall)
                            Text("• \"未检出\" ≠ \"安全\"——本评估仅覆盖 6 类规则,实际还要考虑年龄、肝肾、CYP 基因型、蛋白结合置换、肠肝循环、食物/草本补充剂等", style = MaterialTheme.typography.bodySmall)
                            Text("• 个体差异极大,用药请遵医嘱或咨询临床药师", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { CompareViewModel.setInfoDialogOpen(false) }) { Text("知道了") }
                    }
                )
            }

            // 已选药 (chips 可删)
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
                                            InteractionsViewModel.setSelectedIds(selectedIds - id)
                                            InteractionsViewModel.setHasEvaluated(false)
                                            InteractionsViewModel.setResults(emptyList())
                                        },
                                        label = { Text(drug.genericNameZh) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "移除",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 搜索选药 (替代之前 take(50) 的 dropdown,修复"剩余 73 不可点"bug)
            SearchableDrugPicker(
                label = "添加药 · 搜索 (${availableDrugs.size} 可选)",
                selected = null,
                options = availableDrugs,
                onSelect = { drug ->
                    InteractionsViewModel.setSelectedIds(selectedIds + drug.id)
                    InteractionsViewModel.setHasEvaluated(false)
                    InteractionsViewModel.setResults(emptyList())
                },
                placeholder = "🔍 搜索: 氟 / fluvoxamine / 氯氮平 / F"
            )

            // 评估按钮
            Button(
                onClick = {
                    val userDrugs = selectedIds.map { id ->
                        val drug = catalog.getById(id)
                        UserDrugForRule(id, defaultDose(drug), 2.0)
                    }
                    InteractionsViewModel.setResults(RuleEngine(catalog).evaluate(userDrugs, PatientProfile()))
                    InteractionsViewModel.setHasEvaluated(true)
                },
                enabled = selectedIds.size >= 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasEvaluated) "重新评估 ${selectedIds.size} 药的所有组合"
                    else "评估 ${selectedIds.size} 药的所有组合"
                )
            }

            // 结果
            if (hasEvaluated) {
                if (results.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "✓ RuleEngine 未检出 6 类已知警示。",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "但这不代表安全——本次评估仅覆盖:CYP 抑制/诱导、QTc 累积、抗胆碱能负荷、5-HT 综合征、治疗窗偏离、肾/肝剂量调整。" +
                                    "实际临床还要考虑:年龄、肝肾、基因型(CYP2D6/2C19 慢代谢)、蛋白结合置换、肠肝循环、食物/草本补充剂等。" +
                                    "个体差异极大,仍请遵医嘱。",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "RuleEngine 检出 ${results.size} 条警示",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))
                            results.forEach { InteractionRow(it) }
                        }
                    }
                }
                // 过量警示汇总 (与 RuleEngine 警示并行显示)
                val highOverdose = selectedIds.mapNotNull { id ->
                    val d = catalog.tryGetById(id) ?: return@mapNotNull null
                    d.overdose?.takeIf { it.severity == OverdoseSeverity.LIFE_THREATENING || it.severity == OverdoseSeverity.SEVERE }
                }
                if (highOverdose.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFB71C1C),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "⚠️ ${highOverdose.size} 药为高风险过量 (重症 / 危及生命)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            highOverdose.forEach { o ->
                                val drugName = selectedIds.firstNotNullOfOrNull { id ->
                                    catalog.tryGetById(id)?.takeIf { it.overdose === o }?.genericNameZh
                                } ?: "?"
                                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        "• $drugName (${o.severity.displayName})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB71C1C),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    o.antidote?.let { ant ->
                                        Text(
                                            " 💉 $ant",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB71C1C)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "过量处理需多药协调(活性炭 / 血液透析 / 解毒剂等), 立即拨打 120 或当地中毒急救中心",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 共享 composables (从 InteractionScreen 搬过来)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrugPicker(
    label: String,
    selected: Drug?,
    options: List<Drug>,
    onSelect: (Drug) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${options.size} 个药可选",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selected?.let { "${it.genericNameZh} (${it.genericName})" } ?: "点击选择…",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.take(50).forEach { drug ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(drug.genericNameZh, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${drug.genericName} · ${drug.category.displayName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    onSelect(drug)
                                    expanded = false
                                }
                            )
                        }
                        if (options.size > 50) {
                            DropdownMenuItem(
                                text = { Text("…还有 ${options.size - 50} 个药(v0.4 加搜索)") },
                                onClick = { expanded = false },
                                enabled = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InteractionRow(interaction: Interaction) {
    val color = severityColor(interaction.severity)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        interaction.severity.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    interaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(interaction.detail, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "建议:${interaction.advice}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            MechanismLine(interaction)
            if (interaction.references.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "参考:${interaction.references.take(2).joinToString(";")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MechanismLine(interaction: Interaction) {
    val text = when (interaction) {
        is CypInteraction -> "${interaction.cyp.displayName} · 估算 AUC 变化 ${"%.1f".format(interaction.patientAdjustedFold)}x"
        is QtcInteraction -> "估算 QTc ${"%.0f".format(interaction.finalEstimatedQtcMs)} ms"
        is AnticholinergicLoadInteraction -> "ACB 评分 ${interaction.totalScore}"
        is SerotoninSyndromeRisk -> "涉及 ${interaction.serotonergicDrugs.size} 个 5-HT 增强药"
        else -> null
    }
    if (text != null) {
        Spacer(Modifier.height(2.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun severityColor(severity: Severity): Color = Color(android.graphics.Color.parseColor(severity.color))

/**
 * 给药在 demo 时的默认剂量:取 forms[0].commonDoseRangeMg 的中点。
 */
internal fun defaultDose(drug: Drug): Double {
    val range = drug.forms.firstOrNull()?.commonDoseRangeMg
    return if (range != null) (range.first + range.second) / 2.0 else 100.0
}

// ============================================================
// 设置 / 关于
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置 · 关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "DoseCare",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("v0.3.2 P0 极简版 · Debug", style = MaterialTheme.typography.bodyMedium)
                    Text("com.dosecare.app · AGPL-3.0", style = MaterialTheme.typography.labelSmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("📚 数据源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("• DrugCatalog: AGNP 2017 + FDA DailyMed + Flockhart CYP Table", style = MaterialTheme.typography.bodySmall)
                    Text("• PK 参数: 1 房室口服模型 (One-compartment oral)", style = MaterialTheme.typography.bodySmall)
                    Text("• 治疗窗: AGNP 2017 TDM 共识指南", style = MaterialTheme.typography.bodySmall)
                    Text("• 相互作用: RuleEngine 6 类自研规则", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ 局限性", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("• 1 房室模型对部分药不够准确(苯妥英非线性代谢、锂肾清除)", style = MaterialTheme.typography.bodySmall)
                    Text("• 群体平均参数,个体差异 2-3 倍(CYP 基因型 / 吸烟 / 年龄 / 肾 / 肝)", style = MaterialTheme.typography.bodySmall)
                    Text("• v0.3.2 不接医学数据库 API;PK 参数以 AGNP/FDA 静态数据为基准", style = MaterialTheme.typography.bodySmall)
                    Text("• 不替代医师判断;实际用药请遵医嘱或咨询临床药师", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🗺 路线图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("v0.4 计划: Hilt DI + Room 持久化 + 服药提醒", style = MaterialTheme.typography.bodySmall)
                    Text("v0.5 计划: 多房室 PK + 患者画像(吸烟/肾/肝)校准", style = MaterialTheme.typography.bodySmall)
                    Text("v1.0 计划: 联网医学数据库校准 + 群体 PK Bayes 估计", style = MaterialTheme.typography.bodySmall)
                    Text("v1.5 计划: KMP 多平台", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "⚠️ 本 APP 所有计算结果(PK 估算、警示)仅供参考。" +
                        "个体差异显著,实际用药请遵医嘱。",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
