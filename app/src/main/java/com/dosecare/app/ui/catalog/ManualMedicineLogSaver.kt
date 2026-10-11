package com.dosecare.app.ui.catalog

import com.dosecare.app.data.repository.MedicationLogRepository
import com.dosecare.app.data.repository.MedicineStockRepository
import com.dosecare.app.model.medication.DoseInstruction
import com.dosecare.app.model.medication.MedicationApplicationType
import com.dosecare.app.reminder.MedicationReminderScheduler
import com.dosecare.app.reminder.PostLogStockWarning
import com.dosecare.app.reminder.captureStockStatesForLog
import com.dosecare.app.reminder.resolvePostLogStockWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

internal suspend fun saveManualMedicineLog(
    medicationLogRepository: MedicationLogRepository,
    medicineStockRepository: MedicineStockRepository,
    medicationReminderScheduler: MedicationReminderScheduler,
    medicineUuid: UUID,
    resolvedApplicationType: MedicationApplicationType,
    doseInstruction: DoseInstruction,
    count: Int,
    doseAmountDelta: Double? = null,
    appliedDate: LocalDate,
    appliedTime: LocalTime,
    appliedZoneId: ZoneId,
    note: String? = null,
): ManualMedicineLogSaveOutcome {
    val appliedAt = LocalDateTime.of(
        appliedDate,
        appliedTime.withSecond(0).withNano(0),
    ).atZone(appliedZoneId).toInstant()
    val resolvedDose = if (resolvedApplicationType == MedicationApplicationType.PATCH_OFF) {
        DoseInstruction.Noop
    } else {
        doseInstruction
    }

    // Snapshot stock before the deduction so the warning only fires when this
    // log actually worsens the medicine's tier.
    val beforeStockStates = captureStockStatesForLog(
        medicineStockRepository = medicineStockRepository,
        now = Instant.now(),
    )
    val saveResult = try {
        medicationLogRepository.saveEntry(
            uuid = null,
            medicineUuid = medicineUuid,
            applicationType = resolvedApplicationType,
            doseInstruction = resolvedDose,
            sourceGroupUuid = null,
            scheduleTimeUuid = null,
            appliedAt = appliedAt,
            scheduledFor = null,
            count = count.coerceAtLeast(1),
            appliedAtTimeZoneId = appliedZoneId.id,
            doseAmountDelta = doseAmountDelta,
            note = note,
        )
        null
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        MedicineSlotDraftSaveResult.FAILURE
    }
    if (saveResult != null) {
        return ManualMedicineLogSaveOutcome(saveResult = saveResult)
    }
    val postLogStockWarning = runCatching {
        resolvePostLogStockWarning(
            projections = medicineStockRepository.projectAllOnce(now = Instant.now()),
            affectedMedicineUuids = setOf(medicineUuid),
            beforeStatesByUuid = beforeStockStates,
        )
    }.getOrElse { failure ->
        if (failure is CancellationException) throw failure
        null
    }
    withContext(NonCancellable) {
        runCatching { medicationReminderScheduler.rescheduleAll() }
    }
    return ManualMedicineLogSaveOutcome(
        saveResult = null,
        postLogStockWarning = postLogStockWarning,
    )
}

internal data class ManualMedicineLogSaveOutcome(
    val saveResult: MedicineSlotDraftSaveResult?,
    val postLogStockWarning: PostLogStockWarning? = null,
) {
    val isSuccess: Boolean
        get() = saveResult == null
}
