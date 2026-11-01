/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.software.vpn;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.transportschicht.Segment;
import filius.software.transportschicht.SocketSchnittstelle;
import filius.software.transportschicht.UdpSegment;

/**
 * The UDP port of a VPN client or server. Unlike a UDPSocket of Filius it can send and receive at the same time
 * (a UDPSocket keeps its lock while it waits for data) and it knows the sender of each datagram. Received
 * datagrams are handled one after the other on an own thread.
 */
final class VpnPort implements SocketSchnittstelle {
    private static final Logger LOG = LoggerFactory.getLogger(VpnPort.class);
    private static final Random ZUFALL = new Random();

    interface Empfaenger {
        void empfangen(String ip, int port, String daten);
    }

    private final InternetKnotenBetriebssystem bs;
    final int port;
    private final Empfaenger empfaenger;
    private final LinkedBlockingQueue<Object[]> eingang = new LinkedBlockingQueue<>();
    private volatile boolean offen = true;
    private final Thread arbeiter;

    private VpnPort(InternetKnotenBetriebssystem bs, int port, Empfaenger empfaenger) {
        this.bs = bs;
        this.port = port;
        this.empfaenger = empfaenger;
        this.arbeiter = new Thread(this::arbeiten, "VPN port " + port);
        this.arbeiter.setDaemon(true);
    }

    /** the port with this number, null if it is in use */
    static VpnPort oeffnen(InternetKnotenBetriebssystem bs, int port, Empfaenger empfaenger) {
        VpnPort p = new VpnPort(bs, port, empfaenger);
        if (!bs.holeUdp().reservierePort(port, p)) {
            return null;
        }
        p.arbeiter.start();
        return p;
    }

    /** a free port for a client */
    static VpnPort oeffnenFrei(InternetKnotenBetriebssystem bs, Empfaenger empfaenger) {
        for (int i = 0; i < 200; i++) {
            int nr = 49152 + ZUFALL.nextInt(16000);
            if (bs.holeUdp().isUsed(nr)) {
                continue;
            }
            VpnPort p = oeffnen(bs, nr, empfaenger);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void hinzufuegen(String startIp, int startPort, Segment segment) {
        if (offen && segment != null) {
            eingang.offer(new Object[] { startIp, startPort, segment.getDaten() });
        }
    }

    private void arbeiten() {
        while (offen) {
            Object[] d;
            try {
                d = eingang.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                continue;
            }
            if (d == null || !offen) {
                continue;
            }
            try {
                empfaenger.empfangen((String) d[0], (Integer) d[1], d[2] == null ? "" : d[2].toString());
            } catch (RuntimeException e) {
                LOG.debug("VPN: datagram not handled", e);
            }
        }
    }

    void senden(String zielIp, int zielPort, String daten) {
        UdpSegment s = new UdpSegment();
        s.setQuellPort(this.port);
        s.setZielPort(zielPort);
        s.setDaten(daten);
        bs.holeIP().senden(zielIp, null, 17, 64, s);
    }

    boolean istOffen() {
        return offen;
    }

    @Override
    public void schliessen() {
        if (!offen) {
            return;
        }
        offen = false;
        eingang.clear();
        try {
            if (bs.holeUdp().holeSocket(port) == this) {
                bs.holeUdp().gibPortFrei(port);
            }
        } catch (Exception e) {
            // not registered any more
        }
        arbeiter.interrupt();
    }

    @Override
    public void beenden() {
        schliessen();
    }
}
