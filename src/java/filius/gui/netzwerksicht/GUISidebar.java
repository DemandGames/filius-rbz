/*
 * Filius (RBZ Wirtschaft Kiel Version): based on GUISidebar of Filius 2.9.3 (GPL). The component bar gets a light
 * surface, a heading, a little more air between the items and a hover highlight. Item lookup
 * (findButtonAt) and sizing work as before.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui.netzwerksicht;

import filius.gui.JBackgroundPanel;
import filius.modern.Enhancer;
import filius.modern.Texts;
import filius.modern.Theme;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;

public abstract class GUISidebar {
    protected JBackgroundPanel leistenpanel;
    protected List<JSidebarButton> buttonList = new LinkedList<JSidebarButton>();
    private static int width = 0;

    protected GUISidebar() {
        this.leistenpanel = new JBackgroundPanel(){
            private static final long serialVersionUID = 1L;
            private boolean adding;

            @Override
            public Dimension getPreferredSize() {
                int height = 0;
                for (Component component : this.getComponents()) {
                    if (component instanceof Box.Filler) {
                        height += component.getPreferredSize().height;
                        continue;
                    }
                    int cw = component.getPreferredSize().width;
                    if (cw > width) {
                        width = cw;
                    }
                    height += Math.max(component.getHeight(), component.getPreferredSize().height);
                }
                return new Dimension(Math.max(width, 112) + 16, height += 30);
            }

            @Override
            protected void addImpl(Component comp, Object constraints, int index) {
                if (!this.adding && this.getComponentCount() > 0 && !(comp instanceof Box.Filler)) {
                    this.adding = true;
                    super.addImpl(Box.createVerticalStrut(2), null, -1);
                    this.adding = false;
                }
                if (comp instanceof JSidebarButton) {
                    ((JComponent)comp).putClientProperty(JSidebarButton.SIDEBAR_ITEM, Boolean.TRUE);
                } else if (comp instanceof JLabel && ((JLabel)comp).getIcon() != null) {
                    ((JLabel)comp).putClientProperty(JSidebarButton.SIDEBAR_ITEM, Boolean.TRUE);
                    ((JLabel)comp).setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
                }
                super.addImpl(comp, constraints, index);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.smooth(g.create());
                try {
                    g2.setColor(Theme.SURFACE);
                    g2.fillRect(0, 0, this.getWidth(), this.getHeight());
                    JComponent hovered = Enhancer.hovered();
                    if (hovered != null && hovered.getParent() == this) {
                        int y = hovered.getY();
                        int h = hovered.getHeight();
                        g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.08));
                        g2.fill(new RoundRectangle2D.Double(8, y, this.getWidth() - 16, h, 14, 14));
                    }
                    g2.setColor(Theme.LINE);
                    g2.fillRect(this.getWidth() - 1, 0, 1, this.getHeight());
                } finally {
                    g2.dispose();
                }
            }
        };
        BoxLayout layout = new BoxLayout(this.leistenpanel, 1);
        this.leistenpanel.setLayout(layout);
        this.leistenpanel.setEnabled(false);
        this.leistenpanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 1));
        if (this instanceof GUIDesignSidebar) {
            // heading of the component bar: bold, with a short accent line in the colour of the design mode
            JLabel heading = new JLabel(Texts.get("sidebar.title")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = Theme.smooth(g.create());
                    try {
                        int w = 26;
                        int y = this.getHeight() - 8;
                        g2.setColor(Theme.PATCH_BLUE);
                        g2.fill(new RoundRectangle2D.Double((this.getWidth() - w) / 2.0, y, w, 3.5, 3.5, 3.5));
                    } finally {
                        g2.dispose();
                    }
                }
            };
            heading.setFont(Theme.font(Font.BOLD, 16f));
            heading.setForeground(Theme.INK);
            heading.setAlignmentX(0.5f);
            heading.setBorder(BorderFactory.createEmptyBorder(12, 0, 14, 0));
            this.leistenpanel.add(heading);
        }
        this.addItemsToSidebar();
    }

    protected abstract void addItemsToSidebar();

    public JSidebarButton findButtonAt(int x, int y) {
        JSidebarButton klickLabel = null;
        for (JSidebarButton tmpLbl : this.buttonList) {
            if (x < tmpLbl.getX() || y < tmpLbl.getY() || x > tmpLbl.getX() + tmpLbl.getWidth() || y > tmpLbl.getY() + tmpLbl.getHeight()) continue;
            klickLabel = tmpLbl;
        }
        return klickLabel;
    }

    public JBackgroundPanel getLeistenpanel() {
        return this.leistenpanel;
    }
}
