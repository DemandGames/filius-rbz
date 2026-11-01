package filius.modern;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import filius.gui.GUIContainer;
import filius.gui.JMainFrame;
import filius.gui.anwendungssicht.GUIApplicationWindow;
import filius.gui.anwendungssicht.GUIDesktopPanel;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.hardware.knoten.Host;

/**
 * Global, non-invasive enhancements. A single AWT event listener watches components being added,
 * windows opening and the mouse moving. It
 * <ul>
 * <li>replaces icons by sharp high-DPI versions,</li>
 * <li>restyles a few hard-coded colours of Filius (desktop task bar, app title bars),</li>
 * <li>tracks hover over devices for the hover highlight,</li>
 * <li>reports commands typed into a command line to the learning path.</li>
 * </ul>
 * If the structure of a Filius window is not as expected, nothing happens.
 */
public final class Enhancer implements AWTEventListener {

    private static final String KEY_DONE = "filiusModern.done";
    private static final String KEY_ICON_LISTENER = "filiusModern.iconListener";

    private static boolean mainFrameReady = false;
    private static boolean frameSized = false;
    private static JComponent hovered;

    private Enhancer() {}

    public static void install() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new Enhancer(),
                AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_EVENT_MASK);
    }

    public static boolean isHovered(Component c) {
        return c != null && c == hovered;
    }

    public static JComponent hovered() {
        return hovered;
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        try {
            if (event instanceof ContainerEvent && event.getID() == ContainerEvent.COMPONENT_ADDED) {
                ContainerEvent ce = (ContainerEvent) event;
                if (!frameSized) {
                    sizeMainFrame(ce.getContainer());
                }
                DunkelModus.beimHinzufuegen(ce.getChild());
                process(ce.getChild());
                onAdded(ce.getContainer(), ce.getChild());
            } else if (event instanceof WindowEvent) {
                onWindow((WindowEvent) event);
            } else if (event instanceof MouseEvent && mainFrameReady) {
                onMouse((MouseEvent) event);
            }
        } catch (Throwable t) {
            // an enhancement must never break Filius
        }
    }

    // ------------------------------------------------------------------ components

    /**
     * Colours that Filius sets by hand (plain white backgrounds, black text) become theme colours, which are the
     * same in the light mode and follow the dark mode. Web pages keep their own colours.
     */
    private static void themeFarben(Component c) {
        if (c instanceof javax.swing.JComponent && Boolean.TRUE.equals(((javax.swing.JComponent) c).getClientProperty("filius.eigeneFarben"))) {
            return;
        }
        if (c instanceof javax.swing.JEditorPane && "text/html".equals(((javax.swing.JEditorPane) c).getContentType())) {
            return;
        }
        Color bg = c.getBackground();
        if (bg != null && !(bg instanceof ThemeFarbe) && !(bg instanceof javax.swing.plaf.UIResource) && bg.getAlpha() == 255) {
            int rgb = bg.getRGB() & 0xFFFFFF;
            if (rgb == 0xFFFFFF) {
                c.setBackground(Theme.SURFACE);
            } else if (rgb == 0xEEEEEE || rgb == 0xF0F0F0 || rgb == 0xF5F5F5 || rgb == 0xFAFAFA) {
                c.setBackground(Theme.SURFACE_2);
            }
        }
        if (c instanceof javax.swing.JTabbedPane) {
            // tabs that Filius colours light grey by hand (e.g. firewall dialog) get the colours of the theme
            javax.swing.JTabbedPane tp = (javax.swing.JTabbedPane) c;
            for (int i = 0; i < tp.getTabCount(); i++) {
                Color t = tp.getBackgroundAt(i);
                if (t != null && !(t instanceof ThemeFarbe) && !(t instanceof javax.swing.plaf.UIResource) && hellGrau(t)) {
                    tp.setBackgroundAt(i, null);
                }
            }
        }
        Color fg = c.getForeground();
        if (fg != null && !(fg instanceof ThemeFarbe) && !(fg instanceof javax.swing.plaf.UIResource) && !Color.BLACK.equals(bg)) {
            int r = fg.getRed();
            if (r == fg.getGreen() && r == fg.getBlue() && r < 0x80 && fg.getAlpha() == 255) {
                // black and dark grey text of Filius
                c.setForeground(r == 0 ? Theme.INK : Theme.INK_2);
            }
        }
    }

    private static boolean hellGrau(Color c) {
        int rgb = c.getRGB() & 0xFFFFFF;
        return rgb == 0xFFFFFF || rgb == 0xEEEEEE || rgb == 0xF0F0F0 || rgb == 0xF5F5F5 || rgb == 0xFAFAFA;
    }

    private static void process(Component c) {
        themeFarben(c);
        if (c instanceof JLabel || c instanceof AbstractButton) {
            upgradeIcon((JComponent) c);
        }
        if (c instanceof javax.swing.JTable && Color.DARK_GRAY.equals(c.getBackground())) {
            // data exchange table: the free area below the rows was dark grey
            c.setBackground(Theme.SURFACE);
        }
        if (c instanceof AbstractButton) {
            remapButtonColors((AbstractButton) c);
        }
        if (c instanceof JTextField) {
            ungueltigBehalten((JTextField) c);
        }
        if (c instanceof JLabel && ((JLabel) c).isOpaque() && MODAL_TITLE_GREY.equals(c.getBackground())) {
            // title bar of dialogs inside a simulated desktop
            c.setBackground(Theme.CHROME);
            c.setFont(Theme.font(Font.BOLD, 13f));
        }
        if (c instanceof GUIApplicationWindow) {
            styleAppWindow((GUIApplicationWindow) c);
        } else if (c instanceof GUIDesktopPanel) {
            styleDesktop((GUIDesktopPanel) c);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                process(child);
            }
        }
    }

    private static final Color MODAL_TITLE_GREY = new Color(100, 100, 100);
    private static final String KEY_UNGUELTIG = "filius.ungueltigBehalten";

    /**
     * An invalid entry stays in its field, written in red (version 25). Filius colours an entry red while it is
     * typed, but when the field loses the focus (or Enter is pressed) it puts the stored value back, so the
     * mistake disappeared. Now the red entry stays until it is corrected; the device keeps its old value until
     * then.
     */
    private static void ungueltigBehalten(JTextField f) {
        if (f.getClientProperty(KEY_UNGUELTIG) != null) {
            return;
        }
        f.putClientProperty(KEY_UNGUELTIG, Boolean.TRUE);
        // the invalid text typed last (null = the entry is valid); the listeners of Filius were added before
        // these, so they run first: Filius puts the old value back, then the invalid entry is restored
        final String[] merk = { null };
        final String[] enter = { null };
        f.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    enter[0] = istUngueltig(f) ? f.getText() : null;
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (enter[0] != null) {
                    wiederherstellen(f, enter[0]);
                    enter[0] = null;
                }
                merk[0] = istUngueltig(f) ? f.getText() : null;
            }
        });
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (merk[0] != null && !merk[0].equals(f.getText())) {
                    // the field shows something else now (e.g. another device): forget the old entry
                    merk[0] = null;
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (merk[0] != null) {
                    wiederherstellen(f, merk[0]);
                }
            }
        });
    }

    private static boolean istUngueltig(JTextField f) {
        Color fg = f.getForeground();
        return fg != null && fg.getRGB() == filius.rahmenprogramm.EingabenUeberpruefung.farbeFalsch.getRGB();
    }

    private static void wiederherstellen(JTextField f, String text) {
        if (!text.equals(f.getText())) {
            f.setText(text);
        }
        f.setForeground(filius.rahmenprogramm.EingabenUeberpruefung.farbeFalsch);
    }
    private static final Color CONNECTED_GREEN = new Color(0x8FE0B4);
    private static final String KEY_BG_LISTENER = "filiusModern.bgListener";

    /** "Connected" buttons (e.g. network ports in the connections dialog) were pure #00FF00. */
    private static void remapButtonColors(AbstractButton b) {
        if (Color.GREEN.equals(b.getBackground())) {
            b.setBackground(CONNECTED_GREEN);
        }
        if (b.getClientProperty(KEY_BG_LISTENER) == null) {
            b.putClientProperty(KEY_BG_LISTENER, Boolean.TRUE);
            b.addPropertyChangeListener("background", e -> {
                if (Color.GREEN.equals(e.getNewValue())) {
                    SwingUtilities.invokeLater(() -> {
                        if (Color.GREEN.equals(b.getBackground())) {
                            b.setBackground(CONNECTED_GREEN);
                        }
                    });
                }
            });
        }
    }

    private static void upgradeIcon(JComponent c) {
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            Icon up = HiDpiIcons.upgrade(l.getIcon());
            if (up != l.getIcon()) {
                l.setIcon(up);
            }
        } else {
            AbstractButton b = (AbstractButton) c;
            Icon up = HiDpiIcons.upgrade(b.getIcon());
            if (up != b.getIcon()) {
                b.setIcon(up);
            }
        }
        if (c.getClientProperty(KEY_ICON_LISTENER) == null) {
            c.putClientProperty(KEY_ICON_LISTENER, Boolean.TRUE);
            c.addPropertyChangeListener("icon", e -> {
                Icon now = (Icon) e.getNewValue();
                Icon up = HiDpiIcons.upgrade(now);
                if (up != now) {
                    SwingUtilities.invokeLater(() -> {
                        if (c instanceof JLabel && ((JLabel) c).getIcon() == now) {
                            ((JLabel) c).setIcon(up);
                        } else if (c instanceof AbstractButton && ((AbstractButton) c).getIcon() == now) {
                            ((AbstractButton) c).setIcon(up);
                        }
                    });
                }
            });
        }
    }

    private static void onAdded(Container parent, Component child) {
        // nothing to do at the moment
    }

    /** App window inside a simulated desktop: modern title bar and command line hooks. */
    private static void styleAppWindow(GUIApplicationWindow w) {
        if (w.getClientProperty(KEY_DONE) != null) {
            return;
        }
        w.putClientProperty(KEY_DONE, Boolean.TRUE);
        LayoutManager lm = w.getLayout();
        if (lm instanceof BorderLayout) {
            Component north = ((BorderLayout) lm).getLayoutComponent(BorderLayout.NORTH);
            if (north instanceof JPanel && Color.DARK_GRAY.equals(north.getBackground())) {
                north.setBackground(Theme.CHROME);
                for (Component c : ((JPanel) north).getComponents()) {
                    if (c instanceof JLabel && ((JLabel) c).getIcon() == null) {
                        c.setFont(Theme.font(Font.BOLD, 13f));
                        c.setForeground(Color.WHITE);
                    }
                }
            }
        }
        String cls = w.getClass().getSimpleName();
        if (cls.contains("Terminal") || cls.contains("TextEditor")) {
            // opened or brought to the front: the last lines are in view
            AnsEnde.beobachte(w);
        }
        if (cls.contains("Terminal")) {
            styleTerminal(w);
            for (JTextField t : find(w, JTextField.class)) {
                watchCommands(w, t);
            }
        }
        // the window of a computer can be enlarged: one-line input fields keep their height instead of
        // stretching (Filius lays them out in boxes that grow with the window); after the fonts are set
        for (JTextField t : find(w, JTextField.class)) {
            java.awt.Dimension max = t.getMaximumSize();
            if (max.height > t.getPreferredSize().height) {
                t.setMaximumSize(new java.awt.Dimension(max.width, t.getPreferredSize().height));
            }
        }
        for (javax.swing.JComboBox<?> b : find(w, javax.swing.JComboBox.class)) {
            b.setMaximumSize(new java.awt.Dimension(b.getMaximumSize().width, b.getPreferredSize().height));
        }
    }

    private static final Color TERMINAL_BG = new Color(0x0E1620);
    private static final Color TERMINAL_FG = new Color(0xDCE6F0);

    /** Command line: deep slate instead of pure black, JetBrains Mono, LED-green prompt and caret. */
    private static void styleTerminal(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JPanel && Theme.CHROME.equals(c.getBackground())) {
                continue; // title bar
            }
            if (Color.BLACK.equals(c.getBackground())) {
                c.setBackground(TERMINAL_BG);
            }
            if (c instanceof JTextComponent) {
                JTextComponent t = (JTextComponent) c;
                t.setFont(Theme.mono(Font.PLAIN, 12.5f));
                t.setForeground(TERMINAL_FG);
                t.setCaretColor(Theme.LED_GREEN_BRIGHT);
                t.setSelectionColor(new Color(0x2E6BD8));
                t.setSelectedTextColor(Color.WHITE);
                // Filius sets no border (null); a switch of the look would then install the rounded border of the
                // theme, which made the input line too low and cut the text. An own empty border stays.
                t.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
            } else if (c instanceof JLabel && ((JLabel) c).getIcon() == null) {
                c.setFont(Theme.mono(Font.BOLD, 12.5f));
                c.setForeground(Theme.LED_GREEN_BRIGHT);
            } else if (c instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) c;
                sp.getViewport().setBackground(TERMINAL_BG);
                sp.setBackground(TERMINAL_BG);
                for (javax.swing.JScrollBar bar : new javax.swing.JScrollBar[] { sp.getVerticalScrollBar(), sp.getHorizontalScrollBar() }) {
                    if (bar != null) {
                        bar.setBackground(TERMINAL_BG);
                        bar.putClientProperty("FlatLaf.style", "track: #0E1620; thumb: #33475B; hoverThumbColor: #4A5B6C; pressedThumbColor: #5B6B7C");
                    }
                }
            }
            if (c instanceof Container) {
                styleTerminal((Container) c);
            }
        }
    }

    private static void styleDesktop(GUIDesktopPanel desktop) {
        if (desktop.getClientProperty(KEY_DONE) != null) {
            return;
        }
        desktop.putClientProperty(KEY_DONE, Boolean.TRUE);
        LayoutManager lm = desktop.getLayout();
        if (!(lm instanceof BorderLayout)) {
            return;
        }
        Component south = ((BorderLayout) lm).getLayoutComponent(BorderLayout.SOUTH);
        if (south instanceof JPanel) {
            JPanel taskbar = (JPanel) south;
            taskbar.setBackground(new Color(0x16222E));
            taskbar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x2B3B4B)));
            for (Component c : taskbar.getComponents()) {
                if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    b.setForeground(Color.WHITE);
                    b.setFont(Theme.font(Font.BOLD, 13f));
                    b.putClientProperty("JButton.buttonType", "borderless");
                    b.setFocusPainted(false);
                    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> find(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                result.add((T) c);
            }
            if (c instanceof Container) {
                result.addAll(find((Container) c, type));
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ command line hooks

    /**
     * Commands typed into a command line are reported to the learning path, and "tracert" (the
     * Windows name) is accepted for "traceroute". The listener has to run before the one of the
     * command line, which clears the input field, so the existing listeners are added again after it.
     */
    private static void watchCommands(GUIApplicationWindow window, JTextField input) {
        if (input.getClientProperty(KEY_DONE) != null) {
            return;
        }
        input.putClientProperty(KEY_DONE, Boolean.TRUE);
        java.awt.event.KeyListener[] existing = input.getKeyListeners();
        for (java.awt.event.KeyListener l : existing) {
            input.removeKeyListener(l);
        }
        input.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER) {
                    return;
                }
                String text = input.getText().trim();
                if (text.isEmpty()) {
                    return;
                }
                if (text.matches("(?i)tracert(\\s.*)?")) {
                    text = "traceroute" + text.substring(7);
                    input.setText(text);
                }
                try {
                    String[] parts = text.split("\\s+");
                    filius.hardware.knoten.Knoten node = window.holeAnwendung().getSystemSoftware().getKnoten();
                    filius.modern.lernpfad.Lernpfad.befehl(node, parts[0], java.util.Arrays.asList(parts).subList(1, parts.length));
                } catch (RuntimeException ex) {
                    // reporting must never break the command line
                }
            }
        });
        for (java.awt.event.KeyListener l : existing) {
            input.addKeyListener(l);
        }
    }

    // ------------------------------------------------------------------ windows

    private static void onWindow(WindowEvent e) {
        Window w = e.getWindow();
        int id = e.getID();
        if (id != WindowEvent.WINDOW_OPENED && id != WindowEvent.WINDOW_ACTIVATED) {
            return;
        }
        if (w instanceof JMainFrame && !mainFrameReady) {
            mainFrameReady = true;
            onMainFrame((JMainFrame) w);
        }
        if (id == WindowEvent.WINDOW_OPENED && w instanceof javax.swing.RootPaneContainer) {
            // dialogs that colour their content pane after it was added (e.g. port forwarding)
            themeFarben(((javax.swing.RootPaneContainer) w).getContentPane());
            // DHCP list at once, firewall default action, one-click editing, redraw on another screen (version 31)
            Dialoge.beimOeffnen(w);
        }
        if (!w.isVisible()) {
            return;
        }
    }

    private static void onMainFrame(JMainFrame frame) {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[] { 16, 24, 32, 48, 64, 128, 256 }) {
            java.net.URL url = Enhancer.class.getResource("/gfx/modern/app/filius-" + size + ".png");
            if (url != null) {
                icons.add(new ImageIcon(url).getImage());
            }
        }
        if (!icons.isEmpty()) {
            frame.setIconImages(icons);
        }
        Overlay.install(frame.getRootPane());
        filius.modern.lernpfad.Lernpfad.install(frame);
        // window title with the name of this version ("FILIUS - file" becomes "Filius (RBZ ...) - file")
        frame.addPropertyChangeListener("title", e -> retitle(frame));
        retitle(frame);
        // flat scroll areas around canvas and sidebar
        for (JScrollPane sp : find(frame.getContentPane(), JScrollPane.class)) {
            flatten(sp);
        }
        flatten(GUIContainer.getGUIContainer().getScrollPane());
        flatten((JScrollPane) GUIContainer.getGUIContainer().getSimpanel());
        JScrollPane sidebar = GUIContainer.getGUIContainer().getSidebarScrollpane();
        flatten(sidebar);
        // the component bar may be taller than small windows: allow scrolling
        sidebar.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebar.getVerticalScrollBar().setUnitIncrement(16);
        // canvas as large as the screen, no white edges
        Leinwand.einrichten();
    }

    public static final String PRODUCT = "Filius (RBZ Wirtschaft Kiel Version)";

    private static void retitle(JMainFrame frame) {
        String t = frame.getTitle();
        if (t != null && t.startsWith("FILIUS")) {
            frame.setTitle(PRODUCT + t.substring(6));
        }
    }

    /**
     * Filius opens with 1020 x 700 pixels. On today's screens the modern layout gets more room:
     * up to 1360 x 860, never more than 92 % of the screen. Saved window sizes above 1040 px stay.
     */
    private static void sizeMainFrame(Container c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        if (!(w instanceof JMainFrame) || w.isVisible()) {
            return;
        }
        frameSized = true;
        java.awt.Rectangle b = w.getBounds();
        java.awt.Rectangle screen = w.getGraphicsConfiguration().getBounds();
        if (b.width > 1040 || screen.width < 1100) {
            return;
        }
        int width = Math.min(1360, (int) (screen.width * 0.92));
        int height = Math.min(860, (int) (screen.height * 0.9));
        w.setBounds(new java.awt.Rectangle(screen.x + (screen.width - width) / 2, screen.y + (screen.height - height) / 2, width, height));
    }

    private static void flatten(JScrollPane sp) {
        if (sp != null) {
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setViewportBorder(null);
        }
    }

    // ------------------------------------------------------------------ hover

    private static void onMouse(MouseEvent e) {
        int id = e.getID();
        if (id != MouseEvent.MOUSE_MOVED && id != MouseEvent.MOUSE_EXITED && id != MouseEvent.MOUSE_DRAGGED) {
            return;
        }
        Component src = e.getComponent();
        if (src == null || !(SwingUtilities.getWindowAncestor(src) instanceof JMainFrame) && !(src instanceof JMainFrame)) {
            return;
        }
        JComponent target = null;
        if (id != MouseEvent.MOUSE_EXITED || src instanceof JMainFrame) {
            Point screen = e.getLocationOnScreen();
            GUIContainer gc = GUIContainer.getGUIContainer();
            if (gc.getActiveSite() != Theme.MODE_DOCUMENTATION) {
                for (GUIKnotenItem item : gc.getKnotenItems()) {
                    JComponent label = item.getImageLabel();
                    if (hit(label, screen)) {
                        target = label;
                    }
                }
            }
            if (target == null && gc.getActiveSite() == Theme.MODE_DESIGN) {
                for (Component c : gc.getSidebar().getLeistenpanel().getComponents()) {
                    if (c instanceof JComponent && ((JComponent) c).getClientProperty("filiusModern.sidebarItem") != null
                            && hit((JComponent) c, screen)) {
                        target = (JComponent) c;
                    }
                }
            }
            if (id == MouseEvent.MOUSE_EXITED) {
                target = null;
            }
            // in the simulation a computer can be clicked to open its desktop
            if (gc.getActiveSite() == Theme.MODE_SIMULATION) {
                Component panel = gc.getDesignpanel();
                boolean clickable = false;
                if (target != null) {
                    for (GUIKnotenItem item : gc.getKnotenItems()) {
                        if (item.getImageLabel() == target && item.getKnoten() instanceof Host) {
                            clickable = true;
                        }
                    }
                }
                panel.setCursor(clickable ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        }
        if (target != hovered) {
            JComponent old = hovered;
            hovered = target;
            repaintHover(old);
            repaintHover(target);
        }
    }

    private static void repaintHover(JComponent c) {
        if (c != null) {
            c.repaint();
            if (c.getParent() != null) {
                Container p = c.getParent();
                p.repaint(0, c.getY() - 4, p.getWidth(), c.getHeight() + 8);
            }
        }
    }

    private static boolean hit(JComponent c, Point screen) {
        if (c == null || !c.isShowing()) {
            return false;
        }
        Point p = new Point(screen);
        SwingUtilities.convertPointFromScreen(p, c);
        return p.x >= 0 && p.y >= 0 && p.x < c.getWidth() && p.y < c.getHeight();
    }
}
