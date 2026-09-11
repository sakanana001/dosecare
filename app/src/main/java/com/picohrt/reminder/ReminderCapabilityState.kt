package com.picohrt.reminder

data class ReminderCapabilityState(
    val hasNotificationAccess: Boolean = true,
    val hasExactAlarmAccess: Boolean = true,
)
