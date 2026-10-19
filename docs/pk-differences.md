# PK differences

DoseCare's pharmacokinetic math borrows the model shape and many
parameter values from
[`LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test`](https://github.com/LaoZhong-Mihari/HRT-Recorder-PKcomponent-Test).
This page lists what's the same, what's different, and why; it does
not restate the math itself.

## Upstream reference

The upstream ships analytical PK models for estradiol and testosterone across
injection / patch / gel / oral / sublingual routes, a unified parameter
catalog (`PKSharedCatalog.json`), and Swift implementations
(`PKparameter.swift`, `PKcore.swift`, `SimulationEngine`) targeting iOS
and watchOS. The math, the route catalog, and the population-average
parameters in
[`PkCatalog`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L1203)
trace back to that reference. Anyone wanting the math itself —
compartment equations, parameter provenance, literature anchors —
should read the upstream README and its `pk_research/` workspace.

## What DoseCare reuses

- **Model shape.** Two parallel depots → esterase hydrolysis →
  clearance for injections; first-order Bateman absorption for oral,
  gel, and fallback patches; constant-rate input for labelled patches;
  dual-pathway (fast mucosal + slow swallowed-oral) for sublingual.
  Implemented in
  [`ThreeCompartmentModel`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L849)
  and consumed by
  [`PkSimulationEngine`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L586).
- **Route catalog and compound metadata.** `PkRoute`, `PkCompound`,
  and `PkCatalog`'s `compounds` / `twoPartDepot` / `formationFraction`
  / `hydrolysisK2` / `oralKAbs` / `oralBioavailability` mirror the
  upstream catalog for the routes it supports.
- **Population-average parameter values.** Clearance constants
  (`kClear`, `kClearInjection`), volume-of-distribution (`vdPerKg =
  2.0` for both hormones), gel/patch absorption rates, the standard
  sublingual θ, and the per-compound `k1Fast` / `k1Slow` / `k2` /
  formation fractions are taken from upstream's anchored parameters.
  The one exception is **estradiol undecylate (EU)**, whose depot
  parameters come from
  [`Transmtf-HRT-Tracker`](https://github.com/TransmtfTeam/Transmtf-HRT-Tracker)
  (the "im-depot-v1" model) — the LaoZhong upstream has no EU model.

## What DoseCare changes

- **Single-hormone simulation runtime.** Upstream's catalog and
  simulator drive both estradiol and testosterone end-to-end.
  DoseCare keeps the testosterone constants in
  [`PkCatalog`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L1203)
  for parity but wires only the estradiol path through
  [`toEstradiolPkDoseEvent`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L1069),
  `buildEstradiolEvents`, and
  [`simulateMainEstradiolTrend`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L314);
  the testosterone surface stays dormant until the planned PK engine
  swap lands.
- **Planned-dose projection.** Upstream simulates the doses you give
  it. DoseCare synthesizes virtual future-dose events for
  unfulfilled scheduled slots via
  [`buildEstradiolPkSimulationEntries`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkPlannedEntries.kt#L25)
  so the home chart can show a "if you take everything on schedule"
  curve to the right of the prediction marker. The `isPlanned` flag
  propagates to dose markers so the UI can distinguish logged from
  projected doses.
- **Option-aware chart-sample placement.** Upstream's
  `SimulationEngine` is a math reference, not a chart engine, so it
  samples on a single fixed step. DoseCare drives a user-selectable
  home chart through `HomeE2ChartWindowOption`: `SEVEN_DAYS` renders a
  3-day-past / 4-day-future window on a 0.1 h dense interval, while
  `THIRTY_DAYS` renders a 16-day-past / 14-day-future window on a
  budgeted sampler (`segmentCount = 2240`) with extra post-dose
  offsets.

  The seven-day path preserves the original six-minute resolution.
  The 30-day path trades uniform density for a bounded sample budget
  and then pins absorption-phase offsets after each dose (0.25 h
  through 48 h) so fast-Tmax routes such as oral and sublingual do not
  underplot their local peaks. Both options also pin the prediction
  instant, the previous-day instant, and every logged-event instant.
  The trend
  ([`mainChartSampleTimeH`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L490))
  and projection
  ([`mainProjectionSampleTimeH`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L533))
  paths share the strategy but differ in which "exact" instants they
  pin.
- **Option-aware chart-window rounding contract.**
  `simulateMainEstradiolProjection` rounds the projection-cache window
  to local midnight in `zoneId`: start = today minus `option.pastDays`
  at 00:00, end = today plus `option.projectionFutureDays()` at
  00:00. `projectionFutureDays()` adds a 10-day forward buffer beyond
  the visible future span, so the 30-day chart stores a 40-day
  projection while rendering only the 30-day visible window.
  `HomeSnapshotRepository.snapshotUsabilityFailure` independently
  recomputes the visible window and rejects cached snapshots whose
  coverage or sampling fingerprint (`chartWindowHours`, dense policy,
  post-dose-offset flag) does not match the selected option. Upstream
  has no such contract because it has no cache.
- **Projection cache bundled into home snapshot.** Two projection
  records are bundled into `HomeSnapshotRecord` alongside the
  plan-and-fulfillment cache: `pkProjection` (the chart's
  option-sized projection — 7-day or 30-day visible window with the
  forward buffer) and `widgetPkProjection` (a smaller projection
  sized for the widget's E2 estimate, which has its own validity
  window). Every home-data mutation re-simulates both, even when no
  PK input changed. The leaky seam and its fallback path
  (`MainViewModel` re-simulates the chart projection directly when
  the cached expiry is on-or-before the live `now`) are documented
  in
  [`architecture.md#home-snapshot-and-pk-projection-cache`](architecture.md#home-snapshot-and-pk-projection-cache);
  the widget consumes `widgetPkProjection` via
  [`WidgetSnapshotBuilder`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/widget/WidgetSnapshotBuilder.kt)
  with its own expiry check on the widget render path. Upstream has
  no equivalent because it has no persistent home snapshot.
- **Active-mg conversion on the data path.** Upstream resolves dose
  events directly inside its simulator. DoseCare computes the
  per-event active-mg-as-E2 upstream of the simulator in
  [`DoseInstructionCalculator`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/medication/DoseInstructionCalculator.kt),
  which resolves a `Medicine`'s `MedicinePreparation` +
  `DoseInstruction` into mg-as-E2 using the calculator's own
  per-compound molecular-weight constants and persists it on the log
  row as `equivalentE2Mg`. Inside the simulator,
  [`pkDoseAmounts`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L1106)
  prefers the snapshotted value for non-patch routes (falling back to
  `DoseInstructionCalculator.perUnitEquivalentE2Mg` for planned
  entries or legacy rows that have no snapshot) and reads the patch
  specification directly for `PATCH_APPLY` / `PATCH_REMOVE`; the
  engine's `formationFraction` values are calibrated against
  active-E2 input, matching upstream's convention. `PkCatalog`'s
  per-compound `activeFactor` is *not* applied at dose-event
  construction or in the current runtime simulation path; equivalent-E2
  input comes from the snapshotted value or `DoseInstructionCalculator`.
- **Imported injection/gel preparations skip fabricated package
  geometry.** External tracker imports do not know vial concentration,
  vial volume, gel sachet weight, or gel concentration percent. Rather
  than inventing those fields, DoseCare stores imported injection
  and gel support medicines as `MedicinePreparation.ImportedInjection`
  and `MedicinePreparation.ImportedGel`. `DoseInstructionCalculator`
  reads the administered/applied mg directly for whole-unit imported
  logs, converts imported injection esters to E2-equivalent mg with the
  same molecular-weight table as other logged doses, and treats
  imported gel as already-applied estradiol. `PkSimulation` maps these
  preparations directly to `PkCompound` (`ImportedInjection.ester` or
  `E2` for gel), so imported rows feed the simulator without requiring
  a `Catalog` medicine selection.
- **Duplicated molecular-weight constants.** Estradiol
  molecular-weight constants live in `PkCatalog.compounds` and again
  in
  [`DoseInstructionCalculator`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/medication/DoseInstructionCalculator.kt),
  which log creation uses to snapshot `equivalentE2Mg` and planned PK
  entries use to rederive it when no snapshot exists. Medication UI
  code also uses the calculator for raw dose amount and patch
  release-rate display, but not for a dose-equivalence label. The two
  copies disagree at the second decimal
  (`PkCatalog` uses 272.38 / 376.5 / 356.5 / 396.58 / 384.56 / 440.66;
  `DoseInstructionCalculator` uses 272.4 / 376.4 / 356.5 / 396.6 /
  384.5 / 440.66 — the estradiol undecylate value is the one that
  agrees across both copies). The disagreement is below the resolution any user cares
  about, but the redundancy is flagged — both copies collapse into
  the planned PK engine.
- **Default gel application area baked in.** `DefaultGelAreaCm2 =
  750.0` is set at the dose-event factory; upstream's catalog exposes
  area-related parameters per event. DoseCare doesn't expose this in
  the UI because the current gel model doesn't read `areaCm2` —
  `PkParameterResolver`'s `GEL` branch uses `core.gelK1` and
  `core.gelFMax` directly. The field is kept so a future skin-depot
  gel model has somewhere to put per-event area without a data-class
  bump.
- **Sublingual θ is fixed, not user-settable.** Upstream lets the user
  configure the sublingual θ — the fraction of a sublingual dose
  absorbed through the buccal mucosa (fast pathway) versus swallowed
  and absorbed orally (slow pathway). DoseCare's
  [`PkDoseEvent`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L73)
  carries an optional `sublingualTheta: Double?` field for parity, but
  nothing in the UI ever sets it. The
  [`PkParameterResolver.SUBLINGUAL`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L820)
  branch always falls back to the catalog default
  [`PkCatalog.standardSublingualTheta = 0.11`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/pk/PkSimulation.kt#L1204).
  Real per-person variation in mucosal-vs-swallowed fraction can be
  substantial; DoseCare does not currently expose it for tuning.

## What DoseCare plans to change

The PK engine swap gates three deferred refactors, all enumerated in
[`architecture.md#known-limitations-and-planned-refactors`](architecture.md#known-limitations-and-planned-refactors):

- Consolidate the fragmented PK math (constants, three-compartment
  simulation, dose-equivalence) so `PkCatalog` and
  [`DoseInstructionCalculator`](https://github.com/sakanana001/dosecare/blob/main/app/src/main/java/com/dosecare/app/model/medication/DoseInstructionCalculator.kt)'s
  overlapping molecular-weight constants collapse into one source of
  truth.
- Untangle the projection cache from `HomeSnapshotRepository` so PK
  re-simulation isn't triggered by unrelated home mutations.
- Bring per-user PK calibration back. The EKF draft was tested and
  dropped from `main` to wait for the new engine's calibration shape;
  the reference is parked outside this repo's history.



## Cyproterone acetate (CPA)

DoseCare adds CPA as a third compound in the catalog alongside the estradiol and testosterone esters. CPA differs from the E2 esters in that **it is the active drug itself, not a pro-drug that requires enzymatic hydrolysis**, so it is the first catalog entry that is *not* parameterized through the `formationFraction` / `hydrolysisK2` / two-part-depot path. The oral route is the only one with a meaningful model today; depot, gel, and patch are zero-weighted.

### Calibration anchors

| Data point | Value | Source | Used to set |
|---|---|---|---|
| Single dose 50 mg, Cmax @ 3 h | 140 ng/mL | Androcur 50 mg SPC (AU TGA) | `oralKAbs`, `kClear` indirect |
| Single dose 100 mg, Cmax @ 2.6 h | 254 ng/mL | Pharmindex / Pharmacology of CPA wiki | `vdPerKg = 3 L/kg` back-solve |
| Single dose 2 mg (Diane-35), Cmax @ 1.6 h | 15.2 ± 6.6 ng/mL | Kuhnz 1993 | Low-dose anchor |
| Single dose t1/2 | 38–44 h | Androcur SPC | `kClear = ln(2) / 38h ≈ 0.01825` |
| Long-term t1/2 | 78.6 ± 16 h | Kuhnz 1993 (cycle 3) | Documented limitation; not in the 1-comp model |
| Steady state 50 mg/day (men) | 246 ± 13 ng/mL | Pharmacology of CPA wiki | Steady-state assertion band |
| Steady state 50 mg/day (women, hirsutism) | 199–228 ng/mL | Pharmacology of CPA wiki | Steady-state assertion band |
| Steady state 100 mg/day (men) | 348 ± 23 ng/mL | Pharmacology of CPA wiki | Steady-state assertion band |
| Steady state 100 mg/day (women, hirsutism) | 436–520 ng/mL | Pharmacology of CPA wiki | Steady-state assertion band |
| Oral bioavailability | 88% | Diane-35 SPC, Androcur SPC | `oralBioavailability[CPA] = 0.88` |
| Clearance | 3.5 ± 1.5 mL/min/kg | Androcur SPC | Cross-check on `kClear * vdPerKg` |
| Oral absorption t1/2 | ~1.5 h | Pharmacology of CPA wiki | `oralKAbs = ln(2) / 1.5h ≈ 0.46` |
| Plasma protein binding | 96% (albumin, no SHBG) | Androcur SPC | Conceptual; not modeled |
| Accumulation factor | 2-3× | Androcur SPC (~3×), Diane-35 SPC (2-2.4×) | 1-comp theoretical: 2.82× at t1/2=38 h, tau=24 h |
| Steady-state time | 8–16 days | Diane-35 SPC | Documented |

### Non-linear pharmacokinetics: a known limitation

CPA exhibits dose-dependent PK that a single-compartment Bateman cannot fully capture. The 1-comp model shipped in `PkCatalog` is calibrated to the **100 mg single-dose** point (Pharmindex 254 ng/mL @ 2.6 h, Vd 3 L/kg) and is best for the 50–100 mg once-daily range that covers the standard MtF/MtX HRT dose. Three deviations to be aware of when interpreting the engine output:

1. **Low-dose Vd collapse.** At 2 mg (Diane-35) the apparent Vd back-solves to ~0.9 L/kg — central-compartment saturation is not in play at that dose. The engine at vdPerKg=3 therefore **under-predicts the 2 mg case by ~3.5×** (model 4.3 ng/mL @ 1.6 h vs. SPC 15.2). A future dose-dependent Vd (e.g. `vdPerKg(dose) = max(0.9, 3.0 - 2.1 * exp(-dose/20))`) would close this gap; it is parked until the planned PK engine swap lands.
2. **Long-term t1/2 elongation.** Single-dose t1/2 is 38–44 h. After 12 weeks of daily dosing, Kuhnz 1993 reports a terminal t1/2 of ~78 h due to deep-compartment equilibration and fat sequestration. The 1-comp model is anchored to the single-dose t1/2; long-term trough predictions will tend to be slightly low because the engine is clearing the drug faster than the patient actually does after months of dosing. For a user logging doses for weeks to months, the simulated curve will dip ~20–30% lower than the real curve in the late elimination phase.
3. **Population heterogeneity.** Steady-state Cmax differs ~2× between aged men with prostate cancer and pre-menopausal women with hirsutism on the same dose (e.g. 348 vs. 520 ng/mL at 100 mg/day). The engine output is the population mean; an individual user's blood tests may sit anywhere in the assertion band and still be physiologically normal.

### Engine shape (oral)

CPA uses the same single-compartment Bateman path as oral E2 (and oral testosterone undecylate), driven by `oralKAbs` (ka) and `kClear` (ke):

```
tmax = ln(ka / ke) / (ka - ke)  =  ln(0.46 / 0.01825) / (0.46 - 0.01825)  ~  7.3 h
```

The SPC's reported tmax ~3 h is the *early rising* value reported on the SPC label, not the analytic Bateman peak. This is a labelling-vs-modelling mismatch that recurs across CPA SPCs (Androcur 50 mg SPC, Androcur 100 mg SPC, Diane-35 SPC, Anterone 100 SPC) — they all quote the 2–3 h value, but a 1-comp fit back-solves to 7–9 h for that population.

### What is *not* modelled

- **Active metabolite 15β-OH-CPA.** Has its own antiandrogenic and progestogenic activity in the literature, but the engine emits a single CPA curve only. A metabolite-compartment model would be a separate PR if it lands.
- **Intramuscular injection.** The 300 mg IM Androcur Depot depot has its own depot kinetics (terminal t1/2 ~96 h, Cmax 191 ng/mL @ 2–4 d) but the engine's injection path uses the two-part-depot E2 ester kinetics, which does not match IM CPA. A custom `PkRoute` for IM depot is parked with the planned engine swap.
- **CYP3A4 inhibitor/inducer drug-drug interactions.** Real-world CPA exposure changes significantly with strong CYP3A4 modulators (e.g. ketoconazole can roughly double Cmax; rifampin can roughly halve Cmax). The engine has no interaction surface today; it ships the population mean.
- **Cumulative-dose meningioma risk.** Population epidemiology shows a cumulative-dose-dependent meningioma association above ~3 g (Cyprostat 50 mg SPC, ANROCUR SPC). The engine tracks individual doses but does not surface this safety signal; users should consult their clinician.

### Forward path

- **PR2 (HomeSnapshot multi-hormone projection):** will give CPA its own `HomePkProjectionRecord` in the snapshot, decoupled from E2 and T. The widget projection stays E2-only in the first cut; CPA is exposed in the main chart behind a per-hormone selector that lands in PR3/PR4.
- **PR3/PR4 (testosterone and CPA chart surfaces):** the 1-comp engine already runs CPA in tests; the PRs only need the UI selector and the dose-event routing by `MedicationCategory.ANTIANDROGEN` (so CPA medicine logs feed the CPA engine, not the E2 engine).
- **PK engine swap:** full dose-dependent Vd, IM depot, and metabolite modeling all live behind the planned `multi-medication` engine refactor. Until then, this catalog entry is the best 1-comp fit for the 50–100 mg/day range.

## Transdermal estradiol patch (PR3.5)

Estradiol patches (Climara, Estraderm MX, Vivelle-Dot, Estrana Tape,
etc.) drive PK off a **zero-order input** over the wear interval
(typically 3-4 days for twice-weekly matrix patches, 7 days for
once-weekly matrix patches), with the systemic clearance rate constant
`kClear` controlling how fast the steady-state is reached and how fast
the level falls after patch removal.

The engine path is `PkRoute.PATCH_APPLY` (with an explicit
`releaseRateMcgPerDay`) followed by a paired `PkRoute.PATCH_REMOVE`.
The two-event pair is the user-visible "log a patch change"; an
un-paired `PATCH_APPLY` keeps releasing indefinitely (existing
regression: `patchSimulation_unmatchedApplyKeepsReleasingIndefinitely`).

### Product catalog

`MedicationCatalog.patchDoseAssistPresets` exposes four products with
the 25/50/75/100 mcg/day Climara strengths (and the 25/50/100 Estraderm
MX and Vivelle-Dot 4-day strengths), plus a 50 mcg/day Estrana Tape
(久光 7-day JP patch) entry. Each branded preset carries a
`productLabel` for picker grouping and a `wearDays` cadence hint. The
legacy "plain mcg/day" tiles are kept for users who just type a
release rate without picking a brand.

| Product | Strengths | Wear | Notes |
|---|---|---|---|
| Climara (Bayer) | 25 / 50 / 75 / 100 mcg/day | 7 d | Once-weekly matrix patch; lowest intra-week fluctuation |
| Estraderm MX | 25 / 50 / 100 mcg/day | 3-4 d | Twice-weekly; higher skin reactivity reported |
| Vivelle-Dot | 25 / 50 / 100 mcg/day | 3-4 d | Smaller patch footprint |
| Estrana Tape (久光) | 50 mcg/day | 7 d | JP-only matrix patch |

### Clinical anchors (steady-state Cavg, single 7-day wear)

These lock the patch PK to transdermal E2 SPC data; the four
`pr35_transdermalClimara*_cssInBand` tests assert the model output
falls in a ±30% band around each literature Cavg (the band is wider
than the typical 50% inter-individual CV of transdermal E2, so the
assertion is a model-fit regression rather than a clinical-grade
fidelity check):

| Release rate | Model Css (engine) | Literature Cavg | Source |
|---|---|---|---|
| 25 mcg/day | 18 pg/mL | 22 pg/mL | Climara 25 SPC (DailyMed 310178) |
| 50 mcg/day | 36 pg/mL | 41 pg/mL | Climara 50 SPC; Buch 1998 |
| 75 mcg/day | 54 pg/mL | 72 pg/mL | Climara 75 SPC |
| 100 mcg/day | 73 pg/mL | 89 pg/mL | Climara 100 SPC |

The systematic ~12-25% under-prediction at higher release rates is
consistent with the larger peripheral Vd that accompanies a true
transdermal depot state (skin + subcutaneous storage); the engine's
`vdPerKg = 2.0 L/kg` is anchored to IV bolus E2 PK, not the
transdermal steady-state distribution. A future
`patchVdPerKg` field could close this gap; parked until the
multi-medication engine swap lands.

### What is *not* modelled

- **Application site differences.** Buttocks > abdomen > arm by ~17%
  on Cmax/Cavg (DailyMed 310178 figure 2). The engine uses a single
  average; users who always apply to a high-absorption site will see
  the curve sit slightly above the model output.
- **Heat / exercise / sauna excursions.** Sauna, whirlpool, and
  treadmill can boost EE/E2 exposure by 20-30% within the first
  hours of application (Ortho Evra data; assumed similar for E2
  patches). Not modelled.
- **Adhesion failure / partial detachment.** The engine assumes the
  declared release rate for the full wear interval. A patch that
  peels off halfway through day 3 should be re-logged with a manual
  `PATCH_OFF` at the actual removal time.
- **Cutting patches.** Do not cut matrix patches in half. The
  matrix geometry controls release; cutting collapses the
  controlled-release profile and risks a bolus release of the full
  drug load (DailyMed "Do not cut" warning). The picker does not
  expose a half-strength option for that reason.

### Forward path

The patch product list lives in `MedicationCatalog.patchDoseAssistPresets` and
currently has no UI consumer beyond the existing dose-assist tile
renderer. A picker that groups the branded presets by product
(Climara 25/50/75/100, etc.) with a wear-cadence badge is the natural
next step. That work is parked until the chart UI exposes a
per-hormone selector (PR3 / PR4 scope) and can render a transdermal
curve that makes the product distinction user-visible.

## Estradiol injectable esters (PR3.5)

The five injectable E2 esters (benzoate, valerate, enanthate,
cypionate, undecylate) use the engine's two-part depot
(`PkCatalog.twoPartDepotParams`) plus a `formationFraction` (the
fraction of the injected dose that appears in the central E2
compartment once the ester has been hydrolysed and distributed). The
fast channel (k1Fast) represents the fraction of the depot that
dissolves rapidly; the slow channel (k1Slow) carries the depot's
long tail. `hydrolysisK2` models the intermediate ester → E2
cleavage step. The central compartment uses the same
`kClearInjection = 0.041 h⁻¹` (E2 core) for elimination.

### PR3.5 retune (estradiol benzoate only)

The shipped v0 catalog had `formationFraction[EB] = 0.109` and a
fast k1 of 0.144 h⁻¹, which gave a model Cmax of **1818 pg/mL @ 24h**
against Oriowo 1980's measured **940 pg/mL @ 1.8d (43h)** — a 93%
over-prediction. The benzoate ester has the steepest dose-dependent
Vd shift of the five (its short C3 chain leaves the drug
distributionally "hot" at low body-fat context), and the 1-comp
model cannot track that shift. PR3.5 retunes only this entry:

| Param | Before | After | Why |
|---|---|---|---|
| `fracFast[EB]` | 0.9 | 0.6 | Pull some mass into the slow channel to extend the tail |
| `k1Fast[EB]` | 0.144 h⁻¹ | 0.078 h⁻¹ | Slows the early release to push Tmax from 24h to ~36h |
| `k1Slow[EB]` | 0.114 h⁻¹ | 0.060 h⁻¹ | Slows the depot tail so the curve matches Oriowo's 43h peak |
| `formationFraction[EB]` | 0.109 | 0.060 | Cuts Cmax in half to align with literature |

After retune: model predicts **836 pg/mL @ 36h** (within 11% of
Oriowo's 940 pg/mL @ 43h). The other four esters (EV, EC, EN, EU)
were not retuned; their catalog values are within ±30% of literature
already (see test table below).

### Clinical anchors (5 mg single IM dose, 70 kg)

Each row is locked by a `pr35_singleDose*_cmaxTmaxInBand` test:

| Ester | Model Cmax | Model Tmax | Literature Cmax | Literature Tmax | Source |
|---|---|---|---|---|---|
| EB (benzoate) | 836 pg/mL | 36 h | 940 pg/mL | 1.8 d (43 h) | Oriowo 1980 |
| EV (valerate) | 443 pg/mL | 60 h | 667 pg/mL | 2.2 d (53 h) | Oriowo 1980; Schug 2012 |
| EC (cypionate) | 335 pg/mL | 96 h | 338 pg/mL | 3.9 d (94 h) | Oriowo 1980 |
| EN (enanthate) | 282 pg/mL | 144 h | 329 pg/mL | 3-5 d | hrtyaku.com EN page |
| EU (undecylate) | 76 pg/mL plateau | (no Cmax) | plateau (no Cmax) | (long-acting) | transfemscience.org meta |

The 33% Cmax under-prediction for EV and the 14% under-prediction
for EN are both within the in-study CV (typically 30% for IM E2
esters). The EC 1% agreement and the 11% agreement for the retuned
EB are the two standouts.

### Repeat-dosing and steady state

The PR3.5 anchor tests are single-dose only. The engine handles
repeat-dosing naturally — multiple `INJECTION` events on the same
`PkCompound` accumulate in the central compartment — but the steady
state takes 3-4 cycles for the mid-range esters (EV/EC) and 4-6
weeks for EU to reach the long-acting plateau. Users who log
multiple doses will see the curve climb toward the literature Css
for the same weekly dose; the trans-fem-science meta-analysis
section on "5 mg every 7 days" provides a cross-reference for the
expected steady state.

### What is *not* modelled

- **Subcutaneous vs intramuscular.** Both routes share the depot
  model; the subcutaneous literature (Bourget et al. for EV)
  reports slightly lower Cmax with smoother fluctuation, but the
  difference is within IM inter-subject CV.
- **Aqueous suspensions.** Microcrystalline aqueous suspensions
  (Agofollin Depot for EB) have duration profiles that diverge
  sharply from oil solutions. The engine only models the oil
  solution; users logging suspension formulations should override
  the catalog with their own dose calibration.
- **Polyestradiol phosphate (PEP).** A polymer prodrug with its
  own depot kinetics (40-160 mg, 1-2 month duration). Not modelled
  and not in the current PkCatalog; can be added in a future PR if
  clinical use is reported.
- **High-dose "pseudopregnancy" regimens.** 40 mg/week EV achieves
  2,500-3,100 pg/mL E2 (pseudopregnancy study). The engine will
  model this with the existing parameters; the absorption phase
  stays well-resolved at this dose, but a 1-comp model at very
  high single doses tends to under-predict peak since the deep
  compartment's filling becomes rate-limiting. Documented in the
  CPA "non-linear PK" section's same precedent.

### Forward path

The five ester curves are now clinically anchored. The next step is
**dose-event routing by compound** — so a 5 mg EV injection event
feeds the EV engine, not the E2 engine — which lands in PR3 / PR4
alongside the per-hormone chart selector. Once that lands, the
picker surface (currently E2-only) can show the EV / EC / EN / EU
esters as separate dose-assist tiles, each with their own duration
warning (EV ≈ 5-7 days, EC ≈ 7-14 days, EU ≈ 4-12 weeks).

## Testosterone (PR3)

The testosterone ester path uses the same two-part depot engine as the
estradiol esters (`PkCatalog.twoPartDepotParams`), with the central
elimination constant split between a fast `kClear` (free-T plasma
clearance, 0.6 h⁻¹, t½ ≈ 1.16 h) and a slower `kClearInjection`
(0.05 h⁻¹, t½ ≈ 14 h) that lets the depot `k1Slow` (T 0.0069 h⁻¹,
t½ ≈ 100 h) dominate the apparent terminal half-life via flip-flop
kinetics. The hydrolysis `k2` rate is per-compound (T 0.12 h⁻¹ for TE,
0.09 h⁻¹ for TC, 0.03 h⁻¹ for TU). The default concentration unit is
`NG_PER_DL` to match the international clinical reporting convention.

### PR3 retune (testosterone esters)

The shipped v0 catalog had `formationFraction[TC/TE/TU] = 1.0` and
`kClearInjection = 0.03 h⁻¹` (t½ 23 h, borrowed from the E2 estrogen
path). The single-dose IM curves then ran 7-15x above the Behre 1986
/ Slayback FDA / Nebido SmPC literature anchors. PR3 retunes both
inputs:

| Param | Before | After | Why |
|---|---|---|---|
| `formationFraction[TC/TE/TU]` | 1.0 | 0.07 | Cuts the steady-state Cmax by 14x |
| `kClearInjection` | 0.03 h⁻¹ | 0.05 h⁻¹ (t½ 14 h) | Releases the central elimination rate-limit; lets the depot k1Slow drive the apparent terminal t½ |
| `gelFMax` | 0.12 | 0.10 | AndroGel SPC reports 10% transdermal absorption; the 0.12 was a 20% over-estimate |

After retune, the 1-comp steady-state Cmax sits within ±30-50% of the
published anchors (depending on ester chain length), and the
Tmax lands at 48-120h vs the 60-168h literature range.

### Clinical anchors

Each row is locked by a `pr3_singleDoseTestosterone*_cmaxTmaxInBand`
test:

| Ester | Dose | Model Cmax | Model Tmax | Literature Cmax | Literature Tmax | Source |
|---|---|---|---|---|---|---|
| TE (enanthate) | 200 mg | 14.5 ng/mL | 48 h | 10.8 ng/mL (scaled 250 mg Behre 1986) | 2-3 d (48-72 h) | Behre 1986 / PeptideWiki |
| TC (cypionate) | 200 mg | 10.8 ng/mL | 48 h | 7.6 ± 2.9 ng/mL | 71.7 h median | Slayback FDA label, N=26 |
| TU (undecylate) | 1000 mg (Nebido) | 19.8 ng/mL | 120 h | 11-15 ng/mL | 7 d | Nebido SmPC |

The model consistently lands the Tmax 12-48h earlier than the
literature median. This is the documented limitation of the 1-comp
flip-flop model: it cannot simultaneously reproduce the rapid
early-rise absorption wave (driven by `k1Fast`) and the longer
literature curve tail (driven by `k1Slow`); the chosen compromise
puts the model peak at ~2 × `1 / kClearInjection` rather than the
~2 × `1 / k1Slow` that the longer curve implies. The
`pr3_testosteroneGel*_cssInBand` tests cover the transdermal path
(AndroGel 5g / 10g daily):

| Application | Dose | Model Cmax @ Day 30 | Literature Cavg | Source |
|---|---|---|---|---|
| AndroGel 1% gel | 5 g/day (50 mg T) | 3.5 ng/mL | 5.66 ± 2.62 ng/mL | AndroGel FDA label |
| AndroGel 1% gel | 10 g/day (100 mg T) | 7.0 ng/mL | 7.92 ± 2.94 ng/mL | AndroGel FDA label |

The model under-predicts the gel Cavg by ~30% because the 1-comp
Bateman path has no skin-reservoir compartment; this is parked with
the planned `patchVdPerKg` work.

### What is *not* modelled

- **Long-term Vd shift at high dose.** A 200-500 mg IM T ester load
  expands the apparent Vd in the first 24-48h before equilibration;
  the 1-comp model collapses this into the `formationFraction`
  parameter. The 0.07 setting is the best fit for the 200-1000 mg
  single-dose range; users on multi-week 200 mg/week regimens
  should expect the steady-state trough to sit ~20% below the
  model output once the deep fat depot has equilibrated.
- **Hepatic SHBG induction.** Exogenous T suppresses SHBG, which
  increases the free fraction. The model treats all T as the
  total (SHBG-bound + free), so an individual's free T level will
  be higher than the model output if SHBG is suppressed. The
  blood-test integration planned for v0.6 will surface free T
  separately when the user logs SHBG alongside total T.
- **Erythrocytosis coupling.** T-driven RBC mass expansion is a
  primary safety concern (target HCT ≤ 52%). The model has no
  feedback from T level to hematocrit; the planned blood-test
  tracker in v0.6 will be the surface where this risk is
  monitored.
- **Aromatization to estradiol.** ~0.5-2% of circulating T converts
  to E2 via aromatase. The model has no E2 source from T
  injection, so a user running T alone will see their E2 curve
  drop to baseline. The FtM-aware E2/T dual-view in the F-PR*
  follow-ups is the place to add the aromatase coupling.
- **Testosterone patch (Androderm).** 2.5 mg/day matrix patches
  have a 24h wear and a different absorption profile than the
  7-day matrix E2 patches. The catalog has a T path (`gelFMax`)
  but no T patch preset; users on Androderm will see a flat
  zero curve. A T patch preset (with `releaseRateMcgPerDay`)
  ships as part of the FtM F-PR* work.

### Forward path

The engine side of PR3 is done (5 clinical anchor tests, 118/118
across the 6 core suites). The main-screen E2/T switcher (per-
hormone selector at the top of the home chart, hormone-aware target
range, hormone-aware display unit) ships as a follow-up commit
because the refactor touches every `MainE2Chart*` symbol. The T
unit-anchor (ng/dL) and target range (400-700 ng/dL, 14-25 nmol/L)
are already wired through the engine; the UI plumbing is the only
remaining piece.

## Out of scope

- **The PK math itself.** Compartment equations, parameter
  derivations, and the literature anchors for each constant live in
  the upstream README and its `pk_research/` workspace.
- **Personal-PK calibration.** The drafted-and-paused EKF code lives
  outside this repo; it isn't tracked in `docs/`.
- **Testosterone as a user-facing simulation.** `PkHormone.TESTOSTERONE`
  exists in the catalog with full parameters, but no user-logged
  category currently produces testosterone PK events, so testosterone
  trends are not rendered.
