# Mob Inspector (Fabric, Minecraft 1.21.11)

Client-seitige Mod: zeigt beim Blick auf einen Block ein modernes HUD mit
- **MOBS ON BLOCK**: lebende `MobEntity`s, die auf der Blockoberseite stehen (inkl. Typ-Verteilung)
- **BLOCK DATA** bei Spawnern/Trial-Spawnern: Spawn-Entity und weitere synchronisierte Werte
- **STORED MOBS** ist immer `N/A` – Minecraft speichert keine verlässliche Mob-Anzahl in Spawnern.

## Wichtig vor dem ersten Build
Dieses Projekt wurde OHNE Netzwerk und OHNE Build-Lauf erstellt. Bitte:
1. Auf https://fabricmc.net/develop/ die Werte für 1.21.11 prüfen und in `gradle.properties` anpassen
   (`yarn_mappings`, `loader_version`, `loom_version`, `fabric_version`).
2. Gradle Wrapper erzeugen (liegt nicht bei, da die `gradle-wrapper.jar` binär ist):
   `gradle wrapper --gradle-version 9.2.0` (Gradle muss einmalig installiert sein) – oder `gradlew`, `gradlew.bat`
   und `gradle/wrapper/gradle-wrapper.jar` aus der Fabric-Beispielmod (Generator) hineinkopieren.
3. Java 21 verwenden.

## Bauen
`./gradlew build` → `build/libs/mobinspector-1.0.0.jar` in `.minecraft/mods` legen (zusammen mit Fabric API).
Zum Testen: `./gradlew runClient`.

## Bedienung
- **I** schaltet das HUD um (änderbar unter Optionen → Steuerung → Tastenbelegung → *Mob Inspector*).
- Einstellungen: `/mobinspector` im Chat oder die (standardmäßig ungebundene) Taste *Open Settings*.
- Konfiguration: `config/mobinspector.json`.

## Zählregeln
Eine Entity zählt, wenn sie lebt, am Boden steht, ihre Füße auf Höhe der Kollisionsoberkante des Blocks sind (±0,12)
und ihr Mittelpunkt in der Blocksäule liegt (oder im Modus *Overlap* ihre Bounding Box die Säule überlappt).
Jede Entity wird pro Abfrage nur einmal gezählt. Abfrage mit kleiner Box, 5×/Sekunde, nicht pro Frame.

## Einschränkungen (ehrlich)
- Multiplayer: Es werden nur vom Client geladene Entities gezählt (HUD zeigt Hinweis).
- Vanilla synchronisiert u. a. `SpawnPotentials` nicht zum Client → `N/A`. Trial-Spawner-Felder sind Best Effort.
- Keine Mob-Icons (zuverlässige, performante Darstellung nicht verifizierbar ohne Build).
- Nicht ausgeführt/getestet: Build, Spielstart, alle Punkte der Abnahmeliste (Abschnitt 10).

## Update: Mob Detection Mode & Reset
- **Mob Detection Mode** (Einstellungsmenü, Segment-Schalter): *On The Block* (nur Mobs auf dem Block),
  *In The Block* (nur Block-/Spawner-Daten, sonst „No Mob Data“), *Both* (beide Bereiche, durch Trennlinie getrennt).
  Standard ist *Both* (= bisheriges Verhalten). Gespeichert als `detectionMode` in `config/mobinspector.json`.
- **Reset Settings**: Bestätigungsdialog (Cancel / Reset). Setzt nur `mobinspector.json` (über die Feld-Defaults
  der Config-Klasse) und die beiden Mob-Inspector-Tasten auf Standard zurück; andere Tastenbelegungen bleiben unberührt.
- Das HUD bleibt hinter dem Einstellungsmenü als Live-Vorschau sichtbar (kein Blur), Änderungen wirken sofort.

## Bugfix: "In The Block" erkennt Entities im Blockvolumen
"In The Block" zählt jetzt echte MobEntities, deren Bounding Box das Volumen der Zielblock-Zelle schneidet
(Toleranz 0,01 Blöcke, damit Mobs auf der Oberseite oder an der Außenfläche nicht zählen). Zusätzlich werden
weiterhin Spawner-/Trial-Spawner-Daten angezeigt, falls vorhanden. Die Option "Count Area" betrifft nur "On The Block".
