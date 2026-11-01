package filius.modern.lernpfad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Progress of a student. It is stored inside the project file (.fls), so it travels with the
 * network the student builds: reading position, done tasks, given answers, quiz state.
 */
public final class Fortschritt {

    /**
     * 2 since version 9 of the learning path (12 October 2026): chapters 5 to 7 have a new order
     * (5 DNS, 6 DHCP, 7 home network). 3 since version 19 (17 October 2026): the advanced path has 12
     * chapters (new 4 HTTPS and 11 VPN, firewall moves from 8 to 10, the final project is chapter 12 and
     * new). Older progress is moved to the new numbers when it is read; the old final project is dropped.
     */
    static final int VERSION = 3;

    public static final class Stand {
        public int seite;
        public boolean gelesen;
        public final Set<String> erledigt = new LinkedHashSet<>();
        public final Map<String, String> antworten = new LinkedHashMap<>();
        public boolean praxisFertig;
        /** remaining quiz questions (indices) in the order they will be asked; null = not started */
        public List<Integer> quizOffen;
        public int quizFehler;
        public boolean fertig;
        /** final project: variant 1..3 of the faults, 0 = none chosen */
        public int variante;

        void praxisZuruecksetzen() {
            erledigt.clear();
            antworten.clear();
            praxisFertig = false;
        }
    }

    private final Map<Integer, Stand> kapitel = new TreeMap<>();
    /** chapter whose tasks are checked right now (0 = none) */
    public int aktiv;
    /** the learning path this progress belongs to */
    public final Pfad pfad;

    public Fortschritt() {
        this(Pfad.FORTGESCHRITTEN);
    }

    public Fortschritt(Pfad pfad) {
        this.pfad = pfad;
    }

    /** number of chapters of the path */
    public int kapitelAnzahl() {
        return pfad.kapitel;
    }

    public Stand stand(int nr) {
        return kapitel.computeIfAbsent(nr, k -> new Stand());
    }

    public boolean fertig(int nr) {
        return stand(nr).fertig;
    }

    public boolean freigeschaltet(int nr) {
        return nr <= 1 || fertig(nr - 1) || fertig(nr);
    }

    /** First chapter that is not finished yet; number of chapters + 1 when all are finished. */
    public int aktuell() {
        for (int i = 1; i <= kapitelAnzahl(); i++) {
            if (!fertig(i)) {
                return i;
            }
        }
        return kapitelAnzahl() + 1;
    }

    public int fertigeKapitel() {
        int n = 0;
        for (int i = 1; i <= kapitelAnzahl(); i++) {
            if (fertig(i)) {
                n++;
            }
        }
        return n;
    }

    public boolean leer() {
        for (Stand s : kapitel.values()) {
            if (s.seite > 0 || s.gelesen || !s.erledigt.isEmpty() || s.fertig || s.quizOffen != null) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ storage

    public Properties alsProperties() {
        Properties p = new Properties();
        p.setProperty("version", String.valueOf(VERSION));
        p.setProperty("aktiv", String.valueOf(aktiv));
        for (Map.Entry<Integer, Stand> e : kapitel.entrySet()) {
            String k = "k" + e.getKey() + ".";
            Stand s = e.getValue();
            p.setProperty(k + "seite", String.valueOf(s.seite));
            p.setProperty(k + "gelesen", String.valueOf(s.gelesen));
            p.setProperty(k + "erledigt", String.join(",", s.erledigt));
            for (Map.Entry<String, String> a : s.antworten.entrySet()) {
                p.setProperty(k + "antwort." + a.getKey(), a.getValue());
            }
            p.setProperty(k + "praxisFertig", String.valueOf(s.praxisFertig));
            if (s.quizOffen != null) {
                List<String> q = new ArrayList<>();
                for (Integer i : s.quizOffen) {
                    q.add(String.valueOf(i));
                }
                p.setProperty(k + "quizOffen", String.join(",", q));
            }
            p.setProperty(k + "quizFehler", String.valueOf(s.quizFehler));
            p.setProperty(k + "fertig", String.valueOf(s.fertig));
            p.setProperty(k + "variante", String.valueOf(s.variante));
        }
        return p;
    }

    public static Fortschritt aus(Properties p) {
        return aus(p, Pfad.FORTGESCHRITTEN);
    }

    /** progress of a path; the keys of the standard path have the prefix "standard." in the project file */
    public static Fortschritt aus(Properties alle, Pfad pfad) {
        Fortschritt f = new Fortschritt(pfad);
        Properties p = pfad == Pfad.FORTGESCHRITTEN ? alle : ohnePraefix(alle, pfad.kennung + ".");
        if (p == null || p.isEmpty()) {
            return f;
        }
        // old chapter numbers exist only in the advanced path
        int version = pfad == Pfad.FORTGESCHRITTEN ? zahl(p.getProperty("version"), 1) : VERSION;
        boolean alt = version < VERSION;
        int aktivAlt = zahl(p.getProperty("aktiv"), 0);
        f.aktiv = alt ? Math.max(0, neueNummer(aktivAlt, version)) : aktivAlt;
        for (int i = 1; i <= f.kapitelAnzahl(); i++) {
            String k = "k" + i + ".";
            if (p.getProperty(k + "seite") == null) {
                continue;
            }
            int nr = alt ? neueNummer(i, version) : i;
            if (nr <= 0) {
                continue;
            }
            Stand s = f.stand(nr);
            s.seite = zahl(p.getProperty(k + "seite"), 0);
            s.gelesen = Boolean.parseBoolean(p.getProperty(k + "gelesen"));
            for (String id : p.getProperty(k + "erledigt", "").split(",")) {
                if (!id.isBlank()) {
                    s.erledigt.add(alt ? neueAufgabe(id.trim(), version) : id.trim());
                }
            }
            for (String name : p.stringPropertyNames()) {
                if (name.startsWith(k + "antwort.")) {
                    String id = name.substring((k + "antwort.").length());
                    s.antworten.put(alt ? neueAufgabe(id, version) : id, p.getProperty(name));
                }
            }
            s.praxisFertig = Boolean.parseBoolean(p.getProperty(k + "praxisFertig"));
            String q = p.getProperty(k + "quizOffen");
            if (q != null) {
                s.quizOffen = new ArrayList<>();
                for (String t : q.split(",")) {
                    if (!t.isBlank()) {
                        s.quizOffen.add(zahl(t.trim(), 0));
                    }
                }
            }
            s.quizFehler = zahl(p.getProperty(k + "quizFehler"), 0);
            s.fertig = Boolean.parseBoolean(p.getProperty(k + "fertig"));
            s.variante = zahl(p.getProperty(k + "variante"), 0);
        }
        return f;
    }

    /** the keys with this prefix, without it */
    static Properties ohnePraefix(Properties p, String praefix) {
        if (p == null) {
            return null;
        }
        Properties r = new Properties();
        for (String name : p.stringPropertyNames()) {
            if (name.startsWith(praefix)) {
                r.setProperty(name.substring(praefix.length()), p.getProperty(name));
            }
        }
        return r;
    }

    /** the properties of this progress with a prefix before every key (standard path) */
    public Properties alsProperties(String praefix) {
        Properties p = alsProperties();
        if (praefix == null || praefix.isEmpty()) {
            return p;
        }
        Properties r = new Properties();
        for (String name : p.stringPropertyNames()) {
            r.setProperty(praefix + name, p.getProperty(name));
        }
        return r;
    }

    /** chapter number of version 1 (5 home network, 6 DNS, 7 DHCP) in the order of version 2 */
    static int neueNummer(int alt) {
        switch (alt) {
        case 5:
            return 7;
        case 6:
            return 5;
        case 7:
            return 6;
        default:
            return alt;
        }
    }

    /**
     * chapter number of version 2 in the order of version 3: 4 router -> 5, 5 DNS -> 6, 6 DHCP -> 7,
     * 7 home network -> 8, 8 firewall -> 10, 9 e-mail stays, 10 final project -> 0 (dropped, the new one is
     * different)
     */
    static int nummer3(int alt) {
        switch (alt) {
        case 4:
            return 5;
        case 5:
            return 6;
        case 6:
            return 7;
        case 7:
            return 8;
        case 8:
            return 10;
        case 10:
            return 0;
        default:
            return alt;
        }
    }

    /** chapter number of an older version in the order of this version (0 = dropped) */
    static int neueNummer(int alt, int version) {
        int nr = version < 2 ? neueNummer(alt) : alt;
        return version < 3 ? nummer3(nr) : nr;
    }

    /** task "6.3" of version 1 becomes "7.3" (version 2: "5.3", version 3: "6.3") */
    static String neueAufgabe(String id, int version) {
        int punkt = id.indexOf('.');
        if (punkt <= 0) {
            return id;
        }
        int nr = zahl(id.substring(0, punkt), -1);
        return nr < 0 ? id : neueNummer(nr, version) + id.substring(punkt);
    }

    private static int zahl(String s, int vorgabe) {
        try {
            return s == null ? vorgabe : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return vorgabe;
        }
    }
}
