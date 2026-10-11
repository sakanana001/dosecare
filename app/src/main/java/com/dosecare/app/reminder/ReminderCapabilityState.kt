package com.dosecare.app.reminder

data class ReminderCapabilityState(
    val hasNotificationAccess: Boolean = true,
    val hasExactAlarmAccess: Boolean = true,
)
