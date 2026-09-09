package com.dosecare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.domain.catalog.Drug
import com.dosecare.app.domain.catalog.DrugCatalogService
import com.dosecare.app.domain.prescription.DoseTaken
import com.dosecare.app.domain.prescription.PrescribedDrug
import com.dosecare.app.domain.prescription.PrescriptionGroup
import com.dosecare.app.ui.theme.AccentColors
import com.dosecare.app.ui.theme.DarkModePref
import com.dosecare.app.ui.theme.ThemeController
import com.dosecare.app.ui.theme.ThemeState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val GROUP_COLORS = listOf(
    Color(0xFF1976D2), Color(0xFFD32F2F), Color(0xFF388E3C),
    Color(0xFF7B1FA2), Color(0xFFF57C00), Color(0xFF0097A7),
    Color(0xFFC2185B), Color(0xFF5D4037)
)

/** 临时用药组的固定名字, 用于 PlanRow 临时 badge 判定 + 自动创建时命名 */
const val TEMP_GROUP_NAME = "临时用药"

/**
 * v0.8a Phase F6: Tab 1 — 我的用药 (日历 + 用药计划)
 *
 * - 月历 (CalendarCompose 共享)
 * - 选中日下方: 该日所有用药计划 (按 frequencyPerDay 自动生成时段, 已打卡标绿)
 * - 顶部右上 + 按钮: 弹 BottomSheet — 选「新建分组」或「为已有分组加药」
 * - tap 一个 plan 时段: 打卡 (默认 now, 可改时间)
 *
 * v0.9a i18n: 已用 stringResource 替换大部分用户可见中文 (顶栏 title/actions, dialog 标题/按钮/标签,
 *            颜色选择器, 主题菜单, 删除整组确认等). 还遗留:
 *              - 临时组 badge "临时"
 *              - "还没有分组,点右上 + 创建" / "当日无安排用药" / "新增" / "为已有分组加药" / "📂 分组管理" 等空态文案
 *              - "X 药" / "${n}次/日" / "Xmg · Y/日" 等格式化标签
 *              - "已服" / "待服" 状态文本
 *              - "本组用药 (N)" / "(空) 点右上「加药」加入第一个药" 文本
 *              - "所属: X (单次, M月d日)" 文本
 *              - "⏰ 服药时间:" / "⏰ 用药时间 (点时钟改):" 标签
 *              - "使用现在" / "自定义" / "实际 HH:mm" 文本
 *              - TEMP_GROUP_NAME = "临时用药" (const val, 存数据库识别, 改需数据迁移)
 *              - SimpleDateFormat "M 月 d 日 EEE" 中文 pattern
 *            留待 v0.9b 父 agent 处理. 所有 TODO 标记的 strings.xml 缺失 key 详见行内注释.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarTab(
    catalog: DrugCatalogService,
    pendingReminder: kotlinx.coroutines.flow.StateFlow<com.dosecare.app.MainActivity.PendingReminder?>? = null,
    onPendingConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    // v0.9g: 从 SharedPreferences 版 PrescriptionRepository 切到 Room 版 (object PrescriptionViewModel 已注入)
    val groups by PrescriptionViewModel.groups.collectAsState()

    // 由 pendingReminder 触发的 checkInTarget, 用 stable ref 防重组丢失
    var pendingCheckInTarget by remember { mutableStateOf<CheckInTarget?>(null) }

    fun updateGroups(transform: (List<PrescriptionGroup>) -> List<PrescriptionGroup>) {
        PrescriptionViewModel.updateGroups(transform)
    }

    val selectedDate by CalendarViewModel.selectedDate.collectAsState()
    val currentMonth by CalendarViewModel.currentMonth.collectAsState()
    val dayEvents by CalendarViewModel.dayEvents.collectAsState()

    // v0.9f: 监听 pendingReminder (通知 tap 进来), 找到 drug + slot 弹打卡 dialog
    LaunchedEffect(pendingReminder) {
        if (pendingReminder == null) return@LaunchedEffect
        pendingReminder.collect { reminder ->
            if (reminder == null) return@collect
            // 找 prescribedDrug
            val groupWithDrug = groups.firstOrNull { g ->
                g.drugs.any { it.id == reminder.drugId }
            }
            val drug = groupWithDrug?.drugs?.firstOrNull { it.id == reminder.drugId }
            if (drug == null) {
                // 药已被删, 弹个 toast 提示
                android.widget.Toast.makeText(
                    context,
                    R.string.reminder_checkin_drug_not_found,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onPendingConsumed()
                return@collect
            }
            // 计算 slot 时间 (今天该 slot 的 epochMs)
            val parts = reminder.slotTime.split(":")
            if (parts.size != 2) { onPendingConsumed(); return@collect }
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1].toIntOrNull() ?: 0
            val slotCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val slotMs = slotCal.timeInMillis
            // 选当天
            CalendarViewModel.selectDate(CalendarViewModel.startOfDay(slotMs))
            // 弹 checkInTarget
            val drugEntity = try { catalog.getById(drug.drugId) } catch (e: Exception) { null }
            pendingCheckInTarget = CheckInTarget(
                id = "reminder_${reminder.drugId}_${reminder.slotTime}",
                groupId = groupWithDrug.id,
                groupName = groupWithDrug.name,
                groupColor = groupWithDrug.colorIndex,
                prescribedDrugId = drug.id,
                drugId = drug.drugId,
                drugName = drugEntity?.genericNameZh ?: drugEntity?.genericName ?: drug.drugId,
                doseMg = drug.defaultDoseMg,
                scheduledMillis = slotMs,
                isChecked = drug.dosesTaken.any {
                    kotlin.math.abs(it.timestamp - slotMs) < TimeUnit.HOURS.toMillis(6)
                }
            )
            onPendingConsumed()
        }
    }

    // 监听整月打卡, 算 dayEvents
    LaunchedEffect(currentMonth, groups) {
        val (y, m) = currentMonth
        val cal = Calendar.getInstance().apply { clear(); set(y, m, 1, 0, 0, 0) }
        val sinceMs = cal.timeInMillis
        val untilMs = sinceMs + TimeUnit.DAYS.toMillis(42)
        val medDays = mutableSetOf<Long>()
        groups.forEach { g ->
            g.drugs.forEach { d ->
                d.dosesTaken.forEach { dt ->
                    val t = dt.timestamp
                    if (t in sinceMs..untilMs) medDays.add(CalendarViewModel.startOfDay(t))
                }
            }
        }
        // 日记数据需要从 DiaryRepository 读 — 这里只 set 用药点, 日记 tab 自己 set 自己的
        val current = CalendarViewModel.dayEvents.value.toMutableMap()
        medDays.forEach { day ->
            val prev = current[day] ?: CalendarViewModel.DayEvents()
            current[day] = prev.copy(hasMed = true)
        }
        CalendarViewModel.setDayEvents(current)
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var newGroupDialog by remember { mutableStateOf(false) }
    var drugPickerForGroupId by remember { mutableStateOf<String?>(null) }
    var checkInTarget by remember { mutableStateOf<CheckInTarget?>(null) }
    var tempDrugDialog by remember { mutableStateOf(false) }
    var editDrugTarget by remember { mutableStateOf<EditDrugTarget?>(null) }
    var editGroupTarget by remember { mutableStateOf<EditGroupTarget?>(null) }
    var showThemeMenu by remember { mutableStateOf(false) }
    val expandedGroups by PrescriptionViewModel.expandedGroups.collectAsState()
    val infoDialogOpen by PrescriptionViewModel.infoDialogOpen.collectAsState()
    val themeState by ThemeController.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_title, groups.size)) },
                actions = {
                    // 主题切换 (右上角)
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = stringResource(R.string.calendar_theme),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        ThemeMenu(
                            expanded = showThemeMenu,
                            state = themeState,
                            onDismiss = { showThemeMenu = false }
                        )
                    }
                    // 新建分组/加药
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.calendar_new_group_icon))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            MonthCalendar(
                selectedDate = selectedDate,
                currentYear = currentMonth.year,
                currentMonth = currentMonth.month,
                dayEvents = dayEvents,
                onDateClick = { CalendarViewModel.selectDate(it) },
                onPrevMonth = { CalendarViewModel.goPrevMonth() },
                onNextMonth = { CalendarViewModel.goNextMonth() }
            )

            Spacer(Modifier.height(8.dp))

            // 选中日标题 + 数据校准说明 (右上 Info)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dayTitle = remember(selectedDate, locale) {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    when (locale.language) {
                        "ja" -> SimpleDateFormat("M 月 d 日 EEE", Locale.JAPANESE).format(cal.time)
                        "zh" -> SimpleDateFormat("M 月 d 日 EEE", Locale.SIMPLIFIED_CHINESE).format(cal.time)
                        else -> {
                            val month = cal.get(Calendar.MONTH) + 1
                            val day = cal.get(Calendar.DAY_OF_MONTH)
                            val wd = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
                            val weekday = when (wd) {
                                Calendar.MONDAY -> "Mon"
                                Calendar.TUESDAY -> "Tue"
                                Calendar.WEDNESDAY -> "Wed"
                                Calendar.THURSDAY -> "Thu"
                                Calendar.FRIDAY -> "Fri"
                                Calendar.SATURDAY -> "Sat"
                                else -> "Sun"
                            }
                            String.format(Locale.ENGLISH, "%s %d, %s", Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), day, weekday)
                        }
                    }
                }
                Text(
                    text = "💊 $dayTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { PrescriptionViewModel.setInfoDialogOpen(true) }) {
                    Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.calendar_calibration), tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_no_groups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 选中日所有 plan
                val plans = remember(groups, selectedDate, catalog) {
                    buildDayPlans(groups, selectedDate, catalog)
                }
                if (plans.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_no_plans),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plans.forEach { plan ->
                            PlanRow(
                                plan = plan,
                                onTap = { checkInTarget = plan },
                                onLongPressEdit = {
                                    // 长按 → 编辑/删除药 (含撤销今日打卡)
                                    val group = groups.firstOrNull { it.id == plan.groupId }
                                    val drug = group?.drugs?.firstOrNull { it.id == plan.prescribedDrugId }
                                    if (group != null && drug != null) {
                                        editDrugTarget = EditDrugTarget(
                                            groupId = group.id,
                                            prescribedDrugId = drug.id,
                                            scheduledMillis = plan.scheduledMillis
                                        )
                                    }
                                },
                                onLongPressDelete = {
                                    // 撤销一次打卡: 找该 plan 的最近一次 dose_taken,删除
                                    val ts = plan.scheduledMillis
                                    scope.launch {
                                        updateGroups { gs ->
                                            gs.map { g ->
                                                g.copy(drugs = g.drugs.map { d ->
                                                    if (d.id != plan.prescribedDrugId) d
                                                    else d.copy(dosesTaken = d.dosesTaken.filterNot {
                                                        it.timestamp == ts
                                                    })
                                                })
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // 折叠的分组列表 (在 plan 下方)
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(4.dp))
                // TODO(v0.9d done): 改 R.string.groups_manage
                Text(
                    text = stringResource(R.string.groups_manage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                groups.forEach { g ->
                    val isOpen = g.id in expandedGroups
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .combinedClickable(
                                onClick = { PrescriptionViewModel.toggleGroup(g.id) },
                                onLongClick = { editGroupTarget = EditGroupTarget(g.id) }
                            ),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(GROUP_COLORS[g.colorIndex % GROUP_COLORS.size])
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = g.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.calendar_n_drugs, g.drugs.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                    if (isOpen) {
                        g.drugs.forEach { pd ->
                            PrescribedDrugRow(
                                pd = pd,
                                catalog = catalog,
                                onDelete = {
                                    scope.launch {
                                        updateGroups { gs ->
                                            gs.map { gg ->
                                                if (gg.id == g.id) gg.copy(drugs = gg.drugs.filter { it.id != pd.id })
                                                else gg
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        // 加药按钮
                        OutlinedButton(
                            onClick = { drugPickerForGroupId = g.id },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_add_drug_to, g.name))
                        }
                    }
                }
            }
        }
    }

    // 弹层 1: + 按钮的 BottomSheet
    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                // TODO(v0.9d done): 改 R.string.action_new
                Text(
                    text = stringResource(R.string.action_new),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.calendar_new_group)) },
                    supportingContent = { Text(stringResource(R.string.calendar_new_group_hint)) },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        newGroupDialog = true
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.calendar_temp_drug)) },
                    supportingContent = { Text(stringResource(R.string.calendar_temp_drug_hint)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        tempDrugDialog = true
                    }
                )
                if (groups.isNotEmpty()) {
                    HorizontalDivider()
                    // TODO(v0.9d done): 改 R.string.add_to_existing_group
                    Text(
                        text = stringResource(R.string.add_to_existing_group),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    groups.forEach { g ->
                        ListItem(
                            headlineContent = { Text(g.name) },
                            supportingContent = { Text(stringResource(R.string.calendar_n_drugs, g.drugs.size)) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(GROUP_COLORS[g.colorIndex % GROUP_COLORS.size])
                                )
                            },
                            modifier = Modifier.clickable {
                                showAddSheet = false
                                drugPickerForGroupId = g.id
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // 弹层 2: 新建分组 dialog
    if (newGroupDialog) {
        NewGroupDialog(
            existingCount = groups.size,
            onDismiss = { newGroupDialog = false },
            onCreate = { name, color ->
                val newGroup = PrescriptionGroup(
                    name = name,
                    colorIndex = color
                )
                scope.launch {
                    updateGroups { it + newGroup }
                    newGroupDialog = false
                }
            }
        )
    }

    // 弹层 3: 选药 + 设定频率 (group drug)
    val groupForPicker = drugPickerForGroupId?.let { id -> groups.firstOrNull { it.id == id } }
    if (drugPickerForGroupId != null && groupForPicker != null) {
        AddDrugDialog(
            group = groupForPicker,
            catalog = catalog,
            targetDate = null,
            onDismiss = { drugPickerForGroupId = null },
            onAdd = { drugId, doseMg, freqPerDay, times, _ ->
                val newPd = PrescribedDrug(
                    drugId = drugId,
                    defaultDoseMg = doseMg,
                    frequencyPerDay = freqPerDay,
                    times = times,
                    startDate = System.currentTimeMillis(),
                    targetDate = null
                )
                scope.launch {
                    updateGroups { gs ->
                        gs.map { g ->
                            if (g.id == groupForPicker.id) g.copy(drugs = g.drugs + newPd)
                            else g
                        }
                    }
                    drugPickerForGroupId = null
                }
            }
        )
    }

    // 弹层 6: 临时用药 (单次, 自动创建/复用「临时用药」组)
    if (tempDrugDialog) {
        AddDrugDialog(
            group = PrescriptionGroup(name = TEMP_GROUP_NAME, colorIndex = 4),  // 橙色 (4 = 0xFFF57C00)
            catalog = catalog,
            targetDate = CalendarViewModel.startOfDay(selectedDate),
            onDismiss = { tempDrugDialog = false },
            onAdd = { drugId, doseMg, freqPerDay, times, tDate ->
                val finalTargetDate = tDate ?: CalendarViewModel.startOfDay(selectedDate)
                val newPd = PrescribedDrug(
                    drugId = drugId,
                    defaultDoseMg = doseMg,
                    frequencyPerDay = freqPerDay,
                    times = times,
                    startDate = finalTargetDate,
                    targetDate = finalTargetDate
                )
                scope.launch {
                    updateGroups { gs ->
                        val tempGroup = gs.firstOrNull { it.name == TEMP_GROUP_NAME }
                        if (tempGroup != null) {
                            gs.map { g ->
                                if (g.id == tempGroup.id) g.copy(drugs = g.drugs + newPd)
                                else g
                            }
                        } else {
                            // 自动创建「临时用药」组 (橙色)
                            gs + PrescriptionGroup(
                                name = TEMP_GROUP_NAME,
                                colorIndex = 4,  // GROUP_COLORS 索引 4 = orange #F57C00
                                drugs = listOf(newPd)
                            )
                        }
                    }
                    tempDrugDialog = false
                }
            }
        )
    }

    // 弹层 7: 长按 plan → 编辑/删除用药
    editDrugTarget?.let { target ->
        val group = groups.firstOrNull { it.id == target.groupId }
        val drug = group?.drugs?.firstOrNull { it.id == target.prescribedDrugId }
        if (group != null && drug != null) {
            EditDrugDialog(
                group = group,
                drug = drug,
                catalog = catalog,
                scheduledMillis = target.scheduledMillis,
                onDismiss = { editDrugTarget = null },
                onSave = { newDoseMg, newFreq, newTimes ->
                    scope.launch {
                        updateGroups { gs ->
                            gs.map { g ->
                                if (g.id == group.id) g.copy(drugs = g.drugs.map { d ->
                                    if (d.id == drug.id) d.copy(
                                        defaultDoseMg = newDoseMg,
                                        frequencyPerDay = newFreq,
                                        times = newTimes
                                    ) else d
                                }) else g
                            }
                        }
                        editDrugTarget = null
                    }
                },
                onDelete = {
                    scope.launch {
                        updateGroups { gs ->
                            gs.map { g ->
                                if (g.id == group.id) g.copy(drugs = g.drugs.filter { it.id != drug.id })
                                else g
                            }
                        }
                        editDrugTarget = null
                    }
                },
                onUndoTodayCheckIn = {
                    // 撤销今天 slot 时刻的最近一次打卡
                    scope.launch {
                        updateGroups { gs ->
                            gs.map { g ->
                                if (g.id == group.id) g.copy(drugs = g.drugs.map { d ->
                                    if (d.id == drug.id) d.copy(
                                        dosesTaken = d.dosesTaken.filterNot {
                                            kotlin.math.abs(it.timestamp - target.scheduledMillis) < TimeUnit.HOURS.toMillis(6)
                                        }
                                    ) else d
                                }) else g
                            }
                        }
                    }
                }
            )
        } else {
            // 找不到 drug (可能已被删) — 关掉 dialog
            editDrugTarget = null
        }
    }

    // 弹层 8: 长按分组 → 编辑/删除分组 (含组内用药管理)
    editGroupTarget?.let { target ->
        val group = groups.firstOrNull { it.id == target.groupId }
        if (group != null) {
            EditGroupDialog(
                group = group,
                catalog = catalog,
                onDismiss = { editGroupTarget = null },
                onSave = { newName, newColor ->
                    scope.launch {
                        updateGroups { gs ->
                            gs.map { g ->
                                if (g.id == group.id) g.copy(name = newName, colorIndex = newColor)
                                else g
                            }
                        }
                        editGroupTarget = null
                    }
                },
                onDelete = {
                    scope.launch {
                        updateGroups { gs ->
                            gs.filter { it.id != group.id }
                        }
                        editGroupTarget = null
                    }
                },
                onRemoveDrug = { drugId ->
                    scope.launch {
                        updateGroups { gs ->
                            gs.map { g ->
                                if (g.id == group.id) g.copy(drugs = g.drugs.filter { it.id != drugId })
                                else g
                            }
                        }
                    }
                },
                onAddDrug = { drugId, doseMg, freqPerDay, times ->
                    val newPd = PrescribedDrug(
                        drugId = drugId,
                        defaultDoseMg = doseMg,
                        frequencyPerDay = freqPerDay,
                        times = times,
                        startDate = System.currentTimeMillis(),
                        targetDate = null
                    )
                    scope.launch {
                        updateGroups { gs ->
                            gs.map { g ->
                                if (g.id == group.id) g.copy(drugs = g.drugs + newPd)
                                else g
                            }
                        }
                    }
                }
            )
        } else {
            // 找不到 group — 关掉 dialog
            editGroupTarget = null
        }
    }

    // 弹层 4: 打卡 dialog (v0.9f: pendingCheckInTarget 优先于 checkInTarget)
    val activeCheckInTarget = pendingCheckInTarget ?: checkInTarget
    activeCheckInTarget?.let { target ->
        // 如果是 pending 触发, 不在 dismiss 时清 pendingCheckInTarget
        val onDismiss = if (target === pendingCheckInTarget) {
            { pendingCheckInTarget = null }
        } else {
            { checkInTarget = null }
        }
        CheckInDialog(
            plan = target,
            catalog = catalog,
            onDismiss = onDismiss,
            onConfirm = { userTs ->
                scope.launch {
                    // bug fix: 始终以 slot 时间作为打卡时间,否则 buildDayPlans 的 6h 匹配窗口
                    // 在用户 "现在打卡" 跟 slot 时间偏差 > 6h 时, 仍会显示待服
                    val doseTs = target.scheduledMillis
                    // 自定义时间仅作为 note 保留
                    val note = if (kotlin.math.abs(userTs - doseTs) > TimeUnit.MINUTES.toMillis(1)) {
                        val noteFmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val timeStr = noteFmt.format(java.util.Date(userTs))
                        context.getString(R.string.checkin_actual_time, timeStr)
                    } else null
                    updateGroups { gs ->
                        gs.map { g ->
                            g.copy(drugs = g.drugs.map { d ->
                                if (d.id != target.prescribedDrugId) d
                                else d.copy(
                                    dosesTaken = d.dosesTaken + DoseTaken(
                                        timestamp = doseTs,
                                        doseMg = target.doseMg,
                                        note = note
                                    )
                                )
                            })
                        }
                    }
                    checkInTarget = null
                }
            }
        )
    }

    // 弹层 5: 数据校准说明
    if (infoDialogOpen) {
        AlertDialog(
            onDismissRequest = { PrescriptionViewModel.setInfoDialogOpen(false) },
            title = { Text(stringResource(R.string.calendar_calibration_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.calendar_calibration_lines), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { PrescriptionViewModel.setInfoDialogOpen(false) }) { Text(stringResource(R.string.common_known)) }
            }
        )
    }
}

/**
 * 一个用药计划 = 某药在选中的某天的某个时段
 * (按 frequencyPerDay 切: 1→08, 2→08/20, 3→08/14/20, 4→08/12/18/22)
 * 如果该时段已打卡 (timestamp 落在 [slot-1h, slot+1h]) 视为已服
 */
data class CheckInTarget(
    val id: String,
    val groupId: String,
    val groupName: String,
    val groupColor: Int,
    val prescribedDrugId: String,
    val drugId: String,
    val drugName: String,
    val doseMg: Double,
    val scheduledMillis: Long,   // 该时段应服时间
    val isChecked: Boolean       // 是否已打卡
)

/**
 * 长按 plan 触发的编辑/删除目标
 */
data class EditDrugTarget(
    val groupId: String,
    val prescribedDrugId: String,
    val scheduledMillis: Long  // 用于"撤销今日打卡"匹配
)

/**
 * 长按 分组 触发的编辑/删除目标 (只存 groupId)
 */
data class EditGroupTarget(
    val groupId: String
)

private fun defaultTimeSlots(freq: Int): List<Int> = when (freq.coerceIn(1, 4)) {
    1 -> listOf(8)
    2 -> listOf(8, 20)
    3 -> listOf(8, 14, 20)
    else -> listOf(8, 12, 18, 22)
}

/** 把 "HH:mm" 字符串解析成 epoch ms,落在指定日期 0:00 之后 */
private fun parseTimeOnDay(dayStart: Long, hhmm: String): Long? {
    val parts = hhmm.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return Calendar.getInstance().apply {
        timeInMillis = dayStart
        set(Calendar.HOUR_OF_DAY, h)
        set(Calendar.MINUTE, m)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** 取该药的时段: 有自定义 times 用自定义, 否则按 freq 默认 */
private fun resolveSlots(d: PrescribedDrug): List<Pair<Int, Int>> {
    val custom = d.times
        .mapNotNull { parseTimeOnDay(0, it) }
        .map { ts ->
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
        }
    if (custom.isNotEmpty()) return custom
    return defaultTimeSlots(d.frequencyPerDay).map { it to 0 }
}

private fun buildDayPlans(groups: List<PrescriptionGroup>, selectedDayStart: Long, catalog: DrugCatalogService): List<CheckInTarget> {
    val result = mutableListOf<CheckInTarget>()
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDayStart
    }
    val dayStart = cal.timeInMillis
    groups.forEach { g ->
        g.drugs.forEach { d ->
            // 临时用药: 只在 targetDate 那天显示
            if (d.targetDate != null && d.targetDate != dayStart) return@forEach
            val slots = resolveSlots(d)
            // 查中文名, fallback 到英文 drugId (旧数据/未知药)
            val displayName = catalog.tryGetById(d.drugId)?.genericNameZh ?: d.drugId
            slots.forEachIndexed { idx, (hour, minute) ->
                val ts = Calendar.getInstance().apply {
                    timeInMillis = dayStart
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                // 匹配窗口 6h: 同一天内 ±6h 视为同一 slot 的打卡
                val checked = d.dosesTaken.any { kotlin.math.abs(it.timestamp - ts) < TimeUnit.HOURS.toMillis(6) }
                result.add(
                    CheckInTarget(
                        id = "${d.id}_$idx",
                        groupId = g.id,
                        groupName = g.name,
                        groupColor = g.colorIndex,
                        prescribedDrugId = d.id,
                        drugId = d.drugId,
                        drugName = displayName,
                        doseMg = d.defaultDoseMg,
                        scheduledMillis = ts,
                        isChecked = checked
                    )
                )
            }
        }
    }
    return result.sortedBy { it.scheduledMillis }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanRow(
    plan: CheckInTarget,
    onTap: () -> Unit,
    onLongPressEdit: () -> Unit,
    onLongPressDelete: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.CHINA) }
    val color = GROUP_COLORS[plan.groupColor % GROUP_COLORS.size]
    val isTemp = plan.groupName == TEMP_GROUP_NAME
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPressEdit
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isChecked)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (plan.isChecked) MaterialTheme.colorScheme.primary else color),
                contentAlignment = Alignment.Center
            ) {
                if (plan.isChecked) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.calendar_taken),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = timeFmt.format(Date(plan.scheduledMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plan.drugName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isTemp) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            // TODO(v0.9b): 新增 strings.xml key (临时 badge)
                            Text(
                                text = "临时",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                // TODO(v0.9b): 新增 strings.xml key (groupName · doseMg mg · 已服/待服)
                Text(
                    text = "${plan.groupName} · ${plan.doseMg} mg" +
                            if (plan.isChecked) " · 已服" else " · 待服",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (plan.isChecked) {
                IconButton(onClick = onLongPressDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.calendar_undo_checkin))
                }
            }
        }
    }
}

@Composable
private fun PrescribedDrugRow(
    pd: PrescribedDrug,
    catalog: DrugCatalogService,
    onDelete: () -> Unit
) {
    val drug = remember(pd.drugId) { catalog.tryGetById(pd.drugId) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = drug?.genericNameZh ?: pd.drugId,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        // TODO(v0.9b): 新增 strings.xml key (Xmg · Y/日)
        Text(
            text = "${pd.defaultDoseMg}mg · ${pd.frequencyPerDay}/日",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewGroupDialog(
    existingCount: Int,
    onDismiss: () -> Unit,
    onCreate: (name: String, color: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(existingCount % GROUP_COLORS.size) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_new_group)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.calendar_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.calendar_color_label), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GROUP_COLORS.forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = i }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), color) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDrugDialog(
    group: PrescriptionGroup,
    catalog: DrugCatalogService,
    targetDate: Long? = null,
    onDismiss: () -> Unit,
    onAdd: (drugId: String, doseMg: Double, freqPerDay: Int, times: List<String>, targetDate: Long?) -> Unit
) {
    val isTemp = targetDate != null
    var searchQuery by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Drug?>(null) }
    var doseText by remember { mutableStateOf(if (isTemp) "200" else "100") }
    // 临时用药 freq 固定 1, 不让用户改
    var freq by remember { mutableStateOf(1) }
    // 自定义时段: list of "HH:mm" 字符串, 长度 = freq
    // 临时用药: 默认时间 = 当前时间 + 30 分钟 (向上取整到 5 分钟)
    var times by remember {
        mutableStateOf(
            if (isTemp) {
                val now = System.currentTimeMillis()
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
                cal.add(java.util.Calendar.MINUTE, 30)
                val m = ((cal.get(java.util.Calendar.MINUTE) / 5) + 1) * 5
                if (m >= 60) {
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
                } else {
                    cal.set(java.util.Calendar.MINUTE, m)
                }
                listOf(String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE)))
            } else {
                defaultTimeSlots(1).map { String.format("%02d:00", it) }
            }
        )
    }
    val allDrugs = remember { catalog.all().sortedBy { it.genericNameZh } }
    // v0.8b H1 修复: AddDrugDialog 拼音首字母搜索 (跟 SearchableDrugPicker 一致)
    // 5 维匹配: 中文 / 英文 / 品牌 / ATC / 中文 → 拼音首字母
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) allDrugs
        else {
            val q = searchQuery.trim()
            val ql = q.lowercase()
            val isPinyinInitialsQuery = q.isNotEmpty() && q.all { it in 'a'..'z' || it in 'A'..'Z' }
            allDrugs.filter { d ->
                d.genericNameZh.contains(q, ignoreCase = true) ||
                d.genericName.lowercase().contains(ql) ||
                d.brandNames.any { it.lowercase().contains(ql) } ||
                d.atc?.lowercase()?.contains(ql) == true ||
                (isPinyinInitialsQuery && toPinyinInitials(d.genericNameZh).lowercase().contains(ql))
            }
        }
    }

    // freq 变化时重置 times (保留已有, 不足补默认)
    LaunchedEffect(freq) {
        val defaults = defaultTimeSlots(freq).map { String.format("%02d:00", it) }
        times = List(freq) { i -> times.getOrNull(i) ?: defaults.getOrNull(i) ?: "08:00" }
    }

    var pickerForSlot by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isTemp) {
                    val dateFmt = remember { java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA) }
                    // TODO(v0.9b): 新增 strings.xml key (临时用药 · M月d日)
                    "临时用药 · ${dateFmt.format(java.util.Date(targetDate!!))}"
                } else stringResource(R.string.calendar_add_drug_to, group.name)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                if (selected == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.calendar_pinyin_hint)) },
                        placeholder = { Text(stringResource(R.string.calendar_pinyin_example)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered, key = { it.id }) { d ->
                            ListItem(
                                headlineContent = { Text(d.genericNameZh) },
                                supportingContent = { Text(d.genericName) },
                                modifier = Modifier.clickable { selected = d }
                            )
                            HorizontalDivider()
                        }
                    }
                } else {
                    Text(stringResource(R.string.calendar_selected_drug, selected!!.genericNameZh, selected!!.genericName))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = doseText,
                        onValueChange = { doseText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.calendar_dose_mg)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!isTemp) {
                        Text(stringResource(R.string.calendar_freq_per_day, freq), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 4).forEach { n ->
                                FilterChip(
                                    selected = freq == n,
                                    onClick = { freq = n },
                                    label = { Text(stringResource(R.string.calendar_n_per_day, n)) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // TODO(v0.9b): 新增 strings.xml key (⏰ 服药时间: / ⏰ 用药时间 (点时钟改):)
                    Text(
                        text = if (isTemp) "⏰ 服药时间:" else "⏰ 用药时间 (点时钟改):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        times.forEachIndexed { idx, t ->
                            OutlinedButton(
                                onClick = { pickerForSlot = idx },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(t, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { selected = null }) { Text(stringResource(R.string.calendar_reselect_drug)) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val d = selected ?: return@TextButton
                    val dose = doseText.toDoubleOrNull() ?: 100.0
                    onAdd(d.id, dose, freq, times, targetDate)
                },
                enabled = selected != null
            ) { Text(if (isTemp) stringResource(R.string.calendar_add_drug) else stringResource(R.string.calendar_add_drug)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )

    // 选 slot 时间弹的 TimePicker
    pickerForSlot?.let { slotIdx ->
        val cur = times.getOrNull(slotIdx) ?: "08:00"
        val (curH, curM) = cur.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
        val timeState = rememberTimePickerState(initialHour = curH, initialMinute = curM, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickerForSlot = null },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = String.format("%02d:%02d", timeState.hour, timeState.minute)
                    times = times.toMutableList().also { it[slotIdx] = newTime }
                    pickerForSlot = null
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickerForSlot = null }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.calendar_slot_time, slotIdx + 1)) },
            text = { TimePicker(state = timeState) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInDialog(
    plan: CheckInTarget,
    catalog: DrugCatalogService,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val drug = remember(plan.drugId) { catalog.tryGetById(plan.drugId) }
    var customTimeEnabled by remember { mutableStateOf(false) }
    val initialTime = System.currentTimeMillis()
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }
    val hourFmt = remember { SimpleDateFormat("HH:mm", Locale.CHINA) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableStateOf(CalendarViewModel.startOfDay(initialTime)) }
    var hourMin by remember { mutableStateOf(initialTime % TimeUnit.DAYS.toMillis(1)) }
    val scheduledFmt = remember { SimpleDateFormat("HH:mm", Locale.CHINA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_checkin, drug?.genericNameZh ?: plan.drugName)) },
        text = {
            Column {
                Text(stringResource(R.string.calendar_scheduled_time, scheduledFmt.format(Date(plan.scheduledMillis))), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.calendar_dose_label, plan.doseMg.toInt()), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.calendar_taken_time), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { customTimeEnabled = !customTimeEnabled }) {
                        // TODO(v0.9b): 新增 strings.xml key (使用现在 / 自定义)
                        Text(if (customTimeEnabled) "使用现在" else "自定义")
                    }
                }
                if (customTimeEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) { Text(dateFmt.format(Date(dateMillis))) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) { Text(hourFmt.format(Date(hourMin))) }
                    }
                } else {
                    Text(stringResource(R.string.calendar_now, timeFmt.format(Date())), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ts = if (customTimeEnabled) dateMillis + hourMin else System.currentTimeMillis()
                onConfirm(ts)
            }) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis = dateState.selectedDateMillis ?: dateMillis
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = dateState) }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = Calendar.getInstance().apply { timeInMillis = hourMin }.get(Calendar.HOUR_OF_DAY),
            initialMinute = Calendar.getInstance().apply { timeInMillis = hourMin }.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    hourMin = cal.timeInMillis
                    showTimePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) } },
            text = { TimePicker(state = timeState) }
        )
    }
}

/**
 * 长按 plan 后弹出的编辑/删除用药 dialog
 *
 * - 顶部: 药名 + 所属组 + 临时用药 badge (如适用)
 * - 剂量: TextField
 * - 每日次数: 1/2/3/4 FilterChip
 * - 用药时间: N 个 OutlinedButton, 点开 TimePicker
 * - 底部按钮: [撤销今日打卡] (如有) | [删除这条药] (红) | [保存] [取消]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDrugDialog(
    group: PrescriptionGroup,
    drug: PrescribedDrug,
    catalog: DrugCatalogService,
    scheduledMillis: Long,
    onDismiss: () -> Unit,
    onSave: (doseMg: Double, freq: Int, times: List<String>) -> Unit,
    onDelete: () -> Unit,
    onUndoTodayCheckIn: () -> Unit
) {
    val drugInfo = remember(drug.drugId) { catalog.tryGetById(drug.drugId) }
    val isTemp = drug.targetDate != null
    val isTempGroup = group.name == TEMP_GROUP_NAME

    // 初始值
    var doseText by remember { mutableStateOf(drug.defaultDoseMg.toString()) }
    var freq by remember { mutableStateOf(drug.frequencyPerDay.coerceIn(1, 4)) }
    var times by remember {
        mutableStateOf(
            if (drug.times.isNotEmpty()) drug.times
            else defaultTimeSlots(drug.frequencyPerDay).map { String.format("%02d:00", it) }
        )
    }
    var pickerForSlot by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 是否有该 slot 的今日打卡
    val hasCheckIn = drug.dosesTaken.any {
        kotlin.math.abs(it.timestamp - scheduledMillis) < TimeUnit.HOURS.toMillis(6)
    }

    LaunchedEffect(freq) {
        val defaults = defaultTimeSlots(freq).map { String.format("%02d:00", it) }
        times = List(freq) { i -> times.getOrNull(i) ?: defaults.getOrNull(i) ?: "08:00" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = drugInfo?.genericNameZh ?: drug.drugId,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isTemp || isTempGroup) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        // TODO(v0.9b): 新增 strings.xml key (临时 badge)
                        Text(
                            text = "临时",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column {
                // TODO(v0.9b): 新增 strings.xml key (所属: X (单次, M月d日))
                Text(
                    text = "所属: ${group.name}${if (isTemp) "  (单次, ${drug.targetDate?.let { SimpleDateFormat("M月d日", Locale.CHINA).format(Date(it)) } ?: ""})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = doseText,
                    onValueChange = { doseText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.calendar_dose_mg)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isTemp) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.calendar_freq_per_day, freq), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 4).forEach { n ->
                            FilterChip(
                                selected = freq == n,
                                onClick = { freq = n },
                                label = { Text(stringResource(R.string.calendar_n_per_day, n)) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // TODO(v0.9b): 新增 strings.xml key (⏰ 服药时间: / ⏰ 用药时间 (点时钟改):)
                Text(
                    text = if (isTemp) "⏰ 服药时间:" else "⏰ 用药时间 (点时钟改):",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    times.forEachIndexed { idx, t ->
                        OutlinedButton(
                            onClick = { pickerForSlot = idx },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(t, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (hasCheckIn) {
                    OutlinedButton(
                        onClick = onUndoTodayCheckIn,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_undo_today, SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(scheduledMillis))))
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dose = doseText.toDoubleOrNull() ?: drug.defaultDoseMg
                    onSave(dose, freq, times)
                }
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    )

    // TimePicker
    pickerForSlot?.let { slotIdx ->
        val cur = times.getOrNull(slotIdx) ?: "08:00"
        val (curH, curM) = cur.split(":").let {
            (it.getOrNull(0)?.toIntOrNull() ?: 8) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        val timeState = rememberTimePickerState(initialHour = curH, initialMinute = curM, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickerForSlot = null },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = String.format("%02d:%02d", timeState.hour, timeState.minute)
                    times = times.toMutableList().also { it[slotIdx] = newTime }
                    pickerForSlot = null
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickerForSlot = null }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.calendar_slot_time, slotIdx + 1)) },
            text = { TimePicker(state = timeState) }
        )
    }

    // 删除确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.calendar_delete_drug)) },
            text = {
                Text(stringResource(R.string.calendar_delete_drug_confirm, drugInfo?.genericNameZh ?: drug.drugId))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

/**
 * 主题切换 DropdownMenu (右上角调色板图标)
 *
 * - "主题模式" section: 3 RadioButton (跟随系统 / 亮色 / 暗色)
 * - "主题色" section: 5 个圆点 (浅蓝/暖橙/墨绿/玫瑰红/深紫), 选中带勾
 *
 * 改动会立刻调 ThemeController.setXxx, 落盘到 SharedPreferences, 触发重组
 */
@Composable
private fun ThemeMenu(
    expanded: Boolean,
    state: ThemeState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showColorPicker by remember { mutableStateOf(false) }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.theme_mode_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        DarkModePref.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(stringResource(mode.displayNameRes)) },
                leadingIcon = {
                    if (state.darkMode == mode) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(Modifier.width(24.dp))
                    }
                },
                onClick = {
                    ThemeController.setDarkMode(context, mode)
                    onDismiss()
                }
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            stringResource(R.string.theme_color_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentColors.all.forEachIndexed { idx, accent ->
                val isSelected = state.accentIndex == idx && state.customColor == null
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.lightPrimary)
                        .clickable {
                            ThemeController.setAccent(context, idx)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.theme_accent_selected, stringResource(accent.nameRes)),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            // 自定义色块(若有)
            if (state.customColor != null) {
                val custom = state.customColor
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(custom)
                        .clickable { showColorPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.theme_custom_selected),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // 清除自定义色 → 回到当前 accentIndex 预设
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            ThemeController.clearCustomColor(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.theme_clear_custom),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            // "+" 圆点 → 打开 ColorPicker
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showColorPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.theme_custom_color),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initial = state.customColor ?: AccentColors.all[state.accentIndex].lightPrimary,
            onDismiss = { showColorPicker = false },
            onConfirm = { c ->
                ThemeController.setCustomColor(context, c)
                showColorPicker = false
                onDismiss()
            }
        )
    }
}

/**
 * 长按 分组 后弹出的编辑/删除分组 dialog
 *
 * - 名称: TextField
 * - 颜色: 8 个圆点选择
 * - 本组用药 (N): 列表, 每个药右侧有删除按钮, 底部 + 加药
 * - 底部按钮: [删除整组](红) | [取消] | [保存]
 *
 * 加药走嵌套 AddDrugDialog (targetDate=null, group 固定)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGroupDialog(
    group: PrescriptionGroup,
    catalog: DrugCatalogService,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int) -> Unit,
    onDelete: () -> Unit,
    onRemoveDrug: (drugId: String) -> Unit,
    onAddDrug: (drugId: String, doseMg: Double, freqPerDay: Int, times: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var color by remember { mutableStateOf(group.colorIndex) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddDrug by remember { mutableStateOf(false) }
    val isTemp = group.name == TEMP_GROUP_NAME

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.calendar_edit_group), style = MaterialTheme.typography.titleMedium)
                if (isTemp) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        // TODO(v0.9b): 新增 strings.xml key (临时 badge)
                        Text(
                            text = "临时",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.calendar_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.calendar_color_label), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GROUP_COLORS.forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { color = i }
                        ) {
                            if (color == i) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(R.string.calendar_selected),
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // TODO(v0.9b): 新增 strings.xml key (本组用药 (N))
                    Text(
                        text = "本组用药 (${group.drugs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showAddDrug = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.calendar_add_drug))
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (group.drugs.isEmpty()) {
                    // TODO(v0.9b): 新增 strings.xml key ((空) 点右上「加药」加入第一个药)
                    Text(
                        text = "(空) 点右上「加药」加入第一个药",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        group.drugs.forEach { pd ->
                            val drugInfo = remember(pd.drugId) { catalog.tryGetById(pd.drugId) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = drugInfo?.genericNameZh ?: pd.drugId,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                // TODO(v0.9b): 新增 strings.xml key (Xmg · Y/日)
                                Text(
                                    text = "${pd.defaultDoseMg}mg · ${pd.frequencyPerDay}/日",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { onRemoveDrug(pd.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.common_delete),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim().ifBlank { group.name }, color) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text(stringResource(R.string.calendar_delete_group), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        }
    )

    // 嵌套: 加药 dialog
    if (showAddDrug) {
        AddDrugDialog(
            group = group,
            catalog = catalog,
            targetDate = null,
            onDismiss = { showAddDrug = false },
            onAdd = { drugId, doseMg, freqPerDay, times, _ ->
                onAddDrug(drugId, doseMg, freqPerDay, times)
                showAddDrug = false
            }
        )
    }

    // 删除整组确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.calendar_delete_group)) },
            text = {
                Text(stringResource(R.string.calendar_delete_group_confirm, group.name, group.drugs.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.calendar_delete_group), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}
