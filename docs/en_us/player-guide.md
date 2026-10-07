# Player guide

[Project overview](../../README.md) · [Deutsch](../de_de/spielanleitung.md)

## Status

No playable release exists yet. All mechanics below are planned. This guide will be updated with every milestone.

## Getting started (planned M1)

Rare zombie and skeleton camps appear with an initial grace period. Only assigned citizens receive civilization AI; wild mobs retain vanilla behavior. Citizens collect suitable dropped items and deliver them to camp storage. Undead citizens account for daylight and seek shelter.

## Protection and simulation

Explicit protected areas safeguard player builds. Automatic detection of all player-built structures is not promised. World changes are bounded and checked before execution.

Initial target: eight civilizations with 1,000 total citizens, at most 100 of them simulated in detail simultaneously. This is not a hardware guarantee. Unloaded regions use abstract simulation without force-loading chunks. Optional offline progress is applied as bounded catch-up work at startup and is disabled by default.

## Debug commands (not available yet)

| Command | Purpose |
|---|---|
| `/civ info` | Inspect citizens, camps and supplies |
| `/civ relations` | Inspect relationships |
| `/civ simulate <days>` | Permission-restricted, bounded abstract simulation |

The simulation command does not fast-forward physical entity movement or combat.

## Later gameplay

M2 adds supplies, buildings, economy, trade and human NPCs. M3 adds player nations, recruitment, roles and diplomacy. M4 adds learning, technologies and veterans. M5 expands species, stories, caravans, warfare and events.

## Installation and troubleshooting

No installable release is available. Verified client and dedicated-server instructions, exact configuration keys and defaults, gameplay checks and known limitations will ship with M1.
