package com.dosecare.app.widget

import com.dosecare.app.data.repository.HomeSnapshotRepository
import com.dosecare.app.data.repository.JournalRepository
import com.dosecare.app.data.repository.MedicationGroupRepository
import com.dosecare.app.data.repository.MedicationLogRepository
import com.dosecare.app.data.repository.MedicineStockRepository
import com.dosecare.app.data.repository.SettingsRepository
import com.dosecare.app.di.AppScope
import com.dosecare.app.reminder.ReminderNotificationManager
import com.dosecare.app.util.AppDiagnosticsLogger
import com.dosecare.app.util.AppTimeSource
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
