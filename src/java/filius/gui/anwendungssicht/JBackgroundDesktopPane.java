/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JBackgroundDesktopPane of Filius 2.9.3 (GPL).
 * Changed: the desktop wallpaper of a PC or laptop is no longer a scaled picture. The gradient is painted in code
 * (filius.modern.Tapete) and the network motif keeps its size in the lower right corner, so the desktop stays sharp
 * when the window of the computer is made larger or maximized. Other large pictures are scaled to cover the pane,
 * small patterns are tiled as before.
 */
package filius.gui.anwendungssicht;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;

import filius.modern.Tapete;

public class JBackgroundDesktopPane extends JDesktopPane {
    private static final long serialVersionUID = 1L;
    /** pictures at least this large are scaled instead of tiled */
    private static final int BILD = 300;
    private ImageIcon backgroundImage = new ImageIcon(this.getClass().getResource("/gfx/allgemein/menue_hg.png"));
    private boolean repeatBG = true;
    /** the desktop wallpaper: gradient in code + motif at its own size */
    private boolean tapete;
    /** the painted gradient for the current size and screen scale (painting it is too slow for every repaint) */
    private transient BufferedImage verlauf;
    private int verlaufW;
    private int verlaufH;
    private double verlaufSx;
    private double verlaufSy;

    public void setBackgroundImage(String dateiname) {
        this.backgroundImage = new ImageIcon(this.getClass().getResource("/" + dateiname));
        this.tapete = Tapete.istTapete(dateiname);
        this.verlauf = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Insets ins = this.getInsets();
        int w = this.getWidth() - ins.left - ins.right;
        int h = this.getHeight() - ins.top - ins.bottom;
        if (this.tapete && w > 0 && h > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.translate(ins.left, ins.top);
                g2.clipRect(0, 0, w, h);
                maleVerlauf(g2, w, h);
                Tapete.netz(g2, w, h);
            } finally {
                g2.dispose();
            }
            return;
        }
        int bw = this.backgroundImage.getIconWidth();
        int bh = this.backgroundImage.getIconHeight();
        if (bw <= 0 || bh <= 0) {
            return;
        }
        if (bw >= BILD && bh >= BILD) {
            // cover: the picture fills the whole pane, the middle stays visible
            double s = Math.max(Math.max((double) w / bw, (double) h / bh), 1.0);
            int sw = (int) Math.ceil(bw * s);
            int sh = (int) Math.ceil(bh * s);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.clipRect(ins.left, ins.top, w, h);
                Image img = this.backgroundImage.getImage();
                g2.drawImage(img, ins.left + (w - sw) / 2, ins.top + (h - sh) / 2, sw, sh, this);
            } finally {
                g2.dispose();
            }
        } else if (this.repeatBG) {
            for (int a = 0; a <= this.getHeight() / bh; ++a) {
                for (int i = 0; i <= this.getWidth() / bw; ++i) {
                    this.backgroundImage.paintIcon(this, g, ins.left + i * bw, ins.top + a * bh);
                }
            }
        } else {
            this.backgroundImage.paintIcon(this, g, ins.left, ins.top);
        }
    }

    /**
     * The gradient, painted once per size in the pixels of the screen (sharp also at 125 %, 150 %, 200 %). While the
     * window is being resized, the last gradient is stretched (a gradient stays smooth when stretched) and painted
     * anew once the size has not changed for a moment, so dragging the window edge stays fluid on large screens.
     */
    private void maleVerlauf(Graphics2D g2, int w, int h) {
        AffineTransform t = g2.getTransform();
        double sx = Math.abs(t.getScaleX()) > 0 ? Math.abs(t.getScaleX()) : 1;
        double sy = Math.abs(t.getScaleY()) > 0 ? Math.abs(t.getScaleY()) : 1;
        boolean passt = this.verlauf != null && this.verlaufW == w && this.verlaufH == h && this.verlaufSx == sx && this.verlaufSy == sy;
        if (!passt) {
            if (this.verlauf == null || this.verlaufSx != sx || this.verlaufSy != sy || this.ruhig) {
                neuerVerlauf(w, h, sx, sy);
            }
            // further changes within the next moment (dragging the edge) only stretch the gradient
            this.ruhig = false;
            this.neuMalen.restart();
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(this.verlauf, 0, 0, w, h, null);
    }

    /** true when the size has been still since the last gradient: the next new size is painted at once */
    private boolean ruhig = true;
    /** paints the gradient anew after resizing has stopped for 180 ms */
    private final javax.swing.Timer neuMalen = new javax.swing.Timer(180, e -> {
        this.ruhig = true;
        this.repaint();
    });

    {
        this.neuMalen.setRepeats(false);
    }

    private void neuerVerlauf(int w, int h, double sx, double sy) {
        int pw = Math.max(1, (int) Math.ceil(w * sx));
        int ph = Math.max(1, (int) Math.ceil(h * sy));
        BufferedImage b = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D bg = b.createGraphics();
        try {
            bg.scale(pw / (double) w, ph / (double) h);
            Tapete.verlauf(bg, w, h);
        } finally {
            bg.dispose();
        }
        Tapete.entbaenderung(b);
        this.verlauf = b;
        this.verlaufW = w;
        this.verlaufH = h;
        this.verlaufSx = sx;
        this.verlaufSy = sy;
    }
}
