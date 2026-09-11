# PicoHRT docs

Developer documentation. For the project overview, screenshots, and
download links, see the [repo README](../README.md).

> PicoHRT is a fork of [mkx173/Featherline](https://github.com/mkx173/Featherline), maintained at [sakanana001/PicoHRT](https://github.com/sakanana001/PicoHRT).
> See [PROJECT.md](PROJECT.md) for the full fork rationale, the
> v0.6.0 → current PK engine evolution, and the per-user override
> layer. Chinese mirror: [PROJECT.zh-Hans.md](PROJECT.zh-Hans.md).

## Release notes

- [Changelog](CHANGELOG.md) — what changed in each release,
  organised by feature area (PK model, home screen, statistics
  report, tests, documentation). Follows the
  [Keep a Changelog](https://keepachangelog.com/) format.
- [Changelog (中文)](CHANGELOG.zh-Hans.md) — 中文版，结构与
  英文版一致。

## Project overview

- [Architecture](architecture.md) — layer map, module boundaries, DI,
  Compose navigation, known limitations.
- [Data model](data-model.md) — Room entities, schema relationships,
  encryption, migration policy, per-medicine stock columns.

## Domain deep-dives

- [Reminders](reminders.md) — AlarmManager pipeline, notification
  channels, snooze, exact-alarm permission handling.
- [Widget](widget.md) — Glance app-widget pipeline, snapshot
  persistence, refresh triggers, quick-log action contract.
- [PK differences](pk-differences.md) — upstream PK reference and the
  specific differences in Featherline.
- [Blood tests](blood-tests.md) — analyte catalog, unit-conversion
  factor table, validation pattern.
- [Backup format](backup-format.md) — v3 compressed backup spec,
  restore validation, forward-compatibility policy.

## Operations

- [Building](building.md) — prereqs, gradle commands, flavors.
- [Release process](release-process.md) — versionCode derivation,
  flavor purpose, changelog discipline.
- [Testing](testing.md) — test suite layout and conventions.
- [Localization](localization.md) — adding a new app language,
  resource layout, locale-aware formatters, and validation steps.

## User-facing & legal

- [Privacy](privacy.md) — what is stored, where, what leaves the
  device, permissions used.
- [Safety](safety.md) — medical disclaimer, PK model limits, when to
  consult a clinician.
- [Third-party notices](third-party-notices.md) — dependency, asset, and
  adapted-code license notices.
