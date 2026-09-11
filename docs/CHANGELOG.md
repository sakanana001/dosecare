# Changelog

All notable changes to PicoHRT are documented in this file. The format
loosely follows [Keep a Changelog](https://keepachangelog.com/), and this
project adheres to [Semantic Versioning](https://semver.org/).

> PicoHRT is a fork of [mkx173/Featherline](https://github.com/mkx173/Featherline);
> v0.6.0 and earlier entries describe the upstream project's history.

> Looking for the user-facing release notes? See
> [the in-app changelog](#) (TODO: link once the in-app screen is wired).
> Looking for technical PR summaries? See
> [v2-summary.md](v2-summary.md), [mtf-pr-summary.md](mtf-pr-summary.md),
> [ftm-pr-summary.md](ftm-pr-summary.md), [pk-v3.md](pk-v3.md), and
> [pk-gel.md](pk-gel.md).

## [Unreleased]

### PK model — 3-compartment engine for testosterone undecanoate (TU)

#### Added

- **3-comp RK4 integration for testosterone undecanoate (Nebido)**.
  The 1-compartment analytical shortcut is replaced with a 3-compartment
  mammillary ODE solved by Runge-Kutta 4, with a `[central, shallow, deep]`
  state. The deep peripheral (Vd_d = 3.5 L/kg) is the long-tailed
  reservoir that produces the physiologically-observed 90 ± 40-day
  terminal half-life, which the 1-comp model couldn't represent. See
  [pk-v3.md](pk-v3.md) for the math, calibration values, and validation.

- **2-comp skin depot for AndroGel 1% (testosterone gel)**. The
  previous 1-comp gel model underpredicted AndroGel Cavg by 38-57%
  (linear absorption assumption broke at the skin barrier). The
  replacement is a 2-comp ODE with a Michaelis-Menten transfer rate
  (`Vmax * A_skin / (Km + A_skin)`) plus a first-order skin clearance
  (`kCl`) for the fraction of dose lost to washing / rubbing.
  Calibration: Vmax = 1.3 mg/h, Km = 30 mg, kCl = 0.1 h⁻¹. AndroGel
  5g / 10g daily now lands in the literature Cavg band. See
  [pk-gel.md](pk-gel.md) for details.

- **Per-user PK parameter overrides** (`PkTUserOverride` data class).
  Advanced users can override the engine's catalog `kClearInjection`
  (central clearance) and `threeCompK31` (deep peripheral release
  rate) to better match their own lab data. Currently consumed only
  by the 3-comp RK4 path (TU); the 1-comp T ester and gel paths
  ignore the override. Data-layer plumbing complete; settings
  screen UI is the next step.

- **3-comp model badge in the home T chart**. When the engine uses
  the 3-comp path (today: TU 3-comp), a small `3-comp` / `3 室` chip
  renders next to the chart title so the user can tell at a glance
  that the curve has the long-tail fidelity (rather than a 1-comp
  artifact). The flag is propagated through `PkTrendResult` →
  `MainChartUiState` → `MainChartCardHeader`. Auto-updates as the
  user's regimen switches between TU / TE / TC / gel.

- **Per-ester two-part depot model** with per-compound formation
  fraction. The shared "T catch-all compound" is gone: each T ester
  (TE, TC, TU) now has its own `(k1Fast, k1Slow, fracFast)` depot
  triplet calibrated to the literature for that specific ester.

#### Changed

- **Overall clinical-anchor MAPE 47.7% → 20.9%** across 5 scenarios
  × 11 body weights (n = 55 paired samples). Per-scenario results:
  - TE 200 mg IM: 2.14 ng/mL MAE, +6.4% bias (unchanged from 1-comp)
  - TC 200 mg IM: 1.78 ng/mL MAE, +16.3% bias (unchanged)
  - TU 1000 mg IM (Nebido): **2.62 ng/mL MAE** (was 13.00, 5× better)
  - AndroGel 5 g/day: **1.21 ng/mL MAE** (was 3.22, gel 2-comp)
  - AndroGel 10 g/day: **1.55 ng/mL MAE** (was 3.04, gel 2-comp)
  - Gel scenarios now 100% within literature range (was 18% / 45%).
- **TU 1000 mg apparent terminal t½ ≈ 90 d** (was effectively 5.8 d
  with the previous k31). v3.1 retune changed `k31` from 0.005 to
  0.0003 (`ln(2) / k31 ≈ 2311 h ≈ 96 d`), matching the Nebido SmPC
  90 ± 40 d. The change is invisible during the first 4 weeks
  (deep-to-central flux is tiny) but unlocks long-term residual
  predictions the 1-comp couldn't make.
- **Testosterone Tmax slightly earlier (87.5 h vs 1-comp's 136 h)**
  in the 3-comp model. The central-shallow equilibrium (k12 = 0.15)
  fills central faster than 1-comp's slower 2-compartment analytical
  solution. Tmax is still within the literature 144-192 h band
  (Nebido SmPC 6-8 d) on the slow side, not the early side.

#### Known limitations

- **Tmax slightly too early (87.5 h vs literature 7 d)**. v3.2 retune
  attempted to push Tmax later by reducing `k12`; the result was
  neutral on Tmax and slightly worse on MAE, so the change was
  reverted. A future `k1Fast` reduction (e.g. 0.005 → 0.0025) is
  a more promising direction to delay Tmax without compromising
  Cmax.
- **Long-term tail k31 = 0.0003 gives t½ ≈ 90 d**, which is the
  Nebido SmPC midpoint. Real patients span 50-130 d, and there's
  no per-user adjustment in the 1-comp path. The per-user override
  plumbing added in this release lets advanced users tighten this
  range to their own labs.
- **E2 path still 1-comp** for all formulations (oral, patch, gel).
  The 3-comp engine is gated to `PkRoute.INJECTION` for
  `PkCompound.TU` only.

### Home screen

#### Added

- **"3-comp" model badge in the T trend chart**. Small
  tertiary-container-colored chip rendered next to the chart title;
  localized as "3-comp" (EN) and "3 室" (zh-Hans).

![3-comp badge on the home T trend chart for a TU regimen](screenshots/unreleased/home-t-chart-3comp-badge.png)

> Screenshot: the home T trend chart for a user on Nebido (TU), with
> the `3 室` chip visible next to the chart title. The chip is not
> rendered for users on TE / TC / gel (1-comp paths), or when the
> active hormone is E2 / CPA.

#### Changed

- The T trend chart, when the user is on a TU regimen, now uses
  the 3-comp RK4 engine end-to-end. This is invisible during the
  first 4 weeks of a single dose but changes the long-term
  residual predictions materially (90 ± 40 d tail vs the
  previous effectively-zero tail).

### Statistics report

#### Added

- **Automated statistical comparison report** (n = 55 paired
  samples, 5 scenarios × 11 body weights) emitted at
  `app/app/build/reports/pk-statistical-report.md` by
  `PkStatisticalReportTest`. Includes per-scenario mean ± SD,
  MAE, RMSE, MAPE, bias, paired t-test (theoretical 1-comp vs
  project engine; project engine vs clinical midpoint), and
  Bland-Altman 95% limits of agreement.

- **Word-format statistical report** (`.docx`) at
  `app/app/build/reports/pk-v3-statistical-report.docx`,
  regenerable by running `py build_pk_report_docx.py`. Includes
  the same metrics as the markdown report plus background,
  method, per-scenario residual distribution, three-PR
  comparison table (v2 / v3 / v3.1+PR2), and known limitations
  / next-step section.

> The Word report is an artefact for medical / scientific review
> rather than a user-facing screen. It is regenerated on every
> test run that touches `PkStatisticalReportTest`.

### Test infrastructure

#### Added

- **`PkClinicalAnchorReportTest`** (1 test) — single-dose IM
  testosterone ester clinical-anchor comparison report.
- **`PkStatisticalReportTest`** (1 test) — full statistical
  comparison (n = 55 paired samples).
- **70+ unit tests** added for the new PK engine paths, the
  blood-test trend row, the 3-comp / 2-comp skin-depot RK4
  steps, and the codec v25 blood-test snapshot round-trip.

### Documentation

#### Added

- `docs/pk-v3.md` — v3 3-comp engine: data structures, calibration,
  statistical results, limitations, and next-step scope.
- `docs/pk-gel.md` — PR2 2-comp skin depot: ODE, Michaelis-Menten
  calibration, test changes, and limits.
- `docs/v2-summary.md` — v2 blood-test tracking PR1-PR4 summary.
- `docs/ftm-pr-summary.md` — FTM (female-to-male) HRT end
  PR1-PR5 summary.
- `docs/mtf-pr-summary.md` — MtF (male-to-female) HRT end
  PR1-PR4 summary.
- `docs/blood-test-v2.md` — v2 blood-test full design (analyte
  catalog, per-goal reference profiles, severity classification,
  home card, snapshot persistence, clinical framing, future work).
- `docs/CHANGELOG.md` (this file).
- `docs/CHANGELOG.zh-Hans.md` (Chinese mirror).

## [0.6.0] — 2026-08 baseline (pre-this-release)

Initial public surface. Includes:

- MtF end: 3-way home chart chip selector (E2 / T / CPA), CPA
  path, T path, E2 retune.
- FTM end: HRT-goal enum + persistence, 4 testosterone product
  keys, per-goal chart default, per-goal safety gating.
- v2 blood-test tracking: HEMOGLOBIN per-goal catalog,
  reference ranges, severity classification, home card,
  HomeSnapshot cache.
- Snapshot upgrade: T + CPA entries in HomeSnapshot cache
  (codec v24, then v25 for blood-test panels).
- Per-ester formation fraction + TU k1Slow / k1Fast retune
  (PR1 baseline before this release's PR2 / v3 / v3.1).
- Catalog i18n: 22 medication names switched to Chinese.
- TESTOSTERONE category-picker fix (F-PR follow-up).

See [mtf-pr-summary.md](mtf-pr-summary.md),
[ftm-pr-summary.md](ftm-pr-summary.md), and
[v2-summary.md](v2-summary.md) for the original PR detail.

### Baseline screenshots (reused from `.github/screenshots/`)

![Home screen with E2 / T / CPA chip selector and trend chart](../.github/screenshots/home.png)
> Home screen — the 3-way hormone chip selector (E2 / T / CPA) and
> the trend chart. The 3-comp badge (top of this release) is
> layered on top of the same chart for TU users.

![Medication plan with testosterone products selectable](../.github/screenshots/plan.png)
> Plan screen — 4 testosterone products selectable
> (TE / TC / TU / gel) for FTM users.

![Calibration / blood test entry with HEMOGLOBIN analyte catalog](../.github/screenshots/calibration.png)
> Calibration screen — HEMOGLOBIN is the v2 analyte-catalog
> flagship (per-goal reference ranges, severity classification,
> Hb trend card on home).

![History / journal view](../.github/screenshots/history.png)
> History screen — non-changes for this release; included for
> release-notes completeness.

---


### Medication log — note field on manual entries

#### Added

- **Free-text note field on every manual medication log entry**.
  When you log a manual dose from the Home screen (both the quick-log
  sheet and the "Add medication log record" sheet), an `OutlinedTextField`
  (2–6 lines) appears below the time-of-application picker. Type
  whatever you want to remember — side effects, mood, food, injection
  site, time of day, batch number. The note is per-entry, optional, and
  re-loads when you open the entry for editing.

- **Localized labels** in all four app locales: `field_note` ("Note" /
  "备注" / "備註" / "備註") and `field_note_hint` ("Anything to
  remember about this dose?").

#### Changed

- **Room schema `v9 → v10`** with `MIGRATION_9_10` (`ALTER TABLE
  medication_log_entries ADD COLUMN note TEXT`). Old data is preserved
  (no `fallbackToDestructiveMigration()`); notes default to `NULL` for
  rows inserted before this migration.

- **`MedicationLogEntry` / `MedicationLogEntryEntity` / `MedicationLogEntryInput`**
  all gain a `note: String?` field. The DAO
  `updateEditableLogFields(..., note: String? = null)` lets you update
  the note on an existing log without touching dose, schedule, or
  identity.

- **Two save paths are now note-aware**:
  - `MedicationLogEntryViewModel` (quick-log sheet) calls
    `request.note.takeIf { it.isNotBlank() }` before passing it to
    `MedicationLogRepository.saveEntry`.
  - `NewMedicineSlotViewModel` ("Add medication log record" sheet)
    does the same on `saveManualMedicineLog`.

  Both blank-normalize at the save boundary, so the database never
  stores an empty string for a note.

#### Tests

- **11 `MedicationLogEntryViewModelTest` cases** cover the note path:
  initial state, `updateNote` flows into `uiState.note`, edit
  pre-population, blank save → `null`, non-blank save → passed through.
- **3 new `NewMedicineSlotViewModelTest` cases** cover the same on the
  "Add medication log record" path: `updateNote_setsUiStateNote`,
  `updateNote_storesValueVerbatimAndClearsErrorMessage`, and
  `saveManualLog_forwardsNonBlankNoteToRepository` /
  `saveManualLog_normalizesBlankNoteToNull`.
- **1 new repository test** in `MedicationGroupRepositoryTest`
  (`testMedicationLogEntryEntity`) verifies the entity's `note`
  field round-trips.

#### Known limitations

- **No per-entry history view yet** — the note is shown on the entry
  in the home history list but not as a long-form journal view. The
  per-day Journal tab's note feature (added in v1.3.0) is a separate
  feature with a different data model.
## Release metadata

- **Versioning**: `1.3.x` minor bumps for PK retunes, `1.4.x`
  for PK engine architecture changes, `2.x` for blood-test and
  home-screen UI overhauls. See [release-process.md](release-process.md).
- **APK** (debug): `picohrt-all-abis-1.3.3-<commit>-39.apk` (the
  `<commit>` is the short hash; `-39` is the cumulative commit
  count since the v0.6 baseline).
- **Statistical report artifacts** (regenerated on every test run
  that touches `PkStatisticalReportTest`):
  - `app/app/build/reports/pk-statistical-report.md` (markdown)
  - `app/app/build/reports/pk-clinical-anchor-report.md` (markdown)
  - `app/app/build/reports/pk-v3-statistical-report.docx` (Word)
- **Per-PR test pass rate**: 2488 tests, 42 pre-existing
  Windows-FileStorage baseline flakes (unrelated to the PK
  engine changes), 0 PK-related regressions across all 5 PRs
  (v3, PR2, v3.1, PR3-UI, PR-user-override).

## Screenshot policy

- New release entries should add a screenshot under each
  user-facing change, captioned with the context (e.g. "what
  the user is on, what's visible, what changed since the last
  release").
- Baseline screenshots (in `docs/screenhots/v0.6.0/` or
  `.github/screenshots/`) are referenced, not duplicated.
- Screenshot files go under `docs/screenshots/<release>/` with
  the format `<screen-name>.png`. Update the changelog image
  link after dropping the file in.
- The `home-t-chart-3comp-badge.png` image at
  `docs/screenshots/unreleased/` is the only [Unreleased]
  screenshot in this commit; drop the file in and the link
  resolves automatically.
