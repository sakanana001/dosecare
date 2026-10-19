# DoseCare — project dossier

> Companion document to the repo [README](../README.md). Where the README
> is a "what is this" page aimed at new users, this page is a "why and how
> did it get here" page aimed at contributors and reviewers who need to
> understand the engineering decisions behind the rebrand, the PK
> refinements layered on top of upstream Featherline, and the pharmacology
> merge that turned the fork into a polypharmacy tool.

## Lineage

DoseCare is maintained at [sakanana001/dosecare](https://github.com/sakanana001/dosecare) and is built on two upstream lines of work that were merged into one codebase:

| Layer | Project | License | What it contributes |
|---|---|---|---|
| Base | [mkx173/Featherline](https://github.com/mkx173/Featherline) | GPL-3.0 | Original app architecture, UI system, data model, reminder pipeline, blood-test catalog, backup format, widget, journal, localization |
| Intermediate | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | GPL-3.0 | The rebrand, the 3-compartment PK engine (Runge-Kutta 4) for testosterone undecanoate, the gel skin depot, the per-user `kClearInjection` / `k31` override, stock tracking, encrypted backup, biometric lock |
| Pharmacology | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) | AGPL-3.0 | The 202-drug catalog, the CYP450 / QTc interaction rule engine, plasma-level (TDM) estimation, two-drug spec comparison, dose-adjustment and monitoring guidance |

The PicoHRT line forked upstream Featherline at `v0.6.0`; every entry in
[CHANGELOG.md](CHANGELOG.md) under `## [Unreleased]` is new work, and
everything under `## [0.6.0]` and earlier is the upstream history,
preserved verbatim. The DoseCare line was a separate app until the
merge described below.

AGPL-3.0 is a superset of GPL-3.0, so the move from the PicoHRT layer's
GPL-3.0 to this project's AGPL-3.0 is a one-way upgrade, and both upstream
lines are GPL-family and compatible with it. See
[third-party-notices.md](third-party-notices.md) for the per-layer
attribution.

| | Upstream (Featherline) | Intermediate (PicoHRT) | This project (DoseCare) |
|---|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` | `com.dosecare.app` |
| Repo | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) |
| App display name | Featherline | PicoHRT | DoseCare |
| License | GPL-3.0 | GPL-3.0 | AGPL-3.0 |
| Privacy policy | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | in-repo [privacy.md](privacy.md) | in-repo [privacy.md](privacy.md) until a hosted policy exists |
| 3-comp engine for TU | not present | added in this layer | carried over |
| Gel skin depot | not present | added in this layer | carried over |
| Per-user `kClearInjection` / 3-comp `k31` override | not present | added in this layer | carried over |
| 202-drug pharmacology catalog | not present | not present | added in the merge |
| CYP450 / QTc interaction rules | not present | not present | added in the merge |
| Plasma-level (TDM) estimation | not present | not present | added in the merge |
| Two-drug spec comparison | not present | not present | added in the merge |

The package rename is a full rebrand — `namespace` and `applicationId`
both change — so the apps install side-by-side without overwriting
each other. The launcher icon and the splash are kept from upstream
intentionally; only `app_name` was changed. If a future release of this
project wants to ship its own icon, that is a small follow-up.

## The pharmacology merge

The DoseCare line contributed a pharmacology engine that the HRT tracker
had no equivalent of. The merge brought it in as a set of new modules
alongside the existing HRT domain, rather than rewriting the HRT side:

- **The catalog.** `app/src/main/assets/pharmacology/catalog.json` ships
  202 drug entries across 31 pharmacological categories. Each entry
  carries a one-compartment oral PK model, a CYP450 profile, a risk
  profile (QTc, metabolic syndrome, agranulocytosis, extrapyramidal,
  sedation, sexual, hyperprolactinaemia, anticholinergic load), dose
  adjustments for renal and hepatic impairment, overdose information,
  monitoring requirements, and critical interactions. It is loaded once
  at startup into an in-memory `DrugCatalogService`, so no lookup ever
  touches disk or network.
- **The rule engine.** `model/pharmacology/RuleEngine.kt` evaluates the
  regimen against ten rule categories — CYP450 interaction (inhibition
  and induction), additive QTc, anticholinergic load, 5-HT syndrome,
  therapeutic-window deviation, renal/hepatic dose adjustment, and four
  additive pharmacodynamic rules (sedation synergy, bleeding risk,
  dopamine antagonism, hypotension). Each rule is a pure function over
  the drug list plus the patient profile, so it is individually
  unit-testable and the engine holds no state.
- **The bridge.** The engine needs a dose and a frequency per drug, not
  just a drug id. `InteractionAnalysisRepository` reads the user's own
  regimen, resolves each catalog-linked medicine's preparation strength
  and its group's schedule, and hands the engine a `UserDrugForRule`.
  That is what makes the exposure folds scale to what the user actually
  takes rather than to a catalogue default. The link itself is a column
  on the user's medicine: `medicines.drugCatalogId` (added in schema
  `v11`; empty string means "not linked").
- **The screens.** Three screens arrived with the merge, deliberately
  not in the bottom navigation — they are pushed on top of the Settings
  tab: `Screen.Interactions` (Settings → Interactions),
  `Screen.Tdm` (Settings → Blood level monitoring), and
  `Screen.Comparison` (Settings → Two-drug spec comparison). The
  medicine detail page gained a `PharmacologyDossierSection` that lets
  the user link a medicine to a catalog entry and read its pharmacology
  profile there.

The merged screens follow the base app's conventions rather than the
standalone app's: `@HiltViewModel` + `hiltViewModel()` instead of
process-wide singleton `MutableStateFlow` objects, string resources for
every user-visible label, and the base app's design system. The 202-drug
catalog is a static asset, so the merge does not introduce a network
dependency.

## Project mission

The original Featherline mission is preserved: an on-device, encrypted,
no-account HRT medication tracker that runs offline, projects
pharmacokinetics from your own dose history, and respects that this
data is health data and never leaves the device. The PicoHRT layer
added to that:

- **PK accuracy that holds up for long-acting injectable esters.**
  The upstream 1-compartment analytical Bateman shortcut is fine for
  short-acting routes, but testosterone undecanoate's documented
  90 ± 40-day terminal half-life is a deep-peripheral phenomenon that
  1-comp cannot represent. That layer replaces the TU path with a
  3-compartment RK4 solver ([details below](#pk-engine-changes-since-v06)).
- **A skin-depot path for transdermal testosterone gel.** Upstream
  treats gel as a regular 1-comp dose, which systematically under-
  projects steady-state AndroGel by 38–57 %. That layer adds a
  2-compartment Michaelis-Menten + kCl skin depot so the gel curve
  matches the package insert ([details below](#pk-engine-changes-since-v06)).
- **A per-user override layer for the parameters users can legitimately
  see themselves in.** `kClearInjection` and the 3-comp `k31` are
  exposed to the user as DataStore-backed overrides so a user whose
  own labs disagree with the population average can nudge the model
  without a code change ([details below](#per-user-override-layer)).

The pharmacology merge adds a fourth thing, and it is a different kind of
thing: not a better estimate of one hormone, but a reference view of the
drug itself. The catalogue says what a drug is metabolised by, what it
interacts with, what a safe concentration looks like, and what to watch
for; the rule engine applies that reference to the user's own regimen.
Everything else — reminder pipeline, widget, journal, blood-test
catalog, backup format, localization, safety policy — is upstream
behavior, preserved without modification.

## PK engine changes since v0.6

The PK engine in this project is the cumulative result of five PRs
landed on top of `v0.6.0` by the PicoHRT layer. They are listed here in
the order they landed, with their commit and the metric that justified
the change.
Full per-scenario tables live in the auto-generated report
`app/app/build/reports/pk-v3-statistical-report.docx` (regenerated on
every `PkStatisticalReportTest` run).

### PR-V3-TU: 3-compartment engine for testosterone undecanoate

**Commit:** PR landed as `bb33a9e` (UI badge) and the engine work
that precedes it; full data class + RK4 implementation is in
`app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt`.

**Why.** Upstream's 1-compartment analytical Bateman shortcut is a
single state `C(t) = (F·D/Vd) · (k_a / (k_a - k)) · (e^(-k·t) - e^(-k_a·t))`.
That's one exponential decay plus one absorption. Nebido's SmPC gives
a 90 ± 40-day terminal half-life, which is the time constant of the
*deep peripheral* compartment, not the central. There is no parameter
choice in a 1-comp model that produces a 90-day tail without also
screwing up the early peak.

**What.** For TU, the engine switches to a 3-state ODE over
`[central, shallow, deep]`, integrated with Runge-Kutta 4. The deep
peripheral reservoir (Vd_d = 3.5 L/kg) holds the long-tailed mass.
Shallow peripheral (Vd_s = 1.0 L/kg, k12 = 0.15 h⁻¹, k21 = 0.18 h⁻¹)
captures the fast muscle-equilibrium that the Cmax timing depends on.
The depot compartment is still solved analytically (two-exponential
absorption from the intramuscular depot) and the analytical depot
amount feeds the RK4 stepper as the source term.

**Result.** TU MAE went from a 1-comp broken (v2 retune: 13.00 ng/mL,
bias −100 %) to 2.53 ng/mL with −9.3 % bias. Overall MAPE went from
47.7 % to 31.6 %.

### PR-V3.1-TU-retune: k31 0.005 → 0.0003

**Why.** The first PR left the deep peripheral half-life at
`ln(2)/0.005 ≈ 138 h ≈ 5.8 d` — a tail in the same ballpark as
short-acting esters. The peak fit (and the per-scenario numbers) were
good, but the long-term forecast was still off.

**What.** Lowered `k31` from 0.005 h⁻¹ to 0.0003 h⁻¹, giving a deep-
peripheral half-life of `ln(2)/0.0003 ≈ 2311 h ≈ 96 d`, at the
middle of the SmPC 90 ± 40 d band. The change has negligible effect
on Cmax / Tmax (the deep→central flux is small in the first four
weeks) and dramatically improves the tail behaviour.

**Result.** v3.1 + PR2 (next) gave an overall MAPE of **20.9 %**
(down from 31.6 %) with TU MAE 2.62 ng/mL and gel MAE 1.21–1.55 ng/mL
for the 5 g / 10 g AndroGel anchors.

### PR2-gel: skin-depot 2-comp MM + kCl for testosterone gel

**Commit:** `a353b10` (work landed before the rebrand, on the
upstream's `com.mkx.hrttracker` package; preserved as-is, package
text mechanically rewritten on each rename since).

**Why.** Upstream treated gel as a regular 1-comp dose with depot
absorption. The actual pharmacokinetics of transdermal testosterone
gel is dominated by the skin reservoir — a stratum-corneum depot
that releases into systemic circulation over many hours, with a
saturable first step and a clearance term.

**What.** Replaced the 1-comp absorption for testosterone gel with
a 2-compartment Michaelis-Menten + kCl model: skin depot → systemic
depot → central, with `kCl` from the systemic depot back to skin.
Only the TESTOSTERONE hormone uses this path; estradiol gel retains
its 1-comp behavior.

**Result.** AndroGel 5 g steady-state under-projection went from
3.22 ng/mL error (1-comp) to 1.21 ng/mL (skin depot). AndroGel 10 g
went from 3.04 ng/mL to 1.55 ng/mL.

### PR3-UI-3comp-badge: "3-comp" chip on the T chart

**Commit:** `bb33a9e`.

**Why.** The engine picks 3-comp automatically when the active
events are TU, but the user has no way to know which engine is
running. If the 3-comp result disagrees with the user's own labs,
they need a visible cue so they can decide whether to look at the
override layer.

**What.** Added a `usesThreeCompModel: Boolean` flag to
`PkTrendResult` and `MainChartUiState`. The home chart header renders
a "3-comp" / "3 室" chip with `tertiaryContainer` background when
the flag is set. Tapping the chip is the future entry point for the
per-user override dialog (not yet wired in this PR — the override
data layer is in place, the UI is the next step).

**Result.** 194 main-UI tests + 68 PK tests all pass with the new
flag and the new chip.

### Per-user override layer

**Commit:** `c4b8957`.

**Why.** Population-average PK models are useful but they aren't
*your* PK. A user with three T labs that consistently sit 15 %
above the population curve is not going to trust a tracker that
stays 15 % below their own blood results. Without an override
layer, the only recourse is "stop using the app" or "fork the
code" — neither is acceptable for a personal tracker.

**What.** Added a `PkTUserOverride` data class with two fields
(`kClearInjection: Double?`, `threeCompK31: Double?`). The engine
reads it as an optional override of the catalog defaults:
`userOverride.kClearInjection ?: core.kClearInjection` and
`userOverride.threeCompK31 ?: threeCompOverrides[compound].k31`.
The values are persisted in two new DataStore keys
(`pkClearInjectionOverrideKey`, `pkK31OverrideKey`), surfaced
through `SettingsRepository` and `SettingsState`, and read by
`MainViewModel` on every `simulateMainTestosteroneTrend()` call.

**Scope.** v1 covers exactly two parameters: `kClearInjection` and
3-comp `k31`. k12, k21, k13 are deliberately *not* in scope — the
muscle-equilibrium rates affect the early peak, and getting
those wrong is more dangerous than getting the central clearance
or the deep-peripheral return rate wrong, so they are reserved
for a future "advanced override" mode with stronger warnings.

**What's not done yet (next PR).** The UI to actually edit the
overrides from the home chart's "3-comp" chip. The data layer is
ready and the chip is wired, so this is purely Compose work on
top of an existing entry point.

## Build, test, and CI changes

- `app/build.gradle.kts`: `namespace` and `applicationId` are
  `com.dosecare.app` — renamed from `com.mkx.hrttracker` by the
  PicoHRT layer, then again by the merge. `versionCode` is derived
  from `gitCommitCount` and reads as the cumulative commit count;
  `versionName` is `2.0.0`.
- `settings.gradle.kts`: `rootProject.name` set to `DoseCare`. The
  root project name also drives the APK file name, so artifacts are
  `dosecare-<abi>-<versionName>-<versionCode>.apk`.
- `app/src/main/res/values/strings.xml` (and all locale variants):
  `app_name` set to `DoseCare`. The upstream `privacy_policy_url`
  is preserved with a comment in [privacy.md](privacy.md) explaining
  the relationship; until a hosted DoseCare privacy policy exists, the
  in-repo doc is the technical reference.
- `app/build.gradle.kts` (`THIRD_PARTY_NOTICES_URL`): the URL the
  running app uses to deep-link to the third-party notices now points
  to this repo at the current commit, not the upstream.
- `app/src/main/java/com/dosecare/app/data/backup/BackupExportService.kt`:
  default backup file name `featherline-backup` → `dosecare-backup`.
- `app/src/main/java/com/dosecare/app/util/AppDiagnosticsExportService.kt`:
  diagnostic-export file name `dosecare-diagnostics-<timestamp>.txt`,
  header `DoseCare diagnostic logs`.
- `app/src/main/java/com/dosecare/app/ui/settings/SettingsScreen.kt`:
  `APP_REPOSITORY_URL` points to `https://github.com/sakanana001/dosecare`.
- `LICENSE`: AGPL-3.0, for the reasons in [Lineage](#lineage).

## File map (rename history, in summary)

- `app/src/main/java/com/mkx/hrttracker/**/*.kt` →
  `app/src/main/java/com/dosecare/app/**/*.kt` (the rename ran twice —
  first to `com/picohrt` by the PicoHRT layer, then to
  `com/dosecare/app` by the merge). The pharmacology modules arrived
  from the standalone app already namespaced `com.dosecare.app.*`
  (`model/pharmacology`, `ui/interaction`, `ui/tdm`, `ui/compare`),
  which is why the merge's rename landed on the same target namespace
  the merged-in code already used.
- `app/src/test/java/**` and `app/src/androidTest/java/**` follow the
  same package move
- `benchmark/src/main/java/com/mkx/hrttracker/macrobenchmark/**` →
  `benchmark/src/main/java/com/dosecare/app/macrobenchmark/**`
- `app/src/main/AndroidManifest.xml`: the class name stayed relative
  (`.HrtTrackerApplication`) and resolves to the new namespace
- `app/src/main/res/xml/hrt_widget_*.xml`: `android:configure` and
  related package references rewritten
- `app/src/main/res/values*/strings.xml` (5 locales): `app_name` and
  onboarding copy rebranded
- `app/src/main/assets/pharmacology/catalog.json`: the 202-drug
  catalog, added by the merge
- `fastlane/metadata/android/{en-US,zh-CN}/*`: title and description
  rewritten for the merged app
- `docs/PROJECT.md`, `docs/PROJECT.zh-Hans.md`: this lineage document
- `tools/_rebrand_strings.py`, `tools/_rebrand_strings_v2.py`: tooling
  scripts used for the bulk string-rename step. Kept in-tree so the
  rename history is reproducible from a clean checkout

## Credits

- [mkx173/Featherline](https://github.com/mkx173/Featherline) — the
  upstream project. Every line of code, every UI choice, and every
  test under `v0.6.0` is upstream's work.
- [LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test) — the
  mathematical reference for the PK projection.
- [TransmtfTeam/Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker) — estradiol undecylate PK
  parameters.
- [SmirnovaOyama/Oyama-s-HRT-Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker) — plot display logic.
- The AGNP guideline authors, the Flockhart CYP table maintainers, and
  the DailyMed / OpenFDA label corpus the drug catalog is compiled from.
- The trans health community for testing, feedback, and the prior
  art that makes a tool like this possible.
