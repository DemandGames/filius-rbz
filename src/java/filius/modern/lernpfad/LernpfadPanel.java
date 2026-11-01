package filius.modern.lernpfad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.lernpfad.Kapitel.Aufgabe;
import filius.modern.lernpfad.Kapitel.Begriff;
import filius.modern.lernpfad.Kapitel.Block;
import filius.modern.lernpfad.Kapitel.Frage;
import filius.modern.lernpfad.Kapitel.Gruppe;
import filius.modern.lernpfad.Kapitel.Seite;
import filius.modern.lernpfad.Ui.Karte;
import filius.modern.lernpfad.Ui.Kreis;
import filius.modern.lernpfad.Ui.Status;
import filius.modern.lernpfad.Ui.Text;

/** The learning path panel on the right side of the main window. */
final class LernpfadPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int BREITE = 400;
    private static final String[] TABS = { "lp.tab.read", "lp.tab.practice", "lp.tab.quiz", "lp.tab.glossary" };

    private enum Ansicht {
        WAHL, START, KAPITEL, GLOSSAR
    }

    private Ansicht ansicht = Ansicht.START;
    private int nr = 1;
    private int tab = 0;
    private int seite = 0;

    private final JLabel kopfTitel = Ui.label(t("lernpfad"), 16f, true, Theme.INK);
    private final JButton zurueck = Ui.link(t("lp.overview"));
    private final Ui.Inhalt inhalt = new Ui.Inhalt();
    private final JScrollPane scroll = new JScrollPane(inhalt);

    /** answer feedback per task, kept across rebuilds */
    private final Map<String, String> rueckmeldung = new HashMap<>();
    private final Map<String, Boolean> rueckmeldungOk = new HashMap<>();
    private final Map<String, String> entwurf = new HashMap<>();
    private final Map<String, Boolean> tippOffen = new HashMap<>();
    /** quiz question that was just answered: [question, chosen option, right?] */
    private int[] quizErgebnis;
    private final Random zufall = new Random();
    private final Map<Integer, List<Integer>> optionsFolge = new HashMap<>();

    LernpfadPanel() {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(BREITE, 400));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.LINE));

        JPanel kopf = new JPanel(new BorderLayout());
        kopf.setBackground(Theme.SURFACE);
        kopf.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE_SOFT),
                BorderFactory.createEmptyBorder(8, 14, 8, 8)));
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        links.setOpaque(false);
        zurueck.addActionListener(e -> {
            ansicht = Ansicht.START;
            aufbauen(true);
        });
        links.add(zurueck);
        links.add(kopfTitel);
        kopf.add(links, BorderLayout.WEST);
        JButton zu = Ui.link(null);
        zu.setIcon(new Ui.Zeichen(Ui.Symbol.KREUZ, Theme.INK_3, 16));
        zu.setToolTipText(t("lp.close"));
        zu.addActionListener(e -> Lernpfad.umschalten());
        kopf.add(zu, BorderLayout.EAST);
        add(kopf, BorderLayout.NORTH);

        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setLayout(new FesteBreite());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
        aufbauen(true);
    }

    // ------------------------------------------------------------------ navigation

    /** When the panel is opened: show the chapter the student works on. */
    void zeigeStart() {
        if (!Lernpfad.gewaehlt()) {
            ansicht = Ansicht.WAHL;
            aufbauen(true);
            return;
        }
        Fortschritt f = Lernpfad.fortschritt();
        int weiter = f.aktuell();
        if (f.aktiv > 0) {
            oeffneKapitel(f.aktiv);
        } else if (!f.leer() && weiter <= f.kapitelAnzahl() && f.freigeschaltet(weiter)) {
            // where the student stopped
            oeffneKapitel(weiter);
        } else {
            ansicht = Ansicht.START;
            aufbauen(true);
        }
    }

    void oeffneKapitel(int k) {
        nr = k;
        ansicht = Ansicht.KAPITEL;
        Fortschritt.Stand s = Lernpfad.fortschritt().stand(k);
        seite = Math.min(s.seite, Math.max(0, Lernpfad.kapitel(k).seiten.size() - 1));
        tab = phase(k);
        quizErgebnis = null;
        aufbauen(true);
        // a chapter that has not been started yet: its start in a window of its own (version 30)
        if (Lernpfad.fortschritt().aktiv != k && !s.fertig) {
            final int kapitel = k;
            SwingUtilities.invokeLater(() -> {
                if (ansicht == Ansicht.KAPITEL && nr == kapitel && isShowing()) {
                    startFenster();
                }
            });
        }
    }

    /**
     * The start of a chapter as a window: chapter number and title, its goal, and the choice how to start
     * (own network or start network; chapter 1 and the final project: one button). Cancel goes back to the
     * overview. Only a started chapter can be read page by page and is checked by Filius.
     */
    /** the start window is open (opening the panel and choosing the chapter must not stack two of them) */
    private boolean startFensterOffen;

    private void startFenster() {
        Fortschritt f = Lernpfad.fortschritt();
        Fortschritt.Stand s = f.stand(nr);
        if (f.aktiv == nr || s.fertig || startFensterOffen) {
            return;
        }
        startFensterOffen = true;
        try {
            startFensterZeigen(f);
        } finally {
            startFensterOffen = false;
        }
    }

    private void startFensterZeigen(Fortschritt f) {
        Kapitel k = Lernpfad.kapitel(nr);
        final int breite = 470;
        java.awt.Window fenster = SwingUtilities.getWindowAncestor(this);
        javax.swing.JDialog d = new javax.swing.JDialog(fenster, t("lp.chapter", nr), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel spalte = Ui.spalte();
        spalte.setOpaque(true);
        spalte.setBackground(Theme.SURFACE);
        spalte.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        java.util.List<Text> texte = new java.util.ArrayList<>();
        // the title already names the chapter number ("Kapitel 7 – …"), so the line above it just says "new chapter"
        spalte.add(Ui.label(t("lp.start.window.over").toUpperCase(java.util.Locale.ROOT), 11.5f, true, Theme.PATCH_BLUE));
        spalte.add(Ui.abstand(4));
        Text titel = new Text(t("lp.start.window.title", nr, k.titel), 20f, Theme.INK, true);
        texte.add(titel);
        spalte.add(titel);
        spalte.add(Ui.abstand(8));
        Text ziel = new Text(k.ziel, 13.5f, Theme.INK_2);
        texte.add(ziel);
        spalte.add(ziel);
        spalte.add(Ui.abstand(14));
        boolean abschluss = nr == Lernpfad.pfad().variantenKapitel();
        String hinweis = abschluss ? t(Lernpfad.pfad() == Pfad.STANDARD ? "lp.final.text.standard" : "lp.final.text")
                : nr == 1 ? t("lp.start.first") : t("lp.start.other");
        Karte karte = new Karte(Ui.AMBER_HELL, null);
        Text hinweisText = new Text(hinweis, 12.5f, Theme.INK_2);
        texte.add(hinweisText);
        karte.add(hinweisText);
        spalte.add(karte);
        spalte.add(Ui.abstand(16));
        JPanel knoepfe = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        knoepfe.setOpaque(false);
        knoepfe.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton abbrechen = Ui.knopf(t("lp.start.cancel"), false);
        abbrechen.addActionListener(e -> d.dispose());
        knoepfe.add(abbrechen);
        final boolean[] gestartet = new boolean[1];
        if (abschluss) {
            JButton los = Ui.knopf(t("lp.final.start"), true);
            los.addActionListener(e -> {
                gestartet[0] = true;
                d.dispose();
                Lernpfad.bearbeiten(nr, true);
                aufbauen(true);
            });
            knoepfe.add(los);
        } else if (nr == 1) {
            JButton los = Ui.knopf(t("lp.chapter.start"), true);
            los.addActionListener(e -> {
                gestartet[0] = true;
                d.dispose();
                boolean leer = filius.gui.GUIContainer.getGUIContainer().getKnotenItems().isEmpty();
                Lernpfad.bearbeiten(1, !leer && bestaetigt(t("lp.empty.q"), t("lp.empty.text")));
                aufbauen(true);
            });
            knoepfe.add(los);
        } else {
            JButton start = Ui.knopf(t("lp.load.start"), false);
            start.setToolTipText(t("lp.load.start.tip2"));
            start.addActionListener(e -> {
                gestartet[0] = true;
                d.dispose();
                Lernpfad.bearbeiten(nr, true);
                aufbauen(true);
            });
            JButton eigen = Ui.knopf(t("lp.own"), true);
            eigen.setToolTipText(t("lp.own.tip"));
            eigen.addActionListener(e -> {
                gestartet[0] = true;
                d.dispose();
                Lernpfad.bearbeiten(nr, false);
                aufbauen(true);
            });
            knoepfe.add(start);
            knoepfe.add(eigen);
        }
        knoepfe.setMaximumSize(new Dimension(Integer.MAX_VALUE, knoepfe.getPreferredSize().height));
        spalte.add(knoepfe);
        // wrapped texts get their height for the width of the window
        int innen = breite - 48;
        for (Text tx : texte) {
            tx.setSize(tx == hinweisText ? innen - 28 : innen, Short.MAX_VALUE);
            tx.setPreferredSize(new Dimension(tx == hinweisText ? innen - 28 : innen, tx.getPreferredSize().height));
        }
        // the content is exactly as wide as the texts were wrapped for; the window adds its frame around it
        // (before: the window was set to this width after pack, so on Windows the frame took width from the texts
        // and the last word of a long goal was cut off)
        spalte.setPreferredSize(new Dimension(breite, spalte.getPreferredSize().height));
        d.setContentPane(spalte);
        d.getRootPane().setDefaultButton(abschluss || nr == 1 ? (JButton) knoepfe.getComponent(1) : (JButton) knoepfe.getComponent(2));
        d.getRootPane().registerKeyboardAction(e -> d.dispose(), javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setResizable(false);
        d.pack();
        d.setLocationRelativeTo(fenster);
        d.setVisible(true);
        if (!gestartet[0]) {
            // cancelled: back to the overview, nothing was started
            ansicht = Ansicht.START;
            aufbauen(true);
        }
    }

    private int phase(int k) {
        Fortschritt.Stand s = Lernpfad.fortschritt().stand(k);
        boolean aktiv = Lernpfad.fortschritt().aktiv == k;
        if (s.fertig) {
            return aktiv && !s.praxisFertig ? 1 : 3;
        }
        if (!s.gelesen) {
            return 0;
        }
        return s.praxisFertig ? 2 : 1;
    }

    private boolean tabOffen(int t) {
        Fortschritt.Stand s = Lernpfad.fortschritt().stand(nr);
        switch (t) {
        case 0:
            return true;
        case 1:
            return s.gelesen || s.fertig;
        case 2:
            return s.praxisFertig || s.fertig;
        default:
            return s.fertig;
        }
    }

    void aktualisieren() {
        if (isVisible()) {
            aufbauen(false);
        }
    }

    private void aufbauen(boolean nachOben) {
        int pos = scroll.getVerticalScrollBar().getValue();
        inhalt.removeAll();
        if (ansicht != Ansicht.WAHL && !Lernpfad.gewaehlt()) {
            ansicht = Ansicht.WAHL;
        }
        zurueck.setVisible(ansicht != Ansicht.START && (ansicht != Ansicht.WAHL || Lernpfad.gewaehlt()));
        switch (ansicht) {
        case WAHL:
            kopfTitel.setText(t("lernpfad"));
            baueWahl();
            break;
        case START:
            kopfTitel.setText(t("lernpfad"));
            baueStart();
            break;
        case KAPITEL:
            kopfTitel.setText(t("lp.chapter", nr));
            baueKapitel();
            break;
        case GLOSSAR:
            kopfTitel.setText(t("lp.tab.glossary"));
            baueGlossar();
            break;
        }
        inhalt.add(Box.createVerticalGlue());
        inhalt.revalidate();
        inhalt.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(nachOben ? 0 : pos));
    }

    // ------------------------------------------------------------------ overview

    private void baueStart() {
        Fortschritt f = Lernpfad.fortschritt();
        Pfad pfad = Lernpfad.pfad();
        if (Lernpfad.zweiPfade()) {
            inhalt.add(Ui.label(t(pfad.textKey()).toUpperCase(java.util.Locale.ROOT), 11.5f, true, PFAD_FARBE.get(pfad)));
            inhalt.add(Ui.abstand(2));
        }
        inhalt.add(Ui.label(t("lp.start.title"), 20f, true, Theme.INK));
        inhalt.add(Ui.abstand(4));
        inhalt.add(new Text(t(pfad == Pfad.STANDARD ? "lp.start.intro.standard" : "lp.start.intro"), 13f, Theme.INK_2));
        inhalt.add(Ui.abstand(12));
        int fertig = f.fertigeKapitel();
        inhalt.add(new Ui.Balken(fertig / (double) f.kapitelAnzahl(), Theme.LED_GREEN));
        inhalt.add(Ui.abstand(4));
        inhalt.add(Ui.label(t("lp.start.done", fertig, f.kapitelAnzahl()), 12f, true, Theme.LED_GREEN));
        inhalt.add(Ui.abstand(14));

        int aktuell = f.aktuell();
        for (Kapitel k : Lernpfad.kapitel()) {
            inhalt.add(kapitelZeile(k, aktuell));
            inhalt.add(Ui.abstand(8));
        }
        inhalt.add(Ui.abstand(8));
        JButton glossar = Ui.knopf(t("lp.glossary.all"), false);
        glossar.addActionListener(e -> {
            ansicht = Ansicht.GLOSSAR;
            aufbauen(true);
        });
        inhalt.add(glossar);
        inhalt.add(Ui.abstand(12));
        if (Lernpfad.auswahlMoeglich()) {
            JButton wechseln = Ui.link(t("lp.path.switch"));
            wechseln.setFont(Theme.font(Font.PLAIN, 12f));
            wechseln.setToolTipText(t("lp.path.switch.tip"));
            wechseln.addActionListener(e -> {
                ansicht = Ansicht.WAHL;
                aufbauen(true);
            });
            inhalt.add(wechseln);
        }
        if (!f.leer()) {
            inhalt.add(Ui.abstand(4));
            JButton reset = Ui.link(t("lp.reset"));
            reset.setFont(Theme.font(Font.PLAIN, 12f));
            reset.setForeground(Theme.INK_3);
            reset.addActionListener(e -> {
                if (bestaetigt(t("lp.reset.title"), t("lp.reset.text"))) {
                    Lernpfad.zuruecksetzen();
                    ansicht = Ansicht.START;
                    aufbauen(true);
                }
            });
            inhalt.add(reset);
        }
    }

    /** colour of each path: standard green, advanced violet like the learning path button */
    private static final Map<Pfad, Color> PFAD_FARBE = new java.util.EnumMap<>(Pfad.class);

    static {
        PFAD_FARBE.put(Pfad.STANDARD, Theme.LED_GREEN);
        PFAD_FARBE.put(Pfad.FORTGESCHRITTEN, Theme.farbe(0x5638C4, 0xB7A6FF));
    }

    /** choice between the two learning paths (first start, or "switch learning path") */
    private void baueWahl() {
        inhalt.add(Ui.label(t("lp.choose.title"), 20f, true, Theme.INK));
        inhalt.add(Ui.abstand(4));
        inhalt.add(new Text(t("lp.choose.intro"), 13f, Theme.INK_2));
        inhalt.add(Ui.abstand(14));
        for (Pfad p : Pfad.values()) {
            inhalt.add(pfadKarte(p));
            inhalt.add(Ui.abstand(12));
        }
        inhalt.add(new Text(t("lp.choose.note"), 12f, Theme.INK_3));
    }

    private JComponent pfadKarte(Pfad p) {
        boolean aktuell = Lernpfad.gewaehlt() && Lernpfad.pfad() == p;
        Color farbe = PFAD_FARBE.get(p);
        Karte karte = new Karte(Theme.SURFACE, aktuell ? farbe : Theme.LINE);
        karte.kante = farbe;
        karte.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 14));
        JPanel kopf = new JPanel(new BorderLayout(8, 0));
        kopf.setOpaque(false);
        kopf.setAlignmentX(Component.LEFT_ALIGNMENT);
        kopf.add(Ui.label(t(p.textKey()), 16f, true, Theme.INK), BorderLayout.WEST);
        kopf.add(Ui.label(t("lp.choose.chapters", p.kapitel), 12f, true, farbe), BorderLayout.EAST);
        kopf.setMaximumSize(new Dimension(Integer.MAX_VALUE, kopf.getPreferredSize().height));
        karte.add(kopf);
        karte.add(Ui.abstand(6));
        karte.add(new Text(t(p.textKey() + ".text"), 12.5f, Theme.INK_2));
        karte.add(Ui.abstand(6));
        karte.add(new Text(t(p.textKey() + ".topics"), 12f, Theme.INK_3));
        Fortschritt f = Lernpfad.stand(p);
        if (!f.leer()) {
            karte.add(Ui.abstand(6));
            karte.add(Ui.label(t("lp.start.done", f.fertigeKapitel(), p.kapitel), 12f, true, Theme.LED_GREEN));
        }
        karte.add(Ui.abstand(10));
        JButton los = Ui.knopf(aktuell ? t("lp.choose.back") : !f.leer() ? t("lp.choose.continue") : t("lp.choose.start"), !aktuell);
        los.addActionListener(e -> {
            if (!aktuell) {
                Lernpfad.waehlen(p);
            }
            zeigeStart();
        });
        karte.add(los);
        return karte;
    }

    private JComponent kapitelZeile(Kapitel k, int aktuell) {
        Fortschritt f = Lernpfad.fortschritt();
        Fortschritt.Stand s = f.stand(k.nummer);
        boolean offen = f.freigeschaltet(k.nummer);
        Status st = s.fertig ? Status.FERTIG : !offen ? Status.GESPERRT : k.nummer == aktuell ? Status.AKTUELL : Status.OFFEN;
        boolean hervor = k.nummer == aktuell || f.aktiv == k.nummer;
        Karte karte = new Karte(hervor ? Ui.BLAU_HELL : Theme.SURFACE, hervor ? Theme.farbe(0xC9DAF8, 0x2F4E80) : Theme.LINE);
        karte.setLayout(new BorderLayout(12, 0));
        JLabel icon = new JLabel(new Kreis(st, String.valueOf(k.nummer), 30));
        icon.setVerticalAlignment(JLabel.TOP);
        karte.add(icon, BorderLayout.WEST);
        JPanel text = Ui.spalte();
        text.add(new Text(k.titel, 14f, offen ? Theme.INK : Theme.INK_3, true));
        text.add(Ui.abstand(2));
        text.add(Ui.label(statusText(k), 12f, false, s.fertig ? Theme.LED_GREEN : Theme.INK_3));
        if (offen && !s.fertig && (s.gelesen || s.seite > 0)) {
            text.add(Ui.abstand(6));
            text.add(new Ui.Balken(anteil(k), Theme.PATCH_BLUE));
        }
        karte.add(text, BorderLayout.CENTER);
        if (offen) {
            karte.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            karte.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!Ui.klick(e)) {
                        return;
                    }
                    oeffneKapitel(k.nummer);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    karte.rand = Theme.PATCH_BLUE;
                    karte.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    karte.rand = hervor ? Theme.farbe(0xC9DAF8, 0x2F4E80) : Theme.LINE;
                    karte.repaint();
                }
            });
        } else {
            karte.setToolTipText(t("lp.locked.chapter", k.nummer - 1));
        }
        return karte;
    }

    private String statusText(Kapitel k) {
        Fortschritt f = Lernpfad.fortschritt();
        Fortschritt.Stand s = f.stand(k.nummer);
        if (s.fertig) {
            return f.aktiv == k.nummer && !s.praxisFertig ? t("lp.status.repeat") : t("lp.status.done");
        }
        if (!f.freigeschaltet(k.nummer)) {
            return t("lp.status.locked");
        }
        if (!s.gelesen) {
            return s.seite > 0 ? t("lp.status.reading", s.seite + 1, k.seiten.size()) : t("lp.status.ready");
        }
        if (!s.praxisFertig) {
            return t("lp.status.practice", s.erledigt.size(), Lernpfad.aufgaben(k.nummer).size());
        }
        return t("lp.status.quiz");
    }

    private double anteil(Kapitel k) {
        Fortschritt.Stand s = Lernpfad.fortschritt().stand(k.nummer);
        double lesen = k.seiten.isEmpty() ? 1 : s.gelesen ? 1 : (s.seite + 1) / (double) k.seiten.size();
        int n = Lernpfad.aufgaben(k.nummer).size();
        double praxis = s.praxisFertig ? 1 : n == 0 ? 0 : s.erledigt.size() / (double) n;
        double quiz = s.fertig ? 1 : s.quizOffen == null ? 0 : 1 - s.quizOffen.size() / (double) Math.max(1, k.quiz.size() + s.quizFehler);
        return 0.25 * lesen + 0.5 * praxis + 0.25 * quiz;
    }

    // ------------------------------------------------------------------ chapter

    private void baueKapitel() {
        Kapitel k = Lernpfad.kapitel(nr);
        inhalt.add(new Text(k.titel, 20f, Theme.INK, true));
        inhalt.add(Ui.abstand(4));
        inhalt.add(new Text(k.ziel, 13f, Theme.INK_2));
        inhalt.add(Ui.abstand(12));
        JComponent banner = banner();
        if (banner != null) {
            inhalt.add(banner);
            inhalt.add(Ui.abstand(12));
        }
        if (tab == 0 && !k.seiten.isEmpty()) {
            seite = Math.max(0, Math.min(seite, k.seiten.size() - 1));
            Lernpfad.seiteGelesen(nr, seite);
        }
        inhalt.add(new Reiter());
        inhalt.add(Ui.abstand(14));
        if (!tabOffen(tab)) {
            tab = 0;
        }
        switch (tab) {
        case 0:
            baueLesen(k);
            break;
        case 1:
            bauePraxis(k);
            break;
        case 2:
            baueQuiz(k);
            break;
        default:
            baueKapitelGlossar(k);
            break;
        }
    }

    /** Which chapter is checked right now, and how to start or repeat this one. */
    private JComponent banner() {
        Fortschritt f = Lernpfad.fortschritt();
        Fortschritt.Stand s = f.stand(nr);
        if (f.aktiv == nr) {
            if (s.fertig && s.praxisFertig) {
                return null;
            }
            JPanel zeile = new JPanel(new BorderLayout());
            zeile.setOpaque(false);
            zeile.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel chip = Ui.label(t("lp.checking"), 12f, true, Theme.LED_GREEN);
            chip.setIcon(new Ui.Zeichen(Ui.Symbol.PUNKT, Theme.LED_GREEN, 14));
            chip.setIconTextGap(3);
            zeile.add(chip, BorderLayout.WEST);
            if (s.gelesen || s.fertig) {
                JButton neu = Ui.link(t("lp.load.start"));
                neu.setFont(Theme.font(Font.PLAIN, 12f));
                neu.setToolTipText(t("lp.load.start.tip1"));
                neu.addActionListener(e -> {
                    if (bestaetigt(t("lp.load.start.q"), t("lp.load.start.text", nr))) {
                        Lernpfad.bearbeiten(nr, true);
                        tab = 1;
                        aufbauen(true);
                    }
                });
                zeile.add(neu, BorderLayout.EAST);
            }
            zeile.setMaximumSize(new Dimension(Integer.MAX_VALUE, zeile.getPreferredSize().height));
            return zeile;
        }
        Karte karte = new Karte(s.fertig ? Ui.GRUEN_HELL : Ui.AMBER_HELL, null);
        if (s.fertig) {
            karte.add(new Text(t("lp.done.text"), 12.5f, Theme.INK_2));
            karte.add(Ui.abstand(8));
            JButton w = Ui.knopf(t("lp.repeat"), false);
            w.addActionListener(e -> {
                Lernpfad.bearbeiten(nr, true);
                tab = 0;
                seite = 0;
                aufbauen(true);
            });
            karte.add(w);
            return karte;
        }
        // not started yet: the start (text and choice) is in the start window (version 31); the panel only offers to
        // open it again, so the hint is not shown twice
        JPanel zeile = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        zeile.setOpaque(false);
        zeile.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton los = Ui.knopf(t(nr == Lernpfad.pfad().variantenKapitel() ? "lp.final.start" : "lp.chapter.start"), true);
        los.addActionListener(e -> startFenster());
        zeile.add(los);
        zeile.setMaximumSize(new Dimension(Integer.MAX_VALUE, zeile.getPreferredSize().height));
        return zeile;
    }

    static String t(String key, Object... args) {
        return args.length == 0 ? Texts.get(key) : Texts.format(key, args);
    }

    private boolean bestaetigt(String titel, String text) {
        return javax.swing.JOptionPane.showConfirmDialog(this, text, titel, javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION;
    }

    /** segmented control Lesen | Praxis | Quiz | Glossar */
    private final class Reiter extends JPanel {
        private static final long serialVersionUID = 1L;

        Reiter() {
            setOpaque(false);
            setLayout(new java.awt.GridLayout(1, 4, 4, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            for (int i = 0; i < TABS.length; i++) {
                final int t = i;
                boolean offen = tabOffen(i);
                JButton b = new JButton(t(TABS[i]));
                if (!offen) {
                    b.setIcon(new Ui.Zeichen(Ui.Symbol.SCHLOSS, Theme.INK_3, 12));
                    b.setDisabledIcon(b.getIcon());
                    b.setIconTextGap(3);
                    b.setMargin(new java.awt.Insets(0, 0, 0, 0));
                }
                b.setFont(Theme.font(Font.BOLD, 12.5f));
                b.setFocusable(false);
                b.setEnabled(offen);
                b.setForeground(i == tab ? Color.WHITE : offen ? Theme.INK_2 : Theme.INK_3);
                b.setContentAreaFilled(false);
                b.setBorderPainted(false);
                b.setOpaque(false);
                b.setCursor(Cursor.getPredefinedCursor(offen ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                b.setToolTipText(offen ? null : sperrGrund(i));
                b.addActionListener(e -> {
                    tab = t;
                    quizErgebnis = null;
                    aufbauen(true);
                });
                add(b);
            }
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setPreferredSize(new Dimension(300, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                int h = getHeight();
                g2.setColor(Theme.farbe(0xEEF2F6, 0x223040));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), h, h, h));
                Component c = getComponent(tab);
                g2.setColor(Theme.PATCH_BLUE);
                g2.fill(new RoundRectangle2D.Double(c.getX(), c.getY(), c.getWidth(), c.getHeight(), c.getHeight(), c.getHeight()));
            } finally {
                g2.dispose();
            }
        }
    }

    private String sperrGrund(int t) {
        switch (t) {
        case 1:
            return t("lp.lock.read");
        case 2:
            return t("lp.lock.practice");
        default:
            return t("lp.lock.quiz");
        }
    }

    // ------------------------------------------------------------------ reading

    private void baueLesen(Kapitel k) {
        if (k.seiten.isEmpty()) {
            inhalt.add(new Text(t("lp.nopages"), 13f, Theme.INK_3));
            return;
        }
        Seite s = k.seiten.get(seite);
        inhalt.add(Ui.label(t("lp.page", seite + 1, k.seiten.size()), 12f, true, Theme.PATCH_BLUE));
        inhalt.add(Ui.abstand(2));
        inhalt.add(new Text(s.titel, 17f, Theme.INK, true));
        inhalt.add(Ui.abstand(10));
        for (Block b : s.bloecke) {
            inhalt.add(block(b));
            inhalt.add(Ui.abstand(10));
        }
        inhalt.add(Ui.abstand(6));
        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (seite > 0) {
            JButton z = Ui.knopf(t("lp.back"), false);
            z.addActionListener(e -> {
                seite--;
                aufbauen(true);
            });
            nav.add(z, BorderLayout.WEST);
        }
        if (seite < k.seiten.size() - 1) {
            JButton w = Ui.knopf(t("lp.next"), true);
            w.addActionListener(e -> {
                seite++;
                aufbauen(true);
            });
            nav.add(w, BorderLayout.EAST);
        } else {
            JButton w = Ui.knopf(t("lp.to.practice"), true);
            w.addActionListener(e -> {
                tab = 1;
                aufbauen(true);
            });
            nav.add(w, BorderLayout.EAST);
        }
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, nav.getPreferredSize().height));
        inhalt.add(nav);
    }

    private JComponent block(Block b) {
        switch (b.art) {
        case BILD:
            return new Ui.Bild(Lernpfad.bild(b.text), Lernpfad.bildDunkel(b.text), b.unterschrift);
        case LISTE: {
            JPanel liste = Ui.spalte();
            int nr = 0;
            for (String punkt : b.punkte) {
                Ui.Zeile zeile = new Ui.Zeile(8);
                // numbered steps (e.g. the handshake of TLS) show their number, other lists a dot
                JLabel dot = Ui.label(b.nummeriert ? ++nr + "." : "•", b.nummeriert ? 13.5f : 14f, true, Theme.PATCH_BLUE);
                dot.setVerticalAlignment(JLabel.TOP);
                if (b.nummeriert) {
                    dot.setPreferredSize(new Dimension(18, dot.getPreferredSize().height));
                }
                zeile.add(dot, BorderLayout.WEST);
                zeile.add(new Text(punkt, 13.5f, Theme.INK_2), BorderLayout.CENTER);
                liste.add(zeile);
                liste.add(Ui.abstand(4));
            }
            return liste;
        }
        case TABELLE:
            return new Ui.Tabelle(b.zeilen);
        case MERKE:
        case TIPP: {
            boolean merke = b.art == Kapitel.BlockArt.MERKE;
            Karte k = new Karte(merke ? Ui.BLAU_HELL : Ui.AMBER_HELL, null);
            k.kante = merke ? Theme.PATCH_BLUE : Theme.MARKER_AMBER;
            k.add(Ui.label(merke ? t("lp.note") : t("lp.tip"), 12f, true, merke ? Theme.PATCH_BLUE : Theme.farbe(0xB57609, 0xF0B04A)));
            k.add(Ui.abstand(3));
            k.add(new Text(b.text, 13f, Theme.INK));
            return k;
        }
        default:
            return new Text(b.text, 13.5f, Theme.INK_2);
        }
    }

    // ------------------------------------------------------------------ practice

    private void bauePraxis(Kapitel k) {
        Fortschritt f = Lernpfad.fortschritt();
        Fortschritt.Stand s = f.stand(nr);
        List<Aufgabe> aufgaben = Lernpfad.aufgaben(nr);
        if (f.aktiv != nr) {
            Karte hinweis = new Karte(Ui.AMBER_HELL, null);
            hinweis.add(new Text(t("lp.practice.notyet"), 12.5f, Theme.INK_2));
            inhalt.add(hinweis);
            inhalt.add(Ui.abstand(12));
        }
        int fertig = 0;
        for (Aufgabe a : aufgaben) {
            if (s.erledigt.contains(a.id)) {
                fertig++;
            }
        }
        inhalt.add(new Ui.Balken(aufgaben.isEmpty() ? 0 : fertig / (double) aufgaben.size(), Theme.LED_GREEN));
        inhalt.add(Ui.abstand(4));
        inhalt.add(Ui.label(t("lp.tasks.done", fertig, aufgaben.size()), 12f, true, Theme.INK_2));
        inhalt.add(Ui.abstand(12));

        String naechste = null;
        for (Aufgabe a : aufgaben) {
            if (!s.erledigt.contains(a.id) && Lernpfad.gruppeOffen(nr, a.gruppe)) {
                naechste = a.id;
                break;
            }
        }
        int gruppe = -1;
        for (Aufgabe a : aufgaben) {
            if (a.gruppe != gruppe && a.gruppe >= 0) {
                gruppe = a.gruppe;
                Gruppe g = k.gruppen.get(gruppe);
                boolean offen = Lernpfad.gruppeOffen(nr, gruppe);
                inhalt.add(Ui.abstand(6));
                if (offen) {
                    inhalt.add(new Text(g.titel, 15f, Theme.INK, true));
                } else {
                    JLabel kopf = Ui.label(g.titel, 15f, true, Theme.INK_3);
                    kopf.setIcon(new Ui.Zeichen(Ui.Symbol.SCHLOSS, Theme.INK_3, 15));
                    inhalt.add(kopf);
                }
                if (!g.text.isEmpty()) {
                    inhalt.add(Ui.abstand(3));
                    inhalt.add(new Text(g.text, 12.5f, Theme.INK_2));
                }
                inhalt.add(Ui.abstand(10));
                if (!offen) {
                    inhalt.add(Ui.label(t("lp.lock.part"), 12f, false, Theme.INK_3));
                    inhalt.add(Ui.abstand(10));
                    continue;
                }
            }
            if (a.gruppe >= 0 && !Lernpfad.gruppeOffen(nr, a.gruppe)) {
                continue;
            }
            inhalt.add(aufgabe(a, s, a.id.equals(naechste)));
            inhalt.add(Ui.abstand(10));
        }
        if (s.praxisFertig) {
            inhalt.add(Ui.abstand(4));
            JButton quiz = Ui.knopf(t("lp.to.quiz"), true);
            quiz.addActionListener(e -> {
                tab = 2;
                aufbauen(true);
            });
            inhalt.add(quiz);
        }
    }

    private JComponent aufgabe(Aufgabe a, Fortschritt.Stand s, boolean naechste) {
        boolean erledigt = s.erledigt.contains(a.id);
        Karte karte = new Karte(erledigt ? Ui.GRUEN_HELL : naechste ? Theme.SURFACE : Theme.SURFACE,
                erledigt ? Theme.farbe(0xBFE6CF, 0x2C5B43) : naechste ? Theme.PATCH_BLUE : Theme.LINE);
        karte.setLayout(new BorderLayout(10, 0));
        JLabel icon = new JLabel(new Kreis(erledigt ? Status.FERTIG : naechste ? Status.AKTUELL : Status.OFFEN, "", 22));
        icon.setVerticalAlignment(JLabel.TOP);
        karte.add(icon, BorderLayout.WEST);
        JPanel spalte = Ui.spalte();
        spalte.add(Ui.label(t("lp.task", Lernpfad.anzeige(nr, a.id)), 11.5f, true, erledigt ? Theme.LED_GREEN : Theme.PATCH_BLUE));
        spalte.add(Ui.abstand(3));
        boolean meldung = a.text.startsWith("„") || a.text.startsWith("\"") || a.text.startsWith("“");
        boolean erster = true;
        for (Block b : a.bloecke) {
            if (!erster) {
                spalte.add(Ui.abstand(6));
            }
            if (erster && meldung && b.art == Kapitel.BlockArt.ABSATZ) {
                Karte m = new Karte(Theme.farbe(0xF3F5F8, 0x1C2835), null);
                m.kante = Theme.INK_3;
                m.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 10));
                m.add(Ui.label(t("lp.ticket"), 11f, true, Theme.INK_3));
                m.add(Ui.abstand(2));
                // the card shows that it is a message: without the quotes around it (a closing quote
                // would otherwise sometimes wrap onto a line of its own)
                m.add(new Text(b.text.replaceAll("^[„\"“]\\s*", "").replaceAll("\\s*[“\"”]$", ""), 13f, Theme.INK));
                spalte.add(m);
            } else {
                spalte.add(aufgabenBlock(b));
            }
            erster = false;
        }
        if (erledigt && !a.ursache.isEmpty()) {
            spalte.add(Ui.abstand(6));
            spalte.add(new Text("**" + t("lp.cause") + "** " + a.ursache, 12.5f, Theme.INK_2));
        }
        if (a.hatFrage()) {
            spalte.add(Ui.abstand(8));
            spalte.add(frage(a, s, erledigt));
        }
        if (!erledigt && !a.tipp.isEmpty()) {
            spalte.add(Ui.abstand(6));
            boolean offen = tippOffen.getOrDefault(a.id, false);
            JButton t = Ui.link(offen ? t("lp.tip.hide") : t("lp.tip.show"));
            t.setFont(Theme.font(Font.PLAIN, 12f));
            t.addActionListener(e -> {
                tippOffen.put(a.id, !offen);
                aufbauen(false);
            });
            spalte.add(t);
            if (offen) {
                spalte.add(new Text(a.tipp, 12.5f, Theme.farbe(0x8A5A00, 0xE8B060)));
            }
        }
        karte.add(spalte, BorderLayout.CENTER);
        return karte;
    }

    /** a part of a task text: paragraph, list or table */
    private JComponent aufgabenBlock(Block b) {
        switch (b.art) {
        case LISTE: {
            JPanel liste = Ui.spalte();
            for (int i = 0; i < b.punkte.size(); i++) {
                Ui.Zeile zeile = new Ui.Zeile(7);
                JLabel dot = Ui.label("•", 13f, true, Theme.PATCH_BLUE);
                dot.setVerticalAlignment(JLabel.TOP);
                zeile.add(dot, BorderLayout.WEST);
                zeile.add(new Text(b.punkte.get(i), 13f, Theme.INK), BorderLayout.CENTER);
                liste.add(zeile);
                if (i < b.punkte.size() - 1) {
                    liste.add(Ui.abstand(3));
                }
            }
            return liste;
        }
        case TABELLE:
            return new Ui.Tabelle(b.zeilen);
        default:
            return new Text(b.text, 13f, Theme.INK);
        }
    }

    private JComponent frage(Aufgabe a, Fortschritt.Stand s, boolean erledigt) {
        JPanel p = Ui.spalte();
        p.add(new Text("**" + t("lp.question") + "** " + a.frage, 13f, Theme.INK));
        p.add(Ui.abstand(6));
        String gegeben = s.antworten.get(a.id);
        if (a.hatAuswahl()) {
            for (int i = 0; i < a.optionen.size(); i++) {
                final int idx = i;
                boolean gewaehlt = String.valueOf(i).equals(gegeben);
                Karte o = new Karte(gewaehlt ? Ui.BLAU_HELL : Theme.SURFACE, gewaehlt ? Theme.PATCH_BLUE : Theme.LINE);
                o.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
                o.setLayout(new BorderLayout(8, 0));
                JLabel punkt = new JLabel(new Auswahlpunkt(gewaehlt));
                punkt.setVerticalAlignment(JLabel.TOP);
                o.add(punkt, BorderLayout.WEST);
                o.add(new Text(a.optionen.get(i), 12.5f, Theme.INK), BorderLayout.CENTER);
                if (!erledigt) {
                    o.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    o.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (!Ui.klick(e)) {
                                return;
                            }
                            pruefeAntwort(a, String.valueOf(idx));
                        }
                    });
                }
                p.add(o);
                p.add(Ui.abstand(4));
            }
        } else {
            JPanel zeile = new JPanel(new BorderLayout(6, 0));
            zeile.setOpaque(false);
            zeile.setAlignmentX(Component.LEFT_ALIGNMENT);
            JTextField feld = new JTextField(entwurf.getOrDefault(a.id, gegeben == null ? "" : gegeben));
            feld.setFont(Theme.font(Font.PLAIN, 13f));
            feld.putClientProperty("JTextField.placeholderText", a.eingabe);
            feld.setEnabled(!erledigt);
            feld.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    entwurf.put(a.id, feld.getText());
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    entwurf.put(a.id, feld.getText());
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {}
            });
            feld.addActionListener(e -> pruefeAntwort(a, feld.getText()));
            zeile.add(feld, BorderLayout.CENTER);
            if (!erledigt) {
                JButton ok = Ui.knopf(t("lp.check"), false);
                ok.addActionListener(e -> pruefeAntwort(a, feld.getText()));
                zeile.add(ok, BorderLayout.EAST);
            }
            zeile.setMaximumSize(new Dimension(Integer.MAX_VALUE, zeile.getPreferredSize().height));
            p.add(zeile);
        }
        String r = rueckmeldung.get(a.id);
        if (r != null && !erledigt) {
            p.add(Ui.abstand(4));
            p.add(new Text(r, 12.5f, rueckmeldungOk.getOrDefault(a.id, false) ? Theme.LED_GREEN : Theme.SIGNAL_RED));
        }
        return p;
    }

    private void pruefeAntwort(Aufgabe a, String antwort) {
        if (antwort == null || antwort.isBlank()) {
            return;
        }
        if (Lernpfad.fortschritt().aktiv != nr) {
            rueckmeldung.put(a.id, t("lp.answer.start"));
            rueckmeldungOk.put(a.id, false);
            aufbauen(false);
            return;
        }
        Lernpfad.Ergebnis e = Lernpfad.antworten(nr, a, antwort.trim());
        switch (e) {
        case RICHTIG:
            rueckmeldung.remove(a.id);
            break;
        case RICHTIG_NOCH_OFFEN:
            rueckmeldung.put(a.id, t("lp.answer.right.open"));
            rueckmeldungOk.put(a.id, true);
            break;
        default:
            rueckmeldung.put(a.id, a.hatAuswahl() ? t("lp.answer.wrong.choice") : t("lp.answer.wrong.input"));
            rueckmeldungOk.put(a.id, false);
        }
        aufbauen(false);
    }

    // ------------------------------------------------------------------ quiz

    private void baueQuiz(Kapitel k) {
        Fortschritt.Stand s = Lernpfad.fortschritt().stand(nr);
        if (s.quizOffen == null) {
            Lernpfad.quizBeginnen(nr);
        }
        if (quizErgebnis != null) {
            zeigeQuizErgebnis(k, s);
            return;
        }
        if (s.quizOffen.isEmpty()) {
            Karte fertig = new Karte(Ui.GRUEN_HELL, null);
            fertig.add(Ui.label(t("lp.quiz.done"), 17f, true, Theme.LED_GREEN));
            fertig.add(Ui.abstand(4));
            fertig.add(new Text(s.quizFehler == 0 ? t("lp.quiz.perfect") : s.quizFehler == 1 ? t("lp.quiz.repeated.one") : t("lp.quiz.repeated.many", s.quizFehler), 13f, Theme.INK_2));
            fertig.add(Ui.abstand(10));
            JButton g = Ui.knopf(t("lp.to.glossary"), true);
            g.addActionListener(e -> {
                tab = 3;
                aufbauen(true);
            });
            fertig.add(g);
            fertig.add(Ui.abstand(8));
            JButton nochmal = Ui.link(t("lp.quiz.again"));
            nochmal.addActionListener(e -> {
                Lernpfad.quizBeginnen(nr);
                aufbauen(true);
            });
            fertig.add(nochmal);
            inhalt.add(fertig);
            return;
        }
        int frage = s.quizOffen.get(0);
        Frage f = k.quiz.get(frage);
        int gesamt = k.quiz.size();
        int beantwortet = Math.max(0, gesamt + s.quizFehler - s.quizOffen.size());
        inhalt.add(new Ui.Balken(beantwortet / (double) (gesamt + s.quizFehler), Theme.PATCH_BLUE));
        inhalt.add(Ui.abstand(4));
        boolean wiederholung = beantwortet >= gesamt;
        inhalt.add(Ui.label(wiederholung ? (s.quizOffen.size() == 1 ? t("lp.quiz.repeat.one") : t("lp.quiz.repeat.many", s.quizOffen.size()))
                : t("lp.quiz.question", beantwortet + 1, gesamt), 12f, true, wiederholung ? Theme.farbe(0xB57609, 0xF0B04A) : Theme.INK_2));
        inhalt.add(Ui.abstand(10));
        inhalt.add(new Text(f.text, 16f, Theme.INK, true));
        inhalt.add(Ui.abstand(12));
        for (int opt : folge(frage, f, true)) {
            Karte o = new Karte(Theme.SURFACE, Theme.LINE);
            o.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            o.add(new Text(f.optionen.get(opt), 13.5f, Theme.INK));
            o.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            o.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!Ui.klick(e)) {
                        return;
                    }
                    boolean richtig = Lernpfad.quizAntwort(nr, frage, opt);
                    quizErgebnis = new int[] { frage, opt, richtig ? 1 : 0 };
                    aufbauen(false);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    o.rand = Theme.PATCH_BLUE;
                    o.hintergrund = Ui.BLAU_HELL;
                    o.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    o.rand = Theme.LINE;
                    o.hintergrund = Theme.SURFACE;
                    o.repaint();
                }
            });
            inhalt.add(o);
            inhalt.add(Ui.abstand(6));
        }
    }

    /** order of the options of a question; a repeated question gets a new order */
    private List<Integer> folge(int frage, Frage f, boolean neu) {
        List<Integer> l = optionsFolge.get(frage);
        if (l == null || neu && l.size() != f.optionen.size()) {
            l = new ArrayList<>();
            for (int i = 0; i < f.optionen.size(); i++) {
                l.add(i);
            }
            Collections.shuffle(l, zufall);
            optionsFolge.put(frage, l);
        }
        return l;
    }

    private void zeigeQuizErgebnis(Kapitel k, Fortschritt.Stand s) {
        int frage = quizErgebnis[0];
        int gewaehlt = quizErgebnis[1];
        boolean richtig = quizErgebnis[2] == 1;
        Frage f = k.quiz.get(frage);
        inhalt.add(Ui.label(richtig ? t("lp.quiz.right") : t("lp.quiz.wrong"), 13f, true,
                richtig ? Theme.LED_GREEN : Theme.SIGNAL_RED));
        inhalt.add(Ui.abstand(8));
        inhalt.add(new Text(f.text, 16f, Theme.INK, true));
        inhalt.add(Ui.abstand(12));
        for (int opt : folge(frage, f, false)) {
            boolean istRichtig = opt == f.richtig;
            boolean istGewaehlt = opt == gewaehlt;
            Color bg = istRichtig ? Ui.GRUEN_HELL : istGewaehlt ? Ui.ROT_HELL : Theme.SURFACE;
            Color rand = istRichtig ? Theme.LED_GREEN : istGewaehlt ? Theme.SIGNAL_RED : Theme.LINE;
            Karte o = new Karte(bg, rand);
            o.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            o.setLayout(new BorderLayout(8, 0));
            if (istRichtig || istGewaehlt) {
                JLabel z = new JLabel(istRichtig ? new Kreis(Status.FERTIG, "", 18) : new Ui.Zeichen(Ui.Symbol.FALSCH, Theme.SIGNAL_RED, 18));
                z.setVerticalAlignment(JLabel.TOP);
                o.add(z, BorderLayout.WEST);
            }
            o.add(new Text(f.optionen.get(opt), 13.5f, Theme.INK), BorderLayout.CENTER);
            inhalt.add(o);
            inhalt.add(Ui.abstand(6));
        }
        String warumFalsch = richtig ? "" : f.warumFalsch(gewaehlt);
        if (!warumFalsch.isEmpty()) {
            // first: why the chosen answer is wrong
            inhalt.add(Ui.abstand(4));
            Karte w = new Karte(Ui.ROT_HELL, null);
            w.kante = Theme.SIGNAL_RED;
            w.add(Ui.label(t("lp.quiz.why.wrong"), 12f, true, Theme.SIGNAL_RED));
            w.add(Ui.abstand(3));
            w.add(new Text(warumFalsch, 13f, Theme.INK));
            inhalt.add(w);
            inhalt.add(Ui.abstand(8));
        }
        if (!f.erklaerung.isEmpty()) {
            // then: why the right answer is right
            inhalt.add(Ui.abstand(4));
            Karte e = new Karte(Ui.BLAU_HELL, null);
            e.kante = Theme.PATCH_BLUE;
            e.add(Ui.label(richtig ? t("lp.explanation") : t("lp.quiz.why.right"), 12f, true, Theme.PATCH_BLUE));
            e.add(Ui.abstand(3));
            if (!richtig && f.richtig >= 0) {
                e.add(new Text("**" + f.optionen.get(f.richtig) + "**", 13f, Theme.INK));
                e.add(Ui.abstand(4));
            }
            e.add(new Text(f.erklaerung, 13f, Theme.INK));
            inhalt.add(e);
        }
        inhalt.add(Ui.abstand(12));
        JButton weiter = Ui.knopf(s.quizOffen.isEmpty() ? t("lp.finish") : t("lp.next.question"), true);
        weiter.addActionListener(e -> {
            optionsFolge.remove(frage);
            quizErgebnis = null;
            aufbauen(true);
        });
        inhalt.add(weiter);
    }

    // ------------------------------------------------------------------ glossary

    private void baueKapitelGlossar(Kapitel k) {
        Fortschritt f = Lernpfad.fortschritt();
        inhalt.add(new Text(t("lp.glossary.chapter", nr), 12.5f, Theme.INK_2));
        inhalt.add(Ui.abstand(12));
        for (Begriff b : k.glossar) {
            inhalt.add(begriff(b, true));
            inhalt.add(Ui.abstand(8));
        }
        if (nr < f.kapitelAnzahl() && f.fertig(nr)) {
            inhalt.add(Ui.abstand(6));
            JButton weiter = Ui.knopf(t("lp.next.chapter", nr + 1), true);
            weiter.addActionListener(e -> oeffneKapitel(nr + 1));
            inhalt.add(weiter);
        } else if (nr == f.kapitelAnzahl() && f.fertig(nr)) {
            Karte ende = new Karte(Ui.GRUEN_HELL, null);
            ende.add(Ui.label(t("lp.all.done"), 16f, true, Theme.LED_GREEN));
            ende.add(Ui.abstand(4));
            ende.add(new Text(t(Lernpfad.pfad() == Pfad.STANDARD ? "lp.all.done.text.standard" : "lp.all.done.text"), 13f, Theme.INK_2));
            inhalt.add(ende);
        }
    }

    private JComponent begriff(Begriff b, boolean frei) {
        Karte k = new Karte(frei ? Theme.SURFACE : Theme.SURFACE_2, Theme.LINE);
        k.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        k.add(Ui.label(frei ? b.name : "???", 13.5f, true, frei ? Theme.INK : Theme.INK_3));
        k.add(Ui.abstand(2));
        k.add(new Text(frei ? b.erklaerung : t("lp.glossary.locked"), 12.5f, frei ? Theme.INK_2 : Theme.INK_3));
        return k;
    }

    private void baueGlossar() {
        Fortschritt f = Lernpfad.fortschritt();
        inhalt.add(new Text(t("lp.glossary.intro"), 12.5f, Theme.INK_2));
        inhalt.add(Ui.abstand(12));
        for (Kapitel k : Lernpfad.kapitel()) {
            boolean frei = f.fertig(k.nummer);
            inhalt.add(Ui.label(t("lp.chapter.title", k.nummer, k.titel), 12.5f, true, frei ? Theme.PATCH_BLUE : Theme.INK_3));
            inhalt.add(Ui.abstand(6));
            for (Begriff b : k.glossar) {
                inhalt.add(begriff(b, frei));
                inhalt.add(Ui.abstand(6));
            }
            inhalt.add(Ui.abstand(10));
        }
    }

    /** radio-style marker of a choice option */
    private static final class Auswahlpunkt implements javax.swing.Icon {
        private final boolean an;

        Auswahlpunkt(boolean an) {
            this.an = an;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.smooth(g.create());
            try {
                g2.setStroke(new java.awt.BasicStroke(1.6f));
                g2.setColor(an ? Theme.PATCH_BLUE : Theme.farbe(0xB8C4D0, 0x46586B));
                g2.draw(new java.awt.geom.Ellipse2D.Double(x + 1.5, y + 2.5, 13, 13));
                if (an) {
                    g2.fill(new java.awt.geom.Ellipse2D.Double(x + 5, y + 6, 6, 6));
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }

    /**
     * The page keeps the same width whether the scroll bar is shown or not (version 31). Texts and pictures take
     * their height from their width. When a page was about as tall as the visible area (e.g. chapter 8, page 2 of
     * the standard path in a maximised window), the scroll bar appeared, the page became narrower and taller, the
     * texts were laid out for the old width, the bar disappeared again, and so on: the page shook in every frame.
     * Now the room of the scroll bar stays free when there is no bar, so the width never changes.
     */
    static final class FesteBreite extends javax.swing.ScrollPaneLayout {
        private static final long serialVersionUID = 1L;

        @Override
        public void layoutContainer(java.awt.Container parent) {
            super.layoutContainer(parent);
            if (vsb != null && !vsb.isVisible() && viewport != null) {
                java.awt.Rectangle r = viewport.getBounds();
                int b = vsb.getPreferredSize().width;
                viewport.setBounds(r.x, r.y, Math.max(0, r.width - b), r.height);
            }
        }
    }
}
