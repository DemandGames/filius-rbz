package filius.modern;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Look and feel of Filius (RBZ Wirtschaft Kiel Version). All tuning lives in FiliusLaf.properties (same package),
 * which FlatLaf loads automatically on top of FlatLightLaf.
 */
public class FiliusLaf extends FlatLightLaf {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "Filius RBZ";

    public static boolean setup() {
        return setup(new FiliusLaf());
    }

    @Override
    public String getName() {
        return NAME;
    }
}
