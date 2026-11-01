package filius.modern;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import filius.gui.GUIContainer;

/**
 * Transparent layer above the main window. It shows the empty-canvas hint, short toast messages
 * and the small confetti burst when a mission is completed. It never receives mouse events
 * ({@link #contains(int, int)} is always false), so Filius works underneath exactly as before.
 */
public class Overlay extends JComponent {

    private static final long serialVersionUID = 1L;
    private static Overlay instance;

    private final List<Toast> toasts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Timer timer;
    private final Random random = new Random();

    private float hintAlpha = 0f;
    private boolean hintWanted = false;

    private static final Color[] CONFETTI = { Theme.PATCH_BLUE, Theme.LED_GREEN, Theme.MARKER_AMBER,
            Theme.SIGNAL_RED, new Color(0x7A5AF0), new Color(0x1FB5C9) };

    public static synchronized Overlay install(JRootPane root) {
        if (instance == null) {
            instance = new Overlay(root);
        }
        return instance;
    }

    public static Overlay get() {
        return instance;
    }

    private Overlay(JRootPane root) {
        setOpaque(false);
        setFocusable(false);
        JLayeredPane layers = root.getLayeredPane();
        layers.add(this, Integer.valueOf(JLayeredPane.POPUP_LAYER - 1));
        setBounds(0, 0, layers.getWidth(), layers.getHeight());
        layers.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setBounds(0, 0, layers.getWidth(), layers.getHeight());
            }
        });
        timer = new Timer(16, e -> tick());
        timer.setCoalesce(true);
    }

    @Override
    public boolean contains(int x, int y) {
        return false; // fully transparent for the mouse
    }

    // ------------------------------------------------------------------ API

    public void toast(String text, Color accent) {
        if (text == null || text.isEmpty()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            // the same message should not stack up
            toasts.removeIf(t -> t.text.equals(text));
            toasts.add(new Toast(text, accent, System.currentTimeMillis()));
            while (toasts.size() > 3) {
                toasts.remove(0);
            }
            startTimer();
        });
    }

    /** Small fountain of confetti above the toast area – used when a mission is completed. */
    public void celebrate() {
        if (!Prefs.effectsEnabled()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Rectangle area = canvasBounds();
            if (area == null) {
                area = new Rectangle(0, 0, getWidth(), getHeight());
            }
            spawn(area.x + area.width / 2.0, area.y + area.height - 74, 90);
            startTimer();
        });
    }

    /** size of the confetti cloud: all distances 40 % smaller than at first, same number of pieces */
    private static final double SPREAD = 0.6;

    private void spawn(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(55 + random.nextDouble() * 70); // upwards, fanned out
            double speed = (6 + random.nextDouble() * 7) * SPREAD;
            Particle q = new Particle();
            q.x = x + (random.nextDouble() - 0.5) * 80 * SPREAD;
            q.y = y;
            q.vx = Math.cos(angle) * speed;
            q.vy = -Math.sin(angle) * speed;
            q.rot = random.nextDouble() * Math.PI;
            q.vrot = (random.nextDouble() - 0.5) * 0.3;
            q.phase = random.nextDouble() * Math.PI * 2;
            q.w = 6 + random.nextInt(5);
            q.h = 3 + random.nextInt(4);
            q.color = CONFETTI[random.nextInt(CONFETTI.length)];
            q.life = 1.0f;
            particles.add(q);
        }
    }

    public void setHintWanted(boolean wanted) {
        if (wanted != hintWanted) {
            hintWanted = wanted;
            startTimer();
        }
    }

    // ------------------------------------------------------------------ animation

    private void startTimer() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean busy = false;

        float target = hintWanted ? 1f : 0f;
        if (Math.abs(hintAlpha - target) > 0.01f) {
            hintAlpha += (target - hintAlpha) * (Prefs.effectsEnabled() ? 0.18f : 1f);
            busy = true;
        } else {
            hintAlpha = target;
        }

        for (Iterator<Toast> it = toasts.iterator(); it.hasNext();) {
            Toast t = it.next();
            if (now - t.start > t.duration) {
                it.remove();
            }
        }
        busy |= !toasts.isEmpty();

        for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            Particle q = it.next();
            q.vy = Math.min(q.vy + 0.17 * SPREAD, 2.6 * SPREAD); // paper flutters down slowly
            q.vx *= 0.975;
            q.phase += 0.12;
            q.x += q.vx + Math.sin(q.phase) * 0.6 * SPREAD;
            q.y += q.vy;
            q.rot += q.vrot;
            q.life -= 0.0075f;
            if (q.life <= 0 || q.y > getHeight() + 20) {
                it.remove();
            }
        }
        busy |= !particles.isEmpty();

        repaint();
        if (!busy) {
            timer.stop();
        }
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g.create());
        try {
            Rectangle canvas = canvasBounds();
            if (hintAlpha > 0.01f && canvas != null) {
                paintHint(g2, canvas, hintAlpha);
            }
            if (!toasts.isEmpty()) {
                paintToasts(g2, canvas != null ? canvas : new Rectangle(0, 0, getWidth(), getHeight()));
            }
            for (Particle q : particles) {
                AffineTransform old = g2.getTransform();
                g2.translate(q.x, q.y);
                g2.rotate(q.rot);
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, q.life * 1.4f))));
                g2.setColor(q.color);
                g2.fill(new RoundRectangle2D.Double(-q.w / 2.0, -q.h / 2.0, q.w, q.h, 2, 2));
                g2.setTransform(old);
            }
        } finally {
            g2.dispose();
        }
    }

    /** Visible part of the network area (design or simulation view) in overlay coordinates. */
    private Rectangle canvasBounds() {
        try {
            GUIContainer c = GUIContainer.getGUIContainer();
            JScrollPane view = c.getActiveSite() == Theme.MODE_SIMULATION ? (JScrollPane) c.getSimpanel() : c.getScrollPane();
            if (view == null || !view.isShowing()) {
                return null;
            }
            Component vp = view.getViewport();
            return SwingUtilities.convertRectangle(vp.getParent(), vp.getBounds(), this);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Welcome card on the empty design area: what Filius is and that the new network takes shape here. Deliberately
     * no instructions - those come from the learning path.
     */
    private void paintHint(Graphics2D g2, Rectangle canvas, float alpha) {
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        String title = Texts.get("empty.title");
        String body = Texts.get("empty.body");
        String body2 = Texts.get("empty.body2");

        Font fTitle = Theme.font(Font.BOLD, 22f);
        Font fBody = Theme.font(Font.PLAIN, 14f);
        Font fLead = Theme.font(Font.BOLD, 14f);
        FontMetrics mt = g2.getFontMetrics(fTitle);
        FontMetrics mb = g2.getFontMetrics(fBody);
        FontMetrics ml = g2.getFontMetrics(fLead);
        int pad = 32;
        int w = Math.min(Math.max(mt.stringWidth(title) + 2 * pad, 560), canvas.width - 40);
        if (w < 200) {
            g2.setComposite(AlphaComposite.SrcOver);
            return;
        }
        List<String> lines = umbrechen(body, mb, w - 2 * pad);
        List<String> lines2 = umbrechen(body2, ml, w - 2 * pad);
        int lh = mb.getHeight() + 3;
        int top = 122;
        int h = top + 14 + lines.size() * lh + 10 + lines2.size() * lh + 26;
        int x = canvas.x + (canvas.width - w) / 2;
        int y = canvas.y + Math.max(24, (canvas.height - h) / 2 - 20);

        // soft card with dashed outline like a free slot on a patch panel
        g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.06));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 22, 22));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] { 7f, 6f }, 0f));
        g2.setColor(Theme.alpha(Theme.PATCH_BLUE, 0.45));
        g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 22, 22));

        // the logo of Filius (RBZ Wirtschaft Kiel Version): the networked water tower
        javax.swing.ImageIcon logo = willkommenLogo();
        if (logo != null) {
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            logo.paintIcon(this, g2, x + (w - LOGO) / 2, y + 18);
        }

        g2.setColor(Theme.INK);
        g2.setFont(fTitle);
        g2.drawString(title, x + (w - mt.stringWidth(title)) / 2, y + top);
        int ty = y + top + 14 + mb.getAscent();
        g2.setColor(Theme.INK_2);
        g2.setFont(fBody);
        for (String l : lines) {
            g2.drawString(l, x + (w - mb.stringWidth(l)) / 2, ty);
            ty += lh;
        }
        ty += 10;
        g2.setColor(Theme.PATCH_BLUE);
        g2.setFont(fLead);
        for (String l : lines2) {
            g2.drawString(l, x + (w - ml.stringWidth(l)) / 2, ty);
            ty += lh;
        }
        g2.setComposite(AlphaComposite.SrcOver);
    }

    /** Word wrap for the welcome card. */
    private static List<String> umbrechen(String text, FontMetrics fm, int breite) {
        List<String> out = new ArrayList<String>();
        StringBuilder zeile = new StringBuilder();
        for (String wort : text.split("\\s+")) {
            String probe = zeile.length() == 0 ? wort : zeile + " " + wort;
            if (zeile.length() > 0 && fm.stringWidth(probe) > breite) {
                out.add(zeile.toString());
                zeile = new StringBuilder(wort);
            } else {
                zeile = new StringBuilder(probe);
            }
        }
        if (zeile.length() > 0) {
            out.add(zeile.toString());
        }
        return out;
    }

    private static final Color TOAST = Theme.farbe(0x1C2B3A, 0x263445);

    /** size of the logo on the welcome card (logical pixels) */
    private static final int LOGO = 72;
    private static javax.swing.ImageIcon willkommenLogo;

    private static javax.swing.ImageIcon willkommenLogo() {
        if (willkommenLogo == null) {
            try {
                willkommenLogo = HiDpiIcons.load("/gfx/modern/logo-willkommen.png");
            } catch (RuntimeException e) {
                return null;
            }
        }
        return willkommenLogo;
    }

    private void paintToasts(Graphics2D g2, Rectangle area) {
        long now = System.currentTimeMillis();
        Font f = Theme.font(Font.BOLD, 14f);
        FontMetrics fm = g2.getFontMetrics(f);
        int bottom = area.y + area.height - 22;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            long age = now - t.start;
            float in = Math.min(1f, age / 220f);
            float out = Math.min(1f, Math.max(0f, (t.duration - age) / 320f));
            float a = Math.min(in, out);
            float ease = 1f - (1f - in) * (1f - in);
            int textW = Math.min(fm.stringWidth(t.text), area.width - 120);
            int w = textW + 58;
            int h = 42;
            int x = area.x + (area.width - w) / 2;
            int y = bottom - h + Math.round((1f - ease) * 16f);

            g2.setComposite(AlphaComposite.SrcOver.derive(a));
            // shadow
            for (int s = 0; s < 4; s++) {
                g2.setColor(new Color(16, 28, 40, 14 - s * 3));
                g2.fill(new RoundRectangle2D.Double(x - s, y - s + 3, w + 2 * s, h + 2 * s, h + 2 * s, h + 2 * s));
            }
            g2.setColor(TOAST);
            g2.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
            if (Theme.dunkel()) {
                g2.setColor(new Color(0x3A4C60));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, h - 1, h - 1));
            }
            // LED
            g2.setColor(Theme.alpha(t.accent, 0.35));
            g2.fillOval(x + 14, y + h / 2 - 8, 16, 16);
            g2.setColor(t.accent);
            g2.fillOval(x + 18, y + h / 2 - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(f);
            String text = t.text;
            while (fm.stringWidth(text) > textW && text.length() > 4) {
                text = text.substring(0, text.length() - 2);
            }
            if (!text.equals(t.text)) {
                text = text + "…";
            }
            g2.drawString(text, x + 40, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            bottom = y - 10;
        }
        g2.setComposite(AlphaComposite.SrcOver);
    }

    // ------------------------------------------------------------------ model

    private static final class Toast {
        final String text;
        final Color accent;
        final long start;
        final long duration;

        Toast(String text, Color accent, long start) {
            this.text = text;
            this.accent = accent == null ? Theme.PATCH_BLUE : accent;
            this.start = start;
            this.duration = 2600 + Math.min(2400, text.length() * 30L);
        }
    }

    private static final class Particle {
        double x, y, vx, vy, rot, vrot, phase;
        int w, h;
        Color color;
        float life;
    }
}
