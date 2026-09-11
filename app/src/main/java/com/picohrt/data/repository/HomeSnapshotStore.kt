package com.picohrt.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.picohrt.model.journal.AnchorIcon
import com.picohrt.model.journal.HeroBackground
import com.picohrt.model.journal.TrackedDate
import com.picohrt.model.medication.DoseInstruction
import com.picohrt.model.medication.DoseInstructionKind
import com.picohrt.model.medication.MedicationApplicationType
import com.picohrt.model.medication.MedicationCategory
import com.picohrt.model.medication.MedicationGroup
import com.picohrt.model.medication.MedicationGroupColorKey
import com.picohrt.model.medication.MedicationGroupMedication
import com.picohrt.model.medication.MedicationGroupSchedule
import com.picohrt.model.medication.MedicationGroupScheduleTime
import com.picohrt.model.medication.MedicationGroupScheduleType
import com.picohrt.model.medication.MedicationKey
import com.picohrt.model.medication.MedicationLogEntry
import com.picohrt.model.medication.MedicationSelectionKind
import com.picohrt.model.medication.Medicine
import com.picohrt.model.medication.MedicineDisplayDoseUnit
import com.picohrt.model.medication.MedicineIdentityKey
import com.picohrt.model.medication.MedicinePreparation
import com.picohrt.model.medication.MedicinePreparationType
import com.picohrt.model.medication.MedicineSelection
import com.picohrt.model.medication.MedicineStock
import com.picohrt.model.personalization.UserProfile
import com.picohrt.model.personalization.WeightUnit
import com.picohrt.model.pk.DenseSamplePolicy
import com.picohrt.model.pk.PkHormone
import com.picohrt.util.AppDiagnosticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.homeSnapshotDataStore: DataStore<HomeSnapshotState> by dataStore(
    fileName = "home_snapshot.pb",
    serializer = HomeSnapshotSerializer(AndroidHomeSnapshotCrypto()),
)

private val Context.homeSnapshotGenerationDataStore by preferencesDataStore(
    name = "home_snapshot_metadata",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
class HomeSnapshotStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun observeSnapshot(): Flow<HomeSnapshotRecord?> {
        return context.homeSnapshotDataStore.data
            .map { state -> state.record }
            .distinctUntilChanged()
    }

    suspend fun readSnapshot(): HomeSnapshotRecord? {
        return context.homeSnapshotDataStore.data.first().record
    }

    suspend fun writeSnapshot(record: HomeSnapshotRecord) {
        context.homeSnapshotDataStore.updateData {
            HomeSnapshotState(record = record)
        }
    }

    suspend fun clearSnapshot() {
        context.homeSnapshotDataStore.updateData {
            HomeSnapshotState.Empty
        }
    }
}

@Singleton
class HomeSnapshotGenerationStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun observeGeneration(): Flow<Long> {
        return context.homeSnapshotGenerationDataStore.data
            .map { preferences -> preferences[HOME_SNAPSHOT_GENERATION_KEY] ?: 0L }
            .distinctUntilChanged()
    }

    suspend fun readGeneration(): Long {
        return context.homeSnapshotGenerationDataStore.data
            .map { preferences -> preferences[HOME_SNAPSHOT_GENERATION_KEY] ?: 0L }
            .first()
    }

    suspend fun incrementGeneration(): Long {
        var nextGeneration = 0L
        context.homeSnapshotGenerationDataStore.edit { preferences ->
            nextGeneration = (preferences[HOME_SNAPSHOT_GENERATION_KEY] ?: 0L) + 1L
            preferences[HOME_SNAPSHOT_GENERATION_KEY] = nextGeneration
        }
        return nextGeneration
    }
}

data class HomeSnapshotRecord(
    val schemaVersion: Int,
    val generation: Long = 0L,
    val generatedAtEpochMillis: Long,
    val anchorDateEpochDay: Long,
    val zoneId: String,
    /**
     * Per-hormone PK projections over the home chart window, keyed by
     * [com.picohrt.model.pk.PkHormone]. The current renderer
     * picks the E2 entry for the home chart, but every other hormone
     * (T, CPA) the user has logged doses for is also cached here so
     * PR3/PR4 can switch the chart without an immediate refresh.
     *
     * PR2 codec: a snapshot may carry any subset of hormones; missing
     * entries are recomputed by the next home mutation. Values are
     * nullable to match the legacy [pkProjection] field shape and
     * keep test fixtures straightforward; the codec itself never
     * writes a null value (an entry round-trips a non-null record).
     */
    val pkProjections: Map<PkHormone, HomePkProjectionRecord?> = emptyMap(),
    // Sibling of the E2 [pkProjections] entry simulated with
    // plannedEntries=emptyList(). The widget reads this to display the
    // current body-state estimate from logged doses only — without it,
    // a planned dose 30 min from now would distort the widget's
    // "current" reading. Defaulted for old fixtures/caches.
    val widgetPkProjection: HomePkProjectionRecord? = null,
    /**
     * Back-compat shim. The pre-PR2 codec only cached a single E2
     * projection under this field. The codec writes the E2 entry of
     * [pkProjections] here as well so any code that still reads the
     * legacy field keeps working until PR3 retires it. New code should
     * use [pkProjections][PkHormone.ESTRADIOL] instead.
     */
    @Deprecated(
        "Use pkProjections[PkHormone.ESTRADIOL] instead.",
        ReplaceWith("pkProjections[com.picohrt.model.pk.PkHormone.ESTRADIOL]"),
    )
    val pkProjection: HomePkProjectionRecord? = null,
    val activeGroups: List<MedicationGroup>,
    /** Archived groups, carried so Home/widget can mirror the Plan page. Defaulted for old fixtures/caches. */
    val archivedGroups: List<MedicationGroup> = emptyList(),
    val scheduleEntries: List<MedicationLogEntry>,
    val antiandrogenHistoryEntries: List<MedicationLogEntry>,
    // Cached so Settings (and any other UserProfile consumer) can render the real
    // weight on first frame instead of flashing the empty default while Room cold
    // init completes. Nullable for forward-compat with snapshots from before this
    // field was added.
    val userProfile: UserProfile? = null,
    /** Every active tracked medicine, used so cold-start can re-project ungrouped meds. */
    val stockMedicines: List<Medicine> = emptyList(),
    /** Log entries whose `scheduledFor` falls inside the stock simulation window. */
    val stockFulfillmentEntries: List<MedicationLogEntry> = emptyList(),
    // Real estradiol log entries over the PK input window. Embedded so the
    // snapshot path can re-run the simulation locally — without waiting for
    // Room — when the cached projection expires (e.g. the user missed a
    // future planned dose). Encoded with medicine dedup since a daily doser
    // repeats the same one or two medicines across the whole window.
    //
    // PR2: this list is now keyed off [pkProjections].codec entry index;
    // the codec still embeds one pooled set per record for back-compat.
    val pkEntries: List<MedicationLogEntry> = emptyList(),
    // F-PR-snapshot-upgrade: T + CPA log entries over the same window.
    // Mirrors [pkEntries] but per-hormone so the T / CPA chart paths
    // can short-circuit the live Room flow on cold start. The
    // simulator's toTestosteronePkDoseEvent and toCpaPkDoseEvent
    // both do a category + medicationKey narrow, so carrying the
    // wider ANTIANDROGEN set under [cpaPkEntries] is safe.
    // Defaulted empty for pre-v24 codec back-compat.
    val testosteronePkEntries: List<MedicationLogEntry> = emptyList(),
    val cpaPkEntries: List<MedicationLogEntry> = emptyList(),
    // v2 血检追踪 (BT-PR3): recent blood test panels cached so the
    // "Recent Bloodwork" card renders a non-empty trend / chip on
    // cold start instead of waiting for the live Room flow to
    // re-emit. The card only needs the per-result canonical
    // value + collectedAt timestamp + (for the chip) the analyte
    // key + unit label, so we store a flat record instead of the
    // full BloodTestPanel/BloodTestResult model. The repository
    // rebuilds the full model from these when serving the home
    // screen; the missing fields (notes, timeSinceLastDoseMillis,
    // custom-analyte name, etc.) are not used by the card.
    // Defaulted empty for pre-v25 codec back-compat.
    val bloodTestPanels: List<HomeSnapshotBloodPanelRecord> = emptyList(),
    // First pinned tracked date (the Home hero anchor), cached so cold start can
    // render the hero on the first frame instead of waiting on Room. Null when no
    // date is pinned. Defaulted for forward-compat with pre-field snapshots.
    val homeAnchor: TrackedDate? = null,
)

/**
 * v2 血检追踪 (BT-PR3): a flat, codec-friendly projection of a
 * [com.picohrt.model.bloodtest.BloodTestPanel] scoped to the
 * fields the home bloodwork card actually reads (collected-at
 * timestamp + per-result canonical value). We deliberately do NOT
 * embed the full [com.picohrt.model.bloodtest.BloodTestPanel]
 * model because the panel entity carries heavy fields (notes, dose
 * timing, custom-analyte name+unitLabel) the card never touches;
 * shipping the full model would bloat the snapshot for no win.
 *
 * The repository reconstructs the full model from this projection
 * when the home screen asks for it, so the card builder keeps
 * consuming the same [com.picohrt.model.bloodtest.BloodTestPanel]
 * type and stays unaware of the snapshot transport.
 */
data class HomeSnapshotBloodPanelRecord(
    val panelUuid: UUID,
    val collectedAtEpochMillis: Long,
    val collectedAtTimeZoneId: String,
    val results: List<HomeSnapshotBloodResultRecord>,
)

/**
 * v2 血检追踪 (BT-PR3): a flat, codec-friendly projection of a
 * [com.picohrt.model.bloodtest.BloodTestResult]. builtinKey
 * is the analyte's storage value (e.g. "t" / "hb") or null when
 * the result is a custom analyte. canonicalValue is the value
 * already converted to the analyte's canonical unit by the
 * repository before the snapshot is written, so the card builder
 * can compare it directly against the per-goal reference
 * profile without a second unit-conversion pass.
 */
data class HomeSnapshotBloodResultRecord(
    val resultUuid: UUID,
    val builtinAnalyteKey: String?,
    val canonicalValue: Double,
    val unitSnapshot: String,
    val displayOrder: Int,
)

data class HomePkProjectionRecord(
    /**
     * Which hormone this projection is for. The pre-PR2 codec did not
     * carry this field; the reader infers [com.picohrt.model.pk.PkHormone.ESTRADIOL]
     * when it is missing for back-compat. New writes always set it.
     */
    val hormone: PkHormone,
    val generatedAtEpochMillis: Long,
    val windowStartEpochMillis: Long,
    val windowEndEpochMillis: Long,
    // Wall-clock instant past which the projection is considered stale —
    // typically the soonest planned dose time strictly after generatedAt.
    // Once now crosses this, consumers should treat the projection as
    // missing and request a refresh, because the cached curve still shows
    // doses the user may have missed.
    val pkProjectionExpiresAtEpochMillis: Long,
    val concentrationUnit: String,
    val timeH: List<Double>,
    val concentrations: List<Double>,
    val doseMarkers: List<HomePkProjectionDoseMarkerRecord>,
    /**
     * Latest log entry of the matching [hormone] over the PK input window.
     * Nullable when the user has not logged any dose of this hormone in
     * the input window. Pre-PR2 code named this [latestEstradiolEntry]
     * and assumed E2; new code should read [latestEntryByHormone] (when
     * present) or this field by [hormone].
     */
    val latestEntryByHormone: MedicationLogEntry?,
    /**
     * Back-compat shim. The pre-PR2 codec only carried the E2 latest
     * entry under this name. The reader falls back to
     * [latestEntryByHormone] when this is null. New writes always set
     * both fields to the same value (the E2 latest entry, when present).
     */
    @Deprecated(
        "Use latestEntryByHormone instead. This field always carries the E2 latest entry for back-compat.",
        ReplaceWith("latestEntryByHormone"),
    )
    val latestEstradiolEntry: MedicationLogEntry? = null,
    // Sampling fingerprint: validator compares these against the resolved
    // HomeE2ChartWindowOption rather than the enum name, so option renames
    // do not invalidate caches whose sampling recipe is unchanged.
    val chartWindowHours: Int,
    val densePolicy: HomePkDenseSamplePolicyRecord,
    val includesPostDoseOffsets: Boolean,
)

data class HomePkProjectionDoseMarkerRecord(
    val timeH: Double,
    val concentration: Double,
    val isPlanned: Boolean = false,
)

sealed interface HomePkDenseSamplePolicyRecord {
    data class Interval(val hours: Double) : HomePkDenseSamplePolicyRecord
    data class Budget(val segmentCount: Int) : HomePkDenseSamplePolicyRecord
}

fun DenseSamplePolicy.toRecord(): HomePkDenseSamplePolicyRecord = when (this) {
    is DenseSamplePolicy.Interval -> HomePkDenseSamplePolicyRecord.Interval(hours = hours)
    is DenseSamplePolicy.Budget -> HomePkDenseSamplePolicyRecord.Budget(segmentCount = segmentCount)
}

internal data class HomeSnapshotState(
    val record: HomeSnapshotRecord?,
) {
    companion object {
        val Empty = HomeSnapshotState(record = null)
    }
}

internal class HomeSnapshotSerializer(
    private val crypto: HomeSnapshotCrypto,
    private val diagnosticsLogger: AppDiagnosticsLogger = AppDiagnosticsLogger(),
) : Serializer<HomeSnapshotState> {
    override val defaultValue: HomeSnapshotState = HomeSnapshotState.Empty

    override suspend fun readFrom(input: InputStream): HomeSnapshotState {
        val encryptedBytes = input.readBytes()
        if (encryptedBytes.isEmpty()) {
            return HomeSnapshotState.Empty
        }
        return runCatching {
            HomeSnapshotState(
                record = HomeSnapshotCodec.decode(
                    crypto.decrypt(encryptedBytes)
                )
            )
        }.getOrElse { throwable ->
            diagnosticsLogger.warning(
                TAG,
                "home_snapshot_read_failed bytes=${encryptedBytes.size}",
                throwable,
            )
            HomeSnapshotState.Empty
        }
    }

    override suspend fun writeTo(
        t: HomeSnapshotState,
        output: OutputStream,
    ) {
        val record = t.record ?: return
        output.write(
            crypto.encrypt(
                HomeSnapshotCodec.encode(record)
            )
        )
    }
}

internal object HomeSnapshotCodec {
    fun encode(record: HomeSnapshotRecord): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            stream.writeInt(SNAPSHOT_CODEC_VERSION)
            stream.writeInt(record.schemaVersion)
            stream.writeLong(record.generation)
            stream.writeLong(record.generatedAtEpochMillis)
            stream.writeLong(record.anchorDateEpochDay)
            stream.writeString(record.zoneId)
            // PR2: write the per-hormone projection map. The deprecated
            // `pkProjection` E2-only field is no longer serialized;
            // readers back-fill it from `pkProjections[ESTRADIOL]`.
            stream.writeInt(record.pkProjections.size)
            record.pkProjections.forEach { (hormone, projection) ->
                stream.writeInt(hormone.ordinal)
                stream.writeHomePkProjectionRecord(projection)
            }
            stream.writeHomePkProjectionRecord(record.widgetPkProjection)
            stream.writeList(record.activeGroups) { group -> writeMedicationGroup(group) }
            stream.writeList(record.archivedGroups) { group -> writeMedicationGroup(group) }
            stream.writeList(record.scheduleEntries) { entry -> writeMedicationLogEntry(entry) }
            stream.writeList(record.antiandrogenHistoryEntries) { entry ->
                writeMedicationLogEntry(
                    entry
                )
            }
            stream.writeUserProfile(record.userProfile)
            stream.writeList(record.stockMedicines) { medicine -> writeMedicine(medicine) }
            stream.writeList(record.stockFulfillmentEntries) { entry ->
                writeMedicationLogEntry(
                    entry
                )
            }
            stream.writePooledMedicationLogEntries(record.pkEntries)
            // F-PR-snapshot-upgrade: T + CPA entries written AFTER
            // [pkEntries] so the v23 reader would error on v24 bytes
            // (intentional — the snapshot path fails fast, the room
            // fallback then renders a default chart).
            stream.writePooledMedicationLogEntries(record.testosteronePkEntries)
            stream.writePooledMedicationLogEntries(record.cpaPkEntries)
            // BT-PR3: blood test panels. Written AFTER the v24
            // block so a v24 reader on v25 bytes errors
            // intentionally (the snapshot path fails fast, then the
            // repository runs a fresh refresh).
            stream.writeList(record.bloodTestPanels) { panel -> writeBloodPanelRecord(panel) }
            stream.writeTrackedDate(record.homeAnchor)
        }
        return output.toByteArray()
    }

    fun decode(bytes: ByteArray): HomeSnapshotRecord {
        return DataInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val version = stream.readInt()
            require(version == SNAPSHOT_CODEC_VERSION) {
                "Unsupported Home snapshot version: $version."
            }
            val schemaVersion = stream.readInt()
            val generation = stream.readLong()
            val generatedAtEpochMillis = stream.readLong()
            val anchorDateEpochDay = stream.readLong()
            val zoneId = stream.readString()
            // PR2: per-hormone projection map.
            val pkProjectionsSize = stream.readInt()
            val pkProjections = LinkedHashMap<PkHormone, HomePkProjectionRecord?>(pkProjectionsSize)
            repeat(pkProjectionsSize) {
                val hormone = PkHormone.entries[stream.readInt()]
                val projection = stream.readHomePkProjectionRecord()
                    ?: error("pkProjections entry had a null record at hormone=$hormone")
                pkProjections[hormone] = projection
            }
            val widgetPkProjection = stream.readHomePkProjectionRecord()
            // PR2: the deprecated E2-only `pkProjection` field is no
            // longer back-filled. Callers read `pkProjections[ESTRADIOL]`
            // instead. The field stays in the data class for source
            // compatibility with code that still touches it (e.g. the
            // `pkProjection` widget consumer will be migrated in a
            // follow-up).
            HomeSnapshotRecord(
                schemaVersion = schemaVersion,
                generation = generation,
                generatedAtEpochMillis = generatedAtEpochMillis,
                anchorDateEpochDay = anchorDateEpochDay,
                zoneId = zoneId,
                pkProjections = pkProjections,
                widgetPkProjection = widgetPkProjection,
                pkProjection = null,
                activeGroups = stream.readList { readMedicationGroup() },
                archivedGroups = stream.readList { readMedicationGroup() },
                scheduleEntries = stream.readList { checkNotNull(readMedicationLogEntry()) },
                antiandrogenHistoryEntries = stream.readList { checkNotNull(readMedicationLogEntry()) },
                userProfile = stream.readUserProfile(),
                stockMedicines = stream.readList { readMedicine() },
                stockFulfillmentEntries = stream.readList { checkNotNull(readMedicationLogEntry()) },
                pkEntries = stream.readPooledMedicationLogEntries(),
                // F-PR-snapshot-upgrade: T + CPA entries follow pkEntries
                // in the v24 codec. A v24 reader on v23 bytes would
                // throw because the byte stream is short; we let
                // the codec require() surface that and fall back to
                // a fresh snapshot, which is the right behavior
                // anyway (the user just upgraded).
                testosteronePkEntries = stream.readPooledMedicationLogEntries(),
                cpaPkEntries = stream.readPooledMedicationLogEntries(),
                // BT-PR3: blood test panels follow the v24 block in
                // the v25 codec. Defaulted to empty on a v24 reader
                // path because the strict version check above
                // surfaces the mismatch before this point.
                bloodTestPanels = stream.readList { readBloodPanelRecord() },
                homeAnchor = stream.readTrackedDate(),
            )
        }
    }

    private fun DataOutputStream.writeHomePkProjectionRecord(record: HomePkProjectionRecord?) {
        writeBoolean(record != null)
        record ?: return
        // PR2: write the hormone ordinal first so the reader can
        // disambiguate the projection by hormone. Pre-PR2 codec writes
        // (the E2-only fields) without this leading int.
        writeInt(record.hormone.ordinal)
        writeLong(record.generatedAtEpochMillis)
        writeLong(record.windowStartEpochMillis)
        writeLong(record.windowEndEpochMillis)
        writeLong(record.pkProjectionExpiresAtEpochMillis)
        writeString(record.concentrationUnit)
        writeList(record.timeH) { value -> writeDouble(value) }
        writeList(record.concentrations) { value -> writeDouble(value) }
        writeList(record.doseMarkers) { marker ->
            writeDouble(marker.timeH)
            writeDouble(marker.concentration)
            writeBoolean(marker.isPlanned)
        }
        // PR2: prefer latestEntryByHormone when present; fall back to
        // latestEstradiolEntry for code paths that haven't been updated
        // yet. The reader mirrors the same fallback.
        val latest = record.latestEntryByHormone ?: record.latestEstradiolEntry
        writeMedicationLogEntry(latest)
        writeInt(record.chartWindowHours)
        writeDenseSamplePolicy(record.densePolicy)
        writeBoolean(record.includesPostDoseOffsets)
    }

    private fun DataOutputStream.writeDenseSamplePolicy(policy: HomePkDenseSamplePolicyRecord) {
        when (policy) {
            is HomePkDenseSamplePolicyRecord.Interval -> {
                writeByte(POLICY_DISCRIMINATOR_INTERVAL)
                writeDouble(policy.hours)
            }

            is HomePkDenseSamplePolicyRecord.Budget -> {
                writeByte(POLICY_DISCRIMINATOR_BUDGET)
                writeInt(policy.segmentCount)
            }
        }
    }

    private fun DataInputStream.readDenseSamplePolicy(): HomePkDenseSamplePolicyRecord {
        return when (val discriminator = readByte().toInt()) {
            POLICY_DISCRIMINATOR_INTERVAL -> HomePkDenseSamplePolicyRecord.Interval(hours = readDouble())
            POLICY_DISCRIMINATOR_BUDGET -> HomePkDenseSamplePolicyRecord.Budget(segmentCount = readInt())
            else -> error("Unknown dense sample policy discriminator: $discriminator")
        }
    }

    private fun DataInputStream.readHomePkProjectionRecord(): HomePkProjectionRecord? {
        if (!readBoolean()) {
            return null
        }
        // PR2: read the hormone ordinal first. The reader is only used
        // from codec 23 onwards, so the ordinal is always present.
        val hormone = PkHormone.entries[readInt()]
        val generatedAtEpochMillis = readLong()
        val windowStartEpochMillis = readLong()
        val windowEndEpochMillis = readLong()
        val pkProjectionExpiresAtEpochMillis = readLong()
        val concentrationUnit = readString()
        val timeH = readList { readDouble() }
        val concentrations = readList { readDouble() }
        val doseMarkers = readList {
            HomePkProjectionDoseMarkerRecord(
                timeH = readDouble(),
                concentration = readDouble(),
                isPlanned = readBoolean(),
            )
        }
        // PR2: prefer latestEntryByHormone; mirror to the deprecated
        // E2-only field for legacy readers. Non-E2 projections leave it
        // null.
        val latestEntry = readMedicationLogEntry()
        return HomePkProjectionRecord(
            hormone = hormone,
            generatedAtEpochMillis = generatedAtEpochMillis,
            windowStartEpochMillis = windowStartEpochMillis,
            windowEndEpochMillis = windowEndEpochMillis,
            pkProjectionExpiresAtEpochMillis = pkProjectionExpiresAtEpochMillis,
            concentrationUnit = concentrationUnit,
            timeH = timeH,
            concentrations = concentrations,
            doseMarkers = doseMarkers,
            latestEntryByHormone = latestEntry,
            latestEstradiolEntry = if (hormone == PkHormone.ESTRADIOL) latestEntry else null,
            chartWindowHours = readInt(),
            densePolicy = readDenseSamplePolicy(),
            includesPostDoseOffsets = readBoolean(),
        )
    }

    private fun DataOutputStream.writeMedicationGroup(group: MedicationGroup) {
        writeString(group.uuid.toString())
        writeString(group.name)
        writeString(group.colorKey.name)
        writeMedicationGroupSchedule(group.schedule)
        writeList(group.medications) { medication -> writeMedicationGroupMedication(medication) }
        writeBoolean(group.notificationsEnabled)
        writeLong(group.createdAt.toEpochMilli())
        writeLong(group.updatedAt.toEpochMilli())
        writeNullableLong(group.archivedAt?.toEpochMilli())
        writeNullableString(group.archivedAtLocal?.toString())
        writeBoolean(group.includePastScheduledSlots)
        writeNullableString(group.replacedByGroupUuid?.toString())
        writeNullableString(group.recreatedFromGroupUuid?.toString())
    }

    private fun DataInputStream.readMedicationGroup(): MedicationGroup {
        return MedicationGroup(
            uuid = UUID.fromString(readString()),
            name = readString(),
            colorKey = MedicationGroupColorKey.fromStorageValue(readString()),
            schedule = readMedicationGroupSchedule(),
            medications = readList { readMedicationGroupMedication() },
            notificationsEnabled = readBoolean(),
            createdAt = Instant.ofEpochMilli(readLong()),
            updatedAt = Instant.ofEpochMilli(readLong()),
            archivedAt = readNullableLong()?.let(Instant::ofEpochMilli),
            archivedAtLocal = readNullableString()?.let(LocalDateTime::parse),
            includePastScheduledSlots = readBoolean(),
            replacedByGroupUuid = readNullableString()?.let(UUID::fromString),
            recreatedFromGroupUuid = readNullableString()?.let(UUID::fromString),
        )
    }

    private fun DataOutputStream.writeMedicationGroupSchedule(schedule: MedicationGroupSchedule) {
        writeString(schedule.type.name)
        writeInt(schedule.interval)
        writeLong(schedule.since.toEpochDay())
        writeList(schedule.weeklyDaysOfWeek.sortedBy(DayOfWeek::getValue)) { day ->
            writeInt(day.value)
        }
        writeList(schedule.times) { time -> writeLocalTime(time) }
        writeList(schedule.timeSlots) { timeSlot -> writeMedicationGroupScheduleTime(timeSlot) }
    }

    private fun DataInputStream.readMedicationGroupSchedule(): MedicationGroupSchedule {
        return MedicationGroupSchedule(
            type = MedicationGroupScheduleType.fromStorageValue(readString()),
            interval = readInt(),
            since = LocalDate.ofEpochDay(readLong()),
            weeklyDaysOfWeek = readList { DayOfWeek.of(readInt()) }.toSet(),
            times = readList { readLocalTime() },
            timeSlots = readList { readMedicationGroupScheduleTime() },
        )
    }

    private fun DataOutputStream.writeMedicationGroupScheduleTime(time: MedicationGroupScheduleTime) {
        writeString(time.uuid.toString())
        writeLocalTime(time.time)
        writeString(time.effectiveFrom.toString())
    }

    private fun DataInputStream.readMedicationGroupScheduleTime(): MedicationGroupScheduleTime {
        return MedicationGroupScheduleTime(
            uuid = UUID.fromString(readString()),
            time = readLocalTime(),
            effectiveFrom = LocalDateTime.parse(readString()),
        )
    }

    private fun DataOutputStream.writeMedicationGroupMedication(medication: MedicationGroupMedication) {
        writeString(medication.uuid.toString())
        writeNullableMedicine(medication.medicine)
        writeString(medication.applicationType.name)
        writeDoseInstruction(medication.doseInstruction)
        writeInt(medication.count)
    }

    private fun DataInputStream.readMedicationGroupMedication(): MedicationGroupMedication {
        return MedicationGroupMedication(
            uuid = UUID.fromString(readString()),
            medicine = readNullableMedicine(),
            applicationType = MedicationApplicationType.fromStorageValue(readString()),
            doseInstruction = readDoseInstruction(),
            count = readInt(),
        )
    }

    private fun DataOutputStream.writeMedicationLogEntry(entry: MedicationLogEntry?) {
        writeBoolean(entry != null)
        entry ?: return
        writeString(entry.uuid.toString())
        writeNullableMedicine(entry.medicine)
        writeMedicationLogEntryBody(entry)
    }

    private fun DataInputStream.readMedicationLogEntry(): MedicationLogEntry? {
        if (!readBoolean()) {
            return null
        }
        val uuid = UUID.fromString(readString())
        val medicine = readNullableMedicine()
        return readMedicationLogEntryBody(uuid = uuid, medicine = medicine)
    }

    // Writes a non-null entry list using a deduplicated medicine pool: each
    // distinct medicine is serialized once, and entries reference it by pool
    // index (-1 = no medicine). A daily doser embeds ~200 entries that repeat
    // the same medicine, so inlining each one would roughly double the cost.
    private fun DataOutputStream.writePooledMedicationLogEntries(entries: List<MedicationLogEntry>) {
        val medicinePool = entries.mapNotNull { entry -> entry.medicine }.distinctBy { it.uuid }
        val indexByUuid =
            medicinePool.withIndex().associate { (index, medicine) -> medicine.uuid to index }
        writeList(medicinePool) { medicine -> writeMedicine(medicine) }
        writeList(entries) { entry ->
            writeString(entry.uuid.toString())
            writeInt(entry.medicine?.let { medicine -> indexByUuid.getValue(medicine.uuid) } ?: -1)
            writeMedicationLogEntryBody(entry)
        }
    }

    private fun DataInputStream.readPooledMedicationLogEntries(): List<MedicationLogEntry> {
        val medicinePool = readList { readMedicine() }
        return readList {
            val uuid = UUID.fromString(readString())
            val medicineIndex = readInt()
            val medicine = medicinePool.getOrNull(medicineIndex)
            readMedicationLogEntryBody(uuid = uuid, medicine = medicine)
        }
    }

    // BT-PR3: blood panel codec. Flat, no pooling — blood tests are
    // sparse (quarterly at most), so a small inline list beats the
    // complexity of a custom-analyte pool. The panel UUID is
    // sufficient for the home card (it ignores the UUID, but the
    // model requires a non-null value), and per-result data is
    // self-describing (builtinKey nullable for custom analytes).
    private fun DataOutputStream.writeBloodPanelRecord(panel: HomeSnapshotBloodPanelRecord) {
        writeString(panel.panelUuid.toString())
        writeLong(panel.collectedAtEpochMillis)
        writeString(panel.collectedAtTimeZoneId)
        writeList(panel.results) { result -> writeBloodResultRecord(result) }
    }

    private fun DataInputStream.readBloodPanelRecord(): HomeSnapshotBloodPanelRecord {
        return HomeSnapshotBloodPanelRecord(
            panelUuid = UUID.fromString(readString()),
            collectedAtEpochMillis = readLong(),
            collectedAtTimeZoneId = readString(),
            results = readList { readBloodResultRecord() },
        )
    }

    private fun DataOutputStream.writeBloodResultRecord(result: HomeSnapshotBloodResultRecord) {
        writeString(result.resultUuid.toString())
        writeNullableString(result.builtinAnalyteKey)
        writeDouble(result.canonicalValue)
        writeString(result.unitSnapshot)
        writeInt(result.displayOrder)
    }

    private fun DataInputStream.readBloodResultRecord(): HomeSnapshotBloodResultRecord {
        return HomeSnapshotBloodResultRecord(
            resultUuid = UUID.fromString(readString()),
            builtinAnalyteKey = readNullableString(),
            canonicalValue = readDouble(),
            unitSnapshot = readString(),
            displayOrder = readInt(),
        )
    }

    private fun DataOutputStream.writeTrackedDate(date: TrackedDate?) {
        writeBoolean(date != null)
        date ?: return
        writeString(date.id)
        writeString(date.name)
        writeString(date.icon.storageKey)
        writeLong(date.date.toEpochDay())
        writeNullableString(date.palette?.name)
        writeNullableString(date.heroBackground.storageKey)
        writeBoolean(date.pinnedOrder != null)
        date.pinnedOrder?.let { writeInt(it) }
        writeLong(date.createdAtEpochMillis)
    }

    private fun DataInputStream.readTrackedDate(): TrackedDate? {
        if (!readBoolean()) return null
        return TrackedDate(
            id = readString(),
            name = readString(),
            icon = AnchorIcon.fromStorageValue(readString()),
            date = LocalDate.ofEpochDay(readLong()),
            palette = readNullableString()?.let(MedicationGroupColorKey::fromStorageValueOrNull),
            heroBackground = HeroBackground.fromStorageValue(readNullableString()),
            pinnedOrder = if (readBoolean()) readInt() else null,
            createdAtEpochMillis = readLong(),
        )
    }

    private fun DataOutputStream.writeMedicationLogEntryBody(entry: MedicationLogEntry) {
        writeString(entry.category.name)
        writeString(entry.applicationType.name)
        writeDoseInstruction(entry.doseInstruction)
        writeNullableDouble(entry.equivalentE2Mg)
        writeNullableString(entry.sourceGroupUuid?.toString())
        writeNullableString(entry.scheduleTimeUuid?.toString())
        writeLong(entry.appliedAt.toEpochMilli())
        writeString(entry.appliedAtTimeZoneId)
        writeNullableString(entry.scheduledFor?.toString())
        writeInt(entry.count)
        writeNullableDouble(entry.doseAmountDelta)
        writeNullableString(entry.importSourceApp)
        writeNullableString(entry.importExternalId)
    }

    private fun DataInputStream.readMedicationLogEntryBody(
        uuid: UUID,
        medicine: Medicine?,
    ): MedicationLogEntry {
        return MedicationLogEntry(
            uuid = uuid,
            medicine = medicine,
            category = MedicationCategory.fromStorageValue(readString()),
            applicationType = MedicationApplicationType.fromStorageValue(readString()),
            doseInstruction = readDoseInstruction(),
            equivalentE2Mg = readNullableDouble(),
            sourceGroupUuid = readNullableString()?.let(UUID::fromString),
            scheduleTimeUuid = readNullableString()?.let(UUID::fromString),
            appliedAt = Instant.ofEpochMilli(readLong()),
            appliedAtTimeZoneId = readString(),
            scheduledFor = readNullableString()?.let(LocalDateTime::parse),
            count = readInt(),
            doseAmountDelta = readNullableDouble(),
            importSourceApp = readNullableString(),
            importExternalId = readNullableString(),
        )
    }

    private fun DataOutputStream.writeNullableMedicine(medicine: Medicine?) {
        writeBoolean(medicine != null)
        medicine ?: return
        writeMedicine(medicine)
    }

    private fun DataInputStream.readNullableMedicine(): Medicine? {
        if (!readBoolean()) {
            return null
        }
        return readMedicine()
    }

    private fun DataOutputStream.writeMedicine(medicine: Medicine) {
        writeString(medicine.uuid.toString())
        writeMedicineSelection(medicine.selection)
        writeString(medicine.category.name)
        writeMedicinePreparation(medicine.preparation)
        writeNullableString(medicine.displayName)
        writeString(medicine.identityKey)
        writeBoolean(medicine.importedFromExternalTracker)
        writeLong(medicine.createdAt.toEpochMilli())
        writeLong(medicine.updatedAt.toEpochMilli())
        writeNullableLong(medicine.archivedAt?.toEpochMilli())
        writeString(medicine.displayDoseUnit.name)
        writeMedicineStock(medicine.stock)
    }

    private fun DataInputStream.readMedicine(): Medicine {
        val uuid = UUID.fromString(readString())
        val rawSelection = readMedicineSelection()
        val category = MedicationCategory.fromStorageValue(readString())
        val preparation = readMedicinePreparation()
        val displayName = readNullableString()
        val storedIdentityKey = readString()
        val importedFromExternalTracker = readBoolean()
        val createdAt = Instant.ofEpochMilli(readLong())
        val updatedAt = Instant.ofEpochMilli(readLong())
        val archivedAt = readNullableLong()?.let(Instant::ofEpochMilli)
        val displayDoseUnit = MedicineDisplayDoseUnit.fromStorageValue(readString())
        val stock = readMedicineStock()
        // PATCH_OFF preparation is the discriminator for the sentinel selection,
        // mirroring MedicineEntityMappers.toMedicineModel — the cache writer
        // emits CATALOG selectionKind for storage convenience and we fix it up
        // here on read so a round-tripped PatchOff medicine survives intact.
        val selection = if (preparation is MedicinePreparation.PatchOff) {
            MedicineSelection.PatchOff
        } else {
            rawSelection
        }
        if (importedFromExternalTracker && storedIdentityKey.startsWith("E|")) {
            // Imported keys embed sourceApp + route, which are not recoverable
            // here; verify the dose-key segment, which is derivable from the
            // preparation, so a cached key cannot disagree with the dose it backs.
            check(
                storedIdentityKey.substringAfterLast("|") ==
                    MedicineIdentityKey.importedDoseKey(preparation)
            ) {
                "Cached imported medicine $uuid dose key does not match its preparation."
            }
        } else {
            val expectedIdentityKey = when (selection) {
                is MedicineSelection.Catalog ->
                    MedicineIdentityKey.catalog(selection.medicationKey, preparation)

                is MedicineSelection.Custom ->
                    MedicineIdentityKey.custom(selection.medicationName, preparation)

                is MedicineSelection.PatchOff -> MedicineIdentityKey.patchOff()
            }
            check(storedIdentityKey == expectedIdentityKey) {
                "Cached medicine $uuid identityKey does not match selection and preparation."
            }
        }
        return Medicine(
            uuid = uuid,
            selection = selection,
            category = category,
            preparation = preparation,
            displayName = displayName,
            identityKey = storedIdentityKey,
            createdAt = createdAt,
            updatedAt = updatedAt,
            archivedAt = archivedAt,
            displayDoseUnit = displayDoseUnit,
            stock = stock.normalizedFor(preparation),
            importedFromExternalTracker = importedFromExternalTracker,
        )
    }

    private fun DataOutputStream.writeMedicineStock(stock: MedicineStock) {
        writeBoolean(stock.trackingEnabled)
        writeNullableDouble(stock.unitsRemaining)
        writeNullableDouble(stock.unitsLastTotal)
        writeNullableDouble(stock.openContainerAmount)
        writeInt(stock.warnAtDaysRemaining)
        writeLong(stock.generation)
    }

    private fun DataInputStream.readMedicineStock(): MedicineStock {
        return MedicineStock(
            trackingEnabled = readBoolean(),
            unitsRemaining = readNullableDouble(),
            unitsLastTotal = readNullableDouble(),
            openContainerAmount = readNullableDouble(),
            warnAtDaysRemaining = readInt(),
            generation = readLong(),
        )
    }

    private fun DataOutputStream.writeMedicineSelection(selection: MedicineSelection) {
        writeString(selection.kind.name)
        when (selection) {
            is MedicineSelection.Catalog -> writeString(selection.medicationKey.name)
            is MedicineSelection.Custom -> writeString(selection.medicationName)
            // PATCH_OFF reuses the CATALOG kind tag for storage; the read path
            // recognises it from the PatchOff preparation marker. Write a
            // placeholder string for the catalog branch's medicationKey slot so
            // the binary layout matches the catalog read.
            is MedicineSelection.PatchOff -> writeString(PATCH_OFF_SELECTION_MARKER)
        }
    }

    private fun DataInputStream.readMedicineSelection(): MedicineSelection {
        return when (MedicationSelectionKind.fromStorageValue(readString())) {
            MedicationSelectionKind.CATALOG -> {
                val keyName = readString()
                if (keyName == PATCH_OFF_SELECTION_MARKER) {
                    MedicineSelection.PatchOff
                } else {
                    MedicineSelection.Catalog(
                        medicationKey = checkNotNull(MedicationKey.fromStorageValue(keyName))
                    )
                }
            }

            MedicationSelectionKind.CUSTOM -> MedicineSelection.Custom(
                medicationName = readString()
            )
        }
    }

    private fun DataOutputStream.writeMedicinePreparation(preparation: MedicinePreparation) {
        writeString(preparation.type.name)
        when (preparation) {
            is MedicinePreparation.Pill -> writeDouble(preparation.strengthMgPerTablet)
            is MedicinePreparation.Capsule -> writeDouble(preparation.strengthMgPerCapsule)
            is MedicinePreparation.InjectionSingleUseVial -> writeDouble(preparation.strengthMgPerVial)
            is MedicinePreparation.InjectionMultiUseVial -> {
                writeDouble(preparation.concentrationMgPerMl)
                writeDouble(preparation.vialVolumeMl)
            }

            is MedicinePreparation.GelSachet -> {
                writeDouble(preparation.concentrationPercent)
                writeDouble(preparation.sachetWeightGrams)
            }

            is MedicinePreparation.GelContainer -> {
                writeDouble(preparation.concentrationPercent)
                writeDouble(preparation.containerWeightGrams)
            }

            is MedicinePreparation.ImportedInjection -> {
                writeDouble(preparation.administeredMg)
                writeString(preparation.ester.name)
            }

            is MedicinePreparation.ImportedGel -> {
                writeDouble(preparation.appliedEstradiolMg)
            }

            is MedicinePreparation.Patch -> when (val specification = preparation.specification) {
                is MedicinePreparation.PatchSpecification.TotalMg -> {
                    writeByte(PATCH_SPECIFICATION_TOTAL_MG)
                    writeDouble(specification.valueMg)
                }

                is MedicinePreparation.PatchSpecification.ReleaseRateMcgPerDay -> {
                    writeByte(PATCH_SPECIFICATION_RELEASE_RATE)
                    writeDouble(specification.valueMcgPerDay)
                }
            }
            // No payload — the type tag (PATCH_OFF) is enough to round-trip.
            is MedicinePreparation.PatchOff -> Unit
        }
    }

    private fun DataInputStream.readMedicinePreparation(): MedicinePreparation {
        return when (MedicinePreparationType.fromStorageValue(readString())) {
            MedicinePreparationType.PILL -> MedicinePreparation.Pill(
                strengthMgPerTablet = readDouble()
            )

            MedicinePreparationType.CAPSULE -> MedicinePreparation.Capsule(
                strengthMgPerCapsule = readDouble()
            )

            MedicinePreparationType.INJECTION_SINGLE_USE_VIAL -> MedicinePreparation.InjectionSingleUseVial(
                strengthMgPerVial = readDouble()
            )

            MedicinePreparationType.INJECTION_MULTI_USE_VIAL -> MedicinePreparation.InjectionMultiUseVial(
                concentrationMgPerMl = readDouble(),
                vialVolumeMl = readDouble(),
            )

            MedicinePreparationType.GEL_SACHET -> MedicinePreparation.GelSachet(
                concentrationPercent = readDouble(),
                sachetWeightGrams = readDouble(),
            )

            MedicinePreparationType.GEL_CONTAINER -> MedicinePreparation.GelContainer(
                concentrationPercent = readDouble(),
                containerWeightGrams = readDouble(),
            )

            MedicinePreparationType.IMPORTED_INJECTION -> MedicinePreparation.ImportedInjection(
                administeredMg = readDouble(),
                ester = checkNotNull(MedicationKey.fromStorageValue(readString())) {
                    "Imported injection snapshot medicine is missing ester medicationKey."
                },
            )

            MedicinePreparationType.IMPORTED_GEL -> MedicinePreparation.ImportedGel(
                appliedEstradiolMg = readDouble(),
            )

            MedicinePreparationType.PATCH -> MedicinePreparation.Patch(
                specification = when (val discriminator = readByte().toInt()) {
                    PATCH_SPECIFICATION_TOTAL_MG ->
                        MedicinePreparation.PatchSpecification.TotalMg(valueMg = readDouble())

                    PATCH_SPECIFICATION_RELEASE_RATE ->
                        MedicinePreparation.PatchSpecification.ReleaseRateMcgPerDay(
                            valueMcgPerDay = readDouble()
                        )

                    else -> error("Unknown patch specification discriminator: $discriminator")
                }
            )

            MedicinePreparationType.PATCH_OFF -> MedicinePreparation.PatchOff
        }
    }

    private fun DataOutputStream.writeDoseInstruction(instruction: DoseInstruction) {
        writeString(instruction.kind.name)
        when (instruction) {
            is DoseInstruction.TabletFraction -> {
                writeInt(instruction.numerator)
                writeInt(instruction.denominator)
            }

            DoseInstruction.WholeUnit -> Unit
            is DoseInstruction.VolumeMl -> writeDouble(instruction.valueMl)
            is DoseInstruction.WeightGrams -> writeDouble(instruction.valueGrams)
            DoseInstruction.Noop -> Unit
        }
    }

    private fun DataInputStream.readDoseInstruction(): DoseInstruction {
        return when (DoseInstructionKind.fromStorageValue(readString())) {
            DoseInstructionKind.TABLET_FRACTION -> DoseInstruction.TabletFraction(
                numerator = readInt(),
                denominator = readInt(),
            )

            DoseInstructionKind.WHOLE_UNIT -> DoseInstruction.WholeUnit
            DoseInstructionKind.VOLUME_ML -> DoseInstruction.VolumeMl(valueMl = readDouble())
            DoseInstructionKind.WEIGHT_GRAMS -> DoseInstruction.WeightGrams(valueGrams = readDouble())
            DoseInstructionKind.NOOP -> DoseInstruction.Noop
        }
    }

    private fun DataOutputStream.writeLocalTime(time: LocalTime) {
        writeInt(time.hour)
        writeInt(time.minute)
        writeInt(time.second)
        writeInt(time.nano)
    }

    private fun DataInputStream.readLocalTime(): LocalTime {
        return LocalTime.of(readInt(), readInt(), readInt(), readInt())
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size >= 0) { "String length must not be negative." }
        val bytes = ByteArray(size)
        readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        value?.let { resolvedValue -> writeString(resolvedValue) }
    }

    private fun DataInputStream.readNullableString(): String? {
        return if (readBoolean()) readString() else null
    }

    private fun DataOutputStream.writeNullableDouble(value: Double?) {
        writeBoolean(value != null)
        value?.let(::writeDouble)
    }

    private fun DataInputStream.readNullableDouble(): Double? {
        return if (readBoolean()) readDouble() else null
    }

    private fun DataOutputStream.writeUserProfile(profile: UserProfile?) {
        writeBoolean(profile != null)
        profile ?: return
        writeNullableDouble(profile.weightKg)
        writeNullableDouble(profile.weightOriginalValue)
        writeString(profile.weightOriginalUnit.name)
        writeNullableLong(profile.updatedAt?.toEpochMilli())
    }

    private fun DataInputStream.readUserProfile(): UserProfile? {
        if (!readBoolean()) return null
        return UserProfile(
            weightKg = readNullableDouble(),
            weightOriginalValue = readNullableDouble(),
            weightOriginalUnit = WeightUnit.fromStorageValue(readString()),
            updatedAt = readNullableLong()?.let(Instant::ofEpochMilli),
        )
    }

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        writeBoolean(value != null)
        value?.let(::writeLong)
    }

    private fun DataInputStream.readNullableLong(): Long? {
        return if (readBoolean()) readLong() else null
    }

    private fun <T> DataOutputStream.writeList(
        values: List<T>,
        writeItem: DataOutputStream.(T) -> Unit,
    ) {
        writeInt(values.size)
        values.forEach { value -> writeItem(value) }
    }

    private fun <T> DataInputStream.readList(readItem: DataInputStream.() -> T): List<T> {
        val size = readInt()
        require(size >= 0) { "List size must not be negative." }
        return List(size) { readItem() }
    }
}

internal interface HomeSnapshotCrypto {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

private class AndroidHomeSnapshotCrypto : HomeSnapshotCrypto {
    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val encryptedBytes = cipher.doFinal(plaintext)
        val iv = cipher.iv
        return ByteBuffer
            .allocate(ENCRYPTED_MAGIC_BYTES.size + VERSION_LENGTH_BYTES + IV_LENGTH_BYTES + iv.size + encryptedBytes.size)
            .put(ENCRYPTED_MAGIC_BYTES)
            .put(ENCRYPTED_CONTAINER_VERSION.toByte())
            .put(iv.size.toByte())
            .put(iv)
            .put(encryptedBytes)
            .array()
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(ciphertext)
        val magic = ByteArray(ENCRYPTED_MAGIC_BYTES.size)
        buffer.get(magic)
        require(magic.contentEquals(ENCRYPTED_MAGIC_BYTES)) {
            "Unsupported Home snapshot container."
        }
        val version = buffer.get().toInt() and BYTE_MASK
        require(version == ENCRYPTED_CONTAINER_VERSION) {
            "Unsupported Home snapshot container version: $version."
        }
        val ivLength = buffer.get().toInt() and BYTE_MASK
        require(ivLength > 0 && ivLength <= MAX_GCM_IV_LENGTH_BYTES && buffer.remaining() > ivLength) {
            "Invalid Home snapshot IV."
        }
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encryptedBytes = ByteArray(buffer.remaining())
        buffer.get(encryptedBytes)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateMasterKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return cipher.doFinal(encryptedBytes)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existingKey = keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(AES_KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }
}

private const val TAG = "HomeSnapshotStore"

// v15 carries MedicineStock inside cached Medicine values; older caches are
// rejected by codec-version mismatch so tracked stock is never defaulted away.
// v17 appends the deduped pkEntries pool so the snapshot path can re-simulate
// the E2 curve locally when the cached projection expires.
// v18 appends archivedGroups (after activeGroups) so Home/widget mirror Plan.
// v19 appends doseAmountDelta to medication log entries for stock deltas.
// v20 appends importedFromExternalTracker to cached Medicine and adds imported
// injection/gel preparation payloads.
// v21 appends medication log import provenance.
// v22 appends the cached Home hero anchor tracked date.
// v23 appends the per-hormone PK projection map (PR2).
// v24 appends testosteronePkEntries + cpaPkEntries (F-PR-snapshot-upgrade)
//     so the T / CPA chart paths can short-circuit the live Room
//     flow on cold start. The reader is strict — a v24 reader on
//     v23 bytes throws, which is the right behavior (we want the
//     user to refresh after upgrade rather than silently fall back
//     to a half-cached state).
// v25 appends bloodTestPanels (BT-PR3, v2 血检追踪) so the home
//     "Recent Bloodwork" card renders a non-empty T trend / Hb
//     chip on cold start instead of waiting for the live Room
//     flow to re-emit. Same strict-reader policy: a v25 reader
//     on v24 bytes throws, triggering a fresh snapshot refresh.
private const val SNAPSHOT_CODEC_VERSION = 25
private const val POLICY_DISCRIMINATOR_INTERVAL = 0
private const val POLICY_DISCRIMINATOR_BUDGET = 1
private const val PATCH_SPECIFICATION_TOTAL_MG = 0
private const val PATCH_SPECIFICATION_RELEASE_RATE = 1

// Reserved sentinel string in the medicationKey slot for a PATCH_OFF row in
// the snapshot codec. Distinct from every MedicationKey.name.
private const val PATCH_OFF_SELECTION_MARKER = "__PATCH_OFF__"
private val HOME_SNAPSHOT_GENERATION_KEY = longPreferencesKey("generation")
private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val MASTER_KEY_ALIAS = "hrt_home_snapshot_key"
private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private const val AES_KEY_SIZE_BITS = 256
private const val GCM_TAG_LENGTH_BITS = 128
private const val ENCRYPTED_CONTAINER_VERSION = 1
private const val VERSION_LENGTH_BYTES = 1
private const val IV_LENGTH_BYTES = 1
private const val MAX_GCM_IV_LENGTH_BYTES = 32
private const val BYTE_MASK = 0xff
private val ENCRYPTED_MAGIC_BYTES = byteArrayOf(
    'H'.code.toByte(),
    'O'.code.toByte(),
    'M'.code.toByte(),
    'E'.code.toByte(),
)
