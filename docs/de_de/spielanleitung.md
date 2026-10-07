# Spielanleitung

[Projektübersicht](../../README.md) · [English](../en_us/player-guide.md)

## Stand

Noch keine spielbare Version. Die folgenden Funktionen sind geplant. Diese Anleitung wird mit jedem Meilenstein aktualisiert.

## Einstieg (M1 geplant)

Seltene Zombie- und Skelettlager entstehen mit einer anfänglichen Schonfrist. Nur zugeordnete Bewohner erhalten Zivilisations-KI; wilde Mobs bleiben Vanilla. Bewohner sammeln geeignete herumliegende Gegenstände und liefern sie an ihrer Lagerstelle ab. Untote berücksichtigen Tageslicht und suchen Schutz.

## Schutz und Simulation

Explizite Schutzgebiete sollen Spielerbauten sichern. Eine zuverlässige automatische Erkennung aller Spielerbauten ist nicht vorgesehen. Weltänderungen bleiben begrenzt und werden vor Ausführung geprüft.

Startziel: acht Zivilisationen mit insgesamt 1.000 Bewohnern; höchstens 100 davon werden gleichzeitig detailliert simuliert. Das ist keine Hardwaregarantie. Entladene Gebiete laufen abstrakt weiter, ohne erzwungene Chunk-Ladung. Optionaler Offline-Fortschritt wird beim Start begrenzt nachgeholt und bleibt standardmäßig ausgeschaltet.

## Debug-Befehle (noch nicht verfügbar)

| Befehl | Zweck |
|---|---|
| `/civ info` | Bewohner, Lager und Vorräte prüfen |
| `/civ relations` | Beziehungen ansehen |
| `/civ simulate <tage>` | Berechtigungspflichtige, begrenzte abstrakte Simulation |

Der Simulationsbefehl spult keine realen Entity-Bewegungen oder Kämpfe vor.

## Späteres Gameplay

M2 ergänzt Versorgung, Gebäude, Wirtschaft, Handel und menschliche NPCs. M3 bringt eigene Nationen, Anwerben, Rollen und Diplomatie. M4 ergänzt Lernen, Technologien und Veteranen. M5 erweitert Spezies, Geschichten, Karawanen, Kriege und Ereignisse.

## Installation und Fehlersuche

Es gibt derzeit kein installierbares Release. Geprüfte Installationsschritte für Client und Dedicated Server, konkrete Config-Schlüssel und Standardwerte sowie Spieltests und bekannte Grenzen folgen mit M1.
