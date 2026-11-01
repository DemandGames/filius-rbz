package filius.modern.lernpfad;

import static filius.modern.lernpfad.Ereignis.Art.BEFEHL;
import static filius.modern.lernpfad.Ereignis.Art.DATENAUSTAUSCH;
import static filius.modern.lernpfad.Ereignis.Art.DHCP;
import static filius.modern.lernpfad.Ereignis.Art.DNS;
import static filius.modern.lernpfad.Ereignis.Art.DNS_ANFRAGE;
import static filius.modern.lernpfad.Ereignis.Art.HTTP;
import static filius.modern.lernpfad.Ereignis.Art.HTTP_ANFRAGE;
import static filius.modern.lernpfad.Ereignis.Art.NAT;
import static filius.modern.lernpfad.Ereignis.Art.PING_ANTWORT;
import static filius.modern.lernpfad.Ereignis.Art.PING_EMPFANGEN;
import static filius.modern.lernpfad.Ereignis.Art.PING_OHNE_ANTWORT;
import static filius.modern.lernpfad.Ereignis.Art.IMAP;
import static filius.modern.lernpfad.Ereignis.Art.SAT;
import static filius.modern.lernpfad.Ereignis.Art.SMTP;
import static filius.modern.lernpfad.Ereignis.Art.SYN_OHNE_ANTWORT;
import static filius.modern.lernpfad.Ereignis.Art.TCP_VERBUNDEN;
import static filius.modern.lernpfad.Ereignis.Art.TLS;
import static filius.modern.lernpfad.Ereignis.Art.VPN;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import filius.gui.GUIContainer;
import filius.gui.netzwerksicht.GUIDocuItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.Gateway;
import filius.hardware.knoten.Knoten;
import filius.hardware.knoten.Notebook;
import filius.hardware.knoten.Rechner;
import filius.hardware.knoten.Switch;
import filius.hardware.knoten.Vermittlungsrechner;
import filius.software.Anwendung;
import filius.software.dhcp.DHCPServer;
import filius.software.email.EmailAnwendung;
import filius.software.email.EmailKonto;
import filius.software.email.EmailServer;
import filius.software.firewall.Firewall;
import filius.software.firewall.FirewallRule;
import filius.software.rip.RIPRoute;
import filius.software.rip.RIPTable;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.system.SwitchFirmware;

/** The check rules of all practice tasks, by task id ("1.1", ..., "A1", "B1", "C1"). */
public final class Regeln {

    // ------------------------------------------------------------------ addresses of the course

    static final String MASKE = "255.255.255.0";
    static final String SCHULNETZ = "192.168.10.0";
    static final String PROVIDERNETZ = "203.0.113.0";
    static final String RGGNETZ = "198.51.100.0";
    static final String HEIMNETZ = "192.168.178.0";
    static final String R1_SCHULE = "192.168.10.1";
    static final String R1_PROVIDER = "203.0.113.1";
    static final String R2_PROVIDER = "203.0.113.2";
    static final String R2_RGG = "198.51.100.1";
    static final String HEIM_WAN = "203.0.113.3";
    static final String HEIM_LAN = "192.168.178.1";
    static final String WEBSERVER = "192.168.10.10";
    static final String SHOP = "198.51.100.80";
    static final String BUERO = "198.51.100.30";
    static final String DNS_SERVER = "203.0.113.53";
    static final String DHCP_SERVER = "192.168.10.2";
    static final String MAIL_SCHULE = "192.168.10.20";
    static final String MAIL_RGG = "198.51.100.25";
    static final String BLOG = "192.168.178.10";
    static final String TABLET_IP = "192.168.178.50";
    static final String WWW_SCHULE = "www.rbz-wirtschaft-kiel.de";
    static final String WWW_SHOP = "www.ravensberger24.de";
    static final String DOMAIN_SCHULE = "rbz-wirtschaft-kiel.de";
    static final String DOMAIN_RGG = "ravensberger24.de";

    private static final Map<String, Regel> REGELN = new HashMap<>();
    private static List<String> startseitenUeberschriften;

    private Regeln() {}

    public static Regel regel(String id) {
        if (REGELN.isEmpty()) {
            kapitel1();
            kapitel2();
            kapitel3();
            kapitel4();
            kapitel5();
            kapitel6();
            kapitel7();
            kapitel8();
            kapitel9();
            kapitel10();
            kapitel11();
            kapitel12();
        }
        return REGELN.get(id);
    }

    private static void def(String id, Regel r) {
        REGELN.put(id, r);
    }

    // ------------------------------------------------------------------ devices of the course

    static Knoten laptop(Netz n, int i) {
        Knoten k = n.knoten("Laptop-" + i);
        if (k == null && i == 4) {
            // the teacher laptop of chapter 7 may be renamed as in the standard path (version 31)
            k = n.knoten("Lehrer-Laptop");
        }
        if (k == null) {
            Knoten byIp = n.mitIp("192.168.10.1" + i);
            if (byIp instanceof Notebook) {
                k = byIp;
            }
        }
        return k;
    }

    static List<Knoten> schulLaptops(Netz n) {
        List<Knoten> l = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Knoten k = laptop(n, i);
            if (k != null) {
                l.add(k);
            }
        }
        return l;
    }

    static Knoten pc(Netz n, String name, String ip) {
        Knoten k = n.knoten(name);
        if (k instanceof Rechner) {
            return k;
        }
        Knoten byIp = n.mitIp(ip);
        return byIp instanceof Rechner ? byIp : k;
    }

    static Knoten webserver(Netz n) {
        return pc(n, "Webserver", WEBSERVER);
    }

    static Knoten shop(Netz n) {
        return pc(n, "Shop", SHOP);
    }

    static Knoten buero(Netz n) {
        Knoten k = n.knoten("Büro-Laptop");
        if (k == null) {
            k = n.knoten("Buero-Laptop");
        }
        if (k == null) {
            Knoten byIp = n.mitIp(BUERO);
            k = byIp instanceof Notebook ? byIp : null;
        }
        return k;
    }

    static Knoten heimLaptop(Netz n) {
        Knoten k = n.knoten("Heim-Laptop");
        return k != null ? k : n.knoten("Laptop");
    }

    static Knoten tablet(Netz n) {
        return n.knoten("Tablet");
    }

    static Knoten heimrouter(Netz n) {
        for (Knoten k : n.heimrouter()) {
            Gateway g = (Gateway) k;
            if (HEIM_WAN.equals(g.holeWANInterface().getIp())) {
                return k;
            }
        }
        List<Knoten> alle = n.heimrouter();
        return alle.isEmpty() ? null : alle.get(0);
    }

    static Knoten switchSchule(Netz n) {
        Knoten k = n.knoten("Switch Schule");
        if (k instanceof Switch) {
            return k;
        }
        Knoten l1 = laptop(n, 1);
        return l1 == null ? null : n.switchAn(l1);
    }

    static Knoten switchMit(Netz n, String teil) {
        for (Knoten k : n.switches()) {
            if (Netz.normal(k.getName()).contains(Netz.normal(teil))) {
                return k;
            }
        }
        return null;
    }

    /** Router (Vermittlungsrechner) with a card that has this address. */
    static Knoten routerMit(Netz n, String ip) {
        Knoten k = n.mitIp(ip);
        return k instanceof Vermittlungsrechner ? k : null;
    }

    static Knoten r1(Netz n) {
        return routerMit(n, R1_SCHULE);
    }

    static Knoten r2(Netz n) {
        return routerMit(n, R2_PROVIDER);
    }

    static Knoten dnsServer(Netz n) {
        return pc(n, "DNS-Server", DNS_SERVER);
    }

    static Knoten dhcpServerPc(Netz n) {
        return pc(n, "DHCP-Server", DHCP_SERVER);
    }

    static Knoten mailSchule(Netz n) {
        return pc(n, "Mailserver Schule", MAIL_SCHULE);
    }

    static Knoten mailRgg(Netz n) {
        return pc(n, "Mailserver RGG", MAIL_RGG);
    }

    static Knoten blog(Netz n) {
        return pc(n, "Blog-Server", BLOG);
    }

    /** card of k with this address that is plugged into the given switch */
    static boolean karteAn(Netz n, Knoten k, String ip, String maske, Knoten sw) {
        NetzwerkInterface nic = Netz.nic(k, ip);
        return nic != null && maske.equals(nic.getSubnetzMaske()) && sw != null && n.gegenueber(nic) == sw;
    }

    static boolean amSwitch(Netz n, Knoten host, Knoten sw) {
        return host != null && sw != null && n.switchAn(host) == sw;
    }

    /** host is connected to the switch by Wi-Fi (not by cable) */
    static boolean imWlan(Netz n, Knoten host, Knoten sw) {
        return amSwitch(n, host, sw) && Netz.wlan(host) && Netz.perWlan(host);
    }

    static boolean inNetz(String ip, String netz) {
        return Netz.gleichesNetz(ip, netz, MASKE);
    }

    static boolean schulGeraet(Netz n, Knoten k) {
        return k != null && inNetz(Netz.ip(k), SCHULNETZ);
    }

    static boolean heimGeraet(Knoten k) {
        return k != null && inNetz(Netz.ip(k), HEIMNETZ);
    }

    // ------------------------------------------------------------------ event helpers

    static boolean befehl(Ereignisse e, long seit, Knoten k, String... namen) {
        return k != null && e.gibt(BEFEHL, seit, x -> x.knoten == k && List.of(namen).contains(x.name));
    }

    static Ereignis befehlMit(Ereignisse e, long seit, Knoten k, String parameter, String... namen) {
        return k == null ? null : e.letztes(BEFEHL, seit, x -> x.knoten == k && List.of(namen).contains(x.name) && x.werte.contains(parameter));
    }

    static boolean fenster(Ereignisse e, long seit, Knoten k) {
        return k != null && e.gibt(DATENAUSTAUSCH, seit, x -> x.knoten == k);
    }

    static boolean pingOk(Ereignisse e, long seit, Knoten von, String ziel) {
        return von != null && e.gibt(PING_ANTWORT, seit, x -> x.knoten == von && x.ziel.equals(ziel));
    }

    static boolean http(Ereignisse e, long seit, Knoten client, String server, int port, java.util.function.Predicate<Ereignis> f) {
        return client != null && e.gibt(HTTP, seit, x -> x.knoten == client && x.ziel.equals(server) && x.port == port && x.status == 200 && (f == null || f.test(x)));
    }

    static boolean seiteMit(Ereignis x, String text) {
        return x.inhalt.toString().toLowerCase(Locale.GERMAN).contains(text.toLowerCase(Locale.GERMAN));
    }

    static boolean startseite(Ereignis x) {
        return x.pfad.equals("/") || x.pfad.equalsIgnoreCase("/index.html") || x.pfad.isEmpty();
    }

    static boolean dnsAntwort(Ereignisse e, long seit, Knoten k, String name, String wert) {
        return k != null && e.gibt(DNS, seit, x -> x.knoten == k && x.werte.stream().anyMatch(w -> w.startsWith(name + " ") && (wert == null || w.endsWith(" " + wert))));
    }

    static boolean dhcpAck(Ereignisse e, long seit, Knoten k) {
        String mac = Netz.mac(k);
        return k != null && e.gibt(DHCP, seit, x -> x.name.equals("DHCPACK") && x.werte.size() > 0 && x.werte.get(0).equalsIgnoreCase(mac));
    }

    static boolean smtp(Ereignisse e, long seit, String server, String client, String von, String an) {
        return e.gibt(SMTP, seit, x -> x.ziel.equals(server) && (client == null || x.quelle.equals(client)) && (von == null || x.werte.size() > 0 && x.werte.get(0).equals(von))
                && (an == null || x.werte.subList(1, x.werte.size()).contains(an)));
    }

    static Ereignis smtpEreignis(Ereignisse e, long seit, String server, String client, String von, String an) {
        return e.letztes(SMTP, seit, x -> x.ziel.equals(server) && (client == null || x.quelle.equals(client)) && (von == null || x.werte.size() > 0 && x.werte.get(0).equals(von))
                && (an == null || x.werte.subList(1, x.werte.size()).contains(an)));
    }

    static String nurZiffern(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    static List<String> zahlen(String s) {
        List<String> l = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(s);
        while (m.find()) {
            l.add(m.group().replaceFirst("^0+(?=\\d)", ""));
        }
        return l;
    }

    static String mac(String s) {
        return s.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
    }

    static boolean ipAntwort(String antwort, String erwartet) {
        List<String> a = Netz.adressen(antwort);
        return a.size() == 1 && a.get(0).equals(erwartet);
    }

    /** true if the heading is still the one of the start page of a new web server (German or English) */
    static boolean startseitenUeberschrift(String h1) {
        if (startseitenUeberschriften == null) {
            List<String> l = new ArrayList<>();
            for (String datei : new String[] { "index.html", "index_en.html" }) {
                try (java.io.InputStream in = Regeln.class.getResourceAsStream("/filius/modern/web/startseite/" + datei)) {
                    if (in != null) {
                        l.add(Netz.h1(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).trim().toLowerCase(Locale.ROOT));
                    }
                } catch (Exception e) {
                    // not known
                }
            }
            startseitenUeberschriften = l;
        }
        return startseitenUeberschriften.contains(h1.trim().toLowerCase(Locale.ROOT));
    }

    // ================================================================== chapter 1

    private static void kapitel1() {
        def("1.1", Regel.zustand(n -> laptop(n, 1) instanceof Notebook && n.knoten("Laptop-2") instanceof Notebook));
        def("1.2", Regel.zustand(n -> n.verbunden(laptop(n, 1), n.knoten("Laptop-2"))));
        def("1.3", Regel.zustand(n -> Netz.fest(n.knoten("Laptop-1"), "192.168.10.11", MASKE) && Netz.fest(n.knoten("Laptop-2"), "192.168.10.12", MASKE)));
        def("1.4", Regel.zustand(n -> Netz.hat(laptop(n, 1), Netz.TERMINAL)));
        def("1.5", Regel.nachweis((n, e, s) -> befehl(e, s, laptop(n, 1), "ipconfig")));
        def("1.6", Regel.zustand(n -> laptop(n, 1) != null && laptop(n, 2) != null && Netz.ip(laptop(n, 2)).equals("192.168.10.12"))
                .und((n, e, s) -> pingOk(e, s, laptop(n, 1), "192.168.10.12")));
        def("1.7", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 1)) && e.gibt(PING_ANTWORT, s, x -> x.knoten == laptop(n, 1))));
    }

    // ================================================================== chapter 2

    private static void kapitel2() {
        def("2.1", Regel.zustand(n -> n.knoten("Switch Schule") instanceof Switch && laptop(n, 1) != null && laptop(n, 2) != null
                && !n.verbunden(laptop(n, 1), laptop(n, 2))));
        def("2.2", Regel.zustand(n -> {
            Knoten sw = n.knoten("Switch Schule");
            return sw instanceof Switch && amSwitch(n, laptop(n, 1), sw) && amSwitch(n, laptop(n, 2), sw);
        }));
        def("2.3", Regel.zustand(n -> {
            Knoten sw = n.knoten("Switch Schule");
            Knoten l3 = n.knoten("Laptop-3");
            Knoten l4 = n.knoten("Laptop-4");
            return sw instanceof Switch && l3 instanceof Notebook && l4 instanceof Notebook && Netz.fest(l3, "192.168.10.13", MASKE)
                    && Netz.fest(l4, "192.168.10.14", MASKE) && amSwitch(n, l3, sw) && amSwitch(n, l4, sw);
        }));
        def("2.4", Regel.zustand(n -> amSwitch(n, laptop(n, 1), switchSchule(n)) && amSwitch(n, laptop(n, 4), switchSchule(n)))
                .und((n, e, s) -> pingOk(e, s, laptop(n, 1), "192.168.10.14")));
        def("2.5", Regel.nachweis((n, e, s) -> befehl(e, s, laptop(n, 1), "arp"))
                .antwort((n, e, a) -> laptop(n, 4) != null && mac(a).equals(mac(Netz.mac(laptop(n, 4))))));
        def("2.6", Regel.nachweis((n, e, s) -> e.gibt(SAT, s, x -> x.knoten == switchSchule(n))).antwort((n, e, a) -> {
            Knoten sw = switchSchule(n);
            Knoten l4 = laptop(n, 4);
            if (!(sw instanceof Switch) || l4 == null) {
                return false;
            }
            String erwartet = satAnschluss((Switch) sw, Netz.mac(l4));
            if (erwartet == null) {
                erwartet = String.valueOf(n.switchAnschluss((Switch) sw, l4));
            }
            List<String> z = zahlen(a);
            return z.size() == 1 && z.get(0).equals(erwartet);
        }));
        def("2.7", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 1))));
        def("2.8", Regel.zustand(n -> etikett(switchSchule(n), mitNetz(SCHULNETZ + "/24"))));
    }

    /** port number the switch table shows for that MAC, or null */
    static String satAnschluss(Switch sw, String mac) {
        try {
            Vector<Vector<String>> sat = ((SwitchFirmware) sw.getSystemSoftware()).holeSAT();
            for (Vector<String> zeile : sat) {
                if (zeile.size() > 1 && mac(zeile.get(0)).equals(mac(mac))) {
                    List<String> z = zahlen(zeile.get(1));
                    return z.isEmpty() ? null : z.get(0);
                }
            }
        } catch (RuntimeException e) {
            // no table
        }
        return null;
    }

    // ================================================================== chapter 3

    private static void kapitel3() {
        def("3.1", Regel.zustand(n -> {
            Knoten ws = n.knoten("Webserver");
            return ws instanceof Rechner && Netz.fest(ws, WEBSERVER, MASKE) && amSwitch(n, ws, switchSchule(n));
        }));
        def("3.2", Regel.zustand(n -> Netz.laeuft(webserver(n), Netz.WEBSERVER)));
        def("3.3", Regel.zustand(n -> Netz.hat(laptop(n, 2), Netz.WEBBROWSER))
                .und((n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, 80, Regeln::startseite)));
        def("3.4", Regel.zustand(n -> {
            String h1 = Netz.h1(Netz.datei(webserver(n), "webserver/index.html"));
            return !h1.isEmpty() && !startseitenUeberschrift(h1);
        }));
        def("3.5", Regel.zustand(n -> {
            String h1 = Netz.h1(Netz.datei(webserver(n), "webserver/index.html"));
            return !h1.isEmpty() && !startseitenUeberschrift(h1);
        }).und((n, e, s) -> {
            String h1 = Netz.h1(Netz.datei(webserver(n), "webserver/index.html"));
            return http(e, s, laptop(n, 2), WEBSERVER, 80, x -> startseite(x) && Netz.h1(x.inhalt.toString()).equals(h1));
        }));
        def("3.6", Regel.zustand(n -> {
            Knoten ws = webserver(n);
            String kontakt = Netz.datei(ws, "webserver/kontakt.html");
            String index = Netz.datei(ws, "webserver/index.html");
            return kontakt != null && !Netz.h1(kontakt).isEmpty() && Netz.text(kontakt).length() > Netz.h1(kontakt).length() + 10
                    && index != null && index.toLowerCase(Locale.ROOT).matches("(?s).*href\\s*=\\s*[\"']?(\\./|/)?kontakt\\.html.*");
        }).und((n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, 80, x -> x.pfad.toLowerCase(Locale.ROOT).endsWith("kontakt.html"))));
        def("3.7", Regel.nachweis((n, e, s) -> laptop(n, 2) != null
                && e.gibt(HTTP, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER) && x.status == 404 && x.pfad.contains("gibtsnicht")))
                .antwort((n, e, a) -> zahlen(a).equals(List.of("404"))));
        def("3.8", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 2))
                && e.gibt(TCP_VERBUNDEN, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER))));
    }

    // ================================================================== chapter 4 (HTTPS)

    /** the web server program of the school web server */
    static filius.software.www.WebServer webserverProgramm(Netz n) {
        Anwendung a = Netz.software(webserver(n), Netz.WEBSERVER);
        return a instanceof filius.software.www.WebServer ? (filius.software.www.WebServer) a : null;
    }

    /** HTTPS is on, signed by the certificate authority, and the certificate names all these names */
    static boolean httpsOk(Netz n, String... namen) {
        filius.software.www.WebServer w = webserverProgramm(n);
        if (w == null || !w.isHttps() || w.isSelbstSigniert() || !w.isAktiv()) {
            return false;
        }
        // only the names entered at "Zertifikat für" count (an empty field is no certificate for the server)
        List<String> zert = filius.modern.tls.Tls.namen(w.getZertifikatNamen(), null);
        for (String name : namen) {
            if (!zert.contains(name.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    /** a sign-in page with the right name and password was answered with the internal area */
    static boolean angemeldet(Ereignis x) {
        return x.pfad.toLowerCase(Locale.ROOT).endsWith("/anmelden") && (seiteMit(x, "Angemeldet als") || seiteMit(x, "Signed in as"));
    }

    /** a text field near the device whose text fits */
    static boolean textNeben(Knoten k, Predicate<String> passt) {
        Rectangle bild = null;
        for (GUIKnotenItem item : GUIContainer.getGUIContainer().getKnotenItems()) {
            if (item.getKnoten() == k) {
                bild = item.getImageLabel().getBounds();
            }
        }
        if (bild == null) {
            return false;
        }
        for (GUIDocuItem feld : textfelder()) {
            Rectangle f = new Rectangle(feld.getX(), feld.getY(), feld.getWidth(), feld.getHeight());
            double dx = Math.max(0, Math.max(f.x - (bild.x + bild.width), bild.x - (f.x + f.width)));
            double dy = Math.max(0, Math.max(f.y - (bild.y + bild.height), bild.y - (f.y + f.height)));
            if (Math.hypot(dx, dy) <= 150 && passt.test(feld.getText())) {
                return true;
            }
        }
        return false;
    }

    private static void kapitel4() {
        def("4.1", Regel.nachweis((n, e, s) -> http(e, s, laptop(n, 1), WEBSERVER, 80, Regeln::angemeldet)));
        def("4.2", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 1))));
        def("4.3", Regel.zustand(n -> httpsOk(n, WEBSERVER)));
        def("4.4", Regel.zustand(n -> httpsOk(n, WEBSERVER))
                .und((n, e, s) -> http(e, s, laptop(n, 1), WEBSERVER, filius.modern.tls.Tls.PORT, x -> x.pfad.toLowerCase(Locale.ROOT).startsWith("/intern"))));
        def("4.5", Regel.zustand(n -> httpsOk(n, WEBSERVER))
                .und((n, e, s) -> http(e, s, laptop(n, 1), WEBSERVER, filius.modern.tls.Tls.PORT, Regeln::angemeldet) && fenster(e, s, laptop(n, 1))));
        // the warning of the browser: Laptop-2 rejected the certificate with an alert
        def("4.6", Regel.nachweis((n, e, s) -> laptop(n, 2) != null
                && e.gibt(TLS, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER) && "Alert".equals(x.name) && "gesendet".equals(x.typ)))
                .uebergang());
        def("4.7", Regel.zustand(n -> httpsOk(n, WEBSERVER) && !webserverProgramm(n).isHttp())
                .und((n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, filius.modern.tls.Tls.PORT, null)));
        def("4.8", Regel.zustand(n -> textNeben(webserver(n), t -> t.toLowerCase(Locale.ROOT).contains("https") && t.contains("443"))));
    }

    // ================================================================== chapter 5

    private static boolean r1Ok(Netz n) {
        Knoten r1 = r1(n);
        Knoten prov = switchMit(n, "provider");
        return r1 != null && karteAn(n, r1, R1_SCHULE, MASKE, switchSchule(n)) && karteAn(n, r1, R1_PROVIDER, MASKE, prov);
    }

    private static boolean r2Ok(Netz n) {
        Knoten r2 = r2(n);
        Knoten prov = switchMit(n, "provider");
        Knoten rgg = switchMit(n, "rgg");
        return r2 != null && karteAn(n, r2, R2_PROVIDER, MASKE, prov) && karteAn(n, r2, R2_RGG, MASKE, rgg);
    }

    /** automatic routing (RIP) is switched on at this router */
    static boolean ripAn(Knoten router) {
        InternetKnotenBetriebssystem bs = router == null ? null : Netz.bs(router);
        return bs != null && bs.isRipEnabled();
    }

    private static boolean ripOk(Netz n) {
        return ripAn(r1(n)) && ripAn(r2(n));
    }

    /** hops of the learned route of a router to a network, -1 if it has none */
    static int ripHops(Knoten router, String netz) {
        InternetKnotenBetriebssystem bs = router == null ? null : Netz.bs(router);
        RIPTable t = bs == null || !bs.isRipEnabled() ? null : bs.getRIPTable();
        if (t == null) {
            return -1;
        }
        synchronized (t) {
            for (RIPRoute r : t.routes) {
                if (netz.equals(r.getNetAddress()) && r.hops < RIPTable.INFINITY) {
                    return r.hops;
                }
            }
        }
        return -1;
    }

    private static void kapitel5() {
        // the names given in the task are part of it (German or English)
        def("5.1", Regel.zustand(n -> r1Ok(n) && Netz.heisst(r1(n), "R1 Router Schule") && Netz.heisst(switchMit(n, "provider"), "Switch Provider")));
        def("5.2", Regel.zustand(n -> {
            int gezaehlt = 0;
            for (Knoten k : n.hosts()) {
                if (inNetz(Netz.ip(k), SCHULNETZ)) {
                    if (!R1_SCHULE.equals(Netz.gateway(k))) {
                        return false;
                    }
                    gezaehlt++;
                }
            }
            return gezaehlt >= 5 && R1_SCHULE.equals(Netz.gateway(webserver(n)));
        }));
        def("5.3", Regel.zustand(n -> {
            Knoten rgg = switchMit(n, "rgg");
            Knoten shop = shop(n);
            Knoten buero = buero(n);
            return r2Ok(n) && shop instanceof Rechner && buero instanceof Notebook && Netz.fest(shop, SHOP, MASKE) && Netz.fest(buero, BUERO, MASKE)
                    && R2_RGG.equals(Netz.gateway(shop)) && R2_RGG.equals(Netz.gateway(buero)) && amSwitch(n, shop, rgg) && amSwitch(n, buero, rgg)
                    && Netz.heisst(r2(n), "R2 Router RGG") && Netz.heisst(rgg, "Switch RGG") && Netz.heisst(shop, "Shop") && Netz.heisst(buero, "Büro-Laptop");
        }));
        def("5.4", Regel.zustand(n -> Netz.laeuft(shop(n), Netz.WEBSERVER)
                && Netz.h1(Netz.datei(shop(n), "webserver/index.html")).toLowerCase(Locale.ROOT).contains("ravensberger24")));
        def("5.5", Regel.nachweis((n, e, s) -> laptop(n, 1) != null && e.gibt(PING_OHNE_ANTWORT, s, x -> x.knoten == laptop(n, 1) && x.ziel.equals(SHOP)))
                .uebergang());
        def("5.6", Regel.zustand(Regeln::ripOk)
                .und((n, e, s) -> e.gibt(HTTP, s, x -> schulGeraet(n, x.knoten) && (x.ziel.equals(R1_SCHULE) || x.ziel.equals(R1_PROVIDER))
                        && x.pfad.startsWith("/routes") && x.status == 200))
                .antwort((n, e, a) -> {
                    int h = ripHops(r1(n), RGGNETZ);
                    return zahlen(a).equals(List.of(String.valueOf(h >= 0 ? h : 1)));
                }));
        def("5.7", Regel.zustand(Regeln::ripOk).und((n, e, s) -> pingOk(e, s, laptop(n, 1), SHOP)
                && http(e, s, laptop(n, 2), SHOP, 80, x -> seiteMit(x, "Ravensberger24"))));
        def("5.8", Regel.nachweis((n, e, s) -> befehlMit(e, s, laptop(n, 1), SHOP, "traceroute", "tracert") != null).antwort((n, e, a) -> {
            List<String> ip = Netz.adressen(a);
            return ip.contains(R1_SCHULE) && ip.contains(R2_PROVIDER) && ip.size() == 2;
        }));
        def("5.10", Regel.zustand(n -> etikett(switchMit(n, "provider"), mitNetz(PROVIDERNETZ + "/24"))
                && etikett(switchMit(n, "rgg"), mitNetz(RGGNETZ + "/24"))));
        def("5.9", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 1)) && fenster(e, s, shop(n))
                && e.gibt(PING_EMPFANGEN, s, x -> x.knoten == shop(n) && laptop(n, 1) != null && x.quelle.equals(Netz.ip(laptop(n, 1))))));
        // R1 sees only encrypted data of the sign-in from the RGG
        def("5.11", Regel.nachweis((n, e, s2) -> http(e, s2, buero(n), WEBSERVER, filius.modern.tls.Tls.PORT, Regeln::angemeldet) && fenster(e, s2, r1(n))));
    }

    // ================================================================== chapter 5

    private static void kapitel6() {
        def("6.1", Regel.zustand(n -> {
            Knoten d = n.knoten("DNS-Server");
            if (!(d instanceof Rechner)) {
                d = n.mitIp(DNS_SERVER);
            }
            return d instanceof Rechner && Netz.fest(d, DNS_SERVER, MASKE) && R1_PROVIDER.equals(Netz.gateway(d)) && amSwitch(n, d, switchMit(n, "provider"))
                    && Netz.hat(d, Netz.DNSSERVER) && Netz.heisst(d, "DNS-Server");
        }));
        def("6.2", Regel.zustand(n -> {
            Knoten d = dnsServer(n);
            return Netz.laeuft(d, Netz.DNSSERVER) && Netz.record(d, WWW_SCHULE, "A").contains(WEBSERVER) && Netz.record(d, WWW_SHOP, "A").contains(SHOP);
        }));
        def("6.3", Regel.zustand(n -> {
            List<Knoten> l = schulLaptops(n);
            if (l.size() < 4 || buero(n) == null) {
                return false;
            }
            for (Knoten k : l) {
                if (!DNS_SERVER.equals(Netz.dns(k))) {
                    return false;
                }
            }
            return DNS_SERVER.equals(Netz.dns(buero(n)));
        }));
        def("6.4", Regel.nachweis((n, e, s) -> befehl(e, s, laptop(n, 1), "nslookup") && dnsAntwort(e, s, laptop(n, 1), WWW_SHOP, SHOP)));
        def("6.5", Regel.nachweis((n, e, s) -> dnsAntwort(e, s, laptop(n, 2), WWW_SHOP, SHOP) && http(e, s, laptop(n, 2), SHOP, 80, x -> x.host.startsWith(WWW_SHOP))
                && dnsAntwort(e, s, buero(n), WWW_SCHULE, WEBSERVER)
                && e.gibt(TCP_VERBUNDEN, s, x -> x.knoten == buero(n) && x.ziel.equals(WEBSERVER) && x.port == filius.modern.tls.Tls.PORT)));
        def("6.6", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 2)) && e.gibt(DNS_ANFRAGE, s, x -> x.knoten == laptop(n, 2))));
        // the certificate of the school web server names the web site
        def("6.7", Regel.zustand(n -> httpsOk(n, WWW_SCHULE, DOMAIN_SCHULE))
                .und((n, e, s) -> http(e, s, buero(n), WEBSERVER, filius.modern.tls.Tls.PORT, x -> x.host.startsWith(WWW_SCHULE))));
        def("6.8", Regel.zustand(n -> Netz.record(dnsServer(n), DOMAIN_SCHULE, "A").contains(WEBSERVER))
                .und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER) && x.status == 200 && x.host.replaceFirst(":\\d+$", "").equals(DOMAIN_SCHULE))));
    }

    // ================================================================== chapter 6

    private static boolean dhcpSchuleOk(Netz n) {
        Knoten d = dhcpServerPc(n);
        if (!(d instanceof Rechner) || !Netz.fest(d, DHCP_SERVER, MASKE) || !n.segment(switchSchule(n)).contains(d)) {
            return false;
        }
        DHCPServer s = Netz.dhcpServer(d);
        if (s == null || !s.isAktiv()) {
            return false;
        }
        String von = s.getUntergrenze();
        String bis = s.getObergrenze();
        return inNetz(von, SCHULNETZ) && inNetz(bis, SCHULNETZ) && Netz.wert(von) >= Netz.wert("192.168.10.50") && Netz.wert(bis) <= Netz.wert("192.168.10.254")
                && Netz.wert(bis) - Netz.wert(von) + 1 >= 30 && R1_SCHULE.equals(s.determineGatewayip()) && DNS_SERVER.equals(s.determineDnsserverip())
                && MASKE.equals(s.getSubnetzmaske());
    }

    /** address a school laptop should have from the DHCP server: its static one or one from the range */
    private static boolean dhcpAdresseOk(Netz n, Knoten k) {
        Knoten d = dhcpServerPc(n);
        DHCPServer s = Netz.dhcpServer(d);
        if (s == null || k == null) {
            return false;
        }
        String ip = Netz.ip(k);
        String statisch = Netz.statischeZuweisung(d, Netz.mac(k));
        boolean adresse = statisch != null ? statisch.equals(ip) : Netz.imBereich(ip, s.getUntergrenze(), s.getObergrenze());
        return Netz.dhcp(k) && adresse && R1_SCHULE.equals(Netz.gateway(k)) && DNS_SERVER.equals(Netz.dns(k));
    }

    private static void kapitel7() {
        def("7.1", Regel.zustand(n -> dhcpSchuleOk(n) && Netz.heisst(dhcpServerPc(n), "DHCP-Server")));
        def("7.2", Regel.zustand(n -> {
            List<Knoten> l = schulLaptops(n);
            return l.size() == 4 && l.stream().allMatch(Netz::dhcp);
        }));
        def("7.3", Regel.zustand(n -> {
            List<Knoten> l = schulLaptops(n);
            return l.size() == 4 && dhcpSchuleOk(n) && l.stream().allMatch(k -> dhcpAdresseOk(n, k));
        }).und((n, e, s) -> schulLaptops(n).stream().allMatch(k -> dhcpAck(e, 0, k))
                && schulLaptops(n).stream().anyMatch(k -> befehl(e, s, k, "ipconfig"))));
        Regel.Nachweis doraGesehen = (n, e, s) -> schulLaptops(n).stream().anyMatch(k -> fenster(e, s, k))
                && e.gibt(DHCP, 0, x -> x.name.equals("DHCPDISCOVER"));
        def("7.4", Regel.nachweis(doraGesehen));
        def("7.5", Regel.nachweis(doraGesehen).antwort((n, e, a) -> ipAntwort(a, "0.0.0.0")));
        def("7.6", Regel.zustand(n -> {
            Knoten l4 = laptop(n, 4);
            String ip = Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(l4));
            if (l4 == null || ip == null || !Netz.imBereich(ip, "192.168.10.50", "192.168.10.254")) {
                return false;
            }
            for (Knoten k : n.knoten()) {
                if (k != l4 && Netz.nic(k, ip) != null) {
                    return false;
                }
            }
            return Netz.dhcp(l4) && ip.equals(Netz.ip(l4));
        }));
        def("7.7", Regel.nachweis((n, e, s) -> schulLaptops(n).stream().anyMatch(
                k -> Netz.dhcp(k) && dhcpAdresseOk(n, k) && http(e, s, k, SHOP, 80, x -> x.host.startsWith(WWW_SHOP)))));
        def("7.8", Regel.zustand(n -> {
            DHCPServer d = Netz.dhcpServer(dhcpServerPc(n));
            return d != null && Netz.gueltig(d.getUntergrenze()) && Netz.gueltig(d.getObergrenze())
                    && etikett(switchSchule(n), mitAdressen(d.getUntergrenze(), d.getObergrenze()));
        }));
    }

    // ================================================================== chapter 7

    private static boolean heimrouterOk(Netz n) {
        Knoten h = heimrouter(n);
        if (!(h instanceof Gateway)) {
            return false;
        }
        Gateway g = (Gateway) h;
        NetzwerkInterface wan = g.holeWANInterface();
        NetzwerkInterface lan = g.holeLANInterface();
        Knoten heimSwitch = n.gegenueber(lan);
        String gw = Netz.gateway(h);
        return HEIM_WAN.equals(wan.getIp()) && MASKE.equals(wan.getSubnetzMaske()) && (R1_PROVIDER.equals(wan.getGateway()) || R1_PROVIDER.equals(gw))
                && n.gegenueber(wan) == switchMit(n, "provider") && HEIM_LAN.equals(lan.getIp()) && MASKE.equals(lan.getSubnetzMaske())
                && heimSwitch instanceof Switch && Netz.heisst(heimSwitch, "Switch Heimnetz");
    }

    private static Knoten heimSwitch(Netz n) {
        Knoten h = heimrouter(n);
        return h instanceof Gateway ? n.gegenueber(((Gateway) h).holeLANInterface()) : null;
    }

    private static void kapitel8() {
        def("8.1", Regel.zustand(Regeln::heimrouterOk));
        def("8.2", Regel.zustand(n -> {
            DHCPServer d = Netz.dhcpServer(heimrouter(n));
            return d != null && d.isAktiv() && "192.168.178.100".equals(d.getUntergrenze()) && "192.168.178.150".equals(d.getObergrenze())
                    && DNS_SERVER.equals(d.determineDnsserverip());
        }));
        def("8.3", Regel.zustand(n -> {
            Knoten sw = heimSwitch(n);
            Knoten hl = heimLaptop(n);
            Knoten t = tablet(n);
            return sw instanceof Switch && Netz.gleich(Netz.ssid(sw), "Heim-WLAN") && hl instanceof Notebook && t instanceof Notebook && Netz.dhcp(hl)
                    && Netz.dhcp(t) && imWlan(n, hl, sw) && imWlan(n, t, sw) && Netz.heisst(hl, "Heim-Laptop") && Netz.heisst(t, "Tablet");
        }));
        def("8.4", Regel.nachweis((n, e, s) -> befehl(e, s, heimLaptop(n), "ipconfig")).antwort((n, e, a) -> {
            String ip = Netz.ip(heimLaptop(n));
            return Netz.imBereich(ip, "192.168.178.100", "192.168.178.150") && ipAntwort(a, ip);
        }));
        // The shop page reached a device of the home network, or the shop got the request through the home
        // router (NAT). The second proof also holds when the browser's own view of the answer is incomplete,
        // e.g. while another page of the school web server is still loading through R1.
        def("8.5", Regel.nachweis((n, e, s) -> e.gibt(HTTP, s, x -> heimGeraet(x.knoten) && x.ziel.equals(SHOP) && x.port == 80
                && x.status == 200 && seiteMit(x, "Ravensberger24"))
                || shop(n) != null && e.gibt(HTTP_ANFRAGE, s, x -> x.knoten == shop(n) && x.quelle.equals(HEIM_WAN) && x.port == 80)));
        def("8.6", Regel.nachweis((n, e, s) -> fenster(e, s, shop(n)) && e.gibt(HTTP_ANFRAGE, s, x -> x.knoten == shop(n) && x.quelle.equals(HEIM_WAN)))
                .antwort((n, e, a) -> ipAntwort(a, HEIM_WAN)));
        def("8.7", Regel.nachweis((n, e, s) -> e.gibt(NAT, s, x -> x.knoten == heimrouter(n))));
        def("8.8", Regel.nachweis((n, e, s) -> e.gibt(PING_OHNE_ANTWORT, s, x -> schulGeraet(n, x.knoten) && inNetz(x.ziel, HEIMNETZ))).uebergang());
        // version 31: name and network in the label of the field ("+") or a text field, the note "privat, hinter NAT"
        // in the same label or in another text field of the field
        def("8.9", Regel.zustand(n -> etikett(heimSwitch(n), mitNetz(HEIMNETZ + "/24")) && etikett(heimSwitch(n), t -> {
            String klein = t == null ? "" : t.toLowerCase(Locale.GERMAN);
            return klein.contains("nat") || klein.contains("privat");
        })));
    }

    // ================================================================== chapter 8

    private static boolean portfreigabeOk(Netz n) {
        for (String[] f : Netz.portfreigaben(heimrouter(n))) {
            if (f != null && f.length >= 4 && ("TCP".equalsIgnoreCase(f[0].trim()) || "6".equals(f[0].trim())) && "8080".equals(f[1].trim())
                    && BLOG.equals(f[2].trim()) && "80".equals(f[3].trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean blogVonAussen(Netz n, Ereignisse e, long s) {
        return e.gibt(HTTP, s, x -> x.ziel.equals(HEIM_WAN) && x.port == 8080 && x.status == 200 && !heimGeraet(x.knoten) && seiteMit(x, "blog"));
    }

    /** number (1-based) of the first firewall rule that drops TCP port 80 from the tablet to the shop, or -1 */
    static int sperrRegel(Netz n) {
        Firewall fw = Netz.firewall(heimrouter(n));
        if (fw == null) {
            return -1;
        }
        Vector<FirewallRule> regeln = fw.getRuleset();
        for (int i = 0; i < regeln.size(); i++) {
            FirewallRule r = regeln.get(i);
            boolean quelle = r.srcIP == null || r.srcIP.isEmpty() || Netz.gleichesNetz(TABLET_IP, r.srcIP, r.srcMask);
            boolean ziel = r.destIP == null || r.destIP.isEmpty() || Netz.gleichesNetz(SHOP, r.destIP, r.destMask);
            boolean proto = r.protocol == -1 || r.protocol == 6;
            boolean port = r.port == -1 || r.port == 80;
            if (quelle && ziel && proto && port) {
                return r.action == 0 ? i + 1 : -1;
            }
        }
        return -1;
    }

    private static void kapitel10() {
        def("10.1", Regel.zustand(n -> {
            Knoten b = n.knoten("Blog-Server");
            if (!(b instanceof Rechner)) {
                b = n.mitIp(BLOG);
            }
            // the fixed address by hand, or (version 31) DHCP with a static assignment in the home router
            boolean adresse = Netz.fest(b, BLOG, MASKE) || b != null && Netz.dhcp(b) && BLOG.equals(Netz.ip(b)) && MASKE.equals(Netz.maske(b))
                    && BLOG.equals(Netz.statischeZuweisung(heimrouter(n), Netz.mac(b)));
            return b instanceof Rechner && adresse && HEIM_LAN.equals(Netz.gateway(b)) && amSwitch(n, b, heimSwitch(n))
                    && Netz.laeuft(b, Netz.WEBSERVER) && Netz.h1(Netz.datei(b, "webserver/index.html")).toLowerCase(Locale.GERMAN).contains("blog")
                    && Netz.heisst(b, "Blog-Server");
        }));
        def("10.2", Regel.nachweis((n, e, s) -> e.gibt(SYN_OHNE_ANTWORT, s, x -> schulGeraet(n, x.knoten) && x.ziel.equals(HEIM_WAN) && x.port == 8080))
                .uebergang());
        def("10.3", Regel.zustand(Regeln::portfreigabeOk).und(Regeln::blogVonAussen));
        def("10.4", Regel.zustand(n -> {
            Knoten t = tablet(n);
            return t != null && TABLET_IP.equals(Netz.statischeZuweisung(heimrouter(n), Netz.mac(t))) && Netz.dhcp(t) && TABLET_IP.equals(Netz.ip(t));
        }));
        def("10.5", Regel.zustand(n -> {
            Firewall fw = Netz.firewall(heimrouter(n));
            return fw != null && fw.isActivated() && Netz.hat(tablet(n), Netz.WEBBROWSER);
        }).und((n, e, s) -> {
            Knoten t = tablet(n);
            return TABLET_IP.equals(Netz.ip(t)) && dhcpAck(e, s, t) && http(e, s, t, WEBSERVER, filius.modern.tls.Tls.PORT, null)
                    && e.gibt(SYN_OHNE_ANTWORT, s, x -> x.knoten == t && x.ziel.equals(SHOP) && x.port == 80)
                    && http(e, s, heimLaptop(n), SHOP, 80, null) && blogVonAussen(n, e, s);
        }));
        def("10.6", Regel.nachweis((n, e, s) -> e.gibt(HTTP, s, x -> heimGeraet(x.knoten) && x.ziel.equals(HEIM_LAN) && x.status == 200
                && x.pfad.toLowerCase(Locale.ROOT).contains("log"))).antwort((n, e, a) -> {
                    int nr = sperrRegel(n);
                    return nr > 0 && zahlen(a).equals(List.of(String.valueOf(nr)));
                }));
    }

    // ================================================================== chapter 9

    private static boolean mailserverOk(Netz n, Knoten k, String ip, String gw, String netzSwitch, String domain, String... konten) {
        if (!(k instanceof Rechner) || !Netz.fest(k, ip, MASKE) || !gw.equals(Netz.gateway(k)) || !DNS_SERVER.equals(Netz.dns(k))) {
            return false;
        }
        Knoten sw = netzSwitch == null ? switchSchule(n) : switchMit(n, netzSwitch);
        if (sw == null || !n.segment(sw).contains(k)) {
            return false;
        }
        Anwendung a = Netz.software(k, Netz.EMAILSERVER);
        if (!(a instanceof EmailServer)) {
            return false;
        }
        EmailServer m = (EmailServer) a;
        if (!m.isAktiv() || !domain.equalsIgnoreCase(String.valueOf(m.getMailDomain()).trim())) {
            return false;
        }
        for (String konto : konten) {
            if (!hatKonto(m, konto)) {
                return false;
            }
        }
        return true;
    }

    static boolean hatKonto(EmailServer m, String name) {
        try {
            for (EmailKonto k : m.getListeBenutzerkonten()) {
                if (name.equalsIgnoreCase(k.getBenutzername())) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            // no accounts
        }
        return false;
    }

    private static boolean programmOk(Knoten k, String adresse, String server) {
        Anwendung a = Netz.software(k, Netz.EMAILPROGRAMM);
        if (!(a instanceof EmailAnwendung)) {
            return false;
        }
        Map<String, EmailKonto> konten = ((EmailAnwendung) a).holeKontoListe();
        if (konten == null || konten.isEmpty()) {
            // after opening a project the account is read from the file system when the program
            // window opens; read it here as well
            ((EmailAnwendung) a).laden();
            konten = ((EmailAnwendung) a).holeKontoListe();
        }
        if (konten == null) {
            return false;
        }
        for (EmailKonto konto : konten.values()) {
            if (adresse.equalsIgnoreCase(String.valueOf(konto.getEmailAdresse()).trim()) && server.equalsIgnoreCase(Netz.name(konto.getSmtpserver()))
                    && server.equalsIgnoreCase(Netz.name(konto.getPop3server()))) {
                return true;
            }
        }
        return false;
    }

    private static void kapitel9() {
        String anna = "anna@" + DOMAIN_SCHULE;
        String ben = "ben@" + DOMAIN_SCHULE;
        String verkauf = "verkauf@" + DOMAIN_RGG;
        def("9.1", Regel.zustand(n -> mailserverOk(n, mailSchule(n), MAIL_SCHULE, R1_SCHULE, null, DOMAIN_SCHULE, "anna", "ben")
                && Netz.heisst(mailSchule(n), "Mailserver Schule")));
        def("9.2", Regel.zustand(n -> mailserverOk(n, mailRgg(n), MAIL_RGG, R2_RGG, "rgg", DOMAIN_RGG, "verkauf") && Netz.heisst(mailRgg(n), "Mailserver RGG")));
        def("9.3", Regel.zustand(n -> {
            Knoten d = dnsServer(n);
            return Netz.record(d, "mail." + DOMAIN_SCHULE, "A").contains(MAIL_SCHULE) && Netz.record(d, "mail." + DOMAIN_RGG, "A").contains(MAIL_RGG)
                    && Netz.record(d, DOMAIN_SCHULE, "MX").contains("mail." + DOMAIN_SCHULE) && Netz.record(d, DOMAIN_RGG, "MX").contains("mail." + DOMAIN_RGG);
        }));
        def("9.4", Regel.nachweis((n, e, s) -> befehl(e, s, laptop(n, 1), "host")
                && e.gibt(DNS, s, x -> x.knoten == laptop(n, 1) && x.werte.stream().anyMatch(w -> w.startsWith(DOMAIN_RGG + " MX ")))));
        def("9.5", Regel.zustand(n -> programmOk(laptop(n, 1), anna, "mail." + DOMAIN_SCHULE) && programmOk(buero(n), verkauf, "mail." + DOMAIN_RGG)));
        def("9.6", Regel.nachweis((n, e, s) -> smtp(e, s, MAIL_SCHULE, null, anna, ben)));
        def("9.7", Regel.nachweis((n, e, s) -> smtp(e, s, MAIL_RGG, MAIL_SCHULE, anna, verkauf)));
        def("9.8", Regel.nachweis((n, e, s) -> {
            Knoten b = buero(n);
            boolean abgerufen = b != null && e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_RGG) && x.typ.equals("FETCH") && x.name.equals("verkauf") && x.quelle.equals(Netz.ip(b)));
            Ereignis antwort = smtpEreignis(e, s, MAIL_SCHULE, MAIL_RGG, verkauf, anna);
            return abgerufen && antwort != null
                    && e.gibt(IMAP, antwort.zeit, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("FETCH") && x.name.equals("anna") && x.quelle.equals(HEIM_WAN));
        }));
        def("9.9", Regel.nachweis((n, e, s) -> befehl(e, s, mailSchule(n), "netstat")).antwort((n, e, a) -> {
            List<String> z = zahlen(a);
            return z.size() == 2 && z.contains("25") && z.contains("143");
        }));
        // IMAP: a mail deleted on one device is gone on the server; Laptop-1 reads the mailbox again afterwards
        def("9.10", Regel.nachweis((n, e, s) -> {
            Ereignis geloescht = e.letztes(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("EXPUNGE") && x.name.equals("anna"));
            Knoten l1 = laptop(n, 1);
            return geloescht != null && l1 != null && e.gibt(IMAP, geloescht.zeit, x -> x.ziel.equals(MAIL_SCHULE) && x.name.equals("anna")
                    && x.quelle.equals(Netz.ip(l1)) && (x.typ.equals("LOGIN") || x.typ.equals("FETCH")));
        }));
    }

    // ================================================================== chapter 10

    static final String[] NETZE = { SCHULNETZ, PROVIDERNETZ, RGGNETZ, HEIMNETZ };

    /** the warehouse network: a further router with one card in the provider network, the other one elsewhere */
    static final class Lager {
        Knoten router;
        NetzwerkInterface karte;
        Set<Knoten> segment;
        String netz;
        String maske;
    }

    static Lager lager(Netz n) {
        Knoten r1 = r1(n);
        Knoten r2 = r2(n);
        Knoten prov = switchMit(n, "provider");
        for (Knoten k : n.router()) {
            if (k == r1 || k == r2) {
                continue;
            }
            NetzwerkInterface amProvider = null;
            NetzwerkInterface innen = null;
            for (NetzwerkInterface nic : Netz.nics(k)) {
                if (inNetz(nic.getIp(), PROVIDERNETZ) && n.gegenueber(nic) == prov) {
                    amProvider = nic;
                } else if (Netz.gueltig(nic.getIp()) && n.gegenueber(nic) instanceof Switch) {
                    innen = nic;
                }
            }
            if (amProvider == null || innen == null) {
                continue;
            }
            Lager l = new Lager();
            l.router = k;
            l.karte = innen;
            l.maske = innen.getSubnetzMaske();
            l.netz = Netz.netzadresse(innen.getIp(), l.maske);
            l.segment = n.segment(n.gegenueber(innen));
            return l;
        }
        return null;
    }

    static boolean ueberschneidet(String netz, String maske) {
        for (String anderes : NETZE) {
            String weitere = Netz.wert(maske) < Netz.wert(MASKE) ? maske : MASKE;
            if (Netz.gleichesNetz(netz, anderes, weitere)) {
                return true;
            }
        }
        return false;
    }

    static boolean lagerGeraet(Lager l, Knoten k) {
        return l != null && k != null && l.segment.contains(k) && Netz.gleichesNetz(Netz.ip(k), l.netz, l.maske);
    }

    // ================================================================== chapter 11 (VPN, follows)

    // ================================================================== chapter 11

    static final String INTRANET = "198.51.100.40";
    static final String VPN_SERVER = "198.51.100.10";
    static final String VPN_PASSWORT = "Homeoffice";

    static Knoten intranet(Netz n) {
        return pc(n, "Intranet", INTRANET);
    }

    static Knoten vpnServer(Netz n) {
        return pc(n, "VPN-Server", VPN_SERVER);
    }

    static Knoten homeoffice(Netz n) {
        Knoten k = n.knoten("Homeoffice-Laptop");
        return k instanceof Notebook ? k : null;
    }

    /** the first matching rule of the firewall (or the default action) drops a packet from quelle to ziel:port */
    static boolean verworfen(Firewall fw, String quelle, String ziel, int protokoll, int port) {
        if (fw == null || !fw.isActivated()) {
            return false;
        }
        for (FirewallRule r : fw.getRuleset()) {
            boolean q = r.srcIP == null || r.srcIP.isEmpty() || Netz.gleichesNetz(quelle, r.srcIP, r.srcMask);
            boolean z = r.destIP == null || r.destIP.isEmpty() || Netz.gleichesNetz(ziel, r.destIP, r.destMask);
            boolean pr = r.protocol == -1 || r.protocol == protokoll;
            boolean po = r.port == -1 || r.port == port;
            if (q && z && pr && po) {
                return r.action == 0;
            }
        }
        return fw.getDefaultPolicy() == 0;
    }

    /** the firewall of R2 keeps the intranet and IMAP closed from outside; the shop, SMTP and the VPN stay open */
    static boolean rggFirewallOk(Netz n) {
        Firewall fw = Netz.firewall(r2(n));
        return fw != null && fw.isActivated() && verworfen(fw, HEIM_WAN, INTRANET, 6, 80) && verworfen(fw, HEIM_WAN, MAIL_RGG, 6, 143)
                && !verworfen(fw, HEIM_WAN, SHOP, 6, 80) && !verworfen(fw, MAIL_SCHULE, MAIL_RGG, 6, 25)
                && !verworfen(fw, HEIM_WAN, VPN_SERVER, 17, filius.software.vpn.Vpn.PORT) && !verworfen(fw, DNS_SERVER, BUERO, 17, 53);
    }

    static boolean imapVon(Ereignisse e, long seit, String client, String konto, String... typen) {
        return e.gibt(IMAP, seit, x -> x.ziel.equals(MAIL_RGG) && x.quelle.equals(client) && x.name.equalsIgnoreCase(konto) && List.of(typen).contains(x.typ));
    }

    /** the last VPN Accept the computer received (werte: its VPN address), null if none */
    static Ereignis vpnZugang(Ereignisse e, long seit, Knoten k) {
        return k == null ? null : e.letztes(VPN, seit, x -> x.knoten == k && "Accept".equals(x.name) && "empfangen".equals(x.typ));
    }

    static boolean vpnServerOk(Netz n) {
        Knoten k = vpnServer(n);
        if (!(k instanceof Rechner) || !Netz.fest(k, VPN_SERVER, MASKE) || !R2_RGG.equals(Netz.gateway(k)) || !amSwitch(n, k, switchMit(n, "RGG"))
                || !Netz.heisst(k, "VPN-Server")) {
            return false;
        }
        Anwendung a = Netz.software(k, Netz.VPNSERVER);
        if (!(a instanceof filius.software.vpn.VpnServer)) {
            return false;
        }
        filius.software.vpn.VpnServer v = (filius.software.vpn.VpnServer) a;
        String[] netz = v.freigegebenesNetz();
        boolean konto = false;
        for (Map.Entry<String, String> x : v.konten().entrySet()) {
            konto |= x.getKey().equalsIgnoreCase("verkauf") && x.getValue().equals(VPN_PASSWORT);
        }
        return v.isAktiv() && konto && Netz.gleichesNetz(INTRANET, netz[0], netz[1]) && Netz.gleichesNetz(MAIL_RGG, netz[0], netz[1]);
    }

    private static void kapitel11() {
        String verkauf = "verkauf@" + DOMAIN_RGG;
        def("11.1", Regel.zustand(n -> {
            Knoten k = intranet(n);
            if (!(k instanceof Rechner) || !Netz.fest(k, INTRANET, MASKE) || !R2_RGG.equals(Netz.gateway(k)) || !amSwitch(n, k, switchMit(n, "RGG"))
                    || !Netz.laeuft(k, Netz.WEBSERVER) || !Netz.heisst(k, "Intranet")) {
                return false;
            }
            String seite = String.valueOf(Netz.datei(k, "webserver/index.html")).toLowerCase(Locale.GERMAN);
            String h1 = Netz.h1(seite);
            return (h1.contains("lager") || h1.contains("warehouse")) && (seite.contains("apfelschorle") || seite.contains("apple"));
        }));
        def("11.2", Regel.nachweis((n, e, s) -> http(e, s, buero(n), INTRANET, 80, null) && imapVon(e, s, BUERO, "verkauf", "LOGIN", "FETCH")));
        def("11.3", Regel.zustand(n -> {
            Knoten h = homeoffice(n);
            return h != null && Netz.heisst(h, "Homeoffice-Laptop") && imWlan(n, h, heimSwitch(n)) && Netz.dhcp(h) && inNetz(Netz.ip(h), HEIMNETZ)
                    && Netz.hat(h, Netz.WEBBROWSER) && programmOk(h, verkauf, "mail." + DOMAIN_RGG);
        }));
        def("11.4", Regel.nachweis((n, e, s) -> http(e, s, homeoffice(n), INTRANET, 80, null)).uebergang());
        def("11.5", Regel.zustand(Regeln::rggFirewallOk));
        def("11.6", Regel.zustand(Regeln::rggFirewallOk).und((n, e, s) -> {
            Knoten h = homeoffice(n);
            return h != null && e.gibt(SYN_OHNE_ANTWORT, s, x -> x.knoten == h && (x.ziel.equals(INTRANET) || x.ziel.equals(MAIL_RGG)))
                    && http(e, s, buero(n), INTRANET, 80, null);
        }).uebergang());
        def("11.7", Regel.zustand(Regeln::vpnServerOk));
        def("11.8", Regel.zustand(n -> Netz.hat(homeoffice(n), Netz.VPNCLIENT)).und((n, e, s) -> vpnZugang(e, s, homeoffice(n)) != null)
                .antwort((n, e, a) -> {
                    Ereignis z = vpnZugang(e, 0, homeoffice(n));
                    return z != null && !z.werte.isEmpty() && ipAntwort(a, z.werte.get(0));
                }));
        def("11.9", Regel.nachweis((n, e, s) -> {
            Knoten h = homeoffice(n);
            Knoten v = vpnServer(n);
            Ereignis z = vpnZugang(e, s, h);
            return z != null && v != null && e.gibt(TCP_VERBUNDEN, z.zeit, x -> x.knoten == h && x.ziel.equals(MAIL_RGG) && x.port == 143)
                    && imapVon(e, z.zeit, Netz.ip(v), "verkauf", "LOGIN", "FETCH");
        }));
        def("11.10", Regel.nachweis((n, e, s) -> {
            Knoten h = homeoffice(n);
            Ereignis z = vpnZugang(e, s, h);
            return z != null && http(e, z.zeit, h, INTRANET, 80, null);
        }).antwort((n, e, a) -> {
            Knoten v = vpnServer(n);
            return v != null && ipAntwort(a, Netz.ip(v));
        }));
        def("11.11", Regel.nachweis((n, e, s) -> fenster(e, s, heimrouter(n))));
        def("11.12", Regel.zustand(n -> textNeben(vpnServer(n), t -> {
            String x = t.toLowerCase(Locale.ROOT);
            return x.contains("vpn") && x.contains("1194");
        })));
    }

    private static void kapitel12() {
        // final project: eight of these faults per variant; each one counts when the cause is fixed and the
        // end-to-end test of the ticket was observed afterwards
        def("A1", Regel.zustand(n -> {
            DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
            return s != null && R1_SCHULE.equals(s.determineGatewayip());
        }).und((n, e, s) -> e.gibt(DNS, s, x -> schulLaptops(n).contains(x.knoten) && Netz.dhcp(x.knoten))));
        def("A2", Regel.zustand(n -> ripAn(r2(n))).und((n, e, s) -> e.gibt(PING_ANTWORT, s, x -> schulGeraet(n, x.knoten) && inNetz(x.ziel, RGGNETZ))
                || e.gibt(TCP_VERBUNDEN, s, x -> schulGeraet(n, x.knoten) && inNetz(x.ziel, RGGNETZ))));
        def("A3", Regel.zustand(n -> Netz.laeuft(shop(n), Netz.WEBSERVER)).und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(SHOP) && x.status == 200)));
        def("A4", Regel.zustand(n -> {
            List<String> a = Netz.record(dnsServer(n), WWW_SHOP, "A");
            return !a.isEmpty() && a.stream().allMatch(SHOP::equals);
        }).und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(SHOP) && x.status == 200 && x.host.startsWith(WWW_SHOP))));
        def("A5", Regel.zustand(n -> Netz.record(dnsServer(n), DOMAIN_RGG, "MX").contains("mail." + DOMAIN_RGG))
                .und((n, e, s) -> smtp(e, s, MAIL_RGG, MAIL_SCHULE, null, null)));
        def("A6", Regel.zustand(n -> {
            DHCPServer d = Netz.dhcpServer(heimrouter(n));
            return d != null && DNS_SERVER.equals(d.determineDnsserverip());
        }).und((n, e, s) -> e.gibt(DNS, s, x -> heimGeraet(x.knoten))));
        def("A7", Regel.zustand(Regeln::portfreigabeOk).und(Regeln::blogVonAussen));
        def("A8", Regel.nachweis((n, e, s) -> http(e, s, heimLaptop(n), SHOP, 80, null)
                && e.gibt(SYN_OHNE_ANTWORT, s, x -> x.knoten == tablet(n) && x.ziel.equals(SHOP) && x.port == 80)));
        def("A9", Regel.zustand(n -> {
            Anwendung a = Netz.software(mailSchule(n), Netz.EMAILSERVER);
            return a instanceof EmailServer && ((EmailServer) a).isAktiv();
        }).und((n, e, s) -> e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE))));
        def("A10", Regel.zustand(n -> Netz.dhcp(laptop(n, 3))).und((n, e, s) -> e.gibt(DNS, s, x -> x.knoten == laptop(n, 3))));
        // HTTPS of the school website (chapter 4)
        def("A11", Regel.zustand(n -> {
            filius.software.www.WebServer w = webserverProgramm(n);
            return w != null && w.isHttps() && w.isAktiv();
        }).und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER) && x.port == filius.modern.tls.Tls.PORT && x.status == 200)));
        def("A12", Regel.zustand(n -> httpsOk(n, WEBSERVER)).und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER)
                && x.port == filius.modern.tls.Tls.PORT && x.status == 200 && x.host.startsWith(WWW_SCHULE))
                && !e.gibt(TLS, s, x -> "Alert".equals(x.name) && x.ziel.equals(WEBSERVER))));
        // home office with VPN (chapter 11)
        def("A13", Regel.zustand(Regeln::vpnServerOk).und(Regeln::vpnMails));
        def("A14", Regel.zustand(Regeln::rggFirewallOk).und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(SHOP) && x.status == 200
                && (schulGeraet(n, x.knoten) || heimGeraet(x.knoten)))));
        def("A15", Regel.zustand(n -> Netz.laeuft(vpnServer(n), Netz.VPNSERVER)).und(Regeln::vpnMails));
        def("A16", Regel.zustand(n -> Netz.fest(intranet(n), INTRANET, MASKE)).und((n, e, s) -> http(e, s, buero(n), INTRANET, 80, null)));
        def("A17", Regel.zustand(n -> programmOk(buero(n), "verkauf@" + DOMAIN_RGG, "mail." + DOMAIN_RGG))
                .und((n, e, s) -> imapVon(e, s, BUERO, "verkauf", "LOGIN", "FETCH")));
        def("A18", Regel.zustand(n -> Netz.laeuft(blog(n), Netz.WEBSERVER)).und((n, e, s) -> e.gibt(HTTP, s, x -> x.status == 200
                && (x.ziel.equals(BLOG) || x.ziel.equals(HEIM_WAN) && x.port == 8080) && seiteMit(x, "blog"))));
        def("A19", Regel.zustand(n -> amSwitch(n, webserver(n), switchSchule(n))).und((n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER)
                && x.status == 200)));
    }

    /** VPN end-to-end test: the home office laptop got access and fetched the mails of verkauf through the tunnel */
    static boolean vpnMails(Netz n, Ereignisse e, long s) {
        Knoten h = homeoffice(n);
        Knoten v = vpnServer(n);
        Ereignis z = vpnZugang(e, s, h);
        return z != null && v != null && imapVon(e, z.zeit, Netz.ip(v), "verkauf", "LOGIN", "FETCH");
    }

    @SuppressWarnings("unchecked")
    public static List<GUIDocuItem> textfelder() {
        List<GUIDocuItem> result = new ArrayList<>();
        try {
            Field f = GUIContainer.class.getDeclaredField("docuItems");
            f.setAccessible(true);
            for (GUIDocuItem item : new ArrayList<>((List<GUIDocuItem>) f.get(GUIContainer.getGUIContainer()))) {
                if (item.getType() == GUIDocuItem.TEXT && item.getText() != null) {
                    result.add(item);
                }
            }
        } catch (Exception e) {
            // none
        }
        return result;
    }

    private static boolean netzplanOk(Netz n) {
        Lager l = lager(n);
        if (l == null) {
            return false;
        }
        Map<String, Knoten> netze = new HashMap<>();
        netze.put(SCHULNETZ, switchSchule(n));
        netze.put(PROVIDERNETZ, switchMit(n, "provider"));
        netze.put(RGGNETZ, switchMit(n, "rgg"));
        netze.put(HEIMNETZ, heimSwitch(n));
        netze.put(l.netz, n.gegenueber(l.karte));
        for (Map.Entry<String, Knoten> netz : netze.entrySet()) {
            if (!etikett(netz.getValue(), mitAdressen(netz.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /** text containing all these addresses */
    static Predicate<String> mitAdressen(String... ips) {
        return t -> Netz.adressen(t).containsAll(Arrays.asList(ips));
    }

    /**
     * text naming these networks with their prefix length, e.g. "192.168.10.0/24" (spaces around the slash are
     * fine); the address alone does not count (version 31: the labels of the network plan need the /24)
     */
    static Predicate<String> mitNetz(String... netze) {
        return t -> {
            if (t == null) {
                return false;
            }
            String text = t.replaceAll("\\s*/\\s*", "/");
            for (String netz : netze) {
                if (!java.util.regex.Pattern.compile("(?<![\\d.])" + java.util.regex.Pattern.quote(netz) + "(?!\\d)").matcher(text).find()) {
                    return false;
                }
            }
            return true;
        };
    }

    /** structure fields (rectangles) of the documentation mode */
    @SuppressWarnings("unchecked")
    public static List<GUIDocuItem> flaechen() {
        List<GUIDocuItem> result = new ArrayList<>();
        try {
            Field f = GUIContainer.class.getDeclaredField("docuItems");
            f.setAccessible(true);
            for (GUIDocuItem item : new ArrayList<>((List<GUIDocuItem>) f.get(GUIContainer.getGUIContainer()))) {
                if (item.getType() == GUIDocuItem.RECT) {
                    result.add(item);
                }
            }
        } catch (Exception e) {
            // none
        }
        return result;
    }

    /**
     * The label of the switch's network: every network gets a structure field (Gliederungsfläche) around
     * its devices with a label at the top left. It counts if the smallest structure field around the switch
     * has a label of its own (the "+" of the field) that fits, or contains a text field whose text fits.
     */
    static boolean etikett(Knoten sw, Predicate<String> passt) {
        GUIDocuItem flaeche = bereichFeld(sw);
        if (flaeche == null) {
            return false;
        }
        // the label of the structure field itself (the "+" at its top left)
        String titel = flaeche.getText();
        if (titel != null && passt.test(titel)) {
            return true;
        }
        Rectangle bereich = new Rectangle(flaeche.getX(), flaeche.getY(), flaeche.getWidth(), flaeche.getHeight());
        for (GUIDocuItem feld : textfelder()) {
            double fx = feld.getX() + Math.min(feld.getWidth(), 60) / 2.0;
            double fy = feld.getY() + feld.getHeight() / 2.0;
            if (bereich.contains(fx, fy) && passt.test(feld.getText())) {
                return true;
            }
        }
        return false;
    }

    /** the smallest structure field around the switch, null if there is none */
    static Rectangle bereich(Knoten sw) {
        GUIDocuItem f = bereichFeld(sw);
        return f == null ? null : new Rectangle(f.getX(), f.getY(), f.getWidth(), f.getHeight());
    }

    /** the smallest structure field around the switch (with its label), null if there is none */
    static GUIDocuItem bereichFeld(Knoten sw) {
        if (sw == null) {
            return null;
        }
        Rectangle schalter = null;
        for (GUIKnotenItem item : GUIContainer.getGUIContainer().getKnotenItems()) {
            if (item.getKnoten() == sw) {
                schalter = item.getImageLabel().getBounds();
            }
        }
        if (schalter == null) {
            return null;
        }
        Rectangle beste = null;
        GUIDocuItem besteFlaeche = null;
        for (GUIDocuItem f : flaechen()) {
            Rectangle r = new Rectangle(f.getX(), f.getY(), f.getWidth(), f.getHeight());
            if (r.contains(schalter.getCenterX(), schalter.getCenterY()) && (beste == null || (long) r.width * r.height < (long) beste.width * beste.height)) {
                beste = r;
                besteFlaeche = f;
            }
        }
        return besteFlaeche;
    }
}
