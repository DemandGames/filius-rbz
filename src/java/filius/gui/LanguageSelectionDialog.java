/*
 * Filius (RBZ Wirtschaft Kiel Version): replaces LanguageSelectionDialog of Filius 2.9.3 (GPL).
 * This version runs in German or English with the standard device pictures. At the first start it
 * runs in German without asking; the button "DE" / "EN" in the header switches the language with a
 * restart (filius.modern.Sprache). The settings dialog of the original (language, picture style of
 * the devices, experimental layer view) is not used any more.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui;

import filius.rahmenprogramm.Information;
import java.util.Locale;
import javax.swing.JDialog;

public class LanguageSelectionDialog
extends JDialog {
    private static final long serialVersionUID = 1L;

    private LanguageSelectionDialog() {
    }

    public static void selectLanguage(boolean restart) {
        if (restart) {
            filius.modern.Sprache.umschalten();
        } else if (Information.getInformation().getLocale() == null) {
            Information.getInformation().setLocale(Locale.GERMANY);
        }
    }
}
