package filius.modern;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Personal settings of Filius (RBZ Wirtschaft Kiel Version), stored next to the regular Filius profile
 * (~/.filius/filius-modern.properties). Teachers can switch features off for a whole
 * installation in config/filius.ini:
 *
 * <pre>
 * modern-lernpfad=0    (hide the learning path; the old key modern-missions=0 works too)
 * modern-schichtmodell=osi   (data exchange window starts with the OSI model instead of TCP/IP)
 * modern-browser=klassisch   (web browser uses the classic page view instead of the WebKit engine)
 * modern-startseite=original (a new web server gets the original Filius page instead of the RBZ page)
 * modern-speicherort=H:\Filius (folder that "Open" and "Save" show first; %USERNAME% etc. are replaced)
 * modern-lernpfad-auswahl=standard  (only the standard learning path; fortgeschritten = only the advanced one;
 *                                    beide = students choose, the default)
 * </pre>
 */
public final class Prefs {

    private static final Properties props = new Properties();
    private static File file;
    private static boolean missionsAllowedGlobally = true;
    private static boolean osiByDefault = false;
    private static boolean browserEngineAllowed = true;
    private static boolean modernStartPage = true;
    private static String iniLocale;
    private static String speicherort;
    private static String lernpfadAuswahl;

    private Prefs() {}

    public static synchronized void load() {
        File dir = new File(System.getProperty("user.home"), ".filius");
        file = new File(dir, "filius-modern.properties");
        if (file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                // start with defaults
            }
        }
        readIni();
    }

    /** Reads optional switches from config/filius.ini of the Filius installation. */
    private static void readIni() {
        try {
            URL ini = ClassLoader.getSystemResource("config/filius.ini");
            if (ini == null || !"file".equals(ini.getProtocol())) {
                return;
            }
            byte[] bytes = Files.readAllBytes(new File(ini.toURI()).toPath());
            String inhalt;
            try {
                // editors write UTF-8 today; older files are Latin-1 (umlauts in a folder name)
                inhalt = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            } catch (java.nio.charset.CharacterCodingException e) {
                inhalt = new String(bytes, StandardCharsets.ISO_8859_1);
            }
            for (String raw : inhalt.replace("\uFEFF", "").split("\\r?\\n")) {
                String line = raw.trim();
                if (line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                String key = line.substring(0, line.indexOf('=')).trim();
                String value = line.substring(line.indexOf('=') + 1).trim();
                if (key.equalsIgnoreCase("modern-lernpfad") || key.equalsIgnoreCase("modern-missions")) {
                    missionsAllowedGlobally = !"0".equals(value);
                } else if (key.equalsIgnoreCase("modern-schichtmodell")) {
                    osiByDefault = "osi".equalsIgnoreCase(value);
                } else if (key.equalsIgnoreCase("modern-browser")) {
                    browserEngineAllowed = !"klassisch".equalsIgnoreCase(value) && !"0".equals(value);
                } else if (key.equalsIgnoreCase("modern-startseite")) {
                    modernStartPage = !"original".equalsIgnoreCase(value) && !"0".equals(value);
                } else if (key.equalsIgnoreCase("locale") && !value.isEmpty()) {
                    iniLocale = value;
                } else if (key.equalsIgnoreCase("modern-lernpfad-auswahl")) {
                    lernpfadAuswahl = value;
                } else if (key.equalsIgnoreCase("modern-speicherort") && !value.isEmpty()) {
                    speicherort = value;
                }
            }
        } catch (Exception e) {
            // ini is optional
        }
    }

    public static synchronized void save() {
        if (file == null) {
            return;
        }
        try {
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "Filius (RBZ Wirtschaft Kiel Version)");
            }
        } catch (IOException e) {
            // not critical
        }
    }

    public static synchronized boolean getBool(String key, boolean def) {
        String v = props.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    public static synchronized void setBool(String key, boolean value) {
        props.setProperty(key, Boolean.toString(value));
        save();
    }

    /** Animations and confetti are always on. */
    public static boolean effectsEnabled() {
        return true;
    }

    /** "standard" or "fortgeschritten" if config/filius.ini prescribes one learning path, otherwise null */
    public static synchronized String lernpfadAuswahl() {
        return lernpfadAuswahl;
    }

    /** False if config/filius.ini hides the learning path, and always in the exam edition. */
    public static boolean missionsEnabled() {
        return missionsAllowedGlobally && !Klausur.AKTIV;
    }

    /** False if config/filius.ini asks for the classic page view in the web browser. */
    public static boolean browserEngineAllowed() {
        return browserEngineAllowed;
    }

    /** False if config/filius.ini asks for the original start page of new web servers. */
    public static boolean modernStartPage() {
        return modernStartPage;
    }

    /** True if config/filius.ini asks for the OSI model as the default of the data exchange window. */
    public static boolean osiByDefault() {
        return osiByDefault;
    }

    /**
     * The folder that "Open" and "Save" show first, from config/filius.ini (modern-speicherort), with
     * environment variables replaced (%USERNAME%, %HOMESHARE%, ${HOME}, a leading ~). Null if not set.
     * The system property filius.speicherort comes first: the starter of the exam edition sets it to the
     * folder the starter lies in (the task folder).
     */
    public static String speicherort() {
        String start = System.getProperty("filius.speicherort");
        if (start != null && !start.isBlank()) {
            try {
                return java.nio.file.Paths.get(start.trim()).normalize().toString();
            } catch (RuntimeException e) {
                return start.trim();
            }
        }
        if (speicherort == null) {
            return null;
        }
        String s = speicherort.replaceAll("^[\"']|[\"']$", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("%([A-Za-z0-9_]+)%|\\$\\{([A-Za-z0-9_]+)\\}").matcher(s);
        StringBuilder b = new StringBuilder();
        while (m.find()) {
            String name = m.group(1) != null ? m.group(1) : m.group(2);
            String wert = umgebung(name);
            m.appendReplacement(b, java.util.regex.Matcher.quoteReplacement(wert == null ? m.group() : wert));
        }
        m.appendTail(b);
        s = b.toString();
        if (s.equals("~") || s.startsWith("~/") || s.startsWith("~\\")) {
            s = System.getProperty("user.home") + s.substring(1);
        }
        return s;
    }

    /** environment variable, the name in any case (Windows does not care about case) */
    private static String umgebung(String name) {
        String wert = System.getenv(name);
        if (wert == null) {
            for (java.util.Map.Entry<String, String> e : System.getenv().entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
        }
        return wert;
    }

    /** The language fixed for the installation in config/filius.ini (locale=de_DE), or null. */
    public static String iniLocale() {
        return iniLocale;
    }

    public static synchronized void remove(String prefix) {
        props.stringPropertyNames().stream().filter(k -> k.startsWith(prefix)).forEach(props::remove);
        save();
    }
}
