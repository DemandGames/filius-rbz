/*
 * Filius (RBZ Wirtschaft Kiel Version): based on AggregatedExchangePanel of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: a switch next to the tabs chooses the layer
 * model (TCP/IP, the default, or OSI). When the experimental layer visualization of the original is on,
 * its table is shown for TCP/IP and the layer stack for OSI. Opened, brought to the front or switched to another
 * tab, the table shows its newest messages (last line). Everything else is unchanged.
 */
package filius.gui.nachrichtensicht;

import filius.gui.nachrichtensicht.AggregatedExchangeComponent;
import filius.modern.LayerModel;
import filius.gui.nachrichtensicht.AggregatedMessageTable;
import filius.gui.nachrichtensicht.MessageDetails;
import filius.gui.nachrichtensicht.MessageDetailsPanel;
import filius.gui.nachrichtensicht.MessageDetailsTable;
import filius.hardware.knoten.Host;
import filius.hardware.knoten.InternetKnoten;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.Information;
import filius.software.system.InternetKnotenBetriebssystem;
import filius.software.system.SystemSoftware;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicButtonUI;

public class AggregatedExchangePanel
extends JTabbedPane
implements AggregatedExchangeComponent,
I18n,
Observer {
    private Hashtable<String, JPanel> openedTabs = new Hashtable();
    private Hashtable<String, InternetKnotenBetriebssystem> systems = new Hashtable();
    private Hashtable<String, AggregatedMessageTable> tabellen = new Hashtable();
    /** model switch listeners per tab (LayerModel keeps only weak references) */
    private Hashtable<String, Runnable> modelListeners = new Hashtable<String, Runnable>();

    public AggregatedExchangePanel() {
        this.putClientProperty("JTabbedPane.trailingComponent", LayerModel.createSwitch());
        // the learning path wants to know whose messages are looked at
        this.addChangeListener(e -> this.meldeAuswahl());
        // opened, another tab chosen or brought to the front: the newest messages are in view
        this.addChangeListener(e -> this.ansEnde());
        this.addHierarchyListener(e -> {
            java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (w != null && w != this.fenster) {
                this.fenster = w;
                w.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowActivated(java.awt.event.WindowEvent we) {
                        if (filius.modern.AnsEnde.vonAussen(w)) {
                            AggregatedExchangePanel.this.ansEnde();
                        }
                    }
                });
            }
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && this.isShowing()) {
                this.ansEnde();
            }
        });
    }

    private java.awt.Window fenster;
    private Hashtable<String, JScrollPane> rollen = new Hashtable<String, JScrollPane>();

    /** the table of the chosen computer shows its last line */
    private void ansEnde() {
        Component selected = this.getSelectedComponent();
        for (Map.Entry<String, JPanel> tab : this.openedTabs.entrySet()) {
            if (tab.getValue() == selected) {
                filius.modern.AnsEnde.scrollAnsEnde(this.rollen.get(tab.getKey()));
            }
        }
    }

    private void meldeAuswahl() {
        Component selected = this.getSelectedComponent();
        if (selected == null) {
            return;
        }
        for (Map.Entry<String, JPanel> tab : this.openedTabs.entrySet()) {
            if (tab.getValue() == selected) {
                filius.modern.lernpfad.Lernpfad.datenaustausch(this.systems.get(tab.getKey()));
            }
        }
    }

    @Override
    public void addTable(SystemSoftware system, final String identifier) {
        system.addObserver(this);
        filius.modern.lernpfad.Lernpfad.datenaustausch(system);
        // layer stack (TCP/IP or OSI); with the layer visualization setting, its table is used for TCP/IP
        final MessageDetailsPanel layerStack = new MessageDetailsPanel(identifier);
        final MessageDetails visualization = Information.getInformation().isLayerVisualization() ? new MessageDetailsTable(identifier, 0, Color.WHITE) : null;
        if (this.openedTabs.get(identifier) == null) {
            final AggregatedMessageTable tabelle = new AggregatedMessageTable(this, identifier, system);
            tabelle.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (tabelle.getSelectedRow() >= 0) {
                        layerStack.update(identifier, tabelle.getSelectedRow() + 1);
                        if (visualization != null) {
                            visualization.update(identifier, tabelle.getSelectedRow() + 1);
                        }
                    } else {
                        layerStack.clear();
                        if (visualization != null) {
                            visualization.clear();
                        }
                    }
                }
            });
            JPanel panel = new JPanel(new BorderLayout());
            JScrollPane scrollPane = new JScrollPane(tabelle);
            tabelle.setScrollPane(scrollPane);
            this.rollen.put(identifier, scrollPane);
            final JSplitPane splitPane = new JSplitPane();
            // the divider can be moved; it starts in the middle so the layer stack has room
            splitPane.setEnabled(true);
            splitPane.setDividerLocation(500);
            splitPane.setResizeWeight(0.5);
            splitPane.setOrientation(0);
            final boolean[] dividerPlaced = new boolean[1];
            splitPane.addHierarchyListener(e -> {
                if (!dividerPlaced[0] && (e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && splitPane.isShowing()) {
                    dividerPlaced[0] = true;
                    javax.swing.SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
                }
            });
            splitPane.setTopComponent(scrollPane);
            final JScrollPane detailsScroll = new JScrollPane(visualization != null && !LayerModel.isOsi() ? visualization : layerStack);
            detailsScroll.getVerticalScrollBar().setUnitIncrement(16);
            splitPane.setBottomComponent(detailsScroll);
            if (visualization != null) {
                Runnable onModelChange = () -> detailsScroll.setViewportView(LayerModel.isOsi() ? layerStack : visualization);
                LayerModel.addListener(onModelChange);
                this.modelListeners.put(identifier, onModelChange);
            }
            splitPane.setOneTouchExpandable(true);
            panel.add((Component)splitPane, "Center");
            this.add(panel);
            this.setSelectedComponent(panel);
            TabTitle title = new TabTitle(this, identifier);
            this.setTabComponentAt(this.getSelectedIndex(), title);
            this.openedTabs.put(identifier, panel);
            this.systems.put(identifier, (InternetKnotenBetriebssystem)system);
            this.tabellen.put(identifier, tabelle);
            if (this.openedTabs.size() > 0) {
                this.setVisible(true);
            }
            this.updateTabTitle();
        } else {
            this.setSelectedComponent(this.openedTabs.get(identifier));
            this.tabellen.get(identifier).update();
            this.ansEnde();
        }
    }

    void updateTabTitle() {
        block0: for (int i = 0; i < this.getTabCount(); ++i) {
            for (String identifier : this.openedTabs.keySet()) {
                if (!this.getComponentAt(i).equals(this.openedTabs.get(identifier))) continue;
                SystemSoftware system = this.systems.get(identifier);
                String ipAddress = ((InternetKnoten)system.getKnoten()).getNetzwerkInterfaceByMac(identifier).getIp();
                Object tabTitle = system.getKnoten() instanceof Host && ((Host)system.getKnoten()).isUseIPAsName() ? ipAddress : system.getKnoten().holeAnzeigeName() + " - " + ipAddress;
                TabTitle titlePanel = (TabTitle)this.getTabComponentAt(i);
                titlePanel.setTitle((String)tabTitle);
                continue block0;
            }
        }
    }

    void clearUnavailableComponents() {
        for (Map.Entry<String, InternetKnotenBetriebssystem> system : this.systems.entrySet()) {
            if (system.getValue().isStarted()) continue;
            this.removeTable(system.getKey());
        }
    }

    private void removeTable(String mac) {
        this.removeTable(mac, this.openedTabs.get(mac));
    }

    @Override
    public void removeTable(String mac, JPanel panel) {
        if (mac != null) {
            this.openedTabs.remove(mac);
            this.tabellen.remove(mac);
            this.rollen.remove(mac);
            this.modelListeners.remove(mac);
            this.remove(panel);
            if (this.openedTabs.size() == 0) {
                this.setVisible(false);
            }
        }
    }

    @Override
    public String getTabTitle(String interfaceId) {
        String title = interfaceId.replaceAll(":", "-");
        for (int i = 0; i < this.getTabCount(); ++i) {
            Component tab = this.getComponentAt(i);
            if (tab != this.openedTabs.get(interfaceId)) continue;
            title = ((TabTitle)this.getTabComponentAt(i)).getTitle();
            break;
        }
        return title;
    }

    @Override
    public void reset() {
    }

    @Override
    public void update(Observable o, Object arg) {
        this.updateTabTitle();
    }

    private class TabTitle
    extends JPanel {
        private JLabel label;

        TabTitle(final AggregatedExchangePanel parent, final String identifier) {
            this.setOpaque(false);
            this.label = new JLabel();
            this.add((Component)this.label, "West");
            JButton btnClose = new JButton("X");
            btnClose.setUI(new BasicButtonUI());
            btnClose.setForeground(Color.GRAY);
            btnClose.setBorder(BorderFactory.createEmptyBorder());
            btnClose.setPreferredSize(new Dimension(18, 18));
            btnClose.setToolTipText(I18n.messages.getString("buttontabcomponent_msg1"));
            this.add((Component)btnClose, "East");
            btnClose.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent evt) {
                    parent.removeTable(identifier);
                }
            });
        }

        void setTitle(String title) {
            this.label.setText(title);
        }

        String getTitle() {
            return this.label.getText();
        }
    }
}

