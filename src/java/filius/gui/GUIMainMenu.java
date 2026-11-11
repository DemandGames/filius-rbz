/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIMainMenu of Filius 2.9.3 (GPL). Actions,
 * shortcuts and mode handling are unchanged. The buttons are arranged in the new header
 * (filius.modern.HeaderBar): labelled file actions, a labelled mode switch with the learning path
 * button, the speed control and tools. The software wizard and the settings dialog are removed; the
 * language button switches between German and English with a restart. Loading, saving and new
 * projects also carry the learning progress. When the simulation stops, exceptions for untrusted
 * HTTPS certificates are forgotten (like a browser restart).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui;

import filius.gui.netzwerksicht.GUIKabelItem;
import filius.gui.netzwerksicht.GUIKnotenItem;
import filius.gui.netzwerksicht.JGatewayConfiguration;
import filius.gui.netzwerksicht.JVermittlungsrechnerKonfiguration;
import filius.hardware.Verbindung;
import filius.modern.HeaderBar;
import filius.modern.Overlay;
import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.lernpfad.Lernpfad;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.rahmenprogramm.SzenarioVerwaltung;
import filius.software.system.SystemSoftware;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIMainMenu
implements Serializable,
I18n {
    private static Logger LOG = LoggerFactory.getLogger(GUIMainMenu.class);
    private static final long serialVersionUID = 1L;
    public static final int MODUS_ENTWURF = 1;
    public static final int MODUS_AKTION = 2;
    public static final int MODUS_DOKUMENTATION = 3;
    private JBackgroundPanel menupanel;
    private HeaderBar header;
    private JSlider simulationSpeedInPercent;
    private JLabel geschwindigkeit;
    private int aktuellerModus;
    private JButton btAktionsmodus;
    private JButton btEntwurfsmodus;
    private JButton btDokumodus;
    private JButton btOeffnen;
    private JButton btSpeichern;
    private JButton btNeu;
    private JButton btHilfe;
    private JButton btInfo;
    private JButton btSettings;
    /** modes for which the hint toast was already shown in this session */

    public GUIMainMenu() {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (GUIMainMenu), constr: GUIMainMenu()");
        Container c = JMainFrame.getJMainFrame().getContentPane();
        this.header = new HeaderBar();
        this.menupanel = this.header;
        this.menupanel.setBounds(0, 0, c.getWidth(), 63);
        this.btNeu = new JButton();
        this.header.setIcon(this.btNeu, "tb-new");
        this.btNeu.setActionCommand("neu");
        this.btNeu.setToolTipText(messages.getString("guimainmemu_msg5"));
        this.btOeffnen = new JButton();
        this.header.setIcon(this.btOeffnen, "tb-open");
        this.btOeffnen.setActionCommand("oeffnen");
        this.btOeffnen.setToolTipText(messages.getString("guimainmemu_msg1"));
        this.btSpeichern = new JButton();
        this.header.setIcon(this.btSpeichern, "tb-save");
        this.btSpeichern.setActionCommand("speichern");
        this.btSpeichern.setToolTipText(messages.getString("guimainmemu_msg2"));
        this.btDokumodus = new JButton();
        this.btDokumodus.setActionCommand("dokumodus");
        this.btDokumodus.setToolTipText(messages.getString("guimainmemu_msg14"));
        this.btEntwurfsmodus = new JButton();
        this.btEntwurfsmodus.setActionCommand("entwurfsmodus");
        this.btEntwurfsmodus.setToolTipText(messages.getString("guimainmemu_msg3"));
        this.btAktionsmodus = new JButton();
        this.btAktionsmodus.setActionCommand("aktionsmodus");
        this.btAktionsmodus.setToolTipText(messages.getString("guimainmemu_msg4"));
        this.btHilfe = new JButton();
        this.header.setIcon(this.btHilfe, "tb-help");
        this.btHilfe.setActionCommand("hilfe");
        this.btHilfe.setToolTipText(messages.getString("guimainmemu_msg7"));
        this.btInfo = new JButton();
        this.header.setIcon(this.btInfo, "tb-info");
        this.btInfo.setActionCommand("info");
        this.btInfo.setToolTipText(messages.getString("guimainmemu_msg8"));
        this.btSettings = new HeaderBar.SprachKnopf();
        this.btSettings.setActionCommand("settings");
        ActionListener al = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(GUIMainMenu.this.btHilfe.getActionCommand())) {
                    // a second click hides the help again
                    if (GUIMainMenu.hilfeOffen()) {
                        GUIContainer.getGUIContainer().hideHelp();
                    } else {
                        GUIContainer.getGUIContainer().showHelp();
                    }
                }
                if (e.getActionCommand().equals(GUIMainMenu.this.btNeu.getActionCommand())) {
                    GUIMainMenu.this.newScenario(e);
                }
                if (e.getActionCommand().equals(GUIMainMenu.this.btSpeichern.getActionCommand()) && GUIContainer.getGUIContainer().getActiveSite() != 2) {
                    GUIMainMenu.this.saveScenario();
                }
                if (e.getActionCommand().equals(GUIMainMenu.this.btOeffnen.getActionCommand())) {
                    GUIMainMenu.this.loadScenario();
                }
                if (e.getActionCommand().equals(GUIMainMenu.this.btEntwurfsmodus.getActionCommand())) {
                    GUIMainMenu.this.selectMode(1);
                } else if (e.getActionCommand().equals(GUIMainMenu.this.btAktionsmodus.getActionCommand())) {
                    GUIMainMenu.this.selectMode(2);
                } else if (e.getActionCommand().equals(GUIMainMenu.this.btDokumodus.getActionCommand())) {
                    GUIMainMenu.this.selectMode(3);
                } else if (e.getActionCommand().equals(GUIMainMenu.this.btInfo.getActionCommand())) {
                    // opens the window once; a second click closes it
                    InfoDialog.umschalten(JMainFrame.getJMainFrame());
                } else if (e.getActionCommand().equals(GUIMainMenu.this.btSettings.getActionCommand())) {
                    ((HeaderBar.SprachKnopf)GUIMainMenu.this.btSettings).umschalten();
                }
            }
        };
        this.btNeu.addActionListener(al);
        this.btOeffnen.addActionListener(al);
        this.btSpeichern.addActionListener(al);
        this.btEntwurfsmodus.addActionListener(al);
        this.btAktionsmodus.addActionListener(al);
        this.btDokumodus.addActionListener(al);
        this.btInfo.addActionListener(al);
        this.btHilfe.addActionListener(al);
        this.btSettings.addActionListener(al);
        this.geschwindigkeit = new JLabel();
        this.geschwindigkeit.setVisible(true);
        this.geschwindigkeit.setToolTipText(messages.getString("guimainmemu_msg15"));
        this.geschwindigkeit.setAlignmentX(1.0f);
        this.simulationSpeedInPercent = new JSlider(0, 100);
        this.simulationSpeedInPercent.setToolTipText(messages.getString("guimainmemu_msg16"));
        this.simulationSpeedInPercent.setMaximum(100);
        this.simulationSpeedInPercent.setMinimum(1);
        this.simulationSpeedInPercent.setValue(100 - Verbindung.holeVerzoegerungsFaktor());
        this.simulationSpeedInPercent.setOpaque(false);
        this.simulationSpeedInPercent.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent arg0) {
                GUIMainMenu.this.updateLatency();
            }
        });
        this.updateLatency();
        this.header.setFileButtons(this.btNeu, this.btOeffnen, this.btSpeichern);
        this.header.setModeButtons(this.btEntwurfsmodus, this.btAktionsmodus, this.btDokumodus);
        this.header.setSpeed(this.simulationSpeedInPercent, this.geschwindigkeit);
        // light/dark mode, right of the language switch
        if (filius.modern.Klausur.AKTIV) {
            // exam edition: no help (it explains client, server, gateway ...)
            this.header.setToolButtons(this.btInfo, this.btSettings, new HeaderBar.DunkelKnopf());
        } else {
            this.header.setToolButtons(this.btHilfe, this.btInfo, this.btSettings, new HeaderBar.DunkelKnopf());
        }
    }

    private void updateLatency() {
        Verbindung.setzeVerzoegerungsFaktor(this.simulationSpeedInPercent.getMaximum() - this.simulationSpeedInPercent.getValue() + 1);
        this.geschwindigkeit.setText(this.simulationSpeedInPercent.getValue() + "%");
    }

    private void initCurrentFileOrDirSelection(FileDialog dialog) {
        String scenarioPath = SzenarioVerwaltung.getInstance().holePfad();
        String lastOpenedDir = Information.getInformation().getLastOpenedDirectory();
        File file = null;
        if (scenarioPath != null) {
            file = new File(scenarioPath);
        }
        if (null != file && file.exists()) {
            dialog.setFile(file.getAbsolutePath());
        } else if (null != lastOpenedDir && (file = new File(lastOpenedDir)).exists()) {
            dialog.setDirectory(file.getAbsolutePath());
        }
    }

    public void changeSlider(int diff) {
        if (diff < 0 && this.simulationSpeedInPercent.getValue() + diff < 1) {
            this.simulationSpeedInPercent.setValue(1);
        } else if (diff > 0 && this.simulationSpeedInPercent.getValue() + diff > 10) {
            this.simulationSpeedInPercent.setValue(10);
        } else {
            this.simulationSpeedInPercent.setValue(this.simulationSpeedInPercent.getValue() + diff);
        }
    }

    public boolean doClick(String button) {
        if (button.equals("btAktionsmodus")) {
            this.btAktionsmodus.doClick();
        } else if (button.equals("btEntwurfsmodus")) {
            this.btEntwurfsmodus.doClick();
        } else if (button.equals("btDokumodus")) {
            this.btDokumodus.doClick();
        } else if (button.equals("btOeffnen")) {
            this.btOeffnen.doClick();
        } else if (button.equals("btSpeichern")) {
            this.btSpeichern.doClick();
        } else if (button.equals("btNeu")) {
            this.btNeu.doClick();
        } else if (button.equals("btHilfe")) {
            this.btHilfe.doClick();
        } else if (button.equals("btInfo")) {
            this.btInfo.doClick();
        } else if (button.equals("btSettings")) {
            this.btSettings.doClick();
        } else {
            return false;
        }
        return true;
    }

    private void newScenario(ActionEvent e) {
        int entscheidung = 0;
        try {
            entscheidung = SzenarioVerwaltung.getInstance().istGeaendert() ? JOptionPane.showConfirmDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg9"), messages.getString("guimainmemu_msg10"), 0) : 0;
        } catch (Exception exc) {
            LOG.debug("", (Object)e);
        }
        if (entscheidung == 0) {
            GUIContainer.getGUIContainer().clearAllItems();
            GUIContainer.getGUIContainer().setProperty(null);
            Information.getInformation().reset();
            SzenarioVerwaltung.getInstance().reset();
            Lernpfad.projektNeu();
        }
    }

    private void loadScenario() {
        int entscheidung = 0;
        try {
            entscheidung = SzenarioVerwaltung.getInstance().istGeaendert() ? JOptionPane.showConfirmDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg9"), messages.getString("guimainmemu_msg10"), 0) : 0;
        } catch (Exception exc) {
            LOG.debug("", exc);
        }
        if (entscheidung == 0 && GUIContainer.getGUIContainer().getActiveSite() != 2) {
            FileDialog fileDialog = new FileDialog((Frame)JMainFrame.getJMainFrame(), messages.getString("main_msg3"), 0);
            FilenameFilter filiusFilenameFilter = (dir, name) -> name.endsWith(".fls");
            fileDialog.setFilenameFilter(filiusFilenameFilter);
            this.initCurrentFileOrDirSelection(fileDialog);
            fileDialog.setVisible(true);
            String selectedFile = fileDialog.getFile();
            if (selectedFile != null) {
                Information.getInformation().setLastOpenedDirectory(fileDialog.getDirectory());
                try {
                    Path file = Path.of(fileDialog.getDirectory(), selectedFile);
                    Information.getInformation().reset();
                    SzenarioVerwaltung.getInstance().laden(file.toString(), GUIContainer.getGUIContainer().getKnotenItems(), GUIContainer.getGUIContainer().getCableItems(), GUIContainer.getGUIContainer().getDocuItems());
                    GUIContainer.getGUIContainer().setProperty(null);
                    GUIContainer.getGUIContainer().updateViewport();
                    Thread.sleep(10L);
                    GUIContainer.getGUIContainer().updateCables();
                    Lernpfad.projektGeladen(file.toString());
                } catch (FileNotFoundException e1) {
                    LOG.debug("Selected File could not be found.", e1);
                } catch (Exception e2) {
                    LOG.debug(e2.getMessage(), e2);
                }
            }
        }
    }

    private boolean saveScenario() {
        FileDialog fileDialog = new FileDialog((Frame)JMainFrame.getJMainFrame(), messages.getString("main_msg4"), 1);
        FilenameFilter filiusFilenameFilter = (dir, name) -> name.endsWith(".fls");
        fileDialog.setFilenameFilter(filiusFilenameFilter);
        this.initCurrentFileOrDirSelection(fileDialog);
        fileDialog.setVisible(true);
        if (fileDialog.getFile() != null) {
            String targetFilePath;
            Information.getInformation().setLastOpenedDirectory(fileDialog.getDirectory());
            boolean nameChanged = false;
            if (fileDialog.getFile().endsWith(".fls")) {
                targetFilePath = Path.of(fileDialog.getDirectory(), fileDialog.getFile()).toString();
            } else {
                nameChanged = true;
                targetFilePath = Path.of(fileDialog.getDirectory(), fileDialog.getFile() + ".fls").toString();
            }
            int entscheidung = 0;
            if (nameChanged && new File(targetFilePath).exists()) {
                entscheidung = JOptionPane.showConfirmDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg17"), messages.getString("guimainmemu_msg10"), 0);
            }
            if (entscheidung == 0) {
                boolean erfolg = SzenarioVerwaltung.getInstance().speichern(targetFilePath, GUIContainer.getGUIContainer().getKnotenItems(), GUIContainer.getGUIContainer().getCableItems(), GUIContainer.getGUIContainer().getDocuItems());
                if (erfolg) {
                    Lernpfad.projektGespeichert(targetFilePath);
                    SzenarioVerwaltung.getInstance().setCurrentFile(targetFilePath, false);
                    return true;
                }
                JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg11"));
            }
        }
        return false;
    }

    /**
     * Loads a start network of the learning path. Unsaved work can be saved first. The loaded
     * network has no file name yet, so saving asks for one and never overwrites the template.
     */
    public static boolean vorlageLaden(String datei) {
        GUIContainer gc = GUIContainer.getGUIContainer();
        GUIMainMenu menu = gc.getMenu();
        menu.selectMode(1);
        if (SzenarioVerwaltung.getInstance().istGeaendert() && !gc.getKnotenItems().isEmpty()) {
            Object[] optionen = { Texts.get("dlg.save"), Texts.get("dlg.nosave"), Texts.get("dlg.cancel") };
            int wahl = JOptionPane.showOptionDialog(JMainFrame.getJMainFrame(), Texts.get("tpl.unsaved"),
                    Texts.get("lp.load.start"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen[0]);
            if (wahl == 0) {
                if (!menu.saveScenario()) {
                    return false;
                }
            } else if (wahl != 1) {
                return false;
            }
        }
        try {
            Information.getInformation().reset();
            SzenarioVerwaltung.getInstance().laden(datei, gc.getKnotenItems(), gc.getCableItems(), gc.getDocuItems());
            gc.setProperty(null);
            gc.updateViewport();
            Thread.sleep(10L);
            gc.updateCables();
            SzenarioVerwaltung.getInstance().setCurrentFile(null, true);
            return true;
        } catch (Exception e) {
            LOG.debug("start network could not be loaded", e);
            JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), Texts.get("tpl.error"));
            return false;
        }
    }

    /**
     * Opens a project file as the "Load" button does, without the file dialog: used when the system hands
     * Filius a file to open (macOS: double click on a .fls file sends an event instead of an argument).
     * Unsaved work is asked about first.
     */
    public static boolean projektOeffnen(java.io.File datei) {
        GUIContainer gc = GUIContainer.getGUIContainer();
        GUIMainMenu menu = gc.getMenu();
        if (gc == null || menu == null || datei == null || !datei.isFile()) {
            return false;
        }
        menu.selectMode(1);
        if (SzenarioVerwaltung.getInstance().istGeaendert() && !gc.getKnotenItems().isEmpty()) {
            Object[] optionen = { Texts.get("dlg.save"), Texts.get("dlg.nosave"), Texts.get("dlg.cancel") };
            int wahl = JOptionPane.showOptionDialog(JMainFrame.getJMainFrame(), messages.getString("guimainmemu_msg9"), messages.getString("guimainmemu_msg10"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionen, optionen[0]);
            if (wahl == 0) {
                if (!menu.saveScenario()) {
                    return false;
                }
            } else if (wahl != 1) {
                return false;
            }
        }
        try {
            Information.getInformation().setLastOpenedDirectory(datei.getParent());
            Information.getInformation().reset();
            SzenarioVerwaltung.getInstance().laden(datei.toString(), gc.getKnotenItems(), gc.getCableItems(), gc.getDocuItems());
            gc.setProperty(null);
            gc.updateViewport();
            Thread.sleep(10L);
            gc.updateCables();
            Lernpfad.projektGeladen(datei.toString());
            return true;
        } catch (Exception e) {
            LOG.debug("project could not be opened", e);
            JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), Texts.get("tpl.error"));
            return false;
        }
    }

    /** true if the help panel at the right is open */
    static boolean hilfeOffen() {
        try {
            java.lang.reflect.Field f = GUIContainer.class.getDeclaredField("helpPanel");
            f.setAccessible(true);
            Object p = f.get(GUIContainer.getGUIContainer());
            return p instanceof ControlPanel && ((ControlPanel) p).isMaximiert();
        } catch (Exception e) {
            return false;
        }
    }

    /** Saves the project with the save dialog (before Filius closes or restarts in another language). */
    public static boolean speichernMitDialog() {
        GUIContainer gc = GUIContainer.getGUIContainer();
        gc.getMenu().selectMode(1);
        return gc.getMenu().saveScenario();
    }

    private void resetCableHighlighting(int mode) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (GUIMainMenu), resetCableHL(" + mode + ")");
        if (mode == 2) {
            for (GUIKabelItem cableItem : GUIContainer.getGUIContainer().getCableItems()) {
                cableItem.getDasKabel().setAktiv(false);
            }
        } else {
            if (GUIContainer.getGUIContainer().getProperty() instanceof JVermittlungsrechnerKonfiguration) {
                ((JVermittlungsrechnerKonfiguration)GUIContainer.getGUIContainer().getProperty()).highlightConnCable();
            }
            if (GUIContainer.getGUIContainer().getProperty() instanceof JGatewayConfiguration) {
                ((JGatewayConfiguration)GUIContainer.getGUIContainer().getProperty()).highlightConnCable();
            }
        }
    }

    /**
     * A restarted home router starts with an empty connection table, like a real one. The
     * original keeps learned NAT entries across simulation runs for up to five minutes, so a
     * corrected port forwarding still led to the old address when it had been tried before.
     */
    static void natTabelleLeeren(SystemSoftware system) {
        try {
            if (system instanceof filius.software.system.GatewayFirmware) {
                Object fw = ((filius.software.system.GatewayFirmware)system).holeFirewall();
                if (fw instanceof filius.software.nat.NatGateway) {
                    ((filius.software.nat.NatGateway)fw).getNATTable().getDynamicNATTable().clear();
                }
            }
        } catch (RuntimeException e) {
            LOG.debug("NAT table not cleared", e);
        }
    }

    public synchronized void selectMode(int mode) {
        LOG.trace("INVOKED (" + this.hashCode() + ") " + String.valueOf(this.getClass()) + " (GUIMainMenu), selectMode(" + mode + ")");
        if (mode == 1) {
            this.resetCableHighlighting(mode);
            this.header.setMode(1);
            GUIContainer.getGUIContainer().setActiveSite(1);
            this.stopSimulation();
            this.btOeffnen.setEnabled(true);
            this.btNeu.setEnabled(true);
            this.btSpeichern.setEnabled(true);
        } else if (mode == 3) {
            this.resetCableHighlighting(mode);
            this.header.setMode(3);
            GUIContainer.getGUIContainer().setActiveSite(3);
            this.stopSimulation();
            this.btOeffnen.setEnabled(true);
            this.btNeu.setEnabled(true);
            this.btSpeichern.setEnabled(true);
        } else if (mode == 2 && this.aktuellerModus != 2) {
            this.resetCableHighlighting(mode);
            this.header.setMode(2);
            GUIContainer.getGUIContainer().setActiveSite(2);
            for (GUIKnotenItem knotenItem : GUIContainer.getGUIContainer().getKnotenItems()) {
                SystemSoftware system = knotenItem.getKnoten().getSystemSoftware();
                GUIMainMenu.natTabelleLeeren(system);
                system.starten();
            }
            this.btOeffnen.setEnabled(false);
            this.btNeu.setEnabled(false);
            this.btSpeichern.setEnabled(false);
            this.geschwindigkeit.setEnabled(true);
            this.simulationSpeedInPercent.setEnabled(true);
        }
        this.aktuellerModus = mode;
    }

    private void stopSimulation() {
        for (GUIKnotenItem knotenItem : GUIContainer.getGUIContainer().getKnotenItems()) {
            SystemSoftware system = knotenItem.getKnoten().getSystemSoftware();
            try {
                system.beenden();
            } catch (Exception exception) {}
        }
        // like a browser restart: exceptions for certificates ("open anyway") are forgotten
        filius.modern.tls.Tls.vergessen();
    }

    public JBackgroundPanel getMenupanel() {
        return this.menupanel;
    }

    public void setMenupanel(JBackgroundPanel menupanel) {
        this.menupanel = menupanel;
    }
}
