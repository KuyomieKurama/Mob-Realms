# Automated Linux server installation

Installs Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Pufferfish's Skills 0.19.2 and Mob Realms 0.9.0-dev using official Fabric Installer 1.1.2.

New installations set `pause-when-empty-seconds=-1` in `server.properties` so loaded settlements keep working without players. Set this value yourself on existing servers before restart; the upgrade script preserves existing settings.

Requirements: Bash, Python 3.10+, HTTPS access and at least 2 GiB free disk space. **Java 25 is installed automatically when unavailable.** Building a missing mod JAR requires access to Gradle/Maven; a missing full JDK is provisioned too. No Python packages, sudo, system package changes, firewall changes or service installation are used.

From the repository's `feat/m2-m4-realms` branch:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server"
```

If the built mod is missing, this runs `./gradlew --no-daemon build` with core tests first. Alternatively supply an existing normal mod JAR (not the sources JAR):

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server" \
  --mod /path/mob-realms-0.9.0-dev.jar --xms 1G --xmx 4G
```

Installation downloads the server/libraries, creates `mods/`, installs Fabric API, Pufferfish's Skills and Mob Realms, and initializes server settings without loading a world.

## Check and start

```sh
"$HOME/mob-realms-server/start-server.sh" --check
```

This checks dependencies without starting. It reports EULA status, but a missing acceptance only blocks a real start.

Read the [Minecraft EULA](https://www.minecraft.net/eula). If you agree, manually set `eula=true` in the server's `eula.txt`, then run:

```sh
"$HOME/mob-realms-server/start-server.sh"
```

After reading the EULA, explicit acceptance and immediate startup are also available:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server" --accept-eula --start
```

`--start` alone does not bypass acceptance. The server runs in the foreground; enter `stop` to shut down cleanly. A lock blocks another managed start in the same directory.

## Checks and limits

The scripts verify Java exactly 25, valid Xms/Xmx settings, detected host/container memory limits, write access, required files and their stored SHA-256 hashes, pinned mod versions, duplicate top-level mod IDs, client-only mods and EULA acceptance. Additional mods' complete dependency constraints are resolved by Fabric during bootstrap, not by a custom version resolver.

Fabric Installer/API downloads use HTTPS and upstream Maven SHA-256 checksums. The official installer downloads Minecraft and loader libraries; their installed files are then recorded in a local integrity manifest. That manifest is not signed protection against intentional tampering.

Defaults are Xms 1G / Xmx 4G plus a checked minimum 256 MiB reserve. Leave additional memory for the OS. Select Java through JAVA_BIN (one executable), JAVA_HOME, or PATH. Override memory for one launch with `start-server.sh --xms 512M --xmx 2G`, or edit `server-memory.json` for persistent changes.

The installation is staged before being moved into a fresh/empty destination. Existing non-empty unmanaged directories are rejected. Rerunning against a valid managed server only verifies it and preserves world, mods and settings; new memory flags do not overwrite saved settings. Updates and world migrations are not automatic. Install new versions separately and back up worlds before migrating.

Installed files include `start-server.sh`, `server_manager.py`, `server-memory.json`, `mobrealms-install.json`, `server.properties`, `eula.txt`, and the `mods/` directory. The start scripts work independently of the repository. Restore original managed files or use a fresh installation if integrity checks fail; do not blindly change checksums.

## Automatic Java installation

The installer checks JAVA_BIN, JAVA_HOME and PATH for Java 25, then an existing managed JDK. If none is suitable, it downloads the current Eclipse Temurin JDK 25 through the official Adoptium API, validates its SHA-256, safely extracts it and verifies that it runs. Builds also require `javac`.

The JDK is installed under `${XDG_DATA_HOME:-$HOME/.local/share}/mobrealms/jdk-25-x64` (or `jdk-25-aarch64`). Linux x64/ARM64 with glibc is supported. No sudo or system-Java changes are needed. Allow an additional 2 GiB free space for Java download/extraction and HTTPS access to Adoptium/GitHub.

The start script automatically finds this JDK for the same user and XDG_DATA_HOME. Startup never downloads Java itself; rerun the installer if Java was removed. Broken managed installations are not silently overwritten. Version/download provenance is recorded in `mobrealms-java.json` in the JDK directory. Existing Java 25 installations are reused without automatic updates.

## Upgrade to 0.9.0-dev

Stop the server with `stop` and wait for it to exit. In the repository:

```sh
git switch feat/m2-m4-realms
git pull --ff-only
bash scripts/upgrade-server.sh --dir "$HOME/mob-realms-server"
```

The script finds/provisions Java 25, builds the checked-out mod with tests, validates the managed installation and creates a **full sibling server backup**, including worlds/settings/mods. It replaces the mod and starter validation, preserving Minecraft, Fabric, extra mods, EULA and world data. Sufficient backup disk space is required; symlinks are rejected. It does not fetch an arbitrary latest release or automatically start the server.

Alternatively supply `--mod fabric-mod/build/libs/mob-realms-0.9.0-dev.jar`. Run the installed `start-server.sh --check` and then `start-server.sh`. Replace the old client Mob Realms JAR and install Pufferfish's Skills 0.19.2 for Fabric 26.3 on the client too; Fabric API is unchanged.

Ordinary replacement failures restore the previous files. A crash may leave `mobrealms-upgrade-incomplete.json`, which blocks the new starter; restore the complete backup named there rather than deleting the marker blindly. After save-format migration, downgrading requires the full world backup. Only installer-managed servers with unchanged Minecraft/Fabric versions are supported.
