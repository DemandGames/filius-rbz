/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIApplicationDNSServerWindow of Filius 2.9.3
 * (decompiled, GPL). Changed: the input fields no longer reset their text on every key, so the caret stays
 * where it is (in the original, arrow left jumped back to the end of the field). Everything else is unchanged.
 */
/*
 * Decompiled with CFR 0.152.
 */
package filius.gui.anwendungssicht;

import filius.gui.anwendungssicht.DNSConfigTable;
import filius.gui.anwendungssicht.GUIApplicationWindow;
import filius.gui.anwendungssicht.GUIDesktopPanel;
import filius.rahmenprogramm.EingabenUeberpruefung;
import filius.rahmenprogramm.I18n;
import filius.software.dns.DNSServer;
import filius.software.dns.ResourceRecord;
import filius.software.vermittlungsschicht.IP;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIApplicationDNSServerWindow
extends GUIApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(GUIApplicationDNSServerWindow.class);
    private JTextField aDomainField;
    private JTextField mxURLField;
    private JTextField aIpField;
    private JTextField mxMaildomainField;
    private JTextField nsDomainField;
    private JTextField nsDomainServerField;
    private JCheckBox recResolution;
    private JButton buttonStart;
    private DNSConfigTable aRecordsTable;
    private DNSConfigTable mxRecordsTable;
    private DNSConfigTable nsRecordsTable;

    public GUIApplicationDNSServerWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        this.initialisiereKomponenten();
        this.aktualisieren();
    }

    private void initialisiereKomponenten() {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add((Component)this.createServerControlBox(), "North");
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(messages.getString("dnsserver_msg2"), new ImageIcon(this.getClass().getResource("/gfx/desktop/peertopeer_netzwerk_klein.png")), this.createAPanel());
        tabbedPane.addTab(messages.getString("dnsserver_msg3"), new ImageIcon(this.getClass().getResource("/gfx/desktop/peertopeer_netzwerk_klein.png")), this.createMXPanel());
        tabbedPane.addTab(messages.getString("dnsserver_msg15"), new ImageIcon(this.getClass().getResource("/gfx/desktop/peertopeer_netzwerk_klein.png")), this.createNSPanel());
        Box hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        hBox.add(tabbedPane);
        contentPane.add((Component)hBox, "Center");
        this.add((Component)contentPane, "Center");
    }

    private Box createServerControlBox() {
        this.buttonStart = new JButton(messages.getString("dnsserver_msg1"));
        this.buttonStart.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).isAktiv()) {
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).setAktiv(false);
                } else {
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).setAktiv(true);
                }
            }
        });
        Box hBox = Box.createHorizontalBox();
        hBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        hBox.add(this.buttonStart);
        this.recResolution = new JCheckBox();
        this.recResolution.setText(messages.getString("dnsserver_msg20"));
        DNSServer dnsServer = (DNSServer)this.holeAnwendung();
        this.recResolution.setSelected(dnsServer.isRecursiveResolutionEnabled());
        this.recResolution.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox checkBox = GUIApplicationDNSServerWindow.this.recResolution;
                boolean activated = checkBox.isSelected();
                DNSServer dnsServer = (DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung();
                dnsServer.setRecursiveResolutionEnabled(activated);
                checkBox.setSelected(dnsServer.isRecursiveResolutionEnabled());
            }
        });
        hBox.add(Box.createHorizontalStrut(20));
        hBox.add(this.recResolution);
        return hBox;
    }

    private JPanel createAPanel() {
        JPanel aPanel = new JPanel(new BorderLayout());
        Box vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));
        Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JLabel aDomainLabel = new JLabel(messages.getString("dnsserver_msg4"));
        aDomainLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(aDomainLabel);
        hBox.add(Box.createHorizontalStrut(5));
        this.aDomainField = new JTextField();
        this.aDomainField.setPreferredSize(new Dimension(275, 25));
        this.aDomainField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.aDomainField);
            }
        });
        hBox.add(this.aDomainField);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JLabel aIpLabel = new JLabel(messages.getString("dnsserver_msg5"));
        aIpLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(aIpLabel);
        hBox.add(Box.createHorizontalStrut(5));
        this.aIpField = new JTextField(this.holeAnwendung().getSystemSoftware().primaryIPAdresse());
        this.aIpField.setPreferredSize(new Dimension(275, 25));
        this.aIpField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationDNSServerWindow.this.checkIP(GUIApplicationDNSServerWindow.this.aIpField);
            }
        });
        hBox.add(this.aIpField);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JButton aAddButton = new JButton(messages.getString("dnsserver_msg6"));
        aAddButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.aDomainField) && GUIApplicationDNSServerWindow.this.checkIP(GUIApplicationDNSServerWindow.this.aIpField) && IP.ipCheck(GUIApplicationDNSServerWindow.this.aIpField.getText()) != null) {
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).hinzuRecord(GUIApplicationDNSServerWindow.this.aDomainField.getText(), "A", GUIApplicationDNSServerWindow.this.aIpField.getText());
                    GUIApplicationDNSServerWindow.this.aDomainField.setText("");
                    GUIApplicationDNSServerWindow.this.aIpField.setText("");
                    GUIApplicationDNSServerWindow.this.updateARecordsTable();
                } else {
                    GUIApplicationDNSServerWindow.this.showMessageDialog(I18n.messages.getString("dnsserver_msg21"));
                }
            }
        });
        hBox.add(aAddButton);
        hBox.add(Box.createHorizontalStrut(5));
        JButton buttonEntfernen = new JButton(messages.getString("dnsserver_msg7"));
        buttonEntfernen.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                int zeilenNummer = GUIApplicationDNSServerWindow.this.aRecordsTable.getSelectedRow();
                if (zeilenNummer != -1) {
                    String domainname = GUIApplicationDNSServerWindow.this.aRecordsTable.getValueAt(zeilenNummer, 0).toString();
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).loescheResourceRecord(domainname, "A");
                    LOG.debug("GUIApplicationDNSServerWindow: A-Eintrag " + domainname + " geloescht");
                    GUIApplicationDNSServerWindow.this.updateARecordsTable();
                }
            }
        });
        hBox.add(buttonEntfernen);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        DefaultTableModel tabellenModell = new DefaultTableModel(0, 2);
        this.aRecordsTable = new DNSConfigTable((TableModel)tabellenModell, true, "A");
        this.aRecordsTable.setParentGUI(this);
        this.aRecordsTable.setIntercellSpacing(new Dimension(5, 5));
        this.aRecordsTable.setRowHeight(30);
        this.aRecordsTable.setShowGrid(false);
        this.aRecordsTable.setFillsViewportHeight(true);
        this.aRecordsTable.setBackground(filius.modern.Theme.SURFACE);
        this.aRecordsTable.setShowHorizontalLines(true);
        TableColumnModel tcm = this.aRecordsTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("dnsserver_msg8"));
        tcm.getColumn(1).setHeaderValue(messages.getString("dnsserver_msg9"));
        JScrollPane scrollPane = new JScrollPane(this.aRecordsTable);
        vBox.add(scrollPane);
        aPanel.add((Component)vBox, "Center");
        return aPanel;
    }

    private JPanel createMXPanel() {
        JPanel mxPanel = new JPanel(new BorderLayout());
        Box vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));
        Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JLabel mxMaildomainLabel = new JLabel(messages.getString("dnsserver_msg10"));
        mxMaildomainLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(mxMaildomainLabel);
        hBox.add(Box.createHorizontalStrut(5));
        this.mxMaildomainField = new JTextField();
        this.mxMaildomainField.setPreferredSize(new Dimension(275, 25));
        this.mxMaildomainField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.mxMaildomainField);
            }
        });
        hBox.add(this.mxMaildomainField);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JLabel mxURLLabel = new JLabel(messages.getString("dnsserver_msg11"));
        mxURLLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(mxURLLabel);
        hBox.add(Box.createHorizontalStrut(5));
        this.mxURLField = new JTextField();
        this.mxURLField.setPreferredSize(new Dimension(275, 25));
        this.mxURLField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.mxURLField);
            }
        });
        hBox.add(this.mxURLField);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JButton mxAddButton = new JButton(messages.getString("dnsserver_msg6"));
        mxAddButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.mxMaildomainField) && GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.mxURLField)) {
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).hinzuRecord(GUIApplicationDNSServerWindow.this.mxMaildomainField.getText(), "MX", GUIApplicationDNSServerWindow.this.mxURLField.getText());
                    GUIApplicationDNSServerWindow.this.mxMaildomainField.setText("");
                    GUIApplicationDNSServerWindow.this.mxURLField.setText("");
                    GUIApplicationDNSServerWindow.this.updateMXRecordsTable();
                } else {
                    GUIApplicationDNSServerWindow.this.showMessageDialog(I18n.messages.getString("dnsserver_msg23"));
                }
            }
        });
        hBox.add(mxAddButton);
        JButton buttonMXEntfernen = new JButton(messages.getString("dnsserver_msg7"));
        buttonMXEntfernen.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                int zeilenNummer = GUIApplicationDNSServerWindow.this.mxRecordsTable.getSelectedRow();
                if (zeilenNummer != -1) {
                    String domainname = GUIApplicationDNSServerWindow.this.mxRecordsTable.getValueAt(zeilenNummer, 0).toString();
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).loescheResourceRecord(domainname, "MX");
                    LOG.debug("GUIApplicationDNSServerWindow: MX-Eintrag " + domainname + " geloescht");
                    GUIApplicationDNSServerWindow.this.updateMXRecordsTable();
                }
            }
        });
        hBox.add(buttonMXEntfernen);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        DefaultTableModel tabellenModell = new DefaultTableModel(0, 2);
        this.mxRecordsTable = new DNSConfigTable((TableModel)tabellenModell, true, "MX");
        this.mxRecordsTable.setParentGUI(this);
        this.mxRecordsTable.setIntercellSpacing(new Dimension(5, 5));
        this.mxRecordsTable.setRowHeight(30);
        this.mxRecordsTable.setShowGrid(false);
        this.mxRecordsTable.setFillsViewportHeight(true);
        this.mxRecordsTable.setBackground(filius.modern.Theme.SURFACE);
        this.mxRecordsTable.setShowHorizontalLines(true);
        TableColumnModel tcm = this.mxRecordsTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("dnsserver_msg12"));
        tcm.getColumn(1).setHeaderValue(messages.getString("dnsserver_msg13"));
        JScrollPane scrollPane = new JScrollPane(this.mxRecordsTable);
        vBox.add(scrollPane);
        mxPanel.add((Component)vBox, "Center");
        return mxPanel;
    }

    private JPanel createNSPanel() {
        JPanel nsPanel = new JPanel(new BorderLayout());
        Box vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));
        Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JLabel nsDomainLabel = new JLabel(messages.getString("dnsserver_msg16"));
        nsDomainLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(nsDomainLabel);
        hBox.add(Box.createHorizontalStrut(5));
        this.nsDomainField = new JTextField();
        this.nsDomainField.setPreferredSize(new Dimension(275, 25));
        this.nsDomainField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.nsDomainField);
            }
        });
        hBox.add(this.nsDomainField);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JLabel nsDomainServerLabel = new JLabel(messages.getString("dnsserver_msg17"));
        nsDomainServerLabel.setPreferredSize(new Dimension(170, 25));
        hBox.add(nsDomainServerLabel);
        hBox.add(Box.createHorizontalStrut(5));
        this.nsDomainServerField = new JTextField();
        this.nsDomainServerField.setPreferredSize(new Dimension(275, 25));
        this.nsDomainServerField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e) {
                GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.nsDomainServerField);
            }
        });
        hBox.add(this.nsDomainServerField);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));
        JButton nsAddButton = new JButton(messages.getString("dnsserver_msg6"));
        nsAddButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                if (GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.nsDomainField) && GUIApplicationDNSServerWindow.this.checkFQDN(GUIApplicationDNSServerWindow.this.nsDomainServerField)) {
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).hinzuRecord(GUIApplicationDNSServerWindow.this.nsDomainField.getText(), "NS", GUIApplicationDNSServerWindow.this.nsDomainServerField.getText());
                    GUIApplicationDNSServerWindow.this.nsDomainField.setText("");
                    GUIApplicationDNSServerWindow.this.nsDomainServerField.setText("");
                    GUIApplicationDNSServerWindow.this.updateNSRecordsTable();
                } else {
                    GUIApplicationDNSServerWindow.this.showMessageDialog(I18n.messages.getString("dnsserver_msg22"));
                }
            }
        });
        hBox.add(nsAddButton);
        JButton nsRemoveButton = new JButton(messages.getString("dnsserver_msg7"));
        nsRemoveButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                int zeilenNummer = GUIApplicationDNSServerWindow.this.nsRecordsTable.getSelectedRow();
                if (zeilenNummer != -1) {
                    String domainname = GUIApplicationDNSServerWindow.this.nsRecordsTable.getValueAt(zeilenNummer, 0).toString();
                    ((DNSServer)GUIApplicationDNSServerWindow.this.holeAnwendung()).loescheResourceRecord(domainname, "NS");
                    LOG.debug("GUIApplicationDNSServerWindow: NS-Eintrag " + domainname + " geloescht");
                    GUIApplicationDNSServerWindow.this.updateNSRecordsTable();
                }
            }
        });
        hBox.add(nsRemoveButton);
        vBox.add(hBox);
        vBox.add(Box.createVerticalStrut(5));
        DefaultTableModel tabellenModell = new DefaultTableModel(0, 2);
        this.nsRecordsTable = new DNSConfigTable((TableModel)tabellenModell, true, "NS");
        this.nsRecordsTable.setParentGUI(this);
        this.nsRecordsTable.setIntercellSpacing(new Dimension(5, 5));
        this.nsRecordsTable.setRowHeight(30);
        this.nsRecordsTable.setShowGrid(false);
        this.nsRecordsTable.setFillsViewportHeight(true);
        this.nsRecordsTable.setBackground(filius.modern.Theme.SURFACE);
        this.nsRecordsTable.setShowHorizontalLines(true);
        TableColumnModel tcm = this.nsRecordsTable.getColumnModel();
        tcm.getColumn(0).setHeaderValue(messages.getString("dnsserver_msg18"));
        tcm.getColumn(1).setHeaderValue(messages.getString("dnsserver_msg19"));
        JScrollPane scrollPane = new JScrollPane(this.nsRecordsTable);
        vBox.add(scrollPane);
        nsPanel.add((Component)vBox, "Center");
        return nsPanel;
    }

    public void updateARecordsTable() {
        this.updateRecordsTable(this.aRecordsTable);
    }

    public void updateNSRecordsTable() {
        this.updateRecordsTable(this.nsRecordsTable);
    }

    public void updateMXRecordsTable() {
        this.updateRecordsTable(this.mxRecordsTable);
    }

    private synchronized void updateRecordsTable(DNSConfigTable table) {
        String type = table.getType();
        List<ResourceRecord> tempListe = ((DNSServer)this.holeAnwendung()).holeResourceRecords();
        ArrayList<ResourceRecord> recordsOfSelectedType = new ArrayList<ResourceRecord>();
        for (ResourceRecord rr : tempListe) {
            if (!rr.getType().equals(type)) continue;
            recordsOfSelectedType.add(rr);
        }
        ((DefaultTableModel)table.getModel()).setRowCount(recordsOfSelectedType.size());
        int row = 0;
        for (ResourceRecord rr : recordsOfSelectedType) {
            table.setValueAt(rr.getDomainname(), row, 0);
            table.setValueAt(rr.getRdata(), row, 1);
            ++row;
        }
    }

    /**
     * Removes spaces at the start and end of the field. The original set the text on every key, which put the caret back
     * to the end (arrow left did not work); now the text is only set when there is something to remove, and the caret
     * stays where it was.
     */
    private static void trimmen(JTextField feld) {
        String text = feld.getText();
        String ohne = text.trim();
        if (ohne.equals(text)) {
            return;
        }
        int vorne = text.indexOf(ohne.isEmpty() ? text : ohne);
        if (ohne.isEmpty()) {
            vorne = text.length();
        }
        int pos = feld.getCaretPosition() - vorne;
        feld.setText(ohne);
        feld.setCaretPosition(Math.max(0, Math.min(ohne.length(), pos)));
    }

    private boolean checkIP(JTextField feld) {
        trimmen(feld);
        boolean validAddress = EingabenUeberpruefung.isGueltig(feld.getText(), EingabenUeberpruefung.musterIpAdresse);
        if (validAddress) {
            feld.setForeground(EingabenUeberpruefung.farbeRichtig);
        } else {
            feld.setForeground(EingabenUeberpruefung.farbeFalsch);
        }
        return validAddress;
    }

    private boolean checkFQDN(JTextField feld) {
        trimmen(feld);
        boolean validFqdn = EingabenUeberpruefung.isGueltig(feld.getText(), EingabenUeberpruefung.musterDomain);
        if (validFqdn) {
            feld.setForeground(EingabenUeberpruefung.farbeRichtig);
        } else {
            feld.setForeground(EingabenUeberpruefung.farbeFalsch);
        }
        return validFqdn;
    }

    private void aktualisieren() {
        if (((DNSServer)this.holeAnwendung()).isAktiv()) {
            this.buttonStart.setText(messages.getString("dnsserver_msg14"));
        } else {
            this.buttonStart.setText(messages.getString("dnsserver_msg1"));
        }
        this.updateARecordsTable();
        this.updateMXRecordsTable();
        this.updateNSRecordsTable();
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        if ("A".equals(arg1)) {
            this.updateARecordsTable();
        } else if ("NS".equals(arg1)) {
            this.updateNSRecordsTable();
        } else if ("MX".equals(arg1)) {
            this.updateMXRecordsTable();
        } else if (arg1 != null) {
            this.aktualisieren();
        }
    }
}

