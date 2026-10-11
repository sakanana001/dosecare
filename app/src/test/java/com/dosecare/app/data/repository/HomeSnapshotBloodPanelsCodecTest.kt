package com.dosecare.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

/**
 * BT-PR3: codec round-trip for the new blood test panel field on
 * the home snapshot. The record is a flat projection of
 * [com.dosecare.app.model.bloodtest.BloodTestPanel] (UUID +
 * collectedAt + per-result canonical value), not the full
 * [com.dosecare.app.model.bloodtest.BloodTestPanel] model.
 */
class HomeSnapshotBloodPanelsCodecTest {
    @Test
    fun empty_blood_panels_round_trip() {
        val record = newRecord(bloodTestPanels = emptyList())

        val decoded = HomeSnapshotCodec.decode(HomeSnapshotCodec.encode(record))

        assertEquals(emptyList<HomeSnapshotBloodPanelRecord>(), decoded.bloodTestPanels)
    }

    @Test
    fun single_panel_with_builtin_results_round_trip() {
        val panel = HomeSnapshotBloodPanelRecord(
            panelUuid = UUID.fromString("11111111-2222-3333-4444-555555555555"),
            collectedAtEpochMillis = 1_726_000_000_000L,
            collectedAtTimeZoneId = "America/Los_Angeles",
            results = listOf(
                HomeSnapshotBloodResultRecord(
                    resultUuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                    builtinAnalyteKey = "t",
                    canonicalValue = 500.0,
                    unitSnapshot = "ng_dl",
                    displayOrder = 0,
                ),
                HomeSnapshotBloodResultRecord(
                    resultUuid = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"),
                    builtinAnalyteKey = "hb",
                    canonicalValue = 14.2,
                    unitSnapshot = "g_dl",
                    displayOrder = 1,
                ),
            ),
        )

        val decoded = HomeSnapshotCodec.decode(
            HomeSnapshotCodec.encode(newRecord(bloodTestPanels = listOf(panel)))
        )

        assertEquals(1, decoded.bloodTestPanels.size)
        val decodedPanel = decoded.bloodTestPanels.single()
        assertEquals(panel.panelUuid, decodedPanel.panelUuid)
        assertEquals(panel.collectedAtEpochMillis, decodedPanel.collectedAtEpochMillis)
        assertEquals(panel.collectedAtTimeZoneId, decodedPanel.collectedAtTimeZoneId)
        assertEquals(2, decodedPanel.results.size)
        assertEquals(panel.results[0], decodedPanel.results[0])
        assertEquals(panel.results[1], decodedPanel.results[1])
    }

    @Test
    fun custom_analyte_with_null_builtin_key_round_trip() {
        val panel = HomeSnapshotBloodPanelRecord(
            panelUuid = UUID.fromString("33333333-4444-5555-6666-777777777777"),
            collectedAtEpochMillis = 1_726_001_000_000L,
            collectedAtTimeZoneId = "UTC",
            results = listOf(
                HomeSnapshotBloodResultRecord(
                    resultUuid = UUID.randomUUID(),
                    builtinAnalyteKey = null, // custom analyte
                    canonicalValue = 30.0,
                    unitSnapshot = "ng/dL",
                    displayOrder = 0,
                ),
            ),
        )

        val decoded = HomeSnapshotCodec.decode(
            HomeSnapshotCodec.encode(newRecord(bloodTestPanels = listOf(panel)))
        )

        val decodedResult = decoded.bloodTestPanels.single().results.single()
        assertEquals(null, decodedResult.builtinAnalyteKey)
        assertEquals(30.0, decodedResult.canonicalValue, 1e-9)
        assertEquals("ng/dL", decodedResult.unitSnapshot)
    }

    @Test
    fun multi_panel_preserves_order() {
        val panels = (0..3).map { i ->
            HomeSnapshotBloodPanelRecord(
                panelUuid = UUID.randomUUID(),
                collectedAtEpochMillis = 1_726_000_000_000L + (i * 1000L),
                collectedAtTimeZoneId = "UTC",
                results = listOf(
                    HomeSnapshotBloodResultRecord(
                        resultUuid = UUID.randomUUID(),
                        builtinAnalyteKey = "t",
                        canonicalValue = 400.0 + i * 50,
                        unitSnapshot = "ng_dl",
                        displayOrder = 0,
                    ),
                ),
            )
        }

        val decoded = HomeSnapshotCodec.decode(
            HomeSnapshotCodec.encode(newRecord(bloodTestPanels = panels))
        )

        assertEquals(panels.size, decoded.bloodTestPanels.size)
        decoded.bloodTestPanels.forEachIndexed { i, decodedPanel ->
            assertEquals(panels[i].panelUuid, decodedPanel.panelUuid)
            assertEquals(panels[i].results.single().canonicalValue, decodedPanel.results.single().canonicalValue, 1e-9)
        }
    }

    // ---- helpers ----

    private fun newRecord(bloodTestPanels: List<HomeSnapshotBloodPanelRecord>): HomeSnapshotRecord {
        // Build a minimal record; the codec only round-trips the
        // blood panels field, so the other fields just need to
        // satisfy the constructor's required shape.
        return HomeSnapshotRecord(
            schemaVersion = 10,
            generation = 0L,
            generatedAtEpochMillis = 1_726_000_000_000L,
            anchorDateEpochDay = 20_257L,
            zoneId = "UTC",
            activeGroups = emptyList(),
            scheduleEntries = emptyList(),
            antiandrogenHistoryEntries = emptyList(),
            bloodTestPanels = bloodTestPanels,
        )
    }
}
