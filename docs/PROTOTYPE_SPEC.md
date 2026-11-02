# Prototypumfang

Eine sitzende Person trägt einen Kopf-Laser. Ein stationäres Android-Telefon filmt
mit der Rückkamera eine matte Wand. Die nachzufahrenden Formen werden ausschließlich
im Smartphone-Kamerabild eingeblendet. Kein Projektor und keine Wandvorlage nötig.

## Erste Version

- Deutsche Oberfläche, Kreis, horizontale Linie und Acht; Größe und Umlaufzeit wählbar.
- Klar gekennzeichnete Demo ohne Kamera und echte Kamera-Erkennung für Rot/Grün.
- Kalibrierung eines rechteckigen Bildbereichs durch zwei Eckpunkte.
- Bahn, bewegter Taktpunkt, erkannter Punkt und kurze Spur im selben Bildkoordinatensystem.
- Start, Pause/Weiter, Stopp; keine automatische Fortsetzung nach Hintergrundwechsel.
- Fehlende und mehrdeutige Erkennung hält den Taktpunkt an und trägt keinen Fehlerwert bei.
- Bahnabstand und Taktpunktabstand getrennt, Erkennungsabdeckung und erfasste Dauer.
- Keine klinische Gesamtnote, keine Winkel-/Zentimeterangaben, keine Therapieempfehlung.
- Lokale Bildverarbeitung; keine Bild- oder Sitzungsspeicherung und kein Netzwerkzugriff.

## Technische Abnahme

- APK lässt sich bauen; reine Java-Kernlogik wird automatisch getestet; Android Lint besteht.
- Tests erfassen Farbverwechslung, Störlicht, mehrere Punkte, große/angeschnittene
  Objekte, Kalibrierung, Bildseitenverhältnis, Zeitlücken und YUV-Pufferlayouts.
- Ein unabhängiger Review prüft Messlogik, Kamera-Lebenszyklus und Datenschutz.
- Reale Geräte- und Lasertests sind vor jeder weiteren Einsatzentscheidung nötig.

Nicht Teil dieser Version: klinische Validierung, Patientenverwaltung, Speicherung,
Therapiepläne, Cloud, automatische Erkennung von Telefonbewegung oder OpenRig-Routing.
