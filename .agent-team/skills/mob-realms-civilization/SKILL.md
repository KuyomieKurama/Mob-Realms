---
name: mob-realms-civilization
description: Improve or investigate settlement growth, resident AI, offline progress, economy, skills, and persistence in the Mob-Realms Minecraft Fabric mod. Use when villages stall, become empty, fail to develop, or behave differently after restart.
---

# Mob Realms Civilization

In a Mob-Realms checkout, read `AGENTS.md`, `STATUS-UND-ROADMAP.md`, and the current version rules. Identify whether a symptom belongs to the dependency-free state/rules in `sim-core/` or the loaded-world adapter in `fabric-mod/`; trace both sides when necessary.

Use the bounded diagnostic blocks in `../mob-realms-server/logs/latest.log` to establish camp population, work times, goals, positions, stock, project phase and research before changing AI speed. A faster interval is useful only if work can actually finish. Check empty-camp recovery, housing, food, material supply, chunk loading and server pause separately.

Preserve accounting: deliveries explicitly enter stock; construction and immigration spend recorded goods; unloaded terrain cannot invent resources. Offline progress while the server runs must remain bounded and restart-safe. For new persistent fields, verify backward reading of existing worlds and save/reload behavior. Keep resident experience and roles associated with stable identities.

Prefer a focused core scenario for deterministic rules and a server run for entity movement, block placement or loaded-chunk effects. Run `./gradlew test build` after Java changes and `python3 scripts/check_resources.py` after resource edits. Compare measured progress or tick cost before and after; do not infer a long-term improvement from a short startup run. Use the managed backup/upgrade workflow before touching the live server.
