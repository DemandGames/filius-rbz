package filius.modern;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import filius.gui.JBackgroundPanel;

/**
 * The header: brand, file actions, the mode switch, simulation speed, the learning path and tools.
 * The buttons themselves are the original buttons of Filius' main menu, only arranged and styled
 * differently, so shortcuts and actions keep working.
 */
public class HeaderBar extends JBackgroundPanel {

    private static final long serialVersionUID = 1L;
    private static final int HEIGHT = 60;
    private static final int STRIP = 3;

    private final JLabel brand = new JLabel();
    /** name of this version under the brand */
    private final JLabel brandSub = new JLabel("RBZ Wirtschaft Kiel Version");
    private final ModeSwitch modeSwitch = new ModeSwitch();
    private final JLabel speedTitle = new JLabel();
    /** prominent button next to the mode switch: start, continue or hide the learning path */
    private final LernpfadKnopf missionsButton = new LernpfadKnopf();

    private AbstractButton[] fileButtons = new AbstractButton[0];
    private AbstractButton[] toolButtons = new AbstractButton[0];
    private JSlider slider;
    private JLabel speedValue;

    private Color stripColor = Theme.PATCH_BLUE;
    private Color stripTarget = Theme.PATCH_BLUE;
    private final Timer stripTimer;

    public HeaderBar() {
        setLayout(null);
        setOpaque(true);
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(100, HEIGHT + STRIP));

        brand.setText("Filius");
        brand.setIcon(HiDpiIcons.load("/gfx/modern/brand.png"));
        brand.setIconTextGap(9);
        brand.setFont(Theme.font(Font.BOLD, 19f));
        brand.setForeground(Theme.INK);
        brand.setToolTipText(Enhancer.PRODUCT);
        add(brand);
        brandSub.setFont(Theme.font(Font.BOLD, 10.5f));
        brandSub.setForeground(Theme.INK_3);
        add(brandSub);

        speedTitle.setText(Texts.get("speed"));
        speedTitle.setFont(Theme.font(Font.BOLD, 12f));
        speedTitle.setForeground(Theme.INK_3);
        add(speedTitle);

        add(modeSwitch);

        missionsButton.addActionListener(e -> filius.modern.lernpfad.Lernpfad.umschalten());
        updateMissions();
        filius.modern.lernpfad.Lernpfad.addListener(this::updateMissions);
        if (Prefs.missionsEnabled()) {
            add(missionsButton);
        }

        stripTimer = new Timer(16, e -> {
            stripColor = Theme.mix(stripColor, stripTarget, Prefs.effectsEnabled() ? 0.16 : 1.0);
            if (distance(stripColor, stripTarget) < 3) {
                stripColor = stripTarget;
                ((Timer) e.getSource()).stop();
            }
            repaint(0, getHeight() - STRIP, getWidth(), STRIP);
        });
    }

    private static int distance(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) + Math.abs(a.getGreen() - b.getGreen()) + Math.abs(a.getBlue() - b.getBlue());
    }

    // ------------------------------------------------------------------ setup from GUIMainMenu

    /** New, Open, Save: icon with a label underneath, so the buttons explain themselves */
    public void setFileButtons(AbstractButton... buttons) {
        fileButtons = buttons;
        String[] labels = { "file.new", "file.open", "file.save" };
        for (int i = 0; i < buttons.length; i++) {
            AbstractButton b = buttons[i];
            toolbarStyle(b);
            if (i < labels.length) {
                b.setText(Texts.get(labels[i]));
            }
            b.setFont(Theme.font(Font.BOLD, 11f));
            b.setForeground(Theme.INK_2);
            b.setVerticalTextPosition(SwingConstants.BOTTOM);
            b.setHorizontalTextPosition(SwingConstants.CENTER);
            b.setIconTextGap(1);
            b.setMargin(new Insets(3, 5, 3, 5));
            add(b);
        }
    }

    public void setModeButtons(JButton design, JButton simulation, JButton documentation) {
        modeSwitch.setButtons(design, simulation, documentation);
    }

    public void setSpeed(JSlider slider, JLabel value) {
        this.slider = slider;
        this.speedValue = value;
        slider.setOpaque(false);
        slider.setFocusable(false);
        value.setFont(Theme.font(Font.BOLD, 13f));
        value.setForeground(Theme.INK);
        value.setHorizontalAlignment(SwingConstants.LEFT);
        add(slider);
        add(value);
    }

    public void setToolButtons(AbstractButton... buttons) {
        toolButtons = buttons;
        for (AbstractButton b : buttons) {
            if (b != null) {
                toolbarStyle(b);
                add(b);
            }
        }
    }

    public static void toolbarStyle(AbstractButton b) {
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(6, 7, 6, 7));
        b.setForeground(Theme.INK);
    }

    public void setIcon(AbstractButton b, String name) {
        ImageIcon icon = HiDpiIcons.load("/gfx/modern/" + name + ".png");
        if (icon != null) {
            b.setIcon(icon);
        }
    }

    /** Called when the mode changes (1 design, 2 simulation, 3 documentation). */
    public void setMode(int mode) {
        modeSwitch.select(mode);
        Leinwand.farben(mode);
        stripTarget = Theme.modeColor(mode);
        if (!stripTimer.isRunning()) {
            stripTimer.start();
        }
    }

    private void updateMissions() {
        missionsButton.aktualisieren();
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------ layout

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = HEIGHT;
        int pad = 14;
        int gap = 2;
        int lpGap = 10;

        // decide how compact the header has to be: step by step, the least important text goes first
        boolean brandText = true;
        boolean speedLabel = true;
        boolean modeLabels = true;
        int lpStufe = 0;
        boolean lp = missionsButton.getParent() == this;
        for (int stufe = 0; stufe <= 5; stufe++) {
            brandText = stufe < 1;
            speedLabel = stufe < 2;
            lpStufe = stufe < 3 ? 0 : stufe < 5 ? 1 : 2;
            modeLabels = stufe < 4;
            int need = pad + brandWidth(brandText) + 18 + groupWidth(fileButtons, gap) + 24 + modeSwitch.preferredWidth(modeLabels)
                    + (lp ? lpGap + missionsButton.breite(lpStufe) : 0) + 24 + rightGroupWidth(speedLabel) + pad;
            if (need <= w) {
                break;
            }
        }

        // left: brand + file buttons
        int x = pad;
        brand.setText(brandText ? "Filius" : null);
        Dimension bd = brand.getPreferredSize();
        brandSub.setVisible(brandText);
        if (brandText) {
            Dimension sd = brandSub.getPreferredSize();
            int textX = x + (brand.getIcon() == null ? 0 : brand.getIcon().getIconWidth() + brand.getIconTextGap());
            java.awt.FontMetrics fm = brand.getFontMetrics(brand.getFont());
            int textH = fm.getAscent() + fm.getDescent();
            // the name sits at the top of the icon, the version line right below it
            brand.setVerticalTextPosition(SwingConstants.TOP);
            int block = Math.max(bd.height, textH - 1 + sd.height);
            int top = (h - block) / 2;
            brand.setBounds(x, top, bd.width, bd.height);
            brandSub.setBounds(textX, top + textH - 1, sd.width, sd.height);
            x += Math.max(bd.width, textX - x + sd.width) + 18;
        } else {
            brand.setBounds(x, (h - bd.height) / 2, bd.width, bd.height);
            x += bd.width + 18;
        }
        for (AbstractButton b : fileButtons) {
            Dimension d = fileButtonSize(b);
            b.setBounds(x, (h - d.height) / 2, d.width, d.height);
            x += d.width + gap;
        }
        int leftEnd = x;

        // right: speed + tools
        int rx = w - pad;
        for (int i = toolButtons.length - 1; i >= 0; i--) {
            AbstractButton b = toolButtons[i];
            if (b == null) {
                continue;
            }
            Dimension d = buttonSize(b);
            rx -= d.width;
            b.setBounds(rx, (h - d.height) / 2, d.width, d.height);
            rx -= gap;
        }
        rx -= 16;
        if (slider != null) {
            int vw = 40;
            rx -= vw;
            speedValue.setBounds(rx, (h - 20) / 2, vw, 20);
            int sw = 92;
            rx -= sw + 4;
            slider.setBounds(rx, (h - 28) / 2, sw, 28);
            speedTitle.setVisible(speedLabel);
            if (speedLabel) {
                Dimension d = speedTitle.getPreferredSize();
                rx -= d.width + 8;
                speedTitle.setBounds(rx, (h - d.height) / 2, d.width, d.height);
            }
        }
        int rightStart = rx;

        // centre: mode switch and the learning path button (centred in the window if there is room)
        modeSwitch.setShowLabels(modeLabels);
        missionsButton.setStufe(lpStufe);
        int mw = modeSwitch.preferredWidth(modeLabels);
        int lw = lp ? missionsButton.breite(lpStufe) : 0;
        int gw = mw + (lp ? lpGap + lw : 0);
        int mh = 40;
        int gx = (w - gw) / 2;
        if (gx < leftEnd + 20) {
            gx = leftEnd + 20;
        }
        if (gx + gw > rightStart - 20) {
            gx = Math.max(leftEnd + 12, rightStart - 20 - gw);
        }
        modeSwitch.setBounds(gx, (h - mh) / 2, mw, mh);
        if (lp) {
            missionsButton.setBounds(gx + mw + lpGap, (h - mh) / 2, lw, mh);
        }
    }

    private int brandWidth(boolean withText) {
        String old = brand.getText();
        brand.setText(withText ? "Filius" : null);
        int bw = brand.getPreferredSize().width;
        brand.setText(old);
        if (withText) {
            int iconW = brand.getIcon() == null ? 0 : brand.getIcon().getIconWidth() + brand.getIconTextGap();
            bw = Math.max(bw, iconW + brandSub.getPreferredSize().width);
        }
        return bw;
    }

    private int groupWidth(AbstractButton[] buttons, int gap) {
        int sum = 0;
        for (AbstractButton b : buttons) {
            if (b != null) {
                sum += (buttons == fileButtons ? fileButtonSize(b) : buttonSize(b)).width + gap;
            }
        }
        return sum;
    }

    private int rightGroupWidth(boolean speedLabel) {
        int sum = groupWidth(toolButtons, 2);
        sum += 16 + 40 + 96;
        if (speedLabel) {
            sum += speedTitle.getPreferredSize().width + 8;
        }
        return sum;
    }

    private static Dimension buttonSize(AbstractButton b) {
        Dimension d = b.getPreferredSize();
        return new Dimension(Math.max(36, d.width), 36);
    }

    private static Dimension fileButtonSize(AbstractButton b) {
        Dimension d = b.getPreferredSize();
        return new Dimension(Math.max(46, d.width), 50);
    }

    // ------------------------------------------------------------------ painting

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g.create());
        try {
            g2.setColor(Theme.SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            // separators between groups
            g2.setColor(Theme.LINE);
            if (fileButtons.length > 0) {
                int sx = fileButtons[0].getX() - 10;
                g2.fillRect(sx, 18, 1, HEIGHT - 36);
            }
            if (slider != null) {
                int sx = speedValue.getX() + speedValue.getWidth() + 6;
                g2.fillRect(sx, 18, 1, HEIGHT - 36);
            }
            // mode strip: the colour of the current mode, like a patch cable along the edge
            g2.setColor(Theme.LINE);
            g2.fillRect(0, getHeight() - STRIP - 1, getWidth(), 1);
            g2.setColor(stripColor);
            g2.fillRect(0, getHeight() - STRIP, getWidth(), STRIP);
        } finally {
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ mode switch

    /** Segmented control with a sliding indicator in the colour of the selected mode. */
    static final class ModeSwitch extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final String[] ICONS = { "mode-design", "mode-sim", "mode-doc" };
        private static final String[] LABELS = { "mode.design", "mode.simulation", "mode.documentation" };
        private static final int[] MODES = { Theme.MODE_DESIGN, Theme.MODE_SIMULATION, Theme.MODE_DOCUMENTATION };

        private final JButton[] buttons = new JButton[3];
        private final Icon[] iconOff = new Icon[3];
        private final Icon[] iconOn = new Icon[3];
        private int selected = 0;
        private boolean showLabels = true;
        private double indX = -1;
        private double indW = 0;
        private Color indColor = Theme.PATCH_BLUE;
        private final Timer anim;
        private final Timer pulse;

        ModeSwitch() {
            setLayout(null);
            setOpaque(false);
            anim = new Timer(16, e -> step());
            pulse = new Timer(60, e -> repaint());
        }

        void setButtons(JButton design, JButton simulation, JButton documentation) {
            JButton[] bs = { design, simulation, documentation };
            for (int i = 0; i < 3; i++) {
                JButton b = bs[i];
                buttons[i] = b;
                iconOff[i] = HiDpiIcons.load("/gfx/modern/" + ICONS[i] + ".png");
                iconOn[i] = HiDpiIcons.load("/gfx/modern/" + ICONS[i] + "-on.png");
                b.setText(Texts.get(LABELS[i]));
                b.setFont(Theme.font(Font.BOLD, 13f));
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                b.setFocusPainted(false);
                b.setFocusable(false);
                b.setOpaque(false);
                b.setRolloverEnabled(true);
                b.setIconTextGap(6);
                b.setMargin(new Insets(0, 10, 0, 12));
                b.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 12 + extra(i, true)));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.getModel().addChangeListener(e -> repaint());
                add(b);
            }
            select(Theme.MODE_DESIGN);
        }

        /** room for the live LED in the simulation segment */
        private static int extra(int index, boolean labels) {
            return index == 1 && labels ? 12 : 0;
        }

        void setShowLabels(boolean show) {
            if (show == showLabels) {
                return;
            }
            showLabels = show;
            for (int i = 0; i < 3; i++) {
                if (buttons[i] != null) {
                    buttons[i].setText(show ? Texts.get(LABELS[i]) : null);
                    buttons[i].setBorder(show ? BorderFactory.createEmptyBorder(0, 10, 0, 12 + extra(i, true)) : BorderFactory.createEmptyBorder(0, 10, 0, 10));
                }
            }
            indX = -1;
        }

        int preferredWidth(boolean labels) {
            int w = 8;
            for (int i = 0; i < 3; i++) {
                if (buttons[i] == null) {
                    continue;
                }
                if (labels) {
                    w += buttons[i].getFontMetrics(buttons[i].getFont()).stringWidth(Texts.get(LABELS[i])) + 6 + 18 + 22 + extra(i, true);
                } else {
                    w += 18 + 20;
                }
            }
            return w;
        }

        void select(int mode) {
            for (int i = 0; i < 3; i++) {
                if (MODES[i] == mode) {
                    selected = i;
                }
            }
            for (int i = 0; i < 3; i++) {
                if (buttons[i] == null) {
                    continue;
                }
                buttons[i].setIcon(i == selected ? iconOn[i] : iconOff[i]);
                buttons[i].setForeground(i == selected ? Color.WHITE : Theme.INK_2);
            }
            if (selected == 1 && Prefs.effectsEnabled()) {
                pulse.start();
            } else {
                pulse.stop();
            }
            if (!anim.isRunning()) {
                anim.start();
            }
        }

        private void step() {
            JButton target = buttons[selected];
            if (target == null) {
                anim.stop();
                return;
            }
            double tx = target.getX();
            double tw = target.getWidth();
            Color tc = Theme.modeColor(MODES[selected]);
            if (indX < 0 || !Prefs.effectsEnabled()) {
                indX = tx;
                indW = tw;
                indColor = tc;
            } else {
                indX += (tx - indX) * 0.25;
                indW += (tw - indW) * 0.25;
                indColor = Theme.mix(indColor, tc, 0.25);
            }
            if (Math.abs(tx - indX) < 0.5 && Math.abs(tw - indW) < 0.5) {
                indX = tx;
                indW = tw;
                indColor = tc;
                anim.stop();
            }
            repaint();
        }

        @Override
        public void doLayout() {
            int x = 4;
            int h = getHeight() - 8;
            for (int i = 0; i < 3; i++) {
                JButton b = buttons[i];
                if (b == null) {
                    continue;
                }
                int w;
                if (showLabels) {
                    w = b.getFontMetrics(b.getFont()).stringWidth(b.getText() == null ? "" : b.getText()) + 6 + 18 + 22 + extra(i, true);
                } else {
                    w = 38;
                }
                b.setBounds(x, 4, w, h);
                x += w;
            }
            indX = -1;
            if (!anim.isRunning()) {
                anim.start();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                int w = getWidth();
                int h = getHeight();
                g2.setColor(Theme.CONTROL);
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
                for (int i = 0; i < 3; i++) {
                    JButton b = buttons[i];
                    if (b != null && i != selected && b.getModel().isRollover()) {
                        g2.setColor(Theme.CONTROL_HOVER);
                        g2.fill(new RoundRectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getHeight(), b.getHeight()));
                    }
                }
                if (indX >= 0) {
                    double ih = h - 8;
                    for (int s = 3; s > 0; s--) {
                        g2.setColor(Theme.alpha(indColor, 0.06 * s));
                        g2.fill(new RoundRectangle2D.Double(indX - s + 0.5, 4 - s + 2, indW + 2 * s - 1, ih + 2 * s - 1, ih + 2 * s, ih + 2 * s));
                    }
                    g2.setColor(indColor);
                    g2.fill(new RoundRectangle2D.Double(indX, 4, indW, ih, ih, ih));
                    // live LED while the simulation is running
                    if (selected == 1 && showLabels && Math.abs(indX - buttons[1].getX()) < 1) {
                        double t = (System.currentTimeMillis() % 1600L) / 1600.0;
                        double a = 0.55 + 0.45 * Math.sin(t * Math.PI * 2);
                        double cx = indX + indW - 14;
                        double cy = 4 + ih / 2;
                        g2.setColor(new Color(255, 255, 255, (int) (60 * a)));
                        g2.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
                        g2.setColor(new Color(255, 255, 255, (int) (140 + 115 * a)));
                        g2.fill(new Ellipse2D.Double(cx - 2.5, cy - 2.5, 5, 5));
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------ learning path button

    /** Colour of the learning path: its own colour, so it does not look like one of the modes. */
    public static final Color LERNPFAD = new Color(0x6A4FD6);
    private static final Color LERNPFAD_DUNKEL = new Color(0x5638C4);
    private static final Color LERNPFAD_HELL = Theme.farbe(0xEFEBFC, 0x2B2450);
    private static final Color LERNPFAD_HELL_2 = Theme.farbe(0xE2DBFA, 0x362C66);
    /** text of the open (light) button */
    private static final Color LERNPFAD_TEXT = Theme.farbe(0x5638C4, 0xC9BCFF);

    /**
     * Filled pill button: "Start learning path", "Continue learning path" when a chapter has been
     * started, "Hide learning path" while the panel is open. The ring shows the chapters completed.
     */
    static final class LernpfadKnopf extends JButton {
        private static final long serialVersionUID = 1L;
        private int stufe;

        LernpfadKnopf() {
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(Font.BOLD, 13.5f));
            setIcon(new ProgressRing(this));
            setIconTextGap(8);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 18));
            getModel().addChangeListener(e -> repaint());
            aktualisieren();
        }

        static boolean offen() {
            return filius.modern.lernpfad.Lernpfad.sichtbar();
        }

        private static String text(int stufe, boolean offen, boolean begonnen) {
            if (stufe >= 2) {
                return null;
            }
            if (stufe == 1) {
                return Texts.get("lernpfad");
            }
            return Texts.get(offen ? "lernpfad.hide" : begonnen ? "lernpfad.continue" : "lernpfad.start");
        }

        void setStufe(int stufe) {
            if (this.stufe != stufe) {
                this.stufe = stufe;
                aktualisieren();
            }
        }

        void aktualisieren() {
            boolean offen = offen();
            setText(text(stufe, offen, filius.modern.lernpfad.Lernpfad.begonnen()));
            setForeground(offen ? LERNPFAD_TEXT : Color.WHITE);
            setToolTipText(Texts.get("lernpfad.tooltip") + " · " + Texts.format("lernpfad.progress",
                    filius.modern.lernpfad.Lernpfad.fortschritt().fertigeKapitel(), filius.modern.lernpfad.Lernpfad.fortschritt().kapitelAnzahl()));
            repaint();
        }

        /** the same width in all states, so the header does not jump when the text changes */
        int breite(int stufe) {
            java.awt.FontMetrics fm = getFontMetrics(getFont());
            int text = 0;
            for (int i = 0; i < 3; i++) {
                String t = text(stufe, i == 0, i == 1);
                if (t != null) {
                    text = Math.max(text, fm.stringWidth(t));
                }
            }
            return 16 + 18 + (text > 0 ? getIconTextGap() + text + 18 : 10);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                int w = getWidth();
                int h = getHeight();
                boolean offen = offen();
                boolean hover = getModel().isRollover();
                boolean druck = getModel().isPressed();
                Color c;
                if (offen) {
                    c = druck || hover ? LERNPFAD_HELL_2 : LERNPFAD_HELL;
                } else {
                    c = druck ? LERNPFAD_DUNKEL.darker() : hover ? LERNPFAD_DUNKEL : LERNPFAD;
                    // soft glow so the button stands out
                    for (int s = 3; s > 0; s--) {
                        g2.setColor(Theme.alpha(LERNPFAD, 0.07 * s));
                        g2.fill(new RoundRectangle2D.Double(-s + 0.5 + 3, -s + 2.5, w + 2 * s - 7, h + 2 * s - 5, h, h));
                    }
                }
                g2.setColor(c);
                g2.fill(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h - 1, h - 1));
                if (offen) {
                    g2.setColor(Theme.alpha(LERNPFAD, 0.35));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1, h - 1, h - 1, h - 1));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /** Small ring that fills up with every chapter of the learning path completed. */
    static final class ProgressRing implements Icon {
        private final LernpfadKnopf knopf;

        ProgressRing(LernpfadKnopf knopf) {
            this.knopf = knopf;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                boolean offen = LernpfadKnopf.offen();
                Color voll = offen ? LERNPFAD : Color.WHITE;
                Color spur = offen ? Theme.alpha(LERNPFAD, 0.22) : Theme.alpha(Color.WHITE, 0.32);
                double done = filius.modern.lernpfad.Lernpfad.fortschritt().fertigeKapitel();
                double total = filius.modern.lernpfad.Lernpfad.fortschritt().kapitelAnzahl();
                double s = 18;
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(spur);
                g2.draw(new Ellipse2D.Double(x + 1.5, y + 1.5, s - 3, s - 3));
                g2.setColor(voll);
                if (done > 0) {
                    g2.draw(new Arc2D.Double(x + 1.5, y + 1.5, s - 3, s - 3, 90, -360 * done / total, Arc2D.OPEN));
                }
                if (done >= total) {
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D check = new Path2D.Double();
                    check.moveTo(x + 5.5, y + 9.2);
                    check.lineTo(x + 8, y + 11.8);
                    check.lineTo(x + 12.6, y + 6.6);
                    g2.draw(check);
                } else if (done == 0) {
                    // a small play triangle: here it starts
                    Path2D play = new Path2D.Double();
                    play.moveTo(x + 7.2, y + 5.6);
                    play.lineTo(x + 12.6, y + 9);
                    play.lineTo(x + 7.2, y + 12.4);
                    play.closePath();
                    g2.fill(play);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }

    // ------------------------------------------------------------------ language button

    /**
     * Language switch: a small toggle with "DE" on the left and "EN" on the right; the knob sits on
     * the language Filius runs in. A click slides the knob over and asks whether Filius should restart
     * in the other language; if not, the knob slides back.
     */
    public static final class SprachKnopf extends JButton {
        private static final long serialVersionUID = 1L;
        private static final int SPUR_B = 74;
        private static final int SPUR_H = 26;
        private static final int KNOPF_B = 36;
        private final boolean englisch = Texts.englisch();
        /** position of the knob: 0 = DE (left), 1 = EN (right) */
        private double pos;
        private double ziel;
        private Runnable danach;
        private final Timer anim;

        public SprachKnopf() {
            pos = englisch ? 1 : 0;
            ziel = pos;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(Font.BOLD, 11.5f));
            setToolTipText(Texts.get("lang.tooltip"));
            getAccessibleContext().setAccessibleName("DE / EN");
            getModel().addChangeListener(e -> repaint());
            anim = new Timer(15, e -> {
                pos += (ziel - pos) * 0.32;
                if (Math.abs(ziel - pos) < 0.01) {
                    pos = ziel;
                    ((Timer) e.getSource()).stop();
                    Runnable r = danach;
                    danach = null;
                    if (r != null) {
                        r.run();
                    }
                }
                repaint();
            });
        }

        /** Slides the knob to the other language and asks; if Filius does not restart, it slides back. */
        public void umschalten() {
            if (anim.isRunning()) {
                return;
            }
            bewegen(englisch ? 0 : 1, () -> javax.swing.SwingUtilities.invokeLater(() -> {
                Sprache.umschalten();
                // still running: the student cancelled
                bewegen(englisch ? 1 : 0, null);
            }));
        }

        private void bewegen(double nach, Runnable dann) {
            ziel = nach;
            danach = dann;
            anim.start();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SPUR_B + 8, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double x = (getWidth() - SPUR_B) / 2.0;
                double y = (getHeight() - SPUR_H) / 2.0;
                boolean hover = getModel().isRollover();
                RoundRectangle2D spur = new RoundRectangle2D.Double(x + 0.5, y + 0.5, SPUR_B - 1, SPUR_H - 1, SPUR_H, SPUR_H);
                g2.setColor(hover ? Theme.CONTROL_HOVER : Theme.CONTROL);
                g2.fill(spur);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(hover ? Theme.LINE_STRONG : Theme.LINE);
                g2.draw(spur);
                // knob
                double kx = x + 3 + pos * (SPUR_B - 6 - KNOPF_B);
                double kh = SPUR_H - 6;
                g2.setColor(Theme.alpha(Theme.INK, 0.18));
                g2.fill(new RoundRectangle2D.Double(kx, y + 4, KNOPF_B, kh, kh, kh));
                g2.setColor(Theme.INK);
                g2.fill(new RoundRectangle2D.Double(kx, y + 3, KNOPF_B, kh, kh, kh));
                // labels: white on the knob, grey beside it
                g2.setFont(getFont());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                double links = x + 3 + KNOPF_B / 2.0;
                double rechts = x + SPUR_B - 3 - KNOPF_B / 2.0;
                int ty = (int) Math.round(y + (SPUR_H - fm.getHeight()) / 2.0 + fm.getAscent());
                g2.setColor(Theme.mix(Theme.ON_INK, Theme.INK_3, pos));
                g2.drawString("DE", (int) Math.round(links - fm.stringWidth("DE") / 2.0), ty);
                g2.setColor(Theme.mix(Theme.INK_3, Theme.ON_INK, pos));
                g2.drawString("EN", (int) Math.round(rechts - fm.stringWidth("EN") / 2.0), ty);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Light/dark switch right of the language switch: a small track with a sun on the left and a moon on the
     * right; the knob shows the current mode. By day the sun has a ring of dots and a small cloud floats on the
     * free side, at night a crescent moon with a few stars. A click switches Filius at once (with a short
     * cross-fade).
     */
    public static final class DunkelKnopf extends JButton {
        private static final long serialVersionUID = 1L;
        private static final int SPUR_B = 54;
        private static final int SPUR_H = 26;
        /** position of the knob: 0 = light (left), 1 = dark (right) */
        private double pos;
        private final Timer anim;

        public DunkelKnopf() {
            pos = Theme.dunkel() ? 1 : 0;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tooltip();
            getAccessibleContext().setAccessibleName(Texts.get("dark.tooltip"));
            getModel().addChangeListener(e -> repaint());
            anim = new Timer(15, e -> {
                double ziel = Theme.dunkel() ? 1 : 0;
                pos += (ziel - pos) * 0.25;
                if (Math.abs(ziel - pos) < 0.01) {
                    pos = ziel;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            addActionListener(e -> DunkelModus.umschalten());
            // the knob follows every switch (also one made elsewhere)
            DunkelModus.addListener(() -> {
                tooltip();
                anim.start();
            });
        }

        private void tooltip() {
            setToolTipText(Texts.get(Theme.dunkel() ? "dark.tooltip.off" : "dark.tooltip.on"));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SPUR_B + 8, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                double x = (getWidth() - SPUR_B) / 2.0;
                double y = (getHeight() - SPUR_H) / 2.0;
                boolean hover = getModel().isRollover();
                RoundRectangle2D spur = new RoundRectangle2D.Double(x + 0.5, y + 0.5, SPUR_B - 1, SPUR_H - 1, SPUR_H, SPUR_H);
                // track: a light sky by day, the night sky with a few stars in the dark
                Color tag = hover ? new Color(0xDCE9FB) : new Color(0xE6F0FC);
                Color nacht = hover ? new Color(0x24365A) : new Color(0x1B2A47);
                g2.setColor(Theme.mix(tag, nacht, pos));
                g2.fill(spur);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Theme.mix(new Color(0xC3D6F2), new Color(0x34507E), pos));
                g2.draw(spur);
                if (pos > 0.05) {
                    g2.setColor(Theme.alpha(Color.WHITE, 0.85 * pos));
                    double[][] sterne = { { 11, 8, 1.6 }, { 17, 17, 1.2 }, { 24, 10, 1.0 } };
                    for (double[] st : sterne) {
                        g2.fill(new java.awt.geom.Ellipse2D.Double(x + st[0] - st[2], y + st[1] - st[2], st[2] * 2, st[2] * 2));
                    }
                }
                // a small cloud on the free side of the day sky (the counterpart of the stars at night); it fades
                // quickly, so it does not turn grey on the darkening track
                double wolke = Math.max(0, 1 - pos * 3.5);
                if (wolke > 0) {
                    double wx = x + 34;
                    double wy = y + 17;
                    java.awt.geom.Path2D.Double w = new java.awt.geom.Path2D.Double();
                    w.moveTo(wx + 1.800, wy);
                    w.lineTo(wx + 11.700, wy);
                    w.curveTo(wx + 12.974, wy - 0.326, wx + 13.866, wy - 1.475, wx + 13.866, wy - 2.790);
                    w.curveTo(wx + 13.866, wy - 4.105, wx + 12.974, wy - 5.254, wx + 11.700, wy - 5.580);
                    w.curveTo(wx + 10.956, wy - 6.948, wx + 9.479, wy - 7.752, wx + 7.926, wy - 7.636);
                    w.curveTo(wx + 6.373, wy - 7.520, wx + 5.032, wy - 6.504, wx + 4.500, wy - 5.040);
                    w.curveTo(wx + 3.108, wy - 5.786, wx + 1.376, wy - 5.262, wx + 0.630, wy - 3.870);
                    w.curveTo(wx - 0.116, wy - 2.478, wx + 0.408, wy - 0.746, wx + 1.800, wy);
                    w.closePath();
                    g2.setColor(Theme.alpha(Color.WHITE, wolke));
                    g2.fill(w);
                    g2.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(Theme.alpha(new Color(0xC3D6F2), wolke));
                    g2.draw(w);
                }
                // knob: the sun (disc with a ring of dots) turns into the moon
                double d = SPUR_H - 6;
                double kx = x + 3 + pos * (SPUR_B - 6 - d);
                double ky = y + 3;
                double mx = kx + d / 2;
                double my = ky + d / 2;
                double r = d * (0.29 + 0.21 * pos);
                g2.setColor(new Color(0, 0, 0, (int) Math.round(30 + 10 * pos)));
                g2.fill(new java.awt.geom.Ellipse2D.Double(mx - r, my - r + 1, r * 2, r * 2));
                Color sonne = new Color(0xF5B82E);
                Color mond = new Color(0xE9EEF6);
                g2.setColor(Theme.mix(sonne, mond, pos));
                java.awt.geom.Area knopf = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(mx - r, my - r, r * 2, r * 2));
                double q = (pos - 0.25) / 0.75;
                if (q > 0) {
                    // a second circle comes in from the upper right and cuts the crescent out of the grown disc
                    double rr = d * 0.39;
                    double weg = 1 + 1.6 * (1 - q);
                    double sx = mx + d * 0.37 * weg;
                    double sy = my - d * 0.29 * weg;
                    knopf.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(sx - rr, sy - rr, rr * 2, rr * 2)));
                }
                g2.fill(knopf);
                double strahl = Math.max(0, 1 - pos * 2);
                if (strahl > 0) {
                    // sun rays as eight dots; they fade and move in while the sun becomes the moon
                    g2.setColor(Theme.alpha(new Color(0xF0A21C), strahl));
                    double abstand = d * 0.445 * (1 - 0.25 * pos);
                    double pr = d * 0.0625;
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45);
                        double px = mx + abstand * Math.cos(a);
                        double py = my + abstand * Math.sin(a);
                        g2.fill(new java.awt.geom.Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2));
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    JComponent missionsButton() {
        return missionsButton;
    }
}
