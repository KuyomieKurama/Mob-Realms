# Server automatisch installieren (Linux)

Das Skript installiert Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 und Mob Realms 0.1.0-dev. Es verwendet den offiziellen Fabric Installer 1.1.2. Es ist für Arch Linux und andere Linux-Systeme gedacht, ohne zusätzliche Python-Pakete.

## Voraussetzungen

- Bash, Python 3.10 oder neuer und **Java 25**.
- Falls Mob Realms noch gebaut werden muss: vollständiges JDK 25 und Internetzugriff für Gradle.
- Mindestens 2 GiB freier Platz für die Installation; Welten benötigen später mehr.
- Standard: 1 GiB anfänglicher und 4 GiB maximaler Java-Heap, zusätzlich mindestens 256 MiB Reserve. Für Linux und andere Programme ist weitere Reserve sinnvoll.
- HTTPS-Zugriff auf Fabric/Mojang und beim Build auf Gradle/Maven.

Das Skript prüft die Voraussetzungen, installiert aber keine Systempakete und verwendet kein sudo. Wähle Java 25 über `JAVA_HOME`, `JAVA_BIN` oder deinen PATH. `JAVA_BIN` bezeichnet genau eine ausführbare Datei, keine Shell-Befehlszeile.

## Installation aus dem Repository

Im Repository auf dem Branch `feat/m1-foundation`:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server"
```

Wenn die Mod-JAR fehlt, wird zuerst `./gradlew --no-daemon build` einschließlich der Kerntests ausgeführt. Danach werden Server und Bibliotheken heruntergeladen und ein `mods`-Ordner mit beiden erforderlichen Mods angelegt. Die Installation startet noch keine Welt.

Mit bereits gebauter oder aus dem CI-Artefakt entpackter Mod-JAR:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server" \
  --mod /pfad/mob-realms-0.1.0-dev.jar --xms 1G --xmx 4G
```

Die Datei muss die normale Mod-JAR sein, nicht `-sources.jar`. Relative Pfade und Pfade mit Leerzeichen werden unterstützt.

## Prüfen und starten

```sh
"$HOME/mob-realms-server/start-server.sh" --check
```

`--check` startet nichts. Es meldet auch den EULA-Status; eine fehlende Zustimmung ist hier kein Prüffehler, blockiert aber den tatsächlichen Start.

Lies die [Minecraft-EULA](https://www.minecraft.net/eula). Wenn du zustimmst, setze in `eula.txt` selbst `eula=true`. Danach:

```sh
"$HOME/mob-realms-server/start-server.sh"
```

Wenn du die EULA bereits gelesen hast und ausdrücklich zustimmen möchtest, unterstützt die Installation auch:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server" --accept-eula --start
```

Ohne `--accept-eula` ändert das Installationsskript keine vorhandene Zustimmung in `true`. `--start` allein umgeht die EULA-Prüfung nicht.

Der Server läuft im Vordergrund. Beende ihn regulär mit `stop` in seiner Konsole. Ein zweiter Start über das verwaltete Startskript im gleichen Verzeichnis wird blockiert. Es wird kein Systemdienst eingerichtet und keine Firewall verändert.

## Was wird vor dem Start geprüft?

- Java **genau 25**, die getestete Hauptversion.
- Gültige RAM-Werte, `Xms <= Xmx`, erkannte physische/containerseitige RAM-Grenzen.
- Vorhandene und unveränderte Server-, Loader-, Bibliotheks- und erforderliche Mod-Dateien anhand des Installationsmanifests.
- Mod-IDs und festgelegte Versionen von Mob Realms/Fabric API; doppelte Mod-IDs und reine Client-Mods im `mods`-Ordner.
- Schreibbarer Serverordner und EULA-Zustimmung.

Downloads von Fabric Installer und Fabric API werden über HTTPS und mit der vom Fabric-Maven veröffentlichten SHA-256-Prüfsumme geprüft. Mojang-Server und Loader-Bibliotheken lädt der offizielle Fabric Installer; ihre installierten Dateien werden anschließend im lokalen Integritätsmanifest erfasst. Das Manifest ist kein kryptografisch signierter Schutz gegen absichtliche Manipulation.

Zusätzliche Mods können in `mods/` abgelegt werden. Ihre vollständigen Versionsabhängigkeiten und Konflikte prüft Fabric beim Bootstrap; das Skript ersetzt diesen Resolver nicht. Eigene Mods und Updates der zwei verwalteten JARs sind nicht automatisch kompatibel.

## Dateien und erneute Installation

- `start-server.sh` und `server_manager.py`: unabhängiger Start ohne Repository.
- `mods/mob-realms.jar`, `mods/fabric-api.jar`: installierte Mods.
- `server-memory.json`: dauerhaft gespeicherte RAM-Werte.
- `mobrealms-install.json`: Versions- und SHA-256-Manifest.
- `server.properties`, `eula.txt`: Minecraft-Einstellungen.

Für einen einzelnen Start kannst du RAM überschreiben:

```sh
"$HOME/mob-realms-server/start-server.sh" --xms 512M --xmx 2G
```

Eine wiederholte Installation in einen gültigen, verwalteten Ordner prüft ihn nur und erhält Welt, Mods und Einstellungen. Neue RAM-Optionen überschreiben dabei nicht `server-memory.json`. Ein fremder oder nicht leerer Serverordner wird abgelehnt. Das Skript führt keine Updates oder Weltmigrationen durch; installiere neue Versionen separat und sichere Welten vor manueller Übernahme.

Bei beschädigten oder ausgetauschten verwalteten JARs bricht die Prüfung ab. Stelle die Originaldateien wieder her oder installiere in ein neues Verzeichnis, statt Prüfsummen blind anzupassen.
