# PicoHRT — project dossier

> Companion document to the repo [README](../README.md). Where the README
> is a "what is this" page aimed at new users, this page is a "why and how
> did it get here" page aimed at contributors and reviewers who need to
> understand the engineering decisions behind the rebrand and the PK
> refinements layered on top of upstream Featherline.

## Fork relationship

PicoHRT is a fork of [mkx173/Featherline](https://github.com/mkx173/Featherline), maintained at [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT).
The fork starts at upstream `v0.6.0` and carries every change made on
top of that baseline as the work of this fork. The full history is
visible in [CHANGELOG.md](CHANGELOG.md): entries under `## [Unreleased]`
are new in this fork; entries under `## [0.6.0]` and earlier are the
upstream history, preserved verbatim.

| | Upstream (Featherline) | This fork (PicoHRT) |
|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` |
| Repo | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) |
| App display name | Featherline | PicoHRT |
| Privacy policy | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | in-repo [privacy.md](privacy.md) until a hosted policy exists |
| 3-comp engine for TU | not present | added in this fork |
| Gel skin depot | not present | added in this fork |
| Per-user `kClearInjection` / 3-comp `k31` override | not present | added in this fork |

The package rename is a full rebrand — `namespace` and `applicationId`
both change — so the two apps install side-by-side without overwriting
each other. The launcher icon and the splash are kept from upstream
intentionally; only `app_name` was changed. If a future release of this
fork wants to ship its own icon, that is a small follow-up.

## Project mission

The original Featherline mission is preserved: an on-device, encrypted,
no-account HRT medication tracker that runs offline, projects
pharmacokinetics from your own dose history, and respects that this
data is health data and never leaves the device. PicoHRT adds to that:

- **PK accuracy that holds up for long-acting injectable esters.**
  The upstream 1-compartment analytical Bateman shortcut is fine for
  short-acting routes, but testosterone undecanoate's documented
  90 ± 40-day terminal half-life is a deep-peripheral phenomenon that
  1-comp cannot represent. This fork replaces the TU path with a
  3-compartment RK4 solver ([details below](#pk-engine-changes-since-v06)).
- **A skin-depot path for transdermal testosterone gel.** Upstream
  treats gel as a regular 1-comp dose, which systematically under-
  projects steady-state AndroGel by 38–57 %. This fork adds a
  2-compartment Michaelis-Menten + kCl skin depot so the gel curve
  matches the package insert ([details below](#pk-engine-changes-since-v06)).
- **A per-user override layer for the parameters users can legitimately
  see themselves in.** `kClearInjection` and the 3-comp `k31` are
  exposed to the user as DataStore-backed overrides so a user whose
  own labs disagree with the population average can nudge the model
  without a code change ([details below](#per-user-override-layer)).

Everything else — reminder pipeline, widget, journal, blood-test
catalog, backup format, localization, safety policy — is upstream
behavior, preserved without modification.

## PK engine changes since v0.6

The PK engine in this fork is the cumulative result of five PRs
landed on top of `v0.6.0`. They are listed here in the order they
landed, with their commit and the metric that justified the change.
Full per-scenario tables live in the auto-generated report
`app/app/build/reports/pk-v3-statistical-report.docx` (regenerated on
every `PkStatisticalReportTest` run).

### PR-V3-TU: 3-compartment engine for testosterone undecanoate

**Commit:** PR landed as `bb33a9e` (UI badge) and the engine work
that precedes it; full data class + RK4 implementation is in
`app/src/main/java/com/picohrt/model/pk/PkSimulation.kt`.

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
text mechanically rewritten to `com.picohrt`).

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
3-comp `k31`. k12, k21, k13 are deliberately *not* in scope —
the muscle-equilibrium rates affect the early peak, and getting
those wrong is more dangerous than getting the central clearance
or the deep-peripheral return rate wrong, so they are reserved
for a future "advanced override" mode with stronger warnings.

**What's not done yet (next PR).** The UI to actually edit the
overrides from the home chart's "3-comp" chip. The data layer is
ready and the chip is wired, so this is purely Compose work on
top of an existing entry point.

## Build, test, and CI changes

- `app/build.gradle.kts`: `namespace` and `applicationId` changed
  from `com.mkx.hrttracker` to `com.picohrt`. `versionCode` is
  derived from `gitCommitCount` and reads as the cumulative commit
  count since the v0.6 baseline — currently 39 at the time of the
  rebrand.
- `settings.gradle.kts`: `rootProject.name` set to `PicoHRT`.
- `app/src/main/res/values/strings.xml` (and all locale variants):
  `app_name` set to `PicoHRT`. Onboarding copy that referenced
  "Featherline" updated to "PicoHRT". The upstream `privacy_policy_url`
  is preserved with a comment in [privacy.md](privacy.md) explaining
  the relationship; until a hosted PicoHRT privacy policy exists, the
  in-repo doc is the technical reference.
- `app/build.gradle.kts` line 69 (`THIRD_PARTY_NOTICES_URL`):
  the URL the running app uses to deep-link to the third-party
  notices HTML now points to the PicoHRT repo, not the upstream.

## File map (only files touched by the rebrand, in summary)

- All `app/src/main/java/com/mkx/hrttracker/**/*.kt` →
  `app/src/main/java/com/picohrt/**/*.kt` (301 files, mechanical
  package text + path move; the actual content is unchanged for
  non-PK files)
- All `app/src/test/java/com/mkx/hrttracker/**/*.kt` →
  `app/src/test/java/com/picohrt/**/*.kt` (268 files, same
  treatment)
- All `app/src/androidTest/java/com/mkx/hrttracker/**/*.kt` →
  `app/src/androidTest/java/com/picohrt/**/*.kt` (35 files, same
  treatment; two of them also gained an `onHrtGoalChange = {}`
  parameter to keep up with the F-PR1 SettingsScreen signature
  change)
- `app/src/main/AndroidManifest.xml`: unchanged (it already used
  relative `.HrtTrackerApplication` class names, which resolve to
  the new namespace)
- `app/src/main/res/xml/hrt_widget_*.xml`: `android:configure` and
  related `com.mkx.hrttracker` references rewritten to `com.picohrt`
- `benchmark/src/main/java/com/mkx/hrttracker/macrobenchmark/**` →
  `benchmark/src/main/java/com/picohrt/macrobenchmark/**` (1 file)
- `benchmark/build.gradle.kts`: namespace rewritten
- `app/src/main/res/values*/strings.xml` (5 locales): `app_name`
  and onboarding copy rebranded
- `fastlane/metadata/android/{en-US,zh-CN}/*`: title and description
  rebranded
- `app/src/main/java/com/picohrt/data/backup/BackupExportService.kt`:
  default backup filename `featherline-backup` → `picohrt-backup`
- `app/src/main/java/com/picohrt/util/AppDiagnosticsExportService.kt`:
  diagnostic-export header `Featherline diagnostic logs` →
  `PicoHRT diagnostic logs`
- `app/src/main/java/com/picohrt/ui/settings/SettingsScreen.kt`:
  `APP_REPOSITORY_URL` now points to the PicoHRT repo
- `README.md`, `README.zh-CN.md`: rewritten with "Based on" / "Fork
  relationship" sections
- `CONTRIBUTING.md`: header reworded with the fork relationship
- `docs/CHANGELOG.md`, `docs/CHANGELOG.zh-Hans.md`: pre-`v0.6.0`
  entries kept verbatim as upstream history; post-`v0.6.0` entries
  describe the work in this fork
- `docs/CHANGELOG.md`: APK filename pattern updated from
  `featherline-all-abis-1.3.3-<commit>-36.apk` to
  `picohrt-all-abis-1.3.3-<commit>-39.apk`
- `tools/_rebrand_strings.py`, `tools/_rebrand_strings_v2.py`:
  tooling scripts that were used to do the bulk string-rename
  step. Kept in-tree so the rebrand is reproducible from a clean
  checkout

## Credits

- [mkx173/Featherline](https://github.com/mkx173/Featherline) — the
  upstream project. Every line of code, every UI choice, and every
  test in this fork under `v0.6.0` is upstream's work.
- [LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test) — the
  mathematical reference for the PK projection.
- [TransmtfTeam/Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker) — estradiol undecylate PK
  parameters.
- [SmirnovaOyama/Oyama-s-HRT-Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker) — plot display logic.
- The trans health community for testing, feedback, and the prior
  art that makes a tool like this possible.
