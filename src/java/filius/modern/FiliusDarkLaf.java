package filius.modern;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Dark look and feel of Filius (RBZ Wirtschaft Kiel Version). All tuning lives in FiliusDarkLaf.properties (same
 * package), which FlatLaf loads automatically on top of FlatDarkLaf.
 */
public class FiliusDarkLaf extends FlatDarkLaf {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "Filius RBZ Dark";

    public static boolean setup() {
        return setup(new FiliusDarkLaf());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
