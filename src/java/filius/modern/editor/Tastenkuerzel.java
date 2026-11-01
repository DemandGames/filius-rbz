package filius.modern.editor;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Filius' main window handles Ctrl+N, Ctrl+O, Ctrl+S, Ctrl+D, Ctrl+R, Ctrl+arrow keys and more for the whole
 * program, also while somebody types in a text area. In the text editor these keys belong to the text
 * (Ctrl+S saves the file, Ctrl+arrow moves by words). This dispatcher is installed before the main window
 * exists, so it runs first, and hands such keys directly to the editor.
 */
public final class Tastenkuerzel {

    private Tastenkuerzel() {}

    public static void install() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED || (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) {
                return false;
            }
            Object quelle = e.getSource();
            if (quelle instanceof CodeFlaeche || quelle instanceof Suchleiste) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent((Component) quelle, e);
                return true;
            }
            return false;
        });
    }
}
