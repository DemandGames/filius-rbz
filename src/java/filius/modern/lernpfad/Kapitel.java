package filius.modern.lernpfad;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One chapter of the learning path as read from {@code lernpfad/kapitel-NN.txt}: reading pages,
 * practice tasks, quiz questions and glossary terms.
 */
public final class Kapitel {

    public int nummer;
    public String titel = "";
    public String ziel = "";
    public final List<Seite> seiten = new ArrayList<>();
    public final List<Aufgabe> aufgaben = new ArrayList<>();
    public final List<Gruppe> gruppen = new ArrayList<>();
    public final List<Frage> quiz = new ArrayList<>();
    public final List<Begriff> glossar = new ArrayList<>();

    public Aufgabe aufgabe(String id) {
        for (Aufgabe a : aufgaben) {
            if (a.id.equals(id)) {
                return a;
            }
        }
        return null;
    }

    /** Tasks that belong to the given variant (chapter 10); all tasks for other chapters. */
    public List<Aufgabe> aufgabenFuer(int variante) {
        List<Aufgabe> list = new ArrayList<>();
        for (Aufgabe a : aufgaben) {
            if (a.varianten.isEmpty() || a.varianten.contains(variante)) {
                list.add(a);
            }
        }
        return list;
    }

    public boolean hatVarianten() {
        for (Aufgabe a : aufgaben) {
            if (!a.varianten.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ parts

    public static final class Seite {
        public String titel = "";
        public final List<Block> bloecke = new ArrayList<>();
    }

    public enum BlockArt {
        ABSATZ, LISTE, TABELLE, BILD, MERKE, TIPP
    }

    public static final class Block {
        public BlockArt art;
        /** text of a paragraph or box; file name of a picture */
        public String text = "";
        /** caption of a picture */
        public String unterschrift = "";
        /** items of a list */
        public final List<String> punkte = new ArrayList<>();
        /** a numbered list ("1. ...", "2. ..."): steps in their order (version 31) */
        public boolean nummeriert;
        /** rows of a table, the first one is the header */
        public final List<List<String>> zeilen = new ArrayList<>();
    }

    /** Heading inside the practice part, e.g. "Teil A – Störungen beheben" with an intro text. */
    public static final class Gruppe {
        public String titel = "";
        public String text = "";
        /** index of the first task of this group */
        public int start;
    }

    public static final class Aufgabe {
        public String id = "";
        /** the whole task text in one line (paragraphs, list items and table cells) */
        public String text = "";
        /** the task text as paragraphs, lists and tables */
        public final List<Block> bloecke = new ArrayList<>();
        public String tipp = "";
        public String frage = "";
        /** label of a free-text answer, empty if the answer is a choice or there is no question */
        public String eingabe = "";
        public final List<String> optionen = new ArrayList<>();
        public int richtig = -1;
        /** chapter 10: variants this task belongs to (empty = always) */
        public final Set<Integer> varianten = new LinkedHashSet<>();
        /** chapter 10: cause that is shown once the task is done */
        public String ursache = "";
        /** index into {@link Kapitel#gruppen}, -1 without groups */
        public int gruppe = -1;

        public boolean hatAuswahl() {
            return !optionen.isEmpty();
        }

        public boolean hatEingabe() {
            return !eingabe.isEmpty();
        }

        public boolean hatFrage() {
            return hatAuswahl() || hatEingabe();
        }
    }

    public static final class Frage {
        public String text = "";
        public final List<String> optionen = new ArrayList<>();
        /** for each option: why it is wrong ("Falsch:" line after it), empty for the right one */
        public final List<String> falsch = new ArrayList<>();
        public int richtig = -1;
        /** why the right option is right */
        public String erklaerung = "";

        public String warumFalsch(int option) {
            return option >= 0 && option < falsch.size() ? falsch.get(option) : "";
        }
    }

    public static final class Begriff {
        public String name = "";
        public String erklaerung = "";
    }
}
