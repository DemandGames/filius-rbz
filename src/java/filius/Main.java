/*
 * Based on filius.Main of Filius 2.9.3 (decompiled, GPL), changed for Filius (RBZ Wirtschaft Kiel Version):
 *
 * - Results are only kept in the project file (.fls) the student saves. The original stores unsaved work
 *   silently in its work folder (~/.filius/edited_scenario.fls) when it closes and opens it, or the last
 *   project, again at the next start. On a terminal server with shared or reset profiles this mixes up
 *   students' work and hides that nothing was saved. Now Filius always starts empty (or with the file it
 *   was started with) and asks "Save / Don't save / Cancel" when it is closed with unsaved changes.
 * - config/filius.ini can name the folder that "Open" and "Save" show first (modern-speicherort).
 */
package filius;

import java.awt.Rectangle;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import filius.gui.GUIContainer;
import filius.gui.GUIMainMenu;
import filius.gui.JMainFrame;
import filius.gui.LanguageSelectionDialog;
import filius.gui.SplashScreen;
import filius.hardware.Verbindung;
import filius.modern.Prefs;
import filius.modern.Texts;
import filius.rahmenprogramm.FiliusArgs;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.IconMode;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.SzenarioVerwaltung;

public class Main implements I18n {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /** files of the original's automatic keeping of unsaved work, removed at the start */
    private static final String[] ALTE_SITZUNG = { "edited_scenario.fls", "lernpfad-sitzung.properties" };

    public static void starten(String szenarioDatei) {
        LOG.debug("starten(" + szenarioDatei + ")");
        try {
            Information.getInformation().loadIni();
        } catch (IOException e1) {
            LOG.warn("ini could not be read: " + e1.getMessage());
        }
        String konfigPfad = Information.getInformation().getArbeitsbereichPfad() + "konfig.xml";
        if (!new File(konfigPfad).exists() && null == Information.getInformation().getLocale()) {
            LanguageSelectionDialog.selectLanguage(false);
        } else {
            try (XMLDecoder xmldec = new XMLDecoder(new BufferedInputStream(Files.newInputStream(Paths.get(konfigPfad))))) {
                Object[] programmKonfig = (Object[]) xmldec.readObject();
                if (programmKonfig != null) {
                    if (programmKonfig.length >= 4) {
                        JMainFrame.getJMainFrame().setBounds((Rectangle) programmKonfig[0]);
                        // programmKonfig[1], the last project, is not opened again
                        if (programmKonfig[2] != null && programmKonfig[3] != null && null == Information.getInformation().getLocale()) {
                            Information.getInformation().setLocale(new Locale((String) programmKonfig[2], (String) programmKonfig[3]));
                        }
                    }
                    if (programmKonfig.length >= 5) {
                        Information.getInformation().setLastOpenedDirectory((String) programmKonfig[4]);
                    }
                    // programmKonfig[5] (unsaved work kept in edited_scenario.fls) is not used any more
                    if (programmKonfig.length >= 7) {
                        if (StringUtils.equals(programmKonfig[6].toString(), IconMode.ENTERPRISE.toString())) {
                            Information.getInformation().setIconMode(IconMode.ENTERPRISE);
                        } else if (StringUtils.equals(programmKonfig[6].toString(), IconMode.SYMBOL.toString())) {
                            Information.getInformation().setIconMode(IconMode.SYMBOL);
                        }
                    }
                    if (programmKonfig.length >= 8 && ((Boolean) programmKonfig[7]).booleanValue()) {
                        Information.getInformation().setLayerVisualization((Boolean) programmKonfig[7]);
                    }
                }
            } catch (Exception e) {
                LOG.info("There is no user configuration available.");
            }
        }
        alteSitzungLoeschen();
        speicherortSetzen();
        UIManager.put("OptionPane.cancelButtonText", messages.getString("main_dlg_CANCEL"));
        UIManager.put("OptionPane.noButtonText", messages.getString("main_dlg_NO"));
        UIManager.put("OptionPane.okButtonText", messages.getString("main_dlg_OK"));
        UIManager.put("OptionPane.yesButtonText", messages.getString("main_dlg_YES"));
        SplashScreen splashScreen = new SplashScreen("gfx/allgemein/splashscreen.png", null);
        splashScreen.setVisible(true);
        splashScreen.setAlwaysOnTop(true);
        long splashTime = System.currentTimeMillis();
        try {
            try {
                if (null != szenarioDatei) {
                    SzenarioVerwaltung.getInstance().laden(szenarioDatei, GUIContainer.getGUIContainer().getKnotenItems(),
                            GUIContainer.getGUIContainer().getCableItems(), GUIContainer.getGUIContainer().getDocuItems());
                }
            } catch (Exception e) {
                LOG.debug("", e);
            }
            GUIContainer.getGUIContainer().setProperty(null);
            GUIContainer.getGUIContainer().updateViewport();
            try {
                Thread.sleep(10L);
            } catch (Exception exception) {
                LOG.error("Caught an unexpected exception.", exception);
            }
            GUIContainer.getGUIContainer().updateCables();
            splashTime = System.currentTimeMillis() - splashTime;
            LOG.debug("Splash Screen shown for " + splashTime + " ms");
            if (splashTime < 1000L) {
                try {
                    Thread.sleep(1000L - splashTime);
                } catch (Exception exception) {
                    LOG.error("Caught an unexpected exception.", exception);
                }
            }
        } finally {
            // the start window always goes, also if something above went wrong
            splashScreen.schliessen();
        }
        // macOS: a file double-clicked in the Finder may have arrived while Filius was starting
        filius.modern.MacOeffnen.bereit();
    }

    /** Removes what older versions kept of unsaved work, so it can never turn up again. */
    private static void alteSitzungLoeschen() {
        for (String name : ALTE_SITZUNG) {
            try {
                Files.deleteIfExists(Paths.get(Information.getInformation().getArbeitsbereichPfad(), name));
            } catch (Exception e) {
                LOG.debug("could not delete " + name, e);
            }
        }
    }

    /** The folder from config/filius.ini (modern-speicherort) is shown first by "Open" and "Save". */
    private static void speicherortSetzen() {
        String ort = Prefs.speicherort();
        if (ort == null) {
            return;
        }
        try {
            File ordner = new File(ort);
            if (!ordner.isDirectory()) {
                ordner.mkdirs();
            }
            if (ordner.isDirectory()) {
                Information.getInformation().setLastOpenedDirectory(ordner.getAbsolutePath());
            }
        } catch (Exception e) {
            LOG.debug("storage folder not usable: " + ort, e);
        }
    }

    /** true if there is something to lose: changes in a project that is not empty or has a file */
    private static boolean ungespeichert() {
        SzenarioVerwaltung sv = SzenarioVerwaltung.getInstance();
        if (!sv.istGeaendert()) {
            return false;
        }
        GUIContainer gc = GUIContainer.getGUIContainer();
        boolean leer = gc.getKnotenItems().isEmpty() && gc.getDocuItems().isEmpty();
        return !leer || sv.holePfad() != null || filius.modern.lernpfad.Lernpfad.begonnen();
    }

    /**
     * Ends Filius. With unsaved changes Filius asks first: save, don't save or cancel (Filius stays
     * open). Nothing is kept automatically.
     */
    public static void beenden() {
        LOG.debug("beenden");
        if (ungespeichert()) {
            Object[] optionen = { Texts.get("dlg.save"), Texts.get("dlg.nosave"), Texts.get("dlg.cancel") };
            int wahl = JOptionPane.showOptionDialog(JMainFrame.getJMainFrame(), Texts.get("exit.question"), Texts.get("exit.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, optionen, optionen[0]);
            if (wahl == 0) {
                if (!GUIMainMenu.speichernMitDialog()) {
                    return;
                }
            } else if (wahl != 1) {
                return;
            }
        }
        GUIContainer.getGUIContainer().getMenu().selectMode(1);
        // the configuration keeps window, language and folder, but neither the project nor unsaved work
        Object[] programmKonfig = new Object[] { JMainFrame.getJMainFrame().getBounds(), null, Information.getInformation().getLocale().getLanguage(),
                Information.getInformation().getLocale().getCountry(), Information.getInformation().getLastOpenedDirectory(), Boolean.FALSE,
                Information.getInformation().getIconMode().toString(), Information.getInformation().isLayerVisualization() };
        String applicationConfigPath = Information.getInformation().getArbeitsbereichPfad() + "konfig.xml";
        try (FileOutputStream fos = new FileOutputStream(applicationConfigPath); XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(fos))) {
            encoder.writeObject(programmKonfig);
        } catch (Exception e) {
            LOG.debug("", e);
        }
        alteSitzungLoeschen();
        SzenarioVerwaltung.loescheVerzeichnisInhalt(Information.getInformation().getTempPfad());
        System.exit(0);
    }

    public static void main(String[] args) {
        FiliusArgs filiusArgs = new FiliusArgs();
        try {
            filiusArgs.parseCommandLine(args);
            if (filiusArgs.currWD.isEmpty() || !filiusArgs.currWD.substring(filiusArgs.currWD.length() - 1).equals(System.getProperty("file.separator"))) {
                if (Information.getInformation(filiusArgs.currWD + System.getProperty("file.separator")) == null) {
                    System.exit(6);
                } else if (Information.getInformation(filiusArgs.currWD) == null) {
                    System.exit(6);
                }
            }
            if (filiusArgs.log) {
                System.setProperty("FILIUS_LOG_LEVEL", "DEBUG");
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                loggerContext.reset();
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);
                try {
                    configurator.doConfigure(configurator.getClass().getResourceAsStream("/logback.xml"));
                    LOG.info("Log to file enabled.");
                } catch (JoranException exception) {
                    LOG.error("Caught an unexpected error while trying to load the logback configuration file..", exception);
                }
            }
            if (Information.getInformation(filiusArgs.currWD) == null) {
                System.exit(6);
            }
            Verbindung.setRTTfactor(filiusArgs.rtt);
            Information.setLowResolution(filiusArgs.lowResolution);
            if (filiusArgs.nativeLookAndFeel) {
                Main.activateNativeLookAndFeel();
            }
            if (filiusArgs.help) {
                filiusArgs.showUsageInformation();
            } else {
                Main.starten(filiusArgs.projectFile);
            }
        } catch (ParseException e) {
            filiusArgs.showUsageInformation();
        }
        LOG.info("------------------------------------------------------");
        LOG.info("\tJava Version: " + System.getProperty("java.version"));
        LOG.info("\tJava Directory: " + System.getProperty("java.home"));
        LOG.info("\tFILIUS Version: " + Information.getVersion());
        LOG.info("\tParameters: '" + filiusArgs.argsString.trim() + "'");
        LOG.info("\tFILIUS Installation: " + Information.getInformation().getProgrammPfad());
        LOG.info("\tFILIUS Working Directory: " + Information.getInformation().getArbeitsbereichPfad());
        LOG.info("\tFILIUS Temp Directory: " + Information.getInformation().getTempPfad());
        LOG.info("------------------------------------------------------\n");
    }

    public static void activateNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            LOG.warn("Native look and feel not supported. Using default look and feel.");
        } catch (ClassNotFoundException e) {
            LOG.warn("LookAndFeel class could not be found. Using default look and feel.");
        } catch (IllegalAccessException | InstantiationException e) {
            LOG.warn("Could not instantiate LookAndFeel. Using default look and feel.");
        }
    }
}
