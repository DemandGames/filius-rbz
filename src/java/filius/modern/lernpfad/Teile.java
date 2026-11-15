package filius.modern.lernpfad;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import filius.hardware.knoten.Gateway;
import filius.hardware.knoten.Knoten;
import filius.hardware.knoten.Notebook;
import filius.hardware.knoten.Rechner;
import filius.hardware.knoten.Switch;
import filius.hardware.knoten.Vermittlungsrechner;
import filius.modern.Texts;

/**
 * Building blocks for the {@link Schritt steps} of the check rules that come up again and again: a device
 * exists, has an address, a gateway, a program; a command was typed, a ping was answered. Each block brings
 * its text in German and English, so the hints of the tasks read the same everywhere.
 */
final class Teile {

    static final String MASKE = "255.255.255.0";

    private Teile() {}

    /** a device of the course: how it is found in the network and what it is called in German and English */
    static final class G {
        final Function<Netz, Knoten> finde;
        final String de;
        final String en;

        G(String de, String en, Function<Netz, Knoten> finde) {
            this.de = de;
            this.en = en;
            this.finde = finde;
        }

        Knoten in(Netz n) {
            return finde.apply(n);
        }

        String name() {
            return Texts.englisch() ? en : de;
        }
    }

    static G g(String de, String en, Function<Netz, Knoten> finde) {
        return new G(de, en, finde);
    }

    static G g(String name, Function<Netz, Knoten> finde) {
        return new G(name, name, finde);
    }

    /** German or English, as Filius runs */
    static String t(String de, String en) {
        return Texts.englisch() ? en : de;
    }

    static String oder(String wert, String de, String en) {
        return wert == null || wert.isBlank() ? t(de, en) : wert;
    }

    // ------------------------------------------------------------------ state

    /** the device exists (found by the name the task gives) and is of the right kind */
    static Schritt da(G g, Class<? extends Knoten> art) {
        String de = art == Rechner.class ? "PC" : art == Notebook.class ? "Laptop" : art == Switch.class ? "Switch"
                : art == Vermittlungsrechner.class ? "Vermittlungsrechner" : art == Gateway.class ? "Heimrouter" : "Gerät";
        String en = art == Rechner.class ? "computer" : art == Notebook.class ? "laptop" : art == Switch.class ? "switch"
                : art == Vermittlungsrechner.class ? "router" : art == Gateway.class ? "home router" : "device";
        return Schritt.z(de + " mit dem Namen „" + g.de + "“ aufstellen", "Add a " + en + " named “" + g.en + "”", n -> art.isInstance(g.in(n)));
    }

    /** fixed address and the net mask 255.255.255.0 (no DHCP) */
    static Schritt adresse(G g, String ip) {
        return Schritt.z(g.de + ": IPv4-Adresse " + ip + " und Netzmaske " + MASKE + " eintragen",
                g.en + ": enter the IPv4 address " + ip + " and the netmask " + MASKE, n -> Netz.fest(g.in(n), ip, MASKE)).ist(n -> {
                    Knoten k = g.in(n);
                    if (k == null) {
                        return null;
                    }
                    return oder(Netz.ip(k), "keine Adresse", "no address") + " / " + oder(Netz.maske(k), "keine Maske", "no mask")
                            + (Netz.dhcp(k) ? t(", DHCP ist eingeschaltet", ", DHCP is switched on") : "");
                });
    }

    static Schritt gateway(G g, String gw) {
        return Schritt.z(g.de + ": Gateway " + gw + " eintragen", g.en + ": enter the gateway " + gw, n -> g.in(n) != null && gw.equals(Netz.gateway(g.in(n))))
                .ist(n -> g.in(n) == null ? null : oder(Netz.gateway(g.in(n)), "leer", "empty"));
    }

    static Schritt dns(G g, String dns) {
        return Schritt.z(g.de + ": DNS-Server " + dns + " eintragen", g.en + ": enter the DNS server " + dns, n -> g.in(n) != null && dns.equals(Netz.dns(g.in(n))))
                .ist(n -> g.in(n) == null ? null : oder(Netz.dns(g.in(n)), "leer", "empty"));
    }

    /** the device has this name (German or English) */
    static Schritt name(String de, String en, G g, String name) {
        return Schritt.z(de, en, n -> Netz.heisst(g.in(n), name)).ist(n -> g.in(n) == null ? null : "„" + g.in(n).getName() + "“");
    }

    /** the program is installed on the device */
    static Schritt installiert(G g, String klasse) {
        return new Texte(g, klasse, "{g}: in der Simulation das Programm „{p}“ installieren (Klick auf das Gerät, dann „Software-Installation“)",
                "{g}: install the program “{p}” in the simulation (click the device, then “Software installation”)").z(n -> Netz.hat(g.in(n), klasse));
    }

    /** the server program is installed and started */
    static Schritt laeuft(G g, String klasse) {
        return new Texte(g, klasse, "{g}: das Programm „{p}“ in der Simulation öffnen und starten",
                "{g}: open the program “{p}” in the simulation and start it").z(n -> Netz.laeuft(g.in(n), klasse));
    }

    /** the device takes its settings from a DHCP server */
    static Schritt dhcpAn(G k) {
        return Schritt.z(k.de + ": das Häkchen „DHCP zur Konfiguration verwenden“ setzen", k.en + ": set the tick “Use DHCP for configuration”", n -> Netz.dhcp(k.in(n)));
    }

    /** the host is plugged into the switch */
    static Schritt anSwitch(G host, String swDe, String swEn, Function<Netz, Knoten> sw) {
        return Schritt.z(host.de + " mit einem Kabel an den " + swDe + " anschließen", "Connect " + host.en + " to the " + swEn + " with a cable",
                n -> Regeln.amSwitch(n, host.in(n), sw.apply(n)));
    }

    /** a structure field lies around the switch of the network */
    static Schritt flaeche(String netzDe, String netzEn, Function<Netz, Knoten> sw) {
        return Schritt.z("Im Dokumentationsmodus eine Gliederungsfläche um das " + netzDe + " legen (sein Switch muss in der Fläche liegen)",
                "In the documentation mode, put a structure field around the " + netzEn + " (its switch must lie inside the field)",
                n -> Regeln.bereichFeld(sw.apply(n)) != null);
    }

    /** the label of the field of that network (or a text field in it) fits */
    static Schritt mitEtikett(String de, String en, Function<Netz, Knoten> sw, Predicate<String> passt) {
        return Schritt.z(de, en, n -> Regeln.etikett(sw.apply(n), passt)).ist(n -> Regeln.etikettText(sw.apply(n)));
    }

    /** the heading of the start page of a web server is the student's own */
    static Schritt eigeneUeberschrift(G server) {
        return Schritt.z(server.de + ": mit dem Text-Editor in `webserver/index.html` den Text zwischen `<h1>` und `</h1>` durch eine eigene Überschrift ersetzen und speichern",
                server.en + ": with the text editor, replace the text between `<h1>` and `</h1>` in `webserver/index.html` by a heading of your own and save",
                n -> {
                    String h1 = Netz.h1(Netz.datei(server.in(n), "webserver/index.html"));
                    return !h1.isEmpty() && !Regeln.startseitenUeberschrift(h1);
                }).ist(n -> server.in(n) == null ? null : "„" + Netz.h1(Netz.datei(server.in(n), "webserver/index.html")) + "“");
    }

    // ------------------------------------------------------------------ evidence

    /** a command was typed into the command line of the device */
    static Schritt tippt(G g, String anzeige, String... namen) {
        return Schritt.n(g.de + ": in der Befehlszeile `" + anzeige + "` eingeben", g.en + ": type `" + anzeige + "` in the command line",
                (n, e, s) -> Regeln.befehl(e, s, g.in(n), namen));
    }

    /** the device got an answer to a ping to that address */
    static Schritt pingt(G g, String ziel) {
        return Schritt.n(g.de + ": in der Befehlszeile `ping " + ziel + "` eingeben – es müssen Antworten kommen",
                g.en + ": type `ping " + ziel + "` in the command line – replies must come back", (n, e, s) -> Regeln.pingOk(e, s, g.in(n), ziel));
    }

    /** the data exchange window of the device was open */
    static Schritt austausch(G g) {
        return Schritt.n("Datenaustausch von " + g.de + " öffnen (in der Simulation: Rechtsklick auf das Gerät, „Datenaustausch anzeigen“)",
                "Open the data exchange of " + g.en + " (in the simulation: right-click the device, “Show data exchange”)",
                (n, e, s) -> Regeln.fenster(e, s, g.in(n)));
    }

    /** texts with the name of a device {g} and of a program {p}, resolved in the language Filius runs in */
    private static final class Texte {
        final G g;
        final String klasse;
        final String de;
        final String en;

        Texte(G g, String klasse, String de, String en) {
            this.g = g;
            this.klasse = klasse;
            this.de = de;
            this.en = en;
        }

        Schritt z(Regel.Zustand z) {
            // the program name depends on the language at the time the text is shown, the device name is fixed per language
            return Schritt.z(de.replace("{g}", g.de).replace("{p}", name(false)), en.replace("{g}", g.en).replace("{p}", name(true)), z);
        }

        private String name(boolean englisch) {
            List<String> gleich = List.of(Netz.WEBBROWSER, Netz.WEBSERVER);
            if (gleich.contains(klasse)) {
                return klasse.equals(Netz.WEBBROWSER) ? "Webbrowser" : "Webserver";
            }
            switch (klasse) {
            case Netz.TERMINAL:
                return englisch ? "Command Line" : "Befehlszeile";
            case Netz.TEXTEDITOR:
                return englisch ? "Text editor" : "Text-Editor";
            case Netz.DNSSERVER:
                return englisch ? "DNS server" : "DNS-Server";
            case Netz.EMAILSERVER:
                return englisch ? "Email server" : "E-Mail-Server";
            case Netz.EMAILPROGRAMM:
                return englisch ? "Email program" : "E-Mail-Programm";
            case Netz.VPNSERVER:
                return englisch ? "VPN server" : "VPN-Server";
            case Netz.VPNCLIENT:
                return englisch ? "VPN client" : "VPN-Client";
            default:
                return klasse;
            }
        }
    }
}
