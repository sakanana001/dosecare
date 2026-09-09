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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.dosecare.app.R
import com.dosecare.app.BuildConfig
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

enum class Tab(@StringRes val titleRes: Int, val icon: ImageVector) {
    Calendar(R.string.nav_tab_calendar, Icons.Default.Schedule),
    Diary(R.string.nav_tab_diary, Icons.Default.Edit),
    More(R.string.nav_tab_more, Icons.Default.List)
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
fun AppRoot(
    catalog: DrugCatalogService,
    pendingReminder: kotlinx.coroutines.flow.StateFlow<com.dosecare.app.MainActivity.PendingReminder?>? = null,
    onPendingConsumed: () -> Unit = {}
) {
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
            catalog = catalog,
            pendingReminder = pendingReminder,
            onPendingConsumed = onPendingConsumed
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
    catalog: DrugCatalogService,
    pendingReminder: kotlinx.coroutines.flow.StateFlow<com.dosecare.app.MainActivity.PendingReminder?>? = null,
    onPendingConsumed: () -> Unit = {}
) {
    // 系统返回键: 在非 Calendar 底栏时退回 Calendar,在 Calendar 时退出 APP
    BackHandler(enabled = selectedTab != Tab.Calendar) {
        onTabSelected(Tab.Calendar)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val title = stringResource(tab.titleRes)
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = title
                            )
                        },
                        label = {
                            Text(
                                text = title,
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
                Tab.Calendar -> CalendarTab(
                    catalog = catalog,
                    pendingReminder = pendingReminder,
                    onPendingConsumed = onPendingConsumed
                )
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
            TopAppBar(title = { Text(stringResource(R.string.nav_tab_more)) })
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
                stringResource(R.string.more_section_tools),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            MoreItem(
                stringResource(R.string.more_item_catalog),
                stringResource(R.string.more_item_catalog_sub, totalDrugs),
                Icons.Default.List, onClick = onShowCatalog
            )
            MoreItem(
                stringResource(R.string.more_item_compare),
                stringResource(R.string.more_item_compare_sub),
                Icons.AutoMirrored.Filled.CompareArrows, onClick = onShowCompare
            )
            MoreItem(
                stringResource(R.string.more_item_interaction),
                stringResource(R.string.more_item_interaction_sub),
                Icons.Default.Warning, onClick = onShowInteractions
            )
            MoreItem(
                stringResource(R.string.more_item_tdm),
                stringResource(R.string.more_item_tdm_sub),
                Icons.Default.Science, onClick = onShowTdm
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Text(
                stringResource(R.string.more_section_settings_about),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            MoreItem(
                stringResource(R.string.more_item_settings),
                stringResource(R.string.more_item_settings_sub),
                Icons.Default.Settings, onClick = onShowSettings
            )
            MoreItem(
                stringResource(R.string.more_item_about),
                stringResource(R.string.more_item_about_sub),
                Icons.Default.Info, onClick = onShowAbout
            )
            MoreItem(
                stringResource(R.string.more_item_contact),
                stringResource(R.string.more_item_contact_sub),
                Icons.Default.Edit, onClick = onShowContact
            )
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
                title = { Text(stringResource(R.string.tdm_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
    val isChinesePrimarySort = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "zh"
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
                title = { Text(stringResource(R.string.catalog_title, totalCount)) },
                actions = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.common_settings))
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
                        Text(stringResource(R.string.catalog_expand_all), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
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
                        Text(stringResource(R.string.catalog_collapse_all), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
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
                        Text(stringResource(R.string.common_search), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    }
                }
            }

            // 大类 1: 按药物类别 (药理学分类)
            item(key = "top_cat") {
                TopLevelHeader(
                    title = stringResource(R.string.catalog_by_category),
                    subtitle = stringResource(R.string.catalog_by_category_sub, byCategory.size, totalCount),
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
                            category = stringResource(category.displayNameRes),
                            count = drugs.size,
                            isExpanded = category.name in expandedCategories,
                            onToggle = {
                                CatalogViewModel.toggleCategory(category.name)
                            }
                        )
                    }
                    if (category.name in expandedCategories) {
                        items(drugs.sortedBy { if (isChinesePrimarySort) it.genericNameZh else it.genericName.lowercase() }, key = { "cat_${category.name}_${it.id}" }) { drug ->
                            DrugCard(drug, onClick = { onDrugClick(drug.id) })
                        }
                    }
                }
            }

            // 大类 2: 按主治症状 (IndicationGroup)
            item(key = "top_ind") {
                TopLevelHeader(
                    title = stringResource(R.string.catalog_by_indication),
                    subtitle = stringResource(R.string.catalog_by_indication_sub, byIndication.size, totalCount),
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
                            category = stringResource(indication.displayNameRes),
                            count = drugs.size,
                            isExpanded = indication.name in expandedIndications,
                            onToggle = {
                                CatalogViewModel.toggleIndication(indication.name)
                            }
                        )
                    }
                    if (indication.name in expandedIndications) {
                        items(drugs.sortedBy { if (isChinesePrimarySort) it.genericNameZh else it.genericName.lowercase() }, key = { "ind_${indication.name}_${it.id}" }) { drug ->
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
            label = stringResource(R.string.catalog_search_n, allDrugs.size),
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
                contentDescription = if (isExpanded) stringResource(R.string.catalog_collapse_all) else stringResource(R.string.catalog_expand_all),
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
                contentDescription = if (isExpanded) stringResource(R.string.catalog_collapse_all) else stringResource(R.string.catalog_expand_all),
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
                    stringResource(R.string.catalog_drug_count, count),
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
                title = { Text(stringResource(R.string.compare_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.common_settings))
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
                        stringResource(R.string.compare_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { CompareViewModel.setInfoDialogOpen(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.compare_info),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (infoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { CompareViewModel.setInfoDialogOpen(false) },
                    title = { Text(stringResource(R.string.compare_info_title)) },
                    text = {
                        Column {
                            // 5 行分句,合并为 1 个 strings.xml multiline
                            Text(stringResource(R.string.compare_info_lines), style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { CompareViewModel.setInfoDialogOpen(false) }) { Text(stringResource(R.string.common_known)) }
                    }
                )
            }
            SearchableDrugPicker(
                label = stringResource(R.string.compare_drug_a),
                selected = drugA,
                options = drugs,
                onSelect = {
                    CompareViewModel.setDrugAId(it.id)
                    CompareViewModel.setDrugBId(null)
                }
            )
            SearchableDrugPicker(
                label = stringResource(R.string.compare_drug_b),
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
                title = { Text(stringResource(R.string.interaction_title, selectedIds.size)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.common_settings))
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
                        stringResource(R.string.interaction_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { CompareViewModel.setInfoDialogOpen(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.interaction_info),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            if (infoDialogOpen) {
                AlertDialog(
                    onDismissRequest = { CompareViewModel.setInfoDialogOpen(false) },
                    title = { Text(stringResource(R.string.interaction_info_title)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.interaction_info_lines), style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { CompareViewModel.setInfoDialogOpen(false) }) { Text(stringResource(R.string.common_known)) }
                    }
                )
            }

            // 已选药 (chips 可删)
            if (selectedIds.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.interaction_selected_n, selectedIds.size),
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
                                                contentDescription = stringResource(R.string.common_remove),
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
                label = stringResource(R.string.interaction_add_drug_n, availableDrugs.size),
                selected = null,
                options = availableDrugs,
                onSelect = { drug ->
                    InteractionsViewModel.setSelectedIds(selectedIds + drug.id)
                    InteractionsViewModel.setHasEvaluated(false)
                    InteractionsViewModel.setResults(emptyList())
                },
                placeholder = stringResource(R.string.interaction_search_hint)
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
                    if (hasEvaluated) stringResource(R.string.interaction_reevaluate_n, selectedIds.size)
                    else stringResource(R.string.interaction_evaluate_n, selectedIds.size)
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
                                stringResource(R.string.interaction_no_alerts_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.interaction_no_alerts_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.interaction_alerts_count, results.size),
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
                                    stringResource(R.string.interaction_high_overdose_n, highOverdose.size),
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
                                        "• $drugName (${stringResource(o.severity.displayNameRes)})",
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
                                stringResource(R.string.interaction_overdose_help),
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
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val isChinesePrimary = locale.language == "zh"
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
                    stringResource(R.string.interaction_n_drugs_available, options.size),
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
                        value = selected?.let {
                            val p = if (isChinesePrimary) it.genericNameZh else it.genericName
                            val s = if (isChinesePrimary) it.genericName else it.genericNameZh
                            stringResource(R.string.drug_name_with_zh_subtitle, p, s)
                        } ?: stringResource(R.string.common_search),
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
                            val pName = if (isChinesePrimary) drug.genericNameZh else drug.genericName
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(pName, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            stringResource(drug.category.displayNameRes),
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
                                text = { Text(stringResource(R.string.interaction_more_drugs, options.size - 50)) },
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
                        stringResource(interaction.severity.displayNameRes),
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
                stringResource(R.string.interaction_advice, interaction.advice),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            MechanismLine(interaction)
            if (interaction.references.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.interaction_references, interaction.references.take(2).joinToString(";")),
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
        is CypInteraction -> stringResource(
            R.string.compare_mechanism_auc,
            interaction.cyp.displayName,  // CYP1A2 等是通用代码, 无需 i18n
            "%.1f".format(interaction.patientAdjustedFold),
            "%.1f".format(interaction.patientAdjustedFold)
        )
        // TODO(v0.9d done): 3 行改 R.string.interaction_qtc_format / interaction_acb_format / interaction_serotonin_format
        is QtcInteraction -> stringResource(R.string.interaction_qtc_format, interaction.finalEstimatedQtcMs)
        is AnticholinergicLoadInteraction -> stringResource(R.string.interaction_acb_format, interaction.totalScore)
        is SerotoninSyndromeRisk -> stringResource(R.string.interaction_serotonin_format, interaction.serotonergicDrugs.size)
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "v" + com.dosecare.app.BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) " · Debug" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(stringResource(R.string.about_app_id), style = MaterialTheme.typography.labelSmall)
                }
            }

            // v0.9a: 语言设置入口
            com.dosecare.app.ui.locale.LanguageSettingsCard()

            // v0.9f: 提醒设置入口
            com.dosecare.app.ui.reminder.ReminderSettingsCard()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_data_source), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_data_source_lines), style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_limitations), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_limitations_lines), style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_roadmap), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_roadmap_v04), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.settings_roadmap_v05), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.settings_roadmap_v10), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.settings_roadmap_v15), style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    stringResource(R.string.settings_disclaimer),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
