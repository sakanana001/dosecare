<p align="center">
  <img src=".github/app-icon.png" alt="DoseCare icon" width="120" />
</p>

# DoseCare

**English** · [简体中文](README.zh-CN.md)

> **DoseCare lives at [sakanana001/dosecare](https://github.com/sakanana001/dosecare). It is built on two upstream lines of work: the [PicoHRT](https://github.com/sakanana001/PicoHRT) fork of [mkx173/Featherline](https://github.com/mkx173/Featherline) (the hormone-therapy tracker base), and the original [DoseCare](https://github.com/sakanana001/dosecare) pharmacology app (the 202-drug catalog, interaction rules, and plasma-level estimation), which was merged into that base.**
> All credit for the original app, design, and pre-`v0.6.0` engineering goes to the upstream Featherline maintainer. The PicoHRT layer added a rebrand, a refined 3-compartment PK engine for testosterone undecanoate, a gel depot path, and a per-user override layer for `kClearInjection` / `k31`. The DoseCare layer added the pharmacology capability. See [docs/PROJECT.md](docs/PROJECT.md) for the full lineage and rationale, and [docs/CHANGELOG.md](docs/CHANGELOG.md) for the merged history.

On-device medication tracker for hormone therapy **and** polypharmacy: dose logging, PK projection, blood-test tracking, a 202-drug pharmacology catalog, interaction checking, and plasma-level estimation. For transfeminine (MtF), transmasculine (FtM), and non-binary users on hormone therapy, and for anyone keeping track of several psychiatric or chronic-disease medications at once. Encrypted on device, no account required.

[![License: AGPL v3](https://img.shields.io/badge/License-AGPLv3-blue.svg)](LICENSE)
[![Build](https://github.com/sakanana001/dosecare/actions/workflows/android-release.yml/badge.svg)](https://github.com/sakanana001/dosecare/actions/workflows/android-release.yml)
![minSdk](https://img.shields.io/badge/minSdk-26-blue.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-37-blue.svg)
[![Based on Featherline](https://img.shields.io/badge/fork%20of-mkx173%2FFeatherline-1f6feb)](https://github.com/mkx173/Featherline)

DoseCare logs doses across injection, patch, gel, oral, and sublingual routes; projects estradiol levels from your dose history using a three-compartment pharmacokinetic model; and tracks blood test results with automatic unit conversion across canonical and clinical units. On the pharmacology side it ships a bundled 202-entry drug catalog with CYP450 profiles, QTc risk, adverse-effect flags, and therapeutic windows; a local rule engine that checks your current regimen for interactions; a plasma-level (TDM) estimator; and a side-by-side comparison of any two drugs. Everything stays in an encrypted local database — no accounts, no telemetry, no network calls. Backups are encrypted and compressed. Available in English, Simplified Chinese, Traditional Chinese, Cantonese (Hong Kong), and Japanese (translation in progress).

> ⚠️ **Not medical advice.** DoseCare is a tracking tool, not a medical device, and using it does not establish a clinician relationship. The pharmacokinetic projection and the plasma-level estimate are rough population-average estimates from your logged doses — they are not a substitute for blood tests or for a clinician's interpretation, and you should not use them to make dosing changes. The interaction checker is a rule engine over published reference data, not a pharmacist. See [docs/safety.md](docs/safety.md) for the full disclaimer.

## Get the app

- **GitHub Releases** (signed APK for sideload): [releases page](https://github.com/sakanana001/dosecare/releases)
- Or build from source: see [docs/building.md](docs/building.md)

> The upstream Featherline is on the Play Store and F-Droid under
> `com.mkx.hrttracker`; DoseCare is a separate package (`com.dosecare.app`) and
> will not auto-update from the upstream listing. Sideload the latest
> release APK or build from source.

## Features

### Hormone therapy tracking

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
- Journal tab to track meaningful dates on a timeline with milestones, plus per-day notes; pin any date to your home screen as an anchor widget or shortcut showing its running day count
- Home-screen quick-log widget in two sizes, with progress, next-dose, and tap-to-log
- Encrypted, compressed backup format with restore validation
- App lock with biometric unlock
- Material 3 with dynamic color

### Pharmacology & polypharmacy

- **202-drug pharmacology catalog, bundled offline** — 31 pharmacological categories spanning antipsychotics, mood stabilizers, antidepressants, anxiolytics, and the chronic-disease classes that travel with them (antidiabetics, antihypertensives, statins, antiepileptics, PPIs, and more). Every entry carries a CYP450 profile (substrate / inhibitor / inducer), a QTc-prolongation risk level, adverse-effect flags including anticholinergic load, indication groups, overdose information, and monitoring requirements; the drugs with a published therapeutic window also carry the window itself.
- **Drug-interaction checker** — a local rule engine over the drugs you are actually taking: CYP450 inhibition and induction, additive QTc prolongation, 5-HT syndrome risk, and anticholinergic load, plus therapeutic-window deviation and renal/hepatic dose adjustment in the same pass. Severity is graded `LOW` / `MEDIUM` / `HIGH` / `CONTRAINDICATED`. Doses and frequencies come from your own plan, so the exposure fold is scaled to what you really take rather than to a catalogue default. Reachable from Settings → Interactions.
- **Plasma-level estimation (TDM)** — a one-compartment oral PK model estimates the concentration curve for a drug from dose, frequency, and body weight, and overlays that drug's therapeutic window so you can see at a glance whether the estimate sits inside it. Several drugs can share one chart, each with its own colour and window band. Reachable from Settings → Blood level monitoring.
- **Two-drug spec comparison** — pick any two catalog entries and read them side by side across eight sections: basic identity, PK parameters, therapeutic window, CYP role, key adverse effects, dose adjustment, monitoring requirements, and critical interactions. A plain-language mode rewrites the same comparison without the jargon. Reachable from Settings → Two-drug spec comparison.
- **Pharmacology dossier on the medicine detail page** — link a medicine in your plan to its catalog entry and the detail page gains the pharmacology profile for it: indication groups, CYP role, PK estimate, overdose information, and monitoring requirements. The link is stored per medicine and can be removed again.

### Both

- No accounts, no telemetry, no network calls — everything stays on device
- English, Simplified Chinese, Traditional Chinese, Traditional Chinese (Hong Kong, Cantonese), and Japanese — the Japanese translation is still being filled in

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

## Pharmacology toolkit

The pharmacology half of DoseCare is a self-contained, offline reference tool. It is
built from a catalog that ships inside the APK — no network call is made to
populate it — and every number it shows is traceable to a published source.

### The catalog

`app/src/main/assets/pharmacology/catalog.json` holds 202 drug entries across 31
pharmacological categories, each with:

- a **PK model** (one-compartment oral) with `ke`, `Vd`, bioavailability, half-life, and a protein-binding percentage
- a **CYP450 profile**: which enzymes the drug is a substrate of, and whether it inhibits or induces them
- a **risk profile**: QTc prolongation, metabolic syndrome, agranulocytosis, extrapyramidal symptoms, sedation, sexual effects, hyperprolactinaemia, and a 0–3 anticholinergic load
- a **therapeutic window** where one is published, with the unit conversion factor needed to compare it against the modelled concentration
- **renal and hepatic dose adjustment**, elderly notes, and smoking effects
- **overdose information**: symptoms, severity, estimated toxic and fatal doses for a 70 kg adult, management, and specific antidotes
- **monitoring requirements** and **critical interactions** referenced back to their source

The catalog is compiled from published reference material — DrugBank, OpenFDA /
DailyMed, the AGNP 2017 consensus, the Flockhart CYP Drug Interaction Table
(Indiana University), therapeutic drug monitoring guidelines, and Chinese drug
package inserts. See [docs/third-party-notices.md](docs/third-party-notices.md)
for the full attribution list.

### The rule engine

The interaction engine is self-authored and runs entirely on device. Each rule is
a pure function over the drug list plus the patient's profile, so the rules are
individually unit-testable and the engine holds no state. Rules cover:

| Rule | What it looks at |
|---|---|
| CYP450 interaction | A drug's inhibitory or inducing effect on another drug's main metabolic pathway, scaled by dose and frequency |
| QTc additive | The summed QTc effect of every drug in the regimen, plus a sex-specific baseline |
| Anticholinergic load | The summed 0–3 anticholinergic score across the regimen |
| 5-HT syndrome | How many serotonergic drugs are stacked together, and whether any of them are strong agents |
| Therapeutic-window deviation | Whether the modelled concentration falls outside the drug's published window |
| Renal / hepatic adjustment | Whether the regimen needs dose reduction or monitoring for kidney or liver impairment |
| Sedation synergy, bleeding risk, dopamine antagonism, hypotension | Additive pharmacodynamic effects that the metabolism rules do not cover |

The rules are population-level heuristics with documented false-positive and
false-negative behaviour. They do not replace a clinician's judgment or a
therapeutic drug monitoring protocol.

### How the three screens use it

- **Interactions** (Settings → Interactions) — pick the drugs, or import your own regimen, and get a severity-ranked list back.
- **Blood level monitoring** (Settings → Blood level monitoring) — enter dose, frequency, and weight, and read the estimated concentration against the therapeutic window.
- **Two-drug spec comparison** (Settings → Two-drug spec comparison) — put two drugs side by side, with or without the plain-language layer.

## Hormone modes (MtF / FtM / Non-binary)

DoseCare supports both ends of the gender-affirming hormone therapy spectrum
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
| **MtF** (transfeminine) | E2 (estradiol) | E2 patches, gel, oral, sublingual; cyproterone acetate (CPA) antiandrogen | CPA interaction warnings; E2 route-specific Css anchors | The upstream Featherline default; this project preserves it. |
| **FtM** (transmasculine) | T (testosterone) | TE (testosterone enanthate), TC (testosterone cypionate), TU (testosterone undecanoate, Nebido), T gel; **CPA is fully disabled** | TU 90 ± 40 d terminal half-life warning; T gel Css anchor | The v3 3-comp engine, gel skin depot, and per-user kClear / k31 override are designed for this mode. |
| **Non-binary** | 3-way hormone select (E2 / T / both) | all | none goal-specific | Lets users compose their own regimen without being funneled into a binary default. |
| **Unset** (default) | E2 (legacy pre-v0.6) | all | none goal-specific | Backward-compatible default; the app nudges the user to pick a goal on first run after the v0.6 migration. |

### FtM medication keys

DoseCare ships four first-class testosterone medication keys that were
previously only available as custom entries in upstream Featherline. Catalog
entries are pre-loaded so a new user picking FtM sees the full set without
having to type in a `custom` slot.

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

DoseCare is a single-module Android app written in Kotlin with Jetpack Compose. Doses, reminders, and lab results live in a SQLCipher-encrypted Room database. The pharmacokinetic engine is a three-compartment model that converts every logged dose into an estradiol contribution over time, then sums contributions across all routes to produce a projected curve. The testosterone-undecanoate path uses a separate 3-compartment RK4 solver so the deep peripheral reservoir can match the long 90 ± 40-day terminal half-life; the gel route uses a transdermal skin-depot lag instead of a 2-comp muscle depot. Reminders use AlarmManager with exact-alarm permission handling and snooze support; notifications survive reboots and time changes through a reconciliation layer. The blood test catalog defines analytes with bidirectional unit conversion via a canonical factor table. Backups are encrypted, compressed, and use a versioned format with full restore validation.

The pharmacology half sits alongside that: the drug catalog is loaded once at
startup from the bundled asset into an in-memory service, the rule engine
resolves the current regimen into per-drug dose and frequency before it
evaluates, and the plasma-level estimator runs the catalog's one-compartment
model per selected drug. Nothing in that path touches the network either.

The full architecture, data model, and reminder pipeline are documented in [docs/architecture.md](docs/architecture.md). The 3-comp engine, gel depot, and per-user override layer are documented under [docs/PROJECT.md](docs/PROJECT.md) → "PK engine changes since v0.6".

## Lineage

| | Upstream (Featherline) | Intermediate (PicoHRT) | This project (DoseCare) |
|---|---|---|---|
| Package id | `com.mkx.hrttracker` | `com.picohrt` | `com.dosecare.app` |
| Repo | [mkx173/Featherline](https://github.com/mkx173/Featherline) | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) |
| App display name | Featherline | PicoHRT | DoseCare |
| License | GPL-3.0 | GPL-3.0 | AGPL-3.0 |
| Privacy policy | [asterismlabs.io/featherline/privacy/](https://asterismlabs.io/featherline/privacy/) | in-repo [docs/privacy.md](docs/privacy.md) | in-repo [docs/privacy.md](docs/privacy.md) until a hosted policy exists |
| 3-comp engine for TU | not present | added in the PicoHRT layer | carried over |
| Gel skin depot | not present | added in the PicoHRT layer | carried over |
| Per-user kClear / k31 override | not present | added in the PicoHRT layer | carried over |
| 202-drug pharmacology catalog | not present | not present | added in the DoseCare layer |
| CYP450 / QTc interaction rules | not present | not present | added in the DoseCare layer |
| Plasma-level (TDM) estimation | not present | not present | added in the DoseCare layer |
| Two-drug spec comparison | not present | not present | added in the DoseCare layer |

The first fork commit on the PicoHRT repo was `v0.6.0` of upstream. Every entry in
[docs/CHANGELOG.md](docs/CHANGELOG.md) under `## [Unreleased]` is new work;
everything under `## [0.6.0]` and earlier is the upstream history, preserved
verbatim. AGPL-3.0 is a superset of GPL-3.0, so the move from the PicoHRT
layer's GPL-3.0 to this project's AGPL-3.0 is a one-way upgrade, and both
upstream lines are GPL-family and compatible with it.

## Roadmap

- Replace the pharmacokinetic engine with a more general multi-medication model
- Personal-PK calibration tuned from your own lab results
- Optional encrypted cloud backup (off by default, end-to-end encrypted)
- Finish the Japanese translation — contributions welcome on the upstream
  [Hosted Weblate](https://hosted.weblate.org/projects/featherline/) (see
  [docs/localization.md](docs/localization.md))
- Clinical review of the pharmacology catalog entries and the interaction rules
  before any distribution beyond personal use

## Tech stack

Kotlin, Jetpack Compose with Material 3, Hilt for dependency injection, Room with SQLCipher for encrypted persistence, Coroutines + Flow. See [gradle/libs.versions.toml](gradle/libs.versions.toml) for exact versions.

## Building from source

Prerequisites: JDK 17, a recent Android Studio, and an Android SDK whose API level matches the project's `compileSdk`. Standard Android Studio import; run `./gradlew assembleDebug` or use the IDE.

Detailed instructions, flavors, and CI behavior: see [docs/building.md](docs/building.md).

## Contributing

Contributions are welcome. Read the [Code of Conduct](CODE_OF_CONDUCT.md) first, then see [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, branching conventions, and how to propose changes.

## Privacy

DoseCare stores everything on your device in an encrypted database. There are no accounts, no telemetry, and no network calls. The pharmacology catalog ships inside the APK as a static asset, so it does not need a backend either. See [docs/privacy.md](docs/privacy.md) for the full data-handling description. The in-repo doc is the contributor-facing technical companion; until a hosted DoseCare privacy policy exists, the upstream [Featherline privacy policy](https://asterismlabs.io/featherline/privacy/) is the legal reference.

## License

DoseCare is released under the GNU Affero General Public License, version 3. See [LICENSE](LICENSE) for the full text.
Third-party dependency, asset, adapted-code, and data-source notices are listed in [docs/third-party-notices.md](docs/third-party-notices.md).

## Acknowledgments

- The [Material Symbols](https://fonts.google.com/icons) icon set.
- The pharmacokinetic projection draws on the math reference from [HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test).
- The estradiol undecylate pharmacokinetic parameters come from [Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker).
- Plot display logic was adapted from [Oyama's HRT Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker).
- The upstream [Featherline](https://github.com/mkx173/Featherline) project — the base this app builds on.
- The AGNP guideline authors, the Flockhart CYP table maintainers, and the DailyMed / OpenFDA label corpus the drug catalog is compiled from.
- The broader trans health community for testing, feedback, and the prior art that makes a tool like this possible.
