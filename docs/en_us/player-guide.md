# Playing Mob Realms — 0.6.4-dev

Install Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 and the same `mob-realms-0.6.4-dev.jar` on both client and server. Do not install the sources JAR. See [installation and upgrades](server-installation.md).

## Settlements

Start in a Normal-difficulty test world. `/realm` opens the navy/gold/teal realm atlas. Refresh retrieves a server snapshot; overview and research scroll with the mouse wheel. The map shows claimed chunks on the selected page and dimension, without terrain.

Natural founding searches loaded, clear, unprotected land near players after the configured grace period. Three residents receive a starter shelter, banner and initial food. Wild mobs retain vanilla behavior. Construction priorities are farms, housing, storage, workshops, markets and defenses. Buildings now adapt their plots with clearing, cut/fill foundations and a stepped entrance. The atlas reports the current objective and obstacle.

Builders place blocks and consume inventory. Gatherers and miners extract finite resources from unprotected commons and adjacent loaded land; building sites are excluded. Wood becomes construction material, wheat becomes bread, and workshops smelt raw iron with coal. Construction costs are simplified material packages, not exact vanilla recipes. No chunks are force-loaded.

Staffed farms produce bounded food daily; individual crops are not simulated as full vanilla harvesting. Housing and sustained food permit births. Each house supplies four additional places. Unloaded newborns appear when loaded. Damaged buildings lose their benefit when inspected; repairs are not automatic. Unloaded settlements can farm, trade existing stock and fund already planned projects, but do not discover resources or new building sites.

Eight profiles are available: zombies, skeletons, creepers, spiders, endermen, piglins, illagers and custom human settlers with nine skins. Zombies/skeletons avoid sunlight; endermen carry more and use peaceful autonomous diplomacy; piglins found in the Nether and do not zombify when civilized. Skeletons can equip bows, creepers unlock controlled siege breaches. Unique infection, web traps and a separate gold currency are future work. Most economic behavior is shared.

## Your nation

Craft a founding banner from a white banner and an emerald (shapeless), then use it on open, flat ground with five blocks of clearance, at least 48 blocks from other camps. One nation per player. Use `/realm` to donate held items, claim a neighboring free chunk (maximum 64), and cycle resident roles: gatherer, miner, builder, farmer, guard, soldier, trader, leader.

Recruitment costs eight emeralds, requires a resident within 16 blocks, free housing, and either reputation 20 or a source settlement starving for two days. Player-owned residents cannot be recruited away. Recruits retain their species.

## Diplomacy and trade

A four-emerald gift adds twelve reputation and, when applicable, twelve nation relationship points. Buy four bread for one emerald when stock/reputation permit and your nations are not at war. Barter exchanges four own planks for four foreign cobblestone under a valid treaty. Traders physically travel between nearby loaded settlements; abstract trade still conserves both inventories.

NPC acceptance thresholds: trade 10, non-aggression 20, alliance 50, vassalage 80; peace requires at least −20. Player rulers must accept offers, which expire after three simulation days. War is immediate and cancels outstanding offers. The proposer becomes overlord upon accepted vassalage; tribute/control are not implemented. War and vassalage require UI confirmation. Guards and soldiers fight nearby enemy-nation residents and non-creative/non-spectator players. Physical envoys remain future work.

## Learning

Daily reports score food, labor and losses. A bounded bandit selects prosperity, expansion or security. Workshops unlock five sequential technologies using research and cobblestone: improved agriculture, masonry/walls, shields, flanking and controlled creeper siege breaches. Work and combat grant veteran experience. Arrow adaptation currently observes fatal arrow attacks, not every projectile. Equipment costs actual materials. `learningRate=0` disables adaptation, not production/research.

## Administration

`/civ admin` adds admin controls. Use `/civ found <species>` with completion to test camps; `/civ info`, `/civ relations`, `/civ goals` inspect state. `/civ simulate 30` queues economic days (365 maximum); `/civ simulate cancel` cancels pending work. This does not speed up vanilla movement. `/civ speed 5` accelerates the entire server up to 5×, hardware permitting; `/civ speed 1` resets it. `/civ observe` enters spectator mode; click a mob to follow its camera, sneak to exit that camera, and use `/civ observe creative` or `survival` to leave spectator mode.

`/civ protect` protects the current chunk; `/civ unprotect` removes protection. Newly placed player block items conservatively protect their entire chunk by default, sometimes even after failed placement. Existing player builds must be protected manually. There is no external claim-mod integration. Configure growth, aggression, learning, population, founding and protection in `config/mobrealms.properties`, then restart.

## Acceptance and limitations

Test a copy of your world: establish a camp near wood and surrounding rolling terrain; donate supplies; observe farm/house construction; queue 30 days; trade and assign roles; restart and compare progress. With two players, verify treaty consent and ownership restrictions. Check multiple GUI scales. Reset time-lapse to 1× afterwards.

This is a development candidate, not a gameplay-validated release. Automated tests do not validate navigation, combat, rendering or multiplayer behavior. Each settlement is one polity; multi-city countries, automatic repairs, roads, full underground mines, dynamic technology trees and quests are not implemented. Terrain/resource scarcity can halt development; the atlas reports why.

Save format 5 reads formats 1/2/3/4. Downgrading requires restoring a full world backup. The stopped-server upgrader creates one; update the client JAR too.

## Deaths, births and administration

Confirmed deaths immediately remove a resident exactly once, release housing and reset growth progress. Undelivered cargo is lost; stored goods remain. Chunk unloading is not a death. No birth occurs on a loss-report day; at least two survivors, food and housing allow later recovery. Extinct settlements do not reproduce spontaneously.

Pending births persist across restarts and wait for a collision-free location near camp. They already count toward population and housing. Overview displays cumulative births, losses since the last daily report, fed days and the current growth blocker.

A separate Admin tab contains 1/7/30/365-day simulation presets and queue cancellation. World & camera contains time-lapse, spectator/return controls and chunk protection. Permissions remain server-enforced. Screens refresh every five seconds; narrow layouts use arrows to navigate tabs.

Acceptance: kill one resident and verify population/losses; unload/reload without another loss; supply food/housing to two survivors, simulate days and restart, checking that each pending resident appears only once.

## Admin logs and larger settlements

`/civ admin` → **Logs** shows the last 256 persisted simulation events, latest first, with day and settlement ID. Tooltips show full lines. Page controls browse history; the center button filters the selected settlement. Clicking a row selects its settlement. Logs are sent only to administrators and refresh every five seconds.

Events cover founding, completed buildings, births, deaths, technology, explicit diplomacy actions and changing bottlenecks. Bottleneck reports are limited to once per ten seconds per settlement. This is a bounded simulation log; complete Java errors, stack traces and other mods' output remain in `logs/latest.log`.

The default construction growth target is **96 residents, up from 48**. Set `settlementTargetPopulation=96` (3–256) in `config/mobrealms.properties` and restart. Existing configs without this entry also default to 96, and existing settlements can continue building. This is a planning target, not instant population or a strict birth cap. Food, housing, the 64-chunk territory limit and saved global population limits still apply. Starter camps retain three residents.

## Names and social ranks

Every civilization resident receives a world-unique persisted given name and surname. Recruitment, jobs and restarts preserve it; deceased names are never reassigned. The first 4096 combinations are plain names; later cycles add a number. Existing residents are named during legacy-save migration. The mod owns resident nameplates and replaces manual name-tag edits with the saved identity; wild mobs are unaffected.

Social rank is separate from job and veteran level. At 200 experience the resident receives the senior title for the current stage. Camps use newcomer/pioneer and recruit/veteran; villages (8 residents) use resident/freeman and militiaman/sergeant; cities (24 plus market) use citizen/master and guardsman/captain; realms (64 plus market and three technologies) use subject/patrician and legionary/commander. Leaders are chieftain, elder, governor and sovereign respectively. Population decline can lower stage and titles.

The first resident leads without losing their job. Death or recruitment away selects a remaining successor deterministically; leadership persists. Assigning Leader in your nation appoints that resident. This is basic leadership membership, not a full succession/civil-war system. Names and ranks appear on mobs and in Residents; tooltips include job, veteran level and UUID.

Save format 5 migrates formats 1–4. Back up before updating; builds through 0.6.3 cannot read format 5. Acceptance: compare identities across recruitment and restart, advance settlement stages, and kill the leader to check succession.

## Uneven settlement activity (0.6.1)

There is no one-active-settlement limit. Defaults are eight settlements, 1000 total residents and **100 detailed AI slots shared by the entire world**. The per-settlement construction target is 96. Global limits persist in the save; changing config does not replace them in existing worlds. Admin UI shows the actual saved values.

Detailed slots now rotate every ten seconds across settlements and residents. With fewer slots than settlements, remaining towns receive later windows. Budget-paused residents keep their cargo and display a waiting status; unloaded residents explicitly display abstract simulation. Overview shows active and loaded worker counts.

Gathering, processing, delivering and building are innate rules, not learned skills. Small/unfed settlements recall guards/traders to basic labor. Injured residents near home can consume one stored bread per five seconds to recover two health points until half health. Zombies/skeletons still avoid dangerous daylight, but Nether residents no longer shelter from nonexistent sunlight.

Realm members relinquish vanilla goals. Targeted hooks prevent skeleton equipment changes from reinserting goals, piglin brains from competing, and endermen from abandoning jobs through daylight teleportation. Wild mobs are unchanged. Vanilla pathfinding and damage reactions remain. Creepers use the shared harvesting executor even without rendered hands; block-break effects and inventory show extraction.

Unloaded camp chunks prevent new world actions. Existing farms and known funded projects can progress abstractly; unexplored terrain does not yield invented resources. Other blockers include missing resources, protected land, inaccessible paths and terrain. Construction now surveys nine positions per chunk, without automatically leveling hills.

Manual acceptance: compare eight loaded settlements across day/night, observe AI allocations, equip a skeleton and check continued work, observe enderman daylight behavior, then unload camps and verify status. Compilation/tests do not replace this gameplay check.


## Admin actions and encounters (0.6.2)

**Admin → World → Teleport to town** visits the selected settlement across dimensions. `/civ tp <settlement UUID>` searches within eight horizontal/vertical blocks for a clear landing above solid ground; failure leaves you in place. Destination chunks load. Nearby hazards and hostile residents remain possible.

**AI goals** opens the selected town's resident list. The command field accepts all `/civ` and `/realm` commands with parameters, including `civ simulate 14`, `civ speed 3`, `civ unprotect`, `civ relations` and `civ found mobrealms:creeper`. Execution closes the screen; results/errors appear in chat. Server permission and ownership checks remain authoritative. This is a command field without completion, rather than a separate form for every command. The resident list currently shows up to 256 people per town; `/civ goals` lists everyone.

Active residents near home can admit adult wild mobs of the same species within four blocks and line of sight, when neither is fighting. Admission requires four bread, housing and global population capacity. Named, persistent, tameable, leashed, mounted and already registered entities are excluded. Each town has a one-minute successful-admission cooldown at 20 TPS, reset on server restart. Identity, membership and recruitment log events persist. This is local wild-mob admission, not diplomacy with another civilization.

Manual checks: teleport between towns/dimensions, invalid UUID and non-OP permission denial; run a custom simulation duration from the command field. Near a resident within twelve blocks of home, spawn an unnamed adult matching the town species with housing and four bread available. Allow five seconds per settlement for a scan; check membership, name, bread and log. Repeat with no food, full housing and a named mob: no admission.


## Stalled-worker diagnosis (0.6.3)

Operators see preferred material, target coordinates, time since productive work, rejected targets and settlement survey-column count on resident rows. Hover the role/recruit button for the full text. Extraction, delivery, block placement and arrival for farm tending count as work; walking does not. Diagnostics reset on unload/restart.

Workers consider all missing construction materials. Stocked logs count toward plank demand; cherry, pale oak and stripped wood are supported. A shared scan covers 49 chunks within three chunk rings, up to 32 columns per work update, only in loaded terrain. Unclaimed wilderness can supply the town without a claim; protected land, foreign claims, block entities, the camp core and building footprints remain excluded. A completed unsuccessful scan explicitly reports blocked supply and keeps checking for world changes.

Shallow minerals can be exposed through up to four natural soil layers, removed one real block at a time and transported as cargo. Reach and line of sight still apply; this creates real pits and is not a deep mine/staircase system. Civilization residents have a required navigation path length of 96 blocks, avoiding the default follow-range-derived limit for distant sources. Targets are reserved between workers. Partial paths are rejected; no approach progress for ten seconds or a total minute causes an extraction retry with a 30-second target cooldown at 20 TPS.

Unprotected undead deliberately shelter by day. The UI distinguishes waiting for night from an unreachable shelter. Head equipment permits daytime work while it lasts. Cargo can be delivered at home during rest. Injured residents without healing food resume work rather than waiting indefinitely for unavailable bread. Farmers gain experience only on arrival at the field, at most once per minute, and help with other jobs between visits. Food remains a daily productive-farm model, not individual wheat harvesting.

`/civ simulate 30` does not run 30 days of physical work or advance daylight. Use `/civ speed 5` to watch physical work in loaded chunks, then restore `/civ speed 1`. A full survey is 12,544 columns shared among available worker updates. Missing resources, unloaded terrain and unsuitable construction ground can still block growth.

Manual acceptance: observe extraction/stock/build progress in existing towns; obstruct one source while leaving another accessible and check retries; test cherry/pale oak and a town missing wood but with other needed materials; verify undead resume at night or with a helmet; block a farmer's route and confirm no walking XP; check protected/foreign territory and restart. No interactive game-world acceptance was performed by the automated build.


## Building on uneven ground (0.6.4)

A pre-flattened 9 × 9 plot is no longer required. The survey chooses a median ground level and saves four stages: clear natural obstacles and higher ground, build paid stone foundations from the bottom up, add a three-block-wide stepped north entrance, then construct the original building. Usable excavated material travels as resident cargo. Foundation blocks consume real cobblestone. Building anchors and housing stay at the structure level.

The overview displays the stage and preparation progress. Residents try to leave occupied construction cells; players are not teleported or encased. Planned ground is not removed from under an occupying entity. Protected chunks, foreign claims, block entities and unexpected terrain changes stop the relevant operation.

Per plot: up to four blocks of cutting, six blocks of foundation depth and five blocks of elevation change along a five-step entrance. Excessive cliffs, water/lava, unknown solid structures, missing support and unloaded land cause a different plot to be considered. This remains template construction, not arbitrary architecture or bridge design. The initial camp shelter still needs a clear, firm 3 × 3 location. Existing settlements automatically use the new planning for new projects.

Preparation needs loaded workers; abstract construction funding starts only after actual terrain preparation. Save format 5 retains phases, expected source blocks and progress and migrates formats 1–4. A downgrade to 0.6.3 needs the world backup.

Manual acceptance: observe an existing settlement build on a two-to-four-block slope, check excavation/cargo/material costs and stage changes, restart mid-preparation, and verify the completed entrance, housing and navigation. Protected land and a chest must remain intact. Water or an extreme cliff should lead to another plot. Launch client testing with `./gradlew runClient`. Automated tests do not replace this gameplay acceptance.
