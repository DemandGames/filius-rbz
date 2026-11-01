package filius.modern.editor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.formdev.flatlaf.ui.FlatTextAreaUI;

import filius.modern.Theme;

/**
 * Code area of the text editor: monospaced, no line wrap, syntax colours for HTML/CSS/JavaScript, a
 * highlighted current line, indentation with Tab / Shift+Tab, automatic indentation of new lines, undo
 * and redo. The text itself stays plain text (the colours are only painted), so Filius stores exactly what
 * the student typed.
 */
public class CodeFlaeche extends JTextArea {

    private static final long serialVersionUID = 1L;
    static final Color HINTERGRUND = filius.modern.Theme.farbe(0xFFFFFF, 0x141C25);
    static final Color AKTUELLE_ZEILE = filius.modern.Theme.farbe(0xF4F7FB, 0x1C2732);
    static final Color AUSWAHL = filius.modern.Theme.farbe(0xADD6FF, 0x264F78);
    private static final String EINRUECKUNG = "  ";

    private Syntax.Sprache sprache = Syntax.Sprache.TEXT;
    private byte[] farben = new byte[0];
    private final UndoManager undo = new UndoManager();
    private boolean farbenVeraltet = true;

    public CodeFlaeche() {
        super();
        this.setUI(new UI());
        this.setFont(EditorSchrift.code());
        this.setTabSize(2);
        this.setLineWrap(false);
        this.setBackground(HINTERGRUND);
        this.setForeground(Syntax.FARBEN[Syntax.TEXT]);
        this.setSelectionColor(AUSWAHL);
        this.setSelectedTextColor(null);
        this.setCaretColor(filius.modern.Theme.farbe(0x1F2328, 0xAEAFAD));
        this.setOpaque(false);
        this.setMargin(new java.awt.Insets(4, 8, 4, 8));
        this.undo.setLimit(500);
        this.dokumentBeobachten(this.getDocument());
        this.addPropertyChangeListener("document", e -> this.dokumentBeobachten((Document) e.getNewValue()));
        this.addCaretListener(e -> this.repaint());
        this.tasten();
    }

    private void dokumentBeobachten(Document d) {
        if (d == null) {
            return;
        }
        d.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                CodeFlaeche.this.farbenVeraltet = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                CodeFlaeche.this.farbenVeraltet = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        d.addUndoableEditListener(e -> {
            if (e.getEdit() instanceof AbstractDocument.DefaultDocumentEvent
                    && ((AbstractDocument.DefaultDocumentEvent) e.getEdit()).getType() == DocumentEvent.EventType.CHANGE) {
                return;
            }
            this.gruppieren(e.getEdit());
        });
        this.farbenVeraltet = true;
    }

    /** typing goes back word by word, not letter by letter: edits close together form one step */
    private javax.swing.undo.CompoundEdit gruppe;
    private long gruppeZeit;
    private boolean gruppeTippen;

    private void gruppieren(javax.swing.undo.UndoableEdit edit) {
        long jetzt = System.currentTimeMillis();
        boolean tippen = edit instanceof AbstractDocument.DefaultDocumentEvent && ((AbstractDocument.DefaultDocumentEvent) edit).getLength() == 1;
        if (tippen) {
            try {
                AbstractDocument.DefaultDocumentEvent ev = (AbstractDocument.DefaultDocumentEvent) edit;
                if (ev.getType() == DocumentEvent.EventType.INSERT && " \n".indexOf(this.getDocument().getText(ev.getOffset(), 1)) >= 0) {
                    tippen = false; // a space or a new line ends a word
                }
            } catch (BadLocationException e) {
                // keep it as typing
            }
        }
        boolean dazu = this.gruppe != null && (jetzt - this.gruppeZeit < 40 || tippen && this.gruppeTippen && jetzt - this.gruppeZeit < 1500);
        if (!dazu) {
            this.gruppeBeenden();
            this.gruppe = new javax.swing.undo.CompoundEdit();
            this.undo.addEdit(this.gruppe);
        }
        this.gruppe.addEdit(edit);
        this.gruppeZeit = jetzt;
        this.gruppeTippen = tippen;
    }

    private void gruppeBeenden() {
        if (this.gruppe != null) {
            this.gruppe.end();
            this.gruppe = null;
        }
    }

    public void setSprache(Syntax.Sprache sprache) {
        if (sprache != this.sprache) {
            this.sprache = sprache;
            this.farbenVeraltet = true;
            this.repaint();
        }
    }

    public Syntax.Sprache getSprache() {
        return this.sprache;
    }

    /** text replaced by a file: no undo back to the previous file */
    public void neuerText(String text) {
        this.setText(text);
        this.setCaretPosition(0);
        this.gruppeBeenden();
        this.undo.discardAllEdits();
    }

    byte farbe(int pos) {
        if (this.farbenVeraltet) {
            this.farben = Syntax.faerben(this.getText(), this.sprache);
            this.farbenVeraltet = false;
        }
        return pos >= 0 && pos < this.farben.length ? this.farben[pos] : Syntax.TEXT;
    }

    /** keeps the colour view when the look and feel changes (light/dark mode) */
    @Override
    public void updateUI() {
        this.setUI(new UI());
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(this.getBackground());
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        try {
            Rectangle2D r = this.modelToView2D(this.getCaretPosition());
            if (r != null && (this.getSelectionStart() == this.getSelectionEnd())) {
                g.setColor(AKTUELLE_ZEILE);
                g.fillRect(0, (int) r.getY(), this.getWidth(), (int) Math.ceil(r.getHeight()));
            }
        } catch (BadLocationException e) {
            // no current line
        }
        super.paintComponent(g);
    }

    /** no line wrap: as wide as the longest line, at least as wide as the visible area */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return this.getParent() == null || this.getUI().getPreferredSize(this).width <= this.getParent().getWidth();
    }

    /**
     * a short text still fills the whole visible area (version 31): a click below the last line is a click into the
     * text, so the text gets the keyboard and the caret goes to the end. Before, such a click hit the empty area
     * behind the text, the search field kept the keyboard, and Enter jumped to the next match instead of starting
     * a new line.
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return this.getParent() instanceof javax.swing.JViewport && this.getUI().getPreferredSize(this).height <= this.getParent().getHeight();
    }

    // ------------------------------------------------------------------ keys

    private void tasten() {
        int strg = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        this.aktion(KeyStroke.getKeyStroke(KeyEvent.VK_Z, strg), "rueckgaengig", () -> {
            this.gruppeBeenden();
            try {
                if (this.undo.canUndo()) {
                    this.undo.undo();
                }
            } catch (CannotUndoException e) {
                // nothing to undo
            }
        });
        Runnable wiederholen = () -> {
            this.gruppeBeenden();
            try {
                if (this.undo.canRedo()) {
                    this.undo.redo();
                }
            } catch (CannotRedoException e) {
                // nothing to redo
            }
        };
        this.aktion(KeyStroke.getKeyStroke(KeyEvent.VK_Y, strg), "wiederholen", wiederholen);
        this.aktion(KeyStroke.getKeyStroke(KeyEvent.VK_Z, strg | InputEvent.SHIFT_DOWN_MASK), "wiederholen2", wiederholen);
        this.aktion(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "einruecken", () -> this.einruecken(true));
        this.aktion(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "ausruecken", () -> this.einruecken(false));
        this.aktion(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "neueZeile", this::neueZeile);
    }

    private void aktion(KeyStroke taste, String name, Runnable r) {
        this.getInputMap(JComponent.WHEN_FOCUSED).put(taste, name);
        this.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (CodeFlaeche.this.isEditable()) {
                    r.run();
                }
            }
        });
    }

    /** Enter keeps the indentation of the current line */
    private void neueZeile() {
        try {
            int pos = this.getCaretPosition();
            int zeile = this.getLineOfOffset(pos);
            int start = this.getLineStartOffset(zeile);
            String text = this.getText(start, pos - start);
            int k = 0;
            while (k < text.length() && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) {
                k++;
            }
            this.replaceSelection("\n" + text.substring(0, k));
        } catch (BadLocationException e) {
            this.replaceSelection("\n");
        }
    }

    /** Tab: two spaces, or all selected lines; Shift+Tab: back */
    private void einruecken(boolean rein) {
        try {
            int a = this.getSelectionStart();
            int b = this.getSelectionEnd();
            int z1 = this.getLineOfOffset(a);
            int z2 = this.getLineOfOffset(b);
            if (rein && z1 == z2) {
                this.replaceSelection(EINRUECKUNG);
                return;
            }
            if (z2 > z1 && b == this.getLineStartOffset(z2)) {
                z2--; // the selection ends at the start of a line: that line stays
            }
            int verschiebungErste = 0;
            int verschiebungGesamt = 0;
            for (int z = z1; z <= z2; z++) {
                int start = this.getLineStartOffset(z);
                if (rein) {
                    this.getDocument().insertString(start, EINRUECKUNG, null);
                    verschiebungGesamt += EINRUECKUNG.length();
                    if (z == z1) {
                        verschiebungErste = EINRUECKUNG.length();
                    }
                } else {
                    int ende = this.getLineEndOffset(z);
                    String zeile = this.getText(start, ende - start);
                    int weg = 0;
                    while (weg < EINRUECKUNG.length() && weg < zeile.length() && zeile.charAt(weg) == ' ') {
                        weg++;
                    }
                    if (weg == 0 && zeile.startsWith("\t")) {
                        weg = 1;
                    }
                    if (weg > 0) {
                        this.getDocument().remove(start, weg);
                        verschiebungGesamt -= weg;
                        if (z == z1) {
                            verschiebungErste = -Math.min(weg, a - start);
                        }
                    }
                }
            }
            if (a != b) {
                this.select(Math.max(this.getLineStartOffset(z1), a + verschiebungErste), b + verschiebungGesamt);
            }
        } catch (BadLocationException e) {
            // keep the text as it is
        }
    }

    // ------------------------------------------------------------------ painting with colours

    /** FlatLaf look, but a view that paints the syntax colours */
    private static final class UI extends FlatTextAreaUI {
        @Override
        public View create(Element elem) {
            return new FarbView(elem);
        }

        @Override
        protected void paintBackground(Graphics g) {
            // painted by the component together with the current line
        }

        public static ComponentUI createUI(JComponent c) {
            return new UI();
        }
    }

    private static final class FarbView extends PlainView {
        FarbView(Element elem) {
            super(elem);
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException {
            return this.farbig(g, x, y, p0, p1);
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException {
            return this.farbig(g, x, y, p0, p1);
        }

        private float farbig(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException {
            CodeFlaeche f = (CodeFlaeche) this.getContainer();
            Document doc = this.getDocument();
            Segment seg = new Segment();
            int pos = p0;
            while (pos < p1) {
                byte art = f.farbe(pos);
                int ende = pos + 1;
                while (ende < p1 && f.farbe(ende) == art) {
                    ende++;
                }
                doc.getText(pos, ende - pos, seg);
                g.setColor(Syntax.FARBEN[art]);
                x = Utilities.drawTabbedText(seg, x, y, g, this, pos);
                pos = ende;
            }
            return x;
        }
    }

    /** scrolls so that the range is visible with some room around it */
    public void zeigen(int von, int bis) {
        SwingUtilities.invokeLater(() -> {
            try {
                Rectangle2D a = this.modelToView2D(von);
                Rectangle2D b = this.modelToView2D(bis);
                if (a == null || b == null) {
                    return;
                }
                Rectangle r = a.getBounds().union(b.getBounds());
                r.grow(60, 3 * r.height);
                this.scrollRectToVisible(r);
            } catch (BadLocationException e) {
                // outside the text
            }
        });
    }
}
