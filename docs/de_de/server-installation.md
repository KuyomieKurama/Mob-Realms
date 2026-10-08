# Server automatisch installieren (Linux)

Das Skript installiert Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Pufferfish's Skills 0.19.2 und Mob Realms 0.9.0-dev. Es verwendet den offiziellen Fabric Installer 1.1.2. Es ist für Arch Linux und andere Linux-Systeme gedacht, ohne zusätzliche Python-Pakete.

Neue Installationen setzen `pause-when-empty-seconds=-1` in `server.properties`, damit geladene Siedlungen auch ohne Spieler weiterlaufen. Bei bestehenden Servern diesen Wert vor dem Neustart selbst setzen; das Upgrade-Skript bewahrt bestehende Einstellungen.

## Voraussetzungen

- Bash und Python 3.10 oder neuer. **Java 25 wird bei Bedarf automatisch installiert.**
- Falls Mob Realms noch gebaut werden muss: Internetzugriff für Gradle; ein fehlendes JDK 25 wird ebenfalls automatisch installiert.
- Mindestens 2 GiB freier Platz für die Installation; Welten benötigen später mehr.
- Standard: 1 GiB anfänglicher und 4 GiB maximaler Java-Heap, zusätzlich mindestens 256 MiB Reserve. Für Linux und andere Programme ist weitere Reserve sinnvoll.
- HTTPS-Zugriff auf Fabric/Mojang, bei Java-Installation auf Adoptium/GitHub und beim Build auf Gradle/Maven.

Das Skript prüft `JAVA_BIN`, `JAVA_HOME` und den PATH auf Java 25. Falls kein passendes Java verfügbar ist, lädt es das aktuelle Eclipse Temurin **JDK 25** von Adoptium, prüft dessen SHA-256 und installiert es benutzerlokal. Für einen Build wird auch geprüft, ob `javac` vorhanden ist.

Installationsort: `${XDG_DATA_HOME:-$HOME/.local/share}/mobrealms/jdk-25-x64` beziehungsweise `jdk-25-aarch64`. Unterstützt werden Linux x64 und ARM64 mit glibc (darunter Arch Linux). Das System-Java und andere Java-Versionen bleiben unverändert; `sudo` ist nicht erforderlich. Zusätzlicher Platz für Download und Entpacken: mindestens 2 GiB.

Das Startskript findet diese Installation beim gleichen Benutzer automatisch wieder, auch ohne gesetztes `JAVA_HOME`. Es lädt beim Start selbst nichts nach: Falls Java später entfernt wurde, erneut das Installationsskript ausführen. Bei anderem Benutzer oder verschobenem `XDG_DATA_HOME` muss Java dort verfügbar sein. Beschädigte vorhandene verwaltete JDKs werden nicht blind überschrieben. Die heruntergeladene Version und Prüfsumme stehen in `mobrealms-java.json` im JDK-Verzeichnis; automatische Java-Updates erfolgen nicht.

Du kannst weiterhin ein eigenes Java 25 über `JAVA_HOME`, `JAVA_BIN` oder PATH verwenden. `JAVA_BIN` bezeichnet genau eine ausführbare Datei, keine Shell-Befehlszeile.

## Installation aus dem Repository

Im Repository auf dem Branch `feat/m2-m4-realms`:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server"
```

Wenn die Mod-JAR fehlt, wird zuerst `./gradlew --no-daemon build` einschließlich der Kerntests ausgeführt. Danach werden Server und Bibliotheken heruntergeladen und ein `mods`-Ordner mit Fabric API, Pufferfish's Skills und Mob Realms angelegt. Die Installation startet noch keine Welt.

Mit bereits gebauter oder aus dem CI-Artefakt entpackter Mod-JAR:

```sh
./scripts/install-server.sh --dir "$HOME/mob-realms-server" \
  --mod /pfad/mob-realms-0.9.0-dev.jar --xms 1G --xmx 4G
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

## Diagnose bei stockender Simulation

Mob Realms schreibt alle 1200 Serverticks (bei 20 TPS etwa eine Minute) einen begrenzten Diagnoseblock nach `logs/latest.log`. `Mob Realms diagnostics` zeigt die mittlere und höchste Ausführungszeit des Mods pro Tick, Ticks über 10 ms, die Aufgabenwarteschlange, lokale Materialübergaben und Speicherzeiten. `Mob Realms work` teilt die Zeit auf Tagesverarbeitung, Bewohner-KI, Wirtschaft, Gründung, Zuteilung und Rekrutierung auf. `Mob Realms goals`, `Mob Realms resident` und `Mob Realms camp` zeigen Tätigkeiten, Positionen, Navigationsziele und Baufortschritt. Die Zeiten messen Mob Realms auf dem Serverthread, nicht die gesamte Server-Tickdauer.

Die Diagnose ist standardmäßig aktiv. Mit `diagnosticsEnabled=false` in `config/mobrealms.properties` und einem Serverneustart lässt sie sich abschalten. Für eine Untersuchung mit aktiven Bewohnern den betroffenen Bereich im Spiel laden und mindestens eine Minute `logs/latest.log` aufzeichnen.

Für einen einzelnen Start kannst du RAM überschreiben:

```sh
"$HOME/mob-realms-server/start-server.sh" --xms 512M --xmx 2G
```

Eine wiederholte Installation in einen gültigen, verwalteten Ordner prüft ihn nur und erhält Welt, Mods und Einstellungen. Neue RAM-Optionen überschreiben dabei nicht `server-memory.json`. Ein fremder oder nicht leerer Serverordner wird abgelehnt. Das Skript führt keine Updates oder Weltmigrationen durch; installiere neue Versionen separat und sichere Welten vor manueller Übernahme.

Bei beschädigten oder ausgetauschten verwalteten JARs bricht die Prüfung ab. Stelle die Originaldateien wieder her oder installiere in ein neues Verzeichnis, statt Prüfsummen blind anzupassen.

## Upgrade auf 0.9.0-dev

1. Den Server in seiner Konsole mit `stop` beenden und auf das vollständige Beenden warten.
2. Im Repository den Entwicklungsbranch aktualisieren und das Upgrade ausführen:

```sh
git switch feat/m2-m4-realms
git pull --ff-only
bash scripts/upgrade-server.sh --dir "$HOME/mob-realms-server"
```

Das Skript findet/installiert Java 25, baut die aktuelle Mod inklusive Tests und prüft die verwaltete Installation. Es erstellt ein **vollständiges Backup neben dem Serverordner** (einschließlich Welt, Konfiguration, Mods und Einstellungen). Danach ersetzt es die Mod-JAR und die Startprüfung. Minecraft, Fabric, zusätzliche Mods, EULA und Welt werden nicht aktualisiert. Für das Backup muss entsprechend freier Platz vorhanden sein. Symlinks werden abgelehnt, damit kein unvollständiges Weltbackup entsteht.

Mit bereits gebauter JAR:

```sh
bash scripts/upgrade-server.sh --dir "$HOME/mob-realms-server" \
  --mod fabric-mod/build/libs/mob-realms-0.9.0-dev.jar
"$HOME/mob-realms-server/start-server.sh" --check
"$HOME/mob-realms-server/start-server.sh"
```

Das Skript startet den Server nicht automatisch. Aktualisiere im Client ebenfalls die Mob-Realms-JAR und entferne die alte JAR aus dessen `mods`-Ordner. Fabric API bleibt gleich.

Bei gewöhnlichen Austauschfehlern werden die alten Dateien wiederhergestellt. Bei Prozessabbruch/Stromausfall kann `mobrealms-upgrade-incomplete.json` zurückbleiben; der neue Starter verweigert dann den Start. Den darin genannten vollständigen Backupordner als Serverordner wiederherstellen, statt den Marker blind zu löschen. Nach dem ersten Start wird das Realm-Speicherformat migriert: Ein Downgrade benötigt das vollständige Backup.

Das Skript ist für Server aus unserem Installer und Mod-Upgrades mit unveränderten Minecraft/Fabric-Versionen gedacht. Es lädt keinen beliebigen neuesten Release herunter; es baut den ausgecheckten Repository-Stand.
