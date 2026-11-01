package filius.modern.editor;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Locale;

import filius.modern.Theme;

/**
 * Font of the text editor: Consolas (the code font of Windows, as in VS Code and Notepad++), which is hinted for
 * the screen and keeps an even spacing at every display scale. Where Consolas is missing (Linux, Mac), the
 * bundled JetBrains Mono is used.
 */
final class EditorSchrift {

    private static Boolean consolas;

    private EditorSchrift() {
    }

    static boolean consolas() {
        if (consolas == null) {
            boolean da;
            try {
                da = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT)).contains("Consolas");
            } catch (RuntimeException e) {
                da = false;
            }
            consolas = da;
        }
        return consolas;
    }

    /** the code itself */
    static Font code() {
        return consolas() ? new Font("Consolas", Font.PLAIN, 14) : Theme.mono(Font.PLAIN, 13f);
    }

    /** the line numbers */
    static Font zeilen() {
        return consolas() ? new Font("Consolas", Font.PLAIN, 13) : Theme.mono(Font.PLAIN, 12f);
    }
}
