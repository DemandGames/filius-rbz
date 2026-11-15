package filius.modern.lernpfad;

import static filius.modern.lernpfad.Ereignis.Art.BEFEHL;
import static filius.modern.lernpfad.Teile.adresse;
import static filius.modern.lernpfad.Teile.anSwitch;
import static filius.modern.lernpfad.Teile.dhcpAn;
import static filius.modern.lernpfad.Teile.eigeneUeberschrift;
import static filius.modern.lernpfad.Teile.mitEtikett;
import static filius.modern.lernpfad.Teile.flaeche;
import static filius.modern.lernpfad.Teile.austausch;
import static filius.modern.lernpfad.Teile.da;
import static filius.modern.lernpfad.Teile.dns;
import static filius.modern.lernpfad.Teile.g;
import static filius.modern.lernpfad.Teile.gateway;
import static filius.modern.lernpfad.Teile.installiert;
import static filius.modern.lernpfad.Teile.laeuft;
import static filius.modern.lernpfad.Teile.pingt;
import static filius.modern.lernpfad.Teile.t;
import static filius.modern.lernpfad.Teile.tippt;
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
import filius.modern.lernpfad.Teile.G;

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
        // the device that had the client address when the mails were fetched (with DHCP the address can be another one later)
        return von == null ? null : e.erstes(IMAP, seit, x -> x.ziel.equals(server) && x.typ.equals("FETCH") && x.name.equalsIgnoreCase(konto)
                && (x.gegenstelle == von || x.gegenstelle == null && x.quelle.equals(Netz.ip(von))));
    }

    // ------------------------------------------------------------------ devices as the hints name them

    private static final G L1 = g("Laptop-1", n -> laptop(n, 1));
    private static final G L2 = g("Laptop-2", n -> laptop(n, 2));
    private static final G L3 = g("Laptop-3", n -> laptop(n, 3));
    private static final G L4 = g("Laptop-4", n -> laptop(n, 4));
    private static final G LEHRERG = g("Lehrer-Laptop", "Teacher laptop", RegelnStandard::lehrerLaptop);
    private static final G WS = g("Webserver", "Web server", RegelnStandard::webserver);
    private static final G SH = g("Shop", RegelnStandard::shop);
    private static final G BUE = g("Büro-Laptop", "Office laptop", RegelnStandard::buero);
    private static final G DNSG = g("DNS-Server", "DNS server", RegelnStandard::dnsServer);

    /** the device with exactly this name (the task asks for the name) */
    private static G nur(String de, String en) {
        return g(de, en, n -> n.knoten(de));
    }

    private static G nur(String name) {
        return nur(name, name);
    }

    /** an A or MX record of the DNS server */
    private static Schritt eintrag(String typ, String name, String wert) {
        boolean mx = typ.equals("MX");
        return Schritt.z("DNS-Server: " + (mx ? "MX-Eintrag " : "A-Eintrag ") + name + " → " + wert + " anlegen (Reiter „" + (mx ? "Mailaustausch (MX)" : "Adressen (A)") + "“)",
                "DNS server: add the " + typ + " record " + name + " → " + wert, n -> Netz.record(dnsServer(n), name, typ).contains(wert))
                .ist(n -> {
                    List<String> w = Netz.record(dnsServer(n), name, typ);
                    return w.isEmpty() ? t("kein Eintrag für " + name, "no record for " + name) : name + " → " + String.join(", ", w);
                });
    }

    private static EmailServer mailProgramm(Knoten k) {
        Anwendung a = Netz.software(k, Netz.EMAILSERVER);
        return a instanceof EmailServer ? (EmailServer) a : null;
    }

    /** the steps of a mail server: PC, addresses, cable, program, domain, accounts, started */
    private static List<Schritt> mailserver(G k, String ip, String gw, String swDe, String swEn, java.util.function.Function<Netz, Knoten> sw, String domain,
            String... konten) {
        List<Schritt> l = new ArrayList<>();
        l.add(da(k, Rechner.class));
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
                    n -> mailProgramm(k.in(n)) != null && Regeln.hatKonto(mailProgramm(k.in(n)), konto)));
        }
        l.add(Schritt.z(k.de + ": den E-Mail-Server starten", k.en + ": start the email server",
                n -> mailProgramm(k.in(n)) != null && mailProgramm(k.in(n)).isAktiv()));
        return l;
    }

    private static Regel regel(List<Schritt> s) {
        return Regel.schritte(s.toArray(new Schritt[0]));
    }

    /** a fault of the final project: the cause is fixed, then the test of the ticket */
    private static Regel stoerung(Regel.Zustand behoben, String testDe, String testEn, Regel.Nachweis test) {
        return Regel.schritte(Schritt.z("Die Ursache finden und beheben", "Find the cause and fix it", behoben),
                Schritt.n("Danach testen: " + testDe, "Then test: " + testEn, test));
    }

    // ================================================================== chapter 1

    private static void kapitel1() {
        G l1 = nur("Laptop-1");
        G l2 = nur("Laptop-2");
        def("1.1", Regel.schritte(da(l1, Notebook.class), da(l2, Notebook.class)));
        def("1.2", Regel.schritte(Schritt.z("Laptop-1 und Laptop-2 mit einem Kabel verbinden", "Connect Laptop-1 and Laptop-2 with a cable",
                n -> n.verbunden(n.knoten("Laptop-1"), n.knoten("Laptop-2")))));
        def("1.3", Regel.schritte(adresse(l1, "192.168.0.10"), adresse(l2, "192.168.0.11")));
        def("1.4", Regel.schritte(adresse(L2, "192.168.0.11"), installiert(L1, Netz.TERMINAL).frei(), pingt(L1, "192.168.0.11")));
        def("1.5", Regel.schritte(pingt(L1, "192.168.0.11")));
        def("1.6", Regel.schritte(austausch(L1), Schritt.n("Laptop-1: Laptop-2 noch einmal anpingen (`ping 192.168.0.11`)", "Laptop-1: ping Laptop-2 once more (`ping 192.168.0.11`)",
                (n, e, s) -> e.gibt(PING_ANTWORT, s, x -> x.knoten == laptop(n, 1)))));
        // the experiment: a ping while Laptop-1 had no cable; afterwards the cable is back
        def("1.7", Regel.schritte(
                Schritt.n("Das Kabel im Entwurfsmodus löschen, dann in der Simulation auf Laptop-1 `ping 192.168.0.11` eingeben",
                        "Delete the cable in the design mode, then type `ping 192.168.0.11` on Laptop-1 in the simulation", (n, e, s) -> {
                            Knoten k = laptop(n, 1);
                            return ping(e, 0, k, "192.168.0.11", x -> x.port == 0) != null
                                    || e.gibt(PING_OHNE_ANTWORT, 0, x -> x.knoten == k && x.ziel.equals("192.168.0.11"));
                        }),
                Schritt.z("Danach Laptop-1 und Laptop-2 wieder mit einem Kabel verbinden", "Afterwards connect Laptop-1 and Laptop-2 with a cable again",
                        n -> n.verbunden(laptop(n, 1), laptop(n, 2))).frei().danach()));
    }

    // ================================================================== chapter 2

    private static void kapitel2() {
        java.util.function.Function<Netz, Knoten> sw = n -> n.knoten("Switch Schule");
        G l3 = nur("Laptop-3");
        G l4 = nur("Laptop-4");
        def("2.1", Regel.schritte(
                Schritt.z("Das Kabel zwischen Laptop-1 und Laptop-2 löschen (Rechtsklick auf einen der Laptops)",
                        "Delete the cable between Laptop-1 and Laptop-2 (right-click one of the laptops)", n -> !n.verbunden(laptop(n, 1), laptop(n, 2))),
                da(nur("Switch Schule", "School switch"), Switch.class), anSwitch(L1, "Switch Schule", "School switch", sw),
                anSwitch(L2, "Switch Schule", "School switch", sw)));
        def("2.2", Regel.schritte(da(l3, Notebook.class), adresse(l3, "192.168.0.12"), anSwitch(l3, "Switch Schule", "School switch", sw), da(l4, Notebook.class),
                adresse(l4, LEHRER), anSwitch(l4, "Switch Schule", "School switch", sw)));
        def("2.3", Regel.schritte(anSwitch(L1, "Switch Schule", "School switch", RegelnStandard::switchSchule),
                anSwitch(L4, "Switch Schule", "School switch", RegelnStandard::switchSchule), adresse(L4, LEHRER), pingt(L1, LEHRER)));
        def("2.4", Regel.zustand(n -> laptop(n, 4) != null).antwort((n, e, a) -> laptop(n, 4) != null && Regeln.mac(a).equals(Regeln.mac(Netz.mac(laptop(n, 4))))));
        def("2.5", Regel.schritte(Schritt.n("In der Simulation den Switch Schule anklicken, sodass seine Liste aufgeht (vorher von Laptop-1 aus pingen)",
                "Click the School switch in the simulation so that its list opens (ping from Laptop-1 before)",
                (n, e, s) -> e.gibt(SAT, s, x -> x.knoten == switchSchule(n)))).antwort((n, e, a) -> {
                    Knoten s = switchSchule(n);
                    Knoten k = laptop(n, 4);
                    if (!(s instanceof Switch) || k == null) {
                        return false;
                    }
                    String erwartet = Regeln.satAnschluss((Switch) s, Netz.mac(k));
                    if (erwartet == null) {
                        erwartet = String.valueOf(n.switchAnschluss((Switch) s, k));
                    }
                    List<String> z = Regeln.zahlen(a);
                    return z.size() == 1 && z.get(0).equals(erwartet);
                }));
        // experiment: Laptop-4 in another network part, Laptop-1 pings it in vain
        def("2.6", Regel.schritte(
                Schritt.z("Laptop-4: im Entwurfsmodus die IPv4-Adresse " + ANDERES_NETZ + " eintragen", "Laptop-4: enter the IPv4 address " + ANDERES_NETZ + " in the design mode",
                        n -> ANDERES_NETZ.equals(Netz.ip(n.knoten("Laptop-4")))).ist(n -> n.knoten("Laptop-4") == null ? null : Netz.ip(n.knoten("Laptop-4"))),
                Schritt.n("Laptop-1: in der Simulation `ping " + ANDERES_NETZ + "` eingeben", "Laptop-1: type `ping " + ANDERES_NETZ + "` in the simulation",
                        (n, e, s) -> ping(e, s, laptop(n, 1), ANDERES_NETZ, null) != null
                                || e.gibt(PING_OHNE_ANTWORT, s, x -> x.knoten == laptop(n, 1) && x.ziel.equals(ANDERES_NETZ))))
                .uebergang());
        // back to 192.168.0.13: a ping after the experiment gets an answer again
        def("2.7", Regel.schritte(
                Schritt.n("Zuerst das Experiment aus Aufgabe 2.6 machen (`ping " + ANDERES_NETZ + "` auf Laptop-1)",
                        "First do the experiment of task 2.6 (`ping " + ANDERES_NETZ + "` on Laptop-1)", (n, e, s) -> ping(e, 0, laptop(n, 1), ANDERES_NETZ, null) != null),
                adresse(L4, LEHRER), anSwitch(L4, "Switch Schule", "School switch", RegelnStandard::switchSchule),
                Schritt.n("Laptop-1: nach dem Experiment `ping " + LEHRER + "` eingeben – Laptop-4 muss wieder antworten (Simulation neu starten)",
                        "Laptop-1: after the experiment type `ping " + LEHRER + "` – Laptop-4 must answer again (restart the simulation)", (n, e, s) -> {
                            Ereignis experiment = ping(e, 0, laptop(n, 1), ANDERES_NETZ, null);
                            return experiment != null && pingOk(e, Math.max(s, experiment.zeit), laptop(n, 1), LEHRER);
                        })));
        def("2.8", Regel.schritte(flaeche("Schulnetz", "school network", RegelnStandard::switchSchule),
                mitEtikett("Oben links in die Fläche das Etikett „Schulnetz 192.168.0.0“ schreiben (Klick auf das +)",
                        "Write the label “School network 192.168.0.0” at the top left of the field (click the +)", RegelnStandard::switchSchule,
                        Regeln.mitAdressen(SCHULNETZ))));
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
        G ws = nur("Webserver", "Web server");
        def("3.1", Regel.schritte(da(ws, Rechner.class), adresse(ws, WEBSERVER), anSwitch(ws, "Switch Schule", "School switch", RegelnStandard::switchSchule)));
        def("3.2", Regel.schritte(installiert(WS, Netz.WEBSERVER), laeuft(WS, Netz.WEBSERVER)));
        def("3.3", Regel.schritte(installiert(L2, Netz.WEBBROWSER),
                Schritt.n("Laptop-2: im Webbrowser http://" + WEBSERVER + " aufrufen – die Startseite muss erscheinen",
                        "Laptop-2: open http://" + WEBSERVER + " in the web browser – the start page must appear",
                        (n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, Regeln::startseite))));
        def("3.4", Regel.schritte(eigeneUeberschrift(WS)));
        def("3.5", Regel.schritte(
                Schritt.z("Webserver: in `webserver/index.html` unter die Überschrift einen eigenen Absatz schreiben (`<p>…</p>`) und speichern",
                        "Web server: write a paragraph of your own below the heading in `webserver/index.html` (`<p>…</p>`) and save",
                        n -> eigenerAbsatz(Netz.datei(webserver(n), "webserver/index.html")) != null),
                Schritt.n("Laptop-2: die Seite http://" + WEBSERVER + " im Webbrowser neu laden – dein Absatz muss erscheinen",
                        "Laptop-2: reload the page http://" + WEBSERVER + " in the web browser – your paragraph must appear", (n, e, s) -> {
                            String absatz = eigenerAbsatz(Netz.datei(webserver(n), "webserver/index.html"));
                            return absatz != null && http(e, s, laptop(n, 2), WEBSERVER, x -> Regeln.startseite(x) && absaetze(x.inhalt.toString()).contains(absatz));
                        })));
        def("3.6", Regel.schritte(austausch(L2), Schritt.n("Laptop-2: die Seite http://" + WEBSERVER + " noch einmal aufrufen",
                "Laptop-2: open the page http://" + WEBSERVER + " once more",
                (n, e, s) -> e.gibt(TCP_VERBUNDEN, s, x -> x.knoten == laptop(n, 2) && x.ziel.equals(WEBSERVER)))));
        def("3.7", Regel.schritte(austausch(L2), Schritt.n("Laptop-2: die Seite des Webservers aufrufen, sodass die HTTP-Antwort im Datenaustausch steht",
                "Laptop-2: open the page of the web server so that the HTTP response is in the data exchange", (n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, null))));
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

    /** a card of the router has this address and the net mask */
    private static Schritt karte(String nrDe, String nrEn, String ip) {
        return Schritt.z("Router, " + nrDe + " Karte: IPv4-Adresse " + ip + " und Netzmaske " + MASKE + " eintragen (jede Karte hat einen eigenen Reiter)",
                "Router, " + nrEn + " card: enter the IPv4 address " + ip + " and the netmask " + MASKE + " (every card has a tab of its own)", n -> {
                    NetzwerkInterface nic = Netz.nic(router(n), ip);
                    return nic != null && MASKE.equals(nic.getSubnetzMaske());
                }).ist(n -> {
                    List<String> l = new ArrayList<>();
                    for (NetzwerkInterface nic : Netz.nics(router(n))) {
                        l.add(nic.getIp() + " / " + nic.getSubnetzMaske());
                    }
                    return l.isEmpty() ? null : t("Karten: ", "cards: ") + String.join("; ", l);
                });
    }

    private static void kapitel4() {
        def("4.1", Regel.schritte(
                Schritt.z("Vermittlungsrechner mit dem Namen „Router“ aufstellen", "Add a router named “Router”", n -> router(n) != null && Netz.heisst(router(n), "Router"))
                        .ist(n -> router(n) == null ? null : "„" + router(n).getName() + "“"),
                Schritt.z("Der Router braucht genau drei Schnittstellen (in seinen Einstellungen: „Verbindungen verwalten“)",
                        "The router needs exactly three interfaces (in its settings: “Manage connections”)", n -> Netz.nics(router(n)).size() == 3)
                        .ist(n -> router(n) == null ? null : String.valueOf(Netz.nics(router(n)).size())),
                karte("1.", "1st", ROUTER_SCHULE), karte("2.", "2nd", ROUTER_RGG), karte("3.", "3rd", ROUTER_DNS),
                Schritt.z("Die Karte " + ROUTER_SCHULE + " des Routers mit dem Switch Schule verbinden", "Connect the card " + ROUTER_SCHULE + " of the router to the School switch",
                        n -> karteAn(n, router(n), ROUTER_SCHULE, switchSchule(n)))));
        def("4.2", Regel.schritte(gateway(L1, ROUTER_SCHULE), gateway(L2, ROUTER_SCHULE), gateway(L3, ROUTER_SCHULE),
                gateway(g("Laptop-4", RegelnStandard::lehrerLaptop), ROUTER_SCHULE), gateway(WS, ROUTER_SCHULE)));
        G rgg = nur("Switch RGG", "RGG switch");
        G shop = nur("Shop");
        G buero = nur("Büro-Laptop", "Office laptop");
        def("4.3", Regel.schritte(
                Schritt.z("Der Router ist eingerichtet wie in Aufgabe 4.1", "The router is set up as in task 4.1", RegelnStandard::routerOk), da(rgg, Switch.class),
                Schritt.z("Den Switch RGG mit der 2. Karte des Routers (" + ROUTER_RGG + ") verbinden", "Connect the RGG switch to the 2nd card of the router (" + ROUTER_RGG + ")",
                        n -> karteAn(n, router(n), ROUTER_RGG, rgg.in(n))),
                da(shop, Rechner.class), adresse(shop, SHOP), gateway(shop, ROUTER_RGG), anSwitch(shop, "Switch RGG", "RGG switch", rgg.finde),
                da(buero, Notebook.class), adresse(buero, BUERO), gateway(buero, ROUTER_RGG), anSwitch(buero, "Switch RGG", "RGG switch", rgg.finde)));
        def("4.4", Regel.schritte(installiert(SH, Netz.WEBSERVER),
                Schritt.z("Shop: mit dem Text-Editor in `webserver/index.html` die Überschrift in „Ravensberger24 – Getränke online“ ändern und speichern",
                        "Shop: with the text editor, change the heading in `webserver/index.html` to “Ravensberger24 – Drinks online” and save",
                        n -> Netz.h1(Netz.datei(shop(n), "webserver/index.html")).toLowerCase(Locale.ROOT).contains("ravensberger24"))
                        .ist(n -> shop(n) == null ? null : "„" + Netz.h1(Netz.datei(shop(n), "webserver/index.html")) + "“"),
                laeuft(SH, Netz.WEBSERVER)));
        def("4.5", Regel.schritte(pingt(L1, SHOP), Schritt.n("Laptop-2: im Webbrowser http://" + SHOP + " öffnen – die Shop-Seite muss erscheinen",
                "Laptop-2: open http://" + SHOP + " in the web browser – the shop page must appear",
                (n, e, s) -> http(e, s, laptop(n, 2), SHOP, x -> Regeln.seiteMit(x, "Ravensberger24")))));
        // experiment: a ping to the shop while Laptop-3 had no gateway; afterwards the gateway is back
        def("4.6", Regel.schritte(
                Schritt.n("Bei Laptop-3 das Gateway löschen, dann in der Simulation auf Laptop-3 `ping " + SHOP + "` eingeben",
                        "Delete the gateway of Laptop-3, then type `ping " + SHOP + "` on Laptop-3 in the simulation", (n, e, s) -> {
                            Knoten k = laptop(n, 3);
                            return ping(e, 0, k, SHOP, x -> x.typ.isEmpty()) != null || e.gibt(PING_OHNE_ANTWORT, 0, x -> x.knoten == k && x.ziel.equals(SHOP));
                        }),
                Schritt.z("Danach bei Laptop-3 das Gateway " + ROUTER_SCHULE + " wieder eintragen", "Afterwards enter the gateway " + ROUTER_SCHULE + " at Laptop-3 again",
                        n -> ROUTER_SCHULE.equals(Netz.gateway(laptop(n, 3)))).frei().danach()));
        def("4.7", Regel.schritte(flaeche("RGG-Netz", "RGG network", RegelnStandard::switchRgg),
                mitEtikett("Oben links in die Fläche ein Etikett mit dem Namen und der Netzadresse des RGG-Netzes schreiben (Klick auf das +)",
                        "Write a label with the name and the network address of the RGG network at the top left of the field (click the +)",
                        RegelnStandard::switchRgg, Regeln.mitAdressen(RGGNETZ)))
                .antwort((n, e, a) -> Regeln.ipAntwort(a, RGGNETZ)));
    }

    // ================================================================== chapter 5

    private static void kapitel5() {
        G sw = nur("Switch DNS-Anbieter", "DNS provider switch");
        G d = nur("DNS-Server", "DNS server");
        def("5.1", Regel.schritte(da(sw, Switch.class),
                Schritt.z("Den Switch DNS-Anbieter mit der 3. Karte des Routers (" + ROUTER_DNS + ") verbinden",
                        "Connect the DNS provider switch to the 3rd card of the router (" + ROUTER_DNS + ")", n -> karteAn(n, router(n), ROUTER_DNS, sw.in(n))),
                da(d, Rechner.class), adresse(d, DNS_SERVER), gateway(d, ROUTER_DNS), anSwitch(d, "Switch DNS-Anbieter", "DNS provider switch", sw.finde)));
        def("5.2", Regel.schritte(installiert(DNSG, Netz.DNSSERVER), eintrag("A", WWW_SCHULE, WEBSERVER), eintrag("A", WWW_SHOP, SHOP), laeuft(DNSG, Netz.DNSSERVER)));
        def("5.3", Regel.schritte(dns(L1, DNS_SERVER), dns(L2, DNS_SERVER), dns(L3, DNS_SERVER), dns(g("Laptop-4", RegelnStandard::lehrerLaptop), DNS_SERVER),
                dns(BUE, DNS_SERVER)));
        def("5.4", Regel.schritte(Schritt.n("Laptop-1: in der Befehlszeile `nslookup " + WWW_SHOP + "` eingeben – der DNS-Server muss antworten",
                "Laptop-1: type `nslookup " + WWW_SHOP + "` in the command line – the DNS server must answer",
                (n, e, s) -> befehl(e, s, laptop(n, 1), "nslookup") && dnsAntwort(e, s, laptop(n, 1), WWW_SHOP, SHOP)))
                .antwort((n, e, a) -> Regeln.ipAntwort(a, SHOP)));
        def("5.5", Regel.schritte(
                Schritt.n("Laptop-2: im Webbrowser " + WWW_SHOP + " öffnen – die Shop-Seite muss erscheinen", "Laptop-2: open " + WWW_SHOP + " in the web browser – the shop page must appear",
                        (n, e, s) -> dnsAntwort(e, s, laptop(n, 2), WWW_SHOP, SHOP) && http(e, s, laptop(n, 2), SHOP, x -> x.host.startsWith(WWW_SHOP))),
                Schritt.n("Büro-Laptop: den Webbrowser installieren und " + WWW_SCHULE + " öffnen – die Schulseite muss erscheinen",
                        "Office laptop: install the web browser and open " + WWW_SCHULE + " – the school page must appear",
                        (n, e, s) -> dnsAntwort(e, s, buero(n), WWW_SCHULE, WEBSERVER) && http(e, s, buero(n), WEBSERVER, x -> x.host.startsWith(WWW_SCHULE)))));
        def("5.6", Regel.schritte(flaeche("Netz des DNS-Anbieters", "network of the DNS provider", RegelnStandard::switchDns),
                mitEtikett("Oben links in die Fläche das Etikett „DNS-Anbieter 192.168.2.0“ schreiben (Klick auf das +)",
                        "Write the label “DNS provider 192.168.2.0” at the top left of the field (click the +)", RegelnStandard::switchDns, Regeln.mitAdressen(DNSNETZ))));
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

    /** the e-mail program of the device has the account with the mail server of its domain */
    private static Schritt konto(G k, String adresse, String server, String serverIp) {
        return Schritt.z(k.de + ": im E-Mail-Programm das Konto " + adresse + " einrichten, als SMTP- und IMAP-Server jeweils " + server,
                k.en + ": set up the account " + adresse + " in the email program, with " + server + " as SMTP and as IMAP server",
                n -> programmOk(k.in(n), adresse, server, serverIp));
    }

    private static void kapitel6() {
        String anna = "anna@" + DOMAIN_SCHULE;
        String ben = "ben@" + DOMAIN_SCHULE;
        String verkauf = "verkauf@" + DOMAIN_RGG;
        String mailRgg = "mail." + DOMAIN_RGG;
        String mailSchule = "mail." + DOMAIN_SCHULE;
        def("6.1", regel(mailserver(nur("Mailserver RGG", "RGG mail server"), MAIL_RGG, ROUTER_RGG, "Switch RGG", "RGG switch", RegelnStandard::switchRgg, DOMAIN_RGG, "verkauf")));
        def("6.2", Regel.schritte(eintrag("A", mailRgg, MAIL_RGG)));
        def("6.3", Regel.schritte(installiert(BUE, Netz.EMAILPROGRAMM), konto(BUE, verkauf, mailRgg, MAIL_RGG)));
        // a test mail to the own mailbox and fetched afterwards
        def("6.4", Regel.schritte(
                Schritt.n("Büro-Laptop: im E-Mail-Programm eine Mail an " + verkauf + " senden („Neu“, dann „Senden“)",
                        "Office laptop: send a mail to " + verkauf + " in the email program (“New”, then “Send”)",
                        (n, e, s) -> smtpEreignis(e, s, MAIL_RGG, null, verkauf, verkauf) != null),
                Schritt.n("Büro-Laptop: danach die Mails abrufen („Abrufen“)", "Office laptop: fetch the mails afterwards (“Fetch”)", (n, e, s) -> {
                    Ereignis mail = smtpEreignis(e, s, MAIL_RGG, null, verkauf, verkauf);
                    return mail != null && abruf(e, mail.zeit, MAIL_RGG, "verkauf", buero(n)) != null;
                })));
        List<Schritt> schule = mailserver(nur("Mailserver Schule", "School mail server"), MAIL_SCHULE, ROUTER_SCHULE, "Switch Schule", "School switch",
                RegelnStandard::switchSchule, DOMAIN_SCHULE, "anna", "ben");
        schule.add(eintrag("A", mailSchule, MAIL_SCHULE));
        def("6.5", regel(schule));
        // Anna writes to Ben, Ben fetches the mail on Laptop-3 (version 30)
        def("6.6", Regel.schritte(konto(L1, anna, mailSchule, MAIL_SCHULE),
                Schritt.n("Laptop-1: als Anna eine Mail an " + ben + " senden", "Laptop-1: send a mail to " + ben + " as Anna",
                        (n, e, s) -> smtpEreignis(e, s, MAIL_SCHULE, null, anna, ben) != null),
                Schritt.n("Laptop-3: das E-Mail-Programm installieren, das Konto " + ben + " einrichten (Server " + mailSchule + ") und die Mail abrufen",
                        "Laptop-3: install the email program, set up the account " + ben + " (server " + mailSchule + ") and fetch the mail", (n, e, s) -> {
                            Ereignis mail = smtpEreignis(e, s, MAIL_SCHULE, null, anna, ben);
                            return mail != null && abruf(e, mail.zeit, MAIL_SCHULE, "ben", laptop(n, 3)) != null;
                        })));
        def("6.7", Regel.schritte(eintrag("MX", DOMAIN_SCHULE, mailSchule), eintrag("MX", DOMAIN_RGG, mailRgg)));
        // the order: Anna -> sales (through both mail servers), fetched in the office, answer, fetched on Laptop-1
        def("6.8", Regel.schritte(
                Schritt.n("Laptop-1: als Anna eine Mail an " + verkauf + " senden", "Laptop-1: send a mail to " + verkauf + " as Anna",
                        (n, e, s) -> bestellung(n, e, s, 1) != null),
                Schritt.n("Büro-Laptop: danach die Mails abrufen", "Office laptop: fetch the mails afterwards", (n, e, s) -> bestellung(n, e, s, 2) != null),
                Schritt.n("Büro-Laptop: auf die Bestellung antworten („Antworten“, dann „Senden“)", "Office laptop: reply to the order (“Reply”, then “Send”)",
                        (n, e, s) -> bestellung(n, e, s, 3) != null),
                Schritt.n("Laptop-1: danach als Anna die Mails abrufen", "Laptop-1: fetch the mails as Anna afterwards", (n, e, s) -> bestellung(n, e, s, 4) != null)));
    }

    /** the order of task 6.8 up to the given step (1 order sent, 2 fetched in the office, 3 answered, 4 answer fetched); null if not that far */
    private static Ereignis bestellung(Netz n, Ereignisse e, long s, int bis) {
        String anna = "anna@" + DOMAIN_SCHULE;
        String verkauf = "verkauf@" + DOMAIN_RGG;
        Ereignis x = smtpEreignis(e, s, MAIL_RGG, MAIL_SCHULE, anna, verkauf);
        if (x != null && bis >= 2) {
            x = abruf(e, x.zeit, MAIL_RGG, "verkauf", buero(n));
        }
        if (x != null && bis >= 3) {
            x = smtpEreignis(e, x.zeit, MAIL_SCHULE, MAIL_RGG, verkauf, anna);
        }
        if (x != null && bis >= 4) {
            x = abruf(e, x.zeit, MAIL_SCHULE, "anna", laptop(n, 1));
        }
        return x;
    }

    // ================================================================== chapter 7

    static boolean dhcpOk(Netz n) {
        DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
        return s != null && s.isAktiv() && DHCP_VON.equals(s.getUntergrenze()) && DHCP_BIS.equals(s.getObergrenze()) && MASKE.equals(s.getSubnetzmaske())
                && ROUTER_SCHULE.equals(s.determineGatewayip()) && DNS_SERVER.equals(s.determineDnsserverip());
    }

    /** a setting of the DHCP server of the school */
    private static Schritt dhcp(String de, String en, java.util.function.Predicate<DHCPServer> ok, java.util.function.Function<DHCPServer, String> ist) {
        return Schritt.z(de, en, n -> {
            DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
            return s != null && ok.test(s);
        }).ist(n -> {
            DHCPServer s = Netz.dhcpServer(dhcpServerPc(n));
            return s == null || ist == null ? null : ist.apply(s);
        });
    }

    private static void kapitel7() {
        G d = nur("DHCP-Server", "DHCP server");
        def("7.1", Regel.schritte(da(d, Rechner.class), adresse(d, DHCP_SERVER), gateway(d, ROUTER_SCHULE), dns(d, DNS_SERVER),
                Schritt.z("DHCP-Server mit einem Kabel an den Switch Schule anschließen", "Connect the DHCP server to the School switch with a cable",
                        n -> n.segment(switchSchule(n)).contains(d.in(n)))));
        def("7.2", Regel.schritte(
                dhcp("DHCP-Server: Knopf „DHCP-Server einrichten“ – Adress-Untergrenze " + DHCP_VON + " eintragen",
                        "DHCP server: button “DHCP server setup” – enter the lower address limit " + DHCP_VON, s -> DHCP_VON.equals(s.getUntergrenze()), DHCPServer::getUntergrenze),
                dhcp("DHCP-Server: Adress-Obergrenze " + DHCP_BIS + " eintragen", "DHCP server: enter the upper address limit " + DHCP_BIS, s -> DHCP_BIS.equals(s.getObergrenze()),
                        DHCPServer::getObergrenze),
                dhcp("DHCP-Server: Netzmaske " + MASKE + " verteilen", "DHCP server: hand out the netmask " + MASKE, s -> MASKE.equals(s.getSubnetzmaske()), DHCPServer::getSubnetzmaske),
                dhcp("DHCP-Server: das Gateway " + ROUTER_SCHULE + " verteilen (er nimmt das Gateway des PCs aus Aufgabe 7.1)",
                        "DHCP server: hand out the gateway " + ROUTER_SCHULE + " (it takes the gateway of the PC of task 7.1)", s -> ROUTER_SCHULE.equals(s.determineGatewayip()),
                        DHCPServer::determineGatewayip),
                dhcp("DHCP-Server: den DNS-Server " + DNS_SERVER + " verteilen (er nimmt den DNS-Server des PCs aus Aufgabe 7.1)",
                        "DHCP server: hand out the DNS server " + DNS_SERVER + " (it takes the DNS server of the PC of task 7.1)", s -> DNS_SERVER.equals(s.determineDnsserverip()),
                        DHCPServer::determineDnsserverip),
                dhcp("DHCP-Server: das Häkchen „DHCP aktivieren“ setzen", "DHCP server: set the tick “Activate DHCP”", DHCPServer::isAktiv, null)));
        def("7.3", Regel.schritte(dhcpAn(L1), dhcpAn(L2), dhcpAn(L3), dhcpAn(g("Laptop-4", RegelnStandard::lehrerLaptop))));
        def("7.4", Regel.schritte(tippt(L2, "ipconfig", "ipconfig")).antwort((n, e, a) -> Regeln.dhcpAntwort(e, laptop(n, 2), a, DHCP_VON, DHCP_BIS)));
        // Laptop-4 is renamed "Lehrer-Laptop" and always gets 192.168.0.13 (version 30)
        def("7.5", Regel.schritte(
                Schritt.z("Laptop-4 in „Lehrer-Laptop“ umbenennen", "Rename Laptop-4 to “Teacher laptop”", n -> n.knoten("Lehrer-Laptop") != null),
                Schritt.z("DHCP-Server, Reiter „Statische Adresszuweisung“: die MAC-Adresse des Lehrer-Laptops mit " + LEHRER + " eintragen",
                        "DHCP server, tab “Static Address Assignment”: enter the MAC address of the teacher laptop with " + LEHRER,
                        n -> LEHRER.equals(Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(lehrerLaptop(n))))).ist(n -> {
                            Knoten k = lehrerLaptop(n);
                            if (k == null) {
                                return null;
                            }
                            String ip = Netz.statischeZuweisung(dhcpServerPc(n), Netz.mac(k));
                            return ip == null ? t("keine Zuweisung für die MAC-Adresse " + Netz.mac(k), "no assignment for the MAC address " + Netz.mac(k)) : Netz.mac(k) + " → " + ip;
                        })));
        def("7.6", Regel.schritte(
                Schritt.z("Der DHCP-Server ist eingerichtet wie in Aufgabe 7.2", "The DHCP server is set up as in task 7.2", RegelnStandard::dhcpOk), dhcpAn(L1),
                dhcpAn(LEHRERG).frei(),
                // the address comes anew with every start of the simulation; the tests on Laptop-1 do not wait for it
                Schritt.z("Die Simulation neu starten – der Lehrer-Laptop bekommt " + LEHRER, "Restart the simulation – the teacher laptop gets " + LEHRER,
                        n -> LEHRER.equals(Netz.ip(lehrerLaptop(n)))).frei().ist(n -> lehrerLaptop(n) == null ? null : Netz.ip(lehrerLaptop(n))),
                Schritt.n("Laptop-1: im Webbrowser " + WWW_SHOP + " öffnen", "Laptop-1: open " + WWW_SHOP + " in the web browser",
                        (n, e, s) -> http(e, s, laptop(n, 1), SHOP, x -> x.host.startsWith(WWW_SHOP))),
                Schritt.n("Laptop-1: im E-Mail-Programm als Anna die Mails abrufen", "Laptop-1: fetch the mails as Anna in the email program",
                        (n, e, s) -> abruf(e, s, MAIL_SCHULE, "anna", laptop(n, 1)) != null)));
        def("7.7", Regel.schritte(mitEtikett("Im Dokumentationsmodus ein Textfeld mit beiden Grenzen in die Fläche des Schulnetzes legen: „DHCP: " + DHCP_VON + " – " + DHCP_BIS + "“",
                "In the documentation mode, put a text field with both limits into the field of the school network: “DHCP: " + DHCP_VON + " – " + DHCP_BIS + "”",
                RegelnStandard::switchSchule, Regeln.mitAdressen(DHCP_VON, DHCP_BIS))));
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
        def("A1", stoerung(n -> {
            Knoten l2 = laptop(n, 2);
            return l2 != null && inNetz(Netz.ip(l2), SCHULNETZ) && (Netz.dhcp(l2) || ROUTER_SCHULE.equals(Netz.gateway(l2)));
        }, "Laptop-2 öffnet im Webbrowser http://" + WEBSERVER, "Laptop-2 opens http://" + WEBSERVER + " in the web browser",
                (n, e, s) -> http(e, s, laptop(n, 2), WEBSERVER, null)));
        def("A2", stoerung(n -> {
            DHCPServer d = Netz.dhcpServer(dhcpServerPc(n));
            return d != null && ROUTER_SCHULE.equals(d.determineGatewayip());
        }, "die Simulation neu starten, dann pingt ein Schul-Laptop ein Gerät im RGG-Netz an, zum Beispiel `ping " + SHOP + "`",
                "restart the simulation, then a school laptop pings a device in the RGG network, for example `ping " + SHOP + "`",
                (n, e, s) -> e.gibt(PING_ANTWORT, s, x -> schulLaptops(n).contains(x.knoten) && Netz.dhcp(x.knoten) && inNetz(x.ziel, RGGNETZ))));
        def("A3", stoerung(n -> {
            List<String> a = Netz.record(dnsServer(n), WWW_SHOP, "A");
            return !a.isEmpty() && a.stream().allMatch(SHOP::equals);
        }, "der Büro-Laptop öffnet im Webbrowser http://" + WWW_SHOP, "the Office laptop opens http://" + WWW_SHOP + " in the web browser",
                (n, e, s) -> http(e, s, buero(n), SHOP, x -> x.host.startsWith(WWW_SHOP))));
        def("A4", stoerung(n -> Netz.laeuft(shop(n), Netz.WEBSERVER), "der Büro-Laptop öffnet die Shop-Seite im Webbrowser",
                "the Office laptop opens the shop page in the web browser", (n, e, s) -> http(e, s, buero(n), SHOP, null)));
        def("A5", stoerung(n -> Netz.record(dnsServer(n), DOMAIN_RGG, "MX").contains("mail." + DOMAIN_RGG),
                "Anna schickt eine neue Mail an verkauf@" + DOMAIN_RGG, "Anna sends a new mail to verkauf@" + DOMAIN_RGG,
                (n, e, s) -> smtp(e, s, MAIL_RGG, MAIL_SCHULE, null, "verkauf@" + DOMAIN_RGG)));
        def("A6", stoerung(n -> Netz.record(dnsServer(n), WWW_SCHULE, "A").contains(WEBSERVER), "der Büro-Laptop öffnet im Webbrowser http://" + WWW_SCHULE,
                "the Office laptop opens http://" + WWW_SCHULE + " in the web browser", (n, e, s) -> http(e, s, buero(n), WEBSERVER, x -> x.host.startsWith(WWW_SCHULE))));
        def("A7", stoerung(RegelnStandard::rggGatewayOk, "der Büro-Laptop pingt den Webserver der Schule an: `ping " + WEBSERVER + "`",
                "the Office laptop pings the web server of the school: `ping " + WEBSERVER + "`", (n, e, s) -> pingOk(e, s, buero(n), WEBSERVER)));
        def("A8", stoerung(n -> {
            Anwendung a = Netz.software(mailSchule(n), Netz.EMAILSERVER);
            return a instanceof EmailServer && ((EmailServer) a).isAktiv();
        }, "jemand aus der Schule ruft seine Mails ab, zum Beispiel Anna auf Laptop-1", "someone at the school fetches their mails, for example Anna on Laptop-1",
                (n, e, s) -> e.gibt(IMAP, s, x -> x.ziel.equals(MAIL_SCHULE) && x.typ.equals("FETCH"))));
        def("A9", stoerung(n -> amSwitch(n, laptop(n, 3), switchSchule(n)), "Laptop-3 pingt sein Gateway an: `ping " + ROUTER_SCHULE + "`",
                "Laptop-3 pings its gateway: `ping " + ROUTER_SCHULE + "`", (n, e, s) -> pingOk(e, s, laptop(n, 3), ROUTER_SCHULE)));

        // part B: the network plan
        def("B1", Regel.schritte(
                mitEtikett("Schulnetz: Gliederungsfläche mit einem Etikett oben links, das Name und Netzadresse nennt",
                        "School network: structure field with a label at the top left that names the network and its address", RegelnStandard::switchSchule,
                        Regeln.mitAdressen(SCHULNETZ)),
                mitEtikett("RGG-Netz: Gliederungsfläche mit einem Etikett oben links, das Name und Netzadresse nennt",
                        "RGG network: structure field with a label at the top left that names the network and its address", RegelnStandard::switchRgg,
                        Regeln.mitAdressen(RGGNETZ)),
                mitEtikett("Netz des DNS-Anbieters: Gliederungsfläche mit einem Etikett oben links, das Name und Netzadresse nennt",
                        "Network of the DNS provider: structure field with a label at the top left that names the network and its address", RegelnStandard::switchDns,
                        Regeln.mitAdressen(DNSNETZ))));
        def("B2", Regel.schritte(Schritt.z("Im Dokumentationsmodus direkt neben den Router ein Textfeld mit seinen drei Adressen legen: " + ROUTER_SCHULE + ", " + ROUTER_RGG + " und " + ROUTER_DNS,
                "In the documentation mode, put a text field with its three addresses right next to the router: " + ROUTER_SCHULE + ", " + ROUTER_RGG + " and " + ROUTER_DNS,
                RegelnStandard::routerBeschriftet)));
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
