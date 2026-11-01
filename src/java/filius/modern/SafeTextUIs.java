package filius.modern;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.formdev.flatlaf.ui.FlatEditorPaneUI;
import com.formdev.flatlaf.ui.FlatTextPaneUI;

/**
 * Filius creates parts of its window outside the Swing event thread and updates HTML views
 * (help, web browser, e-mail) from simulation threads. FlatLaf normally lets HTML text inherit
 * the component font ("honorDisplayProperties"), which triggers asynchronous style updates and can
 * dead-lock with Filius' threading. These UIs keep FlatLaf's look but restore the original Swing
 * behaviour; the modern font is given to HTML via the default style sheet instead.
 */
public final class SafeTextUIs {

    private SafeTextUIs() {}

    public static void install() {
        UIManager.put("EditorPaneUI", EditorPaneUI.class.getName());
        UIManager.put("TextPaneUI", TextPaneUI.class.getName());
        try {
            StyleSheet css = new HTMLEditorKit().getStyleSheet();
            String family = Theme.font(java.awt.Font.PLAIN, 13f).getFamily();
            css.addRule("body { font-family: '" + family + "', sans-serif; color: #1C2B3A; }");
            css.addRule("h1, h2, h3 { color: #1C2B3A; }");
            css.addRule("a { color: #2E6BD8; }");
        } catch (Throwable t) {
            // keep the default HTML style
        }
    }

    public static class EditorPaneUI extends FlatEditorPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new EditorPaneUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            getComponent().putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.FALSE);
        }
    }

    public static class TextPaneUI extends FlatTextPaneUI {
        public static ComponentUI createUI(JComponent c) {
            return new TextPaneUI();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            getComponent().putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.FALSE);
        }
    }
}
