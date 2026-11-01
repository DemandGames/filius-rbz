/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.modern;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Small improvements of Filius dialogs that are not replaced as a whole (version 31). Called when a window opens.
 * <ul>
 * <li>DHCP settings (DHCP server and home router): adding or removing a static assignment counts at once. In
 * Filius the list only counted after "OK"; closing the window with the cross brought a removed entry back.</li>
 * <li>Firewall of a router: the default action says which approach it is: "akzeptieren (Blacklist)",
 * "verwerfen (Whitelist)". Only the display changes, the values are those of Filius.</li>
 * <li>Firewall rules and port forwarding: one click into a text cell starts editing (before: double click; the
 * columns with a choice already opened with one click).</li>
 * <li>Every window: when it has been moved to another screen, all windows of Filius are drawn anew (on Windows
 * with two screens the view could be garbled until Filius was minimised and restored).</li>
 * </ul>
 */
public final class Dialoge {

    private static final String KEY = "filius.modern.dialoge";

    private Dialoge() {}

    static void beimOeffnen(Window w) {
        if (!(w instanceof RootPaneContainer)) {
            return;
        }
        JComponent rp = ((RootPaneContainer) w).getRootPane();
        if (rp == null || rp.getClientProperty(KEY) != null) {
            return;
        }
        rp.putClientProperty(KEY, Boolean.TRUE);
        bildschirmWechsel(w);
        String n = w.getClass().getName();
        try {
            if (n.equals("filius.gui.netzwerksicht.JDHCPKonfiguration")) {
                dhcp(w);
            } else if (n.equals("filius.gui.netzwerksicht.JFirewallDialog")) {
                standardaktion(w);
                einfachKlick((JTable) feld(w, "ruleTable"));
            } else if (n.equals("filius.gui.netzwerksicht.JPortForwardingDialog")) {
                einfachKlick((JTable) feld(w, "staticTable"));
            }
        } catch (Throwable t) {
            // an improvement must never break a dialog of Filius
        }
    }

    // ------------------------------------------------------------------ DHCP: static assignments at once

    private static void dhcp(Window w) throws Exception {
        JTable tabelle = (JTable) feld(w, "staticAddressTable");
        Object server = feld(w, "server");
        if (tabelle == null || !(server instanceof filius.software.dhcp.DHCPServer)) {
            return;
        }
        filius.software.dhcp.DHCPServer dhcp = (filius.software.dhcp.DHCPServer) server;
        String hinzu = filius.rahmenprogramm.I18n.messages.getString("jdhcpkonfiguration_msg14");
        String weg = filius.rahmenprogramm.I18n.messages.getString("jdhcpkonfiguration_msg15");
        for (AbstractButton b : knoepfe((Container) w)) {
            if (hinzu.equals(b.getText()) || weg.equals(b.getText())) {
                // the listener of Filius changes the table; afterwards the server gets the list of the table
                b.addActionListener(e -> SwingUtilities.invokeLater(() -> uebernehmen(tabelle, dhcp)));
            }
        }
    }

    private static void uebernehmen(JTable tabelle, filius.software.dhcp.DHCPServer dhcp) {
        TableModel m = tabelle.getModel();
        dhcp.clearStaticAssignments();
        for (int i = 0; i < m.getRowCount(); i++) {
            Object mac = m.getValueAt(i, 0);
            Object ip = m.getValueAt(i, 1);
            if (mac != null && ip != null) {
                dhcp.addStaticAssignment(mac.toString(), ip.toString());
            }
        }
    }

    // ------------------------------------------------------------------ firewall: default action

    private static void standardaktion(Window w) throws Exception {
        Object o = feld(w, "defaultPolicyCombo");
        if (!(o instanceof JComboBox)) {
            return;
        }
        @SuppressWarnings("unchecked")
        JComboBox<Object> combo = (JComboBox<Object>) o;
        String akzeptieren = filius.rahmenprogramm.I18n.messages.getString("jfirewalldialog_msg33");
        String verwerfen = filius.rahmenprogramm.I18n.messages.getString("jfirewalldialog_msg34");
        ListCellRenderer<? super Object> alt = combo.getRenderer();
        combo.setRenderer((list, value, index, selected, focus) -> {
            Component c = alt.getListCellRendererComponent(list, value, index, selected, focus);
            if (c instanceof JLabel && value != null) {
                String v = value.toString();
                if (v.equals(akzeptieren)) {
                    ((JLabel) c).setText(v + " (" + Texts.get("fw.blacklist") + ")");
                } else if (v.equals(verwerfen)) {
                    ((JLabel) c).setText(v + " (" + Texts.get("fw.whitelist") + ")");
                }
            }
            return c;
        });
        combo.setPrototypeDisplayValue(null);
        combo.revalidate();
        Window fenster = SwingUtilities.getWindowAncestor(combo);
        if (fenster != null) {
            fenster.validate();
        }
    }

    // ------------------------------------------------------------------ tables: one click edits a text cell

    private static void einfachKlick(JTable t) {
        if (t == null) {
            return;
        }
        for (Class<?> c : new Class<?>[] { Object.class, String.class }) {
            TableCellEditor e = t.getDefaultEditor(c);
            if (e instanceof DefaultCellEditor && ((DefaultCellEditor) e).getComponent() instanceof javax.swing.JTextField) {
                ((DefaultCellEditor) e).setClickCountToStart(1);
            }
        }
        for (int i = 0; i < t.getColumnModel().getColumnCount(); i++) {
            TableColumn col = t.getColumnModel().getColumn(i);
            TableCellEditor e = col.getCellEditor();
            if (e instanceof DefaultCellEditor && ((DefaultCellEditor) e).getComponent() instanceof javax.swing.JTextField) {
                ((DefaultCellEditor) e).setClickCountToStart(1);
            }
        }
    }

    // ------------------------------------------------------------------ another screen: draw everything anew

    private static void bildschirmWechsel(Window w) {
        final GraphicsDevice[] zuletzt = { geraet(w) };
        Timer neu = new Timer(250, e -> {
            for (Window f : Window.getWindows()) {
                if (f.isShowing()) {
                    f.invalidate();
                    f.validate();
                    f.repaint();
                }
            }
        });
        neu.setRepeats(false);
        w.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                GraphicsDevice jetzt = geraet(w);
                if (jetzt != null && jetzt != zuletzt[0]) {
                    zuletzt[0] = jetzt;
                    neu.restart();
                } else if (neu.isRunning()) {
                    // still moving across the edge: draw when the window has come to rest
                    neu.restart();
                }
            }
        });
        w.addPropertyChangeListener("graphicsConfiguration", e -> neu.restart());
    }

    private static GraphicsDevice geraet(Window w) {
        java.awt.GraphicsConfiguration gc = w.getGraphicsConfiguration();
        return gc == null ? null : gc.getDevice();
    }

    // ------------------------------------------------------------------ helpers

    private static java.util.List<AbstractButton> knoepfe(Container c) {
        java.util.List<AbstractButton> l = new java.util.ArrayList<>();
        for (Component k : c.getComponents()) {
            if (k instanceof AbstractButton) {
                l.add((AbstractButton) k);
            }
            if (k instanceof Container) {
                l.addAll(knoepfe((Container) k));
            }
        }
        return l;
    }

    private static Object feld(Object o, String name) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                // look in the super class
            }
        }
        return null;
    }
}
