package filius.modern;

import javax.swing.UIManager;

/**
 * Entry point of Filius (RBZ Wirtschaft Kiel Version).
 *
 * Installs the modern look and feel and the effect layer, then starts the unchanged Filius
 * program ({@code filius.Main}). Everything that Filius can do stays available; only the
 * presentation changes.
 */
public final class ModernFilius {

    private static String[] argumente = new String[0];

    private ModernFilius() {}

    /** the command line of this start (used when Filius restarts in another language) */
    static String[] argumente() {
        return argumente.clone();
    }

    public static void main(String[] args) {
        argumente = args == null ? new String[0] : args.clone();
        // Windows: draw without Direct3D. With two screens (often with different scaling) the Direct3D pipeline of
        // Java garbled the view when a window was dragged to the other screen (version 31). A value given on the
        // command line (-Dsun.java2d.d3d=true) still wins. Other systems ignore the setting.
        if (System.getProperty("sun.java2d.d3d") == null) {
            System.setProperty("sun.java2d.d3d", "false");
        }
        // the web bridge must be in place before anything creates an http URL
        try {
            filius.modern.web.FxSupport.prepareJvm();
        } catch (Throwable t) {
            System.err.println("Filius RBZ: web bridge not available: " + t);
        }
        try {
            Prefs.load();
            Theme.registerFonts();
            // Windows 10/11: FlatLaf draws a light title bar that matches the app
            System.setProperty("flatlaf.useWindowDecorations", "true");
            System.setProperty("flatlaf.menuBarEmbedded", "false");
            // light or dark mode as chosen last time (sun/moon toggle in the header)
            DunkelModus.beimStart();
            Enhancer.install();
            // before Filius' main window: shortcuts in the text editor belong to the text
            filius.modern.editor.Tastenkuerzel.install();
            // macOS: .fls files opened from the Finder arrive as an event, not as an argument
            MacOeffnen.install();
        } catch (Throwable t) {
            // never block Filius from starting – fall back to the classic look
            System.err.println("Filius RBZ: could not install modern look: " + t);
        }
        filius.Main.main(args);
    }
}
