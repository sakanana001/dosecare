package com.picohrt.widget

import com.picohrt.data.repository.HomeSnapshotRepository
import com.picohrt.data.repository.JournalRepository
import com.picohrt.data.repository.MedicationGroupRepository
import com.picohrt.data.repository.MedicationLogRepository
import com.picohrt.data.repository.MedicineStockRepository
import com.picohrt.data.repository.SettingsRepository
import com.picohrt.di.AppScope
import com.picohrt.reminder.ReminderNotificationManager
import com.picohrt.util.AppDiagnosticsLogger
import com.picohrt.util.AppTimeSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetSnapshotStore(): WidgetSnapshotStore
    fun homeSnapshotRepository(): HomeSnapshotRepository
    fun journalRepository(): JournalRepository
    fun medicationGroupRepository(): MedicationGroupRepository
    fun medicationLogRepository(): MedicationLogRepository
    fun medicineStockRepository(): MedicineStockRepository
    fun settingsRepository(): SettingsRepository
    fun widgetAppearanceRepository(): WidgetAppearanceRepository
    fun reminderNotificationManager(): ReminderNotificationManager
    fun diagnosticsLogger(): AppDiagnosticsLogger
    fun appTimeSource(): AppTimeSource

    @AppScope
    fun appScope(): CoroutineScope
}
