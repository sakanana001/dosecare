package com.dosecare.app.util

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElapsedRealtimeProvider @Inject constructor() {
    fun now(): Long = SystemClock.elapsedRealtime()
}
