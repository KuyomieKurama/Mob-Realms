# Mob Realms

Civilizations for Minecraft Java Edition: mobs establish settlements, gather resources, trade, and eventually develop diplomacy and adaptive strategies.

## Status / Entwicklungsstand

Design approved; M1 implementation pending. No playable release or verified build is available yet.

Architektur freigegeben; M1 steht noch aus. Es gibt noch keine spielbare Version und keinen verifizierten Build.

## Target platform

Minecraft Java Edition 26.3, Java 25, Fabric Loader and Fabric API, Gradle. Development in VS Code on Arch Linux. Use `net.fabricmc.fabric-loom` with unobfuscated Minecraft names, without Yarn mappings. Exact dependency artifacts must be verified and pinned during M1.

## Roadmap

- M1: Pure Java simulation core, persistence, debug commands, zombie and skeleton camps, gathering, protection and bounded background simulation.
- M2: Settlement growth, economy, block-by-block construction, trade and human NPCs.
- M3: Diplomacy, player nations, recruitment, diplomacy UI and border map.
- M4: Daily learning, technology tree and veterans.
- M5: Additional species, chronicles, leaders, caravans, sieges, events and balancing.

## Documentation policy / Dokumentation

Code, tests, resources and documentation are versioned in this repository. Each milestone includes German and English player instructions covering implemented features, installation, configuration, gameplay checks and known limitations. Planned features are explicitly distinguished from available gameplay.

Code, Tests, Ressourcen und Dokumentation werden in diesem Repository versioniert. Jeder Meilenstein erhält eine deutsche und englische Spielanleitung mit Installation, Konfiguration, Spieltests und bekannten Grenzen. Geplante Funktionen werden klar von bereits spielbaren Funktionen getrennt.

## Development

The planned entry point is `./gradlew runClient`. The Gradle wrapper and build configuration have not been added yet; this command is not currently available.
