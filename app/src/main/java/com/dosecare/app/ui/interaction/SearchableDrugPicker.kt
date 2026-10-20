package com.dosecare.app.ui.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dosecare.app.R
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.displayNameFor
import com.dosecare.app.model.pharmacology.matchesNameQuery
import com.dosecare.app.ui.components.HazeTopAppBar
import com.dosecare.app.ui.components.pinnedTopAppBarScrollBehavior
import com.dosecare.app.ui.components.topAppBarScrollToTop
import com.dosecare.app.ui.pharmacology.currentUiLanguage
import com.dosecare.app.ui.pharmacology.displayName
import com.dosecare.app.ui.pharmacology.secondaryDisplayName
import kotlin.math.min

/**
 * 可搜索药物选择器:支持中文名/英文名/品牌名/拼音首字母模糊搜索
 *
 * 为什么不用 ExposedDropdownMenu:
 * 1. v0.4 124 药,普通 dropdown take(50) 截断,剩余不可点
 * 2. 124 药纯滚动难以快速找到目标药
 * 3. ExposedDropdownMenu 在大列表上性能差,焦点处理繁琐
 *
 * 设计:点击 chip/button 弹出全屏 Dialog,顶部搜索框实时过滤,下方 LazyColumn 列出所有匹配项。
 * 搜索规则:中文/英文/品牌名 substring + 拼音首字母 substring (e.g. "氟" -> 氟伏沙明, "C" -> Clozapine/Carbamazepine)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDrugPicker(
    label: String,
    selected: Drug?,
    options: List<Drug>,
    onSelect: (Drug) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_placeholder),
    prefilter: (Drug) -> Boolean = { true },
    chipColor: Color? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (chipColor != null) {
            CardDefaults.cardColors(containerColor = chipColor.copy(alpha = 0.10f))
        } else CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.search_drug_count, options.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { showDialog = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (selected != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(chipColor ?: MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        val primarySel = selected.displayName()
                        val secondarySel = selected.secondaryDisplayName()
                        val selText = when (currentUiLanguage()) {
                            "zh", "yue" -> stringResource(
                                R.string.drug_name_with_zh_subtitle,
                                primarySel,
                                secondarySel,
                            )
                            "ja" -> stringResource(
                                R.string.drug_name_with_ja_subtitle,
                                primarySel,
                                secondarySel,
                            )
                            else -> stringResource(
                                R.string.drug_name_with_en_subtitle,
                                primarySel,
                                secondarySel,
                            )
                        }
                        Text(
                            selText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 列表按当前界面语言的主名排序: 中文界面按中文名 (与底座一致),
    // 日文/英文界面按各自的通用名 —— 原来固定按中文名排, 在这两种界面下顺序是乱的。
    val language = currentUiLanguage()
    val sortedOptions = remember(options, language) {
        options.sortedBy { it.displayNameFor(language) }
    }

    if (showDialog) {
        SearchableDrugDialog(
            options = sortedOptions.filter(prefilter),
            onDismiss = { showDialog = false },
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            title = label
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableDrugDialog(
    options: List<Drug>,
    onDismiss: () -> Unit,
    onSelect: (Drug) -> Unit,
    title: String
) {
    var query by remember { mutableStateOf("") }
    // 搜索规则集中在 model 层 (Drug.matchesNameQuery): 中文名 / 日文名 / 英文名 /
    // 品牌名 / ATC 码 / 拼音首字母。
    val filtered = remember(query, options) {
        if (query.isBlank()) options else options.filter { it.matchesNameQuery(query) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                // Top bar
                // v0.9g merge: use the shared Haze wrapper so the picker chrome
                // honours the app-wide blur switch like every other screen.
                val listState = rememberLazyListState()
                val topAppBarState = rememberTopAppBarState()
                val scrollBehavior = pinnedTopAppBarScrollBehavior(
                    lazyListState = listState,
                    state = topAppBarState
                )
                HazeTopAppBar(
                    modifier = Modifier.topAppBarScrollToTop(scrollBehavior, listState),
                    title = { Text(stringResource(R.string.search_select_title, title)) },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                        }
                    }
                )
                // Search box
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_clear))
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                // Hint
                // TODO(v0.9d done): 改 R.string.search_count_with_query
                Text(
                    stringResource(R.string.search_filtered, filtered.size, options.size) +
                        if (query.isNotEmpty()) stringResource(R.string.search_count_with_query, query) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                // List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.search_no_match),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { drug ->
                            DrugListItem(drug = drug, onClick = { onSelect(drug) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrugListItem(drug: Drug, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    drug.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                // TODO(v0.9d done): 改 R.string.search_brand_prefix; drug name 按 locale 主名
                val primaryName = drug.secondaryDisplayName()
                Text(
                    "$primaryName · ${stringResource(drug.category.labelRes)}" +
                        if (drug.brandNames.isNotEmpty()) stringResource(R.string.search_brand_prefix, drug.brandNames.take(2).joinToString("/")) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (drug.therapeuticWindow != null) {
                // TODO(v0.9b): "TDM" 是国际通用医学缩写 (Therapeutic Drug Monitoring), 三语通用, 可保留
                AssistChip(
                    onClick = onClick,
                    label = { Text("TDM", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}


