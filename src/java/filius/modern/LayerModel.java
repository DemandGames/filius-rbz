package filius.modern;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import filius.rahmenprogramm.nachrichten.Lauscher;

/**
 * Which layer model the data exchange window shows: the TCP/IP model Filius is built on (4 layers,
 * the default) or the OSI model (7 layers). Filius records data for the TCP/IP layers only; in the OSI
 * view they appear as OSI layers 2 (Ethernet), 3 (IP, ARP, ICMP), 4 (TCP, UDP) and 7 (application),
 * while layers 1, 5 and 6 are shown with an explanation instead of data.
 *
 * Teachers can make OSI the default for an installation with {@code modern-schichtmodell=osi} in
 * config/filius.ini.
 */
public final class LayerModel {

    private LayerModel() {}

    /** OSI layer number of the Filius (TCP/IP) layer index 0..3: link, internet, transport, application. */
    private static final int[] OSI_OF_TCPIP = { 2, 3, 4, 7 };

    private static boolean osi = Prefs.osiByDefault();
    private static final List<WeakReference<Runnable>> listeners = new ArrayList<>();

    public static boolean isOsi() {
        return osi;
    }

    public static void setOsi(boolean value) {
        if (osi == value) {
            return;
        }
        osi = value;
        fire();
    }

    /**
     * Calls {@code listener} after every switch. Only a weak reference is kept, so the caller must hold
     * on to the listener as long as it is interested.
     */
    public static synchronized void addListener(Runnable listener) {
        listeners.add(new WeakReference<>(listener));
    }

    private static void fire() {
        List<Runnable> alive = new ArrayList<>();
        synchronized (LayerModel.class) {
            for (Iterator<WeakReference<Runnable>> it = listeners.iterator(); it.hasNext();) {
                Runnable r = it.next().get();
                if (r == null) {
                    it.remove();
                } else {
                    alive.add(r);
                }
            }
        }
        for (Runnable r : alive) {
            r.run();
        }
        for (Window w : Window.getWindows()) {
            w.repaint();
        }
    }

    /** Filius layer index (0..3) of a layer name as stored in the data exchange rows, or -1. */
    public static int tcpipIndex(Object layerName) {
        if (layerName == null) {
            return -1;
        }
        for (int i = 0; i < Lauscher.PROTOKOLL_SCHICHTEN.length; i++) {
            if (Lauscher.PROTOKOLL_SCHICHTEN[i].equals(layerName.toString())) {
                return i;
            }
        }
        return -1;
    }

    public static int osiOf(int tcpipIndex) {
        return tcpipIndex >= 0 && tcpipIndex < OSI_OF_TCPIP.length ? OSI_OF_TCPIP[tcpipIndex] : -1;
    }

    /** TCP/IP layer index (0..3) that contains OSI layer n (1..7). */
    public static int tcpipOfOsi(int n) {
        if (n <= 2) {
            return 0;
        }
        if (n == 3) {
            return 1;
        }
        if (n == 4) {
            return 2;
        }
        return 3;
    }

    public static String osiName(int n) {
        return Texts.get("osi." + n);
    }

    public static String tcpipName(int index) {
        return Lauscher.PROTOKOLL_SCHICHTEN[index];
    }

    /** Text of the "layer" column: the Filius name, or "n Name" in the OSI view. */
    public static String tableLabel(Object layerName) {
        if (!osi || layerName == null) {
            return layerName == null ? "" : layerName.toString();
        }
        int n = osiOf(tcpipIndex(layerName));
        return n < 0 ? layerName.toString() : n + "  " + osiName(n);
    }

    /** Segmented switch "TCP/IP | OSI" for the data exchange window. */
    public static JComponent createSwitch() {
        return new Switch();
    }

    private static final class Switch extends JComponent {
        private static final long serialVersionUID = 1L;
        private final Runnable onChange = this::repaint;
        private int hover = -1;

        Switch() {
            setFont(Theme.font(Font.BOLD, 12.5f));
            setToolTipText(Texts.get("model.tooltip"));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            addListener(onChange);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setOsi(segmentAt(e.getX()) == 1);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int h = segmentAt(e.getX());
                    if (h != hover) {
                        hover = h;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private String[] labels() {
            return new String[] { Texts.get("model.tcpip"), Texts.get("model.osi") };
        }

        private int[] widths() {
            FontMetrics fm = getFontMetrics(getFont());
            String[] l = labels();
            int w = Math.max(fm.stringWidth(l[0]), fm.stringWidth(l[1])) + 28;
            return new int[] { w, w };
        }

        private int segmentAt(int x) {
            int[] w = widths();
            int start = getWidth() - (w[0] + w[1] + 6) - 6;
            return x - start < 3 + w[0] ? 0 : 1;
        }

        @Override
        public Dimension getPreferredSize() {
            int[] w = widths();
            return new Dimension(w[0] + w[1] + 6 + 12, 34);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                int[] w = widths();
                int trackW = w[0] + w[1] + 6;
                int h = 28;
                int x = getWidth() - trackW - 6;
                int y = (getHeight() - h) / 2;
                g2.setColor(Theme.LINE_SOFT);
                g2.fill(new RoundRectangle2D.Double(x, y, trackW, h, h, h));
                int active = osi ? 1 : 0;
                FontMetrics fm = g2.getFontMetrics(getFont());
                String[] l = labels();
                int sx = x + 3;
                for (int i = 0; i < 2; i++) {
                    if (i == active) {
                        g2.setColor(new Color(0, 0, 0, 22));
                        g2.fill(new RoundRectangle2D.Double(sx, y + 4, w[i], h - 6, h - 6, h - 6));
                        g2.setColor(Theme.SURFACE);
                        g2.fill(new RoundRectangle2D.Double(sx, y + 3, w[i], h - 6, h - 6, h - 6));
                    } else if (i == hover) {
                        g2.setColor(Theme.alpha(Color.WHITE, 0.55));
                        g2.fill(new RoundRectangle2D.Double(sx, y + 3, w[i], h - 6, h - 6, h - 6));
                    }
                    g2.setFont(getFont());
                    g2.setColor(i == active ? Theme.INK : Theme.INK_2);
                    int tw = fm.stringWidth(l[i]);
                    g2.drawString(l[i], sx + (w[i] - tw) / 2f, y + (h + fm.getAscent() - fm.getDescent()) / 2f);
                    sx += w[i];
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
