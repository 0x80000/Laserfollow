# Prüfstand des ersten Prototyps

Stand: 2. November 2026.

## Version 0.4: Kamera und Bewegungsspur

Direkter Kamerastart ohne Demo-Auswahl. Kamera und zentrale Bedienelemente
einschließlich Kalibrierung stehen oberhalb des separat scrollbaren Detailbereichs.
Die blaue Spur zeigt bis zu 3.000 gültige Beobachtungen der laufenden Übung,
bleibt bei Pause/Stopp erhalten und wird bei Start, Neukalibrierung und Änderungen
gelöscht. Fehlende Beobachtungen unterbrechen die Linie. Keine dauerhafte Speicherung.
Geräteprüfung von Layout und realem Laser weiterhin ausstehend.

## Version 0.3: Oberfläche

Dunkle Petrol-Palette, mintfarbene Akzente, abgerundete Karten, größere Übungsfläche
und getrennte Bereiche für Steuerung, Verlauf und Einstellungen. Deaktivierte
Schaltflächen haben eigene Farben. Demo bleibt ausdrücklich als Simulation markiert.
Kamera- und Messlogik sind unverändert. Build, Lint und die 35 Kerntests wurden
erneut geprüft. Eine visuelle Geräteprüfung ist noch offen; lokal ist kein
Android-Gerät oder Emulator verbunden. Der reale Lasertest folgt durch den Nutzer.

## Version 0.2: Rückmeldung aus dem ersten Gerätetest

Der Nutzer meldete eine ruckelige Kameraansicht und einen nicht an der Wand
verankerten Bereich. Die alte Bitmap-Vorschau war auf etwa zehn Bilder/s begrenzt.
Version 0.2 verwendet eine native CameraX PreviewView und separate ImageAnalysis
im selben Viewport. Die Analyse bleibt bei etwa zehn Auswertungen/s, begrenzt aber
nicht mehr die Vorschau. Bitmap-Erzeugung und -Rotation entfallen aus dem Analyzer.
Kalibrierung und Detektion nutzen die offizielle CameraX-Koordinatentransformation.

Die Bildschirmorientierung wird im Kameramodus gesperrt; sensorbedingte Wechsel
setzen den Bereich nicht mehr zurück. Der Bereich ist bildfest, nicht weltfest:
bei Bewegung des Telefons bleibt Stativbetrieb oder erneute Kalibrierung nötig.
Eine robuste AR-Verankerung an einer strukturarmen Wand gehört nicht zu diesem Fix.

Neue Kern-Regressionstests prüfen Skalierung, alle Vierteldrehungen, Crop-Zuordnung,
Rücktransformation der Kalibrierung sowie fehlerhafte/singuläre Bildmatrizen.
35 Kerntests bestehen ohne Fehler. Claude Code hat Version 0.2 unabhängig unter
Herdr gelesen und keine Fehler in der Koordinatenzuordnung gefunden. Seine zwei
kleinen Befunde wurden korrigiert: Der Start-Watchdog prüft ausschließlich, ob
überhaupt ein erstes Bild vorlag; Kalibrierungsfehler zeigen ihre konkrete Ursache.
Diese beiden anschließenden Korrekturen wurden durch Build und Lint geprüft,
jedoch nicht nochmals durch Claude gelesen.
Vorschau-Bildrate und exakte Überlagerung müssen nach Installation von 0.2 erneut
am echten Gerät geprüft werden; eine konkrete Bildrate wird nicht garantiert.

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
- In 0.1: Orientierungslistener aktualisierte CameraX bei Drehung. In 0.2 durch
  gesperrte Kameraorientierung und Rücksetzen bei tatsächlicher Bildgeometrieänderung ersetzt.
- Task-Vorschaubilder: Kameramodus setzt FLAG_SECURE.
- Kamera-Observer: beim Schließen abmelden. Berechtigungsablehnung merken statt
  beim Wiederauftauchen automatisch nochmals zu fragen.
- Verarbeitung: feste Bildgrößenobergrenze, klare Meldung bei zu langsamer Verarbeitung,
  Framefreigabe auch bei Fehlern und keine Anzeige alter Punkte.
- YUV-Bereich: voller und begrenzter Wertebereich getestet; unbekannte Metadaten
  verwenden einen dokumentierten Fallback. Physische Farbprüfung bleibt erforderlich.

## Automatisierte Prüfungen

`assembleDebug`, `lintDebug` und `core:test` sind erfolgreich abgeschlossen.
Version 0.2: 35 Kerntests, keine Testfehler; Lint: 0 Fehler und 17 Warnungen.
In Version 0.1 bestanden 26 Kerntests ohne Fehler. Lint meldete keine Fehler und 18 Warnungen,
unter anderem zu neueren Bibliotheksversionen, Ressourcenpflege und UI-Allokationen.
Die Debug-APK wurde gebaut. Der erste Gerätetest durch den Nutzer ist oben dokumentiert.
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
