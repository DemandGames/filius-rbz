/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JCablePanel of Filius 2.9.3 (GPL). The cable geometry, hit test and
 * observer logic are unchanged. New: softer cable colours and a picture of every packet in its direction
 * (version 24): a cable fills with blue from the sender to the receiver and then empties behind the packet,
 * so the blue flows into the receiver; on a Wi-Fi link radio waves go out from the sender towards the
 * receiver and a signal point lights up at the receiver when the packet has arrived. The direction comes
 * from the simulation (SimplexVerbindung -> filius.modern.Paketflug). Outside the simulation a cable marked
 * by the configuration of a router (selected network card) glows blue and pulses.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.netzwerksicht;

import filius.gui.netzwerksicht.GUIKabelItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.hardware.Hardware;
import filius.hardware.Port;
import filius.hardware.Verbindung;
import filius.hardware.knoten.Anschlusssuche;
import filius.modern.Paketflug;
import filius.modern.Prefs;
import filius.modern.Theme;
import filius.rahmenprogramm.IconMode;
import filius.rahmenprogramm.Information;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCablePanel
extends JPanel
implements Observer, Paketflug.Anzeige {
    private static Logger LOG = LoggerFactory.getLogger(JCablePanel.class);
    private static final long serialVersionUID = 1L;
    /** extra space around the cable so that the glow of a packet is not cut off */
    private static final int MARGIN = 16;
    /** around a Wi-Fi link the radio waves of the sender need more room */
    private static final int MARGIN_WLAN = 104;
    /** the picture of a Wi-Fi packet (waves, signal point) runs this much slower than the packet itself */
    private static final double WLAN_FAKTOR = 1.3;
    /** how long the signal point of a Wi-Fi link shines after the packet arrived */
    private static final int SIGNAL_MS = (int) (900 * WLAN_FAKTOR);
    /** one pulse of a marked cable (selected network card in the configuration of a router) */
    private static final int PULS_MS = 1400;
    /** frames that follow each other closer than this in one direction show as one packet */
    private static final int ABSTAND_MS = 110;
    private static final Color GLUT = Theme.farbe(0x5C95F2, 0x8EBBFF);
    private GUIKnotenItem ziel1;
    private GUIKnotenItem ziel2;
    private GUIKabelItem kabelItem;
    private QuadCurve2D currCurve = null;
    private Color kabelFarbe = new Color(64, 64, 64);
    private final Color farbeKabel = new Color(64, 64, 64);
    private final Color farbeWireless = new Color(192, 192, 192);
    private final Color farbeBlinken = new Color(0, 255, 64);
    private final Color farbeFailure = Color.RED;

    /** a packet on its way: start time, flight time, true = from ziel1 to ziel2 */
    private static final class Flug {
        final long start;
        final int dauer;
        final boolean vorwaerts;

        Flug(long start, int dauer, boolean vorwaerts) {
            this.start = start;
            this.dauer = dauer;
            this.vorwaerts = vorwaerts;
        }
    }

    private final List<Flug> fluege = new ArrayList<>();
    private static final Set<JCablePanel> ANIMIERT = Collections.synchronizedSet(new HashSet<JCablePanel>());
    private static Timer animation;
    private static final java.util.concurrent.atomic.AtomicBoolean TIMER_ANGEFORDERT = new java.util.concurrent.atomic.AtomicBoolean();

    public JCablePanel() {
        this.setOpaque(false);
    }

    public void updateBounds() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (JCablePanel), updateBounds()");
        if (null != this.ziel1 && null != this.ziel2) {
            int t1;
            int x1 = this.ziel1.getImageLabel().getX();
            int x2 = this.ziel2.getImageLabel().getX();
            int y1 = this.ziel1.getImageLabel().getY();
            int y2 = this.ziel2.getImageLabel().getY();
            x1 = (int)((double)x1 + 0.5 * (double)this.ziel1.getImageLabel().getWidth());
            y1 = (int)((double)y1 + 0.5 * (double)this.ziel1.getImageLabel().getHeight());
            x2 = (int)((double)x2 + 0.5 * (double)this.ziel2.getImageLabel().getWidth());
            y2 = (int)((double)y2 + 0.5 * (double)this.ziel2.getImageLabel().getHeight());
            if (x1 > x2) {
                t1 = x1;
                x1 = x2;
                x2 = t1;
            }
            if (y1 > y2) {
                t1 = y1;
                y1 = y2;
                y2 = t1;
            }
            int m = this.isWireless() ? MARGIN_WLAN : MARGIN;
            this.setBounds(x1 - m, y1 - m, x2 - x1 + 2 * m, y2 - y1 + 2 * m);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int kp2;
        int kp1;
        super.paintComponent(g);
        int x1 = (int)((double)this.ziel1.getImageLabel().getX() + 0.5 * (double)this.ziel1.getImageLabel().getWidth());
        int x2 = (int)((double)this.ziel2.getImageLabel().getX() + 0.5 * (double)this.ziel2.getImageLabel().getWidth());
        int y1 = (int)((double)this.ziel1.getImageLabel().getY() + 0.5 * (double)this.ziel1.getImageLabel().getHeight());
        int y2 = (int)((double)this.ziel2.getImageLabel().getY() + 0.5 * (double)this.ziel2.getImageLabel().getHeight());
        Graphics2D g2 = Theme.smooth(g.create());
        // the bend of the curve is computed in the coordinates of a panel with the earlier margin of 8 pixels,
        // so a larger margin (room for the effects) does not change the shape of the cable
        int ox = Math.min(x1, x2) - 8;
        int oy = Math.min(y1, y2) - 8;
        int m = this.isWireless() ? MARGIN_WLAN : MARGIN;
        if (this.getX() != Math.min(x1, x2) - m || this.getY() != Math.min(y1, y2) - m) {
            // the kind of link was not known yet when the bounds were set
            SwingUtilities.invokeLater(this::updateBounds);
        }
        if (Information.getInformation().getIconMode().equals((Object)IconMode.SYMBOL)) {
            kp1 = (x1 - ox + x2 - ox) / 2;
            kp2 = (y1 - oy + y2 - oy) / 2;
        } else {
            kp1 = (x1 - ox + x2 - ox) / 4;
            if (x1 > x2 && y1 > y2 || x1 < x2 && y1 < y2) {
                kp1 = 3 * kp1;
            }
            kp2 = (y1 - oy + y2 - oy) / 4;
        }
        kp1 += ox - this.getX();
        kp2 += oy - this.getY();
        QuadCurve2D.Double myCurve = new QuadCurve2D.Double(x1 - this.getX(), y1 - this.getY(), kp1, kp2, x2 - this.getX(), y2 - this.getY());

        boolean active = this.kabelFarbe.equals(this.farbeBlinken);
        boolean failure = this.kabelFarbe.equals(this.farbeFailure);
        boolean wireless = this.isWireless();
        Color base = wireless ? Theme.CABLE_WIRELESS : Theme.CABLE;
        this.anmelden();

        if (failure) {
            paintGlow(g2, myCurve, Theme.SIGNAL_RED, 1.0);
            g2.setColor(Theme.SIGNAL_RED);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else if (active && !simulation()) {
            // the configuration of a router marks the cable of the selected network card: it glows blue and pulses
            double puls = Prefs.effectsEnabled() ? 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 2 * Math.PI / PULS_MS) : 1;
            if (this.markiert() && !ANIMIERT.contains(this)) {
                // e.g. marked again when the design mode came back: keep pulsing
                animieren(this);
            }
            paintGlow(g2, myCurve, GLUT, 0.6 + 0.6 * puls);
            g2.setColor(Theme.PATCH_BLUE);
            g2.setStroke(wireless
                    ? new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6.0f, 7.0f}, 0.0f)
                    : new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else if (active && !Prefs.effectsEnabled()) {
            // without effects the link simply turns blue while it transmits
            g2.setColor(Theme.PATCH_BLUE);
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else if (wireless) {
            g2.setColor(base);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6.0f, 7.0f}, 0.0f));
        } else {
            g2.setColor(base);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g2.draw(myCurve);
        if (!failure) {
            this.paintFluege(g2, myCurve, wireless);
        }
        g2.dispose();
        this.currCurve = myCurve;
        this.setOpaque(false);
    }

    private static void paintGlow(Graphics2D g2, QuadCurve2D curve, Color c, double strength) {
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(c, 0.17 * strength));
        g2.draw(curve);
        g2.setStroke(new BasicStroke(6.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(c, 0.38 * strength));
        g2.draw(curve);
    }

    // ------------------------------------------------------------------ packets in flight

    /** the cable at even speed: points by the fraction of its length */
    private static final class Bahn {
        private static final int N = 64;
        private final double[] x = new double[N + 1];
        private final double[] y = new double[N + 1];
        private final double[] weg = new double[N + 1];
        final double laenge;

        Bahn(QuadCurve2D c) {
            for (int i = 0; i <= N; i++) {
                double t = (double) i / N, u = 1 - t;
                x[i] = u * u * c.getX1() + 2 * u * t * c.getCtrlX() + t * t * c.getX2();
                y[i] = u * u * c.getY1() + 2 * u * t * c.getCtrlY() + t * t * c.getY2();
                if (i > 0) {
                    weg[i] = weg[i - 1] + Math.hypot(x[i] - x[i - 1], y[i] - y[i - 1]);
                }
            }
            laenge = Math.max(1e-6, weg[N]);
        }

        /** point at fraction f of the length; vorwaerts = from the start of the curve */
        Point2D bei(double f, boolean vorwaerts) {
            double d = Math.max(0, Math.min(1, vorwaerts ? f : 1 - f)) * laenge;
            int i = 1;
            while (i < N && weg[i] < d) {
                i++;
            }
            double t = (d - weg[i - 1]) / Math.max(1e-9, weg[i] - weg[i - 1]);
            return new Point2D.Double(x[i - 1] + (x[i] - x[i - 1]) * t, y[i - 1] + (y[i] - y[i - 1]) * t);
        }

        /** the part of the cable between the fractions f0 and f1 */
        Path2D teil(double f0, double f1, boolean vorwaerts, int schritte) {
            Path2D.Double p = new Path2D.Double();
            for (int i = 0; i <= schritte; i++) {
                Point2D q = bei(f0 + (f1 - f0) * i / schritte, vorwaerts);
                if (i == 0) {
                    p.moveTo(q.getX(), q.getY());
                } else {
                    p.lineTo(q.getX(), q.getY());
                }
            }
            return p;
        }
    }

    private void paintFluege(Graphics2D g2, QuadCurve2D curve, boolean wireless) {
        Flug[] aktuell;
        synchronized (this.fluege) {
            if (this.fluege.isEmpty()) {
                return;
            }
            aktuell = this.fluege.toArray(new Flug[0]);
        }
        Bahn bahn = new Bahn(curve);
        long jetzt = System.currentTimeMillis();
        for (Flug fl : aktuell) {
            long dt = jetzt - fl.start;
            double f = Math.min(1.0, (double) dt / fl.dauer);
            long nach = dt >= fl.dauer ? dt - fl.dauer : -1;
            if (wireless) {
                this.paintWlan(g2, bahn, fl, f, nach, curve);
            } else {
                paintFuellung(g2, bahn, fl.vorwaerts, (double) dt / fl.dauer);
            }
        }
    }

    /** 0..1 with a soft start and end */
    private static double sanft(double x) {
        x = Math.max(0, Math.min(1, x));
        return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
    }

    /**
     * cable: the blue fills the cable from the sender to the receiver during the first half of the flight and
     * then flows on behind the packet, so the cable is empty again when the packet is delivered (u = 1)
     */
    private static void paintFuellung(Graphics2D g2, Bahn bahn, boolean vor, double u) {
        double hinten = sanft((u - 0.55) / 0.45);
        double vorne = sanft(u / 0.5);
        if (vorne - hinten < 0.002) {
            return;
        }
        Path2D teil = bahn.teil(hinten, vorne, vor, 40);
        g2.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(GLUT, 0.22));
        g2.draw(teil);
        g2.setStroke(new BasicStroke(4.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.PATCH_BLUE);
        g2.draw(teil);
        if (vorne < 0.999) {
            // the moving edge of the blue shines a little
            Point2D p = bahn.bei(vorne, vor);
            float r = 9f;
            g2.setPaint(new RadialGradientPaint(p, r, new float[] { 0f, 0.35f, 1f },
                    new Color[] { Theme.alpha(Color.WHITE, 0.9), Theme.alpha(GLUT, 0.7), Theme.alpha(Theme.PATCH_BLUE, 0) }));
            g2.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
        }
    }

    /** Wi-Fi: radio waves from the sender towards the receiver, a signal point at the receiver on arrival */
    private void paintWlan(Graphics2D g2, Bahn bahn, Flug fl, double f, long nach, QuadCurve2D curve) {
        Point2D sender = fl.vorwaerts ? curve.getP1() : curve.getP2();
        Point2D empf = fl.vorwaerts ? curve.getP2() : curve.getP1();
        double zeit = nach < 0 ? f * fl.dauer : fl.dauer + nach;
        double richtung = Math.atan2(empf.getY() - sender.getY(), empf.getX() - sender.getX());
        for (int i = 0; i < 3; i++) {
            double alter = zeit - i * fl.dauer * 0.16 * WLAN_FAKTOR;
            double leben = fl.dauer * 0.75 * WLAN_FAKTOR;
            if (alter < 0 || alter > leben) {
                continue;
            }
            double q = alter / leben;
            double r = 36 + 58 * q;
            g2.setStroke(new BasicStroke((float) (3.4 - 1.2 * q), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.alpha(Theme.PATCH_BLUE, (1 - q) * 0.95));
            // Arc2D counts degrees counter-clockwise with the y axis up
            g2.draw(new Arc2D.Double(sender.getX() - r, sender.getY() - r, 2 * r, 2 * r,
                    -Math.toDegrees(richtung) - 35.5, 71, Arc2D.OPEN));
        }
        if (nach >= 0) {
            double s = Math.max(0, 1 - nach / (double) SIGNAL_MS);
            double sichtbar = s < 0.75 ? s / 0.75 : 1;
            if (sichtbar > 0) {
                GUIKnotenItem ziel = fl.vorwaerts ? this.ziel2 : this.ziel1;
                double rand = rand(ziel, richtung + Math.PI);
                Point2D p = bahn.bei(1 - Math.min(0.45, (rand + 10) / bahn.laenge), fl.vorwaerts);
                signalpunkt(g2, p, sichtbar);
            }
        }
    }

    /** distance from the centre of a device to the edge of its picture, seen in the given direction */
    private static double rand(GUIKnotenItem item, double winkel) {
        double hw = 40, hh = 34;
        try {
            JSidebarButton b = item.getImageLabel();
            if (b.getIcon() != null) {
                hw = b.getIcon().getIconWidth() / 2.0 + 6;
            }
            hh = b.getHeight() / 2.0 + 4;
        } catch (RuntimeException e) {
            // keep the defaults
        }
        double c = Math.abs(Math.cos(winkel)), s = Math.abs(Math.sin(winkel));
        return Math.min(c < 1e-6 ? Double.MAX_VALUE : hw / c, s < 1e-6 ? Double.MAX_VALUE : hh / s);
    }

    private static void signalpunkt(Graphics2D g2, Point2D p, double s) {
        float r = 17f;
        g2.setPaint(new RadialGradientPaint(p, r, new float[] { 0f, 1f },
                new Color[] { Theme.alpha(GLUT, 0.8 * s), Theme.alpha(Theme.PATCH_BLUE, 0) }));
        g2.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
        g2.setPaint(Theme.alpha(Theme.PATCH_BLUE, Math.min(1, s * 1.4)));
        g2.fill(new Ellipse2D.Double(p.getX() - 4.8, p.getY() - 4.8, 9.6, 9.6));
        g2.setPaint(Theme.alpha(Color.WHITE, 0.9 * s));
        g2.fill(new Ellipse2D.Double(p.getX() - 2.6, p.getY() - 2.6, 2.8, 2.8));
    }

    /** a frame left the given port (thread of the simulation) */
    @Override
    public void paketGesendet(Port von, int dauerMs) {
        boolean vorwaerts;
        if (this.ziel1 != null && Anschlusssuche.gehoertZu(von, this.ziel1.getKnoten())) {
            vorwaerts = true;
        } else if (this.ziel2 != null && Anschlusssuche.gehoertZu(von, this.ziel2.getKnoten())) {
            vorwaerts = false;
        } else {
            return;
        }
        long jetzt = System.currentTimeMillis();
        synchronized (this.fluege) {
            for (Flug fl : this.fluege) {
                if (fl.vorwaerts == vorwaerts && jetzt - fl.start < ABSTAND_MS) {
                    return;
                }
            }
            if (this.fluege.size() >= 8) {
                this.fluege.remove(0);
            }
            this.fluege.add(new Flug(jetzt, dauerMs, vorwaerts));
            ANIMIERT.add(this);
        }
        animieren(this);
    }

    /** removes packets that are done; true if some are still to be shown */
    private boolean aufraeumen(long jetzt) {
        int nachlauf = this.isWireless() ? SIGNAL_MS : 40;
        synchronized (this.fluege) {
            this.fluege.removeIf(fl -> jetzt - fl.start > fl.dauer + nachlauf);
            boolean weiter = !this.fluege.isEmpty() || this.markiert();
            if (!weiter) {
                // under the same lock as the adding of a packet, so a packet that arrives now is not lost
                ANIMIERT.remove(this);
            }
            return weiter;
        }
    }

    /** marked by the configuration of a router (outside the simulation the cable is only active for that) */
    private boolean markiert() {
        return this.kabelFarbe.equals(this.farbeBlinken) && !simulation() && Prefs.effectsEnabled();
    }

    /** true while the simulation mode is shown; then "active" means a frame is being transmitted */
    private static boolean simulation() {
        try {
            return filius.gui.GUIContainer.getGUIContainer().getActiveSite() == Theme.MODE_SIMULATION;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void animieren(JCablePanel panel) {
        synchronized (panel.fluege) {
            ANIMIERT.add(panel);
        }
        // one start request at a time, also when the simulation sends many frames
        if (TIMER_ANGEFORDERT.getAndSet(true)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (animation == null) {
                animation = new Timer(20, e -> {
                    JCablePanel[] panels;
                    synchronized (ANIMIERT) {
                        panels = ANIMIERT.toArray(new JCablePanel[0]);
                    }
                    long jetzt = System.currentTimeMillis();
                    for (JCablePanel p : panels) {
                        p.neuZeichnen(p.aufraeumen(jetzt));
                    }
                    synchronized (ANIMIERT) {
                        if (ANIMIERT.isEmpty()) {
                            animation.stop();
                            TIMER_ANGEFORDERT.set(false);
                        }
                    }
                });
            }
            if (!animation.isRunning()) {
                animation.start();
            }
        });
    }

    /**
     * Repaints what changes: the whole cable, but on a Wi-Fi link (large panel because of the waves) only the
     * surroundings of its two ends, where the waves and the signal point are.
     */
    private void neuZeichnen(boolean weiter) {
        QuadCurve2D c = this.currCurve;
        if (!this.isWireless() || c == null || !weiter || this.markiert()) {
            this.repaint();
            return;
        }
        int r = 104;
        this.repaint((int) c.getX1() - r, (int) c.getY1() - r, 2 * r, 2 * r);
        this.repaint((int) c.getX2() - r, (int) c.getY2() - r, 2 * r, 2 * r);
    }

    /** this picture shows the packets of its connection */
    private void anmelden() {
        try {
            if (this.kabelItem != null && this.kabelItem.getDasKabel() != null) {
                Paketflug.anmelden(this.kabelItem.getDasKabel(), this);
            }
        } catch (RuntimeException e) {
            // no connection yet
        }
    }

    private boolean isWireless() {
        return this.getKabelItem() != null && this.getKabelItem().getDasKabel() != null && this.getKabelItem().getDasKabel().getWireless();
    }

    public boolean clicked(int x, int y) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (JCablePanel), clicked(" + x + "," + y + ")");
        int delta = 10;
        Rectangle2D.Double absolutePointerRect = new Rectangle2D.Double(x - delta, y - delta, 2 * delta, 2 * delta);
        boolean hitPanel = this.getBounds().intersects(absolutePointerRect);
        Rectangle2D.Double relativePointerRect = new Rectangle2D.Double(x - this.getX() - delta, y - this.getY() - delta, 2 * delta, 2 * delta);
        boolean hitCurve = this.currCurve.intersects(relativePointerRect);
        return hitCurve && hitPanel;
    }

    public GUIKnotenItem getZiel1() {
        return this.ziel1;
    }

    public void setZiel1(GUIKnotenItem ziel1) {
        this.ziel1 = ziel1;
    }

    public GUIKnotenItem getZiel2() {
        return this.ziel2;
    }

    public void setZiel2(GUIKnotenItem ziel2) {
        this.ziel2 = ziel2;
        this.updateBounds();
    }

    public void setKabelItem(GUIKabelItem value) {
        this.kabelItem = value;
    }

    public GUIKabelItem getKabelItem() {
        return this.kabelItem;
    }

    @Override
    public void update(Observable o, Object arg) {
        LOG.trace("update cable: {}", arg);
        if (o instanceof Verbindung) {
            Paketflug.anmelden((Verbindung) o, this);
        }
        if (arg.equals(Boolean.TRUE)) {
            this.kabelFarbe = this.farbeBlinken;
            if (this.markiert()) {
                animieren(this);
            }
        } else if (arg.equals(Hardware.FAILURE)) {
            LOG.debug("set cable color to red");
            this.kabelFarbe = this.farbeFailure;
        } else {
            this.resetColor();
        }
        this.updateUI();
    }

    private void resetColor() {
        this.kabelFarbe = this.isWireless() ? this.farbeWireless : this.farbeKabel;
    }

}
