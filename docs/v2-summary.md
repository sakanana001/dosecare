# v2 血检追踪 PR 汇总 (4 commits)

The v2 milestone surfaces per-(goal, analyte) clinical context
for the two most actionable blood-test markers in HRT
**testosterone (T) and hemoglobin (Hb)**. The data layer was
already in place from v0.6 (BloodTestPanel + BloodTestResult +
CustomBloodAnalyte + BloodTestRepository) but no analyte-aware
reference logic existed and no home-screen surface rendered
blood-test data.

## 路线

- **BT-PR1** — Data foundation: extend `BloodAnalyteKey` with
  HEMOGLOBIN, add `G_DL` / `G_L` units, define per-goal
  reference profiles (T + Hb), add a per-goal severity
  classifier (NORMAL / LOW / HIGH / CRITICAL / UNKNOWN).
  30 new tests, 0 regressions. **commit `4de7c97`**.
- **BT-PR2** — Home UI: add a non-reorderable "Recent
  Bloodwork" card to the home screen with a 6-month T trend
  spark line (per-goal reference band) and a Hb chip
  (per-goal severity color). Inject `BloodTestRepository` into
  `MainViewModel`, add a 10-input vararg combine, plumb a
  nullable `bloodTestCard` through `MainUiState`, render the
  card below the user-orderable home cards.
  10 new tests, 0 regressions. **commit `d5ecd9d`**.
- **BT-PR3** — Snapshot persistence: persist recent blood
  panels in the encrypted home snapshot so the home card
  renders a non-empty trend / chip on cold start. Add flat
  `HomeSnapshotBloodPanelRecord` / `HomeSnapshotBloodResultRecord`
  projections (UUID + collectedAt + per-result canonical
  value), bump codec 24 → 25, bump schema 9 → 10, add
  `bloodTestPanels` to `HomeSnapshotRecord` /
  `HomeSnapshotBuildInputs`.
  4 new codec tests, 0 regressions. **commit `0582d5d`**.
- **BT-PR4** — Docs: `docs/blood-test-v2.md` (data layer,
  reference math, home card, snapshot persistence, clinical
  framing) + this summary. **commit (this commit)`.

## 数字

- 4 commits, **+1255 / -25**
- 6 files created / 6 files modified
- **+44 new tests** (30 BT-PR1 + 10 BT-PR2 + 4 BT-PR3)
- **0 regressions**: full suite is 2481 tests / 42 failed
  (baseline Windows FileStorage flakes, unchanged)
- 累计 (含 v0.6 21 commits): **26 commits, 11 PRs, 7 docs**

## Built-in analyte catalog changes

| Analyte | v0.6 | v2 |
|---|---|---|
| E2 / T / PROG / PRL / FSH / LH | ✓ | ✓ |
| **HEMOGLOBIN** | — | **✓** (new) |
| HEMATOCRIT | — | deferred to v3 |
| Custom (user-defined) | ✓ | ✓ |

## Per-goal reference profiles

### Testosterone (canonical: ng/dL)

| Goal | Normal range | Low | High | Critical |
|---|---|---|---|---|
| MTF | 8 – 60 | — | 60 | 200 |
| FTM | 300 – 1000 | 200 | 1000 | 1500 |
| NON_BINARY | 50 – 200 | — | 200 | 1000 |
| UNSET | 200 – 800 | — | 1000 | 1500 |

### Hemoglobin (canonical: g/dL)

| Goal | Normal range | Low | High | Critical |
|---|---|---|---|---|
| MTF | 12.0 – 16.0 | 12.0 | 16.5 | 18.0 |
| FTM | 13.5 – 17.5 | 12.0 | 17.5 | 18.0 |
| NON_BINARY | 12.0 – 17.5 | 12.0 | 17.5 | 18.0 |
| UNSET | 12.0 – 17.5 | 12.0 | 17.5 | 18.0 |

## Severity classifier

`BloodAnalyteReferenceSeverity` is the per-value classification
(NORMAL / LOW / HIGH / CRITICAL / UNKNOWN). The classifier
prioritizes CRITICAL > HIGH > LOW > NORMAL, with strict-greater
so a value at the threshold boundary stays at the less-severe
label (e.g. Hb = 18.0 for FTM is HIGH, not CRITICAL).

The classifier is consumed by:

- `MainBloodTestCardBuilderTest` (10 tests, BT-PR2)
- `MainBloodTestCardBuilder.buildMainHemoglobinChip` (the
  per-goal severity → chip color mapping)
- `MainBloodTestCard` severity color helpers (NORMAL/UNKNOWN
  use neutral surface tones; LOW uses tertiary; HIGH/CRITICAL
  use error palette)

## 关键文件

- `app/src/main/java/com/mkx/hrttracker/model/bloodtest/BloodTestCatalog.kt`
  — HEMOGLOBIN analyte + G_DL/G_L units
- `app/src/main/java/com/mkx/hrttracker/model/bloodtest/BloodAnalyteReferenceRanges.kt`
  — per-(goal, analyte) reference profile lookup
- `app/src/main/java/com/mkx/hrttracker/model/bloodtest/BloodAnalyteReferenceSeverity.kt`
  — severity enum + classifier
- `app/src/main/java/com/mkx/hrttracker/ui/main/MainUiModels.kt`
  — `MainBloodTestCardUiState`, `MainBloodTrendUiState`,
  `MainHemoglobinChipUiState`, `buildMainBloodTestCard`
- `app/src/main/java/com/mkx/hrttracker/ui/main/MainBloodTestCard.kt`
  — home-screen card composable
- `app/src/main/java/com/mkx/hrttracker/data/repository/HomeSnapshotStore.kt`
  — codec v25 + `HomeSnapshotBloodPanelRecord` /
  `HomeSnapshotBloodResultRecord`
- `app/src/main/java/com/mkx/hrttracker/data/repository/HomeSnapshotRepository.kt`
  — refresh path pulls blood panels + schema 9 → 10
- `docs/blood-test-v2.md` — full data layer + clinical framing

## 后续 (deferred)

- **HEMATOCRIT** as a built-in analyte (v3)
- **"Add blood test" entry** on the home card empty state —
  opens the calibration editor's "new panel" flow
- **Per-analyte reference range override** — let the user
  tweak the band to match their lab's printed range
- **Hb trend over time** — analogous to the T trend, for
  users with enough data points
- **Cold-start wiring** for the home card snapshot path — the
  snapshot now stores the panels (BT-PR3) but the home card
  currently reads the live Room flow; folding in the snapshot
  is a follow-up
- **Lab report import** — PDF / image OCR → auto-fill a new
  panel (stub exists in the importer, not user-facing yet)

## v2 milestone shippability

The home card surfaces per-(goal, analyte) context without any
new entry-point UI: a user who has logged blood tests in the
calibration editor will see their T trend + Hb chip on the
home screen the next time they open the app. The card is
hidden (no empty container) when the user has no panels at
all, so existing v0.6 users see no visual change at first
launch. Users who add a blood test via the existing
calibration editor immediately see the card surface the
new data.
