package com.dosecare.app.ui

import com.dosecare.app.data.db.DiaryEntryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * v0.8a Phase F3: 跨 tab 共享日历状态
 *
 * - selectedDate: 当前选中的某天 (epochDay start 0:00)
 * - currentMonth: 当前展示的月份 (Calendar.YEAR + MONTH)
 * - dayEvents: Map<epochDay, DayEvents>  从 DiaryRepository + PrescriptionRepository 聚合
 *   暂只用日记(用药事件 v0.8b/c 再接 dose_taken 表)
 *
 * 数据从 Composables 的 LaunchedEffect 灌进来, ViewModel 只持 state.
 * 跨 navigation 保留: object 单例 (App 进程不杀就一直在)
 */
object CalendarViewModel {
    /** 单选日, 默认今天 */
    private val _selectedDate = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    fun selectDate(epochDayStart: Long) {
        _selectedDate.value = epochDayStart
    }

    data class YearMonth(val year: Int, val month: Int)

    private val _currentMonth = MutableStateFlow(todayYearMonth())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    fun goPrevMonth() {
        val (y, m) = _currentMonth.value
        val cal = Calendar.getInstance().apply { set(y, m, 1); add(Calendar.MONTH, -1) }
        _currentMonth.value = YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun goNextMonth() {
        val (y, m) = _currentMonth.value
        val cal = Calendar.getInstance().apply { set(y, m, 1); add(Calendar.MONTH, 1) }
        _currentMonth.value = YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun jumpToToday() {
        _currentMonth.value = todayYearMonth()
        _selectedDate.value = startOfDay(System.currentTimeMillis())
    }

    private fun todayYearMonth(): YearMonth {
        val cal = Calendar.getInstance()
        return YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    /**
     * 某天的事件聚合: 暂时只有日记, 后续接 dose_taken
     *  Key: epochDay (当天 0:00)
     */
    data class DayEvents(
        val hasMed: Boolean = false,
        val hasDiary: Boolean = false,
        val diaryCount: Int = 0
    )

    private val _dayEvents = MutableStateFlow<Map<Long, DayEvents>>(emptyMap())
    val dayEvents: StateFlow<Map<Long, DayEvents>> = _dayEvents.asStateFlow()

    /** 注入当月日记 + 打卡记录, 重新算 dayEvents map */
    fun setDayEvents(events: Map<Long, DayEvents>) {
        // 简单整体替换; 翻月时只查当月, 过期数据被覆盖
        _dayEvents.value = events
    }

    /**
     * 给定一组日记条目 + (可选) 用药打卡, 按 epochDay 聚合
     */
    fun aggregate(
        diaryEntries: List<DiaryEntryEntity>,
        medDays: Set<Long> = emptySet()
    ): Map<Long, DayEvents> {
        val byDay = mutableMapOf<Long, DayEvents>()
        diaryEntries.forEach { e ->
            val day = startOfDay(e.timestamp)
            val prev = byDay[day] ?: DayEvents()
            byDay[day] = prev.copy(hasDiary = true, diaryCount = prev.diaryCount + 1)
        }
        medDays.forEach { day ->
            val prev = byDay[day] ?: DayEvents()
            byDay[day] = prev.copy(hasMed = true)
        }
        return byDay
    }

    fun startOfDay(timestamp: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
}
