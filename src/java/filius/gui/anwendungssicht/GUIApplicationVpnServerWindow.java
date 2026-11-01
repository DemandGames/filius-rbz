/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 * Window of the VPN server: start/stop, VPN addresses, network behind the tunnel, user accounts and a log.
 */
package filius.gui.anwendungssicht;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.rahmenprogramm.I18n;
import filius.software.vpn.Vpn;
import filius.software.vpn.VpnServer;

public class GUIApplicationVpnServerWindow extends GUIApplicationWindow {
    private static final long serialVersionUID = 1L;

    private JButton start;
    private JLabel zustand;
    private JTextField netz;
    private JLabel serverAdresse;
    private JTextField freigabe;
    private JTextField name;
    private JTextField passwort;
    private DefaultListModel<String> konten;
    private JList<String> kontenListe;
    private JTextArea log;
    private boolean aktualisiere;

    public GUIApplicationVpnServerWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        this.aufbauen();
        this.aktualisieren();
    }

    private VpnServer server() {
        return (VpnServer) this.holeAnwendung();
    }

    private void aufbauen() {
        JPanel inhalt = new JPanel(new BorderLayout());

        JPanel oben = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        this.start = new JButton(I18n.messages.getString("webserver_msg1"));
        this.start.addActionListener(e -> this.server().setAktiv(!this.server().isAktiv()));
        oben.add(this.start);
        JLabel port = new JLabel(Texts.format("vpn.s.port", String.valueOf(Vpn.PORT)));
        port.setForeground(Theme.INK_2);
        oben.add(Box.createHorizontalStrut(10));
        oben.add(port);
        oben.add(Box.createHorizontalStrut(10));
        this.zustand = new JLabel();
        this.zustand.setFont(Theme.font(Font.BOLD, 12.5f));
        oben.add(this.zustand);

        JPanel einstellungen = new JPanel(new GridBagLayout());
        einstellungen.setBorder(rahmen(Texts.get("vpn.s.settings")));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        einstellungen.add(new JLabel(Texts.get("vpn.s.net")), c);
        c.gridx = 1;
        this.netz = new JTextField(12);
        this.netz.setToolTipText(Texts.get("vpn.s.net.tip"));
        einstellungen.add(this.netz, c);
        c.gridx = 2;
        this.serverAdresse = new JLabel();
        this.serverAdresse.setForeground(Theme.INK_2);
        einstellungen.add(this.serverAdresse, c);
        c.gridx = 0;
        c.gridy = 1;
        einstellungen.add(new JLabel(Texts.get("vpn.s.share")), c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.freigabe = new JTextField(16);
        this.freigabe.setToolTipText(Texts.get("vpn.s.share.tip"));
        einstellungen.add(this.freigabe, c);
        this.netz.getDocument().addDocumentListener(aenderung(() -> this.server().setVpnNetz(this.netz.getText())));
        this.freigabe.getDocument().addDocumentListener(aenderung(() -> this.server().setFreigabe(this.freigabe.getText())));

        JPanel benutzer = new JPanel(new BorderLayout(0, 4));
        benutzer.setBorder(rahmen(Texts.get("vpn.s.users")));
        JPanel neu = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        neu.add(new JLabel(Texts.get("vpn.s.name")));
        this.name = new JTextField(9);
        neu.add(this.name);
        neu.add(new JLabel(Texts.get("vpn.s.password")));
        this.passwort = new JTextField(9);
        neu.add(this.passwort);
        JButton hinzu = new JButton(Texts.get("vpn.s.add"));
        hinzu.addActionListener(e -> this.kontoHinzufuegen());
        this.passwort.addActionListener(e -> this.kontoHinzufuegen());
        neu.add(hinzu);
        JButton weg = new JButton(Texts.get("vpn.s.remove"));
        weg.addActionListener(e -> {
            String s = this.kontenListe.getSelectedValue();
            if (s != null) {
                this.server().kontoEntfernen(s.split(" ")[0]);
                this.aktualisieren();
            }
        });
        neu.add(weg);
        benutzer.add(neu, BorderLayout.NORTH);
        this.konten = new DefaultListModel<>();
        this.kontenListe = new JList<>(this.konten);
        this.kontenListe.setVisibleRowCount(3);
        this.kontenListe.setFont(Theme.mono(Font.PLAIN, 12.5f));
        benutzer.add(new JScrollPane(this.kontenListe), BorderLayout.CENTER);

        JPanel mitte = new JPanel();
        mitte.setLayout(new javax.swing.BoxLayout(mitte, javax.swing.BoxLayout.Y_AXIS));
        for (JPanel p : new JPanel[] { oben, einstellungen, benutzer }) {
            p.setAlignmentX(Component.LEFT_ALIGNMENT);
            mitte.add(p);
        }
        inhalt.add(mitte, BorderLayout.NORTH);

        this.log = new JTextArea();
        this.log.setEditable(false);
        // monospaced like the account list: the normal font has no arrow (→)
        this.log.setFont(Theme.mono(Font.PLAIN, 12f));
        this.log.setLineWrap(true);
        this.log.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(this.log);
        sp.setPreferredSize(new Dimension(300, 110));
        JPanel unten = new JPanel(new BorderLayout());
        unten.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        unten.add(sp, BorderLayout.CENTER);
        inhalt.add(unten, BorderLayout.CENTER);
        this.add(inhalt, BorderLayout.CENTER);
    }

    private static javax.swing.border.Border rahmen(String titel) {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8),
                BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titel), BorderFactory.createEmptyBorder(0, 4, 4, 4)));
    }

    private DocumentListener aenderung(Runnable r) {
        return new DocumentListener() {
            private void los() {
                if (!aktualisiere) {
                    r.run();
                    SwingUtilities.invokeLater(() -> zeigeAdressen());
                }
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                los();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                los();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                los();
            }
        };
    }

    private void kontoHinzufuegen() {
        String n = this.name.getText().trim();
        if (n.isEmpty() || n.contains(":") || n.contains(" ")) {
            return;
        }
        this.server().kontoHinzufuegen(n, this.passwort.getText().trim());
        this.name.setText("");
        this.passwort.setText("");
        this.aktualisieren();
    }

    private void zeigeAdressen() {
        VpnServer s = this.server();
        this.serverAdresse.setText(Texts.format("vpn.s.serverAddress", s.eigeneVpnAdresse()));
        this.freigabe.putClientProperty("JTextField.placeholderText", s.freigabeText());
    }

    private void aktualisieren() {
        VpnServer s = this.server();
        this.aktualisiere = true;
        try {
            this.start.setText(I18n.messages.getString(s.isAktiv() ? "webserver_msg2" : "webserver_msg1"));
            boolean laeuft = s.laeuft();
            this.zustand.setText(Texts.get(laeuft ? "vpn.s.running" : "vpn.s.stopped"));
            this.zustand.setForeground(laeuft ? Theme.LED_GREEN : Theme.INK_3);
            if (!this.netz.hasFocus() && !this.netz.getText().equals(s.getVpnNetz())) {
                this.netz.setText(s.getVpnNetz());
            }
            if (!this.freigabe.hasFocus() && !this.freigabe.getText().equals(s.getFreigabe())) {
                this.freigabe.setText(s.getFreigabe());
            }
            this.konten.clear();
            for (Map.Entry<String, String> e : s.konten().entrySet()) {
                this.konten.addElement(e.getKey() + "  :  " + e.getValue());
            }
            zeigeAdressen();
        } finally {
            this.aktualisiere = false;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            if (arg != null && this.log != null) {
                this.log.append(arg.toString() + "\n");
                this.log.setCaretPosition(this.log.getDocument().getLength());
            }
            try {
                this.aktualisieren();
            } catch (RuntimeException e) {
                // window not ready yet
            }
        });
    }
}
