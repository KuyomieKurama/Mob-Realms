# Changelog

## 0.5.0-dev — M2–M4 development candidate

- Admin-only paginated simulation log with latest-first ordering, settlement filter and persisted 256-entry history. Rate-limited bottleneck events aid debugging.
- Configurable settlement construction target defaults to 96 residents instead of the hard-coded 48; existing worlds can continue growth.

- Confirmed deaths immediately and idempotently remove residents, free housing and reset growth progress; chunk unloading does not count as death.
- Pending births survive restart and wait for a collision-free spawn position; tests cover recovery, extinction and population caps.
- Dedicated Admin tab with 1/7/30/365-day controls, queue cancellation, time-lapse, camera exits and protection. Automatic five-second refresh, compact tab navigation, disabled invalid page/queue buttons and population/growth diagnostics.

- Follow-up: workers select missing materials across the remaining build order, sweep terrain-height-aware resource columns, abandon stalled targets and approach accessible neighboring positions. Idle workers no longer display stale patrol goals; unsuccessful surveys remain productive tasks rather than patrols.

- Persistent settlement objectives, material-paid construction, food/housing growth, processing and equipment.
- Eight species profiles, custom skinned human settlers, dimension-aware natural founding and expanding resource commons.
- Player founding banner, claims, reputation, recruitment, roles, treaty consent, barter and basic combat.
- Realm atlas with overview, territory map, diplomacy, residents and research; German/English localization.
- Daily bounded bandit strategy, five technologies, veterans and fatal-arrow adaptation.
- Save format 3 with legacy migration; installer/upgrade target updated, accepting 0.1/0.2 sources.
- Known limits and gameplay acceptance instructions documented; this is not a stable or interactively validated release.


## 0.2.0-dev – admin and simulation expansion

- `/civ admin`: native dialog with paginated camp snapshots, day input, cancellation and chunk protection; all actions retain player permissions.
- `/civ simulate 1..365` and `/civ simulate cancel`; persisted incremental day cursor, at most 32 citizens per daily task.
- Save format 2 with format-1 migration and regression tests.
- Natural camp searches every 30 seconds after the grace period, configurable bounded attempts and conservative ground filtering; simulated days can trigger searches.
- Night patrols for idle residents; visible server-wide 1–5× tick-rate controls and explicit spectator/creative/survival commands.
- Stopped-server upgrade with complete backup, runtime/world locks, failure rollback and an interrupted-upgrade marker; 21 installer/upgrade tests.
- DE/EN player guides and GUI translations updated. City growth remains planned for M2.

## 0.1.0-dev — M1 development candidate (unreleased)

### Added

- Linux server installer, verified Fabric downloads, pre-start dependency/integrity checks, EULA gate and repeat-install protection.
- Automatic user-local Eclipse Temurin JDK 25 installation when Java/JDK 25 is unavailable; SHA-256 verification and bounded safe archive extraction.
- Seventeen installer/Java failure and success scenarios plus a real installation smoke test in CI.

- Java 25 / Minecraft 26.3 multi-project Fabric build with official example wrapper.
- Independent simulation core: camps, stable citizen IDs, utility scores, cargo, stocks and protected chunks.
- Versioned checksummed saves, previous-save backup and stale-lease protection across simulation handoffs.
- Bounded work scheduler and capped daily simulation.
- Zombie/skeleton datapack profiles, starter camps and material collection in the Overworld.
- `/civ info`, `/civ relations`, `/civ found`, `/civ protect`, `/civ unprotect`, `/civ simulate` and `/civ goals`.
- German/English game text and player instructions, config reference and gameplay checklist.
- Ten dependency-free core scenarios, translation checks and Java 25 GitHub Actions workflow.

### Verified

- Local core compilation and ten scenarios passed on Java 17.
- Full Java 25 Gradle build and ten core scenarios passed on GitHub Actions.
- JSON syntax and parity of 22 German/English translation keys passed locally.

### Limits

- Client/dedicated-server world gameplay acceptance is outstanding; this is not a stable release.
- Background simulation only delivers existing cargo. Offline progression, growth, diplomacy, learning and human NPCs are not implemented.
- Stocks are internal, citizen combat is disabled, and starter shelters have no repair or resource-cost system.
- No graphical goal overlay yet; `/civ goals` supplies textual debugging.
- Mod saves and vanilla chunk/entity saves are not a shared atomic transaction.
