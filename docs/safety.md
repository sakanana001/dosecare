# Safety and Disclaimer

This document is the canonical statement of what DoseCare is, what it is not, and how to read its pharmacokinetic projection, its plasma-level estimate, and its interaction rules. The short banner in the README links here. If anything in the README and this document conflict, this document is the source of truth.

## Not medical advice

DoseCare is a tracking tool. It is not a medical device, and installing or using it does not create a clinician–patient relationship between you and the app's developers, contributors, or distributors.

The app does not diagnose any condition. It does not prescribe, recommend, or contraindicate any medication or dose. It does not treat any condition. Decisions about your hormone therapy — whether to start, change, pause, or stop a regimen — are between you and a clinician you trust. The same applies to the pharmacology screens: an interaction warning is a prompt to ask a pharmacist, not a substitute for asking one.

## Not a medical device

DoseCare is not approved, registered, cleared, or certified as a medical device under any regulatory regime — including the U.S. FDA, the UK MHRA, the EU MDR (CE marking), Japan's PMDA, China's NMPA, or any equivalent authority elsewhere.

It is not subject to medical-device quality controls, clinical validation, or post-market surveillance. If your jurisdiction regulates health-related apps, DoseCare should be treated as a personal log — equivalent in regulatory status to a paper notebook with arithmetic in it — and nothing more.

## How to read the pharmacokinetic projection

The estradiol curve DoseCare shows is a **model estimate**, not a measurement.

The model uses population-average parameters: absorption rates, distribution volumes, elimination half-lives, and metabolic constants drawn from published studies. Your body's actual absorption, distribution, metabolism, and clearance may differ substantially from those averages — sometimes by a factor of two or more in either direction.

Known limitations of the v1 model:

- **Estradiol-only projection.** The simulated curve covers estradiol only. The app *records* blood tests for testosterone, progesterone, prolactin, FSH, LH, and user-defined custom analytes — and tracks the medications that affect them — but it does not simulate or project levels for anything other than estradiol. Use the projection as an E2 reference; read every other analyte from your actual lab results.
- **Three-compartment approximation.** Real human pharmacokinetics involves many more compartments and pathways. The three-compartment model is a tractable simplification, not a faithful biological simulation.
- **No personal calibration.** The model is not tuned to your own lab results. There is no Bayesian update from your blood tests back into the model parameters in v1.

Things you should **not** use the projection for:

- Deciding a dose
- Changing a regimen
- Interpreting a symptom
- Timing labs around an expected peak — the model's peak timing may not match yours

Blood tests, drawn at the appropriate time relative to your dosing and interpreted by a clinician, remain the source of truth for what your levels actually are.

## How to read the pharmacology features

The pharmacology half of DoseCare is a bundled reference tool. It is not clinical
decision support, and it was not built or reviewed by a pharmacist or a
physician.

### Where the data comes from

The 202-drug catalog ships inside the app as a static asset
(`app/src/main/assets/pharmacology/catalog.json`). It is compiled from
published reference material — DrugBank, OpenFDA / DailyMed, the AGNP 2017
consensus, the Flockhart CYP Drug Interaction Table (Indiana University),
therapeutic drug monitoring guidelines, and Chinese drug package inserts. The
full attribution list is in [third-party-notices.md](third-party-notices.md).

What that means in practice:

- The catalog is a **compilation, not a clinical validation**. Entries were
  assembled from published sources, and any field that could influence a
  dosing decision has to be read against the source it came from. Clinical
  review is required before any release that widens distribution beyond
  personal use.
- The catalog is **frozen at the version in the APK**. If a label, a
  consensus guideline, or a CYP table changes after the build, the app
  does not know. There is no update channel, and no network call fetches a
  fresher copy.
- A drug that is **not in the catalog is invisible** to every pharmacology
  screen. If the checker says nothing about a medication you take, that
  usually means the app does not have that medication in its catalog — not
  that the combination is safe.

### The interaction checker

The interaction engine is a set of static rules over the catalog, evaluated
locally on your device. It covers CYP450 inhibition and induction, additive
QTc prolongation, 5-HT syndrome risk, anticholinergic load, therapeutic-window
deviation, renal and hepatic dose adjustment, and four additive
pharmacodynamic rules (sedation synergy, bleeding risk, dopamine antagonism,
hypotension).

Limitations you have to keep in mind:

- The rules are **population-level heuristics** with documented
  false-positive and false-negative behaviour. They can flag a harmless
  combination and can miss a dangerous one. Absence of a warning is not
  evidence of safety.
- The engine **does not consult a live pharmacy database**, a prescribing
  service, or a maintained interaction API. It only knows what is in the
  bundled catalog.
- The exposure folds are scaled by the **dose and frequency it is given**.
  For medicines linked to your own regimen those come from your plan; for
  drugs you picked by hand they fall back to the catalog's midpoint dose at
  twice daily. A wrong or stale dose produces a wrong severity grade.
- The severity grades (`LOW` / `MEDIUM` / `HIGH` / `CONTRAINDICATED`) are
  the app's own labels. They are not a recognised clinical grading system,
  and they do not map one-to-one onto any guideline's categories.
- The engine does not know about your kidney or liver function, your
  genotype, your other conditions, your age, or what you ate. The patient
  profile it evaluates is the one the app holds, not your medical history.

### The plasma-level estimate

The TDM screen runs a **one-compartment oral PK model** from the catalog's
parameters, using the dose, frequency, and body weight you enter, and
compares the resulting curve against the drug's published therapeutic window.

This is the roughest estimate in the app, and the gap between it and
reality is large:

- A one-compartment model cannot represent accumulation, distribution
  phases, or active metabolites.
- Real concentrations depend on your CYP status, liver and kidney
  function, food, adherence, formulation, and interactions — none of
  which the model can see.
- The window it compares against is a population window for a defined
  population, often from a specific guideline, applied to you
  unconditionally.
- The estimate is most useful as a **relative** signal ("this dose looks
  low, this one looks high"), and least useful as an absolute number to
  act on.

Never start, stop, or change a dose because of a number on this screen.

## What DoseCare does NOT do

- It does not recommend doses.
- It does not interpret lab results.
- It does not alert you to concerning trends or anomalies. (The reference-range view does flag entered lab values as below / in / above range, but it does not diagnose anything or recommend actions from those classifications.)
- It does not detect side effects.
- It does not check your medications against a live pharmacy database. Its interaction check runs static rules over the bundled catalog, so it cannot see drugs outside that catalog.
- It does not validate a regimen for you, and it does not know your medical history, your organ function, or your other conditions.
- It does not replace any clinical visit, lab draw, or in-person care.

If a feature looks like it might do one of these things, it doesn't — read the feature's documentation for what it actually does.

## Self-care and clinical care

Decisions about your therapy are yours.

DoseCare is useful as a log and a projector even when you do not have regular clinician or lab access. That is a real situation many people on HRT live with — through gatekeeping, cost, geography, or systemic discrimination — and this app does not pretend otherwise. Tracking what you take and modeling what you'd expect is valuable on its own.

That said, a clinician's interpretation remains the only reliable way to:

- Confirm a model projection against reality (via a blood draw)
- Diagnose a side effect or a concurrent condition
- Adjust a regimen safely when something isn't working
- Manage interactions between hormone therapy and other medications
- Confirm that a plasma-level estimate or an interaction warning matches your situation

A pharmacist is the right person to take an interaction list to. A physician is the right person to take a dose question to.

In an emergency, contact your local emergency services. The app, its maintainers, and the people who built it are not on-call resources and cannot help in real-time.

## No warranty

DoseCare is released under the GNU Affero General Public License, version 3. Sections 15 and 16 of AGPL-3.0 disclaim all warranty and limit liability, and those disclaimers apply in full.

The maintainer is not liable for any decision made with the app's help, for any divergence between the model and reality, for any catalog entry that is wrong or out of date, or for any harm — direct or indirect — arising from use or inability to use the app.
