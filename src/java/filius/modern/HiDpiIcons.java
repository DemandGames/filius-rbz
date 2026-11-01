package filius.modern;

import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.MultiResolutionImage;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Makes Filius' PNG icons sharp on high-DPI screens (125 %, 150 %, 200 % scaling).
 *
 * Filius loads icons with {@code new ImageIcon(URL)}; such icons carry their URL as description.
 * If a "name@2x.png" exists next to "name.png", the icon is replaced by a multi-resolution icon
 * of the same logical size, so layouts do not change.
 */
public final class HiDpiIcons {

    private static final Map<String, Object> cache = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    private HiDpiIcons() {}

    public static Icon upgrade(Icon icon) {
        if (!(icon instanceof ImageIcon) || icon instanceof ThemeIcon) {
            return icon;
        }
        ImageIcon ii = (ImageIcon) icon;
        String desc = ii.getDescription();
        if (desc == null || !desc.endsWith(".png") || !desc.contains("gfx/")) {
            return icon;
        }
        Image bild = ii.getImage();
        boolean geaendert = false;
        if (!(bild instanceof MultiResolutionImage)) {
            Object cached = cache.computeIfAbsent(desc, HiDpiIcons::loadPair);
            // keep the logical size of the icon that Filius asked for
            if (cached != NONE && passt((Image) cached, ii)) {
                bild = (Image) cached;
                geaendert = true;
            }
        }
        // pictures with a dark variant (name_dunkel.png) follow the dark mode
        Object dunkel = dunkelCache.computeIfAbsent(desc, HiDpiIcons::loadDunkel);
        if (dunkel != NONE && passt((Image) dunkel, ii)) {
            return new ThemeIcon(bild, (Image) dunkel, desc);
        }
        return geaendert ? new ImageIcon(bild, desc) : icon;
    }

    private static final Map<String, Object> dunkelCache = new ConcurrentHashMap<>();

    private static boolean passt(Image bild, ImageIcon ii) {
        return bild.getWidth(null) == ii.getIconWidth() && bild.getHeight(null) == ii.getIconHeight();
    }

    private static Object loadDunkel(String desc) {
        String d = desc.substring(0, desc.length() - 4) + "_dunkel.png";
        Object paar = loadPair(d);
        if (paar != NONE) {
            return paar;
        }
        try {
            Image lo = read(new URL(d));
            return lo == null ? NONE : lo;
        } catch (Exception e) {
            return NONE;
        }
    }

    private static Object loadPair(String desc) {
        try {
            URL base = new URL(desc);
            URL hi = new URL(desc.substring(0, desc.length() - 4) + "@2x.png");
            Image lo = read(base);
            Image hiImg = read(hi);
            if (lo == null || hiImg == null) {
                return NONE;
            }
            return new BaseMultiResolutionImage(lo, hiImg);
        } catch (Exception e) {
            return NONE;
        }
    }

    private static Image read(URL url) {
        try (InputStream in = url.openStream()) {
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    /** Loads a bundled icon of Filius (RBZ Wirtschaft Kiel Version) (with @2x variant if available). */
    public static ImageIcon load(String resourcePath) {
        URL url = HiDpiIcons.class.getResource(resourcePath);
        if (url == null) {
            return null;
        }
        ImageIcon icon = (ImageIcon) upgrade(new ImageIcon(url));
        // grey toolbar and mode icons follow the dark mode
        String name = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        if ((name.startsWith("tb-") || name.startsWith("mode-")) && !name.contains("-on")) {
            return new ThemeIcon(icon.getImage(), icon.getDescription());
        }
        return icon;
    }
}
