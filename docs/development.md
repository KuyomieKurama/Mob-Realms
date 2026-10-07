# Development and verification

## Version provenance

Build baseline: [official Fabric example mod, 26.3 commit 44465cb](https://github.com/FabricMC/fabric-example-mod/tree/44465cb0eb83932c72ece5934d32ddfc758802ed). Wrapper scripts and JAR are copied from that commit, retaining their notices. GitHub's Gradle action validates the wrapper JAR.

| Component | Pinned version | Source |
|---|---|---|
| Minecraft | 26.3 | Official 26.3 example |
| Java | 25 | Fabric 26.x requirement |
| Gradle | 9.7.1 | Example wrapper properties |
| Fabric Loader | 0.19.5 | Example gradle.properties |
| Fabric API | 0.161.0+26.3 | Example gradle.properties |
| Loom | 1.18.3 | [Published Fabric Maven directory](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.18.3/) |

The example used `1.18-SNAPSHOT`; this project pins a published stable artifact. GitHub Actions has resolved these dependencies and successfully compiled the full mod on Java 25. No Yarn/mapping declarations or remapping tasks are used. Gson is provided by Minecraft, not a separately added mod dependency.

## VS Code / Arch Linux

Select an installed JDK 25 as both your Java project runtime and Gradle JVM. Check `java -version` and `./gradlew --version` before building. Open the repository root in VS Code. Do not commit machine-specific JDK paths.

```sh
git clone https://github.com/KuyomieKurama/Mob-Realms.git
cd Mob-Realms
git switch feat/m2-m4-realms
./gradlew test
python3 scripts/check_resources.py
./gradlew build
./gradlew runClient
```

Mod artifact: `fabric-mod/build/libs/mob-realms-0.6.4-dev.jar`. The core is included in this JAR. Do not install `sim-core` or the sources JAR separately.

For a development dedicated server:

```sh
./gradlew :fabric-mod:runServer --args=--initSettings
```

This generates server settings without creating a world or accepting the Minecraft EULA. Review the EULA yourself and configure `fabric-mod/run/eula.txt` as appropriate before running:

```sh
./gradlew runServer
```

Use a fresh test world, Normal difficulty and operator permissions. See the player guide for gameplay acceptance.

## Tests

`sim-core:coreTest` is a dependency-free executable harness. It fails the build on any failed expectation without requiring Java's `-ea`. The standard JUnit-discovery task is disabled because the executable harness is the test runner; both `test` and `check` depend on that harness.

Twelve foundation scenarios cover randomized resource conservation, overflow rollback, stale ownership leases, claim/population limits, save/load identity and cargo, corruption/backup behavior, abstract delivery without resource creation, world-clock jumps, bounded scheduling and species utility decisions.

Six additional `DevelopmentTests` scenarios cover growth gates, prepaid abstract construction, atomic barter, treaty consent/claims, learning/technology and persistent recruitment/species. Both executable harnesses run through Gradle test/check. The shipping Gradle build targets Java 25. GitHub Actions runs the Java 25 build and core tests, checks JSON/translation parity and attempts a dedicated-server settings-only bootstrap. Successful development JARs are available as workflow artifacts, not stable releases.

## Known validation limits

A successful compile does not verify navigation, entity unload ordering, daylight shelter effectiveness, item collection in a running world, or multiplayer gameplay. No interactive client/gameplay acceptance has been performed. The settings-only dedicated-server bootstrap does not load a world.

The mod save uses a checksum, a backup and same-filesystem atomic rename. It is not transactionally coupled to Minecraft entity/chunk saves. Do not infer crash-safe cross-file consistency from the save roundtrip tests.

## Extending species

Datapack path: `data/<namespace>/mobrealms/species/<name>.json`. Supply the appropriate vanilla 26.3 datapack metadata. This example uses only implemented behaviors:

```json
{
  "schema": 1,
  "entity_type": "minecraft:zombie",
  "gather_weight": 1.2,
  "regroup_weight": 1.0,
  "carrying_capacity": 16,
  "avoids_sun": true
}
```

Its identifier is `<namespace>:<name>`. Add `species.<namespace>.<name>` to your resource-pack language files. Values must be finite/non-negative; carrying capacity is 1–64. The entity type must create a Mob. Existing profiles cannot be removed while camps reference them. Invalid reloads retain the previous profile set and log the problem; valid definitions activate after `/reload`.

Eight bundled profiles use an optional `dimensions` list. Building templates live in `data/<namespace>/mobrealms/buildings/`; the seven required building kinds each contain bounded relative block/material tiles. Active projects snapshot their tiles. Template changes currently require a server restart. New behavior primitives and independent faction templates still require code.

For a user-local installer JDK on Linux x64, use `export JAVA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}/mobrealms/jdk-25-x64"` and `export PATH="$JAVA_HOME/bin:$PATH"` before invoking Gradle directly (ARM64: `jdk-25-aarch64`). Installer/upgrade scripts select this JDK themselves.

## 0.6.3-dev validation

Local checks: Java 25 compilation of common and client sources against the official Minecraft 26.3 JAR and exact Fabric API modules; 28 core scenarios; 21 installer/upgrade tests; JSON and bilingual translation parity. Local Gradle is blocked by sandbox Unix-socket restrictions, so the repository CI is the full Loom build gate. Do not treat manual javac as a substitute for a successful CI build. No interactive Minecraft rendering or gameplay was run here.


## Terrain construction validation (0.6.4)

`./gradlew test` includes `TerrainTests`: rolling ground, top-down vegetation removal, blocked/fluid/unavailable plots, stepped entrance geometry, restart mid-preparation, a real format-4 fixture and prevention of abstract terrain completion. The fixture was generated by the previous format-4 encoder before changing serialization. There are 39 core scenarios and 21 installer/upgrader tests. `./gradlew build` produces the mod; `./gradlew runClient` launches manual acceptance. Interaction/pathfinding in a running game world still needs the checks in the player guides.
