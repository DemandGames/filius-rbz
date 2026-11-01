package filius.modern;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * A grey toolbar icon that follows the light and dark mode: in the dark mode its dark grey strokes are drawn in
 * light grey; coloured parts stay as they are.
 */
public final class ThemeIcon extends ImageIcon {

    private static final long serialVersionUID = 1L;
    private final Image hell;
    private Image dunkel;

    public ThemeIcon(Image hell, String beschreibung) {
        super(hell, beschreibung);
        this.hell = hell;
    }

    /** An icon with a ready-made picture for the dark mode (e.g. name_dunkel.png). */
    public ThemeIcon(Image hell, Image dunkel, String beschreibung) {
        this(hell, beschreibung);
        this.dunkel = dunkel;
    }

    private Image aktuell() {
        if (!Theme.dunkel()) {
            return this.hell;
        }
        if (this.dunkel == null) {
            this.dunkel = umfaerben(this.hell);
        }
        return this.dunkel;
    }

    @Override
    public Image getImage() {
        return this.aktuell();
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(this.aktuell(), x, y, this.getIconWidth(), this.getIconHeight(), c);
    }

    static Image umfaerben(Image bild) {
        if (bild instanceof MultiResolutionImage) {
            List<Image> varianten = new ArrayList<>();
            for (Image v : ((MultiResolutionImage) bild).getResolutionVariants()) {
                varianten.add(umfaerbenEinzeln(v));
            }
            return new BaseMultiResolutionImage(varianten.toArray(new Image[0]));
        }
        return umfaerbenEinzeln(bild);
    }

    private static Image umfaerbenEinzeln(Image bild) {
        int w = bild.getWidth(null);
        int h = bild.getHeight(null);
        if (w <= 0 || h <= 0) {
            return bild;
        }
        BufferedImage aus = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = aus.createGraphics();
        g.drawImage(bild, 0, 0, null);
        g.dispose();
        Color hellste = new Color(0xE6EDF4);
        Color mitte = new Color(0x8FA0B2);
        float[] hsb = new float[3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = aus.getRGB(x, y);
                int a = argb >>> 24;
                if (a == 0) {
                    continue;
                }
                int r = (argb >> 16) & 0xFF;
                int gr = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                Color.RGBtoHSB(r, gr, b, hsb);
                // only greys and dark slate (the ink of the icons), not the coloured accents
                if (hsb[1] > 0.45 || hsb[2] > 0.75) {
                    continue;
                }
                double t = Math.min(1.0, hsb[2] / 0.6);
                int nr = (int) Math.round(hellste.getRed() + (mitte.getRed() - hellste.getRed()) * t);
                int ng = (int) Math.round(hellste.getGreen() + (mitte.getGreen() - hellste.getGreen()) * t);
                int nb = (int) Math.round(hellste.getBlue() + (mitte.getBlue() - hellste.getBlue()) * t);
                aus.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return aus;
    }
}
