/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JSidebarButton of Filius 2.9.3 (GPL). Only the painting changed:
 * rounded selection and hover highlight, name tag behind the label and a glowing link LED for
 * connected modems. Size, layout and behaviour are unchanged.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.netzwerksicht;

import filius.gui.JMainFrame;
import filius.hardware.NetzwerkInterface;
import filius.hardware.knoten.Host;
import filius.hardware.knoten.InternetKnoten;
import filius.modern.Enhancer;
import filius.modern.Theme;
import filius.rahmenprogramm.I18n;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSidebarButton
extends JLabel
implements Observer,
I18n {
    private static Logger LOG = LoggerFactory.getLogger(JSidebarButton.class);
    private static final int BORDER = 8;
    public static final String SIDEBAR_ITEM = "filiusModern.sidebarItem";
    private String typ;
    private boolean selektiert;
    private boolean modemVerbunden;
    private boolean isTemporaryText;
    private String originalText;
    private Color originalTextColor;

    public boolean isSelektiert() {
        return this.selektiert;
    }

    public void setSelektiert(boolean selektiert) {
        this.selektiert = selektiert;
    }

    public String getTyp() {
        return this.typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public JSidebarButton() {
        this.initAlignment();
    }

    private void initAlignment() {
        this.setVerticalTextPosition(3);
        this.setHorizontalTextPosition(0);
        this.setAlignmentX(0.5f);
        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public JSidebarButton(Icon icon, String typ) {
        super(icon, 0);
        this.typ = typ;
        this.initAlignment();
    }

    public JSidebarButton(String text, Icon icon, String typ) {
        super(text, icon, 0);
        this.typ = typ;
        this.initAlignment();
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle((int)super.getBounds().getX(), (int)super.getBounds().getY(), this.getWidth(), this.getHeight());
    }

    public boolean inBounds(int x, int y) {
        return x >= this.getX() && x <= this.getX() + this.getWidth() && y >= this.getY() && y <= this.getY() + this.getHeight();
    }

    @Override
    public int getWidth() {
        int width = 0;
        if (null != this.getText()) {
            width = this.getFontMetrics(this.getFont()).stringWidth(this.getText());
        }
        if (this.getIcon() != null && this.getIcon().getIconWidth() > width) {
            width = this.getIcon().getIconWidth();
        }
        return width += 21;
    }

    @Override
    public int getHeight() {
        int height = this.getFontMetrics(this.getFont()).getHeight();
        if (this.getIcon() != null) {
            height += this.getIcon().getIconHeight();
        }
        return height += 5;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.getWidth(), this.getHeight());
    }

    public void setTextAndUpdateLocation(String text) {
        int icoW;
        int n = icoW = this.getIcon() != null ? this.getIcon().getIconWidth() : 0;
        if (icoW % 2 == 1) {
            --icoW;
        }
        int dW = (this.getWidth() - icoW) / 2;
        super.setText(text);
        int dW2 = (this.getWidth() - icoW) / 2;
        this.setBounds(this.getX() - dW2 + dW, this.getY(), this.getWidth(), this.getHeight());
    }

    public void initTextAndUpdateLocation(String text) {
        this.setTextAndUpdateLocation(text);
        this.setLocation(this.getX(), this.getY() + 10);
    }

    /** True for devices on the network area (not for the items of the component bar). */
    private boolean isNode() {
        return getClientProperty(SIDEBAR_ITEM) == null && getText() != null && !getText().isEmpty();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g.create());
        try {
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hover = Enhancer.isHovered(this) && getClientProperty(SIDEBAR_ITEM) == null;
            if (this.selektiert) {
                g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.10));
                g2.fill(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 16, 16));
                g2.setStroke(new BasicStroke(1.6f));
                g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.85));
                g2.draw(new RoundRectangle2D.Double(1, 1, w - 2.6, h - 2.6, 16, 16));
            } else if (hover) {
                g2.setColor(Theme.alpha(Theme.INK, 0.055));
                g2.fill(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 16, 16));
            }
            if (isNode()) {
                paintNameTag(g2);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
        if (this.modemVerbunden) {
            Graphics2D g2d = Theme.smooth(g.create());
            int cx = this.getWidth() / 2;
            int cy = this.getHeight() / 2;
            g2d.setColor(Theme.alpha(Theme.LED_GREEN_BRIGHT, 0.25));
            g2d.fillOval(cx - 12, cy - 12, 24, 24);
            g2d.setColor(Theme.alpha(Theme.LED_GREEN_BRIGHT, 0.45));
            g2d.fillOval(cx - 8, cy - 8, 16, 16);
            g2d.setColor(Theme.LED_GREEN_BRIGHT);
            g2d.fillOval(cx - 5, cy - 5, 10, 10);
            g2d.setColor(new Color(255, 255, 255, 170));
            g2d.fillOval(cx - 3, cy - 4, 4, 3);
            g2d.dispose();
        }
    }

    /** White tag behind the device name, so names stay readable on top of cables. */
    private void paintNameTag(Graphics2D g2) {
        Rectangle viewR = new Rectangle();
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();
        java.awt.Insets in = getInsets();
        viewR.x = in.left;
        viewR.y = in.top;
        viewR.width = getWidth() - (in.left + in.right);
        viewR.height = getHeight() - (in.top + in.bottom);
        FontMetrics fm = getFontMetrics(getFont());
        SwingUtilities.layoutCompoundLabel(this, fm, getText(), getIcon(), getVerticalAlignment(), getHorizontalAlignment(),
                getVerticalTextPosition(), getHorizontalTextPosition(), viewR, iconR, textR, getIconTextGap());
        double x = Math.max(0.5, textR.x - 6);
        double tw = Math.min(getWidth() - 1 - x, textR.width + 12);
        double y = textR.y - 1;
        double th = textR.height + 2;
        g2.setColor(filius.modern.Theme.alpha(Theme.SURFACE, 235 / 255.0));
        g2.fill(new RoundRectangle2D.Double(x, y, tw, th, th, th));
        g2.setColor(this.selektiert ? Theme.alpha(Theme.PATCH_BLUE, 0.55) : Theme.LINE);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, tw - 1, th - 1, th, th));
    }

    @Override
    public void update(Observable o, Object arg) {
        LOG.trace("(JSidebarButton), update(" + String.valueOf(o) + "," + String.valueOf(arg) + ")");
        if (arg != null && arg.equals(Boolean.TRUE)) {
            this.modemVerbunden = true;
        } else if (arg != null && arg.equals(Boolean.FALSE)) {
            this.modemVerbunden = false;
        } else if (arg != null && arg instanceof String) {
            meldung((String)arg);
        } else if (arg != null && arg instanceof Host) {
            this.setText(((Host)arg).holeAnzeigeName());
        }
        if (arg != null && arg instanceof InternetKnoten) {
            this.updateTooltip((InternetKnoten)arg);
        }
        this.updateUI();
    }

    /** messages that are on screen right now */
    private static final java.util.Set<String> OFFENE_MELDUNGEN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Shows a message of a simulated device, e.g. a router without a route for a destination. Filius 2.9.3
     * showed it on the thread of the device itself, so the router forwarded nothing at all until the message
     * was closed. Now the message appears on the event thread and the device keeps working; the same message
     * is shown only once at a time.
     */
    static void meldung(String text) {
        if (!OFFENE_MELDUNGEN.add(text)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), text);
            } finally {
                OFFENE_MELDUNGEN.remove(text);
            }
        });
    }

    void updateTooltip(InternetKnoten knoten) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><pre>");
        tooltip.append(messages.getString("jsidebar_tooltip_ipAddress")).append(":");
        String gateway = null;
        String dns = null;
        for (NetzwerkInterface nic : knoten.getNetzwerkInterfaces()) {
            tooltip.append("\n ").append(nic.getIp()).append(" / ").append(nic.getSubnetzMaske());
            tooltip.append(" (").append(nic.getMac()).append(")");
            gateway = nic.getGateway();
            dns = nic.getDns();
        }
        tooltip.append("\n").append(messages.getString("jsidebar_tooltip_gateway")).append(": ").append(gateway);
        if (knoten instanceof Host) {
            tooltip.append("\n").append(messages.getString("jsidebar_tooltip_dnsServer")).append(": ").append(dns);
        }
        tooltip.append("</pre></html>");
        this.setToolTipText(tooltip.toString());
    }

    public void setTemporaryText(String text) {
        if (!this.isTemporaryText) {
            this.originalText = this.getText();
            this.originalTextColor = this.getForeground();
            this.isTemporaryText = true;
        }
        this.setForeground(Color.GRAY);
        this.setTextAndUpdateLocation(text);
    }

    public void resetText() {
        this.setForeground(this.originalTextColor);
        this.setTextAndUpdateLocation(this.originalText);
        this.isTemporaryText = false;
    }
}
