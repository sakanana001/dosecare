package com.dosecare.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v0.9f 开机/重装广播
 *
 * WorkManager 队列在 BOOT_COMPLETED / MY_PACKAGE_REPLACED 后会被系统清空
 * 重新排所有 active drug 的提醒, 避免用户开机后没提醒
 *
 * Hilt @AndroidEntryPoint 让 onReceive 可以 @Inject scheduler
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "onReceive: action=$action")

        if (action !in HANDLED_ACTIONS) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.scheduleAll()
                Log.i(TAG, "scheduleAll done after $action")
            } catch (e: Exception) {
                Log.w(TAG, "scheduleAll failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderBootReceiver"
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )
    }
}
