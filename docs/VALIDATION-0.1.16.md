# Laserfollow 0.1.16 – Prüfung am 5. November 2026

Basis: ausschließlich lokales Projekt work/Laserfollow, vorher 0.1.15/versionCode 12.
Keine GitHub-Quellen übernommen. Manifest unverändert.

Änderungen:
- Feste Höhe für Status und Kamera-Kopfzeile verhindert Verschieben der Starttaste bei Textwechseln.
- Kurze Analyseunterbrechungen deaktivieren die Bedienelemente erst nach 1,5 s; Beobachtungen älter als 350 ms bleiben ausgeschlossen.
- Wiederholter Start während einer laufenden Übung setzt die Messung nicht zurück.
- Pro Analysebild dieselbe ausgewählte Laserfarbe für YUV-Vorfilter und Detektor.
- Vorhandene Erkennung dunkler Punkte und kurzer Bewegungsschlieren sowie Bildpuffer beibehalten. Keine unbestätigten Schwellenwertänderungen.

Prüfung:
- assembleDebug erfolgreich.
- lintDebug: 0 Fehler, 13 Warnungen.
- core:cleanTest core:test: 43 Tests, 0 Fehler, 0 Fehlschläge.
- apksigner verify erfolgreich; Zertifikat SHA256:
  f86cab1c5fe3554e0019e964efe89d3fa066c7261f0e4f36762f68f1f232acd7
- aapt: de.laserfollow.app, versionName 0.1.16, versionCode 13, minSdk 26, targetSdk 35.
- Launcher-Icon in alter und neuer APK sowie Quelldatei bytegleich, SHA256:
  fdaf831293c210b5a7c706835bd1a01a12857e0ddd08ea360388fb69d70acc33
- Kein Android-Gerät verbunden. Bedienung, Installation und reale Lasererkennung am Pixel 9 bleiben zu prüfen; bestehende Kerntests decken keine Android-UI ab.

Build-Umgebung: vorhandenes Gradle 8.11.1 und SDK, offline. Java benötigte einen kurzen Socket-Pfad über JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Schneider.THOENES\Documents\ChatGPT\Laserfollow\outputs. Keine dauerhafte globale Umgebungsänderung.
