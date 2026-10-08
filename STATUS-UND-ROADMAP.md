# Mob Realms: Stand und Roadmap

Stand: 8. Oktober 2026 · Entwicklungszweig `0.9.0-dev` · Minecraft 26.3 / Fabric.

## Was bereits funktioniert

- Acht Siedlungen werden in der bestehenden Testwelt gespeichert und nach Neustarts geladen. Das Spiel zeigt Bewohner, ihre Aufgaben, Vorräte, Baufortschritt und Diagnoseereignisse im Atlas an.
- Bewohner transportieren reale Materialien zum Lager und verbrauchen sie beim Bauen. Feststeckende Transporte werden mit erhaltener Fracht zum sicheren Lagerplatz gebracht. Baumeister können nach Ausfall neu zugeteilt werden.
- Bauernhof, Haus, Lager, Werkstatt, Markt, Wachturm und Mauer haben Baupläne. Neue Häuser, Lager, Werkstätten und Märkte können kompakte Varianten mit vollständigem Dach und passenden Einrichtungsblöcken erhalten. Begonnene Projekte behalten ihren gespeicherten Bauplan.
- `0.8.0-dev` hält einen Bereich von sieben mal sieben Chunks um jedes Lager mit Vanilla-Chunk-Tickets geladen und deaktiviert beim Installieren die Vanilla-Pause ohne Spieler (`pause-when-empty-seconds=-1`). Dadurch laufen Entitäten und Bauarbeiten bei laufendem Server ohne anwesende Spieler. Ein ausgeschalteter Server simuliert weiterhin nichts.
- Zombie- und Skelett-Bewohner erhalten Schutz gegen Tageslichtfeuer; die eigene Arbeits-KI ersetzt ihre Vanilla-Sonnenflucht.
- Individuelle Begabungen (1–5) sind aus der Bewohner-ID stabil abgeleitet. Erfahrung steigert die Arbeitsgeschwindigkeit. Rechtsklick zeigt Name, Beruf, Rang, Begabung, Erfahrung, Ziel und Lager; Namensschilder nutzen Farbe und Symbole.
- `0.9.0-dev` bindet [Pufferfish's Skills 0.19.2](https://modrinth.com/mod/skills) als Abhängigkeit ein und liefert einen Baum mit acht Knoten. Jeder Bewohner hat getrennte, gespeicherte Erfahrung für Bauen, Bergbau, Versorgung und Kampf. Echte Tätigkeiten und Kämpfe steigern den passenden Zweig; Stufen und Meisterschaft wirken auf Arbeits- beziehungsweise Kampftempo. Rechtsklick und Bewohner-Tooltip im Atlas zeigen die Fortschritte.
- Das Upgrade wurde nach vollständigem Backup auf den Live-Server eingespielt. Beim Start um 22:03 Uhr wurden der Pufferfish-Datenbaum, alle acht Knoten, acht Siedlungen und 28 Bewohner geladen; Port 25565 war erreichbar. Der Client benötigt ebenfalls Mob Realms 0.9.0-dev und Pufferfish's Skills 0.19.2.
- [Lithium 0.26.2](https://modrinth.com/mod/lithium) und [FerriteCore 9.0.0](https://modrinth.com/mod/ferrite-core) wurden für Fabric/Minecraft 26.3 verifiziert. Beide starten zusammen mit Mob Realms auf dem Testserver und seit dem Backup vom 8. Oktober, 21:51 Uhr auch auf dem Live-Server.
- Im Live-Probelauf von 21:52 bis 21:53 Uhr waren alle 26 Bewohner geladen und aktiv. Ein Illager-Bauernhof ging von 87/156 Blöcken in den abgeschlossenen Zustand über; anschließend begann dieselbe Siedlung ein Lagerprojekt. Der Mod lag nach dem Chunk-Warmup bei etwa 0,12–0,13 ms mittlerer Arbeit pro Tick. Das ist ein kurzer Test, kein Langzeitnachweis.

## Noch nicht gelöst

- Bestehende große Bauprojekte brauchen viele echte Rohstoffe. Ein fertiger Bau ist erst dann zugesichert, wenn der letzte Block tatsächlich gesetzt und bezahlt wurde; Rohstoffmangel oder ungeeignetes Gelände können weiterhin stoppen. Ein vollständiger Durchlauf aller Gebäudetypen auf der Live-Welt ist noch ausstehend.
- Offline laufende Siedlungen erhöhen die dauerhaft geladenen Chunks (höchstens 49 je nicht überlappendem Lager). Tickzeit und Speicherverbrauch müssen bei acht gleichzeitig aktiven Lagern gemessen werden. Die Tickrate 5× ist ein Zielwert, keine Garantie.
- Die Pufferfish-API verwaltet ihren eigenen Fortschritt und ihre Oberfläche nur für Spieler. Mob Realms liest deshalb die Pufferfish-Knoten, speichert NPC-Fortschritt separat und zeigt ihn in eigener Interaktion an. Die Pufferfish-Spieleroberfläche zeigt keine Bewohner an; ihre Spieler-Belohnungen werden nicht auf NPCs angewendet.
- Direkte Dialoge, Handel und Befehle an einzelne Bewohner sowie vererbbare Eigenschaften fehlen noch.

## Nächste Schritte

1. **0.9.0-dev verifizieren:** Pufferfish-Baum und NPC-Speicherung nach Neustart prüfen. Mindestens ein neues Cottage vollständig bauen und mehrere alte Projekte bis zum Ende beobachten. Chunkzahlen, TPS und Speicher über längere Zeit messen.
2. **Baugarantie verbessern:** Baupläne für alle Gebäudetypen auf erreichbaren Materialbedarf reduzieren, Blockaden automatisch und verlustfrei beheben, echte Fertigstellung einschließlich Dach, Innenraum und Truhen im Spieltest prüfen.
3. **Bewohnerbeziehungen:** Dialoge und Handel am Bewohner, wiederholbare Aufträge, sichtbare individuelle Berufserfahrung und Entscheidungen mit messbaren Folgen.
4. **Zivilisationsentwicklung:** Häuser, Lager, Werkstätten und Forschung zu klaren Ausbaustufen verbinden; Fortschritt, Bevölkerungswachstum und Engpässe im Atlas verständlich darstellen.
5. **Langzeittest:** 24 Stunden ohne Spieler mit acht geladenen Siedlungen; Speicher, TPS, Fortschritt, Speicherdateien und Neustartverhalten prüfen. Erst danach Zielwerte für große Welten festlegen.

Versionierung: `0.9.0-dev` ist **minor**, weil der NPC-Skillbaum neue kompatible Spielfunktionen bringt. Speicherformat 6 liest das vorherige Format 5. Siehe [Versionierungsregeln](docs/versioning.md) und [Changelog](CHANGELOG.md).
