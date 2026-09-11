# v2 血检追踪 (Blood Test Tracking)

The v2 milestone adds per-(goal, analyte) clinical context for the
two most actionable blood-test markers for HRT users:
**testosterone (T)** and **hemoglobin (Hb)**. Both surface on the
home screen as a "Recent Bloodwork" card; the underlying engine
also serves future surfaces (calibration editor, journal,
notifications) that need a per-goal reference range or severity
classification.

This document covers the data layer, reference-profile math, and
the home-card UI. The clinical anchors are intentionally broad
and intentionally not lab-specific — see "Clinical framing" at
the bottom for the design rationale.

## Built-in analyte catalog (BT-PR1)

`BloodAnalyteKey` previously covered E2 / T / PROG / PRL / FSH /
LH (the legacy hormone-panel set). v2 adds:

- `HEMOGLOBIN` (`hb`) — the canonical signal for the polycythemia
  risk on T (FtM users) and the anemia risk on E2 (MtF users)

`BloodUnitKey` previously covered pg/mL / ng/mL / ng/dL / pmol/L /
nmol/L / mIU/L / mIU/mL / IU/L. v2 adds:

- `G_DL` (`g_dl`, canonical for Hb) — US lab unit
- `G_L` (`g_l`, SI) — 1 g/dL = 10 g/L, so toCanonical factor is 0.1

`BloodTestCatalog` exposes the unit conversion:

```kotlin
BloodTestCatalog.toCanonical(HEMOGLOBIN, 142.0, G_L)  // 14.2
BloodTestCatalog.fromCanonical(HEMOGLOBIN, 14.2, G_L) // 142.0
```

`HEMATOCRIT` is intentionally deferred to v3 — Hb is the
clinically actionable signal and shipping two hematocrit-adjacent
analytes in one milestone dilutes the v2 framing. The catalog is
forward-compatible: the existing `custom_blood_analytes` table
already covers user-defined analytes if a power user wants
hematocrit tracked now.

## Per-goal reference profiles (BT-PR1)

`BloodAnalyteReferenceProfile` is a closed numeric range plus
optional low / high / critical thresholds:

```kotlin
data class BloodAnalyteReferenceProfile(
    val normalRange: ReferenceRange,        // e.g. 12.0..16.0 (g/dL)
    val lowThreshold: Double?,              // below this is the LOW direction
    val highThreshold: Double?,             // above this is the HIGH direction
    val criticalThreshold: Double?,         // above this is CRITICAL
)
```

The reference profile is per-`(analyteKey, goal)`. The lookup is
`bloodAnalyteReferenceProfileFor(analyteKey, goal)`.

### Testosterone (canonical unit ng/dL)

| Goal        | Normal range | Low    | High   | Critical |
|-------------|--------------|--------|--------|----------|
| MTF         | 8 – 60       | —      | 60     | 200      |
| FTM         | 300 – 1000   | 200    | 1000   | 1500     |
| NON_BINARY  | 50 – 200     | —      | 200    | 1000     |
| UNSET       | 200 – 800    | —      | 1000   | 1500     |

The MTF band (8-60) is the typical "suppressed" range on E2 +
antiandrogen. The FTM band (300-1000) is the typical "masculine"
range on T. NON_BINARY / UNSET fall back to a mid / unisex band.

Note: MTF has no explicit low cutoff because below 8 is
"very suppressed" but not clinically alarming in the same
direction — the per-goal builder returns `null` for the
lowThreshold to skip the LOW severity direction for that
case. The trend chart still shows the value; only the chip
classification is affected.

### Hemoglobin (canonical unit g/dL)

| Goal        | Normal range | Low   | High   | Critical |
|-------------|--------------|-------|--------|----------|
| MTF         | 12.0 – 16.0  | 12.0  | 16.5   | 18.0     |
| FTM         | 13.5 – 17.5  | 12.0  | 17.5   | 18.0     |
| NON_BINARY  | 12.0 – 17.5  | 12.0  | 17.5   | 18.0     |
| UNSET       | 12.0 – 17.5  | 12.0  | 17.5   | 18.0     |

MTF (E2 users) gets a tighter upper bound (16.5) than FTM (17.5)
because polycythemia on E2 is unusual; the looser FTM bound
reflects that T users run higher Hb physiologically. The shared
critical threshold (18.0) is the "this is polycythemia, look
into it" level that applies to any regimen.

UNSET / NON_BINARY share the unisex band, matching the v0.6
default where the user's goal was not yet captured.

### Why these numbers

The bands are conservative clinical anchors, not lab-specific
cutoffs. The exact reference range for an Hb test depends on
the lab's methodology, the patient's age, and (for transfem
users on E2) the dose + trough timing. We deliberately do not
hard-code the lab's exact number because:

- Most users see a "reference range" printed on their lab
  report that already encodes the lab's calibration. The
  chip labels the band "suppressed range" / "masculine range" /
  "unisex range" rather than "normal" / "abnormal" so the
  framing doesn't conflict with a lab-specific range.
- The per-goal band lets the chip react to a regimen change
  immediately, before the next lab visit. A user who just
  started T and sees their Hb at 16.0 gets a HIGH badge even
  though the lab would still call that "in range" for a male
  patient — the chip's job is to flag *relative to the
  user's regimen*, not to second-guess the lab.

## Severity classification (BT-PR1)

`BloodAnalyteReferenceSeverity` is the enum the home chip / chart
use to color the value:

```kotlin
enum class BloodAnalyteReferenceSeverity {
    NORMAL,    // value is within the normal range
    LOW,       // value is below lowThreshold
    HIGH,      // value is above highThreshold but <= criticalThreshold
    CRITICAL,  // value is above criticalThreshold
    UNKNOWN,   // no profile exists for (analyte, goal) or value is non-finite
}
```

The classifier is `classifyBloodAnalyteValue(analyteKey, goal,
canonicalValue)`. CRITICAL takes priority over HIGH so a
supra-physiological value still surfaces the louder label.

CRITICAL is `value > criticalThreshold` (strict), so a value
exactly at 18.0 g/dL stays HIGH for FTM (17.5 < 18.0 ≤ 18.0).
This is intentional: we want the user to act on "critical"
only when the value is clearly past the threshold, not on
the boundary. The same strict-greater rule applies to LOW.

## Home card (BT-PR2)

`MainBloodTestCard` renders the "Recent Bloodwork" card on the
home screen. Layout (vertical):

1. Header: "Recent Bloodwork" + panel count subtitle ("1 panel" /
   "3 panels")
2. T trend row: title + latest value label + Canvas spark line
   over the trailing 6 months with the per-goal reference band
   drawn as a translucent rectangle behind the line
3. Hb chip row: title + a Surface chip with the latest Hb
   value, a short date ("Sep 1"), and a per-severity dot

The card is null when the user has no blood test panels at all;
the home screen skips the slot in that case. The card is also
non-reorderable (not part of `HomeCardLayout`) because it's a
passive summary, not an interactive card.

### T trend math

`buildMainBloodTrend` filters the user's T results to the
trailing 6 months (`MainBloodTestCardWindowDays = 180`) and
maps each result to the canonical unit (ng/dL) so the
reference-band comparison is unit-agnostic. The Y axis is
padded by 15% of the data range (minimum 5 canonical units)
so the spark line never touches the chart edge.

The reference band is drawn as a translucent rectangle behind
the line. The band is only rendered when its range fits inside
the data window — a band wider than the data range would
cover the whole chart and hide the line.

### Hb chip math

`buildMainHemoglobinChip` picks the most recent Hb result and
classifies it per-goal via `classifyBloodAnalyteValue`. The
chip's color palette:

- NORMAL / UNKNOWN: `surfaceContainerHighest` (neutral)
- LOW: `tertiaryContainer` (amber)
- HIGH / CRITICAL: `errorContainer` (red)

The chip deliberately does not surface the "severity" text —
it shows the value + the date so the user reads the number
and the date without the chip trying to interpret it. The
color is the only interpretation.

## Snapshot persistence (BT-PR3)

`HomeSnapshotRecord` now carries `bloodTestPanels: List<
HomeSnapshotBloodPanelRecord>` (defaulted empty for pre-v25
back-compat). The codec version bumps 24 → 25 and the schema
version 9 → 10. The new field is written *after* cpaPkEntries
in the byte stream so a v24 reader on v25 bytes throws
intentionally — the snapshot path fails fast, the repository
runs a fresh refresh, and the user sees a one-time empty card
on the very first launch after upgrade.

The flat record shape (UUID + collectedAt + per-result
canonical value + unit + display order) is intentional: the
panel entity carries heavy fields (notes, dose timing, custom
analyte name+unitLabel) the home card never reads. Shipping
the full model would bloat the snapshot for no win.

### Why not the live Room flow alone?

The home card was wired to the live Room flow in BT-PR2 (so it
works correctly with a snapshot miss or older cache). BT-PR3
adds the snapshot persistence so the card renders a non-empty
trend / chip on cold start without waiting for the Room
collection to re-emit. The repository already pulls the
panels in the refresh path; the cold-start wiring into the
home card is a follow-up that can land once the codec and
repository plumbing are in place.

## Clinical framing

- The chip / chart never say "normal" or "abnormal". The text
  is always neutral ("suppressed range", "masculine range",
  "above the high threshold") so the UI doesn't claim
  clinical authority it doesn't have.
- The reference numbers are conservative population
  averages, not lab-specific ranges. A user with a
  lab-specific range should still defer to their lab.
- Critical threshold is for "this needs attention" framing,
  not for "this is dangerous" framing. The chip color is a
  nudge, not a panic button.
- NON_BINARY is intentionally treated as UNSET in the
  per-goal Hb path. The user's identity shouldn't constrain
  the safety framing of their blood work.

## Future work (v3+)

- HEMATOCRIT as a built-in analyte (deferred from v2)
- "Add blood test" entry on the home card empty state that
  opens the calibration editor's "new panel" flow
- Per-analyte reference range override (user can tweak the
  band to match their lab's printed range)
- Hb trend over time (analogous to the T trend) for users
  with enough data points
- Lab report import: PDF / image OCR → auto-fill a new
  panel (already a stub in the importer; not user-facing yet)
