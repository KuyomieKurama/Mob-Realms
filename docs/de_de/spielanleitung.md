# Mob Realms spielen — 0.6.4-dev

## Installation und Einstieg

Client und Server benötigen Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 und dieselbe `mob-realms-0.6.4-dev.jar` im jeweiligen `mods`-Ordner. Die Sources-JAR ist keine Mod. Details: [Server installieren und aktualisieren](server-installation.md).

Beginne in einer Testwelt auf Normal. `/realm` öffnet den Reichsatlas. Er zeigt einen Server-Schnappschuss; **Aktualisieren** lädt aktuelle Werte. Gold markiert die gewählte Siedlung, Türkis aktive Entwicklung. Übersicht und Forschung lassen sich mit dem Mausrad scrollen. Die Karte zeigt beanspruchte Chunks der aktuellen Siedlungsseite und Dimension, keine Landschaft.

## Wie eine Zivilisation wächst

Nach der konfigurierten Schonfrist suchen natürliche Gründungen in geladenen Gebieten bei Spielern nach freien Bauplätzen. Ein Lager startet mit drei Bewohnern, Dach, Banner und einmaligen Nahrungsvorräten. Wilde Mobs bleiben Vanilla-Mobs. Auf engen, geschützten, unebenen oder bereits belegten Flächen entsteht kein Lager.

Zuerst wird ein Feld geplant, dann Wohnraum, Lager und Werkstatt, später Markt und Verteidigung. Im Atlas siehst du das nächste Bauziel und den aktuellen Engpass. Baumeister setzen Blöcke einzeln. Sammler und Bergleute beschaffen dafür echte Ressourcen aus ungeschützten, eigenen Rohstoff-Chunks und angrenzendem geladenem Land. Bauflächen werden nicht abgebaut. Neue Gebäude passen ihren Bauplatz durch Erdarbeiten, Fundamente und einen Zugang an das Gelände an. Es werden keine Chunks zwangsgeladen.

Holz wird zu Baumaterial verarbeitet, Weizen zu Brot; Werkstätten verarbeiten Roheisen mit Kohle. Baustoffkosten sind vereinfachte Baupakete, keine exakte Abbildung aller Vanilla-Rezepte. Beispielsweise bezahlt Holz auch Holzmöbel. Dekorative Farmblöcke gehören zur Feldvorlage.

Bauern erzeugen täglich begrenzte Nahrung aus funktionsfähigen Feldern. Dies ist eine Wirtschaftsberechnung; einzelne Weizenpflanzen werden nicht vollständig nach Vanilla-Regeln geerntet. Ohne Nahrung stoppt das Wachstum. Häuser schaffen je vier zusätzliche Wohnplätze. Nach ausreichend versorgten Tagen entstehen Bewohner; in entladenen Gebieten werden sie erst beim Laden sichtbar. Beschädigte Gebäude verlieren nach Prüfung ihren Nutzen. Automatische Reparatur ist noch nicht implementiert.

Geladene Händler reisen für Handel zwischen nahen Siedlungen. Ohne Handelspakt, Bündnis oder Vasallität gibt es keinen automatischen Tausch. Vorräte werden auf beiden Seiten tatsächlich abgezogen. Entladene Siedlungen können bestehende Felder nutzen und bereits geplante Bauprojekte bezahlen; sie erkunden und erfinden keine neuen Rohstoffvorkommen.

## Spezies

| Spezies | Verfügbarkeit und Unterschiede |
|---|---|
| Zombies | Oberwelt, suchen tagsüber Schutz |
| Skelette | Oberwelt/Nether, Sonnenschutz, ausrüstbare Bogenschützen |
| Creeper | Oberwelt, kontrollierte Mauerdurchbrüche nach Belagerungsforschung |
| Spinnen | Oberwelt, eigenes Bewegungsmodell |
| Endermen | Oberwelt/End, größere Tragekapazität, friedliche autonome Diplomatie |
| Piglins | Nether, passende Baupalette; zivilisierte Piglins zombifizieren nicht |
| Illager | Oberwelt, eigene dunkle Baupalette |
| Siedler | Oberwelt, eigene menschliche NPC-Entität mit neun Skins |

Alle nutzen denselben Wirtschaftskern. Infektion, Spinnennetze, ein eigenes Gold-Währungssystem und vollständig einzigartige Militärdoktrinen sind noch nicht enthalten. Angeworbene Bewohner behalten ihre ursprüngliche Spezies.

## Deine Nation

1. Stelle ein **Gründungsbanner** aus einem weißen Banner und einem Smaragd her (formlos).
2. Benutze es auf freiem, ebenem Boden mit fünf Blöcken freiem Raum über der Grundfläche. Halte mindestens 48 Blöcke Abstand zu anderen Lagern. Du kannst eine Nation besitzen.
3. Öffne `/realm` und wähle deine Siedlung. Spende Baumaterial oder Brot mit dem Gegenstand in der Haupthand.
4. Beanspruche einen angrenzenden, freien Chunk über **Diesen Chunk beanspruchen**. Geschützte und fremde Chunks sind ausgeschlossen; maximal 64 Chunks je Siedlung.
5. Weise Bewohnern im Bewohner-Tab Rollen zu. Der Knopf wechselt nacheinander durch Sammler, Bergmann, Baumeister, Bauer, Wache, Soldat, Händler und Anführer.

Anwerben kostet acht Smaragde. Der Bewohner muss innerhalb von 16 Blöcken stehen, aus einer NPC-Siedlung kommen und bei dir Wohnraum finden. Du brauchst dort mindestens 20 Ruf oder die Siedlung muss seit mindestens zwei Tagen hungern. Bewohner anderer Spieler können nicht abgeworben werden.

## Diplomatie

Ein Geschenk kostet vier Smaragde und verbessert Ruf und gegebenenfalls die Beziehung deiner Nation um zwölf. Vier Brot kosten einen Smaragd, sofern Vorrat und Ruf ausreichen und kein Krieg besteht. Der Tauschknopf tauscht vier eigene Holzplanken gegen vier fremde Bruchsteine bei gültigem Vertrag.

NPC-Verträge benötigen Beziehung 10 für Handel, 20 für Nichtangriff, 50 für Bündnis und 80 für Vasallität. Frieden benötigt mindestens −20; aus einem Krieg muss zunächst Frieden werden. Andere Spieler müssen Angebote annehmen; Angebote verfallen nach drei Simulationstagen. Krieg tritt sofort ein und beendet alte Vertragsangebote. Der Initiator einer akzeptierten Vasallität ist der Oberherr; Tribut und politische Kontrolle sind noch nicht umgesetzt.

Wachen und Soldaten bekämpfen nahe Bewohner und Spieler verfeindeter Nationen. Kreativ- und Zuschauerspieler sind ausgenommen. Krieg und Vasallitätsforderung haben einen zusätzlichen Bestätigungsknopf. Persönliche Geschenke sind auch ohne Nation möglich. Physische Gesandte fehlen noch.

## Lernen und Forschung

Jeden Simulationstag bewertet eine Siedlung Versorgung, Arbeit und Verluste. Ein begrenzter Bandit-Algorithmus vergleicht Wohlstand, Wachstum und Sicherheit. Die Gewichte und gelernte Fernkampfgefahr stehen im Forschungstab. Es gibt keine externen ML-Dienste.

Werkstätten erzeugen Forschungspunkte. Die fünf aufeinanderfolgenden Technologien kosten zunehmend Forschung und Bruchstein: Landwirtschaft verbessert Ernten, Mauerwerk ermöglicht Mauern, Schilde ermöglichen ressourcenabhängige Schutzschilde, Flankieren beeinflusst Kampfwege, Belagerung ermöglicht Creeper-Durchbrüche. Veteranen sammeln Erfahrung bei Arbeit und Kämpfen. Die Fernkampfanpassung erfasst aktuell tödliche Pfeilangriffe, nicht jeden abgegebenen Schuss.

## Administration und Zeitraffer

- `/civ admin`: Atlas mit Admin-Steuerung.
- `/civ found <spezies>`: gezieltes Testlager; Tab-Vervollständigung nutzen.
- `/civ info`, `/civ relations`, `/civ goals`: Zustände prüfen.
- `/civ simulate 30`: 30 Wirtschaftstage in begrenzten Arbeitsschritten einreihen; maximal 365 wartende Tage. Dies beschleunigt keine laufenden Vanilla-Bewegungen.
- `/civ simulate cancel`: wartende Tage abbrechen.
- `/civ speed 5`: gesamten Server auf bis zu fünffaches Ticktempo setzen; `/civ speed 1` setzt zurück. Erreichbares Tempo hängt von der Hardware ab.
- `/civ observe`: Zuschauerflug; Mob anklicken, um seine Kamera zu übernehmen. Schleichen verlässt die Mob-Kamera. `/civ observe creative` oder `survival` beendet den Zuschauermodus. Das ist die Vanilla-Zuschauerkamera, keine neue filmische Third-Person-Kamera.
- `/civ protect` und `/civ unprotect`: aktuellen Chunk schützen bzw. freigeben.

Neue von Spielern platzierte Blockgegenstände schützen standardmäßig ihren gesamten Chunk. Ältere Spielerbauten werden nicht automatisch erkannt: ihre Chunks vor dem Spielen manuell schützen. Der Schutz ist konservativ und kann auch nach einem fehlgeschlagenen Platzierungsversuch greifen. Es gibt keine Kompatibilitätsanbindung an fremde Claim-Mods.

Die Konfiguration unter `config/mobrealms.properties` enthält Wachstumsdauer, Aggressivität, Lerntempo/-grenze, Bevölkerung, Tagesbudget, natürliche Gründungen und Bauschutz. Server nach Änderungen neu starten. `learningRate=0` stoppt Strategieanpassung, nicht Wirtschaft und Forschung.

## Abnahmetest und Grenzen

Teste zuerst in einer Kopie deiner Welt: Lager bei Wald gründen und umliegendes Hügelland zum Bauen nutzen, Vorräte spenden, Bau bis Feld und Haus beobachten, 30 Tage simulieren, Rollen und Handel prüfen, speichern/neustarten und Baufortschritt vergleichen. Prüfe mit zwei Spielern, dass Verträge Zustimmung benötigen und fremde Rollen nicht geändert werden können. Teste UI bei verschiedenen GUI-Skalierungen und Zeitraffer anschließend wieder auf 1× zurücksetzen.

0.6.4-dev ist ein Entwicklungskandidat. Die automatisierten Kern- und Skripttests ersetzen keinen Spieltest von Navigation, Kampf, Rendering oder Mehrspielerbetrieb. Siedlungen sind einzelne politische Einheiten, noch keine Länder mit mehreren Städten. Keine automatische Reparatur, Straßenplanung, vollständige Untertageminen, dynamischen Forschungsbäume oder Questketten. Unbeladene neue Bauplätze werden nicht geplant. Gelände und knappe Ressourcen können Entwicklung anhalten; der Atlas zeigt den Engpass.

Speicherformat 5 liest alte Formate 1/2/3/4. Zurück auf eine alte Modversion nur mit vollständigem Welt-Backup; der Upgrader erstellt dieses bei gestopptem Server. Client-JAR beim Upgrade ebenfalls ersetzen.

## Bevölkerungsverluste und Admin-Ansicht

Bestätigte Todesfälle entfernen einen Bewohner sofort und genau einmal. Sein Wohnplatz wird frei, ungetragenes Lagergut bleibt erhalten, seine nicht abgelieferte Fracht geht verloren. Das Entladen eines Chunks zählt nicht als Tod. Verluste setzen den Wachstumsfortschritt zurück; am betroffenen Simulationstag entstehen keine neuen Bewohner. Mit mindestens zwei Überlebenden, Nahrung und Wohnraum kann sich die Bevölkerung danach erholen. Eine ausgestorbene Siedlung vermehrt sich nicht von selbst.

Nachwuchs wird gespeichert, bevor er in der Welt erscheint. Fehlt ein kollisionsfreier Platz beim Lager, bleibt er wartend und wird später erneut platziert. Wartende Bewohner zählen bereits zur Bevölkerung und zum Wohnraumbedarf. Die Übersicht zeigt Geburten seit Gründung, Verluste seit dem letzten Tagesbericht, versorgte Tage und den Wachstumsengpass.

Der eigene Admin-Tab bietet Simulation in 1/7/30/365 Tagen und Abbruch der Warteschlange. Unter Welt & Kamera stehen Zeitraffer, Zuschauermodus, Rückkehr und Chunk-Schutz. Ohne Adminrechte fehlt dieser Tab; Befehle prüfen die Rechte erneut. Alle Ansichten aktualisieren sich alle fünf Sekunden. Bei kleinen GUI-Skalierungen wechseln Pfeile zwischen Tabs.

Abnahmetest: einen Bewohner töten, Population und Verluste prüfen; Chunk entladen/laden (keinen weiteren Verlust erwarten); mit zwei Bewohnern und ausreichend Wohnraum/Nahrung mehrere Tage simulieren, neu starten und sicherstellen, dass jedes neue Mitglied nur einmal erscheint.

## Admin-Protokoll und größere Siedlungen

Unter `/civ admin` → **Protokoll** stehen die letzten 256 gespeicherten Simulationsereignisse, neueste zuerst. Tag, Siedlungskennung und Ereignis werden angezeigt; der Tooltip zeigt den vollständigen Text. Seitenknöpfe blättern, der mittlere Knopf filtert auf die gewählte Siedlung. Ein Klick auf eine Zeile wählt ihre Siedlung aus. Das Protokoll ist nur für Administratoren und aktualisiert sich mit der GUI alle fünf Sekunden.

Erfasst werden Gründungen, Gebäudefertigstellung, Geburten, Todesfälle, Technologien, explizite Diplomatieaktionen und wechselnde Engpässe. Engpässe werden höchstens einmal je zehn Sekunden und Siedlung aufgezeichnet, nicht bei jedem KI-Tick. Es handelt sich um ein begrenztes Simulationsprotokoll; vollständige Java-Fehler, Stacktraces und Meldungen anderer Mods stehen weiterhin in `logs/latest.log`.

Das Standard-Wachstumsziel beträgt jetzt **96 statt 48 Bewohner**. Mit `settlementTargetPopulation=96` in `config/mobrealms.properties` lässt es sich zwischen 3 und 256 einstellen. Fehlt die neue Zeile in einer bestehenden Konfiguration, gilt ebenfalls 96. Änderung per Serverneustart übernehmen. Auch bestehende Siedlungen können dann weiterbauen. Das ist das Ziel der Bauplanung, keine sofortige Bevölkerungsauffüllung oder starre Geburtenobergrenze. Nahrung, Wohnraum, 64 Gebiets-Chunks und globale gespeicherte Bevölkerungslimits gelten weiter; Startlager behalten drei Bewohner.

## Namen und gesellschaftliche Ränge

Alle Zivilisationsbewohner erhalten einen weltweit eindeutigen, gespeicherten Vor- und Familiennamen. Namen bleiben bei Rollenwechsel, Anwerben und Neustart erhalten; Namen verstorbener Bewohner werden nicht neu vergeben. Die ersten 4096 Kombinationen sind reine Namen, danach ergänzt ein fortlaufender Namenszyklus eine Nummer. Bestehende Bewohner erhalten beim Laden älterer Spielstände automatisch Namen. Die Mod verwaltet das Namensschild ihrer Bewohner; manuelle Namensschild-Änderungen werden durch die gespeicherte Identität ersetzt. Wilde Mobs bleiben unberührt.

Gesellschaftlicher Rang, Beruf und Veteranenstufe sind getrennt. Der Rang richtet sich nach Entwicklungsstand und ab 200 Erfahrung nach einer höheren Erfahrungsstufe:

| Stand | Zivile Ränge | Militärische Ränge | Führung |
|---|---|---|---|
| Lager | Neuling → Pionier | Rekrut → Veteran | Häuptling |
| Dorf (ab 8 Bewohnern) | Dorfbewohner → Freisasse | Milizionär → Unteroffizier | Dorfältester |
| Stadt (ab 24 + Markt) | Bürger → Meister | Stadtgardist → Hauptmann | Statthalter |
| Reich (ab 64 + Markt + 3 Technologien) | Reichsbewohner → Patrizier | Legionär → Kommandant | Herrscher |

Der erste Bewohner übernimmt die Führung, ohne seinen Beruf aufzugeben. Beim Tod oder Wegzug folgt deterministisch ein verbliebener Bewohner; die Führung wird gespeichert. In deiner Nation ernennt die Rollenwahl **Anführer** den gewählten Bewohner zum neuen Oberhaupt. Führungsränge sind hier Titel und Mitgliedschaft, keine fertige Erbfolge- oder Bürgerkriegsmechanik. Bei Bevölkerungsverlusten können Entwicklungsstand und Titel zurückgehen. Namen und aktuelle Ränge stehen am Mob und im Bewohner-Tab; Tooltips zeigen zusätzlich Beruf, Veteranenstufe und UUID.

Speicherformat 5 migriert Formate 1–4 automatisch. Vor dem Update ein Backup anlegen; Modstände bis 0.6.3 können Format 5 nicht lesen. Teste nach dem Update Namen, Anwerben, Rangwechsel durch Wachstum und Tod des Oberhaupts sowie einen Neustart.

## Wenn nur einzelne Siedlungen arbeiten (0.6.1)

Es gibt kein Limit von einer arbeitenden Siedlung. Standardmäßig gelten acht Siedlungen, 1000 Bewohner insgesamt und **100 detaillierte KI-Plätze für die gesamte Welt**. Das Bauplanungsziel beträgt 96 Bewohner je Siedlung. Globale Limits sind im Spielstand gespeichert; eine Änderung der Konfigurationsdatei ersetzt sie in bestehenden Welten nicht. Das Admin-GUI zeigt die tatsächlich gespeicherten Werte.

Die Plätze rotieren jetzt alle zehn Sekunden fair zwischen geladenen Siedlungen und Bewohnern. Bei einem kleineren Budget als der Siedlungszahl kommen die übrigen Siedlungen im nächsten Zeitfenster dran. Pausierte Bewohner zeigen „Wartet auf rotierenden KI-Platz“; ihre Fracht bleibt bei ihnen. Entladene Bewohner zeigen ausdrücklich „abstrakte Simulation“. In der Übersicht stehen aktive und geladene Bewohner nebeneinander.

Rohstoffsuche, Verarbeitung, Lieferung und Bauen sind feste Grundfähigkeiten und werden nicht erst erlernt. Kleine Siedlungen ohne stabile Versorgung setzen auch Wachen/Händler als Arbeiter ein. Stark verletzte Bewohner können in Heimnähe alle fünf Sekunden ein gelagertes Brot für zwei Lebenspunkte verbrauchen, bis sie mindestens die halbe Gesundheit erreicht haben. Nahrung wird dabei wirklich abgezogen. Skelette und Zombies bleiben wegen Sonnenbrand tagsüber vorsichtig; im Nether wird ihnen kein fiktiver Sonnenschutz aufgezwungen.

Nur Zivilisationsbewohner erhalten die Mod-Arbeitssteuerung. Vanilla-Ziele werden bei Übernahme entfernt. Spezielle Eingriffe verhindern, dass Skelette beim Ausrüsten Ziele erneut eintragen, Piglin-Brains parallel steuern oder Endermen bei Tageslicht von der Arbeit wegteleportieren. Schadensreaktionen und Pathfinding bleiben grundsätzlich Minecraft-Mechanik. Creeper brauchen keine sichtbaren Hände, um den gemeinsamen Abbauauftrag auszuführen; Blockpartikel und Lagerbestand machen Abbau erkennbar.

Ein unbeladener Lager-Chunk verhindert neue Weltaktionen. Bereits vorhandene Farmen und bekannte, finanzierbare Projekte können abstrakt weiterlaufen, aber es gibt kein Erkunden unbekannten Geländes und kein Erz aus dem Nichts. Andere Gründe bleiben fehlende Rohstoffe, Schutzgebiete, unpassierbare Wege und ungeeignetes Gelände. Die Bauplatzsuche probiert jetzt neun Positionen je Chunk statt nur dessen Mitte; sie planiert keine Hügel automatisch.

Zum Prüfen acht Lager im geladenen Bereich aufstellen, Tag und Nacht beobachten und aktive KI-Zahlen vergleichen. Nach Ausrüstungswechsel eines Skeletts weiter Rohstoffsuche prüfen; Endermen sollen nicht durch Tageslicht verschwinden. Bei entladenen Lagern den expliziten Status erwarten. Diese Welt-/Wegfindungstests sind trotz erfolgreichem Build noch manuell durchzuführen.


## Admin-Aktionen und Begegnungen (0.6.2)

Unter **Admin → Welt → Zur Siedlung** teleportierst du dich zur links ausgewählten Siedlung, auch dimensionsübergreifend. Der Server sucht im Umkreis von acht Blöcken und acht Höhenblöcken eine freie Landestelle mit festem Boden. Ohne passende Stelle schlägt die Aktion fehl. Das lädt Zielchunks; es gibt keine Garantie gegen angrenzende Gefahren oder feindliche Bewohner. Befehl: `/civ tp <Siedlungs-UUID>`.

**KI-Ziele** öffnet die Bewohnerliste mit Namen, Rang, Aufgabe und aktuellem Ziel. Wähle links die gewünschte Siedlung. Das Befehlsfeld unter Simulation/Welt unterstützt sämtliche `/civ`- und `/realm`-Befehle einschließlich Parameter, etwa `civ simulate 14`, `civ speed 3`, `civ unprotect`, `civ relations` oder `civ found mobrealms:creeper`. Ausführen schließt das Fenster; Ergebnisse und Fehler erscheinen im Chat. Die bestehenden Server- und Eigentumsprüfungen bleiben aktiv. Es ist ein Befehlsfeld ohne Autovervollständigung, kein eigener Formularassistent für jeden Befehl. Die Bewohneransicht zeigt derzeit höchstens 256 Bewohner je Siedlung; `/civ goals` erreicht alle.

Aktive Bewohner können wilde erwachsene Mobs derselben Spezies bei einer Begegnung innerhalb von vier Blöcken aufnehmen. Voraussetzungen: Nähe zum Lager (zwölf Blöcke), Sichtkontakt, kein aktuelles Kampfziel, mindestens vier Brot und freier Wohnraum sowie Platz im globalen Bevölkerungslimit. Der Mob behält seine UUID und erhält einen Namen und Zivilisations-KI. Benannte, persistente, gezähmte, angeleinte, berittene und bereits registrierte Mobs sind ausgeschlossen. Pro Lager gilt nach Erfolg eine Minute Pause bei 20 TPS; diese Begegnungspause beginnt nach einem Serverneustart neu. Aufnahme und Identität werden gespeichert, Anwerbungen erscheinen in den Admin-Logs. Dies ist lokale Aufnahme wilder Mobs, keine Diplomatie mit fremden Bewohnern.

Test: Zwei Siedlungen auswählen und jeweils teleportieren; auch Nether-Siedlung und ungültige UUID prüfen. Als Nicht-OP `/civ tp` versuchen. `civ simulate 14` im Befehlsfeld ausführen und Ziele prüfen. Bei einem bewohnten Lager mit Haus und vier Brot einen unbenannten erwachsenen Mob passender Spezies nahe einem aktiven Bewohner spawnen; nach spätestens einer Runde durch alle Siedlungen (fünf Sekunden je Siedlung) Aufnahme, Brotverbrauch, Namen und Log prüfen. Mit vollem Wohnraum, ohne Brot und benanntem Mob wiederholen: keine Aufnahme.


## Diagnose für stehenbleibende Bewohner (0.6.3)

Die Bewohneransicht zeigt Operatoren jetzt eine zusätzliche Arbeitszeile: bevorzugtes Material, Zielkoordinaten, Zeit seit letztem Erfolg, verworfene Abbauziele und die Zahl geprüfter Suchspalten der Siedlung. Der Tooltip am Rollen-/Anwerbeknopf zeigt den vollständigen Text auch auf kleinen Bildschirmen. Erfolg bedeutet Abbau, Ablieferung, platzierter Baublock oder tatsächliche Feldpflege; bloßes Laufen zählt nicht. Diese Diagnosezähler beginnen nach Entladen/Neustart neu. Sie sind keine gespeicherte Leistungshistorie.

- **Rohstoffsuche:** Arbeiter können alle noch fehlenden Baumaterialien suchen; fehlendes Holz sperrt Stein und Erde nicht mehr. Bereits eingelagerte Stämme zählen als verarbeitbares Holz. Kirschholz, Blasseiche, entrindetes Holz und Nether-Stämme funktionieren ebenfalls.
- **Suchgebiet:** gemeinsamer Spaltenscan in 49 Chunks (drei Chunk-Ringe um das Lager), ausschließlich bereits geladene Chunks. Maximal 32 Spalten je Arbeitsupdate. Entdeckte Vorkommen werden wiederverwendet; unbeanspruchtes Umland darf ohne vorherigen Claim genutzt werden. Geschützte Chunks, fremdes Territorium, Block-Entities, der Lagerkern und Gebäudeflächen werden ausgelassen. Nach einem vollständigen Suchdurchlauf heißt das Ziel bei weiterem Misserfolg ausdrücklich „Versorgung weiterhin blockiert“. Die Suche läuft weiter, damit neu geladene Gebiete oder veränderte Rohstoffe gefunden werden.
- **Abbau:** Oberflächennaher Stein kann durch bis zu vier natürliche Erdschichten freigelegt werden. Jede Schicht wird tatsächlich entfernt und als Fracht transportiert. Reichweite, Sichtlinie und Wegfindung begrenzen dies; es ist noch kein System für tiefe Schächte oder Treppenminen. Es entstehen reale Gruben.
- **Wegfindung:** Die erforderliche Pfadlänge ziviler Bewohner ist auf 96 Blöcke angehoben, damit weiter entfernte Quellen nicht schon an der oft nur 16 Blöcke großen Vanilla-Suchlänge scheitern. Teilpfade zählen nicht als erreichbares Ziel. Nach zehn Sekunden ohne Annäherung oder spätestens einer Minute wird ein Abbauziel verworfen und für 30 Sekunden gemieden (jeweils bei 20 TPS). Andere Arbeiter reservieren nicht denselben Abbaublock. Es gibt weiterhin keine Teleportation von Arbeitern oder erfundene Rohstoffe.
- **Unterschlupf:** Ungeschützte Zombies/Skelette pausieren tagsüber bewusst. „Im Unterschlupf – warte auf Nacht oder Kopfschutz“ ist daher kein Lernfehler. Ein getragener Kopfschutz erlaubt Tagesarbeit, solange er vorhanden ist. Fracht kann am Lager auch während der Tagesruhe abgeliefert werden. Ein fehlender Weg wird separat angezeigt. Bei Verletzung ohne Heilvorräte versuchen Bewohner wieder Arbeit, statt unbegrenzt auf nicht vorhandenes Brot zu warten.
- **Bauern:** Erfahrung erst am Feldrand, höchstens einmal je Minute. Zwischen Pflegebesuchen helfen sie bei Versorgung und Bau. Nahrung aus einem aktiven, besetzten Feld wird weiterhin im täglichen Wirtschaftsmodell bilanziert; das ist keine vollständige Simulation jedes Weizenblocks.

`/civ simulate 30` wertet 30 Zivilisationstage aus, führt aber **keine 30 Tage physischer Arbeit** aus und macht aus Mittag keine Nacht. Für sichtbare Entwicklung `/civ speed 5` verwenden und die Siedlungen geladen halten; anschließend `/civ speed 1`. Ein vollständiger Suchdurchlauf umfasst 12.544 Spalten, verteilt über die Arbeiter und deren verfügbare Arbeitsupdates. Grenzen durch fehlende Rohstoffe, ungeladene Chunks oder unpassendes Baugelände bleiben real.

### Abnahme in einer Testwelt

1. Bestehende Welt sichern, Client und Server auf 0.6.4-dev aktualisieren. Eine Creeper-/Illager-Siedlung nahe natürlichem Holz beobachten: Material, Zielkoordinaten, Vorräte und letzter Arbeitserfolg müssen sich ändern; nicht nur die Zielbezeichnung.
2. Holz hinter einer Wand bzw. unerreichbar oberhalb des Mobs anbieten und einen zweiten erreichbaren Bestand belassen. Neuversuche müssen steigen; das erste Ziel darf nicht sofort erneut gewählt werden.
3. Lager mit Kirsch- oder Blasseichenholz in Reichweite testen. Zusätzlich ohne erreichbares Holz, aber mit Stein/Erde: andere fehlende Baustoffe müssen gesammelt werden. Ohne jegliche passende Quelle ist eine Blockade korrekt.
4. Zombies bei Tag und Nacht beobachten, optional Helm ausrüsten. „Warte auf Nacht“ muss bei Nacht enden. Tage vorsimulieren allein ist dafür kein Test.
5. Einen Bauern vom Feld abtrennen: kein Erfahrungsgewinn durchs bloße Anlaufen. Nach Wiederherstellen des Wegs Pflege und anschließende Mitarbeit beobachten.
6. Geschützte Chunks und fremde Claims kontrollieren: keine Entnahme. Abbau, Lieferung, Baufortschritt und Neustart prüfen. Der automatisierte Build ersetzt diese Spielprüfung nicht.


## Bauen auf unebenem Gelände (0.6.4)

Eine fertig planierte 9 × 9-Fläche ist nicht mehr nötig. Die Planung vermisst das Baufeld, wählt die mittlere Geländehöhe und erzeugt einen gespeicherten Arbeitsplan:

1. **Freiräumen:** natürliche Hindernisse und höher liegendes Erdreich im Bauvolumen von oben nach unten entfernen. Alle verfügbaren Arbeiter können helfen. Nutzbarer Aushub wird als reale Fracht abgeliefert.
2. **Fundament:** tiefere Stellen bis zum Untergrund mit bezahlten Bruchsteinblöcken auffüllen. Die Reihenfolge ist von unten nach oben.
3. **Zugang:** einen drei Blöcke breiten, gestuften Eingang an der Nordseite anlegen, mit höchstens einer Blockhöhe je Stufe.
4. **Hochbau:** die normale Gebäudevorlage auf dieser Unterlage bauen. Hausposition und Wohnraum beziehen sich auf das Gebäude, nicht auf die Unterkante des Fundaments.

Die Übersicht zeigt die Phase und erledigte Vorarbeiten. Bewohner versuchen belegte Baufelder freizumachen; Spieler werden nicht wegteleportiert und nicht eingemauert. Unter einer dort stehenden Entität wird kein geplanter Bodenblock abgetragen. Geschützte Chunks, fremdes Territorium und Block-Entities bleiben ausgeschlossen. Wird ein erfasster Block vor seiner Bearbeitung verändert, stoppt der betroffene Arbeitsschritt mit einer Meldung.

**Grenzen:** je Bauplatz höchstens vier Blöcke Abtrag und sechs Blöcke Fundamenttiefe, maximal fünf Blöcke Höhenunterschied am fünf Schritte langen Zugang. Ungeeignete steile Stellen werden verworfen und weitere Standorte geprüft. Wasser/Lava, unbekannte feste Strukturen, fehlender tragfähiger Boden oder ungeladene Bereiche bleiben Hindernisse. Die Gebäude sind weiterhin Vorlagen; dies ist kein freier Architektur- oder Brückenplaner. Das kleine Gründungsdach benötigt weiterhin eine freie, tragfähige 3 × 3-Stelle. Bereits gegründete Siedlungen verwenden die neue Geländeplanung automatisch für neue Projekte.

Erdarbeiten brauchen geladene Bewohner. Die abstrakte Simulation darf erst nach den tatsächlich erledigten Vorarbeiten den weiteren Bau aus Vorräten vorfinanzieren. Dadurch entstehen keine fertig gemeldeten Häuser über unangetastetem Gelände. Arbeitsphasen, Ausgangsblöcke und Fortschritt werden in **Speicherformat 5** gespeichert; laufende Projekte aus Format 4 behalten ihre Fortschritte. Ein Downgrade auf 0.6.3 benötigt das Welt-Backup.

**Im Spiel prüfen:** Eine vorhandene Siedlung neben einem Hang mit zwei bis vier Blöcken Höhenunterschied geladen halten. In der Übersicht sollten Freiräumen → Fundament/Zugang → Hochbau erscheinen; die sichtbaren Änderungen müssen dazu passen. Vorräte und Aushub prüfen. Während der Vorbereitung speichern und neu starten: keine doppelte Bezahlung oder neu begonnene Baustelle. Danach Eingang, Wohnraum und Bewohnerwege prüfen. Einen benachbarten geschützten Chunk und eine Truhe kontrollieren: unverändert. An Wasser und extremen Klippen soll die Planung eine andere Stelle suchen. Für den Client-Test: `./gradlew runClient`.
