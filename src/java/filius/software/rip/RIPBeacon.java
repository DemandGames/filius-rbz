/*
 * Filius (RBZ Wirtschaft Kiel Version) - based on Filius 2.9.3 (GPL), www.lernsoftware-filius.de
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel
 *
 * Sends the routing announcements (RIP) of a router.
 *
 * Changed: In the original, the announce loop of a stopped simulation kept waiting for its old
 * timer (up to 35 seconds) and then simply continued in the next simulation run, still with its
 * old socket. The new run's loop could not start before that. After switching from the design to
 * the simulation mode, routers therefore often learned their routes only half a minute later,
 * and a ping failed in between. Now a loop ends as soon as its run is over, waits in short steps
 * and the next run starts with a fresh socket right away. The first announcement of a run is repeated
 * after three seconds, because neighbours that start at the same time may not listen yet.
 */
package filius.software.rip;

import java.util.Random;

import filius.exception.VerbindungsException;
import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.InternetKnoten;
import filius.software.clientserver.ClientAnwendung;
import filius.software.system.VermittlungsrechnerBetriebssystem;
import filius.software.transportschicht.UDPSocket;

public class RIPBeacon extends ClientAnwendung {
    private Random rand;
    /** number of the current simulation run; a loop of an earlier run stops */
    private volatile int lauf;

    @Override
    public void starten() {
        super.starten();
        this.rand = new Random();
        this.lauf++;
        RIPTable table = this.tabelle();
        if (table != null) {
            synchronized (table) {
                table.notifyAll();
            }
        }
        this.ausfuehren("announce", null);
    }

    @Override
    public void beenden() {
        super.beenden();
        this.lauf++;
        RIPTable table = this.tabelle();
        if (table != null) {
            synchronized (table) {
                table.notifyAll();
            }
        }
    }

    private RIPTable tabelle() {
        try {
            return ((VermittlungsrechnerBetriebssystem) this.getSystemSoftware()).getRIPTable();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean aktuell(int meinLauf) {
        return this.running && meinLauf == this.lauf;
    }

    public void announce() {
        int meinLauf = this.lauf;
        VermittlungsrechnerBetriebssystem bs = (VermittlungsrechnerBetriebssystem) this.getSystemSoftware();
        RIPTable table = bs.getRIPTable();
        if (table == null) {
            return;
        }
        UDPSocket sock = null;
        // the port of the previous run may still be closing: try again for a moment
        for (int versuch = 0; versuch < 20 && sock == null && this.aktuell(meinLauf); versuch++) {
            try {
                UDPSocket s = new UDPSocket(bs, "255.255.255.255", 520, 521);
                s.verbinden();
                sock = s;
            } catch (VerbindungsException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // check again
                }
            }
        }
        if (sock == null) {
            return;
        }
        // the first announcement of a run can get lost while the neighbours are still starting their RIP
        // servers; a second one follows after a few seconds, then the usual interval of about 30 seconds
        int gesendet = 0;
        try {
            while (this.aktuell(meinLauf)) {
                synchronized (table) {
                    long warten;
                    while (this.aktuell(meinLauf) && (warten = table.getNextBeacon() - RIPUtil.getTime()) > 0L) {
                        try {
                            table.wait(Math.min(warten, 250L));
                        } catch (InterruptedException e) {
                            // check again
                        }
                    }
                    if (!this.aktuell(meinLauf)) {
                        break;
                    }
                    table.check();
                    this.broadcast(sock, bs, table);
                    gesendet++;
                    table.setNextBeacon(RIPUtil.getTime() + (gesendet == 1 ? 3000L
                            : (long) ((int) (30000.0 * ((double) (this.rand.nextFloat() / 3.0f) + 0.84)))));
                }
            }
        } finally {
            sock.beenden();
        }
    }

    public void broadcast(UDPSocket sock, VermittlungsrechnerBetriebssystem bs, RIPTable table) {
        InternetKnoten knoten = (InternetKnoten) bs.getKnoten();
        for (NetzwerkInterface nic : knoten.getNetzwerkInterfaces()) {
            RIPMessage msg = new RIPMessage(nic.getIp(), bs.primaryIPAdresse(), 16, 75000);
            for (RIPRoute route : table.routes) {
                if (nic.getIp().equals(route.getInterfaceIpAddress())) {
                    continue;
                }
                msg.addRoute(new RIPMessageRoute(route.getNetAddress(), route.getNetMask(), route.hops));
            }
            sock.bind(nic.getIp());
            sock.senden(msg.toString());
        }
    }
}
