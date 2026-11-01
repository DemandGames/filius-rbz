package filius.modern;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.reflect.Field;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import filius.gui.ControlPanel;
import filius.gui.GUIContainer;
import filius.gui.JMainFrame;

/**
 * The work area (canvas) of Filius without white edges.
 *
 * Filius draws its canvas in a fixed size of 2000 x 1500 pixels. On wide screens, above all in the
 * simulation where the component bar is hidden, the window was wider than the canvas and a white
 * strip remained on the right. Here the canvas grows to the size of the largest screen, and the
 * areas around it (scroll bars, corner, the folded panels at the bottom and on the right with their
 * small tabs) take the colour of the canvas. New structure fields of the documentation mode are white.
 */
public final class Leinwand {

    static final Color ENTWURF = Theme.CANVAS;
    static final Color SIMULATION = Theme.farbe(0xEEF5F1, 0x0D1A17);

    /** colour of the canvas in the current mode */
    private static Color aktuell = ENTWURF;

    private Leinwand() {}

    /** Once, when the main window appears. */
    public static void einrichten() {
        try {
            vergroessern();
        } catch (Exception | LinkageError e) {
            // the canvas keeps its size
        }
        try {
            // the panel at the bottom is exchanged when another device is selected
            JMainFrame.getJMainFrame().getContentPane().addContainerListener(new java.awt.event.ContainerAdapter() {
                @Override
                public void componentAdded(java.awt.event.ContainerEvent e) {
                    raender();
                }
            });
        } catch (RuntimeException e) {
            // colours are cosmetic
        }
        try {
            // a structure field dragged from the bar of the documentation mode starts white instead of grey
            Field f = GUIContainer.class.getDeclaredField("docuDragPanel");
            f.setAccessible(true);
            ((Container) f.get(GUIContainer.getGUIContainer())).addContainerListener(new java.awt.event.ContainerAdapter() {
                @Override
                public void componentAdded(java.awt.event.ContainerEvent e) {
                    if (e.getChild() instanceof filius.gui.netzwerksicht.JDocuElement) {
                        filius.gui.netzwerksicht.JDocuElement el = (filius.gui.netzwerksicht.JDocuElement) e.getChild();
                        if (el.getText() == null) {
                            el.setColor(Color.WHITE);
                        }
                    }
                }
            });
        } catch (Exception | LinkageError e) {
            // the original grey stays
        }
        farben(GUIContainer.getGUIContainer().getActiveSite());
        // the colours of the scroll bars are stored as text: set them again after a switch to or from the dark mode
        DunkelModus.addListener(() -> farben(GUIContainer.getGUIContainer().getActiveSite()));
    }

    /** The folded panels next to the canvas (properties at the bottom, help on the right). */
    private static void raender() {
        try {
            Container cp = JMainFrame.getJMainFrame().getContentPane();
            if (!(cp.getLayout() instanceof BorderLayout)) {
                return;
            }
            BorderLayout bl = (BorderLayout) cp.getLayout();
            faerben(bl.getLayoutComponent(BorderLayout.SOUTH));
            faerben(bl.getLayoutComponent(BorderLayout.EAST));
        } catch (RuntimeException e) {
            // colours are cosmetic
        }
    }

    private static void faerben(Component c) {
        if (c instanceof ControlPanel) {
            c.setBackground(aktuell);
            c.repaint();
        } else if (c instanceof Container && !(c instanceof JScrollPane)) {
            for (Component k : ((Container) c).getComponents()) {
                if (k instanceof ControlPanel) {
                    faerben(k);
                }
            }
        }
    }

    private static void vergroessern() throws Exception {
        GUIContainer gc = GUIContainer.getGUIContainer();
        int bildschirmB = 0;
        int bildschirmH = 0;
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle r = gd.getDefaultConfiguration().getBounds();
            bildschirmB = Math.max(bildschirmB, r.width);
            bildschirmH = Math.max(bildschirmH, r.height);
        }
        int w = Math.max(gc.getWidth(), bildschirmB + 200);
        int h = Math.max(gc.getHeight(), bildschirmH + 200);
        if (w == gc.getWidth() && h == gc.getHeight()) {
            return;
        }
        feld("width").setInt(gc, w);
        feld("height").setInt(gc, h);
        Dimension d = new Dimension(w, h);
        for (String name : new String[] { "layeredPane", "designListenerPanel", "networkPanel", "docuPanel" }) {
            JComponent c = (JComponent) feld(name).get(gc);
            c.setSize(d);
            c.setMinimumSize(d);
            c.setPreferredSize(d);
        }
        for (String name : new String[] { "designBackgroundPanel", "simulationBackgroundPanel", "docuDragPanel", "networkPanel", "docuPanel" }) {
            ((Component) feld(name).get(gc)).setBounds(0, 0, w, h);
        }
        ((JComponent) feld("layeredPane").get(gc)).revalidate();
    }

    private static Field feld(String name) throws NoSuchFieldException {
        Field f = GUIContainer.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /** Scroll bars and corner in the colour of the canvas of this mode. */
    public static void farben(int modus) {
        try {
            Color c = modus == Theme.MODE_SIMULATION ? SIMULATION : ENTWURF;
            aktuell = c;
            raender();
            GUIContainer gc = GUIContainer.getGUIContainer();
            for (Object o : new Object[] { gc.getScrollPane(), gc.getSimpanel() }) {
                if (!(o instanceof JScrollPane)) {
                    continue;
                }
                JScrollPane sp = (JScrollPane) o;
                sp.setBackground(c);
                sp.getViewport().setBackground(c);
                for (JScrollBar sb : new JScrollBar[] { sp.getVerticalScrollBar(), sp.getHorizontalScrollBar() }) {
                    sb.setBackground(c);
                    sb.putClientProperty("FlatLaf.style", "track: " + hex(c));
                }
                JPanel ecke = new JPanel();
                ecke.setBackground(c);
                sp.setCorner(JScrollPane.LOWER_RIGHT_CORNER, ecke);
                sp.repaint();
            }
        } catch (RuntimeException e) {
            // colours are cosmetic
        }
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
