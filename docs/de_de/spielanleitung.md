# Mob Realms – Spielanleitung

[Projektübersicht](../../README.md) · [English](../en_us/player-guide.md)

## Entwicklungsstand

M1 ist ein Entwicklungskandidat, keine stabile Veröffentlichung. Die folgenden Funktionen sind implementiert, müssen aber noch im Client und auf einem Dedicated Server abgenommen werden. Nutze eine neue Testwelt. Diplomatie, Wachstum, Lernen und menschliche NPCs folgen später.

## Installation und Start

Du benötigst Minecraft Java 26.3, Java 25, Fabric Loader 0.19.5 und Fabric API 0.161.0+26.3. Installiere die normale Mod-JAR und Fabric API sowohl auf dem Client als auch auf dem Server; nur so sind auch Übersetzungen vorhanden. Die `-sources.jar` ist nicht zum Spielen gedacht.

Zum Entwickeln auf Arch Linux/VS Code: Java 25 als Projekt-JDK und als Gradle-JVM auswählen, Repository klonen und den Entwicklungsbranch auschecken. Siehe [Build-Anleitung](../development.md).

```sh
./gradlew test
./gradlew build
./gradlew runClient
```

## Dein erstes Lager

1. Erstelle eine Testwelt in der Oberwelt, mit Cheats und Schwierigkeit Normal. Friedlich entfernt feindliche Vanilla-Entity-Typen und eignet sich nicht für diese Version.
2. Suche eine ebene, feste Fläche. Um deine Position müssen 3×3 Blöcke Boden und darüber vier Blöcke freie Luft sein. Das gesamte Lager muss innerhalb eines Chunks liegen; mit F3+G siehst du die Grenzen.
3. Führe `/civ found mobrealms:zombie` aus. Ein kleines Startdach, ein grünes Banner und drei Bewohner werden angelegt.
4. Gehe mindestens 48 Blöcke weiter und führe `/civ found mobrealms:skeleton` aus. Dieses Lager erhält ein blaues Banner.
5. Wirf nachts Knochen oder Bruchstein in die Nähe eines Lagers, innerhalb seines Chunks und höchstens zwölf Blöcke vom Zentrum entfernt. Die Bewohner laufen zu geeigneten Gegenständen und bringen sie zurück.
6. Prüfe mit `/civ info` den internen Lagerbestand und mit `/civ goals` die Ziele der Bewohner.

Das Startdach und Banner sind eine einmalige Grundausstattung. Sie werden noch nicht aus gesammelten Materialien gebaut. Die Lagerbestände sind intern gespeichert; es gibt noch keine plünderbare Truhe und keinen Auszahlungsbefehl.

Gesammelt werden unveränderte Vanilla-Gegenstände: verrottetes Fleisch, Knochen, Pfeile, Stöcke, Bruchstein, Eichenstämme, Kohle und Eisenbarren. Gegenstände mit abweichenden Komponenten werden nicht eingesammelt. Pro Gang tragen Bewohner höchstens 16 Stück. Sie können auch von Spielern weggeworfene Materialien dieser Liste nehmen.

Zombies gewichten Sammeln stärker, Skelette die Rückkehr zur Gruppe. Bei Sonne oder sehr niedriger Gesundheit suchen Bewohner ihr Lager auf. Wird das Dach zerstört, gibt es noch keine automatische Reparatur. Wilde Mobs erhalten keine neue KI. Lagerbewohner greifen in M1 keine Spieler an; Kämpfe und Diplomatie sind noch nicht implementiert.

## Automatische Gründungen

Nach drei Welt-Tagen wird alle 1.200 Server-Ticks mit einer Chance von 1:8 ein Standortversuch in der Nähe eines Spielers unternommen. Nur geeignete, bereits geladene Standorte in der Oberwelt werden verwendet. Das kann auf schwierigem Gelände lange dauern. Für reproduzierbare Tests verwende `/civ found`.

## Befehle

Alle Befehle benötigen die Minecraft-Berechtigung `COMMANDS_GAMEMASTER` (normalerweise Operator-Stufe 2 oder aktivierte Cheats).

| Befehl | Wirkung |
|---|---|
| `/civ info` | Lager, Bewohnerzahl, simulierte Tage und Bestände |
| `/civ goals` | Aktuelle Ziele der Bewohner; noch kein grafisches Overlay |
| `/civ relations` | Beziehungen zwischen Lagern, in M1 immer neutral (0) |
| `/civ found mobrealms:zombie` | Zombielager am eigenen Standort gründen |
| `/civ found mobrealms:skeleton` | Skelettlager am eigenen Standort gründen |
| `/civ protect` | Aktuellen Chunk gegen Gründung und Materialsammeln durch diese Mod schützen |
| `/civ unprotect` | Diesen Schutz entfernen |
| `/civ simulate <tage>` | 1–7 abstrakte Tage zur begrenzten Warteschlange hinzufügen |

Schutz wirkt nur auf Mob-Realms-Aktionen. Er verhindert weder Vanilla-Explosionen noch Spieleraktionen. Es gibt keine automatische Erkennung von Spielergebäuden. Beim Sammeln respektieren Bewohner ihre eigene Chunk-Grenze und geschützte Chunks.

## Hintergrundsimulation

Entladene Bewohner bleiben über stabile IDs mit ihren gespeicherten Minecraft-Entities verbunden. Bereits getragene Fracht wird beim nächsten abstrakten Tag eingelagert. Ohne solche Fracht entsteht kein neuer Bestand. Es gibt noch keine abstrakte Rohstoffgewinnung, Bevölkerungserhöhung oder Kriegsführung.

Geladene Bewohner werden durch `/civ simulate` nicht teleportiert oder beschleunigt. Der Befehl ändert auch keine Minecraft-Uhr. Eine Warteschlange umfasst maximal sieben Tage; noch nicht ausgeführte Debug-Tage werden bei einem Neustart verworfen. Rückwärts gesetzte Weltzeit löst keine erneute Tagesbelohnung aus; große Vorwärtssprünge sind auf sieben Tage begrenzt.

Fortschritt bei ausgeschaltetem Server ist noch nicht implementiert. Auf einem pausierten Dedicated Server tickt die Mod ebenfalls nicht.

## Konfiguration

`config/mobrealms.properties` entsteht beim ersten Serverstart. Änderungen benötigen einen Neustart.

| Schlüssel | Standard | Bedeutung |
|---|---:|---|
| `maxCamps` | 8 | Maximale Lagerzahl |
| `maxPopulation` | 1000 | Maximale Bewohnerzahl |
| `maxDetailed` | 100 | Gleichzeitige detaillierte Bewohner |
| `aiInterval` | 20 | Ticks zwischen gestaffelten Entscheidungen |
| `workPerTick` | 8 | Maximale Aufgaben pro Tick |
| `budgetMicros` | 2000 | Zeitbudget für die Aufgabenwarteschlange |
| `graceDays` | 3 | Welt-Tage vor natürlichen Gründungsversuchen |
| `naturalCamps` | true | Natürliche Gründungsversuche erlauben |

Die drei Bevölkerungs-/Lagergrenzen werden in einer neuen Welt festgeschrieben und mitgespeichert; Änderungen gelten zunächst nur für neue Simulationsstände. Ohne Wachstum entstehen in M1 höchstens drei Bewohner pro Lager. Überzählige geladene Bewohner pausieren ihre detaillierte KI. Das Zeitbudget begrenzt den Scheduler, nicht sämtliche Vanilla-Entity-Kosten, Dateispeicherung oder einmalige Lagergründungen.

## Speichern und Wiederherstellen

Weltzustand: `<welt>/mobrealms/realms.dat`. Die vorherige gültige Version liegt in `realms.dat.bak`. Beschädigte Dateien werden nicht stillschweigend überschrieben. Ein Ladefehler stoppt den Start; ein Speicherfehler hält die Simulation an und wird protokolliert.

Sichere vor Wiederherstellung den gesamten Weltordner. Bewohner-Entities und Mod-Zustand müssen zusammenpassen: Stelle möglichst ein vollständiges Weltbackup wieder her. Die Mod-Datei und Minecraft-Chunk-Dateien sind keine gemeinsame atomare Transaktion; bei Prozessabbruch während des Speicherns ist keine vollständige Crash-Konsistenz garantiert.

## Abnahmetests im Spiel

- Beide Lager gründen; passende Gegenstände ablegen und tatsächliche Ablieferung beobachten.
- Wilde Zombies/Skelette daneben spawnen: Sie behalten Vanilla-Verhalten.
- Einen freien Chunk schützen: Dort muss die Lagergründung scheitern.
- Eine Frachtaufnahme beobachten, das Gebiet entladen und einen abstrakten Tag simulieren. Nach Rückkehr dürfen weder Bewohner noch Fracht verdoppelt sein.
- Welt speichern, schließen und neu laden: IDs, Bestände und Schutzgebiete vergleichen.
- Dieselben Schritte auf einem Dedicated Server durchführen, auch ohne Operatorrechte testen.
- Tagsüber Schutzsuche prüfen und sich außerhalb des Gebietes aufhalten: Die Mod darf keine Chunks zwangsweise laden.

Diese Spieltests sind noch nicht als bestanden bestätigt. Automatisierte Kerntests ersetzen sie nicht.

[Automatische Serverinstallation](server-installation.md)
