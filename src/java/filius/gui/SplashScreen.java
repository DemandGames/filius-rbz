/*
 * Filius (RBZ Wirtschaft Kiel Version): based on SplashScreen of Filius 2.9.3 (GPL). Same window and timing, with the
 * new splash image and a calmer version line. It is shown before the main window is built, so this
 * is where the settings this version does not offer are fixed: German or English only (any other
 * language becomes German), the standard device pictures and no experimental layer view.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.IconMode;
import filius.rahmenprogramm.Information;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

public class SplashScreen
extends JWindow
implements I18n {
    private static final long serialVersionUID = 1L;

    public SplashScreen(String filename, Frame f) {
        super(f);
        SplashScreen.einstellungenFestlegen();
        if (Texts.englisch() && filename.endsWith(".png") && this.getClass().getResource("/" + filename.replace(".png", "_en.png")) != null) {
            filename = filename.replace(".png", "_en.png");
        }
        JLabel l = new JLabel(filius.modern.HiDpiIcons.upgrade(new ImageIcon(this.getClass().getResource("/" + filename))));
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.SURFACE);
        root.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        root.add(l, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.SURFACE_2);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        JLabel info = new JLabel("Version " + InfoDialog.versionMitStand() + " · " + messages.getString("splashscreen_msg1"));
        info.setForeground(Theme.INK_3);
        info.setFont(Theme.font(Font.PLAIN, 11.5f));
        JLabel loading = new JLabel(Texts.get("splash.loading"));
        loading.setForeground(Theme.PATCH_BLUE);
        loading.setFont(Theme.font(Font.BOLD, 11.5f));
        JLabel edited = new JLabel(InfoDialog.EDITED_BY);
        edited.setForeground(Theme.INK_3);
        edited.setFont(Theme.font(Font.PLAIN, 11.5f));
        // line 1: version and licence, line 2: editor and loading note
        JPanel second = new JPanel(new BorderLayout(16, 0));
        second.setOpaque(false);
        second.add(edited, BorderLayout.WEST);
        second.add(loading, BorderLayout.EAST);
        JPanel lines = new JPanel(new GridLayout(2, 1, 0, 1));
        lines.setOpaque(false);
        lines.add(info);
        lines.add(second);
        footer.add(lines, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        this.setContentPane(root);
        this.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = this.getSize();
        this.setLocation(screenSize.width / 2 - size.width / 2, screenSize.height / 2 - size.height / 2);
        // safety net: the start window never stays longer than 30 seconds, whatever happens during the start
        javax.swing.Timer notfall = new javax.swing.Timer(30000, e -> this.schliessen());
        notfall.setRepeats(false);
        notfall.start();
    }

    private volatile boolean geschlossen;

    /** Hides the start window for good: right away and once more on the event thread (safe to call twice). */
    public void schliessen() {
        if (this.geschlossen) {
            return;
        }
        this.geschlossen = true;
        try {
            this.setAlwaysOnTop(false);
            this.setVisible(false);
        } catch (RuntimeException e) {
            // the event thread below does it
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            this.setVisible(false);
            this.dispose();
        });
    }

    private static void einstellungenFestlegen() {
        Information info = Information.getInformation();
        Locale l = info.getLocale();
        if (l == null || !"en".equals(l.getLanguage())) {
            info.setLocale(Locale.GERMANY);
        } else {
            info.setLocale(Locale.UK);
        }
        info.setIconMode(IconMode.DEFAULT);
        info.setLayerVisualization(false);
    }
}
