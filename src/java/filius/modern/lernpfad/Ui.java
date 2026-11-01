package filius.modern.lernpfad;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import filius.modern.Theme;

/** Small building blocks for the learning path panel. */
final class Ui {

    static final Color BLAU_HELL = Theme.farbe(0xEAF1FD, 0x1C2E4A);
    static final Color GRUEN_HELL = Theme.farbe(0xE6F6EE, 0x163327);
    static final Color AMBER_HELL = Theme.farbe(0xFDF4E3, 0x3A2E14);
    static final Color ROT_HELL = Theme.farbe(0xFCEAEA, 0x3D1E22);

    private Ui() {}

    static JLabel label(String text, float groesse, boolean fett, Color farbe) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(fett ? Font.BOLD : Font.PLAIN, groesse));
        l.setForeground(farbe);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** vertical panel that keeps its children left-aligned */
    static JPanel spalte() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static Component abstand(int h) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, h));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        p.setMinimumSize(new Dimension(1, h));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static <T extends JComponent> T links(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** a row (BorderLayout) whose height in a vertical box follows its wrapping content */
    static final class Zeile extends JPanel {
        private static final long serialVersionUID = 1L;

        Zeile(int abstand) {
            super(new BorderLayout(abstand, 0));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /**
     * A released left mouse button inside the component. Used instead of mouseClicked, which Java
     * drops when the mouse moved slightly between pressing and releasing (touchpads).
     */
    static boolean klick(java.awt.event.MouseEvent e) {
        Component c = e.getComponent();
        return SwingUtilities.isLeftMouseButton(e) && c != null && e.getX() >= 0 && e.getY() >= 0 && e.getX() < c.getWidth()
                && e.getY() < c.getHeight();
    }

    /** limits the height of a component in a vertical box to its preferred height */
    static <T extends JComponent> T flach(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    // ------------------------------------------------------------------ wrapping text with markup

    private static final Pattern MARKUP = Pattern.compile("\\*\\*(.+?)\\*\\*|`(.+?)`");

    /** Read-only, wrapping text. **bold** and `code` are formatted. */
    static final class Text extends JTextPane {
        private static final long serialVersionUID = 1L;

        Text(String markup, float groesse, Color farbe) {
            this(markup, groesse, farbe, false);
        }

        Text(String markup, float groesse, Color farbe, boolean fett) {
            this(markup, groesse, farbe, fett, false);
        }

        /** mono: the whole text in the font for addresses (table cells with IP addresses) */
        Text(String markup, float groesse, Color farbe, boolean fett, boolean mono) {
            setEditable(false);
            setFocusable(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new Insets(0, 0, 0, 0));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setHighlighter(null);
            StyledDocument doc = getStyledDocument();
            SimpleAttributeSet normal = new SimpleAttributeSet();
            StyleConstants.setFontFamily(normal, mono ? Theme.mono(Font.PLAIN, groesse).getFamily() : Theme.font(Font.PLAIN, groesse).getFamily());
            StyleConstants.setFontSize(normal, Math.round(mono ? groesse - 0.5f : groesse));
            StyleConstants.setForeground(normal, farbe);
            StyleConstants.setBold(normal, fett);
            StyleConstants.setLineSpacing(normal, 0.12f);
            SimpleAttributeSet bold = new SimpleAttributeSet(normal);
            StyleConstants.setBold(bold, true);
            StyleConstants.setForeground(bold, Theme.INK);
            SimpleAttributeSet code = new SimpleAttributeSet(normal);
            StyleConstants.setFontFamily(code, Theme.mono(Font.PLAIN, groesse).getFamily());
            StyleConstants.setFontSize(code, Math.round(groesse - 1));
            StyleConstants.setBackground(code, Theme.farbe(0xEEF2F6, 0x223040));
            StyleConstants.setForeground(code, Theme.farbe(0x1F3A5F, 0x9CC2FF));
            SimpleAttributeSet rand = new SimpleAttributeSet(code);
            StyleConstants.setFontFamily(rand, Theme.font(Font.PLAIN, groesse).getFamily());
            StyleConstants.setFontSize(rand, Math.max(5, Math.round(groesse * 0.4f)));
            try {
                Matcher m = MARKUP.matcher(markup == null ? "" : markup);
                int pos = 0;
                while (m.find()) {
                    einfuegen(doc, markup.substring(pos, m.start()), normal);
                    if (m.group(1) != null) {
                        einfuegen(doc, m.group(1), bold);
                    } else {
                        // a narrow space in the normal font on both sides (the fonts have no thin space)
                        doc.insertString(doc.getLength(), " ", rand);
                        einfuegen(doc, m.group(2), code);
                        doc.insertString(doc.getLength(), " ", rand);
                    }
                    pos = m.end();
                }
                einfuegen(doc, markup == null ? "" : markup.substring(pos), normal);
                doc.setParagraphAttributes(0, doc.getLength(), normal, false);
            } catch (BadLocationException e) {
                // cannot happen
            }
        }

        /**
         * Inserts text; characters that the font of the text has no glyph for (for example the arrow
         * →) are set in the logical font "Dialog", which finds them in the fonts of the system.
         * Otherwise they would appear as empty boxes.
         */
        private static void einfuegen(StyledDocument doc, String text, SimpleAttributeSet attr) throws BadLocationException {
            if (text.isEmpty()) {
                return;
            }
            Font f = new Font(StyleConstants.getFontFamily(attr), StyleConstants.isBold(attr) ? Font.BOLD : Font.PLAIN, StyleConstants.getFontSize(attr));
            SimpleAttributeSet ersatz = null;
            int start = 0;
            boolean anzeigbar = f.canDisplay(text.codePointAt(0));
            for (int i = 0; i <= text.length(); i += i < text.length() ? Character.charCount(text.codePointAt(i)) : 1) {
                boolean jetzt = i < text.length() && f.canDisplay(text.codePointAt(i));
                if (i == text.length() || jetzt != anzeigbar) {
                    if (i > start) {
                        if (!anzeigbar && ersatz == null) {
                            ersatz = new SimpleAttributeSet(attr);
                            StyleConstants.setFontFamily(ersatz, Font.DIALOG);
                        }
                        doc.insertString(doc.getLength(), text.substring(start, i), anzeigbar ? attr : ersatz);
                    }
                    start = i;
                    anzeigbar = jetzt;
                }
            }
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        /**
         * Transparent for the mouse: a click on the text of a chapter card or an answer reaches the
         * card or answer around it (a text pane would otherwise keep the click for its caret), and the
         * mouse wheel scrolls the page.
         */
        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        /**
         * The height of wrapped text depends on the width, which is only known after the first
         * layout. When the width changes, the layout runs again with the new height (otherwise
         * the last lines of text in a list row could be cut off).
         */
        @Override
        public void setBounds(int x, int y, int w, int h) {
            int alt = getWidth();
            super.setBounds(x, y, w, h);
            if (w != alt && w > 0) {
                SwingUtilities.invokeLater(() -> {
                    if (getPreferredSize().height != getHeight()) {
                        revalidate();
                        repaint();
                    }
                });
            }
        }
    }

    // ------------------------------------------------------------------ containers

    /** Rounded card with optional coloured left edge. */
    static class Karte extends JPanel {
        private static final long serialVersionUID = 1L;
        Color hintergrund;
        Color rand;
        Color kante;

        Karte(Color hintergrund, Color rand) {
            this.hintergrund = hintergrund;
            this.rand = rand;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 14, 14);
                if (hintergrund != null) {
                    g2.setColor(hintergrund);
                    g2.fill(r);
                }
                if (rand != null) {
                    g2.setColor(rand);
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(r);
                }
                if (kante != null) {
                    g2.setClip(r);
                    g2.setColor(kante);
                    g2.fillRect(0, 0, 4, getHeight());
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Panel inside a scroll pane that follows the width of the viewport, so text wraps. */
    static final class Inhalt extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        Inhalt() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createEmptyBorder(14, 18, 22, 18));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height - 40;
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

    // ------------------------------------------------------------------ tables

    /** cell text that is only addresses, masks and ranges: shown in the font for addresses */
    private static final Pattern ADRESSE = Pattern.compile("[0-9./:\\s–—-]+|[–—-]");

    /**
     * A small table inside a task or a reading page ("| a | b |" lines, the first row is the header).
     * Columns that only hold addresses keep their width; the other columns wrap their text.
     */
    static final class Tabelle extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int PAD_X = 8;
        private static final int PAD_Y = 5;
        private static final float GROESSE = 12.5f;

        private final int spalten;
        private final Text[][] zellen;
        private final boolean[] mono;
        private final int[] einzeilig;
        /** width of the longest word of a column: narrower, a word would be cut */
        private final int[] wort;
        private int[] zeilenY = new int[0];
        private int kopfHoehe;

        Tabelle(java.util.List<java.util.List<String>> zeilen) {
            setLayout(null);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            int n = 0;
            for (java.util.List<String> z : zeilen) {
                n = Math.max(n, z.size());
            }
            spalten = n;
            mono = new boolean[n];
            for (int c = 0; c < n; c++) {
                boolean alle = zeilen.size() > 1;
                for (int r = 1; r < zeilen.size(); r++) {
                    String t = c < zeilen.get(r).size() ? zeilen.get(r).get(c) : "";
                    if (!t.isEmpty() && !ADRESSE.matcher(t).matches()) {
                        alle = false;
                    }
                }
                mono[c] = alle;
            }
            zellen = new Text[zeilen.size()][n];
            einzeilig = new int[n];
            wort = new int[n];
            for (int r = 0; r < zeilen.size(); r++) {
                for (int c = 0; c < n; c++) {
                    String t = c < zeilen.get(r).size() ? zeilen.get(r).get(c) : "";
                    boolean kopf = r == 0;
                    // addresses in the font for addresses, also in a column with other values
                    boolean adresse = !kopf && !t.isEmpty() && ADRESSE.matcher(t).matches() && t.matches(".*\\d.*");
                    Text text = new Text(t, kopf ? 11.5f : GROESSE, kopf ? Theme.INK_2 : Theme.INK, kopf, adresse);
                    zellen[r][c] = text;
                    add(text);
                    Font f = kopf ? Theme.font(Font.BOLD, 11.5f) : adresse ? Theme.mono(Font.PLAIN, GROESSE - 0.5f) : Theme.font(Font.PLAIN, GROESSE);
                    String klar = t.replace("**", "").replace("`", "");
                    FontMetrics fm = getFontMetrics(f);
                    einzeilig[c] = Math.max(einzeilig[c], fm.stringWidth(klar) + 3);
                    for (String teil : (adresse ? new String[] { klar } : klar.split("\\s+"))) {
                        wort[c] = Math.max(wort[c], fm.stringWidth(teil) + 3);
                    }
                }
            }
        }

        /** widths of the columns (without padding) for the given inner width */
        private int[] breiten(int innen) {
            int[] w = new int[spalten];
            int platz = innen - spalten * 2 * PAD_X;
            int summe = 0;
            for (int c = 0; c < spalten; c++) {
                summe += einzeilig[c];
            }
            if (summe <= platz) {
                System.arraycopy(einzeilig, 0, w, 0, spalten);
                // the rest goes to the first column, so the table fills the width
                w[0] += platz - summe;
                return w;
            }
            // address columns keep their width, the other columns start with their longest word and
            // share the rest as they need it
            int belegt = 0;
            int wunsch = 0;
            for (int c = 0; c < spalten; c++) {
                w[c] = mono[c] ? einzeilig[c] : Math.min(einzeilig[c], wort[c]);
                belegt += w[c];
                wunsch += einzeilig[c] - w[c];
            }
            int rest = platz - belegt;
            if (rest < 0) {
                // too narrow even for the words: all columns shrink alike
                for (int c = 0; c < spalten; c++) {
                    w[c] = Math.max(20, (int) ((long) w[c] * platz / Math.max(1, belegt)));
                }
                return w;
            }
            for (int c = 0; c < spalten; c++) {
                if (wunsch > 0) {
                    w[c] += (int) ((long) rest * (einzeilig[c] - w[c]) / wunsch);
                }
            }
            return w;
        }

        private static int hoehe(Text t, int breite) {
            javax.swing.text.View v = t.getUI().getRootView(t);
            // a different width first, so the lines are wrapped again with the current font metrics
            // (they differ slightly before the table is on the screen); one pixel less than the cell,
            // so a line is never cut off
            v.setSize(Math.max(1, breite + 7), Integer.MAX_VALUE / 4);
            v.setSize(Math.max(1, breite - 1), Integer.MAX_VALUE / 4);
            return (int) Math.ceil(v.getPreferredSpan(javax.swing.text.View.Y_AXIS));
        }

        /** lays the cells out for this width (or only measures); returns the height */
        private int anordnen(int breite, boolean setzen) {
            int[] w = breiten(breite - 2);
            int y = 1;
            int[] ys = new int[zellen.length + 1];
            for (int r = 0; r < zellen.length; r++) {
                ys[r] = y;
                int h = 0;
                for (int c = 0; c < spalten; c++) {
                    h = Math.max(h, hoehe(zellen[r][c], w[c]));
                }
                if (setzen) {
                    int x = 1;
                    for (int c = 0; c < spalten; c++) {
                        zellen[r][c].setBounds(x + PAD_X, y + PAD_Y, w[c], h);
                        x += w[c] + 2 * PAD_X;
                    }
                }
                y += h + 2 * PAD_Y;
                if (r == 0) {
                    kopfHoehe = y;
                }
            }
            ys[zellen.length] = y;
            if (setzen) {
                zeilenY = ys;
            }
            return y + 1;
        }

        @Override
        public Dimension getPreferredSize() {
            int summe = 2;
            for (int c = 0; c < spalten; c++) {
                summe += einzeilig[c] + 2 * PAD_X;
            }
            int breite = getWidth() > 0 ? getWidth() : summe;
            return new Dimension(Math.min(summe, breite), anordnen(breite, false));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(100, getPreferredSize().height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        public void doLayout() {
            anordnen(getWidth(), true);
        }

        @Override
        public void setBounds(int x, int y, int w, int h) {
            int alt = getWidth();
            super.setBounds(x, y, w, h);
            if (w != alt && w > 0) {
                SwingUtilities.invokeLater(() -> {
                    if (getPreferredSize().height != getHeight()) {
                        revalidate();
                        repaint();
                    }
                });
            }
        }

        @Override
        public boolean contains(int x, int y) {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(Theme.SURFACE);
                g2.fill(r);
                g2.setClip(r);
                g2.setColor(Theme.farbe(0xF1F4F9, 0x1E2A36));
                g2.fillRect(0, 0, getWidth(), kopfHoehe);
                g2.setClip(null);
                g2.setColor(Theme.LINE);
                for (int i = 1; i < zeilenY.length - 1; i++) {
                    g2.drawLine(1, zeilenY[i], getWidth() - 2, zeilenY[i]);
                }
                g2.setStroke(new BasicStroke(1f));
                g2.draw(r);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ buttons

    static JButton knopf(String text, boolean primaer) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 13f));
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primaer) {
            b.putClientProperty("FlatLaf.style", "background: #2E6BD8; foreground: #FFFFFF; borderWidth: 0; arc: 10; "
                    + "hoverBackground: #2860C4; pressedBackground: #2255B0; margin: 6,14,6,14");
        } else {
            b.putClientProperty("FlatLaf.style", "arc: 10; margin: 6,12,6,12");
        }
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    static JButton link(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.font(Font.BOLD, 12.5f));
        b.setForeground(Theme.PATCH_BLUE);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "borderless");
        b.setMargin(new Insets(2, 2, 2, 2));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    // ------------------------------------------------------------------ icons

    /** Status circle of a chapter or task: done (green tick), current (blue ring), locked, open. */
    enum Status {
        FERTIG, AKTUELL, OFFEN, GESPERRT
    }

    static final class Kreis implements Icon {
        final Status status;
        final String text;
        final int groesse;

        Kreis(Status status, String text, int groesse) {
            this.status = status;
            this.text = text;
            this.groesse = groesse;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double s = groesse;
                Ellipse2D e = new Ellipse2D.Double(x + 1, y + 1, s - 2, s - 2);
                switch (status) {
                case FERTIG:
                    g2.setColor(Theme.LED_GREEN);
                    g2.fill(e);
                    if (!text.isEmpty()) {
                        // chapters keep their number, so the overview still shows which chapter is which
                        zentriert(g2, x, y, s, Color.WHITE);
                        break;
                    }
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke((float) (s / 11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D p = new Path2D.Double();
                    p.moveTo(x + s * 0.29, y + s * 0.52);
                    p.lineTo(x + s * 0.44, y + s * 0.66);
                    p.lineTo(x + s * 0.71, y + s * 0.37);
                    g2.draw(p);
                    break;
                case AKTUELL:
                    g2.setColor(Theme.PATCH_BLUE);
                    g2.fill(e);
                    zentriert(g2, x, y, s, Color.WHITE);
                    break;
                case OFFEN:
                    g2.setColor(Color.WHITE);
                    g2.fill(e);
                    g2.setColor(Theme.farbe(0xB8C4D0, 0x46586B));
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.draw(e);
                    zentriert(g2, x, y, s, Theme.INK_2);
                    break;
                case GESPERRT:
                    g2.setColor(Theme.farbe(0xDFE5EC, 0x2A3644));
                    g2.fill(e);
                    if (!text.isEmpty()) {
                        zentriert(g2, x, y, s, Theme.farbe(0x6F7D8E, 0x8E9DAD));
                        break;
                    }
                    g2.setColor(Theme.INK_3);
                    double bw = s * 0.34;
                    double bh = s * 0.26;
                    double bx = x + (s - bw) / 2;
                    double by = y + s * 0.47;
                    g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 3, 3));
                    g2.setStroke(new BasicStroke((float) (s / 14)));
                    g2.draw(new Arc2D.Double(bx + bw * 0.18, by - bh * 0.9, bw * 0.64, bh * 1.5, 0, 180, Arc2D.OPEN));
                    break;
                }
            } finally {
                g2.dispose();
            }
        }

        private void zentriert(Graphics2D g2, int x, int y, double s, Color farbe) {
            if (text == null || text.isEmpty()) {
                return;
            }
            g2.setFont(Theme.font(Font.BOLD, (float) (s * (text.length() > 2 ? 0.34 : 0.44))));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(farbe);
            g2.drawString(text, (float) (x + (s - fm.stringWidth(text)) / 2), (float) (y + (s - fm.getHeight()) / 2 + fm.getAscent()));
        }

        @Override
        public int getIconWidth() {
            return groesse;
        }

        @Override
        public int getIconHeight() {
            return groesse;
        }
    }

    /** Small drawn symbols (the text fonts have no glyphs for them). */
    enum Symbol {
        PUNKT, SCHLOSS, KREUZ, FALSCH
    }

    static final class Zeichen implements Icon {
        final Symbol symbol;
        final Color farbe;
        final int groesse;

        Zeichen(Symbol symbol, Color farbe, int groesse) {
            this.symbol = symbol;
            this.farbe = farbe;
            this.groesse = groesse;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double s = groesse;
                g2.setColor(farbe);
                switch (symbol) {
                case PUNKT:
                    g2.fill(new Ellipse2D.Double(x + s * 0.25, y + s * 0.25, s * 0.5, s * 0.5));
                    break;
                case SCHLOSS: {
                    double bw = s * 0.62;
                    double bh = s * 0.46;
                    double bx = x + (s - bw) / 2;
                    double by = y + s * 0.44;
                    g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 3, 3));
                    g2.setStroke(new BasicStroke((float) Math.max(1.4, s / 9)));
                    g2.draw(new Arc2D.Double(bx + bw * 0.2, by - bh * 0.75, bw * 0.6, bh * 1.4, 0, 180, Arc2D.OPEN));
                    break;
                }
                case KREUZ: {
                    g2.setStroke(new BasicStroke((float) Math.max(1.5, s / 8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double a = s * 0.28;
                    double b = s * 0.72;
                    g2.draw(new java.awt.geom.Line2D.Double(x + a, y + a, x + b, y + b));
                    g2.draw(new java.awt.geom.Line2D.Double(x + b, y + a, x + a, y + b));
                    break;
                }
                case FALSCH: {
                    g2.fill(new Ellipse2D.Double(x + 1, y + 1, s - 2, s - 2));
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke((float) (s / 11), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double a = s * 0.34;
                    double b = s * 0.66;
                    g2.draw(new java.awt.geom.Line2D.Double(x + a, y + a, x + b, y + b));
                    g2.draw(new java.awt.geom.Line2D.Double(x + b, y + a, x + a, y + b));
                    break;
                }
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return groesse;
        }

        @Override
        public int getIconHeight() {
            return groesse;
        }
    }

    /** Thin progress bar. */
    static final class Balken extends JComponent {
        private static final long serialVersionUID = 1L;
        private double anteil;
        private final Color farbe;

        Balken(double anteil, Color farbe) {
            this.anteil = anteil;
            this.farbe = farbe;
            setPreferredSize(new Dimension(100, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double h = getHeight();
                g2.setColor(Theme.farbe(0xE6ECF2, 0x26333F));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), h, h, h));
                if (anteil > 0) {
                    g2.setColor(farbe);
                    g2.fill(new RoundRectangle2D.Double(0, 0, Math.max(h, getWidth() * Math.min(1, anteil)), h, h, h));
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ pictures

    /**
     * A picture from the lernpfad/bilder folder, scaled to the width, with a caption. If a dark variant
     * (name_dunkel.png) exists, it is shown while the dark mode is on.
     */
    static final class Bild extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Image bild;
        private final Image bildDunkel;
        private final int bw;
        private final int bh;
        private final JComponent flaeche;

        Bild(File datei, File dunkel, String unterschrift) {
            setOpaque(false);
            setLayout(new BorderLayout(0, 6));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            Image img = laden(datei);
            bild = img;
            Image d = img == null ? null : laden(dunkel);
            bildDunkel = d != null && d.getWidth(null) == img.getWidth(null) && d.getHeight(null) == img.getHeight(null) ? d : null;
            bw = img == null ? 16 : img.getWidth(null);
            bh = img == null ? 9 : img.getHeight(null);
            flaeche = new JComponent() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Theme.smooth(g.create());
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        int w = getWidth();
                        int h = getHeight();
                        RoundRectangle2D r = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 12, 12);
                        if (bild != null) {
                            g2.setClip(r);
                            g2.drawImage(aktuell(bild, bildDunkel), 0, 0, w, h, this);
                            g2.setClip(null);
                            g2.setColor(Theme.LINE);
                            g2.draw(r);
                        } else {
                            g2.setColor(Theme.SURFACE_2);
                            g2.fill(r);
                            g2.setColor(Theme.LINE);
                            g2.draw(r);
                            g2.setColor(Theme.INK_3);
                            g2.setFont(Theme.font(Font.PLAIN, 12f));
                            String t = datei == null ? "" : datei.getName();
                            g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2, h / 2);
                        }
                    } finally {
                        g2.dispose();
                    }
                }
            };
            add(flaeche, BorderLayout.CENTER);
            if (bild != null) {
                // a click shows the picture in full size
                flaeche.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                flaeche.setToolTipText(filius.modern.Texts.get("lp.zoom"));
                flaeche.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        if (!klick(e)) {
                            return;
                        }
                        gross(flaeche, bild, bildDunkel, unterschrift);
                    }
                });
            }
            if (unterschrift != null && !unterschrift.isEmpty()) {
                JLabel l = label(unterschrift, 12f, false, Theme.INK_3);
                add(l, BorderLayout.SOUTH);
            }
        }

        /** the enlarged pictures that are open: a second click on the picture closes the window again */
        private static final java.util.Map<Image, javax.swing.JDialog> OFFEN = new java.util.HashMap<>();

        private static Image laden(File datei) {
            if (datei != null && datei.isFile()) {
                ImageIcon icon = new ImageIcon(datei.getAbsolutePath());
                if (icon.getIconWidth() > 0) {
                    return icon.getImage();
                }
            }
            return null;
        }

        /** the dark variant in the dark mode, otherwise the normal picture */
        private static Image aktuell(Image hell, Image dunkel) {
            return dunkel != null && Theme.dunkel() ? dunkel : hell;
        }

        static void gross(Component von, Image bild, Image bildDunkel, String titel) {
            javax.swing.JDialog offen = OFFEN.get(bild);
            if (offen != null && offen.isDisplayable()) {
                // a second click on the picture closes its enlarged view again
                offen.dispose();
                return;
            }
            java.awt.Window fenster = javax.swing.SwingUtilities.getWindowAncestor(von);
            javax.swing.JDialog d = new javax.swing.JDialog(fenster, titel == null || titel.isEmpty() ? filius.modern.Texts.get("lp.picture") : titel,
                    java.awt.Dialog.ModalityType.MODELESS);
            Rectangle schirm = fenster.getGraphicsConfiguration().getBounds();
            int w = bild.getWidth(null);
            int h = bild.getHeight(null);
            double f = Math.min(1.0, Math.min((schirm.width - 80) / (double) w, (schirm.height - 120) / (double) h));
            int dw = (int) Math.round(w * f);
            int dh = (int) Math.round(h * f);
            JComponent flaeche = new JComponent() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = Theme.smooth(g.create());
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.drawImage(aktuell(bild, bildDunkel), 0, 0, getWidth(), getHeight(), this);
                    g2.dispose();
                }
            };
            flaeche.setPreferredSize(new Dimension(dw, dh));
            flaeche.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            flaeche.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (!klick(e)) {
                        return;
                    }
                    d.dispose();
                }
            });
            d.getContentPane().add(flaeche);
            d.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            // the picture can still be closed while a modal window of Filius is open (e.g. the settings of the
            // DHCP server in chapter 7, which the student compares with the picture)
            if (java.awt.Toolkit.getDefaultToolkit().isModalExclusionTypeSupported(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE)) {
                d.setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
            }
            d.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    OFFEN.remove(bild, d);
                }
            });
            OFFEN.put(bild, d);
            d.pack();
            d.setLocationRelativeTo(fenster);
            d.setVisible(true);
        }

        /** pictures are never enlarged in the panel, only made smaller; a click shows them in full size */
        private int hoeheFuer(int breite) {
            int w = Math.min(breite, bild == null ? breite : bw);
            return Math.max(40, (int) Math.round(w * (double) bh / bw));
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = hoeheFuer(w);
            int iw = bild == null ? w : (int) Math.round(h * (double) bw / bh);
            iw = Math.min(iw, w);
            flaeche.setBounds(0, 0, iw, h);
            for (Component c : getComponents()) {
                if (c != flaeche) {
                    Dimension d = c.getPreferredSize();
                    c.setBounds(0, h + 6, w, d.height);
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getParent() == null ? 320 : Math.max(120, getParent().getWidth() - getParent().getInsets().left - getParent().getInsets().right);
            int h = hoeheFuer(w);
            for (Component c : getComponents()) {
                if (c != flaeche) {
                    h += 6 + c.getPreferredSize().height;
                }
            }
            return new Dimension(w, h);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** Ring that shows finished chapters in the header button. */
    static final class Ring implements Icon {
        private final java.util.function.IntSupplier fertig;
        private final int gesamt;

        Ring(java.util.function.IntSupplier fertig, int gesamt) {
            this.fertig = fertig;
            this.gesamt = gesamt;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double done = fertig.getAsInt();
                double s = 18;
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(Theme.LINE);
                g2.draw(new Ellipse2D.Double(x + 1.5, y + 1.5, s - 3, s - 3));
                if (done > 0) {
                    g2.setColor(Theme.LED_GREEN);
                    g2.draw(new Arc2D.Double(x + 1.5, y + 1.5, s - 3, s - 3, 90, -360 * done / gesamt, Arc2D.OPEN));
                }
                if (done >= gesamt) {
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D check = new Path2D.Double();
                    check.moveTo(x + 5.5, y + 9.2);
                    check.lineTo(x + 8, y + 11.8);
                    check.lineTo(x + 12.6, y + 6.6);
                    g2.draw(check);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }
}
