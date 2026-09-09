package com.dosecare.app.reminder

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dosecare.app.domain.catalog.DrugCatalogService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * v0.9f 提醒 Worker
 *
 * - WorkManager 在到点时启动
 * - doWork 推通知 + 自动排下一天同一时间
 *
 * InputData:
 * - KEY_DRUG_ID: PrescribedDrug.id
 * - KEY_SLOT_TIME: "HH:mm"
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val drugCatalog: DrugCatalogService,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val drugId = inputData.getString(KEY_DRUG_ID) ?: return Result.failure()
        val slotTime = inputData.getString(KEY_SLOT_TIME) ?: return Result.failure()

        Log.i(TAG, "doWork: drugId=$drugId slot=$slotTime")

        // 1. 查 drug 详情 (名字, 剂量)
        val drugName = try {
            // DrugCatalogService.getById 找不到时抛 NoSuchElementException
            val drug = drugCatalog.getById(drugId)
            drug.genericNameZh ?: drug.genericName
        } catch (e: Exception) {
            Log.w(TAG, "drug lookup failed: ${e.message}")
            drugId
        }

        // 2. 推通知 (如果用户没授权 POST_NOTIFICATIONS, NotificationManagerCompat.notify 静默失败)
        if (NotificationHelper.isPostNotificationsGranted(applicationContext)) {
            val notif = NotificationHelper.buildReminderNotification(
                context = applicationContext,
                drugId = drugId,
                slotTime = slotTime,
                drugDisplayName = drugName
            )
            NotificationManagerCompat.from(applicationContext).notify(
                (drugId + slotTime).hashCode(),
                notif.build()
            )
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS not granted, skip notification")
        }

        // 3. 自动排下一天同一时间 (OneTimeWorkRequest 滚雪球)
        try {
            reminderScheduler.scheduleNextDay(drugId, slotTime)
        } catch (e: Exception) {
            Log.w(TAG, "scheduleNextDay failed: ${e.message}")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "ReminderWorker"
        const val KEY_DRUG_ID = "drugId"
        const val KEY_SLOT_TIME = "slotTime"
    }
}
