/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.software.vpn;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import filius.modern.Texts;
import filius.modern.tls.Tls;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.Segment;
import filius.software.transportschicht.TcpSegment;
import filius.software.transportschicht.UdpSegment;
import filius.software.vermittlungsschicht.IP;
import filius.software.vermittlungsschicht.IpPaket;
import filius.software.vermittlungsschicht.VermittlungsProtokoll;

/**
 * VPN for the simulated network: a VPN client (e.g. a laptop in the home office) builds an encrypted tunnel to a
 * VPN server in a company network. Packets of the client for the network behind the tunnel are packed - encrypted -
 * into UDP datagrams to port 1194 of the server. The server unpacks them and sends them on in its own network with
 * its own address (NAT), and the answers go back through the tunnel. On the way only UDP to port 1194 with
 * unreadable data can be seen.
 *
 * <pre>
 * Client                                   Server
 * VPN ClientHello              ------&gt;
 *                              &lt;------   VPN ServerHello + certificate (public key of the server)
 * VPN Login                    ------&gt;    session key locked with the public key, name and password locked
 *                                         with the session key
 *                              &lt;------   VPN Accept (VPN address, network behind the tunnel) or VPN Reject
 * VPN Data                     &lt;-----&gt;   a whole IP packet, locked with the session key
 * VPN Close                    ------&gt;
 * </pre>
 *
 * Like TLS the handing over of the session key is asymmetric and everything afterwards symmetric. The encryption is
 * real (RSA and AES from Java), the records are text so that they can be read in the data exchange window.
 */
public final class Vpn {

    private Vpn() {}

    /** UDP port of the VPN server (as OpenVPN) */
    public static final int PORT = 1194;
    /** VPN addresses: the server gets .1, the clients .2, .3, ... */
    public static final String NETZ_STANDARD = "10.8.0.0";
    public static final String MASKE = "255.255.255.0";

    static final String CLIENT_HELLO = "VPN ClientHello";
    static final String SERVER_HELLO = "VPN ServerHello";
    static final String LOGIN = "VPN Login";
    static final String ACCEPT = "VPN Accept";
    static final String REJECT = "VPN Reject";
    static final String DATA = "VPN Data";
    static final String CLOSE = "VPN Close";

    private static final SecureRandom RANDOM = new SecureRandom();

    // ------------------------------------------------------------------ records

    /** true if the payload of a datagram is a VPN record */
    public static boolean istRecord(String payload) {
        return payload != null && payload.startsWith("VPN ");
    }

    /** type of a record: "ClientHello", "ServerHello", "Login", "Accept", "Reject", "Data" or "Close" */
    public static String art(String payload) {
        if (!istRecord(payload)) {
            return null;
        }
        String a = payload.split("\n", 2)[0].substring(4).trim();
        int plus = a.indexOf(" +");
        return plus > 0 ? a.substring(0, plus) : a;
    }

    static String wert(String zeile) {
        int i = zeile.indexOf(": ");
        return i < 0 ? zeile.trim() : zeile.substring(i + 2).trim();
    }

    /** value of line nr (1 = the line after the record name) */
    static String feld(String record, int nr) {
        String[] z = record.split("\n");
        return z.length > nr ? wert(z[nr]) : "";
    }

    static String clientHello() {
        return CLIENT_HELLO + "\n" + Texts.get("tls.f.methods") + ": RSA, AES-128\n" + Texts.get("tls.f.random") + ": " + Tls.zufall();
    }

    static String serverHello(Tls.Zertifikat z) {
        return SERVER_HELLO + " + " + Texts.get("tls.certificate") + "\n" + Texts.get("tls.f.for") + ": " + String.join(", ", z.namen) + "\n"
                + Texts.get("tls.f.issuer") + ": " + z.aussteller + "\n" + Texts.get("tls.f.valid") + ": " + z.gueltigBis + "\n"
                + Texts.get("tls.f.key") + ": " + z.schluessel + "\n" + Texts.get("tls.f.signature") + ": " + z.unterschrift;
    }

    static Tls.Zertifikat zertifikatAus(String serverHello) {
        String[] z = serverHello.split("\n");
        if (z.length < 6) {
            return null;
        }
        List<String> namen = new ArrayList<>();
        for (String n : wert(z[1]).split(",")) {
            if (!n.trim().isEmpty()) {
                namen.add(n.trim());
            }
        }
        return new Tls.Zertifikat(namen, wert(z[2]), wert(z[3]), wert(z[4]), wert(z[5]));
    }

    static String login(String verschlossenerSchluessel, String verschluesselteAnmeldung) {
        return LOGIN + "\n" + Texts.get("vpn.f.sessionkey") + ": " + verschlossenerSchluessel + "\n" + Texts.get("vpn.f.login") + ": "
                + verschluesselteAnmeldung;
    }

    static String accept(String verschluesselterZugang) {
        return ACCEPT + "\n" + Texts.get("vpn.f.access") + ": " + verschluesselterZugang;
    }

    static String reject(String grund) {
        return REJECT + "\n" + Texts.get("tls.f.reason") + ": " + grund;
    }

    static String daten(String geheimtext) {
        return DATA + "\n" + Texts.get("tls.f.data") + ": " + geheimtext;
    }

    static String close(String grund) {
        return CLOSE + "\n" + Texts.get("tls.f.reason") + ": " + grund;
    }

    // ------------------------------------------------------------------ keys

    static SecretKey neuerSchluessel() {
        byte[] k = new byte[16];
        RANDOM.nextBytes(k);
        return new SecretKeySpec(k, "AES");
    }

    static String verschliessen(PublicKey oeffentlich, byte[] daten) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, oeffentlich);
        return Base64.getEncoder().encodeToString(c.doFinal(daten));
    }

    static byte[] oeffnen(PrivateKey privat, String verschlossen) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.DECRYPT_MODE, privat);
        return c.doFinal(Base64.getDecoder().decode(verschlossen.trim()));
    }

    // ------------------------------------------------------------------ access data (VPN Accept)

    /** what the server tells a client after the login: its VPN address and the network behind the tunnel */
    static final class Zugang {
        final String adresse;
        final String netz;
        final String netzMaske;
        final String server;

        Zugang(String adresse, String netz, String netzMaske, String server) {
            this.adresse = adresse;
            this.netz = netz;
            this.netzMaske = netzMaske;
            this.server = server;
        }

        String text() {
            return "address=" + adresse + "\nnetmask=" + MASKE + "\nnetwork=" + netz + "\nnetworkmask=" + netzMaske + "\nserver=" + server;
        }

        static Zugang aus(String text) {
            Map<String, String> m = new HashMap<>();
            for (String z : text.split("\n")) {
                int i = z.indexOf('=');
                if (i > 0) {
                    m.put(z.substring(0, i).trim(), z.substring(i + 1).trim());
                }
            }
            if (!m.containsKey("address") || !m.containsKey("network")) {
                return null;
            }
            return new Zugang(m.get("address"), m.get("network"), m.getOrDefault("networkmask", MASKE), m.getOrDefault("server", ""));
        }

        /** for the data exchange window */
        String anzeige() {
            return Texts.get("vpn.access.address") + ": " + adresse + " / " + praefix(MASKE) + "\n" + Texts.get("vpn.access.network") + ": "
                    + netz + "/" + praefix(netzMaske) + "\n" + Texts.get("vpn.access.server") + ": " + server;
        }
    }

    // ------------------------------------------------------------------ the IP packet inside the tunnel

    /** A whole IP packet as text (header lines, then the data), the plain text of a VPN Data record. */
    static String kodieren(IpPaket p) {
        StringBuilder b = new StringBuilder();
        b.append("IP ").append(p.getSender()).append(" > ").append(p.getEmpfaenger()).append(" ttl=").append(p.getTtl()).append(" proto=")
                .append(p.getProtocol());
        Segment s = p.getSegment();
        if (s instanceof TcpSegment) {
            TcpSegment t = (TcpSegment) s;
            b.append("\nTCP ").append(t.getQuellPort()).append(" > ").append(t.getZielPort()).append(" seq=").append(t.getSeqNummer())
                    .append(" ack=").append(t.getAckNummer()).append(" flags=").append(flags(t)).append(" win=").append(t.getWindow())
                    .append(" off=").append(t.getDataOffset()).append(" urgp=").append(t.getUrgentPointer()).append(" sum=")
                    .append(t.getPruefSumme());
        } else {
            b.append("\nUDP ").append(s.getQuellPort()).append(" > ").append(s.getZielPort()).append(" sum=").append(s.getPruefSumme());
            if (s instanceof UdpSegment) {
                b.append(" len=").append(((UdpSegment) s).getLaenge());
            }
        }
        if (s.getDaten() == null) {
            b.append("\n-");
        } else {
            b.append("\n+\n").append(s.getDaten());
        }
        return b.toString();
    }

    private static String flags(TcpSegment t) {
        StringBuilder f = new StringBuilder();
        if (t.isUrg()) {
            f.append('U');
        }
        if (t.isAck()) {
            f.append('A');
        }
        if (t.isPush()) {
            f.append('P');
        }
        if (t.isRst()) {
            f.append('R');
        }
        if (t.isSyn()) {
            f.append('S');
        }
        if (t.isFin()) {
            f.append('F');
        }
        return f.length() == 0 ? "-" : f.toString();
    }

    private static Map<String, String> werte(String[] teile, int ab) {
        Map<String, String> m = new HashMap<>();
        for (int i = ab; i < teile.length; i++) {
            int g = teile[i].indexOf('=');
            if (g > 0) {
                m.put(teile[i].substring(0, g), teile[i].substring(g + 1));
            }
        }
        return m;
    }

    private static int zahl(String s, int standard) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return standard;
        }
    }

    private static long gross(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** the IP packet from its text (see kodieren) */
    static IpPaket dekodieren(String text) {
        String[] t = text.split("\n", 4);
        String[] ip = t[0].split(" ");
        Map<String, String> iw = werte(ip, 4);
        int proto = zahl(iw.getOrDefault("proto", "6"), 6);
        IpPaket p = new IpPaket(proto, -1L);
        p.setSender(ip[1]);
        p.setEmpfaenger(ip[3]);
        p.setTtl(zahl(iw.getOrDefault("ttl", "64"), 64));
        String[] sg = t[1].split(" ");
        Map<String, String> sw = werte(sg, 4);
        Segment s;
        if ("TCP".equals(sg[0])) {
            TcpSegment ts = new TcpSegment();
            ts.setSeqNummer(gross(sw.getOrDefault("seq", "0")));
            ts.setAckNummer(gross(sw.getOrDefault("ack", "0")));
            String f = sw.getOrDefault("flags", "-");
            ts.setUrg(f.indexOf('U') >= 0);
            ts.setAck(f.indexOf('A') >= 0);
            ts.setPush(f.indexOf('P') >= 0);
            ts.setRst(f.indexOf('R') >= 0);
            ts.setSyn(f.indexOf('S') >= 0);
            ts.setFin(f.indexOf('F') >= 0);
            ts.setWindow(zahl(sw.getOrDefault("win", "0"), 0));
            ts.setDataOffset(zahl(sw.getOrDefault("off", "0"), 0));
            ts.setUrgentPointer(zahl(sw.getOrDefault("urgp", "0"), 0));
            s = ts;
        } else {
            UdpSegment us = new UdpSegment();
            us.setLaenge(zahl(sw.getOrDefault("len", "0"), 0));
            s = us;
        }
        s.setQuellPort(zahl(sg[1], 0));
        s.setZielPort(zahl(sg[3], 0));
        s.setPruefSumme(zahl(sw.getOrDefault("sum", "0"), 0));
        if (t.length > 2 && t[2].equals("+")) {
            s.setDaten(t.length > 3 ? t[3] : "");
        }
        p.setSegment(s);
        return p;
    }

    /** the IP packet of a decrypted VPN Data record (see Klartext.paket) */
    public static IpPaket paket(String text) {
        return dekodieren(text);
    }

    /** the inner packet in words, for the data exchange window */
    public static String beschreibung(String paketText) {
        try {
            IpPaket p = dekodieren(paketText);
            Segment s = p.getSegment();
            StringBuilder b = new StringBuilder();
            b.append(Texts.format("vpn.inner.ip", p.getSender(), p.getEmpfaenger()));
            if (s instanceof TcpSegment) {
                TcpSegment t = (TcpSegment) s;
                List<String> f = new ArrayList<>();
                if (t.isSyn()) {
                    f.add("SYN");
                }
                if (t.isFin()) {
                    f.add("FIN");
                }
                if (t.isRst()) {
                    f.add("RST");
                }
                if (t.isAck()) {
                    f.add("ACK");
                }
                b.append("\n").append(Texts.format("vpn.inner.port", "TCP", String.valueOf(s.getQuellPort()), String.valueOf(s.getZielPort())));
                if (!f.isEmpty()) {
                    b.append(" (").append(String.join(", ", f)).append(")");
                }
            } else {
                b.append("\n").append(Texts.format("vpn.inner.port", "UDP", String.valueOf(s.getQuellPort()), String.valueOf(s.getZielPort())));
            }
            if (s.getDaten() != null && !s.getDaten().isEmpty()) {
                b.append("\n\n").append(s.getDaten());
            }
            return b.toString();
        } catch (RuntimeException e) {
            return paketText;
        }
    }

    // ------------------------------------------------------------------ plain text of the records

    /** What an encrypted record contains; only the client and the server know it (they have the session key). */
    public static final class Klartext {
        /** the content in words (shown at the two ends of the tunnel) */
        public final String anzeige;
        /** the inner IP packet as text (VPN Data), otherwise null */
        public final String paket;
        /** true if the client sent the record */
        public final boolean vonClient;
        /** VPN address of the client of this tunnel */
        public final String vpnAdresse;

        Klartext(String anzeige, String paket, boolean vonClient, String vpnAdresse) {
            this.anzeige = anzeige;
            this.paket = paket;
            this.vonClient = vonClient;
            this.vpnAdresse = vpnAdresse;
        }
    }

    private static final Map<String, Klartext> ARCHIV = Collections.synchronizedMap(new LinkedHashMap<String, Klartext>(256, 0.75f, false) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Klartext> eldest) {
            return size() > 20000;
        }
    });

    static void merken(String record, Klartext k) {
        ARCHIV.putIfAbsent(record, k);
    }

    /** plain text of the record with this payload, null if it is not an encrypted VPN record */
    public static Klartext klartext(String payload) {
        return payload == null ? null : ARCHIV.get(payload);
    }

    // ------------------------------------------------------------------ addresses

    public static String netz(String ip, String maske) {
        long a = IP.inetAton(ip);
        long m = IP.inetAton(maske);
        return a < 0 || m < 0 ? null : IP.inetNtoa(a & m);
    }

    public static int praefix(String maske) {
        long m = IP.inetAton(maske);
        return m < 0 ? 0 : Long.bitCount(m);
    }

    public static String maske(int praefix) {
        long m = praefix <= 0 ? 0 : (0xFFFFFFFFL << (32 - Math.min(32, praefix))) & 0xFFFFFFFFL;
        return IP.inetNtoa(m);
    }

    /** "198.51.100.0/24" or "198.51.100.0 255.255.255.0" -> {network, mask}, null if it is no network */
    public static String[] netzAus(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        String ip;
        String maske;
        if (t.contains("/")) {
            ip = t.substring(0, t.indexOf('/')).trim();
            int p;
            try {
                p = Integer.parseInt(t.substring(t.indexOf('/') + 1).trim());
            } catch (RuntimeException e) {
                return null;
            }
            if (p < 0 || p > 32) {
                return null;
            }
            maske = maske(p);
        } else if (t.contains(" ")) {
            ip = t.substring(0, t.indexOf(' ')).trim();
            maske = IP.ipCheck(t.substring(t.indexOf(' ') + 1).trim());
        } else {
            return null;
        }
        if (IP.ipCheck(ip) == null || maske == null) {
            return null;
        }
        return new String[] { netz(ip, maske), maske };
    }

    static boolean imNetz(String ip, String netz, String maske) {
        return ip != null && netz != null && maske != null && VermittlungsProtokoll.gleichesRechnernetz(ip, netz, maske);
    }

    // ------------------------------------------------------------------ clients and servers of the simulation

    private static final Map<InternetKnotenBetriebssystem, VpnClient> CLIENTS = new WeakHashMap<>();
    private static final Map<InternetKnotenBetriebssystem, VpnServer> SERVER = new WeakHashMap<>();
    /** quick check for the IP hooks: is there a tunnel at all? */
    private static volatile int anzahl;

    static synchronized void clientAn(InternetKnotenBetriebssystem bs, VpnClient c) {
        CLIENTS.put(bs, c);
        anzahl = CLIENTS.size() + SERVER.size();
    }

    static synchronized void clientAus(InternetKnotenBetriebssystem bs, VpnClient c) {
        if (CLIENTS.get(bs) == c) {
            CLIENTS.remove(bs);
        }
        anzahl = CLIENTS.size() + SERVER.size();
    }

    static synchronized void serverAn(InternetKnotenBetriebssystem bs, VpnServer s) {
        SERVER.put(bs, s);
        anzahl = CLIENTS.size() + SERVER.size();
    }

    static synchronized void serverAus(InternetKnotenBetriebssystem bs, VpnServer s) {
        if (SERVER.get(bs) == s) {
            SERVER.remove(bs);
        }
        anzahl = CLIENTS.size() + SERVER.size();
    }

    /** the connected VPN client of this computer, null if there is none */
    public static synchronized VpnClient client(InternetKnotenBetriebssystem bs) {
        return bs == null ? null : CLIENTS.get(bs);
    }

    /** the running VPN server of this computer, null if there is none */
    public static synchronized VpnServer server(InternetKnotenBetriebssystem bs) {
        return bs == null ? null : SERVER.get(bs);
    }

    /** IP hook (client): true if the packet went into the tunnel */
    public static boolean ausgehend(InternetKnotenBetriebssystem bs, IpPaket paket) {
        if (anzahl == 0 || bs == null) {
            return false;
        }
        VpnClient c = client(bs);
        return c != null && c.tunneln(paket);
    }

    /** IP hook (server): true if the packet was an answer for the tunnel and went back into it */
    public static boolean eingehend(InternetKnotenBetriebssystem bs, IpPaket paket) {
        if (anzahl == 0 || bs == null) {
            return false;
        }
        VpnServer s = server(bs);
        return s != null && s.zurueck(paket);
    }

    // ------------------------------------------------------------------ command line

    /** additional lines for ipconfig: the VPN adapter of a connected client */
    public static String ipconfig(InternetKnotenBetriebssystem bs) {
        VpnClient c = client(bs);
        if (c == null || !c.istVerbunden()) {
            return "";
        }
        return "\n" + Texts.format("vpn.ipconfig.adapter", c.serverAdresse()) + "\n" + filius.rahmenprogramm.I18n.messages.getString("sw_terminal_msg4")
                + " " + c.vpnAdresse() + "\n" + filius.rahmenprogramm.I18n.messages.getString("sw_terminal_msg5") + " " + MASKE + "\n"
                + Texts.format("vpn.ipconfig.network", c.tunnelNetz()) + "\n";
    }

    /** additional line for route: the network in the tunnel goes to the VPN address of the server */
    public static String route(InternetKnotenBetriebssystem bs, java.util.function.BiFunction<Integer, String, String> fuellen) {
        VpnClient c = client(bs);
        if (c == null || !c.istVerbunden() || c.tunnelNetz() == null) {
            return "";
        }
        String netz = c.tunnelNetz().substring(0, c.tunnelNetz().indexOf('/'));
        String maske = maske(zahl(c.tunnelNetz().substring(c.tunnelNetz().indexOf('/') + 1), 24));
        String[] eintrag = { netz, maske, c.serverVpnAdresse(), c.vpnAdresse() };
        StringBuilder b = new StringBuilder("| ");
        for (String e : eintrag) {
            b.append(e).append(fuellen.apply(15 - e.length(), " ")).append(" | ");
        }
        return b.append("(VPN)\n").toString();
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
