package filius.modern;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;

/**
 * Light and dark mode of Filius (RBZ Wirtschaft Kiel Version). The choice is a personal setting
 * (~/.filius/filius-modern.properties, key modern.dunkel) and is switched with the sun/moon toggle in the header.
 * Switching changes the look and feel and all theme colours at once, with a short cross-fade.
 */
public final class DunkelModus {

    static final String KEY = "modern.dunkel";
    private static final List<Runnable> HOERER = new CopyOnWriteArrayList<>();
    private static final String KEY_GENERATION = "filius.lafGeneration";
    /** counts the switches; components remember in which look they were last shown */
    private static int generation;

    private DunkelModus() {
    }

    /** At start, before the first window: reads the setting and installs the matching look and feel. */
    public static void beimStart() {
        Theme.setDunkel(Prefs.getBool(KEY, false));
        lafEinrichten();
    }

    private static void lafEinrichten() {
        if (Theme.dunkel()) {
            FiliusDarkLaf.setup();
        } else {
            FiliusLaf.setup();
        }
        UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(Theme.font(java.awt.Font.PLAIN, 13f)));
        SafeTextUIs.install();
    }

    /** Code that has to run after a switch (colours that are stored as text, e.g. FlatLaf styles). */
    public static void addListener(Runnable r) {
        HOERER.add(r);
    }

    public static void umschalten() {
        setzen(!Theme.dunkel());
    }

    public static void setzen(boolean an) {
        if (an == Theme.dunkel()) {
            return;
        }
        List<Bild> bilder = schnappschuss();
        Theme.setDunkel(an);
        Prefs.setBool(KEY, an);
        generation++;
        try {
            lafEinrichten();
            FlatLaf.updateUI();
            for (Window w : Window.getWindows()) {
                if (w instanceof RootPaneContainer) {
                    stempeln(((RootPaneContainer) w).getRootPane());
                }
            }
        } catch (Throwable t) {
            System.err.println("Filius RBZ: could not switch the look: " + t);
        }
        for (Runnable r : HOERER) {
            try {
                r.run();
            } catch (RuntimeException e) {
                // cosmetic
            }
        }
        for (Window w : Window.getWindows()) {
            w.repaint();
        }
        ausblenden(bilder);
    }

    /**
     * Called when a component is added somewhere. Panels that were not on screen during a switch (e.g. the side bar
     * of the documentation mode or a configuration panel) still have the colours of the old look; they get the new
     * look when they come back.
     */
    public static void beimHinzufuegen(java.awt.Component c) {
        if (!(c instanceof JComponent)) {
            return;
        }
        if (generation == 0) {
            // no switch yet: only remember the look
            if (((JComponent) c).getClientProperty(KEY_GENERATION) == null) {
                ((JComponent) c).putClientProperty(KEY_GENERATION, generation);
            }
            return;
        }
        if (veraltet(c)) {
            javax.swing.SwingUtilities.updateComponentTreeUI(c);
        }
        stempeln(c);
    }

    /** true if the component or one of its children was last shown in an older look */
    private static boolean veraltet(java.awt.Component c) {
        if (c instanceof JComponent) {
            Object g = ((JComponent) c).getClientProperty(KEY_GENERATION);
            if (g instanceof Integer && ((Integer) g).intValue() != generation) {
                return true;
            }
        }
        if (c instanceof java.awt.Container) {
            for (java.awt.Component k : ((java.awt.Container) c).getComponents()) {
                if (veraltet(k)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void stempeln(java.awt.Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).putClientProperty(KEY_GENERATION, generation);
        }
        if (c instanceof java.awt.Container) {
            for (java.awt.Component k : ((java.awt.Container) c).getComponents()) {
                stempeln(k);
            }
        }
    }

    // ------------------------------------------------------------------ cross-fade

    /** picture of the old look on top of a window, fading out */
    private static final class Bild extends JComponent {
        private static final long serialVersionUID = 1L;
        private final BufferedImage img;
        private final double skala;
        private float alpha = 1f;

        Bild(BufferedImage img, double skala) {
            this.img = img;
            this.skala = skala;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
                g2.scale(1 / skala, 1 / skala);
                g2.drawImage(this.img, 0, 0, null);
            } finally {
                g2.dispose();
            }
        }
    }

    private static List<Bild> schnappschuss() {
        List<Bild> liste = new ArrayList<>();
        if (!Prefs.effectsEnabled()) {
            return liste;
        }
        for (Window w : Window.getWindows()) {
            if (!w.isShowing() || !(w instanceof RootPaneContainer)) {
                continue;
            }
            try {
                JLayeredPane lp = ((RootPaneContainer) w).getLayeredPane();
                int b = lp.getWidth();
                int h = lp.getHeight();
                if (b <= 0 || h <= 0) {
                    continue;
                }
                AffineTransform t = w.getGraphicsConfiguration().getDefaultTransform();
                double skala = Math.max(1.0, t.getScaleX());
                BufferedImage img = new BufferedImage((int) Math.ceil(b * skala), (int) Math.ceil(h * skala), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.scale(skala, skala);
                lp.paint(g);
                g.dispose();
                Bild bild = new Bild(img, skala);
                bild.setBounds(0, 0, b, h);
                lp.add(bild, Integer.valueOf(JLayeredPane.DRAG_LAYER + 1));
                liste.add(bild);
            } catch (RuntimeException e) {
                // this window switches without fading
            }
        }
        return liste;
    }

    private static void ausblenden(List<Bild> bilder) {
        if (bilder.isEmpty()) {
            return;
        }
        long start = System.currentTimeMillis();
        Timer t = new Timer(15, null);
        t.addActionListener(e -> {
            float a = 1f - (System.currentTimeMillis() - start) / 320f;
            for (Bild b : bilder) {
                b.alpha = a;
                b.repaint();
            }
            if (a <= 0f) {
                t.stop();
                for (Bild b : bilder) {
                    java.awt.Container p = b.getParent();
                    if (p != null) {
                        p.remove(b);
                        p.repaint();
                    }
                }
            }
        });
        t.start();
    }
}
