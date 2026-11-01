package filius.modern.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import filius.modern.Texts;
import filius.modern.Theme;

/**
 * Search field of the text editor: Ctrl+F jumps into it, typing marks all matches (case does not matter),
 * Enter or the magnifier goes to the next match, Shift+Enter or the arrow up to the previous one, Esc back
 * to the text. The number of matches is shown in the field.
 */
public class Suchleiste extends JTextField {

    private static final long serialVersionUID = 1L;
    private static final Color TREFFER = Theme.farbe(0xFFE58F, 0x5A4612);
    private static final Color AKTUELL = Theme.farbe(0xFFB54D, 0x9A6414);

    private final CodeFlaeche code;
    private final JLabel anzahl = new JLabel();
    private final Highlighter.HighlightPainter alle = new DefaultHighlighter.DefaultHighlightPainter(TREFFER);
    private final Highlighter.HighlightPainter aktuell = new DefaultHighlighter.DefaultHighlightPainter(AKTUELL);
    private final List<int[]> treffer = new ArrayList<>();
    private final List<Object> markierungen = new ArrayList<>();
    /** current match, -1 = none chosen yet */
    private int index = -1;
    /** the first match from the cursor on: Enter or the magnifier goes there first */
    private int start = -1;
    private final Timer spaeter;
    private final Timer nachText;

    public Suchleiste(CodeFlaeche code) {
        super(16);
        this.code = code;
        this.putClientProperty("JTextField.placeholderText", Texts.get("editor.find.hint"));
        this.putClientProperty("JTextField.leadingIcon", new Lupe(14, Theme.INK_3));
        this.setToolTipText(Texts.get("editor.find.tip"));
        this.anzahl.setFont(Theme.font(java.awt.Font.PLAIN, 11.5f));
        this.anzahl.setForeground(Theme.INK_3);
        this.anzahl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        JToolBar knoepfe = new JToolBar();
        knoepfe.setFloatable(false);
        knoepfe.setOpaque(false);
        knoepfe.setBorder(BorderFactory.createEmptyBorder());
        knoepfe.add(this.anzahl);
        knoepfe.add(this.knopf(new Pfeil(true), Texts.get("editor.find.prev"), () -> this.springe(false)));
        knoepfe.add(this.knopf(new Pfeil(false), Texts.get("editor.find.next"), () -> this.springe(true)));
        knoepfe.add(this.knopf(new Lupe(15, Theme.PATCH_BLUE), Texts.get("editor.find.button"), () -> this.springe(true)));
        this.putClientProperty("JTextField.trailingComponent", knoepfe);
        this.putClientProperty("JTextField.showClearButton", Boolean.TRUE);

        // the search text changes: find again and show the first match from the cursor on
        this.spaeter = new Timer(120, e -> this.suchen(true));
        this.spaeter.setRepeats(false);
        this.getDocument().addDocumentListener(new Aenderung(() -> this.spaeter.restart()));
        // the code changes (the student types): positions of the matches change as well. Only the marks and the
        // number are updated; the view stays where the student writes (version 31: before, every new line made
        // the view jump back to the match, e.g. to <h1>)
        this.nachText = new Timer(120, e -> this.suchen(false));
        this.nachText.setRepeats(false);
        code.getDocument().addDocumentListener(new Aenderung(() -> {
            if (!this.getText().isEmpty()) {
                this.nachText.restart();
            }
        }));

        this.taste(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "weiter", () -> this.springe(true));
        this.taste(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "zurueck", () -> this.springe(false));
        this.taste(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fertig", this::zumText);
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                Suchleiste.this.selectAll();
            }
        });
    }

    private JButton knopf(Icon icon, String tip, Runnable r) {
        JButton b = new JButton(icon);
        b.setToolTipText(tip);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setMargin(new java.awt.Insets(1, 3, 1, 3));
        b.addActionListener(e -> r.run());
        return b;
    }

    private void taste(KeyStroke k, String name, Runnable r) {
        this.getInputMap(JComponent.WHEN_FOCUSED).put(k, name);
        this.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    /** Ctrl+F: into the field; a selected word of one line becomes the search text */
    public void oeffnen() {
        String auswahl = this.code.getSelectedText();
        if (auswahl != null && !auswahl.isEmpty() && auswahl.indexOf('\n') < 0 && auswahl.length() < 80) {
            this.setText(auswahl);
            this.suchen(false);
        }
        this.requestFocusInWindow();
        this.selectAll();
    }

    private void zumText() {
        this.code.requestFocusInWindow();
        if (this.index >= 0 && this.index < this.treffer.size()) {
            int[] t = this.treffer.get(this.index);
            this.code.select(t[0], t[1]);
        }
    }

    /** marks all matches; with springen the next match after the cursor becomes the current one */
    /** finds all matches and marks them; zeigen: scroll to the first match from the cursor on */
    private void suchen(boolean zeigen) {
        Highlighter h = this.code.getHighlighter();
        for (Object m : this.markierungen) {
            h.removeHighlight(m);
        }
        this.markierungen.clear();
        this.treffer.clear();
        String muster = this.getText();
        if (muster.isEmpty()) {
            this.index = -1;
            this.anzahlZeigen();
            return;
        }
        String text = this.code.getText().toLowerCase(Locale.ROOT);
        String m = muster.toLowerCase(Locale.ROOT);
        int pos = text.indexOf(m);
        while (pos >= 0 && this.treffer.size() < 10_000) {
            this.treffer.add(new int[] { pos, pos + m.length() });
            pos = text.indexOf(m, pos + Math.max(1, m.length()));
        }
        // the first match from the cursor on is shown; Enter or the magnifier selects it
        int von = this.code.getSelectionStart();
        this.index = -1;
        this.start = -1;
        for (int i = 0; i < this.treffer.size(); i++) {
            if (this.treffer.get(i)[0] >= von) {
                this.start = i;
                break;
            }
        }
        if (this.start < 0 && !this.treffer.isEmpty()) {
            this.start = 0;
        }
        this.markieren();
        if (zeigen && this.start >= 0) {
            int[] t = this.treffer.get(this.start);
            this.code.zeigen(t[0], t[1]);
        }
    }

    /** Enter, magnifier, arrows: the next or previous match */
    public void springe(boolean vor) {
        if (this.spaeter.isRunning() || this.nachText.isRunning() || this.treffer.isEmpty()) {
            this.spaeter.stop();
            this.nachText.stop();
            this.suchen(false);
        }
        int n = this.treffer.size();
        if (n == 0) {
            return;
        }
        if (this.index < 0) {
            this.index = vor ? Math.max(0, this.start) : (Math.max(0, this.start) - 1 + n) % n;
        } else {
            this.index = vor ? (this.index + 1) % n : (this.index - 1 + n) % n;
        }
        this.markieren();
        this.auswaehlen();
    }

    private void auswaehlen() {
        int[] t = this.treffer.get(this.index);
        this.code.setCaretPosition(t[0]);
        this.code.moveCaretPosition(t[1]);
        this.code.zeigen(t[0], t[1]);
    }

    private void markieren() {
        Highlighter h = this.code.getHighlighter();
        for (Object m : this.markierungen) {
            h.removeHighlight(m);
        }
        this.markierungen.clear();
        for (int i = 0; i < this.treffer.size(); i++) {
            int[] t = this.treffer.get(i);
            try {
                this.markierungen.add(h.addHighlight(t[0], t[1], i == this.index ? this.aktuell : this.alle));
            } catch (BadLocationException e) {
                // the text changed in between
            }
        }
        this.anzahlZeigen();
        this.code.repaint();
    }

    private void anzahlZeigen() {
        if (this.getText().isEmpty()) {
            this.anzahl.setText("");
            this.putClientProperty("JComponent.outline", null);
        } else if (this.treffer.isEmpty()) {
            this.anzahl.setText(Texts.get("editor.find.none"));
            this.anzahl.setForeground(Theme.SIGNAL_RED);
            this.putClientProperty("JComponent.outline", "error");
        } else if (this.index < 0) {
            this.anzahl.setText(Texts.format("editor.find.found", this.treffer.size()));
            this.anzahl.setForeground(Theme.INK_3);
            this.putClientProperty("JComponent.outline", null);
        } else {
            this.anzahl.setText(Texts.format("editor.find.count", this.index + 1, this.treffer.size()));
            this.anzahl.setForeground(Theme.INK_3);
            this.putClientProperty("JComponent.outline", null);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 300), d.height);
    }

    // ------------------------------------------------------------------ helpers

    private static final class Aenderung implements DocumentListener {
        private final Runnable r;

        Aenderung(Runnable r) {
            this.r = r;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            this.r.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            this.r.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }
    }

    /** magnifier */
    static final class Lupe implements Icon {
        private final int groesse;
        private final Color farbe;

        Lupe(int groesse, Color farbe) {
            this.groesse = groesse;
            this.farbe = farbe;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double s = this.groesse / 16.0;
                g2.setColor(this.farbe);
                g2.setStroke(new BasicStroke((float) (1.8 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Ellipse2D.Double(x + 2 * s, y + 2 * s, 8.5 * s, 8.5 * s));
                g2.draw(new Line2D.Double(x + 9.4 * s, y + 9.4 * s, x + 14 * s, y + 14 * s));
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return this.groesse;
        }

        @Override
        public int getIconHeight() {
            return this.groesse;
        }
    }

    /** arrow up or down */
    static final class Pfeil implements Icon {
        private final boolean hoch;

        Pfeil(boolean hoch) {
            this.hoch = hoch;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                g2.setColor(c.isEnabled() ? Theme.INK_2 : Theme.INK_3);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                if (this.hoch) {
                    p.moveTo(x + 3, y + 10);
                    p.lineTo(x + 7, y + 6);
                    p.lineTo(x + 11, y + 10);
                } else {
                    p.moveTo(x + 3, y + 5);
                    p.lineTo(x + 7, y + 9);
                    p.lineTo(x + 11, y + 5);
                }
                g2.draw(p);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 14;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }
}
