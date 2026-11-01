package filius.modern;

import java.awt.Color;

/**
 * A colour of the Filius theme that has a light and a dark value. It reports the value of the current mode
 * every time it is asked (Java2D, Swing and FlatLaf read colours through getRGB), so components that once got
 * such a colour follow a switch between light and dark mode without being touched again.
 */
public final class ThemeFarbe extends Color {

    private static final long serialVersionUID = 1L;
    private final int dunkel;

    /** light and dark value as 0xRRGGBB (opaque) */
    public ThemeFarbe(int hell, int dunkel) {
        this(0xFF000000 | hell, 0xFF000000 | dunkel, true);
    }

    /** light and dark value as 0xAARRGGBB */
    public ThemeFarbe(int hell, int dunkel, boolean mitAlpha) {
        super(hell, true);
        this.dunkel = dunkel;
    }

    @Override
    public int getRGB() {
        return Theme.dunkel() ? this.dunkel : super.getRGB();
    }

    @Override
    public int hashCode() {
        return this.getRGB();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Color && ((Color) obj).getRGB() == this.getRGB();
    }
}
