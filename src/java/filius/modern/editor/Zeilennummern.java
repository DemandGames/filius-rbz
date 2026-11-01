package filius.modern.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import filius.modern.Theme;

/** Line numbers left of the code area, the number of the current line darker (as in VS Code). */
public class Zeilennummern extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final Color GRUND = Theme.farbe(0xFFFFFF, 0x141C25);
    private static final Color ZAHL = Theme.farbe(0x8C959F, 0x5E6B78);
    private static final Color AKTUELL = Theme.farbe(0x1F2328, 0xC6CDD5);

    private final CodeFlaeche code;
    private int stellen = 0;

    public Zeilennummern(CodeFlaeche code) {
        this.code = code;
        // a click on the line numbers puts the keyboard into the text, at the start of that line (version 31)
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int pos = code.viewToModel2D(new java.awt.Point(0, e.getY()));
                if (pos >= 0) {
                    code.setCaretPosition(pos);
                }
                code.requestFocusInWindow();
            }
        });
        this.setFont(EditorSchrift.zeilen());
        this.setOpaque(true);
        this.setBackground(GRUND);
        code.addCaretListener(e -> this.repaint());
        DocumentListener l = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                Zeilennummern.this.pruefeBreite();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                Zeilennummern.this.pruefeBreite();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        code.getDocument().addDocumentListener(l);
        code.addPropertyChangeListener("document", e -> {
            if (e.getOldValue() != null) {
                ((javax.swing.text.Document) e.getOldValue()).removeDocumentListener(l);
            }
            if (e.getNewValue() != null) {
                ((javax.swing.text.Document) e.getNewValue()).addDocumentListener(l);
            }
            this.pruefeBreite();
        });
        this.pruefeBreite();
    }

    private void pruefeBreite() {
        int zeilen = this.code.getDocument().getDefaultRootElement().getElementCount();
        int s = Math.max(2, String.valueOf(zeilen).length());
        if (s != this.stellen) {
            this.stellen = s;
            this.revalidate();
        }
        this.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = this.getFontMetrics(this.getFont());
        return new Dimension(fm.charWidth('0') * this.stellen + 22, this.code.getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g.create());
        try {
            Rectangle clip = g2.getClipBounds();
            g2.setColor(this.getBackground());
            g2.fillRect(clip.x, clip.y, clip.width, clip.height);
            g2.setFont(this.getFont());
            FontMetrics fm = g2.getFontMetrics();
            Element root = this.code.getDocument().getDefaultRootElement();
            int aktuelle = root.getElementIndex(this.code.getCaretPosition());
            int erste = root.getElementIndex(this.code.viewToModel2D(new java.awt.Point(0, clip.y)));
            int letzte = root.getElementIndex(this.code.viewToModel2D(new java.awt.Point(0, clip.y + clip.height)));
            for (int z = erste; z <= letzte && z < root.getElementCount(); z++) {
                Rectangle2D r = this.code.modelToView2D(root.getElement(z).getStartOffset());
                if (r == null) {
                    continue;
                }
                String text = String.valueOf(z + 1);
                g2.setColor(z == aktuelle ? AKTUELL : ZAHL);
                int y = (int) (r.getY() + (r.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.drawString(text, this.getWidth() - 12 - fm.stringWidth(text), y);
            }
        } catch (BadLocationException e) {
            // nothing to paint
        } finally {
            g2.dispose();
        }
    }
}
