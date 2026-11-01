/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JBackgroundPanel of Filius 2.9.3 (GPL).
 * Changed: in the dark mode the panel uses the dark variant of its picture (name_dunkel.png, e.g. the dot grid of
 * the canvas); plain surfaces without a dark variant are filled with the dark surface colour.
 */
package filius.gui;

import java.awt.Graphics;
import java.awt.Insets;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import filius.modern.Theme;

public class JBackgroundPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private ImageIcon backgroundImage;
    private ImageIcon dunkelImage;
    private boolean dunkelGesucht;
    private String name;
    private boolean repeatBG = true;

    public void setBackgroundImage(String dateiname) {
        this.name = dateiname;
        this.backgroundImage = new ImageIcon(this.getClass().getResource("/" + dateiname));
        this.dunkelImage = null;
        this.dunkelGesucht = false;
    }

    private ImageIcon bild() {
        if (!Theme.dunkel() || this.name == null) {
            return this.backgroundImage;
        }
        if (!this.dunkelGesucht) {
            this.dunkelGesucht = true;
            URL u = this.getClass().getResource("/" + this.name.replace(".png", "_dunkel.png"));
            this.dunkelImage = u == null ? null : new ImageIcon(u);
        }
        return this.dunkelImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Insets ins = this.getInsets();
        ImageIcon bg = this.bild();
        if (bg == null && this.backgroundImage != null && Theme.dunkel()) {
            g.setColor(Theme.SURFACE);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            return;
        }
        if (bg != null) {
            if (this.repeatBG) {
                for (int a = 0; a <= this.getHeight() / bg.getIconHeight(); ++a) {
                    for (int i = 0; i <= this.getWidth() / bg.getIconWidth(); ++i) {
                        bg.paintIcon(this, g, ins.left + i * bg.getIconWidth(), ins.top + a * bg.getIconHeight());
                    }
                }
            } else {
                bg.paintIcon(this, g, ins.left, ins.top);
            }
        }
    }
}
