package com.dosecare.app.data.repository

import com.dosecare.app.data.db.DiaryDao
import com.dosecare.app.data.db.DiaryEntryEntity
import com.dosecare.app.data.db.DiaryMood
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v0.8a Phase F: 日记 Repository
 *
 * - 提供 CRUD + 日历范围查询
 * - 提供"日历事件"聚合 (某天有多少条日记),用于日历格子里画点
 * - 日历天: 跟 PrescriptionRepository 一致, 用 Calendar.set(... 0:00) 切天
 */
@Singleton
class DiaryRepository @Inject constructor(
    private val dao: DiaryDao
) {

    /** 新建日记,返回生成的 id */
    suspend fun add(timestamp: Long, mood: DiaryMood, text: String?): String {
        val entry = DiaryEntryEntity(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            mood = mood.name,
            text = text?.takeIf { it.isNotBlank() }
        )
        dao.upsert(entry)
        return entry.id
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    fun observeAll(): Flow<List<DiaryEntryEntity>> = dao.observeAll()

    /** 监听某 30/31 天窗口 (用于日历格点 + 选中日详情) */
    fun observeBetween(sinceMillis: Long, untilMillis: Long): Flow<List<DiaryEntryEntity>> =
        dao.observeBetween(sinceMillis, untilMillis)

    suspend fun getBetween(sinceMillis: Long, untilMillis: Long): List<DiaryEntryEntity> =
        dao.getBetween(sinceMillis, untilMillis)

    suspend fun count(): Int = dao.count()

    /**
     * 给定一个 LocalDate (epochDay 0:00), 拿当天所有日记
     * 用 1 天窗口 query 即可 (DAO 已有 index on timestamp)
     */
    suspend fun getForDay(epochDayStart: Long): List<DiaryEntryEntity> =
        dao.getBetween(epochDayStart, epochDayStart + TimeUnit.DAYS.toMillis(1))

    companion object {
        /** epochDay 0:00 标准化 (脱掉时分秒) */
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

        /** 同一天的结束 (startOfDay + 24h) */
        fun endOfDay(timestamp: Long): Long = startOfDay(timestamp) + TimeUnit.DAYS.toMillis(1)
    }
}
