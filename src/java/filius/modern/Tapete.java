/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.modern;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * The wallpaper of the simulated desktop, sharp at every window size: a soft blue gradient (darker at the upper
 * left, lighter at the lower right) is painted in code, the network motif (gfx/desktop/hintergrund_netz.png, 1x and
 * 2x) keeps its size in the lower right corner. design/backgrounds.py draws the same gradient into the fallback
 * picture gfx/desktop/hintergrundbild.png.
 */
public final class Tapete {

    /** the picture that Filius sets as desktop background; it is replaced by this wallpaper */
    public static final String BILD = "gfx/desktop/hintergrundbild.png";

    /** size of the motif in logical pixels (lower right corner) */
    static final int NETZ_W = 340;
    static final int NETZ_H = 305;

    /** only blue: a little darker at the upper left, a little lighter at the lower right */
    static final Color DUNKEL = new Color(0x1B3769);
    static final Color HELL = new Color(0x3A70CF);
    /** many stops along a soft S-curve, so the gradient has no visible start or end */
    private static final int STUFEN_ANZAHL = 17;
    private static final float[] STUFEN = new float[STUFEN_ANZAHL];
    private static final Color[] FARBEN = new Color[STUFEN_ANZAHL];

    static {
        for (int i = 0; i < STUFEN_ANZAHL; i++) {
            float t = i / (float) (STUFEN_ANZAHL - 1);
            float s = t * t * (3 - 2 * t);
            STUFEN[i] = t;
            FARBEN[i] = new Color(mische(DUNKEL.getRed(), HELL.getRed(), s), mische(DUNKEL.getGreen(), HELL.getGreen(), s),
                    mische(DUNKEL.getBlue(), HELL.getBlue(), s));
        }
    }

    private static int mische(int a, int b, float s) {
        return Math.round(a + (b - a) * s);
    }

    private static Image netz;
    private static boolean netzGeladen;

    private Tapete() {}

    /** true if the picture that Filius asks for is the desktop wallpaper */
    public static boolean istTapete(String dateiname) {
        return dateiname != null && dateiname.replace('\\', '/').endsWith(BILD);
    }

    /** the gradient for a desktop of w x h logical pixels, in the coordinates of g (already scaled for the screen) */
    public static void verlauf(Graphics2D g, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        // blue from the upper left (darker) to the lower right (lighter), like an SVG gradient over the whole box
        g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0), new Point2D.Float(1, 1), STUFEN, FARBEN, CycleMethod.NO_CYCLE,
                ColorSpaceType.SRGB, AffineTransform.getScaleInstance(w, h)));
        g.fillRect(0, 0, w, h);
    }

    /**
     * Light noise of +-1 step against visible bands in the gradient on large screens (8 bits per colour are not
     * enough for a smooth blue over 2000 pixels).
     */
    public static void entbaenderung(BufferedImage b) {
        if (b.getType() != BufferedImage.TYPE_INT_RGB || !(b.getRaster().getDataBuffer() instanceof DataBufferInt)) {
            return;
        }
        int[] px = ((DataBufferInt) b.getRaster().getDataBuffer()).getData();
        int zufall = 0x2545F491;
        for (int i = 0; i < px.length; i++) {
            zufall ^= zufall << 13;
            zufall ^= zufall >>> 17;
            zufall ^= zufall << 5;
            int d = zufall & 3; // 0: one step darker, 3: one step lighter, else unchanged
            if (d == 1 || d == 2) {
                continue;
            }
            int p = px[i];
            // the blue never touches 0 or 255 in any channel, so +-1 cannot overflow
            px[i] = d == 0 ? p - 0x010101 : p + 0x010101;
        }
    }

    /** the network motif in the lower right corner, always at its own size (sharp at 100 %, 150 %, 200 %) */
    public static void netz(Graphics2D g, int w, int h) {
        Image bild = netzBild();
        if (bild == null) {
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(bild, w - NETZ_W, h - NETZ_H, NETZ_W, NETZ_H, null);
    }

    private static synchronized Image netzBild() {
        if (!netzGeladen) {
            netzGeladen = true;
            try {
                BufferedImage lo = lies("/gfx/desktop/hintergrund_netz.png");
                BufferedImage hi = lies("/gfx/desktop/hintergrund_netz@2x.png");
                netz = lo == null ? null : hi == null ? lo : new BaseMultiResolutionImage(lo, hi);
            } catch (Exception e) {
                netz = null;
            }
        }
        return netz;
    }

    private static BufferedImage lies(String pfad) throws Exception {
        URL u = Tapete.class.getResource(pfad);
        return u == null ? null : ImageIO.read(u);
    }
}
