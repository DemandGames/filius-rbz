/*
 * Filius (RBZ Wirtschaft Kiel Version): based on InfoDialog of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: new layout in the design of this version
 * (wide enough for the names, text wraps, scrolls on small screens), the editor of Filius (RBZ
 * Wirtschaft Kiel Version) is named. All credits of the original are kept. The info button opens the
 * window only once; a second click closes it again.
 */
package filius.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import filius.modern.HiDpiIcons;
import filius.modern.Texts;
import filius.modern.Theme;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;

public class InfoDialog extends JDialog implements I18n {
    private static final long serialVersionUID = 1L;
    /** Credit for the modified version (Filius (RBZ Wirtschaft Kiel Version)). */
    public static final String EDITED_BY = "Edited by Thorben Demand @ RBZ Wirtschaft Kiel";
    /** editor of Filius (RBZ Wirtschaft Kiel Version) as shown in the info window: name and e-mail */
    public static final String BEARBEITER = "Thorben Demand\nthorben.demand@rbz-wirtschaft-kiel.de";
    /** date of this version (shown after the version number instead of the build date of Filius 2.9.3) */
    public static final String STAND = "21.10.2026";
    private static final int BREITE = 470;

    private static InfoDialog offen;

    /** Opens the window, or closes it if it is already open (the info button). */
    public static void umschalten(Frame owner) {
        if (offen != null && offen.isDisplayable()) {
            offen.dispose();
            offen = null;
            return;
        }
        offen = new InfoDialog(owner);
        offen.setVisible(true);
    }

    public InfoDialog(Frame owner) {
        super(owner);
        this.setIconImages(owner.getIconImages());
        this.setTitle(messages.getString("infodialog_msg1"));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (offen == InfoDialog.this) {
                    offen = null;
                }
            }
        });
        this.getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JPanel wurzel = new JPanel(new BorderLayout());
        wurzel.setBackground(Theme.SURFACE);
        wurzel.add(kopf(), BorderLayout.NORTH);

        Inhalt inhalt = new Inhalt();
        abschnitt(inhalt, "Filius (RBZ Wirtschaft Kiel Version)", Texts.format("info.about", version()), BEARBEITER);
        abschnitt(inhalt, Texts.get("info.original"), ohneHtml(messages.getString("infodialog_msg2")), null);
        abschnitt(inhalt, messages.getString("infodialog_msg3"),
                "André Asschoff, Johannes Bade, Carsten Dittich, Thomas Gerding, Nadja Haßler, Ernst Johannes Klebert, Michell Weyer", null);
        abschnitt(inhalt, messages.getString("infodialog_msg4"), "Stefan Freischlad, Peer Stechert", null);
        abschnitt(inhalt, messages.getString("infodialog_msg11"),
                "Matthias Brauer, Christian Eibl, Christoph Irniger, Detlef Justen, Andreas B. Mundt, Patrice Treton, pyropeter", null);
        abschnitt(inhalt, messages.getString("infodialog_msg5").replaceAll(":\\s*$", "") + " (" + Texts.get("info.original.tag") + ")",
                "Stefan Freischlad", Texts.get("info.website"));
        abschnitt(inhalt, Texts.get("info.licence"), messages.getString("infodialog_msg10"), null);

        JScrollPane scroll = new JScrollPane(inhalt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        wurzel.add(scroll, BorderLayout.CENTER);

        JPanel fuss = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        fuss.setBackground(Theme.SURFACE_2);
        fuss.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE));
        JButton schliessen = new JButton(Texts.get("info.close"));
        schliessen.addActionListener(e -> dispose());
        fuss.add(schliessen);
        wurzel.add(fuss, BorderLayout.SOUTH);
        this.setContentPane(wurzel);
        this.getRootPane().setDefaultButton(schliessen);

        // the height follows the wrapped content at the fixed width, but the window always fits on
        // the screen (it scrolls if the screen is small)
        JComponent kopf = (JComponent) wurzel.getComponent(0);
        this.pack();
        int rahmenB = this.getWidth() - wurzel.getWidth();
        int rahmenH = this.getHeight() - wurzel.getHeight();
        Rectangle bildschirm = owner.getGraphicsConfiguration().getBounds();
        // lay the text out at the final width first, then take the height it needs
        this.setSize(BREITE + rahmenB, 2000);
        this.validate();
        // the text areas know their wrapped height only now: lay out once more with it
        for (Component c : inhalt.getComponents()) {
            c.invalidate();
        }
        inhalt.invalidate();
        this.validate();
        inhalt.invalidate();
        int hoehe = kopf.getPreferredSize().height + inhalt.getPreferredSize().height + fuss.getPreferredSize().height + rahmenH;
        this.setSize(BREITE + rahmenB, Math.max(300, Math.min(hoehe, bildschirm.height - 80)));
        this.setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(() -> {
            inhalt.revalidate();
            scroll.getVerticalScrollBar().setValue(0);
        });
    }

    /** "2.9.3 (21.10.2026)": version of Filius with the date of this version */
    public static String versionMitStand() {
        return version() + " (" + STAND + ")";
    }

    private static String version() {
        String v = Information.getVersion();
        return v == null ? "2.9.3" : v.replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
    }

    private static String ohneHtml(String s) {
        return s.replaceAll("<[^>]+>", "").replace("&auml;", "ä").replace("&ouml;", "ö").replace("&uuml;", "ü").replace("&szlig;", "ß")
                .replace("&eacute;", "é").replace("&nbsp;", " ").trim();
    }

    /** blue head with the logo, the name and the version */
    private JComponent kopf() {
        JPanel kopf = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x2E6BD8), getWidth(), getHeight(), new Color(0x1B4BA6)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        kopf.setLayout(new BorderLayout(16, 0));
        kopf.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JLabel logo = new JLabel(HiDpiIcons.load("/gfx/modern/logo-72.png"));
        logo.setVerticalAlignment(JLabel.CENTER);
        kopf.add(logo, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("Filius");
        name.setFont(Theme.font(Font.BOLD, 26f));
        name.setForeground(Color.WHITE);
        JLabel ausgabe = new JLabel(Texts.get("info.edition"));
        ausgabe.setFont(Theme.font(Font.BOLD, 13f));
        ausgabe.setForeground(new Color(0xDCE8FF));
        JLabel version = new JLabel(messages.getString("infodialog_msg6") + " " + versionMitStand());
        version.setFont(Theme.font(Font.PLAIN, 12.5f));
        version.setForeground(new Color(0xC4D6F7));
        for (JLabel l : new JLabel[] { name, ausgabe, version }) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(l);
            text.add(Box.createVerticalStrut(3));
        }
        JPanel mitte = new JPanel(new java.awt.GridBagLayout());
        mitte.setOpaque(false);
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.weightx = 1;
        mitte.add(text, c);
        kopf.add(mitte, BorderLayout.CENTER);
        return kopf;
    }

    /** heading, wrapping text and an optional second line in grey */
    private static void abschnitt(JPanel inhalt, String titel, String text, String zusatz) {
        JLabel kopf = new JLabel(titel.replaceAll(":\\s*$", ""));
        kopf.setFont(Theme.font(Font.BOLD, 13f));
        kopf.setForeground(Theme.INK);
        kopf.setAlignmentX(Component.LEFT_ALIGNMENT);
        inhalt.add(kopf);
        inhalt.add(Box.createVerticalStrut(4));
        inhalt.add(fliesstext(text, Theme.INK_2));
        if (zusatz != null) {
            inhalt.add(Box.createVerticalStrut(3));
            inhalt.add(fliesstext(zusatz, Theme.INK_3));
        }
        inhalt.add(Box.createVerticalStrut(16));
    }

    private static JTextArea fliesstext(String text, Color farbe) {
        JTextArea t = new JTextArea(text) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setEditable(false);
        t.setFocusable(false);
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder());
        t.setFont(Theme.font(Font.PLAIN, 13f));
        t.setForeground(farbe);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    /** content that follows the width of the window, so the text wraps */
    private static final class Inhalt extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        Inhalt() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createEmptyBorder(18, 22, 4, 22));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
