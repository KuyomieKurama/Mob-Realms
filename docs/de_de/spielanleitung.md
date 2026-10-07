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

Zombies gewichten Sammeln stärker, Skelette die Rückkehr zur Gruppe. Bei Sonne oder sehr niedriger Gesundheit suchen Bewohner ihr Lager auf. Wird das Dach zerstört, gibt es noch keine automatische Reparatur. Ohne Sammelziel patrouillieren gesunde Bewohner nachts auf begehbarem Boden im eigenen Chunk. Tagsüber bleiben sonnenempfindliche Bewohner im Schutz; auf zu kleinen Inseln fehlen unter Umständen sichere Patrouillenziele. Wilde Mobs erhalten keine neue KI. Lagerbewohner greifen in M1 keine Spieler an; Kämpfe und Diplomatie sind noch nicht implementiert.

## Automatische Gründungen

Nach drei Welt-Tagen oder drei simulierten Tagen beginnt die natürliche Besiedlung. Alle 30 Sekunden werden bis zu acht Standortversuche auf mehrere Ticks verteilt. Ein abgeschlossener simulierter Tag stößt ebenfalls eine begrenzte Suche an. Gleichzeitige Anfragen werden zusammengefasst; 365 Tage garantieren nicht 365 Gründungen.

Gesucht wird 32 bis ungefähr 136 Blöcke um aktive Spieler in der Oberwelt, nur in bereits geladenen Chunks. Es müssen natürliche Bodenarten (Grasblock, Erde, Sand, Podsol, Myzel oder Schneeblock), eine freie ebene 3×3-Fläche und Abstand zu vorhandenen Lagern vorhanden sein. Spieler in anderen Dimensionen werden übersprungen; Zuschauer in der Oberwelt können die Entstehung beobachten. Die Schonfrist, Suchhäufigkeit und Versuche sind konfigurierbar; Lager- und Bevölkerungsgrenzen gelten weiterhin.

Das sind erste Zivilisationslager mit drei neuen Bewohnern; wilde Mobs werden nicht umgewandelt. Noch entstehen keine Städte. Auf schwierigem Gelände kann die Suche scheitern. Mit `/civ protect` schützt du eigene Grundstücke ausdrücklich; auch ein Spieler kann natürliche Bodenblöcke gesetzt haben.

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
| `/civ admin` | Admin-GUI mit Lagerübersicht, Tageszahl, Abbruch und Chunk-Schutz |
| `/civ simulate <tage>` | 1–365 abstrakte Tage vormerken |
| `/civ simulate cancel` | Noch ausstehende Tage abbrechen |

Schutz wirkt nur auf Mob-Realms-Aktionen. Er verhindert weder Vanilla-Explosionen noch Spieleraktionen. Es gibt keine automatische Erkennung von Spielergebäuden. Beim Sammeln respektieren Bewohner ihre eigene Chunk-Grenze und geschützte Chunks.

## Admin-GUI

Öffne `/civ admin`. Das native Minecraft-Dialogfenster zeigt Lager und Koordinaten in Seiten mit je fünf Einträgen, die Gesamtbevölkerung, Vorräte und ausstehende Tage. Gib 1–365 Tage ein und wähle **Simulation starten**. **Aktualisieren** lädt den aktuellen Stand; die Anzeige ist kein Live-Stream. **Ausstehende Tage abbrechen** beendet die Warteschlange; **Aktuellen Chunk schützen** schützt deinen Standort.

Jede Schaltfläche führt einen normalen, erneut berechtigungsgeprüften Serverbefehl als anklickender Spieler aus. Minecraft kann für privilegierte Befehle eine Bestätigung anzeigen. Ohne Operatorrechte/aktivierte Cheats sind die Verwaltungsaktionen nicht verfügbar. Das Fenster pausiert die Simulation nicht. Es verwendet Vanilla-Rendering und benötigt keine zusätzliche GUI-Bibliothek. Mob Realms auf dem Client liefert die Übersetzungen.

## Hintergrundsimulation

Entladene Bewohner bleiben über stabile IDs mit ihren gespeicherten Minecraft-Entities verbunden. Bereits getragene Fracht wird beim nächsten abstrakten Tag eingelagert. Ohne solche Fracht entsteht kein neuer Bestand. Es gibt noch keine abstrakte Rohstoffgewinnung, Bevölkerungserhöhung oder Kriegsführung.

Geladene Bewohner werden durch `/civ simulate` nicht teleportiert oder beschleunigt. Der Befehl ändert auch keine Minecraft-Uhr. Eine Warteschlange umfasst maximal 365 Tage und wird einschließlich des Fortschritts innerhalb eines Tages gespeichert. Pro Tagesaufgabe werden höchstens 32 Bewohner geprüft. Ein Abbruch nimmt bereits erfolgte Lieferungen nicht zurück. Rückwärts gesetzte Weltzeit löst keine erneute Tagesbelohnung aus; große Vorwärtssprünge sind auf sieben Tage begrenzt.

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
| `graceDays` | 3 | Welt- oder simulierte Tage vor natürlichen Gründungsversuchen |
| `naturalCamps` | true | Natürliche Gründungsversuche erlauben |
| `naturalIntervalSeconds` | 30 | Abstand der Suchrunden (5–3600 Sekunden) |
| `naturalAttempts` | 8 | Standortversuche je Runde (1–64), über Ticks verteilt |

Die drei Bevölkerungs-/Lagergrenzen werden in einer neuen Welt festgeschrieben und mitgespeichert; Änderungen gelten zunächst nur für neue Simulationsstände. Ohne Wachstum entstehen in M1 höchstens drei Bewohner pro Lager. Überzählige geladene Bewohner pausieren ihre detaillierte KI. Das Zeitbudget begrenzt den Scheduler, nicht sämtliche Vanilla-Entity-Kosten, Dateispeicherung oder einmalige Lagergründungen.

## Speichern und Wiederherstellen

Bestehende Speicherstände im Format 1 werden gelesen und beim nächsten Speichern ins Format 2 migriert. Alte Mod-Versionen können Format 2 nicht lesen; vor einem Downgrade ein vollständiges Weltbackup wiederherstellen.

Weltzustand: `<welt>/mobrealms/realms.dat`. Die vorherige gültige Version liegt in `realms.dat.bak`. Beschädigte Dateien werden nicht stillschweigend überschrieben. Ein Ladefehler stoppt den Start; ein Speicherfehler hält die Simulation an und wird protokolliert.

Sichere vor Wiederherstellung den gesamten Weltordner. Bewohner-Entities und Mod-Zustand müssen zusammenpassen: Stelle möglichst ein vollständiges Weltbackup wieder her. Die Mod-Datei und Minecraft-Chunk-Dateien sind keine gemeinsame atomare Transaktion; bei Prozessabbruch während des Speicherns ist keine vollständige Crash-Konsistenz garantiert.

## Abnahmetests im Spiel

- `/civ admin` öffnen, 30 Tage simulieren, Anzeige aktualisieren, Abbruch und Seitenwechsel testen. Danach ohne OP erneut versuchen.
- `naturalCamps=true`, `graceDays=0` in einer neuen Testwelt einstellen, neu starten und in offenem Gelände natürliche Lager beobachten. Einen geschützten Bereich mitprüfen.
- Beide Lager gründen; passende Gegenstände ablegen und tatsächliche Ablieferung beobachten.
- Wilde Zombies/Skelette daneben spawnen: Sie behalten Vanilla-Verhalten.
- Einen freien Chunk schützen: Dort muss die Lagergründung scheitern.
- Eine Frachtaufnahme beobachten, das Gebiet entladen und einen abstrakten Tag simulieren. Nach Rückkehr dürfen weder Bewohner noch Fracht verdoppelt sein.
- Welt speichern, schließen und neu laden: IDs, Bestände und Schutzgebiete vergleichen.
- Dieselben Schritte auf einem Dedicated Server durchführen, auch ohne Operatorrechte testen.
- Tagsüber Schutzsuche prüfen und sich außerhalb des Gebietes aufhalten: Die Mod darf keine Chunks zwangsweise laden.

Diese Spieltests sind noch nicht als bestanden bestätigt. Automatisierte Kerntests ersetzen sie nicht.

[Automatische Serverinstallation](server-installation.md)

## Sichtbarer Zeitraffer und Beobachterkamera

`/civ speed 2` oder `/civ speed 5` fordert 40 beziehungsweise 100 Server-Ticks pro Sekunde an. Das beschleunigt echte Bewegungen, KI und Weltzeit für **alle Spieler**. `/civ speed 1` stellt 20 TPS wieder her. Die Vanilla-Berechtigung für `/tick rate` gilt zusätzlich; es werden keine Rechte erhöht. Tatsächlich erreichbares Tempo hängt von der Serverleistung ab. Ein Neustart bzw. Vanilla bestimmt die Lebensdauer dieser Tickrate, die Mod speichert sie nicht.

Mit `/civ observe` wechselst du ausdrücklich in den Zuschauermodus: fliege außerhalb des Lagers für eine freie Beobachteransicht oder klicke einen Mob zum Mitfahren an. Schleichen verlässt die Mob-Kamera. `/civ observe creative` bzw. `/civ observe survival` wechselt zurück in den gewünschten Modus. Der vorherige Modus wird nicht automatisch gespeichert. Für die klassische Third-Person-Sicht bleibe in Kreativ und nutze F5. Das ist keine automatische Orbit-Kamera.

`/civ simulate 30` verarbeitet dagegen nur abstrakte Tageslogik und ersetzt den sichtbaren Zeitraffer nicht. Wachstum, Bergbau und Gebäudeproduktion fehlen weiterhin; Patrouillen erzeugen keine Ressourcen. Teste bei Nacht mit auf den Boden geworfenen Knochen die tatsächliche Sammelbewegung.
