/*
 * Filius (RBZ Wirtschaft Kiel Version): based on MessageDetailsPanel of Filius 2.9.3 (GPL).
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026: the details of a message are shown as a stack
 * of layer cards (highest layer on top) instead of a text tree, either for the TCP/IP model Filius is
 * built on or for the OSI model (see filius.modern.LayerModel). The data shown is the same as before:
 * source, destination, protocol and details of every layer of the selected message.
 * TLS (HTTPS): the card explains the step of the handshake; sender and receiver of encrypted data also see
 * what is inside (they have the session key), every other device only sees that it cannot be read. In the
 * OSI view TLS is layer 6 (presentation) and the decrypted HTTP layer 7.
 * VPN (UDP port 1194): the card explains the record; the two ends of the tunnel also see what is inside, for data
 * the whole inner IP packet.
 */
package filius.gui.nachrichtensicht;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import filius.modern.LayerModel;
import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.tls.Tls;
import filius.rahmenprogramm.I18n;
import filius.rahmenprogramm.nachrichten.Lauscher;

public class MessageDetailsPanel extends MessageDetails implements Scrollable {
    private static final long serialVersionUID = 1L;
    private static final int BADGE_WIDTH = 172;
    private static final int ARC = 12;

    private final String macAddress;
    private int frameNumber = -1;
    /** kept as a field: LayerModel only holds a weak reference */
    private final Runnable onModelChange;

    public MessageDetailsPanel(String macAddress) {
        this.macAddress = macAddress;
        this.onModelChange = () -> {
            if (this.frameNumber > 0) {
                this.update(this.macAddress, this.frameNumber);
            }
        };
        this.setLayout(new BorderLayout());
        this.setBackground(Theme.SURFACE);
        LayerModel.addListener(this.onModelChange);
        this.showEmpty();
    }

    @Override
    public void clear() {
        this.frameNumber = -1;
        this.showEmpty();
    }

    private void showEmpty() {
        this.removeAll();
        JLabel hint = new JLabel(Texts.get("details.empty"), SwingConstants.CENTER);
        hint.setFont(Theme.font(Font.PLAIN, 13f));
        hint.setForeground(Theme.INK_3);
        hint.setBorder(BorderFactory.createEmptyBorder(24, 16, 24, 16));
        this.add(hint, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }

    @Override
    public void update(String interfaceId, int frameNumber) {
        Object[][] daten = Lauscher.getLauscher().getDaten(this.macAddress, false, 0);
        Object[][] layers = new Object[4][];
        String no = null;
        String time = null;
        for (Object[] row : daten) {
            if (row[0] == null || !row[0].toString().equals(Integer.toString(frameNumber))) {
                continue;
            }
            no = row[0].toString();
            time = String.valueOf(row[1]);
            int index = LayerModel.tcpipIndex(row[5]);
            if (index >= 0) {
                layers[index] = row;
            }
        }
        if (no == null) {
            this.clear();
            return;
        }
        this.frameNumber = frameNumber;
        this.tlsAuswerten(layers);
        this.vpnAuswerten(layers);
        this.removeAll();
        this.add(this.build(no, time, layers), BorderLayout.NORTH);
        this.revalidate();
        this.repaint();
        // every message (and every switch of the model) starts at the top layer
        SwingUtilities.invokeLater(() -> {
            if (this.getParent() instanceof JViewport) {
                ((JViewport) this.getParent()).setViewPosition(new java.awt.Point(0, 0));
            }
        });
    }

    // ---------------------------------------------------------------- TLS

    /** the selected message is TLS */
    private boolean tls;
    /** plain text of encrypted data, only if this device is sender or receiver */
    private Tls.Klartext klartext;
    private String tlsArt;

    private void tlsAuswerten(Object[][] layers) {
        this.tls = layers[3] != null && Lauscher.TLS.equals(String.valueOf(layers[3][4]));
        this.klartext = null;
        this.tlsArt = null;
        if (!this.tls) {
            return;
        }
        String payload = layers[3][6] == null ? "" : layers[3][6].toString();
        this.tlsArt = Tls.art(payload);
        boolean endpunkt = false;
        try {
            filius.hardware.NetzwerkInterface nic = Lauscher.getLauscher().getInterfaceByID(this.macAddress);
            String ip = nic == null ? null : nic.getIp();
            endpunkt = ip != null && layers[1] != null && (ip.equals(String.valueOf(layers[1][2])) || ip.equals(String.valueOf(layers[1][3])));
        } catch (Exception e) {
            endpunkt = false;
        }
        if (endpunkt) {
            this.klartext = Tls.klartext(payload);
        }
    }

    private boolean tlsDaten() {
        return this.tls && (this.tlsArt == null || this.tlsArt.startsWith("Application Data"));
    }

    /** what this step of TLS does */
    private String tlsBedeutung() {
        if (this.tlsArt == null) {
            return Texts.get("tls.mean.part");
        }
        String a = this.tlsArt.startsWith("Application Data") ? "data" : this.tlsArt.split("[ +]")[0];
        String t = Texts.get("tls.mean." + a);
        return t.startsWith("tls.mean.") ? null : t;
    }

    /** the decrypted content, or why it cannot be read here */
    private String tlsInhalt() {
        if (!this.tlsDaten()) {
            return null;
        }
        if (this.klartext == null) {
            return Texts.get("tls.hidden");
        }
        String t = this.klartext.text.length() > 2000 ? this.klartext.text.substring(0, 2000) + " …" : this.klartext.text;
        return this.klartext.teile > 1 ? Texts.format("tls.partOf", this.klartext.teil, this.klartext.teile) + "\n" + t : t;
    }

    /** TLS card: protocol, record, meaning and (for data) the content */
    private JComponent tlsCard(int number, String name, Color color, Object[] data, boolean mitInhalt) {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int r = 0;
        r = field(fields, r, I18n.messages.getString("rp_lauscher_msg5"), data[4], Theme.font(Font.BOLD, 13f));
        r = field(fields, r, Texts.get("tls.meaning"), this.tlsBedeutung(), Theme.font(Font.PLAIN, 12.5f));
        String detailsLabel = I18n.messages.getString("rp_lauscher_msg7");
        if (data[6] != null) {
            detailsLabel += " (" + Texts.format("details.bytes", data[6].toString().length()) + ")";
        }
        r = field(fields, r, detailsLabel, data[6], Theme.mono(Font.PLAIN, 12.5f));
        if (mitInhalt) {
            field(fields, r, Texts.get(this.klartext != null ? "tls.content" : "tls.contentHidden"), this.tlsInhalt(),
                    this.klartext != null ? Theme.mono(Font.PLAIN, 12.5f) : Theme.font(Font.ITALIC, 12.5f));
        }
        return new Card(number, name, color, fields, false);
    }

    // ---------------------------------------------------------------- VPN

    /** the selected message is a VPN record */
    private boolean vpn;
    private String vpnArt;
    private filius.software.vpn.Vpn.Klartext vpnKlartext;

    private void vpnAuswerten(Object[][] layers) {
        this.vpn = layers[3] != null && Lauscher.VPN.equals(String.valueOf(layers[3][4]));
        this.vpnArt = null;
        this.vpnKlartext = null;
        if (!this.vpn) {
            return;
        }
        String payload = layers[3][6] == null ? "" : layers[3][6].toString();
        this.vpnArt = filius.software.vpn.Vpn.art(payload);
        if (this.istEndpunkt(layers)) {
            this.vpnKlartext = filius.software.vpn.Vpn.klartext(payload);
        }
    }

    private boolean istEndpunkt(Object[][] layers) {
        try {
            filius.hardware.NetzwerkInterface nic = Lauscher.getLauscher().getInterfaceByID(this.macAddress);
            String ip = nic == null ? null : nic.getIp();
            return ip != null && layers[1] != null && (ip.equals(String.valueOf(layers[1][2])) || ip.equals(String.valueOf(layers[1][3])));
        } catch (Exception e) {
            return false;
        }
    }

    /** records whose content is encrypted */
    private boolean vpnVerschluesselt() {
        return "Login".equals(this.vpnArt) || "Accept".equals(this.vpnArt) || "Data".equals(this.vpnArt);
    }

    /** VPN card: protocol, meaning, record and (if encrypted) the content */
    private JComponent vpnCard(int number, String name, Color color, Object[] data) {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int r = 0;
        r = field(fields, r, I18n.messages.getString("rp_lauscher_msg5"), data[4], Theme.font(Font.BOLD, 13f));
        String bedeutung = this.vpnArt == null ? null : Texts.get("vpn.mean." + this.vpnArt);
        if (bedeutung != null && !bedeutung.startsWith("vpn.mean.")) {
            r = field(fields, r, Texts.get("tls.meaning"), bedeutung, Theme.font(Font.PLAIN, 12.5f));
        }
        String detailsLabel = I18n.messages.getString("rp_lauscher_msg7");
        if (data[6] != null) {
            detailsLabel += " (" + Texts.format("details.bytes", data[6].toString().length()) + ")";
        }
        r = field(fields, r, detailsLabel, data[6], Theme.mono(Font.PLAIN, 12.5f));
        if (this.vpnVerschluesselt()) {
            if (this.vpnKlartext != null) {
                String t = this.vpnKlartext.anzeige;
                t = t.length() > 2000 ? t.substring(0, 2000) + " …" : t;
                field(fields, r, Texts.get("tls.content"), t, Theme.mono(Font.PLAIN, 12.5f));
            } else {
                field(fields, r, Texts.get("tls.contentHidden"), Texts.get("vpn.hidden"), Theme.font(Font.ITALIC, 12.5f));
            }
        }
        return new Card(number, name, color, fields, false);
    }

    // ---------------------------------------------------------------- layout

    private JComponent build(String no, String time, Object[][] layers) {
        boolean osi = LayerModel.isOsi();
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));

        JLabel header = new JLabel(I18n.messages.getString("rp_lauscher_msg1") + " " + no + "   ·   "
                + I18n.messages.getString("rp_lauscher_msg2") + " " + time);
        header.setFont(Theme.font(Font.BOLD, 13.5f));
        header.setForeground(Theme.INK);
        root.add(header, gbc(0, 0, 1, 1, 1, new Insets(0, 0, 10, 0)));

        if (osi) {
            JLabel tcpipHead = new JLabel(Texts.get("osi.tcpip"), SwingConstants.CENTER);
            tcpipHead.setFont(Theme.font(Font.PLAIN, 11.5f));
            tcpipHead.setForeground(Theme.INK_3);
            root.add(tcpipHead, gbc(1, 0, 1, 1, 0, new Insets(0, 8, 10, 0)));
            boolean hasApplication = layers[3] != null;
            for (int n = 7; n >= 1; n--) {
                int row = 8 - n;
                int index = LayerModel.tcpipOfOsi(n);
                String label = LayerModel.osiName(n);
                Color strong = osiColor(n);
                JComponent card;
                if (n == 7 && this.tls) {
                    // TLS: the application (HTTP) is inside the encrypted data
                    if (this.tlsDaten() && this.klartext != null) {
                        Object[] http = new Object[] { null, null, null, null, "HTTP", null, this.tlsInhalt() };
                        card = dataCard(n, label, strong, http, true);
                    } else {
                        card = textCard(n, label, strong, Texts.get(this.tlsDaten() ? "tls.hidden" : "osi.7.tls.handshake"), !this.tlsDaten());
                    }
                } else if (n == 6 && this.tls) {
                    card = tlsCard(n, label, strong, layers[3], false);
                } else if (n == 7 && this.vpn) {
                    card = vpnCard(n, label, strong, layers[3]);
                } else if (n == 2 || n == 3 || n == 4 || n == 7) {
                    card = layers[index] != null ? dataCard(n, label, strong, layers[index], index == 3)
                            : textCard(n, label, strong, Texts.get("osi.absent"), true);
                } else if (n == 1) {
                    card = textCard(n, label, strong, Texts.get("osi.1.hint"), false);
                } else {
                    card = hasApplication ? textCard(n, label, strong, Texts.get("osi." + n + ".hint"), false)
                            : textCard(n, label, strong, Texts.get("osi.absent"), true);
                }
                root.add(card, gbc(0, row, 1, 1, 1, new Insets(0, 0, 6, 0)));
            }
            // the matching TCP/IP layers next to the OSI stack: application 7-5, transport 4, internet 3, link 2-1
            int[][] groups = { { 3, 1, 3 }, { 2, 4, 1 }, { 1, 5, 1 }, { 0, 6, 2 } };
            for (int[] g : groups) {
                GroupLabel group = new GroupLabel(LayerModel.tcpipName(g[0]), tcpipColor(g[0]), layers[g[0]] == null);
                root.add(group, gbc(1, g[1], 1, g[2], 0, new Insets(0, 8, 6, 0)));
            }
        } else {
            int row = 1;
            for (int index = 3; index >= 0; index--) {
                if (layers[index] == null) {
                    continue;
                }
                JComponent card = index == 3 && this.tls ? tlsCard(0, LayerModel.tcpipName(index), tcpipColor(index), layers[index], true)
                        : index == 3 && this.vpn ? vpnCard(0, LayerModel.tcpipName(index), tcpipColor(index), layers[index])
                        : dataCard(0, LayerModel.tcpipName(index), tcpipColor(index), layers[index], index == 3);
                root.add(card, gbc(0, row++, 1, 1, 1, new Insets(0, 0, 6, 0)));
            }
        }
        return root;
    }

    private static GridBagConstraints gbc(int x, int y, int w, int h, double weightx, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        c.weightx = weightx;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = insets;
        return c;
    }

    /** A layer with data: source, destination, protocol, details. */
    private JComponent dataCard(int number, String name, Color color, Object[] data, boolean application) {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int r = 0;
        r = field(fields, r, I18n.messages.getString("rp_lauscher_msg3"), data[2], Theme.mono(Font.PLAIN, 12.5f));
        r = field(fields, r, I18n.messages.getString("rp_lauscher_msg4"), data[3], Theme.mono(Font.PLAIN, 12.5f));
        r = field(fields, r, I18n.messages.getString("rp_lauscher_msg5"), data[4], Theme.font(Font.BOLD, 13f));
        String detailsLabel = I18n.messages.getString("rp_lauscher_msg7");
        if (application && data[6] != null) {
            detailsLabel += " (" + Texts.format("details.bytes", data[6].toString().length()) + ")";
        }
        field(fields, r, detailsLabel, data[6], Theme.mono(Font.PLAIN, 12.5f));
        return new Card(number, name, color, fields, false);
    }

    private static int field(JPanel panel, int row, String label, Object value, Font font) {
        if (value == null || value.toString().trim().isEmpty()) {
            return row;
        }
        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.PLAIN, 12f));
        l.setForeground(Theme.INK_3);
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(row == 0 ? 1 : 5, 0, 0, 14);
        panel.add(l, lc);
        JTextArea v = text(value.toString(), font, Theme.INK);
        GridBagConstraints vc = new GridBagConstraints();
        vc.gridx = 1;
        vc.gridy = row;
        vc.weightx = 1;
        vc.fill = GridBagConstraints.HORIZONTAL;
        vc.anchor = GridBagConstraints.NORTHWEST;
        vc.insets = new Insets(row == 0 ? 0 : 4, 0, 0, 0);
        panel.add(v, vc);
        return row + 1;
    }

    /** A layer without data of its own (OSI 1, 5, 6) or a layer this message does not contain. */
    private JComponent textCard(int number, String name, Color color, String message, boolean absent) {
        JTextArea t = text(message, Theme.font(absent ? Font.PLAIN : Font.ITALIC, 12.5f), absent ? Theme.INK_3 : Theme.INK_2);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(t, BorderLayout.CENTER);
        return new Card(number, name, color, holder, absent);
    }

    private static JTextArea text(String value, Font font, Color color) {
        JTextArea area = new JTextArea(value);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(font);
        area.setForeground(color);
        area.setColumns(8);
        return area;
    }

    // ---------------------------------------------------------------- colours

    private static Color tcpipColor(int index) {
        switch (index) {
            case 0:
                return AggregatedMessageTable.LayerColorVar.LINK.backgroundColor;
            case 1:
                return AggregatedMessageTable.LayerColorVar.INTERNET.backgroundColor;
            case 2:
                return AggregatedMessageTable.LayerColorVar.TRANSPORT.backgroundColor;
            default:
                return AggregatedMessageTable.LayerColorVar.APPLICATION.backgroundColor;
        }
    }

    /** OSI layers take the colour of the TCP/IP layer they belong to; 1, 5 and 6 a lighter shade of it. */
    private static Color osiColor(int n) {
        switch (n) {
            case 1:
                return AggregatedMessageTable.LayerColorVar.LINK.backgroundColorFine;
            case 5:
            case 6:
                return AggregatedMessageTable.LayerColorVar.APPLICATION.backgroundColorFine;
            default:
                return tcpipColor(LayerModel.tcpipOfOsi(n));
        }
    }

    // ---------------------------------------------------------------- components

    /** Rounded card: coloured badge with number and name on the left, contents on the right. */
    private static final class Card extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int number;
        private final String name;
        private final Color color;
        private final boolean muted;

        Card(int number, String name, Color color, JComponent content, boolean muted) {
            super(new BorderLayout());
            this.number = number;
            this.name = name;
            this.color = color;
            this.muted = muted;
            this.setOpaque(false);
            JComponent spacer = new JComponent() {
                private static final long serialVersionUID = 1L;

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(BADGE_WIDTH, 40);
                }
            };
            this.add(spacer, BorderLayout.WEST);
            content.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
            this.add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                int w = this.getWidth();
                int h = this.getHeight();
                Shape shape = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, ARC, ARC);
                Color tint = Theme.mix(Theme.SURFACE, this.color, this.muted ? 0.12 : 0.28);
                g2.setColor(tint);
                g2.fill(shape);
                g2.clip(shape);
                g2.setColor(this.muted ? Theme.mix(Theme.SURFACE, this.color, 0.55) : this.color);
                g2.fillRect(0, 0, BADGE_WIDTH, h);
                g2.setClip(null);
                g2.setColor(Theme.mix(this.color, Theme.INK, this.muted ? 0.08 : 0.18));
                g2.draw(shape);
                // badge text
                Color ink = this.muted ? Theme.INK_3 : Theme.INK;
                g2.setColor(ink);
                int x = 14;
                if (this.number > 0) {
                    Font big = Theme.font(Font.BOLD, 20f);
                    g2.setFont(big);
                    FontMetrics fm = g2.getFontMetrics();
                    String s = Integer.toString(this.number);
                    int baseline = Math.min(h / 2, 30) + fm.getAscent() / 2 - 2;
                    g2.drawString(s, x, baseline);
                    x += 26;
                    g2.setFont(Theme.font(Font.BOLD, 13.5f));
                    g2.drawString(this.name, x, baseline - 1);
                } else {
                    g2.setFont(Theme.font(Font.BOLD, 13.5f));
                    FontMetrics fm = g2.getFontMetrics();
                    int baseline = Math.min(h / 2, 30) + fm.getAscent() / 2 - 2;
                    g2.drawString(this.name, x, baseline);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** The TCP/IP layer that one or more OSI layers belong to, drawn as a bracket next to them. */
    private static final class GroupLabel extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String name;
        private final Color color;
        private final boolean muted;

        GroupLabel(String name, Color color, boolean muted) {
            this.name = name;
            this.color = color;
            this.muted = muted;
            this.setFont(Theme.font(Font.BOLD, 12.5f));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = this.getFontMetrics(this.getFont());
            return new Dimension(Math.max(118, fm.stringWidth(this.name) + 24), 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                int w = this.getWidth();
                int h = this.getHeight();
                Shape shape = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, ARC, ARC);
                g2.setColor(this.muted ? Theme.mix(Theme.SURFACE, this.color, 0.25) : Theme.mix(Theme.SURFACE, this.color, 0.6));
                g2.fill(shape);
                g2.setColor(Theme.mix(this.color, Theme.INK, 0.18));
                g2.draw(shape);
                g2.setFont(this.getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(this.muted ? Theme.INK_3 : Theme.INK);
                g2.drawString(this.name, (w - fm.stringWidth(this.name)) / 2f, (h + fm.getAscent() - fm.getDescent()) / 2f);
            } finally {
                g2.dispose();
            }
        }
    }

    // ---------------------------------------------------------------- scrolling: follow the width of the view

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return this.getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(16, visibleRect.height - 32);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        Component parent = this.getParent();
        return parent instanceof JViewport && parent.getHeight() > this.getPreferredSize().height;
    }
}
