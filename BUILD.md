# Filius (RBZ Wirtschaft Kiel Version) – Aufbau und Bauen

Filius (RBZ Wirtschaft Kiel Version) baut auf einem **unveränderten Filius 2.9.3** auf. Das originale
`filius.jar` wird mitgeliefert; `filius-rbz.jar` steht im Klassenpfad davor (Manifest:
`Class-Path: lib/flatlaf-3.7.2.jar filius.jar`) und

1. installiert ein eigenes Design auf Grundlage von FlatLaf (hell und dunkel, eigene Schriften und Symbole),
2. ersetzt einzelne Klassen von Filius durch angepasste Fassungen (Basis: Filius 2.9.3, GPL; die geänderten
   Dateien tragen einen Hinweis im Dateikopf),
3. ergänzt neue Teile: Lernpfad mit Prüfregeln, HTTPS/TLS, VPN, E-Mail mit IMAP, Browser-Engine (WebKit),
   Text-Editor, Paketeffekte.

## Ordner

| Pfad | Inhalt |
|---|---|
| `src/java/filius/modern/` | neue Klassen: Start (`ModernFilius`), Design (`Theme`, `DunkelModus`), Kopfleiste, Effekte, Editor, Browser-Engine (`web/`), TLS (`tls/`), Lernpfad (`lernpfad/`) |
| `src/java/filius/…` (übrige Pakete) | angepasste Klassen aus Filius 2.9.3 (Oberfläche, Programme, einige Teile der Simulation) und neue Programme (VPN, IMAP) |
| `src/fx/` | Anzeige mit JavaFX WebView; wird als `lib/javafx/filius-rbz-fx.jar` ausgeliefert |
| `src/resources/` | Texte (de/en), Theme, Schriften, Grafiken, Startseite neuer Webserver |
| `dist-files/` | Programmdateien neben dem Jar: Starter, Hilfe, Beispiele, LIESMICH/README |
| `lernpfad/` | Lernpfade: Fortgeschritten (`lernpfad/`) und Standard (`lernpfad/standard/`), je Deutsch und Englisch (`en/`) mit Bildern und Startaufbauten |
| `design/` | Quellen der Grafiken (Python): Symbole, Hintergründe, Logo, Hilfeseiten |
| `third_party/filius-2.9.3/` | unveränderte Programmdateien von Filius 2.9.3 mit `SHA256SUMS` |
| `installer/` | Windows-Installer (NSIS), macOS-App, Debian-Paket |
| `tools/` | Laden der Abhängigkeiten (mit Prüfsummen), Java-Laufzeiten (jlink), Starttest |
| `out/` | Ergebnis (nicht im Repository) |

## Bauen

Alles läuft auch automatisch auf GitHub (`.github/workflows/build.yml`). Von Hand unter Linux
(Ubuntu/Debian; JDK 21, Python 3 mit Pillow, `zip`, `curl`, für die Windows-Installer `makensis` aus dem
Paket `nsis`, für das Linux-Paket `dpkg-deb`):

```
tools/deps.sh                   # FlatLaf, OpenJFX, Temurin und Liberica laden, SHA-256 prüfen (.deps/)
tools/runtimes.sh               # Java-Laufzeiten mit jlink: Windows, Linux, macOS (Apple-Chip, Intel)
./package.sh                    # baut filius-rbz.jar und den Programmordner out/Filius-RBZ-Wirtschaft-Kiel
installer/build-setup.sh        # Windows: Starter „Filius RBZ.exe“, Filius-RBZ-Setup.exe, …-AlleBenutzer.exe
installer/build-mac.sh          # macOS: Filius-RBZ-2.9.3-macOS-AppleSilicon.zip und …-Intel.zip
installer/build-deb.sh          # Linux: filius-rbz_2.9.3+rbz<Stand>_amd64.deb
tools/starttest.sh              # startet das Linux-Paket auf einem virtuellen Bildschirm
```

Hilfeseiten neu erzeugen (Python 3, Pillow): `cd design && python3 help.py ../dist-files`
Grafiken neu rendern (Python 3, Playwright mit Chromium, Pillow): `cd design && python3 render.py`

## Hinweise zur Technik

* Klassen-Overrides funktionieren, weil `filius-rbz.jar` vor `filius.jar` im Klassenpfad steht.
* Filius baut Teile der Oberfläche außerhalb des Swing-Threads auf. `SafeTextUIs` stellt deshalb
  für HTML-Felder das ursprüngliche Swing-Verhalten wieder her.
* WebKit muss `java.net.URLConnection` nutzen (`-Dcom.sun.webkit.useHTTP2Loader=false`, setzt
  `FxSupport.prepareJvm`), damit alle Seitenaufrufe über das simulierte Netz laufen.
* Einstellungen für Lehrkräfte in `config/filius.ini`, z. B. `modern-lernpfad-auswahl=standard|fortgeschritten|beide`,
  `modern-schichtmodell=osi`, `modern-browser=klassisch`, `modern-speicherort=…`, `locale=de_DE`.

Lizenz: GPL v2 oder v3 (wie Filius). FlatLaf: Apache 2.0. Schriften: SIL OFL 1.1. OpenJFX und
Java-Laufzeit: GPL v2 mit Classpath Exception.
