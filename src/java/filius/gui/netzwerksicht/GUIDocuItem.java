/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIDocuItem of Filius 2.9.3 (decompiled, GPL).
 * Changed: a structure field (Gliederungsfläche, RECT) can have a label of its own (the "+" at its top left,
 * see JDocuElement). The label is stored in the property text, the type stays RECT, so the project files keep
 * their format: Filius 2.9.3 opens them and simply ignores the label of a structure field.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.netzwerksicht;

import filius.gui.netzwerksicht.JDocuElement;
import java.awt.Color;
import java.awt.Font;

public class GUIDocuItem {
    public static final int RECT = 1;
    public static final int TEXT = 2;
    private int type;
    private String text;
    private int x;
    private int y;
    private int width;
    private int height;
    private Color color;
    private Font font;
    private JDocuElement elem;

    public static GUIDocuItem createDocuItem(JDocuElement elem) {
        GUIDocuItem item = new GUIDocuItem();
        item.type = elem.isTextElement() ? 2 : 1;
        item.text = item.type == 2 ? elem.getText() : elem.getTitel();
        item.x = elem.getX();
        item.y = elem.getY();
        item.width = elem.getWidth();
        item.height = elem.getHeight();
        item.color = elem.getColor();
        item.font = elem.getFont();
        item.elem = elem;
        return item;
    }

    public JDocuElement asDocuElement() {
        if (this.elem == null) {
            this.elem = new JDocuElement(this.type == 2);
            if (this.type == 2) {
                this.elem.setText(this.text);
            } else {
                this.elem.setTitel(this.text);
            }
            this.elem.setBounds(this.x, this.y, this.width, this.height);
            this.elem.setColor(this.color);
            this.elem.setFont(this.font);
            this.elem.setEnabled(false);
        }
        return this.elem;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /** the text of a text field, or the label of a structure field (null if it has none) */
    public String getText() {
        if (this.elem != null) {
            this.text = this.type == 2 ? this.elem.getText() : this.elem.getTitel();
        }
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
        if (this.elem != null) {
            if (this.type == 2) {
                this.elem.setText(this.text);
            } else {
                this.elem.setTitel(this.text);
            }
        }
    }

    public int getX() {
        if (this.elem != null) {
            this.x = this.elem.getX();
        }
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
        if (this.elem != null) {
            this.elem.setBounds(x, this.y, this.width, this.height);
        }
    }

    public int getY() {
        if (this.elem != null) {
            this.y = this.elem.getY();
        }
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
        if (this.elem != null) {
            this.elem.setBounds(this.x, y, this.width, this.height);
        }
    }

    public int getWidth() {
        if (this.elem != null) {
            this.width = this.elem.getWidth();
        }
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
        if (this.elem != null) {
            this.elem.setBounds(this.x, this.y, width, this.height);
        }
    }

    public int getHeight() {
        if (this.elem != null) {
            this.height = this.elem.getHeight();
        }
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
        if (this.elem != null) {
            this.elem.setBounds(this.x, this.y, this.width, height);
        }
    }

    public Color getColor() {
        if (this.elem != null) {
            this.color = this.elem.getColor();
        }
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
        if (this.elem != null) {
            this.elem.setColor(color);
        }
    }

    public Font getFont() {
        if (this.elem != null) {
            this.font = this.elem.getFont();
        }
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
        if (this.elem != null) {
            this.elem.setFont(font);
        }
    }
}

