---
name: mob-realms-buildings
description: Design, debug, or validate buildable structures and construction progress in the Mob-Realms Fabric mod. Use for blueprint variants, unfinished houses, terrain preparation, roofs, interiors, chests, and material or builder stalls; not for vanilla WorldEdit builds.
---

# Mob Realms Buildings

In a Mob-Realms checkout, read `AGENTS.md` and inspect the existing building JSON under `fabric-mod/src/main/resources/data/mobrealms/mobrealms/buildings/`. Trace the selected blueprint through `Blueprints`, `EconomyController`, `RealmController`, and the persistent project in `sim-core` before changing it.

Keep a project's saved blueprint stable after construction starts. New variants may apply to new projects; changing an in-progress plan needs an explicit migration. A structure is complete only when terrain preparation is done and every planned block, including roof, entrance, interior and chest, has been placed or explicitly accounted for. Construction must consume recorded stock; stranded cargo is not stock until delivered.

For a reported stall, compare `../mob-realms-server/logs/latest.log` diagnostics with project phase, missing materials, builder goal and position. Distinguish pathfinding, blocked terrain, unavailable resources and invalid block placement before adjusting work cadence. Preserve protected chunks and existing player builds.

Validate JSON with `python3 scripts/check_resources.py`, Java changes with `./gradlew test build`, and physical completion on a disposable server or copied world when the behavior depends on Minecraft blocks/entities. Record the exact before/after project progress and remaining uncertainty. Treat `../mob-realms-server` as live data and follow `AGENTS.md` for any upgrade.
