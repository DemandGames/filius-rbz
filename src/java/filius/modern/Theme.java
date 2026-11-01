package filius.modern;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;

/**
 * Design tokens of Filius (RBZ Wirtschaft Kiel Version).
 *
 * The palette is taken from the world of network cabling: every mode has the colour of a patch
 * cable (design = patch blue, simulation = link-LED green, documentation = marker amber).
 */
public final class Theme {

    private Theme() {}

    // ------------------------------------------------------------------ light / dark mode

    private static volatile boolean dunkel;

    /** true while Filius shows the dark mode */
    public static boolean dunkel() {
        return dunkel;
    }

    /** switches all theme colours (the look and feel is switched by {@link DunkelModus}) */
    static void setDunkel(boolean an) {
        dunkel = an;
    }

    /** a colour with a light and a dark value (0xRRGGBB) */
    public static Color farbe(int hell, int dunkel) {
        return new ThemeFarbe(hell, dunkel);
    }

    /** a translucent colour with a light and a dark value (0xAARRGGBB) */
    public static Color farbeAlpha(int hell, int dunkel) {
        return new ThemeFarbe(hell, dunkel, true);
    }

    // neutrals (dark: deep slate like a server room at night)
    public static final Color INK = farbe(0x1C2B3A, 0xE6EDF4);
    public static final Color INK_2 = farbe(0x4A5B6C, 0xADBCCB);
    public static final Color INK_3 = farbe(0x8796A5, 0x7B8C9D);
    public static final Color LINE = farbe(0xDDE4EB, 0x2D3B4A);
    public static final Color LINE_SOFT = farbe(0xEAEFF4, 0x243241);
    public static final Color SURFACE = farbe(0xFFFFFF, 0x17212C);
    public static final Color SURFACE_2 = farbe(0xF6F8FA, 0x1C2835);
    public static final Color CANVAS = farbe(0xF2F5F8, 0x0F1720);
    /** surface of small controls (tracks, segmented controls) */
    public static final Color CONTROL = farbe(0xEEF2F6, 0x223040);
    public static final Color CONTROL_HOVER = farbe(0xE4EAF0, 0x2A3A4C);
    public static final Color LINE_STRONG = farbe(0xB8C4D0, 0x46586B);
    /** selected row of a list or table: a soft blue tint, the text stays readable (version 31, was cyan) */
    public static final Color AUSWAHL = farbe(0xE3ECFA, 0x24395A);
    /** dark title bars (programs in a simulated desktop, dialogs): slate in both modes */
    public static final Color CHROME = farbe(0x1C2B3A, 0x0C131A);
    /** text on a coloured or inverted surface: white in light mode, the dark surface in dark mode */
    public static final Color ON_INK = farbe(0xFFFFFF, 0x17212C);

    // patch cable colours (a little brighter on the dark surface)
    public static final Color PATCH_BLUE = farbe(0x2E6BD8, 0x4C8DF6);
    public static final Color LED_GREEN = farbe(0x13A35A, 0x26BD6C);
    public static final Color LED_GREEN_BRIGHT = farbe(0x1FD173, 0x35E58A);
    public static final Color MARKER_AMBER = farbe(0xE39B12, 0xF2AE30);
    public static final Color SIGNAL_RED = farbe(0xDB3B3B, 0xF06262);

    public static final Color CABLE = farbe(0x5B6B7C, 0x8A9CAF);
    public static final Color CABLE_WIRELESS = farbe(0x9AA8B6, 0x5E7286);

    public static final int MODE_DESIGN = 1;
    public static final int MODE_SIMULATION = 2;
    public static final int MODE_DOCUMENTATION = 3;

    public static final String FONT_FAMILY = "Atkinson Hyperlegible";
    public static final String MONO_FAMILY = "JetBrains Mono";

    private static boolean fontsRegistered;

    public static Color modeColor(int mode) {
        switch (mode) {
            case MODE_SIMULATION:
                return LED_GREEN;
            case MODE_DOCUMENTATION:
                return MARKER_AMBER;
            default:
                return PATCH_BLUE;
        }
    }

    public static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(Math.max(0, Math.min(1, a)) * 255));
    }

    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    /** Registers the bundled Atkinson Hyperlegible fonts. Returns true if the family is usable. */
    public static synchronized boolean registerFonts() {
        if (fontsRegistered) {
            return true;
        }
        String[] files = { "AtkinsonHyperlegible-Regular", "AtkinsonHyperlegible-Bold", "AtkinsonHyperlegible-Italic",
                "AtkinsonHyperlegible-BoldItalic", "JetBrainsMono-Regular", "JetBrainsMono-Bold" };
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int ok = 0;
        for (String f : files) {
            try (InputStream in = Theme.class.getResourceAsStream("/filius/modern/fonts/" + f + ".ttf")) {
                if (in != null) {
                    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
                    ok++;
                }
            } catch (IOException | FontFormatException e) {
                // fall back to the platform default font
            }
        }
        fontsRegistered = ok > 0;
        return fontsRegistered;
    }

    public static Font font(int style, float size) {
        return new Font(fontsRegistered ? FONT_FAMILY : Font.SANS_SERIF, style, Math.round(size)).deriveFont(size);
    }

    public static Font mono(int style, float size) {
        return new Font(fontsRegistered ? MONO_FAMILY : Font.MONOSPACED, style, Math.round(size)).deriveFont(size);
    }

    public static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g2;
    }
}
