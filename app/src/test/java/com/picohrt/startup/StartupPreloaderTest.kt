package com.picohrt.startup

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.picohrt.data.local.DatabaseHolder
import com.picohrt.data.local.HrtTrackerDatabase
import com.picohrt.data.repository.HomeSnapshotRecord
import com.picohrt.data.repository.HomeSnapshotRepository
import com.picohrt.data.repository.MedicationGroupRepository
import com.picohrt.data.repository.MedicationLogRepository
import com.picohrt.data.repository.SettingsRepository
import com.picohrt.data.repository.UserProfileRepository
import com.picohrt.model.personalization.UserProfile
import com.picohrt.model.settings.SettingsState
import com.picohrt.reminder.MedicationReminderScheduler
import com.picohrt.reminder.MedicationReminderSnoozeScheduler
import com.picohrt.util.AppDiagnosticsLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class StartupPreloaderTest {
    private val databaseHolder: DatabaseHolder = mockk()
    private val homeSnapshotRepository: HomeSnapshotRepository = mockk()
    private val medicationGroupRepository: MedicationGroupRepository = mockk()
    private val medicationLogRepository: MedicationLogRepository = mockk()
    private val medicineRepository: com.picohrt.data.repository.MedicineRepository = mockk {
        coEvery { getAll() } returns emptyList()
    }
    private val userProfileRepository: UserProfileRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val medicationReminderScheduler: MedicationReminderScheduler = mockk()
    private val medicationReminderSnoozeScheduler: MedicationReminderSnoozeScheduler = mockk()
    private val diagnosticsLogger: AppDiagnosticsLogger = mockk(relaxed = true)

    @Test
    fun startAfterFirstHomeFrame_reschedulesRemindersAfterWarmup() = runTest {
        val database: HrtTrackerDatabase = mockk()
        val openHelper: SupportSQLiteOpenHelper = mockk()
        val writableDatabase: SupportSQLiteDatabase = mockk()
        every { databaseHolder.get() } returns database
        every { database.openHelper } returns openHelper
        every { openHelper.writableDatabase } returns writableDatabase
        coEvery { medicationGroupRepository.getGroups() } returns emptyList()
        coEvery { medicationLogRepository.getEntries() } returns emptyList()
        coEvery { userProfileRepository.getCurrentProfile() } returns UserProfile()
        coEvery { settingsRepository.getCurrentSettings() } returns SettingsState()
        coEvery { medicationReminderScheduler.rescheduleAll(any()) } returns Unit
        coEvery { medicationReminderSnoozeScheduler.rescheduleAll(any()) } returns Unit

        StartupPreloader(
            appScope = CoroutineScope(Dispatchers.IO),
            databaseHolder = databaseHolder,
            homeSnapshotRepository = homeSnapshotRepository,
            medicationGroupRepository = medicationGroupRepository,
            medicationLogRepository = medicationLogRepository,
            medicineRepository = medicineRepository,
            userProfileRepository = userProfileRepository,
            settingsRepository = settingsRepository,
            medicationReminderScheduler = medicationReminderScheduler,
            medicationReminderSnoozeScheduler = medicationReminderSnoozeScheduler,
            diagnosticsLogger = diagnosticsLogger,
        ).startAfterFirstHomeFrame()

        coVerify(timeout = 1_000, exactly = 1) { medicationReminderScheduler.rescheduleAll(any()) }
        coVerify(
            timeout = 1_000,
            exactly = 1
        ) { medicationReminderSnoozeScheduler.rescheduleAll(any()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startReminderRescheduleFromSnapshot_usesSnapshotWithoutWarmingDatabase() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val snapshot: HomeSnapshotRecord = mockk()
        val now = LocalDateTime.of(2026, 5, 6, 10, 15)
        coEvery { homeSnapshotRepository.readUsableHomeSnapshot(now = now) } returns snapshot
        coEvery {
            medicationReminderScheduler.rescheduleFromHomeSnapshot(
                snapshot = snapshot,
                now = now,
            )
        } returns Unit
        coEvery { medicationReminderSnoozeScheduler.rescheduleAll(now = now) } returns Unit

        StartupPreloader(
            appScope = CoroutineScope(dispatcher),
            databaseHolder = databaseHolder,
            homeSnapshotRepository = homeSnapshotRepository,
            medicationGroupRepository = medicationGroupRepository,
            medicationLogRepository = medicationLogRepository,
            medicineRepository = medicineRepository,
            userProfileRepository = userProfileRepository,
            settingsRepository = settingsRepository,
            medicationReminderScheduler = medicationReminderScheduler,
            medicationReminderSnoozeScheduler = medicationReminderSnoozeScheduler,
            diagnosticsLogger = diagnosticsLogger,
        ).startReminderRescheduleFromSnapshot(now = now)
        advanceUntilIdle()

        coVerify(
            timeout = 1_000,
            exactly = 1
        ) { homeSnapshotRepository.readUsableHomeSnapshot(now = now) }
        coVerify(timeout = 1_000, exactly = 1) {
            medicationReminderScheduler.rescheduleFromHomeSnapshot(
                snapshot = snapshot,
                now = now,
            )
        }
        coVerify(
            timeout = 1_000,
            exactly = 1
        ) { medicationReminderSnoozeScheduler.rescheduleAll(now = now) }
        verify(exactly = 0) { databaseHolder.get() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startReminderRescheduleFromSnapshot_reschedulesSnoozesWhenNoUsableSnapshotExists() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val now = LocalDateTime.of(2026, 5, 6, 10, 15)
            coEvery { homeSnapshotRepository.readUsableHomeSnapshot(now = now) } returns null
            coEvery { medicationReminderSnoozeScheduler.rescheduleAll(now = now) } returns Unit

            StartupPreloader(
                appScope = CoroutineScope(dispatcher),
                databaseHolder = databaseHolder,
                homeSnapshotRepository = homeSnapshotRepository,
                medicationGroupRepository = medicationGroupRepository,
                medicationLogRepository = medicationLogRepository,
                medicineRepository = medicineRepository,
                userProfileRepository = userProfileRepository,
                settingsRepository = settingsRepository,
                medicationReminderScheduler = medicationReminderScheduler,
                medicationReminderSnoozeScheduler = medicationReminderSnoozeScheduler,
                diagnosticsLogger = diagnosticsLogger,
            ).startReminderRescheduleFromSnapshot(now = now)
            advanceUntilIdle()

            coVerify(
                timeout = 1_000,
                exactly = 1
            ) { medicationReminderSnoozeScheduler.rescheduleAll(now = now) }
            coVerify(timeout = 1_000, exactly = 0) {
                medicationReminderScheduler.rescheduleFromHomeSnapshot(any(), any())
            }
            verify(exactly = 0) { databaseHolder.get() }
            verify {
                diagnosticsLogger.info(
                    "StartupPreloader",
                    match { message ->
                        "snapshot_reminder_reschedule_no_usable_snapshot" in message &&
                                "now=2026-05-06T10:15" in message
                    }
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startReminderRescheduleFromWarmDatabase_reschedulesWithoutWarmupPass() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val now = LocalDateTime.of(2026, 5, 6, 10, 15)
        coEvery { medicationReminderScheduler.rescheduleAll(now = now) } returns Unit
        coEvery { medicationReminderSnoozeScheduler.rescheduleAll(now = now) } returns Unit

        StartupPreloader(
            appScope = CoroutineScope(dispatcher),
            databaseHolder = databaseHolder,
            homeSnapshotRepository = homeSnapshotRepository,
            medicationGroupRepository = medicationGroupRepository,
            medicationLogRepository = medicationLogRepository,
            medicineRepository = medicineRepository,
            userProfileRepository = userProfileRepository,
            settingsRepository = settingsRepository,
            medicationReminderScheduler = medicationReminderScheduler,
            medicationReminderSnoozeScheduler = medicationReminderSnoozeScheduler,
            diagnosticsLogger = diagnosticsLogger,
        ).startReminderRescheduleFromWarmDatabase(now = now)
        advanceUntilIdle()

        coVerify(
            timeout = 1_000,
            exactly = 1
        ) { medicationReminderScheduler.rescheduleAll(now = now) }
        coVerify(
            timeout = 1_000,
            exactly = 1
        ) { medicationReminderSnoozeScheduler.rescheduleAll(now = now) }
        verify(exactly = 0) { databaseHolder.get() }
        coVerify(exactly = 0) { medicationGroupRepository.getGroups() }
        coVerify(exactly = 0) { medicationLogRepository.getEntries() }
        coVerify(exactly = 0) { userProfileRepository.getCurrentProfile() }
        coVerify(exactly = 0) { settingsRepository.getCurrentSettings() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startReminderRescheduleFromWarmDatabase_reschedulesSnoozesWhenRoomReminderRescheduleFails() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val now = LocalDateTime.of(2026, 5, 6, 10, 15)
            coEvery { medicationReminderScheduler.rescheduleAll(now = now) } throws
                    RuntimeException("Room unavailable")
            coEvery { medicationReminderSnoozeScheduler.rescheduleAll(now = now) } returns Unit

            StartupPreloader(
                appScope = CoroutineScope(dispatcher),
                databaseHolder = databaseHolder,
                homeSnapshotRepository = homeSnapshotRepository,
                medicationGroupRepository = medicationGroupRepository,
                medicationLogRepository = medicationLogRepository,
                medicineRepository = medicineRepository,
                userProfileRepository = userProfileRepository,
                settingsRepository = settingsRepository,
                medicationReminderScheduler = medicationReminderScheduler,
                medicationReminderSnoozeScheduler = medicationReminderSnoozeScheduler,
                diagnosticsLogger = diagnosticsLogger,
            ).startReminderRescheduleFromWarmDatabase(now = now)
            advanceUntilIdle()

            coVerify(
                timeout = 1_000,
                exactly = 1
            ) { medicationReminderScheduler.rescheduleAll(now = now) }
            coVerify(
                timeout = 1_000,
                exactly = 1
            ) { medicationReminderSnoozeScheduler.rescheduleAll(now = now) }
            verify(exactly = 0) { databaseHolder.get() }
        }
}
