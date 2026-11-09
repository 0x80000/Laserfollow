# Prüfstand des ersten Prototyps

Stand: 2. November 2026.

## Version 0.1.15: Screenshots freigegeben

Auf Nutzerwunsch blockiert der Kameramodus keine Screenshots oder Bildschirmaufnahmen
mehr. FLAG_SECURE wird gelöscht. Damit sind auch normale Android-App-Vorschaubilder
möglich. Frühere Hinweise auf Screenshot-Schutz beschreiben ältere Versionen.
Build und Lint geprüft; Screenshot auf dem Pixel 9 noch nicht lokal geprüft.

## Version 0.1.14: optionaler Zielpunkt und Bahngenauigkeit

Gelber Zielpunkt standardmäßig ausgeblendet, Schalter direkt an den Kamerabedienelementen.
Umschalten setzt die Übung zurück; der Zielpunktabstand wird nur bei sichtbarem Ziel
angezeigt. Bahngenauigkeit ist der Anteil gültiger Beobachtungen mit höchstens
2 Prozent Abstand zur Bahn, normiert auf die kalibrierte Bereichsdiagonale.
Ungültige Beobachtungen sind ausgeschlossen; Erkennungsabdeckung wird separat gezeigt.
Anzeige erst bei ausreichenden Daten. Der Wert misst Bahnnähe, keine vollständige
Formabdeckung, Bewegungsqualität oder klinische Wirkung. Ein stiller Punkt auf der
Bahn kann daher 100 Prozent erreichen. Grenzwert, Ausfälle, verspätete Daten und
Rücksetzen sind durch einen Regressionstest abgesichert. Geräteprüfung bleibt offen.

## Version 0.1.13: dunklere Farbpunkte

Nutzer sieht im gedimmten Kamerabild weiterhin einen roten Punkt. Ein weißes
überbelichtetes Zentrum ist damit nicht als Ursache belegt. Farbfilter angepasst:
Mindestkanalwert 90 statt 150, Farbabstand 35 statt 65, Dominanzfaktor 1,4 statt 1,5.
Der Chroma-Vorfilter wurde entsprechend verkleinert und exhaustiv geprüft.
Neue Tests prüfen dunklere rote/grüne Punkte, falsche Farbe sowie neutrale,
sehr dunkle und schwach gesättigte Pixel. Farbige Umgebungsobjekte können dadurch
häufiger als Kandidaten gelten. Ursache und Verbesserung auf dem Pixel 9 sind
ohne Originalframes weiterhin unbestätigt. Keine Änderung an Kamerabelichtung.

## Version 0.1.12: kurze Bewegungsschlieren

Pixel-9-Rückmeldung: Punkt erscheint erst beim Verharren, keine Spur bei Bewegung.
Arbeitshypothese ist die Ablehnung länglicher Lichtflecken, nicht nachgewiesene
Gerätelatenz. Die App akzeptiert jetzt zusätzlich kurze dünne Farbkomponenten
(auch diagonal), begrenzt auf 12 Prozent der kleineren Bildkante, doppelte
bisherige Maximalfläche und mittlere Dicke höchstens sechs Analysepixel.
Position ist der Schwerpunkt des Lichtstrichs, keine vorhergesagte Spitze.
Tests prüfen horizontale/diagonale Striche und die Ablehnung großer Flächen,
langer Linien und Mehrdeutigkeit. Farbige kurze Striche können nun ebenfalls
Fehlkandidaten sein. Belichtung und realer Laser auf dem Pixel 9 sind ungetestet.

## Version 0.1.11: Verarbeitung und Spur

Wiederverwendeter Kandidatenpuffer mit Chroma-Vorfilter: neutrale Pixel werden
ohne RGB-Umwandlung ausgeschlossen. Ein exhaustiver Test des ausgeschlossenen
Chroma-Bereichs prüft beide Farben und Wertebereiche; ein Vergleichstest prüft
Stride, Crop und Wiederverwendung. Bildgrößenbegrenzung bleibt 640 Pixel.
Die Bewegungsspur verwendet einen gebündelten drawLines-Aufruf statt bis zu
3.000 Zeichenaufrufen. Keine Vorhersage oder erfundenen Zwischenpositionen.
Die Kopfzeile zeigt ausgelieferte Analysebilder/s und die Verarbeitungszeit des
zuletzt angezeigten Frames. Letztere ist keine Messung der gesamten Kamera-Latenz.
38 Kerntests; eine tatsächliche Beschleunigung ist noch nicht am Zielgerät gemessen.

## Version 0.6: schnellere Erfassung

Die feste 100-ms-Sperre im Analyzer entfällt. KEEP_ONLY_LATEST und höchstens eine
ausstehende UI-Zustellung verhindern eine Warteschlange alter Bilder. Große
Detektorpuffer werden wiederverwendet; ein Regressionstest prüft wiederholte
Erkennung, leere Folgebilder, Größenwechsel und Farbwechsel. Die Messwerttexte
werden während der Erfassung höchstens viermal pro Sekunde aktualisiert, die
Punktposition und Spur weiterhin mit jedem ausgewerteten Bild. Die tatsächliche
Rate und Latenz müssen am Zielgerät gemessen werden; es gibt keine 30-fps-Garantie.

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
