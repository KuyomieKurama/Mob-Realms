# Mob Realms

Server-side civilizations for Minecraft Java 26.3: purposeful construction, food and housing, diplomacy, player nations and daily strategy learning.

**0.9.0-dev — M2–M4 development candidate. Interactive client and multiplayer gameplay acceptance remains outstanding.**

[Aktueller Stand und Roadmap](STATUS-UND-ROADMAP.md)

## Play and install

- [Spielanleitung (Deutsch)](docs/de_de/spielanleitung.md)
- [Player guide (English)](docs/en_us/player-guide.md)
- [Server installieren / aktualisieren](docs/de_de/server-installation.md)
- [Server installation / upgrade](docs/en_us/server-installation.md)
- [Build and verification](docs/development.md)
- [Agent team for Claude, Hermes and Codex](docs/agent-team.md)
- [Architecture and scope](docs/architecture.md)
- [Changelog](CHANGELOG.md)
- [Versioning rules](docs/versioning.md)

## Current systems

- Eight profiles: zombies, skeletons, creepers, spiders, endermen, piglins, illagers and human settlers with skins and equipment.
- Persistent roles and settlement goals: farms → storage → workshops → housing → markets and defenses. Blocks consume stock; food and housing govern births.
- Finite world resource collection, processing, equipment crafting and treaty-gated barter. Loaded traders travel between nearby settlements.
- Founding banner, player claims, reputation, paid recruitment, role assignment and consensual player treaties.
- A navy, gold and teal realm atlas: overview, chunk map, diplomacy, residents and research. `/realm` for players; `/civ admin` adds administrator controls.
- Daily bounded bandit learning, five technologies, veterans and resource-dependent shields in response to observed fatal arrow attacks.
- Versioned saves, scheduled background days, protection, natural founding, observer camera and server-wide time-lapse.

## Build

Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5**, Fabric API **0.161.0+26.3**, [Pufferfish's Skills **0.19.2**](https://modrinth.com/mod/skills), Loom **1.18.3**, Gradle **9.7.1**. Official Mojang names; no Yarn.

```sh
git clone https://github.com/KuyomieKurama/Mob-Realms.git
cd Mob-Realms
git switch feat/m2-m4-realms
./gradlew test
./gradlew build
./gradlew runClient
```

Install `fabric-mod/build/libs/mob-realms-0.9.0-dev.jar`, Fabric API and Pufferfish's Skills 0.19.2 for Fabric 26.3 in **both client and server** `mods` folders. Do not install the sources JAR or sim-core separately. The installer provisions Java 25 when necessary; see the installation guide to select that JDK for direct Gradle commands.

This candidate uses simplified production, chunk-level protection and a territory map without terrain. Physical emissaries, full inter-settlement countries, infection, quests, true siege armies and advanced species behaviors remain future work. Read the guides before upgrading a world.
