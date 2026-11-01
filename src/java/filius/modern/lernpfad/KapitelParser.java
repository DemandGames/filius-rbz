package filius.modern.lernpfad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import filius.modern.lernpfad.Kapitel.Aufgabe;
import filius.modern.lernpfad.Kapitel.Begriff;
import filius.modern.lernpfad.Kapitel.Block;
import filius.modern.lernpfad.Kapitel.BlockArt;
import filius.modern.lernpfad.Kapitel.Frage;
import filius.modern.lernpfad.Kapitel.Gruppe;
import filius.modern.lernpfad.Kapitel.Seite;

/**
 * Reads a chapter file. The format is plain text with a few markers, so teachers can edit the
 * texts with any editor:
 *
 * <pre>
 * # Title
 * Ziel: ...
 * ## Lesen      ### page title, paragraphs, "- " lists, "1. " numbered lists, "| a | b |" tables, [Bild: file.png | caption],
 *               "> Merke: ..."
 * ## Praxis     ### 1.2 task text (paragraphs, "- " lists and "| a | b |" tables; the first table row is the
 *               header), Tipp:, Frage:, Eingabe: label or "- ✓ right" / "- wrong" options after Frage:
 * ## Quiz       ### question, four "- " options (✓ marks the right one), each wrong one followed by
 *               "Falsch: why it is wrong", then "Erklärung: why the right one is right"
 * ## Glossar    - **Term** – explanation
 * </pre>
 */
public final class KapitelParser {

    private static final Pattern AUFGABE_ID = Pattern.compile("[A-Z]?\\d+(\\.\\d+)?");
    /** a step of a numbered list in the reading text: "1. ClientHello: …" */
    private static final Pattern NUMMER = Pattern.compile("\\d{1,2}\\.\\s+(.+)");
    private static final Pattern BILD = Pattern.compile("\\[(?:Bild|Picture):\\s*([^|\\]]+?)\\s*(?:\\|\\s*([^\\]]*?))?\\s*\\]");
    private static final Pattern BEGRIFF = Pattern.compile("-\\s*\\*\\*(.+?)\\*\\*\\s*[–—-]\\s*(.+)");
    private static final String HAKEN = "✓";

    private enum Teil {
        KOPF, LESEN, PRAXIS, QUIZ, GLOSSAR
    }

    private KapitelParser() {}

    public static Kapitel lese(Path datei, int nummer) throws IOException {
        return parse(Files.readAllLines(datei, StandardCharsets.UTF_8), nummer);
    }

    public static Kapitel parse(List<String> zeilen, int nummer) {
        Kapitel k = new Kapitel();
        k.nummer = nummer;
        Teil teil = Teil.KOPF;
        Seite seite = null;
        Block absatz = null;
        Block liste = null;
        Block tabelle = null;
        Aufgabe aufgabe = null;
        Gruppe gruppe = null;
        Frage frage = null;

        for (String roh : zeilen) {
            String z = roh.strip();
            if (z.startsWith("﻿")) {
                z = z.substring(1);
            }
            if (z.startsWith("## ")) {
                String name = z.substring(3).trim().toLowerCase();
                teil = name.startsWith("lesen") || name.startsWith("read") ? Teil.LESEN
                        : name.startsWith("praxis") || name.startsWith("practice") ? Teil.PRAXIS
                                : name.startsWith("quiz") ? Teil.QUIZ
                                        : name.startsWith("glossar") || name.startsWith("glossary") ? Teil.GLOSSAR : teil;
                absatz = null;
                liste = null;
                tabelle = null;
                continue;
            }
            if (z.startsWith("# ") && teil == Teil.KOPF) {
                k.titel = z.substring(2).trim();
                continue;
            }
            switch (teil) {
            case KOPF:
                if (z.startsWith("Ziel:") || z.startsWith("Goal:")) {
                    k.ziel = nach(z);
                }
                break;

            case LESEN:
                if (z.startsWith("### ")) {
                    seite = new Seite();
                    seite.titel = z.substring(4).trim();
                    k.seiten.add(seite);
                    absatz = null;
                    liste = null;
                    break;
                }
                if (seite == null) {
                    break;
                }
                if (z.isEmpty()) {
                    absatz = null;
                    liste = null;
                    tabelle = null;
                    break;
                }
                Matcher bild = BILD.matcher(z);
                if (bild.matches()) {
                    Block b = new Block();
                    b.art = BlockArt.BILD;
                    b.text = bild.group(1).trim();
                    b.unterschrift = bild.group(2) == null ? "" : bild.group(2).trim();
                    seite.bloecke.add(b);
                    absatz = null;
                    liste = null;
                    break;
                }
                if (z.startsWith(">")) {
                    String text = z.substring(1).trim();
                    Block b = new Block();
                    if (text.startsWith("Tipp:") || text.startsWith("Tip:")) {
                        b.art = BlockArt.TIPP;
                        b.text = nach(text);
                    } else {
                        b.art = BlockArt.MERKE;
                        b.text = text.startsWith("Merke:") || text.startsWith("Note:") ? nach(text) : text;
                    }
                    seite.bloecke.add(b);
                    absatz = null;
                    liste = null;
                    break;
                }
                if (z.startsWith("|")) {
                    tabelle = zeile(seite.bloecke, tabelle, z);
                    absatz = null;
                    liste = null;
                    break;
                }
                tabelle = null;
                Matcher schritt = NUMMER.matcher(z);
                if (z.startsWith("- ") || schritt.matches()) {
                    boolean nummeriert = !z.startsWith("- ");
                    if (liste == null || liste.nummeriert != nummeriert) {
                        liste = new Block();
                        liste.art = BlockArt.LISTE;
                        liste.nummeriert = nummeriert;
                        seite.bloecke.add(liste);
                    }
                    liste.punkte.add(nummeriert ? schritt.group(1).trim() : z.substring(2).trim());
                    absatz = null;
                    break;
                }
                if (absatz == null) {
                    absatz = new Block();
                    absatz.art = BlockArt.ABSATZ;
                    seite.bloecke.add(absatz);
                    absatz.text = z;
                } else {
                    absatz.text = absatz.text + " " + z;
                }
                liste = null;
                break;

            case PRAXIS:
                if (z.startsWith("### ")) {
                    absatz = null;
                    liste = null;
                    tabelle = null;
                    String kopf = z.substring(4).trim();
                    if (AUFGABE_ID.matcher(kopf).matches()) {
                        aufgabe = new Aufgabe();
                        aufgabe.id = kopf;
                        aufgabe.gruppe = k.gruppen.size() - 1;
                        k.aufgaben.add(aufgabe);
                        gruppe = null;
                    } else {
                        gruppe = new Gruppe();
                        gruppe.titel = kopf;
                        gruppe.start = k.aufgaben.size();
                        k.gruppen.add(gruppe);
                        aufgabe = null;
                    }
                    break;
                }
                if (z.isEmpty()) {
                    absatz = null;
                    liste = null;
                    tabelle = null;
                    break;
                }
                if (gruppe != null) {
                    gruppe.text = gruppe.text.isEmpty() ? z : gruppe.text + " " + z;
                    break;
                }
                if (aufgabe == null) {
                    break;
                }
                boolean schluessel = z.startsWith("Tipp:") || z.startsWith("Tip:") || z.startsWith("Frage:") || z.startsWith("Question:")
                        || z.startsWith("Eingabe:") || z.startsWith("Input:") || z.startsWith("Ursache:") || z.startsWith("Cause:")
                        || z.startsWith("Variante:") || z.startsWith("Varianten:") || z.startsWith("Variant:") || z.startsWith("Variants:");
                boolean auswahl = z.startsWith("- ") && !aufgabe.frage.isEmpty();
                if (schluessel || auswahl) {
                    absatz = null;
                    liste = null;
                    tabelle = null;
                }
                if (!schluessel && !auswahl) {
                    // the task text: paragraphs, lists and tables
                    if (z.startsWith("|")) {
                        tabelle = zeile(aufgabe.bloecke, tabelle, z);
                        absatz = null;
                        liste = null;
                    } else if (z.startsWith("- ")) {
                        if (liste == null) {
                            liste = new Block();
                            liste.art = BlockArt.LISTE;
                            aufgabe.bloecke.add(liste);
                        }
                        liste.punkte.add(z.substring(2).trim());
                        absatz = null;
                        tabelle = null;
                    } else {
                        if (absatz == null) {
                            absatz = new Block();
                            absatz.art = BlockArt.ABSATZ;
                            aufgabe.bloecke.add(absatz);
                            absatz.text = z;
                        } else {
                            absatz.text = absatz.text + " " + z;
                        }
                        liste = null;
                        tabelle = null;
                    }
                    String klar = z.startsWith("|") ? String.join(" ", zellen(z)) : z.startsWith("- ") ? z.substring(2).trim() : z;
                    if (!klar.isEmpty()) {
                        aufgabe.text = aufgabe.text.isEmpty() ? klar : aufgabe.text + " " + klar;
                    }
                } else if (z.startsWith("Tipp:") || z.startsWith("Tip:")) {
                    aufgabe.tipp = nach(z);
                } else if (z.startsWith("Frage:") || z.startsWith("Question:")) {
                    aufgabe.frage = nach(z);
                } else if (z.startsWith("Eingabe:") || z.startsWith("Input:")) {
                    aufgabe.eingabe = nach(z);
                } else if (z.startsWith("Ursache:") || z.startsWith("Cause:")) {
                    aufgabe.ursache = nach(z);
                } else if (z.startsWith("Variante:") || z.startsWith("Varianten:") || z.startsWith("Variant:") || z.startsWith("Variants:")) {
                    for (String v : z.substring(z.indexOf(':') + 1).split("[,\\s]+")) {
                        if (!v.isBlank()) {
                            aufgabe.varianten.add(Integer.parseInt(v.trim()));
                        }
                    }
                } else if (z.startsWith("- ")) {
                    String option = z.substring(2).trim();
                    if (option.startsWith(HAKEN)) {
                        aufgabe.richtig = aufgabe.optionen.size();
                        option = option.substring(HAKEN.length()).trim();
                    }
                    aufgabe.optionen.add(option);
                }
                break;

            case QUIZ:
                if (z.startsWith("### ")) {
                    frage = new Frage();
                    frage.text = z.substring(4).trim();
                    k.quiz.add(frage);
                    break;
                }
                if (frage == null || z.isEmpty()) {
                    break;
                }
                if (z.startsWith("- ")) {
                    String option = z.substring(2).trim();
                    if (option.startsWith(HAKEN)) {
                        frage.richtig = frage.optionen.size();
                        option = option.substring(HAKEN.length()).trim();
                    }
                    frage.optionen.add(option);
                    frage.falsch.add("");
                } else if ((z.startsWith("Falsch:") || z.startsWith("Wrong:")) && !frage.falsch.isEmpty()) {
                    frage.falsch.set(frage.falsch.size() - 1, nach(z));
                } else if (z.startsWith("Erklärung:") || z.startsWith("Explanation:")) {
                    frage.erklaerung = nach(z);
                } else {
                    frage.text = frage.text + " " + z;
                }
                break;

            case GLOSSAR:
                Matcher m = BEGRIFF.matcher(z);
                if (m.matches()) {
                    Begriff b = new Begriff();
                    b.name = m.group(1).trim();
                    b.erklaerung = m.group(2).trim();
                    k.glossar.add(b);
                }
                break;
            }
        }
        return k;
    }

    /** Adds a table row ("| a | b |"); a row of dashes under the header is skipped. Returns the table. */
    private static Block zeile(List<Block> bloecke, Block tabelle, String z) {
        List<String> zellen = zellen(z);
        if (tabelle == null) {
            tabelle = new Block();
            tabelle.art = BlockArt.TABELLE;
            bloecke.add(tabelle);
        }
        boolean trenner = !zellen.isEmpty() && zellen.stream().allMatch(c -> c.matches(":?-{2,}:?"));
        if (!trenner && !zellen.isEmpty()) {
            tabelle.zeilen.add(zellen);
        }
        return tabelle;
    }

    private static List<String> zellen(String z) {
        String innen = z.trim();
        if (innen.startsWith("|")) {
            innen = innen.substring(1);
        }
        if (innen.endsWith("|")) {
            innen = innen.substring(0, innen.length() - 1);
        }
        List<String> l = new java.util.ArrayList<>();
        for (String c : innen.split("\\|", -1)) {
            l.add(c.trim());
        }
        return l;
    }

    /** the text after the keyword and its colon */
    private static String nach(String zeile) {
        return zeile.substring(zeile.indexOf(':') + 1).trim();
    }
}
