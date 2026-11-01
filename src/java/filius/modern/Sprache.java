package filius.modern;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;

import filius.Main;
import filius.gui.GUIContainer;
import filius.gui.GUIMainMenu;
import filius.gui.JMainFrame;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.SzenarioVerwaltung;

/**
 * Filius (RBZ Wirtschaft Kiel Version) runs in German or English. The switch "DE | EN" in the
 * header changes to the other language: Filius asks first, offers to save unsaved work, then ends
 * like a normal exit (the language is written to the user configuration) and starts again with the
 * saved project.
 */
public final class Sprache {

    private Sprache() {}

    /** Called by the language button in the header. */
    public static void umschalten() {
        boolean englisch = Texts.englisch();
        if (Prefs.iniLocale() != null) {
            // config/filius.ini fixes the language for the whole installation
            JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), Texts.format("lang.fixed", Prefs.iniLocale()), Texts.get("lang.title"),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Object[] optionen = { Texts.get("lang.yes"), Texts.get("dlg.cancel") };
        int wahl = JOptionPane.showOptionDialog(JMainFrame.getJMainFrame(), Texts.get("lang.question"), Texts.get("lang.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen[0]);
        if (wahl != 0) {
            return;
        }
        if (!ungespeichertKlaeren()) {
            return;
        }
        neuStarten(englisch ? Locale.GERMANY : Locale.UK);
    }

    /** Unsaved work: save it, discard it or stay. False if the student cancels. */
    private static boolean ungespeichertKlaeren() {
        SzenarioVerwaltung sv = SzenarioVerwaltung.getInstance();
        boolean leer = GUIContainer.getGUIContainer().getKnotenItems().isEmpty() && GUIContainer.getGUIContainer().getDocuItems().isEmpty();
        if (!sv.istGeaendert() || leer && sv.holePfad() == null) {
            return true;
        }
        Object[] optionen = { Texts.get("dlg.save"), Texts.get("dlg.nosave"), Texts.get("dlg.cancel") };
        int wahl = JOptionPane.showOptionDialog(JMainFrame.getJMainFrame(), Texts.get("lang.unsaved"), Texts.get("lang.title"),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, optionen, optionen[0]);
        if (wahl == 0) {
            return GUIMainMenu.speichernMitDialog();
        }
        if (wahl == 1) {
            // Filius would otherwise keep the unsaved state for the next start
            sv.setCurrentFile(sv.holePfad(), false);
            return true;
        }
        return false;
    }

    /** Ends Filius like the normal exit and starts it again in the new language. */
    static void neuStarten(Locale neu) {
        Information.getInformation().setLocale(neu);
        List<String> befehl = befehl();
        if (befehl != null) {
            // runs after Main.beenden() has written the configuration with the new language
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(befehl);
                    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                    pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                    File wd = new File(System.getProperty("user.dir"));
                    if (wd.isDirectory()) {
                        pb.directory(wd);
                    }
                    pb.start();
                } catch (Exception e) {
                    // the language is switched anyway, the student starts Filius again
                }
            }, "filius-neustart"));
        } else {
            JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), Texts.get("lang.error"), Texts.get("lang.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        Main.beenden();
    }

    /** The command that started this Filius: same Java, same options, same program; no project file. */
    private static List<String> befehl() {
        try {
            String home = System.getProperty("java.home");
            boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
            File javaDatei = new File(new File(home, "bin"), windows ? "javaw.exe" : "java");
            if (!javaDatei.isFile()) {
                javaDatei = new File(new File(home, "bin"), windows ? "java.exe" : "java");
            }
            if (!javaDatei.isFile()) {
                return null;
            }
            List<String> b = new ArrayList<>();
            b.add(javaDatei.getAbsolutePath());
            try {
                for (String a : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                    if (!a.startsWith("-agentlib") && !a.startsWith("-javaagent") && !a.startsWith("-Xdebug") && !a.startsWith("-Xrunjdwp")) {
                        b.add(a);
                    }
                }
            } catch (Throwable t) {
                // without java.management: no extra options
            }
            File quelle = new File(ModernFilius.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (quelle.isFile() && quelle.getName().endsWith(".jar")) {
                b.add("-jar");
                b.add(quelle.getAbsolutePath());
            } else {
                b.add("-cp");
                b.add(System.getProperty("java.class.path"));
                b.add(ModernFilius.class.getName());
            }
            // options of Filius, but not the project file it was started with
            String[] args = ModernFilius.argumente();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if (a.startsWith("-")) {
                    b.add(a);
                    if ((a.equals("-w") || a.equals("-wd") || a.equals("--wd") || a.equals("-r")) && i + 1 < args.length) {
                        b.add(args[++i]);
                    }
                }
            }
            // Filius keeps nothing by itself: the saved project is opened again after the restart
            String projekt = SzenarioVerwaltung.getInstance().holePfad();
            if (projekt != null && new File(projekt).isFile()) {
                b.add(new File(projekt).getAbsolutePath());
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }
}
