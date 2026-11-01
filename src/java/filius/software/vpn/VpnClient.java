/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.software.vpn;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.hardware.Verbindung;
import filius.hardware.knoten.InternetKnoten;
import filius.modern.Texts;
import filius.modern.tls.Tls;
import filius.software.Anwendung;
import filius.software.vermittlungsschicht.IP;
import filius.software.vermittlungsschicht.IpPaket;

/**
 * VPN client (Filius RBZ Wirtschaft Kiel Version): connects to a VPN server (UDP port 1194) with name and
 * password. While connected, packets of this computer for the network behind the tunnel go encrypted to the
 * server (see the hook in IP), and the answers come back the same way. The computer then has a second address,
 * its VPN address.
 */
public class VpnClient extends Anwendung {
    private static final Logger LOG = LoggerFactory.getLogger(VpnClient.class);

    public enum Zustand {
        GETRENNT, VERBINDET, VERBUNDEN, FEHLER
    }

    // settings (saved in the project)
    private String server = "";
    private String benutzer = "";
    private String passwort = "";

    private volatile Zustand zustand = Zustand.GETRENNT;
    private volatile String meldung = "";
    private volatile VpnPort port;
    private volatile String serverAdresse;
    private volatile SecretKey schluessel;
    private volatile Vpn.Zugang zugang;
    private volatile Tls.Zertifikat zertifikat;
    private final LinkedBlockingQueue<String> antworten = new LinkedBlockingQueue<>();

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server == null ? "" : server.trim();
    }

    public String getBenutzer() {
        return benutzer;
    }

    public void setBenutzer(String benutzer) {
        this.benutzer = benutzer == null ? "" : benutzer.trim();
    }

    public String getPasswort() {
        return passwort;
    }

    public void setPasswort(String passwort) {
        this.passwort = passwort == null ? "" : passwort;
    }

    public Zustand zustand() {
        return zustand;
    }

    /** the last status message in words */
    public String meldung() {
        return meldung;
    }

    public boolean istVerbunden() {
        return zustand == Zustand.VERBUNDEN;
    }

    /** own VPN address, null if not connected */
    public String vpnAdresse() {
        Vpn.Zugang z = zugang;
        return istVerbunden() && z != null ? z.adresse : null;
    }

    /** network behind the tunnel, e.g. "198.51.100.0/24", null if not connected */
    public String tunnelNetz() {
        Vpn.Zugang z = zugang;
        return istVerbunden() && z != null ? z.netz + "/" + Vpn.praefix(z.netzMaske) : null;
    }

    /** address of the VPN server of the tunnel, null if not connected */
    public String serverAdresse() {
        return istVerbunden() ? serverAdresse : null;
    }

    /** VPN address of the server, null if not connected */
    public String serverVpnAdresse() {
        Vpn.Zugang z = zugang;
        return istVerbunden() && z != null ? z.server : null;
    }

    public Tls.Zertifikat zertifikat() {
        return zertifikat;
    }

    // ------------------------------------------------------------------ connect and disconnect

    /** connects in the thread of the application (the window does not wait) */
    public void verbinden() {
        // a running tunnel is closed first, so the server frees its VPN address
        VpnPort p = port;
        if (zustand == Zustand.VERBUNDEN && p != null && serverAdresse != null) {
            p.senden(serverAdresse, Vpn.PORT, Vpn.close(Texts.get("vpn.close.user")));
        }
        // the window shows at once that something happens
        this.zustand = Zustand.VERBINDET;
        this.meldung = "";
        benachrichtigeBeobachter();
        this.ausfuehren("verbindenJetzt", null);
    }

    public void trennen() {
        this.ausfuehren("trennenJetzt", null);
    }

    private void status(Zustand z, String text) {
        this.zustand = z;
        this.meldung = text;
        benachrichtigeBeobachter(text);
    }

    private long wartezeit() {
        return Math.max(3000L, 8L * Verbindung.holeRTT());
    }

    /** the next answer of the server of the given type(s), null after the waiting time */
    private String warte(String... arten) throws InterruptedException {
        long ende = System.currentTimeMillis() + wartezeit();
        while (true) {
            long rest = ende - System.currentTimeMillis();
            if (rest <= 0) {
                return null;
            }
            String a = antworten.poll(rest, TimeUnit.MILLISECONDS);
            if (a == null) {
                return null;
            }
            String art = Vpn.art(a);
            for (String x : arten) {
                if (x.equals(art)) {
                    return a;
                }
            }
        }
    }

    void verbindenJetzt() {
        if (zustand == Zustand.VERBUNDEN || zustand == Zustand.VERBINDET) {
            aufraeumen();
        }
        String ip = IP.ipCheck(server);
        if (ip == null) {
            status(Zustand.FEHLER, Texts.get("vpn.err.address"));
            return;
        }
        if (benutzer.isEmpty()) {
            status(Zustand.FEHLER, Texts.get("vpn.err.user"));
            return;
        }
        antworten.clear();
        serverAdresse = ip;
        port = VpnPort.oeffnenFrei(this.getSystemSoftware(), this::empfangen);
        if (port == null) {
            status(Zustand.FEHLER, Texts.get("vpn.err.port"));
            return;
        }
        status(Zustand.VERBINDET, Texts.format("vpn.c.hello", ip, String.valueOf(Vpn.PORT)));
        try {
            port.senden(ip, Vpn.PORT, Vpn.clientHello());
            String hallo = warte("ServerHello");
            if (hallo == null) {
                fehler(Texts.format("vpn.err.noAnswer", ip, String.valueOf(Vpn.PORT)));
                return;
            }
            Tls.Zertifikat z = Vpn.zertifikatAus(hallo);
            if (z == null) {
                fehler(Texts.get("vpn.err.protocol"));
                return;
            }
            zertifikat = z;
            if (!z.vonZertifizierungsstelle() || !z.gilt(ip)) {
                port.senden(ip, Vpn.PORT, Vpn.close(Texts.get("tls.alert.certificate")));
                fehler(Texts.format("vpn.err.cert", z.namenText(), z.aussteller));
                return;
            }
            status(Zustand.VERBINDET, Texts.format("vpn.c.cert", z.aussteller));
            SecretKey sk = Vpn.neuerSchluessel();
            String anmeldung = Tls.verschluesseln(sk, benutzer + "\n" + passwort);
            String login = Vpn.login(Vpn.verschliessen(z.oeffentlicherSchluessel(), sk.getEncoded()), anmeldung);
            Vpn.merken(login, new Vpn.Klartext(Texts.format("vpn.login.show", benutzer, passwort), null, true, null));
            port.senden(ip, Vpn.PORT, login);
            status(Zustand.VERBINDET, Texts.format("vpn.c.login", benutzer));
            String antwort = warte("Accept", "Reject");
            if (antwort == null) {
                fehler(Texts.format("vpn.err.noAnswer", ip, String.valueOf(Vpn.PORT)));
                return;
            }
            if ("Reject".equals(Vpn.art(antwort))) {
                fehler(Texts.format("vpn.err.rejected", Vpn.feld(antwort, 1)));
                return;
            }
            Vpn.Zugang zg = Vpn.Zugang.aus(Tls.entschluesseln(sk, Vpn.feld(antwort, 1)));
            if (zg == null) {
                fehler(Texts.get("vpn.err.protocol"));
                return;
            }
            Vpn.merken(antwort, new Vpn.Klartext(zg.anzeige(), null, false, zg.adresse));
            schluessel = sk;
            zugang = zg;
            zustand = Zustand.VERBUNDEN;
            Vpn.clientAn(this.getSystemSoftware(), this);
            status(Zustand.VERBUNDEN, Texts.format("vpn.c.connected", zg.adresse, zg.netz + "/" + Vpn.praefix(zg.netzMaske)));
        } catch (InterruptedException e) {
            aufraeumen();
            status(Zustand.GETRENNT, Texts.get("vpn.c.disconnected"));
        } catch (Exception e) {
            LOG.debug("VPN client: connection failed", e);
            fehler(Texts.get("vpn.err.protocol"));
        }
    }

    private void fehler(String text) {
        aufraeumen();
        status(Zustand.FEHLER, text);
    }

    void trennenJetzt() {
        VpnPort p = port;
        if (p != null && zustand == Zustand.VERBUNDEN && serverAdresse != null) {
            p.senden(serverAdresse, Vpn.PORT, Vpn.close(Texts.get("vpn.close.user")));
        }
        aufraeumen();
        status(Zustand.GETRENNT, Texts.get("vpn.c.disconnected"));
    }

    private void aufraeumen() {
        Vpn.clientAus(this.getSystemSoftware(), this);
        VpnPort p = port;
        port = null;
        if (p != null) {
            p.schliessen();
        }
        schluessel = null;
        zugang = null;
        if (zustand == Zustand.VERBUNDEN || zustand == Zustand.VERBINDET) {
            zustand = Zustand.GETRENNT;
        }
    }

    @Override
    public void beenden() {
        aufraeumen();
        zustand = Zustand.GETRENNT;
        meldung = "";
        super.beenden();
    }

    // ------------------------------------------------------------------ tunnel

    /** a datagram from the server (on the thread of the port) */
    private void empfangen(String ip, int vonPort, String record) {
        if (!ip.equals(serverAdresse) || vonPort != Vpn.PORT) {
            return;
        }
        String art = Vpn.art(record);
        if (art == null) {
            return;
        }
        if (zustand == Zustand.VERBUNDEN && art.equals("Data")) {
            SecretKey sk = schluessel;
            Vpn.Zugang z = zugang;
            if (sk == null || z == null) {
                return;
            }
            try {
                String text = Tls.entschluesseln(sk, Vpn.feld(record, 1));
                Vpn.merken(record, new Vpn.Klartext(Vpn.beschreibung(text), text, false, z.adresse));
                IpPaket p = Vpn.dekodieren(text);
                if (!z.adresse.equals(p.getEmpfaenger())) {
                    return;
                }
                try {
                    p.setRcvNic(((InternetKnoten) this.getSystemSoftware().getKnoten()).getNetzwerkInterfaces().get(0));
                } catch (RuntimeException e) {
                    // no card: the packet is delivered anyway
                }
                ((IP) this.getSystemSoftware().holeIP()).zustellen(p);
            } catch (Exception e) {
                LOG.debug("VPN client: data not readable", e);
            }
        } else if (zustand == Zustand.VERBUNDEN && (art.equals("Reject") || art.equals("Close"))) {
            aufraeumen();
            status(Zustand.FEHLER, Texts.format("vpn.err.lost", Vpn.feld(record, 1)));
        } else {
            antworten.offer(record);
        }
    }

    /** IP hook: a packet of this computer for the network behind the tunnel goes into the tunnel */
    boolean tunneln(IpPaket paket) {
        Vpn.Zugang z = zugang;
        SecretKey sk = schluessel;
        VpnPort p = port;
        if (zustand != Zustand.VERBUNDEN || z == null || sk == null || p == null || paket.getSegment() == null) {
            return false;
        }
        String ziel = paket.getEmpfaenger();
        if (ziel == null || ziel.equals(serverAdresse) || !Vpn.imNetz(ziel, z.netz, z.netzMaske)) {
            return false;
        }
        paket.setSender(z.adresse);
        String text = Vpn.kodieren(paket);
        String record = Vpn.daten(Tls.verschluesseln(sk, text));
        Vpn.merken(record, new Vpn.Klartext(Vpn.beschreibung(text), text, true, z.adresse));
        p.senden(serverAdresse, Vpn.PORT, record);
        return true;
    }
}
