package filius.modern.lernpfad;

import static filius.modern.lernpfad.Ereignis.Art.BEFEHL;
import static filius.modern.lernpfad.Ereignis.Art.HTTP;
import static filius.modern.lernpfad.Ereignis.Art.IMAP;
import static filius.modern.lernpfad.Ereignis.Art.PING_ANTWORT;
import static filius.modern.lernpfad.Ereignis.Art.PING_OHNE_ANTWORT;
import static filius.modern.lernpfad.Ereignis.Art.SAT;
import static filius.modern.lernpfad.Ereignis.Art.TCP_VERBUNDEN;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import filius.gui.GUIContainer;
import filius.gui.netzwerksicht.GUIDocuItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.hardware.NetzwerkInterface;
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

/**
 * The check rules of the standard learning path (8 chapters): three networks on one router with three cards.
 * Task ids "1.1" ... "7.7", chapter 8 "A1" ... "A9" (three variants) and "B1" ... "B3".
 */
public final class RegelnStandard {

    // ------------------------------------------------------------------ addresses of the course

    static final String MASKE = "255.255.255.0";
    static final String SCHULNETZ = "192.168.0.0";
    static final String RGGNETZ = "192.168.1.0";
    static final String DNSNETZ = "192.168.2.0";
    static final String ROUTER_SCHULE = "192.168.0.1";
    static final String ROUTER_RGG = "192.168.1.1";
    static final String ROUTER_DNS = "192.168.2.1";
    static final String WEBSERVER = "192.168.0.14";
    static final String MAIL_SCHULE = "192.168.0.15";
    static final String DHCP_SERVER = "192.168.0.16";
    static final String SHOP = "192.168.1.10";
    static final String BUERO = "192.168.1.11";
    static final String MAIL_RGG = "192.168.1.12";
    static final String DNS_SERVER = "192.168.2.10";
    static final String DHCP_VON = "192.168.0.100";
    static final String DHCP_BIS = "192.168.0.150";
    static final String LEHRER = "192.168.0.13";
    /** the address of the experiment of chapter 2: another network part */
    static final String ANDERES_NETZ = "192.168.5.13";
    static final String WWW_SCHULE = "www.rbz-wirtschaft-kiel.de";
    static final String WWW_SHOP = "www.ravensberger24.de";
    static final String DOMAIN_SCHULE = "rbz-wirtschaft-kiel.de";
    static final String DOMAIN_RGG = "ravensberger24.de";
    /** the address every new card has in Filius */
    static final String FILIUS_VORGABE = "192.168.0.10";

    private static final Map<String, Regel> REGELN = new HashMap<>();
    private static List<String> startseitenAbsaetze;

    private RegelnStandard() {}

    public static synchronized Regel regel(String id) {
        if (REGELN.isEmpty()) {
            kapitel1();
            kapitel2();
            kapitel3();
            kapitel4();
            kapitel5();
            kapitel6();
            kapitel7();
            kapitel8();
        }
        return REGELN.get(id);
    }

    private static void def(String id, Regel r) {
        REGELN.put(id, r);
    }

    // ------------------------------------------------------------------ devices of the course

    /** Laptop-i by name, otherwise the laptop with its fixed address 192.168.0.(9 + i) */
    /** the laptop of the teacher: Laptop-4, renamed "Lehrer-Laptop" in chapter 7 */
    static Knoten lehrerLaptop(Netz n) {
        Knoten k = n.knoten("Lehrer-Laptop");
        return k != null ? k : laptop(n, 4);
    }

    static Knoten laptop(Netz n, int i) {
        Knoten k = n.knoten("Laptop-" + i);
        if (k == null) {
            Knoten byIp = n.mitIp("192.168.0." + (9 + i));
            if (byIp instanceof Notebook) {
                k = byIp;
            }
        }
        return k;
    }

    static List<Knoten> schulLaptops(Netz n) {
        List<Knoten> l = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Knoten k = i == 4 ? lehrerLaptop(n) : laptop(n, i);
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
            Knoten byIp = n.mitIp(BUERO);
            k = byIp instanceof Notebook ? byIp : null;
        }
        return k;
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

    static Knoten switchSchule(Netz n) {
        Knoten k = n.knoten("Switch Schule");
        if (k instanceof Switch) {
            return k;
        }
        Knoten l1 = laptop(n, 1);
        return l1 == null ? null : n.switchAn(l1);
    }

    /** the switch of a network: by its name, otherwise the switch at the router card of that network */
    static Knoten netzSwitch(Netz n, String name, String routerIp) {
        Knoten k = n.knoten(name);
        if (k instanceof Switch) {
            return k;
        }
        Knoten r = router(n);
        Knoten g = r == null ? null : n.gegenueber(Netz.nic(r, routerIp));
        return g instanceof Switch ? g : null;
    }

    static Knoten switchRgg(Netz n) {
        return netzSwitch(n, "Switch RGG", ROUTER_RGG);
    }

    static Knoten switchDns(Netz n) {
        return netzSwitch(n, "Switch DNS-Anbieter", ROUTER_DNS);
    }

    /** the router in the middle: by its name, otherwise the router with the school card */
    static Knoten router(Netz n) {
        Knoten k = n.knoten("Router");
        if (k instanceof Vermittlungsrechner) {
            return k;
        }
        Knoten byIp = n.mitIp(ROUTER_SCHULE);
        return byIp instanceof Vermittlungsrechner ? byIp : null;
    }

    /** card of k with this address that is plugged into the given switch */
    static boolean karteAn(Netz n, Knoten k, String ip, Knoten sw) {
        NetzwerkInterface nic = Netz.nic(k, ip);
        return nic != null && MASKE.equals(nic.getSubnetzMaske()) && sw != null && n.gegenueber(nic) == sw;
    }

    static boolean amSwitch(Netz n, Knoten host, Knoten sw) {
        return host != null && sw != null && n.switchAn(host) == sw;
    }

    static boolean inNetz(String ip, String netz) {
        return Netz.gleichesNetz(ip, netz, MASKE);
    }

    static boolean schulGeraet(Knoten k) {
        return k != null && inNetz(Netz.ip(k), SCHULNETZ);
    }

    /** fixed address, net mask and gateway */
    static boolean fest(Knoten k, String ip, String gateway) {
        return Netz.fest(k, ip, MASKE) && gateway.equals(Netz.gateway(k));
    }

    // ------------------------------------------------------------------ event helpers

    static boolean befehl(Ereignisse e, long seit, Knoten k, String... namen) {
        return Regeln.befehl(e, seit, k, namen);
    }

    static boolean fenster(Ereignisse e, long seit, Knoten k) {
        return Regeln.fenster(e, seit, k);
    }

    static boolean pingOk(Ereignisse e, long seit, Knoten von, String ziel) {
        return Regeln.pingOk(e, seit, von, ziel);
    }

    /** a ping from the device to that address was typed, at a time the device fulfilled the condition */
    static Ereignis ping(Ereignisse e, long seit, Knoten von, String ziel, java.util.function.Predicate<Ereignis> wie) {
        return von == null ? null : e.letztes(BEFEHL, seit, x -> x.knoten == von && x.name.equals("ping") && x.werte.contains(ziel) && (wie == null || wie.test(x)));
    }

    static boolean http(Ereignisse e, long seit, Knoten client, String server, java.util.function.Predicate<Ereignis> f) {
        return Regeln.http(e, seit, client, server, 80, f);
    }

    static boolean dnsAntwort(Ereignisse e, long seit, Knoten k, String name, String wert) {
        return Regeln.dnsAntwort(e, seit, k, name, wert);
    }

    static boolean smtp(Ereignisse e, long seit, String server, String client, String von, String an) {
        return Regeln.smtp(e, seit, server, client, von, an);
    }

    static Ereignis smtpEreignis(Ereignisse e, long seit, String server, String client, String von, String an) {
        return Regeln.smtpEreignis(e, seit, server, client, von, an);
    }

    /** mails of the account were fetched by the device from the server (IMAP) */
    static Ereignis abruf(Ereignisse e, long seit, String server, String konto, Knoten von) {
        return von == null ? null : e.erstes(IMAP, seit, x -> x.ziel.equals(server) && x.typ.equals("FETCH") && x.name.equalsIgnoreCase(konto) && x.quelle.equals(Netz.ip(von)));
    }

    // ================================================================== chapter 1

    private static void kapitel1() {
        def("1.1", Regel.zustand(n -> n.knoten("Laptop-1") instanceof Notebook && n.knoten("Laptop-2") instanceof Notebook));
        def("1.2", Regel.zustand(n -> n.verbunden(n.knoten("Laptop-1"), n.knoten("Laptop-2"))));
        def("1.3", Regel.zustand(n -> Netz.fest(n.knoten("Laptop-1"), "192.168.0.10", MASKE) && Netz.fest(n.knoten("Laptop-2"), "192.168.0.11", MASKE)));
        def("1.4", Regel.zustand(n -> Netz.fest(laptop(n, 2), "192.168.0.11", MASKE)).und((n, e, s) -> pingOk(e, s, laptop(n, 1), "192.168.0.11")));
        def("1.5", Regel.nachweis((n, e, s) -> pingOk(e, s, laptop(n, 1), "192.168.0.11")));
        def("1.6", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 1)) && e.gibt(PING_ANTWORT, s, x -> x.knoten == laptop(n, 1))));
        // the experiment: a ping while Laptop-1 had no cable; afterwards the cable is back
        def("1.7", Regel.zustand(n -> n.verbunden(laptop(n, 1), laptop(n, 2))).und((n, e, s) -> {
            Knoten l1 = laptop(n, 1);
            return ping(e, 0, l1, "192.168.0.11", x -> x.port == 0) != null
                    || e.gibt(PING_OHNE_ANTWORT, 0, x -> x.knoten == l1 && x.ziel.equals("192.168.0.11"));
        }));
    }

    // ================================================================== chapter 2

    private static void kapitel2() {
        def("2.1", Regel.zustand(n -> {
            Knoten sw = n.knoten("Switch Schule");
            return sw instanceof Switch && !n.verbunden(laptop(n, 1), laptop(n, 2)) && amSwitch(n, laptop(n, 1), sw) && amSwitch(n, laptop(n, 2), sw);
        }));
        def("2.2", Regel.zustand(n -> {
            Knoten sw = n.knoten("Switch Schule");
            Knoten l3 = n.knoten("Laptop-3");
            Knoten l4 = n.knoten("Laptop-4");
            return sw instanceof Switch && l3 instanceof Notebook && l4 instanceof Notebook && Netz.fest(l3, "192.168.0.12", MASKE)
                    && Netz.fest(l4, LEHRER, MASKE) && amSwitch(n, l3, sw) && amSwitch(n, l4, sw);
        }));
        def("2.3", Regel.zustand(n -> amSwitch(n, laptop(n, 1), switchSchule(n)) && amSwitch(n, laptop(n, 4), switchSchule(n))
                && Netz.fest(laptop(n, 4), LEHRER, MASKE)).und((n, e, s) -> pingOk(e, s, laptop(n, 1), LEHRER)));
        def("2.4", Regel.zustand(n -> laptop(n, 4) != null).antwort((n, e, a) -> laptop(n, 4) != null && Regeln.mac(a).equals(Regeln.mac(Netz.mac(laptop(n, 4))))));
        def("2.5", Regel.nachweis((n, e, s) -> e.gibt(SAT, s, x -> x.knoten == switchSchule(n))).antwort((n, e, a) -> {
            Knoten sw = switchSchule(n);
            Knoten l4 = laptop(n, 4);
            if (!(sw instanceof Switch) || l4 == null) {
                return false;
            }
            String erwartet = Regeln.satAnschluss((Switch) sw, Netz.mac(l4));
            if (erwartet == null) {
                erwartet = String.valueOf(n.switchAnschluss((Switch) sw, l4));
            }
            List<String> z = Regeln.zahlen(a);
            return z.size() == 1 && z.get(0).equals(erwartet);
        }));
        // experiment: Laptop-4 in another network part, Laptop-1 pings it in vain
        def("2.6", Regel.zustand(n -> ANDERES_NETZ.equals(Netz.ip(n.knoten("Laptop-4"))))
                .und((n, e, s) -> ping(e, s, laptop(n, 1), ANDERES_NETZ, null) != null
                        || e.gibt(PING_OHNE_ANTWORT, s, x -> x.knoten == laptop(n, 1) && x.ziel.equals(ANDERES_NETZ)))
                .uebergang());
        // back to 192.168.0.13: a ping after the experiment gets an answer again
        def("2.7", Regel.zustand(n -> Netz.fest(laptop(n, 4), LEHRER, MASKE) && amSwitch(n, laptop(n, 4), switchSchule(n))).und((n, e, s) -> {
            Ereignis experiment = ping(e, 0, laptop(n, 1), ANDERES_NETZ, null);
            return experiment != null && pingOk(e, Math.max(s, experiment.zeit), laptop(n, 1), LEHRER);
        }));
        def("2.8", Regel.zustand(n -> Regeln.etikett(switchSchule(n), Regeln.mitAdressen(SCHULNETZ))));
    }

    // ================================================================== chapter 3

    /** paragraphs (texts of <p>) of a page */
    static List<String> absaetze(String html) {
        List<String> l = new ArrayList<>();
        if (html == null) {
            return l;
        }
        Matcher m = Pattern.compile("(?is)<p(?:\\s[^>]*)?>(.*?)</p>").matcher(html);
        while (m.find()) {
            String t = Netz.text(m.group(1)).replaceAll("\\s+", " ").trim();
            if (!t.isEmpty()) {
                l.add(t);
            }
        }
        return l;
    }

    /** paragraphs of the start page of a new web server (German and English) */
    static synchronized List<String> startseitenAbsaetze() {
        if (startseitenAbsaetze == null) {
            List<String> l = new ArrayList<>();
            for (String datei : new String[] { "index.html", "index_en.html" }) {
                try (java.io.InputStream in = RegelnStandard.class.getResourceAsStream("/filius/modern/web/startseite/" + datei)) {
                    if (in != null) {
                        l.addAll(absaetze(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
                    }
                } catch (Exception e) {
                    // not known
                }
            }
            startseitenAbsaetze = l;
        }
        return startseitenAbsaetze;
    }

    /** a paragraph the student wrote (not one of the start page), or null */
    static String eigenerAbsatz(String html) {
        for (String a : absaetze(html)) {
            if (a.length() >= 5 && !startseitenAbsaetze().contains(a)) {
                return a;
            }
        }
        return null;
    }

    private static void kapitel3() {
        def("3.1", Regel.zustand(n -> {
            Knoten ws = n.knoten("Webserver");
            return ws instanceof Rechner && Netz.fest(ws, WEBSERVER, MASKE) && amSwitch(n, ws, switchSchule(n));
        }));
        def("3.2", Regel.zustand(n -> Netz.laeuft(webserver(n), Netz.WEBSERVER)));
        def("3.3", Regel.zustand(n -> Netz.hat(laptop(n, 2), Netz.WEBBROWSER)).und((n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, Regeln::startseite)));
        def("3.4", Regel.zustand(n -> {
            String h1 = Netz.h1(Netz.datei(webserver(n), "webserver/index.html"));
            return !h1.isEmpty() && !Regeln.startseitenUeberschrift(h1);
        }));
        def("3.5", Regel.zustand(n -> eigenerAbsatz(Netz.datei(webserver(n), "webserver/index.html")) != null).und((n, e, s) -> {
            String absatz = eigenerAbsatz(Netz.datei(webserver(n), "webserver/index.html"));
            return absatz != null && http(e, s, laptop(n, 2), WEBSERVER, x -> Regeln.startseite(x) && absaetze(x.inhalt.toString()).contains(absatz));
        }));
        def("3.6", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 2)) && e.gibt(TCP_VERBUNDEN, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER))));
        def("3.7", Regel.nachweis((n, e, s) -> fenster(e, s, laptop(n, 2)) && http(e, s, laptop(n, 2), WEBSERVER, null)));
    }

    // ================================================================== chapter 4

    /** the router "Router" with exactly three cards: school, RGG and DNS provider; none keeps 192.168.0.10 */
    static boolean routerOk(Netz n) {
        Knoten r = router(n);
        if (r == null || !Netz.heisst(r, "Router")) {
            return false;
        }
        List<NetzwerkInterface> karten = Netz.nics(r);
        if (karten.size() != 3) {
            return false;
        }
        for (NetzwerkInterface nic : karten) {
            if (FILIUS_VORGABE.equals(nic.getIp()) || !MASKE.equals(nic.getSubnetzMaske())) {
                return false;
            }
        }
        return karteAn(n, r, ROUTER_SCHULE, switchSchule(n)) && Netz.nic(r, ROUTER_RGG) != null && Netz.nic(r, ROUTER_DNS) != null;
    }

    private static void kapitel4() {
        def("4.1", Regel.zustand(RegelnStandard::routerOk));
        def("4.2", Regel.zustand(n -> {
            List<Knoten> l = schulLaptops(n);
            if (l.size() < 4 || webserver(n) == null) {
                return false;
            }
            for (Knoten k : l) {
                if (!ROUTER_SCHULE.equals(Netz.gateway(k))) {
                    return false;
                }
            }
            return ROUTER_SCHULE.equals(Netz.gateway(webserver(n)));
        }));
        def("4.3", Regel.zustand(n -> {
            Knoten rgg = n.knoten("Switch RGG");
            Knoten shop = n.knoten("Shop");
            Knoten buero = n.knoten("Büro-Laptop");
            return routerOk(n) && rgg instanceof Switch && karteAn(n, router(n), ROUTER_RGG, rgg) && shop instanceof Rechner && buero instanceof Notebook
                    && fest(shop, SHOP, ROUTER_RGG) && fest(buero, BUERO, ROUTER_RGG) && amSwitch(n, shop, rgg) && amSwitch(n, buero, rgg);
        }));
        def("4.4", Regel.zustand(n -> Netz.laeuft(shop(n), Netz.WEBSERVER)
                && Netz.h1(Netz.datei(shop(n), "webserver/index.html")).toLowerCase(Locale.ROOT).contains("ravensberger24")));
        def("4.5", Regel.nachweis((n, e, s) -> pingOk(e, s, laptop(n, 1), SHOP) && http(e, s, laptop(n, 2), SHOP, x -> Regeln.seiteMit(x, "Ravensberger24"))));
        // experiment: a ping to the shop while Laptop-3 had no gateway; afterwards the gateway is back
        def("4.6", Regel.zustand(n -> ROUTER_SCHULE.equals(Netz.gateway(laptop(n, 3)))).und((n, e, s) -> {
            Knoten l3 = laptop(n, 3);
            return ping(e, 0, l3, SHOP, x -> x.typ.isEmpty()) != null || e.gibt(PING_OHNE_ANTWORT, 0, x -> x.knoten == l3 && x.ziel.equals(SHOP));
        }));
        def("4.7", Regel.zustand(n -> Regeln.etikett(switchRgg(n), Regeln.mitAdressen(RGGNETZ))).antwort((n, e, a) -> Regeln.ipAntwort(a, RGGNETZ)));
    }

    // ================================================================== chapter 5

    private static void kapitel5() {
        def("5.1", Regel.zustand(n -> {
            Knoten sw = n.knoten("Switch DNS-Anbieter");
            Knoten d = n.knoten("DNS-Server");
            return sw instanceof Switch && karteAn(n, router(n), ROUTER_DNS, sw) && d instanceof Rechner && fest(d, DNS_SERVER, ROUTER_DNS) && amSwitch(n, d, sw);
        }));
        def("5.2", Regel.zustand(n -> {
            Knoten d = dnsServer(n);
            return Netz.laeuft(d, Netz.DNSSERVER) && Netz.record(d, WWW_SCHULE, "A").contains(WEBSERVER) && Netz.record(d, WWW_SHOP, "A").contains(SHOP);
        }));
        def("5.3", Regel.zustand(n -> {
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
        def("5.4", Regel.nachweis((n, e, s) -> befehl(e, s, laptop(n, 1), "nslookup") && dnsAntwort(e, s, laptop(n, 1), WWW_SHOP, SHOP))
                .antwort((n, e, a) -> Regeln.ipAntwort(a, SHOP)));
        def("5.5", Regel.nachweis((n, e, s) -> dnsAntwort(e, s, laptop(n, 2), WWW_SHOP, SHOP) && http(e, s, laptop(n, 2), SHOP, x -> x.host.startsWith(WWW_SHOP))
                && dnsAntwort(e, s, buero(n), WWW_SCHULE, WEBSERVER) && http(e, s, buero(n), WEBSERVER, x -> x.host.startsWith(WWW_SCHULE))));
        def("5.6", Regel.zustand(n -> Regeln.etikett(switchDns(n), Regeln.mitAdressen(DNSNETZ))));
    }

    // ================================================================== chapter 6

    static boolean mailserverOk(Netz n, Knoten k, String ip, String gw, Knoten sw, String domain, String... konten) {
        if (!(k instanceof Rechner) || !fest(k, ip, gw) || !DNS_SERVER.equals(Netz.dns(k)) || sw == null || !n.segment(sw).contains(k)) {
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
            if (!Regeln.hatKonto(m, konto)) {
                return false;
            }
        }
        return true;
    }

    /** the e-mail program of the device has the account; the server is given by its name or address */
    static boolean programmOk(Knoten k, String adresse, String server, String serverIp) {
        Anwendung a = Netz.software(k, Netz.EMAILPROGRAMM);
        if (!(a instanceof EmailAnwendung)) {
            return false;
        }
        Map<String, EmailKonto> konten = ((EmailAnwendung) a).holeKontoListe();
        if (konten == null || konten.isEmpty()) {
            // after opening a project the account is read from the file system when the program window opens
            ((EmailAnwendung) a).laden();
            konten = ((EmailAnwendung) a).holeKontoListe();
        }
        if (konten == null) {
            return false;
        }
        for (EmailKonto konto : konten.values()) {
            String smtp = Netz.name(konto.getSmtpserver());
            String imap = Netz.name(konto.getPop3server());
            if (adresse.equalsIgnoreCase(String.valueOf(konto.getEmailAdresse()).trim()) && (server.equalsIgnoreCase(smtp) || serverIp.equals(smtp))
                    && (server.equalsIgnoreCase(imap) || serverIp.equals(imap))) {
                return true;
            }
        }
        return false;
    }

    private static void kapitel6() {
        String anna = "anna@" + DOMAIN_SCHULE;
        String ben = "ben@" + DOMAIN_SCHULE;
        String verkauf = "verkauf@" + DOMAIN_RGG;
        def("6.1", Regel.zustand(n -> mailserverOk(n, n.knoten("Mailserver RGG"), MAIL_RGG, ROUTER_RGG, switchRgg(n), DOMAIN_RGG, "verkauf")));
        def("6.2", Regel.zustand(n -> Netz.record(dnsServer(n), "mail." + DOMAIN_RGG, "A").contains(MAIL_RGG)));
        def("6.3", Regel.zustand(n -> programmOk(buero(n), verkauf, "mail." + DOMAIN_RGG, MAIL_RGG)));
        // a test mail to the own mailbox and fetched afterwards
        def("6.4", Regel.nachweis((n, e, s) -> {
            Ereignis mail = smtpEreignis(e, s, MAIL_RGG, null, verkauf, verkauf);
            return mail != null && abruf(e, mail.zeit, MAIL_RGG, "verkauf", buero(n)) != null;
        }));
        def("6.5", Regel.zustand(n -> mailserverOk(n, n.knoten("Mailserver Schule"), MAIL_SCHULE, ROUTER_SCHULE, switchSchule(n), DOMAIN_SCHULE, "anna", "ben")
                && Netz.record(dnsServer(n), "mail." + DOMAIN_SCHULE, "A").contains(MAIL_SCHULE)));
        // Anna writes to Ben, Ben fetches the mail on Laptop-3 (version 30)
        def("6.6", Regel.zustand(n -> programmOk(laptop(n, 1), anna, "mail." + DOMAIN_SCHULE, MAIL_SCHULE)).und((n, e, s) -> {
            Ereignis mail = smtpEreignis(e, s, MAIL_SCHULE, null, anna, ben);
            return mail != null && abruf(e, mail.zeit, MAIL_SCHULE, "ben", laptop(n, 3)) != null;
        }));
        def("6.7", Regel.zustand(n -> {
            Knoten d = dnsServer(n);
            return Netz.record(d, DOMAIN_SCHULE, "MX").contains("mail." + DOMAIN_SCHULE) && Netz.record(d, DOMAIN_RGG, "MX").contains("mail." + DOMAIN_RGG);
        }));
        // the order: Anna -> sales (through both mail servers), fetched in the office, answer, fetched on Laptop-1
        def("6.8", Regel.nachweis((n, e, s) -> {
            Ereignis bestellung = smtpEreignis(e, s, MAIL_RGG, MAIL_SCHULE, anna, verkauf);
            Ereignis abgerufen = bestellung == null ? null : abruf(e, bestellung.zeit, MAIL_RGG, "verkauf", buero(n));
            Ereignis antwort = abgerufen == null ? null : smtpEreignis(e, abgerufen.zeit, MAIL_SCHULE, MAIL_RGG, verkauf, anna);
            return antwort != null && abruf(e, antwort.zeit, MAIL_SCHULE, "anna", laptop(n, 1)) != null;
        }));
    }

    // ================================================================== chapter 7

    static boolean dhcpOk(Netz n) {
        DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
        return s != null && s.isAktiv() && DHCP_VON.equals(s.getUntergrenze()) && DHCP_BIS.equals(s.getObergrenze()) && MASKE.equals(s.getSubnetzmaske())
                && ROUTER_SCHULE.equals(s.determineGatewayip()) && DNS_SERVER.equals(s.determineDnsserverip());
    }

    private static void kapitel7() {
        def("7.1", Regel.zustand(n -> {
            Knoten d = n.knoten("DHCP-Server");
            return d instanceof Rechner && fest(d, DHCP_SERVER, ROUTER_SCHULE) && DNS_SERVER.equals(Netz.dns(d)) && n.segment(switchSchule(n)).contains(d);
        }));
        def("7.2", Regel.zustand(RegelnStandard::dhcpOk));
        def("7.3", Regel.zustand(n -> {
            List<Knoten> l = schulLaptops(n);
            return l.size() == 4 && l.stream().allMatch(Netz::dhcp);
        }));
        def("7.4", Regel.nachweis((n, e, s) -> befehl(e, s, laptop(n, 2), "ipconfig")).antwort((n, e, a) -> {
            String ip = Netz.ip(laptop(n, 2));
            return Netz.imBereich(ip, DHCP_VON, DHCP_BIS) && Regeln.ipAntwort(a, ip);
        }));
        // Laptop-4 is renamed "Lehrer-Laptop" and always gets 192.168.0.13 (version 30)
        def("7.5", Regel.zustand(n -> n.knoten("Lehrer-Laptop") != null
                && LEHRER.equals(Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(lehrerLaptop(n))))));
        def("7.6", Regel.zustand(n -> dhcpOk(n) && Netz.dhcp(lehrerLaptop(n)) && LEHRER.equals(Netz.ip(lehrerLaptop(n)))).und((n, e, s) -> {
            Knoten l1 = laptop(n, 1);
            return Netz.dhcp(l1) && http(e, s, l1, SHOP, x -> x.host.startsWith(WWW_SHOP)) && abruf(e, s, MAIL_SCHULE, "anna", l1) != null;
        }));
        def("7.7", Regel.zustand(n -> Regeln.etikett(switchSchule(n), Regeln.mitAdressen(DHCP_VON, DHCP_BIS))));
    }

    // ================================================================== chapter 8

    /** the gateway of the RGG devices is the address of the router card in the RGG network */
    static boolean rggGatewayOk(Netz n) {
        Knoten r = router(n);
        Knoten rgg = switchRgg(n);
        if (r == null || rgg == null) {
            return false;
        }
        String karte = null;
        for (NetzwerkInterface nic : Netz.nics(r)) {
            if (n.gegenueber(nic) == rgg) {
                karte = nic.getIp();
            }
        }
        if (karte == null || !inNetz(karte, RGGNETZ)) {
            return false;
        }
        for (Knoten k : new Knoten[] { shop(n), buero(n), mailRgg(n) }) {
            if (k == null || !karte.equals(Netz.gateway(k))) {
                return false;
            }
        }
        return true;
    }

    private static void kapitel8() {
        // part A: each fault is fixed and the test from the user's device was observed afterwards
        def("A1", Regel.zustand(n -> {
            Knoten l2 = laptop(n, 2);
            return l2 != null && inNetz(Netz.ip(l2), SCHULNETZ) && (Netz.dhcp(l2) || ROUTER_SCHULE.equals(Netz.gateway(l2)));
        }).und((n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, null)));
        def("A2", Regel.zustand(n -> {
            DHCPServer d = Netz.dhcpServer(dhcpServerPc(n));
            return d != null && ROUTER_SCHULE.equals(d.determineGatewayip());
        }).und((n, e, s) -> e.gibt(PING_ANTWORT, s, x -> schulLaptops(n).contains(x.knoten) && Netz.dhcp(x.knoten) && inNetz(x.ziel, RGGNETZ))));
        def("A3", Regel.zustand(n -> {
            List<String> a = Netz.record(dnsServer(n), WWW_SHOP, "A");
            return !a.isEmpty() && a.stream().allMatch(SHOP::equals);
        }).und((n, e, s) -> http(e, s, buero(n), SHOP, x -> x.host.startsWith(WWW_SHOP))));
        def("A4", Regel.zustand(n -> Netz.laeuft(shop(n), Netz.WEBSERVER)).und((n, e, s) -> http(e, s, buero(n), SHOP, null)));
        def("A5", Regel.zustand(n -> Netz.record(dnsServer(n), DOMAIN_RGG, "MX").contains("mail." + DOMAIN_RGG))
                .und((n, e, s) -> smtp(e, s, MAIL_RGG, MAIL_SCHULE, null, "verkauf@" + DOMAIN_RGG)));
        def("A6", Regel.zustand(n -> Netz.record(dnsServer(n), WWW_SCHULE, "A").contains(WEBSERVER))
                .und((n, e, s) -> http(e, s, buero(n), WEBSERVER, x -> x.host.startsWith(WWW_SCHULE))));
        def("A7", Regel.zustand(RegelnStandard::rggGatewayOk).und((n, e, s) -> pingOk(e, s, buero(n), WEBSERVER)));
        def("A8", Regel.zustand(n -> {
            Anwendung a = Netz.software(mailSchule(n), Netz.EMAILSERVER);
            return a instanceof EmailServer && ((EmailServer) a).isAktiv();
        }).und((n, e, s) -> e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("FETCH"))));
        def("A9", Regel.zustand(n -> amSwitch(n, laptop(n, 3), switchSchule(n))).und((n, e, s) -> pingOk(e, s, laptop(n, 3), ROUTER_SCHULE)));

        // part B: the network plan
        def("B1", Regel.zustand(n -> Regeln.etikett(switchSchule(n), Regeln.mitAdressen(SCHULNETZ)) && Regeln.etikett(switchRgg(n), Regeln.mitAdressen(RGGNETZ))
                && Regeln.etikett(switchDns(n), Regeln.mitAdressen(DNSNETZ))));
        def("B2", Regel.zustand(RegelnStandard::routerBeschriftet));
        def("B3", Regel.zustand(n -> true));
    }

    /** a text field near the router with its three addresses */
    static boolean routerBeschriftet(Netz n) {
        Knoten r = router(n);
        Rectangle bild = null;
        for (GUIKnotenItem item : GUIContainer.getGUIContainer().getKnotenItems()) {
            if (item.getKnoten() == r) {
                bild = item.getImageLabel().getBounds();
            }
        }
        if (bild == null) {
            return false;
        }
        for (GUIDocuItem feld : Regeln.textfelder()) {
            Rectangle f = new Rectangle(feld.getX(), feld.getY(), feld.getWidth(), feld.getHeight());
            double dx = Math.max(0, Math.max(f.x - (bild.x + bild.width), bild.x - (f.x + f.width)));
            double dy = Math.max(0, Math.max(f.y - (bild.y + bild.height), bild.y - (f.y + f.height)));
            if (Math.hypot(dx, dy) <= 150 && Regeln.mitAdressen(ROUTER_SCHULE, ROUTER_RGG, ROUTER_DNS).test(feld.getText())) {
                return true;
            }
        }
        return false;
    }
}
