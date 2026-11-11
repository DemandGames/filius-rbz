package filius.modern.lernpfad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import filius.gui.GUIContainer;
import filius.gui.JMainFrame;
import filius.hardware.knoten.Knoten;
import filius.modern.Klausur;
import filius.modern.Overlay;
import filius.modern.Prefs;
import filius.modern.Texts;
import filius.modern.Theme;
import filius.modern.lernpfad.Kapitel.Aufgabe;
import filius.rahmenprogramm.SzenarioVerwaltung;
import filius.software.system.SystemSoftware;

/**
 * The learning path: chapters with reading pages, practice tasks that are checked in the running
 * network, a quiz and a glossary. This class connects the content, the progress, the event
 * observer and the panel on the right of the main window.
 */
public final class Lernpfad {

    /** chapters of each path, read once */
    private static final Map<Pfad, List<Kapitel>> kapitel = new java.util.EnumMap<>(Pfad.class);
    /** progress of both paths; a project file keeps both */
    private static final Map<Pfad, Fortschritt> staende = new java.util.EnumMap<>(Pfad.class);
    /** the path of this project, null while the student has not chosen one */
    private static Pfad pfad = vorgegeben();
    private static final Ereignisse ereignisse = new Ereignisse();
    private static final Beobachter beobachter = new Beobachter(ereignisse);
    private static final Map<String, Long> seit = new HashMap<>();
    private static final List<Runnable> hoerer = new ArrayList<>();
    private static final Random zufall = new Random();

    private static Timer timer;
    private static LernpfadPanel panel;
    private static long installiert;
    private static boolean startGelesen;
    private static int letzterModus;
    private static long praxisPruefungAb;

    private Lernpfad() {}

    // ------------------------------------------------------------------ content

    /** Folder lernpfad/ next to the program. */
    public static File ordner() {
        String eigener = System.getProperty("filius.lernpfad");
        if (eigener != null) {
            return new File(eigener);
        }
        for (Class<?> c : new Class<?>[] { Lernpfad.class, filius.Main.class }) {
            try {
                File ort = new File(c.getProtectionDomain().getCodeSource().getLocation().toURI());
                File dir = ort.isFile() ? ort.getParentFile() : ort;
                for (int i = 0; i < 4 && dir != null; i++) {
                    File f = new File(dir, "lernpfad");
                    if (new File(f, "kapitel-01.txt").isFile()) {
                        return f;
                    }
                    dir = dir.getParentFile();
                }
            } catch (URISyntaxException | RuntimeException e) {
                // try the next
            }
        }
        return new File("lernpfad");
    }

    // ------------------------------------------------------------------ the two paths

    /** the path config/filius.ini prescribes (modern-lernpfad-auswahl), null if students choose */
    static Pfad vorgegeben() {
        Pfad ini = Pfad.aus(Prefs.lernpfadAuswahl());
        if (ini != null) {
            return ini;
        }
        // an installation without the standard path (folder lernpfad/standard) has only the advanced one
        return zweiPfade() ? null : Pfad.FORTGESCHRITTEN;
    }

    /** true if this installation has both learning paths (folder lernpfad/standard) */
    public static boolean zweiPfade() {
        return new File(Pfad.STANDARD.ordner(), "kapitel-01.txt").isFile();
    }

    /** true if the students can choose between the two paths */
    public static boolean auswahlMoeglich() {
        return vorgegeben() == null;
    }

    /** the chosen path; the advanced one stands in while nothing is chosen */
    public static Pfad pfad() {
        return pfad != null ? pfad : Pfad.FORTGESCHRITTEN;
    }

    /** true once a path is chosen (or prescribed) */
    public static boolean gewaehlt() {
        return pfad != null;
    }

    /** chooses a path; its progress continues where it stopped */
    static void waehlen(Pfad p) {
        if (p == null || vorgegeben() != null && p != vorgegeben()) {
            return;
        }
        pfad = p;
        ereignisse.leeren();
        seit.clear();
        praxisPruefungAb = System.currentTimeMillis();
        gespeichertMarkieren();
        geaendert();
    }

    /** progress of a path (for the choice of the paths) */
    static Fortschritt stand(Pfad p) {
        return staende.computeIfAbsent(p, Fortschritt::new);
    }

    /** the rule of a task of the current path */
    public static Regel regel(String id) {
        return pfad().regel(id);
    }

    /**
     * Folder with the texts, pictures and start networks in the language Filius runs in:
     * lernpfad/en for English, lernpfad for German; for the standard path lernpfad/standard/en and lernpfad/standard.
     */
    static File sprachOrdner() {
        File basis = pfad().ordner();
        if (Texts.englisch()) {
            File en = new File(basis, "en");
            if (en.isDirectory()) {
                return en;
            }
        }
        return basis;
    }

    /** A file of the learning path in the language of Filius; the German one if it is missing. */
    private static File datei(String unterordner, String name) {
        File basis = pfad().ordner();
        File sprache = sprachOrdner();
        File f = unterordner == null ? new File(sprache, name) : new File(new File(sprache, unterordner), name);
        if (f.isFile() || sprache.equals(basis)) {
            return f;
        }
        return unterordner == null ? new File(basis, name) : new File(new File(basis, unterordner), name);
    }

    public static synchronized List<Kapitel> kapitel() {
        List<Kapitel> vorhanden = kapitel.get(pfad());
        if (vorhanden == null) {
            List<Kapitel> list = new ArrayList<>();
            for (int i = 1; i <= pfad().kapitel; i++) {
                File f = datei(null, String.format("kapitel-%02d.txt", i));
                try {
                    list.add(KapitelParser.lese(f.toPath(), i));
                } catch (Exception e) {
                    Kapitel k = new Kapitel();
                    k.nummer = i;
                    k.titel = Texts.format("lp.chapter", i);
                    k.ziel = Texts.format("lp.missing.file", f.getName());
                    list.add(k);
                }
            }
            kapitel.put(pfad(), list);
            vorhanden = list;
        }
        return vorhanden;
    }

    public static Kapitel kapitel(int nr) {
        return nr >= 1 && nr <= kapitel().size() ? kapitel().get(nr - 1) : null;
    }

    static File bild(String name) {
        return datei("bilder", name);
    }

    /** dark variant of a picture: name_dunkel.png next to name.png (null if the name has no extension) */
    static File bildDunkel(String name) {
        int punkt = name.lastIndexOf('.');
        return punkt <= 0 ? null : datei("bilder", name.substring(0, punkt) + "_dunkel" + name.substring(punkt));
    }

    static File startDatei(int nr, int variante) {
        String suffix = nr == pfad().variantenKapitel() ? String.valueOf((char) ('a' + Math.max(0, variante - 1))) : "";
        return datei("start", String.format("kapitel-%02d%s.fls", nr, suffix));
    }

    public static Fortschritt fortschritt() {
        return staende.computeIfAbsent(pfad(), Fortschritt::new);
    }

    public static Ereignisse ereignisse() {
        return ereignisse;
    }

    /** Tasks of a chapter, for the final project only those of the chosen variant. */
    static List<Aufgabe> aufgaben(int nr) {
        Kapitel k = kapitel(nr);
        if (k == null) {
            return Collections.emptyList();
        }
        int v = fortschritt().stand(nr).variante;
        return k.hatVarianten() ? k.aufgabenFuer(v == 0 ? 1 : v) : k.aufgaben;
    }

    /**
     * The number shown for a task. The final project shows only the faults of its variant (advanced: eight
     * of nineteen, standard: three of nine); they are shown as A1, A2, ... in the order of the chapter,
     * whatever faults they are (A7 is then for example "A3"). Other tasks keep their number.
     */
    static String anzeige(int nr, String id) {
        Kapitel k = kapitel(nr);
        Aufgabe a = k == null ? null : k.aufgabe(id);
        if (a == null || a.varianten.isEmpty()) {
            return id;
        }
        String praefix = id.replaceAll("\\d.*$", "");
        int n = 0;
        for (Aufgabe b : aufgaben(nr)) {
            if (!b.varianten.isEmpty() && b.id.replaceAll("\\d.*$", "").equals(praefix)) {
                n++;
                if (b.id.equals(id)) {
                    return praefix + n;
                }
            }
        }
        return id;
    }

    private static List<String> anzeige(int nr, List<String> ids) {
        List<String> l = new ArrayList<>();
        for (String id : ids) {
            l.add(anzeige(nr, id));
        }
        return l;
    }

    /** Group of tasks that is unlocked: all tasks of earlier groups are done. */
    static boolean gruppeOffen(int nr, int gruppe) {
        if (gruppe <= 0) {
            return true;
        }
        Fortschritt.Stand s = fortschritt().stand(nr);
        for (Aufgabe a : aufgaben(nr)) {
            if (a.gruppe < gruppe && !s.erledigt.contains(a.id)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ setup

    /** Called once the main window is open. */
    public static void install(JMainFrame frame) {
        // exam edition: no panel, no timer, nothing is read from the lernpfad folder or from project files
        if (Klausur.AKTIV || timer != null) {
            return;
        }
        installiert = System.currentTimeMillis();
        panel = new LernpfadPanel();
        panel.setVisible(false);
        try {
            Container cp = frame.getContentPane();
            if (cp.getLayout() instanceof BorderLayout) {
                Component east = ((BorderLayout) cp.getLayout()).getLayoutComponent(BorderLayout.EAST);
                JPanel dock = new JPanel(new BorderLayout());
                dock.setOpaque(false);
                dock.add(panel, BorderLayout.CENTER);
                if (east != null) {
                    cp.remove(east);
                    dock.add(east, BorderLayout.EAST);
                }
                cp.add(dock, BorderLayout.EAST);
                cp.validate();
            }
        } catch (RuntimeException e) {
            // without the dock the learning path cannot be shown, Filius keeps working
        }
        timer = new Timer(500, e -> tick());
        timer.start();
    }

    public static boolean sichtbar() {
        return panel != null && panel.isVisible();
    }

    public static void umschalten() {
        if (Klausur.AKTIV || panel == null) {
            return;
        }
        panel.setVisible(!panel.isVisible());
        if (panel.isVisible()) {
            panel.zeigeStart();
        }
        Container p = panel.getParent();
        if (p != null) {
            p.revalidate();
            p.getParent().validate();
            p.repaint();
        }
        for (Runnable r : hoerer) {
            r.run();
        }
    }

    /** Opens the learning path where the student stopped (after loading a project with progress). */
    static void zeigen() {
        if (Klausur.AKTIV || panel == null) {
            return;
        }
        if (panel.isVisible()) {
            panel.zeigeStart();
        } else {
            umschalten();
        }
    }

    /** The learning path was started in this project: some progress or a chapter that is checked. */
    public static boolean begonnen() {
        if (Klausur.AKTIV) {
            return false;
        }
        Fortschritt f = fortschritt();
        return gewaehlt() && (f.aktiv > 0 || !f.leer());
    }

    public static void addListener(Runnable r) {
        hoerer.add(r);
    }

    static void geaendert() {
        for (Runnable r : hoerer) {
            r.run();
        }
        if (panel != null) {
            panel.aktualisieren();
        }
    }

    /** Progress changed: Filius treats the project as changed, so it asks to save before closing. */
    static void gespeichertMarkieren() {
        try {
            SzenarioVerwaltung.getInstance().setzeGeaendert();
        } catch (RuntimeException e) {
            // ignore
        }
    }

    // ------------------------------------------------------------------ hooks from Filius

    /** A command typed into a command line. */
    public static void befehl(Knoten k, String befehl, List<String> parameter) {
        if (Klausur.AKTIV) {
            return;
        }
        Ereignis e = new Ereignis(Ereignis.Art.BEFEHL, k, System.currentTimeMillis());
        e.name = befehl == null ? "" : befehl.trim().toLowerCase();
        if (parameter != null) {
            e.werte.addAll(parameter);
        }
        // how the device was set up at that moment (for experiments: ping without gateway or cable)
        try {
            e.quelle = Netz.ip(k);
            e.typ = Netz.gateway(k);
            e.port = new Netz().nachbarn(k).size();
        } catch (RuntimeException ex) {
            // not known
        }
        ereignisse.add(e);
    }

    /** The data exchange window of a device was opened or its tab selected. */
    public static void datenaustausch(SystemSoftware s) {
        if (Klausur.AKTIV) {
            return;
        }
        if (s != null && s.getKnoten() != null) {
            beobachter.fenster(Ereignis.Art.DATENAUSTAUSCH, s.getKnoten(), System.currentTimeMillis());
        }
    }

    /** A project was opened: its progress is used; a file without progress keeps the current one. */
    public static void projektGeladen(String datei) {
        if (Klausur.AKTIV) {
            // the progress stored in the file is not even read
            return;
        }
        Properties p = ProjektSpeicher.lesen(datei);
        startGelesen = true;
        if (p != null) {
            uebernehmen(p);
            if (begonnen()) {
                spaeter(Lernpfad::zeigen);
            }
        } else {
            ereignisse.leeren();
            seit.clear();
            praxisPruefungAb = System.currentTimeMillis();
            geaendert();
        }
    }

    /** New project: the learning path starts from the beginning, progress belongs to its project file. */
    public static void projektNeu() {
        if (Klausur.AKTIV) {
            return;
        }
        startGelesen = true;
        // a new project starts the chosen path from the beginning
        Pfad gewaehlt = pfad;
        uebernehmen((Properties) null);
        pfad = gewaehlt != null ? gewaehlt : vorgegeben();
        geaendert();
        if (panel != null && panel.isVisible()) {
            panel.zeigeStart();
        }
    }

    /** Starts the current learning path from the beginning (button in the overview); the other path keeps its progress. */
    static void zuruecksetzen() {
        staende.put(pfad(), new Fortschritt(pfad()));
        ereignisse.leeren();
        seit.clear();
        praxisPruefungAb = System.currentTimeMillis();
        geaendert();
        gespeichertMarkieren();
    }

    public static void projektGespeichert(String datei) {
        if (Klausur.AKTIV) {
            return;
        }
        ProjektSpeicher.schreiben(datei, alsProperties());
    }

    /** progress of both paths and the chosen path, as stored in the project file */
    static Properties alsProperties() {
        Properties p = staende.computeIfAbsent(Pfad.FORTGESCHRITTEN, Fortschritt::new).alsProperties();
        p.putAll(staende.computeIfAbsent(Pfad.STANDARD, Fortschritt::new).alsProperties(Pfad.STANDARD.kennung + "."));
        if (pfad != null) {
            p.setProperty("pfad", pfad.kennung);
        }
        return p;
    }

    /** takes over the progress of a project file (null: everything from the beginning) */
    private static void uebernehmen(Properties p) {
        staende.clear();
        for (Pfad x : Pfad.values()) {
            staende.put(x, Fortschritt.aus(p, x));
        }
        Pfad gespeichert = p == null ? null : Pfad.aus(p.getProperty("pfad"));
        if (gespeichert == null) {
            // files from before the two paths: their progress belongs to the advanced path
            Fortschritt alt = staende.get(Pfad.FORTGESCHRITTEN);
            if (alt.aktiv > 0 || !alt.leer()) {
                gespeichert = Pfad.FORTGESCHRITTEN;
            }
        }
        pfad = vorgegeben() != null ? vorgegeben() : gespeichert;
        ereignisse.leeren();
        seit.clear();
        praxisPruefungAb = System.currentTimeMillis();
        geaendert();
    }

    /** Filius starts empty or with the project file it was started with; its progress is read once. */
    private static void startFortschritt() {
        startGelesen = true;
        Properties p = ProjektSpeicher.lesen(SzenarioVerwaltung.getInstance().holePfad());
        uebernehmen(p);
        if (begonnen()) {
            zeigen();
        }
    }

    // ------------------------------------------------------------------ actions of the panel

    static void seiteGelesen(int nr, int seite) {
        Fortschritt.Stand s = fortschritt().stand(nr);
        int anzahl = kapitel(nr).seiten.size();
        boolean neu = false;
        if (seite > s.seite) {
            s.seite = seite;
            neu = true;
        }
        if (seite >= anzahl - 1 && !s.gelesen) {
            s.gelesen = true;
            neu = true;
            seit.clear();
            praxisPruefungAb = System.currentTimeMillis();
        }
        if (neu) {
            gespeichertMarkieren();
        }
    }

    enum Ergebnis {
        RICHTIG, FALSCH, RICHTIG_NOCH_OFFEN
    }

    /** Answer to the question of a task. Wrong answers are not stored. */
    static Ergebnis antworten(int nr, Aufgabe a, String antwort) {
        Fortschritt.Stand s = fortschritt().stand(nr);
        boolean richtig;
        if (a.hatAuswahl()) {
            richtig = String.valueOf(a.richtig).equals(antwort);
        } else {
            Regel r = regel(a.id);
            richtig = r == null || r.antwortOk(new Netz(), ereignisse, antwort);
        }
        if (!richtig) {
            s.antworten.remove(a.id);
            gespeichertMarkieren();
            return Ergebnis.FALSCH;
        }
        s.antworten.put(a.id, antwort);
        gespeichertMarkieren();
        pruefen();
        return s.erledigt.contains(a.id) ? Ergebnis.RICHTIG : Ergebnis.RICHTIG_NOCH_OFFEN;
    }

    /** Makes a chapter the one whose tasks are checked, optionally with its start network. */
    static void bearbeiten(int nr, boolean startaufbau) {
        Fortschritt.Stand s = fortschritt().stand(nr);
        if (nr == pfad().variantenKapitel() && (startaufbau || s.variante == 0)) {
            s.variante = 1 + zufall.nextInt(3);
            startaufbau = true;
        }
        if (startaufbau) {
            File f = startDatei(nr, s.variante);
            if (!f.isFile()) {
                JOptionPane.showMessageDialog(JMainFrame.getJMainFrame(), Texts.format("lp.missing.start", f.getName()),
                        Texts.get("lernpfad"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!filius.gui.GUIMainMenu.vorlageLaden(f.getAbsolutePath())) {
                return;
            }
            s.praxisZuruecksetzen();
        } else if (s.fertig) {
            s.praxisZuruecksetzen();
        }
        fortschritt().aktiv = nr;
        ereignisse.leeren();
        seit.clear();
        praxisPruefungAb = System.currentTimeMillis();
        gespeichertMarkieren();
        geaendert();
    }

    static void quizBeginnen(int nr) {
        Fortschritt.Stand s = fortschritt().stand(nr);
        List<Integer> folge = new ArrayList<>();
        for (int i = 0; i < kapitel(nr).quiz.size(); i++) {
            folge.add(i);
        }
        Collections.shuffle(folge, zufall);
        s.quizOffen = folge;
        s.quizFehler = 0;
        gespeichertMarkieren();
    }

    /** Answer to the first open quiz question; a wrong one is asked again at the end. */
    static boolean quizAntwort(int nr, int frage, int option) {
        Fortschritt.Stand s = fortschritt().stand(nr);
        Kapitel.Frage f = kapitel(nr).quiz.get(frage);
        boolean richtig = option == f.richtig;
        if (s.quizOffen != null && !s.quizOffen.isEmpty() && s.quizOffen.get(0) == frage) {
            s.quizOffen.remove(0);
            if (!richtig) {
                s.quizOffen.add(frage);
                s.quizFehler++;
            }
        }
        if (s.quizOffen != null && s.quizOffen.isEmpty() && !s.fertig) {
            s.fertig = true;
            feiern(Texts.format("lp.toast.chapter", nr));
        }
        gespeichertMarkieren();
        return richtig;
    }

    static void feiern(String text) {
        Overlay o = Overlay.get();
        if (o != null) {
            o.toast(text, Theme.LED_GREEN);
            o.celebrate();
        }
        for (Runnable r : hoerer) {
            r.run();
        }
    }

    // ------------------------------------------------------------------ checking

    private static int modus() {
        try {
            return GUIContainer.getGUIContainer().getActiveSite();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static void tick() {
        int modus = modus();
        if (!startGelesen && System.currentTimeMillis() - installiert > 1500) {
            startFortschritt();
        }
        try {
            beobachter.tick(modus == Theme.MODE_SIMULATION);
        } catch (RuntimeException e) {
            // never disturb Filius
        }
        Overlay o = Overlay.get();
        if (o != null) {
            try {
                o.setHintWanted(GUIContainer.getGUIContainer().getKnotenItems().isEmpty() && modus == Theme.MODE_DESIGN && !sichtbar());
            } catch (RuntimeException e) {
                // ignore
            }
        }
        letzterModus = modus;
        pruefen();
    }

    /** Checks the open tasks of the active chapter. */
    static void pruefen() {
        int nr = fortschritt().aktiv;
        Kapitel k = kapitel(nr);
        if (k == null) {
            return;
        }
        Fortschritt.Stand s = fortschritt().stand(nr);
        if (!(s.gelesen || s.fertig) || s.praxisFertig && !s.fertig) {
            return;
        }
        long jetzt = System.currentTimeMillis();
        Netz netz = new Netz();
        List<String> neu = new ArrayList<>();
        for (Aufgabe a : aufgaben(nr)) {
            if (s.erledigt.contains(a.id) || !gruppeOffen(nr, a.gruppe)) {
                continue;
            }
            Regel r = regel(a.id);
            if (r == null) {
                continue;
            }
            String key = nr + "|" + a.id;
            if (!r.zustandOk(netz)) {
                seit.put(key, jetzt);
                continue;
            }
            long ab = Math.max(seit.getOrDefault(key, praxisPruefungAb), praxisPruefungAb);
            if (!r.nachweisOk(netz, ereignisse, ab)) {
                continue;
            }
            if (a.hatFrage()) {
                String antwort = s.antworten.get(a.id);
                boolean ok = a.hatAuswahl() ? String.valueOf(a.richtig).equals(antwort) : r.antwortOk(netz, ereignisse, antwort);
                if (!ok) {
                    continue;
                }
            }
            s.erledigt.add(a.id);
            neu.add(a.id);
        }
        if (neu.isEmpty()) {
            return;
        }
        boolean alle = true;
        for (Aufgabe a : aufgaben(nr)) {
            alle &= s.erledigt.contains(a.id);
        }
        if (alle && !s.praxisFertig) {
            List<String> nichtMehr = endkontrolle(nr, netz);
            if (nichtMehr.isEmpty()) {
                s.praxisFertig = true;
                if (s.quizOffen == null || s.fertig) {
                    quizBeginnen(nr);
                }
                feiern(Texts.get("lp.toast.practice"));
            } else {
                s.erledigt.removeAll(nichtMehr);
                Overlay o = Overlay.get();
                if (o != null) {
                    o.toast(Texts.format("lp.toast.notanymore", String.join(", ", anzeige(nr, nichtMehr))), Theme.MARKER_AMBER);
                }
            }
        } else {
            feiern(neu.size() == 1 ? Texts.format("lp.toast.task", anzeige(nr, neu.get(0))) : Texts.format("lp.toast.tasks", String.join(", ", anzeige(nr, neu))));
        }
        gespeichertMarkieren();
        geaendert();
    }

    /** At the end every task is checked again: what was right must still be right. */
    private static List<String> endkontrolle(int nr, Netz netz) {
        List<String> falsch = new ArrayList<>();
        for (Aufgabe a : aufgaben(nr)) {
            Regel r = regel(a.id);
            if (r == null || r.uebergang) {
                continue;
            }
            if (!r.zustandOk(netz)) {
                falsch.add(a.id);
            }
        }
        return falsch;
    }

    static int letzterModus() {
        return letzterModus;
    }

    static JComponent panel() {
        return panel;
    }

    static void spaeter(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

}
