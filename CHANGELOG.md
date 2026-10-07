# Changelog

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
