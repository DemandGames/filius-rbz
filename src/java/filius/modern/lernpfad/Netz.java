package filius.modern.lernpfad;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import filius.gui.GUIContainer;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.hardware.Kabel;
import filius.hardware.NetzwerkInterface;
import filius.hardware.Port;
import filius.hardware.Verbindung;
import filius.hardware.knoten.Gateway;
import filius.hardware.knoten.Host;
import filius.hardware.knoten.InternetKnoten;
import filius.hardware.knoten.Knoten;
import filius.hardware.knoten.LokalerKnoten;
import filius.hardware.knoten.Notebook;
import filius.hardware.knoten.Rechner;
import filius.hardware.knoten.Switch;
import filius.hardware.knoten.Vermittlungsrechner;
import filius.software.Anwendung;
import filius.software.clientserver.ServerAnwendung;
import filius.software.dhcp.DHCPAddressAssignment;
import filius.software.dhcp.DHCPServer;
import filius.software.dns.DNSServer;
import filius.software.dns.ResourceRecord;
import filius.software.email.EmailServer;
import filius.software.firewall.Firewall;
import filius.software.system.Betriebssystem;
import filius.software.system.Datei;
import filius.software.system.GatewayFirmware;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.system.SwitchFirmware;

/**
 * A read-only view of the network as it is right now: devices, their settings, cables and the
 * installed software. Created fresh for every check, so it always sees the current state.
 */
public final class Netz {

    public static final String WEBSERVER = "filius.software.www.WebServer";
    public static final String WEBBROWSER = "filius.software.www.WebBrowser";
    public static final String TERMINAL = "filius.software.lokal.Terminal";
    public static final String TEXTEDITOR = "filius.software.lokal.TextEditor";
    public static final String DNSSERVER = "filius.software.dns.DNSServer";
    public static final String EMAILSERVER = "filius.software.email.EmailServer";
    public static final String EMAILPROGRAMM = "filius.software.email.EmailAnwendung";
    public static final String VPNSERVER = "filius.software.vpn.VpnServer";
    public static final String VPNCLIENT = "filius.software.vpn.VpnClient";

    private final List<Knoten> knoten = new ArrayList<>();
    private final Map<Port, Knoten> portBesitzer = new IdentityHashMap<>();

    public Netz() {
        List<GUIKnotenItem> items;
        try {
            items = new ArrayList<>(GUIContainer.getGUIContainer().getKnotenItems());
        } catch (RuntimeException e) {
            items = Collections.emptyList();
        }
        for (GUIKnotenItem item : items) {
            Knoten k = item.getKnoten();
            if (k == null) {
                continue;
            }
            knoten.add(k);
            if (k instanceof InternetKnoten) {
                for (NetzwerkInterface nic : ((InternetKnoten) k).getNetzwerkInterfaces()) {
                    if (nic.getPort() != null) {
                        portBesitzer.put(nic.getPort(), k);
                    }
                }
            } else if (k instanceof LokalerKnoten) {
                for (Port p : ((LokalerKnoten) k).getAnschluesse()) {
                    portBesitzer.put(p, k);
                }
            }
        }
    }

    // ------------------------------------------------------------------ devices

    public List<Knoten> knoten() {
        return knoten;
    }

    /** Device by name; spaces, hyphens and case do not matter ("Laptop 1" = "laptop-1"). */
    public Knoten knoten(String name) {
        String n = normal(name);
        for (Knoten k : knoten) {
            if (normal(k.getName()).equals(n)) {
                return k;
            }
        }
        // the same device in the other language (German and English learning path)
        String anders = NAMEN.get(n);
        if (anders != null) {
            for (Knoten k : knoten) {
                if (normal(k.getName()).equals(anders)) {
                    return k;
                }
            }
        }
        return null;
    }

    /** German and English names of the devices of the course, normalised, in both directions. */
    private static final Map<String, String> NAMEN = new HashMap<>();

    static {
        String[][] paare = { { "Switch Schule", "School switch" }, { "R1 Router Schule", "R1 School router" },
                { "Switch Provider", "Provider switch" }, { "R2 Router RGG", "R2 RGG router" }, { "Switch RGG", "RGG switch" },
                { "Büro-Laptop", "Office laptop" }, { "Heimrouter", "Home router" },
                { "Switch Heimnetz", "Home switch" }, { "Heim-Laptop", "Home laptop" }, { "Mailserver Schule", "School mail server" },
                { "Mailserver RGG", "RGG mail server" }, { "Router Lager", "Warehouse router" }, { "Switch Lager", "Warehouse switch" },
                { "DHCP-Server Lager", "Warehouse DHCP server" }, { "Lager-Laptop-1", "Warehouse laptop 1" },
                { "Lager-Laptop-2", "Warehouse laptop 2" }, { "Heim-WLAN", "Home-WiFi" },
                { "VPN-Server", "VPN server" }, { "Homeoffice-Laptop", "Home office laptop" },
                // standard learning path
                { "Switch DNS-Anbieter", "DNS provider switch" }, { "Lehrer-Laptop", "Teacher laptop" } };
        for (String[] p : paare) {
            NAMEN.putIfAbsent(normal(p[0]), normal(p[1]));
            NAMEN.putIfAbsent(normal(p[1]), normal(p[0]));
        }
    }

    /** true if the device has this name, in German or English (spaces, hyphens, case and umlauts do not matter) */
    public static boolean heisst(Knoten k, String name) {
        return k != null && gleich(k.getName(), name);
    }

    /** true if the text is this name, in German or English (spaces, hyphens, case and umlauts do not matter) */
    public static boolean gleich(String text, String name) {
        if (text == null || name == null) {
            return false;
        }
        String ist = normal(text);
        String soll = normal(name);
        return ist.equals(soll) || ist.equals(NAMEN.get(soll));
    }

    /** Device whose name contains the given part (normalised), the first one found. */
    public Knoten knotenMit(String teil) {
        String n = normal(teil);
        for (Knoten k : knoten) {
            if (normal(k.getName()).contains(n)) {
                return k;
            }
        }
        return null;
    }

    public static String normal(String s) {
        return s == null ? "" : s.toLowerCase(Locale.GERMAN).replaceAll("[\\s\\-_]", "").replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
    }

    public List<Host> hosts() {
        List<Host> list = new ArrayList<>();
        for (Knoten k : knoten) {
            if (k instanceof Host) {
                list.add((Host) k);
            }
        }
        return list;
    }

    public List<Knoten> laptops() {
        List<Knoten> list = new ArrayList<>();
        for (Knoten k : knoten) {
            if (k instanceof Notebook) {
                list.add(k);
            }
        }
        return list;
    }

    public List<Knoten> pcs() {
        List<Knoten> list = new ArrayList<>();
        for (Knoten k : knoten) {
            if (k instanceof Rechner) {
                list.add(k);
            }
        }
        return list;
    }

    public List<Knoten> switches() {
        List<Knoten> list = new ArrayList<>();
        for (Knoten k : knoten) {
            if (k instanceof Switch) {
                list.add(k);
            }
        }
        return list;
    }

    public List<Knoten> router() {
        List<Knoten> list = new ArrayList<>();
        for (Knoten k : knoten) {
            if (k instanceof Vermittlungsrechner) {
                list.add(k);
            }
        }
        return list;
    }

    public List<Knoten> heimrouter() {
        List<Knoten> list = new ArrayList<>();
        for (Knoten k : knoten) {
            if (k instanceof Gateway) {
                list.add(k);
            }
        }
        return list;
    }

    /** Device with a network card that has this address. */
    public Knoten mitIp(String ip) {
        for (Knoten k : knoten) {
            if (nic(k, ip) != null) {
                return k;
            }
        }
        return null;
    }

    public static NetzwerkInterface nic(Knoten k, String ip) {
        if (!(k instanceof InternetKnoten) || ip == null) {
            return null;
        }
        for (NetzwerkInterface nic : ((InternetKnoten) k).getNetzwerkInterfaces()) {
            if (ip.trim().equals(nic.getIp())) {
                return nic;
            }
        }
        return null;
    }

    public static List<NetzwerkInterface> nics(Knoten k) {
        if (!(k instanceof InternetKnoten)) {
            return Collections.emptyList();
        }
        return ((InternetKnoten) k).getNetzwerkInterfaces();
    }

    public static InternetKnotenBetriebssystem bs(Knoten k) {
        if (k == null || !(k.getSystemSoftware() instanceof InternetKnotenBetriebssystem)) {
            return null;
        }
        return (InternetKnotenBetriebssystem) k.getSystemSoftware();
    }

    // ------------------------------------------------------------------ host settings

    public static String ip(Knoten k) {
        List<NetzwerkInterface> n = nics(k);
        return n.isEmpty() ? "" : leer(n.get(0).getIp());
    }

    public static String maske(Knoten k) {
        List<NetzwerkInterface> n = nics(k);
        return n.isEmpty() ? "" : leer(n.get(0).getSubnetzMaske());
    }

    public static String gateway(Knoten k) {
        InternetKnotenBetriebssystem bs = bs(k);
        return bs == null ? "" : leer(bs.getStandardGateway());
    }

    public static String dns(Knoten k) {
        InternetKnotenBetriebssystem bs = bs(k);
        return bs == null ? "" : leer(bs.getDNSServer());
    }

    /** true if the network card of the device is set to Wi-Fi ("Wireless (Wi-Fi)") */
    public static boolean wlan(Knoten k) {
        return k != null && k.getSystemSoftware() instanceof Betriebssystem && ((Betriebssystem) k.getSystemSoftware()).wireless();
    }

    /** Wi-Fi name: of a switch its own SSID, of a computer the SSID it is set to; null otherwise */
    public static String ssid(Knoten k) {
        if (k instanceof Switch && k.getSystemSoftware() instanceof SwitchFirmware) {
            return ((SwitchFirmware) k.getSystemSoftware()).getSSID();
        }
        if (k != null && k.getSystemSoftware() instanceof Betriebssystem) {
            return ((Betriebssystem) k.getSystemSoftware()).getSsid();
        }
        return null;
    }

    /** true if the first card of the device is connected by a Wi-Fi connection (dashed line) */
    public static boolean perWlan(Knoten k) {
        List<NetzwerkInterface> n = nics(k);
        if (n.isEmpty() || n.get(0).getPort() == null) {
            return false;
        }
        Verbindung v = n.get(0).getPort().getVerbindung();
        return v instanceof Kabel && ((Kabel) v).getWireless();
    }

    public static boolean dhcp(Knoten k) {
        InternetKnotenBetriebssystem bs = bs(k);
        return bs != null && bs.isDHCPKonfiguration();
    }

    public static String mac(Knoten k) {
        List<NetzwerkInterface> n = nics(k);
        return n.isEmpty() ? "" : leer(n.get(0).getMac()).toUpperCase();
    }

    /** Host with fixed settings ip/mask (no DHCP). */
    public static boolean fest(Knoten k, String ip, String maske) {
        return k != null && !dhcp(k) && ip.equals(ip(k)) && maske.equals(maske(k));
    }

    private static String leer(String s) {
        return s == null ? "" : s.trim();
    }

    // ------------------------------------------------------------------ cables

    /** Device at the other end of the cable of this card, or null. */
    public Knoten gegenueber(Port port) {
        if (port == null) {
            return null;
        }
        Verbindung v = port.getVerbindung();
        if (v == null || v.getAnschluesse() == null) {
            return null;
        }
        for (Port p : v.getAnschluesse()) {
            if (p != port && p != null) {
                return portBesitzer.get(p);
            }
        }
        return null;
    }

    public Knoten gegenueber(NetzwerkInterface nic) {
        return nic == null ? null : gegenueber(nic.getPort());
    }

    /** All devices directly connected to k by a cable. */
    public List<Knoten> nachbarn(Knoten k) {
        List<Knoten> list = new ArrayList<>();
        if (k instanceof InternetKnoten) {
            for (NetzwerkInterface nic : ((InternetKnoten) k).getNetzwerkInterfaces()) {
                Knoten g = gegenueber(nic);
                if (g != null) {
                    list.add(g);
                }
            }
        } else if (k instanceof LokalerKnoten) {
            for (Port p : ((LokalerKnoten) k).getAnschluesse()) {
                Knoten g = gegenueber(p);
                if (g != null) {
                    list.add(g);
                }
            }
        }
        return list;
    }

    public boolean verbunden(Knoten a, Knoten b) {
        return a != null && b != null && nachbarn(a).contains(b);
    }

    /**
     * The local network around a device: everything reachable over cables and switches without
     * passing a router or home router. The routers at the border are included.
     */
    public Set<Knoten> segment(Knoten start) {
        Set<Knoten> seen = new LinkedHashSet<>();
        Deque<Knoten> todo = new ArrayDeque<>();
        todo.add(start);
        seen.add(start);
        while (!todo.isEmpty()) {
            Knoten k = todo.poll();
            if (k != start && !(k instanceof Switch)) {
                continue; // do not walk through hosts and routers
            }
            for (Knoten n : nachbarn(k)) {
                if (seen.add(n)) {
                    todo.add(n);
                }
            }
        }
        return seen;
    }

    /** Segment behind one network card of a router (walks only from that card). */
    public Set<Knoten> segment(NetzwerkInterface nic) {
        Knoten g = gegenueber(nic);
        if (g == null) {
            return Collections.emptySet();
        }
        Set<Knoten> s = g instanceof Switch ? segment(g) : new LinkedHashSet<>(List.of(g));
        return s;
    }

    /** The switch a card is plugged into (directly), or null. */
    public Switch switchAn(NetzwerkInterface nic) {
        Knoten g = gegenueber(nic);
        return g instanceof Switch ? (Switch) g : null;
    }

    public Switch switchAn(Knoten host) {
        List<NetzwerkInterface> n = nics(host);
        return n.isEmpty() ? null : switchAn(n.get(0));
    }

    /** Switch port number (1-based) of the cable that leads to device k, or -1. */
    public int switchAnschluss(Switch sw, Knoten k) {
        List<Port> ports = sw.getAnschluesse();
        for (int i = 0; i < ports.size(); i++) {
            if (gegenueber(ports.get(i)) == k) {
                return i + 1;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ software

    public static Anwendung software(Knoten k, String klasse) {
        InternetKnotenBetriebssystem bs = bs(k);
        return bs == null ? null : bs.holeSoftware(klasse);
    }

    public static boolean hat(Knoten k, String klasse) {
        return software(k, klasse) != null;
    }

    public static boolean laeuft(Knoten k, String klasse) {
        Anwendung a = software(k, klasse);
        if (a instanceof ServerAnwendung) {
            return ((ServerAnwendung) a).isAktiv();
        }
        if (a instanceof EmailServer) {
            return ((EmailServer) a).isAktiv();
        }
        if (a instanceof DNSServer) {
            return ((DNSServer) a).isAktiv();
        }
        if (a instanceof filius.software.vpn.VpnServer) {
            return ((filius.software.vpn.VpnServer) a).isAktiv();
        }
        return false;
    }

    /** Text of a file in the virtual file system, or null. */
    public static String datei(Knoten k, String pfad) {
        InternetKnotenBetriebssystem bs = bs(k);
        if (bs == null) {
            return null;
        }
        Datei d = bs.getDateisystem().holeDatei(bs.getDateisystem().holeRootPfad() + "/" + pfad);
        if (d == null) {
            return null;
        }
        // text files (html, txt) are stored as plain text in Filius
        return d.getDateiInhalt();
    }

    /** Text of the first h1 element of an HTML text without tags, or "". */
    public static String h1(String html) {
        if (html == null) {
            return "";
        }
        String lower = html.toLowerCase(Locale.ROOT);
        int a = lower.indexOf("<h1");
        if (a < 0) {
            return "";
        }
        int b = lower.indexOf('>', a);
        int c = lower.indexOf("</h1", b);
        if (b < 0 || c < 0) {
            return "";
        }
        return entities(html.substring(b + 1, c).replaceAll("<[^>]*>", "")).replaceAll("\\s+", " ").trim();
    }

    /** Text content of an HTML page without tags and with the common entities decoded. */
    public static String text(String html) {
        if (html == null) {
            return "";
        }
        String s = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ").replaceAll("<[^>]*>", " ");
        return entities(s).replaceAll("\\s+", " ").trim();
    }

    private static String entities(String s) {
        return s.replace("&nbsp;", " ").replace("&ndash;", "–").replace("&mdash;", "—").replace("&amp;", "&").replace("&auml;", "ä")
                .replace("&ouml;", "ö").replace("&uuml;", "ü").replace("&Auml;", "Ä").replace("&Ouml;", "Ö").replace("&Uuml;", "Ü")
                .replace("&szlig;", "ß").replace("&middot;", "·").replace("&#8211;", "–");
    }

    // ------------------------------------------------------------------ DNS

    public static String name(String domain) {
        if (domain == null) {
            return "";
        }
        String d = domain.trim().toLowerCase(Locale.ROOT);
        while (d.endsWith(".")) {
            d = d.substring(0, d.length() - 1);
        }
        return d;
    }

    public static List<ResourceRecord> records(Knoten dnsServer) {
        Anwendung a = software(dnsServer, DNSSERVER);
        if (!(a instanceof DNSServer)) {
            return Collections.emptyList();
        }
        try {
            return ((DNSServer) a).holeResourceRecords();
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    /** Value of the record name/type on that server, all matches. */
    public static List<String> record(Knoten dnsServer, String domain, String typ) {
        List<String> werte = new ArrayList<>();
        for (ResourceRecord rr : records(dnsServer)) {
            if (name(rr.getDomainname()).equals(name(domain)) && typ.equalsIgnoreCase(rr.getType())) {
                werte.add(name(rr.getRdata()));
            }
        }
        return werte;
    }

    // ------------------------------------------------------------------ DHCP

    public static DHCPServer dhcpServer(Knoten k) {
        InternetKnotenBetriebssystem bs = bs(k);
        return bs == null ? null : bs.getDHCPServer();
    }

    public static boolean dhcpAktiv(Knoten k) {
        DHCPServer d = dhcpServer(k);
        return d != null && d.isAktiv();
    }

    public static String statischeZuweisung(Knoten server, String mac) {
        DHCPServer d = dhcpServer(server);
        if (d == null || mac == null) {
            return null;
        }
        for (DHCPAddressAssignment a : d.holeStaticAssignedAddresses()) {
            if (mac.equalsIgnoreCase(a.getMAC())) {
                return a.getIp();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ routing

    /** Manual routes of a router as {target, mask, gateway, interface}. */
    public static List<String[]> routen(Knoten router) {
        InternetKnotenBetriebssystem bs = bs(router);
        if (bs == null || bs.getWeiterleitungstabelle() == null) {
            return Collections.emptyList();
        }
        List<String[]> r = bs.getWeiterleitungstabelle().getManuelleTabelle();
        return r == null ? Collections.emptyList() : r;
    }

    /**
     * Whether the router has a usable manual route into the network ziel/maske via gateway
     * (gateway null = any). A default route 0.0.0.0/0 also counts.
     */
    public static boolean route(Knoten router, String ziel, String maske, String gateway) {
        for (String[] r : routen(router)) {
            if (r == null || r.length < 3) {
                continue;
            }
            String rz = leer(r[0]);
            String rm = leer(r[1]);
            String rg = leer(r[2]);
            boolean passt = gueltig(rz) && gueltig(rm) && gueltig(ziel) && gueltig(maske)
                    && (gleichesNetz(ziel, rz, rm) && wert(rm) <= wert(maske) || "0.0.0.0".equals(rm));
            if (passt && (gateway == null || gateway.equals(rg))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ firewall

    public static Firewall firewall(Knoten heimrouter) {
        InternetKnotenBetriebssystem bs = bs(heimrouter);
        if (bs instanceof GatewayFirmware) {
            return ((GatewayFirmware) bs).holeFirewall();
        }
        if (bs instanceof filius.software.system.VermittlungsrechnerBetriebssystem) {
            return ((filius.software.system.VermittlungsrechnerBetriebssystem) bs).holeFirewall();
        }
        return null;
    }

    /** Static port forwardings of a home router: {protocol, wanPort, lanIp, lanPort}. */
    public static List<String[]> portfreigaben(Knoten heimrouter) {
        InternetKnotenBetriebssystem bs = bs(heimrouter);
        if (bs instanceof GatewayFirmware) {
            List<String[]> l = ((GatewayFirmware) bs).getStaticNAT();
            return l == null ? Collections.emptyList() : l;
        }
        return Collections.emptyList();
    }

    // ------------------------------------------------------------------ addresses

    public static boolean gueltig(String ip) {
        if (ip == null) {
            return false;
        }
        String[] t = ip.trim().split("\\.");
        if (t.length != 4) {
            return false;
        }
        for (String s : t) {
            try {
                int v = Integer.parseInt(s);
                if (v < 0 || v > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static long wert(String ip) {
        if (!gueltig(ip)) {
            return -1;
        }
        long v = 0;
        for (String s : ip.trim().split("\\.")) {
            v = v * 256 + Integer.parseInt(s);
        }
        return v;
    }

    public static boolean gleichesNetz(String a, String b, String maske) {
        if (!gueltig(a) || !gueltig(b) || !gueltig(maske)) {
            return false;
        }
        long m = wert(maske);
        return (wert(a) & m) == (wert(b) & m);
    }

    public static String netzadresse(String ip, String maske) {
        if (!gueltig(ip) || !gueltig(maske)) {
            return "";
        }
        long v = wert(ip) & wert(maske);
        return ((v >> 24) & 255) + "." + ((v >> 16) & 255) + "." + ((v >> 8) & 255) + "." + (v & 255);
    }

    public static boolean imBereich(String ip, String von, String bis) {
        long v = wert(ip);
        return v >= 0 && v >= wert(von) && v <= wert(bis);
    }

    /** All IP addresses found in a text, in order. */
    public static List<String> adressen(String text) {
        List<String> list = new ArrayList<>();
        if (text == null) {
            return list;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b\\d{1,3}(\\.\\d{1,3}){3}\\b").matcher(text);
        while (m.find()) {
            if (gueltig(m.group())) {
                list.add(m.group());
            }
        }
        return list;
    }

    /** Counts devices per address, to find duplicates. */
    public Map<String, Integer> adressZaehlung() {
        Map<String, Integer> m = new HashMap<>();
        for (Knoten k : knoten) {
            for (NetzwerkInterface nic : nics(k)) {
                if (gueltig(nic.getIp()) && !"0.0.0.0".equals(nic.getIp())) {
                    m.merge(nic.getIp(), 1, Integer::sum);
                }
            }
        }
        return m;
    }
}
