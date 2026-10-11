# Third-party notices

DoseCare is released under [AGPL-3.0](../LICENSE). This page lists third-party
runtime dependencies, assets, adapted source material, and data sources used by
the app.

## Fork lineage

DoseCare is built on a stack of upstream and sibling work. All three licenses
are GPL-family and compatible with AGPL-3.0 (AGPL-3.0 is a superset of GPL-3.0),
so the combined work can be distributed under AGPL-3.0.

| Layer | Project | License | Role |
|---|---|---|---|
| Base | [mkx173/Featherline](https://github.com/mkx173/Featherline) | GPL-3.0 | Original app architecture, UI system, data model, reminder pipeline |
| Intermediate | [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT) | GPL-3.0 | 3-compartment PK engine (Runge-Kutta 4), gel skin depot, per-user `kClearInjection`/`k31` override, stock tracking, journal timeline, Glance widget, encrypted backup, biometric lock |
| Pharmacology | [sakanana001/dosecare](https://github.com/sakanana001/dosecare) | AGPL-3.0 | 202-drug catalog, CYP450/QTc interaction rule engine, TDM therapeutic windows, dose-adjustment and monitoring guidance |

Credit for the original app design and all pre-`v0.6.0` engineering goes to the
Featherline maintainer.

## Generated dependency inventory

The runtime dependency inventory is generated from Gradle metadata:

- [HTML dependency report](generated/dependency-license-report.html)
- [CSV dependency report](generated/dependency-license-report.csv)

Regenerate these files before a release:

```bash
./gradlew generateLicenseReport --no-parallel
```

## Manual notices

The generated report only sees Gradle dependencies. These entries cover copied,
adapted, or design-derived material that Gradle cannot detect.

### Radix Colors

DoseCare's medication group color palettes adapt selected scale values from
[Radix Colors](https://github.com/radix-ui/colors).

- License: MIT License
- Copyright: Copyright (c) 2021-2022 Modulz; Copyright (c) 2022-Present WorkOS
- Local license text: [MIT.txt](licenses/MIT.txt)

### Material Symbols

DoseCare includes Material Symbols vector icons from Google.

- License: Apache License, Version 2.0
- Source: [Material Symbols guide](https://developers.google.cn/fonts/docs/material_symbols?hl=en)
- Local license text: [APACHE-2.0.txt](licenses/APACHE-2.0.txt)

### Oyama's HRT Tracker

DoseCare adapts plot display logic from
[Oyama's HRT Tracker](https://github.com/SmirnovaOyama/Oyama-s-HRT-Tracker).

- License: MIT License
- Copyright: Copyright (c) 2025 Joseph Smirnova Oyama
- Local license text: [MIT.txt](licenses/MIT.txt)

### HRT-Recorder-PKcomponent-Test

DoseCare's pharmacokinetic projection draws on the model and math reference
from [HRT-Recorder-PKcomponent-Test](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test).
This is listed here as a source attribution for the PK reference material.

### Transmtf-HRT-Tracker

Estradiol undecylate pharmacokinetic parameters come from
[Transmtf-HRT-Tracker](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker).

## Drug catalog data sources

The bundled 202-drug catalog is compiled from the following published sources.
It is reference material only and is **not** medical advice.

- **DrugBank** — drug identities, indications, pharmacology (academic-use subset)
- **OpenFDA / DailyMed** — label text, warnings, adverse reactions, dosage forms
- **AGNP 2017** — antipsychotic dosing consensus
- **Flockhart CYP Table** (Indiana University) — CYP450 substrate / inhibitor /
  inducer assignments used by the interaction rule engine
- **Therapeutic drug monitoring guidelines** — therapeutic windows and sampling
  timing used by the TDM module
- **Chinese drug package inserts** — localized brand names and dosing

Every field that could influence a dosing decision is sourced and carries a
reference in the catalog entry. Clinical review is required before any release
that widens distribution beyond personal use.

## Assay and interaction rules

The CYP450, QTc, bleeding-risk, dopamine-blockade, hypotension, hepatic, and
renal interaction rules are self-authored from the sources above. They are
population-level heuristics with documented false-positive and false-negative
behavior, and they do not replace a clinician's judgment or a therapeutic drug
monitoring protocol.
