package filius.modern;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.lang.reflect.Field;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import filius.gui.anwendungssicht.GUIDesktopPanel;

/**
 * Lets the desktop of a simulated PC or laptop grow with its window. Filius lays the desktop out for exactly
 * 640 x 460 pixels (layers with fixed bounds); here every layer follows the size of the window, so the window
 * can be resized at its border and maximized like a normal Windows window. A second click on the network
 * symbol in the lower right corner closes the network window again (a click on the desktop still does, too).
 */
public final class DesktopGroesse {

    private DesktopGroesse() {
    }

    public static void einrichten(GUIDesktopPanel p) {
        JLayeredPane haupt = feld(p, "mainPaneWithOverlay");
        JComponent arbeit = feld(p, "workingLayer");
        JComponent info = feld(p, "infoLayer");
        JComponent netz = feld(p, "networkInfo");
        JComponent dialog = feld(p, "dialogLayer");
        JComponent apps = feld(p, "appPanel");
        JComponent karte = feld(p, "desktopCard");
        if (haupt == null || arbeit == null) {
            return;
        }
        Runnable anpassen = () -> {
            int w = haupt.getWidth();
            int h = haupt.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            arbeit.setBounds(0, 0, w, h);
            arbeit.revalidate();
            if (info != null) {
                info.setBounds(0, 0, w, h);
                if (netz != null) {
                    netz.setLocation(Math.max(0, w - netz.getWidth()), Math.max(0, h - netz.getHeight()));
                }
            }
            if (dialog != null) {
                dialog.setBounds(0, 30, w, h);
                for (Component c : dialog.getComponents()) {
                    if (c instanceof JLabel && c.getY() == 0) {
                        c.setSize(w, c.getHeight()); // title bar of a dialog inside the desktop
                    } else {
                        c.setLocation(Math.max(0, (w - c.getWidth()) / 2), c.getY());
                    }
                }
            }
            haupt.repaint();
        };
        haupt.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                anpassen.run();
            }
        });
        if (dialog != null) {
            dialog.addContainerListener(new ContainerAdapter() {
                @Override
                public void componentAdded(ContainerEvent e) {
                    SwingUtilities.invokeLater(anpassen);
                }
            });
        }
        // the network symbol in the lower right corner opens the network window and also closes it again
        JLabel symbol = feld(p, "networkIcon");
        if (symbol != null && info != null && haupt != null) {
            for (java.awt.event.MouseListener ml : symbol.getMouseListeners()) {
                if (ml.getClass().getName().startsWith(GUIDesktopPanel.class.getName())) {
                    symbol.removeMouseListener(ml);
                }
            }
            symbol.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            symbol.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    boolean offen = haupt.getPosition(info) == 0 && haupt.getLayer(info) == haupt.getLayer(arbeit);
                    try {
                        java.lang.reflect.Method m = GUIDesktopPanel.class.getDeclaredMethod("setInfoVisible", boolean.class);
                        m.setAccessible(true);
                        m.invoke(p, !offen);
                    } catch (ReflectiveOperationException ex) {
                        // the window stays as it is
                    }
                }
            });
        }
        if (karte != null && apps != null) {
            // the icons of the desktop flow over the whole (larger) desktop
            karte.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    apps.setBounds(0, 0, karte.getWidth(), karte.getHeight());
                    apps.revalidate();
                    karte.repaint();
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T feld(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(o);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    /** for container checks in tests */
    static Container haupt(GUIDesktopPanel p) {
        return feld(p, "mainPaneWithOverlay");
    }
}
