# Development

Target: Java 25, Minecraft 26.3. Build baseline: official Fabric example mod commit `44465cb0eb83932c72ece5934d32ddfc758802ed` (26.3 branch). Wrapper: Gradle 9.7.1, Loader 0.19.5, Fabric API 0.161.0+26.3. Loom pinned to released Maven version 1.18.3 rather than the example's mutable snapshot.

Run from the repository root with JDK 25 selected:

```sh
./gradlew test
./gradlew build
./gradlew runClient
./gradlew runServer
```

The core test harness has no third-party dependencies. Its assertions throw on failure regardless of Java's `-ea` setting. Core source currently uses a Java 17-compatible language subset, allowing an additional local compatibility check; the Gradle build targets Java 25.

Implementation is in progress. A successful build alone is not gameplay acceptance.
