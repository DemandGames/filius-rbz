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
import static filius.modern.lernpfad.Teile.adresse;
import static filius.modern.lernpfad.Teile.anSwitch;
import static filius.modern.lernpfad.Teile.austausch;
import static filius.modern.lernpfad.Teile.da;
import static filius.modern.lernpfad.Teile.dhcpAn;
import static filius.modern.lernpfad.Teile.dns;
import static filius.modern.lernpfad.Teile.eigeneUeberschrift;
import static filius.modern.lernpfad.Teile.mitEtikett;
import static filius.modern.lernpfad.Teile.flaeche;
import static filius.modern.lernpfad.Teile.g;
import static filius.modern.lernpfad.Teile.gateway;
import static filius.modern.lernpfad.Teile.installiert;
import static filius.modern.lernpfad.Teile.laeuft;
import static filius.modern.lernpfad.Teile.name;
import static filius.modern.lernpfad.Teile.pingt;
import static filius.modern.lernpfad.Teile.t;
import static filius.modern.lernpfad.Teile.tippt;

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
import filius.modern.lernpfad.Teile.G;

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

    /** the first such mail (a chain "sent, fetched, answered" starts with the earliest one, so a second mail does not break it) */
    static Ereignis smtpEreignis(Ereignisse e, long seit, String server, String client, String von, String an) {
        return e.erstes(SMTP, seit, x -> x.ziel.equals(server) && (client == null || x.quelle.equals(client)) && (von == null || x.werte.size() > 0 && x.werte.get(0).equals(von))
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

    /**
     * The answer is the address the device got from DHCP: the one it has now or the one it had when `ipconfig`
     * was typed on it (after a restart of the simulation DHCP may hand out another one), inside the range.
     */
    static boolean dhcpAntwort(Ereignisse e, Knoten k, String antwort, String von, String bis) {
        if (k == null) {
            return false;
        }
        List<String> adressen = new ArrayList<>();
        adressen.add(Netz.ip(k));
        for (Ereignis x : e.finde(BEFEHL, 0, y -> y.knoten == k && y.name.equals("ipconfig"))) {
            adressen.add(x.quelle);
        }
        for (String ip : adressen) {
            if (Netz.imBereich(ip, von, bis) && ipAntwort(antwort, ip)) {
                return true;
            }
        }
        return false;
    }

    /** the label of the structure field around the switch as the hint shows it ("no field", "no label" or the text) */
    static String etikettText(Knoten sw) {
        GUIDocuItem flaeche = bereichFeld(sw);
        if (flaeche == null) {
            return Teile.t("noch keine Fläche um den Switch dieses Netzes", "no field around the switch of this network yet");
        }
        String titel = flaeche.getText();
        List<String> texte = new ArrayList<>();
        if (titel != null && !titel.isBlank()) {
            texte.add("„" + titel.trim() + "“");
        }
        Rectangle bereich = new Rectangle(flaeche.getX(), flaeche.getY(), flaeche.getWidth(), flaeche.getHeight());
        for (GUIDocuItem feld : textfelder()) {
            double fx = feld.getX() + Math.min(feld.getWidth(), 60) / 2.0;
            double fy = feld.getY() + feld.getHeight() / 2.0;
            if (bereich.contains(fx, fy) && feld.getText() != null && !feld.getText().isBlank()) {
                texte.add("„" + feld.getText().trim().replaceAll("\\s+", " ") + "“");
            }
        }
        return texte.isEmpty() ? Teile.t("die Fläche hat noch kein Etikett", "the field has no label yet") : String.join(", ", texte);
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

    // ================================================================== steps of the tasks (shared parts)

    // ------------------------------------------------------------------ devices as the hints name them

    private static final G L1 = g("Laptop-1", n -> laptop(n, 1));
    private static final G L2 = g("Laptop-2", n -> laptop(n, 2));
    private static final G L3 = g("Laptop-3", n -> laptop(n, 3));
    private static final G L4 = g("Laptop-4", n -> laptop(n, 4));
    private static final G WS = g("Webserver", "Web server", Regeln::webserver);
    private static final G SHOPG = g("Shop", Regeln::shop);
    private static final G BUE = g("Büro-Laptop", "Office laptop", Regeln::buero);
    private static final G R1G = g("R1 Router Schule", "R1 School router", Regeln::r1);
    private static final G DNSG = g("DNS-Server", "DNS server", Regeln::dnsServer);
    private static final G DHCPG = g("DHCP-Server", "DHCP server", Regeln::dhcpServerPc);
    private static final G HR = g("Heimrouter", "Home router", Regeln::heimrouter);
    private static final G HEIML = g("Heim-Laptop", "Home laptop", Regeln::heimLaptop);
    private static final G TAB = g("Tablet", Regeln::tablet);
    private static final G MS = g("Mailserver Schule", "School mail server", Regeln::mailSchule);
    private static final G MR = g("Mailserver RGG", "RGG mail server", Regeln::mailRgg);
    private static final G BLOGG = g("Blog-Server", "Blog server", n -> {
        Knoten b = n.knoten("Blog-Server");
        return b instanceof Rechner ? b : n.mitIp(BLOG);
    });
    private static final G INTRA = g("Intranet", Regeln::intranet);
    private static final G VPNS = g("VPN-Server", "VPN server", Regeln::vpnServer);
    private static final G HO = g("Homeoffice-Laptop", "Home office laptop", Regeln::homeoffice);

    /** the device with exactly this name (the task asks for the name) */
    private static G nur(String de, String en) {
        return g(de, en, n -> n.knoten(de));
    }

    private static G nur(String name) {
        return nur(name, name);
    }

    private static Regel regel(List<Schritt> s) {
        return Regel.schritte(s.toArray(new Schritt[0]));
    }

    /** the device exists, is of this kind and has the name of the task (it may have been found by its address) */
    private static Schritt stehtMitNamen(G g, Class<? extends Knoten> art, String name) {
        Schritt s = da(g, art);
        return Schritt.z(s.text(false), s.text(true), n -> art.isInstance(g.in(n)) && Netz.heisst(g.in(n), name))
                .ist(n -> g.in(n) == null ? null : "„" + g.in(n).getName() + "“");
    }

    /** an A or MX record of the DNS server; the text does not always give the value away */
    private static Schritt eintrag(String de, String en, String typ, String name, String wert) {
        return Schritt.z(de, en, n -> Netz.record(dnsServer(n), name, typ).contains(wert)).ist(n -> {
            List<String> w = Netz.record(dnsServer(n), name, typ);
            return w.isEmpty() ? t("kein " + typ + "-Eintrag für " + name, "no " + typ + " record for " + name) : name + " → " + String.join(", ", w);
        });
    }

    private static EmailServer mailProgramm(Knoten k) {
        Anwendung a = Netz.software(k, Netz.EMAILSERVER);
        return a instanceof EmailServer ? (EmailServer) a : null;
    }

    /** the steps of a mail server: PC with its name, addresses, cable, program, domain, accounts, started */
    private static List<Schritt> mailserver(G k, String name, String ip, String gw, String swDe, String swEn, java.util.function.Function<Netz, Knoten> sw, String domain,
            String... konten) {
        List<Schritt> l = new ArrayList<>();
        l.add(stehtMitNamen(k, Rechner.class, name));
        l.add(adresse(k, ip));
        l.add(gateway(k, gw));
        l.add(dns(k, DNS_SERVER));
        l.add(Schritt.z(k.de + " mit einem Kabel an den " + swDe + " anschließen", "Connect " + k.en + " to the " + swEn + " with a cable",
                n -> sw.apply(n) != null && n.segment(sw.apply(n)).contains(k.in(n))));
        l.add(installiert(k, Netz.EMAILSERVER));
        l.add(Schritt.z(k.de + ": im E-Mail-Server die Maildomain " + domain + " eintragen (geht nur bei angehaltenem Server)",
                k.en + ": enter the mail domain " + domain + " in the email server (only while the server is stopped)",
                n -> mailProgramm(k.in(n)) != null && domain.equalsIgnoreCase(String.valueOf(mailProgramm(k.in(n)).getMailDomain()).trim()))
                .ist(n -> mailProgramm(k.in(n)) == null ? null : String.valueOf(mailProgramm(k.in(n)).getMailDomain())));
        for (String konto : konten) {
            l.add(Schritt.z(k.de + ": im E-Mail-Server das Konto " + konto + " anlegen", k.en + ": add the account " + konto + " in the email server",
                    n -> mailProgramm(k.in(n)) != null && hatKonto(mailProgramm(k.in(n)), konto)));
        }
        l.add(Schritt.z(k.de + ": den E-Mail-Server starten", k.en + ": start the email server",
                n -> mailProgramm(k.in(n)) != null && mailProgramm(k.in(n)).isAktiv()));
        return l;
    }

    /** the e-mail program of the device has the account, with the mail server given by its name */
    private static Schritt konto(G k, String adresse, String server) {
        return Schritt.z(k.de + ": im E-Mail-Programm das Konto " + adresse + " einrichten, als SMTP- und IMAP-Server jeweils " + server,
                k.en + ": set up the account " + adresse + " in the email program, with " + server + " as SMTP and as IMAP server",
                n -> programmOk(k.in(n), adresse, server));
    }

    /** a fault of the final project: the cause is fixed, then the test of the ticket */
    private static Regel stoerung(Regel.Zustand behoben, String testDe, String testEn, Regel.Nachweis test) {
        return Regel.schritte(Schritt.z("Die Ursache finden und beheben", "Find the cause and fix it", behoben),
                Schritt.n("Danach testen: " + testDe, "Then test: " + testEn, test));
    }

    /** a web page was opened by the device: text of the step and the check */
    private static Schritt seite(String de, String en, Regel.Nachweis n) {
        return Schritt.n(de, en, n);
    }

    /** the encryption of the school web server is set up as in task 4.3 (one step, for the tasks after it) */
    private static Schritt httpsSteht() {
        return Schritt.z("Die Verschlüsselung am Webserver ist eingerichtet wie in Aufgabe 4.3 (HTTPS an, Zertifikat für " + WEBSERVER + ", nicht selbst signiert)",
                "The encryption of the web server is set up as in task 4.3 (HTTPS on, certificate for " + WEBSERVER + ", not self-signed)", n -> httpsOk(n, WEBSERVER));
    }

    private static String zertifikatNamen(Netz n) {
        filius.software.www.WebServer w = webserverProgramm(n);
        if (w == null) {
            return null;
        }
        List<String> namen = filius.modern.tls.Tls.namen(w.getZertifikatNamen(), null);
        return namen.isEmpty() ? t("das Feld ist leer", "the field is empty") : String.join(", ", namen);
    }

    /** the cards of a router as the hint shows them */
    private static String karten(Knoten router) {
        List<String> l = new ArrayList<>();
        for (NetzwerkInterface nic : Netz.nics(router)) {
            l.add(nic.getIp() + " / " + nic.getSubnetzMaske());
        }
        return l.isEmpty() ? null : t("Karten: ", "cards: ") + String.join("; ", l);
    }

    /** a setting of a DHCP server (of the school PC or of the home router) */
    private static Schritt dhcp(java.util.function.Function<Netz, Knoten> geraet, String de, String en, java.util.function.Predicate<DHCPServer> ok,
            java.util.function.Function<DHCPServer, String> ist) {
        return Schritt.z(de, en, n -> {
            DHCPServer s = Netz.dhcpServer(geraet.apply(n));
            return s != null && ok.test(s);
        }).ist(n -> {
            DHCPServer s = Netz.dhcpServer(geraet.apply(n));
            return s == null || ist == null ? null : ist.apply(s);
        });
    }

    private static Knoten heimSwitch(Netz n) {
        Knoten h = heimrouter(n);
        return h instanceof Gateway ? n.gegenueber(((Gateway) h).holeLANInterface()) : null;
    }

    /** a step about the WAN or LAN card of the home router */
    private static Schritt heimKarte(String de, String en, java.util.function.BiPredicate<Netz, Gateway> ok, java.util.function.Function<Gateway, String> ist) {
        return Schritt.z(de, en, n -> heimrouter(n) instanceof Gateway && ok.test(n, (Gateway) heimrouter(n)))
                .ist(n -> !(heimrouter(n) instanceof Gateway) || ist == null ? null : ist.apply((Gateway) heimrouter(n)));
    }

    /** the IMAP event came from this device (its address may have changed since: DHCP) */
    static boolean kommtVon(Ereignis x, Knoten k) {
        return x.gegenstelle == k || x.gegenstelle == null && x.quelle.equals(Netz.ip(k));
    }

    /** the port forwardings of the home router as the hint shows them */
    private static String freigaben(Netz n) {
        List<String> l = new ArrayList<>();
        for (String[] f : Netz.portfreigaben(heimrouter(n))) {
            if (f != null && f.length >= 4) {
                l.add(f[0].trim() + " " + f[1].trim() + " → " + f[2].trim() + ":" + f[3].trim());
            }
        }
        return l.isEmpty() ? t("noch keine Portfreigabe", "no port forwarding yet") : String.join("; ", l);
    }

    /** the first VPN Accept the computer received since then (a later reconnect does not start a test anew) */
    static Ereignis vpnErster(Ereignisse e, long seit, Knoten k) {
        return k == null ? null : e.erstes(VPN, seit, x -> x.knoten == k && "Accept".equals(x.name) && "empfangen".equals(x.typ));
    }

    /** what the firewall of R2 blocks although it has to stay open, for the hint */
    private static String zuViel(Netz n) {
        Firewall fw = Netz.firewall(r2(n));
        if (fw == null) {
            return null;
        }
        List<String> l = new ArrayList<>();
        if (sperrt(fw, HEIM_WAN, SHOP, 6, 80)) {
            l.add(t("Shop (TCP 80)", "shop (TCP 80)"));
        }
        if (sperrt(fw, MAIL_SCHULE, MAIL_RGG, 6, 25)) {
            l.add(t("Mails an die RGG (TCP 25)", "mails to the RGG (TCP 25)"));
        }
        if (sperrt(fw, HEIM_WAN, VPN_SERVER, 17, filius.software.vpn.Vpn.PORT)) {
            l.add("VPN (UDP " + filius.software.vpn.Vpn.PORT + ")");
        }
        if (sperrt(fw, DNS_SERVER, BUERO, 17, 53)) {
            l.add(t("DNS-Antworten (UDP 53)", "DNS answers (UDP 53)"));
        }
        return l.isEmpty() ? null : t("gesperrt ist auch: ", "also blocked: ") + String.join(", ", l);
    }

    private static Schritt vpnVerbunden() {
        return Schritt.n("Homeoffice-Laptop: mit dem VPN-Client verbinden (nach jedem Neustart der Simulation neu) – er muss „Verbunden“ melden",
                "Home office laptop: connect with the VPN client (anew after every restart of the simulation) – it must report “Connected”",
                (n, e, s) -> vpnErster(e, s, homeoffice(n)) != null);
    }

    /** a setting of the VPN server program */
    private static Schritt vpn(String de, String en, Predicate<filius.software.vpn.VpnServer> ok, java.util.function.Function<filius.software.vpn.VpnServer, String> ist) {
        return Schritt.z(de, en, n -> {
            Anwendung a = Netz.software(vpnServer(n), Netz.VPNSERVER);
            return a instanceof filius.software.vpn.VpnServer && ok.test((filius.software.vpn.VpnServer) a);
        }).ist(n -> {
            Anwendung a = Netz.software(vpnServer(n), Netz.VPNSERVER);
            return a instanceof filius.software.vpn.VpnServer && ist != null ? ist.apply((filius.software.vpn.VpnServer) a) : null;
        });
    }

    /** 11.9: the home office laptop got access and its mail program reached the RGG mail server through the tunnel */
    private static boolean vpnMailsDirekt(Netz n, Ereignisse e, long s) {
        Knoten h = homeoffice(n);
        Knoten v = vpnServer(n);
        Ereignis z = vpnErster(e, s, h);
        return z != null && v != null && e.gibt(TCP_VERBUNDEN, z.zeit, x -> x.knoten == h && x.ziel.equals(MAIL_RGG) && x.port == 143)
                && imapVon(e, z.zeit, Netz.ip(v), "verkauf", "LOGIN", "FETCH");
    }

    /** VPN end-to-end test: the home office laptop got access and fetched the mails of verkauf through the tunnel */
    static boolean vpnMails(Netz n, Ereignisse e, long s) {
        Knoten h = homeoffice(n);
        Knoten v = vpnServer(n);
        Ereignis z = vpnErster(e, s, h);
        return z != null && v != null && imapVon(e, z.zeit, Netz.ip(v), "verkauf", "LOGIN", "FETCH");
    }

    // ================================================================== chapter 1

    private static void kapitel1() {
        G l1 = nur("Laptop-1");
        G l2 = nur("Laptop-2");
        def("1.1", Regel.schritte(da(L1, Notebook.class), da(l2, Notebook.class)));
        def("1.2", Regel.schritte(Schritt.z("Laptop-1 und Laptop-2 direkt mit einem Kabel verbinden", "Connect Laptop-1 and Laptop-2 directly with a cable",
                n -> n.verbunden(laptop(n, 1), n.knoten("Laptop-2")))));
        def("1.3", Regel.schritte(adresse(l1, "192.168.10.11"), adresse(l2, "192.168.10.12")));
        def("1.4", Regel.schritte(installiert(L1, Netz.TERMINAL)));
        def("1.5", Regel.schritte(tippt(L1, "ipconfig", "ipconfig")));
        def("1.6", Regel.schritte(
                Schritt.z("Laptop-2: IPv4-Adresse 192.168.10.12 eintragen", "Laptop-2: enter the IPv4 address 192.168.10.12",
                        n -> laptop(n, 1) != null && laptop(n, 2) != null && Netz.ip(laptop(n, 2)).equals("192.168.10.12"))
                        .ist(n -> laptop(n, 2) == null ? null : Netz.ip(laptop(n, 2))),
                pingt(L1, "192.168.10.12")));
        def("1.7", Regel.schritte(austausch(L1), Schritt.n("Laptop-1: Laptop-2 noch einmal anpingen (`ping 192.168.10.12`)",
                "Laptop-1: ping Laptop-2 once more (`ping 192.168.10.12`)", (n, e, s) -> e.gibt(PING_ANTWORT, s, x -> x.knoten == laptop(n, 1)))));
    }

    // ================================================================== chapter 2

    private static void kapitel2() {
        java.util.function.Function<Netz, Knoten> sw = n -> n.knoten("Switch Schule");
        G l3 = nur("Laptop-3");
        G l4 = nur("Laptop-4");
        def("2.1", Regel.schritte(
                Schritt.z("Das Kabel zwischen Laptop-1 und Laptop-2 entfernen (Rechtsklick auf einen der Laptops)",
                        "Remove the cable between Laptop-1 and Laptop-2 (right-click one of the laptops)",
                        n -> laptop(n, 1) != null && laptop(n, 2) != null && !n.verbunden(laptop(n, 1), laptop(n, 2))),
                da(nur("Switch Schule", "School switch"), Switch.class)));
        def("2.2", Regel.schritte(anSwitch(L1, "Switch Schule", "School switch", sw), anSwitch(L2, "Switch Schule", "School switch", sw)));
        def("2.3", Regel.schritte(da(l3, Notebook.class), adresse(l3, "192.168.10.13"), anSwitch(l3, "Switch Schule", "School switch", sw), da(l4, Notebook.class),
                adresse(l4, "192.168.10.14"), anSwitch(l4, "Switch Schule", "School switch", sw)));
        def("2.4", Regel.schritte(anSwitch(L1, "Switch Schule", "School switch", Regeln::switchSchule), anSwitch(L4, "Switch Schule", "School switch", Regeln::switchSchule),
                pingt(L1, "192.168.10.14")));
        def("2.5", Regel.schritte(tippt(L1, "arp", "arp")).antwort((n, e, a) -> laptop(n, 4) != null && mac(a).equals(mac(Netz.mac(laptop(n, 4))))));
        def("2.6", Regel.schritte(Schritt.n("In der Simulation den Switch Schule anklicken, sodass seine Tabelle aufgeht (vorher von Laptop-1 aus pingen)",
                "Click the School switch in the simulation so that its table opens (ping from Laptop-1 before)",
                (n, e, s) -> e.gibt(SAT, s, x -> x.knoten == switchSchule(n)))).antwort((n, e, a) -> {
                    Knoten sch = switchSchule(n);
                    Knoten k = laptop(n, 4);
                    if (!(sch instanceof Switch) || k == null) {
                        return false;
                    }
                    String erwartet = satAnschluss((Switch) sch, Netz.mac(k));
                    if (erwartet == null) {
                        erwartet = String.valueOf(n.switchAnschluss((Switch) sch, k));
                    }
                    List<String> z = zahlen(a);
                    return z.size() == 1 && z.get(0).equals(erwartet);
                }));
        def("2.7", Regel.schritte(austausch(L1)));
        def("2.8", Regel.schritte(flaeche("Schulnetz", "school network", Regeln::switchSchule),
                mitEtikett("Oben links in die Fläche das Etikett „Schulnetz 192.168.10.0/24“ schreiben (Klick auf das +) – wichtig ist das /24",
                        "Write the label “School network 192.168.10.0/24” at the top left of the field (click the +) – the /24 matters", Regeln::switchSchule,
                        mitNetz(SCHULNETZ + "/24"))));
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
        G ws = nur("Webserver", "Web server");
        def("3.1", Regel.schritte(da(ws, Rechner.class), adresse(ws, WEBSERVER), anSwitch(ws, "Switch Schule", "School switch", Regeln::switchSchule)));
        def("3.2", Regel.schritte(installiert(WS, Netz.WEBSERVER), laeuft(WS, Netz.WEBSERVER)));
        def("3.3", Regel.schritte(installiert(L2, Netz.WEBBROWSER),
                seite("Laptop-2: im Webbrowser http://" + WEBSERVER + " aufrufen – die Startseite muss erscheinen",
                        "Laptop-2: open http://" + WEBSERVER + " in the web browser – the start page must appear",
                        (n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, 80, Regeln::startseite))));
        def("3.4", Regel.schritte(eigeneUeberschrift(WS)));
        def("3.5", Regel.schritte(eigeneUeberschrift(WS),
                seite("Laptop-2: die Seite http://" + WEBSERVER + " im Webbrowser neu laden – deine Überschrift muss erscheinen",
                        "Laptop-2: reload the page http://" + WEBSERVER + " in the web browser – your heading must appear", (n, e, s) -> {
                            String h1 = Netz.h1(Netz.datei(webserver(n), "webserver/index.html"));
                            return http(e, s, laptop(n, 2), WEBSERVER, 80, x -> startseite(x) && Netz.h1(x.inhalt.toString()).equals(h1));
                        })));
        def("3.6", Regel.schritte(
                Schritt.z("Webserver: mit dem Text-Editor die Datei `webserver/kontakt.html` anlegen („Speichern unter“), mit einer Überschrift (`<h1>`) und einem Satz",
                        "Web server: create the file `webserver/kontakt.html` with the text editor (“Save as”), with a heading (`<h1>`) and a sentence", n -> {
                            String kontakt = Netz.datei(webserver(n), "webserver/kontakt.html");
                            return kontakt != null && !Netz.h1(kontakt).isEmpty() && Netz.text(kontakt).length() > Netz.h1(kontakt).length() + 10;
                        }),
                Schritt.z("Webserver: auf der Startseite `webserver/index.html` einen Link zur Kontaktseite einbauen, z. B. `<a href=\"kontakt.html\">Kontakt</a>`",
                        "Web server: add a link to the contact page on the start page `webserver/index.html`, e.g. `<a href=\"kontakt.html\">Contact</a>`", n -> {
                            String index = Netz.datei(webserver(n), "webserver/index.html");
                            return index != null && index.toLowerCase(Locale.ROOT).matches("(?s).*href\\s*=\\s*[\"']?(\\./|/)?kontakt\\.html.*");
                        }),
                seite("Laptop-2: die Kontaktseite im Webbrowser aufrufen (über den Link auf der Startseite)",
                        "Laptop-2: open the contact page in the web browser (with the link on the start page)",
                        (n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, 80, x -> x.pfad.toLowerCase(Locale.ROOT).endsWith("kontakt.html")))));
        def("3.7", Regel.schritte(seite("Laptop-2: im Webbrowser http://" + WEBSERVER + "/gibtsnicht.html aufrufen", "Laptop-2: open http://" + WEBSERVER + "/gibtsnicht.html in the web browser",
                (n, e, s) -> laptop(n, 2) != null
                        && e.gibt(HTTP, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER) && x.status == 404 && x.pfad.contains("gibtsnicht"))))
                .antwort((n, e, a) -> zahlen(a).equals(List.of("404"))));
        def("3.8", Regel.schritte(austausch(L2), seite("Laptop-2: die Seite des Webservers noch einmal aufrufen", "Laptop-2: open the page of the web server once more",
                (n, e, s) -> e.gibt(TCP_VERBUNDEN, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER)))));
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
        def("4.1", Regel.schritte(seite("Laptop-1: im Webbrowser http://" + WEBSERVER + "/intern/ öffnen und mit Name anna und Passwort Schulfest anmelden",
                "Laptop-1: open http://" + WEBSERVER + "/intern/ in the web browser and sign in with the name anna and the password Schulfest",
                (n, e, s) -> http(e, s, laptop(n, 1), WEBSERVER, 80, Regeln::angemeldet))));
        def("4.2", Regel.schritte(austausch(L1)));
        def("4.3", Regel.schritte(
                Schritt.z("Webserver: im Fenster des Programms „Webserver“ den Haken „HTTPS anbieten (Port 443)“ setzen",
                        "Web server: set the tick “Offer HTTPS (port 443)” in the window of the program “Webserver”",
                        n -> webserverProgramm(n) != null && webserverProgramm(n).isHttps()),
                Schritt.z("Webserver: bei „Zertifikat für“ die Adresse " + WEBSERVER + " eintragen", "Web server: enter the address " + WEBSERVER + " at “Certificate for”",
                        n -> webserverProgramm(n) != null
                                && filius.modern.tls.Tls.namen(webserverProgramm(n).getZertifikatNamen(), null).contains(WEBSERVER.toLowerCase(Locale.ROOT)))
                        .ist(Regeln::zertifikatNamen),
                Schritt.z("Webserver: bei „Selbst signiert“ darf kein Haken sein", "Web server: “Self-signed” must not be ticked",
                        n -> webserverProgramm(n) != null && !webserverProgramm(n).isSelbstSigniert()),
                Schritt.z("Webserver: das Programm „Webserver“ muss gestartet sein", "Web server: the program “Webserver” must be started",
                        n -> webserverProgramm(n) != null && webserverProgramm(n).isAktiv())));
        def("4.4", Regel.schritte(httpsSteht(), seite("Laptop-1: im Webbrowser https://" + WEBSERVER + "/intern/ aufrufen (mit dem „s“ in https)",
                "Laptop-1: open https://" + WEBSERVER + "/intern/ in the web browser (with the “s” in https)",
                (n, e, s) -> http(e, s, laptop(n, 1), WEBSERVER, filius.modern.tls.Tls.PORT, x -> x.pfad.toLowerCase(Locale.ROOT).startsWith("/intern")))));
        def("4.5", Regel.schritte(httpsSteht(),
                seite("Laptop-1: über https://" + WEBSERVER + "/intern/ noch einmal als anna anmelden", "Laptop-1: sign in as anna once more at https://" + WEBSERVER + "/intern/",
                        (n, e, s) -> http(e, s, laptop(n, 1), WEBSERVER, filius.modern.tls.Tls.PORT, Regeln::angemeldet)),
                austausch(L1)));
        // the warning of the browser: Laptop-2 rejected the certificate with an alert
        def("4.6", Regel.schritte(seite("Am Webserver den Haken „Selbst signiert“ setzen, dann auf Laptop-2 https://" + WEBSERVER + " aufrufen – der Browser muss warnen",
                "Tick “Self-signed” at the web server, then open https://" + WEBSERVER + " on Laptop-2 – the browser must warn", (n, e, s) -> laptop(n, 2) != null
                        && e.gibt(TLS, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER) && "Alert".equals(x.name) && "gesendet".equals(x.typ))))
                .uebergang());
        def("4.7", Regel.schritte(
                Schritt.z("Webserver: den Haken „Selbst signiert“ wieder herausnehmen (HTTPS bleibt an, Zertifikat für " + WEBSERVER + ")",
                        "Web server: remove the tick “Self-signed” again (HTTPS stays on, certificate for " + WEBSERVER + ")", n -> httpsOk(n, WEBSERVER)),
                Schritt.z("Webserver: den Haken „HTTP anbieten (Port 80)“ herausnehmen", "Web server: remove the tick “Offer HTTP (port 80)”",
                        n -> webserverProgramm(n) != null && !webserverProgramm(n).isHttp()),
                seite("Laptop-2: https://" + WEBSERVER + " aufrufen – die Seite muss laden", "Laptop-2: open https://" + WEBSERVER + " – the page must load",
                        (n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, filius.modern.tls.Tls.PORT, null))));
        def("4.8", Regel.schritte(Schritt.z("Im Dokumentationsmodus direkt neben den Webserver ein Textfeld „HTTPS, Port 443“ legen",
                "In the documentation mode, put a text field “HTTPS, port 443” right next to the web server",
                n -> textNeben(webserver(n), t -> t.toLowerCase(Locale.ROOT).contains("https") && t.contains("443")))));
    }

    // ================================================================== chapter 5

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
        java.util.function.Function<Netz, Knoten> prov = n -> switchMit(n, "provider");
        java.util.function.Function<Netz, Knoten> rgg = n -> switchMit(n, "rgg");
        G r2 = g("R2 Router RGG", "R2 RGG router", Regeln::r2);
        // the names given in the task are part of it (German or English)
        def("5.1", Regel.schritte(
                Schritt.z("Switch mit dem Namen „Switch Provider“ aufstellen", "Add a switch named “Provider switch”", n -> Netz.heisst(prov.apply(n), "Switch Provider")),
                Schritt.z("Vermittlungsrechner mit zwei Schnittstellen aufstellen und einer Karte die IPv4-Adresse " + R1_SCHULE + " geben",
                        "Add a router with two interfaces and give one card the IPv4 address " + R1_SCHULE, n -> r1(n) != null),
                name("Den Vermittlungsrechner „R1 Router Schule“ nennen", "Name the router “R1 School router”", R1G, "R1 Router Schule"),
                Schritt.z("R1: die Karte " + R1_SCHULE + " (Netzmaske " + MASKE + ") mit dem Switch Schule verbinden",
                        "R1: connect the card " + R1_SCHULE + " (netmask " + MASKE + ") to the School switch", n -> karteAn(n, r1(n), R1_SCHULE, MASKE, switchSchule(n))),
                Schritt.z("R1: der anderen Karte " + R1_PROVIDER + " (Netzmaske " + MASKE + ") geben und sie mit dem Switch Provider verbinden",
                        "R1: give the other card " + R1_PROVIDER + " (netmask " + MASKE + ") and connect it to the Provider switch",
                        n -> karteAn(n, r1(n), R1_PROVIDER, MASKE, prov.apply(n))).ist(n -> karten(r1(n)))));
        def("5.2", Regel.schritte(Schritt.z("Bei allen Geräten im Schulnetz (Laptop-1 bis Laptop-4 und Webserver) das Gateway " + R1_SCHULE + " eintragen",
                "Enter the gateway " + R1_SCHULE + " at all devices of the school network (Laptop-1 to Laptop-4 and the web server)", n -> {
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
                }).ist(n -> {
                    List<String> ohne = new ArrayList<>();
                    for (Knoten k : n.hosts()) {
                        if (inNetz(Netz.ip(k), SCHULNETZ) && !R1_SCHULE.equals(Netz.gateway(k))) {
                            ohne.add(k.getName());
                        }
                    }
                    return ohne.isEmpty() ? null : t("es fehlt noch bei: ", "still missing at: ") + String.join(", ", ohne);
                })));
        def("5.3", Regel.schritte(
                Schritt.z("Switch mit dem Namen „Switch RGG“ aufstellen", "Add a switch named “RGG switch”", n -> Netz.heisst(rgg.apply(n), "Switch RGG")),
                Schritt.z("Zweiten Vermittlungsrechner mit zwei Schnittstellen aufstellen und einer Karte die IPv4-Adresse " + R2_PROVIDER + " geben",
                        "Add a second router with two interfaces and give one card the IPv4 address " + R2_PROVIDER, n -> r2(n) != null),
                name("Diesen Vermittlungsrechner „R2 Router RGG“ nennen", "Name this router “R2 RGG router”", r2, "R2 Router RGG"),
                Schritt.z("R2: die Karte " + R2_PROVIDER + " (Netzmaske " + MASKE + ") mit dem Switch Provider verbinden",
                        "R2: connect the card " + R2_PROVIDER + " (netmask " + MASKE + ") to the Provider switch", n -> karteAn(n, r2(n), R2_PROVIDER, MASKE, prov.apply(n))),
                Schritt.z("R2: der anderen Karte " + R2_RGG + " (Netzmaske " + MASKE + ") geben und sie mit dem Switch RGG verbinden",
                        "R2: give the other card " + R2_RGG + " (netmask " + MASKE + ") and connect it to the RGG switch", n -> karteAn(n, r2(n), R2_RGG, MASKE, rgg.apply(n)))
                        .ist(n -> karten(r2(n))),
                stehtMitNamen(SHOPG, Rechner.class, "Shop"), adresse(SHOPG, SHOP), gateway(SHOPG, R2_RGG), anSwitch(SHOPG, "Switch RGG", "RGG switch", rgg),
                stehtMitNamen(BUE, Notebook.class, "Büro-Laptop"), adresse(BUE, BUERO), gateway(BUE, R2_RGG), anSwitch(BUE, "Switch RGG", "RGG switch", rgg)));
        def("5.4", Regel.schritte(installiert(SHOPG, Netz.WEBSERVER),
                Schritt.z("Shop: mit dem Text-Editor in `webserver/index.html` die Überschrift in „Ravensberger24 – Getränke online“ ändern und speichern",
                        "Shop: with the text editor, change the heading in `webserver/index.html` to “Ravensberger24 – Drinks online” and save",
                        n -> Netz.h1(Netz.datei(shop(n), "webserver/index.html")).toLowerCase(Locale.ROOT).contains("ravensberger24"))
                        .ist(n -> shop(n) == null ? null : "„" + Netz.h1(Netz.datei(shop(n), "webserver/index.html")) + "“"),
                laeuft(SHOPG, Netz.WEBSERVER)));
        def("5.5", Regel.schritte(seite("Laptop-1: `ping " + SHOP + "` eingeben und abwarten – es kommt noch keine Antwort", "Laptop-1: type `ping " + SHOP + "` and wait – no reply comes yet",
                (n, e, s) -> laptop(n, 1) != null && e.gibt(PING_OHNE_ANTWORT, s, x -> x.knoten == laptop(n, 1) && x.ziel.equals(SHOP)))).uebergang());
        def("5.6", Regel.schritte(
                Schritt.z("R1 Router Schule: im Entwurfsmodus das Häkchen „Automatisches Routing“ setzen (Reiter „Allgemein“)",
                        "R1 School router: set the tick “Automatic routing” in the design mode (tab “General”)", n -> ripAn(r1(n))),
                Schritt.z("R2 Router RGG: das Häkchen „Automatisches Routing“ setzen", "R2 RGG router: set the tick “Automatic routing”", n -> ripAn(r2(n))),
                seite("In der Simulation im Webbrowser eines Schul-Laptops (Laptop-2) die Seite http://" + R1_SCHULE + "/routes öffnen",
                        "In the simulation, open the page http://" + R1_SCHULE + "/routes in the web browser of a school laptop (Laptop-2)",
                        (n, e, s) -> e.gibt(HTTP, s, x -> schulGeraet(n, x.knoten) && (x.ziel.equals(R1_SCHULE) || x.ziel.equals(R1_PROVIDER)) && x.pfad.startsWith("/routes")
                                && x.status == 200)))
                .antwort((n, e, a) -> {
                    int h = ripHops(r1(n), RGGNETZ);
                    return zahlen(a).equals(List.of(String.valueOf(h >= 0 ? h : 1)));
                }));
        def("5.7", Regel.schritte(
                Schritt.z("Bei R1 und bei R2 ist das Häkchen „Automatisches Routing“ gesetzt", "The tick “Automatic routing” is set at R1 and at R2", Regeln::ripOk),
                pingt(L1, SHOP), seite("Laptop-2: im Webbrowser http://" + SHOP + " aufrufen – die Shop-Seite muss erscheinen",
                        "Laptop-2: open http://" + SHOP + " in the web browser – the shop page must appear",
                        (n, e, s) -> http(e, s, laptop(n, 2), SHOP, 80, x -> seiteMit(x, "Ravensberger24")))));
        def("5.8", Regel.schritte(seite("Laptop-1: in der Befehlszeile `traceroute " + SHOP + "` eingeben", "Laptop-1: type `traceroute " + SHOP + "` in the command line",
                (n, e, s) -> befehlMit(e, s, laptop(n, 1), SHOP, "traceroute", "tracert") != null)).antwort((n, e, a) -> {
                    List<String> ip = Netz.adressen(a);
                    return ip.contains(R1_SCHULE) && ip.contains(R2_PROVIDER) && ip.size() == 2;
                }));
        def("5.9", Regel.schritte(austausch(L1), austausch(SHOPG),
                seite("Laptop-1: den Shop noch einmal anpingen (`ping " + SHOP + "`)", "Laptop-1: ping the shop once more (`ping " + SHOP + "`)",
                        (n, e, s) -> e.gibt(PING_EMPFANGEN, s, x -> x.knoten == shop(n) && laptop(n, 1) != null && x.quelle.equals(Netz.ip(laptop(n, 1)))))));
        def("5.10", Regel.schritte(flaeche("Providernetz", "provider network", prov),
                mitEtikett("Providernetz: Etikett mit Name und Netzadresse mit /24, zum Beispiel „Providernetz " + PROVIDERNETZ + "/24“",
                        "Provider network: label with its name and network address with /24, for example “Provider network " + PROVIDERNETZ + "/24”", prov,
                        mitNetz(PROVIDERNETZ + "/24")),
                flaeche("RGG-Netz", "RGG network", rgg),
                mitEtikett("RGG-Netz: Etikett mit Name und Netzadresse mit /24, zum Beispiel „RGG-Netz " + RGGNETZ + "/24“",
                        "RGG network: label with its name and network address with /24, for example “RGG network " + RGGNETZ + "/24”", rgg, mitNetz(RGGNETZ + "/24"))));
        // R1 sees only encrypted data of the sign-in from the RGG
        def("5.11", Regel.schritte(
                seite("Büro-Laptop: im Webbrowser über https://" + WEBSERVER + "/intern/ als anna anmelden", "Office laptop: sign in as anna at https://" + WEBSERVER + "/intern/ in the web browser",
                        (n, e, s2) -> http(e, s2, buero(n), WEBSERVER, filius.modern.tls.Tls.PORT, Regeln::angemeldet)),
                austausch(R1G)));
    }

    // ================================================================== chapter 5

    private static void kapitel6() {
        G d = g("DNS-Server", "DNS server", n -> {
            Knoten k = n.knoten("DNS-Server");
            return k instanceof Rechner ? k : n.mitIp(DNS_SERVER);
        });
        def("6.1", Regel.schritte(stehtMitNamen(d, Rechner.class, "DNS-Server"), adresse(d, DNS_SERVER), gateway(d, R1_PROVIDER),
                anSwitch(d, "Switch Provider", "Provider switch", n -> switchMit(n, "provider")), installiert(d, Netz.DNSSERVER)));
        def("6.2", Regel.schritte(
                eintrag("DNS-Server: A-Eintrag für " + WWW_SCHULE + " mit der Adresse des Webservers der Schule anlegen",
                        "DNS server: add an A record for " + WWW_SCHULE + " with the address of the school web server", "A", WWW_SCHULE, WEBSERVER),
                eintrag("DNS-Server: A-Eintrag für " + WWW_SHOP + " mit der Adresse des Shops anlegen", "DNS server: add an A record for " + WWW_SHOP + " with the address of the shop", "A",
                        WWW_SHOP, SHOP),
                laeuft(DNSG, Netz.DNSSERVER)));
        def("6.3", Regel.schritte(dns(L1, DNS_SERVER), dns(L2, DNS_SERVER), dns(L3, DNS_SERVER), dns(L4, DNS_SERVER), dns(BUE, DNS_SERVER)));
        def("6.4", Regel.schritte(seite("Laptop-1: in der Befehlszeile `nslookup " + WWW_SHOP + "` eingeben – der DNS-Server muss antworten",
                "Laptop-1: type `nslookup " + WWW_SHOP + "` in the command line – the DNS server must answer",
                (n, e, s) -> befehl(e, s, laptop(n, 1), "nslookup") && dnsAntwort(e, s, laptop(n, 1), WWW_SHOP, SHOP))));
        def("6.5", Regel.schritte(
                seite("Laptop-2: im Webbrowser http://" + WWW_SHOP + " öffnen – die Shop-Seite muss erscheinen", "Laptop-2: open http://" + WWW_SHOP + " in the web browser – the shop page must appear",
                        (n, e, s) -> dnsAntwort(e, s, laptop(n, 2), WWW_SHOP, SHOP) && http(e, s, laptop(n, 2), SHOP, 80, x -> x.host.startsWith(WWW_SHOP))),
                seite("Büro-Laptop: im Webbrowser https://" + WWW_SCHULE + " aufrufen (der Browser warnt noch)", "Office laptop: open https://" + WWW_SCHULE + " in the web browser (the browser still warns)",
                        (n, e, s) -> dnsAntwort(e, s, buero(n), WWW_SCHULE, WEBSERVER)
                                && e.gibt(TCP_VERBUNDEN, s, x -> x.knoten == buero(n) && x.ziel.equals(WEBSERVER) && x.port == filius.modern.tls.Tls.PORT))));
        def("6.6", Regel.schritte(austausch(L2), seite("Laptop-2: eine Website über ihren Namen aufrufen, sodass eine DNS-Anfrage im Datenaustausch steht",
                "Laptop-2: open a website by its name so that a DNS query is in the data exchange", (n, e, s) -> e.gibt(DNS_ANFRAGE, s, x -> x.knoten == laptop(n, 2)))));
        // the certificate of the school web server names the web site
        def("6.7", Regel.schritte(
                Schritt.z("Webserver der Schule: bei „Zertifikat für“ zusätzlich " + WWW_SCHULE + " und " + DOMAIN_SCHULE + " eintragen (mit Kommas getrennt)",
                        "School web server: also enter " + WWW_SCHULE + " and " + DOMAIN_SCHULE + " at “Certificate for” (separated by commas)",
                        n -> httpsOk(n, WWW_SCHULE, DOMAIN_SCHULE)).ist(Regeln::zertifikatNamen),
                seite("Büro-Laptop: https://" + WWW_SCHULE + " neu laden – jetzt ohne Warnung", "Office laptop: reload https://" + WWW_SCHULE + " – now without a warning",
                        (n, e, s) -> http(e, s, buero(n), WEBSERVER, filius.modern.tls.Tls.PORT, x -> x.host.startsWith(WWW_SCHULE)))));
        def("6.8", Regel.schritte(
                eintrag("Dafür sorgen, dass der DNS-Server auch den Namen " + DOMAIN_SCHULE + " (ohne www) kennt",
                        "Make sure the DNS server also knows the name " + DOMAIN_SCHULE + " (without www)", "A", DOMAIN_SCHULE, WEBSERVER),
                seite("In einem Webbrowser https://" + DOMAIN_SCHULE + " öffnen – die Schulseite muss erscheinen", "Open https://" + DOMAIN_SCHULE + " in a web browser – the school page must appear",
                        (n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER) && x.status == 200 && x.host.replaceFirst(":\\d+$", "").equals(DOMAIN_SCHULE)))));
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
        java.util.function.Function<Netz, Knoten> d = Regeln::dhcpServerPc;
        def("7.1", Regel.schritte(stehtMitNamen(DHCPG, Rechner.class, "DHCP-Server"), adresse(DHCPG, DHCP_SERVER),
                Schritt.z("DHCP-Server mit einem Kabel an den Switch Schule anschließen", "Connect the DHCP server to the School switch with a cable",
                        n -> d.apply(n) != null && n.segment(switchSchule(n)).contains(d.apply(n))),
                dhcp(d, "DHCP-Server: Knopf „DHCP-Server einrichten“ – Adressbereich für mindestens 30 Laptops im Schulnetz, ab 192.168.10.50 (darunter ist reserviert)",
                        "DHCP server: button “DHCP server setup” – an address range for at least 30 laptops in the school network, from 192.168.10.50 on (below is reserved)",
                        s -> inNetz(s.getUntergrenze(), SCHULNETZ) && inNetz(s.getObergrenze(), SCHULNETZ) && Netz.wert(s.getUntergrenze()) >= Netz.wert("192.168.10.50")
                                && Netz.wert(s.getObergrenze()) <= Netz.wert("192.168.10.254") && Netz.wert(s.getObergrenze()) - Netz.wert(s.getUntergrenze()) + 1 >= 30,
                        s -> s.getUntergrenze() + " – " + s.getObergrenze()),
                dhcp(d, "DHCP-Server: die Netzmaske " + MASKE + " verteilen", "DHCP server: hand out the netmask " + MASKE, s -> MASKE.equals(s.getSubnetzmaske()),
                        DHCPServer::getSubnetzmaske),
                dhcp(d, "DHCP-Server: Damit die Laptops ins Internet kommen, muss er ihnen das Gateway des Schulnetzes mitgeben (er nimmt das Gateway seines eigenen PCs)",
                        "DHCP server: for the way to the Internet it must hand out the gateway of the school network (it takes the gateway of its own PC)",
                        s -> R1_SCHULE.equals(s.determineGatewayip()), s -> Teile.oder(s.determineGatewayip(), "kein Gateway", "no gateway")),
                dhcp(d, "DHCP-Server: Damit die Laptops Namen auflösen können, muss er ihnen den DNS-Server mitgeben (er nimmt den DNS-Server seines eigenen PCs)",
                        "DHCP server: for names it must hand out the DNS server (it takes the DNS server of its own PC)", s -> DNS_SERVER.equals(s.determineDnsserverip()),
                        s -> Teile.oder(s.determineDnsserverip(), "kein DNS-Server", "no DNS server")),
                dhcp(d, "DHCP-Server: das Häkchen „DHCP aktivieren“ setzen", "DHCP server: set the tick “Activate DHCP”", DHCPServer::isAktiv, null)));
        def("7.2", Regel.schritte(dhcpAn(L1), dhcpAn(L2), dhcpAn(L3), dhcpAn(L4)));
        def("7.3", Regel.schritte(
                Schritt.z("Der DHCP-Server ist eingerichtet wie in Aufgabe 7.1", "The DHCP server is set up as in task 7.1", Regeln::dhcpSchuleOk),
                // the addresses come anew with every start of the simulation; ipconfig does not have to wait for the last laptop
                Schritt.z("Die Simulation starten und warten, bis alle vier Schul-Laptops ihre Einstellungen vom DHCP-Server haben",
                        "Start the simulation and wait until all four school laptops have their settings from the DHCP server", n -> {
                            List<Knoten> l = schulLaptops(n);
                            return l.size() == 4 && l.stream().allMatch(k -> dhcpAdresseOk(n, k));
                        }).frei().ist(n -> {
                            List<String> ohne = new ArrayList<>();
                            for (Knoten k : schulLaptops(n)) {
                                if (!dhcpAdresseOk(n, k)) {
                                    ohne.add(k.getName() + " " + Netz.ip(k));
                                }
                            }
                            return ohne.isEmpty() ? null : t("noch nicht: ", "not yet: ") + String.join(", ", ohne);
                        }),
                Schritt.n("Jeder der vier Laptops hat seine Adresse beim Start der Simulation vom DHCP-Server geholt (sonst die Simulation neu starten)",
                        "Each of the four laptops fetched its address from the DHCP server at the start of the simulation (otherwise restart the simulation)",
                        (n, e, s) -> schulLaptops(n).stream().allMatch(k -> dhcpAck(e, 0, k))),
                Schritt.n("Auf einem der Schul-Laptops in der Befehlszeile `ipconfig` eingeben", "Type `ipconfig` in the command line of one of the school laptops",
                        (n, e, s) -> schulLaptops(n).stream().anyMatch(k -> befehl(e, s, k, "ipconfig")))));
        Regel.Nachweis doraGesehen = (n, e, s) -> schulLaptops(n).stream().anyMatch(k -> fenster(e, s, k)) && e.gibt(DHCP, 0, x -> x.name.equals("DHCPDISCOVER"));
        String doraDe = "Datenaustausch eines Schul-Laptops öffnen und seine DHCP-Nachrichten suchen (stehen dort keine, die Simulation neu starten)";
        String doraEn = "Open the data exchange of a school laptop and look for its DHCP messages (if there are none, restart the simulation)";
        def("7.4", Regel.schritte(Schritt.n(doraDe, doraEn, doraGesehen)));
        def("7.5", Regel.schritte(Schritt.n(doraDe, doraEn, doraGesehen)).antwort((n, e, a) -> ipAntwort(a, "0.0.0.0")));
        def("7.6", Regel.schritte(
                Schritt.z("DHCP-Server, Reiter „Statische Adresszuweisung“: der MAC-Adresse von Laptop-4 eine Adresse ab 192.168.10.50 zuweisen",
                        "DHCP server, tab “Static Address Assignment”: assign an address from 192.168.10.50 on to the MAC address of Laptop-4", n -> {
                            String ip = Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(laptop(n, 4)));
                            return laptop(n, 4) != null && ip != null && Netz.imBereich(ip, "192.168.10.50", "192.168.10.254");
                        }).ist(n -> {
                            Knoten k = laptop(n, 4);
                            if (k == null) {
                                return null;
                            }
                            String ip = Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(k));
                            return ip == null ? t("keine Zuweisung für die MAC-Adresse " + Netz.mac(k), "no assignment for the MAC address " + Netz.mac(k)) : Netz.mac(k) + " → " + ip;
                        }),
                Schritt.z("Die zugewiesene Adresse muss frei sein: Kein anderes Gerät darf sie haben", "The assigned address must be free: no other device may have it", n -> {
                    Knoten k = laptop(n, 4);
                    String ip = Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(k));
                    if (k == null || ip == null) {
                        return false;
                    }
                    for (Knoten x : n.knoten()) {
                        if (x != k && Netz.nic(x, ip) != null) {
                            return false;
                        }
                    }
                    return true;
                }),
                Schritt.z("Laptop-4 bleibt auf „DHCP zur Konfiguration verwenden“", "Laptop-4 stays at “Use DHCP for configuration”", n -> Netz.dhcp(laptop(n, 4))),
                Schritt.z("Kurz in den Entwurfsmodus und wieder in die Simulation wechseln – Laptop-4 holt sich dann die zugewiesene Adresse",
                        "Switch to the design mode and back to the simulation – Laptop-4 then fetches the assigned address", n -> {
                            Knoten k = laptop(n, 4);
                            String ip = Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(k));
                            return k != null && ip != null && ip.equals(Netz.ip(k));
                        }).ist(n -> laptop(n, 4) == null ? null : Netz.ip(laptop(n, 4)))));
        def("7.7", Regel.schritte(seite("Auf einem Schul-Laptop, der seine Einstellungen per DHCP bekommen hat, im Webbrowser " + WWW_SHOP + " öffnen",
                "On a school laptop that got its settings by DHCP, open " + WWW_SHOP + " in the web browser",
                (n, e, s) -> schulLaptops(n).stream().anyMatch(k -> Netz.dhcp(k) && dhcpAdresseOk(n, k) && http(e, s, k, SHOP, 80, x -> x.host.startsWith(WWW_SHOP))))));
        def("7.8", Regel.schritte(Schritt.z("Im Dokumentationsmodus ein Textfeld mit dem Adressbereich deines DHCP-Servers in die Fläche des Schulnetzes legen (beide Grenzen als vollständige IPv4-Adressen)",
                "In the documentation mode, put a text field with the address range of your DHCP server into the field of the school network (both limits as complete IPv4 addresses)", n -> {
                    DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
                    return s != null && Netz.gueltig(s.getUntergrenze()) && Netz.gueltig(s.getObergrenze())
                            && etikett(switchSchule(n), mitAdressen(s.getUntergrenze(), s.getObergrenze()));
                }).ist(n -> etikettText(switchSchule(n)))));
    }

    // ================================================================== chapter 7

    private static void kapitel8() {
        java.util.function.Function<Netz, Knoten> hr = Regeln::heimrouter;
        def("8.1", Regel.schritte(Schritt.z("Heimrouter aufstellen", "Add a home router", n -> heimrouter(n) instanceof Gateway),
                heimKarte("Heimrouter, WAN: IPv4-Adresse " + HEIM_WAN + " und Netzmaske " + MASKE + " eintragen", "Home router, WAN: enter the IPv4 address " + HEIM_WAN + " and the netmask " + MASKE,
                        (n, g) -> HEIM_WAN.equals(g.holeWANInterface().getIp()) && MASKE.equals(g.holeWANInterface().getSubnetzMaske()),
                        g -> g.holeWANInterface().getIp() + " / " + g.holeWANInterface().getSubnetzMaske()),
                heimKarte("Heimrouter: Gateway " + R1_PROVIDER + " eintragen", "Home router: enter the gateway " + R1_PROVIDER,
                        (n, g) -> R1_PROVIDER.equals(g.holeWANInterface().getGateway()) || R1_PROVIDER.equals(Netz.gateway(g)), g -> Teile.oder(Netz.gateway(g), "leer", "empty")),
                heimKarte("Heimrouter: den WAN-Anschluss mit dem Switch Provider verbinden", "Home router: connect the WAN port to the Provider switch",
                        (n, g) -> n.gegenueber(g.holeWANInterface()) == switchMit(n, "provider"), null),
                heimKarte("Heimrouter, LAN: IPv4-Adresse " + HEIM_LAN + " und Netzmaske " + MASKE + " eintragen", "Home router, LAN: enter the IPv4 address " + HEIM_LAN + " and the netmask " + MASKE,
                        (n, g) -> HEIM_LAN.equals(g.holeLANInterface().getIp()) && MASKE.equals(g.holeLANInterface().getSubnetzMaske()),
                        g -> g.holeLANInterface().getIp() + " / " + g.holeLANInterface().getSubnetzMaske()),
                Schritt.z("Switch mit dem Namen „Switch Heimnetz“ aufstellen und mit dem LAN-Anschluss des Heimrouters verbinden",
                        "Add a switch named “Home switch” and connect it to the LAN port of the home router",
                        n -> heimSwitch(n) instanceof Switch && Netz.heisst(heimSwitch(n), "Switch Heimnetz")).ist(n -> heimSwitch(n) == null ? null : "„" + heimSwitch(n).getName() + "“")));
        def("8.2", Regel.schritte(
                dhcp(hr, "Heimrouter: Knopf „DHCP-Server einrichten“ (beim LAN-Anschluss) – Adress-Untergrenze 192.168.178.100 eintragen",
                        "Home router: button “DHCP server setup” (at the LAN port) – enter the lower address limit 192.168.178.100",
                        s -> "192.168.178.100".equals(s.getUntergrenze()), DHCPServer::getUntergrenze),
                dhcp(hr, "Heimrouter: Adress-Obergrenze 192.168.178.150 eintragen", "Home router: enter the upper address limit 192.168.178.150",
                        s -> "192.168.178.150".equals(s.getObergrenze()), DHCPServer::getObergrenze),
                dhcp(hr, "Heimrouter: im DHCP-Server den DNS-Server " + DNS_SERVER + " eintragen", "Home router: enter the DNS server " + DNS_SERVER + " in the DHCP server",
                        s -> DNS_SERVER.equals(s.determineDnsserverip()), s -> Teile.oder(s.determineDnsserverip(), "leer", "empty")),
                dhcp(hr, "Heimrouter: das Häkchen „DHCP aktivieren“ setzen", "Home router: set the tick “Activate DHCP”", DHCPServer::isAktiv, null)));
        def("8.3", Regel.schritte(
                Schritt.z("Switch Heimnetz: bei „Name WLAN (SSID)“ Heim-WLAN eintragen", "Home switch: enter Home-WiFi at “Name Wi-Fi (SSID)”",
                        n -> heimSwitch(n) instanceof Switch && Netz.gleich(Netz.ssid(heimSwitch(n)), "Heim-WLAN")).ist(n -> heimSwitch(n) == null ? null : "„" + Netz.ssid(heimSwitch(n)) + "“"),
                stehtMitNamen(HEIML, Notebook.class, "Heim-Laptop"),
                Schritt.z("Heim-Laptop: in den Einstellungen „Drahtlos (WLAN)“ wählen und darunter Heim-WLAN (ein Kabel vorher löschen)",
                        "Home laptop: choose “Wireless (Wi-Fi)” in the settings and Home-WiFi below it (delete a cable first)", n -> imWlan(n, heimLaptop(n), heimSwitch(n))),
                dhcpAn(HEIML), stehtMitNamen(TAB, Notebook.class, "Tablet"),
                Schritt.z("Tablet: in den Einstellungen „Drahtlos (WLAN)“ wählen und darunter Heim-WLAN (ein Kabel vorher löschen)",
                        "Tablet: choose “Wireless (Wi-Fi)” in the settings and Home-WiFi below it (delete a cable first)", n -> imWlan(n, tablet(n), heimSwitch(n))),
                dhcpAn(TAB)));
        def("8.4", Regel.schritte(tippt(HEIML, "ipconfig", "ipconfig")).antwort((n, e, a) -> dhcpAntwort(e, heimLaptop(n), a, "192.168.178.100", "192.168.178.150")));
        // The shop page reached a device of the home network, or the shop got the request through the home
        // router (NAT). The second proof also holds when the browser's own view of the answer is incomplete,
        // e.g. while another page of the school web server is still loading through R1.
        def("8.5", Regel.schritte(seite("Heim-Laptop: den Webbrowser installieren und " + WWW_SHOP + " öffnen", "Home laptop: install the web browser and open " + WWW_SHOP,
                (n, e, s) -> e.gibt(HTTP, s, x -> heimGeraet(x.knoten) && x.ziel.equals(SHOP) && x.port == 80 && x.status == 200 && seiteMit(x, "Ravensberger24"))
                        || shop(n) != null && e.gibt(HTTP_ANFRAGE, s, x -> x.knoten == shop(n) && x.quelle.equals(HEIM_WAN) && x.port == 80))));
        def("8.6", Regel.schritte(austausch(SHOPG), seite("Heim-Laptop: den Shop noch einmal aufrufen", "Home laptop: open the shop once more",
                (n, e, s) -> e.gibt(HTTP_ANFRAGE, s, x -> x.knoten == shop(n) && x.quelle.equals(HEIM_WAN)))).antwort((n, e, a) -> ipAntwort(a, HEIM_WAN)));
        def("8.7", Regel.schritte(seite("In der Simulation den Heimrouter anklicken, sodass seine NAT-Tabelle aufgeht – sie muss einen Eintrag haben (sonst den Shop noch einmal aufrufen)",
                "Click the home router in the simulation so that its NAT table opens – it must have an entry (otherwise open the shop once more)",
                (n, e, s) -> e.gibt(NAT, s, x -> x.knoten == heimrouter(n)))));
        def("8.8", Regel.schritte(seite("Laptop-1: den Heim-Laptop anpingen (`ping` mit seiner Adresse 192.168.178.…) und abwarten",
                "Laptop-1: ping the home laptop (`ping` with its address 192.168.178.…) and wait",
                (n, e, s) -> e.gibt(PING_OHNE_ANTWORT, s, x -> schulGeraet(n, x.knoten) && inNetz(x.ziel, HEIMNETZ)))).uebergang());
        // version 31: name and network in the label of the field ("+") or a text field, the note "privat, hinter NAT"
        // in the same label or in another text field of the field
        def("8.9", Regel.schritte(flaeche("Heimnetz", "home network", Regeln::heimSwitch),
                mitEtikett("Etikett mit Name und Netzadresse mit /24, zum Beispiel „Heimnetz " + HEIMNETZ + "/24“",
                        "Label with the name and the network address with /24, for example “Home network " + HEIMNETZ + "/24”", Regeln::heimSwitch, mitNetz(HEIMNETZ + "/24")),
                mitEtikett("Darunter ein Textfeld mit dem Hinweis „privat, hinter NAT“", "Below it a text field with the note “private, behind NAT”", Regeln::heimSwitch, tx -> {
                    String klein = tx == null ? "" : tx.toLowerCase(Locale.GERMAN);
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
        def("10.1", Regel.schritte(stehtMitNamen(BLOGG, Rechner.class, "Blog-Server"),
                // the fixed address by hand, or (version 31) DHCP with a static assignment in the home router
                Schritt.z("Blog-Server: feste IPv4-Adresse " + BLOG + " und Netzmaske " + MASKE + " – von Hand eintragen oder als statische Zuweisung im DHCP-Server des Heimrouters "
                        + "(dann die Simulation starten)",
                        "Blog server: fixed IPv4 address " + BLOG + " and netmask " + MASKE + " – enter it by hand or as a static assignment in the DHCP server of the home router "
                                + "(then start the simulation)",
                        n -> {
                            Knoten b = BLOGG.in(n);
                            return Netz.fest(b, BLOG, MASKE) || b != null && Netz.dhcp(b) && BLOG.equals(Netz.ip(b)) && MASKE.equals(Netz.maske(b))
                                    && BLOG.equals(Netz.statischeZuweisung(heimrouter(n), Netz.mac(b)));
                        }).ist(n -> BLOGG.in(n) == null ? null : Netz.ip(BLOGG.in(n)) + " / " + Netz.maske(BLOGG.in(n))),
                gateway(BLOGG, HEIM_LAN), anSwitch(BLOGG, "Switch Heimnetz", "Home switch", Regeln::heimSwitch), installiert(BLOGG, Netz.WEBSERVER),
                Schritt.z("Blog-Server: mit dem Text-Editor in `webserver/index.html` die Überschrift in „Mein Blog“ ändern und speichern",
                        "Blog server: with the text editor, change the heading in `webserver/index.html` to “My blog” and save",
                        n -> Netz.h1(Netz.datei(BLOGG.in(n), "webserver/index.html")).toLowerCase(Locale.GERMAN).contains("blog"))
                        .ist(n -> BLOGG.in(n) == null ? null : "„" + Netz.h1(Netz.datei(BLOGG.in(n), "webserver/index.html")) + "“"),
                laeuft(BLOGG, Netz.WEBSERVER)));
        def("10.2", Regel.schritte(seite("Auf einem Schul-Laptop im Webbrowser http://" + HEIM_WAN + ":8080 aufrufen und abwarten – es kommt keine Seite",
                "On a school laptop, open http://" + HEIM_WAN + ":8080 in the web browser and wait – no page comes",
                (n, e, s) -> e.gibt(SYN_OHNE_ANTWORT, s, x -> schulGeraet(n, x.knoten) && x.ziel.equals(HEIM_WAN) && x.port == 8080))).uebergang());
        def("10.3", Regel.schritte(
                Schritt.z("Heimrouter: eine Portfreigabe eintragen – Protokoll TCP, Port außen 8080, an den Blog-Server " + BLOG + ", Port innen 80",
                        "Home router: add a port forwarding – protocol TCP, outside port 8080, to the blog server " + BLOG + ", inside port 80", Regeln::portfreigabeOk)
                        .ist(Regeln::freigaben),
                seite("Auf einem Schul-Laptop http://" + HEIM_WAN + ":8080 öffnen – das Blog muss erscheinen", "On a school laptop, open http://" + HEIM_WAN + ":8080 – the blog must appear",
                        Regeln::blogVonAussen)));
        def("10.4", Regel.schritte(
                Schritt.z("Heimrouter, „DHCP-Server einrichten“, Reiter „Statische Adresszuweisung“: der MAC-Adresse des Tablets die Adresse " + TABLET_IP + " zuweisen",
                        "Home router, “DHCP server setup”, tab “Static Address Assignment”: assign the address " + TABLET_IP + " to the MAC address of the tablet",
                        n -> tablet(n) != null && TABLET_IP.equals(Netz.statischeZuweisung(heimrouter(n), Netz.mac(tablet(n))))).ist(n -> {
                            Knoten k = tablet(n);
                            if (k == null) {
                                return null;
                            }
                            String ip = Netz.statischeZuweisung(heimrouter(n), Netz.mac(k));
                            return ip == null ? t("keine Zuweisung für die MAC-Adresse " + Netz.mac(k), "no assignment for the MAC address " + Netz.mac(k)) : Netz.mac(k) + " → " + ip;
                        }),
                dhcpAn(TAB),
                Schritt.z("Die Simulation (neu) starten – das Tablet holt sich dann die Adresse " + TABLET_IP, "(Re)start the simulation – the tablet then fetches the address " + TABLET_IP,
                        n -> tablet(n) != null && TABLET_IP.equals(Netz.ip(tablet(n)))).ist(n -> tablet(n) == null ? null : Netz.ip(tablet(n)))));
        def("10.5", Regel.schritte(installiert(TAB, Netz.WEBBROWSER),
                Schritt.z("Heimrouter: in „Firewall einrichten“ die Standardaktion auf „akzeptieren (Blacklist)“ stellen, eine Regel anlegen, die dem Tablet (" + TABLET_IP
                        + ", Maske 255.255.255.255) TCP-Port 80 zum Shop verwirft, und „Firewall aktivieren“ ankreuzen",
                        "Home router: in “Firewall settings” set the default action to “accept (blacklist)”, add a rule that drops TCP port 80 from the tablet (" + TABLET_IP
                                + ", mask 255.255.255.255) to the shop, and tick “Activate firewall”",
                        n -> Netz.firewall(heimrouter(n)) != null && Netz.firewall(heimrouter(n)).isActivated())
                        .ist(n -> t("die Firewall ist nicht aktiviert", "the firewall is not activated")),
                Schritt.n("Test 1: Das Tablet bekommt nach dem Start der Simulation weiter seine Adresse " + TABLET_IP + ". (Wenn nicht: Die Standardaktion „verwerfen“ sperrt auch DHCP.)",
                        "Test 1: after the start of the simulation the tablet still gets its address " + TABLET_IP + ". (If not: the default action “drop” blocks DHCP as well.)",
                        (n, e, s) -> TABLET_IP.equals(Netz.ip(tablet(n))) && dhcpAck(e, s, tablet(n))),
                Schritt.n("Test 2: Tablet – im Webbrowser https://" + WWW_SCHULE + " öffnen, die Schulseite muss erscheinen",
                        "Test 2: tablet – open https://" + WWW_SCHULE + " in the web browser, the school page must appear",
                        (n, e, s) -> http(e, s, tablet(n), WEBSERVER, filius.modern.tls.Tls.PORT, null)),
                Schritt.n("Test 3: Tablet – den Shop aufrufen (" + WWW_SHOP + ") und abwarten: Es darf keine Seite kommen",
                        "Test 3: tablet – open the shop (" + WWW_SHOP + ") and wait: no page may come",
                        (n, e, s) -> e.gibt(SYN_OHNE_ANTWORT, s, x -> x.knoten == tablet(n) && x.ziel.equals(SHOP) && x.port == 80)),
                Schritt.n("Test 4: Heim-Laptop – den Shop öffnen, die Seite muss erscheinen", "Test 4: home laptop – open the shop, the page must appear",
                        (n, e, s) -> http(e, s, heimLaptop(n), SHOP, 80, null)),
                Schritt.n("Test 5: Schul-Laptop – das Blog unter http://" + HEIM_WAN + ":8080 öffnen", "Test 5: school laptop – open the blog at http://" + HEIM_WAN + ":8080",
                        Regeln::blogVonAussen)));
        def("10.6", Regel.schritte(seite("Heim-Laptop: im Webbrowser die Verwaltungsseite des Heimrouters öffnen (http://" + HEIM_LAN + ") und dort das Log aufrufen",
                "Home laptop: open the admin page of the home router in the web browser (http://" + HEIM_LAN + ") and go to the log there",
                (n, e, s) -> e.gibt(HTTP, s, x -> heimGeraet(x.knoten) && x.ziel.equals(HEIM_LAN) && x.status == 200 && x.pfad.toLowerCase(Locale.ROOT).contains("log"))))
                .antwort((n, e, a) -> {
                    int nr = sperrRegel(n);
                    return nr > 0 && zahlen(a).equals(List.of(String.valueOf(nr)));
                }));
    }

    // ================================================================== chapter 9

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
        String mailS = "mail." + DOMAIN_SCHULE;
        String mailR = "mail." + DOMAIN_RGG;
        def("9.1", regel(mailserver(MS, "Mailserver Schule", MAIL_SCHULE, R1_SCHULE, "Switch Schule", "School switch", Regeln::switchSchule, DOMAIN_SCHULE, "anna", "ben")));
        def("9.2", regel(mailserver(MR, "Mailserver RGG", MAIL_RGG, R2_RGG, "Switch RGG", "RGG switch", n -> switchMit(n, "rgg"), DOMAIN_RGG, "verkauf")));
        def("9.3", Regel.schritte(
                eintrag("DNS-Server: einen A-Eintrag anlegen, der dem Namen " + mailS + " die Adresse des Mailservers der Schule gibt",
                        "DNS server: add an A record that gives the name " + mailS + " the address of the school mail server", "A", mailS, MAIL_SCHULE),
                eintrag("DNS-Server: einen A-Eintrag anlegen, der dem Namen " + mailR + " die Adresse des Mailservers der RGG gibt",
                        "DNS server: add an A record that gives the name " + mailR + " the address of the RGG mail server", "A", mailR, MAIL_RGG),
                eintrag("DNS-Server: im Reiter „Mailaustausch (MX)“ eintragen, dass " + mailS + " die Mails der Domain " + DOMAIN_SCHULE + " annimmt",
                        "DNS server: enter in the tab “Mail Exchange (MX)” that " + mailS + " takes the mails of the domain " + DOMAIN_SCHULE, "MX", DOMAIN_SCHULE, mailS),
                eintrag("DNS-Server: im Reiter „Mailaustausch (MX)“ eintragen, dass " + mailR + " die Mails der Domain " + DOMAIN_RGG + " annimmt",
                        "DNS server: enter in the tab “Mail Exchange (MX)” that " + mailR + " takes the mails of the domain " + DOMAIN_RGG, "MX", DOMAIN_RGG, mailR)));
        def("9.4", Regel.schritte(tippt(L1, "host " + DOMAIN_RGG, "host"),
                Schritt.n("Die Antwort des DNS-Servers muss den MX-Eintrag von " + DOMAIN_RGG + " nennen (fehlt er, Aufgabe 9.3 und den DNS-Server von Laptop-1 prüfen)",
                        "The answer of the DNS server must name the MX record of " + DOMAIN_RGG + " (if it is missing, check task 9.3 and the DNS server of Laptop-1)",
                        (n, e, s) -> e.gibt(DNS, s, x -> x.knoten == laptop(n, 1) && x.werte.stream().anyMatch(w -> w.startsWith(DOMAIN_RGG + " MX "))))));
        def("9.5", Regel.schritte(installiert(L1, Netz.EMAILPROGRAMM), konto(L1, anna, mailS), installiert(BUE, Netz.EMAILPROGRAMM), konto(BUE, verkauf, mailR)));
        def("9.6", Regel.schritte(Schritt.n("Laptop-1: im E-Mail-Programm eine Mail an " + ben + " schreiben und senden",
                "Laptop-1: write a mail to " + ben + " in the email program and send it", (n, e, s) -> smtp(e, s, MAIL_SCHULE, null, anna, ben))));
        def("9.7", Regel.schritte(
                Schritt.n("Laptop-1: im E-Mail-Programm eine Mail an " + verkauf + " schreiben und senden", "Laptop-1: write a mail to " + verkauf + " in the email program and send it",
                        (n, e, s) -> smtp(e, s, MAIL_SCHULE, null, anna, verkauf) || smtp(e, s, MAIL_RGG, MAIL_SCHULE, anna, verkauf)),
                Schritt.n("Der Mailserver der Schule muss die Mail an den Mailserver der RGG weitergeben. Kommt sie dort nicht an: Kennt der PC „Mailserver Schule“ einen DNS-Server, "
                        + "und gibt es den MX-Eintrag für " + DOMAIN_RGG + "?",
                        "The school mail server must pass the mail on to the RGG mail server. If it does not arrive there: does the computer “School mail server” know a DNS server, "
                                + "and is there an MX record for " + DOMAIN_RGG + "?",
                        (n, e, s) -> smtp(e, s, MAIL_RGG, MAIL_SCHULE, anna, verkauf))));
        // the first confirmation counts: a second mail does not start the chain anew
        Regel.Nachweis bestaetigt = (n, e, s) -> smtpEreignis(e, s, MAIL_SCHULE, MAIL_RGG, verkauf, anna) != null;
        def("9.8", Regel.schritte(
                Schritt.n("Büro-Laptop: im E-Mail-Programm die Mails abrufen – die Bestellung aus 9.7 liegt im Postfach verkauf",
                        "Office laptop: fetch the mails in the email program – the order from 9.7 is in the mailbox verkauf", (n, e, s) -> {
                            Knoten b = buero(n);
                            return b != null && e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_RGG) && x.typ.equals("FETCH") && x.name.equals("verkauf") && kommtVon(x, b));
                        }),
                Schritt.n("Büro-Laptop: auf die Bestellung antworten – die Auftragsbestätigung geht an " + anna + " und muss beim Mailserver der Schule ankommen",
                        "Office laptop: reply to the order – the confirmation goes to " + anna + " and must arrive at the school mail server", bestaetigt),
                Schritt.n("Heim-Laptop: das E-Mail-Programm installieren, Annas Konto einrichten (wie auf Laptop-1) und die Mails abrufen, nachdem die Bestätigung angekommen ist",
                        "Home laptop: install the email program, set up Anna's account (as on Laptop-1) and fetch the mails after the confirmation has arrived", (n, e, s) -> {
                            Ereignis antwort = smtpEreignis(e, s, MAIL_SCHULE, MAIL_RGG, verkauf, anna);
                            return antwort != null
                                    && e.gibt(IMAP, antwort.zeit, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("FETCH") && x.name.equals("anna") && x.quelle.equals(HEIM_WAN));
                        })));
        def("9.9", Regel.schritte(tippt(MS, "netstat", "netstat")).antwort((n, e, a) -> {
            List<String> z = zahlen(a);
            return z.size() == 2 && z.contains("25") && z.contains("143");
        }));
        // IMAP: a mail deleted on one device is gone on the server; Laptop-1 reads the mailbox again afterwards
        // (after the first deletion: deleting another mail later does not ask for another fetch)
        def("9.10", Regel.schritte(
                Schritt.n("Heim-Laptop: im E-Mail-Programm die Auftragsbestätigung anklicken und „E-Mail löschen“ wählen",
                        "Home laptop: click the confirmation in the email program and choose “Delete email”",
                        (n, e, s) -> e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("EXPUNGE") && x.name.equals("anna"))),
                Schritt.n("Laptop-1: danach die Mails noch einmal abrufen", "Laptop-1: fetch the mails once more afterwards", (n, e, s) -> {
                    Ereignis geloescht = e.erstes(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("EXPUNGE") && x.name.equals("anna"));
                    Knoten l1 = laptop(n, 1);
                    return geloescht != null && l1 != null && e.gibt(IMAP, geloescht.zeit,
                            x -> x.ziel.equals(MAIL_SCHULE) && x.name.equals("anna") && kommtVon(x, l1) && (x.typ.equals("LOGIN") || x.typ.equals("FETCH")));
                })));
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
        return fw != null && fw.isActivated() && sperrt(fw, quelle, ziel, protokoll, port);
    }

    /** the rules of the firewall would drop such a packet (whether the firewall is switched on or not) */
    static boolean sperrt(Firewall fw, String quelle, String ziel, int protokoll, int port) {
        if (fw == null) {
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
        java.util.function.Function<Netz, Knoten> rgg = n -> switchMit(n, "RGG");
        def("11.1", Regel.schritte(stehtMitNamen(INTRA, Rechner.class, "Intranet"), adresse(INTRA, INTRANET), gateway(INTRA, R2_RGG), anSwitch(INTRA, "Switch RGG", "RGG switch", rgg),
                installiert(INTRA, Netz.WEBSERVER),
                Schritt.z("Intranet: mit dem Text-Editor in `webserver/index.html` die Überschrift in „Lager der RGG“ ändern",
                        "Intranet: with the text editor, change the heading in `webserver/index.html` to “RGG warehouse”", n -> {
                            String h1 = Netz.h1(String.valueOf(Netz.datei(intranet(n), "webserver/index.html")).toLowerCase(Locale.GERMAN));
                            return h1.contains("lager") || h1.contains("warehouse");
                        }).ist(n -> intranet(n) == null ? null : "„" + Netz.h1(Netz.datei(intranet(n), "webserver/index.html")) + "“"),
                Schritt.z("Intranet: unter die Überschrift den Absatz `<p>Apfelschorle: 120 Kisten</p>` schreiben und speichern",
                        "Intranet: write the paragraph `<p>Sparkling apple juice: 120 crates</p>` below the heading and save", n -> {
                            String seite = String.valueOf(Netz.datei(intranet(n), "webserver/index.html")).toLowerCase(Locale.GERMAN);
                            return seite.contains("apfelschorle") || seite.contains("apple");
                        }),
                laeuft(INTRA, Netz.WEBSERVER)));
        def("11.2", Regel.schritte(
                seite("Büro-Laptop: im Webbrowser die Lager-Seite http://" + INTRANET + " öffnen", "Office laptop: open the warehouse page http://" + INTRANET + " in the web browser",
                        (n, e, s) -> http(e, s, buero(n), INTRANET, 80, null)),
                seite("Büro-Laptop: im E-Mail-Programm die Mails des Verkaufs abrufen", "Office laptop: fetch the mails of the sales account in the email program",
                        (n, e, s) -> imapVon(e, s, BUERO, "verkauf", "LOGIN", "FETCH"))));
        def("11.3", Regel.schritte(da(HO, Notebook.class),
                Schritt.z("Homeoffice-Laptop: in den Einstellungen „Drahtlos (WLAN)“ wählen und darunter Heim-WLAN", "Home office laptop: choose “Wireless (Wi-Fi)” in the settings and Home-WiFi below it",
                        n -> imWlan(n, homeoffice(n), heimSwitch(n))),
                dhcpAn(HO),
                Schritt.z("Die Simulation starten – der Homeoffice-Laptop bekommt dann eine Adresse aus dem Heimnetz (192.168.178.…)",
                        "Start the simulation – the home office laptop then gets an address of the home network (192.168.178.…)",
                        n -> homeoffice(n) != null && inNetz(Netz.ip(homeoffice(n)), HEIMNETZ)).ist(n -> homeoffice(n) == null ? null : Netz.ip(homeoffice(n))),
                installiert(HO, Netz.WEBBROWSER), installiert(HO, Netz.EMAILPROGRAMM), konto(HO, verkauf, "mail." + DOMAIN_RGG)));
        def("11.4", Regel.schritte(seite("Homeoffice-Laptop: im Webbrowser http://" + INTRANET + " öffnen", "Home office laptop: open http://" + INTRANET + " in the web browser",
                (n, e, s) -> http(e, s, homeoffice(n), INTRANET, 80, null))).uebergang());
        def("11.5", Regel.schritte(
                Schritt.z("R2 Router RGG, „Firewall einrichten“: Regel 1 anlegen – Ziel-IPv4 " + INTRANET + ", Netzmaske 255.255.255.255, Protokoll TCP, Port leer, Aktion verwerfen",
                        "R2 RGG router, “Firewall settings”: add rule 1 – dest. IPv4 " + INTRANET + ", netmask 255.255.255.255, protocol TCP, port empty, action drop",
                        n -> Netz.firewall(r2(n)) != null && sperrt(Netz.firewall(r2(n)), HEIM_WAN, INTRANET, 6, 80)),
                Schritt.z("R2: Regel 2 anlegen – Ziel-IPv4 " + MAIL_RGG + ", Netzmaske 255.255.255.255, Protokoll TCP, Port 143, Aktion verwerfen",
                        "R2: add rule 2 – dest. IPv4 " + MAIL_RGG + ", netmask 255.255.255.255, protocol TCP, port 143, action drop",
                        n -> Netz.firewall(r2(n)) != null && sperrt(Netz.firewall(r2(n)), HEIM_WAN, MAIL_RGG, 6, 143)),
                Schritt.z("R2: die Standardaktion auf „akzeptieren (Blacklist)“ stellen – der Shop, Mails an die RGG, das VPN und DNS-Antworten müssen offen bleiben",
                        "R2: set the default action to “accept (blacklist)” – the shop, mails to the RGG, the VPN and DNS answers must stay open", n -> {
                            Firewall fw = Netz.firewall(r2(n));
                            return fw != null && !sperrt(fw, HEIM_WAN, SHOP, 6, 80) && !sperrt(fw, MAIL_SCHULE, MAIL_RGG, 6, 25)
                                    && !sperrt(fw, HEIM_WAN, VPN_SERVER, 17, filius.software.vpn.Vpn.PORT) && !sperrt(fw, DNS_SERVER, BUERO, 17, 53);
                        }).ist(Regeln::zuViel),
                Schritt.z("R2: den Haken „Firewall aktivieren“ setzen", "R2: tick “Activate firewall”", n -> Netz.firewall(r2(n)) != null && Netz.firewall(r2(n)).isActivated())));
        def("11.6", Regel.schritte(Schritt.z("Die Firewall an R2 ist eingerichtet wie in Aufgabe 11.5", "The firewall at R2 is set up as in task 11.5", Regeln::rggFirewallOk),
                seite("Homeoffice-Laptop: die Lager-Seite http://" + INTRANET + " öffnen oder die Mails abrufen und abwarten – es kommt nichts mehr",
                        "Home office laptop: open the warehouse page http://" + INTRANET + " or fetch the mails and wait – nothing comes any more",
                        (n, e, s) -> homeoffice(n) != null && e.gibt(SYN_OHNE_ANTWORT, s, x -> x.knoten == homeoffice(n) && (x.ziel.equals(INTRANET) || x.ziel.equals(MAIL_RGG)))),
                seite("Büro-Laptop: die Lager-Seite http://" + INTRANET + " öffnen – dort klappt es weiter", "Office laptop: open the warehouse page http://" + INTRANET + " – there it still works",
                        (n, e, s) -> http(e, s, buero(n), INTRANET, 80, null))).uebergang());
        def("11.7", Regel.schritte(stehtMitNamen(VPNS, Rechner.class, "VPN-Server"), adresse(VPNS, VPN_SERVER), gateway(VPNS, R2_RGG), anSwitch(VPNS, "Switch RGG", "RGG switch", rgg),
                installiert(VPNS, Netz.VPNSERVER),
                vpn("VPN-Server: im Programm den Benutzer verkauf mit dem Passwort " + VPN_PASSWORT + " anlegen (Groß- und Kleinschreibung zählt)",
                        "VPN server: add the user verkauf with the password " + VPN_PASSWORT + " in the program (upper and lower case matter)", v -> {
                            boolean konto = false;
                            for (Map.Entry<String, String> x : v.konten().entrySet()) {
                                konto |= x.getKey().equalsIgnoreCase("verkauf") && x.getValue().equals(VPN_PASSWORT);
                            }
                            return konto;
                        }, null),
                vpn("VPN-Server: Das „Netz im Tunnel“ muss das Netz der RGG sein (" + RGGNETZ + " / " + MASKE + ")",
                        "VPN server: the “network in the tunnel” must be the network of the RGG (" + RGGNETZ + " / " + MASKE + ")", v -> {
                            String[] netz = v.freigegebenesNetz();
                            return Netz.gleichesNetz(INTRANET, netz[0], netz[1]) && Netz.gleichesNetz(MAIL_RGG, netz[0], netz[1]);
                        }, v -> v.freigegebenesNetz()[0] + " / " + v.freigegebenesNetz()[1]),
                vpn("VPN-Server: auf „Starten“ klicken", "VPN server: click “Start”", filius.software.vpn.VpnServer::isAktiv, null)));
        def("11.8", Regel.schritte(installiert(HO, Netz.VPNCLIENT),
                Schritt.n("Homeoffice-Laptop: im VPN-Client den Server " + VPN_SERVER + ", den Benutzer verkauf und das Passwort " + VPN_PASSWORT + " eintragen und verbinden – er muss „Verbunden“ melden",
                        "Home office laptop: enter the server " + VPN_SERVER + ", the user verkauf and the password " + VPN_PASSWORT + " in the VPN client and connect – it must report “Connected”",
                        (n, e, s) -> vpnZugang(e, s, homeoffice(n)) != null))
                .antwort((n, e, a) -> {
                    Ereignis z = vpnZugang(e, 0, homeoffice(n));
                    return z != null && !z.werte.isEmpty() && ipAntwort(a, z.werte.get(0));
                }));
        def("11.9", Regel.schritte(vpnVerbunden(),
                Schritt.n("Homeoffice-Laptop: bei verbundenem VPN im E-Mail-Programm die Mails des Verkaufs abrufen",
                        "Home office laptop: with the VPN connected, fetch the mails of the sales account in the email program", Regeln::vpnMailsDirekt)));
        def("11.10", Regel.schritte(vpnVerbunden(),
                Schritt.n("Homeoffice-Laptop: bei verbundenem VPN im Webbrowser die Lager-Seite http://" + INTRANET + " öffnen",
                        "Home office laptop: with the VPN connected, open the warehouse page http://" + INTRANET + " in the web browser", (n, e, s) -> {
                            Ereignis z = vpnErster(e, s, homeoffice(n));
                            return z != null && http(e, z.zeit, homeoffice(n), INTRANET, 80, null);
                        })).antwort((n, e, a) -> {
                            Knoten v = vpnServer(n);
                            return v != null && ipAntwort(a, Netz.ip(v));
                        }));
        def("11.11", Regel.schritte(austausch(HR)));
        def("11.12", Regel.schritte(Schritt.z("Im Dokumentationsmodus ein Textfeld mit dem Text „VPN, UDP 1194“ direkt neben den VPN-Server legen",
                "In the documentation mode, put a text field with the text “VPN, UDP 1194” right next to the VPN server", n -> textNeben(vpnServer(n), tx -> {
                    String x = tx.toLowerCase(Locale.ROOT);
                    return x.contains("vpn") && x.contains("1194");
                }))));
    }

    private static void kapitel12() {
        // final project: eight of these faults per variant; each one counts when the cause is fixed and the
        // end-to-end test of the ticket was observed afterwards
        String neuStart = "die Simulation neu starten (kurz in den Entwurfsmodus und zurück), dann ";
        String restart = "restart the simulation (switch to the design mode and back), then ";
        def("A1", stoerung(n -> {
            DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
            return s != null && R1_SCHULE.equals(s.determineGatewayip());
        }, neuStart + "auf einem Schul-Laptop eine Website über ihren Namen öffnen, zum Beispiel " + WWW_SHOP,
                restart + "open a website by its name on a school laptop, for example " + WWW_SHOP,
                (n, e, s) -> e.gibt(DNS, s, x -> schulLaptops(n).contains(x.knoten) && Netz.dhcp(x.knoten))));
        def("A2", stoerung(n -> ripAn(r2(n)), "von einem Schul-Laptop den Shop anpingen (`ping " + SHOP + "`) oder die Shop-Seite öffnen",
                "ping the shop from a school laptop (`ping " + SHOP + "`) or open the shop page",
                (n, e, s) -> e.gibt(PING_ANTWORT, s, x -> schulGeraet(n, x.knoten) && inNetz(x.ziel, RGGNETZ))
                        || e.gibt(TCP_VERBUNDEN, s, x -> schulGeraet(n, x.knoten) && inNetz(x.ziel, RGGNETZ))));
        def("A3", stoerung(n -> Netz.laeuft(shop(n), Netz.WEBSERVER), "die Shop-Seite in einem Webbrowser öffnen", "open the shop page in a web browser",
                (n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(SHOP) && x.status == 200)));
        def("A4", stoerung(n -> {
            List<String> a = Netz.record(dnsServer(n), WWW_SHOP, "A");
            return !a.isEmpty() && a.stream().allMatch(SHOP::equals);
        }, "die Shop-Seite über " + WWW_SHOP + " öffnen", "open the shop page at " + WWW_SHOP,
                (n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(SHOP) && x.status == 200 && x.host.startsWith(WWW_SHOP))));
        def("A5", stoerung(n -> Netz.record(dnsServer(n), DOMAIN_RGG, "MX").contains("mail." + DOMAIN_RGG),
                "von Annas Konto eine Mail an verkauf@" + DOMAIN_RGG + " senden – sie muss beim Mailserver der RGG ankommen",
                "send a mail from Anna's account to verkauf@" + DOMAIN_RGG + " – it must arrive at the RGG mail server", (n, e, s) -> smtp(e, s, MAIL_RGG, MAIL_SCHULE, null, null)));
        def("A6", stoerung(n -> {
            DHCPServer d = Netz.dhcpServer(heimrouter(n));
            return d != null && DNS_SERVER.equals(d.determineDnsserverip());
        }, neuStart + "auf dem Heim-Laptop eine Website über ihren Namen öffnen", restart + "open a website by its name on the home laptop",
                (n, e, s) -> e.gibt(DNS, s, x -> heimGeraet(x.knoten))));
        def("A7", stoerung(Regeln::portfreigabeOk, "das Blog von einem Schul-Laptop aus unter http://" + HEIM_WAN + ":8080 öffnen",
                "open the blog at http://" + HEIM_WAN + ":8080 from a school laptop", Regeln::blogVonAussen));
        def("A8", Regel.schritte(
                Schritt.n("Die Ursache finden und beheben, dann auf dem Heim-Laptop die Shop-Seite öffnen – sie muss wieder erscheinen",
                        "Find the cause and fix it, then open the shop page on the home laptop – it must appear again", (n, e, s) -> http(e, s, heimLaptop(n), SHOP, 80, null)),
                Schritt.n("Für das Tablet soll der Shop gesperrt bleiben: dort die Shop-Seite aufrufen und abwarten – es darf keine Seite kommen",
                        "For the tablet the shop is to stay blocked: open the shop page there and wait – no page may come",
                        (n, e, s) -> e.gibt(SYN_OHNE_ANTWORT, s, x -> x.knoten == tablet(n) && x.ziel.equals(SHOP) && x.port == 80))));
        def("A9", stoerung(n -> {
            Anwendung a = Netz.software(mailSchule(n), Netz.EMAILSERVER);
            return a instanceof EmailServer && ((EmailServer) a).isAktiv();
        }, "Annas Mails abrufen (auf Laptop-1 oder auf dem Heim-Laptop)", "fetch Anna's mails (on Laptop-1 or on the home laptop)",
                (n, e, s) -> e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE))));
        def("A10", stoerung(n -> Netz.dhcp(laptop(n, 3)), neuStart + "auf Laptop-3 eine Website über ihren Namen öffnen", restart + "open a website by its name on Laptop-3",
                (n, e, s) -> e.gibt(DNS, s, x -> x.knoten == laptop(n, 3))));
        // HTTPS of the school website (chapter 4)
        def("A11", stoerung(n -> {
            filius.software.www.WebServer w = webserverProgramm(n);
            return w != null && w.isHttps() && w.isAktiv();
        }, "die Schulseite über https://" + WWW_SCHULE + " öffnen", "open the school page at https://" + WWW_SCHULE,
                (n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER) && x.port == filius.modern.tls.Tls.PORT && x.status == 200)));
        // the page loads without a warning: no browser refused the certificate after the fix, or the page was
        // loaded again after the last refusal (a warning from an experiment in between does not block the task for good)
        def("A12", stoerung(n -> httpsOk(n, WEBSERVER), "die Schulseite über https://" + WWW_SCHULE + " neu laden – der Browser darf nicht mehr warnen",
                "reload the school page at https://" + WWW_SCHULE + " – the browser must not warn any more", (n, e, s) -> {
                    Ereignis warnung = e.letztes(TLS, s, x -> "Alert".equals(x.name) && x.ziel.equals(WEBSERVER));
                    long ab = warnung == null ? s : warnung.zeit + 1;
                    return e.gibt(HTTP, ab, x -> x.ziel.equals(WEBSERVER) && x.port == filius.modern.tls.Tls.PORT && x.status == 200 && x.host.startsWith(WWW_SCHULE));
                }));
        // home office with VPN (chapter 11)
        String vpnTestDe = "auf dem Homeoffice-Laptop den VPN-Client verbinden und die Mails des Verkaufs abrufen";
        String vpnTestEn = "connect the VPN client on the home office laptop and fetch the mails of the sales account";
        def("A13", stoerung(Regeln::vpnServerOk, vpnTestDe, vpnTestEn, Regeln::vpnMails));
        def("A14", stoerung(Regeln::rggFirewallOk, "die Shop-Seite von einem Schul-Laptop oder von zu Hause aus öffnen", "open the shop page from a school laptop or from home",
                (n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(SHOP) && x.status == 200 && (schulGeraet(n, x.knoten) || heimGeraet(x.knoten)))));
        def("A15", stoerung(n -> Netz.laeuft(vpnServer(n), Netz.VPNSERVER), vpnTestDe, vpnTestEn, Regeln::vpnMails));
        def("A16", stoerung(n -> Netz.fest(intranet(n), INTRANET, MASKE), "auf dem Büro-Laptop die Lager-Seite http://" + INTRANET + " öffnen",
                "open the warehouse page http://" + INTRANET + " on the office laptop", (n, e, s) -> http(e, s, buero(n), INTRANET, 80, null)));
        def("A17", stoerung(n -> programmOk(buero(n), "verkauf@" + DOMAIN_RGG, "mail." + DOMAIN_RGG), "auf dem Büro-Laptop die Mails des Verkaufs abrufen",
                "fetch the mails of the sales account on the office laptop", (n, e, s) -> imapVon(e, s, BUERO, "verkauf", "LOGIN", "FETCH")));
        def("A18", stoerung(n -> Netz.laeuft(blog(n), Netz.WEBSERVER), "das Blog öffnen – im Heimnetz unter http://" + BLOG + " oder von außen unter http://" + HEIM_WAN + ":8080",
                "open the blog – in the home network at http://" + BLOG + " or from outside at http://" + HEIM_WAN + ":8080",
                (n, e, s) -> e.gibt(HTTP, s, x -> x.status == 200 && (x.ziel.equals(BLOG) || x.ziel.equals(HEIM_WAN) && x.port == 8080) && seiteMit(x, "blog"))));
        def("A19", stoerung(n -> amSwitch(n, webserver(n), switchSchule(n)), "die Schulseite in einem Webbrowser öffnen", "open the school page in a web browser",
                (n, e, s) -> e.gibt(HTTP, s, x -> x.ziel.equals(WEBSERVER) && x.status == 200)));
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
