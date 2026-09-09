package com.dosecare.app.reminder

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dosecare.app.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v0.9f 提醒调度器
 *
 * 入口 (suspend):
 * - scheduleAll(): 扫所有 active drug × default slots, 入队 OneTimeWorkRequest
 * - cancelAll(): 取消所有 reminder worker
 *
 * 入口 (sync):
 * - scheduleNextDay(drugId, slotTime): worker 触发后内部调, 排下一天
 * - scheduleTestReminder(...): 测试按钮
 *
 * ⚠️ v0.9f 限制:
 * - 当前 PrescribedDrugEntity 没有 times 字段 (v0.7 Room migration 漏, 已知欠债)
 * - scheduleAll() 暂时按 frequencyPerDay 用默认时段:
 *     freq=1 → 08:00
 *     freq=2 → 08:00, 20:00
 *     freq=3 → 08:00, 14:00, 20:00
 *     freq=4 → 08:00, 12:00, 18:00, 22:00
 *   v0.9g 加 PrescribedDrugEntity.times + migration 后, 改读自定义时段
 * - temp drug (targetDate != null) 跳过 (单次提醒不需要预约)
 *
 * 调度算法:
 * - 入队 OneTimeWorkRequest (initialDelay = millisUntilSlot)
 * - 触发后 worker 自动 scheduleNextDay 滚到下一天
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val db: AppDatabase
) {
    private val drugDao get() = db.prescribedDrugDao()
    private val groupDao get() = db.prescriptionGroupDao()
    private val workManager: WorkManager = WorkManager.getInstance(appContext)

    /**
     * 取消所有 reminder worker
     */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(WORK_TAG)
        Log.i(TAG, "cancelAll: cleared all reminder workers")
    }

    /**
     * 扫所有 active drug × default slots, 入队 OneTimeWorkRequest
     * 在 prescribed_drug 增删改后调
     */
    suspend fun scheduleAll() {
        cancelAll()
        val drugs = drugDao.getAll()
        var scheduled = 0
        val now = System.currentTimeMillis()
        for (drug in drugs) {
            if (!drug.active) continue
            // 跳过临时用药 (Room 端: frequencyPerDay <= 0 视为临时, 或有特殊标记)
            if (drug.frequencyPerDay <= 0) continue
            val slots = defaultSlotsFor(drug.frequencyPerDay)
            for (slot in slots) {
                if (scheduleNextSlot(drug.id, slot, fromNowMs = now)) {
                    scheduled++
                }
            }
        }
        Log.i(TAG, "scheduleAll: scheduled $scheduled reminder workers (active drugs=${drugs.count { it.active }})")
    }

    /**
     * 排下一天同一时间 (ReminderWorker.doWork 末尾自动调, 滚雪球)
     * sync — 简单查表 + 入队
     */
    fun scheduleNextDay(drugId: String, slotTime: String) {
        // 找今天这一时刻的 epochMs; 如果今天已过, 加 24h
        val triggerAt = nextTriggerAfter(slotTime, afterMs = System.currentTimeMillis()) ?: return
        val nextDay = triggerAt + 24 * 60 * 60 * 1000L
        enqueueOne(drugId, slotTime, nextDay)
    }

    /**
     * 测试用: N 秒后发一条提醒
     */
    fun scheduleTestReminder(drugId: String, drugName: String, slotTime: String = "测试", delaySec: Long = 5) {
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delaySec, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString(ReminderWorker.KEY_DRUG_ID, drugId)
                    .putString(ReminderWorker.KEY_SLOT_TIME, slotTime)
                    .build()
            )
            .addTag(WORK_TAG)
            .addTag("test")
            .build()
        workManager.enqueueUniqueWork(
            "reminder_test_$drugId",
            ExistingWorkPolicy.REPLACE,
            req
        )
        Log.i(TAG, "scheduleTestReminder: drugId=$drugId in ${delaySec}s")
    }

    // ============== 内部 ==============

    /**
     * v0.9f 临时默认时段 (Room 缺 times 字段的过渡方案)
     * v0.9g 加 times + migration 后, 改读 drug.times
     */
    private fun defaultSlotsFor(freq: Int): List<String> = when (freq) {
        1 -> listOf("08:00")
        2 -> listOf("08:00", "20:00")
        3 -> listOf("08:00", "14:00", "20:00")
        4 -> listOf("08:00", "12:00", "18:00", "22:00")
        else -> emptyList()  // freq 0 (临时) 或 异常值, 不排
    }

    private fun scheduleNextSlot(drugId: String, slot: String, fromNowMs: Long): Boolean {
        val triggerAt = nextTriggerAfter(slot, afterMs = fromNowMs) ?: return false
        enqueueOne(drugId, slot, triggerAt)
        return true
    }

    private fun enqueueOne(drugId: String, slotTime: String, triggerAt: Long) {
        val delayMs = triggerAt - System.currentTimeMillis()
        if (delayMs <= 0) return  // 已过期
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(ReminderWorker.KEY_DRUG_ID, drugId)
                    .putString(ReminderWorker.KEY_SLOT_TIME, slotTime)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)  // 低电量也要提醒
                    .build()
            )
            .addTag(WORK_TAG)
            .addTag("drug_$drugId")
            .addTag("slot_${drugId}_$slotTime")
            .build()
        // unique by drug+slot — 同一药同一时间只能有一个
        workManager.enqueueUniqueWork(
            "reminder_${drugId}_$slotTime",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    /**
     * 给定 "HH:mm" 和 "afterMs" 之后, 算下一次触发 epochMs
     * (当天时间 <= afterMs 时, 排明天同一时刻)
     */
    private fun nextTriggerAfter(hhmm: String, afterMs: Long): Long? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null

        val cal = Calendar.getInstance().apply {
            timeInMillis = afterMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= afterMs) {
            // 当天时间已过, 排明天
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        const val WORK_TAG = "reminder"
    }
}
