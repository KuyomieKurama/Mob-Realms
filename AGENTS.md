# Mob Realms agent guide

This is a Minecraft 26.3 Fabric mod with a dependency-free simulation core. `sim-core/` owns persistent civilization state and rules; `fabric-mod/` connects those rules to Minecraft entities, world blocks, commands, and the client UI.

- Use Java 25. Run `./gradlew test build` after Java changes. `python3 scripts/check_resources.py` checks resource files.
- Classify every user-facing change before delivery using [the versioning rules](docs/versioning.md): **mini/patch** for compatible fixes and polish, **minor** for compatible new gameplay or APIs, **major** for incompatible behavior, save data, or required migration. Bump `gradle.properties` accordingly and keep the installer, upgrade allowlist, changelog, and current-version documentation aligned. Retain the `-dev` suffix until a stable release is explicitly requested.
- Treat the world in `../mob-realms-server` as live user data. Use the provided install/upgrade scripts and a full backup before replacing its mod JAR. Do not edit world files directly.
- For gameplay stalls, inspect `../mob-realms-server/logs/latest.log`. Mob Realms emits one bounded diagnostic block per 1200 ticks, including work times, resident goals, positions, and project progress.
- Preserve resource conservation: cargo reaches a stockpile only through an explicit delivery, and construction consumes recorded materials.
- For parallel agent work, use separate Git worktrees. Keep findings in `.agent-team/runs/` and hand off the exact evidence, changed files, validation, and remaining uncertainty.
- Do not commit, merge, publish, or restart the live server unless the current user task calls for it.

The shared agent roles and CLI workflow are in `docs/agent-team.md`.
