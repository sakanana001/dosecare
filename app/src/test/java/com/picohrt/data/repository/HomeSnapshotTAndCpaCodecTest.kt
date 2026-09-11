package com.picohrt.data.repository

import com.picohrt.model.medication.DoseInstruction
import com.picohrt.model.medication.MedicationApplicationType
import com.picohrt.model.medication.MedicationCategory
import com.picohrt.model.medication.MedicationKey
import com.picohrt.model.medication.MedicationLogEntry
import com.picohrt.model.medication.testMedicationLogEntry
import com.picohrt.model.medication.testMedicine
import com.picohrt.model.medication.testInstant
import com.picohrt.model.pk.PkHormone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

// F-PR-snapshot-upgrade: locks the v24 codec's new
// testosteronePkEntries + cpaPkEntries fields. v23 readers on v24
// bytes (and vice-versa) are intentionally not tested here — the
// codec uses an unchecked version int, so the upgrade fails fast on
// real data and the snapshot path falls back to a fresh refresh.
class HomeSnapshotTAndCpaCodecTest {
    @Test
    fun encodeDecode_roundTripsTestosteroneAndCpaEntries() {
        val zone = ZoneId.of("UTC")
        val now = LocalDateTime.of(2026, 9, 4, 8, 0)
        val testosteroneEntry = testMedicationLogEntry(
            uuid = UUID.fromString("00000000-0000-0000-0000-00000000a001"),
            medicine = testMedicine(
                key = MedicationKey.TESTOSTERONE_ENANTHATE,
                preparation = com.picohrt.model.medication.MedicinePreparation.InjectionSingleUseVial(
                    strengthMgPerVial = 200.0,
                ),
            ),
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.INJECTION,
            doseInstruction = DoseInstruction.WholeUnit,
            sourceGroupUuid = null,
            appliedAt = testInstant(now.minusHours(48)),
        )
        val cpaEntry = testMedicationLogEntry(
            uuid = UUID.fromString("00000000-0000-0000-0000-00000000a002"),
            medicine = testMedicine(
                key = MedicationKey.CYPROTERONE_ACETATE,
                preparation = com.picohrt.model.medication.MedicinePreparation.Pill(
                    strengthMgPerTablet = 50.0,
                ),
            ),
            category = MedicationCategory.ANTIANDROGEN,
            applicationType = MedicationApplicationType.ORAL,
            doseInstruction = DoseInstruction.TabletFraction(1, 1),
            sourceGroupUuid = null,
            appliedAt = testInstant(now.minusHours(24)),
        )

        val record = HomeSnapshotRecord(
            schemaVersion = 9,
            generatedAtEpochMillis = now.atZone(zone).toInstant().toEpochMilli(),
            anchorDateEpochDay = now.toLocalDate().toEpochDay(),
            zoneId = zone.id,
            pkProjections = emptyMap(),
            widgetPkProjection = null,
            pkProjection = null,
            activeGroups = emptyList(),
            archivedGroups = emptyList(),
            scheduleEntries = emptyList(),
            antiandrogenHistoryEntries = emptyList(),
            userProfile = null,
            stockMedicines = emptyList(),
            stockFulfillmentEntries = emptyList(),
            pkEntries = emptyList(),
            testosteronePkEntries = listOf(testosteroneEntry),
            cpaPkEntries = listOf(cpaEntry),
            homeAnchor = null,
        )

        val bytes = HomeSnapshotCodec.encode(record)
        val decoded = HomeSnapshotCodec.decode(bytes)

        assertEquals(1, decoded.testosteronePkEntries.size)
        assertEquals(1, decoded.cpaPkEntries.size)
        assertEquals(testosteroneEntry.uuid, decoded.testosteronePkEntries[0].uuid)
        assertEquals(cpaEntry.uuid, decoded.cpaPkEntries[0].uuid)
        // Spot-check that the medicine-keyed info round-trips so the
        // engine's toTestosteronePkDoseEvent / toCpaPkDoseEvent can
        // re-resolve the compound.
        assertEquals(
            MedicationKey.TESTOSTERONE_ENANTHATE,
            (decoded.testosteronePkEntries[0].medicine?.selection as? com.picohrt.model.medication.MedicineSelection.Catalog)?.medicationKey,
        )
        assertEquals(
            MedicationKey.CYPROTERONE_ACETATE,
            (decoded.cpaPkEntries[0].medicine?.selection as? com.picohrt.model.medication.MedicineSelection.Catalog)?.medicationKey,
        )
    }

    @Test
    fun encodeDecode_emptyTPkEntries_isBackCompatWithV23Default() {
        // v24 writes [testosteronePkEntries = emptyList] as the
        // codec's default; a pre-v24 caller that constructs a
        // HomeSnapshotRecord without setting the new fields still
        // encodes an empty list, and a v24 reader on a v24-encoded
        // empty list yields emptyList on the other side.
        val record = HomeSnapshotRecord(
            schemaVersion = 9,
            generatedAtEpochMillis = 0L,
            anchorDateEpochDay = 0L,
            zoneId = "UTC",
            activeGroups = emptyList(),
            scheduleEntries = emptyList(),
            antiandrogenHistoryEntries = emptyList(),
        )
        val bytes = HomeSnapshotCodec.encode(record)
        val decoded = HomeSnapshotCodec.decode(bytes)
        assertEquals(emptyList<MedicationLogEntry>(), decoded.testosteronePkEntries)
        assertEquals(emptyList<MedicationLogEntry>(), decoded.cpaPkEntries)
        // Sanity: the existing pkEntries field stays the default too.
        assertTrue(decoded.pkEntries.isEmpty())
    }

    @Test
    fun encodeDecode_preservesMultipleTAndCpaEntriesInOriginalOrder() {
        // The codec orders by appliedAtEpochMillis DESC (see the
        // DAO query); the test feeds entries in an unsorted order
        // and asserts the decoder preserves what the encoder
        // received (no implicit re-sort, which is the caller's
        // job).
        val now = LocalDateTime.of(2026, 9, 4, 8, 0)
        val older = testMedicationLogEntry(
            medicine = testMedicine(
                key = MedicationKey.TESTOSTERONE_ENANTHATE,
                preparation = com.picohrt.model.medication.MedicinePreparation.InjectionSingleUseVial(
                    strengthMgPerVial = 100.0,
                ),
            ),
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.INJECTION,
            doseInstruction = DoseInstruction.WholeUnit,
            sourceGroupUuid = null,
            appliedAt = testInstant(now.minusDays(7)),
        )
        val newer = testMedicationLogEntry(
            medicine = testMedicine(
                key = MedicationKey.TESTOSTERONE_CYPIONATE,
                preparation = com.picohrt.model.medication.MedicinePreparation.InjectionSingleUseVial(
                    strengthMgPerVial = 200.0,
                ),
            ),
            category = MedicationCategory.TESTOSTERONE,
            applicationType = MedicationApplicationType.INJECTION,
            doseInstruction = DoseInstruction.WholeUnit,
            sourceGroupUuid = null,
            appliedAt = testInstant(now.minusHours(12)),
        )
        val record = HomeSnapshotRecord(
            schemaVersion = 9,
            generatedAtEpochMillis = 0L,
            anchorDateEpochDay = 0L,
            zoneId = "UTC",
            activeGroups = emptyList(),
            scheduleEntries = emptyList(),
            antiandrogenHistoryEntries = emptyList(),
            testosteronePkEntries = listOf(older, newer),
        )
        val bytes = HomeSnapshotCodec.encode(record)
        val decoded = HomeSnapshotCodec.decode(bytes)
        assertEquals(2, decoded.testosteronePkEntries.size)
        assertEquals(older.uuid, decoded.testosteronePkEntries[0].uuid)
        assertEquals(newer.uuid, decoded.testosteronePkEntries[1].uuid)
    }
}
