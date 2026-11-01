/*
 * Filius (RBZ Wirtschaft Kiel Version): based on JMarkerPanel of Filius 2.9.3 (GPL). Same behaviour; the selection
 * frame is painted as a rounded, translucent patch-blue area instead of a tiled bitmap.
 * Edited by Thorben Demand @ RBZ Wirtschaft Kiel, 2026.
 */
package filius.gui;

import filius.modern.Theme;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

public class JMarkerPanel
extends JBackgroundPanel {
    private static final long serialVersionUID = 1L;

    public boolean inBounds(int x, int y) {
        return x >= this.getX() && x <= this.getX() + this.getWidth() && y >= this.getY() && y <= this.getY() + this.getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = this.getWidth();
        int h = this.getHeight();
        if (w < 2 || h < 2) {
            return;
        }
        Graphics2D g2 = Theme.smooth(g.create());
        try {
            RoundRectangle2D.Double r = new RoundRectangle2D.Double(0.75, 0.75, w - 1.5, h - 1.5, 12, 12);
            g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.08));
            g2.fill(r);
            g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.75));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 6f, 4f }, 0f));
            g2.draw(r);
        } finally {
            g2.dispose();
        }
    }
}
