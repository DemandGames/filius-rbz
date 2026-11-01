/*
 * Filius (RBZ Wirtschaft Kiel Version): new in Filius (RBZ Wirtschaft Kiel Version), not part of Filius 2.9.3.
 * Written by Thorben Demand @ RBZ Wirtschaft Kiel, 2026. Licensed like Filius under the GPL.
 * Window of the VPN client: server, name and password, connect/disconnect, the state of the tunnel and a log.
 */
package filius.gui.anwendungssicht;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.Observable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.software.vpn.VpnClient;

public class GUIApplicationVpnClientWindow extends GUIApplicationWindow {
    private static final long serialVersionUID = 1L;

    private JTextField server;
    private JTextField benutzer;
    private JPasswordField passwort;
    private JButton knopf;
    private Punkt punkt;
    private JLabel zustand;
    private JLabel details;
    private JTextArea log;
    private boolean aktualisiere;

    public GUIApplicationVpnClientWindow(GUIDesktopPanel desktop, String appName) {
        super(desktop, appName);
        this.aufbauen();
        this.aktualisieren();
    }

    private VpnClient client() {
        return (VpnClient) this.holeAnwendung();
    }

    /** coloured dot in front of the state */
    private static final class Punkt extends JPanel {
        private static final long serialVersionUID = 1L;
        Color farbe = Theme.INK_3;

        Punkt() {
            setOpaque(false);
            setPreferredSize(new Dimension(14, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(farbe);
            g2.fillOval(2, 2, 10, 10);
            g2.dispose();
        }
    }

    private void aufbauen() {
        JPanel inhalt = new JPanel(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8),
                BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(Texts.get("vpn.c.title")),
                        BorderFactory.createEmptyBorder(0, 4, 4, 4))));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 6);
        c.anchor = GridBagConstraints.WEST;
        this.server = new JTextField(16);
        this.server.putClientProperty("JTextField.placeholderText", Texts.get("vpn.c.server.placeholder"));
        this.benutzer = new JTextField(16);
        this.passwort = new JPasswordField(16);
        String[] labels = { Texts.get("vpn.c.server"), Texts.get("vpn.c.user"), Texts.get("vpn.c.password") };
        JTextField[] felder = { this.server, this.benutzer, this.passwort };
        for (int i = 0; i < 3; i++) {
            c.gridx = 0;
            c.gridy = i;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            form.add(new JLabel(labels[i]), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            form.add(felder[i], c);
        }
        this.server.getDocument().addDocumentListener(aenderung(() -> this.client().setServer(this.server.getText())));
        this.benutzer.getDocument().addDocumentListener(aenderung(() -> this.client().setBenutzer(this.benutzer.getText())));
        this.passwort.getDocument().addDocumentListener(aenderung(() -> this.client().setPasswort(new String(this.passwort.getPassword()))));
        c.gridx = 1;
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        this.knopf = new JButton(Texts.get("vpn.c.connect"));
        this.knopf.addActionListener(e -> {
            if (this.client().zustand() == VpnClient.Zustand.VERBUNDEN || this.client().zustand() == VpnClient.Zustand.VERBINDET) {
                this.client().trennen();
            } else {
                this.client().verbinden();
            }
        });
        this.passwort.addActionListener(e -> this.client().verbinden());
        form.add(this.knopf, c);

        JPanel status = new JPanel(new BorderLayout(0, 2));
        status.setBorder(BorderFactory.createEmptyBorder(6, 14, 4, 14));
        JPanel zeile = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        this.punkt = new Punkt();
        this.zustand = new JLabel();
        this.zustand.setFont(Theme.font(Font.BOLD, 13f));
        zeile.add(this.punkt);
        zeile.add(this.zustand);
        status.add(zeile, BorderLayout.NORTH);
        this.details = new JLabel();
        this.details.setForeground(Theme.INK_2);
        this.details.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 0));
        status.add(this.details, BorderLayout.CENTER);

        JPanel oben = new JPanel(new BorderLayout());
        oben.add(form, BorderLayout.NORTH);
        oben.add(status, BorderLayout.CENTER);
        inhalt.add(oben, BorderLayout.NORTH);

        this.log = new JTextArea();
        this.log.setEditable(false);
        // monospaced like the account list: the normal font has no arrow (→)
        this.log.setFont(Theme.mono(Font.PLAIN, 12f));
        this.log.setLineWrap(true);
        this.log.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(this.log);
        JPanel unten = new JPanel(new BorderLayout());
        unten.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        unten.add(sp, BorderLayout.CENTER);
        inhalt.add(unten, BorderLayout.CENTER);
        this.add(inhalt, BorderLayout.CENTER);
    }

    private DocumentListener aenderung(Runnable r) {
        return new DocumentListener() {
            private void los() {
                if (!aktualisiere) {
                    r.run();
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

    private void aktualisieren() {
        VpnClient v = this.client();
        this.aktualisiere = true;
        try {
            if (!this.server.hasFocus() && !this.server.getText().equals(v.getServer())) {
                this.server.setText(v.getServer());
            }
            if (!this.benutzer.hasFocus() && !this.benutzer.getText().equals(v.getBenutzer())) {
                this.benutzer.setText(v.getBenutzer());
            }
            if (!this.passwort.hasFocus() && !new String(this.passwort.getPassword()).equals(v.getPasswort())) {
                this.passwort.setText(v.getPasswort());
            }
        } finally {
            this.aktualisiere = false;
        }
        VpnClient.Zustand z = v.zustand();
        boolean an = z == VpnClient.Zustand.VERBUNDEN || z == VpnClient.Zustand.VERBINDET;
        this.knopf.setText(Texts.get(an ? "vpn.c.disconnect" : "vpn.c.connect"));
        for (JTextField f : new JTextField[] { this.server, this.benutzer, this.passwort }) {
            f.setEnabled(!an);
        }
        switch (z) {
        case VERBUNDEN:
            this.punkt.farbe = Theme.LED_GREEN;
            this.zustand.setText(Texts.get("vpn.c.state.connected"));
            this.details.setText(Texts.format("vpn.c.state.details", v.vpnAdresse(), v.tunnelNetz(), v.serverAdresse()));
            break;
        case VERBINDET:
            this.punkt.farbe = Theme.farbe(0xD3860B, 0xF5B43A);
            this.zustand.setText(Texts.get("vpn.c.state.connecting"));
            this.details.setText(v.meldung());
            break;
        case FEHLER:
            this.punkt.farbe = Theme.SIGNAL_RED;
            this.zustand.setText(Texts.get("vpn.c.state.error"));
            this.details.setText("<html><div style='width:420px'>" + v.meldung() + "</div></html>");
            break;
        default:
            this.punkt.farbe = Theme.INK_3;
            this.zustand.setText(Texts.get("vpn.c.state.disconnected"));
            this.details.setText(Texts.get("vpn.c.state.hint"));
        }
        this.zustand.setForeground(z == VpnClient.Zustand.FEHLER ? Theme.SIGNAL_RED : Theme.INK);
        this.punkt.repaint();
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
