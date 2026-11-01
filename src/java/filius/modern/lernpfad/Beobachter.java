package filius.modern.lernpfad;

import java.awt.Window;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import filius.gui.anwendungssicht.NatViewer;
import filius.gui.anwendungssicht.SatViewer;
import filius.hardware.NetzwerkInterface;
import filius.hardware.Verbindung;
import filius.gui.GUIContainer;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.hardware.knoten.Gateway;
import filius.hardware.knoten.InternetKnoten;
import filius.hardware.knoten.Knoten;
import filius.rahmenprogramm.nachrichten.Lauscher;
import filius.software.dns.DNSNachricht;
import filius.software.dns.Query;
import filius.software.dns.ResourceRecord;
import filius.software.nat.NatGateway;
import filius.software.netzzugangsschicht.EthernetFrame;
import filius.software.system.GatewayFirmware;
import filius.software.system.SystemSoftware;
import filius.software.transportschicht.Segment;
import filius.software.transportschicht.TcpSegment;
import filius.software.vermittlungsschicht.IcmpPaket;
import filius.software.vermittlungsschicht.IpPaket;

/**
 * Turns what happens in the simulation into {@link Ereignis events}. It reads the frames that
 * Filius records for its data exchange windows (the {@link Lauscher}), so nothing in the
 * simulation itself changes. Each network card only reports frames it sent or received itself.
 */
public final class Beobachter {

    private final Ereignisse ereignisse;

    /** per network card: the list object last seen and how many entries were already read */
    private final Map<String, Object> listen = new HashMap<>();
    private final Map<String, Integer> gelesen = new HashMap<>();
    private Field datenEinheitenFeld;

    private final List<Offen> offenePings = new ArrayList<>();
    private final List<Offen> offeneSyns = new ArrayList<>();
    private final Map<String, Ereignis> httpVorgaenge = new HashMap<>();
    private final Map<String, Ereignis> httpAntworten = new HashMap<>();
    private final Map<String, Sitzung> sitzungen = new HashMap<>();
    private final Map<String, Long> fensterZuletzt = new HashMap<>();

    public Beobachter(Ereignisse ereignisse) {
        this.ereignisse = ereignisse;
    }

    // ------------------------------------------------------------------ polling

    /** Reads new frames and checks timeouts and open windows. Called on the event thread. */
    public void tick(boolean simulationLaeuft) {
        long jetzt = System.currentTimeMillis();
        if (simulationLaeuft) {
            leseLauscher();
            pruefeFenster(jetzt);
        }
        pruefeZeitueberschreitungen(jetzt, simulationLaeuft);
    }

    @SuppressWarnings("unchecked")
    private void leseLauscher() {
        Lauscher lauscher = Lauscher.getLauscher();
        Map<String, LinkedList<Object[]>> daten;
        try {
            if (datenEinheitenFeld == null) {
                datenEinheitenFeld = Lauscher.class.getDeclaredField("datenEinheiten");
                datenEinheitenFeld.setAccessible(true);
            }
            daten = (Map<String, LinkedList<Object[]>>) datenEinheitenFeld.get(lauscher);
        } catch (Exception e) {
            return;
        }
        List<String> ids;
        synchronized (daten) {
            ids = new ArrayList<>(daten.keySet());
        }
        Map<String, Object[]> karten = null;
        for (String id : ids) {
            LinkedList<Object[]> liste = daten.get(id);
            if (liste == null) {
                continue;
            }
            List<Object[]> neu = new ArrayList<>();
            synchronized (liste) {
                int start = gelesen.getOrDefault(id, 0);
                if (listen.get(id) != liste || start > liste.size()) {
                    start = 0; // the recording was reset
                }
                int i = 0;
                for (Object[] eintrag : liste) {
                    if (i++ >= start) {
                        neu.add(eintrag);
                    }
                }
                listen.put(id, liste);
                gelesen.put(id, liste.size());
            }
            if (neu.isEmpty()) {
                continue;
            }
            // The recorder keeps the first computer it saw for a MAC address, also after another
            // project with the same addresses was loaded (e.g. a start setup). So the cards are
            // looked up in the current network.
            if (karten == null) {
                karten = karten(lauscher);
            }
            Object[] karte = karten.get(id);
            if (karte == null) {
                continue;
            }
            Knoten knoten = (Knoten) karte[0];
            NetzwerkInterface nic = (NetzwerkInterface) karte[1];
            for (Object[] eintrag : neu) {
                try {
                    verarbeite(knoten, nic, (Long) eintrag[0], (EthernetFrame) eintrag[1]);
                } catch (RuntimeException e) {
                    // a frame that cannot be read is simply skipped
                }
            }
        }
    }

    /**
     * MAC address -> {node, card} of the current network. Also corrects the recorder's own table, which
     * other views of Filius use (it is only ever filled, never updated).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object[]> karten(Lauscher lauscher) {
        Map<String, Object[]> m = new HashMap<>();
        for (GUIKnotenItem item : GUIContainer.getGUIContainer().getKnotenItems()) {
            Knoten k = item.getKnoten();
            if (k instanceof InternetKnoten) {
                for (NetzwerkInterface nic : ((InternetKnoten) k).getNetzwerkInterfaces()) {
                    m.put(nic.getMac(), new Object[] { k, nic });
                }
            }
        }
        try {
            Field fi = Lauscher.class.getDeclaredField("InterfaceById");
            Field fs = Lauscher.class.getDeclaredField("SystemSoftwareById");
            fi.setAccessible(true);
            fs.setAccessible(true);
            Map<String, NetzwerkInterface> ni = (Map<String, NetzwerkInterface>) fi.get(lauscher);
            Map<String, SystemSoftware> ns = (Map<String, SystemSoftware>) fs.get(lauscher);
            for (Map.Entry<String, Object[]> e : m.entrySet()) {
                Knoten k = (Knoten) e.getValue()[0];
                // only existing keys: values are replaced, the tables do not change their structure
                // while the simulation threads use them
                if (ni.containsKey(e.getKey()) && ns.containsKey(e.getKey())
                        && (ni.get(e.getKey()) != e.getValue()[1] || ns.get(e.getKey()) != k.getSystemSoftware())) {
                    ni.put(e.getKey(), (NetzwerkInterface) e.getValue()[1]);
                    ns.put(e.getKey(), k.getSystemSoftware());
                }
            }
        } catch (Exception | LinkageError e) {
            // the own table above is enough for the learning path
        }
        return m;
    }

    // ------------------------------------------------------------------ frames

    private void verarbeite(Knoten k, NetzwerkInterface nic, long zeit, EthernetFrame frame) {
        if (!(frame.getDaten() instanceof IpPaket)) {
            return;
        }
        verarbeitePaket(k, nic.getIp(), zeit, (IpPaket) frame.getDaten());
    }

    /** an IP packet sent or received by the computer k with the address ich (a network card or its VPN address) */
    private void verarbeitePaket(Knoten k, String ich, long zeit, IpPaket ip) {
        String q = ip.getSender();
        String z = ip.getEmpfaenger();
        if (ip instanceof IcmpPaket) {
            icmp(k, ich, zeit, (IcmpPaket) ip);
            return;
        }
        Segment s = ip.getSegment();
        if (s == null) {
            return;
        }
        String daten = s.getDaten() == null ? "" : s.getDaten();
        if (ip.getProtocol() == 17 && filius.software.vpn.Vpn.istRecord(daten)) {
            vpn(k, ich, zeit, q, z, s, daten);
            return;
        }
        if (ip.getProtocol() == 17) {
            udp(k, ich, zeit, q, z, s, daten);
            return;
        }
        if (ip.getProtocol() != 6 || !(s instanceof TcpSegment)) {
            return;
        }
        boolean gesendet = q.equals(ich);
        boolean empfangen = z.equals(ich);
        if (!gesendet && !empfangen) {
            return;
        }
        TcpSegment t = (TcpSegment) s;
        int qp = t.getQuellPort();
        int zp = t.getZielPort();
        if (t.isSyn() && !t.isAck() && gesendet) {
            String key = schluessel(k, q, qp, z, zp);
            offeneSyns.removeIf(o -> o.schluessel.equals(key));
            Offen o = new Offen(key, k, q, z, zeit);
            o.port = zp;
            offeneSyns.add(o);
        } else if (t.isSyn() && t.isAck() && empfangen) {
            String key = schluessel(k, z, zp, q, qp);
            for (Iterator<Offen> it = offeneSyns.iterator(); it.hasNext();) {
                Offen o = it.next();
                if (o.schluessel.equals(key)) {
                    it.remove();
                    Ereignis e = new Ereignis(Ereignis.Art.TCP_VERBUNDEN, k, zeit);
                    e.quelle = z;
                    e.ziel = q;
                    e.port = qp;
                    ereignisse.add(e);
                }
            }
        } else if (t.isRst() && empfangen) {
            String key = schluessel(k, z, zp, q, qp);
            for (Iterator<Offen> it = offeneSyns.iterator(); it.hasNext();) {
                Offen o = it.next();
                if (o.schluessel.equals(key)) {
                    it.remove();
                    synOhneAntwort(o, zeit);
                }
            }
        }
        if (daten.isEmpty()) {
            return;
        }
        if (zp == 25 || qp == 25) {
            smtp(k, ich, zeit, q, qp, z, zp, daten);
        } else if (zp == 143 || qp == 143) {
            imap(k, ich, zeit, q, qp, z, zp, daten);
        } else if (zp == filius.modern.tls.Tls.PORT || qp == filius.modern.tls.Tls.PORT) {
            tls(k, gesendet, zeit, q, qp, z, zp, daten);
        } else {
            http(k, gesendet, zeit, q, qp, z, zp, daten);
        }
    }

    private void icmp(Knoten k, String ich, long zeit, IcmpPaket p) {
        String q = p.getSender();
        String z = p.getEmpfaenger();
        if (p.isEchoRequest() && q.equals(ich)) {
            if (z.endsWith(".255")) {
                return;
            }
            Offen o = new Offen(k.hashCode() + "|" + q + "|" + z + "|" + p.getSeqNr(), k, q, z, zeit);
            o.seq = p.getSeqNr();
            offenePings.add(o);
        } else if (p.isEchoRequest() && z.equals(ich)) {
            Ereignis e = new Ereignis(Ereignis.Art.PING_EMPFANGEN, k, zeit);
            e.quelle = q;
            e.ziel = z;
            ereignisse.add(e);
        } else if (p.isEchoResponse() && z.equals(ich)) {
            for (Iterator<Offen> it = offenePings.iterator(); it.hasNext();) {
                Offen o = it.next();
                if (o.knoten == k && o.ziel.equals(q) && o.seq == p.getSeqNr()) {
                    it.remove();
                }
            }
            Ereignis e = new Ereignis(Ereignis.Art.PING_ANTWORT, k, zeit);
            e.quelle = z;
            e.ziel = q;
            ereignisse.add(e);
        } else if (p.getIcmpType() == 11 && z.equals(ich)) {
            offenePings.removeIf(o -> o.knoten == k && o.seq == p.getSeqNr());
            Ereignis e = new Ereignis(Ereignis.Art.ZEIT_ABGELAUFEN, k, zeit);
            e.quelle = q;
            e.ziel = z;
            ereignisse.add(e);
        } else if (p.getIcmpType() == 3 && z.equals(ich)) {
            for (Iterator<Offen> it = offenePings.iterator(); it.hasNext();) {
                Offen o = it.next();
                if (o.knoten == k && o.seq == p.getSeqNr()) {
                    it.remove();
                    pingOhneAntwort(o, zeit);
                }
            }
        }
    }

    private void udp(Knoten k, String ich, long zeit, String q, String z, Segment s, String daten) {
        if (daten.contains("ID=") && daten.contains("QR=") && daten.contains("RCODE=")) {
            DNSNachricht n;
            try {
                n = new DNSNachricht(daten);
            } catch (RuntimeException e) {
                return;
            }
            if (n.getQueryResponse() == 1 && z.equals(ich)) {
                Ereignis e = new Ereignis(Ereignis.Art.DNS, k, zeit);
                e.quelle = z;
                e.ziel = q;
                e.port = s.getQuellPort();
                LinkedList<Query> fragen = n.holeQueries();
                if (fragen != null && !fragen.isEmpty()) {
                    e.name = Netz.name(fragen.getFirst().holeDomainname());
                    e.typ = String.valueOf(fragen.getFirst().holeTyp()).toUpperCase();
                }
                for (ResourceRecord rr : n.holeAntwortResourceRecords()) {
                    e.werte.add(Netz.name(rr.getDomainname()) + " " + String.valueOf(rr.getType()).toUpperCase() + " " + Netz.name(rr.getRdata()));
                }
                ereignisse.add(e);
            } else if (n.getQueryResponse() == 0 && q.equals(ich)) {
                Ereignis e = new Ereignis(Ereignis.Art.DNS_ANFRAGE, k, zeit);
                e.quelle = q;
                e.ziel = z;
                e.port = s.getZielPort();
                e.typ = "UDP";
                LinkedList<Query> fragen = n.holeQueries();
                if (fragen != null && !fragen.isEmpty()) {
                    e.name = Netz.name(fragen.getFirst().holeDomainname());
                }
                ereignisse.add(e);
            }
        } else if (daten.startsWith("DHCP")) {
            // DHCPTYPE\nkey=value\n... (filius.software.dhcp.DHCPMessage)
            String[] t = daten.split("[\n=]");
            Map<String, String> felder = new HashMap<>();
            for (int i = 1; i + 1 < t.length; i += 2) {
                felder.put(t[i].trim(), t[i + 1].trim());
            }
            Ereignis e = new Ereignis(Ereignis.Art.DHCP, k, zeit);
            e.quelle = q;
            e.ziel = z;
            e.name = t[0].trim().toUpperCase();
            e.werte.add(felder.getOrDefault("chaddr", "").toUpperCase());
            e.werte.add(felder.getOrDefault("yiaddr", ""));
            ereignisse.add(e);
        }
    }

    /**
     * VPN: handshake records become VPN events; at the client end of a tunnel the packets inside the tunnel are
     * handled as if the computer had sent or received them with its VPN address.
     */
    private void vpn(Knoten k, String ich, long zeit, String q, String z, Segment s, String daten) {
        boolean gesendet = q.equals(ich);
        boolean empfangen = z.equals(ich);
        if (!gesendet && !empfangen) {
            return;
        }
        String art = filius.software.vpn.Vpn.art(daten);
        filius.software.vpn.Vpn.Klartext klar = filius.software.vpn.Vpn.klartext(daten);
        if (!"Data".equals(art)) {
            Ereignis e = new Ereignis(Ereignis.Art.VPN, k, zeit);
            e.quelle = q;
            e.ziel = z;
            e.port = filius.software.vpn.Vpn.PORT;
            e.name = art;
            e.typ = gesendet ? "gesendet" : "empfangen";
            if (klar != null && klar.vpnAdresse != null) {
                e.werte.add(klar.vpnAdresse);
            }
            ereignisse.add(e);
            return;
        }
        if (klar == null || klar.paket == null || klar.vpnAdresse == null) {
            return;
        }
        if (gesendet && klar.vonClient || empfangen && !klar.vonClient) {
            try {
                verarbeitePaket(k, klar.vpnAdresse, zeit, filius.software.vpn.Vpn.paket(klar.paket));
            } catch (RuntimeException ex) {
                // a packet that cannot be read is skipped
            }
        }
    }

    /** HTTPS: handshake records become TLS events, the decrypted HTTP is handled like HTTP (with port 443) */
    private void tls(Knoten k, boolean gesendet, long zeit, String q, int qp, String z, int zp, String daten) {
        String art = filius.modern.tls.Tls.art(daten);
        if (art != null && !art.startsWith("Application Data")) {
            Ereignis e = new Ereignis(Ereignis.Art.TLS, k, zeit);
            e.quelle = q;
            e.ziel = z;
            e.port = zp;
            e.name = art.split("[ +]")[0];
            e.typ = gesendet ? "gesendet" : "empfangen";
            if (art.startsWith("ServerHello")) {
                String[] zeilen = daten.split("\n");
                if (zeilen.length > 3 && zeilen[3].contains(": ")) {
                    for (String n : zeilen[3].substring(zeilen[3].indexOf(": ") + 2).split(",")) {
                        e.werte.add(n.trim().toLowerCase());
                    }
                }
            }
            ereignisse.add(e);
            return;
        }
        filius.modern.tls.Tls.Klartext klar = filius.modern.tls.Tls.klartext(daten);
        if (klar == null || klar.teil != 1) {
            return;
        }
        http(k, gesendet, zeit, q, qp, z, zp, klar.text);
    }

    private void http(Knoten k, boolean gesendet, long zeit, String q, int qp, String z, int zp, String daten) {
        if (daten.startsWith("GET ") || daten.startsWith("POST ")) {
            String[] zeilen = daten.split("\r?\n");
            String[] teile = zeilen[0].split(" ");
            String pfad = teile.length > 1 ? teile[1] : "/";
            String host = "";
            for (String zeile : zeilen) {
                if (zeile.regionMatches(true, 0, "Host:", 0, 5)) {
                    host = zeile.substring(5).trim().toLowerCase();
                }
            }
            if (gesendet) {
                Ereignis e = new Ereignis(Ereignis.Art.HTTP, k, zeit);
                e.quelle = q;
                e.ziel = z;
                e.port = zp;
                e.pfad = pfad;
                e.host = host;
                httpVorgaenge.put(schluessel(k, q, qp, z, zp), e);
            } else {
                Ereignis e = new Ereignis(Ereignis.Art.HTTP_ANFRAGE, k, zeit);
                e.quelle = q;
                e.ziel = z;
                e.port = zp;
                e.pfad = pfad;
                e.host = host;
                ereignisse.add(e);
            }
            return;
        }
        if (gesendet) {
            return;
        }
        String key = schluessel(k, z, zp, q, qp);
        if (daten.startsWith("HTTP/")) {
            Ereignis e = httpVorgaenge.remove(key);
            if (e == null) {
                return;
            }
            String[] teile = daten.split("[ \r\n]", 3);
            try {
                e.status = Integer.parseInt(teile[1].trim());
            } catch (RuntimeException ex) {
                e.status = 0;
            }
            int leer = daten.indexOf("\n\n");
            int leer2 = daten.indexOf("\r\n\r\n");
            int start = leer2 >= 0 ? leer2 + 4 : leer >= 0 ? leer + 2 : daten.length();
            e.inhalt.append(daten.substring(Math.min(start, daten.length())));
            e.zeit = zeit;
            httpAntworten.put(key, e);
            ereignisse.add(e);
        } else {
            Ereignis e = httpAntworten.get(key);
            if (e != null && e.inhalt.length() < 400_000) {
                e.inhalt.append(daten);
            }
        }
    }

    private void smtp(Knoten k, String ich, long zeit, String q, int qp, String z, int zp, String daten) {
        boolean anServer = zp == 25;
        String client = anServer ? q : z;
        String server = anServer ? z : q;
        if (!server.equals(ich)) {
            return; // only the server side reports, so each mail is counted once
        }
        String key = "smtp|" + schluessel(k, client, anServer ? qp : zp, server, 25);
        Sitzung s = sitzungen.computeIfAbsent(key, x -> new Sitzung());
        if (anServer) {
            String d = daten.trim();
            if (s.zustand == 2) {
                return; // mail text
            }
            if (d.regionMatches(true, 0, "MAIL FROM", 0, 9)) {
                s.von = adresse(d);
                s.an.clear();
            } else if (d.regionMatches(true, 0, "RCPT TO", 0, 7)) {
                s.an.add(adresse(d));
            } else if (d.equalsIgnoreCase("DATA")) {
                s.zustand = 1;
            }
        } else {
            if (s.zustand == 1 && daten.startsWith("354")) {
                s.zustand = 2;
            } else if (s.zustand == 2 && daten.startsWith("250")) {
                Ereignis e = new Ereignis(Ereignis.Art.SMTP, k, zeit);
                e.quelle = client;
                e.ziel = server;
                e.port = 25;
                e.werte.add(s.von);
                e.werte.addAll(s.an);
                ereignisse.add(e);
                s.zustand = 0;
                s.an.clear();
            } else if (s.zustand == 1) {
                s.zustand = 0;
            }
        }
    }

    private static final java.util.regex.Pattern IMAP_BEFEHL = java.util.regex.Pattern.compile("^(\\S+)\\s+(?:UID\\s+)?(\\S+)\\s*(.*)$");
    private static final java.util.regex.Pattern IMAP_FETCH = java.util.regex.Pattern.compile("(?m)^\\* \\d+ FETCH \\(");

    /** IMAP (port 143), seen at the server: LOGIN and the FETCH that hands out mails */
    private void imap(Knoten k, String ich, long zeit, String q, int qp, String z, int zp, String daten) {
        boolean anServer = zp == 143;
        String client = anServer ? q : z;
        String server = anServer ? z : q;
        if (!server.equals(ich)) {
            return;
        }
        String key = "imap|" + schluessel(k, client, anServer ? qp : zp, server, 143);
        Sitzung s = sitzungen.computeIfAbsent(key, x -> new Sitzung());
        String d = daten.trim();
        if (anServer) {
            java.util.regex.Matcher m = IMAP_BEFEHL.matcher(d.split("\n", 2)[0]);
            s.zustand = 0;
            if (m.matches()) {
                String befehl = m.group(2).toUpperCase(java.util.Locale.ROOT);
                if (befehl.equals("LOGIN")) {
                    String[] a = m.group(3).trim().replace("\"", "").split("\\s+");
                    s.von = a.length > 0 ? a[0].toLowerCase() : "";
                    s.zustand = 4;
                } else if (befehl.equals("FETCH")) {
                    s.zustand = 3;
                } else if (befehl.equals("EXPUNGE")) {
                    s.zustand = 5;
                }
            }
        } else if (s.zustand == 4) {
            if (d.contains(" OK LOGIN")) {
                ereignisse.add(imapEreignis(k, zeit, client, server, s.von, "LOGIN", 0));
            }
            s.zustand = 0;
        } else if (s.zustand == 5) {
            int anzahl = 0;
            for (String zeile : d.split("\n")) {
                if (zeile.matches("\\* \\d+ EXPUNGE.*")) {
                    anzahl++;
                }
            }
            if (anzahl > 0) {
                ereignisse.add(imapEreignis(k, zeit, client, server, s.von, "EXPUNGE", anzahl));
            }
            s.zustand = 0;
        } else if (s.zustand == 3) {
            java.util.regex.Matcher m = IMAP_FETCH.matcher(d);
            int anzahl = 0;
            while (m.find()) {
                anzahl++;
            }
            if (anzahl > 0 && d.contains("BODY[]")) {
                ereignisse.add(imapEreignis(k, zeit, client, server, s.von, "FETCH", anzahl));
            }
            s.zustand = 0;
        }
    }

    private static Ereignis imapEreignis(Knoten k, long zeit, String client, String server, String konto, String typ, int anzahl) {
        Ereignis e = new Ereignis(Ereignis.Art.IMAP, k, zeit);
        e.quelle = client;
        e.ziel = server;
        e.port = anzahl;
        e.name = konto == null ? "" : konto;
        e.typ = typ;
        return e;
    }

    private static String adresse(String zeile) {
        int a = zeile.indexOf('<');
        int b = zeile.indexOf('>');
        String s = a >= 0 && b > a ? zeile.substring(a + 1, b) : zeile.substring(zeile.indexOf(':') + 1);
        return s.trim().toLowerCase();
    }

    // ------------------------------------------------------------------ timeouts and windows

    private void pruefeZeitueberschreitungen(long jetzt, boolean simulationLaeuft) {
        if (!simulationLaeuft) {
            // stopping the simulation is no failed ping
            offenePings.clear();
            offeneSyns.clear();
            httpVorgaenge.clear();
            return;
        }
        long grenze = Math.max(9000L, 4L * Verbindung.holeRTT());
        for (Iterator<Offen> it = offenePings.iterator(); it.hasNext();) {
            Offen o = it.next();
            if (jetzt - o.zeit > grenze) {
                it.remove();
                pingOhneAntwort(o, o.zeit);
            }
        }
        for (Iterator<Offen> it = offeneSyns.iterator(); it.hasNext();) {
            Offen o = it.next();
            if (jetzt - o.zeit > grenze) {
                it.remove();
                synOhneAntwort(o, o.zeit);
            }
        }
        httpVorgaenge.values().removeIf(e -> jetzt - e.zeit > 10L * grenze);
    }

    private void pingOhneAntwort(Offen o, long zeit) {
        Ereignis e = new Ereignis(Ereignis.Art.PING_OHNE_ANTWORT, o.knoten, zeit);
        e.quelle = o.quelle;
        e.ziel = o.ziel;
        ereignisse.add(e);
    }

    private void synOhneAntwort(Offen o, long zeit) {
        Ereignis e = new Ereignis(Ereignis.Art.SYN_OHNE_ANTWORT, o.knoten, zeit);
        e.quelle = o.quelle;
        e.ziel = o.ziel;
        e.port = o.port;
        ereignisse.add(e);
    }

    private void pruefeFenster(long jetzt) {
        for (Window w : Window.getWindows()) {
            if (!w.isShowing()) {
                continue;
            }
            if (w instanceof SatViewer) {
                fenster(Ereignis.Art.SAT, ((SatViewer) w).getSwitch(), jetzt);
            } else if (w instanceof NatViewer) {
                Gateway gw = ((NatViewer) w).getGateway();
                if (natEintraege(gw) > 0) {
                    fenster(Ereignis.Art.NAT, gw, jetzt);
                }
            }
        }
    }

    static int natEintraege(Gateway gw) {
        try {
            GatewayFirmware fw = (GatewayFirmware) gw.getSystemSoftware();
            return ((NatGateway) fw.holeFirewall()).getNATTable().getDynamicNATTable().size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** An open window counts as one event that is refreshed while it stays open. */
    void fenster(Ereignis.Art art, Knoten k, long jetzt) {
        if (k == null) {
            return;
        }
        String key = art + "|" + System.identityHashCode(k);
        Long zuletzt = fensterZuletzt.get(key);
        if (zuletzt == null || jetzt - zuletzt > 2000) {
            ereignisse.add(new Ereignis(art, k, jetzt));
        } else {
            ereignisse.auffrischen(art, k, jetzt);
        }
        fensterZuletzt.put(key, jetzt);
    }

    private static String schluessel(Knoten k, String a, int ap, String b, int bp) {
        return System.identityHashCode(k) + "|" + a + ":" + ap + "|" + b + ":" + bp;
    }

    // ------------------------------------------------------------------ helper types

    private static final class Offen {
        final String schluessel;
        final Knoten knoten;
        final String quelle;
        final String ziel;
        final long zeit;
        int seq;
        int port;

        Offen(String schluessel, Knoten knoten, String quelle, String ziel, long zeit) {
            this.schluessel = schluessel;
            this.knoten = knoten;
            this.quelle = quelle;
            this.ziel = ziel;
            this.zeit = zeit;
        }
    }

    private static final class Sitzung {
        int zustand;
        String von = "";
        final List<String> an = new ArrayList<>();
    }
}
