package filius.modern.lernpfad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * Keeps the progress in the project file: the .fls file is a zip archive, the progress is the
 * entry {@code projekt/lernpfad.properties} next to Filius' own {@code projekt/konfiguration.xml}.
 * Filius ignores the extra entry, so such files still open in the original Filius.
 *
 * <p>The progress is only kept here: Filius (RBZ Wirtschaft Kiel Version) keeps nothing in its own
 * work folder, so students take their work with them in their project file.
 */
public final class ProjektSpeicher {

    static final String EINTRAG = "projekt/lernpfad.properties";

    private ProjektSpeicher() {}

    public static Properties lesen(String fls) {
        if (fls == null || !new File(fls).isFile()) {
            return null;
        }
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(fls))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (EINTRAG.equals(e.getName().replace('\\', '/'))) {
                    Properties p = new Properties();
                    p.load(new StringReader(new String(zip.readAllBytes(), StandardCharsets.UTF_8)));
                    return p;
                }
            }
        } catch (IOException ex) {
            // no progress in this file
        }
        return null;
    }

    /** Adds or replaces the progress entry in an existing project file. */
    public static boolean schreiben(String fls, Properties p) {
        File datei = new File(fls);
        if (!datei.isFile()) {
            return false;
        }
        Map<String, byte[]> eintraege = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(datei))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (!e.isDirectory() && !EINTRAG.equals(e.getName().replace('\\', '/'))) {
                    eintraege.put(e.getName(), zip.readAllBytes());
                } else if (e.isDirectory()) {
                    eintraege.put(e.getName(), null);
                }
            }
        } catch (IOException ex) {
            return false;
        }
        File tmp = new File(datei.getParentFile(), datei.getName() + ".tmp");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmp))) {
            for (Map.Entry<String, byte[]> e : eintraege.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                if (e.getValue() != null) {
                    out.write(e.getValue());
                }
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry(EINTRAG));
            out.write(text(p).getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        } catch (IOException ex) {
            tmp.delete();
            return false;
        }
        try {
            Files.move(tmp.toPath(), datei.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ex) {
            tmp.delete();
            return false;
        }
    }

    private static String text(Properties p) {
        StringWriter w = new StringWriter();
        try {
            p.store(w, "Filius (RBZ Wirtschaft Kiel Version) - Lernpfad");
        } catch (IOException e) {
            // cannot happen with a StringWriter
        }
        return w.toString();
    }
}
