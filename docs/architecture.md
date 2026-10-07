# Architecture and acceptance criteria

## Modules

- sim-core: pure Java 25, independent of Minecraft; domain state, utility decisions, tasks, economy, diplomacy and learning.
- fabric-mod: server-side observations, validated world actions, persistence, datapacks, commands and networking. Client rendering and UI use a separate source set.

Core classes: RealmSimulation, Civilization, Settlement, Citizen, UtilityBrain, SpeciesProfile, Task, Memory, Traits, Territory, Stockpile, ResourceSite, BuildOrder and Journey. These are proposed project classes, not Fabric API names.

The core emits intents; the adapter reports success or failure. Collection does not credit stock until delivery succeeds. All game text uses de_de and en_us translation keys.

## Decisions and data

Utility AI scores goals using needs, danger, species, role, character, memories and strategy. Interruptible task sequences execute goals. Hold times prevent oscillation. Datapack JSON can compose implemented behaviors; new behavior primitives require code.

Citizens have stable IDs. Territory keys include dimension and chunk coordinates. Diplomacy stores bounded scores, war state, compatible treaties and directed vassal links separately.

## Simulation consistency

Exactly one simulation authority owns each citizen. Detailed/abstract handoffs preserve identity, inventory and task state without duplication. Abstract extraction consumes finite surveyed deposits; unknown terrain supplies no invented resources. Construction reserves material and revalidates terrain and protection before gradual physical placement. No forced chunk loading.

Initial targets: eight civilizations, 1,000 citizens total, 100 detailed citizens maximum. Start with a configurable 2 ms scheduling target per tick, bounded world actions and navigation requests. Uninterruptible Minecraft calls prevent a strict wall-clock guarantee.

Optional offline catch-up is disabled by default; proposed cap is seven Minecraft days. Day processing handles sleep and time changes without repeated rewards. Backlogs execute in bounded batches.

## Persistence and tests

Version the save schema. Persist stable IDs, random state, reservations, task progress and completed daily reports. Validate datapack reloads before replacing active definitions. Test resource conservation, save round trips, simulation handoffs and deterministic results. Report executed checks separately from unperformed gameplay tests.

## Milestones

| Milestone | Scope | Acceptance |
|---|---|---|
| M1 | Setup, core, saving, config, datapacks, zombie/skeleton camps, dropped-item gathering, protection, background simulation, debug commands | Both species establish camps and deliver resources; restart and chunk transitions preserve identities and inventories |
| M2 | Growth, supply, production, equipment, block construction, trade, human NPCs | Settlements sustain themselves, pay construction costs and trade actual goods |
| M3 | Diplomacy, emissaries, player claims, recruitment, roles, UI and border map | Player creates a nation, recruits residents and signs an effective treaty |
| M4 | Daily reports, bounded bandit learning, technologies, veterans | Repeated observed tactics produce explainable adaptation constrained by resources and technology |
| M5 | More species, chronicles, leaders, caravans, rumors, quests, succession, sieges, events, observer mode | Multiple realms generate coherent stories within measured performance costs |

Prioritize chronicles, leaders and caravans early in M5. M1 camps use only a basic marker and storage point; full construction templates, infection and advanced tactics arrive later.

## Delivery policy

Each milestone includes complete source/resources, pinned build and Gradle wrapper, meaningful core tests, client/server verification status, gameplay checks, known limitations, German/English player guide updates and a changelog. Repository changes remain versioned here. Do not document planned behavior as shipped.

## M2–M4 implementation checkpoint

`RealmSimulation` owns citizen identity, leases, resident indexes, cargo and inventories. `Development` owns towns, roles, construction reservations, claims, reputation, treaties, consent offers, technology, veterans and daily bandit state. `RealmStore` format 3 serializes both; formats 1/2 migrate on load.

`EconomyController` executes loaded extraction/building/trader/combat actions and one queued town report per update. `Blueprints` validates seven datapack templates and snapshots each accepted project. `SettlerEntity` is a custom humanoid NPC. `NationCommands` validates mutations server-side; `RealmDashboard` sends bounded JSON snapshots. Client-only `RealmScreen` renders the atlas; `SettlerRenderer` renders skin/equipment layers.

The original roadmap above remains the target, not a claim that all goals are shipped. Current towns are single-settlement polities, emissaries are commands rather than physical actors, technology is a fixed five-step sequence, and only known farms/planned construction operate abstractly. There is no autonomous unloaded terrain discovery, broad species-specific doctrine, multi-city government or full offline catch-up. Performance budgets are scheduling targets, not hard guarantees around individual Minecraft calls.
