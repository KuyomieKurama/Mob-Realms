# Mob Realms

Minecraft mobs establish camps and gather materials, with a server-side simulation designed to grow into civilizations.

**Status: M1 development candidate on `feat/m1-foundation`. Not a stable release. Client and dedicated-server gameplay acceptance is still outstanding.**

## Documentation

- [Spielanleitung (Deutsch)](docs/de_de/spielanleitung.md)
- [Player guide (English)](docs/en_us/player-guide.md)
- [Build, tests and dependency provenance](docs/development.md)
- [Architecture and roadmap](docs/architecture.md)
- [Changelog](CHANGELOG.md)

## Implemented in the development branch

- Pure Java simulation core with camps, citizen identities, utility scoring and protected claims.
- Zombie and skeleton datapack profiles; three residents per starter camp.
- Physical collection of whitelisted dropped materials and internal camp inventories.
- Abstract delivery of already-carried cargo, without creating resources in unloaded chunks.
- Versioned, checksummed world saves with a previous-save backup.
- Server commands for founding, inspection, protection and bounded day simulation.
- Java 25 CI build and ten dependency-free core test scenarios.

## Build

Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5**, Fabric API **0.161.0+26.3**, Loom **1.18.3**, Gradle **9.7.1**. No Yarn mappings or additional runtime mod dependencies.

```sh
git clone https://github.com/KuyomieKurama/Mob-Realms.git
cd Mob-Realms
git switch feat/m1-foundation
./gradlew test
./gradlew build
./gradlew runClient
```

Development JAR: `fabric-mod/build/libs/mob-realms-0.1.0-dev.jar`. The sources JAR is not an installable mod.

## Planned

M2 adds production, growth, full building templates, trade and human NPCs. M3 adds player nations and diplomacy. M4 adds learning and technology. M5 adds more species and emergent stories. These are not current features.

All source, tests, resources and documentation are versioned here. Each milestone updates both player guides and states what was actually tested.
