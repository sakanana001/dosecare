package com.dosecare.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDao {
    @Transaction
    @Query(
        """
        SELECT * FROM medication_groups
        WHERE archivedAtEpochMillis IS NULL
        ORDER BY createdAtEpochMillis ASC, updatedAtEpochMillis DESC, uuid ASC
        """
    )
    fun observeActiveGroups(): Flow<List<MedicationGroupWithItemsEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM medication_groups
        WHERE archivedAtEpochMillis IS NULL
        ORDER BY createdAtEpochMillis ASC, updatedAtEpochMillis DESC, uuid ASC
        """
    )
    suspend fun getActiveGroups(): List<MedicationGroupWithItemsEntity>

    @Transaction
    @Query(
        """
        SELECT * FROM medication_groups
        WHERE archivedAtEpochMillis IS NOT NULL
        ORDER BY createdAtEpochMillis ASC, updatedAtEpochMillis DESC, uuid ASC
        """
    )
    fun observeArchivedGroups(): Flow<List<MedicationGroupWithItemsEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM medication_groups
        WHERE archivedAtEpochMillis IS NOT NULL
        ORDER BY createdAtEpochMillis ASC, updatedAtEpochMillis DESC, uuid ASC
        """
    )
    suspend fun getArchivedGroups(): List<MedicationGroupWithItemsEntity>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE (
            scheduledForIso IS NOT NULL
            AND scheduledForIso >= :scheduledStartIso
            AND scheduledForIso <= :scheduledEndIso
        ) OR (
            scheduledForIso IS NULL
            AND appliedAtEpochMillis >= :manualStartEpochMillis
            AND appliedAtEpochMillis < :manualEndEpochMillis
        )
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    fun observeScheduleEntries(
        scheduledStartIso: String,
        scheduledEndIso: String,
        manualStartEpochMillis: Long,
        manualEndEpochMillis: Long,
    ): Flow<List<MedicationLogEntryEntity>>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE (
            scheduledForIso IS NOT NULL
            AND scheduledForIso >= :scheduledStartIso
            AND scheduledForIso <= :scheduledEndIso
        ) OR (
            scheduledForIso IS NULL
            AND appliedAtEpochMillis >= :manualStartEpochMillis
            AND appliedAtEpochMillis < :manualEndEpochMillis
        )
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    suspend fun getScheduleEntries(
        scheduledStartIso: String,
        scheduledEndIso: String,
        manualStartEpochMillis: Long,
        manualEndEpochMillis: Long,
    ): List<MedicationLogEntryEntity>

    @Query(
        """
        SELECT
            uuid, category, medicineUuid, applicationType, doseInstructionKind,
            tabletFractionNumerator, tabletFractionDenominator, doseVolumeMl,
            doseWeightGrams, equivalentE2Mg, sourceGroupUuid, scheduleTimeUuid,
            appliedAtEpochMillis, appliedAtTimeZoneId, scheduledForIso, count,
            gelApplicationArea, doseAmountDelta, note
        FROM (
            SELECT *, ROW_NUMBER() OVER (
                PARTITION BY applicationType, medicineUuid, sourceGroupUuid,
                             doseInstructionKind, tabletFractionNumerator,
                             tabletFractionDenominator, doseVolumeMl, doseWeightGrams
                ORDER BY appliedAtEpochMillis DESC, uuid DESC
            ) AS rn
            FROM medication_log_entries
            WHERE category IN ('ANTIANDROGEN', 'SERM', 'GNRH_AGONIST')
              AND appliedAtEpochMillis <= :onOrBeforeEpochMillis
        )
        WHERE rn = 1
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    fun observeLatestAntiandrogenEntriesOnOrBefore(
        onOrBeforeEpochMillis: Long,
    ): Flow<List<MedicationLogEntryEntity>>

    @Query(
        """
        SELECT
            uuid, category, medicineUuid, applicationType, doseInstructionKind,
            tabletFractionNumerator, tabletFractionDenominator, doseVolumeMl,
            doseWeightGrams, equivalentE2Mg, sourceGroupUuid, scheduleTimeUuid,
            appliedAtEpochMillis, appliedAtTimeZoneId, scheduledForIso, count,
            gelApplicationArea, doseAmountDelta, note
        FROM (
            SELECT *, ROW_NUMBER() OVER (
                PARTITION BY applicationType, medicineUuid, sourceGroupUuid,
                             doseInstructionKind, tabletFractionNumerator,
                             tabletFractionDenominator, doseVolumeMl, doseWeightGrams
                ORDER BY appliedAtEpochMillis DESC, uuid DESC
            ) AS rn
            FROM medication_log_entries
            WHERE category IN ('ANTIANDROGEN', 'SERM', 'GNRH_AGONIST')
              AND appliedAtEpochMillis <= :onOrBeforeEpochMillis
        )
        WHERE rn = 1
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    suspend fun getLatestAntiandrogenEntriesOnOrBefore(
        onOrBeforeEpochMillis: Long,
    ): List<MedicationLogEntryEntity>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'ESTRADIOL'
          AND appliedAtEpochMillis >= :startEpochMillis
          AND appliedAtEpochMillis <= :endEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    fun observeEstradiolPkEntries(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): Flow<List<MedicationLogEntryEntity>>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'ESTRADIOL'
          AND appliedAtEpochMillis >= :startEpochMillis
          AND appliedAtEpochMillis <= :endEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    suspend fun getEstradiolPkEntries(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): List<MedicationLogEntryEntity>

    // F-PR-engine-snapshot: mirror the estradiol query for
    // testosterone and cyproterone acetate so the home snapshot
    // can carry T / CPA log entries alongside the E2 ones. The
    // snapshot path then has all three hormones cached, so a
    // user who flips the home chart to T (or CPA) doesn't pay
    // a Room round-trip for the first emission.
    //
    // CPA lives in the ANTIANDROGEN category alongside
    // spironolactone / bicalutamide / finasteride / dutasteride.
    // The simulator's toCpaPkDoseEvent does the second narrow
    // (medicationKey == CYPROTERONE_ACETATE) so we fetch the
    // wider ANTIANDROGEN set here and let the engine drop the
    // rest. Storage cost is negligible (a handful of
    // non-CPA rows per user at most).
    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'TESTOSTERONE'
          AND appliedAtEpochMillis >= :startEpochMillis
          AND appliedAtEpochMillis <= :endEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    suspend fun getTestosteronePkEntries(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): List<MedicationLogEntryEntity>

    // F-PR-snapshot-upgrade: Flow variant for the live Room path
    // so new T doses logged in-session propagate to the home
    // chart without a full snapshot refresh.
    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'TESTOSTERONE'
          AND appliedAtEpochMillis >= :startEpochMillis
          AND appliedAtEpochMillis <= :endEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    fun observeTestosteronePkEntries(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): Flow<List<MedicationLogEntryEntity>>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'ANTIANDROGEN'
          AND appliedAtEpochMillis >= :startEpochMillis
          AND appliedAtEpochMillis <= :endEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    suspend fun getCpaPkEntries(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): List<MedicationLogEntryEntity>

    // F-PR-snapshot-upgrade: Flow variant for the live Room path
    // (see observeTestosteronePkEntries comment above).
    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'ANTIANDROGEN'
          AND appliedAtEpochMillis >= :startEpochMillis
          AND appliedAtEpochMillis <= :endEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        """
    )
    fun observeCpaPkEntries(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): Flow<List<MedicationLogEntryEntity>>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'ESTRADIOL'
          AND appliedAtEpochMillis <= :onOrBeforeEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        LIMIT 1
        """
    )
    fun observeLatestEstradiolEntryOnOrBefore(
        onOrBeforeEpochMillis: Long,
    ): Flow<MedicationLogEntryEntity?>

    @Query(
        """
        SELECT * FROM medication_log_entries
        WHERE category = 'ESTRADIOL'
          AND appliedAtEpochMillis <= :onOrBeforeEpochMillis
        ORDER BY appliedAtEpochMillis DESC
        LIMIT 1
        """
    )
    suspend fun getLatestEstradiolEntryOnOrBefore(
        onOrBeforeEpochMillis: Long,
    ): MedicationLogEntryEntity?

    @Query(
        """
        SELECT * FROM user_profile
        WHERE id = :id
        LIMIT 1
        """
    )
    fun observeProfile(id: String = UserProfileEntity.SINGLETON_ID): Flow<UserProfileEntity?>
}
