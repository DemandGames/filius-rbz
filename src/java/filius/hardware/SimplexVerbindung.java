/*
 * Filius (RBZ Wirtschaft Kiel Version) - based on Filius 2.9.3 (GPL), www.lernsoftware-filius.de
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel
 *
 * One direction of a cable: takes the frames from the output buffer of the sending port and delivers
 * them to the other port after the delay of the connection.
 *
 * Changed: every frame that goes onto the cable is reported to filius.modern.Paketflug together with
 * the sending port, so the cable can show a packet flying in the right direction (shooting star on a
 * cable, radio waves and a signal point on a Wi-Fi link). Otherwise unchanged.
 */
package filius.hardware;

import filius.hardware.Port;
import filius.hardware.Verbindung;
import filius.rahmenprogramm.nachrichten.Lauscher;
import filius.software.netzzugangsschicht.EthernetFrame;
import java.util.LinkedList;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplexVerbindung
implements Runnable {
    private static Logger LOG = LoggerFactory.getLogger(SimplexVerbindung.class);
    private boolean threadRunning = true;
    private Verbindung verbindung = null;
    private Port anschluss1 = null;
    private Port anschluss2 = null;

    public SimplexVerbindung(Port anschluss1, Port anschluss2, Verbindung verbindung) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (SimplexVerbindung), constr: SimplexVerbindung(" + String.valueOf(anschluss1) + "," + String.valueOf(anschluss2) + "," + String.valueOf(verbindung) + ")");
        this.anschluss1 = anschluss1;
        this.anschluss2 = anschluss2;
        this.verbindung = verbindung;
    }

    @Override
    public void run() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (SimplexVerbindung), run()");
        while (this.threadRunning) {
            LinkedList<EthernetFrame> linkedList = this.anschluss1.holeAusgangsPuffer();
            synchronized (linkedList) {
                if (this.anschluss1.holeAusgangsPuffer().size() < 1) {
                    try {
                        this.verbindung.setAktiv(false);
                        this.anschluss1.holeAusgangsPuffer().wait();
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                }
                if (this.anschluss1.holeAusgangsPuffer().size() > 0) {
                    EthernetFrame frame = this.anschluss1.holeAusgangsPuffer().removeFirst();
                    boolean dropFrame = Verbindung.isDrop();
                    if (dropFrame) {
                        this.verbindung.setFailure();
                    } else {
                        this.verbindung.setAktiv(true);
                        filius.modern.Paketflug.gesendet(this.verbindung, this.anschluss1);
                    }
                    Object object = this;
                    synchronized (object) {
                        try {
                            Thread.sleep(Verbindung.holeVerzoegerung());
                        }
                        catch (InterruptedException interruptedException) {
                            // empty catch block
                        }
                    }
                    if (!dropFrame) {
                        object = this.anschluss2.holeEingangsPuffer();
                        synchronized (object) {
                            this.anschluss2.holeEingangsPuffer().add(SerializationUtils.clone(frame));
                            this.anschluss2.holeEingangsPuffer().notify();
                        }
                    } else {
                        Lauscher.getLauscher().addDroppedDataUnit(frame);
                        LOG.debug("Frame dropped.");
                    }
                }
            }
        }
    }

    public Port getPort1() {
        return this.anschluss1;
    }

    public void setPort1(Port anschluss1) {
        this.anschluss1 = anschluss1;
    }

    public Port getPort2() {
        return this.anschluss2;
    }

    public void setPort2(Port anschluss2) {
        this.anschluss2 = anschluss2;
    }

    public void anschluesseTrennen() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (SimplexVerbindung), anschluesseTrennen()");
        this.anschluss1.entferneVerbindung();
        this.anschluss2.entferneVerbindung();
        this.setThreadRunning(false);
    }

    public Verbindung getVerbindung() {
        return this.verbindung;
    }

    public void setVerbindung(Verbindung verbindung) {
        this.verbindung = verbindung;
    }

    public boolean isThreadRunning() {
        return this.threadRunning;
    }

    public void setThreadRunning(boolean threadRunning) {
        this.threadRunning = threadRunning;
    }
}

