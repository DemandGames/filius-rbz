/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 */
package filius.modern;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import filius.gui.GUIMainMenu;

/**
 * macOS: a double click on a .fls file in the Finder does not pass the file as a program argument; the
 * system sends an "open files" event instead. This registers for that event and opens the file with the
 * "Load" logic. Files that arrive before the main window exists wait until Filius is ready. On Windows and
 * Linux the event is not supported and nothing is registered.
 */
public final class MacOeffnen {

    private static final List<File> WARTEND = new ArrayList<>();
    private static boolean bereit;

    private MacOeffnen() {}

    static void install() {
        try {
            if (!Desktop.isDesktopSupported()) {
                return;
            }
            Desktop d = Desktop.getDesktop();
            if (!d.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                return;
            }
            d.setOpenFileHandler(e -> {
                for (File f : e.getFiles()) {
                    oeffnen(f);
                }
            });
        } catch (Throwable t) {
            // an integration must never keep Filius from starting
        }
    }

    /** called once the main window is shown: files that arrived earlier are opened now */
    public static void bereit() {
        List<File> jetzt;
        synchronized (WARTEND) {
            bereit = true;
            jetzt = new ArrayList<>(WARTEND);
            WARTEND.clear();
        }
        for (File f : jetzt) {
            oeffnen(f);
        }
    }

    private static void oeffnen(File f) {
        synchronized (WARTEND) {
            if (!bereit) {
                WARTEND.add(f);
                return;
            }
        }
        SwingUtilities.invokeLater(() -> GUIMainMenu.projektOeffnen(f));
    }
}
