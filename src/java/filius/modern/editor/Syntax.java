package filius.modern.editor;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Syntax colours for the text editor: HTML (with CSS in style elements and JavaScript in script elements),
 * CSS and JavaScript. The text is scanned as a whole; the result is one colour class per character. The
 * colours are those of the "Light+" theme of Visual Studio Code.
 */
public final class Syntax {

    private Syntax() {}

    public enum Sprache {
        TEXT, HTML, CSS, JS
    }

    // colour classes
    static final byte TEXT = 0;
    static final byte TAG = 1;
    static final byte ATTRIBUT = 2;
    static final byte WERT = 3;
    static final byte KOMMENTAR = 4;
    static final byte ENTITY = 5;
    static final byte SELEKTOR = 6;
    static final byte EIGENSCHAFT = 7;
    static final byte CSS_WERT = 8;
    static final byte ZAHL = 9;
    static final byte AT_REGEL = 10;
    static final byte STRING = 11;
    static final byte SCHLUESSELWORT = 12;
    static final byte KONTROLLE = 13;
    static final byte FUNKTION = 14;
    static final byte VARIABLE = 15;
    static final byte REGEX = 16;

    static final Color[] FARBEN = {
            filius.modern.Theme.farbe(0x1F2328, 0xD4D4D4), // text
            filius.modern.Theme.farbe(0x800000, 0x569CD6), // tag
            filius.modern.Theme.farbe(0xE50000, 0x9CDCFE), // attribute name
            filius.modern.Theme.farbe(0x0000FF, 0xCE9178), // attribute value
            filius.modern.Theme.farbe(0x008000, 0x6A9955), // comment
            filius.modern.Theme.farbe(0xEE0000, 0x569CD6), // entity
            filius.modern.Theme.farbe(0x800000, 0xD7BA7D), // CSS selector
            filius.modern.Theme.farbe(0xE50000, 0x9CDCFE), // CSS property
            filius.modern.Theme.farbe(0x0451A5, 0xCE9178), // CSS value
            filius.modern.Theme.farbe(0x098658, 0xB5CEA8), // number
            filius.modern.Theme.farbe(0xAF00DB, 0xC586C0), // at-rule
            filius.modern.Theme.farbe(0xA31515, 0xCE9178), // string
            filius.modern.Theme.farbe(0x0000FF, 0x569CD6), // keyword
            filius.modern.Theme.farbe(0xAF00DB, 0xC586C0), // control keyword
            filius.modern.Theme.farbe(0x795E26, 0xDCDCAA), // function
            filius.modern.Theme.farbe(0x001080, 0x9CDCFE), // variable / property
            filius.modern.Theme.farbe(0x811F3F, 0xD16969), // regular expression
    };

    private static final Set<String> SCHLUESSEL = new HashSet<>(Arrays.asList("var", "let", "const", "function", "class", "new",
            "typeof", "instanceof", "this", "true", "false", "null", "undefined", "in", "of", "void", "delete", "async", "static",
            "get", "set", "extends", "super", "NaN", "Infinity"));
    private static final Set<String> KONTROLLWORTE = new HashSet<>(Arrays.asList("if", "else", "for", "while", "do", "return",
            "break", "continue", "switch", "case", "default", "try", "catch", "finally", "throw", "await", "import", "export", "from",
            "yield", "with"));
    /** after these words a slash starts a regular expression */
    private static final Set<String> VOR_REGEX = new HashSet<>(Arrays.asList("return", "typeof", "case", "do", "else", "in", "of",
            "new", "delete", "void", "throw", "yield", "await"));

    /** language by file name; without a known ending by the content */
    public static Sprache sprache(String dateiname, String inhalt) {
        String n = dateiname == null ? "" : dateiname.toLowerCase(Locale.ROOT).trim();
        if (n.endsWith(".html") || n.endsWith(".htm") || n.endsWith(".xhtml") || n.endsWith(".svg") || n.endsWith(".xml")) {
            return Sprache.HTML;
        }
        if (n.endsWith(".css")) {
            return Sprache.CSS;
        }
        if (n.endsWith(".js") || n.endsWith(".json") || n.endsWith(".mjs")) {
            return Sprache.JS;
        }
        if (n.endsWith(".txt") || n.endsWith(".csv") || n.endsWith(".log")) {
            return Sprache.TEXT;
        }
        String t = inhalt == null ? "" : inhalt.trim();
        if (t.startsWith("<") || t.contains("</")) {
            return Sprache.HTML;
        }
        return Sprache.TEXT;
    }

    /** colour class of every character */
    public static byte[] faerben(String s, Sprache sprache) {
        byte[] a = new byte[s.length()];
        switch (sprache) {
        case HTML:
            html(s, a);
            break;
        case CSS:
            css(s, 0, s.length(), a);
            break;
        case JS:
            js(s, 0, s.length(), a);
            break;
        default:
            break;
        }
        return a;
    }

    private static void setze(byte[] a, int von, int bis, byte art) {
        Arrays.fill(a, Math.max(0, von), Math.min(a.length, bis), art);
    }

    // ------------------------------------------------------------------ HTML

    private static void html(String s, byte[] a) {
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '<') {
                if (s.startsWith("<!--", i)) {
                    int e = s.indexOf("-->", i + 4);
                    e = e < 0 ? n : e + 3;
                    setze(a, i, e, KOMMENTAR);
                    i = e;
                    continue;
                }
                if (i + 1 < n && (s.charAt(i + 1) == '!' || s.charAt(i + 1) == '?')) {
                    int e = s.indexOf('>', i);
                    e = e < 0 ? n : e + 1;
                    setze(a, i, e, TAG);
                    i = e;
                    continue;
                }
                int j = i + 1;
                boolean schliessend = j < n && s.charAt(j) == '/';
                if (schliessend) {
                    j++;
                }
                int nameStart = j;
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '-' || s.charAt(j) == ':')) {
                    j++;
                }
                if (j == nameStart) {
                    i++; // a lonely "<" in the text
                    continue;
                }
                String name = s.substring(nameStart, j).toLowerCase(Locale.ROOT);
                setze(a, i, j, TAG);
                int ende = tagRest(s, j, a);
                i = ende;
                if (!schliessend && (name.equals("style") || name.equals("script"))) {
                    int schluss = indexOfIgnoreCase(s, "</" + name, i);
                    if (schluss < 0) {
                        schluss = n;
                    }
                    if (name.equals("style")) {
                        css(s, i, schluss, a);
                    } else {
                        js(s, i, schluss, a);
                    }
                    i = schluss;
                }
                continue;
            }
            if (c == '&') {
                int j = i + 1;
                while (j < n && j - i < 12 && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '#')) {
                    j++;
                }
                if (j < n && s.charAt(j) == ';' && j > i + 1) {
                    setze(a, i, j + 1, ENTITY);
                    i = j + 1;
                    continue;
                }
            }
            i++;
        }
    }

    /** attributes up to and including the closing ">" of a tag; returns the position after it */
    private static int tagRest(String s, int i, byte[] a) {
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '>') {
                a[i] = TAG;
                return i + 1;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '>') {
                setze(a, i, i + 2, TAG);
                return i + 2;
            }
            if (c == '<') {
                return i; // unfinished tag
            }
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '=') {
                i++;
                while (i < n && Character.isWhitespace(s.charAt(i))) {
                    i++;
                }
                if (i < n && (s.charAt(i) == '"' || s.charAt(i) == '\'')) {
                    char q = s.charAt(i);
                    int e = s.indexOf(q, i + 1);
                    e = e < 0 ? n : e + 1;
                    setze(a, i, e, WERT);
                    i = e;
                } else {
                    int st = i;
                    while (i < n && !Character.isWhitespace(s.charAt(i)) && s.charAt(i) != '>') {
                        i++;
                    }
                    setze(a, st, i, WERT);
                }
                continue;
            }
            int st = i;
            while (i < n && !Character.isWhitespace(s.charAt(i)) && s.charAt(i) != '=' && s.charAt(i) != '>' && s.charAt(i) != '<'
                    && !(s.charAt(i) == '/' && i + 1 < n && s.charAt(i + 1) == '>')) {
                i++;
            }
            if (i == st) {
                i++;
            } else {
                setze(a, st, i, ATTRIBUT);
            }
        }
        return n;
    }

    private static int indexOfIgnoreCase(String s, String muster, int von) {
        int m = muster.length();
        for (int i = Math.max(0, von); i + m <= s.length(); i++) {
            if (s.regionMatches(true, i, muster, 0, m)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ CSS

    private static void css(String s, int von, int bis, byte[] a) {
        int i = von;
        int tiefe = 0;
        while (i < bis) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < bis && s.charAt(i + 1) == '*') {
                i = cssKommentar(s, i, bis, a);
                continue;
            }
            if (c == '}') {
                tiefe = Math.max(0, tiefe - 1);
                i++;
                continue;
            }
            // a part up to ; { or }
            int ende = i;
            int doppelpunkt = -1;
            char abschluss = 0;
            while (ende < bis) {
                char d = s.charAt(ende);
                if (d == '/' && ende + 1 < bis && s.charAt(ende + 1) == '*') {
                    break;
                }
                if (d == '"' || d == '\'') {
                    int e = s.indexOf(d, ende + 1);
                    ende = e < 0 || e >= bis ? bis : e + 1;
                    continue;
                }
                if (d == ';' || d == '{' || d == '}') {
                    abschluss = d;
                    break;
                }
                if (d == ':' && doppelpunkt < 0) {
                    doppelpunkt = ende;
                }
                if (d == '(' ) {
                    int e = s.indexOf(')', ende);
                    if (e > 0 && e < bis && s.substring(ende, e).indexOf('{') < 0) {
                        ende = e + 1;
                        continue;
                    }
                }
                ende++;
            }
            boolean deklaration = tiefe > 0 && abschluss != '{' && doppelpunkt >= 0 && c != '@';
            if (c == '@') {
                int w = i + 1;
                while (w < ende && (Character.isLetterOrDigit(s.charAt(w)) || s.charAt(w) == '-')) {
                    w++;
                }
                setze(a, i, w, AT_REGEL);
                cssWert(s, w, ende, a);
            } else if (deklaration) {
                setze(a, i, doppelpunkt, EIGENSCHAFT);
                cssWert(s, doppelpunkt + 1, ende, a);
            } else {
                setze(a, i, ende, SELEKTOR);
                // strings in selectors, e.g. [type="text"]
                for (int k = i; k < ende; k++) {
                    char d = s.charAt(k);
                    if (d == '"' || d == '\'') {
                        int e = s.indexOf(d, k + 1);
                        e = e < 0 || e >= ende ? ende : e + 1;
                        setze(a, k, e, STRING);
                        k = e - 1;
                    }
                }
            }
            if (abschluss == '{') {
                tiefe++;
                i = ende + 1;
            } else if (abschluss == ';') {
                i = ende + 1;
            } else {
                i = ende;
            }
        }
    }

    private static int cssKommentar(String s, int i, int bis, byte[] a) {
        int e = s.indexOf("*/", i + 2);
        e = e < 0 || e + 2 > bis ? bis : e + 2;
        setze(a, i, e, KOMMENTAR);
        return e;
    }

    private static void cssWert(String s, int von, int bis, byte[] a) {
        int i = von;
        while (i < bis) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < bis && s.charAt(i + 1) == '*') {
                i = cssKommentar(s, i, bis, a);
                continue;
            }
            if (c == '"' || c == '\'') {
                int e = s.indexOf(c, i + 1);
                e = e < 0 || e >= bis ? bis : e + 1;
                setze(a, i, e, STRING);
                i = e;
                continue;
            }
            if (c == '#') {
                int e = i + 1;
                while (e < bis && Character.isLetterOrDigit(s.charAt(e))) {
                    e++;
                }
                setze(a, i, e, CSS_WERT);
                i = e;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' || c == '-' || c == '+') && i + 1 < bis && Character.isDigit(s.charAt(i + 1))
                    && (i == von || !Character.isLetterOrDigit(s.charAt(i - 1)))) {
                int e = i + 1;
                while (e < bis && (Character.isLetterOrDigit(s.charAt(e)) || s.charAt(e) == '.' || s.charAt(e) == '%')) {
                    e++;
                }
                setze(a, i, e, ZAHL);
                i = e;
                continue;
            }
            if (c == '!' ) {
                int e = i + 1;
                while (e < bis && Character.isLetter(s.charAt(e))) {
                    e++;
                }
                setze(a, i, e, SCHLUESSELWORT);
                i = e;
                continue;
            }
            if (Character.isLetter(c) || c == '-' || c == '_') {
                int e = i;
                while (e < bis && (Character.isLetterOrDigit(s.charAt(e)) || s.charAt(e) == '-' || s.charAt(e) == '_')) {
                    e++;
                }
                setze(a, i, e, e < bis && s.charAt(e) == '(' ? FUNKTION : CSS_WERT);
                i = e;
                continue;
            }
            i++;
        }
    }

    // ------------------------------------------------------------------ JavaScript

    private static void js(String s, int von, int bis, byte[] a) {
        int i = von;
        // what came before: true if a slash now starts a regular expression
        boolean regexMoeglich = true;
        while (i < bis) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < bis && s.charAt(i + 1) == '/') {
                int e = s.indexOf('\n', i);
                e = e < 0 || e > bis ? bis : e;
                setze(a, i, e, KOMMENTAR);
                i = e;
                continue;
            }
            if (c == '/' && i + 1 < bis && s.charAt(i + 1) == '*') {
                int e = s.indexOf("*/", i + 2);
                e = e < 0 || e + 2 > bis ? bis : e + 2;
                setze(a, i, e, KOMMENTAR);
                i = e;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                int e = i + 1;
                while (e < bis && s.charAt(e) != c) {
                    if (s.charAt(e) == '\\') {
                        e++;
                    } else if (s.charAt(e) == '\n' && c != '`') {
                        break;
                    }
                    e++;
                }
                e = Math.min(bis, e + 1);
                setze(a, i, e, STRING);
                i = e;
                regexMoeglich = false;
                continue;
            }
            if (c == '/' && regexMoeglich) {
                int e = i + 1;
                boolean klasse = false;
                while (e < bis && s.charAt(e) != '\n') {
                    char d = s.charAt(e);
                    if (d == '\\') {
                        e += 2;
                        continue;
                    }
                    if (d == '[') {
                        klasse = true;
                    } else if (d == ']') {
                        klasse = false;
                    } else if (d == '/' && !klasse) {
                        break;
                    }
                    e++;
                }
                if (e < bis && s.charAt(e) == '/') {
                    e++;
                    while (e < bis && Character.isLetter(s.charAt(e))) {
                        e++;
                    }
                    setze(a, i, e, REGEX);
                    i = e;
                    regexMoeglich = false;
                    continue;
                }
            }
            if (Character.isDigit(c) || c == '.' && i + 1 < bis && Character.isDigit(s.charAt(i + 1))) {
                int e = i + 1;
                while (e < bis && (Character.isLetterOrDigit(s.charAt(e)) || s.charAt(e) == '.' || s.charAt(e) == '_')) {
                    e++;
                }
                setze(a, i, e, ZAHL);
                i = e;
                regexMoeglich = false;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int e = i + 1;
                while (e < bis && Character.isJavaIdentifierPart(s.charAt(e))) {
                    e++;
                }
                String wort = s.substring(i, e);
                boolean nachPunkt = vorher(s, i, von) == '.';
                byte art;
                if (!nachPunkt && KONTROLLWORTE.contains(wort)) {
                    art = KONTROLLE;
                } else if (!nachPunkt && SCHLUESSEL.contains(wort)) {
                    art = SCHLUESSELWORT;
                } else if (naechstes(s, e, bis) == '(') {
                    art = FUNKTION;
                } else {
                    art = VARIABLE;
                }
                setze(a, i, e, art);
                regexMoeglich = !nachPunkt && VOR_REGEX.contains(wort);
                i = e;
                continue;
            }
            regexMoeglich = c != ')' && c != ']' && c != '}';
            i++;
        }
    }

    private static char vorher(String s, int i, int von) {
        for (int k = i - 1; k >= von; k--) {
            char c = s.charAt(k);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return 0;
    }

    private static char naechstes(String s, int i, int bis) {
        for (int k = i; k < bis; k++) {
            char c = s.charAt(k);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return 0;
    }
}
