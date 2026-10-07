# Mob Realms – Player guide

[Project overview](../../README.md) · [Deutsch](../de_de/spielanleitung.md)

## Status

M1 is a development candidate, not a stable release. The mechanics below are implemented but still need client and dedicated-server gameplay acceptance. Use a new test world. Diplomacy, growth, learning and human NPCs are future features.

## Requirements and build

Minecraft Java 26.3, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3. Install the normal mod JAR and Fabric API on both client and server for translated messages. Do not install the sources JAR.

For VS Code on Arch Linux, select Java 25 for both the project and Gradle. See [development instructions](../development.md).

```sh
./gradlew test
./gradlew build
./gradlew runClient
```

## First camps

1. Create a new Overworld test world with cheats and Normal difficulty. Peaceful removes hostile vanilla entity types and is unsuitable for this version.
2. Find solid, flat 3×3 ground with four blocks of air above it, entirely inside one chunk. F3+G shows chunk borders.
3. Run `/civ found mobrealms:zombie` to create a starter roof, green banner and three citizens.
4. Move at least 48 blocks away and run `/civ found mobrealms:skeleton` for a blue-banner camp.
5. At night, drop bones or cobblestone inside the camp's chunk, within twelve blocks of its center. Citizens collect suitable drops and return them to camp.
6. Use `/civ info` for stored goods and `/civ goals` for current intentions.

The roof/banner are a one-time starter endowment, not resource-funded construction. Supplies are an internal inventory; there is no lootable chest or withdrawal command yet.

Accepted unmodified vanilla items: rotten flesh, bones, arrows, sticks, cobblestone, oak logs, coal and iron ingots. Modified item components are excluded. Carrying capacity is 16 items per trip. Citizens can collect these materials when dropped by players too.

Zombies favor gathering; skeletons favor regrouping. Sunlight or very low health makes citizens return to their shelter. Destroyed roofs are not repaired. Wild mobs retain vanilla behavior. Citizens do not attack players in M1; combat/diplomacy are not implemented.

## Natural founding

After three world or simulated days, the server schedules up to eight site attempts every 30 seconds, spread across ticks. Completed simulated days also request a search. Overlapping requests coalesce; 365 days do not guarantee 365 camps.

Only loaded Overworld sites 32 to about 136 blocks from players (including spectators) are considered. Sites require a flat, empty 3×3 area over grass blocks, dirt, sand, podzol, mycelium or snow blocks, adequate camp spacing and available population/camp capacity. Protect player land explicitly with `/civ protect`: player-placed natural blocks cannot be distinguished. No chunks are force-loaded. Failed searches retry later.

These are starter civilizations with three new residents, not cities or converted wild mobs. Natural founding remains configurable.

## Commands

All commands require Minecraft's `COMMANDS_GAMEMASTER` permission (normally operator level 2 or cheats).

| Command | Effect |
|---|---|
| `/civ info` | Camps, population, simulated days and stock |
| `/civ goals` | Citizen goals; no graphical overlay yet |
| `/civ relations` | Camp relationships, always neutral (0) in M1 |
| `/civ found mobrealms:zombie` | Found a zombie camp at your position |
| `/civ found mobrealms:skeleton` | Found a skeleton camp at your position |
| `/civ protect` | Protect the current chunk from this mod's gathering/founding |
| `/civ unprotect` | Remove that protection |
| `/civ admin` | Admin GUI: camps, simulation, cancellation and protection |
| `/civ simulate <days>` | Queue 1–365 abstract days |
| `/civ simulate cancel` | Cancel pending days |

Protection does not block vanilla explosions or player actions. Player buildings are not automatically detected. Gathering respects the camp's claimed chunk and protected chunks.

## Admin GUI

Open `/civ admin`. The native dialog shows camps/coordinates in pages of five, stored item totals, population, elapsed and pending days. Enter 1–365 days and press **Start simulation**. **Refresh** updates the snapshot; it is not a live feed. **Cancel pending days** stops remaining work. **Protect current chunk** protects your location.

Every button runs a normal server command as the clicking player; permissions are checked again. Minecraft may ask for confirmation of privileged commands. The GUI does not pause the simulation. There is no additional GUI library; client-side Mob Realms supplies translations.

## Background simulation

Unloaded citizens remain associated with saved Minecraft entities through stable IDs. Existing cargo is delivered on the next abstract day. Empty citizens create no resources. Abstract extraction, population growth and warfare are not implemented yet.

`/civ simulate` does not move loaded entities or change Minecraft clocks. The queue holds up to 365 days and persists progress, including the cursor within a day. Each daily task processes at most 32 citizens. Cancellation preserves transfers already completed. Backward world-clock jumps do not repeat daily processing, and forward jumps are capped at seven days.

Offline progress is not implemented. Paused dedicated servers do not advance the simulation.

## Configuration

`config/mobrealms.properties` is generated on first server startup. Restart after changing it.

| Key | Default | Meaning |
|---|---:|---|
| `maxCamps` | 8 | Maximum camps |
| `maxPopulation` | 1000 | Maximum citizens |
| `maxDetailed` | 100 | Simultaneously detailed citizens |
| `aiInterval` | 20 | Ticks between staggered decisions |
| `workPerTick` | 8 | Maximum queued tasks executed per tick |
| `budgetMicros` | 2000 | Queue execution time target |
| `graceDays` | 3 | World or simulated days before natural founding attempts |
| `naturalCamps` | true | Enable natural founding |
| `naturalIntervalSeconds` | 30 | Seconds between search rounds (5–3600) |
| `naturalAttempts` | 8 | Site attempts per round (1–64), spread across ticks |

The first three limits are stored with a new simulation; changes currently affect new simulations only. M1 has no growth, so population is at most three citizens per camp. Excess loaded citizens pause detailed AI. The budget covers queue execution, not all vanilla entity work, saves or one-time founding operations.

## Saving and recovery

State is stored at `<world>/mobrealms/realms.dat`; the previous valid file is `realms.dat.bak`. Corrupt saves are not silently overwritten. Loading errors stop startup; saving errors halt simulation and are logged.

Back up the whole world before recovery. Entity files and mod state must agree, so prefer restoring a complete world backup. Minecraft chunks and the mod file are not one atomic transaction; process crashes during saves are not guaranteed to be fully consistent.

## Gameplay acceptance checklist

- Found both species; observe pickup, movement and actual delivery.
- Spawn nearby wild mobs; confirm vanilla behavior.
- Protect a free chunk; verify founding fails there.
- Unload a citizen carrying cargo and advance an abstract day; return and check that neither cargo nor citizens duplicate.
- Save/restart; compare IDs, stock and protected chunks.
- Repeat on a dedicated server; check commands without operator permission.
- Verify daylight sheltering and that distant camps do not force-load chunks.

These gameplay checks have not yet been confirmed as passed. Core tests do not replace them.

[Automated server installation](server-installation.md)

Save format 1 remains readable and upgrades to format 2 on save. Older mod builds cannot read format 2; restore a full world backup before downgrading.

Additional acceptance checks: open `/civ admin`, simulate 30 days, refresh, cancel, test pages and repeat without OP. Test natural camps in open terrain with `graceDays=0` after a restart, including protected land. These gameplay checks remain unconfirmed.

## Visible time-lapse and observer camera

`/civ speed 2` requests 40 TPS; `/civ speed 5` requests 100 TPS. This accelerates real movement, AI and world time for **everyone on the server**. Reset with `/civ speed 1` (20 TPS). Vanilla `/tick rate` permissions still apply, without privilege escalation; actual speed depends on server capacity. The mod does not persist this tick rate.

`/civ observe` explicitly switches you to spectator: fly around the camp or click a mob to follow its camera. Sneak exits the mob camera. Use `/civ observe creative` or `/civ observe survival` to choose your return mode; previous modes are not saved automatically. For conventional third-person view stay in creative and press F5. This is not an automatic orbit camera.

Healthy residents now patrol safe terrain inside their camp chunk at night if there are no gathering targets. Sun-sensitive residents stay sheltered by day; very small islands may lack reachable patrol targets. Patrols create no resources. Test gathering by dropping bones nearby at night. Abstract `/civ simulate 30` is distinct from visible time-lapse; production and city growth remain unimplemented.
