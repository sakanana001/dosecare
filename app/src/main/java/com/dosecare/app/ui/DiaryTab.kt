package com.dosecare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dosecare.app.R
import com.dosecare.app.data.db.DiaryEntryEntity
import com.dosecare.app.data.db.DiaryMood
import com.dosecare.app.data.repository.DiaryRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * v0.8a Phase F5: 日记 Tab
 *
 * - 共享日历 (CalendarViewModel)
 * - 选中日下方: 该日所有日记 (mood emoji + 时间 + text)
 * - 无日记时显示空态文案, 不显示日记 section
 * - 右上 + 按钮: 新建 (mood 下拉 + 文本框 + 时间戳, 默认系统时间可改)
 */

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiaryEntryPoint {
    fun diaryRepository(): DiaryRepository
}

private fun resolveDiaryRepo(context: android.content.Context): DiaryRepository {
    val app = context.applicationContext
    return EntryPointAccessors.fromApplication(app, DiaryEntryPoint::class.java).diaryRepository()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryTab() {
    val context = LocalContext.current
    val repo = remember(context) { resolveDiaryRepo(context) }
    val scope = rememberCoroutineScope()

    val selectedDate by CalendarViewModel.selectedDate.collectAsState()
    val currentMonth by CalendarViewModel.currentMonth.collectAsState()
    val dayEvents by CalendarViewModel.dayEvents.collectAsState()

    // 监听整月日记, 算 dayEvents
    LaunchedEffect(currentMonth) {
        val (y, m) = currentMonth
        val cal = Calendar.getInstance().apply { clear(); set(y, m, 1, 0, 0, 0) }
        val sinceMs = cal.timeInMillis
        val untilMs = sinceMs + TimeUnit.DAYS.toMillis(42)
        val entries = repo.getBetween(sinceMs, untilMs)
        CalendarViewModel.setDayEvents(CalendarViewModel.aggregate(entries))
    }

    var addOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DiaryEntryEntity?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    // 选中日的日记
    val dayEntries by produceState<List<DiaryEntryEntity>>(emptyList(), selectedDate, refreshKey) {
        value = repo.getForDay(selectedDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diary_title)) },
                actions = {
                    IconButton(onClick = { editing = null; addOpen = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.diary_new))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            // 选中日标题
            val dayTitle = remember(selectedDate) {
                // TODO(v0.9b): SimpleDateFormat pattern "M 月 d 日 EEE" 硬编码中文
                SimpleDateFormat("M 月 d 日 EEE", Locale.CHINA).format(Date(selectedDate))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 $dayTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // TODO(v0.9b): "X 条" 计数标签需要 i18n (新增 diary_n_entries key)
                Text(
                    text = "${dayEntries.size} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (dayEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO(v0.9e done): 改 R.string.home_no_diary (3 语言)
                    Text(
                        text = stringResource(R.string.home_no_diary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(dayEntries, key = { it.id }) { e ->
                        DiaryCard(
                            entry = e,
                            onEdit = { editing = e; addOpen = true },
                            onDelete = {
                                scope.launch {
                                    repo.delete(e.id)
                                    refreshKey++
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (addOpen) {
        DiaryEditorDialog(
            initial = editing,
            onDismiss = { addOpen = false; editing = null },
            onSave = { ts, mood, text ->
                scope.launch {
                    if (editing == null) {
                        repo.add(ts, mood, text)
                    } else {
                        // 复用 upsert, id 不变
                        // (DiaryRepository.add 是新建, 这里直接 delete + add)
                        repo.delete(editing!!.id)
                        repo.add(ts, mood, text)
                    }
                    refreshKey++
                    addOpen = false
                    editing = null
                }
            }
        )
    }
}

@Composable
private fun DiaryCard(
    entry: DiaryEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val mood = DiaryMood.fromName(entry.mood)
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.CHINA) }
    val dayFmt = remember { SimpleDateFormat("M/d HH:mm", Locale.CHINA) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = mood.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(mood.displayNameRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dayFmt.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), modifier = Modifier.size(18.dp))
                }
            }
            if (!entry.text.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryEditorDialog(
    initial: DiaryEntryEntity?,
    onDismiss: () -> Unit,
    onSave: (timestamp: Long, mood: DiaryMood, text: String?) -> Unit
) {
    var mood by remember { mutableStateOf(initial?.let { DiaryMood.fromName(it.mood) } ?: DiaryMood.Calm) }
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var customTimeEnabled by remember { mutableStateOf(initial != null && initial.timestamp != System.currentTimeMillis()) }
    val initialTime = initial?.timestamp ?: System.currentTimeMillis()
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }
    val hourFmt = remember { SimpleDateFormat("HH:mm", Locale.CHINA) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableStateOf(CalendarViewModel.startOfDay(initialTime)) }
    var hourMin by remember { mutableStateOf(initialTime % TimeUnit.DAYS.toMillis(1)) }

    fun composeTimestamp(): Long = dateMillis + hourMin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) stringResource(R.string.diary_new) else /* TODO(v0.9b): 新增 strings.xml key (编辑日记, diary_edit_title) */ "编辑日记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 心境下拉
                var moodOpen by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = moodOpen,
                    onExpandedChange = { moodOpen = it }
                ) {
                    OutlinedTextField(
                        value = "${mood.emoji} ${stringResource(mood.displayNameRes)}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.diary_mood)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = moodOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = moodOpen, onDismissRequest = { moodOpen = false }) {
                        DiaryMood.entries.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.emoji} ${stringResource(m.displayNameRes)}") },
                                onClick = { mood = m; moodOpen = false }
                            )
                        }
                    }
                }

                // 时间 (默认系统时间, 可改)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // TODO(v0.9b): "时间" label 需要 i18n (新增 diary_time_label)
                    Text(
                        text = "时间",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { customTimeEnabled = !customTimeEnabled }) {
                        // TODO(v0.9b): "使用现在" / "自定义" 需要 i18n
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
                    // TODO(v0.9b): "现在: X" 格式需要 i18n
                    Text(
                        text = "现在: ${timeFmt.format(Date())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 文本框
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.diary_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ts = if (customTimeEnabled) composeTimestamp() else System.currentTimeMillis()
                onSave(ts, mood, text.takeIf { it.isNotBlank() })
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
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
