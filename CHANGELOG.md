# 0.9.0-dev — minor

- Add Pufferfish's Skills 0.19.2 as a required Fabric dependency and ship an eight-node resident tree covering building, mining, gathering and combat.
- Give each resident separate persistent experience and levels in those four branches. Real work and combat advance the corresponding branch; levels and mastery improve work or attack cadence.
- Show branch levels, experience and unlocked nodes on resident right-click and in the atlas resident tooltip. Existing resident experience migrates into the branch matching the saved role.
- Save format 6 reads earlier worlds. The Pufferfish API is player-oriented, so resident progress is stored by Mob Realms and evaluated against the shared Pufferfish tree; the native player skill screen does not display NPC progress.
- This is a minor release because it adds compatible gameplay and a backward-readable save migration.

# 0.8.0-dev — minor

- Keep seven-by-seven chunk work areas around each camp loaded while the server runs, allowing settlements to work when no player is online. Force-loading is paced at 16 chunks per second.
- Disable Minecraft's default empty-server pause on new managed installations; protect zombie and skeleton residents from sunlight because their work AI replaces vanilla sun avoidance.
- Add compact, furnished variants for new house, store, workshop and market projects; existing projects retain their saved plans. A matching already-placed chest tile can now be replayed after an interrupted save.
- Give residents stable individual aptitudes and let practiced residents work faster. Right-click a citizen for role, aptitude, rank, experience, goal and camp; nameplates now show color and symbols.
- Document the current state and outstanding work in `STATUS-UND-ROADMAP.md`.
- This is a minor version because offline work, house variants and resident interaction add compatible gameplay. Save data remains readable by this version.

# 0.7.1-dev — mini/patch

- Rescue residents whose cargo cannot reach a loaded settlement depot, preserving the collected goods.
- Keep stone surveys near accessible surface deposits and reject failed targets longer; search farther for timber without loading chunks.
- Use genuinely gathered dirt or planks for foundations, access paths and simple ground floors when cobblestone is missing.
- Correct builder reach at block centers, let gatherers and miners help with funded construction, and replace an NPC town's dead builder.
- Save format remains 5; existing worlds and client/server protocol remain compatible with this patch.

# 0.7.0-dev — minor

- Residents craft and wear pickaxes for stone and ore; experienced workers receive more frequent work opportunities.
- Wider local cargo delivery and nearer resource-target selection reduce construction stalls.
- Empty loaded settlements recover through nearby wild mobs or immigration paid from existing bread or seeds.
- Depots and new storehouses receive chests when supplies permit; stock accounting remains in the settlement inventory.
- Farms lead to storage and workshops sooner, productive work starts research, and harvested seeds can unlock agriculture.
- Bounded diagnostics include material stocks, research and technology. Save format remains 5.

# 0.6.4-dev

- Replace the flat 9x9 building-site requirement with bounded terrain cut/fill, paid foundations and a stepped entrance.
- Shared resident clearing, real excavated cargo, entity occupancy checks and persistent preparation phases.
- Keep building anchors independent of foundation/entrance geometry; prevent abstract completion before physical preparation.
- Format 5 migrates formats 1–4; seven terrain/save regression scenarios and DE/EN guides.

# 0.6.3-dev

- Parallel material deficits, shared bounded wilderness surveys and discovered-source reuse.
- Cherry/pale oak/stripped wood support and accounting for logs awaiting processing.
- Citizen navigation length increased to match the expanded survey without changing wild mobs.
- Reserved resource targets, 30-second rejection cooldowns, actual path reachability and progress timeouts.
- Shallow soil overburden extraction with real cargo and line-of-sight checks; protected land, foreign claims and settlement footprints remain excluded.
- Farmer arrival/cooldown checks before experience, useful work between tending visits.
- Separate sheltered daylight rest, blocked shelter/delivery, processing and exhausted-search goals; operator work diagnostics.
- Regression coverage for complete search coverage, alternative materials and path progress; save format remains 4.

# 0.6.2-dev

- Operator settlement teleport with bounded landing checks and dimension support.
- Admin goal shortcuts and parameterized /civ and /realm command field.
- Nearby wild-mob admission with housing, food, ownership exclusions and recruitment logs.
- Regression coverage for admission, duplicate IDs, food, caps and persistence.

# Changelog

## 0.6.1-dev — Fair civilization work

- Rotate detailed-AI slots fairly across settlements and residents; budget-paused residents retain cargo without abstract delivery.
- Prevent skeleton equipment changes from restoring vanilla goals and enderman daylight teleports from interrupting jobs; wild mobs remain vanilla.
- Survey nine candidate building positions per chunk, include deeper shallow resources, allow collision-free vegetation in approach paths and deliver at the central camp depot.
- Small/hungry settlements recall guards/traders for basic work; injured residents can recover using stored bread. Resource extraction emits block-break effects for all species.
- Expose loaded/active/waiting worker states and actual saved AI/population limits in the GUI. No forced chunk loading or invented remote resources.

## 0.6.0-dev — Resident identities and social ranks

- Persist world-unique resident names and leadership in save format 4, migrating formats 1–3. Names survive recruitment/restarts and are not reused after deaths.
- Social ranks adapt to settlement stage, civilian/military work and experience; leadership titles progress from chieftain to sovereign. Show identities on mobs and in resident tooltips.

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
