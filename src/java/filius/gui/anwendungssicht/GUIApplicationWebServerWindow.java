/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUIApplicationWebServerWindow of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: new area "Encryption (HTTPS)": HTTPS on
 * port 443 on/off, HTTP on port 80 on/off, the names the certificate is made for, self-signed or
 * signed by the Filius certificate authority, and a button that shows the certificate. Start/stop,
 * virtual hosts and the log work as before.
 */
package filius.gui.anwendungssicht;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Observable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.tls.Tls;
import filius.rahmenprogramm.I18n;
import filius.software.www.WebServer;

public class GUIApplicationWebServerWindow extends GUIApplicationWindow {
    private static Logger LOG = LoggerFactory.getLogger(GUIApplicationWebServerWindow.class);
    private static final long serialVersionUID = 1L;
    private JPanel backPanel;
    private JTextArea logArea;
    private JButton buttonStart;
    private JCheckBox showVHosts;
    private VHostConfigTable vHostTable;
    private Box vHostBox = null;
    private Box logBox = null;

    private JCheckBox https;
    private JCheckBox http;
    private JTextField namen;
    private JCheckBox selbst;
    private JButton zeigen;
    private boolean aktualisiere;

    public GUIApplicationWebServerWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        this.initialisiereKomponenten();
        this.aktualisieren();
    }

    private WebServer server() {
        return (WebServer) this.holeAnwendung();
    }

    private void initialisiereKomponenten() {
        this.backPanel = new JPanel(new BorderLayout());
        this.buttonStart = new JButton(messages.getString("webserver_msg1"));
        this.buttonStart.addActionListener(e -> {
            if (this.buttonStart.getText().equals(I18n.messages.getString("webserver_msg1"))) {
                this.server().setAktiv(true);
            } else {
                this.server().setAktiv(false);
            }
        });
        Box upperBox = Box.createHorizontalBox();
        upperBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        upperBox.add(this.buttonStart);
        upperBox.add(Box.createHorizontalStrut(40));
        this.showVHosts = new JCheckBox();
        this.showVHosts.setText(messages.getString("webserver_msg6"));
        this.showVHosts.addActionListener(evt -> {
            this.server().setUseVHost(this.showVHosts.isSelected());
            this.aktualisieren();
        });
        upperBox.add(this.showVHosts);
        upperBox.add(Box.createHorizontalGlue());

        JPanel nord = new JPanel(new BorderLayout());
        nord.setOpaque(false);
        nord.add(upperBox, BorderLayout.NORTH);
        nord.add(this.httpsBereich(), BorderLayout.CENTER);
        this.backPanel.add((Component) nord, "North");

        this.vHostBox = Box.createVerticalBox();
        this.vHostBox.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel label = new JLabel(messages.getString("webserver_msg3"));
        Box vHostLabel = Box.createHorizontalBox();
        vHostLabel.add(label);
        DefaultTableModel tablemodel = new DefaultTableModel(5, 2);
        this.vHostTable = new VHostConfigTable((TableModel) tablemodel, true);
        this.vHostTable.setParentGUI(this);
        JScrollPane tableScrollPane = new JScrollPane(this.vHostTable);
        tableScrollPane.setVerticalScrollBarPolicy(20);
        tableScrollPane.setHorizontalScrollBarPolicy(31);
        this.vHostTable.getColumnModel().getColumn(0).setHeaderValue(messages.getString("webserver_msg4"));
        this.vHostTable.getColumnModel().getColumn(1).setHeaderValue(messages.getString("webserver_msg5"));
        this.vHostTable.setIntercellSpacing(new Dimension(5, 5));
        this.vHostTable.setRowHeight(26);
        this.vHostTable.setShowGrid(true);
        this.vHostTable.setFillsViewportHeight(true);
        this.vHostTable.setBackground(Color.WHITE);
        this.vHostTable.setShowHorizontalLines(true);
        this.vHostBox.add(vHostLabel);
        this.vHostBox.add(Box.createVerticalStrut(5));
        this.vHostBox.add(tableScrollPane);
        this.logArea = new JTextArea();
        this.logArea.setEditable(false);
        JScrollPane sPane = new JScrollPane(this.logArea);
        this.logBox = Box.createHorizontalBox();
        this.logBox.setBorder(BorderFactory.createEmptyBorder(6, 10, 10, 10));
        this.logBox.add(sPane);
        this.backPanel.add((Component) this.logBox, "South");
        this.add((Component) this.backPanel, "Center");
    }

    /** area "Encryption (HTTPS)" */
    private JPanel httpsBereich() {
        JPanel p = new JPanel();
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8),
                BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(Texts.get("ws.https.title")),
                        BorderFactory.createEmptyBorder(0, 4, 4, 4))));

        JPanel zeile1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        this.https = new JCheckBox(Texts.get("ws.https.on"));
        this.https.addActionListener(e -> {
            if (!this.aktualisiere) {
                this.server().setHttps(this.https.isSelected());
            }
        });
        this.http = new JCheckBox(Texts.get("ws.http.on"));
        this.http.addActionListener(e -> {
            if (!this.aktualisiere) {
                this.server().setHttp(this.http.isSelected());
            }
        });
        zeile1.add(this.https);
        zeile1.add(Box.createHorizontalStrut(18));
        zeile1.add(this.http);

        JPanel zeile2 = new JPanel(new BorderLayout(8, 0));
        zeile2.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 0));
        JLabel l = new JLabel(Texts.get("ws.cert.for"));
        this.namen = new JTextField();
        this.namen.putClientProperty("JTextField.placeholderText", Texts.get("ws.cert.placeholder"));
        this.namen.setToolTipText(Texts.get("ws.cert.tip"));
        this.namen.addActionListener(e -> this.namenUebernehmen());
        this.namen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                GUIApplicationWebServerWindow.this.namenUebernehmen();
            }
        });
        zeile2.add(l, BorderLayout.WEST);
        zeile2.add(this.namen, BorderLayout.CENTER);

        JPanel zeile3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        this.selbst = new JCheckBox(Texts.get("ws.cert.self"));
        this.selbst.addActionListener(e -> {
            if (!this.aktualisiere) {
                this.server().setSelbstSigniert(this.selbst.isSelected());
            }
        });
        this.zeigen = new JButton(Texts.get("ws.cert.show"));
        this.zeigen.addActionListener(e -> this.zeigeZertifikat());
        zeile3.add(this.selbst);
        zeile3.add(Box.createHorizontalStrut(18));
        zeile3.add(this.zeigen);

        for (JPanel z : new JPanel[] { zeile1, zeile2, zeile3 }) {
            z.setOpaque(false);
            z.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(z);
        }
        return p;
    }

    private void namenUebernehmen() {
        if (!this.aktualisiere && !this.namen.getText().trim().equals(this.server().getZertifikatNamen())) {
            this.server().setZertifikatNamen(this.namen.getText());
        }
    }

    /** the certificate of the server as it is sent in the TLS handshake */
    private void zeigeZertifikat() {
        this.namenUebernehmen();
        Tls.Zertifikat z = this.server().zertifikat();
        JPanel inhalt = new JPanel(new java.awt.GridBagLayout());
        inhalt.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.anchor = java.awt.GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.insets = new java.awt.Insets(0, 0, 8, 0);
        JLabel t = new JLabel(Texts.get("tls.certificate"));
        t.setFont(Theme.font(Font.BOLD, 14f));
        inhalt.add(t, c);
        String[][] zeilen = { { Texts.get("tls.f.for"), z.namenText() }, { Texts.get("tls.f.issuer"), z.aussteller },
                { Texts.get("tls.f.valid"), z.gueltigBis }, { Texts.get("tls.f.key"), z.schluesselKurz() } };
        for (String[] zeile : zeilen) {
            c.gridy++;
            c.gridwidth = 1;
            c.gridx = 0;
            c.insets = new java.awt.Insets(2, 0, 2, 12);
            JLabel a = new JLabel(zeile[0]);
            a.setForeground(Theme.INK_3);
            inhalt.add(a, c);
            c.gridx = 1;
            c.insets = new java.awt.Insets(2, 0, 2, 0);
            inhalt.add(new JLabel(zeile[1]), c);
        }
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new java.awt.Insets(10, 0, 0, 0);
        JLabel h = new JLabel("<html><div style='width:300px'>"
                + (z.vonZertifizierungsstelle() ? Texts.get("ws.cert.caHint") : Texts.get("ws.cert.selfHint")) + "</div></html>");
        h.setForeground(Theme.INK_2);
        inhalt.add(h, c);
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        popup.add(inhalt);
        popup.show(this.zeigen, 0, this.zeigen.getHeight());
    }

    private void showVHostTable(boolean tableVisible) {
        if (tableVisible) {
            this.backPanel.add((Component) this.vHostBox, "Center");
            this.logBox.setPreferredSize(new Dimension(300, 120));
        } else {
            this.backPanel.remove(this.vHostBox);
            this.logBox.setPreferredSize(new Dimension(300, 250));
        }
        this.showVHosts.setSelected(tableVisible);
        Dimension size = this.getSize();
        this.setSize(size);
        this.updateUI();
    }

    public void updateVHostTable() {
        String[][] vhosts = this.server().getVHostArray();
        DefaultTableModel tablemodel = (DefaultTableModel) this.vHostTable.getModel();
        tablemodel.setRowCount(0);
        for (int i = 0; i < vhosts.length; ++i) {
            Vector<String> v = new Vector<String>();
            v.add(vhosts[i][0] != null ? vhosts[i][0] : "");
            v.add(vhosts[i][1] != null ? vhosts[i][1] : "");
            tablemodel.addRow(v);
        }
        for (int i = vhosts.length; i < 5; ++i) {
            Vector<String> v = new Vector<String>();
            v.add("");
            v.add("");
            tablemodel.addRow(v);
        }
    }

    private void aktualisieren() {
        WebServer webserver = this.server();
        if (webserver.isAktiv()) {
            this.buttonStart.setText(messages.getString("webserver_msg2"));
        } else {
            this.buttonStart.setText(messages.getString("webserver_msg1"));
        }
        this.buttonStart.setEnabled(true);
        this.aktualisiere = true;
        try {
            this.https.setSelected(webserver.isHttps());
            this.http.setSelected(webserver.isHttp());
            this.selbst.setSelected(webserver.isSelbstSigniert());
            if (!this.namen.hasFocus() && !this.namen.getText().equals(webserver.getZertifikatNamen())) {
                this.namen.setText(webserver.getZertifikatNamen());
            }
        } finally {
            this.aktualisiere = false;
        }
        this.updateVHostTable();
        this.showVHostTable(webserver.isUseVHost());
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        if (this.logArea != null && arg1 != null) {
            this.logArea.append(arg1.toString() + "\n");
        }
        try {
            this.aktualisieren();
        } catch (Exception e) {
            LOG.debug("GUIApplicationWebServerWindow: update() Exception: " + e.getMessage());
        }
    }
}
