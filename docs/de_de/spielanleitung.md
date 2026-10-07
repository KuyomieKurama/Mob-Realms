# Mob Realms spielen — 0.6.1-dev

## Installation und Einstieg

Client und Server benötigen Minecraft 26.3, Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3 und dieselbe `mob-realms-0.6.1-dev.jar` im jeweiligen `mods`-Ordner. Die Sources-JAR ist keine Mod. Details: [Server installieren und aktualisieren](server-installation.md).

Beginne in einer Testwelt auf Normal. `/realm` öffnet den Reichsatlas. Er zeigt einen Server-Schnappschuss; **Aktualisieren** lädt aktuelle Werte. Gold markiert die gewählte Siedlung, Türkis aktive Entwicklung. Übersicht und Forschung lassen sich mit dem Mausrad scrollen. Die Karte zeigt beanspruchte Chunks der aktuellen Siedlungsseite und Dimension, keine Landschaft.

## Wie eine Zivilisation wächst

Nach der konfigurierten Schonfrist suchen natürliche Gründungen in geladenen Gebieten bei Spielern nach freien Bauplätzen. Ein Lager startet mit drei Bewohnern, Dach, Banner und einmaligen Nahrungsvorräten. Wilde Mobs bleiben Vanilla-Mobs. Auf engen, geschützten, unebenen oder bereits belegten Flächen entsteht kein Lager.

Zuerst wird ein Feld geplant, dann Wohnraum, Lager und Werkstatt, später Markt und Verteidigung. Im Atlas siehst du das nächste Bauziel und den aktuellen Engpass. Baumeister setzen Blöcke einzeln. Sammler und Bergleute beschaffen dafür echte Ressourcen aus ungeschützten, eigenen Rohstoff-Chunks und angrenzendem geladenem Land. Bauflächen werden nicht abgebaut. Neue Gebäude brauchen eine freie, ebene Fläche von bis zu 9 × 9 Blöcken. Es werden keine Chunks zwangsgeladen.

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

Teste zuerst in einer Kopie deiner Welt: Lager bei Wald und ebener Freifläche gründen, Vorräte spenden, Bau bis Feld und Haus beobachten, 30 Tage simulieren, Rollen und Handel prüfen, speichern/neustarten und Baufortschritt vergleichen. Prüfe mit zwei Spielern, dass Verträge Zustimmung benötigen und fremde Rollen nicht geändert werden können. Teste UI bei verschiedenen GUI-Skalierungen und Zeitraffer anschließend wieder auf 1× zurücksetzen.

0.6.1-dev ist ein Entwicklungskandidat. Die automatisierten Kern- und Skripttests ersetzen keinen Spieltest von Navigation, Kampf, Rendering oder Mehrspielerbetrieb. Siedlungen sind einzelne politische Einheiten, noch keine Länder mit mehreren Städten. Keine automatische Reparatur, Straßenplanung, vollständige Untertageminen, dynamischen Forschungsbäume oder Questketten. Unbeladene neue Bauplätze werden nicht geplant. Gelände und knappe Ressourcen können Entwicklung anhalten; der Atlas zeigt den Engpass.

Speicherformat 4 liest alte Formate 1/2/3. Zurück auf eine alte Modversion nur mit vollständigem Welt-Backup; der Upgrader erstellt dieses bei gestopptem Server. Client-JAR beim Upgrade ebenfalls ersetzen.

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

Speicherformat 4 migriert Formate 1–3 automatisch. Vor dem Update ein Backup anlegen; alte Modstände können Format 4 nicht lesen. Teste nach dem Update Namen, Anwerben, Rangwechsel durch Wachstum und Tod des Oberhaupts sowie einen Neustart.

## Wenn nur einzelne Siedlungen arbeiten (0.6.1)

Es gibt kein Limit von einer arbeitenden Siedlung. Standardmäßig gelten acht Siedlungen, 1000 Bewohner insgesamt und **100 detaillierte KI-Plätze für die gesamte Welt**. Das Bauplanungsziel beträgt 96 Bewohner je Siedlung. Globale Limits sind im Spielstand gespeichert; eine Änderung der Konfigurationsdatei ersetzt sie in bestehenden Welten nicht. Das Admin-GUI zeigt die tatsächlich gespeicherten Werte.

Die Plätze rotieren jetzt alle zehn Sekunden fair zwischen geladenen Siedlungen und Bewohnern. Bei einem kleineren Budget als der Siedlungszahl kommen die übrigen Siedlungen im nächsten Zeitfenster dran. Pausierte Bewohner zeigen „Wartet auf rotierenden KI-Platz“; ihre Fracht bleibt bei ihnen. Entladene Bewohner zeigen ausdrücklich „abstrakte Simulation“. In der Übersicht stehen aktive und geladene Bewohner nebeneinander.

Rohstoffsuche, Verarbeitung, Lieferung und Bauen sind feste Grundfähigkeiten und werden nicht erst erlernt. Kleine Siedlungen ohne stabile Versorgung setzen auch Wachen/Händler als Arbeiter ein. Stark verletzte Bewohner können in Heimnähe alle fünf Sekunden ein gelagertes Brot für zwei Lebenspunkte verbrauchen, bis sie mindestens die halbe Gesundheit erreicht haben. Nahrung wird dabei wirklich abgezogen. Skelette und Zombies bleiben wegen Sonnenbrand tagsüber vorsichtig; im Nether wird ihnen kein fiktiver Sonnenschutz aufgezwungen.

Nur Zivilisationsbewohner erhalten die Mod-Arbeitssteuerung. Vanilla-Ziele werden bei Übernahme entfernt. Spezielle Eingriffe verhindern, dass Skelette beim Ausrüsten Ziele erneut eintragen, Piglin-Brains parallel steuern oder Endermen bei Tageslicht von der Arbeit wegteleportieren. Schadensreaktionen und Pathfinding bleiben grundsätzlich Minecraft-Mechanik. Creeper brauchen keine sichtbaren Hände, um den gemeinsamen Abbauauftrag auszuführen; Blockpartikel und Lagerbestand machen Abbau erkennbar.

Ein unbeladener Lager-Chunk verhindert neue Weltaktionen. Bereits vorhandene Farmen und bekannte, finanzierbare Projekte können abstrakt weiterlaufen, aber es gibt kein Erkunden unbekannten Geländes und kein Erz aus dem Nichts. Andere Gründe bleiben fehlende Rohstoffe, Schutzgebiete, unpassierbare Wege und ungeeignetes Gelände. Die Bauplatzsuche probiert jetzt neun Positionen je Chunk statt nur dessen Mitte; sie planiert keine Hügel automatisch.

Zum Prüfen acht Lager im geladenen Bereich aufstellen, Tag und Nacht beobachten und aktive KI-Zahlen vergleichen. Nach Ausrüstungswechsel eines Skeletts weiter Rohstoffsuche prüfen; Endermen sollen nicht durch Tageslicht verschwinden. Bei entladenen Lagern den expliziten Status erwarten. Diese Welt-/Wegfindungstests sind trotz erfolgreichem Build noch manuell durchzuführen.
