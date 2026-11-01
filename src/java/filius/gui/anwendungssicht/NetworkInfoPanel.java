/*
 * Filius (RBZ Wirtschaft Kiel Version): based on NetworkInfoPanel of Filius 2.9.3 (decompiled, GPL).
 * Changed: the network window of the desktop (symbol in the lower right corner) is a clear card. The addresses
 * are plain text without frames (in the original they were tiny input fields with a frame, in which the
 * addresses could hardly be read), in the font of addresses (JetBrains Mono), light and dark. The fields stay
 * read-only as in the original; the button "change" of the original was never shown and is gone.
 * A second click on the network symbol closes the window again (filius.modern.DesktopGroesse).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.anwendungssicht;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.Host;
import filius.modern.Theme;
import filius.rahmenprogramm.I18n;
import filius.software.system.Betriebssystem;

public class NetworkInfoPanel extends JPanel implements I18n {
    private static final long serialVersionUID = 1L;
    private final GUIDesktopPanel dp;
    private final JTextField ipField = wert();
    private final JTextField netmaskField = wert();
    private final JTextField gatewayField = wert();
    private final JTextField dnsField = wert();
    private final JTextField macField = wert();

    public NetworkInfoPanel(GUIDesktopPanel dp) {
        super(new GridBagLayout());
        this.dp = dp;
        this.setOpaque(false);
        this.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));
        JLabel titel = new JLabel(filius.modern.Texts.get("desktop.netz.titel"));
        titel.setFont(Theme.font(Font.BOLD, 13.5f));
        titel.setForeground(Theme.INK);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 8, 0);
        this.add(titel, c);
        this.zeile(1, messages.getString("network_msg1"), this.ipField);
        this.zeile(2, messages.getString("network_msg4"), this.netmaskField);
        this.zeile(3, messages.getString("network_msg3"), this.gatewayField);
        this.zeile(4, messages.getString("network_msg2"), this.dnsField);
        this.zeile(5, messages.getString("network_msg9"), this.macField);
        this.updateInfo();
    }

    private void zeile(int y, String name, JTextField feld) {
        JLabel l = new JLabel(name);
        l.setFont(Theme.font(Font.PLAIN, 12.5f));
        l.setForeground(Theme.INK_2);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 18);
        this.add(l, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(2, 0, 2, 0);
        this.add(feld, c);
    }

    /** an address as plain text: no frame, no background, selectable but read-only */
    private static JTextField wert() {
        JTextField f = new JTextField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                this.setForeground(Theme.INK);
                super.paintComponent(g);
            }
        };
        f.setEditable(false);
        f.setFocusable(false);
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder());
        f.putClientProperty("JComponent.outline", null);
        f.setFont(Theme.mono(Font.PLAIN, 12.5f));
        f.setColumns(16);
        return f;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.SURFACE);
        g2.fillRoundRect(0, 0, this.getWidth() - 1, this.getHeight() + 12, 14, 14);
        g2.setColor(Theme.LINE_STRONG);
        g2.drawRoundRect(0, 0, this.getWidth() - 1, this.getHeight() + 12, 14, 14);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(300, d.width), d.height);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            this.updateInfo();
        }
        super.setVisible(b);
    }

    public void updateInfo() {
        Betriebssystem bs = this.dp.getBetriebssystem();
        if (bs == null || !(bs.getKnoten() instanceof Host)) {
            return;
        }
        NetzwerkInterface nic = ((Host) bs.getKnoten()).getNetzwerkInterfaces().get(0);
        this.ipField.setText(nic.getIp());
        this.netmaskField.setText(nic.getSubnetzMaske());
        this.gatewayField.setText(bs.getStandardGateway());
        this.dnsField.setText(bs.getDNSServer());
        this.macField.setText(nic.getMac());
    }
}
