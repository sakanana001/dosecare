<p align="center">
  <img src=".github/app-icon.png" alt="PicoHRT icon" width="120" />
</p>

# PicoHRT

**English** · [简体中文](README.zh-CN.md)

> **PicoHRT is a fork of [mkx173/Featherline](https://github.com/mkx173/Featherline), maintained at [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT).**
> All credit for the original app, design, and pre-`v0.6.0` engineering goes to
> the upstream maintainer. PicoHRT continues from `v0.6.0` with a rebrand,
> a refined 3-compartment PK engine for testosterone undecanoate, gel depot
> support, and a per-user override layer for kClearInjection / k31. See
> [docs/PROJECT.md](docs/PROJECT.md) for the full fork rationale and
> [docs/CHANGELOG.md](docs/CHANGELOG.md) for the merged history.

HRT medication tracker for Android with PK projections and lab tracking — for transfeminine (MtF), transmasculine (FtM), and non-binary users. On-device, encrypted, no account required.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Build](https://github.com/sakanana001/PicoHRT/actions/workflows/android-release.yml/badge.svg)](https://github.com/sakanana001/PicoHRT/actions/workflows/android-release.yml)
![minSdk](https://img.shields.io/badge/minSdk-26-blue.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-37-blue.svg)
[![Based on Featherline](https://img.shields.io/badge/fork%20of-mkx173%2FFeatherline-1f6feb)](https://github.com/mkx173/Featherline)

PicoHRT logs doses across injection, patch, gel, oral, and sublingual routes; projects estradiol levels from your dose history using a three-compartment pharmacokinetic model; and tracks blood test results with automatic unit conversion across canonical and clinical units. Everything stays in an encrypted local database — no accounts, no telemetry, no network calls. Backups are encrypted and compressed. Available in English and Simplified Chinese.

> ⚠️ **Not medical advice.** PicoHRT is a tracking tool, not a medical device, and using it does not establish a clinician relationship. The pharmacokinetic projection is a rough population-average estimate from your logged doses — it is not a substitute for blood tests or for a clinician's interpretation, and you should not use it to make dosing changes. See [docs/safety.md](docs/safety.md) for the full disclaimer.

## Get the app

- **GitHub Releases** (signed APK for sideload): [releases page](https://github.com/sakanana001/PicoHRT/releases)
- Or build from source: see [docs/building.md](docs/building.md)

> The upstream Featherline is on the Play Store and F-Droid under
> `com.mkx.hrttracker`; PicoHRT is a separate package (`com.picohrt`) and
> will not auto-update from the upstream listing. Sideload the latest
> release APK or build from source.

## Features

- Log doses across injection, patch, gel, oral, and sublingual routes
- Attach a free-text **note** (2–6 lines) to each manual medication log entry — e.g. side effects, mood, food, injection site, or anything else worth remembering when you review history later; blank notes are stored as `NULL`
- **Hormone modes (MtF / FtM / Non-binary)** — pick a goal in Settings → Personalization → HRT goal; the home chart default, available medications, and safety prompts follow (see [Hormone modes](#hormone-modes-mtf--ftm--non-binary) below)
- **MtF** — estradiol patches, gel, oral, sublingual; optional cyproterone acetate (CPA) antiandrogen
- **FtM** — first-class medication keys for testosterone enanthate (TE), testosterone cypionate (TC), testosterone undecanoate (TU, Nebido), and testosterone gel; CPA is fully disabled
- Configurable reminder schedules with snooze and exact-alarm handling
- Group medications and apply schedules to grouped doses
- Optional medicine stock tracking with low-stock warnings and schedule-aware "days remaining" estimates
- Estradiol pharmacokinetic projection from your dose history
- **3-compartment PK engine for testosterone undecanoate** (Runge-Kutta 4; deep peripheral gives 90 ± 40-day terminal half-life to match Nebido SmPC)
- **Gel skin depot** for testosterone gel (transdermal absorption lag, not 2-comp muscle depot)
- **Per-user override** for `kClearInjection` and 3-comp `k31` from the home chart "3-comp" badge menu (intended for FtM users whose own labs disagree with the population average)
- Blood test catalog with automatic unit conversion (pg/mL ↔ pmol/L, ng/dL ↔ nmol/L)
- Encrypted, compressed backup format with restore validation
- App lock with biometric unlock
- Home-screen quick-log widget in two sizes, with progress, next-dose, and tap-to-log
- Journal tab to track meaningful dates on a timeline with milestones, plus per-day notes; pin any date to your home screen as an anchor widget or shortcut showing its running day count
- No accounts, no telemetry, no network calls — everything stays on device
- English, Simplified Chinese, Traditional Chinese, and Traditional Chinese (Hong Kong, Cantonese)
- Material 3 with dynamic color

## Screenshots

<table>
  <tr>
    <td width="25%"><img src=".github/screenshots/home.png" alt="Home screen"></td>
    <td width="25%"><img src=".github/screenshots/plan.png" alt="Plan view"></td>
    <td width="25%"><img src=".github/screenshots/history.png" alt="History log"></td>
    <td width="25%"><img src=".github/screenshots/calibration.png" alt="Calibration screen"></td>
  </tr>
  <tr>
    <td align="center">Home</td>
    <td align="center">Plan</td>
    <td align="center">History</td>
    <td align="center">Calibration</td>
  </tr>
</table>

## Hormone modes (MtF / FtM / Non-binary)

PicoHRT supports both ends of the gender-affirming hormone therapy spectrum
through a single `HrtGoal` enum (`UNSET` / `MTF` / `FTM` / `NON_BINARY`).
The selected goal controls three things:

1. **Default home chart** — the trend that opens when you launch the app.
2. **Available medications** — the medication keys surfaced in the catalog and the planning flows.
3. **Safety prompts** — which "watch out for this" messages the app shows around doses and labs.

Set or change the goal in **Settings → Personalization → HRT goal**. The
default is `UNSET`, which preserves the pre-v0.6 behavior (E2 chart, all
chips visible, no goal-specific prompts) so existing users see no
regression on upgrade.

| Mode | Default home chart | Available medications | Safety prompts | Notes |
|---|---|---|---|---|
| **MtF** (transfeminine) | E2 (estradiol) | E2 patches, gel, oral, sublingual; cyproterone acetate (CPA) antiandrogen | CPA interaction warnings; E2 route-specific Css anchors | The upstream Featherline default; this fork preserves it. |
| **FtM** (transmasculine) | T (testosterone) | TE (testosterone enanthate), TC (testosterone cypionate), TU (testosterone undecanoate, Nebido), T gel; **CPA is fully disabled** | TU 90 ± 40 d terminal half-life warning; T gel Css anchor | The v3 3-comp engine, gel skin depot, and per-user kClear / k31 override are designed for this mode. |
| **Non-binary** | 3-way hormone select (E2 / T / both) | all | none goal-specific | Lets users compose their own regimen without being funneled into a binary default. |
| **Unset** (default) | E2 (legacy pre-v0.6) | all | none goal-specific | Backward-compatible default; the app nudges the user to pick a goal on first run after the v0.6 migration. |

### FtM medication keys

PicoHRT (this fork) ships four first-class testosterone medication keys
that were previously only available as custom entries in upstream
Featherline. Catalog entries are pre-loaded so a new user picking FtM
sees the full set without having to type in a `custom` slot.

| `MedicationKey` | `PkCompound` | Route | Half-life (literature) | Notes |
|---|---|---|---|---|
| `TESTOSTERONE_ENANTHATE` | `TE` | IM (200 mg/mL) | t½ ≈ 4.5 d | Short-acting ester; weekly or biweekly |
| `TESTOSTERONE_CYPIONATE` | `TC` | IM (200 mg/mL) | t½ ≈ 8 d | Slightly longer than TE; weekly or biweekly |
| `TESTOSTERONE_UNDECANOATE` | `TU` | IM (1000 mg/4 mL, Nebido) | t½ ≈ 21 d absorption, **90 ± 40 d** terminal | Uses the v3 3-comp RK4 engine with deep peripheral reservoir |
| `TESTOSTERONE_GEL` | `T` (AndroGel kinetics) | transdermal (5 / 10 g/day) | steady-state Css anchor | Uses the PR2 2-comp skin depot (MM + kCl) instead of a 2-comp muscle depot |

The full FtM rationale, anchor numbers, and historical PR breakdown
(F-PR1 goal enum → F-PR2 first-class keys → F-PR3 chart default → F-PR4
safety prompts) lives in [docs/ftm-hrt.md](docs/ftm-hrt.md). The PR
summary is in [docs/ftm-pr-summary.md](docs/ftm-pr-summary.md).

### What "model" means here

"Hormone mode" is a single switch the app reads in three places: the
home chart's default selection, the medication catalog's
`PkCompound` filter, and the safety-prompt registration list. There is
no per-user "do I want the safety prompt" override — pick a goal and
the prompts that go with it are on. The design is intentional: a
single coherent mode per user, not a pile of independent toggles.

## How it works

PicoHRT is a single-module Android app written in Kotlin with Jetpack Compose. Doses, reminders, and lab results live in a SQLCipher-encrypted Room database. The pharmacokinetic engine is a three-compartment model that converts every logged dose into an estradiol contribution over time, then sums contributions across all routes to produce a projected curve. The testosterone-undecanoate path uses a separate 3-compartment RK4 solver so the deep peripheral reservoir can match the long 90 ± 40-day terminal half-life; the gel route uses a transdermal skin-depot lag instead of a 2-comp muscle depot. Reminders use AlarmManager with exact-alarm permission handling and snooze support; notifications survive reboots and time changes through a reconciliation layer. The blood test catalog defines analytes with bidirectional unit conversion via a canonical factor table. Backups are encrypted, compressed, and use a versioned format with full restore validation.

The full architecture, data model, and reminder pipeline are documented in [docs/architecture.md](docs/architecture.md). The 3-comp engine, gel depot, and per-user override layer are documented under [docs/PROJECT.md](docs/PROJECT.md) → "PK engine changes since v0.6".

## Fork relationship

| | Upstream (Featherline) | This fork (PicoHRT) |
|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` |
| Repo | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) |
| App display name | Featherline | PicoHRT |
| Privacy policy | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | in-repo [docs/privacy.md](docs/privacy.md) until a hosted policy exists |
| 3-comp engine | not present | added in this fork |
| Gel skin depot | not present | added in this fork |
| Per-user kClear / k31 override | not present | added in this fork |

The first fork commit on this repo is `v0.6.0` of upstream. Every entry in
[docs/CHANGELOG.md](docs/CHANGELOG.md) under `## [Unreleased]` is new work
in this fork; everything under `## [0.6.0]` and earlier is the upstream
history, preserved verbatim.

## Roadmap

- Replace the pharmacokinetic engine with a more general multi-medication model
- Personal-PK calibration tuned from your own lab results
- Optional encrypted cloud backup (off by default, end-to-end encrypted)
- Additional languages — translation contributions welcomed on the upstream
  [Hosted Weblate](https://hosted.weblate.org/projects/featherline/) (see
  [docs/localization.md](docs/localization.md)) until a PicoHRT project is
  registered

## Tech stack

Kotlin, Jetpack Compose with Material 3, Hilt for dependency injection, Room with SQLCipher for encrypted persistence, Coroutines + Flow. See [gradle/libs.versions.toml](gradle/libs.versions.toml) for exact versions.

## Building from source

Prerequisites: JDK 17, a recent Android Studio, and an Android SDK whose API level matches the project's `compileSdk`. Standard Android Studio import; run `./gradlew assembleDebug` or use the IDE.

Detailed instructions, flavors, and CI behavior: see [docs/building.md](docs/building.md).

## Contributing

Contributions are welcome. Read the [Code of Conduct](CODE_OF_CONDUCT.md) first, then see [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, branching conventions, and how to propose changes.

## Privacy

PicoHRT stores everything on your device in an encrypted database. There are no accounts, no telemetry, and no network calls. See [docs/privacy.md](docs/privacy.md) for the full data-handling description. The in-repo doc is the contributor-facing technical companion; until a hosted PicoHRT privacy policy exists, the upstream [Featherline privacy policy](https://asterismlabs.io/featherline/privacy/) is the legal reference.

## License

PicoHRT is released under the GNU General Public License, version 3.0. See [LICENSE](LICENSE) for the full text.
Third-party dependency, asset, and adapted-code notices are listed in [docs/third-party-notices.md](docs/third-party-notices.md).

## Acknowledgments

- The [Material Symbols](https://fonts.google.com/icons) icon set.
- The pharmacokinetic projection draws on the math reference from [HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test).
- The estradiol undecylate pharmacokinetic parameters come from [Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker).
- Plot display logic was adapted from [Oyama's HRT Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker).
- The upstream [Featherline](https://github.com/mkx173/Featherline) project — the base that PicoHRT builds on.
- The broader trans health community for testing, feedback, and the prior art that makes a tool like this possible.
