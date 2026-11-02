# Laserfollow

Android-Prototyp für die lokale Verfolgung eines roten oder grünen Lichtpunkts.
Die Person sitzt, ein Kopf-Laser zeigt auf eine matte Wand und das fest aufgestellte
Smartphone filmt die Wand mit seiner Rückkamera. **Kreis, Linie und Acht erscheinen
im Kamerabild des Smartphones**, nicht auf der Wand.

Physio-Forschungsprototyp, **nicht klinisch validiert**. Keine Diagnose, kein
Therapieplan und keine Bewertung von Halswinkeln oder Genesung. Übungen und
Geräteauswahl mit einer Fachperson abstimmen. Bei Schmerzen oder Schwindel stoppen.
Laser niemals auf Augen, Menschen oder reflektierende Flächen richten.

## Ausprobieren

Android 8.0 oder neuer. Eine Debug-APK ist für Entwicklungsversuche gedacht,
nicht für einen klinischen Einsatz oder die Veröffentlichung im Play Store.

1. Die App startet in **Demo – simulierte Daten**. Mit Start lassen sich alle
   Formen ohne Laser ausprobieren. Die Simulation ist überall gekennzeichnet.
2. Für echte Erkennung auf **Kamera – Live-Erkennung** wechseln und den
   Kamerazugriff erlauben. Die Rückkamera muss die Wand sehen, das Display die Person.
3. Laserfarbe auswählen. **Wandbereich kalibrieren** antippen, dann im Bild zwei
   gegenüberliegende Ecken des Übungsbereichs wählen. Start ist erst nach einem
   frischen Kamerabild und gültiger Kalibrierung verfügbar.
4. Bahn, Größe und Umlaufzeit selbst auswählen. Türkis zeigt die Bahn, Gelb den
   bewegten Taktpunkt und Weiß den erkannten Punkt mit kurzer Spur.
5. Pause/Weiter und Stopp bedienen die Sitzung. Bei verlorenem oder mehrdeutigem
   Punkt wartet der Taktpunkt. Einstellungen setzen die Messung zurück.
6. Nach jeder Telefonbewegung neu kalibrieren. Drehen und Hintergrundwechsel
   unterbrechen die Übung; kein automatisches Fortsetzen.

## Was die Zahlen bedeuten

- **Nächster Bahnabstand:** mittlerer Abstand zur nächstgelegenen Stelle der Form.
  Stillhalten auf der Linie kann diesen Wert klein machen; er belegt keine
  vollständig nachgefahrene Form.
- **Abstand zum Taktpunkt:** mittlerer Abstand zum gelben bewegten Ziel. Dadurch
  wird Stillhalten auf der Bahn nicht als korrektes Folgen dargestellt.
- Beide Abstände werden in Prozent der Übungsbereichsdiagonale angegeben und
  berücksichtigen das Seitenverhältnis des Bildes. Keine Zentimeter oder Halswinkel.
- **Erfasst:** Dauer zwischen aufeinanderfolgenden gültigen Beobachtungen mit
  höchstens 350 ms Abstand. Lücken werden nicht überbrückt.
- **Erkennungsabdeckung:** erfasste Dauer geteilt durch die beobachtete aktive
  Zeit; auch lange Kameraausfälle senken sie. Bewusste Pausen sind ausgeschlossen.
- Unter fünf gültigen Beobachtungen oder 0,5 s erfasster Dauer erscheint
  „Unzureichende Daten“. Fehlerwerte sind Mittelwerte über gültige Bilder,
  keine klinischen Noten. Es gibt keine Erfolgsmeldung allein aufgrund dieser Werte.

## Erkennungsgrenzen und Datenschutz

Ein einfacher Farbschwellendetektor erkennt helle, farbdominante, kompakte
Komponenten. Kleine farbige Gegenstände können trotzdem als Punkt erscheinen:
**die App kann nicht beweisen, dass Licht von einem Laser stammt.** Sonne,
Reflexionen, weiße überbelichtete Zentren, Belichtungsautomatik und Bewegung des
Telefons können Ergebnisse verfälschen. Mehrere ähnlich helle Kandidaten werden
als mehrdeutig behandelt, statt einen erfundenen Mittelpunkt zu bilden.

Die Kamera verarbeitet höchstens etwa zehn Bilder pro Sekunde. Anzeige und
Erkennung verwenden dasselbe aufrechte Bild, begrenzt auf höchstens 640 Pixel
an der längeren Seite. Ein kreisförmiges Ziel bleibt auch im rechteckigen Bild rund.
Die YUV-Konvertierung berücksichtigt Crop, Zeilen-/Pixelstride und Bufferposition.
Sie nutzt BT.601-Koeffizienten mit vollem Wertebereich als Fallback und erkennt
bekannte begrenzte SDR-Datenspaces ab Android 13. Das ist keine photometrische
Kalibrierung; Farbtreue und Schwellen müssen auf echten Geräten geprüft werden.

Keine INTERNET-Berechtigung, kein Konto, keine Analytics, kein Backend, keine
Bild- oder Sitzungsspeicherung. Im Kameramodus verhindert `FLAG_SECURE` außerdem
Screenshots und die übliche Android-Task-Vorschau. Die Kamera wird beim Verlassen
freigegeben. Der Demomodus enthält ausschließlich simulierte Daten.

## Bauen und testen

JDK 17 oder 21, Android SDK Platform 35 und Build Tools 35.0.0 bereitstellen.
SDK in Android Studio oder per ignorierter `local.properties` konfigurieren.
Der echte Gradle-Wrapper 8.11.1 inklusive Distributionsprüfsumme liegt bei.

```sh
./gradlew :core:test :app:lintDebug :app:assembleDebug
```

Unter Windows `gradlew.bat` verwenden. Ergebnis:
`app/build/outputs/apk/debug/app-debug.apk`.

Das Projekt verwendet Java 17, AGP 8.9.2, CameraX 1.4.2 und Activity 1.9.3.
Die reine Java-Bibliothek `core` enthält Erkennung, Geometrie, Messlogik und
Pufferkonvertierung. Die Android-App verwendet native Views und CameraX.

Prüfstand und offene Geräteprüfungen: [VALIDATION.md](docs/VALIDATION.md).

## Quellen

- [CameraX Image analysis](https://developer.android.com/media/camera/camerax/analyze)
- [FDA: Illuminating Facts About Laser Pointers](https://www.fda.gov/radiation-emitting-products/alerts-and-notices/illuminating-facts-about-laser-pointers)
