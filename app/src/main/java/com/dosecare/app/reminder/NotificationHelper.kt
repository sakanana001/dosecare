package com.dosecare.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dosecare.app.MainActivity
import com.dosecare.app.R

/**
 * v0.9f 通知 helper
 *
 * - NotificationChannel (Android 8+ 必须先建 channel 才能发通知)
 * - build 通知并 tap → MainActivity (带 drugId + slot extra)
 * - isPostNotificationsGranted() 权限检查 (API 33+)
 */
object NotificationHelper {

    const val CHANNEL_ID = "dosecare_reminder"
    const val CHANNEL_NAME = "服药提醒"
    const val CHANNEL_DESC = "到点吃药推送通知 (点开进入打卡)"

    /**
     * 创建/更新通知 channel
     * 幂等, 多次调用无副作用
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * API 33+ 检查 POST_NOTIFICATIONS 权限
     */
    fun isPostNotificationsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true  // API < 33 不需要运行时权限
        }
    }

    /**
     * 构造通知 builder
     *
     * @param context 用于启动 MainActivity 的 PendingIntent
     * @param drugId 药物 ID (PrescribedDrug.id), 用于 intent extra
     * @param slotTime 计划时间 "HH:mm", 用于 intent extra + body
     * @param drugDisplayName 药物显示名 (从 drugId 查 DrugCatalog, 跟 locale)
     */
    fun buildReminderNotification(
        context: Context,
        drugId: String,
        slotTime: String,
        drugDisplayName: String
    ): NotificationCompat.Builder {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            // singleTask + CLEAR_TOP 避免堆栈叠多个 MainActivity
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_DRUG_ID, drugId)
            putExtra(EXTRA_REMINDER_SLOT_TIME, slotTime)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (drugId + slotTime).hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)  // 暂时复用 launcher icon
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_body, drugDisplayName, slotTime))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)  // 用户点后自动消失
            .setContentIntent(pendingIntent)
    }

    const val EXTRA_REMINDER_DRUG_ID = "com.dosecare.app.reminder.DRUG_ID"
    const val EXTRA_REMINDER_SLOT_TIME = "com.dosecare.app.reminder.SLOT_TIME"
}
