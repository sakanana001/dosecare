package com.dosecare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * v0.8a Phase F4: 月历 Composable
 *
 * - 6x7 网格 (含上月末尾 + 下月开头填充格)
 * - 每天: 数字 + 两个事件点 (蓝=用药, 橙=日记)
 * - 选中日: 蓝色实心圆
 * - 今天: 蓝色边
 *
 * 设计参考: 简洁单色, 选中态用 primary 实心圆.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendar(
    selectedDate: Long,
    currentYear: Int,
    currentMonth: Int,
    dayEvents: Map<Long, CalendarViewModel.DayEvents>,
    onDateClick: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleFmt = remember { SimpleDateFormat("yyyy 年 M 月", Locale.CHINA) }
    val titleCal = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
    }
    val monthTitle = remember(titleCal) { titleFmt.format(titleCal.time) }
    val todayStart = remember { CalendarViewModel.startOfDay(System.currentTimeMillis()) }

    // 当月 1 号对应的星期 (周一=1, 周日=0/7)
    val firstWeekday = remember(currentYear, currentMonth) {
        val c = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
        // 转成 周一=0 ... 周日=6
        val wd = c.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
        (wd + 5) % 7
    }
    // 当月总天数
    val daysInMonth = remember(currentYear, currentMonth) {
        val c = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    // 总格子数 (头空格 + 当月 + 尾空格 = 6 行 × 7 = 42)
    val totalCells = 42

    val medColor = Color(0xFF1976D2)
    val diaryColor = Color(0xFFE65100)

    Column(modifier = modifier) {
        // 顶部: 年月 + 翻页箭头
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
            }
            Text(
                text = monthTitle,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
            }
        }

        // 星期表头
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(
                    text = w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 6x7 格子
        var cellIdx = 0
        repeat(6) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                repeat(7) { col ->
                    val dayNumber: Int? = when {
                        cellIdx < firstWeekday -> null
                        cellIdx >= firstWeekday + daysInMonth -> null
                        else -> cellIdx - firstWeekday + 1
                    }
                    val dayTimestamp: Long? = dayNumber?.let {
                        Calendar.getInstance().apply {
                            set(currentYear, currentMonth, it, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val isSelected = dayTimestamp != null && dayTimestamp == selectedDate
                    val isToday = dayTimestamp != null && dayTimestamp == todayStart
                    val events = dayTimestamp?.let { dayEvents[it] } ?: CalendarViewModel.DayEvents()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .border(
                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .let {
                                if (dayNumber != null) it.clickable { onDateClick(dayTimestamp!!) }
                                else it
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (dayNumber != null) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (events.hasMed || events.hasDiary) {
                                    Spacer(Modifier.height(2.dp))
                                    Row {
                                        if (events.hasMed) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else medColor)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                        }
                                        if (events.hasDiary) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else diaryColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    cellIdx++
                }
            }
        }
    }
}
