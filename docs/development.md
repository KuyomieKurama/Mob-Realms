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
git switch feat/m1-foundation
./gradlew test
python3 scripts/check_resources.py
./gradlew build
./gradlew runClient
```

Mod artifact: `fabric-mod/build/libs/mob-realms-0.1.0-dev.jar`. The core is included in this JAR. Do not install `sim-core` or the sources JAR separately.

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

Ten scenarios cover randomized resource conservation, overflow rollback, stale ownership leases, claim/population limits, save/load identity and cargo, corruption/backup behavior, abstract delivery without resource creation, world-clock jumps, bounded scheduling and species utility decisions.

Core sources also compile with a Java 17 language subset, which enabled additional local checks in the development environment. The shipping Gradle build targets Java 25. GitHub Actions runs the Java 25 build and core tests, checks JSON/translation parity and attempts a dedicated-server settings-only bootstrap. Successful development JARs are available as workflow artifacts, not stable releases.

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

M1 still uses a fixed material whitelist and the same starter shelter for all profiles. Completely new behaviors, construction styles and faction templates are later work, not JSON-programmable features of this candidate.
