/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JDocuElement of Filius 2.9.3 (GPL). While the
 * documentation mode is active, a structure field (Gliederungsfläche) lies above the devices. The
 * original filled it almost opaque, so the devices inside were barely visible; here the fill is
 * lighter. Outside the documentation mode the field is painted as before.
 * Also changed: in the documentation mode a structure field shows a small "+" at its top left. A click on it
 * adds a label that belongs to the field (name and address of the network): it sits at the top left, moves
 * with the field and is saved with it (GUIDocuItem). The "+" is hidden outside the documentation mode and
 * while the field has a label. A text field is moved by the bar above the text (in the original: below).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.netzwerksicht;

import filius.gui.GUIContainer;
import filius.gui.JMainFrame;
import filius.gui.netzwerksicht.GUIDocumentationPanel;
import filius.gui.netzwerksicht.JDocuColorSelector;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.SzenarioVerwaltung;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;

public class JDocuElement
extends JPanel
implements I18n {
    private static final long serialVersionUID = 1L;
    private int updateType;
    private Dimension elemMoveOffset;
    private boolean enabled = true;
    private boolean localFocus = false;
    private boolean enlargedPanel = false;
    private boolean dragDropCreation = false;
    private Color referenceColor;
    private Color opaqueColor;
    private Color transparentColor;
    private Color borderColor;
    private boolean isText;
    private JDocuTextArea textArea;
    private static final int RECT_CORNER_SIZE = 10;
    private static final int RECT_MIN_WIDTH = 20;
    private static final int RECT_MIN_HEIGHT = 20;
    private static final int TEXT_CORNER_SIZE = 10;
    private static final int TEXT_MIN_WIDTH = 100;
    private static final int TEXT_MIN_HEIGHT = 48;
    private static final int TEXT_BORDER_WIDTH = 7;
    private static final int TEXT_BORDER_BOTTOM = 30;
    private static final Color[] fontColor = new Color[]{Color.BLACK, new Color(0.4f, 0.4f, 0.4f), new Color(0.6f, 0.6f, 0.6f), new Color(0.7f, 0.7f, 0.7f), Color.WHITE, new Color(1.0f, 0.0f, 0.0f), new Color(0.7f, 0.64f, 0.78f), new Color(0.31f, 0.51f, 0.74f), new Color(0.61f, 0.73f, 0.35f), new Color(0.58f, 0.54f, 0.33f), new Color(0.58f, 0.22f, 0.21f), new Color(0.37f, 0.29f, 0.48f), new Color(0.12f, 0.29f, 0.49f), new Color(0.43f, 0.53f, 0.22f), new Color(0.35f, 0.33f, 0.19f)};
    private static final Color[] panelColor = new Color[]{Color.BLACK, new Color(0.8f, 0.8f, 0.8f), new Color(0.92f, 0.92f, 0.92f), Color.WHITE, new Color(1.0f, 0.79f, 0.79f), new Color(0.99f, 0.79f, 0.64f), new Color(1.0f, 1.0f, 0.89f), new Color(0.83f, 0.81f, 0.71f), new Color(0.87f, 0.83f, 1.0f), new Color(0.82f, 0.91f, 1.0f), new Color(0.75f, 1.0f, 0.86f), new Color(0.8f, 0.87f, 0.68f)};
    private static final Color panelFocusColor = new Color(0.22f, 0.6f, 0.98f, 0.3f);
    private static final Color borderFocusColor = new Color(0.48f, 0.73f, 0.98f);
    private static final Color textPanelBorderNormalColor = new Color(0.7f, 0.7f, 0.7f);
    private static final Color transparent = new Color(0, 0, 0, 0);

    public JDocuElement(boolean isText) {
        if (isText) {
            this.textArea = new JDocuTextArea();
            this.add(this.textArea);
            this.isText = true;
            this.setColor(Color.BLACK);
            this.setOpaque(false);
            this.setLayout(null);
            this.setSize(100, 50);
            this.updateTextAreaBounds();
        } else {
            this.setSize(100, 50);
            this.setColor(panelColor[1]);
            this.setOpaque(false);
            this.setFocusable(true);
            this.setLayout(null);
            this.plusKnopf = new PlusKnopf();
            this.add(this.plusKnopf);
            this.titelAnordnen();
        }
        this.initListeners();
    }

    // ------------------------------------------------------------------ label of a structure field

    /** the label of a structure field (null: none), shown at its top left */
    private javax.swing.JTextField titelFeld;
    /** the "+" at the top left of a structure field in the documentation mode */
    private javax.swing.JComponent plusKnopf;

    public boolean isTextElement() {
        return this.isText;
    }

    /** the label of a structure field, null if it has none (text fields: always null) */
    public String getTitel() {
        if (this.isText || this.titelFeld == null) {
            return null;
        }
        String t = this.titelFeld.getText();
        return t == null || t.trim().isEmpty() ? null : t;
    }

    public void setTitel(String titel) {
        if (this.isText) {
            return;
        }
        if (titel == null || titel.trim().isEmpty()) {
            if (this.titelFeld != null) {
                this.remove(this.titelFeld);
                this.titelFeld = null;
            }
        } else {
            this.titelFeldErzeugen().setText(titel);
        }
        this.titelAnordnen();
        this.repaint();
    }

    private javax.swing.JTextField titelFeldErzeugen() {
        if (this.titelFeld == null) {
            final javax.swing.JTextField f = new javax.swing.JTextField() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    this.setForeground(filius.modern.Theme.INK);
                    super.paintComponent(g);
                }
            };
            f.setOpaque(false);
            f.setFont(filius.modern.Theme.font(Font.BOLD, 14f));
            f.putClientProperty("JTextField.placeholderText", filius.modern.Texts.get("doku.titel.hinweis"));
            f.putClientProperty("JComponent.outline", null);
            f.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
            f.setEditable(this.enabled);
            f.setFocusable(this.enabled);
            f.addActionListener(e -> this.titelFertig());
            f.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    f.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                            javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0x2E6BD8)),
                            javax.swing.BorderFactory.createEmptyBorder(2, 4, 0, 4)));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    f.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
                    JDocuElement.this.titelFertig();
                }
            });
            f.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    SzenarioVerwaltung.getInstance().setzeGeaendert();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    SzenarioVerwaltung.getInstance().setzeGeaendert();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
            this.titelFeld = f;
            this.add(f);
        }
        return this.titelFeld;
    }

    /** an empty label is removed again, so the "+" comes back */
    private void titelFertig() {
        if (this.titelFeld != null && (this.titelFeld.getText() == null || this.titelFeld.getText().trim().isEmpty())
                && !this.titelFeld.isFocusOwner()) {
            this.setTitel(null);
        } else if (this.titelFeld != null && this.titelFeld.isFocusOwner()) {
            this.requestFocusInWindow();
        }
    }

    /** places the label and the "+" at the top left of the structure field */
    private void titelAnordnen() {
        if (this.isText) {
            return;
        }
        int w = this.getWidth();
        if (this.titelFeld != null) {
            int h = this.titelFeld.getPreferredSize().height;
            this.titelFeld.setBounds(9, 8, Math.max(40, w - 18), h);
            this.titelFeld.setEditable(this.enabled);
            this.titelFeld.setFocusable(this.enabled);
        }
        if (this.plusKnopf != null) {
            this.plusKnopf.setBounds(9, 9, 22, 22);
            this.plusKnopf.setVisible(this.enabled && this.titelFeld == null);
        }
    }

    /** a click on the "+" adds an empty label and puts the cursor into it */
    private void titelAnlegen() {
        javax.swing.JTextField f = this.titelFeldErzeugen();
        this.titelAnordnen();
        this.repaint();
        f.requestFocusInWindow();
    }

    private final class PlusKnopf extends javax.swing.JComponent {
        private static final long serialVersionUID = 1L;
        private boolean ueber;

        PlusKnopf() {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.setToolTipText(filius.modern.Texts.get("doku.titel.plus"));
            this.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    PlusKnopf.this.ueber = true;
                    PlusKnopf.this.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    PlusKnopf.this.ueber = false;
                    PlusKnopf.this.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && JDocuElement.this.enabled) {
                        JDocuElement.this.titelAnlegen();
                    }
                }
            });
        }

        /** a text field already lies on the corner (the network was labelled with a text field) */
        private boolean verdeckt() {
            java.awt.Container p = JDocuElement.this.getParent();
            if (p == null) {
                return false;
            }
            Rectangle ecke = new Rectangle(JDocuElement.this.getX() + this.getX(), JDocuElement.this.getY() + this.getY(), this.getWidth(), this.getHeight());
            for (Component c : p.getComponents()) {
                if (c instanceof JDocuElement && c != JDocuElement.this && ((JDocuElement) c).isText && c.getBounds().intersects(ecke)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(int x, int y) {
            return super.contains(x, y) && !this.verdeckt();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (this.verdeckt()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color blau = filius.modern.Theme.PATCH_BLUE;
            g2.setColor(this.ueber ? blau : filius.modern.Theme.SURFACE);
            g2.fillRoundRect(1, 1, this.getWidth() - 2, this.getHeight() - 2, 8, 8);
            g2.setColor(blau);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(1, 1, this.getWidth() - 3, this.getHeight() - 3, 8, 8);
            g2.setColor(this.ueber ? Color.WHITE : blau);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int m = this.getWidth() / 2;
            g2.drawLine(m, 6, m, this.getHeight() - 7);
            g2.drawLine(6, this.getHeight() / 2, this.getWidth() - 7, this.getHeight() / 2);
            g2.dispose();
        }
    }

    public JDocuElement(boolean isText, boolean dragDropCreation) {
        this(isText);
        this.dragDropCreation = dragDropCreation;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!(this.getParent() instanceof GUIDocumentationPanel)) {
            return;
        }
        if (this.dragDropCreation) {
            this.setLocalFocus(true);
        }
    }

    private void initListeners() {
        this.addComponentListener(new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent e) {
                if (JDocuElement.this.isText) {
                    JDocuElement.this.updateTextAreaBounds();
                    JDocuElement.this.repaint();
                } else {
                    JDocuElement.this.titelAnordnen();
                }
            }
        });
        this.addKeyListener(new KeyAdapter(){

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case 127: {
                        JDocuElement.this.delete();
                        break;
                    }
                    case 27: {
                        JDocuElement.this.setLocalFocus(false);
                    }
                }
            }
        });
        this.addMouseListener(new MouseInputAdapter(){

            @Override
            public void mousePressed(MouseEvent e) {
                if (JDocuElement.this.enabled) {
                    if (!JDocuElement.this.isText) {
                        JDocuElement.this.requestFocusInWindow();
                        if (!JDocuElement.this.localFocus) {
                            JDocuElement.this.setLocalFocus(true);
                        }
                    }
                    switch (e.getButton()) {
                        case 1: {
                            if (JDocuElement.this.updateType != 1) break;
                            JDocuElement.this.elemMoveOffset = new Dimension(e.getX(), e.getY());
                            break;
                        }
                        case 3: {
                            JDocuElement.this.showPopupMenu(e.getX(), e.getY());
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (JDocuElement.this.enabled) {
                    JDocuElement.this.updateType = 0;
                    JDocuElement.this.updateUI();
                    SzenarioVerwaltung.getInstance().setzeGeaendert();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (JDocuElement.this.enabled) {
                    JDocuElement.this.setPanelCursor(0);
                }
            }
        });
        this.addMouseMotionListener(new MouseInputAdapter(){

            @Override
            public void mouseMoved(MouseEvent e) {
                if (JDocuElement.this.enabled && (!JDocuElement.this.isText || JDocuElement.this.enlargedPanel)) {
                    boolean lower;
                    boolean left = e.getX() <= 7;
                    boolean right = e.getX() >= JDocuElement.this.getWidth() - 7;
                    boolean upper = e.getY() <= 7;
                    boolean bl = lower = e.getY() >= JDocuElement.this.getHeight() - 7;
                    JDocuElement.this.updateType = left ? (upper ? 10 : (lower ? 18 : 2)) : (right ? (upper ? 12 : (lower ? 20 : 4)) : (upper ? 8 : (lower ? 16 : 1)));
                    switch (JDocuElement.this.updateType) {
                        case 1: {
                            JDocuElement.this.setPanelCursor(13);
                            break;
                        }
                        case 2: {
                            JDocuElement.this.setPanelCursor(10);
                            break;
                        }
                        case 4: {
                            JDocuElement.this.setPanelCursor(11);
                            break;
                        }
                        case 8: {
                            JDocuElement.this.setPanelCursor(8);
                            break;
                        }
                        case 16: {
                            JDocuElement.this.setPanelCursor(9);
                            break;
                        }
                        case 10: {
                            JDocuElement.this.setPanelCursor(9);
                            break;
                        }
                        case 12: {
                            JDocuElement.this.setPanelCursor(7);
                            break;
                        }
                        case 18: {
                            JDocuElement.this.setPanelCursor(4);
                            break;
                        }
                        case 20: {
                            JDocuElement.this.setPanelCursor(5);
                            break;
                        }
                        default: {
                            JDocuElement.this.setPanelCursor(0);
                        }
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (JDocuElement.this.isText && !JDocuElement.this.enlargedPanel) {
                        return;
                    }
                    boolean left = (JDocuElement.this.updateType & 2) != 0;
                    boolean right = (JDocuElement.this.updateType & 4) != 0;
                    boolean upper = (JDocuElement.this.updateType & 8) != 0;
                    boolean lower = (JDocuElement.this.updateType & 0x10) != 0;
                    int elemX = JDocuElement.this.getX();
                    int elemY = JDocuElement.this.getY();
                    int elemW = JDocuElement.this.getWidth();
                    int elemH = JDocuElement.this.getHeight();
                    if (JDocuElement.this.enabled) {
                        if (JDocuElement.this.updateType == 1) {
                            JDocuElement.this.setLocation(e.getX() - (int)JDocuElement.this.elemMoveOffset.getWidth() + elemX, e.getY() - (int)JDocuElement.this.elemMoveOffset.getHeight() + elemY);
                        } else if (JDocuElement.this.updateType != 0) {
                            int newH;
                            int newY;
                            int newW;
                            int newX;
                            int minH;
                            int minW = JDocuElement.this.isText ? 100 : 20;
                            int n = minH = JDocuElement.this.isText ? 48 : 20;
                            if (left) {
                                newX = elemX + e.getX();
                                newW = elemW - e.getX();
                                if (newW < minW) {
                                    newX -= minW - newW;
                                    newW = minW;
                                }
                            } else if (right) {
                                newX = elemX;
                                newW = e.getX();
                                if (newW < minW) {
                                    newW = minW;
                                }
                            } else {
                                newX = elemX;
                                newW = elemW;
                            }
                            if (upper) {
                                newY = elemY + e.getY();
                                newH = elemH - e.getY();
                                if (newH < minH) {
                                    newY -= minH - newH;
                                    newH = minH;
                                }
                            } else if (lower) {
                                newY = elemY;
                                newH = e.getY();
                                if (newH < minH) {
                                    newH = minH;
                                }
                            } else {
                                newY = elemY;
                                newH = elemH;
                            }
                            JDocuElement.this.setBounds(newX, newY, newW, newH);
                        }
                    }
                }
            }
        });
    }

    private void showPopupMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        if (this.isText) {
            JMenu mFontSize = new JMenu(messages.getString("guievents_msg15"));
            String[] fontSizeText = new String[]{messages.getString("guievents_msg16"), messages.getString("guievents_msg17"), messages.getString("guievents_msg18")};
            float[] fontSize = new float[]{12.0f, 18.0f, 24.0f};
            float tafs = this.textArea.getFont().getSize();
            for (int i = 0; i < fontSize.length; ++i) {
                JRadioButtonMenuItem mi = new JRadioButtonMenuItem(fontSizeText[i]);
                mi.putClientProperty("fontSize", Float.valueOf(fontSize[i]));
                mi.setSelected(tafs == fontSize[i]);
                mi.addActionListener(new ActionListener(){

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JMenuItem smi = (JMenuItem)e.getSource();
                        float fs = ((Float)smi.getClientProperty("fontSize")).floatValue();
                        JDocuElement.this.textArea.setFont(JDocuElement.this.textArea.getFont().deriveFont(fs));
                        JDocuElement.this.repaint();
                    }
                });
                mFontSize.add(mi);
            }
            menu.add(mFontSize);
            final int[] fontStyles = new int[]{0, 2, 1};
            String[] fontStyleText = new String[]{messages.getString("guievents_msg19"), messages.getString("guievents_msg20"), messages.getString("guievents_msg21")};
            JMenu mFontStyle = new JMenu(messages.getString("guievents_msg14"));
            int tafst = this.textArea.getFont().getStyle();
            for (int i = 0; i < fontStyles.length; ++i) {
                JRadioButtonMenuItem mi = new JRadioButtonMenuItem(fontStyleText[i]);
                mi.putClientProperty("fontStyle", i);
                mi.setSelected(tafst == fontStyles[i]);
                mi.addActionListener(new ActionListener(){

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JMenuItem smi = (JMenuItem)e.getSource();
                        int fs = (Integer)smi.getClientProperty("fontStyle");
                        JDocuElement.this.textArea.setFont(JDocuElement.this.textArea.getFont().deriveFont(fontStyles[fs]));
                        JDocuElement.this.repaint();
                    }
                });
                mFontStyle.add(mi);
            }
            menu.add(mFontStyle);
            final JDocuColorSelector mFontColor = new JDocuColorSelector(messages.getString("guievents_msg13"), fontColor, 3, this.getColor());
            mFontColor.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    JDocuElement.this.setColor(mFontColor.getColor());
                    JDocuElement.this.repaint();
                }
            });
            menu.add(mFontColor);
        } else {
            final JDocuColorSelector mPanelColor = new JDocuColorSelector(messages.getString("guievents_msg13"), panelColor, 3, this.getColor());
            mPanelColor.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e) {
                    JDocuElement.this.setColor(mPanelColor.getColor());
                    JDocuElement.this.repaint();
                }
            });
            menu.add(mPanelColor);
        }
        menu.addSeparator();
        JMenuItem miDelete = new JMenuItem(messages.getString("guievents_msg7"));
        miDelete.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                JDocuElement.this.delete();
            }
        });
        menu.add(miDelete);
        menu.show(this, x, y);
    }

    private void setPanelCursor(int cursor) {
        JMainFrame.getJMainFrame().setCursor(Cursor.getPredefinedCursor(cursor));
    }

    private void delete() {
        GUIContainer.getGUIContainer().removeDocuElement(this);
        GUIContainer.getGUIContainer().updateViewport();
        this.setPanelCursor(0);
    }

    public String getText() {
        if (this.isText) {
            return this.textArea.getText();
        }
        return null;
    }

    public void setText(String text) {
        if (this.isText) {
            this.textArea.setText(text);
        }
    }

    public Color getColor() {
        return this.referenceColor;
    }

    public void setColor(Color color) {
        if (null == color) {
            color = Color.LIGHT_GRAY;
        }
        this.referenceColor = color;
        if (this.isText) {
            this.textArea.setForeground(color);
        } else {
            int red = color.getRed();
            red = 255 + (red - 255) * 8 / 10;
            int green = color.getGreen();
            green = 255 + (green - 255) * 8 / 10;
            int blue = color.getBlue();
            blue = 255 + (blue - 255) * 8 / 10;
            this.opaqueColor = new Color(red, green, blue);
            this.transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 110);
            this.borderColor = new Color(color.getRed() * 4 / 5, color.getGreen() * 4 / 5, color.getBlue() * 4 / 5);
        }
    }

    @Override
    public Font getFont() {
        if (this.isText) {
            return this.textArea.getFont();
        }
        return null;
    }

    @Override
    public void setFont(Font font) {
        if (this.isText) {
            this.textArea.setFont(font);
        }
    }

    public void setSelected(boolean selected) {
        this.repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled == this.enabled) {
            return;
        }
        this.enabled = enabled;
        this.setFocusable(enabled);
        if (this.isText) {
            this.textArea.setEditable(enabled);
            this.textArea.setFocusable(enabled);
        } else {
            this.titelAnordnen();
        }
        if (!enabled) {
            this.setLocalFocus(false);
        }
    }

    public void setLocalFocus(boolean focused) {
        if (focused == this.localFocus) {
            return;
        }
        if (focused) {
            Component[] c = this.getParent().getComponents();
            for (int i = 0; i < c.length; ++i) {
                if (!(c[i] instanceof JDocuElement) || c[i] == this) continue;
                ((JDocuElement)c[i]).setLocalFocus(false);
            }
        }
        this.localFocus = focused;
        if (this.isText) {
            this.updatePanelSize();
        }
        this.repaint();
        if (this.dragDropCreation) {
            if (this.isText) {
                this.delayFocusOnTextArea();
            }
            this.dragDropCreation = false;
        }
    }

    private void delayFocusOnTextArea() {
        new Timer().schedule(new TimerTask(){

            @Override
            public void run() {
                JDocuElement.this.textArea.requestFocusInWindow();
            }
        }, 100L);
    }

    private void updateTextAreaBounds() {
        if (this.localFocus) {
            this.textArea.setBounds(7, TEXT_BORDER_BOTTOM - 7, this.getBounds().width - 14, this.getBounds().height - 30);
        } else {
            this.textArea.setBounds(1, 1, this.getBounds().width - 2, this.getBounds().height - 2);
        }
    }

    private void updatePanelSize() {
        if (this.localFocus) {
            if (this.enlargedPanel) {
                return;
            }
            Rectangle b = this.getBounds();
            // the bar for moving the field is above the text (7 px frame on the other sides)
            this.setBounds(b.x - 7 + 1, b.y - (TEXT_BORDER_BOTTOM - 7) + 1, b.width - 2 + 14, b.height - 2 + 30);
            this.enlargedPanel = true;
        } else {
            if (!this.enlargedPanel) {
                return;
            }
            Rectangle b = this.getBounds();
            this.setBounds(b.x - 1 + 7, b.y - 1 + (TEXT_BORDER_BOTTOM - 7), b.width - 14 + 2, b.height - 30 + 2);
            this.enlargedPanel = false;
        }
        this.updateTextAreaBounds();
    }

    private Polygon invertRectangle(Rectangle R, int corner) {
        Polygon clip;
        int extW = this.getBounds().width;
        int extH = this.getBounds().height;
        int inW = R.width;
        int inH = R.height;
        if (corner == 0) {
            clip = new Polygon(new int[]{0, extW, extW, 0, 0, R.x, R.x, R.x + inW, R.x + inW, 0}, new int[]{0, 0, extH, extH, R.y, R.y, R.y + inH, R.y + inH, R.y, R.y}, 10);
        } else {
            int ct = corner;
            int cb = corner + 1;
            clip = new Polygon(new int[]{0, extW, extW, 0, 0, R.x + ct, R.x, R.x, R.x + cb, R.x + inW - cb, R.x + inW, R.x + inW, R.x + inW - ct, 0}, new int[]{0, 0, extH, extH, R.y, R.y, R.y + ct, R.y + inH - cb, R.y + inH, R.y + inH, R.y + inH - cb, R.y + ct, R.y, R.y}, 14);
        }
        return clip;
    }

    /** dark mode: light text instead of dark text (colours that are already light stay) */
    private static Color dunkelText(Color c) {
        double l = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
        if (l >= 0.5) {
            return c;
        }
        return filius.modern.Theme.mix(c, new Color(0xE6EDF4), l < 0.15 ? 1.0 : 0.55);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fuellung = this.transparentColor;
        Color fest = this.opaqueColor;
        Color rand = this.borderColor;
        if (filius.modern.Theme.dunkel() && this.referenceColor != null) {
            if (this.isText) {
                Color t = dunkelText(this.referenceColor);
                if (!t.equals(this.textArea.getForeground())) {
                    this.textArea.setForeground(t);
                }
            } else {
                // a light field becomes a dark field with a hint of its colour
                Color f = filius.modern.Theme.mix(new Color(0x18222D), this.referenceColor, 0.05);
                fest = f;
                fuellung = new Color(f.getRed(), f.getGreen(), f.getBlue(), 200);
                rand = filius.modern.Theme.mix(new Color(0x31404F), this.referenceColor, 0.22);
            }
        } else if (this.isText && this.referenceColor != null && !this.referenceColor.equals(this.textArea.getForeground())) {
            this.textArea.setForeground(this.referenceColor);
        }
        if (this.isText) {
            if (this.enabled) {
                if (this.localFocus) {
                    g2.setColor(panelFocusColor);
                    Shape prevClip = g2.getClip();
                    g2.setClip(this.invertRectangle(this.textArea.getBounds(), 0));
                    g2.fillRoundRect(1, 1, this.getWidth() - 2, this.getHeight() - 2, 10, 10);
                    g2.setClip(prevClip);
                    g2.setColor(borderFocusColor);
                    BasicStroke stroke = new BasicStroke(2.0f);
                    g2.setStroke(stroke);
                    g2.drawRoundRect(1, 1, this.getWidth() - 3, this.getHeight() - 3, 10, 10);
                    // grip: the bar above the text moves the field
                    g2.setColor(new Color(0x2E6BD8));
                    int gx = this.getWidth() / 2 - 12;
                    int gy = (TEXT_BORDER_BOTTOM - 7) / 2 - 2;
                    for (int r = 0; r < 2; ++r) {
                        for (int c = 0; c < 6; ++c) {
                            g2.fillOval(gx + c * 5, gy + r * 5, 3, 3);
                        }
                    }
                } else {
                    g2.setColor(filius.modern.Theme.dunkel() ? new Color(0x56687C) : textPanelBorderNormalColor);
                    BasicStroke stroke = new BasicStroke(1.0f);
                    g2.setStroke(stroke);
                    g2.drawRect(this.textArea.getX() - 1, this.textArea.getY() - 1, this.textArea.getWidth() + 1, this.textArea.getHeight() + 1);
                }
            }
        } else if (this.enabled && this.localFocus) {
            g2.setColor(panelFocusColor);
            Shape prevClip = g2.getClip();
            Rectangle R = this.getBounds();
            R.x = 0;
            R.y = 0;
            R.grow(-7, -7);
            g2.setClip(this.invertRectangle(R, 3));
            g2.fillRoundRect(1, 1, this.getWidth() - 2, this.getHeight() - 2, 10, 10);
            g2.setClip(prevClip);
            g2.setColor(fuellung);
            g2.fillRoundRect(R.x, R.y, R.width, R.height, 10, 10);
            g2.setColor(borderFocusColor);
            BasicStroke stroke = new BasicStroke(2.0f);
            g2.setStroke(stroke);
            g2.drawRoundRect(1, 1, this.getWidth() - 3, this.getHeight() - 3, 10, 10);
        } else {
            g2.setColor(this.enabled ? fuellung : fest);
            g2.fillRoundRect(1, 1, this.getWidth() - 2, this.getHeight() - 2, 10, 10);
            g2.setColor(rand);
            BasicStroke stroke = new BasicStroke(1.0f);
            g2.setStroke(stroke);
            g2.drawRoundRect(1, 1, this.getWidth() - 3, this.getHeight() - 3, 10, 10);
        }
    }

    private class JDocuTextArea
    extends JTextArea {
        private static final long serialVersionUID = 1L;

        private JDocuTextArea() {
            this.setBackground(transparent);
            this.setEnabled(JDocuElement.this.enabled);
            this.setWrapping();
            this.initListeners();
        }

        private void initListeners() {
            this.addKeyListener(new KeyAdapter(){

                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case 127: {
                            if (e.getModifiersEx() != 128) break;
                            JDocuElement.this.delete();
                            break;
                        }
                        case 27: {
                            JDocuElement.this.setLocalFocus(false);
                            JDocuTextArea.this.getRootPane().requestFocusInWindow();
                        }
                    }
                }
            });
            this.addMouseListener(new MouseInputAdapter(){

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (JDocuElement.this.enabled) {
                        JDocuElement.this.setPanelCursor(2);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (JDocuElement.this.enabled) {
                        JDocuElement.this.setPanelCursor(0);
                    }
                }
            });
            this.addMouseListener(new MouseInputAdapter(){

                @Override
                public void mousePressed(MouseEvent e) {
                    JDocuTextArea.this.requestFocusInWindow();
                    if (JDocuElement.this.enabled && e.getButton() == 3) {
                        JDocuElement.this.showPopupMenu(e.getX(), e.getY());
                    }
                }
            });
            this.addFocusListener(new FocusListener(){

                @Override
                public void focusGained(FocusEvent e) {
                    JDocuElement.this.setLocalFocus(true);
                }

                @Override
                public void focusLost(FocusEvent e) {
                }
            });
        }

        private void setWrapping() {
            this.getDocument().addDocumentListener(new DocumentListener(){

                @Override
                public void insertUpdate(DocumentEvent e) {
                    JDocuTextArea.this.setLineWrap(true);
                    JDocuTextArea.this.setWrapStyleWord(true);
                    JDocuTextArea.this.getDocument().removeDocumentListener(this);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
        }
    }
}

