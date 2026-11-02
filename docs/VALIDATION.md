# Prüfstand des ersten Prototyps

Stand: 2. November 2026.

## Entwicklungsablauf

Codex erstellte den ersten Code in einer isolierten Umgebung unter Herdr.
Der Aufruf endete nach der Codeerstellung am Abo-Nutzungslimit. Der koordinierende
Agent übernahm Build und Fertigstellung. Claude Code prüfte anschließend unabhängig
die ursprünglichen Quellen, ohne Dateien zu ändern oder Tests vorzutäuschen.

## Befunde und Korrekturen

- Mehrdeutiger Java-Import `Region`: explizite Imports ergänzt, Build geprüft.
- Statischer Punkt auf einer Bahn: zusätzlicher Abstand zum bewegten Taktpunkt;
  kein klinischer Erfolgsscore. Regressionstest für Stillhalten.
- Größere Farbflecken bevorzugt: mittlere statt aufsummierte Helligkeit, kleinere
  Maximalfläche und Kompaktheitsprüfung; unterschiedlich große, ähnlich helle
  Kandidaten bleiben mehrdeutig. Einzelne farbige Objekte bleiben eine Grenze.
- Am ROI-Rand abgeschnittene Objekte: volle Komponente prüfen und grenzüberschreitende
  Kandidaten verwerfen. Regressionstest vorhanden.
- Verzerrter Kreis und richtungsabhängige Normierung: Seitenverhältnis berücksichtigt.
- Lange Ausfälle und verspätete Frames: Abdeckung senken, Zeit nicht zurücksetzen.
- Einzelbild-Ergebnis: mindestens fünf gültige Beobachtungen und 0,5 s erforderlich.
- 180-Grad-Drehung: Orientierungslistener aktualisiert CameraX und verwirft Kalibrierung.
- Task-Vorschaubilder: Kameramodus setzt FLAG_SECURE.
- Kamera-Observer: beim Schließen abmelden. Berechtigungsablehnung merken statt
  beim Wiederauftauchen automatisch nochmals zu fragen.
- Verarbeitung: feste Bildgrößenobergrenze, klare Meldung bei zu langsamer Verarbeitung,
  Framefreigabe auch bei Fehlern und keine Anzeige alter Punkte.
- YUV-Bereich: voller und begrenzter Wertebereich getestet; unbekannte Metadaten
  verwenden einen dokumentierten Fallback. Physische Farbprüfung bleibt erforderlich.

## Automatisierte Prüfungen

`assembleDebug`, `lintDebug` und `core:test` sind erfolgreich abgeschlossen.
26 Kerntests bestehen, ohne Fehler. Lint meldet keine Fehler und 18 Warnungen,
unter anderem zu neueren Bibliotheksversionen, Ressourcenpflege und UI-Allokationen.
Die Debug-APK wurde gebaut. Es gab noch keinen Emulator- oder echten Gerätelauf.
Der Review allein ersetzt weder diese Prüfungen noch die folgende Geräteliste.

## Noch nicht geprüft: echtes Android-Gerät und realer Laser

- [ ] Installation, Demo, alle Formen und Bedienelemente; kleines Display.
- [ ] Kamera erlauben, verweigern und dauerhaft verweigern; keine Rückkamera.
- [ ] Zwei Eckpunkte, umgekehrte Reihenfolge, winziger Bereich, Bildränder.
- [ ] Rot/Grün, weißes Licht, Farbflecken, mehrere Punkte, Punkt außerhalb der ROI.
- [ ] Punkt entfernen und wiederbringen: keine Nullfehler und keine überbrückte Zeit.
- [ ] Stillhalten auf der Bahn: Taktpunktabstand macht fehlendes Folgen sichtbar.
- [ ] Pause, Hintergrund, Sperrbildschirm, 90-/180-/270-Grad-Drehung.
- [ ] Kamerafehler und langsame Bildverarbeitung; mehrere Minuten Betrieb.
- [ ] Kalibrierung und Overlay unter verschiedenen Auflösungen und Lichtverhältnissen.
- [ ] Keine Kameravorschau in Android-Recents oder Screenshots.

Es wurde keine klinische Validierung vorgenommen. Eine automatische
Unterscheidung zwischen Laser und jedem beliebigen farbigen Gegenstand ist
nicht nachgewiesen. Schwellwerte sind auf Zielgeräten fachlich zu prüfen.
