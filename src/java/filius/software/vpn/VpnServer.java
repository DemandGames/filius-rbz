/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.software.vpn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.InternetKnoten;
import filius.modern.Texts;
import filius.modern.tls.Tls;
import filius.software.Anwendung;
import filius.software.transportschicht.Segment;
import filius.software.transportschicht.SocketSchnittstelle;
import filius.software.transportschicht.TcpSegment;
import filius.software.transportschicht.TransportProtokoll;
import filius.software.vermittlungsschicht.IP;
import filius.software.vermittlungsschicht.IpPaket;

/**
 * VPN server (Filius RBZ Wirtschaft Kiel Version): waits at UDP port 1194 for VPN clients. A client logs in with
 * name and password and gets an address from the VPN network (10.8.0.2, ...). Packets from the tunnel are sent on
 * in the shared network with the address of the server (NAT), so the computers there answer the server, and the
 * server sends the answers back through the tunnel.
 */
public class VpnServer extends Anwendung {
    private static final Logger LOG = LoggerFactory.getLogger(VpnServer.class);

    // settings (saved in the project)
    private String vpnNetz = Vpn.NETZ_STANDARD;
    private String freigabe = "";
    private String benutzer = "";
    private boolean aktiv = false;

    private volatile VpnPort port;
    private Tls.Identitaet identitaet;

    /** a logged in client */
    static final class Sitzung {
        final String ip;
        final int port;
        final SecretKey schluessel;
        final String name;
        final String vpnAdresse;

        Sitzung(String ip, int port, SecretKey schluessel, String name, String vpnAdresse) {
            this.ip = ip;
            this.port = port;
            this.schluessel = schluessel;
            this.name = name;
            this.vpnAdresse = vpnAdresse;
        }
    }

    /** a connection from the tunnel into the shared network, under a port of the server */
    static final class Weg {
        final int protokoll;
        final Sitzung sitzung;
        final int clientPort;
        final String ziel;
        final int zielPort;
        final int serverPort;

        Weg(int protokoll, Sitzung sitzung, int clientPort, String ziel, int zielPort, int serverPort) {
            this.protokoll = protokoll;
            this.sitzung = sitzung;
            this.clientPort = clientPort;
            this.ziel = ziel;
            this.zielPort = zielPort;
            this.serverPort = serverPort;
        }
    }

    private final Map<String, Sitzung> sitzungen = new ConcurrentHashMap<>();
    private final Map<String, Weg> wegeNachPort = new ConcurrentHashMap<>();
    private final Map<String, Weg> wegeNachVerbindung = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ settings

    public String getVpnNetz() {
        return vpnNetz;
    }

    public void setVpnNetz(String vpnNetz) {
        this.vpnNetz = vpnNetz == null ? Vpn.NETZ_STANDARD : vpnNetz.trim();
    }

    /** the network behind the tunnel, e.g. "198.51.100.0/24"; empty = the network of the server */
    public String getFreigabe() {
        return freigabe;
    }

    public void setFreigabe(String freigabe) {
        this.freigabe = freigabe == null ? "" : freigabe.trim();
    }

    /** user accounts, one per line: name:password */
    public String getBenutzer() {
        return benutzer;
    }

    public void setBenutzer(String benutzer) {
        this.benutzer = benutzer == null ? "" : benutzer;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
        if (this.running && this.getSystemSoftware() != null) {
            if (aktiv) {
                oeffnen();
            } else {
                schliessen();
            }
        }
        benachrichtigeBeobachter();
    }

    /** name -> password */
    public Map<String, String> konten() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String z : benutzer.split("\r?\n")) {
            int i = z.indexOf(':');
            if (i > 0 && !z.trim().startsWith("#")) {
                m.put(z.substring(0, i).trim(), z.substring(i + 1).trim());
            }
        }
        return m;
    }

    public void kontoHinzufuegen(String name, String passwort) {
        Map<String, String> m = konten();
        m.put(name.trim(), passwort.trim());
        kontenSpeichern(m);
    }

    public void kontoEntfernen(String name) {
        Map<String, String> m = konten();
        m.remove(name);
        kontenSpeichern(m);
    }

    private void kontenSpeichern(Map<String, String> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!e.getKey().isEmpty()) {
                b.append(e.getKey()).append(':').append(e.getValue()).append('\n');
            }
        }
        this.benutzer = b.toString();
        benachrichtigeBeobachter();
    }

    /** address of the server (first network card) */
    public String eigeneAdresse() {
        try {
            return this.getSystemSoftware().primaryIPAdresse();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** {network, mask} behind the tunnel */
    public String[] freigegebenesNetz() {
        String[] n = Vpn.netzAus(freigabe);
        if (n != null) {
            return n;
        }
        try {
            InternetKnoten k = (InternetKnoten) this.getSystemSoftware().getKnoten();
            NetzwerkInterface nic = k.getNetzwerkInterfaces().get(0);
            return new String[] { Vpn.netz(nic.getIp(), nic.getSubnetzMaske()), nic.getSubnetzMaske() };
        } catch (RuntimeException e) {
            return new String[] { "0.0.0.0", "255.255.255.255" };
        }
    }

    /** "198.51.100.0/24" */
    public String freigabeText() {
        String[] n = freigegebenesNetz();
        return n[0] + "/" + Vpn.praefix(n[1]);
    }

    /** the VPN network (always /24): {network, mask} */
    private String vpnNetzAdresse() {
        String n = IP.ipCheck(vpnNetz);
        return n == null ? Vpn.NETZ_STANDARD : Vpn.netz(n, Vpn.MASKE);
    }

    /** VPN address of the server itself (.1 of the VPN network) */
    public String eigeneVpnAdresse() {
        return IP.inetNtoa(IP.inetAton(vpnNetzAdresse()) + 1);
    }

    /** the logged in clients: name and VPN address */
    public List<String[]> verbundene() {
        List<String[]> l = new ArrayList<>();
        for (Sitzung s : sitzungen.values()) {
            l.add(new String[] { s.name, s.vpnAdresse, s.ip + ":" + s.port });
        }
        return l;
    }

    public boolean laeuft() {
        return port != null;
    }

    /** the certificate of the server (for its own address, signed by the Filius certificate authority) */
    public Tls.Zertifikat zertifikat() {
        return identitaet().zertifikat;
    }

    private synchronized Tls.Identitaet identitaet() {
        String ip = eigeneAdresse();
        identitaet = Tls.identitaet(identitaet, Collections.singletonList(ip == null ? "?" : ip), false);
        return identitaet;
    }

    // ------------------------------------------------------------------ start and stop

    @Override
    public void starten() {
        super.starten();
        if (aktiv) {
            oeffnen();
        }
    }

    @Override
    public void beenden() {
        super.beenden();
        schliessen();
    }

    private synchronized void oeffnen() {
        if (port != null || this.getSystemSoftware() == null) {
            return;
        }
        port = VpnPort.oeffnen(this.getSystemSoftware(), Vpn.PORT, this::empfangen);
        if (port == null) {
            benachrichtigeBeobachter(Texts.format("vpn.log.portBusy", String.valueOf(Vpn.PORT)));
            return;
        }
        identitaet();
        Vpn.serverAn(this.getSystemSoftware(), this);
        benachrichtigeBeobachter(Texts.format("vpn.log.started", String.valueOf(Vpn.PORT), freigabeText()));
    }

    private synchronized void schliessen() {
        if (port == null) {
            return;
        }
        port.schliessen();
        port = null;
        Vpn.serverAus(this.getSystemSoftware(), this);
        for (Sitzung s : new ArrayList<>(sitzungen.values())) {
            wegeFreigeben(s);
        }
        sitzungen.clear();
        benachrichtigeBeobachter(Texts.get("vpn.log.stopped"));
    }

    // ------------------------------------------------------------------ records from clients

    private void empfangen(String ip, int clientPort, String record) {
        String art = Vpn.art(record);
        VpnPort p = port;
        if (art == null || p == null) {
            return;
        }
        String key = ip + ":" + clientPort;
        try {
            if (art.equals("ClientHello")) {
                p.senden(ip, clientPort, Vpn.serverHello(identitaet().zertifikat));
                benachrichtigeBeobachter(Texts.format("vpn.log.hello", key));
            } else if (art.equals("Login")) {
                anmelden(p, ip, clientPort, record);
            } else if (art.equals("Data")) {
                Sitzung s = sitzungen.get(key);
                if (s == null) {
                    p.senden(ip, clientPort, Vpn.reject(Texts.get("vpn.reject.session")));
                    return;
                }
                String text = Tls.entschluesseln(s.schluessel, Vpn.feld(record, 1));
                Vpn.merken(record, new Vpn.Klartext(Vpn.beschreibung(text), text, true, s.vpnAdresse));
                weiterleiten(s, Vpn.dekodieren(text));
            } else if (art.equals("Close")) {
                Sitzung s = sitzungen.remove(key);
                if (s != null) {
                    wegeFreigeben(s);
                    benachrichtigeBeobachter(Texts.format("vpn.log.closed", s.name, s.vpnAdresse));
                }
            }
        } catch (Exception e) {
            LOG.debug("VPN server: record not handled", e);
            benachrichtigeBeobachter(Texts.format("vpn.log.error", key));
        }
    }

    private void anmelden(VpnPort p, String ip, int clientPort, String record) throws Exception {
        String key = ip + ":" + clientPort;
        byte[] k = Vpn.oeffnen(identitaet().privaterSchluessel(), Vpn.feld(record, 1));
        SecretKey sk = new SecretKeySpec(k, "AES");
        String anmeldung = Tls.entschluesseln(sk, Vpn.feld(record, 2));
        String[] np = anmeldung.split("\n", 2);
        String name = np[0].trim();
        String passwort = np.length > 1 ? np[1].trim() : "";
        Vpn.merken(record, new Vpn.Klartext(Texts.format("vpn.login.show", name, passwort), null, true, null));
        String richtig = null;
        for (Map.Entry<String, String> e : konten().entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                richtig = e.getValue();
            }
        }
        if (richtig == null || !richtig.equals(passwort)) {
            p.senden(ip, clientPort, Vpn.reject(Texts.get("vpn.reject.login")));
            benachrichtigeBeobachter(Texts.format("vpn.log.rejected", name, key));
            return;
        }
        Sitzung alt = sitzungen.remove(key);
        if (alt != null) {
            wegeFreigeben(alt);
        }
        String adresse = freieAdresse();
        if (adresse == null) {
            p.senden(ip, clientPort, Vpn.reject(Texts.get("vpn.reject.full")));
            return;
        }
        Sitzung s = new Sitzung(ip, clientPort, sk, name, adresse);
        sitzungen.put(key, s);
        String[] netz = freigegebenesNetz();
        Vpn.Zugang z = new Vpn.Zugang(adresse, netz[0], netz[1], eigeneVpnAdresse());
        String antwort = Vpn.accept(Tls.verschluesseln(sk, z.text()));
        Vpn.merken(antwort, new Vpn.Klartext(z.anzeige(), null, false, adresse));
        p.senden(ip, clientPort, antwort);
        benachrichtigeBeobachter(Texts.format("vpn.log.accepted", name, key, adresse));
    }

    /** the lowest free address of the VPN network (from .2) */
    private String freieAdresse() {
        long basis = IP.inetAton(vpnNetzAdresse());
        for (int i = 2; i < 255; i++) {
            String a = IP.inetNtoa(basis + i);
            boolean frei = true;
            for (Sitzung s : sitzungen.values()) {
                if (s.vpnAdresse.equals(a)) {
                    frei = false;
                }
            }
            if (frei) {
                return a;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ NAT between tunnel and shared network

    private TransportProtokoll transport(int protokoll) {
        return protokoll == 6 ? this.getSystemSoftware().holeTcp() : this.getSystemSoftware().holeUdp();
    }

    /** keeps a port of the server for a connection from the tunnel; what arrives there is handled by zurueck() */
    private static final SocketSchnittstelle PLATZHALTER = new SocketSchnittstelle() {
        @Override
        public void hinzufuegen(String ip, int port, Segment segment) {
            // an answer that does not belong to the connection: dropped
        }

        @Override
        public void schliessen() {
        }

        @Override
        public void beenden() {
        }
    };

    private static int freierPort(TransportProtokoll t) {
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 500; i++) {
            int nr = 1024 + r.nextInt(64000);
            if (!t.isUsed(nr) && t.reservierePort(nr, PLATZHALTER)) {
                return nr;
            }
        }
        return t.reserviereFreienPort();
    }

    /** a packet from the tunnel: sent on in the shared network with the address of the server */
    private void weiterleiten(Sitzung s, IpPaket innen) {
        int proto = innen.getProtocol();
        Segment seg = innen.getSegment();
        if ((proto != 6 && proto != 17) || seg == null || !s.vpnAdresse.equals(innen.getSender())) {
            return;
        }
        String[] netz = freigegebenesNetz();
        if (!Vpn.imNetz(innen.getEmpfaenger(), netz[0], netz[1])) {
            return;
        }
        String verbindung = proto + "|" + s.vpnAdresse + "|" + seg.getQuellPort() + "|" + innen.getEmpfaenger() + "|" + seg.getZielPort();
        Weg w = wegeNachVerbindung.get(verbindung);
        if (w == null) {
            int serverPort = freierPort(transport(proto));
            w = new Weg(proto, s, seg.getQuellPort(), innen.getEmpfaenger(), seg.getZielPort(), serverPort);
            wegeNachVerbindung.put(verbindung, w);
            wegeNachPort.put(proto + "|" + serverPort, w);
            benachrichtigeBeobachter(Texts.format("vpn.log.flow", s.name, s.vpnAdresse, innen.getEmpfaenger() + ":" + seg.getZielPort(),
                    proto == 6 ? "TCP" : "UDP", eigeneAdresse() + ":" + serverPort));
        }
        seg.setQuellPort(w.serverPort);
        int ttl = Math.max(1, innen.getTtl() - 1);
        this.getSystemSoftware().holeIP().senden(innen.getEmpfaenger(), null, proto, ttl, seg);
    }

    /** IP hook: an answer for a client behind the tunnel goes back into the tunnel */
    boolean zurueck(IpPaket paket) {
        int proto = paket.getProtocol();
        Segment seg = paket.getSegment();
        if ((proto != 6 && proto != 17) || seg == null) {
            return false;
        }
        Weg w = wegeNachPort.get(proto + "|" + seg.getZielPort());
        if (w == null || !w.ziel.equals(paket.getSender()) || w.zielPort != seg.getQuellPort()) {
            return false;
        }
        Sitzung s = w.sitzung;
        VpnPort p = port;
        if (p == null || sitzungen.get(s.ip + ":" + s.port) != s) {
            return true;
        }
        IpPaket innen = new IpPaket(proto, -1L);
        innen.setSender(paket.getSender());
        innen.setEmpfaenger(s.vpnAdresse);
        innen.setTtl(Math.max(1, paket.getTtl() - 1));
        innen.setSegment(seg);
        String text = Vpn.kodieren(innen);
        // back to the port of the client
        text = text.replaceFirst("\n(TCP|UDP) (\\d+) > (\\d+)", "\n$1 $2 > " + w.clientPort);
        String record = Vpn.daten(Tls.verschluesseln(s.schluessel, text));
        Vpn.merken(record, new Vpn.Klartext(Vpn.beschreibung(text), text, false, s.vpnAdresse));
        p.senden(s.ip, s.port, record);
        if (seg instanceof TcpSegment && (((TcpSegment) seg).isRst())) {
            wegEntfernen(w);
        }
        return true;
    }

    private void wegEntfernen(Weg w) {
        wegeNachPort.remove(w.protokoll + "|" + w.serverPort);
        wegeNachVerbindung.values().remove(w);
        transport(w.protokoll).gibPortFrei(w.serverPort);
    }

    private void wegeFreigeben(Sitzung s) {
        for (Weg w : new ArrayList<>(wegeNachPort.values())) {
            if (w.sitzung == s) {
                wegEntfernen(w);
            }
        }
    }
}
