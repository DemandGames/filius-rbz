/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.modern;

import java.awt.Component;
import java.awt.Container;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.HierarchyEvent;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * The text editor, the command line and the data exchange open "at the end": when such a window is opened or
 * brought to the front, it shows the last lines (the newest output, the newest messages). A click into a window
 * that is already open does not move it, so the place the student clicked stays where it is.
 */
public final class AnsEnde {

    private static final String KEY = "filius.modern.ansEnde";

    private AnsEnde() {}

    /** an app window of a computer (command line, text editor): to the end whenever it becomes visible */
    public static void beobachte(Container fenster) {
        if (!(fenster instanceof javax.swing.JComponent) || ((javax.swing.JComponent) fenster).getClientProperty(KEY) != null) {
            return;
        }
        ((javax.swing.JComponent) fenster).putClientProperty(KEY, Boolean.TRUE);
        fenster.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && fenster.isShowing()) {
                textAnsEnde(fenster);
            }
        });
    }

    /** the largest scrolled text in the container shows its last line, the caret goes to the end */
    public static void textAnsEnde(Container root) {
        JTextComponent text = groessterText(root);
        if (text == null) {
            return;
        }
        Runnable r = () -> {
            try {
                text.setCaretPosition(text.getDocument().getLength());
            } catch (RuntimeException e) {
                // the document changed in between
            }
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, text);
            if (sp != null) {
                JScrollBar v = sp.getVerticalScrollBar();
                v.setValue(v.getMaximum());
            }
        };
        // after the layout of the window that has just become visible
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(r));
    }

    /** a scroll pane shows its last line */
    public static void scrollAnsEnde(JScrollPane sp) {
        if (sp == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            JScrollBar v = sp.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        }));
    }

    private static JTextComponent groessterText(Container root) {
        JTextComponent beste = null;
        long flaeche = -1;
        for (Component c : root.getComponents()) {
            JTextComponent t = null;
            if (c instanceof JTextComponent && SwingUtilities.getAncestorOfClass(JScrollPane.class, c) != null) {
                t = (JTextComponent) c;
            } else if (c instanceof Container) {
                t = groessterText((Container) c);
            }
            if (t != null) {
                JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, t);
                long f = sp == null ? 0 : (long) sp.getWidth() * sp.getHeight();
                if (f > flaeche) {
                    flaeche = f;
                    beste = t;
                }
            }
        }
        return beste;
    }

    /**
     * A window is brought to the front by the student, not by a click into its content: the mouse is outside the
     * window (task bar, network area, keyboard) or on its title bar.
     */
    public static boolean vonAussen(Window w) {
        try {
            PointerInfo pi = MouseInfo.getPointerInfo();
            if (pi == null) {
                return true;
            }
            Point p = pi.getLocation();
            if (!w.getBounds().contains(p)) {
                return true;
            }
            Container inhalt = w instanceof javax.swing.RootPaneContainer ? ((javax.swing.RootPaneContainer) w).getContentPane() : w;
            Point o = inhalt.getLocationOnScreen();
            return !new Rectangle(o.x, o.y, inhalt.getWidth(), inhalt.getHeight()).contains(p);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
