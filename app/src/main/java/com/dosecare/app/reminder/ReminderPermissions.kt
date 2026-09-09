package com.dosecare.app.reminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * v0.9f 提醒权限 helper
 *
 * 三种权限:
 * 1. POST_NOTIFICATIONS (API 33+) — Activity 走 registerForActivityResult 申请
 * 2. SCHEDULE_EXACT_ALARM (API 31+) — 用户到系统设置开启, Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
 * 3. USE_EXACT_ALARM (API 33+) — manifest declared, 自动 granted (但仅限 reminder/calendar 类 app)
 *
 * 内部自检 + 引导用户去设置
 */
object ReminderPermissions {

    private const val TAG = "ReminderPermissions"

    /**
     * 是否可以发通知 (POST_NOTIFICATIONS 授权 OR API < 33)
     */
    fun canPostNotifications(context: Context): Boolean {
        return NotificationHelper.isPostNotificationsGranted(context)
    }

    /**
     * 是否可以排精确闹钟
     * - API < 31: 默认可以 (setExactAndAllowWhileIdle 不需特殊权限)
     * - API 31+: 用户必须到系统设置手动开 SCHEDULE_EXACT_ALARM
     * - API 33+: 也可以用 USE_EXACT_ALARM (manifest declared, 自动 granted), 但限定 app 类型
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true  // API < 31 默认可以
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /**
     * 生成"引导用户到系统设置开启 SCHEDULE_EXACT_ALARM"的 Intent
     * API 31+ 用, 无权时调
     */
    fun buildExactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            // API < 31 没有这个 action, fallback 到 app 详情页
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * 综合自检: 所有提醒相关权限是否就绪
     */
    fun allReady(context: Context): Boolean {
        val notif = canPostNotifications(context)
        val alarm = canScheduleExactAlarms(context)
        Log.d(TAG, "permissions: notif=$notif alarm=$alarm")
        return notif && alarm
    }
}
