/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIInstallationsDialog of Filius 2.9.3 (GPL). New: every program in
 * both lists carries a role tag (Server, Client, Peer-to-Peer, Werkzeug) that is explained on
 * hover, and a legend next to the confirm button defines the four roles. Selecting, installing
 * and removing programs works exactly as before.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.anwendungssicht;

import filius.modern.AppRoles;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.software.Anwendung;
import filius.software.system.Betriebssystem;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

public class GUIInstallationsDialog
extends GUIApplicationWindow
implements I18n {
    private JList<String> softwareInstalliert;
    private JList<String> softwareVerfuegbar;
    private DefaultListModel<String> lmVerfuegbar;
    private DefaultListModel<String> lmInstalliert;
    private List<Map<String, String>> programme = Information.getInformation().ladeProgrammListe();
    /** program name as shown in the lists -> program class (for the role tags) */
    private final Map<String, String> nameZuKlasse = new HashMap<String, String>();

    public GUIInstallationsDialog(GUIDesktopPanel dp) {
        super(dp);
        if (this.programme != null) {
            for (Map<String, String> programmInfo : this.programme) {
                this.nameZuKlasse.put(programmInfo.get("Anwendung"), programmInfo.get("Klasse"));
            }
        }
        this.setTitle(messages.getString("installationsdialog_msg1"));
        this.setIcon("/gfx/desktop/icon_softwareinstallation.png");
        Box gesamtBox = Box.createVerticalBox();
        Box listenBox = Box.createHorizontalBox();
        listenBox.add(Box.createHorizontalStrut(10));
        listenBox.add(this.createInstalledAppsBox());
        listenBox.add(Box.createHorizontalGlue());
        listenBox.add(this.createSelectionButtonBox());
        listenBox.add(Box.createHorizontalGlue());
        listenBox.add(this.createAvailableAppsBox());
        listenBox.add(Box.createHorizontalStrut(10));
        gesamtBox.add(Box.createVerticalStrut(10));
        gesamtBox.add(listenBox);
        gesamtBox.add(Box.createVerticalStrut(10));
        gesamtBox.add(this.createLowerButtonBox());
        gesamtBox.add(Box.createVerticalStrut(10));
        this.add((Component)gesamtBox, "Center");
    }

    /** Shows the role tag of every entry and explains it on hover. */
    private void zeigeRollen(JList<String> liste) {
        liste.setCellRenderer(new AppRoles.ListRenderer(this.nameZuKlasse::get));
        ToolTipManager.sharedInstance().registerComponent(liste);
    }

    /** Column heading, flush with the left edge of its list. */
    private JLabel ueberschrift(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(0f);
        return label;
    }

    private Box createInstalledAppsBox() {
        Box wrapperInstBox = Box.createVerticalBox();
        wrapperInstBox.add(this.ueberschrift(messages.getString("installationsdialog_msg3")));
        wrapperInstBox.add(Box.createVerticalStrut(8));
        this.lmInstalliert = new DefaultListModel<String>();
        Anwendung[] anwendungen = this.desktop.getBetriebssystem().holeArrayInstallierteSoftware();
        for (int i = 0; i < anwendungen.length; ++i) {
            if (anwendungen[i] == null) continue;
            this.lmInstalliert.addElement(anwendungen[i].holeAnwendungsName());
        }
        this.softwareInstalliert = new JList<String>();
        this.softwareInstalliert.setModel(this.lmInstalliert);
        this.zeigeRollen(this.softwareInstalliert);
        this.softwareInstalliert.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    GUIInstallationsDialog.this.entfernen();
                }
            }
        });
        JScrollPane scrollAnwendungInstallieren = new JScrollPane(this.softwareInstalliert);
        scrollAnwendungInstallieren.setPreferredSize(new Dimension(235, 200));
        scrollAnwendungInstallieren.setAlignmentX(0f);
        wrapperInstBox.add(scrollAnwendungInstallieren);
        return wrapperInstBox;
    }

    private Box createAvailableAppsBox() {
        Box wrapperAvailBox = Box.createVerticalBox();
        wrapperAvailBox.add(this.ueberschrift(messages.getString("installationsdialog_msg4")));
        wrapperAvailBox.add(Box.createVerticalStrut(8));
        this.lmVerfuegbar = new DefaultListModel<String>();
        if (this.programme != null) {
            for (Map<String, String> programmInfo : this.programme) {
                String awKlasse = programmInfo.get("Klasse");
                if (this.desktop.getBetriebssystem().holeSoftware(awKlasse) != null) continue;
                this.lmVerfuegbar.addElement(programmInfo.get("Anwendung"));
            }
        }
        this.softwareVerfuegbar = new JList<String>();
        this.softwareVerfuegbar.setModel(this.lmVerfuegbar);
        this.zeigeRollen(this.softwareVerfuegbar);
        this.softwareVerfuegbar.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    GUIInstallationsDialog.this.hinzufuegen();
                }
            }
        });
        JScrollPane scrollAnwendungVerfuegbar = new JScrollPane(this.softwareVerfuegbar);
        scrollAnwendungVerfuegbar.setPreferredSize(new Dimension(235, 200));
        scrollAnwendungVerfuegbar.setAlignmentX(0f);
        wrapperAvailBox.add(scrollAnwendungVerfuegbar);
        return wrapperAvailBox;
    }

    private Box createSelectionButtonBox() {
        Box topButtonBox = Box.createVerticalBox();
        JButton addButton = new JButton(new ImageIcon(this.getClass().getResource("/gfx/allgemein/pfeil_links.png")));
        addButton.setMargin(new Insets(2, 2, 2, 2));
        addButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                GUIInstallationsDialog.this.hinzufuegen();
            }
        });
        topButtonBox.add(addButton);
        topButtonBox.add(Box.createVerticalStrut(10));
        JButton removeButton = new JButton(new ImageIcon(this.getClass().getResource("/gfx/allgemein/pfeil_rechts.png")));
        removeButton.setMargin(new Insets(2, 2, 2, 2));
        removeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                GUIInstallationsDialog.this.entfernen();
            }
        });
        topButtonBox.add(removeButton);
        return topButtonBox;
    }

    /** Legend of the role tags on the left, confirm button on the right. */
    private Box createLowerButtonBox() {
        Box bottomButtonBox = Box.createHorizontalBox();
        JButton confirmButton = new JButton(messages.getString("installationsdialog_msg2"));
        confirmButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                GUIInstallationsDialog.this.aenderungenSpeichern();
                GUIInstallationsDialog.this.setVisible(false);
            }
        });
        JComponent legende = AppRoles.legend();
        legende.setMaximumSize(legende.getPreferredSize());
        legende.setAlignmentY(0.5f);
        confirmButton.setAlignmentY(0.5f);
        bottomButtonBox.add(Box.createHorizontalStrut(10));
        bottomButtonBox.add(legende);
        bottomButtonBox.add(Box.createHorizontalGlue());
        bottomButtonBox.add(Box.createHorizontalStrut(12));
        bottomButtonBox.add(confirmButton);
        bottomButtonBox.add(Box.createHorizontalStrut(10));
        // only as tall as needed, so the lists get the remaining height
        bottomButtonBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, bottomButtonBox.getPreferredSize().height));
        return bottomButtonBox;
    }

    private void hinzufuegen() {
        int[] selektiertIndices;
        Vector<String> vLoeschen = new Vector<String>();
        for (int i : selektiertIndices = this.softwareVerfuegbar.getSelectedIndices()) {
            this.lmInstalliert.addElement(this.lmVerfuegbar.get(i));
            vLoeschen.add(this.lmVerfuegbar.get(i));
        }
        Enumeration<String> e = vLoeschen.elements();
        while (e.hasMoreElements()) {
            String oZuLoeschen = e.nextElement();
            this.lmVerfuegbar.removeElement(oZuLoeschen);
        }
    }

    private void entfernen() {
        int[] selektiertIndices = this.softwareInstalliert.getSelectedIndices();
        Vector<String> hinzu = new Vector<String>();
        for (int i : selektiertIndices) {
            this.lmVerfuegbar.addElement(this.lmInstalliert.getElementAt(i));
            hinzu.add(this.lmInstalliert.getElementAt(i));
        }
        Enumeration<String> e = hinzu.elements();
        while (e.hasMoreElements()) {
            String hinzuObjekt = e.nextElement();
            this.lmInstalliert.removeElement(hinzuObjekt);
        }
    }

    private void aenderungenSpeichern() {
        Betriebssystem bs = this.desktop.getBetriebssystem();
        for (Map<String, String> appInfo : this.programme) {
            Anwendung anwendung;
            int i;
            for (i = 0; i < this.lmInstalliert.getSize(); ++i) {
                if (!this.lmInstalliert.getElementAt(i).equals(appInfo.get("Anwendung")) || bs.holeSoftware(appInfo.get("Klasse").toString()) != null) continue;
                bs.installAppIfAvailable(appInfo.get("Klasse").toString());
                anwendung = bs.holeSoftware(appInfo.get("Klasse").toString());
                anwendung.starten();
            }
            for (i = 0; i < this.lmVerfuegbar.getSize(); ++i) {
                if (!this.lmVerfuegbar.getElementAt(i).equals(appInfo.get("Anwendung")) || (anwendung = bs.holeSoftware(appInfo.get("Klasse").toString())) == null) continue;
                anwendung.beenden();
                bs.entferneSoftware(appInfo.get("Klasse").toString());
            }
        }
        this.desktop.updateAppPane();
    }

    @Override
    public void update(Observable arg0, Object arg1) {
    }
}
